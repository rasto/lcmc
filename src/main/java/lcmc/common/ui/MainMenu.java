/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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
package lcmc.common.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileFilter;

import lcmc.Exceptions;
import lcmc.cluster.domain.ClusterStarter;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.wizard.AddClusterDialog;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.UserConfig;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.utils.Dialogs;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostFactory;
import lcmc.host.ui.AddHostDialog;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * An implementation of a menu panel.
 */
@Named
@Singleton
public final class MainMenu extends JPanel implements ActionListener {
    private static final Logger LOG = LoggerFactory.getLogger(MainMenu.class);
    private static final Map<String, String> LOOK_AND_FEEL_MAP = new HashMap<>();
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(Tools.getDefault("MainMenu.HostIcon"));
    private JMenuBar menuBar;
    private boolean turnOff = false;
    private JComboBox<String> operatingModesCB;
    private final JEditorPane upgradeTextField = new JEditorPane(MainData.MIME_TYPE_TEXT_HTML, "");
    private final JEditorPane infoTextField = new JEditorPane(MainData.MIME_TYPE_TEXT_HTML, "");
    private String upgradeCheck = "";
    private String infoText = null;
    private final JPanel infoTextPanel = new JPanel();
    private final Provider<AddClusterDialog> addClusterDialogProvider;
    private final UserConfig userConfig;
    private final Provider<AddHostDialog> addHostDialogProvider;
    private final HostFactory hostFactory;
    private final MainData mainData;
    private final MainPresenter mainPresenter;
    private final Application application;
    private final SwingUtils swingUtils;
    private final BugReport bugReport;
    private final About aboutDialog;
    private final Dialogs dialogs;
    private final Access access;
    private final ClusterStarter clusterStarter;

    public MainMenu(UserConfig userConfig, Provider<AddClusterDialog> addClusterDialogProvider,
            Provider<AddHostDialog> addHostDialogProvider, HostFactory hostFactory, MainData mainData, MainPresenter mainPresenter,
            Application application, SwingUtils swingUtils, Access access, BugReport bugReport, About aboutDialog, Dialogs dialogs,
            ClusterStarter clusterStarter) {
        this.userConfig = userConfig;
        this.addClusterDialogProvider = addClusterDialogProvider;
        this.addHostDialogProvider = addHostDialogProvider;
        this.hostFactory = hostFactory;
        this.mainData = mainData;
        this.mainPresenter = mainPresenter;
        this.application = application;
        this.swingUtils = swingUtils;
        this.access = access;
        this.bugReport = bugReport;
        this.aboutDialog = aboutDialog;
        this.dialogs = dialogs;
        this.clusterStarter = clusterStarter;
    }

