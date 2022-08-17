package org.utbot.python.typing

import com.beust.klaxon.Klaxon
import org.utbot.python.utils.moduleOfType

object StubFileFinder {
    val methodToTypeMap: MutableMap<String, MutableSet<StubFileStructures.FunctionInfo>> = emptyMap<String, MutableSet<StubFileStructures.FunctionInfo>>().toMutableMap()
    val functionToTypeMap: MutableMap<String, MutableSet<StubFileStructures.FunctionInfo>> = emptyMap<String, MutableSet<StubFileStructures.FunctionInfo>>().toMutableMap()
    val fieldToTypeMap: MutableMap<String, MutableSet<StubFileStructures.FieldInfo>> = emptyMap<String, MutableSet<StubFileStructures.FieldInfo>>().toMutableMap()
    val nameToClassMap: MutableMap<String, StubFileStructures.ClassInfo> = emptyMap<String, StubFileStructures.ClassInfo>().toMutableMap()

    var isInitialized: Boolean = false

    private fun parseJson(json: String): StubFileStructures.JsonData? {
        return Klaxon().parse<StubFileStructures.JsonData>(json)
    }

    fun updateStubs(
        json: String
    ) {
        val jsonData = parseJson(json)
        if (jsonData != null) {
            updateMethods(jsonData.method_annotations)
            updateFields(jsonData.field_annotations)
            updateFunctions(jsonData.function_annotations)
            updateClasses(jsonData.class_annotations)
        }
        isInitialized = true
    }

    private fun updateMethods(newMethods: List<StubFileStructures.MethodIndex>) {
        newMethods.forEach { function ->
            if (!methodToTypeMap.containsKey(function.name))
                methodToTypeMap[function.name] = function.definitions.toMutableSet()
            else
                methodToTypeMap[function.name]?.addAll(function.definitions)
        }
    }

    private fun updateFunctions(newFunctions: List<StubFileStructures.FunctionIndex>) {
        newFunctions.forEach { function ->
            if (!functionToTypeMap.containsKey(function.name))
                functionToTypeMap[function.name] = function.definitions.toMutableSet()
            else
                functionToTypeMap[function.name]?.addAll(function.definitions)
        }
    }

    private fun updateFields(newFields: List<StubFileStructures.FieldIndex>) {
        newFields.forEach { field ->
            if (!fieldToTypeMap.containsKey(field.name))
                fieldToTypeMap[field.name] = field.definitions.toMutableSet()
            else
                fieldToTypeMap[field.name]?.addAll(field.definitions)
        }
    }

    private fun updateClasses(newClasses: List<StubFileStructures.ClassInfo>) {
        newClasses.forEach { pyClass ->
            nameToClassMap[pyClass.className] = pyClass
        }
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

