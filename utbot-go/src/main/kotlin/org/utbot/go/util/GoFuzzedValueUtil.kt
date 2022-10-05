package org.utbot.go.util

import org.utbot.framework.plugin.api.go.GoUtModel
import org.utbot.fuzzer.FuzzedValue

val FuzzedValue.goRequiredImports get() = (this.model as GoUtModel).requiredImports