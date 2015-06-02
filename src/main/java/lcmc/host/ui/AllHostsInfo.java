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
package lcmc.host.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.Info;
import lcmc.common.ui.main.MainData;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.ui.Browser;
import lcmc.cluster.ui.widget.GenericWidget.MTextField;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Clusters;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostFactory;
import lcmc.common.domain.UserConfig;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;

/**
 * This class holds all hosts that are added to the GUI as opposite to all
 * hosts in a cluster.
 */
@Named
@Singleton
public final class AllHostsInfo extends Info {
    private static final Logger LOG = LoggerFactory.getLogger(AllHostsInfo.class);
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(Tools.getDefault("ClusterTab.ClusterIcon"));
    private static final String QUICK_CLUSTER_BOX_TITLE = Tools.getString("AllHostsInfo.QuickCluster");
    private static final String CLUSTER_NAME_PLACE_HOLDER = "cluster name...";
    private static final String DEFAULT_CLUSTER_NAME = "default";
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(Tools.getDefault("EmptyBrowser.HostIcon"));
    private JPanel infoPanel = null;
    private final Map<Cluster, JCheckBox> allClusterCheckboxes = new HashMap<Cluster, JCheckBox>();
    private final Map<Cluster, MyButton> allLoadButtons = new HashMap<Cluster, MyButton>();
    private final Map<Cluster, JPanel> clusterBoxBackgrounds = new HashMap<Cluster, JPanel>();
    private final JPanel mainPanel = new JPanel(new GridBagLayout());
    private final GridBagConstraints gridBagConstraints = new GridBagConstraints();
    @Inject
    private WidgetFactory widgetFactory;
    private MyButton loadMarkedClustersButton;
    /** Stop marked clusters button. */
    private MyButton unloadMarkedClustersButton;
    /** Remove marked clusters button. */
    private MyButton removeMarkedClustersButton;
    @Inject
    private UserConfig userConfig;
    @Inject
    private Provider<AddHostDialog> addHostDialogProvider;
    @Inject
    private HostFactory hostFactory;
    @Inject
    private MainData mainData;
    @Inject
    private MainPresenter mainPresenter;
    @Inject
    private Provider<Cluster> clusterProvider;
    @Inject
    private Clusters allClusters;
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private TreeMenuController treeMenuController;

    public void init(final Browser browser) {
        super.init(Tools.getString("ClusterBrowser.AllHosts"), browser);
        loadMarkedClustersButton = widgetFactory.createButton(
                Tools.getString("EmptyBrowser.LoadMarkedClusters"),
                CLUSTER_ICON,
                Tools.getString("EmptyBrowser.LoadMarkedClusters.ToolTip"));
        unloadMarkedClustersButton = widgetFactory.createButton(
                Tools.getString("EmptyBrowser.UnloadMarkedClusters"),
                CLUSTER_ICON,
                Tools.getString("EmptyBrowser.UnloadMarkedClusters.ToolTip"));
        removeMarkedClustersButton = widgetFactory.createButton(
                Tools.getString("EmptyBrowser.RemoveMarkedClusters"),
                CLUSTER_ICON,
                Tools.getString("EmptyBrowser.RemoveMarkedClusters.ToolTip"));
    }

