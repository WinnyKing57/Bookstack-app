package com.winnyking.bookstackcompanion.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object HtmlToMarkdownConverter {

    fun convert(html: String): String {
        if (html.isBlank()) return ""
        val doc = Jsoup.parse(html)
        val body = doc.body()
        val builder = StringBuilder()
        for (node in body.childNodes()) {
            appendNode(node, builder)
        }
        return builder.toString()
            .replace(Regex("\n{3,}"), "\n\n")
            .trim() + "\n"
    }

    private fun appendNode(node: Node, out: StringBuilder) {
        when (node) {
            is TextNode -> out.append(node.text().replace(Regex("\\s+"), " "))
            is Element -> appendElement(node, out)
            else -> {}
        }
    }

    private fun appendChildren(element: Element, out: StringBuilder) {
        for (child in element.childNodes()) {
            appendNode(child, out)
        }
    }

    private fun appendElement(element: Element, out: StringBuilder) {
        val tag = element.tagName().lowercase()
        when (tag) {
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val level = tag[1].digitToInt()
                out.append("\n\n").append("#".repeat(level)).append(' ')
                appendChildren(element, out)
                out.append("\n\n")
            }
            "p" -> {
                out.append("\n\n")
                appendChildren(element, out)
                out.append("\n\n")
            }
            "br" -> out.append("  \n")
            "hr" -> out.append("\n\n---\n\n")
            "strong", "b" -> {
                out.append("**")
                appendChildren(element, out)
                out.append("**")
            }
            "em", "i" -> {
                out.append("*")
                appendChildren(element, out)
                out.append("*")
            }
            "code" -> if (element.parent()?.tagName()?.lowercase() == "pre") {
                appendChildren(element, out)
            } else {
                out.append('`')
                appendChildren(element, out)
                out.append('`')
            }
            "pre" -> {
                val code = element.wholeText().removeSuffix("\n")
                out.append("\n\n```\n").append(code).append("\n```\n\n")
            }
            "a" -> {
                val href = element.absUrl("href").ifBlank { element.attr("href") }
                out.append('[')
                appendChildren(element, out)
                out.append("](").append(href).append(')')
            }
            "img" -> {
                val src = element.absUrl("src").ifBlank { element.attr("src") }
                val alt = element.attr("alt")
                out.append("![").append(alt).append("](").append(src).append(')')
            }
            "ul", "ol" -> appendList(element, out, ordered = tag == "ol", depth = 0)
            "li" -> appendChildren(element, out)
            "blockquote" -> {
                out.append("\n\n")
                val inner = StringBuilder()
                appendChildren(element, inner)
                inner.toString().trim().lines().forEach { line ->
                    out.append("> ").append(line.trim()).append("\n")
                }
                out.append("\n")
            }
            "table" -> appendTable(element, out)
            "div", "section", "article", "span", "figure", "figcaption", "main" -> appendChildren(element, out)
            else -> appendChildren(element, out)
        }
    }

    private fun appendList(list: Element, out: StringBuilder, ordered: Boolean, depth: Int) {
        out.append('\n')
        var index = 1
        for (child in list.children()) {
            if (child.tagName().lowercase() != "li") continue
            val indent = "    ".repeat(depth)
            val marker = if (ordered) "$index." else "-"
            out.append('\n').append(indent).append(marker).append(' ')
            appendListItemContent(child, out, depth + 1)
            index++
        }
        out.append("\n")
    }

    private fun appendListItemContent(li: Element, out: StringBuilder, childDepth: Int) {
        for (child in li.childNodes()) {
            if (child is Element && child.tagName().lowercase() in listOf("ul", "ol")) {
                appendList(child, out, ordered = child.tagName().lowercase() == "ol", depth = childDepth)
            } else {
                appendInline(child, out)
            }
        }
    }

    private fun appendInline(node: Node, out: StringBuilder) {
        when (node) {
            is TextNode -> out.append(node.text().replace(Regex("\\s+"), " "))
            is Element -> {
                val tag = node.tagName().lowercase()
                if (tag == "p") {
                    appendChildren(node, out)
                } else {
                    appendElement(node, out)
                }
            }
            else -> {}
        }
    }

    private fun appendTable(table: Element, out: StringBuilder) {
        val rows = table.select("tr")
        if (rows.isEmpty()) return
        out.append("\n\n")

        fun cellsOf(row: Element) = row.select("th, td").map { it.text().replace("|", "\\|").trim() }

        val headerCells = cellsOf(rows.first())
        out.append("| ").append(headerCells.joinToString(" | ")).append(" |\n")
        out.append("| ").append(headerCells.joinToString(" | ") { "---" }).append(" |\n")

        rows.drop(1).forEach { row ->
            val cells = cellsOf(row)
            out.append("| ").append(cells.joinToString(" | ")).append(" |\n")
        }
        out.append("\n")
    }
}
