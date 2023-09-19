package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.provider.Arguments
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import org.utbot.pages.IdeaFrame
import org.utbot.pages.IdeaGradleFrame
import org.utbot.pages.IdeaMavenFrame
import org.utbot.pages.idea
import org.utbot.utils.RemoteRobotExtension
import org.utbot.utils.StepsLogger
import java.time.Duration.ofSeconds

@ExtendWith(RemoteRobotExtension::class)
open class BaseTest {
    fun getIdeaFrameForBuildSystem(remoteRobot: RemoteRobot, ideaBuildSystem: IdeaBuildSystem): IdeaFrame {
        when (ideaBuildSystem) {
            IdeaBuildSystem.INTELLIJ -> return remoteRobot.find(IdeaFrame::class.java, ofSeconds(10))
            IdeaBuildSystem.GRADLE -> return remoteRobot.find(IdeaGradleFrame::class.java, ofSeconds(10))
            IdeaBuildSystem.MAVEN -> return remoteRobot.find(IdeaMavenFrame::class.java, ofSeconds(10))
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
        fun init(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
            StepsLogger.init()
        }

        private val supportedProjectsList: List<Arguments> =
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