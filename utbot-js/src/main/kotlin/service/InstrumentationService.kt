package service

import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.jscomp.SourceFile
import com.google.javascript.rhino.Node
import java.io.File
import org.apache.commons.io.FileUtils
import parser.JsFunctionAstVisitor
import parser.JsParserUtils.getAnyValue
import utils.JsCmdExec
import kotlin.math.roundToInt

class InstrumentationService(context: ServiceContext, private val funcDeclOffset: Pair<Int, Int>): ContextOwner by context {

    private val destinationFolderPath = "${projectPath}/${utbotDir}/instr"
    private val instrumentedFilePath = "$destinationFolderPath/${filePathToInference.substringAfterLast("/")}"
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
    private fun Node.getObjectFirstKey(): Node = this.firstFirstChild ?: throw IllegalStateException("Node doesn't have child of child")

    private fun Node.getObjectField(fieldName: String): Node? {
        var fieldNode: Node? = this.getObjectFirstKey()
        do {
            if (fieldNode?.string == fieldName) return fieldNode
            fieldNode = fieldNode?.next
        } while (fieldNode != null)
        return null
    }

    private fun Node.getObjectValue(): Any = this.firstChild?.getAnyValue() ?: throw IllegalStateException("Can't get Node's simple value")

    private fun Node.getObjectLocation(locKey: String?): Location {
        val generalField = locKey?.let { this.getObjectField(it)} ?: this
        val startField = generalField.getObjectFirstKey()
        val endField = startField.next!!
        return Location(
            startField.getLineValue() to startField.getColumnValue(),
            endField.getLineValue() to endField.getColumnValue()
        )
    }

    private fun Node.getLineValue(): Int = this.getObjectFirstKey().getObjectValue().toString().toFloat().roundToInt()

    private fun Node.getColumnValue(): Int = this.getObjectFirstKey().next!!.getObjectValue().toString().toFloat().roundToInt()

    private fun Node.findAndIterateOver(key: String, func: (Node?) -> Unit) {
        NodeUtil.visitPreOrder(this) { node ->
            if(node.isStringKey && node.string == key) {
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
        val fileText = File(instrumentedFilePath).readText()
        val rootNode = Compiler().parse(SourceFile.fromCode("jsFile", fileText))
        val funcVisitor = JsFunctionAstVisitor(covFunName, null)
        funcVisitor.accept(rootNode)
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
            if (funcLocation.start < stmtLocation.start && funcLocation.end > stmtLocation.end) add(currKey.string.toInt())
        }
    }

    private fun getFuncLocation(covFuncNode: Node): Location {
        var result = Location(0 to 0, Int.MAX_VALUE to Int.MAX_VALUE)
        covFuncNode.findAndIterateOver("fnMap") { currKey ->
            val declLocation = currKey!!.getObjectLocation("decl")
            if (funcDeclOffset == declLocation.start) {
                result =  currKey.getObjectLocation("loc")
                return@findAndIterateOver
            }
        }
        return result
    }

    fun instrument() {
        val fileName = filePathToInference.substringAfterLast("/")

        JsCmdExec.runCommand(
            cmd = arrayOf(settings.pathToNYC, "instrument", fileName, destinationFolderPath),
            dir = filePathToInference.substringBeforeLast("/"),
            shouldWait = true,
            timeout = settings.timeout,
        )
        val instrumentedFileText = File(instrumentedFilePath).readText()
        val covFunRegex = Regex("function (cov_.*)\\(\\).*")
        val funName = covFunRegex.find(instrumentedFileText.takeWhile { it != '{' })?.groups?.get(1)?.value ?: throw IllegalStateException("")
        val fixedFileText = "$instrumentedFileText\nexports.$funName = $funName"
        File(instrumentedFilePath).writeText(fixedFileText)

        covFunName = funName
    }

    fun removeTempFiles() {
        FileUtils.deleteDirectory(File("$projectPath/$utbotDir/instr"))
    }

}