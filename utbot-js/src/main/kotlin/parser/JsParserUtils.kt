package parser

import com.oracle.js.parser.ir.ClassNode
import com.oracle.js.parser.ir.FunctionNode

object JsParserUtils {

    // TODO SEVERE: function only works in the same file scope. Add search in exports.
    fun searchForClassDecl(className: String?, parsedFile: FunctionNode, strict: Boolean = false): ClassNode? {
        val visitor = JsClassAstVisitor(className)
        parsedFile.accept(visitor)
        return try {
            visitor.targetClassNode
        } catch (e: Exception) {
            if (!strict && visitor.classNodesCount == 1) {
                visitor.atLeastSomeClassNode
            } else null
        }
    }
}