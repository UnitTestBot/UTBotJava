package org.utbot.python.typing

object StubFileStructures {

    data class FieldDatasetInfo(
        val name: String,
        val typeInfos: List<FieldInfo>
    )

    data class FunctionDatasetInfo(
        val name: String,
        val typeInfos: List<FunctionInfo>
    )

    data class MethodDatasetInfo(
        val name: String,
        val typeInfos: List<ClassMethodInfo>
    )

    data class ClassDatasetInfo(
        val name: String,
        val typeInfos: List<ClassInfo>
    )

    data class FieldInfo(
        val annotation: List<String>,
        val className: String,
        val module: String,
    )

    data class FunctionInfo(
        val function: FunctionDefInfo,
        val module: String,
    )

    data class AttributeInfo(
        val annotation: List<AnnotationInfo>,
        val target: String,
    )

    data class ClassInfo(
        val attributes: List<AttributeInfo>,
        val methods: List<FunctionDefInfo>,
        val name: String,
    )

    data class ClassMethodInfo(
        val className: String,
        val method: FunctionDefInfo,
        val module: String,
    )

    data class FunctionDefInfo(
        val args: List<ArgInfo>,
        val kwonlyargs: List<ArgInfo>,
        val name: String,
        val returns: List<String>,
    )

    data class ArgInfo(
        val annotation: List<String>,
        val arg: String,
    )

    data class AnnotationInfo(
        val annotation: List<String>,
        val target: String,
    )

    data class PythonInfoType(
        val name: String,
        val module: String,
    ) {
        constructor(name: String) : this(name, "")

        val fullName: String
            get() = name
    }
}