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
package lcmc.vm.ui.resource;

import java.awt.BorderLayout;
import java.awt.Color;
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

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileSystemView;

import lcmc.common.ui.main.MainData;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.vm.domain.LinuxFile;
import lcmc.common.domain.StringValue;
import lcmc.vm.domain.VmsXml;
import lcmc.common.domain.Value;
import lcmc.common.domain.ResourceValue;
import lcmc.common.ui.Browser;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.ui.EditableInfo;
import lcmc.common.ui.Info;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.common.domain.EnablePredicate;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.domain.Predicate;
import lcmc.common.domain.util.Tools;
import lcmc.common.domain.Unit;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.SshOutput;

import org.w3c.dom.Node;

/**
 * This class holds info about Virtual Hardware.
 */
@Named
public abstract class HardwareInfo extends EditableInfo {
    private static final Logger LOG = LoggerFactory.getLogger(HardwareInfo.class);
    /**
     * Back to overview icon.
     */
    private static final ImageIcon BACK_ICON = Tools.createImageIcon(Tools.getDefault("BackIcon"));
    /**
     * Pattern that parses stat output.
     */
    private static final Pattern STAT_PATTERN = Pattern.compile("(.).{9}\\s+(\\d+)\\s+(\\d+)\\s+(\\d+) (.*)$");
    /**
     * Whether file chooser needs file or dir.
     */
    protected static final boolean FILECHOOSER_DIR_ONLY = true;
    protected static final boolean FILECHOOSER_FILE_ONLY = !FILECHOOSER_DIR_ONLY;
    private JComponent infoPanel = null;
    private DomainInfo vmsVirtualDomainInfo;
    private final Map<String, LinuxFile> linuxFileCache = new HashMap<String, LinuxFile>();
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private WidgetFactory widgetFactory;
    @Inject
    private MainData mainData;
    @Inject
    private TreeMenuController treeMenuController;

    void init(final String name, final Browser browser, final DomainInfo vmsVirtualDomainInfo) {
        super.init(name, browser);
        setResource(new ResourceValue(name));
        this.vmsVirtualDomainInfo = vmsVirtualDomainInfo;
    }

