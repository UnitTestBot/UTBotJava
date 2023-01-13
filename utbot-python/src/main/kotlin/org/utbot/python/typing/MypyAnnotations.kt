package org.utbot.python.typing

object MypyAnnotations {
    const val TEMPORARY_MYPY_FILE = "<TEMPORARY MYPY FILE>"

    data class MypyReportLine(
        val line: Int,
        val type: String,
        val message: String,
        val file: String
    )

}

