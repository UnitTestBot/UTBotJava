package api

import org.utbot.framework.plugin.api.ClassId
import org.utbot.fuzzer.FuzzedConcreteValue

abstract class AbstractFunctionEntity(
    val name: String,
    val parametersMap: Map<String, ClassId>,
    val concreteValues: Set<FuzzedConcreteValue>,
) {
}