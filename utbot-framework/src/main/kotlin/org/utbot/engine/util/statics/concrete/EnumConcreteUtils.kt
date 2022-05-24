package org.utbot.engine.util.statics.concrete

import org.utbot.common.withAccessibility
import org.utbot.engine.MemoryChunkDescriptor
import org.utbot.engine.MethodResult
import org.utbot.engine.ObjectValue
import org.utbot.engine.SymbolicSuccess
import org.utbot.engine.SymbolicValue
import org.utbot.engine.TypeResolver
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.engine.addr
import org.utbot.engine.canRetrieveBody
import org.utbot.engine.constructEnumStaticFieldResult
import org.utbot.engine.fieldId
import org.utbot.engine.nullObjectAddr
import org.utbot.engine.pc.addrEq
import org.utbot.engine.pc.mkEq
import org.utbot.engine.pc.mkNot
import org.utbot.engine.pc.select
import org.utbot.engine.staticInitializerOrNull
import org.utbot.engine.symbolic.SymbolicStateUpdate
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.toMethodResult
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.field
import soot.SootClass
import soot.SootField
import soot.SootMethod
import soot.Type
import soot.jimple.StaticFieldRef
import soot.jimple.Stmt
import soot.jimple.internal.JAssignStmt

fun UtBotSymbolicEngine.makeSymbolicValuesFromEnumConcreteValues(
    type: Type,
    enumConstantRuntimeValues: List<Enum<*>>
): Pair<List<ObjectValue>, Map<String, MethodResult>> {
    val enumConstantResultsWithNames = enumConstantRuntimeValues.map {
        it.name to toMethodResult(it, type)
    }
    val enumConstantSymbolicValuesWithNames = enumConstantResultsWithNames.map {
        it.first to (it.second.symbolicResult as SymbolicSuccess).value as ObjectValue
    }

    val enumConstantSymbolicValues = enumConstantSymbolicValuesWithNames.map { it.second }

    val enumConstantSymbolicResultsByName = enumConstantResultsWithNames.toMap()

    return enumConstantSymbolicValues to enumConstantSymbolicResultsByName
}

fun associateEnumSootFieldsWithConcreteValues(
    enumFields: List<SootField>,
    enumConstants: List<Enum<*>>
): List<Pair<SootField, List<Any>>> =
    enumFields.map { enumSootField ->
        val enumField = enumSootField.fieldId.field

        val fieldValues = if (enumSootField.isStatic) {
            val staticFieldValue = enumField.withAccessibility { enumField.get(null) }

            listOf(staticFieldValue)
        } else {
            // extract ordinal, name and other non static fields for every enum constant
            enumConstants.map { concreteValue ->
                enumField.withAccessibility { enumField.get(concreteValue) }
            }
        }

        enumSootField to fieldValues
    }

/**
 * Construct symbolic updates for enum static fields and a symbolic value for a local in the left part of the assignment.
 */
fun UtBotSymbolicEngine.makeEnumStaticFieldsUpdates(
    staticFields: List<Pair<SootField, List<Any>>>,
    declaringClass: SootClass,
    enumConstantSymbolicResultsByName: Map<String, MethodResult>,
    enumConstantSymbolicValues: List<ObjectValue>,
    enumClassValue: ObjectValue,
    fieldId: FieldId
): Pair<SymbolicStateUpdate, SymbolicValue?> {
    var staticFieldsUpdates = SymbolicStateUpdate()
    var symbolicValueForLocal: SymbolicValue? = null

    staticFields.forEach { (sootStaticField, staticFieldRuntimeValue) ->
        val fieldName = sootStaticField.name
        val fieldType = sootStaticField.type
        val (fieldSymbolicResult, fieldSymbolicStateUpdate) = constructEnumStaticFieldResult(
            fieldName,
            fieldType,
            declaringClass,
            enumConstantSymbolicResultsByName,
            staticFieldRuntimeValue.single(),
            enumConstantSymbolicValues
        )

        val fieldSymbolicValue = (fieldSymbolicResult as SymbolicSuccess).value
        val fieldValue = valueToExpression(fieldSymbolicValue, fieldType)

        val fieldUpdate = objectUpdate(enumClassValue, sootStaticField, fieldValue)

        staticFieldsUpdates += fieldSymbolicStateUpdate + fieldUpdate

        // enum constant could not be null
        if (fieldName in enumConstantSymbolicResultsByName) {
            val canBeNull = addrEq(fieldSymbolicValue.addr, nullObjectAddr)
            val canNotBeNull = mkNot(canBeNull)

            staticFieldsUpdates += canNotBeNull.asHardConstraint()
        }

        // save value to associate it with local if required
        if (sootStaticField.name == fieldId.name) {
            symbolicValueForLocal = fieldSymbolicValue
        }
    }

    return staticFieldsUpdates to symbolicValueForLocal
}

