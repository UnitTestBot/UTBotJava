package settings

import api.NodeCoverageMode
import api.NodeDynamicSettings

class JsDynamicSettings(
    val pathToNode: String = "node",
    override val pathToNYC: String = "nyc",
    val pathToNPM: String = "npm",
    override val timeout: Long = 15L,
    override val coverageMode: NodeCoverageMode = NodeCoverageMode.FAST
): NodeDynamicSettings(pathToNYC, timeout, coverageMode)
