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
import java.util.List;

import lcmc.gui.CallbackAction;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.drbd.domain.DrbdXml;
import lcmc.host.domain.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.EnablePredicate;
import lcmc.utilities.MenuAction;
import lcmc.utilities.MenuFactory;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Predicate;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VolumeMenu {
    private VolumeInfo volumeInfo;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Application application;

    public List<UpdatableItem> getPulldownMenu(final VolumeInfo volumeInfo) {
        this.volumeInfo = volumeInfo;
        final Application.RunMode runMode = Application.RunMode.LIVE;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        final MyMenuItem connectMenu = menuFactory.createMenuItem(
                Tools.getString("ClusterBrowser.Drbd.ResourceConnect") + ' ' + getResourceInfo().getName(),
                null,
                Tools.getString("ClusterBrowser.Drbd.ResourceConnect.ToolTip"),

                Tools.getString("ClusterBrowser.Drbd.ResourceDisconnect") + ' ' + getResourceInfo().getName(),
                null,
                Tools.getString("ClusterBrowser.Drbd.ResourceDisconnect.ToolTip"),
                new AccessMode(Application.AccessType.OP, true),
                new AccessMode(Application.AccessType.OP, false))
                .predicate(new Predicate() {
                    @Override
                    public boolean check() {
                        return !volumeInfo.isConnectedOrWF(runMode);
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!application.isAdvancedMode() && getResourceInfo().isUsedByCRM()) {
                            return VolumeInfo.IS_USED_BY_CRM_STRING;
                        }
                        if (volumeInfo.isSyncing()) {
                            return VolumeInfo.IS_SYNCING_STRING;
                        }
                        return null;
                    }
                });
        connectMenu.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                final BlockDevInfo sourceBDI = getBrowser().getDrbdGraph().getSource(volumeInfo);
                final BlockDevInfo destBDI = getBrowser().getDrbdGraph().getDest(volumeInfo);
                if (connectMenu.getText().equals(
                        Tools.getString("ClusterBrowser.Drbd.ResourceConnect")
                                + ' ' + getResourceInfo().getName())) {
                    if (!destBDI.isConnectedOrWF(runMode)) {
                        destBDI.connect(runMode);
                    }
                    if (!sourceBDI.isConnectedOrWF(runMode)) {
                        sourceBDI.connect(runMode);
                    }
                } else {
                    destBDI.disconnect(runMode);
                    sourceBDI.disconnect(runMode);
                }
            }
        });
        final ButtonCallback connectItemCallback =
                getBrowser().new DRBDMenuItemCallback(null)
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                final BlockDevInfo sourceBDI = getBrowser().getDrbdGraph().getSource(volumeInfo);
                                final BlockDevInfo destBDI = getBrowser().getDrbdGraph().getDest(volumeInfo);
                                final BlockDevInfo bdi;
                                if (sourceBDI.getHost() == dcHost) {
                                    bdi = sourceBDI;
                                } else if (destBDI.getHost() == dcHost) {
                                    bdi = destBDI;
                                } else {
                                    return;
                                }
                                if (sourceBDI.isConnected(Application.RunMode.LIVE) && destBDI.isConnected(Application.RunMode.LIVE)) {
                                    bdi.disconnect(Application.RunMode.TEST);
                                } else {
                                    bdi.connect(Application.RunMode.TEST);
                                }
                            }
                        });
        volumeInfo.addMouseOverListener(connectMenu, connectItemCallback);
        items.add(connectMenu);

        final MyMenuItem resumeSync = menuFactory.createMenuItem(
                Tools.getString("ClusterBrowser.Drbd.ResourceResumeSync"),
                null,
                Tools.getString("ClusterBrowser.Drbd.ResourceResumeSync.ToolTip"),

                Tools.getString("ClusterBrowser.Drbd.ResourcePauseSync"),
                null,
                Tools.getString("ClusterBrowser.Drbd.ResourcePauseSync.ToolTip"),
                new AccessMode(Application.AccessType.OP, false),
                new AccessMode(Application.AccessType.OP, false))
                .predicate(new Predicate() {
                    @Override
                    public boolean check() {
                        return volumeInfo.isPausedSync();
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!volumeInfo.isSyncing()) {
                            return "it is not syncing";
                        }
                        return null;
                    }
                });
        resumeSync.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                final BlockDevInfo sourceBDI = getBrowser().getDrbdGraph().getSource(volumeInfo);
                final BlockDevInfo destBDI = getBrowser().getDrbdGraph().getDest(volumeInfo);
                if (resumeSync.getText().equals(Tools.getString("ClusterBrowser.Drbd.ResourceResumeSync"))) {
                    if (destBDI.getBlockDevice().isPausedSync()) {
                        destBDI.resumeSync(runMode);
                    }
                    if (sourceBDI.getBlockDevice().isPausedSync()) {
                        sourceBDI.resumeSync(runMode);
                    }
                } else {
                    sourceBDI.pauseSync(runMode);
                    destBDI.pauseSync(runMode);
                }
            }
        });
        items.add(resumeSync);
    
        /* resolve split-brain */
        final UpdatableItem splitBrainMenu = menuFactory.createMenuItem(
                Tools.getString("ClusterBrowser.Drbd.ResolveSplitBrain"),
                null,
                Tools.getString("ClusterBrowser.Drbd.ResolveSplitBrain.ToolTip"),
                new AccessMode(Application.AccessType.OP, false),
                new AccessMode(Application.AccessType.OP, false))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (volumeInfo.isSplitBrain()) {
                            return null;
                        } else {
                            return "";
                        }
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        volumeInfo.resolveSplitBrain();
                    }
                });
        items.add(splitBrainMenu);
    
        /* start online verification */
        final UpdatableItem verifyMenu = menuFactory.createMenuItem(
                Tools.getString("ClusterBrowser.Drbd.Verify"),
                null,
                Tools.getString("ClusterBrowser.Drbd.Verify.ToolTip"),
                new AccessMode(Application.AccessType.OP, false),
                new AccessMode(Application.AccessType.OP, false))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!volumeInfo.isConnected(runMode)) {
                            return "not connected";
                        }
                        if (volumeInfo.isSyncing()) {
                            return VolumeInfo.IS_SYNCING_STRING;
                        }
                        if (volumeInfo.isVerifying()) {
                            return VolumeInfo.IS_VERIFYING_STRING;
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        volumeInfo.verify(runMode);
                    }
                });
        items.add(verifyMenu);
        /* remove resource */
        final UpdatableItem removeResMenu = menuFactory.createMenuItem(
                Tools.getString("ClusterBrowser.Drbd.RemoveEdge"),
                ClusterBrowser.REMOVE_ICON,
                Tools.getString("ClusterBrowser.Drbd.RemoveEdge.ToolTip"),
                new AccessMode(Application.AccessType.ADMIN, false),
                new AccessMode(Application.AccessType.OP, false))
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        /* this resourceInfo remove myself and this calls
                           removeDrbdResource in this class, that removes the edge
                           in the graph. */
                        volumeInfo.removeMyself(runMode);
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        final DrbdXml dxml = getBrowser().getDrbdXml();
                        if (!application.isAdvancedMode() && getResourceInfo().isUsedByCRM()) {
                            return VolumeInfo.IS_USED_BY_CRM_STRING;
                        } else if (dxml.isDrbdDisabled()) {
                            return "disabled because of config";
                        }
                        return null;
                    }
                });
        items.add(removeResMenu);
    
        /* view log */
        final UpdatableItem viewLogMenu = menuFactory.createMenuItem(
                Tools.getString("ClusterBrowser.Drbd.ViewLogs"),
                VolumeInfo.LOGFILE_ICON,
                null,
                new AccessMode(Application.AccessType.RO, false),
                new AccessMode(Application.AccessType.RO, false))
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        volumeInfo.hidePopup();
                        volumeInfo.startDrbdLogsDialog();
                    }
                });
        items.add(viewLogMenu);
        return items;
    }

    private ResourceInfo getResourceInfo() {
        return volumeInfo.getDrbdResourceInfo();
    }

    private ClusterBrowser getBrowser() {
        return volumeInfo.getBrowser();
    }
}
