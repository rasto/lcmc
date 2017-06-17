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

package lcmc.drbd.ui.resource;

import com.google.common.base.Optional;
import lcmc.Exceptions;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.ColorText;
import lcmc.common.domain.ResourceValue;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.EditableInfo;
import lcmc.common.ui.Info;
import lcmc.common.ui.MainPanel;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.drbd.domain.BlockDevice;
import lcmc.drbd.domain.DRBDtestData;
import lcmc.drbd.domain.DrbdXml;
import lcmc.drbd.service.DRBD;
import lcmc.drbd.ui.DrbdGraph;
import lcmc.host.domain.Host;
import lcmc.host.ui.HostBrowser;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.lvm.service.LVM;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class holds info data for a block device.
 */
@Named
public class BlockDevInfo extends EditableInfo {
    private static final Logger LOG = LoggerFactory.getLogger(BlockDevInfo.class);
    private static final Value DRBD_MD_TYPE_FLEXIBLE = new StringValue("Flexible");
    private static final String DRBD_MD_PARAM = "DrbdMetaDisk";
    private static final String DRBD_MD_INDEX_PARAM = "DrbdMetaDiskIndex";
    public static final ImageIcon HARDDISK_ICON_LARGE = Tools.createImageIcon(
                           Tools.getDefault("BlockDevInfo.HarddiskIconLarge"));
    public static final ImageIcon HARDDISK_DRBD_ICON_LARGE =
                    Tools.createImageIcon(Tools.getDefault("BlockDevInfo.HarddiskDRBDIconLarge"));
    public static final ImageIcon NO_HARDDISK_ICON_LARGE =
                    Tools.createImageIcon(Tools.getDefault("BlockDevInfo.NoHarddiskIconLarge"));
    public static final ImageIcon HARDDISK_ICON = Tools.createImageIcon(Tools.getDefault("BlockDevInfo.HarddiskIcon"));
    private static final ColorText METADISK_COLOR_TEXT = new ColorText("meta-disk", Color.BLUE, Color.BLACK);
    private static final ColorText SWAP_COLOR_TEXT = new ColorText("swap", Color.BLUE, Color.BLACK);
    private static final ColorText MOUNTED_COLOR_TEXT = new ColorText("mounted", Color.BLUE, Color.BLACK);
    private static final ColorText PHYSICAL_VOLUME_COLOR_TEXT = new ColorText("PV", Color.BLUE, Color.GREEN);
    private static final int MAX_RIGHT_CORNER_STRING_LENGTH = 28;
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String NO_DRBD_RESOURCE_STRING = "it is not a drbd resource";
    public static final String ALLOW_TWO_PRIMARIES = "allow-two-primaries";
    public static final String PROXY_UP = "Proxy Up";
    private static final String PROXY_DOWN = "Proxy Down";

    private static final String BY_UUID_PATH = "/dev/disk/by-uuid/";
    @Inject
    private MainPanel mainPanel;
    @Inject
    private BlockDevMenu blockDevMenu;
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private TreeMenuController treeMenuController;

    public void init(final String name, final BlockDevice blockDevice, final Browser browser) {
        super.einit(Optional.<ResourceValue>of(blockDevice), name, browser);
    }

    /**
     * Return whether the block device is unimportant (for the GUI), e.g.
     * cdrom or swap.
     */
    private static boolean isUnimportant(final String name, final String type, final String mountedOn) {
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
    /** DRBD resource in which this block device is member. */
    private VolumeInfo volumeInfo;
    /** Map from parameters to the fact if the last entered value was
     * correct. */
    private final Map<String, Boolean> paramCorrectValueMap = new HashMap<String, Boolean>();
    /** Cache for the info panel. */
    private JComponent infoPanel = null;

    /**
     * Returns object of the other block device that is connected via drbd
     * to this block device.
     */
    public BlockDevInfo getOtherBlockDevInfo() {
        final VolumeInfo dvi = volumeInfo;
        if (dvi == null) {
            return null;
        }
        return dvi.getOtherBlockDevInfo(this);
    }

    @Override
    public HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    void setInfoPanel(final JComponent infoPanel) {
        this.infoPanel = infoPanel;
    }

    /**
     * Remove this block device.
     *
     * TODO: check this
     */
    @Override
    public void removeMyself(final Application.RunMode runMode) {
        getBlockDevice().setValue(DRBD_MD_PARAM, null);
        getBlockDevice().setValue(DRBD_MD_INDEX_PARAM, null);
        super.removeMyself(runMode);
        if (Application.isLive(runMode)) {
            treeMenuController.removeNode(getNode());
        }
        infoPanel = null;
    }

    public Host getHost() {
        return getBrowser().getHost();
    }

    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return BlockDevInfo.HARDDISK_ICON;
    }

