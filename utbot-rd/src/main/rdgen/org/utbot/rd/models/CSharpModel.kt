@file:Suppress("unused")
package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object CSharpRoot: Root()

object VSharpModel: Ext(CSharpRoot) {
    val methodDescriptor = structdef {
        field("methodName", PredefinedType.string)
        field("typeName", PredefinedType.string)
    }

    val generateArguments = structdef {
        field("assemblyPath", PredefinedType.string)
        field("projectCsprojPath", PredefinedType.string)
        field("solutionFilePath", PredefinedType.string)
        field("method", methodDescriptor)
        field("generationTimeoutInSeconds", PredefinedType.int)
        field("targetFramework", PredefinedType.string.nullable)
    }

    val generateResults = structdef {
        field("isGenerated", PredefinedType.bool)
        field("generatedProjectPath", PredefinedType.string)
        field("generatedFilesPaths", array(PredefinedType.string))
        field("exceptionMessage", PredefinedType.string.nullable)
    }

    init {
        call("generate", generateArguments, generateResults).async
        signal("ping", PredefinedType.string).async
        signal("log", PredefinedType.string).async
    }
}