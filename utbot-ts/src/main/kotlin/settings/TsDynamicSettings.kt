package settings

import service.TsCoverageMode

data class TsDynamicSettings (
    val pathToNYC: String = "nyc",
    val timeout: Long = 15L,
    val coverageMode: TsCoverageMode = TsCoverageMode.FAST,
    val tsNycModulePath: String,
    val tsNodePath: String = "ts-node",
    val tsModulePath: String,
    val godObject: String? = null,
)
