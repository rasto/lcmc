/*
 * This file is part of LCMC
 *
 * Copyright (C) 2012, Rastislav Levrinc.
 *
 * LCMC is free software; you can redistribute it and/or
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

package lcmc.gui.widget;

import lcmc.utilities.Tools;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.gui.SpringUtilities;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.text.Document;
import javax.swing.JLabel;
import javax.swing.Box;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;

import java.awt.BorderLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;

import java.awt.Component;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public abstract class Widget extends JPanel {
    /** Widget type. */
    public enum Type { LABELFIELD, TEXTFIELD, PASSWDFIELD, COMBOBOX,
                       RADIOGROUP, CHECKBOX, TEXTFIELDWITHUNIT };
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Component of this widget. */
    private JComponent component;
    /** Whether the field is editable. */
    private boolean editable = false;
    /** Whether the field should be always editable. */
    private boolean alwaysEditable = false;
    /** File chooser button or some other button. */
    private MyButton fieldButton;
    /** Component part of field with button. */
    private JComponent componentPart = null;
    /** Background of the field if the value is wrong. */
    private static final Color ERROR_VALUE_BACKGROUND =
                            Tools.getDefaultColor("Widget.ErrorValue");
    /** Background of the field if the value has changed. */
    private static final Color CHANGED_VALUE_COLOR =
                            Tools.getDefaultColor("Widget.ChangedValue");
    /** Background of the field if the value is default. */
    private static final Color DEFAULT_VALUE_COLOR =
                            Tools.getDefaultColor("Widget.DefaultValue");
    /** Background of the field if the value is saved. */
    private static final Color SAVED_VALUE_COLOR =
                            Tools.getDefaultColor("Widget.SavedValue");
    /** No scrollbar ever. */
    protected static final int SCROLLBAR_MAX_ROWS = 10;
    /** Widget default height. */
    protected static final int WIDGET_HEIGHT = 28;
    /** Widget enclosing component default height. */
    private static final int WIDGET_COMPONENT_HEIGHT = 30;
    /** Nothing selected string, that returns null, if selected. */
    public static final String NOTHING_SELECTED =
                                Tools.getString("Widget.NothingSelected");
    /** Label of this component. */
    private JLabel label = null;
    /** Whether the component should be enabled. */
    private boolean enablePredicate = true;
    /** Whether the extra text field button should be enabled. */
    private boolean tfButtonEnabled = true;
    /** Access Type for this component to become enabled. */
    private AccessMode enableAccessMode = new AccessMode(
                                                    ConfigData.AccessType.RO,
                                                    false);
    /** Tooltip if element is enabled. */
    private String toolTipText = null;
    /** Tooltip for label if it is enabled. */
    private String labelToolTipText = null;
    /** getValue setValue lock. */
    private final ReadWriteLock mValueLock = new ReentrantReadWriteLock();
    private final Lock mValueReadLock = mValueLock.readLock();
    private final Lock mValueWriteLock = mValueLock.writeLock();
    /** Regexp that this field must match. */
    private final String regexp;
    /** Reason why it is disabled. */
    private String disabledReason = null;
    /** List of widget listeners. */
    private final List<WidgetListener> widgetListeners =
                                              new ArrayList<WidgetListener>();

    public static final Type GUESS_TYPE = null;
    public static final String NO_DEFAULT = null;
    public static final Object[] NO_ITEMS = null;
    public static final String NO_REGEXP = null;
    public static final Map<String, String> NO_ABBRV = null;
    public static final MyButton NO_BUTTON = null;

    /** Prepares a new <code>Widget</code> object. */
    public Widget(final String regexp,
                  final AccessMode enableAccessMode) {
        this(regexp,
             enableAccessMode,
             NO_BUTTON); /* without button */
    }

    /** Prepares a new <code>Widget</code> object. */
    public Widget(final String regexp,
                  final AccessMode enableAccessMode,
                  final MyButton fieldButton) {
        super();
        this.enableAccessMode = enableAccessMode;
        this.fieldButton = fieldButton;
        setLayout(new BorderLayout(0, 0));
        if (regexp != null && regexp.indexOf("@NOTHING_SELECTED@") > -1) {
            this.regexp =
                    regexp.replaceAll("@NOTHING_SELECTED@", NOTHING_SELECTED);
        } else {
            this.regexp = regexp;
        }
    }

    protected final void addComponent(final JComponent newComp,
                                      final int width) {
        if (fieldButton == null) {
            component = newComp;
        } else {
            componentPart = newComp;
            componentPart.setPreferredSize(new Dimension(width / 3 * 2,
                                                         WIDGET_HEIGHT));
            componentPart.setMinimumSize(componentPart.getPreferredSize());
            componentPart.setMaximumSize(componentPart.getPreferredSize());
            component = new JPanel();
            component.setLayout(new SpringLayout());

            component.add(newComp);
            component.add(fieldButton);
            /** add button */
            SpringUtilities.makeCompactGrid(component, 1, 2,
                                                       0, 0,
                                                       0, 0);
        }
        component.setPreferredSize(new Dimension(width, WIDGET_HEIGHT));
        if (componentPart != null) {
            componentPart.setPreferredSize(new Dimension(width, WIDGET_HEIGHT));
        }
        setPreferredSize(new Dimension(width, WIDGET_COMPONENT_HEIGHT));
        if (width != 0) {
            component.setMaximumSize(new Dimension(width, WIDGET_HEIGHT));
            setMaximumSize(new Dimension(width, WIDGET_COMPONENT_HEIGHT));
        }

        add(Box.createRigidArea(new Dimension(0, 1)), BorderLayout.PAGE_START);
        add(component, BorderLayout.CENTER);
        add(Box.createRigidArea(new Dimension(0, 1)), BorderLayout.PAGE_END);
        processAccessMode();
    }

    public void reloadComboBox(final String selectedValue,
                               final Object[] items) {
    }

    /** Sets the tooltip text. */
    @Override
    public void setToolTipText(String text) {
        toolTipText = text;
        final String disabledReason0 = disabledReason;
        if (disabledReason0 != null) {
            text = text + "<br>" + disabledReason0;
        }
        if (enableAccessMode.getAccessType() != ConfigData.AccessType.NEVER) {
            final boolean accessible =
                     Tools.getConfigData().isAccessible(enableAccessMode);
            if (!accessible) {
                text = text + "<br>" + getDisabledTooltip();
            }
        }
        component.setToolTipText("<html>" + text + "</html>");
        if (fieldButton != null) {
            componentPart.setToolTipText("<html>" + text + "</html>");
            fieldButton.setToolTipText("<html>" + text + "</html>");
        }
    }

    /** Sets label tooltip text. */
    private void setLabelToolTipText(String text) {
        labelToolTipText = text;
        if (text == null || label == null) {
            return;
        }
        String disabledTooltip = null;
        if (enableAccessMode.getAccessType() != ConfigData.AccessType.NEVER) {
            final boolean accessible =
                     Tools.getConfigData().isAccessible(enableAccessMode);
            if (!accessible) {
                disabledTooltip = getDisabledTooltip();
            }
        }
        final String disabledReason0 = disabledReason;
        if (disabledReason0 != null || disabledTooltip != null) {
            final StringBuilder tt = new StringBuilder(40);
            if (disabledReason0 != null) {
                tt.append(disabledReason0);
                tt.append("<br>");
            }
            if (disabledTooltip != null) {
                tt.append(disabledTooltip);
            }
            if (text.length() > 6 && "<html>".equals(text.substring(0, 6))) {
                text = "<html>" + tt.toString() + "<br>" + "<br>"
                       + text.substring(6);
            } else {
                text = Tools.html(text + "<br>" + tt.toString());
            }
        }
        label.setToolTipText(text);
    }

    /** Returns tooltip for disabled element. */
    private String getDisabledTooltip() {
        String advanced = "";
        if (enableAccessMode.isAdvancedMode()) {
            advanced = "Advanced ";
        }
        final StringBuilder sb = new StringBuilder(100);
        sb.append("editable in \"");
        sb.append(advanced);
        sb.append(
                ConfigData.OP_MODES_MAP.get(enableAccessMode.getAccessType()));
        sb.append("\" mode");

        if (disabledReason != null) {
            /* yet another reason */
            sb.append(' ');
            sb.append(disabledReason);
        }

        return sb.toString();
    }

    /** Sets the field editable. */
    public final void setEditable() {
        setEditable(editable);
    }

    /** Sets combo box editable. */
    public void setEditable(final boolean editable) {
        this.editable = editable;
    }

    /**
     * Returns string value. If object value is null, returns empty string (not
     * null).
     */
    public abstract String getStringValue();

    public final Object getValue() {
        mValueReadLock.lock();
        final Object value = getValueInternal();
        mValueReadLock.unlock();
        return value;
    }

    /** Return value, that user have chosen in the field or typed in. */
    abstract Object getValueInternal();

    /** Clears the combo box. */
    public void clear() {
    }

    /** Sets component visible or invisible and remembers this state. */
    @Override
    public final void setVisible(final boolean visible) {
        setComponentsVisible(visible);
    }

    /** Sets component visible or invisible. */
    protected void setComponentsVisible(final boolean visible) {
        JComponent c;
        if (fieldButton == null) {
            c = component;
        } else {
            c = componentPart;
        }
        final JComponent comp = c;
        super.setVisible(visible);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (label != null) {
                    label.setVisible(visible);
                }
                comp.setVisible(visible);
                repaint();
            }
        });
    }


    /** Sets component enabled or disabled and remembers this state. */
    @Override
    public final void setEnabled(final boolean enabled) {
        enablePredicate = enabled;
        setComponentsEnabled(
                   enablePredicate
                   && Tools.getConfigData().isAccessible(enableAccessMode));
    }

    /** Sets extra button enabled. */
    public final void setTFButtonEnabled(final boolean tfButtonEnabled) {
        this.tfButtonEnabled = tfButtonEnabled;
        fieldButton.setEnabled(tfButtonEnabled);
    }

    /** Sets component enabled or disabled. */
    protected void setComponentsEnabled(final boolean enabled) {
        component.setEnabled(enabled);
        if (fieldButton != null) {
            componentPart.setEnabled(enabled);
            fieldButton.setEnabled(enabled && tfButtonEnabled);
        }
    }

    /**
     * Enables/Disables component in a group of components identified by
     * specified string. This works only with RADIOGROUP at the moment.
     */
    public void setEnabled(final String s, final boolean enabled) {
    }

    /** Returns whether component is editable or not. */
    abstract boolean isEditable();

    /** Sets item/value in the component and waits till it is set. */
    public final void setValueAndWait(final Object item) {
        if (Tools.areEqual(item, getValue())) {
            return;
        }
        mValueWriteLock.lock();
        setValueAndWait0(item);
        mValueWriteLock.unlock();
    }

    /** Sets item/value in the component and waits till it is set. */
    protected abstract void setValueAndWait0(final Object item);

    /** Sets item/value in the component, disable listeners. */
    public final void setValueNoListeners(final Object item) {
        if (Tools.areEqual(item, getValue())) {
            return;
        }
        for (final WidgetListener wl : widgetListeners) {
            wl.setEnabled(false);
        }
        mValueWriteLock.lock();
        setValueAndWait0(item);
        mValueWriteLock.unlock();
        for (final WidgetListener wl : widgetListeners) {
            wl.setEnabled(true);
        }
    }

    /** Sets item/value in the component. */
    public final void setValue(final Object item) {
        if (Tools.areEqual(item, getValue())) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                mValueWriteLock.lock();
                setValueAndWait0(item);
                mValueWriteLock.unlock();
            }
        });
    }

    /** Sets selected index. */
    public void setSelectedIndex(final int index) {
    }

    /** Returns document object of the component. */
    public abstract Document getDocument();

    /** Selects part after first '*' in the ip. */
    public void selectSubnet() {
    }

    protected final void addDocumentListener(final Document doc,
                                             final WidgetListener wl) {
        doc.addDocumentListener(
                new DocumentListener() {
                    private void check(final DocumentEvent e) {
                        if (wl.isEnabled()) {
                            try {
                                final String text =
                                   e.getDocument().getText(0, doc.getLength());

                                final Thread t = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        wl.check(text);
                                    }
                                });
                                t.start();
                            } catch (BadLocationException ble) {
                                Tools.appWarning("document listener error");
                            }
                        }
                    }

                    @Override
                    public void insertUpdate(final DocumentEvent e) {
                        check(e);
                    }

                    @Override
                    public void removeUpdate(final DocumentEvent e) {
                        check(e);
                    }

                    @Override
                    public void changedUpdate(final DocumentEvent e) {
                        check(e);
                    }
                }
            );

    }

    protected ItemListener getItemListener(final WidgetListener wl) {
        return new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (wl.isEnabled()
                    && e.getStateChange() == ItemEvent.SELECTED) {
                    final Object value = e.getItem();
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            wl.check(value);
                        }
                    });
                    t.start();
                }
            }
        };
    }

    /** Adds item listener to the component. */
    public void addListeners(final WidgetListener wl) {
        widgetListeners.add(wl);
    }

    /**
     * Sets the background for the component which value is incorrect (failed).
     */
    public final void wrongValue() {
        setBackgroundColor(ERROR_VALUE_BACKGROUND);
        if (label != null) {
            label.setForeground(Color.RED);
        }
    }

    /** Sets background without considering the label. */
    public final void setBackground(final Object defaultValue,
                                    final Object savedValue,
                                    final boolean required) {
        setBackground(null, defaultValue, null, savedValue, required);
    }

    /**
     * Sets background of the component depending if the value is the same
     * as its default value and if it is a required argument.
     * Must be called after combo box was already added to some panel.
     *
     * It also disables, hides the component depending on the access type.
     * TODO: rename the function
     */
    public final void setBackground(final String defaultLabel,
                                    final Object defaultValue,
                                    final String savedLabel,
                                    final Object savedValue,
                                    final boolean required) {
        if (getParent() == null) {
            return;
        }
        JComponent comp;
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        final Object value = getValue();
        String labelText = null;
        if (savedLabel != null) {
            labelText = label.getText();
        }

        final Color backgroundColor = getParent().getBackground();
        final Color compColor = Color.WHITE;
        if (!Tools.areEqual(value, savedValue)
            || (savedLabel != null && !Tools.areEqual(labelText, savedLabel))) {
            if (label != null) {
                Tools.debug(this, "changed label: " + labelText + " != "
                                   + savedLabel, 1);
                Tools.debug(this, "changed: " + value + " != "
                                         + savedValue, 1);
                /*
                   Tools.printStackTrace("changed: " + value + " != "
                                         + savedValue);
                 */
                label.setForeground(CHANGED_VALUE_COLOR);
            }
        } else if (Tools.areEqual(value, defaultValue)
                   && (savedLabel == null
                       || Tools.areEqual(labelText, defaultLabel))) {
            if (label != null) {
                label.setForeground(DEFAULT_VALUE_COLOR);
            }
        } else {
            if (label != null) {
                label.setForeground(SAVED_VALUE_COLOR);
            }
        }
        setBackground(backgroundColor);
        setComponentBackground(backgroundColor, compColor);
        processAccessMode();
    }

    protected void setComponentBackground(final Color backgroundColor,
                                          final Color compColor) {
    }

    /** Workaround for jcombobox so that it works with default button. */
    static class ActivateDefaultButtonListener extends KeyAdapter
                                               implements ActionListener {
        /** Combobox, that should work with default button. */
        private final JComboBox box;

        /** Creates new ActivateDefaultButtonListener. */
        ActivateDefaultButtonListener(final JComboBox box) {
            super();
            this.box = box;
        }

        /** Is called when a key was pressed. */
        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                /* Simulte click on default button. */
                doClick(e);
            }
        }

        /** Is called when an action was performed. */
        @Override
        public void actionPerformed(final ActionEvent e) {
            doClick(e);
        }

        /** Do click. */
        private void doClick(final java.util.EventObject e) {
            final Component c = (Component) e.getSource();

            final JRootPane rootPane = SwingUtilities.getRootPane(c);

            if (rootPane != null) {
                final JButton defaultButton = rootPane.getDefaultButton();

                if (defaultButton != null && !box.isPopupVisible()) {
                    final Object selection = box.getEditor().getItem();
                    box.setSelectedItem(selection);
                    defaultButton.doClick();
                }
            }
        }
    }

    /**
     * TextField that selects all when focused.
     */
    public static final class MTextField extends JTextField {
        /** Serial Version UID. */
        private static final long serialVersionUID = 1L;
        /** To select all only once. */
        private volatile boolean selected = false;

        /** Creates a new MTextField object. */
        public MTextField(final String text) {
            super(text);
        }

        /** Creates a new MTextField object. */
        public MTextField(final String text,
                          final int columns) {
            super(text, columns);
        }

        /** Creates a new MTextField object. */
        MTextField(final Document doc,
                   final String text,
                   final int columns) {
            super(doc, text, columns);
        }

        /** Focus event. */
        @Override
        protected void processFocusEvent(final FocusEvent e) {
            super.processFocusEvent(e);
            if (!selected) {
                selected = true;
                if (e.getID() == FocusEvent.FOCUS_GAINED) {
                    selectAll();
                }
            }
        }
    }

    /** Sets flag that determines whether the combo box is always editable. */
    public final void setAlwaysEditable(final boolean alwaysEditable) {
        this.alwaysEditable = alwaysEditable;
        setEditable(alwaysEditable);
    }

    protected final boolean isAlwaysEditable() {
        return alwaysEditable;
    }

    /** Requests focus if applicable. */
    @Override
    public void requestFocus() {
    }

    /** Selects the whole text in the widget if applicable. */
    void selectAll() {
    }

    /** Sets the width of the widget. */
    public final void setWidth(final int newWidth) {
        final int hc = (int) component.getPreferredSize().getHeight();
        final int h = (int) getPreferredSize().getHeight();
        component.setMinimumSize(new Dimension(newWidth, hc));
        component.setPreferredSize(new Dimension(newWidth, hc));
        component.setMaximumSize(new Dimension(newWidth, hc));
        setMinimumSize(new Dimension(newWidth, h));
        setPreferredSize(new Dimension(newWidth, h));
        setMaximumSize(new Dimension(newWidth, h));
        revalidate();
        component.revalidate();
        repaint();
        component.repaint();
    }

    /** Returns its component. */
    final JComponent getJComponent() {
        return component;
    }

    /** Sets background color. */
    public void setBackgroundColor(final Color bg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setBackground(bg);
            }
        });
    }

    /** Sets label for this component. */
    public final void setLabel(final JLabel label,
                               final String labelToolTipText) {
        this.label = label;
        this.labelToolTipText = labelToolTipText;
    }

    /** Returns label for this component. */
    public final JLabel getLabel() {
        return label;
    }

    /** Sets this item enabled and visible according to its access type. */
    public final void processAccessMode() {
        final boolean accessible =
                       Tools.getConfigData().isAccessible(enableAccessMode);
        setComponentsEnabled(enablePredicate && accessible);
        if (toolTipText != null) {
            setToolTipText(toolTipText);
        }
        if (label != null) {
            if (labelToolTipText != null) {
                setLabelToolTipText(labelToolTipText);
            }
            label.setEnabled(enablePredicate && accessible);
        }
    }

    /** Returns item at the specified index. */
    Object getItemAt(final int i) {
        return null;
    }

    /** Cleanup whatever would cause a leak. */
    public void cleanup() {
        widgetListeners.clear();
    }

    /** Returns regexp of this field. */
    public final String getRegexp() {
        return regexp;
    }

    /** Sets reason why it is disabled. */
    public final void setDisabledReason(final String disabledReason) {
        this.disabledReason = disabledReason;
    }

    /** Returns component. */
    protected final JComponent getComponent() {
        if (fieldButton == null) {
            return component;
        } else {
            return componentPart;
        }
    }

    /** Returns widget listeners. */
    protected final List<WidgetListener> getWidgetListeners() {
        return widgetListeners;
    }

    /** Return enable predicate. */
    protected final boolean isEnablePredicate() {
        return enablePredicate;
    }

    /** Return access mode at which this component is enabled. */
    protected final AccessMode getEnableAccessMode() {
        return enableAccessMode;
    }
}
