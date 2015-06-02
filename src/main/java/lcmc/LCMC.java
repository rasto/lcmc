/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2011, Rastislav Levrinc
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 *
 * LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * LCMC
 * written by Rasto Levrinc
 */
package lcmc;

import java.awt.Container;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import lcmc.cluster.service.storage.FileSystemService;
import lcmc.cluster.service.storage.MountPointService;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.configs.AppDefaults;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.MainMenu;
import lcmc.common.domain.Application;
import lcmc.common.ui.MainPanel;
import lcmc.common.ui.ProgressIndicatorPanel;
import lcmc.cluster.service.storage.BlockDeviceService;
import lcmc.cluster.service.NetworkService;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;

/**
 * This is the central class with main function. It starts the LCMC GUI.
 */
@Named
@Singleton
public final class LCMC extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(LCMC.class);
    private static volatile boolean uncaughtExceptionFlag = false;

    private static final int TOOLTIP_INITIAL_DELAY_MILLIS = 200;
    private static final int TOOLTIP_DISMISS_DELAY_MILLIS = 100000;

    @Inject
    private Application application;
    @Inject
    private ArgumentParser argumentParser;
    @Inject
    private MainPanel mainPanel;
    @Inject
    private MainMenu menu;
    @Inject
    private ProgressIndicatorPanel mainGlassPane;
    @Inject
    private MainData mainData;
    @Inject
    private MainPresenter mainPresenter;
    @Inject
    private BlockDeviceService blockDeviceService;
    @Inject
    private MountPointService mountPointService;
    @Inject
    private FileSystemService fileSystemService;
    @Inject
    private NetworkService networkService;
    @Inject
    private SwingUtils swingUtils;

    protected void createAndShowGUI(final Container mainFrame) {
        setupUiManager();
        displayMainFrame(mainFrame);
    }

    protected JPanel getMainPanel() {
        mainPanel.init();
        mainPanel.setOpaque(true); //content panes must be opaque
        return mainPanel;
    }

    protected JMenuBar getMenuBar() {
        /* glass pane is used for progress bar etc. */
        menu.init();
        return menu.getMenuBar();
    }

    protected ProgressIndicatorPanel getMainGlassPane() {
        mainGlassPane.init();
        return mainGlassPane;
    }

    protected void initApp(final String[] args) {
        setupUiLookupFeelAndFeel();
        setupUncaughtExceptionHandler();
        argumentParser.parseOptionsAndReturnAutoArguments(args);
        setupServices();
    }
    public static void main(final String[] args) {
        final LCMC lcmc = AppContext.getBean(LCMC.class);
        lcmc.launch(args);
    }

    public void launch(final String[] args) {
        Tools.init();
        final JFrame mainFrame = new JFrame(Tools.getString("DrbdMC.Title") + ' ' + Tools.getRelease());
        final List<Image> il = new ArrayList<Image>();
        for (final String iconS : new String[]{"LCMC.AppIcon32",
                                               "LCMC.AppIcon48",
                                               "LCMC.AppIcon64",
                                               "LCMC.AppIcon128",
                                               "LCMC.AppIcon256"}) {
            il.add(Tools.createImageIcon(Tools.getDefault(iconS)).getImage());
        }
        mainFrame.setIconImages(il);
        initApp(args);
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                createMainFrame(mainFrame);
                createAndShowGUI(mainFrame);
            }
        });
        mainData.setMainFrame(mainFrame);
        //final Thread t = new Thread(new Runnable() {
        //    public void run() {
        //        drbd.utilities.RoboTest.startMover(600000, true);
        //    }
        //});
        //t.start();
    }

    void createMainFrame(final JFrame mainFrame) {
        mainFrame.setGlassPane(getMainGlassPane());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.addWindowListener(new ExitListener());
        mainFrame.setJMenuBar(getMenuBar());
        mainFrame.setContentPane(getMainPanel());
    }

    /** Adds the exit listener and disconnects all hosts prior to exiting. */
    public class ExitListener extends WindowAdapter {
        /**
         * Called when window is closed.
         */
        @Override
        public final void windowClosing(final WindowEvent event) {
            cleanupBeforeClosing();
            System.exit(0);
        }

    }

    /** Cleanup before closing. */
    private void cleanupBeforeClosing() {
        final Thread t = new Thread(new Runnable() {
            @Override
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
        mainData.getMainFrame().setVisible(false);
        final String saveFile = application.getDefaultSaveFile();
        mainPresenter.saveConfig(saveFile, false);
        application.disconnectAllHosts();
    }


    private void displayMainFrame(Container mainFrame) {
        mainFrame.setSize(Tools.getDefaultInt("DrbdMC.width"), Tools.getDefaultInt("DrbdMC.height"));
        mainFrame.setVisible(true);
    }

    private void setupUiManager() {
        final List<Object> buttonGradient = Arrays.asList(new Object[]{
                0.5f,
                1.0f,
                new ColorUIResource(0xFFFFFF),
                new ColorUIResource(ClusterBrowser.PANEL_BACKGROUND),
                new ColorUIResource(ClusterBrowser.BUTTON_PANEL_BACKGROUND)});
        final List<Object> checkboxGradient = Arrays.asList(new Object[]{
                0.3f,
                0.0f,
                new ColorUIResource(ClusterBrowser.PANEL_BACKGROUND),
                new ColorUIResource(ClusterBrowser.PANEL_BACKGROUND),
                new ColorUIResource(0xFFFFFF)});
        ToolTipManager.sharedInstance().setInitialDelay(TOOLTIP_INITIAL_DELAY_MILLIS);
        ToolTipManager.sharedInstance().setDismissDelay(TOOLTIP_DISMISS_DELAY_MILLIS);
        UIManager.put("TableHeader.background", Tools.getDefaultColor("DrbdMC.TableHeader"));
        UIManager.put("TableHeader.font", UIManager.getFont("Label.font"));
        UIManager.put("Button.gradient", buttonGradient);
        UIManager.put("Button.select", ClusterBrowser.PANEL_BACKGROUND);

        UIManager.put("CheckBox.gradient", checkboxGradient);
        UIManager.put("CheckBoxMenuItem.gradient", checkboxGradient);
        UIManager.put("RadioButton.gradient", checkboxGradient);
        UIManager.put("RadioButton.rollover", Boolean.TRUE);
        UIManager.put("RadioButtonMenuItem.gradient", checkboxGradient);
        UIManager.put("ScrollBar.gradient", buttonGradient);
        UIManager.put("ToggleButton.gradient", buttonGradient);

        UIManager.put("Menu.selectionBackground", ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        UIManager.put("MenuItem.selectionBackground", ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        UIManager.put("List.selectionBackground", ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        UIManager.put("ComboBox.selectionBackground", ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        UIManager.put("OptionPane.background", ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        UIManager.put("Panel.background", ClusterBrowser.PANEL_BACKGROUND);
    }


    private void setupUiLookupFeelAndFeel() {
        try {
            /* Metal */
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(LCMC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(LCMC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(LCMC.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LCMC.class.getName()).log(Level.SEVERE, null, ex);
        }
        MetalLookAndFeel.setCurrentTheme(
                new OceanTheme() {
                    /** e.g. arrows on split pane... */
                    @Override
                    protected ColorUIResource getPrimary1() {
                        return new ColorUIResource(ClusterBrowser.STATUS_BACKGROUND);
                    }

                    /** unknown to me */
                    @Override
                    protected ColorUIResource getPrimary2() {
                        return new ColorUIResource(ClusterBrowser.PANEL_BACKGROUND);
                    }

                    /** unknown to me */
                    @Override
                    protected ColorUIResource getPrimary3() {
                        return new ColorUIResource(ClusterBrowser.PANEL_BACKGROUND);
                    }

                    /** Button and other borders. */
                    @Override
                    protected ColorUIResource getSecondary1() {
                        return new ColorUIResource(AppDefaults.BACKGROUND_DARK);
                    }

                    @Override
                    protected ColorUIResource getSecondary2() {
                        return new ColorUIResource(ClusterBrowser.PANEL_BACKGROUND);
                    }

                    /** Split pane divider. Line in the main menu. */
                    @Override
                    protected ColorUIResource getSecondary3() {
                        return new ColorUIResource(ClusterBrowser.PANEL_BACKGROUND);
                    }
                }
        );
    }

    private void setupUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(final Thread t, final Throwable e) {
                        System.out.println(e);
                        System.out.println(Tools.getStackTrace(e));
                        if (!uncaughtExceptionFlag && mainData.getMainFrame() != null) {
                            uncaughtExceptionFlag = true;
                            LOG.appError("", e.toString(), e);
                        }
                    }
                });
    }

    private void setupServices() {
        blockDeviceService.init();
        mountPointService.init();
        fileSystemService.init();
        networkService.init();
    }
}
