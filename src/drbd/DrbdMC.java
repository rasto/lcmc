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
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import javax.swing.ToolTipManager;

/**
 * This is the central class with main function. It starts the DRBD GUI.
 */
public class DrbdMC extends JPanel {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;

    /** Initial delay for showing any tool tip in milliseconds. */
    private static final int TOOLTIP_INITIAL_DELAY = 500;
    /** Dismiss delay for showing any tool tip in milliseconds. */
    private static final int TOOLTIP_DISMISS_DELAY = 100000;
    /**
     * Create the GUI and show it.
     */
    private static void createAndShowGUI() {
        ToolTipManager.sharedInstance().setInitialDelay(TOOLTIP_INITIAL_DELAY);
        ToolTipManager.sharedInstance().setDismissDelay(TOOLTIP_DISMISS_DELAY);
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
        public void windowClosing(final WindowEvent event) {
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
            //if (args.length != 0 && args[0].equals("load")) {
            //    Tools.loadConfigData("drbdGui.ser");
            //}
            //Schedule a job for the event-dispatching thread:
            //creating and showing this application's GUI.
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
