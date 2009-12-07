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
import drbd.gui.GuiComboBox;

import javax.swing.JPanel;
import javax.swing.JOptionPane;
import drbd.utilities.MyButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.JCheckBox;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Arrays;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.concurrent.CountDownLatch;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * An implementation of a dialog with buttons. Ok button is predefined.
 * The dialogs should extend this class and overwrite at least
 * getDialogTitle(), getDescription(), getInputPane() and nextDialog() methods.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public abstract class ConfigDialog {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** The whole option pane. */
    private JOptionPane optionPane;
    /** dialog panel. */
    private JDialog dialogPanel;
    /** Which button was pressed. */
    private String pressedButton = "";
    /** Map from the button name to its object. */
    private final Map<String, MyButton> buttonToObjectMap =
                                            new HashMap<String, MyButton>();
    /** Answer pane. The pane were texts can be easily written. */
    private JEditorPane answerPane = null;
    /** Components that were disabled and can be enabled later. */
    private final List<JComponent> disabledComponents =
                                                new ArrayList<JComponent>();
    /** Size of the imput pane. */
    private static final int INPUT_PANE_HEIGHT = 200;
    /** Gate to synchronize the non-modal dialog and the answer.*/
    private CountDownLatch dialogGate;
    /** Skip button, can be null, if there is no skip button. */
    private JCheckBox skipButton = null;
    /** Answer from the optionpane. */
    private volatile Object optionPaneAnswer;

    /**
     * Gets dialogPanel.
     */
    public final JDialog getDialogPanel() {
        return dialogPanel;
    }

    /**
     * Gets location of the dialog panel.
     */
    public final Point getLocation() {
        return dialogPanel.getLocation();
    }

    /**
     * Gets the title of the dialog as string.
     */
    protected abstract String getDialogTitle();

    /**
     * Returns description for dialog. This can be HTML defined in
     * TextResource.
     */
    protected abstract String getDescription();

    /**
     * Returns pane where user input can be defined.
     */
    protected abstract JComponent getInputPane();

    /**
     * Returns the option pane.
     */
    protected final JOptionPane getOptionPane() {
        return optionPane;
    }

    /**
     * Returns answer pane in a scroll pane.
     */
    public final JScrollPane getAnswerPane(final String initialText) {
        answerPane = new JEditorPane(Tools.MIME_TYPE_TEXT_PLAIN, initialText);
        answerPane.setBackground(
                            Tools.getDefaultColor("ConfigDialog.AnswerPane"));
        answerPane.setEditable(false);
        final JScrollPane scrollPane = new JScrollPane(answerPane);
        scrollPane.setHorizontalScrollBarPolicy(
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    /**
     * Sets text to the answer pane.
     */
    public final void answerPaneSetText(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                answerPane.setText(text);
            }
        });
    }

    /**
     * Appends text to the answer pane.
     */
    public final void answerPaneAddText(final String text) {
        answerPaneSetText(answerPane.getText() + "\n" + text);
    }


    /**
     * Sets the error text in the answer pane and sets the text color to red.
     */
    public final void answerPaneSetTextError(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                answerPane.setForeground(
                       Tools.getDefaultColor("ConfigDialog.AnswerPane.Error"));
                answerPane.setText(text);
            }
        });
    }

    /**
     * Appends the error text to the answer pane.
     */
    public final void answerPaneAddTextError(final String text) {
        answerPaneSetTextError(answerPane.getText() + "\n" + text);
    }

    /**
     * Creates body of the dialog. You can redefine getDialogTitle(),
     * getDescription(), getInputPane() methods to customize this
     * body.
     */
    protected final JPanel body() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        final JEditorPane descPane = new JEditorPane(
                       Tools.MIME_TYPE_TEXT_HTML,
                       "<p style='font-family:Dialog; font-size:16; "
                       + "font-weight: bold'>"
                       + getDialogTitle() + "</p>"
                       + "<p style='font-family:Dialog; font-size:12;'>"
                       + getDescription() + "</p>");
        descPane.setBackground(pane.getBackground());
        descPane.setEditable(false);
        final JScrollPane descSP = new JScrollPane(descPane);
        descSP.setBorder(null);
        descSP.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(descSP);
        final JComponent inputPane = getInputPane();
        if (inputPane != null) {
            inputPane.setPreferredSize(new Dimension(Short.MAX_VALUE,
                                                     INPUT_PANE_HEIGHT));
            inputPane.setBackground(
                            Tools.getDefaultColor("ConfigDialog.Background"));
            inputPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            pane.add(inputPane);
        }

        return pane;
    }

    /**
     * Returns an icon or null for default icon.
     */
    protected ImageIcon icon() {
        return Tools.createImageIcon(Tools.getDefault("ConfigDialog.Icon"));
    }

    /**
     * Array of buttons that are used in the dialog. Wrapper function
     * like okButton() should be used instead of simple "Ok", so
     * it can be localized. In TextResources.java file it can be than
     * redifined with ConfigDialog.Ok.
     */
    protected String[] buttons() {
        return new String[]{okButton()};
    }

    /**
     * Returns icons for buttons in the same order as the buttons are defined.
     */
    protected ImageIcon[] getIcons() {
        return new ImageIcon[]{null};
    }

    /**
     * One default button from the buttons() method.
     */
    protected String defaultButton() {
        return okButton();
    }

    /**
     * Compares pressed button with button that is passed as parameter.
     */
    public final boolean isPressedButton(final String button) {
        return pressedButton.equals(button);
    }

    /**
     * Set pressed button to the string passed as a parameter.
     */
    protected final void setPressedButton(final String button) {
        pressedButton = button;
    }

    /**
     * Returns localized string for Ok button.
     */
    public final String okButton() {
        return buttonString("Ok");
    }


    /**
     * Returns localized string of Cancel button.
     */
    public final String cancelButton() {
        return buttonString("Cancel");
    }

    /**
     * TextResource files contain texts in different languages. Text for every
     * button has to be defined there. If Ok button is used, resource file
     * has to contain ConfigDialog.Ok item.
     */
    protected String buttonString(final String b) {
        return Tools.getString("Dialog.Dialog." + b);
    }

    /**
     * Returns class of the button, so that it can be enabled or
     * disabled.
     */
    protected final JButton buttonClass(final String button) {
        return buttonToObjectMap.get(button);
    }

    /**
     * This method is called before the dialog is shown. It also disables
     * buttons. They have to be enabled with enableComponents()
     */
    protected void initDialog() {
        if (buttonClass(okButton()) != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    buttonClass(okButton()).setEnabled(true);
                }
            });
        }
        disableComponents();
    }

    /**
     * This method is called during every insert, update and remove events of
     * field that was added with addCheckField(). It does nothing by default.
     */
    protected void checkFields(final GuiComboBox field) {
        /* Does nothing by default. */
    }

    /**
     * Add listener to the field. The checkFields(field) will be called on every
     * insert, update and remove event.
     */
    protected final void addCheckField(final GuiComboBox field) {
        field.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void insertUpdate(final DocumentEvent e) {
                        final Thread t = new Thread(new Runnable() {
                            public void run() {
                                checkFields(field);
                            }
                        });
                        t.start();
                    }

                    public void removeUpdate(final DocumentEvent e) {
                        final Thread t = new Thread(new Runnable() {
                            public void run() {
                                checkFields(field);
                            }
                        });
                        t.start();
                    }

                    public void changedUpdate(final DocumentEvent e) {
                        final Thread t = new Thread(new Runnable() {
                            public void run() {
                                checkFields(field);
                            }
                        });
                        t.start();
                    }
                });
    }

    /**
     * This method is called after user has pushed the button.
     */
    protected ConfigDialog checkAnswer() {
        return null;
    }

    /**
     * Returns the width of the dialog.
     */
    protected int dialogWidth() {
        return Tools.getDefaultInt("ConfigDialog.width");
    }

    /**
     * Returns the height of the dialog.
     */
    protected int dialogHeight() {
        return Tools.getDefaultInt("ConfigDialog.height");
    }

    /**
     * Returns the message type of the JOptionPane. Default is
     * INFORMATION_MESSAGE.
     */
    protected int getMessageType() {
        return JOptionPane.INFORMATION_MESSAGE;
    }

    /**
     * Whether the skip button is enabled.
     */
    protected boolean skipButtonEnabled() {
        return false;
    }

    /**
     * Returns the listener for the skip button.
     */
    protected ItemListener skipButtonListener() {
        return null;
    }

    /**
     * Returns whether skip button is selected.
     */
    protected final boolean skipButtonIsSelected() {
        if (skipButton != null) {
            return skipButton.isSelected();
        }
        return false;
    }

    /**
     * Enable/disable skip button.
     */
    protected final void skipButtonSetEnabled(final boolean enable) {
        if (skipButton != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    skipButton.setEnabled(enable);
                }
            });
        }
    }

    /**
     * Shows dialog and wait for answer.
     * Returns next dialog, or null if it there is no next dialog.
     */
    public final ConfigDialog showDialog() {
        final String[] buttons = buttons();
        final ImageIcon[] icons = getIcons();
        MyButton[] options = new MyButton[buttons.length];
        MyButton defaultButtonClass = null;
        final List<JComponent> allOptions = new ArrayList<JComponent>();
        if (skipButtonEnabled()) {
            skipButton = new JCheckBox(Tools.getString(
                                           "Dialog.ConfigDialog.SkipButton"));
            skipButton.setBackground(
                    Tools.getDefaultColor("ConfigDialog.Background.Dark"));
            skipButton.addItemListener(skipButtonListener());
            allOptions.add(skipButton);
        }
        /* populate buttonToObjectMap */
        for (int i = 0; i < buttons.length; i++) {
            options[i] = new MyButton(buttons[i], icons[i]);
            allOptions.add(options[i]);
            buttonToObjectMap.put(buttons[i], options[i]);
            if (buttons[i].equals(defaultButton())) {
                defaultButtonClass = options[i];
            }
        }
        /* create option pane */
        optionPane = new JOptionPane(
                                body(),
                                getMessageType(),
                                JOptionPane.DEFAULT_OPTION,
                                icon(),
                                allOptions.toArray(
                                            new JComponent[allOptions.size()]),
                                defaultButtonClass);
        /* making non modal dialog */
        dialogGate = new CountDownLatch(1);
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                if (JOptionPane.VALUE_PROPERTY.equals(evt.getPropertyName())
                    && !"uninitializedValue".equals(evt.getNewValue())) {
                    optionPaneAnswer = optionPane.getValue();
                    dialogGate.countDown();
                }
            }
        });

        optionPane.setPreferredSize(new Dimension(dialogWidth(),
                                                  dialogHeight()));
        optionPane.setMaximumSize(new Dimension(dialogWidth(),
                                                dialogHeight()));
        optionPane.setMinimumSize(new Dimension(dialogWidth(),
                                                dialogHeight()));

        /* add action listeners */
        for (final MyButton o : options) {
            o.addActionListener(new OptionPaneActionListener());
        }

        optionPane.setBackground(
                    Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        dialogPanel = optionPane.createDialog(Tools.getGUIData().getMainFrame(),
                                              getDialogTitle());
        dialogPanel.setModal(false);
        dialogPanel.setPreferredSize(new Dimension(dialogWidth(),
                                                   dialogHeight()));
        dialogPanel.setMaximumSize(new Dimension(dialogWidth(),
                                                 dialogHeight()));
        dialogPanel.setMinimumSize(new Dimension(dialogWidth(),
                                                 dialogHeight()));
        for (Component c : optionPane.getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Dark"));
            }
        }
        /* set location like the previous dialog */
        dialogPanel.setVisible(true);
        initDialog();
        try {
            dialogGate.await();
        } catch (InterruptedException ignored) {
            /* ignored */
        }

        if (optionPaneAnswer instanceof String) {
            setPressedButton((String) optionPaneAnswer);
        } else {
            setPressedButton(cancelButton());
        }
        return checkAnswer();
    }

    /**
     * Disables array of components and all the buttons. The ones that
     * were enabled are stored in disabledComponents list so that they
     * can be later enabled with call to enableComponents.
     */
    protected final void disableComponents(final JComponent[] components) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (final String b : buttons()) {
                    final JComponent option = buttonClass(b);
                    if (option.isEnabled()) {
                        disabledComponents.add(option);
                        option.setEnabled(false);
                    }
                }
                for (final JComponent c : components) {
                    if (c.isEnabled()) {
                        disabledComponents.add(c);
                        c.setEnabled(false);
                    }
                }
            }
        });
    }

    /**
     * Disables all the option buttons. The ones that
     * were enabled are stored in disabledComponents list so that they
     * can be later enabled with call to enableComponents.
     */
    protected final void disableComponents() {
        disableComponents(new JComponent[]{});
    }

    /**
     * Enables components that were disabled with disableComponents, except
     * the ones that are in componentsToDisable array.
     */
    protected void enableComponents(
                                    final JComponent[] componentsToDisable) {
        final HashSet<JComponent> ctdHash =
                new HashSet<JComponent>(Arrays.asList(componentsToDisable));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (final JComponent dc : disabledComponents) {
                    if (!ctdHash.contains(dc)) {
                        dc.setEnabled(true);
                    }
                }
                disabledComponents.clear();
            }
        });
    }

    /**
     * Enables components after disableComponents, but they will be really
     * enabled only after enableComponents without arguments is be called.
     */
    protected final void enableComponentsLater(
                                    final JComponent[] componentsToEnable) {
        for (final JComponent c : componentsToEnable) {
            disabledComponents.add(c);
        }
    }

    /**
     * Enables components that were disabled with disableComponents.
     */
    protected void enableComponents() {
        enableComponents(new JComponent[]{});
    }

    /**
     * Is called after dialog was canceled. It does nothing by default.
     */
    public void cancelDialog() {
        /* Does nothing by default. */
    }

    /**
     * Action listener for custom buttons.
     */
    class OptionPaneActionListener implements ActionListener {
        /**
         * Action performered on custom button.
         */
        public void actionPerformed(final ActionEvent event) {
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            optionPane.setValue(
                                      ((JButton) event.getSource()).getText());
                        }
                    });
                }
            });
            t.start();
        }
    }
}
