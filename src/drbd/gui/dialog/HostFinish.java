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

import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.data.Host;
import drbd.AddHostDialog;
import drbd.AddClusterDialog;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.ImageIcon;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;

/**
 * Host finish dialog with buttons to configure next host or configure the
 * clsuter.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostFinish extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Add another host button. */
    private MyButton addAnotherHostButton;
    /** Configure cluster button. */
    private MyButton confClusterButton;
    /** Host icon for add another host button. */
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(
                            Tools.getDefault("Dialog.HostFinish.HostIcon"));
    /** Cluster icon for define cluster button. */
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                            Tools.getDefault("Dialog.HostFinish.ClusterIcon"));
    /** Dimensions of the buttons. */
    private static final Dimension BUTTON_DIMENSION = new Dimension(300, 100);
    /**
     * Prepares a new <code>HostFinish</code> object.
     */
    public HostFinish(final WizardDialog previousDialog,
                      final Host host) {
        super(previousDialog, host);
    }

    /**
     * Returns next dialog. Null
     */
    public WizardDialog nextDialog() {
        return null;
    }

    /**
     * Inits the dialog.
     */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton()),
                                               buttonClass(finishButton())});

        enableComponents(new JComponent[]{buttonClass(nextButton()),
                                          buttonClass(finishButton())});
        if (Tools.getConfigData().danglingHostsCount() < 2) {
            /* workaround, TODO: is it necessary? */
            requestFocusLater(addAnotherHostButton);
        } else {
            requestFocusLater(confClusterButton);
        }
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.HostFinish.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostFinish.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.HostFinish.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.HostFinish.Description");
    }

    /**
     * Returns input pane with two big buttons: configure a cluster or add
     * another host.
     */
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel();
        addAnotherHostButton = new MyButton(
                    Tools.getString("Dialog.HostFinish.AddAnotherHostButton"),
                    HOST_ICON);
        addAnotherHostButton.setPreferredSize(BUTTON_DIMENSION);
        addAnotherHostButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    public void run() {
                        final AddHostDialog h = new AddHostDialog();
                        h.showDialogs();
                    }
                });
                t.start();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        addAnotherHostButton.setEnabled(false);
                        ((MyButton) buttonClass(finishButton())).pressButton();
                    }
                });
            }
        });
        confClusterButton = new MyButton(
                    Tools.getString("Dialog.HostFinish.ConfigureClusterButton"),
                    CLUSTER_ICON);
        confClusterButton.setPreferredSize(BUTTON_DIMENSION);
        confClusterButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    public void run() {
                        final AddClusterDialog c = new AddClusterDialog();
                        c.showDialogs();
                    }
                });
                t.start();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        confClusterButton.setEnabled(false);
                        ((MyButton) buttonClass(finishButton())).pressButton();
                    }
                });
            }
        });
        pane.add(addAnotherHostButton);
        if (Tools.getConfigData().danglingHostsCount() < 2) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    confClusterButton.setEnabled(false);
                }
            });
        }
        pane.add(confClusterButton);
        return pane;
    }
}
