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


package lcmc.common.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lcmc.gui.ProgressBar;
import lcmc.gui.widget.Check;
import lcmc.model.Application;
import lcmc.utilities.CancelCallback;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;

/**
 * An implementation of a wizard dialog with next, back, finish and cancel
 * buttons.
 * The dialogs that are in a row of dialog steps should extend this class
 * and overwrite at least * body() and nextDialog() methods.
 */
public abstract class WizardDialog extends ConfigDialog {
    static final ImageIcon CANCEL_ICON = Tools.createImageIcon(Tools.getDefault("Dialog.Dialog.CancelIcon"));
    static final ImageIcon FINISH_ICON = Tools.createImageIcon(Tools.getDefault("Dialog.Dialog.FinishIcon"));
    private static final ImageIcon NEXT_ICON = Tools.createImageIcon(Tools.getDefault("Dialog.Dialog.NextIcon"));
    private static final ImageIcon BACK_ICON = Tools.createImageIcon(Tools.getDefault("Dialog.Dialog.BackIcon"));
    /** Previous dialog object. A dialog that will be displayed after clicking on the back button */
    private WizardDialog previousDialog;
    private ProgressBar progressBar = null;
    @Inject
    private Application application;
    @Inject
    private Provider<ProgressBar> progressBarProvider;

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

    public String nextButton() {
        return buttonString("Next");
    }

    public final String backButton() {
        return buttonString("Back");
    }

    public String finishButton() {
        return buttonString("Finish");
    }

    final String retryButton() {
        return buttonString("Retry");
    }

    public final boolean isPressedCancelButton() {
        return isPressedButton(cancelButton());
    }

    public final boolean isPressedFinishButton() {
        return isPressedButton(finishButton());
    }

    /**
     * Array of buttons that are used in the dialog. Wrapper function
     * like nextButton() should be used instead of simple "Next", so
     * it can be localized. In TextResources.java file it can be than
     * redifined with Dialog.Dialog.Next.
     */
    @Override
    protected final String[] buttons() {
        return new String[]{retryButton(), // this one is hidden.
                            backButton(),
                            nextButton(),
                            finishButton(),
                            cancelButton()};
    }

