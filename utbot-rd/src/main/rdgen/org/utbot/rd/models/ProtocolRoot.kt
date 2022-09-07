package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object ProtocolRoot : Root()

object ProtocolModel : Ext(ProtocolRoot) {
    val AddPathsParams = structdef {
        field("pathsToUserClasses", PredefinedType.string)
        field("pathsToDependencyClasses", PredefinedType.string)
    }

    val SetInstrumentationParams = structdef {
        field("instrumentation", array(PredefinedType.byte))
    }

    val InvokeMethodCommandParams = structdef {
        field("classname", PredefinedType.string)
        field("signature", PredefinedType.string)
        field("arguments", array(PredefinedType.byte))
        field("parameters", array(PredefinedType.byte))
    }

    val InvokeMethodCommandResult = structdef {
        field("result", array(PredefinedType.byte))
    }

    val CollectCoverageParams = structdef {
        field("clazz", array(PredefinedType.byte))
    }

    val CollectCoverageResult = structdef {
        field("coverageInfo", array(PredefinedType.byte))
    }

    init {
        call("AddPaths", AddPathsParams, PredefinedType.void).apply {
            async
            documentation =
                "The main process tells where the child process should search for the classes"
        }
        call("Warmup", PredefinedType.void, PredefinedType.void).apply {
            async
            documentation =
                "Load classes from classpath and instrument them"
        }
        call("SetInstrumentation", SetInstrumentationParams, PredefinedType.void).apply {
            async
            documentation =
                "The main process sends [instrumentation] to the child process"
        }
        call("InvokeMethodCommand", InvokeMethodCommandParams, InvokeMethodCommandResult).apply {
            async
            documentation =
            "The main process requests the child process to execute a method with the given [signature],\n" +
                    "which declaring class's name is [className].\n" +
                    "@property parameters are the parameters needed for an execution, e.g. static environment"
        }
        call("StopProcess", PredefinedType.void, PredefinedType.void).apply {
            async
            documentation =
                "This command tells the child process to stop"
        }
        call("CollectCoverage", CollectCoverageParams, CollectCoverageResult).apply {
            async
            documentation =
                "This command is sent to the child process from the [ConcreteExecutor] if user wants to collect coverage for the\n" +
                        "[clazz]"
        }
    }
}