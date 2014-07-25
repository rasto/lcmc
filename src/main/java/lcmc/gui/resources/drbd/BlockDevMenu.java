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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Cluster;
import lcmc.model.drbd.DrbdXml;
import lcmc.model.Host;
import lcmc.model.resources.BlockDevice;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.dialog.drbd.DrbdLog;
import lcmc.gui.dialog.lvm.LVCreate;
import lcmc.gui.dialog.lvm.LVResize;
import lcmc.gui.dialog.lvm.LVSnapshot;
import lcmc.gui.dialog.lvm.VGCreate;
import lcmc.gui.dialog.lvm.VGRemove;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.DRBD;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class BlockDevMenu {
    /** Logger. */
    private static final Logger LOG =
                                   LoggerFactory.getLogger(BlockDevMenu.class);

    /** Name of the create PV menu item. */
    private static final String PV_CREATE_MENU_ITEM = "Create PV";
    /** Description. */
    private static final String PV_CREATE_MENU_DESCRIPTION =
                             "Initialize a disk or partition for use by LVM.";
    /** Name of the remove PV menu item. */
    private static final String PV_REMOVE_MENU_ITEM = "Remove PV";
    /** Description. */
    private static final String PV_REMOVE_MENU_DESCRIPTION =
                                                    "Remove a physical volume.";
    /** Name of the create VG menu item. */
    private static final String VG_CREATE_MENU_ITEM = "Create VG";
    /** Description create VG. */
    private static final String VG_CREATE_MENU_DESCRIPTION =
                                                    "Create a volume group.";
    /** Name of the remove VG menu item. */
    private static final String VG_REMOVE_MENU_ITEM = "Remove VG";
    /** Description. */
    private static final String VG_REMOVE_MENU_DESCRIPTION =
                                                      "Remove a volume group.";
    /** Name of the create menu item. */
    private static final String LV_CREATE_MENU_ITEM = "Create LV in VG ";
    /** Description create LV. */
    private static final String LV_CREATE_MENU_DESCRIPTION =
                                                    "Create a logical volume.";
    /** Name of the LV remove menu item. */
    private static final String LV_REMOVE_MENU_ITEM = "Remove LV";
    /** Description for LV remove. */
    private static final String LV_REMOVE_MENU_DESCRIPTION =
                                                    "Remove the logical volume";
    /** Name of the resize menu item. */
    private static final String LV_RESIZE_MENU_ITEM = "Resize LV";
    /** Description LVM resize. */
    private static final String LV_RESIZE_MENU_DESCRIPTION =
                                                    "Resize the logical volume";
    /** Name of the snapshot menu item. */
    private static final String LV_SNAPSHOT_MENU_ITEM = "Create LV Snapshot ";
    /** Description LV snapshot. */
    private static final String LV_SNAPSHOT_MENU_DESCRIPTION =
                                    "Create a snapshot of the logical volume.";
    
    private final BlockDevInfo blockDevInfo;
    public BlockDevMenu(BlockDevInfo blockDevInfo) {
        this.blockDevInfo = blockDevInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final Application.RunMode runMode = Application.RunMode.LIVE;
        final UpdatableItem repMenuItem = new MyMenu(
                        Tools.getString("HostBrowser.Drbd.AddDrbdResource"),
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                final DrbdXml dxml = getClusterBrowser().getDrbdXml();
                if (getDrbdVolumeInfo() != null) {
                    return "it is already a drbd resouce";
                } else if (!getHost().isConnected()) {
                    return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                } else if (!getHost().isDrbdLoaded()) {
                    return "drbd is not loaded";
                } else if (getBlockDevice().isMounted()) {
                    return "is mounted";
                } else if (getBlockDevice().isVolumeGroupOnPhysicalVolume()) {
                    return "is volume group";
                } else if (!getBlockDevice().isAvailable()) {
                    return "not available";
                } else if (dxml.isDrbdDisabled()) {
                    return "disabled because of config";
                } else {
                    final String noavail =
                                getClusterBrowser().isDrbdAvailable(getHost());
                    if (noavail != null) {
                        return "DRBD installation problem: " + noavail;
                    }
                }
                return null;
            }

            @Override
            public void updateAndWait() {
                super.updateAndWait();
                final Cluster cluster = getHost().getCluster();
                final Host[] otherHosts = cluster.getHostsArray();
                final Collection<MyMenu> hostMenus = new ArrayList<MyMenu>();
                for (final Host oHost : otherHosts) {
                    if (oHost == getHost()) {
                        continue;
                    }
                    final MyMenu hostMenu = new MyMenu(oHost.getName(),
                                                 new AccessMode(
                                                    Application.AccessType.ADMIN,
                                                    false),
                                                 new AccessMode(
                                                    Application.AccessType.OP,
                                                    false)) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public String enablePredicate() {
                            final DrbdXml dxml =
                                               getClusterBrowser().getDrbdXml();
                            if (!oHost.isConnected()) {
                                return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                            } else if (!oHost.isDrbdLoaded()) {
                                return "drbd is not loaded";
                            } else {
                                final String noavail =
                                 getClusterBrowser().isDrbdAvailable(getHost());
                                if (noavail != null) {
                                    return "DRBD installation problem: "
                                           + noavail;
                                }
                                return null;
                            }
                            //return oHost.isConnected()
                            //       && oHost.isDrbdLoaded();
                        }

                        @Override
                        public void updateAndWait() {
                            super.updateAndWait();
                            removeAll();
                            final Set<BlockDevInfo> blockDevInfos =
                                        oHost.getBrowser().getSortedBlockDevInfos();
                            final List<BlockDevInfo> blockDevInfosS =
                                                new ArrayList<BlockDevInfo>();
                            for (final BlockDevInfo oBdi : blockDevInfos) {
                                if (oBdi.getName().equals(
                                             getBlockDevice().getName())) {
                                    blockDevInfosS.add(0, oBdi);
                                } else {
                                    blockDevInfosS.add(oBdi);
                                }
                            }

                            for (final BlockDevInfo oBdi : blockDevInfosS) {
                                if (oBdi.getDrbdVolumeInfo() == null
                                    && oBdi.getBlockDevice().isAvailable()) {
                                    add(addDrbdResourceMenuItem(oBdi,
                                                                runMode));
                                }
                                if (oBdi.getName().equals(
                                            getBlockDevice().getName())) {
                                    addSeparator();
                                }
                            }
                        }
                    };
                    hostMenu.updateAndWait();
                    hostMenus.add(hostMenu);
                }
                removeAll();
                for (final MyMenu hostMenu : hostMenus) {
                    add(hostMenu);
                }
            }
        };
        items.add(repMenuItem);
        /* PV Create */
        items.add(getPVCreateItem());
        /* PV Remove */
        items.add(getPVRemoveItem());
        /* VG Create */
        items.add(getVGCreateItem());
        /* VG Remove */
        items.add(getVGRemoveItem());
        /* LV Create */
        items.add(getLVCreateItem());
        /* LV Remove */
        items.add(getLVRemoveItem());
        /* LV Resize */
        items.add(getLVResizeItem());
        /* LV Snapshot */
        items.add(getLVSnapshotItem());
        /* attach / detach */
        final MyMenuItem attachMenu =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.Detach"),
                           BlockDevInfo.NO_HARDDISK_ICON_LARGE,
                           Tools.getString("HostBrowser.Drbd.Detach.ToolTip"),

                           Tools.getString("HostBrowser.Drbd.Attach"),
                           BlockDevInfo.HARDDISK_DRBD_ICON_LARGE,
                           Tools.getString("HostBrowser.Drbd.Attach.ToolTip"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return !getBlockDevice().isDrbd()
                           || getBlockDevice().isAttached();
                }

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getApplication().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return VolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return VolumeInfo.IS_SYNCING_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    if (this.getText().equals(
                                Tools.getString("HostBrowser.Drbd.Attach"))) {
                        blockDevInfo.attach(runMode);
                    } else {
                        blockDevInfo.detach(runMode);
                    }
                }
            };
        final ClusterBrowser cb = getClusterBrowser();
        if (cb != null) {
            final ButtonCallback attachItemCallback =
                                       cb.new DRBDMenuItemCallback(getHost()) {
                @Override
                public void action(final Host host) {
                    if (blockDevInfo.isDiskless(Application.RunMode.LIVE)) {
                        blockDevInfo.attach(Application.RunMode.TEST);
                    } else {
                        blockDevInfo.detach(Application.RunMode.TEST);
                    }
                }
            };
            blockDevInfo.addMouseOverListener(attachMenu, attachItemCallback);
        }
        items.add(attachMenu);

        /* connect / disconnect */
        final MyMenuItem connectMenu =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.Disconnect"),
                           null,
                           Tools.getString("HostBrowser.Drbd.Disconnect"),
                           Tools.getString("HostBrowser.Drbd.Connect"),
                           null,
                           Tools.getString("HostBrowser.Drbd.Connect"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return blockDevInfo.isConnectedOrWF(runMode);
                }

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getApplication().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return VolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isSyncing()
                        || ((getBlockDevice().isPrimary()
                            && getBlockDevice().isSyncSource())
                            || (blockDevInfo.getOtherBlockDevInfo()
                                                  .getBlockDevice().isPrimary()
                                && getBlockDevice().isSyncTarget()))) {
                        return null;
                    } else {
                        return VolumeInfo.IS_SYNCING_STRING;
                    }
                }

                @Override
                public void action() {
                    if (this.getText().equals(
                            Tools.getString("HostBrowser.Drbd.Connect"))) {
                        blockDevInfo.connect(runMode);
                    } else {
                        blockDevInfo.disconnect(runMode);
                    }
                }
            };
        if (cb != null) {
            final ButtonCallback connectItemCallback =
                                       cb.new DRBDMenuItemCallback(getHost()) {
                @Override
                public void action(final Host host) {
                    if (blockDevInfo.isConnectedOrWF(Application.RunMode.LIVE)) {
                        blockDevInfo.disconnect(Application.RunMode.TEST);
                    } else {
                        blockDevInfo.connect(Application.RunMode.TEST);
                    }
                }
            };
            blockDevInfo.addMouseOverListener(connectMenu, connectItemCallback);
        }
        items.add(connectMenu);

        /* set primary */
        final UpdatableItem setPrimaryItem =
            new MyMenuItem(Tools.getString(
                                  "HostBrowser.Drbd.SetPrimaryOtherSecondary"),
                           null,
                           Tools.getString(
                                  "HostBrowser.Drbd.SetPrimaryOtherSecondary"),

                           Tools.getString("HostBrowser.Drbd.SetPrimary"),
                           null,
                           Tools.getString("HostBrowser.Drbd.SetPrimary"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return getBlockDevice().isSecondary()
                         && blockDevInfo.getOtherBlockDevInfo()
                                                 .getBlockDevice().isPrimary();
                }

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getApplication().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return VolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isSecondary()) {
                        return "cannot do that to the primary";
                    }
                    return null;
                }

                @Override
                public void action() {
                    final BlockDevInfo oBdi = blockDevInfo.getOtherBlockDevInfo();
                    if (oBdi != null && oBdi.getBlockDevice().isPrimary()
                        && !"yes".equals(
                            getDrbdResourceInfo().getParamSaved(
                                              BlockDevInfo.ALLOW_TWO_PRIMARIES)
                                                       .getValueForConfig())) {
                        oBdi.setSecondary(runMode);
                    }
                    blockDevInfo.setPrimary(runMode);
                }
            };
        items.add(setPrimaryItem);

        /* set secondary */
        final UpdatableItem setSecondaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetSecondary"),
                           null,
                           Tools.getString(
                                "HostBrowser.Drbd.SetSecondary.ToolTip"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getApplication().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return VolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isPrimary()) {
                        return "cannot do that to the secondary";
                    }
                    return null;
                }

                @Override
                public void action() {
                    blockDevInfo.setSecondary(runMode);
                }
            };
        //enableMenu(setSecondaryItem, false);
        items.add(setSecondaryItem);

        /* force primary */
        final UpdatableItem forcePrimaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ForcePrimary"),
                           null,
                           Tools.getString("HostBrowser.Drbd.ForcePrimary"),
                           new AccessMode(Application.AccessType.OP, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getApplication().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return VolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    blockDevInfo.forcePrimary(runMode);
                }
            };
        items.add(forcePrimaryItem);

        /* invalidate */
        final UpdatableItem invalidateItem =
            new MyMenuItem(
                   Tools.getString("HostBrowser.Drbd.Invalidate"),
                   null,
                   Tools.getString("HostBrowser.Drbd.Invalidate.ToolTip"),
                   new AccessMode(Application.AccessType.ADMIN, true),
                   new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getApplication().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return VolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return VolumeInfo.IS_SYNCING_STRING;
                    }
                    if (getDrbdVolumeInfo().isVerifying()) {
                        return VolumeInfo.IS_VERIFYING_STRING;
                    }
                    return null;
                    //return !getBlockDevice().isSyncing()
                    //       && !getDrbdVolumeInfo().isVerifying();
                }

                @Override
                public void action() {
                    blockDevInfo.invalidateBD(runMode);
                }
            };
        items.add(invalidateItem);

        /* resume / pause sync */
        final UpdatableItem resumeSyncItem =
            new MyMenuItem(
                       Tools.getString("HostBrowser.Drbd.ResumeSync"),
                       null,
                       Tools.getString("HostBrowser.Drbd.ResumeSync.ToolTip"),

                       Tools.getString("HostBrowser.Drbd.PauseSync"),
                       null,
                       Tools.getString("HostBrowser.Drbd.PauseSync.ToolTip"),
                       new AccessMode(Application.AccessType.OP, true),
                       new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return getBlockDevice().isSyncing()
                           && getBlockDevice().isPausedSync();
                }

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getApplication().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return VolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isSyncing()) {
                        return "it is not being synced";
                    }
                    return null;
                }

                @Override
                public void action() {
                    if (this.getText().equals(
                            Tools.getString("HostBrowser.Drbd.ResumeSync"))) {
                        blockDevInfo.resumeSync(runMode);
                    } else {
                        blockDevInfo.pauseSync(runMode);
                    }
                }
            };
        items.add(resumeSyncItem);

        /* resize */
        final UpdatableItem resizeItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.Resize"),
                           null,
                           Tools.getString("HostBrowser.Drbd.Resize.ToolTip"),
                           new AccessMode(Application.AccessType.ADMIN, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getApplication().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return VolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return VolumeInfo.IS_SYNCING_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    blockDevInfo.resizeDrbd(runMode);
                }
            };
        items.add(resizeItem);

        /* discard my data */
        final UpdatableItem discardDataItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.DiscardData"),
                           null,
                           Tools.getString(
                                     "HostBrowser.Drbd.DiscardData.ToolTip"),
                           new AccessMode(Application.AccessType.ADMIN, true),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getApplication().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return VolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return VolumeInfo.IS_SYNCING_STRING;
                    }
                    //if (isConnected(runMode)) { // ? TODO: check this
                    //    return "is connected";
                    //}
                    if (getBlockDevice().isPrimary()) {
                        return "cannot do that to the primary";
                    }
                    return null;
                    //return !getBlockDevice().isSyncing()
                    //       && !isConnected(runMode)
                    //       && !getBlockDevice().isPrimary();
                }

                @Override
                public void action() {
                    blockDevInfo.discardData(runMode);
                }
            };
        items.add(discardDataItem);

        /* proxy up/down */
        final UpdatableItem proxyItem =
            new MyMenuItem(Tools.getString("BlockDevInfo.Drbd.ProxyDown"),
                           null,
                           getMenuToolTip("DRBD.proxyDown"),
                           Tools.getString("BlockDevInfo.Drbd.ProxyUp"),
                           null,
                           getMenuToolTip("DRBD.proxyUp"),
                           new AccessMode(Application.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return getDrbdResourceInfo().isProxy(getHost());
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                    }
                    final ResourceInfo dri = getDrbdResourceInfo();
                    final Host pHost =
                         dri.getProxyHost(getHost(), !ResourceInfo.WIZARD);
                    if (pHost == null) {
                        return "not a proxy";
                    }
                    if (!pHost.isConnected()) {
                        return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                    }
                    if (!pHost.isDrbdProxyRunning()) {
                        return "proxy daemon is not running";
                    }
                    return null;
                }

                @Override
                public boolean predicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    final ResourceInfo dri = getDrbdResourceInfo();
                    final Host pHost =
                         dri.getProxyHost(getHost(), !ResourceInfo.WIZARD);
                    if (pHost == null) {
                        return false;
                    }
                    if (getBlockDevice().isDrbd()) {
                        return pHost.isDrbdProxyUp(
                                              getDrbdResourceInfo().getName());
                    } else {
                        return true;
                    }
                }

                @Override
                public void action() {
                    final ResourceInfo dri = getDrbdResourceInfo();
                    final Host pHost =
                         dri.getProxyHost(getHost(), !ResourceInfo.WIZARD);
                    if (pHost.isDrbdProxyUp(getDrbdResourceInfo().getName())) {
                        DRBD.proxyDown(
                                pHost,
                                getDrbdResourceInfo().getName(),
                                getDrbdVolumeInfo().getName(),
                                runMode);
                    } else {
                        DRBD.proxyUp(
                                pHost,
                                getDrbdResourceInfo().getName(),
                                getDrbdVolumeInfo().getName(),
                                runMode);
                    }
                    getClusterBrowser().updateProxyHWInfo(pHost);
                }
            };
        items.add(proxyItem);

        /* view log */
        final UpdatableItem viewDrbdLogItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewDrbdLog"),
                           BlockDevInfo.LOGFILE_ICON,
                           null,
                           new AccessMode(Application.AccessType.RO, false),
                           new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    final String device = getDrbdVolumeInfo().getDevice();
                    final DrbdLog l = new DrbdLog(getHost(), device);
                    l.showDialog();
                }
            };
        items.add(viewDrbdLogItem);

        return items;
    }
    
    private ClusterBrowser getClusterBrowser() {
        return blockDevInfo.getBrowser().getClusterBrowser();
    }

    private Host getHost() {
        return blockDevInfo.getHost();
    }

    private BlockDevice getBlockDevice() {
        return blockDevInfo.getBlockDevice();
    }

    private VolumeInfo getDrbdVolumeInfo() {
        return blockDevInfo.getDrbdVolumeInfo();
    }

    private ResourceInfo getDrbdResourceInfo() {
        return getDrbdVolumeInfo().getDrbdResourceInfo();
    }

    /** Returns 'add drbd resource' menu item. */
    private MyMenuItem addDrbdResourceMenuItem(final BlockDevInfo oBdi,
                                               final Application.RunMode runMode) {
        return new MyMenuItem(oBdi.toString(),
                              null,
                              null,
                              new AccessMode(Application.AccessType.ADMIN,
                                             false),
                              new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                final GlobalInfo globalInfo =
                              getClusterBrowser().getDrbdGraph().getDrbdInfo();
                cleanup();
                blockDevInfo.resetInfoPanel();
                blockDevInfo.setInfoPanel(null);
                oBdi.cleanup();
                oBdi.resetInfoPanel();
                oBdi.setInfoPanel(null);
                globalInfo.addDrbdVolume(blockDevInfo,
                                         oBdi,
                                         true,
                                         runMode);
            }
        };
    }

    /** Returns 'PV create' menu item. */
    private UpdatableItem getPVCreateItem() {
        return new MyMenuItem(PV_CREATE_MENU_ITEM,
                              null,
                              PV_CREATE_MENU_DESCRIPTION,
                              new AccessMode(Application.AccessType.OP, false),
                              new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                return blockDevInfo.canCreatePV();
            }

            @Override
            public String enablePredicate() {
                if (getBlockDevice().isDrbd()
                    && !getBlockDevice().isPrimary()) {
                    return "must be primary";
                }
                return null;
            }

            @Override
            public void action() {
                final boolean ret = blockDevInfo.pvCreate(Application.RunMode.LIVE);
                if (!ret) {
                    Tools.progressIndicatorFailed(
                                Tools.getString("BlockDevInfo.PVCreate.Failed",
                                                blockDevInfo.getName()));
                }
                getClusterBrowser().updateHWInfo(getHost(), Host.UPDATE_LVM);
            }
        };
    }

    /** Returns 'PV remove' menu item. */
    private UpdatableItem getPVRemoveItem() {
        return new MyMenuItem(PV_REMOVE_MENU_ITEM,
                              null,
                              PV_REMOVE_MENU_DESCRIPTION,
                              new AccessMode(Application.AccessType.OP, false),
                              new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                return blockDevInfo.canRemovePV();
            }

            @Override
            public String enablePredicate() {
                if (getBlockDevice().isDrbd()
                    && !getBlockDevice().isDrbdPhysicalVolume()) {
                    return "DRBD is on it";
                }
                return null;
            }

            @Override
            public void action() {
                final boolean ret = blockDevInfo.pvRemove(Application.RunMode.LIVE);
                if (!ret) {
                    Tools.progressIndicatorFailed(
                                Tools.getString("BlockDevInfo.PVRemove.Failed",
                                                blockDevInfo.getName()));
                }
                getClusterBrowser().updateHWInfo(getHost(), Host.UPDATE_LVM);
            }
        };
    }

    /** Returns 'vg create' menu item. */
    private UpdatableItem getVGCreateItem() {
        return new MyMenuItem(
                          VG_CREATE_MENU_ITEM,
                          null,
                          VG_CREATE_MENU_DESCRIPTION,
                          new AccessMode(Application.AccessType.OP, false),
                          new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                final BlockDevice bd;
                if (getBlockDevice().isDrbd()) {
                    if (!getBlockDevice().isPrimary()) {
                        return false;
                    }
                    bd = getBlockDevice().getDrbdBlockDevice();
                    if (bd == null) {
                        return false;
                    }
                } else {
                    bd = getBlockDevice();
                }
                return bd.isPhysicalVolume()
                       && !bd.isVolumeGroupOnPhysicalVolume();
            }

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void action() {
                final VGCreate vgCreate = new VGCreate(getHost(), blockDevInfo);
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

    /** Returns 'VG remove' menu item. */
    private UpdatableItem getVGRemoveItem() {
        return new MyMenuItem(VG_REMOVE_MENU_ITEM,
                              null,
                              VG_REMOVE_MENU_DESCRIPTION,
                              new AccessMode(Application.AccessType.OP, false),
                              new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                final BlockDevice bd;
                if (getBlockDevice().isDrbd()) {
                    if (!getBlockDevice().isPrimary()) {
                        return false;
                    }
                    bd = getBlockDevice().getDrbdBlockDevice();
                    if (bd == null) {
                        return false;
                    }
                } else {
                    bd = getBlockDevice();
                }
                return bd.isVolumeGroupOnPhysicalVolume();
            }

            @Override
            public String enablePredicate() {
                final String vg;
                final BlockDevice bd = getBlockDevice();
                final BlockDevice drbdBD = bd.getDrbdBlockDevice();
                if (drbdBD == null) {
                    vg = bd.getVolumeGroupOnPhysicalVolume();
                } else {
                    vg = drbdBD.getVolumeGroupOnPhysicalVolume();
                }
                if (getHost().getLogicalVolumesFromVolumeGroup(vg) != null) {
                    return "has LV on it";
                }
                return null;
            }

            @Override
            public void action() {
                final VGRemove vgRemove = new VGRemove(blockDevInfo);
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
    private UpdatableItem getLVCreateItem() {
        String name = LV_CREATE_MENU_ITEM;
        final String vgName = blockDevInfo.getVGName();
        if (vgName != null) {
            name += vgName;
        }

        final MyMenuItem mi = new MyMenuItem(
                           name,
                           null,
                           LV_CREATE_MENU_DESCRIPTION,
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                final String vg = blockDevInfo.getVGName();
                return vg != null
                       && !"".equals(vg)
                       && getHost().getVolumeGroupNames().contains(vg);
            }

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void action() {
                final LVCreate lvCreate = new LVCreate(
                                                 getHost(),
                                                 blockDevInfo.getVGName(),
                                                 blockDevInfo.getBlockDevice());
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
                setText1(LV_CREATE_MENU_ITEM + blockDevInfo.getVGName());
                super.updateAndWait();
            }
        };
        mi.setToolTipText(LV_CREATE_MENU_DESCRIPTION);
        return mi;
    }

    /** Returns 'LV remove' menu item. */
    private UpdatableItem getLVRemoveItem() {
        return new MyMenuItem(LV_REMOVE_MENU_ITEM,
                              null,
                              LV_REMOVE_MENU_DESCRIPTION,
                              new AccessMode(Application.AccessType.OP, false),
                              new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean predicate() {
                return true;
            }

            @Override
            public boolean visiblePredicate() {
                return blockDevInfo.isLVM();
            }

            @Override
            public String enablePredicate() {
                if (getBlockDevice().isDrbd()) {
                    return "DRBD is on it";
                }
                return null;
            }

            @Override
            public void action() {
                if (Tools.confirmDialog(
                        "Remove Logical Volume",
                        "Remove logical volume and DESTROY all the data on it?",
                        "Remove",
                        "Cancel")) {
                    final boolean ret =
                               blockDevInfo.lvRemove(Application.RunMode.LIVE);
                    if (!ret) {
                        LOG.info("Removing of logical volume failed");
                    }
                    getClusterBrowser().updateHWInfo(getHost(), Host.UPDATE_LVM);
                }
            }
        };
    }

    /** Returns 'LV remove' menu item. */
    private UpdatableItem getLVResizeItem() {
        return new MyMenuItem(LV_RESIZE_MENU_ITEM,
                              null,
                              LV_RESIZE_MENU_DESCRIPTION,
                              new AccessMode(Application.AccessType.OP, false),
                              new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean visiblePredicate() {
                return blockDevInfo.isLVM();
            }

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void action() {
                final LVResize lvmrd = new LVResize(blockDevInfo);
                while (true) {
                    lvmrd.showDialog();
                    if (lvmrd.isPressedCancelButton()) {
                        lvmrd.cancelDialog();
                        return;
                    } else if (lvmrd.isPressedFinishButton()) {
                        break;
                    }
                }
            }
        };
    }

    /** Returns 'LV snapshot' menu item. */
    private UpdatableItem getLVSnapshotItem() {
        return new MyMenuItem(LV_SNAPSHOT_MENU_ITEM,
                              null,
                              LV_SNAPSHOT_MENU_DESCRIPTION,
                              new AccessMode(Application.AccessType.OP, false),
                              new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                return blockDevInfo.isLVM();
            }

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void action() {
                final LVSnapshot lvsd = new LVSnapshot(blockDevInfo);
                while (true) {
                    lvsd.showDialog();
                    if (lvsd.isPressedCancelButton()) {
                        lvsd.cancelDialog();
                        return;
                    } else if (lvsd.isPressedFinishButton()) {
                        break;
                    }
                }
            }
        };
    }

    /** Tool tip for menu items. */
    private String getMenuToolTip(final String cmd) {
        if (getBlockDevice().isDrbd()) {
            return DRBD.getDistCommand(
                            cmd,
                            getHost(),
                            getDrbdResourceInfo().getName(),
                            getDrbdVolumeInfo().getName()).replaceAll("@.*?@", "");
        } else {
            return null;
        }
    }
}
