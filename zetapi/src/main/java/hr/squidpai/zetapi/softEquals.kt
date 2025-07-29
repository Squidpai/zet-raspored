package hr.squidpai.zetapi

private val croatianMapping = intIntMapOf(
    'č'.code, 'c'.code,
    'ć'.code, 'c'.code,
    'š'.code, 's'.code,
    'đ'.code, 'd'.code,
    'ž'.code, 'z'.code,
)

public fun Char.lowercaseAscii(): Char {
    val lowercaseCode = this.lowercaseChar().code
    return croatianMapping.getOrDefault(lowercaseCode, lowercaseCode).toChar()
}

public fun Char.softEquals(other: Char): Boolean {
    if (this == other)
        return true

    val thisLowercase = this.lowercaseChar()
    val otherLowercase = other.lowercaseChar()

    if (thisLowercase == otherLowercase)
        return true

    val thisMapped = croatianMapping.getOrDefault(thisLowercase.code, thisLowercase.code)
    val otherMapped = croatianMapping.getOrDefault(otherLowercase.code, otherLowercase.code)

    return thisMapped == otherMapped
}

public fun String.softEquals(other: String): Boolean {
    if (this.length != other.length)
        return false

    for (i in this.indices)
        if (!this[i].softEquals(other[i]))
            return false

    return true
}

public fun String.softContains(other: String): Boolean {
    if (this.length < other.length)
        return false
    if (other.isEmpty())
        return true

    outerLoop@ for (i in 0..this.length - other.length) {
        for (j in other.indices) {
            if (!this[i + j].softEquals(other[j]))
                continue@outerLoop
        }
        return true
    }

    return false
}

public fun String.isShortenedFrom(longForm: String): Boolean {
    val longFormIterator = longForm.iterator()

    shortFormLoop@ for (shortFormChar in this) {
        val shortFormLowercase = shortFormChar.lowercaseAscii()
        if (!shortFormLowercase.isLetter())
            continue

        while (longFormIterator.hasNext()) {
            val longFormLowercase = longFormIterator.nextChar().lowercaseAscii()

            if (shortFormLowercase == longFormLowercase)
                continue@shortFormLoop
        }

        return false
    }

    return true
}
