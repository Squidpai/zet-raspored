package hr.squidpai.zetlive.news

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.IntSize
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.nodes.Element as ElementNode

class DocumentParser {

    private val elements = ArrayList<Element>()

    private var textBuilder = AnnotatedString.Builder()

    private var currentLink: String? = null

    fun parse(node: Node) {
        when (node) {
            is TextNode -> parse(node)
            is ElementNode -> parse(node)
            is Comment -> {
                // ignore comment
            }
        }
    }

    fun build() = elements.toList()

    private fun parse(node: TextNode) {
        val text = node.text()
        if (text.isNotBlank()) {
            textBuilder.append(text)
            lastElementIsBr = false
        }
    }

    private var lastElementIsBr = false

    private fun parse(node: ElementNode) {
        val tagName = node.tagName()

        if (tagName == "br") {
            parseBr()
            return
        }
        lastElementIsBr = false

        val elementIsBlock = elementIsBlockElement(node)

        if (elementIsBlock)
            finishText()

        val style: SpanStyle?

        when (tagName) {
            "img" -> {
                parseImg(node)
                return
            }

            "a" -> {
                parseA(node)
                return
            }

            "li" -> {
                parseLi(node)
                return
            }

            "b", "strong" -> style = boldStyle
            "i", "em" -> style = italicStyle
            "u" -> style = underlineStyle
            else -> style = null
        }

        if (style != null)
            textBuilder.pushStyle(style)

        for (childNode in node.childNodes())
            parse(childNode)

        if (style != null)
            textBuilder.tryPop()

        if (elementIsBlock)
            finishText()
    }

    private fun parseBr() {
        if (lastElementIsBr)
            finishText()
        else {
            textBuilder.append('\n')
            lastElementIsBr = true
        }
    }

    private fun parseImg(node: ElementNode) {
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

        if (lastElementIsBr)
            finishText()

        elements += Element.Image(
            sourceUrl,
            altText,
            size = IntSize(width, height),
            clickableUrl = currentLink,
        )
    }

    private fun parseA(node: ElementNode) {
        val link = node.attr("href")

        if (link.isNotEmpty()) {
            currentLink = link
            textBuilder.pushLink(LinkAnnotation.Url(link))
        }

        for (childNode in node.childNodes())
            parse(childNode)

        if (link.isNotEmpty()) {
            currentLink = null
            textBuilder.tryPop()
        }
    }

    private fun parseLi(node: ElementNode) {
        textBuilder.pushStyle(ParagraphStyle(textIndent = TextIndent(
            firstLine = Bullet.DefaultIndentation,
            restLine = Bullet.DefaultIndentation,
        )))
        textBuilder.pushBullet(Bullet.Default)

        for (childNode in node.childNodes())
            parse(childNode)

        textBuilder.tryPop()
        textBuilder.tryPop()
    }

    private fun finishText() {
        if (textBuilder.length > 0) {
            elements += Element.Text(textBuilder.toAnnotatedString())
            textBuilder = AnnotatedString.Builder()
            lastElementIsBr = false
        }
    }

    private fun elementIsBlockElement(node: ElementNode) =
        node.tagName() in blockElements

    companion object {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
        val underlineStyle = SpanStyle(textDecoration = TextDecoration.Underline)

        private val blockElements = setOf("div", "ul")

        private fun AnnotatedString.Builder.tryPop() =
            try {
                pop()
            } catch (_: IllegalStateException) {
                // ignore if we cannot pop
            }
    }

}