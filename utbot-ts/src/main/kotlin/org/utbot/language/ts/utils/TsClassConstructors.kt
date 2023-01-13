package org.utbot.language.ts.utils

import org.utbot.language.ts.framework.api.ts.TsClassId
import org.utbot.language.ts.framework.api.ts.TsConstructorId
import org.utbot.language.ts.framework.api.ts.TsMethodId
import java.util.Locale
import org.utbot.language.ts.parser.TsParserUtils
import org.utbot.language.ts.parser.ast.BaseTypeNode
import org.utbot.language.ts.parser.ast.ClassDeclarationNode
import org.utbot.language.ts.parser.ast.CustomTypeNode
import org.utbot.language.ts.parser.ast.FunctionNode
import org.utbot.language.ts.parser.ast.FunctionTypeNode
import org.utbot.language.ts.parser.ast.TypeNode
import org.utbot.language.ts.service.TsServiceContext

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
                name = methodNode.name.value,
                returnType = methodNode.returnType.value.makeTsClassIdFromType(serviceContext),
                parameters = methodNode.parameters.map { param -> param.type.makeTsClassIdFromType(serviceContext) }
            )
        }?.asSequence() ?:
        // used for toplevel functions
        functions.map { functionNode ->
            TsMethodId(
                classId = TsClassId(name),
                name = functionNode.name.value,
                returnType = functionNode.returnType.value.makeTsClassIdFromType(serviceContext),
                parameters = functionNode.parameters.map { param -> param.type.makeTsClassIdFromType(serviceContext) },
                staticModifier = true
            )
        }.asSequence()
        return methods
    }
}

fun TypeNode.makeTsClassIdFromType(serviceContext: TsServiceContext): TsClassId {
    return when (this) {
        is BaseTypeNode -> TsClassId(stringTypeName.lowercase(Locale.getDefault()))
        is CustomTypeNode -> {
            val classNode = TsParserUtils.searchForClassDecl(
                className = stringTypeName,
                parsedFile = serviceContext.parsedFile,
                strict = true,
                parsedImportedFiles = serviceContext.parsedFiles,
            )
            classNode?.let {
                TsClassId(stringTypeName).constructClass(
                    classNode = it,
                    serviceContext = serviceContext
                )
            } ?: run {
                throw IllegalStateException("Could not build instance of $stringTypeName")
            }
        }
        is FunctionTypeNode -> TsClassId(stringTypeName)
        else -> throw IllegalStateException(this.stringTypeName)
    }
}
