/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package lcmc.gui.resources;

import lcmc.AddHostDialog;
import lcmc.gui.Browser;
import lcmc.gui.TerminalPanel;
import lcmc.data.Cluster;
import lcmc.data.Clusters;
import lcmc.data.Host;
import lcmc.data.Application;
import lcmc.data.AccessMode;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.Tools;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.MyButton;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;
import javax.swing.JScrollPane;
import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.FlowLayout;
import java.awt.Insets;

import lcmc.gui.widget.GenericWidget.MTextField;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class holds all hosts that are added to the GUI as opposite to all
 * hosts in a cluster.
 */
public final class AllHostsInfo extends Info {
    /** Logger. */
    private static final Logger LOG =
                                 LoggerFactory.getLogger(AllHostsInfo.class);
    /** Cluster icon. */
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                                   Tools.getDefault("ClusterTab.ClusterIcon"));
    /** Title over quick connect box. */
    private static final String QUICK_CLUSTER_TITLE =
                                  Tools.getString("AllHostsInfo.QuickCluster");
    /** Place holder for cluster name in the textfield. */
    private static final String CLUSTER_NAME_PH = "cluster name...";
    /** Default cluster name. */
    private static final String DEFAULT_CLUSTER_NAME = "default";
    /** Host icon. */
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(
                                Tools.getDefault("EmptyBrowser.HostIcon"));
    /** infoPanel cache. */
    private JPanel infoPanel = null;
    /** Checkboxes in the cluster boxes. */
    private final Map<Cluster, JCheckBox> allCheckboxes =
                                         new HashMap<Cluster, JCheckBox>();
    /** Start/Load buttons. */
    private final Map<Cluster, MyButton> allLoadButtons =
                                         new HashMap<Cluster, MyButton>();
    /** Backgrounds of the small boxes with clusters. */
    private final Map<Cluster, JPanel> clusterBackgrounds =
                                         new HashMap<Cluster, JPanel>();
    /** Main panel. */
    private final JPanel mainPanel = new JPanel(new GridBagLayout());
    /** Constraints. */
    private final GridBagConstraints gridBagConstraints = new GridBagConstraints();
    /** Start marked clusters button. */
    private final MyButton loadMarkedClustersBtn = new MyButton(
              Tools.getString("EmptyBrowser.LoadMarkedClusters"),
              CLUSTER_ICON,
              Tools.getString("EmptyBrowser.LoadMarkedClusters.ToolTip"));
    /** Stop marked clusters button. */
    private final MyButton unloadMarkedClustersBtn = new MyButton(
                  Tools.getString("EmptyBrowser.UnloadMarkedClusters"),
                  CLUSTER_ICON,
                  Tools.getString(
                             "EmptyBrowser.UnloadMarkedClusters.ToolTip"));
    /** Remove marked clusters button. */
    private final MyButton removeMarkedClustersBtn = new MyButton(
                  Tools.getString("EmptyBrowser.RemoveMarkedClusters"),
                  CLUSTER_ICON,
                  Tools.getString(
                             "EmptyBrowser.RemoveMarkedClusters.ToolTip"));
    /** Creates a new AllHostsInfo instance. */
    public AllHostsInfo(final Browser browser) {
        super(Tools.getString("ClusterBrowser.AllHosts"), browser);
    }

    /** Remove marked clusters. */
    private void removeMarkedClusters() {
        LOG.debug1("removeMarkedClusters: start");
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final Collection<Cluster> selectedRunningClusters =
                                              new ArrayList<Cluster>();
                final Collection<Cluster> selectedClusters = new ArrayList<Cluster>();
                final List<String> clusterNames = new ArrayList<String>();
                final Set<Cluster> clusters =
                           Tools.getApplication().getClusters().getClusterSet();
                for (final Cluster cluster : clusters) {
                    final JCheckBox wi = allCheckboxes.get(cluster);
                    LOG.debug1("removeMarkedClusters: cluster: "
                               + cluster.getName()
                               + ": wi: "
                               + (wi != null));
                    if (wi.isSelected()) {
                        selectedClusters.add(cluster);
                        clusterNames.add(cluster.getName());
                        if (cluster.getClusterTab() != null) {
                            selectedRunningClusters.add(cluster);
                        }
                    }
                }
                final String clustersString =
                      Tools.join(", ", clusterNames.toArray(
                                     new String[clusterNames.size()]));
                if (!Tools.confirmDialog(
                     Tools.getString(
                         "EmptyBrowser.confirmRemoveMarkedClusters.Title"),
                     Tools.getString(
                         "EmptyBrowser.confirmRemoveMarkedClusters.Desc").
                         replaceAll("@CLUSTERS@",
                                    Matcher.quoteReplacement(clustersString)),
                     Tools.getString(
                         "EmptyBrowser.confirmRemoveMarkedClusters.Yes"),
                     Tools.getString(
                         "EmptyBrowser.confirmRemoveMarkedClusters.No"))) {
                    return;
                }
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        removeMarkedClustersBtn.setEnabled(false);
                        loadMarkedClustersBtn.setEnabled(false);
                        unloadMarkedClustersBtn.setEnabled(false);
                    }
                });
                Tools.stopClusters(selectedRunningClusters);
                Tools.removeClusters(selectedClusters);
                final String saveFile = Tools.getApplication().getSaveFile();
                Tools.save(saveFile, false);
                mainPanel.repaint();
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for (final Cluster cluster : selectedClusters) {
                            final JPanel p = clusterBackgrounds.get(
                                                                  cluster);
                            if (p != null) {
                                clusterBackgrounds.remove(cluster);
                                allCheckboxes.remove(cluster);
                                allLoadButtons.remove(cluster);
                                mainPanel.remove(p);
                                getBrowser().reload(getNode(), false);
                                getBrowser().repaintTree();
                            }
                        }
                    }
                });
            }
        });
        t.start();
    }

    /**
     * Returns info panel of all hosts menu item. If a host is selected,
     * its tab is selected.
     */
    @Override
    public JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));

        infoPanel.setBackground(Browser.PANEL_BACKGROUND);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(3, 3, 0, 0);

        mainPanel.setBackground(Browser.PANEL_BACKGROUND);
        mainPanel.setBackground(Color.WHITE);

        final Set<Cluster> clusters =
                           Tools.getApplication().getClusters().getClusterSet();
        if (clusters != null) {
            final JPanel bPanel =
                           new JPanel(new BorderLayout());
            bPanel.setMaximumSize(new Dimension(10000, 60));
            final JPanel markedPanel = new JPanel(
                                        new FlowLayout(FlowLayout.LEADING));
            markedPanel.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
            /* start marked clusters */
            loadMarkedClustersBtn.setEnabled(false);
            markedPanel.add(loadMarkedClustersBtn);

            /* stop marked clusters */
            unloadMarkedClustersBtn.setEnabled(false);
            markedPanel.add(unloadMarkedClustersBtn);
            /* remove marked clusters */
            removeMarkedClustersBtn.setEnabled(false);
            markedPanel.add(removeMarkedClustersBtn);

            bPanel.add(markedPanel, BorderLayout.CENTER);
            /* actions menu */
            bPanel.add(getActionsButton(), BorderLayout.LINE_END);
            infoPanel.add(bPanel);
            for (final Cluster cluster : clusters) {
                addClusterBox(cluster);
            }
            /* quick cluster box. */
            addQuickClusterBox();

            /* start marked clusters action listener */
            loadMarkedClustersBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("getInfoPanel: BUTTON: load marked");
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            loadMarkedClusters();
                        }
                    });
                    t.start();
                }
            });

            /* stop marked clusters action listener */
            unloadMarkedClustersBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("getInfoPanel: BUTTON: unload marked");
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            unloadMarkedClusters(clusters);
                        }
                    });
                    t.start();
                }
            });

            /* remove marked clusters action listener */
            removeMarkedClustersBtn.addActionListener(
                                                new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("getInfoPanel: BUTTON: remove marked");
                    removeMarkedClusters();
                }
            });

            /* mark checkbox item listeners */
            for (final Cluster cluster : clusters) {
                addCheckboxListener(cluster);
            }
        }

        final JPanel mPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        mPanel.add(mainPanel);
        mPanel.setBackground(Color.WHITE);
        final JScrollPane clustersPane =
                    new JScrollPane(
                            mPanel,
                            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        infoPanel.add(clustersPane);
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Tools.sleep(3000);
                if (Tools.getApplication().getAutoHosts().isEmpty()
                    && !Tools.getApplication().getAutoClusters().isEmpty()) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            for (final Map.Entry<Cluster, MyButton> clusterEntry : allLoadButtons.entrySet()) {
                                if (clusterEntry.getKey().getClusterTab() == null
                                    && Tools.getApplication().getAutoClusters()
                                                    .contains(clusterEntry.getKey().getName())) {
                                    clusterEntry.getValue().pressButton();
                                }
                            }
                        }
                    });
                }
            }
        });
        t.start();
        return infoPanel;
    }

    /** adds one cluster box to the main cluster. */
    public void addClusterBox(final Cluster cluster) {
        final JPanel label = new JPanel();
        label.setBackground(Browser.PANEL_BACKGROUND);
        label.setLayout(new BoxLayout(label, BoxLayout.PAGE_AXIS));
        label.add(new JLabel(cluster.getName()));
        for (final Host h : cluster.getHosts()) {
            final StringBuilder hostLabel = new StringBuilder();
            if (!h.isRoot()) {
                hostLabel.append(h.getUsername());
                hostLabel.append('@');
            }
            hostLabel.append(h.getName());
            final String port = h.getSSHPort();
            if (port != null && !"22".equals(port)) {
                hostLabel.append(':');
                hostLabel.append(port);
            }

            final JLabel nl = new JLabel("   " + hostLabel);
            final Font font = nl.getFont();
            final Font newFont = font.deriveFont(
                                           Font.PLAIN,
                                           (float) (font.getSize() / 1.2));
            nl.setFont(newFont);
            label.add(nl);
        }
        final JPanel startPanel = new JPanel(new BorderLayout());
        startPanel.setBackground(Browser.PANEL_BACKGROUND);
        clusterBackgrounds.put(cluster, startPanel);
        startPanel.setBorder(new LineBorder(Tools.getDefaultColor(
                                   "EmptyBrowser.StartPanelTitleBorder")));
        final JPanel left = new JPanel();
        left.setBackground(Browser.PANEL_BACKGROUND);
        clusterBackgrounds.put(cluster, startPanel);
        final JCheckBox markWi = new JCheckBox();
        markWi.setBackground(Browser.PANEL_BACKGROUND);
        allCheckboxes.put(cluster, markWi);
        left.add(markWi);
        left.add(label);
        startPanel.add(left, BorderLayout.LINE_START);
        final MyButton loadClusterBtn = loadClusterButton(cluster, markWi);
        startPanel.add(loadClusterBtn, BorderLayout.LINE_END);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(startPanel, gridBagConstraints);
        gridBagConstraints.gridx++;
        if (gridBagConstraints.gridx > 2) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
        }
    }

    private MyButton loadClusterButton(final Cluster cluster,
                                       final JCheckBox markWi) {
        /* Load cluster button */
        final MyButton loadClusterBtn = new MyButton(
           Tools.getString("EmptyBrowser.LoadClusterButton"));
        allLoadButtons.put(cluster, loadClusterBtn);
        loadClusterBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LOG.debug1("LoadClusterButton: BUTTON: load cluster: "
                           + cluster.getName());
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Tools.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                loadClusterBtn.setEnabled(false);
                            }
                        });
                        final Collection<Cluster> selectedClusters =
                                                 new ArrayList<Cluster>();
                        selectedClusters.add(cluster);
                        Tools.startClusters(selectedClusters);

                        if (cluster.getClusterTab() == null) {
                            loadClusterBtn.setEnabled(true);
                        } else {
                            Tools.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    clusterBackgrounds.get(cluster)
                                                   .setBackground(Color.GREEN);
                                    markWi.setSelected(false);
                                }
                            });
                        }
                    }
                });
                t.start();
            }
        });
        return loadClusterBtn;
    }

    /** Adds quick cluster box to the main cluster, where a user can enter
     * hosts via textfield. */
    public void addQuickClusterBox() {
        final JPanel label = new JPanel();
        label.setBackground(Browser.PANEL_BACKGROUND);
        label.setLayout(new BoxLayout(label, BoxLayout.PAGE_AXIS));
        final JTextField clusterTF = new MTextField(CLUSTER_NAME_PH);
        label.add(clusterTF);
        final Collection<JTextField> hostsTF = new ArrayList<JTextField>();
        for (int i = 1; i < 3; i++) {
            final JTextField nl = new MTextField("node" + i + "...", 15);
            nl.setToolTipText("<html><b>enter the node name or ip</b><br>node"
                              + i + "<br>or ...<br>"
                              + System.getProperty("user.name")
                              + "@node" + i + ":22..." + "<br>");
            hostsTF.add(nl);
            nl.selectAll();
            final Font font = nl.getFont();
            final Font newFont = font.deriveFont(
                                           Font.PLAIN,
                                           (float) (font.getSize() / 1.2));
            nl.setFont(newFont);
            label.add(nl);
        }
        final JPanel startPanel = new JPanel(new BorderLayout());

        startPanel.setBackground(Browser.PANEL_BACKGROUND);
        final TitledBorder titleBorder = Tools.getBorder(QUICK_CLUSTER_TITLE);
        startPanel.setBorder(titleBorder);
        final JPanel left = new JPanel();
        left.setBackground(Browser.PANEL_BACKGROUND);
        left.add(label);
        startPanel.add(left, BorderLayout.LINE_START);
        final MyButton loadClusterBtn = quickClusterButton(clusterTF, hostsTF);
        startPanel.add(loadClusterBtn, BorderLayout.LINE_END);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        if (gridBagConstraints.gridx != 0) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
        }
        mainPanel.add(startPanel, gridBagConstraints);
        gridBagConstraints.gridx++;
        if (gridBagConstraints.gridx > 2) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
        }
    }

    /** Return quick connect button. */
    private MyButton quickClusterButton(final JTextField clusterTF,
                                        final Iterable<JTextField> hostsTF) {
        /* Quick cluster button */
        final MyButton quickClusterBtn = new MyButton(
                           Tools.getString("EmptyBrowser.LoadClusterButton"));
        quickClusterBtn.setEnabled(false);

        quickClusterBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LOG.debug1("quickClusterButton: BUTTON: quick cluster");
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String clusterName = clusterTF.getText();
                        final Cluster cluster = new Cluster();
                        final String newClusterName;
                        if (CLUSTER_NAME_PH.equals(clusterName)) {
                            newClusterName = DEFAULT_CLUSTER_NAME;
                        } else {
                            newClusterName = clusterName;
                        }
                        final Clusters clusters =
                                           Tools.getApplication().getClusters();
                        if (clusters.isClusterName(newClusterName)) {
                            cluster.setName(clusters.getNextClusterName(
                                                       newClusterName + ' '));
                        } else {
                            cluster.setName(newClusterName);
                        }
                        Tools.getApplication().addClusterToClusters(cluster);
                        addClusterBox(cluster);
                        addCheckboxListener(cluster);
                        for (final JTextField hostTF : hostsTF) {
                            final String entered = hostTF.getText();
                            String hostName;
                            String username = null;
                            final int a = entered.indexOf('@');
                            if (a > 0) {
                                username = entered.substring(0, a);
                                hostName = entered.substring(
                                                    a + 1, entered.length());
                            } else {
                                hostName = entered;
                            }
                            final int p = hostName.indexOf(':');
                            String port = null;
                            if (p > 0) {
                                port = hostName.substring(
                                                p + 1, hostName.length());
                                hostName = hostName.substring(0, p);
                            }
                            final Host host = new Host(hostName);
                            if (username == null) {
                                host.setUsername(Host.ROOT_USER);
                            } else {
                                host.setUseSudo(true);
                                host.setUsername(username);
                            }
                            if (port == null) {
                                host.setSSHPort(Host.DEFAULT_SSH_PORT);
                            } else {
                                host.setSSHPort(port);
                            }
                            new TerminalPanel(host);
                            host.setCluster(cluster);
                            host.setHostname(hostName);
                            cluster.addHost(host);
                            Tools.getApplication().addHostToHosts(host);
                            Tools.getGUIData().allHostsUpdate();
                        }
                        Tools.getApplication().addClusterToClusters(cluster);
                        final Collection<Cluster> selectedClusters =
                                                 new ArrayList<Cluster>();
                        selectedClusters.add(cluster);
                        Tools.startClusters(selectedClusters);
                    }
                });
                t.start();
            }
        });

        textfieldListener(clusterTF, quickClusterBtn);
        for (final JTextField htf : hostsTF) {
            textfieldListener(htf, quickClusterBtn);
        }
        return quickClusterBtn;
    }

    /** Add listeners that enablle the quick connect button. */
    private void textfieldListener(final JTextField textfield,
                                   final MyButton button) {
        textfield.getDocument().addDocumentListener(new DocumentListener() {
                    private void check() {
                        Tools.invokeLater(new Runnable() {
                    @Override
                            public void run() {
                                button.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void insertUpdate(final DocumentEvent e) {
                        check();
                    }

                    @Override
                    public void removeUpdate(final DocumentEvent e) {
                        check();
                    }

                    @Override
                    public void changedUpdate(final DocumentEvent e) {
                        check();
                    }
                });
    }

    /**
     * Sets this cluster as connected. It is called after user enters a
     * cluster through the dialogs.
     */
    public void setConnected(final Cluster cluster) {
        final MyButton loadButton = allLoadButtons.get(cluster);
        if (loadButton == null) {
            return;
        }
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                loadButton.setEnabled(false);
                clusterBackgrounds.get(cluster).setBackground(Color.GREEN);
            }
        });
    }

    /** Sets this cluster as disconnected. */
    public void setDisconnected(final Cluster cluster) {
        final MyButton loadButton = allLoadButtons.get(cluster);
        if (loadButton != null) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    loadButton.setEnabled(true);
                }
            });
        }
    }

    /** Adds checkbox listener for this cluster's checkbox. */
    public void addCheckboxListener(final Cluster cluster) {
        final JCheckBox wi = allCheckboxes.get(cluster);
        wi.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        allCheckboxesListener(wi);
                    }
                });
                thread.start();
            }

        });
    }

    /** Starts marked clusters. */
    private void loadMarkedClusters() {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                loadMarkedClustersBtn.setEnabled(false);
            }
        });
        final Collection<Cluster> selectedClusters = new ArrayList<Cluster>();
        for (final Map.Entry<Cluster, JCheckBox> checkBoxEntry : allCheckboxes.entrySet()) {
            if (checkBoxEntry.getKey().getClusterTab() == null) {
                final JCheckBox wi = checkBoxEntry.getValue();
                if (wi.isSelected()) {
                    selectedClusters.add(checkBoxEntry.getKey());
                    setConnected(checkBoxEntry.getKey());
                } else if (checkBoxEntry.getKey().getClusterTab() == null) {
                    final MyButton loadButton = allLoadButtons.get(checkBoxEntry.getKey());
                    if (loadButton != null) {
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                loadButton.setEnabled(true);
                            }
                        });
                    }
                }
            }
        }
        Tools.startClusters(selectedClusters);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final Map.Entry<Cluster, JCheckBox> checkBoxEntry : allCheckboxes.entrySet()) {
                    if (selectedClusters.contains(checkBoxEntry.getKey())) {
                        checkBoxEntry.getValue().setSelected(false);
                    }
                }
            }
        });
    }

    /** Stops marked clusters. */
    private void unloadMarkedClusters(final Iterable<Cluster> clusters) {
        Tools.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                unloadMarkedClustersBtn.setEnabled(false);
            }
        });
        final Collection<Cluster> selectedClusters = new ArrayList<Cluster>();
        for (final Cluster cluster : clusters) {
            if (cluster.getClusterTab() != null) {
                final JCheckBox wi = allCheckboxes.get(cluster);
                if (wi.isSelected()) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            clusterBackgrounds.get(cluster).setBackground(
                                                               Color.WHITE);
                            allCheckboxes.get(cluster).setSelected(false);
                        }
                    });
                    selectedClusters.add(cluster);
                }
            }
        }
        Tools.stopClusters(selectedClusters);
    }

    /** Listener for checkboxes that is called from thread. */
    private void allCheckboxesListener(final JCheckBox wi) {
        int rc = 0;
        int nrc = 0;
        for (final Map.Entry<Cluster, JCheckBox> checkBoxEntry : allCheckboxes.entrySet()) {
            if (checkBoxEntry.getValue().isSelected()) {
                if (checkBoxEntry.getKey().getClusterTab() == null) {
                    /* not running */
                    nrc++;
                } else {
                    rc++;
                }
            }
        }
        final int runningCount = rc;
        final int notRunningCount = nrc;
        if (wi.isSelected()) {
            /* disable all start cluster buttons */
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (notRunningCount >= 1) {
                        for (final Cluster cluster : allCheckboxes.keySet()) {
                            final MyButton loadButton =
                                                  allLoadButtons.get(cluster);
                            if (loadButton != null) {
                                loadButton.setEnabled(false);
                            }
                        }
                        /* enable start etc marked clusters button */
                        loadMarkedClustersBtn.setEnabled(runningCount == 0);
                    }
                    if (runningCount >= 1) {
                        unloadMarkedClustersBtn.setEnabled(
                                                 notRunningCount == 0);
                    }
                    //TODO: still not working
                    removeMarkedClustersBtn.setEnabled(true);
                }
            });
        } else {
            /* deselecting */
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (notRunningCount == 0) {
                        for (final Cluster cluster : allCheckboxes.keySet()) {
                            final MyButton loadButton =
                                                  allLoadButtons.get(cluster);
                            if (loadButton != null) {
                                if (cluster.getClusterTab() == null) {
                                    loadButton.setEnabled(true);
                                }
                            }
                        }
                        loadMarkedClustersBtn.setEnabled(false);
                        if (runningCount > 0) {
                            unloadMarkedClustersBtn.setEnabled(true);
                        }
                    }
                    if (runningCount == 0) {
                        unloadMarkedClustersBtn.setEnabled(false);
                        if (notRunningCount > 0) {
                            loadMarkedClustersBtn.setEnabled(true);
                        }
                    }
                    if (runningCount + notRunningCount == 0) {
                        removeMarkedClustersBtn.setEnabled(false);
                    }
                }
            });
        }
    }

    /** Creates the popup for all hosts. */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        /* host wizard */
        final MyMenuItem newHostWizardItem =
            new MyMenuItem(Tools.getString("EmptyBrowser.NewHostWizard"),
                           HOST_ICON,
                           null,
                           new AccessMode(Application.AccessType.RO, false),
                           new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final AddHostDialog dialog = new AddHostDialog(new Host());
                    dialog.showDialogs();
                }
            };
        items.add(newHostWizardItem);
        Tools.getGUIData().registerAddHostButton(newHostWizardItem);
        return items;
    }
}
