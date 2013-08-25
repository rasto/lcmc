/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */


package lcmc.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import lcmc.data.Cluster;
import lcmc.data.ConfigData;
import lcmc.data.Host;
import lcmc.gui.dialog.BugReport;

/**
 * This class provides tools for logging.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Logger {
    /** Name of the class. */
    private final String className;
    /** Map with all warnings, so that they don't appear more than once. */
    private final Set<String> appWarningHash = new HashSet<String>();
    /** Map with all errors, so that they don't appear more than once. */
    private final Set<String> appErrorHash = new HashSet<String>();
    /** String that starts error messages. */
    private static final String ERROR_STRING = "ERROR: ";
    /** String that starts info messages. */
    private static final String INFO_STRING = "INFO: ";
    /** String that starts debug messages. */
    private static final String DEBUG_STRING = "DEBUG: ";
    /** String that starts debug messages. */
    private static final String DEBUG1_STRING = "DEBUG(1): ";
    /** String that starts debug messages. */
    private static final String DEBUG2_STRING = "DEBUG(2): ";
    /** String that starts trace messages. */
    private static final String TRACE_STRING = "TRACE: ";
    /** string that starts application warnings. */
    private static final String APPWARNING_STRING = "APPWARNING: ";
    /** String that starts application errors. */
    private static final String APPERROR_STRING = "APPERROR: ";
    /** Time when the application started in seconds. */
    private static final long START_TIME = System.currentTimeMillis() / 1000;
    /** Patterns that match exceptions that can be ignored. */
    public static final List<Pattern> IGNORE_EXCEPTION_PATTERNS =
        Collections.unmodifiableList(new ArrayList<Pattern>(Arrays.asList(
            Pattern.compile(".*:1.6.0_27:.*ToolTipManager\\.java.*",
                            Pattern.DOTALL))));

    public Logger(final String className) {
        this.className = className;
    }

    /** Returns seconds since start. */
    private long seconds() {
        return System.currentTimeMillis() / 1000 - START_TIME;
    }

    /** Prints info message to the STDOUT. */
    public void info(final String msg) {
        final String msg0 = INFO_STRING + msg;
        System.out.println(msg0);
        LoggerFactory.LOG_BUFFER.add(msg0);
    }

    public void debug(final String msg) {
        debug(DEBUG_STRING, msg, 0);
    }

    public void debug1(final String msg) {
        debug(DEBUG1_STRING, msg, 1);
    }

    public void debug2(final String msg) {
        debug(DEBUG2_STRING, msg, 2);
    }

    public void trace(final String msg) {
        debug(TRACE_STRING, msg, 3);
    }

    /**
     * Prints debug message to the stdout. Only messages with level smaller
     * or equal than debug level will be printed.
     *
     * @param msg
     *          debug message
     * @param level
     *          level of this message.
     */
    private void debug(final String prefix, final String msg, final int level) {
        if (level <= LoggerFactory.getDebugLevel() + 1) {
            final String msg0 = new StringBuilder()
                                .append(prefix)
                                .append('[')
                                .append(seconds())
                                .append("s] ")
                                .append(msg)
                                .append(" (")
                                .append(className)
                                .append(')')
                                .toString();
            if (level <= LoggerFactory.getDebugLevel()) {
                System.out.println(msg0);
            }
            LoggerFactory.LOG_BUFFER.add(msg0);
        }
    }

    /**
     * Shows error message dialog and prints error to the stdout.
     *
     * @param msg
     *          error message
     */
    public void error(final String msg) {
        final String msg0 = ERROR_STRING + msg;
        System.out.println(msg0);
        LoggerFactory.LOG_BUFFER.add(msg0);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(
                            Tools.getGUIData().getMainFrame(),
                            new JScrollPane(new JTextArea(msg, 20, 60)),
                            Tools.getString("Error.Title"),
                            JOptionPane.ERROR_MESSAGE);
            }
        });

    }

    /** Show an ssh error message. */
    public void sshError(final Host host,
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
        appWarning(Tools.getString("Tools.sshError.command")
                   + " '" + command + "'" + onHost.toString() + "\n"
                   + Tools.getString("Tools.sshError.returned")
                   + " " + exitCode + "\n"
                   + ans + "\n"
                   + stacktrace);
    }

    /**
     * Shows application warning message dialog if application warning messages
     * are enabled.
     *
     * @param msg
     *          warning message
     */
    public void appWarning(final String msg) {
        if (!appWarningHash.contains(msg)) {
            appWarningHash.add(msg);
            final String msg0 = APPWARNING_STRING + msg;
            if (LoggerFactory.getAppWarning()) {
                System.out.println(msg0);
                LoggerFactory.LOG_BUFFER.add(msg0);
            } else {
                debug(msg0);
            }
        }
    }

    /** Warning with exception error message. */
    public void appWarning(final String msg, final Exception e) {
        if (!appWarningHash.contains(msg)) {
            appWarningHash.add(msg);
            final String msg0 = APPWARNING_STRING + msg + ": "
                                + e.getMessage();
            if (LoggerFactory.getAppWarning()) {
                System.out.println(msg0);
                LoggerFactory.LOG_BUFFER.add(msg0);
            } else {
                debug(msg0);
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
    public void appError(final String msg) {
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
    public void appError(final String msg, final String msg2) {
        appError(msg, msg2, null);
    }

    /** Shows application error message dialog, with a stacktrace. */
    public void appError(final String msg, final Throwable e) {
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
    public void appError(final String msg,
                         final String msg2,
                         final Throwable e) {
        if (appErrorHash.contains(msg + msg2)) {
            return;
        }
        appErrorHash.add(msg + msg2);
        final StringBuilder errorString = new StringBuilder(300);
        errorString.append("\nApplication error, ")
                   .append("switching to read-only mode.\n")
                   .append(msg)
                   .append("\nLCMC release: ")
                   .append(Tools.getRelease())
                   .append("\nJava: ")
                   .append(System.getProperty("java.vendor"))
                   .append(' ')
                   .append(System.getProperty("java.version"))
                   .append("\n\n=== error ===\n")
                   .append(msg2)
                   .append(Tools.getStackTrace(e));

        if (e == null) {
            /* stack trace */
            errorString.append('\n');
            errorString.append(Tools.getStackTrace());
        }


        System.out.println(APPERROR_STRING + errorString);
        if (!LoggerFactory.getAppError()) {
            return;
        }
        if (e != null && ignoreException(e)) {
            System.out.println("ignoring: " + APPERROR_STRING + errorString);
            return;
        }

        Tools.getGUIData().getMainMenu().setOperatingMode(
                                                        ConfigData.OP_MODE_RO);

        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final BugReport br = new BugReport(BugReport.UNKNOWN_CLUSTER,
                                                   errorString.toString());
                br.showDialog();
            }
        });
        t.start();
    }

    /** Return whether to ignore some exception. */
    private boolean ignoreException(final Throwable e) {
        final String vendor = System.getProperty("java.vendor");
        final String version = System.getProperty("java.version");
        final String stackTrace = Tools.getStackTrace(e);
        final String exception = new StringBuilder(vendor)
                                        .append(':')
                                        .append(version)
                                        .append(':')
                                        .append(e.getMessage())
                                        .append('\n')
                                        .append(stackTrace)
                                        .toString();
        for (final Pattern p : IGNORE_EXCEPTION_PATTERNS) {
            if (p.matcher(exception).matches()) {
                return true;
            }
        }
        return false;
    }
}
