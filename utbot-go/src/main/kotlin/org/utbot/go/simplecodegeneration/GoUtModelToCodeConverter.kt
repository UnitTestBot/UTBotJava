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

        is GoUtStructModel -> {
            val typeName = model.typeId.getRelativeName(destinationPackage, aliases)
            "$typeName${structModelToGoCodeWithoutStructName(model)}"
        }

        is GoUtArrayModel -> arrayModelToGoCode(model)

        is GoUtSliceModel -> sliceModelToGoCode(model)

        else -> error("Converting a $javaClass to Go code isn't supported")
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

    private fun arrayModelToGoCode(model: GoUtArrayModel): String =
        when (val elementType = model.typeId.elementTypeId!!) {
            is GoStructTypeId -> model.getElements().joinToString(
                prefix = "[${model.length}]${
                    elementType.getRelativeName(destinationPackage, aliases)
                }{",
                postfix = "}"
            ) {
                structModelToGoCodeWithoutStructName(it as GoUtStructModel)
            }

            is GoArrayTypeId -> model.getElements().joinToString(
                prefix = "[${model.length}]${
                    elementType.getRelativeName(destinationPackage, aliases)
                }{",
                postfix = "}"
            ) {
                arrayModelToGoCodeWithoutTypeName(it as GoUtArrayModel)
            }

            is GoSliceTypeId -> model.getElements().joinToString(
                prefix = "[${model.length}]${
                    elementType.getRelativeName(destinationPackage, aliases)
                }{",
                postfix = "}"
            ) {
                sliceModelToGoCodeWithoutTypeName(it as GoUtSliceModel)
            }

            else -> model.getElements().joinToString(
                prefix = "[${model.length}]${elementType.getRelativeName(destinationPackage, aliases)}{",
                postfix = "}"
            )
        }

    private fun arrayModelToGoCodeWithoutTypeName(model: GoUtArrayModel): String =
        when (model.typeId.elementTypeId!!) {
            is GoStructTypeId -> model.getElements().joinToString(prefix = "{", postfix = "}") {
                structModelToGoCodeWithoutStructName(it as GoUtStructModel)
            }

            is GoArrayTypeId -> model.getElements().joinToString(prefix = "{", postfix = "}") {
                arrayModelToGoCodeWithoutTypeName(it as GoUtArrayModel)
            }

            is GoSliceTypeId -> model.getElements().joinToString(prefix = "{", postfix = "}") {
                sliceModelToGoCodeWithoutTypeName(it as GoUtSliceModel)
            }

            else -> model.getElements().joinToString(prefix = "{", postfix = "}")
        }

    private fun sliceModelToGoCode(model: GoUtSliceModel): String =
        when (val elementType = model.typeId.elementTypeId!!) {
            is GoStructTypeId -> model.getElements().joinToString(
                prefix = "[]${
                    elementType.getRelativeName(destinationPackage, aliases)
                }{",
                postfix = "}"
            ) {
                structModelToGoCodeWithoutStructName(it as GoUtStructModel)
            }

            is GoArrayTypeId -> model.getElements().joinToString(
                prefix = "[]${
                    elementType.getRelativeName(destinationPackage, aliases)
                }{",
                postfix = "}"
            ) {
                arrayModelToGoCodeWithoutTypeName(it as GoUtArrayModel)
            }

            is GoSliceTypeId -> model.getElements().joinToString(
                prefix = "[]${
                    elementType.getRelativeName(destinationPackage, aliases)
                }{",
                postfix = "}"
            ) {
                sliceModelToGoCodeWithoutTypeName(it as GoUtSliceModel)
            }

            else -> model.getElements().joinToString(
                prefix = "[]${elementType.getRelativeName(destinationPackage, aliases)}{",
                postfix = "}"
            )
        }

    private fun sliceModelToGoCodeWithoutTypeName(model: GoUtSliceModel): String =
        when (model.typeId.elementTypeId!!) {
            is GoStructTypeId -> model.getElements().joinToString(prefix = "{", postfix = "}") {
                structModelToGoCodeWithoutStructName(it as GoUtStructModel)
            }

            is GoArrayTypeId -> model.getElements().joinToString(prefix = "{", postfix = "}") {
                arrayModelToGoCodeWithoutTypeName(it as GoUtArrayModel)
            }

            is GoSliceTypeId -> model.getElements().joinToString(prefix = "{", postfix = "}") {
                sliceModelToGoCodeWithoutTypeName(it as GoUtSliceModel)
            }

            else -> model.getElements().joinToString(prefix = "{", postfix = "}")
        }
}
