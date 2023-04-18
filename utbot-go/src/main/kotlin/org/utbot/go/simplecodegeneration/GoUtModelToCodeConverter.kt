package org.utbot.go.simplecodegeneration

import org.utbot.go.api.*
import org.utbot.go.api.util.goStringTypeId
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoUtModel

class GoUtModelToCodeConverter(
    private val destinationPackage: GoPackage,
    private val aliases: Map<GoPackage, String?>
) {

    fun toGoCode(model: GoUtModel, withTypeConversion: Boolean = true): String = when (model) {
        is GoUtNilModel -> "nil"

        is GoUtPrimitiveModel -> when (model.explicitCastMode) {
            ExplicitCastMode.REQUIRED -> primitiveModelToCastedValueGoCode(model)
            ExplicitCastMode.DEPENDS, ExplicitCastMode.NEVER -> primitiveModelToValueGoCode(model)
        }

        is GoUtArrayModel -> arrayModelToGoCode(model)

        is GoUtSliceModel -> sliceModelToGoCode(model)

        is GoUtMapModel -> mapModelToGoCode(model)

        is GoUtNamedModel -> if (!withTypeConversion && model.value is GoUtPrimitiveModel) {
            toGoCodeWithoutTypeName(model.value)
        } else {
            namedModelToGoCode(model)
        }

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

        is GoUtMapModel -> mapModelToGoCodeWithoutTypeName(model)

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
        val typeName = model.typeId.getRelativeName(destinationPackage, aliases)
        return typeName + arrayModelToGoCodeWithoutTypeName(model)
    }

    private fun arrayModelToGoCodeWithoutTypeName(model: GoUtArrayModel): String =
        model.getElements().joinToString(prefix = "{", postfix = "}") {
            toGoCodeWithoutTypeName(it)
        }

    private fun sliceModelToGoCode(model: GoUtSliceModel): String {
        val typeName = model.typeId.getRelativeName(destinationPackage, aliases)
        return typeName + sliceModelToGoCodeWithoutTypeName(model)
    }

    private fun sliceModelToGoCodeWithoutTypeName(model: GoUtSliceModel): String =
        model.getElements().joinToString(prefix = "{", postfix = "}") {
            toGoCodeWithoutTypeName(it)
        }

    private fun mapModelToGoCode(model: GoUtMapModel): String {
        val typeName = model.typeId.getRelativeName(destinationPackage, aliases)
        return typeName + mapModelToGoCodeWithoutTypeName(model)
    }

    private fun mapModelToGoCodeWithoutTypeName(model: GoUtMapModel): String =
        model.value.entries.joinToString(prefix = "{", postfix = "}") {
            "${toGoCode(it.key)}: ${toGoCodeWithoutTypeName(it.value)}"
        }

    private fun namedModelToGoCode(model: GoUtNamedModel): String {
        val typeName = model.typeId.getRelativeName(destinationPackage, aliases)
        return if (model.value is GoUtPrimitiveModel || model.value is GoUtNilModel) {
            "$typeName(${toGoCodeWithoutTypeName(model.value)})"
        } else {
            "$typeName${toGoCodeWithoutTypeName(model.value)}"
        }
    }
}
