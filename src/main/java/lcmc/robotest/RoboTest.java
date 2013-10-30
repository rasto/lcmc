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

package lcmc.robotest;

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
import java.util.regex.Pattern;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class RoboTest {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(RoboTest.class);
    /** Screen device. */
    private static final GraphicsDevice SCREEN_DEVICE =
     GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    /** Don't move the mosue pointer smothly. */
    private static final boolean MOVE_MOUSE_FAST = false;
    /** Confirm remove variable. */
    static final boolean CONFIRM_REMOVE = true;
    /** Y position of Primitive/Clone/MS radio buttons. */
    static final int CLONE_RADIO_Y = 125;
    /** Host y position. */
    static final int HOST_Y = 100;
    /** Previous position of the mouse. */
    private static volatile Point2D prevP = null;
    /** Whether the test was aborted. */
    static volatile boolean aborted = false;
    /** Slow down the animation. */
    static float slowFactor = 1f;
    /** Robot. */
    static Robot robot;
    /** Cluster. */
    private static Cluster cluster;

    static final boolean PROXY = true;

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
            LOG.appWarning("restoreMouse: robot error");
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
                    LOG.appWarning("startClicker0: robot error");
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
                    LOG.appWarning("startMover: robot error");
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
                    LOG.appWarning("startTest: robot error");
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
                        GUITest1.start(count);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == '2') {
                        /* cluster wizard deadlock */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        GUITest2.start(count);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    }
                } else if (type == Type.PCMK) {
                    moveToMenu(Tools.getString("ClusterBrowser.ClusterManager"));
                    leftClick();
                    final int count = 200;
                    if (index == '0') {
                        /* all pacemaker tests */
                        int i = 1;
                        while (true) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            PcmkTest1.start(cluster);
                            if (aborted) {
                                break;
                            }
                            PcmkTest2.start();
                            if (aborted) {
                                break;
                            }
                            PcmkTest3.start(4);
                            if (aborted) {
                                break;
                            }
                            PcmkTest4.start();
                            if (aborted) {
                                break;
                            }
                            PcmkTest5.start(5);
                            if (aborted) {
                                break;
                            }
                            PcmkTest6.start(5);
                            if (aborted) {
                                break;
                            }
                            PcmkTest7.start(5);
                            if (aborted) {
                                break;
                            }
                            PcmkTest8.start(10);
                            if (aborted) {
                                break;
                            }
                            //PcmkTest9.start();
                            if (aborted) {
                                break;
                            }
                            PcmkTestA.start(5);
                            if (aborted) {
                                break;
                            }
                            PcmkTestB.start(5);
                            if (aborted) {
                                break;
                            }
                            PcmkTestC.start(5);
                            if (aborted) {
                                break;
                            }
                            PcmkTestD.start(5);
                            if (aborted) {
                                break;
                            }
                            PcmkTestE.start(5);
                            if (aborted) {
                                break;
                            }
                            PcmkTestF.start(cluster, 2);
                            if (aborted) {
                                break;
                            }
                            PcmkTestG.start(5);
                            if (aborted) {
                                break;
                            }
                            PcmkTestH.start(5);
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
                            PcmkTest1.start(cluster);
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
                            PcmkTest2.start();
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
                        PcmkTest3.start(200);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + " no " + i + ", secs: " + secs);
                    } else if (index == '4') {
                        /* placeholders 6 dummies */
                        int i = 1;
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            PcmkTest4.start();
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
                            PcmkTest5.start(10);
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
                            PcmkTest6.start(10);
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
                        PcmkTest7.start(100);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == '8') {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        PcmkTest8.start(30);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'a') {
                        /* pacemaker leak test group */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        PcmkTestA.start(200);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'b') {
                        /* pacemaker leak test clone */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        PcmkTestB.start(200);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'c') {
                        /* pacemaker master/slave test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        PcmkTestC.start(200);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'd') {
                        /* pacemaker leak test */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        PcmkTestD.start(count);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'e') {
                        /* host wizard deadlock */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        PcmkTestE.start(count);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'f') {
                        int i = 1;
                        while (!aborted) {
                            /* cloned group */
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            PcmkTestF.start(cluster, 2);
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
                        PcmkTestG.start(15);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    } else if (index == 'h') {
                        /* ipmi */
                        final long startTime = System.currentTimeMillis();
                        info("test" + index);
                        PcmkTestH.start(15);
                        final int secs = (int) (System.currentTimeMillis()
                                                 - startTime) / 1000;
                        info("test" + index + ", secs: " + secs);
                    }
                } else if (type == Type.DRBD) {
                    moveToMenu(Tools.getString("Dialog.vm.Storage.Title"));
                    leftClick();
                    Tools.getGUIData().expandTerminalSplitPane(1);
                    if (index == '0') {
                        /* all DRBD tests */
                        int i = 1;
                        final int blockDevY = getBlockDevY();
                        while (!aborted) {
                            final long startTime = System.currentTimeMillis();
                            info("test" + index + " no " + i);
                            DrbdTest1.start(cluster, blockDevY);
                            if (aborted) {
                                break;
                            }
                            DrbdTest2.start(cluster, blockDevY);
                            if (aborted) {
                                break;
                            }
                            DrbdTest3.start(cluster, blockDevY);
                            if (aborted) {
                                break;
                            }
                            if (cluster.getHostsArray()[0].hasVolumes()) {
                                DrbdTest4.start(cluster, blockDevY);
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
                            DrbdTest1.start(cluster, blockDevY);
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
                            DrbdTest2.start(cluster, blockDevY);
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
                            DrbdTest3.start(cluster, blockDevY);
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
                            DrbdTest4.start(cluster, blockDevY);
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
                            DrbdTest5.start(cluster, blockDevY);
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
                            DrbdTest8.start(cluster, blockDevY);
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
                            VMTest1.start(cluster, "vm-test" + index, 2);
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
                            VMTest1.start(cluster, "vm-test" + testIndex, 10);
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
                            VMTest1.start(cluster, "vm-test" + testIndex, 30);
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
                        VMTest4.start("vm-test" + index, 100);
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
                            VMTest5.start(cluster, "vm-test" + index, 2);
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
    static void checkTest(final String test, final double no) {
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
    static void checkDRBDTest(final String test, final double no) {
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
    static void checkVMTest(final Host host,
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
    static void checkVMTest(final String test,
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

    /** Stop everything. */
    static void stopEverything() {
        sleep(10000);
        moveTo(Tools.getString("Browser.AdvancedMode"));
        sleep(2000);
        leftClick();
        sleep(2000);
        moveTo(700, 520);
        rightClick(); /* popup */
        sleep(3000);
        moveTo(Tools.getString("ClusterBrowser.Hb.StopAllServices"));
        sleep(3000);
        leftClick();
        moveTo(Tools.getString("Browser.AdvancedMode")); /* not advanced */
        sleep(2000);
        leftClick();
        sleep(2000);
    }

    /** Remove everything. */
    static void removeEverything() {
        sleep(10000);
        moveTo(Tools.getString("Browser.AdvancedMode"));
        sleep(2000);
        leftClick();
        sleep(2000);
        moveTo(700, 520);
        rightClick(); /* popup */
        sleep(3000);
        moveTo(Tools.getString("ClusterBrowser.Hb.RemoveAllServices"));
        sleep(3000);
        leftClick();
        dialogColorTest("remove everything");
        confirmRemove();
        sleep(3000);
        leftClick();
        moveTo(Tools.getString("Browser.AdvancedMode")); /* not advanced */
        sleep(2000);
        leftClick();
        sleep(2000);
    }

    /** Enable stonith if it is enabled. */
    static void enableStonith() {
        moveTo(265, 202);
        leftClick(); /* global options */
        final String stonith = cluster.getBrowser()
                    .getClusterStatus().getGlobalParam("stonith-enabled");
        if (stonith != null && "false".equals(stonith)) {
            moveTo("Stonith Enabled", JCheckBox.class);
            leftClick(); /* enable stonith */
            moveTo(Tools.getString("Browser.CommitResources"));
        }
        sleep(2000);
        leftClick();
    }

    /** Disable stonith if it is enabled. */
    static void disableStonith() {

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
            moveTo(Tools.getString("Browser.CommitResources"));
            sleep(2000);
            leftClick();
        }
    }

    /** Sets location. */
    static void setLocation(final Integer[] events) {
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
        moveTo(Tools.getString("Browser.ApplyResource"));
        sleep(4000);
        leftClick();
        sleep(2000);

        moveScrollBar(false);

    }

    /** Choose dummy resource. */
    static void typeDummy() {
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
    static void setTimeouts(final boolean migrateTimeouts) {
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
    static void sleep(final double x) {
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
    static boolean maybe() {
        if (Math.random() < 0.5) {
            return true;
        }
        return false;
    }

    /** Create dummy resource. */
    static void chooseDummy(final int x,
                            final int y,
                            final boolean clone,
                            final boolean apply) {
        moveTo(x, y);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
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
            moveTo(Tools.getString("Browser.ApplyResource"));
            sleep(4000);
            leftClick();
            sleep(2000);
        }
    }

    /** Removes service. */
    static void removeResource(final int x,
                               final int y,
                               final boolean confirm) {
        moveTo(x + 20, y);
        rightClick();
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.RemoveService"));
        leftClick();
        if (confirm == CONFIRM_REMOVE) {
            confirmRemove();
        }
    }

    /** Removes group. */
    static void removeGroup(final int x, final int y) {
        moveTo(x + 20, y);
        rightClick();
        sleep(10000);
        moveTo(Tools.getString("ClusterBrowser.Hb.RemoveService"));
        leftClick();
        confirmRemove();
    }

    /** Removes placeholder. */
    static void removePlaceHolder(final int x,
                                  final int y,
                                  final boolean confirm) {
        moveTo(x + 20, y);
        rightClick();
        sleep(1000);
        moveTo(Tools.getString("ConstraintPHInfo.Remove"));
        leftClick();
        if (confirm) {
            confirmRemove();
        }
    }

    /** Confirms remove dialog. */
    static void confirmRemove() {
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
    static void stopResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(2000);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.StopResource"));
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Stops group. */
    static void stopGroup() {
        rightClick(); /* popup */
        sleep(3000);
        moveTo(Tools.getString("ClusterBrowser.Hb.StopResource"));
        sleep(10000); /* ptest */
        leftClick(); /* stop */
    }

    /** Removes target role. */
    static void resetStartStopResource(final int x, final int y) {
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
        moveTo(Tools.getString("Browser.ApplyResource"));
        sleep(6000); /* ptest */
        leftClick();
    }

    /** Starts resource. */
    static void startResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        moveTo("^" + Tools.getString("ClusterBrowser.Hb.StartResource") + "$");
        sleep(6000);
        leftClick();
    }

    /** Migrate resource. */
    static void migrateResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.MigrateFromResource"));
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Unmigrate resource. */
    static void unmigrateResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(6000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.UnmigrateResource"));
        sleep(12000); /* ptest */
        leftClick(); /* stop */
    }

    /** Unmanage resource. */
    static void unmanageResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.UnmanageResource"));
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Manage resource. */
    static void manageResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.ManageResource"));
        sleep(6000); /* ptest */
        leftClick(); /* stop */
    }

    /** Adds constraint from vertex. */
    static void addConstraint(final int number) {
        rightClick();
        sleep(5000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddStartBefore"));
        sleep(1000);
        leftClick();
        for (int i = 0; i < number; i++) {
            press(KeyEvent.VK_DOWN);
            sleep(500);
        }
        press(KeyEvent.VK_ENTER);
    }

    /** Adds constraint (order only) from vertex. */
    static void addConstraintOrderOnly(final int x,
                                       final int y,
                                       final int number) {
        moveTo(x + 20, y + 5);
        rightClick();
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddStartBefore"));
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
    static void addConstraintColocationOnly(final int x,
                                            final int y,
                                            final int number) {
        moveTo(x + 20, y + 5);
        rightClick();
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddStartBefore"));
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
    static void removeConstraint(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(10000);
        rightClick(); /* constraint popup */
        sleep(2000);
        moveTo(Tools.getString("ClusterBrowser.Hb.RemoveEdge"));
        sleep(6000); /* ptest */
        leftClick(); /* remove ord */
    }

    /** Removes order. */
    static void removeOrder(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        sleep(1000);
        final String s = Tools.getString("ClusterBrowser.Hb.RemoveOrder");
        moveTo(s.substring(0, s.length() - 2));
        sleep(6000); /* ptest */
        leftClick(); /* remove ord */
    }

    /** Adds order. */
    static void addOrder(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddOrder"));
        sleep(6000); /* ptest */
        leftClick(); /* add ord */
    }

    /** Removes colocation. */
    static void removeColocation(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        sleep(1000);
        moveTo(
            Tools.getString("ClusterBrowser.Hb.RemoveColocation").substring(1));
        sleep(6000); /* ptest */
        leftClick(); /* remove col */
    }

    /** Adds colocation. */
    static void addColocation(final int popX, final int popY) {
        moveTo(popX, popY);
        sleep(1000);
        rightClick(); /* constraint popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddColocation").substring(1));
        sleep(6000); /* ptest */
        leftClick(); /* add col */
    }

    /** Press button. */
    static void press(final int ke)  {
        if (aborted) {
            return;
        }
        robot.keyPress(ke);
        robot.keyRelease(ke);
        sleepNoFactor(200);
    }

    /** Control left click. */
    static void controlLeftClick()  {
        robot.keyPress(KeyEvent.VK_CONTROL);
        leftClick();
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }

    /** Left click. */
    static void leftClick()  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        Tools.sleep(400);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Tools.sleep(300);
    }

    /** Left press. */
    static void leftPress()  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        sleepNoFactor(300);
    }

    /** Left release. */
    static void leftRelease()  {
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        if (aborted) {
            return;
        }
        sleepNoFactor(300);
    }

    /** Right click. */
    static void rightClick()  {
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
    static boolean isColor(final int fromX,
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

    static void moveToSlowly(final int toX, final int toY) {
        slowFactor *= 50;
        moveTo(toX, toY);
        slowFactor /= 50;
    }

    /** Move to position. */
    static void moveTo(final int toX, final int toY) {
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

    static int getY() {
        final Point2D origP = MouseInfo.getPointerInfo().getLocation();
        final Point2D endP =
            Tools.getGUIData().getMainFrameContentPane().getLocationOnScreen();
        return (int) origP.getY() - (int) endP.getY();
    }

    static int getX() {
        final Point2D origP = MouseInfo.getPointerInfo().getLocation();
        final Point2D endP =
            Tools.getGUIData().getMainFrameContentPane().getLocationOnScreen();
        return (int) origP.getX() - (int) endP.getX();
    }

    static void moveTo(final String text) {
        if (aborted) {
            return;
        }
        moveTo(text, null);
    }

    static void moveScrollBar(final boolean down) {
        moveScrollBar(down, 300);
    }

    static void moveScrollBar(final boolean down, final int delta) {
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

    static void moveTo(final Class<?> clazz, final int number) {
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

    static void moveToMenu(final String text) {
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
        LOG.info("moveToMenu: cannot find " + text + " the tree");
    }

    static void moveToGraph(final String text) {
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
        LOG.info("moveToGraph: cannot find " + text + " in the graph");
    }

    static void moveTo(final String text, final Class<?> clazz) {
        moveTo(text, 1, clazz);
    }

    static void moveTo(final String text,
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
                LOG.info("moveTo: cannot find: " + text);
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

    static void moveToAbs(final int endX, final int endY) {
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
            LOG.appWarning("registerMovement: robot error");
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

    static boolean dialogColorTest(final String text) {
        if (aborted) {
            return false;
        }
        sleepNoFactor(1000);
        Component dialog = getFocusedWindow();
        for (int i = 0; i < 60; i++) {
            if (dialog instanceof JDialog || aborted) {
                break;
            }
            dialog = getFocusedWindow();
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

    static void saveAndExit() {
        Tools.save(Tools.getConfigData().getSaveFile(), false);
        sleepNoFactor(10000);
        System.exit(0);
    }

    static void resetTerminalAreas() {
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
        LOG.info(text);
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

    static Container findComponent(final String text,
                                   final Container component,
                                   final Integer[] number) {
        String quotedText;
        if (text.contains("*") || text.contains("$") || text.contains("^")) {
            quotedText = text;
        } else {
            quotedText = Pattern.quote(text);
        }
        if (component instanceof AbstractButton) {
            if (component.isShowing() && component.isEnabled()) {
                final String t = ((AbstractButton) component).getText();
                if (t != null && (t.matches(quotedText + ".*")
                                  || t.matches(".*" + quotedText))) {
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
                if (t != null && (t.matches(quotedText + ".*")
                                  || t.matches(".*" + quotedText))) {
                    if (number[0] <= 1) {
                        return component;
                    } else {
                        number[0]--;
                    }
                }
            }
        }
        if (component == null) {
            LOG.info("findComponent: cannot find " + text);
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

    static Point2D getAppPosition() {
        final Point2D loc =
             Tools.getGUIData().getMainFrameContentPane().getLocationOnScreen();
        final Point2D pos = MouseInfo.getPointerInfo().getLocation();
        final Point2D newPos = new Point2D.Double(pos.getX() - loc.getX(),
                                                  pos.getY() - loc.getY());
        return newPos;
    }

    static void waitForMe() {
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

    static void checkNumberOfVertices(final String name, final int should) {
        if (aborted) {
            return;
        }
        final CRMGraph graph = cluster.getBrowser().getCRMGraph();
        int i = 0;
        while (i < 10 && should != graph.getNumberOfVertices()) {
            LOG.info("checkNumberOfVertices: " + name
                       + " number of vertices: "
                       + should + " -> "
                       + graph.getNumberOfVertices());
            Tools.sleep(1000);
            i++;
        }
    }

    /** Return vertical position of the blockdevices. */
    static int getBlockDevY() {
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
}
