package org.utbot.visual

import org.utbot.common.dateTimeFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


abstract class AbstractHtmlReport(bodyWidth: Int = 600) {
    val builder = HtmlBuilder(bodyMaxWidth = bodyWidth)

    private fun nameWithDate() =
        "logs/Report_" + dateTimeFormatter.format(LocalDateTime.now()) + ".html"

    fun save(filename: String = nameWithDate()) {
        builder.saveHtml(filename)
    }
}

