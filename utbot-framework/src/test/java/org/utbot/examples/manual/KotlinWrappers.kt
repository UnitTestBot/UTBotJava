package org.utbot.examples.manual

import org.utbot.common.FileUtil
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.findField
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.FieldId
import java.nio.file.Path

object SootUtils {
    @JvmStatic
    fun runSoot(clazz: Class<*>) {
        val buildDir = FileUtil.locateClassPath(clazz.kotlin) ?: FileUtil.isolateClassFiles(clazz.kotlin)
        val buildDirPath = buildDir.toPath()

        if (buildDirPath != previousBuildDir) {
            org.utbot.framework.util.runSoot(buildDirPath, null)
            previousBuildDir = buildDirPath
        }
    }

    private var previousBuildDir: Path? = null
}

fun fields(
    classId: ClassId,
    vararg fields: Pair<String, Any>
): MutableMap<FieldId, UtModel> {
    return fields
        .associate {
            val fieldId = classId.findField(it.first)
            val fieldValue = when (val value = it.second) {
                is UtModel -> value
                else -> UtPrimitiveModel(value)
            }
            fieldId to fieldValue
        }
        .toMutableMap()
}