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

import lcmc.utilities.MyButton;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;

import lcmc.utilities.Tools;
import lcmc.utilities.CancelCallback;

/**
 * This class creates titled pane with progress bar and functions that update
 * the progress bar.
 */
public final class ProgressBar implements ActionListener {
    /** The progress bar. */
    private JProgressBar progressBar;
    /** Progress bar panel. */
    private JPanel pbPanel;
    /** Whether the progress bar should be stopped now. */
    private volatile boolean stopNow = false;
    /** Whether to hold the progress bar. */
    private boolean holdIt = false;
    /** Progress of the progress bar. */
    private int progress = 0;
    /** Timethat passed in the progress bar. */
    private int time = 0;
    /** Timeout for the progress bar in milliseconds. */
    private int timeout;
    /** Default timeout. */
    private static final int DEFAULT_TIMEOUT = 50 * 1000;
    /** Thread with progress bar. */
    private Thread progressThread = null;
    /** This is threshold to catch threads that are out of the line.
     * TODO: not for production. */
    private static final int DEBUG_THRESHOLD = 120000;
    /** Max value in the progress bar. */
    private static final int MAX_PB_VALUE = 100;
    /** Cancel button. */
    private MyButton cancelButton = null;
    /** Cancel callback function that will be called, when cancel was pressed.
     */
    private CancelCallback cancelCallback;
    /** Cancel icon. */
    private static final ImageIcon CANCEL_ICON =
            Tools.createImageIcon(Tools.getDefault("ProgressBar.CancelIcon"));
    /** Hide progress bar after the time in milliseconds. */
    private static final int HIDE_PB_AFTER = 1000;

    /** Prepares a new <code>ProgressBar</code> object. */
    ProgressBar(final String title,
                final CancelCallback cancelCallback,
                final int width,
                final int height) {
        this.cancelCallback = cancelCallback;
        progressBar = new JProgressBar(0, MAX_PB_VALUE);
        progressBar.setPreferredSize(new Dimension(width, height));
        progressBar.setBackground(
                            Tools.getDefaultColor("ProgressBar.Background"));
        progressBar.setForeground(
                            Tools.getDefaultColor("ProgressBar.Foreground"));
        pbPanel = new JPanel();
        if (title != null) {
            final TitledBorder titledBorder =
                                    BorderFactory.createTitledBorder(title);
            pbPanel.setBorder(titledBorder);
        }
        pbPanel.add(progressBar);

        if (cancelCallback != null) {
            cancelButton = new MyButton(Tools.getString("ProgressBar.Cancel"),
                                        CANCEL_ICON);
            cancelButton.setEnabled(false);
            cancelButton.addActionListener(this);
            pbPanel.add(cancelButton);
        }
        final Dimension d = new Dimension(
                              Short.MAX_VALUE,
                              (int) pbPanel.getPreferredSize().getHeight());
        pbPanel.setMaximumSize(d);
        pbPanel.setPreferredSize(d);
        progressBar.setVisible(false);
        if (cancelButton != null) {
            cancelButton.setVisible(false);
        }
    }

    /** Prepares a new <code>ProgressBar</code> object without title. */
    ProgressBar(final CancelCallback cancelCallbackA,
                final int width,
                final int height) {
        this(null, cancelCallbackA, width, height);
    }

    /** Prepares a new <code>ProgressBar</code> object. */
    public ProgressBar(final String title,
                       final CancelCallback cancelCallbackA) {
        this(title,
             cancelCallbackA,
             Tools.getDefaultInt("ProgressBar.DefaultWidth"),
             Tools.getDefaultInt("ProgressBar.DefaultHeight"));
    }

    /** Prepares a new <code>ProgressBar</code> object without title. */
    public ProgressBar(final CancelCallback cancelCallbackA) {
        this(null,
             cancelCallbackA,
             Tools.getDefaultInt("ProgressBar.DefaultWidth"),
             Tools.getDefaultInt("ProgressBar.DefaultHeight"));
    }

