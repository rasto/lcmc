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
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.Tools;
import drbd.gui.SpringUtilities;
import drbd.gui.TerminalPanel;
import drbd.gui.GuiComboBox;
import drbd.gui.dialog.WizardDialog;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.BorderLayout;
import java.util.List;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

/**
 * An implementation of a dialog where user can enter either ip or hostname of
 * the host and user name.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class NewHost extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** hostField can be ip or hostname with or without domainname. */
    private GuiComboBox hostField;
    /** User name field. */
    private GuiComboBox usernameField;
    /** SSH Port field. */
    private GuiComboBox sshPortField;
    /** Whether sudo should be used. */
    private GuiComboBox useSudoField;
    /** Whether the fields are big (if more hops are being used). */
    private boolean bigFields = false;
    /** Normal widths of the fields. */
    private static final int FIELD_WIDTH = 120;
    /** Widths of the fields if hops are used. */
    private static final int BIG_FIELD_WIDTH = 400;
    /** Default ssh user. */
    private static final String SSH_ROOT_USER = Tools.getDefault("SSH.User");
    /** Default ssh port. */
    private static final String SSH_PORT = Tools.getDefault("SSH.Port");

    /**
     * Prepares a new <code>NewHost</code> object.
     */
    public NewHost(final WizardDialog previousDialog,
                   final Host host) {
        super(previousDialog, host);
    }

    /**
     * Finishes the dialog, stores the values and adds the host tab.
     */
    protected final void finishDialog() {
        final String hostnameEntered = hostField.getStringValue().trim();
        getHost().setHostnameEntered(hostnameEntered);
        final String username = usernameField.getStringValue().trim();
        getHost().setUsername(username);
        Tools.getConfigData().setLastEnteredUser(username);
        final String sshPort = sshPortField.getStringValue().trim();
        getHost().setSSHPort(sshPort);
        Tools.getConfigData().setLastEnteredSSHPort(sshPort);
        final String useSudoString = useSudoField.getStringValue().trim();
        getHost().setUseSudo("true".equals(useSudoString));
        Tools.getConfigData().setLastEnteredUseSudo(
                                                "true".equals(useSudoString));
        if (!Tools.getConfigData().existsHost(getHost())) {
            Tools.getConfigData().addHostToHosts(getHost());
            final TerminalPanel terminalPanel = new TerminalPanel(getHost());
            Tools.getGUIData().setTerminalPanel(terminalPanel);
        }
    }

    /**
     * Sets nextDialog to Configuration.
     */
    public final WizardDialog nextDialog() {
        return new Configuration(this, getHost());
    }

    /**
     * Checks host and username field and if both are not empty enables
     * next and finish buttons.
     */
    protected final void checkFields(final GuiComboBox field) {
        final String hs = hostField.getStringValue().trim();
        final String us = usernameField.getStringValue().trim();
        final String ps = sshPortField.getStringValue().trim();
        boolean hf = (hs.length() > 0);
        boolean uf = (us.length() > 0);
        final boolean pf = (ps.length() > 0);
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
                                            getHost().getHostnameEntered(),
                                            true);
                }
            });
        } else {
            hostField.wrongValue();
        }

        if (uf) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    usernameField.setBackground(getHost().getUsername(),
                                                getHost().getUsername(),
                                                true);
                    if (useSudoField != null) {
                        if ("root".equals(us)) {
                            useSudoField.setValueAndWait("false");
                            useSudoField.setEnabled(false);
                        } else {
                            useSudoField.setEnabled(true);
                        }
                    }
                }
            });
        } else {
            usernameField.wrongValue();
        }

        if (pf) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    sshPortField.setBackground(getHost().getSSHPort(),
                                               getHost().getSSHPort(),
                                               true);
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
     * Dialog.Host.NewHost.Title in TextResources.
     */
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.NewHost.Title");
    }

    /**
     * Returns the description of the dialog, defined as
     * Dialog.Host.NewHost.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.NewHost.Description");
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        enableComponents();
        checkFields((GuiComboBox) null);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                hostField.requestFocus();

            }
        });
        if (!Tools.getConfigData().getAutoHosts().isEmpty()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    hostField.setValue(
                                Tools.getConfigData().getAutoHosts().get(0));
                }
            });
            Tools.sleep(3000);
            pressNextButton();
        }
    }

    /**
     * Returns an input pane where user can enter a host and username. Username
     * is normally root and host can be entered either as ip or node name etc.
     */
    protected final JComponent getInputPane() {
        final JPanel p = new JPanel(new BorderLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Tools.getDefaultColor(
                                            "ConfigDialog.Background.Light"));
        inputPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Host */
        final JLabel hostLabel = new JLabel(
                        Tools.getString("Dialog.Host.NewHost.EnterHost"));
        inputPane.add(hostLabel);
        final String regexp = "^[,\\w.-]+$";
        hostField = new GuiComboBox(getHost().getHostnameEntered(),
                                    null, /* items */
                                    null, /* units */
                                    null, /* type */
                                    regexp,
                                    FIELD_WIDTH,
                                    null, /* abbrv */
                                    new AccessMode(ConfigData.AccessType.RO,
                                                   false)); /* only adv. mode */

        addCheckField(hostField);
        hostLabel.setLabelFor(hostField);
        inputPane.add(hostField);
        hostField.setBackground(getHost().getHostnameEntered(),
                                getHost().getHostnameEntered(),
                                true);

        /* SSH Port */
        final JLabel sshPortLabel = new JLabel(
                        Tools.getString("Dialog.Host.NewHost.SSHPort"));

        inputPane.add(sshPortLabel);
        String sshPort = getHost().getSSHPort();
        if (sshPort == null) {
            sshPort = Tools.getConfigData().getLastEnteredSSHPort();
            if (sshPort == null) {
                sshPort = SSH_PORT;
            }
        }
        sshPortField = new GuiComboBox(sshPort,
                                       null, /* items */
                                       null, /* units */
                                       null, /* type */
                                       "^\\d+$",
                                       50,
                                       null, /* abbrv */
                                       new AccessMode(ConfigData.AccessType.RO,
                                                      false)); /* only adv. */
        addCheckField(sshPortField);
        sshPortLabel.setLabelFor(sshPortField);
        inputPane.add(sshPortField);
        sshPortField.setBackground(getHost().getSSHPort(),
                                   getHost().getSSHPort(),
                                   true);


        /* Username */
        final JLabel usernameLabel = new JLabel(
                        Tools.getString("Dialog.Host.NewHost.EnterUsername"));

        inputPane.add(usernameLabel);
        String userName = getHost().getUsername();
        if (userName == null) {
            userName = Tools.getConfigData().getLastEnteredUser();
            if (userName == null) {
                userName = SSH_ROOT_USER;
            }
        }
        final List<String> users = new ArrayList<String>();
        final String user = System.getProperty("user.name");
        if (!SSH_ROOT_USER.equals(user)) {
            users.add(SSH_ROOT_USER);
        }
        users.add(user);
        usernameField = new GuiComboBox(
                                    userName,
                                    users.toArray(new String[users.size()]),
                                    null, /* units */
                                    null, /* type */
                                    regexp,
                                    FIELD_WIDTH,
                                    null, /* abbrv */
                                    new AccessMode(ConfigData.AccessType.RO,
                                                   false)); /* only adv. mode */
        usernameField.setEditable(true);
        addCheckField(usernameField);
        usernameLabel.setLabelFor(usernameField);
        inputPane.add(usernameField);
        usernameField.setBackground(getHost().getUsername(),
                                    getHost().getUsername(),
                                    true);
        /* use sudo */
        final JLabel useSudoLabel = new JLabel(
                        Tools.getString("Dialog.Host.NewHost.UseSudo"));

        inputPane.add(useSudoLabel);
        Boolean useSudo = getHost().isUseSudo();
        if (useSudo == null) {
            useSudo = Tools.getConfigData().getLastEnteredUseSudo();
            if (useSudo == null) {
                useSudo = false;
            }
        }
        useSudoField = new GuiComboBox(
                                   useSudo.toString(),
                                   new String[]{"true", "false"}, /* items */
                                   null, /* units */
                                   null, /* type */
                                   null, /* regexp */
                                   50,
                                   null, /* abbrv */
                                   new AccessMode(ConfigData.AccessType.RO,
                                                  false)); /* only adv. mode */
        //addCheckField(useSudoField);
        useSudoLabel.setLabelFor(useSudoField);
        inputPane.add(useSudoField);
        useSudoField.setBackground(useSudo,
                                   useSudo,
                                   true);

        SpringUtilities.makeCompactGrid(inputPane, 2, 4,  // rows, cols
                                                   1, 1,  // initX, initY
                                                   1, 1); // xPad, yPad
        p.add(inputPane, BorderLayout.SOUTH);
        return p;
    }
}
