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

import drbd.utilities.MyButton;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;

import drbd.utilities.Tools;
import drbd.utilities.CancelCallback;

/**
 * This class creates titled pane with progress bar and functions that update
 * the progress bar.
 */
public class ProgressBar implements ActionListener {
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

    /**
     * Prepares a new <code>ProgressBar</code> object.
     */
    public ProgressBar(final String title,
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
        pbPanel.setMaximumSize(pbPanel.getPreferredSize());
        //pbPanel.setVisible(false);
    }

    /**
     * Prepares a new <code>ProgressBar</code> object without title.
     */
    public ProgressBar(final CancelCallback cancelCallbackA,
                       final int width,
                       final int height) {
        this(null, cancelCallbackA, width, height);
    }

    /**
     * Prepares a new <code>ProgressBar</code> object.
     */
    public ProgressBar(final String title,
                       final CancelCallback cancelCallbackA) {
        this(title,
             cancelCallbackA,
             Tools.getDefaultInt("ProgressBar.DefaultWidth"),
             Tools.getDefaultInt("ProgressBar.DefaultHeight"));
    }

    /**
     * Prepares a new <code>ProgressBar</code> object without title.
     */
    public ProgressBar(final CancelCallback cancelCallbackA) {
        this(null,
             cancelCallbackA,
             Tools.getDefaultInt("ProgressBar.DefaultWidth"),
             Tools.getDefaultInt("ProgressBar.DefaultHeight"));
    }
    /**
     * Enables or disables cancel button if it exists.
     */
    public final void setCancelEnabled(final boolean enable) {
        if (cancelButton != null) {
            cancelButton.setEnabled(enable);
        }
    }

    /**
     * Starts progress bar thread.
     */
    public final void start(final int t) {
        this.timeout = t;
        if (timeout == 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        if (progressThread == null) {
            final Runnable runnable = new Runnable() {
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
                        // show progress bar after delay
                        if (time > progressBarDelay) {
                            // TODO:this causes deadlock
                            if (!isVisible) {
                                isVisible = true;
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        pbPanel.setVisible(true);
                                    }
                                });
                            }
                        }
                        if (!holdIt) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    progressBar.setValue(
                                            progress * MAX_PB_VALUE / timeout);
                                }
                            });
                            progress += sleepTime;
                        }

                        time += sleepTime;
                        // catch unclosed threads !!!!!
                        if (time > threshold) {
                            Tools.appWarning("Thread with timeout: "
                                             + timeout
                                             + " is running way too long");
                            threshold += DEBUG_THRESHOLD;
                            //break;
                        }
                        if (progress >= timeout) {
                            /* premature end */
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    progressBar.setIndeterminate(true);
                                }
                            });
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {
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

    /**
     * Returns progress bar panel.
     */
    public final JPanel getProgressBarPane() {
        return pbPanel;
    }

    /**
     * Stops progress bar and sets it to the maximum position.
     */
    public final void done() {
        if (cancelButton != null) {
            cancelButton.setEnabled(false);
        }
        stopNow = true;
        progress = timeout;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(false);
                progressBar.setValue(MAX_PB_VALUE);
            }
        });
    }

    /**
     * Stops progress bar and sets it to 0.
     */
    public final void doneError() {
        if (cancelButton != null) {
            cancelButton.setEnabled(false);
        }
        stopNow = true;
        progress = 0;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
            }
        });
    }

    /**
     * Stops progress bar and hides it after 1 second.
     */
    public final void doneHide() {
        done();
        try {
            Thread.sleep(HIDE_PB_AFTER);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressBar.setVisible(false);
            }
        });
    }

    /**
     * Stops progress bar and sets it to 0 and hide it after 1 second.
     */
    public final void doneErrorHide() {
        doneError();
        try {
            Thread.sleep(HIDE_PB_AFTER);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressBar.setVisible(false);
            }
        });
    }

    /**
     * Holds progress bar until cont() is called and sets it in indeterminate
     * state.
     */
    public final void hold() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressBar.setVisible(true);
                progressBar.setIndeterminate(true);
            }
        });
        holdIt = true;
    }

    /**
     * Starts progress bar again from the position it was before calling
     * hold().
     */
    public final void cont() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(false);
            }
        });
        holdIt = false;
    }

    /**
     * cancels ssh connection.
     */
    public final void actionPerformed(final ActionEvent e) {
        final String command = e.getActionCommand();

        if (command.equals(Tools.getString("ProgressBar.Cancel"))) {
            Tools.debug(this, "cancel button pressed", 1);
            cancelCallback.cancel();
        }
    }
}
