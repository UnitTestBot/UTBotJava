package org.utbot.python.typing

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.framework.plugin.api.NormalizedPythonAnnotation
import org.utbot.framework.plugin.api.PythonClassId

object StubFileFinder {
    val methodToTypeMap: MutableMap<String, MutableSet<StubFileStructures.FunctionInfo>> = mutableMapOf()
    val functionToTypeMap: MutableMap<String, MutableSet<StubFileStructures.FunctionInfo>> = mutableMapOf()
    val fieldToTypeMap: MutableMap<String, MutableSet<StubFileStructures.FieldInfo>> = mutableMapOf()
    val nameToClassMap: MutableMap<String, StubFileStructures.ClassInfo> = mutableMapOf()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val jsonAdapter = moshi.adapter(StubFileStructures.JsonData::class.java)

    private fun parseJson(json: String): StubFileStructures.JsonData? {
        return if (json.isNotEmpty()) {
            jsonAdapter.fromJson(json)
        } else {
            null
        }
    }

    fun updateStubs(
        json: String
    ) {
        val jsonData = parseJson(json)
        if (jsonData != null) {
            jsonData.normalizeAnnotations()
            updateMethods(jsonData.methodAnnotations)
            updateFields(jsonData.fieldAnnotations)
            updateFunctions(jsonData.functionAnnotations)
            updateClasses(jsonData.classAnnotations)
        }
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

    fun findTypeWithMethod(
        methodName: String
    ): Set<PythonClassId> {
        return (methodToTypeMap[methodName] ?: emptyList()).mapNotNull { methodInfo ->
            methodInfo.className?.let { PythonClassId(it) }
        }.toSet()
    }

    fun findTypeWithField(
        fieldName: String
    ): Set<PythonClassId> {
        return (fieldToTypeMap[fieldName] ?: emptyList()).map {
            PythonClassId(it.className)
        }.toSet()
    }

    fun findAnnotationByFunctionWithArgumentPosition(
        functionName: String,
        argumentName: String? = null,
        argumentPosition: Int? = null,
    ): Set<NormalizedPythonAnnotation> {
        val functionInfos = functionToTypeMap[functionName] ?: emptyList()
        val types = mutableSetOf<NormalizedPythonAnnotation>()
        if (argumentName != null) {
            functionInfos.forEach { functionInfo ->
                (functionInfo.args + functionInfo.kwonlyargs).forEach {
                    if (it.arg == argumentName && it.annotation != "")
                        types.add(NormalizedPythonAnnotation(it.annotation))
                }
            }
        } else if (argumentPosition != null) {
            functionInfos.forEach { functionInfo ->
                val checkCountArgs = functionInfo.args.size > argumentPosition
                val ann = functionInfo.args.getOrNull(argumentPosition)?.annotation ?: ""
                if (checkCountArgs && ann != "") {
                    types.add(NormalizedPythonAnnotation(ann))
                }
            }
        } else {
            functionInfos.forEach { functionInfo ->
                functionInfo.args.forEach {
                    if (it.annotation != "")
                        types.add(NormalizedPythonAnnotation(it.annotation))
                }
            }
        }
        return types
    }

    fun findAnnotationByFunctionReturnValue(functionName: String): Set<NormalizedPythonAnnotation> {
        return functionToTypeMap[functionName]?.map {
            NormalizedPythonAnnotation(it.returns)
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

