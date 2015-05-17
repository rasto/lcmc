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
package lcmc.cluster.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import lcmc.common.ui.ProgressBar;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;

/**
 * An implementation of dialogs that are needed for establishing of a ssh
 * connection.
 */
public final class SSHGui {
    private static final Logger LOG = LoggerFactory.getLogger(SSHGui.class);
    private static final int DEFAULT_FIELD_LENGTH = 20;
    /** Root pane on which the dialogs are comming to. */
    private final Container rootPane;
    private final Host host;
    private final ProgressBar progressBar;

    public SSHGui(final Container rootPane, final Host host, final ProgressBar progressBar) {
        super();
        this.rootPane = rootPane;
        this.host = host;
        this.progressBar = progressBar;
    }

    /** Displays Confirm Dialog whith Yes, No, Cancel options. */
    public int getConfirmDialogChoice(final String message) {
        LOG.debug("getConfirmDialogChoice: start");
        return JOptionPane.showConfirmDialog(rootPane, message);
    }

    /** Checks if choice is yes option. */
    public boolean isConfirmDialogYes(final int choice) {
        return choice == JOptionPane.YES_OPTION;
    }

    /** Checks if choice is cancel option. */
    public boolean isConfirmDialogCancel(final int choice) {
        return choice == JOptionPane.CANCEL_OPTION;
    }

    /** Creates dialog with some text or password field to enter by user. */
    public String enterSomethingDialog(final String title,
                                       final String[] content,
                                       final String underText,
                                       final String defaultValue,
                                       final boolean isPassword) {
        final EnterSomethingDialog esd;
        if (rootPane instanceof JDialog) {
            esd = new EnterSomethingDialog((JDialog) rootPane, title, content, underText, defaultValue, isPassword);
        } else if (rootPane instanceof JApplet) {
            esd = new EnterSomethingDialog((JApplet) rootPane, title, content, underText, defaultValue, isPassword);
        } else {
            esd = new EnterSomethingDialog((Frame) rootPane, title, content, underText, defaultValue, isPassword);
        }

        esd.setVisible(true);

        return esd.answer;
    }

    /**
     * This dialog displays a number of text lines and a text field.
     * The text field can either be plain text or a password field.
     */
    private class EnterSomethingDialog extends JDialog {
        private JTextField answerField;
        private JPasswordField passwordField;
        /** Whether there is password field. */
        private boolean isPassword;
        /** User answer. */
        private String answer;

        EnterSomethingDialog(final JDialog parent,
                             final String title,
                             final String[] content,
                             final String underText,
                             final String defaultValue,
                             final boolean isPasswordA) {
            super(parent, title, true);
            init(content, underText, defaultValue, isPasswordA);
            setLocationRelativeTo(parent);
        }

        EnterSomethingDialog(final Frame parent,
                             final String title,
                             final String[] content,
                             final String underText,
                             final String defaultValue,
                             final boolean isPasswordA) {
            super(parent, title, true);
            init(content, underText, defaultValue, isPasswordA);
            setLocationRelativeTo(parent);
        }

        EnterSomethingDialog(final JApplet parent,
                             final String title,
                             final String[] content,
                             final String underText,
                             final String defaultValue,
                             final boolean isPasswordA) {
            super((Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent), title, true);
            init(content, underText, defaultValue, isPasswordA);
            setLocationRelativeTo(parent);
        }

        private void init(final String[] content,
                          final String underText,
                          final String defaultValue,
                          final boolean isPassword) {
            this.isPassword = isPassword;
            final List<String> strippedContent = new ArrayList<String>();
            for (final String s : content) {
                if (s != null && !s.isEmpty()) {
                    /* strip some html */
                    strippedContent.add(s.replaceAll("\\<.*?\\>", "").replaceAll("&nbsp;", " "));
                }
            }
            host.getTerminalPanel().addCommandOutput(strippedContent.toArray(new String[strippedContent.size()]));

            if (progressBar != null) {
                progressBar.hold();
            }

            final JPanel pan = new JPanel();
            pan.setBorder(new LineBorder(Tools.getDefaultColor("ConfigDialog.Background.Light"), 5));
            pan.setBackground(Color.WHITE);
            pan.setLayout(new BoxLayout(pan, BoxLayout.PAGE_AXIS));
            if (host != null && host.getName() != null) {
                pan.add(new JLabel("host: " + host.getName()));
            }

            for (final String el : content) {
                if (el == null || el.isEmpty()) {
                    continue;
                }
                final JLabel contentLabel = new JLabel(el);
                pan.add(contentLabel);
            }

            answerField = new JTextField(DEFAULT_FIELD_LENGTH);
            passwordField = new JPasswordField(DEFAULT_FIELD_LENGTH);

            if (isPassword) {
                if (defaultValue != null) {
                    passwordField.setText(defaultValue);
                    passwordField.selectAll();
                }
                pan.add(passwordField);
            } else {
                if (defaultValue != null) {
                    answerField.setText(defaultValue);
                    answerField.selectAll();
                }
                pan.add(answerField);
            }

            final KeyListener kl = new KeyAdapter() {
                @Override
                public void keyTyped(final KeyEvent e) {
                    if (e.getKeyChar() == '\n') {
                        finish();
                    }
                }
            };

            answerField.addKeyListener(kl);
            passwordField.addKeyListener(kl);

            getContentPane().add(BorderLayout.CENTER, pan);
            if (underText != null) {
                final JLabel l = new JLabel(underText);
                final Font font = l.getFont();
                final String name = font.getFontName();
                final int style = Font.ITALIC;
                final int size = font.getSize();
                l.setFont(new Font(name, style, size - 3));
                l.setForeground(Color.GRAY);
                pan.add(l);
            }
            setResizable(false);
            pack();
        }

        private void finish() {
            if (isPassword) {
                answer = new String(passwordField.getPassword());
            } else {
                answer = answerField.getText();
            }

            if (progressBar != null) {
                progressBar.cont();
            }
            host.getTerminalPanel().addCommandOutput("\n");
            dispose();
        }
    }
}
