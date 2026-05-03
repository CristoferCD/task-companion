package es.cristcd.taskcompanion.parser

typealias Attributes = Map<String, String>

data class ParsedAttributes(val attributes: Attributes, val remainingText: String)

fun parseInlineAttributes(text: String): ParsedAttributes {
    val match = Patterns.ATTRS_STR_PATTERN.matchAt(text, 0) ?: return ParsedAttributes(emptyMap(), text)

    val attrString = match.groupValues.first()
    val attrs = parseAttributes(attrString)

    return ParsedAttributes(attrs, text.replace(attrString, ""))
}

private fun parseAttributes(attrString: String): Attributes {
    val attributes = mutableMapOf<String, String>()
    var remainingString = attrString

    Patterns.LANG_PATTERN.matchAt(remainingString, 0)?.let { match ->
        val lang = match.groupValues.getOrNull(1) ?: ""
        attributes["lang"] = lang

        remainingString = remainingString.replace(lang, "")
    }

    Patterns.CLASS_ID_PATTERN.matchAt(remainingString, 0)?.let { match ->
        match.groups["class"]?.value?.let { attributes["class"] = it }
        match.groups["id"]?.value?.let { attributes["id"] = it }

        remainingString = remainingString.replace(match.value, "")
    }

    Patterns.CSS_PROPS_PATTERN.matchAt(remainingString, 0)?.let { match ->
        val props = match.groupValues.getOrNull(1) ?: ""

        props.split(Patterns.CSS_PROPS_SPLIT_PATTERN).forEach { cssProp ->
            val propMatch = Patterns.CSS_PROP_STR_PATTERN.matchAt(cssProp, 0)
            val cssKey = propMatch?.groups["key"]?.value
            val cssValue = propMatch?.groups["value"]?.value
            if (cssKey != null && cssValue != null) {
                attributes[cssKey] = cssValue
            }
        }

        remainingString = remainingString.replace(match.value, "")
    }

    return attributes
}

