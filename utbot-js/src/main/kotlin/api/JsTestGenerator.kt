package api

import codegen.JsCodeGenerator
import com.oracle.js.parser.ErrorManager
import com.oracle.js.parser.Parser
import com.oracle.js.parser.ScriptEnvironment
import com.oracle.js.parser.Source
import com.oracle.js.parser.ir.ClassNode
import com.oracle.js.parser.ir.FunctionNode
import com.oracle.truffle.api.strings.TruffleString
import fuzzer.JsFuzzer
import fuzzer.providers.JsObjectModelProvider
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
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.js.JsClassId
import org.utbot.framework.plugin.api.js.JsMethodId
import org.utbot.framework.plugin.api.js.JsMultipleClassId
import org.utbot.framework.plugin.api.js.JsPrimitiveModel
import org.utbot.framework.plugin.api.js.util.isJsBasic
import org.utbot.framework.plugin.api.js.util.jsErrorClassId
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
import parser.JsClassAstVisitor
import parser.JsFunctionAstVisitor
import parser.JsFuzzerAstVisitor
import parser.JsParserUtils
import parser.JsToplevelFunctionAstVisitor
import service.CoverageService
import service.ServiceContext
import service.TernService
import settings.JsTestGenerationSettings.dummyClassName
import settings.JsTestGenerationSettings.fileUnderTestAliases
import settings.JsTestGenerationSettings.functionCallResultAnchor
import settings.JsTestGenerationSettings.tempFileName
import utils.JsCmdExec
import utils.PathResolver
import utils.constructClass
import utils.toJsAny
import java.io.File
import java.util.Collections


