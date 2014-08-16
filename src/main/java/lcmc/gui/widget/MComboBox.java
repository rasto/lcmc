/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package lcmc.gui.widget;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.Container;
import java.awt.Dimension;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

public final class MComboBox<E> extends JComboBox<E> {
    private boolean layingOut = false;
    
    public MComboBox(final E[] items) {
        super(items);
    }

    public MComboBox(final Vector<E> items) {
        super(items);
    }

    public MComboBox(final ComboBoxModel<E> aModel) {
        super(aModel);
    }

    @Override
    public void doLayout() {
        try {
            layingOut = true;
            super.doLayout();
        } finally {
            layingOut = false;
        }
    }

    /** Get new size if popup items are wider than the item. */
    @Override
    public Dimension getSize() {
        final Dimension dim = super.getSize();
        if (!layingOut) {
            final Object c = getUI().getAccessibleChild(this, 0);
            if (c instanceof JPopupMenu) {
                final JScrollPane scrollPane = (JScrollPane) ((Container) c).getComponent(0);
                final JComponent view = (JComponent) scrollPane.getViewport().getView();
                final int newSize = view.getPreferredSize().width + 2;
                dim.width = Math.max(dim.width, newSize);
            }
        }
        return dim;
    }
}
