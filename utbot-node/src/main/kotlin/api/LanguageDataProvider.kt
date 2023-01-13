package api

interface LanguageDataOwner {
    val sourceFilePath: String
    val projectPath: String
    val selectedMethods: List<String>?
    val parentClassName: String?
    val settings: NodeDynamicSettings
    val fileEntity: AbstractFileEntity
}

open class LanguageDataProvider(
    override val sourceFilePath: String,
    override val projectPath: String,
    override val selectedMethods: List<String>? = null,
    override val parentClassName: String? = null,
    override val settings: NodeDynamicSettings,
    override val fileEntity: AbstractFileEntity
): LanguageDataOwner