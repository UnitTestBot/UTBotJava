package org.utbot.intellij.plugin.language.ts

import javax.swing.JToggleButton
import org.utbot.language.ts.service.TsCoverageMode

object TsCoverageModeButtons {

    var mode = TsCoverageMode.FAST
    val baseButton = JToggleButton("Basic")
    val fastButton = JToggleButton("Fast")

    init {
        val baseButtonModel = baseButton.model
        baseButtonModel.addChangeListener {
            if (baseButtonModel.isPressed) {
                mode = TsCoverageMode.BASIC
            }
        }
        val fastButtonModel = fastButton.model
        fastButtonModel.addChangeListener {
            if (baseButtonModel.isPressed) {
                mode = TsCoverageMode.FAST
            }
        }
    }
}
