package org.utbot.python.typing

object StubFileStructures {

    data class FieldIndex(
        val name: String,
        val definitions: List<FieldInfo>
    )

    data class FunctionIndex(
        val name: String,
        val definitions: List<FunctionInfo>
    )

    data class MethodIndex(
        val name: String,
        val definitions: List<FunctionInfo>
    )

    data class FieldInfo(
        val annotation: String,
        val className: String,
        val name: String,
    )

    data class ClassInfo(
        val className: String,
        val fields: List<FieldInfo>,
        val methods: List<FunctionInfo>,
    )

    data class FunctionInfo(
        val className: String?,
        val args: List<ArgInfo> = emptyList(),
        val kwonlyargs: List<ArgInfo> = emptyList(),
        val name: String,
        val returns: String,
    ) {
        val module: String
            get() = name.split('.').last()

        val defName: String
            get() = name.split('.').dropLast(1).joinToString(".")
    }

    data class ArgInfo(
        val arg: String,
        val annotation: String,
    )
}