    public void init() {
        if (application.isUpgradeCheckEnabled()) {
            upgradeCheck = Tools.getString("MainPanel.UpgradeCheck");
        } else {
            upgradeCheck = Tools.getString("MainPanel.UpgradeCheckDisabled");
        }
        menuBar = new JMenuBar();

        /* session */
        JMenu submenu = addMenu(Tools.getString("MainMenu.Session"), KeyEvent.VK_S);
        final JMenu menuNew = addMenu(Tools.getString("MainMenu.New"), KeyEvent.VK_E);
        final JMenuItem hostItem = addMenuItem(Tools.getString("MainMenu.Host"),
                                               menuNew,
                                               KeyEvent.VK_H,
                                               KeyEvent.VK_N,
                                               newHostActionListener(),
                                               HOST_ICON);
        mainData.registerAddHostButton(hostItem);

        final JMenuItem cmi = addMenuItem(Tools.getString("MainMenu.Cluster"),
                                          menuNew,
                                          0,
                                          0,
                                          newClusterActionListener(),
                                          ClusterBrowser.CLUSTER_ICON_SMALL);

        mainData.registerAddClusterButton(cmi);
        mainPresenter.checkAddClusterButtons();


        submenu.add(menuNew);

        final JMenuItem loadItem = addMenuItem(Tools.getString("MainMenu.Load"),
                                               submenu,
                                               KeyEvent.VK_L,
                                               KeyEvent.VK_L,
                                               loadActionListener(),
                                               null);
        access.addToEnabledInAccessType(loadItem, new AccessMode(AccessMode.GOD, AccessMode.NORMAL));

        final JMenuItem item = addMenuItem(Tools.getString("MainMenu.RemoveEverything"),
                                           submenu,
                                           0,
                                           0,
                                           removeEverythingActionListener(),
                                           null);
        access.addToVisibleInAccessType(item, new AccessMode(AccessMode.GOD, AccessMode.NORMAL));

        addMenuItem(Tools.getString("MainMenu.Save"),
                    submenu,
                    KeyEvent.VK_S,
                    0,
                    saveActionListener(),
                    null);

        addMenuItem(Tools.getString("MainMenu.SaveAs"),
                    submenu,
                    KeyEvent.VK_A,
                    0,
                    saveAsActionListener(),
                    null);

        submenu.addSeparator();
        if (!mainData.isApplet()) {
            addMenuItem(Tools.getString("MainMenu.Exit"),
                        submenu,
                        KeyEvent.VK_X,
                        KeyEvent.VK_X,
                        exitActionListener(),
                        null);
        }
        menuBar.add(submenu);

        /* settings */
        submenu = addMenu(Tools.getString("MainMenu.Settings"), 0);
        access.addToVisibleInAccessType(submenu, new AccessMode(AccessMode.GOD, AccessMode.NORMAL));
        final JMenu menuLookAndFeel = addMenu(Tools.getString("MainMenu.LookAndFeel"), 0);
        final UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
        for (final UIManager.LookAndFeelInfo lookAndFeel : lookAndFeels) {
            final String className = lookAndFeel.getClassName();
            final String classNamePart = className.substring(className.lastIndexOf('.') + 1);
            LOOK_AND_FEEL_MAP.put(classNamePart, className);
            addMenuItem(classNamePart, menuLookAndFeel, 0, 0, lookAndFeelActionListener(), null);
        }
        submenu.add(menuLookAndFeel);
        menuBar.add(submenu);

        /* Edit */
        submenu = addMenu(Tools.getString("MainMenu.Edit"), KeyEvent.VK_E);
        addMenuItem(Tools.getString("MainMenu.Copy"),
                    submenu,
                    KeyEvent.VK_O,
                    KeyEvent.VK_C,
                    copyActionListener(),
                    null);
        addMenuItem(Tools.getString("MainMenu.Paste"),
                    submenu,
                    KeyEvent.VK_P,
                    KeyEvent.VK_V,
                    pasteActionListener(),
                    null);
        menuBar.add(submenu);

        /* help */
        submenu = addMenu(Tools.getString("MainMenu.Help"), KeyEvent.VK_H);
        addMenuItem(Tools.getString("MainMenu.About"),
                    submenu,
                    KeyEvent.VK_A,
                    0,
                    aboutActionListener(),
                    null);

        menuBar.add(submenu);

        /* Bug Report */
        addMenuItem(Tools.getString("MainMenu.BugReport"),
                    submenu,
                    KeyEvent.VK_B,
                    0,
                    bugReportActionListener(),
                    null);

        menuBar.add(submenu);

        /* advanced mode button */
        final var advancedModeCB = createAdvancedModeButton();
        /* Operating mode */
        operatingModesCB = createOperationModeCb();
        final JPanel opModePanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        menuBar.add(getInfoTextField());
        opModePanel.add(getUpgradeTextField());
        opModePanel.add(operatingModesCB);
        opModePanel.add(advancedModeCB);
        // workaround for menuBar invalidating throwing an exception
        opModePanel.setPreferredSize(new Dimension(300, 1));

        menuBar.add(opModePanel);
        if (application.isUpgradeCheckEnabled()) {
            startUpgradeCheck();
        }
    }

    /** Turn on menu. */
    public void turnOn() {
        turnOff = false;
    }

    /** Turn off menu. */
    public void turnOff() {
        turnOff = true;
    }

    private ActionListener exitActionListener() {
        return e -> {
            LOG.debug1("actionPerformed: MENU ACTION: exit");
            final Thread t = new Thread(() -> {
                application.disconnectAllHosts();
                System.exit(0);
            });
            t.start();
        };
    }

    private ActionListener newHostActionListener() {
        return e -> {
            if (turnOff) {
                return;
            }
            LOG.debug1("actionPerformed: MENU ACTION: new host");
            final Thread t = new Thread(() -> {
                final Host host = hostFactory.createInstance();
                addHostDialogProvider.get().showDialogs(host);
            });
            t.start();
        };
    }

    /** Load from file action listener. */
    private ActionListener loadActionListener() {
        return e -> {
            if (turnOff) {
                return;
            }
            LOG.debug1("actionPerformed: MENU ACTION: load");
            final Optional<String> name = dialogs.getLcmcConfFilename();
            if (name.isPresent()) {
                application.setDefaultSaveFile(name.get());
                loadConfigData(name.get());
            }
        };
    }

    private void loadConfigData(final String filename) {
        LOG.debug("loadConfigData: start");
        final String xml = Tools.loadFile(mainPresenter, filename, true);
        if (xml == null) {
            return;
        }
        clusterStarter.startClusters(null);
        mainPresenter.allHostsUpdate();
    }

