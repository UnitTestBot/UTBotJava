package org.utbot.modifications

import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.id
import org.utbot.framework.util.executableId
import org.utbot.modifications.util.kotlinIntrinsicsClassId
import org.utbot.modifications.util.retrieveJimpleBody
import soot.Scene
import soot.SootMethod
import soot.jimple.InvokeExpr
import soot.jimple.JimpleBody
import soot.jimple.ParameterRef
import soot.jimple.StaticFieldRef
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JIdentityStmt
import soot.jimple.internal.JInstanceFieldRef
import soot.jimple.internal.JInvokeStmt
import soot.jimple.internal.JReturnStmt
import soot.jimple.internal.JReturnVoidStmt
import soot.jimple.internal.JimpleLocal

/**
 * Information about [executableId] required to use it
 * in assemble model construction process.
 *
 * @param params describes the params to call [executableId] with
 * @param setFields describes fields set to required value in [executableId]
 * @param affectedFields describes all fields affected in [executableId]
 * */
data class ExecutableAssembleInfo(
    val executableId: ExecutableId,
    val params: Map<Int, FieldId> = mapOf(),
    val setFields: Set<FieldId> = setOf(),
    val affectedFields: Set<FieldId> = setOf()
)

/**
 * Analyzer of constructors and methods based on Soot.
 */
class ExecutableAnalyzer {
    // `Scene.v()` may not yet be initialized when `ExecutableAnalyzer` is created
    private val scene get() = Scene.v()

    /**
     * Verifies that [executableId] can be used in assemble models.
     * Analyses Soot representation of [executableId] for that.
     */
    fun isAppropriate(executableId: ExecutableId): Boolean {
        val sootMethod = sootMethod(executableId) ?: return false
        return isAppropriate(sootMethod)
    }

    /**
     * Retrieves information about [executableId] params and modified fields from Soot.
     */
    fun analyze(executableId: ExecutableId): ExecutableAssembleInfo {
        val setFields = mutableSetOf<FieldId>()
        val affectedFields = mutableSetOf<FieldId>()

        val sootMethod = sootMethod(executableId)
            ?: error("Soot representation of $executableId is not found.")
        val params = analyze(sootMethod, setFields, affectedFields)

        return ExecutableAssembleInfo(executableId, params, setFields, affectedFields)
    }

    //A cache of executable that has been analyzed if they are appropriate or not
    private val analyzedExecutables: MutableMap<SootMethod, Boolean> = mutableMapOf()

    /**
     * Verifies that the body of this [sootMethod]
     * contains only statements matching pattern
     * this.a = something
     * where "a" is an argument of the [sootMethod].
     *
     * NOTE: In case of constructors, calls to other constructors
     * (i.e. `super(...)` and `this(...)`) are also allowed.
     */
    private fun isAppropriate(sootMethod: SootMethod): Boolean {
        if (sootMethod in analyzedExecutables) {
            return analyzedExecutables[sootMethod]!!
        }
        analyzedExecutables[sootMethod] = false

        val jimpleBody = retrieveJimpleBody(sootMethod) ?: return false
        if (hasSuspiciousInstructions(jimpleBody) || modifiesStatics(jimpleBody)) {
            return false
        }

        //find all invoked constructors (support of inheritance), verify they are appropriate
        val invocations = invocations(jimpleBody).map { it.method }
        invocations.forEach { constructor ->
            if (!constructor.isConstructor || !isAppropriate(constructor)) {
                return false
            }
        }

        analyzedExecutables[sootMethod] = true
        return true
    }

    /**
     * Verifies that assignment has only parameter variable in right part.
     *
     * Parameter variables differ form other by the first symbol: it is not $.
     */
    private fun JAssignStmt.isPrimitive(): Boolean {
        val jimpleLocal = this.rightOp as? JimpleLocal ?: return false
        return jimpleLocal.name.first() != '$'
    }

