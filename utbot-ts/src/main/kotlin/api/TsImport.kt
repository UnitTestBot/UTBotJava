package api

import java.nio.file.Path

data class TsImport(
    val alias: String,
    val path: Path,
)
