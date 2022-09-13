package org.utbot.framework.synthesis

import org.utbot.framework.modifications.StatementsStorage
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId

class CompositeUnitExpander(
    private val statementsStorage: StatementsStorage
) {
    private fun ExecutableId.thisParamOrEmptyList() =
        if (this is MethodId && !this.isStatic) {
            listOf(this.classId)
        } else {
            emptyList()
        }

    private val StatementsStorage.definedClasses get() = items.keys.map { it.classId }.toSet()

    fun expand(objectUnit: ObjectUnit): List<MethodUnit> {
        if (objectUnit.isPrimitive()) {
            return emptyList()
        }
        if (objectUnit.classId !in statementsStorage.definedClasses) {
            statementsStorage.update(setOf(objectUnit.classId).expandable())
        }
        val mutators = findAllMutators(objectUnit.classId)

        val expanded = mutators.map { method ->
            MethodUnit(
                objectUnit.classId,
                method,
                (method.thisParamOrEmptyList() + method.parameters).map { ObjectUnit(it) }
            )
        }
        return expanded
    }

    private fun findAllMutators(classId: ClassId) = findConstructors(classId) + findMutators(classId)

    private fun findConstructors(classId: ClassId): List<ExecutableId> =
        statementsStorage.items
            .filterKeys { method -> method.classId == classId }
            .keys
            .filterIsInstance<ConstructorId>()
            .toList()

    private fun findMutators(classId: ClassId): List<ExecutableId> =
        statementsStorage.items
            .filter { (method, info) ->
                val sameClass = method.classId == classId
                val modifiesSomething = info.modifiedFields.any { it.declaringClass == classId }
                val isStaticInit = method.name == "<clinit>"
                sameClass && modifiesSomething && !isStaticInit
            }
            .keys
            .filterIsInstance<ExecutableId>()
            .toList()
}
