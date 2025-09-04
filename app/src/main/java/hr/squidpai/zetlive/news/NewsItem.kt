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

    }
}