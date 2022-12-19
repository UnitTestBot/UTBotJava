package org.utbot.engine.greyboxfuzzer

import org.junit.jupiter.api.Assertions
import org.utbot.common.FileUtil
import org.utbot.common.PathUtil.getUrlsFromClassLoader
import org.utbot.engine.greyboxfuzzer.PredefinedGeneratorParameters.getMethodByName
import org.utbot.examples.GraphAlgorithms
import org.utbot.external.api.*
import org.utbot.external.api.UtBotJavaApi.fuzzingTestSets
import org.utbot.external.api.UtBotJavaApi.stopConcreteExecutorOnExit
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.UtContext.Companion.setUtContext
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import java.io.File
import java.lang.reflect.Method
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import kotlin.io.path.ExperimentalPathApi
import kotlin.system.exitProcess

class FuzzerExecutor {

    private val context: AutoCloseable
    private val modelFactory: UtModelFactory

    init {
        SootUtils.runSoot(GraphAlgorithms::class.java)
        context = setUtContext(UtContext(GraphAlgorithms::class.java.classLoader))
        modelFactory = UtModelFactory()
    }

    fun testSimpleFuzzing(clazz: Class<*>) {
        //runSoot(StringSwitchExample::class.java)
        stopConcreteExecutorOnExit = false

        val classpath = getClassPath(clazz)
        val dependencyClassPath = getDependencyClassPath()

        val classUnderTestModel = modelFactory.produceCompositeModel(
            classIdForType(clazz)
        )

        val methodsUnderTest = clazz.declaredMethods
        val methodsInfo = methodsUnderTest.map { methodUnderTest ->
            val models = modelFactory.produceAssembleModel(
                methodUnderTest,
                clazz, listOf(classUnderTestModel)
            )

            val methodState = EnvironmentModels(
                models[classUnderTestModel],
                Arrays.asList(UtPrimitiveModel("initial model"), UtPrimitiveModel(-10), UtPrimitiveModel(0)), emptyMap()
            )
            TestMethodInfo(
                methodUnderTest,
                methodState
            )
        }

        val testSets1: List<UtMethodTestSet> = fuzzingTestSets(
            methodsForAutomaticGeneration = methodsInfo,
            classUnderTest = clazz,
            classpath = classpath,
            dependencyClassPath = dependencyClassPath!!,
            mockStrategyApi = MockStrategyApi.OTHER_PACKAGES,
            generationTimeoutInMillis = 100000L,
            isGreyBoxFuzzing = true
        ) { type: Class<*> ->
            if (Int::class.javaPrimitiveType == type || Int::class.java == type) {
                return@fuzzingTestSets Arrays.asList<Any>(
                    0,
                    Int.MIN_VALUE,
                    Int.MAX_VALUE
                )
            }
            null
        }

//        val generate = generate(
//            listOf(methodInfo),
//            testSets1,
//            org.utbot.examples.manual.PredefinedGeneratorParameters.destinationClassName,
//            classpath,
//            dependencyClassPath!!,
//            StringSwitchExample::class.java
//        )
        exitProcess(0)
    }
    fun testSimpleFuzzing(clazz: Class<*>, funName: String) {


        //runSoot(StringSwitchExample::class.java)
        stopConcreteExecutorOnExit = false

        val classpath = getClassPath(clazz)
        val dependencyClassPath = getDependencyClassPath()

        val classUnderTestModel = modelFactory.produceCompositeModel(
            classIdForType(clazz)
        )

        val methodUnderTest: Method = getMethodByName(
            clazz, funName
        )

        val models = modelFactory.produceAssembleModel(
            methodUnderTest,
            clazz, listOf(classUnderTestModel)
        )

        val methodState = EnvironmentModels(
            models[classUnderTestModel],
            Arrays.asList(UtPrimitiveModel("initial model"), UtPrimitiveModel(-10), UtPrimitiveModel(0)), emptyMap()
        )

        val methodInfo = TestMethodInfo(
            methodUnderTest,
            methodState
        )

        val testSets1: List<UtMethodTestSet> = fuzzingTestSets(
            methodsForAutomaticGeneration = listOf(methodInfo),
            classUnderTest = clazz,
            classpath = classpath,
            dependencyClassPath = dependencyClassPath!!,
            mockStrategyApi = MockStrategyApi.OTHER_PACKAGES,
            generationTimeoutInMillis = 100000L,
            isGreyBoxFuzzing = true
        ) { type: Class<*> ->
            if (Int::class.javaPrimitiveType == type || Int::class.java == type) {
                return@fuzzingTestSets Arrays.asList<Any>(
                    0,
                    Int.MIN_VALUE,
                    Int.MAX_VALUE
                )
            }
            null
        }

//        val generate = generate(
//            listOf(methodInfo),
//            testSets1,
//            org.utbot.examples.manual.PredefinedGeneratorParameters.destinationClassName,
//            classpath,
//            dependencyClassPath!!,
//            StringSwitchExample::class.java
//        )
        exitProcess(0)
//        stopConcreteExecutorOnExit = false
//        val classpath: String = getClassPath(clazz)
//        val dependencyClassPath: String = getDependencyClassPath()!!
//        val classUnderTestModel: UtCompositeModel = modelFactory.produceCompositeModel(
//            classIdForType(clazz)
//        )
//        val methodUnderTest = PredefinedGeneratorParameters.getMethodByName(
//            clazz, funName
//        )
//        val models: IdentityHashMap<UtModel, UtModel> = modelFactory.produceAssembleModel(
//            methodUnderTest,
//            clazz, listOf(classUnderTestModel)
//        )
//        val methodState = EnvironmentModels(
//            models[classUnderTestModel],
//            Arrays.asList(UtPrimitiveModel("initial model"), UtPrimitiveModel(-10), UtPrimitiveModel(0)), emptyMap()
//        )
//        val methodInfo = TestMethodInfo(
//            methodUnderTest,
//            methodState
//        )
//        val testSets: List<UtMethodTestSet> = fuzzingTestSets(
//            listOf(methodInfo),
//            clazz,
//            classpath,
//            dependencyClassPath,
//            MockStrategyApi.OTHER_PACKAGES,
//            3000L
//        )
//        exitProcess(0)
//        val utTestCases1: List<UtTestCase> = fuzzingTestCases(
//            listOf(
//                methodInfo
//            ),
//            clazz,
//            classpath,
//            dependencyClassPath,
//            MockStrategyApi.OTHER_PACKAGES,
//            100000L
//        ) { type: Class<*> ->
//            if (Int::class.javaPrimitiveType == type || Int::class.java == type) {
//                return@fuzzingTestCases Arrays.asList<Any>(
//                    0,
//                    Int.MIN_VALUE,
//                    Int.MAX_VALUE
//                )
//            }
//            null
//        }

//        generate(
//            listOf(methodInfo),
//            testSets,
//            PredefinedGeneratorParameters.destinationClassName,
//            classpath,
//            dependencyClassPath,
//            clazz
//        )
//        val snippet2 = Snippet(CodegenLanguage.JAVA, generate)
//        compileClassFile(PredefinedGeneratorParameters.destinationClassName, snippet2)
    }

