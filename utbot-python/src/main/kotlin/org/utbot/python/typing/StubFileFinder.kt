package org.utbot.python.typing

import com.beust.klaxon.Klaxon

object StubFileFinder {

    private val builtinMethods: List<StubFileStructures.MethodIndex>
    private val builtinFields: List<StubFileStructures.FieldIndex>
    private val builtinFunctions: List<StubFileStructures.FunctionIndex>
    private val builtinClasses: List<StubFileStructures.ClassInfo>
    var isInitialized = false

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
        isInitialized = true
    }

    val methodToTypeMap: Map<String, List<StubFileStructures.FunctionInfo>> by lazy {
        val result = mutableMapOf<String, List<StubFileStructures.FunctionInfo>>()
        builtinMethods.forEach { function ->
            result[function.name] = function.definitions
        }
        result
    }

    val functionToTypeMap: Map<String, List<StubFileStructures.FunctionInfo>> by lazy {
        val result = mutableMapOf<String, List<StubFileStructures.FunctionInfo>>()
        builtinFunctions.forEach { function ->
            result[function.name] = function.definitions
        }
        result
    }

    val fieldToTypeMap: Map<String, List<StubFileStructures.FieldInfo>> by lazy {
        val result = mutableMapOf<String, List<StubFileStructures.FieldInfo>>()
        builtinFields.forEach { field ->
            result[field.name] = field.definitions
        }
        result
    }

    val nameToClassMap: Map<String, StubFileStructures.ClassInfo> by lazy {
        val result = mutableMapOf<String, StubFileStructures.ClassInfo>()
        builtinClasses.forEach { pyClass ->
            result[pyClass.className] = pyClass
        }
        result
    }

    fun findTypeWithMethod(
        methodName: String
    ): Set<String> {
        return (methodToTypeMap[methodName] ?: emptyList()).mapNotNull {
            it.className
        }.toSet()
    }

    fun findTypeWithField(
        fieldName: String
    ): Set<String> {
        return (fieldToTypeMap[fieldName] ?: emptyList()).map {
            it.className
        }.toSet()
    }

    fun findTypeByFunctionWithArgumentPosition(
        functionName: String,
        argumentName: String? = null,
        argumentPosition: Int? = null,
    ): Set<String> {
        val annotations = functionToTypeMap[functionName] ?: emptyList()
        val types = mutableSetOf<String>()
        if (argumentName != null) {
            annotations.forEach { annotation ->
                (annotation.args + annotation.kwonlyargs).forEach {
                    if (it.arg == argumentName && it.annotation != null)
                        types.add(it.annotation)
                }
            }
        } else if (argumentPosition != null) {
            annotations.forEach { annotation ->
                val checkCountArgs = annotation.args.size > argumentPosition
                val ann = annotation.args[argumentPosition].annotation
                if (checkCountArgs && ann != null) {
                    types.add(ann)
                }
            }
        } else {
            annotations.forEach { annotation ->
                annotation.args.forEach {
                    if (it.annotation != null)
                        types.add(it.annotation)
                }
            }
        }
        return types
    }

    fun findTypeByFunctionReturnValue(functionName: String): Set<String> {
        return functionToTypeMap[functionName]?.map {
            it.returns
        }?.toSet() ?: emptySet()
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

