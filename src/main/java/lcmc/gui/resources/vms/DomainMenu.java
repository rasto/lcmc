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

package lcmc.gui.resources.vms;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import javax.swing.ImageIcon;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.model.vm.VmsXml;
import lcmc.model.resources.Resource;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.HostBrowser;
import lcmc.gui.resources.NetInfo;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class DomainMenu {
    /** Resume icon. */
    private static final ImageIcon RESUME_ICON = Tools.createImageIcon(
                                      Tools.getDefault("VMS.Resume.IconLarge"));
    /** Shutdown icon. */
    private static final ImageIcon SHUTDOWN_ICON = Tools.createImageIcon(
                                    Tools.getDefault("VMS.Shutdown.IconLarge"));
    /** Reboot icon. */
    private static final ImageIcon REBOOT_ICON = Tools.createImageIcon(
                                      Tools.getDefault("VMS.Reboot.IconLarge"));
    /** Destroy icon. */
    private static final ImageIcon DESTROY_ICON = Tools.createImageIcon(
                                    Tools.getDefault("VMS.Destroy.IconLarge"));
    private final DomainInfo domainInfo;

    public DomainMenu(DomainInfo domainInfo) {
        super();
        this.domainInfo = domainInfo;
    }


    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        /* vnc viewers */
        for (final Host h : getBrowser().getClusterHosts()) {
            addVncViewersToTheMenu(items, h);
        }

        /* start */
        for (final Host h : getBrowser().getClusterHosts()) {
            addStartMenu(items, h);
        }

        /* shutdown */
        for (final Host h : getBrowser().getClusterHosts()) {
            addShutdownMenu(items, h);
        }

        /* reboot */
        for (final Host h : getBrowser().getClusterHosts()) {
            addRebootMenu(items, h);
        }

        /* destroy */
        for (final Host h : getBrowser().getClusterHosts()) {
            addDestroyMenu(items, h);
        }

        /* resume */
        for (final Host h : getBrowser().getClusterHosts()) {
            addResumeMenu(items, h);
        }
        items.add(getAddNewHardwareMenu(
                      Tools.getString("DomainInfo.AddNewHardware")));

        /* advanced options */
        final MyMenu advancedSubmenu = new MyMenu(
                        Tools.getString("DomainInfo.MoreOptions"),
                        new AccessMode(Application.AccessType.OP, false),
                        new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public String enablePredicate() {
                return null;
            }
        };
        items.add(advancedSubmenu);

        /* suspend */
        for (final Host h : getBrowser().getClusterHosts()) {
            addSuspendMenu(advancedSubmenu, h);
        }

        /* resume */
        for (final Host h : getBrowser().getClusterHosts()) {
            addResumeAdvancedMenu(advancedSubmenu, h);
        }

        /* remove domain */
        final UpdatableItem removeMenuItem = new MyMenuItem(
                       Tools.getString("DomainInfo.RemoveDomain"),
                       ClusterBrowser.REMOVE_ICON,
                       Tools.getString("DomainInfo.RemoveDomain"),
                       Tools.getString("DomainInfo.CancelDomain"),
                       ClusterBrowser.REMOVE_ICON,
                       Tools.getString("DomainInfo.CancelDomain"),
                       new AccessMode(Application.AccessType.ADMIN, false),
                       new AccessMode(Application.AccessType.OP, false)) {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public boolean predicate() {
                                return !domainInfo.getResource().isNew();
                            }
                            @Override
                            public String enablePredicate() {
                                if (!Tools.getApplication().isAdvancedMode()
                                    && domainInfo.isUsedByCRM()) {
                                    return DomainInfo.IS_USED_BY_CRM_STRING;
                                }
                                for (final Host host
                                           : getBrowser().getClusterHosts()) {
                                    final VmsXml vmsXml =
                                                getBrowser().getVmsXml(host);
                                    if (vmsXml == null) {
                                        continue;
                                    }
                                    if (vmsXml.isRunning(domainInfo.getDomainName())) {
                                        return "it is running";
                                    }
                                }
                                return null;
                            }

                            @Override
                            public void action() {
                                domainInfo.hidePopup();
                                domainInfo.removeMyself(Application.RunMode.LIVE);
                            }
        };
        items.add(removeMenuItem);
        return items;
    }

    /** Add new hardware. */
    private UpdatableItem getAddNewHardwareMenu(final String name) {
        return new MyMenu(name,
                          new AccessMode(Application.AccessType.ADMIN, false),
                          new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void updateAndWait() {
                removeAll();
                final Point2D pos = getPos();
                /* disk */
                final MyMenuItem newDiskMenuItem = new MyMenuItem(
                   Tools.getString("DomainInfo.AddNewDisk"),
                   BlockDevInfo.HARDDISK_ICON_LARGE,
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        domainInfo.hidePopup();
                        domainInfo.addDiskPanel();
                    }
                };
                newDiskMenuItem.setPos(pos);
                add(newDiskMenuItem);

                /* fs */
                final MyMenuItem newFilesystemMenuItem = new MyMenuItem(
                   Tools.getString("DomainInfo.AddNewFilesystem"),
                   BlockDevInfo.HARDDISK_ICON_LARGE,
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        domainInfo.hidePopup();
                        domainInfo.addFilesystemPanel();
                    }
                };
                newFilesystemMenuItem.setPos(pos);
                add(newFilesystemMenuItem);

                /* interface */
                final MyMenuItem newInterfaceMenuItem = new MyMenuItem(
                   Tools.getString("DomainInfo.AddNewInterface"),
                   NetInfo.NET_INTERFACE_ICON_LARGE,
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        domainInfo.hidePopup();
                        domainInfo.addInterfacePanel();
                    }
                };
                newInterfaceMenuItem.setPos(pos);
                add(newInterfaceMenuItem);

                /* graphics */
                final MyMenuItem newGraphicsMenuItem = new MyMenuItem(
                   Tools.getString("DomainInfo.AddNewGraphics"),
                   DomainInfo.VNC_ICON,
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        domainInfo.hidePopup();
                        domainInfo.addGraphicsPanel();
                    }
                };
                newGraphicsMenuItem.setPos(pos); add(newGraphicsMenuItem);


                /* input dev */
                final MyMenuItem newInputDevMenuItem = new MyMenuItem(
                   Tools.getString("DomainInfo.AddNewInputDev"),
                   null,
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        domainInfo.hidePopup();
                        domainInfo.addInputDevPanel();
                    }
                };
                newInputDevMenuItem.setPos(pos);
                add(newInputDevMenuItem);

                /* sounds */
                final MyMenuItem newSoundsMenuItem = new MyMenuItem(
                   Tools.getString("DomainInfo.AddNewSound"),
                   null,
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        domainInfo.hidePopup();
                        domainInfo.addSoundsPanel();
                    }
                };
                newSoundsMenuItem.setPos(pos);
                add(newSoundsMenuItem);

                /* serials */
                final MyMenuItem newSerialsMenuItem = new MyMenuItem(
                   Tools.getString("DomainInfo.AddNewSerial"),
                   null,
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        domainInfo.hidePopup();
                        domainInfo.addSerialsPanel();
                    }
                };
                newSerialsMenuItem.setPos(pos);
                add(newSerialsMenuItem);

                /* parallels */
                final MyMenuItem newParallelsMenuItem = new MyMenuItem(
                   Tools.getString("DomainInfo.AddNewParallel"),
                   null,
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        domainInfo.hidePopup();
                        domainInfo.addParallelsPanel();
                    }
                };
                newParallelsMenuItem.setPos(pos);
                add(newParallelsMenuItem);

                /* videos */
                final MyMenuItem newVideosMenuItem = new MyMenuItem(
                   Tools.getString("DomainInfo.AddNewVideo"),
                   null,
                   new AccessMode(Application.AccessType.ADMIN, true),
                   new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        domainInfo.hidePopup();
                        domainInfo.addVideosPanel();
                    }
                };
                newVideosMenuItem.setPos(pos);
                add(newVideosMenuItem);
                super.updateAndWait();
            }
        };
    }

    /** Adds vm domain start menu item. */
    private void addStartMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem startMenuItem = new MyMenuItem(
                            Tools.getString("DomainInfo.StartOn")
                            + host.getName(),
                            HostBrowser.HOST_ON_ICON_LARGE,
                            Tools.getString("DomainInfo.StartOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                final VmsXml vmsXml = getBrowser().getVmsXml(host);
                return vmsXml != null
                       && vmsXml.getDomainNames().contains(domainInfo.getDomainName())
                       && !vmsXml.isRunning(domainInfo.getDomainName());
            }

            @Override
            public String enablePredicate() {
                if (!Tools.getApplication().isAdvancedMode() && domainInfo.isUsedByCRM()) {
                    return DomainInfo.IS_USED_BY_CRM_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                domainInfo.hidePopup();
                final VmsXml vxml = getBrowser().getVmsXml(host);
                if (vxml != null && host != null) {
                    domainInfo.start(host);
                }
            }
        };
        items.add(startMenuItem);
    }

    /** Adds vm domain shutdown menu item. */
    void addShutdownMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem shutdownMenuItem = new MyMenuItem(
                            Tools.getString("DomainInfo.ShutdownOn")
                            + host.getName(),
                            SHUTDOWN_ICON,
                            Tools.getString("DomainInfo.ShutdownOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                final VmsXml vmsXml = getBrowser().getVmsXml(host);
                return vmsXml != null
                       && vmsXml.getDomainNames().contains(domainInfo.getDomainName())
                       && vmsXml.isRunning(domainInfo.getDomainName());
            }

            @Override
            public String enablePredicate() {
                if (!Tools.getApplication().isAdvancedMode() && domainInfo.isUsedByCRM()) {
                    return DomainInfo.IS_USED_BY_CRM_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                domainInfo.hidePopup();
                final VmsXml vxml = getBrowser().getVmsXml(host);
                if (vxml != null && host != null) {
                    domainInfo.shutdown(host);
                }
            }
        };
        items.add(shutdownMenuItem);
    }

    /** Adds vm domain reboot menu item. */
    void addRebootMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem rebootMenuItem = new MyMenuItem(
                            Tools.getString("DomainInfo.RebootOn")
                            + host.getName(),
                            REBOOT_ICON,
                            Tools.getString("DomainInfo.RebootOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                final VmsXml vmsXml = getBrowser().getVmsXml(host);
                return vmsXml != null
                       && vmsXml.getDomainNames().contains(domainInfo.getDomainName())
                       && vmsXml.isRunning(domainInfo.getDomainName());
            }

            @Override
            public String enablePredicate() {
                if (!Tools.getApplication().isAdvancedMode() && domainInfo.isUsedByCRM()) {
                    return DomainInfo.IS_USED_BY_CRM_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                domainInfo.hidePopup();
                final VmsXml vxml = getBrowser().getVmsXml(host);
                if (vxml != null && host != null) {
                    domainInfo.reboot(host);
                }
            }
        };
        items.add(rebootMenuItem);
    }

    /** Adds vm domain resume menu item. */
    private void addResumeMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem resumeMenuItem = new MyMenuItem(
                            Tools.getString("DomainInfo.ResumeOn")
                            + host.getName(),
                            RESUME_ICON,
                            Tools.getString("DomainInfo.ResumeOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                final VmsXml vmsXml = getBrowser().getVmsXml(host);
                return vmsXml != null
                       && vmsXml.getDomainNames().contains(domainInfo.getDomainName())
                       && vmsXml.isSuspended(domainInfo.getDomainName());
            }

            @Override
            public String enablePredicate() {
                if (getResource().isNew()) {
                    return DomainInfo.NOT_APPLIED;
                }
                final VmsXml vmsXml = getBrowser().getVmsXml(host);
                if (vmsXml == null) {
                    return DomainInfo.NO_VM_STATUS_STRING;
                }
                if (!vmsXml.isSuspended(domainInfo.getDomainName())) {
                    return "it is not suspended";
                }
                return null;
                //return vmsxml != null
                //       && vmsxml.isSuspended(domainInfo.getDomainName());
            }

            @Override
            public void action() {
                domainInfo.hidePopup();
                final VmsXml vxml = getBrowser().getVmsXml(host);
                if (vxml != null && host != null) {
                    domainInfo.resume(host);
                }
            }
        };
        items.add(resumeMenuItem);
    }


    /** Adds vm domain destroy menu item. */
    void addDestroyMenu(final Collection<UpdatableItem> items,
                        final Host host) {
        final UpdatableItem destroyMenuItem = new MyMenuItem(
                            Tools.getString("DomainInfo.DestroyOn")
                            + host.getName(),
                            DESTROY_ICON,
                            Tools.getString("DomainInfo.DestroyOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;


            @Override
            public String enablePredicate() {
                if (!Tools.getApplication().isAdvancedMode() && domainInfo.isUsedByCRM()) {
                    return DomainInfo.IS_USED_BY_CRM_STRING;
                }
                if (getResource().isNew()) {
                    return DomainInfo.NOT_APPLIED;
                }
                final VmsXml vmsXml = getBrowser().getVmsXml(host);
                if (vmsXml == null
                    || !vmsXml.getDomainNames().contains(domainInfo.getDomainName())) {
                    return DomainInfo.NO_VM_STATUS_STRING;
                }
                if (!vmsXml.isRunning(domainInfo.getDomainName())) {
                    return "not running";
                }
                return null;
            }

            @Override
            public void action() {
                domainInfo.hidePopup();
                final VmsXml vxml = getBrowser().getVmsXml(host);
                if (vxml != null && host != null) {
                    domainInfo.destroy(host);
                }
            }
        };
        items.add(destroyMenuItem);
    }

    /** Adds vm domain suspend menu item. */
    private void addSuspendMenu(final MyMenu advancedSubmenu, final Host host) {
        final MyMenuItem suspendMenuItem = new MyMenuItem(
                            Tools.getString("DomainInfo.SuspendOn")
                            + host.getName(),
                            DomainInfo.PAUSE_ICON,
                            Tools.getString("DomainInfo.SuspendOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getResource().isNew()) {
                    return DomainInfo.NOT_APPLIED;
                }
                final VmsXml vmsXml = getBrowser().getVmsXml(host);
                if (vmsXml == null
                    || !vmsXml.getDomainNames().contains(domainInfo.getDomainName())) {
                    return DomainInfo.NO_VM_STATUS_STRING;
                }
                if (!vmsXml.isRunning(domainInfo.getDomainName())) {
                    return "not running";
                }
                if (vmsXml.isSuspended(domainInfo.getDomainName())) {
                    return "it is already suspended";
                }
                return null;
            }

            @Override
            public void action() {
                domainInfo.hidePopup();
                final VmsXml vxml = getBrowser().getVmsXml(host);
                if (vxml != null && host != null) {
                    domainInfo.suspend(host);
                }
            }
        };
        advancedSubmenu.add(suspendMenuItem);
    }

    /** Adds vm domain resume menu item. */
    void addResumeAdvancedMenu(final MyMenu advancedSubmenu, final Host host) {
        final MyMenuItem resumeMenuItem = new MyMenuItem(
                            Tools.getString("DomainInfo.ResumeOn")
                            + host.getName(),
                            RESUME_ICON,
                            Tools.getString("DomainInfo.ResumeOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getResource().isNew()) {
                    return DomainInfo.NOT_APPLIED;
                }
                final VmsXml vmsXml = getBrowser().getVmsXml(host);
                if (vmsXml == null
                    || !vmsXml.getDomainNames().contains(domainInfo.getDomainName())) {
                    return DomainInfo.NO_VM_STATUS_STRING;
                }
                if (!vmsXml.isRunning(domainInfo.getDomainName())) {
                    return "not running";
                }
                if (!vmsXml.isSuspended(domainInfo.getDomainName())) {
                    return "it is not suspended";
                }
                return null;
            }

            @Override
            public void action() {
                domainInfo.hidePopup();
                final VmsXml vxml = getBrowser().getVmsXml(host);
                if (vxml != null && host != null) {
                    domainInfo.resume(host);
                }
            }
        };
        advancedSubmenu.add(resumeMenuItem);
    }

    /** Adds vnc viewer menu items. */
    private void addVncViewersToTheMenu(final Collection<UpdatableItem> items,
                                        final Host host) {
        if (Tools.getApplication().isUseTightvnc()) {
            /* tight vnc test menu */
            final UpdatableItem tightvncViewerMenu = new MyMenuItem(
                            getVNCMenuString("TIGHT", host),
                            DomainInfo.VNC_ICON,
                            getVNCMenuString("TIGHT", host),
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getResource().isNew()) {
                        return DomainInfo.NOT_APPLIED;
                    }
                    final VmsXml vmsXml = getBrowser().getVmsXml(host);
                    if (vmsXml == null) {
                        return DomainInfo.NO_VM_STATUS_STRING;
                    }
                    if (!vmsXml.isRunning(domainInfo.getDomainName())) {
                        return "not running";
                    }
                    return null;
                }

                @Override
                public void action() {
                    domainInfo.hidePopup();
                    final VmsXml vxml = getBrowser().getVmsXml(host);
                    if (vxml != null) {
                        final int remotePort = vxml.getRemotePort(
                                                            domainInfo.getDomainName());
                        final Host host = vxml.getDefinedOnHost();
                        if (host != null && remotePort > 0) {
                            Tools.startTightVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(tightvncViewerMenu);
        }

        if (Tools.getApplication().isUseUltravnc()) {
            /* ultra vnc test menu */
            final UpdatableItem ultravncViewerMenu = new MyMenuItem(
                            getVNCMenuString("ULTRA", host),
                            DomainInfo.VNC_ICON,
                            getVNCMenuString("ULTRA", host),
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getResource().isNew()) {
                        return DomainInfo.NOT_APPLIED;
                    }
                    final VmsXml vmsXml = getBrowser().getVmsXml(host);
                    if (vmsXml == null) {
                        return DomainInfo.NO_VM_STATUS_STRING;
                    }
                    if (!vmsXml.isRunning(domainInfo.getDomainName())) {
                        return "not running";
                    }
                    return null;
                }

                @Override
                public void action() {
                    domainInfo.hidePopup();
                    final VmsXml vxml = getBrowser().getVmsXml(host);
                    if (vxml != null) {
                        final int remotePort = vxml.getRemotePort(
                                                             domainInfo.getDomainName());
                        final Host host = vxml.getDefinedOnHost();
                        if (host != null && remotePort > 0) {
                            Tools.startUltraVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(ultravncViewerMenu);
        }

        if (Tools.getApplication().isUseRealvnc()) {
            /* real vnc test menu */
            final UpdatableItem realvncViewerMenu = new MyMenuItem(
                            getVNCMenuString("REAL", host),
                            DomainInfo.VNC_ICON,
                            getVNCMenuString("REAL", host),
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getResource().isNew()) {
                        return DomainInfo.NOT_APPLIED;
                    }
                    final VmsXml vmsXml = getBrowser().getVmsXml(host);
                    if (vmsXml == null) {
                        return DomainInfo.NO_VM_STATUS_STRING;
                    }
                    if (!vmsXml.isRunning(domainInfo.getDomainName())) {
                        return "not running";
                    }
                    return null;
                }

                @Override
                public void action() {
                    domainInfo.hidePopup();
                    final VmsXml vxml = getBrowser().getVmsXml(host);
                    if (vxml != null) {
                        final int remotePort = vxml.getRemotePort(
                                                            domainInfo.getDomainName());
                        final Host host = vxml.getDefinedOnHost();
                        if (host != null && remotePort > 0) {
                            Tools.startRealVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(realvncViewerMenu);
        }
    }

    private ClusterBrowser getBrowser() {
        return domainInfo.getBrowser();
    }

    private Resource getResource() {
        return domainInfo.getResource();
    }

    /** Returns menu item for VNC different viewers. */
    private String getVNCMenuString(final String viewer, final Host host) {
        return Tools.getString("DomainInfo.StartVNCViewerOn")
                            .replaceAll("@VIEWER@",
                                        Matcher.quoteReplacement(viewer))
               + host.getName();
    }
}