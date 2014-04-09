/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2012, Rasto Levrinc
 *
 * LCMC is free software; you can redistribute it and/or
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

package lcmc.gui.dialog;

import lcmc.utilities.Tools;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH;
import lcmc.utilities.SSH.ExecCommandThread;
import lcmc.data.Host;
import lcmc.configs.DistResource;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.text.Document;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.Color;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.FlowLayout;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * An implementation of an edit config dialog.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class EditConfig extends ConfigDialog {
    /** Logger. */
    private static final Logger LOG =
                                    LoggerFactory.getLogger(EditConfig.class);
    /** File to edit. */
    private final String file;
    /** Cluster hosts. */
    private final Set<Host> hosts;
    /** Map from hosts to checkboxes. */
    private final Map<Host, JCheckBox> hostCbs = new HashMap<Host, JCheckBox>();
    /** Backup checkbox. */
    private final JCheckBox backupCB = new JCheckBox(
                            Tools.getString("Dialog.EditConfig.Backup.Button"));

    /** Config text area. */
    private final JTextArea configArea = new JTextArea(
                                Tools.getString("Dialog.EditConfig.Loading"));

    /** Error panel for error messages. */
    private final JLabel errorPanel = new JLabel();
    /** Whether config area is being filled so that save button,
        doesn't get enabled. */
    private volatile boolean configInProgress = true;

    /**
     * Create new editConfig object.
     */
    public EditConfig(final String file, final Set<Host> hosts) {
        super();
        this.file = file;
        this.hosts = hosts;
        addToOptions(backupCB);
    }

    /** Returns the content of the edit config dialog. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        final JPanel hostsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        errorPanel.setForeground(Color.RED);
        backupCB.setBackground(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));
        hostsPanel.add(backupCB);
        for (final Host host : hosts) {
            final JCheckBox hcb = new JCheckBox(host.getName());
            hostCbs.put(host, hcb);
            hcb.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Light"));

            hostsPanel.add(hcb);
        }
        hostsPanel.add(errorPanel);
        pane.add(hostsPanel);

        pane.add(configArea);
        final JScrollPane sp = new JScrollPane(pane);
        sp.setPreferredSize(sp.getMaximumSize());
        return sp;
    }


    /** Inits the dialog and enables all the components. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        /* align buttons to the right */
        final FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.TRAILING);

        if (buttonClass(cancelButton()) != null) {
            buttonClass(cancelButton()).getParent().setLayout(layout);
        }
        buttonClass(saveButton()).setEnabled(false);
        enableComponents();
    }

    /** Set config are with config and error text, if any. */
    private void setConfigArea(final String text,
                               final int errorCode,
                               final boolean selected) {
        if (selected) {
            final String config;
            if (errorCode == 0) {
                config = text;
            } else {
                errorPanel.setText("WARNING: " + text);
                config = Tools.getString("Dialog.EditConfig.NewConfig");
            }
            configArea.setText(config);
        }
        configArea.setCaretPosition(0);
        configArea.requestFocus();
        if (errorCode != 0) {
            configArea.selectAll();
        }
        configInProgress = false;
    }

    /** This method is called immediatly after the dialog is shown. */
    @Override
    protected void initDialogAfterVisible() {
        final ExecCommandThread[] threads = new ExecCommandThread[hosts.size()];
        final String[] results = new String[hosts.size()];
        final Integer[] errors = new Integer[hosts.size()];
        int i = 0;
        for (final Host host : hosts) {
            final int index = i;
            final ExecCallback execCallback =
                new ExecCallback() {
                    @Override
                    public void done(final String answer) {
                        final String text = answer.replaceAll("\r", "")
                                               .replaceFirst("\n$", "");
                        results[index] = text;
                        errors[index] = 0;
                    }

                    @Override
                    public void doneError(final String answer,
                                          final int errorCode) {
                        results[index] = answer;
                        errors[index] = errorCode;
                        LOG.sshError(host, "", answer, "", errorCode);
                    }

                };
            threads[i] = host.execCommandRaw(DistResource.SUDO + "cat " + file,
                                             execCallback,
                                             false, /* outputVisible */
                                             true,  /* commandVisible */
                                             SSH.DEFAULT_COMMAND_TIMEOUT);
            i++;
        }
        for (final ExecCommandThread t : threads) {
            try {
                t.join(0);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        i = 0;
        for (final Host host : hosts) {
            final JCheckBox hcb = hostCbs.get(host);
            if (i == 0 || results[0].equals(results[i])) {
                hcb.setSelected(true);
            } else {
                errorPanel.setText(
                          Tools.getString("Dialog.EditConfig.DifferentFiles"));
            }
            i++;
        }
        i = 0;
        for (final Host host : hosts) {
            final int index = i;
            final JCheckBox hcb = hostCbs.get(host);
            hcb.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setConfigArea(
                                     results[index],
                                     errors[index],
                                     e.getStateChange() == ItemEvent.SELECTED);
                            if (e.getStateChange() == ItemEvent.SELECTED) {
                                for (final Host h : hosts) {
                                    final JCheckBox thiscb = hostCbs.get(h);
                                    if (h == host) {
                                        thiscb.setBackground(
                                            Tools.getDefaultColor(
                                             "ConfigDialog.Background.Darker"));
                                    } else {
                                        thiscb.setBackground(
                                            Tools.getDefaultColor(
                                              "ConfigDialog.Background.Light"));
                                    }
                                }
                            }
                        }
                    });
                }
            });
        }
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                setConfigArea(results[0], errors[0], true);
            }
        });
        final Document caDocument = configArea.getDocument();
        caDocument.addDocumentListener(new DocumentListener() {
            private void update() {
                if (!configInProgress) {
                    buttonClass(saveButton()).setEnabled(true);
                }
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                update();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                update();
            }
        });

    }

    /** Gets the title of the dialog as string. */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.EditConfig.Title")
               + ' ' + file;
    }

    /**
     * Returns description for dialog. This can be HTML defined in
     * TextResource.
     */
    @Override
    protected String getDescription() {
        return "";
    }

    /** One default button from the buttons() method. */
    @Override
    protected String defaultButton() {
        return cancelButton();
    }

    /** Returns localized string for Save button. */
    private String saveButton() {
        return buttonString("Save");
    }

    @Override
    protected String[] buttons() {
        return new String[]{saveButton(), cancelButton()};
    }

    /**
     * Returns icons for buttons in the same order as the buttons are defined.
     */
    @Override
    protected ImageIcon[] getIcons() {
        return new ImageIcon[]{null, null};
    }

    /** Action when save button was pressed. */
    private void saveButtonWasPressed() {
        final String iText = "saving " + file + "...";
        Tools.startProgressIndicator(iText);
        for (final Host host : hosts) {
            if (hostCbs.get(host).isSelected()) {

                host.getSSH().createConfig(configArea.getText(),
                                           file,
                                           "",   /* dir */
                                           null, /* mode */
                                           backupCB.isSelected(), /* backup */
                                           null,   /* pre cmd */
                                           null);  /* post cmd */
            }
        }
        Tools.stopProgressIndicator(iText);
    }

    /** This method is called after user has pushed the button. */
    @Override
    protected ConfigDialog checkAnswer() {
        if (isPressedButton(saveButton())) {
            saveButtonWasPressed();
        }
        return null;
    }
}
