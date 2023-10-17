package org.utbot.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.utbot.data.IdeaBuildSystem
import java.time.Duration.ofSeconds

@DefaultXpath("IdeFrameImpl type", "//div[@class='IdeFrameImpl']")
class IdeaMavenFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : IdeaFrame(remoteRobot, remoteComponent) {

    override val buildSystemToUse = IdeaBuildSystem.MAVEN

    override fun waitProjectIsBuilt() {
        super.waitProjectIsBuilt()
        waitForIgnoringError (ofSeconds(60)) {
            projectViewTree.hasText("Main.java").not()
            projectViewTree.hasText("Main")
        }
    }
}