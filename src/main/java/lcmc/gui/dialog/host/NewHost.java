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

import lcmc.data.*;
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

import lcmc.gui.widget.Check;
import lcmc.utilities.MyButton;


/**
 * An implementation of a dialog where user can enter either ip or hostname of
 * the host and user name.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class NewHost extends DialogHost {
    /** Normal widths of the fields. */
    private static final int FIELD_WIDTH = 120;
    /** Widths of the fields if hops are used. */
    private static final int BIG_FIELD_WIDTH = 400;
    /** Default ssh user. */
    private static final String SSH_ROOT_USER = Tools.getDefault("SSH.User");
    /** Default ssh port. */
    private static final String SSH_PORT = Tools.getDefault("SSH.Port");
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
    /** Enable hostname after it was enabled at least once. */
    private boolean enableHostname = false;

    /** Prepares a new {@code NewHost} object. */
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
        Tools.getApplication().setLastEnteredUser(username);
        final String sshPort = sshPortField.getStringValue().trim();
        getHost().setSSHPort(sshPort);
        Tools.getApplication().setLastEnteredSSHPort(sshPort);
        final String useSudoString = useSudoField.getStringValue().trim();
        getHost().setUseSudo("true".equals(useSudoString));
        Tools.getApplication().setLastEnteredUseSudo(
                                                "true".equals(useSudoString));
        if (!Tools.getApplication().existsHost(getHost())) {
            Tools.getApplication().addHostToHosts(getHost());
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
        boolean hf = (!hs.isEmpty());
        boolean uf = (!us.isEmpty());
        final boolean pf = (!ps.isEmpty());
        final int hc = Tools.charCount(hs, ',');
        final int uc = Tools.charCount(us, ',');
        final List<String> incorrect = new ArrayList<String>();
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
                    hostField.setBackground(new StringValue(getHost().getHostnameEntered()),
                                            new StringValue(getHost().getHostnameEntered()),
                                            true);
                }
            });
        } else {
            hostField.wrongValue();
            incorrect.add("host");
        }

        if (uf) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    usernameField.setBackground(new StringValue(getHost().getUsername()),
                                                new StringValue(getHost().getUsername()),
                                                true);
                    if (useSudoField != null) {
                        if (Host.ROOT_USER.equals(us)) {
                            useSudoField.setValueAndWait(new StringValue("false"));
                            useSudoField.setEnabled(false);
                        } else {
                            useSudoField.setValueAndWait(new StringValue("true"));
                            useSudoField.setEnabled(true);
                        }
                    }
                }
            });
        } else {
            usernameField.wrongValue();
            incorrect.add("username");
        }

        if (pf) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    sshPortField.setBackground(new StringValue(getHost().getSSHPort()),
                                               new StringValue(getHost().getSSHPort()),
                                               true);
                }
            });
        } else {
            sshPortField.wrongValue();
            incorrect.add("SSH port");
        }

        final List<String> changed = new ArrayList<String>();
        enableNextButtons(incorrect, changed);
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
    }

    /** Inits the dialog. */
    @Override
    protected final void initDialogAfterVisible() {
        enableComponents();
        makeDefaultButton(buttonClass(nextButton()));
        checkFields(null);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                hostField.requestFocus();
            }
        });
        if (!Tools.getApplication().getAutoHosts().isEmpty()) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    hostField.setValue(
                                new StringValue(Tools.getApplication().getAutoHosts().get(0)));
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
        final String hostname = getHost().getHostname();
        final String hn;
        if (hostname == null || Host.DEFAULT_HOSTNAME.equals(hostname)) {
            hn = getHost().getHostnameEntered();
        } else {
            hn = hostname;
        }
        final String regexp = "^[,\\w.-]+$";
        hostField = WidgetFactory.createInstance(
                                       Widget.GUESS_TYPE,
                                       new StringValue(hn),
                                       Widget.NO_ITEMS,
                                       regexp,
                                       FIELD_WIDTH,
                                       Widget.NO_ABBRV,
                                       new AccessMode(Application.AccessType.RO,
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
        hostLabel.setLabelFor(hostField.getComponent());
        inputPane.add(hostField.getComponent());
        hostField.setBackground(new StringValue(getHost().getHostnameEntered()),
                                new StringValue(getHost().getHostnameEntered()),
                                true);

        /* SSH Port */
        final JLabel sshPortLabel = new JLabel(
                        Tools.getString("Dialog.Host.NewHost.SSHPort"));

        inputPane.add(sshPortLabel);
        String sshPort = getHost().getSSHPort();
        if (sshPort == null) {
            sshPort = Tools.getApplication().getLastEnteredSSHPort();
            if (sshPort == null) {
                sshPort = SSH_PORT;
            }
        }
        sshPortField = WidgetFactory.createInstance(
                                      Widget.GUESS_TYPE,
                                      new StringValue(sshPort),
                                      Widget.NO_ITEMS,
                                      "^\\d+$",
                                      50,
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.RO,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
        addCheckField(sshPortField);
        sshPortLabel.setLabelFor(sshPortField.getComponent());
        inputPane.add(sshPortField.getComponent());
        sshPortField.setBackground(new StringValue(getHost().getSSHPort()),
                                   new StringValue(getHost().getSSHPort()),
                                   true);


        /* Username */
        final JLabel usernameLabel = new JLabel(
                        Tools.getString("Dialog.Host.NewHost.EnterUsername"));

        inputPane.add(usernameLabel);
        String userName = getHost().getUsername();
        if (userName == null) {
            userName = Tools.getApplication().getLastEnteredUser();
            if (userName == null) {
                userName = SSH_ROOT_USER;
            }
        }
        final List<Value> users = new ArrayList<Value>();
        final String user = System.getProperty("user.name");
        if (!SSH_ROOT_USER.equals(user)) {
            users.add(new StringValue(SSH_ROOT_USER));
        }
        users.add(new StringValue(user));
        usernameField = WidgetFactory.createInstance(
                                   Widget.GUESS_TYPE,
                                   new StringValue(userName),
                                   users.toArray(new Value[users.size()]),
                                   regexp,
                                   FIELD_WIDTH,
                                   Widget.NO_ABBRV,
                                   new AccessMode(Application.AccessType.RO,
                                                  !AccessMode.ADVANCED),
                                   Widget.NO_BUTTON);
        usernameField.setEditable(true);
        addCheckField(usernameField);
        usernameLabel.setLabelFor(usernameField.getComponent());
        inputPane.add(usernameField.getComponent());
        usernameField.setBackground(new StringValue(getHost().getUsername()),
                                    new StringValue(getHost().getUsername()),
                                    true);
        /* use sudo */
        final JLabel useSudoLabel = new JLabel(
                        Tools.getString("Dialog.Host.NewHost.UseSudo"));

        inputPane.add(useSudoLabel);
        Boolean useSudo = getHost().isUseSudo();
        if (useSudo == null) {
            useSudo = Tools.getApplication().getLastEnteredUseSudo();
            if (useSudo == null) {
                useSudo = false;
            }
        }
        final Value useSudoValue = new StringValue(useSudo.toString());
        useSudoField = WidgetFactory.createInstance(
                                      Widget.GUESS_TYPE,
                                      useSudoValue,
                                      new Value[]{new StringValue("true"),
                                                  new StringValue("false")},
                                      Widget.NO_REGEXP,
                                      50,
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.RO,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
        //addCheckField(useSudoField);
        useSudoLabel.setLabelFor(useSudoField.getComponent());
        inputPane.add(useSudoField.getComponent());
        useSudoField.setBackground(useSudoValue,
                                   useSudoValue,
                                   true);

        SpringUtilities.makeCompactGrid(inputPane, 2, 4,  // rows, cols
                                                   1, 1,  // initX, initY
                                                   1, 1); // xPad, yPad
        p.add(inputPane, BorderLayout.PAGE_END);
        return p;
    }
}
