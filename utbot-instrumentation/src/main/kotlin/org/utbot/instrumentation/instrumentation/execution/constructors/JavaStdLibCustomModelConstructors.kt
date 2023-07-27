package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.primitiveWrappers
import org.utbot.framework.plugin.api.util.voidWrapperClassId

val javaStdLibCustomModelConstructors: Map<Class<*>, () -> UtCustomModelConstructor> =
    mutableMapOf<Class<*>, () -> UtAssembleModelConstructorBase>(
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
        java.util.concurrent.CopyOnWriteArrayList::class.java to { CollectionConstructor() },

        /**
         * Queues, deques
         */
        java.util.PriorityQueue::class.java to { CollectionConstructor() },
        java.util.ArrayDeque::class.java to { CollectionConstructor() },
        java.util.concurrent.LinkedBlockingQueue::class.java to { CollectionConstructor() },
        java.util.concurrent.LinkedBlockingDeque::class.java to { CollectionConstructor() },
        java.util.concurrent.ConcurrentLinkedQueue::class.java to { CollectionConstructor() },
        java.util.concurrent.ConcurrentLinkedDeque::class.java to { CollectionConstructor() },
        java.util.Queue::class.java to { CollectionConstructor() },
        java.util.Deque::class.java to { CollectionConstructor() },

        /**
         * Sets
         */
        java.util.HashSet::class.java to { CollectionConstructor() },
        java.util.TreeSet::class.java to { CollectionConstructor() },
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
        this += primitiveWrappers
            .filter { it != voidWrapperClassId }
            .associate { it.jClass to { PrimitiveWrapperConstructor() } }
    }