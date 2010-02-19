/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package drbd.gui.resources;

import drbd.gui.Browser;
import drbd.utilities.Tools;
import javax.swing.ImageIcon;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.table.TableCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.BoxLayout;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.border.EmptyBorder;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Point;
import java.awt.Color;
import java.awt.Insets;

import java.util.Comparator;
import java.util.List;
import javax.swing.RowSorter;


/**
 * This class holds info data for a category.
 */
public class CategoryInfo extends Info {
    /** Info panel. */
    private JComponent infoPanel = null;
    /** Table. */
    private JTable table = null;
    /** Table model. */
    private DefaultTableModel tableModel = null;
    /** Sorter for the table. */
    private TableRowSorter sorter = null;

    /** Border for jlabel so, that there is spacing in the table. Table spacing
     * cannot be used because of row colors. */
    private static final EmptyBorder EMPTY_BORDER =
                                    new EmptyBorder(new Insets(0, 4, 0, 4));
    /**
     * Prepares a new <code>CategoryInfo</code> object.
     */
    public CategoryInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /**
     * Info panel for the category.
     */
    public String getInfo() {
        return null;
    }

    /**
     * Returns the icon.
     */
    public ImageIcon getMenuIcon(final boolean testOnly) {
        return Browser.CATEGORY_ICON;
    }

    /**
     * Returns info panel for this resource.
     */
    @SuppressWarnings("unchecked")
    public JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        final String[] colNames = getColumnNames();
        if (colNames != null) {
            final Object[][] data = getTableData();
            tableModel = new DefaultTableModel(data, colNames);
            table = new JTable(tableModel) {
                /** Serial version uid. */
                private static final long serialVersionUID = 1L;
                /**
                 * Overriding so that jlabels show up.
                 */
                public Class getColumnClass(final int c) {
                    return getValueAt(0, c).getClass();
                }
            };
            //table.setAutoCreateRowSorter(true);
            sorter = new TableRowSorter<DefaultTableModel>(tableModel);
            for (int i = 0; i < colNames.length; i++) {
                final Comparator<String> c = getColComparator(i);
                if (c != null) {
                    sorter.setComparator(i, c);
                }

            }
            table.setRowSorter((RowSorter) sorter);
            sorter.setSortsOnUpdates(true);

            table.getTableHeader().setReorderingAllowed(true);

            infoPanel.setBackground(Browser.PANEL_BACKGROUND);
            table.setBackground(Browser.PANEL_BACKGROUND);
            table.setDefaultRenderer(Object.class, new MyCellRenderer());
            final int h = getRowHeight();
            if (h >= 0) {
                table.setRowHeight(h);
            }
            table.addMouseListener(new MouseAdapter() {
                public final void mouseClicked(final MouseEvent e) {
                    final JTable table = (JTable) e.getSource();
                    final Point p = e.getPoint();
                    final int row = table.rowAtPoint(p);
                    final String key =
                            ((JLabel) table.getValueAt(row, 0)).getText();
                    rowClicked(key);
                }
            });
            final JScrollPane sp = new JScrollPane(table);
            sp.getViewport().setBackground(Browser.PANEL_BACKGROUND);
            sp.setBackground(Browser.PANEL_BACKGROUND);
            infoPanel.add(sp);
            resizeTable(table);
        }
        return infoPanel;
    }

    /**
     * Alignment for the specified column.
     */
    protected int getColumnAlignment(final int column) {
        return SwingConstants.LEFT;
    }

    /**
     * Returns columns for the table.
     */
    protected String[] getColumnNames() {
        return null;
    }

    /**
     * Returns data for the table.
     */
    protected Object[][] getTableData() {
        return null;
    }

    /**
     * Updates data in the table.
     */
    @SuppressWarnings("unchecked")
    public final void update() {
        if (tableModel != null) {
            final String[] colNames = getColumnNames();
            if (colNames != null) {
                final Object[][] data = getTableData();
                Tools.debug(this, "update table in: " + getName());

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        final List sortKeys = sorter.getSortKeys();
                        tableModel.setDataVector(data, colNames);
                        sorter.setSortKeys(sortKeys);
                        tableModel.fireTableDataChanged();
                        resizeTable(table);
                    }
                });
            }
        }
    }

    /**
     * Execute when row in the table was clicked.
     */
    protected void rowClicked(final String key) {
        /* do nothing */
    }

    /**
     * Returns row height for the table.
     */
    protected int getRowHeight() {
        return -1;
    }

    /**
     * Cells with jlabels, widths and colors.
     */
    private class MyCellRenderer extends JLabel
                                 implements TableCellRenderer {
        /** Serial version uid. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new MyCellRenderer object.
         */
        public MyCellRenderer() {
            super();
            setOpaque(true);
        }

        /**
         * Sets background color and padding in jlabels for every cell.
         */
        public Component getTableCellRendererComponent(
                                                final JTable table,
                                                final Object value,
                                                final boolean isSelected,
                                                final boolean hasFocus,
                                                final int row,
                                                final int column) {
            JLabel ret;
            if (value instanceof JLabel) {
                ret  = (JLabel) value;
            } else {
                setText(value.toString());
                ret = this;
            }
            final int al = getColumnAlignment(column);
            ret.setHorizontalAlignment(al);
            final String key =
                            ((JLabel) table.getValueAt(row, 0)).getText();
            final Color bg = getColorForRow(key);
            ret.setBackground(bg);
            ret.setBorder(EMPTY_BORDER);
            return ret;
        }
    }

    /**
     * Retrurns color for some rows.
     */
    protected Color getColorForRow(final String key) {
        return null;
    }

    /**
     * Resize table.
     */
    public final void resizeTable(final JTable table) {
        final int margin = 5;

        for (int i = 0; i < table.getColumnCount(); i++) {
            final int vColIndex = i;
            final DefaultTableColumnModel colModel =
                            (DefaultTableColumnModel) table.getColumnModel();
            final TableColumn col = colModel.getColumn(vColIndex);
            int width = 0;
            TableCellRenderer renderer = col.getHeaderRenderer();

            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }
            Component comp = renderer.getTableCellRendererComponent(
                                                        table,
                                                        col.getHeaderValue(),
                                                        false,
                                                        false,
                                                        0,
                                                        0);
            width = comp.getPreferredSize().width;
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, vColIndex);
                comp = renderer.getTableCellRendererComponent(
                                              table,
                                              table.getValueAt(r, vColIndex),
                                              false,
                                              false,
                        r, vColIndex);
                width = Math.max(width, comp.getPreferredSize().width);
            }
            width += 2 * margin;
            col.setPreferredWidth(width);
        }
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer())
                            .setHorizontalAlignment(SwingConstants.CENTER);
    }

    /**
     * Returns comparator for column.
     */
    protected Comparator<String> getColComparator(final int col) {
        return null;
    }
}
