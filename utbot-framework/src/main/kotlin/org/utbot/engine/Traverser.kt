package org.utbot.engine

import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import org.utbot.common.WorkaroundReason.HACK
import org.utbot.framework.UtSettings.ignoreStaticsFromTrustedLibraries
import org.utbot.common.WorkaroundReason.IGNORE_STATICS_FROM_TRUSTED_LIBRARIES
import org.utbot.common.unreachableBranch
import org.utbot.common.withAccessibility
import org.utbot.common.workaround
import org.utbot.engine.overrides.UtArrayMock
import org.utbot.engine.overrides.UtLogicMock
import org.utbot.engine.overrides.UtOverrideMock
import org.utbot.engine.pc.NotBoolExpression
import org.utbot.engine.pc.UtAddNoOverflowExpression
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtAndBoolExpression
import org.utbot.engine.pc.UtArrayApplyForAll
import org.utbot.engine.pc.UtArrayExpressionBase
import org.utbot.engine.pc.UtArraySelectExpression
import org.utbot.engine.pc.UtArraySetRange
import org.utbot.engine.pc.UtArraySort
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.UtBoolOpExpression
import org.utbot.engine.pc.UtBvConst
import org.utbot.engine.pc.UtBvLiteral
import org.utbot.engine.pc.UtByteSort
import org.utbot.engine.pc.UtCastExpression
import org.utbot.engine.pc.UtCharSort
import org.utbot.engine.pc.UtContextInitializer
import org.utbot.engine.pc.UtExpression
import org.utbot.engine.pc.UtFalse
import org.utbot.engine.pc.UtInstanceOfExpression
import org.utbot.engine.pc.UtIntSort
import org.utbot.engine.pc.UtIsExpression
import org.utbot.engine.pc.UtIteExpression
import org.utbot.engine.pc.UtLongSort
import org.utbot.engine.pc.UtMkTermArrayExpression
import org.utbot.engine.pc.UtNegExpression
import org.utbot.engine.pc.UtOrBoolExpression
import org.utbot.engine.pc.UtPrimitiveSort
import org.utbot.engine.pc.UtShortSort
import org.utbot.engine.pc.UtSolver
import org.utbot.engine.pc.UtSolverStatusSAT
import org.utbot.engine.pc.UtSubNoOverflowExpression
import org.utbot.engine.pc.UtTrue
import org.utbot.engine.pc.addrEq
import org.utbot.engine.pc.align
import org.utbot.engine.pc.cast
import org.utbot.engine.pc.findTheMostNestedAddr
import org.utbot.engine.pc.isInteger
import org.utbot.engine.pc.mkAnd
import org.utbot.engine.pc.mkBVConst
import org.utbot.engine.pc.mkBoolConst
import org.utbot.engine.pc.mkChar
import org.utbot.engine.pc.mkEq
import org.utbot.engine.pc.mkFalse
import org.utbot.engine.pc.mkFpConst
import org.utbot.engine.pc.mkInt
import org.utbot.engine.pc.mkNot
import org.utbot.engine.pc.mkOr
import org.utbot.engine.pc.select
import org.utbot.engine.pc.store
import org.utbot.engine.symbolic.emptyAssumption
import org.utbot.engine.symbolic.emptyHardConstraint
import org.utbot.engine.symbolic.emptySoftConstraint
import org.utbot.engine.symbolic.SymbolicStateUpdate
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.symbolic.asSoftConstraint
import org.utbot.engine.symbolic.asAssumption
import org.utbot.engine.symbolic.asUpdate
import org.utbot.engine.util.trusted.isFromTrustedLibrary
import org.utbot.engine.util.statics.concrete.associateEnumSootFieldsWithConcreteValues
import org.utbot.engine.util.statics.concrete.isEnumAffectingExternalStatics
import org.utbot.engine.util.statics.concrete.isEnumValuesFieldName
import org.utbot.engine.util.statics.concrete.makeEnumNonStaticFieldsUpdates
import org.utbot.engine.util.statics.concrete.makeEnumStaticFieldsUpdates
import org.utbot.engine.util.statics.concrete.makeSymbolicValuesFromEnumConcreteValues
import org.utbot.framework.UtSettings
import org.utbot.framework.UtSettings.maximizeCoverageUsingReflection
import org.utbot.framework.UtSettings.preferredCexOption
import org.utbot.framework.UtSettings.substituteStaticsWithSymbolicVariable
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.executable
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isConstructor
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.util.executableId
import org.utbot.framework.util.graph
import org.utbot.framework.util.isInaccessibleViaReflection
import java.lang.reflect.ParameterizedType
import kotlin.collections.plus
import kotlin.collections.plusAssign
import kotlin.math.max
import kotlin.math.min
import soot.ArrayType
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.Modifier
import soot.PrimType
import soot.RefLikeType
import soot.RefType
import soot.Scene
import soot.ShortType
import soot.SootClass
import soot.SootField
import soot.SootMethod
import soot.SootMethodRef
import soot.Type
import soot.Value
import soot.VoidType
import soot.jimple.ArrayRef
import soot.jimple.BinopExpr
import soot.jimple.ClassConstant
import soot.jimple.Constant
import soot.jimple.DefinitionStmt
import soot.jimple.DoubleConstant
import soot.jimple.Expr
import soot.jimple.FieldRef
import soot.jimple.FloatConstant
import soot.jimple.IdentityRef
import soot.jimple.IntConstant
import soot.jimple.InvokeExpr
import soot.jimple.LongConstant
import soot.jimple.MonitorStmt
import soot.jimple.NeExpr
import soot.jimple.NullConstant
import soot.jimple.ParameterRef
import soot.jimple.ReturnStmt
import soot.jimple.StaticFieldRef
import soot.jimple.Stmt
import soot.jimple.StringConstant
import soot.jimple.SwitchStmt
import soot.jimple.ThisRef
import soot.jimple.internal.JAddExpr
import soot.jimple.internal.JArrayRef
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JBreakpointStmt
import soot.jimple.internal.JCastExpr
import soot.jimple.internal.JCaughtExceptionRef
import soot.jimple.internal.JDivExpr
import soot.jimple.internal.JDynamicInvokeExpr
import soot.jimple.internal.JEqExpr
import soot.jimple.internal.JGeExpr
import soot.jimple.internal.JGotoStmt
import soot.jimple.internal.JGtExpr
import soot.jimple.internal.JIdentityStmt
import soot.jimple.internal.JIfStmt
import soot.jimple.internal.JInstanceFieldRef
import soot.jimple.internal.JInstanceOfExpr
import soot.jimple.internal.JInterfaceInvokeExpr
import soot.jimple.internal.JInvokeStmt
import soot.jimple.internal.JLeExpr
import soot.jimple.internal.JLengthExpr
import soot.jimple.internal.JLookupSwitchStmt
import soot.jimple.internal.JLtExpr
import soot.jimple.internal.JMulExpr
import soot.jimple.internal.JNeExpr
import soot.jimple.internal.JNegExpr
import soot.jimple.internal.JNewArrayExpr
import soot.jimple.internal.JNewExpr
import soot.jimple.internal.JNewMultiArrayExpr
import soot.jimple.internal.JNopStmt
import soot.jimple.internal.JRemExpr
import soot.jimple.internal.JRetStmt
import soot.jimple.internal.JReturnStmt
import soot.jimple.internal.JReturnVoidStmt
import soot.jimple.internal.JSpecialInvokeExpr
import soot.jimple.internal.JStaticInvokeExpr
import soot.jimple.internal.JSubExpr
import soot.jimple.internal.JTableSwitchStmt
import soot.jimple.internal.JThrowStmt
import soot.jimple.internal.JVirtualInvokeExpr
import soot.jimple.internal.JimpleLocal
import soot.toolkits.graph.ExceptionalUnitGraph
import java.lang.reflect.GenericArrayType
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.javaType

private val CAUGHT_EXCEPTION = LocalVariable("@caughtexception")

