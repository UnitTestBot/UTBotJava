package service

import com.google.javascript.jscomp.Compiler
import com.google.javascript.jscomp.NodeUtil
import com.google.javascript.jscomp.SourceFile
import java.io.File
import utils.JsCmdExec

class InstrumentationService(context: ServiceContext): ContextOwner by context {

    private val destinationFolderPath = "${projectPath}/${utbotDir}/instr"
    private val instrumentedFilePath = "$destinationFolderPath/${filePathToInference.substringAfterLast("/")}"
    lateinit var covFunName: String

    val allStatements: Set<Int>
        get() = getStatementMapKeys()

    private fun getStatementMapKeys() = buildSet {
        val fileText = File(instrumentedFilePath).readText()
        val rootNode = Compiler().parse(SourceFile.fromCode("jsFile", fileText))
        NodeUtil.visitPreOrder(rootNode) { node ->
            if(node.isStringKey && node.string == "statementMap") {
                var currKey = node.firstChild!!.firstChild!!
                add(currKey.string.toInt())
                while (currKey.next != null) {
                    currKey = currKey.next ?: throw IllegalStateException("Next node can't be null")
                    add(currKey.string.toInt())
                }
            }
        }
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
}