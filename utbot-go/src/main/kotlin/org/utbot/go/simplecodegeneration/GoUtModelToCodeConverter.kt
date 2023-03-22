package org.utbot.go.simplecodegeneration

import org.utbot.go.api.*
import org.utbot.go.api.util.goStringTypeId
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoUtModel

class GoUtModelToCodeConverter(
    private val destinationPackage: GoPackage,
    private val aliases: Map<GoPackage, String?>
) {

    fun toGoCode(model: GoUtModel): String = when (model) {
        is GoUtNilModel -> "nil"

        is GoUtPrimitiveModel -> when (model.explicitCastMode) {
            ExplicitCastMode.REQUIRED -> primitiveModelToCastedValueGoCode(model)
            ExplicitCastMode.DEPENDS, ExplicitCastMode.NEVER -> primitiveModelToValueGoCode(model)
        }

        is GoUtArrayModel -> arrayModelToGoCode(model)

        is GoUtSliceModel -> sliceModelToGoCode(model)

        is GoUtNamedModel -> namedModelToGoCode(model)

        else -> error("Converting a ${model.javaClass} to Go code isn't supported")
    }

    private fun toGoCodeWithoutTypeName(model: GoUtModel): String = when (model) {
        is GoUtNilModel -> "nil"

        is GoUtPrimitiveModel -> when (model.explicitCastMode) {
            ExplicitCastMode.REQUIRED -> primitiveModelToCastedValueGoCode(model)
            ExplicitCastMode.DEPENDS, ExplicitCastMode.NEVER -> primitiveModelToValueGoCode(model)
        }

        is GoUtStructModel -> structModelToGoCodeWithoutStructName(model)

        is GoUtArrayModel -> arrayModelToGoCodeWithoutTypeName(model)

        is GoUtSliceModel -> sliceModelToGoCodeWithoutTypeName(model)

        is GoUtNamedModel -> toGoCodeWithoutTypeName(model.value)

        else -> error("Converting a ${model.javaClass} to Go code isn't supported")
    }

    fun primitiveModelToValueGoCode(model: GoUtPrimitiveModel): String = when (model) {
        is GoUtComplexModel -> complexModeToValueGoCode(model)
        else -> if (model.typeId == goStringTypeId) "\"${model.value}\"" else "${model.value}"
    }

    fun primitiveModelToCastedValueGoCode(model: GoUtPrimitiveModel): String =
        "${model.typeId}(${primitiveModelToValueGoCode(model)})"

    private fun complexModeToValueGoCode(model: GoUtComplexModel) =
        "complex(${toGoCode(model.realValue)}, ${toGoCode(model.imagValue)})"

    private fun structModelToGoCodeWithoutStructName(model: GoUtStructModel): String =
        model.getVisibleFields(destinationPackage).joinToString(prefix = "{", postfix = "}") {
            "${it.fieldId.name}: ${toGoCode(it.model)}"
        }

    private fun arrayModelToGoCode(model: GoUtArrayModel): String {
        val elementType = model.typeId.elementTypeId!!
        val elementTypeName = elementType.getRelativeName(destinationPackage, aliases)
        return model.getElements().joinToString(prefix = "[${model.length}]$elementTypeName{", postfix = "}") {
            toGoCodeWithoutTypeName(it)
        }
    }

    private fun arrayModelToGoCodeWithoutTypeName(model: GoUtArrayModel): String =
        model.getElements().joinToString(prefix = "{", postfix = "}") {
            toGoCodeWithoutTypeName(it)
        }

    private fun sliceModelToGoCode(model: GoUtSliceModel): String {
        val elementType = model.typeId.elementTypeId!!
        val elementTypeName = elementType.getRelativeName(destinationPackage, aliases)
        return model.getElements().joinToString(prefix = "[]$elementTypeName{", postfix = "}") {
            toGoCodeWithoutTypeName(it)
        }
    }

    private fun sliceModelToGoCodeWithoutTypeName(model: GoUtSliceModel): String =
        model.getElements().joinToString(prefix = "{", postfix = "}") {
            toGoCodeWithoutTypeName(it)
        }

    private fun namedModelToGoCode(model: GoUtNamedModel): String {
        val typeName = model.typeId.getRelativeName(destinationPackage, aliases)
        return if (model.value is GoUtPrimitiveModel) {
            "$typeName(${toGoCodeWithoutTypeName(model.value)})"
        } else {
            "$typeName${toGoCodeWithoutTypeName(model.value)}"
        }
    }
}
