package org.utbot.spring.api

/**
 * Zero argument methods of `MockHttpServletResponse` that extract all data out of
 * `MockHttpServletResponse` instance, that is needed for assertion generation
 *
 * @see SpringApi.getMockMvcResponseDataMethod
 */
val relevantMockMvcResponseDataGetterNames = listOf(
    "getStatus", "getContentAsString", "getErrorMessage"
)
