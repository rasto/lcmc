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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import lcmc.common.ui.Access;
import lcmc.common.ui.CallbackAction;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.cluster.domain.Cluster;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.drbd.domain.DrbdXml;
import lcmc.host.domain.Host;
import lcmc.drbd.domain.BlockDevice;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.drbd.ui.DrbdLog;
import lcmc.lvm.ui.LVCreate;
import lcmc.lvm.ui.LVResize;
import lcmc.lvm.ui.LVSnapshot;
import lcmc.lvm.ui.VGCreate;
import lcmc.lvm.ui.VGRemove;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.drbd.service.DRBD;
import lcmc.common.domain.EnablePredicate;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.domain.Predicate;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.common.domain.VisiblePredicate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BlockDevMenu {

    private final ProgressIndicator progressIndicator;
    private final MenuFactory menuFactory;
    private final Application application;
    private final Supplier<VGCreate> vgCreateProvide;
    private final Supplier<VGRemove> vgRemoveProvider;
    private final Supplier<LVCreate> lvCreateProvider;
    private final Supplier<LVResize> lvResizeProvder;
    private final Supplier<LVSnapshot> lvSnapshotProvider;
    private final Supplier<DrbdLog> drbdLogProvider;
    private final Access access;
    private static final Logger LOG = LoggerFactory.getLogger(BlockDevMenu.class);

    private static final String PV_CREATE_MENU_ITEM = "Create PV";
    private static final String PV_CREATE_MENU_DESCRIPTION = "Initialize a disk or partition for use by LVM.";
    private static final String PV_REMOVE_MENU_ITEM = "Remove PV";
    private static final String PV_REMOVE_MENU_DESCRIPTION = "Remove a physical volume.";
    private static final String VG_CREATE_MENU_ITEM = "Create VG";
    private static final String VG_CREATE_MENU_DESCRIPTION = "Create a volume group.";
    private static final String VG_REMOVE_MENU_ITEM = "Remove VG";
    private static final String VG_REMOVE_MENU_DESCRIPTION = "Remove a volume group.";
    private static final String LV_CREATE_MENU_ITEM = "Create LV in VG ";
    private static final String LV_CREATE_MENU_DESCRIPTION = "Create a logical volume.";
    private static final String LV_REMOVE_MENU_ITEM = "Remove LV";
    private static final String LV_REMOVE_MENU_DESCRIPTION = "Remove the logical volume";
    private static final String LV_RESIZE_MENU_ITEM = "Resize LV";
    private static final String LV_RESIZE_MENU_DESCRIPTION = "Resize the logical volume";
    private static final String LV_SNAPSHOT_MENU_ITEM = "Create LV Snapshot ";
    private static final String LV_SNAPSHOT_MENU_DESCRIPTION = "Create a snapshot of the logical volume.";

    private BlockDevInfo blockDevInfo;

    public List<UpdatableItem> getPulldownMenu(final BlockDevInfo blockDevInfo) {
        this.blockDevInfo = blockDevInfo;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final Application.RunMode runMode = Application.RunMode.LIVE;
        final MyMenu repMenuItem = menuFactory.createMenu(
                Tools.getString("HostBrowser.Drbd.AddDrbdResource"),
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
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
                            final String noavail = getClusterBrowser().isDrbdAvailable(getHost());
                            if (noavail != null) {
                                return "DRBD installation problem: " + noavail;
                            }
                        }
                        return null;
                    }
                });
        repMenuItem.onUpdate(new Runnable() {
            @Override
            public void run() {
                repMenuItem.updateMenuComponents();
                final Cluster cluster = getHost().getCluster();
                final Host[] otherHosts = cluster.getHostsArray();
                final Collection<MyMenu> hostMenus = new ArrayList<MyMenu>();
                for (final Host oHost : otherHosts) {
                    if (oHost == getHost()) {
                        continue;
                    }
                    final MyMenu hostMenu = menuFactory.createMenu(oHost.getName(),
                            new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                            new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                            .enablePredicate(new EnablePredicate() {
                                @Override
                                public String check() {
                                    final DrbdXml dxml = getClusterBrowser().getDrbdXml();
                                    if (!oHost.isConnected()) {
                                        return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                                    } else if (!oHost.isDrbdLoaded()) {
                                        return "drbd is not loaded";
                                    } else {
                                        final String noavail = getClusterBrowser().isDrbdAvailable(getHost());
                                        if (noavail != null) {
                                            return "DRBD installation problem: " + noavail;
                                        }
                                        return null;
                                    }
                                }
                            });
                    hostMenu.onUpdate(new Runnable() {
                        @Override
                        public void run() {
                            hostMenu.updateMenuComponents();
                            hostMenu.removeAll();
                            final Set<BlockDevInfo> blockDevInfos = oHost.getBrowser().getSortedBlockDevInfos();
                            final List<BlockDevInfo> blockDevInfosS = new ArrayList<BlockDevInfo>();
                            for (final BlockDevInfo oBdi : blockDevInfos) {
                                if (oBdi.getName().equals(getBlockDevice().getName())) {
                                    blockDevInfosS.add(0, oBdi);
                                } else {
                                    blockDevInfosS.add(oBdi);
                                }
                            }

                            for (final BlockDevInfo oBdi : blockDevInfosS) {
                                if (oBdi.getDrbdVolumeInfo() == null && oBdi.getBlockDevice().isAvailable()) {
                                    hostMenu.add(addDrbdResourceMenuItem(oBdi, runMode));
                                }
                                if (oBdi.getName().equals(getBlockDevice().getName())) {
                                    hostMenu.addSeparator();
                                }
                            }
                            hostMenu.processAccessMode();
                        }
                    });
                    hostMenu.updateAndWait();
                    hostMenus.add(hostMenu);
                }
                repMenuItem.removeAll();
                for (final MyMenu hostMenu : hostMenus) {
                    repMenuItem.add(hostMenu);
                }
                repMenuItem.processAccessMode();

            }
        });
        items.add(repMenuItem);
        items.add(getPVCreateItem());
        items.add(getPVRemoveItem());
        items.add(getVGCreateItem());
        items.add(getVGRemoveItem());
        items.add(getLVCreateItem());
        items.add(getLVRemoveItem());
        items.add(getLVResizeItem());
        items.add(getLVSnapshotItem());
        final MyMenuItem attachMenu =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.Detach"),
                        BlockDevInfo.NO_HARDDISK_ICON_LARGE,
                        Tools.getString("HostBrowser.Drbd.Detach.ToolTip"),

                        Tools.getString("HostBrowser.Drbd.Attach"),
                        BlockDevInfo.HARDDISK_DRBD_ICON_LARGE,
                        Tools.getString("HostBrowser.Drbd.Attach.ToolTip"),
                        new AccessMode(AccessMode.OP, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                return !getBlockDevice().isDrbd() || getBlockDevice().isAttached();
                            }
                        })

                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isDrbd();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                                }
                                if (!access.isAdvancedMode() && getDrbdResourceInfo().isUsedByCRM()) {
                                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                                }
                                if (getBlockDevice().isSyncing()) {
                                    return VolumeInfo.IS_SYNCING_STRING;
                                }
                                return null;
                            }
                        });
        attachMenu.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                if (attachMenu.getText().equals(Tools.getString("HostBrowser.Drbd.Attach"))) {
                    blockDevInfo.attach(runMode);
                } else {
                    blockDevInfo.detach(runMode);
                }
            }
        });
        final ClusterBrowser cb = getClusterBrowser();
        if (cb != null) {
            final ButtonCallback attachItemCallback = cb.new DRBDMenuItemCallback(getHost())
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host host) {
                            if (blockDevInfo.isDiskless(Application.RunMode.LIVE)) {
                                blockDevInfo.attach(Application.RunMode.TEST);
                            } else {
                                blockDevInfo.detach(Application.RunMode.TEST);
                            }
                        }
                    });
            blockDevInfo.addMouseOverListener(attachMenu, attachItemCallback);
        }
        items.add(attachMenu);

        /* connect / disconnect */
        final MyMenuItem connectMenu =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.Disconnect"),
                        null,
                        Tools.getString("HostBrowser.Drbd.Disconnect"),
                        Tools.getString("HostBrowser.Drbd.Connect"),
                        null,
                        Tools.getString("HostBrowser.Drbd.Connect"),
                        new AccessMode(AccessMode.OP, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                return blockDevInfo.isConnectedOrWF(runMode);
                            }
                        })
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isDrbd();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                                }
                                if (!access.isAdvancedMode() && getDrbdResourceInfo().isUsedByCRM()) {
                                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                                }
                                if (!getBlockDevice().isSyncing()
                                    || ((getBlockDevice().isPrimary()
                                         && getBlockDevice().isSyncSource())
                                         || (blockDevInfo.getOtherBlockDevInfo().getBlockDevice().isPrimary()
                                         && getBlockDevice().isSyncTarget()))) {
                                    return null;
                                } else {
                                    return VolumeInfo.IS_SYNCING_STRING;
                                }
                            }
                        });
        connectMenu.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                if (connectMenu.getText().equals(Tools.getString("HostBrowser.Drbd.Connect"))) {
                    blockDevInfo.connect(runMode);
                } else {
                    blockDevInfo.disconnect(runMode);
                }
            }
        });
        if (cb != null) {
            final ButtonCallback connectItemCallback =
                    cb.new DRBDMenuItemCallback(getHost())
                            .addAction(new CallbackAction() {
                                @Override
                                public void run(final Host host) {
                                    if (blockDevInfo.isConnectedOrWF(Application.RunMode.LIVE)) {
                                        blockDevInfo.disconnect(Application.RunMode.TEST);
                                    } else {
                                        blockDevInfo.connect(Application.RunMode.TEST);
                                    }
                                }
                            });
            blockDevInfo.addMouseOverListener(connectMenu, connectItemCallback);
        }
        items.add(connectMenu);

        /* set primary */
        final UpdatableItem setPrimaryItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.SetPrimaryOtherSecondary"),
                        null,
                        Tools.getString("HostBrowser.Drbd.SetPrimaryOtherSecondary"),

                        Tools.getString("HostBrowser.Drbd.SetPrimary"),
                        null,
                        Tools.getString("HostBrowser.Drbd.SetPrimary"),
                        new AccessMode(AccessMode.OP, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return false;
                                }
                                return getBlockDevice().isSecondary()
                                        && blockDevInfo.getOtherBlockDevInfo().getBlockDevice().isPrimary();
                            }
                        })
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isDrbd();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                                }
                                if (!access.isAdvancedMode() && getDrbdResourceInfo().isUsedByCRM()) {
                                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                                }
                                if (!getBlockDevice().isSecondary()) {
                                    return "cannot do that to the primary";
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                final BlockDevInfo oBdi = blockDevInfo.getOtherBlockDevInfo();
                                if (oBdi != null
                                        && oBdi.getBlockDevice().isPrimary()
                                        && !"yes".equals(getDrbdResourceInfo().getParamSaved(
                                        BlockDevInfo.ALLOW_TWO_PRIMARIES).getValueForConfig())) {
                                    oBdi.setSecondary(runMode);
                                }
                                blockDevInfo.setPrimary(runMode);
                            }
                        });
        items.add(setPrimaryItem);

        /* set secondary */
        final UpdatableItem setSecondaryItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.SetSecondary"),
                        null,
                        Tools.getString("HostBrowser.Drbd.SetSecondary.ToolTip"),
                        new AccessMode(AccessMode.OP, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isDrbd();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                                }
                                if (!access.isAdvancedMode() && getDrbdResourceInfo().isUsedByCRM()) {
                                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                                }
                                if (!getBlockDevice().isPrimary()) {
                                    return "cannot do that to the secondary";
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                blockDevInfo.setSecondary(runMode);
                            }
                        });
        items.add(setSecondaryItem);

        /* force primary */
        final UpdatableItem forcePrimaryItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.ForcePrimary"),
                        null,
                        Tools.getString("HostBrowser.Drbd.ForcePrimary"),
                        new AccessMode(AccessMode.OP, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isDrbd();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                                }
                                if (!access.isAdvancedMode() && getDrbdResourceInfo().isUsedByCRM()) {
                                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                blockDevInfo.forcePrimary(runMode);
                            }
                        });
        items.add(forcePrimaryItem);

        /* invalidate */
        final UpdatableItem invalidateItem =
                menuFactory.createMenuItem(
                        Tools.getString("HostBrowser.Drbd.Invalidate"),
                        null,
                        Tools.getString("HostBrowser.Drbd.Invalidate.ToolTip"),
                        new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isDrbd();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                                }
                                if (!access.isAdvancedMode() && getDrbdResourceInfo().isUsedByCRM()) {
                                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                                }
                                if (getBlockDevice().isSyncing()) {
                                    return VolumeInfo.IS_SYNCING_STRING;
                                }
                                if (getDrbdVolumeInfo().isVerifying()) {
                                    return VolumeInfo.IS_VERIFYING_STRING;
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                blockDevInfo.invalidateBD(runMode);
                            }
                        });
        items.add(invalidateItem);

        /* resume / pause sync */
        final MyMenuItem resumeSyncItem =
                menuFactory.createMenuItem(
                        Tools.getString("HostBrowser.Drbd.ResumeSync"),
                        null,
                        Tools.getString("HostBrowser.Drbd.ResumeSync.ToolTip"),

                        Tools.getString("HostBrowser.Drbd.PauseSync"),
                        null,
                        Tools.getString("HostBrowser.Drbd.PauseSync.ToolTip"),
                        new AccessMode(AccessMode.OP, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isSyncing() && getBlockDevice().isPausedSync();
                            }
                        })
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isDrbd();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                                }
                                if (!access.isAdvancedMode() && getDrbdResourceInfo().isUsedByCRM()) {
                                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                                }
                                if (!getBlockDevice().isSyncing()) {
                                    return "it is not being synced";
                                }
                                return null;
                            }
                        });
        resumeSyncItem.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                if (resumeSyncItem.getText().equals(Tools.getString("HostBrowser.Drbd.ResumeSync"))) {
                    blockDevInfo.resumeSync(runMode);
                } else {
                    blockDevInfo.pauseSync(runMode);
                }
            }
        });
        items.add(resumeSyncItem);

        /* resize */
        final UpdatableItem resizeItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.Resize"),
                        null,
                        Tools.getString("HostBrowser.Drbd.Resize.ToolTip"),
                        new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isDrbd();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                                }
                                if (!access.isAdvancedMode() && getDrbdResourceInfo().isUsedByCRM()) {
                                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                                }
                                if (getBlockDevice().isSyncing()) {
                                    return VolumeInfo.IS_SYNCING_STRING;
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                blockDevInfo.resizeDrbd(runMode);
                            }
                        });
        items.add(resizeItem);

        /* discard my data */
        final UpdatableItem discardDataItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.DiscardData"),
                        null,
                        Tools.getString("HostBrowser.Drbd.DiscardData.ToolTip"),
                        new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isDrbd();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                                }
                                if (!access.isAdvancedMode() && getDrbdResourceInfo().isUsedByCRM()) {
                                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                                }
                                if (getBlockDevice().isSyncing()) {
                                    return VolumeInfo.IS_SYNCING_STRING;
                                }
                                if (getBlockDevice().isPrimary()) {
                                    return "cannot do that to the primary";
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                blockDevInfo.discardData(runMode);
                            }
                        });
        items.add(discardDataItem);

        /* proxy up/down */
        final UpdatableItem proxyItem =
                menuFactory.createMenuItem(Tools.getString("BlockDevInfo.Drbd.ProxyDown"),
                        null,
                        getMenuToolTip("DRBD.proxyDown"),
                        Tools.getString("BlockDevInfo.Drbd.ProxyUp"),
                        null,
                        getMenuToolTip("DRBD.proxyUp"),
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return false;
                                }
                                return getDrbdResourceInfo().isProxy(getHost());
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return BlockDevInfo.NO_DRBD_RESOURCE_STRING;
                                }
                                final ResourceInfo dri = getDrbdResourceInfo();
                                final Host pHost = dri.getProxyHost(getHost(), !ResourceInfo.WIZARD);
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
                        })
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                if (!getBlockDevice().isDrbd()) {
                                    return false;
                                }
                                final ResourceInfo dri = getDrbdResourceInfo();
                                final Host pHost = dri.getProxyHost(getHost(), !ResourceInfo.WIZARD);
                                if (pHost == null) {
                                    return false;
                                }
                                if (getBlockDevice().isDrbd()) {
                                    return pHost.getHostParser().isDrbdProxyUp(getDrbdResourceInfo().getName());
                                } else {
                                    return true;
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                final ResourceInfo dri = getDrbdResourceInfo();
                                final Host pHost = dri.getProxyHost(getHost(), !ResourceInfo.WIZARD);
                                if (pHost.getHostParser().isDrbdProxyUp(getDrbdResourceInfo().getName())) {
                                    DRBD.proxyDown(pHost, getDrbdResourceInfo().getName(), getDrbdVolumeInfo().getName(), runMode);
                                } else {
                                    DRBD.proxyUp(pHost, getDrbdResourceInfo().getName(), getDrbdVolumeInfo().getName(), runMode);
                                }
                                getClusterBrowser().updateProxyHWInfo(pHost);
                            }
                        });
        items.add(proxyItem);

        /* view log */
        final UpdatableItem viewDrbdLogItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.ViewDrbdLog"),
                        BlockDevInfo.LOGFILE_ICON,
                        null,
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getBlockDevice().isDrbd();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                final String device = getDrbdVolumeInfo().getDevice();
                                final DrbdLog drbdLog = drbdLogProvider.get();
                                drbdLog.init(getHost(), device);
                                drbdLog.showDialog();
                            }
                        });
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

    /**
     * Returns 'add drbd resource' menu item.
     */
    private MyMenuItem addDrbdResourceMenuItem(final BlockDevInfo oBdi, final Application.RunMode runMode) {
        final MyMenuItem drbdResourceMenuItem = menuFactory.createMenuItem(oBdi.toString(),
                null,
                null,
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL));
        drbdResourceMenuItem.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                drbdResourceMenuItem.cleanup();
                blockDevInfo.resetInfoPanel();
                blockDevInfo.setInfoPanel(null);
                oBdi.cleanup();
                oBdi.resetInfoPanel();
                oBdi.setInfoPanel(null);
                getClusterBrowser().getGlobalInfo().addDrbdVolume(blockDevInfo, oBdi, true, runMode);
            }
        });
        return drbdResourceMenuItem;
    }

    private UpdatableItem getPVCreateItem() {
        return menuFactory.createMenuItem(PV_CREATE_MENU_ITEM,
                null,
                PV_CREATE_MENU_DESCRIPTION,
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        return blockDevInfo.canCreatePV();
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (getBlockDevice().isDrbd() && !getBlockDevice().isPrimary()) {
                            return "must be primary";
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final boolean ret = blockDevInfo.pvCreate(Application.RunMode.LIVE);
                        if (!ret) {
                            progressIndicator.progressIndicatorFailed(Tools.getString("BlockDevInfo.PVCreate.Failed",
                                    blockDevInfo.getName()));
                        }
                        getClusterBrowser().updateHWInfo(getHost(), Host.UPDATE_LVM);
                    }
                });
    }

    private UpdatableItem getPVRemoveItem() {
        return menuFactory.createMenuItem(PV_REMOVE_MENU_ITEM,
                null,
                PV_REMOVE_MENU_DESCRIPTION,
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        return blockDevInfo.canRemovePV();
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (getBlockDevice().isDrbd()
                                && !getBlockDevice().isDrbdPhysicalVolume()) {
                            return "DRBD is on it";
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final boolean ret = blockDevInfo.pvRemove(Application.RunMode.LIVE);
                        if (!ret) {
                            progressIndicator.progressIndicatorFailed(
                                    Tools.getString("BlockDevInfo.PVRemove.Failed", blockDevInfo.getName()));
                        }
                        getClusterBrowser().updateHWInfo(getHost(), Host.UPDATE_LVM);
                    }
                });
    }

    private UpdatableItem getVGCreateItem() {
        return menuFactory.createMenuItem(
                VG_CREATE_MENU_ITEM,
                null,
                VG_CREATE_MENU_DESCRIPTION,
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
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
                        return bd.isPhysicalVolume() && !bd.isVolumeGroupOnPhysicalVolume();
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final VGCreate vgCreate = vgCreateProvide.get();
                        vgCreate.init(getHost(), blockDevInfo);
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

    private UpdatableItem getVGRemoveItem() {
        return menuFactory.createMenuItem(VG_REMOVE_MENU_ITEM,
                null,
                VG_REMOVE_MENU_DESCRIPTION,
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
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
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        final String vg;
                        final BlockDevice bd = getBlockDevice();
                        final BlockDevice drbdBD = bd.getDrbdBlockDevice();
                        if (drbdBD == null) {
                            vg = bd.getVgOnPhysicalVolume();
                        } else {
                            vg = drbdBD.getVgOnPhysicalVolume();
                        }
                        if (getHost().getHostParser().getLogicalVolumesFromVolumeGroup(vg) != null) {
                            return "has LV on it";
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final VGRemove vgRemove = vgRemoveProvider.get();
                        vgRemove.init(blockDevInfo);
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

    private UpdatableItem getLVCreateItem() {
        String name = LV_CREATE_MENU_ITEM;
        final String vgName = blockDevInfo.getVGName();
        if (vgName != null) {
            name += vgName;
        }

        final MyMenuItem mi = menuFactory.createMenuItem(
                name,
                null,
                LV_CREATE_MENU_DESCRIPTION,
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        final String vg = blockDevInfo.getVGName();
                        return vg != null && !"".equals(vg) && getHost().getHostParser().getVolumeGroupNames().contains(vg);
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final LVCreate lvCreate = lvCreateProvider.get();
                        lvCreate.init(getHost(), blockDevInfo.getVGName(), blockDevInfo.getBlockDevice());
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
                mi.setText1(LV_CREATE_MENU_ITEM + blockDevInfo.getVGName());
            }
        });
        mi.setToolTipText(LV_CREATE_MENU_DESCRIPTION);
        return mi;
    }

    private UpdatableItem getLVRemoveItem() {
        return menuFactory.createMenuItem(LV_REMOVE_MENU_ITEM,
                null,
                LV_REMOVE_MENU_DESCRIPTION,
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .predicate(new Predicate() {
                    @Override
                    public boolean check() {
                        return true;
                    }
                })
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        return blockDevInfo.isLVM();
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (getBlockDevice().isDrbd()) {
                            return "DRBD is on it";
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        if (application.confirmDialog(
                                "Remove Logical Volume",
                                "Remove logical volume and DESTROY all the data on it?",
                                "Remove",
                                "Cancel")) {
                            final boolean ret = blockDevInfo.lvRemove(Application.RunMode.LIVE);
                            if (!ret) {
                                LOG.info("Removing of logical volume failed");
                            }
                            getClusterBrowser().updateHWInfo(getHost(), Host.UPDATE_LVM);
                        }
                    }
                });
    }

    private UpdatableItem getLVResizeItem() {
        return menuFactory.createMenuItem(LV_RESIZE_MENU_ITEM,
                null,
                LV_RESIZE_MENU_DESCRIPTION,
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        return blockDevInfo.isLVM();
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final LVResize lvResize = lvResizeProvder.get();
                        lvResize.init(blockDevInfo);
                        while (true) {
                            lvResize.showDialog();
                            if (lvResize.isPressedCancelButton()) {
                                lvResize.cancelDialog();
                                return;
                            } else if (lvResize.isPressedFinishButton()) {
                                break;
                            }
                        }
                    }
                });
    }

    private UpdatableItem getLVSnapshotItem() {
        return menuFactory.createMenuItem(LV_SNAPSHOT_MENU_ITEM,
                null,
                LV_SNAPSHOT_MENU_DESCRIPTION,
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(new VisiblePredicate() {
                    @Override
                    public boolean check() {
                        return blockDevInfo.isLVM();
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final LVSnapshot lvSnapshot = lvSnapshotProvider.get();
                        lvSnapshot.init(blockDevInfo);
                        while (true) {
                            lvSnapshot.showDialog();
                            if (lvSnapshot.isPressedCancelButton()) {
                                lvSnapshot.cancelDialog();
                                return;
                            } else if (lvSnapshot.isPressedFinishButton()) {
                                break;
                            }
                        }
                    }
                });
    }

    private String getMenuToolTip(final String cmd) {
        if (getBlockDevice().isDrbd()) {
            return DRBD.getDistCommand(cmd,
                                       getHost(),
                                       getDrbdResourceInfo().getName(),
                                       getDrbdVolumeInfo().getName()).replaceAll("@.*?@", "");
        } else {
            return null;
        }
    }
}
