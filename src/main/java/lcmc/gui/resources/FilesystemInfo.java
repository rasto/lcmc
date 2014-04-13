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
package lcmc.gui.resources;

import java.util.ArrayList;
import java.util.List;

import lcmc.data.ResourceAgent;
import lcmc.data.Host;
import lcmc.data.AccessMode;
import lcmc.configs.DistResource;
import lcmc.utilities.Tools;
import lcmc.utilities.SSH;
import lcmc.utilities.WidgetListener;
import lcmc.gui.resources.drbd.CommonDeviceInterface;
import lcmc.gui.resources.drbd.DrbdVolumeInfo;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.Browser;

import java.util.Map;

import lcmc.data.Application;
import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.gui.widget.Check;

/**
 * This class holds info about Filesystem service. It is treated in special
 * way, so that it can use block device information and drbd devices. If
 * drbd device is selected, the drbddisk service will be added too.
 */
public final class FilesystemInfo extends ServiceInfo {
    /** linbit::drbd service object. */
    private LinbitDrbdInfo linbitDrbdInfo = null;
    /** drbddisk service object. */
    private DrbddiskInfo drbddiskInfo = null;
    /** Block device combo box. */
    private Widget blockDeviceParamWi = null;
    /** Filesystem type combo box. */
    private Widget fstypeParamWi = null;
    /** Whether old style drbddisk is preferred. */
    private boolean drbddiskIsPreferred = false;
    /** Name of the device parameter in the file system. */
    private static final String FS_RES_PARAM_DEV = "device";

    /** Creates the FilesystemInfo object. */
    FilesystemInfo(final String name,
                   final ResourceAgent ra,
                   final Browser browser) {
        super(name, ra, browser);
    }

