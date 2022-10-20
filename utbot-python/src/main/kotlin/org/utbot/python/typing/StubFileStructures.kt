package org.utbot.python.typing

import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.utils.AnnotationNormalizer

object StubFileStructures {

    data class JsonData(
        val classAnnotations: List<ClassInfo>,
        val fieldAnnotations: List<FieldIndex>,
        val functionAnnotations: List<FunctionIndex>,
        val methodAnnotations: List<MethodIndex>,
    ) {
        fun normalizeAnnotations() {
            classAnnotations.forEach { clazz ->
                clazz.normalizeAnnotations()
            }
            fieldAnnotations.forEach { field ->
                field.definitions.forEach { def ->
                    def.normalizeAnnotations()
                }
            }
            functionAnnotations.forEach { function ->
                function.definitions.forEach { def ->
                    def.normalizeAnnotations()
                }
            }
            methodAnnotations.forEach { method ->
                method.definitions.forEach { def ->
                    def.normalizeAnnotations()
                }
            }
        }
    }

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
        var annotation: String,  // must be NormalizedAnnotation
        val className: String,  // must be PythonClassId
        val name: String,
    ) {
        fun normalizeAnnotations() {
            this.annotation = getNormalAnnotation(this.annotation)
        }
    }

    data class ClassInfo(
        val className: String,  // must be PythonClassId
        val fields: List<FieldInfo>,
        val methods: List<FunctionInfo>,
    ) {
        fun normalizeAnnotations() {
            this.fields.forEach { field ->
                field.normalizeAnnotations()
            }
            this.methods.forEach { method ->
                method.normalizeAnnotations()
            }
        }
    }

    data class FunctionInfo(
        val className: String?,  // must be PythonClassId?
        val args: List<ArgInfo> = emptyList(),
        val kwonlyargs: List<ArgInfo> = emptyList(),
        val name: String,
        var returns: String, // must be NormalizedAnnotation
    ) {
        val defName: String
            get() = name.split('.').last()

        val module: String
            get() = name.split('.').dropLast(1).joinToString(".")

        fun normalizeAnnotations() {
            this.args.forEach { arg ->
                arg.normalizeAnnotations()
            }
            this.kwonlyargs.forEach { kwarg ->
                kwarg.normalizeAnnotations()
            }
            this.returns = getNormalAnnotation(this.returns)
        }
    }

    data class ArgInfo(
        val arg: String,
        var annotation: String, // must be NormalizedAnnotation
    ) {
        fun normalizeAnnotations() {
            this.annotation = getNormalAnnotation(this.annotation)
        }
    }

    fun getNormalAnnotation(annotation: String): String {
        return AnnotationNormalizer.pythonClassIdToNormalizedAnnotation(PythonClassId(annotation)).name
    }
}