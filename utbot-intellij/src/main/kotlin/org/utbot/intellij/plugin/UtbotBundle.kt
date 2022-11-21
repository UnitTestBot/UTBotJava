package org.utbot.intellij.plugin

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

class UtbotBundle : DynamicBundle(BUNDLE) {
    companion object {
        @NonNls
        private const val BUNDLE = "bundles.UtbotBundle"
        private val INSTANCE: UtbotBundle = UtbotBundle()

        fun message(
            @PropertyKey(resourceBundle = BUNDLE) key: String,
            vararg params: Any
        ): String {
            return INSTANCE.getMessage(key, *params)
        }

        fun takeIf(
            @PropertyKey(resourceBundle = BUNDLE) key: String,
            vararg params: Any,
            condition: () -> Boolean): String? {
            if (condition())
                return message(key, params)
            return null
        }
    }
}