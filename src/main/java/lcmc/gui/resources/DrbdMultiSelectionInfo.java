/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2011-2012, Rasto Levrinc
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
package lcmc.gui.resources;

import lcmc.data.*;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.data.resources.BlockDevice;
import lcmc.utilities.Tools;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.CRM;
import lcmc.utilities.DRBD;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import javax.swing.JMenuBar;
import javax.swing.AbstractButton;
import javax.swing.JColorChooser;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import lcmc.gui.dialog.lvm.LVCreate;
import lcmc.gui.dialog.lvm.VGCreate;
import lcmc.gui.dialog.lvm.VGRemove;
import lcmc.utilities.ComponentWithTest;

/**
 * This class provides menus for block device and host multi selection.
 */
public final class DrbdMultiSelectionInfo extends EditableInfo {
    /** All selected objects. */
    private final List<Info> selectedInfos;

    private static final String LV_CREATE_MENU_ITEM =
                            Tools.getString("DrbdMultiSelectionInfo.LVCreate");

    /** Prepares a new {@code DrbdMultiSelectionInfo} object. */
    public DrbdMultiSelectionInfo(final List<Info> selectedInfos,
                                  final Browser browser) {
        super("selection", browser);
        this.selectedInfos = selectedInfos;
    }

    @Override
    protected String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /** @see EditableInfo#getInfo() */
    @Override
    public String getInfo() {
        final StringBuilder s = new StringBuilder(80);
        s.append(Tools.getString("DrbdMultiSelectionInfo.Selection"));
        for (final Info si : selectedInfos) {
            s.append(si);
            s.append("<br />");
        }
        return s.toString();
    }

