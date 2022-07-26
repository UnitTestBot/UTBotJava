package org.utbot.framework.synthesis

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
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
    val elements: List<Pair<SynthesisUnit, SynthesisUnit>>,
    val length: SynthesisUnit = ObjectUnit(intClassId),
    val bases: List<Pair<SynthesisUnit, SynthesisUnit>> = elements,
    val currentIndex: Int = 0
) : SynthesisUnit() {
    fun isPrimitive() = classId.elementClassId in primitives
}

data class NullUnit(
    override val classId: ClassId
) : SynthesisUnit()

data class ReferenceToUnit(
    override val classId: ClassId,
    val referenceParam: Int
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
    is ArrayUnit -> elements.all { it.second.isFullyDefined() }
    is MethodUnit -> params.all { it.isFullyDefined() }
}