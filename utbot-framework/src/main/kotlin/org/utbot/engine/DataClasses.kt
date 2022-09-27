package org.utbot.engine

import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.engine.TypeRegistry.Companion.objectTypeStorage
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.UtInstanceOfExpression
import org.utbot.engine.pc.UtIsExpression
import org.utbot.engine.pc.UtTrue
import org.utbot.engine.pc.mkAnd
import org.utbot.engine.pc.mkOr
import org.utbot.engine.symbolic.*
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtInstrumentation
import soot.RefType
import java.util.Objects
import soot.Scene
import soot.SootMethod
import soot.Type
import soot.jimple.Stmt
import soot.toolkits.graph.ExceptionalUnitGraph

/**
 * Result of method invocation: either Success or Failure with some exception
 */
sealed class SymbolicResult

data class SymbolicSuccess(val value: SymbolicValue) : SymbolicResult()

/**
 * Represents symbolic failure.
 *
 * Contains:
 * * symbolic - symbolic value of Exception
 * * concrete - sometimes we have concrete value of Exception
 * * explicit - exception thrown explicitly with throw instruction
 * * inNested - exception thrown in nested call
 *
 */
data class SymbolicFailure(
    val symbolic: SymbolicValue,
    val concrete: Throwable?,
    val explicit: Boolean,
    val inNestedMethod: Boolean
) : SymbolicResult()

/**
 * Explicitly thrown exception. Exception cluster depends on inNestedMethod.
 */
fun explicitThrown(symbolic: SymbolicValue, inNestedMethod: Boolean) =
    SymbolicFailure(symbolic, extractConcrete(symbolic), explicit = true, inNestedMethod = inNestedMethod)

/**
 * Explicitly thrown exception. Exception cluster depends on inNestedMethod.
 */
fun explicitThrown(exception: Throwable, addr: UtAddrExpression, inNestedMethod: Boolean) =
    SymbolicFailure(symbolic(exception, addr), exception, explicit = true, inNestedMethod = inNestedMethod)

/**
 * Implicitly thrown exception. There are no difference if it happens in nested call or not.
 */
fun implicitThrown(exception: Throwable, addr: UtAddrExpression, inNestedMethod: Boolean) =
    SymbolicFailure(symbolic(exception, addr), exception, explicit = false, inNestedMethod = inNestedMethod)

private fun symbolic(exception: Throwable, addr: UtAddrExpression) =
    objectValue(Scene.v().getRefType(exception.javaClass.canonicalName), addr, ThrowableWrapper(exception))

private fun extractConcrete(symbolic: SymbolicValue) =
    (symbolic.asWrapperOrNull as? ThrowableWrapper)?.throwable

inline fun <R> SymbolicFailure.fold(
    onConcrete: (concrete: Throwable) -> R,
    onSymbolic: (symbolic: SymbolicValue) -> R
): R {
    return if (concrete != null) {
        onConcrete(concrete)
    } else {
        onSymbolic(symbolic)
    }
}

data class Parameter(private val localVariable: LocalVariable, private val type: Type, val value: SymbolicValue)

/**
 * Keeps most common type and possible types, to resolve types in uncertain situations, like virtual invokes.
 *
 * Note: [leastCommonType] might be an interface or abstract type in opposite to the [possibleConcreteTypes]
 * that **usually** contains only concrete types (so-called appropriate). The only way to create [TypeStorage] with
 * inappropriate possibleType is to create it using constructor with the only type.
 *
 * @see isAppropriate
 */
data class TypeStorage(val leastCommonType: Type, val possibleConcreteTypes: Set<Type>) {
    private val hashCode = Objects.hash(leastCommonType, possibleConcreteTypes)

    /**
     * Construct a type storage with some type. In this case [possibleConcreteTypes] might contains
     * abstract class or interface. Usually it means such typeStorage represents wrapper object type.
     */
    constructor(concreteType: Type) : this(concreteType, setOf(concreteType))

