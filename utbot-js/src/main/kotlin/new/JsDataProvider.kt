package new

import api.AbstractFileEntity
import api.LanguageDataProvider
import api.NodeDynamicSettings

class JsDataProvider(
    sourceFilePath: String,
    projectPath: String,
    selectedMethods: List<String>?,
    parentClassName: String?,
    settings: NodeDynamicSettings,
    fileEntity: AbstractFileEntity
) : LanguageDataProvider(sourceFilePath, projectPath, selectedMethods, parentClassName, settings, fileEntity) {

}