package org.utbot.python.typing

import com.beust.klaxon.Klaxon
import org.utbot.python.utils.moduleOfType

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

    data class SearchResult(val typeName: String, val module: String)

    fun findTypeWithMethod(
        methodName: String
    ): Set<SearchResult> {
        return (methodToTypeMap[methodName] ?: emptyList()).mapNotNull { methodInfo ->
            methodInfo.className?.let { SearchResult(it, methodInfo.module) }
        }.toSet()
    }

    fun findTypeWithField(
        fieldName: String
    ): Set<SearchResult> {
        return (fieldToTypeMap[fieldName] ?: emptyList()).map {
            SearchResult(it.className, moduleOfType(it.className)!!)
        }.toSet()
    }

    fun findAnnotationByFunctionWithArgumentPosition(
        functionName: String,
        argumentName: String? = null,
        argumentPosition: Int? = null,
    ): Set<SearchResult> {
        val functionInfos = functionToTypeMap[functionName] ?: emptyList()
        val types = mutableSetOf<SearchResult>()
        if (argumentName != null) {
            functionInfos.forEach { functionInfo ->
                val module = functionInfo.module
                (functionInfo.args + functionInfo.kwonlyargs).forEach {
                    if (it.arg == argumentName && it.annotation != "")
                        types.add(SearchResult(it.annotation, module))
                }
            }
        } else if (argumentPosition != null) {
            functionInfos.forEach { functionInfo ->
                val checkCountArgs = functionInfo.args.size > argumentPosition
                val ann = functionInfo.args.getOrNull(argumentPosition)?.annotation ?: ""
                if (checkCountArgs && ann != "") {
                    types.add(SearchResult(ann, functionInfo.module))
                }
            }
        } else {
            functionInfos.forEach { functionInfo ->
                val module = functionInfo.module
                functionInfo.args.forEach {
                    if (it.annotation != "")
                        types.add(SearchResult(it.annotation, module))
                }
            }
        }
        return types
    }

    fun findAnnotationByFunctionReturnValue(functionName: String): Set<SearchResult> {
        return functionToTypeMap[functionName]?.map {
            SearchResult(it.returns, it.module)
        }?.toSet() ?: emptySet()
    }
}

fun main() {
    println(StubFileFinder.findTypeWithMethod("__add__"))
    println(
        StubFileFinder.findAnnotationByFunctionWithArgumentPosition(
            "print", "sep"
        )
    )
    println(
        StubFileFinder.findAnnotationByFunctionWithArgumentPosition(
            "heapify", argumentPosition = 0
        )
    )
    println(
        StubFileFinder.findAnnotationByFunctionWithArgumentPosition(
            "gt"
        )
    )
    println(
        StubFileFinder.findTypeWithField(
            "value"
        )
    )
}

