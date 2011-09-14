/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2011, Rastislav Levrinc
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
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import javax.swing.plaf.metal.OceanTheme;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;



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

    /** The --help option. */
    private static final String HELP_OP = "help";
    /** The --version option. */
    private static final String VERSION_OP = "version";
    /** The --nolrm option. */
    private static final String NOLRM_OP = "nolrm";
    /** The --auto option. */
    private static final String AUTO_OP = "auto";
    /** The --ro option. */
    private static final String RO_OP = "ro";
    /** The --op option. */
    private static final String OP_OP = "op";
    /** The --admin option. */
    private static final String ADMIN_OP = "admin";
    /** The --op-mode option. */
    private static final String OP_MODE_OP = "op-mode";
    /** The --no-upgrade-check option. */
    private static final String NO_UPGRADE_CHECK_OP = "no-upgrade-check";
    /** The --no-plugin-check option. DEPRECATED, doesn't do anything */
    private static final String NO_PLUGIN_CHECK_OP = "no-plugin-check";
    /** The --tightvnc option. */
    private static final String TIGHTVNC_OP = "tightvnc";
    /** The --ultravnc option. */
    private static final String ULTRAVNC_OP = "ultravnc";
    /** The --realvnc option. */
    private static final String REALVNC_OP = "realvnc";
    /** The --big-drbd-conf option. */
    private static final String BIGDRBDCONF_OP = "big-drbd-conf";
    /** The --staging-drbd option. */
    private static final String STAGING_DRBD_OP = "staging-drbd";
    /** The --staging-pacemaker option. */
    private static final String STAGING_PACEMAKER_OP = "staging-pacemaker";
    /** The --vnc-port-offset option. */
    private static final String VNC_PORT_OFFSET_OP = "vnc-port-offset";
    /** The --slow option. */
    private static final String SLOW_OP = "slow";
    /** The --restore-mouse option. */
    private static final String RESTORE_MOUSE_OP = "restore-mouse";
    /** The --keep-helper option. */
    private static final String KEEP_HELPER_OP = "keep-helper";
    /** The --id-dsa option. */
    private static final String ID_DSA_OP = "id-dsa";
    /** The --id-rsa option. */
    private static final String ID_RSA_OP = "id-rsa";
    /** The --known-hosts option. */
    private static final String KNOWN_HOSTS_OP = "known-hosts";
    /** The --out option. */
    private static final String OUT_OP = "out";
    /** The --debug option. */
    private static final String DEBUG_OP = "debug";

    /**
     * Private constructor.
     */
    private DrbdMC() {
        /* no instantiation possible. */
    }
    /** Create the GUI and show it. */
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
                      ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        UIManager.put("MenuItem.selectionBackground",
                      ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        UIManager.put("List.selectionBackground",
                      ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        UIManager.put("ComboBox.selectionBackground",
                      ClusterBrowser.BUTTON_PANEL_BACKGROUND);
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
            cleanupBeforeClosing();
            System.exit(0);
        }
    }

    /** Cleanup before closing. */
    public static void cleanupBeforeClosing() {
        final Thread t = new Thread(new Runnable() {
            @Override public void run() {
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
    }

    /** Inits the application. */
    protected static String initApp(final String[] args) {
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
                                  drbd.configs.AppDefaults.BACKGROUND_DARK);
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
        Thread.setDefaultUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(final Thread t,
                                              final Throwable ex) {
                    Tools.appError("uncaught exception",
                                   ex.toString(),
                                   (Exception) ex);
                }
            });
        float fps = 20.0f;
        final Options options = new Options();

        options.addOption("h", HELP_OP, false, "print this help");
        options.addOption(null,
                          KEEP_HELPER_OP,
                          false,
                          "do not overwrite the drbd-gui-helper program");
        options.addOption(null, RO_OP, false, "read only mode");
        options.addOption(null, OP_OP, false, "operator mode");
        options.addOption(null, ADMIN_OP, false, "administrator mode");
        options.addOption(null,
                          OP_MODE_OP,
                          true,
                          "operating mode. <arg> can be:\n"
                          + "ro - read only\n"
                          + "op - operator\n"
                          + "admin - administrator");
        options.addOption(null, NOLRM_OP, false,
                          "do not show removed resources from LRM.");
        options.addOption(null, "auto", true, "for testing");
        options.addOption("v", VERSION_OP, false, "print version");
        options.addOption(null, AUTO_OP, true, "for testing");
        options.addOption(null,
                          NO_UPGRADE_CHECK_OP,
                          false,
                          "disable upgrade check");
        options.addOption(
                      null,
                      NO_PLUGIN_CHECK_OP,
                      false,
                      "disable plugin check, DEPRECATED: there are no plugins");
        options.addOption(null, TIGHTVNC_OP, false, "enable tight vnc viewer");
        options.addOption(null, ULTRAVNC_OP, false, "enable ultra vnc viewer");
        options.addOption(null, REALVNC_OP, false, "enable real vnc viewer");
        options.addOption(null,
                          BIGDRBDCONF_OP,
                          false,
                          "create one big drbd.conf, instead of many"
                          + " files in drbd.d/ directory");
        options.addOption(null,
                          STAGING_DRBD_OP,
                          false,
                          "enable more DRBD installation options");
        options.addOption(null,
                          STAGING_PACEMAKER_OP,
                          false,
                          "enable more Pacemaker installation options");
        options.addOption(null,
                          VNC_PORT_OFFSET_OP,
                          true,
                          "offset for port forwarding");
        options.addOption(null,
                          SLOW_OP,
                          false,
                          "specify this if you have slow computer");
        options.addOption(null,
                          RESTORE_MOUSE_OP,
                          false,
                          "for testing");
        options.addOption(null,
                          ID_DSA_OP,
                          true,
                          "location of id_dsa file ($HOME/.ssh/id_dsa)");
        options.addOption(null,
                          ID_RSA_OP,
                          true,
                          "location of id_rsa file ($HOME/.ssh/id_rsa)");
        options.addOption(
                     null,
                     KNOWN_HOSTS_OP,
                     true,
                     "location of known_hosts file ($HOME/.ssh/known_hosts)");
        options.addOption(
                     null,
                     OUT_OP,
                     true,
                     "where to redirect the standard out");
        options.addOption(
                     null,
                     DEBUG_OP,
                     true,
                     "debug level, 0 - none, 3 - all");
        final CommandLineParser parser = new PosixParser();
        String autoArgs = null;
        try {
            final CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption(OUT_OP)) {
                final String out = cmd.getOptionValue(OUT_OP);
                if (out != null) {
                    try {
                        System.setOut(
                                    new PrintStream(new FileOutputStream(out)));
                    } catch (final FileNotFoundException e) {
                        System.exit(2);
                    }
                }
            }
            if (cmd.hasOption(DEBUG_OP)) {
                final String level = cmd.getOptionValue(DEBUG_OP);
                if (level != null && Tools.isNumber(level)) {
                    Tools.setDebugLevel(Integer.parseInt(level));
                } else {
                    throw new ParseException(
                                        "cannot parse debug level: " + level);
                }
            }
            boolean tightvnc = cmd.hasOption(TIGHTVNC_OP);
            boolean ultravnc = cmd.hasOption(ULTRAVNC_OP);
            final boolean realvnc = cmd.hasOption(REALVNC_OP);
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
            Tools.getConfigData().setTightvnc(tightvnc);
            Tools.getConfigData().setUltravnc(ultravnc);
            Tools.getConfigData().setRealvnc(realvnc);

            Tools.getConfigData().setUpgradeCheckEnabled(
                                          !cmd.hasOption(NO_UPGRADE_CHECK_OP));
            Tools.getConfigData().setBigDRBDConf(
                                                cmd.hasOption(BIGDRBDCONF_OP));
            Tools.getConfigData().setStagingDrbd(
                                               cmd.hasOption(STAGING_DRBD_OP));
            Tools.getConfigData().setStagingPacemaker(
                                          cmd.hasOption(STAGING_PACEMAKER_OP));
            Tools.getConfigData().setNoLRM(cmd.hasOption(NOLRM_OP));
            Tools.getConfigData().setKeepHelper(cmd.hasOption(KEEP_HELPER_OP));
            final String pwd = System.getProperty("user.home");
            final String idDsaPath = cmd.getOptionValue(ID_DSA_OP,
                                                        pwd + "/.ssh/id_dsa");
            final String idRsaPath = cmd.getOptionValue(ID_RSA_OP,
                                                        pwd + "/.ssh/id_rsa");
            final String knownHostsPath = cmd.getOptionValue(
                                                    KNOWN_HOSTS_OP,
                                                    pwd + "/.ssh/known_hosts");
            Tools.getConfigData().setIdDSAPath(idDsaPath);
            Tools.getConfigData().setIdRSAPath(idRsaPath);
            Tools.getConfigData().setKnownHostPath(knownHostsPath);


            final String opMode = cmd.getOptionValue(OP_MODE_OP);
            autoArgs = cmd.getOptionValue(AUTO_OP);
            if (cmd.hasOption(HELP_OP)) {
                final HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar DMC.jar [OPTIONS]", options);
                System.exit(0);
            }
            if (cmd.hasOption(VERSION_OP)) {
                System.out.println("MANAGEMENT CONSOLE "
                                   + Tools.getRelease()
                                   + " by Rasto Levrinc");
                System.exit(0);
            }
            if (cmd.hasOption("ro") || "ro".equals(opMode)) {
                Tools.getConfigData().setAccessType(ConfigData.AccessType.RO);
                Tools.getConfigData().setMaxAccessType(
                                                    ConfigData.AccessType.RO);
            } else if (cmd.hasOption("op") || "op".equals(opMode)) {
                Tools.getConfigData().setAccessType(ConfigData.AccessType.OP);
                Tools.getConfigData().setMaxAccessType(
                                                    ConfigData.AccessType.OP);
            } else if (cmd.hasOption("admin") || "admin".equals(opMode)) {
                Tools.getConfigData().setAccessType(
                                          ConfigData.AccessType.ADMIN);
                Tools.getConfigData().setMaxAccessType(
                                          ConfigData.AccessType.ADMIN);
            } else if (opMode != null) {
                Tools.appWarning("unknown operating mode: " + opMode);
            }
            if (cmd.hasOption(SLOW_OP)) {
                fps = fps / 2;
            }
            if (cmd.hasOption(RESTORE_MOUSE_OP)) {
                /* restore mouse if it is stuck in pressed state, during
                 * robot tests. */
                RoboTest.restoreMouse();
            }
            final String vncPortOffsetString =
                                        cmd.getOptionValue(VNC_PORT_OFFSET_OP);
            if (vncPortOffsetString != null
                && Tools.isNumber(vncPortOffsetString)) {
                Tools.getConfigData().setVncPortOffset(
                                        Integer.parseInt(vncPortOffsetString));
            }
            Tools.getConfigData().setAnimFPS(fps);
        } catch (ParseException exp) {
            System.out.println("ERROR: " + exp.getMessage());
            System.exit(1);
        }
        return autoArgs;
    }

    /** The main function for starting the application. */
    public static void main(final String[] args) {
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
            @Override public void run() {
                createAndShowGUI((Container) mainFrame);
            }
        });
        if (autoArgs != null) {
            Tools.parseAutoArgs(autoArgs);
        }
        //final Thread t = new Thread(new Runnable() {
        //    public void run() {
        //        drbd.utilities.RoboTest.startMover(600000, true);
        //    }
        //});
        //t.start();
    }
}
