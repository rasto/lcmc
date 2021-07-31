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

package lcmc.drbd.ui.resource;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JColorChooser;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Info;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.drbd.domain.BlockDevice;
import lcmc.drbd.service.DRBD;
import lcmc.drbd.ui.DrbdsLog;
import lcmc.drbd.ui.ProxyHostWizard;
import lcmc.host.domain.Host;
import lcmc.host.ui.EditHostDialog;
import lcmc.host.ui.HostBrowser;
import lcmc.lvm.ui.LVCreate;
import lcmc.lvm.ui.VGCreate;

@Named
public class HostDrbdMenu {
    private static final String LVM_MENU = "LVM";
    private static final String VG_CREATE_MENU_ITEM = "Create VG";
    private static final String VG_CREATE_MENU_DESCRIPTION = "Create a volume group.";
    private static final String LV_CREATE_MENU_ITEM = "Create LV in VG ";
    private static final String LV_CREATE_MENU_DESCRIPTION = "Create a logical volume.";
    @Inject
    private EditHostDialog editHostDialog;
    @Inject
    private MainData mainData;
    @Inject
    private MainPresenter mainPresenter;
    @Inject
    private ProxyHostWizard proxyHostWizard;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Application application;
    @Inject
    private Provider<VGCreate> vgCreateProvider;
    @Inject
    private Provider<LVCreate> lvCreateProvider;
    @Inject
    private Provider<DrbdsLog> drbdsLogProvider;

