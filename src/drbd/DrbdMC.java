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
import drbd.gui.ProgressIndicatorPanel;
import drbd.utilities.Tools;
import drbd.data.ConfigData;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

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
    private static void createAndShowGUI() {
        try {
            /* Metal */
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
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
        /* Create and set up the window. */
        final JFrame mainFrame = new JFrame(
               Tools.getString("DrbdMC.Title") + " " + Tools.getRelease());
        Tools.getGUIData().setMainFrame(mainFrame);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /* Create the Main Panel */
        final JPanel mainPanel = new MainPanel();
        mainPanel.setOpaque(true); //content panes must be opaque
        final MainMenu menu = new MainMenu();
        Tools.getGUIData().setMainMenu(menu);
        mainFrame.setContentPane(mainPanel);
        mainFrame.setJMenuBar(menu.getMenuBar());

        /* Display the window. */
        mainFrame.setSize(Tools.getDefaultInt("DrbdMC.width"),
                          Tools.getDefaultInt("DrbdMC.height"));
        mainFrame.addWindowListener(new ExitListener());
        mainFrame.setVisible(true);

        /* glass pane is used for progress bar etc. */
        final ProgressIndicatorPanel mainGlassPane =
                                             new ProgressIndicatorPanel();
        Tools.getGUIData().setMainGlassPane(mainGlassPane);
        mainFrame.setGlassPane(mainGlassPane);

    }

    /**
     * Adds te exit listener and disconnects all hosts prior to exiting.
     */
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

    /**
     * The main function for starting the application.
     */
    public static void main(final String[] args) {
        try {
            Tools.init();
            Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(final Thread t,
                                                  final Throwable ex) {
                        Tools.appError("uncaught exception",
                                       ex.toString(),
                                       (Exception) ex);
                    }
                });
            boolean auto = false;
            boolean tightvnc = false;
            boolean ultravnc = false;
            boolean realvnc = false;
            boolean vncportoffset = false;
            boolean opMode = false;
            float fps = 20.0f;
            for (final String arg : args) {
                if (vncportoffset && Tools.isNumber(arg)) {
                    Tools.getConfigData().setVncPortOffset(
                                                        Integer.parseInt(arg));
                }
                if ("--vnc-port-offset".equals(arg)) {
                    vncportoffset = true;
                    continue;
                } else {
                    vncportoffset = false;
                }

                if (opMode) {
                    if ("ro".equals(arg)) {
                        Tools.getConfigData().setAccessType(
                                                  ConfigData.AccessType.RO);
                    } else if ("op".equals(arg)) {
                        Tools.getConfigData().setAccessType(
                                                  ConfigData.AccessType.OP);
                    } else if ("admin".equals(arg)) {
                        Tools.getConfigData().setAccessType(
                                                  ConfigData.AccessType.ADMIN);
                    } else {
                        Tools.appWarning("unknown operating mode: " + arg);
                    }
                }
                if ("--op-mode".equals(arg)
                    || "--operating-mode".equals(arg)) {
                    opMode = true;
                    continue;
                } else {
                    opMode = false;
                }
                    
                if (auto) {
                    Tools.parseAutoArgs(arg);
                } else if ("--keep-helper".equals(arg)) {
                    Tools.debug(null, "--keep-helper option specified");
                    Tools.getConfigData().setKeepHelper(true);
                } else if ("--help".equals(arg)) {
                    System.out.println("--help print this help.");
                    System.out.println("--keep-helper do not overwrite "
                                       + "the drbd-gui-helper program.");
                    System.out.println("--auto for testing");
                    System.out.println("--tightvnc enable tight vnc viewer");
                    System.out.println("--ultravnc enable ultra vnc viewer");
                    System.out.println("--realvnc enable real vnc viewer");
                    System.out.println(
                                "--vnc-port-offset offset for port forwarding");
                    System.out.println(
                        "--slow specify this if you have slow computer. Can be"
                        + " specified more times");
                    System.exit(0);
                } else if ("--tightvnc".equals(arg)) {
                    tightvnc = true;
                } else if ("--ultravnc".equals(arg)) {
                    ultravnc = true;
                } else if ("--realvnc".equals(arg)) {
                    realvnc = true;
                } else if ("--auto".equals(arg)) {
                    auto = true;
                } else if ("--staging-drbd".equals(arg)) {
                    Tools.getConfigData().setStagingDrbd(true);
                } else if ("--slow".equals(arg)) {
                    fps = fps / 2;
                } else if ("--fast".equals(arg)) {
                    /* undocumented */
                    fps = fps * 2;
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
            Tools.getConfigData().setTightvnc(tightvnc);
            Tools.getConfigData().setUltravnc(ultravnc);
            Tools.getConfigData().setRealvnc(realvnc);
            /* Schedule a job for the event-dispatching thread:
               creating and showing this application's GUI. */
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });
        } catch (Exception e) {
            Tools.appError("Error in the application", "", e);
        }
    }
}