    fun isObjectTypeStorage(): Boolean = possibleConcreteTypes.size == objectTypeStorage.possibleConcreteTypes.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeStorage

        if (leastCommonType != other.leastCommonType) return false
        if (possibleConcreteTypes != other.possibleConcreteTypes) return false

        return true
    }

    override fun hashCode() = hashCode

    override fun toString() = if (possibleConcreteTypes.size == 1) {
        "$leastCommonType"
    } else {
        "(leastCommonType=$leastCommonType, ${possibleConcreteTypes.size} possibleTypes=${possibleConcreteTypes.take(10)})"
    }
}

sealed class InvokeResult

data class MethodResult(
    val symbolicResult: SymbolicResult,
    val symbolicStateUpdate: SymbolicStateUpdate = SymbolicStateUpdate()
) : InvokeResult() {
    val memoryUpdates by symbolicStateUpdate::memoryUpdates

    constructor(
        symbolicResult: SymbolicResult,
        hardConstraints: HardConstraint = emptyHardConstraint(),
        softConstraints: SoftConstraint = emptySoftConstraint(),
        assumption: Assumption = emptyAssumption(),
        memoryUpdates: MemoryUpdate = MemoryUpdate()
    ) : this(symbolicResult, SymbolicStateUpdate(hardConstraints, softConstraints, assumption, memoryUpdates))

    constructor(
        value: SymbolicValue,
        hardConstraints: HardConstraint = emptyHardConstraint(),
        softConstraints: SoftConstraint = emptySoftConstraint(),
        assumption: Assumption = emptyAssumption(),
        memoryUpdates: MemoryUpdate = MemoryUpdate(),
    ) : this(SymbolicSuccess(value), hardConstraints, softConstraints, assumption, memoryUpdates)
}

/**
 * Data class that represents new graph to traverse as a InvokeResult.
 * @param graph - graph of method that need to be traversed
 * @param constraints - UtBoolExpressions that need to be
 */
data class GraphResult(
    val graph: ExceptionalUnitGraph,
    val constraints: Set<UtBoolExpression> = emptySet()
) : InvokeResult()

data class Environment(var method: SootMethod, var state: ExecutionState)

data class Step(
    val stmt: Stmt,
    val depth: Int,
    val decision: Int
)

data class OverrideResult(
    val success: Boolean,
    val results: List<InvokeResult> = emptyList()
) {
    constructor(
        success: Boolean,
        result: InvokeResult
    ) : this(success, listOf(result))
}

/**
 * Contains information about resolved condition:
 * * [condition] is the resolved condition itself: i.e., for `x > 5` it will be `Ge(x, 5)`
 * * [softConstraints] are the constraints we want to add to make result values more human-readable
 * * [symbolicStateUpdates] are used to update symbolic state in the postive and negative case respectively. For now
 * there is only one usage of it: to support instanceof for arrays we have to update them in the memory.
 *
 * @see UtInstanceOfExpression
 * @see Traverser.resolveIfCondition
 */
data class ResolvedCondition(
    val condition: UtBoolExpression,
    val softConstraints: SoftConstraintsForResolvedCondition = SoftConstraintsForResolvedCondition(),
    val symbolicStateUpdates: SymbolicStateUpdateForResolvedCondition = SymbolicStateUpdateForResolvedCondition(),
)

/**
 * Contains soft constraints for the resolved condition. It might be suitable for cases when we want to add
 * preferred values to both positive and negative cases.
 *
 * In example, we have a condition: x > 5.
 * [positiveCaseConstraint] might be `x == 6` and [negativeCaseConstraint] might be `x == 5`
 */
data class SoftConstraintsForResolvedCondition(
    val positiveCaseConstraint: UtBoolExpression? = null,
    val negativeCaseConstraint: UtBoolExpression? = null
)

/**
 * Contains updates for the resolved condition.
 */
