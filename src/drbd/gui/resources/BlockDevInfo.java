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

import drbd.Exceptions;
import drbd.gui.Browser;
import drbd.gui.HostBrowser;
import drbd.gui.ClusterBrowser;
import drbd.gui.DrbdGraph;
import drbd.data.ConfigData;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.DRBD;
import drbd.utilities.LVM;
import drbd.utilities.ButtonCallback;
import drbd.gui.GuiComboBox;
import drbd.data.Host;
import drbd.data.Subtext;
import drbd.data.Cluster;
import drbd.data.DRBDtestData;
import drbd.data.resources.BlockDevice;
import drbd.data.AccessMode;
import drbd.data.DrbdXML;

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
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.CountDownLatch;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;

/**
 * This class holds info data for a block device.
 */
public final class BlockDevInfo extends EditableInfo {
    /** DRBD resource in which this block device is member. */
    private DrbdResourceInfo drbdResourceInfo;
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
    /** Internal parameter name of drbd network interface. */
    private static final String DRBD_NI_PARAM         = "DrbdNetInterface";
    /** Internal parameter name of drbd network interface port. */
    private static final String DRBD_NI_PORT_PARAM    = "DrbdNetInterfacePort";
    /** Large harddisk icon. */
    public static final ImageIcon HARDDISK_ICON_LARGE = Tools.createImageIcon(
                           Tools.getDefault("BlockDevInfo.HarddiskIconLarge"));
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
    /** String length after the cut. */
    private static final int MAX_RIGHT_CORNER_STRING_LENGTH = 28;
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String NO_DRBD_RESOURCE_STRING =
                                                "it is not a drbd resource";

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
        if (drbdResourceInfo == null) {
            return null;
        }
        return drbdResourceInfo.getOtherBlockDevInfo(this);
    }


    /** Returns browser object of this info. */
    @Override protected HostBrowser getBrowser() {
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
    @Override public void removeMyself(final boolean testOnly) {
        getBlockDevice().setValue(DRBD_NI_PARAM, null);
        getBlockDevice().setValue(DRBD_NI_PORT_PARAM, null);
        getBlockDevice().setValue(DRBD_MD_PARAM, null);
        getBlockDevice().setValue(DRBD_MD_INDEX_PARAM, null);
        super.removeMyself(testOnly);
        infoPanel = null;
    }

    /** Returns host on which is this block device. */
    public Host getHost() {
        return getBrowser().getHost();
    }

    /** Returns block device icon for the menu. */
    @Override public ImageIcon getMenuIcon(final boolean testOnly) {
        return BlockDevInfo.HARDDISK_ICON;
    }

    /** Returns info of this block device as string. */
    @Override String getInfo() {
        final StringBuffer ret = new StringBuffer(120);
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

    /** Returns tool tip for this block device. */
    @Override public String getToolTipForGraph(final boolean testOnly) {
        final StringBuffer tt = new StringBuffer(60);

        if (getBlockDevice().isDrbd()) {
            tt.append("<b>");
            tt.append(drbdResourceInfo.getDevice());
            tt.append("</b> (");
            tt.append(getBlockDevice().getName());
            tt.append(')');
        } else {
            tt.append("<b>");
            tt.append(getBlockDevice().getName());
            tt.append("</b>");
        }
        tt.append("</b>");
        if (getBlockDevice().isDrbdMetaDisk()) {
            tt.append(" (Meta Disk)\n");
            for (final BlockDevice mb
                         : getBlockDevice().getMetaDiskOfBlockDevices()) {
                tt.append("&nbsp;&nbsp;of ");
                tt.append(mb.getName());
                tt.append('\n');
            }

        }

        if (getBlockDevice().isDrbd()) {
            if (getHost().isDrbdStatus()) {
                String cs = getBlockDevice().getConnectionState();
                String st = getBlockDevice().getNodeState();
                String ds = getBlockDevice().getDiskState();
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
    String drbdNodeConfig(final String resource,
                          final String drbdDevice)
            throws Exceptions.DrbdConfigException {

        if (drbdDevice == null) {
            throw new Exceptions.DrbdConfigException(
                                    "Drbd device not defined for host "
                                    + getHost().getName()
                                    + " (" + resource + ")");
        }
        if (getBlockDevice().getDrbdNetInterfaceWithPort() == null) {
            throw new Exceptions.DrbdConfigException(
                                    "Net interface not defined for host "
                                    + getHost().getName()
                                    + " (" + resource + ")");
        }
        if (getBlockDevice().getName() == null) {
            throw new Exceptions.DrbdConfigException(
                                    "Block device not defined for host "
                                    + getHost().getName()
                                    + " (" + resource + ")");
        }

        final StringBuffer config = new StringBuffer(120);
        config.append("\ton ");
        config.append(getHost().getName());
        config.append(" {\n\t\tdevice\t");
        config.append(drbdDevice);
        config.append(";\n\t\tdisk\t");
        config.append(getBlockDevice().getName());
        config.append(";\n\t\taddress\t");
        config.append(getBlockDevice().getDrbdNetInterfaceWithPort(
                                        getComboBoxValue(DRBD_NI_PARAM),
                                        getComboBoxValue(DRBD_NI_PORT_PARAM)));
        config.append(";\n\t\t");
        config.append(getBlockDevice().getMetaDiskString(
                                       getComboBoxValue(DRBD_MD_PARAM),
                                       getComboBoxValue(DRBD_MD_INDEX_PARAM)));
        config.append(";\n\t}\n");
        return config.toString();
    }

    /** Sets whether this block device is drbd. */
    void setDrbd(final boolean drbd) {
        getBlockDevice().setDrbd(drbd);
    }

    /** Returns section of this paramter. */
    @Override protected String getSection(final String param) {
        return getBlockDevice().getSection(param);
    }

    /** Returns possible choices of this paramter. */
    @Override protected Object[] getPossibleChoices(final String param) {
        return getBlockDevice().getPossibleChoices(param);
    }

    /** Returns default value of this paramter. */
    protected Object getDefaultValue(final String param) {
        return "<select>";
    }

    /** Returns next network interface port. TODO: VI? */
    int getNextVIPort() {
        int port = Tools.getDefaultInt("HostBrowser.DrbdNetInterfacePort") - 1;
        for (final String portString : getBrowser().getDrbdVIPortList()) {
            final int p = Integer.valueOf(portString);
            if (p > port) {
                port = p;
            }
        }
        return port;
    }

    /** Sets network interface port. TODO: VI? */
    void setDefaultVIPort(final int port) {
        final String value = Integer.toString(port);
        getBlockDevice().setValue(DRBD_NI_PORT_PARAM, value);
        getBrowser().getDrbdVIPortList().add(value);
    }

    /** Returns combobox for this parameter. */
    @Override protected GuiComboBox getParamComboBox(final String param,
                                                     final String prefix,
                                                     final int width) {
        GuiComboBox paramCb;
        if (DRBD_NI_PORT_PARAM.equals(param)) {
            final List<String> drbdVIPorts = new ArrayList<String>();
            String defaultPort = getBlockDevice().getValue(param);
            if (defaultPort == null) {
                defaultPort = getBlockDevice().getDefaultValue(param);
            }
            drbdVIPorts.add(defaultPort);
            int i = 0;
            int index = Tools.getDefaultInt("HostBrowser.DrbdNetInterfacePort");
            while (i < 10) {
                final String port = Integer.toString(index);
                if (!getBrowser().getDrbdVIPortList().contains(port)) {
                    drbdVIPorts.add(port);
                    i++;
                }
                index++;
            }
            String regexp = null;
            if (isInteger(param)) {
                regexp = "^\\d*$";
            }
            final GuiComboBox gcb = new GuiComboBox(
                       defaultPort,
                       drbdVIPorts.toArray(new String[drbdVIPorts.size()]),
                       null, /* units */
                       null, /* type */
                       regexp,
                       width,
                       null, /* abbrv */
                       new AccessMode(getAccessType(param),
                                      isEnabledOnlyInAdvancedMode(param)));
            paramCb = gcb;
            //gcb.setValue(defaultPort);
            paramComboBoxAdd(param, prefix, gcb);
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    gcb.setEnabled(true);
                    gcb.setAlwaysEditable(true);
                }
            });
        } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
            final GuiComboBox gcb =
                                super.getParamComboBox(param, prefix, width);
            paramCb = gcb;
            //SwingUtilities.invokeLater(new Runnable() {
            //    @Override public void run() {
            //        gcb.setAlwaysEditable(true);
            //    }
            //});
        } else {
            final GuiComboBox gcb =
                                 super.getParamComboBox(param, prefix, width);
            paramCb = gcb;
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    gcb.setEditable(false);
                }
            });
        }
        return paramCb;
    }

    /** Returns true if a paramter is correct. */
    @Override protected boolean checkParam(final String param, String value) {
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
                    final GuiComboBox ind = paramComboBoxGet(
                                                           DRBD_MD_INDEX_PARAM,
                                                           null);
                    final GuiComboBox indW = paramComboBoxGet(
                                                           DRBD_MD_INDEX_PARAM,
                                                           "wizard");
                    if (internal) {
                        ind.setValue(DRBD_MD_TYPE_FLEXIBLE);
                        if (indW != null) {
                            indW.setValue(DRBD_MD_TYPE_FLEXIBLE);
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            ind.setEnabled(!internal);
                        }
                    });
                    if (indW != null) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                indW.setEnabled(!internal);
                            }
                        });
                    }
                }
            }
        } else if (DRBD_NI_PORT_PARAM.equals(param)) {
            if (getBrowser().getDrbdVIPortList().contains(value)
                && !value.equals(getBlockDevice().getValue(param))) {
                ret = false;
            }
            final Pattern p = Pattern.compile(".*\\D.*");
            final Matcher m = p.matcher(value);
            if (m.matches()) {
                ret = false;
            }
        } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
            if (getBrowser().getDrbdVIPortList().contains(value)
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
    @Override protected boolean isRequired(final String param) {
        return true;
    }

    /** Returns whether this parameter is advanced. */
    @Override protected boolean isAdvanced(final String param) {
        return false;
    }

    /** Returns access type of this parameter. */
    @Override protected ConfigData.AccessType getAccessType(
                                                         final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /** Whether the parameter should be enabled. */
    @Override protected String isEnabled(final String param) {
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override protected boolean isEnabledOnlyInAdvancedMode(
                                                        final String param) {
         return false;
    }

    /** Returns whether this type is integer. */
    @Override protected boolean isInteger(final String param) {
        if (DRBD_NI_PORT_PARAM.equals(param)) {
            return true;
        }
        return false;
    }

    /** Returns whether this type is a label. */
    @Override protected boolean isLabel(final String param) {
        return false;
    }

    /** Returns whether this parameter is of a time type. */
    @Override protected boolean isTimeType(final String param) {
        /* not required */
        return false;
    }

    /** Returns whether this parameter is a checkbox. */
    @Override protected boolean isCheckBox(final String param) {
        return false;
    }

    /** Returns type of this parameter. */
    @Override protected String getParamType(final String param) {
        return null;
    }

    /** Returns the regexp of the parameter. */
    @Override protected String getParamRegexp(final String param) {
        return null;
    }

    /** Returns possible choices for the parameter. */
    @Override protected Object[] getParamPossibleChoices(final String param) {
        if (DRBD_NI_PARAM.equals(param)) {
            /* net interfaces */
            StringInfo defaultNetInterface = null;
            String netInterfaceString =
                                    getBlockDevice().getValue(DRBD_NI_PARAM);
            if (netInterfaceString == null
                || netInterfaceString.equals("")) {
                defaultNetInterface =
                    new StringInfo(
                        Tools.getString("HostBrowser.DrbdNetInterface.Select"),
                        null,
                        getBrowser());
                netInterfaceString = defaultNetInterface.toString();
                getBlockDevice().setDefaultValue(DRBD_NI_PARAM, null);
//                                                 netInterfaceString);
            }
            return getNetInterfaces(defaultNetInterface,
                                getBrowser().getNetInterfacesNode().children());
        } else if (DRBD_MD_PARAM.equals(param)) {
            /* meta disk */
            final StringInfo internalMetaDisk =
                    new StringInfo(Tools.getString(
                                        "HostBrowser.MetaDisk.Internal"),
                                   "internal",
                                   getBrowser());
            final String defaultMetaDiskString = internalMetaDisk.toString();
            getBrowser().lockBlockDevInfos();
            final Info[] blockDevices = getAvailableBlockDevicesForMetaDisk(
                                internalMetaDisk,
                                getName(),
                                getBrowser().getBlockDevicesNode().children());
            getBrowser().unlockBlockDevInfos();

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
    @Override protected String getParamDefault(final String param) {
        return getBlockDevice().getDefaultValue(param);
    }

    /** Returns preferred value of this parameter. */
    @Override protected String getParamPreferred(final String param) {
        return getBlockDevice().getPreferredValue(param);
    }

    /** Return whether the value is correct from the cache. */
    @Override protected boolean checkParamCache(final String param) {
        final Boolean cv = paramCorrectValueMap.get(param);
        if (cv == null) {
            return false;
        }
        return cv.booleanValue();
    }

    /** Ruturns all net interfaces. */
    protected Object[] getNetInterfaces(final Info defaultValue,
                                        final Enumeration e) {
        final List<Object> list = new ArrayList<Object>();

        if (defaultValue != null) {
            list.add(defaultValue);
        }

        while (e.hasMoreElements()) {
            final Info i =
              (Info) ((DefaultMutableTreeNode) e.nextElement()).getUserObject();
            list.add(i);
        }
        return list.toArray(new Object[list.size()]);
    }

    /** Returns block devices that are available for drbd meta-disk. */
    protected Info[] getAvailableBlockDevicesForMetaDisk(
                                           final Info defaultValue,
                                           final String serviceName,
                                           final Enumeration e) {
        final List<Info> list = new ArrayList<Info>();
        final String savedMetaDisk = getBlockDevice().getValue(DRBD_MD_PARAM);

        if (defaultValue != null) {
            list.add(defaultValue);
        }

        while (e.hasMoreElements()) {
            final BlockDevInfo bdi =
              (BlockDevInfo) ((DefaultMutableTreeNode) e.nextElement())
                                                            .getUserObject();
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
        DRBD.attach(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** DRBD detach. */
    void detach(final boolean testOnly) {
        DRBD.detach(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** DRBD connect. */
    void connect(final boolean testOnly) {
        DRBD.connect(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** DRBD disconnect. */
    void disconnect(final boolean testOnly) {
        DRBD.disconnect(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** DRBD pause sync. */
    void pauseSync(final boolean testOnly) {
        DRBD.pauseSync(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** DRBD resume sync. */
    void resumeSync(final boolean testOnly) {
        DRBD.resumeSync(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** DRBD up command. */
    void drbdUp(final boolean testOnly) {
        DRBD.up(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** Sets this drbd block device to the primary state. */
    void setPrimary(final boolean testOnly) {
        DRBD.setPrimary(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** Sets this drbd block device to the secondary state. */
    public void setSecondary(final boolean testOnly) {
        DRBD.setSecondary(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** Initializes drbd block device. */
    void initDrbd(final boolean testOnly) {
        DRBD.initDrbd(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** Make filesystem. */
     public void makeFilesystem(final String filesystem,
                                final boolean testOnly) {
        DRBD.makeFilesystem(getHost(),
                            getDrbdResourceInfo().getDevice(),
                            filesystem,
                            testOnly);
    }

    /** Resize LVM. */
    public boolean lvmResize(final String size,
                             final boolean testOnly) {
        final String device = getBlockDevice().getName();
        return LVM.resize(getHost(), device, size, testOnly);
    }

    /** Force primary. */
    public void forcePrimary(final boolean testOnly) {
        DRBD.forcePrimary(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** Invalidate the block device. */
    void invalidateBD(final boolean testOnly) {
        DRBD.invalidate(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** Discard the data. */
    void discardData(final boolean testOnly) {
        DRBD.discardData(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** Start on-line verification. */
    void verify(final boolean testOnly) {
        DRBD.verify(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** Resize DRBD. */
    public boolean resizeDrbd(final boolean testOnly) {
        return DRBD.resize(getHost(), drbdResourceInfo.getName(), testOnly);
    }

    /** Returns the graphical view. */
    @Override public JPanel getGraphicalView() {
        if (getBlockDevice().isDrbd()) {
            drbdResourceInfo.getDrbdInfo().setSelectedNode(this);
        }
        return getBrowser().getDrbdGraph().getDrbdInfo().getGraphicalView();
    }

    /** Set the terminal panel. */
    @Override protected void setTerminalPanel() {
        if (getHost() != null) {
            Tools.getGUIData().setTerminalPanel(getHost().getTerminalPanel());
        }
    }

    /** Returns the info panel. */
    @Override public JComponent getInfoPanel() {
        return getInfoPanelBD();
    }

    /** Returns all parameters. */
    @Override public String[] getParametersFromXML() {
        final String[] params = {
                            DRBD_NI_PARAM,
                            DRBD_NI_PORT_PARAM,
                            DRBD_MD_PARAM,
                            DRBD_MD_INDEX_PARAM,
                          };
        return params;
    }

    /** Apply all fields. */
    public void apply(final boolean testOnly) {
        if (!testOnly) {
            final String[] params = getParametersFromXML();
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                }
            });
            if (getBlockDevice().getMetaDisk() != null) {
                getBlockDevice().getMetaDisk().removeMetadiskOfBlockDevice(
                                                             getBlockDevice());
            }
            getBrowser().getDrbdVIPortList().remove(
                               getBlockDevice().getValue(DRBD_NI_PORT_PARAM));

            getBlockDevice().setNew(false);
            storeComboBoxValues(params);

            getBrowser().getDrbdVIPortList().add(
                                getBlockDevice().getValue(DRBD_NI_PORT_PARAM));
            final Object o = paramComboBoxGet(DRBD_MD_PARAM, null).getValue();
            if (Tools.isStringInfoClass(o)) {
                getBlockDevice().setMetaDisk(null); /* internal */
            } else {
                final BlockDevice metaDisk =
                                        ((BlockDevInfo) o).getBlockDevice();
                getBlockDevice().setMetaDisk(metaDisk);
            }
            final DrbdResourceInfo dri = drbdResourceInfo;
            if (dri != null) {
                dri.getDrbdInfo().setAllApplyButtons();
            }
        }
    }

    /** Returns block device panel. */
    JComponent getInfoPanelBD() {
        if (infoPanel != null) {
            return infoPanel;
        }
        final BlockDevInfo thisClass = this;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /**
             * Whether the whole thing should be enabled.
             */
            @Override public final boolean isEnabled() {
                return true;
            }

            @Override public final void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                final DrbdGraph drbdGraph = getBrowser().getDrbdGraph();
                drbdGraph.stopTestAnimation(getApplyButton());
                getApplyButton().setToolTipText(null);
            }

            @Override public final void mouseOver() {
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
                final Map<Host,String> testOutput =
                                         new LinkedHashMap<Host, String>();
                try {
                    drbdResourceInfo.getDrbdInfo().createDrbdConfig(true);
                    for (final Host h
                                    : getHost().getCluster().getHostsArray()) {
                        DRBD.adjust(h, "all", true);
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
        buttonPanel.setBackground(HostBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(HostBrowser.PANEL_BACKGROUND);
        final JMenu serviceCombo = getActionsMenu();
        mb.add(serviceCombo);

        buttonPanel.add(mb, BorderLayout.EAST);
        if (getBlockDevice().isDrbd()) {
            final String[] params = getParametersFromXML();

            addParams(optionsPanel,
                      params,
                      Tools.getDefaultInt("HostBrowser.DrbdDevLabelWidth"),
                      Tools.getDefaultInt("HostBrowser.DrbdDevFieldWidth"),
                      null);


            /* apply button */
            getApplyButton().addActionListener(new ActionListener() {
                @Override public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            Tools.invokeAndWait(new Runnable() {
                                @Override public void run() {
                                    getApplyButton().setEnabled(false);
                                    getRevertButton().setEnabled(false);
                                }
                            });
                            getBrowser().getClusterBrowser().drbdStatusLock();
                            try {
                                drbdResourceInfo.getDrbdInfo()
                                              .createDrbdConfig(false);
                                for (final Host h
                                    : getHost().getCluster().getHostsArray()) {
                                    DRBD.adjust(h, "all", false);
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
                    @Override public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override public void run() {
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
        final Font f = new Font("Monospaced", Font.PLAIN, 12);
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
        return infoPanel;
    }

    /** TODO: dead code? */
    @Override boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Sets drbd resource for this block device. */
    void setDrbdResourceInfo(final DrbdResourceInfo drbdResourceInfo) {
        this.drbdResourceInfo = drbdResourceInfo;
    }

    /** Returns drbd resource info in which this block device is member. */
    public DrbdResourceInfo getDrbdResourceInfo() {
        return drbdResourceInfo;
    }

    /** Returns block device resource object. */
    public BlockDevice getBlockDevice() {
        return (BlockDevice) getResource();
    }

    /** Removes this block device from drbd data structures. */
    void removeFromDrbd() {
        getBrowser().getDrbdVIPortList().remove(
                                getBlockDevice().getValue(DRBD_NI_PORT_PARAM));
        setDrbd(false);
        setDrbdResourceInfo(null);
    }

    /** Returns short description of the parameter. */
    @Override protected String getParamShortDesc(final String param) {
        return Tools.getString(param);
    }

    /** Returns long description of the parameter. */
    @Override protected String getParamLongDesc(final String param) {
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
            @Override public void action() {
                final DrbdInfo drbdInfo =
                                    getBrowser().getDrbdGraph().getDrbdInfo();
                cleanup();
                setInfoPanel(null);
                oBdi.cleanup();
                oBdi.setInfoPanel(null);
                drbdInfo.addDrbdResource(null,
                                         null,
                                         thisClass,
                                         oBdi,
                                         true,
                                         testOnly);
            }
        };
    }

    /** Creates popup for the block device. */
    @Override public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final BlockDevInfo thisClass = this;
        if (!getBlockDevice().isDrbd() && !getBlockDevice().isAvailable()) {
            /* block devices are not available */
            return null;
        }
        final boolean testOnly = false;
        final MyMenu repMenuItem = new MyMenu(
                        Tools.getString("HostBrowser.Drbd.AddDrbdResource"),
                        new AccessMode(ConfigData.AccessType.ADMIN, false),
                        new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override public final String enablePredicate() {
                if (drbdResourceInfo != null) {
                    return "it is already a drbd resouce";
                } else if (!getHost().isConnected()) {
                    return Host.NOT_CONNECTED_STRING;
                } else if (!getHost().isDrbdLoaded()) {
                    return "drbd is not loaded";
                }
                return null;
                //return drbdResourceInfo == null
                //       && getHost().isConnected()
                //       && getHost().isDrbdLoaded();
            }

            @Override public void update() {
                super.update();
                Tools.invokeAndWait(new Runnable() {
                    @Override public void run() {
                        removeAll();
                    }
                });
                Cluster cluster = getHost().getCluster();
                Host[] otherHosts = cluster.getHostsArray();
                for (final Host oHost : otherHosts) {
                    if (oHost == getHost()) {
                        continue;
                    }
                    MyMenu hostMenu = new MyMenu(oHost.getName(),
                                                 new AccessMode(
                                                    ConfigData.AccessType.ADMIN,
                                                    false),
                                                 new AccessMode(
                                                    ConfigData.AccessType.OP,
                                                    false)) {
                        private static final long serialVersionUID = 1L;

                        @Override public final String enablePredicate() {
                            if (!oHost.isConnected()) {
                                return Host.NOT_CONNECTED_STRING;
                            } else if (!oHost.isDrbdLoaded()) {
                                return "drbd is not loaded";
                            } else {
                                return null;
                            }
                            //return oHost.isConnected()
                            //       && oHost.isDrbdLoaded();
                        }

                        @Override public final void update() {
                            super.update();
                            Tools.invokeAndWait(new Runnable() {
                                @Override public void run() {
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
                                if (oBdi.getDrbdResourceInfo() == null
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
                    add(hostMenu);
                }
            }
        };
        items.add(repMenuItem);
        /* attach / detach */
        final MyMenuItem attachMenu =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.Detach"),
                           NO_HARDDISK_ICON_LARGE,
                           Tools.getString("HostBrowser.Drbd.Detach.ToolTip"),

                           Tools.getString("HostBrowser.Drbd.Attach"),
                           HARDDISK_ICON_LARGE,
                           Tools.getString("HostBrowser.Drbd.Attach.ToolTip"),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override public boolean predicate() {
                    return !getBlockDevice().isDrbd()
                           || getBlockDevice().isAttached();
                }

                @Override public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override public String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdResourceInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return DrbdResourceInfo.IS_SYNCING_STRING;
                    }
                    return null;
                }

                @Override public void action() {
                    if (this.getText().equals(
                                Tools.getString("HostBrowser.Drbd.Attach"))) {
                        attach(testOnly);
                    } else {
                        detach(testOnly);
                    }
                }
            };
        final ClusterBrowser cb = getBrowser().getClusterBrowser();
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback attachItemCallback =
                            cb.new DRBDMenuItemCallback(attachMenu, getHost()) {
                @Override public void action(final Host host) {
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

                @Override public final boolean predicate() {
                    return isConnectedOrWF(testOnly);
                }

                @Override public final boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override public final String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdResourceInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isSyncing()
                        || ((getBlockDevice().isPrimary()
                            && getBlockDevice().isSyncSource())
                            || (getOtherBlockDevInfo().getBlockDevice().
                                                                isPrimary()
                                && getBlockDevice().isSyncTarget()))) {
                        return null;
                    } else {
                        return DrbdResourceInfo.IS_SYNCING_STRING;
                    }
                }

                @Override public void action() {
                    if (this.getText().equals(
                            Tools.getString("HostBrowser.Drbd.Connect"))) {
                        connect(testOnly);
                    } else {
                        disconnect(testOnly);
                    }
                }
            };
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback connectItemCallback =
                               cb.new DRBDMenuItemCallback(connectMenu,
                                                           getHost()) {
                @Override public void action(final Host host) {
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

                @Override public boolean predicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return false;
                    }
                    return getBlockDevice().isSecondary()
                         && getOtherBlockDevInfo().getBlockDevice().isPrimary();
                }

                @Override public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override public final String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdResourceInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isSecondary()) {
                        return "cannot do that to the primary";
                    }
                    return null;
                }

                @Override public void action() {
                    BlockDevInfo oBdi = getOtherBlockDevInfo();
                    if (oBdi != null && oBdi.getBlockDevice().isPrimary()) {
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

                @Override public boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override public final String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdResourceInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isPrimary()) {
                        return "cannot do that to the secondary";
                    }
                    return null;
                }

                @Override public void action() {
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

                @Override public final boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override public final String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdResourceInfo.IS_USED_BY_CRM_STRING;
                    }
                    return null;
                }

                @Override public void action() {
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

                @Override public final boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override public final String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdResourceInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return DrbdResourceInfo.IS_SYNCING_STRING;
                    }
                    if (getDrbdResourceInfo().isVerifying()) {
                        return DrbdResourceInfo.IS_VERIFYING_STRING;
                    }
                    return null;
                    //return !getBlockDevice().isSyncing()
                    //       && !getDrbdResourceInfo().isVerifying();
                }

                @Override public void action() {
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

                @Override public boolean predicate() {
                    return getBlockDevice().isSyncing()
                           && getBlockDevice().isPausedSync();
                }

                @Override public final boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override public final String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdResourceInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (!getBlockDevice().isSyncing()) {
                        return "it is not being synced";
                    }
                    return null;
                }

                @Override public void action() {
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

                @Override public final boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override public final String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdResourceInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return DrbdResourceInfo.IS_SYNCING_STRING;
                    }
                    return null;
                }

                @Override public void action() {
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

                @Override public final boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override public final String enablePredicate() {
                    if (!getBlockDevice().isDrbd()) {
                        return NO_DRBD_RESOURCE_STRING;
                    }
                    if (!Tools.getConfigData().isAdvancedMode()
                        && getDrbdResourceInfo().isUsedByCRM()) {
                        return DrbdResourceInfo.IS_USED_BY_CRM_STRING;
                    }
                    if (getBlockDevice().isSyncing()) {
                        return DrbdResourceInfo.IS_SYNCING_STRING;
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

                @Override public void action() {
                    discardData(testOnly);
                }
            };
        items.add(discardDataItem);

        /* view log */
        final MyMenuItem viewDrbdLogItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewDrbdLog"),
                           LOGFILE_ICON,
                           null,
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override public final boolean visiblePredicate() {
                    return getBlockDevice().isDrbd();
                }

                @Override public final String enablePredicate() {
                    return null;
                }

                @Override public void action() {
                    String device = getDrbdResourceInfo().getDevice();
                    drbd.gui.dialog.drbd.DrbdLog l =
                           new drbd.gui.dialog.drbd.DrbdLog(getHost(), device);
                    l.showDialog();
                }
            };
        items.add(viewDrbdLogItem);

        return items;
    }

    /** Returns how much of the block device is used. */
    @Override public int getUsed() {
        if (drbdResourceInfo != null) {
            return drbdResourceInfo.getUsed();
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

    /** Returns text that appears in the corner of the drbd graph. */
    public Subtext getRightCornerTextForDrbdGraph(final boolean testOnly) {
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
             return new Subtext(s, Color.BLUE, Color.BLACK);
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
        return dtd.isConnected(getHost(), drbdResourceInfo.getDevice())
               || (!dtd.isDisconnected(getHost(),
                                       drbdResourceInfo.getDevice())
                   && getBlockDevice().isConnectedOrWF());
    }

    /** Returns whether this device is diskless. */
    public boolean isDiskless(final boolean testOnly) {
        final DRBDtestData dtd = getDRBDtestData();
        final DrbdResourceInfo dri = drbdResourceInfo;
        if (testOnly && dtd != null && dri != null) {
            return dtd.isDiskless(getHost(), drbdResourceInfo.getDevice())
                   || (!dtd.isAttached(getHost(),
                                       drbdResourceInfo.getDevice())
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

    /** Compares ignoring case and using drbd device names if available. */
    @Override public int compareTo(final Object o) {
        String name;
        String oName;
        if (getBlockDevice().isDrbd()) {
            name = getDrbdResourceInfo().getDevice();
        } else {
            name = getName();
        }
        if (((BlockDevInfo) o).getBlockDevice().isDrbd()) {
            oName = ((BlockDevInfo) o).getDrbdResourceInfo().getDevice();
        } else {
            oName = ((BlockDevInfo) o).getName();
        }
        return name.compareToIgnoreCase(oName);
    }

    /** Sets stored parameters. */
    public void setParameters(final String resName) {
        getBlockDevice().setNew(false);
        final ClusterBrowser clusterBrowser = getBrowser().getClusterBrowser();
        if (clusterBrowser == null) {
            return;
        }
        final DrbdXML dxml = clusterBrowser.getDrbdXML();
        final String hostName = getHost().getName();
        final DrbdGraph drbdGraph = getBrowser().getDrbdGraph();
        String value = null;
        for (final String param : getParametersFromXML()) {
            if (DRBD_NI_PORT_PARAM.equals(param)) {
                value = dxml.getVirtualInterfacePort(hostName, resName);
            } else if (DRBD_NI_PARAM.equals(param)) {
                value = dxml.getVirtualInterface(hostName, resName);
            } else if (DRBD_MD_PARAM.equals(param)) {
                value = dxml.getMetaDisk(hostName, resName);
                if (!"internal".equals(value)) {
                    final BlockDevInfo mdI =
                                   drbdGraph.findBlockDevInfo(hostName, value);
                    if (mdI != null) {
                        getBlockDevice().setMetaDisk(mdI.getBlockDevice());
                    }
                }
            } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
                value = dxml.getMetaDiskIndex(hostName, resName);
            }
            final String defaultValue = getParamDefault(param);
            if (value == null) {
                value = defaultValue;
            }
            if (value == null) {
                value = "";
            }
            final String oldValue = getParamSaved(param);
            final GuiComboBox cb = paramComboBoxGet(param, null);
            if (!Tools.areEqual(value, oldValue)) {
                getResource().setValue(param, value);
                if (cb != null) {
                    cb.setValue(value);
                }
            }
        }
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    @Override public boolean checkResourceFieldsChanged(final String param,
                                                        final String[] params) {
        return checkResourceFieldsChanged(param, params, false, false);
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
                                   final boolean fromDrbdResourceInfo) {
        final DrbdResourceInfo dri = getDrbdResourceInfo();
        if (dri != null && !fromDrbdResourceInfo) {
            if (!fromDrbdInfo) {
                dri.setApplyButtons(null, dri.getParametersFromXML());
            }
            //return dri.checkResourceFieldsChanged(param,
            //                                      dri.getParametersFromXML(),
            //                                      fromDrbdInfo);
        }
        return super.checkResourceFieldsChanged(param, params);
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override public boolean checkResourceFieldsCorrect(final String param,
                                                        final String[] params) {
        return checkResourceFieldsCorrect(param, params, false, false);
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
                                       final boolean fromDrbdResourceInfo) {
        return super.checkResourceFieldsCorrect(param, params);
    }

    /** Returns whether this block device is LVM. */
    public boolean isLVM() {
        return getBlockDevice().getVolumeGroup() != null;
    }

    /** Returns how much is free space in a volume group. */
    public Long getFreeInVolumeGroup() {
        return getHost().getFreeInVolumeGroup(
                                           getBlockDevice().getVolumeGroup());
    }
}
