package org.utbot.fuzzer

import kotlinx.coroutines.runBlocking
import org.utbot.framework.concrete.UtModelConstructor
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.providers.AbstractModelProvider
import org.utbot.jcdb.api.*
import java.util.*

/**
 * Provides some simple default models of any class.
 *
 * Used as a fallback implementation until other providers cover every type.
 */
open class FallbackModelProvider(
    private val idGenerator: IdGenerator<Int>
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
            classId.isPrimitive || classId.isEnum || classId.isClassType ->
                classId.defaultValueModel()
            classId.isArray ->
                UtArrayModel(
                    id = idGenerator.createId(),
                    classId,
                    length = 0,
                    classId.ifArrayGetElementClass()!!.defaultValueModel(),
                    mutableMapOf()
                )
            classId.isIterable -> with(reflection) {
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

    private suspend fun createSimpleModel(classId: ClassId): UtModel = with(reflection) {
        val defaultConstructor = classId.methods().firstOrNull {
            it.isPublic() && it.isConstructor && it.parameters().isEmpty()
        }
        val kclass = classId.kClass
        return when {
            kclass.isAbstract -> {
                // sealed class is abstract by itself
                UtNullModel(kclass.java.id)

            }
            kclass.java.isEnum || kclass == java.lang.Class::class -> {
                // No sensible fallback solution for these classes except returning default `null` value
                UtNullModel(kclass.java.id)
            }
            defaultConstructor != null -> {
                val chain = mutableListOf<UtStatementModel>()
                val model = UtAssembleModel(
                    id = idGenerator.createId(),
                    kclass.id,
                    kclass.id.toString(),
                    chain
                )
                chain.add(
                    UtExecutableCallModel(model, defaultConstructor.asExecutableConstructor(), listOf(), model)
                )
                model
            }
            else -> {
                UtCompositeModel(
                    id = idGenerator.createId(),
                    kclass.id,
                    isMock = false
                )
            }
        }
    }
}
