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

package lcmc.vm.ui.resource;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.ImageIcon;

import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.vm.domain.VmsXml;
import lcmc.common.domain.Resource;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.host.ui.HostBrowser;
import lcmc.cluster.ui.resource.NetInfo;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.common.domain.EnablePredicate;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.domain.Predicate;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.common.domain.VisiblePredicate;

@Named
public class DomainMenu {
    private static final ImageIcon RESUME_ICON = Tools.createImageIcon(Tools.getDefault("VMS.Resume.IconLarge"));
    private static final ImageIcon SHUTDOWN_ICON = Tools.createImageIcon(Tools.getDefault("VMS.Shutdown.IconLarge"));
    private static final ImageIcon REBOOT_ICON = Tools.createImageIcon(Tools.getDefault("VMS.Reboot.IconLarge"));
    private static final ImageIcon DESTROY_ICON = Tools.createImageIcon(Tools.getDefault("VMS.Destroy.IconLarge"));
    private DomainInfo domainInfo;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Application application;

    public List<UpdatableItem> getPulldownMenu(final DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
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
        final MyMenu advancedSubmenu = menuFactory.createMenu(
                Tools.getString("DomainInfo.MoreOptions"),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        return null;
                    }
                });
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
        final UpdatableItem removeMenuItem = menuFactory.createMenuItem(
                Tools.getString("DomainInfo.RemoveDomain"),
                ClusterBrowser.REMOVE_ICON,
                Tools.getString("DomainInfo.RemoveDomain"),

                Tools.getString("DomainInfo.CancelDomain"),
                ClusterBrowser.REMOVE_ICON,
                Tools.getString("DomainInfo.CancelDomain"),

                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .predicate(new Predicate() {
                    @Override
                    public boolean check() {
                        return !domainInfo.getResource().isNew();
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!application.isAdvancedMode() && domainInfo.isUsedByCRM()) {
                            return DomainInfo.IS_USED_BY_CRM_STRING;
                        }
                        for (final Host host : getBrowser().getClusterHosts()) {
                            final VmsXml vmsXml = getBrowser().getVmsXml(host);
                            if (vmsXml == null) {
                                continue;
                            }
                            if (vmsXml.isRunning(domainInfo.getDomainName())) {
                                return "it is running";
                            }
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        domainInfo.hidePopup();
                        domainInfo.removeMyself(Application.RunMode.LIVE);
                    }
                });
        items.add(removeMenuItem);
        return items;
    }

    /**
     * Add new hardware.
     */
    private UpdatableItem getAddNewHardwareMenu(final String name) {
        final MyMenu newHardwareMenu = menuFactory.createMenu(
                name,
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL));
        newHardwareMenu.onUpdate(new Runnable() {
            @Override
            public void run() {
                newHardwareMenu.removeAll();
                final Point2D pos = newHardwareMenu.getPos();
                /* disk */
                final MyMenuItem newDiskMenuItem = menuFactory.createMenuItem(
                        Tools.getString("DomainInfo.AddNewDisk"),
                        BlockDevInfo.HARDDISK_ICON_LARGE,
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                domainInfo.hidePopup();
                                domainInfo.addDiskPanel();
                            }
                        });
                newDiskMenuItem.setPos(pos);
                newHardwareMenu.add(newDiskMenuItem);

                /* fs */
                final MyMenuItem newFilesystemMenuItem = menuFactory.createMenuItem(
                        Tools.getString("DomainInfo.AddNewFilesystem"),
                        BlockDevInfo.HARDDISK_ICON_LARGE,
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                domainInfo.hidePopup();
                                domainInfo.addFilesystemPanel();
                            }
                        });
                newFilesystemMenuItem.setPos(pos);
                newHardwareMenu.add(newFilesystemMenuItem);

                /* interface */
                final MyMenuItem newInterfaceMenuItem = menuFactory.createMenuItem(
                        Tools.getString("DomainInfo.AddNewInterface"),
                        NetInfo.NET_INTERFACE_ICON_LARGE,
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                domainInfo.hidePopup();
                                domainInfo.addInterfacePanel();
                            }
                        });
                newInterfaceMenuItem.setPos(pos);
                newHardwareMenu.add(newInterfaceMenuItem);

                /* graphics */
                final MyMenuItem newGraphicsMenuItem = menuFactory.createMenuItem(
                        Tools.getString("DomainInfo.AddNewGraphics"),
                        DomainInfo.VNC_ICON,
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                domainInfo.hidePopup();
                                domainInfo.addGraphicsPanel();
                            }
                        });
                newGraphicsMenuItem.setPos(pos);
                newHardwareMenu.add(newGraphicsMenuItem);

                /* input dev */
                final MyMenuItem newInputDevMenuItem = menuFactory.createMenuItem(
                        Tools.getString("DomainInfo.AddNewInputDev"),
                        null,
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                domainInfo.hidePopup();
                                domainInfo.addInputDevPanel();
                            }
                        });
                newInputDevMenuItem.setPos(pos);
                newHardwareMenu.add(newInputDevMenuItem);

                /* sounds */
                final MyMenuItem newSoundsMenuItem = menuFactory.createMenuItem(
                        Tools.getString("DomainInfo.AddNewSound"),
                        null,
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                domainInfo.hidePopup();
                                domainInfo.addSoundsPanel();
                            }
                        });
                newSoundsMenuItem.setPos(pos);
                newHardwareMenu.add(newSoundsMenuItem);

                /* serials */
                final MyMenuItem newSerialsMenuItem = menuFactory.createMenuItem(
                        Tools.getString("DomainInfo.AddNewSerial"),
                        null,
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                domainInfo.hidePopup();
                                domainInfo.addSerialsPanel();
                            }
                        });
                newSerialsMenuItem.setPos(pos);
                newHardwareMenu.add(newSerialsMenuItem);

                /* parallels */
                final MyMenuItem newParallelsMenuItem = menuFactory.createMenuItem(
                        Tools.getString("DomainInfo.AddNewParallel"),
                        null,
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                domainInfo.hidePopup();
                                domainInfo.addParallelsPanel();
                            }
                        });
                newParallelsMenuItem.setPos(pos);
                newHardwareMenu.add(newParallelsMenuItem);

                /* videos */
                final MyMenuItem newVideosMenuItem = menuFactory.createMenuItem(
                        Tools.getString("DomainInfo.AddNewVideo"),
                        null,
                        new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                domainInfo.hidePopup();
                                domainInfo.addVideosPanel();
                            }
                        });
                newVideosMenuItem.setPos(pos);
                newHardwareMenu.add(newVideosMenuItem);
                newHardwareMenu.updateMenuComponents();
                newHardwareMenu.processAccessMode();
            }
        });
        return newHardwareMenu;
    }

    /**
     * Adds vm domain start menu item.
     */
    private void addStartMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem startMenuItem = menuFactory.createMenuItem(
                Tools.getString("DomainInfo.StartOn") + host.getName(),
                HostBrowser.HOST_ON_ICON_LARGE,
                Tools.getString("DomainInfo.StartOn") + host.getName(),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        final VmsXml vmsXml = getBrowser().getVmsXml(host);
                        return vmsXml != null
                                && vmsXml.getDomainNames().contains(domainInfo.getDomainName())
                                && !vmsXml.isRunning(domainInfo.getDomainName());
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!application.isAdvancedMode() && domainInfo.isUsedByCRM()) {
                            return DomainInfo.IS_USED_BY_CRM_STRING;
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        domainInfo.hidePopup();
                        final VmsXml vxml = getBrowser().getVmsXml(host);
                        if (vxml != null && host != null) {
                            domainInfo.start(host);
                        }
                    }
                });
        items.add(startMenuItem);
    }

    /**
     * Adds vm domain shutdown menu item.
     */
    void addShutdownMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem shutdownMenuItem = menuFactory.createMenuItem(
                Tools.getString("DomainInfo.ShutdownOn") + host.getName(),
                SHUTDOWN_ICON,
                Tools.getString("DomainInfo.ShutdownOn") + host.getName(),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        final VmsXml vmsXml = getBrowser().getVmsXml(host);
                        return vmsXml != null
                               && vmsXml.getDomainNames().contains(domainInfo.getDomainName())
                               && vmsXml.isRunning(domainInfo.getDomainName());
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!application.isAdvancedMode() && domainInfo.isUsedByCRM()) {
                            return DomainInfo.IS_USED_BY_CRM_STRING;
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        domainInfo.hidePopup();
                        final VmsXml vxml = getBrowser().getVmsXml(host);
                        if (vxml != null && host != null) {
                            domainInfo.shutdown(host);
                        }
                    }
                });
        items.add(shutdownMenuItem);
    }

    /**
     * Adds vm domain reboot menu item.
     */
    void addRebootMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem rebootMenuItem = menuFactory.createMenuItem(
                Tools.getString("DomainInfo.RebootOn") + host.getName(),
                REBOOT_ICON,
                Tools.getString("DomainInfo.RebootOn") + host.getName(),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        final VmsXml vmsXml = getBrowser().getVmsXml(host);
                        return vmsXml != null
                               && vmsXml.getDomainNames().contains(domainInfo.getDomainName())
                               && vmsXml.isRunning(domainInfo.getDomainName());
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!application.isAdvancedMode() && domainInfo.isUsedByCRM()) {
                            return DomainInfo.IS_USED_BY_CRM_STRING;
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        domainInfo.hidePopup();
                        final VmsXml vxml = getBrowser().getVmsXml(host);
                        if (vxml != null && host != null) {
                            domainInfo.reboot(host);
                        }
                    }
                });
        items.add(rebootMenuItem);
    }

    /**
     * Adds vm domain resume menu item.
     */
    private void addResumeMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem resumeMenuItem = menuFactory.createMenuItem(
                Tools.getString("DomainInfo.ResumeOn") + host.getName(),
                RESUME_ICON,
                Tools.getString("DomainInfo.ResumeOn") + host.getName(),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        final VmsXml vmsXml = getBrowser().getVmsXml(host);
                        return vmsXml != null
                               && vmsXml.getDomainNames().contains(domainInfo.getDomainName())
                               && vmsXml.isSuspended(domainInfo.getDomainName());
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
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
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        domainInfo.hidePopup();
                        final VmsXml vxml = getBrowser().getVmsXml(host);
                        if (vxml != null && host != null) {
                            domainInfo.resume(host);
                        }
                    }
                });
        items.add(resumeMenuItem);
    }

    /**
     * Adds vm domain destroy menu item.
     */
    void addDestroyMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem destroyMenuItem = menuFactory.createMenuItem(
                Tools.getString("DomainInfo.DestroyOn") + host.getName(),
                DESTROY_ICON,
                Tools.getString("DomainInfo.DestroyOn") + host.getName(),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!application.isAdvancedMode() && domainInfo.isUsedByCRM()) {
                            return DomainInfo.IS_USED_BY_CRM_STRING;
                        }
                        if (getResource().isNew()) {
                            return DomainInfo.NOT_APPLIED;
                        }
                        final VmsXml vmsXml = getBrowser().getVmsXml(host);
                        if (vmsXml == null || !vmsXml.getDomainNames().contains(domainInfo.getDomainName())) {
                            return DomainInfo.NO_VM_STATUS_STRING;
                        }
                        if (!vmsXml.isRunning(domainInfo.getDomainName())) {
                            return "not running";
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        domainInfo.hidePopup();
                        final VmsXml vxml = getBrowser().getVmsXml(host);
                        if (vxml != null && host != null) {
                            domainInfo.destroy(host);
                        }
                    }
                });
        items.add(destroyMenuItem);
    }

    /**
     * Adds vm domain suspend menu item.
     */
    private void addSuspendMenu(final MyMenu advancedSubmenu, final Host host) {
        final MyMenuItem suspendMenuItem = menuFactory.createMenuItem(
                Tools.getString("DomainInfo.SuspendOn") + host.getName(),
                DomainInfo.PAUSE_ICON,
                Tools.getString("DomainInfo.SuspendOn") + host.getName(),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (getResource().isNew()) {
                            return DomainInfo.NOT_APPLIED;
                        }
                        final VmsXml vmsXml = getBrowser().getVmsXml(host);
                        if (vmsXml == null || !vmsXml.getDomainNames().contains(domainInfo.getDomainName())) {
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
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        domainInfo.hidePopup();
                        final VmsXml vxml = getBrowser().getVmsXml(host);
                        if (vxml != null && host != null) {
                            domainInfo.suspend(host);
                        }
                    }
                });
        advancedSubmenu.add(suspendMenuItem);
    }

    /**
     * Adds vm domain resume menu item.
     */
    void addResumeAdvancedMenu(final MyMenu advancedSubmenu, final Host host) {
        final MyMenuItem resumeMenuItem = menuFactory.createMenuItem(
                Tools.getString("DomainInfo.ResumeOn") + host.getName(),
                RESUME_ICON,
                Tools.getString("DomainInfo.ResumeOn") + host.getName(),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (getResource().isNew()) {
                            return DomainInfo.NOT_APPLIED;
                        }
                        final VmsXml vmsXml = getBrowser().getVmsXml(host);
                        if (vmsXml == null || !vmsXml.getDomainNames().contains(domainInfo.getDomainName())) {
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
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        domainInfo.hidePopup();
                        final VmsXml vxml = getBrowser().getVmsXml(host);
                        if (vxml != null && host != null) {
                            domainInfo.resume(host);
                        }
                    }
                });
        advancedSubmenu.add(resumeMenuItem);
    }

    private void addVncViewersToTheMenu(final Collection<UpdatableItem> items, final Host host) {
        if (application.isUseTightvnc()) {
            /* tight vnc test menu */
            final UpdatableItem tightvncViewerMenu = menuFactory.createMenuItem(
                    getVNCMenuString("TIGHT", host),
                    DomainInfo.VNC_ICON,
                    getVNCMenuString("TIGHT", host),
                    new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                    new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                    .enablePredicate(new EnablePredicate() {
                        @Override
                        public String check() {
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
                    })
                    .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                            domainInfo.hidePopup();
                            final VmsXml vxml = getBrowser().getVmsXml(host);
                            if (vxml != null) {
                                final int remotePort = vxml.getRemotePort(domainInfo.getDomainName());
                                final Host host = vxml.getDefinedOnHost();
                                if (host != null && remotePort > 0) {
                                    application.startTightVncViewer(host, remotePort);
                                }
                            }
                        }
                    });
            items.add(tightvncViewerMenu);
        }

        if (application.isUseUltravnc()) {
            final UpdatableItem ultravncViewerMenu = menuFactory.createMenuItem(
                    getVNCMenuString("ULTRA", host),
                    DomainInfo.VNC_ICON,
                    getVNCMenuString("ULTRA", host),
                    new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                    new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                    .enablePredicate(new EnablePredicate() {
                        @Override
                        public String check() {
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
                    })
                    .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                            domainInfo.hidePopup();
                            final VmsXml vxml = getBrowser().getVmsXml(host);
                            if (vxml != null) {
                                final int remotePort = vxml.getRemotePort(domainInfo.getDomainName());
                                final Host host = vxml.getDefinedOnHost();
                                if (host != null && remotePort > 0) {
                                    application.startUltraVncViewer(host, remotePort);
                                }
                            }
                        }
                    });
            items.add(ultravncViewerMenu);
        }

        if (application.isUseRealvnc()) {
            final UpdatableItem realvncViewerMenu = menuFactory.createMenuItem(
                    getVNCMenuString("REAL", host),
                    DomainInfo.VNC_ICON,
                    getVNCMenuString("REAL", host),
                    new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                    new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                    .enablePredicate(new EnablePredicate() {
                        @Override
                        public String check() {
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
                    })
                    .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                            domainInfo.hidePopup();
                            final VmsXml vxml = getBrowser().getVmsXml(host);
                            if (vxml != null) {
                                final int remotePort = vxml.getRemotePort(domainInfo.getDomainName());
                                final Host host = vxml.getDefinedOnHost();
                                if (host != null && remotePort > 0) {
                                    application.startRealVncViewer(host, remotePort);
                                }
                            }
                        }
                    });
            items.add(realvncViewerMenu);
        }
    }

    private ClusterBrowser getBrowser() {
        return domainInfo.getBrowser();
    }

    private Resource getResource() {
        return domainInfo.getResource();
    }

    /**
     * Returns menu item for VNC different viewers.
     */
    private String getVNCMenuString(final String viewer, final Host host) {
        return Tools.getString("DomainInfo.StartVNCViewerOn").replaceAll("@VIEWER@", Matcher.quoteReplacement(viewer))
                + host.getName();
    }
}
