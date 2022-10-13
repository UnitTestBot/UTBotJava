package org.utbot.framework.process

import com.jetbrains.rd.framework.IProtocol
import kotlinx.coroutines.runBlocking
import mu.KLogger
import org.utbot.common.SettingsContainer
import org.utbot.common.SettingsContainerFactory
import org.utbot.framework.process.generated.SettingForArgument
import org.utbot.framework.process.generated.settingsModel
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RdSettingsContainerFactory(private val protocol: IProtocol) : SettingsContainerFactory {
    override fun createSettingsContainer(
        logger: KLogger,
        defaultKeyForSettingsPath: String,
        defaultSettingsPath: String?
    ): SettingsContainer {
        return RdSettingsContainer(logger, defaultKeyForSettingsPath, protocol)
    }
}

class RdSettingsContainer(val logger: KLogger, val key: String, val protocol: IProtocol): SettingsContainer {

    override fun <T> settingFor(
        defaultValue: T,
        converter: (String) -> T
    ): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> {
        return PropertyDelegateProvider { _, prop ->
            object: ReadWriteProperty<Any?, T> {
                override fun getValue(thisRef: Any?, property: KProperty<*>): T = runBlocking {
                    return@runBlocking protocol.settingsModel.settingFor.startSuspending(SettingForArgument(key, property.name)).value?.let {
                        converter(it)
                    } ?: defaultValue
                }

                override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                    throw NotImplementedError("Setting properties from child process not supported")
                }
            }
        }
    }
}