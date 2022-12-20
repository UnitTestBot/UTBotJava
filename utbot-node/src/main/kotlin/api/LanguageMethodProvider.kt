package api

import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedValue


abstract class LanguageMethodProvider(
    private val dataProvider: LanguageDataProvider
): LanguageDataOwner by dataProvider {

    abstract fun getClassMethods(className: String): List<AbstractFunctionEntity>

    abstract fun extractToplevelFunctions(): List<AbstractFunctionEntity>

    abstract fun getFunctionEntity(name: String, className: String?): AbstractFunctionEntity

    abstract fun runFuzzer(dataProvider: LanguageDataProvider):
            Pair<Set<FuzzedConcreteValue>, List<List<FuzzedValue>>>

    fun getMethodsToTest() =
        parentClassName?.let {
            getClassMethods(it)
        } ?: extractToplevelFunctions().ifEmpty {
            getClassMethods("")
        }

    private fun makeMethodsToTest(): List<AbstractFunctionEntity> =
        selectedMethods?.map {
            getFunctionEntity(
                name = it,
                className = parentClassName,
            )
        } ?: getMethodsToTest()

    fun analyzeCoverage(coverageList: List<Set<Int>>): List<Int> {
        val allCoveredBranches = mutableSetOf<Int>()
        val resultList = mutableListOf<Int>()
        coverageList.forEachIndexed { index, it ->
            if (!allCoveredBranches.containsAll(it)) {
                resultList += index
                allCoveredBranches.addAll(it)
            }
        }
        return resultList
    }
}