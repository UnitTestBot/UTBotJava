package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.data.IdeaBuildSystem
import org.utbot.pages.IdeaFrame
import org.utbot.pages.idea
import org.utbot.pages.IdeaGradleFrame
import org.utbot.pages.IdeaIntelliJFrame
import org.utbot.utils.RemoteRobotExtension
import org.utbot.utils.StepsLogger
import java.time.Duration.ofSeconds

@ExtendWith(RemoteRobotExtension::class)
open class UTBotTest {
    fun getIdeaFrameForSpecificBuildSystem(remoteRobot: RemoteRobot, ideaBuildSystem: IdeaBuildSystem): IdeaFrame {
        when (ideaBuildSystem) {
            IdeaBuildSystem.INTELLIJ -> return remoteRobot.find(IdeaIntelliJFrame::class.java, ofSeconds(10))
            IdeaBuildSystem.GRADLE -> return remoteRobot.find(IdeaGradleFrame::class.java, ofSeconds(10))
        }
        throw IllegalArgumentException("ideaBuildSystem not recognized: $ideaBuildSystem")
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
    }
}