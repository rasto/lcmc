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

package lcmc.cluster.ui.network;

import lcmc.cluster.ui.ClusterBrowser;
import javax.swing.JComponent;
import javax.swing.JTable;

public class NetworkView {
    private static final int HEADER_COL_WIDTH = 70;
    final NetworkModel model;
    private JTable table;

    public NetworkView(final NetworkModel model) {
        this.model = model;
    }

    public void show(final JComponent panel) {
        table = new JTable(model.getTableModel());
        table.setShowGrid(false);
        setFirstColWidth();
        table.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        panel.add(table);
    }

    public void update() {
        if (table != null) {
            setFirstColWidth();
        }
    }

    private void setFirstColWidth() {
        table.getColumnModel().getColumn(0).setMinWidth(HEADER_COL_WIDTH);
        table.getColumnModel().getColumn(0).setMaxWidth(HEADER_COL_WIDTH);
    }
}
