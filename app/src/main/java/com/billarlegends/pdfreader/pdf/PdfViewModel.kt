package com.billarlegends.pdfreader.pdf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Owns both the on-disk PDF library (import/list/delete, via [PdfLibraryRepository]) and the
 * lifecycle of the currently open [PdfRenderer]. PdfRenderer can only have one page open at a
 * time and is not thread-safe, so every access to it is serialized through [renderMutex].
 */
class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfLibraryRepository(application)

    var uiState by mutableStateOf<PdfUiState>(PdfUiState.Library(isBusy = true))
        private set

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private val renderMutex = Mutex()
    private val pageCache = mutableMapOf<Int, Bitmap>()

    init {
        viewModelScope.launch { refreshLibrary() }
    }

    /** Copies [uri] into the app's private storage and opens the copy. Used for both the
     * file picker and documents received from other apps (share / "open with"). */
    fun importAndOpen(uri: Uri) {
        viewModelScope.launch {
            setBusy(true)
            val displayName = queryDisplayName(uri)
            val type = inferDocumentType(uri, displayName)
            if (type == null) {
                showLibraryError("Formato no compatible. Esta app abre archivos PDF y Word (.docx).")
                return@launch
            }
            try {
                val entry = withContext(Dispatchers.IO) {
                    repository.importDocument(uri, displayName, type)
                }
                openEntryInternal(entry)
            } catch (e: Exception) {
                showLibraryError("No se pudo importar el archivo: ${e.message ?: "error desconocido"}")
            }
        }
    }

    fun openEntry(entry: LibraryEntry) {
        viewModelScope.launch { openEntryInternal(entry) }
    }

    fun closeViewer() {
        viewModelScope.launch {
            closeCurrentDocumentLocked()
            refreshLibrary()
        }
    }

    fun deleteEntry(entry: LibraryEntry) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(entry) }
            refreshLibrary()
        }
    }

    fun dismissError() {
        (uiState as? PdfUiState.Library)?.let { uiState = it.copy(errorMessage = null) }
    }

    suspend fun renderPage(index: Int, targetWidthPx: Int): Bitmap? {
        if (targetWidthPx <= 0) return null

        return renderMutex.withLock {
            pageCache[index]?.let { cached ->
                if (cached.width == targetWidthPx) return@withLock cached
            }

            val currentRenderer = renderer ?: return@withLock null
            if (index < 0 || index >= currentRenderer.pageCount) return@withLock null

            withContext(Dispatchers.IO) {
                val page = currentRenderer.openPage(index)
                try {
                    val scale = targetWidthPx.toFloat() / page.width
                    val targetHeightPx = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(AndroidColor.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pageCache[index]?.recycle()
                    pageCache[index] = bitmap
                    bitmap
                } finally {
                    page.close()
                }
            }
        }
    }

    private suspend fun openEntryInternal(entry: LibraryEntry) {
        closeCurrentDocumentLocked()

        uiState = try {
            when (entry.type) {
                DocumentType.PDF -> openPdfEntry(entry)
                DocumentType.DOCX -> openDocxEntry(entry)
            }
        } catch (e: SecurityException) {
            PdfUiState.Library(
                entries = withContext(Dispatchers.IO) { repository.listEntries() },
                errorMessage = "Este archivo está protegido con contraseña y no se puede abrir."
            )
        } catch (e: Exception) {
            PdfUiState.Library(
                entries = withContext(Dispatchers.IO) { repository.listEntries() },
                errorMessage = "No se pudo abrir el archivo: ${e.message ?: "error desconocido"}"
            )
        }
    }

    private suspend fun openPdfEntry(entry: LibraryEntry): PdfUiState {
        val file = repository.fileFor(entry)
        val descriptor = withContext(Dispatchers.IO) {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        val newRenderer = withContext(Dispatchers.IO) { PdfRenderer(descriptor) }

        fileDescriptor = descriptor
        renderer = newRenderer

        return PdfUiState.LoadedPdf(entry = entry, pageCount = newRenderer.pageCount)
    }

    private suspend fun openDocxEntry(entry: LibraryEntry): PdfUiState {
        val file = repository.fileFor(entry)
        val text = withContext(Dispatchers.IO) { DocxTextExtractor.extractText(file) }
        return PdfUiState.LoadedText(entry = entry, content = text)
    }

    /** Acquires [renderMutex] itself, so it never races an in-flight [renderPage] call. */
    private suspend fun closeCurrentDocumentLocked() {
        renderMutex.withLock {
            withContext(Dispatchers.IO) {
                pageCache.values.forEach { it.recycle() }
                pageCache.clear()
                renderer?.close()
                renderer = null
                fileDescriptor?.close()
                fileDescriptor = null
            }
        }
    }

    private suspend fun refreshLibrary() {
        val entries = withContext(Dispatchers.IO) { repository.listEntries() }
        uiState = PdfUiState.Library(entries = entries)
    }

    private fun setBusy(busy: Boolean) {
        uiState = (uiState as? PdfUiState.Library)?.copy(isBusy = busy)
            ?: PdfUiState.Library(isBusy = busy)
    }

    private suspend fun showLibraryError(message: String) {
        uiState = PdfUiState.Library(
            entries = withContext(Dispatchers.IO) { repository.listEntries() },
            errorMessage = message
        )
    }

    private fun queryDisplayName(uri: Uri): String {
        if (uri.scheme == "content") {
            runCatching {
                getApplication<Application>().contentResolver
                    .query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && cursor.moveToFirst()) {
                            return cursor.getString(nameIndex) ?: "Documento"
                        }
                    }
            }
        }
        return uri.lastPathSegment ?: "Documento"
    }

    private fun inferDocumentType(uri: Uri, displayName: String): DocumentType? {
        val mimeType = getApplication<Application>().contentResolver.getType(uri)
        return when {
            mimeType == "application/pdf" -> DocumentType.PDF
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DocumentType.DOCX
            displayName.endsWith(".pdf", ignoreCase = true) -> DocumentType.PDF
            displayName.endsWith(".docx", ignoreCase = true) -> DocumentType.DOCX
            else -> null
        }
    }

    override fun onCleared() {
        // viewModelScope is already cancelled by the time onCleared() runs, so this can't
        // go through the mutex; it's a best-effort release as the ViewModel is torn down.
        pageCache.values.forEach { it.recycle() }
        pageCache.clear()
        renderer?.close()
        renderer = null
        fileDescriptor?.close()
        fileDescriptor = null
    }
}
