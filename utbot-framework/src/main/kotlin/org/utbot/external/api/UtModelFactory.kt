package org.utbot.external.api

import org.utbot.framework.assemble.AssembleModelGenerator
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.findField
import org.utbot.framework.plugin.api.util.id
import org.utbot.jcdb.api.ClassId
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.jvm.kotlinFunction


class UtModelFactory(
    // generateMocks
    private val generateMocksForFunctionsWithoutParameters: Boolean = false
) {
    private val modelIdCounter: AtomicInteger = AtomicInteger(0)

    /**
     * Produces a composite model with a unique id. Takes into account
     * generateMocksForFunctionsWithoutParameters constructor parameter.
     * @param classId class id. To create the class id it is handy to use
     *        the classIdForType function
     * @param fields models of the object fields
     * @param mocks mocks for the functions
     */
    @JvmOverloads
    fun produceCompositeModel(
        classId: ClassId,
        fields: Map<String, UtModel> = emptyMap(),
        mocks: MutableMap<ExecutableId, List<UtModel>> = mutableMapOf()
    ): UtCompositeModel = UtCompositeModel(
        modelIdCounter.incrementAndGet(),
        classId,
        generateMocksForFunctionsWithoutParameters,
        fields.entries.associate { entry -> classId.findField(entry.key) to entry.value }.toMutableMap(),
        mocks
    )

    @JvmOverloads
    fun produceAssembleModel(
        methodUnderTest: Method,
        classUnderTest: Class<*>,
        models: List<UtModel>
    ): IdentityHashMap<UtModel, UtModel> = AssembleModelGenerator(UtMethod(methodUnderTest.kotlinFunction!!, classUnderTest.kotlin)).
    createAssembleModels(models)

    @JvmOverloads
    fun produceArrayModel(
        classId: ClassId,
        length: Int = 0,
        constModel: UtModel,  // todo remove dependency from the Sergey's concrete execution code
        elements: Map<Int, UtModel>
    ): UtArrayModel = UtArrayModel(
        modelIdCounter.incrementAndGet(),
        classId,
        length,
        constModel,
        elements.toMutableMap()
    )

    fun produceClassRefModel(clazz: Class<*>) = UtClassRefModel(
        modelIdCounter.incrementAndGet(),
        clazz.id,
        clazz
    )
}

/**
 * This is just a wrapper function to call from Java code
 * @param clazz a class that the id is supposed to be created
 */
fun classIdForType(clazz: Class<*>): ClassId {
    return clazz.id
}