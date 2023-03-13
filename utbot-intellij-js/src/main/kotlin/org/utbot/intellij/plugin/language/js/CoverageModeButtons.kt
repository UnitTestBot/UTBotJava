package org.utbot.intellij.plugin.language.js

import javax.swing.ButtonGroup
import javax.swing.JRadioButton
import service.coverage.CoverageMode

object CoverageModeButtons {

    var mode = CoverageMode.FAST

    val fastButton = JRadioButton("Fast")
    val baseButton = JRadioButton("Basic")


    init {
        val buttonGroup = ButtonGroup()
        fastButton.isSelected = true
        val baseButtonModel = baseButton.model
        baseButtonModel.addChangeListener {
            if (baseButtonModel.isPressed) {
                mode = CoverageMode.BASIC
            }
        }
        val fastButtonModel = fastButton.model
        fastButtonModel.addChangeListener {
            if (baseButtonModel.isPressed) {
                mode = CoverageMode.FAST
            }
        }
        buttonGroup.add(fastButton)
        buttonGroup.add(baseButton)
    }
}
