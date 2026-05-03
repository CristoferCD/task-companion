package es.cristcd.taskcompanion.parser

sealed interface InlineItem {
    data class Link(val text: String, val url: String) : InlineItem
    data class Image(val text: String, val url: String) : InlineItem
    data class Bold(val items: List<InlineItem>) : InlineItem
    data class Italic(val items: List<InlineItem>) : InlineItem
    data class Subscript(val items: List<InlineItem>) : InlineItem
    data class Superscript(val items: List<InlineItem>) : InlineItem
    data class Insertion(val items: List<InlineItem>) : InlineItem
    data class Deletion(val items: List<InlineItem>) : InlineItem
    data class Citation(val items: List<InlineItem>) : InlineItem
    data class Abbreviation(val abbreviation: String, val description: String) : InlineItem
    data class Code(val text: String) : InlineItem
    data class Text(val text: String) : InlineItem
    data class Span(val items: List<InlineItem>): InlineItem
    data object LineBreak : InlineItem
}

data class ParseLineResult(val item: InlineItem, val size: Int)

fun parseInline(lines: List<String>): List<InlineItem> {
    val items = mutableListOf<InlineItem>()

    lines.forEachIndexed { idx, line ->
        var charIdx = 0
        val unrecognizedChars = mutableListOf<Char>()
        while (charIdx < line.length) {
            val element = parseElement(line.slice(charIdx until line.length))
            if (element != null) {
                if (unrecognizedChars.isNotEmpty()) {
                    items.add(InlineItem.Text(unrecognizedChars.toCharArray().concatToString()))
                    unrecognizedChars.clear()
                }
                items.add(element.item)
                charIdx += element.size
            } else {
                unrecognizedChars.add(line[charIdx])
                charIdx++
            }
        }
        if (unrecognizedChars.isNotEmpty()) {
            items.add(InlineItem.Text(unrecognizedChars.toCharArray().concatToString()))
        }
        if (lines.getOrNull(idx + 1) != null) {
            items.add(InlineItem.LineBreak)
        }
        if (lines.getOrNull(idx + 1)?.isBlank() == true) {
            items.add(InlineItem.LineBreak)
        }
    }

    return items
}

private fun parseElement(line: String): ParseLineResult? {
    val abbr = parseAbbreviation(line)
    if (abbr != null) {
        return abbr
    }
    val bold = parseBold(line)
    if (bold != null) {
        return bold
    }
    val citation = parseCitation(line)
    if (citation != null) {
        return citation
    }
    val code = parseCode(line)
    if (code != null) {
        return code
    }
    val img = parseImage(line)
    if (img != null) {
        return img
    }
    val italic = parseItalic(line)
    if (italic != null) {
        return italic
    }
    val link = parseLink(line)
    if (link != null) {
        return link
    }
    val notextile = parseNoTextile(line)
    if (notextile != null) {
        return notextile
    }
    val span = parseSpan(line)
    if (span != null) {
        return span
    }
    val insertion = parseInsertion(line)
    if (insertion != null) {
        return insertion
    }
    val deletion = parseDeletion(line)
    if (deletion != null) {
        return deletion
    }
    val superscript = parseSuperscript(line)
    if (superscript != null) {
        return superscript
    }
    val subscript = parseSubscript(line)
    if (subscript != null) {
        return subscript
    }


    return null
}

private fun parseAbbreviation(line: String): ParseLineResult? {
    val match = Patterns.ABBREVIATION_PATTERN.matchAt(line, 0) ?: return null

    val abbr = match.groups["abbreviation"]?.value ?: ""
    val description = match.groups["description"]?.value

    return if (description.isNullOrBlank()) {
        ParseLineResult(
            InlineItem.Text(abbr),
            match.groupValues.first().length
        )
    } else {
        ParseLineResult(
            InlineItem.Abbreviation(abbr, description),
            match.groupValues.first().length
        )
    }
}

private fun parseBold(line: String): ParseLineResult? {
    val match = Patterns.BOLD_TEXT_PATTERN.matchAt(line, 0) ?: return null

    val startMarker = match.groups["count1"]?.value?.length ?: 0
    val endMarker = match.groups["count2"]?.value?.length ?: 0

    return if (startMarker == endMarker && (startMarker == 1 || startMarker == 2)) {
        val innerText = match.groups["string"]?.value ?: return null
        val (attrs, text) = parseInlineAttributes(innerText)
        ParseLineResult(
            InlineItem.Bold(parseInline(listOf(text))),
            match.groupValues.first().length
        )
    } else {
        ParseLineResult(
            InlineItem.Text(match.groupValues.first()),
            match.groupValues.first().length
        )
    }
}

private fun parseCitation(line: String): ParseLineResult? {
    val match = Patterns.CITATION_PATTERN.matchAt(line, 0) ?: return null

    val innerText = match.groups["string"]?.value ?: return null
    val (attrs, text) = parseInlineAttributes(innerText)
    return ParseLineResult(
        InlineItem.Citation(parseInline(listOf(text))),
        match.groupValues.first().length
    )
}

private fun parseCode(line: String): ParseLineResult? {
    val match = Patterns.CODE_PATTERN.matchAt(line, 0) ?: return null
    val innerText = match.groups["code"]?.value ?: return null

    return ParseLineResult(
        InlineItem.Code(innerText),
        match.groupValues.first().length
    )
}

private fun parseImage(line: String): ParseLineResult? {
    val match = Patterns.IMAGE_PATTERN.matchAt(line, 0) ?: return null

    val innerText = match.groups["string"]?.value ?: ""
    val (attrs, text) = parseInlineAttributes(innerText)
    val src = match.groups["href"]?.value ?: ""

    return ParseLineResult(
        InlineItem.Image(text, src),
        match.groupValues.first().length
    )
}