    /** Creates the FilesystemInfo object. */
    FilesystemInfo(final String name,
                   final ResourceAgent ra,
                   final String hbId,
                   final Map<String, String> resourceNode,
                   final Browser browser) {
        super(name, ra, hbId, resourceNode, browser);
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

    /**
     * Sets DrbddiskInfo object for this Filesystem service if it uses drbd
     * block device.
     */
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
    public Check checkResourceFields(final String param,
                                     final String[] params) {
        final Widget wi = getWidget(FS_RES_PARAM_DEV, null);
        final List<String> incorrect = new ArrayList<String>();

        if (wi == null || wi.getValue() == null) {
            incorrect.add(FS_RES_PARAM_DEV);
        }

        final Check check = new Check(incorrect, new ArrayList<String>());
        check.addCheck(super.checkResourceFields(param, params));
        return check;
    }

    /** Applies changes to the Filesystem service parameters. */
    @Override
	public void apply(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                }
            });
            getInfoPanel();
            waitForInfoPanel();
            final String dir = getComboBoxValue("directory").getValueForConfig();
            boolean confirm = false; /* confirm only once */
            for (final Host host : getBrowser().getClusterHosts()) {
                final String statCmd =
                        DistResource.SUDO + "stat -c \"%F\" " + dir + "||true";
                final SSH.SSHOutput ret =
                               Tools.execCommandProgressIndicator(
                                    host,
                                    statCmd,
                                    null,
                                    true,
                                    statCmd.replaceAll(DistResource.SUDO, ""),
                                    SSH.DEFAULT_COMMAND_TIMEOUT);

                if (ret == null
                    || !"directory".equals(ret.getOutput().trim())) {
                    String title =
                          Tools.getString("ClusterBrowser.CreateDir.Title");
                    String desc  = Tools.getString(
                                    "ClusterBrowser.CreateDir.Description");
                    title = title.replaceAll("@DIR@", dir);
                    title = title.replaceAll("@HOST@", host.getName());
                    desc  = desc.replaceAll("@DIR@", dir);
                    desc  = desc.replaceAll("@HOST@", host.getName());
                    if (confirm || Tools.confirmDialog(
                          title,
                          desc,
                          Tools.getString("ClusterBrowser.CreateDir.Yes"),
                          Tools.getString("ClusterBrowser.CreateDir.No"))) {
                        final String cmd = DistResource.SUDO
                                           + "/bin/mkdir " + dir;
                        Tools.execCommandProgressIndicator(
                                        host,
                                        cmd,
                                        null,
                                        true,
                                        cmd.replaceAll(DistResource.SUDO, ""),
                                        SSH.DEFAULT_COMMAND_TIMEOUT);
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
        paramWi.addListeners(
                    new WidgetListener() {
                        @Override
                        public void check(final Value value) {
                            if (fstypeParamWi != null) {
                                if (!(value instanceof Info)) {
                                    return;
                                }
                                if (value.isNothingSelected()) {
                                    return;
                                }
                                final String selectedValue =
                                                  getParamSaved("fstype").getValueForConfig();
                                final String createdFs;
                                if (selectedValue == null
                                    || selectedValue.isEmpty()) {
                                    final CommonDeviceInterface cdi =
                                             (CommonDeviceInterface) value;
                                    createdFs = cdi.getCreatedFs();
                                } else {
                                    createdFs = selectedValue;
                                }
                                if (createdFs != null
                                    && !createdFs.isEmpty()) {
                                    fstypeParamWi.setValue(new StringValue(createdFs));
                                }
                            }
                        }
                    });
    }

    /** Returns editable element for the parameter. */
    @Override
    protected Widget createWidget(final String param,
                                  final String prefix,
                                  final int width) {
        final Widget paramWi;
        if (FS_RES_PARAM_DEV.equals(param)) {
            Value selectedValue = getPreviouslySelected(param, prefix);
            if (selectedValue == null) {
                selectedValue = getParamSaved(param);
            }
            final DrbdVolumeInfo selectedInfo =
                            getBrowser().getDrbdVolumeFromDev(selectedValue.getValueForConfig());
            if (selectedInfo != null) {
                selectedValue = selectedInfo;
            }
            Value defaultValue = null;
            if (selectedValue.isNothingSelected()) {
                defaultValue = 
                          new StringValue() {
                              @Override
                              public String getNothingSelected() {
                                  return Tools.getString(
                                         "ClusterBrowser.SelectBlockDevice");
                              }
                          };
            }
            final Value[] commonBlockDevInfos =
                                        getCommonBlockDevInfos(defaultValue,
                                                               getName());
            blockDeviceParamWi = WidgetFactory.createInstance(
                                   Widget.GUESS_TYPE,
                                   selectedValue,
                                   commonBlockDevInfos,
                                   Widget.NO_REGEXP,
                                   width,
                                   Widget.NO_ABBRV,
                                   new AccessMode(
                                           getAccessType(param),
                                           isEnabledOnlyInAdvancedMode(param)),
                                   Widget.NO_BUTTON);
            blockDeviceParamWi.setAlwaysEditable(true);
            paramWi = blockDeviceParamWi;
            addParamComboListeners(paramWi);
            widgetAdd(param, prefix, paramWi);
        } else if ("fstype".equals(param)) {
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

            paramWi = WidgetFactory.createInstance(
                              Widget.GUESS_TYPE,
                              selectedValue,
                              getBrowser().getCommonFileSystems(defaultValue),
                              Widget.NO_REGEXP,
                              width,
                              Widget.NO_ABBRV,
                              new AccessMode(
                                       getAccessType(param),
                                       isEnabledOnlyInAdvancedMode(param)),
                              Widget.NO_BUTTON);
            fstypeParamWi = paramWi;

            widgetAdd(param, prefix, paramWi);
            paramWi.setEditable(false);
        } else if ("directory".equals(param)) {
            final String[] cmp = getBrowser().getCommonMountPoints();
            final Value[] items = new Value[cmp.length + 1];
            final Value defaultValue = new StringValue() {
                              @Override
                              public String getNothingSelected() {
                                  return Tools.getString(
                                            "ClusterBrowser.SelectMountPoint");
                              }
                          };
            items[0] = defaultValue;
            int i = 1;
            for (final String c : cmp) {
                items[i] = new StringValue(c);
                i++;
            }

            getResource().setPossibleChoices(param, items);
            Value selectedValue = getPreviouslySelected(param, prefix);
            if (selectedValue == null) {
                selectedValue = getParamSaved(param);
            }
            if (selectedValue == null || selectedValue.isNothingSelected()) {
                selectedValue = defaultValue;
            }
            final String regexp = "^.+$";
            paramWi = WidgetFactory.createInstance(
                                 Widget.GUESS_TYPE,
                                 selectedValue,
                                 items,
                                 regexp,
                                 width,
                                 Widget.NO_ABBRV,
                                 new AccessMode(
                                         getAccessType(param),
                                         isEnabledOnlyInAdvancedMode(param)),
                                 Widget.NO_BUTTON);
            widgetAdd(param, prefix, paramWi);
            paramWi.setAlwaysEditable(true);
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
        final DrbdVolumeInfo dvi = getBrowser().getDrbdVolumeFromDev(
                                             getParamSaved(FS_RES_PARAM_DEV).getValueForConfig());
        if (dvi == null) {
            id = getParamSaved(FS_RES_PARAM_DEV).getValueForConfig();
        } else {
            id = dvi.getDrbdResourceInfo().getName();
            s.delete(0, s.length());
            s.append("Filesystem / Drbd");
        }
        if (id == null || id.isEmpty()) {
            id = Tools.getString(
                        "ClusterBrowser.ClusterBlockDevice.Unconfigured");
        }
        s.append(" (");
        s.append(id);
        s.append(')');

        return s.toString();
    }

    /** Removes the service without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Host dcHost,
                                         final Application.RunMode runMode) {
        final DrbdVolumeInfo oldDvi = getBrowser().getDrbdVolumeFromDev(
                                            getParamSaved(FS_RES_PARAM_DEV).getValueForConfig());
        super.removeMyselfNoConfirm(dcHost, runMode);
        if (oldDvi != null && Application.isLive(runMode)) {
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    oldDvi.updateMenus(null);
                }
            });
            t.start();
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
        final DrbdVolumeInfo oldDvi = getBrowser().getDrbdVolumeFromDev(
                                            getParamSaved(FS_RES_PARAM_DEV).getValueForConfig());
        if (oldDvi != null) {
            // TODO: disabled because it does not work well at the moment.
            return;
        }
        final DrbdVolumeInfo newDvi = getBrowser().getDrbdVolumeFromDev(
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
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    oldDvi.updateMenus(null);
                }
            });
            t.start();
            oldDvi.getDrbdResourceInfo().setUsedByCRM(null);
            //final Thread t = new Thread(new Runnable() {
            //    @Override
            //    public void run() {
            //        oldDvi.updateMenus(null);
            //    }
            //});
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
                    getBrowser().getCRMXML().getHbDrbddisk().getName(),
                    fsId);
            newDvi.addDrbdDisk(this, dcHost, drbdId, runMode);
        } else {
            final String drbdId = getBrowser().getFreeId(
                    getBrowser().getCRMXML().getHbLinbitDrbd().getName(),
                    fsId);
            newDvi.addLinbitDrbd(this, dcHost, drbdId, runMode);
        }
    }

    /** Returns how much of the filesystem is used. */
    @Override
    public int getUsed() {
        if (blockDeviceParamWi != null) {
            final Value value = blockDeviceParamWi.getValue();
            if (value == null
                || value.isNothingSelected()
                || !(value instanceof CommonDeviceInterface)) {
                return -1;
            }
            final CommonDeviceInterface cdi = (CommonDeviceInterface) value;
            return cdi.getUsed();
        }
        return -1;
    }

    /** Sets whether the old style drbddisk is preferred. */
    void setDrbddiskIsPreferred(final boolean drbddiskIsPreferred) {
        this.drbddiskIsPreferred = drbddiskIsPreferred;
    }

    /** Reload combo boxes. */
    @Override
    public void reloadComboBoxes() {
        super.reloadComboBoxes();
        final DrbdVolumeInfo selectedInfo =
                                getBrowser().getDrbdVolumeFromDev(
                                            getParamSaved(FS_RES_PARAM_DEV).getValueForConfig());
        final Value selectedValue;
        if (selectedInfo == null) {
            selectedValue = getParamSaved(FS_RES_PARAM_DEV);
        } else {
            selectedValue = selectedInfo;
        }
        Value defaultValue = null;
        if (selectedValue == null || selectedValue.isNothingSelected()) {
            defaultValue = new StringValue() {
                              @Override
                              public String getNothingSelected() {
                                  return Tools.getString(
                                         "ClusterBrowser.SelectBlockDevice");
                              }
                          };
        }
        final Value[] commonBlockDevInfos = getCommonBlockDevInfos(defaultValue,
                                                                   getName());
        if (blockDeviceParamWi != null) {
            final Value value = blockDeviceParamWi.getValue();
            blockDeviceParamWi.reloadComboBox(value,
                                              commonBlockDevInfos);
        }
    }
}
