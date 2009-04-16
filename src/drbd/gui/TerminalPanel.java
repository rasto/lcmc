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


package drbd.gui;

import drbd.data.Host;
import drbd.utilities.Tools;
import drbd.utilities.ExecCallback;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.StyleConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;

import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultCaret;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.plaf.TextUI;

import java.awt.Font;
import java.awt.BorderLayout;

/**
 * An implementation of a terminal panel that show commands and output from
 * remote host. It is also possible to write commands and execute them.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class TerminalPanel extends JScrollPane {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Host data object. */
    private final Host host;
    /** Text pane with terminal area. */
    private final JTextPane terminalArea;
    /** Color for commands. */
    private final MutableAttributeSet commandColor;
    /** Color for errors. */
    private final MutableAttributeSet errorColor;
    /** Color for output. */
    private final MutableAttributeSet outputColor;
    /** Color for prompt. */
    private final MutableAttributeSet promptColor;
    /** Offset of a command. */
    private int commandOffset = 0;
    /** User command. */
    private boolean userCommand = false;
    /** Whether typing in the commands is enabled. */
    private boolean editEnabled = false;

    /**
     * Prepares a new <code>TerminalPanel</code> object.
     */
    public TerminalPanel(final Host host) {
        super();
        this.host = host;
        host.setTerminalPanel(this);
        final Font f = new Font("Monospaced", Font.PLAIN, 14);

        terminalArea = new JTextPane();
        terminalArea.setStyledDocument(new MyDocument());
        final DefaultCaret caret = new DefaultCaret() {
            private static final long serialVersionUID = 1L;

            public void paint(final Graphics g) {
                /* painting cursor. If it is not visible it is out of focus, we
                 * make it barely visible. */
                try {
                    TextUI mapper = getComponent().getUI();
                    Rectangle r = mapper.modelToView(getComponent(),
                                                     getDot(),
                                                     getDotBias());
                    if (r == null) {
                        return;
                    }
                    g.setColor(getComponent().getCaretColor());
                    if (isVisible() && editEnabled) {
                        g.fillRect(r.x,
                                   r.y,
                                   8,
                                   r.height);
                    } else {
                        g.drawRect(r.x,
                                   r.y,
                                   8,
                                   r.height);
                    }
                } catch (BadLocationException e) {
                    Tools.appError("Drawing of cursor failed", e);
                }
            }
        };
        terminalArea.setCaret(caret);
        terminalArea.addCaretListener(new CaretListener() {
            public void caretUpdate(final CaretEvent e) {
                // don't do this if caret moved because of selection
                if (e != null) {
                    if (e.getDot() < commandOffset && e.getDot() == e.getMark()) {
                        terminalArea.setCaretPosition(commandOffset);
                    }
                }
            }
        });

        // set font and colors
        terminalArea.setFont(f);
        terminalArea.setBackground(
            Tools.getDefaultColor("TerminalPanel.Background"));

        commandColor = new SimpleAttributeSet();
        StyleConstants.setForeground(commandColor,
            Tools.getDefaultColor("TerminalPanel.Command"));

        errorColor = new SimpleAttributeSet();
        StyleConstants.setForeground(errorColor,
            Tools.getDefaultColor("TerminalPanel.Error"));

        outputColor = new SimpleAttributeSet();
        StyleConstants.setForeground(outputColor,
            Tools.getDefaultColor("TerminalPanel.Output"));

        promptColor = new SimpleAttributeSet();
        StyleConstants.setForeground(promptColor, host.getColor());
        //    Tools.getDefaultColor("TerminalPanel.Prompt"));

        append(prompt(), promptColor);
        terminalArea.setEditable(true);
        //final Host thisHost = host;
        //terminalArea.getDocument().addDocumentListener(
        //    new DocumentListener() {
        //        public void insertUpdate(DocumentEvent e) {
        //            if (userCommand) {
        //                Document d = e.getDocument();
        //                int o = e.getOffset();
        //                int l = e.getLength();
        //                try {
        //                    String letter = d.getText(o, l);
        //                    if (letter.equals("\n")) {
        //                        String command =
        //                                  d.getText(commandOffset,
        //                                            o - commandOffset).trim();
        //                        execCommand(command);
        //                    }
        //                } catch (Exception de) {
        //                }
        //            }
        //        }

        //        public void removeUpdate(DocumentEvent e) {
        //        }

        //        public void changedUpdate(DocumentEvent e) {
        //        }
        //    }
        //);

        getViewport().add(terminalArea, BorderLayout.PAGE_END);

    }

    /**
     * Appends a text whith specified color to the terminal area.
     */
    private void append(final String text, final MutableAttributeSet color) {
        userCommand = false;
        final MyDocument doc = (MyDocument) terminalArea.getStyledDocument();
        //int start = doc.getLength();
        final int start = terminalArea.getCaretPosition();
        try {
            doc.insertString(start, text, color);

            //byte[] bytes = text.getBytes(); // TODO: backspace handling
            //int i = 0;
            //int j = 0;
            //for (byte b : bytes) {
            //    if (b == 8) {
            //        doc.removeForced(start + j - 1, 2);
            //        j = j - 2;
            //    //} else if (b == 13 && bytes[i + 1] == 10) { // new line
            //    //    prev_line = start + j;
            //    //} else if (b == 13) { // beginning of the same line
            //        doc.removeForced(prev_line, doc.getLength() - prev_line);
            //    //    j = prev_line;
            //    }
            //    i++;
            //    j++;
            //}
        } catch (Exception e) {
            Tools.appError("TerminalPanel.getLength", "", e);
        }

        //terminalArea.setCaretPosition(terminalArea.getText().length());
        commandOffset = terminalArea.getDocument().getLength();
        userCommand = true;
    }

    /**
     * Sets the terminal area editable.
     */
    public final void setEditable(final boolean editable) {
        terminalArea.setEditable(editable);
    }

    /**
     * Executes the specified command, if the host is connected.
     */
    public final void execCommand(final String command) {
        final String hostName = host.getName();

        if (!host.isConnected()) {
            return;
        }
        if (!"".equals(command)) {
            Tools.startProgressIndicator(hostName, "Executing command");
        }
        host.execCommandRaw(command,
             new ExecCallback() {
                 public void done(final String ans) {
                     //try {
                     //    Thread.sleep(1000);
                     //} catch (InterruptedException e) {
                     //    Tools.appError("sleep was interrupted.");
                     //}
                     if (!"".equals(command)) {
                        Tools.stopProgressIndicator(hostName,
                                                    "Executing command");
                     }
                 }

                 public void doneError(final String ans, final int exitCode) {
                     if (!"".equals(command)) {
                        Tools.stopProgressIndicator(hostName,
                                                    "Executing command");
                     }
                 }
             },
             true, false);
    }

    /**
     * Sets the prompt color to the host's color when it is added to the
     * cluster.
     */
    public final void resetPromptColor() {
        StyleConstants.setForeground(promptColor, host.getColor());
        addCommand("");
        nextCommand();
    }

    /**
     * Returns a prompt.
     */
    private String prompt() {
        if (host.isConnected()) {
            return "[" + host.getUserAtHost() + ":~#] ";
        } else {
            return "# ";
        }
    }

    /**
     * Is called after execution of command is finnished. It shows the prompt
     * and scrolls the text up.
     */
    public final void nextCommand() {
        append(prompt(), promptColor);
    }

    /**
     * Adds command to the terminal textarea and scrolls up.
     */
    public final void addCommand(final String command) {
        final String[] lines = command.split("\\r?\\n");

        append(lines[0], commandColor);
        for (int i = 1; i < lines.length; i++) {

            append(" \\\n> " + lines[i], commandColor);
        }
        append("\n", commandColor);
    }

    /**
     * Adds command output to the terminal textarea and scrolls up.
     */
    public final void addCommandOutput(final String output) {
        append(output, outputColor);
    }

    /**
     * Adds array of command output to the terminal textarea and scrolls up.
     */
    public final void addCommandOutput(final String[] output) {
        for (int i = 0; i < output.length; i++) {
            if (output[i] != null) {
                String newLine = "";
                if (i != output.length - 1) {
                    newLine = "\n";
                }
                append(output[i] + newLine, outputColor);
            }
        }
    }

    ///**
    // * Adds content to the terminal textarea and scrolls up.
    // */
    //public void addContent(byte[] data, int len) {
    //    StringBuffer text = new StringBuffer("");
    //    for (int i = 0; i < len; i++) {
    //        char c = (char) (data[i] & 0xFF);
    //        if (c == 8 - 127) { //backspace
    //            text.append("backspace");
    //        }
    //        text.append(c);
    //    }
    //    append(text.toString(), outputColor);
    //}

    /**
     * Adds content string (output of a command) to the terminal area.
     */
    public final void addContent(final String c) {
        append(c, outputColor);
    }

    /**
     * Adds content to the terminal textarea and scrolls up.
     */
    public final void addContentErr(final byte[] data, final int len) {
        final StringBuffer text = new StringBuffer("");
        for (int i = 0; i < len; i++) {
            final char c = (char) (data[i] & 0xFF);
            text.append(c);
        }
        append(text.toString(), errorColor);
    }

    /**
     * This class overwrites the DefaultStyledDocument in order to add godmode
     * feature.
     */
    public class MyDocument extends DefaultStyledDocument {
        /** Serial version UID. */
        private static final long serialVersionUID = 1L;
        /** Command to turn on the god mode. */
        private static final String COMMAND_CHEAT_ON  = "godmode";
        /** Command to turn off the god mode. */
        private static final String COMMAND_CHEAT_OFF = "nogodmode";
        /** Position while writing the cheat code. */
        private int cheatPos = 0;

        /**
         * Is called while a string is inserted. It checks if a cheat code is
         * in the string. */
        public final void insertString(int offs,
                                       final String s,
                                       final AttributeSet a)
            throws BadLocationException {
            if (offs < commandOffset) {
                terminalArea.setCaretPosition(commandOffset);
                offs = commandOffset;
            }
            if (userCommand) {
                String cheatCode;
                if (editEnabled) {
                    super.insertString(offs, s, commandColor);
                    if ("\n".equals(s)) {
                        final String command =
                                        getText(commandOffset,
                                                offs - commandOffset).trim();
                        execCommand(command);
                    }
                    cheatCode = COMMAND_CHEAT_OFF;
                } else {
                    cheatCode = COMMAND_CHEAT_ON;
                }
                if (s.equals(cheatCode.substring(cheatPos, cheatPos + 1))) {
                    cheatPos++;
                    if (cheatPos == cheatCode.length()) {
                        editEnabled = !editEnabled;
                        Tools.getGUIData().godModeChanged(editEnabled);
                        cheatPos = 0;
                    }
                } else {
                    cheatPos = 0;
                }
            } else {
                super.insertString(offs, s, a);
            }
        }

        /**
         * Is called while characters is removed.
         */
        public final void remove(final int offs,
                                 final int len) throws BadLocationException {
            if (offs >= commandOffset) {
                if (cheatPos > 0) {
                    cheatPos--;
                }
                if (editEnabled) {
                    super.remove(offs, len);
                }
            }
        }

        /**
         * Same as remove.
         */
        public final void removeForced(final int offs,
                                       final int len)
        throws BadLocationException {
            super.remove(offs, len);
        }
    }
}