    /** Returns info of this block device as string. */
    @Override
    public String getInfo() {
        final StringBuilder ret = new StringBuilder(120);
        ret.append("Host            : ")
           .append(getHost().getName())
           .append("\nDevice          : ")
           .append(getBlockDevice().getName())
           .append("\nMeta disk       : ")
           .append(getBlockDevice().isDrbdMetaDisk())
           .append("\nSize            : ")
           .append(getBlockDevice().getBlockSize())
           .append(" blocks");
        if (getBlockDevice().getMountedOn() == null) {
            ret.append("\nnot mounted");
        } else {
            ret.append("\nMounted on      : ")
               .append(getBlockDevice().getMountedOn())
               .append("\nType            : ")
               .append(getBlockDevice().getFsType());
            if (getUsed() >= 0) {
                ret.append("\nUsed:           : ")
                   .append(getUsed())
                   .append('%');
            }
        }
        if (getBlockDevice().isDrbd()) {
            ret.append("\nConnection state: ")
               .append(getBlockDevice().getConnectionState())
               .append("\nNode state      : ")
               .append(getBlockDevice().getNodeState())
               .append("\nDisk state      : ")
               .append(getBlockDevice().getDiskState())
               .append('\n');
        }
        return ret.toString();
    }

    /** Append hierarchy  of block devices in the string buffer using HTML. */
    private void appendBlockDeviceHierarchy(final BlockDevice bd, final StringBuilder tt, final int shift) {
        String tab = "";
        for (int i = 0; i != shift; ++i) {
            tab += "    ";
        }
        /* physical volumes */
        String vg = null;
        String selectedPV = null;
        if (bd.isVolumeGroupOnPhysicalVolume()) {
            vg = bd.getVgOnPhysicalVolume();
            selectedPV = bd.getName();
        }  else if (isLVM()) {
            vg = bd.getVolumeGroup();
        }
        if (vg != null) {
            for (final BlockDevice pv : getHost().getHostParser().getPhysicalVolumes(vg)) {
                if (pv.getName().equals(selectedPV)) {
                    tt.append("<b>").append(tab).append(pv).append("</b>");
                } else {
                    tt.append(tab).append(pv);
                }
                tt.append('\n');
            }
        }
        /* volume groups */
        if (vg != null) {
            String selectedLV = null;
            if (bd.isVolumeGroupOnPhysicalVolume()) {
                tt.append("<b>")
                  .append("    ")
                  .append(tab)
                  .append(vg)
                  .append("</b>\n");
            } else if (isLVM()) {
                tt.append("    ").append(tab).append(vg).append('\n');
                selectedLV = bd.getName();
            }
            final Set<String> lvs = getHost().getHostParser().getLogicalVolumesFromVolumeGroup(vg);
            if (lvs != null) {
                for (final String lv : lvs) {
                    tt.append("        ").append(tab);
                    final String lvName = "/dev/" + vg + '/' + lv;
                    if (lvName.equals(selectedLV)) {
                        if (bd.isDrbd()) {
                            tt.append(lv).append('\n');
                            final BlockDevice drbdBD = bd.getDrbdBlockDevice();
                            if (drbdBD != null) {
                                appendBlockDeviceHierarchy(drbdBD, tt, shift + 3);
                            }
                        } else {
                            tt.append("<b>").append(lv).append("</b>\n");
                        }
                    } else {
                        tt.append(lv).append('\n');
                    }
                }
            }
        } else {
            final BlockDevice drbdBD = bd.getDrbdBlockDevice();
            if (drbdBD != null) {
                tt.append(tab).append(bd.getName()).append('\n');
                appendBlockDeviceHierarchy(drbdBD, tt, shift + 1);
            } else {
                tt.append("<b>")
                  .append(tab)
                  .append(bd.getName())
                  .append("</b>\n");
            }
        }
    }

    @Override
    public String getToolTipForGraph(final Application.RunMode runMode) {
        final StringBuilder tt = new StringBuilder(60);
        final BlockDevice bd = getBlockDevice();

        tt.append("<pre>");
        appendBlockDeviceHierarchy(bd, tt, 0);
        tt.append("</pre>");
        if (bd.isDrbdMetaDisk()) {
            tt.append(" (Meta Disk)\n");
            for (final BlockDevice mb : getBlockDevice().getMetaDiskOfBlockDevices()) {
                tt.append("&nbsp;&nbsp;of ").append(mb.getName()).append('\n');
            }
        }

        final String uuid = bd.getDiskUuid();
        tt.append("\n<table>");
        if (uuid != null && uuid.startsWith(BY_UUID_PATH)) {
            tt.append("<tr><td><b>UUID:</b></td><td>")
              .append(uuid.substring(18))
              .append("</td></tr>");
        }
        String label = "ID:";
        for (final String diskId : bd.getDiskIds()) {
            if (diskId.length() > 16) {
                tt.append("<tr><td><b>")
                  .append(label)
                  .append("</b></td><td>")
                  .append(diskId.substring(16))
                  .append("</td></tr>");
                label = "";
            }
        }
        tt.append("\n</table>");

        if (bd.isDrbd()) {
            if (getHost().isDrbdStatusOk()) {
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

                tt.append("\n<table><tr><td><b>cs:</b></td><td>")
                  .append(cs)
                  .append("</td></tr><tr><td><b>ro:</b></td><td>")
                  .append(st)
                  .append("</td></tr><tr><td><b>ds:</b></td><td>")
                  .append(ds)
                  .append("</td></tr></table>");
            } else {
                tt.append('\n')
                  .append(Tools.getString("HostBrowser.Hb.NoInfoAvailable"));
            }
        }
        return tt.toString();
    }

