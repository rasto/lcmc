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
package lcmc.gui.resources.crm;

import lcmc.data.*;
import lcmc.gui.Browser;
import lcmc.gui.resources.vms.VMSVirtualDomainInfo;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class holds info about VirtualDomain service in the cluster menu.
 */
public final class VirtualDomainInfo extends ServiceInfo {
    /** VirtualDomain in the VMs menu. */
    private VMSVirtualDomainInfo vmsVirtualDomainInfo = null;
    /** Pattern that captures a name from xml file name. */
    static final Pattern LIBVIRT_CONF_PATTERN =
                                            Pattern.compile(".*?([^/]+).xml$");
    /** Parameters. */
    private static final String CONFIG_PARAM = "config";
    private static final String HYPERVISOR_PARAM = "hypervisor";
    /** Hypervisor choices. */
    private static final Value[] HYPERVISORS = {new StringValue("qemu:///system"),
                                                new StringValue("xen:///"),
                                                new StringValue("lxc:///"),
                                                new StringValue("vbox:///"),
                                                new StringValue("openvz:///system"),
                                                new StringValue("uml:///system")};
    private static final String PARAM_ALLOW_MIGRATE = "allow-migrate";

    /** Creates the VirtualDomainInfo object. */
    public VirtualDomainInfo(final String name,
                      final ResourceAgent ra,
                      final Browser browser) {
        super(name, ra, browser);
    }

    /** Creates the VirtualDomainInfo object. */
    public VirtualDomainInfo(final String name,
                      final ResourceAgent ra,
                      final String hbId,
                      final Map<String, String> resourceNode,
                      final Browser browser) {
        super(name, ra, hbId, resourceNode, browser);
    }


    /** Returns the first on which this vm is running. */
    private Host getRunningOnHost() {
        final List<String> nodes = getRunningOnNodes(Application.RunMode.LIVE);
        if (nodes != null
            && !nodes.isEmpty()) {
            return getBrowser().getCluster().getHostByName(nodes.get(0));
        }
        return null;
    }

    /** Returns object with vm data. */
    VMSXML getVMSXML(final Host host) {
        return getBrowser().getVMSXML(host);
    }

    /** Removes the service without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Host dcHost,
                                         final Application.RunMode runMode) {
        super.removeMyselfNoConfirm(dcHost, runMode);
    }

    /** Sets service parameters with values from resourceNode hash. */
    @Override
	protected
    void setParameters(final Map<String, String> resourceNode) {
        super.setParameters(resourceNode);
        connectWithVMS();
    }

