package api

import codegen.JsCodeGenerator
import com.google.javascript.rhino.Node
import framework.api.js.JsClassId
import framework.api.js.JsMethodId
import framework.api.js.JsMultipleClassId
import framework.api.js.util.isJsBasic
import framework.api.js.util.jsErrorClassId
import framework.api.js.util.toJsClassId
import fuzzer.JsFuzzer
import fuzzer.new.JsFeedback
import fuzzer.new.JsFuzzedConcreteValue
import fuzzer.new.JsMethodDescription
import fuzzer.new.runFuzzing
import fuzzer.providers.JsObjectModelProvider
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.fuzzing.Control
import parser.JsClassAstVisitor
import parser.JsFunctionAstVisitor
import parser.JsFuzzerAstVisitor
import parser.JsParserUtils
import parser.JsParserUtils.getAbstractFunctionName
import parser.JsParserUtils.getAbstractFunctionParams
import parser.JsParserUtils.getClassMethods
import parser.JsParserUtils.getClassName
import parser.JsParserUtils.getParamName
import parser.JsParserUtils.runParser
import parser.JsToplevelFunctionAstVisitor
import service.CoverageServiceProvider
import service.InstrumentationService
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

    private lateinit var parsedFile: Node

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
            parsedFile = parsedFile,
            settings = settings,
        )
        val ternService = TernService(context)
        val paramNames = mutableMapOf<ExecutableId, List<String>>()
        val testSets = mutableListOf<CgMethodTestSet>()
        val classNode =
            JsParserUtils.searchForClassDecl(
                className = parentClassName,
                parsedFile = parsedFile,
                strict = selectedMethods?.isNotEmpty() ?: false
            )
        parentClassName = classNode?.getClassName()
        val classId = makeJsClassId(classNode, ternService)
        val methods = makeMethodsToTest()
        if (methods.isEmpty()) throw IllegalArgumentException("No methods to test were found!")
        methods.forEach { funcNode ->
            makeTestsForMethod(classId, funcNode, classNode, context, testSets, paramNames)
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
        funcNode: Node,
        classNode: Node?,
        context: ServiceContext,
        testSets: MutableList<CgMethodTestSet>,
        paramNames: MutableMap<ExecutableId, List<String>>
    ) {
        val execId = classId.allMethods.find {
            it.name == funcNode.getAbstractFunctionName()
        } ?: throw IllegalStateException()
        manageExports(classNode, funcNode, execId)
        val kek = mutableListOf<UtExecutionResult>()
        runBlocking {
            runLol(funcNode, execId, context).collect {
                kek += it
            }
        }
        val (concreteValues, fuzzedValues) = runFuzzer(funcNode, execId, context)
        val (allCoveredStatements, executionResults) =
            CoverageServiceProvider(context, InstrumentationService(context)).get(
                settings.coverageMode,
                fuzzedValues,
                execId
            )
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
            executableId = execId,
            errors = errorsForGenerator,
            executions = testsForGenerator,
        )
        testSets += testSet
        paramNames[execId] = funcNode.getAbstractFunctionParams().map { it.getParamName() }
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

    private fun runLol(
        funcNode: Node,
        execId: JsMethodId,
        context: ServiceContext,
    ): Flow<UtExecutionResult> = flow {
        val fuzzerVisitor = JsFuzzerAstVisitor()
        fuzzerVisitor.accept(funcNode)
        val jsDescription = JsMethodDescription(
            name = funcNode.getAbstractFunctionName(),
            parameters = execId.parameters,
            concreteValues = fuzzerVisitor.fuzzedConcreteValues.map {
                JsFuzzedConcreteValue(
                    it.classId.toJsClassId(),
                    it.value,
                    it.fuzzedContext
                )
            }
        )
        val collectedValues = mutableListOf<List<FuzzedValue>>()
        val chunkThreshold = 3
        val instrService = InstrumentationService(context)
        instrService.instrument()
        val allStmts = instrService.allStatements
        val currentlyCoveredStmts = mutableSetOf<Int>()
        runFuzzing(jsDescription) { description, values ->
            collectedValues += values
            if (collectedValues.size > chunkThreshold) {
                try {
                    val (coveredStmts, executionResults) = CoverageServiceProvider(context, instrService).get(
                        context.settings.coverageMode,
                        collectedValues,
                        execId
                    )
                    collectedValues.clear()
                    coveredStmts.forEachIndexed { paramIndex, covSet ->
                        if (!currentlyCoveredStmts.containsAll(covSet)) {
                            val result =
                                getUtModelResult(
                                    execId = execId,
                                    returnText = executionResults[paramIndex]
                                )
                            emit(result)
                            currentlyCoveredStmts += covSet
                            return@runFuzzing when (result) {
                                is UtExecutionSuccess -> JsFeedback(Control.CONTINUE)
                                else -> JsFeedback(Control.PASS)
                            }
                        }
                    }
                    if (allStmts == coveredStmts) return@runFuzzing JsFeedback(Control.STOP)
                } catch (e: TimeoutException) {
                    emit(
                        UtTimeoutException(
                            TimeoutException("Timeout on unknown test case. Consider using \"Basic\" coverage mode")
                        )
                    )
                    return@runFuzzing JsFeedback(Control.PASS)
                }
            }
            return@runFuzzing JsFeedback(Control.PASS)
        }
    }

    private fun runFuzzer(
        funcNode: Node,
        execId: JsMethodId,
        context: ServiceContext,
    ): Pair<Set<FuzzedConcreteValue>, List<List<FuzzedValue>>> {
        val fuzzerVisitor = JsFuzzerAstVisitor()
        fuzzerVisitor.accept(funcNode)
        val methodUnderTestDescription =
            FuzzedMethodDescription(execId, fuzzerVisitor.fuzzedConcreteValues).apply {
                compilableName = funcNode.getAbstractFunctionName()
                val names = funcNode.getAbstractFunctionParams().map { it.getParamName() }
                parameterNameMap = { index -> names.getOrNull(index) }
            }

        val fuzzedValues =
            JsFuzzer.jsFuzzing(methodUnderTestDescription = methodUnderTestDescription).toList()
        return fuzzerVisitor.fuzzedConcreteValues.toSet() to fuzzedValues
    }

    private fun manageExports(
        classNode: Node?,
        funcNode: Node,
        execId: JsMethodId
    ) {
        val obligatoryExport = (classNode?.getClassName() ?: funcNode.getAbstractFunctionName()).toString()
        val collectedExports = collectExports(execId)
        exports += (collectedExports + obligatoryExport)
        exportsManager(exports.toList())
    }

    private fun makeMethodsToTest(): List<Node> {
        return selectedMethods?.map {
            getFunctionNode(
                focusedMethodName = it,
                parentClassName = parentClassName,
            )
        } ?: getMethodsToTest()
    }

    private fun makeJsClassId(
        classNode: Node?,
        ternService: TernService
    ): JsClassId {
        return classNode?.let {
            JsClassId(parentClassName!!).constructClass(ternService, classNode)
        } ?: JsClassId("undefined").constructClass(
            ternService = ternService,
            functions = extractToplevelFunctions()
        )
    }

    private fun extractToplevelFunctions(): List<Node> {
        val visitor = JsToplevelFunctionAstVisitor()
        visitor.accept(parsedFile)
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

    private fun getFunctionNode(focusedMethodName: String, parentClassName: String?): Node {
        val visitor = JsFunctionAstVisitor(
            focusedMethodName,
            if (parentClassName != dummyClassName) parentClassName else null
        )
        visitor.accept(parsedFile)
        return visitor.targetFunctionNode
    }

    private fun getMethodsToTest() =
        parentClassName?.let {
            getClassMethods(it)
        } ?: extractToplevelFunctions().ifEmpty {
            getClassMethods("")
        }

    private fun getClassMethods(className: String): List<Node> {
        val visitor = JsClassAstVisitor(className)
        visitor.accept(parsedFile)
        val classNode = JsParserUtils.searchForClassDecl(className, parsedFile)
        return classNode?.getClassMethods() ?: throw IllegalStateException("Can't extract methods of class $className")
    }
}
