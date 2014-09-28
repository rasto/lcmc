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
package lcmc.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
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
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import lcmc.cluster.ui.wizard.AddClusterDialog;
import lcmc.host.ui.AddHostDialog;
import lcmc.Exceptions;
import lcmc.common.ui.About;
import lcmc.common.ui.BugReport;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostFactory;
import lcmc.common.domain.UserConfig;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * An implementation of a menu panel.
 */
@Named
@Singleton
public final class MainMenu extends JPanel implements ActionListener {
    private static final Logger LOG = LoggerFactory.getLogger(MainMenu.class);
    /** Look and feel map. */
    private static final Map<String, String> LOOK_AND_FEEL_MAP = new HashMap<String, String>();
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(Tools.getDefault("MainMenu.HostIcon"));
    private JMenuBar menuBar;
    /**
     * because glassPane does not capture key events in my version of java,
     * the menu must turned off explicitly. */
    private boolean turnOff = false;
    /** Combo box with operating modes. */
    private JComboBox<String> operatingModesCB;
    /** Advanced mode button. */
    private JCheckBox advancedModeCB;
    /** Upgrade check text field. */
    private final JEditorPane upgradeTextField = new JEditorPane(GUIData.MIME_TYPE_TEXT_HTML, "");
    private final JEditorPane infoTextField = new JEditorPane(GUIData.MIME_TYPE_TEXT_HTML, "");
    /** Upgrade check text. */
    private String upgradeCheck = "";
    private String infoText = null;
    private final JPanel infoTextPanel = new JPanel();
    @Inject
    private Provider<AddClusterDialog> addClusterDialogProvider;
    @Inject
    private UserConfig userConfig;
    @Inject
    private Provider<AddHostDialog> addHostDialogProvider;
    @Inject
    private HostFactory hostFactory;
    @Inject
    private GUIData guiData;
    @Inject
    private Application application;
    @Inject
    private BugReport bugReport;
    @Inject
    private About aboutDialog;

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
        guiData.registerAddHostButton(hostItem);

        final JMenuItem cmi = addMenuItem(Tools.getString("MainMenu.Cluster"),
                                          menuNew,
                                          0,
                                          0,
                                          newClusterActionListener(),
                                          ClusterBrowser.CLUSTER_ICON_SMALL);

        guiData.registerAddClusterButton(cmi);
        guiData.checkAddClusterButtons();


        submenu.add(menuNew);

        final JMenuItem loadItem = addMenuItem(Tools.getString("MainMenu.Load"),
                                               submenu,
                                               KeyEvent.VK_L,
                                               KeyEvent.VK_L,
                                               loadActionListener(),
                                               null);
        guiData.addToEnabledInAccessType(loadItem, new AccessMode(Application.AccessType.GOD, false));

        final JMenuItem item = addMenuItem(Tools.getString("MainMenu.RemoveEverything"),
                                           submenu,
                                           0,
                                           0,
                                           removeEverythingActionListener(),
                                           null);
        guiData.addToVisibleInAccessType(item, new AccessMode(Application.AccessType.GOD, false));

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
        if (!guiData.isApplet()) {
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
        guiData.addToVisibleInAccessType(submenu, new AccessMode(Application.AccessType.GOD, false));
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
        advancedModeCB = createAdvancedModeButton();
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
    void turnOn() {
        turnOff = false;
    }

    /** Turn off menu. */
    void turnOff() {
        turnOff = true;
    }

    private ActionListener exitActionListener() {
        return new ActionListener() {
             @Override
             public void actionPerformed(final ActionEvent e) {
                 LOG.debug1("actionPerformed: MENU ACTION: exit");
                 final Thread t = new Thread(new Runnable() {
                     @Override
                     public void run() {
                         application.disconnectAllHosts();
                         System.exit(0);
                     }
                 });
                 t.start();
             }
        };
    }

    private ActionListener newHostActionListener() {
        return new ActionListener() {
             @Override
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 LOG.debug1("actionPerformed: MENU ACTION: new host");
                 final Thread t = new Thread(new Runnable() {
                     @Override
                     public void run() {
                         final Host host = hostFactory.createInstance();
                         addHostDialogProvider.get().showDialogs(host);
                     }
                 });
                 t.start();
             }
        };
    }