    @Override
    public final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    @Override
    public final JComponent getInfoPanel() {
        swingUtils.isSwingThread();
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
        optionsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        final String[] params = getParametersFromXML();
        initApplyButton(null);
        /* add item listeners to the apply button. */
        if (!abExisted) {
            getApplyButton().addAction(new Runnable() {
                @Override
                public void run() {
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
            });
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
        final JPanel extraButtonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 3, 0));
        extraButtonPanel.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.add(extraButtonPanel);
        addApplyButton(buttonPanel);
        addRevertButton(extraButtonPanel);
        final MyButton overviewButton = widgetFactory.createButton("VM Host Overview", BACK_ICON);
        application.makeMiniButton(overviewButton);
        overviewButton.setPreferredSize(new Dimension(130, 50));
        overviewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                vmsVirtualDomainInfo.selectMyself();
            }
        });
        extraButtonPanel.add(overviewButton);
        addParams(optionsPanel,
                  params,
                  application.getServiceLabelWidth(),
                  application.getServiceFieldWidth() * 2,
                  null);
        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);

        mainPanel.add(optionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(application.getServiceLabelWidth()
                                         + application.getServiceFieldWidth() * 2 + 4));
        newPanel.add(new JScrollPane(mainPanel));
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                getApplyButton().setVisible(!getVMSVirtualDomainInfo().getResource().isNew());
                setApplyButtons(null, params);
            }
        });
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    @Override
    protected final Unit[] getUnits(final String param) {
        return VmsXml.getUnits();
    }

    /**
     * Returns the default unit for the parameter.
     */
    protected final String getDefaultUnit(final String param) {
        return null;
    }

    /**
     * Returns columns for the table.
     */
    @Override
    protected final String[] getColumnNames(final String tableName) {
        return vmsVirtualDomainInfo.getColumnNames(tableName);
    }

    /**
     * Execute when row in the table was clicked.
     */
    @Override
    protected final void rowClicked(final String tableName, final String key, final int column) {
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

    /**
     * Retrurns color for some rows.
     */
    @Override
    protected final Color getTableRowColor(final String tableName, final String key) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo.getTableRowColor(tableName, key);
        }
        return Browser.PANEL_BACKGROUND;
    }

    @Override
    protected final int getTableColumnAlignment(final String tableName, final int column) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo.getTableColumnAlignment(tableName, column);
        }
        return SwingConstants.LEFT;
    }

    /**
     * Returns info object for this row.
     */
    @Override
    protected final Info getTableInfo(final String tableName, final String key) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo;
        }
        return null;
    }

    protected final DomainInfo getVMSVirtualDomainInfo() {
        return vmsVirtualDomainInfo;
    }

    /**
     * Get first host that has this vm and is connected.
     */
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

    /**
     * Updates parameters.
     */
    abstract void updateParameters();

    /**
     * Returns list of menu items.
     */
    @Override
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        /* remove service */
        final ComponentWithTest removeMenuItem = menuFactory.createMenuItem(
                Tools.getString("HardwareInfo.Menu.Remove"),
                ClusterBrowser.REMOVE_ICON,
                ClusterBrowser.STARTING_PTEST_TOOLTIP,
                Tools.getString("HardwareInfo.Menu.Cancel"),
                ClusterBrowser.REMOVE_ICON,
                ClusterBrowser.STARTING_PTEST_TOOLTIP,
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .predicate(new Predicate() {
                    @Override
                    public boolean check() {
                        return !getResource().isNew();
                    }
                })
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (getResource().isNew()) {
                            return null;
                        }
                        return isRemoveable();
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        hidePopup();
                        removeMyself(Application.RunMode.LIVE);
                    }
                });
        addMouseOverListener(removeMenuItem, null);
        items.add((UpdatableItem) removeMenuItem);
        return items;
    }

    /**
     * Removes this hardware from the libvirt with confirmation dialog.
     */
    @Override
    public final void removeMyself(final Application.RunMode runMode) {
        if (getResource().isNew()) {
            super.removeMyself(runMode);
            getResource().setNew(false);
            treeMenuController.removeNode(getNode());
            return;
        }
        String desc = Tools.getString("HardwareInfo.confirmRemove.Description");

        desc = desc.replaceAll("@HW@", Matcher.quoteReplacement(toString()));
        if (application.confirmDialog(
                Tools.getString("HardwareInfo.confirmRemove.Title"),
                desc,
                Tools.getString("HardwareInfo.confirmRemove.Yes"),
                Tools.getString("HardwareInfo.confirmRemove.No"))) {
            removeMyselfNoConfirm(runMode);
            getResource().setNew(false);
        }
    }

    /**
     * Removes this disk without confirmation dialog.
     */
    protected abstract void removeMyselfNoConfirm(final Application.RunMode runMode);

    /**
     * Applies the changes.
     */
    abstract void apply(final Application.RunMode runMode);

    /**
     * Adds disk table with only this disk to the main panel.
     */
    protected abstract void addHardwareTable(final JPanel mainPanel);

    /**
     * Modify device xml.
     */
    protected abstract void modifyXML(final VmsXml vmsXml,
                                      final Node node,
                                      final String domainName,
                                      final Map<String, String> params);

    /**
     * Returns whether the column is a button, 0 column is always a button.
     */
    @Override
    protected final Map<Integer, Integer> getDefaultWidths(final String tableName) {
        return vmsVirtualDomainInfo.getDefaultWidths(tableName);
    }

    protected Map<String, String> getHWParameters(final boolean allParams) {
        swingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                getInfoPanel();
            }
        });
        final Map<String, String> parameters = new HashMap<String, String>();
        for (final String param : getParametersFromXML()) {
            final Value value = getComboBoxValue(param);
            if (allParams || !getParamSaved(param).equals(value)) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value.getValueForConfig());
                }
            }
        }
        return parameters;
    }

    /**
     * Returns default widths for columns. Null for computed width.
     */
    @Override
    protected final boolean isControlButton(final String tableName, final int column) {
        return vmsVirtualDomainInfo.isControlButton(tableName, column);
    }

    /**
     * Returns tool tip text in the table.
     */
    @Override
    protected final String getTableToolTip(final String tableName,
                                           final String key,
                                           final Object object,
                                           final int raw,
                                           final int column) {
        return vmsVirtualDomainInfo.getTableToolTip(tableName, key, object, raw, column);
    }

    /**
     * Returns cached file object.
     */
    public final LinuxFile getLinuxDir(final String dir, final Host host) {
        LinuxFile ret = linuxFileCache.get(dir);
        if (ret == null) {
            ret = new LinuxFile(this, host, dir, "d", 0, 0);
            linuxFileCache.put(dir, ret);
        }
        return ret;
    }

    /**
     * Returns file system view that allows remote browsing.
     */
    private FileSystemView getFileSystemView(final Host host, final String directory) {
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
            public File[] getFiles(final File dir, final boolean useFileHiding) {
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
                            final long lastModified = Long.parseLong(m.group(3)) * 1000;
                            final long size = Long.parseLong(m.group(4));
                            final String filename = m.group(5);
                            LinuxFile lf = linuxFileCache.get(filename);
                            if (lf == null) {
                                lf = new LinuxFile(thisClass, host, filename, type, lastModified, size);
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
        final JFileChooser fc = new JFileChooser(getLinuxDir(directory, host), getFileSystemView(host, directory)) {
            @Override
            public void setCurrentDirectory(final File dir) {
                super.setCurrentDirectory(new LinuxFile(thisClass, host, dir.toString(), "d", 0, 0));
            }
        };
        fc.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        fc.setDialogType(JFileChooser.CUSTOM_DIALOG);
        if (dirOnly) {
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        fc.setDialogTitle(Tools.getString("DiskInfo.FileChooserTitle") + host.getName());
        fc.setApproveButtonToolTipText(Tools.getString("DiskInfo.Approve.ToolTip"));
        fc.putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
        final int ret = fc.showDialog(mainData.getMainFrame(), Tools.getString("DiskInfo.Approve"));
        linuxFileCache.clear();
        if (ret == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
            final String name = fc.getSelectedFile().getAbsolutePath();
            paramWi.setValue(new StringValue(name));
        }
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. Don't check the invisible for the type parameters.
     */
    @Override
    public final Check checkResourceFields(final String param, final String[] params) {
        return checkResourceFields(param, params, false);
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. Don't check the invisible for the type parameters.
     */
    final Check checkResourceFields(final String param, final String[] params, final boolean fromDomain) {
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

    protected final void checkOneParam(final String param) {
        checkResourceFields(param, new String[]{param}, true);
    }

    String[] getRealParametersFromXML() {
        return getParametersFromXML();
    }

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
