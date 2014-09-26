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

package lcmc.gui.dialog;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import lcmc.gui.GUIData;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.model.Application;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog with buttons. Ok button is predefined.
 * The dialogs should extend this class and overwrite at least
 * getDialogTitle(), getDescription(), getInputPane() and nextDialog() methods.
 */
@Named
public abstract class ConfigDialog {
    private static final int INPUT_PANE_HEIGHT = 200;
    private volatile JOptionPane optionPane;
    private JDialog dialogPanel;
    private String pressedButton = "";
    private final Map<String, MyButton> buttonToObjectMap = new HashMap<String, MyButton>();
    private final StringBuilder answerPaneText = new StringBuilder(100);
    private JEditorPane answerPane = null;
    private final Collection<java.awt.Component> disabledComponents = new ArrayList<java.awt.Component>();
    private CountDownLatch dialogGate;
    private JCheckBox skipButton = null;
    private volatile Object optionPaneAnswer;
    private boolean skipButtonShouldBeEnabled = true;
    private final List<JComponent> additionalOptions = new ArrayList<JComponent>();
    @Inject
    private Application application;
    @Inject
    private WidgetFactory widgetFactory;
    private final MyButton[] options = new MyButton[buttons().length];
    @Inject
    private GUIData guiData;

    protected final JDialog getDialogPanel() {
        return dialogPanel;
    }

    final void setDialogPanel(final JDialog dialogPanel) {
        this.dialogPanel = dialogPanel;
    }

    final Point getLocation() {
        return dialogPanel.getLocation();
    }

    protected abstract String getDialogTitle();

    protected abstract String getDescription();

    protected abstract JComponent getInputPane();

    protected final JOptionPane getOptionPane() {
        return optionPane;
    }

