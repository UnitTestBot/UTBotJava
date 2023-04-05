package service

import com.google.javascript.rhino.Node
import framework.api.js.JsClassId
import framework.api.js.JsMultipleClassId
import framework.api.js.util.jsUndefinedClassId
import org.json.JSONException
import org.json.JSONObject
import parser.JsParserUtils
import parser.JsParserUtils.getAbstractFunctionName
import parser.JsParserUtils.getAbstractFunctionParams
import parser.JsParserUtils.getClassName
import parser.JsParserUtils.getConstructor
import providers.imports.IImportsProvider
import utils.JsCmdExec
import utils.constructClass
import utils.data.MethodTypes
import java.io.File

/**
 * Installs and sets up scripts for running Tern.js type guesser.
 */
class TernService(val context: ServiceContext) : ContextOwner by context {

    private val importProvider = IImportsProvider.providerByPackageJson(packageJson, context)

    private fun ternScriptCode() = """
${generateImportsSection()}

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
    options = {load: options};
    runTest(options);
}

test(["${filePathToInference.joinToString(separator = "\", \"")}"])
    """

    init {
        with(context) {
            setupTernEnv("$projectPath/$utbotDir")
            runTypeInferencer()
        }
    }

    private lateinit var json: JSONObject

    private fun generateImportsSection(): String = importProvider.ternScriptImports

    private fun setupTernEnv(path: String) {
        File(path).mkdirs()
        val ternScriptFile = File("$path/ternScript.js")
        ternScriptFile.writeText(ternScriptCode())
    }

    private fun runTypeInferencer() {
        val (inputText, _) = JsCmdExec.runCommand(
            dir = "$projectPath/$utbotDir/",
            shouldWait = true,
            timeout = 20,
            cmd = arrayOf("\"${settings.pathToNode}\"", "\"${projectPath}/$utbotDir/ternScript.js\""),
        )
        json = try {
            JSONObject(inputText.replaceAfterLast("}", ""))
        } catch (_: Throwable) {
            JSONObject()
        }
    }

    fun processConstructor(classNode: Node): List<JsClassId> {
        return try {
            val classJson = json.getJSONObject(classNode.getClassName())
            val constructorFunc = classJson.getString("!type")
                .filterNot { setOf(' ', '+').contains(it) }
            extractParameters(constructorFunc)
        } catch (e: JSONException) {
            classNode.getConstructor()?.getAbstractFunctionParams()?.map { jsUndefinedClassId } ?: emptyList()
        }
    }

    private fun extractParameters(line: String): List<JsClassId> {
        val parametersRegex = Regex("fn[(](.+)[)]")
        return parametersRegex.find(line)?.groups?.get(1)?.let { matchResult ->
            val value = matchResult.value
            val paramGroupList = Regex("(\\w+:\\[[\\w|]+(,[\\w|]+)*]|\\w+:[\\w|]+)|\\w+:\\?").findAll(value).toList()
            paramGroupList.map { paramGroup ->
                val paramReg = Regex("\\w*:(.*)")
                try {
                    val param = paramGroup.groups[0]!!.value
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

    fun processMethod(className: String?, funcNode: Node, isToplevel: Boolean = false): MethodTypes {
        // Js doesn't support nested classes, so if the function is not top-level, then we can check for only one parent class.
        try {
            var scope = className?.let {
                if (!isToplevel) json.getJSONObject(it) else json
            } ?: json
            try {
                scope.getJSONObject(funcNode.getAbstractFunctionName())
            } catch (e: JSONException) {
                scope = scope.getJSONObject("prototype")
            }
            val methodJson = scope.getJSONObject(funcNode.getAbstractFunctionName())
            val typesString = methodJson.getString("!type")
                .filterNot { setOf(' ', '+').contains(it) }
            val parametersList = lazy { extractParameters(typesString) }
            val returnType = lazy { extractReturnType(typesString) }

            return MethodTypes(parametersList, returnType)
        } catch (e: Exception) {
            return MethodTypes(
                lazy { funcNode.getAbstractFunctionParams().map { jsUndefinedClassId } },
                lazy { jsUndefinedClassId }
            )
        }
    }

    private fun makeClassId(name: String): JsClassId {
        val classId = when {
            name == "?" || name.toIntOrNull() != null || name.contains('!') -> jsUndefinedClassId
            Regex("\\[(.*)]").matches(name) -> {
                val arrType = Regex("\\[(.*)]").find(name)?.groups?.get(1)?.value?.substringBefore(",")
                    ?: throw IllegalStateException()
                JsClassId(
                    jsName = "array",
                    elementClassId = makeClassId(arrType)
                )
            }

            name.contains('|') -> {
                JsMultipleClassId(name.split("|").map { makeClassId(it) })
            }
            else -> JsClassId(name)
        }

        return try {
            val classNode = importsMap[name] ?: JsParserUtils.searchForClassDecl(
                className = name,
                parsedFile = parsedFile,
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