    private ActionListener removeEverythingActionListener() {
        return e -> {
            LOG.debug1("actionPerformed: MENU ACTION: remove everything");
            final Thread thread = new Thread(mainPresenter::removeEverything);
            thread.start();
        };
    }

    /** Save to file action listener. */
    private ActionListener saveActionListener() {
        return e -> {
            if (turnOff) {
                return;
            }
            LOG.debug1("actionPerformed: MENU ACTION: save");
            final Thread thread = new Thread(() -> userConfig.saveConfig(application.getDefaultSaveFile(), true));
            thread.start();
        };
    }

    private ActionListener saveAsActionListener() {
        return e -> {
            if (turnOff) {
                return;
            }
            LOG.debug1("actionPerformed: MENU ACTION: save as");
            final JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(application.getDefaultSaveFile()));
            final FileFilter filter = new FileFilter() {
                @Override
                public boolean accept(final File f) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    final String name = f.getName();
                    final int i = name.lastIndexOf('.');
                    if (i > 0 && i < name.length() - 1) {
                        final String ext = name.substring(i + 1);
                        return ext.equals(Tools.getDefault("MainMenu.DrbdGuiFiles.Extension"));
                    }
                    return false;
                }

                @Override
                public String getDescription() {
                    return "LCMC GUI Files";
                }
            };
            fc.setFileFilter(filter);
            final int ret = fc.showSaveDialog(mainData.getMainFrame());
            if (ret == JFileChooser.APPROVE_OPTION) {
                final String name = fc.getSelectedFile().getAbsolutePath();
                application.setDefaultSaveFile(name);
                userConfig.saveConfig(application.getDefaultSaveFile(), true);
            }
        };
    }

    private ActionListener newClusterActionListener() {
        return e -> {
            if (turnOff) {
                return;
            }
            LOG.debug1("actionPerformed: MENU ACTION: new cluster");
            final Thread t = new Thread(() -> addClusterDialogProvider.get().showDialogs());
            t.start();
        };
    }

    private ActionListener lookAndFeelActionListener() {
        return e -> {
            if (turnOff) {
                return;
            }
            LOG.debug1("actionPerformed: MENU ACTION: look and feel");
            final String lookAndFeel = LOOK_AND_FEEL_MAP.get(((AbstractButton) e.getSource()).getText());

            try {
                UIManager.setLookAndFeel(lookAndFeel);
                final JComponent componentToSwitch = mainData.getMainFrameRootPane();
                SwingUtilities.updateComponentTreeUI(componentToSwitch);
                componentToSwitch.invalidate();
                componentToSwitch.validate();
                componentToSwitch.repaint();
            } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                throw new RuntimeException("cannot set look and feel: " + lookAndFeel, ex);
            }
        };
    }

    private ActionListener copyActionListener() {
        return e -> {
            LOG.debug1("actionPerformed: MENU ACTION: copy");
            final Thread t = new Thread(mainData::copy);
            t.start();
        };
    }

    private ActionListener pasteActionListener() {
        return e -> {
            LOG.debug1("actionPerformed: MENU ACTION: paste");
            final Thread t = new Thread(mainData::paste);
            t.start();
        };
    }

    private ActionListener aboutActionListener() {
        return e -> {
            LOG.debug1("actionPerformed: MENU ACTION: about");
            final Thread t = new Thread(aboutDialog::showDialog);
            t.start();
        };
    }

    /** Bug report action listener. */
    private ActionListener bugReportActionListener() {
        return e -> {
            LOG.debug1("actionPerformed: MENU ACTION: bug report");
            final Thread t = new Thread(() -> {
                bugReport.init(BugReport.UNKNOWN_CLUSTER, BugReport.NO_ERROR_TEXT);
                bugReport.showDialog();
            });
            t.start();
        };
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    /** Action performed, to catch not implemented actions. */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final JMenuItem source = (JMenuItem) e.getSource();
        LOG.appError("actionPerformed: action \"" + source.getText() + "\" not implemented");
    }

    /** Adds sub menu. */
    private JMenu addMenu(final String name, final int shortcut) {
        final JMenu submenu = new JMenu(name);
        if (shortcut != 0) {
            submenu.setMnemonic(shortcut);
        }
        return submenu;
    }

    private JMenuItem addMenuItem(final String name,
                                  final JMenu parentMenu,
                                  final int shortcut,
                                  final int accelerator,
                                  final ActionListener al,
                                  final Icon icon) {
        final JMenuItem item = new JMenuItem(name);
        if (shortcut != 0) {
            item.setMnemonic(shortcut);
        }
        if (accelerator != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke( accelerator, ActionEvent.CTRL_MASK));
        }
        item.addActionListener(Objects.requireNonNullElse(al, this));
        if (icon != null) {
            item.setIcon(icon);
        }
        parentMenu.add(item);
        return item;
    }

    /** Returns advanced mode check box. That hides advanced options. */
    private JCheckBox createAdvancedModeButton() {
        final JCheckBox emCb = new JCheckBox(Tools.getString("Browser.AdvancedMode"));
        emCb.setSelected(access.isAdvancedMode());
        emCb.addItemListener(e -> {
            final boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            if (selected != access.isAdvancedMode()) {
                final Thread thread = new Thread(() -> {
                    access.setAdvancedMode(selected);
                    access.checkAccessOfEverything();
                });
                thread.start();
            }
        });
        emCb.setToolTipText(Tools.getString("MainMenu.OperatingMode.ToolTip"));
        return emCb;
    }

    private JComboBox<String> createOperationModeCb() {
        final String[] modes = access.getOperatingModes();
        final JComboBox<String> opModeCB = new JComboBox<>(modes);

        final AccessMode.Type accessType = access.getAccessType();
        opModeCB.setSelectedItem(AccessMode.OP_MODES_MAP.get(accessType));
        opModeCB.addItemListener(e -> {
            final String opMode = (String) e.getItem();
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final Thread thread = new Thread(() -> {
                    AccessMode.Type type = AccessMode.ACCESS_TYPE_MAP.get(opMode);
                    if (type == null) {
                        LOG.appError("run: unknown mode: " + opMode);
                        type = AccessMode.RO;
                    }
                    access.setAccessType(type);
                    access.checkAccessOfEverything();
                });
                thread.start();
            }
        });
        opModeCB.setToolTipText(Tools.getString("MainMenu.OperatingMode.ToolTip"));
        return opModeCB;
    }

    /** Modify the operating modes combo box according to the godmode. */
    public void resetOperatingModes(final boolean godMode) {
        swingUtils.invokeLater(() -> {
            if (godMode) {
                operatingModesCB.addItem(AccessMode.OP_MODE_GOD);
                operatingModesCB.setSelectedItem(AccessMode.OP_MODE_GOD);
            } else {
                operatingModesCB.removeItem(AccessMode.OP_MODE_GOD);
            }
        });
    }

    /** Sets operating mode. */
    public void setOperatingMode(final String opMode) {
        swingUtils.invokeLater(() -> operatingModesCB.setSelectedItem(opMode));
    }

    private void startUpgradeCheck() {
        final Thread thread = new Thread(() -> {
            final String[] serverCheck = Tools.getLatestVersion();
            final String latestVersion = serverCheck[0];
            infoText = serverCheck[1];
            if (latestVersion == null) {
                upgradeCheck = "";
            } else {
                final String release = Tools.getRelease();
                try {
                    if (Tools.compareVersions(release, latestVersion) < 0) {
                        upgradeCheck = Tools.getString("MainPanel.UpgradeAvailable").replaceAll("@LATEST@", latestVersion);
                    } else {
                        upgradeCheck = Tools.getString("MainPanel.NoUpgradeAvailable");
                    }
                } catch (final Exceptions.IllegalVersionException e) {
                    upgradeCheck = Tools.getString("MainPanel.UpgradeCheckFailed");
                }
            }
            final String text = upgradeCheck;
            swingUtils.invokeLater(() -> {
                upgradeTextField.setText(text);
                upgradeTextField.setVisible(text != null && !text.isEmpty());
                infoTextField.setText(infoText);
                infoTextPanel.setVisible(infoText != null);
            });
        });
        thread.start();
    }

    /**
     * Return upgrade text field, that will be updated, when upgrade check is
     * done.
     */
    private JEditorPane getUpgradeTextField() {
        final Border border = new LineBorder(Color.RED);
        upgradeTextField.setBorder(border);
        Tools.setEditorFont(upgradeTextField);
        upgradeTextField.setEditable(false);
        upgradeTextField.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                Tools.openBrowser(e.getURL().toString());
            }
        });
        upgradeTextField.setBackground(Color.WHITE);
        final String text = upgradeCheck;
        upgradeTextField.setText(text);
        upgradeTextField.setVisible(text != null && !text.isEmpty());
        return upgradeTextField;
    }

    /**
     * Return info text field, that will be updated, when info from the server
     * is ready.
     */
    private JPanel getInfoTextField() {
        final Border border = new LineBorder(Color.RED);
        infoTextField.setBorder(border);
        Tools.setEditorFont(infoTextField);
        infoTextField.setEditable(false);
        infoTextField.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                Tools.openBrowser(e.getURL().toString());
            }
        });
        infoTextField.setBackground(Color.WHITE);
        final String text = infoText;
        infoTextField.setText(text);
        infoTextPanel.add(infoTextField);
        infoTextPanel.setVisible(text != null);
        return infoTextPanel;
    }
}
