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
import drbd.gui.GuiComboBox;
import drbd.data.VMSXML;
import drbd.data.VMSXML.SerialData;
import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.w3c.dom.Node;

/**
 * This class holds info about virtual serial device.
 */
public class VMSSerialInfo extends VMSParallelSerialInfo {
    /** Creates the VMSSerialInfo object. */
    public VMSSerialInfo(final String name, final Browser browser,
                        final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);
    }

    /** Returns data for the table. */
    protected final Object[][] getTableData(final String tableName) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (VMSVirtualDomainInfo.SERIAL_TABLE.equals(tableName)) {
            if (getResource().isNew()) {
                return new Object[][]{};
            }
            return new Object[][]{getVMSVirtualDomainInfo().getSerialDataRow(
                                getName(),
                                null,
                                getVMSVirtualDomainInfo().getSerials(),
                                true)};
        }
        return new Object[][]{};
    }

    /** Updates parameters. */
    public final void updateParameters() {
        final Map<String, SerialData> serials =
                              getVMSVirtualDomainInfo().getSerials();
        if (serials != null) {
            final SerialData serialData = serials.get(getName());
            if (serialData != null) {
                for (final String param : getParametersFromXML()) {
                    final String oldValue = getParamSaved(param);
                    String value = getParamSaved(param);
                    final GuiComboBox cb = paramComboBoxGet(param, null);
                    for (final Host h
                             : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                        final VMSXML vmsxml = getBrowser().getVMSXML(h);
                        if (vmsxml != null) {
                            final String savedValue =
                                                  serialData.getValue(param);
                            if (savedValue != null) {
                                value = savedValue;
                            }
                        }
                    }
                    if (!Tools.areEqual(value, oldValue)) {
                        getResource().setValue(param, value);
                        if (cb != null) {
                            /* only if it is not changed by user. */
                            cb.setValue(value);
                        }
                    }
                }
            }
        }
        updateTable(VMSVirtualDomainInfo.HEADER_TABLE);
        updateTable(VMSVirtualDomainInfo.SERIAL_TABLE);
    }

    /** Returns string representation. */
    public final String toString() {
        final StringBuffer s = new StringBuffer(30);
        final String type = getParamSaved(SerialData.TYPE);
        if (type == null) {
            s.append("new serial device...");
        } else {
            s.append(getName());
        }
        return s.toString();
    }

    /** Removes this serial device without confirmation dialog. */
    protected final void removeMyselfNoConfirm(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(SerialData.SAVED_TYPE,
                               getParamSaved(SerialData.TYPE));
                vmsxml.removeSerialXML(
                                    getVMSVirtualDomainInfo().getDomainName(),
                                    parameters);
            }
        }
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
    }

    /** Returns "add new" button. */
    protected final MyButton getNewBtn0(final VMSVirtualDomainInfo vdi) {
        return getNewBtn(vdi);
    }

    /** Returns "add new" button. */
    public static MyButton getNewBtn(final VMSVirtualDomainInfo vdi) {
        final MyButton newBtn = new MyButton("Add Serial Device");
        newBtn.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    public void run() {
                        vdi.addSerialsPanel();
                    }
                });
                t.start();
            }
        });
        return newBtn;
    }

    /** Modify device xml. */
    protected final void modifyXML(final VMSXML vmsxml,
                                   final Node node,
                                   final String domainName,
                                   final Map<String, String> params) {
        if (vmsxml != null) {
            vmsxml.modifySerialXML(node, domainName, params);
        }
    }

    /** Return table name that appears on the screen. */
    protected final String getTableScreenName() {
        return "Serial Device";
    }

    /** Return table name. */
    protected final String getTableName() {
        return VMSVirtualDomainInfo.SERIAL_TABLE;
    }

    /** Returns device parameters. */
    protected final Map<String, String> getHWParametersAndSave() {
        final Map<String, String> parameters = super.getHWParametersAndSave();
        setName("serial "
                + getParamSaved(SerialData.TARGET_PORT)
                + " / "
                + getParamSaved(SerialData.TYPE));
        return parameters;
    }
}
