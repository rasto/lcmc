/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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

package lcmc.utilities;

import lcmc.Exceptions;
import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.configs.AppDefaults;
import lcmc.gui.widget.Widget;
import lcmc.gui.DrbdGraph;
import lcmc.gui.CRMGraph;
import lcmc.gui.resources.Info;
import java.awt.Robot;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.geom.Point2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.JLabel;
import javax.swing.AbstractButton;
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
    /** Confirm remove variable. */
    private static final boolean CONFIRM_REMOVE = true;
    /** Y position of Primitive/Clone/MS radio buttons. */
    private static final int CLONE_RADIO_Y = 140;
    /** Host y position. */
    private static final int HOST_Y = 100;
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

    private static final boolean PROXY = true;

    public static class Test {
        private final RoboTest.Type type;
        private final char index;

        public Test(final RoboTest.Type type, final char index) {
            this.type = type;
            this.index = index;
        }

        public RoboTest.Type getType() {
            return type;
        }

        public char getIndex() {
            return index;
        }
    }


    public enum Type {
        PCMK("pcmk"), DRBD("drbd"), VM("vm"), GUI("gui");

        private String testName;

        private Type(final String name) {
            testName = "start" + name + "test";
        }

        public String getTestName() {
            return testName;
        }
    }

    /** Private constructor, cannot be instantiated. */
    private RoboTest() {
        /* Cannot be instantiated. */
    }

    /** Abort if mouse moved. */
    private static boolean abortWithMouseMovement() {
        if (MouseInfo.getPointerInfo() == null) {
            return false;
        }
        Point2D p = getAppPosition();
        double x = p.getX();
        if (x > 1536 || x < -100) {
            int i = 0;
            while (x > 1536 || x < -100) {
                if (i % 10 == 0) {
                    info("sleep: " + x);
                }
                Tools.sleep(500);
                if (MouseInfo.getPointerInfo() != null) {
                    p = getAppPosition();
                    x = p.getX();
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
            @Override
            public void run() {
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
            @Override
            public void run() {
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
                final int xOffset = getOffset();
                final Point2D pos = getAppPosition();
                final int origX = (int) pos.getX();
                final int origY = (int) pos.getY();
                info("move mouse to the end position");
                sleepNoFactor(5000);
                final Point2D endP = getAppPosition();
                final int endX = (int) endP.getX();
                final int endY = (int) endP.getY();
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
    public static void startTest(final Test autoTest,
                                 final Cluster c) {
        final Type type = autoTest.getType();
        final char index = autoTest.getIndex();
        Tools.getGUIData().getMainFrame().setSize(
                                  Tools.getDefaultInt("DrbdMC.width"),
                                  Tools.getDefaultInt("DrbdMC.height") + 50);
        cluster = c;
        aborted = false;
        info("start test " + index + " in 3 seconds");
        if (cluster != null) {
            for (final Host host : cluster.getHosts()) {
                host.getSSH().installTestFiles();
            }
            final Host firstHost = cluster.getHostsArray()[0];
            Tools.getGUIData().setTerminalPanel(firstHost.getTerminalPanel());
        }
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
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
                if (type == Type.GUI) {
                    moveTo(30, 20);
                    leftClick();
                    final int count = 200;
                    if (index == '1') {
                        /* cluster wizard deadlock */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startGUITest1(count);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == '2') {
                        /* cluster wizard deadlock */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startGUITest2(count);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    }
                } else if (type == Type.PCMK) {
                    moveToMenu("CRM ");
                    leftClick();
                    final int count = 200;
                    if (index == '0') {
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
                            startTest3(4);
                            if (aborted) {
                                break;
                            }
                            startTest4();
                            if (aborted) {
                                break;
                            }
                            startTest5(5);
                            if (aborted) {
                                break;
                            }
                            startTest6(5);
                            if (aborted) {
                                break;
                            }
                            startTest7(5);
                            if (aborted) {
                                break;
                            }
                            startTest8(10);
                            if (aborted) {
                                break;
                            }
                            //startTest9();
                            if (aborted) {
                                break;
                            }
                            startTestA(5);
                            if (aborted) {
                                break;
                            }
                            startTestB(5);
                            if (aborted) {
                                break;
                            }
                            startTestC(5);
                            if (aborted) {
                                break;
                            }
                            startTestD(5);
                            if (aborted) {
                                break;
                            }
                            startTestE(5);
                            if (aborted) {
                                break;
                            }
                            startTestF(2);
                            if (aborted) {
                                break;
                            }
                            startTestG(5);
                            if (aborted) {
                                break;
                            }
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            i++;
                        }
                    } else if (index == '1') {
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
                    } else if (index == '2') {
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
                    } else if (index == '3') {
                        /* pacemaker drbd */
                        final int i = 1;
                        final long startTime = System.currentTimeMillis();
                        info("test" + index + " no " + i);
                        startTest3(200);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + " no " + i + ", secs: " + secs);
                    } else if (index == '4') {
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
                    } else if (index == '5') {
                        int i = 1;
                        while (!aborted) {
                            /* pacemaker */
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startTest5(10);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            i++;
                        }
                    } else if (index == '6') {
                        int i = 1;
                        while (!aborted) {
                            /* pacemaker */
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startTest6(10);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            i++;
                        }
                    } else if (index == '7') {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTest7(100);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == '8') {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTest8(30);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'a') {
                        /* pacemaker leak test group */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestA(200);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'b') {
                        /* pacemaker leak test clone */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestB(200);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'c') {
                        /* pacemaker master/slave test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestC(200);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'd') {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestD(count);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'e') {
                        /* host wizard deadlock */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestE(count);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'f') {
                        int i = 1;
                        while (!aborted) {
                            /* cloned group */
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startTestF(2);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            i++;
                        }
                    } else if (index == 'g') {
                        /* big group */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startTestG(15);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    }
                } else if (type == Type.DRBD) {
                    moveToMenu("Storage ");
                    leftClick();
                    Tools.getGUIData().expandTerminalSplitPane(1);
                    if (index == '0') {
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
                    } else if (index == '1') {
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
                    } else if (index == '2') {
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
                    } else if (index == '3') {
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
                    } else if (index == '4') {
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
                    } else if (index == '5') {
                        int i = 1;
                        final int blockDevY = getBlockDevY();
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startDRBDTest5(blockDevY);
                            if (aborted) {
                                break;
                            }
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                        }
                    } else if (index == '8') {
                        /* proxy */
                        int i = 1;
                        final int blockDevY = getBlockDevY();
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startDRBDTest8(blockDevY);
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
                } else if (type == Type.VM) {
                    moveToMenu("VMs ");
                    leftClick();
                    if (index == '1') {
                        /* VMs */
                        int i = 1;
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startVMTest1("vm-test" + index, 2);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                        }
                    } else if (index == '2') {
                        /* VMs */
                        int i = 1;
                        String testIndex = "1";
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startVMTest1("vm-test" + testIndex, 10);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                        }
                    } else if (index == '3') {
                        /* VMs */
                        int i = 1;
                        String testIndex = "1";
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startVMTest1("vm-test" + testIndex, 30);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                        }
                    } else if (index == '4') {
                        /* VMs dialog disabled textfields check. */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        startVMTest4("vm-test" + index, 100);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                        resetTerminalAreas();
                    } else if (index == '5') {
                        /* VMs */
                        int i = 1;
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            startVMTest5("vm-test" + index, 2);
                            final int secs = (int) (System.currentTimeMillis()
                                                     - startTime) / 1000;
                            info("test" + index + " no " + i + ", secs: "
                                 + secs);
                            resetTerminalAreas();
                            i++;
                        }
                    }
                }
                info(type + " test " + index + " done");
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

    /** Check DRBD test on the first two hosts. */
    private static void checkDRBDTest(final String test, final double no) {
        int h = 1;
        for (final Host host : cluster.getHosts()) {
            if (abortWithMouseMovement()) {
                return;
            }
            if (!aborted) {
                host.checkDRBDTest(test, no);
            }
            if (h == 2) {
                break;
            }
            h++;
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
        slowFactor = 0.4f;
        aborted = false;
        /* create IPaddr2 with 192.168.100.100 ip */
        final int ipX = 235;
        final int ipY = 207;
        final int gx = 230;
        final int gy = 305;
        final int popX = 340;
        final int popY = 257;
        final int statefulX = 500;
        final int statefulY = 207;
        String testName = "test1";
        final String pmV = cluster.getHostsArray()[0].getPacemakerVersion();
        try {
            if (pmV != null && Tools.compareVersions(pmV, "1.1.6") < 0) {
                testName = "test1-1.0";
            }
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
        }

        disableStonith();
        checkTest(testName, 1);
        enableStonith();
        checkTest(testName, 1.1);
        disableStonith();
        checkTest(testName, 1);
        moveTo(ipX, ipY);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Add Service");
        sleep(1000);
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("IPaddr2");
        leftClick();
        final float savedSlowFactor = slowFactor;
        slowFactor = 0.00001f;
        for (final Integer pos1 : new Integer[]{850, 900, 1000}) {
            for (final Integer pos2 : new Integer[]{850, 900, 1000}) {
                if (pos1 == pos2) {
                    continue;
                }
                for (int i = 0; i < 70; i++) {
                    moveTo(pos1, CLONE_RADIO_Y);
                    robot.mousePress(InputEvent.BUTTON1_MASK);
                    Tools.sleep(20);
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                    moveTo(pos2, CLONE_RADIO_Y);
                    robot.mousePress(InputEvent.BUTTON1_MASK);
                    Tools.sleep(20);
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                }
            }
        }
        slowFactor = savedSlowFactor;
        removeResource(ipX, ipY, !CONFIRM_REMOVE);
        /* again */
        moveTo(ipX, ipY);
        rightClick(); /* popup */
        moveTo("Add Service");
        sleep(1000);
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("IPaddr2");
        leftClick();

        moveTo("IPv4 address", Widget.MComboBox.class);
        leftClick();
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER);
        press(KeyEvent.VK_1);
        press(KeyEvent.VK_0);
        press(KeyEvent.VK_0);
        sleep(1000);
        setTimeouts(false);
        moveTo("Apply");
        sleep(6000); /* ptest */
        leftClick(); /* apply */
        /* CIDR netmask 24 */
        sleep(10000);

        moveTo("CIDR netmask", Widget.MTextField.class); /* CIDR */
        sleep(3000);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_2);
        sleep(200);
        press(KeyEvent.VK_4);
        sleep(1000);

        moveTo("Apply");
        sleep(6000); /* ptest */
        leftClick(); /* apply */

        checkTest(testName, 2); /* 2 */

        /* pingd */
        moveScrollBar(true);
        moveTo("pingd", Widget.MComboBox.class);
        leftClick();
        sleep(500);
        press(KeyEvent.VK_DOWN); /* no ping */
        sleep(200);
        press(KeyEvent.VK_DOWN); /* no ping */
        sleep(200);
        press(KeyEvent.VK_ENTER); /* no ping */
        sleep(200);
        moveTo("Apply");
        sleep(2000);
        leftClick();
        sleep(2000);
        checkTest(testName, 2.1); /* 2.1 */

        moveTo("pingd", Widget.MComboBox.class);
        leftClick();
        sleep(500);
        press(KeyEvent.VK_UP); /* no ping */
        sleep(200);
        press(KeyEvent.VK_UP); /* no ping */
        sleep(200);
        press(KeyEvent.VK_ENTER); /* no ping */
        sleep(200);
        moveTo("Apply");
        sleep(2000);
        leftClick();

        moveScrollBar(false);

        /* group with dummy resources */
        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        moveTo(gx + 46, gy + 11);
        sleep(1000);
        leftClick(); /* choose group */
        sleep(3000);
        /* remove it */
        removeResource(gx, gy, !CONFIRM_REMOVE);

        moveTo(gx, gy);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Add Group");
        leftClick();
        sleep(3000);

        rightClick(); /* group popup */
        sleep(1000);
        moveTo("Add Group Service");
        sleep(1000);
        moveTo("OCF Resource Agents");
        sleep(1000);
        typeDummy();
        sleep(1000);

        /* remove it */
        removeResource(gx, gy, !CONFIRM_REMOVE);

        /* group with dummy resources, once again */
        moveTo(gx, gy);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Add Group");
        leftClick();
        sleep(3000);
        rightClick(); /* group popup */
        sleep(1000);
        moveTo("Add Group Service");
        sleep(1000);
        moveTo("OCF Resource Agents");
        sleep(1000);
        typeDummy();
        sleep(1000);

        moveToMenu("Dummy (1)");
        rightClick();
        sleep(1000);

        moveTo("Remove Service");
        leftClick(); /* menu remove service */
        removeResource(gx, gy, !CONFIRM_REMOVE);

        /* group with dummy resources, once again */
        moveTo(gx, gy);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Add Group");
        leftClick();
        sleep(3000);
        rightClick(); /* group popup */
        sleep(1000);
        moveTo("Add Group Service");
        sleep(1000);
        moveTo("OCF Resource Agents");
        sleep(1000);
        typeDummy();
        sleep(1000);

        removeResource(gx, gy, !CONFIRM_REMOVE);

        /* once again */
        moveTo(gx, gy);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Add Group");
        leftClick();
        sleep(3000);

        checkTest(testName, 2); /* 2 */

        rightClick(); /* group popup */
        sleep(1000);
        moveTo("Add Group Service");
        sleep(1000);
        moveTo("OCF Resource Agents");
        sleep(1000);
        typeDummy();
        sleep(1000);

        setTimeouts(true);
        moveTo("Apply");
        sleep(2000);
        leftClick();
        for (int i = 0; i < 2; i++) {
            /* another group resource */
            moveTo(gx + 10, gy - 25);
            rightClick(); /* popup */
            sleep(10000);
            moveTo("Add Group Service");
            sleep(1000);
            moveTo("OCF Resource Agents");
            sleep(1000);
            typeDummy();
            sleep(i * 300);
            setTimeouts(true);
            moveTo("Apply");
            sleep(6000);
            leftClick();
            sleep(1000);
        }
        sleep(4000);
        checkTest(testName, 3); /* 3 */

        /* constraints */
        moveTo(gx + 10, gy - 25);
        addConstraint(1);
        checkTest(testName, 3.1); /* 3.1 */

        try {
            if (pmV != null && Tools.compareVersions(pmV, "1.0.8") > 0) {
                /* move up, move down */
                for (int i = 0; i < 2; i++) {
                    moveToMenu("Dummy (3)");
                    rightClick();
                    sleep(1000);
                    moveTo("Move Up");
                    leftClick(); /* move res 3 up */
                    sleepNoFactor(2000);
                    checkTest(testName, 3.11); /* 3.11 */
                    moveToMenu("Dummy (3)");
                    rightClick();
                    moveTo("Move Down");
                    leftClick();
                    sleepNoFactor(2000);
                    checkTest(testName, 3.12); /* 3.12 */
                }
            }
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
        }

        moveToMenu("Dummy (3)");
        sleep(1000);
        leftClick();

        moveScrollBar(true);

        for (int i = 0; i < 2; i++) {
            sleep(1000);
            moveTo("Same As", 2, Widget.MComboBox.class);
            sleep(2000);
            leftClick();
            sleep(1000);
            press(KeyEvent.VK_DOWN); /* choose another dummy */
            sleep(20000);
            press(KeyEvent.VK_DOWN);
            sleep(20000);
            press(KeyEvent.VK_ENTER);
            sleep(10000);
            moveTo("Apply");
            sleep(4000);
            leftClick();
            sleep(4000);
            checkTest(testName, 3.2); /* 3.2 */

            moveTo("Same As", 2, Widget.MComboBox.class);
            sleep(2000);
            leftClick();
            sleep(1000);

            press(KeyEvent.VK_PAGE_UP); /* choose "nothing selected */
            sleep(10000);
            press(KeyEvent.VK_ENTER);
            sleep(10000);

            moveTo("Apply");
            sleep(4000);
            leftClick();
            sleep(9000);
            checkTest(testName, 4); /* 4 */
            sleep(2000);
        }
        moveScrollBar(false);

        /* locations */
        moveTo(ipX + 20, ipY);
        leftClick(); /* choose ip */
        setLocation(new Integer[]{KeyEvent.VK_I});
        sleep(3000);
        checkTest(testName, 4.1); /* 4.1 */

        setLocation(new Integer[]{KeyEvent.VK_END,
                                  KeyEvent.VK_BACK_SPACE,
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
        checkTest(testName, 4.2); /* 4.2 */

        setLocation(new Integer[]{KeyEvent.VK_END,
                                  KeyEvent.VK_BACK_SPACE,
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
        checkTest(testName, 4.3); /* 4.3 */

        setLocation(new Integer[]{KeyEvent.VK_END,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE,
                                  KeyEvent.VK_BACK_SPACE});
        sleep(3000);
        checkTest(testName, 4.4); /* 4.4 */
        removeConstraint(popX, popY);
        checkTest(testName, 5); /* 5 */
        moveTo(gx + 10, gy - 25);
        addConstraint(1);
        checkTest(testName, 5.1); /* 4.4 */

        removeConstraint(popX, popY);
        sleep(3000);
        checkTest(testName, 5.2); /* 5 */
        sleep(1000);

        moveTo(gx + 10, gy - 25);
        addConstraint(1);
        sleep(5000);
        checkTest(testName, 6); /* 6 */

        removeOrder(popX, popY);
        sleep(4000);
        checkTest(testName, 7);

        addOrder(popX, popY);
        sleep(4000);
        checkTest(testName, 8);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 9);

        addColocation(popX, popY);
        sleep(4000);
        checkTest(testName, 10);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 10.1);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest(testName, 10.2);

        addConstraintOrderOnly(gx, gy - 30, 2);
        sleep(4000);
        checkTest(testName, 10.3);

        addColocation(popX, popY);
        sleep(4000);
        checkTest(testName, 10.4);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest(testName, 10.5);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 10.6);

        addConstraintColocationOnly(gx, gy - 30, 2);
        sleep(4000);
        checkTest(testName, 10.7);

        addOrder(popX, popY);
        sleep(4000);
        checkTest(testName, 10.8);

        removeConstraint(popX, popY);
        sleep(4000);
        checkTest(testName, 10.9);

        moveTo(ipX, ipY);
        addConstraint(1);
        sleep(5000);
        checkTest(testName, 10.91);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest(testName, 10.92);

        addOrder(popX, popY);
        sleep(5000);
        checkTest(testName, 10.93);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 10.94);

        addColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 10.95);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 10.96);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest(testName, 10.97);

        addConstraintColocationOnly(ipX, ipY, 1);
        sleep(5000);
        checkTest(testName, 10.98);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 10.99);

        addConstraintOrderOnly(ipX, ipY, 1);
        sleep(5000);
        checkTest(testName, 11);

        addColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 11.1);

        removeConstraint(popX, popY);
        sleep(5000);
        checkTest(testName, 11.2);

        moveTo(ipX, ipY);
        addConstraint(3);
        sleep(5000);
        checkTest(testName, 11.3);
        stopResource(ipX, ipY);
        sleep(5000);
        checkTest(testName, 11.4);
        resetStartStopResource(ipX, ipY);
        sleep(5000);
        checkTest(testName, 11.5);

        moveTo(ipX + 20, ipY + 10);
        leftClick(); /* choose ip */
        sleep(3000);
        moveTo("Menu"); /* actions menu stop */
        sleep(2000);
        rightClick();
        moveTo("Stop");
        sleep(6000);
        leftClick();
        sleep(5000);
        checkTest(testName, 11.501);

        moveTo(ipX + 20, ipY + 10);
        leftClick(); /* choose ip */
        sleep(10000);
        moveTo("Menu"); /* actions menu start */
        sleep(1000);
        rightClick(); /* popup */
        sleep(2000);
        moveTo("tart"); /* (Start) */
        sleep(6000);
        leftClick();
        sleep(5000);
        checkTest(testName, 11.502);

        resetStartStopResource(ipX, ipY);
        sleep(5000);
        checkTest(testName, 11.5);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 11.51);

        addColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 11.52);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest(testName, 11.53);

        addOrder(popX, popY);
        sleep(5000);
        checkTest(testName, 11.54);

        removeColocation(popX, popY);
        sleep(5000);
        checkTest(testName, 11.55);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest(testName, 11.56);

        addConstraintOrderOnly(ipX, ipY, 3);
        sleep(5000);
        checkTest(testName, 11.57);

        removeOrder(popX, popY);
        sleep(5000);
        checkTest(testName, 11.58);

        addConstraintColocationOnly(ipX, ipY, 3);
        sleep(5000);
        checkTest(testName, 11.59);

        addOrder(popX, popY);
        sleep(5000);
        checkTest(testName, 11.6);

        removeConstraint(popX, popY);
        sleep(5000);
        checkTest(testName, 11.7);

        moveTo(gx + 10, gy - 25);
        addConstraint(1);
        sleep(5000);
        checkTest(testName, 11.8);
        /** Add m/s Stateful resource */
        moveTo(statefulX, statefulY);
        rightClick();
        sleep(1000);
        moveTo("Add Service");
        sleep(1000);
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("OCF Resource Agents");
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

        moveTo("Apply");
        sleep(5000);
        leftClick();
        checkTest(testName, 11.9);
        sleep(3000);
        /* set clone max to 1 */
        moveTo("Clone Max", Widget.MComboBox.class);
        sleep(3000);
        leftClick();
        sleep(3000);
        leftClick();
        sleep(3000);
        press(KeyEvent.VK_BACK_SPACE);
        sleep(3000);
        press(KeyEvent.VK_1);
        sleep(3000);
        setTimeouts(false);
        moveTo("Apply");
        sleep(3000);
        leftClick();
        sleep(3000);
        checkTest(testName, 12);
        stopResource(statefulX, statefulY);
        sleep(3000);
        checkTest(testName, 13);

        startResource(statefulX, statefulY);
        sleep(10000);
        checkTest(testName, 14);
        unmanageResource(statefulX, statefulY);
        sleep(3000);
        checkTest(testName, 15);
        manageResource(statefulX, statefulY);
        sleep(3000);
        checkTest(testName, 16);

        /* IP addr cont. */
        stopResource(ipX, ipY);
        sleep(3000);
        checkTest(testName, 17);
        startResource(ipX, ipY);
        sleep(3000);
        checkTest(testName, 18);
        unmanageResource(ipX, ipY);
        sleep(3000);
        checkTest(testName, 19);
        manageResource(ipX, ipY);
        sleep(3000);
        checkTest(testName, 20);
        migrateResource(ipX, ipY);
        sleep(3000);
        checkTest(testName, 21);
        unmigrateResource(ipX, ipY);
        sleep(3000);
        checkTest(testName, 22);

        /* Group cont. */
        stopResource(gx, gy - 30);
        sleep(10000);
        checkTest(testName, 23);
        startResource(gx, gy - 30);
        sleep(20000);
        checkTest(testName, 24);
        unmanageResource(gx, gy - 30);
        sleep(5000);
        checkTest(testName, 25);
        manageResource(gx, gy - 30);
        sleep(5000);
        checkTest(testName, 26);
        migrateResource(gx, gy - 30);
        sleep(5000);
        moveTo(gx, gy);
        leftClick();
        checkTest(testName, 27);
        moveTo("Menu");
        sleep(6000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Remove Migration Constraint");
        sleep(12000); /* ptest */
        leftClick(); /* stop */
        sleep(20000);
        checkTest(testName, 28);

        moveTo(700, 450); /* rectangle */
        leftPress();
        moveTo(220, 65);
        leftRelease();

        moveTo(ipX, ipY);
        rightClick();
        sleep(500);
        moveTo(ipX + 30, ipY); /* ptest */
        moveToSlowly(ipX + 30, ipY + 350);
        moveTo(ipX, ipY);

        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_ENTER); /* standby selected hosts */
        checkTest(testName, 28.1);

        moveTo(700, 450);
        leftPress();
        moveTo(220, 65);
        leftRelease();
        moveTo(ipX, ipY);
        rightClick();
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_ENTER); /* online selected hosts */
        checkTest(testName, 28.2);

        moveTo(700, 450);
        leftPress();
        moveTo(220, 65);
        leftRelease();
        moveTo(ipX, ipY);
        rightClick();
        sleep(1000);
        moveTo("Stop Selected Services");
        leftClick();
        checkTest(testName, 28.3);

        moveTo(700, 450);
        leftPress();
        moveTo(220, 65);
        leftRelease();
        moveTo(ipX, ipY);
        rightClick();
        sleep(1000);
        moveTo("Start Selected Services");
        leftClick();
        checkTest(testName, 28.4);
        sleep(10000);
        moveTo(700, 520); /* reset selection */
        leftClick();

        stopResource(ipX, ipY);
        sleep(5000);
        moveTo(gx, gy);
        leftClick();
        moveTo("Menu");
        stopGroup();
        sleep(5000);
        moveTo(statefulX, statefulY);
        stopGroup();
        sleep(5000);
        checkTest(testName, 29);

        if (true) {
            removeResource(ipX, ipY, CONFIRM_REMOVE);
            sleep(5000);
            removeGroup(gx, gy - 20);
            sleep(5000);
            removeGroup(statefulX, statefulY);
        } else {
            removeEverything();
        }
        if (!aborted) {
            sleepNoFactor(20000);
        }
        checkTest(testName, 1);
    }


    /** Stop everything. */
    private static void stopEverything() {
        sleep(10000);
        moveTo("Advanced");
        sleep(2000);
        leftClick();
        sleep(2000);
        moveTo(700, 520);
        rightClick(); /* popup */
        sleep(3000);
        moveTo("Stop All Services");
        sleep(3000);
        leftClick();
        moveTo("Advanced"); /* not advanced */
        sleep(2000);
        leftClick();
        sleep(2000);
    }

    /** Remove everything. */
    private static void removeEverything() {
        sleep(10000);
        moveTo("Advanced");
        sleep(2000);
        leftClick();
        sleep(2000);
        moveTo(700, 520);
        rightClick(); /* popup */
        sleep(3000);
        moveTo("Remove All Services");
        sleep(3000);
        leftClick();
        dialogColorTest("remove everything");
        confirmRemove();
        sleep(3000);
        leftClick();
        moveTo("Advanced"); /* not advanced */
        sleep(2000);
        leftClick();
        sleep(2000);
    }

    /** Enable stonith if it is enabled. */
    private static void enableStonith() {
        moveTo(265, 202);
        leftClick(); /* global options */
        final String stonith = cluster.getBrowser()
                    .getClusterStatus().getGlobalParam("stonith-enabled");
        if (stonith != null && "false".equals(stonith)) {
            moveTo("Stonith Enabled", JCheckBox.class);
            leftClick(); /* enable stonith */
            moveTo("Apply All");
        }
        sleep(2000);
        leftClick();
    }

    /** Disable stonith if it is enabled. */
    private static void disableStonith() {

        moveTo(265, 202);
        leftClick(); /* global options */
        boolean apply = false;
        final String stonith = cluster.getBrowser()
                    .getClusterStatus().getGlobalParam("stonith-enabled");
        if (stonith == null || "true".equals(stonith)) {
            moveTo("Stonith Enabled", JCheckBox.class);
            leftClick();
            apply = true;
        }
        final String quorum = cluster.getBrowser()
                    .getClusterStatus().getGlobalParam("no-quorum-policy");
        if (!"ignore".equals(quorum)) {
            moveTo("No Quorum Policy", Widget.MComboBox.class);
            leftClick();
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_UP);
            press(KeyEvent.VK_UP);
            press(KeyEvent.VK_ENTER); /* ignore */
            apply = true;
        }
        if (apply) {
            moveTo("Apply All");
            sleep(2000);
            leftClick();
        }
    }

    /** TEST 2. */
    private static void startTest2() {
        slowFactor = 0.6f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 207;
        final int dummy2X = 545;
        final int dummy2Y = 207;
        final int dummy3X = 235;
        final int dummy3Y = 342;
        final int dummy4X = 545;
        final int dummy4Y = 342;
        final int phX = 445;
        final int phY = 342;

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
        moveTo("Placeholder (AND)");
        sleep(2000);
        leftClick();
        checkTest("test2", 3);
        /* constraints */
        moveTo(phX, phY);
        addConstraint(1); /* with dummy 1 */
        sleep(2000);
        moveTo("Apply");
        sleep(2000);
        leftClick();
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

        moveTo(dummy3X, dummy3Y);
        addConstraint(5); /* with ph */
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

        moveTo(phX, phY);
        addConstraint(1); /* with dummy 2 */
        sleep(5000);
        checkTest("test2", 10);
        moveTo(dummy4X, dummy4Y);
        addConstraint(5); /* with ph */
        sleep(5000);
        checkTest("test2", 11);

        /* ph -> dummy2 */
        final int dum2PopX = dummy2X - 10;
        final int dum2PopY = dummy2Y + 70;
        removeConstraint(dum2PopX, dum2PopY);
        sleep(4000);
        checkTest("test2", 11.1);
        addConstraintOrderOnly(phX, phY, 2); /* with dummy 2 */
        sleep(4000);
        checkTest("test2", 11.2);
        removeOrder(dum2PopX, dum2PopY);
        sleep(4000);
        checkTest("test2", 11.3);
        addConstraintColocationOnly(phX, phY, 2); /* with dummy 2 */
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
        addConstraintColocationOnly(dummy4X, dummy4Y, 5); /* with ph */
        sleep(4000);
        checkTest("test2", 11.7);
        removeColocation(dum4PopX, dum4PopY);
        sleep(4000);
        checkTest("test2", 11.8);
        moveTo(dummy4X + 20, dummy4Y + 5);
        sleep(1000);
        rightClick(); /* workaround for the next popup not working. */
        sleep(1000);
        addConstraintOrderOnly(dummy4X, dummy4Y, 5); /* ph 2 */
        sleep(4000);
        checkTest("test2", 11.9);
        addColocation(dum4PopX, dum4PopY);
        sleep(4000);
        checkTest("test2", 11.91);
        /* remove one dummy */
        stopResource(dummy1X, dummy1Y);
        sleep(5000);
        checkTest("test2", 11.92);
        removeResource(dummy1X, dummy1Y, CONFIRM_REMOVE);
        sleep(5000);
        checkTest("test2", 12);
        stopResource(dummy2X, dummy2Y);
        sleep(10000);
        stopResource(dummy3X, dummy3Y);
        sleep(10000);
        stopResource(dummy3X, dummy3Y);
        sleep(10000);
        stopResource(dummy4X, dummy4Y);
        stopEverything();
        sleep(10000);
        checkTest("test2", 12.5);
        if (maybe()) {
            /* remove placeholder */
            moveTo(phX , phY);
            rightClick();
            sleep(1000);
            moveTo("Remove");
            leftClick();
            confirmRemove();
            sleep(5000);

            /* remove rest of the dummies */
            removeResource(dummy2X, dummy2Y, CONFIRM_REMOVE);
            sleep(5000);
            checkTest("test2", 14);
            removeResource(dummy3X, dummy3Y, CONFIRM_REMOVE);
            sleep(5000);
            checkTest("test2", 15);
            removeResource(dummy4X, dummy4Y, CONFIRM_REMOVE);
            sleep(5000);
        } else {
            removeEverything();
            /* remove placeholder */
            moveTo(phX , phY);
            rightClick();
            sleep(1000);
            moveTo("Remove");
            leftClick();
            confirmRemove();
            sleep(5000);
        }
        if (!aborted) {
            sleepNoFactor(20000);
        }
        checkTest("test2", 16);
    }

    /** TEST 4. */
    private static void startTest4() {
        slowFactor = 0.6f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 207;

        final int dummy2X = 545;
        final int dummy2Y = 207;

        final int dummy3X = 235;
        final int dummy3Y = 346;

        final int dummy4X = 545;
        final int dummy4Y = 346;

        final int dummy5X = 235;
        final int dummy5Y = 505;

        final int dummy6X = 545;
        final int dummy6Y = 505;

        final int ph1X = 445;
        final int ph1Y = 266;

        final int ph2X = 445;
        final int ph2Y = 425;

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
            moveTo("Placeholder (AND)");
            sleep(2000);
            leftClick();

            moveTo(ph2X, ph2Y);
            rightClick();
            sleep(1000);
            moveTo("Placeholder (AND)");
            sleep(1000);
            leftClick();
            checkTest("test4", 2);

            /* constraints */
            /* menu dummy 5 with ph2 */
            moveToMenu("Dummy (5)");
            addConstraint(7);
            /* menu dummy 6 with ph2 */
            moveToMenu("Dummy (6)");
            addConstraint(7);

            /* with dummy 3 */
            moveTo(ph2X, ph2Y);
            addConstraint(3);
            /* with dummy 4 */
            moveTo(ph2X, ph2Y);
            addConstraint(3);

            /* with ph1 */
            moveTo(dummy3X, dummy3Y);
            addConstraint(4);
            /* with ph1 */
            moveTo(dummy4X, dummy4Y);
            addConstraint(4);

            moveTo(ph1X, ph1Y);
            addConstraint(1); /* with dummy 1 */
            moveTo(ph1X, ph1Y);
            addConstraint(1); /* with dummy 2 */
            checkTest("test4", 2);

        }
        moveTo("Apply");
        sleep(2000);
        leftClick();

        checkTest("test4", 3);
        stopEverything();
        checkTest("test4", 4);
        removeEverything();
        removePlaceHolder(ph1X, ph1Y, !CONFIRM_REMOVE);
        removePlaceHolder(ph2X, ph2Y, !CONFIRM_REMOVE);
        sleep(40000);
    }

    /** TEST 5. */
    private static void startTest5(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 207;

        final int dummy2X = 500;
        final int dummy2Y = 207;

        final int ph1X = 380;
        final int ph1Y = 452;


        disableStonith();
        /* create 2 dummies */
        checkTest("test5", 1);

        /* placeholders */
        moveTo(ph1X, ph1Y);
        rightClick();
        sleep(2000);
        moveTo("Placeholder (AND)");
        sleep(2000);
        leftClick();

        chooseDummy(dummy1X, dummy1Y, false, true);
        chooseDummy(dummy2X, dummy2Y, false, true);
        checkTest("test5", 2);

        moveTo(dummy2X, dummy2Y);
        addConstraint(2);
        sleep(20000);
        checkTest("test5", 2);
        moveTo(ph1X, ph1Y);
        addConstraint(1);

        moveTo(ph1X, ph1Y);
        sleep(2000);
        leftClick();
        sleep(2000);
        moveTo("Apply");
        sleep(2000);
        leftClick();
        checkTest("test5", 2.1);

        final int dum1PopX = dummy1X + 80;
        final int dum1PopY = dummy1Y + 60;
        removeConstraint(dum1PopX, dum1PopY);
        checkTest("test5", 2.5);
        /* constraints */
        for (int i = 1; i <= count; i++) {
            moveTo(dummy1X, dummy1Y);
            addConstraint(2);

            checkTest("test5", 3);

            removeConstraint(dum1PopX, dum1PopY);
            checkTest("test5", 2.5);

            moveTo(ph1X, ph1Y);
            addConstraint(1);

            checkTest("test5", 3.5);

            removeConstraint(dum1PopX, dum1PopY);
            checkTest("test5", 2.5);
            info("i: " + i);
        }
        stopEverything();
        checkTest("test5", 3.1);
        removeResource(dummy1X, dummy1Y, CONFIRM_REMOVE);
        removeResource(dummy2X, dummy2Y, CONFIRM_REMOVE);
        removePlaceHolder(ph1X, ph1Y, !CONFIRM_REMOVE);
        sleep(5000);
        checkTest("test5", 1);
    }

    /** TEST 6. */
    private static void startTest6(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 207;

        final int ph1X = 315;
        final int ph1Y = 346;


        //disableStonith();
        /* create 2 dummies */
        //checkTest("test5", 1);

        /* placeholders */
        moveTo(ph1X, ph1Y);
        rightClick();
        sleep(2000);
        moveTo("Placeholder (AND)");
        sleep(2000);
        leftClick();

        chooseDummy(dummy1X, dummy1Y, false, true);
        final int dum1PopX = dummy1X + 70;
        final int dum1PopY = dummy1Y + 60;
        for (int i = 0; i < count; i++) {
            if (i % 5 == 0) {
                info("test6 i: " + i);
            }
            moveTo(ph1X, ph1Y);
            addConstraint(1);
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

    private static void startTest7(final int count) {
        slowFactor = 0.5f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 207;
        disableStonith();
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("test7 I: " + i);
            }
            checkTest("test7", 1);
            /* create dummy */
            sleep(5000);
            chooseDummy(dummy1X, dummy1Y, false, true);
            checkTest("test7", 2);
            sleep(5000);
            stopResource(dummy1X, dummy1Y);
            checkTest("test7", 3);
            sleep(5000);
            /* copy/paste */
            moveTo(dummy1X + 10 , dummy1Y + 10);
            leftClick();
            robot.keyPress(KeyEvent.VK_CONTROL);
            press(KeyEvent.VK_C);
            press(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            moveTo(dummy1X + 10 , dummy1Y + 90);
            leftClick();
            moveTo("Apply");
            sleep(4000);
            leftClick();
            checkTest("test7", 4);

            removeResource(dummy1X, dummy1Y, CONFIRM_REMOVE);
            removeResource(dummy1X, dummy1Y + 90, CONFIRM_REMOVE);
        }
        System.gc();
    }

    private static void startTest8(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 540;
        final int dummy1Y = 202;
        disableStonith();
        checkTest("test8", 1);
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("test8 i: " + i);
            }
            //checkTest("test7", 1);
            sleep(5000);
            chooseDummy(dummy1X, dummy1Y, false, true);
            sleep(5000);
            moveTo(550, 202);
            leftPress(); /* move the reosurce */
            moveTo(300, 202);
            leftRelease();
        }
        checkTest("test8-" + count, 2);
        stopEverything();
        checkTest("test8-" + count, 3);
        removeEverything();
        checkTest("test8", 4);
        resetTerminalAreas();
    }

    private static void startTestA(final int count) {
        slowFactor = 0.5f;
        aborted = false;
        final int gx = 235;
        final int gy = 207;
        disableStonith();
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("testA I: " + i);
            }
            checkTest("testA", 1);
            /* group with dummy resources */
            moveTo(gx, gy);
            sleep(1000);
            rightClick(); /* popup */
            sleep(1000);
            moveTo("Add Group");
            leftClick();
            sleep(3000);
            /* create dummy */
            moveTo(gx + 46, gy + 11);
            rightClick(); /* group popup */
            sleep(2000);
            moveTo("Add Group Service");
            sleep(1000);
            moveTo("OCF Resource Agents");
            sleep(1000);
            typeDummy();
            sleep(300);
            setTimeouts(true);
            moveTo("Apply");
            sleep(6000);
            leftClick();
            sleep(6000);
            checkTest("testA", 2);
            stopResource(gx, gy);
            sleep(6000);
            checkTest("testA", 3);

            /* copy/paste */
            moveTo(gx + 10 , gy + 10);
            leftClick();
            robot.keyPress(KeyEvent.VK_CONTROL);
            press(KeyEvent.VK_C);
            press(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            moveTo(gx + 10 , gy + 90);
            leftClick();
            moveTo("Apply");
            sleep(4000);
            leftClick();
            checkTest("testA", 4);

            removeResource(gx, gy, CONFIRM_REMOVE);
            removeResource(gx, gy + 90, CONFIRM_REMOVE);
            resetTerminalAreas();
        }
        System.gc();
    }

    private static void startTestB(final int count) {
        slowFactor = 0.5f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 207;
        disableStonith();
        final String testName = "testB";
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info(testName + " I: " + i);
            }
            checkTest(testName, 1);
            /* create dummy */
            sleep(5000);
            chooseDummy(dummy1X, dummy1Y, true, true);
            checkTest(testName, 2);
            sleep(5000);
            stopResource(dummy1X, dummy1Y);
            checkTest(testName, 3);
            sleep(5000);
            /* copy/paste */
            moveTo(dummy1X + 10 , dummy1Y + 10);
            leftClick();
            robot.keyPress(KeyEvent.VK_CONTROL);
            press(KeyEvent.VK_C);
            press(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            moveTo(dummy1X + 10 , dummy1Y + 90);
            leftClick();
            moveTo("Apply");
            sleep(4000);
            leftClick();
            checkTest("testB", 4);

            removeResource(dummy1X, dummy1Y + 90, CONFIRM_REMOVE);
            removeResource(dummy1X, dummy1Y, CONFIRM_REMOVE);
            resetTerminalAreas();
        }
        System.gc();
    }

    private static void startTestC(final int count) {
        slowFactor = 0.5f;
        final int statefulX = 500;
        final int statefulY = 207;
        disableStonith();
        final String testName = "testC";
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info(testName + " I: " + i);
            }
            checkTest(testName, 1);
            /** Add m/s Stateful resource */
            moveTo(statefulX, statefulY);
            rightClick(); /* popup */
            sleep(1000);
            moveTo("Add Service");
            sleep(1000);
            moveTo("Filesystem + Linbit:DRBD");
            sleep(1000);
            moveTo("OCF Resource Agents");
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

            moveTo("Apply");
            sleep(1000);
            leftClick();
            sleep(4000);
            stopResource(statefulX, statefulY);
            checkTest(testName, 2);
            sleep(5000);
            /* copy/paste */
            moveTo(statefulX, statefulY);
            leftClick();
            robot.keyPress(KeyEvent.VK_CONTROL);
            press(KeyEvent.VK_C);
            press(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            moveTo(245, statefulY + 90);
            leftClick();
            moveTo("Apply");
            sleep(4000);
            leftClick();
            checkTest(testName, 4);


            removeResource(statefulX, statefulY, CONFIRM_REMOVE);
            removeResource(245, statefulY + 90, CONFIRM_REMOVE);
            resetTerminalAreas();
        }
    }

    /** Pacemaker Leak tests. */
    private static void startTestD(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 540;
        final int dummy1Y = 202;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("testD 1 I: " + i);
            }
            chooseDummy(dummy1X, dummy1Y, false, false);
            removeResource(dummy1X, dummy1Y, !CONFIRM_REMOVE);
        }
        chooseDummy(dummy1X, dummy1Y, false, false);
        int pos = 0;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("testD 2 I: " + i);
            }
            final double rand = Math.random();
            if (rand < 0.33) {
                if (pos == 1) {
                    continue;
                }
                pos = 1;
                moveTo(796, CLONE_RADIO_Y);
                leftClick();
            } else if (rand < 0.66) {
                if (pos == 2) {
                    continue;
                }
                pos = 2;
                moveTo(894, CLONE_RADIO_Y);
                leftClick();
            } else {
                if (pos == 3) {
                    continue;
                }
                pos = 3;
                moveTo(994, CLONE_RADIO_Y);
                leftClick();
            }
        }
        removeResource(dummy1X, dummy1Y, !CONFIRM_REMOVE);
    }

    /** Host wizard deadlock. */
    private static void startTestE(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                info("testE I: " + i);
            }
            moveTo(300 , HOST_Y); /* host */
            sleep(2000);
            rightClick();
            sleep(9000);
            moveTo("Host Wizard");
            sleep(2000);
            leftClick();
            sleep(30000);
            moveTo("Cancel");
            sleep(2000);
            leftClick();
            sleep(2000);
        }
    }

    /** Cloned group. */
    private static void startTestF(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        final int gx = 235;
        final int gy = 207;
        disableStonith();
        final String testName = "testF";
        final String distro = cluster.getHostsArray()[0].getDist();
        checkTest(testName, 1);
        /* group with dummy resources */
        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Add Group");
        leftClick(); /* choose group */
        sleep(1000);
        moveTo("Clone");
        leftClick(); /* clone */

        final int type = 1;
        //int type = 2;
        for (int i = count; i > 0; i--) {
            info("I: " + i);
            /* create dummy */
            moveToMenu("Group (1)");
            rightClick(); /* group popup */
            sleep(2000);
            moveTo("Add Group Service");
            sleep(2000);
            moveTo("OCF Resource Agents");
            sleep(1000);
            typeDummy();
            sleep(300);
            setTimeouts(true);
            if (type == 1) {
                moveTo("Apply");
                sleep(6000);
                leftClick();
                sleep(6000);
            }
        }
        if (type != 1) {
            moveTo("Apply");
            sleep(6000);
            leftClick();
            sleep(6000);
        }
        checkTest(testName, 2);
        /* set resource stickiness */
        moveTo("Resource Stickiness", Widget.MComboBox.class);
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_BACK_SPACE);
        sleep(200);
        press(KeyEvent.VK_2);
        sleep(1000);
        moveTo("Apply");
        sleep(6000);
        leftClick();
        sleep(6000);
        checkTest(testName, 3);

        stopResource(gx, gy);
        sleep(6000);
        checkTest(testName, 4);
        removeResource(gx, gy, CONFIRM_REMOVE);
        resetTerminalAreas();
        System.gc();
    }

    private static void startTestG(final int count) {
        slowFactor = 0.5f;
        aborted = false;
        final int gx = 235;
        final int gy = 207;
        disableStonith();
        checkTest("testG", 1);
        /* group with dummy resources */
        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Add Group");
        leftClick();
        sleep(3000);
        /* create dummy */
        moveTo(gx + 46, gy + 11);
        rightClick(); /* group popup */
        sleep(2000);

        for (int i = 0; i < count; i++) {
            /* another group resource */
            moveTo(gx + 10, gy - 25);
            rightClick(); /* popup */
            sleep(10000);
            moveTo("Add Group Service");
            sleep(1000);
            moveTo("OCF Resource Agents");
            sleep(1000);
            typeDummy();
            sleep(i * 300);
            setTimeouts(true);
            moveTo("Apply");
            sleep(6000);
            leftClick();
            sleep(1000);
        }
        checkTest("testG", 2);
        sleep(4000);
        stopResource(gx, gy);
        sleep(6000);
        checkTest("testG", 3);

        /* copy/paste */
        moveTo(gx + 10 , gy + 10);
        leftClick();
        robot.keyPress(KeyEvent.VK_CONTROL);
        press(KeyEvent.VK_C);
        press(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        moveTo(gx + 10 , gy + 90);
        leftClick();
        moveTo("Apply");
        sleep(4000);
        leftClick();
        checkTest("testG", 4);

        if (count < 10) {
            removeResource(gx, gy, CONFIRM_REMOVE);
            removeResource(gx, gy + 90, CONFIRM_REMOVE);
            resetTerminalAreas();
        }
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
            moveTo("Add Host / Wizard");
            sleep(500);
            leftClick();
            sleep(1000);
            if (!isColor(360, 462, new Color(255, 100, 100), true)) {
                info("gui-test1 1: error");
                break;
            }
            boolean ok = false;
            for (int error = 0; error < 5; error++) {
                sleep(100);
                press(KeyEvent.VK_X);
                if (!isColor(360, 462, new Color(255, 100, 100), false)) {
                    sleepNoFactor(1000);
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                info("gui-test1 2: failed");
                break;
            }
            moveTo("Cancel"); /* cancel */
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

            moveTo("Add Cluster / Wizard");
            sleep(500);
            leftClick();
            sleep(2000);
            if (!isColor(336, 472, new Color(184, 207, 229), true)) {
                info("gui-test2: error");
                break;
            }
            moveTo("Cancel"); /* cancel */
            sleep(500);
            leftClick();
            sleep(1000);
        }
    }

    /** Sets location. */
    private static void setLocation(final Integer[] events) {
        moveScrollBar(true);

        moveTo("on ", Widget.MComboBox.class);
        sleep(1000);
        leftClick();
        sleep(1000);
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
        moveTo("Apply");
        sleep(4000);
        leftClick();
        sleep(2000);

        moveScrollBar(false);

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
        sleep(3000);
        moveScrollBar(true);
        moveTo("Start / Timeout", Widget.MTextField.class);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_2);
        sleep(2000);
        press(KeyEvent.VK_0);
        sleep(2000);
        press(KeyEvent.VK_0);
        sleep(5000);

        moveTo("Stop / Timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_1);
        sleep(200);
        press(KeyEvent.VK_9);
        sleep(200);
        press(KeyEvent.VK_2);
        sleep(5000);

        moveTo("Monitor / Timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_1);
        sleep(200);
        press(KeyEvent.VK_5);
        sleep(200);
        press(KeyEvent.VK_4);
        sleep(5000);

        moveTo("Monitor / Interval", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_1);
        sleep(200);
        press(KeyEvent.VK_2);
        sleep(200);
        press(KeyEvent.VK_1);
        sleep(5000);
        if (migrateTimeouts) {
            moveTo("Reload / Timeout", Widget.MTextField.class);
            leftClick();
            press(KeyEvent.VK_BACK_SPACE);
            sleep(200);
            press(KeyEvent.VK_BACK_SPACE);
            sleep(5000);

            moveTo("Migrate_to / Timeout", Widget.MTextField.class);
            leftClick();
            press(KeyEvent.VK_1);
            sleep(200);
            press(KeyEvent.VK_2);
            sleep(200);
            press(KeyEvent.VK_3);
            sleep(5000);

            moveTo("Migrate_from / T", Widget.MTextField.class);
            leftClick();
            press(KeyEvent.VK_1);
            sleep(200);
            press(KeyEvent.VK_2);
            sleep(200);
            press(KeyEvent.VK_2);
            sleep(5000);
        }

        moveScrollBar(false);
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
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Add Service");
        sleep(1000);
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("OCF Resource Agents");
        sleep(2000);
        typeDummy();
        if (apply) {
            sleep(2000);
            setTimeouts(true);
            if (clone) {
                moveTo(893, CLONE_RADIO_Y);
                leftClick(); /* clone */
            }
            moveTo("Apply");
            sleep(4000);
            leftClick();
            sleep(2000);
        }
    }

    /** Removes service. */
    private static void removeResource(final int x,
                                       final int y,
                                       final boolean confirm) {
        moveTo(x + 20, y);
        rightClick();
        sleep(1000);
        moveTo("Remove Service");
        leftClick();
        if (confirm == CONFIRM_REMOVE) {
            confirmRemove();
        }
    }

    /** Removes group. */
    private static void removeGroup(final int x, final int y) {
        moveTo(x + 20, y);
        rightClick();
        sleep(10000);
        moveTo("Remove Service");
        leftClick();
        confirmRemove();
    }

    /** Removes placeholder. */
    private static void removePlaceHolder(final int x,
                                          final int y,
                                          final boolean confirm) {
        moveTo(x + 20, y);
        rightClick();
        sleep(1000);
        moveTo("Remove");
        leftClick();
        if (confirm) {
            confirmRemove();
        }
    }

    /** Confirms remove dialog. */
    private static void confirmRemove() {
        sleep(1000);
        dialogColorTest("confirm remove");
        press(KeyEvent.VK_TAB);
        sleep(500);
        press(KeyEvent.VK_TAB);
        sleep(500);
        press(KeyEvent.VK_SPACE);
        sleep(500);
    }

    /** Stops resource. */
    private static void stopResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(2000);
        rightClick(); /* popup */
        moveTo("Stop");
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Stops group. */
    private static void stopGroup() {
        rightClick(); /* popup */
        sleep(3000);
        moveTo("Stop");
        sleep(10000); /* ptest */
        leftClick(); /* stop */
    }

    /** Removes target role. */
    private static void resetStartStopResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        leftClick(); /* select */

        moveTo("Target Role", Widget.MComboBox.class);
        sleep(2000);
        leftClick(); /* pull down */
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_UP);
        sleep(500);
        press(KeyEvent.VK_UP);
        sleep(500);
        leftClick();
        moveTo("Apply");
        sleep(6000); /* ptest */
        leftClick();
    }

    /** Starts resource. */
    private static void startResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        moveTo("tart");
        sleep(6000);
        leftClick();
    }

    /** Migrate resource. */
    private static void migrateResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Migrate FROM");
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Unmigrate resource. */
    private static void unmigrateResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(6000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Remove Migration Constraint");
        sleep(12000); /* ptest */
        leftClick(); /* stop */
    }

    /** Unmanage resource. */
    private static void unmanageResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Do not manage by CRM");
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Manage resource. */
    private static void manageResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Manage by CRM");
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Adds constraint from vertex. */
    private static void addConstraint(final int number) {
        rightClick();
        sleep(5000);
        moveTo("Start Before");
        sleep(1000);
        leftClick();
        for (int i = 0; i < number; i++) {
            press(KeyEvent.VK_DOWN);
            sleep(500);
        }
        press(KeyEvent.VK_ENTER);
    }

    /** Adds constraint (order only) from vertex. */
    private static void addConstraintOrderOnly(final int x,
                                               final int y,
                                               final int number) {

        moveTo(x + 20, y + 5);
        rightClick();
        sleep(1000);
        moveTo("Start Before");
        sleep(500);
        leftClick();
        press(KeyEvent.VK_TAB);
        sleep(200);
        press(KeyEvent.VK_SPACE); /* disable colocation */
        sleep(200);
        press(KeyEvent.VK_TAB);
        sleep(200);
        press(KeyEvent.VK_TAB); /* list */
        sleep(200);

        for (int i = 0; i < number; i++) {
            press(KeyEvent.VK_DOWN);
            sleep(200);
        }
        press(KeyEvent.VK_ENTER);
    }

    /** Adds constraint (colocation only) from vertex. */
    private static void addConstraintColocationOnly(final int x,
                                                    final int y,
                                                    final int number) {
        moveTo(x + 20, y + 5);
        rightClick();
        sleep(1000);
        moveTo("Start Before");
        sleep(500);
        leftClick();
        press(KeyEvent.VK_TAB);
        sleep(200);
        press(KeyEvent.VK_TAB);
        sleep(200);
        press(KeyEvent.VK_SPACE); /* disable order */
        sleep(200);
        press(KeyEvent.VK_TAB); /* list */
        sleep(200);

        for (int i = 0; i < number; i++) {
            press(KeyEvent.VK_DOWN);
            sleep(200);
        }
        press(KeyEvent.VK_ENTER);
    }

    /** Removes constraint. */
    private static void removeConstraint(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(10000);
        rightClick(); /* constraint popup */
        sleep(2000);
        moveTo("Remove Colocation and Order");
        sleep(6000); /* ptest */
        leftClick(); /* remove ord */
    }

    /** Removes order. */
    private static void removeOrder(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        sleep(1000);
        moveTo("Remove Order");
        sleep(6000); /* ptest */
        leftClick(); /* remove ord */
    }

    /** Adds order. */
    private static void addOrder(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        sleep(1000);
        moveTo("Add Order");
        sleep(6000); /* ptest */
        leftClick(); /* add ord */
    }

    /** Removes colocation. */
    private static void removeColocation(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        sleep(1000);
        moveTo("emove Colocation");
        sleep(6000); /* ptest */
        leftClick(); /* remove col */
    }

    /** Adds colocation. */
    private static void addColocation(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        sleep(1000);
        moveTo("dd Colocation"); /* match from end */
        sleep(6000); /* ptest */
        leftClick(); /* add col */
    }

    /** TEST 3. */
    private static void startTest3(final int count) {
        slowFactor = 0.3f;
        aborted = false;
        disableStonith();
        final String testName = "test3";
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info(testName + " I: " + i);
            }
            checkTest(testName, 1);
            /* filesystem/drbd */
            moveTo(577, 205);
            rightClick(); /* popup */
            sleep(1000);
            moveTo("Add Service");
            sleep(1000);
            moveTo("Filesystem + Linbit:DRBD");
            leftClick(); /* choose fs */

            moveTo("block device", Widget.MComboBox.class); /* choose drbd */
            leftClick();
            sleep(2000);
            press(KeyEvent.VK_DOWN);
            sleep(200);
            press(KeyEvent.VK_DOWN);
            sleep(200);
            press(KeyEvent.VK_ENTER);

            moveTo("mount point", Widget.MComboBox.class);
            leftClick();
            sleep(2000);
            press(KeyEvent.VK_DOWN);
            sleep(200);
            press(KeyEvent.VK_DOWN);
            sleep(200);
            press(KeyEvent.VK_ENTER);

            moveTo("filesystem type", Widget.MComboBox.class);
            leftClick();
            sleep(2000);
            press(KeyEvent.VK_E);
            sleep(200);
            press(KeyEvent.VK_E);
            sleep(200);
            press(KeyEvent.VK_ENTER);

            moveTo("Apply");
            leftClick();
            sleep(2000);
            checkTest(testName, 2);
            checkNumberOfVertices(testName, 4);
            stopEverything();
            checkTest(testName, 3);
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

    /** Control left click. */
    private static void controlLeftClick()  {
        robot.keyPress(KeyEvent.VK_CONTROL);
        leftClick();
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }

    /** Left click. */
    private static void leftClick()  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        Tools.sleep(400);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Tools.sleep(300);
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
                     Tools.getGUIData().getMainFrameContentPane()
                                        .getLocationOnScreen();
        final int appX = (int) appP.getX() + fromX;
        final int appY = (int) appP.getY() + fromY;
        for (int i = 0; i < 7; i++) {
            boolean isColor = false;
            for (int y = -20; y < 20; y++) {
                if (i > 0) {
                    moveTo(fromX - i, fromY + y);
                }
                if (expected) {
                    if (color.equals(
                                robot.getPixelColor(appX + xOffset - i,
                                                    appY + y))) {
                        return true;
                    }
                } else {
                    if (color.equals(
                              robot.getPixelColor(appX + xOffset - i,
                                                  appY + y))) {
                        isColor = true;
                    }
                }
                if (aborted) {
                    return false;
                }
            }
            if (!expected && !isColor) {
                return false;
            }
            Tools.sleep(500 * i);
        }
        return !expected;
    }

    private static void moveToSlowly(final int toX, final int toY) {
        slowFactor *= 50;
        moveTo(toX, toY);
        slowFactor /= 50;
    }

    /** Move to position. */
    private static void moveTo(final int toX, final int toY) {
        if (aborted) {
            return;
        }
        prevP = null;
        final Point2D origP = MouseInfo.getPointerInfo().getLocation();
        final int origX = (int) origP.getX();
        final int origY = (int) origP.getY();
        final Point2D endP =
            Tools.getGUIData().getMainFrameContentPane().getLocationOnScreen();
        final int endX = (int) endP.getX() + toX;
        final int endY = (int) endP.getY() + toY;
        moveToAbs(endX, endY);
    }

    private static int getY() {
        final Point2D origP = MouseInfo.getPointerInfo().getLocation();
        final Point2D endP =
            Tools.getGUIData().getMainFrameContentPane().getLocationOnScreen();
        return (int) origP.getY() - (int) endP.getY();
    }

    private static int getX() {
        final Point2D origP = MouseInfo.getPointerInfo().getLocation();
        final Point2D endP =
            Tools.getGUIData().getMainFrameContentPane().getLocationOnScreen();
        return (int) origP.getX() - (int) endP.getX();
    }

    private static void moveTo(final String text) {
        if (aborted) {
            return;
        }
        moveTo(text, null);
    }

    private static void moveScrollBar(final boolean down) {
        moveScrollBar(down, 300);
    }

    private static void moveScrollBar(final boolean down, final int delta) {
        if (aborted) {
            return;
        }
        final List<Component> res = new ArrayList<Component>();
        try {
            findInside(Tools.getGUIData().getMainFrame(),
                       Class.forName("javax.swing.JScrollPane$ScrollBar"),
                       res);
        } catch (ClassNotFoundException e) {
            Tools.printStackTrace("can't find the scrollbar");
            return;
        }
        Component scrollbar = null;
        final Component app = Tools.getGUIData().getMainFrameContentPane();
        final int mX =
                  (int) app.getLocationOnScreen().getX() + app.getWidth() / 2;
        final int mY =
                  (int) app.getLocationOnScreen().getY() + app.getHeight() / 2;
        int scrollbarX = 0;
        int scrollbarY = 0;
        for (final Component c : res) {
            final Point2D p = c.getLocationOnScreen();
            final int pX = (int) p.getX();
            final int pY = (int) p.getY();
            if (pX > mX && pY < mY) {
                scrollbar = c;
                scrollbarX = pX + c.getWidth() / 2;
                scrollbarY = pY + c.getHeight() / 2;
            }
        }
        if (scrollbar == null) {
            Tools.printStackTrace("can't find the scrollbar");
        } else {
            moveToAbs(scrollbarX, scrollbarY);
            leftPress();
            if (down) {
                moveToAbs(scrollbarX, scrollbarY + delta);
            } else {
                moveToAbs(scrollbarX, scrollbarY - delta);
            }
            leftRelease();
        }
    }

    private static Component getFocusedWindow() {
        for (final Window w : Window.getWindows()) {
            Component c = w.getFocusOwner();
            if (c != null) {
                while (c.getParent() != null
                       && !(c instanceof JDialog || c instanceof JFrame)) {
                    c = c.getParent();
                }
                return c;
            }
        }
        return null;
    }

    private static void moveTo(final Class<?> clazz, final int number) {
        if (aborted) {
            return;
        }
        Component c = null;
        int i = 0;
        while (c == null && i < 60 && !aborted) {
            c = findInside(getFocusedWindow(), clazz, number);
            sleepNoFactor(1000);
            i++;
        }
        if (aborted) {
            return;
        }
        if (c == null) {
            Tools.printStackTrace("can't find: " + clazz);
            return;
        }
        final Point2D endP = c.getLocationOnScreen();
        final int endX = (int) endP.getX() + c.getWidth() / 2;
        final int endY = (int) endP.getY() + c.getHeight() / 2;
        moveToAbs(endX, endY);
    }

    private static void moveToMenu(final String text) {
        if (aborted) {
            return;
        }
        final JTree tree = (JTree) findInside(Tools.getGUIData().getMainFrame(),
                                              JTree.class,
                                              0);
        if (tree == null) {
            info("can't find the tree");
        }
        for (int i = 0; i < tree.getRowCount(); i++) {
            final String item =
                        tree.getPathForRow(i).getLastPathComponent().toString();
            if (item.startsWith(text) || item.endsWith(text)) {
                moveToAbs((int) (tree.getLocationOnScreen().getX()
                                 + tree.getRowBounds(i).getX()) + 2,
                          (int) (tree.getLocationOnScreen().getY()
                                 + tree.getRowBounds(i).getY()) + 2);
                return;
            }
        }
        Tools.info("can't find " + text + " the tree");
    }

    private static void moveToGraph(final String text) {
        if (aborted) {
            return;
        }
        final DrbdGraph graph = cluster.getBrowser().getDrbdGraph();
        for (final Info i : graph.infoToVertexKeySet()) {
            final String item = graph.getMainText(graph.getVertex(i), false);
            if (item.startsWith(text) || item.endsWith(text)) {
                final Point2D loc = graph.getLocation(i);
                moveToAbs((int) (graph.getVisualizationViewer()
                                      .getLocationOnScreen().getX()
                                 + loc.getX()),
                          (int) (graph.getVisualizationViewer()
                                      .getLocationOnScreen().getY()
                                 + loc.getY()));
                return;
            }
        }
        Tools.info("can't find " + text + " in the graph");
    }

    private static void moveTo(final String text, final Class<?> clazz) {
        moveTo(text, 1, clazz);
    }

    private static void moveTo(final String text,
                               final int number,
                               final Class<?> clazz) {
        if (aborted) {
            return;
        }
        Component c = null;
        int i = 0;
        while (c == null && i < 30 && !aborted) {
            c = (Component) findComponent(text, number);
            if (i > 0) {
                Tools.info("can't find: " + text);
            }
            sleepNoFactor(1000);
            i++;
        }
        if (aborted) {
            return;
        }
        if (c == null) {
            Tools.printStackTrace("can't find: " + text);
            return;
        }
        Component n;
        if (clazz == null) {
            n = c;
        } else {
            n = findNext(c, clazz);
            if (n == null) {
                Tools.printStackTrace("can't find: " + text + " -> " + clazz);
                return;
            }
        }
        final Point2D endP = n.getLocationOnScreen();
        final int endX = (int) endP.getX() + 15;
        final int endY = (int) endP.getY() + c.getHeight() / 2;
        moveToAbs(endX, endY);
    }

    private static void moveToAbs(final int endX, final int endY) {
        if (aborted) {
            return;
        }
        final int xOffset = getOffset();
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
        final Robot robot0 = rbt;
        info("start register movement in 3 seconds");
        sleepNoFactor(3000);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Point2D prevP = new Point2D.Double(0, 0);
                Point2D prevPrevP = new Point2D.Double(0, 0);
                while (true) {
                    final Point2D newPos = getAppPosition();

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
        info("move to position, start in 3 seconds");
        sleepNoFactor(3000);
        final Point2D pos = getAppPosition();
        final int x = (int) pos.getX();
        final int y = (int) pos.getY();
        if (y > 532 || x < 150) {
            return 160;
        }
        return y;
    }

    private static boolean dialogColorTest(final String text) {
        if (aborted) {
            return false;
        }
        sleepNoFactor(2000);
        final Component dialog = getFocusedWindow();
        for (int i = 0; i < 60; i++) {
            if (dialog instanceof JDialog || aborted) {
                break;
            }
            sleepNoFactor(1000);
        }
        if (!(dialog instanceof JDialog) || aborted) {
            info(text + ": color test: no dialog");
            return false;
        }
        moveToAbs((int) dialog.getLocationOnScreen().getX() + 5,
                  (int) dialog.getLocationOnScreen().getY() + 40);
        final Point2D p = getAppPosition();
        if (!isColor((int) p.getX(),
                     (int) p.getY(),
                     AppDefaults.BACKGROUND,
                     true)) {
            info(text + ": color test: error");
            return false;
        } else {
            return true;
        }
    }

    private static void addDrbdResource(final int blockDevY) {
        moveTo(334, blockDevY); /* add drbd resource */
        rightClick();
        sleep(1000);
        moveTo("Add Mirrored Disk");
        sleep(1000);
        moveTo(cluster.getHostsArray()[1].getName());
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_ENTER);
        sleep(20000);
        dialogColorTest("addDrbdResource");
    }

    private static void drbdNext() {
        press(KeyEvent.VK_ENTER);
    }

    private static void newDrbdResource() {
        drbdNext();
        sleep(10000);
        dialogColorTest("newDrbdResource");
    }

    private static void chooseDrbdResourceInterface(final String hostName,
                                                    final boolean proxy) {
        moveTo("on " + hostName, Widget.MComboBox.class); /* interface */
        leftClick();
        sleep(500);
        if (proxy) {
            press(KeyEvent.VK_P); /* select first interface to proxy*/
        } else {
            press(KeyEvent.VK_E); /* select first interface */
        }
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(1000);
        dialogColorTest("chooseDrbdResourceInterface");
    }

    private static void chooseDrbdResource() {
        chooseDrbdResourceInterface(cluster.getHostsArray()[0].getName(),
                                    !PROXY);
        chooseDrbdResourceInterface(cluster.getHostsArray()[1].getName(),
                                    !PROXY);

        drbdNext();
        sleep(10000);
        dialogColorTest("chooseDrbdResource");
    }

    private static void addDrbdVolume() {
        drbdNext();
        sleep(10000);
        dialogColorTest("addDrbdVolume");
    }

    private static void addBlockDevice() {
        drbdNext();
        sleep(10000);
        dialogColorTest("addBlockDevice");
    }

    private static void addMetaData() {
        drbdNext();
        sleep(30000);
        dialogColorTest("addMetaData");
    }

    private static void addFileSystem() {
        /* do nothing. */
        dialogColorTest("addFileSystem");
    }

    private static void removeDrbdVolume(final boolean really) {
        if (aborted) {
            return;
        }
        moveTo(480, 152); /* rsc popup */
        rightClick(); /* remove */
        moveTo("Remove DRBD Volume"); /* remove */
        leftClick();
        Tools.sleep(10000);
        dialogColorTest("removeDrbdVolume");
        if (really) {
            confirmRemove();
        } else {
            press(KeyEvent.VK_ENTER); /* cancel */
        }
    }

    private static void createPV(final int blockDevX, final int blockDevY) {
        moveTo(blockDevX, blockDevY);
        rightClick();
        sleep(1000);
        moveTo("Create PV");
        leftClick();
        sleep(5000);
    }

    private static void pvRemove(final int blockDevX, final int blockDevY) {
        moveTo(blockDevX, blockDevY);
        rightClick();
        sleep(3000);
        moveTo("Remove PV");
        leftClick();
    }

    private static void createVG(final int blockDevY) {
        moveTo(334, blockDevY);
        rightClick();
        sleep(1000);
        moveTo("Create VG");
        leftClick();
        sleep(2000);
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();
        moveTo("Create VG"); /* button */
        leftClick();
    }

    private static void createVGMulti(final int blockDevY) {
        moveTo(334, blockDevY);
        rightClick();
        sleep(1000);
        moveTo("Create VG");
        leftClick();
        sleep(2000);
        moveTo("Create VG"); /* button */
        leftClick();
    }

    private static void createLV() {
        rightClick();
        sleep(1000);
        moveTo("Create LV in VG");
        leftClick();
        sleep(2000);
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();
        moveTo("Create"); /* button */
        leftClick();
        sleep(3000);
        moveTo("Close");
        leftClick();
    }

    private static void createLVMulti() {
        rightClick();
        moveTo("Create LV in VG");
        leftClick();
        sleep(2000);
        moveTo("Create"); /* button */
        leftClick();
        sleep(3000);
        moveTo("Close");
        leftClick();
    }

    private static void resizeLV() {
        rightClick();
        sleep(1000);
        moveTo("Resize LV");
        leftClick();
        sleep(5000);
        press(KeyEvent.VK_2);
        press(KeyEvent.VK_5);
        press(KeyEvent.VK_2);
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();

        moveTo("Resize");
        leftClick();
        sleep(3000);
        moveTo("Close");
        leftClick();
    }

    private static void vgRemove() {
        rightClick();
        sleep(1000);
        moveTo("Remove VG");
        leftClick();
        sleep(2000);
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();
        moveTo("Remove VG"); /* button */
        leftClick();
    }

    private static void lvRemove() {
        sleep(5000);
        rightClick();
        sleep(5000);
        moveTo("Remove LV");
        leftClick();
        sleep(2000);
        moveTo("Remove"); /* button */
        leftClick();
    }

    private static void lvRemoveMulti() {
        sleep(5000);
        rightClick();
        sleep(5000);
        moveTo("Remove selected LV");
        leftClick();
        sleep(2000);
        moveTo("Remove"); /* button */
        leftClick();
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
        sleep(1000);
        addBlockDevice();
        sleep(20000);
        addMetaData();
        addFileSystem();
        sleep(5000);
        moveTo("Finish");
        leftClick();
        sleep(10000);
        checkDRBDTest(drbdTest, 1.1);
        for (int i = 0; i < 2; i++) {
            info("i: " + i);
            removeDrbdVolume(false);
        }
        removeDrbdVolume(true);
        checkDRBDTest(drbdTest, 2);

    }

    /** DRBD Test 2. */
    private static void startDRBDTest2(final int blockDevY) {
        final String drbdTest = "drbd-test1";
        slowFactor = 0.2f;
        aborted = false;
        for (int i = 0; i < 2; i++) {
            info(drbdTest + "/1");
            addDrbdResource(blockDevY);

            moveTo("Cancel");
            leftClick();
            sleep(2000);
        }

        info(drbdTest + "/2");
        addDrbdResource(blockDevY);
        chooseDrbdResource();

        moveTo("Cancel");
        leftClick();
        sleep(20000);

        info(drbdTest + "/3");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();

        moveTo("Cancel");
        leftClick();
        sleep(10000);

        info(drbdTest + "/4");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();
        sleep(20000);

        moveTo("Cancel");
        leftClick();
        sleep(10000);

        info(drbdTest + "/5");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(20000);
        checkDRBDTest(drbdTest, 1);

        moveTo("Cancel");
        leftClick();
        confirmRemove();
        sleep(60000);

        info(drbdTest + "/6");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(20000);
        addMetaData();

        moveTo("Cancel");
        leftClick();
        confirmRemove();
        sleep(20000);

        info(drbdTest + "/7");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(20000);
        addMetaData();
        addFileSystem();
        checkDRBDTest(drbdTest, 1.1);
        sleep(10000);

        moveTo("Cancel");
        sleep(2000);
        leftClick();
        sleep(20000);
        confirmRemove();
        sleep(20000);

        info(drbdTest + "/8");
        addDrbdResource(blockDevY);
        chooseDrbdResource();
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(20000);
        addMetaData();
        addFileSystem();
        moveTo("Finish");
        leftClick();
        sleep(10000);
        checkDRBDTest(drbdTest, 1.1);

        for (int i = 0; i < 3; i++) {
            removeDrbdVolume(false);
        }
        removeDrbdVolume(true);
        checkDRBDTest(drbdTest, 2);
    }

    /** DRBD Test 3. */
    private static void startDRBDTest3(final int blockDevY) {
        /* Two drbds. */
        final String drbdTest = "drbd-test3";
        slowFactor = 0.2f;
        aborted = false;
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
            sleep(20000);

            if (offset == 0) {
                checkDRBDTest(drbdTest, 1.1);
            } else {
                checkDRBDTest(drbdTest, 1.2);
            }
            sleep(10000);
            addMetaData();
            addFileSystem();
            sleep(10000);
            moveTo("Finish");
            leftClick();
            sleep(10000);

            offset += 40;
        }
        checkDRBDTest(drbdTest, 2);

        moveTo(730, 475); /* rectangle */
        leftPress();
        moveTo(225, 65);
        leftRelease();

        moveTo(334, blockDevY);
        rightClick();
        moveToSlowly(400, blockDevY + 160);

        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_ENTER); /* detach */
        checkDRBDTest(drbdTest, 2.01);

        moveTo(400, blockDevY);
        rightClick();
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_ENTER); /* attach */
        checkDRBDTest(drbdTest, 2.02);

        moveTo(480, 152); /* select r0 */
        leftClick();

        moveTo("Protocol", Widget.MComboBox.class);
        leftClick();
        press(KeyEvent.VK_UP); /* protocol b */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Fence peer", Widget.MComboBox.class);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        sleep(200);
        press(KeyEvent.VK_DOWN); /* select dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);
        Tools.getGUIData().expandTerminalSplitPane(1);

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_9);
        sleep(2000);

        moveTo("Max buffers", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_5);
        sleep(1000);
        moveTo("Max buffers", Widget.MComboBox.class); /* Unit */
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_DOWN);
        sleep(1000);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveScrollBar(true);
        final String v = cluster.getHostsArray()[0].getDrbdVersion();
        try {
            if (v != null && Tools.compareVersions(v, "8.4.0") < 0) {
                moveTo("After", Widget.MComboBox.class);
            } else {
                moveTo("after", Widget.MComboBox.class);
            }
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
        }
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_DOWN);
        sleep(1000);
        press(KeyEvent.VK_ENTER);
        sleep(1000);

        moveScrollBar(false);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        checkDRBDTest(drbdTest, 2.1); /* 2.1 */


        /* common */
        moveTo(500, 342); /* select background */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_3);
        sleep(2000);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        sleep(10000);
        checkDRBDTest(drbdTest, 2.11); /* 2.11 */
        moveTo("Wfc timeout", Widget.MTextField.class);
        sleep(6000);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();

        /* resource */
        moveTo(480, 152); /* select r0 */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo("Protocol", Widget.MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN); /* protocol c */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Fence peer", Widget.MComboBox.class);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        sleep(200);
        press(KeyEvent.VK_UP); /* deselect dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_5);
        sleep(2000);

        moveTo("Max buffers", Widget.MTextField.class);
        leftClick();
        sleep(1000);
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_2);
        sleep(500);
        press(KeyEvent.VK_0);
        sleep(500);
        press(KeyEvent.VK_4);
        sleep(500);
        press(KeyEvent.VK_8);
        sleep(500);
        moveTo("Max buffers", Widget.MComboBox.class); /* Unit */
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_UP);
        sleep(1000);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveScrollBar(true);
        try {
            if (v != null && Tools.compareVersions(v, "8.4.0") < 0) {
                moveTo("After", Widget.MComboBox.class);
            } else {
                moveTo("after", Widget.MComboBox.class);
            }
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
        }
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_UP);
        sleep(1000);
        press(KeyEvent.VK_ENTER);
        sleep(1000);

        moveScrollBar(false);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        checkDRBDTest(drbdTest, 2.2); /* 2.2 */

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick();
        checkDRBDTest(drbdTest, 2.3); /* 2.3 */

        moveTo(480, 152); /* rsc popup */
        rightClick();
        moveTo("Remove DRBD Volume");
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 3);
        moveTo(480, 152); /* rsc popup */
        rightClick();
        moveTo("Remove DRBD Volume");
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
        int protocolY = 552;
        int correctionY = 0;
        if (!cluster.getHostsArray()[0].hasVolumes()) {
            protocolY = 352;
            correctionY = 30;
        }
        for (int i = 0; i < 2; i++) {
            addDrbdResource(blockDevY + offset);
            if (i == 0) {
                /* first one */
                chooseDrbdResource();
            } else {
                /* existing drbd resource */
                moveTo("DRBD Resource", Widget.MComboBox.class);
                leftClick();
                press(KeyEvent.VK_DOWN); /* drbd: r0 */
                sleep(200);
                press(KeyEvent.VK_ENTER);

                drbdNext();
                sleep(10000);

                //addDrbdVolume();
            }
            addDrbdVolume();

            addBlockDevice();
            addBlockDevice();
            sleep(20000);
            if (offset == 0) {
                checkDRBDTest(drbdTest, 1.1);
            } else {
                checkDRBDTest(drbdTest, 1.2);
            }
            sleep(10000);

            addMetaData();
            addFileSystem();
            sleep(20000);
            moveTo("Finish"); /* fs */
            leftClick();
            sleep(10000);

            if (offset == 0) {
                checkDRBDTest(drbdTest, 1);
            }
            offset += 40;
        }
        checkDRBDTest(drbdTest, 2);

        moveTo(480, 152); /* select r0 */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo("Protocol", Widget.MComboBox.class);
        leftClick();
        press(KeyEvent.VK_UP); /* protocol b */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Fence peer", Widget.MComboBox.class);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        sleep(200);
        press(KeyEvent.VK_DOWN); /* select dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_9);
        sleep(2000);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        checkDRBDTest(drbdTest, 2.1); /* 2.1 */


        /* common */
        moveTo(500, 342); /* select background */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_3);
        sleep(2000);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        sleep(10000);
        checkDRBDTest(drbdTest, 2.11); /* 2.11 */
        moveTo("Wfc timeout", Widget.MTextField.class);
        sleep(6000);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);
        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();

        /* resource */
        moveTo(480, 152); /* select r0 */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo("Protocol", Widget.MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN); /* protocol c */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Fence peer", Widget.MComboBox.class);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        sleep(200);
        press(KeyEvent.VK_UP); /* deselect dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_5);
        sleep(2000);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        checkDRBDTest(drbdTest, 2.2); /* 2.2 */

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick();
        checkDRBDTest(drbdTest, 2.3); /* 2.3 */

        moveTo(480, 152); /* rsc popup */
        rightClick();
        moveTo("Remove DRBD Volume");
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 3);
        moveTo(480, 152); /* rsc popup */
        rightClick();
        moveTo("Remove DRBD Volume");
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 4);
    }

    /** Create LV. */
    private static void startDRBDTest5(final int blockDevY) {
        /* Two bds. */
        slowFactor = 0.6f;
        aborted = false;
        /* multi */
        Tools.info("create pvs");
        moveTo(334, blockDevY);
        leftClick();

        moveTo(534, getY());
        controlLeftClick();

        moveTo(334, blockDevY + 40);
        controlLeftClick();

        moveTo(534, blockDevY + 40);
        controlLeftClick();

        createPV(334, blockDevY);
        Tools.info("create vgs");
        createVGMulti(blockDevY);
        sleepNoFactor(5000);
        Tools.info("create lvs");
        moveToGraph("VG vg00");
        sleepNoFactor(5000);
        createLVMulti();
        Tools.info("remove lvs");
        moveTo(334, blockDevY);
        leftClick();
        moveTo(534, blockDevY);
        controlLeftClick();
        lvRemoveMulti();
        Tools.info("remove vgs");
        sleepNoFactor(5000);
        moveToGraph("VG vg00");
        leftClick();
        moveTo(534, getY());
        controlLeftClick();
        rightClick();
        sleep(1000);
        moveTo("Remove selected VGs");
        leftClick();
        sleep(1000);
        moveTo("Remove VG"); /* button */
        leftClick();
        Tools.info("remove pvs");
        moveTo(334, blockDevY);
        leftClick();
        moveTo(534, blockDevY);
        controlLeftClick();
        moveTo(334, blockDevY + 40);
        controlLeftClick();
        moveTo(534, blockDevY + 40);
        controlLeftClick();
        rightClick();
        sleep(3000);
        moveTo("Remove selected PVs");
        sleep(2000);
        leftClick();

        moveTo(430, 90);
        leftClick(); // reset selection

        /* single */
        for (int i = 0; i < 2; i++) {
            Tools.info("create pv 1 " + i);
            createPV(334, blockDevY);
            Tools.info("create pv 2 " + i);
            createPV(534, blockDevY);
            Tools.info("create vg " + i);
            createVG(blockDevY);
            sleepNoFactor(10000);
            Tools.info("create lv" + i);
            moveToGraph("VG vg0" + i);
            createLV();
            Tools.info("resize lv" + i);
            moveToGraph("vg0" + i + "/lvol0");
            resizeLV();
        }
        int offset = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                moveToGraph("vg0" + i + "/lvol0");
                Tools.info("remove lv " + i + " " + j);
                lvRemove();
                sleepNoFactor(10000);
            }
            Tools.info("remove vg " + i);
            moveToGraph("VG vg0" + i);
            vgRemove();
            sleepNoFactor(10000);
            pvRemove(334, blockDevY + offset);
            pvRemove(534, blockDevY + offset);
            offset += 40;
        }
    }

    /** DRBD Test 8 / proxy. */
    private static void startDRBDTest8(final int blockDevY) {
        /* Two drbds. */
        final String drbdTest = "drbd-test8";
        slowFactor = 0.2f;
        aborted = false;
        int offset = 0;
        for (int i = 0; i < 2; i++) {
            addDrbdResource(blockDevY + offset);
            if (i == 1 && cluster.getHostsArray()[0].hasVolumes()) {
                newDrbdResource();
            }
            chooseDrbdResourceInterface(cluster.getHostsArray()[0].getName(),
                                        PROXY);
            chooseDrbdResourceInterface(cluster.getHostsArray()[1].getName(),
                                        PROXY);

            moveTo(700, 450);
            leftClick();
            robot.mouseWheel(70);

            moveTo(Tools.getString("DrbdResourceInfo.ProxyOutsideIp"),
                   Widget.MComboBox.class); /* outside */
            leftClick();
            sleep(500);
            press(KeyEvent.VK_E);
            sleep(500);
            press(KeyEvent.VK_ENTER);

            moveTo(Widget.MComboBox.class, 8); /* outside */
            leftClick();
            sleep(500);
            press(KeyEvent.VK_E);
            sleep(500);
            press(KeyEvent.VK_ENTER);

            drbdNext();
            sleep(10000);
            dialogColorTest("chooseDrbdResource");

            addDrbdVolume();
            addBlockDevice();
            addBlockDevice();
            sleep(20000);

            if (offset == 0) {
                checkDRBDTest(drbdTest, 1.1);
            } else {
                checkDRBDTest(drbdTest, 1.2);
            }
            sleep(10000);
            addMetaData();
            addFileSystem();
            sleep(10000);
            moveTo("Finish");
            leftClick();
            sleep(10000);

            offset += 40;
        }
        checkDRBDTest(drbdTest, 2);

        moveTo(730, 475); /* rectangle */
        leftPress();
        moveTo(225, 65);
        leftRelease();

        moveTo(334, blockDevY);
        rightClick();
        moveToSlowly(400, blockDevY + 160);

        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_ENTER); /* detach */
        checkDRBDTest(drbdTest, 2.01);

        moveTo(400, blockDevY);
        rightClick();
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_ENTER); /* attach */
        checkDRBDTest(drbdTest, 2.02);

        moveTo(480, 152); /* select r0 */
        leftClick();

        moveTo(900, 300);
        robot.mouseWheel(100);

        moveTo("Protocol", Widget.MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN); /* protocol b */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Fence peer", Widget.MComboBox.class);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        sleep(200);
        press(KeyEvent.VK_DOWN); /* select dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);
        Tools.getGUIData().expandTerminalSplitPane(1);

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_9);
        sleep(2000);

        moveTo("Max buffers", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_5);
        sleep(1000);
        moveTo("Max buffers", Widget.MComboBox.class); /* Unit */
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_DOWN);
        sleep(1000);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveScrollBar(true);
        sleep(5000);
        final String v = cluster.getHostsArray()[0].getDrbdVersion();
        try {
            if (v != null && Tools.compareVersions(v, "8.4.0") < 0) {
                moveTo("After", Widget.MComboBox.class);
            } else {
                moveTo("after", Widget.MComboBox.class);
            }
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
        }
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_DOWN);
        sleep(1000);
        press(KeyEvent.VK_ENTER);
        sleep(1000);

        moveScrollBar(false);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        checkDRBDTest(drbdTest, 2.1); /* 2.1 */


        /* common */
        moveTo(500, 342); /* select background */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_3);
        sleep(2000);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        sleep(10000);
        checkDRBDTest(drbdTest, 2.11); /* 2.11 */
        moveTo("Wfc timeout", Widget.MTextField.class);
        sleep(6000);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();

        /* resource */
        moveTo(480, 152); /* select r0 */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo(900, 300);
        robot.mouseWheel(100);

        moveTo("Protocol", Widget.MComboBox.class);
        leftClick();
        press(KeyEvent.VK_UP); /* protocol a */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Fence peer", Widget.MComboBox.class);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        sleep(200);
        press(KeyEvent.VK_UP); /* deselect dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_5);
        sleep(2000);

        moveTo("Max buffers", Widget.MTextField.class);
        leftClick();
        sleep(1000);
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_2);
        sleep(500);
        press(KeyEvent.VK_0);
        sleep(500);
        press(KeyEvent.VK_4);
        sleep(500);
        press(KeyEvent.VK_8);
        sleep(500);
        moveTo("Max buffers", Widget.MComboBox.class); /* Unit */
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_UP);
        sleep(1000);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveScrollBar(true);
        try {
            if (v != null && Tools.compareVersions(v, "8.4.0") < 0) {
                moveTo("After", Widget.MComboBox.class);
            } else {
                moveTo("after", Widget.MComboBox.class);
            }
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
        }
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_UP);
        sleep(1000);
        press(KeyEvent.VK_ENTER);
        sleep(1000);

        moveScrollBar(false);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        checkDRBDTest(drbdTest, 2.2); /* 2.2 */

        moveTo("Wfc timeout", Widget.MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);

        moveTo("Apply");
        sleep(6000); /* test */
        leftClick();
        checkDRBDTest(drbdTest, 2.3); /* 2.3 */

        moveTo(480, 152); /* rsc popup */
        rightClick();
        moveTo("Remove DRBD Volume");
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 3);
        moveTo(480, 152); /* rsc popup */
        rightClick();
        moveTo("Remove DRBD Volume");
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 4);
    }

    /** VM Test 1. */
    private static void startVMTest(final String vmTest,
                                    final String type,
                                    final int count) {
        slowFactor = 0.6f;
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
            moveToMenu("VMs (KVM");
            rightClick();
            sleep(2000);
            moveTo("Add New Virtual Machine");
            leftClick();
            sleep(10000);
            dialogColorTest("new domain");
            moveTo("Domain name", Widget.MTextField.class); /* domain name */
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
            /* type */
            moveTo("Domain Type", Widget.MComboBox.class);
            leftClick();
            sleep(1000);
            if ("lxc".equals(type)) {
                press(KeyEvent.VK_DOWN);
                sleep(1000);
                press(KeyEvent.VK_DOWN);
                sleep(1000);
                press(KeyEvent.VK_DOWN);
                sleep(1000);
                press(KeyEvent.VK_ENTER);
                sleep(3000);
            }

            /* next */
            moveTo("Next");
            leftClick();
            sleep(2000);

            if ("lxc".equals(type)) {
                /* filesystem */
                dialogColorTest("filesystem");
                moveTo("Source Dir", Widget.MComboBox.class);
                sleep(2000);
                leftClick();
                sleep(2000);
                press(KeyEvent.VK_END);
                sleep(200);
                press(KeyEvent.VK_DOWN);
                sleep(200);
                press(KeyEvent.VK_DOWN);
                sleep(200);
                press(KeyEvent.VK_ENTER);
                sleep(200);
                press(KeyEvent.VK_SLASH);
                sleep(200);
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
                press(KeyEvent.VK_SLASH);
                sleep(200);
                press(KeyEvent.VK_R);
                sleep(200);
                press(KeyEvent.VK_O);
                sleep(200);
                press(KeyEvent.VK_O);
                sleep(200);
                press(KeyEvent.VK_T);
                sleep(200);
                press(KeyEvent.VK_F);
                sleep(200);
                press(KeyEvent.VK_S);
                sleep(2000);
                moveTo("Next");
                leftClick();
            } else {
                /* source file */
                dialogColorTest("source file");

                moveTo("File", Widget.MComboBox.class);
                sleep(2000);
                leftClick();
                sleep(2000);
                press(KeyEvent.VK_END);
                sleep(200);
                press(KeyEvent.VK_T);
                sleep(200);
                press(KeyEvent.VK_E);
                sleep(200);
                press(KeyEvent.VK_S);
                sleep(200);
                press(KeyEvent.VK_T);
                sleep(2000);
                for (int i = 0; i < count2; i++) {
                    moveTo("Disk/block device");
                    leftClick();
                    sleep(1000);
                    moveTo("Image file");
                    leftClick();
                    sleep(1000);
                }

                moveTo("Next");
                leftClick();
                sleep(5000);
                dialogColorTest("disk image");
                moveTo("Next");
                leftClick();
            }
            sleep(5000);
            dialogColorTest("network");
            for (int i = 0; i < count2; i++) {
                moveTo("bridge");
                leftClick();
                sleep(1000);
                moveTo("network");
                leftClick();
                sleep(1000);
            }
            moveTo("Next");
            leftClick();
            sleep(10000);
            if (!"lxc".equals(type)) {
                dialogColorTest("display");
                for (int i = 0; i < count2; i++) {
                    moveTo("sdl"); /* sdl */
                    leftClick();
                    sleep(1000);
                    moveTo("vnc"); /* vnc */
                    leftClick();
                    sleep(1000);
                }
                moveTo("Next");
                leftClick();
                sleep(20000);
            }
            dialogColorTest("create config");

            sleep(10000);
            moveTo("Create Config");
            sleep(4000);
            leftClick();
            checkVMTest(vmTest, 2, name);
            sleep(8000);


            final String firstHost = cluster.getHostsArray()[0].getName();
            final String secondHost = cluster.getHostsArray()[1].getName();
            if (cluster.getHosts().size() > 1) {
                for (int i = 0; i < 3; i++) {
                    /* two hosts */
                    moveTo(firstHost, JCheckBox.class); /* deselect first */
                    leftClick();
                    sleep(10000);
                    moveTo("Create Config");
                    sleep(2000);
                    leftClick();
                    checkVMTest(cluster.getHostsArray()[0], vmTest, 1, name);
                    checkVMTest(cluster.getHostsArray()[1], vmTest, 2, name);

                    moveTo(firstHost, JCheckBox.class); /* select first */
                    sleep(1000);
                    leftClick();
                    sleep(1000);
                    moveTo(secondHost, JCheckBox.class); /* deselect second */
                    sleep(1000);
                    leftClick();
                    sleep(10000);
                    moveTo("Create Config");
                    sleep(2000);
                    leftClick();
                    checkVMTest(cluster.getHostsArray()[0], vmTest, 2, name);
                    checkVMTest(cluster.getHostsArray()[1], vmTest, 1, name);

                    moveTo(secondHost, JCheckBox.class); /* select second */
                    leftClick();
                    sleep(10000);
                    moveTo("Create Config");
                    sleep(4000);
                    leftClick();
                    checkVMTest(vmTest, 2, name);
                }
            }

            sleepNoFactor(2000);
            moveTo("Finish"); /* finish */
            leftClick();
            sleepNoFactor(5000);

            moveTo("Number of CPUs", Widget.MTextField.class);
            sleep(1000);
            leftClick();
            sleep(500);
            press(KeyEvent.VK_BACK_SPACE);
            sleep(500);
            press(KeyEvent.VK_2);
            sleep(500);
            moveTo("Apply");
            sleep(1000);
            leftClick();
            sleep(1000);
            checkVMTest(vmTest, 3, name);

            if (j  == 0) {
                for (int i = 0; i < count2; i++) {
                    /* remove net interface */
                    moveToMenu("dmc");
                    leftClick();
                    moveToMenu("default (:");
                    rightClick();
                    moveTo("Remove");
                    leftClick();
                    confirmRemove();
                    checkVMTest(vmTest, 3.001, name);

                    /* add net interface */
                    moveToMenu("dmc");
                    rightClick();
                    sleep(1000);
                    moveTo("Add Hardware");
                    sleep(1000);
                    moveTo("New Disk");
                    moveTo("New Network Interface");
                    leftClick();
                    sleep(2000);
                    moveTo("network");
                    leftClick();
                    sleep(2000);
                    moveTo("Apply");
                    leftClick();
                    checkVMTest(vmTest, 3, name);
                }
            }
            checkVMTest(vmTest, 3, name);

            if (j  == 0 && !"lxc".equals(type)) {
                /* add disk */
                moveToMenu("dmc");
                rightClick();
                sleep(2000);
                moveTo("Add Hardware");
                sleep(1000);
                moveTo("New Disk");
                leftClick();
                sleep(2000);
                moveTo("Disk/block device");
                leftClick();
                sleep(2000);
                moveTo("Device", Widget.MComboBox.class);
                leftClick();
                press(KeyEvent.VK_SLASH);
                press(KeyEvent.VK_D);
                press(KeyEvent.VK_E);
                press(KeyEvent.VK_V);

                press(KeyEvent.VK_SLASH);
                press(KeyEvent.VK_S);
                press(KeyEvent.VK_D);
                press(KeyEvent.VK_A);
                press(KeyEvent.VK_1);
                press(KeyEvent.VK_ENTER);
                moveTo("Apply");
                leftClick();
                checkVMTest(vmTest, 3.01, name);

                /* remove disk */
                moveToMenu("hdb (IDE");
                rightClick();
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_ENTER); /* remove */
                confirmRemove();
                checkVMTest(vmTest, 3, name);
            }

            if (!"lxc".equals(type)) {
                /* disk readonly */
                moveToMenu("hda (IDE"); /* popup */
                leftClick();
                sleep(1000);
                moveTo("Readonly", JCheckBox.class);
                sleep(1000);
                leftClick();
                sleep(1000);
                moveTo("Apply"); /* apply */
                sleep(1000);
                leftClick();
                checkVMTest(vmTest, 3.1, name);
                sleep(1000);
                Tools.getGUIData().expandTerminalSplitPane(1);
                moveTo("Readonly", JCheckBox.class);
                sleep(1000);
                leftClick();

                sleep(1000);
                moveTo("VM Host Overview"); /* host overview */
                sleep(1000);
                leftClick();
                sleep(1000);

                moveTo("Apply"); /* host apply */
                leftClick();
                checkVMTest(vmTest, 3.2, name);
            }

            names.add(name);
            for (final String n : names) {
                checkVMTest(vmTest, 3, n);
            }
            name += "i";
        }

        sleepNoFactor(5000);
        for (int j = 0; j < count; j++) {
            moveToMenu("dmc");
            rightClick();
            sleep(1000);
            moveTo("Remove Domain");
            leftClick();
            sleepNoFactor(2000);
            dialogColorTest("remove VM");
            confirmRemove();
            leftClick();
            sleepNoFactor(5000);
        }
    }

    /** VM Test 1. */
    private static void startVMTest1(final String vmTest,
                                     final int count) {
        startVMTest(vmTest, "kvm", count);
    }

    /** Cluster wizard locked until focus is lost. */
    private static void startVMTest4(final String vmTest, final int count) {
        slowFactor = 0.1f;
        aborted = false;
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                info(vmTest + " I: " + i);
            }
            moveTo("Add New Virtual Machine"); /* new VM */
            sleep(500);
            leftClick();
            sleep(1000);
            moveTo("Domain name", Widget.MTextField.class);
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
                sleep(100);
                press(KeyEvent.VK_X);
                if (!isColor((int) p.getX(),
                             (int) p.getY(),
                             new Color(255, 100, 100),
                             false)) {
                    sleepNoFactor(1000);
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                info(vmTest + " 2: failed");
                break;
            }
            moveTo("Cancel"); /* cancel */
            sleep(500);
            leftClick();
            sleep(1000);
        }
    }

    /** VM Test 1. */
    private static void startVMTest5(final String vmTest,
                                     final int count) {
        startVMTest(vmTest, "lxc", count);
    }

    private static void saveAndExit() {
        Tools.save(Tools.getConfigData().getSaveFile(), false);
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

    public static Component findInside(final Component component,
                                       final Class<?> clazz,
                                       final int position) {
        final List<Component> res = new ArrayList<Component>();
        findInside(component, clazz, res);
        if (res.size() > position) {
            return res.get(position);
        }
        return null;
    }

    public static void findInside(final Component component,
                                  final Class<?> clazz,
                                  final List<Component> results) {
        if (component.getClass().equals(clazz)
                   && component.isShowing()) {
            results.add(component);
        }
        if (component instanceof Container) {
            for (final Component c : ((Container) component).getComponents()) {
                findInside(c, clazz, results);
            }
        }
    }

    /** Find component that is next to the specified component and is of the
     * specified class. */
    public static Component findNext(final Component component,
                                     final Class<?> clazz) {
        boolean next = false;
        for (final Component c : component.getParent().getComponents()) {
            if (next) {
                final Component f = findInside(c, clazz, 0);
                if (f != null) {
                    return f;
                }
                if (c.getClass().equals(clazz)) {
                    return c;
                }
            } else {
                if (c == component) {
                    next = true;
                }
            }
        }
        return null;
    }

    private static Container findComponent(final String text,
                                           final Container component,
                                           final Integer[] number) {
        if (component instanceof AbstractButton) {
            if (component.isShowing() && component.isEnabled()) {
                final String t = ((AbstractButton) component).getText();
                if (t != null && (t.startsWith(text) || t.endsWith(text))) {
                    if (number[0] <= 1) {
                        return component;
                    } else {
                        number[0]--;
                    }
                }
            }
        } else if (component instanceof JLabel) {
            if (component.isShowing()) {
                final String t = ((JLabel) component).getText();
                if (t != null && (t.startsWith(text) || t.endsWith(text))) {
                    if (number[0] <= 1) {
                        return component;
                    } else {
                        number[0]--;
                    }
                }
            }
        }
        if (component == null) {
            Tools.info("can't find " + text);
            return null;
        }
        for (final Component c : component.getComponents()) {
            if (c instanceof Container) {
                final Container ret =
                                    findComponent(text, (Container) c, number);
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    public static Container findComponent(final String text) {
        return findComponent(text, 1);
    }

    public static Container findComponent(final String text, final int number) {
        return findComponent(text,
                             (Container) getFocusedWindow(),
                             new Integer[]{number});
    }

    private static Point2D getAppPosition() {
        final Point2D loc =
             Tools.getGUIData().getMainFrameContentPane().getLocationOnScreen();
        final Point2D pos = MouseInfo.getPointerInfo().getLocation();
        final Point2D newPos = new Point2D.Double(pos.getX() - loc.getX(),
                                                  pos.getY() - loc.getY());
        return newPos;
    }

    private static void waitForMe() {
        info("waiting...");
        final Point2D iPos = MouseInfo.getPointerInfo().getLocation();
        while (true) {
            final Point2D pos = MouseInfo.getPointerInfo().getLocation();
            if (Math.abs(iPos.getX() - pos.getX()) > 10
                || Math.abs(iPos.getY() - pos.getY()) > 10) {
                break;
            }
            Tools.sleep(100);
        }
        prevP = getAppPosition();
        info("continue...");
    }

    private static void checkNumberOfVertices(final String name,
                                              final int should) {
        if (aborted) {
            return;
        }
        final CRMGraph graph = cluster.getBrowser().getCRMGraph();
        int i = 0;
        while (i < 10 && should != graph.getNumberOfVertices()) {
            Tools.info(name
                       + " number of vertices: "
                       + should + " -> "
                       + graph.getNumberOfVertices());
            Tools.sleep(1000);
            i++;
        }
    }
}
