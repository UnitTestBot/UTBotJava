package framework.codegen.model.constructor.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgConstructorCall
import org.utbot.framework.codegen.domain.models.CgExecutableCall
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgMethodCall
import org.utbot.framework.codegen.domain.models.CgStaticFieldAccess
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.services.access.CgIncompleteMethodCall
import org.utbot.framework.codegen.util.resolve
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId

class JsCgCallableAccessManager(context: CgContext) : CgCallableAccessManager,
    CgContextOwner by context {

    override operator fun CgExpression?.get(methodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(methodId, this)

    override operator fun ClassId.get(staticMethodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(staticMethodId, null)

    override fun CgExpression.get(fieldId: FieldId): CgExpression {
        TODO("Not yet implemented")
    }

    override fun ClassId.get(fieldId: FieldId): CgStaticFieldAccess {
        TODO("Not yet implemented")
    }

    override operator fun ConstructorId.invoke(vararg args: Any?): CgExecutableCall {
        val resolvedArgs = args.resolve()
        val constructorCall = CgConstructorCall(this, resolvedArgs)
        newConstructorCall(this)
        return constructorCall
    }

    override fun CgIncompleteMethodCall.invoke(vararg args: Any?): CgMethodCall {
        val resolvedArgs = args.resolve()
        val methodCall = CgMethodCall(caller, method, resolvedArgs)
        newMethodCall(method)
        return methodCall
    }

    private fun newConstructorCall(constructorId: ConstructorId) {
        importedClasses += constructorId.classId
    }

    private fun newMethodCall(methodId: MethodId) {
        if (methodId.classId.name == "undefined") {
            importedStaticMethods += methodId
            return
        }
        importedClasses += methodId.classId
    }
}