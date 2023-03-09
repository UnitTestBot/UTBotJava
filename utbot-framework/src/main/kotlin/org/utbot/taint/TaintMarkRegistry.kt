package org.utbot.taint

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.utbot.taint.model.TaintMark

/**
 * Conversion between the taint mark name and 64-bit taint vector (some power of 2).
 * Can contain a maximum of 64 different marks.
 */
class TaintMarkRegistry {

    fun idByMark(mark: TaintMark): Long =
        taintMarkToIdBiMap.getOrPut(mark) {
            nextId.also { nextId *= 2 }
        }

    fun containsMark(mark: TaintMark): Boolean =
        mark in taintMarkToIdBiMap

    fun markById(id: Long): TaintMark = // TODO: return null?
        taintMarkToIdBiMap.inverse()[id] ?: error("Unknown mark id: $id")

    // internal

    private var nextId: Long = 1 // 2 ** 0

    private val taintMarkToIdBiMap: BiMap<TaintMark, Long> = HashBiMap.create()
}