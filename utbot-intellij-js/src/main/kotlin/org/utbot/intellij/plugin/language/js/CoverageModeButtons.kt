package org.utbot.intellij.plugin.language.js

import service.CoverageMode
import javax.swing.ButtonGroup
import javax.swing.JToggleButton

object CoverageModeButtons {

    var mode = CoverageMode.FAST

    val fastButton = JToggleButton("Fast").apply { this.isSelected = true }
    val baseButton = JToggleButton("Basic")
    val buttonGroup = ButtonGroup().apply {
        this.add(fastButton)
        this.add(baseButton)
        this.setSelected(fastButton.model, true)
    }

    init {
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
    }
}
