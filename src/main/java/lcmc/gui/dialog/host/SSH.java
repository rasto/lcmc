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

package lcmc.gui.dialog.host;

import lcmc.data.Host;
import lcmc.utilities.Tools;
import lcmc.utilities.CancelCallback;
import lcmc.utilities.ConnectionCallback;
import lcmc.gui.SSHGui;
import lcmc.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * An implementation of a dialog where ssh connection will be established.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class SSH extends DialogHost {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(SSH.class);

    /** Prepares a new {@code SSH} object. */
    public SSH(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /** Connects to all hosts. */
    private String connectHost() {
        final SSHGui sshGui = new SSHGui(getDialogPanel(),
                                         getHost(),
                                         getProgressBar());

        getHost().connect(sshGui, getProgressBar(),
                     new ConnectionCallback() {
                         @Override
                         public void done(final int flag) {
                             /* flag 0 now connected
                              * flag 1 already connected. */
                             LOG.debug1("done: callback done flag: " + flag);
                             getHost().setConnected();
                             progressBarDone();
                             answerPaneSetText(
                                Tools.getString("Dialog.Host.SSH.Connected"));
                             //enableComponents();
                             Tools.invokeLater(new Runnable() {
                                 @Override
                                 public void run() {
                                    buttonClass(nextButton()).pressButton();
                                 }
                             });
                         }

                         @Override
                         public void doneError(final String errorText) {
                             getHost().setConnected();
                             Tools.invokeLater(new Runnable() {
                                 @Override
                                 public void run() {
                                    printErrorAndRetry(Tools.getString(
                                                "Dialog.Host.SSH.NotConnected")
                                                + '\n' + errorText);
                                 }
                             });
                         }
                      });
        getProgressBar().setCancelEnabled(true);

        return null;
    }

    /** Returns the next dialog. Devices */
    @Override
    public WizardDialog nextDialog() {
        return new Devices(getPreviousDialog(), getHost());
    }

    /** Inits the dialog and start connecting to the hosts. */
    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(nextButtons());
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected final void initDialogAfterVisible() {
        final Thread thread = new Thread(
            new Runnable() {
                @Override
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
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.SSH.Title");
    }

    /**
     * Returns the description of the dialog, defined as
     * Dialog.Host.SSH.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.SSH.Description");
    }

    /** Returns a pane where ssh connection will be attempted. */
    @Override
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel();
        //final JPanel pane = new JPanel(new SpringLayout());
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        pane.add(getProgressBarPane(
                    new CancelCallback() {
                        @Override
                        public void cancel() {
                            LOG.debug("cancel: callback");
                            getHost().getSSH().cancelConnection();
                        }
                    }
                ));
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.SSH.Connecting")));

//        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
//                                              1, 1,  // initX, initY
//                                              1, 1); // xPad, yPad
        return pane;
    }

    /** Buttons that are enabled/disabled during checks. */
    protected JComponent[] nextButtons() {
        return new JComponent[]{buttonClass(nextButton())};
    }
}
