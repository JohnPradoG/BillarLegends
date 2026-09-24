package com.billarlegends.pdfreader.pdf

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class DocumentType(val extension: String) {
    PDF("pdf"),
    DOCX("docx")
}

data class LibraryEntry(
    val id: String,
    val displayName: String,
    val addedAt: Long,
    val sizeBytes: Long,
    val type: DocumentType
)

/**
 * Keeps imported documents (PDF and Word) as private copies under the app's internal storage,
 * so they remain available even if the original file (picked via SAF or received from another
 * app) is moved, deleted, or its access grant expires. A small JSON index next to the copies
 * tracks metadata.
 *
 * Not safe for concurrent use from multiple threads; callers are expected to serialize access
 * (the ViewModel does this by only ever touching it from its own coroutine scope).
 */
class PdfLibraryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "pdfs").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")

    fun listEntries(): List<LibraryEntry> {
        if (!indexFile.exists()) return emptyList()
        val text = runCatching { indexFile.readText() }.getOrNull() ?: return emptyList()
        val array = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()

        val entries = mutableListOf<LibraryEntry>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val type = runCatching { DocumentType.valueOf(obj.optString("type", "PDF")) }
                .getOrDefault(DocumentType.PDF)
            entries += LibraryEntry(
                id = obj.optString("id"),
                displayName = obj.optString("displayName", "Documento"),
                addedAt = obj.optLong("addedAt"),
                sizeBytes = obj.optLong("sizeBytes"),
                type = type
            )
        }
        return entries.sortedByDescending { it.addedAt }
    }

    fun importDocument(uri: Uri, displayName: String, type: DocumentType): LibraryEntry {
        val id = UUID.randomUUID().toString()
        val destFile = fileFor(id, type)

        appContext.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("No se pudo leer el archivo seleccionado")

        val entry = LibraryEntry(
            id = id,
            displayName = displayName,
            addedAt = System.currentTimeMillis(),
            sizeBytes = destFile.length(),
            type = type
        )

        writeIndex(listOf(entry) + listEntries())
        return entry
    }

    fun fileFor(entry: LibraryEntry): File = fileFor(entry.id, entry.type)

    private fun fileFor(id: String, type: DocumentType): File = File(dir, "$id.${type.extension}")

    fun delete(entry: LibraryEntry) {
        fileFor(entry).delete()
        writeIndex(listEntries().filterNot { it.id == entry.id })
    }

    private fun writeIndex(entries: List<LibraryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("displayName", entry.displayName)
                    put("addedAt", entry.addedAt)
                    put("sizeBytes", entry.sizeBytes)
                    put("type", entry.type.name)
                }
            )
        }
        indexFile.writeText(array.toString())
    }
}
