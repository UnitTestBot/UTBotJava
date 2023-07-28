package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.AnnotationTarget
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.services.access.CgCallableAccessManagerImpl
import org.utbot.framework.codegen.tree.CgComponents.getStatementConstructorBy
import org.utbot.framework.plugin.api.UtCustomModel
import org.utbot.framework.plugin.api.UtSpringMockMvcResultActionsModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.autoConfigureMockMvcClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.contentMatchersStringMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.mockMvcResultHandlersClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.mockMvcResultMatchersClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.resultActionsAndDoMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.resultActionsAndExpectMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.resultHandlersPrintMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.resultMatchersContentMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.resultMatchersStatusMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.resultMatchersViewMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.statusMatchersIsMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.viewMatchersNameMethodId

fun CgCustomAssertConstructor.withCustomAssertForMockMvcResultActions() =
    CgMockMvcResultActionsAssertConstructor(context, this)

class CgMockMvcResultActionsAssertConstructor(
    context: CgContext,
    private val delegateAssertConstructor: CgCustomAssertConstructor
) : CgCustomAssertConstructor by delegateAssertConstructor,
    CgContextOwner by context,
    CgStatementConstructor by getStatementConstructorBy(context),
    CgCallableAccessManager by CgCallableAccessManagerImpl(context) {
    override fun tryConstructCustomAssert(expected: UtCustomModel, actual: CgVariable): Boolean {
        if (expected is UtSpringMockMvcResultActionsModel) {
            addAnnotation(autoConfigureMockMvcClassId, AnnotationTarget.Class)
            var expr = actual[resultActionsAndDoMethodId](mockMvcResultHandlersClassId[resultHandlersPrintMethodId]())
            expr = expr[resultActionsAndExpectMethodId](
                mockMvcResultMatchersClassId[resultMatchersStatusMethodId]()[statusMatchersIsMethodId](expected.status)
            )
            expected.viewName?.let { viewName ->
                expr = expr[resultActionsAndExpectMethodId](
                    mockMvcResultMatchersClassId[resultMatchersViewMethodId]()[viewMatchersNameMethodId](viewName)
                )
            }
            expr = expr[resultActionsAndExpectMethodId](
                mockMvcResultMatchersClassId[resultMatchersContentMethodId]()[contentMatchersStringMethodId](expected.contentAsString)
            )
            +expr
            return true
        } else
            return delegateAssertConstructor.tryConstructCustomAssert(expected, actual)
    }
}