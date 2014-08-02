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
package lcmc.gui.resources.vms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileSystemView;

import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.model.LinuxFile;
import lcmc.model.StringValue;
import lcmc.model.vm.VmsXml;
import lcmc.model.Value;
import lcmc.model.resources.Resource;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.EditableInfo;
import lcmc.gui.resources.Info;
import lcmc.gui.widget.Check;
import lcmc.gui.widget.Widget;
import lcmc.utilities.ComponentWithTest;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.Unit;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.SshOutput;

import org.w3c.dom.Node;

/**
 * This class holds info about Virtual Hardware.
 */
public abstract class HardwareInfo extends EditableInfo {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(HardwareInfo.class);
    /** Back to overview icon. */
    private static final ImageIcon BACK_ICON = Tools.createImageIcon(
                                                 Tools.getDefault("BackIcon"));
    /** Pattern that parses stat output. */
    private static final Pattern STAT_PATTERN = Pattern.compile(
                                                       "(.).{9}\\s+(\\d+)\\s+"
                                                       + "(\\d+)\\s+"
                                                       + "(\\d+) (.*)$");
    /** Whether file chooser needs file or dir. */
    protected static final boolean FILECHOOSER_DIR_ONLY = true;
    protected static final boolean FILECHOOSER_FILE_ONLY =
                                                        !FILECHOOSER_DIR_ONLY;
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** VMS virtual domain info object. */
    private final DomainInfo vmsVirtualDomainInfo;
    /** Cache for files. */
    private final Map<String, LinuxFile> linuxFileCache =
                                            new HashMap<String, LinuxFile>();

    /** Creates the HardwareInfo object. */
    HardwareInfo(final String name,
                    final Browser browser,
                    final DomainInfo vmsVirtualDomainInfo) {
        super.init(name, browser);
        setResource(new Resource(name));
        this.vmsVirtualDomainInfo = vmsVirtualDomainInfo;
    }

