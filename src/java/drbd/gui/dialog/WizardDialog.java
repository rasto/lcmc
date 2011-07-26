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


package drbd.gui.dialog;

import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.CancelCallback;
import drbd.gui.ProgressBar;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.Container;

/**
 * An implementation of a wizard dialog with next, back, finish and cancel
 * buttons.
 * The dialogs that are in a row of dialog steps should extend this class
 * and overwrite at least * body() and nextDialog() methods.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public abstract class WizardDialog extends ConfigDialog {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Previous dialog object. A dialog that will be displayed after
     * clicking on the back button */
    private WizardDialog previousDialog;
    /** Cancel icon. */
    private static final ImageIcon CANCEL_ICON =
            Tools.createImageIcon(Tools.getDefault("Dialog.Dialog.CancelIcon"));
    /** Finish icon. */
    private static final ImageIcon FINISH_ICON =
            Tools.createImageIcon(Tools.getDefault("Dialog.Dialog.FinishIcon"));
    /** Next icon. */
    private static final ImageIcon NEXT_ICON =
            Tools.createImageIcon(Tools.getDefault("Dialog.Dialog.NextIcon"));
    /** Back icon. */
    private static final ImageIcon BACK_ICON =
            Tools.createImageIcon(Tools.getDefault("Dialog.Dialog.BackIcon"));
    /** Progress bar. */
    private ProgressBar progressBar = null;

    /** Prepares a new <code>WizardDialog</code> object. */
    protected WizardDialog(final WizardDialog previousDialog) {
        super();
        this.previousDialog = previousDialog;
    }

    /**
     * Returns previous dialog. It is used to get with the back button to
     * the dialog before this one.
     */
    public WizardDialog getPreviousDialog() {
        return previousDialog;
    }

    /**
     * Returns previous dialog. It is used to get with the back button to
     * the dialog before this one.
     */
    protected final void setPreviousDialog(final WizardDialog previousDialog) {
        this.previousDialog = previousDialog;
    }

    ///**
    // * TextResource files contain texts in different languages. Text for every
    // * button has to be defined there. If Next button is used, resource file
    // * has to contain Dialog.Next item.
    // */
    //protected final String buttonString(final String b) {
    //    return Tools.getString("Dialog.Dialog." + b);
    //}

    /** Returns localized string of Next button. */
    public String nextButton() {
        return buttonString("Next");
    }

    /** Returns localized string of Back button. */
    public final String backButton() {
        return buttonString("Back");
    }

    /** Returns localized string of Finish button. */
    public String finishButton() {
        return buttonString("Finish");
    }

    /** Returns localized string of Retry button. */
    final String retryButton() {
        return buttonString("Retry");
    }

    /** Returns true if Cancel button was pressed. */
    public final boolean isPressedCancelButton() {
        return isPressedButton(cancelButton());
    }

    /** Returns true if Retry button was pressed. */
    final boolean isPressedRetryButton() {
        return isPressedButton(retryButton());
    }

    /** Returns true if Finish button was pressed. */
    public final boolean isPressedFinishButton() {
        return isPressedButton(finishButton());
    }

    /**
     * Array of buttons that are used in the dialog. Wrapper function
     * like nextButton() should be used instead of simple "Next", so
     * it can be localized. In TextResources.java file it can be than
     * redifined with Dialog.Dialog.Next.
     */
    @Override protected final String[] buttons() {
        final String[] btns = {retryButton(), // this one is hidden.
                               backButton(),
                               nextButton(),
                               finishButton(),
                               cancelButton()};
        return btns;
    }

    /**
     * Returns the listener for the skip button, that enables "next" button if
     * it is checked.
     */
    @Override protected final ItemListener skipButtonListener() {
        return new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                buttonClass(nextButton()).setEnabled(true);
                skipButtonSetEnabled(false);
            }
        };
    }

    /** Enable next button, with skip button logic. */
    protected final void nextButtonSetEnabled(final boolean enable) {
        if (skipButtonIsSelected()) {
            return;
        } else {
            if (enable) {
                skipButtonSetEnabled(false);
            } else {
                skipButtonSetEnabled(true);
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    buttonClass(nextButton()).setEnabled(enable);
                    if (enable) {
                        makeDefaultAndRequestFocus(buttonClass(nextButton()));
                    }
                }
            });
        }
    }

    /** Returns icons for the buttons. */
    @Override protected final ImageIcon[] getIcons() {
        final ImageIcon[] icons = {null,
                                   BACK_ICON,
                                   NEXT_ICON,
                                   FINISH_ICON,
                                   CANCEL_ICON
                             };
        return icons;
    }

    /** Returns default button, none by default. */
    @Override protected final String defaultButton() {
        return null;
    }

    /** After next or finish buttons are pressed, this function is called. */
    protected final boolean checkAfterNextFinish() {
        return true;
    }

    /** After next or finish buttons are pressed, this function is called. */
    protected void finishDialog() {
        /* no action */
    }

    /** Returns next dialog, that follows after pressing next button. */
    public abstract WizardDialog nextDialog();

    /**
     * Enables components except the ones that are passed as the argument.
     */
    @Override protected final void enableComponents(
                                     final JComponent[] componentsToDisable) {
        super.enableComponents(componentsToDisable);
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (buttonClass(retryButton()) != null
                    && buttonClass(retryButton()).isVisible()
                    && buttonClass(retryButton()).isEnabled()) {
                    makeDefaultAndRequestFocus(buttonClass(retryButton()));
                } else if (buttonClass(nextButton()) != null
                    && buttonClass(nextButton()).isEnabled()) {
                    makeDefaultAndRequestFocus(buttonClass(nextButton()));
                } else if (buttonClass(finishButton()) != null
                    && buttonClass(finishButton()).isEnabled()) {
                    makeDefaultAndRequestFocus(buttonClass(finishButton()));
                }
            }
        });
    }

    /** Requests focus. */
    protected final void makeDefaultAndRequestFocus(final JComponent b) {
        if (b instanceof JButton) {
            getDialogPanel().getRootPane().setDefaultButton((JButton) b);
        }
        b.requestFocus();
    }

    /** Requests focus in the swing thread. */
    protected final void makeDefaultAndRequestFocusLater(final JComponent b) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                makeDefaultAndRequestFocus(b);
            }
        });
    }

    /** Sets as default button. */
    protected final void makeDefaultButton(final JButton b) {
        getDialogPanel().getRootPane().setDefaultButton(b);
    }

    /** Enables components. */
    @Override protected final void enableComponents() {
        enableComponents(new JComponent[]{});
    }

    /** Inits the dialog. */
    @Override protected void initDialog() {
        /* align buttons to the right */
        final FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.RIGHT);

        if (buttonClass(cancelButton()) != null) {
            buttonClass(cancelButton()).getParent().setLayout(layout);
        }

        /* disable back button if there is no previous dialog */
        if (previousDialog == null && buttonClass(backButton()) != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    buttonClass(backButton()).setEnabled(false);
                }
            });
        }

        /* disable next and finish buttons */
        if (buttonClass(nextButton()) != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    buttonClass(nextButton()).setEnabled(false);
                }
            });
        }
        if (buttonClass(finishButton()) != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    buttonClass(finishButton()).setEnabled(false);
                }
            });
        }
        if (buttonClass(retryButton()) != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    buttonClass(retryButton()).setVisible(false);
                    buttonClass(retryButton()).setBackgroundColor(Color.RED);
                }
            });
        }
        disableComponents();
    }

    /** if retry button was pressed this method will be executed. */
    protected final void retryWasPressed() {
        /* no action */
    }

    /** if back button was pressed this method will be executed. */
    protected final void backButtonWasPressed() {
        /* no action */
    }

    /**
     * Check which button was pressed. Return previous dialog if back button
     * was pressed. Call checkAfterNextFinish() if next or back button were
     * pressed. If checkAfterNextFinish() returns true return next dialog,
     * if next button was pressed.
     */
    @Override protected final ConfigDialog checkAnswer() {
        if (isPressedButton(backButton())) {
            backButtonWasPressed();
            return getPreviousDialog();
        }
        if (isPressedButton(nextButton())
            || isPressedButton(finishButton())
            || isPressedButton(retryButton())) {
            if (checkAfterNextFinish()) {
                finishDialog();
                if (isPressedButton(nextButton())) {
                    return nextDialog();
                } else if (isPressedButton(retryButton())) {
                    retryWasPressed();
                    setDialogPanel(null);
                    return this;
                } else {
                    return null;
                }
            } else {
                return this;
            }
        }
        return null;
    }

    /**
     * prints error text in the answer pane, reenables
     * buttons and adds retry button.
     */
    public void printErrorAndRetry(final String text) {
        printErrorAndRetry(text, null, 0);
    }

    /**
     * prints error text in the answer pane, reenables
     * buttons and adds retry button.
     */
    public final void printErrorAndRetry(String text,
                                         final String ans,
                                         final int exitCode) {
        if (ans != null) {
            text += "\n" + Tools.getString("Dialog.Dialog.PrintErrorAndRetry")
                 + exitCode + "\n" + ans;
        }
        answerPaneSetTextError(text);
        addRetryButton();
        if (buttonClass(retryButton()) != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    buttonClass(retryButton()).requestFocus();
                }
            });
        }
        if (buttonClass(nextButton()) != null) {
            enableComponents(new JComponent[]{buttonClass(nextButton())});
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    buttonClass(nextButton()).setEnabled(false);
                }
            });
        }
    }

    /** Reenables buttons and adds retry button. */
    public final void retry() {
        addRetryButton();
        if (buttonClass(retryButton()) != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    buttonClass(retryButton()).requestFocus();
                }
            });
            if (buttonClass(nextButton()) != null) {
                enableComponents(new JComponent[]{buttonClass(nextButton())});
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        buttonClass(nextButton()).setEnabled(false);
                    }
                });
            }
        }
    }

    /** Adds the retry button. */
    final void addRetryButton() {
        if (buttonClass(retryButton()) != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    getOptionPane().setInitialValue(
                                                buttonClass(retryButton()));
                }
            });
        }
        // setInitialValue destroys layout, so once
        // again...
        final FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.RIGHT);

        if (buttonClass(cancelButton()) != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    final Container parent =
                                    buttonClass(cancelButton()).getParent();
                    if (parent != null) {
                        buttonClass(cancelButton()).getParent().setLayout(
                                                                       layout);
                    }
                }
            });
        }

        if (buttonClass(retryButton()) != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    buttonClass(retryButton()).setVisible(true);
                    buttonClass(retryButton()).setEnabled(true);
                }
            });
        }
    }

    /** Hides the retry button if it is there. */
    public final void hideRetryButton() {
        final MyButton rb = buttonClass(retryButton());

        if (rb != null && rb.isVisible()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    rb.setVisible(false);
                }
            });
        }
    }

    /** Presses the retry button. */
    final void pressRetryButton() {
        final MyButton rb = buttonClass(retryButton());

        if (rb != null && rb.isVisible() && rb.isEnabled()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    rb.pressButton();
                }
            });
        }
    }

    /** Presses the next button. */
    public final void pressNextButton() {
        final MyButton nb = buttonClass(nextButton());
        if (nb != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    if (nb.isVisible() && nb.isEnabled()) {
                        nb.pressButton();
                    }
                }
            });
        }
    }

    /**
     * Creates progress bar that can be used during connecting to the host
     * and returns pane, where the progress bar is displayed.
     */
    public JPanel getProgressBarPane(final CancelCallback cancelCallback) {
        progressBar = new ProgressBar(cancelCallback);
        final JPanel p = progressBar.getProgressBarPane();
        p.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        return p;
    }

    /** Is called after failed connection. */
    public final void progressBarDoneError() {
        progressBar.doneError();
    }

    /** Is called after successful connection. */
    public final void progressBarDone() {
        progressBar.done();
    }

    /** Returns progressBar object. */
    public final ProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * Creates progress bar that can be used during connecting to the host
     * and returns pane, where the progress bar is displayed.
     */
    public final JPanel getProgressBarPane(
                                        final String title,
                                        final CancelCallback cancelCallback) {
        progressBar = new ProgressBar(title, cancelCallback);
        final JPanel p = progressBar.getProgressBarPane();
        p.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        return p;
    }
}