    /** Connects with VMSVirtualDomainInfo object. */
    @Override
    public VMSVirtualDomainInfo connectWithVMS() {
        final Value config = getParamSaved(CONFIG_PARAM);
        VMSVirtualDomainInfo newVMSVDI = null;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                final String name = vxml.getNameFromConfig(config.getValueForConfig());
                newVMSVDI = getBrowser().findVMSVirtualDomainInfo(name);
                if (newVMSVDI != null) {
                    newVMSVDI.setUsedByCRM(true);
                    break;
                }
            }
        }
        vmsVirtualDomainInfo = newVMSVDI;
        return newVMSVDI;
    }

    /** Adds vnc viewer menu items. */
    void addVncViewersToTheMenu(final Collection<UpdatableItem> items) {
        if (Tools.getApplication().isTightvnc()) {
            /* tight vnc test menu */
            final UpdatableItem tightvncViewerMenu = new MyMenuItem(
                            "start TIGHT VNC viewer",
                            null,
                            null,
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    final VMSXML vxml = getVMSXML(getRunningOnHost());
                    if (vxml == null || vmsVirtualDomainInfo == null) {
                        return "VM is not available";
                    }
                    final int remotePort = vxml.getRemotePort(
                                               vmsVirtualDomainInfo.getName());
                    if (remotePort <= 0) {
                        return "remote port is not greater than 0";
                    }
                    return null;
                }

                @Override
                public void action() {
                    hidePopup();
                    final VMSVirtualDomainInfo vvdi = vmsVirtualDomainInfo;
                    final VMSXML vxml = getVMSXML(getRunningOnHost());
                    if (vxml != null && vvdi != null) {
                        final int remotePort = vxml.getRemotePort(
                                                               vvdi.getName());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startTightVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(tightvncViewerMenu);
        }

        if (Tools.getApplication().isUltravnc()) {
            /* ultra vnc test menu */
            final UpdatableItem ultravncViewerMenu = new MyMenuItem(
                            "start ULTRA VNC viewer",
                            null,
                            null,
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    final VMSXML vxml = getVMSXML(getRunningOnHost());
                    if (vxml == null || vmsVirtualDomainInfo == null) {
                        return "VM is not available";
                    }
                    final int remotePort = vxml.getRemotePort(
                                               vmsVirtualDomainInfo.getName());
                    if (remotePort <= 0) {
                        return "remote port is not greater than 0";
                    }
                    return null;
                }

                @Override
                public void action() {
                    hidePopup();
                    final VMSVirtualDomainInfo vvdi = vmsVirtualDomainInfo;
                    final VMSXML vxml = getVMSXML(getRunningOnHost());
                    if (vxml != null && vvdi != null) {
                        final int remotePort = vxml.getRemotePort(
                                                           vvdi.getName());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startUltraVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(ultravncViewerMenu);
        }

        if (Tools.getApplication().isRealvnc()) {
            /* real vnc test menu */
            final UpdatableItem realvncViewerMenu = new MyMenuItem(
                            "start REAL VNC test",
                            null,
                            null,
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    final VMSXML vxml = getVMSXML(getRunningOnHost());
                    if (vxml == null || vmsVirtualDomainInfo == null) {
                        return "VM is not available";
                    }
                    final int remotePort = vxml.getRemotePort(
                                               vmsVirtualDomainInfo.getName());
                    if (remotePort <= 0) {
                        return "remote port is not greater than 0";
                    }
                    return null;
                }

                @Override
                public void action() {
                    hidePopup();
                    final VMSVirtualDomainInfo vvdi = vmsVirtualDomainInfo;
                    final VMSXML vxml = getVMSXML(getRunningOnHost());
                    if (vxml != null && vvdi != null) {
                        final int remotePort = vxml.getRemotePort(
                                                            vvdi.getName());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startRealVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(realvncViewerMenu);
        }
    }

    /** Returns the possible values for the pulldown menus, if applicable. */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        if (CONFIG_PARAM.equals(param)) {
            final Set<Value> configs = new TreeSet<Value>();
            for (final Host host : getBrowser().getClusterHosts()) {
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null) {
                    configs.addAll(vxml.getConfigs());
                }
            }
            return configs.toArray(new Value[configs.size()]);
        } else if (HYPERVISOR_PARAM.equals(param)) {
            return HYPERVISORS;
        } else {
            return super.getParamPossibleChoices(param);
        }
    }

    /**
     * Returns list of items for service popup menu with actions that can
     * be executed on the pacemaker services.
     */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = super.createPopup();
        addVncViewersToTheMenu(items);
        return items;
    }

    /** Returns a name of the service with virtual domain name. */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(30);
        s.append(getName());
        final String string;
        final String id = getService().getId();
        final Value configName = getParamSaved(CONFIG_PARAM);
        if (configName == null || configName.getValueForConfig() == null) {
            string = id;
        } else {
            final Matcher m = LIBVIRT_CONF_PATTERN.matcher(configName.getValueForConfig());
            if (m.matches()) {
                string = m.group(1);
            } else {
                string = id;
            }
        }
        if (string == null) {
            s.insert(0, "new ");
        } else {
            if (string != null && !string.isEmpty()) {
                s.append(" (");
                s.append(string);
                s.append(')');
            }
        }
        return s.toString();
    }

    /** Applies the changes to the service parameters. */
    @Override
	public void apply(final Host dcHost, final Application.RunMode runMode) {
        super.apply(dcHost, runMode);
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        return !PARAM_ALLOW_MIGRATE.equals(param) && super.isAdvanced(param);
    }
}
