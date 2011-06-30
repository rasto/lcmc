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
                    Tools.info("sleep: " + x);
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
                final Robot robot = rbt;
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
                Tools.info("click test done");
            }
        });
        thread.start();
    }

    /** Starts automatic mouse mover in 10 seconds. */
    public static void startMover(final int duration,
                                  final boolean withClicks) {
        aborted = false;
        slowFactor = 0.3f;
        Tools.info("start mouse move test in 10 seconds");
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
                final Robot robot = rbt;
                final int xOffset = getOffset();
                final Point2D origP = MouseInfo.getPointerInfo().getLocation();
                final int origX = (int) (origP.getX() - locP.getX());
                final int origY = (int) (origP.getY() - locP.getY());
                Tools.info("move mouse to the end position");
                sleepNoFactor(5000);
                final Point2D endP = MouseInfo.getPointerInfo().getLocation();
                final int endX = (int) (endP.getX() - locP.getX());
                final int endY = (int) (endP.getY() - locP.getY());
                int destX = origX;
                int destY = origY;
                Tools.info("test started");
                final long startTime = System.currentTimeMillis();
                int i = 1;
                while (!aborted) {
                    moveTo(robot, destX, destY);
                    if (abortWithMouseMovement()) {
                        break;
                    }
                    if (withClicks) {
                        leftClick(robot);
                        sleepNoFactor(1000);
                    }
                    moveTo(robot, endX, endY);
                    if (abortWithMouseMovement()) {
                        break;
                    }
                    if (withClicks) {
                        leftClick(robot);
                        sleepNoFactor(1000);
                    }
                    System.out.println("mouse move test: " + i);
                    i++;
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
    public static void startTest(final String index, final Cluster cluster) {
        aborted = false;
        Tools.info("start test " + index + " in 3 seconds");
        for (final Host host : cluster.getHosts()) {
            host.getSSH().installTestFiles();
        }
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                sleepNoFactor(3000);
                Robot robot = null;
                try {
                    robot = new Robot(SCREEN_DEVICE);
                } catch (final java.awt.AWTException e) {
                    Tools.appWarning("Robot error");
                }
                if (robot == null) {
                    return;
                }
                final String selected =
                         cluster.getBrowser().getTree()
                                   .getLastSelectedPathComponent().toString();
                if ("Services".equals(selected)
                    || Tools.getString("ClusterBrowser.ClusterManager").equals(
                                                                   selected)) {
                    if ("0".equals(index)) {
                        /* all pacemaker tests */
                        int i = 1;
                        while (true) {
                            final long startTime = System.currentTimeMillis();
                            Tools.info("test" + index + " no " + i);
                            startTest1(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTest2(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTest3(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTest4(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTest5(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTest6(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTest7(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTest8(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            //startTest9(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTestA(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTestB(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTestC(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            startTestD(robot, cluster);
                            if (aborted) {
                                break;
                            }
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            i++;
                        }
                    } else if ("1".equals(index)) {
                        /* pacemaker */
                        int i = 1;
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            Tools.info("test" + index + " no " + i);
                            startTest1(robot, cluster);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            resetTerminalAreas(cluster);
                            i++;
                        }
                    } else if ("2".equals(index)) {
                        /* resource sets */
                        int i = 1;
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            Tools.info("test" + index + " no " + i);
                            startTest2(robot, cluster);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            resetTerminalAreas(cluster);
                            i++;
                        }
                    } else if ("3".equals(index)) {
                        /* pacemaker drbd */
                        final int i = 1;
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index + " no " + i);
                        startTest3(robot, cluster);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + " no " + i + ", secs: "
                                   + secs);
                    } else if ("4".equals(index)) {
                        /* placeholders 6 dummies */
                        int i = 1;
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            Tools.info("test" + index + " no " + i);
                            startTest4(robot, cluster);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            i++;
                        }
                    } else if ("5".equals(index)) {
                        int i = 1;
                        while (!aborted) {
                            /* pacemaker */
                            final long startTime = System.currentTimeMillis();
                            Tools.info("test" + index + " no " + i);
                            startTest5(robot, cluster);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            i++;
                        }
                    } else if ("6".equals(index)) {
                        int i = 1;
                        while (!aborted) {
                            /* pacemaker */
                            final long startTime = System.currentTimeMillis();
                            Tools.info("test" + index + " no " + i);
                            startTest6(robot, cluster);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            i++;
                        }
                    } else if ("7".equals(index)) {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index);
                        startTest7(robot, cluster);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + ", secs: "
                                   + secs);
                    } else if ("8".equals(index)) {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index);
                        startTest8(robot, cluster);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + ", secs: "
                                   + secs);
                    } else if ("a".equals(index)) {
                        /* pacemaker leak test group */
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index);
                        startTestA(robot, cluster);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + ", secs: "
                                   + secs);
                    } else if ("b".equals(index)) {
                        /* pacemaker leak test clone */
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index);
                        startTestB(robot, cluster);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + ", secs: "
                                   + secs);
                    } else if ("c".equals(index)) {
                        /* pacemaker master/slave test */
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index);
                        startTestC(robot, cluster);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + ", secs: "
                                   + secs);
                    } else if ("d".equals(index)) {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index);
                        startTestD(robot, cluster);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + ", secs: "
                                   + secs);
                    } else if ("e".equals(index)) {
                        /* host wizard deadlock */
                        final long startTime = System.currentTimeMillis();
                        Tools.info("test" + index);
                        startTestE(robot, cluster);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        Tools.info("test" + index + ", secs: "
                                   + secs);
                    } else if ("f".equals(index)) {
                        int i = 1;
                        while (!aborted) {
                            /* cloned group */
                            final long startTime = System.currentTimeMillis();
                            Tools.info("test" + index + " no " + i);
                            startTestF(robot, cluster);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
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
                            Tools.info("test" + index + " no " + i);
                            startDRBDTest1(robot, cluster, blockDevY);
                            if (aborted) {
                                break;
                            }
                            startDRBDTest2(robot, cluster, blockDevY);
                            if (aborted) {
                                break;
                            }
                            startDRBDTest3(robot, cluster, blockDevY);
                            if (aborted) {
                                break;
                            }
                            if (cluster.getHostsArray()[0].hasVolumes()) {
                                startDRBDTest4(robot, cluster, blockDevY);
                                if (aborted) {
                                    break;
                                }
                            }
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            resetTerminalAreas(cluster);
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
                            Tools.info("test" + index + " no " + i);
                            startDRBDTest1(robot, cluster, blockDevY);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            resetTerminalAreas(cluster);
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
                            Tools.info("test" + index + " no " + i);
                            startDRBDTest2(robot, cluster, blockDevY);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            resetTerminalAreas(cluster);
                            i++;
                        }
                    } else if ("3".equals(index)) {
                        /* DRBD 2 resoruces */
                        int i = 1;
                        final int blockDevY = getBlockDevY();
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            Tools.info("test" + index + " no " + i);
                            startDRBDTest3(robot, cluster, blockDevY);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            resetTerminalAreas(cluster);
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
                            Tools.info("test" + index + " no " + i);
                            startDRBDTest4(robot, cluster, blockDevY);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            resetTerminalAreas(cluster);
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
                            Tools.info("test" + index + " no " + i);
                            startVMTest1("vm-test" + index, robot, cluster, 2);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            Tools.info("test" + index + " no " + i + ", secs: "
                                       + secs);
                            resetTerminalAreas(cluster);
                            i++;
                        }
                    }
                }
                Tools.info(selected + " test " + index + " done");
            }
        });
        thread.start();
    }

    /** Check test. */
    private static void checkTest(final Cluster cluster,
                                  final String test,
                                  final double no) {
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
    private static void checkDRBDTest(final Cluster cluster,
                                      final String test,
                                      final double no) {
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
    private static void checkVMTest(final Cluster cluster,
                                    final String test,
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
    private static void startTest1(final Robot robot, final Cluster cluster) {
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
        disableStonith(robot, cluster);
        checkTest(cluster, "test1", 1);
        enableStonith(robot, cluster);
        checkTest(cluster, "test1", 1.1);
        disableStonith(robot, cluster);
        checkTest(cluster, "test1", 1);
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
        moveTo(robot, 1044, 450);
        leftClick(robot); /* choose */
        sleep(1000);
        press(robot, KeyEvent.VK_1);
        press(robot, KeyEvent.VK_0);
        press(robot, KeyEvent.VK_0);
        sleep(1000);
        setTimeouts(robot, false);
        moveTo(robot, 814, 189);
        sleep(6000); /* ptest */
        leftClick(robot); /* apply */
        /* CIDR netmask 24 */
        sleep(10000);
        moveTo(robot, 335, 129); /* advanced */
        sleep(2000);
        leftClick(robot);
        sleep(2000);

        moveTo(robot, 960, 475); /* CIDR */
        sleep(3000);
        leftClick(robot);
        sleep(2000);
        press(robot, KeyEvent.VK_2);
        sleep(200);
        press(robot, KeyEvent.VK_4);
        sleep(1000);

        moveTo(robot, 335, 129); /* not advanced */
        sleep(2000);
        leftClick(robot);
        sleep(2000);

        moveTo(robot, 814, 189);
        sleep(6000); /* ptest */
        leftClick(robot); /* apply */

        checkTest(cluster, "test1", 2); /* 2 */

        /* pingd */
        moveTo(robot, 1100, 298);
        leftPress(robot); /* scroll bar */
        moveTo(robot, 1100, 510);
        leftRelease(robot);
        moveTo(robot, 1076, 387);
        leftClick(robot);
        moveTo(robot, 1037, 454);
        leftClick(robot); /* no ping */
        moveTo(robot, 809, 192); /* ptest */
        sleep(2000);
        leftClick(robot); /*  apply */
        sleep(2000);
        checkTest(cluster, "test1", 2.1); /* 2.1 */

        moveTo(robot, 1076, 387);
        leftClick(robot);
        moveTo(robot, 1067, 412);
        leftClick(robot); /* default */
        moveTo(robot, 809, 192); /* ptest */
        sleep(2000);
        leftClick(robot); /*  apply */

        moveTo(robot, 1100, 298);
        leftPress(robot); /* scroll bar */
        moveTo(robot, 1100, 550);
        leftRelease(robot);

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
        moveTo(robot, gx + 580, gy + 22);
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
        moveTo(robot, gx + 580, gy + 22);
        sleep(1000);
        typeDummy(robot);
        sleep(1000);

        moveTo(robot, 125, 320);
        sleep(1000);
        rightClick(robot);
        sleep(1000);
        moveTo(robot, 150, 611);
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
        moveTo(robot, gx + 580, gy + 22);
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
        checkTest(cluster, "test1", 2); /* 2 */
        rightClick(robot); /* group popup */
        moveTo(robot, gx + 80, gy + 20);
        moveTo(robot, gx + 84, gy + 22);
        moveTo(robot, gx + 580, gy + 22);
        sleep(1000);
        typeDummy(robot);
        sleep(1000);
        setTimeouts(robot, true);
        moveTo(robot, 809, 192); /* ptest */
        sleep(2000);
        leftClick(robot); /*  apply */
        for (int i = 0; i < 2; i++) {
            /* another group resource */
            sleep(20000);
            moveTo(robot, gx + 46, gy - 19);
            rightClick(robot); /* group popup */
            sleep(2000 + i * 500);
            moveTo(robot, gx + 80, gy - 10);
            moveTo(robot, gx + 84, gy - 8);
            moveTo(robot, gx + 580, gy - 8);
            sleep(1000);
            typeDummy(robot);
            sleep(i * 300);
            setTimeouts(robot, true);
            moveTo(robot, 809, 192); /* ptest */
            sleep(6000);
            leftClick(robot); /* apply */
            sleep(1000);
        }
        sleep(4000);
        checkTest(cluster, "test1", 3); /* 3 */
        /* constraints */
        addConstraint(robot, gx, gy - 30, 0, true, -1);
        checkTest(cluster, "test1", 3.1); /* 3.1 */

        /* move up, move down */
        for (int i = 0; i < 2; i++) {
            moveTo(robot, 137, 344);
            rightClick(robot);
            sleep(1000);
            moveTo(robot, 221, 493);
            leftClick(robot); /* move res 3 up */
            sleepNoFactor(10000);
            checkTest(cluster, "test1", 3.11); /* 3.11 */
            moveTo(robot, 137, 328);
            rightClick(robot);
            moveTo(robot, 236, 515);
            leftClick(robot); /* move res 3 down */
            sleepNoFactor(10000);
            checkTest(cluster, "test1", 3.12); /* 3.12 */
        }

        /* same as */
        moveTo(robot, 125, 345);
        sleep(1000);
        leftClick(robot);

        moveTo(robot, 1100, 298);
        leftPress(robot); /* scroll bar */
        moveTo(robot, 1100, 510);
        leftRelease(robot);

        for (int i = 0; i < 2; i++) {
            sleep(1000);
            moveTo(robot, 1073, 360);
            sleep(20000);
            leftClick(robot);
            sleep(1000);
            moveTo(robot, 1073, 410);
            sleep(1000);
            leftClick(robot); /* choose another dummy */
            sleep(1000);
            moveTo(robot, 809, 192); /* ptest */
            sleep(4000);
            leftClick(robot); /* apply */
            sleep(4000);
            checkTest(cluster, "test1", 3.2); /* 3.2 */

            moveTo(robot, 1073 , 360);
            sleep(30000);
            leftClick(robot);
            sleep(1000);
            moveTo(robot, 1073 , 380);
            leftClick(robot); /* choose "nothing selected */
            sleep(1000);
            moveTo(robot, 809, 192); /* ptest */
            sleep(4000);
            leftClick(robot); /* apply */
            sleep(9000);
            checkTest(cluster, "test1", 4); /* 4 */
            sleep(20000);
        }
        moveTo(robot, 1100, 298);
        leftPress(robot); /* scroll bar back */
        moveTo(robot, 1100, 150);
        leftRelease(robot);

        /* locations */
        moveTo(robot, ipX + 20, ipY);
        leftClick(robot); /* choose ip */
        setLocation(robot, new Integer[]{KeyEvent.VK_I});
        sleep(3000);
        checkTest(cluster, "test1", 4.1); /* 4.1 */

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
        checkTest(cluster, "test1", 4.2); /* 4.2 */

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
        checkTest(cluster, "test1", 4.3); /* 4.3 */

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
        checkTest(cluster, "test1", 4.4); /* 4.4 */
        for (int i = 0; i < 3; i++) {
            removeConstraint(robot, popX, popY);
            checkTest(cluster, "test1", 5); /* 5 */
            addConstraint(robot, gx, gy - 30, 0, true, -1);
            checkTest(cluster, "test1", 4.4); /* 4.4 */
        }

        removeConstraint(robot, popX, popY);
        sleep(3000);
        checkTest(cluster, "test1", 5); /* 5 */
        sleep(1000);

        addConstraint(robot, gx, gy - 30, 9, true, -1);
        sleep(5000);
        checkTest(cluster, "test1", 6); /* 6 */

        removeOrder(robot, popX, popY);
        sleep(4000);
        checkTest(cluster, "test1", 7);

        addOrder(robot, popX, popY);
        sleep(4000);
        checkTest(cluster, "test1", 8);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 9);

        addColocation(robot, popX, popY);
        sleep(4000);
        checkTest(cluster, "test1", 10);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.1);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.2);

        addConstraintOrderOnly(robot, gx, gy - 30, 0, 25, 0, true, -1);
        sleep(4000);
        checkTest(cluster, "test1", 10.3);

        addColocation(robot, popX, popY);
        sleep(4000);
        checkTest(cluster, "test1", 10.4);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.5);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.6);

        addConstraintColocationOnly(robot, gx, gy - 30, 0, 25, 0, true, -1);
        sleep(4000);
        checkTest(cluster, "test1", 10.7);

        addOrder(robot, popX, popY);
        sleep(4000);
        checkTest(cluster, "test1", 10.8);

        removeConstraint(robot, popX, popY);
        sleep(4000);
        checkTest(cluster, "test1", 10.9);

        addConstraint(robot, ipX, ipY, 0, false, -1);
        sleep(5000);
        checkTest(cluster, "test1", 10.91);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.92);

        addOrder(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.93);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.94);

        addColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.95);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.96);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.97);

        addConstraintColocationOnly(robot, ipX, ipY, -20, 100, 0, false, -1);
        sleep(5000);
        checkTest(cluster, "test1", 10.98);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 10.99);

        addConstraintOrderOnly(robot, ipX, ipY, -20, 100, 0, false, -1);
        sleep(5000);
        checkTest(cluster, "test1", 11);

        addColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.1);

        removeConstraint(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.2);

        addConstraint(robot, ipX, ipY, 60, false, -1);
        sleep(5000);
        checkTest(cluster, "test1", 11.3);
        stopResource(robot, ipX, ipY, 0);
        sleep(5000);
        checkTest(cluster, "test1", 11.4);
        resetStartStopResource(robot, ipX, ipY);
        sleep(5000);
        checkTest(cluster, "test1", 11.5);

        moveTo(robot, ipX + 20, ipY + 10);
        leftClick(robot); /* choose ip */
        stopResource(robot, 1010, 180, 10); /* actions menu stop */
        sleep(5000);
        checkTest(cluster, "test1", 11.501);

        moveTo(robot, ipX + 20, ipY + 10);
        leftClick(robot); /* choose ip */
        startResource(robot, 1010, 180, 20); /* actions menu start */
        sleep(5000);
        checkTest(cluster, "test1", 11.502);

        resetStartStopResource(robot, ipX, ipY);
        sleep(5000);
        checkTest(cluster, "test1", 11.5);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.51);

        addColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.52);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.53);

        addOrder(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.54);

        removeColocation(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.55);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.56);

        addConstraintOrderOnly(robot, ipX, ipY, -20, 100, 55, false, -1);
        sleep(5000);
        checkTest(cluster, "test1", 11.57);

        removeOrder(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.58);

        addConstraintColocationOnly(robot, ipX, ipY, -20, 100, 55, false, -1);
        sleep(5000);
        checkTest(cluster, "test1", 11.59);

        addOrder(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.6);

        removeConstraint(robot, popX, popY);
        sleep(5000);
        checkTest(cluster, "test1", 11.7);

        addConstraint(robot, gx, gy - 30, 9, true, -1);
        sleep(5000);
        checkTest(cluster, "test1", 11.8);
        /** Add m/s Stateful resource */
        moveTo(robot, statefulX, statefulY);
        rightClick(robot); /* popup */
        moveTo(robot, statefulX + 137, statefulY + 8);
        moveTo(robot, statefulX + 137, statefulY + 28);
        moveTo(robot, statefulX + 412, statefulY + 27);
        moveTo(robot, statefulX + 432, statefulY + 70);
        moveTo(robot, statefulX + 665, statefulY + 70);
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
        sleep(5000);
        leftClick(robot); /* apply */
        checkTest(cluster, "test1", 11.9);
        sleep(3000);
        /* set clone max to 1 */
        moveTo(robot, 978, 381);
        sleep(3000);
        leftClick(robot); /* Clone Max */
        sleep(3000);
        leftClick(robot);
        sleep(3000);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(3000);
        press(robot, KeyEvent.VK_1);
        sleep(3000);
        setTimeouts(robot, false);
        moveTo(robot, 812, 179);
        sleep(3000);
        leftClick(robot); /* apply */
        sleep(3000);
        checkTest(cluster, "test1", 12);
        stopResource(robot, statefulX, statefulY, 0);
        sleep(3000);
        checkTest(cluster, "test1", 13);

        startResource(robot, statefulX, statefulY, 0);
        sleep(10000);
        checkTest(cluster, "test1", 14);
        unmanageResource(robot, statefulX, statefulY, 0);
        sleep(3000);
        checkTest(cluster, "test1", 15);
        manageResource(robot, statefulX, statefulY, 0);
        sleep(3000);
        checkTest(cluster, "test1", 16);

        /* IP addr cont. */
        stopResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(cluster, "test1", 17);
        startResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(cluster, "test1", 18);
        unmanageResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(cluster, "test1", 19);
        manageResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(cluster, "test1", 20);
        migrateResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(cluster, "test1", 21);
        unmigrateResource(robot, ipX, ipY, 0);
        sleep(3000);
        checkTest(cluster, "test1", 22);

        /* Group cont. */
        stopResource(robot, gx, gy - 30, 15);
        sleep(10000);
        checkTest(cluster, "test1", 23);
        startResource(robot, gx, gy - 30, 15);
        sleep(8000);
        checkTest(cluster, "test1", 24);
        unmanageResource(robot, gx, gy - 30, 15);
        sleep(5000);
        checkTest(cluster, "test1", 25);
        manageResource(robot, gx, gy - 30, 15);
        sleep(5000);
        checkTest(cluster, "test1", 26);
        migrateResource(robot, gx, gy - 30, 25);
        sleep(5000);
        moveTo(robot, gx, gy);
        leftClick(robot);
        checkTest(cluster, "test1", 27);
        unmigrateResource(robot, 1020, 180, 55); /* actions menu unmigrate */
        sleep(5000);
        checkTest(cluster, "test1", 28);
        stopResource(robot, ipX, ipY, 0);
        sleep(5000);
        moveTo(robot, gx, gy);
        leftClick(robot);
        stopGroup(robot, 1020, 180, 25); /* actions menu stop */
        sleep(5000);
        stopGroup(robot, statefulX, statefulY, 0);
        stopEverything(robot); /* to be sure */
        sleep(5000);
        checkTest(cluster, "test1", 29);

        if (true) {
            removeResource(robot, ipX, ipY, -15);
            sleep(5000);
            removeGroup(robot, gx, gy - 20, 0);
            sleep(5000);
            removeGroup(robot, statefulX, statefulY, -15);
        } else {
            removeEverything(robot);
        }
        if (!aborted) {
            sleepNoFactor(20000);
        }
        checkTest(cluster, "test1", 1);
    }


    /** Stop everything. */
    private static void stopEverything(final Robot robot) {
        sleep(10000);
        moveTo(robot, 335, 129); /* advanced */
        sleep(2000);
        leftClick(robot);
        sleep(2000);
        moveTo(robot, 700, 568);
        rightClick(robot); /* popup */
        sleep(3000);
        moveTo(robot, 760, 644);
        sleep(3000);
        leftClick(robot);
        moveTo(robot, 335, 129); /* not advanced */
        sleep(2000);
        leftClick(robot);
        sleep(2000);
    }

    /** Remove everything. */
    private static void removeEverything(final Robot robot) {
        sleep(10000);
        moveTo(robot, 335, 129); /* advanced */
        sleep(2000);
        leftClick(robot);
        sleep(2000);
        moveTo(robot, 700, 568);
        rightClick(robot); /* popup */
        sleep(3000);
        moveTo(robot, 760, 674);
        sleep(3000);
        leftClick(robot);
        confirmRemove(robot);
        sleep(3000);
        leftClick(robot);
        moveTo(robot, 335, 129); /* not advanced */
        sleep(2000);
        leftClick(robot);
        sleep(2000);
    }

    /** Enable stonith if it is enabled. */
    private static void enableStonith(final Robot robot,
                                      final Cluster cluster) {
        moveTo(robot, 271, 250);
        leftClick(robot); /* global options */
        final String stonith = cluster.getBrowser()
                    .getClusterStatus().getGlobalParam("stonith-enabled");
        if (stonith != null && "false".equals(stonith)) {
            moveTo(robot, 944, 298);
            leftClick(robot); /* enable stonith */
        }
        moveTo(robot, 828, 183);
        sleep(2000);
        leftClick(robot); /* apply */
    }

    /** Disable stonith if it is enabled. */
    private static void disableStonith(final Robot robot,
                                       final Cluster cluster) {
        moveTo(robot, 271, 250);
        leftClick(robot); /* global options */
        final String stonith = cluster.getBrowser()
                    .getClusterStatus().getGlobalParam("stonith-enabled");
        if (stonith == null || "true".equals(stonith)) {
            moveTo(robot, 944, 298);
            leftClick(robot); /* disable stonith */
        }
        moveTo(robot, 1073, 337);
        leftClick(robot); /* no quorum policy */
        moveTo(robot, 1058, 355);
        leftClick(robot); /* ignore */
        moveTo(robot, 828, 183);
        sleep(2000);
        leftClick(robot); /* apply */
    }

    /** TEST 2. */
    private static void startTest2(final Robot robot, final Cluster cluster) {
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

        disableStonith(robot, cluster);
        checkTest(cluster, "test2", 1);
        /* create 4 dummies */
        chooseDummy(robot, dummy1X, dummy1Y, false, true);
        chooseDummy(robot, dummy2X, dummy2Y, false, true);
        chooseDummy(robot, dummy3X, dummy3Y, false, true);
        chooseDummy(robot, dummy4X, dummy4Y, false, true);
        checkTest(cluster, "test2", 2);

        /* placeholder */
        moveTo(robot, phX, phY);
        rightClick(robot);
        sleep(2000);
        moveTo(robot, phX + 30 , phY + 45);
        sleep(2000);
        leftClick(robot);
        checkTest(cluster, "test2", 3);
        /* constraints */
        addConstraint(robot, phX, phY, 0, false, -1); /* with dummy 1 */
        sleep(2000);
        moveTo(robot, 809, 192); /* ptest */
        sleep(2000);
        leftClick(robot); /*  apply */
        sleep(5000);
        checkTest(cluster, "test2", 4);

        final int dum1PopX = dummy1X + 130;
        final int dum1PopY = dummy1Y + 50;
        for (int i = 0; i < 1; i++) {
            removeOrder(robot, dum1PopX, dum1PopY);
            sleep(4000);

            checkTest(cluster, "test2", 5);

            addOrder(robot, dum1PopX, dum1PopY);
            sleep(4000);
            checkTest(cluster, "test2", 6);

            removeColocation(robot, dum1PopX, dum1PopY);
            sleep(5000);
            checkTest(cluster, "test2", 7);

            addColocation(robot, dum1PopX, dum1PopY);
            sleep(4000);
            checkTest(cluster, "test2", 8);
        }

        addConstraint(robot, dummy3X, dummy3Y, 80, false, -1); /* with ph */
        sleep(5000);
        checkTest(cluster, "test2", 9);

        final int dum3PopX = dummy3X + 165;
        final int dum3PopY = dummy3Y - 10;
        for (int i = 0; i < 2; i++) {
            removeColocation(robot, dum3PopX, dum3PopY);
            sleep(4000);

            checkTest(cluster, "test2", 9.1);

            addColocation(robot, dum3PopX, dum3PopY);
            sleep(4000);
            checkTest(cluster, "test2", 9.2);

            removeOrder(robot, dum3PopX, dum3PopY);
            sleep(5000);
            checkTest(cluster, "test2", 9.3);

            addOrder(robot, dum3PopX, dum3PopY);
            sleep(4000);
            checkTest(cluster, "test2", 9.4);
        }

        addConstraint(robot, phX, phY, 0, false, -1); /* with dummy 2 */
        sleep(5000);
        checkTest(cluster, "test2", 10);
        addConstraint(robot, dummy4X, dummy4Y, 80, false, -1); /* with ph */
        sleep(5000);
        checkTest(cluster, "test2", 11);

        /* ph -> dummy2 */
        final int dum2PopX = dummy2X - 10;
        final int dum2PopY = dummy2Y + 70;
        removeConstraint(robot, dum2PopX, dum2PopY);
        sleep(4000);
        checkTest(cluster, "test2", 11.1);
        addConstraintOrderOnly(robot,
                               phX,
                               phY,
                               -50,
                               20,
                               0,
                               false,
                               -1); /* with dummy 2 */
        sleep(4000);
        checkTest(cluster, "test2", 11.2);
        removeOrder(robot, dum2PopX, dum2PopY);
        sleep(4000);
        checkTest(cluster, "test2", 11.3);
        addConstraintColocationOnly(robot,
                                    phX,
                                    phY,
                                    -50,
                                    23,
                                    0,
                                    false,
                                    -1); /* with dummy 2 */
        sleep(4000);
        checkTest(cluster, "test2", 11.4);
        addOrder(robot, dum2PopX, dum2PopY);
        sleep(4000);
        checkTest(cluster, "test2", 11.5);

        /* dummy4 -> ph */
        final int dum4PopX = dummy4X - 40;
        final int dum4PopY = dummy4Y - 10;
        removeConstraint(robot, dum4PopX, dum4PopY);
        sleep(4000);
        checkTest(cluster, "test2", 11.6);
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
        checkTest(cluster, "test2", 11.7);
        removeColocation(robot, dum4PopX, dum4PopY);
        sleep(4000);
        checkTest(cluster, "test2", 11.8);
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
        checkTest(cluster, "test2", 11.9);
        addColocation(robot, dum4PopX, dum4PopY);
        sleep(4000);
        checkTest(cluster, "test2", 11.91);
        /* remove one dummy */
        stopResource(robot, dummy1X, dummy1Y, 0);
        sleep(5000);
        checkTest(cluster, "test2", 11.92);
        removeResource(robot, dummy1X, dummy1Y, -15);
        sleep(5000);
        checkTest(cluster, "test2", 12);
        stopResource(robot, dummy2X, dummy2Y, 0);
        sleep(10000);
        stopResource(robot, dummy3X, dummy3Y, 0);
        sleep(10000);
        stopResource(robot, dummy3X, dummy3Y, 0);
        sleep(10000);
        stopResource(robot, dummy4X, dummy4Y, 0);
        stopEverything(robot);
        sleep(10000);
        checkTest(cluster, "test2", 12.5);
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
            checkTest(cluster, "test2", 14);
            removeResource(robot, dummy3X, dummy3Y, -15);
            sleep(5000);
            checkTest(cluster, "test2", 15);
            removeResource(robot, dummy4X, dummy4Y, -15);
            sleep(5000);
        } else {
            removeEverything(robot);
            removePlaceHolder(robot, phX, phY);
        }
        if (!aborted) {
            sleepNoFactor(20000);
        }
        checkTest(cluster, "test2", 16);
    }

    /** TEST 4. */
    private static void startTest4(final Robot robot, final Cluster cluster) {
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

        disableStonith(robot, cluster);
        checkTest(cluster, "test4", 1);
        /* create 6 dummies */
        chooseDummy(robot, dummy1X, dummy1Y, false, true);
        chooseDummy(robot, dummy2X, dummy2Y, false, true);
        chooseDummy(robot, dummy3X, dummy3Y, false, true);
        chooseDummy(robot, dummy4X, dummy4Y, false, true);
        chooseDummy(robot, dummy5X, dummy5Y, false, true);
        chooseDummy(robot, dummy6X, dummy6Y, false, true);
        checkTest(cluster, "test4", 2);

        /* 2 placeholders */
        final int count = 1;
        for (int i = 0; i < count; i++) {
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
            checkTest(cluster, "test4", 2);

            /* constraints */
            /* menu dummy 5 with ph2 */
            addConstraint(robot, 120, 346, 160, false, -1);
            /* menu dummy 6 with ph2 */
            addConstraint(robot, 120, 364, 160, false, -1);

            /* with dummy 3 */
            addConstraint(robot, ph2X, ph2Y, 60, false, -1);
            /* with dummy 4 */
            addConstraint(robot, ph2X, ph2Y, 60, false, -1);

            /* with ph1 */
            addConstraint(robot, dummy3X, dummy3Y, 80, false, -1);
            /* with ph1 */
            addConstraint(robot, dummy4X, dummy4Y, 80, false, -1);

            addConstraint(robot, ph1X, ph1Y, 5, false, -1); /* with dummy 1 */
            addConstraint(robot, ph1X, ph1Y, 5, false, -1); /* with dummy 2 */
            checkTest(cluster, "test4", 2);

            /* TEST test */
            if (i < count - 1) {
                removePlaceHolder(robot, ph1X, ph1Y);
                removePlaceHolder(robot, ph2X, ph2Y);
            }
        }
        moveTo(robot, 809, 192); /* ptest */
        sleep(2000);
        leftClick(robot); /*  apply */

        checkTest(cluster, "test4", 3);
        stopEverything(robot);
        checkTest(cluster, "test4", 4);
        removeEverything(robot);
        removePlaceHolder(robot, ph1X, ph1Y);
        removePlaceHolder(robot, ph2X, ph2Y);
        sleep(40000);
    }

    /** TEST 5. */
    private static void startTest5(final Robot robot, final Cluster cluster) {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;

        final int dummy2X = 500;
        final int dummy2Y = 255;

        final int ph1X = 380;
        final int ph1Y = 500;


        disableStonith(robot, cluster);
        /* create 2 dummies */
        checkTest(cluster, "test5", 1);

        /* placeholders */
        moveTo(robot, ph1X, ph1Y);
        rightClick(robot);
        sleep(2000);
        moveTo(robot, ph1X + 30 , ph1Y + 45);
        sleep(2000);
        leftClick(robot);

        chooseDummy(robot, dummy1X, dummy1Y, false, true);
        chooseDummy(robot, dummy2X, dummy2Y, false, true);
        checkTest(cluster, "test5", 2);

        addConstraint(robot, dummy2X, dummy2Y, 35, false, -1);
        sleep(20000);
        checkTest(cluster, "test5", 2);
        addConstraint(robot, ph1X, ph1Y, 5, false, -1);

        moveTo(robot, ph1X, ph1Y);
        sleep(2000);
        leftClick(robot);
        sleep(2000);
        moveTo(robot, 809, 192); /* ptest */
        sleep(2000);
        leftClick(robot); /*  apply */
        checkTest(cluster, "test5", 2.1);

        leftClick(robot); /*  apply */
        int dum1PopX = dummy1X + 80;
        int dum1PopY = dummy1Y + 60;
        removeConstraint(robot, dum1PopX, dum1PopY);
        checkTest(cluster, "test5", 2.5);
        /* constraints */
        for (int i = 1; i <= 10; i++) {
            addConstraint(robot, dummy1X, dummy1Y, 35, false, -1);

            checkTest(cluster, "test5", 3);

            removeConstraint(robot, dum1PopX, dum1PopY);
            checkTest(cluster, "test5", 2.5);

            addConstraint(robot, ph1X, ph1Y, 5, false, -1);

            checkTest(cluster, "test5", 3.5);

            removeConstraint(robot, dum1PopX, dum1PopY);
            checkTest(cluster, "test5", 2.5);
            Tools.info("i: " + i);
        }
        stopEverything(robot);
        checkTest(cluster, "test5", 3.1);
        removeEverything(robot);
        sleep(5000);
        checkTest(cluster, "test5", 1);
    }

    /** TEST 6. */
    private static void startTest6(final Robot robot, final Cluster cluster) {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;

        final int ph1X = 315;
        final int ph1Y = 394;


        //disableStonith(robot, cluster);
        /* create 2 dummies */
        //checkTest(cluster, "test5", 1);

        /* placeholders */
        moveTo(robot, ph1X, ph1Y);
        rightClick(robot);
        sleep(2000);
        moveTo(robot, ph1X + 30 , ph1Y + 45);
        sleep(2000);
        leftClick(robot);

        chooseDummy(robot, dummy1X, dummy1Y, false, true);
        //checkTest(cluster, "test5", 2);
        final int dum1PopX = dummy1X + 70;
        final int dum1PopY = dummy1Y + 60;
        for (int i = 0; i < 20; i++) {
            Tools.info("i: " + i);
            addConstraint(robot, ph1X, ph1Y, 5, false, -1);
            if (!aborted) {
                sleepNoFactor(2000);
            }
            removeConstraint(robot, dum1PopX, dum1PopY);
        }
        stopEverything(robot);
        sleepNoFactor(20000);
        removeEverything(robot);
        sleepNoFactor(20000);
        resetTerminalAreas(cluster);
    }

    private static void startTest7(final Robot robot, final Cluster cluster) {
        slowFactor = 0.5f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;
        disableStonith(robot, cluster);
        for (int i = 30; i > 0; i--) {
            Tools.info("I: " + i);
            checkTest(cluster, "test7", 1);
            /* create dummy */
            sleep(5000);
            chooseDummy(robot, dummy1X, dummy1Y, false, true);
            checkTest(cluster, "test7", 2);
            sleep(5000);
            stopResource(robot, dummy1X, dummy1Y, 0);
            checkTest(cluster, "test7", 3);
            sleep(5000);
            removeResource(robot, dummy1X, dummy1Y, -15);
        }
        System.gc();
    }

    private static void startTest8(final Robot robot, final Cluster cluster) {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 540;
        final int dummy1Y = 250;
        disableStonith(robot, cluster);
        checkTest(cluster, "test8", 1);
        final int count = 30;
        for (int i = count; i > 0; i--) {
            Tools.info("I: " + i);
            //checkTest(cluster, "test7", 1);
            sleep(5000);
            chooseDummy(robot, dummy1X, dummy1Y, false, true);
            sleep(5000);
            moveTo(robot, 550, 250);
            leftPress(robot); /* move the reosurce */
            moveTo(robot, 300, 250);
            leftRelease(robot);
        }
        checkTest(cluster, "test8-" + count, 2);
        stopEverything(robot);
        checkTest(cluster, "test8-" + count, 3);
        removeEverything(robot);
        checkTest(cluster, "test8", 4);
        resetTerminalAreas(cluster);
    }

    private static void startTestA(final Robot robot, final Cluster cluster) {
        slowFactor = 0.5f;
        aborted = false;
        final int gx = 235;
        final int gy = 255;
        disableStonith(robot, cluster);
        for (int i = 20; i > 0; i--) {
            Tools.info("I: " + i);

            checkTest(cluster, "testA", 1);
            /* group with dummy resources */
            moveTo(robot, gx, gy);
            sleep(1000);
            rightClick(robot); /* popup */
            moveTo(robot, gx + 46, gy + 11);
            sleep(1000);
            leftClick(robot); /* choose group */
            sleep(3000);
            /* create dummy */
            moveTo(robot, gx + 46, gy + 11);
            rightClick(robot); /* group popup */
            sleep(2000 + i * 500);
            moveTo(robot, gx + 80, gy + 20);
            moveTo(robot, gx + 84, gy + 22);
            moveTo(robot, gx + 580, gy + 22);
            sleep(1000);
            typeDummy(robot);
            sleep(i * 300);
            setTimeouts(robot, true);
            moveTo(robot, 809, 192); /* ptest */
            sleep(6000);
            leftClick(robot); /* apply */
            sleep(6000);
            checkTest(cluster, "testA", 2);
            stopResource(robot, gx, gy, 0);
            sleep(6000);
            checkTest(cluster, "testA", 3);
            removeResource(robot, gx, gy, 0);
            resetTerminalAreas(cluster);
        }
        System.gc();
    }

    private static void startTestB(final Robot robot, final Cluster cluster) {
        slowFactor = 0.5f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 255;
        disableStonith(robot, cluster);
        for (int i = 20; i > 0; i--) {
            Tools.info("I: " + i);
            checkTest(cluster, "testB", 1);
            /* create dummy */
            sleep(5000);
            chooseDummy(robot, dummy1X, dummy1Y, true, true);
            checkTest(cluster, "testB", 2);
            sleep(5000);
            stopResource(robot, dummy1X, dummy1Y, 0);
            checkTest(cluster, "testB", 3);
            sleep(5000);
            removeResource(robot, dummy1X, dummy1Y, -15);
            resetTerminalAreas(cluster);
        }
        System.gc();
    }

    private static void startTestC(final Robot robot, final Cluster cluster) {
        slowFactor = 0.5f;
        final int statefulX = 500;
        final int statefulY = 255;
        disableStonith(robot, cluster);
        for (int i = 20; i > 0; i--) {
            Tools.info("I: " + i);
            checkTest(cluster, "testC", 1);
            /** Add m/s Stateful resource */
            moveTo(robot, statefulX, statefulY);
            rightClick(robot); /* popup */
            moveTo(robot, statefulX + 137, statefulY + 8);
            moveTo(robot, statefulX + 137, statefulY + 28);
            moveTo(robot, statefulX + 412, statefulY + 27);
            moveTo(robot, statefulX + 432, statefulY + 70);
            moveTo(robot, statefulX + 665, statefulY + 70);
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
            stopResource(robot, statefulX, statefulY, -20);
            checkTest(cluster, "testC", 2);
            sleep(5000);
            removeResource(robot, statefulX, statefulY, -20);
            resetTerminalAreas(cluster);
        }
    }

    /** Pacemaker Leak tests. */
    private static void startTestD(final Robot robot, final Cluster cluster) {
        slowFactor = 0.2f;
        aborted = false;
        int count = 20;
        final int dummy1X = 540;
        final int dummy1Y = 250;
        for (int i = count; i > 0; i--) {
            Tools.info("1 I: " + i);
            chooseDummy(robot, dummy1X, dummy1Y, false, false);
            removeResource(robot, dummy1X, dummy1Y, -20);
        }
        chooseDummy(robot, dummy1X, dummy1Y, false, false);
        int pos = 0;
        for (int i = count; i > 0; i--) {
            Tools.info("2 I: " + i);
            double rand = Math.random();
            if (rand < 0.33) {
                if (pos == 1) {
                    continue;
                }
                pos = 1;
                moveTo(robot, 796, 247);
                leftClick(robot);
            } else if (rand < 0.66) {
                if (pos == 2) {
                    continue;
                }
                pos = 2;
                moveTo(robot, 894, 248);
                leftClick(robot);
            } else {
                if (pos == 3) {
                    continue;
                }
                pos = 3;
                moveTo(robot, 994, 248);
                leftClick(robot);
            }
        }
        removeResource(robot, dummy1X, dummy1Y, -20);
    }

    /** Host wizard deadlock. */
    private static void startTestE(final Robot robot, final Cluster cluster) {
        slowFactor = 0.2f;
        aborted = false;
        int count = 100;
        for (int i = count; i > 0; i--) {
            Tools.info("1 I: " + i);
            moveTo(robot, 300 , 200); /* host */
            sleep(2000);
            rightClick(robot);
            sleep(2000);
            moveTo(robot, 400 , 220); /* wizard */
            sleep(2000);
            leftClick(robot);
            sleep(10000);
            moveTo(robot, 940 , 570); /* cancel */
            sleep(2000);
            leftClick(robot);
            sleep(2000);
        }
    }

    /** Cloned group. */
    private static void startTestF(final Robot robot, final Cluster cluster) {
        slowFactor = 0.2f;
        aborted = false;
        final int gx = 235;
        final int gy = 255;
        disableStonith(robot, cluster);
        checkTest(cluster, "testF", 1);
        /* group with dummy resources */
        moveTo(robot, gx, gy);
        sleep(1000);
        rightClick(robot); /* popup */
        moveTo(robot, gx + 46, gy + 11);
        sleep(1000);
        leftClick(robot); /* choose group */
        sleep(3000);
        moveTo(robot, 900, 250);
        leftClick(robot); /* clone */

        final int gxM = 110; /* tree menu */
        final int gyM = 290;
        int type = 1;
        //int type = 2;
        for (int i = 2; i > 0; i--) {
            Tools.info("I: " + i);
            /* create dummy */
            moveTo(robot, gxM + 46, gyM + 11);
            rightClick(robot); /* group popup */
            sleep(2000 + i * 500);
            moveTo(robot, gxM + 80, gyM + 20);
            moveTo(robot, gxM + 84, gyM + 22);
            moveTo(robot, gxM + 580, gyM + 22);
            sleep(1000);
            typeDummy(robot);
            sleep(i * 300);
            setTimeouts(robot, true);
            if (type == 1) {
                moveTo(robot, 809, 192); /* ptest */
                sleep(6000);
                leftClick(robot); /* apply */
                sleep(6000);
            }
        }
        if (type != 1) {
            moveTo(robot, 809, 192); /* ptest */
            sleep(6000);
            leftClick(robot); /* apply */
            sleep(6000);
        }
        checkTest(cluster, "testF", 2);
        /* set resource stickiness */
        moveTo(robot, 1000, 480);
        sleep(1000);
        leftClick(robot);
        sleep(1000);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(200);
        press(robot, KeyEvent.VK_2);
        sleep(1000);
        moveTo(robot, 809, 192); /* ptest */
        sleep(6000);
        leftClick(robot); /* apply */
        sleep(6000);
        checkTest(cluster, "testF", 3);

        stopResource(robot, gx, gy, 0);
        sleep(6000);
        checkTest(cluster, "testF", 4);
        removeResource(robot, gx, gy, -40);
        resetTerminalAreas(cluster);
        System.gc();
    }

    /** Sets location. */

    /** Sets location. */
    private static void setLocation(final Robot robot, final Integer[] events) {
        moveTo(robot, 1100, 298);
        leftPress(robot); /* scroll bar */
        moveTo(robot, 1100, 510);
        leftRelease(robot);

        moveTo(robot, 1041 , 331);
        sleep(2000);
        leftClick(robot);
        for (final int ev : events) {
            if (ev == KeyEvent.VK_PLUS) {
                robot.keyPress(KeyEvent.VK_SHIFT);
                sleep(400);
            }
            press(robot, ev);
            sleep(400);
            if (ev == KeyEvent.VK_PLUS) {
                robot.keyRelease(KeyEvent.VK_SHIFT);
                sleep(400);
            }
        }
        moveTo(robot, 809, 192); /* ptest */
        sleep(4000);
        leftClick(robot); /* apply */
        sleep(2000);

        moveTo(robot, 1100, 350);
        leftPress(robot); /* scroll bar back */
        moveTo(robot, 1100, 150);
        leftRelease(robot);

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
    private static void setTimeouts(final Robot robot,
                                    final boolean migrateTimeouts) {
        int yCorr = -5;
        if (migrateTimeouts) {
            yCorr = -95;
        }
        sleep(3000);
        moveTo(robot, 1100, 298);
        leftPress(robot); /* scroll bar */
        moveTo(robot, 1100, 550);
        leftRelease(robot);
        moveTo(robot, 956, 490 + yCorr);
        leftClick(robot); /* start timeout */
        press(robot, KeyEvent.VK_2);
        sleep(200);
        press(robot, KeyEvent.VK_0);
        sleep(200);
        press(robot, KeyEvent.VK_0);
        sleep(200);

        moveTo(robot, 956, 520 + yCorr);
        leftClick(robot); /* stop timeout */
        press(robot, KeyEvent.VK_1);
        sleep(200);
        press(robot, KeyEvent.VK_9);
        sleep(200);
        press(robot, KeyEvent.VK_2);
        sleep(200);

        moveTo(robot, 956, 550 + yCorr);
        leftClick(robot); /* monitor timeout */
        press(robot, KeyEvent.VK_1);
        sleep(200);
        press(robot, KeyEvent.VK_5);
        sleep(200);
        press(robot, KeyEvent.VK_4);
        sleep(200);

        moveTo(robot, 956, 580 + yCorr);
        leftClick(robot); /* monitor interval */
        press(robot, KeyEvent.VK_1);
        sleep(200);
        press(robot, KeyEvent.VK_2);
        sleep(200);
        press(robot, KeyEvent.VK_1);
        sleep(200);
        if (migrateTimeouts) {
            moveTo(robot, 956, 610 + yCorr);
            leftClick(robot); /* reload */
            press(robot, KeyEvent.VK_BACK_SPACE);
            sleep(200);
            press(robot, KeyEvent.VK_BACK_SPACE);
            sleep(200);

            moveTo(robot, 956, 630 + yCorr);
            leftClick(robot); /* migrate from */
            press(robot, KeyEvent.VK_1);
            sleep(200);
            press(robot, KeyEvent.VK_2);
            sleep(200);
            press(robot, KeyEvent.VK_3);
            sleep(200);

            moveTo(robot, 956, 660 + yCorr);
            leftClick(robot); /* migrate to */
            press(robot, KeyEvent.VK_1);
            sleep(200);
            press(robot, KeyEvent.VK_2);
            sleep(200);
            press(robot, KeyEvent.VK_2);
            sleep(200);
        }

        moveTo(robot, 1100, 350);
        leftPress(robot); /* scroll bar back */
        moveTo(robot, 1100, 150);
        leftRelease(robot);
    }
    private static void sleepNoFactor(final double x) {
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
    private static void chooseDummy(final Robot robot,
                                    final int x,
                                    final int y,
                                    final boolean clone,
                                    final boolean apply) {
        moveTo(robot, x, y);
        sleep(1000);
        rightClick(robot); /* popup */
        sleep(1000);
        moveTo(robot, x + 57, y + 28);
        moveTo(robot, x + 290, y + 28);
        moveTo(robot, x + 290, y + 72);
        moveTo(robot, x + 580, y + 72);
        sleep(2000);
        typeDummy(robot);
        if (apply) {
            sleep(2000);
            setTimeouts(robot, true);
            if (clone) {
                moveTo(robot, 893, 250);
                leftClick(robot); /* clone */
            }
            moveTo(robot, 809, 192); /* ptest */
            sleep(4000);
            leftClick(robot); /* apply */
            sleep(2000);
        }
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
        sleep(120000);
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
        moveTo(robot, 512 , 472);
        leftClick(robot);
    }

    /** Stops resource. */
    private static void stopResource(final Robot robot,
                                     final int x,
                                     final int y,
                                     final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        sleep(2000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 70, y + 130 + yFactor);
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
        moveTo(robot, x + 70, y + 130 + yFactor);
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
        moveTo(robot, x + 70, y + 80 + yFactor);
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
        moveTo(robot, x + 70, y + 220 + yFactor);
        sleep(6000); /* ptest */
        leftClick(robot); /* stop */
    }

    /** Unmigrate resource. */
    private static void unmigrateResource(final Robot robot,
                                          final int x,
                                          final int y,
                                          final int yFactor) {
        moveTo(robot, x + 50, y + 5);
        sleep(6000);
        rightClick(robot); /* popup */
        moveTo(robot, x + 70, y + 260 + yFactor);
        sleep(12000); /* ptest */
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
        moveTo(robot, x + 70, y + 200 + yFactor);
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
        moveTo(robot, x + 70, y + 200 + yFactor);
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
               x + 520 + xCorrection,
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
        sleep(1000);
        moveTo(robot, x + 335, y + 50 + groupcor);
        sleep(1000);
        moveTo(robot, x + 335, y + 50 + groupcor + skipY + 7);
        sleep(1000);
        moveTo(robot, x + 500 + xCorrection, y + 50 + groupcor + skipY + 7);
        sleep(1000);
        moveTo(robot,
               x + 520 + xCorrection,
               y + 50 + groupcor + skipY + 7 + with);
        sleep(6000); /* ptest */
        leftClick(robot); /* start before */
    }

    /** Removes constraint. */
    private static void removeConstraint(final Robot robot,
                                         final int popX,
                                         final int popY) {
        moveTo(robot, popX, popY);
        sleep(10000);
        rightClick(robot); /* constraint popup */
        sleep(2000);
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
    private static void startTest3(final Robot robot, final Cluster cluster) {
        slowFactor = 0.3f;
        aborted = false;
        disableStonith(robot, cluster);
        for (int i = 20; i > 0; i--) {
            Tools.info("I: " + i);
            checkTest(cluster, "test3", 1);
            /* filesystem/drbd */
            moveTo(robot, 577, 253);
            rightClick(robot); /* popup */
            moveTo(robot, 709, 278);
            moveTo(robot, 894, 283);
            leftClick(robot); /* choose fs */
            moveTo(robot, 1075, 406);
            leftClick(robot); /* choose drbd */
            moveTo(robot, 1043, 444);
            leftClick(robot); /* choose drbd */
            moveTo(robot, 1068, 439);
            leftClick(robot); /* mount point */
            moveTo(robot, 1039, 475);
            leftClick(robot); /* mount point */

            moveTo(robot, 1068, 475);
            leftClick(robot); /* filesystem type */
            moveTo(robot, 1039, 560);
            leftClick(robot); /* ext3 */

            moveTo(robot, 815, 186);
            leftClick(robot); /* apply */
            sleep(2000);
            checkTest(cluster, "test3", 2);
            stopEverything(robot);
            checkTest(cluster, "test3", 3);
            removeEverything(robot);
            resetTerminalAreas(cluster);
        }
        System.gc();
    }

    /** Press button. */
    private static void press(final Robot robot, final int ke)  {
        if (aborted) {
            return;
        }
        robot.keyPress(ke);
        robot.keyRelease(ke);
        sleepNoFactor(200);
    }

    /** Left click. */
    private static void leftClick(final Robot robot)  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        Tools.sleep(400);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        sleepNoFactor(300);
    }

    /** Left press. */
    private static void leftPress(final Robot robot)  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        sleepNoFactor(300);
    }

    /** Left release. */
    private static void leftRelease(final Robot robot)  {
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        if (aborted) {
            return;
        }
        sleepNoFactor(300);
    }

    /** Right click. */
    private static void rightClick(final Robot robot)  {
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
    private static void rightClickGroup(final Robot robot)  {
        if (aborted) {
            return;
        }
        rightClick(robot);
        sleepNoFactor(10000);
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
        Tools.info("start register movement in 3 seconds");
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

    /** Return vertical position of the blockdevices. */
    private static int getBlockDevY() {
        Tools.info("move to position, start in 10 seconds");
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

    private static void addDrbdResource(final Robot robot,
                                        final int blockDevY) {
        moveTo(robot, 334, blockDevY); /* add drbd resource */
        rightClick(robot);
        moveTo(robot, 342, blockDevY + 6);
        moveTo(robot, 667, blockDevY + 7);
        leftClick(robot);
        sleep(20000);
    }

    private static void newDrbdResource(final Robot robot) {
        moveTo(robot, 720, 570); /* new drbd resource */
        leftClick(robot); /* next */
        sleep(10000);
    }

    private static void chooseDrbdResourceInterface(final Robot robot,
                                                    final int offset) {
        moveTo(robot, 521, 372 + offset); /* interface */
        leftClick(robot);
        moveTo(robot, 486, 416 + offset);
        leftClick(robot);
        sleep(1000);
    }

    private static void chooseDrbdResource(final Robot robot) {
        chooseDrbdResourceInterface(robot, 0);
        chooseDrbdResourceInterface(robot, 30);

        moveTo(robot, 720, 570);
        leftClick(robot); /* next */
        sleep(10000);
    }

    private static void addDrbdVolume(final Robot robot) {
        moveTo(robot, 720, 570); /* volume */
        leftClick(robot); /* next */
        sleep(10000);
    }

    private static void addBlockDevice(final Robot robot) {
        moveTo(robot, 720, 570); /* block device */
        leftClick(robot); /* next */
        sleep(10000);
    }

    private static void addMetaData(final Robot robot) {
        moveTo(robot, 720, 570); /* meta-data */
        leftClick(robot); /* next */
        sleep(20000);
    }

    private static void addFileSystem(final Robot robot) {
        /* do nothing. */
    }

    private static void removeDrbdVolume(final Robot robot) {
        moveTo(robot, 480, 250); /* rsc popup */
        rightClick(robot); /* finish */
        moveTo(robot, 555, 340); /* remove */
        leftClick(robot);
        confirmRemove(robot);
    }


    /** DRBD Test 1. */
    private static void startDRBDTest1(final Robot robot,
                                       final Cluster cluster,
                                       final int blockDevY) {
        final String drbdTest = "drbd-test1";
        slowFactor = 0.2f;
        aborted = false;

        addDrbdResource(robot, blockDevY);
        chooseDrbdResource(robot);
        addDrbdVolume(robot);
        addBlockDevice(robot);
        addBlockDevice(robot);
        sleep(50000);
        checkDRBDTest(cluster, drbdTest, 1);
        addMetaData(robot);
        addFileSystem(robot);
        moveTo(robot, 820, 570); /* fs */
        leftClick(robot); /* finish */
        sleep(10000);
        checkDRBDTest(cluster, drbdTest, 1.1);
        removeDrbdVolume(robot);
        checkDRBDTest(cluster, drbdTest, 2);

        checkDRBDTest(cluster, drbdTest, 2);
    }

    /** DRBD Test 2. */
    private static void startDRBDTest2(final Robot robot,
                                       final Cluster cluster,
                                       final int blockDevY) {
        final String drbdTest = "drbd-test1";
        slowFactor = 0.2f;
        aborted = false;

        Tools.info(drbdTest + "/1");
        addDrbdResource(robot, blockDevY);

        moveTo(robot, 960, 570);
        leftClick(robot); /* cancel */
        sleep(60000);

        Tools.info(drbdTest + "/2");
        addDrbdResource(robot, blockDevY);
        chooseDrbdResource(robot);

        moveTo(robot, 960, 570);
        leftClick(robot); /* cancel */
        sleep(60000);

        Tools.info(drbdTest + "/3");
        addDrbdResource(robot, blockDevY);
        chooseDrbdResource(robot);
        addDrbdVolume(robot);

        moveTo(robot, 960, 570);
        leftClick(robot); /* cancel */
        confirmRemove(robot);
        sleep(60000);

        Tools.info(drbdTest + "/4");
        addDrbdResource(robot, blockDevY);
        chooseDrbdResource(robot);
        addDrbdVolume(robot);
        addBlockDevice(robot);

        moveTo(robot, 960, 570);
        leftClick(robot); /* cancel */
        confirmRemove(robot);
        sleep(60000);

        Tools.info(drbdTest + "/5");
        addDrbdResource(robot, blockDevY);
        chooseDrbdResource(robot);
        addDrbdVolume(robot);
        addBlockDevice(robot);
        addBlockDevice(robot);
        sleep(50000);
        checkDRBDTest(cluster, drbdTest, 1);

        moveTo(robot, 960, 570);
        leftClick(robot); /* cancel */
        confirmRemove(robot);
        sleep(60000);

        Tools.info(drbdTest + "/6");
        addDrbdResource(robot, blockDevY);
        chooseDrbdResource(robot);
        addDrbdVolume(robot);
        addBlockDevice(robot);
        addBlockDevice(robot);
        sleep(50000);
        checkDRBDTest(cluster, drbdTest, 1);
        addMetaData(robot);

        moveTo(robot, 960, 570);
        leftClick(robot); /* cancel */
        confirmRemove(robot);
        sleep(60000);

        Tools.info(drbdTest + "/7");
        addDrbdResource(robot, blockDevY);
        chooseDrbdResource(robot);
        addDrbdVolume(robot);
        addBlockDevice(robot);
        addBlockDevice(robot);
        sleep(50000);
        checkDRBDTest(cluster, drbdTest, 1);
        addMetaData(robot);
        addFileSystem(robot);
        checkDRBDTest(cluster, drbdTest, 1.1);
        sleep(20000);

        moveTo(robot, 960, 570);
        sleep(2000);
        leftClick(robot); /* cancel */
        sleep(20000);
        confirmRemove(robot);
        sleep(60000);

        Tools.info(drbdTest + "/8");
        addDrbdResource(robot, blockDevY);
        chooseDrbdResource(robot);
        addDrbdVolume(robot);
        addBlockDevice(robot);
        addBlockDevice(robot);
        sleep(50000);
        checkDRBDTest(cluster, drbdTest, 1);
        addMetaData(robot);
        addFileSystem(robot);
        moveTo(robot, 820, 570); /* fs */
        leftClick(robot); /* finish */
        sleep(10000);
        checkDRBDTest(cluster, drbdTest, 1.1);

        removeDrbdVolume(robot);
        checkDRBDTest(cluster, drbdTest, 2);
    }

    /** DRBD Test 3. */
    private static void startDRBDTest3(final Robot robot,
                                       final Cluster cluster,
                                       final int blockDevY) {
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
            addDrbdResource(robot, blockDevY + offset);
            if (i == 1 && cluster.getHostsArray()[0].hasVolumes()) {
                newDrbdResource(robot);
            }
            chooseDrbdResource(robot);

            addDrbdVolume(robot);
            addBlockDevice(robot);
            addBlockDevice(robot);
            sleep(50000);

            if (offset == 0) {
                checkDRBDTest(cluster, drbdTest, 1.1);
            } else {
                checkDRBDTest(cluster, drbdTest, 1.2);
            }
            sleep(10000);
            addMetaData(robot);
            addFileSystem(robot);
            moveTo(robot, 820, 570); /* fs */
            leftClick(robot); /* finish */
            sleep(10000);

            if (offset == 0) {
                checkDRBDTest(cluster, drbdTest, 1);
            }
            offset += 40;
        }
        checkDRBDTest(cluster, drbdTest, 2);

        moveTo(robot, 480, 250); /* select r0 */
        leftClick(robot);
        sleep(2000);
        leftClick(robot);

        moveTo(robot, 1073, protocolY); /* select protocol */
        leftClick(robot);
        press(robot, KeyEvent.VK_UP); /* protocol b */
        sleep(200);
        press(robot, KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(robot, 1075, 423 + correctionY); /* select fence peer */
        leftClick(robot);
        sleep(2000);
        press(robot, KeyEvent.VK_DOWN); /* select dopd */
        sleep(200);
        press(robot, KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(robot, 970, 480 + correctionY); /* wfc timeout */
        leftClick(robot);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(robot, KeyEvent.VK_9);
        sleep(2000);

        moveTo(robot, 814, 189);
        sleep(6000); /* test */
        leftClick(robot); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(robot); /* apply */
        checkDRBDTest(cluster, drbdTest, 2.1); /* 2.1 */


        /* common */
        moveTo(robot, 500, 390); /* select background */
        leftClick(robot);
        sleep(2000);
        leftClick(robot);

        moveTo(robot, 970, 383); /* wfc timeout */
        leftClick(robot);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(robot, KeyEvent.VK_3);
        sleep(2000);

        moveTo(robot, 814, 189);
        sleep(6000); /* test */
        leftClick(robot); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(robot); /* apply */
        sleep(10000);
        checkDRBDTest(cluster, drbdTest, 2.11); /* 2.11 */
        moveTo(robot, 970, 383); /* wfc timeout */
        sleep(6000);
        leftClick(robot);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(robot, KeyEvent.VK_0);
        sleep(2000);
        moveTo(robot, 814, 189);
        sleep(6000); /* test */
        leftClick(robot); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(robot); /* apply */

        /* resource */
        moveTo(robot, 480, 250); /* select r0 */
        leftClick(robot);
        sleep(2000);
        leftClick(robot);

        moveTo(robot, 1073, protocolY); /* select protocol */
        leftClick(robot);
        press(robot, KeyEvent.VK_DOWN); /* protocol c */
        sleep(200);
        press(robot, KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(robot, 1075, 423 + correctionY); /* select fence peer */
        leftClick(robot);
        sleep(2000);
        press(robot, KeyEvent.VK_UP); /* deselect dopd */
        sleep(200);
        press(robot, KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(robot, 970, 480 + correctionY); /* wfc timeout */
        leftClick(robot);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(robot, KeyEvent.VK_5);
        sleep(2000);

        moveTo(robot, 814, 189);
        sleep(6000); /* test */
        leftClick(robot); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(robot); /* apply */
        checkDRBDTest(cluster, drbdTest, 2.2); /* 2.2 */

        moveTo(robot, 970, 480 + correctionY); /* wfc timeout */
        leftClick(robot);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(robot, KeyEvent.VK_0);
        sleep(2000);

        moveTo(robot, 814, 189);
        sleep(6000); /* test */
        leftClick(robot); /* apply */
        checkDRBDTest(cluster, drbdTest, 2.3); /* 2.3 */

        moveTo(robot, 480, 250); /* rsc popup */
        rightClick(robot);
        moveTo(robot, 555, 340); /* remove */
        leftClick(robot);
        confirmRemove(robot);
        checkDRBDTest(cluster, drbdTest, 3);
        moveTo(robot, 480, 250); /* rsc popup */
        rightClick(robot);
        moveTo(robot, 555, 340); /* remove */
        leftClick(robot);
        confirmRemove(robot);
        checkDRBDTest(cluster, drbdTest, 4);
    }

    /** DRBD Test 4. */
    private static void startDRBDTest4(final Robot robot,
                                       final Cluster cluster,
                                       final int blockDevY) {
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
            addDrbdResource(robot, blockDevY + offset);
            if (i == 0) {
                /* first one */
                chooseDrbdResource(robot);
            } else {
                /* existing drbd resource */
                moveTo(robot, 460, 400);
                leftClick(robot);
                moveTo(robot, 460, 440);
                leftClick(robot); /* drbd: r0 */
                moveTo(robot, 720, 570);
                leftClick(robot); /* next */
                sleep(10000);

                //addDrbdVolume(robot);
            }
            addDrbdVolume(robot);

            addBlockDevice(robot);
            addBlockDevice(robot);
            sleep(50000);
            if (offset == 0) {
                checkDRBDTest(cluster, drbdTest, 1.1);
            } else {
                checkDRBDTest(cluster, drbdTest, 1.2);
            }
            sleep(10000);

            addMetaData(robot);
            addFileSystem(robot);
            moveTo(robot, 820, 570); /* fs */
            leftClick(robot); /* finish */
            sleep(10000);

            if (offset == 0) {
                checkDRBDTest(cluster, drbdTest, 1);
            }
            offset += 40;
        }
        checkDRBDTest(cluster, drbdTest, 2);

        moveTo(robot, 480, 250); /* select r0 */
        leftClick(robot);
        sleep(2000);
        leftClick(robot);

        moveTo(robot, 1073, protocolY); /* select protocol */
        leftClick(robot);
        press(robot, KeyEvent.VK_UP); /* protocol b */
        sleep(200);
        press(robot, KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(robot, 1075, 423 + correctionY); /* select fence peer */
        leftClick(robot);
        sleep(2000);
        press(robot, KeyEvent.VK_DOWN); /* select dopd */
        sleep(200);
        press(robot, KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(robot, 970, 480 + correctionY); /* wfc timeout */
        leftClick(robot);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(robot, KeyEvent.VK_9);
        sleep(2000);

        moveTo(robot, 814, 189);
        sleep(6000); /* test */
        leftClick(robot); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(robot); /* apply */
        checkDRBDTest(cluster, drbdTest, 2.1); /* 2.1 */


        /* common */
        moveTo(robot, 500, 390); /* select background */
        leftClick(robot);
        sleep(2000);
        leftClick(robot);

        moveTo(robot, 970, 383); /* wfc timeout */
        leftClick(robot);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(robot, KeyEvent.VK_3);
        sleep(2000);

        moveTo(robot, 814, 189);
        sleep(6000); /* test */
        leftClick(robot); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(robot); /* apply */
        sleep(10000);
        checkDRBDTest(cluster, drbdTest, 2.11); /* 2.11 */
        moveTo(robot, 970, 383); /* wfc timeout */
        sleep(6000);
        leftClick(robot);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(robot, KeyEvent.VK_0);
        sleep(2000);
        moveTo(robot, 814, 189);
        sleep(6000); /* test */
        leftClick(robot); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(robot); /* apply */

        /* resource */
        moveTo(robot, 480, 250); /* select r0 */
        leftClick(robot);
        sleep(2000);
        leftClick(robot);

        moveTo(robot, 1073, protocolY); /* select protocol */
        leftClick(robot);
        press(robot, KeyEvent.VK_DOWN); /* protocol c */
        sleep(200);
        press(robot, KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(robot, 1075, 423 + correctionY); /* select fence peer */
        leftClick(robot);
        sleep(2000);
        press(robot, KeyEvent.VK_UP); /* deselect dopd */
        sleep(200);
        press(robot, KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo(robot, 970, 480 + correctionY); /* wfc timeout */
        leftClick(robot);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(robot, KeyEvent.VK_5);
        sleep(2000);

        moveTo(robot, 814, 189);
        sleep(6000); /* test */
        leftClick(robot); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick(robot); /* apply */
        checkDRBDTest(cluster, drbdTest, 2.2); /* 2.2 */

        moveTo(robot, 970, 480 + correctionY); /* wfc timeout */
        leftClick(robot);
        press(robot, KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(robot, KeyEvent.VK_0);
        sleep(2000);

        moveTo(robot, 814, 189);
        sleep(6000); /* test */
        leftClick(robot); /* apply */
        checkDRBDTest(cluster, drbdTest, 2.3); /* 2.3 */

        moveTo(robot, 480, 250); /* rsc popup */
        rightClick(robot);
        moveTo(robot, 555, 340); /* remove */
        leftClick(robot);
        confirmRemove(robot);
        checkDRBDTest(cluster, drbdTest, 3);
        moveTo(robot, 480, 250); /* rsc popup */
        rightClick(robot);
        moveTo(robot, 555, 340); /* remove */
        leftClick(robot);
        confirmRemove(robot);
        checkDRBDTest(cluster, drbdTest, 4);
    }

    /** VM Test 1. */
    private static void startVMTest1(final String vmTest,
                                     final Robot robot,
                                     final Cluster cluster,
                                     final int count) {
        slowFactor = 0.2f;
        aborted = false;
        String name = "dmc";
        final int count2 = 3;
        for (int j = 0; j < count; j++) {
            checkVMTest(cluster, vmTest, 1, name);
            name += "i";
        }
        name = "dmc";
        final List<String> names = new ArrayList<String>();

        for (int j = 0; j < count; j++) {
            moveTo(robot, 56, 252); /* popup */
            rightClick(robot);
            moveTo(robot, 159, 271); /* new domain */
            leftClick(robot);
            moveTo(robot, 450, 380); /* domain name */
            leftClick(robot);
            press(robot, KeyEvent.VK_D);
            sleep(200);
            press(robot, KeyEvent.VK_M);
            sleep(200);
            press(robot, KeyEvent.VK_C);
            sleep(200);
            for (int k = 0; k < j; k++) {
                press(robot, KeyEvent.VK_I); /* dmci, dmcii, etc. */
                sleep(200);
            }
            moveTo(robot, 730, 570);
            leftClick(robot);
            //press(robot, KeyEvent.VK_ENTER);

            moveTo(robot, 593, 440); /* source file */
            sleep(2000);
            leftClick(robot);
            sleep(2000);
            press(robot, KeyEvent.VK_T);
            sleep(200);
            press(robot, KeyEvent.VK_E);
            sleep(200);
            press(robot, KeyEvent.VK_S);
            sleep(200);
            press(robot, KeyEvent.VK_T);
            sleep(2000);
            for (int i = 0; i < count2; i++) {
                moveTo(robot, 600, 375); /* disk/block device */
                leftClick(robot);
                sleep(1000);
                moveTo(robot, 430, 375); /* image */
                leftClick(robot);
                sleep(1000);
            }

            moveTo(robot, 730, 570);
            leftClick(robot);
            //press(robot, KeyEvent.VK_ENTER);
            sleep(5000);
            moveTo(robot, 730, 570);
            leftClick(robot);
            //press(robot, KeyEvent.VK_ENTER); /* storage */
            sleep(5000);
            for (int i = 0; i < count2; i++) {
                moveTo(robot, 600, 375); /* bridge */
                leftClick(robot);
                sleep(1000);
                moveTo(robot, 430, 375); /* network */
                leftClick(robot);
                sleep(1000);
            }
            moveTo(robot, 730, 570);
            leftClick(robot);
            //press(robot, KeyEvent.VK_ENTER); /* network */
            sleep(5000);
            for (int i = 0; i < count2; i++) {
                moveTo(robot, 600, 375); /* sdl */
                leftClick(robot);
                sleep(1000);
                moveTo(robot, 430, 375); /* vnc */
                leftClick(robot);
                sleep(1000);
            }
            moveTo(robot, 730, 570);
            leftClick(robot);
            //press(robot, KeyEvent.VK_ENTER); /* display */
            sleep(5000);

            final int yMoreHosts = 30 * (cluster.getHosts().size() - 1);
            moveTo(robot, 530, 410 + yMoreHosts); /* create config */
            leftClick(robot);
            checkVMTest(cluster, vmTest, 2, name);

            if (cluster.getHosts().size() > 1) {
                /* two hosts */
                moveTo(robot, 410, 370); /* deselect first */
                leftClick(robot);
                moveTo(robot, 560, 410 + yMoreHosts); /* create config */
                leftClick(robot);
                checkVMTest(cluster.getHostsArray()[0], vmTest, 1, name);
                checkVMTest(cluster.getHostsArray()[1], vmTest, 2, name);

                moveTo(robot, 410, 370); /* select first */
                leftClick(robot);
                moveTo(robot, 410, 405); /* deselect second */
                leftClick(robot);
                moveTo(robot, 560, 410 + yMoreHosts); /* create config */
                leftClick(robot);
                checkVMTest(cluster.getHostsArray()[0], vmTest, 2, name);
                checkVMTest(cluster.getHostsArray()[1], vmTest, 1, name);

                moveTo(robot, 410, 405); /* select second */
                leftClick(robot);
                moveTo(robot, 560, 410 + yMoreHosts); /* create config */
                leftClick(robot);
                checkVMTest(cluster, vmTest, 2, name);
            }

            sleepNoFactor(2000);
            moveTo(robot, 814, 570); /* finish */
            leftClick(robot);
            sleepNoFactor(5000);

            moveTo(robot, 620, 480 + yMoreHosts); /* number of cpus */
            sleep(1000);
            leftClick(robot);
            sleep(500);
            press(robot, KeyEvent.VK_BACK_SPACE);
            sleep(500);
            press(robot, KeyEvent.VK_2);
            sleep(500);
            moveTo(robot, 250, 190); /* apply */
            sleep(1000);
            leftClick(robot);
            sleep(1000);
            checkVMTest(cluster, vmTest, 3, name);

            /* disk readonly */
            moveTo(robot, 1100, 298);
            leftPress(robot); /* scroll bar */
            moveTo(robot, 1100, 410);
            leftRelease(robot);

            sleep(1000);
            moveTo(robot, 400, 450 + yMoreHosts); /* choose disk */
            sleep(1000);
            leftClick(robot);
            sleep(1000);

            moveTo(robot, 390, 540); /* readonly */
            sleep(1000);
            leftClick(robot);
            sleep(1000);
            moveTo(robot, 250, 190); /* apply */
            sleep(1000);
            leftClick(robot);
            checkVMTest(cluster, vmTest, 3.1, name);
            sleep(1000);
            moveTo(robot, 390, 645); /* readonly */
            sleep(1000);
            leftClick(robot);

            sleep(1000);
            moveTo(robot, 950, 180); /* host overview */
            sleep(1000);
            leftClick(robot);
            sleep(1000);

            moveTo(robot, 250, 190); /* host apply */
            leftClick(robot);
            checkVMTest(cluster, vmTest, 3.2, name);

            /* remove interface */

            // ...
            moveTo(robot, 1100, 410);
            leftPress(robot); /* scroll bar back */
            moveTo(robot, 1100, 200);
            leftRelease(robot);

            names.add(name);
            for (final String n : names) {
                checkVMTest(cluster, vmTest, 3, n);
            }
            name += "i";
        }

        sleepNoFactor(2000);
        moveTo(robot, 1066, 284); /* remove */
        leftClick(robot);
        sleepNoFactor(2000);
        moveTo(robot, 516, 480); /* confirm */
        leftClick(robot);
        sleepNoFactor(5000);
        for (int j = 1; j < count; j++) {
            moveTo(robot, 1066, 225); /* remove */
            leftClick(robot);
            sleepNoFactor(2000);
            moveTo(robot, 516, 480); /* confirm */
            leftClick(robot);
            sleepNoFactor(5000);
        }
    }

    private static void saveAndExit() {
        Tools.save(Tools.getConfigData().getSaveFile());
        sleepNoFactor(10000);
        System.exit(0);
    }

    private static void resetTerminalAreas(final Cluster cluster) {
        for (final Host h : cluster.getHosts()) {
            h.getTerminalPanel().resetTerminalArea();
        }
    }
}
