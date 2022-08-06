package org.utbot.framework.plugin.api.util

import org.utbot.framework.UtSettings

/**
 * Runs [block] with [UtSettings.substituteStaticsWithSymbolicVariable] value
 * modified in accordance with given [condition].
 */
inline fun <T> withStaticsSubstitutionRequired(condition: Boolean, block: () -> T) {
    val standardSubstitutionSetting = UtSettings.substituteStaticsWithSymbolicVariable
    UtSettings.substituteStaticsWithSymbolicVariable = standardSubstitutionSetting && condition
    try {
        block()
    } finally {
        UtSettings.substituteStaticsWithSymbolicVariable = standardSubstitutionSetting
    }
}