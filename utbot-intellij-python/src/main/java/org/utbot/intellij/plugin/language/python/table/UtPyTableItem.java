package org.utbot.intellij.plugin.language.python.table;

import com.jetbrains.python.psi.PyElement;

import javax.swing.*;

interface UtPyTableItem {

    public PyElement getContent();

    public Icon getIcon();

    boolean isChecked();

    void setChecked(boolean valueToBeSet);
}
