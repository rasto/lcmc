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

package drbd.gui;

import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.UpdatableItem;
import drbd.utilities.MyMenuItem;
import drbd.data.Host;
import drbd.data.Cluster;
import drbd.AddHostDialog;
import drbd.gui.resources.Info;

import java.awt.Color;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Font;

import javax.swing.BoxLayout;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.border.LineBorder;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

/**
 * This class holds cluster resource data in a tree. It shows panels that allow
 * to edit data of services etc.
 * Every resource has its Info object, that accessible through the tree view.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class EmptyBrowser extends Browser {
    /** Menu's all hosts node. */
    private DefaultMutableTreeNode allHostsNode;
    /** Panel that holds this browser. */
    private EmptyViewPanel emptyViewPanel = null;
    /** Tree menu root. */
    private JTree treeMenu;
    /** Color of the most of backgrounds. */
    private static final Color PANEL_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Background");
    /** Color of the status bar background. */
    private static final Color STATUS_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Status.Background");
    /** Cluster icon. */
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                                   Tools.getDefault("ClusterTab.ClusterIcon"));
    /** Host icon. */
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(
                                Tools.getDefault("EmptyBrowser.HostIcon"));
    /** All hosts info object of the host of this browser. */
    private final AllHostsInfo allHostsInfo = new AllHostsInfo(this);

    /**
     * Prepares a new <code>CusterBrowser</code> object.
     */
    public EmptyBrowser() {
        super();
        /* Load the default file */
        final String saveFile = Tools.getConfigData().getSaveFile();
        final String xml = Tools.loadFile(saveFile, false);
        if (xml != null) {
            Tools.loadXML(xml);
        }
        setTreeTop();
    }

    /**
     * Adds small box with cluster possibility to load it and remove it.
     */
    public final void addClusterBox(final Cluster cluster) {
        allHostsInfo.addClusterBox(cluster);
        allHostsInfo.setAsStarted(cluster);
        allHostsInfo.addCheckboxListener(cluster);
    }

    /**
     * Sets the empty view panel.
     */
    public final void setEmptyViewPanel(final EmptyViewPanel emptyViewPanel) {
        this.emptyViewPanel = emptyViewPanel;
    }

    /**
     * Returns empty view panel.
     */
    public final EmptyViewPanel getEmptyViewPanel() {
        return emptyViewPanel;
    }

    /**
     * Sets the info panel component in the cluster view panel.
     */
    public final void setRightComponentInView(final Info i) {
        emptyViewPanel.setRightComponentInView(this, i);
    }

    /**
     * Initializes hosts tree for the empty view.
     */
    public final void initHosts() {
        /* all hosts */
        allHostsNode = new DefaultMutableTreeNode(allHostsInfo);
        setNode(allHostsNode);
        topAdd(allHostsNode);
    }

    /**
     * Updates resources of a cluster in the tree.
     */
    public final void updateHosts(final JTree treeMenu) {
        this.treeMenu = treeMenu;
        DefaultMutableTreeNode resource;

        /* all hosts */
        final Host[] allHosts =
                              Tools.getConfigData().getHosts().getHostsArray();
        allHostsNode.removeAllChildren();
        for (Host host : allHosts) {
            final HostBrowser hostBrowser = host.getBrowser();
            resource = new DefaultMutableTreeNode(hostBrowser.getHostInfo());
            //setNode(resource);
            allHostsNode.add(resource);
        }
        reload(allHostsNode);
    }

    /**
     * This class holds all hosts that are added to the GUI as opposite to all
     * hosts in a cluster.
     */
    private class AllHostsInfo extends Info {
        /** Possibly selected host or null. */
        private final Host host;
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
        private final GridBagConstraints c = new GridBagConstraints();
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

        /**
         * Creates a new AllHostsInfo instance.
         */
        public AllHostsInfo(final Browser browser) {
            super(Tools.getString("ClusterBrowser.AllHosts"), browser);
            host = null;
        }

        /**
         * Remove marked clusters.
         */
        private void removeMarkedClusters() {
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    final List<Cluster> selectedRunningClusters =
                                                  new ArrayList<Cluster>();
                    final List<Cluster> selectedClusters =
                                                  new ArrayList<Cluster>();
                    final List<String> clusterNames = new ArrayList<String>();
                    final Set<Cluster> clusters =
                         Tools.getConfigData().getClusters().getClusterSet();
                    for (final Cluster cluster : clusters) {
                        final JCheckBox cb = allCheckboxes.get(cluster);
                        if (cb.isSelected()) {
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
                             replaceAll("@CLUSTERS@", clustersString),
                         Tools.getString(
                             "EmptyBrowser.confirmRemoveMarkedClusters.Yes"),
                         Tools.getString(
                             "EmptyBrowser.confirmRemoveMarkedClusters.No"))) {
                        return;
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            removeMarkedClustersBtn.setEnabled(false);
                            loadMarkedClustersBtn.setEnabled(false);
                            unloadMarkedClustersBtn.setEnabled(false);
                        }
                    });
                    Tools.stopClusters(selectedRunningClusters);
                    Tools.removeClusters(selectedClusters);
                    final String saveFile = Tools.getConfigData().getSaveFile();
                    Tools.save(saveFile);
                    mainPanel.repaint();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            for (final Cluster cluster : selectedClusters) {
                                final JPanel p = clusterBackgrounds.get(
                                                                      cluster);
                                if (p != null) {
                                    clusterBackgrounds.remove(cluster);
                                    allCheckboxes.remove(cluster);
                                    allLoadButtons.remove(cluster);
                                    mainPanel.remove(p);
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
        public final JComponent getInfoPanel() {
            if (infoPanel != null) {
                return infoPanel;
            }
            infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

            infoPanel.setBackground(PANEL_BACKGROUND);
            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(3, 3, 0, 0);

            mainPanel.setBackground(PANEL_BACKGROUND);

            final Set<Cluster> clusters =
                    Tools.getConfigData().getClusters().getClusterSet();
            if (clusters != null) {
                final JPanel bPanel =
                               new JPanel(new BorderLayout());
                bPanel.setMaximumSize(new Dimension(10000, 60));
                final JPanel markedPanel = new JPanel(
                                            new FlowLayout(FlowLayout.LEFT));
                markedPanel.setBackground(STATUS_BACKGROUND);
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
                final JMenuBar mb = new JMenuBar();
                mb.setBackground(PANEL_BACKGROUND);
                final JMenu actionsMenu = getActionsMenu();
                updateMenus(null);
                mb.add(actionsMenu);
                bPanel.add(mb, BorderLayout.EAST);
                infoPanel.add(bPanel);
                for (final Cluster cluster : clusters) {
                    addClusterBox(cluster);
                }

                /* start marked clusters action listener */
                loadMarkedClustersBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread t = new Thread(new Runnable() {
                            public void run() {
                                loadMarkedClusters(clusters);
                            }
                        });
                        t.start();
                    }
                });

                /* stop marked clusters action listener */
                unloadMarkedClustersBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread t = new Thread(new Runnable() {
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
                    public void actionPerformed(final ActionEvent e) {
                        removeMarkedClusters();
                    }
                });

                /* mark checkbox item listeners */
                for (final Cluster cluster : clusters) {
                    addCheckboxListener(cluster);
                }
            }

            final JPanel mPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            mPanel.add(mainPanel);
            mPanel.setBackground(PANEL_BACKGROUND);
            final JScrollPane clustersPane =
                        new JScrollPane(
                                mPanel,
                                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            infoPanel.add(clustersPane);
            if (Tools.getConfigData().getAutoHosts().isEmpty()
                && !Tools.getConfigData().getAutoClusters().isEmpty()) {
                for (final Cluster cl : allLoadButtons.keySet()) {
                    if (Tools.getConfigData().getAutoClusters().contains(
                                                               cl.getName())) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                allLoadButtons.get(cl).pressButton();
                            }
                        });
                    }
                }
            }
            return infoPanel;
        }

        /**
         * adds one cluster box to the main cluster.
         */
        public void addClusterBox(final Cluster cluster) {
            final JPanel label = new JPanel();
            label.setLayout(new BoxLayout(label, BoxLayout.Y_AXIS));
            label.add(new JLabel(cluster.getName()));
            for (final Host host : cluster.getHosts()) {
                final JLabel nl = new JLabel("   " + host.getName());
                final Font font = nl.getFont();
                final Font newFont = font.deriveFont(
                                               Font.PLAIN,
                                               (float) (font.getSize() / 1.2));
                nl.setFont(newFont);
                label.add(nl);
            }
            final JPanel startPanel = new JPanel(new BorderLayout());
            startPanel.setBackground(Color.WHITE);
            clusterBackgrounds.put(cluster, startPanel);
            startPanel.setBorder(new LineBorder(Tools.getDefaultColor(
                                       "EmptyBrowser.StartPanelTitleBorder")));
            final JPanel left = new JPanel();
            final JCheckBox markCB = new JCheckBox();
            allCheckboxes.put(cluster, markCB);
            left.add(markCB);
            left.add(label);
            startPanel.add(left, BorderLayout.LINE_START);
            /* Load cluster button */
            final MyButton loadClusterBtn = new MyButton(
               Tools.getString("EmptyBrowser.LoadClusterButton"));
            loadClusterBtn.setEnabled(cluster.getClusterTab() == null);
            allLoadButtons.put(cluster, loadClusterBtn);
            loadClusterBtn.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    loadClusterBtn.setEnabled(false);
                    final Thread t = new Thread(new Runnable() {
                        public void run() {
                            List<Cluster> selectedClusters =
                                                     new ArrayList<Cluster>();
                            selectedClusters.add(cluster);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    clusterBackgrounds.get(cluster)
                                                   .setBackground(Color.GREEN);
                                    markCB.setSelected(false);
                                }
                            });
                            Tools.startClusters(selectedClusters);
                        }
                    });
                    t.start();
                }
            });
            startPanel.add(loadClusterBtn, BorderLayout.LINE_END);
            c.fill = GridBagConstraints.HORIZONTAL;
            mainPanel.add(startPanel, c);
            c.gridx++;
            if (c.gridx > 2) {
                c.gridx = 0;
                c.gridy++;
            }
        }

        /**
         * Sets this cluster as started. It is called after user enters a
         * cluster through the dialogs.
         */
        public void setAsStarted(final Cluster cluster) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    allLoadButtons.get(cluster).setEnabled(false);
                    clusterBackgrounds.get(cluster).setBackground(
                                                       Color.GREEN);
                }
            });
        }

        /**
         * Adds checkbox listener for this cluster's checkbox.
         */
        public void addCheckboxListener(final Cluster cluster) {
            final JCheckBox cb = allCheckboxes.get(cluster);
            cb.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            final Set<Cluster> clusters =
                                    Tools.getConfigData()
                                    .getClusters()
                                    .getClusterSet();

                            allCheckboxesListener(
                                            clusters,
                                            cb);
                        }
                    });
                    thread.start();
                }

            });
        }

        /**
         * Starts marked clusters.
         */
        private void loadMarkedClusters(final Set<Cluster> clusters) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    loadMarkedClustersBtn.setEnabled(false);
                }
            });
            final List<Cluster> selectedClusters = new ArrayList<Cluster>();
            for (final Cluster cluster : clusters) {
                if (cluster.getClusterTab() == null) {
                    final JCheckBox cb = allCheckboxes.get(cluster);
                    if (cb.isSelected()) {
                        selectedClusters.add(cluster);
                        setAsStarted(cluster);
                    } else if (cluster.getClusterTab() == null) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                allLoadButtons.get(cluster).setEnabled(true);
                            }
                        });
                    }
                }
            }
            Tools.startClusters(selectedClusters);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (final Cluster cluster : clusters) {
                        if (selectedClusters.contains(cluster)) {
                            allCheckboxes.get(cluster).setSelected(false);
                        }
                    }
                }
            });
        }

        /**
         * Stops marked clusters.
         */
        private void unloadMarkedClusters(final Set<Cluster> clusters) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    unloadMarkedClustersBtn.setEnabled(false);
                }
            });
            final List<Cluster> selectedClusters = new ArrayList<Cluster>();
            for (final Cluster cluster : clusters) {
                if (cluster.getClusterTab() != null) {
                    final JCheckBox cb = allCheckboxes.get(cluster);
                    if (cb.isSelected()) {
                        SwingUtilities.invokeLater(new Runnable() {
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

        /**
         * Listener for checkboxes that is called from thread.
         */
        private void allCheckboxesListener(final Set<Cluster> clusters,
                                           final JCheckBox cb) {
            int rc = 0;
            int nrc = 0;
            int ac = 0;
            for (final Cluster cluster : clusters) {
                ac++;
                if (allCheckboxes.get(cluster).isSelected()) {
                    if (cluster.getClusterTab() == null) {
                        /* not running */
                        nrc++;
                    } else {
                        rc++;
                    }
                }
            }
            final int runningCount = rc;
            final int notRunningCount = nrc;
            final int allCount = ac;
            if (cb.isSelected()) {
                /* disable all start cluster buttons */
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (notRunningCount >= 1) {
                            for (final Cluster cluster : clusters) {
                                allLoadButtons.get(cluster).setEnabled(false);
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
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (notRunningCount == 0) {
                            for (final Cluster cluster : clusters) {
                                if (cluster.getClusterTab() == null) {
                                    allLoadButtons.get(cluster).setEnabled(
                                                                        true);
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

        /**
         * Creates the popup for all hosts.
         */
        public final List<UpdatableItem> createPopup() {
            final List<UpdatableItem>items = new ArrayList<UpdatableItem>();

            /* host wizard */
            final MyMenuItem newHostWizardItem =
                new MyMenuItem(Tools.getString("EmptyBrowser.NewHostWizard"),
                               HOST_ICON,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return true;
                    }

                    public void action() {
                        final AddHostDialog dialog = new AddHostDialog();
                        dialog.showDialogs();
                    }
                };
            items.add(newHostWizardItem);
            registerMenuItem(newHostWizardItem);
            Tools.getGUIData().registerAddHostButton(newHostWizardItem);
            return items;
        }
    }
}
