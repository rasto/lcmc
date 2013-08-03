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

import lcmc.Exceptions;
import lcmc.gui.Browser;
import lcmc.gui.HostBrowser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.DrbdGraph;

import lcmc.gui.dialog.lvm.VGCreate;
import lcmc.gui.dialog.lvm.VGRemove;
import lcmc.gui.dialog.lvm.LVCreate;
import lcmc.gui.dialog.lvm.LVResize;
import lcmc.gui.dialog.lvm.LVSnapshot;
import lcmc.gui.dialog.drbd.DrbdLog;


import lcmc.data.ConfigData;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.Tools;
import lcmc.utilities.DRBD;
import lcmc.utilities.LVM;
import lcmc.utilities.ButtonCallback;
import lcmc.gui.widget.Widget;
import lcmc.data.Host;
import lcmc.data.Subtext;
import lcmc.data.Cluster;
import lcmc.data.DRBDtestData;
import lcmc.data.resources.BlockDevice;
import lcmc.data.AccessMode;
import lcmc.data.DrbdXML;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.CountDownLatch;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;

/**
 * This class holds info data for a block device.
 */
public final class BlockDevInfo extends EditableInfo {
    /** DRBD resource in which this block device is member. */
    private DrbdVolumeInfo drbdVolumeInfo;
    /** Map from paremeters to the fact if the last entered value was
     * correct. */
    private final Map<String, Boolean> paramCorrectValueMap =
                                            new HashMap<String, Boolean>();
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Keyword that denotes flexible meta-disk. */
    private static final String DRBD_MD_TYPE_FLEXIBLE = "Flexible";
    /** Internal parameter name of drbd meta-disk. */
    private static final String DRBD_MD_PARAM         = "DrbdMetaDisk";
    /** Internal parameter name of drbd meta-disk index. */
    private static final String DRBD_MD_INDEX_PARAM   = "DrbdMetaDiskIndex";
    /** Large harddisk icon. */
    public static final ImageIcon HARDDISK_ICON_LARGE = Tools.createImageIcon(
                           Tools.getDefault("BlockDevInfo.HarddiskIconLarge"));
    /** Large harddisk with drbd icon. */
    public static final ImageIcon HARDDISK_DRBD_ICON_LARGE =
                    Tools.createImageIcon(
                       Tools.getDefault("BlockDevInfo.HarddiskDRBDIconLarge"));
    /** Large no harddisk icon. */
    public static final ImageIcon NO_HARDDISK_ICON_LARGE =
                    Tools.createImageIcon(
                         Tools.getDefault("BlockDevInfo.NoHarddiskIconLarge"));
    /** Harddisk icon. */
    public static final ImageIcon HARDDISK_ICON = Tools.createImageIcon(
                                Tools.getDefault("BlockDevInfo.HarddiskIcon"));
    /** Meta-disk subtext. */
    private static final Subtext METADISK_SUBTEXT =
                             new Subtext("meta-disk", Color.BLUE, Color.BLACK);
    /** Swap subtext. */
    private static final Subtext SWAP_SUBTEXT =
                                  new Subtext("swap", Color.BLUE, Color.BLACK);
    /** Mounted subtext. */
    private static final Subtext MOUNTED_SUBTEXT =
                               new Subtext("mounted", Color.BLUE, Color.BLACK);
    /** Physical volume subtext. */
    private static final Subtext PHYSICAL_VOLUME_SUBTEXT =
                               new Subtext("PV", Color.BLUE, Color.GREEN);
    /** String length after the cut. */
    private static final int MAX_RIGHT_CORNER_STRING_LENGTH = 28;
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String NO_DRBD_RESOURCE_STRING =
                                                "it is not a drbd resource";
    /** Allow two primaries paramater. */
    private static final String ALLOW_TWO_PRIMARIES = "allow-two-primaries";
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
    /** "Proxy up" text for graph. */
    public static final String PROXY_UP = "Proxy Up";
    /** "Proxy down" text for graph. */
    private static final String PROXY_DOWN = "Proxy Down";

    /**
     * Prepares a new <code>BlockDevInfo</code> object.
     *
     * @param name
     *      name that will be shown in the tree
     * @param blockDevice
     *      bock device
     */
    public BlockDevInfo(final String name,
                 final BlockDevice blockDevice,
                 final Browser browser) {
        super(name, browser);
        setResource(blockDevice);
    }

    /**
     * Returns object of the other block device that is connected via drbd
     * to this block device.
     */
    public BlockDevInfo getOtherBlockDevInfo() {
        final DrbdVolumeInfo dvi = drbdVolumeInfo;
        if (dvi == null) {
            return null;
        }
        return dvi.getOtherBlockDevInfo(this);
    }


    /** Returns browser object of this info. */
    @Override
    public HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /** Sets info panel of this block devices. TODO: explain why. */
    void setInfoPanel(final JComponent infoPanel) {
        this.infoPanel = infoPanel;
    }

    /**
     * Remove this block device.
     *
     * TODO: check this
     */
    @Override
    public void removeMyself(final boolean testOnly) {
        getBlockDevice().setValue(DRBD_MD_PARAM, null);
        getBlockDevice().setValue(DRBD_MD_INDEX_PARAM, null);
        super.removeMyself(testOnly);
        if (!testOnly) {
            removeNode();
        }
        infoPanel = null;
    }

    /** Returns host on which is this block device. */
    public Host getHost() {
        return getBrowser().getHost();
    }

    /** Returns block device icon for the menu. */
    @Override
    public ImageIcon getMenuIcon(final boolean testOnly) {
        return BlockDevInfo.HARDDISK_ICON;
    }

    /** Returns info of this block device as string. */
    @Override
    String getInfo() {
        final StringBuilder ret = new StringBuilder(120);
        ret.append("Host            : ");
        ret.append(getHost().getName());
        ret.append("\nDevice          : ");
        ret.append(getBlockDevice().getName());
        ret.append("\nMeta disk       : ");
        ret.append(getBlockDevice().isDrbdMetaDisk());
        ret.append("\nSize            : ");
        ret.append(getBlockDevice().getBlockSize());
        ret.append(" blocks");
        if (getBlockDevice().getMountedOn() == null) {
            ret.append("\nnot mounted");
        } else {
            ret.append("\nMounted on      : ");
            ret.append(getBlockDevice().getMountedOn());
            ret.append("\nType            : ");
            ret.append(getBlockDevice().getFsType());
            if (getUsed() >= 0) {
                ret.append("\nUsed:           : ");
                ret.append(getUsed());
                ret.append('%');
            }
        }
        if (getBlockDevice().isDrbd()) {
            ret.append("\nConnection state: ");
            ret.append(getBlockDevice().getConnectionState());
            ret.append("\nNode state      : ");
            ret.append(getBlockDevice().getNodeState());
            ret.append("\nDisk state      : ");
            ret.append(getBlockDevice().getDiskState());
            ret.append('\n');
        }
        return ret.toString();
    }

