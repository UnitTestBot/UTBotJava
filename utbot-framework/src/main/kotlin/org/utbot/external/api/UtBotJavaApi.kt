package org.utbot.external.api

import org.utbot.common.FileUtil
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.concrete.UtConcreteExecutionData
import org.utbot.framework.concrete.UtConcreteExecutionResult
import org.utbot.framework.concrete.UtExecutionInstrumentation
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TestCaseGenerator
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.primitiveByWrapper
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.plugin.api.util.wrapperByPrimitive
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.execute
import kotlin.reflect.jvm.kotlinFunction

object UtBotJavaApi {

    @JvmStatic
    var stopConcreteExecutorOnExit: Boolean = true

    @JvmStatic
    @JvmOverloads
    fun generate(
        methodsForGeneration: List<TestMethodInfo>,
        generatedTestCases: List<UtMethodTestSet> = mutableListOf(),
        destinationClassName: String,
        classpath: String,
        dependencyClassPath: String,
        classUnderTest: Class<*>,
        testFramework: TestFramework = Junit5,
        mockFramework: MockFramework = MockFramework.MOCKITO,
        codegenLanguage: CodegenLanguage = CodegenLanguage.JAVA,
        staticsMocking: StaticsMocking = NoStaticMocking,
        generateWarningsForStaticMocking: Boolean = false,
        forceStaticMocking: ForceStaticMocking = ForceStaticMocking.DO_NOT_FORCE,
        testClassPackageName: String = classUnderTest.packageName
    ): String {

        val utContext = UtContext(classUnderTest.classLoader)

        val testSets: MutableList<UtMethodTestSet> = generatedTestCases.toMutableList()

        val concreteExecutor = ConcreteExecutor(
            UtExecutionInstrumentation,
            classpath,
            dependencyClassPath
        )

        testSets.addAll(generateUnitTests(concreteExecutor, methodsForGeneration, classUnderTest))

        if (stopConcreteExecutorOnExit) {
            concreteExecutor.close()
        }

        return withUtContext(utContext) {
            val codeGenerator = CodeGenerator(
                    classUnderTest = classUnderTest.id,
                    testFramework = testFramework,
                    mockFramework = mockFramework,
                    codegenLanguage = codegenLanguage,
                    staticsMocking = staticsMocking,
                    forceStaticMocking = forceStaticMocking,
                    generateWarningsForStaticMocking = generateWarningsForStaticMocking,
                    testClassPackageName = testClassPackageName
                )

            codeGenerator.generateAsString(testSets, destinationClassName)
        }
    }

    /**
     * Generates test sets using default workflow.
     *
     * @see [fuzzingTestSets]
     */
    @JvmStatic
    @JvmOverloads
    fun generateTestSets(
        methodsForAutomaticGeneration: List<TestMethodInfo>,
        classUnderTest: Class<*>,
        classpath: String,
        dependencyClassPath: String,
        mockStrategyApi: MockStrategyApi = MockStrategyApi.OTHER_PACKAGES,
        generationTimeoutInMillis: Long = UtSettings.utBotGenerationTimeoutInMillis
    ): MutableList<UtMethodTestSet> {

        val utContext = UtContext(classUnderTest.classLoader)
        val testSets: MutableList<UtMethodTestSet> = mutableListOf()

        testSets.addAll(withUtContext(utContext) {
            val buildPath = FileUtil.isolateClassFiles(classUnderTest).toPath()
            TestCaseGenerator(buildPath, classpath, dependencyClassPath, jdkInfo = JdkInfoDefaultProvider().info)
                .generate(
                    methodsForAutomaticGeneration.map {
                        it.methodToBeTestedFromUserInput.executableId
                    },
                    mockStrategyApi,
                    chosenClassesToMockAlways = emptySet(),
                    generationTimeoutInMillis
                )
        })

        return testSets
    }

