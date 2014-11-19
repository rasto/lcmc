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

import org.junit.Before;
import org.junit.Test;

import javax.swing.table.TableModel;

import static org.junit.Assert.assertTrue;

public class NetworkModelTest {
    private static final String SOME_IPS = "1.2.3.4, 1.2.3.5";
    private static final String SOME_NETWORK = "1.2.3.0";
    private static final String SOME_CIDR = "24";
    private final NetworkModel model = new NetworkModel();

    @Before
    public void initTable() {
        model.updateTable();

    }

    @Test
    public void settingIpsShouldUpdateTable() {
        model.setIps(SOME_IPS);

        assertTrue("table does not contain " + SOME_IPS, assertTableContains(SOME_IPS));
    }

    @Test
    public void settingNetworkShouldUpdateTable() {
        model.setIps(SOME_NETWORK);

        assertTrue("table does not contain " + SOME_NETWORK, assertTableContains(SOME_NETWORK));
    }

    @Test
    public void settingCidrShouldUpdateTable() {
        model.setIps(SOME_CIDR);

        assertTrue("table does not contain " + SOME_CIDR, assertTableContains(SOME_CIDR));
    }

    private boolean assertTableContains(final String string) {
        final TableModel tableModel = model.getTableModel();
        for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
            for (int colIndex = 0; colIndex < tableModel.getColumnCount(); colIndex++) {
                final Object value = tableModel.getValueAt(rowIndex, colIndex);
                if (value != null && value.toString().equals(string)) {
                    return true;
                }
            }
        }
        return false;
    }
}