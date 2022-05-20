package org.utbot.framework

import java.nio.file.Path
import java.nio.file.Paths

open class JdkPathDefaultProvider: JdkPathProvider() {
    override val jdkPath: Path
        get() = Paths.get(System.getProperty("java.home"))
}
