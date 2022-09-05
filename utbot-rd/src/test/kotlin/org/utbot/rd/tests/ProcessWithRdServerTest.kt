package org.utbot.rd.tests

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.utbot.rd.*
import java.io.File

class ProcessWithRdServerTest {
    lateinit var def: LifetimeDefinition
    private val parent: Lifetime
        get() = def.lifetime

    @BeforeEach
    fun initLifetimes() {
        def = LifetimeDefinition()
    }

    @AfterEach
    fun terminateLifetimes() {
        def.terminate()
    }

    private fun processMockCmd(delayInSeconds: Long, port: Int, shouldStartProtocol: Boolean): List<String> {
        val jar = System.getProperty("PROCESS_WITH_RD_SERVER_MOCK")
        val javaHome = System.getProperty("java.home")
        val java = File(javaHome, "bin").resolve("java")

        return listOf(java.canonicalPath, "-ea", "-jar", jar, delayInSeconds.toString(), port.toString(), shouldStartProtocol.toString())
    }

    private fun assertProcessAlive(proc: LifetimedProcess) = runBlocking {
        delay(1000) // if proc not started in 1 seconds - something is bad
        Assertions.assertTrue(proc.lifetime.isAlive)
        Assertions.assertTrue(proc.process.isAlive)
    }

    private fun assertProcessDead(proc: LifetimedProcess) = runBlocking {
        delay(1000) // if proc is not dead in 1 second - something is bad
        Assertions.assertFalse(proc.lifetime.isAlive)
        Assertions.assertFalse(proc.process.isAlive)
    }

    @Test
    fun testParentOnly() = runBlocking {
        var lifetimedProcess: LifetimedProcess? = null
        val exception = assertThrows<CancellationException> {
            withTimeout(5000) {
                startUtProcessWithRdServer(parent) {
                    val cmds = processMockCmd(3, it, false)
                    val proc = ProcessBuilder(cmds).start()
                    val lfProc = proc.toLifetimedProcess(parent)

                    assertProcessAlive(lfProc)
                    lifetimedProcess = lfProc
                    proc
                }
            }
        }

        Assertions.assertFalse(exception is TimeoutCancellationException)
        Assertions.assertTrue(parent.isAlive)
        assertProcessDead(lifetimedProcess!!)
    }

    @Test
    fun testParentWithChild() = runBlocking {
        val child = startUtProcessWithRdServer(parent) {
                val cmds = processMockCmd(3, it, true)

                ProcessBuilder(cmds).start()
            }

        assertProcessAlive(child)
        delay(3000)
        assertProcessDead(child)

        Assertions.assertTrue(parent.isAlive)
        Assertions.assertFalse(child.protocol.lifetime.isAlive)
    }

    @Test
    fun testCancellation() = runBlocking {
        var lifetimedProcess: LifetimedProcess? = null
        val exception = assertThrows<CancellationException> {
            withTimeout(1000) {
                startUtProcessWithRdServer(parent) {
                    val cmds = processMockCmd(3, it, false)
                    val proc = ProcessBuilder(cmds).start()
                    val lfProc = proc.toLifetimedProcess(parent)

                    assertProcessAlive(lfProc)
                    lifetimedProcess = lfProc
                    proc
                }
            }
        }

        Assertions.assertTrue(exception is TimeoutCancellationException)
        Assertions.assertTrue(parent.isAlive)
        assertProcessDead(lifetimedProcess!!)
    }
}