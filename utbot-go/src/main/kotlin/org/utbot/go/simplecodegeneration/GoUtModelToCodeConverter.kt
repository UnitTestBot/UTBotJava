package org.utbot.go.simplecodegeneration

import org.utbot.go.api.*
import org.utbot.go.api.util.goDefaultValueModel
import org.utbot.go.api.util.goStringTypeId
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

class GoUtModelToCodeConverter(
    private val destinationPackage: GoPackage,
    private val aliases: Map<GoPackage, String?>
) {

    fun toGoCode(model: GoUtModel): String = when (model) {
        is GoUtNilModel -> nilModelToGoCode(model)

        is GoUtPrimitiveModel -> when (model.explicitCastMode) {
            ExplicitCastMode.REQUIRED, ExplicitCastMode.DEPENDS -> primitiveModelToCastedValueGoCode(model)
            ExplicitCastMode.NEVER -> primitiveModelToValueGoCode(model)
        }

        is GoUtArrayModel -> arrayModelToGoCode(model)

        is GoUtSliceModel -> sliceModelToGoCode(model)

        is GoUtMapModel -> mapModelToGoCode(model)

        is GoUtChanModel -> chanModelToGoCode(model)

        is GoUtNamedModel -> namedModelToGoCode(model)

        is GoUtPointerModel -> pointerModelToGoCode(model)

        else -> error("Converting a ${model.javaClass} to Go code isn't supported")
    }

    fun toGoCodeWithoutTypeName(model: GoUtModel): String = when (model) {
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

        is GoUtPointerModel -> pointerModelToGoCode(model)

        else -> error("Converting a ${model.javaClass} to Go code isn't supported")
    }

    private fun nilModelToGoCode(model: GoUtNilModel): String {
        val typeName = model.typeId.getRelativeName(destinationPackage, aliases)
        return "($typeName)(nil)"
    }

    private fun primitiveModelToValueGoCode(model: GoUtPrimitiveModel): String = when (model) {
        is GoUtComplexModel -> complexModeToValueGoCode(model)
        else -> if (model.typeId == goStringTypeId) "\"${model.value}\"" else "${model.value}"
    }

    private fun primitiveModelToCastedValueGoCode(model: GoUtPrimitiveModel): String {
        val typeName = model.typeId.getRelativeName(destinationPackage, aliases)
        return "$typeName(${primitiveModelToValueGoCode(model)})"
    }

    private fun complexModeToValueGoCode(model: GoUtComplexModel) =
        "complex(${toGoCode(model.realValue)}, ${toGoCode(model.imagValue)})"

    private fun structModelToGoCodeWithoutStructName(model: GoUtStructModel): String =
        model.value.entries.filter { (fieldId, model) -> model != fieldId.declaringType.goDefaultValueModel() }
            .joinToString(prefix = "{", postfix = "}") { (fieldId, model) ->
                "${fieldId.name}: ${toGoCode(model)}"
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

    private fun chanModelToGoCode(model: GoUtChanModel): String {
        val elemTypeName = model.typeId.elementTypeId!!.getRelativeName(destinationPackage, aliases)
        return "make(chan $elemTypeName, ${model.value.size})"
    }

    private fun namedModelToGoCode(model: GoUtNamedModel): String {
        if (model.value is GoUtNamedModel) {
            return toGoCode(model.value)
        }
        val typeName = model.typeId.getRelativeName(destinationPackage, aliases)
        return if (model.value is GoUtPrimitiveModel || model.value is GoUtNilModel) {
            "$typeName(${toGoCodeWithoutTypeName(model.value)})"
        } else {
            "$typeName${toGoCodeWithoutTypeName(model.value)}"
        }
    }

    private fun pointerToZeroValueOfType(typeId: GoTypeId): String {
        val typeName = typeId.getRelativeName(destinationPackage, aliases)
        return "new($typeName)"
    }

    private fun pointerModelToGoCode(model: GoUtPointerModel): String = when (val value = model.value) {
        is GoUtNilModel -> pointerToZeroValueOfType(value.typeId)
        is GoUtPrimitiveModel -> pointerToZeroValueOfType(value.typeId)

        is GoUtNamedModel -> {
            if (value.value is GoUtPrimitiveModel) {
                pointerToZeroValueOfType(value.typeId)
            } else {
                "&${toGoCode(value)}"
            }
        }

        else -> "&${toGoCode(value)}"
    }
}
