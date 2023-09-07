package org.utbot.framework.process

import org.utbot.framework.process.kryo.KryoHelper

/**
 * Implementations of this interface can be passed to engine process for execution and should
 * be used for adding feature-specific (e.g. Spring-specific) tasks without inflating core UtBot codebase.
 *
 * Such tasks are serialised with kryo when passed between processes, meaning that for successful execution same
 * implementation of [EngineProcessTask] should be present on the classpath of both parent process and engine process.
 *
 * @param R result type of the task (should be present on the classpath of both processes).
 */
interface EngineProcessTask<R> {
    fun perform(kryoHelper: KryoHelper): R
}