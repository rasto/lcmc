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

import javax.inject.Named;
import javax.inject.Singleton;

import lcmc.cluster.ui.widget.GenericWidget.MTextField;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class VMTest4 {
    private final RoboTest roboTest;

    public VMTest4(RoboTest roboTest) {
        this.roboTest = roboTest;
    }

    /**
     * Cluster wizard locked until focus is lost.
     */
    void start(final String vmTest, final int count) {
        roboTest.setSlowFactor(0.1f);
        roboTest.setAborted(false);
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                roboTest.info(vmTest + " I: " + i);
            }
            roboTest.moveTo("Add New Virtual Machine"); /* new VM */
            roboTest.leftClick();
            roboTest.sleep(1000);
            roboTest.moveTo("Domain name", MTextField.class);
            final Point2D p = roboTest.getAppPosition();
            if (!roboTest.isColor((int) p.getX(), (int) p.getY(), new Color(255, 100, 100), true)) {
                roboTest.info(vmTest + " 1: error");
                break;
            }
            boolean ok = false;
            for (int error = 0; error < 5; error++) {
                roboTest.press(KeyEvent.VK_X);
                if (!roboTest.isColor((int) p.getX(), (int) p.getY(), new Color(255, 100, 100), false)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                roboTest.info(vmTest + " 2: failed");
                break;
            }
            roboTest.moveTo("Cancel"); /* cancel */
            roboTest.leftClick();
        }
    }
}
