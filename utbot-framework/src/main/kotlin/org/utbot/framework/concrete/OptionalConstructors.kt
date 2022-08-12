package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.objectClassId
import java.util.Optional
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.OptionalLong
import kotlin.reflect.KFunction1


internal sealed class OptionalConstructorBase : UtAssembleModelConstructorBase() {
    abstract val classId: ClassId
    abstract val elementClassId: ClassId

    abstract val isPresent: KFunction1<*, Boolean>
    abstract val getter: KFunction1<*, Any>

    private val emptyMethodId by lazy { MethodId(classId, "empty", classId, emptyList()) }
    private val ofMethodId by lazy { MethodId(classId, "of", classId, listOf(elementClassId)) }

    final override fun UtAssembleModel.modifyChains(
        internalConstructor: UtModelConstructorInterface,
        instantiationChain: MutableList<UtStatementModel>,
        modificationChain: MutableList<UtStatementModel>,
        valueToConstructFrom: Any
    ) {
        require(classId.jClass.isInstance(valueToConstructFrom)) {
            "Can't cast $valueToConstructFrom to ${classId.jClass} in $this assemble constructor."
        }

        instantiationChain += if (!isPresent.call(valueToConstructFrom)) {
            UtExecutableCallModel(
                instance = null,
                emptyMethodId,
                emptyList(),
                this
            )
        } else {
            UtExecutableCallModel(
                instance = null,
                ofMethodId,
                listOf(internalConstructor.construct(getter.call(valueToConstructFrom), elementClassId)),
                this
            )
        }
    }
}

internal class OptionalConstructor : OptionalConstructorBase() {
    override val classId = Optional::class.java.id
    override val elementClassId = objectClassId
    override val isPresent = Optional<*>::isPresent
    override val getter = Optional<*>::get
}

internal class OptionalIntConstructor : OptionalConstructorBase() {
    override val classId = OptionalInt::class.java.id
    override val elementClassId = intClassId
    override val isPresent = OptionalInt::isPresent
    override val getter = OptionalInt::getAsInt
}

internal class OptionalLongConstructor : OptionalConstructorBase() {
    override val classId = OptionalLong::class.java.id
    override val elementClassId = longClassId
    override val isPresent = OptionalLong::isPresent
    override val getter = OptionalLong::getAsLong
}

internal class OptionalDoubleConstructor : OptionalConstructorBase() {
    override val classId = OptionalDouble::class.java.id
    override val elementClassId = doubleClassId
    override val isPresent = OptionalDouble::isPresent
    override val getter = OptionalDouble::getAsDouble
}

// there are no optional wrappers for other primitive types in java.util.*