package api

import codegen.TsCodeGenerator
import framework.api.ts.TsClassId
import framework.api.ts.TsMethodId
import framework.api.ts.util.tsErrorClassId
import fuzzer.TsFuzzer
import fuzzer.providers.TsObjectModelProvider
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
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
import parser.TsParser
import parser.TsParserUtils
import parser.ast.AstNode
import parser.ast.ClassDeclarationNode
import parser.ast.FunctionNode
import parser.visitors.TsClassAstVisitor
import parser.visitors.TsFunctionAstVisitor
import parser.visitors.TsFuzzerAstVisitor
import parser.visitors.TsImportsAstVisitor
import parser.visitors.TsToplevelFunctionAstVisitor
import service.TsCoverageServiceProvider
import service.TsServiceContext
import settings.TsDynamicSettings
import utils.constructClass
import utils.toTsAny

class TsTestGenerator(
    private var sourceFilePath: String,
    private var projectPath: String = sourceFilePath.substringBeforeLast("/"),
    private val selectedMethods: List<String>? = null,
    private var parentClassName: String? = null,
    private var outputFilePath: String?,
    private val settings: TsDynamicSettings,
) {

    private lateinit var parsedFile: AstNode

    private val utbotDir = "utbotTs"

    init {
        println("My source file path: $sourceFilePath")
        println("My project path: $projectPath")
        println("My output file path: $outputFilePath")

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
        val tsParser = TsParser(File(settings.tsModulePath))
        parsedFile = tsParser.parse(File(sourceFilePath).readText())
        val importsVisitor = TsImportsAstVisitor(sourceFilePath, tsParser)
        importsVisitor.accept(parsedFile)
        val context = TsServiceContext(
            utbotDir = utbotDir,
            projectPath = projectPath,
            filePathToInference = sourceFilePath,
            parsedFile = parsedFile,
            settings = settings,
            imports = importsVisitor.importObjects,
            parsedFiles = importsVisitor.parsedFiles
        )
        val paramNames = mutableMapOf<ExecutableId, List<String>>()
        val testSets = mutableListOf<CgMethodTestSet>()
        val classNode =
            TsParserUtils.searchForClassDecl(
                className = parentClassName,
                parsedFile = parsedFile,
                strict = selectedMethods?.isNotEmpty() ?: false,
                parsedImportedFiles = importsVisitor.parsedFiles,
            )
        parentClassName = classNode?.name
        val classId = makeTsClassId(classNode, context)
        val methods = makeMethodsToTest()
        if (methods.isEmpty()) throw IllegalArgumentException("No methods to test were found!")
        methods.forEach { funcNode ->
            makeTestsForMethod(classId, funcNode, classNode, context, testSets, paramNames)
        }
        val codeGen = TsCodeGenerator(
            classUnderTest = classId,
            paramNames = paramNames,
        )
        return codeGen.generateAsStringWithTestReport(testSets).generatedCode
    }

    private fun makeTestsForMethod(
        classId: TsClassId,
        funcNode: FunctionNode,
        classNode: ClassDeclarationNode?,
        context: TsServiceContext,
        testSets: MutableList<CgMethodTestSet>,
        paramNames: MutableMap<ExecutableId, List<String>>
    ) {
        val execId = classId.allMethods.find {
            it.name == funcNode.name
        } ?: throw IllegalStateException()
        val (concreteValues, fuzzedValues) = runFuzzer(funcNode, execId)
        val (allCoveredStatements, executionResults) =
            TsCoverageServiceProvider(context).get(
                settings.coverageMode,
                fuzzedValues,
                execId,
                classNode
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
            execId,
            testsForGenerator,
            errorsForGenerator
        )
        testSets += testSet
        paramNames[execId] = funcNode.parameters.map { it.name }
    }

    private fun makeThisInstance(
        execId: TsMethodId,
        classId: TsClassId,
        concreteValues: Set<FuzzedConcreteValue>
    ): UtModel? {
        val thisInstance = when {
            execId.isStatic -> null
            classId.allConstructors.first().parameters.isEmpty() -> {
                val id = TsObjectModelProvider.idGenerator.asInt
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
                TsObjectModelProvider.generate(
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
        execId: TsMethodId,
        returnText: String
    ): UtExecutionResult {
        val (returnValue, valueClassId) = returnText.toTsAny(execId.returnType)
        val result = TsUtModelConstructor().construct(returnValue, valueClassId)
        return when (result.classId) {
            tsErrorClassId -> UtExplicitlyThrownException(Throwable(returnValue.toString()), false)
            else -> UtExecutionSuccess(result)
        }
    }

    private fun runFuzzer(
        funcNode: FunctionNode,
        execId: TsMethodId
    ): Pair<Set<FuzzedConcreteValue>, List<List<FuzzedValue>>> {
        val fuzzerVisitor = TsFuzzerAstVisitor()
        fuzzerVisitor.accept(funcNode)
        val methodUnderTestDescription =
            FuzzedMethodDescription(execId, fuzzerVisitor.fuzzedConcreteValues).apply {
                compilableName = funcNode.name
                val names = funcNode.parameters.map { it.name }
                parameterNameMap = { index -> names.getOrNull(index) }
            }
        val fuzzedValues =
            TsFuzzer.tsFuzzing(methodUnderTestDescription = methodUnderTestDescription).toList()
        return fuzzerVisitor.fuzzedConcreteValues.toSet() to fuzzedValues
    }

    private fun makeMethodsToTest(): List<FunctionNode> =
        selectedMethods?.map {
            getFunctionNode(
                focusedMethodName = it,
                parentClassName = parentClassName,
            )
        } ?: getMethodsToTest()

    private fun makeTsClassId(classNode: ClassDeclarationNode?, serviceContext: TsServiceContext): TsClassId =
        classNode?.let {
            TsClassId(parentClassName!!).constructClass(
                classNode = classNode,
                serviceContext = serviceContext
            )
        } ?: TsClassId("undefined").constructClass(
            functions = extractToplevelFunctions(),
            serviceContext = serviceContext
        )

    private fun extractToplevelFunctions(): List<FunctionNode> {
        val visitor = TsToplevelFunctionAstVisitor()
        visitor.accept(parsedFile)
        return visitor.extractedMethods
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

    private fun getFunctionNode(focusedMethodName: String, parentClassName: String?): FunctionNode {
        val visitor = TsFunctionAstVisitor(
            focusedMethodName,
            parentClassName
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

    private fun getClassMethods(className: String): List<FunctionNode> {
        val visitor = TsClassAstVisitor(className)
        visitor.accept(parsedFile)
        val classNode = TsParserUtils.searchForClassDecl(
            className = className,
            parsedFile = parsedFile,
        )
        return classNode?.methods ?: throw IllegalStateException("Can't extract methods of class $className")
    }
}
