package api

import codegen.JsCodeGenerator
import com.oracle.js.parser.ErrorManager
import com.oracle.js.parser.Parser
import com.oracle.js.parser.ScriptEnvironment
import com.oracle.js.parser.Source
import com.oracle.js.parser.ir.ClassNode
import com.oracle.js.parser.ir.FunctionNode
import fuzzer.JsFuzzer
import fuzzer.providers.JsObjectModelProvider
import java.io.File
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtModel
import framework.api.js.JsClassId
import framework.api.js.JsMethodId
import framework.api.js.JsMultipleClassId
import framework.api.js.util.isJsBasic
import framework.api.js.util.jsErrorClassId
import org.graalvm.polyglot.Context
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
import parser.JsClassAstVisitor
import parser.JsFunctionAstVisitor
import parser.JsFuzzerAstVisitor
import parser.JsParserUtils
import parser.JsToplevelFunctionAstVisitor
import service.CoverageServiceProvider
import service.ServiceContext
import service.TernService
import settings.JsDynamicSettings
import settings.JsTestGenerationSettings.dummyClassName
import utils.PathResolver
import utils.constructClass
import utils.toJsAny


class JsTestGenerator(
    private val fileText: String,
    private var sourceFilePath: String,
    private var projectPath: String = sourceFilePath.substringBeforeLast("/"),
    private val selectedMethods: List<String>? = null,
    private var parentClassName: String? = null,
    private var outputFilePath: String?,
    private val exportsManager: (List<String>) -> Unit,
    private val settings: JsDynamicSettings,
) {

    private val exports = mutableSetOf<String>()

    private lateinit var parsedFile: FunctionNode

    private val utbotDir = "utbotJs"

    init {
        fixPathDelims()
    }

    private fun fixPathDelims() {
        projectPath = projectPath.replace("\\", "/")
        outputFilePath = outputFilePath?.replace("\\", "/")
        sourceFilePath = sourceFilePath.replace("\\", "/")
    }

    /**
     * Returns String representation of generated tests.
     */
    fun run(): String {
        parsedFile = runParser(fileText)
        val context = ServiceContext(
            utbotDir = utbotDir,
            projectPath = projectPath,
            filePathToInference = sourceFilePath,
            trimmedFileText = fileText,
            fileText = fileText,
            settings = settings,
        )
        val ternService = TernService(context)
        val paramNames = mutableMapOf<ExecutableId, List<String>>()
        val testSets = mutableListOf<CgMethodTestSet>()
        val classNode =
            JsParserUtils.searchForClassDecl(
                className = parentClassName,
                fileText = fileText,
                strict = selectedMethods?.isNotEmpty() ?: false
            )
        parentClassName = classNode?.ident?.name?.toString()
        val classId = makeJsClassId(classNode, ternService)
        val methods = makeMethodsToTest()
        if (methods.isEmpty()) throw IllegalArgumentException("No methods to test were found!")
        methods.forEach { funcNode ->
            try {
                makeTestsForMethod(classId, funcNode, classNode, context, testSets, paramNames)
            } catch (e: Exception) {
                throw e
            }
        }
        val importPrefix = makeImportPrefix()
        val codeGen = JsCodeGenerator(
            classUnderTest = classId,
            paramNames = paramNames,
            importPrefix = importPrefix
        )
        return codeGen.generateAsStringWithTestReport(testSets).generatedCode
    }

    private fun makeTestsForMethod(
        classId: JsClassId,
        funcNode: FunctionNode,
        classNode: ClassNode?,
        context: ServiceContext,
        testSets: MutableList<CgMethodTestSet>,
        paramNames: MutableMap<ExecutableId, List<String>>
    ) {
        val execId = classId.allMethods.find {
            it.name == funcNode.name.toString()
        } ?: throw IllegalStateException()
        manageExports(classNode, funcNode, execId)
        val (concreteValues, fuzzedValues) = runFuzzer(funcNode, execId)
        val (allCoveredStatements, executionResults) =
            CoverageServiceProvider(context).get(
                settings.coverageMode,
                fuzzedValues,
                execId,
                classNode
            )
        val testsForGenerator = mutableListOf<UtExecution>()
        val errorsForGenerator = mutableMapOf<String, Int>()
        executionResults.forEachIndexed {index, value ->
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
            val thisInstance = makeThisInstance(execId, classId, concreteValues)
            val initEnv = EnvironmentModels(thisInstance, param.map { it.model }, mapOf())
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
        paramNames[execId] = funcNode.parameters.map { it.name.toString() }
    }

    private fun makeImportPrefix(): String {
        return outputFilePath?.let {
            PathResolver.getRelativePath(
                File(it).parent,
                File(sourceFilePath).parent,
            )
        } ?: ""
    }

    private fun makeThisInstance(
        execId: JsMethodId,
        classId: JsClassId,
        concreteValues: Set<FuzzedConcreteValue>
    ): UtModel? {
        val thisInstance = when {
            execId.isStatic -> null
            classId.allConstructors.first().parameters.isEmpty() -> {
                val id = JsObjectModelProvider.idGenerator.asInt
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

            else -> {
                JsObjectModelProvider.generate(
                    FuzzedMethodDescription(
                        name = "thisInstance",
                        returnType = voidClassId,
                        parameters = listOf(classId),
                        concreteValues = concreteValues
                    )
                ).take(10).toList()
                    .shuffled().map { it.value.model }.first()
            }
        }
        return thisInstance
    }

    private fun getUtModelResult(
        execId: JsMethodId,
        returnText: String
    ): UtExecutionResult {
        val (returnValue, valueClassId) = returnText.toJsAny(execId.returnType)
        val result = JsUtModelConstructor().construct(returnValue, valueClassId)
        val utExecResult = when (result.classId) {
            jsErrorClassId -> UtExplicitlyThrownException(Throwable(returnValue.toString()), false)
            else -> UtExecutionSuccess(result)
        }
        return utExecResult
    }

    private fun runFuzzer(
        funcNode: FunctionNode,
        execId: JsMethodId
    ): Pair<Set<FuzzedConcreteValue>, List<List<FuzzedValue>>> {
        val fuzzerVisitor = JsFuzzerAstVisitor()
        funcNode.body.accept(fuzzerVisitor)
        val methodUnderTestDescription =
            FuzzedMethodDescription(execId, fuzzerVisitor.fuzzedConcreteValues).apply {
                compilableName = funcNode.name.toString()
                val names = funcNode.parameters.map { it.name.toString() }
                parameterNameMap = { index -> names.getOrNull(index) }
            }
        val fuzzedValues =
            JsFuzzer.jsFuzzing(methodUnderTestDescription = methodUnderTestDescription).toList()
        return fuzzerVisitor.fuzzedConcreteValues.toSet() to fuzzedValues
    }

    private fun manageExports(
        classNode: ClassNode?,
        funcNode: FunctionNode,
        execId: JsMethodId
    ) {
        val obligatoryExport = (classNode?.ident?.name ?: funcNode.ident.name).toString()
        val collectedExports = collectExports(execId)
        exports += (collectedExports + obligatoryExport)
        exportsManager(exports.toList())
    }

    private fun makeMethodsToTest(): List<FunctionNode> {
        return selectedMethods?.map {
            getFunctionNode(
                focusedMethodName = it,
                parentClassName = parentClassName,
                fileText = fileText
            )
        } ?: getMethodsToTest()
    }

    private fun makeJsClassId(
        classNode: ClassNode?,
        ternService: TernService
    ): JsClassId {
        return classNode?.let {
            JsClassId(parentClassName!!).constructClass(ternService, classNode)
        } ?: JsClassId("undefined").constructClass(
            ternService = ternService,
            functions = extractToplevelFunctions()
        )
    }

    private fun runParser(fileText: String): FunctionNode {
        // Fixes problem with Graal.polyglot missing from classpath, resulting in error.
        withUtContext(UtContext(Context::class.java.classLoader)) {
            val parser = Parser(
                ScriptEnvironment.builder().build(),
                Source.sourceFor("jsFile", fileText),
                ErrorManager.ThrowErrorManager()
            )
            return parser.parse()
        }
    }

    private fun extractToplevelFunctions(): List<FunctionNode> {
        val visitor = JsToplevelFunctionAstVisitor()
        parsedFile.body.accept(visitor)
        return visitor.extractedMethods
    }

    private fun collectExports(methodId: JsMethodId): List<String> {
        val res = mutableListOf<String>()
        methodId.parameters.forEach {
            if (!(it.isJsBasic || it is JsMultipleClassId)) {
                res += it.name
            }
        }
        if (!(methodId.returnType.isJsBasic || methodId.returnType is JsMultipleClassId)) res += methodId.returnType.name
        return res
    }

    private fun analyzeCoverage(coverageList: List<Set<Int>>): List<Int> {
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

    private fun getFunctionNode(focusedMethodName: String, parentClassName: String?, fileText: String): FunctionNode {
        val parser = Parser(
            ScriptEnvironment.builder().build(),
            Source.sourceFor("jsFile", fileText),
            ErrorManager.ThrowErrorManager()
        )
        val fileNode = parser.parse()
        val visitor = JsFunctionAstVisitor(
            focusedMethodName,
            if (parentClassName != dummyClassName) parentClassName else null
        )
        fileNode.accept(visitor)
        return visitor.targetFunctionNode
    }

    private fun getMethodsToTest() =
        parentClassName?.let {
            getClassMethods(it)
        } ?: extractToplevelFunctions().ifEmpty {
            getClassMethods("")
        }

    private fun getClassMethods(className: String): List<FunctionNode> {
        val visitor = JsClassAstVisitor(className)
        parsedFile.body.accept(visitor)
        val classNode = JsParserUtils.searchForClassDecl(className, fileText)
        return classNode?.classElements?.filter {
            it.value is FunctionNode
        }?.map { it.value as FunctionNode } ?: throw IllegalStateException("Can't extract methods of class $className")
    }
}