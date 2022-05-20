package org.utbot.engine.pc

import org.utbot.common.WorkaroundReason.HACK
import org.utbot.common.workaround
import org.utbot.engine.PrimitiveValue
import java.util.Objects

sealed class UtStringExpression : UtExpression(UtSeqSort)

data class UtStringConst(val name: String) : UtStringExpression() {

    override val hashCode = Objects.hash(name)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(String$sort $name)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtStringConst

        if (name != other.name) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtConcatExpression(val parts: List<UtStringExpression>) : UtStringExpression() {
    override val hashCode = parts.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.concat ${parts.joinToString()})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtConcatExpression

        if (parts != other.parts) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtConvertToString(val expression: UtExpression) : UtStringExpression() {
    init {
        require(expression.sort is UtBvSort)
    }

    override val hashCode = expression.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.tostr $expression)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtConvertToString

        if (expression != other.expression) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtStringToInt(val expression: UtExpression, override val sort: UtBvSort) : UtBvExpression(sort) {
    init {
        require(expression.sort is UtSeqSort)
    }

    override val hashCode = Objects.hash(expression, sort)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.toint $expression)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtStringToInt

        if (expression != other.expression) return false
        if (sort != other.sort) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtStringLength(val string: UtExpression) : UtBvExpression(UtIntSort) {
    override val hashCode = string.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.len $string)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtStringLength

        if (string != other.string) return false

        return true
    }

    override fun hashCode() = hashCode
}

/**
 * Creates constraint to get rid of negative string length (z3 bug?)
 */
data class UtStringPositiveLength(val string: UtExpression) : UtBoolExpression() {
    override val hashCode = string.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(positive.str.len $string)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtStringPositiveLength

        if (string != other.string) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtStringCharAt(
    val string: UtExpression,
    val index: UtExpression
) : UtExpression(UtCharSort) {
    override val hashCode = Objects.hash(string, index)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.at $string $index)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtStringCharAt

        if (string != other.string) return false
        if (index != other.index) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtStringEq(val left: UtExpression, val right: UtExpression) : UtBoolExpression() {
    override val hashCode = Objects.hash(left, right)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.eq $left $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtStringEq

        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtSubstringExpression(
    val string: UtExpression,
    val beginIndex: UtExpression,
    val length: UtExpression
) : UtStringExpression() {
    override val hashCode = Objects.hash(string, beginIndex, length)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.substr $string $beginIndex $length)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtSubstringExpression

        if (string != other.string) return false
        if (beginIndex != other.beginIndex) return false
        if (length != other.length) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtReplaceExpression(
    val string: UtExpression,
    val regex: UtExpression,
    val replacement: UtExpression
) : UtStringExpression() {
    override val hashCode = Objects.hash(string, regex, replacement)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.replace $string $regex $replacement)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtReplaceExpression

        if (string != other.string) return false
        if (regex != other.regex) return false
        if (replacement != other.replacement) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtStartsWithExpression(
    val string: UtExpression,
    val prefix: UtExpression
) : UtBoolExpression() {
    override val hashCode = Objects.hash(string, prefix)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.prefixof $string $prefix)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtStartsWithExpression

        if (string != other.string) return false
        if (prefix != other.prefix) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtEndsWithExpression(
    val string: UtExpression,
    val suffix: UtExpression
) : UtBoolExpression() {
    override val hashCode = Objects.hash(string, suffix)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.suffixof $string $suffix)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtEndsWithExpression

        if (string != other.string) return false
        if (suffix != other.suffix) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtIndexOfExpression(
    val string: UtExpression,
    val substring: UtExpression
) : UtBoolExpression() {
    override val hashCode = Objects.hash(string, substring)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.indexof $string $substring)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtIndexOfExpression

        if (string != other.string) return false
        if (substring != other.substring) return false

        return true
    }

    override fun hashCode() = hashCode

}

data class UtContainsExpression(
    val string: UtExpression,
    val substring: UtExpression
) : UtBoolExpression() {
    override val hashCode = Objects.hash(string, substring)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.contains $string $substring)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtContainsExpression

        if (string != other.string) return false
        if (substring != other.substring) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtToStringExpression(
    val isNull: UtBoolExpression,
    val notNullExpr: UtExpression
) : UtStringExpression() {
    override val hashCode = Objects.hash(isNull, notNullExpr)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(ite $isNull 'null' $notNullExpr)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtToStringExpression

        if (isNull != other.isNull) return false
        if (notNullExpr != other.notNullExpr) return false

        return true
    }

    override fun hashCode() = hashCode
}

data class UtArrayToString(
    val arrayExpression: UtExpression,
    val offset: PrimitiveValue,
    val length: PrimitiveValue
) : UtStringExpression() {
    override val hashCode = Objects.hash(arrayExpression, offset)

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = "(str.array_to_str $arrayExpression, offset = $offset)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArrayToString

        if (arrayExpression == other.arrayExpression) return false
        if (offset == other.offset) return false

        return true
    }

    override fun hashCode() = hashCode
}

// String literal (not a String Java object!)
data class UtSeqLiteral(val value: String) : UtExpression(UtSeqSort) {
    override val hashCode = value.hashCode()

    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override fun toString() = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtSeqLiteral

        if (value != other.value) return false

        return true
    }

    override fun hashCode() = hashCode
}