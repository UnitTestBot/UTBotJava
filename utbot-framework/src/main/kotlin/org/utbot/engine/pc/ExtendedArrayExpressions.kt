package org.utbot.engine.pc

import org.utbot.engine.PrimitiveValue
import java.util.Objects

sealed class UtExtendedArrayExpression(sort: UtArraySort) : UtArrayExpressionBase(sort)

data class UtArrayInsert(
    val arrayExpression: UtExpression,
    val index: PrimitiveValue,
    val element: UtExpression
) : UtExtendedArrayExpression(arrayExpression.sort as UtArraySort) {
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)


    override val hashCode = Objects.hash(arrayExpression, index, element)

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArrayInsert

        if (arrayExpression != other.arrayExpression) return false
        if (index != other.index) return false
        if (element != other.element) return false

        return true
    }
}

data class UtArrayInsertRange(
    val arrayExpression: UtExpression,
    val index: PrimitiveValue,
    val elements: UtExpression,
    val from: PrimitiveValue,
    val length: PrimitiveValue,
) : UtExtendedArrayExpression(arrayExpression.sort as UtArraySort) {
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)


    override val hashCode = Objects.hash(arrayExpression, index, length, elements)

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArrayInsertRange

        if (arrayExpression != other.arrayExpression) return false
        if (index != other.index) return false
        if (length != other.length) return false
        if (elements != other.elements) return false

        return true
    }
}

data class UtArraySetRange(
    val arrayExpression: UtExpression,
    val index: PrimitiveValue,
    val elements: UtExpression,
    val from: PrimitiveValue,
    val length: PrimitiveValue,
) : UtExtendedArrayExpression(arrayExpression.sort as UtArraySort) {
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)


    override val hashCode = Objects.hash(arrayExpression, index, length, elements)

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArraySetRange

        if (arrayExpression != other.arrayExpression) return false
        if (index != other.index) return false
        if (length != other.length) return false
        if (elements != other.elements) return false

        return true
    }
}

data class UtArrayShiftIndexes(
    val arrayExpression: UtExpression,
    val offset: PrimitiveValue
) : UtExtendedArrayExpression(arrayExpression.sort as UtArraySort) {
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override val hashCode = Objects.hash(arrayExpression, offset)

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArrayShiftIndexes

        if (arrayExpression != other.arrayExpression) return false
        if (offset != other.offset) return false

        return true
    }
}

data class UtArrayRemove(
    val arrayExpression: UtExpression,
    val index: PrimitiveValue
) : UtExtendedArrayExpression(arrayExpression.sort as UtArraySort) {
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)


    override val hashCode = Objects.hash(arrayExpression, index)

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArrayRemove

        if (arrayExpression != other.arrayExpression) return false
        if (index != other.index) return false

        return true
    }
}

data class UtArrayRemoveRange(
    val arrayExpression: UtExpression,
    val index: PrimitiveValue,
    val length: PrimitiveValue
) : UtExtendedArrayExpression(arrayExpression.sort as UtArraySort) {
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)


    override val hashCode = Objects.hash(arrayExpression, index, length)

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArrayRemoveRange

        if (arrayExpression != other.arrayExpression) return false
        if (index != other.index) return false
        if (length != other.length) return false

        return true
    }
}

data class UtStringToArray(
    val stringExpression: UtExpression,
    val offset: PrimitiveValue
) : UtExtendedArrayExpression(UtArraySort(UtIntSort, UtCharSort)) {
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override val hashCode = Objects.hash(stringExpression, offset)

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtStringToArray

        if (stringExpression != other.stringExpression) return false
        if (offset != other.offset) return false

        return true
    }
}

data class UtArrayApplyForAll(
    val arrayExpression: UtExpression,
    val constraint: (UtExpression, PrimitiveValue) -> UtBoolExpression
) : UtExtendedArrayExpression(arrayExpression.sort as UtArraySort) {
    override fun <TResult> accept(visitor: UtExpressionVisitor<TResult>): TResult = visitor.visit(this)

    override val hashCode = Objects.hash(arrayExpression, constraint)

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArrayApplyForAll

        if (arrayExpression != other.arrayExpression) return false
        if (constraint != other.constraint) return false

        return true
    }
}