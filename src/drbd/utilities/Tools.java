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
import drbd.gui.ClusterBrowser;
import drbd.data.DrbdGuiXML;
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

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.text.html.HTMLDocument;
import javax.swing.UIManager;

import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.InputStreamReader;

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
        final java.net.URL imgURL =
                        Tools.class.getResource("/images/" + imageFilename);
        if (imgURL == null) {
            Tools.appWarning("Couldn't find image: " + imageFilename);
            return null;
        } else {
            return new ImageIcon(imgURL);
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
            System.out.println(DEBUG_STRING + msg + " (drbd.utilities.Tools)");
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
            System.out.println(DEBUG_STRING + msg
                               + " (" + object.getClass().getName() + ")");
        }
    }

    /**
     * Shows warning message dialog and prints warning to the stdout.
     *
     * @param msg
     *          warning message
     */
    public static void warning(final String msg) {
        System.out.println("WARNING: " + getString(msg));
        JOptionPane.showMessageDialog(
                            guiData.getMainFrame(),
                            getString(msg),
                            getString("Warning.Title"),
                            JOptionPane.WARNING_MESSAGE
                           );

    }

    /**
     * Shows error message dialog and prints error to the stdout.
     *
     * @param msg
     *          error message
     */
    public static void error(final String msg) {
        System.out.println("ERROR: " + getErrorString(msg));
        JOptionPane.showMessageDialog(
                            guiData.getMainFrame(),
                            getErrorString(msg),
                            getString("Error.Title"),
                            JOptionPane.ERROR_MESSAGE
                           );

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
        Tools.error(Tools.getString("Tools.sshError.command")
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
     * Shows exec command dialog with progress bar. The dialog disposes
     * of itself after command has finished. If command failed and retry
     * button was pressed the dialog will be restarted.
     *
     * @param host
     *          host
     * @param command
     *          command
     * @param outputVisible
     *          whether the output of the command should be visible
     */
    public static String execCommandProgressIndicator(final Host host,
                                           final String command,
                                           final ExecCallback execCallback,
                                           final boolean outputVisible) {
        return execCommandProgressIndicator(host,
                                            command,
                                            execCallback,
                                            outputVisible,
                                            command);
    }

    /**
     * Executes a command with progress indicator.
     */
    public static String execCommandProgressIndicator(final Host host,
                                           final String command,
                                           final ExecCallback execCallback,
                                           final boolean outputVisible,
                                           final String text) {
        ExecCallback ec;
        final String hostName = host.getName();
        Tools.startProgressIndicator(hostName, text);
        final StringBuffer output = new StringBuffer("");
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
                                 output.append(ans);
                             }
                         };
        } else {
            ec = execCallback;
        }
        final Thread commandThread = host.execCommandRaw(command,
                                                         ec,
                                                         outputVisible,
                                                         true);


        try {
            if (commandThread != null) {
                commandThread.join(0);
            }
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Tools.stopProgressIndicator(hostName, text);
        return output.toString();
    }

    /**
     * Executes a command.
     */
    public static String execCommand(final Host host,
                                     final String command,
                                     final ExecCallback execCallback,
                                     final boolean outputVisible) {
        return execCommand(host, command, execCallback, outputVisible, null);
    }

    /**
     * Executes a command.
     */
    public static String execCommand(final Host host,
                                     final String command,
                                     final ExecCallback execCallback,
                                     final boolean outputVisible,
                                     final String text) {
        final StringBuffer output = new StringBuffer("");
        ExecCallback ec;
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
                             }
                           };
        } else {
            ec = execCallback;
        }

        final Thread commandThread = host.execCommandRaw(command,
                                                         ec,
                                                         outputVisible,
                                                         true);


        try {
            if (commandThread != null) {
                commandThread.join(0);
            }
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return output.toString();
    }

    /**
     * Shows application warning message dialog if application warning messages
     * are enabled.
     *
     * @param msg
     *          warning message
     */
    public static void appWarning(final String msg) {
        if (appWarning) {
            System.out.println("APPWARNING: " + msg);
        } else {
            debug("APPWARNING: " + msg, 2);
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
        final StringBuffer errorString = new StringBuffer(300);
        errorString.append(getErrorString("AppError.Text"));
        errorString.append("\nrelease: ");
        errorString.append(getRelease());
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

        if (!appError) {
            debug("APPERROR: " + errorString);
            return;
        }


        System.out.println("APPERROR: " + errorString);
        final JEditorPane errorPane = new JEditorPane("text/plain",
                                                      errorString.toString());
        errorPane.setEditable(false);
        errorPane.setMinimumSize(DIALOG_PANEL_SIZE);
        errorPane.setMaximumSize(DIALOG_PANEL_SIZE);
        errorPane.setPreferredSize(DIALOG_PANEL_SIZE);
        JOptionPane.showMessageDialog(
                            guiData.getMainFrame(),
                            new JScrollPane(errorPane),
                            getErrorString("AppError.Title"),
                            JOptionPane.ERROR_MESSAGE
                           );
    }

    /**
     * Dialog that informs a user about something with ok button.
     */
    public static void infoDialog(final String title,
                                  final String info1,
                                  final String info2) {
        final JEditorPane infoPane = new JEditorPane("text/plain",
                                                     info1 + "\n" + info2);
        infoPane.setEditable(false);
        infoPane.setMinimumSize(DIALOG_PANEL_SIZE);
        infoPane.setMaximumSize(DIALOG_PANEL_SIZE);
        infoPane.setPreferredSize(DIALOG_PANEL_SIZE);
        JOptionPane.showMessageDialog(
                            guiData.getMainFrame(),
                            new JScrollPane(infoPane),
                            getErrorString(title),
                            JOptionPane.ERROR_MESSAGE
                           );
    }

    /**
     * Checks string if it is an ip.
     *
     * @param ipString
     *          Ip string to be checked.
     *
     * @return whether string is ip or not.
     */
    public static boolean checkIp(final String ipString) {
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
    public static String loadSaveFile(final String filename,
                                      final boolean showError) {
        BufferedReader in = null;
        final StringBuffer xml = new StringBuffer("");
        //Tools.startProgressIndicator(getString("Tools.Loading"));
        try {
            in = new BufferedReader(new FileReader(filename));
            String line = "";
            while ((line = in.readLine()) != null) {
                xml.append(line);
            }
        } catch (Exception ex) {
            //Tools.stopProgressIndicator(getString("Tools.Loading"));
            //Tools.progressIndicatorFailed(getString("Tools.Loading")
            //                              + " failed");
            if (showError) {
                infoDialog("Load Error",
                           "The file " + filename + " failed to load",
                           ex.getMessage());
            }
            return null;
        }
        //Tools.stopProgressIndicator(getString("Tools.Loading"));
        if (in != null)  {
            try {
                in.close();
            } catch (IOException ex) {
                Tools.appError("Could not close: " + filename, ex);
            }
        }
        return xml.toString();
    }

    /**
     * Loads config data from the specified file.
     */
    public static void loadConfigData(final String filename) {
        debug("load", 0);
        final String xml = loadSaveFile(filename, true);
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
                host.setCluster(null);
            }
            getGUIData().getClustersPanel().removeTab(cluster);
        }
    }

    /**
     * Removes the specified clusters from the gui.
     */
    public static void removeClusters(final List<Cluster> selectedClusters) {
        for (final Cluster cluster : selectedClusters) {
            getConfigData().removeClusterFromClusters(cluster);
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
        startProgressIndicator(getString("Tools.Saving"));
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
            stopProgressIndicator(getString("Tools.Saving"));
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
        try {
            return ((Integer) resourceAppDefaults.getObject(option)).intValue();
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
                                       String version) {
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
        try {
            final String ret = resourceString.getString(text);
            debug("ret: " + ret, 2);
            return ret;
        } catch (Exception e) {
            return null;
        }
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
                                 final ConvertCmdCallback convertCmdCallback) {
        String ret = getDistString(text, dist, version);
        if (convertCmdCallback != null) {
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
                             + service);
            return new String[]{};
        }
    }

    /**
     * Converts kernelVersion as parsed from uname to a version that is used
     * in the download area on the website.
     */
    public static String getKernelDownloadDir(final String kernelVersion,
                                              final String dist,
                                              final String version) {
        final String regexp = getDistString("kerneldir", dist, version);
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
            return "";
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
    public static String[] enumToStringArray(final Enumeration e) {
        final List<String> list = new ArrayList<String>();
        while (e.hasMoreElements()) {
            list.add((String) e.nextElement());
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
    public static List<String> getIntersection(final List<String> setA,
                                               final List<String> setB) {
        final List<String> resultSet = new ArrayList<String>();
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
     * converts score to the string.
     *
     * @param score
     *          score, that is to be converted
     *
     * @return score as string
     */
    public static String scoreToString(final String score) {
        String str = "";
        if ("-INFINITY".equals(score)) {
            str = getString("Score.MinusInfinityString");
        } else if ("INFINITY".equals(score)) {
            str = getString("Score.InfinityString");
        } else {
            try {
                final long scoreInt = Integer.valueOf(score);
                if (scoreInt == 0) {
                    str = getString("Score.ZeroString");
                } else if (scoreInt > 0) {
                    str = getString("Score.PlusString");
                } else {
                    str = getString("Score.MinusString");
                }
            } catch (Exception e) {
                str = getString("Score.Unknown");
            }
        }
        return str + " (" + scoreToHBString(score) + ")";
    }

    /**
     * converts score to the score as used in the heartbeat.
     *
     * @param score
     *          score, that is to be converted
     *
     * @return score as used in the heartbeat
     */
    public static String scoreToHBString(final String score) {
        String str = "";
        if ("-INFINITY".equals(score)) {
            str = "-infinity";
        } else if ("INFINITY".equals(score)) {
            str = "infinity";
        } else {
            str = score;
        }
        return str;
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
        //if (o.getClass().getName().equals("java.lang.String"))
        //    return true;
        return false;
    }

    /**
     * Returns thrue if object is in StringInfo class.
     */
    public static boolean isStringInfoClass(final Object o) {
        if (o == null
            || o.getClass().getName().equals("drbd.gui.Browser$StringInfo")) {
            return true;
        }
        return false;
    }

    /**
     * Hides panel with expert options if not in an expert mode.
     *
     * @param extraOptionsPanel
     *          panel with extra options
     */
    public static void hideExpertModePanel(final JPanel extraOptionsPanel) {
        if (!Tools.getConfigData().getExpertMode()) {
            extraOptionsPanel.setVisible(false);
        }
    }

    /**
     * Returns expert mode check box. That hides extraOptionsPanel if expert
     * mode was deactivated.
     *
     * @param extraOptionsPanel
     *          panel with extra options
     *
     * @return expert mode checkbox
     */
    public static JCheckBox expertModeButton(final JPanel extraOptionsPanel) {
        final JCheckBox expertCB = new JCheckBox(Tools.getString(
                                                    "Browser.ExpertMode"));
        expertCB.setBackground(Tools.getDefaultColor(
                                            "ViewPanel.Status.Background"));
        expertCB.setSelected(Tools.getConfigData().getExpertMode());
        expertCB.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final boolean selected =
                                    e.getStateChange() == ItemEvent.SELECTED;
                if (selected != Tools.getConfigData().getExpertMode()) {
                    Tools.getConfigData().setExpertMode(selected);
                    //selectMyself();
                    extraOptionsPanel.setVisible(selected);
                    extraOptionsPanel.invalidate();
                    extraOptionsPanel.validate();
                    extraOptionsPanel.repaint();
                }
            }
        });
        return expertCB;
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
        if (text == null) {
            getGUIData().getMainGlassPane().start(
                                        getString("Tools.ExecutingCommand"));
        } else {
            getGUIData().getMainGlassPane().start(text);
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
        if (v1a.length != 3 || v2a.length != 3) {
            return -100;
        }
        int i = 0;
        for (String v1 : v1a) {
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

    /**
     * Returns a popup in a scrolling pane.
     */
    public static JScrollPane getScrollingMenu(final MyMenu menu,
                                               final DefaultListModel m) {
        final JList list = new JList(m);
        list.addMouseListener(new MouseAdapter() {
            public void mousePressed(final MouseEvent evt) {
                final int index = list.locationToIndex(evt.getPoint());
                list.setSelectedIndex(index);
                menu.setPopupMenuVisible(false);
                menu.setSelected(false);
                ((MyMenuItem) m.elementAt(index)).action();
            }
        });

        list.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(final MouseEvent evt) {
                final int index = list.locationToIndex(evt.getPoint());
                list.setSelectedIndex(index);
            }
        });
        return new JScrollPane(list);
    }

    /**
     * Returns whether the computer, where this program is run, is Linux.
     */
    public static boolean isLinux() {
        return "Linux".equals(System.getProperty("os.name"));
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
}
