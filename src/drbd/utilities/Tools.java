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

package drbd.utilities;

import drbd.data.ConfigData;
import drbd.data.Host;
import drbd.data.Cluster;
import drbd.data.Clusters;
import drbd.data.DrbdGuiXML;
import drbd.gui.resources.DrbdResourceInfo;
import drbd.gui.resources.Info;
import drbd.gui.resources.ServiceInfo;
import drbd.gui.ClusterBrowser;
import drbd.gui.GUIData;
import drbd.gui.dialog.ConfirmDialog;

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
import java.util.TreeSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.DefaultListModel;
import javax.swing.text.html.HTMLDocument;
import javax.swing.UIManager;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.Cursor;
import java.awt.image.MemoryImageSource;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.net.InetAddress;
import java.lang.reflect.InvocationTargetException;

import java.net.URLClassLoader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

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
    /** Locales. */
    private static String localeLang = "";
    /** Locales. */
    private static String localeCountry = "";

    /** Resource bundle. */
    private static ResourceBundle resource = null;
    /** Application defaults bundle. */
    private static ResourceBundle resourceAppDefaults = null;

    /** Config data object. */
    private static ConfigData configData;
    /** Gui data object. */
    private static GUIData guiData;
    /** Drbd gui xml object. */
    private static DrbdGuiXML drbdGuiXML = new DrbdGuiXML();
    /** String that starts info messages. */
    private static final String INFO_STRING = "INFO: ";
    /** String that starts debug messages. */
    private static final String DEBUG_STRING = "DEBUG: ";
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
    /** Random number generator. */
    private static final Random RANDOM = new Random();
    /** Hash mit classes from plugin. */
    private static final Map<String, RemotePlugin> pluginObjects =
                                           new HashMap<String, RemotePlugin>();
    /** Local plugin directory. */
    private static final String PLUGIN_DIR = System.getProperty("user.home")
                                             + "/.drbd-mc/plugins/";
    /** Remote plugin location. */
    private static final String PLUGIN_LOCATION =
                                     "oss.linbit.com/drbd-mc/drbd-mc-plugins/";
    /**
     * Private constructor.
     */
    private Tools() {
        /* no instantiation possible. */
    }

    /**
     * This is to make this class a singleton.
     */
    public static Tools getInstance() {
        synchronized (Tools.class) {
            if (instance == null) {
                instance = new Tools();
            }
        }
        return instance;
    }

    /**
     * Inits this class.
     */
    public static void init() {
        setDefaults();
        configData         = new ConfigData();
        guiData            = new GUIData();
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

    /**
     * Returns the drbd gui release version.
     */
    public static String getRelease() {
        if (release != null) {
            return release;
        }
        final Properties p = new Properties();
        try {
            p.load(Tools.class.getResourceAsStream("/drbd/release.properties"));
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

    /**
     * Sets defaults from AppDefaults bundle.
     */
    public static void setDefaults() {
        debugLevel = getDefaultInt("DebugLevel");
        if (getDefault("AppWarning").equals("y")) {
            appWarning = true;
        }
        if (getDefault("AppError").equals("y")) {
            appError = true;
        }
        localeLang = getDefault("Locale.Lang");
        localeCountry = getDefault("Locale.Country");

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
            System.out.println(DEBUG_STRING + msg + " (drbd.utilities.Tools)");
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
                               + msg + " (drbd.utilities.Tools)");
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
                System.out.println(DEBUG_STRING + msg);
            } else {
                System.out.println(DEBUG_STRING + msg
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
        System.out.println("ERROR: " + getErrorString(msg));
        SwingUtilities.invokeLater(new Runnable() {
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

    /**
     * Show an ssh error message.
     */
    public static void sshError(final Host host,
                                final String command,
                                final String ans,
                                final int exitCode) {
        final StringBuffer onHost = new StringBuffer("");
        if (host != null) {
            onHost.append(" on host ");
            final Cluster cluster = host.getCluster();
            if (cluster != null) {
                onHost.append(cluster.getName());
                onHost.append(" / ");
            }
            onHost.append(host.getName());
        }
        Tools.printStackTrace();
        Tools.appWarning(Tools.getString("Tools.sshError.command")
                         + " '" + command + "'" + onHost.toString() + "\n"
                         + Tools.getString("Tools.sshError.returned")
                         + " " + exitCode + "\n"
                         + ans);
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

    /**
     * Executes a command with progress indicator.
     */
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
        final StringBuffer output = new StringBuffer("");
        final Integer[] exitCodeHolder = new Integer[]{0};
        if (execCallback == null) {
            ec = new ExecCallback() {
                             public void done(final String ans) {
                                 output.append(ans);
                             }

                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 Tools.appWarning("error: "
                                                  + command
                                                  + " "
                                                  + ans + " rc: "
                                                  + exitCode);
                                 if (outputVisible) {
                                    Tools.sshError(host,
                                                   command,
                                                   ans,
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
        return host.getSSH().new SSHOutput(output.toString(),
                                           exitCodeHolder[0]);
    }

    /** Executes a command. */
    public static SSH.SSHOutput execCommand(final Host host,
                                            final String command,
                                            final ExecCallback execCallback,
                                            final boolean outputVisible,
                                            final int commandTimeout) {
        ExecCallback ec;
        final StringBuffer output = new StringBuffer("");
        final Integer[] exitCodeHolder = new Integer[]{0};
        if (execCallback == null) {
            ec = new ExecCallback() {
                             public void done(final String ans) {
                                 output.append(ans);
                             }

                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 if (outputVisible) {
                                    Tools.sshError(host,
                                                   command,
                                                   ans,
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
        return host.getSSH().new SSHOutput(output.toString(),
                                           exitCodeHolder[0]);
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
                System.out.println("APPWARNING: " + msg);
            } else {
                debug("APPWARNING: " + msg, 2);
            }
        }
    }

    /** Warning with exception error message. */
    public static void appWarning(final String msg, final Exception e) {
        if (!appWarningHash.contains(msg)) {
            appWarningHash.add(msg);
            if (appWarning) {
                System.out.println("APPWARNING: " + msg + ": "
                                   + e.getMessage());
            } else {
                debug("APPWARNING: " + msg, 2);
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

    /**
     * Shows application error message dialog, with a stacktrace.
     */
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
        final StringBuffer errorString = new StringBuffer(300);
        errorString.append(getErrorString("AppError.Text"));
        errorString.append("\nrelease: ");
        errorString.append(getRelease());
        errorString.append("\njava: ");
        errorString.append(System.getProperty("java.vendor"));
        errorString.append(' ');
        errorString.append(System.getProperty("java.version"));
        errorString.append("\n\n");
        errorString.append(getErrorString(msg));
        errorString.append('\n');
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
            final Throwable th = new Throwable();
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            th.printStackTrace(pw);
            pw.close();
            errorString.append('\n');
            errorString.append(sw.toString());
        }


        System.out.println("APPERROR: " + errorString);
        if (!appError) {
            return;
        }

        final JEditorPane errorPane = new JEditorPane(MIME_TYPE_TEXT_PLAIN,
                                                      errorString.toString());
        errorPane.setEditable(false);
        errorPane.setMinimumSize(DIALOG_PANEL_SIZE);
        errorPane.setMaximumSize(DIALOG_PANEL_SIZE);
        errorPane.setPreferredSize(DIALOG_PANEL_SIZE);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(guiData.getMainFrame(),
                                              new JScrollPane(errorPane),
                                              getErrorString("AppError.Title"),
                                              JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Dialog that informs a user about something with ok button.
     */
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

    /**
     * Prints stack trace with text.
     */
    public static void printStackTrace(final String text) {
        System.out.println(text);
        printStackTrace();
    }
    /**
     * Prints stack trace.
     */
    public static void printStackTrace() {
        final Throwable th = new Throwable();
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        pw.close();
        System.out.println(sw.toString());
    }

    /**
     * Loads the save file and returns its content as string. Return null, if
     * nothing was loaded.
     */
    public static String loadFile(final String filename,
                                      final boolean showError) {
        BufferedReader in = null;
        final StringBuffer content = new StringBuffer("");
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

    /**
     * Loads config data from the specified file.
     */
    public static void loadConfigData(final String filename) {
        debug("load", 0);
        final String xml = loadFile(filename, true);
        if (xml == null) {
            return;
        }
        drbdGuiXML.startClusters(null);
        Tools.getGUIData().allHostsUpdate();
    }

    /**
     * Starts the specified clusters and connects to the hosts of these
     * clusters.
     */
    public static void startClusters(final List<Cluster> selectedClusters) {
        drbdGuiXML.startClusters(selectedClusters);
    }

    /**
     * Stops the specified clusters in the gui.
     */
    public static void stopClusters(final List<Cluster> selectedClusters) {
        for (final Cluster cluster : selectedClusters) {
            for (final Host host : cluster.getHosts()) {
                // TODO: can be run concurrently.
                host.disconnect();
            }
            cluster.removeCluster();
            getGUIData().getClustersPanel().removeTab(cluster);
        }
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

    /**
     * Returns cluster names from the parsed save file.
     */
    public static void loadXML(final String xml) {
        drbdGuiXML.loadXML(xml);
    }

    /**
     * Removes all the hosts and clusters from all the panels and data.
     */
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
     */
    public static void save(final String filename) {
        debug("save");
        final String text =
            Tools.getString("Tools.Saving").replaceAll("@FILENAME@", filename);
        startProgressIndicator(text);
        try {
            final FileOutputStream fileOut = new FileOutputStream(filename);
            drbdGuiXML.saveXML(fileOut);
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
                        ResourceBundle.getBundle("drbd.configs.AppDefaults");
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
                        ResourceBundle.getBundle("drbd.configs.AppDefaults");
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
                        ResourceBundle.getBundle("drbd.configs.AppDefaults");
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
                final Locale currentLocale = new Locale(localeLang,
                                                        localeCountry);
                resource =
                    ResourceBundle.getBundle("drbd.configs.TextResource",
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
     */
    public static String getErrorString(final String text) {
        return text;
    }

    /**
     * Returns string that is specific to a distribution and version.
     */
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
                ResourceBundle.getBundle("drbd.configs.DistResource", locale);
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
                                 final ConvertCmdCallback convertCmdCallback) {
        final String[] texts = text.split(";;;");
        final List<String> results =  new ArrayList<String>();
        for (final String t : texts) {
            results.add(getDistString(t, dist, version, arch));
        }
        String ret;
        if (texts.length == 0) {
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
     * Returns array of services with service definiton from
     * ServiceDefinitions resource bundle.
     *
     * @return array of services.
     */
    public static String[] getServiceDefinitions() {
        final ResourceBundle resourceSD =
                  ResourceBundle.getBundle("drbd.configs.ServiceDefinitions");
        try {
            return enumToStringArray(resourceSD.getKeys());
        } catch (Exception e) {
            Tools.appError("cannot get service definitions");
            return new String[]{};
        }
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
                ResourceBundle.getBundle("drbd.configs.ServiceDefinitions");
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
        if (regexp != null) {
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
        debug("dist: " + dist + ", version: " + version, 1);
        final Locale locale = new Locale(dist, "");
        final ResourceBundle resourceCommand =
                ResourceBundle.getBundle("drbd.configs.DistResource", locale);
        String distVersion = null;
        try {
            distVersion = resourceCommand.getString("version:" + version);
        } catch (Exception e) {
            /* with wildcard */
            final StringBuffer buf = new StringBuffer(version);
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
        debug("dist version: " + distVersion, 1);
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
            return null;
        }
        if (strings.length == 1 && strings[0] == null) {
            return null;
        }
        final StringBuffer ret = new StringBuffer("");
        for (int i = 0; i < strings.length - 1; i++) {
            ret.append(strings[i]);
            ret.append(delim);
        }
        ret.append(strings[strings.length - 1]);
        return ret.toString();
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
        if (strings == null || length == 0) {
            return "";
        }
        final StringBuffer ret = new StringBuffer("");
        for (int i = 0; i < length - 1; i++) {
            ret.append(strings[i]);
            ret.append(delim);
        }
        ret.append(strings[length - 1]);
        return ret.toString();
    }

    /**
     * Uppercases the first character.
     */
    public static String ucfirst(final String s) {
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

    /**
     * Returns thrue if object is in StringInfo class.
     */
    public static boolean isStringInfoClass(final Object o) {
        if (o == null
            || o.getClass().getName().equals("drbd.gui.resources.StringInfo")) {
            return true;
        }
        return false;
    }

    /**
     * Escapes for config file.
     */
    public static String escapeConfig(final String value) {
        if (value.indexOf(' ') > -1) {
            return "\"" + value + "\"";
        }
        return value;
    }

    /**
     * Starts progress indicator with specified text.
     */
    public static void startProgressIndicator(final String text) {
        final boolean rightMovement = RANDOM.nextBoolean();
        if (text == null) {
            getGUIData().getMainGlassPane().start(
                                    Tools.getString("Tools.ExecutingCommand"),
                                    null,
                                    rightMovement);
        } else {
            getGUIData().getMainGlassPane().start(text, null, rightMovement);
        }
    }

    /**
     * Starts progress indicator for host or cluster command.
     */
    public static void startProgressIndicator(final String name,
                                              final String text) {
        startProgressIndicator(name + ": " + text);
    }

    /**
     * Stops progress indicator with specified text.
     */
    public static void stopProgressIndicator(final String text) {
        getGUIData().getMainGlassPane().stop(text);
    }

    /**
     * Stops progress indicator for host or cluster command.
     */
    public static void stopProgressIndicator(final String name,
                                             final String text) {
        stopProgressIndicator(name + ": " + text);
    }

    /**
     * Stops progress indicator with failure message.
     */
    public static void progressIndicatorFailed(final String text) {
        getGUIData().getMainGlassPane().failure(text);
    }

    /**
     * Stops progress indicator with failure message for host or cluster
     * command.
     */
    public static void progressIndicatorFailed(final String name,
                                               final String text) {
        progressIndicatorFailed(name + ": " + text);
    }

    /**
     * Sets fixed size for component.
     */
    public static void setSize(final Component c,
                               final int width,
                               final int height) {
        final Dimension d = new Dimension(width, height);
        c.setMaximumSize(d);
        c.setMinimumSize(d);
        c.setPreferredSize(d);
    }

    /**
     * Returns -1 if version1 is smaller that version2, 0 if version1 equals
     * version2 and 1 if version1 is bigger than version2.
     * -100 if version is in bad form.
     */
    public static int compareVersions(final String version1,
                                      final String version2) {
        if (version1 == null || version2 == null) {
            return -100;
        }
        final String[] v1a = version1.split("\\.");
        final String[] v2a = version2.split("\\.");
        if (v1a.length < 1 || v2a.length < 1) {
            return -100;
        }
        int i = 0;
        for (String v1 : v1a) {
            if (i == v1a.length || i == v2a.length) {
                break;
            }
            final String v2 = v2a[i];
            final int v1i = Integer.parseInt(v1);
            final int v2i = Integer.parseInt(v2);

            if (v1i < v2i) {
                return -1;
            } else if (v1i > v2i) {
                return 1;
            }
            i++;
        }
        return 0;
    }

    /**
     * Returns number of characters 'c' in a string 's'.
     */
    public static int charCount(final String s, final char c) {
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
            host.getSSH().createConfig(config, fileName, dir, mode, makeBackup);
        }
    }

    /**
     * Returns border with title.
     */
    public static TitledBorder getBorder(final String text) {
        final TitledBorder titledBorder = new TitledBorder(
                BorderFactory.createLineBorder(Color.BLACK, 1), text);
        titledBorder.setTitleJustification(TitledBorder.LEFT);
        return titledBorder;
    }

    /** Returns a popup in a scrolling pane. */
    public static JScrollPane getScrollingMenu(
                        final MyMenu menu,
                        final DefaultListModel dlm,
                        final MyList list,
                        final Map<MyMenuItem, ButtonCallback> callbackHash) {
        prevScrollingMenuIndex = -1;
        list.setFixedCellHeight(25);
        final int maxSize = dlm.getSize();
        if (maxSize <= 0) {
            return null;
        }
        if (maxSize > 20) {
            list.setVisibleRowCount(20);
        } else {
            list.setVisibleRowCount(maxSize);
        }
        list.addMouseListener(new MouseAdapter() {
            public void mouseExited(final MouseEvent evt) {
                prevScrollingMenuIndex = -1;
                if (callbackHash != null) {
                    for (final MyMenuItem item : callbackHash.keySet()) {
                        callbackHash.get(item).mouseOut();
                        list.clearSelection();
                    }
                }
            }
            public void mouseEntered(final MouseEvent evt) {
                list.requestFocus();
            }

            public void mousePressed(final MouseEvent evt) {
                prevScrollingMenuIndex = -1;
                if (callbackHash != null) {
                    for (final MyMenuItem item : callbackHash.keySet()) {
                        callbackHash.get(item).mouseOut();
                    }
                }
                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        final int index = list.locationToIndex(evt.getPoint());
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                list.setSelectedIndex(index);
                                //TODO: some submenus stay visible, during
                                //ptest, but this breaks group popup menu
                                //setMenuVisible(menu, false);
                                menu.setSelected(false);
                            }
                        });
                        final MyMenuItem item =
                                            (MyMenuItem) dlm.elementAt(index);
                        item.action();
                    }
                });
                thread.start();
            }
        });

        list.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(final MouseEvent evt) {
                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        int pIndex = list.locationToIndex(evt.getPoint());
                        if (!list.getCellBounds(pIndex, pIndex).contains(
                                                         evt.getPoint())) {
                            pIndex = -1;
                        }
                        final int index = pIndex;
                        final int lastIndex = prevScrollingMenuIndex;
                        if (index == lastIndex) {
                            return;
                        }
                        prevScrollingMenuIndex = index;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                list.setSelectedIndex(index);
                            }
                        });
                        if (callbackHash != null) {
                            if (lastIndex >= 0) {
                                final MyMenuItem lastItem =
                                        (MyMenuItem) dlm.elementAt(lastIndex);
                                callbackHash.get(lastItem).mouseOut();
                            }
                            if (index >= 0) {
                                final MyMenuItem item =
                                            (MyMenuItem) dlm.elementAt(index);
                                callbackHash.get(item).mouseOver();
                            }
                        }
                    }
                });
                thread.start();
            }
        });
        final JScrollPane sp = new JScrollPane(list);
        sp.setViewportBorder(null);
        sp.setBorder(null);
        list.addKeyListener(new KeyAdapter() {
            public void keyTyped(final KeyEvent e) {
                final char ch = e.getKeyChar();
                if (ch == ' ' || ch == '\n') {
                    final MyMenuItem item =
                                       (MyMenuItem) list.getSelectedValue();
                    //SwingUtilities.invokeLater(new Runnable() {
                    //    public void run() {
                    //        //menu.setPopupMenuVisible(false);
                    //        setMenuVisible(menu, false);
                    //    }
                    //});
                    if (item != null) {
                        item.action();
                    }
                } else {
                    if (Character.isLetterOrDigit(ch)) {
                        final Thread t = new Thread(new Runnable() {
                            public void run() {
                                getGUIData().getMainGlassPane().start("" + ch,
                                                                      null,
                                                                      true);
                                stopProgressIndicator("" + ch);
                            }
                        });
                        t.start();
                    }
                }
            }
        });
        return sp;
    }

    /**
     * Returns whether the computer, where this program is run, is Linux.
     */
    public static boolean isLinux() {
        return "Linux".equals(System.getProperty("os.name"));
    }

    /**
     * Returns whether the computer, where this program is run, is Windows.
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").indexOf("Windows") == 0;
    }

    /**
     * Sets the html font of the editor pane to be the default font.
     */
    public static void setEditorFont(final JEditorPane ep) {
        final Font font = UIManager.getFont("Label.font");
        final String bodyRule = "body { font-family: " + font.getFamily()
                                + "; "
                                + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) ep.getDocument()).getStyleSheet().addRule(bodyRule);
    }

    /**
     * Reads and returns a content of a text file.
     */
    public static String getFile(final String fileName) {
        try {
            final BufferedReader br =
                        new BufferedReader(
                            new InputStreamReader(
                               Tools.class.getResource(fileName).openStream()));
            final StringBuffer content = new StringBuffer("");
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

    /**
     * Convenience sleep wrapper.
     */
    public static void sleep(final int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Convenience sleep wrapper with float argument.
     */
    public static void sleep(final float ms) {
        sleep((int) ms);
    }

    /** Returns the latest version of this application. */
    public static String getLatestVersion() {
        String version = null;
        final Pattern vp = Pattern.compile(
                              ".*<a\\s+href=\"drbd-mc-([0-9.]*?)\\.tar\\..*");
        try {
            final String url = "http://oss.linbit.com/drbd-mc/?drbd-mc-check-"
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
                    if (version == null || compareVersions(v, version) > 0) {
                        version = v;
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

    /**
     * Opens default browser.
     */
    public static void openBrowswer(final String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new URI(url));
        } catch (java.io.IOException e) {
            Tools.appError("wrong uri", e);
        } catch (java.net.URISyntaxException e) {
            Tools.appError("error opening browser", e);
        }
    }

    /**
     * Prepares vnc viewer, gets the port and creates ssh tunnel. Returns true
     * if ssh tunnel was created.
     */
    private static int prepareVncViewer(final Host host,
                                        final int remotePort) {
        if (remotePort < 0) {
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

    /**
     * Cleans up after vnc viewer. It stops ssh tunnel.
     */
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

    /**
     * Starts Tight VNC viewer.
     */
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

    /**
     * Starts Ultra VNC viewer.
     */
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

    /**
     * Starts Real VNC viewer.
     */
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

    /**
     * Hides mouse pointer.
     */
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

    /**
     * Check whether the string is number.
     */
    public static boolean isNumber(final String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (final NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * Returns list that is expandable by shell. {'a','b'...}
     */
    public static String shellList(final String[] items) {
        final StringBuffer list = new StringBuffer("");
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
            final String device = ((DrbdResourceInfo) o1).getStringValue();
            if (device.equals(o2)) {
                return true;
            }
            final String res = ((DrbdResourceInfo) o1).getName();
            if (res.equals(o2)) {
                return true;
            }
            return false;
        } else if (o1 != null && o1 instanceof Info) {
            final String s1 = ((Info) o1).getStringValue();
            if (s1 == null) {
                return o2 == null;
            }
            if (o2 == null) {
                return false;
            }
            if (o2 instanceof Info) {
                return s1.equals(((Info) o2).getStringValue());
            } else {
                return s1.equals(o2) || o1.toString().equals(o2);
            }
        } else if (o2 != null && o2 instanceof Info) {
            final String s2 = ((Info) o2).getStringValue();
            if (s2 == null) {
                return o1 == null;
            }
            if (o1 == null) {
                return false;
            }
            if (o1 instanceof Info) {
                return s2.equals(((Info) o1).getStringValue());
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

    /**
     * Returns random secret of the specified lenght.
     */
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
        final StringBuffer s = new StringBuffer(len + 1);
        for (int i = 0; i < len; i++) {
            s.append(chars[rand.nextInt(chars.length)]);
        }
        return s.toString();
    }

    /**
     * Returns whether the ip is localhost.
     */
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
        if (isNumber(kb)) {
            final double k = Integer.parseInt(kb);
            if (k / 1024 != (int) (k / 1024)) {
                return kb + "K";
            }
            final double m = k / 1024;
            if (m / 1024 != (int) (m / 1024)) {
                return Integer.toString((int) m) + "M";
            }
            final double g = m / 1024;
            if (g / 1024 != (int) (g / 1024)) {
                return Integer.toString((int) g) + "G";
            }
            final double t = g / 1024;
            if (t / 1024 != (int) (t / 1024)) {
                return Integer.toString((int) t) + "T";
            }
            return Integer.toString((int) g) + "G";
        }
        return kb;
    }

    /** Converts value with unit to kilobites. */
    public static long convertToKilobytes(final String value) {
        final Object[] v = Tools.extractUnit(value);
        if (v.length == 2 && Tools.isNumber((String) v[0])) {
            int num = Integer.parseInt((String) v[0]);
            final String unit = (String) v[1];
            if ("T".equals(unit)) {
                num = num * 1024 * 1024 * 1024;
            } else if ("G".equals(unit)) {
                num = num * 1024 * 1024;
            } else if ("M".equals(unit)) {
                num = num * 1024;
            } else if ("K".equals(unit)) {
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
        if (parent instanceof javax.swing.JViewport) {
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
                for (final java.awt.Component c : invP.getComponents()) {
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
            for (final java.awt.Component c : parent.getComponents()) {
                ((JComponent) c).setVisible(visible);
            }
            parent.repaint();
        }
    }

    /** Sets the menu and all its parents opaque, not opaque. */
    public static void setMenuOpaque(final JComponent menu,
                                      final boolean opaque) {
        JComponent parent = (JComponent) menu.getParent();
        if (parent instanceof javax.swing.JViewport) {
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
                invP.setOpaque(opaque);
                for (final java.awt.Component c : invP.getComponents()) {
                    ((JComponent) c).setOpaque(opaque);
                }
                final JComponent pp = (JComponent) invP.getParent();
                if (pp != null) {
                    pp.setOpaque(opaque);
                }
                inv = (JComponent) ((JPopupMenu) invP).getInvoker();
            }
            menu.setOpaque(opaque);
            parent.setOpaque(opaque);
            final JComponent pp = (JComponent) parent.getParent();
            if (pp != null) {
                pp.setOpaque(opaque);
            }
            for (final java.awt.Component c : parent.getComponents()) {
                ((JComponent) c).setOpaque(opaque);
            }
            parent.repaint();
        }
    }

    /** Converts windows path to unix path. */
    public static String getUnixPath(final String dir) {
        String unixPath;
        if (isWindows()) {
            unixPath = dir.replaceAll("\\\\", "/");
            if (unixPath.length() >= 2
                && "c:".equalsIgnoreCase(unixPath.substring(0, 2))) {
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
    public static boolean serviceInfoListEquals(final List<ServiceInfo> l1,
                                                final List<ServiceInfo> l2) {
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
        final StringBuffer out = new StringBuffer(text.length() + 10);
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
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    /* just wait */
                }
            });
        } catch (final InterruptedException ix) {
            Thread.currentThread().interrupt();
        } catch (final InvocationTargetException x) {
            Tools.printStackTrace();
        }
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

    /** Returns list of plugins. */
    private static Set<String> getPluginList() {
        final Pattern p = Pattern.compile(
                ".*<img src=\"/icons/folder.gif\" alt=\"\\[DIR\\]\">"
                + "</td><td><a href=\".*?/\">(.*?)/</a>.*");
        final Set<String> pluginList = new LinkedHashSet<String>();
        try {
            final String url = "http://" + PLUGIN_LOCATION
                               + "/?drbd-mc-plugin-check-" + getRelease();
            final BufferedReader reader = new BufferedReader(
                             new InputStreamReader(new URL(url).openStream()));
            do {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                final Matcher m = p.matcher(line);
                if (m.matches()) {
                    final String pluginName = m.group(1);
                    pluginList.add(pluginName);
                }
            } while (true);
        } catch (MalformedURLException mue) {
        } catch (IOException ioe) {
        }
        final File dir = new File(PLUGIN_DIR);
        final String[] l = dir.list();
        if (dir != null && l != null) {
            for (final String fn : l) {
                pluginList.add(fn);
            }
        }
        return pluginList;
    }

    /** Loads one plugin. */
    private static Class loadPlugin(final String pluginName,
                                    final String url) {
        final String user = getConfigData().getPluginUser();
        final String passwd = getConfigData().getPluginPassword();

        if (user != null && passwd != null) {
            Authenticator.setDefault(new java.net.Authenticator() {
                protected PasswordAuthentication
                                            getPasswordAuthentication() {
                    return new PasswordAuthentication(user,
                                                   passwd.toCharArray());
                }
            });
        }
        URLClassLoader loader;
        final String[] dirs = pluginName.split(":");
        try {
            loader = new URLClassLoader(new URL[] {
                new URL(url + pluginName + "/" + getRelease() + "/")
            });
        } catch (java.net.MalformedURLException e) {
            Tools.appWarning("could not get: " + url, e);
            return null;
        }

        Class c = null;
        final String className = "plugins." + dirs[dirs.length - 1];
        try {
            c = loader.loadClass(className);
        } catch (java.lang.ClassNotFoundException e) {
            Tools.debug("could not load " + url + " " + className, 1);
            return null;
        }
        return c;
    }

    /** Saves the class. */
    private static void savePluginClass(final String pluginName,
                                        final Class c) {
        for (final Class sc : c.getDeclaredClasses()) {
            savePluginClass(pluginName, sc);
        }
        try {
            final String className = c.getName();
            final String classAsPath = className.replace('.', '/') + ".class";
            final String dirToCreate = PLUGIN_DIR
                                       + pluginName
                                       + "/"
                                       + getRelease()
                                       + "/";
            (new File(dirToCreate + "plugins/")).mkdirs();
            final FileOutputStream fileOut = new FileOutputStream(
                                                                dirToCreate
                                                                + classAsPath);
            final InputStream stream =
                           c.getClassLoader().getResourceAsStream(classAsPath);

            final byte[] buff = new byte[512];
            while (stream.available() > 0) {
                final int l = stream.read(buff);
                fileOut.write(buff, 0, l);
            }
            fileOut.close();
        } catch(FileNotFoundException e) {
            Tools.appWarning("plugin not found", e);
            return;
        } catch (IOException e) {
            Tools.appWarning("could not save plugin", e);
            return;
        }
    }

    /** Load all plugins */
    public static void loadPlugins() {
        final Set<String> pluginList = getPluginList();
        getGUIData().getMainMenu().reloadPluginsMenu(pluginList);
        for (final String pluginName : pluginList) {
            Class c = loadPlugin(pluginName, "http://" + PLUGIN_LOCATION);
            if (c == null) {
                c = loadPlugin(pluginName, "file://" + PLUGIN_DIR);
                if (c == null) {
                    continue;
                }
            } else {
                savePluginClass(pluginName, c);
                /* cache it. */
            }
            final Class pluginClass = c;
            RemotePlugin remotePlugin = null;
            try {
                remotePlugin = (RemotePlugin) pluginClass.newInstance();
            } catch (java.lang.InstantiationException e) {
                Tools.appWarning("could not instantiate plugin: " + pluginName,
                                 e);
                continue;
            } catch (java.lang.IllegalAccessException e) {
                Tools.appWarning("could not access plugin: " + pluginName, e);
                continue;
            }
            remotePlugin.init();
            pluginObjects.put(pluginName, remotePlugin);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    getGUIData().getMainMenu().enablePluginMenu(
                                                        pluginName,
                                                        pluginClass != null);
                    if (pluginClass != null) {
                        Tools.info(pluginName + " was enabled");
                    }
                }
            });
        }
    }

    /** Show plugin description. */
    public static void showPluginDescription(final String pluginName) {
        final RemotePlugin remotePlugin = pluginObjects.get(pluginName);
        if (remotePlugin != null) {
            remotePlugin.showDescription();
        }
    }

    /** Adds menu items from plugins. */
    public static void addPluginMenuItems(final Info info,
                                          final List<UpdatableItem> items) {
        for (final String pluginName : pluginObjects.keySet()) {
            pluginObjects.get(pluginName).addPluginMenuItems(info, items);
        }
    }
}
