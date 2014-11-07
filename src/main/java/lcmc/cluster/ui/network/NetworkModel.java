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

import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

public class NetworkModel {
    final SimpleStringProperty network = new SimpleStringProperty();
    final SimpleStringProperty ips = new SimpleStringProperty();
    final SimpleIntegerProperty cidr = new SimpleIntegerProperty();

    final DefaultTableModel tableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public void initTable() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        rows.add(new Object[]{"Network", network.get()});
        rows.add(new Object[]{"IPs", ips.get()});
        rows.add(new Object[]{"CIDR", cidr.get()});
        tableModel.setDataVector(rows.toArray(new Object[rows.size()][]), new Object[2]);
        addTableListener(network);
        addTableListener(cidr);
        addTableListener(ips);
    }

    public TableModel getTableModel() {
        return tableModel;
    }

    public void setNetwork(final String network) {
        this.network.set(network);
    }

    public void setIps(final String ips) {
        this.ips.set(ips);
    }

    public void setCidr(final Integer cidr) {
        this.cidr.set(cidr);
    }

    private void addTableListener(final Property property) {
        property.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldValue, String newValue) {
                initTable();
            }
        });
    }
}
