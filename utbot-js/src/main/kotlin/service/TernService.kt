package service

import com.oracle.js.parser.ir.ClassNode
import com.oracle.js.parser.ir.FunctionNode
import org.json.JSONException
import org.json.JSONObject
import org.utbot.framework.plugin.api.js.JsClassId
import org.utbot.framework.plugin.api.js.JsMultipleClassId
import org.utbot.framework.plugin.api.js.util.jsUndefinedClassId
import parser.JsParserUtils
import utils.JsCmdExec
import utils.MethodTypes
import utils.constructClass
import java.io.File
import java.nio.charset.Charset
import java.util.Locale

/*
    NOTE: this approach is quite bad, but we failed to implement alternatives.
    TODO: 1. MINOR: Find a better solution after the first stable version.
          2. SEVERE: Load all necessary .js files in Tern.js since functions can be exported and used in other files.
 */

/**
 * Installs and sets up scripts for running Tern.js type guesser.
 */
class TernService(val context: ServiceContext) {


    private fun ternScriptCode() = """
const tern = require("tern/lib/tern")
const condense = require("tern/lib/condense.js")
const util = require("tern/test/util.js")
const fs = require("fs")
const path = require("path")

var condenseDir = "";

function runTest(options) {

    var server = new tern.Server({
        projectDir: util.resolve(condenseDir),
        defs: [util.ecmascript],
        plugins: options.plugins,
        getFile: function(name) {
            return fs.readFileSync(path.resolve(condenseDir, name), "utf8");
        }
    });
    options.load.forEach(function(file) {
        server.addFile(file)
    });
    server.flush(function() {
        var origins = options.include || options.load;
        var condensed = condense.condense(origins, null, {sortOutput: true});
        var out = JSON.stringify(condensed, null, 2);
        console.log(out)
    });
}

function test(options) {
    if (typeof options == "string") options = {load: [options]};
    runTest(options);
}

test("${context.filePathToInference}")
    """

    init {
        run()
    }

    private lateinit var json: JSONObject

    private fun run() {
        with(context) {
            setupTernEnv("$projectPath${File.separator}$utbotDir")
            installDeps("$projectPath${File.separator}$utbotDir")
            runTypeInferencer()
        }
    }

    private fun installDeps(path: String) {
        JsCmdExec.runCommand(
            cmd = "npm install tern -l",
            dir = path,
        )
    }

    private fun setupTernEnv(path: String) {
        File(path).mkdirs()
        val ternScriptFile = File("$path${File.separator}ternScript.js")
        ternScriptFile.writeText(ternScriptCode(), Charset.defaultCharset())
    }

    private fun runTypeInferencer() {
        with(context) {
            val (reader, _) = JsCmdExec.runCommand(
                cmd = "node ${projectPath}${File.separator}$utbotDir${File.separator}ternScript.js",
                dir = "$projectPath${File.separator}$utbotDir${File.separator}",
                shouldWait = true,
                timeout = 20
            )
            val text = reader.readText().replaceAfterLast("}", "")
            json = try {
                JSONObject(text)
            } catch (_: Throwable) {
                JSONObject()
            }
        }
    }

    fun processConstructor(classNode: ClassNode): List<JsClassId> {
        return try {
            val classJson = json.getJSONObject(classNode.ident.name.toString())
            val constructorFunc = classJson.getString("!type")
                .filterNot { setOf(' ', '+', '!').contains(it) }
            extractParameters(constructorFunc)
        } catch (e: JSONException) {
            (classNode.constructor.value as FunctionNode).parameters.map { jsUndefinedClassId }
        }
    }

    private fun extractParameters(line: String): List<JsClassId> {
        val parametersRegex = Regex("fn[(](.+)[)]")
        return parametersRegex.find(line)?.groups?.get(1)?.let { matchResult ->
            val value = matchResult.value
            val paramList = value.split(',')
            paramList.map { param ->
                val paramReg = Regex(":(.*)")
                try {
                    makeClassId(
                        paramReg.find(param)?.groups?.get(1)?.value
                            ?: throw IllegalStateException()
                    )
                } catch (t: Throwable) {
                    jsUndefinedClassId
                }
            }
        } ?: emptyList()
    }

    private fun extractReturnType(line: String): JsClassId {
        val returnTypeRegex = Regex("->(.*)")
        return returnTypeRegex.find(line)?.groups?.get(1)?.let { matchResult ->
            val value = matchResult.value
            try {
                makeClassId(value)
            } catch (t: Throwable) {
                jsUndefinedClassId
            }
        } ?: jsUndefinedClassId
    }

    fun processMethod(className: String?, funcNode: FunctionNode, isToplevel: Boolean = false): MethodTypes {
        // Js doesn't support nested classes, so if the function is not top-level, then we can check for only one parent class.
        try {
            var scope = className?.let {
                if (!isToplevel) json.getJSONObject(it) else json
            } ?: json
            try {
                scope.getJSONObject(funcNode.name.toString())
            } catch (e: JSONException) {
                scope = scope.getJSONObject("prototype")
            }
            val methodJson = scope.getJSONObject(funcNode.name.toString())
            val typesString = methodJson.getString("!type")
                .filterNot { setOf(' ', '+', '!').contains(it) }
            val parametersList = lazy { extractParameters(typesString) }
            val returnType = lazy { extractReturnType(typesString) }

            return MethodTypes(parametersList, returnType)
        } catch (e: Exception) {
            return MethodTypes(
                lazy { funcNode.parameters.map { jsUndefinedClassId } },
                lazy { jsUndefinedClassId }
            )
        }
    }

    //TODO MINOR: move to appropriate place (JsIdUtil or JsClassId constructor)
    private fun makeClassId(name: String): JsClassId {
        val classId = when {
            // TODO SEVERE: I don't know why Tern sometimes says that type is "0"
            name == "?" || name.toIntOrNull() != null -> jsUndefinedClassId
            Regex("\\[(.*)]").matches(name) -> {
                val arrType = Regex("\\[(.*)]").find(name)?.groups?.get(1)?.value ?: throw IllegalStateException()
                JsClassId(
                    jsName = "array",
                    elementClassId = makeClassId(arrType)
                )
            }

            name.contains('|') -> JsMultipleClassId(name.lowercase(Locale.getDefault()))
            else -> JsClassId(name.lowercase(Locale.getDefault()))
        }

        return try {
            val classNode = JsParserUtils.searchForClassDecl(
                className = name,
                fileText = context.fileText ?: context.trimmedFileText,
                strict = true,
            )
            classNode?.let {
                JsClassId(name).constructClass(this, it)
            } ?: classId
        } catch (e: Exception) {
            classId
        }
    }
}