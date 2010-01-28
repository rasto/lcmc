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
import drbd.data.ResourceAgent;
import drbd.utilities.UpdatableItem;
import drbd.utilities.MyMenuItem;
import drbd.utilities.Tools;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.tree.DefaultMutableTreeNode;
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
        addVirtualDomain();
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
        addVirtualDomain();
    }

    /**
     * Removes the service without confirmation dialog.
     */
    protected void removeMyselfNoConfirm(final Host dcHost,
                                         final boolean testOnly) {
        if (!testOnly) {
            removeVirtualDomain();
        }
        super.removeMyselfNoConfirm(dcHost, testOnly);
    }

    /**
     * Adds VirtualDomain panel in the VMs menu. */
    public final void addVirtualDomain() {
        getBrowser().addVMSNode();
        vmsVirtualDomainInfo = new VMSVirtualDomainInfo(this, getBrowser());
        final DefaultMutableTreeNode vmNode =
                             new DefaultMutableTreeNode(vmsVirtualDomainInfo);
        vmsVirtualDomainInfo.setNode(vmNode);
        final DefaultMutableTreeNode vmsNode = getBrowser().getVMSNode();
        vmsNode.add(vmNode);
        getBrowser().reload(vmsNode);
    }

    /**
     * Removes VirtualDomain panel from the VMS menu.
     */
    public final void removeVirtualDomain() {
        final VMSVirtualDomainInfo vdi = vmsVirtualDomainInfo;
        if (vdi != null) {
            final DefaultMutableTreeNode vmsNode = getBrowser().getVMSNode();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    vmsNode.remove(vdi.getNode());
                    getBrowser().reload(vmsNode);
                }
            });
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
                                                    null) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return !getService().isNew() && isRunning(testOnly);
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    final List<String> nodes = getRunningOnNodes(testOnly);
                    if (nodes != null && !nodes.isEmpty()) {
                        Tools.startTightVncViewer(
                         getBrowser().getCluster().getHostByName(nodes.get(0)),
                         getComboBoxValue("config"));
                    }
                }
            };
            registerMenuItem(tightvncViewerMenu);
            items.add(tightvncViewerMenu);
        }

        if (Tools.getConfigData().isUltravnc()) {
            /* ultra vnc test menu */
            final MyMenuItem ultravncViewerMenu = new MyMenuItem(
                                                    "start ULTRA VNC viewer",
                                                    null,
                                                    null) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return !getService().isNew() && isRunning(testOnly);
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    final List<String> nodes = getRunningOnNodes(testOnly);
                    if (nodes != null && !nodes.isEmpty()) {
                        Tools.startUltraVncViewer(
                         getBrowser().getCluster().getHostByName(nodes.get(0)),
                         getComboBoxValue("config"));
                    }
                }
            };
            registerMenuItem(ultravncViewerMenu);
            items.add(ultravncViewerMenu);
        }

        if (Tools.getConfigData().isRealvnc()) {
            /* real vnc test menu */
            final MyMenuItem realvncViewerMenu = new MyMenuItem(
                                                        "start REAL VNC test",
                                                        null,
                                                        null) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return !getService().isNew() && isRunning(testOnly);
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    final List<String> nodes = getRunningOnNodes(testOnly);
                    if (nodes != null && !nodes.isEmpty()) {
                        Tools.startRealVncViewer(
                         getBrowser().getCluster().getHostByName(nodes.get(0)),
                         getComboBoxValue("config"));
                    }
                }
            };
            registerMenuItem(realvncViewerMenu);
            items.add(realvncViewerMenu);
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
        getBrowser().reload(getBrowser().getVMSNode());
    }
}
