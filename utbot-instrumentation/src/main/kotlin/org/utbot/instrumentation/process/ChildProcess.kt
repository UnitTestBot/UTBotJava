package org.utbot.instrumentation.process

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import org.utbot.common.scanForClasses
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.instrumentation.agent.Agent
import org.utbot.instrumentation.classloaders.HandlerClassLoader
import org.utbot.instrumentation.classloaders.InstrumentationClassLoader
import org.utbot.instrumentation.classloaders.UserRuntimeClassLoader
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.util.KryoHelper
import org.utbot.instrumentation.util.Protocol
import org.utbot.instrumentation.util.UnexpectedCommand

// Logging
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
private fun log(any: Any?) {
    System.err.println(LocalDateTime.now().format(dateFormatter) + " | $any")
}

private val kryoHelper: KryoHelper = KryoHelper(System.`in`, System.`out`)

/**
 * It should be compiled into separate jar file (child_process.jar) and be run with an agent (agent.jar) option.
 */
fun main() {
    // We don't want user code to litter the standard output, so we redirect it.
    val tmpStream = PrintStream(object : OutputStream() {
        override fun write(b: Int) {}
    })
    System.setOut(tmpStream)

    val classPaths = readClasspath()
    val pathsToUserClasses = classPaths.pathsToUserClasses.split(File.pathSeparatorChar).toSet()
    val pathsToDependencyClasses = classPaths.pathsToDependencyClasses.split(File.pathSeparatorChar).toSet()

    val (userRuntimeClassLoader, instrumentationClassLoader) = getClassLoaders(
        pathsToUserClasses + pathsToDependencyClasses
    )

    kryoHelper.setKryoClassLoader(instrumentationClassLoader) // Now kryo will use our classloader when it encounters unregistered class.

    log("User classes:" + pathsToUserClasses.joinToString())

    kryoHelper.use {
        UtContext.setUtContext(UtContext(userRuntimeClassLoader)).use {
            getInstrumentation()?.let { instrumentation ->
                Agent.dynamicClassTransformer.transformer = instrumentation // classTransformer is set
                Agent.dynamicClassTransformer.addUserPaths(pathsToUserClasses)
                instrumentation.init(pathsToUserClasses)

                try {
                    loop(instrumentation, userRuntimeClassLoader)
                } catch (e: Throwable) {
                    log("Terminating process because exception occured: ${e.stackTraceToString()}")
                    exitProcess(1)
                }
            }
        }
    }
}

private fun send(cmdId: Long, cmd: Protocol.Command) {
    try {
        kryoHelper.writeCommand(cmdId, cmd)
        log("Send << $cmdId")
    } catch (e: Exception) {
        log("Failed to serialize << $cmdId with exception: ${e.stackTraceToString()}")
        log("Writing it to kryo...")
        kryoHelper.writeCommand(cmdId, Protocol.ExceptionInChildProcess(e))
        log("Successfuly wrote.")
    }
}

private fun read(cmdId: Long): Protocol.Command {
    try {
        val cmd = kryoHelper.readCommand()
        log("Received :> $cmdId")
        return cmd
    } catch (e: Exception) {
        log("Failed to read :> $cmdId with exception: ${e.stackTraceToString()}")
        throw e
    }
}

/**
 * Main loop. Processes incoming commands.
 */
private fun loop(instrumentation: Instrumentation<*>, userRuntimeClassLoader: UserRuntimeClassLoader) {
    while (true) {
        val cmdId = kryoHelper.readLong()
        val cmd = try {
            read(cmdId)
        } catch (e: Exception) {
            send(cmdId, Protocol.ExceptionInChildProcess(e))
            continue
        }

        when (cmd) {
            is Protocol.WarmupCommand -> {
                val time = measureTimeMillis {
                    userRuntimeClassLoader.scanForClasses("").toList() // here we transform classes
                }
                System.err.println("warmup finished in $time ms")
            }
            is Protocol.InvokeMethodCommand -> {
                val resultCmd = try {
                    val clazz = userRuntimeClassLoader.loadClass(cmd.className)
                    val res = instrumentation.invoke(
                        clazz,
                        cmd.signature,
                        cmd.arguments,
                        cmd.parameters
                    )
                    Protocol.InvocationResultCommand(res)
                } catch (e: Throwable) {
                    System.err.println(e.stackTraceToString())
                    Protocol.ExceptionInChildProcess(e)
                }
                send(cmdId, resultCmd)
            }
            is Protocol.StopProcessCommand -> {
                break
            }
            is Protocol.InstrumentationCommand -> {
                val result = instrumentation.handle(cmd)
                result?.let {
                    send(cmdId, it)
                }
            }
            else -> {
                send(cmdId, Protocol.ExceptionInChildProcess(UnexpectedCommand(cmd)))
            }
        }
    }
}

/**
 * Retrieves the actual instrumentation. It is passed from the main process during
 * [org.utbot.instrumentation.ConcreteExecutor] instantiation.
 */
private fun getInstrumentation(): Instrumentation<*>? {
    val cmdId = kryoHelper.readLong()
    return when (val cmd = kryoHelper.readCommand()) {
        is Protocol.SetInstrumentationCommand<*> -> {
            cmd.instrumentation
        }
        is Protocol.StopProcessCommand -> null
        else -> {
            send(cmdId, Protocol.ExceptionInChildProcess(UnexpectedCommand(cmd)))
            null
        }
    }
}

private fun readClasspath(): Protocol.AddPathsCommand {
    val cmdId = kryoHelper.readLong()
    return kryoHelper.readCommand().let { cmd ->
        if (cmd is Protocol.AddPathsCommand) {
            cmd
        } else {
            send(cmdId, Protocol.ExceptionInChildProcess(UnexpectedCommand(cmd)))
            error("No classpath!")
        }
    }
}

private fun getClassLoaders(userPaths: Iterable<String>): Pair<UserRuntimeClassLoader, ClassLoader> {
    val cmdId = kryoHelper.readLong()
    return kryoHelper.readCommand().let { cmd ->
        if (cmd is Protocol.UseSeparateClassLoadersCommand) {
            if (cmd.useSeparate) {
                UserRuntimeClassLoader(userPaths) to InstrumentationClassLoader(userPaths)
            } else {
                val handlerClassLoader = HandlerClassLoader(userPaths)
                handlerClassLoader to handlerClassLoader
            }
        } else {
            send(cmdId, Protocol.ExceptionInChildProcess(UnexpectedCommand(cmd)))
            error("ClassLoaders can't initialized!")
        }
    }
}
