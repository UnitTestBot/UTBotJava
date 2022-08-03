package org.utbot.framework.codegen.model.util

import org.utbot.framework.codegen.model.constructor.tree.CgCallableAccessManager
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.util.*
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.FieldId
import org.utbot.jcdb.api.MethodId

fun CgExpression.at(index: Any?): CgArrayElementAccess =
    CgArrayElementAccess(this, index.resolve())

infix fun CgExpression.equalTo(other: Any?): CgEqualTo =
    CgEqualTo(this, other.resolve())

infix fun CgExpression.lessThan(other: Any?): CgLessThan =
    CgLessThan(this, other.resolve())

infix fun CgExpression.greaterThan(other: Any?): CgGreaterThan =
    CgGreaterThan(this, other.resolve())

// Literals

// TODO: is it OK to use Object as a type of null literal?
fun nullLiteral() = CgLiteral(objectClassId.type(), null)

fun intLiteral(num: Int) = CgLiteral(intClassId.type(false), num)

fun longLiteral(num: Long) = CgLiteral(longClassId.type(false), num)

fun byteLiteral(num: Byte) = CgLiteral(byteClassId.type(false), num)

fun shortLiteral(num: Short) = CgLiteral(shortClassId.type(false), num)

fun floatLiteral(num: Float) = CgLiteral(floatClassId.type(false), num)

fun doubleLiteral(num: Double) = CgLiteral(doubleClassId.type(false), num)

fun booleanLiteral(b: Boolean) = CgLiteral(booleanClassId.type(false), b)

fun charLiteral(c: Char) = CgLiteral(charClassId.type(false), c)

fun stringLiteral(string: String) = CgLiteral(stringClassId.type(false), string)

// Field access

// non-static fields
operator fun CgExpression.get(fieldId: FieldId): CgFieldAccess =
    CgFieldAccess(this, fieldId)

// static fields
// TODO: unused receiver
operator fun ClassId.get(fieldId: FieldId): CgStaticFieldAccess =
    CgStaticFieldAccess(fieldId)

// Get array length

/**
 * Returns length field access for array type variable and [getArrayLengthMethodId] call otherwise.
 */
fun CgVariable.length(
    cgCallableAccessManager: CgCallableAccessManager,
    thisInstance: CgThisInstance,
    getArrayLengthMethodId: MethodId
): CgExpression {
    val thisVariable = this

    return if (type.classId.isArray) {
        CgGetLength(thisVariable)
    } else {
        with(cgCallableAccessManager) { thisInstance[getArrayLengthMethodId](thisVariable) }
    }
}

// Increment and decrement

fun CgVariable.inc(): CgIncrement = CgIncrement(this)

fun CgVariable.dec(): CgDecrement = CgDecrement(this)

fun Any?.resolve(): CgExpression = when (this) {
    null -> nullLiteral()
    is Int -> intLiteral(this)
    is Long -> longLiteral(this)
    is Byte -> byteLiteral(this)
    is Short -> shortLiteral(this)
    is Float -> floatLiteral(this)
    is Double -> doubleLiteral(this)
    is Boolean -> booleanLiteral(this)
    is Char -> charLiteral(this)
    is String -> stringLiteral(this)
    is CgExpression -> this
    else -> error("Expected primitive, string, null or CgExpression, but got: ${this::class}")
}

fun Array<*>.resolve(): List<CgExpression> = map { it.resolve() }

fun classLiteralAnnotationArgument(id: ClassId, codegenLanguage: CodegenLanguage): CgGetClass = when (codegenLanguage) {
    CodegenLanguage.JAVA -> CgGetJavaClass(id)
    CodegenLanguage.KOTLIN -> CgGetKotlinClass(id)
}
