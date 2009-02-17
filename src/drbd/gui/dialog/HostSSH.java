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
import drbd.utilities.Tools;
import drbd.utilities.CancelCallback;
import drbd.gui.SpringUtilities;
import drbd.utilities.ConnectionCallback;
import drbd.gui.SSHGui;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * An implementation of a dialog where ssh connection will be established.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostSSH extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Prepares a new <code>HostSSH</code> object.
     */
    public HostSSH(final WizardDialog previousDialog,
                   final Host host) {
        super(previousDialog, host);
    }

    /**
     * Connects to all hosts.
     */
    private String connectHost() {
        final String res = null;
        final SSHGui sshGui = new SSHGui(getDialogPanel(),
                                         getHost(),
                                         getProgressBar());

        getHost().connect(sshGui, getProgressBar(),
                     new ConnectionCallback() {
                         public void done(final int flag) {
                             /* flag 0 now connected
                              * flag 1 already connected. */
                             Tools.debug(this,
                                         "callback done flag: " + flag, 1);
                             getHost().setConnected();
                             progressBarDone();
                             SwingUtilities.invokeLater(new Runnable() {
                                 public void run() {
                                    answerPaneSetText(
                                        Tools.getString(
                                                "Dialog.HostSSH.Connected"));
                                    enableComponents();
                                 }
                             });
                             buttonClass(nextButton()).requestFocus();
                         }

                         public void doneError(final String errorText) {
                             getHost().setConnected();
                             SwingUtilities.invokeLater(new Runnable() {
                                 public void run() {
                                    printErrorAndRetry(Tools.getString(
                                                "Dialog.HostSSH.NotConnected")
                                                + "\n" + errorText);
                                 }
                             });
                         }
                      });
        getProgressBar().setCancelEnabled(true);

        return res;
    }

    /**
     * Returns the next dialog. HostDevices
     */
    public WizardDialog nextDialog() {
        return new HostDevices(getPreviousDialog(), getHost());
    }

    /**
     * Inits the dialog and start connecting to the hosts.
     */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        final Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    connectHost();
                }
            });
        thread.start();
    }

    /**
     * Returns the title of the dialog, defined as
     * Dialog.HostSSH.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostSSH.Title");
    }

    /**
     * Returns the description of the dialog, defined as
     * Dialog.HostSSH.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.HostSSH.Description");
    }

    /**
     * Returns a pane where ssh connection will be attempted.
     */
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane(
                    new CancelCallback() {
                        public void cancel() {
                            Tools.debug(this, "cancel callback");
                            getHost().getSSH().cancelConnection();
                        }
                    }
                ));
        pane.add(getAnswerPane(Tools.getString("Dialog.HostSSH.Connecting")));

        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }
}