    /** Append hierarchy  of block devices in the string buffer using HTML. */
    private void appendBlockDeviceHierarchy(final BlockDevice bd,
                                            final StringBuilder tt,
                                            final int shift) {
        String tab = "";
        for (int i = 0; i != shift; ++i) {
            tab += "    ";
        }
        /* physical volumes */
        String vg = null;
        String selectedPV = null;
        if (bd.isVolumeGroupOnPhysicalVolume()) {
            vg = bd.getVolumeGroupOnPhysicalVolume();
            selectedPV = bd.getName();
        }  else if (isLVM()) {
            vg = bd.getVolumeGroup();
        }
        if (vg != null) {
            for (final BlockDevice pv : getHost().getPhysicalVolumes(vg)) {
                if (pv.equals(selectedPV)) {
                    tt.append("<b>");
                    tt.append(tab + pv);
                    tt.append("</b>");
                } else {
                    tt.append(tab + pv);
                }
                tt.append('\n');
            }
        }
        /* volume groups */
        String selectedLV = null;
        if (vg != null) {
            if (bd.isVolumeGroupOnPhysicalVolume()) {
                tt.append("<b>");
                tt.append("    " + tab + vg);
                tt.append("</b>\n");
            } else if (isLVM()) {
                tt.append("    " + tab);
                tt.append(vg);
                tt.append('\n');
                selectedLV = bd.getName();
            }
            final Set<String> lvs =
                             getHost().getLogicalVolumesFromVolumeGroup(vg);
            if (lvs != null) {
                for (final String lv : lvs) {
                    tt.append("        " + tab);
                    final String lvName = "/dev/" + vg + "/" + lv;
                    if (lvName.equals(selectedLV)) {
                        if (bd.isDrbd()) {
                            tt.append(lv);
                            tt.append("\n");
                            final BlockDevice drbdBD = bd.getDrbdBlockDevice();
                            if (drbdBD != null) {
                                appendBlockDeviceHierarchy(drbdBD,
                                                           tt,
                                                           shift + 3);
                            }
                        } else {
                            tt.append("<b>");
                            tt.append(lv);
                            tt.append("</b>\n");
                        }
                    } else {
                        tt.append(lv);
                        tt.append('\n');
                    }
                }
            }
        } else {
            final BlockDevice drbdBD = bd.getDrbdBlockDevice();
            if (drbdBD != null) {
                tt.append(tab + bd.getName());
                tt.append('\n');
                appendBlockDeviceHierarchy(drbdBD, tt, shift + 1);
            } else {
                tt.append("<b>");
                tt.append(tab + bd.getName());
                tt.append("</b>\n");
            }
        }
    }

    /** Returns tool tip for this block device. */
    @Override
    public String getToolTipForGraph(final boolean testOnly) {
        final StringBuilder tt = new StringBuilder(60);

        final BlockDevice bd = getBlockDevice();
        tt.append("<pre>");
        appendBlockDeviceHierarchy(bd, tt, 0);
        tt.append("</pre>");
        if (bd.isDrbdMetaDisk()) {
            tt.append(" (Meta Disk)\n");
            for (final BlockDevice mb
                         : getBlockDevice().getMetaDiskOfBlockDevices()) {
                tt.append("&nbsp;&nbsp;of ");
                tt.append(mb.getName());
                tt.append('\n');
            }
        }

        if (bd.isDrbd()) {
            if (getHost().isDrbdStatus()) {
                String cs = bd.getConnectionState();
                String st = bd.getNodeState();
                String ds = bd.getDiskState();
                if (cs == null) {
                    cs = "not available";
                }
                if (st == null) {
                    st = "not available";
                }
                if (ds == null) {
                    ds = "not available";
                }

                tt.append("\n<table><tr><td><b>cs:</b></td><td>");
                tt.append(cs);
                tt.append("</td></tr><tr><td><b>ro:</b></td><td>");
                tt.append(st);
                tt.append("</td></tr><tr><td><b>ds:</b></td><td>");
                tt.append(ds);
                tt.append("</td></tr></table>");
            } else {
                tt.append('\n');
                tt.append(Tools.getString("HostBrowser.Hb.NoInfoAvailable"));
            }
        }
        return tt.toString();
    }

    /** Creates config for one node. */
    String drbdBDConfig(final String resource,
                        final String drbdDevice,
                        final boolean volumesAvailable)
            throws Exceptions.DrbdConfigException {

        if (drbdDevice == null) {
            throw new Exceptions.DrbdConfigException(
                                    "Drbd device not defined for host "
                                    + getHost().getName()
                                    + " (" + resource + ")");
        }
        if (getBlockDevice().getName() == null) {
            throw new Exceptions.DrbdConfigException(
                                    "Block device not defined for host "
                                    + getHost().getName()
                                    + " (" + resource + ")");
        }

        final StringBuilder config = new StringBuilder(120);
        String tabs;
        if (volumesAvailable) {
            tabs = "\t\t\t";
        } else {
            tabs = "\t\t";
        }
        config.append(tabs + "device\t\t");
        config.append(drbdDevice);
        config.append(";\n" + tabs + "disk\t\t");
        config.append(getBlockDevice().getName());
        config.append(";\n" + tabs);
        config.append(getBlockDevice().getMetaDiskString(
                                       getComboBoxValue(DRBD_MD_PARAM),
                                       getComboBoxValue(DRBD_MD_INDEX_PARAM)));
        config.append(';');
        return config.toString();
    }

    /** Sets whether this block device is drbd. */
    void setDrbd(final boolean drbd) {
        getBlockDevice().setDrbd(drbd);
    }

    /** Returns section of this paramter. */
    @Override
    protected String getSection(final String param) {
        return getBlockDevice().getSection(param);
    }

    /** Returns possible choices of this paramter. */
    @Override
    protected Object[] getPossibleChoices(final String param) {
        return getBlockDevice().getPossibleChoices(param);
    }

    /** Returns default value of this paramter. */
    protected Object getDefaultValue(final String param) {
        return "<select>";
    }

