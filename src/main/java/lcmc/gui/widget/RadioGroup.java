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

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.text.Document;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.StringValue;
import lcmc.model.Value;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public final class RadioGroup extends GenericWidget<JComponent> {
    private Value radioGroupValue;
    /** Radio group hash, from string that is displayed to the object. */
    private final Map<String, Value> radioGroupHash = new HashMap<String, Value>();
    /** Name to component hash. */
    private final Map<String, JComponent> componentsHash = new HashMap<String, JComponent>();
    private final ReadWriteLock mComponentsLock = new ReentrantReadWriteLock();
    private final Lock mComponentsReadLock = mComponentsLock.readLock();
    private final Lock mComponentsWriteLock = mComponentsLock.writeLock();
    @Autowired
    private Application application;

    public void init(final Value selectedValue,
                     final Value[] items,
                     final String regexp,
                     final int width,
                     final AccessMode enableAccessMode,
                     final MyButton fieldButton) {
        super.init(regexp, enableAccessMode, fieldButton);
        addComponent(getRadioGroup(selectedValue, items), width);
    }

    protected JComponent getRadioGroup(final Value selectedValue, final Value[] items) {
        final ButtonGroup group = new ButtonGroup();
        final JPanel radioPanel = new JPanel(new GridLayout(1, 1));
        mComponentsWriteLock.lock();
        componentsHash.clear();
        if (items != null) {
            for (final Value item : items) {
                final JRadioButton rb = new JRadioButton(item.getValueForGui());
                radioGroupHash.put(item.getValueForConfig(), item);
                rb.setActionCommand(item.getValueForConfig());
                group.add(rb);
                radioPanel.add(rb);

                componentsHash.put(item.getValueForConfig(), rb);
                if (item.equals(selectedValue)) {
                    rb.setSelected(true);
                    radioGroupValue = selectedValue;
                }

                rb.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        mComponentsReadLock.lock();
                        try {
                            radioGroupValue = radioGroupHash.get(e.getActionCommand());
                        } finally {
                            mComponentsReadLock.unlock();
                        }
                    }
                });
            }
        }
        mComponentsWriteLock.unlock();

        return radioPanel;
    }

    /**
     * Enables/Disables component in a group of components identified by
     * specified string. This works only with RADIOGROUP at the moment.
     */
    @Override
    public void setEnabled(final String s, final boolean enabled) {
        final boolean accessible = application.isAccessible(getEnableAccessMode());
        mComponentsReadLock.lock();
        final JComponent c;
        try {
            c = componentsHash.get(s);
        } finally {
            mComponentsReadLock.unlock();
        }
        if (c != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    c.setEnabled(isEnablePredicate() && accessible);
                }
            });
        }
        final JLabel label = getLabel();
        if (label != null) {
            label.setEnabled(accessible);
        }
    }

    /**
     * Sets visible/invisible a component in a group of components identified
     * by specified string. This works only with RADIOGROUP in a moment.
     */
    public void setVisible(final String s, final boolean visible) {
        mComponentsReadLock.lock();
        final JComponent c;
        try {
            c = componentsHash.get(s);
        } finally {
            mComponentsReadLock.unlock();
        }
        if (c != null) {
            c.setVisible(visible);
        }
    }

    /**
     * Returns string value. If object value is null, returns empty string (not null).
     */
    @Override
    public String getStringValue() {
        final Object o = getValue();
        if (o == null) {
            return "";
        }
        return o.toString();
    }

    /** Return value, that user have chosen in the field or typed in. */
    @Override
    protected Value getValueInternal() {
        final Value value = radioGroupValue;
        if (value == null || value.isNothingSelected()) {
            return null;
        }
        return value;
    }

    /** Sets component visible or invisible. */
    @Override
    protected void setComponentsVisible(final boolean visible) {
        final JComponent comp = getInternalComponent();
        final JLabel label = getLabel();
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (label != null) {
                    label.setVisible(visible);
                }
                comp.setVisible(visible);
                mComponentsReadLock.lock();
                try {
                    for (final JComponent c : componentsHash.values()) {
                        c.setVisible(visible);
                    }
                } finally {
                    mComponentsReadLock.unlock();
                }
                repaint();
            }
        });
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    protected void setValueAndWait0(final Value item) {
        if (item != null) {
            mComponentsReadLock.lock();
            final JRadioButton rb;
            try {
                rb = (JRadioButton) componentsHash.get(item.getValueForConfig());
            } finally {
                mComponentsReadLock.unlock();
            }
            if (rb != null) {
                rb.setSelected(true);
            }
            radioGroupValue = item;
        }
    }

    @Override
    public Document getDocument() {
        return null;
    }

    @Override
    public void addListeners(final WidgetListener widgetListener) {
        getWidgetListeners().add(widgetListener);
        mComponentsReadLock.lock();
        try {
            final ItemListener il = getItemListener(widgetListener);
            for (final JComponent c : componentsHash.values()) {
                ((ItemSelectable) c).addItemListener(il);
            }
        } finally {
            mComponentsReadLock.unlock();
        }
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor, final Color compColor) {
        getInternalComponent().setBackground(backgroundColor);
        mComponentsReadLock.lock();
        try {
            for (final JComponent c : componentsHash.values()) {
                c.setBackground(backgroundColor);
            }
        } finally {
            mComponentsReadLock.unlock();
        }
    }

    @Override
    public void setBackgroundColor(final Color bg) {
        final JComponent comp = getInternalComponent();
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                setBackground(bg);
                comp.setBackground(bg);
                mComponentsReadLock.lock();
                try {
                    for (final JComponent c : componentsHash.values()) {
                        c.setBackground(bg);
                    }
                } finally {
                    mComponentsReadLock.unlock();
                }
            }
        });
    }

    /** Cleanup whatever would cause a leak. */
    @Override
    public void cleanup() {
        getWidgetListeners().clear();
        mComponentsReadLock.lock();
        try {
            for (final JComponent c : componentsHash.values()) {
                for (final ItemListener il : ((AbstractButton) c).getItemListeners()) {
                    ((ItemSelectable) c).removeItemListener(il);
                }
            }
        } finally {
            mComponentsReadLock.unlock();
        }
    }

    @Override
    protected void setComponentsEnabled(final boolean enabled) {
        super.setComponentsEnabled(enabled);
        mComponentsReadLock.lock();
        try {
            for (final JComponent c : componentsHash.values()) {
                c.setEnabled(enabled);
            }
        } finally {
            mComponentsReadLock.unlock();
        }
    }

    @Override
    protected ItemListener getItemListener(final WidgetListener wl) {
        return new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (wl.isEnabled()
                    && e.getStateChange() == ItemEvent.SELECTED) {
                    final Value value = new StringValue(((AbstractButton) e.getItem()).getText());
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
}
