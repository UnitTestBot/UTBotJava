package api

import codegen.JsCodeGenerator
import com.google.javascript.rhino.Node
import framework.api.js.JsClassId
import framework.api.js.JsMethodId
import framework.api.js.JsUtFuzzedExecution
import framework.api.js.util.isExportable
import framework.api.js.util.isJsBasic
import framework.api.js.util.jsErrorClassId
import framework.api.js.util.jsUndefinedClassId
import fuzzer.JsFeedback
import fuzzer.JsFuzzedValue
import fuzzer.JsFuzzingExecutionFeedback
import fuzzer.JsMethodDescription
import fuzzer.JsStatement
import fuzzer.JsTimeoutExecution
import fuzzer.JsValidExecution
import fuzzer.runFuzzing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.utbot.common.runBlockingWithCancellationPredicate
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.utils.Trie
import parser.JsAstScrapper
import parser.JsFuzzerAstVisitor
import parser.JsParserUtils
import parser.JsParserUtils.getAbstractFunctionName
import parser.JsParserUtils.getAbstractFunctionParams
import parser.JsParserUtils.getClassMethods
import parser.JsParserUtils.getClassName
import parser.JsParserUtils.getParamName
import parser.JsParserUtils.runParser
import parser.JsToplevelFunctionAstVisitor
import providers.exports.IExportsProvider
import service.InstrumentationService
import service.PackageJson
import service.PackageJsonService
import service.ServiceContext
import service.TernService
import service.coverage.CoverageMode
import service.coverage.CoverageServiceProvider
import settings.JsDynamicSettings
import settings.JsTestGenerationSettings.fuzzingThreshold
import settings.JsTestGenerationSettings.fuzzingTimeout
import utils.PathResolver
import utils.constructClass
import utils.data.ResultData
import utils.toJsAny
import java.io.File
import java.util.concurrent.CancellationException

private val logger = KotlinLogging.logger {}

