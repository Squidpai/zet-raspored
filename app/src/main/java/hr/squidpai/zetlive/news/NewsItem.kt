package hr.squidpai.zetlive.news

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.io.Serializable
import java.time.LocalDateTime
import org.jsoup.nodes.Element as ElementNode

class NewsItem(
    val title: String?,
    private val rawDescription: String?,
    val publicationDate: LocalDateTime?,
) : Serializable {
    // this is here because AnnotatedString is not serializable
    @Transient // TODO remove this
    private var _parsedDescription: List<Element>? = null

    private val parsedDescription get() = _parsedDescription
        ?: parseDescription(rawDescription).also { _parsedDescription = it }

    val briefDescription get() = parsedDescription.firstTextOrBlank().text

    val elements get() = parsedDescription

    companion object {
        private const val TAG = "NewsItem"

        private fun List<Element>.firstTextOrBlank(): Element.Text {
            for (element in this)
                if (element is Element.Text)
                    return element
            return Element.Text.Blank
        }

        private fun parseDescription(description: String?): List<Element> {
            if (description == null)
                return emptyList()

            val parser = DocumentParser()

            val documentBody = Jsoup.parse(description).body()

            parser.parse(documentBody)

            return parser.build()
        }

        private class DocumentParser {

            private val elements = ArrayList<Element>()

            private var textBuilder = AnnotatedString.Builder()

            fun parse(node: Node) {
                when (node) {
                    is TextNode -> parse(node)
                    is ElementNode -> parse(node)
                    is Comment -> { /* ignore comment */ }
                }
            }

            fun build() = elements.toList()

            private fun parse(node: TextNode) {
                textBuilder.append(node.text())
            }

            private var lastElementIsBr = false

            private fun parse(node: ElementNode) {
                val tagName = node.tagName()

                if (tagName == "br") {
                    if (lastElementIsBr) {
                        finishText()
                        lastElementIsBr = false
                    } else {
                        textBuilder.append('\n')
                        lastElementIsBr = true
                    }
                    return
                }

                if (tagName == "img") {
                    val sourceUrl = node.attr("abs:src")
                    val altText = node.attr("alt").takeIf { it.isNotEmpty() }

                    val style = node.style
                    val widthString = style["width"]
                    val heightString = style["height"]

                    val width =
                        if (widthString?.endsWith("px") == true)
                            widthString.substring(0, widthString.length - 2).toIntOrNull() ?: 0
                        else 0
                    val height =
                        if (width != 0 && heightString?.endsWith("px") == true)
                            heightString.substring(0, heightString.length - 2).toIntOrNull() ?: 0
                        else 0

                    elements += Element.Image(sourceUrl, altText, size = IntSize(width, height))
                    return
                }

                val style = when (tagName) {
                    "b", "strong" -> boldStyle
                    "i", "em" -> italicStyle
                    "u" -> underlineStyle
                    else -> null
                }

                if (style != null)
                    textBuilder.pushStyle(style)

                for (childNode in node.childNodes())
                    parse(childNode)

                if (style != null)
                    try {
                        textBuilder.pop()
                    } catch (_: IllegalStateException) {
                        // ignore if we cannot pop styles
                    }
            }

            private fun finishText() {
                if (textBuilder.length > 0) {
                    elements += Element.Text(textBuilder.toAnnotatedString())
                    textBuilder = AnnotatedString.Builder()
                }
            }

            companion object {
                val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
                val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
                val underlineStyle = SpanStyle(textDecoration = TextDecoration.Underline)
            }

        }
    }
}
