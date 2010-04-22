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
                Tools.sleep(10000);
                Robot robot = null;
                try {
                    robot = new Robot(SCREEN_DEVICE);
                } catch (final java.awt.AWTException e) {
                    Tools.appWarning("Robot error");
                }
                if (robot == null) {
                    return;
                }
                final long startTime = System.currentTimeMillis();
                while (true) {
                    robot.mousePress(InputEvent.BUTTON1_MASK);
                    if (lazy) {
                        Tools.sleep(500);

                    }
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                    if (abortWithMouseMovement()) {
                        break;
                    }
                    final long current = System.currentTimeMillis();
                    if ((current - startTime) > duration * 60 * 1000) {
                        break;
                    }
                    if (lazy) {
                        Tools.sleep(100);
                    } else {
                        Tools.sleep(10);
                    }
                }
                Tools.info("click test done");
            }
        });
        thread.start();
    }

    /** Starts automatic mouse mover in 10 seconds. */
    public static void startMover(final int duration,
                                  final boolean lazy) {
        Tools.info("start mouse move test in 10 seconds");
        prevP = null;
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                Tools.sleep(10000);
                Robot robot = null;
                try {
                    robot = new Robot(SCREEN_DEVICE);
                } catch (final java.awt.AWTException e) {
                    Tools.appWarning("Robot error");
                }
                if (robot == null) {
                    return;
                }
                int xOffset = getOffset();
                final Point2D origP =
                            MouseInfo.getPointerInfo().getLocation();
                final int origX = (int) origP.getX();
                final int origY = (int) origP.getY();
                Tools.info("move mouse to the end position");
                Tools.sleep(5000);
                final Point2D endP =
                            MouseInfo.getPointerInfo().getLocation();
                final int endX = (int) endP.getX();
                final int endY = (int) endP.getY();
                int destX = origX;
                int destY = origY;
                Tools.info("test started");
                final long startTime = System.currentTimeMillis();
                while (true) {
                    final Point2D p =
                                MouseInfo.getPointerInfo().getLocation();

                    final int x = (int) p.getX();
                    final int y = (int) p.getY();
                    int directionX;
                    int directionY;
                    if (x < destX) {
                        directionX = 1;
                    } else if (x > destX) {
                        directionX = -1;
                    } else {
                        if (destX == endX) {
                            destX = origX;
                            directionX = -1;
                        } else {
                            destX = endX;
                            directionX = 1;
                        }
                    }
                    if (y < destY) {
                        directionY = 1;
                    } else if (y > destY) {
                        directionY = -1;
                    } else {
                        if (destY == endY) {
                            destY = origY;
                            directionY = -1;
                        } else {
                            destY = endY;
                            directionY = 1;
                        }
                    }
                    robot.mouseMove((int) p.getX() + xOffset + directionX,
                                    (int) p.getY() + directionY);
                    if (lazy) {
                        Tools.sleep(40);
                    } else {
                        Tools.sleep(5);
                    }
                    if (abortWithMouseMovement()) {
                        break;
                    }
                    final long current = System.currentTimeMillis();
                    if ((current - startTime) > duration * 60 * 1000) {
                        break;
                    }
                }
                Tools.info("mouse move test done");
            }
        });
        thread.start();
    }

    /** workaround for dual monitors that are flipped. */
    private static int getOffset() {
        final Point2D p = MouseInfo.getPointerInfo().getLocation();
        final GraphicsDevice[] devices =
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                               .getScreenDevices();

        int xOffset = 0;
        if (devices.length >= 2) {
            final int x1 =
                devices[0].getDefaultConfiguration().getBounds().x;
            final int x2 =
                devices[1].getDefaultConfiguration().getBounds().x;
            if (x1 > x2) {
                xOffset = -x1;
            }
        }
        return xOffset;
    }
}
