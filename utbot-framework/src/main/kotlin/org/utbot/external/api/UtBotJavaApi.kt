package org.utbot.external.api

import org.utbot.common.FileUtil
import org.utbot.common.packageName
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.model.ModelBasedCodeGeneratorService
import org.utbot.framework.concrete.UtConcreteExecutionData
import org.utbot.framework.concrete.UtConcreteExecutionResult
import org.utbot.framework.concrete.UtExecutionInstrumentation
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtBotTestCaseGenerator
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.execute
import java.lang.reflect.Method
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

fun toUtMethod(method: Method, kClass: KClass<*>) = UtMethod(method.kotlinFunction as KCallable<*>, kClass)

object UtBotJavaApi {

    @JvmStatic
    var stopConcreteExecutorOnExit: Boolean = true

    @JvmStatic
    @JvmOverloads
    fun generate(
        methodsForGeneration: List<TestMethodInfo>,
        generatedTestCases: List<UtTestCase> = mutableListOf(),
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

        val testCases: MutableList<UtTestCase> = generatedTestCases.toMutableList()

        val concreteExecutor = ConcreteExecutor(
            UtExecutionInstrumentation,
            classpath,
            dependencyClassPath
        )

        testCases.addAll(generateUnitTests(concreteExecutor, methodsForGeneration, classUnderTest))

        if (stopConcreteExecutorOnExit) {
            concreteExecutor.close()
        }

        return withUtContext(utContext) {
            val testGenerator = ModelBasedCodeGeneratorService().serviceProvider.apply {
                init(
                    classUnderTest = classUnderTest,
                    params = mutableMapOf(),
                    testFramework = testFramework,
                    mockFramework = mockFramework,
                    codegenLanguage = codegenLanguage,
                    staticsMocking = staticsMocking,
                    forceStaticMocking = forceStaticMocking,
                    generateWarningsForStaticMocking = generateWarningsForStaticMocking,
                    testClassPackageName = testClassPackageName
                )
            }

            testGenerator.generateAsString(
                testCases,
                destinationClassName
            )
        }
    }

    @JvmStatic
    @JvmOverloads
    fun generateTestCases(
        methodsForAutomaticGeneration: List<TestMethodInfo>,
        classUnderTest: Class<*>,
        classpath: String,
        dependencyClassPath: String,
        mockStrategyApi: MockStrategyApi = MockStrategyApi.OTHER_PACKAGES,
        generationTimeoutInMillis: Long = UtSettings.utBotGenerationTimeoutInMillis
    ): MutableList<UtTestCase> {

        val utContext = UtContext(classUnderTest.classLoader)
        val testCases: MutableList<UtTestCase> = mutableListOf()

        testCases.addAll(withUtContext(utContext) {
            UtBotTestCaseGenerator
                .apply {
                    init(
                        FileUtil.isolateClassFiles(classUnderTest.kotlin).toPath(), classpath, dependencyClassPath
                    )
                }
                .generateForSeveralMethods(
                    methodsForAutomaticGeneration.map {
                        toUtMethod(
                            it.methodToBeTestedFromUserInput,
                            classUnderTest.kotlin
                        )
                    },
                    mockStrategyApi,
                    chosenClassesToMockAlways = emptySet(),
                    generationTimeoutInMillis
                )
        })

        return testCases
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

        val utExecution = UtExecution(
            testInfo.initialState,
            testInfo.initialState, // it seems ok for concrete execution
            utExecutionResult,
            emptyList(),
            mutableListOf(),
            listOf()
        )

        val utMethod = UtMethod(methodCallable, containingClass.kotlin)

        UtTestCase(
            utMethod,
            listOf(utExecution)
        )
    }.toList()
}