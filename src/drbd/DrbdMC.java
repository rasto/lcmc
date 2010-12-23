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

/*
 * DrbdMC
 * (c) Linbit
 * written by Rasto Levrinc
 */
package drbd;

import drbd.gui.MainPanel;
import drbd.gui.MainMenu;
import drbd.gui.ClusterBrowser;
import drbd.gui.ProgressIndicatorPanel;
import drbd.data.ConfigData;
import drbd.utilities.Tools;
import drbd.utilities.RoboTest;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.ColorUIResource;
import javax.swing.JMenuBar;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.Container;
import java.util.Arrays;
import javax.swing.plaf.metal.OceanTheme;


/**
 * This is the central class with main function. It starts the DRBD GUI.
 */
public final class DrbdMC extends JPanel {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;

    /** Initial delay for showing any tool tip in milliseconds. */
    private static final int TOOLTIP_INITIAL_DELAY = 200;
    /** Dismiss delay for showing any tool tip in milliseconds. */
    private static final int TOOLTIP_DISMISS_DELAY = 100000;
    /**
     * Private constructor.
     */
    private DrbdMC() {
        /* no instantiation possible. */
    }
    /**
     * Create the GUI and show it.
     */
    protected static void createAndShowGUI(final Container mainFrame) {
        final java.util.List<Object> buttonGradient = Arrays.asList(
          new Object[] {new Float(.3f),
                        new Float(0f),
                        new ColorUIResource(ClusterBrowser.PANEL_BACKGROUND),
                        new ColorUIResource(0xFFFFFF),
                        new ColorUIResource(ClusterBrowser.STATUS_BACKGROUND)});
        final java.util.List<Object> checkboxGradient = Arrays.asList(
          new Object[] {new Float(.3f),
                        new Float(0f),
                        new ColorUIResource(ClusterBrowser.PANEL_BACKGROUND),
                        new ColorUIResource(ClusterBrowser.PANEL_BACKGROUND),
                        new ColorUIResource(0xFFFFFF)});
        try {
            /* Metal */
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            MetalLookAndFeel.setCurrentTheme(
                new OceanTheme() {
                    /** e.g. arrows on split pane... */
                    protected ColorUIResource getPrimary1() {
                        return new ColorUIResource(
                                            ClusterBrowser.STATUS_BACKGROUND);
                    }

                    /** unknown to me */
                    protected ColorUIResource getPrimary2() {
                        return new ColorUIResource(
                                            ClusterBrowser.PANEL_BACKGROUND);
                    }
                    /** unknown to me */
                    protected ColorUIResource getPrimary3() {
                        return new ColorUIResource(
                                            ClusterBrowser.PANEL_BACKGROUND);
                    }
                    /** Button and other borders. */
                    protected ColorUIResource getSecondary1() {
                        return new ColorUIResource(
                                  drbd.configs.AppDefaults.LINBIT_DARK_ORANGE);
                    }
                    protected ColorUIResource getSecondary2() {
                        return new ColorUIResource(
                                            ClusterBrowser.PANEL_BACKGROUND);
                    }
                    /** Split pane divider. Line in the main menu. */
                    protected ColorUIResource getSecondary3() {
                        return new ColorUIResource(
                                            ClusterBrowser.PANEL_BACKGROUND);
                    }
                }
            );

        } catch (final Exception e) {
            /* ignore it then */
        }
        ToolTipManager.sharedInstance().setInitialDelay(
                                                    TOOLTIP_INITIAL_DELAY);
        ToolTipManager.sharedInstance().setDismissDelay(
                                                    TOOLTIP_DISMISS_DELAY);
        UIManager.put("TableHeader.background",
                      Tools.getDefaultColor("DrbdMC.TableHeader"));
        UIManager.put("TableHeader.font",
                      UIManager.getFont("Label.font"));
        UIManager.put("Button.gradient", buttonGradient);
        UIManager.put("Button.select", ClusterBrowser.PANEL_BACKGROUND);

        UIManager.put("CheckBox.gradient", checkboxGradient);
        UIManager.put("CheckBoxMenuItem.gradient", checkboxGradient);
        UIManager.put("RadioButton.gradient", checkboxGradient);
        UIManager.put("RadioButton.rollover", Boolean.TRUE);
        UIManager.put("RadioButtonMenuItem.gradient", checkboxGradient);
        UIManager.put("ScrollBar.gradient", buttonGradient);
        UIManager.put("ToggleButton.gradient", buttonGradient);

        UIManager.put("Menu.selectionBackground",
                      ClusterBrowser.PANEL_BACKGROUND);
        UIManager.put("MenuItem.selectionBackground",
                      ClusterBrowser.PANEL_BACKGROUND);
        UIManager.put("List.selectionBackground",
                      ClusterBrowser.PANEL_BACKGROUND);
        UIManager.put("ComboBox.selectionBackground",
                      ClusterBrowser.PANEL_BACKGROUND);
        UIManager.put("OptionPane.background",
                      ClusterBrowser.STATUS_BACKGROUND);
        UIManager.put("Panel.background",
                      ClusterBrowser.PANEL_BACKGROUND);


        /* Create and set up the window. */
        Tools.getGUIData().setMainFrame(mainFrame);


        /* Display the window. */
        mainFrame.setSize(Tools.getDefaultInt("DrbdMC.width"),
                          Tools.getDefaultInt("DrbdMC.height"));
        mainFrame.setVisible(true);


    }

