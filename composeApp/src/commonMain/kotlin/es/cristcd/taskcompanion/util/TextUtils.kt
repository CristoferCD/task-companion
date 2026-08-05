package es.cristcd.taskcompanion.util

//UI freezes when rendering a Text with more than 500k chars
fun String.trimToComposeLength() = if (isNullOrBlank()) this else trim().substring(0, minOf(trim().length - 1, 500000))