package org.utbot.framework

import java.nio.file.Path
import kotlin.reflect.KProperty

abstract class JdkPathProvider {
    operator fun getValue(service: JdkPathService, property: KProperty<*>): Path {
        return jdkPath
    }

    abstract val jdkPath: Path

    abstract val jdkVersion: String
}
