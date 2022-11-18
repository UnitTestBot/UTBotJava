package utils

import framework.api.ts.TsClassId
import framework.api.ts.TsConstructorId
import framework.api.ts.TsMethodId
import java.util.Locale
import parser.TsParserUtils
import parser.ast.BaseTypeNode
import parser.ast.ClassDeclarationNode
import parser.ast.CustomTypeNode
import parser.ast.FunctionNode
import parser.ast.TypeNode
import service.TsServiceContext

fun TsClassId.constructClass(
    classNode: ClassDeclarationNode? = null,
    functions: List<FunctionNode> = emptyList(),
    serviceContext: TsServiceContext
): TsClassId {
    val methods = constructMethods(classNode, functions, serviceContext)
    val constructor = classNode?.let {
        TsConstructorId(
            TsClassId(name),
            it.constructor?.parameters?.map { param ->
                param.type.makeTsClassIdFromType(serviceContext)
            } ?: emptyList(),
        )
    }
    val newClassId = TsClassId(
        tsName = name,
        methods = methods,
        constructor = constructor,
        classPackagePath = serviceContext.projectPath,
        classFilePath = serviceContext.filePathToInference,
    )
    methods.forEach {
        it.classId = newClassId
    }
    constructor?.classId = newClassId
    return newClassId
}

private fun TsClassId.constructMethods(
    classNode: ClassDeclarationNode?,
    functions: List<FunctionNode>,
    serviceContext: TsServiceContext,
): Sequence<TsMethodId> {
    with(this) {
        val methods = classNode?.methods?.map { methodNode ->
            TsMethodId(
                classId = TsClassId(name),
                name = methodNode.name,
                returnType = methodNode.returnType.makeTsClassIdFromType(serviceContext),
                parameters = methodNode.parameters.map { param -> param.type.makeTsClassIdFromType(serviceContext) }
            )
        }?.asSequence() ?:
        // used for toplevel functions
        functions.map { functionNode ->
            TsMethodId(
                classId = TsClassId(name),
                name = functionNode.name,
                returnType = functionNode.returnType.makeTsClassIdFromType(serviceContext),
                parameters = functionNode.parameters.map { param -> param.type.makeTsClassIdFromType(serviceContext) },
                staticModifier = true
            )
        }.asSequence()
        return methods
    }
}

fun TypeNode.makeTsClassIdFromType(serviceContext: TsServiceContext): TsClassId {
    return when (this) {
        is BaseTypeNode -> TsClassId(this.stringTypeName.lowercase(Locale.getDefault()))
        is CustomTypeNode -> {
            val classNode = TsParserUtils.searchForClassDecl(
                className = this.stringTypeName,
                parsedFile = serviceContext.parsedFile,
                basePath = serviceContext.filePathToInference,
                strict = true,
            )
            classNode?.let {
                TsClassId(this.stringTypeName).constructClass(
                    classNode = it,
                    serviceContext = serviceContext
                )
            } ?: throw IllegalStateException("Could not build instance of ${this.stringTypeName}")
        }
        else -> throw IllegalStateException("")
    }
}