    String drbdBDConfig(final String resource,
                        final String drbdDevice,
                        final boolean volumesAvailable) throws Exceptions.DrbdConfigException {
        if (drbdDevice == null) {
            throw new Exceptions.DrbdConfigException(
                                       "Drbd device not defined for host "
                                       + getHost().getName()
                                       + " (" + resource + ')');
        }
        if (getBlockDevice().getName() == null) {
            throw new Exceptions.DrbdConfigException(
                                       "Block device not defined for host "
                                       + getHost().getName()
                                       + " (" + resource + ')');
        }

        final StringBuilder config = new StringBuilder(120);
        final String tabs;
        if (volumesAvailable) {
            tabs = "\t\t\t";
        } else {
            tabs = "\t\t";
        }
        config.append(tabs)
              .append("device\t\t")
              .append(drbdDevice)
              .append(";\n")
              .append(tabs)
              .append("disk\t\t");
        final String backingDisk = getBlockDevice().getDrbdBackingDisk();
        if (backingDisk == null) {
            config.append(getBlockDevice().getName());
        } else {
            config.append(backingDisk);
        }
        config.append(";\n")
              .append(tabs)
              .append(getBlockDevice().getMetaDiskString(
                   getComboBoxValue(DRBD_MD_PARAM).getValueForConfig(),
                   getComboBoxValue(DRBD_MD_INDEX_PARAM).getValueForConfig()))
              .append(';');
        return config.toString();
    }

    /** Sets whether this block device is drbd. */
    void setDrbd(final boolean drbd) {
        getBlockDevice().setDrbd(drbd);
    }

    @Override
    protected String getSection(final String param) {
        return getBlockDevice().getSection(param);
    }

    @Override
    protected Value[] getPossibleChoices(final String param) {
        return getBlockDevice().getPossibleChoices(param);
    }

    protected Object getDefaultValue(final String param) {
        return "<select>";
    }

