package es.cristcd.taskcompanion.parser

import kotlin.reflect.KClass

sealed interface Block {
    val attributes: List<BlockAttribute>

    data class Heading(val items: List<InlineItem>, override val attributes: List<BlockAttribute> = emptyList(), val level: Int): Block
    data class Paragraph(val items: List<InlineItem>, override val attributes: List<BlockAttribute> = emptyList(), val explicitMarker: Boolean): Block
    data class Pre(val lines: List<String>, override val attributes: List<BlockAttribute> = emptyList()): Block
    data class BlockCode(val lines: List<String>, override val attributes: List<BlockAttribute> = emptyList()): Block
    data class BlockQuote(val items: List<Block>, override val attributes: List<BlockAttribute> = emptyList(), val cite: String? = null): Block
    data class Comment(val lines: List<String>, override val attributes: List<BlockAttribute> = emptyList()): Block
    data class NoTextile(val lines: List<String>, override val attributes: List<BlockAttribute> = emptyList()): Block
    sealed class ListBlock(override val attributes: List<BlockAttribute>) : Block {
        abstract val items: List<ListItem>

        data class UnorderedList(override val items: List<ListItem>, override val attributes: List<BlockAttribute> = emptyList()): ListBlock(attributes)
        data class OrderedList(override val items: List<ListItem>, override val attributes: List<BlockAttribute> = emptyList()): ListBlock(attributes)
    }
}

data class ListItem(val value: String, val attributes: List<String> = emptyList(), val level: Int, val children: Block.ListBlock? = null)

sealed interface BlockAttribute {
    data class Padding(val left: Int = 0, val right: Int = 0): BlockAttribute
    enum class Alignment : BlockAttribute {
        LEFT,
        RIGHT,
        JUSTIFY,
        CENTER
    }
}

data class ParseBlockResult(val block: Block, val consumedLines: Int)

fun parseBlocks(lines: List<String>): List<Block> {
    val blocks = mutableListOf<Block>()
    var currentLine = 0
    while (currentLine < lines.size) {
        parseBlock(lines.drop(currentLine))?.let { (block, consumedLines) ->
            blocks.add(block)
            currentLine += consumedLines
        }
    }
    return blocks
}

private fun parseBlock(lines: List<String>): ParseBlockResult? {
    val nonBlankLines = lines.dropWhile { it.isBlank() }
    if (nonBlankLines.isEmpty()) {
        return null
    }
    val linesDropped = lines.size - nonBlankLines.size

    val heading = parseHeading(nonBlankLines)
    if (heading != null) {
        return heading.copy(consumedLines = heading.consumedLines + linesDropped)
    }
    val pre = parsePre(nonBlankLines)
    if (pre != null) {
        return pre.copy(consumedLines = pre.consumedLines + linesDropped)
    }
    val blockCode = parseBlockCode(nonBlankLines)
    if (blockCode != null) {
        return blockCode.copy(consumedLines = blockCode.consumedLines + linesDropped)
    }
    val blockQuote = parseBlockQuote(nonBlankLines)
    if (blockQuote != null) {
        return blockQuote.copy(consumedLines = blockQuote.consumedLines + linesDropped)
    }
    val comment = parseComment(nonBlankLines)
    if (comment != null) {
        return comment.copy(consumedLines = comment.consumedLines + linesDropped)
    }
    val noTextile = parseNoTextile(nonBlankLines)
    if (noTextile != null) {
        return noTextile.copy(consumedLines = noTextile.consumedLines + linesDropped)
    }
    val list = parseList(nonBlankLines)
    if (list != null) {
        return list.copy(consumedLines = list.consumedLines + linesDropped)
    }

    //Always last
    val paragraph = parseParagraph(nonBlankLines)
    if (paragraph != null) {
        return paragraph.copy(consumedLines = paragraph.consumedLines + linesDropped)
    }
    return null
}

private fun parseHeading(lines: List<String>): ParseBlockResult? {
    val match = Patterns.HEADING_PATTERN.matchAt(lines.first(), 0) ?: return null

    val level = match.groups["level"]?.value!!.toInt()
    val attributes = match.groups["attributes"]?.value

    val firstLine = lines.first().slice(match.groupValues.first().length until lines.first().length)
    val heading = listOf(firstLine) + lines.drop(1).takeWhile { it.isNotBlank() }

    return ParseBlockResult(
        Block.Heading(parseInline(heading), parseBlockAttributes(attributes), level),
        heading.size
    )
}

private fun parseParagraph(lines: List<String>): ParseBlockResult? {
    if (lines.firstOrNull()?.isBlank() == null || lines.firstOrNull()?.isBlank() == true) {
        return null
    }
    val match = Patterns.PARAGRAPH_PATTERN.matchAt(lines.first(), 0) ?: return null

    val explicitMarker = match.groupValues.firstOrNull() ?: ""
    val attributes = match.groups["attributes"]?.value

    val firstLine = lines.first().slice((explicitMarker?.length ?: 0) until lines.first().length)
    val paragraph = listOf(firstLine) + lines.drop(1).takeWhile { it.isNotBlank() }
    return ParseBlockResult(
        Block.Paragraph(parseInline(paragraph), parseBlockAttributes(attributes), explicitMarker.isNotEmpty()),
        paragraph.size
    )
}

