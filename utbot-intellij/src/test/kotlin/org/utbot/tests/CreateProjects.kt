package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.utbot.data.*
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
    ) {
        val newProjectName = ideaBuildSystem.system + jdkVersion.number
        remoteRobot.welcomeFrame {
            createNewProject(
                projectName = newProjectName,
                buildSystem = ideaBuildSystem,
                jdkVersion = jdkVersion,
                location = CURRENT_RUN_DIRECTORY_FULL_PATH
            )
        }
        val ideaFrame = getIdeaFrameForBuildSystem(remoteRobot, ideaBuildSystem)
        return with(ideaFrame) {
            waitProjectIsCreated()
            waitFor(Duration.ofSeconds(30)) {
                !isDumbMode()
            }
        }
    }
}