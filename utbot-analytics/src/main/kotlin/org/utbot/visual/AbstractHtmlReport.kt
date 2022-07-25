package org.utbot.visual

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


abstract class AbstractHtmlReport(bodyWidth: Int = 600) {
    val builder = HtmlBuilder(bodyMaxWidth = bodyWidth)

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss")

    private fun nameWithDate() =
        "logs/Report_" + dateTimeFormatter.format(LocalDateTime.now()) + ".html"

    fun save(filename: String = nameWithDate()) {
        builder.saveHTML(filename)
    }
}

