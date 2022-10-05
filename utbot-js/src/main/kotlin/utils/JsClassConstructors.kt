package utils

import com.oracle.js.parser.ir.ClassNode
import com.oracle.js.parser.ir.FunctionNode
import org.utbot.framework.plugin.api.js.JsClassId
import org.utbot.framework.plugin.api.js.JsConstructorId
import org.utbot.framework.plugin.api.js.JsMethodId
import org.utbot.framework.plugin.api.js.util.jsUndefinedClassId
import service.TernService

fun JsClassId.constructClass(
    ternService: TernService,
    classNode: ClassNode? = null,
    functions: List<FunctionNode> = emptyList()
): JsClassId {
    val className = classNode?.ident?.name?.toString()
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
        classPackagePath = ternService.context.projectPath,
        classFilePath = ternService.context.filePathToInference,
    )
    methods.forEach {
        it.classId = newClassId
    }
    constructor?.classId = newClassId
    return newClassId
}

private fun JsClassId.constructMethods(
    classNode: ClassNode?,
    ternService: TernService,
    className: String?,
    functions: List<FunctionNode>
): Sequence<JsMethodId> {
    with(this) {
        val methods = classNode?.classElements?.map {
            val funcNode = it.value as FunctionNode
            val types = ternService.processMethod(className, funcNode)
            JsMethodId(
                classId = JsClassId(name),
                name = funcNode.name.toString(),
                returnTypeNotLazy = jsUndefinedClassId,
                parametersNotLazy = emptyList(),
                staticModifier = it.isStatic,
                lazyReturnType = types.returnType,
                lazyParameters = types.parameters,
            )
        }?.asSequence() ?:
        // used for toplevel functions
        functions.map { funcNode ->
            val types = ternService.processMethod(className, funcNode, true)
            JsMethodId(
                classId = JsClassId(name),
                name = funcNode.name.toString(),
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