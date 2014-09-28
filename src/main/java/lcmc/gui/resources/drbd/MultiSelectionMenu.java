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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JColorChooser;

import lcmc.gui.CallbackAction;
import lcmc.gui.GUIData;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.drbd.domain.BlockDevice;
import lcmc.gui.ClusterBrowser;
import lcmc.lvm.ui.LVCreate;
import lcmc.lvm.ui.VGCreate;
import lcmc.lvm.ui.VGRemove;
import lcmc.gui.resources.Info;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.DRBD;
import lcmc.utilities.EnablePredicate;
import lcmc.utilities.MenuAction;
import lcmc.utilities.MenuFactory;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.VisiblePredicate;

@Named
public class MultiSelectionMenu {
    private static final String LV_CREATE_MENU_ITEM = Tools.getString("MultiSelectionInfo.LVCreate");
    
    private MultiSelectionInfo multiSelectionInfo;

    private List<Info> selectedInfos;

    @Inject
    private GUIData guiData;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Application application;
    @Inject
    private Provider<VGCreate> vgCreateProvider;
    @Inject
    private Provider<VGRemove> vgRemoveProvider;
    @Inject
    private Provider<LVCreate> lvCreateProvider;

    public List<UpdatableItem> getPulldownMenu(final MultiSelectionInfo multiSelectionInfo,
                                               final List<Info> selectedInfos) {
        this.multiSelectionInfo = multiSelectionInfo;
        this.selectedInfos = selectedInfos;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final List<BlockDevInfo> selectedBlockDevInfos = new ArrayList<BlockDevInfo>();
        final List<HostDrbdInfo> selectedHostInfos = new ArrayList<HostDrbdInfo>();
        for (final Info i : selectedInfos) {
            if (i instanceof BlockDevInfo) {
                selectedBlockDevInfos.add((BlockDevInfo) i);
            } else if (i instanceof HostDrbdInfo) {
                selectedHostInfos.add((HostDrbdInfo) i);
            }
        }
        if (!selectedHostInfos.isEmpty()) {
            createSelectedHostsPopup(selectedHostInfos, items);
        }
        if (!selectedBlockDevInfos.isEmpty()) {
            createSelectedBlockDevPopup(selectedBlockDevInfos, items);
        }
        return items;
    }

