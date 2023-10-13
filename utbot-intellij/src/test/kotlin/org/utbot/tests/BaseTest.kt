package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.provider.Arguments
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import org.utbot.pages.*
import org.utbot.utils.RemoteRobotExtension
import org.utbot.utils.StepsLogger
import java.time.Duration.ofSeconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(RemoteRobotExtension::class)
open class BaseTest {
    fun getIdeaFrameForBuildSystem(remoteRobot: RemoteRobot, ideaBuildSystem: IdeaBuildSystem): IdeaFrame {
        return when (ideaBuildSystem) {
            IdeaBuildSystem.INTELLIJ -> remoteRobot.find(IdeaFrame::class.java, ofSeconds(10))
            IdeaBuildSystem.GRADLE -> remoteRobot.find(IdeaGradleFrame::class.java, ofSeconds(10))
            IdeaBuildSystem.MAVEN -> remoteRobot.find(IdeaMavenFrame::class.java, ofSeconds(10))
        }
    }

    @BeforeEach
    fun `Close each project before test`(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        try {
            idea {
                closeProject()
            }
        } catch (ignore: Throwable) {}
    }

    @AfterEach
    fun `Close each project after test`(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        idea {
            closeProject()
        }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun init(remoteRobot: RemoteRobot) {
            StepsLogger.init()
        }

        internal val supportedProjectsList: List<Arguments> =
            addPairsToList(true)
        private val unsupportedProjectsList: List<Arguments> =
            addPairsToList(false)

        @JvmStatic
        fun supportedProjectsProvider(): List<Arguments> {
            return supportedProjectsList
        }

        @JvmStatic
        fun unsupportedProjectsProvider(): List<Arguments> {
            return unsupportedProjectsList
        }

        @JvmStatic
        fun allProjectsProvider(): List<Arguments> {
            return supportedProjectsList + unsupportedProjectsList
        }

        @JvmStatic
        private fun addPairsToList(supported: Boolean): List<Arguments> {
            val ideaBuildSystems = IdeaBuildSystem.values()
            ideaBuildSystems.shuffle()
            var j = 0

            val listOfArguments: MutableList<Arguments> = mutableListOf()
            JDKVersion.values().toMutableList().filter {
                it.supported == supported
            }.forEach {
                listOfArguments.add(
                    Arguments.of(ideaBuildSystems[j], it) //each (un)supported JDK with a random build system
                )
                j++
                if (j >= ideaBuildSystems.size) {
                    j = 0
                }
            }
            return listOfArguments
        }
    }
}