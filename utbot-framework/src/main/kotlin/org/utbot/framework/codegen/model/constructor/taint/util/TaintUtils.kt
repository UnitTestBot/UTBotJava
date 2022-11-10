package org.utbot.framework.codegen.model.constructor.taint.util

import org.utbot.engine.taint.TaintAnalysisError
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.id

/**
 * An error that should be used in generated tests for taints instead of original [TaintAnalysisError].
 */
val taintErrorClassId: ClassId = VerifyError::class.id

fun constructAssertionMessage(taintAnalysisError: TaintAnalysisError): String =
    with(taintAnalysisError) {
        buildString {
            append("$message, taint sink: \"$taintSink\"")
            sinkSourcePosition?.let {
                append(", taint sink source line: $it")
            }
        }
    }