class JsTestGenerator(
    private val fileText: String,
    private var sourceFilePath: String,
    private var projectPath: String = sourceFilePath.substringBeforeLast("/"),
    private val selectedMethods: List<String>? = null,
    private var parentClassName: String? = null,
    private var outputFilePath: String?,
    private val exportsManager: (List<String>, (String?) -> String) -> Unit,
    private val settings: JsDynamicSettings,
    private val isCancelled: () -> Boolean = { false }
) {

    private val exports = mutableSetOf<String>()

    private lateinit var parsedFile: Node

    private lateinit var astScrapper: JsAstScrapper

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
        astScrapper = JsAstScrapper(parsedFile, sourceFilePath)
        val context = ServiceContext(
            utbotDir = utbotDir,
            projectPath = projectPath,
            filePathToInference = astScrapper.filesToInfer,
            parsedFile = parsedFile,
            settings = settings,
        )
        context.packageJson = PackageJsonService(
            sourceFilePath,
            projectPath,
        ).findClosestConfig()
        val paramNames = mutableMapOf<ExecutableId, List<String>>()
        val testSets = mutableListOf<CgMethodTestSet>()
        val classNode =
            JsParserUtils.searchForClassDecl(
                className = parentClassName,
                parsedFile = parsedFile,
                strict = selectedMethods?.isNotEmpty() ?: false
            )
        parentClassName = classNode?.getClassName()
        val classId = makeJsClassId(classNode, TernService(context))
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
        manageExports(classNode, funcNode, execId, context.packageJson)
        val executionResults = mutableListOf<JsFuzzingExecutionFeedback>()
        try {
            runBlockingWithCancellationPredicate(isCancelled) {
                runFuzzingFlow(funcNode, execId, context).collect {
                    executionResults += it
                }
            }
        } catch (e: CancellationException) {
            logger.info { "Fuzzing was stopped due to test generation cancellation" }
        }
        if (executionResults.isEmpty()) {
            if (isCancelled()) return
            throw UnsupportedOperationException("No test cases were generated for ${funcNode.getAbstractFunctionName()}")
        }
        logger.info { "${executionResults.size} test cases were suggested after fuzzing" }
        val testsForGenerator = mutableListOf<UtExecution>()
        val errorsForGenerator = mutableMapOf<String, Int>()
        executionResults.forEach { value ->
            when (value) {
                is JsTimeoutExecution -> errorsForGenerator[value.utTimeout.exception.message!!] = 1
                is JsValidExecution -> testsForGenerator.add(value.utFuzzedExecution)
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

    private fun getUtModelResult(
        execId: JsMethodId,
        resultData: ResultData,
        fuzzedValues: List<JsFuzzedValue>
    ): UtExecutionResult {
        if (resultData.isError && resultData.rawString == "Timeout") return UtTimeoutException(
            TimeoutException("  Timeout in generating test for ${
                execId.parameters
                    .zip(fuzzedValues)
                    .joinToString(
                        prefix = "${execId.name}(",
                        separator = ", ",
                        postfix = ")"
                    ) { (_, value) -> value.model.toString() }
            }")
        )
        val (returnValue, valueClassId) = resultData.toJsAny(
            if (execId.returnType.isJsBasic) JsClassId(resultData.type) else execId.returnType
        )
        val result = JsUtModelConstructor().construct(returnValue, valueClassId)
        val utExecResult = when (result.classId) {
            jsErrorClassId -> UtExplicitlyThrownException(Throwable(returnValue.toString()), false)
            else -> UtExecutionSuccess(result)
        }
        return utExecResult
    }

    private fun runFuzzingFlow(
        funcNode: Node,
        execId: JsMethodId,
        context: ServiceContext,
    ): Flow<JsFuzzingExecutionFeedback> = flow {
        val fuzzerVisitor = JsFuzzerAstVisitor()
        fuzzerVisitor.accept(funcNode)
        val jsDescription = JsMethodDescription(
            name = funcNode.getAbstractFunctionName(),
            parameters = execId.parameters,
            classId = execId.classId,
            concreteValues = fuzzerVisitor.fuzzedConcreteValues,
            tracer = Trie(JsStatement::number)
        )
        val collectedValues = mutableListOf<List<JsFuzzedValue>>()
        // .location field gets us "jsFile:A:B", then we get A and B as ints
        val funcLocation = funcNode.firstChild!!.location.substringAfter("jsFile:")
            .split(":").map { it.toInt() }
        logger.info { "Function under test location according to parser is [${funcLocation[0]}, ${funcLocation[1]}]" }
        val instrService = InstrumentationService(context, funcLocation[0] to funcLocation[1])
        instrService.instrument()
        val coverageProvider = CoverageServiceProvider(
            context,
            instrService,
            context.settings.coverageMode,
            jsDescription
        )
        val allStmts = instrService.allStatements
        logger.info { "Statements to cover: (${allStmts.joinToString { toString() }})" }
        val currentlyCoveredStmts = mutableSetOf<Int>()
        val startTime = System.currentTimeMillis()
        runFuzzing(jsDescription) { description, values ->
            if (isCancelled() || System.currentTimeMillis() - startTime > fuzzingTimeout)
                return@runFuzzing JsFeedback(Control.STOP)
            collectedValues += values
            if (collectedValues.size >= if (context.settings.coverageMode == CoverageMode.FAST) fuzzingThreshold else 1) {
                try {
                    val (coveredStmts, executionResults) = coverageProvider.get(
                        collectedValues,
                        execId
                    )
                    coveredStmts.zip(executionResults).forEach { (covData, resultData) ->
                        val params = collectedValues[resultData.index]
                        val result =
                            getUtModelResult(
                                execId = execId,
                                resultData = resultData,
                                jsDescription.thisInstance?.let { params.drop(1) } ?: params
                            )
                        if (result is UtTimeoutException) {
                            emit(JsTimeoutExecution(result))
                            return@runFuzzing JsFeedback(Control.PASS)
                        } else if (!currentlyCoveredStmts.containsAll(covData.additionalCoverage)) {
                            val (thisObject, modelList) = if (!funcNode.parent!!.isClassMembers) {
                                null to params.map { it.model }
                            } else params[0].model to params.drop(1).map { it.model }
                            val initEnv =
                                EnvironmentModels(thisObject, modelList, mapOf())
                            emit(
                                JsValidExecution(
                                    JsUtFuzzedExecution(
                                        stateBefore = initEnv,
                                        stateAfter = initEnv,
                                        result = result,
                                    )
                                )
                            )
                            currentlyCoveredStmts += covData.additionalCoverage
                            val trieNode = description.tracer.add(covData.additionalCoverage.map { JsStatement(it) })
                            return@runFuzzing JsFeedback(control = Control.CONTINUE, result = trieNode)
                        }
                        if (currentlyCoveredStmts.containsAll(allStmts)) return@runFuzzing JsFeedback(Control.STOP)
                    }
                } catch (e: TimeoutException) {
                    emit(
                        JsTimeoutExecution(
                            UtTimeoutException(
                                TimeoutException("Timeout on unknown test case. Consider using \"Basic\" coverage mode")
                            )
                        )
                    )
                    return@runFuzzing JsFeedback(Control.STOP)
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
        execId: JsMethodId,
        packageJson: PackageJson
    ) {
        val obligatoryExport = (classNode?.getClassName() ?: funcNode.getAbstractFunctionName()).toString()
        val collectedExports = collectExports(execId)
        val exportsProvider = IExportsProvider.providerByPackageJson(packageJson)
        exports += (collectedExports + obligatoryExport)
        exportsManager(exports.toList()) { existingSection ->
            val existingExportsSet = existingSection?.let { section ->
                val trimmedSection = section.substringAfter(exportsProvider.exportsPrefix)
                    .substringBeforeLast(exportsProvider.exportsPostfix)
                val exportRegex = exportsProvider.exportsRegex
                val existingExports = trimmedSection.split(exportsProvider.exportsDelimiter)
                    .filter { it.contains(exportRegex) && it.isNotBlank() }
                existingExports.map { rawLine ->
                    exportRegex.find(rawLine)?.groups?.get(1)?.value ?: throw IllegalStateException()
                }.toSet()
            } ?: emptySet()
            val resultSet = existingExportsSet + exports.toSet()
            val resSection = resultSet.joinToString(
                separator = exportsProvider.exportsDelimiter,
                prefix = exportsProvider.exportsPrefix,
                postfix = exportsProvider.exportsPostfix,
            ) {
                exportsProvider.getExportsFrame(it)
            }
            existingSection?.let { fileText.replace(existingSection, resSection) } ?: resSection
        }
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
        } ?: jsUndefinedClassId.constructClass(
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
            if (it.isExportable && !astScrapper.importsMap.contains(it.name)) {
                res += it.name
            }
        }
        if (methodId.returnType.isExportable && !astScrapper.importsMap.contains(methodId.returnType.name))
            res += methodId.returnType.name
        return res
    }

    private fun getFunctionNode(focusedMethodName: String, parentClassName: String?): Node {
        return parentClassName?.let { astScrapper.findMethod(parentClassName, focusedMethodName, parsedFile) }
            ?: astScrapper.findFunction(focusedMethodName, parsedFile)
            ?: throw IllegalStateException(
                "Couldn't locate function \"$focusedMethodName\" with class ${parentClassName ?: ""}"
            )
    }

    private fun getMethodsToTest() =
        parentClassName?.let {
            getClassMethods(it)
        } ?: extractToplevelFunctions().ifEmpty {
            getClassMethods("")
        }

    private fun getClassMethods(className: String): List<Node> {
        val classNode = astScrapper.findClass(className, parsedFile)
        return classNode?.getClassMethods() ?: throw IllegalStateException("Can't extract methods of class $className")
    }
}
