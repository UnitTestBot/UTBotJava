package org.utbot.rd

import mu.KLogger
import org.utbot.common.SettingsContainer
import org.utbot.common.SettingsContainerFactory
import org.utbot.rd.generated.SettingForArgument
import org.utbot.rd.generated.SettingsModel
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RdSettingsContainerFactory(private val settingsModel: SettingsModel) : SettingsContainerFactory {
    override fun createSettingsContainer(
        logger: KLogger,
        defaultKeyForSettingsPath: String,
        defaultSettingsPath: String?
    ): SettingsContainer {
        return RdSettingsContainer(logger, defaultKeyForSettingsPath, settingsModel)
    }
}

class RdSettingsContainer(val logger: KLogger, val key: String, val settingsModel: SettingsModel) : SettingsContainer {

    override fun <T> settingFor(
        defaultValue: T,
        range : Triple<T, T, Comparator<T>>?,
        converter: (String) -> T
    ): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> {
        return PropertyDelegateProvider { _, _ ->
            object : ReadWriteProperty<Any?, T> {
                override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                    val params = SettingForArgument(key, property.name)
                    return settingsModel.settingFor.startBlocking(params).value?.let {
                        try {
                            return range?.run {
                                // Coerce parsed value into the specified range
                                minOf(
                                    range.second,
                                    maxOf(converter(it), range.first, range.third),
                                    range.third
                                )
                            } ?: converter(it)
                        } catch (e: Exception) {
                            logger.warn("Cannot parse value for $key, default value [$defaultValue] will be used instead") { e }
                            defaultValue
                        }
                    } ?: defaultValue
                }

                override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                    throw IllegalStateException("Setting properties allowed only from plugin process")
                }
            }
        }
    }
}