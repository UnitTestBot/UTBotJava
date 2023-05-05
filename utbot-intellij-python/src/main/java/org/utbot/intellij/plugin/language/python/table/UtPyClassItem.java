package org.utbot.intellij.plugin.language.python.table;

import com.intellij.icons.AllIcons;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyElement;

import javax.swing.*;

public class UtPyClassItem implements UtPyTableItem {
    private final PyClass pyClass;
    private boolean isChecked;

    public UtPyClassItem(PyClass clazz) {
        pyClass = clazz;
        isChecked = false;
    }

    @Override
    public PyElement getContent() {
        return pyClass;
    }

    @Override
    public String getIdName() {
        return pyClass.getQualifiedName();
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Nodes.Class;
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void setChecked(boolean valueToBeSet) {
        isChecked = valueToBeSet;
    }
}
