/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * by Rasto Levrinc.
 *
 * Copyright (C) 2009, Rastislav Levrinc
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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

package drbd.utilities;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Insets;

/**
 * Cells with jlabels, widths and colors.
 */
public class MyCellRenderer extends JLabel
                             implements TableCellRenderer {
    /** Serial version uid. */
    private static final long serialVersionUID = 1L;
    /** Border for jlabel so, that there is spacing in the table. Table spacing
     * cannot be used because of row colors. */
    private static final EmptyBorder EMPTY_BORDER =
                                       new EmptyBorder(new Insets(0, 4, 0, 4));
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
    public final Component getTableCellRendererComponent(
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
        final String key = ((JLabel) table.getValueAt(row, 0)).getText();
        final Color bg = getRowColor(key);
        ret.setBackground(bg);
        ret.setBorder(EMPTY_BORDER);
        return ret;
    }

    /**
     * Alignment for the specified column.
     */
    protected int getColumnAlignment(final int column) {
        return SwingConstants.LEFT;
    }

    /**
     * Retrurns color for some rows.
     */
    protected Color getRowColor(final String key) {
        return null;
    }
}

