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

package lcmc.gui.dialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import lcmc.utilities.Tools;
import lcmc.utilities.Http;
import lcmc.gui.ClusterBrowser;
import lcmc.data.ClusterStatus;
import lcmc.data.DrbdXML;
import lcmc.data.VMSXML;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import lcmc.data.Cluster;
import lcmc.data.Host;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * An implementation of a bug report dialog.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class BugReport extends ConfigDialog {
    /** Logger. */
    private static final Logger LOG =
                                    LoggerFactory.getLogger(BugReport.class);
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Cluster. Can be null, if unknown. */
    private final Cluster cluster;
    /** Error text, exceptions. Can be null. */
    private final String errorText;
    /** Text area for the bug report. */
    private final JTextPane textArea = new JTextPane();
    /** Map from. */
    private final Map<String, JCheckBox> configCbMap =
                                            new HashMap<String, JCheckBox>();
    private final Map<Cluster, JCheckBox> clusterCbMap =
                                            new HashMap<Cluster, JCheckBox>();
    /** The whole log buffer. */
    private final String logs;

    private static final String CONFIG_CIB = "pcmk configs";
    private static final String CONFIG_DRBD = "DRBD configs";
    private static final String CONFIG_LIBVIRT = "libvirt configs";
    private static final String GENERATED_DELIM = "=== configs ===";
    private static final String LOG_BUFFER_DELIM = "=== logs ===";
    public static final Cluster UNKNOWN_CLUSTER = null;
    public static final String NO_ERROR_TEXT = null;

    /** Prepares a new <code>BugReport</code> object. */
    public BugReport(final Cluster cluster, final String errorText) {
        super();
        this.cluster = cluster;
        this.errorText = errorText;
        logs = LoggerFactory.getLogBuffer();
    }

    /** Inits the dialog and enables all the components. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        refresh();
        enableComponents();
    }

    /** Enables/disables all the components. */
    private void enableAllComponents(final boolean enable) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final String name : configCbMap.keySet()) {
                    configCbMap.get(name).setEnabled(enable);
                }
            }
        });
    }

    /** Gets the title of the dialog as string. */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.BugReport.Title");
    }

    /**
     * Returns description for dialog. This can be HTML defined in
     * TextResource.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.BugReport.Description");
    }

    /** Returns a map from pattern name to its pattern. */
    private List<String> getConfigChoices() {
        final List<String> choices = new ArrayList<String>();
        choices.add(CONFIG_CIB);
        choices.add(CONFIG_DRBD);
        choices.add(CONFIG_LIBVIRT);
        return choices;
    }

    /** Returns panel with config checkboxes. */
    private JComponent getConfigPane() {
        final List<String> config = getConfigChoices();
        final JPanel pane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pane.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        for (final String name : config) {
            final JCheckBox cb = new JCheckBox(name, true);
            cb.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Dark"));
            configCbMap.put(name, cb);
            cb.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    refresh();
                }
            });
            pane.add(cb);
        }
        return pane;
    }

    /** Returns panel with checkboxes. */
    private JComponent getClustersPane(final Set<Cluster> clusters) {
        final JPanel clusterPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        clusterPane.setBorder(Tools.getBorder("Clusters"));
        clusterPane.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        for (final Cluster cl : clusters) {
            final JCheckBox cb = new JCheckBox(cl.getName(),
                                               cl == cluster
                                               || clusters.size() == 1);
            cb.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Dark"));
            clusterCbMap.put(cl, cb);
            cb.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    refresh();
                }
            });
            clusterPane.add(cb);
        }
        final JScrollPane sp =
                    new JScrollPane(clusterPane,
                                    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setMinimumSize(new Dimension(0, 75));
        return sp;
    }

    /** Returns the content of the bug report dialog. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        textArea.setEditable(true);
        textArea.setText("loading...");
        final Set<Cluster> clusters =
                          Tools.getConfigData().getClusters().getClusterSet();
        final JComponent clPane = getClustersPane(clusters);
        if (clusters.size() > 1) {
            pane.add(clPane);
        }
        if (!clusters.isEmpty()) {
            pane.add(getConfigPane());
        }

        final JScrollPane sp = new JScrollPane(textArea);
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        pane.add(sp);
        pane.setMaximumSize(new Dimension(Short.MAX_VALUE,
                                          pane.getPreferredSize().height));
        return pane;
    }

    protected void refresh() {
        enableAllComponents(false);
        final Set<Cluster> clusters =
                          Tools.getConfigData().getClusters().getClusterSet();
        final String allOldText = textArea.getText();
        final int i = allOldText.indexOf(GENERATED_DELIM);
        String oldText = "email: anonymous\nerror description:\n"
                         + (errorText == null ? "" : errorText)
                         + "\n";
        if (i > 0) {
            oldText = allOldText.substring(0, i - 1);
        }
        final StringBuffer text = new StringBuffer();
        text.append(oldText).append("\n").append(GENERATED_DELIM);
        for (final Cluster cl : clusters) {
            if (clusterCbMap.get(cl).isSelected()) {
                text.append("\n== ")
                    .append(cl.getName())
                    .append(", br: ")
                    .append(cl.getBrowser() != null)
                    .append(" ==\n");
                for (final Host host : cl.getHosts()) {
                    if (host == null) {
                        text.append("host == null");
                    } else {
                        text.append(host.getName()).append(" c: ")
                            .append(host.getCluster() != null)
                            .append(" br: ")
                            .append(host.getBrowser() != null);
                        if (host.getBrowser() != null) {
                            text.append(" cbr: ").append(
                             host.getBrowser().getClusterBrowser() != null);
                        }
                    }
                    text.append('\n');
                }
                /* cib */
                text.append("= ").append(CONFIG_CIB).append(" =\n");
                if (configCbMap.get(CONFIG_CIB).isSelected()) {
                    String cib = null;
                    final ClusterBrowser cb = cl.getBrowser();
                    if (cb != null) {
                        final ClusterStatus cs = cb.getClusterStatus();
                        if (cs != null) {
                            cib = cs.getCibXml();
                        }
                    }
                    if (cib == null) {
                        text.append("not available\n");
                    } else {
                        text.append(cib);
                    }
                }

                /* DRBD */
                text.append("\n\n= ").append(CONFIG_DRBD).append(" =\n");
                if (configCbMap.get(CONFIG_DRBD).isSelected()) {
                    final ClusterBrowser cb = cl.getBrowser();
                    String cib = null;
                    if (cb != null) {
                        final DrbdXML drbdXml = cb.getDrbdXML();
                        if (drbdXml != null) {
                            cib = drbdXml.getOldConfig();
                        }
                    }
                    if (cib == null) {
                        text.append("not available\n");
                    } else {
                        text.append(cib);
                    }
                }
                /* libvirt */
                text.append("\n\n= ")
                    .append(CONFIG_LIBVIRT)
                    .append(" =\n");
                if (configCbMap.get(CONFIG_LIBVIRT).isSelected()) {
                    for (final Host host : cl.getHosts()) {
                        text.append("\n\n== ")
                            .append(host.getName())
                            .append(" ==\n");
                        final ClusterBrowser cb = cl.getBrowser();
                        String cfg = null;
                        if (cb != null) {
                            final VMSXML vmsXml = cb.getVMSXML(host);
                            if (vmsXml != null) {
                                cfg = vmsXml.getConfig();
                            }
                        }
                        if (cfg == null) {
                            text.append("not available\n");
                        } else {
                            text.append(cfg);
                        }
                    }
                }
            }
        }
        /** logs */
        text.append('\n').append(LOG_BUFFER_DELIM).append('\n').append(logs);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                textArea.setText(text.toString());
                textArea.setCaretPosition(0);
            }
        });
        enableAllComponents(true);
    }

    String sendReportButton() {
        return buttonString("SendReport");
    }

    @Override
    protected String[] buttons() {
        return new String[]{cancelButton(), sendReportButton()};
    }

    /** @see ConfigDialog#checkAnswer() */
    @Override
    protected ConfigDialog checkAnswer() {
        if (isPressedButton(sendReportButton())) {
            LOG.info("checkAnswer: send report");
            Http.post("lcmc", textArea.getText());
        }
        return null;
    }

    /** @see ConfigDialog#getIcons() */
    @Override
    protected ImageIcon[] getIcons() {
        return new ImageIcon[]{WizardDialog.CANCEL_ICON, WizardDialog.FINISH_ICON};
    }
}
