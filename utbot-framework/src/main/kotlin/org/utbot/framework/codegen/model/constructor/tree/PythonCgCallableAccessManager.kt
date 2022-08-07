package org.utbot.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.CgComponents
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgMethodCall
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.PythonMethodId

internal class PythonCgCallableAccessManagerImpl(val context: CgContext) : CgCallableAccessManager,
    CgContextOwner by context {

    private val statementConstructor by lazy { CgComponents.getStatementConstructorBy(context) }

    private val variableConstructor by lazy { CgComponents.getVariableConstructorBy(context) }

    override fun CgExpression?.get(methodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(methodId, this)

    override fun ClassId.get(staticMethodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(staticMethodId, null)

    override fun ConstructorId.invoke(vararg args: Any?): CgExecutableCall {
        TODO("Not yet implemented")
    }

    override fun CgIncompleteMethodCall.invoke(vararg args: Any?): CgMethodCall {
        val resolvedArgs = args.resolve()
        val methodCall = CgMethodCall(caller, method, resolvedArgs)
        newMethodCall(method)
        return methodCall
    }

    private fun newMethodCall(methodId: MethodId) {
//        importIfNeeded(methodId)
    }

}
