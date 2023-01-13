package api

abstract class AbstractCoverageServiceProvider(
    private val dataProvider: LanguageDataProvider
): LanguageDataOwner by dataProvider {

    fun get(): Pair<List<Set<Int>>, List<String>> =
        when (settings.coverageMode) {
            NodeCoverageMode.FAST -> runFastCoverageAnalysis()
            NodeCoverageMode.BASIC -> runBasicCoverageAnalysis()
        }

    abstract fun runFastCoverageAnalysis(): Pair<List<Set<Int>>, List<String>>

    abstract fun runBasicCoverageAnalysis(): Pair<List<Set<Int>>, List<String>>
}