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

import lcmc.data.ResourceAgent;
import lcmc.data.Host;
import lcmc.data.AccessMode;
import lcmc.configs.DistResource;
import lcmc.utilities.Tools;
import lcmc.utilities.SSH;
import lcmc.utilities.WidgetListener;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.Browser;

import java.util.Map;

/**
 * This class holds info about Filesystem service. It is treated in special
 * way, so that it can use block device information and drbd devices. If
 * drbd device is selected, the drbddisk service will be added too.
 */
final class FilesystemInfo extends ServiceInfo {
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
    void setLinbitDrbdInfo(final LinbitDrbdInfo linbitDrbdInfo) {
        this.linbitDrbdInfo = linbitDrbdInfo;
    }

    /**
     * Returns linbit::drbd info object that is associated with the drbd
     * device or null if it is not a drbd device.
     */
    LinbitDrbdInfo getLinbitDrbdInfo() {
        return linbitDrbdInfo;
    }

    /**
     * Sets DrbddiskInfo object for this Filesystem service if it uses drbd
     * block device.
     */
    void setDrbddiskInfo(final DrbddiskInfo drbddiskInfo) {
        this.drbddiskInfo = drbddiskInfo;
    }

    /**
     * Returns DrbddiskInfo object that is associated with the drbd device
     * or null if it is not a drbd device.
     */
    DrbddiskInfo getDrbddiskInfo() {
        return drbddiskInfo;
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params) {
        final boolean ret = super.checkResourceFieldsCorrect(param, params);
        if (!ret) {
            return false;
        }
        final Widget wi = getWidget(FS_RES_PARAM_DEV, null);
        if (wi == null || wi.getValue() == null) {
            return false;
        }
        return true;
    }

