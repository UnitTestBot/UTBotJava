package org.utbot.intellij.plugin.python.table;

import com.intellij.icons.AllIcons;
import com.jetbrains.python.psi.PyElement;
import com.jetbrains.python.psi.PyFunction;

import javax.swing.*;

public class UtPyFunctionItem implements UtPyTableItem {
    private final PyFunction pyFunction;
    private boolean isChecked;

    public UtPyFunctionItem(PyFunction function) {
        pyFunction = function;
        isChecked = false;
    }

    @Override
    public PyElement getContent() {
        return pyFunction;
    }

    @Override
    public String getIdName() {
        return pyFunction.getQualifiedName();
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Nodes.Function;
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
