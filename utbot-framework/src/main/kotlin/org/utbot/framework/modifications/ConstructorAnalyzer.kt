package org.utbot.framework.modifications

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.framework.plugin.api.util.jClass
import soot.Scene
import soot.SootMethod
import soot.Type
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
 * Information about constructor required to use it
 * in assemble model construction process.
 *
 * @param params describes the params to call constructor with
 * @param setFields describes fields set to required value in constructor
 * @param affectedFields describes all fields affected in constructor
 * */
data class ConstructorAssembleInfo(
    val constructorId: ConstructorId,
    val params: Map<Int, FieldId>,
    val setFields: Set<FieldId>,
    val affectedFields: Set<FieldId>
)

/**
 * Analyzer of constructors based on Soot.
 */
class ConstructorAnalyzer {
    private val scene = Scene.v()

    /**
     * Verifies that [constructorId] can be used in assemble models.
     * Analyses Soot representation of constructor for that.
     */
    fun isAppropriate(constructorId: ConstructorId): Boolean {
        val sootConstructor = sootConstructor(constructorId) ?: return false
        return isAppropriate(sootConstructor)
    }

    /**
     * Retrieves information about [constructorId] params and modified fields from Soot.
     */
    fun analyze(constructorId: ConstructorId): ConstructorAssembleInfo {
        val setFields = mutableSetOf<FieldId>()
        val affectedFields = mutableSetOf<FieldId>()

        val sootConstructor = sootConstructor(constructorId)
            ?: error("Soot representation of $constructorId is not found.")
        val params = analyze(sootConstructor, setFields, affectedFields)

        return ConstructorAssembleInfo(constructorId, params, setFields, affectedFields)
    }

    //A cache of constructors been analyzed if they are appropriate or not
    private val analyzedConstructors: MutableMap<SootMethod, Boolean> = mutableMapOf()

    /**
     *Verifies that the body of this constructor
     * contains only statements matching pattern
     * this.a = something
     * where "a" is an argument of the constructor.
     */
    private fun isAppropriate(sootConstructor: SootMethod): Boolean {
        if (sootConstructor in analyzedConstructors) {
            return analyzedConstructors[sootConstructor]!!
        }
        analyzedConstructors[sootConstructor] = false

        val jimpleBody = retrieveJimpleBody(sootConstructor) ?: return false
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

        analyzedConstructors[sootConstructor] = true
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

    private val visitedConstructors = mutableSetOf<SootMethod>()

    private fun analyze(
        sootConstructor: SootMethod,
        setFields: MutableSet<FieldId>,
        affectedFields: MutableSet<FieldId>,
    ): Map<Int, FieldId> {
        if (sootConstructor in visitedConstructors) {
            return emptyMap()
        }
        visitedConstructors.add(sootConstructor)

        val jimpleBody = retrieveJimpleBody(sootConstructor) ?: return emptyMap()
        analyzeAssignments(jimpleBody, setFields, affectedFields)

        val indexOfLocals = jimpleVariableIndices(jimpleBody)
        val indexedFields = indexToField(sootConstructor).toMutableMap()

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
     * Matches an index of constructor argument with a [FieldId].
     */
    private fun indexToField(sootConstructor: SootMethod): Map<Int, FieldId> {
        val jimpleBody = retrieveJimpleBody(sootConstructor) ?: return emptyMap()
        val assignments = assignments(jimpleBody)

        val indexedFields = mutableMapOf<Int, FieldId>()
        for (assn in assignments) {
            val jimpleLocal = assn.rightOp as? JimpleLocal ?: continue

            val field = (assn.leftOp as? JInstanceFieldRef)?.field ?: continue
            val parameterIndex = jimpleBody.locals.indexOfFirst { it.name == jimpleLocal.name }
            indexedFields[parameterIndex - 1] = FieldId(field.declaringClass.id, field.name)
        }

        return indexedFields
    }

    /**
     * Matches Jimple variable name with an index in current constructor.
     */
    private fun jimpleVariableIndices(jimpleBody: JimpleBody) = jimpleBody.units
        .filterIsInstance<JIdentityStmt>()
        .filter { it.leftOp is JimpleLocal && it.rightOp is ParameterRef }
        .associate { it.leftOp as JimpleLocal to (it.rightOp as ParameterRef).index }

    private val sootConstructorCache = mutableMapOf<ConstructorId, SootMethod>()

    private fun sootConstructor(constructorId: ConstructorId): SootMethod? {
        if (constructorId in sootConstructorCache) {
            return sootConstructorCache[constructorId]
        }
        val sootClass = scene.getSootClass(constructorId.classId.name)
        val allConstructors = sootClass.methods.filter { it.isConstructor }
        val sootConstructor = allConstructors.firstOrNull { sameParameterTypes(it, constructorId) }

        if (sootConstructor != null) {
            sootConstructorCache[constructorId] = sootConstructor
            return sootConstructor
        }

        return null
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

    private fun sameParameterTypes(sootMethod: SootMethod, constructorId: ConstructorId): Boolean {
        val sootConstructorTypes = sootMethod.parameterTypes
        val constructorTypes = constructorId.parameters.map { getParameterType(it) }

        val sootConstructorParamsCount = sootConstructorTypes.count()
        val constructorParamsCount = constructorTypes.count()

        if (sootConstructorParamsCount != constructorParamsCount) return false
        for (i in 0 until sootConstructorParamsCount) {
            if (sootConstructorTypes[i] != constructorTypes[i]) return false
        }

        return true
    }

    /**
     * Restores [Type] by [ClassId] if possible.
     *
     * Note: we return null if restore process failed. Possibly we need to
     * enlarge a set of cases types we can deal with in the future.
     */
    private fun getParameterType(type: ClassId): Type? =
        try {
            when {
                type.isRefType -> scene.getRefType(type.name)
                type.isArray -> scene.getType(type.jClass.canonicalName)
                else ->  scene.getType(type.name)
            }
        } catch (e: Exception) {
            null
        }
}