package org.utbot.framework.synthesis

import mu.KotlinLogging
import org.utbot.engine.ResolvedModels
import org.utbot.framework.modifications.StatementsStorage
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.synthesis.postcondition.constructors.toSoot

internal fun Collection<ClassId>.expandable() = filter { !it.isArray && !it.isPrimitive }.toSet()

class ConstrainedSynthesizer(
    models: ResolvedModels,
    val depth: Int = 4
) {
    val parameters = models.parameters

    companion object {
        private val logger = KotlinLogging.logger("ConstrainedSynthesizer")
        private var attempts = 0
        private var successes = 0


        fun stats(): String = buildString {
            appendLine("Total attempts: $attempts")
            appendLine("Successful attempts $successes")
            appendLine("Success rate: ${String.format("%.2f", successes.toDouble() / attempts)}")
        }

        fun success() {
            ++attempts
            ++successes
            logger.debug { stats() }
        }

        fun failure() {
            ++attempts
            logger.debug { stats() }
        }
    }

    private val logger = KotlinLogging.logger("ConstrainedSynthesizer")
    private val statementStorage = StatementsStorage().also { storage ->
        storage.update(parameters.map { it.classId }.expandable())
    }

    private val queueIterator = SynthesisUnitContextQueue(parameters, statementStorage, depth)
    private val unitChecker = ConstrainedSynthesisUnitChecker(objectClassId.toSoot())

    fun synthesize(): List<UtModel>? {
        while (queueIterator.hasNext()) {
            val units = queueIterator.next()
            if (!units.isFullyDefined) continue
            logger.debug { "Visiting state: $units" }

            val assembleModel = unitChecker.tryGenerate(units, parameters)
            if (assembleModel != null) {
                logger.debug { "Found $assembleModel" }
                success()
                return assembleModel
            }
        }
        failure()
        return null
    }
}

class SynthesisUnitContext(
    val models: List<UtModel>,
    initialMap: Map<UtModel, SynthesisUnit> = emptyMap()
) {
    private val mapping = initialMap.toMutableMap()

    val isFullyDefined get() = models.all { it.synthesisUnit.isFullyDefined() }

    init {
        models.forEach { it.synthesisUnit }
    }

    val UtModel.synthesisUnit: SynthesisUnit
        get() = mapping.getOrPut(this) {
            when (this) {
                is UtNullModel -> NullUnit(this.classId)
                is UtPrimitiveConstraintModel -> ObjectUnit(this.classId)
                is UtReferenceConstraintModel -> ObjectUnit(this.classId)
                is UtArrayConstraintModel -> ArrayUnit(
                    this.classId,
                    this.elements.toList(),
                    this.length
                )

                is UtReferenceToConstraintModel -> ReferenceToUnit(this.classId, this.reference)
                else -> error("Only UtSynthesisModel supported")
            }
        }

    operator fun get(utModel: UtModel): SynthesisUnit = mapping[utModel]
        ?: utModel.synthesisUnit

    fun set(model: UtModel, newUnit: SynthesisUnit): SynthesisUnitContext {
        val newMapping = mapping.toMutableMap()
        newMapping[model] = newUnit
        return SynthesisUnitContext(models, newMapping)
    }

    fun SynthesisUnit.isFullyDefined(): Boolean = when (this) {
        is NullUnit -> true
        is ReferenceToUnit -> true
        is ObjectUnit -> isPrimitive()
        is ArrayUnit -> elements.all {
            this@SynthesisUnitContext[it.first].isFullyDefined() && this@SynthesisUnitContext[it.second].isFullyDefined()
        }
        is MethodUnit -> params.all { it.isFullyDefined() }
    }
}

class SynthesisUnitContextQueue(
    val models: List<UtModel>,
    statementsStorage: StatementsStorage,
    val depth: Int
) : Iterator<SynthesisUnitContext> {
    private val leafExpander = CompositeUnitExpander(statementsStorage)
    val queue = ArrayDeque<SynthesisUnitContext>().also {
        it.addLast(SynthesisUnitContext(models))
    }

    override fun hasNext(): Boolean {
        return queue.isNotEmpty()
    }

    override fun next(): SynthesisUnitContext {
        val result = queue.removeFirst()
        queue.addAll(produceNext(result))
        return result
    }

    private fun produceNext(context: SynthesisUnitContext): List<SynthesisUnitContext> {
        var index = 0
        var currentContext = context
        while (true) {
            with(currentContext) {
                if (index >= models.size) {
                    return emptyList()
                }

                val currentModel = models[index]
                val newContexts = produce(currentContext, currentModel)
                if (newContexts.isEmpty()) {
                    currentContext = currentContext.set(currentModel, currentModel.synthesisUnit)
                    index++
                } else {
                    return newContexts
                }
            }
        }
    }

    private fun produce(
        context: SynthesisUnitContext,
        model: UtModel
    ): List<SynthesisUnitContext> = when (val unit = context[model]) {
        is NullUnit -> emptyList()
        is ReferenceToUnit -> emptyList()
        is MethodUnit -> produce(unit).map {
            context.set(model, it)
        }

        is ObjectUnit -> produce(unit).map {
            context.set(model, it)
        }

        is ArrayUnit -> {
            if (unit.isPrimitive()) emptyList()
            else {
                var currentContext = context
                var result = emptyList<SynthesisUnitContext>()
                var index = 0

                while (true) {
                    model as UtArrayConstraintModel
                    val current = currentContext[model] as ArrayUnit
                    val elements = current.elements
                    if (index >= elements.size) break

                    val currentModel = model.elements[unit.elements[index].first]!!
                    val newLeafs = produce(context, currentModel)
                    if (newLeafs.isEmpty()) {
                        for (i in 0..index) {
                            currentContext = currentContext.set(currentModel, currentContext[currentModel])
                        }
                        index++
                    } else {
                        result = newLeafs
                        break
                    }
                }
                result
            }
        }
    }

    private fun produce(state: SynthesisUnit): List<SynthesisUnit> =
        when (state) {
            is MethodUnit -> state.params.run {
                flatMapIndexed { idx, leaf ->
                    val newLeafs = produce(leaf)
                    newLeafs.map { newLeaf ->
                        val newParams = toMutableList()
                        newParams[idx] = newLeaf
                        state.copy(params = newParams)
                    }
                }
            }

            is ObjectUnit -> leafExpander.expand(state)
            is NullUnit -> emptyList()
            is ReferenceToUnit -> emptyList()
            is ArrayUnit -> emptyList()
        }
}