    /** Create menu items for selected hosts. */
    private void createSelectedHostsPopup(final List<HostDrbdInfo> selectedHostInfos,
                                          final Collection<UpdatableItem> items) {
        /* load drbd */
        final UpdatableItem loadItem =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.LoadDrbd"),
                           null,
                           Tools.getString("MultiSelectionInfo.LoadDrbd"),
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false))
            .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        for (final HostDrbdInfo hi : selectedHostInfos) {
                            if (hi.getHost().isConnected()) {
                                if (hi.getHost().isDrbdLoaded()) {
                                    return "already loaded";
                                }
                            } else {
                                return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                            }
                        }
                        return null;
                    }})
            .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        for (final HostDrbdInfo hi : selectedHostInfos) {
                            DRBD.load(hi.getHost(), Application.RunMode.LIVE);
                        }
                        for (final HostDrbdInfo hi : selectedHostInfos) {
                            getBrowser().updateHWInfo(hi.getHost(), !Host.UPDATE_LVM);
                        }
                    }});
        items.add(loadItem);

        /* load DRBD config / adjust all */
        final MyMenuItem adjustAllItem =
            menuFactory.createMenuItem(
                   Tools.getString("MultiSelectionInfo.AdjustAllDrbd"),
                   null,
                   Tools.getString("MultiSelectionInfo.AdjustAllDrbd"),
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false))
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (!hi.getHost().isConnected()) {
                            return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                        }
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.adjustApply(hi.getHost(), DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.LIVE);
                    }
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost(), !Host.UPDATE_LVM);
                    }
                }});
        items.add(adjustAllItem);
        final ButtonCallback adjustAllItemCallback =
               getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost())
                   .addAction(new CallbackAction() {
                           @Override
                           public void run(final Host host) {
                for (final HostDrbdInfo hi : selectedHostInfos) {
                    DRBD.adjustApply(hi.getHost(), DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                }
            }});
        multiSelectionInfo.addMouseOverListener(adjustAllItem, adjustAllItemCallback);

        /* start drbd */
        final MyMenuItem upAllItem =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.UpAll"),
                           null,
                           Tools.getString("MultiSelectionInfo.UpAll"),
                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.ADMIN, false))
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (!hi.getHost().isDrbdStatusOk()) {
                            return HostDrbdInfo.NO_DRBD_STATUS_TOOLTIP;
                        }
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.up(hi.getHost(), DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.LIVE);
                    }
                }});
        items.add(upAllItem);
        final ButtonCallback upAllItemCallback =
             getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost())
                 .addAction(new CallbackAction() {
                         @Override
                         public void run(final Host host) {
                for (final HostDrbdInfo hi : selectedHostInfos) {
                    DRBD.up(hi.getHost(), DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                }
            }});
        multiSelectionInfo.addMouseOverListener(upAllItem, upAllItemCallback);

        /* stop drbd proxy with init script */
        final UpdatableItem stopProxyItem =
            menuFactory.createMenuItem(
                        Tools.getString("MultiSelectionInfo.HostStopProxy"),
                        null,
                        Tools.getString("MultiSelectionInfo.HostStopProxy"),
                        new AccessMode(Application.AccessType.OP, false),
                        new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (hi.getHost().isDrbdProxyRunning()) {
                            return true;
                        }
                    }
                    return false;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.stopProxy(hi.getHost(), Application.RunMode.LIVE);
                    }
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost(), !Host.UPDATE_LVM);
                    }
                }});
        items.add(stopProxyItem);

        /* start drbd proxy with init script */
        final UpdatableItem startProxyItem = menuFactory.createMenuItem(
                      Tools.getString("MultiSelectionInfo.HostStartProxy"),
                      null,
                      Tools.getString("MultiSelectionInfo.HostStartProxy"),
                      new AccessMode(Application.AccessType.OP, false),
                      new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (!hi.getHost().isDrbdProxyRunning()) {
                            return true;
                        }
                    }
                    return false;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.startProxy(hi.getHost(), Application.RunMode.LIVE);
                    }
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost(), !Host.UPDATE_LVM);
                    }
                }});
        items.add(startProxyItem);

        /* change host color */
        final UpdatableItem changeHostColorItem = menuFactory.createMenuItem(
                    Tools.getString("MultiSelectionInfo.ChangeHostColor"),
                    null,
                    "",
                    new AccessMode(Application.AccessType.RO, false),
                    new AccessMode(Application.AccessType.RO, false))
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final Host firstHost = selectedHostInfos.get(0).getHost();
                        final Color newColor = JColorChooser.showDialog(
                                guiData.getMainFrame(),
                                "Choose " + selectedHostInfos + " color",
                                firstHost.getPmColors()[0]);
                        for (final HostDrbdInfo hi : selectedHostInfos) {
                            if (newColor != null) {
                                hi.getHost().setSavedHostColorInGraphs(newColor);
                            }
                        }
                    }
                });
        items.add(changeHostColorItem);
    }

    /** Returns 'PV create' menu item. */
    private UpdatableItem getPVCreateItem(final Iterable<BlockDevInfo> selectedBlockDevInfos) {
        return menuFactory.createMenuItem(
                    Tools.getString("MultiSelectionInfo.PVCreate"),
                    null,
                    Tools.getString("MultiSelectionInfo.PVCreate.ToolTip"),
                    new AccessMode(Application.AccessType.OP, false),
                    new AccessMode(Application.AccessType.OP, false))
            .visiblePredicate(new VisiblePredicate() {
                @Override
                public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.canCreatePV()
                                && (!bdi.getBlockDevice().isDrbd() || bdi.getBlockDevice().isPrimary())) {
                            return true;
                        }
                    }
                    return false;
                }
            })
            .addAction(new MenuAction() {
                @Override
                public void run(final String text) {
                /* at least one must be true */
                    final Collection<Host> hosts = new HashSet<Host>();
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.canCreatePV() && (!bdi.getBlockDevice().isDrbd() || bdi.getBlockDevice().isPrimary())) {
                            final boolean ret = bdi.pvCreate(Application.RunMode.LIVE);
                            if (!ret) {
                                guiData.progressIndicatorFailed(
                                        Tools.getString("BlockDevInfo.PVCreate.Failed", bdi.getName()));
                            }
                            hosts.add(bdi.getHost());
                        }
                    }
                    for (final Host h : hosts) {
                        h.getBrowser().getClusterBrowser().updateHWInfo(h, Host.UPDATE_LVM);
                    }
                }
            });
    }

    /** Returns 'PV remove' menu item. */
    private UpdatableItem getPVRemoveItem(final Iterable<BlockDevInfo> selectedBlockDevInfos) {
        return menuFactory.createMenuItem(
                    Tools.getString("MultiSelectionInfo.PVRemove"),
                    null,
                    Tools.getString("MultiSelectionInfo.PVRemove.ToolTip"),
                    new AccessMode(Application.AccessType.OP, false),
                    new AccessMode(Application.AccessType.OP, false))
            .visiblePredicate(new VisiblePredicate() {
                @Override
                public boolean check() {
                /* at least one must be true */
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.canRemovePV()
                                && (!bdi.getBlockDevice().isDrbd() || !bdi.getBlockDevice().isDrbdPhysicalVolume())) {
                            return true;
                        }
                    }
                    return false;
                }
            })
            .addAction(new MenuAction() {
                @Override
                public void run(final String text) {
                    final Collection<Host> hosts = new HashSet<Host>();
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.canRemovePV()
                                && (!bdi.getBlockDevice().isDrbd() || !bdi.getBlockDevice().isDrbdPhysicalVolume())) {
                            final boolean ret = bdi.pvRemove(Application.RunMode.LIVE);
                            if (!ret) {
                                guiData.progressIndicatorFailed(
                                        Tools.getString("BlockDevInfo.PVRemove.Failed", bdi.getName()));
                            }
                            hosts.add(bdi.getHost());
                        }
                    }
                    for (final Host h : hosts) {
                        h.getBrowser().getClusterBrowser().updateHWInfo(h, Host.UPDATE_LVM);
                    }
                }
            });
    }

    /** Returns 'vg create' menu item. */
    private UpdatableItem getVGCreateItem(final List<BlockDevInfo> selectedBlockDevInfos) {
        return menuFactory.createMenuItem(
                  Tools.getString("MultiSelectionInfo.VGCreate"),
                  null,
                  Tools.getString("MultiSelectionInfo.VGCreate.ToolTip"),
                  new AccessMode(Application.AccessType.OP, false),
                  new AccessMode(Application.AccessType.OP, false))
            .visiblePredicate(new VisiblePredicate() {
                @Override
                public boolean check() {
                /* all of them must be true */

                    if (selectedBlockDevInfos.isEmpty()) {
                        return false;
                    }

                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        final BlockDevice bd;
                        if (bdi.getBlockDevice().isDrbd()) {
                            if (!bdi.getBlockDevice().isPrimary()) {
                                return false;
                            }
                            bd = bdi.getBlockDevice().getDrbdBlockDevice();
                            if (bd == null) {
                                return false;
                            }
                        } else {
                            bd = bdi.getBlockDevice();
                        }
                        if (!bd.isPhysicalVolume() || bd.isVolumeGroupOnPhysicalVolume()) {
                            return false;
                        }
                    }
                    return true;
                }
            })
            .addAction(new MenuAction() {
                @Override
                public void run(final String text) {
                    final VGCreate vgCreate = vgCreateProvider.get();
                    vgCreate.init(selectedBlockDevInfos.get(0).getHost(), selectedBlockDevInfos);
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
    }

    /** Returns 'vg remove' menu item. */
    private UpdatableItem getVGRemoveItem(final Collection<BlockDevInfo> selectedBlockDevInfos) {
        return menuFactory.createMenuItem(
                  Tools.getString("MultiSelectionInfo.VGRemove"),
                  null,
                  Tools.getString("MultiSelectionInfo.VGRemove.ToolTip"),
                  new AccessMode(Application.AccessType.OP, false),
                  new AccessMode(Application.AccessType.OP, false))
            .visiblePredicate(new VisiblePredicate() {
                @Override
                public boolean check() {
                /* all of them must be true */

                    if (selectedBlockDevInfos.isEmpty()) {
                        return false;
                    }
                /* at least one can be removed */
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.canRemoveVG()) {
                            return true;
                        }
                    }
                    return false;
                }
            })
            .addAction(new MenuAction() {
                @Override
                public void run(final String text) {
                    final List<BlockDevInfo> canRemove = new ArrayList<BlockDevInfo>();
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.canRemoveVG()) {
                            canRemove.add(bdi);
                        }
                    }
                    final VGRemove vgRemove = vgRemoveProvider.get();
                    vgRemove.init(canRemove);
                    while (true) {
                        vgRemove.showDialog();
                        if (vgRemove.isPressedCancelButton()) {
                            vgRemove.cancelDialog();
                            return;
                        } else if (vgRemove.isPressedFinishButton()) {
                            break;
                        }
                    }
                }
            });
    }

    /** Returns 'lv create' menu item. */
    private Collection<MyMenuItem> getLVCreateItems(final Iterable<BlockDevInfo> selectedBlockDevInfos) {
        final Map<String, Set<BlockDevInfo>> vgs = new LinkedHashMap<String, Set<BlockDevInfo>>();
        final Collection<MyMenuItem> mis = new ArrayList<MyMenuItem>();
        for (final BlockDevInfo bdi : selectedBlockDevInfos) {
            final String vgName = bdi.getVGName();
            Set<BlockDevInfo> bdis = vgs.get(vgName);
            if (bdis == null) {
                bdis = new LinkedHashSet<BlockDevInfo>();
                vgs.put(vgName, bdis);
            }
            bdis.add(bdi);
        }
        for (final Map.Entry<String, Set<BlockDevInfo>> entry : vgs.entrySet()) {
            final String vgName = entry.getKey();
            final Set<BlockDevInfo> bdis = entry.getValue();
            String name = LV_CREATE_MENU_ITEM;
            if (vgName != null) {
                name += vgName;
            }

            final MyMenuItem mi = menuFactory.createMenuItem(
                    name,
                    null,
                    Tools.getString("MultiSelectionInfo.LVCreate.ToolTip"),
                    new AccessMode(Application.AccessType.OP, false),
                    new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        for (final BlockDevInfo bdi : bdis) {
                            final String vg = bdi.getVGName();
                            if (vg != null && !vg.isEmpty() && bdi.getHost().getVolumeGroupNames().contains(vg)) {
                                return true;
                            }
                        }
                        return false;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final LVCreate lvCreate = lvCreateProvider.get();
                        lvCreate.init(bdis, bdis.iterator().next().getVGName());
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
                    mi.setText1(LV_CREATE_MENU_ITEM
                             + bdis.iterator().next().getVGName());
                }
            });
            mis.add(mi);
        }
        return mis;
    }

    /** Returns 'LV remove' menu item. */
    private UpdatableItem getLVRemoveItem(final Iterable<BlockDevInfo> selectedBlockDevInfos) {
        return menuFactory.createMenuItem(
                    Tools.getString("MultiSelectionInfo.LVRemove"),
                    null,
                    Tools.getString("MultiSelectionInfo.LVRemove.ToolTip"),
                    new AccessMode(Application.AccessType.OP, false),
                    new AccessMode(Application.AccessType.OP, false))
            .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.isLVM() && !bdi.getBlockDevice().isDrbd()) {
                        return true;
                    }
                }
                return false;
            }})
            .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                final Collection<Host> selectedHosts = new HashSet<Host>();
                final Collection<String> bdNames = new ArrayList<String>();
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    bdNames.add(bdi.getName());
                    selectedHosts.add(bdi.getHost());
                }
                if (application.confirmDialog(
                        Tools.getString("MultiSelectionInfo.LVRemove.Confirm.Title"),
                        Tools.getString("MultiSelectionInfo.LVRemove.Confirm.Desc", Tools.join(", ", bdNames)),
                        Tools.getString("MultiSelectionInfo.LVRemove.Confirm.Remove"),
                        Tools.getString("MultiSelectionInfo.LVRemove.Confirm.Cancel"))) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        bdi.lvRemove(Application.RunMode.LIVE);
                    }
                    for (final Host h : selectedHosts) {
                        getBrowser().updateHWInfo(h, Host.UPDATE_LVM);
                    }
                }
            }});
    }

    /** Create menu items for selected block devices. */
    private void createSelectedBlockDevPopup(final List<BlockDevInfo> selectedBlockDevInfos,
                                             final Collection<UpdatableItem> items) {
        /* detach */
        final MyMenuItem detachMenu =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.Detach"),
                           BlockDevInfo.NO_HARDDISK_ICON_LARGE,
                           Tools.getString("MultiSelectionInfo.Detach"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        boolean oneAttached = false;
                        for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                            if (!bdi.getBlockDevice().isDrbd()) {
                                continue;
                            }
                            if (!bdi.isDiskless(Application.RunMode.LIVE)) {
                                oneAttached = true;
                            }
                        }
                        return oneAttached;
                    }
                })
                    .enablePredicate(new EnablePredicate() {
                        @Override
                        public String check() {
                            boolean detachable = false;
                            for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                                if (!bdi.getBlockDevice().isDrbd()) {
                                    continue;
                                }
                                if (!application.isAdvancedMode() && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                                    continue;
                                }
                                if (bdi.getBlockDevice().isSyncing()) {
                                    continue;
                                }
                                detachable = true;
                            }
                            if (detachable) {
                                return null;
                            } else {
                                return "nothing do detach";
                            }
                        }
                    })
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.getBlockDevice().isSyncing()
                            && !bdi.isDiskless(Application.RunMode.LIVE)) {
                            bdi.detach(Application.RunMode.LIVE);
                        }
                    }
                }});
        final ButtonCallback detachItemCallback =
              getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost())
                  .addAction(new CallbackAction() {
                          @Override
                          public void run(final Host host) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && !bdi.getBlockDevice().isSyncing()
                        && !bdi.isDiskless(Application.RunMode.LIVE)) {
                        bdi.detach(Application.RunMode.TEST);
                    }
                }
            }});
        multiSelectionInfo.addMouseOverListener(detachMenu, detachItemCallback);
        items.add(detachMenu);

        /* attach */
        final MyMenuItem attachMenu =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.Attach"),
                           BlockDevInfo.HARDDISK_DRBD_ICON_LARGE,
                           Tools.getString("MultiSelectionInfo.Attach"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    boolean oneDetached = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (bdi.isDiskless(Application.RunMode.LIVE)) {
                            oneDetached = true;
                        }
                    }
                    return oneDetached;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean attachable = true;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!application.isAdvancedMode() && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        attachable = true;
                    }
                    if (attachable) {
                        return null;
                    } else {
                        return "nothing to attach";
                    }
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.getBlockDevice().isSyncing()
                            && bdi.isDiskless(Application.RunMode.LIVE)) {
                            bdi.attach(Application.RunMode.LIVE);
                        }
                    }
                }});
        final ButtonCallback attachItemCallback =
             getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost())
                 .addAction(new CallbackAction() {
                         @Override
                         public void run(final Host host) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && !bdi.getBlockDevice().isSyncing()
                        && bdi.isDiskless(Application.RunMode.LIVE)) {
                        bdi.attach(Application.RunMode.TEST);
                    }
                }
            }});
        multiSelectionInfo.addMouseOverListener(attachMenu, attachItemCallback);
        items.add(attachMenu);

        /* connect */
        final MyMenuItem connectMenu =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.Connect"),
                           null,
                           Tools.getString("MultiSelectionInfo.Connect"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    boolean oneDisconnected = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.isConnectedOrWF(Application.RunMode.LIVE)) {
                            oneDisconnected = true;
                        }
                    }
                    return oneDisconnected;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean connectable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!application.isAdvancedMode() && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.isConnectedOrWF(Application.RunMode.LIVE)) {
                            continue;
                        }
                        connectable = true;
                    }
                    if (connectable) {
                        return null;
                    } else {
                        return "nothing to connect";
                    }
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.isConnectedOrWF(Application.RunMode.LIVE)) {
                            bdi.connect(Application.RunMode.LIVE);
                        }
                    }
                }});
        final ButtonCallback connectItemCallback =
              getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost())
                  .addAction(new CallbackAction() {
                          @Override
                          public void run(final Host host) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && !bdi.isConnectedOrWF(Application.RunMode.LIVE)) {
                        bdi.connect(Application.RunMode.TEST);
                    }
                }
            }});
        multiSelectionInfo.addMouseOverListener(connectMenu, connectItemCallback);
        items.add(connectMenu);

        /* disconnect */
        final MyMenuItem disconnectMenu =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.Disconnect"),
                           null,
                           Tools.getString("MultiSelectionInfo.Disconnect"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    boolean oneConnected = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (bdi.isConnectedOrWF(Application.RunMode.LIVE)) {
                            oneConnected = true;
                        }
                    }
                    return oneConnected;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean disconnectable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!application.isAdvancedMode() && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()
                            && ((bdi.getBlockDevice().isPrimary() && bdi.getBlockDevice().isSyncTarget())
                                || (bdi.getOtherBlockDevInfo().getBlockDevice().isPrimary()
                                    && bdi.getBlockDevice().isSyncSource()))) {
                            continue;
                        }
                        if (!bdi.isConnectedOrWF(Application.RunMode.LIVE)) {
                            continue;
                        }
                        disconnectable = true;
                    }
                    if (disconnectable) {
                        return null;
                    } else {
                        return "nothing to disconnect";
                    }
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.isConnectedOrWF(Application.RunMode.LIVE)
                            && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && (!bdi.getBlockDevice().isSyncing()
                                || (bdi.getBlockDevice().isPrimary() && bdi.getBlockDevice().isSyncSource())
                                   || (bdi.getOtherBlockDevInfo().getBlockDevice().isPrimary()
                                       && bdi.getBlockDevice().isSyncTarget()))) {
                            bdi.disconnect(Application.RunMode.LIVE);
                        }
                    }
                }});
        final ButtonCallback disconnectItemCallback =
              getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost())
                  .addAction(new CallbackAction() {
                          @Override
                          public void run(final Host host) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && bdi.isConnectedOrWF(Application.RunMode.LIVE)
                        && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && (!bdi.getBlockDevice().isSyncing()
                            || (bdi.getBlockDevice().isPrimary() && bdi.getBlockDevice().isSyncSource())
                               || (bdi.getOtherBlockDevInfo().getBlockDevice().isPrimary()
                                   && bdi.getBlockDevice().isSyncTarget()))) {
                        bdi.disconnect(Application.RunMode.TEST);
                    }
                }
            }});
        multiSelectionInfo.addMouseOverListener(disconnectMenu, disconnectItemCallback);
        items.add(disconnectMenu);

        /* set primary */
        final UpdatableItem setPrimaryItem =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.SetPrimary"),
                           null,
                           Tools.getString("MultiSelectionInfo.SetPrimary"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean oneSecondary = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!application.isAdvancedMode() && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        final BlockDevInfo oBdi = bdi.getOtherBlockDevInfo();
                        if (bdi.getBlockDevice().isSecondary()
                            && ((!oBdi.getBlockDevice().isPrimary() && !selectedBlockDevInfos.contains(oBdi))
                                || bdi.allowTwoPrimaries())) {
                            oneSecondary = true;
                        }
                    }
                    if (!oneSecondary) {
                        return "nothing to promote";
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.getBlockDevice().isPrimary()
                            && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            final BlockDevInfo oBdi = bdi.getOtherBlockDevInfo();
                            if (oBdi != null
                                && oBdi.getBlockDevice().isPrimary()
                                && !selectedBlockDevInfos.contains(oBdi)
                                && !bdi.allowTwoPrimaries()) {
                                continue;
                            }
                            bdi.setPrimary(Application.RunMode.LIVE);
                        }
                    }
                }});
        items.add(setPrimaryItem);

        /* set secondary */
        final UpdatableItem setSecondaryItem =
            menuFactory.createMenuItem(
                        Tools.getString("MultiSelectionInfo.SetSecondary"),
                        null,
                        Tools.getString("MultiSelectionInfo.SetSecondary"),
                        new AccessMode(Application.AccessType.OP, true),
                        new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean onePrimary = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!application.isAdvancedMode() && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isPrimary()) {
                            onePrimary = true;
                        }
                    }
                    if (!onePrimary) {
                        return "nothing to demote";
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.getBlockDevice().isPrimary()
                            && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            bdi.setSecondary(Application.RunMode.LIVE);
                        }
                    }
                }});
        items.add(setSecondaryItem);

        /* force primary */
        final UpdatableItem forcePrimaryItem =
            menuFactory.createMenuItem(
                        Tools.getString("MultiSelectionInfo.ForcePrimary"),
                        null,
                        Tools.getString("MultiSelectionInfo.ForcePrimary"),
                        new AccessMode(Application.AccessType.ADMIN, true),
                        new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean oneSecondary = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!application.isAdvancedMode() && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        final BlockDevInfo oBdi = bdi.getOtherBlockDevInfo();
                        if (bdi.getBlockDevice().isSecondary()
                            && ((!oBdi.getBlockDevice().isPrimary() && !selectedBlockDevInfos.contains(oBdi))
                                || bdi.allowTwoPrimaries())) {
                            oneSecondary = true;
                        }
                    }
                    if (!oneSecondary) {
                        return "nothing to promote";
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.getBlockDevice().isPrimary()
                            && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            final BlockDevInfo oBdi = bdi.getOtherBlockDevInfo();
                            if (oBdi != null
                                && oBdi.getBlockDevice().isPrimary()
                                && !selectedBlockDevInfos.contains(oBdi)
                                && !bdi.allowTwoPrimaries()) {
                                continue;
                            }
                            bdi.forcePrimary(Application.RunMode.LIVE);
                        }
                    }
                }});
        items.add(forcePrimaryItem);

        /* invalidate */
        final UpdatableItem invalidateItem =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.Invalidate"),
                           null,
                           Tools.getString("MultiSelectionInfo.Invalidate"),
                           new AccessMode(Application.AccessType.ADMIN, true),
                           new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean canInvalidate = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!application.isAdvancedMode() && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (bdi.getDrbdVolumeInfo().isVerifying()) {
                            continue;
                        }
                        if (selectedBlockDevInfos.contains(bdi.getOtherBlockDevInfo())) {
                            continue;
                        }
                        canInvalidate = true;
                    }
                    if (canInvalidate) {
                        return null;
                    } else {
                        return "nothing to invalidate";
                    }
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.getBlockDevice().isSyncing()
                            && !bdi.getDrbdVolumeInfo().isVerifying()
                            && !selectedBlockDevInfos.contains(bdi.getOtherBlockDevInfo())
                            && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            bdi.invalidateBD(Application.RunMode.LIVE);
                        }
                    }
                }});
        items.add(invalidateItem);

        /* resume */
        final UpdatableItem resumeSyncItem =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.ResumeSync"),
                           null,
                           Tools.getString("MultiSelectionInfo.ResumeSync"),

                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean resumable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncTarget() || bdi.getBlockDevice().isSyncSource()) {
                            continue;
                        }
                        resumable = true;
                    }
                    if (!resumable) {
                        return "nothing to resume";
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.getBlockDevice().isSyncing()
                            && !bdi.getBlockDevice().isSyncTarget()
                            && !bdi.getBlockDevice().isSyncSource()) {
                            bdi.resumeSync(Application.RunMode.LIVE);
                        }
                    }
                }});
        items.add(resumeSyncItem);

        /* pause sync */
        final UpdatableItem pauseSyncItem =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.PauseSync"),
                           null,
                           Tools.getString("MultiSelectionInfo.PauseSync"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean pausable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.getBlockDevice().isSyncTarget() && !bdi.getBlockDevice().isSyncSource()) {
                            continue;
                        }
                        pausable = true;
                    }
                    if (!pausable) {
                        return "nothing to pause";
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (bdi.getBlockDevice().isSyncTarget() || bdi.getBlockDevice().isSyncSource())) {
                            bdi.pauseSync(Application.RunMode.LIVE);
                        }
                    }
                }});
        items.add(pauseSyncItem);

        /* resize */
        final UpdatableItem resizeItem =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.Resize"),
                           null,
                           Tools.getString("MultiSelectionInfo.Resize"),
                           new AccessMode(Application.AccessType.ADMIN, true),
                           new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean resizable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (selectedBlockDevInfos.contains(bdi.getOtherBlockDevInfo())) {
                            continue;
                        }
                        resizable = true;
                    }
                    if (resizable) {
                        return null;
                    } else {
                        return "nothing to resize";
                    }
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !selectedBlockDevInfos.contains(bdi.getOtherBlockDevInfo())
                            && !bdi.getBlockDevice().isSyncing()) {
                            bdi.resizeDrbd(Application.RunMode.LIVE);
                        }
                    }
                }});
        items.add(resizeItem);

        /* discard my data */
        final UpdatableItem discardDataItem =
            menuFactory.createMenuItem(
                         Tools.getString("MultiSelectionInfo.DiscardData"),
                         null,
                         Tools.getString("MultiSelectionInfo.DiscardData"),
                         new AccessMode(Application.AccessType.ADMIN, true),
                         new AccessMode(Application.AccessType.OP, false))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    boolean discardable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!application.isAdvancedMode() && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isPrimary()) {
                            continue;
                        }
                        if (selectedBlockDevInfos.contains(bdi.getOtherBlockDevInfo())) {
                            continue;
                        }
                        discardable = true;
                    }
                    if (discardable) {
                        return null;
                    } else {
                        return "nothing to discard";
                    }
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !selectedBlockDevInfos.contains(bdi.getOtherBlockDevInfo())
                            && (application.isAdvancedMode() || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.getBlockDevice().isSyncing()
                            && !bdi.getBlockDevice().isPrimary()) {
                            bdi.discardData(Application.RunMode.LIVE);
                        }
                    }
                }});
        items.add(discardDataItem);

        /* proxy down */
        final UpdatableItem proxyDownItem =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.ProxyDown"),
                           null,
                           Tools.getString("MultiSelectionInfo.ProxyDown"),
                           new AccessMode(Application.AccessType.ADMIN, !AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final ResourceInfo dri = bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost = dri.getProxyHost(bdi.getHost(), !ResourceInfo.WIZARD);
                        if (pHost == null) {
                            return false;
                        }
                        if (pHost.isDrbdProxyUp(dri.getName())) {
                            return true;
                        }
                    }
                    return false;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final ResourceInfo dri = bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost = dri.getProxyHost(bdi.getHost(), !ResourceInfo.WIZARD);
                        if (pHost == null) {
                            return "";
                        }
                        if (!pHost.isConnected()) {
                            return Host.PROXY_NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                        }
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    final Collection<Host> hosts = new HashSet<Host>();
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final ResourceInfo dri = bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost = dri.getProxyHost(bdi.getHost(), !ResourceInfo.WIZARD);
                        if (pHost.isDrbdProxyUp(dri.getName())) {
                            DRBD.proxyDown(pHost,
                                           dri.getName(),
                                           bdi.getDrbdVolumeInfo().getName(),
                                           Application.RunMode.LIVE);
                            hosts.add(pHost);
                        }
                    }
                    for (final Host h : hosts) {
                        getBrowser().updateProxyHWInfo(h);
                    }
                }});
        items.add(proxyDownItem);

        /* proxy up */
        final UpdatableItem proxyUpItem =
            menuFactory.createMenuItem(Tools.getString("MultiSelectionInfo.ProxyUp"),
                           null,
                           Tools.getString("MultiSelectionInfo.ProxyUp"),
                           new AccessMode(Application.AccessType.ADMIN, !AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED))
                .visiblePredicate(new VisiblePredicate() {
                        @Override
                        public boolean check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final ResourceInfo dri = bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost = dri.getProxyHost(bdi.getHost(), !ResourceInfo.WIZARD);
                        if (pHost == null) {
                            return false;
                        }
                        if (!pHost.isDrbdProxyUp(dri.getName())) {
                            return true;
                        }
                    }
                    return false;
                }})
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final ResourceInfo dri = bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost = dri.getProxyHost(bdi.getHost(), !ResourceInfo.WIZARD);
                        if (pHost == null) {
                            return "";
                        }
                        if (!pHost.isConnected()) {
                            return Host.PROXY_NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                        }
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    final Collection<Host> hosts = new HashSet<Host>();
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final ResourceInfo dri = bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost = dri.getProxyHost(bdi.getHost(), !ResourceInfo.WIZARD);
                        if (!pHost.isDrbdProxyUp(dri.getName())) {
                            DRBD.proxyUp(pHost,
                                         dri.getName(),
                                         bdi.getDrbdVolumeInfo().getName(),
                                         Application.RunMode.LIVE);
                            hosts.add(pHost);
                        }
                    }
                    for (final Host h : hosts) {
                        getBrowser().updateProxyHWInfo(h);
                    }
                }});
        items.add(proxyUpItem);
        /* PV Create */
        items.add(getPVCreateItem(selectedBlockDevInfos));
        /* PV Remove */
        items.add(getPVRemoveItem(selectedBlockDevInfos));
        /* VG Create */
        items.add(getVGCreateItem(selectedBlockDevInfos));
        /* VG Remove */
        items.add(getVGRemoveItem(selectedBlockDevInfos));
        /* LV Create */
        items.addAll(getLVCreateItems(selectedBlockDevInfos));
        /* LV Remove */
        items.add(getLVRemoveItem(selectedBlockDevInfos));
    }
    
    private ClusterBrowser getBrowser() {
        return multiSelectionInfo.getBrowser();
    }
}
