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
package lcmc.gui.resources.vms;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.data.vm.VmsXml;
import lcmc.data.vm.VmsXml.SerialData;
import lcmc.data.Value;
import lcmc.gui.Browser;
import lcmc.gui.widget.Widget;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import org.w3c.dom.Node;

/**
 * This class holds info about virtual serial device.
 */
final class SerialInfo extends ParallelSerialInfo {
    /** Returns "add new" button. */
    static MyButton getNewBtn(final DomainInfo vdi) {
        final MyButton newBtn = new MyButton("Add Serial Device");
        newBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        vdi.addSerialsPanel();
                    }
                });
                t.start();
            }
        });
        return newBtn;
    }

    /** Creates the SerialInfo object. */
    SerialInfo(final String name, final Browser browser, final DomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);
    }

    /** Returns data for the table. */
    @Override
    protected Object[][] getTableData(final String tableName) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (DomainInfo.SERIAL_TABLE.equals(tableName)) {
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
    @Override
         void updateParameters() {
         final Map<String, SerialData> serials =
             getVMSVirtualDomainInfo().getSerials();
         if (serials != null) {
             final SerialData serialData = serials.get(getName());
             if (serialData != null) {
                 for (final String param : getParametersFromXML()) {
                     final Value oldValue = getParamSaved(param);
                     Value value = getParamSaved(param);
                     final Widget wi = getWidget(param, null);
                     for (final Host h
                         : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                         final VmsXml vmsXml = getBrowser().getVmsXml(h);
                         if (vmsXml != null) {
                             final Value savedValue =
                                 serialData.getValue(param);
                             if (savedValue != null) {
                                 value = savedValue;
                             }
                         }
                     }
                     if (!Tools.areEqual(value, oldValue)) {
                         getResource().setValue(param, value);
                         if (wi != null) {
                             /* only if it is not changed by user. */
                             wi.setValue(value);
                         }
                     }
                 }
             }
         }
         updateTable(DomainInfo.HEADER_TABLE);
         updateTable(DomainInfo.SERIAL_TABLE);
         checkResourceFields(null, getParametersFromXML());
     }

    /** Returns string representation. */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(30);
        final Value type = getParamSaved(SerialData.TYPE);
        if (type == null || type.isNothingSelected()) {
            s.append("new serial device...");
        } else {
            s.append(getName());
        }
        return s.toString();
    }

    /** Removes this serial device without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Application.RunMode runMode) {
        if (Application.isTest(runMode)) {
            return;
        }
        final String virshOptions = getVMSVirtualDomainInfo().getVirshOptions();
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null) {
                final Map<String, String> parameters =
                    new HashMap<String, String>();
                parameters.put(SerialData.SAVED_TYPE,
                               getParamSaved(SerialData.TYPE).getValueForConfig());
                vmsXml.removeSerialXML(
                    getVMSVirtualDomainInfo().getDomainName(),
                                    parameters,
                                    virshOptions);
            }
        }
        getBrowser().periodicalVmsUpdate(
                getVMSVirtualDomainInfo().getDefinedOnHosts());
        removeNode();
    }

    /** Returns "add new" button. */
    @Override
    protected MyButton getNewBtn0(final DomainInfo vdi) {
        return getNewBtn(vdi);
    }

    /** Modify device xml. */
    @Override
    protected void modifyXML(final VmsXml vmsXml,
                             final Node node,
                             final String domainName,
                             final Map<String, String> params) {
        if (vmsXml != null) {
            vmsXml.modifySerialXML(node, domainName, params);
        }
    }

    /** Return table name that appears on the screen. */
    @Override
    protected String getTableScreenName() {
        return "Serial Device";
    }

    /** Return table name. */
    @Override
    protected String getTableName() {
        return DomainInfo.SERIAL_TABLE;
    }

    /** Returns device parameters. */
    @Override
    protected Map<String, String> getHWParameters(final boolean allParams) {
        final Map<String, String> parameters =
                                          super.getHWParameters(allParams);
        setName("serial "
                + getParamSaved(SerialData.TARGET_PORT)
                + " / "
                + getParamSaved(SerialData.TYPE));
        return parameters;
    }
}
