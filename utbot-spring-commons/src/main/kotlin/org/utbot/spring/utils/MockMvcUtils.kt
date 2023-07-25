package org.utbot.spring.utils

import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.utbot.spring.api.relevantMockMvcResponseDataGetterNames

fun getMockMvcResponseData(
    // we ask for it, to force its initialization during value construction phase
    @Suppress("UNUSED_PARAMETER") controllerInstance: Any,
    mockMvc: MockMvc,
    requestBuilder: RequestBuilder
): Map<String, Any?> {
    val response = mockMvc.perform(requestBuilder).andReturn().response
    return relevantMockMvcResponseDataGetterNames.associateWith { getterName ->
        response::class.java.getMethod(getterName).invoke(response)
    }
}