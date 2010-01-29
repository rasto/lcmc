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


package drbd.gui.dialog;

import drbd.utilities.Tools;
import drbd.data.Cluster;
import drbd.data.Host;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import drbd.gui.SpringUtilities;
import drbd.utilities.ExecCallback;
import drbd.gui.ProgressBar;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Arrays;

import javax.swing.text.Document;
import javax.swing.text.StyleConstants;
import javax.swing.text.SimpleAttributeSet;
import java.awt.Color;
import javax.swing.JTextPane;

/**
 * An implementation of an dialog with log files from many hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class ClusterLogs extends ConfigDialog {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Text area for the log. */
    private final JTextPane logTextArea = new JTextPane();
    /** Cluster object. */
    private final Cluster cluster;

    /**
     * Prepares a new <code>ClusterLogs</code> object.
     */
    public ClusterLogs(final Cluster cluster) {
        super();
        this.cluster = cluster;
    }

    /**
     * Returns the cluster object.
     */
    protected final Cluster getCluster() {
        return cluster;
    }

    /**
     * Command that gets the log. The command must be specified in the
     * DistResource or some such.
     */
    protected String logFileCommand() {
        return "Logs.hbLog";
    }

    /**
     * Grep pattern for the log.
     */
    protected String grepPattern() {
        return "lrmd";
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        final Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    getLogs();
                }
            });
        thread.start();
    }

    /**
     * Gets logs from specified command (logFileCommand) and grep pattern
     * (grepPatter). It also mixes the log files from all the nodes, sorts
     * them, and assigns colors for lines from different hosts.
     */
    protected final void getLogs() {
        final Host[] hosts = cluster.getHostsArray();
        Thread[] threads = new Thread[hosts.length];
        final String[] texts = new String[hosts.length];

        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@GREPPATTERN@", grepPattern());

        int i = 0;
        for (final Host host : hosts) {
            final int index = i;
            final String command = host.getDistCommand(logFileCommand(),
                                                       replaceHash);
            threads[index] = host.execCommandRaw(command,
                                 (ProgressBar) null,
                                 new ExecCallback() {
                                     public void done(final String ans) {
                                         texts[index] = ans;
                                     }
                                     public void doneError(final String ans,
                                                           final int exitCode) {
                                         Tools.sshError(host,
                                                        command,
                                                        ans,
                                                        exitCode);
                                     }
                                 }, false, false);
            i++;
        }
        i = 0;
        final StringBuffer ans = new StringBuffer("");
        for (Thread t : threads) {
            try {
                t.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ans.append(texts[i]);
            i++;
        }
        final String[] output = ans.toString().split("\r\n");
        final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                 "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        final Pattern p = Pattern.compile(
                                    "(" + Tools.join("|", months)
                                    + ") +(\\d+) +(\\d+):(\\d+):(\\d+).*");
        final Map<String, Integer> monthsHash = new HashMap<String, Integer>();
        i = 0;
        for (String m : months) {
            monthsHash.put(m, i);
            i++;
        }
        Arrays.sort(output,
                    new Comparator<String>() {
                        public int compare(final String o1, final String o2) {
                            final Matcher m1 = p.matcher(o1);
                            final Matcher m2 = p.matcher(o2);
                            if (m1.matches() && m2.matches()) {
                                final int month1 = monthsHash.get(m1.group(1));
                                final int month2 = monthsHash.get(m2.group(1));

                                final int day1   = Integer.valueOf(m1.group(2));
                                final int day2   = Integer.valueOf(m2.group(2));

                                final int hour1  = Integer.valueOf(m1.group(3));
                                final int hour2  = Integer.valueOf(m2.group(3));

                                final int min1   = Integer.valueOf(m1.group(4));
                                final int min2   = Integer.valueOf(m2.group(4));

                                final int sec1   = Integer.valueOf(m1.group(5));
                                final int sec2   = Integer.valueOf(m2.group(5));

                                if (month1 != month2) {
                                    return month1 < month2 ? -1 : 1;
                                }
                                if (day1 != day2) {
                                    return day1 < day2 ? -1 : 1;
                                }
                                if (hour1 != hour2) {
                                    return hour1 < hour2 ? -1 : 1;
                                }
                                if (min1 != min2) {
                                    return min1 < min2 ? -1 : 1;
                                }
                                if (sec1 != sec2) {
                                    return sec1 < sec2 ? -1 : 1;
                                }
                            }
                            return 0;
                        }
                    }
                   );
        logTextArea.setText("");
        final Document doc = logTextArea.getStyledDocument();
        final SimpleAttributeSet color1 = new SimpleAttributeSet();
        final SimpleAttributeSet color2 = new SimpleAttributeSet();
        SimpleAttributeSet color = null;
        //promptColorStyleConstants.setForeground(c, host.getColor());
        StyleConstants.setForeground(color1, Color.BLACK);
        StyleConstants.setForeground(color2, Color.BLUE);
        int start = 0;
        int a = 0;
        String prevHost = "";
        for (String line : output) {
            final String[] tok = line.split("\\s+");
            if (tok.length > 3) {
                final String host = tok[3];
                if (!host.equals(prevHost)) {
                    if (a == 0) {
                        a++;
                        color = color1;
                    } else {
                        a--;
                        color = color2;
                    }
                }
                prevHost = host;
            }
            try {
                doc.insertString(start, line + "\n", color);
            } catch (Exception e) {
                Tools.appError("Could not insert string", e);
                continue;
            }

            start = start + line.length() + 1;
                logTextArea.setCaretPosition(
                                    logTextArea.getDocument().getLength());
        }

        //logTextArea.setText(Tools.join("\n", output));
        enableComponents();
    }

    /**
     * Gets the title of the dialog as string.
     */
    protected final String getDialogTitle() {
        return Tools.getString("Dialog.ClusterLogs.Title");
    }

    /**
     * Returns description for dialog. This can be HTML defined in
     * TextResource.
     */
    protected final String getDescription() {
        return "";
    }

    /**
     * Returns panel for logs.
     */
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        logTextArea.setEditable(false);
        logTextArea.setText("loading...");

        pane.add(new JScrollPane(logTextArea));
        SpringUtilities.makeCompactGrid(pane, 1, 1,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad
        return pane;
    }
}
