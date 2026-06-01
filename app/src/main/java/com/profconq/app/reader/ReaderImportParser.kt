package com.profconq.app.reader

import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipInputStream

object ReaderImportParser {
    fun readContent(
        uri: Uri,
        openStream: (Uri) -> InputStream?,
        mimeType: (Uri) -> String?,
    ): String {
        val path = uri.lastPathSegment.orEmpty().lowercase(Locale.ROOT)
        val type = mimeType(uri).orEmpty().lowercase(Locale.ROOT)
        val isDocx = path.endsWith(".docx") || type.contains("wordprocessingml.document")
        val isDoc = path.endsWith(".doc") || type.contains("application/msword")
        if (isDoc) {
            error("Формат .doc не поддерживается. Сохраните файл как .docx.")
        }
        return if (isDocx) {
            openStream(uri)?.use { extractDocxText(it) } ?: error("Не удалось прочитать .docx файл.")
        } else {
            openStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Не удалось прочитать файл.")
        }
    }

    fun extractDocxText(stream: InputStream): String {
        val xml = ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var read = zip.read(buffer)
                    while (read > 0) {
                        out.write(buffer, 0, read)
                        read = zip.read(buffer)
                    }
                    return@use out.toString(StandardCharsets.UTF_8.name())
                }
                entry = zip.nextEntry
            }
            null
        } ?: error("Не удалось прочитать .docx файл.")

        val withBreaks = xml
            .replace(Regex("<w:tab[^>]*/>"), "\t")
            .replace(Regex("<w:br[^>]*/>"), "\n")
            // Paragraph boundaries should stay explicit for fast pagination in Reader.
            .replace("</w:p>", "\n\n")
            .replace("</w:tr>", "\n")
            .replace("</w:tbl>", "\n\n")
        val raw = withBreaks.replace(Regex("<[^>]+>"), "")
        return unescapeXml(raw)
            .lineSequence()
            .map { it.trimEnd() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun unescapeXml(text: String): String = text
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
}
