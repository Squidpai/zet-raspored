package hr.squidpai.zetlive.news

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import java.io.Serializable

sealed interface Element : Serializable {

    @JvmInline
    value class Text(val text: AnnotatedString) : Element {
        companion object {
            val Blank = Text(AnnotatedString(""))
        }
    }

    data class Image(
        val sourceUrl: String,
        val altText: String?,
        val size: IntSize,
        val clickableUrl: String?,
    ) : Element

}