    /** Load from file action listener. */
    private ActionListener loadActionListener() {
        return new ActionListener() {
             @Override
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 LOG.debug1("actionPerformed: MENU ACTION: load");
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
                             if (ext.equals(Tools.getDefault("MainMenu.DrbdGuiFiles.Extension"))) {
                                 return true;
                             }
                         }
                         return false;
                     }

                     @Override
                     public String getDescription() {
                         return Tools.getString("MainMenu.DrbdGuiFiles");
                     }
                 };
                 fc.setFileFilter(filter);
                 final int ret = fc.showOpenDialog(guiData.getMainFrame());
                 if (ret == JFileChooser.APPROVE_OPTION) {
                     final String name = fc.getSelectedFile().getAbsolutePath();
                     application.setDefaultSaveFile(name);
                     loadConfigData(name);
                 }
             }
        };
    }

    private void loadConfigData(final String filename) {
        LOG.debug("loadConfigData: start");
        final String xml = Tools.loadFile(guiData, filename, true);
        if (xml == null) {
            return;
        }
        userConfig.startClusters(null);
        guiData.allHostsUpdate();
    }

    private ActionListener removeEverythingActionListener() {
        return new ActionListener() {
             @Override
             public void actionPerformed(final ActionEvent e) {
                 LOG.debug1("actionPerformed: MENU ACTION: remove everything");
                 final Thread thread = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            application.removeEverything();
                        }
                    }
                 );
                 thread.start();
             }
        };
    }

    /** Save to file action listener. */
    private ActionListener saveActionListener() {
        return new ActionListener() {
             @Override
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 LOG.debug1("actionPerformed: MENU ACTION: save");
                 final Thread thread = new Thread(new Runnable() {
                     @Override
                     public void run() {
                         application.saveConfig(application.getDefaultSaveFile(), true);
                     }
                 });
                 thread.start();
             }
        };
    }

    private ActionListener saveAsActionListener() {
        return new ActionListener() {
             @Override
             public void actionPerformed(final ActionEvent e) {
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
                             if (ext.equals(Tools.getDefault("MainMenu.DrbdGuiFiles.Extension"))) {
                                 return true;
                             }
                         }
                         return false;
                     }

                     @Override
                     public String getDescription() {
                         return "LCMC GUI Files";
                     }
                 };
                 fc.setFileFilter(filter);
                 final int ret = fc.showSaveDialog(guiData.getMainFrame());
                 if (ret == JFileChooser.APPROVE_OPTION) {
                     final String name = fc.getSelectedFile().getAbsolutePath();
                     application.setDefaultSaveFile(name);
                     application.saveConfig(application.getDefaultSaveFile(), true);
                 }
             }
        };
    }

    private ActionListener newClusterActionListener() {
        return new ActionListener() {
             @Override
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 LOG.debug1("actionPerformed: MENU ACTION: new cluster");
                 final Thread t = new Thread(new Runnable() {
                     @Override
                     public void run() {
                         addClusterDialogProvider.get().showDialogs();
                     }
                 });
                 t.start();
             }
        };
    }

    private ActionListener lookAndFeelActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (turnOff) {
                     return;
                }
                LOG.debug1("actionPerformed: MENU ACTION: look and feel");
                final String lookAndFeel = LOOK_AND_FEEL_MAP.get(((AbstractButton) e.getSource()).getText());

                try {
                    UIManager.setLookAndFeel(lookAndFeel);
                    final JComponent componentToSwitch = guiData.getMainFrameRootPane();
                    SwingUtilities.updateComponentTreeUI(componentToSwitch);
                    componentToSwitch.invalidate();
                    componentToSwitch.validate();
                    componentToSwitch.repaint();
                } catch (final ClassNotFoundException ex) {
                    throw new RuntimeException("cannot set look and feel: " + lookAndFeel, ex);
                } catch (final InstantiationException ex) {
                    throw new RuntimeException("cannot set look and feel: " + lookAndFeel, ex);
                } catch (final IllegalAccessException ex) {
                    throw new RuntimeException("cannot set look and feel: " + lookAndFeel, ex);
                } catch (final UnsupportedLookAndFeelException ex) {
                    throw new RuntimeException("cannot set look and feel: " + lookAndFeel, ex);
                }
            }
        };
    }

    private ActionListener copyActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LOG.debug1("actionPerformed: MENU ACTION: copy");
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        guiData.copy();
                    }
                });
                t.start();
            }
        };
    }

    private ActionListener pasteActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LOG.debug1("actionPerformed: MENU ACTION: paste");
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        guiData.paste();
                    }
                });
                t.start();
            }
        };
    }

    private ActionListener aboutActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LOG.debug1("actionPerformed: MENU ACTION: about");
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        aboutDialog.showDialog();
                    }
                });
                t.start();
            }
        };
    }

    /** Bug report action listener. */
    private ActionListener bugReportActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LOG.debug1("actionPerformed: MENU ACTION: bug report");
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        bugReport.init(BugReport.UNKNOWN_CLUSTER, BugReport.NO_ERROR_TEXT);
                        bugReport.showDialog();
                    }
                });
                t.start();
            }
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
        if (al == null) {
            item.addActionListener(this);
        } else {
            item.addActionListener(al);
        }
        if (icon != null) {
            item.setIcon(icon);
        }
        parentMenu.add(item);
        return item;
    }

    /** Returns advanced mode check box. That hides advanced options. */
    private JCheckBox createAdvancedModeButton() {
        final JCheckBox emCb = new JCheckBox(Tools.getString("Browser.AdvancedMode"));
        emCb.setSelected(application.isAdvancedMode());
        emCb.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final boolean selected = e.getStateChange() == ItemEvent.SELECTED;
                if (selected != application.isAdvancedMode()) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            application.setAdvancedMode(selected);
                            application.checkAccessOfEverything();
                        }
                    });
                    thread.start();
                }
            }
        });
        emCb.setToolTipText(Tools.getString("MainMenu.OperatingMode.ToolTip"));
        return emCb;
    }

    private JComboBox<String> createOperationModeCb() {
        final String[] modes = application.getOperatingModes();
        final JComboBox<String> opModeCB = new JComboBox<String>(modes);

        final Application.AccessType accessType = application.getAccessType();
        opModeCB.setSelectedItem(Application.OP_MODES_MAP.get(accessType));
        opModeCB.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final String opMode = (String) e.getItem();
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Application.AccessType type = Application.ACCESS_TYPE_MAP.get(opMode);
                            if (type == null) {
                                LOG.appError("run: unknown mode: " + opMode);
                                type = Application.AccessType.RO;
                            }
                            application.setAccessType(type);
                            application.checkAccessOfEverything();
                        }
                    });
                    thread.start();
                }
            }
        });
        opModeCB.setToolTipText(Tools.getString("MainMenu.OperatingMode.ToolTip"));
        return opModeCB;
    }

    /** Modify the operating modes combo box according to the godmode. */
    void resetOperatingModes(final boolean godMode) {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (godMode) {
                    operatingModesCB.addItem(Application.OP_MODE_GOD);
                    operatingModesCB.setSelectedItem(Application.OP_MODE_GOD);
                } else {
                    operatingModesCB.removeItem(Application.OP_MODE_GOD);
                }
            }
        });
    }

    /** Sets operating mode. */
    public void setOperatingMode(final String opMode) {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                operatingModesCB.setSelectedItem(opMode);
            }
        });
    }

    private void startUpgradeCheck() {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final String[] serverCheck = Tools.getLatestVersion();
                final String latestVersion = serverCheck[0];
                infoText = serverCheck[1];
                if (latestVersion == null) {
                    upgradeCheck = "";
                } else {
                    final String release = Tools.getRelease();
                    try {
                        if (Tools.compareVersions(release, latestVersion) < 0) {
                            upgradeCheck =
                                Tools.getString("MainPanel.UpgradeAvailable").replaceAll("@LATEST@", latestVersion);
                        } else {
                            upgradeCheck = Tools.getString("MainPanel.NoUpgradeAvailable");
                        }
                    } catch (final Exceptions.IllegalVersionException e) {
                        upgradeCheck = Tools.getString("MainPanel.UpgradeCheckFailed");
                    }
                }
                final String text = upgradeCheck;
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        upgradeTextField.setText(text);
                        upgradeTextField.setVisible(text != null && !text.isEmpty());
                        infoTextField.setText(infoText);
                        infoTextPanel.setVisible(infoText != null);
                    }
                });
            }
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
        upgradeTextField.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                    Tools.openBrowser(e.getURL().toString());
                }
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
        infoTextField.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                    Tools.openBrowser(e.getURL().toString());
                }
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
