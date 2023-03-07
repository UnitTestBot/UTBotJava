package service

import com.google.javascript.rhino.Node
import settings.JsDynamicSettings

class ServiceContext(
    override val utbotDir: String,
    override val projectPath: String,
    override val filePathToInference: String,
    override val parsedFile: Node,
    override val settings: JsDynamicSettings,
    override var packageJson: PackageJson = PackageJson.defaultConfig
) : ContextOwner

interface ContextOwner {
    val utbotDir: String
    val projectPath: String
    val filePathToInference: String
    val parsedFile: Node
    val settings: JsDynamicSettings
    var packageJson: PackageJson
}
