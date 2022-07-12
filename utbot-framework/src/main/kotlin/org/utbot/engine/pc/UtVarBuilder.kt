package org.utbot.engine.pc

import org.utbot.engine.*
import org.utbot.engine.z3.intValue
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.Parameter
import org.utbot.framework.plugin.api.util.objectClassId
import soot.ArrayType
import soot.PrimType
import soot.RefType
import soot.Type



class UtVarBuilder(
    val holder: UtSolverStatusSAT,
    val typeRegistry: TypeRegistry,
    val typeResolver: TypeResolver,
) : UtExpressionVisitor<Var> {
    private val internalAddrs = mutableMapOf<Address, Var>()

    override fun visit(expr: UtArraySelectExpression): Var {
        when (val array = expr.arrayExpression) {
            is UtMkArrayExpression -> {
                when (array.name) {
                    "arraysLength" -> {
                        val instance = expr.index.accept(this)
                        return ArrayLengthAccess(instance)
                    }
                    "RefValues_Arrays" -> {
                        val instance = expr.index.accept(this)
                        return instance
                    }
                    else -> {
                        val (type, field) = array.name.split("_")
                        val instance = expr.index.accept(this)
                        return FieldAccess(instance, FieldId(ClassId(type), field))
                    }
                }
            }
            is UtArraySelectExpression -> when (val array2 = array.arrayExpression) {
                is UtMkArrayExpression -> when (array2.name) {
                    "RefValues_Arrays" -> return expr.index.accept(this)
                    else -> error("Unexpected")
                }
                else -> error("Unexpected")
            }
            else -> error("Unexpected")
        }
    }

    override fun visit(expr: UtConstArrayExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtMkArrayExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtArrayMultiStoreExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtBvLiteral): Var {
        return NumericConstant(expr.value)
    }

    override fun visit(expr: UtBvConst): Var {
        return Parameter(
            expr.name,
            when (expr.size) {
                8 -> primitiveModelValueToClassId(0.toByte())
                16 -> primitiveModelValueToClassId(0.toShort())
                32 -> primitiveModelValueToClassId(0)
                64 -> primitiveModelValueToClassId(0L)
                else -> error("Unexpected")
            }
        )
    }

    override fun visit(expr: UtAddrExpression): Var {
        return when (val internal = expr.internal) {
            is UtBvConst -> Parameter((expr.internal as UtBvConst).name, holder.findBaseTypeOrNull(expr)?.classId ?: objectClassId)
            is UtBvLiteral -> when (internal.value) {
                0 -> NullVar(objectClassId)
                else -> internalAddrs.getOrPut(internal.value.toInt()) {
                    Parameter("object${internal.value}", holder.findBaseTypeOrNull(expr)?.classId ?: objectClassId)
                }
            }
            else -> expr.internal.accept(this)
        }
    }

    override fun visit(expr: UtFpLiteral): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtFpConst): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtOpExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtTrue): Var {
        return BoolConstant(true)
    }

    override fun visit(expr: UtFalse): Var {
        return BoolConstant(true)
    }

    override fun visit(expr: UtEqExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtBoolConst): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: NotBoolExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtOrBoolExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtAndBoolExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtNegExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtCastExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtBoolOpExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtIsExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtGenericExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtIsGenericTypeExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtEqGenericTypeParametersExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtInstanceOfExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtIteExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtMkTermArrayExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtStringConst): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtConcatExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtConvertToString): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtStringToInt): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtStringLength): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtStringPositiveLength): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtStringCharAt): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtStringEq): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtSubstringExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtReplaceExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtStartsWithExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtEndsWithExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtIndexOfExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtContainsExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtToStringExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtSeqLiteral): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtArrayToString): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtArrayInsert): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtArrayInsertRange): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtArrayRemove): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtArrayRemoveRange): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtArraySetRange): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtArrayShiftIndexes): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtArrayApplyForAll): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtStringToArray): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtAddNoOverflowExpression): Var {
        TODO("Not yet implemented")
    }

    override fun visit(expr: UtSubNoOverflowExpression): Var {
        TODO("Not yet implemented")
    }


    /**
     * Returns evaluated type by object's [addr] or null if there is no information about evaluated typeId.
     */
    private fun UtSolverStatusSAT.findBaseTypeOrNull(addr: UtAddrExpression): Type? {
        val typeId = eval(typeRegistry.symTypeId(addr)).intValue()
        return typeRegistry.typeByIdOrNull(typeId)
    }

    /**
     * We have a constraint stated that every number of dimensions is in [0..MAX_NUM_DIMENSIONS], so if we have a value
     * from outside of the range, it means that we have never touched the number of dimensions for the given addr.
     */
    private fun UtSolverStatusSAT.findNumDimensionsOrNull(addr: UtAddrExpression): Int? {
        val numDimensions = eval(typeRegistry.symNumDimensions(addr)).intValue()
        return if (numDimensions in 0..MAX_NUM_DIMENSIONS) numDimensions else null
    }

    private fun UtSolverStatusSAT.constructTypeOrNull(addr: UtAddrExpression, defaultType: Type): Type? {
        val evaluatedType = findBaseTypeOrNull(addr) ?: return defaultType
        val numDimensions = findNumDimensionsOrNull(addr) ?: defaultType.numDimensions

        // If we have numDimensions greater than zero, we have to check if the object is a java.lang.Object
        // that is actually an instance of some array (e.g., Object -> Int[])
        if (defaultType.isJavaLangObject() && numDimensions > 0) {
            return evaluatedType.makeArrayType(numDimensions)
        }

        // If it does not, the numDimension must be exactly the same as in the defaultType; otherwise, it means that we
        // have never `touched` the element during the analysis. Note that `isTouched` does not point on it,
        // because there might be an aliasing between this addr and an addr of some other object, that we really
        // touched, e.g., the addr of `this` object. In such case we can remove null to construct UtNullModel later.
        if (numDimensions != defaultType.numDimensions) {
            return null
        }

        require(numDimensions == defaultType.numDimensions)

        // if we have a RefType, but not an instance of java.lang.Object, or an java.lang.Object with zero dimension
        if (defaultType is RefType) {
            val inheritors = typeResolver.findOrConstructInheritorsIncludingTypes(defaultType)
            return evaluatedType.takeIf { evaluatedType in inheritors }
                ?: fallbackToDefaultTypeIfPossible(evaluatedType, defaultType)
        }

        defaultType as ArrayType

        return constructArrayTypeOrNull(evaluatedType, defaultType, numDimensions)
            ?: fallbackToDefaultTypeIfPossible(evaluatedType, defaultType)
    }

    private fun constructArrayTypeOrNull(evaluatedType: Type, defaultType: ArrayType, numDimensions: Int): ArrayType? {
        if (numDimensions <= 0) return null

        val actualType = evaluatedType.makeArrayType(numDimensions)
        val actualBaseType = actualType.baseType
        val defaultBaseType = defaultType.baseType
        val defaultNumDimensions = defaultType.numDimensions

        if (actualType == defaultType) return actualType

        // i.e., if defaultType is Object[][], the actualType must be at least primType[][][]
        if (actualBaseType is PrimType && defaultBaseType.isJavaLangObject() && numDimensions > defaultNumDimensions) {
            return actualType
        }

        // i.e., if defaultType is Object[][], the actualType must be at least RefType[][]
        if (actualBaseType is RefType && defaultBaseType.isJavaLangObject() && numDimensions >= defaultNumDimensions) {
            return actualType
        }

        if (actualBaseType is RefType && defaultBaseType is RefType) {
            val inheritors = typeResolver.findOrConstructInheritorsIncludingTypes(defaultBaseType)
            // if actualBaseType in inheritors, actualType and defaultType must have the same numDimensions
            if (actualBaseType in inheritors && numDimensions == defaultNumDimensions) return actualType
        }

        return null
    }

    /**
     * Tries to determine whether it is possible to use [defaultType] instead of [actualType] or not.
     */
    private fun fallbackToDefaultTypeIfPossible(actualType: Type, defaultType: Type): Type? {
        val defaultBaseType = defaultType.baseType

        // It might be confusing we do we return null instead of default type here for the touched element.
        // The answer is because sometimes we may have a real object with different type as an element here.
        // I.e. we have int[][]. In the z3 memory it is an infinite array represented by const model and stores.
        // Let's assume we know that the array has only one element. It means that solver can do whatever it wants
        // with every other element but the first one. In such cases sometimes it sets as const model (or even store
        // outside the array's length) existing objects (that has been touched during the execution) with a wrong
        // (for the array) type. Because of such cases we have to return null as a sign that construction failed.
        // If we return defaultType, it will mean that it might try to put model with an inappropriate type
        // as const or store model.
        if (defaultBaseType is PrimType) return null

        val actualBaseType = actualType.baseType

        require(actualBaseType is RefType) { "Expected RefType, but $actualBaseType found" }
        require(defaultBaseType is RefType) { "Expected RefType, but $defaultBaseType found" }

        val ancestors = typeResolver.findOrConstructAncestorsIncludingTypes(defaultBaseType)

        // This is intended to fix a specific problem. We have code:
        // ColoredPoint foo(Point[] array) {
        //     array[0].x = 5;
        //     return (ColoredPoint[]) array;
        // }
        // Since we don't have a way to connect types of the array and the elements within it, there might be situation
        // when the array is ColoredPoint[], but the first element of it got type Point from the solver.
        // In such case here we'll have ColoredPoint as defaultType and Point as actualType. It is obvious from the example
        // that we can construct ColoredPoint instance instead of it with randomly filled colored-specific fields.
        return defaultType.takeIf { actualBaseType in ancestors && actualType.numDimensions == defaultType.numDimensions }
    }
}