package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.utbot.data.*
import org.utbot.pages.idea
import org.utbot.pages.welcomeFrame
import java.io.File
import java.time.Duration

@Order(1)
class CreateProjects : BaseTest() {
    @ParameterizedTest(name = "Create {0} project with JDK {1}")
    @Tags(Tag("Setup"), Tag("Java"), Tag("UTBot"))
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
                location = CURRENT_RUN_DIRECTORY_FULL_PATH,
                locationPart = CURRENT_RUN_DIRECTORY_END
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

    @Test
    @DisplayName("Clone Spring project")
    @Tags(Tag("Setup"), Tag("Java"), Tag("Spring"), Tag("UTBot"))
    fun cloneSpringProject(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        welcomeFrame {
            cloneProjectFromVC(
                SPRING_PROJECT_URL,
                CURRENT_RUN_DIRECTORY_FULL_PATH + File.separator + SPRING_PROJECT_NAME,
                springProjectBuildSystem
            )
        }
        with (getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)) {
            waitProjectIsBuilt()
            expandProjectTree() //this particular project has gradle default structure
        }
        idea {
            openProjectStructureDialog()
            projectStructureDialog.setProjectSdk(JDKVersion.JDK_17)
            projectStructureDialog.okButton.click()
        }
    }
}