    @Override
    protected Widget createWidget(final String param, final String prefix, final int width) {
        final Widget paramWi;
        if (DRBD_MD_INDEX_PARAM.equals(param)) {
            paramWi = super.createWidget(param, prefix, width);
        } else {
            final Widget gwi = super.createWidget(param, prefix, width);
            paramWi = gwi;
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    gwi.setEditable(false);
                }
            });
        }
        return paramWi;
    }

    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        boolean ret = true;
        if (newValue.isNothingSelected() && isRequired(param)) {
            ret = false;
        } else if (DRBD_MD_PARAM.equals(param)) {
            if (infoPanel != null) {
                if (!getHost().getHostParser().getWaitForServerStatusLatch()) {
                    final boolean internal = "internal".equals(newValue.getValueForConfig());
                    final Widget ind = getWidget(DRBD_MD_INDEX_PARAM, null);
                    final Widget indW = getWidget(DRBD_MD_INDEX_PARAM,
                    Widget.WIZARD_PREFIX);
                    if (internal) {
                        ind.setValue(DRBD_MD_TYPE_FLEXIBLE);
                        if (indW != null) {
                            indW.setValue(DRBD_MD_TYPE_FLEXIBLE);
                        }
                    }
                    swingUtils.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            ind.setEnabled(!internal);
                        }
                    });
                    if (indW != null) {
                        swingUtils.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                indW.setEnabled(!internal);
                            }
                        });
                    }
                }
            }
        } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
            if (getBrowser().getUsedPorts().contains(newValue.getValueForConfig())
                && !newValue.equals(getBlockDevice().getValue(param))) {
                ret = false;
            }
            final Pattern p = Pattern.compile(".*\\D.*");
            final Matcher m = p.matcher(newValue.getValueForConfig());
            if (m.matches() && !DRBD_MD_TYPE_FLEXIBLE.equals(newValue)) {
                ret = false;
            }
        }
        paramCorrectValueMap.remove(param);
        paramCorrectValueMap.put(param, ret);
        return ret;
    }

    @Override
    protected boolean isRequired(final String param) {
        return true;
    }

    @Override
    protected boolean isAdvanced(final String param) {
        return false;
    }

    @Override
    protected AccessMode.Type getAccessType(final String param) {
        return AccessMode.ADMIN;
    }

    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    @Override
    protected AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
        return AccessMode.NORMAL;
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
        /* not required */
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
    protected String getParamRegexp(final String param) {
        return null;
    }

    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        if (DRBD_MD_PARAM.equals(param)) {
            /* meta disk */
            final Value internalMetaDisk =
                                    new StringValue("internal", Tools.getString("HostBrowser.MetaDisk.Internal"));
            final Value defaultMetaDiskString = internalMetaDisk;
            getBrowser().lockBlockDevInfosRead();
            final Value[] blockDevices = getAvailableBlockDevicesForMetaDisk(
            internalMetaDisk,
            getName(),
            getBrowser().getSortedBlockDevInfos());
            getBrowser().unlockBlockDevInfosRead();

            getBlockDevice().setDefaultValue(DRBD_MD_PARAM,
            defaultMetaDiskString);
            return blockDevices;
        } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
            final Value dmdiValue = getBlockDevice().getValue(DRBD_MD_INDEX_PARAM);
            String defaultMetaDiskIndex;
            if (dmdiValue == null) {
                defaultMetaDiskIndex = null;
            } else {
                defaultMetaDiskIndex = dmdiValue.getValueForConfig();
            }

            if ("internal".equals(defaultMetaDiskIndex)) {
                defaultMetaDiskIndex = Tools.getString("HostBrowser.MetaDisk.Internal");
            }

            int index = 0;
            if (defaultMetaDiskIndex == null) {
                defaultMetaDiskIndex = DRBD_MD_TYPE_FLEXIBLE.getValueForConfig();
            } else if (!DRBD_MD_TYPE_FLEXIBLE.getValueForConfig().equals(defaultMetaDiskIndex)) {
                index = Integer.valueOf(defaultMetaDiskIndex) - 5;
                if (index < 0) {
                    index = 0;
                }
            }

            final Value[] indeces = new Value[11];
            indeces[0] = DRBD_MD_TYPE_FLEXIBLE;
            for (int i = 1; i < 11; i++) {
                indeces[i] = new StringValue(Integer.toString(index));
                index++;
            }

            getBlockDevice().setDefaultValue(DRBD_MD_INDEX_PARAM,
            DRBD_MD_TYPE_FLEXIBLE);
            return indeces;
        }
        return null;
    }

    @Override
    protected Value getParamDefault(final String param) {
        return getBlockDevice().getDefaultValue(param);
    }

    @Override
    protected Value getParamPreferred(final String param) {
        return getBlockDevice().getPreferredValue(param);
    }

    @Override
    protected boolean checkParamCache(final String param) {
        final Boolean cv = paramCorrectValueMap.get(param);
        if (cv == null) {
            return false;
        }
        return cv.booleanValue();
    }

    protected Value[] getAvailableBlockDevicesForMetaDisk(final Value defaultValue,
                                                          final String serviceName,
                                                          final Iterable<BlockDevInfo> blockDevInfos) {
        final List<Value> list = new ArrayList<Value>();
        final Value savedMetaDisk = getBlockDevice().getValue(DRBD_MD_PARAM);

        if (defaultValue != null) {
            list.add(defaultValue);
        }

        for (final BlockDevInfo bdi : blockDevInfos) {
            final BlockDevice bd = bdi.getBlockDevice();
            if (bdi.equals(savedMetaDisk) || (!bd.isDrbd() && !bd.isUsedByCRM() && !bd.isMounted())) {
                list.add(bdi);
            }
        }
        return list.toArray(new Value[list.size()]);
    }

    public void attach(final Application.RunMode runMode) {
        DRBD.attach(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    void detach(final Application.RunMode runMode) {
        DRBD.detach(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    void connect(final Application.RunMode runMode) {
        DRBD.connect(getHost(), volumeInfo.getDrbdResourceInfo().getName(), null, runMode);
    }

    public void disconnect(final Application.RunMode runMode) {
        DRBD.disconnect(getHost(), volumeInfo.getDrbdResourceInfo().getName(), null, runMode);
    }

    void pauseSync(final Application.RunMode runMode) {
        DRBD.pauseSync(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    void resumeSync(final Application.RunMode runMode) {
        DRBD.resumeSync(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    void drbdUp(final Application.RunMode runMode) {
        DRBD.up(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    /** Sets this drbd block device to the primary state. */
    void setPrimary(final Application.RunMode runMode) {
        DRBD.setPrimary(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    /** Sets this drbd block device to the secondary state. */
    public void setSecondary(final Application.RunMode runMode) {
        DRBD.setSecondary(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    /** Initializes drbd block device. */
    void initDrbd(final Application.RunMode runMode) {
        DRBD.initDrbd(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    /** Make filesystem. */
    public void makeFilesystem(final String filesystem, final Application.RunMode runMode) {
        DRBD.makeFilesystem(getHost(), getDrbdVolumeInfo().getDevice(), filesystem, runMode);
    }

    public boolean pvCreate(final Application.RunMode runMode) {
        final String device;
        if (getBlockDevice().isDrbd()) {
            device = volumeInfo.getDevice();
        } else {
            device = getBlockDevice().getName();
        }
        final boolean ret = LVM.pvCreate(getHost(), device, runMode);
        if (ret) {
            getBlockDevice().setVolumeGroupOnPhysicalVolume("");
        }
        return ret;
    }

    public boolean pvRemove(final Application.RunMode runMode) {
        final String device;
        if (getBlockDevice().isDrbd()) {
            device = volumeInfo.getDevice();
        } else {
            device = getBlockDevice().getName();
        }
        final boolean ret = LVM.pvRemove(getHost(), device, runMode);
        if (ret) {
            if (getBlockDevice().isDrbd()) {
                getBlockDevice().getDrbdBlockDevice().setVolumeGroupOnPhysicalVolume(null);
            } else {
                getBlockDevice().setVolumeGroupOnPhysicalVolume(null);
            }
        }
        return ret;
    }

    public boolean lvRemove(final Application.RunMode runMode) {
        final String device = getBlockDevice().getName();
        return LVM.lvRemove(getHost(), device, runMode);
    }

    public boolean lvSnapshot(final String snapshotName, final String size, final Application.RunMode runMode) {
        final String device = getBlockDevice().getName();
        return LVM.createLVSnapshot(getHost(), snapshotName, device, size, runMode);
    }

    public void skipInitialFullSync(final Application.RunMode runMode) {
        DRBD.skipInitialFullSync(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    public void forcePrimary(final Application.RunMode runMode) {
        DRBD.forcePrimary(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    void invalidateBD(final Application.RunMode runMode) {
        DRBD.invalidate(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    void discardData(final Application.RunMode runMode) {
        DRBD.discardData(getHost(), volumeInfo.getDrbdResourceInfo().getName(), null, runMode);
    }

    public void verify(final Application.RunMode runMode) {
        DRBD.verify(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    public boolean resizeDrbd(final Application.RunMode runMode) {
        return DRBD.resize(getHost(), volumeInfo.getDrbdResourceInfo().getName(), volumeInfo.getName(), runMode);
    }

    @Override
    public JPanel getGraphicalView() {
        if (getBlockDevice().isDrbd()) {
            getBrowser().getClusterBrowser().getGlobalInfo().setSelectedNode(this);
        }
        return getBrowser().getClusterBrowser().getGlobalInfo().getGraphicalView();
    }

    @Override
    protected void setTerminalPanel() {
        if (getHost() != null) {
            mainPanel.setTerminalPanel(getHost().getTerminalPanel());
        }
    }

    @Override
    public JComponent getInfoPanel() {
        swingUtils.isSwingThread();
        return getInfoPanelBD();
    }

    @Override
    public String[] getParametersFromXML() {
        final String[] params = {
            DRBD_MD_PARAM,
            DRBD_MD_INDEX_PARAM,
        };
        return params;
    }

    /** Apply all fields. */
    public void apply(final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            final String[] params = getParametersFromXML();
            swingUtils.invokeAndWait(new Runnable() {
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

            final Value v = getWidget(DRBD_MD_PARAM, null).getValue();
            if (v.isNothingSelected() || "internal".equals(v.getValueForConfig())) {
                getBlockDevice().setMetaDisk(null); /* internal */
            } else {
                final BlockDevice metaDisk = ((BlockDevInfo) v).getBlockDevice();
                getBlockDevice().setMetaDisk(metaDisk);
            }
            getBrowser().getClusterBrowser().getGlobalInfo().setAllApplyButtons();
        }
    }

    /** Returns block device panel. */
    JComponent getInfoPanelBD() {
        swingUtils.isSwingThread();
        if (infoPanel != null) {
            infoPanelDone();
            return infoPanel;
        }
        final BlockDevInfo thisClass = this;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public void mouseOut(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                final DrbdGraph drbdGraph = getBrowser().getDrbdGraph();
                drbdGraph.stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }

            @Override
            public void mouseOver(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                component.setToolTipText(Tools.getString("ClusterBrowser.StartingDRBDtest"));
                component.setToolTipBackground(Tools.getDefaultColor("ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                final DrbdGraph drbdGraph = getBrowser().getDrbdGraph();
                drbdGraph.startTestAnimation((JComponent) component, startTestLatch);
                getBrowser().drbdtestLockAcquire();
                thisClass.setDRBDtestData(null);
                apply(Application.RunMode.TEST);
                final Map<Host, String> testOutput = new LinkedHashMap<Host, String>();
                try {
                    getBrowser().getClusterBrowser().getGlobalInfo().createConfigDryRun(testOutput);
                    final DRBDtestData dtd = new DRBDtestData(testOutput);
                    component.setToolTipText(dtd.getToolTip());
                    thisClass.setDRBDtestData(dtd);
                } finally {
                    getBrowser().drbdtestLockRelease();
                    startTestLatch.countDown();
                }
            }
        };
        initApplyButton(buttonCallback);

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);
        if (getBlockDevice().isDrbd()) {
            final String[] params = getParametersFromXML();

            addParams(optionsPanel,
                      params,
                      application.getDefaultSize("HostBrowser.DrbdDevLabelWidth"),
                      application.getDefaultSize("HostBrowser.DrbdDevFieldWidth"),
                      null);

            /* apply button */
            getApplyButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            swingUtils.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    getApplyButton().setEnabled(false);
                                    getRevertButton().setEnabled(false);
                                }
                            });
                            getBrowser().getClusterBrowser().drbdStatusLock();
                            try {
                                getBrowser().getClusterBrowser().getGlobalInfo().createDrbdConfigLive();
                                for (final Host h : getHost().getCluster().getHostsArray()) {
                                    DRBD.adjustApply(h, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.LIVE);
                                }
                                apply(Application.RunMode.LIVE);
                            } catch (final Exceptions.DrbdConfigException e) {
                                LOG.appError("getInfoPanelBD: config failed", e);
                            } catch (final UnknownHostException e) {
                                LOG.appError("getInfoPanelBD: config failed", e);
                            } finally {
                                getBrowser().getClusterBrowser().drbdStatusUnlock();
                            }
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
        final JPanel riaPanel = new JPanel();
        riaPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        riaPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        riaPanel.add(super.getInfoPanel());
        mainPanel.add(riaPanel);

        mainPanel.add(optionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(new JScrollPane(mainPanel));
        infoPanel = newPanel;
        infoPanelDone();
        setApplyButtons(null, getParametersFromXML());
        return infoPanel;
    }

    /** Sets drbd resource for this block device. */
    void setDrbdVolumeInfo(final VolumeInfo volumeInfo) {
        this.volumeInfo = volumeInfo;
    }

    /** Returns drbd resource info in which this block device is member. */
    public VolumeInfo getDrbdVolumeInfo() {
        return volumeInfo;
    }

    public BlockDevice getBlockDevice() {
        return (BlockDevice) getResource();
    }

    /** Removes this block device from drbd data structures. */
    public void removeFromDrbd() {
        setDrbd(false);
        getBlockDevice().setDrbdBlockDevice(null);
        setDrbdVolumeInfo(null);
    }

    @Override
    protected String getParamShortDesc(final String param) {
        return Tools.getString(param);
    }

    @Override
    protected String getParamLongDesc(final String param) {
        return Tools.getString(param + ".Long");
    }

    public String getVGName() {
        final BlockDevice bd;
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
            return bd.getVgOnPhysicalVolume();
        } else {
            /* lv on vg */
            return vg;
        }
    }

    @Override
    public List<UpdatableItem> createPopup() {
        return blockDevMenu.getPulldownMenu(this);
    }

    /** Returns how much of the block device is used. */
    @Override
    public int getUsed() {
        final VolumeInfo dvi = volumeInfo;
        if (dvi != null) {
            return dvi.getUsed();
        }
        return getBlockDevice().getUsed();
    }

    public String getIconTextForGraph(final Application.RunMode runMode) {
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
            final String vg = getBlockDevice().getVgOnPhysicalVolume();
            if (vg != null && !"".equals(vg)) {
                return "VG " + vg;
            }
        }
        return getName();
    }

    public ColorText getRightCornerTextForDrbdGraph(final Application.RunMode runMode) {
        final String vg;
        if (isLVM()) {
            vg = getBlockDevice().getVolumeGroup();
        } else {
            vg = getBlockDevice().getVgOnPhysicalVolume();
        }

        if (getBlockDevice().isDrbdMetaDisk()) {
            return METADISK_COLOR_TEXT;
        } else if (getBlockDevice().isSwap()) {
            return SWAP_COLOR_TEXT;
        } else if (getBlockDevice().getMountedOn() != null) {
            return MOUNTED_COLOR_TEXT;
        } else if (getBlockDevice().isDrbd()) {
            String s = getBlockDevice().getName();
            // TODO: cache that
            if (s.length() > MAX_RIGHT_CORNER_STRING_LENGTH) {
                s = "..." + s.substring(s.length() - MAX_RIGHT_CORNER_STRING_LENGTH + 3,
                s.length());
            }
            if (getBlockDevice().isDrbdPhysicalVolume()) {
                final String drbdVG = getBlockDevice().getDrbdBlockDevice().getVgOnPhysicalVolume();
                if (drbdVG != null && !"".equals(drbdVG)) {
                    s = s + " VG:" + drbdVG;
                } else {
                    s += " PV";
                }
            }
            return new ColorText(s, Color.BLUE, Color.BLACK);
        } else if (vg != null && !"".equals(vg)) {
            if (isLVM()) {
                return new ColorText("LV in " + vg, Color.BLUE, Color.GREEN);
            } else {
                return new ColorText(getName(), Color.BLUE, Color.GREEN);
            }
        } else if (getBlockDevice().isPhysicalVolume()) {
            return PHYSICAL_VOLUME_COLOR_TEXT;
        }
        return null;
    }

    /** Returns whether this device is connected via drbd. */
    public boolean isConnected(final Application.RunMode runMode) {
        final DRBDtestData dtd = getDRBDtestData();
        if (dtd != null && Application.isTest(runMode)) {
            return isConnectedTest(dtd) && !isWFConnection(runMode);
        } else {
            return getBlockDevice().isConnected();
        }
    }

    /** Returns whether this device is connected or wait-for-c via drbd. */
    public boolean isConnectedOrWF(final Application.RunMode runMode) {
        final DRBDtestData dtd = getDRBDtestData();
        if (dtd != null && Application.isTest(runMode)) {
            return isConnectedTest(dtd);
        } else {
            return getBlockDevice().isConnectedOrWF();
        }
    }

    /** Returns whether this device is in wait-for-connection state. */
    public boolean isWFConnection(final Application.RunMode runMode) {
        final DRBDtestData dtd = getDRBDtestData();
        if (dtd != null && Application.isTest(runMode)) {
            return isConnectedOrWF(runMode) && isConnectedTest(dtd) && !getOtherBlockDevInfo().isConnectedTest(dtd);
        } else {
            return getBlockDevice().isWFConnection();
        }
    }

    /** Returns whether this device will be disconnected. */
    boolean isConnectedTest(final DRBDtestData dtd) {
        return dtd.isConnected(getHost(), volumeInfo.getDrbdResourceInfo().getName())
               || (!dtd.isDisconnected(getHost(), volumeInfo.getDrbdResourceInfo().getName())
                   && getBlockDevice().isConnectedOrWF());
    }

    public boolean isDiskless(final Application.RunMode runMode) {
        final DRBDtestData dtd = getDRBDtestData();
        final VolumeInfo dvi = volumeInfo;
        if (dtd != null && dvi != null && Application.isTest(runMode)) {
            return dtd.isDiskless(getHost(), volumeInfo.getDevice())
                   || (!dtd.isAttached(getHost(), volumeInfo.getDevice()) && getBlockDevice().isDiskless());
        } else {
            return getBlockDevice().isDiskless();
        }
    }

    DRBDtestData getDRBDtestData() {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getDRBDtestData();
    }

    void setDRBDtestData(final DRBDtestData drbdtestData) {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return;
        }
        b.setDRBDtestData(drbdtestData);
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
        final String name;
        int volume = 0;
        final VolumeInfo dvi = getDrbdVolumeInfo();
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
        final VolumeInfo odvi = obdi.getDrbdVolumeInfo();
        final String oName;
        int oVolume = 0;
        if (obdi.getBlockDevice().isDrbd() && odvi != null) {
            oName = odvi.getDrbdResourceInfo().getName();
            final String v = odvi.getName();
            if (Tools.isNumber(v)) {
                oVolume = Integer.parseInt(v);
            }
        } else {
            oName = o.getName();
        }
        /* drbds up */
        if (getBlockDevice().isDrbd() && !obdi.getBlockDevice().isDrbd()) {
            return -1;
        }
        if (!getBlockDevice().isDrbd() && obdi.getBlockDevice().isDrbd()) {
            return 1;
        }

        /* cdroms, swap etc down */
        final boolean unimportant = isUnimportant(name, getBlockDevice().getFsType(), getBlockDevice().getMountedOn());
        final boolean oUnimportant = isUnimportant(oName,
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

    public void setParameters(final String resName) {
        swingUtils.isSwingThread();
        getBlockDevice().setNew(false);

        final ClusterBrowser clusterBrowser = getBrowser().getClusterBrowser();
        if (clusterBrowser == null) {
            return;
        }
        final VolumeInfo dvi = volumeInfo;
        if (dvi == null) {
            return;
        }
        final DrbdXml dxml = clusterBrowser.getDrbdXml();
        final String hostName = getHost().getName();
        final DrbdGraph drbdGraph = getBrowser().getDrbdGraph();
        Value value = null;
        final String volumeNr = dvi.getName();
        for (final String param : getParametersFromXML()) {
            if (DRBD_MD_PARAM.equals(param)) {
                final String metaDisk = dxml.getMetaDisk(hostName, resName, volumeNr);
                if (value == null || !"internal".equals(value.getValueForConfig())) {
                    final BlockDevInfo mdI = drbdGraph.findBlockDevInfo(hostName, metaDisk);
                    if (mdI != null) {
                        getBlockDevice().setMetaDisk(mdI.getBlockDevice());
                    }
                }
                value = new StringValue(metaDisk);
            } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
                value = new StringValue(dxml.getMetaDiskIndex(hostName, resName, volumeNr));
            }
            final Value defaultValue = getParamDefault(param);
            if (value == null) {
                value = defaultValue;
            }
            final Value oldValue = getParamSaved(param);
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
    public Check checkResourceFields(final String param, final String[] params) {
        return checkResourceFields(param, params, false, false, false);
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    Check checkResourceFields(final String param,
                              final String[] params,
                              final boolean fromDrbdInfo,
                              final boolean fromDrbdResourceInfo,
                              final boolean fromDrbdVolumeInfo) {
        final VolumeInfo dvi = getDrbdVolumeInfo();
        if (dvi != null && !fromDrbdVolumeInfo && !fromDrbdResourceInfo && !fromDrbdInfo) {
            dvi.setApplyButtons(null, dvi.getParametersFromXML());
        }
        final DrbdXml dxml = getBrowser().getClusterBrowser().getDrbdXml();
        final List<String> incorrect = new ArrayList<String>();
        if (dxml != null && dxml.isDrbdDisabled()) {
            incorrect.add("drbd is disabled");
        }
        final Check check = new Check(incorrect, new ArrayList<String>());
        check.addCheck(super.checkResourceFields(param, params));
        return check;
    }

    /** Returns whether this block device is a volume group in LVM. */
    public boolean isLVM() {
        return getBlockDevice().getVolumeGroup() != null;
    }

    /** Returns how much is free space in a volume group. */
    public Long getFreeInVolumeGroup() {
        return getHost().getHostParser().getFreeInVolumeGroup(getBlockDevice().getVolumeGroup());
    }

    /** Returns true if this is the first volume in the resource. Returns true
     * if this is not a DRBD resource. */
    public boolean isFirstDrbdVolume() {
        if (!getBlockDevice().isDrbd()) {
            return true;
        }
        final Set<VolumeInfo> drbdVolumes = getDrbdVolumeInfo().getDrbdResourceInfo().getDrbdVolumes();
        if (drbdVolumes == null || drbdVolumes.isEmpty()) {
            return true;
        }
        return drbdVolumes.iterator().next() == getDrbdVolumeInfo();
    }

    /** Return whether two primaries are allowed. */
    boolean allowTwoPrimaries() {
        final ResourceInfo dri = volumeInfo.getDrbdResourceInfo();
        return "yes".equals(dri.getParamSaved(ALLOW_TWO_PRIMARIES).getValueForConfig());
    }

    /**
     * Proxy status for graph, null if there's no proxy configured for the
     * resource.
     */
    public String getProxyStateForGraph(final Application.RunMode runMode) {
        final VolumeInfo dvi = volumeInfo;
        if (dvi == null) {
            return null;
        }
        final ResourceInfo dri = dvi.getDrbdResourceInfo();
        final Host pHost = dri.getProxyHost(getHost(), !WIZARD);
        if (dri.isProxy(getHost())) {
            if (pHost == null) {
                return "ERROR";
            }
            if (pHost.isConnected()) {
                if (pHost.getHostParser().isDrbdProxyUp(dri.getName())) {
                    return PROXY_UP;
                } else {
                    return PROXY_DOWN;
                }
            } else {
                if (dvi.isConnected(runMode)) {
                    return PROXY_UP;
                } else {
                    return pHost.getName();
                }
            }
        }
        return null;
    }

    /** Whether PV can be created on this BD. */
    public boolean canCreatePV() {
        return !isLVM() && !getBlockDevice().isPhysicalVolume() && !getBlockDevice().isDrbdPhysicalVolume();
    }

    /** Whether PV can be removed from this BD. */
    boolean canRemovePV() {
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

    /** Whether VG can be removed. */
    boolean canRemoveVG() {
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
        if (!bd.isVolumeGroupOnPhysicalVolume()) {
            return false;
        }
        final String vg = bd.getVgOnPhysicalVolume();
        return getHost().getHostParser().getLogicalVolumesFromVolumeGroup(vg) == null;
    }
}