private fun parsePre(lines: List<String>): ParseBlockResult? {
    val match = Patterns.PRE_PATTERN.matchAt(lines.first(), 0) ?: return null

    val mode = match.groups["mode"]?.value?.length ?: 0
    val attributes = match.groups["attributes"]?.value

    val firstLine = lines.first().slice(match.groupValues.first().length until lines.first().length)
    val pre = listOf(firstLine) + lines.drop(1).takeWhile { it.validInMultilineBlock(mode) }

    return ParseBlockResult(
        Block.Pre(pre, parseBlockAttributes(attributes)),
        pre.size
    )
}

private fun parseBlockCode(lines: List<String>): ParseBlockResult? {
    val match = Patterns.CODE_BLOCK_PATTERN.matchAt(lines.first(), 0) ?: return null

    val mode = match.groups["mode"]?.value?.length ?: 0
    val attributes = match.groups["attributes"]?.value

    val firstLine = lines.first().slice(match.groupValues.first().length until lines.first().length)
    val blockCode = listOf(firstLine) + lines.drop(1).takeWhile { it.validInMultilineBlock(mode) }

    return ParseBlockResult(
        Block.BlockCode(blockCode, parseBlockAttributes(attributes)),
        blockCode.size
    )
}

private fun parseBlockQuote(lines: List<String>): ParseBlockResult? {
    val match = Patterns.BLOCK_QUOTATION_PATTERN.matchAt(lines.first(), 0) ?: return null

    val mode = match.groups["mode"]?.value?.length ?: 0
    val attributes = match.groups["attributes"]?.value
    val cite = match.groups["cite"]?.value

    val firstLine = lines.first().slice(match.groupValues.first().length until lines.first().length)
    val blockQuoteLines = listOf(firstLine) + lines.drop(1).takeWhile { it.validInMultilineBlock(mode) }
    var linePosition = 0
    val blocks = mutableListOf<Block>()
    while (linePosition < blockQuoteLines.size) {
        if (blockQuoteLines[linePosition].isBlank()) {
            linePosition++
        }
        val nestedParagraph = parseParagraph(blockQuoteLines.drop(linePosition))
        if (nestedParagraph != null) {
            linePosition += nestedParagraph.consumedLines
            blocks.add(nestedParagraph.block)
        }
    }

    return ParseBlockResult(
        Block.BlockQuote(blocks, parseBlockAttributes(attributes), cite),
        linePosition
    )
}

private fun parseComment(lines: List<String>): ParseBlockResult? {
    val match = Patterns.COMMENT_PATTERN.matchAt(lines.first(), 0) ?: return null

    val mode = match.groups["mode"]?.value?.length ?: 0
    val attributes = match.groups["attributes"]?.value

    val firstLine = lines.first().slice(match.groupValues.first().length until lines.first().length)
    val comment = listOf(firstLine) + lines.drop(1).takeWhile { it.validInMultilineBlock(mode) }

    return ParseBlockResult(
        Block.Comment(comment, parseBlockAttributes(attributes)),
        comment.size
    )
}

private fun parseNoTextile(lines: List<String>): ParseBlockResult? {
    val match = Patterns.NO_TEXTILE_BLOCK_PATTERN.matchAt(lines.first(), 0) ?: return null

    val mode = match.groups["mode"]?.value?.length ?: 0

    val firstLine = lines.first().slice(match.groupValues.first().length until lines.first().length)
    val noTextile = listOf(firstLine) + lines.drop(1).takeWhile { it.validInMultilineBlock(mode) }

    return ParseBlockResult(
        Block.NoTextile(noTextile, emptyList()),
        noTextile.size
    )
}

private fun parseList(lines: List<String>, listLevel: Int = 1): ParseBlockResult? {
    val ordered = parseOrderedList(lines, listLevel)
    if (ordered != null) {
        return ordered
    }
    val unordered = parseUnorderedList(lines, listLevel)
    if (unordered != null) {
        return unordered
    }

    return null
}

