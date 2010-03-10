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

package drbd.gui.resources;

import drbd.AddDrbdUpgradeDialog;
import drbd.EditHostDialog;
import drbd.gui.Browser;
import drbd.gui.HostBrowser;
import drbd.gui.DrbdGraph;
import drbd.gui.SpringUtilities;
import drbd.data.Host;
import drbd.data.Subtext;
import drbd.data.ConfigData;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.ExecCallback;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.DRBD;

import java.util.List;
import java.util.ArrayList;
import java.awt.Font;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.JColorChooser;

/**
 * This class holds info data for a host.
 * It shows host view, just like in the host tab.
 */
public class HostDrbdInfo extends Info {
    /** Host data. */
    private final Host host;
    /**
     * Prepares a new <code>HostDrbdInfo</code> object.
     */
    public HostDrbdInfo(final Host host, final Browser browser) {
        super(host.getName(), browser);
        this.host = host;
    }

    /**
     * Returns browser object of this info.
     */
    protected final HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /**
     * Returns a host icon for the menu.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return HostBrowser.HOST_ICON;
    }

    /**
     * Returns id, which is name of the host.
     */
    public final String getId() {
        return host.getName();
    }

    /**
     * Returns a host icon for the category in the menu.
     */
    public final ImageIcon getCategoryIcon() {
        return HostBrowser.HOST_ICON;
    }

    /**
     * Start upgrade drbd dialog.
     */
    public final void upgradeDrbd() {
        final AddDrbdUpgradeDialog adud = new AddDrbdUpgradeDialog(this);
        adud.showDialogs();
    }

    /**
     * Returns tooltip for the host.
     */
    public final String getToolTipForGraph(final boolean testOnly) {
        return getBrowser().getHostToolTip(host);
    }

