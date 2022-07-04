package org.utbot.framework.util

import org.utbot.engine.MockStrategy
import org.utbot.framework.plugin.api.MockStrategyApi

fun MockStrategyApi.toModel(): MockStrategy =
    when (this) {
        MockStrategyApi.NO_MOCKS -> MockStrategy.NO_MOCKS
        MockStrategyApi.OTHER_PACKAGES -> MockStrategy.OTHER_PACKAGES
        MockStrategyApi.OTHER_CLASSES -> MockStrategy.OTHER_CLASSES
        else -> error("Cannot map API Mock Strategy model to Engine model: $this")
    }