package new

import api.AbstractFileEntity
import java.io.File

class JsFileEntity(
    override val path: File,
    override val topLevelFunctions: List<JsFunctionEntity>,
    override val classes: List<JsClassEntity>,
    override val imports: List<JsImportsEntity>,
): AbstractFileEntity(path, topLevelFunctions, classes, imports) {
}