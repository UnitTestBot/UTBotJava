package org.utbot.python

import com.beust.klaxon.Klaxon


data class ClassDatasetInfo(
    val name: String,
    val typeInfos: List<ClassInfo>
)

data class FunctionDatasetInfo(
    val name: String,
    val typeInfos: List<FunctionInfo>
)

data class MethodDatasetInfo(
    val name: String,
    val typeInfos: List<ClassDefInfo>
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

data class ClassDefInfo(
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

object StubFileFinder {

    private val builtinMethods: List<MethodDatasetInfo>
    private val builtinFunctions: List<FunctionDatasetInfo>
    private val builtinClasses: List<ClassDatasetInfo>

    init {
        val methodResource = StubFileFinder::class.java.getResourceAsStream("/method_annotations.json")
            ?: error("Didn't find method_annotations.json")
        val functionResource = StubFileFinder::class.java.getResourceAsStream("/function_annotations.json")
            ?: error("Didn't find function_annotations.json")
        val classResource = StubFileFinder::class.java.getResourceAsStream("/classes_annotations.json")
            ?: error("Didn't find function_annotations.json")

        builtinMethods = Klaxon().parseArray(methodResource) ?: emptyList()
        builtinFunctions = Klaxon().parseArray(functionResource) ?: emptyList()
        builtinClasses = Klaxon().parseArray(classResource) ?: emptyList()
    }

    val methodToTypeMap: Map<String, List<ClassDefInfo>> by lazy {
        val result = mutableMapOf<String, List<ClassDefInfo>>()
        builtinMethods.forEach { function ->
            result[function.name] = function.typeInfos
        }
        result
    }

    val functionToAnnotationMap: Map<String, List<FunctionDefInfo>> by lazy {
        val result = mutableMapOf<String, List<FunctionDefInfo>>()
        builtinFunctions.forEach { function ->
            result[function.name] = function.typeInfos.map {
                it.function
            }
        }
        result
    }

    val classToTypeMap: Map<String, List<ClassInfo>> by lazy {
        val result = mutableMapOf<String, List<ClassInfo>>()
        builtinClasses.forEach { pyClass ->
            result[pyClass.name] = pyClass.typeInfos
        }
        result
    }

    fun findTypeWithMethod(
        methodName: String
    ): Set<String> {
        return (methodToTypeMap[methodName] ?: emptyList()).map {
            "${it.module}.${it.className}"
        }.toSet()
    }

    fun findTypeByFunctionWithArgumentPosition(
        functionName: String,
        argumentName: String? = null,
        argumentPosition: Int? = null,
    ): Set<String> {
        val annotations = functionToAnnotationMap[functionName] ?: emptyList()
        val types = mutableSetOf<String>()
        if (argumentName != null) {
            annotations.forEach { annotation ->
                (annotation.args + annotation.kwonlyargs).forEach {
                    if (it.arg == argumentName)
                        types += it.annotation
                }
            }
        } else if (argumentPosition != null) {
            annotations.forEach { annotation ->
                val checkCountArgs = annotation.args.size > argumentPosition
                if (checkCountArgs) {
                    types += annotation.args[argumentPosition].annotation
                }
            }
        } else {
            annotations.forEach { annotation ->
                annotation.args.forEach {
                    types.addAll(it.annotation)
                }
            }
        }
        return types
    }
}

fun main() {
    println(StubFileFinder.findTypeWithMethod("__add__"))
    println(StubFileFinder.findTypeByFunctionWithArgumentPosition(
        "print", "sep"
    ))
    println(StubFileFinder.findTypeByFunctionWithArgumentPosition(
        "heapify"
    ))
    println(StubFileFinder.findTypeByFunctionWithArgumentPosition(
        "gt"
    ))
    println(StubFileFinder.classToTypeMap["int"])
}

