package org.utbot.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.utils.waitFor
import org.utbot.data.IdeaBuildSystem
import java.time.Duration.ofSeconds

@DefaultXpath("IdeFrameImpl type", "//div[@class='IdeFrameImpl']")
class IdeaGradleFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : IdeaFrame(remoteRobot, remoteComponent) {

    override val buildSystemToUse = IdeaBuildSystem.GRADLE

    override fun waitProjectIsCreated() {
        super.waitProjectIsOpened()
        waitFor(ofSeconds(20)) {
            buildResult.retrieveData().textDataList.toString().contains("BUILD SUCCESSFUL")
        }
    }

    override fun expandProjectTree(projectName: String) {
        with(projectViewTree) {
            try {
                waitFor(ofSeconds(10)) {
                    hasText("src").and(hasText(".idea"))
                }
            } catch (ignore: Throwable) {} // if tree is collapsed - proceed to expand
            if (hasText("src").not()) {
                findText(projectName).doubleClick()
                waitFor{
                    hasText("src").and(hasText(".idea"))
                }
            }
            if (hasText("main").not()) {
                findText("src").doubleClick()
                waitFor{
                    hasText("src").and(hasText("main"))
                }
            }
            if (hasText("java").not()) {
                findText("main").doubleClick()
                waitFor{
                    hasText("src").and(hasText("main")).and(hasText("java"))
                }
            }
            if (hasText("org.example").not()) {
                findText("java").doubleClick()
            }
        }
    }
}