package org.utbot.framework.synthesis

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.primitives

sealed class SynthesisUnit {
    abstract val classId: ClassId
}

data class ObjectUnit(
    override val classId: ClassId
) : SynthesisUnit() {
    fun isPrimitive() = classId in primitives
}

sealed class ElementContainingUnit(
    override val classId: ClassId,
    open val elements: List<Pair<UtModel, UtModel>>,
    open val length: UtModel
) : SynthesisUnit() {
    fun isPrimitive() = classId.elementClassId in primitives
}

data class ArrayUnit(
    override val classId: ClassId,
    override val elements: List<Pair<UtModel, UtModel>>,
    override val length: UtModel
) : ElementContainingUnit(classId, elements, length)

data class ListUnit(
    override val classId: ClassId,
    override val elements: List<Pair<UtModel, UtModel>>,
    override val length: UtModel
) : ElementContainingUnit(classId, elements, length) {
    val constructorId get() = classId.allConstructors.first { it.parameters.isEmpty() }
    val addId get() = classId.allMethods.first {
        it.name == "add" && it.parameters == listOf(objectClassId)
    }
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
    is ElementContainingUnit -> true
    is MethodUnit -> params.all { it.isFullyDefined() }
}