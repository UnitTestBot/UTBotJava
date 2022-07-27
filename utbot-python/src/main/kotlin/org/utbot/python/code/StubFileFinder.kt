package org.utbot.python.code

import com.beust.klaxon.Klaxon

object StubFileFinder {

    private val builtinMethods: List<StubFileStructures.MethodDatasetInfo>
    private val builtinFields: List<StubFileStructures.FieldDatasetInfo>
    private val builtinFunctions: List<StubFileStructures.FunctionDatasetInfo>
    private val builtinClasses: List<StubFileStructures.ClassDatasetInfo>

    init {
        val methodResource = StubFileFinder::class.java.getResourceAsStream("/method_annotations.json")
            ?: error("Didn't find method_annotations.json")
        val fieldResource = StubFileFinder::class.java.getResourceAsStream("/field_annotations.json")
            ?: error("Didn't find fields_annotations.json")
        val functionResource = StubFileFinder::class.java.getResourceAsStream("/function_annotations.json")
            ?: error("Didn't find function_annotations.json")
        val classResource = StubFileFinder::class.java.getResourceAsStream("/class_annotations.json")
            ?: error("Didn't find class_annotations.json")

        builtinMethods = Klaxon().parseArray(methodResource) ?: emptyList()
        builtinFunctions = Klaxon().parseArray(functionResource) ?: emptyList()
        builtinFields = Klaxon().parseArray(fieldResource) ?: emptyList()
        builtinClasses = Klaxon().parseArray(classResource) ?: emptyList()
    }

    val methodToTypeMap: Map<String, List<StubFileStructures.ClassMethodInfo>> by lazy {
        val result = mutableMapOf<String, List<StubFileStructures.ClassMethodInfo>>()
        builtinMethods.forEach { function ->
            result[function.name] = function.typeInfos
        }
        result
    }

    val functionToAnnotationMap: Map<String, List<StubFileStructures.FunctionDefInfo>> by lazy {
        val result = mutableMapOf<String, List<StubFileStructures.FunctionDefInfo>>()
        builtinFunctions.forEach { function ->
            result[function.name] = function.typeInfos.map {
                it.function
            }
        }
        result
    }

    val fieldToTypeMap: Map<String, List<StubFileStructures.FieldInfo>> by lazy {
        val result = mutableMapOf<String, List<StubFileStructures.FieldInfo>>()
        builtinFields.forEach { field ->
            result[field.name] = field.typeInfos
        }
        result
    }

    val nameToClassMap: Map<String, List<StubFileStructures.ClassInfo>> by lazy {
        val result = mutableMapOf<String, List<StubFileStructures.ClassInfo>>()
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

    fun findTypeWithField(
        fieldName: String
    ): Set<String> {
        return (fieldToTypeMap[fieldName] ?: emptyList()).map {
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
    println(
        StubFileFinder.findTypeByFunctionWithArgumentPosition(
            "print", "sep"
        )
    )
    println(
        StubFileFinder.findTypeByFunctionWithArgumentPosition(
            "heapify", argumentPosition = 0
        )
    )
    println(
        StubFileFinder.findTypeByFunctionWithArgumentPosition(
            "gt"
        )
    )
    println(
        StubFileFinder.findTypeWithField(
            "value"
        )
    )
}

