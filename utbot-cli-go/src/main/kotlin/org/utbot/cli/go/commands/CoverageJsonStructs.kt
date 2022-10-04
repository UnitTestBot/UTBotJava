package org.utbot.cli.go.commands

import com.beust.klaxon.Json

internal data class Position(@Json(index = 1) val line: Int, @Json(index = 2) val column: Int)

internal data class CodeRegion(@Json(index = 1) val start: Position, @Json(index = 2) val end: Position)

internal data class CoveredSourceFile(
    @Json(index = 1) val sourceFileName: String,
    @Json(index = 2) val covered: List<CodeRegion>,
    @Json(index = 3) val uncovered: List<CodeRegion>
)