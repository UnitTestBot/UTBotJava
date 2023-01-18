package org.utbot.framework.codegen.util

import org.utbot.framework.codegen.domain.builtin.UtilMethodProvider
import org.utbot.framework.codegen.domain.models.CgArrayElementAccess
import org.utbot.framework.codegen.domain.models.CgDecrement
import org.utbot.framework.codegen.domain.models.CgEqualTo
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgGetClass
import org.utbot.framework.codegen.domain.models.CgGetJavaClass
import org.utbot.framework.codegen.domain.models.CgGetKotlinClass
import org.utbot.framework.codegen.domain.models.CgGetLength
import org.utbot.framework.codegen.domain.models.CgGreaterThan
import org.utbot.framework.codegen.domain.models.CgIncrement
import org.utbot.framework.codegen.domain.models.CgLessThan
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.tree.CgMethodConstructor
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.stringClassId

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
fun nullLiteral() = CgLiteral(objectClassId, null)

fun nullLiteralWithCast(classId: ClassId) = CgLiteral(classId, null)

fun intLiteral(num: Int) = CgLiteral(intClassId, num)

fun longLiteral(num: Long) = CgLiteral(longClassId, num)

fun byteLiteral(num: Byte) = CgLiteral(byteClassId, num)

fun shortLiteral(num: Short) = CgLiteral(shortClassId, num)

fun floatLiteral(num: Float) = CgLiteral(floatClassId, num)

fun doubleLiteral(num: Double) = CgLiteral(doubleClassId, num)

fun booleanLiteral(b: Boolean) = CgLiteral(booleanClassId, b)

fun charLiteral(c: Char) = CgLiteral(charClassId, c)

fun stringLiteral(string: String) = CgLiteral(stringClassId, string)

// Get array length

/**
 * Returns length field access for array type variable and [UtilMethodProvider.getArrayLengthMethodId] call otherwise.
 */
internal fun CgVariable.length(methodConstructor: CgMethodConstructor): CgExpression {
    val thisVariable = this

    return if (type.isArray) {
        CgGetLength(thisVariable)
    } else {
        with(methodConstructor) { utilsClassId[getArrayLength](thisVariable) }
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
