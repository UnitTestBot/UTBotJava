package api

import codegen.JsCodeGenerator
import com.google.javascript.rhino.Node
import framework.api.js.JsClassId
import framework.api.js.JsMethodId
import framework.api.js.JsMultipleClassId
import framework.api.js.util.isJsBasic
import framework.api.js.util.jsErrorClassId
import framework.api.js.util.toJsClassId
import fuzzer.new.JsFeedback
import fuzzer.new.JsFuzzingExecutionFeedback
import fuzzer.new.JsMethodDescription
import fuzzer.new.JsTimeoutExecution
import fuzzer.new.JsValidExecution
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
import org.utbot.fuzzer.FuzzedConcreteValue
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
import settings.JsTestGenerationSettings.fuzzingThreshold
import utils.PathResolver
import utils.ResultData
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
        val executionResults = mutableListOf<JsFuzzingExecutionFeedback>()
        // TODO: add timeout predicate
        runBlocking {
            runLol(funcNode, execId, context).collect {
                executionResults += it
            }
        }
        val testsForGenerator = mutableListOf<UtExecution>()
        val errorsForGenerator = mutableMapOf<String, Int>()
        executionResults.forEach { value ->
            when (value) {
                is JsTimeoutExecution -> errorsForGenerator[value.utTimeout.exception.toString()] = 1
                is JsValidExecution -> testsForGenerator.add(value.utFuzzedExecution)
            }
            if (value is JsTimeoutExecution) {
                errorsForGenerator[value.utTimeout.exception.toString()] = 1
            }
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
        classId: JsClassId
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

            else -> throw UnsupportedOperationException("Not yet implemented!")
        }
        return thisInstance
    }

    private fun getUtModelResult(
        execId: JsMethodId,
        resultData: ResultData,
        fuzzedValues: List<FuzzedValue>
    ): UtExecutionResult {
        if (resultData.rawString == "Error:Timeout") return UtTimeoutException(
            TimeoutException("\"Timeout in generating test for ${
                fuzzedValues.joinToString { f -> f.model.toString() }
            } parameters\"")
        )
        val (returnValue, valueClassId) = resultData.rawString.toJsAny(execId.returnType)
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
    ): Flow<JsFuzzingExecutionFeedback> = flow {
        val fuzzerVisitor = JsFuzzerAstVisitor()
        fuzzerVisitor.accept(funcNode)
        val jsDescription = JsMethodDescription(
            name = funcNode.getAbstractFunctionName(),
            parameters = execId.parameters,
            // TODO: make visitor return JsFuzzedConcreteValue
            concreteValues = fuzzerVisitor.fuzzedConcreteValues.map {
                FuzzedConcreteValue(
                    it.classId.toJsClassId(),
                    it.value,
                    it.fuzzedContext
                )
            }
        )
        val thisInstance = makeThisInstance(execId, execId.classId)
        val collectedValues = mutableListOf<List<FuzzedValue>>()
        val instrService = InstrumentationService(context)
        instrService.instrument()
        val coverageProvider = CoverageServiceProvider(
            context,
            instrService,
            context.settings.coverageMode
        )
        val allStmts = instrService.allStatements
        val currentlyCoveredStmts = mutableSetOf<Int>().apply {
            this.addAll(coverageProvider.baseCoverage)
        }
        val startTime = System.currentTimeMillis()
        runFuzzing(jsDescription) { _, values ->
            if (System.currentTimeMillis() - startTime > 5_000) return@runFuzzing JsFeedback(Control.STOP)
            collectedValues += values
            if (collectedValues.size > fuzzingThreshold) {
                try {
                    val (coveredStmts, executionResults) = coverageProvider.get(
                        collectedValues,
                        execId
                    )
                    coveredStmts.forEachIndexed { paramIndex, covData ->
                        if (!currentlyCoveredStmts.containsAll(covData.additionalCoverage)) {
                            val result =
                                getUtModelResult(
                                    execId = execId,
                                    resultData = executionResults[paramIndex],
                                    collectedValues[paramIndex]
                                )
                            val initEnv =
                                EnvironmentModels(thisInstance, collectedValues[paramIndex].map { it.model }, mapOf())
                            emit(
                                JsValidExecution(
                                    UtFuzzedExecution(
                                        stateBefore = initEnv,
                                        stateAfter = initEnv,
                                        result = result,
                                    )
                                )
                            )
                            currentlyCoveredStmts += covData.additionalCoverage
                            return@runFuzzing when (result) {
                                is UtExecutionSuccess -> JsFeedback(Control.CONTINUE)
                                // TODO: Maybe continue?
                                else -> JsFeedback(Control.PASS)
                            }
                        }
                    }
                    if (currentlyCoveredStmts.containsAll(allStmts)) return@runFuzzing JsFeedback(Control.STOP)
                } catch (e: TimeoutException) {
                    emit(
                        JsTimeoutExecution(
                            UtTimeoutException(
                                TimeoutException("Timeout on unknown test case. Consider using \"Basic\" coverage mode")
                            )
                        )
                    )
                    return@runFuzzing JsFeedback(Control.PASS)
                } finally {
                    collectedValues.clear()
                }
            }
            return@runFuzzing JsFeedback(Control.PASS)
        }
        instrService.removeTempFiles()
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