    /** Returns browser object of this info. */
    @Override
    public final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Returns info panel. */
    @Override
    public final JComponent getInfoPanel() {
        Tools.isSwingThread();
        if (infoPanel != null) {
            return infoPanel;
        }
        final boolean abExisted = getApplyButton() != null;
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        final JTable headerTable = getTable(DomainInfo.HEADER_TABLE);
        if (headerTable != null) {
            mainPanel.add(headerTable.getTableHeader());
            mainPanel.add(headerTable);
        }
        addHardwareTable(mainPanel);

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final String[] params = getParametersFromXML();
        initApplyButton(null);
        /* add item listeners to the apply button. */
        if (!abExisted) {
            getApplyButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(Application.RunMode.LIVE);
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
            getRevertButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                revert();
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
        }
        final JPanel extraButtonPanel =
                           new JPanel(new FlowLayout(FlowLayout.TRAILING, 3, 0));
        extraButtonPanel.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.add(extraButtonPanel);
        addApplyButton(buttonPanel);
        addRevertButton(extraButtonPanel);
        final MyButton overviewButton = new MyButton("VM Host Overview",
                                                     BACK_ICON);
        overviewButton.miniButton();
        overviewButton.setPreferredSize(new Dimension(130, 50));
        //overviewButton.setPreferredSize(new Dimension(200, 50));
        overviewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                vmsVirtualDomainInfo.selectMyself();
            }
        });
        extraButtonPanel.add(overviewButton);
        addParams(optionsPanel,
                  params,
                  ClusterBrowser.SERVICE_LABEL_WIDTH,
                  ClusterBrowser.SERVICE_FIELD_WIDTH * 2,
                  null);
        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);

        mainPanel.add(optionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
                              ClusterBrowser.SERVICE_LABEL_WIDTH
                              + ClusterBrowser.SERVICE_FIELD_WIDTH * 2 + 4));
        newPanel.add(new JScrollPane(mainPanel));
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                getApplyButton().setVisible(
                            !getVMSVirtualDomainInfo().getResource().isNew());
                setApplyButtons(null, params);
            }
        });
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /** Returns units. */
    @Override
    protected final Unit[] getUnits(final String param) {
        return VmsXml.getUnits();
    }

    /** Returns the default unit for the parameter. */
    protected final String getDefaultUnit(final String param) {
        return null;
    }

    /** Returns columns for the table. */
    @Override
    protected final String[] getColumnNames(final String tableName) {
        return vmsVirtualDomainInfo.getColumnNames(tableName);
    }

    /** Execute when row in the table was clicked. */
    @Override
    protected final void rowClicked(final String tableName,
                                    final String key,
                                    final int column) {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (isControlButton(tableName, column)) {
                    vmsVirtualDomainInfo.rowClicked(tableName, key, column);
                } else {
                    vmsVirtualDomainInfo.selectMyself();
                }
            }
        });
        thread.start();
    }

    /** Retrurns color for some rows. */
    @Override
    protected final Color getTableRowColor(final String tableName,
                                           final String key) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo.getTableRowColor(tableName, key);
        }
        return Browser.PANEL_BACKGROUND;
    }

    /** Alignment for the specified column. */
    @Override
    protected final int getTableColumnAlignment(final String tableName,
                                                final int column) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo.getTableColumnAlignment(tableName,
                                                                column);
        }
        return SwingConstants.LEFT;
    }

    /** Returns info object for this row. */
    @Override
    protected final Info getTableInfo(final String tableName,
                                      final String key) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo;
        }
        return null;
    }

    /** Return info object of the whole domain. */
    protected final DomainInfo getVMSVirtualDomainInfo() {
        return vmsVirtualDomainInfo;
    }

    /** Get first host that has this vm and is connected. */
    protected final Host getFirstConnectedHost() {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null && h.isConnected()) {
                return h;
            }
        }
        return null;
    }

    /**
     * Returns whether this item is removeable (null), or string why it isn't.
     */
    protected abstract String isRemoveable();

    /** Updates parameters. */
    abstract void updateParameters();

    /** Returns list of menu items. */
    @Override
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        /* remove service */
        final ComponentWithTest removeMenuItem = new MyMenuItem(
                    Tools.getString("HardwareInfo.Menu.Remove"),
                    ClusterBrowser.REMOVE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    Tools.getString("HardwareInfo.Menu.Cancel"),
                    ClusterBrowser.REMOVE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    new AccessMode(Application.AccessType.ADMIN, false),
                    new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean predicate() {
                return !getResource().isNew();
            }

            @Override
            public String enablePredicate() {
                if (getResource().isNew()) {
                    return null;
                }
                return isRemoveable();
            }

            @Override
            public void action() {
                hidePopup();
                removeMyself(Application.RunMode.LIVE);
            }
        };
        addMouseOverListener(removeMenuItem, null);
        items.add((UpdatableItem) removeMenuItem);
        return items;
    }

    /** Removes this hardware from the libvirt with confirmation dialog. */
    @Override
    public final void removeMyself(final Application.RunMode runMode) {
        if (getResource().isNew()) {
            super.removeMyself(runMode);
            getResource().setNew(false);
            removeNode();
            return;
        }
        String desc = Tools.getString(
                                "HardwareInfo.confirmRemove.Description");

        desc  = desc.replaceAll("@HW@", Matcher.quoteReplacement(toString()));
        if (Tools.confirmDialog(
               Tools.getString("HardwareInfo.confirmRemove.Title"),
               desc,
               Tools.getString("HardwareInfo.confirmRemove.Yes"),
               Tools.getString("HardwareInfo.confirmRemove.No"))) {
            removeMyselfNoConfirm(runMode);
            getResource().setNew(false);
        }
    }

    /** Removes this disk without confirmation dialog. */
    protected abstract void removeMyselfNoConfirm(final Application.RunMode runMode);

    /** Applies the changes. */
    abstract void apply(final Application.RunMode runMode);

    /** Adds disk table with only this disk to the main panel. */
    protected abstract void addHardwareTable(final JPanel mainPanel);

    /** Modify device xml. */
    protected abstract void modifyXML(final VmsXml vmsXml,
                                      final Node node,
                                      final String domainName,
                                      final Map<String, String> params);

    /** Returns whether the column is a button, 0 column is always a button. */
    @Override
    protected final Map<Integer, Integer> getDefaultWidths(
                                                    final String tableName) {
        return vmsVirtualDomainInfo.getDefaultWidths(tableName);
    }

    /** Returns device parameters. */
    protected Map<String, String> getHWParameters(final boolean allParams) {
        Tools.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                getInfoPanel();
            }
        });
        final Map<String, String> parameters = new HashMap<String, String>();
        for (final String param : getParametersFromXML()) {
            final Value value = getComboBoxValue(param);
            if (allParams
                || !getParamSaved(param).equals(value)) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value.getValueForConfig());
                }
            }
        }
        return parameters;
    }


    /** Returns default widths for columns. Null for computed width. */
    @Override
    protected final boolean isControlButton(final String tableName,
                                            final int column) {
        return vmsVirtualDomainInfo.isControlButton(tableName, column);
    }

    /** Returns tool tip text in the table. */
    @Override
    protected final String getTableToolTip(final String tableName,
                                           final String key,
                                           final Object object,
                                           final int raw,
                                           final int column) {
        return vmsVirtualDomainInfo.getTableToolTip(tableName,
                                                    key,
                                                    object,
                                                    raw,
                                                    column);
    }

    /** Returns cached file object. */
    public final LinuxFile getLinuxDir(final String dir, final Host host) {
        LinuxFile ret = linuxFileCache.get(dir);
        if (ret == null) {
            ret = new LinuxFile(this, host, dir, "d", 0, 0);
            linuxFileCache.put(dir, ret);
        }
        return ret;
    }

    /** Returns file system view that allows remote browsing. */
    private FileSystemView getFileSystemView(final Host host,
                                             final String directory) {
        final HardwareInfo thisClass = this;
        return new FileSystemView() {
            @Override
            public File[] getRoots() {
                return new LinuxFile[]{getLinuxDir("/", host)};
            }

            @Override
            public boolean isRoot(final File f) {
                final String path = Tools.getUnixPath(f.toString());
                return "/".equals(path);
            }

            @Override
            public File createNewFolder(final File containingDir) {
                return null;
            }

            @Override
            public File getHomeDirectory() {
                return getLinuxDir(directory, host);
            }

            @Override
            public Boolean isTraversable(final File f) {
                final LinuxFile lf = linuxFileCache.get(f.toString());
                return lf == null || lf.isDirectory();
            }

            @Override
            public File getParentDirectory(final File dir) {
                return getLinuxDir(dir.getParent(), host);
            }

            @Override
            public File[] getFiles(final File dir,
                                   final boolean useFileHiding) {
                final StringBuilder dirSB = new StringBuilder(dir.toString());
                if ("/".equals(dir.toString())) {
                    dirSB.append('*');
                } else {
                    dirSB.append("/*");
                }
                final SshOutput out = host.captureCommandProgressIndicator(
                        "executing...",
                        new ExecCommandConfig().command("stat -c \"%A %a %Y %s %n\" " + dirSB + " 2>/dev/null")
                                               .silentOutput());
                final List<LinuxFile> files = new ArrayList<LinuxFile>();
                if (out.getExitCode() == 0) {
                    for (final String line : out.getOutput().split("\r\n")) {
                        if (line.trim().isEmpty()) {
                            continue;
                        }
                        final Matcher m = STAT_PATTERN.matcher(line);
                        if (m.matches()) {
                            final String type = m.group(1);
                            final long lastModified =
                                           Long.parseLong(m.group(3)) * 1000;
                            final long size = Long.parseLong(m.group(4));
                            final String filename = m.group(5);
                            LinuxFile lf = linuxFileCache.get(filename);
                            if (lf == null) {
                                lf = new LinuxFile(thisClass,
                                                   host,
                                                   filename,
                                                   type,
                                                   lastModified,
                                                   size);
                                linuxFileCache.put(filename, lf);
                            } else {
                                lf.update(type, lastModified, size);
                            }
                            files.add(lf);
                        } else {
                            LOG.appWarning("getFileSystemView: could not match: " + line);
                        }
                    }
                }
                return files.toArray(new LinuxFile[files.size()]);
            }
        };
    }

    /**
     * Starts file chooser.
     * @param directory whether it needs dir or file
     */
    protected final void startFileChooser(final Widget paramWi,
                                          final String directory,
                                          final boolean dirOnly) {
        final Host host = getFirstConnectedHost();
        if (host == null) {
            LOG.error("startFileChooser: connection to host lost.");
            return;
        }
        final HardwareInfo thisClass = this;
        final JFileChooser fc = new JFileChooser(
                                    getLinuxDir(directory, host),
                                    getFileSystemView(host, directory)) {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;
                @Override
                public void setCurrentDirectory(final File dir) {
                    super.setCurrentDirectory(new LinuxFile(
                                                    thisClass,
                                                    host,
                                                    dir.toString(),
                                                    "d",
                                                    0,
                                                    0));
                }

            };
        fc.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        fc.setDialogType(JFileChooser.CUSTOM_DIALOG);
        if (dirOnly) {
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        fc.setDialogTitle(Tools.getString("DiskInfo.FileChooserTitle")
                          + host.getName());
//        fc.setApproveButtonText(Tools.getString("DiskInfo.Approve"));
        fc.setApproveButtonToolTipText(
                               Tools.getString("DiskInfo.Approve.ToolTip"));
        fc.putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
        final int ret = fc.showDialog(Tools.getGUIData().getMainFrame(),
                                      Tools.getString("DiskInfo.Approve"));
        linuxFileCache.clear();
        if (ret == JFileChooser.APPROVE_OPTION
            && fc.getSelectedFile() != null) {
            final String name = fc.getSelectedFile().getAbsolutePath();
            paramWi.setValue(new StringValue(name));
        }
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. Don't check the invisible for the type parameters.
     */
    @Override
    public final Check checkResourceFields(final String param,
                                           final String[] params) {
        return checkResourceFields(param, params, false);
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. Don't check the invisible for the type parameters.
     */
    final Check checkResourceFields(final String param,
                                    final String[] params,
                                    final boolean fromDomain) {
        final DomainInfo vdi = vmsVirtualDomainInfo;
        if (!fromDomain && vdi != null && params.length != 1) {
            vdi.setApplyButtons(null, vdi.getParametersFromXML());
        }
        final String[] parameters;
        if (params == null || params.length == 1) {
            /* just one param */
            parameters = params;
        } else {
            parameters = getRealParametersFromXML();
        }
        return super.checkResourceFields(param, parameters);
    }

    /** Checks one parameter. */
    protected final void checkOneParam(final String param) {
        checkResourceFields(param, new String[]{param}, true);
    }

    /** Returns parameters. */
    String[] getRealParametersFromXML() {
        return getParametersFromXML();
    }

    /** Saves all preferred values. */
    public final void savePreferredValues() {
        for (final String param : getParametersFromXML()) {
            final Value pv = getParamPreferred(param);
            if (pv != null) {
                getResource().setValue(param, pv);
            }
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HardwareInfo other = (HardwareInfo) obj;
        if (this.getName() != other.getName() && (this.getName() == null || !this.getName().equals(other.getName()))) {
            return false;
        }
        return this.vmsVirtualDomainInfo == other.vmsVirtualDomainInfo
               || (this.vmsVirtualDomainInfo != null
                   && this.vmsVirtualDomainInfo.equals(
                                                  other.vmsVirtualDomainInfo));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.getName() != null ? this.getName().hashCode() : 0);
        hash = 97 * hash + (this.vmsVirtualDomainInfo != null ? this.vmsVirtualDomainInfo.hashCode() : 0);
        return hash;
    }
}
