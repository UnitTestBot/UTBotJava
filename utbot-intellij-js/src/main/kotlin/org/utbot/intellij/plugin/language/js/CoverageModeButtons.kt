package org.utbot.intellij.plugin.language.js

import api.NodeCoverageMode
import javax.swing.JToggleButton

object CoverageModeButtons {

    var mode = NodeCoverageMode.FAST
    val baseButton = JToggleButton("Basic")
    val fastButton = JToggleButton("Fast")

    init {
        val baseButtonModel = baseButton.model
        baseButtonModel.addChangeListener {
            if (baseButtonModel.isPressed) {
                mode = NodeCoverageMode.BASIC
            }
        }
        val fastButtonModel = fastButton.model
        fastButtonModel.addChangeListener {
            if (baseButtonModel.isPressed) {
                mode = NodeCoverageMode.FAST
            }
        }
    }
}