    /** Enables or disables cancel button if it exists. */
    public void setCancelEnabled(final boolean enable) {
        if (cancelButton != null) {
            cancelButton.setEnabled(enable);
        }
    }

    /** Starts progress bar thread. */
    public void start(final int t) {
        this.timeout = t;
        stopNow = false;
        if (timeout == 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        if (progressThread == null) {
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Tools.debug(this,
                                "running postgresbar timeout: " + timeout,
                                1);
                    int sleepTime = Tools.getDefaultInt("ProgressBar.Sleep");
                    int progressBarDelay =
                                    Tools.getDefaultInt("ProgressBar.Delay");
                    int threshold = DEBUG_THRESHOLD;
                    boolean isVisible = false;
                    while (!stopNow) { // && progress <= timeout) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        /* show progress bar after delay */
                        if (time > progressBarDelay && !isVisible) {
                            isVisible = true;
                            Tools.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisible(true);
                                    if (cancelButton != null) {
                                        cancelButton.setVisible(true);
                                    }
                                }
                            });
                        }
                        if (!holdIt) {
                            Tools.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setValue(
                                            progress * MAX_PB_VALUE / timeout);
                                }
                            });
                            progress += sleepTime;
                        }

                        time += sleepTime;
                        if (time > threshold) {
                            Tools.appWarning("Thread with timeout: "
                                             + timeout
                                             + " is running way too long");
                            threshold += DEBUG_THRESHOLD;
                        }
                        if (progress >= timeout) {
                            /* premature end */
                            Tools.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setIndeterminate(true);
                                }
                            });
                        }
                    }
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setIndeterminate(false);
                            progressBar.setValue(MAX_PB_VALUE);
                        }
                    });
                    progressThread = null;
                };
            };
            progressThread = new Thread(runnable);
            progressThread.start();
        } else {
            progress = 0;
            time = 0;
        }
    }

    /** Returns progress bar panel. */
    public JPanel getProgressBarPane() {
        return pbPanel;
    }

    /** Stops progress bar and sets it to the maximum position. */
    public void done() {
        if (cancelButton != null) {
            cancelButton.setEnabled(false);
        }
        stopNow = true;
        progress = timeout;
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                progressBar.setIndeterminate(false);
                progressBar.setValue(MAX_PB_VALUE);
            }
        });
    }

    /** Stops progress bar and sets it to 0. */
    public void doneError() {
        if (cancelButton != null) {
            cancelButton.setEnabled(false);
        }
        stopNow = true;
        progress = 0;
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
            }
        });
    }

    /** Stops progress bar and hides it after 1 second. */
    void doneHide() {
        done();
        try {
            Thread.sleep(HIDE_PB_AFTER);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisible(false);
                if (cancelButton != null) {
                    cancelButton.setVisible(false);
                }
            }
        });
    }

    /** Stops progress bar and sets it to 0 and hide it after 1 second. */
    void doneErrorHide() {
        doneError();
        try {
            Thread.sleep(HIDE_PB_AFTER);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisible(false);
                if (cancelButton != null) {
                    cancelButton.setVisible(false);
                }
            }
        });
    }

    /**
     * Holds progress bar until cont() is called and sets it in indeterminate
     * state.
     */
    public void hold() {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisible(true);
                if (cancelButton != null) {
                    cancelButton.setVisible(true);
                }
                progressBar.setIndeterminate(true);
            }
        });
        holdIt = true;
    }

    /**
     * Starts progress bar again from the position it was before calling
     * hold().
     */
    void cont() {
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                progressBar.setIndeterminate(false);
            }
        });
        holdIt = false;
    }

    /** cancels ssh connection. */
    @Override
    public void actionPerformed(final ActionEvent e) {
        final String command = e.getActionCommand();

        if (command.equals(Tools.getString("ProgressBar.Cancel"))) {
            Tools.debug(this, "cancel button pressed", 1);
            cancelCallback.cancel();
        }
    }
}
