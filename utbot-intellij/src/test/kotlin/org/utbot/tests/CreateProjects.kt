package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitFor
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Tags
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.utbot.data.DEFAULT_TEST_GENERATION_TIMEOUT
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import org.utbot.data.NEW_PROJECT_NAME_START
import org.utbot.pages.welcomeFrame
import java.time.Duration

@Order(1)
class CreateProjects : UTBotTest() {
    @ParameterizedTest(name = "Run UTBot action in a new {0} project")
    @Tags(Tag("smoke"), Tag("NewProject"), Tag("Java"), Tag("UTBot"))
    @MethodSource("projectListProvider")
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
            waitFor(Duration.ofSeconds(30)){
                !isDumbMode()
            }
            expandProjectTree(projectName)
            callUnitTestBotActionOn("Main")
            waitForIgnoringError(Duration.ofSeconds(10)) {
                inlineProgressTextPanel.isShowing
            }
            waitFor(Duration.ofSeconds(30)) {
                inlineProgressTextPanel.hasText("Gradle").not().and(hasText("Indexing").not())
            }
            waitForIgnoringError(Duration.ofSeconds(30)) {
                inlineProgressTextPanel.hasText("Generate tests: read classes")
            }
            waitForIgnoringError(Duration.ofSeconds(10)) {
                inlineProgressTextPanel.hasText("Generate test cases for class Main")
            }
            waitForIgnoringError(Duration.ofSeconds(DEFAULT_TEST_GENERATION_TIMEOUT)) { //Can be changed to 60 for a complex class
                infoNotification.isShowing
            }
            assertThat(infoNotification.title.hasText("UnitTestBot: unit tests generated successfully")).isTrue
            assertThat(textEditor().editor.text).contains("class MainTest")
            assertThat(textEditor().editor.text).contains("@Test\n")
        }
    }
}