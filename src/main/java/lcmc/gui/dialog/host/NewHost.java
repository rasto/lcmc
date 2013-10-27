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
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.Tools;
import lcmc.gui.SpringUtilities;
import lcmc.gui.TerminalPanel;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.dialog.WizardDialog;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.BorderLayout;
import java.util.List;
import java.util.ArrayList;


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
    private Widget hostField;
    /** User name field. */
    private Widget usernameField;
    /** SSH Port field. */
    private Widget sshPortField;
    /** Whether sudo should be used. */
    private Widget useSudoField;
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
    /** Enable hostname after it was enabled at least once. */
    private boolean enableHostname = false;

    /** Prepares a new <code>NewHost</code> object. */
    public NewHost(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /** Finishes the dialog, stores the values and adds the host tab. */
    @Override
    protected void finishDialog() {
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

    /** Sets nextDialog to Configuration. */
    @Override
    public WizardDialog nextDialog() {
        return new Configuration(this, getHost());
    }

    /**
     * Checks host and username field and if both are not empty enables
     * next and finish buttons.
     */
    @Override
    protected final void checkFields(final Widget field) {
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
            Tools.invokeLater(new Runnable() {
                @Override
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
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    usernameField.setBackground(getHost().getUsername(),
                                                getHost().getUsername(),
                                                true);
                    if (useSudoField != null) {
                        if (Host.ROOT_USER.equals(us)) {
                            useSudoField.setValueAndWait("false");
                            useSudoField.setEnabled(false);
                        } else {
                            useSudoField.setValueAndWait("true");
                            useSudoField.setEnabled(true);
                        }
                    }
                }
            });
        } else {
            usernameField.wrongValue();
        }

        if (pf) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    sshPortField.setBackground(getHost().getSSHPort(),
                                               getHost().getSSHPort(),
                                               true);
                }
            });
        } else {
            sshPortField.wrongValue();
        }

        final boolean hostF = hf;
        final boolean userF = hf;
        final boolean sshPortF = pf;
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final JComponent btn : nextButtons()) {
                    btn.setEnabled(hostF && userF && sshPortF);
                }
            }
        });
    }

    /**
     * Returns the title of the dialog, defined as
     * Dialog.Host.NewHost.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.NewHost.Title");
    }

    /**
     * Returns the description of the dialog, defined as
     * Dialog.Host.NewHost.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.NewHost.Description");
    }

    /** Inits the dialog. */
    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(nextButtons());
    }

    /** Inits the dialog. */
    @Override
    protected final void initDialogAfterVisible() {
        enableComponents();
        checkFields((Widget) null);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                hostField.requestFocus();
            }
        });
        if (!Tools.getConfigData().getAutoHosts().isEmpty()) {
            Tools.invokeLater(new Runnable() {
                @Override
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
    @Override
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
        final String hostname = getHost().getHostname();
        String hn;
        if (hostname == null || Host.DEFAULT_HOSTNAME.equals(hostname)) {
            hn = getHost().getHostnameEntered();
        } else {
            hn = hostname;
        }
        hostField = WidgetFactory.createInstance(
                                       Widget.GUESS_TYPE,
                                       hn,
                                       Widget.NO_ITEMS,
                                       regexp,
                                       FIELD_WIDTH,
                                       Widget.NO_ABBRV,
                                       new AccessMode(ConfigData.AccessType.RO,
                                                      !AccessMode.ADVANCED),
                                       Widget.NO_BUTTON);
        if (hostname == null || Host.DEFAULT_HOSTNAME.equals(hostname)) {
            /* so that hostname is not disabled after going back in the wizard*/
            enableHostname = true;
        } else {
            if (!enableHostname) {
                hostField.setEnabled(false);
            }
        }

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
        sshPortField = WidgetFactory.createInstance(
                                      Widget.GUESS_TYPE,
                                      sshPort,
                                      Widget.NO_ITEMS,
                                      "^\\d+$",
                                      50,
                                      Widget.NO_ABBRV,
                                      new AccessMode(ConfigData.AccessType.RO,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
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
        usernameField = WidgetFactory.createInstance(
                                   Widget.GUESS_TYPE,
                                   userName,
                                   users.toArray(new String[users.size()]),
                                   regexp,
                                   FIELD_WIDTH,
                                   Widget.NO_ABBRV,
                                   new AccessMode(ConfigData.AccessType.RO,
                                                  !AccessMode.ADVANCED),
                                   Widget.NO_BUTTON);
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
        useSudoField = WidgetFactory.createInstance(
                                      Widget.GUESS_TYPE,
                                      useSudo.toString(),
                                      new String[]{"true", "false"},
                                      Widget.NO_REGEXP,
                                      50,
                                      Widget.NO_ABBRV,
                                      new AccessMode(ConfigData.AccessType.RO,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
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

    /** Buttons that are enabled/disabled during checks. */
    protected JComponent[] nextButtons() {
        return new JComponent[]{buttonClass(nextButton())};
    }
}
