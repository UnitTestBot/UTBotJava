package org.utbot.instrumentation.instrumentation.spring

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSpringMockMvcResultActionsModel
import org.utbot.framework.plugin.api.util.SpringModelUtils.modelAndViewGetModelMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.modelAndViewGetViewNameMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.mvcResultGetModelAndViewMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.mvcResultGetResponseMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.responseGetContentAsStringMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.responseGetErrorMessageMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.responseGetStatusMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.resultActionsAndReturnMethodId
import org.utbot.framework.plugin.api.util.mapClassId
import org.utbot.framework.plugin.api.util.method
import org.utbot.instrumentation.instrumentation.execution.constructors.UtCustomModelConstructor
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructorInterface

class UtMockMvcResultActionsModelConstructor : UtCustomModelConstructor {
    override fun constructCustomModel(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        valueClassId: ClassId,
        id: Int?,
        saveToCache: (UtModel) -> Unit
    ): UtModel {
        val mvcResult = resultActionsAndReturnMethodId.method.invoke(value)
        val response = mvcResultGetResponseMethodId.method.invoke(mvcResult)
        val modelAndView = mvcResultGetModelAndViewMethodId.method.invoke(mvcResult)

        return UtSpringMockMvcResultActionsModel(
            id = id,
            status = responseGetStatusMethodId.method.invoke(response) as Int,
            errorMessage = responseGetErrorMessageMethodId.method.invoke(response) as String?,
            contentAsString = responseGetContentAsStringMethodId.method.invoke(response) as String,
            viewName = modelAndView?.let { modelAndViewGetViewNameMethodId.method.invoke(modelAndView) } as String?,
            model = modelAndView?.let { modelAndViewGetModelMethodId.method.invoke(modelAndView) }?.let {
                internalConstructor.construct((it as Map<*, *>).toMap(), mapClassId)
            }
        ).also(saveToCache)
    }
}