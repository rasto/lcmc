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

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import drbd.gui.SpringUtilities;
import drbd.utilities.ConnectionCallback;
import drbd.gui.SSHGui;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * An implementation of a dialog where connection to every host will be checked
 * and established if there isn't one.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ClusterConnect extends DialogCluster {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Prepares a new <code>ClusterConnect</code> object. */
    public ClusterConnect(final WizardDialog previousDialog,
                          final Cluster cluster) {
        super(previousDialog, cluster);
    }

    /**
     * Returns the next dialog which is ClusterDrbdConf.
     */
    public final WizardDialog nextDialog() {
        // TODO: or ClusterHbConfig
        return new ClusterChooseStack(this, getCluster());
    }

    /**
     * Returns cluster dialog title.
     */
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.ClusterConnect.Title");
    }

    /**
     * Returns description.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.ClusterConnect.Description");
    }

    /**
     * Checks hosts, if they are connected and if not reconnects them.
     */
    protected final void checkHosts() {
        final StringBuffer text = new StringBuffer();
        boolean pending = false;
        boolean oneFailed = false;
        for (final Host host : getCluster().getHosts()) {
            String status;
            if (host.getSSH().isConnectionFailed()) {
                status = "failed.";
                oneFailed = true;
            } else if (host.isConnected()) {
                status = "connected.";
            } else {
                pending = true;
                status = "connecting...";
            }
            text.append(host.getName() + " " + status + "\n");
        }
        Tools.debug(this, "pending: " + pending + ", one failed: " + oneFailed);
        if (pending) {
             answerPaneSetText(text.toString());
        } else if (oneFailed) {
             printErrorAndRetry(text.toString());
        } else {
             answerPaneSetText(text.toString());
             answerPaneSetText(text.toString());
             enableComponents();
             SwingUtilities.invokeLater(new Runnable() {
                 public void run() {
                    buttonClass(nextButton()).requestFocus();
                 }
             });
        }
    }

    /**
     * Connects the specified host.
     */
    private String connectHost(final Host host) {
        final String res = null;
        final SSHGui sshGui = new SSHGui(getDialogPanel(), host, null);
        host.connect(sshGui, null, //getProgressBar(),
                     new ConnectionCallback() {
                         public void done(final int flag) {
                             checkHosts();
                         }

                         public void doneError(final String errorText) {
                             checkHosts();
                         }
                      });
        return res;
    }

    /**
     * Connects all cluster hosts.
     */
    protected final void connectHosts() {
        boolean allConnected = true;
        for (final Host host : getCluster().getHosts()) {
            if (!host.isConnected()) {
                allConnected = false;
                connectHost(host);
            }
        }
        if (allConnected) {
            checkHosts();
        }
    }

    /**
     * Inits the dialog and connects the hosts.
     */
    protected final void initDialog() {
        super.initDialog();
        final JComponent[] c = {buttonClass(nextButton())};
        enableComponentsLater(c);
        // TODO: Tools.startProgressIndicator

        //SwingUtilities.invokeLater(new Runnable() { public void run() {
        final Thread t = new Thread(new Runnable() {
            public void run() {
                connectHosts();
            }
        });
        t.start();
        //} });
    }

    /**
     * Returns the connect hosts dialog content.
     */
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getAnswerPane(""));

        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }
}
