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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import lcmc.model.Application;
import lcmc.model.Cluster;
import lcmc.model.Clusters;
import lcmc.model.crm.ClusterStatus;
import lcmc.model.drbd.DrbdXml;
import lcmc.model.Host;
import lcmc.model.vm.VmsXml;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.Http;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * An implementation of a bug report dialog.
 */
@Named
public final class BugReport extends ConfigDialog {
    private static final Logger LOG = LoggerFactory.getLogger(BugReport.class);

    private static final String CONFIG_CIB = "pcmk configs";
    private static final String CONFIG_DRBD = "DRBD configs";
    private static final String CONFIG_LIBVIRT = "libvirt configs";
    private static final String GENERATED_DELIM = "=== configs ===";
    private static final String LOG_BUFFER_DELIM = "=== logs ===";
    public static final Cluster UNKNOWN_CLUSTER = null;
    public static final String NO_ERROR_TEXT = null;
    public static final int MINIMUM_CLUSTERS_PANE_HEIGHT = 75;

    private Cluster selectedCluster;
    private String errorText;
    private final JTextPane bugReportTextArea = new JTextPane();
    private final Map<String, JCheckBox> configCheckBoxMap = new HashMap<String, JCheckBox>();
    private final Map<Cluster, JCheckBox> clusterCheckBoxMap = new HashMap<Cluster, JCheckBox>();
    private String logBuffer;
    @Inject
    private Application application;
    @Inject
    private Clusters allClusters;

    public void init(final Cluster selectedCluster, final String errorText) {
        this.selectedCluster = selectedCluster;
        this.errorText = errorText;
        logBuffer = LoggerFactory.getLogBuffer();
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        refresh();
        enableComponents();
    }

