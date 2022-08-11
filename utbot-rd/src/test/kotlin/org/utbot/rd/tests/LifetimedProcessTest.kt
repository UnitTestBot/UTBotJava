package org.utbot.rd.tests

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.utbot.rd.LifetimedProcess
import org.utbot.rd.startLifetimedProcess
import java.io.File

class LifetimedProcessTest {
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

    private fun processMockCmd(delayInSeconds: Long): List<String> {
        val jar = System.getProperty("RD_MOCK_PROCESS")
        val javaHome = System.getProperty("java.home")
        val java = File(javaHome, "bin").resolve("java")

        return listOf(java.canonicalPath, "-ea", "-jar", jar, delayInSeconds.toString())
    }

    private fun List<String>.startLifetimedProcessWithAssertion(block: (LifetimedProcess) -> Unit) {
        val proc = startLifetimedProcess(this, parent)

        assertProcessAlive(proc)
        block(proc)
        assertProcessDead(proc)
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
    fun testProcessLifetimeTermination() {
        val cmds = processMockCmd(10)

        cmds.startLifetimedProcessWithAssertion {
            it.terminate()
        }
        Assertions.assertTrue(parent.isAlive)
    }

    @Test
    fun testParentLifetimeTermination() {
        val cmds = processMockCmd(10)

        cmds.startLifetimedProcessWithAssertion {
            terminateLifetimes()
        }
        Assertions.assertFalse(parent.isAlive)
    }

    @Test
    fun testProcessDeath() {
        val cmds = processMockCmd(3)

        cmds.startLifetimedProcessWithAssertion {
            runBlocking {
                delay(5000)
            }
        }
        Assertions.assertTrue(parent.isAlive)
    }

    @Test
    fun testProcessKill() {
        val cmds = processMockCmd(10)

        cmds.startLifetimedProcessWithAssertion {
            it.process.destroyForcibly()
        }
        Assertions.assertTrue(parent.isAlive)
    }
}