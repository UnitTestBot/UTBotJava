package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.provider.Arguments
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import org.utbot.data.random
import org.utbot.pages.IdeaFrame
import org.utbot.pages.IdeaGradleFrame
import org.utbot.pages.idea
import org.utbot.utils.RemoteRobotExtension
import org.utbot.utils.StepsLogger
import java.time.Duration.ofSeconds

@ExtendWith(RemoteRobotExtension::class)
open class UTBotTest {
    fun getIdeaFrameForSpecificBuildSystem(remoteRobot: RemoteRobot, ideaBuildSystem: IdeaBuildSystem): IdeaFrame {
        when (ideaBuildSystem) {
            IdeaBuildSystem.INTELLIJ -> return remoteRobot.find(IdeaFrame::class.java, ofSeconds(10))
            IdeaBuildSystem.GRADLE -> return remoteRobot.find(IdeaGradleFrame::class.java, ofSeconds(10))
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

        private val createdProjectsList: MutableList<Arguments> = mutableListOf()
        @JvmStatic
        fun projectListProvider(): List<Arguments> {
            if (createdProjectsList.isEmpty()) {
                combineEnums()
            }
            return createdProjectsList
        }

        @JvmStatic
        private fun combineEnums() {
            val ideaBuildSystems = IdeaBuildSystem.values()
            val jdkVersions = JDKVersion.values()

            var j = 0
            ideaBuildSystems.forEach { system ->
                j = random.nextInt(jdkVersions.size)
                createdProjectsList.add(
                    Arguments.of(system, jdkVersions[j])
                )
            }
        }
    }
}