package hr.squidpai.zetlive.news

import org.jsoup.nodes.Node

val Node.style: Map<String, String>
    get() {
        val returnValue = mutableMapOf<String, String>()

        val style = attr("style")
        val styleAttrs = style.split(";")

        for (rawStyle in styleAttrs) {
            val indexOfColon = rawStyle.indexOf(':')
            if (indexOfColon == -1)
                returnValue[rawStyle.trim()] = ""
            else
                returnValue[rawStyle.substring(0, indexOfColon).trim()] =
                    rawStyle.substring(indexOfColon + 1).trim()
        }

        return returnValue
    }