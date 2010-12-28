/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.gui;

import drbd.utilities.Tools;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.gui.dialog.PluginLogin;
import drbd.AddHostDialog;
import drbd.AddClusterDialog;

import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JComponent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.SwingUtilities;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * An implementation of a menu panel.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class MainMenu extends JPanel implements ActionListener {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Menu bar. */
    private final JMenuBar menuBar;
    /** Plugins submenu. */
    private final JMenu pluginsMenu;
    /** Look and feel map. */
    private static final Map<String, String> LOOK_AND_FEEL_MAP =
                                                new HashMap<String, String>();
    /** Map from plugin name to it's menu item. */
    private final Map<String, JMenuItem> pluginHash =
                                               new HashMap<String, JMenuItem>();
    /**
     * because glassPane does not capture key events in my version of java,
     * the menu must turned off explicitly. */
    private boolean turnOff = false;
    /** Host icon. */
    private static final ImageIcon HOST_ICON =
                Tools.createImageIcon(Tools.getDefault("MainMenu.HostIcon"));

    /**
     * Prepares a new <code>MainMenu</code> object with main menu.
     */
    public MainMenu() {
        super();
        JMenu submenu, menuNew, menuLookAndFeel;

        menuBar = new JMenuBar();

        /* session */
        submenu = addMenu(Tools.getString("MainMenu.Session"), KeyEvent.VK_S);
        menuNew = addMenu(Tools.getString("MainMenu.New"), KeyEvent.VK_E);
        final JMenuItem hostItem = addMenuItem(Tools.getString("MainMenu.Host"),
                                               menuNew,
                                               KeyEvent.VK_H,
                                               KeyEvent.VK_N,
                                               newHostActionListener(),
                                               HOST_ICON);
        Tools.getGUIData().registerAddHostButton(hostItem);

        final JMenuItem cmi = addMenuItem(Tools.getString("MainMenu.Cluster"),
                                          menuNew,
                                          KeyEvent.VK_C,
                                          KeyEvent.VK_C,
                                          newClusterActionListener(),
                                          ClusterBrowser.CLUSTER_ICON_SMALL);

        Tools.getGUIData().registerAddClusterButton(cmi);
        Tools.getGUIData().checkAddClusterButtons();


        submenu.add(menuNew);

        final JMenuItem loadItem = addMenuItem(Tools.getString("MainMenu.Load"),
                                               submenu,
                                               KeyEvent.VK_L,
                                               KeyEvent.VK_L,
                                               loadActionListener(),
                                               null);
        Tools.getGUIData().addToEnabledInAccessType(loadItem,
                                                    new AccessMode(
                                                     ConfigData.AccessType.GOD,
                                                     false));

        final JMenuItem item = addMenuItem(
                                Tools.getString("MainMenu.RemoveEverything"),
                                submenu,
                                0,
                                0,
                                removeEverythingActionListener(),
                                null);
        Tools.getGUIData().addToVisibleInAccessType(item,
                                                    new AccessMode(
                                                     ConfigData.AccessType.GOD,
                                                     false));

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
        addMenuItem(Tools.getString("MainMenu.Exit"),
                    submenu,
                    KeyEvent.VK_X,
                    KeyEvent.VK_X,
                    exitActionListener(),
                    null);

        menuBar.add(submenu);

        /* plugins */
        pluginsMenu = addMenu(Tools.getString("MainMenu.Plugins"),
                              KeyEvent.VK_P);
        menuBar.add(pluginsMenu);

        /* settings */
        submenu = addMenu(Tools.getString("MainMenu.Settings"), 0);
        Tools.getGUIData().addToVisibleInAccessType(submenu,
                                                    new AccessMode(
                                                     ConfigData.AccessType.GOD,
                                                     false));
        menuLookAndFeel = addMenu(Tools.getString("MainMenu.LookAndFeel"), 0);
        final UIManager.LookAndFeelInfo[] lookAndFeels =
                                        UIManager.getInstalledLookAndFeels();
        for (int i = 0; i < lookAndFeels.length; i++) {
            final String className = lookAndFeels[i].getClassName();
            final String classNamePart =
                        className.substring(className.lastIndexOf('.') + 1);

            LOOK_AND_FEEL_MAP.put(classNamePart, className);
            addMenuItem(classNamePart,
                        menuLookAndFeel,
                        0,
                        0,
                        lookAndFeelActionListener(),
                        null);
        }
        submenu.add(menuLookAndFeel);
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

    }

    /**
     * Turn on menu.
     */
    public final void turnOn() {
        turnOff = false;
    }

    /**
     * Turn off menu.
     */
    public final void turnOff() {
        turnOff = true;
    }

    /**
     * Exit action listener.
     */
    private ActionListener exitActionListener() {
        return new ActionListener() {
             public void actionPerformed(final ActionEvent e) {
                 final Thread t = new Thread(new Runnable() {
                     public void run() {
                         Tools.getConfigData().disconnectAllHosts();
                         System.exit(0);
                     }
                 });
                 t.start();
             }
        };
    }

    /** Add new host action listener. */
    private ActionListener newHostActionListener() {
        return new ActionListener() {
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 final Thread t = new Thread(new Runnable() {
                     public void run() {
                         final AddHostDialog h = new AddHostDialog();
                         h.showDialogs();
                     }
                 });
                 t.start();
             }
        };
    }

    /** Add new plugin action listener. */
    private ActionListener newPluginActionListener(final String pluginName) {
        return new ActionListener() {
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 final Thread t = new Thread(new Runnable() {
                     public void run() {
                         Tools.showPluginDescription(pluginName);
                     }
                 });
                 t.start();
             }
        };
    }

    /** Add new register plugins action listener. */
    private ActionListener newRegisterPluginsActionListener() {
        return new ActionListener() {
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 final Thread t = new Thread(new Runnable() {
                     public void run() {
                         final PluginLogin pl = new PluginLogin(null);
                         pl.showDialog();
                     }
                 });
                 t.start();
             }
        };
    }

    /**
     * Load from file action listener.
     */
    private ActionListener loadActionListener() {
        return new ActionListener() {
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 final JFileChooser fc = new JFileChooser();
                 fc.setSelectedFile(new File(
                                    Tools.getConfigData().getSaveFile()));
                 final FileFilter filter = new FileFilter() {
                     public boolean accept(final File f) {
                         if (f.isDirectory()) {
                            return true;
                         }
                         String name = f.getName();
                         int i = name.lastIndexOf('.');
                         if (i > 0 && i < name.length() - 1) {
                             String ext = name.substring(i + 1);
                             if (ext.equals(Tools.getDefault(
                                        "MainMenu.DrbdGuiFiles.Extension"))) {
                                 return true;
                             }
                         }
                         return false;
                     }

                     public String getDescription() {
                         return Tools.getString("MainMenu.DrbdGuiFiles");
                     }
                 };
                 fc.setFileFilter(filter);
                 final int ret =
                        fc.showOpenDialog(Tools.getGUIData().getMainFrame());
                 if (ret == JFileChooser.APPROVE_OPTION) {
                     final String name =
                                    fc.getSelectedFile().getAbsolutePath();
                     Tools.getConfigData().setSaveFile(name);
                     Tools.loadConfigData(name);
                 }
             }
        };
    }

    /**
     * Remove everything action listener.
     */
    private ActionListener removeEverythingActionListener() {
        return new ActionListener() {
             public void actionPerformed(final ActionEvent e) {
                 final Thread thread = new Thread(
                    new Runnable() {
                        public void run() {
                            Tools.removeEverything();
                        }
                    }
                 );
                 thread.start();
             }
        };
    }

    /**
     * Save to file action listener.
     */
    private ActionListener saveActionListener() {
        return new ActionListener() {
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 final Thread thread = new Thread(
                    new Runnable() {
                        public void run() {
                            Tools.save(Tools.getConfigData().getSaveFile());
                        }
                    }
                 );
                 thread.start();
             }
        };
    }

    /**
     * 'Save as' action listener.
     */
    private ActionListener saveAsActionListener() {
        return new ActionListener() {
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 final JFileChooser fc = new JFileChooser();
                 fc.setSelectedFile(new File(
                                    Tools.getConfigData().getSaveFile()));
                 final FileFilter filter = new FileFilter() {
                     public boolean accept(final File f) {
                         if (f.isDirectory()) {
                            return true;
                         }
                         String name = f.getName();
                         int i = name.lastIndexOf('.');
                         if (i > 0 && i < name.length() - 1) {
                             String ext = name.substring(i + 1);
                             if (ext.equals(Tools.getDefault(
                                        "MainMenu.DrbdGuiFiles.Extension"))) {
                                 return true;
                             }
                         }
                         return false;
                     }

                     public String getDescription() {
                         return "Drbd GUI Files";
                     }
                 };
                 fc.setFileFilter(filter);
                 final int ret =
                        fc.showSaveDialog(Tools.getGUIData().getMainFrame());
                 if (ret == JFileChooser.APPROVE_OPTION) {
                     final String name =
                                    fc.getSelectedFile().getAbsolutePath();
                     Tools.getConfigData().setSaveFile(name);
                     Tools.save(Tools.getConfigData().getSaveFile());
                 }

             }
        };
    }

    /**
     * Add a new cluster action listener.
     */
    private ActionListener newClusterActionListener() {
        return new ActionListener() {
             public void actionPerformed(final ActionEvent e) {
                 if (turnOff) {
                     return;
                 }
                 final Thread t = new Thread(new Runnable() {
                     public void run() {
                         final AddClusterDialog c = new AddClusterDialog();
                         c.showDialogs();
                     }
                 });
                 t.start();
             }
        };
    }

    /**
     * Change look and feel action listener.
     */
    private ActionListener lookAndFeelActionListener() {
        return new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (turnOff) {
                     return;
                }
                try {
                    final String lookAndFeel =
                            LOOK_AND_FEEL_MAP.get(
                                    ((JMenuItem) e.getSource()).getText());

                    UIManager.setLookAndFeel(lookAndFeel);
                    final JComponent componentToSwitch =
                            Tools.getGUIData().getMainFrameRootPane();
                    SwingUtilities.updateComponentTreeUI(componentToSwitch);
                    componentToSwitch.invalidate();
                    componentToSwitch.validate();
                    componentToSwitch.repaint();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                } catch (InstantiationException ex) {
                    ex.printStackTrace();
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                } catch (UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    /**
     * About action listener.
     */
    private ActionListener aboutActionListener() {
        return new ActionListener() {
             public void actionPerformed(final ActionEvent e) {
                 final Thread t = new Thread(new Runnable() {
                     public void run() {
                         final drbd.gui.dialog.About a =
                                                new drbd.gui.dialog.About();
                         a.showDialog();
                     }
                 });
                 t.start();
             }
        };
    }

    /**
     * Returns the JMenuBar object.
     */
    public final JMenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Action performed, to catch not implemented actions.
     */
    public final void actionPerformed(final ActionEvent e) {
        final JMenuItem source = (JMenuItem) (e.getSource());
        Tools.appError("action \"" + source.getText() + "\" not implemented");
    }

    /**
     * Adds sub menu.
     */
    private JMenu addMenu(final String name, final int shortcut) {
        final JMenu submenu = new JMenu(name);
        if (shortcut != 0) {
            submenu.setMnemonic(shortcut);
        }
        return submenu;
    }

    /**
     * Adds menu item.
     */
    private JMenuItem addMenuItem(final String name,
                                  final JMenu parentMenu,
                                  final int shortcut,
                                  final int accelerator,
                                  final ActionListener al,
                                  final ImageIcon icon) {
        final JMenuItem item = new JMenuItem(name);
        if (shortcut != 0) {
            item.setMnemonic(shortcut);
        }
        if (accelerator != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(
                                    accelerator,
                                    ActionEvent.ALT_MASK));
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

    /** Reloads plugin menu. */
    public final void reloadPluginsMenu(final Set<String> pluginList) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pluginsMenu.removeAll();
                final JMenuItem hostItem =
                        addMenuItem(Tools.getString("MainMenu.RegisterPlugins"),
                                    pluginsMenu,
                                    KeyEvent.VK_R,
                                    0,
                                    newRegisterPluginsActionListener(),
                                    null);
                for (final String pluginName : pluginList) {
                    String newName = pluginName;
                    final String[] dirs = newName.split(":");
                    JMenu submenu = pluginsMenu;
                    if (dirs.length > 1) {
                        for (int i = 0; i < dirs.length - 1; i++) {
                            final JMenu m =
                                    addMenu(dirs[i].replaceAll("_", " "), 0);
                            submenu.add(m);
                            submenu = m;
                        }
                        newName = dirs[dirs.length - 1];
                    }
                    final JMenuItem pluginItem = addMenuItem(
                                           "About "
                                           + newName.replaceAll("_", " " ),
                                           submenu,
                                           0,
                                           0,
                                           newPluginActionListener(pluginName),
                                           null);
                    pluginHash.put(pluginName, pluginItem);
                    pluginItem.setEnabled(false);
                }
            }
        });
    }

    /** Enable plugin menu item. */
    public final void enablePluginMenu(final String pluginName,
                                       final boolean enable) {
        final JMenuItem item = pluginHash.get(pluginName);
        if (item != null) {
            item.setEnabled(enable);
        }
    }
}
