package service

import com.google.javascript.jscomp.CodePrinter
import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.rhino.Node
import org.apache.commons.io.FileUtils
import parser.visitors.JsFunctionAstVisitor
import parser.JsParserUtils.getAnyValue
import parser.JsParserUtils.getRequireImportText
import parser.JsParserUtils.isRequireImport
import parser.JsParserUtils.runParser
import utils.JsCmdExec
import utils.PathResolver.getRelativePath
import java.io.File
import java.nio.file.Paths
import parser.JsParserUtils.getModuleImportText
import providers.exports.IExportsProvider
import kotlin.io.path.pathString
import kotlin.math.roundToInt

class InstrumentationService(context: ServiceContext, private val funcDeclOffset: Pair<Int, Int>) :
    ContextOwner by context {

    private val destinationFolderPath = "${projectPath}/${utbotDir}/instr"
    private val instrumentedFilePath = "$destinationFolderPath/${filePathToInference.first().substringAfterLast("/")}"
    private lateinit var parsedInstrFile: Node
    lateinit var covFunName: String

    val allStatements: Set<Int>
        get() = getStatementMapKeys()

    private data class Location(
        val start: Pair<Int, Int>,
        val end: Pair<Int, Int>
    )


    /*
        Extension functions below are used to parse instrumented file's block that looks like this:

        fnMap: {
            "0": {
                name: "testFunction",
                decl: {start: {line: 4, column: 9}, end: {line: 4, column: 12}},
                loc: {start: {line: 4, column: 20}, end: {line: 9, column: 1}},
                line: 4
            }
        }

         Node.getObjectFirstKey() returns "0" Node if called on "fnMap" Node.
         Node.getObjectField("loc") returns loc Node if called on "0" Node.
         Node.getObjectValue() returns 4 if called on any "line" Node.
         Node.getObjectLocation("decl") returns Location class ((4, 9), (4, 12)) if called on "0" Node.
     */
    private fun Node.getObjectFirstKey(): Node = this.firstFirstChild
        ?: throw IllegalStateException("Node doesn't have child of child")

    private fun Node.getObjectField(fieldName: String): Node? {
        var fieldNode: Node? = this.getObjectFirstKey()
        do {
            if (fieldNode?.string == fieldName) return fieldNode
            fieldNode = fieldNode?.next
        } while (fieldNode != null)
        return null
    }

    private fun Node.getObjectValue(): Any = this.firstChild?.getAnyValue()
        ?: throw IllegalStateException("Can't get Node's simple value")

    private fun Node.getObjectLocation(locKey: String?): Location {
        val generalField = locKey?.let { this.getObjectField(it) } ?: this
        val startField = generalField.getObjectFirstKey()
        val endField = startField.next!!
        return Location(
            startField.getLineValue() to startField.getColumnValue(),
            endField.getLineValue() to endField.getColumnValue()
        )
    }

    private fun Node.getLineValue(): Int = this.getObjectFirstKey().getObjectValue()
        .toString()
        .toFloat()
        .roundToInt()

    private fun Node.getColumnValue(): Int = this.getObjectFirstKey().next!!
        .getObjectValue()
        .toString()
        .toFloat()
        .roundToInt()

    private fun Node.findAndIterateOver(key: String, func: (Node?) -> Unit) {
        NodeUtil.visitPreOrder(this) { node ->
            if (node.isStringKey && node.string == key) {
                var currKey: Node? = node.firstChild!!.firstChild!!
                do {
                    func(currKey)
                    currKey = currKey?.next
                } while (currKey != null)
                return@visitPreOrder
            }
        }
    }

    private fun getStatementMapKeys() = buildSet {
        val funcVisitor = JsFunctionAstVisitor(covFunName, null)
        funcVisitor.accept(parsedInstrFile)
        val funcNode = funcVisitor.targetFunctionNode
        val funcLocation = getFuncLocation(funcNode)
        funcNode.findAndIterateOver("statementMap") { currKey ->
            operator fun Pair<Int, Int>.compareTo(other: Pair<Int, Int>): Int =
                when {
                    this.first < other.first || (this.first == other.first && this.second <= other.second) -> -1
                    this.first > other.first || (this.first == other.first && this.second >= other.second) -> 1
                    else -> 0
                }

            val stmtLocation = currKey!!.getObjectLocation(null)
            if (funcLocation.start < stmtLocation.start && funcLocation.end > stmtLocation.end)
                add(currKey.string.toInt())
        }
    }

    private fun getFuncLocation(covFuncNode: Node): Location {
        var result = Location(0 to 0, Int.MAX_VALUE to Int.MAX_VALUE)
        covFuncNode.findAndIterateOver("fnMap") { currKey ->
            val declLocation = currKey!!.getObjectLocation("decl")
            if (funcDeclOffset == declLocation.start) {
                result = currKey.getObjectLocation("loc")
                return@findAndIterateOver
            }
        }
        return result
    }

    fun instrument() {
        val fileName = filePathToInference.first().substringAfterLast("/")

        JsCmdExec.runCommand(
            cmd = arrayOf(settings.pathToNYC, "instrument", fileName, destinationFolderPath),
            dir = filePathToInference.first().substringBeforeLast("/"),
            shouldWait = true,
            timeout = settings.timeout,
        )
        val instrumentedFileText = File(instrumentedFilePath).readText()
        parsedInstrFile = runParser(instrumentedFileText, instrumentedFilePath)
        val covFunRegex = Regex("function (cov_.*)\\(\\).*")
        val funName = covFunRegex.find(instrumentedFileText.takeWhile { it != '{' })?.groups?.get(1)?.value
            ?: throw IllegalStateException("")
        val fixedFileText = fixImportsInInstrumentedFile() +
                IExportsProvider.providerByPackageJson(packageJson).instrumentationFunExport(funName)
        File(instrumentedFilePath).writeTextAndUpdate(fixedFileText)

        covFunName = funName
    }

    private fun File.writeTextAndUpdate(newText: String) {
        this.writeText(newText)
        parsedInstrFile = runParser(File(instrumentedFilePath).readText(), instrumentedFilePath)
    }

    private fun fixImportsInInstrumentedFile(): String {
        // nyc poorly handles imports paths in file to instrument. Manual fix required.
        NodeUtil.visitPreOrder(parsedInstrFile) { node ->
            when {
                node.isRequireImport() -> {
                    val currString = node.getRequireImportText()
                    val relPath = Paths.get(
                        getRelativePath(
                            "${projectPath}/${utbotDir}/instr",
                            File(filePathToInference.first()).parent
                        )
                    ).resolve(currString).pathString.replace("\\", "/")
                    node.firstChild!!.next!!.string = relPath
                }
                node.isImport -> {
                    val currString = node.getModuleImportText()
                    val relPath = Paths.get(
                        getRelativePath(
                            "${projectPath}/${utbotDir}/instr",
                            File(filePathToInference.first()).parent
                        )
                    ).resolve(currString).pathString.replace("\\", "/")
                    node.firstChild!!.next!!.next!!.string = relPath
                }
            }
        }
        return CodePrinter.Builder(parsedInstrFile).build()
    }

    fun removeTempFiles() {
        FileUtils.deleteDirectory(File("$projectPath/$utbotDir/instr"))
    }

}
