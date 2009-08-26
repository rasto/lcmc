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

import drbd.data.Host;
import drbd.data.Cluster;
import drbd.utilities.Tools;
import drbd.utilities.ExecCallback;
import drbd.utilities.SSH.ExecCommandThread;
import drbd.gui.ProgressBar;
import drbd.gui.GuiComboBox;

import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * An implementation of a dialog where user can choose cluster stack, that can
 * be Corosync or Heartbeat.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ClusterCommStack extends DialogCluster {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Radio Combo box. */
    private GuiComboBox chooseStackCombo;
    /** Name of the Corosync in the radio group. */
    private static final String COROSYNC_NAME =
                                        Tools.getConfigData().COROSYNC_NAME;
    /** Name of the Heartbeat in the radio group. */
    private static final String HEARTBEAT_NAME =
                                        Tools.getConfigData().HEARTBEAT_NAME;

    //TODO: progressbar
    /**
     * Prepares a new <code>ClusterCommStack</code> object.
     */
    public ClusterCommStack(final WizardDialog previousDialog,
                        final Cluster cluster) {
        super(previousDialog, cluster);
    }

    /**
     * Returns the next dialog.
     */
    public final WizardDialog nextDialog() {
        if (HEARTBEAT_NAME.equals(chooseStackCombo.getValue())) {
            Tools.getConfigData().setLastInstalledClusterStack(HEARTBEAT_NAME);
            return new ClusterHbConfig(this, getCluster());
        } else {
            Tools.getConfigData().setLastInstalledClusterStack(COROSYNC_NAME);
            return new ClusterAisConfig(this, getCluster());
        }
    }

    /**
     * Returns the title of the dialog.
     */
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.ClusterCommStack.Title");
    }

    /**
     * Returns the description of the dialog.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.ClusterCommStack.Description");
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{});

        final Host[] hosts = getCluster().getHostsArray();
        final ExecCommandThread[] infoThreads =
                                        new ExecCommandThread[hosts.length];
        int i = 0;
        for (final Host host : hosts) {
            //infoThreads[i] = h.execCommand("ClusterInit.getInstallationInfo",
            infoThreads[i] = host.execCommand("ClusterInit.getInstallationInfo",
                             (ProgressBar) null,
                             new ExecCallback() {
                                 public void done(final String ans) {
                                     //drbdLoaded[index] = true;
                                     for (final String line
                                                    : ans.split("\\r?\\n")) {
                                         host.parseInstallationInfo(line);
                                     }
                                 }
                                 public void doneError(final String ans,
                                                       final int exitCode) {
                                     Tools.appWarning(
                                                "could not get install info");
                                 }
                             },
                             null,   /* ConvertCmdCallback */
                             false); /* outputVisible */
            i++;
        }
        for (final ExecCommandThread t : infoThreads) {
            /* wait for all of them */
            try {
                t.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        boolean aisIsPossible = true;
        boolean hbIsPossible = true;
        for (final Host host : hosts) {
            if (host.getCorosyncVersion() == null
                && host.getOpenaisVersion() == null) {
                aisIsPossible = false;
            }
            if (host.getHeartbeatVersion() == null) {
                hbIsPossible = false;
            }
        }
        if (!aisIsPossible && hbIsPossible) {
            chooseStackCombo.setValue(HEARTBEAT_NAME);
        }
        final boolean ais = aisIsPossible;
        final boolean hb = hbIsPossible;
        if (ais || hb) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (ais) {
                        chooseStackCombo.setEnabled(COROSYNC_NAME, true);
                    }
                    if (hb) {
                        chooseStackCombo.setEnabled(HEARTBEAT_NAME, true);
                    }
                }
            });
        }
        enableComponents();
        if (ais || hb) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    buttonClass(nextButton()).setEnabled(true);
                    requestFocusLater(buttonClass(nextButton()));
                }
            });
        }
    }

    /**
     * Returns the panel with radio boxes.
     */
    protected final JComponent getInputPane() {
        final JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEADING, 1, 1));
        final Host[] hosts = getCluster().getHostsArray();
        boolean hbImpossible = false;
        boolean aisImpossible = false;
        int aisIsRc = 0;
        int hbIsRc = 0;
        int aisIsRunning = 0;
        int hbIsRunning = 0;
        for (final Host host : hosts) {
            if (host.getHeartbeatVersion() == null) {
                hbImpossible = true;
            }
            if (host.getCorosyncVersion() == null
                && host.getOpenaisVersion() == null) {
                aisImpossible = true;
            }
            if (host.isCsAisRc()) {
                aisIsRc++;
            }
            if (host.isHeartbeatRc()) {
                hbIsRc++;
            }
            if (host.isCsAisRunning()) {
                aisIsRunning++;
            }
            if (host.isHeartbeatRunning()) {
                hbIsRunning++;
            }
        }
        /* slight preference to corosync */
        String defaultValue = null;
        if (hbImpossible) {
            defaultValue = COROSYNC_NAME;
        } else if (aisImpossible) {
            defaultValue = HEARTBEAT_NAME;
        } else if (aisIsRc < hbIsRc) {
            defaultValue = HEARTBEAT_NAME;
        } else if (aisIsRc > hbIsRc) {
            defaultValue = COROSYNC_NAME;
        } else if (aisIsRunning < hbIsRunning) {
            defaultValue = HEARTBEAT_NAME;
        } else if (aisIsRunning > hbIsRunning) {
            defaultValue = COROSYNC_NAME;
        } else {
            defaultValue = Tools.getConfigData().getLastInstalledClusterStack();
        }
        if (defaultValue == null) {
            defaultValue = COROSYNC_NAME;
        }
        chooseStackCombo = new GuiComboBox(defaultValue,
                                           new String[]{HEARTBEAT_NAME,
                                                        COROSYNC_NAME},
                                           null,
                                           GuiComboBox.Type.RADIOGROUP,
                                           null,
                                           500);
        chooseStackCombo.setEnabled(COROSYNC_NAME, false);
        chooseStackCombo.setEnabled(HEARTBEAT_NAME, false);
        chooseStackCombo.setBackground(Color.WHITE); // TODO: does not work
        p1.add(chooseStackCombo);
        return p1;
    }

    /**
     * Enable skip button.
     */
    protected final boolean skipButtonEnabled() {
        return true;
    }
}
