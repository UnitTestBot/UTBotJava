@file:Suppress("unused")
package org.utbot.rd.models

import com.jetbrains.rd.generator.nova.*

object InstrumentedProcessRoot : Root()

object InstrumentedProcessModel : Ext(InstrumentedProcessRoot) {
    val AddPathsParams = structdef {
        field("pathsToUserClasses", PredefinedType.string)
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

    val ComputeStaticFieldParams = structdef {
        field("fieldId", array(PredefinedType.byte))
    }

    val ComputeStaticFieldResult = structdef {
        field("result", array(PredefinedType.byte))
    }

    val GetSpringBeanParams = structdef {
        field("beanName", PredefinedType.string)
    }

    val GetSpringBeanResult = structdef {
        field("beanModel", array(PredefinedType.byte))
    }

    val GetSpringRepositoriesParams = structdef {
        field("classId", array(PredefinedType.byte))
    }

    val GetSpringRepositoriesResult = structdef {
        field("springRepositoryIds", array(PredefinedType.byte))
    }

    init {
        call("AddPaths", AddPathsParams, PredefinedType.void).apply {
            async
            documentation =
                "The main process tells where the instrumented process should search for the classes"
        }
        call("Warmup", PredefinedType.void, PredefinedType.void).apply {
            async
            documentation =
                "Load classes from classpath and instrument them"
        }
        call("SetInstrumentation", SetInstrumentationParams, PredefinedType.void).apply {
            async
            documentation =
                "The main process sends [instrumentation] to the instrumented process"
        }
        call("InvokeMethodCommand", InvokeMethodCommandParams, InvokeMethodCommandResult).apply {
            async
            documentation =
            "The main process requests the instrumented process to execute a method with the given [signature],\n" +
                    "which declaring class's name is [className].\n" +
                    "@property parameters are the parameters needed for an execution, e.g. static environment"
        }
        call("CollectCoverage", CollectCoverageParams, CollectCoverageResult).apply {
            async
            documentation =
                "This command is sent to the instrumented process from the [ConcreteExecutor] if user wants to collect coverage for the\n" +
                        "[clazz]"
        }
        call("ComputeStaticField", ComputeStaticFieldParams, ComputeStaticFieldResult).apply {
            async
            documentation =
                "This command is sent to the instrumented process from the [ConcreteExecutor] if user wants to get value of static field\n" +
                        "[fieldId]"
        }
        call("GetSpringBean", GetSpringBeanParams, GetSpringBeanResult).apply {
            async
            documentation = "Gets Spring bean by name (requires Spring instrumentation)"
        }
        call("getRelevantSpringRepositories", GetSpringRepositoriesParams, GetSpringRepositoriesResult).apply {
            async
            documentation = "Get Spring repositories by bean names (requires Spring instrumentation)"
        }
    }
}