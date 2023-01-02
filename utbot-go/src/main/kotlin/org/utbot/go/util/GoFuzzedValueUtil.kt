package org.utbot.go.util

import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.fuzzer.FuzzedValue

val FuzzedValue.goRequiredImports
    get() = (this.model as GoUtModel).requiredImports

val List<FuzzedValue>.goRequiredImports
    get() = this.map { it.goRequiredImports }.reduceOrNull { acc, imports -> acc + imports } ?: setOf()