class Traverser(
    private val methodUnderTest: ExecutableId,
    internal val typeRegistry: TypeRegistry,
    internal val hierarchy: Hierarchy,
    // TODO HACK violation of encapsulation
    internal val typeResolver: TypeResolver,
    private val globalGraph: InterProceduralUnitGraph,
    private val mocker: Mocker,
) : UtContextInitializer() {

    private val visitedStmts: MutableSet<Stmt> = mutableSetOf()

    private val classLoader: ClassLoader
        get() = utContext.classLoader

    // TODO: move this and other mutable fields to [TraversalContext]
    lateinit var environment: Environment
    private val solver: UtSolver
        get() = environment.state.solver

    // TODO HACK violation of encapsulation
    val memory: Memory
        get() = environment.state.memory

    private val localVariableMemory: LocalVariableMemory
        get() = environment.state.localVariableMemory

    //HACK (long strings)
    internal var softMaxArraySize = 40

    /**
     * Contains information about the generic types used in the parameters of the method under test.
     */
    private val parameterAddrToGenericType = mutableMapOf<UtAddrExpression, ParameterizedType>()

    private val preferredCexInstanceCache = mutableMapOf<ObjectValue, MutableSet<SootField>>()

    private var queuedSymbolicStateUpdates = SymbolicStateUpdate()

    private val objectCounter = AtomicInteger(TypeRegistry.objectCounterInitialValue)
    private fun findNewAddr(insideStaticInitializer: Boolean): UtAddrExpression {
        val newAddr = objectCounter.getAndIncrement()
        // return negative address for objects created inside static initializer
        // to make their address space be intersected with address space of
        // parameters of method under symbolic execution
        // @see ObjectWithFinalStaticTest::testParameterEqualsFinalStatic
        val signedAddr = if (insideStaticInitializer) -newAddr else newAddr
        return UtAddrExpression(signedAddr)
    }
    internal fun findNewAddr() = findNewAddr(environment.state.isInsideStaticInitializer).also { touchAddress(it) }

    // Counter used for a creation symbolic results of "hashcode" and "equals" methods.
    private var equalsCounter = 0
    private var hashcodeCounter = 0

    // A counter for objects created as native method call result.
    private var unboundedConstCounter = 0

    fun traverse(state: ExecutionState): Collection<ExecutionState> {
        val context = TraversalContext()

        val currentStmt = state.stmt
        environment = Environment(globalGraph.method(state.stmt), state)


        if (currentStmt !in visitedStmts) {
            environment.state.updateIsVisitedNew()
            visitedStmts += currentStmt
        }

        environment.state.lastEdge?.let {
            globalGraph.visitEdge(it)
        }

        try {
            val exception = environment.state.exception
            if (exception != null) {
                context.traverseException(currentStmt, exception)
            } else {
                context.traverseStmt(currentStmt)
            }
        } catch (ex: Throwable) {
            environment.state.close()

            logger.error(ex) { "Test generation failed on stmt $currentStmt, symbolic stack trace:\n$symbolicStackTrace" }
            // TODO: enrich with nice description for known issues
            throw ex
        }
        queuedSymbolicStateUpdates = SymbolicStateUpdate()
        return context.nextStates
    }

    private fun TraversalContext.traverseStmt(current: Stmt) {
        if (doPreparatoryWorkIfRequired(current)) return

        when (current) {
            is JAssignStmt -> traverseAssignStmt(current)
            is JIdentityStmt -> traverseIdentityStmt(current)
            is JIfStmt -> traverseIfStmt(current)
            is JInvokeStmt -> traverseInvokeStmt(current)
            is SwitchStmt -> traverseSwitchStmt(current)
            is JReturnStmt -> processResult(symbolicSuccess(current))
            is JReturnVoidStmt -> processResult(SymbolicSuccess(voidValue))
            is JRetStmt -> error("This one should be already removed by Soot: $current")
            is JThrowStmt -> traverseThrowStmt(current)
            is JBreakpointStmt -> offerState(environment.state.updateQueued(globalGraph.succ(current)))
            is JGotoStmt -> offerState(environment.state.updateQueued(globalGraph.succ(current)))
            is JNopStmt -> offerState(environment.state.updateQueued(globalGraph.succ(current)))
            is MonitorStmt -> offerState(environment.state.updateQueued(globalGraph.succ(current)))
            is DefinitionStmt -> TODO("$current")
            else -> error("Unsupported: ${current::class}")
        }
    }

    /**
     * Handles preparatory work for static initializers and multi-dimensional arrays creation.
     *
     * For instance, it could push handmade graph with preparation statements to the path selector.
     *
     * Returns:
     * - True if work is required and the constructed graph was pushed. In this case current
     *   traverse stops and continues after the graph processing;
     * - False if preparatory work is not required or it is already done.
     * environment.state.methodResult can contain the work result.
     */
    private fun TraversalContext.doPreparatoryWorkIfRequired(current: Stmt): Boolean {
        if (current !is JAssignStmt) return false

        return when {
            processStaticInitializerIfRequired(current) -> true
            unfoldMultiArrayExprIfRequired(current) -> true
            else -> false
        }
    }

    /**
     * Handles preparatory work for static initializers. To do it, this method checks if any parts of the given
     * statement is StaticRefField and the class this field belongs to hasn't been initialized yet.
     * If so, it pushes a graph of the corresponding `<clinit>` to the path selector.
     *
     * Returns:
     * - True if the work is required and the graph was pushed. In this case current
     *   traversal stops and continues after the graph processing;
     * - False if preparatory work is not required or it is already done. In this case a result from the
     * environment.state.methodResult already processed and applied.
     *
     * Note: similar but more granular approach used if Engine decides to process static field concretely.
     */
    private fun TraversalContext.processStaticInitializerIfRequired(stmt: JAssignStmt): Boolean {
        val right = stmt.rightOp
        val left = stmt.leftOp
        val method = environment.method
        val declaringClass = method.declaringClass
        val result = listOf(right, left)
            .filterIsInstance<StaticFieldRef>()
            .filterNot { insideStaticInitializer(it, method, declaringClass) }
            .firstOrNull { processStaticInitializer(it, stmt) }

        return result != null
    }

    /**
     * Handles preparatory work for multi-dimensional arrays. Constructs unfolded representation for
     * JNewMultiArrayExpr in the [unfoldMultiArrayExpr].
     *
     * Returns:
     * - True if right part of the JAssignStmt contains JNewMultiArrayExpr and there is no calculated result in the
     * environment.state.methodResult.
     * - False otherwise
     */
    private fun TraversalContext.unfoldMultiArrayExprIfRequired(stmt: JAssignStmt): Boolean {
        // We have already unfolded the statement and processed constructed graph, have the calculated result
        if (environment.state.methodResult != null) return false

        val right = stmt.rightOp
        if (right !is JNewMultiArrayExpr) return false

        val graph = unfoldMultiArrayExpr(stmt)
        val resolvedSizes = right.sizes.map { (resolve(it, IntType.v()) as PrimitiveValue).align() }

        negativeArraySizeCheck(*resolvedSizes.toTypedArray())

        pushToPathSelector(graph, caller = null, resolvedSizes)
        return true
    }

    /**
     * Processes static initialization for class.
     *
     * If class is not initialized yet, creates graph for that and pushes to the path selector;
     * otherwise class is initialized and environment.state.methodResult can contain initialization result.
     *
     * If contains, adds state with the last edge to the path selector;
     * if doesn't contain, it's already processed few steps before, nothing to do.
     *
     * Returns true if processing takes place and Engine should end traversal of current statement.
     */
    private fun TraversalContext.processStaticInitializer(
        fieldRef: StaticFieldRef,
        stmt: Stmt
    ): Boolean {
        if (shouldProcessStaticFieldConcretely(fieldRef)) {
            return processStaticFieldConcretely(fieldRef, stmt)
        }

        val field = fieldRef.field
        val declaringClass = field.declaringClass
        val declaringClassId = declaringClass.id
        val methodResult = environment.state.methodResult
        if (!memory.isInitialized(declaringClassId) &&
            !isStaticInstanceInMethodResult(declaringClassId, methodResult)
        ) {
            val initializer = declaringClass.staticInitializerOrNull()
            return if (initializer == null) {
                false
            } else {
                val graph = classInitGraph(initializer)
                pushToPathSelector(graph, null, emptyList())
                true
            }
        }

        val result = methodResult ?: return false

        when (result.symbolicResult) {
            // This branch could be useful if we have a static field, i.e. x = 5 / 0
            is SymbolicFailure -> traverseException(stmt, result.symbolicResult)
            is SymbolicSuccess -> offerState(
                environment.state.updateQueued(
                    environment.state.lastEdge!!,
                    result.symbolicStateUpdate
                )
            )
        }
        return true
    }

    /**
     * Decides should we read this static field concretely or not.
     */
    private fun shouldProcessStaticFieldConcretely(fieldRef: StaticFieldRef): Boolean {
        workaround(HACK) {
            val className = fieldRef.field.declaringClass.name

            // We should process clinit sections for classes from these packages.
            // Note that this list is not exhaustive, so it may be supplemented in the future.
            val packagesToProcessConcretely = javaPackagesToProcessConcretely + sunPackagesToProcessConcretely

            val declaringClass = fieldRef.field.declaringClass

            val isFromPackageToProcessConcretely = packagesToProcessConcretely.any { className.startsWith(it) }
                    // it is required to remove classes we override, since
                    // we could accidentally initialize their final fields
                    // with values that will later affect our overridden classes
                    && fieldRef.field.declaringClass.type !in classToWrapper.keys
                    // because of the same reason we should not use
                    // concrete information from clinit sections for enums
                    && !fieldRef.field.declaringClass.isEnum
                    //hardcoded string for class name is used cause class is not public
                    //this is a hack to avoid crashing on code with Math.random()
                    && !className.endsWith("RandomNumberGeneratorHolder")

            // we can process concretely only enums that does not affect the external system
            val isEnumNotAffectingExternalStatics = declaringClass.let {
                it.isEnum && !it.isEnumAffectingExternalStatics(typeResolver)
            }

            return isEnumNotAffectingExternalStatics || isFromPackageToProcessConcretely
        }
    }

    private val javaPackagesToProcessConcretely = listOf(
        "applet", "awt", "beans", "io", "lang", "math", "net",
        "nio", "rmi", "security", "sql", "text", "time", "util"
    ).map { "java.$it" }

    private val sunPackagesToProcessConcretely = listOf(
        "applet", "audio", "awt", "corba", "font", "instrument",
        "invoke", "io", "java2d", "launcher", "management", "misc",
        "net", "nio", "print", "reflect", "rmi", "security",
        "swing", "text", "tools.jar", "tracing", "util"
    ).map { "sun.$it" }

    /**
     * Checks if field was processed (read) already.
     * Otherwise offers to path selector the same statement, but with memory and constraints updates for this field.
     *
     * Returns true if processing takes place and Engine should end traversal of current statement.
     */
    private fun TraversalContext.processStaticFieldConcretely(fieldRef: StaticFieldRef, stmt: Stmt): Boolean {
        val field = fieldRef.field
        val fieldId = field.fieldId
        if (memory.isInitialized(fieldId)) {
            return false
        }

        // Gets concrete value, converts to symbolic value
        val declaringClass = field.declaringClass

        val updates = if (declaringClass.isEnum) {
            makeConcreteUpdatesForEnumsWithStmt(fieldId, declaringClass, stmt)
        } else {
            makeConcreteUpdatesForNonEnumStaticField(field, fieldId, declaringClass, stmt)
        }

        // a static initializer can be the first statement in method so there will be no last edge
        // for example, as it is during Enum::values method analysis:
        // public static ClassWithEnum$StatusEnum[] values()
        // {
        //      ClassWithEnum$StatusEnum[] $r0, $r2;
        //      java.lang.Object $r1;

        //      $r0 = <ClassWithEnum$StatusEnum: ClassWithEnum$StatusEnum[] $VALUES>;
        val edge = environment.state.lastEdge ?: globalGraph.succ(stmt)

        val newState = environment.state.updateQueued(edge, updates)
        offerState(newState)

        return true
    }

    private fun makeConcreteUpdatesForEnum(
        type: RefType,
        fieldId: FieldId? = null
    ): Pair<SymbolicStateUpdate, SymbolicValue?> {
        val jClass = type.id.jClass

        // symbolic value for enum class itself
        val enumClassValue = findOrCreateStaticObject(type)

        // values for enum constants
        val enumConstantConcreteValues = jClass.enumConstants.filterIsInstance<Enum<*>>()

        val (enumConstantSymbolicValues, enumConstantSymbolicResultsByName) =
            makeSymbolicValuesFromEnumConcreteValues(type, enumConstantConcreteValues)

        val enumFields = typeResolver.findFields(type)

        val sootFieldsWithRuntimeValues =
            associateEnumSootFieldsWithConcreteValues(enumFields, enumConstantConcreteValues)

        val (staticFields, nonStaticFields) = sootFieldsWithRuntimeValues.partition { it.first.isStatic }

        val (staticFieldUpdates, curFieldSymbolicValueForLocalVariable) = makeEnumStaticFieldsUpdates(
            staticFields,
            type.sootClass,
            enumConstantSymbolicResultsByName,
            enumConstantSymbolicValues,
            enumClassValue,
            fieldId
        )

        val nonStaticFieldsUpdates = makeEnumNonStaticFieldsUpdates(enumConstantSymbolicValues, nonStaticFields)

        // we do not mark static fields for enum constants and $VALUES as meaningful
        // because we should not set them in generated code
        val meaningfulStaticFields = staticFields.filterNot {
            val name = it.first.name

            name in enumConstantSymbolicResultsByName.keys || isEnumValuesFieldName(name)
        }

        val initializedStaticFieldsMemoryUpdate = MemoryUpdate(
            initializedStaticFields = staticFields.map { it.first.fieldId }.toPersistentSet(),
            meaningfulStaticFields = meaningfulStaticFields.map { it.first.fieldId }.toPersistentSet(),
            symbolicEnumValues = enumConstantSymbolicValues.toPersistentList()
        )

        return Pair(
            staticFieldUpdates + nonStaticFieldsUpdates + initializedStaticFieldsMemoryUpdate,
            curFieldSymbolicValueForLocalVariable
        )
    }

    @Suppress("UnnecessaryVariable")
    private fun makeConcreteUpdatesForEnumsWithStmt(
        fieldId: FieldId,
        declaringClass: SootClass,
        stmt: Stmt
    ): SymbolicStateUpdate {
        val (enumUpdates, curFieldSymbolicValueForLocalVariable) =
            makeConcreteUpdatesForEnum(declaringClass.type, fieldId)
        val allUpdates = enumUpdates + createConcreteLocalValueUpdate(stmt, curFieldSymbolicValueForLocalVariable)
        return allUpdates
    }

    @Suppress("UnnecessaryVariable")
    private fun makeConcreteUpdatesForNonEnumStaticField(
        field: SootField,
        fieldId: FieldId,
        declaringClass: SootClass,
        stmt: Stmt
    ): SymbolicStateUpdate {
        val concreteValue = extractConcreteValue(field)
        val (symbolicResult, symbolicStateUpdate) = toMethodResult(concreteValue, field.type)
        val symbolicValue = (symbolicResult as SymbolicSuccess).value

        // Collects memory updates
        val initializedFieldUpdate =
            MemoryUpdate(initializedStaticFields = persistentHashSetOf(fieldId))

        val objectUpdate = objectUpdate(
            instance = findOrCreateStaticObject(declaringClass.type),
            field = field,
            value = valueToExpression(symbolicValue, field.type)
        )
        val allUpdates = symbolicStateUpdate +
                initializedFieldUpdate +
                objectUpdate +
                createConcreteLocalValueUpdate(stmt, symbolicValue)

        return allUpdates
    }

    /**
     * Creates a local update consisting [symbolicValue] for a local variable from [stmt] in case [stmt] is [JAssignStmt].
     */
    private fun createConcreteLocalValueUpdate(
        stmt: Stmt,
        symbolicValue: SymbolicValue?,
    ): LocalMemoryUpdate {
        // we need to make locals update if it is an assignment statement
        // for enums we have only two types for assignment with enums — enum constant or $VALUES field
        // for example, a jimple body for Enum::values method starts with the following lines:
        //  public static ClassWithEnum$StatusEnum[] values()
        //  {
        //      ClassWithEnum$StatusEnum[] $r0, $r2;
        //      java.lang.Object $r1;
        //      $r0 = <ClassWithEnum$StatusEnum: ClassWithEnum$StatusEnum[] $VALUES>;
        //      $r1 = virtualinvoke $r0.<java.lang.Object: java.lang.Object clone()>();

        // so, we have to make an update for the local $r0

        return if (stmt is JAssignStmt && stmt.leftOp is JimpleLocal) {
            val local = stmt.leftOp as JimpleLocal

            localMemoryUpdate(local.variable to symbolicValue)
        } else {
            LocalMemoryUpdate()
        }
    }

    // Some fields are inaccessible with reflection, so we have to instantiate it by ourselves.
    // Otherwise, extract it from the class.
    // TODO JIRA:1593
    private fun extractConcreteValue(field: SootField): Any? =
        when (field.signature) {
            SECURITY_FIELD_SIGNATURE -> SecurityManager()
            // todo change to class loading
            //FIELD_FILTER_MAP_FIELD_SIGNATURE -> mapOf(Reflection::class to arrayOf("fieldFilterMap", "methodFilterMap"))
            METHOD_FILTER_MAP_FIELD_SIGNATURE -> emptyMap<Class<*>, Array<String>>()
            else -> {
                val fieldId = field.fieldId
                val jField = fieldId.jField
                jField.let {
                    it.withAccessibility {
                        it.get(null)
                    }
                }
            }
        }

    private fun isStaticInstanceInMethodResult(id: ClassId, methodResult: MethodResult?) =
        methodResult != null && id in methodResult.memoryUpdates.staticInstanceStorage

    private fun TraversalContext.skipVerticesForThrowableCreation(current: JAssignStmt) {
        val rightType = current.rightOp.type as RefType
        val exceptionType = Scene.v().getSootClass(rightType.className).type
        val createdException = createObject(findNewAddr(), exceptionType, true)
        val currentExceptionJimpleLocal = current.leftOp as JimpleLocal

        queuedSymbolicStateUpdates += localMemoryUpdate(currentExceptionJimpleLocal.variable to createdException)

        // mark the rest of the path leading to the '<init>' statement as covered
        do {
            environment.state = environment.state.updateQueued(globalGraph.succ(environment.state.stmt))
            globalGraph.visitEdge(environment.state.lastEdge!!)
            globalGraph.visitNode(environment.state)
        } while (!environment.state.stmt.isConstructorCall(currentExceptionJimpleLocal))

        offerState(environment.state.updateQueued(globalGraph.succ(environment.state.stmt)))
    }

    private fun TraversalContext.traverseAssignStmt(current: JAssignStmt) {
        val rightValue = current.rightOp

        workaround(HACK) {
            val rightType = rightValue.type
            if (rightValue is JNewExpr && rightType is RefType) {
                val throwableType = Scene.v().getSootClass("java.lang.Throwable").type
                val throwableInheritors = typeResolver.findOrConstructInheritorsIncludingTypes(throwableType)

                // skip all the vertices in the CFG between `new` and `<init>` statements
                if (rightType in throwableInheritors) {
                    skipVerticesForThrowableCreation(current)
                    return
                }
            }
        }

        val rightPartWrappedAsMethodResults = if (rightValue is InvokeExpr) {
            invokeResult(rightValue)
        } else {
            val value = resolve(rightValue, current.leftOp.type)
            listOf(MethodResult(value))
        }

        rightPartWrappedAsMethodResults.forEach { methodResult ->
            when (methodResult.symbolicResult) {

                is SymbolicFailure -> { //exception thrown
                    if (environment.state.executionStack.last().doesntThrow) return@forEach

                    val nextState = environment.state.createExceptionState(
                        methodResult.symbolicResult,
                        queuedSymbolicStateUpdates + methodResult.symbolicStateUpdate
                    )
                    globalGraph.registerImplicitEdge(nextState.lastEdge!!)
                    offerState(nextState)
                }

                is SymbolicSuccess -> {
                    val update = traverseAssignLeftPart(
                        current.leftOp,
                        methodResult.symbolicResult.value
                    )
                    offerState(
                        environment.state.updateQueued(
                            globalGraph.succ(current),
                            update + methodResult.symbolicStateUpdate
                        )
                    )
                }
            }
        }
    }

    /**
     * This hack solves the problem with static final fields, which are equal by reference with parameter.
     *
     * Let be the address of a parameter and correspondingly the address of final field be p0.
     * The initial state of a chunk array for this static field is always (mkArray Class_field Int -> Int)
     * And the current state of this chunk array is (store (mkArray Class_field Int -> Int) (p0: someValue))
     * At initial chunk array under address p0 can be placed any value, because during symbolic execution we
     * always refer only to current state.
     *
     * At the resolving stage, to resolve model of parameter before invoke, we get it from initial chunk array
     * by address p0, where can be placed any value. However, resolved model for parameter after execution will
     * be correct, as current state has correct value in chunk array under p0 address.
     */
    private fun addConstraintsForFinalAssign(left: SymbolicValue, value: SymbolicValue) {
        if (left is PrimitiveValue) {
            if (left.type is DoubleType) {
                queuedSymbolicStateUpdates += mkOr(
                    Eq(left, value as PrimitiveValue),
                    Ne(left, left),
                    Ne(value, value)
                ).asHardConstraint()
            } else {
                queuedSymbolicStateUpdates += Eq(left, value as PrimitiveValue).asHardConstraint()
            }
        } else if (left is ReferenceValue) {
            queuedSymbolicStateUpdates += addrEq(left.addr, (value as ReferenceValue).addr).asHardConstraint()
        }
    }

    /**
     * Check for [ArrayStoreException] when an array element is assigned
     *
     * @param arrayInstance Symbolic value corresponding to the array being updated
     * @param value Symbolic value corresponding to the right side of the assignment
     */
    private fun TraversalContext.arrayStoreExceptionCheck(arrayInstance: SymbolicValue, value: SymbolicValue) {
        require(arrayInstance is ArrayValue)
        val valueType = value.type
        val valueBaseType = valueType.baseType
        val arrayElementType = arrayInstance.type.elementType

        // We should check for [ArrayStoreException] only for reference types.
        // * For arrays of primitive types, incorrect assignment is prevented as compile time.
        // * When assigning primitive literals (e.g., `1`) to arrays of corresponding boxed classes (`Integer`),
        //   the conversion to the reference type is automatic.
        // * [System.arraycopy] and similar functions that can throw [ArrayStoreException] accept [Object] arrays
        //   as arguments, so array elements are references.

        if (valueBaseType is RefType) {
            val arrayElementTypeStorage = typeResolver.constructTypeStorage(arrayElementType, useConcreteType = false)

            // Generate ASE only if [value] is not a subtype of the type of array elements
            val isExpression = typeRegistry.typeConstraint(value.addr, arrayElementTypeStorage).isConstraint()
            val notIsExpression = mkNot(isExpression)

            // `null` is compatible with any reference type, so we should not throw ASE when `null` is assigned
            val nullEqualityConstraint = addrEq(value.addr, nullObjectAddr)
            val notNull = mkNot(nullEqualityConstraint)

            // Currently the negation of [UtIsExpression] seems to work incorrectly for [java.lang.Object]:
            // https://github.com/UnitTestBot/UTBotJava/issues/1007

            // It is related to [org.utbot.engine.pc.Z3TranslatorVisitor.filterInappropriateTypes] that removes
            // internal engine classes for [java.lang.Object] type storage, and this logic is not fully
            // consistent with the negation.

            // Here we have a specific test for [java.lang.Object] as the type of array elements:
            // any reference type may be assigned to elements of an Object array, so we should not generate
            // [ArrayStoreException] in these cases.

            // TODO: remove enclosing `if` when [UtIfExpression] negation is fixed
            if (!arrayElementType.isJavaLangObject()) {
                implicitlyThrowException(ArrayStoreException(), setOf(notIsExpression, notNull))
            }

            // If ASE is not thrown, we know that either the value is null, or it has a compatible type
            queuedSymbolicStateUpdates += mkOr(isExpression, nullEqualityConstraint).asHardConstraint()
        }
    }

    /**
     * Traverses left part of assignment i.e. where to store resolved value.
     */
    private fun TraversalContext.traverseAssignLeftPart(left: Value, value: SymbolicValue): SymbolicStateUpdate = when (left) {
        is ArrayRef -> {
            val arrayInstance = resolve(left.base) as ArrayValue
            val addr = arrayInstance.addr
            nullPointerExceptionCheck(addr)

            val index = (resolve(left.index) as PrimitiveValue).align()
            val length = memory.findArrayLength(addr)
            indexOutOfBoundsChecks(index, length)

            queuedSymbolicStateUpdates += Le(length, softMaxArraySize).asHardConstraint() // TODO: fix big array length

            arrayStoreExceptionCheck(arrayInstance, value)

            // add constraint for possible array type
            val valueType = value.type
            val valueBaseType = valueType.baseType
            if (valueBaseType is RefType) {
                val valueTypeAncestors = typeResolver.findOrConstructAncestorsIncludingTypes(valueBaseType)
                val valuePossibleBaseTypes = value.typeStorage.possibleConcreteTypes.map { it.baseType }
                // Either one of the possible types or one of their ancestor (to add interfaces and abstract classes)
                val arrayPossibleBaseTypes = valueTypeAncestors + valuePossibleBaseTypes

                val arrayPossibleTypes = arrayPossibleBaseTypes.map {
                    it.makeArrayType(arrayInstance.type.numDimensions)
                }
                val typeStorage = typeResolver.constructTypeStorage(OBJECT_TYPE, arrayPossibleTypes)

                queuedSymbolicStateUpdates += typeRegistry.typeConstraint(arrayInstance.addr, typeStorage)
                    .isConstraint().asHardConstraint()
            }

            val elementType = arrayInstance.type.elementType
            val valueExpression = valueToExpression(value, elementType)
            SymbolicStateUpdate(memoryUpdates = arrayUpdate(arrayInstance, index, valueExpression))
        }
        is FieldRef -> {
            val instanceForField = resolveInstanceForField(left)

            val objectUpdate = objectUpdate(
                instance = instanceForField,
                field = left.field,
                value = valueToExpression(value, left.field.type)
            )

            // This hack solves the problem with static final fields, which are equal by reference with parameter
            workaround(HACK) {
                if (left.field.isFinal) {
                    addConstraintsForFinalAssign(resolve(left), value)
                }
            }

            if (left is StaticFieldRef) {
                val fieldId = left.field.fieldId
                val staticFieldMemoryUpdate = StaticFieldMemoryUpdateInfo(fieldId, value)
                val touchedStaticFields = persistentListOf(staticFieldMemoryUpdate)
                queuedSymbolicStateUpdates += MemoryUpdate(staticFieldsUpdates = touchedStaticFields)
                if (!environment.method.isStaticInitializer && isStaticFieldMeaningful(left.field)) {
                    queuedSymbolicStateUpdates += MemoryUpdate(meaningfulStaticFields = persistentSetOf(fieldId))
                }
            }

            SymbolicStateUpdate(memoryUpdates = objectUpdate)
        }
        is JimpleLocal -> SymbolicStateUpdate(localMemoryUpdates = localMemoryUpdate(left.variable to value))
        is InvokeExpr -> TODO("Not implemented: $left")
        else -> error("${left::class} is not implemented")
    }

    /**
     * Resolves instance for field. For static field it's a special object represents static fields of particular class.
     */
    private fun TraversalContext.resolveInstanceForField(fieldRef: FieldRef) = when (fieldRef) {
        is JInstanceFieldRef -> {
            // Runs resolve() to check possible NPE and create required arrays related to the field.
            // Ignores the result of resolve().
            resolve(fieldRef)
            val baseObject = resolve(fieldRef.base) as ObjectValue
            val typeStorage = TypeStorage(fieldRef.field.declaringClass.type)
            baseObject.copy(typeStorage = typeStorage)
        }
        is StaticFieldRef -> {
            val declaringClassType = fieldRef.field.declaringClass.type
            val fieldTypeId = fieldRef.field.type.classId
            val generator = UtMockInfoGenerator { mockAddr ->
                val fieldId = FieldId(declaringClassType.id, fieldRef.field.name)
                UtFieldMockInfo(fieldTypeId, mockAddr, fieldId, ownerAddr = null)
            }
            findOrCreateStaticObject(declaringClassType, generator)
        }
        else -> error("Unreachable branch")
    }

    /**
     * Converts value to expression with cast to target type for primitives.
     */
    fun valueToExpression(value: SymbolicValue, type: Type): UtExpression = when (value) {
        is ReferenceValue -> value.addr
        // TODO: shall we add additional constraint that aligned expression still equals original?
        // BitVector can lose valuable bites during extraction
        is PrimitiveValue -> UtCastExpression(value, type)
    }

    private fun TraversalContext.traverseIdentityStmt(current: JIdentityStmt) {
        val localVariable = (current.leftOp as? JimpleLocal)?.variable ?: error("Unknown op: ${current.leftOp}")
        when (val identityRef = current.rightOp as IdentityRef) {
            is ParameterRef, is ThisRef -> {
                // Nested method calls already have input arguments in state
                val value = if (environment.state.inputArguments.isNotEmpty()) {
                    environment.state.inputArguments.removeFirst().let {
                        // implicit cast, if we pass to function with
                        // int parameter a value with e.g. byte type
                        if (it is PrimitiveValue && it.type != identityRef.type) {
                            it.cast(identityRef.type)
                        } else {
                            it
                        }
                    }
                } else {
                    val suffix = if (identityRef is ParameterRef) "${identityRef.index}" else "_this"
                    val pName = "p$suffix"
                    val mockInfoGenerator = parameterMockInfoGenerator(identityRef)

                    val isNonNullable = if (identityRef is ParameterRef) {
                        environment.method.paramHasNotNullAnnotation(identityRef.index)
                    } else {
                        true // "this" must be not null
                    }

                    val createdValue = identityRef.createConst(pName, mockInfoGenerator)

                    if (createdValue is ReferenceValue) {
                        // Update generic type info for method under test' parameters
                        updateGenericTypeInfo(identityRef, createdValue)

                        if (isNonNullable) {
                            queuedSymbolicStateUpdates += mkNot(
                                addrEq(
                                    createdValue.addr,
                                    nullObjectAddr
                                )
                            ).asHardConstraint()
                        }
                    }
                    if (preferredCexOption) {
                        applyPreferredConstraints(createdValue)
                    }
                    createdValue
                }

                environment.state.parameters += Parameter(localVariable, identityRef.type, value)

                val nextState = environment.state.updateQueued(
                    globalGraph.succ(current),
                    SymbolicStateUpdate(localMemoryUpdates = localMemoryUpdate(localVariable to value))
                )
                offerState(nextState)
            }
            is JCaughtExceptionRef -> {
                val value = localVariableMemory.local(CAUGHT_EXCEPTION)
                    ?: error("Exception wasn't caught, stmt: $current, line: ${current.lines}")
                val nextState = environment.state.updateQueued(
                    globalGraph.succ(current),
                    SymbolicStateUpdate(localMemoryUpdates = localMemoryUpdate(localVariable to value, CAUGHT_EXCEPTION to null))
                )
                offerState(nextState)
            }
            else -> error("Unsupported $identityRef")
        }
    }

    /**
     * Creates mock info for method under test' non-primitive parameter.
     *
     * Returns null if mock is not allowed - Engine traverses nested method call or parameter type is not RefType.
     */
    private fun parameterMockInfoGenerator(parameterRef: IdentityRef): UtMockInfoGenerator? {
        if (environment.state.isInNestedMethod()) return null
        if (parameterRef !is ParameterRef) return null
        val type = parameterRef.type
        if (type !is RefType) return null
        return UtMockInfoGenerator { mockAddr -> UtObjectMockInfo(type.id, mockAddr) }
    }

    /**
     * Stores information about the generic types used in the parameters of the method under test.
     */
    private fun updateGenericTypeInfo(identityRef: IdentityRef, value: ReferenceValue) {
        val callable = methodUnderTest.executable
        val kCallable = ::updateGenericTypeInfo
        val test = kCallable.instanceParameter?.type?.javaType
        val type = if (identityRef is ThisRef) {
            // TODO: for ThisRef both methods don't return parameterized type
            if (methodUnderTest.isConstructor) {
                callable.annotatedReturnType?.type
            } else {
                callable.declaringClass // same as it was, but it isn't parametrized type
                    ?: error("No instanceParameter for ${callable} found")
            }
        } else {
            // Sometimes out of bound exception occurred here, e.g., com.alibaba.fescar.core.model.GlobalStatus.<init>
            workaround(HACK) {
                val index = (identityRef as ParameterRef).index
                val valueParameters = callable.genericParameterTypes

                if (index > valueParameters.lastIndex) return
                valueParameters[index]
            }
        }

        if (type is ParameterizedType) {
            val typeStorages = type.actualTypeArguments.map { actualTypeArgument ->
                when (actualTypeArgument) {
                    is WildcardType -> {
                        val upperBounds = actualTypeArgument.upperBounds
                        val lowerBounds = actualTypeArgument.lowerBounds
                        val allTypes = upperBounds + lowerBounds

                        if (allTypes.any { it is GenericArrayType }) {
                            val errorTypes = allTypes.filterIsInstance<GenericArrayType>()
                            TODO("we do not support GenericArrayTypeImpl yet, and $errorTypes found. SAT-1446")
                        }

                        val upperBoundsTypes = typeResolver.intersectInheritors(upperBounds)
                        val lowerBoundsTypes = typeResolver.intersectAncestors(lowerBounds)

                        typeResolver.constructTypeStorage(OBJECT_TYPE, upperBoundsTypes.intersect(lowerBoundsTypes))
                    }
                    is TypeVariable<*> -> { // it is a type variable for the whole class, not the function type variable
                        val upperBounds = actualTypeArgument.bounds

                        if (upperBounds.any { it is GenericArrayType }) {
                            val errorTypes = upperBounds.filterIsInstance<GenericArrayType>()
                            TODO("we do not support GenericArrayType yet, and $errorTypes found. SAT-1446")
                        }

                        val upperBoundsTypes = typeResolver.intersectInheritors(upperBounds)

                        typeResolver.constructTypeStorage(OBJECT_TYPE, upperBoundsTypes)
                    }
                    is GenericArrayType -> {
                        // TODO bug with T[][], because there is no such time T JIRA:1446
                        typeResolver.constructTypeStorage(OBJECT_TYPE, useConcreteType = false)
                    }
                    is ParameterizedType, is Class<*> -> {
                        val sootType = Scene.v().getType(actualTypeArgument.rawType.typeName)

                        typeResolver.constructTypeStorage(sootType, useConcreteType = false)
                    }
                    else -> error("Unsupported argument type ${actualTypeArgument::class}")
                }
            }

            queuedSymbolicStateUpdates += typeRegistry.genericTypeParameterConstraint(value.addr, typeStorages).asHardConstraint()
            parameterAddrToGenericType += value.addr to type

            typeRegistry.saveObjectParameterTypeStorages(value.addr, typeStorages)
        }
    }

    private fun TraversalContext.traverseIfStmt(current: JIfStmt) {
        // positiveCaseEdge could be null - see Conditions::emptyBranches
        val (negativeCaseEdge, positiveCaseEdge) = globalGraph.succs(current).let { it[0] to it.getOrNull(1) }
        val cond = current.condition
        val resolvedCondition = resolveIfCondition(cond as BinopExpr)
        val positiveCasePathConstraint = resolvedCondition.condition
        val (positiveCaseSoftConstraint, negativeCaseSoftConstraint) = resolvedCondition.softConstraints
        val negativeCasePathConstraint = mkNot(positiveCasePathConstraint)

        if (positiveCaseEdge != null) {
            environment.state.definitelyFork()
        }

        /* assumeOrExecuteConcrete in jimple looks like:
            ``` z0 = a > 5
                if (z0 == 1) goto label1
                assumeOrExecuteConcretely(z0)

                label1:
                assumeOrExecuteConcretely(z0)
            ```

            We have to detect such situations to avoid addition `a > 5` into hardConstraints,
            because we want to add them into Assumptions.

            Note: we support only simple predicates right now (one logical operation),
            otherwise they will be added as hard constraints, and we will not execute
            the state concretely if there will be UNSAT because of assumptions.
         */
        val isAssumeExpr = positiveCaseEdge?.let { isConditionForAssumeOrExecuteConcretely(it.dst) } ?: false

        // in case of assume we want to have the only branch where $z = 1 (it is a negative case)
        if (!isAssumeExpr) {
            positiveCaseEdge?.let { edge ->
                environment.state.expectUndefined()
                val positiveCaseState = environment.state.updateQueued(
                    edge,
                    SymbolicStateUpdate(
                        hardConstraints = positiveCasePathConstraint.asHardConstraint(),
                        softConstraints = setOfNotNull(positiveCaseSoftConstraint).asSoftConstraint()
                    ) + resolvedCondition.symbolicStateUpdates.positiveCase
                )
                offerState(positiveCaseState)
            }
        }

        // Depending on existance of assumeExpr we have to add corresponding hardConstraints and assumptions
        val hardConstraints = if (!isAssumeExpr) negativeCasePathConstraint.asHardConstraint() else emptyHardConstraint()
        val assumption = if (isAssumeExpr) negativeCasePathConstraint.asAssumption() else emptyAssumption()

        val negativeCaseState = environment.state.updateQueued(
            negativeCaseEdge,
            SymbolicStateUpdate(
                hardConstraints = hardConstraints,
                softConstraints = setOfNotNull(negativeCaseSoftConstraint).asSoftConstraint(),
                assumptions = assumption
            ) + resolvedCondition.symbolicStateUpdates.negativeCase
        )
        offerState(negativeCaseState)
    }

    /**
     * Returns true if the next stmt is an [assumeOrExecuteConcretelyMethod] invocation, false otherwise.
     */
    private fun isConditionForAssumeOrExecuteConcretely(stmt: Stmt): Boolean {
        val successor = globalGraph.succStmts(stmt).singleOrNull() as? JInvokeStmt ?: return false
        val invokeExpression = successor.invokeExpr as? JStaticInvokeExpr ?: return false
        return invokeExpression.method.isUtMockAssumeOrExecuteConcretely
    }

    private fun TraversalContext.traverseInvokeStmt(current: JInvokeStmt) {
        val results = invokeResult(current.invokeExpr)

        results.forEach { result ->
            if (result.symbolicResult is SymbolicFailure && environment.state.executionStack.last().doesntThrow) {
                return@forEach
            }

            offerState(
                when (result.symbolicResult) {
                    is SymbolicFailure -> environment.state.createExceptionState(
                        result.symbolicResult,
                        queuedSymbolicStateUpdates + result.symbolicStateUpdate
                    )
                    is SymbolicSuccess -> environment.state.updateQueued(
                        globalGraph.succ(current),
                        result.symbolicStateUpdate
                    )
                }
            )
        }
    }

    private fun TraversalContext.traverseSwitchStmt(current: SwitchStmt) {
        val valueExpr = resolve(current.key) as PrimitiveValue
        val successors = when (current) {
            is JTableSwitchStmt -> {
                val indexed = (current.lowIndex..current.highIndex).mapIndexed { i, index ->
                    Edge(current, current.getTarget(i) as Stmt, i) to Eq(valueExpr, index)
                }
                val targetExpr = mkOr(
                    Lt(valueExpr, current.lowIndex),
                    Gt(valueExpr, current.highIndex)
                )
                indexed + (Edge(current, current.defaultTarget as Stmt, indexed.size) to targetExpr)
            }
            is JLookupSwitchStmt -> {
                val lookups = current.lookupValues.mapIndexed { i, value ->
                    Edge(current, current.getTarget(i) as Stmt, i) to Eq(valueExpr, value.value)
                }
                val targetExpr = mkNot(mkOr(lookups.map { it.second }))
                lookups + (Edge(current, current.defaultTarget as Stmt, lookups.size) to targetExpr)
            }
            else -> error("Unknown switch $current")
        }
        if (successors.size > 1) {
            environment.state.expectUndefined()
            environment.state.definitelyFork()
        }

        successors.forEach { (target, expr) ->
            offerState(
                environment.state.updateQueued(
                    target,
                    SymbolicStateUpdate(hardConstraints = expr.asHardConstraint()),
                )
            )
        }
    }

    private fun TraversalContext.traverseThrowStmt(current: JThrowStmt) {
        val symException = explicitThrown(resolve(current.op), environment.state.isInNestedMethod())
        traverseException(current, symException)
    }

    // TODO HACK violation of encapsulation
    fun createObject(
        addr: UtAddrExpression,
        type: RefType,
        useConcreteType: Boolean,
        mockInfoGenerator: UtMockInfoGenerator? = null
    ): ObjectValue {
        touchAddress(addr)

        if (mockInfoGenerator != null) {
            val mockInfo = mockInfoGenerator.generate(addr)

            queuedSymbolicStateUpdates += MemoryUpdate(addrToMockInfo = persistentHashMapOf(addr to mockInfo))

            val mockedObject = mocker.mock(type, mockInfo)

            if (mockedObject != null) {
                queuedSymbolicStateUpdates += MemoryUpdate(mockInfos = persistentListOf(MockInfoEnriched(mockInfo)))

                // add typeConstraint for mocked object. It's a declared type of the object.
                queuedSymbolicStateUpdates += typeRegistry.typeConstraint(addr, mockedObject.typeStorage).all().asHardConstraint()
                queuedSymbolicStateUpdates += mkEq(typeRegistry.isMock(mockedObject.addr), UtTrue).asHardConstraint()

                return mockedObject
            }
        }

        // construct a type storage that might contain our own types, i.e., UtArrayList
        val typeStoragePossiblyWithOverriddenTypes = typeResolver.constructTypeStorage(type, useConcreteType)
        val leastCommonType = typeStoragePossiblyWithOverriddenTypes.leastCommonType as RefType

        // If the leastCommonType of the created typeStorage is one of our own classes,
        // we must create a copy of the typeStorage with the real classes instead of wrappers.
        // It is required because we do not want to have situations when some object might have
        // only artificial classes as their possible, that would cause problems in the type constraints.
        val typeStorage = if (leastCommonType in wrapperToClass.keys) {
            typeStoragePossiblyWithOverriddenTypes.copy(possibleConcreteTypes = wrapperToClass.getValue(leastCommonType))
        } else {
            typeStoragePossiblyWithOverriddenTypes
        }

        wrapper(type, addr)?.let {
            queuedSymbolicStateUpdates += typeRegistry.typeConstraint(addr, typeStorage).all().asHardConstraint()
            return it
        }

        if (typeStorage.possibleConcreteTypes.isEmpty()) {
            requireNotNull(mockInfoGenerator) {
                "An object with $addr and $type doesn't have concrete possible types," +
                        "but there is no mock info generator provided to construct a mock value."
            }

            val mockInfo = mockInfoGenerator.generate(addr)
            val mockedObject = mocker.forceMock(type, mockInfoGenerator.generate(addr))

            queuedSymbolicStateUpdates += MemoryUpdate(mockInfos = persistentListOf(MockInfoEnriched(mockInfo)))

            // add typeConstraint for mocked object. It's a declared type of the object.
            queuedSymbolicStateUpdates += typeRegistry.typeConstraint(addr, mockedObject.typeStorage).all().asHardConstraint()
            queuedSymbolicStateUpdates += mkEq(typeRegistry.isMock(mockedObject.addr), UtTrue).asHardConstraint()

            return mockedObject
        }

        // If we have this$0 with UtArrayList type, we have to create such instance.
        // We should create an object with typeStorage of all possible real types and concrete implementation
        // Otherwise we'd have either a wrong type in the resolver, or missing method like 'preconditionCheck'.
        val concreteImplementation = wrapperToClass[type]?.first()?.let { wrapper(it, addr) }?.concrete

        queuedSymbolicStateUpdates += typeRegistry.typeConstraint(addr, typeStorage).all().asHardConstraint()
        queuedSymbolicStateUpdates += mkEq(typeRegistry.isMock(addr), UtFalse).asHardConstraint()

        return ObjectValue(typeStorage, addr, concreteImplementation)
    }

    private fun TraversalContext.resolveConstant(constant: Constant): SymbolicValue =
        when (constant) {
            is IntConstant -> constant.value.toPrimitiveValue()
            is LongConstant -> constant.value.toPrimitiveValue()
            is FloatConstant -> constant.value.toPrimitiveValue()
            is DoubleConstant -> constant.value.toPrimitiveValue()
            is StringConstant -> {
                val addr = findNewAddr()
                val refType = constant.type as RefType

                // We disable creation of string literals to avoid unsats because of too long lines
                if (UtSettings.ignoreStringLiterals && constant.value.length > MAX_STRING_SIZE) {
                    // instead of it we create an unbounded symbolic variable
                    workaround(HACK) {
                        offerState(environment.state.withLabel(StateLabel.CONCRETE))
                        createObject(addr, refType, useConcreteType = true)
                    }
                } else {
                    queuedSymbolicStateUpdates += typeRegistry.typeConstraint(addr, TypeStorage(refType)).all().asHardConstraint()

                    objectValue(refType, addr, StringWrapper()).also {
                        initStringLiteral(it, constant.value)
                    }
                }
            }
            is ClassConstant -> {
                val sootType = constant.toSootType()
                val result = if (sootType is RefLikeType) {
                    typeRegistry.createClassRef(sootType.baseType, sootType.numDimensions)
                } else {
                    error("Can't get class constant for ${constant.value}")
                }
                queuedSymbolicStateUpdates += result.symbolicStateUpdate
                (result.symbolicResult as SymbolicSuccess).value
            }
            else -> error("Unsupported type: $constant")
        }

    private fun TraversalContext.resolve(expr: Expr, valueType: Type = expr.type): SymbolicValue =
        when (expr) {
            is BinopExpr -> {
                val left = resolve(expr.op1)
                val right = resolve(expr.op2)
                when {
                    left is ReferenceValue && right is ReferenceValue -> {
                        when (expr) {
                            is JEqExpr -> addrEq(left.addr, right.addr).toBoolValue()
                            is JNeExpr -> mkNot(addrEq(left.addr, right.addr)).toBoolValue()
                            else -> TODO("Unknown op $expr for $left and $right")
                        }
                    }
                    left is PrimitiveValue && right is PrimitiveValue -> {
                        // division by zero special case
                        if ((expr is JDivExpr || expr is JRemExpr) && left.expr.isInteger() && right.expr.isInteger()) {
                            divisionByZeroCheck(right)
                        }

                        if (UtSettings.treatOverflowAsError) {
                            // overflow detection
                            if (left.expr.isInteger() && right.expr.isInteger()) {
                                intOverflowCheck(expr, left, right)
                            }
                        }

                        doOperation(expr, left, right).toPrimitiveValue(expr.type)
                    }
                    else -> TODO("Unknown op $expr for $left and $right")
                }
            }
            is JNegExpr -> UtNegExpression(resolve(expr.op) as PrimitiveValue).toPrimitiveValue(expr.type)
            is JNewExpr -> {
                val addr = findNewAddr()
                val generator = UtMockInfoGenerator { mockAddr ->
                    UtNewInstanceMockInfo(
                        expr.baseType.id,
                        mockAddr,
                        environment.method.declaringClass.id
                    )
                }
                val objectValue = createObject(addr, expr.baseType, useConcreteType = true, generator)
                addConstraintsForDefaultValues(objectValue)
                objectValue
            }
            is JNewArrayExpr -> {
                val size = (resolve(expr.size) as PrimitiveValue).align()
                val type = expr.type as ArrayType
                negativeArraySizeCheck(size)
                createNewArray(size, type, type.elementType).also {
                    val defaultValue = type.defaultSymValue
                    queuedSymbolicStateUpdates += arrayUpdateWithValue(it.addr, type, defaultValue as UtArrayExpressionBase)
                }
            }
            is JNewMultiArrayExpr -> {
                val result = environment.state.methodResult
                    ?: error("There is no unfolded JNewMultiArrayExpr found in the methodResult")
                queuedSymbolicStateUpdates += result.symbolicStateUpdate
                (result.symbolicResult as SymbolicSuccess).value
            }
            is JLengthExpr -> {
                val operand = expr.op as? JimpleLocal ?: error("Unknown op: ${expr.op}")
                when (operand.type) {
                    is ArrayType -> {
                        val arrayInstance = localVariableMemory.local(operand.variable) as ArrayValue?
                            ?: error("${expr.op} not found in the locals")
                        nullPointerExceptionCheck(arrayInstance.addr)
                        memory.findArrayLength(arrayInstance.addr).also { length ->
                            queuedSymbolicStateUpdates += Ge(length, 0).asHardConstraint()
                        }
                    }
                    else -> error("Unknown op: ${expr.op}")
                }
            }
            is JCastExpr -> when (val value = resolve(expr.op, valueType)) {
                is PrimitiveValue -> value.cast(expr.type)
                is ObjectValue -> {
                    castObject(value, expr.type, expr.op)
                }
                is ArrayValue -> castArray(value, expr.type)
            }
            is JInstanceOfExpr -> when (val value = resolve(expr.op, valueType)) {
                is PrimitiveValue -> error("Unexpected instanceof on primitive $value")
                is ObjectValue -> objectInstanceOf(value, expr.checkType, expr.op)
                is ArrayValue -> arrayInstanceOf(value, expr.checkType)
            }
            else -> TODO("$expr")
        }

    private fun initStringLiteral(stringWrapper: ObjectValue, value: String) {
        queuedSymbolicStateUpdates += objectUpdate(
            stringWrapper.copy(typeStorage = TypeStorage(utStringClass.type)),
            STRING_LENGTH,
            mkInt(value.length)
        )
        queuedSymbolicStateUpdates += MemoryUpdate(visitedValues = persistentListOf(stringWrapper.addr))

        val type = CharType.v()
        val arrayType = type.arrayType
        val arrayValue = createNewArray(value.length.toPrimitiveValue(), arrayType, type).also {
            val defaultValue = arrayType.defaultSymValue
            queuedSymbolicStateUpdates += arrayUpdateWithValue(it.addr, arrayType, defaultValue as UtArrayExpressionBase)
        }
        queuedSymbolicStateUpdates += objectUpdate(
            stringWrapper.copy(typeStorage = TypeStorage(utStringClass.type)),
            STRING_VALUE,
            arrayValue.addr
        )
        val newArray = value.indices.fold(selectArrayExpressionFromMemory(arrayValue)) { array, index ->
            array.store(mkInt(index), mkChar(value[index]))
        }

        queuedSymbolicStateUpdates += arrayUpdateWithValue(arrayValue.addr, CharType.v().arrayType, newArray)
        environment.state = environment.state.update(queuedSymbolicStateUpdates)
        queuedSymbolicStateUpdates = queuedSymbolicStateUpdates.copy(memoryUpdates = MemoryUpdate())
    }

    /**
     * Return a symbolic value of the ordinal corresponding to the enum value with the given address.
     */
    private fun findEnumOrdinal(type: RefType, addr: UtAddrExpression): PrimitiveValue {
        val array = memory.findArray(MemoryChunkDescriptor(ENUM_ORDINAL, type, IntType.v()))
        return array.select(addr).toIntValue()
    }

    /**
     * Initialize enum class: create symbolic values for static enum values and generate constraints
     * that restrict the new instance to match one of enum values.
     */
    private fun initEnum(type: RefType, addr: UtAddrExpression, ordinal: PrimitiveValue) {
        val classId = type.id
        var predefinedEnumValues = memory.getSymbolicEnumValues(classId)
        if (predefinedEnumValues.isEmpty()) {
            val (enumValuesUpdate, _) = makeConcreteUpdatesForEnum(type)
            queuedSymbolicStateUpdates += enumValuesUpdate
            predefinedEnumValues = enumValuesUpdate.memoryUpdates.getSymbolicEnumValues(classId)
        }

        val enumValueConstraints = mkOr(
            listOf(addrEq(addr, nullObjectAddr)) + predefinedEnumValues.map {
                mkAnd(
                    addrEq(addr, it.addr),
                    mkEq(ordinal, findEnumOrdinal(it.type, it.addr))
                )
            }
        )

        queuedSymbolicStateUpdates += enumValueConstraints.asHardConstraint()
    }

    private fun arrayInstanceOf(value: ArrayValue, checkType: Type): PrimitiveValue {
        val notNullConstraint = mkNot(addrEq(value.addr, nullObjectAddr))

        if (checkType.isJavaLangObject()) {
            return UtInstanceOfExpression(notNullConstraint.asHardConstraint().asUpdate()).toBoolValue()
        }

        require(checkType is ArrayType)

        val checkBaseType = checkType.baseType

        // i.e., int[][] instanceof Object[]
        if (checkBaseType.isJavaLangObject()) {
            return UtInstanceOfExpression(notNullConstraint.asHardConstraint().asUpdate()).toBoolValue()
        }

        // Object[] instanceof int[][]
        if (value.type.baseType.isJavaLangObject() && checkBaseType is PrimType) {
            val updatedTypeStorage = typeResolver.constructTypeStorage(checkType, useConcreteType = false)
            val typeConstraint = typeRegistry.typeConstraint(value.addr, updatedTypeStorage).isConstraint()

            val constraint = mkAnd(notNullConstraint, typeConstraint)
            val memoryUpdate = arrayTypeUpdate(value.addr, checkType)
            val symbolicStateUpdate = SymbolicStateUpdate(
                hardConstraints = constraint.asHardConstraint(),
                memoryUpdates = memoryUpdate
            )

            return UtInstanceOfExpression(symbolicStateUpdate).toBoolValue()
        }

        // We must create a new typeStorage containing ALL the inheritors for checkType,
        // because later we will create a negation for the typeConstraint
        val updatedTypeStorage = typeResolver.constructTypeStorage(checkType, useConcreteType = false)

        val typesIntersection = updatedTypeStorage.possibleConcreteTypes.intersect(value.possibleConcreteTypes)
        if (typesIntersection.isEmpty()) return UtFalse.toBoolValue()

        val typeConstraint = typeRegistry.typeConstraint(value.addr, updatedTypeStorage).isConstraint()
        val constraint = mkAnd(notNullConstraint, typeConstraint)

        val arrayType = updatedTypeStorage.leastCommonType as ArrayType
        val memoryUpdate = arrayTypeUpdate(value.addr, arrayType)
        val symbolicStateUpdate = SymbolicStateUpdate(
            hardConstraints = constraint.asHardConstraint(),
            memoryUpdates = memoryUpdate
        )

        return UtInstanceOfExpression(symbolicStateUpdate).toBoolValue()
    }

    private fun objectInstanceOf(value: ObjectValue, checkType: Type, op: Value): PrimitiveValue {
        val notNullConstraint = mkNot(addrEq(value.addr, nullObjectAddr))

        // the only way to get false here is for the value to be null
        if (checkType.isJavaLangObject()) {
            return UtInstanceOfExpression(notNullConstraint.asHardConstraint().asUpdate()).toBoolValue()
        }

        if (value.type.isJavaLangObject() && checkType is ArrayType) {
            val castedArray =
                createArray(value.addr, checkType, useConcreteType = false, addQueuedTypeConstraints = false)
            val localVariable = (op as? JimpleLocal)?.variable ?: error("Unexpected op in the instanceof expr: $op")

            val typeMemoryUpdate = arrayTypeUpdate(value.addr, castedArray.type)
            val localMemoryUpdate = localMemoryUpdate(localVariable to castedArray)

            val typeConstraint = typeRegistry.typeConstraint(value.addr, castedArray.typeStorage).isConstraint()
            val constraint = mkAnd(notNullConstraint, typeConstraint)
            val symbolicStateUpdate = SymbolicStateUpdate(
                hardConstraints = constraint.asHardConstraint(),
                memoryUpdates = typeMemoryUpdate,
                localMemoryUpdates = localMemoryUpdate
            )

            return UtInstanceOfExpression(symbolicStateUpdate).toBoolValue()
        }

        require(checkType is RefType)

        // We must create a new typeStorage containing ALL the inheritors for checkType,
        // because later we will create a negation for the typeConstraint
        val updatedTypeStorage = typeResolver.constructTypeStorage(checkType, useConcreteType = false)

        // drop this branch if we don't have an appropriate type in the possibleTypes
        val typesIntersection = updatedTypeStorage.possibleConcreteTypes.intersect(value.possibleConcreteTypes)
        if (typesIntersection.isEmpty()) return UtFalse.toBoolValue()

        val typeConstraint = typeRegistry.typeConstraint(value.addr, updatedTypeStorage).isConstraint()
        val constraint = mkAnd(notNullConstraint, typeConstraint)

        return UtInstanceOfExpression(constraint.asHardConstraint().asUpdate()).toBoolValue()
    }

    private fun addConstraintsForDefaultValues(objectValue: ObjectValue) {
        val type = objectValue.type
        for (field in typeResolver.findFields(type)) {
            // final fields must be initialized inside the body of a constructor
            if (field.isFinal) continue
            val chunkId = hierarchy.chunkIdForField(type, field)
            val memoryChunkDescriptor = MemoryChunkDescriptor(chunkId, type, field.type)
            val array = memory.findArray(memoryChunkDescriptor)
            val defaultValue = if (field.type is RefLikeType) nullObjectAddr else field.type.defaultSymValue
            queuedSymbolicStateUpdates += mkEq(array.select(objectValue.addr), defaultValue).asHardConstraint()
        }
    }

    private fun TraversalContext.castObject(objectValue: ObjectValue, typeAfterCast: Type, op: Value): SymbolicValue {
        classCastExceptionCheck(objectValue, typeAfterCast)

        val currentType = objectValue.type
        val nullConstraint = addrEq(objectValue.addr, nullObjectAddr)

        // If we're trying to cast type A to the same type A
        if (currentType == typeAfterCast) return objectValue

        // java.lang.Object -> array
        if (currentType.isJavaLangObject() && typeAfterCast is ArrayType) {
            val array = createArray(objectValue.addr, typeAfterCast, useConcreteType = false)

            val localVariable = (op as? JimpleLocal)?.variable ?: error("Unexpected op in the cast: $op")

/*
            val typeConstraint = typeRegistry.typeConstraint(array.addr, array.typeStorage).isOrNullConstraint()

            queuedSymbolicStateUpdates += typeConstraint.asHardConstraint()
*/

            queuedSymbolicStateUpdates += localMemoryUpdate(localVariable to array)
            queuedSymbolicStateUpdates += arrayTypeUpdate(array.addr, array.type)

            return array
        }

        val ancestors = typeResolver.findOrConstructAncestorsIncludingTypes(currentType)
        // if we're trying to cast type A to it's predecessor
        if (typeAfterCast in ancestors) return objectValue

        require(typeAfterCast is RefType)

        val castedObject = typeResolver.downCast(objectValue, typeAfterCast)

        // The objectValue must be null to be casted to an impossible type
        if (castedObject.possibleConcreteTypes.isEmpty()) {
            queuedSymbolicStateUpdates += nullConstraint.asHardConstraint()
            return objectValue.copy(addr = nullObjectAddr)
        }

        val typeConstraint = typeRegistry.typeConstraint(castedObject.addr, castedObject.typeStorage).isOrNullConstraint()

        // When we do downCast, we should add possible equality to null
        // to avoid situation like this:
        // we have class A, class B extends A, class C extends A
        // void foo(a A) { (B) a; (C) a; } -> a is null
        queuedSymbolicStateUpdates += typeConstraint.asHardConstraint()
        queuedSymbolicStateUpdates += typeRegistry.zeroDimensionConstraint(objectValue.addr).asHardConstraint()

        // If we are casting to an enum class, we should initialize enum values and add value equality constraints
        if (typeAfterCast.sootClass?.isEnum == true) {
            initEnum(typeAfterCast, castedObject.addr, findEnumOrdinal(typeAfterCast, castedObject.addr))
        }

        // TODO add memory constraints JIRA:1523
        return castedObject
    }

    private fun TraversalContext.castArray(arrayValue: ArrayValue, typeAfterCast: Type): ArrayValue {
        classCastExceptionCheck(arrayValue, typeAfterCast)

        if (typeAfterCast.isJavaLangObject()) return arrayValue

        require(typeAfterCast is ArrayType)

        // cast A[] to A[]
        if (arrayValue.type == typeAfterCast) return arrayValue

        val baseTypeBeforeCast = arrayValue.type.baseType
        val baseTypeAfterCast = typeAfterCast.baseType

        val nullConstraint = addrEq(arrayValue.addr, nullObjectAddr)

        // i.e. cast Object[] -> int[][]
        if (baseTypeBeforeCast.isJavaLangObject() && baseTypeAfterCast is PrimType) {
            val castedArray = createArray(arrayValue.addr, typeAfterCast)

            val memoryUpdate = arrayTypeUpdate(castedArray.addr, castedArray.type)

            queuedSymbolicStateUpdates += memoryUpdate

            return castedArray
        }

        // int[][] -> Object[]
        if (baseTypeBeforeCast is PrimType && baseTypeAfterCast.isJavaLangObject()) return arrayValue

        require(baseTypeBeforeCast is RefType)
        require(baseTypeAfterCast is RefType)

        // Integer[] -> Number[]
        val ancestors = typeResolver.findOrConstructAncestorsIncludingTypes(baseTypeBeforeCast)
        if (baseTypeAfterCast in ancestors) return arrayValue

        val castedArray = typeResolver.downCast(arrayValue, typeAfterCast)

        // cast to an unreachable type
        if (castedArray.possibleConcreteTypes.isEmpty()) {
            queuedSymbolicStateUpdates += nullConstraint.asHardConstraint()
            return arrayValue.copy(addr = nullObjectAddr)
        }

        val typeConstraint = typeRegistry.typeConstraint(castedArray.addr, castedArray.typeStorage).isOrNullConstraint()
        val memoryUpdate = arrayTypeUpdate(castedArray.addr, castedArray.type)

        queuedSymbolicStateUpdates += typeConstraint.asHardConstraint()
        queuedSymbolicStateUpdates += memoryUpdate

        return castedArray
    }

    /**
     * @param size [SymbolicValue] representing size of an array. It's caller responsibility to handle negative
     * size.
     */
    internal fun createNewArray(size: PrimitiveValue, type: ArrayType, elementType: Type): ArrayValue {
        val addr = findNewAddr()
        val length = memory.findArrayLength(addr)

        queuedSymbolicStateUpdates += Eq(length, size).asHardConstraint()
        queuedSymbolicStateUpdates += Ge(length, 0).asHardConstraint()
        workaround(HACK) {
            if (size.expr is UtBvLiteral) {
                softMaxArraySize = min(HARD_MAX_ARRAY_SIZE, max(size.expr.value.toInt(), softMaxArraySize))
            }
        }
        queuedSymbolicStateUpdates += Le(length, softMaxArraySize).asHardConstraint() // TODO: fix big array length

        if (preferredCexOption) {
            queuedSymbolicStateUpdates += Le(length, PREFERRED_ARRAY_SIZE).asSoftConstraint()
        }
        val chunkId = typeRegistry.arrayChunkId(type)
        touchMemoryChunk(MemoryChunkDescriptor(chunkId, type, elementType))

        return ArrayValue(TypeStorage(type), addr).also {
            queuedSymbolicStateUpdates += typeRegistry.typeConstraint(addr, it.typeStorage).all().asHardConstraint()
        }
    }

    private fun SymbolicValue.simplify(): SymbolicValue =
        when (this) {
            is PrimitiveValue -> copy(expr = expr.accept(solver.rewriter))
            is ObjectValue -> copy(addr = addr.accept(solver.rewriter) as UtAddrExpression)
            is ArrayValue -> copy(addr = addr.accept(solver.rewriter) as UtAddrExpression)
        }


    // Type is needed for null values: we should know, which null do we require.
    // If valueType is NullType, return typelessNullObject. It can happen in a situation,
    // where we cannot find the type, for example in condition (null == null)
    private fun TraversalContext.resolve(
        value: Value,
        valueType: Type = value.type
    ): SymbolicValue = when (value) {
        is JimpleLocal -> localVariableMemory.local(value.variable) ?: error("${value.name} not found in the locals")
        is Constant -> if (value is NullConstant) typeResolver.nullObject(valueType) else resolveConstant(value)
        is Expr -> resolve(value, valueType).simplify()
        is JInstanceFieldRef -> {
            val instance = (resolve(value.base) as ObjectValue)
            recordInstanceFieldRead(instance.addr, value.field)
            nullPointerExceptionCheck(instance.addr)

            val objectType = if (instance.concrete?.value is BaseOverriddenWrapper) {
                instance.concrete.value.overriddenClass.type
            } else {
                value.field.declaringClass.type as RefType
            }
            val generator = (value.field.type as? RefType)?.let { refType ->
                UtMockInfoGenerator { mockAddr ->
                    val fieldId = FieldId(objectType.id, value.field.name)
                    UtFieldMockInfo(refType.id, mockAddr, fieldId, instance.addr)
                }
            }
            createFieldOrMock(objectType, instance.addr, value.field, generator).also { fieldValue ->
                preferredCexInstanceCache[instance]?.let { usedCache ->
                    if (usedCache.add(value.field)) {
                        applyPreferredConstraints(fieldValue)
                    }
                }
            }
        }
        is JArrayRef -> {
            val arrayInstance = resolve(value.base) as ArrayValue
            nullPointerExceptionCheck(arrayInstance.addr)

            val index = (resolve(value.index) as PrimitiveValue).align()
            val length = memory.findArrayLength(arrayInstance.addr)
            indexOutOfBoundsChecks(index, length)

            val type = arrayInstance.type
            val elementType = type.elementType
            val chunkId = typeRegistry.arrayChunkId(type)
            val descriptor = MemoryChunkDescriptor(chunkId, type, elementType).also { touchMemoryChunk(it) }
            val array = memory.findArray(descriptor)

            when (elementType) {
                is RefType -> {
                    val generator = UtMockInfoGenerator { mockAddr -> UtObjectMockInfo(elementType.id, mockAddr) }

                    val objectValue = createObject(
                        UtAddrExpression(array.select(arrayInstance.addr, index.expr)),
                        elementType,
                        useConcreteType = false,
                        generator
                    )

                    if (objectValue.type.isJavaLangObject()) {
                        queuedSymbolicStateUpdates += typeRegistry.zeroDimensionConstraint(objectValue.addr).asSoftConstraint()
                    }

                    objectValue
                }
                is ArrayType -> createArray(
                    UtAddrExpression(array.select(arrayInstance.addr, index.expr)),
                    elementType,
                    useConcreteType = false
                )
                else -> PrimitiveValue(elementType, array.select(arrayInstance.addr, index.expr))
            }
        }
        is StaticFieldRef -> readStaticField(value)
        else -> error("${value::class} is not supported")
    }

    private fun readStaticField(fieldRef: StaticFieldRef): SymbolicValue {
        val field = fieldRef.field
        val declaringClassType = field.declaringClass.type
        val staticObject = findOrCreateStaticObject(declaringClassType)

        val generator = (field.type as? RefType)?.let { refType ->
            UtMockInfoGenerator { mockAddr ->
                val fieldId = FieldId(declaringClassType.id, field.name)
                UtFieldMockInfo(refType.id, mockAddr, fieldId, ownerAddr = null)
            }
        }
        val createdField = createFieldOrMock(declaringClassType, staticObject.addr, field, generator).also { value ->
            preferredCexInstanceCache.entries
                .firstOrNull { declaringClassType == it.key.type }?.let {
                    if (it.value.add(field)) {
                        applyPreferredConstraints(value)
                    }
                }
        }

        val fieldId = field.fieldId
        val staticFieldMemoryUpdate = StaticFieldMemoryUpdateInfo(fieldId, createdField)
        val touchedStaticFields = persistentListOf(staticFieldMemoryUpdate)
        queuedSymbolicStateUpdates += MemoryUpdate(staticFieldsUpdates = touchedStaticFields)

        // TODO filter enum constant static fields JIRA:1681
        if (!environment.method.isStaticInitializer && isStaticFieldMeaningful(field)) {
            queuedSymbolicStateUpdates += MemoryUpdate(meaningfulStaticFields = persistentSetOf(fieldId))
        }

        return createdField
    }

    /**
     * For now the field is `meaningful` if it is safe to set, that is, it is not an internal system field nor a
     * synthetic field. This filter is needed to prohibit changing internal fields, which can break up our own
     * code and which are useless for the user.
     *
     * @return `true` if the field is meaningful, `false` otherwise.
     */
    private fun isStaticFieldMeaningful(field: SootField) =
        !Modifier.isSynthetic(field.modifiers) &&
            // we don't want to set fields that cannot be set via reflection anyway
            !field.fieldId.isInaccessibleViaReflection &&
            // we don't want to set fields from library classes
            !workaround(IGNORE_STATICS_FROM_TRUSTED_LIBRARIES) {
                ignoreStaticsFromTrustedLibraries && field.declaringClass.isFromTrustedLibrary()
            }

    /**
     * Locates object represents static fields of particular class.
     *
     * If object does not exist in the memory, returns null.
     */
    fun locateStaticObject(classType: RefType): ObjectValue? = memory.findStaticInstanceOrNull(classType.id)

    /**
     * Locates object represents static fields of particular class.
     *
     * If object is not exist in memory, creates a new one and put it into memory updates.
     */
    private fun findOrCreateStaticObject(
        classType: RefType,
        mockInfoGenerator: UtMockInfoGenerator? = null
    ): ObjectValue {
        val fromMemory = locateStaticObject(classType)

        // true if the object exists in the memory and he already has concrete value or mockInfoGenerator is null
        // It's important to avoid situations when we've already created object earlier without mock, and now
        // we want to mock this object
        if (fromMemory != null && (fromMemory.concrete != null || mockInfoGenerator == null)) {
            return fromMemory
        }
        val addr = fromMemory?.addr ?: findNewAddr()
        val created = createObject(addr, classType, useConcreteType = false, mockInfoGenerator)
        queuedSymbolicStateUpdates += MemoryUpdate(staticInstanceStorage = persistentHashMapOf(classType.id to created))
        return created
    }

    private fun TraversalContext.resolveParameters(parameters: List<Value>, types: List<Type>) =
        parameters.zip(types).map { (value, type) -> resolve(value, type) }

    private fun applyPreferredConstraints(value: SymbolicValue) {
        when (value) {
            is PrimitiveValue, is ArrayValue -> queuedSymbolicStateUpdates += preferredConstraints(value).asSoftConstraint()
            is ObjectValue -> preferredCexInstanceCache.putIfAbsent(value, mutableSetOf())
        }
    }

    private fun preferredConstraints(variable: SymbolicValue): List<UtBoolExpression> =
        when (variable) {
            is PrimitiveValue ->
                when (variable.type) {
                    is ByteType, is ShortType, is IntType, is LongType -> {
                        listOf(Ge(variable, MIN_PREFERRED_INTEGER), Le(variable, MAX_PREFERRED_INTEGER))
                    }
                    is CharType -> {
                        listOf(Ge(variable, MIN_PREFERRED_CHARACTER), Le(variable, MAX_PREFERRED_CHARACTER))
                    }
                    else -> emptyList()
                }
            is ArrayValue -> {
                val type = variable.type
                val elementType = type.elementType
                val constraints = mutableListOf<UtBoolExpression>()
                val array = memory.findArray(
                    MemoryChunkDescriptor(
                        typeRegistry.arrayChunkId(variable.type),
                        variable.type,
                        elementType
                    )
                )
                constraints += Le(memory.findArrayLength(variable.addr), PREFERRED_ARRAY_SIZE)
                for (i in 0 until softMaxArraySize) {
                    constraints += preferredConstraints(
                        array.select(variable.addr, mkInt(i)).toPrimitiveValue(elementType)
                    )
                }
                constraints
            }
            is ObjectValue -> error("Unsupported type of $variable for preferredConstraints option")
        }

    private fun createField(
        objectType: RefType,
        addr: UtAddrExpression,
        fieldType: Type,
        chunkId: ChunkId,
        mockInfoGenerator: UtMockInfoGenerator? = null
    ): SymbolicValue {
        val descriptor = MemoryChunkDescriptor(chunkId, objectType, fieldType)
        val array = memory.findArray(descriptor)
        val value = array.select(addr)
        touchMemoryChunk(descriptor)
        return when (fieldType) {
            is RefType -> createObject(
                UtAddrExpression(value),
                fieldType,
                useConcreteType = false,
                mockInfoGenerator
            )
            is ArrayType -> createArray(UtAddrExpression(value), fieldType, useConcreteType = false)
            else -> PrimitiveValue(fieldType, value)
        }
    }

    /**
     * Creates field that can be mock. Mock strategy to decide.
     */
    fun createFieldOrMock(
        objectType: RefType,
        addr: UtAddrExpression,
        field: SootField,
        mockInfoGenerator: UtMockInfoGenerator?
    ): SymbolicValue {
        val chunkId = hierarchy.chunkIdForField(objectType, field)
        val createdField = createField(objectType, addr, field.type, chunkId, mockInfoGenerator)

        if (field.type is RefLikeType) {
            if (field.shouldBeNotNull()) {
                queuedSymbolicStateUpdates += mkNot(mkEq(createdField.addr, nullObjectAddr)).asHardConstraint()
            }

            // See docs/SpeculativeFieldNonNullability.md for details
            checkAndMarkLibraryFieldSpeculativelyNotNull(field, createdField)
        }

        return createdField
    }

    /**
     * Marks the [createdField] as speculatively not null if the [field] is considering as
     * not producing [NullPointerException].
     *
     * @see [SootField.speculativelyCannotProduceNullPointerException], [markAsSpeculativelyNotNull], [isFromTrustedLibrary].
     */
    private fun checkAndMarkLibraryFieldSpeculativelyNotNull(field: SootField, createdField: SymbolicValue) {
        if (maximizeCoverageUsingReflection || !field.declaringClass.isFromTrustedLibrary()) {
            return
        }

        if (field.speculativelyCannotProduceNullPointerException()) {
            markAsSpeculativelyNotNull(createdField.addr)
        }
    }

    /**
     * Checks whether accessing [this] field (with a method invocation or field access) speculatively can produce
     * [NullPointerException] (according to its finality or accessibility).
     *
     * @see docs/SpeculativeFieldNonNullability.md for more information.
     */
    @Suppress("KDocUnresolvedReference")
    private fun SootField.speculativelyCannotProduceNullPointerException(): Boolean = isFinal || !isPublic

    private fun createArray(pName: String, type: ArrayType): ArrayValue {
        val addr = UtAddrExpression(mkBVConst(pName, UtIntSort))
        return createArray(addr, type, useConcreteType = false)
    }

    /**
     * Creates an array with given [addr] and [type].
     *
     * [addQueuedTypeConstraints] is used to indicate whether we want to create array and work with its information
     * by ourselves (i.e. in the instanceof) or to create an array and add type information
     * into the [queuedSymbolicStateUpdates] right here.
     */
    internal fun createArray(
        addr: UtAddrExpression, type: ArrayType,
        @Suppress("SameParameterValue") useConcreteType: Boolean = false,
        addQueuedTypeConstraints: Boolean = true
    ): ArrayValue {
        touchAddress(addr)

        val length = memory.findArrayLength(addr)

        queuedSymbolicStateUpdates += Ge(length, 0).asHardConstraint()
        queuedSymbolicStateUpdates += Le(length, softMaxArraySize).asHardConstraint() // TODO: fix big array length

        if (preferredCexOption) {
            queuedSymbolicStateUpdates += Le(length, PREFERRED_ARRAY_SIZE).asSoftConstraint()
            if (type.elementType is RefType) {
                val descriptor = MemoryChunkDescriptor(typeRegistry.arrayChunkId(type), type, type.elementType)
                val array = memory.findArray(descriptor)
                queuedSymbolicStateUpdates += (0 until softMaxArraySize).flatMap {
                    val innerAddr = UtAddrExpression(array.select(addr, mkInt(it)))
                    mutableListOf<UtBoolExpression>().apply {
                        add(addrEq(innerAddr, nullObjectAddr))

                        // if we have an array of Object, assume that all of them have zero number of dimensions
                        if (type.elementType.isJavaLangObject()) {
                            add(typeRegistry.zeroDimensionConstraint(UtAddrExpression(innerAddr)))
                        }
                    }
                }.asSoftConstraint()
            }

        }
        val typeStorage = typeResolver.constructTypeStorage(type, useConcreteType)

        if (addQueuedTypeConstraints) {
            queuedSymbolicStateUpdates += typeRegistry.typeConstraint(addr, typeStorage).all().asHardConstraint()
        }

        touchMemoryChunk(MemoryChunkDescriptor(typeRegistry.arrayChunkId(type), type, type.elementType))
        return ArrayValue(typeStorage, addr)
    }

    /**
     * RefType and ArrayType consts have addresses less or equals to NULL_ADDR in order to separate objects
     * created inside our program and given from outside. All given objects have negative addr or equal to NULL_ADDR.
     * Since createConst called only for objects from outside at the beginning of the analysis,
     * we can set Le(addr, NULL_ADDR) for all RefValue objects.
     */
    private fun Value.createConst(pName: String, mockInfoGenerator: UtMockInfoGenerator? = null): SymbolicValue =
        createConst(type, pName, mockInfoGenerator)

    fun createConst(type: Type, pName: String, mockInfoGenerator: UtMockInfoGenerator? = null): SymbolicValue =
        when (type) {
            is ByteType -> mkBVConst(pName, UtByteSort).toByteValue()
            is ShortType -> mkBVConst(pName, UtShortSort).toShortValue()
            is IntType -> mkBVConst(pName, UtIntSort).toIntValue()
            is LongType -> mkBVConst(pName, UtLongSort).toLongValue()
            is FloatType -> mkFpConst(pName, Float.SIZE_BITS).toFloatValue()
            is DoubleType -> mkFpConst(pName, Double.SIZE_BITS).toDoubleValue()
            is BooleanType -> mkBoolConst(pName).toBoolValue()
            is CharType -> mkBVConst(pName, UtCharSort).toCharValue()
            is ArrayType -> createArray(pName, type).also {
                val addr = it.addr.toIntValue()
                queuedSymbolicStateUpdates += Le(addr, nullObjectAddr.toIntValue()).asHardConstraint()
                // if we don't 'touch' this array during the execution, it should be null
                queuedSymbolicStateUpdates += addrEq(it.addr, nullObjectAddr).asSoftConstraint()
            }
            is RefType -> {
                val addr = UtAddrExpression(mkBVConst(pName, UtIntSort))
                queuedSymbolicStateUpdates += Le(addr.toIntValue(), nullObjectAddr.toIntValue()).asHardConstraint()
                // if we don't 'touch' this object during the execution, it should be null
                queuedSymbolicStateUpdates += addrEq(addr, nullObjectAddr).asSoftConstraint()

                if (type.sootClass.isEnum) {
                    createEnum(type, addr)
                } else {
                    createObject(addr, type, useConcreteType = addr.isThisAddr, mockInfoGenerator)
                }
            }
            is VoidType -> voidValue
            else -> error("Can't create const from ${type::class}")
        }

    private fun createEnum(type: RefType, addr: UtAddrExpression): ObjectValue {
        val typeStorage = typeResolver.constructTypeStorage(type, useConcreteType = true)

        queuedSymbolicStateUpdates += typeRegistry.typeConstraint(addr, typeStorage).all().asHardConstraint()

        val ordinal = findEnumOrdinal(type, addr)
        val enumSize = classLoader.loadClass(type.sootClass.name).enumConstants.size

        queuedSymbolicStateUpdates += mkOr(Ge(ordinal, 0), addrEq(addr, nullObjectAddr)).asHardConstraint()
        queuedSymbolicStateUpdates += mkOr(Lt(ordinal, enumSize), addrEq(addr, nullObjectAddr)).asHardConstraint()

        initEnum(type, addr, ordinal)
        touchAddress(addr)

        return ObjectValue(typeStorage, addr)
    }

    private fun arrayUpdate(array: ArrayValue, index: PrimitiveValue, value: UtExpression): MemoryUpdate {
        val type = array.type
        val chunkId = typeRegistry.arrayChunkId(type)
        val descriptor = MemoryChunkDescriptor(chunkId, type, type.elementType)

        val updatedNestedArray = memory.findArray(descriptor)
            .select(array.addr)
            .store(index.expr, value)

        return MemoryUpdate(persistentListOf(simplifiedNamedStore(descriptor, array.addr, updatedNestedArray)))
    }

    fun objectUpdate(
        instance: ObjectValue,
        field: SootField,
        value: UtExpression
    ): MemoryUpdate {
        val chunkId = hierarchy.chunkIdForField(instance.type, field)
        val descriptor = MemoryChunkDescriptor(chunkId, instance.type, field.type)
        return MemoryUpdate(persistentListOf(simplifiedNamedStore(descriptor, instance.addr, value)))
    }

    fun arrayUpdateWithValue(
        addr: UtAddrExpression,
        type: ArrayType,
        newValue: UtExpression
    ): MemoryUpdate {
        require(newValue.sort is UtArraySort) { "Expected UtArraySort, but ${newValue.sort} was found" }

        val chunkId = typeRegistry.arrayChunkId(type)
        val descriptor = MemoryChunkDescriptor(chunkId, type, type.elementType)

        return MemoryUpdate(persistentListOf(simplifiedNamedStore(descriptor, addr, newValue)))
    }

    fun selectArrayExpressionFromMemory(
        array: ArrayValue
    ): UtExpression {
        val addr = array.addr
        val arrayType = array.type
        val chunkId = typeRegistry.arrayChunkId(arrayType)
        val descriptor = MemoryChunkDescriptor(chunkId, arrayType, arrayType.elementType)
        return memory.findArray(descriptor).select(addr)
    }

    private fun touchMemoryChunk(chunkDescriptor: MemoryChunkDescriptor) {
        queuedSymbolicStateUpdates += MemoryUpdate(touchedChunkDescriptors = persistentSetOf(chunkDescriptor))
    }

    private fun touchAddress(addr: UtAddrExpression) {
        queuedSymbolicStateUpdates += MemoryUpdate(touchedAddresses = persistentListOf(addr))
    }

    private fun markAsSpeculativelyNotNull(addr: UtAddrExpression) {
        queuedSymbolicStateUpdates += MemoryUpdate(speculativelyNotNullAddresses = persistentListOf(addr))
    }

    /**
     * Add a memory update to reflect that a field was read.
     *
     * If the field belongs to a substitute object, record the read access for the real type instead.
     */
    private fun recordInstanceFieldRead(addr: UtAddrExpression, field: SootField) {
        val realType = typeRegistry.findRealType(field.declaringClass.type)
        if (realType is RefType) {
            val readOperation = InstanceFieldReadOperation(addr, FieldId(realType.id, field.name))
            queuedSymbolicStateUpdates += MemoryUpdate(instanceFieldReads = persistentSetOf(readOperation))
        }
    }

    private fun TraversalContext.traverseException(current: Stmt, exception: SymbolicFailure) {
        if (!traverseCatchBlock(current, exception, emptySet())) {
            processResult(exception)
        }
    }

    /**
     * Finds appropriate catch block and adds it as next state to path selector.
     *
     * Returns true if found, false otherwise.
     */
    private fun TraversalContext.traverseCatchBlock(
        current: Stmt,
        exception: SymbolicFailure,
        conditions: Set<UtBoolExpression>
    ): Boolean {
        val classId = exception.fold(
            { it.javaClass.id },
            { (exception.symbolic as ObjectValue).type.id }
        )
        val edge = findCatchBlock(current, classId) ?: return false

        offerState(
            environment.state.updateQueued(
                edge,
                SymbolicStateUpdate(
                    hardConstraints = conditions.asHardConstraint(),
                    localMemoryUpdates = localMemoryUpdate(CAUGHT_EXCEPTION to exception.symbolic)
                )
            )
        )
        return true
    }

    private fun findCatchBlock(current: Stmt, classId: ClassId): Edge? {
        val stmtToEdge = globalGraph.exceptionalSuccs(current).associateBy { it.dst }
        return globalGraph.traps.asSequence().mapNotNull { (stmt, exceptionClass) ->
            stmtToEdge[stmt]?.let { it to exceptionClass }
        }.firstOrNull { it.second in hierarchy.ancestors(classId) }?.first
    }

    private fun TraversalContext.invokeResult(invokeExpr: Expr): List<MethodResult> =
        environment.state.methodResult?.let {
            listOf(it)
        } ?: when (invokeExpr) {
            is JStaticInvokeExpr -> staticInvoke(invokeExpr)
            is JInterfaceInvokeExpr -> virtualAndInterfaceInvoke(invokeExpr.base, invokeExpr.methodRef, invokeExpr.args)
            is JVirtualInvokeExpr -> virtualAndInterfaceInvoke(invokeExpr.base, invokeExpr.methodRef, invokeExpr.args)
            is JSpecialInvokeExpr -> specialInvoke(invokeExpr)
            is JDynamicInvokeExpr -> dynamicInvoke(invokeExpr)
            else -> error("Unknown class ${invokeExpr::class}")
        }

    /**
     * Returns a [MethodResult] containing a mock for a static method call
     * of the [method] if it should be mocked, null otherwise.
     *
     * @see Mocker.shouldMock
     * @see UtStaticMethodMockInfo
     */
    private fun mockStaticMethod(method: SootMethod, args: List<SymbolicValue>): List<MethodResult>? {
        val methodId = method.executableId as MethodId
        val declaringClassType = method.declaringClass.type

        val generator = UtMockInfoGenerator { addr -> UtStaticObjectMockInfo(declaringClassType.classId, addr) }
        // It is important to save the previous state of the queuedMemoryUpdates, because `findOrCreateStaticObject`
        // will change it. If we should not mock the object, we must `reset` memory updates to the previous state.
        val prevMemoryUpdate = queuedSymbolicStateUpdates.memoryUpdates
        val static = findOrCreateStaticObject(declaringClassType, generator)

        val mockInfo = UtStaticMethodMockInfo(static.addr, methodId)

        // We don't want to mock synthetic, private and protected methods
        val isUnwantedToMockMethod = method.isSynthetic || method.isPrivate || method.isProtected
        val shouldMock = mocker.shouldMock(declaringClassType, mockInfo)
        val privateAndProtectedMethodsInArgs = parametersContainPrivateAndProtectedTypes(method)

        if (!shouldMock || method.isStaticInitializer) {
            queuedSymbolicStateUpdates = queuedSymbolicStateUpdates.copy(memoryUpdates = prevMemoryUpdate)
            return null
        }

        // TODO temporary we return unbounded symbolic variable with a wrong name.
        // TODO Probably it'll lead us to the path divergence
        workaround(HACK) {
            if (isUnwantedToMockMethod || privateAndProtectedMethodsInArgs) {
                queuedSymbolicStateUpdates = queuedSymbolicStateUpdates.copy(memoryUpdates = prevMemoryUpdate)
                return listOf(unboundedVariable(name = "staticMethod", method))
            }
        }

        return static.asWrapperOrNull?.run {
            invoke(static, method, args).map { it as MethodResult }
        }
    }

    private fun parametersContainPrivateAndProtectedTypes(method: SootMethod) =
        method.parameterTypes.any { paramType ->
            (paramType.baseType as? RefType)?.let {
                it.sootClass.isPrivate || it.sootClass.isProtected
            } == true
        }

    /**
     * Returns [MethodResult] with a mock for [org.utbot.api.mock.UtMock.makeSymbolic] call,
     * if the [invokeExpr] contains it, null otherwise.
     *
     * @see mockStaticMethod
     */
    private fun TraversalContext.mockMakeSymbolic(invokeExpr: JStaticInvokeExpr): List<MethodResult>? {
        val methodSignature = invokeExpr.method.signature
        if (methodSignature != makeSymbolicMethod.signature && methodSignature != nonNullableMakeSymbolic.signature) return null

        val method = environment.method
        val declaringClass = method.declaringClass
        val isInternalMock = method.hasInternalMockAnnotation || declaringClass.allMethodsAreInternalMocks || declaringClass.isOverridden
        val parameters = resolveParameters(invokeExpr.args, invokeExpr.method.parameterTypes)
        val mockMethodResult = mockStaticMethod(invokeExpr.method, parameters)?.single()
            ?: error("Unsuccessful mock attempt of the `makeSymbolic` method call: $invokeExpr")
        val mockResult = mockMethodResult.symbolicResult as SymbolicSuccess
        val mockValue = mockResult.value

        // the last parameter of the makeSymbolic is responsible for nullability
        val isNullable = if (parameters.isEmpty()) UtFalse else UtCastExpression(
            parameters.last() as PrimitiveValue,
            BooleanType.v()
        )

        //  isNullable || mockValue != null
        val additionalConstraint = mkOr(
            mkEq(isNullable, UtTrue),
            mkNot(mkEq(mockValue.addr, nullObjectAddr)),
        )

        // since makeSymbolic returns Object and casts it during the next instruction, we should
        // disable ClassCastException for it to avoid redundant ClassCastException
        typeRegistry.disableCastClassExceptionCheck(mockValue.addr)

        return listOf(
            MethodResult(
                mockValue,
                hardConstraints = additionalConstraint.asHardConstraint(),
                memoryUpdates = if (isInternalMock) MemoryUpdate() else mockMethodResult.memoryUpdates
            )
        )
    }

    private fun TraversalContext.staticInvoke(invokeExpr: JStaticInvokeExpr): List<MethodResult> {
        val parameters = resolveParameters(invokeExpr.args, invokeExpr.method.parameterTypes)
        val result = mockMakeSymbolic(invokeExpr) ?: mockStaticMethod(invokeExpr.method, parameters)

        if (result != null) return result

        val method = invokeExpr.retrieveMethod()
        val invocation = Invocation(null, method, parameters, InvocationTarget(null, method))
        return commonInvokePart(invocation)
    }

    /**
     * Identifies different invocation targets by finding all overloads of invoked method.
     * Each target defines/reduces object type to set of concrete (not abstract, not interface)
     * classes with particular method implementation.
     */
    private fun TraversalContext.virtualAndInterfaceInvoke(
        base: Value,
        methodRef: SootMethodRef,
        parameters: List<Value>
    ): List<MethodResult> {
        val instance = resolve(base)
        if (instance !is ReferenceValue) error("We cannot run $methodRef on $instance")

        nullPointerExceptionCheck(instance.addr)

        if (instance.isNullObject()) return emptyList() // Nothing to call

        val method = methodRef.resolve()
        val resolvedParameters = resolveParameters(parameters, method.parameterTypes)

        val invocation = Invocation(instance, method, resolvedParameters) {
            when (instance) {
                is ObjectValue -> findInvocationTargets(instance, methodRef.subSignature.string)
                is ArrayValue -> listOf(InvocationTarget(instance, method))
            }
        }
        return commonInvokePart(invocation)
    }

    /**
     * Returns invocation targets for particular method implementation.
     *
     * Note: for some well known classes returns hardcoded choices.
     */
    private fun findInvocationTargets(
        instance: ObjectValue,
        methodSubSignature: String
    ): List<InvocationTarget> {
        val visitor = solver.rewriter.axiomInstantiationVisitor
        val simplifiedAddr = instance.addr.accept(visitor)
        // UtIsExpression for object with address the same as instance.addr
        val instanceOfConstraint = solver.assertions.singleOrNull {
            it is UtIsExpression && it.addr == simplifiedAddr
        } as? UtIsExpression
        // if we have UtIsExpression constraint for [instance], then find invocation targets
        // for possibleTypes from this constraints, instead of the type maintained by solver.

        // While using simplifications with RewritingVisitor, assertions can maintain types
        // for objects (especially objects with type equals to type parameter of generic)
        // better than engine.
        val types = instanceOfConstraint?.typeStorage?.possibleConcreteTypes ?: instance.possibleConcreteTypes

        val allPossibleConcreteTypes = typeResolver
            .constructTypeStorage(instance.type, useConcreteType = false)
            .possibleConcreteTypes

        val methodInvocationTargets = findLibraryTargets(instance.type, methodSubSignature)?.takeIf {
            // we have no specified types, so we can take only library targets (if present) for optimization purposes
            types.size == allPossibleConcreteTypes.size
        } ?: findMethodInvocationTargets(types, methodSubSignature)

        return methodInvocationTargets
            .map { (method, implementationClass, possibleTypes) ->
                val typeStorage = typeResolver.constructTypeStorage(implementationClass, possibleTypes)
                val mockInfo = memory.mockInfoByAddr(instance.addr)
                val mockedObject = mockInfo?.let {
                    // TODO rewrite to fix JIRA:1611
                    val type = Scene.v().getSootClass(mockInfo.classId.name).type
                    val ancestorTypes = typeResolver.findOrConstructAncestorsIncludingTypes(type)
                    val updatedMockInfo = if (implementationClass in ancestorTypes) {
                        it
                    } else {
                        it.copyWithClassId(classId = implementationClass.id)
                    }
                    mocker.mock(implementationClass, updatedMockInfo)
                }

                if (mockedObject == null) {
                    // Above we might get implementationClass that has to be substituted.
                    // For example, for a call "Collection.size()" such classes will be produced.
                    val wrapperOrInstance = wrapper(implementationClass, instance.addr)
                        ?: instance.copy(typeStorage = typeStorage)

                    val typeConstraint = typeRegistry.typeConstraint(instance.addr, wrapperOrInstance.typeStorage)
                    val constraints = setOf(typeConstraint.isOrNullConstraint())

                    // TODO add memory updated for types JIRA:1523

                    InvocationTarget(wrapperOrInstance, method, constraints)
                } else {
                    val typeConstraint = typeRegistry.typeConstraint(mockedObject.addr, mockedObject.typeStorage)
                    val constraints = setOf(typeConstraint.isOrNullConstraint())

                    // TODO add memory updated for types JIRA:1523
                    // TODO isMock????
                    InvocationTarget(mockedObject, method, constraints)
                }
            }
    }

    private fun findLibraryTargets(type: RefType, methodSubSignature: String): List<MethodInvocationTarget>? {
        val libraryTargets = libraryTargets[type.className] ?: return null
        return libraryTargets.mapNotNull { className ->
            val implementationClass = Scene.v().getSootClass(className)
            val method = implementationClass.findMethodOrNull(methodSubSignature)
            method?.let {
                MethodInvocationTarget(method, implementationClass.type, listOf(implementationClass.type))
            }
        }
    }

    /**
     * Returns sorted list of particular method implementations (invocation targets).
     */
    private fun findMethodInvocationTargets(
        concretePossibleTypes: Set<Type>,
        methodSubSignature: String
    ): List<MethodInvocationTarget> {
        val implementationToClasses = concretePossibleTypes
            .filterIsInstance<RefType>()
            .groupBy { it.sootClass.findMethodOrNull(methodSubSignature)?.declaringClass }
            .filterValues { it.appropriateClasses().isNotEmpty() }

        val targets = mutableListOf<MethodInvocationTarget>()
        for ((sootClass, types) in implementationToClasses) {
            if (sootClass != null) {
                targets += MethodInvocationTarget(sootClass.getMethod(methodSubSignature), sootClass.type, types)
            }
        }

        // do some hopeless sorting
        return targets
            .asSequence()
            .sortedByDescending { typeRegistry.findRating(it.implementationClass) }
            .take(10)
            .sortedByDescending { it.possibleTypes.size }
            .sortedBy { it.method.isNative }
            .take(5)
            .sortedByDescending { typeRegistry.findRating(it.implementationClass) }
            .toList()
    }

    private fun TraversalContext.specialInvoke(invokeExpr: JSpecialInvokeExpr): List<MethodResult> {
        val instance = resolve(invokeExpr.base)
        if (instance !is ReferenceValue) error("We cannot run ${invokeExpr.methodRef} on $instance")

        nullPointerExceptionCheck(instance.addr)

        if (instance.isNullObject()) return emptyList() // Nothing to call

        val method = invokeExpr.retrieveMethod()
        val parameters = resolveParameters(invokeExpr.args, method.parameterTypes)
        val invocation = Invocation(instance, method, parameters, InvocationTarget(instance, method))

        // Calls with super syntax are represented by invokeSpecial instruction, but we don't support them in wrappers
        // TODO: https://github.com/UnitTestBot/UTBotJava/issues/819 -- Support super calls in inherited wrappers
        return commonInvokePart(invocation)
    }

    private fun TraversalContext.dynamicInvoke(invokeExpr: JDynamicInvokeExpr): List<MethodResult> {
        workaround(HACK) {
            // The engine does not yet support JDynamicInvokeExpr, so switch to concrete execution if we encounter it
            offerState(environment.state.withLabel(StateLabel.CONCRETE))
            queuedSymbolicStateUpdates += UtFalse.asHardConstraint()
            return emptyList()
        }
    }

    /**
     * Runs common invocation part for object wrapper or object instance.
     *
     * Returns results of native calls cause other calls push changes directly to path selector.
     */
    private fun TraversalContext.commonInvokePart(invocation: Invocation): List<MethodResult> {
        /**
         * First, check if there is override for the invocation itself, not for the targets.
         *
         * Note that calls with super syntax are represented by invokeSpecial instruction, but we don't support them in wrappers,
         * so here we resolve [invocation] to the inherited method invocation if it's something like:
         *
         * ```java
         * public class InheritedWrapper extends BaseWrapper {
         *     public void add(Object o) {
         *         // some stuff
         *         super.add(o); // this resolves to InheritedWrapper::add instead of BaseWrapper::add
         *     }
         * }
         * ```
         *
         * TODO: https://github.com/UnitTestBot/UTBotJava/issues/819 -- Support super calls in inherited wrappers
         */
        val artificialMethodOverride = overrideInvocation(invocation, target = null)

        // If so, return the result of the override
        if (artificialMethodOverride.success) {
            if (artificialMethodOverride.results.size > 1) {
                environment.state.definitelyFork()
            }

            return mutableListOf<MethodResult>().apply {
                for (result in artificialMethodOverride.results) {
                    when (result) {
                        is MethodResult -> add(result)
                        is GraphResult -> pushToPathSelector(
                            result.graph,
                            invocation.instance,
                            invocation.parameters,
                            result.constraints,
                            isLibraryMethod = true
                        )
                    }
                }
            }
        }

        // If there is no such invocation, use the generator to produce invocation targets
        val targets = invocation.generator.invoke()

        // Take all the targets and run them, at least one target must exist
        require(targets.isNotEmpty()) { "No targets for $invocation" }

        // Note that sometimes invocation on the particular targets should be overridden as well.
        // For example, Collection.size will produce two targets (ArrayList and HashSet)
        // that will override the invocation.
        val overrideResults = targets.map { it to overrideInvocation(invocation, it) }

        if (overrideResults.sumOf { (_, overriddenResult) -> overriddenResult.results.size } > 1) {
            environment.state.definitelyFork()
        }

        // Separate targets for which invocation should be overridden
        // from the targets that should be processed regularly.
        val (overridden, original) = overrideResults.partition { it.second.success }

        val overriddenResults = overridden
            .flatMap { (target, overriddenResult) ->
                mutableListOf<MethodResult>().apply {
                    for (result in overriddenResult.results) {
                        when (result) {
                            is MethodResult -> add(result)
                            is GraphResult -> pushToPathSelector(
                                result.graph,
                                // take the instance from the target
                                target.instance,
                                invocation.parameters,
                                // It is important to add constraints for the target as well, because
                                // those constraints contain information about the type of the
                                // instance from the target
                                target.constraints + result.constraints,
                                // Since we override methods of the classes from the standard library
                                isLibraryMethod = true
                            )
                        }
                    }
                }
            }

        // Add results for the targets that should be processed without override
        val originResults = original.flatMap { (target: InvocationTarget, _) ->
            invoke(target, invocation.parameters)
        }

        // Return their concatenation
        return overriddenResults + originResults
    }

    private fun TraversalContext.invoke(
        target: InvocationTarget,
        parameters: List<SymbolicValue>
    ): List<MethodResult> = with(target.method) {
        val substitutedMethod = typeRegistry.findSubstitutionOrNull(this)

        if (isNative && substitutedMethod == null) return processNativeMethod(target)

        // If we face UtMock.assume call, we should continue only with the branch
        // where the predicate from the parameters is equal true
        when {
            isUtMockAssume || isUtMockAssumeOrExecuteConcretely -> {
                val param = UtCastExpression(parameters.single() as PrimitiveValue, BooleanType.v())

                val assumptionStmt = mkEq(param, UtTrue)
                val (hardConstraints, assumptions) = if (isUtMockAssume) {
                    // For UtMock.assume we must add the assumeStmt to the hard constraints
                    setOf(assumptionStmt) to emptySet()
                } else {
                    // For assumeOrExecuteConcretely we must add the statement to the assumptions.
                    // It is required to have opportunity to remove it later in case of unsat state
                    // because of it and execute the state concretely.
                    emptySet<UtBoolExpression>() to setOf(assumptionStmt)
                }

                val symbolicStateUpdate = SymbolicStateUpdate(
                    hardConstraints = hardConstraints.asHardConstraint(),
                    assumptions = assumptions.asAssumption()
                )

                val stateToContinue = environment.state.updateQueued(
                    globalGraph.succ(environment.state.stmt),
                    symbolicStateUpdate
                )
                offerState(stateToContinue)

                // we already pushed state with the fulfilled predicate, so we can just drop our branch here by
                // adding UtFalse to the constraints.
                queuedSymbolicStateUpdates += UtFalse.asHardConstraint()
                emptyList()
            }
            declaringClass == utOverrideMockClass -> utOverrideMockInvoke(target, parameters)
            declaringClass == utLogicMockClass -> utLogicMockInvoke(target, parameters)
            declaringClass == utArrayMockClass -> utArrayMockInvoke(target, parameters)
            else -> {
                val graph = substitutedMethod?.jimpleBody()?.graph() ?: jimpleBody().graph()
                pushToPathSelector(graph, target.instance, parameters, target.constraints, isLibraryMethod)
                emptyList()
            }
        }
    }

    private fun TraversalContext.utOverrideMockInvoke(target: InvocationTarget, parameters: List<SymbolicValue>): List<MethodResult> {
        when (target.method.name) {
            utOverrideMockAlreadyVisitedMethodName -> {
                return listOf(MethodResult(memory.isVisited(parameters[0].addr).toBoolValue()))
            }
            utOverrideMockVisitMethodName -> {
                return listOf(
                    MethodResult(
                        voidValue,
                        memoryUpdates = MemoryUpdate(visitedValues = persistentListOf(parameters[0].addr))
                    )
                )
            }
            utOverrideMockDoesntThrowMethodName -> {
                val stateToContinue = environment.state.updateQueued(
                    globalGraph.succ(environment.state.stmt),
                    doesntThrow = true
                )
                offerState(stateToContinue)
                queuedSymbolicStateUpdates += UtFalse.asHardConstraint()
                return emptyList()
            }
            utOverrideMockParameterMethodName -> {
                when (val param = parameters.single() as ReferenceValue) {
                    is ObjectValue -> {
                        val addr = param.addr.toIntValue()
                        val stateToContinue = environment.state.updateQueued(
                            globalGraph.succ(environment.state.stmt),
                            SymbolicStateUpdate(
                                hardConstraints = Le(addr, nullObjectAddr.toIntValue()).asHardConstraint()
                            )
                        )
                        offerState(stateToContinue)
                    }
                    is ArrayValue -> {
                        val addr = param.addr
                        val descriptor =
                            MemoryChunkDescriptor(
                                typeRegistry.arrayChunkId(OBJECT_TYPE.arrayType),
                                OBJECT_TYPE.arrayType,
                                OBJECT_TYPE
                            )

                        val update = MemoryUpdate(
                            persistentListOf(
                                simplifiedNamedStore(
                                    descriptor,
                                    addr,
                                    UtArrayApplyForAll(memory.findArray(descriptor).select(addr)) { array, i ->
                                        Le(array.select(i.expr).toIntValue(), nullObjectAddr.toIntValue())
                                    }
                                )
                            )
                        )
                        val stateToContinue = environment.state.updateQueued(
                            edge = globalGraph.succ(environment.state.stmt),
                            SymbolicStateUpdate(
                                hardConstraints = Le(addr.toIntValue(), nullObjectAddr.toIntValue()).asHardConstraint(),
                                memoryUpdates = update
                            )
                        )
                        offerState(stateToContinue)
                    }
                }


                // we already pushed state with the fulfilled predicate, so we can just drop our branch here by
                // adding UtFalse to the constraints.
                queuedSymbolicStateUpdates += UtFalse.asHardConstraint()
                return emptyList()
            }
            utOverrideMockExecuteConcretelyMethodName -> {
                offerState(environment.state.withLabel(StateLabel.CONCRETE))
                queuedSymbolicStateUpdates += UtFalse.asHardConstraint()
                return emptyList()
            }
            else -> unreachableBranch("unknown method ${target.method.signature} in ${UtOverrideMock::class.qualifiedName}")
        }
    }

    private fun utArrayMockInvoke(target: InvocationTarget, parameters: List<SymbolicValue>): List<MethodResult> {
        when (target.method.name) {
            utArrayMockArraycopyMethodName -> {
                val src = parameters[0] as ArrayValue
                val dst = parameters[2] as ArrayValue
                val copyValue = UtArraySetRange(
                    selectArrayExpressionFromMemory(dst),
                    parameters[3] as PrimitiveValue,
                    selectArrayExpressionFromMemory(src),
                    parameters[1] as PrimitiveValue,
                    parameters[4] as PrimitiveValue
                )
                return listOf(
                    MethodResult(
                        voidValue,
                        memoryUpdates = arrayUpdateWithValue(dst.addr, dst.type, copyValue)
                    )
                )
            }
            utArrayMockCopyOfMethodName -> {
                val src = parameters[0] as ArrayValue
                val length = parameters[1] as PrimitiveValue
                val arrayType = target.method.returnType as ArrayType
                val newArray = createNewArray(length, arrayType, arrayType.elementType)
                return listOf(
                    MethodResult(
                        newArray,
                        memoryUpdates = arrayUpdateWithValue(newArray.addr, arrayType, selectArrayExpressionFromMemory(src))
                    )
                )
            }
            else -> unreachableBranch("unknown method ${target.method.signature} for ${UtArrayMock::class.qualifiedName}")
        }
    }

    private fun utLogicMockInvoke(target: InvocationTarget, parameters: List<SymbolicValue>): List<MethodResult> {
        when (target.method.name) {
            utLogicMockLessMethodName -> {
                val a = parameters[0] as PrimitiveValue
                val b = parameters[1] as PrimitiveValue
                return listOf(MethodResult(Lt(a, b).toBoolValue()))
            }
            utLogicMockIteMethodName -> {
                var isPrimitive = false
                val thenExpr = parameters[1].let {
                    if (it is PrimitiveValue) {
                        isPrimitive = true
                        it.expr
                    } else {
                        it.addr.internal
                    }
                }
                val elseExpr = parameters[2].let {
                    if (it is PrimitiveValue) {
                        isPrimitive = true
                        it.expr
                    } else {
                        it.addr.internal
                    }
                }
                val condition = (parameters[0] as PrimitiveValue).expr as UtBoolExpression
                val iteExpr = UtIteExpression(condition, thenExpr, elseExpr)
                val result = if (isPrimitive) {
                    PrimitiveValue(target.method.returnType, iteExpr)
                } else {
                    ObjectValue(
                        typeResolver.constructTypeStorage(target.method.returnType, useConcreteType = false),
                        UtAddrExpression(iteExpr)
                    )
                }
                return listOf(MethodResult(result))
            }
            else -> unreachableBranch("unknown method ${target.method.signature} in ${UtLogicMock::class.qualifiedName}")
        }
    }

    /**
     * Tries to override method. Override can be object wrapper or similar implementation.
     *
     * Proceeds overridden method as non-library.
     */
    private fun TraversalContext.overrideInvocation(invocation: Invocation, target: InvocationTarget?): OverrideResult {
        // If we try to override invocation itself, the target is null, and we have to process
        // the instance from the invocation, otherwise take the one from the target
        val instance = if (target == null) invocation.instance else target.instance
        val subSignature = invocation.method.subSignature

        if (subSignature == "java.lang.Class getClass()") {
            return when (instance) {
                is ReferenceValue -> {
                    val type = instance.type
                    val createClassRef = if (type is RefLikeType) {
                        typeRegistry.createClassRef(type.baseType, type.numDimensions)
                    } else {
                        error("Can't get class name for $type")
                    }
                    OverrideResult(success = true, createClassRef)
                }
                null -> unreachableBranch("Static getClass call: $invocation")
            }
        }

        val instanceAsWrapperOrNull = instance?.asWrapperOrNull

        if (instanceAsWrapperOrNull is UtMockWrapper && subSignature == HASHCODE_SIGNATURE) {
            val result = MethodResult(mkBVConst("hashcode${hashcodeCounter++}", UtIntSort).toIntValue())
            return OverrideResult(success = true, result)
        }

        if (instanceAsWrapperOrNull is UtMockWrapper && subSignature == EQUALS_SIGNATURE) {
            val result = MethodResult(mkBoolConst("equals${equalsCounter++}").toBoolValue())
            return OverrideResult(success = true, result)
        }

        // we cannot mock synthetic methods and methods that have private or protected arguments
        val impossibleToMock =
            invocation.method.isSynthetic || invocation.method.isProtected || parametersContainPrivateAndProtectedTypes(
                invocation.method
            )

        if (instanceAsWrapperOrNull is UtMockWrapper && impossibleToMock) {
            // TODO temporary we return unbounded symbolic variable with a wrong name.
            // TODO Probably it'll lead us to the path divergence
            workaround(HACK) {
                val result = unboundedVariable("unbounded", invocation.method)
                return OverrideResult(success = true, result)
            }
        }

        if (instance is ArrayValue && invocation.method.name == "clone") {
            return OverrideResult(success = true, cloneArray(instance))
        }

        instanceAsWrapperOrNull?.run {
            val results = invoke(instance as ObjectValue, invocation.method, invocation.parameters)
            if (results.isEmpty()) {
                // Drop the branch and switch to concrete execution
                offerState(environment.state.withLabel(StateLabel.CONCRETE))
                queuedSymbolicStateUpdates += UtFalse.asHardConstraint()
            }
            return OverrideResult(success = true, results)
        }

        return OverrideResult(success = false)
    }

    private fun cloneArray(array: ArrayValue): MethodResult {
        val addr = findNewAddr()

        val type = array.type
        val elementType = type.elementType
        val chunkId = typeRegistry.arrayChunkId(type)
        val descriptor = MemoryChunkDescriptor(chunkId, type, elementType)
        val arrays = memory.findArray(descriptor)

        val arrayLength = memory.findArrayLength(array.addr)
        val cloneLength = memory.findArrayLength(addr)

        val constraints = setOf(
            mkEq(typeRegistry.symTypeId(array.addr), typeRegistry.symTypeId(addr)),
            mkEq(typeRegistry.symNumDimensions(array.addr), typeRegistry.symNumDimensions(addr)),
            mkEq(cloneLength, arrayLength)
        ) + (0 until softMaxArraySize).map {
            val index = mkInt(it)
            mkEq(
                arrays.select(array.addr, index).toPrimitiveValue(elementType),
                arrays.select(addr, index).toPrimitiveValue(elementType)
            )
        }

//      TODO: add preferred cex to:  val softConstraints = preferredConstraints(clone)

        val memoryUpdate = MemoryUpdate(touchedChunkDescriptors = persistentSetOf(descriptor))

        val clone = ArrayValue(TypeStorage(array.type), addr)
        return MethodResult(clone, constraints.asHardConstraint(), memoryUpdates = memoryUpdate)
    }

    // For now, we just create unbounded symbolic variable as a result.
    private fun processNativeMethod(target: InvocationTarget): List<MethodResult> =
        listOf(unboundedVariable(name = "nativeConst", target.method))

    private fun unboundedVariable(name: String, method: SootMethod): MethodResult {
        val value = when (val returnType = method.returnType) {
            is RefType -> createObject(findNewAddr(), returnType, useConcreteType = true)
            is ArrayType -> createArray(findNewAddr(), returnType, useConcreteType = true)
            else -> createConst(returnType, "$name${unboundedConstCounter++}")
        }

        return MethodResult(value)
    }

    fun SootClass.findMethodOrNull(subSignature: String): SootMethod? {
        adjustLevel(SootClass.SIGNATURES)

        val classes = generateSequence(this) { it.superClassOrNull() }
        val interfaces = generateSequence(this) { it.superClassOrNull() }.flatMap { sootClass ->
            sootClass.interfaces.flatMap { hierarchy.ancestors(it.id) }
        }.distinct()
        return (classes + interfaces)
            .filter {
                it.adjustLevel(SootClass.SIGNATURES)
                it.declaresMethod(subSignature)
            }
            .mapNotNull { it.getMethod(subSignature) }
            .firstOrNull { it.canRetrieveBody() || it.isNative }
    }

    private fun TraversalContext.pushToPathSelector(
        graph: ExceptionalUnitGraph,
        caller: ReferenceValue?,
        callParameters: List<SymbolicValue>,
        constraints: Set<UtBoolExpression> = emptySet(),
        isLibraryMethod: Boolean = false
    ) {
        globalGraph.join(environment.state.stmt, graph, !isLibraryMethod)
        val parametersWithThis = listOfNotNull(caller) + callParameters
        offerState(
            environment.state.push(
                graph.head,
                inputArguments = ArrayDeque(parametersWithThis),
                queuedSymbolicStateUpdates + constraints.asHardConstraint(),
                graph.body.method
            )
        )
    }

    private fun ExecutionState.updateQueued(
        edge: Edge,
        update: SymbolicStateUpdate = SymbolicStateUpdate(),
        doesntThrow: Boolean = false
    ) = this.update(
        edge,
        queuedSymbolicStateUpdates + update,
        doesntThrow
    )

    private fun TraversalContext.resolveIfCondition(cond: BinopExpr): ResolvedCondition {
        // We add cond.op.type for null values only. If we have condition like "null == r1"
        // we'll have ObjectInstance(r1::type) and ObjectInstance(r1::type) for now
        // For non-null values type is ignored.
        val lhs = resolve(cond.op1, cond.op2.type)
        val rhs = resolve(cond.op2, cond.op1.type)
        return when {
            lhs.isNullObject() || rhs.isNullObject() -> {
                val eq = addrEq(lhs.addr, rhs.addr)
                if (cond is NeExpr) ResolvedCondition(mkNot(eq)) else ResolvedCondition(eq)
            }
            lhs is ReferenceValue && rhs is ReferenceValue -> {
                ResolvedCondition(compareReferenceValues(lhs, rhs, cond is NeExpr))
            }
            else -> {
                val expr = resolve(cond).asPrimitiveValueOrError as UtBoolExpression
                val memoryUpdates = collectSymbolicStateUpdates(expr)
                ResolvedCondition(
                    expr,
                    constructSoftConstraintsForCondition(cond),
                    symbolicStateUpdates = memoryUpdates
                )
            }
        }
    }

    /**
     * Tries to collect all memory updates from nested [UtInstanceOfExpression]s in the [expr].
     * Resolves only basic cases: `not`, `and`, `z0 == 0`, `z0 == 1`, `z0 != 0`, `z0 != 1`.
     *
     * It's impossible now to make this function complete, because our [Memory] can't deal with some expressions
     * (e.g. [UtOrBoolExpression] consisted of [UtInstanceOfExpression]s).
     */
    private fun collectSymbolicStateUpdates(expr: UtBoolExpression): SymbolicStateUpdateForResolvedCondition {
        return when (expr) {
            is UtInstanceOfExpression -> { // for now only this type of expression produces deferred updates
                val onlyMemoryUpdates = expr.symbolicStateUpdate.copy(
                    hardConstraints = emptyHardConstraint(),
                    softConstraints = emptySoftConstraint(),
                    assumptions = emptyAssumption()
                )
                SymbolicStateUpdateForResolvedCondition(onlyMemoryUpdates)
            }
            is UtAndBoolExpression -> {
                expr.exprs.fold(SymbolicStateUpdateForResolvedCondition()) { updates, nestedExpr ->
                    val nextPosUpdates = updates.positiveCase + collectSymbolicStateUpdates(nestedExpr).positiveCase
                    val nextNegUpdates = updates.negativeCase + collectSymbolicStateUpdates(nestedExpr).negativeCase
                    SymbolicStateUpdateForResolvedCondition(nextPosUpdates, nextNegUpdates)
                }
            }
            // TODO: JIRA:1667 -- Engine can't apply memory updates for some expressions
            is UtOrBoolExpression -> SymbolicStateUpdateForResolvedCondition() // Which clause should we apply?
            is NotBoolExpression -> collectSymbolicStateUpdates(expr.expr).swap()
            is UtBoolOpExpression -> {
                // Java `instanceof` in `if` translates to UtBoolOpExpression.
                // More precisely, something like this will be generated:
                //      ...
                //      z0: bool = obj instanceof A
                //      if z0 == 0 goto ...
                //      ...
                // while traversing the condition, `BinopExpr` resolves to `UtBoolOpExpression` with the left part
                // equals to `UtBoolExpession` (usually `UtInstanceOfExpression`), because it is stored in local `z0`
                // and the right part equals to UtBvLiteral with the integer constant.
                //
                // If something more complex is written in the original `if`, these matches could not success.
                // TODO: JIRA:1667
                val lhs = expr.left.expr as? UtBoolExpression
                    ?: return SymbolicStateUpdateForResolvedCondition()
                val rhsAsIntValue = (expr.right.expr as? UtBvLiteral)?.value?.toInt()
                    ?: return SymbolicStateUpdateForResolvedCondition()
                val updates = collectSymbolicStateUpdates(lhs)

                when (expr.operator) {
                    is Eq -> {
                        when (rhsAsIntValue) {
                            1 -> updates // z0 == 1
                            0 -> updates.swap() // z0 == 0
                            else -> SymbolicStateUpdateForResolvedCondition()
                        }
                    }
                    is Ne -> {
                        when (rhsAsIntValue) {
                            1 -> updates.swap() // z0 != 1
                            0 -> updates // z0 != 0
                            else -> SymbolicStateUpdateForResolvedCondition()
                        }
                    }
                    else -> SymbolicStateUpdateForResolvedCondition()
                }
            }
            // TODO: JIRA:1667 -- Engine can't apply memory updates for some expressions
            else -> SymbolicStateUpdateForResolvedCondition()
        }
    }

    private fun TraversalContext.constructSoftConstraintsForCondition(cond: BinopExpr): SoftConstraintsForResolvedCondition {
        var positiveCaseConstraint: UtBoolExpression? = null
        var negativeCaseConstraint: UtBoolExpression? = null

        val left = resolve(cond.op1, cond.op2.type)
        val right = resolve(cond.op2, cond.op1.type)

        if (left !is PrimitiveValue || right !is PrimitiveValue) return SoftConstraintsForResolvedCondition()

        val one = 1.toPrimitiveValue()

        when (cond) {
            is JLtExpr -> {
                positiveCaseConstraint = mkEq(left, Sub(right, one).toIntValue())
                negativeCaseConstraint = mkEq(left, right)
            }
            is JLeExpr -> {
                positiveCaseConstraint = mkEq(left, right)
                negativeCaseConstraint = mkEq(Sub(left, one).toIntValue(), right)
            }
            is JGeExpr -> {
                positiveCaseConstraint = mkEq(left, right)
                negativeCaseConstraint = mkEq(left, Sub(right, one).toIntValue())
            }
            is JGtExpr -> {
                positiveCaseConstraint = mkEq(Sub(left, one).toIntValue(), right)
                negativeCaseConstraint = mkEq(left, right)

            }
            else -> Unit
        }

        return SoftConstraintsForResolvedCondition(positiveCaseConstraint, negativeCaseConstraint)
    }

    /**
     * Compares two objects with types, lhs :: lhsType and rhs :: rhsType.
     *
     * Does it by checking types equality, then addresses equality.
     *
     * Notes:
     * - Content (assertions on fields) comparison is not necessary cause solver finds on its own and provides
     * different object addresses in such case
     * - We do not compare null addresses here, it happens in resolveIfCondition
     *
     * @see Traverser.resolveIfCondition
     */
    private fun compareReferenceValues(
        lhs: ReferenceValue,
        rhs: ReferenceValue,
        negate: Boolean
    ): UtBoolExpression {
        val eq = addrEq(lhs.addr, rhs.addr)
        return if (negate) mkNot(eq) else eq
    }

    private fun TraversalContext.nullPointerExceptionCheck(addr: UtAddrExpression) {
        val canBeNull = addrEq(addr, nullObjectAddr)
        val canNotBeNull = mkNot(canBeNull)
        val notMarked = mkEq(memory.isSpeculativelyNotNull(addr), mkFalse())
        val notMarkedAndNull = mkAnd(notMarked, canBeNull)

        if (environment.method.checkForNPE(environment.state.executionStack.size)) {
            implicitlyThrowException(NullPointerException(), setOf(notMarkedAndNull))
        }

        queuedSymbolicStateUpdates += canNotBeNull.asHardConstraint()
    }

    private fun TraversalContext.divisionByZeroCheck(denom: PrimitiveValue) {
        val equalsToZero = Eq(denom, 0)
        implicitlyThrowException(ArithmeticException("/ by zero"), setOf(equalsToZero))
        queuedSymbolicStateUpdates += mkNot(equalsToZero).asHardConstraint()
    }

    // Use cast to Int and cmp with min/max for Byte and Short.
    // Formulae for Int and Long does not work for lower integers because of sign_extend ops in SMT.
    private fun lowerIntMulOverflowCheck(
        left: PrimitiveValue,
        right: PrimitiveValue,
        minValue: Int,
        maxValue: Int,
    ): UtBoolExpression {
        val castedLeft = UtCastExpression(left, UtIntSort.type).toIntValue()
        val castedRight = UtCastExpression(right, UtIntSort.type).toIntValue()

        val res = Mul(castedLeft, castedRight).toIntValue()

        val lessThanMinValue = Lt(
            res,
            minValue.toPrimitiveValue(),
        ).toBoolValue()

        val greaterThanMaxValue = Gt(
            res,
            maxValue.toPrimitiveValue(),
        ).toBoolValue()

        return Ne(
            Or(
                lessThanMinValue,
                greaterThanMaxValue,
            ).toBoolValue(),
            0.toPrimitiveValue()
        )
    }


    // Z3 internal operator for MulNoOverflow is currently bugged.
    // Use formulae from Math.mulExact to detect mul overflow for Int and Long.
    private fun higherIntMulOverflowCheck(
        left: PrimitiveValue,
        right: PrimitiveValue,
        bits: Int,
        minValue: Long,
        toValue: (it: UtExpression) -> PrimitiveValue,
    ): UtBoolExpression {
        // https://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/lang/Math.java#l882
        val leftValue = toValue(left.expr)
        val rightValue = toValue(right.expr)
        val res = toValue(Mul(leftValue, rightValue))

        // extract absolute values
        // https://www.geeksforgeeks.org/compute-the-integer-absolute-value-abs-without-branching/
        val leftAbsMask = toValue(Ushr(leftValue, (bits - 1).toPrimitiveValue()))
        val leftAbs = toValue(
            Xor(
                toValue(Add(leftAbsMask, leftValue)),
                leftAbsMask
            )
        )
        val rightAbsMask = toValue(Ushr(rightValue, (bits - 1).toPrimitiveValue()))
        val rightAbs = toValue(
            Xor(
                toValue(Add(rightAbsMask, rightValue)),
                rightAbsMask
            )
        )

        // (((ax | ay) >>> 31 != 0))
        val bigEnough = Ne(
            toValue(
                Ushr(
                    toValue(Or(leftAbs, rightAbs)),
                    (bits ushr 1 - 1).toPrimitiveValue()
                )
            ),
            0.toPrimitiveValue()
        )

        // (((y != 0) && (r / y != x))
        val incorrectDiv = And(
            Ne(rightValue, 0.toPrimitiveValue()).toBoolValue(),
            Ne(toValue(Div(res, rightValue)), leftValue).toBoolValue(),
        )

        // (x == Long.MIN_VALUE && y == -1))
        val minValueEdgeCase = And(
            Eq(leftValue, minValue.toPrimitiveValue()).toBoolValue(),
            Eq(rightValue, (-1).toPrimitiveValue()).toBoolValue()
        )

        return Ne(
            And(
                bigEnough.toBoolValue(),
                Or(
                    incorrectDiv.toBoolValue(),
                    minValueEdgeCase.toBoolValue(),
                ).toBoolValue()
            ).toBoolValue(),
            0.toPrimitiveValue()
        )
    }

    private fun TraversalContext.intOverflowCheck(op: BinopExpr, leftRaw: PrimitiveValue, rightRaw: PrimitiveValue) {
        // cast to the bigger type
        val sort = simpleMaxSort(leftRaw, rightRaw) as UtPrimitiveSort
        val left = leftRaw.expr.toPrimitiveValue(sort.type)
        val right = rightRaw.expr.toPrimitiveValue(sort.type)

        val overflow = when (op) {
            is JAddExpr -> {
                mkNot(UtAddNoOverflowExpression(left.expr, right.expr))
            }
            is JSubExpr -> {
                mkNot(UtSubNoOverflowExpression(left.expr, right.expr))
            }
            is JMulExpr -> when (sort.type) {
                is ByteType -> lowerIntMulOverflowCheck(left, right, Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt())
                is ShortType -> lowerIntMulOverflowCheck(left, right, Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                is IntType -> higherIntMulOverflowCheck(left, right, Int.SIZE_BITS, Int.MIN_VALUE.toLong()) { it: UtExpression -> it.toIntValue() }
                is LongType -> higherIntMulOverflowCheck(left, right, Long.SIZE_BITS, Long.MIN_VALUE) { it: UtExpression -> it.toLongValue() }
                else -> null
            }
            else -> null
        }

        if (overflow != null) {
            implicitlyThrowException(ArithmeticException("${left.type} ${op.symbol} overflow"), setOf(overflow))
            queuedSymbolicStateUpdates += mkNot(overflow).asHardConstraint()
        }
    }

    private fun TraversalContext.indexOutOfBoundsChecks(index: PrimitiveValue, length: PrimitiveValue) {
        val ltZero = Lt(index, 0)
        implicitlyThrowException(IndexOutOfBoundsException("Less than zero"), setOf(ltZero))

        val geLength = Ge(index, length)
        implicitlyThrowException(IndexOutOfBoundsException("Greater or equal than length"), setOf(geLength))

        queuedSymbolicStateUpdates += mkNot(ltZero).asHardConstraint()
        queuedSymbolicStateUpdates += mkNot(geLength).asHardConstraint()
    }

    private fun TraversalContext.negativeArraySizeCheck(vararg sizes: PrimitiveValue) {
        val ltZero = mkOr(sizes.map { Lt(it, 0) })
        implicitlyThrowException(NegativeArraySizeException("Less than zero"), setOf(ltZero))
        queuedSymbolicStateUpdates += mkNot(ltZero).asHardConstraint()
    }

    /**
     * Checks for ClassCastException.
     *
     * Note: if we have the valueToCast.addr related to some parameter with addr p_0, and that parameter's type is a parameterizedType,
     * we ignore potential exception throwing if the typeAfterCast is one of the generics included in that type.
     */
    private fun TraversalContext.classCastExceptionCheck(valueToCast: ReferenceValue, typeAfterCast: Type) {
        val baseTypeAfterCast = if (typeAfterCast is ArrayType) typeAfterCast.baseType else typeAfterCast
        val addr = valueToCast.addr

        // Expected in the parameters baseType is an RefType because it is either an RefType itself or an array of RefType values
        if (baseTypeAfterCast is RefType) {
            // Find parameterized type for the object if it is a parameter of the method under test and it has generic type
            val newAddr = addr.accept(solver.rewriter) as UtAddrExpression
            val parameterizedType = when (newAddr.internal) {
                is UtArraySelectExpression -> parameterAddrToGenericType[findTheMostNestedAddr(newAddr.internal)]
                is UtBvConst -> parameterAddrToGenericType[newAddr]
                else -> null
            }

            if (parameterizedType != null) {
                // Find all generics used in the type of the parameter and it's superclasses
                // If we're trying to cast something related to the parameter and typeAfterCast is equal to one of the generic
                // types used in it, don't throw ClassCastException
                val genericTypes = generateSequence(parameterizedType) { it.ownerType as? ParameterizedType }
                    .flatMapTo(mutableSetOf()) { it.actualTypeArguments.map { arg -> arg.typeName } }

                if (baseTypeAfterCast.className in genericTypes) {
                    return
                }
            }
        }

        val inheritorsTypeStorage = typeResolver.constructTypeStorage(typeAfterCast, useConcreteType = false)
        val preferredTypesForCastException = valueToCast.possibleConcreteTypes.filterNot { it in inheritorsTypeStorage.possibleConcreteTypes }

        val isExpression = typeRegistry.typeConstraint(addr, inheritorsTypeStorage).isConstraint()
        val notIsExpression = mkNot(isExpression)

        val nullEqualityConstraint = addrEq(addr, nullObjectAddr)
        val notNull = mkNot(nullEqualityConstraint)

        val classCastExceptionAllowed = mkEq(UtTrue, typeRegistry.isClassCastExceptionAllowed(addr))

        implicitlyThrowException(
            ClassCastException("The object with type ${valueToCast.type} can not be casted to $typeAfterCast"),
            setOf(notIsExpression, notNull, classCastExceptionAllowed),
            setOf(constructConstraintForType(valueToCast, preferredTypesForCastException))
        )

        queuedSymbolicStateUpdates += mkOr(isExpression, nullEqualityConstraint).asHardConstraint()
    }

    private fun TraversalContext.implicitlyThrowException(
        exception: Exception,
        conditions: Set<UtBoolExpression>,
        softConditions: Set<UtBoolExpression> = emptySet()
    ) {
        if (environment.state.executionStack.last().doesntThrow) return

        val symException = implicitThrown(exception, findNewAddr(), environment.state.isInNestedMethod())
        if (!traverseCatchBlock(environment.state.stmt, symException, conditions)) {
            environment.state.expectUndefined()
            val nextState = environment.state.createExceptionState(
                symException,
                queuedSymbolicStateUpdates
                        + conditions.asHardConstraint()
                        + softConditions.asSoftConstraint()
            )
            globalGraph.registerImplicitEdge(nextState.lastEdge!!)
            offerState(nextState)
        }
    }


    private val symbolicStackTrace: String
        get() {
            val methods = environment.state.executionStack.mapNotNull { it.caller }
                .map { globalGraph.method(it) } + environment.method
            return methods.reversed().joinToString(separator = "\n") { method ->
                if (method.isDeclared) "$method" else method.subSignature
            }
        }

    private fun constructConstraintForType(value: ReferenceValue, possibleTypes: Collection<Type>): UtBoolExpression {
        val preferredTypes = typeResolver.findTopRatedTypes(possibleTypes, take = NUMBER_OF_PREFERRED_TYPES)
        val mostCommonType = preferredTypes.singleOrNull() ?: OBJECT_TYPE
        val typeStorage = typeResolver.constructTypeStorage(mostCommonType, preferredTypes)
        return typeRegistry.typeConstraint(value.addr, typeStorage).isOrNullConstraint()
    }

    /**
     * Adds soft default values for the initial values of all the arrays that exist in the program.
     *
     * Almost all of them can be found in the local memory, but there are three "common"
     * arrays that we need to add
     *
     *
     * @see Memory.initialArrays
     * @see Memory.softZeroArraysLengths
     * @see TypeRegistry.softEmptyTypes
     * @see TypeRegistry.softZeroNumDimensions
     */
    private fun addSoftDefaults() {
        memory.initialArrays.forEach { queuedSymbolicStateUpdates += UtMkTermArrayExpression(it).asHardConstraint() }
        queuedSymbolicStateUpdates += memory.softZeroArraysLengths().asHardConstraint()
        queuedSymbolicStateUpdates += typeRegistry.softZeroNumDimensions().asHardConstraint()
        queuedSymbolicStateUpdates += typeRegistry.softEmptyTypes().asHardConstraint()
    }

    /**
     * Takes queued [updates] at the end of static initializer processing, extracts information about
     * updated static fields and substitutes them with unbounded symbolic variables.
     *
     * @return updated memory updates.
     */
    private fun substituteStaticFieldsWithSymbolicVariables(
        declaringClass: SootClass,
        updates: MemoryUpdate
    ): MemoryUpdate {
        val declaringClassId = declaringClass.id

        val staticFieldsUpdates = updates.staticFieldsUpdates.toMutableList()
        val updatedFields = staticFieldsUpdates.mapTo(mutableSetOf()) { it.fieldId }
        val objectUpdates = mutableListOf<UtNamedStore>()

        // we assign unbounded symbolic variables for every non-final meaningful field of the class
        // fields from predefined library classes are excluded, because there are not meaningful
        typeResolver
            .findFields(declaringClass.type)
            .filter { !it.isFinal && it.fieldId in updatedFields && isStaticFieldMeaningful(it) }
            .forEach {
                // remove updates from clinit, because we'll replace those values
                // with new unbounded symbolic variable
                staticFieldsUpdates.removeAll { update -> update.fieldId == it.fieldId }

                val value = createConst(it.type, it.name)
                val valueToStore = if (value is ReferenceValue) {
                    value.addr
                } else {
                    (value as PrimitiveValue).expr
                }

                // we always know that this instance exists because we've just returned from its clinit method
                // in which we had to create such instance
                val staticObject = updates.staticInstanceStorage.getValue(declaringClassId)
                staticFieldsUpdates += StaticFieldMemoryUpdateInfo(it.fieldId, value)

                objectUpdates += objectUpdate(staticObject, it, valueToStore).stores
            }

        return updates.copy(
            stores = updates.stores.addAll(objectUpdates),
            staticFieldsUpdates = staticFieldsUpdates.toPersistentList(),
            classIdToClearStatics = declaringClassId
        )
    }

    private fun TraversalContext.processResult(symbolicResult: SymbolicResult) {
        val resolvedParameters = environment.state.parameters.map { it.value }

        //choose types that have biggest priority
        resolvedParameters
            .filterIsInstance<ReferenceValue>()
            .forEach { queuedSymbolicStateUpdates += constructConstraintForType(it, it.possibleConcreteTypes).asSoftConstraint() }

        val returnValue = (symbolicResult as? SymbolicSuccess)?.value as? ObjectValue
        if (returnValue != null) {
            queuedSymbolicStateUpdates += constructConstraintForType(returnValue, returnValue.possibleConcreteTypes).asSoftConstraint()
        }

        //fill arrays with default 0/null and other stuff
        addSoftDefaults()

        //deal with @NotNull annotation
        val isNotNullableResult = environment.method.returnValueHasNotNullAnnotation()
        if (symbolicResult is SymbolicSuccess && symbolicResult.value is ReferenceValue && isNotNullableResult) {
            queuedSymbolicStateUpdates += mkNot(mkEq(symbolicResult.value.addr, nullObjectAddr)).asHardConstraint()
        }

        val symbolicState = environment.state.symbolicState + queuedSymbolicStateUpdates
        val memory = symbolicState.memory
        val solver = symbolicState.solver

        //no need to respect soft constraints in NestedMethod
        val holder = solver.check(respectSoft = !environment.state.isInNestedMethod())

        if (holder !is UtSolverStatusSAT) {
            logger.trace { "processResult<${environment.method.signature}> UNSAT" }
            return
        }
        val methodResult = MethodResult(symbolicResult)

        //execution frame from level 2 or above
        if (environment.state.isInNestedMethod()) {
            // static fields substitution
            // TODO: JIRA:1610 -- better way of working with statics
            val updates = if (environment.method.name == STATIC_INITIALIZER && substituteStaticsWithSymbolicVariable) {
                substituteStaticFieldsWithSymbolicVariables(
                    environment.method.declaringClass,
                    memory.queuedStaticMemoryUpdates()
                )
            } else {
                MemoryUpdate() // all memory updates are already added in [environment.state]
            }
            val methodResultWithUpdates = methodResult.copy(symbolicStateUpdate = queuedSymbolicStateUpdates + updates)
            val stateToOffer = environment.state.pop(methodResultWithUpdates)
            offerState(stateToOffer)

            logger.trace { "processResult<${environment.method.signature}> return from nested method" }
            return
        }

        // toplevel method
        // TODO: investigate very strange behavior when some constraints are not added leading to failing CodegenExampleTest::firstExampleTest fails
        val terminalExecutionState = environment.state.copy(
            symbolicState = symbolicState,
            methodResult = methodResult, // the way to put SymbolicResult into terminal state
            label = StateLabel.TERMINAL
        )
        offerState(terminalExecutionState)
    }

    private fun TraversalContext.symbolicSuccess(stmt: ReturnStmt): SymbolicSuccess {
        val type = environment.method.returnType
        val value = when (val instance = resolve(stmt.op, type)) {
            is PrimitiveValue -> instance.cast(type)
            else -> instance
        }
        return SymbolicSuccess(value)
    }

    internal fun asMethodResult(function: Traverser.() -> SymbolicValue): MethodResult {
        val prevSymbolicStateUpdate = queuedSymbolicStateUpdates.copy()
        // TODO: refactor this `try` with `finally` later
        queuedSymbolicStateUpdates = SymbolicStateUpdate()
        try {
            val result = function()
            return MethodResult(
                SymbolicSuccess(result),
                queuedSymbolicStateUpdates
            )
        } finally {
             queuedSymbolicStateUpdates = prevSymbolicStateUpdate
        }
    }
}