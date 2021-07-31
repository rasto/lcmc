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

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import lcmc.common.domain.util.Tools;

public class NetworkModel {
    String network;
    String ips;
    Integer cidr;

    final DefaultTableModel tableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public void updateTable() {
        final List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"Network", network});
        rows.add(new Object[]{"IPs", ips});
        rows.add(new Object[]{"CIDR", cidr});
        synchronized (this) {
            tableModel.setDataVector(rows.toArray(new Object[rows.size()][]), new Object[2]);
        }
    }

    public TableModel getTableModel() {
        return tableModel;
    }

    public void setNetwork(final String network) {
        if (!Tools.areEqual(this.network, network)) {
            this.network = network;
            updateTable();
        }
    }

    public void setIps(final String ips) {
        if (!Tools.areEqual(this.ips, ips)) {
            this.ips = ips;
            updateTable();
        }
    }

    public void setCidr(final Integer cidr) {
        if (!Tools.areEqual(this.cidr, cidr)) {
            this.cidr = cidr;
            updateTable();
        }
    }
}
