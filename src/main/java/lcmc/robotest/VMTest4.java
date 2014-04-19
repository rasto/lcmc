/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
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

package lcmc.robotest;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import lcmc.gui.widget.GenericWidget.MTextField;
import static lcmc.robotest.RoboTest.*;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class VMTest4 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(VMTest4.class);

    /** Cluster wizard locked until focus is lost. */
    static void start(final String vmTest, final int count) {
        slowFactor = 0.1f;
        aborted = false;
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                info(vmTest + " I: " + i);
            }
            moveTo("Add New Virtual Machine"); /* new VM */
            leftClick();
            sleep(1000);
            moveTo("Domain name", MTextField.class);
            final Point2D p = getAppPosition();
            if (!isColor((int) p.getX(),
                         (int) p.getY(),
                         new Color(255, 100, 100),
                         true)) {
                info(vmTest + " 1: error");
                break;
            }
            boolean ok = false;
            for (int error = 0; error < 5; error++) {
                press(KeyEvent.VK_X);
                if (!isColor((int) p.getX(),
                             (int) p.getY(),
                             new Color(255, 100, 100),
                             false)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                info(vmTest + " 2: failed");
                break;
            }
            moveTo("Cancel"); /* cancel */
            leftClick();
        }
    }

    /** Private constructor, cannot be instantiated. */
    private VMTest4() {
        /* Cannot be instantiated. */
    }
}