    private fun analyze(
        sootMethod: SootMethod,
        setFields: MutableSet<FieldId>,
        affectedFields: MutableSet<FieldId>,
    ): Map<Int, FieldId> {
        val jimpleBody = retrieveJimpleBody(sootMethod) ?: return emptyMap()
        analyzeAssignments(jimpleBody, setFields, affectedFields)

        val indexOfLocals = jimpleVariableIndices(jimpleBody)
        val indexedFields = indexToField(sootMethod).toMutableMap()

        for (invocation in invocations(jimpleBody)) {
            val invokedIndexedFields = analyze(invocation.method, setFields, affectedFields)

            for ((index, argument) in invocation.args.withIndex()) {
                val fieldId = invokedIndexedFields[index] ?: continue
                val fieldIndex = indexOfLocals[argument] ?: continue

                indexedFields[fieldIndex] = fieldId
            }
        }

        return indexedFields
    }

    /**
     * Analyze assignments if they are primitive and allow
     * to set a field into required value so on.
     */
    private fun analyzeAssignments(
        jimpleBody: JimpleBody,
        setFields: MutableSet<FieldId>,
        affectedFields: MutableSet<FieldId>,
    ) {
        for (assn in assignments(jimpleBody)) {
            val leftPart = assn.leftOp as? JInstanceFieldRef ?: continue

            val fieldId = FieldId(leftPart.field.declaringClass.id, leftPart.field.name)
            if (assn.isPrimitive()) {
                setFields.add(fieldId)
            } else {
                affectedFields.add(fieldId)
            }
        }
    }

    /**
     * Matches an index of executable argument with a [FieldId].
     */
    private fun indexToField(sootMethod: SootMethod): Map<Int, FieldId> {
        val jimpleBody = retrieveJimpleBody(sootMethod) ?: return emptyMap()
        val assignments = assignments(jimpleBody)

        val indexedFields = mutableMapOf<Int, FieldId>()
        for (assn in assignments) {
            val jimpleLocal = assn.rightOp as? JimpleLocal ?: continue

            val field = (assn.leftOp as? JInstanceFieldRef)?.field ?: continue
            val parameterIndex = jimpleBody.parameterLocals.indexOfFirst { it.name == jimpleLocal.name }
            indexedFields[parameterIndex] = FieldId(field.declaringClass.id, field.name)
        }

        return indexedFields
    }

    /**
     * Matches Jimple variable name with an index in current executable.
     */
    private fun jimpleVariableIndices(jimpleBody: JimpleBody) = jimpleBody.units
        .filterIsInstance<JIdentityStmt>()
        .filter { it.leftOp is JimpleLocal && it.rightOp is ParameterRef }
        .associate { it.leftOp as JimpleLocal to (it.rightOp as ParameterRef).index }

    private val sootMethodCache = mutableMapOf<ExecutableId, SootMethod?>()

    private fun sootMethod(executableId: ExecutableId): SootMethod? = sootMethodCache.getOrPut(executableId) {
        scene.getSootClass(executableId.classId.name).methods.firstOrNull {
            it.executableId == executableId
        }
    }

    private fun hasSuspiciousInstructions(jimpleBody: JimpleBody): Boolean =
        jimpleBody.units.any {
            it !is JIdentityStmt
                    && !(it is JAssignStmt && it.rightOp !is InvokeExpr)
                    && it !is JInvokeStmt
                    && it !is JReturnStmt
                    && it !is JReturnVoidStmt
        }

    private fun modifiesStatics(jimpleBody: JimpleBody): Boolean =
        jimpleBody.units.any { it is JAssignStmt && it.leftOp is StaticFieldRef }

    private fun assignments(jimpleBody: JimpleBody) =
        jimpleBody.units
            .filterIsInstance<JAssignStmt>()

    private fun invocations(jimpleBody: JimpleBody): List<InvokeExpr> =
        jimpleBody.units
            .filterIsInstance<JInvokeStmt>()
            .map { it.invokeExpr }
            // These are instructions inserted by Kotlin compiler to check that arguments are not null, we should ignore them
            .filterNot { it.method.declaringClass.id == kotlinIntrinsicsClassId }
}