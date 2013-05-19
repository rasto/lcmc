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

package lcmc.utilities;

import lcmc.data.ConfigData;
import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.data.Clusters;
import lcmc.data.UserConfig;
import lcmc.data.HostOptions;
import lcmc.configs.DistResource;
import lcmc.gui.resources.DrbdResourceInfo;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.ServiceInfo;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.GUIData;
import lcmc.gui.dialog.ConfirmDialog;
import lcmc.Exceptions;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Properties;
import java.util.Locale;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.Collection;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.LinkedHashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.text.html.HTMLDocument;
import javax.swing.UIManager;
import javax.swing.JTable;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.JDialog;
import javax.swing.AbstractButton;

import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.JApplet;
import java.awt.Cursor;
import java.awt.image.MemoryImageSource;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Desktop;
import java.awt.Container;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.URI;
import java.net.InetAddress;
import java.lang.reflect.InvocationTargetException;
import javax.swing.plaf.FontUIResource;

/**
 * This class provides tools, that are not classified.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Tools {
    /** Singleton. */
    private static Tools instance = null;
    /** Release version. */
    private static String release = null;
    /** Debug level. */
    private static int debugLevel = -1;
    /** Whether the warnings should be shown. */
    private static boolean appWarning;
    /** Whether application errors should show a dialog. */
    private static boolean appError;
    /** Map with all warnings, so that they don't appear more than once. */
    private static Set<String> appWarningHash = new HashSet<String>();
    /** Map with all errors, so that they don't appear more than once. */
    private static Set<String> appErrorHash = new HashSet<String>();
    /** Image icon cache. */
    private static Map<String, ImageIcon> imageIcons =
                                              new HashMap<String, ImageIcon>();
    /** Resource bundle. */
    private static ResourceBundle resource = null;
    /** Application defaults bundle. */
    private static ResourceBundle resourceAppDefaults = null;

    /** Config data object. */
    private static ConfigData configData;
    /** Gui data object. */
    private static GUIData guiData;
    /** Drbd gui xml object. */
    private static UserConfig userConfig = new UserConfig();
    /** String that starts error messages. */
    private static final String ERROR_STRING = "ERROR: ";
    /** String that starts info messages. */
    private static final String INFO_STRING = "INFO: ";
    /** String that starts debug messages. */
    private static final String DEBUG_STRING = "DEBUG: ";
    /** string that starts application warnings. */
    private static final String APPWARNING_STRING = "APPWARNING: ";
    /** String that starts application errors. */
    private static final String APPERROR_STRING = "APPERROR: ";
    /** Default dialog panel width. */
    private static final int DIALOG_PANEL_WIDTH = 400;
    /** Default dialog panel height. */
    private static final int DIALOG_PANEL_HEIGHT = 300;
    /** Default dialog panel size. */
    private static final Dimension DIALOG_PANEL_SIZE = new Dimension(
                                                          DIALOG_PANEL_WIDTH,
                                                          DIALOG_PANEL_HEIGHT);
    /** Previous index in the scrolling menu. */
    private static volatile int prevScrollingMenuIndex = -1;
    /** Text/html mime type. */
    public static final String MIME_TYPE_TEXT_HTML = "text/html";
    /** Text/plain mime type. */
    public static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
    /** Pattern that matches a number and unit. */
    private static final Pattern UNIT_PATTERN = Pattern.compile("(\\d*)(\\D*)");
    /** Time when the application started in seconds. */
    private static final long START_TIME = System.currentTimeMillis() / 1000;
    /** Private constructor. */
    private Tools() {
        /* no instantiation possible. */
    }

    /** This is to make this class a singleton. */
    public static Tools getInstance() {
        synchronized (Tools.class) {
            if (instance == null) {
                instance = new Tools();
            }
        }
        return instance;
    }

    /** Inits this class. */
    public static void init() {
        setDefaults();
        configData = new ConfigData();
        guiData = new GUIData();
    }

    /** Returns seconds since start. */
    private static long seconds() {
        return System.currentTimeMillis() / 1000 - START_TIME;
    }


    /**
     * Returns an ImageIcon, or null if the path was invalid.
     *
     * return image icon object.
     */
    public static ImageIcon createImageIcon(final String imageFilename) {
        final ImageIcon imageIcon = imageIcons.get(imageFilename);
        if (imageIcon != null) {
            return imageIcon;
        }
        final java.net.URL imgURL =
                        Tools.class.getResource("/images/" + imageFilename);
        if (imgURL == null) {
            Tools.appWarning("Couldn't find image: " + imageFilename);
            return null;
        } else {
            final ImageIcon newIcon = new ImageIcon(imgURL);
            imageIcons.put(imageFilename, newIcon);
            return newIcon;
        }
    }

    /** Returns the drbd gui release version. */
    public static String getRelease() {
        if (release != null) {
            return release;
        }
        final Properties p = new Properties();
        try {
            p.load(Tools.class.getResourceAsStream("/release.properties"));
            release = p.getProperty("release");
            return release;
        } catch (IOException e) {
            appError("cannot open release file", "", e);
            return "unknown";
        }
    }

    /** Prints info message to the stdout. */
    public static void info(final String msg) {
        System.out.println(INFO_STRING + msg);
    }

    /** Sets defaults from AppDefaults bundle. */
    public static void setDefaults() {
        debugLevel = getDefaultInt("DebugLevel");
        if (getDefault("AppWarning").equals("y")) {
            appWarning = true;
        }
        if (getDefault("AppError").equals("y")) {
            appError = true;
        }
    }

    /** Increments the debug level. */
    public static void incrementDebugLevel() {
        debugLevel++;
        info("debug level: " + debugLevel);
    }

    /** Decrements the debug level. */
    public static void decrementDebugLevel() {
        debugLevel--;
        info("debug level: " + debugLevel);
    }

    /**
     * Sets debug level.
     *
     * @param level
     *          debug level usually from 0 to 2. 0 means no debug output.
     */
    public static void setDebugLevel(final int level) {
        debugLevel = level;
    }

    /**
     * Prints debug message to the stdout.
     *
     * @param msg
     *          debug message
     */
    private static void debug(final String msg) {
        if (debugLevel > 0) {
            System.out.println(DEBUG_STRING
                               + "[" + seconds() + "s] "
                               + msg + " (lcmc.utilities.Tools)");
        }
    }

    /**
     * Prints debug message to the stdout. Only messages with level smaller
     * or equal than debug level will be printed.
     *
     * @param msg
     *          debug message
     *
     * @param level
     *          level of this message.
     */
    private static void debug(final String msg, final int level) {
        if (level <= debugLevel) {
            System.out.println(DEBUG_STRING
                               + "(" + level + ") "
                               + "[" + seconds() + "s] "
                               + msg + " (lcmc.utilities.Tools)");
        }
    }

    /**
     * Prints debug message to the stdout.
     *
     * @param object
     *          object from which this message originated. Use "this" by
     *          caller for this.
     * @param msg
     *          debug message
     */
    public static void debug(final Object object, final String msg) {
        if (debugLevel > -1) {
            if (object == null) {
                System.out.println(DEBUG_STRING
                                   + "[" + seconds() + "s] "
                                   + msg);
            } else {
                System.out.println(DEBUG_STRING
                                   + "[" + seconds() + "s] "
                                   + msg
                                   + " (" + object.getClass().getName() + ")");
            }
        }
    }

    /**
     * Prints debug message to the stdout. Only messages with level smaller
     * or equal than debug level will be printed.
     *
     * @param object
     *          object from which this message originated. Use "this" by
     *          caller for this.
     * @param msg
     *          debug message
     * @param level
     *          level of this message.
     */
    public static void debug(final Object object,
                             final String msg,
                             final int level) {
        if (level <= debugLevel) {
            String from = "";
            if (object != null) {
                from = " (" + object.getClass().getName() + ")";
            }
            System.out.println(DEBUG_STRING
                               + "(" + level + ") "
                               + "[" + seconds() + "s] "
                               + msg
                               + from);
        }
    }

    /**
     * Shows error message dialog and prints error to the stdout.
     *
     * @param msg
     *          error message
     */
    public static void error(final String msg) {
        System.out.println(ERROR_STRING + getErrorString(msg));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(
                            guiData.getMainFrame(),
                            new JScrollPane(new JTextArea(getErrorString(msg),
                                                          20,
                                                          60)),
                            Tools.getString("Error.Title"),
                            JOptionPane.ERROR_MESSAGE);
            }
        });

    }

    /** Show an ssh error message. */
    public static void sshError(final Host host,
                                final String command,
                                final String ans,
                                final String stacktrace,
                                final int exitCode) {
        final StringBuilder onHost = new StringBuilder("");
        if (host != null) {
            onHost.append(" on host ");
            final Cluster cluster = host.getCluster();
            if (cluster != null) {
                onHost.append(cluster.getName());
                onHost.append(" / ");
            }
            onHost.append(host.getName());
        }
        Tools.appWarning(Tools.getString("Tools.sshError.command")
                         + " '" + command + "'" + onHost.toString() + "\n"
                         + Tools.getString("Tools.sshError.returned")
                         + " " + exitCode + "\n"
                         + ans + "\n"
                         + stacktrace);
    }

    /**
     * Shows confirm dialog with yes and no options and returns true if yes
     * button was pressed.
     */
    public static boolean confirmDialog(final String title,
                                        final String desc,
                                        final String yesButton,
                                        final String noButton) {
        final ConfirmDialog cd = new ConfirmDialog(title,
                                                   desc,
                                                   yesButton,
                                                   noButton);
        cd.showDialog();
        return cd.isPressedYesButton();
    }

    /** Executes a command with progress indicator. */
    public static SSH.SSHOutput execCommandProgressIndicator(
                                           final Host host,
                                           final String command,
                                           final ExecCallback execCallback,
                                           final boolean outputVisible,
                                           final String text,
                                           final int commandTimeout) {
        ExecCallback ec;
        final String hostName = host.getName();
        Tools.startProgressIndicator(hostName, text);
        final StringBuilder output = new StringBuilder("");
        final Integer[] exitCodeHolder = new Integer[]{0};
        if (execCallback == null) {
            final String stacktrace = getStackTrace();
            ec = new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                                 output.append(ans);
                             }

                             @Override
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 Tools.appWarning(ERROR_STRING
                                                  + command
                                                  + " "
                                                  + ans + " rc: "
                                                  + exitCode);
                                 if (outputVisible) {
                                    Tools.sshError(host,
                                                   command,
                                                   ans,
                                                   stacktrace,
                                                   exitCode);
                                 }
                                 exitCodeHolder[0] = exitCode;
                                 output.append(ans);
                             }
                         };
        } else {
            ec = execCallback;
        }
        final Thread commandThread = host.execCommandRaw(command,
                                                         ec,
                                                         outputVisible,
                                                         true,
                                                         commandTimeout);


        try {
            if (commandThread != null) {
                commandThread.join(0);
            }
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Tools.stopProgressIndicator(hostName, text);
        return new SSH.SSHOutput(output.toString(), exitCodeHolder[0]);
    }

    /** Executes a command. */
    public static SSH.SSHOutput execCommand(final Host host,
                                            final String command,
                                            final ExecCallback execCallback,
                                            final boolean outputVisible,
                                            final int commandTimeout) {
        ExecCallback ec;
        final StringBuilder output = new StringBuilder("");
        final Integer[] exitCodeHolder = new Integer[]{0};
        if (execCallback == null) {
            final String stacktrace = getStackTrace();
            ec = new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                                 output.append(ans);
                             }

                             @Override
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 if (outputVisible) {
                                    Tools.sshError(host,
                                                   command,
                                                   ans,
                                                   stacktrace,
                                                   exitCode);
                                 }
                                 exitCodeHolder[0] = exitCode;
                                 output.append(ans);
                             }
                           };
        } else {
            ec = execCallback;
        }

        final Thread commandThread = host.execCommandRaw(command,
                                                         ec,
                                                         outputVisible,
                                                         true,
                                                         commandTimeout);


        try {
            if (commandThread != null) {
                commandThread.join(0);
            }
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new SSH.SSHOutput(output.toString(), exitCodeHolder[0]);
    }

    /**
     * Shows application warning message dialog if application warning messages
     * are enabled.
     *
     * @param msg
     *          warning message
     */
    public static void appWarning(final String msg) {
        if (!appWarningHash.contains(msg)) {
            appWarningHash.add(msg);
            if (appWarning) {
                System.out.println(APPWARNING_STRING + msg);
            } else {
                debug(APPWARNING_STRING + msg, 2);
            }
        }
    }

    /** Warning with exception error message. */
    public static void appWarning(final String msg, final Exception e) {
        if (!appWarningHash.contains(msg)) {
            appWarningHash.add(msg);
            if (appWarning) {
                System.out.println(APPWARNING_STRING + msg + ": "
                                   + e.getMessage());
            } else {
                debug(APPWARNING_STRING + msg, 2);
            }
        }
    }

    /**
     * Shows application error message dialog if application error messages
     * are enabled.
     *
     * @param msg
     *          error message
     */
    public static void appError(final String msg) {
        appError(msg, "", null);
    }

    /**
     * Shows application error message dialog if application error messages
     * are enabled.
     *
     * @param msg
     *          error message
     * @param msg2
     *          second error message in a new line.
     */
    public static void appError(final String msg, final String msg2) {
        appError(msg, msg2, null);
    }

    /** Shows application error message dialog, with a stacktrace. */
    public static void appError(final String msg, final Exception e) {
        appError(msg, "", e);
    }

    /**
     * Shows application error message dialog with stack trace if
     * application error messages are enabled.
     *
     * @param msg
     *          error message
     * @param msg2
     *          second error message in a new line.
     *
     * @param e
     *          Exception object.
     */
    public static void appError(final String msg,
                                final String msg2,
                                final Exception e) {
        if (appErrorHash.contains(msg + msg2)) {
            return;
        }
        appErrorHash.add(msg + msg2);
        final StringBuilder errorString = new StringBuilder(300);
        errorString.append(getErrorString(msg));
        errorString.append('\n');
        errorString.append("\nrelease: ");
        errorString.append(getRelease());
        errorString.append("\njava: ");
        errorString.append(System.getProperty("java.vendor"));
        errorString.append(' ');
        errorString.append(System.getProperty("java.version"));
        errorString.append("\n\n");
        errorString.append(msg2);
        if (e != null) {
            errorString.append('\n');
            errorString.append(e.getMessage());
            final StackTraceElement[] st = e.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                errorString.append('\n');
                errorString.append(e.getStackTrace()[i].toString());
            }
        }

        if (e == null) {
            /* stack trace */
            errorString.append('\n');
            errorString.append(getStackTrace());
        }


        System.out.println(APPERROR_STRING + errorString);
        if (!appError) {
            return;
        }

        final JEditorPane errorPane = new JEditorPane(MIME_TYPE_TEXT_PLAIN,
                                                      errorString.toString());
        errorPane.setMinimumSize(DIALOG_PANEL_SIZE);
        errorPane.setMaximumSize(DIALOG_PANEL_SIZE);
        errorPane.setPreferredSize(DIALOG_PANEL_SIZE);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(guiData.getMainFrame(),
                                              new JScrollPane(errorPane),
                                              getErrorString("AppError.Title"),
                                              JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /** Dialog that informs a user about something with ok button. */
    public static void infoDialog(final String title,
                                  final String info1,
                                  final String info2) {
        final JEditorPane infoPane = new JEditorPane(MIME_TYPE_TEXT_PLAIN,
                                                     info1 + "\n" + info2);
        infoPane.setEditable(false);
        infoPane.setMinimumSize(DIALOG_PANEL_SIZE);
        infoPane.setMaximumSize(DIALOG_PANEL_SIZE);
        infoPane.setPreferredSize(DIALOG_PANEL_SIZE);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(guiData.getMainFrame(),
                                              new JScrollPane(infoPane),
                                              getErrorString(title),
                                              JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Checks string if it is an ip.
     *
     * @param ipString
     *          Ip string to be checked.
     *
     * @return whether string is ip or not.
     */
    public static boolean isIp(final String ipString) {
        boolean wasValid = true;
        // Inet4Address ip;
        Pattern pattern = null;
        final String ipPattern =
                "([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})";
        if ("".equals(ipString)) {
            wasValid = false;
        } else {
            try {
                pattern = Pattern.compile(ipPattern);
            } catch (PatternSyntaxException exception) {
                return true;
            }
            final Matcher myMatcher = pattern.matcher(ipString);
            if (myMatcher.matches())  {
                for (int i = 1; i < 5; i++) {
                    if (Integer.parseInt(myMatcher.group(i)) > 255) {
                        wasValid = false;
                        break;
                    }
                }
            } else {
                wasValid = false;
            }
        }
        return wasValid;
    }

    /** Prints stack trace with text. */
    public static void printStackTrace(final String text) {
        System.out.println(text);
        printStackTrace();
    }

    /** Prints stack trace. */
    public static void printStackTrace() {
        System.out.println(getStackTrace());
    }

    /** Returns stack trace. */
    public static String getStackTrace() {
        final Throwable th = new Throwable();
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }


    /**
     * Loads the save file and returns its content as string. Return null, if
     * nothing was loaded.
     */
    public static String loadFile(final String filename,
                                      final boolean showError) {
        BufferedReader in = null;
        final StringBuilder content = new StringBuilder("");
        //Tools.startProgressIndicator(Tools.getString("Tools.Loading"));
        try {
            in = new BufferedReader(new FileReader(filename));
            String line = "";
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
        } catch (Exception ex) {
            if (showError) {
                infoDialog("Load Error",
                           "The file " + filename + " failed to load",
                           ex.getMessage());
            }
            return null;
        }
        if (in != null)  {
            try {
                in.close();
            } catch (IOException ex) {
                Tools.appError("Could not close: " + filename, ex);
            }
        }
        return content.toString();
    }

    /** Loads config data from the specified file. */
    public static void loadConfigData(final String filename) {
        debug("load", 0);
        final String xml = loadFile(filename, true);
        if (xml == null) {
            return;
        }
        userConfig.startClusters(null);
        Tools.getGUIData().allHostsUpdate();
    }

    /**
     * Starts the specified clusters and connects to the hosts of these
     * clusters.
     */
    public static void startClusters(final List<Cluster> selectedClusters) {
        userConfig.startClusters(selectedClusters);
    }

    /** Stops the specified clusters in the gui. */
    public static void stopClusters(final List<Cluster> selectedClusters) {
        for (final Cluster cluster : selectedClusters) {
            stopCluster(cluster);
        }
    }

    /** Stops the specified clusters in the gui. */
    public static void stopCluster(final Cluster cluster) {
        cluster.removeCluster();
        for (final Host host : cluster.getHosts()) {
            // TODO: can be run concurrently.
            host.disconnect();
        }
        getGUIData().getClustersPanel().removeTab(cluster);
    }

    /** Removes the specified clusters from the gui. */
    public static void removeClusters(final List<Cluster> selectedClusters) {
        for (final Cluster cluster : selectedClusters) {
            getConfigData().removeClusterFromClusters(cluster);
            for (final Host host : cluster.getHosts()) {
                host.setCluster(null);
            }
        }
    }

    /** Returns cluster names from the parsed save file. */
    public static void loadXML(final String xml) {
        userConfig.loadXML(xml);
    }


    /** Sets user config from command line options.
        returns host, for which dns lookup failed. */
    public static String setUserConfigFromOptions(
                               final Map<String, List<HostOptions>> clusters) {
        final Map<String, List<Host>> hostMap =
                                       new LinkedHashMap<String, List<Host>>();
        for (final String clusterName : clusters.keySet()) {
            for (final HostOptions hostOptions : clusters.get(clusterName)) {
                final String hostnameEntered = hostOptions.getHost();
                InetAddress[] addresses = null;
                try {
                    addresses = InetAddress.getAllByName(hostnameEntered);
                } catch (UnknownHostException e) {
                }
                String ip = null;
                if (addresses != null) {
                    if (addresses.length == 0) {
                        Tools.debug("lookup failed");
                        /* lookup failed */
                    } else {
                        ip = addresses[0].getHostAddress();
                    }
                }
                if (ip == null) {
                    return hostnameEntered;
                }
                userConfig.setHost(hostMap,
                                   hostOptions.getUser(),
                                   hostnameEntered,
                                   ip,
                                   hostOptions.getPort(),
                                   null,
                                   hostOptions.getSudo(),
                                   false);
            }
        }
        for (final String clusterName : clusters.keySet()) {
            final Cluster cluster = new Cluster();
            cluster.setName(clusterName);
            cluster.setSavable(false);
            Tools.getConfigData().addClusterToClusters(cluster);
            for (final HostOptions ho : clusters.get(clusterName)) {
                userConfig.setHostCluster(hostMap,
                                          cluster,
                                          ho.getHost(),
                                          !UserConfig.PROXY_HOST);
            }
        }
        return null;
    }

    /** Removes all the hosts and clusters from all the panels and data. */
    public static void removeEverything() {
        Tools.startProgressIndicator("Removing Everything");
        Tools.getConfigData().disconnectAllHosts();
        getGUIData().getClustersPanel().removeAllTabs();
        Tools.stopProgressIndicator("Removing Everything");
    }


    /**
     * Saves config data.
     *
     * @param filename
     *          filename where are the data stored.
     * @param saveAll
                whether to save clusters specified from the command line
     */
    public static void save(final String filename, final boolean saveAll) {
        debug("save");
        final String text =
            Tools.getString("Tools.Saving").replaceAll(
                                           "@FILENAME@",
                                           Matcher.quoteReplacement(filename));
        startProgressIndicator(text);
        try {
            final FileOutputStream fileOut = new FileOutputStream(filename);
            userConfig.saveXML(fileOut, saveAll);
            debug("saved: " + filename, 0);
        } catch (IOException e) {
            appError("error saving: " + filename, "", e);
        } finally {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            final Clusters clusters = Tools.getConfigData().getClusters();
            if (clusters != null) {
                for (final Cluster cluster : clusters.getClusterSet()) {
                    final ClusterBrowser cb = cluster.getBrowser();
                    if (cb != null) {
                        cb.saveGraphPositions();
                    }
                }
            }
            stopProgressIndicator(text);
        }

    }

    /**
     * Gets config data object.
     *
     * @return config data object.
     */
    public static ConfigData getConfigData() {
        return configData;
    }

    /**
     * Gets gui data object.
     *
     * @return gui data object.
     */
    public static GUIData getGUIData() {
        return guiData;
    }

    /**
     * Returns default value for option from AppDefaults resource bundle.
     *
     * @param option
     *          String that holds option.
     *
     * @return string with default value.
     */
    public static String getDefault(final String option) {
        synchronized (Tools.class) {
            if (resourceAppDefaults == null) {
                resourceAppDefaults =
                        ResourceBundle.getBundle("lcmc.configs.AppDefaults");
            }
        }
        try {
            return resourceAppDefaults.getString(option);
        } catch (Exception e) {
            appError("unresolved config resource", option, e);
            return option;
        }
    }

    /**
     * Returns default color for option from AppDefaults resource bundle.
     *
     * @param option
     *          String that holds option.
     *
     * @return default color.
     */
    public static Color getDefaultColor(final String option) {
        synchronized (Tools.class) {
            if (resourceAppDefaults == null) {
                resourceAppDefaults =
                        ResourceBundle.getBundle("lcmc.configs.AppDefaults");
            }
        }
        try {
            return (Color) resourceAppDefaults.getObject(option);
        } catch (Exception e) {
            appError("unresolved config resource", option, e);
            return Color.WHITE;
        }
    }

    /**
     * Returns default value for integer option from AppDefaults resource
     * bundle and scales it according the --scale option.
     */
    public static int getDefaultSize(final String option) {
        return getConfigData().scaled(getDefaultInt(option));
    }

    /**
     * Returns default value for integer option from AppDefaults resource
     * bundle.
     *
     * @param option
     *          String that holds option.
     *
     * @return integer with default value.
     */
    public static int getDefaultInt(final String option) {
        synchronized (Tools.class) {
            if (resourceAppDefaults == null) {
                resourceAppDefaults =
                        ResourceBundle.getBundle("lcmc.configs.AppDefaults");
            }
        }
        try {
            return (Integer) resourceAppDefaults.getObject(option);
        } catch (Exception e) {
            appError("AppError.getInt.Exception",
                     option + ": " + getDefault(option),
                     e);
            return 0;
        }
        /*
        try {
            return Integer.parseInt(getDefault(option));
        } catch (Exception e) {
            appError("AppError.getInt.Exception",
                     option + ": " + getDefault(option),
                     e);
            return 0;
        }
        */
    }

    /**
     * Returns localized string from TextResource resource bundle.
     *
     * @param text
     *          String that holds text.
     *
     * @return localized string.
     */
    public static String getString(final String text) {
        synchronized (Tools.class) {
            if (resource == null) {
                /* set locale */
                final Locale currentLocale = Locale.getDefault();
                resource =
                    ResourceBundle.getBundle("lcmc.configs.TextResource",
                                             currentLocale);
            }
        }
        try {
            return resource.getString(text);
        } catch (Exception e) {
            appError("unresolved resource: " + text);
            return text;
        }
    }

    /**
     * Returns converted error string. TODO: at the moment there is no
     * conversion.
     * TODO: ?
     */
    public static String getErrorString(final String text) {
        return text;
    }

    /** Returns string that is specific to a distribution and version. */
    public static String getDistString(final String text,
                                       String dist,
                                       String version,
                                       final String arch) {
        if (dist == null) {
            dist = "";
        }
        if (version == null) {
            version = "";
        }
        final Locale locale = new Locale(dist, version);
        debug("getDistString text: "
              + text
              + " dist: "
              + dist
              + " version: "
              + version, 2);
        final ResourceBundle resourceString =
                ResourceBundle.getBundle("lcmc.configs.DistResource", locale);
        String ret;
        try {
            ret = resourceString.getString(text + "." + arch);
        } catch (Exception e) {
            ret = null;
        }
        if (ret == null) {
            try {
                if (ret == null) {
                    ret = resourceString.getString(text);
                }
                debug("ret: " + ret, 2);
                return ret;
            } catch (Exception e) {
                return null;
            }
        }
        return ret;
    }

    /** Returns string that is specific to a distribution and version. */
    @SuppressWarnings("unchecked")
    public static List<String> getDistStrings(final String text,
                                              String dist,
                                              String version,
                                              final String arch) {
        if (dist == null) {
            dist = "";
        }
        if (version == null) {
            version = "";
        }
        final Locale locale = new Locale(dist, version);
        debug("getDistStrings text: "
              + text
              + " dist: "
              + dist
              + " version: "
              + version, 2);
        final ResourceBundle resourceString =
                ResourceBundle.getBundle("lcmc.configs.DistResource", locale);
        List<String> ret;
        try {
            ret = (List<String>) resourceString.getObject(text);
        } catch (Exception e) {
            ret = new ArrayList<String>();
        }
        return ret;
    }

    /**
     * Returns command from DistResource resource bundle for specific
     * distribution and version.
     *
     * @param text
     *          text that should be found.
     * @param dist
     *          distribution
     * @param version
     *          version of this distribution.
     *
     * @return command.
     */
    public static String getDistCommand(
                                 final String text,
                                 final String dist,
                                 final String version,
                                 final String arch,
                                 final ConvertCmdCallback convertCmdCallback,
                                 final boolean inBash) {
        if (text == null) {
            return null;
        }
        final String[] texts = text.split(";;;");
        final List<String> results =  new ArrayList<String>();
        int i = 0;
        for (final String t : texts) {
            String distString = getDistString(t, dist, version, arch);
            if (distString == null) {
                Tools.appWarning("unknown command: " + t);
                distString = t;
            }
            if (inBash && i == 0) {
                results.add(DistResource.SUDO + "bash -c \""
                            + Tools.escapeQuotes(distString, 1)
                            + "\"");
            } else {
                results.add(distString);
            }
            i++;
        }
        String ret;
        if (results.isEmpty()) {
            ret = text;
        } else {
            ret = Tools.join(";;;",
                             results.toArray(new String[results.size()]));
        }
        if (convertCmdCallback != null && ret != null) {
            ret = convertCmdCallback.convert(ret);
        }
        return ret;
    }

    /**
     * Returns service definiton from ServiceDefinitions resource bundle.
     *
     * @param service
     *          service name.
     *
     * @return service definition as array of strings.
     */
    public static String[] getServiceDefinition(final String service) {
        final ResourceBundle resourceSD =
                ResourceBundle.getBundle("lcmc.configs.ServiceDefinitions");
        try {
            return resourceSD.getStringArray(service);
        } catch (Exception e) {
            Tools.appWarning("cannot get service definition for service: "
                             + service, e);
            return new String[]{};
        }
    }

    /**
     * Converts kernelVersion as parsed from uname to a version that is used
     * in the download area on the website.
     */
    public static String getKernelDownloadDir(final String kernelVersion,
                                              final String dist,
                                              final String version,
                                              final String arch) {
        final String regexp = getDistString("kerneldir", dist, version, arch);
        if (regexp != null && kernelVersion != null) {
            final Pattern p = Pattern.compile(regexp);
            final Matcher m = p.matcher(kernelVersion);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }

    /**
     * Gets compact representation of distribution and version. Distribution
     * and version are joined with "_" and all spaces and '.' are replaced by
     * "_" as well.
     *
     * @param dist
     *          distribution
     * @param version
     *          version of this distribution
     * @return string that represents distribution and version.
     */
    public static String getDistVersionString(String dist,
                                              final String version) {
        if (dist == null) {
            dist = "";
        }
        debug("dist: " + dist + ", version: " + version, 2);
        final Locale locale = new Locale(dist, "");
        final ResourceBundle resourceCommand =
                ResourceBundle.getBundle("lcmc.configs.DistResource", locale);
        String distVersion = null;
        try {
            distVersion = resourceCommand.getString("version:" + version);
        } catch (Exception e) {
            /* with wildcard */
            final StringBuilder buf = new StringBuilder(version);
            for (int i = version.length() - 1; i >= 0; i--) {
                try {
                    distVersion = resourceCommand.getString("version:"
                                                            + buf.toString()
                                                            + "*");
                } catch (Exception e2) {
                    distVersion = null;
                }
                if (distVersion != null) {
                    break;
                }
                buf.setLength(i);
            }
            if (distVersion == null) {
                distVersion = version;
            }
        }
        debug("dist version: " + distVersion, 2);
        return distVersion;
    }
    /**
     * Joins String array into one string with specified delimiter.
     *
     * @param delim
     *          delimiter
     * @param strings
     *          array of strings
     *
     * @return
     *          joined string from array
     */
    public static String join(final String delim, final String[] strings) {
        if (strings == null || strings.length == 0) {
            return "";
        }
        if (strings.length == 1 && strings[0] == null) {
            return "";
        }
        final StringBuilder ret = new StringBuilder("");
        for (int i = 0; i < strings.length - 1; i++) {
            if (strings[i] != null) {
                ret.append(strings[i]);
                if (delim != null && strings[i + 1] != null) {
                    ret.append(delim);
                }
            }
        }
        if (strings[strings.length - 1] != null) {
            ret.append(strings[strings.length - 1]);
        }
        return ret.toString();
    }

    /** Joins String list into one string with specified delimiter. */
    public static String join(final String delim,
                              final Collection<String> strings) {
        if (strings == null) {
            return "";
        }
        return join(delim, strings.toArray(new String[strings.size()]));
    }

    /**
     * Joins String array into one string with specified delimiter.
     *
     * @param delim
     *          delimiter
     * @param strings
     *          array of strings
     * @param length
     *          length of the string array
     *
     * @return
     *          joined string from array
     */
    public static String join(final String delim,
                              final String[] strings,
                              final int length) {
        if (strings == null || strings.length == 0 || length <= 0) {
            return "";
        }
        final StringBuilder ret = new StringBuilder("");
        int i;
        for (i = 0; i < length - 1 && i < strings.length - 1; i++) {
            ret.append(strings[i]);
            if (delim != null) {
                ret.append(delim);
            }
        }
        i++;
        ret.append(strings[i - 1]);
        return ret.toString();
    }

    /** Uppercases the first character. */
    public static String ucfirst(final String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        final String f = s.substring(0, 1);
        return s.replaceFirst(".", f.toUpperCase(Locale.US));
    }

    /**
     * Converts enumeration to the string array.
     *
     * @param e
     *          enumeration
     * @return string array
     */
    public static String[] enumToStringArray(final Enumeration<String> e) {
        if (e == null) {
            return null;
        }
        final List<String> list = new ArrayList<String>();
        while (e.hasMoreElements()) {
            list.add(e.nextElement());
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * returns intersection of two string lists as List of string.
     *
     * @param setA
     *          set A
     * @param setB
     *          set B
     * @return
     *          intersection of set A and B.
     */
    public static Set<String> getIntersection(final Set<String> setA,
                                              final Set<String> setB) {
        final Set<String> resultSet = new TreeSet<String>();
        if (setB == null) {
            return setA;
        }
        if (setA == null) {
            return setB;
        }
        for (final String item : setA) {
            if (setB.contains(item)) {
                resultSet.add(item);
            }
        }
        return resultSet;
    }

    /**
     * Convert text to html.
     *
     * @param text
     *          text to convert
     *
     * @return html
     */
    public static String html(final String text) {
        if (text == null) {
            return "<html>\n</html>";
        }
        return "<html><p>" + text.replaceAll("\n", "<br>") + "\n</html>";
    }

    /**
     * Checks if object is of the string class. Returns true if object is null.
     *
     * @param o
     *          object to be checked
     * @return true if object is of string class
     */
    public static boolean isStringClass(final Object o) {
        if (o == null || o instanceof String) {
            return true;
        }
        return false;
    }

    /** Returns thrue if object is in StringInfo class. */
    public static boolean isStringInfoClass(final Object o) {
        if (o != null
            && o.getClass().getName().equals("lcmc.gui.resources.StringInfo")) {
            return true;
        }
        return false;
    }

    /** Escapes for config file. */
    public static String escapeConfig(final String value) {
        if (value == null) {
            return null;
        }
        if (value.indexOf(' ') > -1 || value.indexOf('"') > -1) {
            return "\"" + value.replaceAll("\"", "\\\\\"") + "\"";
        }
        return value;
    }

    /** Starts progress indicator with specified text. */
    public static void startProgressIndicator(final String text) {
        getGUIData().getMainGlassPane().start(text, null);
    }

    /** Starts progress indicator for host or cluster command. */
    public static void startProgressIndicator(final String name,
                                              final String text) {
        startProgressIndicator(name + ": " + text);
    }

    /** Stops progress indicator with specified text. */
    public static void stopProgressIndicator(final String text) {
        getGUIData().getMainGlassPane().stop(text);
    }

    /** Stops progress indicator for host or cluster command. */
    public static void stopProgressIndicator(final String name,
                                             final String text) {
        stopProgressIndicator(name + ": " + text);
    }

    /** Progress indicator with failure message. */
    public static void progressIndicatorFailed(final String text) {
        getGUIData().getMainGlassPane().failure(text);
    }

    /** Progress indicator with failure message for host or cluster command. */
    public static void progressIndicatorFailed(final String name,
                                               final String text) {
        progressIndicatorFailed(name + ": " + text);
    }

    /** Progress indicator with failure message that shows for n seconds. */
    public static void progressIndicatorFailed(final String text, final int n) {
        getGUIData().getMainGlassPane().failure(text, n);
    }

    /**
     * Progress indicator with failure message for host or cluster command,
     * that shows for n seconds.
     */
    public static void progressIndicatorFailed(final String name,
                                               final String text,
                                               final int n) {
        progressIndicatorFailed(name + ": " + text, n);
    }

    /** Sets fixed size for component. */
    public static void setSize(final Component c,
                               final int width,
                               final int height) {
        final Dimension d = new Dimension(width, height);
        c.setMaximumSize(new Dimension(Short.MAX_VALUE, height));
        c.setMinimumSize(d);
        c.setPreferredSize(d);
    }

    /**
     * Returns -1 if version1 is smaller that version2, 0 if version1 equals
     * version2 and 1 if version1 is bigger than version2.
     * @Throws Exceptions.IllegalVersionException
     */
    public static int compareVersions(final String version1,
                                      final String version2)
                                  throws Exceptions.IllegalVersionException {
        if (version1 == null || version2 == null) {
            throw new Exceptions.IllegalVersionException(version1, version2);
        }
        final Pattern p = Pattern.compile("(.*\\d+)rc(\\d+)$");
        final Matcher m1 = p.matcher(version1);
        String version1a;
        int rc1 = Integer.MAX_VALUE;
        if (m1.matches()) {
            version1a = m1.group(1);
            try {
                rc1 = Integer.parseInt(m1.group(2));
            } catch (java.lang.NumberFormatException e) {
                e.printStackTrace();
            }
        } else {
            version1a = version1;
        }
        int index = version1a.indexOf('-');
        if (index < 0) {
            index = version1a.indexOf('_');
        }
        if (index >= 0) {
            version1a = version1a.substring(0, index);
        }

        final Matcher m2 = p.matcher(version2);
        String version2a;
        int rc2 = Integer.MAX_VALUE;
        if (m2.matches()) {
            version2a = m2.group(1);
            try {
                rc2 = Integer.parseInt(m2.group(2));
            } catch (java.lang.NumberFormatException e) {
                e.printStackTrace();
            }
        } else {
            version2a = version2;
        }
        final String[] v1a = version1a.split("\\.");
        final String[] v2a = version2a.split("\\.");
        if (v1a.length < 1 || v2a.length < 1) {
            throw new Exceptions.IllegalVersionException(version1, version2);
        }
        int i = 0;
        while (true) {
            if (i >= v1a.length && i >= v2a.length) {
                break;
            } else if (i >= v1a.length || i >= v2a.length) {
                return 0;
            }
            int v1i = 0;
            if (i < v1a.length) {
                final String v1 = v1a[i];
                try {
                    v1i = Integer.parseInt(v1);
                } catch (java.lang.NumberFormatException e) {
                    throw new Exceptions.IllegalVersionException(version1);
                }
            }

            int v2i = 0;
            if (i < v2a.length) {
                final String v2 = v2a[i];
                try {
                    v2i = Integer.parseInt(v2);
                } catch (java.lang.NumberFormatException e) {
                    throw new Exceptions.IllegalVersionException(version2);
                }
            }

            if (v1i < v2i) {
                return -1;
            } else if (v1i > v2i) {
                return 1;
            }
            i++;
        }
        if (rc1 < rc2) {
            return -1;
        } else if (rc1 > rc2) {
            return 1;
        }
        return 0;
    }

    /** Returns number of characters 'c' in a string 's'. */
    public static int charCount(final String s, final char c) {
        if (s == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    /**
     * Creates config on all hosts with specified name in the specified
     * directory.
     *
     * @param config
     *          config content as a string
     * @param fileName
     *          file name of the config
     * @param dir
     *          directory where the config should be stored
     * @param mode
     *          mode, e.g. "0700"
     * @param makeBackup
     *          whether to make backup or not
     */
    public static void createConfigOnAllHosts(final Host[] hosts,
                                              final String config,
                                              final String fileName,
                                              final String dir,
                                              final String mode,
                                              final boolean makeBackup) {
        for (Host host : hosts) {
            host.getSSH().createConfig(config,
                                       fileName,
                                       dir,
                                       mode,
                                       makeBackup,
                                       null,
                                       null);
        }
    }

    /** Returns border with title. */
    public static TitledBorder getBorder(final String text) {
        final TitledBorder titledBorder = new TitledBorder(
                BorderFactory.createLineBorder(Color.BLACK, 1), text);
        titledBorder.setTitleJustification(TitledBorder.LEFT);
        return titledBorder;
    }

    /** Returns a popup in a scrolling pane. */
    public static boolean getScrollingMenu(
                        final String name,
                        final JPanel optionsPanel,
                        final MyMenu menu,
                        final MyListModel<MyMenuItem> dlm,
                        final MyList<MyMenuItem> list,
                        final Info infoObject,
                        final List<JDialog> popups,
                        final Map<MyMenuItem, ButtonCallback> callbackHash) {
        final int maxSize = dlm.getSize();
        if (maxSize <= 0) {
            return false;
        }
        prevScrollingMenuIndex = -1;
        list.setFixedCellHeight(25);
        if (maxSize > 10) {
            list.setVisibleRowCount(10);
        } else {
            list.setVisibleRowCount(maxSize);
        }
        final JScrollPane sp = new JScrollPane(list);
        sp.setViewportBorder(null);
        sp.setBorder(null);
        final JTextField typeToSearchField = dlm.getFilterField();
        final Container mainFrame = Tools.getGUIData().getMainFrame();
        final JDialog popup;
        if (mainFrame instanceof JApplet) {
            popup = new JDialog(new JFrame(), name, false);
        } else {
            popup = new JDialog((JFrame) mainFrame, name, false);
        }
        popup.setUndecorated(true);
        popup.setAlwaysOnTop(true);
        final JPanel popupPanel = new JPanel();
        popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.PAGE_AXIS));
        if (maxSize > 10) {
            popupPanel.add(typeToSearchField);
        }
        popupPanel.add(sp);
        if (optionsPanel != null) {
            popupPanel.add(optionsPanel);
        }
        popup.setContentPane(popupPanel);
        popups.add(popup);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(final MouseEvent evt) {
                prevScrollingMenuIndex = -1;
                if (callbackHash != null) {
                    for (final MyMenuItem item : callbackHash.keySet()) {
                        callbackHash.get(item).mouseOut();
                        list.clearSelection();
                    }
                }
            }
            @Override
            public void mouseEntered(final MouseEvent evt) {
                /* request focus here causes the applet making all
                   textfields to be not editable. */
                list.requestFocus();
            }

            @Override
            public void mousePressed(final MouseEvent evt) {
                prevScrollingMenuIndex = -1;
                if (callbackHash != null) {
                    for (final MyMenuItem item : callbackHash.keySet()) {
                        callbackHash.get(item).mouseOut();
                    }
                }
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final int index = list.locationToIndex(evt.getPoint());
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                list.setSelectedIndex(index);
                                //TODO: some submenus stay visible, during
                                //ptest, but this breaks group popup menu
                                //setMenuVisible(menu, false);
                                menu.setSelected(false);
                            }
                        });
                        final MyMenuItem item = dlm.getElementAt(index);
                        item.action();
                    }
                });
                thread.start();
            }
        });

        list.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent evt) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int pIndex = list.locationToIndex(evt.getPoint());
                        final Rectangle r = list.getCellBounds(pIndex, pIndex);
                        if (r == null) {
                            return;
                        }
                        if (!r.contains(evt.getPoint())) {
                            pIndex = -1;
                        }
                        final int index = pIndex;
                        final int lastIndex = prevScrollingMenuIndex;
                        if (index == lastIndex) {
                            return;
                        }
                        prevScrollingMenuIndex = index;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                list.setSelectedIndex(index);
                            }
                        });
                        if (callbackHash != null) {
                            if (lastIndex >= 0) {
                                final MyMenuItem lastItem =
                                                   dlm.getElementAt(lastIndex);
                                final ButtonCallback bc =
                                                    callbackHash.get(lastItem);
                                if (bc != null) {
                                    bc.mouseOut();
                                }
                            }
                            if (index >= 0) {
                                final MyMenuItem item = dlm.getElementAt(index);
                                final ButtonCallback bc =
                                                        callbackHash.get(item);
                                if (bc != null) {
                                    bc.mouseOver();
                                }
                            }
                        }
                    }
                });
                thread.start();
            }
        });
        list.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent e) {
                final int ch = e.getKeyCode();
                if (ch == KeyEvent.VK_UP && list.getSelectedIndex() == 0) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            typeToSearchField.requestFocus();
                        }
                    });
                } else if (ch == KeyEvent.VK_ESCAPE) {
                    for (final JDialog otherP : popups) {
                        otherP.dispose();
                    }
                    infoObject.hidePopup();
                } else if (ch == KeyEvent.VK_SPACE || ch == KeyEvent.VK_ENTER) {
                    final MyMenuItem item = list.getSelectedValue();
                    //SwingUtilities.invokeLater(new Runnable() {
                    //    @Override
                    //    public void run() {
                    //        //menu.setPopupMenuVisible(false);
                    //        setMenuVisible(menu, false);
                    //    }
                    //});
                    if (item != null) {
                        item.action();
                    }
                }
            }
            @Override
            public void keyReleased(final KeyEvent e) {
            }
            @Override
            public void keyTyped(final KeyEvent e) {
            }
        });
        popup.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(final WindowEvent e) {
            }
            @Override
            public void windowLostFocus(final WindowEvent e) {
                popup.dispose();
            }
        });

        typeToSearchField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent e) {
                final int ch = e.getKeyCode();
                if (ch == KeyEvent.VK_DOWN) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            list.requestFocus();
                            /* don't need to press down arrow twice */
                            list.setSelectedIndex(0);
                        }
                    });
                } else if (ch == KeyEvent.VK_ESCAPE) {
                    for (final JDialog otherP : popups) {
                        otherP.dispose();
                    }
                    infoObject.hidePopup();
                } else if (ch == KeyEvent.VK_SPACE || ch == KeyEvent.VK_ENTER) {
                    final MyMenuItem item = list.getModel().getElementAt(0);
                    if (item != null) {
                        item.action();
                    }
                }
            }
            @Override
            public void keyReleased(final KeyEvent e) {
            }
            @Override
            public void keyTyped(final KeyEvent e) {
            }
        });

        /* menu is not new. */
        for (final MenuListener ml : menu.getMenuListeners()) {
            menu.removeMenuListener(ml);
        }
        menu.addMenuListener(new MenuListener() {
            public void menuCanceled(final MenuEvent e) {
            }

            public void menuDeselected(final MenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        boolean pVisible = false;
                        JPopupMenu p = (JPopupMenu) menu.getParent();
                        while (p != null) {
                            if (p.isVisible()) {
                                pVisible = true;
                                break;
                            }
                            p = (JPopupMenu) p.getParent();
                        }
                        for (final JDialog otherP : popups) {
                            if (popup != otherP || pVisible) {
                                /* don't dispose the popup if it was clicked.
                                 */
                                otherP.dispose();
                            }
                        }
                    }
                });
            }

            public void menuSelected(final MenuEvent e) {
                final Point l = menu.getLocationOnScreen();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (final JDialog otherP : popups) {
                            otherP.dispose();
                        }
                        popup.setLocation(
                           (int) (l.getX() + menu.getBounds().getWidth()),
                           (int) l.getY() - 1);
                        popup.pack();
                        popup.setVisible(true);
                        typeToSearchField.requestFocus();
                    }
                });
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        /* Setting location again. Moving it one pixel fixes
                           the "gray window" problem. */
                        popup.setLocation(
                           (int) (l.getX() + menu.getBounds().getWidth()),
                           (int) l.getY());
                    }
                });
            }
        });
        return true;
    }

    /** Returns whether the computer, where this program is run, is Linux. */
    public static boolean isLinux() {
        return "Linux".equals(System.getProperty("os.name"));
    }

    /** Returns whether the computer, where this program is run, is Windows. */
    public static boolean isWindows() {
        return System.getProperty("os.name").indexOf("Windows") == 0;
    }

    /** Sets the html font of the editor pane to be the default font. */
    public static void setEditorFont(final JEditorPane ep) {
        final Font font = UIManager.getFont("Label.font");
        final String bodyRule = "body { font-family: " + font.getFamily()
                                + "; "
                                + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) ep.getDocument()).getStyleSheet().addRule(bodyRule);
    }

    /** Reads and returns a content of a text file. */
    public static String getFile(final String fileName) {
        if (fileName == null) {
            return null;
        }
        final URL url = Tools.class.getResource(fileName);
        if (url == null) {
            return null;
        }
        try {
            final BufferedReader br =
                        new BufferedReader(
                            new InputStreamReader(url.openStream()));
            final StringBuilder content = new StringBuilder("");
            while (br.ready()) {
                content.append(br.readLine());
                content.append('\n');
            }
            return content.toString();
        } catch (IOException e) {
            Tools.appError("could not read: " + fileName, "", e);
            return null;
        }
    }

    /**
     * Parses arguments from --auto command line option, it makes some
     * automatical gui actions, that help to test the gui and can find some
     * other uses later.
     * To find out which options are available, you'd have to grep for
     * getAutoOptionHost and getAutoOptionCluster
     */
    public static void parseAutoArgs(final String line) {
        if (line == null) {
            return;
        }
        final String[] args = line.split(",");
        String host = null;
        String cluster = null;
        boolean global = false;
        for (final String arg : args) {
            final String[] pair = arg.split(":");
            if (pair == null || pair.length != 2) {
                appWarning("cannot parse: " + line);
                return;
            }
            final String option = pair[0];
            final String value = pair[1];
            if ("host".equals(option)) {
                cluster = null;
                host = value;
                Tools.getConfigData().addAutoHost(host);
                continue;
            } else if ("cluster".equals(option)) {
                host = null;
                cluster = value;
                Tools.getConfigData().addAutoCluster(cluster);
                continue;
            } else if ("global".equals(option)) {
                host = null;
                cluster = null;
                global = true;
                continue;
            }
            if (host != null) {
                Tools.getConfigData().addAutoOption(host, option, value);
            } else if (cluster != null) {
                Tools.getConfigData().addAutoOption(cluster, option, value);
            } else if (global) {
                Tools.getConfigData().addAutoOption("global", option, value);
            } else {
                appWarning("cannot parse: " + line);
                return;
            }
        }
    }

    /** Convenience sleep wrapper. */
    public static void sleep(final int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /** Convenience sleep wrapper with float argument. */
    public static void sleep(final float ms) {
        sleep((int) ms);
    }

    /** Returns the latest version of this application. */
    public static String getLatestVersion() {
        String version = null;
        //final Pattern vp = Pattern.compile(
        //                      ".*<a\\s+href=\"drbd-mc-([0-9.]*?)\\.tar\\..*");
        final Pattern vp = Pattern.compile(".*version:\\s+([0-9.]*)");
        try {
            //final String url = "http://oss.linbit.com/drbd-mc/?drbd-mc-check-"
            //                   + getRelease();
            final String url =
                "http://lcmc.sourceforge.net/version.html?lcmc-check-"
                + getRelease();
            final BufferedReader reader = new BufferedReader(
                             new InputStreamReader(new URL(url).openStream()));
            String line;
            do {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                final Matcher m = vp.matcher(line);
                if (m.matches()) {
                    final String v = m.group(1);
                    try {
                        if (version == null
                            || compareVersions(v, version) > 0) {
                            version = v;
                        }
                    } catch (Exceptions.IllegalVersionException e) {
                        Tools.appWarning(e.getMessage(), e);
                    }
                }
            } while (true);
        } catch (MalformedURLException mue) {
            return null;
        } catch (IOException ioe) {
            return version;
        }
        return version;
    }

    /** Opens default browser. */
    public static void openBrowser(final String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (final IOException e) {
            Tools.appError("wrong uri", e);
        } catch (final URISyntaxException e) {
            Tools.appError("error opening browser", e);
        }
    }

    /**
     * Prepares vnc viewer, gets the port and creates ssh tunnel. Returns true
     * if ssh tunnel was created.
     */
    private static int prepareVncViewer(final Host host,
                                        final int remotePort) {
        if (remotePort < 0 || host == null) {
            return -1;
        }
        if (Tools.isLocalIp(host.getIp())) {
            return remotePort;
        }
        final int localPort = remotePort + getConfigData().getVncPortOffset();
        debug("start port forwarding " + remotePort + " -> " + localPort);
        try {
            host.getSSH().startVncPortForwarding(host.getIp(), remotePort);
        } catch (final java.io.IOException e) {
            Tools.appError("unable to create tunnel", e);
            return -1;
        }
        return localPort;
    }

    /** Cleans up after vnc viewer. It stops ssh tunnel. */
    private static void cleanupVncViewer(final Host host,
                                         final int localPort) {
        if (Tools.isLocalIp(host.getIp())) {
            return;
        }
        final int remotePort = localPort - getConfigData().getVncPortOffset();
        debug("stop port forwarding " + remotePort);
        try {
            host.getSSH().stopVncPortForwarding(remotePort);
        } catch (final java.io.IOException e) {
            Tools.appError("unable to close tunnel", e);
        }
    }

    /** Starts Tight VNC viewer. */
    public static void startTightVncViewer(final Host host,
                                           final int remotePort) {
        final int localPort = prepareVncViewer(host, remotePort);
        if (localPort < 0) {
            return;
        }
        final tightvnc.VncViewer v = new tightvnc.VncViewer(
                                     new String[]{"HOST",
                                                  "127.0.0.1",
                                                  "PORT",
                                                  Integer.toString(localPort)},
                                     false,
                                     true);
        v.init();
        v.start();
        v.join();
        cleanupVncViewer(host, localPort);
    }

    /** Starts Ultra VNC viewer. */
    public static void startUltraVncViewer(final Host host,
                                           final int remotePort) {
        final int localPort = prepareVncViewer(host, remotePort);
        if (localPort < 0) {
            return;
        }
        final JavaViewer.VncViewer v = new JavaViewer.VncViewer(
                                     new String[]{"HOST",
                                                  "127.0.0.1",
                                                  "PORT",
                                                  Integer.toString(localPort)},
                                     false,
                                     true);

        v.init();
        v.start();
        v.join();
        cleanupVncViewer(host, localPort);
    }

    /** Starts Real VNC viewer. */
    public static void startRealVncViewer(final Host host,
                                          final int remotePort) {
        final int localPort = prepareVncViewer(host, remotePort);
        if (localPort < 0) {
            return;
        }
        final vncviewer.VNCViewer v = new vncviewer.VNCViewer(
                              new String[]{"127.0.0.1:"
                                  + (Integer.toString(localPort - 5900))});

        v.start();
        v.join();
        cleanupVncViewer(host, localPort);
    }

    /** Hides mouse pointer. */
    public static void hideMousePointer(final Component c) {
        final int[] pixels = new int[16 * 16];
        final Image image = Toolkit.getDefaultToolkit().createImage(
                                 new MemoryImageSource(16, 16, pixels, 0, 16));
        final Cursor transparentCursor =
             Toolkit.getDefaultToolkit().createCustomCursor(image,
                                                            new Point(0, 0),
                                                            "invisibleCursor");
        c.setCursor(transparentCursor);
    }

    /** Check whether the string is number. */
    public static boolean isNumber(final String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (final NumberFormatException nfe) {
            return false;
        }
    }

    /** Returns list that is expandable by shell. {'a','b'...} */
    public static String shellList(final String[] items) {
        final StringBuilder list = new StringBuilder("");
        if (items == null || items.length == 0) {
            return null;
        } else if (items.length == 1) {
            list.append(items[0]);
        } else {
            list.append('{');
            for (int i = 0; i < items.length - 1; i++) {
                list.append('\'');
                list.append(items[i]);
                list.append("',");
            }
            if (items.length != 0) {
                list.append('\'');
                list.append(items[items.length - 1]);
                list.append('\'');
            }
            list.append('}');
        }
        return list.toString();
    }

    /**
     * Returns whether two objects are equal. Special handling for Units and
     * StringInfo objects.
     */
    public static boolean areEqual(final Object o1, final Object o2) {
        if (o1 == null && o2 == null) {
            return true;
        } else if (o1 != null && o1 instanceof DrbdResourceInfo) {
            /* this is special case, because this object represents devices in
             * filesystem ra and also after field in drbd.conf. */
            final String device = ((DrbdResourceInfo) o1).getInternalValue();
            if (device != null && device.equals(o2)) {
                return true;
            }
            final String res = ((DrbdResourceInfo) o1).getName();
            if (res == null) {
                return res == o2;
            }
            if (res.equals(o2)) {
                return true;
            }
            return false;
        } else if (o1 != null && o1 instanceof Info) {
            final String s1 = ((Info) o1).getInternalValue();
            if (s1 == null) {
                return o2 == null;
            }
            if (o2 == null) {
                return false;
            }
            if (o2 instanceof Info) {
                return s1.equals(((Info) o2).getInternalValue());
            } else {
                return s1.equals(o2) || o1.toString().equals(o2);
            }
        } else if (o2 != null && o2 instanceof Info) {
            final String s2 = ((Info) o2).getInternalValue();
            if (s2 == null) {
                return o1 == null;
            }
            if (o1 == null) {
                return false;
            }
            if (o1 instanceof Info) {
                return s2.equals(((Info) o1).getInternalValue());
            } else {
                return s2.equals(o1) || o2.toString().equals(o1);
            }
        } else if (o1 == null && o2 != null) {
            return o2.toString().equals("");
        } else if (o2 == null && o1 != null) {
            return o1.toString().equals("");
        } else if (o1 instanceof Object[]
                   && o2 instanceof Object[]) {
            final Object[] array1 = (Object[]) o1;
            final Object[] array2 = (Object[]) o2;
            for (int i = 0; i < array1.length; i++) {
                if (!areEqual(array1[i], array2[i])) {
                    return false;
                }
            }
            return true;
        } else if (o1 instanceof Unit) {
            return ((Unit) o1).equals(o2);
        } else if (o2 instanceof Unit) {
            return ((Unit) o2).equals(o1);
        } else if (o1 instanceof ComboInfo) {
            return ((ComboInfo) o1).equals(o2);
        } else if (o2 instanceof ComboInfo) {
            return ((ComboInfo) o2).equals(o1);
        } else {
            return o1.equals(o2);
        }
    }

    /**
     * Returns value unit pair extracting from string. E.g. "10min" becomes 10
     * and "min" pair.
     */
    public static Object[] extractUnit(final String time) {
        final Object[] o = new Object[]{null, null};
        if (time == null) {
            return o;
        }
        final Matcher m = UNIT_PATTERN.matcher(time);
        if (m.matches()) {
            o[0] = m.group(1);
            o[1] = m.group(2);
        }
        return o;
    }

    /** Returns random secret of the specified lenght. */
    public static String getRandomSecret(final int len) {
        final Random rand = new Random();
        final ArrayList<Character> charsL = new ArrayList<Character>();
        for (int a = 'a'; a <= 'z'; a++) {
            charsL.add((char) a);
            charsL.add(Character.toUpperCase((char) a));
        }
        for (int a = '0'; a <= '9'; a++) {
            charsL.add((char) a);
        }

        final Character[] chars = charsL.toArray(new Character[charsL.size()]);
        final StringBuilder s = new StringBuilder(len + 1);
        for (int i = 0; i < len; i++) {
            s.append(chars[rand.nextInt(chars.length)]);
        }
        return s.toString();
    }

    /** Returns whether the ip is localhost. */
    public static boolean isLocalIp(final String ip) {
        if (ip == null
            || "127.0.0.1".equals(ip)
            || "127.0.1.1".equals(ip)) {
            return true;
        }
        try {
            final String localIp = InetAddress.getLocalHost().getHostAddress();
            return ip.equals(localIp);
        } catch (java.net.UnknownHostException e) {
            return false;
        }
    }

    /** Converts value in kilobytes. */
    public static String convertKilobytes(final String kb) {
        if (!isNumber(kb)) {
            return kb;
        }
        final double k = Long.parseLong(kb);
        if (k == 0) {
            return "0K";
        }
        if (k / 1024 != (long) (k / 1024)) {
            return kb + "K";
        }
        final double m = k / 1024;
        if (m / 1024 != (long) (m / 1024)) {
            return Long.toString((long) m) + "M";
        }
        final double g = m / 1024;
        if (g / 1024 != (long) (g / 1024)) {
            return Long.toString((long) g) + "G";
        }
        final double t = g / 1024;
        if (t / 1024 != (long) (t / 1024)) {
            return Long.toString((long) t) + "T";
        }
        return Long.toString((long) (t / 1024)) + "P";
    }

    /** Converts value with unit to kilobites. */
    public static long convertToKilobytes(final String value) {
        final Object[] v = Tools.extractUnit(value);
        if (v.length == 2 && Tools.isNumber((String) v[0])) {
            long num = Long.parseLong((String) v[0]);
            final String unit = (String) v[1];
            if ("P".equalsIgnoreCase(unit)) {
                num = num * 1024 * 1024 * 1024 * 1024;
            } else if ("T".equalsIgnoreCase(unit)) {
                num = num * 1024 * 1024 * 1024;
            } else if ("G".equalsIgnoreCase(unit)) {
                num = num * 1024 * 1024;
            } else if ("M".equalsIgnoreCase(unit)) {
                num = num * 1024;
            } else if ("K".equalsIgnoreCase(unit)) {
            } else {
                return -1;
            }
            return num;
        }
        return -1;
    }

    /** Converts value with units. */
    public static long convertUnits(final String value) {
        final Object[] v = Tools.extractUnit(value);
        if (v.length == 2 && Tools.isNumber((String) v[0])) {
            long num = Long.parseLong((String) v[0]);
            final String unit = (String) v[1];
            if ("P".equalsIgnoreCase(unit)) {
                num = num * 1024 * 1024 * 1024 * 1024 * 1024;
            } else if ("T".equalsIgnoreCase(unit)) {
                num = num * 1024 * 1024 * 1024 * 1024;
            } else if ("G".equalsIgnoreCase(unit)) {
                num = num * 1024 * 1024 * 1024;
            } else if ("M".equalsIgnoreCase(unit)) {
                num = num * 1024 * 1024;
            } else if ("K".equalsIgnoreCase(unit)) {
                num = num * 1024;
            } else if ("".equalsIgnoreCase(unit)) {
            } else {
                return -1;
            }
            return num;
        }
        return -1;
    }

    /** Resize table. */
    public static void resizeTable(final JTable table,
                                   final Map<Integer, Integer> defaultWidths) {
        final int margin = 3;
        if (table == null) {
            return;
        }

        for (int i = 0; i < table.getColumnCount(); i++) {
            final int vColIndex = i;
            final DefaultTableColumnModel colModel =
                            (DefaultTableColumnModel) table.getColumnModel();
            final TableColumn col = colModel.getColumn(vColIndex);
            int width = 0;
            TableCellRenderer renderer = col.getHeaderRenderer();

            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            }
            if (renderer == null) {
                continue;
            }
            Component comp = renderer.getTableCellRendererComponent(
                                                        table,
                                                        col.getHeaderValue(),
                                                        false,
                                                        false,
                                                        0,
                                                        0);
            Integer dw = null;
            if (defaultWidths != null) {
                dw = defaultWidths.get(i);
            }
            if (dw == null) {
                width = comp.getPreferredSize().width;
                for (int r = 0; r < table.getRowCount(); r++) {
                    renderer = table.getCellRenderer(r, vColIndex);
                    if (renderer == null) {
                        continue;
                    }
                    comp = renderer.getTableCellRendererComponent(
                                              table,
                                              table.getValueAt(r, vColIndex),
                                              false,
                                              false,
                            r, vColIndex);
                    width = Math.max(width, comp.getPreferredSize().width);
                }
            } else {
                width = dw;
                col.setMaxWidth(width);
            }
            width += 2 * margin;
            col.setPreferredWidth(width);
        }
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer())
                            .setHorizontalAlignment(SwingConstants.CENTER);
    }

    /** Sets the menu and all its parents visible, not visible. */
    public static void setMenuVisible(final JComponent menu,
                                      final boolean visible) {
        JComponent parent = (JComponent) menu.getParent();
        if (parent instanceof JViewport) {
            /* MyList */
            parent = (JComponent) parent.getParent();
            parent = (JComponent) parent.getParent();
        }
        if (parent instanceof JPopupMenu) {
            JComponent inv = (JComponent) ((JPopupMenu) parent).getInvoker();
            while (inv != null) {
                final JComponent invP = (JComponent) inv.getParent();
                if (!(invP instanceof JPopupMenu)) {
                    break;
                }
                invP.setVisible(visible);
                for (final Component c : invP.getComponents()) {
                    ((JComponent) c).setVisible(visible);
                }
                final JComponent pp = (JComponent) invP.getParent();
                if (pp != null) {
                    pp.setVisible(visible);
                }
                inv = (JComponent) ((JPopupMenu) invP).getInvoker();
            }
            menu.setVisible(visible);
            parent.setVisible(visible);
            final JComponent pp = (JComponent) parent.getParent();
            if (pp != null) {
                pp.setVisible(visible);
            }
            for (final Component c : parent.getComponents()) {
                ((JComponent) c).setVisible(visible);
            }
            parent.repaint();
        }
    }

    /** Sets background color to be opaque or semi-transparent. */
    private static void setBGColor(final JComponent c,
                                   final boolean opaque) {
        final Color oc = c.getBackground();
        c.setBackground(new Color(oc.getRed(),
                                  oc.getGreen(),
                                  oc.getBlue(),
                                  opaque ? 255 : 120));
    }

    /** Sets the menu and all its parents opaque, transparent. */
    public static void setMenuOpaque(final JComponent menu,
                                     final boolean opaque) {
        JComponent parent = (JComponent) menu.getParent();
        if (parent instanceof JViewport) {
            /* MyList */
            parent = (JComponent) parent.getParent();
            parent = (JComponent) parent.getParent();
        }
        if (parent instanceof JPopupMenu) {
            JComponent inv = (JComponent) ((JPopupMenu) parent).getInvoker();
            while (inv != null) {
                final JComponent invP = (JComponent) inv.getParent();
                if (!(invP instanceof JPopupMenu)) {
                    break;
                }
                setBGColor(invP, opaque);
                for (final Component c : invP.getComponents()) {
                    setBGColor((JComponent) c, opaque);
                }
                final JComponent pp = (JComponent) invP.getParent();
                if (pp != null) {
                    setBGColor(pp, opaque);
                }
                inv = (JComponent) ((JPopupMenu) invP).getInvoker();
            }
            setBGColor(menu, opaque);
            setBGColor(parent, opaque);
            final JComponent pp = (JComponent) parent.getParent();
            if (pp != null) {
                setBGColor(pp, opaque);
            }
            for (final Component c : parent.getComponents()) {
                setBGColor((JComponent) c, opaque);
            }
            parent.repaint();
        }
    }

    /** Converts windows path to unix path. */
    public static String getUnixPath(final String dir) {
        if (dir == null) {
            return null;
        }
        String unixPath;
        if (isWindows()) {
            unixPath = dir.replaceAll("\\\\", "/");
            if (unixPath.length() >= 2
                && ":".equalsIgnoreCase(unixPath.substring(1, 2))) {
                unixPath = unixPath.substring(2);
            }
        } else {
            unixPath = dir;
        }
        return unixPath;
    }

    /** Returns bounds of the whole screen. */
    public static Rectangle getScreenBounds(final JComponent component) {
        final GraphicsConfiguration gc = component.getGraphicsConfiguration();
        final Rectangle sBounds = gc.getBounds();
        final Insets screenInsets =
                                Toolkit.getDefaultToolkit().getScreenInsets(gc);
        /* Take into account screen insets, decrease viewport */
        sBounds.x += screenInsets.left;
        sBounds.y += screenInsets.top;
        sBounds.width -= (screenInsets.left + screenInsets.right);
        sBounds.height -= (screenInsets.top + screenInsets.bottom);
        return sBounds;
    }

    /** Compares two Lists with services if thery are equal. The order does not
     * matter. */
    public static boolean serviceInfoListEquals(final Set<ServiceInfo> l1,
                                                final Set<ServiceInfo> l2) {
        if (l1 == l2) {
            return true;
        }
        if (l1 == null || l2 == null) {
            return false;
        }
        if (l1.size() != l2.size()) {
            return false;
        }
        for (final ServiceInfo s1 : l1) {
            if (!l2.contains(s1)) {
                return false;
            }
        }
        return true;
    }

    /** Trims text to have displayable width. */
    public static String trimText(final String text) {
        final int width = 80;
        if (text == null || text.length() <= width) {
            return text;
        }
        final StringBuilder out = new StringBuilder(text.length() + 10);
        /* find next space */
        String t = text;
        while (true) {
            final int pos = t.indexOf(' ', width);
            if (pos > 0) {
                out.append(t.substring(0, pos));
                out.append('\n');
                t = t.substring(pos + 1);
            } else {
                break;
            }
        }
        out.append(t);
        return out.toString();
    }

    /** Wait for next swing threads to finish. It's used for synchronization */
    public static void waitForSwing() {
        invokeAndWait(new Runnable() {
                          @Override
                          public void run() {
                              /* just wait */
                          }
                      });
    }

    /** Convenience invoke and wait function. */
    public static void invokeAndWait(final Runnable runnable) {
        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (final InterruptedException ix) {
            Thread.currentThread().interrupt();
        } catch (final InvocationTargetException x) {
            Tools.printStackTrace();
        }
    }

    /** Return directory part (to the /) of the filename. */
    public static String getDirectoryPart(final String filename) {
        if (filename == null) {
            return null;
        }
        final int i = filename.lastIndexOf('/');
        if (i < 0) {
            return filename;
        }
        return filename.substring(0, i + 1);
    }

    /** Escapes the quotes for the stacked ssh commands. */
    public static String escapeQuotes(final String s, final int count) {
        if (s == null) {
            return null;
        }
        if (count <= 0) {
            return s;
        }
        final StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"' || c == '$' || c == '`') {
                sb.append('\\');
                sb.append(c);
            } else if (c == '\n') {
                sb.append("\n");
            } else {
                sb.append(c);
            }
        }
        return escapeQuotes(sb.toString(), count - 1);
    }

    /** Escapes the single quotes. */
    public static String escapeSingleQuotes(final String s, final int count) {
        if (s == null) {
            return null;
        }
        if (count <= 0) {
            return s;
        }
        final StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '\n') {
                sb.append("\n");
            } else if (c == '\'') {
                sb.append("'\\''");
            } else {
                sb.append(c);
            }
        }
        return escapeQuotes(sb.toString(), count - 1);
    }

    /** Returns array of host checkboxes in the specified cluster. */
    public static Map<Host, JCheckBox> getHostCheckBoxes(
                                                       final Cluster cluster) {
        final Map<Host, JCheckBox> components =
                                        new LinkedHashMap<Host, JCheckBox>();
        for (final Host host : cluster.getHosts()) {
            final JCheckBox button = new JCheckBox(host.getName());
            button.setBackground(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));
            components.put(host, button);
        }
        return components;
    }

    /**
     * Returns true if the hb version on the host is smaller or equal
     * Heartbeat 2.1.4.
     */
    public static boolean versionBeforePacemaker(final Host host) {
        final String hbV = host.getHeartbeatVersion();
        final String pcmkV = host.getPacemakerVersion();
        try {
            return pcmkV == null
                   && hbV != null
                   && Tools.compareVersions(hbV, "2.99.0") < 0;
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
            return false;
        }
    }

    /** Makes the buttons font smaller. */
    public static void makeMiniButton(final AbstractButton ab) {
        final Font font = ab.getFont();
        final String name = font.getFontName();
        final int style = font.getStyle();
        final int size = font.getSize();
        ab.setFont(new Font(name, style, getConfigData().scaled(10)));
        ab.setMargin(new Insets(2, 2, 2, 2));
        ab.setIconTextGap(0);
    }

    /** Trim the white space (' ', '\n') at the end of the string buffer.*/
    public static void chomp(final StringBuffer sb) {
        final int l = sb.length();
        int i = l;

        while (i > 0 && (sb.charAt(i - 1) == '\n')) {
            i--;
        }
        if (i >= 0 && i < l - 1) {
            sb.delete(i, l - 1);
        }
    }

    /**
     * Resize all fonts. Must be called before GUI is started.
     * @param scale in percent 100% - is the same size.
     */
    public static void resizeFonts(int scale) {
        if (scale == 100) {
            return;
        }
        if (scale < 5) {
            scale = 5;
        }
        if (scale > 10000) {
            scale = 10000;
        }
        for (final Enumeration<Object> e = UIManager.getDefaults().keys();
             e.hasMoreElements();) {
            final Object key = e.nextElement();
            final Object value = UIManager.get(key);
            if (value instanceof Font)    {
                final Font f = (Font) value;
                UIManager.put(key,
                              new FontUIResource(
                                         f.getName(),
                                         f.getStyle(),
                                         getConfigData().scaled(f.getSize())));
            }
        }
    }

    /**
     * Set maximum access type.
     */
    public static void setMaxAccessType(
                                      final ConfigData.AccessType accessType) {
        getConfigData().setAccessType(ConfigData.AccessType.RO);
        getConfigData().setMaxAccessType(ConfigData.AccessType.RO);
        for (final Cluster c : getConfigData().getClusters().getClusterSet()) {
            final ClusterBrowser cb = c.getBrowser();
            if (cb != null) {
                cb.checkAccessOfEverything();
            }
        }
    }

    /** Return brigher version of the color (or darker). */
    public static Color brighterColor(final Color c, final double x) {
        double r = c.getRed() * x;
        if (r > 255) {
            r = 255;
        }
        double g = c.getGreen() * x;
        if (g > 255) {
            g = 255;
        }
        double b = c.getBlue() * x;
        if (b > 255) {
            b = 255;
        }
        return new Color((int) r, (int) g, (int) b);
    }

    public static String generateVMMacAddress() {
        StringBuilder mac = new StringBuilder("52:54:00");
        for (int i = 0; i < 3; i++) {
            mac.append(':');
            mac.append(String.format("%02x", (int) (Math.random() * 256)));
        }
        return mac.toString();
    }

    private static List<String> getNameParts(final String name) {
        final List<String> parts = new ArrayList<String>();
        if (name == null) {
            return parts;
        }
        final Pattern p = Pattern.compile("(\\d+|\\D+)");
        final Matcher m = p.matcher(name);
        while (m.find()) {
            parts.add(m.group());
        }
        return parts;
    }

    /**
     * Compare two names, doing the right thing there are numbers in the
     * beginning or in the end of the string.
     */
    public static int compareNames(final String s1, final String s2) {
        final List<String> parts1 = getNameParts(s1);
        final List<String> parts2 = getNameParts(s2);
        int i = 0;
        for (final String p1 : parts1) {
            if (i >= parts2.size()) {
                return 1;
            }
            final String p2 = parts2.get(i);
            int res;
            if (Character.isDigit(p1.charAt(0))) {
                res = Long.compare(Long.parseLong(p1), Long.parseLong(p2));
            } else {
                res = p1.compareToIgnoreCase(p2);
            }
            if (res != 0) {
                return res;
            }
            i++;
        }
        if (parts1.size() == parts2.size()) {
            return 0;
        }
        return -1;
    }
}
