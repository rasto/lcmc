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

package lcmc.gui.dialog.cluster;

import java.awt.Color;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Cluster;
import lcmc.data.Host;
import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.SSH;
import lcmc.utilities.SSH.ExecCommandThread;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog where user can choose cluster stack, that can
 * be Corosync or Heartbeat.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class CommStack extends DialogCluster {
    /** Logger. */
    private static final Logger LOG =
                                    LoggerFactory.getLogger(CommStack.class);
    /** Radio Combo box. */
    private Widget chooseStackCombo;

    /** Prepares a new {@code CommStack} object. */
    CommStack(final WizardDialog previousDialog, final Cluster cluster) {
        super(previousDialog, cluster);
    }

    /** Returns the next dialog. */
    @Override
    public WizardDialog nextDialog() {
        if (Application.HEARTBEAT_NAME.equals(chooseStackCombo.getValue().getValueForConfig())) {
            Tools.getApplication().setLastInstalledClusterStack(
                                                    Application.HEARTBEAT_NAME);
            return new HbConfig(this, getCluster());
        } else {
            Tools.getApplication().setLastInstalledClusterStack(
                                                    Application.COROSYNC_NAME);
            return new CoroConfig(this, getCluster());
        }
    }

    /** Returns the title of the dialog. */
    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.CommStack.Title");
    }

    /** Returns the description of the dialog. */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.CommStack.Description");
    }

    /** Inits the dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{});
    }

    /** Inits dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        final Host[] hosts = getCluster().getHostsArray();
        final ExecCommandThread[] infoThreads =
                                        new ExecCommandThread[hosts.length];
        getProgressBar().start(10000);
        int i = 0;
        for (final Host host : hosts) {
            infoThreads[i] = host.execCommand(
                             "Cluster.Init.getInstallationInfo",
                             getProgressBar(),
                             new ExecCallback() {
                                 @Override
                                 public void done(final String answer) {
                                     //drbdLoaded[index] = true;
                                     for (final String line
                                                    : answer.split("\\r?\\n")) {
                                         host.parseInstallationInfo(line);
                                     }
                                 }
                                 @Override
                                 public void doneError(final String answer,
                                                       final int errorCode) {
                                     skipButtonSetEnabled(false);
                                     LOG.error("initDialogAfterVisible: "
                                             + host.getName()
                                             + ": could not get install info: "
                                             + answer);
                                 }
                             },
                             null,   /* ConvertCmdCallback */
                             false,  /* outputVisible */
                             SSH.DEFAULT_COMMAND_TIMEOUT);
            i++;
        }
        for (final ExecCommandThread t : infoThreads) {
            /* wait for all of them */
            try {
                t.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        progressBarDone();
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
            chooseStackCombo.setValue(new StringValue(Application.HEARTBEAT_NAME));
        }
        final boolean ais = aisIsPossible;
        final boolean hb = hbIsPossible;
        if (ais || hb) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (ais) {
                        chooseStackCombo.setEnabled(Application.COROSYNC_NAME,
                                                    true);
                    }
                    if (hb) {
                        chooseStackCombo.setEnabled(Application.HEARTBEAT_NAME,
                                                    true);
                    }
                }
            });
        }
        enableComponents();
        if (ais || hb) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(nextButton()).setEnabled(true);
                    makeDefaultAndRequestFocus(buttonClass(nextButton()));
                }
            });
            if (!Tools.getApplication().getAutoClusters().isEmpty()) {
                Tools.sleep(1000);
                pressNextButton();
            }
        }
    }


    /** Returns the panel with radio boxes. */
    @Override
    protected JComponent getInputPane() {
        final JPanel inputPane = new JPanel(new SpringLayout());
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
            if (host.isCsRc() || host.isAisRc()) {
                aisIsRc++;
            }
            if (host.isHeartbeatRc()) {
                hbIsRc++;
            }
            if (host.isCsRunning() || host.isAisRunning()) {
                aisIsRunning++;
            }
            if (host.isHeartbeatRunning()) {
                hbIsRunning++;
            }
        }
        /* slight preference to corosync */
        String defaultValue;
        if (hbImpossible) {
            defaultValue = Application.COROSYNC_NAME;
        } else if (aisImpossible) {
            defaultValue = Application.HEARTBEAT_NAME;
        } else if (aisIsRc < hbIsRc) {
            defaultValue = Application.HEARTBEAT_NAME;
        } else if (aisIsRc > hbIsRc) {
            defaultValue = Application.COROSYNC_NAME;
        } else if (aisIsRunning < hbIsRunning) {
            defaultValue = Application.HEARTBEAT_NAME;
        } else if (aisIsRunning > hbIsRunning) {
            defaultValue = Application.COROSYNC_NAME;
        } else {
            defaultValue = Tools.getApplication().getLastInstalledClusterStack();
        }
        if (defaultValue == null) {
            defaultValue = Application.COROSYNC_NAME;
        }
        chooseStackCombo = WidgetFactory.createInstance(
                                          Widget.Type.RADIOGROUP,
                                          new StringValue(defaultValue),
                                          new Value[]{
                                                 new StringValue(Application.HEARTBEAT_NAME),
                                                 new StringValue(Application.COROSYNC_NAME)},
                                          Widget.NO_REGEXP,
                                          500,
                                          Widget.NO_ABBRV,
                                          new AccessMode(
                                                 Application.AccessType.ADMIN,
                                                 false), /* only adv. mode */
                                          Widget.NO_BUTTON);
        chooseStackCombo.setEnabled(Application.COROSYNC_NAME, false);
        chooseStackCombo.setEnabled(Application.HEARTBEAT_NAME, false);
        chooseStackCombo.setBackgroundColor(Color.WHITE);
        inputPane.add(getProgressBarPane(null));
        inputPane.add(chooseStackCombo.getComponent());
        SpringUtilities.makeCompactGrid(inputPane, 2, 1,  // rows, cols
                                                   0, 0,  // initX, initY
                                                   0, 0); // xPad, yPad
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(inputPane);
        panel.add(Box.createVerticalStrut(100));

        return panel;
    }

    /** Enable skip button. */
    @Override
    protected boolean skipButtonEnabled() {
        return true;
    }
}
