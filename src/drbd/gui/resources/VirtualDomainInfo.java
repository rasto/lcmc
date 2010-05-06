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
import drbd.data.Host;
import drbd.data.VMSXML;
import drbd.data.ResourceAgent;
import drbd.data.ConfigData;
import drbd.utilities.UpdatableItem;
import drbd.utilities.MyMenuItem;
import drbd.utilities.Tools;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.SwingUtilities;

/**
 * This class holds info about VirtualDomain service in the cluster menu.
 */
class VirtualDomainInfo extends ServiceInfo {
    /** VirtualDomain in the VMs menu. */
    private VMSVirtualDomainInfo vmsVirtualDomainInfo = null;
    /** pattern that captures a name from xml file name. */
    public static final Pattern LIBVIRT_CONF_PATTERN =
                                            Pattern.compile(".*?([^/]+).xml$");

    /**
     * Creates the VirtualDomainInfo object.
     */
    public VirtualDomainInfo(final String name,
                             final ResourceAgent ra,
                             final Browser browser) {
        super(name, ra, browser);
    }

    /**
     * Returns the first on which this vm is running.
     */
    private Host getRunningOnHost() {
        final List<String> nodes = getRunningOnNodes(false);
        if (nodes != null
            && !nodes.isEmpty()) {
            final Host host = getBrowser().getCluster().getHostByName(
                                                                nodes.get(0));
            return host;
        }
        return null;
    }

    /**
     * Returns object with vm data.
     */
    public final VMSXML getVMSXML(final Host host) {
        return getBrowser().getVMSXML(host);
    }

    /**
     * Creates the VirtualDomainInfo object.
     */
    public VirtualDomainInfo(final String name,
                             final ResourceAgent ra,
                             final String hbId,
                             final Map<String, String> resourceNode,
                             final Browser browser) {
        super(name, ra, hbId, resourceNode, browser);
    }

    /**
     * Removes the service without confirmation dialog.
     */
    protected final void removeMyselfNoConfirm(final Host dcHost,
                                         final boolean testOnly) {
        super.removeMyselfNoConfirm(dcHost, testOnly);
    }

    /**
     * Sets service parameters with values from resourceNode hash.
     */
    public final void setParameters(final Map<String, String> resourceNode) {
        super.setParameters(resourceNode);
        connectWithVMS();
    }

    /**
     * Connects with VMSVirtualDomainInfo object.
     */
    public final void connectWithVMS() {
        final String config = getParamSaved("config");
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                final String name = vxml.getNameFromConfig(config);
                vmsVirtualDomainInfo = getBrowser().findVMSVirtualDomainInfo(
                                                                         name);
                break;
            }
        }
    }

    /**
     * Adds vnc viewer menu items.
     */
    public final void addVncViewersToTheMenu(final List<UpdatableItem> items) {
        final boolean testOnly = false;
        if (Tools.getConfigData().isTightvnc()) {
            /* tight vnc test menu */
            final MyMenuItem tightvncViewerMenu = new MyMenuItem(
                                                    "start TIGHT VNC viewer",
                                                    null,
                                                    null,
                                                    ConfigData.AccessType.RO,
                                                    ConfigData.AccessType.RO) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    final VMSXML vxml = getVMSXML(getRunningOnHost());
                    if (vxml == null || vmsVirtualDomainInfo == null) {
                        return false;
                    }
                    final int remotePort = vxml.getRemotePort(
                                               vmsVirtualDomainInfo.getName());
                    return remotePort > 0;
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
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

        if (Tools.getConfigData().isUltravnc()) {
            /* ultra vnc test menu */
            final MyMenuItem ultravncViewerMenu = new MyMenuItem(
                                                    "start ULTRA VNC viewer",
                                                    null,
                                                    null,
                                                    ConfigData.AccessType.RO,
                                                    ConfigData.AccessType.RO) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    final VMSXML vxml = getVMSXML(getRunningOnHost());
                    if (vxml == null || vmsVirtualDomainInfo == null) {
                        return false;
                    }
                    final int remotePort = vxml.getRemotePort(
                                               vmsVirtualDomainInfo.getName());
                    return remotePort > 0;
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
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

        if (Tools.getConfigData().isRealvnc()) {
            /* real vnc test menu */
            final MyMenuItem realvncViewerMenu = new MyMenuItem(
                                                    "start REAL VNC test",
                                                    null,
                                                    null,
                                                    ConfigData.AccessType.RO,
                                                    ConfigData.AccessType.RO) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    final VMSXML vxml = getVMSXML(getRunningOnHost());
                    if (vxml == null || vmsVirtualDomainInfo == null) {
                        return false;
                    }
                    final int remotePort = vxml.getRemotePort(
                                               vmsVirtualDomainInfo.getName());
                    return remotePort > 0;
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
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

    /**
     * Returns the possible values for the pulldown menus, if applicable.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        if ("config".equals(param)) {
            final Set<String> configs = new TreeSet<String>();
            for (final Host host : getBrowser().getClusterHosts()) {
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null) {
                    configs.addAll(vxml.getConfigs());
                }
            }
            return configs.toArray(new String[configs.size()]);
        } else {
            return super.getParamPossibleChoices(param);
        }
    }

    /**
     * Returns list of items for service popup menu with actions that can
     * be executed on the pacemaker services.
     */
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = super.createPopup();
        addVncViewersToTheMenu(items);
        return items;
    }

    /**
     * Returns a name of the service with virtual domain name.
     */
    public String toString() {
        final StringBuffer s = new StringBuffer(30);
        s.append(getName());
        final String string;
        final String id = getService().getId();
        final String configName = getParamSaved("config");
        if (configName != null) {
            final Matcher m = LIBVIRT_CONF_PATTERN.matcher(configName);
            if (m.matches()) {
                string = m.group(1);
            } else {
                string = id;
            }
        } else {
            string = id;
        }
        if (string == null) {
            s.insert(0, "new ");
        } else {
            if (!"".equals(string)) {
                s.append(" (");
                s.append(string);
                s.append(')');
            }
        }
        return s.toString();
    }

    /**
     * Applies the changes to the service parameters.
     */
    public void apply(final Host dcHost, final boolean testOnly) {
        super.apply(dcHost, testOnly);
    }
}
