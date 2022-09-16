package org.utbot.engine.symbolic

import org.utbot.engine.LocalMemoryUpdate
import org.utbot.engine.MemoryUpdate
import org.utbot.engine.pc.UtBoolExpression
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet

/**
 * Represents appendable-only immutable constraints.
 */
sealed class Constraint<T : Constraint<T>>(constraints: Set<UtBoolExpression> = emptySet()) {
    private val _constraints: PersistentSet<UtBoolExpression> = constraints.toPersistentSet()
    val constraints: Set<UtBoolExpression> by ::_constraints

    protected fun addConstraints(constraints: Collection<UtBoolExpression>): Set<UtBoolExpression> =
        _constraints.addAll(constraints)

    abstract operator fun plus(other: T): T
}

/**
 * Represents hard constraints.
 */
open class HardConstraint(
    constraints: Set<UtBoolExpression> = emptySet()
) : Constraint<HardConstraint>(constraints) {
    override fun plus(other: HardConstraint): HardConstraint =
        HardConstraint(addConstraints(other.constraints))

    companion object {
        internal val EMPTY: HardConstraint = HardConstraint()
    }
}

fun emptyHardConstraint(): HardConstraint = HardConstraint.EMPTY

/**
 * Represents soft constraints.
 */
class SoftConstraint(
    constraints: Set<UtBoolExpression> = emptySet()
) : Constraint<SoftConstraint>(constraints) {
    override fun plus(other: SoftConstraint): SoftConstraint =
        SoftConstraint(addConstraints(other.constraints))

    companion object {
        internal val EMPTY: SoftConstraint = SoftConstraint()
    }
}

fun emptySoftConstraint(): SoftConstraint = SoftConstraint.EMPTY

/**
 * Represent constraints that must be satisfied for symbolic execution.
 * At the same time, if they don't, the state they belong to still
 * might be executed concretely without these assume.
 *
 * @see
 */
class Assumption(
    constraints: Set<UtBoolExpression> = emptySet()
): Constraint<Assumption>(constraints) {
    override fun plus(other: Assumption): Assumption = Assumption(addConstraints(other.constraints))

    override fun toString() = constraints.joinToString(System.lineSeparator())

    companion object {
        internal val EMPTY: Assumption = Assumption()
    }
}

fun emptyAssumption(): Assumption = Assumption.EMPTY

/**
 * Represents one or more updates that can be applied to [SymbolicState].
 *
 * TODO: move [localMemoryUpdates] to another place
 */
data class SymbolicStateUpdate(
    val hardConstraints: HardConstraint = emptyHardConstraint(),
    val softConstraints: SoftConstraint = emptySoftConstraint(),
    val assumptions: Assumption = emptyAssumption(),
    val memoryUpdates: MemoryUpdate = MemoryUpdate(),
    val localMemoryUpdates: LocalMemoryUpdate = LocalMemoryUpdate()
) {
    operator fun plus(update: SymbolicStateUpdate): SymbolicStateUpdate =
        SymbolicStateUpdate(
            hardConstraints = hardConstraints + update.hardConstraints,
            softConstraints = softConstraints + update.softConstraints,
            assumptions = assumptions + update.assumptions,
            memoryUpdates = memoryUpdates + update.memoryUpdates,
            localMemoryUpdates = localMemoryUpdates + update.localMemoryUpdates
        )

    operator fun plus(update: HardConstraint): SymbolicStateUpdate =
        plus(SymbolicStateUpdate(hardConstraints = update))

    operator fun plus(update: SoftConstraint): SymbolicStateUpdate =
        plus(SymbolicStateUpdate(softConstraints = update))

    operator fun plus(update: Assumption): SymbolicStateUpdate =
        plus(SymbolicStateUpdate(assumptions = update))

    operator fun plus(update: MemoryUpdate): SymbolicStateUpdate =
        plus(SymbolicStateUpdate(memoryUpdates = update))

    operator fun plus(update: LocalMemoryUpdate): SymbolicStateUpdate =
        plus(SymbolicStateUpdate(localMemoryUpdates = update))
}

/**
 * This method does not copy expressions in case using with [Set].
 * If [this] should be copied, do it manually.
 */
fun Collection<UtBoolExpression>.asHardConstraint() = HardConstraint(transformToSet())

fun UtBoolExpression.asHardConstraint() = HardConstraint(setOf(this))

/**
 * This method does not copy expressions in case using with [Set].
 * If [this] should be copied, do it manually.
 */
fun Collection<UtBoolExpression>.asSoftConstraint() = SoftConstraint(transformToSet())

fun UtBoolExpression.asSoftConstraint() = SoftConstraint(setOf(this))

fun Collection<UtBoolExpression>.asAssumption() = Assumption(transformToSet())

fun UtBoolExpression.asAssumption() = Assumption(setOf(this))

private fun <T> Collection<T>.transformToSet(): Set<T> = if (this is Set<T>) this else toSet()

fun HardConstraint.asUpdate() = SymbolicStateUpdate(hardConstraints = this)
fun SoftConstraint.asUpdate() = SymbolicStateUpdate(softConstraints = this)
fun MemoryUpdate.asUpdate() = SymbolicStateUpdate(memoryUpdates = this)
