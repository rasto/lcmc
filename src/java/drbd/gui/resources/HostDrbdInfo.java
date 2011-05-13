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
import drbd.data.AccessMode;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.ExecCallback;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.DRBD;
import drbd.utilities.SSH;

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
public final class HostDrbdInfo extends Info {
    /** Host data. */
    private final Host host;
    /** String that is displayed as a tool tip for disabled menu item. */
    private static final String NO_DRBD_STATUS_STRING =
                                                "drbd status is not available";
    /** Prepares a new <code>HostDrbdInfo</code> object. */
    public HostDrbdInfo(final Host host, final Browser browser) {
        super(host.getName(), browser);
        this.host = host;
    }

    /** Returns browser object of this info. */
    @Override protected HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /** Returns a host icon for the menu. */
    @Override public ImageIcon getMenuIcon(final boolean testOnly) {
        return HostBrowser.HOST_ICON;
    }

    /** Returns id, which is name of the host. */
    @Override public String getId() {
        return host.getName();
    }

    /** Returns a host icon for the category in the menu. */
    @Override public ImageIcon getCategoryIcon(final boolean testOnly) {
        return HostBrowser.HOST_ICON;
    }

    /** Start upgrade drbd dialog. */
    void upgradeDrbd() {
        final AddDrbdUpgradeDialog adud = new AddDrbdUpgradeDialog(this);
        adud.showDialogs();
    }

    /** Returns tooltip for the host. */
    @Override public String getToolTipForGraph(final boolean testOnly) {
        return getBrowser().getHostToolTip(host);
    }

