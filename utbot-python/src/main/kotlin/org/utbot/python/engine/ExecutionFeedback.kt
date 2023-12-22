package org.utbot.python.engine

import org.utbot.framework.plugin.api.UtError
import org.utbot.python.framework.api.python.PythonUtExecution

sealed interface ExecutionFeedback
class ValidExecution(val utFuzzedExecution: PythonUtExecution): ExecutionFeedback
class InvalidExecution(val utError: UtError): ExecutionFeedback
class TypeErrorFeedback(val message: String) : ExecutionFeedback
class ArgumentsTypeErrorFeedback(val message: String) : ExecutionFeedback
class CachedExecutionFeedback(val cachedFeedback: ExecutionFeedback) : ExecutionFeedback
object FakeNodeFeedback : ExecutionFeedback