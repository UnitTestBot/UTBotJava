package api

interface LanguageDataOwner {
    val sourceFilePath: String
    val projectPath: String
    val selectedMethods: List<String>?
    val parentClassName: String?
    val settings: DynamicSettings
}

data class LanguageDataProvider(
    override val sourceFilePath: String,
    override val projectPath: String,
    override val selectedMethods: List<String>? = null,
    override val parentClassName: String? = null,
    override val settings: DynamicSettings
): LanguageDataOwner