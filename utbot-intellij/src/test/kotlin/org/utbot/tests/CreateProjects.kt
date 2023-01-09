package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitFor
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import org.utbot.data.NEW_PROJECT_NAME_START
import org.utbot.pages.welcomeFrame
import java.time.Duration

@Order(1)
class CreateProjects : UTBotTest() {
    @ParameterizedTest(name = "Run UTBot action in a new {0} project")
    @CsvSource(
        "INTELLIJ, JDK_11",
        "GRADLE, JDK_11",
        "INTELLIJ, JDK_1_8",
        "GRADLE, JDK_1_8")
    @Tags(Tag("smoke"), Tag("NewProject"), Tag("Java"), Tag("UTBot"))
    fun runActionInNewProject(ideaBuildSystem: IdeaBuildSystem, jdkVersion: JDKVersion,
                              remoteRobot: RemoteRobot) : Unit = with(remoteRobot){
        val newProjectName = NEW_PROJECT_NAME_START + ideaBuildSystem.system + jdkVersion.number
        remoteRobot.welcomeFrame {
            createNewProject(
                projectName = newProjectName,
                buildSystem = ideaBuildSystem,
                jdkVersion = jdkVersion )
        }
        val ideaFrame = getIdeaFrameForSpecificBuildSystem(remoteRobot, ideaBuildSystem)
        with (ideaFrame) {
            waitProjectIsCreated()
            waitFor(Duration.ofSeconds(200)){
                !isDumbMode()
            }
            expandProjectTree(projectName)
            callUnitTestBotActionOn("Main")
            waitFor(Duration.ofSeconds(100)) {
                inlineProgressTextPanel.isShowing
            }
            waitFor(Duration.ofSeconds(100)) {
                inlineProgressTextPanel.hasText("Generate tests: read classes")
            }
            waitFor(Duration.ofSeconds(100)) {
                inlineProgressTextPanel.hasText("Generate test cases for class Main")
            }
            Assertions.assertThat(infoNotification.title.hasText("UTBot: unit tests generated successfully")).isTrue
            Assertions.assertThat(textEditor().editor.text).contains("class MainTest")
            Assertions.assertThat(textEditor().editor.text).contains("@Test\n")
        }
    }
}