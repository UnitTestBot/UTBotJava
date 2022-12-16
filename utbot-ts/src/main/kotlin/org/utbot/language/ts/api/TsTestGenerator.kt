package org.utbot.language.ts.api


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
import org.utbot.language.ts.codegen.TsCodeGenerator
import org.utbot.language.ts.framework.api.ts.TsClassId
import org.utbot.language.ts.framework.api.ts.TsFieldId
import org.utbot.language.ts.framework.api.ts.TsMethodId
import org.utbot.language.ts.framework.api.ts.util.tsErrorClassId
import org.utbot.language.ts.framework.api.ts.util.tsStringClassId
import org.utbot.language.ts.fuzzer.TsFuzzer
import org.utbot.language.ts.fuzzer.providers.TsObjectModelProvider
import org.utbot.language.ts.parser.TSAstScrapper
import org.utbot.language.ts.parser.TsParser
import org.utbot.language.ts.parser.TsParserUtils
import org.utbot.language.ts.parser.ast.AstNode
import org.utbot.language.ts.parser.ast.ClassDeclarationNode
import org.utbot.language.ts.parser.ast.FunctionNode
import org.utbot.language.ts.parser.ast.PropertyDeclarationNode
import org.utbot.language.ts.parser.visitors.TsClassAstVisitor
import org.utbot.language.ts.parser.visitors.TsFunctionAstVisitor
import org.utbot.language.ts.parser.visitors.TsFuzzerAstVisitor
import org.utbot.language.ts.parser.visitors.TsImportsAstVisitor
import org.utbot.language.ts.parser.visitors.TsToplevelFunctionAstVisitor
import org.utbot.language.ts.service.TsCoverageServiceProvider
import org.utbot.language.ts.service.TsServiceContext
import org.utbot.language.ts.service.TsWorkMode
import org.utbot.language.ts.settings.TsDynamicSettings
import org.utbot.language.ts.utils.constructClass
import org.utbot.language.ts.utils.makeTsClassIdFromType
import org.utbot.language.ts.utils.toTsAny


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
        val statics = settings.godObject?.let { line ->
            val pathToObjectFile = line.substringBeforeLast(".")
            val objectName = line.substringAfterLast(".")
            val parsedFile = tsParser.parse(File(pathToObjectFile).readText())
            val godObject = TSAstScrapper(parsedFile).findClass(objectName) ?: throw IllegalStateException()
            val proc = TsUIProcessor()
            proc.traverseCallGraph(parsedFile, godObject)
            proc.staticSet
        }
        parentClassName = classNode?.name
        val classId = makeTsClassId(classNode, context)
        val methods = makeMethodsToTest()
        if (methods.isEmpty()) throw IllegalArgumentException("No methods to test were found!")
        methods.forEach { funcNode ->
            makeTestsForMethod(classId, funcNode, classNode, context, testSets, paramNames, statics ?: emptySet())
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
        paramNames: MutableMap<ExecutableId, List<String>>,
        statics: Set<PropertyDeclarationNode>
    ) {
        val execId = classId.allMethods.find {
            it.name == funcNode.name.value
        } ?: throw IllegalStateException()
        val (concreteValues, fuzzedValues) = runFuzzer(funcNode, execId, statics, context)
        val (allCoveredStatements, executionResults) =
            TsCoverageServiceProvider(context).get(
                settings.coverageMode,
                fuzzedValues,
                execId,
                classNode,
                statics
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
                    returnText = executionResults[paramIndex],
                    mode = settings.workMode
                )
            val thisInstance = makeThisInstance(execId, classId, concreteValues)
            val initEnv = when (context.settings.workMode) {
                TsWorkMode.EXPERIMENTAL -> EnvironmentModels(thisInstance, emptyList(), buildMap {
                    statics.forEachIndexed { index, st ->
                        this[TsFieldId(TsClassId(st.parentClass.value.name), st.name, st.type.makeTsClassIdFromType(context))] = param[index].model
                    }
                })
                else -> EnvironmentModels(thisInstance, param.map { it.model }, mapOf())
            }
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
        returnText: String,
        mode: TsWorkMode
    ): UtExecutionResult {
        val (returnValue, valueClassId) = when (mode) {
            TsWorkMode.PLANE -> returnText.toTsAny(execId.returnType)
            TsWorkMode.EXPERIMENTAL -> returnText to tsStringClassId
        }
        val result = TsUtModelConstructor(settings).construct(returnValue, valueClassId)
        return when (result.classId) {
            tsErrorClassId -> UtExplicitlyThrownException(Throwable(returnValue.toString()), false)
            else -> UtExecutionSuccess(result)
        }
    }

    private fun runFuzzer(
        funcNode: FunctionNode,
        execId: TsMethodId,
        statics: Set<PropertyDeclarationNode> = emptySet(),
        serviceContext: TsServiceContext,
    ): Pair<Set<FuzzedConcreteValue>, List<List<FuzzedValue>>> {
        when (settings.workMode) {
            TsWorkMode.PLANE -> {
                val fuzzerVisitor = TsFuzzerAstVisitor()
                fuzzerVisitor.accept(funcNode)
                val methodUnderTestDescription =
                    FuzzedMethodDescription(execId, fuzzerVisitor.fuzzedConcreteValues).apply {
                        compilableName = funcNode.name.value
                        val names = funcNode.parameters.map { it.name }
                        parameterNameMap = { index -> names.getOrNull(index) }
                    }
                val fuzzedValues =
                    TsFuzzer.tsFuzzing(methodUnderTestDescription = methodUnderTestDescription).toList()
                return fuzzerVisitor.fuzzedConcreteValues.toSet() to fuzzedValues
            }
            TsWorkMode.EXPERIMENTAL -> {
                val newExecId = TsMethodId(
                    classId = execId.classId,
                    name = execId.name,
                    returnType = execId.returnType,
                    parameters = statics.map { stat -> stat.type.makeTsClassIdFromType(serviceContext) }
                )
                val methodUnderTestDescription =
                    FuzzedMethodDescription(newExecId, emptySet()).apply {
                        compilableName = funcNode.name.value
                        val names = statics.map { it.name }
                        parameterNameMap = { index -> names.getOrNull(index) }
                    }
                val fuzzedValues =
                    TsFuzzer.tsFuzzing(methodUnderTestDescription = methodUnderTestDescription).toList()
                return emptySet<FuzzedConcreteValue>() to fuzzedValues
            }
        }

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
