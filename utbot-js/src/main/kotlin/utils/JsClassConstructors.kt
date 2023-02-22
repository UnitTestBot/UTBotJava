package utils

import com.google.javascript.rhino.Node
import framework.api.js.JsClassId
import framework.api.js.JsConstructorId
import framework.api.js.JsMethodId
import framework.api.js.util.jsUndefinedClassId
import parser.JsParserUtils.getAbstractFunctionName
import parser.JsParserUtils.getClassMethods
import parser.JsParserUtils.getClassName
import parser.JsParserUtils.isStatic
import service.TernService

fun JsClassId.constructClass(
    ternService: TernService,
    classNode: Node? = null,
    functions: List<Node> = emptyList()
): JsClassId {
    val className = classNode?.getClassName()
    val methods = constructMethods(classNode, ternService, className, functions)

    val constructor = classNode?.let {
        JsConstructorId(
            JsClassId(name),
            ternService.processConstructor(it),
        )
    }
    val newClassId = JsClassId(
        jsName = name,
        methods = methods,
        constructor = constructor,
        classPackagePath = ternService.projectPath,
        classFilePath = ternService.filePathToInference,
    )
    methods.forEach {
        it.classId = newClassId
    }
    constructor?.classId = newClassId
    return newClassId
}

private fun JsClassId.constructMethods(
    classNode: Node?,
    ternService: TernService,
    className: String?,
    functions: List<Node>
): Sequence<JsMethodId> {
    with(this) {
        val methods = classNode?.getClassMethods()?.map { methodNode ->
            val types = ternService.processMethod(className, methodNode)
            JsMethodId(
                classId = JsClassId(name),
                name = methodNode.getAbstractFunctionName(),
                returnTypeNotLazy = jsUndefinedClassId,
                parametersNotLazy = emptyList(),
                staticModifier = methodNode.isStatic(),
                lazyReturnType = types.returnType,
                lazyParameters = types.parameters,
            )
        }?.asSequence() ?:
        // used for toplevel functions
        functions.map { funcNode ->
            val types = ternService.processMethod(className, funcNode, true)
            JsMethodId(
                classId = JsClassId(name),
                name = funcNode.getAbstractFunctionName(),
                returnTypeNotLazy = jsUndefinedClassId,
                parametersNotLazy = emptyList(),
                staticModifier = true,
                lazyReturnType = types.returnType,
                lazyParameters = types.parameters,
            )
        }.asSequence()
        return methods
    }
}