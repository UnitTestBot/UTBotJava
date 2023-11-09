package org.utbot.summary.fuzzer.names

import org.utbot.framework.plugin.api.DocStatement

/**
 * Information that can be used to generate test meta-information, including name, display name and JavaDoc.
 */
class TestSuggestedInfo(
    val testName: String? = null,
    val displayName: String? = null,
    val javaDoc:  List<DocStatement>? = null
)