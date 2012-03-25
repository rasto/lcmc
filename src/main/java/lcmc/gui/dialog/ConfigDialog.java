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

package lcmc.gui.dialog;

import lcmc.utilities.Tools;
import lcmc.gui.GuiComboBox;
import lcmc.utilities.MyButton;

import javax.swing.JPanel;
import javax.swing.JOptionPane;
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
import javax.swing.JLabel;
import javax.swing.JApplet;
import javax.swing.JFrame;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Component;
import java.awt.FlowLayout;

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
    private volatile JOptionPane optionPane;
    /** dialog panel. */
    private JDialog dialogPanel;
    /** Which button was pressed. */
    private String pressedButton = "";
    /** Map from the button name to its object. */
    private final Map<String, MyButton> buttonToObjectMap =
                                            new HashMap<String, MyButton>();
    /** Text in answer pane. */
    private final StringBuilder answerPaneText = new StringBuilder(100);
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
    /** Whether the skipt button should be enabled. */
    private boolean skipButtonShouldBeEnabled = true;
    /** Option buttons. */
    private final MyButton[] options = new MyButton[buttons().length];
    /** Additional options. */
    private final List<JComponent> additionalOptions =
                                                   new ArrayList<JComponent>();
    /** Gets dialogPanel. */
    protected final JDialog getDialogPanel() {
        return dialogPanel;
    }

    /** Sets this dialog panel. */
    final void setDialogPanel(final JDialog dialogPanel) {
        this.dialogPanel = dialogPanel;
    }


    /** Gets location of the dialog panel. */
    final Point getLocation() {
        return dialogPanel.getLocation();
    }

    /** Gets the title of the dialog as string. */
    protected abstract String getDialogTitle();

    /**
     * Returns description for dialog. This can be HTML defined in
     * TextResource.
     */
    protected abstract String getDescription();

    /** Returns pane where user input can be defined. */
    protected abstract JComponent getInputPane();

    /** Returns the option pane. */
    protected final JOptionPane getOptionPane() {
        return optionPane;
    }

    /** Returns answer pane in a scroll pane. */
    protected final JScrollPane getAnswerPane(final String initialText) {
        answerPane = new JEditorPane(Tools.MIME_TYPE_TEXT_PLAIN, initialText);
        answerPane.setBackground(
                            Tools.getDefaultColor("ConfigDialog.AnswerPane"));
        answerPane.setForeground(java.awt.Color.WHITE);
        //answerPane.setBackground(
        //                    Tools.getDefaultColor("ConfigDialog.AnswerPane"));
        //descSP.setBorder(null);
        answerPane.setEditable(false);
        final JScrollPane scrollPane = new JScrollPane(answerPane);
        scrollPane.setHorizontalScrollBarPolicy(
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));
        scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, 80));
        return scrollPane;
    }

    /** Sets text to the answer pane. */
    protected final void answerPaneSetText(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final int l = answerPaneText.length();
                if (l > 1) {
                    answerPaneText.delete(0, l);
                }
                answerPaneText.append(text);
                answerPane.setText(answerPaneText.toString());
            }
        });
    }

    /** Appends text to the answer pane. */
    protected final void answerPaneAddText(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                answerPaneText.append('\n');
                answerPaneText.append(text);
                answerPaneSetText(answerPaneText.toString());
            }
        });
    }


    /**
     * Sets the error text in the answer pane and sets the text color to red.
     */
    protected final void answerPaneSetTextError(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                answerPane.setForeground(
                       Tools.getDefaultColor("ConfigDialog.AnswerPane.Error"));
                final int l = answerPaneText.length();
                if (l > 1) {
                    answerPaneText.delete(0, l);
                }
                answerPaneText.append(text);
                answerPane.setText(answerPaneText.toString());
            }
        });
    }

    /** Appends the error text to the answer pane. */
    protected final void answerPaneAddTextError(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                answerPaneText.append('\n');
                answerPaneText.append(text);
                answerPaneSetTextError(answerPaneText.toString());
            }
        });
    }

    /**
     * Creates body of the dialog. You can redefine getDialogTitle(),
     * getDescription(), getInputPane() methods to customize this
     * body.
     */
    protected final JPanel body() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        final JEditorPane descPane = new JEditorPane(
           Tools.MIME_TYPE_TEXT_HTML,
           "<span style='font:bold italic;font-family:Dialog; font-size:"
           + Tools.getConfigData().scaled(14)
           + ";'>"
           + getDialogTitle() + "</span><br>"
           + "<span style='font-family:Dialog; font-size:"
           + Tools.getConfigData().scaled(12)
           + ";'>"
           + getDescription() + "</span>");
        descPane.setSize(300, Integer.MAX_VALUE);

        descPane.setBackground(
                            Tools.getDefaultColor("ConfigDialog.Background"));
        descPane.setEditable(false);
        final JScrollPane descSP = new JScrollPane(descPane);
        descSP.setBorder(null);
        descSP.setAlignmentX(Component.LEFT_ALIGNMENT);
        descSP.setMinimumSize(new Dimension(0, 50));
        pane.add(descSP);
        final JComponent inputPane = getInputPane();
        if (inputPane != null) {
            inputPane.setMinimumSize(new Dimension(Short.MAX_VALUE,
                                                     INPUT_PANE_HEIGHT));
            inputPane.setBackground(
                            Tools.getDefaultColor("ConfigDialog.Background"));
            inputPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            pane.add(inputPane);
        }
        pane.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Light"));
        return pane;
    }

    /** Returns an icon or null for default icon. */
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

    /** One default button from the buttons() method. */
    protected String defaultButton() {
        return okButton();
    }

    /** Compares pressed button with button that is passed as parameter. */
    public final boolean isPressedButton(final String button) {
        return pressedButton.equals(button);
    }

    /** Set pressed button to the string passed as a parameter. */
    protected final void setPressedButton(final String button) {
        pressedButton = button;
    }

    /** Returns localized string for Ok button. */
    final String okButton() {
        return buttonString("Ok");
    }


    /** Returns localized string of Cancel button. */
    protected String cancelButton() {
        return buttonString("Cancel");
    }

    /**
     * TextResource files contain texts in different languages. Text for every
     * button has to be defined there. If Ok button is used, resource file
     * has to contain ConfigDialog.Ok item.
     */
    protected final String buttonString(final String b) {
        return Tools.getString("Dialog.Dialog." + b);
    }

    /**
     * Returns class of the button, so that it can be enabled or
     * disabled.
     */
    protected final MyButton buttonClass(final String button) {
        return buttonToObjectMap.get(button);
    }

    /**
     * This method is called before the dialog is shown. It also disables
     * buttons. They have to be enabled with enableComponents()
     */
    protected void initDialog() {
    }

    /**
     * This method is called immediatly after the dialog is shown.
     */
    protected void initDialogAfterVisible() {
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
                    @Override
                    public void insertUpdate(final DocumentEvent e) {
                        final Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                checkFields(field);
                            }
                        });
                        t.start();
                    }

                    @Override
                    public void removeUpdate(final DocumentEvent e) {
                        final Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                checkFields(field);
                            }
                        });
                        t.start();
                    }

                    @Override
                    public void changedUpdate(final DocumentEvent e) {
                        final Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                checkFields(field);
                            }
                        });
                        t.start();
                    }
                });
    }

    /** This method is called after user has pushed the button. */
    protected ConfigDialog checkAnswer() {
        return null;
    }

    /** Returns the width of the dialog. */
    protected int dialogWidth() {
        return Tools.getDefaultInt("ConfigDialog.width");
    }

    /** Returns the height of the dialog. */
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

    /** Whether the skip button is enabled. */
    protected boolean skipButtonEnabled() {
        return false;
    }

    /** Returns the listener for the skip button. */
    protected ItemListener skipButtonListener() {
        return null;
    }

    /** Returns whether skip button is selected. */
    protected final boolean skipButtonIsSelected() {
        if (skipButton != null) {
            return skipButton.isSelected();
        }
        return false;
    }

    /** Enable/disable skip button. */
    protected final void skipButtonSetEnabled(final boolean enable) {
        skipButtonShouldBeEnabled = enable;
    }

    /**
     * Shows dialog and wait for answer.
     * Returns next dialog, or null if it there is no next dialog.
     */
    public final ConfigDialog showDialog() {
        /* making non modal dialog */
        dialogGate = new CountDownLatch(1);
        dialogPanel = null; /* TODO: disabled caching because back button
                               wouldn't work with
                               dialogPanel.setContentPane(optionPane) method
                               it would work with optionPane.createDialog...
                               but that causes lockups with old javas and
                               gnome. */
        if (dialogPanel == null) {
            final ImageIcon[] icons = getIcons();
            MyButton defaultButtonClass = null;
            final List<JComponent> allOptions =
                                new ArrayList<JComponent>(additionalOptions);
            if (skipButtonEnabled()) {
                skipButton = new JCheckBox(Tools.getString(
                                            "Dialog.ConfigDialog.SkipButton"));
                skipButton.setEnabled(false);
                skipButton.setBackground(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));
                skipButton.addItemListener(skipButtonListener());
                allOptions.add(skipButton);
            }
            final String[] buttons = buttons();
            /* populate buttonToObjectMap */
            for (int i = 0; i < buttons.length; i++) {
                options[i] = new MyButton(buttons[i], icons[i]);
                options[i].setBackgroundColor(
                               Tools.getDefaultColor("ConfigDialog.Button"));
                allOptions.add(options[i]);
                buttonToObjectMap.put(buttons[i], options[i]);
                if (buttons[i].equals(defaultButton())) {
                    defaultButtonClass = options[i];
                }
            }
            /* create option pane */
            final JPanel b = body();
            final MyButton dbc = defaultButtonClass;
            Tools.invokeAndWait(new Runnable() {
                public void run() {
                    optionPane = new JOptionPane(
                                        b,
                                        getMessageType(),
                                        JOptionPane.DEFAULT_OPTION,
                                        icon(),
                                        allOptions.toArray(
                                         new JComponent[allOptions.size()]),
                                        dbc);
                    optionPane.setPreferredSize(
                                        new Dimension(dialogWidth(),
                                                      dialogHeight()));
                    optionPane.setMaximumSize(
                                        new Dimension(dialogWidth(),
                                                      dialogHeight()));
                    optionPane.setMinimumSize(
                                        new Dimension(dialogWidth(),
                                                      dialogHeight()));

                    optionPane.setBackground(
                               Tools.getDefaultColor(
                                          "ConfigDialog.Background.Dark"));
                    final Container mainFrame =
                                    Tools.getGUIData().getMainFrame();
                    if (mainFrame instanceof JApplet) {
                        final JFrame noframe = new JFrame();
                        dialogPanel = new JDialog(noframe);
                        dialogPanel.setContentPane(optionPane);
                    } else {
                        dialogPanel = new JDialog((JFrame) mainFrame);
                        dialogPanel.setContentPane(optionPane);
                    }
                    dialogPanel.setModal(false);
                    dialogPanel.setResizable(true);
                }
            });
            /* set location like the previous dialog */
        }
        /* add action listeners */
        final Map<MyButton,
                  OptionPaneActionListener> optionPaneActionListeners =
                             new HashMap<MyButton, OptionPaneActionListener>();
        for (final MyButton o : options) {
            final OptionPaneActionListener ol = new OptionPaneActionListener();
            optionPaneActionListeners.put(o, ol);
            o.addActionListener(ol);
        }

        final PropertyChangeListener propertyChangeListener =
            new PropertyChangeListener() {
                @Override
                public void propertyChange(final PropertyChangeEvent evt) {
                    if (JOptionPane.VALUE_PROPERTY.equals(
                                                        evt.getPropertyName())
                        && !"uninitializedValue".equals(evt.getNewValue())) {
                        optionPaneAnswer = optionPane.getValue();
                        dialogGate.countDown();
                    }
                }
            };
        optionPane.addPropertyChangeListener(propertyChangeListener);
        initDialog();
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                dialogPanel.setPreferredSize(
                        new Dimension(dialogWidth(), dialogHeight()));
                dialogPanel.setMaximumSize(
                        new Dimension(dialogWidth(), dialogHeight()));
                dialogPanel.setMinimumSize(
                        new Dimension(dialogWidth(), dialogHeight()));
                dialogPanel.setLocationRelativeTo(
                               Tools.getGUIData().getMainFrame());
                dialogPanel.setVisible(true);
            }
        });
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                dialogPanel.setLocationRelativeTo(
                               Tools.getGUIData().getMainFrameContentPane());
                /* although the location was set before, it is set again as a
                 * workaround for gray dialogs with nothing in it, that appear
                 * in some comination of Java and compiz. */
            }
        });
        initDialogAfterVisible();
        try {
            dialogGate.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        if (optionPaneAnswer instanceof String) {
            setPressedButton((String) optionPaneAnswer);
        } else {
            setPressedButton(cancelButton());
        }
        optionPane.removePropertyChangeListener(propertyChangeListener);
        /* remove action listeners */
        for (final MyButton o : options) {
            o.removeActionListener(optionPaneActionListeners.get(o));
        }
        dialogPanel.dispose();
        return checkAnswer();
    }

    /**
     * Disables array of components and all the buttons. The ones that
     * were enabled are stored in disabledComponents list so that they
     * can be later enabled with call to enableComponents.
     */
    protected final void disableComponents(final JComponent[] components) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
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
    protected void enableComponents(final JComponent[] componentsToDisable) {
        final HashSet<JComponent> ctdHash =
                new HashSet<JComponent>(Arrays.asList(componentsToDisable));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final JComponent dc : disabledComponents) {
                    if (!ctdHash.contains(dc)) {
                        dc.setEnabled(true);
                    }
                }
                disabledComponents.clear();
                if (skipButton != null) {
                    skipButton.setEnabled(skipButtonShouldBeEnabled);
                }
            }
        });
    }

    /**
     * Enables components after disableComponents, but they will be really
     * enabled only after enableComponents without arguments is be called.
     */
    protected void enableComponentsLater(
                                    final JComponent[] componentsToEnable) {
        for (final JComponent c : componentsToEnable) {
            disabledComponents.add(c);
        }
    }

    /** Enables components that were disabled with disableComponents. */
    protected void enableComponents() {
        enableComponents(new JComponent[]{});
    }

    /** Is called after dialog was canceled. It does nothing by default. */
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
        @Override
        public void actionPerformed(final ActionEvent event) {
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
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

    /** Returns panel with checkbox. */
    protected final JPanel getComponentPanel(final String text,
                                             final JComponent component) {
        final JPanel mp = new JPanel(
                     new FlowLayout(FlowLayout.LEFT, 0, 0));
        mp.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        mp.add(new JLabel(text));
        mp.add(new JLabel(" "));
        mp.add(component);
        mp.setAlignmentX(Component.LEFT_ALIGNMENT);
        return mp;
    }

    /** Close dialog other than pressing button. */
    protected final void disposeDialog() {
        dialogGate.countDown();
    }

    /** Add the compoment to the options. */
    protected final void addToOptions(final JComponent c) {
        additionalOptions.add(c);
    }
}
