package org.utbot.features

import org.utbot.engine.pc.*
import java.lang.Integer.max


private const val TREES = "Trees"
private const val MAX_NODES = "MaxNodes"
private const val MIN_NODES = "MinNodes"
private const val AVE_NODES = "AveNodes"
private const val MAX_LEVEL = "MaxLevel"
private const val MIN_LEVEL = "MinLevel"
private const val AVE_LEVEL = "AveLevel"

val featureIndex = listOf(
        UtArraySelectExpression::class.simpleName,
        UtMkArrayExpression::class.simpleName,
        UtArrayMultiStoreExpression::class.simpleName,
        UtBvLiteral::class.simpleName,
        UtBvConst::class.simpleName,
        UtAddrExpression::class.simpleName,
        UtFpLiteral::class.simpleName,
        UtFpConst::class.simpleName,
        UtOpExpression::class.simpleName,
        UtTrue::class.simpleName,
        UtFalse::class.simpleName,
        UtEqExpression::class.simpleName,
        UtBoolConst::class.simpleName,
        NotBoolExpression::class.simpleName,
        UtOrBoolExpression::class.simpleName,
        UtAndBoolExpression::class.simpleName,
        UtNegExpression::class.simpleName,
        UtCastExpression::class.simpleName,
        UtBoolOpExpression::class.simpleName,
        UtIsExpression::class.simpleName,
        UtIteExpression::class.simpleName,
        TREES,
        MAX_NODES,
        MIN_NODES,
        AVE_NODES,
        MAX_LEVEL,
        MIN_LEVEL,
        AVE_LEVEL
).mapIndexed { index, cls -> cls!! to index }.toMap()

val featureIndexHash = featureIndex.hashCode()

class UtExpressionStructureCounter(private val input: Iterable<UtExpression>) : UtExpressionVisitor<NestStat> {
    private val features = DoubleArray(featureIndex.size)

    fun extract(): DoubleArray {
        val trees = input.count()
        if (trees == 0) {
            return features
        }
        val stats = input.map { buildState(it) }

        features[featureIndex.getValue(TREES)] = trees.toDouble()

        features[featureIndex.getValue(MAX_NODES)] = stats.maxOf { it.nodes }.toDouble()
        features[featureIndex.getValue(MIN_NODES)] = stats.minOf { it.nodes }.toDouble()
        features[featureIndex.getValue(AVE_NODES)] = stats.sumOf { it.nodes }.toDouble() / trees

        features[featureIndex.getValue(MAX_LEVEL)] = stats.maxOf { it.level }.toDouble()
        features[featureIndex.getValue(MIN_LEVEL)] = stats.minOf { it.level }.toDouble()
        features[featureIndex.getValue(AVE_LEVEL)] = stats.sumOf { it.level }.toDouble() / trees

        return features
    }

    private fun buildState(expr: UtExpression): NestStat {
        val key = expr::class.simpleName
        featureIndex[key]?.let {
            features[it]++
        }

        return expr.accept(this)
    }

    override fun visit(expr: UtArraySelectExpression): NestStat {
        val stat = buildState(expr.arrayExpression)
        val stats = buildState(expr.index)
        return NestStat(
                nodes = stat.nodes + stats.nodes + 1,
                level = max(stats.level, stat.level) + 1
        )
    }

    // array declared
    override fun visit(expr: UtMkArrayExpression) = NestStat()

    override fun visit(expr: UtConstArrayExpression): NestStat {
        val stat = buildState(expr.constValue)
        stat.level++
        stat.nodes++
        return stat
    }

    override fun visit(expr: UtArrayMultiStoreExpression): NestStat {
        val statInitial = buildState(expr.initial)
        val stats = expr.stores.map { it.index }.map { buildState(it) }
        val maxStat = stats.maxByOrNull { it.level } ?: NestStat()
        return NestStat(
                nodes = stats.sumOf { it.nodes } + statInitial.nodes + 1,
                level = statInitial.level + maxStat.level + 1
        )
    }

    override fun visit(expr: UtBvLiteral) = NestStat()

    override fun visit(expr: UtBvConst) = NestStat()

    override fun visit(expr: UtAddrExpression): NestStat {
        val stat = buildState(expr.internal)
        stat.level++
        stat.nodes++
        return stat
    }

