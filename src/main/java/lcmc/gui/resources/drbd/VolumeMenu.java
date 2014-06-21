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
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.drbd.DrbdXML;
import lcmc.data.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class VolumeMenu {
    
    private final VolumeInfo volumeInfo;

    public VolumeMenu(VolumeInfo volumeInfo) {
        this.volumeInfo = volumeInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final Application.RunMode runMode = Application.RunMode.LIVE;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
    
        final MyMenuItem connectMenu = new MyMenuItem(
            Tools.getString("ClusterBrowser.Drbd.ResourceConnect")
            + ' ' + getResourceInfo().getName(),
            null,
            Tools.getString("ClusterBrowser.Drbd.ResourceConnect.ToolTip"),
    
            Tools.getString("ClusterBrowser.Drbd.ResourceDisconnect")
            + ' ' + getResourceInfo().getName(),
            null,
            Tools.getString("ClusterBrowser.Drbd.ResourceDisconnect.ToolTip"),
            new AccessMode(Application.AccessType.OP, true),
            new AccessMode(Application.AccessType.OP, false)) {
    
            private static final long serialVersionUID = 1L;
    
            @Override
            public boolean predicate() {
                return !volumeInfo.isConnectedOrWF(runMode);
            }
    
            @Override
            public String enablePredicate() {
                if (!Tools.getApplication().isAdvancedMode()
                    && getResourceInfo().isUsedByCRM()) {
                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                }
                if (volumeInfo.isSyncing()) {
                    return VolumeInfo.IS_SYNCING_STRING;
                }
                return null;
            }
    
            @Override
            public void action() {
                final BlockDevInfo sourceBDI =
                              getBrowser().getDrbdGraph().getSource(volumeInfo);
                final BlockDevInfo destBDI =
                                getBrowser().getDrbdGraph().getDest(volumeInfo);
                if (this.getText().equals(
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
        };
        final ButtonCallback connectItemCallback =
               getBrowser().new DRBDMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                final BlockDevInfo sourceBDI =
                              getBrowser().getDrbdGraph().getSource(volumeInfo);
                final BlockDevInfo destBDI =
                                getBrowser().getDrbdGraph().getDest(volumeInfo);
                final BlockDevInfo bdi;
                if (sourceBDI.getHost() == dcHost) {
                    bdi = sourceBDI;
                } else if (destBDI.getHost() == dcHost) {
                    bdi = destBDI;
                } else {
                    return;
                }
                if (sourceBDI.isConnected(Application.RunMode.LIVE)
                    && destBDI.isConnected(Application.RunMode.LIVE)) {
                    bdi.disconnect(Application.RunMode.TEST);
                } else {
                    bdi.connect(Application.RunMode.TEST);
                }
            }
        };
        volumeInfo.addMouseOverListener(connectMenu, connectItemCallback);
        items.add(connectMenu);
    
        final UpdatableItem resumeSync = new MyMenuItem(
           Tools.getString("ClusterBrowser.Drbd.ResourceResumeSync"),
           null,
           Tools.getString("ClusterBrowser.Drbd.ResourceResumeSync.ToolTip"),
    
           Tools.getString("ClusterBrowser.Drbd.ResourcePauseSync"),
           null,
           Tools.getString("ClusterBrowser.Drbd.ResourcePauseSync.ToolTip"),
           new AccessMode(Application.AccessType.OP, false),
           new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
    
            @Override
            public boolean predicate() {
                return volumeInfo.isPausedSync();
            }
    
            @Override
            public String enablePredicate() {
                if (!volumeInfo.isSyncing()) {
                    return "it is not syncing";
                }
                return null;
            }
            @Override
            public void action() {
                final BlockDevInfo sourceBDI =
                              getBrowser().getDrbdGraph().getSource(volumeInfo);
                final BlockDevInfo destBDI =
                                getBrowser().getDrbdGraph().getDest(volumeInfo);
                if (this.getText().equals(Tools.getString(
                            "ClusterBrowser.Drbd.ResourceResumeSync"))) {
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
        };
        items.add(resumeSync);
    
        /* resolve split-brain */
        final UpdatableItem splitBrainMenu = new MyMenuItem(
                Tools.getString("ClusterBrowser.Drbd.ResolveSplitBrain"),
                null,
                Tools.getString(
                            "ClusterBrowser.Drbd.ResolveSplitBrain.ToolTip"),
                new AccessMode(Application.AccessType.OP, false),
                new AccessMode(Application.AccessType.OP, false)) {
    
            private static final long serialVersionUID = 1L;
    
            @Override
            public String enablePredicate() {
                if (volumeInfo.isSplitBrain()) {
                    return null;
                } else {
                    return "";
                }
            }
    
            @Override
            public void action() {
                volumeInfo.resolveSplitBrain();
            }
        };
        items.add(splitBrainMenu);
    
        /* start online verification */
        final UpdatableItem verifyMenu = new MyMenuItem(
                Tools.getString("ClusterBrowser.Drbd.Verify"),
                null,
                Tools.getString("ClusterBrowser.Drbd.Verify.ToolTip"),
                new AccessMode(Application.AccessType.OP, false),
                new AccessMode(Application.AccessType.OP, false)) {
    
            private static final long serialVersionUID = 1L;
    
            @Override
            public String enablePredicate() {
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
    
            @Override
            public void action() {
                volumeInfo.verify(runMode);
            }
        };
        items.add(verifyMenu);
        /* remove resource */
        final UpdatableItem removeResMenu = new MyMenuItem(
                        Tools.getString("ClusterBrowser.Drbd.RemoveEdge"),
                        ClusterBrowser.REMOVE_ICON,
                        Tools.getString(
                                "ClusterBrowser.Drbd.RemoveEdge.ToolTip"),
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                /* this resourceInfo remove myself and this calls
                   removeDrbdResource in this class, that removes the edge
                   in the graph. */
                volumeInfo.removeMyself(runMode);
            }
    
            @Override
            public String enablePredicate() {
                final DrbdXML dxml = getBrowser().getDrbdXML();
                if (!Tools.getApplication().isAdvancedMode()
                    && getResourceInfo().isUsedByCRM()) {
                    return VolumeInfo.IS_USED_BY_CRM_STRING;
                } else if (dxml.isDrbdDisabled()) {
                    return "disabled because of config";
                }
                return null;
            }
        };
        items.add(removeResMenu);
    
        /* view log */
        final UpdatableItem viewLogMenu = new MyMenuItem(
                           Tools.getString("ClusterBrowser.Drbd.ViewLogs"),
                           VolumeInfo.LOGFILE_ICON,
                           null,
                           new AccessMode(Application.AccessType.RO, false),
                           new AccessMode(Application.AccessType.RO, false)) {
    
            private static final long serialVersionUID = 1L;
    
            @Override
            public void action() {
                volumeInfo.hidePopup();
                volumeInfo.startDrbdLogsDialog();
            }
        };
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
