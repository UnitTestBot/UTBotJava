package org.utbot.external.api

import org.utbot.framework.assemble.AssembleModelGenerator
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicInteger


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
        fields.entries.associate { entry -> FieldId(classId, entry.key) to entry.value }.toMutableMap(),
        mocks
    )

    @JvmOverloads
    fun produceAssembleModel(
        methodUnderTest: Method,
        classUnderTest: Class<*>,
        models: List<UtModel>
    ): IdentityHashMap<UtModel, UtModel> =
        AssembleModelGenerator(classUnderTest.packageName)
            .createAssembleModels(models)

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
        classIdForType(clazz),
        clazz
    )
}

fun fieldIdForJavaField(field: Field): FieldId {
    val declaringClass = field.declaringClass
    return FieldId(ClassId(field.declaringClass.canonicalName), field.name)
}

/**
 * This is just a wrapper function to call from Java code
 * @param clazz a class that the id is supposed to be created
 */
fun classIdForType(clazz: Class<*>): ClassId {
    return clazz.id
}