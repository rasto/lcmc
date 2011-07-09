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
import drbd.data.Cluster;
import java.awt.Robot;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.geom.Point2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

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
    private static volatile Point2D prevP = null;
    /** Whether the test was aborted. */
    private static volatile boolean aborted = false;
    /** Slow down the animation. */
    private static float slowFactor = 1f;
    /** Robot. */
    private static Robot robot;
    /** Cluster. */
    private static Cluster cluster;
    /** Private constructor, cannot be instantiated. */
    private RoboTest() {
        /* Cannot be instantiated. */
    }

    /** Abort if mouse moved. */
    private static boolean abortWithMouseMovement() {
        if (MouseInfo.getPointerInfo() == null) {
            return false;
        }
        final Point2D loc =
                       Tools.getGUIData().getMainFrame().getLocationOnScreen();
        Point2D p = MouseInfo.getPointerInfo().getLocation();
        double x = p.getX() - loc.getX();
        if (x > 1536 || x < -100) {
            int i = 0;
            while (x > 1536 || x < -100) {
                if (i % 10 == 0) {
                    info("sleep: " + x);
                }
                Tools.sleep(500);
                if (MouseInfo.getPointerInfo() != null) {
                    p = MouseInfo.getPointerInfo().getLocation();
                    x = p.getX() - loc.getX();
                }
                i++;
            }
            prevP = p;
            return false;
        }
        if (prevP != null
            && prevP.getX() - p.getX() > 200
            && p.getY() - prevP.getY() > 200) {
            prevP = null;
            info("test aborted");
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
        try {
            robot = new Robot(SCREEN_DEVICE);
        } catch (final java.awt.AWTException e) {
            robot = null;
            Tools.appWarning("Robot error");
        }
        if (robot == null) {
            return;
        }
        leftClick();
        rightClick();
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
        info("start click test in 10 seconds");
        prevP = null;
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                sleepNoFactor(10000);
                Robot rbt = null;
                try {
                    rbt = new Robot(SCREEN_DEVICE);
                } catch (final java.awt.AWTException e) {
                    Tools.appWarning("Robot error");
                }
                if (rbt == null) {
                    return;
                }
                robot = rbt;
                final long startTime = System.currentTimeMillis();
                while (true) {
                    robot.mousePress(buttonMask);
                    if (lazy) {
                        sleepNoFactor(timeAfterClickLazy);
                    } else {
                        sleepNoFactor(timeAfterClick);
                    }
                    robot.mouseRelease(buttonMask);
                    if (lazy) {
                        sleepNoFactor(timeAfterRelaseLazy);
                    } else {
                        sleepNoFactor(timeAfterRelase);
                    }
                    robot.keyPress(KeyEvent.VK_ESCAPE);
                    sleepNoFactor(100);
                    robot.keyRelease(KeyEvent.VK_ESCAPE);
                    if (abortWithMouseMovement()) {
                        break;
                    }
                    final long current = System.currentTimeMillis();
                    if ((current - startTime) > duration * 60 * 1000) {
                        break;
                    }
                    sleepNoFactor(500);
                }
                info("click test done");
            }
        });
        thread.start();
    }

    /** Starts automatic mouse mover in 10 seconds. */
    public static void startMover(final int duration,
                                  final boolean withClicks) {
        aborted = false;
        slowFactor = 0.3f;
        info("start mouse move test in 10 seconds");
        prevP = null;
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                sleepNoFactor(10000);
                Robot rbt = null;
                try {
                    rbt = new Robot(SCREEN_DEVICE);
                } catch (final java.awt.AWTException e) {
                    Tools.appWarning("Robot error");
                }
                if (rbt == null) {
                    return;
                }

                final Point2D locP =
                       Tools.getGUIData().getMainFrame().getLocationOnScreen();
                robot = rbt;
                final int xOffset = getOffset();
                final Point2D origP = MouseInfo.getPointerInfo().getLocation();
                final int origX = (int) (origP.getX() - locP.getX());
                final int origY = (int) (origP.getY() - locP.getY());
                info("move mouse to the end position");
                sleepNoFactor(5000);
                final Point2D endP = MouseInfo.getPointerInfo().getLocation();
                final int endX = (int) (endP.getX() - locP.getX());
                final int endY = (int) (endP.getY() - locP.getY());
                int destX = origX;
                int destY = origY;
                info("test started");
                final long startTime = System.currentTimeMillis();
                int i = 1;
                while (!aborted) {
                    moveTo(destX, destY);
                    if (abortWithMouseMovement()) {
                        break;
                    }
                    if (withClicks) {
                        leftClick();
                        sleepNoFactor(1000);
                    }
                    moveTo(endX, endY);
                    if (abortWithMouseMovement()) {
                        break;
                    }
                    if (withClicks) {
                        leftClick();
                        sleepNoFactor(1000);
                    }
                    System.out.println("mouse move test: " + i);
                    i++;
                }
                info("mouse move test done");
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
    public static void startTest(final String index, final Cluster c) {
        cluster = c;
        aborted = false;
        info("start test " + index + " in 3 seconds");
        if (cluster != null) {
            for (final Host host : cluster.getHosts()) {
                host.getSSH().installTestFiles();
            }
        }
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                sleepNoFactor(3000);
                robot = null;
                try {
                    robot = new Robot(SCREEN_DEVICE);
                } catch (final java.awt.AWTException e) {
                    robot = null;
                    Tools.appWarning("Robot error");
                }
                if (robot == null) {
                    return;
                }
                String selected = null;
                if (cluster != null) {
                    selected =
                         cluster.getBrowser().getTree()
                                   .getLastSelectedPathComponent().toString();
                }
                if (selected == null) {
                    if ("1".equals(index)) {
                        /* cluster wizard deadlock */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startGUITest1(100);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if ("2".equals(index)) {
                        /* cluster wizard deadlock */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startGUITest2(100);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    }
                } else if ("Services".equals(selected)
                    || Tools.getString("ClusterBrowser.ClusterManager").equals(
                                                                   selected)) {
                    if ("0".equals(index)) {
                        /* all pacemaker tests */
                        int i = 1;
                        while (true) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startTest1();
                            if (aborted) {
                                break;
                            }
                            startTest2();
                            if (aborted) {
                                break;
                            }
                            startTest3();
                            if (aborted) {
                                break;
                            }
                            startTest4();
                            if (aborted) {
                                break;
                            }
                            startTest5();
                            if (aborted) {
                                break;
                            }
                            startTest6();
                            if (aborted) {
                                break;
                            }
                            startTest7();
                            if (aborted) {
                                break;
                            }
                            startTest8();
                            if (aborted) {
                                break;
                            }
                            //startTest9();
                            if (aborted) {
                                break;
                            }
                            startTestA();
                            if (aborted) {
                                break;
                            }
                            startTestB();
                            if (aborted) {
                                break;
                            }
                            startTestC();
                            if (aborted) {
                                break;
                            }
                            startTestD();
                            if (aborted) {
                                break;
                            }
                            startTestE();
                            if (aborted) {
                                break;
                            }
                            startTestF();
                            if (aborted) {
                                break;
                            }
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            i++;
                        }
                    } else if ("1".equals(index)) {
                        /* pacemaker */
                        int i = 1;
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startTest1();
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                        }
                    } else if ("2".equals(index)) {
                        /* resource sets */
                        int i = 1;
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startTest2();
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                        }
                    } else if ("3".equals(index)) {
                        /* pacemaker drbd */
                        final int i = 1;
                        final long startTime = System.currentTimeMillis();
                        info("test" + index + " no " + i);
                        startTest3();
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + " no " + i + ", secs: " + secs);
                    } else if ("4".equals(index)) {
                        /* placeholders 6 dummies */
                        int i = 1;
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startTest4();
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            i++;
                        }
                    } else if ("5".equals(index)) {
                        int i = 1;
                        while (!aborted) {
                            /* pacemaker */
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startTest5();
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            i++;
                        }
                    } else if ("6".equals(index)) {
                        int i = 1;
                        while (!aborted) {
                            /* pacemaker */
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startTest6();
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            i++;
                        }
                    } else if ("7".equals(index)) {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTest7();
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if ("8".equals(index)) {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTest8();
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if ("a".equals(index)) {
                        /* pacemaker leak test group */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestA();
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if ("b".equals(index)) {
                        /* pacemaker leak test clone */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestB();
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if ("c".equals(index)) {
                        /* pacemaker master/slave test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestC();
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if ("d".equals(index)) {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestD();
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if ("e".equals(index)) {
                        /* host wizard deadlock */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestE();
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if ("f".equals(index)) {
                        int i = 1;
                        while (!aborted) {
                            /* cloned group */
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startTestF();
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            i++;
                        }
                    }
                } else if ("Storage (DRBD)".equals(selected)) {
                    if ("0".equals(index)) {
                        /* all DRBD tests */
                        int i = 1;
                        final int blockDevY = getBlockDevY();
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startDRBDTest1(blockDevY);
                            if (aborted) {
                                break;
                            }
                            startDRBDTest2(blockDevY);
                            if (aborted) {
                                break;
                            }
                            startDRBDTest3(blockDevY);
                            if (aborted) {
                                break;
                            }
                            if (cluster.getHostsArray()[0].hasVolumes()) {
                                startDRBDTest4(blockDevY);
                                if (aborted) {
                                    break;
                                }
                            }
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                            if (cluster.getHostsArray()[0].hasVolumes()) {
                                Tools.getConfigData().setBigDRBDConf(
                                      !Tools.getConfigData().getBigDRBDConf());
                            }
                        }
                    } else if ("1".equals(index)) {
                        /* DRBD 1 link */
                        int i = 1;
                        final int blockDevY = getBlockDevY();
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startDRBDTest1(blockDevY);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                            if (cluster.getHostsArray()[0].hasVolumes()) {
                                Tools.getConfigData().setBigDRBDConf(
                                      !Tools.getConfigData().getBigDRBDConf());
                            }
                        }
                    } else if ("2".equals(index)) {
                        /* DRBD cancel */
                        int i = 1;
                        final int blockDevY = getBlockDevY();
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startDRBDTest2(blockDevY);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                        }
                    } else if ("3".equals(index)) {
                        /* DRBD 2 resoruces */
                        int i = 1;
                        final int blockDevY = getBlockDevY();
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startDRBDTest3(blockDevY);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                            if (cluster.getHostsArray()[0].hasVolumes()) {
                                Tools.getConfigData().setBigDRBDConf(
                                      !Tools.getConfigData().getBigDRBDConf());
                            }
                        }
                    } else if ("4".equals(index)) {
                        /* DRBD 2 volumes */
                        int i = 1;
                        final int blockDevY = getBlockDevY();
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startDRBDTest4(blockDevY);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                            if (cluster.getHostsArray()[0].hasVolumes()) {
                                Tools.getConfigData().setBigDRBDConf(
                                      !Tools.getConfigData().getBigDRBDConf());
                            }
                        }
                    }
                } else if ("VMs".equals(selected)) {
                    if ("1".equals(index) || "x1".equals(index)) {
                        /* VMs */
                        int i = 1;
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startVMTest1("vm-test" + index, 10);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                        }
                    } else if ("2".equals(index)) {
                        /* VMs dialog disabled textfields check. */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startVMTest2("vm-test" + index, 100);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                        resetTerminalAreas();
                    }
                }
                info(selected + " test " + index + " done");
            }
        });
        thread.start();
    }

    /** Check test. */
    private static void checkTest(final String test, final double no) {
        for (final Host host : cluster.getHosts()) {
            if (abortWithMouseMovement()) {
                return;
            }
            if (!aborted) {
                host.checkPCMKTest(test, no);
            }
        }
    }

    /** Check DRBD test. */
    private static void checkDRBDTest(final String test, final double no) {
        for (final Host host : cluster.getHosts()) {
            if (abortWithMouseMovement()) {
                return;
            }
            if (!aborted) {
                host.checkDRBDTest(test, no);
            }
        }
    }

    /** Check VM test on one host. */
    private static void checkVMTest(final Host host,
                                    final String test,
                                    final double no,
                                    final String name) {
        if (abortWithMouseMovement()) {
            return;
        }
        if (!aborted) {
            host.checkVMTest(test, no, name);
        }
    }
    /** Check VM test. */
    private static void checkVMTest(final String test,
                                    final double no,
                                    final String name) {
        for (final Host host : cluster.getHosts()) {
            if (abortWithMouseMovement()) {
                return;
            }
            if (!aborted) {
                host.checkVMTest(test, no, name);
            }
        }
    }

    /** TEST 1. */
    private static void startTest1() {
        slowFactor = 0.2f;
        aborted = false;
        /* create IPaddr2 with 192.168.100.100 ip */
        final int ipX = 235;
        final int ipY = 255;
        final int gx = 230;
        final int gy = 369;
        final int popX = 340;
        final int popY = 305;
        final int statefulX = 500;
        final int statefulY = 255;
        disableStonith();
        checkTest("test1", 1);
        enableStonith();
        checkTest("test1", 1.1);
        disableStonith();
        checkTest("test1", 1);
        moveTo(ipX, ipY);
        rightClick(); /* popup */
        moveTo(ipX + 57, ipY + 28);
        moveTo(ipX + 270, ipY + 28);
        moveTo(ipX + 267, ipY + 52);
        leftClick(); /* choose ipaddr */
        removeResource(ipX, ipY, -15);
        /* again */
        moveTo(ipX, ipY);
        rightClick(); /* popup */
        moveTo(ipX + 57, ipY + 28);
        moveTo(ipX + 270, ipY + 28);
        moveTo(ipX + 267, ipY + 52);
        leftClick(); /* choose ipaddr */

        moveTo(1072, 405);
        leftClick(); /* pull down */
        moveTo(1044, 450);
        leftClick(); /* choose */
        sleep(1000);
        press(KeyEvent.VK_1);
        press(KeyEvent.VK_0);
        press(KeyEvent.VK_0);
        sleep(1000);
        setTimeouts(false);
        moveTo(814, 189);
        sleep(6000); /* ptest */
        leftClick(); /* apply */
        /* CIDR netmask 24 */
        sleep(10000);
        moveTo(335, 129); /* advanced */
        sleep(2000);
        leftClick();
        sleep(2000);

        moveTo(960, 475); /* CIDR */
        sleep(3000);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_2);
        sleep(200);
        press(KeyEvent.VK_4);
        sleep(1000);

        moveTo(335, 129); /* not advanced */
        sleep(2000);
        leftClick();
        sleep(2000);

        moveTo(814, 189);
        sleep(6000); /* ptest */
        leftClick(); /* apply */

        checkTest("test1", 2); /* 2 */

        /* pingd */
        moveTo(1100, 298);
        leftPress(); /* scroll bar */
        moveTo(1100, 510);
        leftRelease();
        moveTo(1076, 387);
        leftClick();
        moveTo(1037, 454);
        leftClick(); /* no ping */
        moveTo(809, 192); /* ptest */
        sleep(2000);
        leftClick(); /*  apply */
        sleep(2000);
        checkTest("test1", 2.1); /* 2.1 */

        moveTo(1076, 387);
        leftClick();
        moveTo(1067, 412);
        leftClick(); /* default */
        moveTo(809, 192); /* ptest */
        sleep(2000);
        leftClick(); /*  apply */

        moveTo(1100, 298);
        leftPress(); /* scroll bar */
        moveTo(1100, 550);
        leftRelease();

        /* group with dummy resources */
        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(gx + 46, gy + 11);
        sleep(1000);
        leftClick(); /* choose group */
        sleep(3000);
        /* remove it */
        removeResource(gx, gy, 0);

        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(gx + 46, gy + 11);
        sleep(1000);
        leftClick(); /* choose group */
        sleep(3000);

        rightClick(); /* group popup */
        moveTo(gx + 80, gy + 20);
        moveTo(gx + 84, gy + 22);
        moveTo(gx + 580, gy + 22);
        sleep(1000);
        typeDummy();
        sleep(1000);

        /* remove it */
        removeResource(gx, gy, 0);

        /* group with dummy resources, once again */
        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(gx + 46, gy + 11);
        sleep(1000);
        leftClick(); /* choose group */
        sleep(3000);
        rightClick(); /* group popup */
        moveTo(gx + 80, gy + 20);
        moveTo(gx + 84, gy + 22);
        moveTo(gx + 580, gy + 22);
        sleep(1000);
        typeDummy();
        sleep(1000);

        moveTo(125, 320);
        sleep(1000);
        rightClick();
        sleep(1000);
        moveTo(150, 611);
        leftClick(); /* remove service */
        removeResource(gx, gy, 0);

        /* group with dummy resources, once again */
        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(gx + 46, gy + 11);
        sleep(1000);
        leftClick(); /* choose group */
        sleep(3000);
        rightClick(); /* group popup */
        moveTo(gx + 80, gy + 20);
        moveTo(gx + 84, gy + 22);
        moveTo(gx + 580, gy + 22);
        sleep(1000);
        typeDummy();
        sleep(1000);

        removeResource(gx, gy, 0);

        /* once again */
        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(gx + 46, gy + 11);
        sleep(1000);
        leftClick(); /* choose group */
        sleep(3000);
        checkTest("test1", 2); /* 2 */
        rightClick(); /* group popup */
        moveTo(gx + 80, gy + 20);
        moveTo(gx + 84, gy + 22);
        moveTo(gx + 580, gy + 22);
        sleep(1000);
        typeDummy();
        sleep(1000);
        setTimeouts(true);
        moveTo(809, 192); /* ptest */
        sleep(2000);
        leftClick(); /*  apply */
        for (int i = 0; i < 2; i++) {
            /* another group resource */
            sleep(20000);
            moveTo(gx + 46, gy - 19);
            rightClick(); /* group popup */
            sleep(2000 + i * 500);
            moveTo(gx + 80, gy - 10);
            moveTo(gx + 84, gy - 8);
            moveTo(gx + 580, gy - 8);
            sleep(1000);
            typeDummy();
            sleep(i * 300);
            setTimeouts(true);
            moveTo(809, 192); /* ptest */
            sleep(6000);
            leftClick(); /* apply */
            sleep(1000);
        }
        sleep(4000);
        checkTest("test1", 3); /* 3 */
        /* constraints */
        addConstraint(gx, gy - 30, 0, true, -1);
        checkTest("test1", 3.1); /* 3.1 */

        /* move up, move down */
        for (int i = 0; i < 2; i++) {
            moveTo(137, 344);
            rightClick();
            sleep(1000);
            moveTo(221, 493);
            leftClick(); /* move res 3 up */
            sleepNoFactor(10000);
            checkTest("test1", 3.11); /* 3.11 */
            moveTo(137, 328);
            rightClick();
            moveTo(236, 515);
            leftClick(); /* move res 3 down */
            sleepNoFactor(10000);
            checkTest("test1", 3.12); /* 3.12 */
        }

        /* same as */
        moveTo(125, 345);
        sleep(1000);
        leftClick();

        moveTo(1100, 298);
        leftPress(); /* scroll bar */
        moveTo(1100, 510);
        leftRelease();

        for (int i = 0; i < 2; i++) {
            sleep(1000);
            moveTo(1073, 360);
            sleep(20000);
            leftClick();
            sleep(1000);
            moveTo(1073, 410);
            sleep(1000);
            leftClick(); /* choose another dummy */
            sleep(1000);
            moveTo(809, 192); /* ptest */
            sleep(4000);
            leftClick(); /* apply */
            sleep(4000);
            checkTest("test1", 3.2); /* 3.2 */

            moveTo(1073 , 360);
            sleep(30000);
            leftClick();
            sleep(1000);
            moveTo(1073 , 380);
            leftClick(); /* choose "nothing selected */
            sleep(1000);
            moveTo(809, 192); /* ptest */
            sleep(4000);
            leftClick(); /* apply */
            sleep(9000);
            checkTest("test1", 4); /* 4 */
            sleep(20000);
        }
        moveTo(1100, 298);
        leftPress(); /* scroll bar back */
        moveTo(1100, 150);
        leftRelease();

        /* locations */
        moveTo(ipX + 20, ipY);
        leftClick(); /* choose ip */
        setLocation(new Integer[]{KeyEvent.VK_I});
        sleep(3000);
        checkTest("test1", 4.1); /* 4.1 */

        setLocation(new Integer[]{KeyEvent.VK_BACK_SPACE,
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
        checkTest("test1", 4.2); /* 4.2 */

        setLocation(new Integer[]{KeyEvent.VK_BACK_SPACE,
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
        checkTest("test1", 4.3); /* 4.3 */

        setLocation(new Integer[]{KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE});
        sleep(3000);
        checkTest("test1", 4.4); /* 4.4 */
        for (int i = 0; i < 3; i++) {
            removeConstraint(popX, popY);
            checkTest("test1", 5); /* 5 */
            addConstraint(gx, gy - 30, 0, true, -1);
            checkTest("test1", 4.4); /* 4.4 */
        }

        removeConstraint(popX, popY);
        sleep(3000);
        checkTest("test1", 5); /* 5 */
        sleep(1000);

        addConstraint(gx, gy - 30, 9, true, -1);
        sleep(5000);
        checkTest("test1", 6); /* 6 */

        removeOrder(popX, popY);
        sleep(4000);
        checkTest("test1", 7);

        addOrder(popX, popY);
        sleep(4000);
        checkTest("test1", 8);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 9);

        addColocation(popX, popY);
        sleep(4000);
        checkTest("test1", 10);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 10.1);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest("test1", 10.2);

        addConstraintOrderOnly(gx, gy - 30, 0, 25, 0, true, -1);
        sleep(4000);
        checkTest("test1", 10.3);

        addColocation(popX, popY);
        sleep(4000);
        checkTest("test1", 10.4);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest("test1", 10.5);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 10.6);

        addConstraintColocationOnly(gx, gy - 30, 0, 25, 0, true, -1);
        sleep(4000);
        checkTest("test1", 10.7);

        addOrder(popX, popY);
        sleep(4000);
        checkTest("test1", 10.8);

        removeConstraint(popX, popY);
        sleep(4000);
        checkTest("test1", 10.9);

        addConstraint(ipX, ipY, 0, false, -1);
        sleep(5000);
        checkTest("test1", 10.91);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest("test1", 10.92);

        addOrder(popX, popY);
        sleep(5000);
        checkTest("test1", 10.93);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 10.94);

        addColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 10.95);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 10.96);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest("test1", 10.97);

        addConstraintColocationOnly(ipX, ipY, -20, 100, 0, false, -1);
        sleep(5000);
        checkTest("test1", 10.98);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 10.99);

        addConstraintOrderOnly(ipX, ipY, -20, 100, 0, false, -1);
        sleep(5000);
        checkTest("test1", 11);

        addColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 11.1);

        removeConstraint(popX, popY);
        sleep(5000);
        checkTest("test1", 11.2);

        addConstraint(ipX, ipY, 60, false, -1);
        sleep(5000);
        checkTest("test1", 11.3);
        stopResource(ipX, ipY, 0);
        sleep(5000);
        checkTest("test1", 11.4);
        resetStartStopResource(ipX, ipY);
        sleep(5000);
        checkTest("test1", 11.5);

        moveTo(ipX + 20, ipY + 10);
        leftClick(); /* choose ip */
        stopResource(1010, 180, 10); /* actions menu stop */
        sleep(5000);
        checkTest("test1", 11.501);

        moveTo(ipX + 20, ipY + 10);
        leftClick(); /* choose ip */
        startResource(1010, 180, 20); /* actions menu start */
        sleep(5000);
        checkTest("test1", 11.502);

        resetStartStopResource(ipX, ipY);
        sleep(5000);
        checkTest("test1", 11.5);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 11.51);

        addColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 11.52);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest("test1", 11.53);

        addOrder(popX, popY);
        sleep(5000);
        checkTest("test1", 11.54);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest("test1", 11.55);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest("test1", 11.56);

        addConstraintOrderOnly(ipX, ipY, -20, 100, 55, false, -1);
        sleep(5000);
        checkTest("test1", 11.57);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest("test1", 11.58);

        addConstraintColocationOnly(ipX, ipY, -20, 100, 55, false, -1);
        sleep(5000);
        checkTest("test1", 11.59);

        addOrder(popX, popY);
        sleep(5000);
        checkTest("test1", 11.6);

        removeConstraint(popX, popY);
        sleep(5000);
        checkTest("test1", 11.7);

        addConstraint(gx, gy - 30, 9, true, -1);
        sleep(5000);
        checkTest("test1", 11.8);
        /** Add m/s Stateful resource */
        moveTo(statefulX, statefulY);
        rightClick(); /* popup */
        moveTo(statefulX + 137, statefulY + 8);
        moveTo(statefulX + 137, statefulY + 28);
        moveTo(statefulX + 412, statefulY + 27);
        moveTo(statefulX + 432, statefulY + 70);
        moveTo(statefulX + 665, statefulY + 70);
        sleep(1000);

        press(KeyEvent.VK_S);
        sleep(200);
        press(KeyEvent.VK_T);
        sleep(200);
        press(KeyEvent.VK_A);
        sleep(200);
        press(KeyEvent.VK_T);
        sleep(200);
        press(KeyEvent.VK_E);
        sleep(200);
        press(KeyEvent.VK_F);
        sleep(200);
        press(KeyEvent.VK_ENTER); /* choose Stateful */
        sleep(1000);

        moveTo(812, 179);
        sleep(5000);
        leftClick(); /* apply */
        checkTest("test1", 11.9);
        sleep(3000);
        /* set clone max to 1 */
        moveTo(978, 381);
        sleep(3000);
        leftClick(); /* Clone Max */
        sleep(3000);
        leftClick();
        sleep(3000);
        press(KeyEvent.VK_BACK_SPACE);
        sleep(3000);
        press(KeyEvent.VK_1);
        sleep(3000);
        setTimeouts(false);
        moveTo(812, 179);
        sleep(3000);
        leftClick(); /* apply */
        sleep(3000);
        checkTest("test1", 12);
        stopResource(statefulX, statefulY, 0);
        sleep(3000);
        checkTest("test1", 13);

        startResource(statefulX, statefulY, 0);
        sleep(10000);
        checkTest("test1", 14);
        unmanageResource(statefulX, statefulY, 0);
        sleep(3000);
        checkTest("test1", 15);
        manageResource(statefulX, statefulY, 0);
        sleep(3000);
        checkTest("test1", 16);

        /* IP addr cont. */
        stopResource(ipX, ipY, 0);
        sleep(3000);
        checkTest("test1", 17);
        startResource(ipX, ipY, 0);
        sleep(3000);
        checkTest("test1", 18);
        unmanageResource(ipX, ipY, 0);
        sleep(3000);
        checkTest("test1", 19);
        manageResource(ipX, ipY, 0);
        sleep(3000);
        checkTest("test1", 20);
        migrateResource(ipX, ipY, 0);
        sleep(3000);
        checkTest("test1", 21);
        unmigrateResource(ipX, ipY, 0);
        sleep(3000);
        checkTest("test1", 22);

        /* Group cont. */
        stopResource(gx, gy - 30, 15);
        sleep(10000);
        checkTest("test1", 23);
        startResource(gx, gy - 30, 15);
        sleep(8000);
        checkTest("test1", 24);
        unmanageResource(gx, gy - 30, 15);
        sleep(5000);
        checkTest("test1", 25);
        manageResource(gx, gy - 30, 15);
        sleep(5000);
        checkTest("test1", 26);
        migrateResource(gx, gy - 30, 25);
        sleep(5000);
        moveTo(gx, gy);
        leftClick();
        checkTest("test1", 27);
        unmigrateResource(1020, 180, 55); /* actions menu unmigrate */
        sleep(5000);
        checkTest("test1", 28);
        stopResource(ipX, ipY, 0);
        sleep(5000);
        moveTo(gx, gy);
        leftClick();
        stopGroup(1020, 180, 25); /* actions menu stop */
        sleep(5000);
        stopGroup(statefulX, statefulY, 0);
        stopEverything(); /* to be sure */
        sleep(5000);
        checkTest("test1", 29);

        if (true) {
            removeResource(ipX, ipY, -15);
            sleep(5000);
            removeGroup(gx, gy - 20, 0);
            sleep(5000);
            removeGroup(statefulX, statefulY, -15);
        } else {
            removeEverything();
        }
        if (!aborted) {
            sleepNoFactor(20000);
        }
        checkTest("test1", 1);
    }


    /** Stop everything. */
    private static void stopEverything() {
        sleep(10000);
        moveTo(335, 129); /* advanced */
        sleep(2000);
        leftClick();
        sleep(2000);
        moveTo(700, 568);
        rightClick(); /* popup */
        sleep(3000);
        moveTo(760, 644);
        sleep(3000);
        leftClick();
        moveTo(335, 129); /* not advanced */
        sleep(2000);
        leftClick();
        sleep(2000);
    }

    /** Remove everything. */
    private static void removeEverything() {
        sleep(10000);
        moveTo(335, 129); /* advanced */
        sleep(2000);
        leftClick();
        sleep(2000);
        moveTo(700, 568);
        rightClick(); /* popup */
        sleep(3000);
        moveTo(760, 674);
        sleep(3000);
        leftClick();
        confirmRemove();
        sleep(3000);
        leftClick();
        moveTo(335, 129); /* not advanced */
        sleep(2000);
        leftClick();
        sleep(2000);
    }

    /** Enable stonith if it is enabled. */
    private static void enableStonith() {
        moveTo(271, 250);
        leftClick(); /* global options */
        final String stonith = cluster.getBrowser()
                    .getClusterStatus().getGlobalParam("stonith-enabled");
        if (stonith != null && "false".equals(stonith)) {
            moveTo(944, 298);
            leftClick(); /* enable stonith */
        }
        moveTo(828, 183);
        sleep(2000);
        leftClick(); /* apply */
    }

    /** Disable stonith if it is enabled. */
    private static void disableStonith() {
        moveTo(271, 250);
        leftClick(); /* global options */
        final String stonith = cluster.getBrowser()
                    .getClusterStatus().getGlobalParam("stonith-enabled");
        if (stonith == null || "true".equals(stonith)) {
            moveTo(944, 298);
            leftClick(); /* disable stonith */
        }
        moveTo(1073, 337);
        leftClick(); /* no quorum policy */
        moveTo(1058, 355);
        leftClick(); /* ignore */
        moveTo(828, 183);
        sleep(2000);
        leftClick(); /* apply */
    }

    /** TEST 2. */
    private static void startTest2() {
        slowFactor = 0.3f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;
        final int dummy2X = 545;
        final int dummy2Y = 255;
        final int dummy3X = 235;
        final int dummy3Y = 390;
        final int dummy4X = 545;
        final int dummy4Y = 390;
        final int phX = 445;
        final int phY = 390;

        disableStonith();
        checkTest("test2", 1);
        /* create 4 dummies */
        chooseDummy(dummy1X, dummy1Y, false, true);
        chooseDummy(dummy2X, dummy2Y, false, true);
        chooseDummy(dummy3X, dummy3Y, false, true);
        chooseDummy(dummy4X, dummy4Y, false, true);
        checkTest("test2", 2);

        /* placeholder */
        moveTo(phX, phY);
        rightClick();
        sleep(2000);
        moveTo(phX + 30 , phY + 45);
        sleep(2000);
        leftClick();
        checkTest("test2", 3);
        /* constraints */
        addConstraint(phX, phY, 0, false, -1); /* with dummy 1 */
        sleep(2000);
        moveTo(809, 192); /* ptest */
        sleep(2000);
        leftClick(); /*  apply */
        sleep(5000);
        checkTest("test2", 4);

        final int dum1PopX = dummy1X + 130;
        final int dum1PopY = dummy1Y + 50;
        for (int i = 0; i < 1; i++) {
            removeOrder(dum1PopX, dum1PopY);
            sleep(4000);

            checkTest("test2", 5);

            addOrder(dum1PopX, dum1PopY);
            sleep(4000);
            checkTest("test2", 6);

            removeColocation(dum1PopX, dum1PopY);
            sleep(5000);
            checkTest("test2", 7);

            addColocation(dum1PopX, dum1PopY);
            sleep(4000);
            checkTest("test2", 8);
        }

        addConstraint(dummy3X, dummy3Y, 80, false, -1); /* with ph */
        sleep(5000);
        checkTest("test2", 9);

        final int dum3PopX = dummy3X + 165;
        final int dum3PopY = dummy3Y - 10;
        for (int i = 0; i < 2; i++) {
            removeColocation(dum3PopX, dum3PopY);
            sleep(4000);

            checkTest("test2", 9.1);

            addColocation(dum3PopX, dum3PopY);
            sleep(4000);
            checkTest("test2", 9.2);

            removeOrder(dum3PopX, dum3PopY);
            sleep(5000);
            checkTest("test2", 9.3);

            addOrder(dum3PopX, dum3PopY);
            sleep(4000);
            checkTest("test2", 9.4);
        }

        addConstraint(phX, phY, 0, false, -1); /* with dummy 2 */
        sleep(5000);
        checkTest("test2", 10);
        addConstraint(dummy4X, dummy4Y, 80, false, -1); /* with ph */
        sleep(5000);
        checkTest("test2", 11);

        /* ph -> dummy2 */
        final int dum2PopX = dummy2X - 10;
        final int dum2PopY = dummy2Y + 70;
        removeConstraint(dum2PopX, dum2PopY);
        sleep(4000);
        checkTest("test2", 11.1);
        addConstraintOrderOnly(phX,
                               phY,
                               -50,
                               20,
                               0,
                               false,
                               -1); /* with dummy 2 */
        sleep(4000);
        checkTest("test2", 11.2);
        removeOrder(dum2PopX, dum2PopY);
        sleep(4000);
        checkTest("test2", 11.3);
        addConstraintColocationOnly(phX,
                                    phY,
                                    -50,
                                    23,
                                    0,
                                    false,
                                    -1); /* with dummy 2 */
        sleep(4000);
        checkTest("test2", 11.4);
        addOrder(dum2PopX, dum2PopY);
        sleep(4000);
        checkTest("test2", 11.5);

        /* dummy4 -> ph */
        final int dum4PopX = dummy4X - 40;
        final int dum4PopY = dummy4Y - 10;
        removeConstraint(dum4PopX, dum4PopY);
        sleep(4000);
        checkTest("test2", 11.6);
        moveTo(dummy4X + 20, dummy4Y + 5);
        sleep(1000);
        rightClick(); /* workaround for the next popup not working. */
        sleep(1000);
        rightClick(); /* workaround for the next popup not working. */
        sleep(1000);
        addConstraintColocationOnly(dummy4X,
                                    dummy4Y,
                                    -50,
                                    93,
                                    80,
                                    false,
                                    -1); /* with ph */
        sleep(4000);
        checkTest("test2", 11.7);
        removeColocation(dum4PopX, dum4PopY);
        sleep(4000);
        checkTest("test2", 11.8);
        moveTo(dummy4X + 20, dummy4Y + 5);
        sleep(1000);
        rightClick(); /* workaround for the next popup not working. */
        sleep(1000);
        addConstraintOrderOnly(dummy4X,
                               dummy4Y,
                               -50,
                               90,
                               80,
                               false,
                               -1); /* ph 2 */
        sleep(4000);
        checkTest("test2", 11.9);
        addColocation(dum4PopX, dum4PopY);
        sleep(4000);
        checkTest("test2", 11.91);
        /* remove one dummy */
        stopResource(dummy1X, dummy1Y, 0);
        sleep(5000);
        checkTest("test2", 11.92);
        removeResource(dummy1X, dummy1Y, -15);
        sleep(5000);
        checkTest("test2", 12);
        stopResource(dummy2X, dummy2Y, 0);
        sleep(10000);
        stopResource(dummy3X, dummy3Y, 0);
        sleep(10000);
        stopResource(dummy3X, dummy3Y, 0);
        sleep(10000);
        stopResource(dummy4X, dummy4Y, 0);
        stopEverything();
        sleep(10000);
        checkTest("test2", 12.5);
        if (maybe()) {
            /* remove placeholder */
            moveTo(phX , phY);
            rightClick();
            sleep(1000);
            moveTo(phX + 40 , phY + 80);
            leftClick();
            confirmRemove();
            sleep(5000);

            /* remove rest of the dummies */
            removeResource(dummy2X, dummy2Y, -15);
            sleep(5000);
            checkTest("test2", 14);
            removeResource(dummy3X, dummy3Y, -15);
            sleep(5000);
            checkTest("test2", 15);
            removeResource(dummy4X, dummy4Y, -15);
            sleep(5000);
        } else {
            removeEverything();
            removePlaceHolder(phX, phY);
        }
        if (!aborted) {
            sleepNoFactor(20000);
        }
        checkTest("test2", 16);
    }

    /** TEST 4. */
    private static void startTest4() {
        slowFactor = 0.5f;
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

        disableStonith();
        checkTest("test4", 1);
        /* create 6 dummies */
        chooseDummy(dummy1X, dummy1Y, false, true);
        chooseDummy(dummy2X, dummy2Y, false, true);
        chooseDummy(dummy3X, dummy3Y, false, true);
        chooseDummy(dummy4X, dummy4Y, false, true);
        chooseDummy(dummy5X, dummy5Y, false, true);
        chooseDummy(dummy6X, dummy6Y, false, true);
        checkTest("test4", 2);

        /* 2 placeholders */
        final int count = 1;
        for (int i = 0; i < count; i++) {
            moveTo(ph1X, ph1Y);
            rightClick();
            sleep(2000);
            moveTo(ph1X + 30 , ph1Y + 45);
            sleep(2000);
            leftClick();

            moveTo(ph2X, ph2Y);
            rightClick();
            sleep(2000);
            moveTo(ph2X + 30 , ph2Y + 45);
            sleep(2000);
            leftClick();
            checkTest("test4", 2);

            /* constraints */
            /* menu dummy 5 with ph2 */
            addConstraint(120, 346, 160, false, -1);
            /* menu dummy 6 with ph2 */
            addConstraint(120, 364, 160, false, -1);

            /* with dummy 3 */
            addConstraint(ph2X, ph2Y, 60, false, -1);
            /* with dummy 4 */
            addConstraint(ph2X, ph2Y, 60, false, -1);

            /* with ph1 */
            addConstraint(dummy3X, dummy3Y, 80, false, -1);
            /* with ph1 */
            addConstraint(dummy4X, dummy4Y, 80, false, -1);

            addConstraint(ph1X, ph1Y, 5, false, -1); /* with dummy 1 */
            addConstraint(ph1X, ph1Y, 5, false, -1); /* with dummy 2 */
            checkTest("test4", 2);

            /* TEST test */
            if (i < count - 1) {
                removePlaceHolder(ph1X, ph1Y);
                removePlaceHolder(ph2X, ph2Y);
            }
        }
        moveTo(809, 192); /* ptest */
        sleep(2000);
        leftClick(); /*  apply */

        checkTest("test4", 3);
        stopEverything();
        checkTest("test4", 4);
        removeEverything();
        removePlaceHolder(ph1X, ph1Y);
        removePlaceHolder(ph2X, ph2Y);
        sleep(40000);
    }

    /** TEST 5. */
    private static void startTest5() {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;

        final int dummy2X = 500;
        final int dummy2Y = 255;

        final int ph1X = 380;
        final int ph1Y = 500;


        disableStonith();
        /* create 2 dummies */
        checkTest("test5", 1);

        /* placeholders */
        moveTo(ph1X, ph1Y);
        rightClick();
        sleep(2000);
        moveTo(ph1X + 30 , ph1Y + 45);
        sleep(2000);
        leftClick();

        chooseDummy(dummy1X, dummy1Y, false, true);
        chooseDummy(dummy2X, dummy2Y, false, true);
        checkTest("test5", 2);

        addConstraint(dummy2X, dummy2Y, 35, false, -1);
        sleep(20000);
        checkTest("test5", 2);
        addConstraint(ph1X, ph1Y, 5, false, -1);

        moveTo(ph1X, ph1Y);
        sleep(2000);
        leftClick();
        sleep(2000);
        moveTo(809, 192); /* ptest */
        sleep(2000);
        leftClick(); /*  apply */
        checkTest("test5", 2.1);

        leftClick(); /*  apply */
        int dum1PopX = dummy1X + 80;
        int dum1PopY = dummy1Y + 60;
        removeConstraint(dum1PopX, dum1PopY);
        checkTest("test5", 2.5);
        /* constraints */
        for (int i = 1; i <= 5; i++) {
            addConstraint(dummy1X, dummy1Y, 35, false, -1);

            checkTest("test5", 3);

            removeConstraint(dum1PopX, dum1PopY);
            checkTest("test5", 2.5);

            addConstraint(ph1X, ph1Y, 5, false, -1);

            checkTest("test5", 3.5);

            removeConstraint(dum1PopX, dum1PopY);
            checkTest("test5", 2.5);
            info("i: " + i);
        }
        stopEverything();
        checkTest("test5", 3.1);
        removeEverything();
        sleep(5000);
        checkTest("test5", 1);
    }

    /** TEST 6. */
    private static void startTest6() {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;

        final int ph1X = 315;
        final int ph1Y = 394;


        //disableStonith();
        /* create 2 dummies */
        //checkTest("test5", 1);

        /* placeholders */
        moveTo(ph1X, ph1Y);
        rightClick();
        sleep(2000);
        moveTo(ph1X + 30 , ph1Y + 45);
        sleep(2000);
        leftClick();

        chooseDummy(dummy1X, dummy1Y, false, true);
        final int dum1PopX = dummy1X + 70;
        final int dum1PopY = dummy1Y + 60;
        for (int i = 0; i < 20; i++) {
            if (i % 5 == 0) {
                info("test6 i: " + i);
            }
            addConstraint(ph1X, ph1Y, 5, false, -1);
            if (!aborted) {
                sleepNoFactor(2000);
            }
            removeConstraint(dum1PopX, dum1PopY);
        }
        stopEverything();
        sleepNoFactor(20000);
        removeEverything();
        sleepNoFactor(20000);
        resetTerminalAreas();
    }

    private static void startTest7() {
        slowFactor = 0.5f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;
        disableStonith();
        for (int i = 30; i > 0; i--) {
            if (i % 5 == 0) {
                info("test7 I: " + i);
            }
            checkTest("test7", 1);
            /* create dummy */
            sleep(5000);
            chooseDummy(dummy1X, dummy1Y, false, true);
            checkTest("test7", 2);
            sleep(5000);
            stopResource(dummy1X, dummy1Y, 0);
            checkTest("test7", 3);
            sleep(5000);
            removeResource(dummy1X, dummy1Y, -15);
        }
        System.gc();
    }

    private static void startTest8() {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 540;
        final int dummy1Y = 250;
        disableStonith();
        checkTest("test8", 1);
        final int count = 30;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("test8 i: " + i);
            }
            //checkTest("test7", 1);
            sleep(5000);
            chooseDummy(dummy1X, dummy1Y, false, true);
            sleep(5000);
            moveTo(550, 250);
            leftPress(); /* move the reosurce */
            moveTo(300, 250);
            leftRelease();
        }
        checkTest("test8-" + count, 2);
        stopEverything();
        checkTest("test8-" + count, 3);
        removeEverything();
        checkTest("test8", 4);
        resetTerminalAreas();
    }

    private static void startTestA() {
        slowFactor = 0.5f;
        aborted = false;
        final int gx = 235;
        final int gy = 255;
        disableStonith();
        for (int i = 20; i > 0; i--) {
            if (i % 5 == 0) {
                info("testA I: " + i);
            }
            checkTest("testA", 1);
            /* group with dummy resources */
            moveTo(gx, gy);
            sleep(1000);
            rightClick(); /* popup */
            moveTo(gx + 46, gy + 11);
            sleep(1000);
            leftClick(); /* choose group */
            sleep(3000);
            /* create dummy */
            moveTo(gx + 46, gy + 11);
            rightClick(); /* group popup */
            sleep(2000 + i * 500);
            moveTo(gx + 80, gy + 20);
            moveTo(gx + 84, gy + 22);
            moveTo(gx + 580, gy + 22);
            sleep(1000);
            typeDummy();
            sleep(i * 300);
            setTimeouts(true);
            moveTo(809, 192); /* ptest */
            sleep(6000);
            leftClick(); /* apply */
            sleep(6000);
            checkTest("testA", 2);
            stopResource(gx, gy, 0);
            sleep(6000);
            checkTest("testA", 3);
            removeResource(gx, gy, 0);
            resetTerminalAreas();
        }
        System.gc();
    }

    private static void startTestB() {
        slowFactor = 0.5f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;
        disableStonith();
        for (int i = 20; i > 0; i--) {
            if (i % 5 == 0) {
                info("testB I: " + i);
            }
            checkTest("testB", 1);
            /* create dummy */
            sleep(5000);
            chooseDummy(dummy1X, dummy1Y, true, true);
            checkTest("testB", 2);
            sleep(5000);
            stopResource(dummy1X, dummy1Y, 0);
            checkTest("testB", 3);
            sleep(5000);
            removeResource(dummy1X, dummy1Y, -15);
            resetTerminalAreas();
        }
        System.gc();
    }

    private static void startTestC() {
        slowFactor = 0.5f;
        final int statefulX = 500;
        final int statefulY = 255;
        disableStonith();
        for (int i = 20; i > 0; i--) {
            if (i % 5 == 0) {
                info("testC I: " + i);
            }
            checkTest("testC", 1);
            /** Add m/s Stateful resource */
            moveTo(statefulX, statefulY);
            rightClick(); /* popup */
            moveTo(statefulX + 137, statefulY + 8);
            moveTo(statefulX + 137, statefulY + 28);
            moveTo(statefulX + 412, statefulY + 27);
            moveTo(statefulX + 432, statefulY + 70);
            moveTo(statefulX + 665, statefulY + 70);
            sleep(1000);

            press(KeyEvent.VK_S);
            sleep(200);
            press(KeyEvent.VK_T);
            sleep(200);
            press(KeyEvent.VK_A);
            sleep(200);
            press(KeyEvent.VK_T);
            sleep(200);
            press(KeyEvent.VK_E);
            sleep(200);
            press(KeyEvent.VK_F);
            sleep(200);
            press(KeyEvent.VK_ENTER); /* choose Stateful */
            sleep(1000);

            moveTo(812, 179);
            sleep(1000);
            leftClick(); /* apply */
            sleep(4000);
            stopResource(statefulX, statefulY, -20);
            checkTest("testC", 2);
            sleep(5000);
            removeResource(statefulX, statefulY, -20);
            resetTerminalAreas();
        }
    }

    /** Pacemaker Leak tests. */
    private static void startTestD() {
        slowFactor = 0.2f;
        aborted = false;
        int count = 20;
        final int dummy1X = 540;
        final int dummy1Y = 250;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("testD 1 I: " + i);
            }
            chooseDummy(dummy1X, dummy1Y, false, false);
            removeResource(dummy1X, dummy1Y, -20);
        }
        chooseDummy(dummy1X, dummy1Y, false, false);
        int pos = 0;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("testD 2 I: " + i);
            }
            double rand = Math.random();
            if (rand < 0.33) {
                if (pos == 1) {
                    continue;
                }
                pos = 1;
                moveTo(796, 247);
                leftClick();
            } else if (rand < 0.66) {
                if (pos == 2) {
                    continue;
                }
                pos = 2;
                moveTo(894, 248);
                leftClick();
            } else {
                if (pos == 3) {
                    continue;
                }
                pos = 3;
                moveTo(994, 248);
                leftClick();
            }
        }
        removeResource(dummy1X, dummy1Y, -20);
    }

    /** Host wizard deadlock. */
    private static void startTestE() {
        slowFactor = 0.2f;
        aborted = false;
        int count = 200;
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                info("testE I: " + i);
            }
            moveTo(300 , 200); /* host */
            sleep(2000);
            rightClick();
            sleep(2000);
            moveTo(400 , 220); /* wizard */
            sleep(2000);
            leftClick();
            sleep(30000);
            moveTo(940 , 570); /* cancel */
            sleep(2000);
            leftClick();
            sleep(2000);
        }
    }

    /** Cloned group. */
    private static void startTestF() {
        slowFactor = 0.2f;
        aborted = false;
        final int gx = 235;
        final int gy = 255;
        disableStonith();
        checkTest("testF", 1);
        /* group with dummy resources */
        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(gx + 46, gy + 11);
        sleep(1000);
        leftClick(); /* choose group */
        sleep(3000);
        moveTo(900, 250);
        leftClick(); /* clone */

        final int gxM = 110; /* tree menu */
        final int gyM = 290;
        int type = 1;
        //int type = 2;
        for (int i = 2; i > 0; i--) {
            info("I: " + i);
            /* create dummy */
            moveTo(gxM + 46, gyM + 11);
            rightClick(); /* group popup */
            sleep(2000 + i * 500);
            moveTo(gxM + 80, gyM + 20);
            moveTo(gxM + 84, gyM + 22);
            moveTo(gxM + 580, gyM + 22);
            sleep(1000);
            typeDummy();
            sleep(i * 300);
            setTimeouts(true);
            if (type == 1) {
                moveTo(809, 192); /* ptest */
                sleep(6000);
                leftClick(); /* apply */
                sleep(6000);
            }
        }
        if (type != 1) {
            moveTo(809, 192); /* ptest */
            sleep(6000);
            leftClick(); /* apply */
            sleep(6000);
        }
        checkTest("testF", 2);
        /* set resource stickiness */
        moveTo(1000, 480);
        sleep(1000);
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_BACK_SPACE);
        sleep(200);
        press(KeyEvent.VK_2);
        sleep(1000);
        moveTo(809, 192); /* ptest */
        sleep(6000);
        leftClick(); /* apply */
        sleep(6000);
        checkTest("testF", 3);

        stopResource(gx, gy, 0);
        sleep(6000);
        checkTest("testF", 4);
        removeResource(gx, gy, -40);
        resetTerminalAreas();
        System.gc();
    }

    /** Host wizard locked until focus is lost. */
    private static void startGUITest1(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                info("gui-test1 I: " + i);    
            }
            moveTo(470, 120); /* host wizard */
            sleep(500);
            leftClick();
            sleep(1000);
            if (!isColor(360, 490, new Color(255, 100, 100), true)) {
                info("gui-test1 1: failed");    
                break;
            }
            boolean ok = false;
            for (int error = 0; error < 5; error++) {
                sleep(100);
                press(KeyEvent.VK_X);
                if (!isColor(360, 490, new Color(255, 100, 100), false)) {
                    sleepNoFactor(1000);
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                info("gui-test1 2: failed");    
                break;
            }
            moveTo(910 , 565); /* cancel */
            sleep(500);
            leftClick();
            sleep(1000);
        }
    }

    /** Cluster wizard locked until focus is lost. */
    private static void startGUITest2(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                info("gui-test2 " + i);    
            }

            moveTo(800 , 120); /* cluster wizard */
            sleep(500);
            leftClick();
            sleep(2000);
            if (!isColor(336, 520, new Color(184, 207, 229), true)) {
                info("gui-test2: failed");    
                break;
            }
            moveTo(910 , 565); /* cancel */
            sleep(500);
            leftClick();
            sleep(1000);
        }
    }

    /** Sets location. */
    private static void setLocation(final Integer[] events) {
        moveTo(1100, 298);
        leftPress(); /* scroll bar */
        moveTo(1100, 510);
        leftRelease();

        moveTo(1041 , 331);
        sleep(2000);
        leftClick();
        for (final int ev : events) {
            if (ev == KeyEvent.VK_PLUS) {
                robot.keyPress(KeyEvent.VK_SHIFT);
                sleep(400);
            }
            press(ev);
            sleep(400);
            if (ev == KeyEvent.VK_PLUS) {
                robot.keyRelease(KeyEvent.VK_SHIFT);
                sleep(400);
            }
        }
        moveTo(809, 192); /* ptest */
        sleep(4000);
        leftClick(); /* apply */
        sleep(2000);

        moveTo(1100, 350);
        leftPress(); /* scroll bar back */
        moveTo(1100, 150);
        leftRelease();

    }

    /** Choose dummy resource. */
    private static void typeDummy() {
        press(KeyEvent.VK_D);
        sleep(200);
        press(KeyEvent.VK_U);
        sleep(200);
        press(KeyEvent.VK_M);
        sleep(200);
        press(KeyEvent.VK_M);
        sleep(200);
        press(KeyEvent.VK_Y);
        sleep(200);
        press(KeyEvent.VK_ENTER); /* choose dummy */
    }

    /** Sets start timeout. */
    private static void setTimeouts(final boolean migrateTimeouts) {
        int yCorr = -5;
        if (migrateTimeouts) {
            yCorr = -95;
        }
        sleep(3000);
        moveTo(1100, 298);
        leftPress(); /* scroll bar */
        moveTo(1100, 550);
        leftRelease();
        moveTo(956, 490 + yCorr);
        leftClick(); /* start timeout */
        press(KeyEvent.VK_2);
        sleep(200);
        press(KeyEvent.VK_0);
        sleep(200);
        press(KeyEvent.VK_0);
        sleep(200);

        moveTo(956, 520 + yCorr);
        leftClick(); /* stop timeout */
        press(KeyEvent.VK_1);
        sleep(200);
        press(KeyEvent.VK_9);
        sleep(200);
        press(KeyEvent.VK_2);
        sleep(200);

        moveTo(956, 550 + yCorr);
        leftClick(); /* monitor timeout */
        press(KeyEvent.VK_1);
        sleep(200);
        press(KeyEvent.VK_5);
        sleep(200);
        press(KeyEvent.VK_4);
        sleep(200);

        moveTo(956, 580 + yCorr);
        leftClick(); /* monitor interval */
        press(KeyEvent.VK_1);
        sleep(200);
        press(KeyEvent.VK_2);
        sleep(200);
        press(KeyEvent.VK_1);
        sleep(200);
        if (migrateTimeouts) {
            moveTo(956, 610 + yCorr);
            leftClick(); /* reload */
            press(KeyEvent.VK_BACK_SPACE);
            sleep(200);
            press(KeyEvent.VK_BACK_SPACE);
            sleep(200);

            moveTo(956, 630 + yCorr);
            leftClick(); /* migrate from */
            press(KeyEvent.VK_1);
            sleep(200);
            press(KeyEvent.VK_2);
            sleep(200);
            press(KeyEvent.VK_3);
            sleep(200);

            moveTo(956, 660 + yCorr);
            leftClick(); /* migrate to */
            press(KeyEvent.VK_1);
            sleep(200);
            press(KeyEvent.VK_2);
            sleep(200);
            press(KeyEvent.VK_2);
            sleep(200);
        }

        moveTo(1100, 350);
        leftPress(); /* scroll bar back */
        moveTo(1100, 150);
        leftRelease();
    }

    public static void sleepNoFactor(final double x) {
        sleep(x / slowFactor);
    }

    /** Sleep for x milliseconds * slowFactor + some random time. */
    private static void sleep(final double x) {
        if (abortWithMouseMovement()) {
            return;
        }
        if (!aborted) {
            final double sleepTime = x * slowFactor
                                     + x * slowFactor * Math.random();
            final double step = 100;
            double rest = sleepTime;
            for (double i = step; i < sleepTime; i += step) {
                Tools.sleep((int) step);
                if (abortWithMouseMovement()) {
                    return;
                }
                rest -= step;
            }
            if (rest > 0) {
                Tools.sleep((int) rest);
            }
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
    private static void chooseDummy(final int x,
                                    final int y,
                                    final boolean clone,
                                    final boolean apply) {
        moveTo(x, y);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(x + 57, y + 28);
        moveTo(x + 290, y + 28);
        moveTo(x + 290, y + 72);
        moveTo(x + 580, y + 72);
        sleep(2000);
        typeDummy();
        if (apply) {
            sleep(2000);
            setTimeouts(true);
            if (clone) {
                moveTo(893, 250);
                leftClick(); /* clone */
            }
            moveTo(809, 192); /* ptest */
            sleep(4000);
            leftClick(); /* apply */
            sleep(2000);
        }
    }

    /** Removes service. */
    private static void removeResource(final int x,
                                       final int y,
                                       final int corr) {
        moveTo(x + 20, y);
        rightClick();
        sleep(1000);
        moveTo(x + 40 , y + 250 + corr);
        leftClick();
        confirmRemove();
    }

    /** Removes group. */
    private static void removeGroup(final int x, final int y, final int corr) {
        moveTo(x + 20, y);
        rightClick();
        sleep(120000);
        moveTo(x + 40 , y + 250 + corr);
        leftClick();
        confirmRemove();
    }

    /** Removes placeholder. */
    private static void removePlaceHolder(final int x, final int y) {
        moveTo(x + 20, y);
        rightClick();
        sleep(1000);
        moveTo(x + 40 , y + 60);
        leftClick();
        confirmRemove();
    }

    /** Confirms remove dialog. */
    private static void confirmRemove() {
        sleep(1000);
        moveTo(512 , 472);
        leftClick();
    }

    /** Stops resource. */
    private static void stopResource(final int x,
                                     final int y,
                                     final int yFactor) {
        moveTo(x + 50, y + 5);
        sleep(2000);
        rightClick(); /* popup */
        moveTo(x + 70, y + 130 + yFactor);
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Stops group. */
    private static void stopGroup(final int x,
                                  final int y,
                                  final int yFactor) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(x + 70, y + 130 + yFactor);
        sleep(120000); /* ptest */
        leftClick(); /* stop */
    }

    /** Removes target role. */
    private static void resetStartStopResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        leftClick(); /* select */

        moveTo(1072, 496);
        leftClick(); /* pull down */
        moveTo(1044, 520);
        leftClick(); /* choose */
        moveTo(814, 189);
        sleep(6000); /* ptest */
        leftClick(); /* apply */
    }

    /** Starts resource. */
    private static void startResource(final int x,
                                      final int y,
                                      final int yFactor) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(x + 70, y + 80 + yFactor);
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Migrate resource. */
    private static void migrateResource(final int x,
                                        final int y,
                                        final int yFactor) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(x + 70, y + 220 + yFactor);
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Unmigrate resource. */
    private static void unmigrateResource(final int x,
                                          final int y,
                                          final int yFactor) {
        moveTo(x + 50, y + 5);
        sleep(6000);
        rightClick(); /* popup */
        moveTo(x + 70, y + 260 + yFactor);
        sleep(12000); /* ptest */
        leftClick(); /* stop */
    }

    /** Unmanage resource. */
    private static void unmanageResource(final int x,
                                         final int y,
                                         final int yFactor) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(x + 70, y + 200 + yFactor);
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Manage resource. */
    private static void manageResource(final int x,
                                       final int y,
                                       final int yFactor) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(x + 70, y + 200 + yFactor);
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Go to the group service menu. */
    private static void groupServiceMenu(final int x,
                                         final int y,
                                         final int groupService) {
        final int groupServicePos = 365 + groupService;
        final int startBefore = 100 + groupService;
        moveTo(x + 82, y + groupServicePos);
        moveTo(x + 382, y + groupServicePos);
    }

    /** Adds constraint from vertex. */
    private static void addConstraint(final int x,
                                      final int y,
                                      final int with,
                                      final boolean group,
                                      final int groupService) {
        int groupcor = 0;
        if (group) {
            groupcor = 24;
        }
        moveTo(x + 20, y + 5);
        sleep(1000);
        if (group) {
            rightClickGroup(); /* popup */
        } else {
            rightClick(); /* popup */
        }
        if (groupService >= 0) {
            groupServiceMenu(x, y, groupService);
            final int startBefore = 100 + groupService;
            moveTo(x + 382, y + startBefore);
            moveTo(x + 632, y + startBefore);
            moveTo(x + 632, y + startBefore + with);
        } else {
            moveTo(x + 82, y + 50 + groupcor);
            moveTo(x + 335, y + 50 + groupcor);
            moveTo(x + 335, y + 50 + groupcor + with);
        }
        sleep(6000); /* ptest */
        leftClick(); /* start before */
    }

    /** Adds constraint (order only) from vertex. */
    private static void addConstraintOrderOnly(final int x,
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
        moveTo(x + 20, y + 5);
        sleep(1000);
        if (group) {
            rightClickGroup(); /* popup */
        } else {
            rightClick(); /* popup */
        }
        moveTo(x + 82, y + 50 + groupcor);
        moveTo(x + 335, y + 50 + groupcor);
        moveTo(x + 335, y + 50 + groupcor + skipY + 34);
        moveTo(x + 500 + xCorrection, y + 50 + groupcor + skipY + 34);
        moveTo(x + 520 + xCorrection, y + 50 + groupcor + skipY + 34 + with);
        sleep(6000); /* ptest */
        leftClick(); /* start before */
    }

    /** Adds constraint (colocation only) from vertex. */
    private static void addConstraintColocationOnly(final int x,
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
        moveTo(x + 20, y + 5);
        sleep(1000);
        if (group) {
            rightClickGroup(); /* popup */
        } else {
            rightClick(); /* popup */
        }
        moveTo(x + 82, y + 50 + groupcor);
        sleep(1000);
        moveTo(x + 335, y + 50 + groupcor);
        sleep(1000);
        moveTo(x + 335, y + 50 + groupcor + skipY + 7);
        sleep(1000);
        moveTo(x + 500 + xCorrection, y + 50 + groupcor + skipY + 7);
        sleep(1000);
        moveTo(x + 520 + xCorrection, y + 50 + groupcor + skipY + 7 + with);
        sleep(6000); /* ptest */
        leftClick(); /* start before */
    }

    /** Removes constraint. */
    private static void removeConstraint(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(10000);
        rightClick(); /* constraint popup */
        sleep(2000);
        moveTo(popX + 70, popY + 20);
        sleep(6000); /* ptest */
        leftClick(); /* remove ord */
    }

    /** Removes order. */
    private static void removeOrder(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        moveTo(popX + 70, popY + 50);
        sleep(6000); /* ptest */
        leftClick(); /* remove ord */
    }

    /** Adds order. */
    private static void addOrder(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        moveTo(popX + 70, popY + 50);
        sleep(6000); /* ptest */
        leftClick(); /* add ord */
    }

    /** Removes colocation. */
    private static void removeColocation(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        moveTo(popX + 70, popY + 93);
        sleep(6000); /* ptest */
        leftClick(); /* remove col */
    }

    /** Adds colocation. */
    private static void addColocation(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        moveTo(popX + 70, popY + 90);
        sleep(6000); /* ptest */
        leftClick(); /* add col */
    }

    /** TEST 3. */
    private static void startTest3() {
        slowFactor = 0.3f;
        aborted = false;
        disableStonith();
        for (int i = 20; i > 0; i--) {
            if (i % 5 == 0) {
                info("test3 I: " + i);
            }
            checkTest("test3", 1);
            /* filesystem/drbd */
            moveTo(577, 253);
            rightClick(); /* popup */
            moveTo(709, 278);
            moveTo(894, 283);
            leftClick(); /* choose fs */
            moveTo(1075, 406);
            leftClick(); /* choose drbd */
            moveTo(1043, 444);
            leftClick(); /* choose drbd */
            moveTo(1068, 439);
            leftClick(); /* mount point */
            moveTo(1039, 475);
            leftClick(); /* mount point */

            moveTo(1068, 475);
            leftClick(); /* filesystem type */
            moveTo(1039, 560);
            leftClick(); /* ext3 */

            moveTo(815, 186);
            leftClick(); /* apply */
            sleep(2000);
            checkTest("test3", 2);
            stopEverything();
            checkTest("test3", 3);
            removeEverything();
            resetTerminalAreas();
        }
        System.gc();
    }

    /** Press button. */
    private static void press(final int ke)  {
        if (aborted) {
            return;
        }
        robot.keyPress(ke);
        robot.keyRelease(ke);
        sleepNoFactor(200);
    }

    /** Left click. */
    private static void leftClick()  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        Tools.sleep(400);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        sleepNoFactor(300);
    }

    /** Left press. */
    private static void leftPress()  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        sleepNoFactor(300);
    }

    /** Left release. */
    private static void leftRelease()  {
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        if (aborted) {
            return;
        }
        sleepNoFactor(300);
    }

    /** Right click. */
    private static void rightClick()  {
        if (aborted) {
            return;
        }
        sleepNoFactor(1000);
        robot.mousePress(InputEvent.BUTTON3_MASK);
        sleepNoFactor(500);
        robot.mouseRelease(InputEvent.BUTTON3_MASK);
        sleep(6000);
    }

    /** Right click. */
    private static void rightClickGroup()  {
        if (aborted) {
            return;
        }
        rightClick();
        sleepNoFactor(10000);
    }

    /** Returns true if there is the specified color on this position. */
    private static boolean isColor(final int fromX,
                                   final int fromY,
                                   final Color color,
                                   final boolean expected) {
        if (aborted) {
            return true;
        }
        final int xOffset = getOffset();
        final Point2D appP =
                       Tools.getGUIData().getMainFrame().getLocationOnScreen();
        final int appX = (int) appP.getX() + fromX;
        final int appY = (int) appP.getY() + fromY;
        for (int i = 0; i < 5; i++) {
            boolean isColor = false;
            for (int y = -20; y < 20; y++) {
                if (i > 0) {
                    moveTo(fromX - i, fromY + y);
                }
                if (expected) {
                    if (color.equals(
                          robot.getPixelColor(appX + xOffset - i, appY + y))) {
                        return true;
                    }
                } else {
                    if (color.equals(
                          robot.getPixelColor(appX + xOffset - i, appY + y))) {
                        isColor = true;
                    }
                }
            }
            if (!expected && !isColor) {
                return false;
            }
            Tools.sleep(1000);
        }
        return !expected;
    }

    /** Move to position. */
    private static void moveTo(final int toX, final int toY) {
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
            final int directionX0 = directionX;
            final int directionY0 = directionY;
            robot.mouseMove((int) p.getX() + xOffset + directionX0,
                            (int) p.getY() + directionY0);
            sleep(5);
            if (abortWithMouseMovement()) {
                break;
            }
        }
        final Point2D p = MouseInfo.getPointerInfo().getLocation();
    }

    /** Register movement. */
    public static void registerMovement() {
        Robot rbt = null;
        try {
            rbt = new Robot(SCREEN_DEVICE);
        } catch (final java.awt.AWTException e) {
            Tools.appWarning("Robot error");
        }
        if (rbt == null) {
            return;
        }
        final Robot robot = rbt;
        info("start register movement in 3 seconds");
        sleepNoFactor(3000);
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
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
                    sleepNoFactor(2000);
                    if (newPos.equals(prevP) && !prevPrevP.equals(prevP)) {
                        info("moveTo("
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
                info("stopped movement registering");
            }
        });
        thread.start();
    }

    /** Return vertical position of the blockdevices. */
    private static int getBlockDevY() {
        info("move to position, start in 10 seconds");
        sleepNoFactor(10000);
        final Point2D loc =
           Tools.getGUIData().getMainFrame().getLocationOnScreen();
        final Point2D pos =
                          MouseInfo.getPointerInfo().getLocation();
        final int y = (int) (pos.getY() - loc.getY());
        if (y > 580) {
            return 315;
        }
        return y;
    }

    private static void addDrbdResource(final int blockDevY) {
        moveTo(334, blockDevY); /* add drbd resource */
        rightClick();
        moveTo(342, blockDevY + 6);
        moveTo(667, blockDevY + 7);
        leftClick();
        sleep(20000);
    }

    private static void newDrbdResource() {
        moveTo(720, 570); /* new drbd resource */
        leftClick(); /* next */
        sleep(10000);
    }

    private static void chooseDrbdResourceInterface(final int offset) {
        moveTo(521, 372 + offset); /* interface */
        leftClick();
        moveTo(486, 416 + offset);
        leftClick();
        sleep(1000);
    }

    private static void chooseDrbdResource() {
        chooseDrbdResourceInterface(0);
        chooseDrbdResourceInterface(30);

        moveTo(720, 570);
        leftClick(); /* next */
        sleep(10000);
    }

    private static void addDrbdVolume() {
        moveTo(720, 570); /* volume */
        leftClick(); /* next */
        sleep(10000);
    }

    private static void addBlockDevice() {
        moveTo(720, 570); /* block device */
        leftClick(); /* next */
        sleep(10000);
    }

    private static void addMetaData() {
        moveTo(720, 570); /* meta-data */
        leftClick(); /* next */
        sleep(20000);
    }

    private static void addFileSystem() {
        /* do nothing. */
    }

    private static void removeDrbdVolume() {
        moveTo(480, 250); /* rsc popup */
        rightClick(); /* finish */
        moveTo(555, 340); /* remove */
        leftClick();
        confirmRemove();
    }


    /** DRBD Test 1. */
    private static void startDRBDTest1(final int blockDevY) {
        final String drbdTest = "drbd-test1";
        slowFactor = 0.2f;
        aborted = false;

        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(50000);
        checkDRBDTest(drbdTest, 1);
        addMetaData();
        addFileSystem();
        moveTo(820, 570); /* fs */
        leftClick(); /* finish */
        sleep(10000);
        checkDRBDTest(drbdTest, 1.1);
        removeDrbdVolume();
        checkDRBDTest(drbdTest, 2);

        checkDRBDTest(drbdTest, 2);
    }

    /** DRBD Test 2. */
    private static void startDRBDTest2(final int blockDevY) {
        final String drbdTest = "drbd-test1";
        slowFactor = 0.2f;
        aborted = false;

        info(drbdTest + "/1");
        addDrbdResource(blockDevY);

        moveTo(960, 570);
        leftClick(); /* cancel */
        sleep(60000);

        info(drbdTest + "/2");
        addDrbdResource(blockDevY);
        chooseDrbdResource();

        moveTo(960, 570);
        leftClick(); /* cancel */
        sleep(60000);

        info(drbdTest + "/3");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();

        moveTo(960, 570);
        leftClick(); /* cancel */
        confirmRemove();
        sleep(60000);

        info(drbdTest + "/4");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();

        moveTo(960, 570);
        leftClick(); /* cancel */
        confirmRemove();
        sleep(60000);

        info(drbdTest + "/5");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(50000);
        checkDRBDTest(drbdTest, 1);

        moveTo(960, 570);
        leftClick(); /* cancel */
        confirmRemove();
        sleep(60000);

        info(drbdTest + "/6");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(50000);
        checkDRBDTest(drbdTest, 1);
        addMetaData();

        moveTo(960, 570);
        leftClick(); /* cancel */
        confirmRemove();
        sleep(60000);

        info(drbdTest + "/7");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(50000);
        checkDRBDTest(drbdTest, 1);
        addMetaData();
        addFileSystem();
        checkDRBDTest(drbdTest, 1.1);
        sleep(20000);

        moveTo(960, 570);
        sleep(2000);
        leftClick(); /* cancel */
        sleep(20000);
        confirmRemove();
        sleep(60000);

        info(drbdTest + "/8");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(50000);
        checkDRBDTest(drbdTest, 1);
        addMetaData();
        addFileSystem();
        moveTo(820, 570); /* fs */
        leftClick(); /* finish */
        sleep(10000);
        checkDRBDTest(drbdTest, 1.1);

        removeDrbdVolume();
        checkDRBDTest(drbdTest, 2);
    }

    /** DRBD Test 3. */
    private static void startDRBDTest3(final int blockDevY) {
        /* Two drbds. */
        final String drbdTest = "drbd-test3";
        slowFactor = 0.2f;
        aborted = false;
        int protocolY = 600;
        int correctionY = 0;
        if (!cluster.getHostsArray()[0].hasVolumes()) {
            protocolY = 400;
            correctionY = 30;
        }
        int offset = 0;
        for (int i = 0; i < 2; i++) {
            addDrbdResource(blockDevY + offset);
            if (i == 1 && cluster.getHostsArray()[0].hasVolumes()) {
                newDrbdResource();
            }
            chooseDrbdResource();

            addDrbdVolume();
            addBlockDevice();
            addBlockDevice();
            sleep(50000);

            if (offset == 0) {
                checkDRBDTest(drbdTest, 1.1);
            } else {
                checkDRBDTest(drbdTest, 1.2);
            }
            sleep(10000);
            addMetaData();
            addFileSystem();
            moveTo(820, 570); /* fs */
            leftClick(); /* finish */
            sleep(10000);

            if (offset == 0) {
                checkDRBDTest(drbdTest, 1);
            }
            offset += 40;
        }
        checkDRBDTest(drbdTest, 2);

        moveTo(480, 250); /* select r0 */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo(1073, protocolY); /* select protocol */
        leftClick();
        press(KeyEvent.VK_UP); /* protocol b */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(1075, 423 + correctionY); /* select fence peer */
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_DOWN); /* select dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(970, 480 + correctionY); /* wfc timeout */
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_9);
        sleep(2000);

        moveTo(814, 189);
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(); /* apply */
        checkDRBDTest(drbdTest, 2.1); /* 2.1 */


        /* common */
        moveTo(500, 390); /* select background */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo(970, 383); /* wfc timeout */
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_3);
        sleep(2000);

        moveTo(814, 189);
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(); /* apply */
        sleep(10000);
        checkDRBDTest(drbdTest, 2.11); /* 2.11 */
        moveTo(970, 383); /* wfc timeout */
        sleep(6000);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);
        moveTo(814, 189);
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(); /* apply */

        /* resource */
        moveTo(480, 250); /* select r0 */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo(1073, protocolY); /* select protocol */
        leftClick();
        press(KeyEvent.VK_DOWN); /* protocol c */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(1075, 423 + correctionY); /* select fence peer */
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_UP); /* deselect dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(970, 480 + correctionY); /* wfc timeout */
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_5);
        sleep(2000);

        moveTo(814, 189);
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(); /* apply */
        checkDRBDTest(drbdTest, 2.2); /* 2.2 */

        moveTo(970, 480 + correctionY); /* wfc timeout */
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);

        moveTo(814, 189);
        sleep(6000); /* test */
        leftClick(); /* apply */
        checkDRBDTest(drbdTest, 2.3); /* 2.3 */

        moveTo(480, 250); /* rsc popup */
        rightClick();
        moveTo(555, 340); /* remove */
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 3);
        moveTo(480, 250); /* rsc popup */
        rightClick();
        moveTo(555, 340); /* remove */
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 4);
    }

    /** DRBD Test 4. */
    private static void startDRBDTest4(final int blockDevY) {
        final String drbdTest = "drbd-test4";
        /* Two drbds. */
        slowFactor = 0.2f;
        aborted = false;
        int offset = 0;
        int protocolY = 600;
        int correctionY = 0;
        if (!cluster.getHostsArray()[0].hasVolumes()) {
            protocolY = 400;
            correctionY = 30;
        }
        for (int i = 0; i < 2; i++) {
            addDrbdResource(blockDevY + offset);
            if (i == 0) {
                /* first one */
                chooseDrbdResource();
            } else {
                /* existing drbd resource */
                moveTo(460, 400);
                leftClick();
                moveTo(460, 440);
                leftClick(); /* drbd: r0 */
                moveTo(720, 570);
                leftClick(); /* next */
                sleep(10000);

                //addDrbdVolume();
            }
            addDrbdVolume();

            addBlockDevice();
            addBlockDevice();
            sleep(50000);
            if (offset == 0) {
                checkDRBDTest(drbdTest, 1.1);
            } else {
                checkDRBDTest(drbdTest, 1.2);
            }
            sleep(10000);

            addMetaData();
            addFileSystem();
            moveTo(820, 570); /* fs */
            leftClick(); /* finish */
            sleep(10000);

            if (offset == 0) {
                checkDRBDTest(drbdTest, 1);
            }
            offset += 40;
        }
        checkDRBDTest(drbdTest, 2);

        moveTo(480, 250); /* select r0 */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo(1073, protocolY); /* select protocol */
        leftClick();
        press(KeyEvent.VK_UP); /* protocol b */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(1075, 423 + correctionY); /* select fence peer */
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_DOWN); /* select dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(970, 480 + correctionY); /* wfc timeout */
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_9);
        sleep(2000);

        moveTo(814, 189);
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(); /* apply */
        checkDRBDTest(drbdTest, 2.1); /* 2.1 */


        /* common */
        moveTo(500, 390); /* select background */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo(970, 383); /* wfc timeout */
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_3);
        sleep(2000);

        moveTo(814, 189);
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(); /* apply */
        sleep(10000);
        checkDRBDTest(drbdTest, 2.11); /* 2.11 */
        moveTo(970, 383); /* wfc timeout */
        sleep(6000);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);
        moveTo(814, 189);
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(); /* apply */

        /* resource */
        moveTo(480, 250); /* select r0 */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo(1073, protocolY); /* select protocol */
        leftClick();
        press(KeyEvent.VK_DOWN); /* protocol c */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(1075, 423 + correctionY); /* select fence peer */
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_UP); /* deselect dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(970, 480 + correctionY); /* wfc timeout */
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_5);
        sleep(2000);

        moveTo(814, 189);
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(); /* apply */
        checkDRBDTest(drbdTest, 2.2); /* 2.2 */

        moveTo(970, 480 + correctionY); /* wfc timeout */
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);

        moveTo(814, 189);
        sleep(6000); /* test */
        leftClick(); /* apply */
        checkDRBDTest(drbdTest, 2.3); /* 2.3 */

        moveTo(480, 250); /* rsc popup */
        rightClick();
        moveTo(555, 340); /* remove */
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 3);
        moveTo(480, 250); /* rsc popup */
        rightClick();
        moveTo(555, 340); /* remove */
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 4);
    }

    /** VM Test 1. */
    private static void startVMTest1(final String vmTest, final int count) {
        slowFactor = 0.2f;
        aborted = false;
        String name = "dmc";
        final int count2 = 3;
        for (int j = 0; j < count; j++) {
            checkVMTest(vmTest, 1, name);
            name += "i";
        }
        name = "dmc";
        final List<String> names = new ArrayList<String>();

        for (int j = 0; j < count; j++) {
            moveTo(56, 252); /* popup */
            rightClick();
            moveTo(159, 271); /* new domain */
            leftClick();
            moveTo(450, 380); /* domain name */
            leftClick();
            press(KeyEvent.VK_D);
            sleep(200);
            press(KeyEvent.VK_M);
            sleep(200);
            press(KeyEvent.VK_C);
            sleep(200);
            for (int k = 0; k < j; k++) {
                press(KeyEvent.VK_I); /* dmci, dmcii, etc. */
                sleep(200);
            }
            moveTo(730, 570);
            leftClick();
            //press(KeyEvent.VK_ENTER);

            moveTo(593, 440); /* source file */
            sleep(2000);
            leftClick();
            sleep(2000);
            press(KeyEvent.VK_T);
            sleep(200);
            press(KeyEvent.VK_E);
            sleep(200);
            press(KeyEvent.VK_S);
            sleep(200);
            press(KeyEvent.VK_T);
            sleep(2000);
            for (int i = 0; i < count2; i++) {
                moveTo(600, 375); /* disk/block device */
                leftClick();
                sleep(1000);
                moveTo(430, 375); /* image */
                leftClick();
                sleep(1000);
            }

            moveTo(730, 570);
            leftClick();
            //press(KeyEvent.VK_ENTER);
            sleep(5000);
            moveTo(730, 570);
            leftClick();
            //press(KeyEvent.VK_ENTER); /* storage */
            sleep(5000);
            for (int i = 0; i < count2; i++) {
                moveTo(600, 375); /* bridge */
                leftClick();
                sleep(1000);
                moveTo(430, 375); /* network */
                leftClick();
                sleep(1000);
            }
            moveTo(730, 570);
            leftClick();
            //press(KeyEvent.VK_ENTER); /* network */
            sleep(5000);
            for (int i = 0; i < count2; i++) {
                moveTo(600, 375); /* sdl */
                leftClick();
                sleep(1000);
                moveTo(430, 375); /* vnc */
                leftClick();
                sleep(1000);
            }
            moveTo(730, 570);
            leftClick();
            //press(KeyEvent.VK_ENTER); /* display */
            sleep(5000);

            final int yMoreHosts = 30 * (cluster.getHosts().size() - 1);
            moveTo(530, 410 + yMoreHosts); /* create config */
            leftClick();
            checkVMTest(vmTest, 2, name);

            if (cluster.getHosts().size() > 1) {
                /* two hosts */
                moveTo(410, 370); /* deselect first */
                leftClick();
                moveTo(560, 410 + yMoreHosts); /* create config */
                leftClick();
                checkVMTest(cluster.getHostsArray()[0], vmTest, 1, name);
                checkVMTest(cluster.getHostsArray()[1], vmTest, 2, name);

                moveTo(410, 370); /* select first */
                leftClick();
                moveTo(410, 405); /* deselect second */
                leftClick();
                moveTo(560, 410 + yMoreHosts); /* create config */
                leftClick();
                checkVMTest(cluster.getHostsArray()[0], vmTest, 2, name);
                checkVMTest(cluster.getHostsArray()[1], vmTest, 1, name);

                moveTo(410, 405); /* select second */
                leftClick();
                moveTo(560, 410 + yMoreHosts); /* create config */
                leftClick();
                checkVMTest(vmTest, 2, name);
            }

            sleepNoFactor(2000);
            moveTo(814, 570); /* finish */
            leftClick();
            sleepNoFactor(5000);

            moveTo(620, 480 + yMoreHosts); /* number of cpus */
            sleep(1000);
            leftClick();
            sleep(500);
            press(KeyEvent.VK_BACK_SPACE);
            sleep(500);
            press(KeyEvent.VK_2);
            sleep(500);
            moveTo(250, 190); /* apply */
            sleep(1000);
            leftClick();
            sleep(1000);
            checkVMTest(vmTest, 3, name);

            /* disk readonly */
            moveTo(56, 252); /* popup */
            leftClick();
            sleep(1000);
            press(KeyEvent.VK_DOWN);
            sleep(200);
            press(KeyEvent.VK_DOWN);
            for (int down = 0; down < j; down++) {
                sleep(200);
                press(KeyEvent.VK_DOWN);
            }

            //moveTo(100, 280 + j * 18); /* choose disk */
            //sleep(1000);
            //leftClick();
            //moveTo(1100, 298);
            //leftPress(); /* scroll bar */
            //moveTo(1100, 410);
            //leftRelease();

            //sleep(1000);
            //moveTo(400, 450 + yMoreHosts); /* choose disk */
            //sleep(1000);
            //leftClick();
            //sleep(1000);

            moveTo(390, 540); /* readonly */
            sleep(1000);
            leftClick();
            sleep(1000);
            moveTo(250, 190); /* apply */
            sleep(1000);
            leftClick();
            checkVMTest(vmTest, 3.1, name);
            sleep(1000);
            moveTo(390, 645); /* readonly */
            sleep(1000);
            leftClick();

            sleep(1000);
            moveTo(950, 180); /* host overview */
            sleep(1000);
            leftClick();
            sleep(1000);

            moveTo(250, 190); /* host apply */
            leftClick();
            checkVMTest(vmTest, 3.2, name);

            /* remove interface */

            // ...
            //moveTo(1100, 410);
            //leftPress(); /* scroll bar back */
            //moveTo(1100, 200);
            //leftRelease();

            names.add(name);
            for (final String n : names) {
                checkVMTest(vmTest, 3, n);
            }
            name += "i";
        }

        sleepNoFactor(2000);
        moveTo(1066, 284); /* remove */
        leftClick();
        sleepNoFactor(2000);
        moveTo(516, 476); /* confirm */
        leftClick();
        sleepNoFactor(5000);
        for (int j = 1; j < count; j++) {
            moveTo(1066, 225); /* remove */
            leftClick();
            sleepNoFactor(2000);
            moveTo(516, 476); /* confirm */
            leftClick();
            sleepNoFactor(5000);
        }
    }

    /** Cluster wizard locked until focus is lost. */
    private static void startVMTest2(final String vmTest, final int count) {
        slowFactor = 0.1f;
        aborted = false;
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                info("vm-test2 I: " + i);
            }
            moveTo(330, 170); /* new VM */
            sleep(500);
            leftClick();
            sleep(1000);
            if (!isColor(480, 370, new Color(255, 100, 100), true)) {
                info("vm-test2 1: failed");    
                break;
            }
            boolean ok = false;
            for (int error = 0; error < 5; error++) {
                sleep(100);
                press(KeyEvent.VK_X);
                if (!isColor(480, 370, new Color(255, 100, 100), false)) {
                    sleepNoFactor(1000);
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                info("vm-test2 2: failed");    
                break;
            }
            moveTo(910 , 565); /* cancel */
            sleep(500);
            leftClick();
            sleep(1000);
        }
    }

    private static void saveAndExit() {
        Tools.save(Tools.getConfigData().getSaveFile());
        sleepNoFactor(10000);
        System.exit(0);
    }

    private static void resetTerminalAreas() {
        for (final Host h : cluster.getHosts()) {
            if (!aborted) {
                h.getTerminalPanel().resetTerminalArea();
            }
        }
    }

    public static void info(final String text) {
        if (cluster != null) {
            for (final Host h : cluster.getHosts()) {
                h.getTerminalPanel().addCommandOutput(text + "\n");
            }
        }
        Tools.info(text);
    }
}