fun UtBotSymbolicEngine.makeEnumNonStaticFieldsUpdates(
    enumConstantSymbolicValues: List<ObjectValue>,
    nonStaticFields: List<Pair<SootField, List<Any>>>
): SymbolicStateUpdate {
    var nonStaticFieldsUpdates = SymbolicStateUpdate()

    for ((i, enumConstantSymbolicValue) in enumConstantSymbolicValues.withIndex()) {
        nonStaticFields.forEach { (sootNonStaticField, nonStaticFieldRuntimeValues) ->
            val nonStaticFieldRuntimeValue = nonStaticFieldRuntimeValues[i]

            val fieldType = sootNonStaticField.type
            val (fieldSymbolicResult, fieldSymbolicStateUpdate) = toMethodResult(
                nonStaticFieldRuntimeValue,
                fieldType
            )

            nonStaticFieldsUpdates += fieldSymbolicStateUpdate

            val fieldSymbolicValue = (fieldSymbolicResult as SymbolicSuccess).value
            val fieldValue = valueToExpression(fieldSymbolicValue, fieldType)

            val chunkId = hierarchy.chunkIdForField(enumConstantSymbolicValue.type, sootNonStaticField)
            val descriptor = MemoryChunkDescriptor(chunkId, enumConstantSymbolicValue.type, fieldType)
            val array = memory.findArray(descriptor)
            val arraySelectEqualsValue = mkEq(array.select(enumConstantSymbolicValue.addr), fieldValue)

            nonStaticFieldsUpdates += arraySelectEqualsValue.asHardConstraint()
        }
    }

    return nonStaticFieldsUpdates
}

fun isEnumValuesFieldName(fieldName: String): Boolean = fieldName == "\$VALUES"

/**
 * Checks that [this] is enum which affects any external static fields in its <init>/<clinit> sections.
 */
fun SootClass.isEnumAffectingExternalStatics(typeResolver: TypeResolver): Boolean {
    if (!isEnum) {
        return false
    }

    // enum <clinit> active body contains <init> invocations so we can check only <clinit>
    val staticInitializer = staticInitializerOrNull() ?: return false

    return staticInitializer.isAffectingExternalStatics(this, mutableSetOf(), typeResolver)
}

/**
 * Returns whether [this] method affects any statics from any types except [currentClass] and its interfaces.
 *
 * NOTE: see org.utbot.examples.enums.ClassWithEnum.EnumWithStaticAffectingInit for examples.
 */
fun SootMethod.isAffectingExternalStatics(
    currentClass: SootClass,
    alreadyProcessed: MutableSet<SootMethod>,
    typeResolver: TypeResolver
): Boolean {
    if (this in alreadyProcessed) {
        return false
    }

    alreadyProcessed += this

    // active body could be missing (<java.lang.Enum: void <init>(java.lang.String,int)>, for example)
    // so consider it as not affecting external statics
    if (!canRetrieveBody()) {
        return false
    }

    return activeBody.units.any {
        if (it !is Stmt) {
            return@any false
        }

        when (it) {
            is JAssignStmt -> {
                val leftOp = it.leftOp

                if (leftOp !is StaticFieldRef) {
                    return@any false
                }

                val declaringClass = leftOp.fieldRef.declaringClass()

                val currentClassImplementedInterfaces = typeResolver
                    .findOrConstructAncestorsIncludingTypes(currentClass.type)
                    .filter { type -> type.sootClass.isInterface }
                val inImplementedInterfaces = declaringClass.type in currentClassImplementedInterfaces

                // check that no system statics are affected (but implemented interfaces fields are allowed)
                !(inImplementedInterfaces || declaringClass == currentClass)
            }
            else -> {
                if (it.containsInvokeExpr()) {
                    it.invokeExpr.method.isAffectingExternalStatics(currentClass, alreadyProcessed, typeResolver)
                } else {
                    false
                }
            }
        }
    }
}