private fun parseItalic(line: String): ParseLineResult? {
    val match = Patterns.ITALIC_TEXT_PATTERN.matchAt(line, 0) ?: return null

    val startMarker = match.groups["count1"]?.value?.length ?: 0
    val endMarker = match.groups["count2"]?.value?.length ?: 0

    return if (startMarker == endMarker && (startMarker == 1 || startMarker == 2)) {
        val innerText = match.groups["string"]?.value ?: return null
        val (attrs, text) = parseInlineAttributes(innerText)
        ParseLineResult(
            InlineItem.Italic(parseInline(listOf(text))),
            match.groupValues.first().length
        )
    } else {
        ParseLineResult(
            InlineItem.Text(match.groupValues.first()),
            match.groupValues.first().length
        )
    }
}

private fun parseLink(line: String): ParseLineResult? {
    val match = Patterns.LINK_PATTERN.matchAt(line, 0) ?: return null

    val innerText = match.groups["string"]?.value ?: return null
    val (attrs, text) = parseInlineAttributes(innerText)
    val src = match.groups["href"]?.value ?: ""

    return if (text == "$") {
        ParseLineResult(
            InlineItem.Link(text = src, url = src),
            match.groupValues.first().length
        )
    } else {
        ParseLineResult(
            InlineItem.Link(text = text, url = src),
            match.groupValues.first().length
        )
    }
}

private fun parseNoTextile(line: String): ParseLineResult? {
    val match = Patterns.NO_TEXTILE_INLINE_PATTERN.matchAt(line, 0) ?: return null

    val innerText = match.groups["string"]?.value ?: return null

    return ParseLineResult(
        InlineItem.Text(innerText),
        match.groupValues.first().length
    )
}

private fun parseSpan(line: String): ParseLineResult? {
    val match = Patterns.SPAN_PATTERN.matchAt(line, 0) ?: return null

    val startMarker = match.groups["count1"]?.value?.length ?: 0
    val endMarker = match.groups["count2"]?.value?.length ?: 0

    return if (startMarker == endMarker && (startMarker == 1 || startMarker == 2)) {
        val innerText = match.groups["string"]?.value ?: return null
        val (attrs, text) = parseInlineAttributes(innerText)
        ParseLineResult(
            InlineItem.Span(parseInline(listOf(text))),
            match.groupValues.first().length
        )
    } else {
        ParseLineResult(
            InlineItem.Text(match.groupValues.first()),
            match.groupValues.first().length
        )
    }
}

private fun parseInsertion(line: String): ParseLineResult? {
    val match = Patterns.INSERTION_TEXT_PATTERN.matchAt(line, 0) ?: return null

    val startMarker = match.groups["count1"]?.value?.length ?: 0
    val endMarker = match.groups["count2"]?.value?.length ?: 0

    return if (startMarker == endMarker && (startMarker == 1 || startMarker == 2)) {
        val innerText = match.groups["string"]?.value ?: return null
        val (attrs, text) = parseInlineAttributes(innerText)
        ParseLineResult(
            InlineItem.Insertion(parseInline(listOf(text))),
            match.groupValues.first().length
        )
    } else {
        ParseLineResult(
            InlineItem.Text(match.groupValues.first()),
            match.groupValues.first().length
        )
    }


}

private fun parseDeletion(line: String): ParseLineResult? {
    val match = Patterns.DELETION_TEXT_PATTERN.matchAt(line, 0) ?: return null

    val startMarker = match.groups["count1"]?.value?.length ?: 0
    val endMarker = match.groups["count2"]?.value?.length ?: 0

    return if (startMarker == endMarker && (startMarker == 1 || startMarker == 2)) {
        val innerText = match.groups["string"]?.value ?: return null
        val (attrs, text) = parseInlineAttributes(innerText)
        ParseLineResult(
            InlineItem.Deletion(parseInline(listOf(text))),
            match.groupValues.first().length
        )
    } else {
        ParseLineResult(
            InlineItem.Text(match.groupValues.first()),
            match.groupValues.first().length
        )
    }
}

private fun parseSuperscript(line: String): ParseLineResult? {
    val match = Patterns.SUPERSCRIPT_TEXT_PATTERN.matchAt(line, 0) ?: return null

    val startMarker = match.groups["count1"]?.value?.length ?: 0
    val endMarker = match.groups["count2"]?.value?.length ?: 0

    return if (startMarker == endMarker && (startMarker == 1 || startMarker == 2)) {
        val innerText = match.groups["string"]?.value ?: return null
        val (attrs, text) = parseInlineAttributes(innerText)
        ParseLineResult(
            InlineItem.Superscript(parseInline(listOf(text))),
            match.groupValues.first().length
        )
    } else {
        ParseLineResult(
            InlineItem.Text(match.groupValues.first()),
            match.groupValues.first().length
        )
    }
}

private fun parseSubscript(line: String): ParseLineResult? {
    val match = Patterns.SUBSCRIPT_TEXT_PATTERN.matchAt(line, 0) ?: return null

    val startMarker = match.groups["count1"]?.value?.length ?: 0
    val endMarker = match.groups["count2"]?.value?.length ?: 0

    return if (startMarker == endMarker && (startMarker == 1 || startMarker == 2)) {
        val innerText = match.groups["string"]?.value ?: return null
        val (attrs, text) = parseInlineAttributes(innerText)
        ParseLineResult(
            InlineItem.Subscript(parseInline(listOf(text))),
            match.groupValues.first().length
        )
    } else {
        ParseLineResult(
            InlineItem.Text(match.groupValues.first()),
            match.groupValues.first().length
        )
    }
}



