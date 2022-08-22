package org.utbot.python.typing

import org.utbot.python.utils.AnnotationNormalizer

object StubFileStructures {

    data class JsonData(
        val classAnnotations: List<ClassInfo>,
        val fieldAnnotations: List<FieldIndex>,
        val functionAnnotations: List<FunctionIndex>,
        val methodAnnotations: List<MethodIndex>,
    ) {
        fun normalizeAnnotations(pythonPath: String, module: String) {
            classAnnotations.forEach { clazz ->
                clazz.normalizeAnnotations(pythonPath, module)
            }
            fieldAnnotations.forEach { field ->
                field.definitions.forEach { def ->
                    def.normalizeAnnotations(pythonPath, module)
                }
            }
            functionAnnotations.forEach { function ->
                function.definitions.forEach { def ->
                    def.normalizeAnnotations(pythonPath, module)
                }
            }
            methodAnnotations.forEach { method ->
                method.definitions.forEach { def ->
                    def.normalizeAnnotations(pythonPath, module)
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
        var annotation: String,
        val className: String,
        val name: String,
    ) {
        fun normalizeAnnotations(pythonPath: String, module: String) {
            this.annotation = getNormalAnnotation(this.annotation, pythonPath, module)
        }
    }

    data class ClassInfo(
        val className: String,
        val fields: List<FieldInfo>,
        val methods: List<FunctionInfo>,
    ) {
        fun normalizeAnnotations(pythonPath: String, module: String) {
            this.fields.forEach { field ->
                field.normalizeAnnotations(pythonPath, module)
            }
            this.methods.forEach { method ->
                method.normalizeAnnotations(pythonPath, module)
            }
        }
    }

    data class FunctionInfo(
        val className: String?,
        val args: List<ArgInfo> = emptyList(),
        val kwonlyargs: List<ArgInfo> = emptyList(),
        val name: String,
        val returns: String,
    ) {
        val defName: String
            get() = name.split('.').last()

        val module: String
            get() = name.split('.').dropLast(1).joinToString(".")

        fun normalizeAnnotations(pythonPath: String, module: String) {
            this.args.forEach { arg ->
                arg.normalizeAnnotations(pythonPath, module)
            }
            this.kwonlyargs.forEach { kwarg ->
                kwarg.normalizeAnnotations(pythonPath, module)
            }
        }
    }

    data class ArgInfo(
        val arg: String,
        var annotation: String,
    ) {
        fun normalizeAnnotations(pythonPath: String, module: String) {
            this.annotation = getNormalAnnotation(this.annotation, pythonPath, module)
        }
    }

    fun getNormalAnnotation(annotation: String, pythonPath: String, module: String): String {
        return AnnotationNormalizer.annotationFromStubToClassId(annotation, pythonPath, module).name
    }
}