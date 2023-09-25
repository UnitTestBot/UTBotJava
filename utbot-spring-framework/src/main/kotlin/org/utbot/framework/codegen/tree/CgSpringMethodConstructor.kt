package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.util.SpringModelUtils.mockMvcPerformMethodId
import org.utbot.framework.plugin.api.util.SpringModelUtils.nestedServletExceptionClassIds
import org.utbot.framework.plugin.api.util.id

class CgSpringMethodConstructor(context: CgContext) : CgMethodConstructor(context) {
    override fun shouldTestPassWithException(execution: UtExecution, exception: Throwable): Boolean =
        !isNestedServletException(exception) && super.shouldTestPassWithException(execution, exception)

    override fun collectNeededStackTraceLines(
        exception: Throwable,
        executableToStartCollectingFrom: ExecutableId
    ): List<String> =
        // `mockMvc.perform` wraps exceptions from user code into NestedServletException, so we unwrap them back
        exception.takeIf {
            executableToStartCollectingFrom == mockMvcPerformMethodId && isNestedServletException(it)
        }?.cause?.let { cause ->
            super.collectNeededStackTraceLines(cause, currentExecutableUnderTest!!)
        } ?: super.collectNeededStackTraceLines(exception, executableToStartCollectingFrom)

    private fun isNestedServletException(exception: Throwable): Boolean =
        exception::class.java.id in nestedServletExceptionClassIds
}
