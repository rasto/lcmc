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

package lcmc.utilities;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import javax.swing.SwingConstants;
import java.awt.Color;

/**
 * Cells with jlabels, widths and colors.
 */
public class MyButtonCellRenderer extends MyButton
                                  implements TableCellRenderer {
    /** Serial version uid. */
    private static final long serialVersionUID = 1L;

    /** Sets background color and padding in jlabels for every cell. */
    @Override
    public final Component getTableCellRendererComponent(
                                                      final JTable table,
                                                      final Object value,
                                                      final boolean isSelected,
                                                      final boolean hasFocus,
                                                      final int row,
                                                      final int column) {
        if (!(value instanceof MyButton)) {
            return this;
        }
        final MyButton button = (MyButton) value;
        if (button == null) {
            setText("");
            getModel().setPressed(false);
            getModel().setArmed(false);
            getModel().setRollover(false);
        } else {
            if (button.getModel().isPressed()) {
                setOpaque(true);
            } else {
                setOpaque(button.isOpaque());
            }
            setText(button.getText());
            setIcon(button.getIcon());
            setMargin(button.getInsets());
            setFont(button.getFont());
            setIconTextGap(button.getIconTextGap());
            getModel().setPressed(button.getModel().isPressed());
            getModel().setArmed(button.getModel().isArmed());
            getModel().setRollover(button.getModel().isRollover());
        }

        final int al = getColumnAlignment(column);
        setHorizontalAlignment(al);
        final Object v = table.getValueAt(row, 0);
        if (v instanceof MyButton) {
            final String key = ((MyButton) v).getText();
            final Color bg = getRowColor(key);
            setBackgroundColor(bg);
            //setToolTipText(button.getText());
        }
        //(((MyButton) this).createToolTip()).setTipText("asdf");
        //(((MyButton) button).createToolTip()).setTipText("asdff");
        return this;
    }

    /** Alignment for the specified column. */
    protected int getColumnAlignment(final int column) {
        return SwingConstants.LEFT;
    }

    /** Retrurns color for some rows. */
    protected Color getRowColor(final String key) {
        return null;
    }
}

