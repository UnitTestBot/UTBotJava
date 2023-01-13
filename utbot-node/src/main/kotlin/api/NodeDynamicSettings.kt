package api

open class NodeDynamicSettings(
    open val pathToNYC: String = "nyc",
    open val timeout: Long = 15L,
    open val coverageMode: NodeCoverageMode = NodeCoverageMode.FAST,
)