class JsTestGenerator(
    private val fileText: String,
    private var sourceFilePath: String,
    private var projectPath: String = sourceFilePath.replaceAfterLast(File.separator, ""),
    private val selectedMethods: List<String>? = null,
    private var parentClassName: String? = null,
    private var outputFilePath: String?,
    private val exportsManager: (List<String>) -> Unit,
    private val timeout: Long,
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
            nodeTimeout = timeout,
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
            } catch (_: Exception) {

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
        val coveredBranchesArray = Array<Set<Int>>(fuzzedValues.size) { emptySet() }
        val timeoutErrors =
            runCoverageAnalysis(context, fuzzedValues, execId, classNode, coveredBranchesArray)
        val testsForGenerator = mutableListOf<UtExecution>()
        val resultRegex = Regex("$functionCallResultAnchor (.*)")
        val errorResultRegex = Regex(".*(Error: .*)")
        val illegalStateExceptionError = if (timeoutErrors.size == fuzzedValues.size)
            mapOf("No successful tests were generated! Please check the function under test." to 1)
        else
            emptyMap()
        val errorsForGenerator = timeoutErrors.associate {
            "Timeout in generating test for ${fuzzedValues[it].joinToString { f -> f.model.toString() }} parameters" to 1
        } + illegalStateExceptionError
        analyzeCoverage(coveredBranchesArray.toList()).forEach { paramIndex ->
            val param = fuzzedValues[paramIndex]
            val result =
                getUtModelResult(param, execId, classNode, File(sourceFilePath).name, resultRegex, errorResultRegex)
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
        val importPrefix = outputFilePath?.let {
            PathResolver.getRelativePath(
                File(it).parent,
                File(sourceFilePath).parent,
            )
        } ?: ""
        return importPrefix
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
        param: List<FuzzedValue>,
        execId: JsMethodId,
        classNode: ClassNode?,
        importText: String,
        resultRegex: Regex,
        errorResultRegex: Regex
    ): UtExecutionResult {
        val utConstructor = JsUtModelConstructor()
        val scriptText =
            makeStringForRunJs(param, execId, classNode?.ident?.name, importText)
        val returnText = runJs(
            scriptText,
            File(sourceFilePath).parent,
        )
        val unparsedValueSeq = resultRegex.findAll(returnText)
        val errorSeq = errorResultRegex.findAll(returnText)
        val unparsedValue = if (unparsedValueSeq.any()) {
            unparsedValueSeq.last().groups[1]?.value ?: throw IllegalStateException()
        } else {
            errorSeq.last().groups[1]?.value ?: throw IllegalStateException()
        }
        val (returnValue, valueClassId) = unparsedValue.toJsAny(execId.returnType)
        val result = utConstructor.construct(returnValue, valueClassId)
        val utExecResult = when (result.classId) {
            jsErrorClassId -> UtExplicitlyThrownException(Throwable(returnValue.toString()), false)
            else -> UtExecutionSuccess(result)
        }
        return utExecResult
    }

    private fun runCoverageAnalysis(
        context: ServiceContext,
        fuzzedValues: List<List<FuzzedValue>>,
        execId: JsMethodId,
        classNode: ClassNode?,
        coveredBranchesArray: Array<Set<Int>>
    ): MutableList<Int> {
        val timeoutErrors = Collections.synchronizedList(mutableListOf<Int>())
        val basicCoverageService = CoverageService(
            context = context,
            scriptText = context.trimmedFileText,
            id = 1024,
            originalFileName = File(sourceFilePath).name,
            newFileName = File(sourceFilePath).name,
            errors = mutableListOf()
        )
        val basicCoverage = basicCoverageService.getCoveredLines()
        basicCoverageService.removeTempFiles()
        fuzzedValues.indices.toList().parallelStream().forEach {
            val scriptText =
                makeStringForRunJs(
                    fuzzedValue = fuzzedValues[it],
                    method = execId,
                    containingClass = classNode?.ident?.name,
                    importText = File(context.filePathToInference).name,
                )
            val coverageService = CoverageService(
                context = context,
                scriptText = scriptText,
                id = it,
                originalFileName = File(sourceFilePath).name,
                newFileName = tempFileName,
                basicCoverage = basicCoverage,
                errors = timeoutErrors,
            )
            coveredBranchesArray[it] = coverageService.getCoveredLines().toSet()
            coverageService.removeTempFiles()
        }
        return timeoutErrors
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
                .shuffled()
                .take(1_000)
        return Pair(fuzzerVisitor.fuzzedConcreteValues.toSet(), fuzzedValues)
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
        val methods = selectedMethods?.map {
            getFunctionNode(
                focusedMethodName = it,
                parentClassName = parentClassName,
                fileText = fileText
            )
        } ?: getMethodsToTest()
        return methods
    }

    private fun makeJsClassId(
        classNode: ClassNode?,
        ternService: TernService
    ): JsClassId {
        val classId = classNode?.let {
            JsClassId(parentClassName!!).constructClass(ternService, classNode)
        } ?: JsClassId("undefined").constructClass(
            ternService = ternService,
            functions = extractToplevelFunctions()
        )
        return classId
    }

    private fun runParser(fileText: String): FunctionNode {
        val parser = Parser(
            ScriptEnvironment.builder().build(),
            Source.sourceFor("jsFile", fileText),
            ErrorManager.ThrowErrorManager()
        )
        return parser.parse()
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

    private fun runJs(scriptText: String, workDir: String): String {
        val (reader, errorReader) = JsCmdExec.runCommand(
            cmd = "node -e \"$scriptText\"",
            dir = workDir,
            shouldWait = true,
            timeout = timeout
        )
        return errorReader.readText().ifEmpty { reader.readText() }
    }

    private fun makeStringForRunJs(
        fuzzedValue: List<FuzzedValue>,
        method: JsMethodId,
        containingClass: TruffleString?,
        importText: String,
    ): String {
        val callString = makeCallFunctionString(fuzzedValue, method, containingClass)
        val prefix = functionCallResultAnchor
        val temp = "console.log(`$prefix \\\"\${res}\\\"`)"
        return "const $fileUnderTestAliases = require(\\\"./$importText\\\"); " +
                "let prefix = \\\"$prefix\\\"; " +
                "let res = $callString; " +
                "if (typeof res == \\\"string\\\") {$temp} else console.log(prefix, res)"
    }

    private fun makeCallFunctionString(
        fuzzedValue: List<FuzzedValue>,
        method: JsMethodId,
        containingClass: TruffleString?
    ): String {
        val initClass = containingClass?.let {
            if (!method.isStatic) {
                "new $fileUnderTestAliases.${it}()."
            } else "$fileUnderTestAliases.$it."
        } ?: "$fileUnderTestAliases."
        var callString = "$initClass${method.name}"
        callString += fuzzedValue.joinToString(
            prefix = "(",
            postfix = ")",
        ) { value -> value.model.toCallString() }
        return callString
    }

    private fun Any.quoteWrapIfNecessary(): String =
        when (this) {
            is String -> "\"$this\""
            else -> "$this"
        }

    private fun UtAssembleModel.toParamString(): String =
        with(this) {
            val callConstructorString = "new $fileUnderTestAliases.${classId.name}"
            val paramsString = instantiationCall.params.joinToString(
                prefix = "(",
                postfix = ")",
            ) {
                (it as JsPrimitiveModel).value.quoteWrapIfNecessary()
            }
            return callConstructorString + paramsString
        }

    private fun UtModel.toCallString(): String =
        when (this) {
            is UtAssembleModel -> this.toParamString()
            else -> {
                (this as JsPrimitiveModel).value.quoteWrapIfNecessary()
            }
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