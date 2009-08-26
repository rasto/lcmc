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
    private final AllHostsInfo allHostsInfo = new AllHostsInfo();

    /**
     * Prepares a new <code>CusterBrowser</code> object.
     */
    public EmptyBrowser() {
        super();
        /* Load the default file */
        final String saveFile = Tools.getConfigData().getSaveFile();
        final String xml = Tools.loadSaveFile(saveFile, false);
        if (xml != null) {
            Tools.loadXML(xml);
        }
        setTreeTop();
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

        /**
         * Creates a new AllHostsInfo instance.
         */
        public AllHostsInfo() {
            super(Tools.getString("ClusterBrowser.AllHosts"));
            host = null;
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
            final JPanel mainPanel = new JPanel(new GridBagLayout());
            final GridBagConstraints c = new GridBagConstraints();
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
                final MyButton startMarkedClustersBtn = new MyButton(
                      Tools.getString("EmptyBrowser.StartMarkedClusters"),
                      CLUSTER_ICON,
                      Tools.getString(
                                "EmptyBrowser.StartMarkedClusters.ToolTip"));
                startMarkedClustersBtn.setEnabled(false);
                markedPanel.add(startMarkedClustersBtn);

                /* stop marked clusters */
                final MyButton stopMarkedClustersBtn = new MyButton(
                      Tools.getString("EmptyBrowser.StopMarkedClusters"),
                      CLUSTER_ICON,
                      Tools.getString(
                                   "EmptyBrowser.StopMarkedClusters.ToolTip"));
                stopMarkedClustersBtn.setEnabled(false);
                markedPanel.add(stopMarkedClustersBtn);
                /* remove marked clusters */
                final MyButton removeMarkedClustersBtn = new MyButton(
                      Tools.getString("EmptyBrowser.RemoveMarkedClusters"),
                      CLUSTER_ICON,
                      Tools.getString(
                                "EmptyBrowser.RemoveMarkedClusters.ToolTip"));
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
                final Map<Cluster, JCheckBox> allCheckboxes =
                                        new HashMap<Cluster, JCheckBox>();
                final Map<Cluster, MyButton> allStartButtons =
                                        new HashMap<Cluster, MyButton>();
                final Map<Cluster, JPanel> clusterBackgrounds =
                                        new HashMap<Cluster, JPanel>();
                for (final Cluster cluster : clusters) {
                    addClusterPanel(cluster,
                                    mainPanel,
                                    clusterBackgrounds,
                                    c,
                                    allCheckboxes,
                                    allStartButtons);
                }

                /* start marked clusters action listener */
                startMarkedClustersBtn.addActionListener(
                                                    new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread t = new Thread(new Runnable() {
                            public void run() {
                                startMarkedClusters(clusters,
                                                    startMarkedClustersBtn,
                                                    clusterBackgrounds,
                                                    allCheckboxes,
                                                    allStartButtons);
                            }
                        });
                        t.start();
                    }
                });

                /* stop marked clusters action listener */
                stopMarkedClustersBtn.addActionListener(
                                                    new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread t = new Thread(new Runnable() {
                            public void run() {
                                stopMarkedClusters(clusters,
                                                   stopMarkedClustersBtn,
                                                   clusterBackgrounds,
                                                   allCheckboxes);
                            }
                        });
                        t.start();
                    }
                });

                /* remove marked clusters action listener */
                removeMarkedClustersBtn.addActionListener(
                                                    new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread t = new Thread(new Runnable() {
                            public void run() {
                                // TODO: are you sure? dialog
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        removeMarkedClustersBtn.setEnabled(
                                                                     false);
                                    }
                                });
                                final List<Cluster> selectedRunningClusters =
                                                  new ArrayList<Cluster>();
                                final List<Cluster> selectedClusters =
                                                  new ArrayList<Cluster>();
                                for (final Cluster cluster : clusters) {
                                    selectedClusters.add(cluster);
                                    if (cluster.getClusterTab() != null) {
                                        final JCheckBox cb =
                                            allCheckboxes.get(cluster);
                                        if (cb.isSelected()) {
                                            SwingUtilities.invokeLater(
                                                new Runnable() {
                                                    public void run() {
                                                        clusterBackgrounds.get(
                                                         cluster).setBackground(
                                                                   Color.WHITE);
                                                    }
                                                });
                                            selectedRunningClusters.add(
                                                                       cluster);
                                        }
                                    }
                                }
                                Tools.stopClusters(selectedRunningClusters);
                                Tools.removeClusters(selectedClusters);
                            }
                        });
                        t.start();
                    }
                });

                /* mark checkbox item listeners */
                for (final Cluster cluster : clusters) {
                    final JCheckBox cb = allCheckboxes.get(cluster);
                    cb.addItemListener(new ItemListener() {
                        public void itemStateChanged(final ItemEvent e) {
                            final Thread thread = new Thread(new Runnable() {
                                public void run() {
                                    allCheckboxesListener(
                                                    clusters,
                                                    allCheckboxes,
                                                    allStartButtons,
                                                    startMarkedClustersBtn,
                                                    stopMarkedClustersBtn,
                                                    removeMarkedClustersBtn,
                                                    cb);
                                }
                            });
                            thread.start();
                        }

                    });
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
            return infoPanel;
        }

        /**
         * adds one cluster panel to the main cluster.
         */
        private void addClusterPanel(
                             final Cluster cluster,
                             final JPanel mainPanel,
                             final Map<Cluster, JPanel> clusterBackgrounds,
                             final GridBagConstraints c,
                             final Map<Cluster, JCheckBox> allCheckboxes,
                             final Map<Cluster, MyButton> allStartButtons) {
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
            /* Start Cluster Button */
            final MyButton startClusterBtn = new MyButton(
               Tools.getString("EmptyBrowser.StartClusterButton"));
            startClusterBtn.setEnabled(cluster.getClusterTab() == null);
            allStartButtons.put(cluster, startClusterBtn);
            startClusterBtn.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    startClusterBtn.setEnabled(false);
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
            startPanel.add(startClusterBtn, BorderLayout.LINE_END);
            c.fill = GridBagConstraints.HORIZONTAL;
            mainPanel.add(startPanel, c);
            c.gridx++;
            if (c.gridx > 2) {
                c.gridx = 0;
                c.gridy++;
            }
        }

        /**
         * Starts marked clusters.
         */
        private void startMarkedClusters(
                             final Set<Cluster> clusters,
                             final MyButton startMarkedClustersBtn,
                             final Map<Cluster, JPanel> clusterBackgrounds,
                             final Map<Cluster, JCheckBox> allCheckboxes,
                             final Map<Cluster, MyButton> allStartButtons) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    startMarkedClustersBtn.setEnabled(false);
                }
            });
            final List<Cluster> selectedClusters = new ArrayList<Cluster>();
            for (final Cluster cluster : clusters) {
                if (cluster.getClusterTab() == null) {
                    final JCheckBox cb = allCheckboxes.get(cluster);
                    if (cb.isSelected()) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                allStartButtons.get(cluster).setEnabled(false);
                                clusterBackgrounds.get(cluster).setBackground(
                                                                   Color.GREEN);
                                allCheckboxes.get(cluster).setSelected(false);
                            }
                        });
                        selectedClusters.add(cluster);
                    } else if (cluster.getClusterTab() == null) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                allStartButtons.get(cluster).setEnabled(true);
                            }
                        });
                    }
                }
            }
            Tools.startClusters(selectedClusters);
        }

        /**
         * Stops marked clusters.
         */
        private void stopMarkedClusters(
                             final Set<Cluster> clusters,
                             final MyButton stopMarkedClustersBtn,
                             final Map<Cluster, JPanel> clusterBackgrounds,
                             final Map<Cluster, JCheckBox> allCheckboxes) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    stopMarkedClustersBtn.setEnabled(false);
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
        private void allCheckboxesListener(
                                 final Set<Cluster> clusters,
                                 final Map<Cluster, JCheckBox> allCheckboxes,
                                 final Map<Cluster, MyButton> allStartButtons,
                                 final MyButton startMarkedClustersBtn,
                                 final MyButton stopMarkedClustersBtn,
                                 final MyButton removeMarkedClustersBtn,
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
                                allStartButtons.get(cluster).setEnabled(
                                                             false);
                            }
                            /* enable start etc marked clusters button */
                            startMarkedClustersBtn.setEnabled(
                                                    runningCount == 0);
                        }
                        if (runningCount >= 1) {
                            stopMarkedClustersBtn.setEnabled(
                                                     notRunningCount == 0);
                        }
                        //TODO: still not working
                        //removeMarkedClustersBtn.setEnabled(true);
                    }
                });
            } else {
                /* deselecting */
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (notRunningCount == 0) {
                            for (final Cluster cluster : clusters) {
                                if (cluster.getClusterTab() == null) {
                                    allStartButtons.get(cluster).setEnabled(
                                                        true);
                                }
                            }
                            startMarkedClustersBtn.setEnabled(false);
                            if (runningCount > 0) {
                                stopMarkedClustersBtn.setEnabled(true);
                            }
                        }
                        if (runningCount == 0) {
                            stopMarkedClustersBtn.setEnabled(false);
                            if (notRunningCount > 0) {
                                startMarkedClustersBtn.setEnabled(true);
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
