package com.mazkiplay.trade.data.api

import com.mazkiplay.trade.data.model.NewsItem
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Minimal RSS / Atom reader built on the platform DOM parser.
 *
 * Android ships javax.xml.parsers, so no extra dependency is needed. The parser is
 * deliberately forgiving: feeds in the wild mix RSS 2.0 with Atom, use several date
 * formats, and sometimes omit the description entirely.
 */
object RssParser {

    private val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss"
    ).map { SimpleDateFormat(it, Locale.US).apply { isLenient = true } }

    fun parse(xml: String, source: String, limit: Int = 30): List<NewsItem> {
        if (xml.isBlank()) return emptyList()
        return runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isExpandEntityReferences = false
                // Harden against XXE on hostile feeds.
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

            val items = doc.getElementsByTagName("item")
            val entries = if (items.length > 0) items else doc.getElementsByTagName("entry")

            (0 until entries.length).mapNotNull { i ->
                val node = entries.item(i)
                if (node !is Element) return@mapNotNull null
                val title = node.textOf("title").stripHtml()
                if (title.isBlank()) return@mapNotNull null
                val link = node.textOf("link").ifBlank {
                    node.getElementsByTagName("link").item(0)?.attributes?.getNamedItem("href")?.nodeValue.orEmpty()
                }
                val rawDate = node.textOf("pubDate").ifBlank {
                    node.textOf("updated").ifBlank { node.textOf("published") }
                }
                val summary = node.textOf("description")
                    .ifBlank { node.textOf("summary") }
                    .ifBlank { node.textOf("content:encoded") }
                    .stripHtml()
                    .take(400)
                NewsItem(
                    id = "$source|${title.hashCode()}",
                    title = title,
                    summary = summary,
                    url = link.trim(),
                    source = source,
                    publishedAt = parseDate(rawDate),
                    category = categorize(title)
                )
            }.sortedByDescending { it.publishedAt }.take(limit)
        }.getOrElse { emptyList() }
    }

    private fun Element.textOf(tag: String): String {
        val list = getElementsByTagName(tag)
        if (list.length == 0) return ""
        val node: Node = list.item(0)
        return node.textContent?.trim().orEmpty()
    }

    private fun parseDate(raw: String): Long {
        if (raw.isBlank()) return System.currentTimeMillis()
        dateFormats.forEach { fmt ->
            runCatching { fmt.parse(raw)?.time }.getOrNull()?.let { return it }
        }
        return System.currentTimeMillis()
    }

    /** Strip tags/entities from feed summaries so they render as clean text. */
    private fun String.stripHtml(): String = this
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun categorize(title: String): String {
        val t = title.lowercase()
        return when {
            t.contains("gold") || t.contains("emas") -> "Gold"
            t.contains("bitcoin") || t.contains("crypto") -> "Crypto"
            t.contains("fed") || t.contains("ecb") || t.contains("bank") || t.contains("rate") -> "Central Bank"
            t.contains("oil") || t.contains("minyak") -> "Commodity"
            t.contains("stock") || t.contains("saham") || t.contains("index") -> "Equity"
            else -> "Forex"
        }
    }
}