    /** Create menu items for selected hosts. */
    private void createSelectedHostsPopup(
                                    final List<HostDrbdInfo> selectedHostInfos,
                                    final Collection<UpdatableItem> items) {
        /* load drbd */
        final UpdatableItem loadItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.LoadDrbd"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.LoadDrbd"),
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (hi.getHost().isConnected()) {
                            if (hi.getHost().isDrbdLoaded()) {
                                return "already loaded";
                            }
                        } else {
                            return Host.NOT_CONNECTED_STRING;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.load(hi.getHost(), CRM.LIVE);
                    }
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost(),
                                                  !Host.UPDATE_LVM);
                    }
                }
            };
        items.add(loadItem);

        /* load DRBD config / adjust all */
        final MyMenuItem adjustAllItem =
            new MyMenuItem(
                   Tools.getString("DrbdMultiSelectionInfo.AdjustAllDrbd"),
                   null,
                   Tools.getString("DrbdMultiSelectionInfo.AdjustAllDrbd"),
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (!hi.getHost().isConnected()) {
                            return Host.NOT_CONNECTED_STRING;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.adjustApply(hi.getHost(),
                                         DRBD.ALL,
                                         null,
                                         CRM.LIVE);
                    }
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost(),
                                                  !Host.UPDATE_LVM);
                    }
                }
            };
        items.add(adjustAllItem);
        final ButtonCallback adjustAllItemCallback =
               getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final HostDrbdInfo hi : selectedHostInfos) {
                    DRBD.adjustApply(hi.getHost(),
                                     DRBD.ALL,
                                     null,
                                     CRM.TESTONLY);
                }
            }
        };
        addMouseOverListener(adjustAllItem, adjustAllItemCallback);

        /* start drbd */
        final MyMenuItem upAllItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.UpAll"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.UpAll"),
                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (!hi.getHost().isDrbdStatus()) {
                            return HostDrbdInfo.NO_DRBD_STATUS_STRING;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.up(hi.getHost(), DRBD.ALL, null, CRM.LIVE);
                    }
                }
            };
        items.add(upAllItem);
        final ButtonCallback upAllItemCallback =
             getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final HostDrbdInfo hi : selectedHostInfos) {
                    DRBD.up(hi.getHost(), DRBD.ALL, null, CRM.TESTONLY);
                }
            }
        };
        addMouseOverListener(upAllItem, upAllItemCallback);

        /* stop drbd proxy with init script */
        final UpdatableItem stopProxyItem =
            new MyMenuItem(
                        Tools.getString("DrbdMultiSelectionInfo.HostStopProxy"),
                        null,
                        Tools.getString("DrbdMultiSelectionInfo.HostStopProxy"),
                        new AccessMode(Application.AccessType.OP, false),
                        new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (hi.getHost().isDrbdProxyRunning()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void action() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.stopProxy(hi.getHost(), CRM.LIVE);
                    }
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost(),
                                                  !Host.UPDATE_LVM);
                    }
                }
            };
        items.add(stopProxyItem);

        /* start drbd proxy with init script */
        final UpdatableItem startProxyItem =
            new MyMenuItem(
                      Tools.getString("DrbdMultiSelectionInfo.HostStartProxy"),
                      null,
                      Tools.getString("DrbdMultiSelectionInfo.HostStartProxy"),
                      new AccessMode(Application.AccessType.OP, false),
                      new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (!hi.getHost().isDrbdProxyRunning()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void action() {
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        DRBD.startProxy(hi.getHost(), CRM.LIVE);
                    }
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost(),
                                                  !Host.UPDATE_LVM);
                    }
                }
            };
        items.add(startProxyItem);

        /* change host color */
        final UpdatableItem changeHostColorItem =
            new MyMenuItem(
                    Tools.getString("DrbdMultiSelectionInfo.ChangeHostColor"),
                    null,
                    "",
                    new AccessMode(Application.AccessType.RO, false),
                    new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final Host firstHost = selectedHostInfos.get(0).getHost();
                    final Color newColor = JColorChooser.showDialog(
                                            Tools.getGUIData().getMainFrame(),
                                            "Choose " + selectedHostInfos
                                            + " color",
                                            firstHost.getPmColors()[0]);
                    for (final HostDrbdInfo hi : selectedHostInfos) {
                        if (newColor != null) {
                            hi.getHost().setSavedColor(newColor);
                        }
                    }
                }
            };
        items.add(changeHostColorItem);
    }

    /** Returns 'PV create' menu item. */
    private UpdatableItem getPVCreateItem(
                              final Iterable<BlockDevInfo> selectedBlockDevInfos) {
        return new MyMenuItem(
                    Tools.getString("DrbdMultiSelectionInfo.PVCreate"),
                    null,
                    Tools.getString("DrbdMultiSelectionInfo.PVCreate.ToolTip"),
                    new AccessMode(Application.AccessType.OP, false),
                    new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.canCreatePV()
                        && (!bdi.getBlockDevice().isDrbd()
                            || bdi.getBlockDevice().isPrimary())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void action() {
                /* at least one must be true */
                final Collection<Host> hosts = new HashSet<Host>();
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.canCreatePV()
                        && (!bdi.getBlockDevice().isDrbd()
                            || bdi.getBlockDevice().isPrimary())) {
                        final boolean ret = bdi.pvCreate(DRBD.LIVE);
                        if (!ret) {
                            Tools.progressIndicatorFailed(
                                Tools.getString("BlockDevInfo.PVCreate.Failed",
                                                bdi.getName()));
                        }
                        hosts.add(bdi.getHost());
                    }
                }
                for (final Host h : hosts) {
                    h.getBrowser().getClusterBrowser().updateHWInfo(
                                                            h,
                                                            Host.UPDATE_LVM);
                }
            }
        };
    }

    /** Returns 'PV remove' menu item. */
    private UpdatableItem getPVRemoveItem(
                              final Iterable<BlockDevInfo> selectedBlockDevInfos) {
        return new MyMenuItem(
                    Tools.getString("DrbdMultiSelectionInfo.PVRemove"),
                    null,
                    Tools.getString("DrbdMultiSelectionInfo.PVRemove.ToolTip"),
                    new AccessMode(Application.AccessType.OP, false),
                    new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                /* at least one must be true */
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.canRemovePV()
                        && (!bdi.getBlockDevice().isDrbd()
                            || !bdi.getBlockDevice().isDrbdPhysicalVolume())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void action() {
                final Collection<Host> hosts = new HashSet<Host>();
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.canRemovePV()
                        && (!bdi.getBlockDevice().isDrbd()
                            || !bdi.getBlockDevice().isDrbdPhysicalVolume())) {
                        final boolean ret = bdi.pvRemove(DRBD.LIVE);
                        if (!ret) {
                            Tools.progressIndicatorFailed(
                                Tools.getString("BlockDevInfo.PVRemove.Failed",
                                                bdi.getName()));
                        }
                        hosts.add(bdi.getHost());
                    }
                }
                for (final Host h : hosts) {
                    h.getBrowser().getClusterBrowser().updateHWInfo(
                                                            h,
                                                            Host.UPDATE_LVM);
                }
            }
        };
    }

    /** Returns 'vg create' menu item. */
    private UpdatableItem getVGCreateItem(
                              final List<BlockDevInfo> selectedBlockDevInfos) {
        return new MyMenuItem(
                  Tools.getString("DrbdMultiSelectionInfo.VGCreate"),
                  null,
                  Tools.getString("DrbdMultiSelectionInfo.VGCreate.ToolTip"),
                  new AccessMode(Application.AccessType.OP, false),
                  new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
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
                    if (!bd.isPhysicalVolume()
                        || bd.isVolumeGroupOnPhysicalVolume()) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void action() {
                final VGCreate vgCreate = new VGCreate(
                                        selectedBlockDevInfos.get(0).getHost(),
                                        selectedBlockDevInfos);
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
    }

    /** Returns 'vg remove' menu item. */
    private UpdatableItem getVGRemoveItem(
                              final Collection<BlockDevInfo> selectedBlockDevInfos) {
        return new MyMenuItem(
                  Tools.getString("DrbdMultiSelectionInfo.VGRemove"),
                  null,
                  Tools.getString("DrbdMultiSelectionInfo.VGRemove.ToolTip"),
                  new AccessMode(Application.AccessType.OP, false),
                  new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
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

            @Override
            public void action() {
                final List<BlockDevInfo> canRemove =
                                                 new ArrayList<BlockDevInfo>();
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.canRemoveVG()) {
                        canRemove.add(bdi);
                    }
                }
                final VGRemove vgRemove = new VGRemove(canRemove);
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
        };
    }

    /** Returns 'lv create' menu item. */
    private Collection<MyMenuItem> getLVCreateItems(
                              final Iterable<BlockDevInfo> selectedBlockDevInfos) {
        final Map<String, Set<BlockDevInfo>> vgs =
                                new LinkedHashMap<String, Set<BlockDevInfo>>();
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
        for (final Map.Entry<String, Set<BlockDevInfo>> entry
                                                            : vgs.entrySet()) {
            final String vgName = entry.getKey();
            final Set<BlockDevInfo> bdis = entry.getValue();
            String name = LV_CREATE_MENU_ITEM;
            if (vgName != null) {
                name += vgName;
            }

            final MyMenuItem mi = new MyMenuItem(
                    name,
                    null,
                    Tools.getString("DrbdMultiSelectionInfo.LVCreate.ToolTip"),
                    new AccessMode(Application.AccessType.OP, false),
                    new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : bdis) {
                        final String vg = bdi.getVGName();
                        if (vg != null
                            && !vg.isEmpty()
                            && bdi.getHost().getVolumeGroupNames()
                                            .contains(vg)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void action() {
                    final LVCreate lvCreate = new LVCreate(
                                           bdis,
                                           bdis.iterator().next().getVGName());
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
                    setText1(LV_CREATE_MENU_ITEM
                             + bdis.iterator().next().getVGName());
                    super.updateAndWait();
                }
            };
            mis.add(mi);
        }
        return mis;
    }

    /** Returns 'LV remove' menu item. */
    private UpdatableItem getLVRemoveItem(
                              final Iterable<BlockDevInfo> selectedBlockDevInfos) {
        return new MyMenuItem(
                    Tools.getString("DrbdMultiSelectionInfo.LVRemove"),
                    null,
                    Tools.getString("DrbdMultiSelectionInfo.LVRemove.ToolTip"),
                    new AccessMode(Application.AccessType.OP, false),
                    new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.isLVM() && !bdi.getBlockDevice().isDrbd()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void action() {
                final Collection<Host> selectedHosts = new HashSet<Host>();
                final Collection<String> bdNames = new ArrayList<String>();
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    bdNames.add(bdi.getName());
                    selectedHosts.add(bdi.getHost());
                }
                if (Tools.confirmDialog(
                        Tools.getString(
                            "DrbdMultiSelectionInfo.LVRemove.Confirm.Title"),
                        Tools.getString(
                            "DrbdMultiSelectionInfo.LVRemove.Confirm.Desc",
                            Tools.join(", ", bdNames)),
                        Tools.getString(
                            "DrbdMultiSelectionInfo.LVRemove.Confirm.Remove"),
                        Tools.getString(
                            "DrbdMultiSelectionInfo.LVRemove.Confirm.Cancel")
                        )) {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        bdi.lvRemove(CRM.LIVE);
                    }
                    for (final Host h : selectedHosts) {
                        getBrowser().updateHWInfo(h, Host.UPDATE_LVM);
                    }
                }
            }
        };
    }

    /** Create menu items for selected block devices. */
    private void createSelectedBlockDevPopup(
                                 final List<BlockDevInfo> selectedBlockDevInfos,
                                 final Collection<UpdatableItem> items) {
        /* detach */
        final MyMenuItem detachMenu =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Detach"),
                           BlockDevInfo.NO_HARDDISK_ICON_LARGE,
                           Tools.getString("DrbdMultiSelectionInfo.Detach"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    boolean oneAttached = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.isDiskless(CRM.LIVE)) {
                            oneAttached = true;
                        }
                    }
                    return oneAttached;
                }

                @Override
                public String enablePredicate() {
                    boolean detachable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getApplication().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
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

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (Tools.getApplication().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.getBlockDevice().isSyncing()
                            && !bdi.isDiskless(CRM.LIVE)) {
                            bdi.detach(CRM.LIVE);
                        }
                    }
                }
            };
        final ButtonCallback detachItemCallback =
              getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && (Tools.getApplication().isAdvancedMode()
                            || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && !bdi.getBlockDevice().isSyncing()
                        && !bdi.isDiskless(CRM.LIVE)) {
                        bdi.detach(CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(detachMenu, detachItemCallback);
        items.add(detachMenu);

        /* attach */
        final MyMenuItem attachMenu =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Attach"),
                           BlockDevInfo.HARDDISK_DRBD_ICON_LARGE,
                           Tools.getString("DrbdMultiSelectionInfo.Attach"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    boolean oneDetached = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (bdi.isDiskless(CRM.LIVE)) {
                            oneDetached = true;
                        }
                    }
                    return oneDetached;
                }

                @Override
                public String enablePredicate() {
                    boolean attachable = true;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getApplication().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
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
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (Tools.getApplication().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.getBlockDevice().isSyncing()
                            && bdi.isDiskless(CRM.LIVE)) {
                            bdi.attach(CRM.LIVE);
                        }
                    }
                }
            };
        final ButtonCallback attachItemCallback =
             getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && (Tools.getApplication().isAdvancedMode()
                            || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && !bdi.getBlockDevice().isSyncing()
                        && bdi.isDiskless(CRM.LIVE)) {
                        bdi.attach(CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(attachMenu, attachItemCallback);
        items.add(attachMenu);

        /* connect */
        final MyMenuItem connectMenu =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Connect"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.Connect"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    boolean oneDisconnected = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.isConnectedOrWF(CRM.LIVE)) {
                            oneDisconnected = true;
                        }
                    }
                    return oneDisconnected;
                }

                @Override
                public String enablePredicate() {
                    boolean connectable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getApplication().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.isConnectedOrWF(CRM.LIVE)) {
                            continue;
                        }
                        connectable = true;
                    }
                    if (connectable) {
                        return null;
                    } else {
                        return "nothing to connect";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (Tools.getApplication().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.isConnectedOrWF(CRM.LIVE)) {
                            bdi.connect(CRM.LIVE);
                        }
                    }
                }
            };
        final ButtonCallback connectItemCallback =
              getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && (Tools.getApplication().isAdvancedMode()
                            || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && !bdi.isConnectedOrWF(CRM.LIVE)) {
                        bdi.connect(CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(connectMenu, connectItemCallback);
        items.add(connectMenu);

        /* disconnect */
        final MyMenuItem disconnectMenu =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Disconnect"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.Disconnect"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    boolean oneConnected = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (bdi.isConnectedOrWF(CRM.LIVE)) {
                            oneConnected = true;
                        }
                    }
                    return oneConnected;
                }

                @Override
                public String enablePredicate() {
                    boolean disconnectable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getApplication().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()
                            && ((bdi.getBlockDevice().isPrimary()
                                 && bdi.getBlockDevice().isSyncTarget())
                                || (bdi.getOtherBlockDevInfo().getBlockDevice().
                                                                    isPrimary()
                                    && bdi.getBlockDevice().isSyncSource()))) {
                            continue;
                        }
                        if (!bdi.isConnectedOrWF(CRM.LIVE)) {
                            continue;
                        }
                        disconnectable = true;
                    }
                    if (disconnectable) {
                        return null;
                    } else {
                        return "nothing to disconnect";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.isConnectedOrWF(CRM.LIVE)
                            && (Tools.getApplication().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && (!bdi.getBlockDevice().isSyncing()
                                || (bdi.getBlockDevice().isPrimary()
                                    && bdi.getBlockDevice().isSyncSource())
                                   || (bdi.getOtherBlockDevInfo()
                                          .getBlockDevice().isPrimary()
                                       && bdi.getBlockDevice().isSyncTarget()))) {
                            bdi.disconnect(CRM.LIVE);
                        }
                    }
                }
            };
        final ButtonCallback disconnectItemCallback =
              getBrowser().new DRBDMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                    if (bdi.getBlockDevice().isDrbd()
                        && bdi.isConnectedOrWF(CRM.LIVE)
                        && (Tools.getApplication().isAdvancedMode()
                            || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                        && (!bdi.getBlockDevice().isSyncing()
                            || (bdi.getBlockDevice().isPrimary()
                                && bdi.getBlockDevice().isSyncSource())
                               || (bdi.getOtherBlockDevInfo()
                                      .getBlockDevice().isPrimary()
                                   && bdi.getBlockDevice().isSyncTarget()))) {
                        bdi.disconnect(CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(disconnectMenu, disconnectItemCallback);
        items.add(disconnectMenu);

        /* set primary */
        final UpdatableItem setPrimaryItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.SetPrimary"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.SetPrimary"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean oneSecondary = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getApplication().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        final BlockDevInfo oBdi = bdi.getOtherBlockDevInfo();
                        if (bdi.getBlockDevice().isSecondary()
                            && ((!oBdi.getBlockDevice().isPrimary()
                                 && !selectedBlockDevInfos.contains(oBdi))
                                || bdi.allowTwoPrimaries())) {
                            oneSecondary = true;
                        }
                    }
                    if (!oneSecondary) {
                        return "nothing to promote";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.getBlockDevice().isPrimary()
                            && (Tools.getApplication().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            final BlockDevInfo oBdi =
                                                    bdi.getOtherBlockDevInfo();
                            if (oBdi != null
                                && oBdi.getBlockDevice().isPrimary()
                                && !selectedBlockDevInfos.contains(oBdi)
                                && !bdi.allowTwoPrimaries()) {
                                continue;
                            }
                            bdi.setPrimary(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(setPrimaryItem);

        /* set secondary */
        final UpdatableItem setSecondaryItem =
            new MyMenuItem(
                        Tools.getString("DrbdMultiSelectionInfo.SetSecondary"),
                        null,
                        Tools.getString("DrbdMultiSelectionInfo.SetSecondary"),
                        new AccessMode(Application.AccessType.OP, true),
                        new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean onePrimary = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getApplication().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
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
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.getBlockDevice().isPrimary()
                            && (Tools.getApplication().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            bdi.setSecondary(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(setSecondaryItem);

        /* force primary */
        final UpdatableItem forcePrimaryItem =
            new MyMenuItem(
                        Tools.getString("DrbdMultiSelectionInfo.ForcePrimary"),
                        null,
                        Tools.getString("DrbdMultiSelectionInfo.ForcePrimary"),
                        new AccessMode(Application.AccessType.ADMIN, true),
                        new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean oneSecondary = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getApplication().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        final BlockDevInfo oBdi = bdi.getOtherBlockDevInfo();
                        if (bdi.getBlockDevice().isSecondary()
                            && ((!oBdi.getBlockDevice().isPrimary()
                                 && !selectedBlockDevInfos.contains(oBdi))
                                || bdi.allowTwoPrimaries())) {
                            oneSecondary = true;
                        }
                    }
                    if (!oneSecondary) {
                        return "nothing to promote";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.getBlockDevice().isPrimary()
                            && (Tools.getApplication().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            final BlockDevInfo oBdi =
                                                    bdi.getOtherBlockDevInfo();
                            if (oBdi != null
                                && oBdi.getBlockDevice().isPrimary()
                                && !selectedBlockDevInfos.contains(oBdi)
                                && !bdi.allowTwoPrimaries()) {
                                continue;
                            }
                            bdi.forcePrimary(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(forcePrimaryItem);

        /* invalidate */
        final UpdatableItem invalidateItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Invalidate"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.Invalidate"),
                           new AccessMode(Application.AccessType.ADMIN, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean canInvalidate = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getApplication().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (bdi.getDrbdVolumeInfo().isVerifying()) {
                            continue;
                        }
                        if (selectedBlockDevInfos.contains(
                                               bdi.getOtherBlockDevInfo())) {
                            continue;
                        }
                        canInvalidate = true;
                    }
                    if (canInvalidate) {
                        return null;
                    } else {
                        return "nothing to invalidate";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.getBlockDevice().isSyncing()
                            && !bdi.getDrbdVolumeInfo().isVerifying()
                            && !selectedBlockDevInfos.contains(
                                                   bdi.getOtherBlockDevInfo())
                            && (Tools.getApplication().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())) {
                            bdi.invalidateBD(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(invalidateItem);

        /* resume */
        final UpdatableItem resumeSyncItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.ResumeSync"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.ResumeSync"),

                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean resumable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncTarget()
                            || bdi.getBlockDevice().isSyncSource()) {
                            continue;
                        }
                        resumable = true;
                    }
                    if (!resumable) {
                        return "nothing to resume";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.getBlockDevice().isSyncing()
                            && !bdi.getBlockDevice().isSyncTarget()
                            && !bdi.getBlockDevice().isSyncSource()) {
                            bdi.resumeSync(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(resumeSyncItem);

        /* pause sync */
        final UpdatableItem pauseSyncItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.PauseSync"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.PauseSync"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean pausable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!bdi.getBlockDevice().isSyncTarget()
                            && !bdi.getBlockDevice().isSyncSource()) {
                            continue;
                        }
                        pausable = true;
                    }
                    if (!pausable) {
                        return "nothing to pause";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && (bdi.getBlockDevice().isSyncTarget()
                                || bdi.getBlockDevice().isSyncSource())) {
                            bdi.pauseSync(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(pauseSyncItem);

        /* resize */
        final UpdatableItem resizeItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.Resize"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.Resize"),
                           new AccessMode(Application.AccessType.ADMIN, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean resizable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (selectedBlockDevInfos.contains(
                                               bdi.getOtherBlockDevInfo())) {
                            continue;
                        }
                        resizable = true;
                    }
                    if (resizable) {
                        return null;
                    } else {
                        return "nothing to resize";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !selectedBlockDevInfos.contains(
                                                   bdi.getOtherBlockDevInfo())
                            && !bdi.getBlockDevice().isSyncing()) {
                            bdi.resizeDrbd(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(resizeItem);

        /* discard my data */
        final UpdatableItem discardDataItem =
            new MyMenuItem(
                         Tools.getString("DrbdMultiSelectionInfo.DiscardData"),
                         null,
                         Tools.getString("DrbdMultiSelectionInfo.DiscardData"),
                         new AccessMode(Application.AccessType.ADMIN, true),
                         new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    boolean discardable = false;
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        if (!Tools.getApplication().isAdvancedMode()
                            && bdi.getDrbdVolumeInfo().isUsedByCRM()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isSyncing()) {
                            continue;
                        }
                        if (bdi.getBlockDevice().isPrimary()) {
                            continue;
                        }
                        if (selectedBlockDevInfos.contains(
                                               bdi.getOtherBlockDevInfo())) {
                            continue;
                        }
                        discardable = true;
                    }
                    if (discardable) {
                        return null;
                    } else {
                        return "nothing to discard";
                    }
                }

                @Override
                public void action() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !selectedBlockDevInfos.contains(
                                                   bdi.getOtherBlockDevInfo())
                            && (Tools.getApplication().isAdvancedMode()
                                || !bdi.getDrbdVolumeInfo().isUsedByCRM())
                            && !bdi.getBlockDevice().isSyncing()
                            && !bdi.getBlockDevice().isPrimary()) {
                            bdi.discardData(CRM.LIVE);
                        }
                    }
                }
            };
        items.add(discardDataItem);

        /* proxy down */
        final UpdatableItem proxyDownItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.ProxyDown"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.ProxyDown"),
                           new AccessMode(Application.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final DrbdResourceInfo dri =
                                 bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost =
                                    dri.getProxyHost(bdi.getHost(),
                                                     !DrbdResourceInfo.WIZARD);
                        if (pHost == null) {
                            return false;
                        }
                        if (pHost.isDrbdProxyUp(dri.getName())) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final DrbdResourceInfo dri =
                                 bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost =
                                    dri.getProxyHost(bdi.getHost(),
                                                     !DrbdResourceInfo.WIZARD);
                        if (pHost == null) {
                            return "";
                        }
                        if (!pHost.isConnected()) {
                            return Host.PROXY_NOT_CONNECTED_STRING;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    final Collection<Host> hosts = new HashSet<Host>();
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final DrbdResourceInfo dri =
                                 bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost =
                                    dri.getProxyHost(bdi.getHost(),
                                                     !DrbdResourceInfo.WIZARD);
                        if (pHost.isDrbdProxyUp(dri.getName())) {
                            DRBD.proxyDown(pHost,
                                           dri.getName(),
                                           bdi.getDrbdVolumeInfo().getName(),
                                           CRM.LIVE);
                            hosts.add(pHost);
                        }
                    }
                    for (final Host h : hosts) {
                        getBrowser().updateProxyHWInfo(h);
                    }
                }
            };
        items.add(proxyDownItem);

        /* proxy up */
        final UpdatableItem proxyUpItem =
            new MyMenuItem(Tools.getString("DrbdMultiSelectionInfo.ProxyUp"),
                           null,
                           Tools.getString("DrbdMultiSelectionInfo.ProxyUp"),
                           new AccessMode(Application.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final DrbdResourceInfo dri =
                                 bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost =
                                    dri.getProxyHost(bdi.getHost(),
                                                     !DrbdResourceInfo.WIZARD);
                        if (pHost == null) {
                            return false;
                        }
                        if (!pHost.isDrbdProxyUp(dri.getName())) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final DrbdResourceInfo dri =
                                 bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost =
                                    dri.getProxyHost(bdi.getHost(),
                                                     !DrbdResourceInfo.WIZARD);
                        if (pHost == null) {
                            return "";
                        }
                        if (!pHost.isConnected()) {
                            return Host.PROXY_NOT_CONNECTED_STRING;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    final Collection<Host> hosts = new HashSet<Host>();
                    for (final BlockDevInfo bdi : selectedBlockDevInfos) {
                        if (!bdi.getBlockDevice().isDrbd()) {
                            continue;
                        }
                        final DrbdResourceInfo dri =
                                 bdi.getDrbdVolumeInfo().getDrbdResourceInfo();
                        final Host pHost =
                                    dri.getProxyHost(bdi.getHost(),
                                                     !DrbdResourceInfo.WIZARD);
                        if (!pHost.isDrbdProxyUp(dri.getName())) {
                            DRBD.proxyUp(pHost,
                                         dri.getName(),
                                         bdi.getDrbdVolumeInfo().getName(),
                                         CRM.LIVE);
                            hosts.add(pHost);
                        }
                    }
                    for (final Host h : hosts) {
                        getBrowser().updateProxyHWInfo(h);
                    }
                }
            };
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

    /** @see EditableInfo#createPopup() */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final List<BlockDevInfo> selectedBlockDevInfos =
                                                 new ArrayList<BlockDevInfo>();
        final List<HostDrbdInfo> selectedHostInfos =
                                                new ArrayList<HostDrbdInfo>();
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

    @Override
    protected ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getDrbdGraph().getGraphPanel();
    }

    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
        return false;
    }

    @Override
    protected Application.AccessType getAccessType(final String param) {
        return null;
    }

    @Override
    protected String getSection(final String param) {
        return null;
    }

    @Override
    protected boolean isRequired(final String param) {
        return false;
    }

    @Override
    protected boolean isAdvanced(final String param) {
        return false;
    }

    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    @Override
    protected boolean isInteger(final String param) {
        return false;
    }

    @Override
    protected boolean isLabel(final String param) {
        return false;
    }

    @Override
    protected boolean isTimeType(final String param) {
        return false;
    }

    @Override
    protected boolean isCheckBox(final String param) {
        return false;
    }

    @Override
    protected String getParamType(final String param) {
        return null;
    }

    @Override
    public String[] getParametersFromXML() {
        return null;
    }

    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        return null;
    }

    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        return true;
    }

    @Override
    public Value getParamDefault(final String param) {
        return null;
    }

    @Override
    protected Value getParamPreferred(final String param) {
        return null;
    }

    @Override
    protected String getParamShortDesc(final String param) {
        return null;
    }

    @Override
    protected String getParamLongDesc(final String param) {
        return null;
    }

    @Override
    public JComponent getInfoPanel() {
        final boolean abExisted = getApplyButton() != null;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;
            /**
             * Whether the whole thing should be enabled.
             */
            @Override
            public boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                return dcHost != null && !Tools.versionBeforePacemaker(dcHost);
            }
            @Override
            public void mouseOut(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getCRMGraph().stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }

            @Override
            public void mouseOver(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                component.setToolTipText(
                                        ClusterBrowser.STARTING_PTEST_TOOLTIP);
                component.setToolTipBackground(Tools.getDefaultColor(
                                   "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getCRMGraph().startTestAnimation((JComponent) component,
                                                              startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                try {
                    final ClusterStatus cs = getBrowser().getClusterStatus();
                    cs.setPtestData(null);
                    apply(dcHost, CRM.TESTONLY);
                    final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                    component.setToolTipText(ptestData.getToolTip());
                    cs.setPtestData(ptestData);
                } finally {
                    getBrowser().ptestLockRelease();
                }
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);
        /* add item listeners to the apply button. */
        if (!abExisted) {
            getApplyButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), CRM.LIVE);
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );

            getRevertButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                revert();
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
        }
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final AbstractButton serviceMenu = getActionsButton();
        buttonPanel.add(serviceMenu, BorderLayout.LINE_END);

        /* apply button */
        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);
        final String[] params = getParametersFromXML();
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                /* invoke later on purpose  */
                setApplyButtons(null, params);
            }
        });
        mainPanel.add(optionsPanel);
        mainPanel.add(super.getInfoPanel());
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        newPanel.add(new JScrollPane(mainPanel));
        /* if id textfield was changed and this id is not used,
         * enable apply button */
        infoPanelDone();
        return newPanel;
    }

    /**
     * Apply the changes to the drbd parameters.
     * not implemented
     */
    void apply(final Host dcHost, final boolean testOnly) {
    }
}
