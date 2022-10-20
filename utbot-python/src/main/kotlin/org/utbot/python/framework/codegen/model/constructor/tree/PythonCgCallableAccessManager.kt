package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.tree.CgCallableAccessManager
import org.utbot.framework.codegen.model.constructor.tree.CgIncompleteMethodCall
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.plugin.api.*
import org.utbot.python.framework.api.python.PythonMethodId
import org.utbot.python.framework.api.python.pythonAnyClassId
import org.utbot.framework.plugin.api.util.exceptions
import org.utbot.python.framework.codegen.model.constructor.util.importIfNeeded

class PythonCgCallableAccessManagerImpl(val context: CgContext) : CgCallableAccessManager,
    CgContextOwner by context {

    override fun CgExpression?.get(methodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(methodId, this)

    override fun ClassId.get(staticMethodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(staticMethodId, CgThisInstance(pythonAnyClassId))

    override fun CgExpression.get(fieldId: FieldId): CgExpression {
        TODO("Not yet implemented")
    }

    override fun ClassId.get(fieldId: FieldId): CgStaticFieldAccess {
        TODO("Not yet implemented")
    }

    override fun ConstructorId.invoke(vararg args: Any?): CgExecutableCall {
        val resolvedArgs = args.resolve()
        val constructorCall = CgConstructorCall(this, resolvedArgs)
        newConstructorCall(this)
        return constructorCall
    }

    override fun CgIncompleteMethodCall.invoke(vararg args: Any?): CgMethodCall {
        val resolvedArgs = args.resolve()
        val methodCall = CgMethodCall(caller, method, resolvedArgs)
        if (method is PythonMethodId)
            newMethodCall(method)
        return methodCall
    }

    private fun newMethodCall(methodId: MethodId) {
        importIfNeeded(methodId as PythonMethodId)
    }

    private fun newConstructorCall(constructorId: ConstructorId) {
        importIfNeeded(constructorId.classId)
        for (exception in constructorId.exceptions) {
            addExceptionIfNeeded(exception)
        }
    }

}
