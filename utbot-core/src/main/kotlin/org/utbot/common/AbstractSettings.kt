package org.utbot.common

import java.io.FileInputStream
import java.io.IOException
import java.util.*
import mu.KLogger
import org.utbot.common.PathUtil.toPath
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty

abstract class AbstractSettings(
    private val logger: KLogger,
    defaultKeyForSettingsPath: String,
    defaultSettingsPath: String? = null
) {
    protected val properties = Properties().also { props ->
        val settingsPath = System.getProperty(defaultKeyForSettingsPath) ?: defaultSettingsPath
        val settingsPathFile = settingsPath?.toPath()?.toFile()
        if (settingsPathFile?.exists() == true) {
            try {
                FileInputStream(settingsPathFile).use { reader ->
                    props.load(reader)
                }
            } catch (e: IOException) {
                logger.info(e) { e.message }
            }
        }
    }

    protected val settingsValues: MutableMap<KProperty<*>, Any?> = mutableMapOf()

    inner class SettingDelegate<T>(val property: KProperty<*>, val initializer: () -> T) {
        private var value = initializer()

        init {
            updateSettingValue()
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            this.value = value
            updateSettingValue()
        }

        private fun updateSettingValue() {
            settingsValues[property] = value
        }
    }

    protected fun <T> getProperty(
        defaultValue: T,
        converter: (String) -> T
    ): PropertyDelegateProvider<Any?, SettingDelegate<T>> {
        return PropertyDelegateProvider { _, property ->
            SettingDelegate(property) {
                try {
                    properties.getProperty(property.name)?.let(converter) ?: defaultValue
                } catch (e: Throwable) {
                    logger.info(e) { e.message }
                    defaultValue
                }
            }
        }
    }

    protected fun getBooleanProperty(defaultValue: Boolean) = getProperty(defaultValue, String::toBoolean)
    protected fun getIntProperty(defaultValue: Int) = getProperty(defaultValue, String::toInt)
    protected fun getLongProperty(defaultValue: Long) = getProperty(defaultValue, String::toLong)
    protected fun getStringProperty(defaultValue: String) = getProperty(defaultValue) { it }
    protected inline fun <reified T : Enum<T>> getEnumProperty(defaultValue: T) =
        getProperty(defaultValue) { enumValueOf(it) }

    override fun toString(): String =
        settingsValues
            .mapKeys { it.key.name }
            .entries
            .sortedBy { it.key }
            .joinToString(separator = System.lineSeparator()) { "\t${it.key}=${it.value}" }
}