package org.utbot.common

import java.io.FileInputStream
import java.io.IOException
import java.util.*
import mu.KLogger
import org.utbot.common.PathUtil.toPath
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface SettingsContainer {
    fun <T> settingFor(defaultValue: T, converter: (String) -> T): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>>
}

interface SettingsContainerFactory {
    fun createSettingsContainer(
        logger: KLogger,
        defaultKeyForSettingsPath: String,
        defaultSettingsPath: String? = null) : SettingsContainer
}

class PropertiesSettingsContainer(
    private val logger: KLogger,
    defaultKeyForSettingsPath: String,
    defaultSettingsPath: String? = null): SettingsContainer {
    companion object: SettingsContainerFactory {
        override fun createSettingsContainer(
            logger: KLogger,
            defaultKeyForSettingsPath: String,
            defaultSettingsPath: String?
        ): SettingsContainer = PropertiesSettingsContainer(logger, defaultKeyForSettingsPath, defaultSettingsPath)
    }

    private val properties = Properties().also { props ->
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

    private val settingsValues: MutableMap<KProperty<*>, Any?> = mutableMapOf()

    inner class SettingDelegate<T>(val property: KProperty<*>, val initializer: () -> T): ReadWriteProperty<Any?, T> {
        private var value = initializer()

        init {
            updateSettingValue()
        }

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            this.value = value
            updateSettingValue()
        }

        private fun updateSettingValue() {
            settingsValues[property] = value
        }
    }

    override fun <T> settingFor(
        defaultValue: T,
        converter: (String) -> T
    ): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> {
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

    override fun toString(): String =
        settingsValues
            .mapKeys { it.key.name }
            .entries
            .sortedBy { it.key }
            .joinToString(separator = System.lineSeparator()) { "\t${it.key}=${it.value}" }
}

abstract class AbstractSettings(
    logger: KLogger,
    defaultKeyForSettingsPath: String,
    defaultSettingsPath: String? = null) {
    private val container: SettingsContainer = createSettingsContainer(logger, defaultKeyForSettingsPath, defaultSettingsPath)
    init {
        allSettings[defaultKeyForSettingsPath] = this
    }
    companion object : SettingsContainerFactory {
        val allSettings = mutableMapOf<String, AbstractSettings>()
        private var factory: SettingsContainerFactory? = null
        override fun createSettingsContainer(
            logger: KLogger,
            defaultKeyForSettingsPath: String,
            defaultSettingsPath: String?
        ): SettingsContainer {
            return (factory ?: PropertiesSettingsContainer).createSettingsContainer(logger, defaultKeyForSettingsPath, defaultSettingsPath)
        }

        fun setupFactory(factory: SettingsContainerFactory) {
            this.factory = factory
        }
    }

    protected fun <T> getProperty(
        defaultValue: T,
        converter: (String) -> T
    ): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> {
        return container.settingFor(defaultValue, converter)
    }

    protected fun getBooleanProperty(defaultValue: Boolean) = getProperty(defaultValue, String::toBoolean)
    protected fun getIntProperty(defaultValue: Int) = getProperty(defaultValue, String::toInt)
    protected fun getLongProperty(defaultValue: Long) = getProperty(defaultValue, String::toLong)
    protected fun getStringProperty(defaultValue: String) = getProperty(defaultValue) { it }
    protected inline fun <reified T : Enum<T>> getEnumProperty(defaultValue: T) =
        getProperty(defaultValue) { enumValueOf(it) }

    override fun toString(): String = container.toString()
}