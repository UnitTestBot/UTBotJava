package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.reflection
import org.utbot.framework.plugin.api.util.primitiveWrappers
import org.utbot.framework.plugin.api.util.voidWrapperClassId
import org.utbot.framework.util.nextModelName
import org.utbot.jcdb.api.ClassId

private val predefinedConstructors = mutableMapOf<Class<*>, () -> UtAssembleModelConstructorBase>(
    /**
     * Optionals
     */
    java.util.OptionalInt::class.java to { OptionalIntConstructor() },
    java.util.OptionalLong::class.java to { OptionalLongConstructor() },
    java.util.OptionalDouble::class.java to { OptionalDoubleConstructor() },
    java.util.Optional::class.java to { OptionalConstructor() },

    /**
     * Lists
     */
    java.util.LinkedList::class.java to { CollectionConstructor() },
    java.util.ArrayList::class.java to { CollectionConstructor() },
    java.util.AbstractList::class.java to { CollectionConstructor() },
    java.util.List::class.java to { CollectionConstructor() },
    java.util.Deque::class.java to { CollectionConstructor() },
    java.util.ArrayDeque::class.java to { CollectionConstructor() },
    java.util.concurrent.LinkedBlockingDeque::class.java to { CollectionConstructor() },

    /**
     * Sets
     */
    java.util.HashSet::class.java to { CollectionConstructor() },
    java.util.LinkedHashSet::class.java to { CollectionConstructor() },
    java.util.AbstractSet::class.java to { CollectionConstructor() },
    java.util.Set::class.java to { CollectionConstructor() },

    /**
     * Maps
     */
    java.util.HashMap::class.java to { MapConstructor() },
    java.util.TreeMap::class.java to { MapConstructor() },
    java.util.LinkedHashMap::class.java to { MapConstructor() },
    java.util.AbstractMap::class.java to { MapConstructor() },
    java.util.concurrent.ConcurrentMap::class.java to { MapConstructor() },
    java.util.concurrent.ConcurrentHashMap::class.java to { MapConstructor() },
    java.util.IdentityHashMap::class.java to { MapConstructor() },
    java.util.WeakHashMap::class.java to { MapConstructor() },

    /**
     * Hashtables
     */
    java.util.Hashtable::class.java to { MapConstructor() },


    /**
     * String wrapper
     */
    java.lang.String::class.java.let { it to { PrimitiveWrapperConstructor() } },

    /**
     * TODO: JIRA:1405 -- Add assemble constructors for another standard classes as well.
     */
).apply {
    /**
     * Primitive wrappers
     */
        this += with(reflection) {
            primitiveWrappers
                .filter { it != voidWrapperClassId }
                .associate { it.javaClass to { PrimitiveWrapperConstructor() } }
        }
    }


internal fun findUtAssembleModelConstructor(classId: ClassId): UtAssembleModelConstructorBase? = with(reflection){
    predefinedConstructors[classId.javaClass]?.invoke()
}

internal abstract class UtAssembleModelConstructorBase {
    fun constructAssembleModel(
        internalConstructor: UtModelConstructorInterface,
        valueToConstructFrom: Any,
        valueClassId: ClassId,
        id: Int?,
        init: (UtAssembleModel) -> Unit
    ): UtAssembleModel {
        val instantiationChain = mutableListOf<UtStatementModel>()
        val modificationChain = mutableListOf<UtStatementModel>()
        val baseName = valueClassId.simpleName.decapitalize()
        return UtAssembleModel(id, valueClassId, nextModelName(baseName), instantiationChain, modificationChain)
            .also(init)
            .apply { modifyChains(internalConstructor, instantiationChain, modificationChain, valueToConstructFrom) }
    }

    protected abstract fun UtAssembleModel.modifyChains(
        internalConstructor: UtModelConstructorInterface,
        instantiationChain: MutableList<UtStatementModel>,
        modificationChain: MutableList<UtStatementModel>,
        valueToConstructFrom: Any
    )
}

internal fun UtAssembleModelConstructorBase.checkClassCast(expected: Class<*>, actual: Class<*>) {
    require(expected.isAssignableFrom(actual)) {
        "Can't cast $actual to $expected in $this assemble constructor."
    }
}