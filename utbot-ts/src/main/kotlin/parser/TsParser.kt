package parser

import com.eclipsesource.v8.NodeJS
import com.eclipsesource.v8.V8Object
import java.io.File
import parser.TsParserUtils.getAstNodeByKind
import parser.ast.AstNode

class TsParser(pathToTSModule: File) {

    private val typescript: V8Object
    private val compilerOptions: V8Object

    init {
        val nodeJs = NodeJS.createNodeJS()
        typescript = nodeJs
            .require(pathToTSModule)
        val moduleKind = typescript.getObject("ScriptTarget")
        val system = moduleKind.getInteger("Latest")
        compilerOptions = V8Object(nodeJs.runtime)
        compilerOptions.add("module", system)
        TsParserUtils.initParserUtils(typescript, this)
    }

    fun parse(fileText: String): AstNode {
        return (typescript
            .executeJSFunction("createSourceFile", "parsed", fileText, compilerOptions, true)
                as V8Object).getAstNodeByKind(null)
    }
}