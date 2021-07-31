/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package lcmc.crm.ui.resource;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import com.google.common.eventbus.Subscribe;

import lcmc.ClusterEventBus;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.SshOutput;
import lcmc.cluster.service.storage.BlockDeviceService;
import lcmc.cluster.service.storage.FileSystemService;
import lcmc.cluster.service.storage.MountPointService;
import lcmc.cluster.ui.resource.CommonDeviceInterface;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.Info;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.WidgetListener;
import lcmc.configs.DistResource;
import lcmc.crm.domain.ResourceAgent;
import lcmc.drbd.ui.resource.ResourceInfo;
import lcmc.drbd.ui.resource.VolumeInfo;
import lcmc.event.CommonBlockDevicesChangedEvent;
import lcmc.event.CommonMountPointsEvent;
import lcmc.host.domain.Host;

/**
 * This class holds info about Filesystem service. It is treated in special
 * way, so that it can use block device information and drbd devices. If
 * drbd device is selected, the drbddisk service will be added too.
 */
@Named
public class FilesystemRaInfo extends ServiceInfo {
    /** Name of the device parameter in the file system. */
    private static final String FS_RES_PARAM_DEV = "device";
    private static final String FS_RES_PARAM_DIRECTORY = "directory";
    private static final String FS_RES_PARAM_FS_TYPE = "fstype";
    private LinbitDrbdInfo linbitDrbdInfo = null;
    private DrbddiskInfo drbddiskInfo = null;
    private Widget blockDeviceParamWidget = null;
    private Widget fstypeParamWidget = null;
    private Optional<Widget> directoryParamWidget = Optional.empty();
    private boolean drbddiskIsPreferred = false;
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private WidgetFactory widgetFactory;
    @Inject
    private BlockDeviceService blockDeviceService;
    @Inject
    private MountPointService mountPointService;
    @Inject
    private ClusterEventBus clusterEventBus;
    @Inject
    private ClusterTreeMenu clusterTreeMenu;
    @Inject
    private FileSystemService fileSystemService;

    @Override
    public void init(final String name, final ResourceAgent resourceAgent, final Browser browser) {
        super.init(name, resourceAgent, browser);
        clusterEventBus.register(this);
    }

    @Override
    public void init(
            final String name,
            final ResourceAgent resourceAgent,
            final String heartbeatId,
            final Map<String, String> resourceNode,
            final Browser browser) {
        super.init(name, resourceAgent, heartbeatId, resourceNode, browser);
        clusterEventBus.register(this);
    }
    /**
     * Sets Linbit::drbd info object for this Filesystem service if it uses
     * drbd block device.
     */
    public void setLinbitDrbdInfo(final LinbitDrbdInfo linbitDrbdInfo) {
        this.linbitDrbdInfo = linbitDrbdInfo;
    }

    /**
     * Returns linbit::drbd info object that is associated with the drbd
     * device or null if it is not a drbd device.
     */
    public LinbitDrbdInfo getLinbitDrbdInfo() {
        return linbitDrbdInfo;
    }

    public void setDrbddiskInfo(final DrbddiskInfo drbddiskInfo) {
        this.drbddiskInfo = drbddiskInfo;
    }

    /**
     * Returns DrbddiskInfo object that is associated with the drbd device
     * or null if it is not a drbd device.
     */
    public DrbddiskInfo getDrbddiskInfo() {
        return drbddiskInfo;
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        final Widget wi = getWidget(FS_RES_PARAM_DEV, null);
        final List<String> incorrect = new ArrayList<>();

        if (wi == null || wi.getValue() == null) {
            incorrect.add(FS_RES_PARAM_DEV);
        }

        final Check check = new Check(incorrect, new ArrayList<>());
        check.addCheck(super.checkResourceFields(param, params));
        return check;
    }