private fun parseOrderedList(lines: List<String>, listLevel: Int): ParseBlockResult? {
    val match = Patterns.ORDERED_LIST_PATTERN.matchAt(lines.first(), 0) ?: return null

    val items = mutableListOf<ListItem>()
    var linePosition = 0
    while (linePosition < lines.size) {
        val line = lines[linePosition]
        val currentMatch = Patterns.ORDERED_LIST_PATTERN.matchAt(line, 0) ?: error("Error inner line after passing prior match")
        val currentLevel = currentMatch.groups["level"]?.value?.length ?: 0
        val lineValue = line.slice(currentMatch.groupValues.first().length until line.length)

        if (currentLevel < listLevel) {
            break
        }

        val nextLevel = lines.getOrNull(linePosition + 1)?.let { checkListItemLevel(it) }
        if (nextLevel?.level == null || nextLevel.level < listLevel || (nextLevel.type == Block.ListBlock.UnorderedList::class && nextLevel.level <= listLevel)) {
            items.add(ListItem(lineValue, level = listLevel))
            linePosition++
            break
        } else if (nextLevel.level == listLevel) {
            items.add(ListItem(lineValue, level = listLevel))
            linePosition++
        } else {
            val listBlock = parseList(lines.drop(linePosition + 1), nextLevel.level)
            items.add(ListItem(lineValue, level = listLevel, children = (listBlock!!.block as Block.ListBlock)))
            linePosition += listBlock.consumedLines
        }
    }

    return ParseBlockResult(
        Block.ListBlock.OrderedList(items, emptyList()),
        linePosition + 1
    )

}

private fun parseUnorderedList(lines: List<String>, listLevel: Int): ParseBlockResult? {
    val match = Patterns.UNORDERED_LIST_PATTERN.matchAt(lines.first(), 0) ?: return null

    val items = mutableListOf<ListItem>()
    var linePosition = 0
    while (linePosition < lines.size) {
        val line = lines[linePosition]
        val currentMatch = Patterns.UNORDERED_LIST_PATTERN.matchAt(line, 0) ?: error("Error inner line after passing prior match")
        val currentLevel = currentMatch.groups["level"]?.value?.length ?: 0
        val lineValue = line.slice(currentMatch.groupValues.first().length until line.length)

        if (currentLevel < listLevel) {
            break
        }

        val nextLevel = lines.getOrNull(linePosition + 1)?.let { checkListItemLevel(it) }
        if (nextLevel?.level == null || nextLevel.level < listLevel || (nextLevel.type == Block.ListBlock.OrderedList::class && nextLevel.level <= listLevel)) {
            items.add(ListItem(lineValue, level = listLevel))
            linePosition++
            break
        } else if (nextLevel.level == listLevel) {
            items.add(ListItem(lineValue, level = listLevel))
            linePosition++
        } else {
            val listBlock = parseList(lines.drop(linePosition + 1), nextLevel.level)
            items.add(ListItem(lineValue, level = listLevel, children = (listBlock!!.block as Block.ListBlock)))
            linePosition += listBlock.consumedLines
        }
    }

    return ParseBlockResult(
        Block.ListBlock.UnorderedList(items, emptyList()),
        linePosition + 1
    )
}

data class ListLevelInfo(val level: Int?, val type: KClass<out Block.ListBlock>)
private fun checkListItemLevel(line: String): ListLevelInfo? {
    val matchOrdered = Patterns.ORDERED_LIST_PATTERN.matchAt(line, 0)
    if (matchOrdered != null) {
        return ListLevelInfo(matchOrdered.groups["level"]?.value?.length, Block.ListBlock.OrderedList::class)
    }
    val matchUnordered = Patterns.UNORDERED_LIST_PATTERN.matchAt(line, 0)
    if (matchUnordered != null) {
        return ListLevelInfo(matchUnordered.groups["level"]?.value?.length, Block.ListBlock.UnorderedList::class)
    }
    return null
}

private fun String.validInMultilineBlock(mode: Int): Boolean {
    return if (mode == 1) {
        isNotBlank()
    } else { //Extended block allows blank lines. Breaks when another block is found
        when (val nestedBlock = parseBlock(listOf(this))?.block) {
            is Block.Paragraph -> !nestedBlock.explicitMarker
            null -> true
            else -> false
        }
    }
}

private fun parseBlockAttributes(attributeString: String?): List<BlockAttribute> {
    if (attributeString.isNullOrBlank()) {
        return emptyList()
    }

    //TODO: lang, classid and css
    val attrs = mutableListOf<BlockAttribute>()
    val matchPadding = Patterns.PADDING_PATTERN.matchAt(attributeString, 0)
    if (matchPadding != null) {
        val padding = matchPadding.value
        if (padding.startsWith("(")) {
            attrs.add(BlockAttribute.Padding(left = padding.length))
        } else {
            attrs.add(BlockAttribute.Padding(right = padding.length))
        }
    }

    val matchAlign = Patterns.ALIGN_PATTERN.matchAt(attributeString, 0)
    if (matchAlign != null) {
        val align = when(matchAlign.value) {
            ">" -> BlockAttribute.Alignment.RIGHT
            "<>" -> BlockAttribute.Alignment.JUSTIFY
            "=" -> BlockAttribute.Alignment.CENTER
            else -> BlockAttribute.Alignment.LEFT
        }

        attrs.add(align)
    }

    return attrs
}