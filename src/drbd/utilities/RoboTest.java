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

import drbd.data.Host;
import java.awt.Robot;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.geom.Point2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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
    /** Don't move the mosue pointer smothly. */
    private static final boolean MOVE_MOUSE_FAST = false;
    /** Previous position of the mouse. */
    private static Point2D prevP = null;
    /** Whether the test was aborted. */
    private static volatile boolean aborted = false;
    /** Slow down the animation. */
    private static float slowFactor = 1f;
    /** Private constructor, cannot be instantiated. */
    private RoboTest() {
        /* Cannot be instantiated. */
    }

    /** Abort if mouse moved. */
    private static boolean abortWithMouseMovement() {
        if (MouseInfo.getPointerInfo() == null) {
            return false;
        }
        final Point2D p = MouseInfo.getPointerInfo().getLocation();
        if (prevP != null
            && (Math.abs(p.getX() - prevP.getX()) > 200
                || Math.abs(p.getY() - prevP.getY()) > 200)) {
            prevP = null;
            Tools.info("test aborted");
            aborted = true;
            return true;
        }
        prevP = p;
        return false;
    }

    /**
     * Restore mouse when the program was interrupted, while a mouse button
     * was pressed.
     */
    public static void restoreMouse() {
        Robot robot = null;
        try {
            robot = new Robot(SCREEN_DEVICE);
        } catch (final java.awt.AWTException e) {
            Tools.appWarning("Robot error");
        }
        if (robot == null) {
            return;
        }
        leftClick(robot);
        rightClick(robot);
    }

    /** Starts automatic left clicker in 10 seconds. */
    public static void startClicker(final int duration,
                                    final boolean lazy) {
        startClicker0(duration, lazy, InputEvent.BUTTON1_MASK,
                      10,   /* after click */
                      10,   /* after release */
                      500,  /* lazy after click */
                      100); /* lazy after release */
    }

    /** Starts automatic right clicker in 10 seconds. */
    public static void startRightClicker(final int duration,
                                         final boolean lazy) {
        startClicker0(duration, lazy, InputEvent.BUTTON3_MASK,
                      10,   /* after click */
                      500,   /* after release */
                      500,  /* lazy after click */
                      5000); /* lazy after release */
    }

    /** Starts automatic clicker in 10 seconds. */
    private static void startClicker0(final int duration,
                                      final boolean lazy,
                                      final int buttonMask,
                                      final int timeAfterClick,
                                      final int timeAfterRelase,
                                      final int timeAfterClickLazy,
                                      final int timeAfterRelaseLazy) {
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
                    robot.mousePress(buttonMask);
                    if (lazy) {
                        Tools.sleep(timeAfterClickLazy);
                    } else {
                        Tools.sleep(timeAfterClick);
                    }
                    robot.mouseRelease(buttonMask);
                    if (lazy) {
                        Tools.sleep(timeAfterRelaseLazy);
                    } else {
                        Tools.sleep(timeAfterRelase);
                    }
                    robot.keyPress(KeyEvent.VK_ESCAPE);
                    Tools.sleep(100);
                    robot.keyRelease(KeyEvent.VK_ESCAPE);
                    if (abortWithMouseMovement()) {
                        break;
                    }
                    final long current = System.currentTimeMillis();
                    if ((current - startTime) > duration * 60 * 1000) {
                        break;
                    }
                    Tools.sleep(500);
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
                final Point2D origP = MouseInfo.getPointerInfo().getLocation();
                final int origX = (int) origP.getX();
                final int origY = (int) origP.getY();
                Tools.info("move mouse to the end position");
                Tools.sleep(5000);
                final Point2D endP = MouseInfo.getPointerInfo().getLocation();
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

    /** Automatic tests. */
    public static void startTest(final int index, final Host host) {
        Tools.info("start test " + index + " in 3 seconds");
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                Tools.sleep(3000);
                Robot robot = null;
                try {
                    robot = new Robot(SCREEN_DEVICE);
                } catch (final java.awt.AWTException e) {
                    Tools.appWarning("Robot error");
                }
                if (robot == null) {
                    return;
                }
                if (index == 1) {
                    /* pacemaker */
                    int i = 1;
                    while (!aborted) {
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index + " no " + i);
                        startTest1(robot, host);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + " no " + i + ", secs: "
                                   + secs);
                        i++;
                    }
                } else if (index == 2) {
                    /* resource sets */
                    int i = 1;
                    while (!aborted) {
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index + " no " + i);
                        startTest2(robot, host);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + " no " + i + ", secs: "
                                   + secs);
                        i++;
                    }
                } else if (index == 3) {
                    /* pacemaker drbd */
                    final int i = 1;
                    final long startTime = System.currentTimeMillis();
                    Tools.info("test" + index + " no " + i);
                    startTest3(robot);
                    final int secs = (int) (System.currentTimeMillis()
                                             - startTime) / 1000;
                    Tools.info("test" + index + " no " + i + ", secs: "
                               + secs);
                } else if (index == 4) {
                    /* placeholders 6 dummies */
                    final int i = 1;
                    final long startTime = System.currentTimeMillis();
                    Tools.info("test" + index + " no " + i);
                    startTest4(robot, host);
                    final int secs = (int) (System.currentTimeMillis()
                                             - startTime) / 1000;
                    Tools.info("test" + index + " no " + i + ", secs: "
                               + secs);
                } else if (index == 5) {
                    int i = 1;
                    while (true) {
                        /* pacemaker */
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index + " no " + i);
                        startTest5(robot, host);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + " no " + i + ", secs: "
                                   + secs);
                        i++;
                    }
                } else if (index == 6) {
                    int i = 1;
                    while (true) {
                        /* pacemaker */
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index + " no " + i);
                        startTest6(robot, host);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + " no " + i + ", secs: "
                                   + secs);
                        i++;
                    }
                } else if (index == 9) {
                    /* all pacemaker tests */
                    int i = 1;
                    while (true) {
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index + " no " + i);
                        startTest1(robot, host);
                        startTest2(robot, host);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + " no " + i + ", secs: "
                                   + secs);
                        i++;
                    }
                }
                Tools.info("test " + index + " done");
            }
        });
        thread.start();
    }

    /** Check test. */
    private static void checkTest(final Host host,
                                  final String test,
                                  final double no) {
        if (!aborted) {
            host.checkTest(test, no);
        }
    }

    /** TEST 1. */
    private static void startTest1(final Robot robot, final Host host) {
        slowFactor = 0.2f;
        host.getSSH().installTestFiles(1);
        aborted = false;
        /* create IPaddr2 with 192.168.100.100 ip */
        final int ipX = 235;
        final int ipY = 255;
        final int gx = 230;
        final int gy = 374;
        final int popX = 343;
        final int popY = 300;
        final int statefulX = 500;
        final int statefulY = 255;
        disableStonith(robot, host);
        checkTest(host, "test1", 1);
        moveTo(robot, ipX, ipY);
        rightClick(robot); /* popup */
        moveTo(robot, ipX + 57, ipY + 28);
        moveTo(robot, ipX + 270, ipY + 28);
        moveTo(robot, ipX + 267, ipY + 52);
        leftClick(robot); /* choose ipaddr */
        removeResource(robot, ipX, ipY, -15);
        /* again */
        moveTo(robot, ipX, ipY);
        rightClick(robot); /* popup */
        moveTo(robot, ipX + 57, ipY + 28);
        moveTo(robot, ipX + 270, ipY + 28);
        moveTo(robot, ipX + 267, ipY + 52);
        leftClick(robot); /* choose ipaddr */

        moveTo(robot, 1072, 405);
        leftClick(robot); /* pull down */
        moveTo(robot, 1044, 442);
        leftClick(robot); /* choose */
        sleep(1000);
        press(robot, KeyEvent.VK_1);
        press(robot, KeyEvent.VK_0);
        press(robot, KeyEvent.VK_0);
        sleep(1000);
        setTimeouts(robot);
        moveTo(robot, 814, 189);
        sleep(6000); /* ptest */
        leftClick(robot); /* apply */

        /* group with dummy resources */
        moveTo(robot, gx, gy);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, gx + 46, gy + 11);
        sleep(1000);
        leftClick(robot); /* choose group */
        sleep(3000);
        /* remove it */
        removeResource(robot, gx, gy, 0);

        moveTo(robot, gx, gy);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, gx + 46, gy + 11);
        sleep(1000);
        leftClick(robot); /* choose group */
        sleep(3000);

        rightClick(robot); /* group popup */
        moveTo(robot, gx + 80, gy + 20);
        moveTo(robot, gx + 84, gy + 22);
        moveTo(robot, gx + 560, gy + 22);
        sleep(1000);
        typeDummy(robot);
        sleep(1000);

        /* remove it */
        removeResource(robot, gx, gy, 0);

        /* group with dummy resources, once again */
        moveTo(robot, gx, gy);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, gx + 46, gy + 11);
        sleep(1000);
        leftClick(robot); /* choose group */
        sleep(3000);
        rightClick(robot); /* group popup */
        moveTo(robot, gx + 80, gy + 20);
        moveTo(robot, gx + 84, gy + 22);
        moveTo(robot, gx + 560, gy + 22);
        sleep(1000);
        typeDummy(robot);
        sleep(1000);

        moveTo(robot, 125, 320);
        sleep(1000);
        rightClick(robot);
        sleep(1000);
        moveTo(robot, 150, 552);
        leftClick(robot); /* remove service */
        removeResource(robot, gx, gy, 0);

        /* group with dummy resources, once again */
        moveTo(robot, gx, gy);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, gx + 46, gy + 11);
        sleep(1000);
        leftClick(robot); /* choose group */
        sleep(3000);
        rightClick(robot); /* group popup */
        moveTo(robot, gx + 80, gy + 20);
        moveTo(robot, gx + 84, gy + 22);
        moveTo(robot, gx + 560, gy + 22);
        sleep(1000);
        typeDummy(robot);
        sleep(1000);

        removeResource(robot, gx, gy, 0);


        /* once again */
        moveTo(robot, gx, gy);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, gx + 46, gy + 11);
        sleep(1000);
        leftClick(robot); /* choose group */
        sleep(3000);
        checkTest(host, "test1", 2); /* 2 */
        rightClick(robot); /* group popup */
        moveTo(robot, gx + 80, gy + 20);
        moveTo(robot, gx + 84, gy + 22);
        moveTo(robot, gx + 560, gy + 22);
        sleep(1000);
        typeDummy(robot);
        sleep(1000);
        
        setTimeouts(robot);
        moveTo(robot, 809, 192); /* ptest */
        sleep(2000);
        leftClick(robot); /*  apply */
        for (int i = 0; i < 2; i++) {
            /* another group resource */
            sleep(3000);
            moveTo(robot, gx + 46, gy + 11);
            rightClick(robot); /* group popup */
            sleep(2000 + i * 500);
            moveTo(robot, gx + 80, gy + 20);
            moveTo(robot, gx + 84, gy + 22);
            moveTo(robot, gx + 560, gy + 22);
            sleep(1000);
            typeDummy(robot);
            sleep(i * 300);
            setTimeouts(robot);
            moveTo(robot, 809, 192); /* ptest */
            sleep(6000);
            leftClick(robot); /* apply */
            sleep(1000);
        }
        sleep(4000);
        checkTest(host, "test1", 3); /* 3 */
        /* constraints */
        addConstraint(robot, gx, gy, 0, true, -1);
        checkTest(host, "test1", 3.1); /* 3.1 */

        /* same as */
        moveTo(robot, 125, 345);
        sleep(1000);
        leftClick(robot);
        sleep(1000);
        moveTo(robot, 1078 , 612);
        leftClick(robot);
        sleep(1000);
        moveTo(robot, 1078 , 670);
        leftClick(robot); /* choose another dummy */
        sleep(1000);
        moveTo(robot, 809, 192); /* ptest */
        sleep(4000);
        leftClick(robot); /* apply */
        sleep(4000);
        checkTest(host, "test1", 3.2); /* 3.2 */

        moveTo(robot, 1078 , 612);
        leftClick(robot);
        sleep(1000);
        moveTo(robot, 1078 , 642);
        leftClick(robot); /* choose "nothing selected */
        sleep(1000);
        moveTo(robot, 809, 192); /* ptest */
        sleep(4000);
        leftClick(robot); /* apply */
        sleep(5000);
        checkTest(host, "test1", 4); /* 4 */

        /* locations */
        moveTo(robot, ipX + 20, ipY);
        leftClick(robot); /* choose ip */
        setLocation(robot, new Integer[]{KeyEvent.VK_I});
        sleep(3000);
        checkTest(host, "test1", 4.1); /* 4.1 */

        setLocation(robot, new Integer[]{KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_MINUS,
                                         KeyEvent.VK_I});
        
        sleep(3000);
        checkTest(host, "test1", 4.2); /* 4.2 */

        setLocation(robot, new Integer[]{KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_PLUS});
        sleep(3000);
        checkTest(host, "test1", 4.3); /* 4.3 */

        setLocation(robot, new Integer[]{KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE,
                                         KeyEvent.VK_BACK_SPACE});
        sleep(3000);
        checkTest(host, "test1", 4.4); /* 4.4 */

        removeConstraint(robot, popX, popY);
        sleep(3000);
        checkTest(host, "test1", 5); /* 5 */
        sleep(1000);

        addConstraint(robot, gx, gy, 9, true, -1);
        sleep(5000);
        checkTest(host, "test1", 6); /* 6 */

        removeOrder(robot, popX, popY);
        sleep(4000);
        checkTest(host, "test1", 7);

        addOrder(robot, popX, popY);
        sleep(4000);
        checkTest(host, "test1", 8);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 9);

        addColocation(robot, popX, popY);
        sleep(4000);
        checkTest(host, "test1", 10);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.1);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.2);

        addConstraintOrderOnly(robot, gx, gy, 0, 25, 0, true, -1);
        sleep(4000);
        checkTest(host, "test1", 10.3);

        addColocation(robot, popX, popY);
        sleep(4000);
        checkTest(host, "test1", 10.4);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.5);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.6);

        addConstraintColocationOnly(robot, gx, gy, 0, 25, 0, true, -1);
        sleep(4000);
        checkTest(host, "test1", 10.7);

        addOrder(robot, popX, popY);
        sleep(4000);
        checkTest(host, "test1", 10.8);

        removeConstraint(robot, popX, popY);
        sleep(4000);
        checkTest(host, "test1", 10.9);

        addConstraint(robot, ipX, ipY, 0, false, -1);
        sleep(5000);
        checkTest(host, "test1", 10.91);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.92);

        addOrder(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.93);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.94);

        addColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.95);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.96);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.97);

        addConstraintColocationOnly(robot, ipX, ipY, 0, 100, 0, false, -1);
        sleep(5000);
        checkTest(host, "test1", 10.98);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 10.99);

        addConstraintOrderOnly(robot, ipX, ipY, 0, 100, 0, false, -1);
        sleep(5000);
        checkTest(host, "test1", 11);

        addColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.1);

        removeConstraint(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.2);

        addConstraint(robot, ipX, ipY, 60, false, -1);
        sleep(5000);
        checkTest(host, "test1", 11.3);
        stopResource(robot, ipX, ipY, 0);
        sleep(5000);
        checkTest(host, "test1", 11.4);
        resetStartStopResource(robot, ipX, ipY);
        sleep(5000);
        checkTest(host, "test1", 11.5);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.51);

        addColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.52);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.53);

        addOrder(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.54);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.55);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.56);

        addConstraintOrderOnly(robot, ipX, ipY, 0, 100, 55, false, -1);
        sleep(5000);
        checkTest(host, "test1", 11.57);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.58);

        addConstraintColocationOnly(robot, ipX, ipY, 0, 100, 55, false, -1);
        sleep(5000);
        checkTest(host, "test1", 11.59);

        addOrder(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.6);

        removeConstraint(robot, popX, popY);
        sleep(5000);
        checkTest(host, "test1", 11.7);

        addConstraint(robot, gx, gy, 9, true, -1);
        sleep(5000);
        checkTest(host, "test1", 11.8);

        /** Add m/s Stateful resource */
        moveTo(robot, statefulX, statefulY);
        rightClick(robot); /* popup */
        moveTo(robot, 637, 263);
        moveTo(robot, 637, 283);
        moveTo(robot, 912, 282);
        moveTo(robot, 932, 325);
        moveTo(robot, 1165, 325);
        sleep(1000);

        press(robot, KeyEvent.VK_S);
        sleep(200);
        press(robot, KeyEvent.VK_T);
        sleep(200);
        press(robot, KeyEvent.VK_A);
        sleep(200);
        press(robot, KeyEvent.VK_T);
        sleep(200);
        press(robot, KeyEvent.VK_E);
        sleep(200);
        press(robot, KeyEvent.VK_F);
        sleep(200);
        press(robot, KeyEvent.VK_ENTER); /* choose Stateful */
        sleep(1000);

        moveTo(robot, 812, 179);
        sleep(1000);
        leftClick(robot); /* apply */
        sleep(4000);
        /* set clone max to 1 */
        moveTo(robot, 978, 381);
        leftClick(robot); /* Clone Max */
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(200);
        press(robot, KeyEvent.VK_1);
        setTimeouts(robot);
        moveTo(robot, 812, 179);
        sleep(3000);
        leftClick(robot); /* apply */
        sleep(3000);
        checkTest(host, "test1", 12);
        stopResource(robot, statefulX, statefulY, 0);
        sleep(3000);
        checkTest(host, "test1", 13);
        startResource(robot, statefulX, statefulY, 0);
        sleep(10000);
        checkTest(host, "test1", 14);
        unmanageResource(robot, statefulX, statefulY, 0);
        sleep(3000);
        checkTest(host, "test1", 15);
        manageResource(robot, statefulX, statefulY, 0);
        sleep(3000);
        checkTest(host, "test1", 16);

        /* IP addr cont. */
        stopResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(host, "test1", 17);
        startResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(host, "test1", 18);
        unmanageResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(host, "test1", 19);
        manageResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(host, "test1", 20);
        migrateResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(host, "test1", 21);
        unmigrateResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(host, "test1", 22);

        /* Group cont. */
        stopResource(robot, gx, gy, 15);
        sleep(10000);
        checkTest(host, "test1", 23);
        startResource(robot, gx, gy, 15);
        sleep(8000);
        checkTest(host, "test1", 24);
        unmanageResource(robot, gx, gy, 15);
        sleep(5000);
        checkTest(host, "test1", 25);
        manageResource(robot, gx, gy, 15);
        sleep(5000);
        checkTest(host, "test1", 26);
        migrateResource(robot, gx, gy, 25);
        sleep(5000);
        checkTest(host, "test1", 27);
        unmigrateResource(robot, gx, gy, 25);
        sleep(5000);
        checkTest(host, "test1", 28);
        stopResource(robot, ipX,ipY, 0);
        sleep(5000);
        stopGroup(robot, gx, gy, 15);
        sleep(5000);
        stopGroup(robot, statefulX, statefulY, 0);
        sleep(5000);
        checkTest(host, "test1", 29);
        if (maybe()) {
            removeEverything(robot);
        } else {
            removeResource(robot, ipX, ipY, -15);
            removeGroup(robot, gx, gy, 0);
            removeGroup(robot, statefulX, statefulY, -15);
        }
        if (!aborted) {
            Tools.sleep(10000);
        }
        checkTest(host, "test1", 1);
    }


    /** Remove everything. */
    private static void removeEverything(final Robot robot) {
        moveTo(robot, 335, 129); /* advanced */
        leftClick(robot);
        sleep(2000);
        moveTo(robot, 271, 568);
        rightClick(robot); /* popup */
        sleep(3000);
        moveTo(robot, 332, 644);
        sleep(3000);
        leftClick(robot);
        confirmRemove(robot);
        sleep(3000);
        leftClick(robot);
        moveTo(robot, 335, 129); /* not advanced */
        leftClick(robot);
    }

    /** Disable stonith if it is enabled. */
    private static void disableStonith(final Robot robot, final Host host) {
        moveTo(robot, 271, 250);
        leftClick(robot); /* global options */
        final String stonith = host.getCluster().getBrowser()
                    .getClusterStatus().getGlobalParam("stonith-enabled");
        if (stonith == null || "true".equals(stonith)) {
            moveTo(robot, 944, 298);
            leftClick(robot); /* disable stonith */
        }
        moveTo(robot, 1073, 337);
        leftClick(robot); /* no quorum policy */
        moveTo(robot, 1058, 350);
        leftClick(robot); /* ignore */
        moveTo(robot, 828, 183);
        sleep(2000);
        leftClick(robot); /* apply */
    }

    /** TEST 2. */
    private static void startTest2(final Robot robot, final Host host) {
        slowFactor = 0.3f;
        host.getSSH().installTestFiles(2);
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;
        final int dummy2X = 545;
        final int dummy2Y = 255;
        final int dummy3X = 235;
        final int dummy3Y = 500;
        final int dummy4X = 545;
        final int dummy4Y = 390;
        final int phX = 445;
        final int phY = 390;

        disableStonith(robot, host);
        checkTest(host, "test2", 1);
        /* create 4 dummies */
        chooseDummy(robot, dummy1X, dummy1Y);
        chooseDummy(robot, dummy2X, dummy2Y);
        chooseDummy(robot, dummy3X, dummy3Y);
        chooseDummy(robot, dummy4X, dummy4Y);
        checkTest(host, "test2", 2);

        /* placeholder */
        moveTo(robot, phX, phY);
        rightClick(robot);
        sleep(2000);
        moveTo(robot, phX + 30 , phY + 45);
        sleep(2000);
        leftClick(robot);
        checkTest(host, "test2", 3);
        /* constraints */
        addConstraint(robot, phX, phY, 0, false, -1); /* with dummy 1 */
        sleep(2000);
        moveTo(robot, 809, 192); /* ptest */
        sleep(2000);
        leftClick(robot); /*  apply */
        sleep(5000);
        checkTest(host, "test2", 4);

        final int dum1PopX = dummy1X + 130;
        final int dum1PopY = dummy1Y + 50;
        for (int i = 0; i < 1; i++) {
            removeOrder(robot, dum1PopX, dum1PopY);
            sleep(4000);

            checkTest(host, "test2", 5);

            addOrder(robot, dum1PopX, dum1PopY);
            sleep(4000);
            checkTest(host, "test2", 6);

            removeColocation(robot, dum1PopX, dum1PopY);
            sleep(5000);
            checkTest(host, "test2", 7);

            addColocation(robot, dum1PopX, dum1PopY);
            sleep(4000);
            checkTest(host, "test2", 8);
        }

        addConstraint(robot, dummy3X, dummy3Y, 80, false, -1); /* with ph */
        sleep(5000);
        checkTest(host, "test2", 9);

        final int dum3PopX = dummy3X + 130;
        final int dum3PopY = dummy3Y - 50;
        for (int i = 0; i < 2; i++) {
            removeColocation(robot, dum3PopX, dum3PopY);
            sleep(4000);

            checkTest(host, "test2", 9.1);

            addColocation(robot, dum3PopX, dum3PopY);
            sleep(4000);
            checkTest(host, "test2", 9.2);

            removeOrder(robot, dum3PopX, dum3PopY);
            sleep(5000);
            checkTest(host, "test2", 9.3);

            addOrder(robot, dum3PopX, dum3PopY);
            sleep(4000);
            checkTest(host, "test2", 9.4);
        }

        addConstraint(robot, phX, phY, 0, false, -1); /* with dummy 2 */
        sleep(5000);
        checkTest(host, "test2", 10);
        addConstraint(robot, dummy4X, dummy4Y, 80, false, -1); /* with ph */
        sleep(5000);
        checkTest(host, "test2", 11);

        /* ph -> dummy2 */
        final int dum2PopX = dummy2X - 10;
        final int dum2PopY = dummy2Y + 70;
        removeConstraint(robot, dum2PopX, dum2PopY);
        sleep(4000);
        checkTest(host, "test2", 11.1);
        addConstraintOrderOnly(robot,
                               phX,
                               phY,
                               -50,
                               20,
                               0,
                               false,
                               -1); /* with dummy 2 */
        sleep(4000);
        checkTest(host, "test2", 11.2);
        removeOrder(robot, dum2PopX, dum2PopY);
        sleep(4000);
        checkTest(host, "test2", 11.3);
        addConstraintColocationOnly(robot,
                                    phX,
                                    phY,
                                    -50,
                                    23,
                                    0,
                                    false,
                                    -1); /* with dummy 2 */
        sleep(4000);
        checkTest(host, "test2", 11.4);
        addOrder(robot, dum2PopX, dum2PopY);
        sleep(4000);
        checkTest(host, "test2", 11.5);

        /* dummy4 -> ph */
        final int dum4PopX = dummy4X - 40;
        final int dum4PopY = dummy4Y - 10;
        removeConstraint(robot, dum4PopX, dum4PopY);
        sleep(4000);
        checkTest(host, "test2", 11.6);
        moveTo(robot, dummy4X + 20, dummy4Y + 5);
        sleep(1000);
        rightClick(robot); /* workaround for the next popup not working. */
        sleep(1000);
        rightClick(robot); /* workaround for the next popup not working. */
        sleep(1000);
        addConstraintColocationOnly(robot,
                                    dummy4X,
                                    dummy4Y,
                                    -50,
                                    93,
                                    80,
                                    false,
                                    -1); /* with ph */
        sleep(4000);
        checkTest(host, "test2", 11.7);
        removeColocation(robot, dum4PopX, dum4PopY);
        sleep(4000);
        checkTest(host, "test2", 11.8);
        moveTo(robot, dummy4X + 20, dummy4Y + 5);
        sleep(1000);
        rightClick(robot); /* workaround for the next popup not working. */
        sleep(1000);
        addConstraintOrderOnly(robot,
                               dummy4X,
                               dummy4Y,
                               -50,
                               90,
                               80,
                               false,
                               -1); /* ph 2 */
        sleep(4000);
        checkTest(host, "test2", 11.9);
        addColocation(robot, dum4PopX, dum4PopY);
        sleep(4000);
        checkTest(host, "test2", 11.91);
        /* remove one dummy */
        stopResource(robot, dummy1X, dummy1Y, 0);
        sleep(5000);
        checkTest(host, "test2", 11.92);
        removeResource(robot, dummy1X, dummy1Y, -15);
        sleep(5000);
        checkTest(host, "test2", 12);
        stopResource(robot, dummy2X, dummy2Y, 0);
        sleep(10000);
        stopResource(robot, dummy3X, dummy3Y, 0);
        sleep(10000);
        stopResource(robot, dummy3X, dummy3Y, 0);
        sleep(10000);
        stopResource(robot, dummy4X, dummy4Y, 0);
        sleep(10000);
        checkTest(host, "test2", 12.5);
        if (maybe()) {
            /* remove placeholder */
            moveTo(robot, phX , phY);
            rightClick(robot);
            sleep(1000);
            moveTo(robot, phX + 40 , phY + 80);
            leftClick(robot);
            confirmRemove(robot);
            sleep(5000);

            /* remove rest of the dummies */
            removeResource(robot, dummy2X, dummy2Y, -15);
            sleep(5000);
            checkTest(host, "test2", 14);
            removeResource(robot, dummy3X, dummy3Y, -15);
            sleep(5000);
            checkTest(host, "test2", 15);
            removeResource(robot, dummy4X, dummy4Y, -15);
            sleep(5000);
        } else {
            removeEverything(robot);
        }
        checkTest(host, "test2", 16);
    }

    /** TEST 4. */
    private static void startTest4(final Robot robot, final Host host) {
        slowFactor = 0.5f;
        host.getSSH().installTestFiles(2);
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;

        final int dummy2X = 545;
        final int dummy2Y = 255;

        final int dummy3X = 235;
        final int dummy3Y = 394;

        final int dummy4X = 545;
        final int dummy4Y = 394;

        final int dummy5X = 235;
        final int dummy5Y = 553;

        final int dummy6X = 545;
        final int dummy6Y = 553;

        final int ph1X = 445;
        final int ph1Y = 314;

        final int ph2X = 445;
        final int ph2Y = 473;

        disableStonith(robot, host);
        checkTest(host, "test4", 1);
        /* create 6 dummies */
        chooseDummy(robot, dummy1X, dummy1Y);
        chooseDummy(robot, dummy2X, dummy2Y);
        chooseDummy(robot, dummy3X, dummy3Y);
        chooseDummy(robot, dummy4X, dummy4Y);
        chooseDummy(robot, dummy5X, dummy5Y);
        chooseDummy(robot, dummy6X, dummy6Y);

        /* 2 placeholders */
        while (true) {
        moveTo(robot, ph1X, ph1Y);
        rightClick(robot);
        sleep(2000);
        moveTo(robot, ph1X + 30 , ph1Y + 45);
        sleep(2000);
        leftClick(robot);

        moveTo(robot, ph2X, ph2Y);
        rightClick(robot);
        sleep(2000);
        moveTo(robot, ph2X + 30 , ph2Y + 45);
        sleep(2000);
        leftClick(robot);
        checkTest(host, "test4", 2);

        /* constraints */
        addConstraint(robot, dummy5X, dummy5Y, 160, false, -1); /* with ph2 */
        addConstraint(robot, dummy6X, dummy6Y, 160, false, -1); /* with ph2 */

        addConstraint(robot, ph2X, ph2Y, 60, false, -1); /* with dummy 3 */
        addConstraint(robot, ph2X, ph2Y, 60, false, -1); /* with dummy 4 */

        addConstraint(robot, dummy3X, dummy3Y, 80, false, -1); /* with ph1 */
        addConstraint(robot, dummy4X, dummy4Y, 80, false, -1); /* with ph1 */

        addConstraint(robot, ph1X, ph1Y, 5, false, -1); /* with dummy 1 */
        addConstraint(robot, ph1X, ph1Y, 5, false, -1); /* with dummy 2 */
        checkTest(host, "test4", 3);

        /* TEST test */
        removePlaceHolder(robot, ph1X, ph1Y);
        removePlaceHolder(robot, ph2X, ph2Y);
        }
    }

    /** TEST 5. */
    private static void startTest5(final Robot robot, final Host host) {
        slowFactor = 0.2f;
        host.getSSH().installTestFiles(2);
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;

        final int dummy2X = 235;
        final int dummy2Y = 500;

        final int ph1X = 315;
        final int ph1Y = 394;


        //disableStonith(robot, host);
        /* create 2 dummies */
        checkTest(host, "test5", 1);

        /* placeholders */
        moveTo(robot, ph1X, ph1Y);
        rightClick(robot);
        sleep(2000);
        moveTo(robot, ph1X + 30 , ph1Y + 45);
        sleep(2000);
        leftClick(robot);

        chooseDummy(robot, dummy1X, dummy1Y);
        chooseDummy(robot, dummy2X, dummy2Y);
        //checkTest(host, "test5", 2);

        addConstraint(robot, dummy2X, dummy2Y, 35, false, -1);
        addConstraint(robot, ph1X, ph1Y, 5, false, -1);

        moveTo(robot, ph1X, ph1Y);
        sleep(2000);
        leftClick(robot);
        sleep(2000);
        moveTo(robot, 809, 192); /* ptest */
        sleep(2000);
        leftClick(robot); /*  apply */
        checkTest(host, "test5", 3);

        leftClick(robot); /*  apply */
        removeEverything(robot);
        sleep(5000);
        checkTest(host, "test5", 4);
        //int dum1PopX = dummy1X + 70;
        //int dum1PopY = dummy1Y + 60;
        ///* constraints */
        //for (int i = 1; i <=10; i++) {
        //    addConstraint(robot, dummy1X, dummy1Y, 5, false, -1);

        //    moveTo(robot, ph1X , ph1Y);
        //    sleep(2000);
        //    leftClick(robot);
        //    sleep(2000);
        //    moveTo(robot, 809, 192); /* ptest */
        //    sleep(2000);
        //    leftClick(robot); /*  apply */

        //    checkTest(host, "test5", 2);

        //    removeConstraint(robot, dum1PopX, dum1PopY);
        //    checkTest(host, "test5", 3);

        //    addConstraint(robot, ph1X, ph1Y, 5, false, -1);

        //    moveTo(robot, ph1X, ph1Y);
        //    sleep(2000);
        //    leftClick(robot);
        //    sleep(2000);
        //    moveTo(robot, 809, 192); /* ptest */
        //    sleep(2000);
        //    leftClick(robot); /*  apply */

        //    checkTest(host, "test5", 4);

        //    removeConstraint(robot, dum1PopX, dum1PopY);
        //    checkTest(host, "test5", 3);
        //    Tools.info("i: " + i);
        //}
    }

    /** TEST 6. */
    private static void startTest6(final Robot robot, final Host host) {
        slowFactor = 0.1f;
        host.getSSH().installTestFiles(2);
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;

        final int ph1X = 315;
        final int ph1Y = 394;


        //disableStonith(robot, host);
        /* create 2 dummies */
        //checkTest(host, "test5", 1);

        /* placeholders */
        moveTo(robot, ph1X, ph1Y);
        rightClick(robot);
        sleep(2000);
        moveTo(robot, ph1X + 30 , ph1Y + 45);
        sleep(2000);
        leftClick(robot);

        chooseDummy(robot, dummy1X, dummy1Y);
        //checkTest(host, "test5", 2);
        final int dum1PopX = dummy1X + 70;
        final int dum1PopY = dummy1Y + 60;
        while (true) {
            addConstraint(robot, ph1X, ph1Y, 5, false, -1);
            if (!aborted) {
                Tools.sleep(2000);
            }
            removeConstraint(robot, dum1PopX, dum1PopY);
        }

    }

    /** Sets location. */
    private static void setLocation(final Robot robot, final Integer[] events) {
        moveTo(robot, 1041 , 615);
        leftClick(robot);
        sleep(1000);
        for (final int ev : events) {
            if (ev == KeyEvent.VK_PLUS) {
                robot.keyPress(KeyEvent.VK_SHIFT);
                sleep(200);
            }
            press(robot, ev);
            sleep(200);
            if (ev == KeyEvent.VK_PLUS) {
                robot.keyRelease(KeyEvent.VK_SHIFT);
                sleep(200);
            }
        }
        moveTo(robot, 809, 192); /* ptest */
        sleep(4000);
        leftClick(robot); /* apply */
        sleep(2000);
    }

    /** Choose dummy resource. */
    private static void typeDummy(final Robot robot) {
        press(robot, KeyEvent.VK_D);
        sleep(200);
        press(robot, KeyEvent.VK_U);
        sleep(200);
        press(robot, KeyEvent.VK_M);
        sleep(200);
        press(robot, KeyEvent.VK_M);
        sleep(200);
        press(robot, KeyEvent.VK_Y);
        sleep(200);
        press(robot, KeyEvent.VK_ENTER); /* choose dummy */
    }

    /** Sets start timeout. */
    private static void setTimeouts(final Robot robot) {
        sleep(3000);
        moveTo(robot, 1105, 298);
        leftPress(robot); /* scroll bar */
        moveTo(robot, 1105, 550);
        leftRelease(robot);
        moveTo(robot, 956, 520);
        leftClick(robot); /* start timeout */
        press(robot, KeyEvent.VK_2);
        sleep(200);
        press(robot, KeyEvent.VK_0);
        sleep(200);
        press(robot, KeyEvent.VK_0);
        sleep(200);

        moveTo(robot, 956, 550);
        leftClick(robot); /* stop timeout */
        press(robot, KeyEvent.VK_1);
        sleep(200);
        press(robot, KeyEvent.VK_9);
        sleep(200);
        press(robot, KeyEvent.VK_2);
        sleep(200);

        moveTo(robot, 956, 580);
        leftClick(robot); /* monitor timeout */
        press(robot, KeyEvent.VK_1);
        sleep(200);
        press(robot, KeyEvent.VK_5);
        sleep(200);
        press(robot, KeyEvent.VK_4);
        sleep(200);

        moveTo(robot, 956, 610);
        leftClick(robot); /* monitor interval */
        press(robot, KeyEvent.VK_1);
        sleep(200);
        press(robot, KeyEvent.VK_2);
        sleep(200);
        press(robot, KeyEvent.VK_1);
        sleep(200);
        moveTo(robot, 1105, 350);

        leftPress(robot); /* scroll bar back */
        moveTo(robot, 1105, 150);
        leftRelease(robot);
    }

    /** Sleep for x milliseconds * slowFactor + some random time. */
    private static void sleep(final int x) {
        if (!aborted) {
            Tools.sleep((int) (x * slowFactor));
            Tools.sleep((int) (x * slowFactor * Math.random()));
        }
    }

    /** Returns maybe true. */
    private static boolean maybe() {
        if (Math.random() < 0.5) {
            return true;
        }
        return false;
    }

    /** Create dummy resource. */
    private static void chooseDummy(final Robot robot,
                                    final int x,
                                    final int y) {
        moveTo(robot, x, y);
        sleep(1000);
        rightClick(robot); /* popup */
        sleep(1000);
        moveTo(robot, x + 57, y + 28);
        moveTo(robot, x + 290, y + 28);
        moveTo(robot, x + 290, y + 72);
        moveTo(robot, x + 560, y + 72);
        sleep(2000);
        typeDummy(robot);
        sleep(2000);
        setTimeouts(robot);
        moveTo(robot, 809, 192); /* ptest */
        sleep(4000);
        leftClick(robot); /* apply */
        sleep(2000);
    }

    /** Removes service. */
    private static void removeResource(final Robot robot,
                                       final int x,
                                       final int y,
                                       final int corr) {
        moveTo(robot, x + 20, y);
        rightClick(robot);
        sleep(1000);
        moveTo(robot, x + 40 , y + 250 + corr);
        leftClick(robot);
        confirmRemove(robot);
    }

    /** Removes group. */
    private static void removeGroup(final Robot robot,
                                    final int x,
                                    final int y,
                                    final int corr) {
        moveTo(robot, x + 20, y);
        rightClick(robot);
        sleep(60000);
        moveTo(robot, x + 40 , y + 250 + corr);
        leftClick(robot);
        confirmRemove(robot);
    }

    /** Removes placeholder. */
    private static void removePlaceHolder(final Robot robot,
                                          final int x,
                                          final int y) {
        moveTo(robot, x + 20, y);
        rightClick(robot);
        sleep(1000);
        moveTo(robot, x + 40 , y + 60);
        leftClick(robot);
        confirmRemove(robot);
    }

    /** Confirms remove dialog. */
    private static void confirmRemove(final Robot robot) {
        sleep(1000);
        moveTo(robot, 512 , 480);
        leftClick(robot);
    }

    /** Stops resource. */
    private static void stopResource(final Robot robot,
                                     final int x,
                                     final int y,
                                     final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 130 + yFactor);
        sleep(6000); /* ptest */
        leftClick(robot); /* stop */
    }

    /** Stops group. */
    private static void stopGroup(final Robot robot,
                                  final int x,
                                  final int y,
                                  final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 130 + yFactor);
        sleep(120000); /* ptest */
        leftClick(robot); /* stop */
    }

    /** Removes target role. */
    private static void resetStartStopResource(final Robot robot,
                                               final int x,
                                               final int y) {
        moveTo(robot, x + 50, y + 5);
        sleep(1000);
        leftClick(robot); /* select */

        moveTo(robot, 1072, 496);
        leftClick(robot); /* pull down */
        moveTo(robot, 1044, 520);
        leftClick(robot); /* choose */
        moveTo(robot, 814, 189);
        sleep(6000); /* ptest */
        leftClick(robot); /* apply */
    }

    /** Starts resource. */
    private static void startResource(final Robot robot,
                                     final int x,
                                     final int y,
                                     final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 80 + yFactor);
        sleep(6000); /* ptest */
        leftClick(robot); /* stop */
    }

    /** Migrate resource. */
    private static void migrateResource(final Robot robot,
                                        final int x,
                                        final int y,
                                        final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 220 + yFactor);
        sleep(6000); /* ptest */
        leftClick(robot); /* stop */
    }

    /** Unmigrate resource. */
    private static void unmigrateResource(final Robot robot,
                                          final int x,
                                          final int y,
                                          final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 260 + yFactor);
        sleep(6000); /* ptest */
        leftClick(robot); /* stop */
    }

    /** Unmanage resource. */
    private static void unmanageResource(final Robot robot,
                                         final int x,
                                         final int y,
                                         final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 200 + yFactor);
        sleep(6000); /* ptest */
        leftClick(robot); /* stop */
    }

    /** Manage resource. */
    private static void manageResource(final Robot robot,
                                         final int x,
                                         final int y,
                                         final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 200 + yFactor);
        sleep(6000); /* ptest */
        leftClick(robot); /* stop */
    }

    /** Go to the group service menu. */
    private static void groupServiceMenu(final Robot robot,
                                         final int x,
                                         final int y,
                                         final int groupService) {
        final int groupServicePos = 365 + groupService;
        final int startBefore = 100 + groupService;
        moveTo(robot, x + 82, y + groupServicePos);
        moveTo(robot, x + 382, y + groupServicePos);
    }

    /** Adds constraint from vertex. */
    private static void addConstraint(final Robot robot,
                                      final int x,
                                      final int y,
                                      final int with,
                                      final boolean group,
                                      final int groupService) {
        int groupcor = 0;
        if (group) {
            groupcor = 24;
        }
        moveTo(robot, x + 20, y + 5);
        sleep(1000);
        if (group) {
            rightClickGroup(robot); /* popup */
        } else {
            rightClick(robot); /* popup */
        }
        if (groupService >= 0) {
            groupServiceMenu(robot, x, y, groupService);
            final int startBefore = 100 + groupService;
            moveTo(robot, x + 382, y + startBefore);
            moveTo(robot, x + 632, y + startBefore);
            moveTo(robot, x + 632, y + startBefore + with);
        } else {
            moveTo(robot, x + 82, y + 50 + groupcor);
            moveTo(robot, x + 335, y + 50 + groupcor);
            moveTo(robot, x + 335, y + 50 + groupcor + with);
        }
        sleep(6000); /* ptest */
        leftClick(robot); /* start before */
    }

    /** Adds constraint (order only) from vertex. */
    private static void addConstraintOrderOnly(final Robot robot,
                                               final int x,
                                               final int y,
                                               final int xCorrection,
                                               final int skipY,
                                               final int with,
                                               final boolean group,
                                               final int groupService) {
        int groupcor = 0;
        if (group) {
            groupcor = 24;
        }
        moveTo(robot, x + 20, y + 5);
        sleep(1000);
        if (group) {
            rightClickGroup(robot); /* popup */
        } else {
            rightClick(robot); /* popup */
        }
        moveTo(robot, x + 82, y + 50 + groupcor);
        moveTo(robot, x + 335, y + 50 + groupcor);
        moveTo(robot, x + 335, y + 50 + groupcor + skipY + 34);
        moveTo(robot, x + 500 + xCorrection, y + 50 + groupcor + skipY + 34);
        moveTo(robot,
               x + 500 + xCorrection,
               y + 50 + groupcor + skipY + 34 + with);
        sleep(6000); /* ptest */
        leftClick(robot); /* start before */
    }

    /** Adds constraint (colocation only) from vertex. */
    private static void addConstraintColocationOnly(final Robot robot,
                                                    final int x,
                                                    final int y,
                                                    final int xCorrection,
                                                    final int skipY,
                                                    final int with,
                                                    final boolean group,
                                                    final int groupService) {
        int groupcor = 0;
        if (group) {
            groupcor = 24;
        }
        moveTo(robot, x + 20, y + 5);
        sleep(1000);
        if (group) {
            rightClickGroup(robot); /* popup */
        } else {
            rightClick(robot); /* popup */
        }
        moveTo(robot, x + 82, y + 50 + groupcor);
        moveTo(robot, x + 335, y + 50 + groupcor);
        moveTo(robot, x + 335, y + 50 + groupcor + skipY + 9);
        moveTo(robot, x + 500 + xCorrection, y + 50 + groupcor + skipY + 9);
        moveTo(robot,
               x + 500 + xCorrection,
               y + 50 + groupcor + skipY + 9 + with);
        sleep(6000); /* ptest */
        leftClick(robot); /* start before */
    }

    /** Removes constraint. */
    private static void removeConstraint(final Robot robot,
                                         final int popX,
                                         final int popY) {
        moveTo(robot, popX, popY);
        sleep(1000);
        rightClick(robot); /* constraint popup */
        moveTo(robot, popX + 70, popY + 20);
        sleep(6000); /* ptest */
        leftClick(robot); /* remove ord */
    }

    /** Removes order. */
    private static void removeOrder(final Robot robot,
                                    final int popX,
                                    final int popY) {
        moveTo(robot, popX, popY);
        sleep(1000);
        rightClick(robot); /* constraint popup */
        moveTo(robot, popX + 70, popY + 50);
        sleep(6000); /* ptest */
        leftClick(robot); /* remove ord */
    }

    /** Adds order. */
    private static void addOrder(final Robot robot,
                                 final int popX,
                                 final int popY) {
        moveTo(robot, popX, popY);
        sleep(1000);
        rightClick(robot); /* constraint popup */
        moveTo(robot, popX + 70, popY + 50);
        sleep(6000); /* ptest */
        leftClick(robot); /* add ord */
    }

    /** Removes colocation. */
    private static void removeColocation(final Robot robot,
                                         final int popX,
                                         final int popY) {
        moveTo(robot, popX, popY);
        sleep(1000);
        rightClick(robot); /* constraint popup */
        moveTo(robot, popX + 70, popY + 93);
        sleep(6000); /* ptest */
        leftClick(robot); /* remove col */
    }

    /** Adds colocation. */
    private static void addColocation(final Robot robot,
                                      final int popX,
                                      final int popY) {
        moveTo(robot, popX, popY);
        sleep(1000);
        rightClick(robot); /* constraint popup */
        moveTo(robot, popX + 70, popY + 90);
        sleep(6000); /* ptest */
        leftClick(robot); /* add col */
    }

    /** TEST 3. */
    private static void startTest3(final Robot robot) {
        aborted = false;
        /* filesystem/drbd */
        moveTo(robot, 577, 253);
        rightClick(robot); /* popup */
        moveTo(robot, 609, 278);
        moveTo(robot, 794, 283);
        leftClick(robot); /* choose fs */
        moveTo(robot, 1075, 406);
        leftClick(robot); /* choose drbd */
        moveTo(robot, 1043, 444);
        leftClick(robot); /* choose drbd */
        moveTo(robot, 1068, 439);
        leftClick(robot); /* mount point */
        moveTo(robot, 1039, 475);
        leftClick(robot); /* mount point */
        moveTo(robot, 815, 186);
        leftClick(robot); /* apply */
        sleep(2000);
        aborted = false;
    }

    /** Press button. */
    private static void press(final Robot robot, final int ke)  {
        if (aborted) {
            return;
        }
        robot.keyPress(ke);
        //Tools.sleep(10);
        robot.keyRelease(ke);
        Tools.sleep(200);
    }

    /** Left click. */
    private static void leftClick(final Robot robot)  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        Tools.sleep(300);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    /** Left press. */
    private static void leftPress(final Robot robot)  {
        robot.mousePress(InputEvent.BUTTON1_MASK);
        Tools.sleep(300);
    }

    /** Left release. */
    private static void leftRelease(final Robot robot)  {
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Tools.sleep(300);
    }

    /** Right click. */
    private static void rightClick(final Robot robot)  {
        if (aborted) {
            return;
        }
        Tools.sleep(1000);
        robot.mousePress(InputEvent.BUTTON3_MASK);
        Tools.sleep(500);
        robot.mouseRelease(InputEvent.BUTTON3_MASK);
        sleep(6000);
    }

    /** Right click. */
    private static void rightClickGroup(final Robot robot)  {
        if (aborted) {
            return;
        }
        rightClick(robot);
        Tools.sleep(10000);
    }

    /** Move to position. */
    private static void moveTo(final Robot robot,
                               final int toX,
                               final int toY) {
        if (aborted) {
            return;
        }
        prevP = null;
        final int xOffset = getOffset();
        final Point2D origP = MouseInfo.getPointerInfo().getLocation();
        final int origX = (int) origP.getX();
        final int origY = (int) origP.getY();
        final Point2D endP =
                       Tools.getGUIData().getMainFrame().getLocationOnScreen();
        final int endX = (int) endP.getX() + toX;
        final int endY = (int) endP.getY() + toY;
        if (MOVE_MOUSE_FAST) {
            robot.mouseMove(endX, endY);
            return;
        }
        final int destX = endX;
        final int destY = endY;
        while (true) {
            if (MouseInfo.getPointerInfo() == null) {
                return;
            }
            final Point2D p = MouseInfo.getPointerInfo().getLocation();

            final int x = (int) p.getX();
            final int y = (int) p.getY();
            int directionX = 0;
            int directionY = 0;
            if (x < destX) {
                directionX = 1;
            } else if (x > destX) {
                directionX = -1;
            }
            if (y < destY) {
                directionY = 1;
            } else if (y > destY) {
                directionY = -1;
            }
            if (directionY == 0 && directionX == 0) {
                break;
            }
            robot.mouseMove((int) p.getX() + xOffset + directionX,
                            (int) p.getY() + directionY);
            sleep(5);
            if (abortWithMouseMovement()) {
                break;
            }
        }
    }

    /** Register movement. */
    public static void registerMovement() {
        Tools.info("start register movement in 3 seconds");
        Tools.sleep(3000);
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                Point2D prevP = new Point2D.Double(0, 0);
                Point2D prevPrevP = new Point2D.Double(0, 0);
                while (true) {
                    final Point2D loc =
                       Tools.getGUIData().getMainFrame().getLocationOnScreen();
                    final Point2D pos =
                                      MouseInfo.getPointerInfo().getLocation();
                    final Point2D newPos = new Point2D.Double(
                                                      pos.getX() - loc.getX(),
                                                      pos.getY() - loc.getY());
                    Tools.sleep(200);
                    if (newPos.equals(prevP) && !prevPrevP.equals(prevP)) {
                        Tools.info("moveTo(robot, "
                                    + (int) newPos.getX()
                                    + ", "
                                    + (int) newPos.getY() + ");");
                    }
                    prevPrevP = prevP;
                    prevP = newPos;
                    if (abortWithMouseMovement()) {
                        break;
                    }
                }
                Tools.info("stopped movement registering");
            }
        });
        thread.start();
    }
}
