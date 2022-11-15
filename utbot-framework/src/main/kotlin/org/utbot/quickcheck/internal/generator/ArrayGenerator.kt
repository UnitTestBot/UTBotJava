package org.utbot.quickcheck.internal.generator

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.booleanArrayClassId
import org.utbot.framework.plugin.api.util.byteArrayClassId
import org.utbot.framework.plugin.api.util.charArrayClassId
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.doubleArrayClassId
import org.utbot.framework.plugin.api.util.floatArrayClassId
import org.utbot.framework.plugin.api.util.intArrayClassId
import org.utbot.framework.plugin.api.util.longArrayClassId
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.shortArrayClassId
import org.utbot.quickcheck.generator.Distinct
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.generator.Generators
import org.utbot.quickcheck.generator.Size
import org.utbot.quickcheck.internal.Ranges
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.random.SourceOfRandomness
import java.lang.reflect.AnnotatedType

class ArrayGenerator(private val componentType: Class<*>, val component: Generator) : Generator(Any::class.java) {
    private var lengthRange: Size? = null
    private var distinct = false

    /**
     * Tells this generator to produce values with a length within a specified
     * minimum and/or maximum, inclusive, chosen with uniform distribution.
     *
     * @param size annotation that gives the length constraints
     */
    fun configure(size: Size) {
        lengthRange = size
        Ranges.checkRange(Ranges.Type.INTEGRAL, size.min, size.max)
    }

    /**
     * Tells this generator to produce values which are distinct from each
     * other.
     *
     * @param distinct Generated values will be distinct if this param is not
     * null.
     */
    fun configure(distinct: Distinct?) {
        this.distinct = distinct != null
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val length = length(random, status)
        val componentTypeId = classIdForType(componentType)
        val modelId = utModelConstructor.computeUnusedIdAndUpdate()
        return UtArrayModel(
            modelId,
            getClassIdForArrayType(componentType),
            length,
            componentTypeId.defaultValueModel(),
            (0 until length).associateWithTo(hashMapOf()) { component.generate(random, status) }
        )
    }

    private fun getClassIdForArrayType(componentType: Class<*>): ClassId = when (componentType) {
        Int::class.javaPrimitiveType -> intArrayClassId
        Boolean::class.javaPrimitiveType -> booleanArrayClassId
        Byte::class.javaPrimitiveType -> byteArrayClassId
        Char::class.javaPrimitiveType -> charArrayClassId
        Double::class.javaPrimitiveType -> doubleArrayClassId
        Float::class.javaPrimitiveType -> floatArrayClassId
        Long::class.javaPrimitiveType -> longArrayClassId
        Short::class.javaPrimitiveType -> shortArrayClassId
        else -> ClassId("[L", classIdForType(componentType))
    }

    override fun provide(provided: Generators) {
        super.provide(provided)
        component.provide(provided)
    }

    override fun configure(annotatedType: AnnotatedType?) {
        super.configure(annotatedType)
        val annotated = Reflection.annotatedComponentTypes(annotatedType)
        if (annotated.isNotEmpty()) {
            component.configure(annotated[0])
        }
    }

    private fun length(random: SourceOfRandomness, status: GenerationStatus): Int {
        return if (lengthRange != null) random.nextInt(lengthRange!!.min, lengthRange!!.max) else status.size()
    }
}