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
package drbd.gui.resources;

import drbd.data.ResourceAgent;
import drbd.data.Host;
import drbd.utilities.Tools;
import drbd.utilities.SSH;
import drbd.gui.GuiComboBox;
import drbd.gui.Browser;

import java.util.Map;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

/**
 * This class holds info about Filesystem service. It is treated in special
 * way, so that it can use block device information and drbd devices. If
 * drbd device is selected, the drbddisk service will be added too.
 */
class FilesystemInfo extends ServiceInfo {
    /** linbit::drbd service object. */
    private LinbitDrbdInfo linbitDrbdInfo = null;
    /** drbddisk service object. */
    private DrbddiskInfo drbddiskInfo = null;
    /** Block device combo box. */
    private GuiComboBox blockDeviceParamCb = null;
    /** Filesystem type combo box. */
    private GuiComboBox fstypeParamCb = null;
    /** Whether old style drbddisk is preferred. */
    private boolean drbddiskIsPreferred = false;
    /** Name of the device parameter in the file system. */
    private static final String FS_RES_PARAM_DEV = "device";

    /**
     * Creates the FilesystemInfo object.
     */
    public FilesystemInfo(final String name,
                          final ResourceAgent ra,
                          final Browser browser) {
        super(name, ra, browser);
    }