    @Override
    public void apply(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            swingUtils.invokeAndWait(() -> {
                getApplyButton().setEnabled(false);
                getRevertButton().setEnabled(false);
            });
            getInfoPanel();
            waitForInfoPanel();
            final String dir = getComboBoxValue(FS_RES_PARAM_DIRECTORY).getValueForConfig();
            boolean confirm = false; /* confirm only once */
            for (final Host host : getBrowser().getClusterHosts()) {
                final String statCmd = DistResource.SUDO + "stat -c \"%F\" " + dir + "||true";
                final String text = statCmd.replaceAll(DistResource.SUDO, "");
                final SshOutput ret = host.captureCommandProgressIndicator(text, new ExecCommandConfig().command(statCmd));

                if (ret == null || !FS_RES_PARAM_DIRECTORY.equals(ret.getOutput().trim())) {
                    String title = Tools.getString("ClusterBrowser.CreateDir.Title");
                    String desc  = Tools.getString("ClusterBrowser.CreateDir.Description");
                    title = title.replaceAll("@DIR@", dir);
                    title = title.replaceAll("@HOST@", host.getName());
                    desc  = desc.replaceAll("@DIR@", dir);
                    desc  = desc.replaceAll("@HOST@", host.getName());
                    if (confirm || application.confirmDialog(title,
                                                       desc,
                                                       Tools.getString("ClusterBrowser.CreateDir.Yes"),
                                                       Tools.getString("ClusterBrowser.CreateDir.No"))) {
                        final String cmd = DistResource.SUDO + "/bin/mkdir " + dir;
                        final String progressText = cmd.replaceAll(DistResource.SUDO, "");
                        final SshOutput out = host.captureCommandProgressIndicator(progressText,
                                                                                   new ExecCommandConfig().command(cmd));
                        confirm = true;
                    }
                }
            }
        }
        super.apply(dcHost, runMode);
        //TODO: escape dir
    }

    /** Adds combo box listener for the parameter. */
    private void addParamComboListeners(final Widget paramWi) {
        paramWi.addListeners(new WidgetListener() {
                                 @Override
                                 public void check(final Value value) {
                                     if (fstypeParamWidget != null) {
                                         if (!(value instanceof Info)) {
                                             return;
                                         }
                                         if (value.isNothingSelected()) {
                                             return;
                                         }
                                         final String selectedValue = getParamSaved(FS_RES_PARAM_FS_TYPE).getValueForConfig();
                                         final String createdFs;
                                         if (selectedValue == null || selectedValue.isEmpty()) {
                                             final CommonDeviceInterface cdi = (CommonDeviceInterface) value;
                                             createdFs = cdi.getLastCreatedFs();
                                         } else {
                                             createdFs = selectedValue;
                                         }
                                         if (createdFs != null && !createdFs.isEmpty()) {
                                             fstypeParamWidget.setValue(new StringValue(createdFs));
                                         }
                                     }
                                 }
                             });
    }

    @Override
    protected Widget createWidget(final String param, final String prefix, final int width) {
        final Widget paramWi;
        if (FS_RES_PARAM_DEV.equals(param)) {
            final Value[] commonBlockDevInfos = getCommonBlockDevices(prefix);
            blockDeviceParamWidget = widgetFactory.createInstance(
                                   Widget.GUESS_TYPE,
                                   commonBlockDevInfos[0],
                                   commonBlockDevInfos,
                                   Widget.NO_REGEXP,
                                   width,
                                   Widget.NO_ABBRV,
                                   new AccessMode(getAccessType(param), isEnabledOnlyInAdvancedMode(param)),
                                   Widget.NO_BUTTON);
            blockDeviceParamWidget.setAlwaysEditable(true);
            paramWi = blockDeviceParamWidget;
            addParamComboListeners(paramWi);
            widgetAdd(param, prefix, paramWi);
        } else if (FS_RES_PARAM_FS_TYPE.equals(param)) {
            final Value defaultValue =
              new StringValue() {
                  @Override
                  public String getNothingSelected() {
                      return Tools.getString("ClusterBrowser.SelectFilesystem");
                  }
              };

            Value selectedValue = getPreviouslySelected(param, prefix);
            if (selectedValue == null) {
                selectedValue = getParamSaved(param);
            }
            if (selectedValue == null || selectedValue.isNothingSelected()) {
                selectedValue = defaultValue;
            }

            final Set<String> commonFileSystems = fileSystemService.getCommonFileSystems(getBrowser().getCluster());
            paramWi = widgetFactory.createInstance(
                              Widget.GUESS_TYPE,
                              selectedValue,
                              Tools.getCommonFileSystemsWithDefault(commonFileSystems, defaultValue),
                              Widget.NO_REGEXP,
                              width,
                              Widget.NO_ABBRV,
                              new AccessMode(getAccessType(param), isEnabledOnlyInAdvancedMode(param)),
                              Widget.NO_BUTTON);
            fstypeParamWidget = paramWi;

            widgetAdd(param, prefix, paramWi);
            paramWi.setEditable(false);
        } else if (FS_RES_PARAM_DIRECTORY.equals(param)) {
            final Set<String> commonMountPoints = mountPointService.getCommonMountPoints(getBrowser().getCluster());
            final Value[] items = getCommonMountPointValues(commonMountPoints);
            getResource().setPossibleChoices(param, items);
            Value selectedValue = getPreviouslySelected(param, prefix);
            if (selectedValue == null) {
                selectedValue = getParamSaved(param);
            }
            if (selectedValue == null || selectedValue.isNothingSelected()) {
                selectedValue = items[0];
            }
            final String regexp = "^.+$";
            paramWi = widgetFactory.createInstance(
                                 Widget.GUESS_TYPE,
                                 selectedValue,
                                 items,
                                 regexp,
                                 width,
                                 Widget.NO_ABBRV,
                                 new AccessMode(getAccessType(param), isEnabledOnlyInAdvancedMode(param)),
                                 Widget.NO_BUTTON);
            widgetAdd(param, prefix, paramWi);
            paramWi.setAlwaysEditable(true);
            directoryParamWidget = Optional.of(paramWi);
        } else {
            paramWi = super.createWidget(param, prefix, width);
        }
        return paramWi;
    }

    /** Returns string representation of the filesystem service. */
    @Override
    public String toString() {
        String id = getService().getId();
        if (id == null) {
            return super.toString(); /* this is for 'new Filesystem' */
        }

        final StringBuilder s = new StringBuilder(getName());
        final VolumeInfo dvi = getBrowser().getDrbdVolumeFromDev(getParamSaved(FS_RES_PARAM_DEV).getValueForConfig());
        if (dvi == null) {
            id = getParamSaved(FS_RES_PARAM_DEV).getValueForConfig();
        } else {
            id = dvi.getDrbdResourceInfo().getName();
            s.delete(0, s.length());
            s.append("Filesystem / Drbd");
        }
        if (id == null || id.isEmpty()) {
            id = Tools.getString("ClusterBrowser.ClusterBlockDevice.Unconfigured");
        }
        s.append(" (");
        s.append(id);
        s.append(')');

        return s.toString();
    }

    /** Removes the service without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Host dcHost, final Application.RunMode runMode) {
        final VolumeInfo oldDvi = getBrowser().getDrbdVolumeFromDev(
                                            getParamSaved(FS_RES_PARAM_DEV).getValueForConfig());
        super.removeMyselfNoConfirm(dcHost, runMode);
        if (oldDvi != null && Application.isLive(runMode)) {
            final Thread t = new Thread(() -> oldDvi.updateMenus(null));
            t.start();
        }
        if (runMode == Application.RunMode.LIVE) {
            clusterEventBus.unregister(this);
        }
    }

    /**
     * Adds DrbddiskInfo before the filesysteminfo is added, returns true
     * if something was added.
     */
    @Override
    void addResourceBefore(final Host dcHost, final Application.RunMode runMode) {
        if (getGroupInfo() != null) {
            // TODO: disabled for now
            return;
        }
        final VolumeInfo oldDvi = getBrowser().getDrbdVolumeFromDev(
                                            getParamSaved(FS_RES_PARAM_DEV).getValueForConfig());
        if (oldDvi != null) {
            // TODO: disabled because it does not work well at the moment.
            return;
        }
        final VolumeInfo newDvi = getBrowser().getDrbdVolumeFromDev(
                                          getComboBoxValue(FS_RES_PARAM_DEV).getValueForConfig());
        if (newDvi == null || newDvi.equals(oldDvi)) {
            return;
        }
        final boolean oldDrbddisk = getDrbddiskInfo() != null || drbddiskIsPreferred;
        if (oldDvi != null) {
            if (oldDrbddisk) {
                oldDvi.removeDrbdDisk(this, dcHost, runMode);
            } else {
                oldDvi.removeLinbitDrbd(this, dcHost, runMode);
            }
            final Thread t = new Thread(() -> oldDvi.updateMenus(null));
            t.start();
            oldDvi.getDrbdResourceInfo().setUsedByCRM(null);
//            final Thread t = new Thread(() -> oldDvi.updateMenus(null));
            if (oldDrbddisk) {
                setDrbddiskInfo(null);
            } else {
                setLinbitDrbdInfo(null);
            }
        }

        //newDvi.getDrbdResourceInfo().setUsedByCRM(true);
        //final Thread t = new Thread(new Runnable() {
        //    @Override
        //    public void run() {
        //        newDvi.updateMenus(null);
        //    }
        //});
        //t.start();
        final String fsId = getService().getId();
        if (oldDrbddisk) {
            final String drbdId = getBrowser().getFreeId(
                    getBrowser().getCrmXml().getDrbddiskResourceAgent().getServiceName(),
                    fsId);
            newDvi.addDrbdDisk(this, dcHost, drbdId, runMode);
        } else {
            final String drbdId = getBrowser().getFreeId(
                    getBrowser().getCrmXml().getLinbitDrbdResourceAgent().getServiceName(),
                    fsId);
            newDvi.addLinbitDrbd(this, dcHost, drbdId, runMode);
        }
    }

    /** Returns how much of the filesystem is used. */
    @Override
    public int getUsed() {
        if (blockDeviceParamWidget != null) {
            final Value value = blockDeviceParamWidget.getValue();
            if (value == null || value.isNothingSelected() || !(value instanceof CommonDeviceInterface)) {
                return -1;
            }
            final CommonDeviceInterface cdi = (CommonDeviceInterface) value;
            return cdi.howMuchIsUsed();
        }
        return -1;
    }

    void setDrbddiskIsPreferred(final boolean drbddiskIsPreferred) {
        this.drbddiskIsPreferred = drbddiskIsPreferred;
    }

    @Subscribe
    public void onCommonBlockDevicesChanged(final CommonBlockDevicesChangedEvent event) {
        final Value[] commonBlockDevInfos = getCommonBlockDevices(null);
        if (blockDeviceParamWidget != null) {
            final Value value = blockDeviceParamWidget.getValue();
            blockDeviceParamWidget.reloadComboBox(value, commonBlockDevInfos);
        }
    }

    @Subscribe
    public void onCommonMountPointsChanged(final CommonMountPointsEvent event) {
        if (event.getCluster() != getBrowser().getCluster()) {
            return;
        }
        final Set<String> mountPoints = event.getCommonMountPoints();
        if (directoryParamWidget.isPresent()) {
            final Value selectedValue = directoryParamWidget.get().getValue();
            final Value[] items = getCommonMountPointValues(mountPoints);
            getResource().setPossibleChoices(FS_RES_PARAM_DIRECTORY, items);
            directoryParamWidget.get().reloadComboBox(selectedValue, items);
        }
    }

    private Value[] getCommonMountPointValues(final Set<String> commonMountPoints) {
        final Value[] items = new Value[commonMountPoints.size() + 1];
        final Value defaultValue = new StringValue() {
            @Override
            public String getNothingSelected() {
                return Tools.getString("ClusterBrowser.SelectMountPoint");
            }
        };
        items[0] = defaultValue;
        int i = 1;
        for (final String c : commonMountPoints) {
            items[i] = new StringValue(c);
            i++;
        }
        return items;
    }

    private Value[] getCommonBlockDevices(final String prefix) {
        Value selectedValue = getPreviouslySelected(FS_RES_PARAM_DEV, prefix);
        if (selectedValue == null) {
            selectedValue = getParamSaved(FS_RES_PARAM_DEV);
        }
        final VolumeInfo selectedInfo = getBrowser().getDrbdVolumeFromDev(selectedValue.getValueForConfig());
        if (selectedInfo != null) {
            selectedValue = selectedInfo;
        }
        Value defaultValue = null;
        if (selectedValue.isNothingSelected()) {
            defaultValue =  new StringValue() {
                @Override
                public String getNothingSelected() {
                    return Tools.getString("ClusterBrowser.SelectBlockDevice");
                }
            };
        }
        return getCommonBlockDevInfos(defaultValue, getName());
    }

    /**
     * Returns info object of all block devices on all hosts that have the
     * same names and other attributes.
     */
    private Value[] getCommonBlockDevInfos(final Value defaultValue, final String serviceName) {
        final List<Value> list = new ArrayList<>();

        /* drbd resources */
        final Enumeration<TreeNode> drbdResources = getBrowser().getDrbdNode().children();

        if (defaultValue != null) {
            list.add(defaultValue);
        }
        for (final Info drbdRes : clusterTreeMenu.nodesToInfos(drbdResources)) {
            if (!(drbdRes instanceof ResourceInfo)) {
                continue;
            }
            final DefaultMutableTreeNode drbdResNode = drbdRes.getNode();
            if (drbdResNode != null) {
                final Enumeration<TreeNode> drbdVolumes = drbdResNode.children();
                list.addAll(clusterTreeMenu.nodesToInfos(drbdVolumes));
            }
        }

        list.addAll(blockDeviceService.getCommonBlockDevViews());
        return list.toArray(new Value[0]);
    }
}

