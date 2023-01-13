package new

import api.AbstractClassEntity
import api.AbstractFunctionEntity

class JsClassEntity(
    override val name: String,
    override val methods: List<JsFunctionEntity>
): AbstractClassEntity(name, methods) {
}