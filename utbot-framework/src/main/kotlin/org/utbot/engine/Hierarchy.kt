package org.utbot.engine

import org.utbot.engine.types.OBJECT_TYPE
import org.utbot.engine.types.TypeRegistry
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.id
import soot.RefType
import soot.Scene
import soot.SootClass
import soot.SootField
import soot.Type

/**
 * Holds hierarchy knowledge for all loaded classes and provides array name for class field.
 */
class Hierarchy(private val typeRegistry: TypeRegistry) {
    // The list contains ancestors in order from the key class to the farthest parent
    private val ancestorMap: Map<ClassId, List<SootClass>> = lazyMap(::findAncestors)

    // The list  contains inheritors in order from the farthest inheritor to the key class
    private val inheritorsMap: Map<ClassId, List<SootClass>> = lazyMap(::findInheritors)

    /**
     * Returns a chunkId for the [field]. If it's a field for a class containing substitutions,
     * a chunkId for the corresponding field of the real class will be returned.
     *
     * @see TypeRegistry.findRealType
     */
    fun chunkIdForField(type: Type, field: SootField): ChunkId {
        type as? RefType ?: error("$type is not a refType")

        val realType = typeRegistry.findRealType(type) as RefType
        val realFieldDeclaringType = typeRegistry.findRealType(field.declaringClass.type) as RefType

        // java.lang.Thread class has package-private fields, that can be used outside the class.
        // Since wrapper UtThread does not inherit java.lang.Thread, we cannot use this inheritance condition only.
        // The possible presence of hidden field is not important here - we just need
        // to know whether we have at least one such field.
        val realTypeHasFieldByName = realType.sootClass.getFieldUnsafe(field.subSignature) != null
        val realTypeIsInheritor = realFieldDeclaringType.sootClass in ancestors(realType.sootClass.id)

        if (!realTypeIsInheritor && !realTypeHasFieldByName) {
            error("No such field ${field.subSignature} found in ${realType.sootClass.name}")
        }
        return ChunkId("$realFieldDeclaringType", field.name)
    }

    /**
     * Returns ancestors for the class with given ClassId including it.
     *
     * The resulting list has order from the class with given ClassId to the farthest parent.
     */
    fun ancestors(id: ClassId) = ancestorMap[id] ?: error("No such class $id found in ancestors map")

    /**
     * Returns inheritors for the class with given ClassId including it.
     *
     * The resulting list has order from the farthest inheritor to the class with given ClassId.
     */
    fun inheritors(id: ClassId) = inheritorsMap[id] ?: error("No such class $id found in inheritors map")
}

private fun findAncestors(id: ClassId) =
    with(Scene.v().getSootClass(id.name)) {
        val superClasses = mutableListOf<SootClass>()
        val superInterfaces = mutableListOf<SootClass>()

        if (isInterface) {
            superClasses += OBJECT_TYPE.sootClass
            superInterfaces += Scene.v().activeHierarchy.getSuperinterfacesOfIncluding(this)
        } else {
            superClasses += Scene.v().activeHierarchy.getSuperclassesOfIncluding(this)
            superInterfaces += superClasses
                .flatMap { it.interfaces }
                .flatMap { Scene.v().activeHierarchy.getSuperinterfacesOfIncluding(it) }
        }

        superClasses + superInterfaces
    }

private fun findInheritors(id: ClassId) =
    with(Scene.v().getSootClass(id.name)) {
        when {
            // Important to notice that we cannot just take subclasses of `java.lang.Object` because it will not return interfaces.
            type.isJavaLangObject() -> Scene.v().classes.toList()
            isInterface -> Scene.v().activeHierarchy.getSubinterfacesOfIncluding(this) + Scene.v().activeHierarchy.getImplementersOf(this)
            else -> Scene.v().activeHierarchy.getSubclassesOfIncluding(this)
        }
    }