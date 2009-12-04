/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * by Rasto Levrinc.
 *
 * Copyright (C) 2009, Rastislav Levrinc
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

import java.awt.Color;
import java.awt.geom.Point2D;
import javax.swing.JList;
import javax.swing.JToolTip;
import javax.swing.ListModel;

import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

/**
 * A Jlist with updatable tooltips.
 */
public class MyList extends JList {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Tools tip object. */
    private JToolTip toolTip;
    /** Robot to move a mouse a little if a tooltip has changed. */
    private Robot robot = null;
    /** Screen device. */
    private static final GraphicsDevice SCREEN_DEVICE =
     GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

    /**
     * Prepares a new <code>MyList</code> object.
     */
    public MyList(final ListModel dataModel, final Color bg) {
        super(dataModel);
        toolTip = createToolTip();
        try {
            robot = new Robot(SCREEN_DEVICE);
        } catch (java.awt.AWTException e) {
            Tools.appWarning("Robot error");
        }
        setBackground(bg);
    }

    /**
     * Creates tooltip.
     */
    public final JToolTip createToolTip() {
        toolTip = super.createToolTip();
        toolTip.setBackground(Color.YELLOW);
        return toolTip;
    }

    /**
     * Sets tooltip and wiggles the mouse to refresh it.
     */
    public final void setToolTipText(final String toolTipText) {
        super.setToolTipText(toolTipText);
        if (toolTip != null && toolTip.isShowing() && robot != null) {
            final GraphicsDevice[] devices =
                            GraphicsEnvironment.getLocalGraphicsEnvironment()
                                               .getScreenDevices();
            int xOffset = 0;
            if (devices.length >= 2) {
                /* workaround for dual monitors that are flipped. */
                //TODO: not sure how is it with three monitors
                final int x1 =
                    devices[0].getDefaultConfiguration().getBounds().x;
                final int x2 =
                    devices[1].getDefaultConfiguration().getBounds().x;
                if (x1 > x2) {
                    xOffset = -x1;
                }
            }
            final Point2D p = MouseInfo.getPointerInfo().getLocation();
            robot.mouseMove((int) p.getX() + xOffset - 1, (int) p.getY());
            robot.mouseMove((int) p.getX() + xOffset + 1, (int) p.getY());
            robot.mouseMove((int) p.getX() + xOffset, (int) p.getY());
        }
    }
}
