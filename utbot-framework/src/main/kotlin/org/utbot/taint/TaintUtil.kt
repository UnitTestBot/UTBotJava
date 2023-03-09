package org.utbot.taint

import org.utbot.taint.model.*

object TaintUtil {

    /**
     * Chooses base, argN or result corresponding to [this] entity.
     */
    fun <T> TaintEntity.chooseRelatedValue(base: T, args: List<T>, result: T): T? =
        when (this) {
            TaintEntityThis -> base
            is TaintEntityArgument -> args.elementAtOrNull(index.toInt() - 1)
            TaintEntityReturn -> result
        }

    /**
     * Check if [this] does not contain marks.
     */
    fun TaintMarks.isEmpty(): Boolean =
        when (this) {
            TaintMarksAll -> false
            is TaintMarksSet -> marks.isEmpty()
        }
}