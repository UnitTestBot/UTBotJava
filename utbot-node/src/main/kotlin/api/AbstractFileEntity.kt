package api

import java.io.File

// TODO: May be lazy
abstract class AbstractFileEntity(
    open val path: File,
    open val topLevelFunctions: List<AbstractFunctionEntity>,
    open val classes: List<AbstractClassEntity>,
    open val imports: List<AbstractImportsEntity>,
) {
}
