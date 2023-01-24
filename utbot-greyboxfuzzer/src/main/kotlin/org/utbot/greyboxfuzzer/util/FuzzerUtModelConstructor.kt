package org.utbot.greyboxfuzzer.util

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel

data class FuzzerUtModelConstructor(
    val construct: (Any?, ClassId) -> UtModel,
    val computeUnusedIdAndUpdate: () -> Int
)