    override fun visit(expr: UtFpLiteral) = NestStat()

    override fun visit(expr: UtFpConst) = NestStat()

    override fun visit(expr: UtOpExpression) = multipleExpressions(expr.left.expr, expr.right.expr)

    override fun visit(expr: UtTrue) = NestStat()

    override fun visit(expr: UtFalse) = NestStat()

    override fun visit(expr: UtEqExpression) = multipleExpressions(expr.left, expr.right)

    override fun visit(expr: UtBoolConst) = NestStat()

    override fun visit(expr: NotBoolExpression): NestStat {
        val stat = NestStat()
        stat.level++
        stat.nodes++
        return stat
    }

    override fun visit(expr: UtOrBoolExpression) = multipleExpression(expr.exprs)

    override fun visit(expr: UtAndBoolExpression) = multipleExpression(expr.exprs)

    override fun visit(expr: UtAddNoOverflowExpression) = multipleExpressions(expr.left, expr.right)

    override fun visit(expr: UtSubNoOverflowExpression) = multipleExpressions(expr.left, expr.right)
    override fun visit(expr: UtMulNoOverflowExpression)= multipleExpressions(expr.left, expr.right)

    override fun visit(expr: UtNegExpression): NestStat {
        val stat = buildState(expr.variable.expr)
        stat.level++
        stat.nodes++
        return stat
    }

    override fun visit(expr: UtBvNotExpression): NestStat {
        val stat = buildState(expr.variable.expr)
        stat.level++
        stat.nodes++
        return stat
    }

    override fun visit(expr: UtCastExpression): NestStat {
        val stat = buildState(expr.variable.expr)
        stat.level++
        stat.nodes++
        return stat
    }

    override fun visit(expr: UtBoolOpExpression) = multipleExpressions(expr.left.expr, expr.right.expr)

    override fun visit(expr: UtIsExpression): NestStat {
        val stat = buildState(expr.addr)
        stat.level++
        stat.nodes++
        return stat
    }


    override fun visit(expr: UtGenericExpression): NestStat {
        return NestStat()
    }

    override fun visit(expr: UtIsGenericTypeExpression): NestStat {
        return NestStat()
    }

    override fun visit(expr: UtEqGenericTypeParametersExpression): NestStat {
        return NestStat()
    }


    override fun visit(expr: UtInstanceOfExpression): NestStat {
        val stat = buildState(expr)
        stat.level++
        stat.nodes++
        return stat
    }

    override fun visit(expr: UtIteExpression): NestStat {
        val stateCondition = buildState(expr.condition)
        val stateThen = buildState(expr.thenExpr)
        val stateElse = buildState(expr.elseExpr)

        return NestStat(
                nodes = (stateElse.nodes + stateThen.nodes) / 2 + stateCondition.nodes + 1,
                level = maxOf(stateElse.level, stateThen.level, stateCondition.level) + 1
        )
    }

    private fun multipleExpressions(vararg expressions: UtExpression) = multipleExpression(expressions.toList())

    private fun multipleExpression(expressions: List<UtExpression>): NestStat {
        val stats = expressions.map { buildState(it) }
        val level = stats.maxOfOrNull { it.level } ?: 0
        return NestStat(
                level = level + 1,
                nodes = stats.sumOf { it.nodes } + 1
        )
    }

    override fun visit(expr: UtMkTermArrayExpression): NestStat {
        return NestStat()
    }

    override fun visit(expr: UtArrayInsert): NestStat {
        return NestStat()
    }

    override fun visit(expr: UtArrayInsertRange): NestStat {
        return NestStat()
    }

    override fun visit(expr: UtArrayRemove): NestStat {
        return NestStat()
    }

    override fun visit(expr: UtArrayRemoveRange): NestStat {
        return NestStat()
    }

    override fun visit(expr: UtArraySetRange): NestStat {
        return NestStat()
    }

    override fun visit(expr: UtArrayShiftIndexes): NestStat {
        return NestStat()
    }

    override fun visit(expr: UtArrayApplyForAll): NestStat {
        return NestStat()
    }
}

data class NestStat(var nodes: Int = 1, var level: Int = 1)
