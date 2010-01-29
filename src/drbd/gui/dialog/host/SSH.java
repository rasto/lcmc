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

package drbd.gui.dialog.host;

import drbd.data.Host;
import drbd.utilities.Tools;
import drbd.utilities.CancelCallback;
import drbd.utilities.MyButton;
import drbd.utilities.ConnectionCallback;
import drbd.gui.SpringUtilities;
import drbd.gui.SSHGui;
import drbd.gui.dialog.WizardDialog;

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
public class SSH extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Prepares a new <code>SSH</code> object.
     */
    public SSH(final WizardDialog previousDialog,
               final Host host) {
        super(previousDialog, host);
    }

    /**
     * Connects to all hosts.
     */
    private final String connectHost() {
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
                             answerPaneSetText(
                                Tools.getString("Dialog.Host.SSH.Connected"));
                             //enableComponents();
                             SwingUtilities.invokeLater(new Runnable() {
                                 public void run() {
                                    ((MyButton) buttonClass(
                                                nextButton())).pressButton();
                                 }
                             });
                         }

                         public void doneError(final String errorText) {
                             getHost().setConnected();
                             SwingUtilities.invokeLater(new Runnable() {
                                 public void run() {
                                    printErrorAndRetry(Tools.getString(
                                                "Dialog.Host.SSH.NotConnected")
                                                + "\n" + errorText);
                                 }
                             });
                         }
                      });
        getProgressBar().setCancelEnabled(true);

        return res;
    }

    /**
     * Returns the next dialog. Devices
     */
    public final WizardDialog nextDialog() {
        return new Devices(getPreviousDialog(), getHost());
    }

    /**
     * Inits the dialog and start connecting to the hosts.
     */
    protected final void initDialog() {
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
     * Dialog.Host.SSH.Title in TextResources.
     */
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.SSH.Title");
    }

    /**
     * Returns the description of the dialog, defined as
     * Dialog.Host.SSH.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.SSH.Description");
    }

    /**
     * Returns a pane where ssh connection will be attempted.
     */
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane(
                    new CancelCallback() {
                        public void cancel() {
                            Tools.debug(this, "cancel callback");
                            getHost().getSSH().cancelConnection();
                        }
                    }
                ));
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.SSH.Connecting")));

        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }
}