    private fun getClassPath(clazz: Class<*>): String {
        return clazz.protectionDomain.codeSource.location.path
    }

    private fun getDependencyClassPath(): String? {
        val contextClassLoader = Thread.currentThread().contextClassLoader
        val urls = getUrlsFromClassLoader(contextClassLoader)
        return Arrays.stream(urls).map { url: URL ->
            try {
                return@map File(url.toURI()).toString()
            } catch (e: URISyntaxException) {
                Assertions.fail<Any>(e)
            }
            throw RuntimeException()
        }.collect(Collectors.joining(File.pathSeparator))
    }
}

internal object PredefinedGeneratorParameters {
    var destinationClassName = "GeneratedTest"
    fun getMethodByName(clazz: Class<*>, name: String): Method {
        return clazz.declaredMethods.first { it.name == name }
    }
}


object SootUtils {
    @JvmStatic
    fun runSoot(clazz: Class<*>) {
        val buildDir = FileUtil.locateClassPath(clazz) ?: FileUtil.isolateClassFiles(clazz)
        val buildDirPath = buildDir.toPath()

        if (buildDirPath != previousBuildDir) {
            org.utbot.framework.util.SootUtils.runSoot(listOf(buildDirPath), null, true, JdkInfoDefaultProvider().info)
            previousBuildDir = buildDirPath
        }
    }

    private var previousBuildDir: Path? = null
}

fun fields(
    classId: ClassId,
    vararg fields: Pair<String, Any>
): MutableMap<FieldId, UtModel> {
    return fields
        .associate {
            val fieldId = FieldId(classId, it.first)
            val fieldValue = when (val value = it.second) {
                is UtModel -> value
                else -> UtPrimitiveModel(value)
            }
            fieldId to fieldValue
        }
        .toMutableMap()
}

@OptIn(ExperimentalPathApi::class)
fun main() {
//    val random = GreyBoxFuzzerGenerators.sourceOfRandomness
//    val status = GreyBoxFuzzerGenerators.genStatus
//    withUtContext(UtContext(GraphAlgorithms::class.java.classLoader)) {
//        val gen = IntegerGenerator()
//        val generatedInt = gen.generateWithState(random, status)
//        println(generatedInt)
//        gen.generationState = GenerationState.CACHE
//        println(gen.generateWithState(random, status))
//    }
//
//    exitProcess(0)
//    val cl = Files.walk(Paths.get("utbot-framework/src/main/java/org/utbot/example/")).toList()
//        .filter { it!!.name.endsWith(".java") }
//        .map { it.toFile().absolutePath.substringAfterLast("java/").replace('/', '.').substringBeforeLast(".java") }
//        .map { Class.forName(it) }
////    //114!!
//    var i = 0
//    for (c in cl) {
//        ++i
//        //if (c.name != "org.utbot.example.casts.GenericCastExample") continue
//        //if (i < 169) continue
//        val methods = c.declaredMethods.filter { it.parameters.isNotEmpty() }.filter { !it.name.contains('$') }
//        for (m in methods) {
//            println("$i CLASS = ${c.name} from ${cl.size} method = ${m.name}")
//            try {
//                FuzzerExecutor().testSimpleFuzzing(c, m.name)
//            } catch (e: RuntimeException) {
//                println("No method source")
//            }
//        }
//    }

    repeat(1) {
        FuzzerExecutor().testSimpleFuzzing(GraphAlgorithms::class.java, "testFunc3")
        //FuzzerExecutor().testSimpleFuzzing(GraphAlgorithms::class.java)
    }
    //FuzzerExecutor().testSimpleFuzzing(DateFormatterTest::class.java, "testLocalDateTimeSerialization")
}