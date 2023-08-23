package org.utbot.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.utbot.data.IdeaBuildSystem
import java.time.Duration.ofSeconds

@DefaultXpath("IdeFrameImpl type", "//div[@class='IdeFrameImpl']")
class IdeaGradleFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : IdeaFrame(remoteRobot, remoteComponent) {

    override val buildSystemToUse = IdeaBuildSystem.GRADLE

    override fun waitProjectIsCreated() {
        super.waitProjectIsOpened()
        repeat (120) {
            inlineProgressTextPanel.isShowing.not()
        }
    }

    override fun expandProjectTree(projectName: String) {
        with(projectViewTree) {
            waitForIgnoringError(ofSeconds(10)) {
                hasText("src")
            }
            if (hasText("src").not()) {
                findText(projectName).doubleClick()
                waitForIgnoringError{
                    hasText("src")
                }
            }
            if (hasText("main").not()) {
                findText("src").doubleClick()
                waitForIgnoringError{
                    hasText("src").and(hasText("main"))
                }
            }
            if (hasText("java").not()) {
                findText("main").doubleClick()
                waitForIgnoringError{
                    hasText("src").and(hasText("main")).and(hasText("java"))
                }
            }
            if (hasText("Main").not()) {
                findText("java").doubleClick()
            }
        }
    }
}