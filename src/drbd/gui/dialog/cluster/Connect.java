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


package drbd.gui.dialog.cluster;

import drbd.data.Host;
import drbd.data.Cluster;
import drbd.utilities.Tools;
import drbd.gui.SpringUtilities;
import drbd.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
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
public class Connect extends DialogCluster {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Prepares a new <code>Connect</code> object. */
    public Connect(final WizardDialog previousDialog,
                   final Cluster cluster) {
        super(previousDialog, cluster);
    }

    /**
     * Returns the next dialog which is ClusterDrbdConf.
     */
    public final WizardDialog nextDialog() {
        return new CommStack(getPreviousDialog(), getCluster());
    }

    /**
     * Returns cluster dialog title.
     */
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.Connect.Title");
    }

    /**
     * Returns description.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.Cluster.Connect.Description");
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
             try {
                 Thread.sleep(1000);
             } catch (InterruptedException ex) {
                 Thread.currentThread().interrupt();
             }

             SwingUtilities.invokeLater(new Runnable() {
                 public void run() {
                    buttonClass(nextButton()).pressButton();
                 }
             });
        }
    }

    /**
     * Connects all cluster hosts.
     */
    protected final void connectHosts() {
        getCluster().connect(getDialogPanel());
        for (final Host host : getCluster().getHosts()) {
            host.waitOnLoading();
        }
        checkHosts();
    }

    /**
     * Inits the dialog and connects the hosts.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        // TODO: Tools.startProgressIndicator
        final Thread t = new Thread(new Runnable() {
            public void run() {
                connectHosts();
            }
        });
        t.start();
    }

    /**
     * Returns the connect hosts dialog content.
     */
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        final StringBuffer text = new StringBuffer();
        for (final Host host : getCluster().getHosts()) {
            text.append(host.getName());
            text.append(" connecting...\n");
        }
        pane.add(getAnswerPane(text.toString()));

        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }

    /**
     * Enable skip button.
     */
    protected final boolean skipButtonEnabled() {
        return true;
    }
}
