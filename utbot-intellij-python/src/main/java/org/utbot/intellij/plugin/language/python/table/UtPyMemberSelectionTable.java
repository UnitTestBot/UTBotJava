package org.utbot.intellij.plugin.language.python.table;

import com.intellij.openapi.actionSystem.BackgroundableDataProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.refactoring.ui.EnableDisableAction;
import com.intellij.ui.*;
import com.intellij.ui.icons.RowIcon;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.jetbrains.python.psi.PyElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UtPyMemberSelectionTable<T extends UtPyTableItem> extends JBTable implements BackgroundableDataProvider {
    protected static final int CHECKED_COLUMN = 0;
    protected static final int DISPLAY_NAME_COLUMN = 1;
    protected static final int ICON_POSITION = 0;

    protected List<T> myItems;
    protected MyTableModel<T> myTableModel;
    private DataProvider dataProvider;

    public UtPyMemberSelectionTable(Collection<T> items) {
        myItems = new ArrayList<>(items);
        myTableModel = new MyTableModel<>(this);
        setModel(myTableModel);

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
    public @Nullable DataProvider createBackgroundDataProvider() {
        if (dataProvider == null) {
            dataProvider = new DataProvider() {
                @Override
                public @Nullable Object getData(@NotNull @NonNls String dataId) {
                    if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
                        for (UtPyTableItem item : getSelectedMemberInfos()) {
                            PyElement pyElement = item.getContent();
                            if (pyElement != null) return pyElement;
                        }
                    }
                    return null;
                }
            };
        }
        return dataProvider;
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
            for (int row : rows) {
                final T memberInfo = myItems.get(row);
                memberInfo.setChecked(valueToBeSet);
            }
            final int[] selectedRows = getSelectedRows();
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
            if (modelColumn == DISPLAY_NAME_COLUMN) {
                Icon itemIcon = item.getIcon();
                RowIcon icon = IconManager.getInstance().createRowIcon(3);
                icon.setIcon(itemIcon, ICON_POSITION);
                setIcon(icon);
            }
            else {
                setIcon(null);
            }
            setIconOpaque(false);
            setOpaque(false);

            if (value == null) return;
            append((String)value);
        }

    }

    protected static class MyTableModel<T extends UtPyTableItem> extends AbstractTableModel {
        private final UtPyMemberSelectionTable<T> myTable;
        private Boolean removePrefix;

        public MyTableModel(UtPyMemberSelectionTable<T> table) {
            myTable = table;
        }

        private void initRemovePrefix() {
            List<String> names = new ArrayList<>();
            for (UtPyTableItem item: myTable.myItems) {
                names.add(item.getIdName());
            }
            removePrefix = Utils.haveCommonPrefix(names);
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return myTable.myItems.size();
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if (columnIndex == CHECKED_COLUMN) {
                return Boolean.class;
            }
            return super.getColumnClass(columnIndex);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (removePrefix == null) {
                initRemovePrefix();
            }
            final T itemInfo = myTable.myItems.get(rowIndex);
            if (columnIndex == CHECKED_COLUMN) {
                return itemInfo.isChecked();
            } else if (columnIndex == DISPLAY_NAME_COLUMN) {
                if (removePrefix) {
                    return Utils.getSuffix(itemInfo.getIdName());
                }
                return itemInfo.getIdName();
            } else {
                throw new RuntimeException("Incorrect column index");
            }
        }

        @Override
        public String getColumnName(int column) {
            if (column == CHECKED_COLUMN) {
                return " ";
            } else if (column == DISPLAY_NAME_COLUMN) {
                return "Members";
            } else {
                throw new RuntimeException("Incorrect column index");
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == CHECKED_COLUMN;
        }


        @Override
        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
            if (columnIndex == CHECKED_COLUMN) {
                myTable.myItems.get(rowIndex).setChecked((Boolean) aValue);
            }
        }
    }
}
