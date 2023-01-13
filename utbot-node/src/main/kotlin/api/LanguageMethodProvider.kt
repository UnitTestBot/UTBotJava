package api

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntSupplier
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution


abstract class LanguageMethodProvider(
    private val dataProvider: LanguageDataProvider
): LanguageDataOwner by dataProvider {

    fun getClassMethods(className: String) =
        fileEntity.classes.find { it.name == className }?.methods
            ?: throw IllegalStateException()

    fun extractToplevelFunctions() = fileEntity.topLevelFunctions

    fun getFunctionEntity(functionName: String, className: String?) = run {
        className?.let { classNameNotNull ->
            getClassMethods(classNameNotNull).find { it.name == functionName }
        } ?: fileEntity.topLevelFunctions.find { it.name == functionName }
    } ?: throw IllegalStateException()

    abstract fun runFuzzer(functionEntity: AbstractFunctionEntity, execId: MethodId):
            List<List<FuzzedValue>>

    fun getMethodsToTest() =
        parentClassName?.let {
            getClassMethods(it)
        } ?: extractToplevelFunctions().ifEmpty {
            getClassMethods("")
        }

    private fun makeMethodsToTest(): List<AbstractFunctionEntity> =
        selectedMethods?.map {
            getFunctionEntity(
                functionName = it,
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

    abstract fun getUtModelResult(
        execId: MethodId,
        returnText: String
    ): UtExecutionResult

    private class SimpleIdGenerator : IntSupplier {
        private val id = AtomicInteger()
        override fun getAsInt() = id.incrementAndGet()
    }

    fun makeThisInstance(
        execId: MethodId,
        classId: ClassId,
        concreteValues: Set<FuzzedConcreteValue>,
    ): UtModel? {
        val thisInstance = when {
            execId.isStatic -> null
            classId.allConstructors.first().parameters.isEmpty() -> {
                val id = SimpleIdGenerator().asInt
                val constructor = classId.allConstructors.first()
                val instantiationCall = UtExecutableCallModel(
                    instance = null,
                    executable = constructor,
                    params = emptyList(),
                )
                UtAssembleModel(
                    id = id,
                    classId = constructor.classId,
                    modelName = "${constructor.classId.name}${constructor.parameters}#" + id.toString(16),
                    instantiationCall = instantiationCall,
                )
            }
            else -> buildThisInstance(classId, concreteValues)
        }
        return thisInstance
    }

    abstract fun buildThisInstance(classId: ClassId, concreteValues: Set<FuzzedConcreteValue>): UtModel

    private fun makeTestsForMethod(
        coverageServiceProvider: AbstractCoverageServiceProvider,
        classId: ClassId,
        funcNode: AbstractFunctionEntity,
        testSets: MutableList<CgMethodTestSet>,
        paramNames: MutableMap<ExecutableId, List<String>>,
    ) {
        val execId = classId.allMethods.find {
            it.name == funcNode.name
        } ?: throw IllegalStateException()
        val fuzzedValues = runFuzzer(funcNode, execId)
        val (allCoveredStatements, executionResults) =
            coverageServiceProvider.get()
        val testsForGenerator = mutableListOf<UtExecution>()
        val errorsForGenerator = mutableMapOf<String, Int>()
        executionResults.forEachIndexed { index, value ->
            if (value == "Error:Timeout") {
                errorsForGenerator["Timeout in generating test for ${
                    fuzzedValues[index]
                        .joinToString { f -> f.model.toString() }
                } parameters"] = 1
            }
        }

        analyzeCoverage(allCoveredStatements).forEach { paramIndex ->
            val param = fuzzedValues[paramIndex]
            val result =
                getUtModelResult(
                    execId = execId,
                    returnText = executionResults[paramIndex]
                )
            val initEnv = buildInitEnv(execId, classId, funcNode, param)
            testsForGenerator.add(
                UtFuzzedExecution(
                    stateBefore = initEnv,
                    stateAfter = initEnv,
                    result = result,
                )
            )
        }
        val testSet = CgMethodTestSet(
            execId,
            testsForGenerator,
            errorsForGenerator
        )
        testSets += testSet
        paramNames[execId] = funcNode.parametersMap.keys.toList()
    }

    abstract fun buildInitEnv(execId: MethodId, classId: ClassId, functionEntity: AbstractFunctionEntity, param: List<FuzzedValue>): EnvironmentModels
}