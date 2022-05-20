package org.utbot.fuzzer

import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtValueTestCase

interface ObsoleteTestCaseGenerator {
    fun generate(method: UtMethod<*>, mockStrategy: MockStrategyApi): UtValueTestCase<*>
}