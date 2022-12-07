@file:Suppress("unused")
package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object CSharpRoot: Root()

object VSharpModel: Ext(CSharpRoot) {
    val generateArguments = structdef {
        field("assemblyPath", PredefinedType.string)
        field("projectCsprojPath", PredefinedType.string)
        field("solutionFilePath", PredefinedType.string)
        field("moduleFqnName", PredefinedType.string)
        field("methodToken", PredefinedType.int)
        field("generationTimeoutInSeconds", PredefinedType.int)
    }

    val generateResults = structdef {
        field("generatedProjectPath", PredefinedType.string)
        field("generatedFilesPaths", array(PredefinedType.string))
    }

    init {
        call("generate", generateArguments, generateResults).async
        signal("ping", PredefinedType.string).async
    }
}