    /**
     * Creates the FilesystemInfo object.
     */
    public FilesystemInfo(final String name,
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
    public boolean checkResourceFieldsCorrect(final String param,
                                              final String[] params) {
        final boolean ret = super.checkResourceFieldsCorrect(param, params);
        if (!ret) {
            return false;
        }
        final GuiComboBox cb = paramComboBoxGet(FS_RES_PARAM_DEV, null);
        if (cb == null || cb.getValue() == null) {
            return false;
        }
        return true;
    }

    /**
     * Applies changes to the Filesystem service parameters.
     */
    public void apply(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            final String dir = getComboBoxValue("directory");
            boolean confirm = false; /* confirm only once */
            for (Host host : getBrowser().getClusterHosts()) {
                final String hostName = host.getName();
                final String statCmd = "stat -c \"%F\" " + dir + "||true";
                final SSH.SSHOutput ret =
                                   Tools.execCommandProgressIndicator(
                                                host,
                                                statCmd,
                                                null,
                                                true,
                                                statCmd,
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
                        final String cmd = "mkdir " + dir;
                        Tools.execCommandProgressIndicator(
                                                host,
                                                cmd,
                                                null,
                                                true,
                                                cmd,
                                                SSH.DEFAULT_COMMAND_TIMEOUT);
                        confirm = true;
                    }
                }
            }
        }
        super.apply(dcHost, testOnly);
        //TODO: escape dir
    }

    /**
     * Adds combo box listener for the parameter.
     */
    private void addParamComboListeners(final GuiComboBox paramCb) {
        paramCb.addListeners(
            new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED
                        && fstypeParamCb != null) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                if (!(e.getItem() instanceof Info)) {
                                    return;
                                }
                                final Info item = (Info) e.getItem();
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
                                    fstypeParamCb.setValue(createdFs);
                                }
                            }
                        });
                        thread.start();
                    }
                }
            },
            null);
    }
    ///** Sets service parameters with values from resourceNode hash. */
    //public void setParameters(final Map<String, String> resourceNode) {
    //    super.setParameters(resourceNode);
    //    //final DrbdResourceInfo dri = getBrowser().getDrbdDevHash().get(
    //    //                                 getParamSaved(FS_RES_PARAM_DEV));
    //    //if (dri != null) {
    //    //    dri.setUsedByCRM(true);
    //    //    final Thread t = new Thread(new Runnable() {
    //    //        public void run() {
    //    //            dri.updateMenus(null);
    //    //        }
    //    //    });
    //    //    t.start();
    //    //}
    //}

    /** Returns editable element for the parameter. */
    protected GuiComboBox getParamComboBox(final String param,
                                           final String prefix,
                                           final int width) {
        GuiComboBox paramCb;
        if (FS_RES_PARAM_DEV.equals(param)) {
            final DrbdResourceInfo selectedInfo =
                          getBrowser().getDrbdDevHash().get(
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
            final Info[] commonBlockDevInfos =
                                        getCommonBlockDevInfos(defaultValue,
                                                               getName());
            paramCb = new GuiComboBox(selectedValue,
                                      commonBlockDevInfos,
                                      null, /* units */
                                      null, /* type */
                                      null, /* regexp */
                                      width,
                                      null, /* abbrv */
                                      getAccessType(param));
            blockDeviceParamCb = paramCb;
            addParamComboListeners(paramCb);
            paramComboBoxAdd(param, prefix, paramCb);
        } else if ("fstype".equals(param)) {
            final String defaultValue =
                        Tools.getString("ClusterBrowser.SelectFilesystem");
            String selectedValue = getParamSaved("fstype");
            if (selectedValue == null || "".equals(selectedValue)) {
                selectedValue = defaultValue;
            }
            paramCb = new GuiComboBox(
                              selectedValue,
                              getBrowser().getCommonFileSystems(defaultValue),
                              null, /* units */
                              null, /* type */
                              null, /* regexp */
                              width,
                              null, /* abbrv */
                              getAccessType(param));
            fstypeParamCb = paramCb;

            paramComboBoxAdd(param, prefix, paramCb);
            paramCb.setEditable(false);
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
            String selectedValue = getParamSaved("directory");
            if (selectedValue == null || "".equals(selectedValue)) {
                selectedValue = defaultValue;
            }
            final String regexp = "^.+$";
            paramCb = new GuiComboBox(selectedValue,
                                      items,
                                      null, /* units */
                                      null, /* type */
                                      regexp,
                                      width,
                                      null, /* abbrv */
                                      getAccessType(param));
            paramComboBoxAdd(param, prefix, paramCb);
            paramCb.setAlwaysEditable(true);
        } else {
            paramCb = super.getParamComboBox(param, prefix, width);
        }
        return paramCb;
    }

    /**
     * Returns string representation of the filesystem service.
     */
    public String toString() {
        String id = getService().getId();
        if (id == null) {
            return super.toString(); /* this is for 'new Filesystem' */
        }

        final StringBuffer s = new StringBuffer(getName());
        final DrbdResourceInfo dri = getBrowser().getDrbdResHash().get(
                                             getParamSaved(FS_RES_PARAM_DEV));
        if (dri == null) {
            id = getParamSaved(FS_RES_PARAM_DEV);
        } else {
            id = dri.getName();
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

    /**
     * Adds DrbddiskInfo before the filesysteminfo is added, returns true
     * if something was added.
     */
    public final void addResourceBefore(final Host dcHost,
                                        final boolean testOnly) {
        final DrbdResourceInfo oldDri =
                    getBrowser().getDrbdDevHash().get(
                                            getParamSaved(FS_RES_PARAM_DEV));
        final DrbdResourceInfo newDri =
                    getBrowser().getDrbdDevHash().get(
                                        getComboBoxValue(FS_RES_PARAM_DEV));
        if (newDri == null || newDri.equals(oldDri)) {
            return;
        }
        boolean oldDrbddisk = false;
        if (getDrbddiskInfo() != null) {
            oldDrbddisk = true;
        } else {
            oldDrbddisk = drbddiskIsPreferred;
        }
        if (oldDri != null) {
            if (oldDrbddisk) {
                oldDri.removeDrbdDisk(this, dcHost, testOnly);
            } else {
                oldDri.removeLinbitDrbd(this, dcHost, testOnly);
            }
            //oldDri.setUsedByCRM(false);
            //final Thread t = new Thread(new Runnable() {
            //    public void run() {
            //        oldDri.updateMenus(null);
            //    }
            //});
            if (oldDrbddisk) {
                setDrbddiskInfo(null);
            } else {
                setLinbitDrbdInfo(null);
            }
        }
        if (newDri != null) {
            //newDri.setUsedByCRM(true);
            //final Thread t = new Thread(new Runnable() {
            //    public void run() {
            //        newDri.updateMenus(null);
            //    }
            //});
            //t.start();
            if (oldDrbddisk) {
                newDri.addDrbdDisk(this, dcHost, testOnly);
            } else {
                newDri.addLinbitDrbd(this, dcHost, testOnly);
            }
        }
    }

    /**
     * Returns how much of the filesystem is used.
     */
    public final int getUsed() {
        if (blockDeviceParamCb != null) {
            final Object value = blockDeviceParamCb.getValue();
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
    public final void setDrbddiskIsPreferred(
                                           final boolean drbddiskIsPreferred) {
        this.drbddiskIsPreferred = drbddiskIsPreferred;
    }

    /** Reload combo boxes. */
    public void reloadComboBoxes() {
        super.reloadComboBoxes();
        final DrbdResourceInfo selectedInfo =
                          getBrowser().getDrbdDevHash().get(
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
        if (blockDeviceParamCb != null) {
            final String value = blockDeviceParamCb.getStringValue();
            blockDeviceParamCb.reloadComboBox(value,
                                              commonBlockDevInfos);
        }
    }
}
