package org.utbot.framework.process

import kotlinx.coroutines.runBlocking
import mu.KLogger
import org.utbot.common.SettingsContainer
import org.utbot.common.SettingsContainerFactory
import org.utbot.framework.process.generated.SettingForArgument
import org.utbot.framework.process.generated.SettingsModel
import org.utbot.rd.startBlocking
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
        converter: (String) -> T
    ): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> {
        return PropertyDelegateProvider { _, _ ->
            object : ReadWriteProperty<Any?, T> {
                override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                    val params = SettingForArgument(key, property.name)
                    return settingsModel.settingFor.startBlocking(params).value?.let {
                        converter(it)
                    } ?: defaultValue
                }

                override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                    throw NotImplementedError("Setting properties allowed only from plugin process")
                }
            }
        }
    }
}