    /** Returns the main panel. */
    protected static JPanel getMainPanel() {
        final JPanel mainPanel = new MainPanel();
        Tools.getGUIData().setMainPanel(mainPanel);
        mainPanel.setOpaque(true); //content panes must be opaque
        return mainPanel;
    }

    /** Returns the menu bar. */
    protected static JMenuBar getMenuBar() {
        /* glass pane is used for progress bar etc. */
        final MainMenu menu = new MainMenu();
        Tools.getGUIData().setMainMenu(menu);
        return menu.getMenuBar();
    }

    /** Returns the main glass pane. */
    protected static ProgressIndicatorPanel getMainGlassPane() {
        final ProgressIndicatorPanel mainGlassPane =
                                             new ProgressIndicatorPanel();
        Tools.getGUIData().setMainGlassPane(mainGlassPane);
        return mainGlassPane;
    }

    /** Adds te exit listener and disconnects all hosts prior to exiting. */
    public static class ExitListener extends WindowAdapter {
        /**
         * Called when window is closed.
         */
        public final void windowClosing(final WindowEvent event) {
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    // TODO: don't try to reconnect when exiting
                    System.out.println("saving...");
                    for (int i = 0; i < 10; i++) {
                        System.out.println(".");
                        System.out.flush();
                        Tools.sleep(2000);
                    }
                    System.out.println();
                    System.out.println("force exit.");
                    System.exit(5);
                }
            });
            t.start();
            Tools.getGUIData().getMainFrame().setVisible(false);
            final String saveFile = Tools.getConfigData().getSaveFile();
            Tools.save(saveFile);
            Tools.getConfigData().disconnectAllHosts();
            System.exit(0);
        }
    }

    /** Inits the application. */
    protected static String initApp(final String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(final Thread t,
                                              final Throwable ex) {
                    Tools.appError("uncaught exception",
                                   ex.toString(),
                                   (Exception) ex);
                }
            });
        String autoArgs = null;
        boolean upgradeCheck = true;
        boolean pluginCheck = true;
        boolean auto = false;
        boolean tightvnc = false;
        boolean ultravnc = false;
        boolean realvnc = false;
        boolean vncportoffset = false;
        boolean opMode = false;
        float fps = 20.0f;
        for (final String arg : args) {
            if (vncportoffset && Tools.isNumber(arg)) {
                Tools.getConfigData().setVncPortOffset(Integer.parseInt(arg));
            }
            if ("--vnc-port-offset".equals(arg)) {
                vncportoffset = true;
                continue;
            } else {
                vncportoffset = false;
            }

            if ("--ro".equals(arg)
                || opMode && "ro".equals(arg)) {
                Tools.getConfigData().setAccessType(ConfigData.AccessType.RO);
                Tools.getConfigData().setMaxAccessType(
                                                  ConfigData.AccessType.RO);
            } else if ("--op".equals(arg)
                       || opMode && "op".equals(arg)) {
                Tools.getConfigData().setAccessType(ConfigData.AccessType.OP);
                Tools.getConfigData().setMaxAccessType(
                                          ConfigData.AccessType.OP);
            } else if ("--admin".equals(arg)
                       || opMode && "admin".equals(arg)) {
                Tools.getConfigData().setAccessType(
                                          ConfigData.AccessType.ADMIN);
                Tools.getConfigData().setMaxAccessType(
                                          ConfigData.AccessType.ADMIN);
            } else if (opMode) {
                Tools.appWarning("unknown operating mode: " + arg);
            }
            if ("--op-mode".equals(arg)
                || "--operating-mode".equals(arg)) {
                opMode = true;
                continue;
            } else {
                opMode = false;
            }

            if (auto) {
                autoArgs = arg;
            } else if ("--keep-helper".equals(arg)) {
                Tools.debug(null, "--keep-helper option specified");
                Tools.getConfigData().setKeepHelper(true);
            } else if ("--version".equals(arg)) {
                System.out.println("DRBD MANAGEMENT CONSOLE "
                                   + Tools.getRelease()
                                   + " by Rasto Levrinc");
                System.exit(0);
            } else if ("--help".equals(arg)) {
                System.out.println("DRBD MANAGEMENT CONSOLE: "
                                   + Tools.getRelease());
                System.out.println("--help, print this help.");
                System.out.println("--keep-helper, do not overwrite "
                                   + "the drbd-gui-helper program.");
                System.out.println("--auto for testing");
                System.out.println("--no-upgrade-check disable upgrade check");
                System.out.println("--no-plugin-check disable plugin check");
                System.out.println("--tightvnc, enable tight vnc viewer");
                System.out.println("--ultravnc, enable ultra vnc viewer");
                System.out.println("--realvnc, enable real vnc viewer");
                System.out.println(
                           "--vnc-port-offset OFFSET, for port forwarding");
                System.out.println(
                           "--staging-pacemaker, enable more installation"
                           + " options for pacemaker");
                System.out.println(
                   "--slow, specify this if you have slow computer. Can be"
                   + " specified more times");
                System.out.print(
                          "--op-mode MODE, operating mode. MODE can be: ");
                System.out.println("ro - read only");
                System.out.print(
                          "                                             ");
                System.out.println("op - operator");
                System.out.print(
                          "                                             ");
                System.out.println("admin - administrator");
                System.exit(0);
            } else if ("--tightvnc".equals(arg)) {
                tightvnc = true;
            } else if ("--ultravnc".equals(arg)) {
                ultravnc = true;
            } else if ("--realvnc".equals(arg)) {
                realvnc = true;
            } else if ("--no-upgrade-check".equals(arg)) {
                upgradeCheck = false;
            } else if ("--no-plugin-check".equals(arg)) {
                pluginCheck = false;
            } else if ("--auto".equals(arg)) {
                auto = true;
            } else if ("--staging-drbd".equals(arg)) {
                Tools.getConfigData().setStagingDrbd(true);
            } else if ("--staging-pacemaker".equals(arg)) {
                Tools.getConfigData().setStagingPacemaker(true);
            } else if ("--slow".equals(arg)) {
                fps = fps / 2;
            } else if ("--fast".equals(arg)) {
                /* undocumented */
                fps = fps * 2;
            } else if ("--restore-mouse".equals(arg)) {
                /* restore mouse if it is stuck in pressed state, during
                 * robot tests. */
                RoboTest.restoreMouse();
            }
        }
        Tools.getConfigData().setAnimFPS(fps);
        if (!tightvnc && !ultravnc && !realvnc) {
            if (Tools.isLinux()) {
                tightvnc = true;
            } else if (Tools.isWindows()) {
                ultravnc = true;
            } else {
                tightvnc = true;
                ultravnc = true;
            }
        }
        Tools.getConfigData().setUpgradeCheckEnabled(upgradeCheck);
        Tools.getConfigData().setPluginsEnabled(pluginCheck);
        Tools.getConfigData().setTightvnc(tightvnc);
        Tools.getConfigData().setUltravnc(ultravnc);
        Tools.getConfigData().setRealvnc(realvnc);
        return autoArgs;
    }

    /** The main function for starting the application. */
    public static void main(final String[] args) {
        try {
            Tools.init();
            final JFrame mainFrame = new JFrame(
               Tools.getString("DrbdMC.Title") + " " + Tools.getRelease());
            final String autoArgs = initApp(args);
            mainFrame.setGlassPane(getMainGlassPane());
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.addWindowListener(new ExitListener());
            mainFrame.setJMenuBar(getMenuBar());
            mainFrame.setContentPane(getMainPanel());
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createAndShowGUI((Container) mainFrame);
                }
            });
            if (autoArgs != null) {
                Tools.parseAutoArgs(autoArgs);
            }
        } catch (Exception e) {
            Tools.appError("Error in the application", "", e);
        }
    }
}
