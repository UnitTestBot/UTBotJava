package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import org.utbot.data.NEW_PROJECT_NAME_START
import org.utbot.data.TEST_RUN_NUMBER
import org.utbot.pages.*
import org.utbot.steps.autocomplete
import java.time.Duration

@Order(2)
class UnitTestBotActionTest : UTBotTest() {

    @ParameterizedTest(name = "Run UTBot action on a new Java class in existing {0} project")
    @CsvSource(
        "INTELLIJ, JDK_11",
        "GRADLE, JDK_11",
        "INTELLIJ, JDK_1_8",
        "GRADLE, JDK_1_8" )
    @Tags(Tag("Java"), Tag("UTBot"), Tag("NewClass"))
    fun runActionOnNewJavaClassInExistingProject(ideaBuildSystem: IdeaBuildSystem, jdkVersion: JDKVersion,
                                                 remoteRobot: RemoteRobot) : Unit = with(remoteRobot) {
        val createdProjectName = NEW_PROJECT_NAME_START + ideaBuildSystem.system + jdkVersion.number
        remoteRobot.welcomeFrame {
            findText(createdProjectName).click()
        }
        val ideaFrame = getIdeaFrameForSpecificBuildSystem(remoteRobot, ideaBuildSystem)
        with (ideaFrame) {
            waitProjectIsOpened()
            expandProjectTree(projectName)
            val newClassName = "Example"
            createNewJavaClass(newClassName)
            with(textEditor()) {
                editor.selectText(newClassName)
                keyboard {
                    key(java.awt.event.KeyEvent.VK_END)
                    enter()
                }
                autocomplete("main")
                autocomplete("sout")
                keyboard {
                    enterText("\"")
                    enterText("Hello from UTBot UI ${TEST_RUN_NUMBER} test in ${ideaBuildSystem.system} project with ${jdkVersion.number} JDK")
                }
            }
            callUnitTestBotActionOn(newClassName)
            waitFor (Duration.ofSeconds(10)){
                inlineProgressTextPanel.isShowing
            }
            waitFor (Duration.ofSeconds(10)){
                inlineProgressTextPanel.hasText("Generate tests: read classes")
            }
            waitFor (Duration.ofSeconds(10)){
                inlineProgressTextPanel.hasText("Generate test cases for class $newClassName")
            }
            assertThat(infoNotification.title.hasText("UTBot: unit tests generated successfully")).isTrue
            assertThat(textEditor().editor.text).contains("class ${newClassName}Test")
            assertThat(textEditor().editor.text).contains("@Test\n")
        }
    }
}