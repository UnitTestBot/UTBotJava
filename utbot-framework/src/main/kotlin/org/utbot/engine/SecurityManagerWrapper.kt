package org.utbot.engine

import org.utbot.engine.overrides.security.UtSecurityManager
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.util.nextModelName
import soot.Scene
import soot.SootClass
import soot.SootMethod

class SecurityManagerWrapper : BaseOverriddenWrapper(utSecurityManagerClass.name) {
    private val baseModelName: String = "securityManager"

    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? {
        // We currently do not overload any [SecurityManager] method symbolically
        return null
    }

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel = resolver.run {
        val classId = wrapper.type.classId
        val addr = holder.concreteAddr(wrapper.addr)
        val modelName = nextModelName(baseModelName)

        val instantiationChain = mutableListOf<UtStatementModel>()
        val modificationChain = mutableListOf<UtStatementModel>()
        return UtAssembleModel(addr, classId, modelName, instantiationChain, modificationChain)
    }

    companion object {
        val utSecurityManagerClass: SootClass
            get() = Scene.v().getSootClass(UtSecurityManager::class.qualifiedName)
    }
}
