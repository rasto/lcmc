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

package lcmc.gui.resources.drbd;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JColorChooser;
import lcmc.EditHostDialog;
import lcmc.ProxyHostWizard;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.data.resources.BlockDevice;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.HostBrowser;
import lcmc.gui.dialog.drbd.DrbdsLog;
import lcmc.gui.dialog.lvm.LVCreate;
import lcmc.gui.dialog.lvm.VGCreate;
import lcmc.gui.resources.Info;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.DRBD;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class HostDrbdMenu {

    /** LVM menu. */
    private static final String LVM_MENU = "LVM";
    /** Name of the create VG menu item. */
    private static final String VG_CREATE_MENU_ITEM = "Create VG";
    /** Description create VG. */
    private static final String VG_CREATE_MENU_DESCRIPTION =
                                                    "Create a volume group.";
    /** Name of the create menu item. */
    private static final String LV_CREATE_MENU_ITEM = "Create LV in VG ";
    /** Description create LV. */
    private static final String LV_CREATE_MENU_DESCRIPTION =
                                                    "Create a logical volume.";
    private final Host host;
    private final HostDrbdInfo hostDrbdInfo;

    public HostDrbdMenu(final Host host, final HostDrbdInfo hostDrbdInfo) {
        this.host = host;
        this.hostDrbdInfo = hostDrbdInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        /* host wizard */
        final MyMenuItem hostWizardItem =
            new MyMenuItem(Tools.getString("HostBrowser.HostWizard"),
                           HostBrowser.HOST_ICON_LARGE,
                           Tools.getString("HostBrowser.HostWizard"),
                           new AccessMode(Application.AccessType.RO,
                                          false),
                           new AccessMode(Application.AccessType.RO,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final EditHostDialog dialog = new EditHostDialog(host);
                    dialog.showDialogs();
                }
            };
        items.add(hostWizardItem);
        Tools.getGUIData().registerAddHostButton(hostWizardItem);

        /* proxy host wizard */
        final MyMenuItem proxyHostWizardItem =
            new MyMenuItem(Tools.getString("HostBrowser.ProxyHostWizard"),
                           HostBrowser.HOST_ICON_LARGE,
                           Tools.getString("HostBrowser.ProxyHostWizard"),
                           new AccessMode(Application.AccessType.RO,
                                          false),
                           new AccessMode(Application.AccessType.RO,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final ProxyHostWizard dialog = new ProxyHostWizard(host,
                                                                       null);
                    dialog.showDialogs();
                }
            };
        items.add(proxyHostWizardItem);
        Tools.getGUIData().registerAddHostButton(proxyHostWizardItem);
        final Application.RunMode runMode = Application.RunMode.LIVE;
        /* load drbd */
        final UpdatableItem loadItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.LoadDrbd"),
                           null,
                           Tools.getString("HostBrowser.Drbd.LoadDrbd"),
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (host.isConnected()) {
                        if (host.isDrbdLoaded()) {
                            return "already loaded";
                        } else {
                            return null;
                        }
                    } else {
                        return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                    }
                }

                @Override
                public void action() {
                    DRBD.load(host, runMode);
                    hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(
                                                        host,
                                                        !Host.UPDATE_LVM);
                }
            };
        items.add(loadItem);

        /* proxy start/stop */
        final UpdatableItem proxyItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Drbd.StopProxy"),
                           null,
                           hostDrbdInfo.getMenuToolTip("DRBD.stopProxy", ""),
                           Tools.getString("HostDrbdInfo.Drbd.StartProxy"),
                           null,
                           hostDrbdInfo.getMenuToolTip("DRBD.startProxy", ""),
                           new AccessMode(Application.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return host.isDrbdProxyRunning();
                }

                @Override
                public void action() {
                    if (host.isDrbdProxyRunning()) {
                        DRBD.stopProxy(host, runMode);
                    } else {
                        DRBD.startProxy(host, runMode);
                    }
                    hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(
                                                        host,
                                                        !Host.UPDATE_LVM);
                }
            };
        items.add(proxyItem);

        /* all proxy connections up */
        final UpdatableItem allProxyUpItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyUp"),
                           null,
                           hostDrbdInfo.getMenuToolTip("DRBD.proxyUp", DRBD.ALL),
                           new AccessMode(Application.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return host.isConnected() && host.isDrbdProxyRunning();
                }

                @Override
                public void action() {
                    DRBD.proxyUp(host, DRBD.ALL, null, runMode);
                    hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(
                                                        host,
                                                        !Host.UPDATE_LVM);
                }
            };
        items.add(allProxyUpItem);

        /* all proxy connections down */
        final UpdatableItem allProxyDownItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyDown"),
                           null,
                           hostDrbdInfo.getMenuToolTip("DRBD.proxyDown", DRBD.ALL),
                           new AccessMode(Application.AccessType.ADMIN,
                                          AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return host.isConnected() && host.isDrbdProxyRunning();
                }

                @Override
                public void action() {
                    DRBD.proxyDown(host, DRBD.ALL, null, runMode);
                    hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(
                                                        host,
                                                        !Host.UPDATE_LVM);
                }
            };
        items.add(allProxyDownItem);

        /* load DRBD config / adjust all */
        final MyMenuItem adjustAllItem =
            new MyMenuItem(
                   Tools.getString("HostBrowser.Drbd.AdjustAllDrbd"),
                   null,
                   Tools.getString("HostBrowser.Drbd.AdjustAllDrbd.ToolTip"),
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (host.isConnected()) {
                        return null;
                    } else {
                        return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                    }
                }

                @Override
                public void action() {
                    DRBD.adjust(host, DRBD.ALL, null, runMode);
                    hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(
                                                        host,
                                                        !Host.UPDATE_LVM);
                }
            };
        items.add(adjustAllItem);
        final ClusterBrowser cb = hostDrbdInfo.getBrowser().getClusterBrowser();
        if (cb != null) {
            final ButtonCallback adjustAllItemCallback =
                                       cb.new DRBDMenuItemCallback(host) {
                @Override
                public void action(final Host dcHost) {
                    DRBD.adjust(host, DRBD.ALL, null, Application.RunMode.TEST);
                }
            };
            hostDrbdInfo.addMouseOverListener(adjustAllItem, adjustAllItemCallback);
        }

        /* start drbd */
        final MyMenuItem upAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.UpAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.UpAll"),
                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (!host.isDrbdStatusOk()) {
                        return HostDrbdInfo.NO_DRBD_STATUS_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    DRBD.up(host, DRBD.ALL, null, runMode);
                }
            };
        items.add(upAllItem);
        if (cb != null) {
            final ButtonCallback upAllItemCallback =
                                      cb.new DRBDMenuItemCallback(host) {
                @Override
                public void action(final Host dcHost) {
                    DRBD.up(host, DRBD.ALL, null, Application.RunMode.TEST);
                }
            };
            hostDrbdInfo.addMouseOverListener(upAllItem, upAllItemCallback);
        }

        /* upgrade drbd */
        final UpdatableItem upgradeDrbdItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.UpgradeDrbd"),
                           null,
                           Tools.getString("HostBrowser.Drbd.UpgradeDrbd"),
                           new AccessMode(Application.AccessType.GOD,
                                          false), // TODO: does not work yet
                           new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (!host.isConnected()) {
                        return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                    }
                    return null;
                }

                @Override
                public void action() {
                    hostDrbdInfo.upgradeDrbd();
                }
            };
        items.add(upgradeDrbdItem);

        /* change host color */
        final UpdatableItem changeHostColorItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                           null,
                           Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                           new AccessMode(Application.AccessType.RO, false),
                           new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final Color newColor = JColorChooser.showDialog(
                                            Tools.getGUIData().getMainFrame(),
                                            "Choose " + host.getName()
                                            + " color",
                                            host.getPmColors()[0]);
                    if (newColor != null) {
                        host.setSavedHostColorInGraphs(newColor);
                    }
                }
            };
        items.add(changeHostColorItem);

        /* view logs */
        final UpdatableItem viewLogsItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           Info.LOGFILE_ICON,
                           Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           new AccessMode(Application.AccessType.RO, false),
                           new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (!host.isConnected()) {
                        return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                    }
                    return null;
                }

                @Override
                public void action() {
                    final DrbdsLog l = new DrbdsLog(host);
                    l.showDialog();
                }
            };
        items.add(viewLogsItem);

        /* connect all */
        final MyMenuItem connectAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ConnectAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.ConnectAll"),
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (host.isDrbdStatusOk()) {
                        return null;
                    } else {
                        return HostDrbdInfo.NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.connect(host, DRBD.ALL, null, Application.RunMode.TEST);
                }
            };
        items.add(connectAllItem);
        if (cb != null) {
            final ButtonCallback connectAllItemCallback =
                                       cb.new DRBDMenuItemCallback(host) {
                @Override
                public void action(final Host dcHost) {
                    DRBD.connect(host, DRBD.ALL, null, Application.RunMode.TEST);
                }
            };
            hostDrbdInfo.addMouseOverListener(connectAllItem, connectAllItemCallback);
        }

        /* disconnect all */
        final MyMenuItem disconnectAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.DisconnectAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.DisconnectAll"),
                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (host.isDrbdStatusOk()) {
                        return null;
                    } else {
                        return HostDrbdInfo.NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.disconnect(host, DRBD.ALL, null, runMode);
                }
            };
        items.add(disconnectAllItem);
        if (cb != null) {
            final ButtonCallback
                    disconnectAllItemCallback =
                                      cb.new DRBDMenuItemCallback(host) {
                @Override
                public void action(final Host dcHost) {
                    DRBD.disconnect(host, DRBD.ALL, null, Application.RunMode.TEST);
                }
            };
            hostDrbdInfo.addMouseOverListener(disconnectAllItem, disconnectAllItemCallback);
        }

        /* attach dettached */
        final MyMenuItem attachAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.AttachAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.AttachAll"),
                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (host.isDrbdStatusOk()) {
                        return null;
                    } else {
                        return HostDrbdInfo.NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.attach(host, DRBD.ALL, null, runMode);
                }
            };
        items.add(attachAllItem);
        if (cb != null) {
            final ButtonCallback
                    attachAllItemCallback =
                                       cb.new DRBDMenuItemCallback(host) {
                @Override
                public void action(final Host dcHost) {
                    DRBD.attach(host, DRBD.ALL, null, Application.RunMode.TEST);
                }
            };
            hostDrbdInfo.addMouseOverListener(attachAllItem, attachAllItemCallback);
        }

        /* detach */
        final MyMenuItem detachAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.DetachAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.DetachAll"),
                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (host.isDrbdStatusOk()) {
                        return null;
                    } else {
                        return HostDrbdInfo.NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.detach(host, DRBD.ALL, null, runMode);
                }
            };
        items.add(detachAllItem);
        if (cb != null) {
            final ButtonCallback
                    detachAllItemCallback = cb.new DRBDMenuItemCallback(host) {
                @Override
                public void action(final Host dcHost) {
                    DRBD.detach(host, DRBD.ALL, null, Application.RunMode.TEST);
                }
            };
            hostDrbdInfo.addMouseOverListener(detachAllItem, detachAllItemCallback);
        }

        /* set all primary */
        final MyMenuItem setAllPrimaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetAllPrimary"),
                           null,
                           Tools.getString("HostBrowser.Drbd.SetAllPrimary"),
                           new AccessMode(
                                   Application.AccessType.ADMIN,
                                   false),
                           new AccessMode(
                                   Application.AccessType.OP,
                                   false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (host.isDrbdStatusOk()) {
                        return null;
                    } else {
                        return HostDrbdInfo.NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.setPrimary(host, DRBD.ALL, null, runMode);
                }
            };
        items.add(setAllPrimaryItem);
        if (cb != null) {
            final ButtonCallback
                    setAllPrimaryItemCallback =
                                       cb.new DRBDMenuItemCallback(host) {
                @Override
                public void action(final Host dcHost) {
                    DRBD.setPrimary(host, DRBD.ALL, null, Application.RunMode.TEST);
                }
            };
            hostDrbdInfo.addMouseOverListener(setAllPrimaryItem, setAllPrimaryItemCallback);
        }

        /* set all secondary */
        final MyMenuItem setAllSecondaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetAllSecondary"),
                           null,
                           Tools.getString("HostBrowser.Drbd.SetAllSecondary"),
                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (host.isDrbdStatusOk()) {
                        return null;
                    } else {
                        return HostDrbdInfo.NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.setSecondary(host, DRBD.ALL, null, runMode);
                }
            };
        items.add(setAllSecondaryItem);
        if (cb != null) {
            final ButtonCallback
                    setAllSecondaryItemCallback =
                                       cb.new DRBDMenuItemCallback(host) {
                @Override
                public void action(final Host dcHost) {
                    DRBD.setSecondary(host, DRBD.ALL, null, Application.RunMode.TEST);
                }
            };
            hostDrbdInfo.addMouseOverListener(setAllSecondaryItem,
                                 setAllSecondaryItemCallback);
        }

        /* remove host from gui */
        final UpdatableItem removeHostItem =
            new MyMenuItem(Tools.getString("HostBrowser.RemoveHost"),
                           HostBrowser.HOST_REMOVE_ICON,
                           Tools.getString("HostBrowser.RemoveHost"),
                           new AccessMode(Application.AccessType.RO, false),
                           new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (!host.isInCluster()) {
                        return "it is a member of a cluster";
                    }
                    return null;
                }

                @Override
                public void action() {
                    host.disconnect();
                    Tools.getApplication().removeHostFromHosts(host);
                    Tools.getGUIData().allHostsUpdate();
                }
            };
        items.add(removeHostItem);

        /* advanced options */
        final UpdatableItem hostAdvancedSubmenu = new MyMenu(
                                Tools.getString("HostBrowser.AdvancedSubmenu"),
                                new AccessMode(Application.AccessType.OP, false),
                                new AccessMode(Application.AccessType.OP,
                                               false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (!host.isConnected()) {
                    return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                }
                return null;
            }

            @Override
            public void updateAndWait() {
                super.updateAndWait();
                hostDrbdInfo.getBrowser().addAdvancedMenu(this);
            }
        };
        items.add(hostAdvancedSubmenu);
        items.add(getLVMMenu());
        return items;
    }

    /** Returns lvm menu. */
    private UpdatableItem getLVMMenu() {
        return new MyMenu(LVM_MENU,
                          new AccessMode(Application.AccessType.OP, true),
                          new AccessMode(Application.AccessType.OP, true)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void updateAndWait() {
                super.updateAndWait();
                addLVMMenu(this);
            }
        };
    }

    /** Adds menus to manage LVMs. */
    private void addLVMMenu(final MyMenu submenu) {
        submenu.removeAll();
        submenu.add(getVGCreateItem());
        for (final BlockDevice bd : host.getBlockDevices()) {
            final String vg;
            final BlockDevice drbdBD = bd.getDrbdBlockDevice();
            if (drbdBD == null) {
                vg = bd.getVolumeGroupOnPhysicalVolume();
            } else {
                vg = drbdBD.getVolumeGroupOnPhysicalVolume();
            }
            if (vg != null) {
                submenu.add(getLVMCreateItem(vg, bd));
            }
        }
    }

    /** Return "Create VG" menu item. */
    private MyMenuItem getVGCreateItem() {
        final MyMenuItem mi = new MyMenuItem(
                            VG_CREATE_MENU_ITEM,
                            null,
                            VG_CREATE_MENU_DESCRIPTION,
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                final VGCreate vgCreate = new VGCreate(host);
                while (true) {
                    vgCreate.showDialog();
                    if (vgCreate.isPressedCancelButton()) {
                        vgCreate.cancelDialog();
                        return;
                    } else if (vgCreate.isPressedFinishButton()) {
                        break;
                    }
                }
            }
        };
        mi.setToolTipText(VG_CREATE_MENU_DESCRIPTION);
        return mi;
    }

    /** Return create LV menu item. */
    private MyMenuItem getLVMCreateItem(final String volumeGroup,
                                        final BlockDevice blockDevice) {
        final String name = LV_CREATE_MENU_ITEM + volumeGroup;
        final MyMenuItem mi = new MyMenuItem(
                             name,
                             null,
                             LV_CREATE_MENU_DESCRIPTION,
                             new AccessMode(Application.AccessType.OP, false),
                             new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean visiblePredicate() {
                return volumeGroup != null && !volumeGroup.isEmpty()
                       && host.getVolumeGroupNames().contains(volumeGroup);
            }

            @Override
            public void action() {
                final LVCreate lvCreate = new LVCreate(host,
                                                       volumeGroup,
                                                       blockDevice);
                while (true) {
                    lvCreate.showDialog();
                    if (lvCreate.isPressedCancelButton()) {
                        lvCreate.cancelDialog();
                        return;
                    } else if (lvCreate.isPressedFinishButton()) {
                        break;
                    }
                }
            }

            @Override
            public void updateAndWait() {
                setText1(LV_CREATE_MENU_ITEM + volumeGroup);
                super.updateAndWait();
            }
        };

        mi.setToolTipText(LV_CREATE_MENU_DESCRIPTION);
        return mi;
    }

}
