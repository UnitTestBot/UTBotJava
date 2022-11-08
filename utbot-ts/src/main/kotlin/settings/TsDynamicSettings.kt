package settings

import service.TsCoverageMode

data class TsDynamicSettings (
    val pathToNode: String = "node",
    val pathToNYC: String = "nyc",
    val timeout: Long = 15L,
    val coverageMode: TsCoverageMode = TsCoverageMode.FAST,
    val tsNycPath: String = "",
    val tsNodePath: String = "",
)