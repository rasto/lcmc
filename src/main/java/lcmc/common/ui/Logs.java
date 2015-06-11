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


package lcmc.common.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.common.domain.ExecCallback;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.domain.util.Tools;
import lcmc.cluster.service.ssh.ExecCommandConfig;

/**
 * An implementation of an dialog with log files from many hosts.
 */
@Named
public class Logs extends ConfigDialog {
    private static final Logger LOG = LoggerFactory.getLogger(Logs.class);
    private final JTextPane logTextArea = new JTextPane();
    private final Map<String, JCheckBox> patternToCheckBoxMap = new HashMap<String, JCheckBox>();
    private final Lock mRefreshLock = new ReentrantLock();
    private final Collection<JComponent> additionalComponents = new ArrayList<JComponent>();
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private WidgetFactory widgetFactory;

    /**
     * Command that gets the log. The command must be specified in the
     * DistResource or some such.
     */
    protected String logFileCommand() {
        return "Logs.hbLog";
    }

    protected final String grepPattern() {
        final StringBuilder pattern = new StringBuilder(40);
        pattern.append('\'');
        final Map<String, String> patternMap = getPatternMap();
        boolean first = true;
        for (final Map.Entry<String, String> patternEntry : patternMap.entrySet()) {
            if (patternToCheckBoxMap.get(patternEntry.getKey()).isSelected()) {
                if (!first) {
                    pattern.append(".*");
                }
                pattern.append(patternEntry.getValue());
                first = false;
            }
        }
        pattern.append('\'');
        return pattern.toString();
    }

    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableAllComponents(false);
        refreshLogsThread();
    }

    protected void refreshLogsThread() {
        final Thread thread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    if (!mRefreshLock.tryLock()) {
                        return;
                    }
                    try {
                        refreshLogs();
                    } finally {
                        mRefreshLock.unlock();
                    }
                }
            });
        thread.start();
    }

    protected Host[] getHosts() {
        return new Host[]{};
    }

    protected void enableAllComponents(final boolean enable) {
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final Map.Entry<String, JCheckBox> checkBoxEntry : patternToCheckBoxMap.entrySet()) {
                    checkBoxEntry.getValue().setEnabled(enable);
                }
                for (final JComponent ac : additionalComponents) {
                    ac.setEnabled(enable);
                }
            }
        });
    }

    protected Map<String, String> getOptionsHash() {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@GREPPATTERN@", grepPattern());
        return replaceHash;
    }

    /**
     * Gets logs from specified command (logFileCommand) and grep pattern
     * (grepPatter). It also mixes the log files from all the nodes, sorts
     * them, and assigns colors for lines from different hosts.
     */
    protected final void refreshLogs() {
        enableAllComponents(false);
        final Host[] hosts = getHosts();
        final Thread[] threads = new Thread[hosts.length];
        final String[] texts = new String[hosts.length];

        final Map<String, String> replaceHash = getOptionsHash();

        int i = 0;
        final String stacktrace = Tools.getStackTrace();
        for (final Host host : hosts) {
            final int index = i;
            final String command = host.getHostParser().getDistCommand(logFileCommand(), replaceHash);
            threads[index] = host.execCommand(new ExecCommandConfig()
                                                   .command(command)
                                                   .execCallback(new ExecCallback() {
                                                       @Override
                                                       public void done(final String answer) {
                                                           texts[index] = answer;
                                                       }
                                                       @Override
                                                       public void doneError(final String answer,
                                                                             final int errorCode) {
                                                           texts[index] = host.getName() + ": " + answer + '\n';
                                                           LOG.sshError(host, command, answer, stacktrace, errorCode);
                                                       }
                                                   })
                                                   .sshCommandTimeout(30000)
                                                   .silentCommand()
                                                   .silentOutput());
            i++;
        }
        i = 0;
        final StringBuilder ans = new StringBuilder("");
        for (final Thread t : threads) {
            try {
                t.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            ans.append(texts[i]);
            i++;
        }
        final String[] output = ans.toString().split("\r\n");
        final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        final Pattern p = Pattern.compile('(' + Tools.join("|", months) + ") +(\\d+) +(\\d+):(\\d+):(\\d+).*");
        final Map<String, Integer> monthsHash = new HashMap<String, Integer>();
        i = 0;
        for (final String m : months) {
            monthsHash.put(m, i);
            i++;
        }
        Arrays.sort(output,
                    new Comparator<String>() {
                        @Override
                        public int compare(final String o1, final String o2) {
                            final Matcher m1 = p.matcher(o1);
                            final Matcher m2 = p.matcher(o2);
                            if (m1.matches() && m2.matches()) {
                                final int month1 = monthsHash.get(m1.group(1));
                                final int month2 = monthsHash.get(m2.group(1));

                                final int day1 = Integer.valueOf(m1.group(2));
                                final int day2 = Integer.valueOf(m2.group(2));

                                final int hour1 = Integer.valueOf(m1.group(3));
                                final int hour2 = Integer.valueOf(m2.group(3));

                                final int min1 = Integer.valueOf(m1.group(4));
                                final int min2 = Integer.valueOf(m2.group(4));

                                final int sec1 = Integer.valueOf(m1.group(5));
                                final int sec2 = Integer.valueOf(m2.group(5));

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
        StyleConstants.setForeground(color1, Color.BLACK);
        StyleConstants.setForeground(color2, Color.BLUE);
        SimpleAttributeSet color = null;
        int start = 0;
        int a = 0;
        String prevHost = "";
        for (final String line : output) {
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
            final SimpleAttributeSet color0 = color;
            final int start0 = start;
            try {
                doc.insertString(start0, line + '\n', color0);
            } catch (final BadLocationException e) {
                LOG.appError("refreshLogs: could not insert line", e);
            }

            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            start += line.length() + 1;
        }

        enableComponents();
        enableAllComponents(true);
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.ClusterLogs.Title");
    }

    @Override
    protected String getDescription() {
        return "";
    }

    protected final String wordBoundary(final String w) {
        return "\\\\<" + w + "\\\\>";
    }

    /** Returns a map from pattern name to its pattern. */
    protected Map<String, String> getPatternMap() {
        return new LinkedHashMap<String, String>();
    }

    /** Returns which pattern names are selected by default. */
    protected Set<String> getSelectedSet() {
        return new HashSet<String>();
    }

    /** Returns panel with checkboxes. */
    private JPanel getGrepChoicesPane() {
        final JPanel pane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        pane.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        final Map<String, String> patternMap = getPatternMap();
        for (final String name : patternMap.keySet()) {
            final JCheckBox cb = new JCheckBox(name, getSelectedSet().contains(name));
            cb.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
            patternToCheckBoxMap.put(name, cb);
            cb.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    refreshLogsThread();
                }
            });
            pane.add(cb);
        }
        for (final JComponent ac : getAdditionalComponents()) {
            additionalComponents.add(ac);
            pane.add(ac);
        }
        return pane;
    }

    protected MyButton getRefreshBtn() {
        final MyButton refreshBtn = widgetFactory.createButton(Tools.getString("Dialog.Logs.RefreshButton"));
        refreshBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                refreshLogsThread();
            }
        });
        return refreshBtn;
    }

    protected JComponent[] getAdditionalComponents() {
        return new JComponent[]{getRefreshBtn()};
    }

    @Override
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        pane.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        logTextArea.setEditable(false);
        logTextArea.setText("loading...");
        pane.add(getGrepChoicesPane());
        final JScrollPane sp = new JScrollPane(logTextArea);
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        pane.add(sp);
        pane.setMaximumSize(new Dimension(Short.MAX_VALUE, pane.getPreferredSize().height));
        return pane;
    }

    @Override
    protected final ImageIcon icon() {
        return Info.LOGFILE_ICON;
    }
}
