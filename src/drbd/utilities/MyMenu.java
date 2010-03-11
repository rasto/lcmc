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

package drbd.utilities;

import drbd.data.ConfigData;
import javax.swing.JMenu;
import java.awt.geom.Point2D;

/**
 * This is a menu object that holds MyMenuItems.
 */
public class MyMenu extends JMenu implements UpdatableItem {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Position of the menu that can be stored and retrieved. */
    private Point2D pos = null;
    /** Access Type for this component to become enabled */
    final ConfigData.AccessType enableAccessType;
    /** Access Type for this component to become visible */
    final ConfigData.AccessType visibleAccessType;

    /**
     * Prepares a new <code>MyMenu</code> object.
     */
    public MyMenu(final String text,
                  final ConfigData.AccessType enableAccessType,
                  final ConfigData.AccessType visibleAccessType) {
        super(text);
        this.enableAccessType = enableAccessType;
        this.visibleAccessType = visibleAccessType;
        processAccessType();
    }

    /**
     * Stores the position.
     */
    public final void setPos(final Point2D pos) {
        this.pos = pos;
    }

    /**
     * Gets the position.
     */
    protected final Point2D getPos() {
        return pos;
    }

    /**
     * Predicate that can be used, but it is not.
     */
    public boolean predicate() {
        return true;
    }

    /**
     * Whether the menu item should be enabled or not.
     */
    public boolean enablePredicate() {
        return true;
    }

    /**
     * Returns whether the item should be visible or not.
     */
    public boolean visiblePredicate() {
        return true;
    }

    /**
     * This function is usually overriden and is called when the menu and its
     * items are to be updated.
     */
    public void update() {
        processAccessType();
    }

    /** Sets this item enabled and visible according to its access type. */
    private void processAccessType() {
        final boolean accessible = 
                   Tools.getConfigData().isAccessible(enableAccessType);
        setEnabled(enablePredicate() && accessible);
        setVisible(visiblePredicate()
                   && Tools.getConfigData().isAccessible(visibleAccessType));
        if (isVisible() && !accessible) {
            setToolTipText("<html><b>"
                           + getText()
                           + " (disabled)</b><br>available in \""
                           + ConfigData.OP_NODES_MAP.get(enableAccessType)
                           + "\" mode</html>");
        }
    }
}
