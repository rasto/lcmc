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
import drbd.gui.SpringUtilities;
import drbd.gui.TerminalPanel;
import drbd.gui.GuiComboBox;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.BorderLayout;

import javax.swing.SwingUtilities;

/**
 * An implementation of a dialog where user can enter either ip or hostname of
 * the host and user name.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostNewHost extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** hostField can be ip or hostname with or without domainname. */
    private GuiComboBox hostField;
    /** User name field. */
    private GuiComboBox usernameField;
    /** SSH Port field. */
    private GuiComboBox sshPortField;
    /** Whether the fields are big (if more hops are being used). */
    private boolean bigFields = false;
    /** Normal widths of the fields. */
    private static final int FIELD_WIDTH = 120;
    /** Widths of the fields if hops are used. */
    private static final int BIG_FIELD_WIDTH = 400;

    /**
     * Prepares a new <code>HostNewHost</code> object.
     */
    public HostNewHost(final WizardDialog previousDialog,
                       final Host host) {
        super(previousDialog, host);
    }

    /**
     * Finishes the dialog, stores the values and adds the host tab.
     */
    protected void finishDialog() {
        final String hostnameEntered = hostField.getStringValue().trim();
        getHost().setHostnameEntered(hostnameEntered);
        getHost().setUsername(usernameField.getStringValue().trim());
        getHost().setSSHPort(sshPortField.getStringValue().trim());
        if (!Tools.getConfigData().existsHost(getHost())) {
            Tools.getConfigData().addHostToHosts(getHost());
            final TerminalPanel terminalPanel = new TerminalPanel(getHost());
            Tools.getGUIData().setTerminalPanel(terminalPanel);
        }
    }

    /**
     * Sets nextDialog to HostConfiguration.
     */
    public WizardDialog nextDialog() {
        return new HostConfiguration(this, getHost());
    }

    /**
     * Checks host and username field and if both are not empty enables
     * next and finish buttons.
     */
    protected void checkFields(final GuiComboBox field) {
        final String hs = hostField.getStringValue().trim();
        final String us = usernameField.getStringValue().trim();
        final String ps = sshPortField.getStringValue().trim();
        boolean hf = (hs.length() > 0);
        boolean uf = (us.length() > 0);
        boolean pf = (ps.length() > 0);
        final int hc = Tools.charCount(hs, ',');
        final int uc = Tools.charCount(us, ',');
        if (hf && uf) {
            if (hc != uc) {
                uf = false;
            } else if (uc > Tools.getDefaultInt("MaxHops") - 1) {
                uf = false;
            }
            if (hc > Tools.getDefaultInt("MaxHops") - 1) {
                hf = false;
            }

            if (hc > 0 || uc > 0) {
                if (!bigFields) {
                    hostField.setWidth(BIG_FIELD_WIDTH);
                    usernameField.setWidth(BIG_FIELD_WIDTH);
                    bigFields = true;
                }
            } else {
                if (bigFields) {
                    hostField.setWidth(FIELD_WIDTH);
                    usernameField.setWidth(FIELD_WIDTH);
                    bigFields = false;
                }
            }
        }

        if (hf) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    hostField.setBackground(getHost().getHostnameEntered(),
                                            true);
                }
            });
        } else {
            hostField.wrongValue();
        }

        if (uf) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    usernameField.setBackground(getHost().getUsername(), true);
                }
            });
        } else {
            usernameField.wrongValue();
        }

        if (pf) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    sshPortField.setBackground(getHost().getSSHPort(), true);
                }
            });
        } else {
            sshPortField.wrongValue();
        }

        final boolean hostField = hf;
        final boolean userField = hf;
        final boolean sshPortField = pf;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                buttonClass(nextButton()).setEnabled(hostField
                                                     && userField
                                                     && sshPortField);
            }
        });
    }

    /**
     * Returns the title of the dialog, defined as
     * Dialog.HostNewHost.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostNewHost.Title");
    }

    /**
     * Returns the description of the dialog, defined as
     * Dialog.HostNewHost.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.HostNewHost.Description");
    }

    /**
     * Inits the dialog.
     */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        enableComponents();
        checkFields((GuiComboBox) null);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                hostField.requestFocus();
                hostField.selectAll();
            }
        });
    }

    /**
     * Returns an input pane where user can enter a host and username. Username
     * is normally root and host can be entered either as ip or node name etc.
     */
    protected JComponent getInputPane() {
        final JPanel p = new JPanel(new BorderLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Tools.getDefaultColor(
                                            "ConfigDialog.Background.Light"));
        inputPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Host */
        final JLabel hostLabel = new JLabel(
                        Tools.getString("Dialog.HostNewHost.EnterHost"));
        inputPane.add(hostLabel);
        final String regexp = "^[,\\w.-]+$";
        hostField = new GuiComboBox(getHost().getHostnameEntered(),
                                    null, null, regexp, FIELD_WIDTH);

        addCheckField(hostField);
        hostField.selectAll();
        hostLabel.setLabelFor(hostField);
        inputPane.add(hostField);
        hostField.setBackground(getHost().getHostnameEntered(), true);

        /* SSH Port */
        final JLabel sshPortLabel = new JLabel(
                        Tools.getString("Dialog.HostNewHost.SSHPort"));

        inputPane.add(sshPortLabel);
        sshPortField = new GuiComboBox(getHost().getSSHPort(),
                                        null,
                                        null,
                                        "^\\d+$",
                                        50);
        addCheckField(sshPortField);
        sshPortField.selectAll();
        sshPortLabel.setLabelFor(sshPortField);
        inputPane.add(sshPortField);
        sshPortField.setBackground(getHost().getSSHPort(), true);


        /* Username */
        final JLabel usernameLabel = new JLabel(
                        Tools.getString("Dialog.HostNewHost.EnterUsername"));

        inputPane.add(usernameLabel);
        usernameField = new GuiComboBox(getHost().getUsername(),
                                        null, null, regexp, FIELD_WIDTH);
        addCheckField(usernameField);
        usernameField.selectAll();
        usernameLabel.setLabelFor(usernameField);
        inputPane.add(usernameField);
        usernameField.setBackground(getHost().getUsername(), true);
        inputPane.add(new JLabel(""));
        inputPane.add(new JLabel(""));

        SpringUtilities.makeCompactGrid(inputPane, 2, 4,  // rows, cols
                                                   1, 1,  // initX, initY
                                                   1, 1); // xPad, yPad
        p.add(inputPane, BorderLayout.SOUTH);
        return p;
    }
}
