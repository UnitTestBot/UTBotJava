package org.utbot.go.util

import org.utbot.fuzzer.FuzzedValue
import org.utbot.go.framework.api.go.GoUtModel

val FuzzedValue.goRequiredImports
    get() = (this.model as GoUtModel).getRequiredImports()
