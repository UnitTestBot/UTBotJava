package org.utbot.engine.pc

import org.utbot.engine.SeqType
import com.microsoft.z3.Context
import com.microsoft.z3.Sort
import java.util.Objects
import soot.ArrayType
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.PrimType
import soot.RefType
import soot.ShortType
import soot.Type

/**
 *
 */
sealed class UtSort {
    override fun toString(): String = this.javaClass.simpleName.removeSurrounding("Ut", "Sort")
}

data class UtArraySort(val indexSort: UtSort, val itemSort: UtSort) : UtSort() {
    private val hashCode = Objects.hash(indexSort, itemSort)

    override fun toString() = "$indexSort -> $itemSort"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtArraySort

        if (indexSort != other.indexSort) return false
        if (itemSort != other.itemSort) return false

        return true
    }

    override fun hashCode() = hashCode
}

// String literal (not a String Java object!)
object UtSeqSort : UtSort()

sealed class UtPrimitiveSort(val type: PrimType) : UtSort()

//float, double
object UtFp32Sort : UtPrimitiveSort(FloatType.v())
object UtFp64Sort : UtPrimitiveSort(DoubleType.v())

//boolean
object UtBoolSort : UtPrimitiveSort(BooleanType.v())

//integers and char
sealed class UtBvSort(val size: Int, type: PrimType) : UtPrimitiveSort(type) {
    override fun toString() = super.toString() + size
}

//@formatter:off
object UtByteSort   : UtBvSort(Byte     .SIZE_BITS, ByteType.v())
object UtShortSort  : UtBvSort(Short    .SIZE_BITS, ShortType.v())
object UtIntSort    : UtBvSort(Int      .SIZE_BITS, IntType.v())
object UtLongSort   : UtBvSort(Long     .SIZE_BITS, LongType.v())
object UtCharSort   : UtBvSort(Char     .SIZE_BITS, CharType.v()) //the same size as short

val UtInt32Sort = UtIntSort
val UtAddrSort = UtInt32Sort //maybe int64 in future


fun Type.toSort(): UtSort = when (this) {
    is ByteType     -> UtByteSort
    is CharType     -> UtCharSort
    is ShortType    -> UtShortSort
    is IntType      -> UtIntSort
    is LongType     -> UtLongSort

    is FloatType    -> UtFp32Sort
    is DoubleType   -> UtFp64Sort

    is BooleanType  -> UtBoolSort

    is SeqType      -> UtSeqSort

    is ArrayType -> if (numDimensions == 1) UtArraySort(UtAddrSort, elementType.toSort()) else UtArraySort(UtAddrSort, UtAddrSort)
    is RefType -> UtAddrSort

    else -> error("${this::class} sort is not implemented")
}
//@formatter:on


fun UtSort.toZ3Sort(ctx: Context): Sort = when (this) {

    is UtBvSort -> ctx.mkBitVecSort(size)
    UtFp32Sort -> ctx.mkFPSort32()
    UtFp64Sort -> ctx.mkFPSort64()
    UtBoolSort -> ctx.mkBoolSort()

    UtSeqSort -> ctx.stringSort

    is UtArraySort -> ctx.mkArraySort(indexSort.toZ3Sort(ctx), itemSort.toZ3Sort(ctx))
}

// TODO remove it
fun equalSorts(fst: UtSort, snd: UtSort) =
    when (fst) {
        is UtCharSort -> snd == UtShortSort
        is UtShortSort -> snd == UtCharSort
        else -> false
    }
