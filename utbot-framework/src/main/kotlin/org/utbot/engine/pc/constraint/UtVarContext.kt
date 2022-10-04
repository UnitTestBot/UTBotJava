package org.utbot.engine.pc.constraint

import org.utbot.engine.*
import org.utbot.engine.pc.*
import org.utbot.engine.z3.boolValue
import org.utbot.engine.z3.intValue
import org.utbot.engine.z3.value
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.UtConstraintParameter
import org.utbot.framework.plugin.api.util.*
import soot.ArrayType
import soot.PrimType
import soot.RefType
import soot.Type


class UtVarContext(
    val holder: UtSolverStatusSAT,
    val typeRegistry: TypeRegistry,
    val typeResolver: TypeResolver,
) : UtDefaultExpressionVisitor<UtConstraintVariable>({
    throw NotSupportedByConstraintResolverException()
}) {
    private val internalAddrs = mutableMapOf<Address, UtConstraintVariable>()
    private val var2Expression = mutableMapOf<UtConstraintVariable, UtExpression>()

    fun evalOrNull(variable: UtConstraintVariable) = when {
        hasExpr(variable) -> holder.eval(var2Expression[variable]!!).value()
        else -> null
    }

    operator fun get(variable: UtConstraintVariable) = var2Expression[variable]
        ?: throw IllegalArgumentException()

    fun hasExpr(variable: UtConstraintVariable) = variable in var2Expression

    fun bind(base: UtConstraintVariable, binder: UtConstraintVariable) {
        if (!hasExpr(binder))
            throw IllegalArgumentException()
        var2Expression[base] = var2Expression[binder]!!
    }

    fun evalTypeOrNull(variable: UtConstraintVariable): Type? {
        val addr = var2Expression[variable] as? UtAddrExpression ?: return null
        return holder.findTypeOrNull(addr)
    }


    override fun visit(expr: UtArraySelectExpression): UtConstraintVariable {
        val res = when (val base = expr.arrayExpression.accept(this)) {
            is UtConstraintParameter -> when (base.name) {
                "arraysLength" -> {
                    val instance = expr.index.accept(this)
                    UtConstraintArrayLength(instance)
                }

                "RefValues_Arrays" -> expr.index.accept(this)
                "Multi_Arrays" -> expr.index.accept(this)
                "boolean_Arrays" -> expr.index.accept(this)
                "char_Arrays" -> expr.index.accept(this)
                "int_Arrays" -> expr.index.accept(this)
                "long_Arrays" -> expr.index.accept(this)
                "byte_Arrays" -> expr.index.accept(this)
                "short_Arrays" -> expr.index.accept(this)
                "float_Arrays" -> expr.index.accept(this)
                "double_Arrays" -> expr.index.accept(this)
                else -> {
                    val instance = expr.index.accept(this)
                    try {
                        val (type, field) = base.name.split("_", limit = 2)
                        val fieldId = FieldId(ClassId(type), field)
                        fieldId.type
                        UtConstraintFieldAccess(instance, fieldId)
                    } catch (e: Throwable) {
                        arrayAccess(base, instance)
                    }
                }
            }

            is UtConstraintFieldAccess -> arrayAccess(base, expr.index.accept(this))
            is UtConstraintNumericConstant -> expr.index.accept(this)
            is UtConstraintNull -> UtConstraintArrayAccess(base, expr.index.accept(this), objectClassId)
            is UtConstraintArrayAccess -> arrayAccess(base, expr.index.accept(this))
            else -> error("Unexpected: $base")
        }
        if (res.isPrimitive) {
            var2Expression[res] = expr
        }
        return res
    }

    private fun arrayAccess(base: UtConstraintVariable, index: UtConstraintVariable) = when {
        base.classId.isArray && index.classId.isPrimitive -> {
            UtConstraintArrayAccess(base, index, base.classId.elementClassId!!)
        }

        base.classId.isMap -> {
            UtConstraintArrayAccess(base, index, objectClassId)
        }

        base.classId.isSet -> {
            UtConstraintArrayAccess(base, index, objectClassId)
        }

        else -> index
    }

    override fun visit(expr: UtMkArrayExpression): UtConstraintVariable {
        return UtConstraintParameter(expr.name, objectClassId).also {
            var2Expression[it] = expr
        }
    }

    override fun visit(expr: UtArrayMultiStoreExpression): UtConstraintVariable {
        if (expr.initial !is UtConstArrayExpression) {
            return expr.initial.accept(this)
        }

        val stores = expr.stores.map { it.index.accept(this) }
        return stores.first()
    }

    override fun visit(expr: UtBvLiteral): UtConstraintVariable {
        return UtConstraintNumericConstant(expr.value).also {
            var2Expression[it] = expr
        }
    }

    override fun visit(expr: UtBvConst): UtConstraintVariable {
        return UtConstraintParameter(
            expr.name,
            when (expr.size) {
                8 -> primitiveModelValueToClassId(0.toByte())
                16 -> primitiveModelValueToClassId(0.toShort())
                32 -> primitiveModelValueToClassId(0)
                64 -> primitiveModelValueToClassId(0L)
                else -> error("Unexpected")
            }
        ).also {
            var2Expression[it] = expr
        }
    }

    override fun visit(expr: UtAddrExpression): UtConstraintVariable {
        return when (val internal = expr.internal) {
            is UtBvConst -> UtConstraintParameter(
                (expr.internal as UtBvConst).name,
                holder.findTypeOrNull(expr)?.classId ?: objectClassId
            )

            is UtBvLiteral -> when (internal.value) {
                0 -> UtConstraintNull(objectClassId)
                else -> internalAddrs.getOrPut(internal.value.toInt()) {
                    UtConstraintParameter(
                        "object${internal.value}",
                        holder.findTypeOrNull(expr)?.classId ?: objectClassId
                    )
                }
            }

            else -> expr.internal.accept(this)
        }.also {
            var2Expression[it] = expr
        }
    }

    override fun visit(expr: UtFpLiteral): UtConstraintVariable = UtConstraintNumericConstant(expr.value).also {
        var2Expression[it] = expr
    }

    override fun visit(expr: UtFpConst): UtConstraintVariable =
        UtConstraintParameter(
            expr.name, when (expr.sort) {
                UtFp32Sort -> floatClassId
                UtFp64Sort -> doubleClassId
                else -> error("Unexpected")
            }
        ).also {
            var2Expression[it] = expr
        }

    override fun visit(expr: UtOpExpression): UtConstraintVariable {
        val lhv = expr.left.expr.accept(this)
        val rhv = expr.right.expr.accept(this)
        return when (expr.operator) {
            Add -> UtConstraintAdd(lhv, rhv)
            And -> UtConstraintAnd(lhv, rhv)
            Cmp -> UtConstraintCmp(lhv, rhv)
            Cmpg -> UtConstraintCmpg(lhv, rhv)
            Cmpl -> UtConstraintCmpl(lhv, rhv)
            Div -> UtConstraintDiv(lhv, rhv)
            Mul -> UtConstraintMul(lhv, rhv)
            Or -> UtConstraintOr(lhv, rhv)
            Rem -> UtConstraintRem(lhv, rhv)
            Shl -> UtConstraintShl(lhv, rhv)
            Shr -> UtConstraintShr(lhv, rhv)
            Sub -> UtConstraintSub(lhv, rhv)
            Ushr -> UtConstraintUshr(lhv, rhv)
            Xor -> UtConstraintXor(lhv, rhv)
        }.also {
            var2Expression[it] = expr
        }
    }

    override fun visit(expr: UtTrue): UtConstraintVariable {
        return UtConstraintBoolConstant(true).also {
            var2Expression[it] = expr
        }
    }

    override fun visit(expr: UtFalse): UtConstraintVariable {
        return UtConstraintBoolConstant(true).also {
            var2Expression[it] = expr
        }
    }

    override fun visit(expr: UtBoolConst): UtConstraintVariable {
        return UtConstraintParameter(expr.name, booleanClassId).also {
            var2Expression[it] = expr
        }
    }

    override fun visit(expr: NotBoolExpression): UtConstraintVariable {
        return UtConstraintNot(expr.expr.accept(this)).also {
            var2Expression[it] = expr
        }
    }

    override fun visit(expr: UtNegExpression): UtConstraintVariable {
        return UtConstraintNeg(
            expr.variable.expr.accept(this)
        ).also {
            var2Expression[it] = expr.variable.expr
        }
    }

    override fun visit(expr: UtCastExpression): UtConstraintVariable {
        return UtConstraintCast(
            expr.variable.expr.accept(this),
            expr.type.classId
        ).also {
            var2Expression[it] = expr.variable.expr
        }
    }

    override fun visit(expr: UtIteExpression): UtConstraintVariable {
        val condValue = holder.eval(expr.condition).boolValue()
        return when {
            condValue -> expr.thenExpr.accept(this)
            else -> expr.elseExpr.accept(this)
        }
    }

    /**
     * Returns evaluated type by object's [addr] or null if there is no information about evaluated typeId.
     */
    private fun UtSolverStatusSAT.findTypeOrNull(addr: UtAddrExpression): Type? {
        val base = findBaseTypeOrNull(addr)
        val dimensions = findNumDimensionsOrNull(addr)
        return base?.let { b ->
            dimensions?.let { d ->
                if (d == 0) b
                else b.makeArrayType(d)
            }
        }
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