    /** Returns combobox for this parameter. */
    @Override
    protected Widget createWidget(final String param,
                                  final String prefix,
                                  final int width) {
        Widget paramWi;
        if (DRBD_MD_INDEX_PARAM.equals(param)) {
            final Widget gwi = super.createWidget(param, prefix, width);
            paramWi = gwi;
            //Tools.invokeLater(new Runnable() {
            //    @Override
            //    public void run() {
            //        gwi.setAlwaysEditable(true);
            //    }
            //});
        } else {
            final Widget gwi = super.createWidget(param, prefix, width);
            paramWi = gwi;
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    gwi.setEditable(false);
                }
            });
        }
        return paramWi;
    }

    /** Returns true if a paramter is correct. */
    @Override
    protected boolean checkParam(final String param, String value) {
        boolean ret = true;
        if (value == null) {
            value = "";
        }
        if ("".equals(value) && isRequired(param)) {
            ret = false;
        } else if (DRBD_MD_PARAM.equals(param)) {
            if (infoPanel != null) {
                if (!getHost().isServerStatusLatch()) {
                    final boolean internal = "internal".equals(value);
                    final Widget ind = getWidget(DRBD_MD_INDEX_PARAM, null);
                    final Widget indW = getWidget(DRBD_MD_INDEX_PARAM,
                                                  Widget.WIZARD_PREFIX);
                    if (internal) {
                        ind.setValue(DRBD_MD_TYPE_FLEXIBLE);
                        if (indW != null) {
                            indW.setValue(DRBD_MD_TYPE_FLEXIBLE);
                        }
                    }
                    Tools.invokeLater(!Tools.CHECK_SWING_THREAD,
                                      new Runnable() {
                        @Override
                        public void run() {
                            ind.setEnabled(!internal);
                        }
                    });
                    if (indW != null) {
                        Tools.invokeLater(!Tools.CHECK_SWING_THREAD,
                                          new Runnable() {
                            @Override
                            public void run() {
                                indW.setEnabled(!internal);
                            }
                        });
                    }
                }
            }
        } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
            if (getBrowser().getUsedPorts().contains(value)
                && !value.equals(getBlockDevice().getValue(param))) {
                ret = false;
            }
            final Pattern p = Pattern.compile(".*\\D.*");
            final Matcher m = p.matcher(value);
            if (m.matches() && !DRBD_MD_TYPE_FLEXIBLE.equals(value)) {
                ret = false;
            }
        }
        paramCorrectValueMap.remove(param);
        paramCorrectValueMap.put(param, ret);
        return ret;
    }

    /** Returns whether this parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        return true;
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        return false;
    }

    /** Returns access type of this parameter. */
    @Override
    protected ConfigData.AccessType getAccessType(final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
         return false;
    }

    /** Returns whether this type is integer. */
    @Override
    protected boolean isInteger(final String param) {
        return false;
    }

    /** Returns whether this type is a label. */
    @Override
    protected boolean isLabel(final String param) {
        return false;
    }

    /** Returns whether this parameter is of a time type. */
    @Override
    protected boolean isTimeType(final String param) {
        /* not required */
        return false;
    }

    /** Returns whether this parameter is a checkbox. */
    @Override
    protected boolean isCheckBox(final String param) {
        return false;
    }

    /** Returns type of this parameter. */
    @Override
    protected String getParamType(final String param) {
        return null;
    }

    /** Returns the regexp of the parameter. */
    @Override
    protected String getParamRegexp(final String param) {
        return null;
    }

    /** Returns possible choices for the parameter. */
    @Override
    protected Object[] getParamPossibleChoices(final String param) {
        if (DRBD_MD_PARAM.equals(param)) {
            /* meta disk */
            final StringInfo internalMetaDisk =
                    new StringInfo(Tools.getString(
                                        "HostBrowser.MetaDisk.Internal"),
                                   "internal",
                                   getBrowser());
            final String defaultMetaDiskString = internalMetaDisk.toString();
            getBrowser().lockBlockDevInfosRead();
            @SuppressWarnings("unchecked")
            final Info[] blockDevices = getAvailableBlockDevicesForMetaDisk(
                                            internalMetaDisk,
                                            getName(),
                                            getBrowser().getBlockDevInfos());
            getBrowser().unlockBlockDevInfosRead();

            getBlockDevice().setDefaultValue(DRBD_MD_PARAM,
                                             defaultMetaDiskString);
            return blockDevices;
        } else if (DRBD_MD_INDEX_PARAM.equals(param)) {

            String defaultMetaDiskIndex = getBlockDevice().getValue(
                                                       DRBD_MD_INDEX_PARAM);
            if ("internal".equals(defaultMetaDiskIndex)) {
                defaultMetaDiskIndex =
                         Tools.getString("HostBrowser.MetaDisk.Internal");
            }

            String[] indeces = new String[11];
            int index = 0;
            if (defaultMetaDiskIndex == null) {
                defaultMetaDiskIndex = DRBD_MD_TYPE_FLEXIBLE;
            } else if (!DRBD_MD_TYPE_FLEXIBLE.equals(defaultMetaDiskIndex)) {
                index = Integer.valueOf(defaultMetaDiskIndex) - 5;
                if (index < 0) {
                    index = 0;
                }
            }

            indeces[0] = DRBD_MD_TYPE_FLEXIBLE;
            for (int i = 1; i < 11; i++) {
                indeces[i] = Integer.toString(index);
                index++;
            }

            getBlockDevice().setDefaultValue(DRBD_MD_INDEX_PARAM,
                                             DRBD_MD_TYPE_FLEXIBLE);
            return indeces;
        }
        return null;
    }

    /** Returns default for this parameter. */
    @Override
    protected String getParamDefault(final String param) {
        return getBlockDevice().getDefaultValue(param);
    }

    /** Returns preferred value of this parameter. */
    @Override
    protected String getParamPreferred(final String param) {
        return getBlockDevice().getPreferredValue(param);
    }

    /** Return whether the value is correct from the cache. */
    @Override
    protected boolean checkParamCache(final String param) {
        final Boolean cv = paramCorrectValueMap.get(param);
        if (cv == null) {
            return false;
        }
        return cv.booleanValue();
    }

    /** Returns block devices that are available for drbd meta-disk. */
    protected Info[] getAvailableBlockDevicesForMetaDisk(
                               final Info defaultValue,
                               final String serviceName,
                               final Set<BlockDevInfo> blockDevInfos) {
        final List<Info> list = new ArrayList<Info>();
        final String savedMetaDisk = getBlockDevice().getValue(DRBD_MD_PARAM);

        if (defaultValue != null) {
            list.add(defaultValue);
        }

        for (final BlockDevInfo bdi : blockDevInfos) {
            final BlockDevice bd = bdi.getBlockDevice();
            if (bd.toString().equals(savedMetaDisk)
                || (!bd.isDrbd() && !bd.isUsedByCRM() && !bd.isMounted())) {
                list.add(bdi);
            }
        }
        return list.toArray(new Info[list.size()]);
    }

    /** DRBD attach. */
    void attach(final boolean testOnly) {
        DRBD.attach(getHost(),
                    drbdVolumeInfo.getDrbdResourceInfo().getName(),
                    drbdVolumeInfo.getName(),
                    testOnly);
    }

    /** DRBD detach. */
    void detach(final boolean testOnly) {
        DRBD.detach(getHost(),
                    drbdVolumeInfo.getDrbdResourceInfo().getName(),
                    drbdVolumeInfo.getName(),
                    testOnly);
    }

    /** DRBD connect. */
    void connect(final boolean testOnly) {
        DRBD.connect(getHost(),
                     drbdVolumeInfo.getDrbdResourceInfo().getName(),
                     null,
                     testOnly);
    }

    /** DRBD disconnect. */
    void disconnect(final boolean testOnly) {
        DRBD.disconnect(getHost(),
                        drbdVolumeInfo.getDrbdResourceInfo().getName(),
                        null,
                        testOnly);
    }

    /** DRBD pause sync. */
    void pauseSync(final boolean testOnly) {
        DRBD.pauseSync(getHost(),
                       drbdVolumeInfo.getDrbdResourceInfo().getName(),
                       drbdVolumeInfo.getName(),
                       testOnly);
    }

    /** DRBD resume sync. */
    void resumeSync(final boolean testOnly) {
        DRBD.resumeSync(getHost(),
                        drbdVolumeInfo.getDrbdResourceInfo().getName(),
                        drbdVolumeInfo.getName(),
                        testOnly);
    }

    /** DRBD up command. */
    void drbdUp(final boolean testOnly) {
        DRBD.up(getHost(),
                drbdVolumeInfo.getDrbdResourceInfo().getName(),
                drbdVolumeInfo.getName(),
                testOnly);
    }

    /** Sets this drbd block device to the primary state. */
    void setPrimary(final boolean testOnly) {
        DRBD.setPrimary(getHost(),
                        drbdVolumeInfo.getDrbdResourceInfo().getName(),
                        drbdVolumeInfo.getName(),
                        testOnly);
    }

    /** Sets this drbd block device to the secondary state. */
    public void setSecondary(final boolean testOnly) {
        DRBD.setSecondary(getHost(),
                          drbdVolumeInfo.getDrbdResourceInfo().getName(),
                          drbdVolumeInfo.getName(),
                          testOnly);
    }

    /** Initializes drbd block device. */
    void initDrbd(final boolean testOnly) {
        DRBD.initDrbd(getHost(),
                      drbdVolumeInfo.getDrbdResourceInfo().getName(),
                      drbdVolumeInfo.getName(),
                      testOnly);
    }

    /** Make filesystem. */
     public void makeFilesystem(final String filesystem,
                                final boolean testOnly) {
        DRBD.makeFilesystem(getHost(),
                            getDrbdVolumeInfo().getDevice(),
                            filesystem,
                            testOnly);
    }

    /** Initialize a physical volume. */
    public boolean pvCreate(final boolean testOnly) {
        String device;
        if (getBlockDevice().isDrbd()) {
            device = drbdVolumeInfo.getDevice();
        } else {
            device = getBlockDevice().getName();
        }
        final boolean ret = LVM.pvCreate(getHost(), device, testOnly);
        if (ret) {
            getBlockDevice().setVolumeGroupOnPhysicalVolume("");
        }
        return ret;
    }

    /** Remove a physical volume. */
    public boolean pvRemove(final boolean testOnly) {
        String device;
        if (getBlockDevice().isDrbd()) {
            device = drbdVolumeInfo.getDevice();
        } else {
            device = getBlockDevice().getName();
        }
        final boolean ret = LVM.pvRemove(getHost(), device, testOnly);
        if (ret) {
            if (getBlockDevice().isDrbd()) {
                getBlockDevice().getDrbdBlockDevice()
                                .setVolumeGroupOnPhysicalVolume(null);
            } else {
                getBlockDevice().setVolumeGroupOnPhysicalVolume(null);
            }
        }
        return ret;
    }

    /** Remove a logical volume. */
    public boolean lvRemove(final boolean testOnly) {
        final String device = getBlockDevice().getName();
        return LVM.lvRemove(getHost(), device, testOnly);
    }

    /** Make snapshot. */
    public boolean lvSnapshot(final String snapshotName,
                              final String size,
                              final boolean testOnly) {
        final String device = getBlockDevice().getName();
        return LVM.lvSnapshot(getHost(), snapshotName, device, size, testOnly);
    }

    /** Skip initial full sync. */
    public void skipInitialFullSync(final boolean testOnly) {
        DRBD.skipInitialFullSync(getHost(),
                                 drbdVolumeInfo.getDrbdResourceInfo().getName(),
                                 drbdVolumeInfo.getName(),
                                 testOnly);
    }

    /** Force primary. */
    public void forcePrimary(final boolean testOnly) {
        DRBD.forcePrimary(getHost(),
                          drbdVolumeInfo.getDrbdResourceInfo().getName(),
                          drbdVolumeInfo.getName(),
                          testOnly);
    }

    /** Invalidate the block device. */
    void invalidateBD(final boolean testOnly) {
        DRBD.invalidate(getHost(),
                        drbdVolumeInfo.getDrbdResourceInfo().getName(),
                        drbdVolumeInfo.getName(),
                        testOnly);
    }

    /** Discard the data. */
    void discardData(final boolean testOnly) {
        DRBD.discardData(getHost(),
                         drbdVolumeInfo.getDrbdResourceInfo().getName(),
                         null,
                         testOnly);
    }

    /** Start on-line verification. */
    void verify(final boolean testOnly) {
        DRBD.verify(getHost(),
                    drbdVolumeInfo.getDrbdResourceInfo().getName(),
                    drbdVolumeInfo.getName(),
                    testOnly);
    }

    /** Resize DRBD. */
    public boolean resizeDrbd(final boolean testOnly) {
        return DRBD.resize(getHost(),
                           drbdVolumeInfo.getDrbdResourceInfo().getName(),
                           drbdVolumeInfo.getName(),
                           testOnly);
    }

    /** Returns the graphical view. */
    @Override
    public JPanel getGraphicalView() {
        if (getBlockDevice().isDrbd()) {
            getBrowser().getDrbdGraph().getDrbdInfo().setSelectedNode(this);
        }
        return getBrowser().getDrbdGraph().getDrbdInfo().getGraphicalView();
    }

    /** Set the terminal panel. */
    @Override
    protected void setTerminalPanel() {
        if (getHost() != null) {
            Tools.getGUIData().setTerminalPanel(getHost().getTerminalPanel());
        }
    }

    /** Returns the info panel. */
    @Override
    public JComponent getInfoPanel() {
        Tools.isSwingThread();
        return getInfoPanelBD();
    }

    /** Returns all parameters. */
    @Override
    public String[] getParametersFromXML() {
        final String[] params = {
                            DRBD_MD_PARAM,
                            DRBD_MD_INDEX_PARAM,
                          };
        return params;
    }

    /** Apply all fields. */
    public void apply(final boolean testOnly) {
        if (!testOnly) {
            final String[] params = getParametersFromXML();
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                    getInfoPanel();
                }
            });
            waitForInfoPanel();
            if (getBlockDevice().getMetaDisk() != null) {
                getBlockDevice().getMetaDisk().removeMetadiskOfBlockDevice(
                                                             getBlockDevice());
            }
            getBlockDevice().setNew(false);
            storeComboBoxValues(params);

            final Object o = getWidget(DRBD_MD_PARAM, null).getValue();
            if (Tools.isStringInfoClass(o)) {
                getBlockDevice().setMetaDisk(null); /* internal */
            } else {
                final BlockDevice metaDisk =
                                        ((BlockDevInfo) o).getBlockDevice();
                getBlockDevice().setMetaDisk(metaDisk);
            }
            getBrowser().getDrbdGraph().getDrbdInfo().setAllApplyButtons();
        }
    }

    /** Returns block device panel. */
    JComponent getInfoPanelBD() {
        Tools.isSwingThread();
        if (infoPanel != null) {
            infoPanelDone();
            return infoPanel;
        }
        final BlockDevInfo thisClass = this;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /**
             * Whether the whole thing should be enabled.
             */
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                final DrbdGraph drbdGraph = getBrowser().getDrbdGraph();
                drbdGraph.stopTestAnimation(getApplyButton());
                getApplyButton().setToolTipText("");
            }

            @Override
            public void mouseOver() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                getApplyButton().setToolTipText(Tools.getString(
                                         "ClusterBrowser.StartingDRBDtest"));
                getApplyButton().setToolTipBackground(Tools.getDefaultColor(
                                  "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                final DrbdGraph drbdGraph = getBrowser().getDrbdGraph();
                drbdGraph.startTestAnimation(getApplyButton(), startTestLatch);
                getBrowser().drbdtestLockAcquire();
                thisClass.setDRBDtestData(null);
                apply(true);
                final Map<Host, String> testOutput =
                                         new LinkedHashMap<Host, String>();
                try {
                    getBrowser().getDrbdGraph().getDrbdInfo().createDrbdConfig(
                                                                          true);
                    for (final Host h
                                    : getHost().getCluster().getHostsArray()) {
                        DRBD.adjustApply(h, DRBD.ALL, null, true);
                        testOutput.put(h, DRBD.getDRBDtest());
                    }
                } catch (Exceptions.DrbdConfigException dce) {
                    Tools.appError("config failed");
                }
                final DRBDtestData dtd = new DRBDtestData(testOutput);
                getApplyButton().setToolTipText(dtd.getToolTip());
                thisClass.setDRBDtestData(dtd);
                getBrowser().drbdtestLockRelease();
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.EAST);
        if (getBlockDevice().isDrbd()) {
            final String[] params = getParametersFromXML();

            addParams(optionsPanel,
                      params,
                      Tools.getDefaultSize("HostBrowser.DrbdDevLabelWidth"),
                      Tools.getDefaultSize("HostBrowser.DrbdDevFieldWidth"),
                      null);


            /* apply button */
            getApplyButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Tools.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    getApplyButton().setEnabled(false);
                                    getRevertButton().setEnabled(false);
                                }
                            });
                            getBrowser().getClusterBrowser().drbdStatusLock();
                            try {
                                getBrowser().getDrbdGraph().getDrbdInfo()
                                              .createDrbdConfig(false);
                                for (final Host h
                                    : getHost().getCluster().getHostsArray()) {
                                    DRBD.adjustApply(h, DRBD.ALL, null, false);
                                }
                            } catch (Exceptions.DrbdConfigException e) {
                                getBrowser()
                                        .getClusterBrowser()
                                                .drbdStatusUnlock();
                                Tools.appError("config failed");
                                return;
                            }
                            apply(false);
                            getBrowser().getClusterBrowser().drbdStatusUnlock();
                        }
                    });
                    thread.start();
                }
            });
            getRevertButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                revert();
                            }
                        });
                        thread.start();
                    }
                }
            );
            addApplyButton(buttonPanel);
            addRevertButton(buttonPanel);
        }

        /* info */
        final Font f = new Font("Monospaced",
                                Font.PLAIN,
                                Tools.getConfigData().scaled(12));
        final JPanel riaPanel = new JPanel();
        riaPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        riaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        riaPanel.add(super.getInfoPanel());
        mainPanel.add(riaPanel);

        mainPanel.add(optionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(new JScrollPane(mainPanel));
        infoPanel = newPanel;
        infoPanelDone();
        setApplyButtons(null, getParametersFromXML());
        return infoPanel;
    }

    /** TODO: dead code? */
    @Override
    boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Sets drbd resource for this block device. */
    void setDrbdVolumeInfo(final DrbdVolumeInfo drbdVolumeInfo) {
        this.drbdVolumeInfo = drbdVolumeInfo;
    }

    /** Returns drbd resource info in which this block device is member. */
    public DrbdVolumeInfo getDrbdVolumeInfo() {
        return drbdVolumeInfo;
    }

    /** Returns block device resource object. */
    public BlockDevice getBlockDevice() {
        return (BlockDevice) getResource();
    }

    /** Removes this block device from drbd data structures. */
    public void removeFromDrbd() {
        setDrbd(false);
        getBlockDevice().setDrbdBlockDevice(null);
        setDrbdVolumeInfo(null);
    }

    /** Returns short description of the parameter. */
    @Override
    protected String getParamShortDesc(final String param) {
        return Tools.getString(param);
    }

    /** Returns long description of the parameter. */
    @Override
    protected String getParamLongDesc(final String param) {
        return Tools.getString(param + ".Long");
    }

    /** Returns 'add drbd resource' menu item. */
    private MyMenuItem addDrbdResourceMenuItem(final BlockDevInfo oBdi,
                                               final boolean testOnly) {
        final BlockDevInfo thisClass = this;
        return new MyMenuItem(oBdi.toString(),
                              null,
                              null,
                              new AccessMode(ConfigData.AccessType.ADMIN,
                                             false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                final DrbdInfo drbdInfo =
                                    getBrowser().getDrbdGraph().getDrbdInfo();
                cleanup();
                resetInfoPanel();
                setInfoPanel(null);
                oBdi.cleanup();
                oBdi.resetInfoPanel();
                oBdi.setInfoPanel(null);
                drbdInfo.addDrbdVolume(thisClass,
                                       oBdi,
                                       true,
                                       testOnly);
            }
        };
    }

    /** Returns 'PV create' menu item. */
    private MyMenuItem getPVCreateItem() {
        final BlockDevInfo thisBDI = this;
        return new MyMenuItem(PV_CREATE_MENU_ITEM,
                              null,
                              PV_CREATE_MENU_DESCRIPTION,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                return canCreatePV();
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
                final boolean ret = thisBDI.pvCreate(DRBD.LIVE);
                if (!ret) {
                    Tools.progressIndicatorFailed(
                                Tools.getString("BlockDevInfo.PVCreate.Failed",
                                                thisBDI.getName()));
                }
                getBrowser().getClusterBrowser().updateHWInfo(
                                                           thisBDI.getHost());
            }
        };
    }

    /** Returns 'PV remove' menu item. */
    private MyMenuItem getPVRemoveItem() {
        final BlockDevInfo thisBDI = this;
        return new MyMenuItem(PV_REMOVE_MENU_ITEM,
                              null,
                              PV_REMOVE_MENU_DESCRIPTION,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                return canRemovePV();
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
                final boolean ret = thisBDI.pvRemove(false);
                if (!ret) {
                    Tools.progressIndicatorFailed(
                                Tools.getString("BlockDevInfo.PVRemove.Failed",
                                                thisBDI.getName()));
                }
                getBrowser().getClusterBrowser().updateHWInfo(
                                                            thisBDI.getHost());
            }
        };
    }

    /** Returns 'vg create' menu item. */
    private MyMenuItem getVGCreateItem() {
        final BlockDevInfo thisBDI = this;
        return new MyMenuItem(
                          VG_CREATE_MENU_ITEM,
                          null,
                          VG_CREATE_MENU_DESCRIPTION,
                          new AccessMode(ConfigData.AccessType.OP, false),
                          new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                BlockDevice bd;
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
                final VGCreate vgCreate = new VGCreate(getHost(), thisBDI);
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
    private MyMenuItem getVGRemoveItem() {
        final BlockDevInfo thisBDI = this;
        return new MyMenuItem(VG_REMOVE_MENU_ITEM,
                              null,
                              VG_REMOVE_MENU_DESCRIPTION,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                BlockDevice bd;
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
                String vg;
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
                final VGRemove vgRemove = new VGRemove(thisBDI);
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

    public String getVGName() {
        BlockDevice bd;
        if (getBlockDevice().isDrbd()) {
            bd = getBlockDevice().getDrbdBlockDevice();
            if (bd == null) {
                return null;
            }
        } else {
            bd = getBlockDevice();
        }
        final String vg = bd.getVolumeGroup();
        if (vg == null) {
            /* vg on pv */
            return bd.getVolumeGroupOnPhysicalVolume();
        } else {
            /* lv on vg */
            return vg;
        }
    }

    /** Returns 'lv create' menu item. */
    private MyMenuItem getLVCreateItem() {
        String name = LV_CREATE_MENU_ITEM;
        final String vgName = getVGName();
        if (vgName != null) {
            name += vgName;
        }
        final BlockDevInfo thisClass = this;

        final MyMenuItem mi = new MyMenuItem(
                           name,
                           null,
                           LV_CREATE_MENU_DESCRIPTION,
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                final String vg = getVGName();
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
                                                   thisClass.getVGName(),
                                                   thisClass.getBlockDevice());
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
            public void update() {
                setText1(LV_CREATE_MENU_ITEM + thisClass.getVGName());
                super.update();
            }
        };
        mi.setToolTipText(LV_CREATE_MENU_DESCRIPTION);
        return mi;
    }

    /** Returns 'LV remove' menu item. */
    private MyMenuItem getLVRemoveItem() {
        return new MyMenuItem(LV_REMOVE_MENU_ITEM,
                              null,
                              LV_REMOVE_MENU_DESCRIPTION,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean predicate() {
                return true;
            }

            @Override
            public boolean visiblePredicate() {
                return isLVM();
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
                    final boolean ret = lvRemove(false);
                    final Host host = getHost();
                    getBrowser().getClusterBrowser().updateHWInfo(host);
                }
            }
        };
    }

    /** Returns 'LV remove' menu item. */
    private MyMenuItem getLVResizeItem() {
        final BlockDevInfo thisBDI = this;
        return new MyMenuItem(LV_RESIZE_MENU_ITEM,
                              null,
                              LV_RESIZE_MENU_DESCRIPTION,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean visiblePredicate() {
                return isLVM();
            }

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void action() {
                final LVResize lvmrd = new LVResize(thisBDI);
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
    private MyMenuItem getLVSnapshotItem() {
        final BlockDevInfo thisBDI = this;
        return new MyMenuItem(LV_SNAPSHOT_MENU_ITEM,
                              null,
                              LV_SNAPSHOT_MENU_DESCRIPTION,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                return isLVM();
            }

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void action() {
                final LVSnapshot lvsd = new LVSnapshot(thisBDI);
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

    /** Creates popup for the block device. */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final BlockDevInfo thisClass = this;
        final boolean testOnly = false;
        final MyMenu repMenuItem = new MyMenu(
                        Tools.getString("HostBrowser.Drbd.AddDrbdResource"),
                        new AccessMode(ConfigData.AccessType.ADMIN, false),
                        new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                final DrbdXML dxml =
                                 getBrowser().getClusterBrowser().getDrbdXML();
                if (drbdVolumeInfo != null) {
                    return "it is already a drbd resouce";
                } else if (!getHost().isConnected()) {
                    return Host.NOT_CONNECTED_STRING;
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
                    final String noavail = getBrowser().getClusterBrowser()
                                                   .isDrbdAvailable(getHost());
                    if (noavail != null) {
                        return "DRBD installation problem: " + noavail;
                    }
                }
                return null;
            }

            @Override
            public void update() {
                super.update();
                Cluster cluster = getHost().getCluster();
                Host[] otherHosts = cluster.getHostsArray();
                final List<MyMenu> hostMenus = new ArrayList<MyMenu>();
                for (final Host oHost : otherHosts) {
                    if (oHost == getHost()) {
                        continue;
                    }
                    final MyMenu hostMenu = new MyMenu(oHost.getName(),
                                                 new AccessMode(
                                                    ConfigData.AccessType.ADMIN,
                                                    false),
                                                 new AccessMode(
                                                    ConfigData.AccessType.OP,
                                                    false)) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public String enablePredicate() {
                            final DrbdXML dxml =
                                 getBrowser().getClusterBrowser().getDrbdXML();
                            if (!oHost.isConnected()) {
                                return Host.NOT_CONNECTED_STRING;
                            } else if (!oHost.isDrbdLoaded()) {
                                return "drbd is not loaded";
                            } else {
                                final String noavail =
                                        getBrowser().getClusterBrowser()
                                                    .isDrbdAvailable(getHost());
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
                        public void update() {
                            super.update();
                            Tools.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    removeAll();
                                }
                            });
                            Set<BlockDevInfo> blockDevInfos =
                                        oHost.getBrowser().getBlockDevInfos();
                            List<BlockDevInfo> blockDevInfosS =
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
                                                                testOnly));
                                }
                                if (oBdi.getName().equals(
                                            getBlockDevice().getName())) {
                                    addSeparator();
                                }
                            }
                        }
                    };
                    hostMenu.update();
                    hostMenus.add(hostMenu);
                }
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        removeAll();
                        for (final MyMenu hostMenu : hostMenus) {
                            add(hostMenu);
                        }
                    }
                });
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
                           NO_HARDDISK_ICON_LARGE,
                           Tools.getString("HostBrowser.Drbd.Detach.ToolTip"),

                           Tools.getString("HostBrowser.Drbd.Attach"),
                           HARDDISK_DRBD_ICON_LARGE,
                           Tools.getString("HostBrowser.Drbd.Attach.ToolTip"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
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
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && drbdVolumeInfo.getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdVolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return DrbdVolumeInfo.IS_SYNCING_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    if (this.getText().equals(
                                Tools.getString("HostBrowser.Drbd.Attach"))) {
                        attach(testOnly);
                    } else {
                        detach(testOnly);
                    }
                }
            };
        final ClusterBrowser wi = getBrowser().getClusterBrowser();
        if (wi != null) {
            final ClusterBrowser.DRBDMenuItemCallback attachItemCallback =
                            wi.new DRBDMenuItemCallback(attachMenu, getHost()) {
                @Override
                public void action(final Host host) {
                    if (isDiskless(false)) {
                        attach(true);
                    } else {
                        detach(true);
                    }
                }
            };
            addMouseOverListener(attachMenu, attachItemCallback);
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
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return isConnectedOrWF(testOnly);
                }

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && drbdVolumeInfo.getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdVolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isSyncing()
                        || ((getBlockDevice().isPrimary()
                            && getBlockDevice().isSyncSource())
                            || (getOtherBlockDevInfo().getBlockDevice().
                                                                isPrimary()
                                && getBlockDevice().isSyncTarget()))) {
                        return null;
                    } else {
                        return DrbdVolumeInfo.IS_SYNCING_STRING;
                    }
                }

                @Override
                public void action() {
                    if (this.getText().equals(
                            Tools.getString("HostBrowser.Drbd.Connect"))) {
                        connect(testOnly);
                    } else {
                        disconnect(testOnly);
                    }
                }
            };
        if (wi != null) {
            final ClusterBrowser.DRBDMenuItemCallback connectItemCallback =
                               wi.new DRBDMenuItemCallback(connectMenu,
                                                           getHost()) {
                @Override
                public void action(final Host host) {
                    if (isConnectedOrWF(false)) {
                        disconnect(true);
                    } else {
                        connect(true);
                    }
                }
            };
            addMouseOverListener(connectMenu, connectItemCallback);
        }
        items.add(connectMenu);

        /* set primary */
        final MyMenuItem setPrimaryItem =
            new MyMenuItem(Tools.getString(
                                  "HostBrowser.Drbd.SetPrimaryOtherSecondary"),
                           null,
                           Tools.getString(
                                  "HostBrowser.Drbd.SetPrimaryOtherSecondary"),

                           Tools.getString("HostBrowser.Drbd.SetPrimary"),
                           null,
                           Tools.getString("HostBrowser.Drbd.SetPrimary"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return getBlockDevice().isSecondary()
                         && getOtherBlockDevInfo().getBlockDevice().isPrimary();
                }

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && drbdVolumeInfo.getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdVolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isSecondary()) {
                        return "cannot do that to the primary";
                    }
                    return null;
                }

                @Override
                public void action() {
                    BlockDevInfo oBdi = getOtherBlockDevInfo();
                    if (oBdi != null && oBdi.getBlockDevice().isPrimary()
                        && !"yes".equals(
                            drbdVolumeInfo.getDrbdResourceInfo().getParamSaved(
                                                       ALLOW_TWO_PRIMARIES))) {
                        oBdi.setSecondary(testOnly);
                    }
                    setPrimary(testOnly);
                }
            };
        items.add(setPrimaryItem);

        /* set secondary */
        final MyMenuItem setSecondaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetSecondary"),
                           null,
                           Tools.getString(
                                "HostBrowser.Drbd.SetSecondary.ToolTip"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && drbdVolumeInfo.getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdVolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isPrimary()) {
                        return "cannot do that to the secondary";
                    }
                    return null;
                }

                @Override
                public void action() {
                    setSecondary(testOnly);
                }
            };
        //enableMenu(setSecondaryItem, false);
        items.add(setSecondaryItem);

        /* force primary */
        final MyMenuItem forcePrimaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ForcePrimary"),
                           null,
                           Tools.getString("HostBrowser.Drbd.ForcePrimary"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && drbdVolumeInfo.getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdVolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    forcePrimary(testOnly);
                }
            };
        items.add(forcePrimaryItem);

        /* invalidate */
        final MyMenuItem invalidateItem =
            new MyMenuItem(
                   Tools.getString("HostBrowser.Drbd.Invalidate"),
                   null,
                   Tools.getString("HostBrowser.Drbd.Invalidate.ToolTip"),
                   new AccessMode(ConfigData.AccessType.ADMIN, true),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && drbdVolumeInfo.getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdVolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return DrbdVolumeInfo.IS_SYNCING_STRING;
                    }
                    if (getDrbdVolumeInfo().isVerifying()) {
                        return DrbdVolumeInfo.IS_VERIFYING_STRING;
                    }
                    return null;
                    //return !getBlockDevice().isSyncing()
                    //       && !getDrbdVolumeInfo().isVerifying();
                }

                @Override
                public void action() {
                    invalidateBD(testOnly);
                }
            };
        items.add(invalidateItem);

        /* resume / pause sync */
        final MyMenuItem resumeSyncItem =
            new MyMenuItem(
                       Tools.getString("HostBrowser.Drbd.ResumeSync"),
                       null,
                       Tools.getString("HostBrowser.Drbd.ResumeSync.ToolTip"),

                       Tools.getString("HostBrowser.Drbd.PauseSync"),
                       null,
                       Tools.getString("HostBrowser.Drbd.PauseSync.ToolTip"),
                       new AccessMode(ConfigData.AccessType.OP, true),
                       new AccessMode(ConfigData.AccessType.OP, false)) {
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
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && drbdVolumeInfo.getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdVolumeInfo.IS_USED_BY_CRM_STRING;
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
                        resumeSync(testOnly);
                    } else {
                        pauseSync(testOnly);
                    }
                }
            };
        items.add(resumeSyncItem);

        /* resize */
        final MyMenuItem resizeItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.Resize"),
                           null,
                           Tools.getString("HostBrowser.Drbd.Resize.ToolTip"),
                           new AccessMode(ConfigData.AccessType.ADMIN, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && drbdVolumeInfo.getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdVolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return DrbdVolumeInfo.IS_SYNCING_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    resizeDrbd(testOnly);
                }
            };
        items.add(resizeItem);

        /* discard my data */
        final MyMenuItem discardDataItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.DiscardData"),
                           null,
                           Tools.getString(
                                     "HostBrowser.Drbd.DiscardData.ToolTip"),
                           new AccessMode(ConfigData.AccessType.ADMIN, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && drbdVolumeInfo.getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdVolumeInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return DrbdVolumeInfo.IS_SYNCING_STRING;
                    }
                    //if (isConnected(testOnly)) { // ? TODO: check this
                    //    return "is connected";
                    //}
                    if (getBlockDevice().isPrimary()) {
                        return "cannot do that to the primary";
                    }
                    return null;
                    //return !getBlockDevice().isSyncing()
                    //       && !isConnected(testOnly)
                    //       && !getBlockDevice().isPrimary();
                }

                @Override
                public void action() {
                    discardData(testOnly);
                }
            };
        items.add(discardDataItem);

        /* proxy up/down */
        final MyMenuItem proxyItem =
            new MyMenuItem(Tools.getString("BlockDevInfo.Drbd.ProxyDown"),
                           null,
                           getMenuToolTip("DRBD.proxyDown"),
                           Tools.getString("BlockDevInfo.Drbd.ProxyUp"),
                           null,
                           getMenuToolTip("DRBD.proxyUp"),
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           new AccessMode(ConfigData.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return getDrbdVolumeInfo().getDrbdResourceInfo().isProxy(
                                                                    getHost());
                }

                @Override
                public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    final DrbdResourceInfo dri =
                                          drbdVolumeInfo.getDrbdResourceInfo();
                    final Host pHost =
                         dri.getProxyHost(getHost(), !DrbdResourceInfo.WIZARD);
                    if (pHost == null) {
                        return "not a proxy";
                    }
                    if (!pHost.isConnected()) {
                        return Host.NOT_CONNECTED_STRING;
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
                    final DrbdResourceInfo dri =
                                          drbdVolumeInfo.getDrbdResourceInfo();
                    final Host pHost =
                         dri.getProxyHost(getHost(), !DrbdResourceInfo.WIZARD);
                    if (pHost == null) {
                        return false;
                    }
                    if (getBlockDevice().isDrbd()) {
                        return pHost.isDrbdProxyUp(
                             drbdVolumeInfo.getDrbdResourceInfo().getName());
                    } else {
                        return true;
                    }
                }

                @Override
                public void action() {
                    final DrbdResourceInfo dri =
                                          drbdVolumeInfo.getDrbdResourceInfo();
                    final Host pHost =
                         dri.getProxyHost(getHost(), !DrbdResourceInfo.WIZARD);
                    if (pHost.isDrbdProxyUp(
                             drbdVolumeInfo.getDrbdResourceInfo().getName())) {
                        DRBD.proxyDown(
                                pHost,
                                drbdVolumeInfo.getDrbdResourceInfo().getName(),
                                drbdVolumeInfo.getName(),
                                testOnly);
                    } else {
                        DRBD.proxyUp(
                                pHost,
                                drbdVolumeInfo.getDrbdResourceInfo().getName(),
                                drbdVolumeInfo.getName(),
                                testOnly);
                    }
                    getBrowser().getClusterBrowser().updateProxyHWInfo(pHost);
                }
            };
        items.add(proxyItem);

        /* view log */
        final MyMenuItem viewDrbdLogItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewDrbdLog"),
                           LOGFILE_ICON,
                           null,
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {
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
                    String device = getDrbdVolumeInfo().getDevice();
                    DrbdLog l = new DrbdLog(getHost(), device);
                    l.showDialog();
                }
            };
        items.add(viewDrbdLogItem);

        return items;
    }

    /** Returns how much of the block device is used. */
    @Override
    public int getUsed() {
        final DrbdVolumeInfo dvi = drbdVolumeInfo;
        if (dvi != null) {
            return dvi.getUsed();
        }
        return getBlockDevice().getUsed();
    }

    /** Returns text that appears above the icon. */
    public String getIconTextForGraph(final boolean testOnly) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Drbd.NoInfoAvailable");
        }
        if (getBlockDevice().isDrbd()) {
            return getBlockDevice().getNodeState();
        }
        return null;
    }

    @Override
    public String getMainTextForGraph() {
        if (!isLVM()) {
            final String vg = getBlockDevice().getVolumeGroupOnPhysicalVolume();
            if (vg != null && !"".equals(vg)) {
                return "VG " + vg;
            }
        }
        return getName();
    }

    /** Returns text that appears in the corner of the drbd graph. */
    public Subtext getRightCornerTextForDrbdGraph(final boolean testOnly) {
         String vg = null;
         if (isLVM()) {
             vg = getBlockDevice().getVolumeGroup();
         } else {
             vg = getBlockDevice().getVolumeGroupOnPhysicalVolume();
         }

         if (getBlockDevice().isDrbdMetaDisk()) {
             return METADISK_SUBTEXT;
         } else if (getBlockDevice().isSwap()) {
             return SWAP_SUBTEXT;
         } else if (getBlockDevice().getMountedOn() != null) {
             return MOUNTED_SUBTEXT;
         } else if (getBlockDevice().isDrbd()) {
             String s = getBlockDevice().getName();
             // TODO: cache that
             if (s.length() > MAX_RIGHT_CORNER_STRING_LENGTH) {
                 s = "..." + s.substring(
                               s.length()
                               - MAX_RIGHT_CORNER_STRING_LENGTH + 3,
                               s.length());
             }
             if (getBlockDevice().isDrbdPhysicalVolume()) {
                 final String drbdVG = getBlockDevice().getDrbdBlockDevice()
                                             .getVolumeGroupOnPhysicalVolume();
                 if (drbdVG != null && !"".equals(drbdVG)) {
                     s = s + " VG:" + drbdVG;
                 } else {
                     s += " PV";
                 }
             }
             return new Subtext(s, Color.BLUE, Color.BLACK);
         } else if (vg != null && !"".equals(vg)) {
             if (isLVM()) {
                 return new Subtext("LV in " + vg, Color.BLUE, Color.GREEN);
             } else {
                 return new Subtext(getName(), Color.BLUE, Color.GREEN);
             }
         } else if (getBlockDevice().isPhysicalVolume()) {
             return PHYSICAL_VOLUME_SUBTEXT;
         }
         return null;
    }

    /** Returns whether this device is connected via drbd. */
    public boolean isConnected(final boolean testOnly) {
        final DRBDtestData dtd = getDRBDtestData();
        if (testOnly && dtd != null) {
            return isConnectedTest(dtd) && !isWFConnection(testOnly);
        } else {
            return getBlockDevice().isConnected();
        }
    }

    /** Returns whether this device is connected or wait-for-c via drbd. */
    boolean isConnectedOrWF(final boolean testOnly) {
        final DRBDtestData dtd = getDRBDtestData();
        if (testOnly && dtd != null) {
            return isConnectedTest(dtd);
        } else {
            return getBlockDevice().isConnectedOrWF();
        }
    }

    /** Returns whether this device is in wait-for-connection state. */
    public boolean isWFConnection(final boolean testOnly) {
        final DRBDtestData dtd = getDRBDtestData();
        if (testOnly && dtd != null) {
            return isConnectedOrWF(testOnly)
                   && isConnectedTest(dtd)
                   && !getOtherBlockDevInfo().isConnectedTest(dtd);
        } else {
            return getBlockDevice().isWFConnection();
        }
    }

    /** Returns whether this device will be disconnected. */
    boolean isConnectedTest(final DRBDtestData dtd) {
        return dtd.isConnected(getHost(),
                               drbdVolumeInfo.getDrbdResourceInfo().getName())
               || (!dtd.isDisconnected(
                               getHost(),
                               drbdVolumeInfo.getDrbdResourceInfo().getName())
                   && getBlockDevice().isConnectedOrWF());
    }

    /** Returns whether this device is diskless. */
    public boolean isDiskless(final boolean testOnly) {
        final DRBDtestData dtd = getDRBDtestData();
        final DrbdVolumeInfo dvi = drbdVolumeInfo;
        if (testOnly && dtd != null && dvi != null) {
            return dtd.isDiskless(getHost(), drbdVolumeInfo.getDevice())
                   || (!dtd.isAttached(getHost(),
                                       drbdVolumeInfo.getDevice())
                       && getBlockDevice().isDiskless());
        } else {
            return getBlockDevice().isDiskless();
        }
    }

    /** Returns drbd test data. */
    DRBDtestData getDRBDtestData() {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getDRBDtestData();
    }

    /** Sets drbd test data. */
    void setDRBDtestData(final DRBDtestData drbdtestData) {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return;
        }
        b.setDRBDtestData(drbdtestData);
    }

    /**
     * Return whether the block device is unimportant (for the GUI), e.g.
     * cdrom or swap.
     */
    private static boolean isUnimportant(final String name,
                                         final String type,
                                         final String mountedOn) {
        return "swap".equals(type)
               || "/".equals(mountedOn)
               || "/boot".equals(mountedOn)
               || name.startsWith("/dev/cdrom")
               || name.startsWith("/dev/fd")
               || name.startsWith("/dev/sr")
               || name.endsWith("/root")
               || name.endsWith("/lv_root")
               || name.endsWith("/lv_swap")
               || name.contains("/swap");
    }

    /** Compares ignoring case and using drbd device names if available. */
    @Override
    public int compareTo(final Info o) {
        if (o == null) {
            return -1;
        }
        if (o == this) {
            return 0;
        }
        String name;
        String oName;
        int volume = 0;
        int oVolume = 0;
        final DrbdVolumeInfo dvi = getDrbdVolumeInfo();
        if (getBlockDevice().isDrbd() && dvi != null) {
            name = dvi.getDrbdResourceInfo().getName();
            final String v = dvi.getName();
            if (Tools.isNumber(v)) {
                volume = Integer.parseInt(v);
            }
        } else {
            name = getName();
        }
        final BlockDevInfo obdi = (BlockDevInfo) o;
        final DrbdVolumeInfo odvi = obdi.getDrbdVolumeInfo();
        if (obdi.getBlockDevice().isDrbd() && odvi != null) {
            oName = odvi.getDrbdResourceInfo().getName();
            final String v = odvi.getName();
            if (Tools.isNumber(v)) {
                oVolume = Integer.parseInt(v);
            }
        } else {
            oName = ((BlockDevInfo) o).getName();
        }
        /* drbds up */
        if (getBlockDevice().isDrbd()
            && !obdi.getBlockDevice().isDrbd()) {
            return -1;
        }
        if (!getBlockDevice().isDrbd()
            && obdi.getBlockDevice().isDrbd()) {
            return 1;
        }

        /* cdroms, swap etc down */
        final boolean unimportant =
                                isUnimportant(name,
                                              getBlockDevice().getFsType(),
                                              getBlockDevice().getMountedOn());
        final boolean oUnimportant =
                           isUnimportant(oName,
                                         obdi.getBlockDevice().getFsType(),
                                         obdi.getBlockDevice().getMountedOn());
        if (unimportant && !oUnimportant) {
            return 1;
        }
        if (!unimportant && oUnimportant) {
            return -1;
        }

        /* volume groups down */
        if (getBlockDevice().isVolumeGroupOnPhysicalVolume()
            && !obdi.getBlockDevice().isVolumeGroupOnPhysicalVolume()) {
            return 1;
        }
        if (!getBlockDevice().isVolumeGroupOnPhysicalVolume()
            && obdi.getBlockDevice().isVolumeGroupOnPhysicalVolume()) {
            return -1;
        }
        final int ret = name.compareToIgnoreCase(oName);
        if (ret == 0) {
            return volume - oVolume;
        }
        return ret;
    }

    /** Sets stored parameters. */
    public void setParameters(final String resName) {
        Tools.isSwingThread();
        getBlockDevice().setNew(false);

        final ClusterBrowser clusterBrowser = getBrowser().getClusterBrowser();
        if (clusterBrowser == null) {
            return;
        }
        final DrbdVolumeInfo dvi = drbdVolumeInfo;
        if (dvi == null) {
            return;
        }
        final DrbdXML dxml = clusterBrowser.getDrbdXML();
        final String hostName = getHost().getName();
        final DrbdGraph drbdGraph = getBrowser().getDrbdGraph();
        String value = null;
        final String volumeNr = dvi.getName();
        for (final String param : getParametersFromXML()) {
            if (DRBD_MD_PARAM.equals(param)) {
                value = dxml.getMetaDisk(hostName, resName, volumeNr);
                if (!"internal".equals(value)) {
                    final BlockDevInfo mdI =
                                   drbdGraph.findBlockDevInfo(hostName, value);
                    if (mdI != null) {
                        getBlockDevice().setMetaDisk(mdI.getBlockDevice());
                    }
                }
            } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
                value = dxml.getMetaDiskIndex(hostName, resName, volumeNr);
            }
            final String defaultValue = getParamDefault(param);
            if (value == null) {
                value = defaultValue;
            }
            if (value == null) {
                value = "";
            }
            final String oldValue = getParamSaved(param);
            final Widget wi = getWidget(param, null);
            if (!Tools.areEqual(value, oldValue)) {
                getResource().setValue(param, value);
                if (wi != null) {
                    wi.setValueAndWait(value);
                }
            }
        }
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    @Override
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params) {
        return checkResourceFieldsChanged(param, params, false, false, false);
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    boolean checkResourceFieldsChanged(
                                   final String param,
                                   final String[] params,
                                   final boolean fromDrbdInfo,
                                   final boolean fromDrbdResourceInfo,
                                   final boolean fromDrbdVolumeInfo) {
        final DrbdVolumeInfo dvi = getDrbdVolumeInfo();
        if (dvi != null
            && !fromDrbdVolumeInfo
            && !fromDrbdResourceInfo
            && !fromDrbdInfo) {
            dvi.setApplyButtons(null, dvi.getParametersFromXML());
        }
        return super.checkResourceFieldsChanged(param, params);
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override
    public boolean checkResourceFieldsCorrect(final String param,
                                              final String[] params) {
        return checkResourceFieldsCorrect(param, params, false, false, false);
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params,
                                       final boolean fromDrbdInfo,
                                       final boolean fromDrbdResourceInfo,
                                       final boolean fromDrbdVolumeInfo) {
        boolean correct = true;
        final DrbdXML dxml = getBrowser().getClusterBrowser().getDrbdXML();
        if (dxml != null && dxml.isDrbdDisabled()) {
            correct = false;
        }
        return super.checkResourceFieldsCorrect(param, params) && correct;
    }

    /** Returns whether this block device is a volume group in LVM. */
    public boolean isLVM() {
        return getBlockDevice().getVolumeGroup() != null;
    }

    /** Returns how much is free space in a volume group. */
    public Long getFreeInVolumeGroup() {
        return getHost().getFreeInVolumeGroup(
                                           getBlockDevice().getVolumeGroup());
    }

    /** Returns true if this is the first volume in the resource. Returns true
     * if this is not a DRBD resource. */
    public boolean isFirstDrbdVolume() {
        if (!getBlockDevice().isDrbd()) {
            return true;
        }
        final Set<DrbdVolumeInfo> drbdVolumes =
                    getDrbdVolumeInfo().getDrbdResourceInfo().getDrbdVolumes();
        if (drbdVolumes == null || drbdVolumes.isEmpty()) {
            return true;
        }
        return drbdVolumes.iterator().next() == getDrbdVolumeInfo();
    }

    /** Return whether two primaries are allowed. */
    boolean allowTwoPrimaries() {
        final DrbdResourceInfo dri = drbdVolumeInfo.getDrbdResourceInfo();
        return "yes".equals(dri.getParamSaved(ALLOW_TWO_PRIMARIES));
    }

    /**
     * Proxy status for graph, null if there's no proxy configured for the
     * resource.
     */
    public String getProxyStateForGraph(final boolean testOnly) {
        final DrbdResourceInfo dri = drbdVolumeInfo.getDrbdResourceInfo();
        final Host pHost =
                         dri.getProxyHost(getHost(), !DrbdResourceInfo.WIZARD);
        if (dri.isProxy(getHost())) {
            if (pHost.isConnected()) {
                if (pHost.isDrbdProxyUp(dri.getName())) {
                    return PROXY_UP;
                } else {
                    return PROXY_DOWN;
                }
            } else {
                if (drbdVolumeInfo.isConnected(testOnly)) {
                    return PROXY_UP;
                } else {
                    return pHost.getName();
                }
            }
        }
        return null;
    }

    /** Tool tip for menu items. */
    private String getMenuToolTip(final String cmd) {
        if (getBlockDevice().isDrbd()) {
            return DRBD.getDistCommand(
                            cmd,
                            getHost(),
                            drbdVolumeInfo.getDrbdResourceInfo().getName(),
                            drbdVolumeInfo.getName()).replaceAll("@.*?@", "");
        } else {
            return null;
        }
    }

    /** Whether PV can be created on this BD. */
    public boolean canCreatePV() {
        return !isLVM()
                && !getBlockDevice().isPhysicalVolume()
                && !getBlockDevice().isDrbdPhysicalVolume();
    }

    /** Whether PV can be removed from this BD. */
    final boolean canRemovePV() {
        BlockDevice bd;
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

    /** Whether VG can be removed. */
    final boolean canRemoveVG() {
        BlockDevice bd;
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
        if (!bd.isVolumeGroupOnPhysicalVolume()) {
            return false;
        }
        final String vg = bd.getVolumeGroupOnPhysicalVolume();
        if (getHost().getLogicalVolumesFromVolumeGroup(vg) != null) {
            return false;
        }
        return true;
    }
}
