package org.utbot.fuzzer

import org.utbot.engine.isPublic
import org.utbot.framework.concrete.UtModelConstructor
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isIterable
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.kClass
import org.utbot.fuzzer.providers.AbstractModelProvider
import java.util.*
import java.util.function.IntSupplier
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KClass

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

    fun toModel(klazz: KClass<*>): UtModel = createSimpleModelByKClass(klazz)

    private fun createModelByClassId(classId: ClassId): UtModel {
        val modelConstructor = UtModelConstructor(IdentityHashMap())
        val defaultConstructor = classId.jClass.constructors.firstOrNull {
            it.parameters.isEmpty() && it.isPublic
        }
        return when {
            classId.isPrimitive ->
                classId.defaultValueModel()
            classId.isArray ->
                UtArrayModel(
                    id = idGenerator.asInt,
                    classId,
                    length = 0,
                    classId.elementClassId!!.defaultValueModel(),
                    mutableMapOf()
                )
            classId.isIterable -> {
                @Suppress("RemoveRedundantQualifierName") // ArrayDeque must be taken from java, not from kotlin
                val defaultInstance = when {
                    defaultConstructor != null -> defaultConstructor.newInstance()
                    classId.jClass.isAssignableFrom(java.util.ArrayList::class.java) -> ArrayList<Any>()
                    classId.jClass.isAssignableFrom(java.util.TreeSet::class.java) -> TreeSet<Any>()
                    classId.jClass.isAssignableFrom(java.util.HashMap::class.java) -> HashMap<Any, Any>()
                    classId.jClass.isAssignableFrom(java.util.ArrayDeque::class.java) -> java.util.ArrayDeque<Any>()
                    classId.jClass.isAssignableFrom(java.util.BitSet::class.java) -> BitSet()
                    else -> null
                }
                if (defaultInstance != null)
                    modelConstructor.construct(defaultInstance, classId)
                else
                    createSimpleModelByKClass(classId.kClass)
            }
            else ->
                createSimpleModelByKClass(classId.kClass)
        }
    }

    private fun createSimpleModelByKClass(kclass: KClass<*>): UtModel {
        val defaultConstructor = kclass.java.constructors.firstOrNull {
            it.parameters.isEmpty() && it.isPublic // check constructor is public
        }
        return if (kclass.isAbstract) { // sealed class is abstract by itself
            UtNullModel(kclass.java.id)
        } else if (defaultConstructor != null) {
            val chain = mutableListOf<UtStatementModel>()
            val model = UtAssembleModel(
                id = idGenerator.asInt,
                kclass.id,
                kclass.id.toString(),
                chain
            )
            chain.add(
                UtExecutableCallModel(model, defaultConstructor.executableId, listOf(), model)
            )
            model
        } else {
            UtCompositeModel(
                id = idGenerator.asInt,
                kclass.id,
                isMock = false
            )
        }
    }
}
