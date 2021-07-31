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

package lcmc.common.ui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.ExecCommandThread;
import lcmc.common.domain.ExecCallback;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.configs.DistResource;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * An implementation of an edit config dialog.
 */
@Named
public final class EditConfig extends ConfigDialog {
    private static final Logger LOG = LoggerFactory.getLogger(EditConfig.class);
    private String fileToEdit;
    private Set<Host> hosts;
    private final Map<Host, JCheckBox> hostCheckBoxes = new HashMap<>();
    private final JCheckBox backupCheckBoxes = new JCheckBox(Tools.getString("Dialog.EditConfig.Backup.Button"));

    private final JTextArea configArea = new JTextArea(Tools.getString("Dialog.EditConfig.Loading"));

    private final JLabel errorMessagePanel = new JLabel();
    /** Whether config area is being filled so that save button,
        doesn't get enabled. */
    private volatile boolean configInProgress = true;
    @Inject
    private ProgressIndicator progressIndicator;
    @Inject
    private SwingUtils swingUtils;

    public void init(final String fileToEdit, final Set<Host> hosts) {
        this.fileToEdit = fileToEdit;
        this.hosts = hosts;
        addToOptions(backupCheckBoxes);
    }

    /** Returns the content of the edit config dialog. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        final JPanel hostsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        errorMessagePanel.setForeground(Color.RED);
        backupCheckBoxes.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));
        hostsPanel.add(backupCheckBoxes);
        for (final Host host : hosts) {
            final JCheckBox hcb = new JCheckBox(host.getName());
            hostCheckBoxes.put(host, hcb);
            hcb.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));

            hostsPanel.add(hcb);
        }
        hostsPanel.add(errorMessagePanel);
        pane.add(hostsPanel);

        pane.add(configArea);
        final JScrollPane sp = new JScrollPane(pane);
        sp.setPreferredSize(sp.getMaximumSize());
        return sp;
    }


    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        final FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.TRAILING);

        if (buttonClass(cancelButton()) != null) {
            buttonClass(cancelButton()).getParent().setLayout(layout);
        }
        buttonClass(saveButton()).setEnabled(false);
        enableComponents();
    }

    private void setConfigArea(final String text, final int errorCode, final boolean selected) {
        if (selected) {
            final String config;
            if (errorCode == 0) {
                config = text;
            } else {
                errorMessagePanel.setText("WARNING: " + text);
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
                        final String text = answer.replaceAll("\r", "").replaceFirst("\n$", "");
                        results[index] = text;
                        errors[index] = 0;
                    }

                    @Override
                    public void doneError(final String answer, final int errorCode) {
                        results[index] = answer;
                        errors[index] = errorCode;
                        LOG.sshError(host, "", answer, "", errorCode);
                    }

                };
            threads[i] = host.execCommand(new ExecCommandConfig().command(DistResource.SUDO + "cat " + fileToEdit)
                                                                 .execCallback(execCallback));
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
            final JCheckBox hcb = hostCheckBoxes.get(host);
            if (i == 0 || results[0].equals(results[i])) {
                hcb.setSelected(true);
            } else {
                errorMessagePanel.setText(Tools.getString("Dialog.EditConfig.DifferentFiles"));
            }
            i++;
        }
        i = 0;
        for (final Host host : hosts) {
            final int index = i;
            final JCheckBox hcb = hostCheckBoxes.get(host);
            hcb.addItemListener(e -> swingUtils.invokeLater(() -> {
                setConfigArea(results[index], errors[index], e.getStateChange() == ItemEvent.SELECTED);
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    for (final Host h : hosts) {
                        final JCheckBox thiscb = hostCheckBoxes.get(h);
                        if (h == host) {
                            thiscb.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Darker"));
                        } else {
                            thiscb.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));
                        }
                    }
                }
            }));
        }
        swingUtils.invokeLater(() -> setConfigArea(results[0], errors[0], true));
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

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.EditConfig.Title") + ' ' + fileToEdit;
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

    private void saveButtonWasPressed() {
        final String iText = "saving " + fileToEdit + "...";
        progressIndicator.startProgressIndicator(iText);
        for (final Host host : hosts) {
            if (hostCheckBoxes.get(host).isSelected()) {
                host.getSSH().createConfig(configArea.getText(),
                                           fileToEdit,
                                           "",   /* dir */
                                           null, /* mode */
                                           backupCheckBoxes.isSelected(), /* backup */
                                           null,   /* pre cmd */
                                           null);  /* post cmd */
            }
        }
        progressIndicator.stopProgressIndicator(iText);
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
