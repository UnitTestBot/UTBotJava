package org.utbot.instrumentation.instrumentation.execution.data

import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.UtInstrumentation

/**
 * Consists of the data needed to execute the method concretely. Also includes method arguments stored in models.
 *
 * @property [stateBefore] is necessary for construction of parameters of a concrete call.
 * @property [instrumentation] is necessary for mocking static methods and new instances.
 * @property [timeout] is timeout for specific concrete execution (in milliseconds).
 * @property [specification] is strategy for controlling of results handling and validation (null means "without any validation").
 * By default, is initialized from [UtSettings.concreteExecutionTimeoutInInstrumentedProcess]
 */
data class UtConcreteExecutionData(
    val stateBefore: EnvironmentModels,
    val instrumentation: List<UtInstrumentation>,
    val timeout: Long = UtSettings.concreteExecutionTimeoutInInstrumentedProcess,
    val specification: UtConcreteExecutionSpecification? = null,
)