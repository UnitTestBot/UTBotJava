package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.utbot.data.DEFAULT_PROJECT_DIRECTORY
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import org.utbot.data.NEW_PROJECT_NAME_START
import org.utbot.pages.welcomeFrame
import java.time.Duration

@Order(1)
class CreateProjects : BaseTest() {
    @ParameterizedTest(name = "Create {0} project with JDK {1}")
    @Tags(Tag("smoke"), Tag("NewProject"), Tag("Java"), Tag("UTBot"), Tag(""))
    @MethodSource("allProjectsProvider")
    fun createProjectWithJDK(
        ideaBuildSystem: IdeaBuildSystem, jdkVersion: JDKVersion,
        remoteRobot: RemoteRobot
    ): Unit = with(remoteRobot) {
        val newProjectName = NEW_PROJECT_NAME_START + ideaBuildSystem.system + jdkVersion.number
        remoteRobot.welcomeFrame {
            createNewProject(
                projectName = newProjectName,
                buildSystem = ideaBuildSystem,
                jdkVersion = jdkVersion,
                location = DEFAULT_PROJECT_DIRECTORY
            )
        }
        val ideaFrame = getIdeaFrameForBuildSystem(remoteRobot, ideaBuildSystem)
        with(ideaFrame) {
            waitProjectIsCreated()
            waitFor(Duration.ofSeconds(30)) {
                !isDumbMode()
            }
        }
    }
}