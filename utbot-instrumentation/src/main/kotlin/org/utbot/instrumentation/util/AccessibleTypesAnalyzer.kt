package org.utbot.instrumentation.util

import java.util.IdentityHashMap

class AccessibleTypesAnalyzer {
    fun collectAccessibleTypes(bean: Any): Set<Class<*>> {
        val accessibleObjects = collectFromObject(bean, analyzedObjects = IdentityHashMap())

        return accessibleObjects
            .map { it::class.java }
            .toSet()
    }

    private fun collectFromObject(obj: Any, analyzedObjects: IdentityHashMap<Any, Unit>): Set<Any> {
        if (analyzedObjects.contains(obj)) {
            return emptySet()
        }

        analyzedObjects[obj] = Unit

        val clazz = obj::class.java
        val objects = mutableSetOf<Any>()

        var current: Class<*> = clazz
        while (current.superclass != null) {
            objects.addAll(current.declaredFields.mapNotNull {
                it.get(obj)
            }.flatMap {
                listOf(it) + collectFromObject(it, analyzedObjects)
            })
            current = current.superclass
        }

        return objects
    }
}
