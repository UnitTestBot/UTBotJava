@file:Suppress("unused")
package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object CSharpRoot: Root()

object VSharpModel: Ext(CSharpRoot) {
    val methodDescriptor = structdef {
        field("methodName", PredefinedType.string)
        field("typeName", PredefinedType.string)
        field("hasNoOverloads", PredefinedType.bool)
        field("parameters", immutableList(PredefinedType.string))
    }

    val mapEntry = structdef {
        field("key", PredefinedType.string)
        field("value", PredefinedType.string)
    }

    val generateArguments = structdef {
        field("assemblyPath", PredefinedType.string)
        field("projectCsprojPath", PredefinedType.string)
        field("solutionFilePath", PredefinedType.string)
        field("methods", immutableList(methodDescriptor))
        field("generationTimeoutInSeconds", PredefinedType.int)
        field("targetFramework", PredefinedType.string.nullable)
        field("assembliesFullNameToTheirPath", immutableList(mapEntry))
    }

    val generateResults = structdef {
        field("generatedProjectPath", PredefinedType.string.nullable)
        field("generatedFilesPaths", immutableList(PredefinedType.string))
        field("exceptionMessage", PredefinedType.string.nullable)
        field("testsCount", PredefinedType.int)
        field("errorsCount", PredefinedType.int)
    }

    init {
        call("generate", generateArguments, generateResults).async
        signal("ping", PredefinedType.string).async
        signal("log", PredefinedType.string).async
    }
}