    /** Returns the info panel. */
    @Override public JComponent getInfoPanel() {
        final Font f = new Font("Monospaced", Font.PLAIN, 12);
        final JTextArea ta = new JTextArea();
        ta.setFont(f);

        final String stacktrace = Tools.getStackTrace();
        final ExecCallback execCallback =
            new ExecCallback() {
                @Override public void done(final String ans) {
                    ta.setText(ans);
                }

                @Override public void doneError(final String ans,
                                                final int exitCode) {
                    ta.setText("error");
                    Tools.sshError(host, "", ans, stacktrace, exitCode);
                }

            };
        // TODO: disable buttons if disconnected?
        final MyButton procDrbdButton = new MyButton("/proc/drbd");
        procDrbdButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                host.execCommand("DRBD.getProcDrbd",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 true,  /* outputVisible */
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
            }
        });
        host.registerEnableOnConnect(procDrbdButton);
        final MyButton drbdProcsButton = new MyButton("DRBD Processes");
        drbdProcsButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                host.execCommand("DRBD.getProcesses",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 true,  /* outputVisible */
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
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

    /** Returns host. */
    public Host getHost() {
        return host;
    }


    /**
     * Compares this host info name with specified hostdrbdinfo's name.
     *
     * @param otherHI
     *              other host info
     * @return true if they are equal
     */
    boolean equals(final HostDrbdInfo otherHI) {
        if (otherHI == null) {
            return false;
        }
        return otherHI.toString().equals(host.getName());
    }

    /** Returns string representation of the host. It's same as name. */
    @Override public String toString() {
        return host.getName();
    }

    /** Returns name of the host. */
    @Override public String getName() {
        return host.getName();
    }

    /** Creates the popup for the host. */
    @Override public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        /* host wizard */
        final MyMenuItem hostWizardItem =
            new MyMenuItem(Tools.getString("HostBrowser.HostWizard"),
                           HostBrowser.HOST_ICON_LARGE,
                           Tools.getString("HostBrowser.HostWizard"),
                           new AccessMode(ConfigData.AccessType.RO,
                                          false),
                           new AccessMode(ConfigData.AccessType.RO,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    return null;
                }

                @Override public void action() {
                    final EditHostDialog dialog = new EditHostDialog(host);
                    dialog.showDialogs();
                }
            };
        items.add(hostWizardItem);
        Tools.getGUIData().registerAddHostButton(hostWizardItem);
        final boolean testOnly = false;
        /* load drbd */
        final MyMenuItem loadItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.LoadDrbd"),
                           null,
                           Tools.getString("HostBrowser.Drbd.LoadDrbd"),
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (getHost().isConnected()) {
                        if (getHost().isDrbdStatus()) {
                            return "already loaded";
                        } else {
                            return null;
                        }
                    } else {
                        return Host.NOT_CONNECTED_STRING;
                    }
                }

                @Override public void action() {
                    DRBD.load(getHost(), testOnly);
                    getBrowser().getClusterBrowser().updateHWInfo(host);
                }
            };
        items.add(loadItem);

        /* start drbd */
        final MyMenuItem upAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.UpAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.UpAll"),
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (!getHost().isDrbdStatus()) {
                        return NO_DRBD_STATUS_STRING;
                    }
                    return null;
                }

                @Override public void action() {
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

        /* upgrade drbd */
        final MyMenuItem upgradeDrbdItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.UpgradeDrbd"),
                           null,
                           Tools.getString("HostBrowser.Drbd.UpgradeDrbd"),
                           new AccessMode(ConfigData.AccessType.GOD,
                                          false), // TODO: does not work yet
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (!getHost().isConnected()) {
                        return Host.NOT_CONNECTED_STRING;
                    }
                    return null;
                }

                @Override public void action() {
                    upgradeDrbd();
                }
            };
        items.add(upgradeDrbdItem);

        /* change host color */
        final MyMenuItem changeHostColorItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                           null,
                           Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    return null;
                }

                @Override public void action() {
                    Color newColor = JColorChooser.showDialog(
                                            Tools.getGUIData().getMainFrame(),
                                            "Choose " + host.getName()
                                            + " color",
                                            host.getPmColors()[0]);
                    if (newColor != null) {
                        host.setSavedColor(newColor);
                    }
                }
            };
        items.add(changeHostColorItem);

        /* view logs */
        final MyMenuItem viewLogsItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           LOGFILE_ICON,
                           Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (!getHost().isConnected()) {
                        return Host.NOT_CONNECTED_STRING;
                    }
                    return null;
                }

                @Override public void action() {
                    drbd.gui.dialog.drbd.DrbdsLog l =
                                      new drbd.gui.dialog.drbd.DrbdsLog(host);
                    l.showDialog();
                }
            };
        items.add(viewLogsItem);

        /* connect all */
        final MyMenuItem connectAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ConnectAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.ConnectAll"),
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override public void action() {
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

        /* disconnect all */
        final MyMenuItem disconnectAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.DisconnectAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.DisconnectAll"),
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override public void action() {
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

        /* attach dettached */
        final MyMenuItem attachAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.AttachAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.AttachAll"),
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override public void action() {
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

        /* set all primary */
        final MyMenuItem setAllPrimaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetAllPrimary"),
                           null,
                           Tools.getString("HostBrowser.Drbd.SetAllPrimary"),
                           new AccessMode(
                                   ConfigData.AccessType.ADMIN,
                                   false),
                           new AccessMode(
                                   ConfigData.AccessType.OP,
                                   false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override public void action() {
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

        /* set all secondary */
        final MyMenuItem setAllSecondaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetAllSecondary"),
                           null,
                           Tools.getString("HostBrowser.Drbd.SetAllSecondary"),
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override public void action() {
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

        /* remove host from gui */
        final MyMenuItem removeHostItem =
            new MyMenuItem(Tools.getString("HostBrowser.RemoveHost"),
                           HostBrowser.HOST_REMOVE_ICON,
                           Tools.getString("HostBrowser.RemoveHost"),
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (getHost().getCluster() != null) {
                        return "it is a member of a cluster";
                    }
                    return null;
                }

                @Override public void action() {
                    getHost().disconnect();
                    Tools.getConfigData().removeHostFromHosts(getHost());
                    Tools.getGUIData().allHostsUpdate();
                }
            };
        items.add(removeHostItem);

        /* advanced options */
        final MyMenu hostAdvancedSubmenu = new MyMenu(
                                Tools.getString("HostBrowser.AdvancedSubmenu"),
                                new AccessMode(ConfigData.AccessType.OP, false),
                                new AccessMode(ConfigData.AccessType.OP,
                                               false)) {
            private static final long serialVersionUID = 1L;
            @Override public String enablePredicate() {
                if (!host.isConnected()) {
                    return Host.NOT_CONNECTED_STRING;
                }
                return null;
            }

            @Override public void update() {
                super.update();
                getBrowser().addAdvancedMenu(this);
            }
        };
        items.add(hostAdvancedSubmenu);

        return items;
    }

    /** Returns grahical view if there is any. */
    @Override public JPanel getGraphicalView() {
        final DrbdGraph dg = getBrowser().getDrbdGraph();
        if (dg == null) {
            return null;
        }
        dg.getDrbdInfo().setSelectedNode(null);
        return dg.getDrbdInfo().getGraphicalView();
    }

    /** Returns how much of this is used. */
    public int getUsed() {
        // TODO: maybe the load?
        return -1;
    }

    /**
     * Returns subtexts that appears in the host vertex in the cluster
     * graph.
     */
    Subtext[] getSubtextsForGraph() {
        final List<Subtext> texts = new ArrayList<Subtext>();
        if (getHost().isConnected()) {
            if (!getHost().isClStatus()) {
               texts.add(new Subtext("waiting for cluster status...",
                                     null,
                                     Color.BLACK));
            }
        } else {
            texts.add(new Subtext("connecting...", null, Color.BLACK));
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /** Returns subtexts that appears in the host vertex in the drbd graph. */
    public Subtext[] getSubtextsForDrbdGraph(final boolean testOnly) {
        final List<Subtext> texts = new ArrayList<Subtext>();
        if (getHost().isConnected()) {
            if (!getHost().isDrbdLoaded()) {
               texts.add(new Subtext("DRBD not loaded", null, Color.BLACK));
            } else if (!getHost().isDrbdStatus()) {
               texts.add(new Subtext("waiting...", null, Color.BLACK));
            }
        } else {
            texts.add(new Subtext("connecting...", null, Color.BLACK));
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /** Returns text that appears above the icon in the drbd graph. */
    public String getIconTextForDrbdGraph(final boolean testOnly) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Drbd.NoInfoAvailable");
        }
        return null;
    }

    /** Returns text that appears in the corner of the drbd graph. */
    public Subtext getRightCornerTextForDrbdGraph(final boolean testOnly) {
        return null;
    }
}
