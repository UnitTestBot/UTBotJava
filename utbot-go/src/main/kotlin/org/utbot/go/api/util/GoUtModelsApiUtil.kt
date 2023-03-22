package org.utbot.go.api.util

import org.utbot.go.api.*
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.worker.*

fun GoUtModel.isNaNOrInf(): Boolean = this is GoUtFloatNaNModel || this is GoUtFloatInfModel

fun GoUtModel.doesNotContainNaNOrInf(): Boolean {
    if (this.isNaNOrInf()) return false
    val asComplexModel = (this as? GoUtComplexModel) ?: return true
    return !(asComplexModel.realValue.isNaNOrInf() || asComplexModel.imagValue.isNaNOrInf())
}

fun GoUtModel.containsNaNOrInf(): Boolean = !this.doesNotContainNaNOrInf()

fun GoUtModel.convertToRawValue(destinationPackage: GoPackage, aliases: Map<GoPackage, String?>): RawValue =
    when (val model = this) {
        is GoUtComplexModel -> PrimitiveValue(
            model.typeId.getRelativeName(destinationPackage, aliases),
            "${model.realValue}@${model.imagValue}"
        )

        is GoUtNamedModel -> NamedValue(
            model.typeId.getRelativeName(destinationPackage, aliases),
            model.value.convertToRawValue(destinationPackage, aliases)
        )

        is GoUtArrayModel -> ArrayValue(
            model.typeId.getRelativeName(destinationPackage, aliases),
            model.typeId.elementTypeId!!.getRelativeName(destinationPackage, aliases),
            model.length,
            model.getElements().map { it.convertToRawValue(destinationPackage, aliases) }
        )


        is GoUtSliceModel -> SliceValue(
            model.typeId.getRelativeName(destinationPackage, aliases),
            model.typeId.elementTypeId!!.getRelativeName(destinationPackage, aliases),
            model.length,
            model.getElements().map { it.convertToRawValue(destinationPackage, aliases) }
        )

        is GoUtStructModel -> StructValue(
            model.typeId.getRelativeName(destinationPackage, aliases),
            model.value.map {
                StructValue.FieldValue(
                    it.fieldId.name,
                    it.model.convertToRawValue(destinationPackage, aliases),
                    it.fieldId.isExported
                )
            }
        )

        is GoUtPrimitiveModel -> PrimitiveValue(model.typeId.name, model.value.toString())

        else -> error("Converting ${model.javaClass} to RawValue is not supported")
    }