package settings

import service.coverage.CoverageMode

data class JsDynamicSettings(
    val pathToNode: String = "node",
    val pathToNYC: String = "nyc",
    val pathToNPM: String = "npm",
    val timeout: Long = 15L,
    val coverageMode: CoverageMode = CoverageMode.FAST
)