    /** Applies changes to the Filesystem service parameters. */
    @Override
    void apply(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                }
            });
            getInfoPanel();
            waitForInfoPanel();
            final String dir = getComboBoxValue("directory");
            boolean confirm = false; /* confirm only once */
            for (Host host : getBrowser().getClusterHosts()) {
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
        super.apply(dcHost, testOnly);
        //TODO: escape dir
    }

    /** Adds combo box listener for the parameter. */
    private void addParamComboListeners(final Widget paramWi) {
        paramWi.addListeners(
                    new WidgetListener() {
                        @Override
                        public void check(final Object value) {
                            if (fstypeParamWi != null) {
                                if (!(value instanceof Info)) {
                                    return;
                                }
                                final Info item = (Info) value;
                                if (item.getStringValue() == null
                                    || "".equals(item.getStringValue())) {
                                    return;
                                }
                                final String selectedValue =
                                                     getParamSaved("fstype");
                                String createdFs;
                                if (selectedValue == null
                                    || "".equals(selectedValue)) {
                                    final CommonDeviceInterface cdi =
                                             (CommonDeviceInterface) item;
                                    createdFs = cdi.getCreatedFs();
                                } else {
                                    createdFs = selectedValue;
                                }
                                if (createdFs != null
                                    && !"".equals(createdFs)) {
                                    fstypeParamWi.setValue(createdFs);
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
        Widget paramWi;
        if (FS_RES_PARAM_DEV.equals(param)) {
            String selectedValue = getPreviouslySelected(param, prefix);
            if (selectedValue == null) {
                selectedValue = getParamSaved(param);
            }
            final DrbdVolumeInfo selectedInfo =
                            getBrowser().getDrbdVolumeFromDev(selectedValue);
            if (selectedInfo != null) {
                selectedValue = selectedInfo.toString();
            }
            Info defaultValue = null;
            if (selectedValue == null || "".equals(selectedValue)) {
                defaultValue = new StringInfo(
                           Tools.getString("ClusterBrowser.SelectBlockDevice"),
                           "",
                           getBrowser());
            }
            final Info[] commonBlockDevInfos =
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
            final String defaultValue =
                        Tools.getString("ClusterBrowser.SelectFilesystem");
            String selectedValue = getPreviouslySelected(param, prefix);
            if (selectedValue == null) {
                selectedValue = getParamSaved(param);
            }
            if (selectedValue == null || "".equals(selectedValue)) {
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
            Object[] items = new Object[cmp.length + 1];
            System.arraycopy(cmp,
                             0,
                             items,
                             1,
                             cmp.length);
            final String defaultValue =
                           Tools.getString("ClusterBrowser.SelectMountPoint");
            items[0] = new StringInfo(
                            defaultValue,
                            null,
                            getBrowser());
            getResource().setPossibleChoices(param, items);
            String selectedValue = getPreviouslySelected(param, prefix);
            if (selectedValue == null) {
                selectedValue = getParamSaved(param);
            }
            if (selectedValue == null || "".equals(selectedValue)) {
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
                                             getParamSaved(FS_RES_PARAM_DEV));
        if (dvi == null) {
            id = getParamSaved(FS_RES_PARAM_DEV);
        } else {
            id = dvi.getDrbdResourceInfo().getName();
            s.delete(0, s.length());
            s.append("Filesystem / Drbd");
        }
        if (id == null || "".equals(id)) {
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
                                         final boolean testOnly) {
        final DrbdVolumeInfo oldDvi = getBrowser().getDrbdVolumeFromDev(
                                            getParamSaved(FS_RES_PARAM_DEV));
        super.removeMyselfNoConfirm(dcHost, testOnly);
        if (oldDvi != null && !testOnly) {
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
    void addResourceBefore(final Host dcHost, final boolean testOnly) {
        if (getGroupInfo() != null) {
            // TODO: disabled for now
            return;
        }
        final DrbdVolumeInfo oldDvi = getBrowser().getDrbdVolumeFromDev(
                                            getParamSaved(FS_RES_PARAM_DEV));
        if (oldDvi != null) {
            // TODO: disabled because it does not work well at the moment.
            return;
        }
        final DrbdVolumeInfo newDvi = getBrowser().getDrbdVolumeFromDev(
                                          getComboBoxValue(FS_RES_PARAM_DEV));
        if (newDvi == null || newDvi.equals(oldDvi)) {
            return;
        }
        boolean oldDrbddisk = false;
        if (getDrbddiskInfo() == null) {
            oldDrbddisk = drbddiskIsPreferred;
        } else {
            oldDrbddisk = true;
        }
        if (oldDvi != null) {
            if (oldDrbddisk) {
                oldDvi.removeDrbdDisk(this, dcHost, testOnly);
            } else {
                oldDvi.removeLinbitDrbd(this, dcHost, testOnly);
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
        if (newDvi != null) {
            //newDvi.getDrbdResourceInfo().setUsedByCRM(true);
            //final Thread t = new Thread(new Runnable() {
            //    @Override
            //    public void run() {
            //        newDvi.updateMenus(null);
            //    }
            //});
            //t.start();
            if (oldDrbddisk) {
                newDvi.addDrbdDisk(this, dcHost, testOnly);
            } else {
                newDvi.addLinbitDrbd(this, dcHost, testOnly);
            }
        }
    }

    /** Returns how much of the filesystem is used. */
    @Override
    public int getUsed() {
        if (blockDeviceParamWi != null) {
            final Object value = blockDeviceParamWi.getValue();
            if (Tools.isStringClass(value)) {
                // TODO:
                return -1;
            }
            final Info item = (Info) value;
            final String sValue = item.getStringValue();
            if (sValue == null || "".equals(sValue)) {
                return -1;
            }
            final CommonDeviceInterface cdi = (CommonDeviceInterface) item;
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
                                            getParamSaved(FS_RES_PARAM_DEV));
        String selectedValue = null;
        if (selectedInfo == null) {
            selectedValue = getParamSaved(FS_RES_PARAM_DEV);
        } else {
            selectedValue = selectedInfo.toString();
        }
        Info defaultValue = null;
        if (selectedValue == null || "".equals(selectedValue)) {
            defaultValue = new StringInfo(
                       Tools.getString("ClusterBrowser.SelectBlockDevice"),
                       "",
                       getBrowser());
        }
        final Info[] commonBlockDevInfos = getCommonBlockDevInfos(defaultValue,
                                                                  getName());
        if (blockDeviceParamWi != null) {
            final String value = blockDeviceParamWi.getStringValue();
            blockDeviceParamWi.reloadComboBox(value,
                                              commonBlockDevInfos);
        }
    }
}
