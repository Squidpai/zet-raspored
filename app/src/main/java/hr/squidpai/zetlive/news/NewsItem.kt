package hr.squidpai.zetlive.news

import org.jsoup.Jsoup
import java.io.Serializable
import java.time.LocalDateTime

class NewsItem(
    val title: String?,
    private val rawDescription: String?,
    val publicationDate: LocalDateTime?,
) : Serializable {
    // this is here because AnnotatedString, FontStyle, etc. are not serializable
    // this doesn't really matter as it takes around 2-10 ms to load a news article
    @Transient
    private var _parsedDescription: List<Element>? = null

    private val parsedDescription get() = _parsedDescription
        ?: parseDescription(rawDescription).also { _parsedDescription = it }

    val briefDescription get() = parsedDescription.firstTextOrBlank().text

    val elements get() = parsedDescription

    companion object {
        @Suppress("unused")
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