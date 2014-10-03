/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.common.ui.treemenu;

import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.CategoryInfo;
import lcmc.common.ui.Info;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/** Renders the cells for the menu. */
public class CellRenderer extends DefaultTreeCellRenderer {

    CellRenderer() {
        super();
        setBackgroundNonSelectionColor(Browser.PANEL_BACKGROUND);
        setBackgroundSelectionColor(Tools.getDefaultColor("ViewPanel.Status.Background"));
        setTextNonSelectionColor(Tools.getDefaultColor("ViewPanel.Foreground"));
        setTextSelectionColor(Tools.getDefaultColor("ViewPanel.Status.Foreground"));
    }

    /**
     * Returns the CellRenderer component, setting up the icons and
     * tooltips.
     */
    @Override
    public java.awt.Component getTreeCellRendererComponent(
            final JTree tree,
            final Object value,
            final boolean sel,
            final boolean expanded,
            final boolean leaf,
            final int row,
            final boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        final Info info = (Info) ((DefaultMutableTreeNode) value).getUserObject();
        if (info == null) {
            return this;
        }
        if (leaf) {
            final ImageIcon icon = info.getMenuIcon(Application.RunMode.LIVE);
            if (icon != null) {
                setIcon(icon);
            }
            setToolTipText("");
        } else {
            setToolTipText("");
            ImageIcon icon = info.getCategoryIcon(Application.RunMode.LIVE);
            if (icon == null) {
                icon = CategoryInfo.CATEGORY_ICON;
            }
            setIcon(icon);
        }
        return this;
    }
}