    protected final JScrollPane getAnswerPane(final String initialText) {
        answerPane = new JEditorPane(GUIData.MIME_TYPE_TEXT_PLAIN, initialText);
        answerPane.setBackground(Tools.getDefaultColor("ConfigDialog.AnswerPane"));
        answerPane.setForeground(Color.WHITE);
        answerPane.setEditable(false);
        final JScrollPane scrollPane = new JScrollPane(answerPane);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 80));
        scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, 80));
        return scrollPane;
    }

    protected final void answerPaneSetText(final String text) {
        application.invokeLater(new Runnable() {
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

    protected final void answerPaneAddText(final String text) {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                answerPaneText.append('\n');
                answerPaneText.append(text);
                answerPaneSetText(answerPaneText.toString());
            }
        });
    }


    protected final void answerPaneSetTextError(final String text) {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                answerPane.setForeground(Tools.getDefaultColor("ConfigDialog.AnswerPane.Error"));
                final int l = answerPaneText.length();
                if (l > 1) {
                    answerPaneText.delete(0, l);
                }
                answerPaneText.append(text);
                answerPane.setText(answerPaneText.toString());
            }
        });
    }

    protected final void answerPaneAddTextError(final String text) {
        application.invokeLater(new Runnable() {
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
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        final JEditorPane descPane = new JEditorPane(GUIData.MIME_TYPE_TEXT_HTML,
                                                     "<span style='font:bold italic;font-family:Dialog; font-size:"
                                                     + application.scaled(14)
                                                     + ";'>"
                                                     + getDialogTitle() + "</span><br>"
                                                     + "<span style='font-family:Dialog; font-size:"
                                                     + application.scaled(12)
                                                     + ";'>"
                                                     + getDescription() + "</span>");
        descPane.setSize(300, Integer.MAX_VALUE);

        descPane.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        descPane.setEditable(false);
        final JScrollPane descSP = new JScrollPane(descPane);
        descSP.setBorder(null);
        descSP.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        descSP.setMinimumSize(new Dimension(0, 50));
        pane.add(descSP);
        final JComponent inputPane = getInputPane();
        if (inputPane != null) {
            inputPane.setMinimumSize(new Dimension(Short.MAX_VALUE, INPUT_PANE_HEIGHT));
            inputPane.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
            inputPane.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            pane.add(inputPane);
        }
        pane.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));
        return pane;
    }

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

    /** This method is called before the dialog is screated.  */
    protected void initDialogBeforeCreated() {
    }

    /**
     * This method is called before the dialog is shown but it was created.
     * It also disables buttons. They have to be enabled with enableComponents()
     */
    protected void initDialogBeforeVisible() {
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
    protected void checkFields(final Widget field) {
        /* Does nothing by default. */
    }

    /**
     * Add listener to the field. The checkFields(field) will be called on every
     * insert, update and remove event.
     */
    protected final void addCheckField(final Widget field) {
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
        return skipButton != null && skipButton.isSelected();
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
        initDialogBeforeCreated();
        if (dialogPanel == null) {
            final ImageIcon[] icons = getIcons();
            final Collection<JComponent> allOptions = new ArrayList<JComponent>(additionalOptions);
            if (skipButtonEnabled()) {
                skipButton = new JCheckBox(Tools.getString("Dialog.ConfigDialog.SkipButton"));
                skipButton.setEnabled(false);
                skipButton.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));
                skipButton.addItemListener(skipButtonListener());
                allOptions.add(skipButton);
            }
            final String[] buttons = buttons();
            /* populate buttonToObjectMap */
            MyButton defaultButtonClass = null;
            for (int i = 0; i < buttons.length; i++) {
                options[i] = widgetFactory.createButton(buttons[i], icons[i]);
                options[i].setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
                allOptions.add(options[i]);
                buttonToObjectMap.put(buttons[i], options[i]);
                if (buttons[i].equals(defaultButton())) {
                    defaultButtonClass = options[i];
                }
            }
            /* create option pane */
            final MyButton dbc = defaultButtonClass;
            application.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    optionPane = new JOptionPane(body(),
                                                 getMessageType(),
                                                 JOptionPane.DEFAULT_OPTION,
                                                 icon(),
                                                 allOptions.toArray(new JComponent[allOptions.size()]),
                                                 dbc);
                    optionPane.setPreferredSize(new Dimension(dialogWidth(), dialogHeight()));
                    optionPane.setMaximumSize(new Dimension(dialogWidth(), dialogHeight()));
                    optionPane.setMinimumSize(new Dimension(dialogWidth(), dialogHeight()));

                    optionPane.setBackground(Tools.getDefaultColor( "ConfigDialog.Background.Dark"));
                    final Container mainFrame = guiData.getMainFrame();
                    if (mainFrame instanceof JApplet) {
                        final JFrame noframe = new JFrame();
                        dialogPanel = new JDialog(noframe);
                        dialogPanel.setContentPane(optionPane);
                    } else {
                        dialogPanel = new JDialog((Frame) mainFrame);
                        dialogPanel.setContentPane(optionPane);
                    }
                    dialogPanel.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(final WindowEvent e) {
                            disposeDialog();
                        }
                    });
                    dialogPanel.setModal(false);
                    dialogPanel.setResizable(true);
                }
            });
            /* set location like the previous dialog */
        }
        /* add action listeners */
        final Map<MyButton, OptionPaneActionListener> optionPaneActionListeners =
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
                    if (JOptionPane.VALUE_PROPERTY.equals(evt.getPropertyName())
                        && !"uninitializedValue".equals(evt.getNewValue())) {
                        optionPaneAnswer = optionPane.getValue();
                        dialogGate.countDown();
                    }
                }
            };
        optionPane.addPropertyChangeListener(propertyChangeListener);
        initDialogBeforeVisible();
        application.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                dialogPanel.setPreferredSize(new Dimension(dialogWidth(), dialogHeight()));
                dialogPanel.setMaximumSize(new Dimension(dialogWidth(), dialogHeight()));
                dialogPanel.setMinimumSize(new Dimension(dialogWidth(), dialogHeight()));
                dialogPanel.setLocationRelativeTo(guiData.getMainFrame());
                dialogPanel.setVisible(true);
            }
        });
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                dialogPanel.setLocationRelativeTo(guiData.getMainFrame());
                /* although the location was set before, it is set again as a
                 * workaround for gray dialogs with nothing in it, that appear
                 * in some comination of Java and compiz. */
            }
        });
        initDialogAfterVisible();
        try {
            dialogGate.await();
        } catch (final InterruptedException ignored) {
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
    protected final void disableComponents(final java.awt.Component[] components) {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final String b : buttons()) {
                    final JComponent option = buttonClass(b);
                    if (option.isEnabled()) {
                        disabledComponents.add(option);
                        option.setEnabled(false);
                    }
                }
                for (final java.awt.Component c : components) {
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
        final Collection<java.awt.Component> ctdHash = new HashSet<java.awt.Component>(Arrays.asList(componentsToDisable));
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final java.awt.Component dc : disabledComponents) {
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
    protected void enableComponentsLater(final JComponent[] componentsToEnable) {
        Collections.addAll(disabledComponents, componentsToEnable);
    }

    /** Enables components that were disabled with disableComponents. */
    protected void enableComponents() {
        enableComponents(new JComponent[]{});
    }

    /** Is called after dialog was canceled. It does nothing by default. */
    public void cancelDialog() {
        /* Does nothing by default. */
    }

    /** Returns panel with checkbox. */
    protected final JPanel getComponentPanel(final String text, final java.awt.Component component) {
        final JPanel mp = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        mp.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        mp.add(new JLabel(text));
        mp.add(new JLabel(" "));
        mp.add(component);
        mp.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
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

    /**
     * Action listener for custom buttons.
     */
    class OptionPaneActionListener implements ActionListener {

        /**
         * Action performered on custom button.
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            optionPane.setValue(((AbstractButton) e.getSource()).getText());
                        }
                    });
                }
            });
            t.start();
        }
    }
}