    /**
     * Returns info panel.
     *
     * @return info panel
     */
    public final JComponent getInfoPanel() {
        Tools.getGUIData().setTerminalPanel(host.getTerminalPanel());
        final Font f = new Font("Monospaced", Font.PLAIN, 12);
        final JTextArea ta = new JTextArea();
        ta.setFont(f);

        final ExecCallback execCallback =
            new ExecCallback() {
                public void done(final String ans) {
                    ta.setText(ans);
                }

                public void doneError(final String ans,
                                      final int exitCode) {
                    ta.setText("error");
                    Tools.sshError(host, "", ans, exitCode);
                }

            };
        // TODO: disable buttons if disconnected?
        final MyButton procDrbdButton = new MyButton("/proc/drbd");
        procDrbdButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                host.execCommand("DRBD.getProcDrbd",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 true); /* outputVisible */
            }
        });
        host.registerEnableOnConnect(procDrbdButton);
        final MyButton drbdProcsButton = new MyButton("DRBD Processes");
        drbdProcsButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                host.execCommand("DRBD.getProcesses",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 true); /* outputVisible */
            }
        });
        host.registerEnableOnConnect(drbdProcsButton);

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(HostBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        mainPanel.add(buttonPanel);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(HostBrowser.PANEL_BACKGROUND);
        final JMenu serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);
        final JPanel p = new JPanel(new SpringLayout());
        p.setBackground(HostBrowser.STATUS_BACKGROUND);

        p.add(procDrbdButton);
        p.add(drbdProcsButton);
        SpringUtilities.makeCompactGrid(p, 2, 1,  // rows, cols
                                           1, 1,  // initX, initY
                                           1, 1); // xPad, yPad
        mainPanel.setMinimumSize(new Dimension(
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")));
        mainPanel.setPreferredSize(new Dimension(
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")));
        buttonPanel.add(p);
        mainPanel.add(new JScrollPane(ta));
        return mainPanel;
    }

    /**
     * Gets host.
     *
     * @return host of this info
     */
    public final Host getHost() {
        return host;
    }


    /**
     * Compares this host info name with specified hostdrbdinfo's name.
     *
     * @param otherHI
     *              other host info
     * @return true if they are equal
     */
    public final boolean equals(final HostDrbdInfo otherHI) {
        if (otherHI == null) {
            return false;
        }
        return otherHI.toString().equals(host.getName());
    }

    /**
     * Returns string representation of the host. It's same as name.
     */
    public final String toString() {
        return host.getName();
    }

    /**
     * Returns name of the host.
     */
    public final String getName() {
        return host.getName();
    }

    /**
     * Creates the popup for the host.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem>items = new ArrayList<UpdatableItem>();

        /* host wizard */
        final MyMenuItem hostWizardItem =
            new MyMenuItem(Tools.getString("HostBrowser.HostWizard"),
                           HostBrowser.HOST_ICON_LARGE,
                           null,
                           ConfigData.AccessType.RO,
                           ConfigData.AccessType.RO) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return true;
                }

                public void action() {
                    final EditHostDialog dialog = new EditHostDialog(host);
                    dialog.showDialogs();
                }
            };
        items.add(hostWizardItem);
        registerMenuItem(hostWizardItem);
        Tools.getGUIData().registerAddHostButton(hostWizardItem);
        final boolean testOnly = false;
        /* load drbd */
        final MyMenuItem loadItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.LoadDrbd"),
                           null,
                           null,
                           ConfigData.AccessType.OP1,
                           ConfigData.AccessType.OP1) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().isConnected()
                           && !getHost().isDrbdStatus();
                }

                public void action() {
                    DRBD.load(getHost(), testOnly);
                }
            };
        items.add(loadItem);
        registerMenuItem(loadItem);

        /* start drbd */
        final MyMenuItem upAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.UpAll"),
                           null,
                           null,
                           ConfigData.AccessType.ADMIN1,
                           ConfigData.AccessType.ADMIN1) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().isDrbdStatus();
                }

                public void action() {
                    for (final BlockDevInfo bdi
                                         : getBrowser().getBlockDevInfos()) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.isConnected(testOnly)
                            && !bdi.getBlockDevice().isAttached()) {
                            bdi.drbdUp(testOnly);
                        }
                    }
                }
            };
        items.add(upAllItem);
        registerMenuItem(upAllItem);

        /* upgrade drbd */
        final MyMenuItem upgradeDrbdItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.UpgradeDrbd"),
                           null,
                           null,
                           ConfigData.AccessType.GOD, // TODO: does not work yet
                           ConfigData.AccessType.ADMIN1) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().isConnected();
                }

                public void action() {
                    upgradeDrbd();
                }
            };
        items.add(upgradeDrbdItem);
        registerMenuItem(upgradeDrbdItem);

        /* change host color */
        final MyMenuItem changeHostColorItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                           null,
                           null,
                           ConfigData.AccessType.RO,
                           ConfigData.AccessType.RO) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return true;
                }

                public void action() {
                    final JColorChooser tcc = new JColorChooser();
                    Color newColor = tcc.showDialog(
                                            Tools.getGUIData().getMainFrame(),
                                            "asdf",
                                            host.getPmColors()[0]);
                    if (newColor != null) {
                        host.setColor(newColor);
                    }
                }
            };
        items.add(changeHostColorItem);
        registerMenuItem(changeHostColorItem);

        /* view logs */
        final MyMenuItem viewLogsItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           null,
                           null,
                           ConfigData.AccessType.RO,
                           ConfigData.AccessType.RO) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().isConnected();
                }

                public void action() {
                    drbd.gui.dialog.drbd.DrbdsLog l =
                                      new drbd.gui.dialog.drbd.DrbdsLog(host);
                    l.showDialog();
                }
            };
        items.add(viewLogsItem);
        registerMenuItem(viewLogsItem);

        /* connect all */
        final MyMenuItem connectAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ConnectAll"),
                           null,
                           null,
                           ConfigData.AccessType.OP1,
                           ConfigData.AccessType.OP1) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().isDrbdStatus();
                }

                public void action() {
                    for (final BlockDevInfo bdi
                                         : getBrowser().getBlockDevInfos()) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.isConnectedOrWF(testOnly)) {
                            bdi.connect(testOnly);
                        }
                    }
                }
            };
        items.add(connectAllItem);
        registerMenuItem(connectAllItem);

        /* disconnect all */
        final MyMenuItem disconnectAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.DisconnectAll"),
                           null,
                           null,
                           ConfigData.AccessType.ADMIN1,
                           ConfigData.AccessType.OP1) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().isDrbdStatus();
                }

                public void action() {
                    for (final BlockDevInfo bdi
                                          : getBrowser().getBlockDevInfos()) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.isConnectedOrWF(testOnly)) {
                            bdi.disconnect(testOnly);
                        }
                    }
                }
            };
        items.add(disconnectAllItem);
        registerMenuItem(disconnectAllItem);

        /* attach dettached */
        final MyMenuItem attachAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.AttachAll"),
                           null,
                           null,
                           ConfigData.AccessType.ADMIN1,
                           ConfigData.AccessType.OP1) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().isDrbdStatus();
                }

                public void action() {
                    for (final BlockDevInfo bdi
                                           : getBrowser().getBlockDevInfos()) {
                        if (bdi.getBlockDevice().isDrbd()
                            && !bdi.getBlockDevice().isAttached()) {
                            bdi.attach(testOnly);
                        }
                    }
                }
            };
        items.add(attachAllItem);
        registerMenuItem(attachAllItem);

        /* set all primary */
        final MyMenuItem setAllPrimaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetAllPrimary"),
                                           null,
                                           null,
                                           ConfigData.AccessType.ADMIN1,
                                           ConfigData.AccessType.OP1) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().isDrbdStatus();
                }

                public void action() {
                    for (final BlockDevInfo bdi
                                          : getBrowser().getBlockDevInfos()) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.getBlockDevice().isSecondary()) {
                            bdi.setPrimary(testOnly);
                        }
                    }
                }
            };
        items.add(setAllPrimaryItem);
        registerMenuItem(setAllPrimaryItem);

        /* set all secondary */
        final MyMenuItem setAllSecondaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetAllSecondary"),
                           null,
                           null,
                           ConfigData.AccessType.ADMIN1,
                           ConfigData.AccessType.ADMIN1) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().isDrbdStatus();
                }

                public void action() {
                    for (final BlockDevInfo bdi
                                        : getBrowser().getBlockDevInfos()) {
                        if (bdi.getBlockDevice().isDrbd()
                            && bdi.getBlockDevice().isPrimary()) {
                            bdi.setSecondary(testOnly);
                        }
                    }
                }
            };
        items.add(setAllSecondaryItem);
        registerMenuItem(setAllSecondaryItem);

        /* remove host from gui */
        final MyMenuItem removeHostItem =
            new MyMenuItem(Tools.getString("HostBrowser.RemoveHost"),
                           HostBrowser.HOST_REMOVE_ICON,
                           null,
                           ConfigData.AccessType.RO,
                           ConfigData.AccessType.RO) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().getCluster() == null;
                }

                public void action() {
                    getHost().disconnect();
                    Tools.getConfigData().removeHostFromHosts(getHost());
                    Tools.getGUIData().allHostsUpdate();
                }
            };
        registerMenuItem(removeHostItem);
        items.add(removeHostItem);

        /* expert options */
        final MyMenu hostExpertSubmenu = new MyMenu(
                                Tools.getString("HostBrowser.ExpertSubmenu"),
                                ConfigData.AccessType.OP1,
                                ConfigData.AccessType.OP1) {
            private static final long serialVersionUID = 1L;
            public boolean enablePredicate() {
                return host.isConnected();
            }

            public void update() {
                super.update();
                getBrowser().addExpertMenu(this);
            }
        };
        items.add(hostExpertSubmenu);
        registerMenuItem(hostExpertSubmenu);

        return items;
    }

    /**
     * Returns grahical view if there is any.
     */
    public final JPanel getGraphicalView() {
        final DrbdGraph dg = getBrowser().getDrbdGraph();
        if (dg == null) {
            return null;
        }
        dg.getDrbdInfo().setSelectedNode(null);
        return dg.getDrbdInfo().getGraphicalView();
    }
    /**
     * Returns how much of this is used.
     */
    public final int getUsed() {
        // TODO: maybe the load?
        return -1;
    }

    /**
     * Returns subtexts that appears in the host vertex in the cluster
     * graph.
     */
    public final Subtext[] getSubtextsForGraph() {
        final List<Subtext> texts = new ArrayList<Subtext>();
        if (getHost().isConnected()) {
            if (!getHost().isClStatus()) {
               texts.add(new Subtext("waiting for cluster status...", null));
            }
        } else {
            texts.add(new Subtext("connecting...", null));
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /**
     * Returns subtexts that appears in the host vertex in the drbd graph.
     */
    public final Subtext[] getSubtextsForDrbdGraph(final boolean testOnly) {
        final List<Subtext> texts = new ArrayList<Subtext>();
        if (getHost().isConnected()) {
            if (!getHost().isDrbdLoaded()) {
               texts.add(new Subtext("DRBD not loaded", null));
            } else if (!getHost().isDrbdStatus()) {
               texts.add(new Subtext("waiting...", null));
            }
        } else {
            texts.add(new Subtext("connecting...", null));
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /**
     * Returns text that appears above the icon in the drbd graph.
     */
    public final String getIconTextForDrbdGraph(final boolean testOnly) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Drbd.NoInfoAvailable");
        }
        return null;
    }

    /**
     * Returns text that appears in the corner of the drbd graph.
     */
    public final Subtext getRightCornerTextForDrbdGraph(
                                                 final boolean testOnly) {
        return null;
    }
}