    /**
     * Generates test cases using only fuzzing workflow.
     *
     * @see [generateTestSets]
     */
    @JvmStatic
    @JvmOverloads
    fun fuzzingTestSets(
        methodsForAutomaticGeneration: List<TestMethodInfo>,
        classUnderTest: Class<*>,
        classpath: String,
        dependencyClassPath: String,
        mockStrategyApi: MockStrategyApi = MockStrategyApi.OTHER_PACKAGES,
        generationTimeoutInMillis: Long = UtSettings.utBotGenerationTimeoutInMillis,
        primitiveValuesSupplier: CustomFuzzerValueSupplier = CustomFuzzerValueSupplier { null }
    ): MutableList<UtMethodTestSet> {
        fun createPrimitiveModels(supplier: CustomFuzzerValueSupplier, classId: ClassId): Sequence<UtPrimitiveModel> =
            supplier
                .takeIf { classId.isPrimitive || classId.isPrimitiveWrapper || classId == stringClassId }
                ?.get(classId.jClass)
                ?.asSequence()
                ?.filter {
                    val valueClassId = it.javaClass.id
                    when {
                        classId == valueClassId -> true
                        classId.isPrimitive -> wrapperByPrimitive[classId] == valueClassId
                        classId.isPrimitiveWrapper -> primitiveByWrapper[classId] == valueClassId
                        else -> false
                    }
                }
                ?.map { UtPrimitiveModel(it) } ?: emptySequence()

        val customModelProvider = ModelProvider { description ->
            sequence {
                description.parametersMap.forEach { (classId, indices) ->
                    createPrimitiveModels(primitiveValuesSupplier, classId).forEach { model ->
                        indices.forEach { index ->
                            yieldValue(index, FuzzedValue(model))
                        }
                    }
                }
            }
        }

        return withUtContext(UtContext(classUnderTest.classLoader)) {
            val buildPath = FileUtil.isolateClassFiles(classUnderTest).toPath()
            TestCaseGenerator(buildPath, classpath, dependencyClassPath, jdkInfo = JdkInfoDefaultProvider().info)
                .generate(
                    methodsForAutomaticGeneration.map {
                        it.methodToBeTestedFromUserInput.executableId
                    },
                    mockStrategyApi,
                    chosenClassesToMockAlways = emptySet(),
                    generationTimeoutInMillis,
                    generate = { symbolicEngine ->
                        symbolicEngine.fuzzing { defaultModelProvider ->
                            customModelProvider.withFallback(defaultModelProvider)
                        }
                    }
                )
        }.toMutableList()
    }

    private fun generateUnitTests(
        concreteExecutor: ConcreteExecutor<UtConcreteExecutionResult, UtExecutionInstrumentation>,
        testMethods: List<TestMethodInfo>,
        containingClass: Class<*>
    ) = testMethods.map { testInfo ->

        val methodTobeTested = testInfo.methodToBeTestedFromUserInput

        if (containingClass !== methodTobeTested.declaringClass) {
            throw IllegalArgumentException(
                "Method ${methodTobeTested.name}" +
                        " is not in the class ${containingClass.canonicalName} it is " +
                        " in ${methodTobeTested.declaringClass.canonicalName}"
            )
        }

        val methodCallable = methodTobeTested?.kotlinFunction

        if (methodCallable === null) {
            throw IllegalArgumentException(
                "Method " + methodTobeTested.name +
                        " failed to be converted in kotlin function"
            )
        }

        val utExecutionResult = if (testInfo.utResult == null) {
            concreteExecutor.execute(
                methodCallable,
                arrayOf(),
                parameters = UtConcreteExecutionData(
                    testInfo.initialState,
                    instrumentation = emptyList()
                )
            ).result
        } else {
            testInfo.utResult
        }

        val utExecution = UtSymbolicExecution(
            stateBefore = testInfo.initialState,
            stateAfter = testInfo.initialState, // it seems ok for concrete execution
            result = utExecutionResult,
            instrumentation = emptyList(),
            path = mutableListOf(),
            fullPath = listOf()
        )

        UtMethodTestSet(
            methodCallable.executableId,
            listOf(utExecution)
        )
    }.toList()
}

/**
 * Accepts type of parameter and returns collection of values for this type.
 *
 * Value types which can be generated:
 *
 *  - primitive types: boolean, char, byte, short, int, long, float, double
 *  - primitive wrappers: Boolean, Character, Byte, Short, Integer, Long, Float, Double
 *  - String
 *
 *  If null is returned instead of empty collection then default fuzzer values are produced.
 */
fun interface CustomFuzzerValueSupplier {
    fun get(type: Class<*>): Collection<Any>?
}