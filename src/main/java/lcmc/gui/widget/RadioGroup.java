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
import lcmc.data.AccessMode;
import lcmc.gui.resources.Info;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.ButtonGroup;


import java.awt.GridLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;

import java.util.Map;
import java.util.HashMap;

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
public final class RadioGroup extends Widget {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Value of the radio group. */
    private String radioGroupValue;
    /** Radio group hash, from string that is displayed to the object. */
    private final Map<String, Object> radioGroupHash =
                                                 new HashMap<String, Object>();
    /** Name to component hash. */
    private final Map<String, JComponent> componentsHash =
                                             new HashMap<String, JComponent>();
    /** group components lock. */
    private final ReadWriteLock mComponentsLock = new ReentrantReadWriteLock();
    private final Lock mComponentsReadLock = mComponentsLock.readLock();
    private final Lock mComponentsWriteLock = mComponentsLock.writeLock();
    /** Prepares a new <code>RadioGroup</code> object. */
    public RadioGroup(final String selectedValue,
                      final Object[] items,
                      final String regexp,
                      final int width,
                      final AccessMode enableAccessMode,
                      final MyButton fieldButton) {
        super(regexp,
              enableAccessMode,
              fieldButton);
        addComponent(getRadioGroup(selectedValue, items), width);
    }

    /** Returns radio group with selected value. */
    protected JComponent getRadioGroup(final String selectedValue,
                                     final Object[] items) {
        final ButtonGroup group = new ButtonGroup();
        final JPanel radioPanel = new JPanel(new GridLayout(1, 1));
        mComponentsWriteLock.lock();
        componentsHash.clear();
        for (int i = 0; i < items.length; i++) {
            final Object item = items[i];
            final JRadioButton rb = new JRadioButton(item.toString());
            radioGroupHash.put(item.toString(), item);
            rb.setActionCommand(item.toString());
            group.add(rb);
            radioPanel.add(rb);

            String v;
            if (item instanceof Info) {
                v = ((Info) item).getStringValue();
            } else {
                v = item.toString();
            }
            componentsHash.put(v, rb);
            if (v.equals(selectedValue)) {
                rb.setSelected(true);
                radioGroupValue = selectedValue;
            }

            rb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    mComponentsReadLock.lock();
                    final Object item =
                                    radioGroupHash.get(e.getActionCommand());
                    mComponentsReadLock.unlock();
                    String v;
                    if (item instanceof Info) {
                        v = ((Info) item).getStringValue();
                    } else {
                        v = item.toString();
                    }
                    radioGroupValue = v;
                }
            });
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
        final boolean accessible =
                   Tools.getConfigData().isAccessible(getEnableAccessMode());
        mComponentsReadLock.lock();
        final JComponent c = componentsHash.get(s);
        mComponentsReadLock.unlock();
        if (c != null) {
            SwingUtilities.invokeLater(new Runnable() {
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
        final JComponent c = componentsHash.get(s);
        mComponentsReadLock.unlock();
        if (c != null) {
            c.setVisible(visible);
        }
    }

    /**
     * Returns string value. If object value is null, returns empty string (not
     * null).
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
    protected Object getValueInternal() {
        final Object value = radioGroupValue;
        if (NOTHING_SELECTED.equals(value)) {
            return null;
        }
        return value;
    }

    /** Sets component visible or invisible. */
    @Override
    protected void setComponentsVisible(final boolean visible) {
        final JComponent comp = getComponent();
        final JLabel label = getLabel();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (label != null) {
                    label.setVisible(visible);
                }
                comp.setVisible(visible);
                mComponentsReadLock.lock();
                for (final JComponent c : componentsHash.values()) {
                    c.setVisible(visible);
                }
                mComponentsReadLock.unlock();
                repaint();
            }
        });
    }

    /** Returns whether component is editable or not. */
    @Override
    boolean isEditable() {
        return false;
    }

    /** Sets item/value in the component and waits till it is set. */
    @Override
    protected void setValueAndWait0(final Object item) {
        if (item != null) {
            mComponentsReadLock.lock();
            final JRadioButton rb =
                            (JRadioButton) componentsHash.get(item);
            mComponentsReadLock.unlock();
            if (rb != null) {
                rb.setSelected(true);
            }
            if (item instanceof Info) {
                radioGroupValue = ((Info) item).getStringValue();
            } else {
                radioGroupValue = (String) item;
            }
        }
    }

    /** Returns document object of the component. */
    @Override
    public Document getDocument() {
        return null;
    }

    /** Adds item listener to the component. */
    @Override
    public void addListeners(final WidgetListener wl) {
        getWidgetListeners().add(wl);
        mComponentsReadLock.lock();
        final ItemListener il = getItemListener(wl);
        for (final JComponent c : componentsHash.values()) {
            ((JRadioButton) c).addItemListener(il);
        }
        mComponentsReadLock.unlock();
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor,
                                          final Color compColor) {
        getComponent().setBackground(backgroundColor);
        mComponentsReadLock.lock();
        for (final JComponent c : componentsHash.values()) {
            c.setBackground(backgroundColor);
        }
        mComponentsReadLock.unlock();
    }

    /** Sets background color. */
    @Override
    public void setBackgroundColor(final Color bg) {
        final JComponent comp = getComponent();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setBackground(bg);
                comp.setBackground(bg);
                mComponentsReadLock.lock();
                for (final JComponent c : componentsHash.values()) {
                    c.setBackground(bg);
                }
                mComponentsReadLock.unlock();
            }
        });
    }

    /** Returns item at the specified index. */
    @Override
    Object getItemAt(final int i) {
        return getComponent();
    }

    /** Cleanup whatever would cause a leak. */
    @Override
    public void cleanup() {
        getWidgetListeners().clear();
        mComponentsReadLock.lock();
        for (final JComponent c : componentsHash.values()) {
            for (final ItemListener il
                            : ((JRadioButton) c).getItemListeners()) {
                ((JRadioButton) c).removeItemListener(il);
            }
        }
        mComponentsReadLock.unlock();
    }

    /** Sets component enabled or disabled. */
    protected void setComponentsEnabled(final boolean enabled) {
        super.setComponentsEnabled(enabled);
        mComponentsReadLock.lock();
        for (final JComponent c : componentsHash.values()) {
            c.setEnabled(enabled);
        }
        mComponentsReadLock.unlock();
    }
}
