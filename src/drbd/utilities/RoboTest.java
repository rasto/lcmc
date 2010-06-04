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
    /** Previous position of the mouse. */
    private static Point2D prevP = null;
    /** Whether the test was aborted. */
    private static volatile boolean aborted = false;
    /** Slow down the animation. */
    private static final float slowFactor = 10f;
    /**
     * Private constructor, cannot be instantiated.
     */
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
                    startTest1(robot, host);
                } else if (index == 2) {
                    startTest2(robot, host);
                } else if (index == 3) {
                    startTest3(robot, host);
                }
                Tools.info("test " + index + " done");
            }
        });
        thread.start();
    }

    /** TEST 1. */
    private static void startTest1(final Robot robot, final Host host) {
        host.getSSH().installTestFiles(1);
        host.checkTest("test1", 1);
        /* create IPaddr2 with 192.168.100.100 ip */
        final int ipX = 235;
        final int ipY = 255;
        aborted = false;
        moveTo(robot, ipX, ipY);
        rightClick(robot); /* popup */
        moveTo(robot, ipX + 57, ipY + 28);
        moveTo(robot, ipX + 270, ipY + 28);
        moveTo(robot, ipX + 267, ipY + 67);
        leftClick(robot); /* choose ipaddr */
        moveTo(robot, 1072, 405);
        leftClick(robot); /* pull down */
        moveTo(robot, 1044, 442);
        leftClick(robot); /* choose */
        Tools.sleep(1000);
        press(robot, KeyEvent.VK_1);
        press(robot, KeyEvent.VK_0);
        press(robot, KeyEvent.VK_0);
        moveTo(robot, 814, 189);
        Tools.sleep(3000); /* ptest */
        leftClick(robot); /* apply */
        
        /* group with dummy resources */
        final int gx = 230;
        final int gy = 374;
        moveTo(robot, gx, gy);
        Tools.sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, gx + 46, gy + 11);
        Tools.sleep(1000);
        leftClick(robot); /* choose group */
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 2); /* 2 */
        rightClick(robot); /* group popup */
        moveTo(robot, gx + 80, gy + 20);
        moveTo(robot, gx + 84, gy + 22);
        moveTo(robot, gx + 560, gy + 22);
        Tools.sleep(1000);
        chooseDummy(robot);
        Tools.sleep(1000);
        moveTo(robot, 809, 192); /* ptest */
        Tools.sleep(2000);
        leftClick(robot); /*  apply */
        for (int i = 0; i < 5; i++) {
            /* another group resource */
            Tools.sleep(3000);
            moveTo(robot, gx + 46, gy + 11); 
            rightClick(robot); /* group popup */
            Tools.sleep(2000 + i * 500);
            moveTo(robot, gx + 80, gy + 20);
            moveTo(robot, gx + 84, gy + 22);
            moveTo(robot, gx + 560, gy + 22);
            Tools.sleep(1000);
            chooseDummy(robot);
            Tools.sleep(i * 300);
            moveTo(robot, 809, 192); /* ptest */
            Tools.sleep(2000);
            leftClick(robot); /* apply */
            Tools.sleep(1000);
        }
        Tools.sleep(4000 * slowFactor);
        host.checkTest("test1", 3); /* 3 */
        /* constraints */
        final int popX = 343;
        final int popY = 300;
        addConstraint(robot, gx, gy, 9);
        Tools.sleep(5000 * slowFactor);
        host.checkTest("test1", 4); /* 4 */

        removeConstraint(robot, popX, popY);
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 5); /* 5 */
        Tools.sleep(1000);

        addConstraint(robot, gx, gy, 9);
        Tools.sleep(5000 * slowFactor);
        host.checkTest("test1", 6); /* 6 */

        removeOrder(robot, popX, popY);
        Tools.sleep(4000 * slowFactor);
        host.checkTest("test1", 7);

        addOrder(robot, popX, popY);
        Tools.sleep(4000 * slowFactor);
        host.checkTest("test1", 8);

        removeColocation(robot, popX, popY);
        Tools.sleep(5000 * slowFactor);
        host.checkTest("test1", 9);

        addColocation(robot, popX, popY);
        Tools.sleep(4000 * slowFactor);
        host.checkTest("test1", 10);

        /** Add m/s Stateful resource */
        final int statefulX = 577;
        final int statefulY = 255;
        moveTo(robot, statefulX, statefulY);
        rightClick(robot); /* popup */
        moveTo(robot, 637, 263);
        moveTo(robot, 637, 283);
        moveTo(robot, 812, 282);
        moveTo(robot, 832, 340);
        moveTo(robot, 1065, 342);
        Tools.sleep(1000);

        press(robot, KeyEvent.VK_S);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_T);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_A);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_T);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_E);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_F);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_ENTER); /* choose Stateful */
        Tools.sleep(1000);

        moveTo(robot, 812, 179);
        Tools.sleep(1000);
        leftClick(robot); /* apply */
        Tools.sleep(4000 * slowFactor);
        host.checkTest("test1", 11);
        Tools.sleep(1000);
        /* set clone max to 1 */
        moveTo(robot, 978, 381);
        leftClick(robot); /* Clone Max */
        press(robot, KeyEvent.VK_1); // TODO: should be backspace, 1
        press(robot, KeyEvent.VK_LEFT); //TODO remove
        press(robot, KeyEvent.VK_BACK_SPACE); //TODO remove, once it's fixed
        moveTo(robot, 812, 179);
        Tools.sleep(1000);
        leftClick(robot); /* apply */
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 12);
        stopResource(robot, statefulX, statefulY, 0);
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 13);
        startResource(robot, statefulX, statefulY, 0);
        Tools.sleep(10000 * slowFactor);
        host.checkTest("test1", 14);
        unmanageResource(robot, statefulX, statefulY, 0);
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 15);
        manageResource(robot, statefulX, statefulY, 0);
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 16);

        /* IP addr cont. */
        stopResource(robot, ipX, ipY, 0);
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 17);
        startResource(robot, ipX, ipY, 0);
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 18);
        unmanageResource(robot, ipX, ipY, 0);
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 19);
        manageResource(robot, ipX, ipY, 0);
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 20);
        migrateResource(robot, ipX, ipY, 0);
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 21);
        unmigrateResource(robot, ipX, ipY, 0);
        Tools.sleep(3000 * slowFactor);
        host.checkTest("test1", 22);

        /* Group cont. */
        stopResource(robot, gx, gy, 15);
        Tools.sleep(10000 * slowFactor);
        host.checkTest("test1", 23);
        startResource(robot, gx, gy, 15);
        Tools.sleep(8000 * slowFactor);
        host.checkTest("test1", 24);
        unmanageResource(robot, gx, gy, 15);
        Tools.sleep(5000 * slowFactor);
        host.checkTest("test1", 25);
        manageResource(robot, gx, gy, 15);
        Tools.sleep(5000 * slowFactor);
        host.checkTest("test1", 26);
        migrateResource(robot, gx, gy, 25);
        Tools.sleep(5000 * slowFactor);
        host.checkTest("test1", 27);
        unmigrateResource(robot, gx, gy, 25);
        Tools.sleep(5000 * slowFactor);
        host.checkTest("test1", 28);

        aborted = false;
    }

    /** TEST 2. */
    private static void startTest2(final Robot robot, final Host host) {
        host.getSSH().installTestFiles(1);
        host.checkTest("test2", 2);
        /* create IPaddr2 with 192.168.100.100 ip */
        final int ipX = 235;
        final int ipY = 255;
        aborted = false;
        moveTo(robot, ipX, ipY);
        rightClick(robot); /* popup */
        aborted = false;
    }

    /** Choose dummy resource. */
    private static void chooseDummy(final Robot robot) {
        press(robot, KeyEvent.VK_D);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_U);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_M);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_M);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_Y);
        Tools.sleep(200);
        press(robot, KeyEvent.VK_ENTER); /* choose dummy */
    }

    /** Stops resource. */
    private static void stopResource(final Robot robot,
                                     final int x,
                                     final int y,
                                     final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        Tools.sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 130 + yFactor);
        Tools.sleep(3000); /*  ptest */
        leftClick(robot); /* stop */
    }

    /** Starts resource. */
    private static void startResource(final Robot robot,
                                     final int x,
                                     final int y,
                                     final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        Tools.sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 80 + yFactor);
        Tools.sleep(3000); /*  ptest */
        leftClick(robot); /* stop */
    }

    /** Migrate resource. */
    private static void migrateResource(final Robot robot,
                                        final int x,
                                        final int y,
                                        final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        Tools.sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 220 + yFactor);
        Tools.sleep(3000); /*  ptest */
        leftClick(robot); /* stop */
    }

    /** Unmigrate resource. */
    private static void unmigrateResource(final Robot robot,
                                          final int x,
                                          final int y,
                                          final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        Tools.sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 260 + yFactor);
        Tools.sleep(3000); /*  ptest */
        leftClick(robot); /* stop */
    }

    /** Unmanage resource. */
    private static void unmanageResource(final Robot robot,
                                         final int x,
                                         final int y,
                                         final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        Tools.sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 200 + yFactor);
        Tools.sleep(3000); /*  ptest */
        leftClick(robot); /* stop */
    }

    /** Manage resource. */
    private static void manageResource(final Robot robot,
                                         final int x,
                                         final int y,
                                         final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        Tools.sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 140, y + 200 + yFactor);
        Tools.sleep(3000); /*  ptest */
        leftClick(robot); /* stop */
    }


    /** Adds constraint from vertex. */
    private static void addConstraint(final Robot robot,
                                      final int x,
                                      final int y,
                                      final int with) {
        moveTo(robot, x + 50, y + 5);
        Tools.sleep(1000);
        rightClick(robot); /* popup */ 
        moveTo(robot, x + 82, y + 65);
        moveTo(robot, x + 335, y + 65 + with);
        Tools.sleep(3000); /*  ptest */
        leftClick(robot); /* start before */
    }
    /** Removes constraint. */
    private static void removeConstraint(final Robot robot,
                                         final int popX,
                                         final int popY) {
        moveTo(robot, popX, popY);
        Tools.sleep(1000);
        rightClick(robot); /* constraint popup */
        moveTo(robot, popX + 70, popY + 20);
        Tools.sleep(3000); /* ptest */
        leftClick(robot); /* remove ord */
    }

    /** Removes order. */
    private static void removeOrder(final Robot robot,
                                    final int popX,
                                    final int popY) {
        moveTo(robot, popX, popY);
        Tools.sleep(1000);
        rightClick(robot); /* constraint popup */
        moveTo(robot, popX + 70, popY + 50);
        Tools.sleep(3000); /* ptest */
        leftClick(robot); /* remove ord */
    }

    /** Adds order. */
    private static void addOrder(final Robot robot,
                                 final int popX,
                                 final int popY) {
        moveTo(robot, popX, popY);
        Tools.sleep(1000);
        rightClick(robot); /* constraint popup */
        moveTo(robot, popX + 70, popY + 50);
        Tools.sleep(3000); /* ptest */
        leftClick(robot); /* add ord */
    }

    /** Removes colocation. */
    private static void removeColocation(final Robot robot,
                                         final int popX,
                                         final int popY) {
        moveTo(robot, popX, popY);
        Tools.sleep(1000);
        rightClick(robot); /* constraint popup */
        moveTo(robot, popX + 70, popY + 100);
        Tools.sleep(3000); /* ptest */
        leftClick(robot); /* remove col */
    }

    /** Adds colocation. */
    private static void addColocation(final Robot robot,
                                      final int popX,
                                      final int popY) {
        moveTo(robot, popX, popY);
        Tools.sleep(1000);
        rightClick(robot); /* constraint popup */
        moveTo(robot, popX + 70, popY + 90);
        Tools.sleep(3000); /* ptest */
        leftClick(robot); /* add col */
    }

    /** TEST 2. */
    private static void startTest3(final Robot robot, final Host host) {
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
        Tools.sleep(2000);
        aborted = false;
    }

    /** Press button. */
    private static void press(final Robot robot, final int ke)  {
        if (aborted) {
            return;
        }
        robot.keyPress(ke);
        Tools.sleep(10);
        robot.keyRelease(ke);
        Tools.sleep(10);
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
    /** Right click. */
    private static void rightClick(final Robot robot)  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON3_MASK);
        Tools.sleep(500);
        robot.mouseRelease(InputEvent.BUTTON3_MASK);
        Tools.sleep(6000);
    }

    /** Move to position. */
    private static void moveTo(final Robot robot, int toX, int toY) {
        if (aborted) {
            return;
        }
        prevP = null;
        int xOffset = getOffset();
        final Point2D origP = MouseInfo.getPointerInfo().getLocation();
        final int origX = (int) origP.getX();
        final int origY = (int) origP.getY();
        final Point2D endP =
                       Tools.getGUIData().getMainFrame().getLocationOnScreen();
        final int endX = (int) endP.getX() + toX;
        final int endY = (int) endP.getY() + toY;
        int destX = endX;
        int destY = endY;
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
            Tools.sleep(5);
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
                    final Point2D pos = MouseInfo.getPointerInfo().getLocation();
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