    private void removeMarkedClusters() {
        LOG.debug1("removeMarkedClusters: start");
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Collection<Cluster> selectedRunningClusters = new ArrayList<Cluster>();
                final Collection<Cluster> selectedClusters = new ArrayList<Cluster>();
                final List<String> clusterNames = new ArrayList<String>();
                final Set<Cluster> clusters = allClusters.getClusterSet();
                for (final Cluster cluster : clusters) {
                    final JCheckBox wi = allClusterCheckboxes.get(cluster);
                    LOG.debug1("removeMarkedClusters: cluster: " + cluster.getName() + ": wi: " + (wi != null));
                    if (wi.isSelected()) {
                        selectedClusters.add(cluster);
                        clusterNames.add(cluster.getName());
                        if (cluster.getClusterTab() != null) {
                            selectedRunningClusters.add(cluster);
                        }
                    }
                }
                final String clustersString = Tools.join(", ", clusterNames.toArray(new String[clusterNames.size()]));
                if (!application.confirmDialog(Tools.getString("EmptyBrowser.confirmRemoveMarkedClusters.Title"),
                                         Tools.getString("EmptyBrowser.confirmRemoveMarkedClusters.Desc")
                                              .replaceAll("@CLUSTERS@", Matcher.quoteReplacement(clustersString)),
                                         Tools.getString("EmptyBrowser.confirmRemoveMarkedClusters.Yes"),
                                         Tools.getString("EmptyBrowser.confirmRemoveMarkedClusters.No"))) {
                    return;
                }
                swingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        removeMarkedClustersButton.setEnabled(false);
                        loadMarkedClustersButton.setEnabled(false);
                        unloadMarkedClustersButton.setEnabled(false);
                    }
                });
                Tools.stopClusters(selectedRunningClusters);
                application.removeClusters(selectedClusters);
                final String saveFile = application.getDefaultSaveFile();
                userConfig.saveConfig(saveFile, false);
                mainPanel.repaint();
                swingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for (final Cluster cluster : selectedClusters) {
                            final JPanel p = clusterBoxBackgrounds.get(cluster);
                            if (p != null) {
                                clusterBoxBackgrounds.remove(cluster);
                                allClusterCheckboxes.remove(cluster);
                                allLoadButtons.remove(cluster);
                                mainPanel.remove(p);
                                treeMenuController.reloadNode(getNode(), false);
                                treeMenuController.repaintMenuTree();
                            }
                        }
                    }
                });
            }
        });
        thread.start();
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

        final Set<Cluster> clusters = allClusters.getClusterSet();
        if (clusters != null) {
            final JPanel bPanel = new JPanel(new BorderLayout());
            bPanel.setMaximumSize(new Dimension(10000, 60));
            final JPanel markedPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
            markedPanel.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
            /* start marked clusters */
            loadMarkedClustersButton.setEnabled(false);
            markedPanel.add(loadMarkedClustersButton);

            /* stop marked clusters */
            unloadMarkedClustersButton.setEnabled(false);
            markedPanel.add(unloadMarkedClustersButton);
            /* remove marked clusters */
            removeMarkedClustersButton.setEnabled(false);
            markedPanel.add(removeMarkedClustersButton);

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
            loadMarkedClustersButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("getInfoPanel: BUTTON: load marked");
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            loadMarkedClusters();
                        }
                    });
                    thread.start();
                }
            });

            /* stop marked clusters action listener */
            unloadMarkedClustersButton.addActionListener(new ActionListener() {
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
            removeMarkedClustersButton.addActionListener(
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
        final JScrollPane clustersPane = new JScrollPane(mPanel,
                                                         JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        infoPanel.add(clustersPane);
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Tools.sleep(3000);
                if (application.getAutoHosts().isEmpty() && !application.getAutoClusters().isEmpty()) {
                    swingUtils.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            for (final Map.Entry<Cluster, MyButton> clusterEntry : allLoadButtons.entrySet()) {
                                if (clusterEntry.getKey().getClusterTab() == null
                                    && application.getAutoClusters().contains(clusterEntry.getKey().getName())) {
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
            final Font newFont = font.deriveFont(Font.PLAIN, (float) (font.getSize() / 1.2));
            nl.setFont(newFont);
            label.add(nl);
        }
        final JPanel startPanel = new JPanel(new BorderLayout());
        startPanel.setBackground(Browser.PANEL_BACKGROUND);
        clusterBoxBackgrounds.put(cluster, startPanel);
        startPanel.setBorder(new LineBorder(Tools.getDefaultColor("EmptyBrowser.StartPanelTitleBorder")));
        final JPanel left = new JPanel();
        left.setBackground(Browser.PANEL_BACKGROUND);
        clusterBoxBackgrounds.put(cluster, startPanel);
        final JCheckBox markWi = new JCheckBox();
        markWi.setBackground(Browser.PANEL_BACKGROUND);
        allClusterCheckboxes.put(cluster, markWi);
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

    private MyButton loadClusterButton(final Cluster cluster, final JCheckBox markWi) {
        final MyButton loadClusterBtn = widgetFactory.createButton(Tools.getString("EmptyBrowser.LoadClusterButton"));
        allLoadButtons.put(cluster, loadClusterBtn);
        loadClusterBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LOG.debug1("LoadClusterButton: BUTTON: load cluster: " + cluster.getName());
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        swingUtils.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                loadClusterBtn.setEnabled(false);
                            }
                        });
                        final Collection<Cluster> selectedClusters = new ArrayList<Cluster>();
                        selectedClusters.add(cluster);
                        userConfig.startClusters(selectedClusters);

                        if (cluster.getClusterTab() == null) {
                            loadClusterBtn.setEnabled(true);
                        } else {
                            swingUtils.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    clusterBoxBackgrounds.get(cluster).setBackground(Color.GREEN);
                                    markWi.setSelected(false);
                                }
                            });
                        }
                    }
                });
                thread.start();
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
        final JTextField clusterTF = new MTextField(CLUSTER_NAME_PLACE_HOLDER);
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
            final Font newFont = font.deriveFont(Font.PLAIN, (float) (font.getSize() / 1.2));
            nl.setFont(newFont);
            label.add(nl);
        }
        final JPanel startPanel = new JPanel(new BorderLayout());

        startPanel.setBackground(Browser.PANEL_BACKGROUND);
        final TitledBorder titleBorder = Tools.getBorder(QUICK_CLUSTER_BOX_TITLE);
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

    private MyButton quickClusterButton(final JTextField clusterTF, final Iterable<JTextField> hostsTF) {
        /* Quick cluster button */
        final MyButton quickClusterBtn = widgetFactory.createButton(Tools.getString("EmptyBrowser.LoadClusterButton"));
        quickClusterBtn.setEnabled(false);

        quickClusterBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LOG.debug1("quickClusterButton: BUTTON: quick cluster");
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String clusterName = clusterTF.getText();
                        final Cluster cluster = clusterProvider.get();
                        final String newClusterName;
                        if (CLUSTER_NAME_PLACE_HOLDER.equals(clusterName)) {
                            newClusterName = DEFAULT_CLUSTER_NAME;
                        } else {
                            newClusterName = clusterName;
                        }
                        if (allClusters.isClusterName(newClusterName)) {
                            cluster.setName(allClusters.getNextClusterName(newClusterName + ' '));
                        } else {
                            cluster.setName(newClusterName);
                        }
                        application.addClusterToClusters(cluster);
                        addClusterBox(cluster);
                        addCheckboxListener(cluster);
                        for (final JTextField hostTF : hostsTF) {
                            final String entered = hostTF.getText();
                            String hostName;
                            String username = null;
                            final int a = entered.indexOf('@');
                            if (a > 0) {
                                username = entered.substring(0, a);
                                hostName = entered.substring(a + 1, entered.length());
                            } else {
                                hostName = entered;
                            }
                            final int p = hostName.indexOf(':');
                            String port = null;
                            if (p > 0) {
                                port = hostName.substring(p + 1, hostName.length());
                                hostName = hostName.substring(0, p);
                            }
                            final Host host = hostFactory.createInstance(hostName);
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
                            host.setCluster(cluster);
                            host.setHostname(hostName);
                            cluster.addHost(host);
                            application.addHostToHosts(host);
                            mainPresenter.allHostsUpdate();
                        }
                        application.addClusterToClusters(cluster);
                        final Collection<Cluster> selectedClusters = new ArrayList<Cluster>();
                        selectedClusters.add(cluster);
                        userConfig.startClusters(selectedClusters);
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

    /** Add listeners that enable the quick connect button. */
    private void textfieldListener(final JTextField textfield, final MyButton button) {
        textfield.getDocument().addDocumentListener(new DocumentListener() {
                    private void check() {
                        swingUtils.invokeLater(new Runnable() {
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
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                loadButton.setEnabled(false);
                clusterBoxBackgrounds.get(cluster).setBackground(Color.GREEN);
            }
        });
    }

    public void setDisconnected(final Cluster cluster) {
        final MyButton loadButton = allLoadButtons.get(cluster);
        if (loadButton != null) {
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    loadButton.setEnabled(true);
                }
            });
        }
    }

    /** Adds checkbox listener for this cluster's checkbox. */
    public void addCheckboxListener(final Cluster cluster) {
        final JCheckBox wi = allClusterCheckboxes.get(cluster);
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

    private void loadMarkedClusters() {
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                loadMarkedClustersButton.setEnabled(false);
            }
        });
        final Collection<Cluster> selectedClusters = new ArrayList<Cluster>();
        for (final Map.Entry<Cluster, JCheckBox> checkBoxEntry : allClusterCheckboxes.entrySet()) {
            if (checkBoxEntry.getKey().getClusterTab() == null) {
                final JCheckBox wi = checkBoxEntry.getValue();
                if (wi.isSelected()) {
                    selectedClusters.add(checkBoxEntry.getKey());
                    setConnected(checkBoxEntry.getKey());
                } else if (checkBoxEntry.getKey().getClusterTab() == null) {
                    final MyButton loadButton = allLoadButtons.get(checkBoxEntry.getKey());
                    if (loadButton != null) {
                        swingUtils.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                loadButton.setEnabled(true);
                            }
                        });
                    }
                }
            }
        }
        userConfig.startClusters(selectedClusters);
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final Map.Entry<Cluster, JCheckBox> checkBoxEntry : allClusterCheckboxes.entrySet()) {
                    if (selectedClusters.contains(checkBoxEntry.getKey())) {
                        checkBoxEntry.getValue().setSelected(false);
                    }
                }
            }
        });
    }

    private void unloadMarkedClusters(final Iterable<Cluster> clusters) {
        swingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                unloadMarkedClustersButton.setEnabled(false);
            }
        });
        final Collection<Cluster> selectedClusters = new ArrayList<Cluster>();
        for (final Cluster cluster : clusters) {
            if (cluster.getClusterTab() != null) {
                final JCheckBox wi = allClusterCheckboxes.get(cluster);
                if (wi.isSelected()) {
                    swingUtils.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            clusterBoxBackgrounds.get(cluster).setBackground(Color.WHITE);
                            allClusterCheckboxes.get(cluster).setSelected(false);
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
        for (final Map.Entry<Cluster, JCheckBox> checkBoxEntry : allClusterCheckboxes.entrySet()) {
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
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (notRunningCount >= 1) {
                        for (final Cluster cluster : allClusterCheckboxes.keySet()) {
                            final MyButton loadButton = allLoadButtons.get(cluster);
                            if (loadButton != null) {
                                loadButton.setEnabled(false);
                            }
                        }
                        /* enable start etc marked clusters button */
                        loadMarkedClustersButton.setEnabled(runningCount == 0);
                    }
                    if (runningCount >= 1) {
                        unloadMarkedClustersButton.setEnabled(notRunningCount == 0);
                    }
                    //TODO: still not working
                    removeMarkedClustersButton.setEnabled(true);
                }
            });
        } else {
            /* deselecting */
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (notRunningCount == 0) {
                        for (final Cluster cluster : allClusterCheckboxes.keySet()) {
                            final MyButton loadButton = allLoadButtons.get(cluster);
                            if (loadButton != null) {
                                if (cluster.getClusterTab() == null) {
                                    loadButton.setEnabled(true);
                                }
                            }
                        }
                        loadMarkedClustersButton.setEnabled(false);
                        if (runningCount > 0) {
                            unloadMarkedClustersButton.setEnabled(true);
                        }
                    }
                    if (runningCount == 0) {
                        unloadMarkedClustersButton.setEnabled(false);
                        if (notRunningCount > 0) {
                            loadMarkedClustersButton.setEnabled(true);
                        }
                    }
                    if (runningCount + notRunningCount == 0) {
                        removeMarkedClustersButton.setEnabled(false);
                    }
                }
            });
        }
    }

    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        /* host wizard */
        final MyMenuItem newHostWizardItem =
            menuFactory.createMenuItem(Tools.getString("EmptyBrowser.NewHostWizard"),
                           HOST_ICON,
                           null,
                           new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                           new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    final Host host = hostFactory.createInstance();
                    final AddHostDialog addHostDialog = addHostDialogProvider.get();
                    addHostDialog.showDialogs(host);
                }});
        items.add(newHostWizardItem);
        mainData.registerAddHostButton(newHostWizardItem);
        return items;
    }
}
