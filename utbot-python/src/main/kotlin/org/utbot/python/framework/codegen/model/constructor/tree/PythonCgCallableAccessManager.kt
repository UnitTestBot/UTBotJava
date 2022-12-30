package org.utbot.python.framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgConstructorCall
import org.utbot.framework.codegen.domain.models.CgExecutableCall
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgFieldAccess
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgStaticFieldAccess
import org.utbot.framework.codegen.domain.models.CgThisInstance
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.services.access.CgIncompleteMethodCall
import org.utbot.framework.codegen.tree.importIfNeeded
import org.utbot.framework.codegen.util.resolve
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.exceptions
import org.utbot.python.framework.api.python.PythonMethodId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.python.framework.codegen.model.tree.CgPythonTree

class PythonCgCallableAccessManagerImpl(val context: CgContext) : CgCallableAccessManager,
    CgContextOwner by context {

    override fun CgExpression?.get(methodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(methodId, this)

    override fun ClassId.get(staticMethodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(staticMethodId, CgThisInstance(pythonAnyClassId))

    override fun CgExpression.get(fieldId: FieldId): CgExpression {
        return CgFieldAccess(this, fieldId)
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
        val resolvedArgs = emptyList<CgExpression>().toMutableList()
        args.forEach { arg ->
            if (arg is CgPythonTree) {
                resolvedArgs.add(arg.value)
//                arg.children.forEach { +it }
            } else {
                resolvedArgs.add(arg as CgExpression)
            }
        }
//        resolvedArgs.forEach {
//            if (it is CgPythonTree) {
//                it.children.forEach { child ->
//                    if (child is CgAssignment) {
//                        if (!existingVariableNames.contains(child.lValue.toString())) {
//                            +child
//                        }
//                    } else {
//                        +child
//                    }
//                }
//            }
//        }
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
