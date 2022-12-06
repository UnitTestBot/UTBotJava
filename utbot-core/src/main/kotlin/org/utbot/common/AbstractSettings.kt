package org.utbot.common

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.Comparator
import mu.KLogger
import org.utbot.common.PathUtil.toPath
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface SettingsContainer {
    fun <T> settingFor(
        defaultValue: T,
        range : Triple<T, T, Comparator<T>>? = null,
        converter: (String) -> T
    ): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>>

    fun getInputStream() : InputStream? = null

    // Returns true iff some properties have non-default values
    fun isCustomized() = false
}

interface SettingsContainerFactory {
    fun createSettingsContainer(
        logger: KLogger,
        defaultKeyForSettingsPath: String,
        defaultSettingsPath: String? = null) : SettingsContainer
}

internal open class PropertiesSettingsContainer(
    private val logger: KLogger,
    val defaultKeyForSettingsPath: String,
    val defaultSettingsPath: String? = null): SettingsContainer {
    companion object: SettingsContainerFactory {
        override fun createSettingsContainer(
            logger: KLogger,
            defaultKeyForSettingsPath: String,
            defaultSettingsPath: String?
        ): SettingsContainer = PropertiesSettingsContainer(logger, defaultKeyForSettingsPath, defaultSettingsPath)
    }

    private val properties = Properties().also { props ->
        try {
            getInputStream()?.use {
                props.load(it)
            }
        } catch (e: IOException) {
            logger.info(e) { e.message }
        }
    }

    override fun getInputStream() : InputStream? {
        val settingsPath = System.getProperty(defaultKeyForSettingsPath) ?: defaultSettingsPath
        val settingsPathFile = settingsPath?.toPath()?.toFile()
        return if (settingsPathFile?.exists() == true) FileInputStream(settingsPathFile) else null
    }

    private val settingsValues: MutableMap<KProperty<*>, Any?> = mutableMapOf()
    private var customized: Boolean = false

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
        range : Triple<T, T, Comparator<T>>?,
        converter: (String) -> T
    ): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> {
        return PropertyDelegateProvider { _, property ->
            SettingDelegate(property) {
                try {
                    properties.getProperty(property.name)?.let {
                        var parsedValue = converter.invoke(it)
                        range?.let {
                            // Coerce parsed value into the specified range
                            parsedValue = maxOf(parsedValue, range.first, range.third)
                            parsedValue = minOf(parsedValue, range.second, range.third)
                        }
                        customized = customized or (parsedValue != defaultValue)
                        return@SettingDelegate parsedValue
                    }
                    defaultValue
                } catch (e: Throwable) {
                    logger.warn("Cannot parse value for ${property.name}, default value [$defaultValue] will be used instead") { e }
                    defaultValue
                }
            }
        }
    }

    override fun isCustomized(): Boolean {
        return customized
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

    fun areCustomized(): Boolean = container.isCustomized()

    protected fun <T> getProperty(
        defaultValue: T,
        range : Triple<T, T, Comparator<T>>?,
        converter: (String) -> T
    ): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> where T: Comparable<T> {
        return container.settingFor(defaultValue, range, converter)
    }

    protected fun <T> getProperty(
        defaultValue: T,
        converter: (String) -> T
    ): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> {
        return container.settingFor(defaultValue, null, converter)
    }

    protected fun getBooleanProperty(defaultValue: Boolean) = getProperty(defaultValue, converter = {
        //Invalid values shouldn't be parsed as "false"
        if (it.equals("true", true)) true
        else if (it.equals("false", true)) false
        else defaultValue
    })
    protected fun getIntProperty(defaultValue: Int) = getProperty(defaultValue, converter = String::toInt)
    protected fun getIntProperty(defaultValue: Int, minValue: Int, maxValue: Int) = getProperty(defaultValue, Triple(minValue, maxValue, Comparator(Integer::compare)), String::toInt)
    protected fun getLongProperty(defaultValue: Long) = getProperty(defaultValue, converter = String::toLong)
    protected fun getLongProperty(defaultValue: Long, minValue: Long, maxValue: Long) = getProperty(defaultValue, Triple(minValue, maxValue, Comparator(Long::compareTo)), String::toLong)
    protected fun getStringProperty(defaultValue: String) = getProperty(defaultValue) { it }
    protected inline fun <reified T : Enum<T>> getEnumProperty(defaultValue: T) =
        getProperty(defaultValue) { enumValueOf(it) }
    protected fun getListProperty(defaultValue: List<String>) =
        getProperty(defaultValue) { it.split(';') }
    protected inline fun <reified T> getListProperty(defaultValue: List<T>, crossinline elementTransform: (String) -> T) =
        getProperty(defaultValue) { it.split(';').map(elementTransform) }
    override fun toString(): String = container.toString()
}