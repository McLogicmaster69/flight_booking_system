package util

fun String.singleLine(): String =
    lines()
        .joinToString(" ") { it.trim() }
        .replace(Regex("\\s+"), " ")
        .trim()
