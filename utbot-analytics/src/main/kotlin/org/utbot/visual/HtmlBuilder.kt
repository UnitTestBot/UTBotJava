package org.utbot.visual

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import tech.tablesaw.plotly.components.Figure

class HtmlBuilder(bodyMaxWidth: Int = 600,
                  pathToStyle: List<String> = listOf(),
                  pathToJs: List<String> = listOf()) {
    private val pageTop = ("<html>"
            + System.lineSeparator()
            + "<head>"
            + System.lineSeparator()
            + "    <title>Multi-plot test</title>"
            + System.lineSeparator()
            + "    <script src=\"https://cdn.plot.ly/plotly-latest.min.js\"></script>"
            + "<style type=\"text/css\">"
            + pathToStyle.joinToString("") { "@import \"$it\";\n" }
            + "</style>"
            + pathToJs.joinToString("") { "<script type=\"text/javascript\" src=\"$it\"></script>" }
            + System.lineSeparator()
            + "</head>"
            + System.lineSeparator()
            + "<body style ='max-width: 600px; margin: auto;'>"
            + System.lineSeparator())

    private val pageBottom = "</body>" + System.lineSeparator() + "</html>"

    private var pageBuilder = StringBuilder(pageTop).append(System.lineSeparator())
    private var figres_num = 0

    fun addFigure(figure: Figure) {
        figure.asJavascript("plot${this.figres_num}")
        pageBuilder.append("<div id='plot${this.figres_num}'>").append(System.lineSeparator())
            .append(figure.asJavascript("plot${this.figres_num}")).append(System.lineSeparator()).append("</div>")
        this.figres_num += 1
    }

    fun saveHTML(fileName: String = "Report.html") {
        File(fileName).writeText(pageBuilder.toString() + pageBottom)
    }

    fun addHeader(ModelName: String, properties: Properties) {
        val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
        pageBuilder.append("<h1>Model : ${ModelName}</h1>").append("<h2>$currentDateTime</h2>")
            .append("<h3>Hyperparameters:</h3>")
        for (property in properties) {
            pageBuilder.append("<div> ${property.key} : ${property.value} </div>").append(System.lineSeparator())
        }

    }

    fun addHeader(header: String, h: String = "h1") {
        pageBuilder.append("<$h>${header}</$h>")
    }

    fun addTable(statistics: List<Map<String, Double>>,
                 selectorNames: List<String>,
                 colors: List<String>,
                 scope: String) {
        pageBuilder.append("<table class=\"coverageStats\">")
        pageBuilder.append("<tr> <th class=\"name\">$scope</th>\n")

        selectorNames.forEach { pageBuilder.append("<th class=\"coverageStat\">${it}</th>\n") }
        pageBuilder.append("</tr>")

        statistics.first().keys.forEach { key ->
            pageBuilder.append("<tr><td class=\"name\"><a href=\"${key}/index.html\">${key}</a></td>\n")

            for (i in statistics.indices) {
                pageBuilder.append(
                    "<td class=\"coverageStat\">\n" +
                        "<span class=\"percent\">\n" +
                            "<p style=\"color:${colors[i]};\">${String.format("%.0f", statistics[i][key])}</p>\n" +
                        "</span>\n" +
                    "</td>\n"
                )
            }
            pageBuilder.append("</tr>")
        }
        pageBuilder.append("</table>")
    }

    fun addText(text: String) {
        pageBuilder.append("<div>$text</div>")
    }

}
