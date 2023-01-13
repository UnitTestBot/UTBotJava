package new

import api.AbstractFunctionEntity
import framework.api.js.JsClassId
import org.utbot.fuzzer.FuzzedConcreteValue

class JsFunctionEntity(
    override val name: String,
    override val parametersMap: Map<String, JsClassId>,
    override val concreteValues: Set<FuzzedConcreteValue>,
): AbstractFunctionEntity(name, parametersMap, concreteValues) {
}