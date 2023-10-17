package org.utbot.external.api

import org.utbot.common.FileUtil
import org.utbot.common.nameOfPackage
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.*
import org.utbot.framework.codegen.generator.CodeGeneratorParams
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.framework.context.ApplicationContext
import org.utbot.framework.context.utils.transformValueProvider
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionData
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.UtExecutionInstrumentation
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
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Seed
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.execute
import kotlin.reflect.jvm.kotlinFunction
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.plugin.api.*
import java.lang.reflect.Method

object UtBotJavaApi {

    /**
     * For running tests it could be reasonable to reuse the same concrete executor
     */
    @JvmStatic
    var stopConcreteExecutorOnExit: Boolean = true

    /**
     * Generates test code
     * @param methodsForGeneration specify methods that are supposed to be executed concretely.
     *                             In order to execute method you are supposed to provide some
     *                             values to pass in it this is why we use [TestMethodInfo] here.
     * @param generatedTestCases specify [UtMethodTestSet]s that are used for test code
     *                           generation. By comparison with the first parameter,
     *                           {@code UtMethodTestSet} contains more information about
     *                           test, including result of the executions. Note, that
     *                           you can get the object with any sort of analysis,
     *                           for instance, symbolic or fuzz execution.
     * @param destinationClassName the name of containing class for the generated tests
     * @param classpath classpath that are used to build the class under test
     * @param dependencyClassPath class path including dependencies required for the code generation
     * @param classUnderTest for this class test should be generated
     * @param projectType JVM, Spring, Python, or other type of project
     * @param testFramework test framework that is going to be used for running generated tests
     * @param mockFramework framework that will be used in the generated tests
     * @param codegenLanguage the target language of the test generation. It can be different from the source language.
     * @param staticsMocking the approach to the statics mocking
     * @param generateWarningsForStaticMocking enable generation of warning about forced static mocking in comments
     *                                         of generated tests.
     * @param forceStaticMocking enables static mocking
     * @param testClassPackageName package name for the generated class with the tests
     * @param applicationContext specify application context here
     */
    @JvmStatic
    @JvmOverloads
    fun generateTestCode(
        methodsForGeneration: List<TestMethodInfo>,
        generatedTestCases: List<UtMethodTestSet> = mutableListOf(),
        destinationClassName: String,
        classpath: String,
        dependencyClassPath: String,
        classUnderTest: Class<*>,
        projectType: ProjectType = ProjectType.PureJvm,
        testFramework: TestFramework = Junit5,
        mockFramework: MockFramework = MockFramework.MOCKITO,
        codegenLanguage: CodegenLanguage = CodegenLanguage.JAVA,
        staticsMocking: StaticsMocking = NoStaticMocking,
        generateWarningsForStaticMocking: Boolean = false,
        forceStaticMocking: ForceStaticMocking = ForceStaticMocking.DO_NOT_FORCE,
        testClassPackageName: String = classUnderTest.nameOfPackage,
        applicationContext: ApplicationContext
    ): String {

        val testSets: MutableList<UtMethodTestSet> = generatedTestCases.toMutableList()

        val concreteExecutor = ConcreteExecutor(
            applicationContext.createConcreteExecutionContext(
                fullClasspath = dependencyClassPath,
                classpathWithoutDependencies = classpath
            ).instrumentationFactory,
            classpath
        )

        testSets.addAll(generateUnitTests(concreteExecutor, methodsForGeneration, classUnderTest))

        if (stopConcreteExecutorOnExit) {
            concreteExecutor.close()
        }

        return withUtContext(UtContext(classUnderTest.classLoader)) {
            applicationContext.createCodeGenerator(
                CodeGeneratorParams(
                    classUnderTest = classUnderTest.id,
                    projectType = projectType,
                    testFramework = testFramework,
                    mockFramework = mockFramework,
                    codegenLanguage = codegenLanguage,
                    cgLanguageAssistant = CgLanguageAssistant.getByCodegenLanguage(codegenLanguage),
                    staticsMocking = staticsMocking,
                    forceStaticMocking = forceStaticMocking,
                    generateWarningsForStaticMocking = generateWarningsForStaticMocking,
                    testClassPackageName = testClassPackageName,
                )
            ).generateAsString(testSets, destinationClassName)
        }
    }

    /**
     * Generates test sets using default workflow.
     *
     * @see [fuzzingTestSets]
     */
    @JvmStatic
    @JvmOverloads
    fun generateTestSetsForMethods(
        methodsToAnalyze: List<Method>,
        classUnderTest: Class<*>,
        classpath: String,
        dependencyClassPath: String,
        mockStrategyApi: MockStrategyApi = MockStrategyApi.OTHER_PACKAGES,
        generationTimeoutInMillis: Long = UtSettings.utBotGenerationTimeoutInMillis,
        applicationContext: ApplicationContext
    ): MutableList<UtMethodTestSet> {

        assert(methodsToAnalyze.all {classUnderTest.declaredMethods.contains(it)})
            { "Some methods are absent in the ${classUnderTest.name} class." }

        val utContext = UtContext(classUnderTest.classLoader)
        val testSets: MutableList<UtMethodTestSet> = mutableListOf()

        testSets.addAll(withUtContext(utContext) {
            val buildPath = FileUtil.isolateClassFiles(classUnderTest).toPath()
            TestCaseGenerator(
                listOf(buildPath),
                classpath,
                dependencyClassPath,
                jdkInfo = JdkInfoDefaultProvider().info,
                applicationContext = applicationContext
            ).generate(
                methodsToAnalyze.map { it.executableId },
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
     * @see [generateTestSetsForMethods]
     */
    @JvmStatic
    @JvmOverloads
    fun fuzzingTestSets(
        methodsToAnalyze: List<Method>,
        classUnderTest: Class<*>,
        classpath: String,
        dependencyClassPath: String,
        mockStrategyApi: MockStrategyApi = MockStrategyApi.OTHER_PACKAGES,
        generationTimeoutInMillis: Long = UtSettings.utBotGenerationTimeoutInMillis,
        primitiveValuesSupplier: CustomFuzzerValueSupplier = CustomFuzzerValueSupplier { null },
        applicationContext: ApplicationContext
    ): MutableList<UtMethodTestSet> {

        assert(methodsToAnalyze.all {classUnderTest.declaredMethods.contains(it)})
            { "Some methods are absent in the ${classUnderTest.name} class." }

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

        val customModelProvider = JavaValueProvider { _, type ->
            sequence {
                createPrimitiveModels(primitiveValuesSupplier, type.classId).forEach { model ->
                    yield(Seed.Simple(FuzzedValue(model)))
                }
            }
        }

        return withUtContext(UtContext(classUnderTest.classLoader)) {
            val buildPath = FileUtil.isolateClassFiles(classUnderTest).toPath()
            TestCaseGenerator(
                listOf(buildPath),
                classpath,
                dependencyClassPath,
                jdkInfo = JdkInfoDefaultProvider().info,
                applicationContext = applicationContext.transformValueProvider { defaultModelProvider ->
                    customModelProvider.withFallback(defaultModelProvider)
                }
            )
                .generate(
                    methodsToAnalyze.map{ it.executableId },
                    mockStrategyApi,
                    chosenClassesToMockAlways = emptySet(),
                    generationTimeoutInMillis,
                    generate = { symbolicEngine -> symbolicEngine.fuzzing() }
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
                    instrumentation = emptyList(),
                    UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis,
                    isRerun = false,
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