data class SymbolicStateUpdateForResolvedCondition(
    val positiveCase: SymbolicStateUpdate = SymbolicStateUpdate(),
    val negativeCase: SymbolicStateUpdate = SymbolicStateUpdate()
) {
    fun swap() = SymbolicStateUpdateForResolvedCondition(
        negativeCase,
        positiveCase
    )
}


data class ResolvedExecution(
    val modelsBefore: ResolvedModels,
    val modelsAfter: ResolvedModels,
    val instrumentation: List<UtInstrumentation>
)

/**
 * Class containing information for memory update of the static field.
 */
data class StaticFieldMemoryUpdateInfo(
    val fieldId: FieldId,
    val value: SymbolicValue
)

/**
 * Class containing two states for [fieldId]: value of the first initialization and the last value of the [fieldId]
 */
data class FieldStates(val stateBefore: SymbolicValue, val stateAfter: SymbolicValue)

/**
 * Wrapper to add order by [id] for elements of the [MockInfoEnriched.executables].
 *
 * It is important in situations like:
 *
 *     void foo(A fst, A snd) {
 *         int a = fst.generateInt();
 *         int b = snd.generateInt();
 *         if (a + b > 10) {
 *             doSomething()
 *         }
 *     }
 *
 * If 'fst' and 'snd' have the same address, we should merge their executables into one list. To set order for the
 * elements we add unique id corresponded to the time of the call.
 */
data class MockExecutableInstance(val id: Int, val value: SymbolicValue)

/**
 * A class containing constraints for object's type.
 *
 * * [isConstraint] responsible for the type choice: information about typeId and numberOfDimensions
 * * [nullConstraint] is required to represent possible equality of the object to null.
 * It is not a part of the [isConstraint] to make it possible to negate `isExpression` without losing this equality.
 * * [correctnessConstraint] is about correctness of the type. It has information about borders for the typeId,
 * information about minimum and maximum numDimensions values and constraints for correct array constructions (i.e.,
 * that Object[] can be transformed in an array of primitives with at least numberDimensions equals two).
 *
 * It is important to separate these constraints because of `instanceof` instruction. We should negate [isConstraint],
 * but we should not touch [correctnessConstraint] to make sure we constructed a possible type.
 *
 * Note: use [isConstraint] whenever it is needed to clarify a type of the already constructed object.
 * For object creation both [isConstraint] and [correctnessConstraint] must be used.
 */
data class TypeConstraint(
    private val isConstraint: UtIsExpression,
    private val nullConstraint: UtBoolExpression,
    private val correctnessConstraint: UtBoolExpression
) {
    val hashcode: Int = Objects.hash(isConstraint(), correctnessConstraint)

    /**
     * Returns a conjunction of the [isConstraint] and [correctnessConstraint]. Suitable for an object creation.
     */
    fun all(): UtBoolExpression = mkAnd(isOrNullConstraint(), correctnessConstraint)

    /**
     * Returns a condition that either the object is an instance of the types in [isConstraint], or it is null.
     */
    fun isOrNullConstraint(): UtBoolExpression = mkOr(isConstraint(), nullConstraint)

    /**
     * Returns a pure [isConstraint]. You should use this whenever you sure the object cannot be null.
     * For example, it is suitable for instanceof checks or negation of equality with some types.
     * NOTE: for Object we always return UtTrue.
     */
    fun isConstraint(): UtBoolExpression = if (isConstraint.typeStorage.isObjectTypeStorage()) UtTrue else isConstraint

    override fun hashCode(): Int = this.hashcode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeConstraint

        if (isConstraint != other.isConstraint) return false
        if (correctnessConstraint != other.correctnessConstraint) return false
        if (hashcode != other.hashcode) return false

        return true
    }
}

/**
 * A class that represents instance field read operations.
 *
 * Tracking read accesses is necessary to check whether the specific field of a parameter object
 * should be initialized. We don't need to initialize fields that are not accessed in the method being tested.
 */
data class InstanceFieldReadOperation(val addr: UtAddrExpression, val fieldId: FieldId)
