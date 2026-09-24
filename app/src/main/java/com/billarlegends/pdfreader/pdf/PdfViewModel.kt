package com.billarlegends.pdfreader.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Owns the lifecycle of the currently open [PdfRenderer]. PdfRenderer can only have one
 * page open at a time and is not thread-safe, so every access to it is serialized through
 * [renderMutex] on a background dispatcher.
 */
class PdfViewModel : ViewModel() {

    var uiState by mutableStateOf<PdfUiState>(PdfUiState.Empty)
        private set

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private val renderMutex = Mutex()
    private val pageCache = mutableMapOf<Int, Bitmap>()

    fun openDocument(context: Context, uri: Uri) {
        viewModelScope.launch {
            uiState = PdfUiState.Loading
            closeCurrentDocumentLocked()

            uiState = try {
                val descriptor = withContext(Dispatchers.IO) {
                    context.contentResolver.openFileDescriptor(uri, "r")
                } ?: throw IllegalStateException("El proveedor de contenido no devolvió el archivo")

                val newRenderer = withContext(Dispatchers.IO) { PdfRenderer(descriptor) }

                fileDescriptor = descriptor
                renderer = newRenderer

                PdfUiState.Loaded(
                    fileName = queryDisplayName(context, uri),
                    pageCount = newRenderer.pageCount
                )
            } catch (e: SecurityException) {
                PdfUiState.Error("Este PDF está protegido con contraseña y no se puede abrir.")
            } catch (e: Exception) {
                PdfUiState.Error("No se pudo abrir el PDF: ${e.message ?: "error desconocido"}")
            }
        }
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

    fun closeDocument() {
        viewModelScope.launch {
            closeCurrentDocumentLocked()
            uiState = PdfUiState.Empty
        }
    }

    /** Must only be called while holding [renderMutex], so it never races an in-flight [renderPage]. */
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

    private fun queryDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex) ?: "Documento PDF"
                    }
                }
            }
        }
        return uri.lastPathSegment ?: "Documento PDF"
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
