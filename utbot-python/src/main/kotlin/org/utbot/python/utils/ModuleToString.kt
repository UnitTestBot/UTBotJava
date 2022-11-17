package org.utbot.python.utils

import io.github.danielnaczo.python3parser.model.mods.Module
import io.github.danielnaczo.python3parser.visitors.prettyprint.IndentationPrettyPrint
import io.github.danielnaczo.python3parser.visitors.prettyprint.ModulePrettyPrintVisitor

fun moduleToString(module: Module): String {
    val modulePrettyPrintVisitor = ModulePrettyPrintVisitor()
    return modulePrettyPrintVisitor.visitModule(module, IndentationPrettyPrint(0))
}