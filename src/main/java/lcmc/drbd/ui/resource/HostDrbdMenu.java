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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JColorChooser;

import lcmc.host.ui.EditHostDialog;
import lcmc.drbd.ui.ProxyHostWizard;
import lcmc.common.ui.CallbackAction;
import lcmc.common.ui.GUIData;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.drbd.domain.BlockDevice;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.host.ui.HostBrowser;
import lcmc.drbd.ui.DrbdsLog;
import lcmc.lvm.ui.LVCreate;
import lcmc.lvm.ui.VGCreate;
import lcmc.common.ui.Info;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.drbd.service.DRBD;
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
public class HostDrbdMenu {
    private static final String LVM_MENU = "LVM";
    private static final String VG_CREATE_MENU_ITEM = "Create VG";
    private static final String VG_CREATE_MENU_DESCRIPTION = "Create a volume group.";
    private static final String LV_CREATE_MENU_ITEM = "Create LV in VG ";
    private static final String LV_CREATE_MENU_DESCRIPTION = "Create a logical volume.";
    @Inject
    private EditHostDialog editHostDialog;
    @Inject
    private GUIData guiData;
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
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        /* host wizard */
        final MyMenuItem hostWizardItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.HostWizard"),
                        HostBrowser.HOST_ICON_LARGE,
                        Tools.getString("HostBrowser.HostWizard"),
                        new AccessMode(Application.AccessType.RO, false),
                        new AccessMode(Application.AccessType.RO, false))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                editHostDialog.showDialogs(host);
                            }
                        });
        items.add(hostWizardItem);
        guiData.registerAddHostButton(hostWizardItem);

        /* proxy host wizard */
        final MyMenuItem proxyHostWizardItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.ProxyHostWizard"),
                        HostBrowser.HOST_ICON_LARGE,
                        Tools.getString("HostBrowser.ProxyHostWizard"),
                        new AccessMode(Application.AccessType.RO, false),
                        new AccessMode(Application.AccessType.RO, false))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                proxyHostWizard.init(host, null);
                                proxyHostWizard.showDialogs();
                            }
                        });
        items.add(proxyHostWizardItem);
        guiData.registerAddHostButton(proxyHostWizardItem);
        final Application.RunMode runMode = Application.RunMode.LIVE;
        /* load drbd */
        final UpdatableItem loadItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.LoadDrbd"),
                        null,
                        Tools.getString("HostBrowser.Drbd.LoadDrbd"),
                        new AccessMode(Application.AccessType.OP, false),
                        new AccessMode(Application.AccessType.OP, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
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
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.load(host, runMode);
                                hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(host, !Host.UPDATE_LVM);
                            }
                        });
        items.add(loadItem);

        /* proxy start/stop */
        final UpdatableItem proxyItem =
                menuFactory.createMenuItem(Tools.getString("HostDrbdInfo.Drbd.StopProxy"),
                        null,
                        hostDrbdInfo.getMenuToolTip("DRBD.stopProxy", ""),
                        Tools.getString("HostDrbdInfo.Drbd.StartProxy"),
                        null,
                        hostDrbdInfo.getMenuToolTip("DRBD.startProxy", ""),
                        new AccessMode(Application.AccessType.ADMIN, !AccessMode.ADVANCED),
                        new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED))
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                return host.isDrbdProxyRunning();
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                if (host.isDrbdProxyRunning()) {
                                    DRBD.stopProxy(host, runMode);
                                } else {
                                    DRBD.startProxy(host, runMode);
                                }
                                hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(host, !Host.UPDATE_LVM);
                            }
                        });
        items.add(proxyItem);

        /* all proxy connections up */
        final UpdatableItem allProxyUpItem =
                menuFactory.createMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyUp"),
                        null,
                        hostDrbdInfo.getMenuToolTip("DRBD.proxyUp", DRBD.ALL_DRBD_RESOURCES),
                        new AccessMode(Application.AccessType.ADMIN, !AccessMode.ADVANCED),
                        new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return host.isConnected() && host.isDrbdProxyRunning();
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.proxyUp(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                                hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(host, !Host.UPDATE_LVM);
                            }
                        });
        items.add(allProxyUpItem);

        /* all proxy connections down */
        final UpdatableItem allProxyDownItem =
                menuFactory.createMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyDown"),
                        null,
                        hostDrbdInfo.getMenuToolTip("DRBD.proxyDown", DRBD.ALL_DRBD_RESOURCES),
                        new AccessMode(Application.AccessType.ADMIN, AccessMode.ADVANCED),
                        new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return host.isConnected() && host.isDrbdProxyRunning();
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.proxyDown(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                                hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(host, !Host.UPDATE_LVM);
                            }
                        });
        items.add(allProxyDownItem);

        /* load DRBD config / adjust all */
        final MyMenuItem adjustAllItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.AdjustAllDrbd"),
                        null,
                        Tools.getString("HostBrowser.Drbd.AdjustAllDrbd.ToolTip"),
                        new AccessMode(Application.AccessType.OP, false),
                        new AccessMode(Application.AccessType.OP, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (host.isConnected()) {
                                    return null;
                                } else {
                                    return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.adjust(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                                hostDrbdInfo.getBrowser().getClusterBrowser().updateHWInfo(host, !Host.UPDATE_LVM);
                            }
                        });
        items.add(adjustAllItem);
        final ClusterBrowser cb = hostDrbdInfo.getBrowser().getClusterBrowser();
        if (cb != null) {
            final ButtonCallback adjustAllItemCallback = cb.new DRBDMenuItemCallback(host)
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host host) {
                            DRBD.adjust(host, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                        }
                    });
            hostDrbdInfo.addMouseOverListener(adjustAllItem, adjustAllItemCallback);
        }

        /* start drbd */
        final MyMenuItem upAllItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.UpAll"),
                        null,
                        Tools.getString("HostBrowser.Drbd.UpAll"),
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.ADMIN, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!host.isDrbdStatusOk()) {
                                    return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.up(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                            }
                        });
        items.add(upAllItem);
        if (cb != null) {
            final ButtonCallback upAllItemCallback = cb.new DRBDMenuItemCallback(host)
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host host) {
                            DRBD.up(host, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                        }
                    });
            hostDrbdInfo.addMouseOverListener(upAllItem, upAllItemCallback);
        }

        /* change host color */
        final UpdatableItem changeHostColorItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                        null,
                        Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                        new AccessMode(Application.AccessType.RO, false),
                        new AccessMode(Application.AccessType.RO, false))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                final Color newColor = JColorChooser.showDialog(guiData.getMainFrame(),
                                        "Choose " + host.getName() + " color",
                                        host.getPmColors()[0]);
                                if (newColor != null) {
                                    host.setSavedHostColorInGraphs(newColor);
                                }
                            }
                        });
        items.add(changeHostColorItem);

        /* view logs */
        final UpdatableItem viewLogsItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"),
                        Info.LOGFILE_ICON,
                        Tools.getString("HostBrowser.Drbd.ViewLogs"),
                        new AccessMode(Application.AccessType.RO, false),
                        new AccessMode(Application.AccessType.RO, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!host.isConnected()) {
                                    return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                final DrbdsLog drbdsLog = drbdsLogProvider.get();
                                drbdsLog.init(host);
                                drbdsLog.showDialog();
                            }
                        });
        items.add(viewLogsItem);

        /* connect all */
        final MyMenuItem connectAllItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.ConnectAll"),
                        null,
                        Tools.getString("HostBrowser.Drbd.ConnectAll"),
                        new AccessMode(Application.AccessType.OP, false),
                        new AccessMode(Application.AccessType.OP, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (host.isDrbdStatusOk()) {
                                    return null;
                                } else {
                                    return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.connect(host, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                            }
                        });
        items.add(connectAllItem);
        if (cb != null) {
            final ButtonCallback connectAllItemCallback = cb.new DRBDMenuItemCallback(host)
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host host) {
                            DRBD.connect(host, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                        }
                    });
            hostDrbdInfo.addMouseOverListener(connectAllItem, connectAllItemCallback);
        }

        /* disconnect all */
        final MyMenuItem disconnectAllItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.DisconnectAll"),
                        null,
                        Tools.getString("HostBrowser.Drbd.DisconnectAll"),
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (host.isDrbdStatusOk()) {
                                    return null;
                                } else {
                                    return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.disconnect(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                            }
                        });
        items.add(disconnectAllItem);
        if (cb != null) {
            final ButtonCallback disconnectAllItemCallback = cb.new DRBDMenuItemCallback(host)
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host host) {
                            DRBD.disconnect(host, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                        }
                    });
            hostDrbdInfo.addMouseOverListener(disconnectAllItem, disconnectAllItemCallback);
        }

        /* attach dettached */
        final MyMenuItem attachAllItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.AttachAll"),
                        null,
                        Tools.getString("HostBrowser.Drbd.AttachAll"),
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (host.isDrbdStatusOk()) {
                                    return null;
                                } else {
                                    return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.attach(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                            }
                        });
        items.add(attachAllItem);
        if (cb != null) {
            final ButtonCallback attachAllItemCallback = cb.new DRBDMenuItemCallback(host)
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host host) {
                            DRBD.attach(host, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                        }
                    });
            hostDrbdInfo.addMouseOverListener(attachAllItem, attachAllItemCallback);
        }

        /* detach */
        final MyMenuItem detachAllItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.DetachAll"),
                        null,
                        Tools.getString("HostBrowser.Drbd.DetachAll"),
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (host.isDrbdStatusOk()) {
                                    return null;
                                } else {
                                    return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.detach(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                            }
                        });
        items.add(detachAllItem);
        if (cb != null) {
            final ButtonCallback detachAllItemCallback = cb.new DRBDMenuItemCallback(host)
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host host) {
                            DRBD.detach(host, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                        }
                    });
            hostDrbdInfo.addMouseOverListener(detachAllItem, detachAllItemCallback);
        }

        /* set all primary */
        final MyMenuItem setAllPrimaryItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.SetAllPrimary"),
                        null,
                        Tools.getString("HostBrowser.Drbd.SetAllPrimary"),
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (host.isDrbdStatusOk()) {
                                    return null;
                                } else {
                                    return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.setPrimary(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                            }
                        });
        items.add(setAllPrimaryItem);
        if (cb != null) {
            final ButtonCallback setAllPrimaryItemCallback = cb.new DRBDMenuItemCallback(host)
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host host) {
                            DRBD.setPrimary(host, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                        }
                    });
            hostDrbdInfo.addMouseOverListener(setAllPrimaryItem, setAllPrimaryItemCallback);
        }

        /* set all secondary */
        final MyMenuItem setAllSecondaryItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.SetAllSecondary"),
                        null,
                        Tools.getString("HostBrowser.Drbd.SetAllSecondary"),
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.ADMIN, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (host.isDrbdStatusOk()) {
                                    return null;
                                } else {
                                    return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.setSecondary(host, DRBD.ALL_DRBD_RESOURCES, null, runMode);
                            }
                        });
        items.add(setAllSecondaryItem);
        if (cb != null) {
            final ButtonCallback setAllSecondaryItemCallback = cb.new DRBDMenuItemCallback(host)
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host host) {
                            DRBD.setSecondary(host, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                        }
                    });
            hostDrbdInfo.addMouseOverListener(setAllSecondaryItem, setAllSecondaryItemCallback);
        }

        /* remove host from gui */
        final UpdatableItem removeHostItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.RemoveHost"),
                        HostBrowser.HOST_REMOVE_ICON,
                        Tools.getString("HostBrowser.RemoveHost"),
                        new AccessMode(Application.AccessType.RO, false),
                        new AccessMode(Application.AccessType.RO, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!host.isInCluster()) {
                                    return "it is a member of a cluster";
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                host.disconnect();
                                application.removeHostFromHosts(host);
                                guiData.allHostsUpdate();
                            }
                        });
        items.add(removeHostItem);

        /* advanced options */
        final MyMenu hostAdvancedSubmenu = menuFactory.createMenu(
                Tools.getString("HostBrowser.AdvancedSubmenu"),
                new AccessMode(Application.AccessType.OP, false),
                new AccessMode(Application.AccessType.OP, false))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!host.isConnected()) {
                            return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                        }
                        return null;
                    }
                });
        hostAdvancedSubmenu.onUpdate(new Runnable() {
            @Override
            public void run() {
                hostAdvancedSubmenu.updateMenuComponents();
                hostDrbdInfo.getBrowser().addAdvancedMenu(hostAdvancedSubmenu);
                hostAdvancedSubmenu.processAccessMode();
            }
        });
        items.add(hostAdvancedSubmenu);
        items.add(getLVMMenu(host));
        return items;
    }

    /**
     * Returns lvm menu.
     */
    private UpdatableItem getLVMMenu(final Host host) {
        final MyMenu lvmMenu = menuFactory.createMenu(LVM_MENU,
                new AccessMode(Application.AccessType.OP, true),
                new AccessMode(Application.AccessType.OP, true));
        lvmMenu.onUpdate(new Runnable() {
            @Override
            public void run() {
                lvmMenu.updateMenuComponents();
                addLVMMenu(lvmMenu, host);
                lvmMenu.processAccessMode();
            }
        });
        return lvmMenu;
    }

    private void addLVMMenu(final MyMenu submenu, final Host host) {
        submenu.removeAll();
        submenu.add(getVGCreateItem(host));
        for (final BlockDevice bd : host.getBlockDevices()) {
            final String vg;
            final BlockDevice drbdBD = bd.getDrbdBlockDevice();
            if (drbdBD == null) {
                vg = bd.getVolumeGroupOnPhysicalVolume();
            } else {
                vg = drbdBD.getVolumeGroupOnPhysicalVolume();
            }
            if (vg != null) {
                submenu.add(getLVMCreateItem(vg, bd, host));
            }
        }
    }

    private MyMenuItem getVGCreateItem(final Host host) {
        final MyMenuItem mi = menuFactory.createMenuItem(VG_CREATE_MENU_ITEM,
                null,
                VG_CREATE_MENU_DESCRIPTION,
                new AccessMode(Application.AccessType.OP, false),
                new AccessMode(Application.AccessType.OP, false))
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
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
        final MyMenuItem mi = menuFactory.createMenuItem(name,
                null,
                LV_CREATE_MENU_DESCRIPTION,
                new AccessMode(Application.AccessType.OP, false),
                new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        return volumeGroup != null
                                && !volumeGroup.isEmpty()
                                && host.getVolumeGroupNames().contains(volumeGroup);
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
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
                    }
                });
        mi.onUpdate(new Runnable() {
            @Override
            public void run() {
                mi.setText1(LV_CREATE_MENU_ITEM + volumeGroup);
            }
        });

        mi.setToolTipText(LV_CREATE_MENU_DESCRIPTION);
        return mi;
    }
}