    public List<UpdatableItem> getPulldownMenu(final Host host, final HostDrbdInfo hostDrbdInfo) {
        final List<UpdatableItem> items = new ArrayList<>();

        /* host wizard */
        final MyMenuItem hostWizardItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.HostWizard"), HostBrowser.HOST_ICON_LARGE,
                        Tools.getString("HostBrowser.HostWizard"), new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL)).addAction(text -> editHostDialog.showDialogs(host));
        items.add(hostWizardItem);
        mainData.registerAddHostButton(hostWizardItem);

        /* proxy host wizard */
        final MyMenuItem proxyHostWizardItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.ProxyHostWizard"), HostBrowser.HOST_ICON_LARGE,
                        Tools.getString("HostBrowser.ProxyHostWizard"), new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL)).addAction(text -> {
                    proxyHostWizard.init(host, null);
                    proxyHostWizard.showDialogs();
                });
        items.add(proxyHostWizardItem);
        mainData.registerAddHostButton(proxyHostWizardItem);
        final Application.RunMode runMode = Application.RunMode.LIVE;
        /* load drbd */
        final UpdatableItem loadItem = menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.LoadDrbd"), null,
                Tools.getString("HostBrowser.Drbd.LoadDrbd"), new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL)).enablePredicate(() -> {
            if (host.isConnected()) {
                if (host.isDrbdLoaded()) {
                    return "already loaded";
                } else {
                    return null;
                }
            } else {
                return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
            }
        }).addAction(text -> {
            DRBD.load(host, runMode);
            hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(host, !Host.UPDATE_LVM);
        });
        items.add(loadItem);

        /* proxy start/stop */
        final UpdatableItem proxyItem = menuFactory.createMenuItem(Tools.getString("HostDrbdInfo.Drbd.StopProxy"), null,
                hostDrbdInfo.getMenuToolTip("DRBD.stopProxy", ""), Tools.getString("HostDrbdInfo.Drbd.StartProxy"), null,
                hostDrbdInfo.getMenuToolTip("DRBD.startProxy", ""), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL)).predicate(host::isDrbdProxyRunning).addAction(text -> {
            if (host.isDrbdProxyRunning()) {
                DRBD.stopProxy(host, runMode);
            } else {
                DRBD.startProxy(host, runMode);
            }
            hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(host, !Host.UPDATE_LVM);
        });
        items.add(proxyItem);

        /* all proxy connections up */
        final UpdatableItem allProxyUpItem = menuFactory.createMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyUp"), null,
                        hostDrbdInfo.getMenuToolTip("DRBD.proxyUp", DRBD.ALL_DRBD_RESOURCES),
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(() -> host.isConnected() && host.isDrbdProxyRunning())
                .addAction(text -> {
                    DRBD.proxyUp(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                    hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(host, !Host.UPDATE_LVM);
                });
        items.add(allProxyUpItem);

        /* all proxy connections down */
        final UpdatableItem allProxyDownItem = menuFactory.createMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyDown"), null,
                        hostDrbdInfo.getMenuToolTip("DRBD.proxyDown", DRBD.ALL_DRBD_RESOURCES),
                        new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(() -> host.isConnected() && host.isDrbdProxyRunning())
                .addAction(text -> {
                    DRBD.proxyDown(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                    hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(host, !Host.UPDATE_LVM);
                });
        items.add(allProxyDownItem);

        /* load DRBD config / adjust all */
        final MyMenuItem adjustAllItem = menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.AdjustAllDrbd"), null,
                Tools.getString("HostBrowser.Drbd.AdjustAllDrbd.ToolTip"), new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL)).enablePredicate(() -> {
            if (host.isConnected()) {
                return null;
            } else {
                return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
            }
        }).addAction(text -> {
            DRBD.adjust(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
            hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(host, !Host.UPDATE_LVM);
        });
        items.add(adjustAllItem);
        final ClusterBrowser cb = hostDrbdInfo.getBrowser().getClusterBrowser();
        if (cb != null) {
            final ButtonCallback adjustAllItemCallback = cb.new DRBDMenuItemCallback(host).addAction(
                    host18 -> DRBD.adjust(host18, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST));
            hostDrbdInfo.addMouseOverListener(adjustAllItem, adjustAllItemCallback);
        }

        /* start drbd */
        final MyMenuItem upAllItem = menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.UpAll"), null,
                Tools.getString("HostBrowser.Drbd.UpAll"), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL)).enablePredicate(() -> {
            if (!host.isDrbdStatusOk()) {
                return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
            }
            return null;
        }).addAction(text -> DRBD.up(host, DRBD.ALL_DRBD_RESOURCES, null, runMode));
        items.add(upAllItem);
        if (cb != null) {
            final ButtonCallback upAllItemCallback = cb.new DRBDMenuItemCallback(host).addAction(
                    host17 -> DRBD.up(host17, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST));
            hostDrbdInfo.addMouseOverListener(upAllItem, upAllItemCallback);
        }

        /* change host color */
        final UpdatableItem changeHostColorItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.ChangeHostColor"), null,
                        Tools.getString("HostBrowser.Drbd.ChangeHostColor"), new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL)).addAction(text -> {
                    final Color newColor = JColorChooser.showDialog(mainData.getMainFrame(), "Choose " + host.getName() + " color",
                            host.getPmColors()[0]);
                    if (newColor != null) {
                        host.setSavedHostColorInGraphs(newColor);
                    }
                });
        items.add(changeHostColorItem);

        /* view logs */
        final UpdatableItem viewLogsItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"), Info.LOGFILE_ICON,
                        Tools.getString("HostBrowser.Drbd.ViewLogs"), new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL)).enablePredicate(() -> {
                    if (!host.isConnected()) {
                        return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                    }
                    return null;
                }).addAction(text -> {
                    final DrbdsLog drbdsLog = drbdsLogProvider.get();
                    drbdsLog.init(host);
                    drbdsLog.showDialog();
                });
        items.add(viewLogsItem);

        /* connect all */
        final MyMenuItem connectAllItem = menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.ConnectAll"), null,
                Tools.getString("HostBrowser.Drbd.ConnectAll"), new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL)).enablePredicate(() -> {
            if (host.isDrbdStatusOk()) {
                return null;
            } else {
                return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
            }
        }).addAction(text -> DRBD.connect(host, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST));
        items.add(connectAllItem);
        if (cb != null) {
            final ButtonCallback connectAllItemCallback = cb.new DRBDMenuItemCallback(host).addAction(
                    host16 -> DRBD.connect(host16, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST));
            hostDrbdInfo.addMouseOverListener(connectAllItem, connectAllItemCallback);
        }

        /* disconnect all */
        final MyMenuItem disconnectAllItem = menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.DisconnectAll"), null,
                Tools.getString("HostBrowser.Drbd.DisconnectAll"), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL)).enablePredicate(() -> {
            if (host.isDrbdStatusOk()) {
                return null;
            } else {
                return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
            }
        }).addAction(text -> DRBD.disconnect(host, DRBD.ALL_DRBD_RESOURCES, null, runMode));
        items.add(disconnectAllItem);
        if (cb != null) {
            final ButtonCallback disconnectAllItemCallback = cb.new DRBDMenuItemCallback(host).addAction(
                    host15 -> DRBD.disconnect(host15, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST));
            hostDrbdInfo.addMouseOverListener(disconnectAllItem, disconnectAllItemCallback);
        }

        /* attach dettached */
        final MyMenuItem attachAllItem = menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.AttachAll"), null,
                Tools.getString("HostBrowser.Drbd.AttachAll"), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL)).enablePredicate(() -> {
            if (host.isDrbdStatusOk()) {
                return null;
            } else {
                return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
            }
        }).addAction(text -> DRBD.attach(host, DRBD.ALL_DRBD_RESOURCES, null, runMode));
        items.add(attachAllItem);
        if (cb != null) {
            final ButtonCallback attachAllItemCallback = cb.new DRBDMenuItemCallback(host).addAction(
                    host14 -> DRBD.attach(host14, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST));
            hostDrbdInfo.addMouseOverListener(attachAllItem, attachAllItemCallback);
        }

        /* detach */
        final MyMenuItem detachAllItem = menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.DetachAll"), null,
                Tools.getString("HostBrowser.Drbd.DetachAll"), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL)).enablePredicate(() -> {
            if (host.isDrbdStatusOk()) {
                return null;
            } else {
                return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
            }
        }).addAction(text -> DRBD.detach(host, DRBD.ALL_DRBD_RESOURCES, null, runMode));
        items.add(detachAllItem);
        if (cb != null) {
            final ButtonCallback detachAllItemCallback = cb.new DRBDMenuItemCallback(host).addAction(
                    host13 -> DRBD.detach(host13, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST));
            hostDrbdInfo.addMouseOverListener(detachAllItem, detachAllItemCallback);
        }

        /* set all primary */
        final MyMenuItem setAllPrimaryItem = menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.SetAllPrimary"), null,
                Tools.getString("HostBrowser.Drbd.SetAllPrimary"), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL)).enablePredicate(() -> {
            if (host.isDrbdStatusOk()) {
                return null;
            } else {
                return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
            }
        }).addAction(text -> DRBD.setPrimary(host, DRBD.ALL_DRBD_RESOURCES, null, runMode));
        items.add(setAllPrimaryItem);
        if (cb != null) {
            final ButtonCallback setAllPrimaryItemCallback = cb.new DRBDMenuItemCallback(host).addAction(
                    host12 -> DRBD.setPrimary(host12, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST));
            hostDrbdInfo.addMouseOverListener(setAllPrimaryItem, setAllPrimaryItemCallback);
        }

        /* set all secondary */
        final MyMenuItem setAllSecondaryItem = menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.SetAllSecondary"), null,
                Tools.getString("HostBrowser.Drbd.SetAllSecondary"), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL)).enablePredicate(() -> {
            if (host.isDrbdStatusOk()) {
                return null;
            } else {
                return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
            }
        }).addAction(text -> DRBD.setSecondary(host, DRBD.ALL_DRBD_RESOURCES, null, runMode));
        items.add(setAllSecondaryItem);
        if (cb != null) {
            final ButtonCallback setAllSecondaryItemCallback = cb.new DRBDMenuItemCallback(host).addAction(
                    host1 -> DRBD.setSecondary(host1, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST));
            hostDrbdInfo.addMouseOverListener(setAllSecondaryItem, setAllSecondaryItemCallback);
        }

        /* remove host from gui */
        final UpdatableItem removeHostItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.RemoveHost"), HostBrowser.HOST_REMOVE_ICON,
                        Tools.getString("HostBrowser.RemoveHost"), new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL)).enablePredicate(() -> {
                    if (!host.isInCluster()) {
                        return "it is a member of a cluster";
                    }
                    return null;
                }).addAction(text -> {
                    host.disconnect();
                    application.removeHostFromHosts(host);
                    mainPresenter.allHostsUpdate();
                });
        items.add(removeHostItem);

        /* advanced options */
        final MyMenu hostAdvancedSubmenu = menuFactory.createMenu(Tools.getString("HostBrowser.AdvancedSubmenu"),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(() -> {
                    if (!host.isConnected()) {
                        return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                    }
                    return null;
                });
        hostAdvancedSubmenu.onUpdate(() -> {
            hostAdvancedSubmenu.updateMenuComponents();
            hostDrbdInfo.getBrowser().addAdvancedMenu(hostAdvancedSubmenu);
            hostAdvancedSubmenu.processAccessMode();
        });
        items.add(hostAdvancedSubmenu);
        items.add(getLVMMenu(host));
        return items;
    }

    /**
     * Returns lvm menu.
     */
    private UpdatableItem getLVMMenu(final Host host) {
        final MyMenu lvmMenu = menuFactory.createMenu(LVM_MENU, new AccessMode(AccessMode.OP, AccessMode.ADVANCED),
                new AccessMode(AccessMode.OP, AccessMode.ADVANCED));
        lvmMenu.onUpdate(() -> {
            lvmMenu.updateMenuComponents();
            addLVMMenu(lvmMenu, host);
            lvmMenu.processAccessMode();
        });
        return lvmMenu;
    }

    private void addLVMMenu(final MyMenu submenu, final Host host) {
        submenu.removeAll();
        submenu.add(getVGCreateItem(host));
        for (final BlockDevice bd : host.getBlockDevices()) {
            final String vg;
            final BlockDevice drbdBD = bd.getDrbdBlockDevice();
            vg = Objects.requireNonNullElse(drbdBD, bd).getVgOnPhysicalVolume();
            if (vg != null) {
                submenu.add(getLVMCreateItem(vg, bd, host));
            }
        }
    }

    private MyMenuItem getVGCreateItem(final Host host) {
        final MyMenuItem mi = menuFactory.createMenuItem(VG_CREATE_MENU_ITEM, null, VG_CREATE_MENU_DESCRIPTION,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .addAction(text -> {
                    final VGCreate vgCreate = vgCreateProvider.get();
                    vgCreate.init(host);
                    while (true) {
                        vgCreate.showDialog();
                        if (vgCreate.isPressedCancelButton()) {
                            vgCreate.cancelDialog();
                            return;
                        } else if (vgCreate.isPressedFinishButton()) {
                            break;
                        }
                    }
                });
        mi.setToolTipText(VG_CREATE_MENU_DESCRIPTION);
        return mi;
    }

    /**
     * Return create LV menu item.
     */
    private MyMenuItem getLVMCreateItem(final String volumeGroup, final BlockDevice blockDevice, final Host host) {
        final String name = LV_CREATE_MENU_ITEM + volumeGroup;
        final MyMenuItem mi =
                menuFactory.createMenuItem(name, null, LV_CREATE_MENU_DESCRIPTION, new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(() -> volumeGroup != null && !volumeGroup.isEmpty() && host.getHostParser()
                                .getVolumeGroupNames()
                                .contains(volumeGroup))
                        .addAction(text -> {
                            final LVCreate lvCreate = lvCreateProvider.get();
                            lvCreate.init(host, volumeGroup, blockDevice);
                            while (true) {
                                lvCreate.showDialog();
                                if (lvCreate.isPressedCancelButton()) {
                                    lvCreate.cancelDialog();
                                    return;
                                } else if (lvCreate.isPressedFinishButton()) {
                                    break;
                                }
                            }
                        });
        mi.onUpdate(() -> mi.setText1(LV_CREATE_MENU_ITEM + volumeGroup));

        mi.setToolTipText(LV_CREATE_MENU_DESCRIPTION);
        return mi;
    }
}