    /**
     * Returns the listener for the skip button, that enables "next" button if
     * it is checked.
     */
    @Override
    protected final ItemListener skipButtonListener() {
        return new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                buttonClass(nextButton()).setEnabled(true);
                skipButtonSetEnabled(false);
            }
        };
    }

    /** Enable next button, with skip button logic. */
    protected final void nextButtonSetEnabled(final Check check) {
        if (!skipButtonIsSelected()) {
            if (check.isCorrect()) {
                skipButtonSetEnabled(false);
            } else {
                skipButtonSetEnabled(true);
            }
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(nextButton()).setEnabledCorrect(check);
                    if (check.isCorrect()) {
                        makeDefaultAndRequestFocus(buttonClass(nextButton()));
                    }
                }
            });
        }
    }

    /** Returns icons for the buttons. */
    @Override
    protected final ImageIcon[] getIcons() {
        return new ImageIcon[]{null, BACK_ICON, NEXT_ICON, FINISH_ICON, CANCEL_ICON};
    }

    @Override
    protected final String defaultButton() {
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
    @Override
    protected final void enableComponents(final JComponent[] componentsToDisable) {
        super.enableComponents(componentsToDisable);
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (buttonClass(retryButton()) != null
                    && buttonClass(retryButton()).isVisible()
                    && buttonClass(retryButton()).isEnabled()) {
                    makeDefaultAndRequestFocus(buttonClass(retryButton()));
                } else if (buttonClass(nextButton()) != null && buttonClass(nextButton()).isEnabled()) {
                    makeDefaultAndRequestFocus(buttonClass(nextButton()));
                } else if (buttonClass(finishButton()) != null && buttonClass(finishButton()).isEnabled()) {
                    makeDefaultAndRequestFocus(buttonClass(finishButton()));
                }
            }
        });
    }

    /** Requests focus. */
    protected final void makeDefaultAndRequestFocus(final java.awt.Component button) {
        if (button instanceof JButton) {
            getDialogPanel().getRootPane().setDefaultButton((JButton) button);
        }
        button.requestFocus();
    }

    protected final void makeDefaultAndRequestFocusLater(final java.awt.Component b) {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                makeDefaultAndRequestFocus(b);
            }
        });
    }

    protected final void makeDefaultButton(final JButton b) {
        getDialogPanel().getRootPane().setDefaultButton(b);
    }

    @Override
    protected final void enableComponents() {
        enableComponents(new JComponent[]{});
    }

    @Override
    protected void initDialogBeforeVisible() {
        /* align buttons to the right */
        final FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.TRAILING);

        if (buttonClass(cancelButton()) != null) {
            buttonClass(cancelButton()).getParent().setLayout(layout);
        }

        /* disable back button if there is no previous dialog */
        if (previousDialog == null && buttonClass(backButton()) != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(backButton()).setEnabled(false);
                }
            });
        }

        /* disable next and finish buttons */
        if (buttonClass(nextButton()) != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(nextButton()).setEnabled(false);
                }
            });
        }
        if (buttonClass(finishButton()) != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(finishButton()).setEnabled(false);
                }
            });
        }
        if (buttonClass(retryButton()) != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
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
    @Override
    protected final ConfigDialog checkAnswer() {
        if (isPressedButton(backButton())) {
            backButtonWasPressed();
            return getPreviousDialog();
        }
        if (isPressedButton(nextButton()) || isPressedButton(finishButton()) || isPressedButton(retryButton())) {
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
        return dialogAfterCancel();
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
    public final void printErrorAndRetry(String text, final String errorMessage, final int exitCode) {
        if (errorMessage != null) {
            text += '\n' + Tools.getString("Dialog.Dialog.PrintErrorAndRetry") + exitCode + '\n' + errorMessage;
        }
        answerPaneSetTextError(text);
        addRetryButton();
        if (buttonClass(retryButton()) != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(retryButton()).requestFocus();
                }
            });
        }
        final List<String> incorrect = new ArrayList<String>();
        incorrect.add(text);
        final List<String> changed = new ArrayList<String>();
        if (buttonClass(nextButton()) != null) {
            enableComponents(new JComponent[]{buttonClass(nextButton())});
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(nextButton()).setEnabledCorrect(new Check(incorrect, changed));
                }
            });
        }
    }

    /** Reenables buttons and adds retry button. */
    public final void retry() {
        addRetryButton();
        if (buttonClass(retryButton()) != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(retryButton()).requestFocus();
                }
            });
            if (buttonClass(nextButton()) != null) {
                enableComponents(new JComponent[]{buttonClass(nextButton())});
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        buttonClass(nextButton()).setEnabled(false);
                    }
                });
            }
        }
    }

    /** Adds the retry button. */
    final void addRetryButton() {
        if (buttonClass(retryButton()) != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getOptionPane().setInitialValue(buttonClass(retryButton()));
                }
            });
        }
        // setInitialValue destroys layout, so once
        // again...
        final FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.TRAILING);

        if (buttonClass(cancelButton()) != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final Container parent = buttonClass(cancelButton()).getParent();
                    if (parent != null) {
                        buttonClass(cancelButton()).getParent().setLayout(layout);
                    }
                }
            });
        }

        if (buttonClass(retryButton()) != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(retryButton()).setVisible(true);
                    buttonClass(retryButton()).setEnabled(true);
                }
            });
        }
    }

    public final void hideRetryButton() {
        final MyButton retryButton = buttonClass(retryButton());

        if (retryButton != null && retryButton.isVisible()) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    retryButton.setVisible(false);
                }
            });
        }
    }

    public final void pressNextButton() {
        final MyButton nextButton = buttonClass(nextButton());
        if (nextButton != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (nextButton.isVisible() && nextButton.isEnabled()) {
                        nextButton.pressButton();
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
        progressBar = progressBarProvider.get();
        progressBar.init(cancelCallback);
        final JPanel progressPane = progressBar.getProgressBarPane();
        progressPane.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        return progressPane;
    }

    /** Is called after failed connection. */
    public final void progressBarDoneError() {
        progressBar.doneError();
    }

    /** Is called after successful connection. */
    public final void progressBarDone() {
        progressBar.done();
    }

    public final ProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * Creates progress bar that can be used during connecting to the host
     * and returns pane, where the progress bar is displayed.
     */
    public final JPanel getProgressBarPane(final String title, final CancelCallback cancelCallback) {
        progressBar = progressBarProvider.get();
        progressBar.init(title, cancelCallback);
        final JPanel progressPane = progressBar.getProgressBarPane();
        progressPane.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        return progressPane;
    }

    protected WizardDialog dialogAfterCancel() {
        return null;
    }
}
