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

import java.awt.Robot;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.geom.Point2D;
import java.awt.event.InputEvent;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class RoboTest {
    /** Screen device. */
    private static final GraphicsDevice SCREEN_DEVICE =
     GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    /** Previous position of the mouse. */
    private static Point2D prevP = null;
    /**
     * Private constructor, cannot be instantiated.
     */
    private RoboTest() {
        /* Cannot be instantiated. */
    }

    /** Abort if mouse moved. */
    private static boolean abortWithMouseMovement() {
        final Point2D p = MouseInfo.getPointerInfo().getLocation();
        if (prevP != null
            && (Math.abs(p.getX() - prevP.getX()) > 50
                || Math.abs(p.getY() - prevP.getY()) > 50)) {
            prevP = null;
            Tools.info("test aborted");
            return true;
        }
        prevP = p;
        return false;
    }

    /** Starts automatic clicker in 10 seconds. */
    public static void startClicker(final int duration,
                                    final boolean lazy) {
        Tools.info("start click test in 10 seconds");
        prevP = null;
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                Tools.sleep(5000);
                Robot robot = null;
                try {
                    robot = new Robot(SCREEN_DEVICE);
                } catch (final java.awt.AWTException e) {
                    Tools.appWarning("Robot error");
                }
                if (robot != null) {
                    for (int i = 0; i < duration; i++) {
                        robot.mousePress(InputEvent.BUTTON1_MASK);
                        if (lazy) {
                            Tools.sleep(500);

                        }
                        robot.mouseRelease(InputEvent.BUTTON1_MASK);
                        if (abortWithMouseMovement()) {
                            break;
                        }
                        if (lazy) {
                            Tools.sleep(500);

                        }
                    }
                    Tools.info("click test done");
                }
            }
        });
        thread.start();
    }
                //final Point2D p = MouseInfo.getPointerInfo().getLocation();
                //final GraphicsDevice[] devices =
                //    GraphicsEnvironment.getLocalGraphicsEnvironment()
                //                       .getScreenDevices();

                //int xOffset = 0;
                //if (devices.length >= 2) {
                //    /* workaround for dual monitors that are flipped. */
                //    //TODO: not sure how is it with three monitors
                //    final int x1 =
                //        devices[0].getDefaultConfiguration().getBounds().x;
                //    final int x2 =
                //        devices[1].getDefaultConfiguration().getBounds().x;
                //    if (x1 > x2) {
                //        xOffset = -x1;
                //    }
                //}

                //final Point2D p = MouseInfo.getPointerInfo().getLocation();
                //robot.mouseMove((int) p.getX() + xOffset - 1, (int) p.getY());
}
