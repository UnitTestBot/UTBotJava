package org.utbot.intellij.plugin.language.python.table;

import com.jetbrains.python.psi.PyElement;

import javax.swing.*;

public interface UtPyTableItem {

    public PyElement getContent();

    public String getIdName();

    public Icon getIcon();

    boolean isChecked();

    void setChecked(boolean valueToBeSet);
}
