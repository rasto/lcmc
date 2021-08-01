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
package lcmc.vm.ui.resource;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.w3c.dom.Node;

import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Access;
import lcmc.common.ui.Browser;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.vm.domain.VmsXml;
import lcmc.vm.domain.data.ParallelData;

/**
 * This class holds info about virtual parallel device.
 */
@Named
public class ParallelInfo extends ParallelSerialInfo {
    private final ClusterTreeMenu clusterTreeMenu;

    public ParallelInfo(Application application, SwingUtils swingUtils, Access access, MainData mainData,
            WidgetFactory widgetFactory, MenuFactory menuFactory, ClusterTreeMenu clusterTreeMenu) {
        super(application, swingUtils, access, mainData, widgetFactory, menuFactory, clusterTreeMenu);
        this.clusterTreeMenu = clusterTreeMenu;
    }

    @Override
    void init(final String name, final Browser browser, final DomainInfo vmsVirtualDomainInfo) {
        super.init(name, browser, vmsVirtualDomainInfo);
    }

    /**
     * Returns data for the table.
     */
    @Override
    protected Object[][] getTableData(final String tableName) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (DomainInfo.PARALLEL_TABLE.equals(tableName)) {
            if (getResource().isNew()) {
                return new Object[][]{};
            }
            return new Object[][]{getVMSVirtualDomainInfo().getParallelDataRow(getName(),
                                null,
                                getVMSVirtualDomainInfo().getParallels(),
                                true)};
        }
        return new Object[][]{};
    }

    /** Updates parameters. */
    @Override
         void updateParameters() {
         final Map<String, ParallelData> parallels =
             getVMSVirtualDomainInfo().getParallels();
         if (parallels != null) {
             final ParallelData parallelData = parallels.get(getName());
             if (parallelData != null) {
                 for (final String param : getParametersFromXML()) {
                     final Value oldValue = getParamSaved(param);
                     Value value = getParamSaved(param);
                     final Widget wi = getWidget(param, null);
                     for (final Host h
                         : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                         final VmsXml vmsXml = getBrowser().getVmsXml(h);
                         if (vmsXml != null) {
                             final Value savedValue =
                                 parallelData.getValue(param);
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
         updateTable(DomainInfo.PARALLEL_TABLE);
         checkResourceFields(null, getParametersFromXML());
     }

    /** Returns string representation. */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(30);
        final Value type = getParamSaved(ParallelData.TYPE);
        if (type == null || type.isNothingSelected()) {
            s.append("new parallel device...");
        } else {
            s.append(getName());
        }
        return s.toString();
    }

    /** Removes this parallel device without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Application.RunMode runMode) {
        if (Application.isTest(runMode)) {
            return;
        }
        final String virshOptions = getVMSVirtualDomainInfo().getVirshOptions();
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null) {
                final Map<String, String> parameters = new HashMap<>();
                parameters.put(ParallelData.SAVED_TYPE,
                               getParamSaved(ParallelData.TYPE).getValueForConfig());
                vmsXml.removeParallelXML(
                    getVMSVirtualDomainInfo().getDomainName(),
                                    parameters,
                                    virshOptions);
            }
        }
        getBrowser().periodicalVmsUpdate(
                getVMSVirtualDomainInfo().getDefinedOnHosts());
        clusterTreeMenu.removeNode(getNode());
    }

    /** Returns "add new" button. */
    @Override
    protected MyButton getNewBtn0(final DomainInfo vdi) {
        return vdi.getNewParallelBtn();
    }

    /** Modify device xml. */
    @Override
    protected void modifyXML(final VmsXml vmsXml,
                             final Node node,
                             final String domainName,
                             final Map<String, String> params) {
        if (vmsXml != null) {
            vmsXml.modifyParallelXML(node, domainName, params);
        }
    }

    /** Return table name that appears on the screen. */
    @Override
    protected String getTableScreenName() {
        return "Parallel Device";
    }

    /** Return table name. */
    @Override
    protected String getTableName() {
        return DomainInfo.PARALLEL_TABLE;
    }

    /** Returns device parameters. */
    @Override
    protected Map<String, String> getHWParameters(final boolean allParams) {
        final Map<String, String> parameters =
                                        super.getHWParameters(allParams);
        setName("parallel "
                + getParamSaved(ParallelData.TARGET_PORT)
                + " / "
                + getParamSaved(ParallelData.TYPE));
        return parameters;
    }
}
