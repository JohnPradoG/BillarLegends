package com.billarlegends.pdfreader.pdf

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * A .docx file is a zip archive; the document's text lives in word/document.xml as a sequence
 * of <w:p> paragraphs containing <w:t> text runs. This extracts a plain-text approximation of
 * the document (paragraph breaks preserved, formatting/images dropped) without pulling in a
 * full OOXML library, since Android's XmlPullParser and java.util.zip already cover what's
 * needed for read-only text extraction.
 */
object DocxTextExtractor {

    fun extractText(file: File): String {
        ZipInputStream(file.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    return parseDocumentXml(zip).ifEmpty {
                        "(Este documento no tiene texto extraíble)"
                    }
                }
                entry = zip.nextEntry
            }
        }
        throw IllegalStateException("El archivo no parece ser un documento de Word (.docx) válido")
    }

    private fun parseDocumentXml(input: InputStream): String {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")

        val builder = StringBuilder()
        var inTextRun = false
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.localName()) {
                    "p" -> if (builder.isNotEmpty()) builder.append("\n\n")
                    "tab" -> builder.append('\t')
                    "br", "cr" -> builder.append('\n')
                    "t" -> inTextRun = true
                }

                XmlPullParser.TEXT -> if (inTextRun) builder.append(parser.text)

                XmlPullParser.END_TAG -> if (parser.localName() == "t") inTextRun = false
            }
            eventType = parser.next()
        }

        return builder.toString().trim()
    }

    /** Works whether or not the parser has namespace processing enabled: with it off (the
     * default for [Xml.newPullParser]), [XmlPullParser.getName] returns the raw "w:t"-style
     * prefixed name instead of just "t". */
    private fun XmlPullParser.localName(): String = name?.substringAfterLast(':') ?: ""
}
