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


package lcmc.gui;

import lcmc.data.Host;
import lcmc.utilities.Tools;
import lcmc.utilities.ExecCallback;
import lcmc.robotest.RoboTest;
import lcmc.utilities.SSH;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import java.awt.Dimension;

import javax.swing.plaf.TextUI;

import java.awt.Font;
import java.awt.Color;
import java.awt.BorderLayout;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * An implementation of a terminal panel that show commands and output from
 * remote host. It is also possible to write commands and execute them.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class TerminalPanel extends JScrollPane {
    /** Logger. */
    private static final Logger LOG =
                                LoggerFactory.getLogger(TerminalPanel.class);
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
    /** Begining of the previous line. */
    private int prevLine = 0;
    /** Position of the cursor in the text. */
    private int pos = 0;
    /** Position in terminal area lock. */
    private final Lock mPosLock = new ReentrantLock();
    /** Maximum position of the cursor in the text. */
    private int maxPos = 0;
    /** Terminal output colors. */
    private final Map<String, Color> terminalColor =
                                            new HashMap<String, Color>();
    /** Default text color of the output in the terminal. */
    private final Color defaultOutputColor;

    /** Command to list all the cheats. */
    private static final String CHEAT_LIST  = "cheatlist";
    /** Command to turn off the god mode. */
    private static final String GOD_OFF = "nogodmode";
    /** Command to turn on the god mode. */
    private static final String GOD_ON  = "godmode";
    /** Command to run gc. */
    private static final String RUN_GC  = "rungc";
    /** Allocate 10MB of memory for 1 minute. */
    private static final String ALLOCATE_10 = "allocate10";
    /** Command to start frenzy clicking for short period. */
    private static final String CLICKTEST_SHORT = "lclicksh";
    /** Command to start frenzy clicking for longer period. */
    private static final String CLICKTEST_LONG = "lclicklo";
    /** Command to start lazy clicking for short period. */
    private static final String CLICKTEST_LAZY_SHORT = "lclicklazysh";
    /** Command to start lazy clicking for longer period. */
    private static final String CLICKTEST_LAZY_LONG = "lclicklazylo";
    /** Command to start frenzy rigth clicking for short period. */
    private static final String RIGHT_CLICKTEST_SHORT = "rclicksh";
    /** Command to start frenzy rigth clicking for longer period. */
    private static final String RIGHT_CLICKTEST_LONG = "rclicklo";
    /** Command to start lazy rigth clicking for short period. */
    private static final String RIGHT_CLICKTEST_LAZY_SHORT =
                                                    "rclicklazysh";
    /** Command to start lazy rigth clicking for longer period. */
    private static final String RIGHT_CLICKTEST_LAZY_LONG =
                                                        "rclicklazylo";
    /** Command to start short mouse moving. */
    private static final String MOVETEST_SHORT = "movetestsh";
    /** Command to start mouse moving. */
    private static final String MOVETEST_LONG = "movetestlo";
    /** Command to start mouse moving. */
    private static final String MOVETEST_LAZY_SHORT = "movetestlazysh";
    /** Command to start mouse moving. */
    private static final String MOVETEST_LAZY_LONG = "movetestlazylo";
    /** Command to increment debug level. */
    private static final String DEBUG_INC = "debuginc";
    /** Command to decrement debug level. */
    private static final String DEBUG_DEC = "debugdec";
    /** Starts tests. */
    private static final Map<String, RoboTest.Test> TESTS =
                                          new HashMap<String, RoboTest.Test>();
    static {
        for (final RoboTest.Type type : new RoboTest.Type[]{
                                                          RoboTest.Type.PCMK,
                                                          RoboTest.Type.DRBD,
                                                          RoboTest.Type.VM,
                                                          RoboTest.Type.GUI}) {
            for (final char index : new Character[]{
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'}) {
                TESTS.put(type.getTestName() + index,
                          new RoboTest.Test(type, index));
            }
        }
    }
    /** Register mouse movement. */
    private static final String REGISTER_MOVEMENT = "registermovement";
    /** List of cheats, with positions while typing them. */
    private static final Map<String, Integer> CHEATS_MAP =
                                     new LinkedHashMap<String, Integer>();
    static {
        CHEATS_MAP.put(CHEAT_LIST, 0);
        CHEATS_MAP.put(GOD_OFF, 0); /* off must be before on */
        CHEATS_MAP.put(GOD_ON, 0);
        CHEATS_MAP.put(RUN_GC, 0);
        CHEATS_MAP.put(ALLOCATE_10, 0);
        CHEATS_MAP.put(CLICKTEST_SHORT, 0);
        CHEATS_MAP.put(CLICKTEST_LONG, 0);
        CHEATS_MAP.put(CLICKTEST_LAZY_SHORT, 0);
        CHEATS_MAP.put(CLICKTEST_LAZY_LONG, 0);
        CHEATS_MAP.put(RIGHT_CLICKTEST_SHORT, 0);
        CHEATS_MAP.put(RIGHT_CLICKTEST_LONG, 0);
        CHEATS_MAP.put(RIGHT_CLICKTEST_LAZY_SHORT, 0);
        CHEATS_MAP.put(RIGHT_CLICKTEST_LAZY_LONG, 0);
        CHEATS_MAP.put(MOVETEST_SHORT, 0);
        CHEATS_MAP.put(MOVETEST_LONG, 0);
        CHEATS_MAP.put(MOVETEST_LAZY_SHORT, 0);
        CHEATS_MAP.put(MOVETEST_LAZY_LONG, 0);
        CHEATS_MAP.put(DEBUG_INC, 0);
        CHEATS_MAP.put(DEBUG_DEC, 0);
        for (final String test : TESTS.keySet()) {
            CHEATS_MAP.put(test, 0);
        }
        CHEATS_MAP.put(REGISTER_MOVEMENT, 0);
    }


    /** Prepares a new <code>TerminalPanel</code> object. */
    public TerminalPanel(final Host host) {
        super();
        this.host = host;
        host.setTerminalPanel(this);
        /* Sets terminal some of the output colors. This is in no way complete
         * or correct and probably doesn't have to be. */
        terminalColor.put("0",
                          Tools.getDefaultColor("TerminalPanel.TerminalWhite"));
        terminalColor.put("30",
                          Tools.getDefaultColor("TerminalPanel.TerminalBlack"));
        terminalColor.put("31",
                          Tools.getDefaultColor("TerminalPanel.TerminalRed"));
        terminalColor.put("32",
                          Tools.getDefaultColor("TerminalPanel.TerminalGreen"));
        terminalColor.put("33",
                         Tools.getDefaultColor("TerminalPanel.TerminalYellow"));
        terminalColor.put("34",
                          Tools.getDefaultColor("TerminalPanel.TerminalBlue"));
        terminalColor.put("35",
                         Tools.getDefaultColor("TerminalPanel.TerminalPurple"));
        terminalColor.put("36",
                          Tools.getDefaultColor("TerminalPanel.TerminalCyan"));
        final Font f = new Font("Monospaced",
                                Font.PLAIN,
                                Tools.getConfigData().scaled(14));
        terminalArea = new JTextPane();
        terminalArea.setStyledDocument(new MyDocument());
        final DefaultCaret caret = new DefaultCaret() {
            private static final long serialVersionUID = 1L;

            @Override
            protected synchronized void damage(final Rectangle r) {
                if (r != null) {
                    x = r.x;
                    y = r.y;
                    width = 8;
                    height = r.height;
                    repaint();
                }
            }

            @Override
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
                    LOG.appError("paint: drawing of cursor failed", e);
                }
            }
        };
        terminalArea.setCaret(caret);
        terminalArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(final CaretEvent e) {
                /* don't do this if caret moved because of selection */
                mPosLock.lock();
                try {
                    if (e != null
                        && e.getDot() < commandOffset
                        && e.getDot() == e.getMark()) {
                        terminalArea.setCaretPosition(commandOffset);
                    }
                } finally {
                    mPosLock.unlock();
                }
            }
        });

        /* set font and colors */
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
        defaultOutputColor = Tools.getDefaultColor("TerminalPanel.Output");
        StyleConstants.setForeground(outputColor, defaultOutputColor);

        promptColor = new SimpleAttributeSet();
        StyleConstants.setForeground(promptColor, host.getPmColors()[0]);

        append(prompt(), promptColor);
        terminalArea.setEditable(true);
        getViewport().add(terminalArea, BorderLayout.PAGE_END);
        setPreferredSize(new Dimension(
                    Short.MAX_VALUE,
                    Tools.getDefaultInt("MainPanel.TerminalPanelHeight")));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
    }

    /** Returns terminal output color. */
    private Color getColorFromString(final String s) {
        /* "]" default color */
        if ("[".equals(s)) {
            return null;
        }
        final Pattern p1 = Pattern.compile("^\\[\\d+;(\\d+)$");
        final Matcher m1 = p1.matcher(s);
        if (m1.matches()) {
            final String c = m1.group(1);
            /* can be null */
            return terminalColor.get(c);
        }
        /* [0;1;32 */
        final Pattern p2 = Pattern.compile("^\\[\\d+;\\d+;(\\d+)$");
        final Matcher m2 = p2.matcher(s);
        if (m2.matches()) {
            final String c = m2.group(1);
            /* can be null */
            return terminalColor.get(c);
        }
        return null;
    }

    /** Get char count. */
    private int getCharCount(final String s) {
        final Pattern p1 = Pattern.compile("^\\[(\\d+)$");
        final Matcher m1 = p1.matcher(s);
        if (m1.matches()) {
            return Integer.parseInt(m1.group(1));
        }
        return 0;
    }


    /** Appends a text whith specified color to the terminal area. */
    private void append(final String text,
                        final MutableAttributeSet colorAS) {
        userCommand = false;
        final MyDocument doc = (MyDocument) terminalArea.getStyledDocument();
        mPosLock.lock();
        final int end = terminalArea.getDocument().getLength();
        pos = end + pos - maxPos;
        maxPos = end;
        final char[] chars = text.toCharArray();
        StringBuilder colorString = new StringBuilder(10);
        boolean inside = false;
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            final String s = Character.toString(c);
            boolean printit = true;
            if (c == 8) { /* one position to the left */
                printit = false;
                pos--;
            } else if (i < chars.length - 1
                       && c == 13 && chars[i + 1] == 10) { /* new line */
                prevLine = maxPos + 2;
                pos = maxPos;
            } else if (c == 13) { /* beginning of the same line */
                pos = prevLine;
                printit = false;
            } else if (c == 27) {
                /* funny colors, e.g. in sles */
                inside = true;
                printit = false;
                colorString = new StringBuilder(10);
            }
            if (inside) {
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    /* we are done */
                    inside = false;
                    if (c == 'm') {
                        Color newColor =
                                     getColorFromString(colorString.toString());

                        if (newColor == null) {
                            newColor = defaultOutputColor;
                        }
                        StyleConstants.setForeground(colorAS, newColor);
                        colorString = new StringBuilder(10);
                    } else if (c == 'G') {
                        final int g = getCharCount(colorString.toString());
                        pos = prevLine + g;
                        while (pos > maxPos) {
                            try {
                                doc.insertString(maxPos,
                                                 " ",
                                                 colorAS);
                                maxPos++;
                            } catch (javax.swing.text.BadLocationException e1) {
                                LOG.appError("append: terminalPanel pos: "
                                             + pos, e1);
                            }
                        }
                    }
                } else if (printit) {
                    colorString.append(s);
                }
                printit = false;
            }

            if (printit) {
                if (pos < maxPos) {
                    try {
                        commandOffset = pos - 1;
                        doc.removeForced(pos, 1);
                    } catch (javax.swing.text.BadLocationException e) {
                        LOG.appError("append: terminalPanel pos: " + pos, e);
                    }
                }
                try {
                    doc.insertString(pos,
                                     s,
                                     colorAS);
                } catch (javax.swing.text.BadLocationException e1) {
                    LOG.appError("append: terminalPanel pos: " + pos, e1);
                }
                pos++;
                if (maxPos < pos) {
                    maxPos = pos;
                }
            }
        }
        commandOffset = terminalArea.getDocument().getLength();
        terminalArea.setCaretPosition(terminalArea.getDocument().getLength());
        mPosLock.unlock();
        userCommand = true;
    }

    /** Sets the terminal area editable. */
    void setEditable(final boolean editable) {
        terminalArea.setEditable(editable);
    }

    /** Executes the specified command, if the host is connected. */
    void execCommand(final String command) {
        final String hostName = host.getName();

        if (!host.isConnected()) {
            return;
        }
        if (!"".equals(command)) {
            Tools.startProgressIndicator(hostName, "Executing command");
        }
        host.execCommandRaw(command,
             new ExecCallback() {
                 @Override
                 public void done(final String ans) {
                     if (!"".equals(command)) {
                        Tools.stopProgressIndicator(hostName,
                                                    "Executing command");
                     }
                 }

                 @Override
                 public void doneError(final String ans, final int exitCode) {
                     if (!"".equals(command)) {
                        Tools.stopProgressIndicator(hostName,
                                                    "Executing command");
                     }
                 }
             },
             true,
             false,
             SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /**
     * Sets the prompt color to the host's color when it is added to the
     * cluster.
     */
    public void resetPromptColor() {
        StyleConstants.setForeground(promptColor, host.getPmColors()[0]);
        addCommand("");
        nextCommand();
    }

    /** Returns a prompt. */
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
    public void nextCommand() {
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                append(prompt(), promptColor);
            }
        });
    }

    /** Adds command to the terminal textarea and scrolls up. */
    public void addCommand(final String command) {
        final String[] lines = command.split("\\r?\\n");

        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                append(lines[0], commandColor);
                for (int i = 1; i < lines.length; i++) {

                    append(" \\\n> " + lines[i], commandColor);
                }
                append("\n", commandColor);
            }
        });
    }

    /** Adds command output to the terminal textarea and scrolls up. */
    public void addCommandOutput(final String output) {
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                append(output, outputColor);
            }
        });
    }

    /** Adds array of command output to the terminal textarea and scrolls up. */
    void addCommandOutput(final String[] output) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    /** Adds content string (output of a command) to the terminal area. */
    public void addContent(final String c) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                append(c, outputColor);
            }
        });
    }

    /** Adds content to the terminal textarea and scrolls up. */
    public void addContentErr(final String c) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                append(c, errorColor);
            }
        });
    }


    /**
     * This class overwrites the DefaultStyledDocument in order to add godmode
     * feature.
     */
    class MyDocument extends DefaultStyledDocument {
        /** Serial version UID. */
        private static final long serialVersionUID = 1L;

        /**
         * Is called while a string is inserted. It checks if a cheat code is
         * in the string. */
        @Override
        public void insertString(int offs,
                                 final String s,
                                 final AttributeSet a)
            throws BadLocationException {
            mPosLock.lock();
            if (offs < commandOffset) {
                terminalArea.setCaretPosition(commandOffset);
                offs = commandOffset;
            }
            if (userCommand) {
                if (editEnabled) {
                    if (s.charAt(s.length() - 1) == '\n') {
                        final int end = terminalArea.getDocument().getLength();
                        super.insertString(end, "\n", commandColor);
                        final String command =
                                (getText(commandOffset,
                                         end - commandOffset) + s).trim();
                        prevLine = end + 1;
                        pos = end;
                        maxPos = end;
                        execCommand(command);
                    } else {
                        super.insertString(offs, s, commandColor);
                    }
                }
                for (final String cheat : CHEATS_MAP.keySet()) {
                    int cheatPos = CHEATS_MAP.get(cheat);
                    if (s.equals(cheat.substring(cheatPos, cheatPos + 1))) {
                        cheatPos++;
                        CHEATS_MAP.put(cheat, cheatPos);
                        if (cheatPos == cheat.length()) {
                            for (final String ch : CHEATS_MAP.keySet()) {
                                CHEATS_MAP.put(ch, 0);
                            }
                            startCheat(cheat);
                        }
                    } else {
                        CHEATS_MAP.put(cheat, 0);
                    }
                }
            } else {
                super.insertString(offs, s, a);
            }
            mPosLock.unlock();
        }

        /** Is called while characters is removed. */
        @Override
        public void remove(final int offs,
                           final int len) throws BadLocationException {
            mPosLock.lock();
            try {
                if (offs >= commandOffset) {
                    for (final String cheat : CHEATS_MAP.keySet()) {
                        final int cheatPos = CHEATS_MAP.get(cheat);
                        if (cheatPos > 0) {
                            CHEATS_MAP.put(cheat, cheatPos - 1);
                        }
                    }
                    if (editEnabled) {
                        super.remove(offs, len);
                    }
                }
            } finally {
                mPosLock.unlock();
            }
        }

        /** Same as remove. */
        void removeForced(final int offs, final int len)
        throws BadLocationException {
            super.remove(offs, len);
        }
    }

    /** Starts action after cheat was entered. */
    private void startCheat(final String cheat) {
        if (!editEnabled) {
            addCommand(cheat);
        }
        if (!editEnabled && GOD_ON.equals(cheat)) {
            editEnabled = true;
            Tools.getGUIData().godModeChanged(editEnabled);
        } else if (editEnabled && GOD_OFF.equals(cheat)) {
            editEnabled = false;
            Tools.getGUIData().godModeChanged(editEnabled);
        } else if (CHEAT_LIST.equals(cheat)) {
            final StringBuilder list = new StringBuilder();
            for (final String ch : CHEATS_MAP.keySet()) {
                list.append(ch);
                list.append('\n');
            }
            addCommandOutput(list.toString());
        } else if (RUN_GC.equals(cheat)) {
            System.gc();
            LOG.info("startCheat: run gc");
        } else if (ALLOCATE_10.equals(cheat)) {
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    LOG.info("startCheat: allocate mem");
                    Byte[] b = new Byte[1024000];
                    LOG.info("startCheat: allocate mem done");

                    System.gc();
                    LOG.info("startCheat: run gc");
                    Tools.sleep(60000);
                    LOG.info("startCheat: free mem");
                }
            });
            t.start();
        } else if (CLICKTEST_SHORT.equals(cheat)) {
            RoboTest.startClicker(1, false);
        } else if (CLICKTEST_LONG.equals(cheat)) {
            RoboTest.startClicker(8 * 60, false);
        } else if (CLICKTEST_LAZY_SHORT.equals(cheat)) {
            RoboTest.startClicker(1, true);
        } else if (CLICKTEST_LAZY_LONG.equals(cheat)) {
            RoboTest.startClicker(8 * 60, true); /* 8 hours */
        } else if (RIGHT_CLICKTEST_SHORT.equals(cheat)) {
            RoboTest.startRightClicker(1, false);
        } else if (RIGHT_CLICKTEST_LONG.equals(cheat)) {
            RoboTest.startRightClicker(8 * 60, false);
        } else if (RIGHT_CLICKTEST_LAZY_SHORT.equals(cheat)) {
            RoboTest.startRightClicker(1, true);
        } else if (RIGHT_CLICKTEST_LAZY_LONG.equals(cheat)) {
            RoboTest.startRightClicker(8 * 60, true); /* 8 hours */
        } else if (MOVETEST_SHORT.equals(cheat)) {
            RoboTest.startMover(1, false);
        } else if (MOVETEST_LONG.equals(cheat)) {
            RoboTest.startMover(8 * 60, false);
        } else if (MOVETEST_LAZY_SHORT.equals(cheat)) {
            RoboTest.startMover(1, true);
        } else if (MOVETEST_LAZY_LONG.equals(cheat)) {
            RoboTest.startMover(8 * 60, true);
        } else if (DEBUG_INC.equals(cheat)) {
            LoggerFactory.incrementDebugLevel();
        } else if (DEBUG_DEC.equals(cheat)) {
            LoggerFactory.decrementDebugLevel();
        } else if (TESTS.containsKey(cheat)) {
            RoboTest.startTest(TESTS.get(cheat), host.getCluster());
        } else if (REGISTER_MOVEMENT.equals(cheat)) {
            RoboTest.registerMovement();
        }
        nextCommand();
    }

    /** Reset the terminal are. */
    public void resetTerminalArea() {
        for (int i = 0; i < 10; i++) { /* try 10 times */
            try {
                final MyDocument doc =
                                (MyDocument) terminalArea.getStyledDocument();
                mPosLock.lock();
                try {
                    commandOffset = 0;
                    pos = 0;
                    maxPos = 0;
                    doc.removeForced(0, doc.getLength());
                } finally {
                    mPosLock.unlock();
                }
                return;
            } catch (BadLocationException e) {
                continue;
            }
        }
    }
}
