package parser

import com.oracle.js.parser.ErrorManager
import com.oracle.js.parser.Parser
import com.oracle.js.parser.ScriptEnvironment
import com.oracle.js.parser.Source
import com.oracle.js.parser.ir.ClassNode

object JsParserUtils {

    // TODO SEVERE: function only works in the same file scope. Add search in exports.
    fun searchForClassDecl(className: String?, fileText: String, strict: Boolean = false): ClassNode? {
        val parser = Parser(
            ScriptEnvironment.builder().build(),
            Source.sourceFor("jsFile", fileText),
            ErrorManager.ThrowErrorManager()
        )
        val fileNode = parser.parse()
        val visitor = JsClassAstVisitor(className)
        fileNode.accept(visitor)
        return try {
            visitor.targetClassNode
        } catch (e: Exception) {
            if (!strict && visitor.classNodesCount == 1) {
                visitor.atLeastSomeClassNode
            } else null
        }
    }
}