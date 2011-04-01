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
import drbd.data.VMSXML.ParallelData;
import drbd.data.Host;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;

import java.util.Map;
import java.util.HashMap;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.w3c.dom.Node;

/**
 * This class holds info about virtual parallel device.
 */
final class VMSParallelInfo extends VMSParallelSerialInfo {
    /** Creates the VMSParallelInfo object. */
    VMSParallelInfo(final String name, final Browser browser,
                   final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);
    }

    /** Returns data for the table. */
    @Override protected Object[][] getTableData(final String tableName) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (VMSVirtualDomainInfo.PARALLEL_TABLE.equals(tableName)) {
            if (getResource().isNew()) {
                return new Object[][]{};
            }
            return new Object[][]{getVMSVirtualDomainInfo().getParallelDataRow(
                                getName(),
                                null,
                                getVMSVirtualDomainInfo().getParallels(),
                                true)};
        }
        return new Object[][]{};
    }

    /** Updates parameters. */
    @Override void updateParameters() {
        final Map<String, ParallelData> parallels =
                              getVMSVirtualDomainInfo().getParallels();
        if (parallels != null) {
            final ParallelData parallelData = parallels.get(getName());
            if (parallelData != null) {
                for (final String param : getParametersFromXML()) {
                    final String oldValue = getParamSaved(param);
                    String value = getParamSaved(param);
                    final GuiComboBox cb = paramComboBoxGet(param, null);
                    for (final Host h
                            : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                        final VMSXML vmsxml = getBrowser().getVMSXML(h);
                        if (vmsxml != null) {
                            final String savedValue =
                                                  parallelData.getValue(param);
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
        updateTable(VMSVirtualDomainInfo.PARALLEL_TABLE);
        checkResourceFieldsChanged(null, getParametersFromXML());
    }

    /** Returns string representation. */
    @Override public String toString() {
        final StringBuffer s = new StringBuffer(30);
        final String type = getParamSaved(ParallelData.TYPE);
        if (type == null) {
            s.append("new parallel device...");
        } else {
            s.append(getName());
        }
        return s.toString();
    }

    /** Removes this parallel device without confirmation dialog. */
    @Override protected void removeMyselfNoConfirm(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(ParallelData.SAVED_TYPE,
                               getParamSaved(ParallelData.TYPE));
                vmsxml.removeParallelXML(
                                    getVMSVirtualDomainInfo().getDomainName(),
                                    parameters);
            }
        }
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
    }

    /** Returns "add new" button. */
    @Override protected MyButton getNewBtn0(final VMSVirtualDomainInfo vdi) {
        return getNewBtn(vdi);
    }

    /** Returns "add new" button. */
    static MyButton getNewBtn(final VMSVirtualDomainInfo vdi) {
        final MyButton newBtn = new MyButton("Add Parallel Device");
        newBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        vdi.addParallelsPanel();
                    }
                });
                t.start();
            }
        });
        return newBtn;
    }

    /** Modify device xml. */
    @Override protected void modifyXML(final VMSXML vmsxml,
                                       final Node node,
                                       final String domainName,
                                       final Map<String, String> params) {
        if (vmsxml != null) {
            vmsxml.modifyParallelXML(node, domainName, params);
        }
    }

    /** Return table name that appears on the screen. */
    @Override protected String getTableScreenName() {
        return "Parallel Device";
    }

    /** Return table name. */
    @Override protected String getTableName() {
        return VMSVirtualDomainInfo.PARALLEL_TABLE;
    }

    /** Returns device parameters. */
    @Override protected Map<String, String> getHWParametersAndSave(
                                                   final boolean allParams) {
        final Map<String, String> parameters =
                                    super.getHWParametersAndSave(allParams);
        setName("parallel "
                + getParamSaved(ParallelData.TARGET_PORT)
                + " / "
                + getParamSaved(ParallelData.TYPE));
        return parameters;
    }
}
