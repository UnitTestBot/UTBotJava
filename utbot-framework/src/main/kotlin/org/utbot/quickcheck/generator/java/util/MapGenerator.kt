package org.utbot.quickcheck.generator.java.util

import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.quickcheck.generator.ComponentizedGenerator
import org.utbot.quickcheck.generator.Distinct
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Size
import org.utbot.quickcheck.internal.Ranges
import org.utbot.quickcheck.random.SourceOfRandomness

/**
 *
 * Base class for generators of [Map]s.
 *
 *
 * The generated map has a number of entries limited by
 * [GenerationStatus.size], or else by the attributes of a [Size]
 * marking. The individual keys and values will have types corresponding to the
 * property parameter's type arguments.
 *
 * @param <T> the type of map generated
</T> */
abstract class MapGenerator protected constructor(type: Class<*>) : ComponentizedGenerator(type) {
    private var sizeRange: Size? = null
    private var distinct = false

    /**
     *
     * Tells this generator to add key-value pairs to the generated map a
     * number of times within a specified minimum and/or maximum, inclusive,
     * chosen with uniform distribution.
     *
     *
     * Note that maps disallow duplicate keys, so the number of pairs added
     * may not be equal to the map's [Map.size].
     *
     * @param size annotation that gives the size constraints
     */
    fun configure(size: Size) {
        sizeRange = size
        Ranges.checkRange(Ranges.Type.INTEGRAL, size.min, size.max)
    }

    /**
     * Tells this generator to add entries whose keys are distinct from
     * each other.
     *
     * @param distinct Keys of generated entries will be distinct if this
     * param is not null
     */
    fun configure(distinct: Distinct?) {
        this.distinct = distinct != null
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val size = size(random, status)
        val classId = types().single().id
        val keyGenerator = componentGenerators()[0]
        val valueGenerator = componentGenerators()[1]

        val generatedModelId = utModelConstructor.computeUnusedIdAndUpdate()
        val constructorId = ConstructorId(classId, emptyList())
        return UtAssembleModel(
            generatedModelId,
            classId,
            constructorId.name + "#" + generatedModelId,
            UtExecutableCallModel(null, constructorId, emptyList()),
        ) {
            val putMethodId = MethodId(classId, "put", objectClassId, listOf(objectClassId, objectClassId))
            generateSequence {
                val key = keyGenerator.generate(random, status)
                val value = valueGenerator.generate(random, status)
                key to value
            }.filter { (key, value) ->
                okToAdd(key, value)
            }.map { (key, value) ->
                UtExecutableCallModel(this, putMethodId, listOf(key, value))
            }.take(size).toList()
        }
    }

    override fun numberOfNeededComponents(): Int {
        return 2
    }

    protected open fun okToAdd(key: Any?, value: Any?): Boolean {
        return true
    }

    private fun size(random: SourceOfRandomness, status: GenerationStatus): Int {
        return if (sizeRange != null) random.nextInt(sizeRange!!.min, sizeRange!!.max) else status.size()
    }
}