package org.utbot.fuzzer

import kotlinx.coroutines.runBlocking
import org.utbot.framework.concrete.UtModelConstructor
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.providers.AbstractModelProvider
import org.utbot.jcdb.api.*
import java.util.*
import java.util.function.IntSupplier

/**
 * Provides some simple default models of any class.
 *
 * Used as a fallback implementation until other providers cover every type.
 */
open class FallbackModelProvider(
    private val idGenerator: IntSupplier
): AbstractModelProvider() {

    override fun toModel(classId: ClassId): UtModel {
        return createModelByClassId(classId)
    }

    private fun createModelByClassId(classId: ClassId): UtModel = runBlocking {
        val modelConstructor = UtModelConstructor(IdentityHashMap())
        val defaultConstructor = classId.allConstructors().firstOrNull {
            it.parameters().isEmpty() && it.isPublic()
        }
        when {
            classId.isPrimitive ->
                classId.defaultValueModel()
            classId.isArray ->
                UtArrayModel(
                    id = idGenerator.asInt,
                    classId,
                    length = 0,
                    classId.ifArrayGetElementClass()!!.defaultValueModel(),
                    mutableMapOf()
                )
            classId.isIterable -> {
                @Suppress("RemoveRedundantQualifierName") // ArrayDeque must be taken from java, not from kotlin
                val defaultInstance = when {
                    defaultConstructor != null -> (defaultConstructor.asExecutable() as ConstructorExecutableId).constructor.newInstance()
                    classId isSubtypeOf asClass<java.util.ArrayList<*>>() -> ArrayList<Any>()
                    classId isSubtypeOf asClass<java.util.TreeSet<*>>() -> TreeSet<Any>()
                    classId isSubtypeOf asClass<java.util.HashMap<*,*>>() -> HashMap<Any, Any>()
                    classId isSubtypeOf asClass<java.util.ArrayDeque<*>>() -> java.util.ArrayDeque<Any>()
                    classId isSubtypeOf asClass<java.util.BitSet>() -> BitSet()
                    else -> null
                }
                if (defaultInstance != null) {
                    modelConstructor.construct(defaultInstance, classId)
                }else {
                    createSimpleModel(classId)
                }
            }
            else ->
                createSimpleModel(classId)
        }
    }

    private suspend fun createSimpleModel(classId: ClassId): UtModel {
        val defaultConstructor = classId.methods().firstOrNull {
            it.isPublic() && it.isConstructor && it.parameters().isEmpty()
        }
        return if (classId.isAbstract()) { // sealed class is abstract by itself
            UtNullModel(classId)
        } else if (defaultConstructor != null) {
            val chain = mutableListOf<UtStatementModel>()
            val model = UtAssembleModel(
                id = idGenerator.asInt,
                classId,
                classId.name,
                chain
            )
            chain.add(
                UtExecutableCallModel(model, defaultConstructor.asExecutable(), listOf(), model)
            )
            model
        } else {
            UtCompositeModel(
                id = idGenerator.asInt,
                classId,
                isMock = false
            )
        }
    }
}
