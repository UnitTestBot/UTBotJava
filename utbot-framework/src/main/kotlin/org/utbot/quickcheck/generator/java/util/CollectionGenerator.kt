package org.utbot.quickcheck.generator.java.util

import org.utbot.engine.greyboxfuzzer.util.FuzzerIllegalStateException
import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.quickcheck.generator.*
import org.utbot.quickcheck.internal.Ranges
import org.utbot.quickcheck.random.SourceOfRandomness

/**
 *
 * Base class for generators of [Collection]s.
 *
 *
 * The generated collection has a number of elements limited by
 * [GenerationStatus.size], or else by the attributes of a [Size]
 * marking. The individual elements will have a type corresponding to the
 * collection's type argument.
 *
 * @param <T> the type of collection generated
</T> */
abstract class CollectionGenerator(type: Class<*>) : ComponentizedGenerator(type) {
    private var sizeRange: Size? = null
    private var distinct = false

    /**
     *
     * Tells this generator to add elements to the generated collection
     * a number of times within a specified minimum and/or maximum, inclusive,
     * chosen with uniform distribution.
     *
     *
     * Note that some kinds of collections disallow duplicates, so the
     * number of elements added may not be equal to the collection's
     * [Collection.size].
     *
     * @param size annotation that gives the size constraints
     */
    open fun configure(size: Size) {
        sizeRange = size
        Ranges.checkRange(Ranges.Type.INTEGRAL, size.min, size.max)
    }

    /**
     * Tells this generator to add elements which are distinct from each other.
     *
     * @param distinct Generated elements will be distinct if this param is
     * not null
     */
    fun configure(distinct: Distinct?) {
        setDistinct(distinct != null)
    }

    protected fun setDistinct(distinct: Boolean) {
        this.distinct = distinct
    }


    override fun createModifiedUtModel(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        val cachedModel = generatedUtModel ?: throw FuzzerIllegalStateException("Nothing to modify")
        val collectionClassId = types().single().id
        val collectionConstructorId = ConstructorId(collectionClassId, emptyList())
        val genId = cachedModel.getIdOrThrow()
        return UtAssembleModel(
            genId,
            cachedModel.classId,
            collectionConstructorId.name + "#" + genId,
            UtExecutableCallModel(null, collectionConstructorId, emptyList())
        ) {
            val addMethodId = MethodId(classId, "add", booleanClassId, listOf(objectClassId))
            (0 until nestedGenerators.size).map { ind ->
                val generator = nestedGenerators[ind]
                val item = generator.generateImpl(random, status)
                generator.generationState = GenerationState.CACHE
                UtExecutableCallModel(
                    this,
                    addMethodId,
                    listOf(item)
                )
            }
        }
    }

    private fun regenerate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        nestedGenerators.clear()
        val collectionClassId = types().single().id
        val collectionConstructorId = ConstructorId(collectionClassId, emptyList())
        val genId =  generatorContext.utModelConstructor.computeUnusedIdAndUpdate()
        return UtAssembleModel(
            genId,
            collectionClassId,
            collectionConstructorId.name + "#" + genId,
            UtExecutableCallModel(null, collectionConstructorId, emptyList()),
        ) {
            val size = size(random, status)
            (0..size).map {
                val addMethodId = MethodId(classId, "add", booleanClassId, listOf(objectClassId))
                val generator = componentGenerators().first().copy().also { nestedGenerators.add(it) }
                val item = generator.generateImpl(random, status)
                UtExecutableCallModel(this, addMethodId, listOf(item))
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
        return 1
    }

    private fun size(random: SourceOfRandomness, status: GenerationStatus): Int {
        return if (sizeRange != null) random.nextInt(sizeRange!!.min, sizeRange!!.max) else status.size()
    }
}