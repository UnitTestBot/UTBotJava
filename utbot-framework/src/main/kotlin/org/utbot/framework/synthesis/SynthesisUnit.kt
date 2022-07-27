package org.utbot.framework.synthesis

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.primitives

sealed class SynthesisUnit {
    abstract val classId: ClassId
}

data class ObjectUnit(
    override val classId: ClassId
) : SynthesisUnit() {
    fun isPrimitive() = classId in primitives
}

data class ArrayUnit(
    override val classId: ClassId,
    val elements: List<Pair<UtModel, UtModel>>,
    val length: UtModel
) : SynthesisUnit() {
    fun isPrimitive() = classId.elementClassId in primitives
}

data class NullUnit(
    override val classId: ClassId
) : SynthesisUnit()

data class ReferenceToUnit(
    override val classId: ClassId,
    val reference: UtModel
) : SynthesisUnit()

data class MethodUnit(
    override val classId: ClassId,
    val method: ExecutableId,
    val params: List<SynthesisUnit>
) : SynthesisUnit()

fun SynthesisUnit.isFullyDefined(): Boolean = when (this) {
    is NullUnit -> true
    is ReferenceToUnit -> true
    is ObjectUnit -> isPrimitive()
    is ArrayUnit -> true
    is MethodUnit -> params.all { it.isFullyDefined() }
}