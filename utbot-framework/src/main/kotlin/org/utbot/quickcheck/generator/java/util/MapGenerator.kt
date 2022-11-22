package org.utbot.quickcheck.generator.java.util

import org.utbot.engine.greyboxfuzzer.util.FuzzerIllegalStateException
import org.utbot.engine.greyboxfuzzer.util.UtModelGenerator.utModelConstructor
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.quickcheck.generator.*
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

    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        val cachedUtModel = generatedUtModel ?: throw FuzzerIllegalStateException("Nothing to modify")
        val size = nestedGenerators.size / 2
        val classId = types().single().id

        val modelId = cachedUtModel.getIdOrThrow()
        val constructorId = ConstructorId(classId, emptyList())
        return UtAssembleModel(
            modelId,
            classId,
            constructorId.name + "#" + modelId,
            UtExecutableCallModel(null, constructorId, emptyList()),
        ) {
            val putMethodId = MethodId(classId, "put", objectClassId, listOf(objectClassId, objectClassId))
            (0 until size).map { ind ->
                val keyGenerator = nestedGenerators[ind * 2]
                val valueGenerator = nestedGenerators[ind * 2 + 1]
                val key = keyGenerator.generateImpl(random, status)
                val value = valueGenerator.generateImpl(random, status)
                keyGenerator.generationState = GenerationState.CACHE
                valueGenerator.generationState = GenerationState.CACHE
                UtExecutableCallModel(this, putMethodId, listOf(key, value))
            }
        }
    }
    private fun regenerate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val size = size(random, status)
        val classId = types().single().id

        val generatedModelId = utModelConstructor.computeUnusedIdAndUpdate()
        val constructorId = ConstructorId(classId, emptyList())
        nestedGenerators.clear()
        return UtAssembleModel(
            generatedModelId,
            classId,
            constructorId.name + "#" + generatedModelId,
            UtExecutableCallModel(null, constructorId, emptyList()),
        ) {
            val putMethodId = MethodId(classId, "put", objectClassId, listOf(objectClassId, objectClassId))
            (0..size).map {
                val keyGenerator = componentGenerators()[0].copy().also { nestedGenerators.add(it) }
                val valueGenerator = componentGenerators()[1].copy().also { nestedGenerators.add(it) }
                val key = keyGenerator.generateImpl(random, status)
                val value = valueGenerator.generateImpl(random, status)
                key to value
                UtExecutableCallModel(this, putMethodId, listOf(key, value))
            }
        }
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel =
        when (generationState) {
            GenerationState.REGENERATE -> regenerate(random, status)
            GenerationState.MODIFY -> modify(random, status)
            GenerationState.MODIFYING_CHAIN -> createModifiedUtModel(random, status)
            GenerationState.CACHE -> generatedUtModel ?: throw FuzzerIllegalStateException("No cached model")
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