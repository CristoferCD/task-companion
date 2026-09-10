package es.cristcd.taskcompanion.util

import java.text.Normalizer

//UI freezes when rendering a Text with more than 500k chars
fun String.trimToComposeLength() = if (isNullOrBlank()) this else trim().substring(0, minOf(trim().length, 500000))

fun String.unaccent() = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
