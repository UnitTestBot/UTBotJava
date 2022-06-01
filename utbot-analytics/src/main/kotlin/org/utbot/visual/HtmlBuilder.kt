package org.utbot.visual

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class HtmlBuilder(bodyMaxWidth: Int = 600) {
    private val pageTop = ("<html>"
            + System.lineSeparator()
            + "<head>"
            + System.lineSeparator()
            + "    <title>Multi-plot test</title>"
            + System.lineSeparator()
            + "    <script src=\"https://cdn.plot.ly/plotly-latest.min.js\"></script>"
            + System.lineSeparator()
            + "<style>"
            + System.lineSeparator()
            + "table, th, td {"
            + "  border: 1px solid black;"
            + "  border-collapse: collapse;"
            + "}"
            + System.lineSeparator()
            + "th, td {"
            + "  padding: 5px;"
            + "}"
            + System.lineSeparator()
            + "</style>"
            + System.lineSeparator()
            + "</head>"
            + System.lineSeparator()
            + "<body style ='max-width: " + bodyMaxWidth + "px; margin: auto;'>"
            + System.lineSeparator())

    private val pageBottom = "</body>" + System.lineSeparator() + "</html>"

    private var pageBuilder = StringBuilder(pageTop).appendLine()

    fun saveHtml(fileName: String = "Report.html") {
        File(fileName).writeText(pageBuilder.toString() + pageBottom)
    }

    fun addText(text: String) {
        pageBuilder.append("<div>$text</div>")
    }

    fun addRawHTML(HTMLCode: String) {
        pageBuilder.append(HTMLCode)
    }

    fun addBreak() {
        addText("<div style=\"height:50px;\"> </div>")
    }

    fun addHeader1(header: String){
        addText("<h1>$header</h1>")
    }

    fun addHeader2(header: String){
        addText("<h2>$header</h2>")
    }

    fun addHeader3(header: String){
        addText("<h3>$header</h3>")
    }

}
