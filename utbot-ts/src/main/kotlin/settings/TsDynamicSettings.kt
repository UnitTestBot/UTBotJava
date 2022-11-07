package settings

import service.CoverageMode

data class TsDynamicSettings (
    val pathToNode: String = "node",
    val pathToNYC: String = "nyc",
    val timeout: Long = 15L,
    val coverageMode: CoverageMode = CoverageMode.FAST,
)