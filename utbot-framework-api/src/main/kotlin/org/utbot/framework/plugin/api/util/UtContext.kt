package org.utbot.framework.plugin.api.util

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.runBlocking
import org.utbot.common.StopWatch
import org.utbot.common.currentThreadInfo
import org.utbot.framework.plugin.api.util.UtContext.Companion.setUtContext
import org.utbot.framework.plugin.jcdb.DelegatingClasspathSet
import org.utbot.jcdb.api.ClasspathSet
import org.utbot.jcdb.api.JCDB
import org.utbot.jcdb.jcdb
import java.io.Closeable
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.coroutines.CoroutineContext

val utContext: UtContext
    get() = UtContext.currentContext()
        ?: error("No context is set. Please use `withUtContext() {...}` or `setUtContext().use {...}`. Thread: ${currentThreadInfo()}")

interface JCDBProvider {
    val jcdb: JCDB
}

open class PrototypeJCDB : JCDBProvider {
    override val jcdb = runBlocking {
        jcdb {
            useProcessJavaRuntime()
        }
    }
}

object SingletonJCDB : JCDBProvider {

    override val jcdb by lazy {
        runBlocking {
            jcdb {
                useProcessJavaRuntime()
            }
        }
    }
}

class UtContext(val classLoader: ClassLoader, val provider: JCDBProvider, val classpath: ClasspathSet) :
    ThreadContextElement<UtContext?>, Closeable {

    // This StopWatch is used to respect bytecode transforming time while invoking with timeout
    var stopWatch: StopWatch? = null
        private set

    constructor(classLoader: ClassLoader) : this(classLoader, SingletonJCDB, classLoader.asClasspath(SingletonJCDB))

    constructor(classLoader: ClassLoader, buildDir: Path) : this(classLoader, SingletonJCDB, buildDir.asClasspath(SingletonJCDB))

    constructor(classLoader: ClassLoader, stopWatch: StopWatch) : this(classLoader) {
        this.stopWatch = stopWatch
    }

    override fun toString() = "UtContext(classLoader=$classLoader, hashCode=${hashCode()})"

    private class Cookie(context: UtContext) : AutoCloseable {
        private val contextToRestoreOnClose: UtContext? = threadLocalContextHolder.get()
        private val currentContext: UtContext = context

        init {
            threadLocalContextHolder.set(currentContext)
        }

        override fun close() {
            val context = threadLocalContextHolder.get()

            require(context === currentContext) {
                "Trying to close UtContext.Cookie but it seems that last set context $context is not equal context set on cookie creation $currentContext"
            }

            restore(contextToRestoreOnClose)
        }
    }


    companion object {
        private val Key = object : CoroutineContext.Key<UtContext> {}
        private val threadLocalContextHolder = ThreadLocal<UtContext>()

        private val ClassLoader.urls: List<URL>?
            get() {
                if (this is URLClassLoader) {
                    return urLs.toList()
                }
                // jdk9+ need to use reflection
                val clazz = javaClass

                try {
                    val field = clazz.getDeclaredField("ucp")
                    field.isAccessible = true
                    val classpath = field.get(this)
                    val value = classpath.javaClass.getDeclaredMethod("getURLs").invoke(classpath) as Array<URL>
                    return value.toList()
                } catch (e: Exception) {
                    return null
                }
            }

        fun currentContext(): UtContext? = threadLocalContextHolder.get()
        fun setUtContext(context: UtContext): AutoCloseable = Cookie(context)

        private fun restore(contextToRestore: UtContext?) {
            if (contextToRestore != null) {
                threadLocalContextHolder.set(contextToRestore)
            } else {
                threadLocalContextHolder.remove()
            }
        }

        private fun Path.asClasspath(provider: JCDBProvider): ClasspathSet = runBlocking {
            val file = toFile()
            DelegatingClasspathSet(provider.jcdb.classpathSet(listOf(file)))
        }

        private var previousClassLoader: ClassLoader? = null
        private var previousClasspathSet: ClasspathSet? = null

        private fun ClassLoader.asClasspath(provider: JCDBProvider): ClasspathSet =
            previousClasspathSet.takeIf { previousClassLoader == this }
                ?: runBlocking {
                    val files = urls?.map { Paths.get(it.toURI()).toFile() }
                        ?: throw IllegalStateException("Can't grab classpath from ${this@asClasspath}")
                    DelegatingClasspathSet(provider.jcdb.classpathSet(files))
                }.also { classpathSet ->
                    previousClassLoader = this
                    previousClasspathSet = classpathSet
                }

    }

    override val key: CoroutineContext.Key<UtContext> get() = Key

    override fun restoreThreadContext(context: CoroutineContext, oldState: UtContext?) = restore(oldState)

    override fun updateThreadContext(context: CoroutineContext): UtContext? {
        val prevUtContext = threadLocalContextHolder.get()
        threadLocalContextHolder.set(this)
        return prevUtContext
    }

    override fun close() {
        classpath.close()
    }

}

inline fun <T> withUtContext(context: UtContext, block: () -> T): T = setUtContext(context).use { block() }
