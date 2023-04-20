package org.utbot.intellij.plugin.language.python.table;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.refactoring.ui.EnableDisableAction;
import com.intellij.ui.*;
import com.intellij.ui.icons.RowIcon;
import com.intellij.ui.table.JBTable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UtPyMemberSelectionTable<T extends UtPyTableItem> extends JBTable implements DataProvider {
    protected static final int CHECKED_COLUMN = 0;
    protected static final int DISPLAY_NAME_COLUMN = 1;
    protected static final int ICON_POSITION = 0;

    protected List<T> myItems;

    public UtPyMemberSelectionTable(Collection<T> items) {
        myItems = new ArrayList<>(items);

        TableColumnModel model = getColumnModel();
        model.getColumn(DISPLAY_NAME_COLUMN).setCellRenderer(new MyTableRenderer<>(this));
        TableColumn checkBoxColumn = model.getColumn(CHECKED_COLUMN);
        TableUtil.setupCheckboxColumn(checkBoxColumn);
        checkBoxColumn.setCellRenderer(new MyBooleanRenderer<>(this));
        setPreferredScrollableViewportSize(JBUI.size(400, -1));
        setVisibleRowCount(12);
        getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        new MyEnableDisableAction().register();
    }

    public void setItems(Collection<T> items) {
        myItems = new ArrayList<>(items);
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
            return ContainerUtil.getFirstItem(getSelectedMemberInfos());
        }
        return null;
    }

    public Collection<T> getSelectedMemberInfos() {
        ArrayList<T> list = new ArrayList<>(myItems.size());
        for (T info : myItems) {
            if (info.isChecked()) {
                list.add(info);
            }
        }
        return list;
    }

    private class MyEnableDisableAction extends EnableDisableAction {

        @Override
        protected JTable getTable() {
            return UtPyMemberSelectionTable.this;
        }

        @Override
        protected void applyValue(int[] rows, boolean valueToBeSet) {
            List<T> changedInfo = new ArrayList<>();
            for (int row : rows) {
                final T memberInfo = myItems.get(row);
                memberInfo.setChecked(valueToBeSet);
                changedInfo.add(memberInfo);
            }
//            fireMemberInfoChange(changedInfo);
            final int[] selectedRows = getSelectedRows();
//            myTableModel.fireTableDataChanged();
            final ListSelectionModel selectionModel = getSelectionModel();
            for (int selectedRow : selectedRows) {
                selectionModel.addSelectionInterval(selectedRow, selectedRow);
            }
        }

        @Override
        protected boolean isRowChecked(final int row) {
            return myItems.get(row).isChecked();
        }
    }

    private static class MyBooleanRenderer<T extends UtPyTableItem> extends BooleanTableCellRenderer {
        private final UtPyMemberSelectionTable<T> myTable;

        MyBooleanRenderer(UtPyMemberSelectionTable<T> table) {
            myTable = table;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JCheckBox) {
                int modelColumn = myTable.convertColumnIndexToModel(column);
                T itemInfo = myTable.myItems.get(row);
                component.setEnabled(modelColumn == CHECKED_COLUMN || itemInfo.isChecked());
            }
            return component;
        }
    }

    private static class MyTableRenderer<T extends UtPyTableItem> extends ColoredTableCellRenderer {
        private final UtPyMemberSelectionTable<T> myTable;

        MyTableRenderer(UtPyMemberSelectionTable<T> table) {
            myTable = table;
        }

        @Override
        public void customizeCellRenderer(@NotNull JTable table, final Object value,
                                          boolean isSelected, boolean hasFocus, final int row, final int column) {

            final int modelColumn = myTable.convertColumnIndexToModel(column);
            final T item = myTable.myItems.get(row);
//            setToolTipText(myTable.myMemberInfoModel.getTooltipText(memberInfo));
            if (modelColumn == DISPLAY_NAME_COLUMN) {
                Icon itemIcon = item.getIcon();
                RowIcon icon = IconManager.getInstance().createRowIcon(3);
                icon.setIcon(itemIcon, ICON_POSITION);
//                myTable.setVisibilityIcon(memberInfo, icon);
                setIcon(icon);
            }
            else {
                setIcon(null);
            }
            setIconOpaque(false);
            setOpaque(false);
//            final boolean cellEditable = myTable.myMemberInfoModel.isMemberEnabled(memberInfo);
//            setEnabled(cellEditable);

            if (value == null) return;
//            final int problem = myTable.myMemberInfoModel.checkForProblems(memberInfo);
//            Color c = null;
//            if (problem == MemberInfoModel.ERROR) {
//                c = JBColor.RED;
//            }
//            else if (problem == MemberInfoModel.WARNING && !isSelected) {
//                c = JBColor.BLUE;
//            }
//            append((String)value, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, c));
        }

    }
}