    private void enableAllComponents(final boolean enable) {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final Map.Entry<String, JCheckBox> configEntry : configCheckBoxMap.entrySet()) {
                    configEntry.getValue().setEnabled(enable);
                }
            }
        });
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.BugReport.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.BugReport.Description");
    }

    private List<String> getConfigChoices() {
        final List<String> choices = new ArrayList<String>();
        choices.add(CONFIG_CIB);
        choices.add(CONFIG_DRBD);
        choices.add(CONFIG_LIBVIRT);
        return choices;
    }

    private JComponent getConfigPane() {
        final List<String> config = getConfigChoices();
        final JPanel pane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        pane.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        for (final String name : config) {
            final JCheckBox cb = new JCheckBox(name, true);
            cb.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
            configCheckBoxMap.put(name, cb);
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

    private JComponent getClustersPane(final Iterable<Cluster> clusters) {
        final JPanel clusterPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        clusterPane.setBorder(Tools.getBorder("Clusters"));
        clusterPane.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        for (final Cluster cluster : clusters) {
            final JCheckBox cb = new JCheckBox(cluster.getName(), true);
            cb.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
            clusterCheckBoxMap.put(cluster, cb);
            cb.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    refresh();
                }
            });
            clusterPane.add(cb);
        }
        final JScrollPane sp = new JScrollPane(clusterPane,
                                               JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setMinimumSize(new Dimension(0, MINIMUM_CLUSTERS_PANE_HEIGHT));
        return sp;
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        pane.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        bugReportTextArea.setEditable(true);
        bugReportTextArea.setText("loading...");
        final Set<Cluster> clusters = allClusters.getClusterSet();
        final JComponent clPane = getClustersPane(clusters);
        if (clusters.size() > 1) {
            pane.add(clPane);
        }
        if (!clusters.isEmpty()) {
            pane.add(getConfigPane());
        }

        final JScrollPane sp = new JScrollPane(bugReportTextArea);
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        pane.add(sp);
        pane.setMaximumSize(new Dimension(Short.MAX_VALUE, pane.getPreferredSize().height));
        return pane;
    }

    protected void refresh() {
        enableAllComponents(false);
        final Set<Cluster> clusters = allClusters.getClusterSet();
        final String allOldText = bugReportTextArea.getText();
        final int i = allOldText.indexOf(GENERATED_DELIM);
        String oldText = "email: anonymous\nerror description:\n" + (errorText == null ? "" : errorText) + '\n';
        if (i > 0) {
            oldText = allOldText.substring(0, i - 1);
        }
        final StringBuffer text = new StringBuffer();
        text.append(oldText).append('\n').append(GENERATED_DELIM);
        for (final Cluster cluster : clusters) {
            if (clusterCheckBoxMap.get(cluster).isSelected()) {
                appendClusterText(text, cluster);
                appendHostText(text, cluster);
                appendCibText(text, cluster);
                appendDrbdText(text, cluster);
                appendLibvirtText(text, cluster);
            }
        }
        appendLogText(text);
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                bugReportTextArea.setText(text.toString());
                bugReportTextArea.setCaretPosition(0);
            }
        });
        enableAllComponents(true);
    }

    private void appendClusterText(StringBuffer text, Cluster cluster) {
        text.append("\n== ")
            .append(cluster.getName())
            .append(", br: ")
            .append(cluster.getBrowser() != null);
        if (cluster == this.selectedCluster) {
            text.append(" *");
        }
        text.append(" ==\n");
    }

    private void appendHostText(StringBuffer text, Cluster cluster) {
        for (final Host host : cluster.getHosts()) {
            if (host == null) {
                text.append("host == null");
            } else {
                text.append(host.getName()).append(" c: ")
                        .append(host.getCluster() != null)
                        .append(" br: ")
                        .append(host.getBrowser() != null);
                if (host.getBrowser() != null) {
                    text.append(" cbr: ").append(host.getBrowser().getClusterBrowser() != null);
                }
            }
            text.append('\n');
        }
    }

    private void appendCibText(StringBuffer text, Cluster cluster) {
        text.append("= ").append(CONFIG_CIB).append(" =\n");
        if (configCheckBoxMap.get(CONFIG_CIB).isSelected()) {
            String cib = null;
            final ClusterBrowser cb = cluster.getBrowser();
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
    }

    private void appendDrbdText(StringBuffer text, Cluster cluster) {
        text.append("\n\n= ").append(CONFIG_DRBD).append(" =\n");
        if (configCheckBoxMap.get(CONFIG_DRBD).isSelected()) {
            final ClusterBrowser cb = cluster.getBrowser();
            String cib = null;
            if (cb != null) {
                final DrbdXml drbdXml = cb.getDrbdXml();
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
    }

    private void appendLibvirtText(StringBuffer text, Cluster cluster) {
        text.append("\n\n= ")
                .append(CONFIG_LIBVIRT)
                .append(" =\n");
        if (configCheckBoxMap.get(CONFIG_LIBVIRT).isSelected()) {
            for (final Host host : cluster.getHosts()) {
                text.append("\n\n== ")
                        .append(host.getName())
                        .append(" ==\n");
                final ClusterBrowser cb = cluster.getBrowser();
                String cfg = null;
                if (cb != null) {
                    final VmsXml vmsXml = cb.getVmsXml(host);
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

    private void appendLogText(StringBuffer text) {
        text.append('\n').append(LOG_BUFFER_DELIM).append('\n').append(logBuffer);
    }

    String sendReportButton() {
        return buttonString("SendReport");
    }

    @Override
    protected String[] buttons() {
        return new String[]{cancelButton(), sendReportButton()};
    }

    @Override
    protected ConfigDialog checkAnswer() {
        if (isPressedButton(sendReportButton())) {
            LOG.info("checkAnswer: send report");
            Http.post("lcmc", bugReportTextArea.getText());
        }
        return null;
    }

    @Override
    protected ImageIcon[] getIcons() {
        return new ImageIcon[]{WizardDialog.CANCEL_ICON, WizardDialog.FINISH_ICON};
    }
}
