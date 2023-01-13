package api

import org.utbot.framework.plugin.api.ClassId
import org.utbot.fuzzer.FuzzedConcreteValue

abstract class AbstractFunctionEntity(
    open val name: String,
    open val parametersMap: Map<String, ClassId>,
    open val concreteValues: Set<FuzzedConcreteValue>,
) {
}