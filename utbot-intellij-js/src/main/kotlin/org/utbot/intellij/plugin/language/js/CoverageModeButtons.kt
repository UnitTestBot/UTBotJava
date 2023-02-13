package org.utbot.intellij.plugin.language.js

import service.coverage.CoverageMode
import javax.swing.JToggleButton

object CoverageModeButtons {

    var mode = CoverageMode.FAST
    val baseButton = JToggleButton("Basic")
    val fastButton = JToggleButton("Fast")

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
