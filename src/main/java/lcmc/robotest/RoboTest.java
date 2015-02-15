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

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Singleton;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTree;

import lcmc.configs.AppDefaults;
import lcmc.common.ui.GUIData;
import lcmc.common.domain.Application;
import lcmc.cluster.domain.Cluster;
import lcmc.host.domain.Host;
import lcmc.crm.ui.CrmGraph;
import lcmc.drbd.ui.DrbdGraph;
import lcmc.common.ui.Info;
import lcmc.cluster.ui.widget.GenericWidget.MTextField;
import lcmc.cluster.ui.widget.MComboBox;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
public final class RoboTest {
    private static final Logger LOG = LoggerFactory.getLogger(RoboTest.class);
    private static final GraphicsDevice SCREEN_DEVICE =
                                        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    /** Don't move the mosue pointer smothly. */
    private static final boolean MOVE_MOUSE_FAST = true;
    /** Confirm remove variable. */
    static final boolean CONFIRM_REMOVE = true;
    /** Y position of Primitive/Clone/MS radio buttons. */
    static final int CLONE_RADIO_Y = 125;
    /** Host y position. */
    static final int HOST_Y = 100;
    /** Previous position of the mouse. */
    private volatile Point2D prevP = null;
    /** Whether the test was aborted. */
    private boolean aborted = false;
    /** Slow down the animation. */
    private float slowFactor = 1f;
    private Robot robot;
    /** Cluster. */
    private Cluster cluster;

    static final boolean PROXY = true;
    @Inject
    private Application application;
    @Inject
    private GUIData guiData;

    public void initRobot(final Cluster cluster) {
        this.cluster = cluster;
        robot = null;
        try {
            robot = new Robot(SCREEN_DEVICE);
        } catch (final AWTException e) {
            LOG.appError("startTest: robot error");
        }
    }

    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    public boolean isAborted() {
        return aborted;
    }

    public void setSlowFactor(float slowFactor) {
        this.slowFactor = slowFactor;
    }

    public float getSlowFactor() {
        return slowFactor;
    }

    /** Abort if mouse moved. */
    private boolean abortWithMouseMovement() {
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
            && prevP.getX() - p.getX() > 300
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
    public void restoreMouse() {
        try {
            robot = new Robot(SCREEN_DEVICE);
        } catch (final AWTException e) {
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
    public void startClicker(final int duration, final boolean lazy) {
        startClicker0(duration, lazy, InputEvent.BUTTON1_MASK,
                                      10,   /* after click */
                                      10,   /* after release */
                                      500,  /* lazy after click */
                                      100); /* lazy after release */
    }

    /** Starts automatic right clicker in 10 seconds. */
    public void startRightClicker(final int duration, final boolean lazy) {
        startClicker0(duration, lazy, InputEvent.BUTTON3_MASK,
                                      10,   /* after click */
                                      500,   /* after release */
                                      500,  /* lazy after click */
                                      5000); /* lazy after release */
    }

    /** Starts automatic clicker in 10 seconds. */
    private void startClicker0(final int duration, final boolean lazy, final int buttonMask, final int timeAfterClick, final int timeAfterRelase, final int timeAfterClickLazy, final int timeAfterRelaseLazy) {
        info("start click test in 10 seconds");
        prevP = null;
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                sleepNoFactor(10000);
                Robot rbt = null;
                try {
                    rbt = new Robot(SCREEN_DEVICE);
                } catch (final AWTException e) {
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
    public void startMover(final int duration, final boolean withClicks) {
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
                } catch (final AWTException e) {
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
                final int destX = origX;
                final int destY = origY;
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
    private int getOffset() {
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


    /** Check test. */
    void checkTest(final String test, final double no) {
        for (final Host host : getClusterHosts()) {
            if (abortWithMouseMovement()) {
                return;
            }
            if (!aborted) {
                host.checkPCMKTest(test, no);
            }
        }
    }

    /** Check DRBD test on the first two hosts. */
    void checkDRBDTest(final String test, final double no) {
        int h = 1;
        for (final Host host : getClusterHosts()) {
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
    void checkVMTest(final Host host, final String test, final double no, final String name) {
        if (abortWithMouseMovement()) {
            return;
        }
        if (!aborted) {
            host.checkVMTest(test, no, name);
        }
    }

    /** Check VM test. */
    void checkVMTest(final String test, final double no, final String name) {
        for (final Host host : getClusterHosts()) {
            if (abortWithMouseMovement()) {
                return;
            }
            if (!aborted) {
                host.checkVMTest(test, no, name);
            }
        }
    }
    /** Stop everything. */
    void stopEverything() {
        moveTo(Tools.getString("Browser.AdvancedMode"));
        leftClick();
        moveTo(700, 470);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.StopAllServices"));
        leftClick();
        Tools.sleep(500);
        moveTo(Tools.getString("Browser.AdvancedMode")); /* not advanced */
        leftClick();
    }

    /** Remove everything. */
    void removeEverything() {
        moveTo(Tools.getString("Browser.AdvancedMode"));
        leftClick();
        moveTo(700, 470);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.RemoveAllServices"));
        leftClick();
        dialogColorTest("remove everything");
        confirmRemove();
        leftClick();
        moveTo(Tools.getString("Browser.AdvancedMode")); /* not advanced */
        leftClick();
    }

    /** Enable stonith if it is enabled. */
    void enableStonith() {
        moveTo(265, 202);
        leftClick(); /* global options */
        final String stonith = cluster.getBrowser()
            .getClusterStatus().getGlobalParam("stonith-enabled");
        if (stonith != null && "false".equals(stonith)) {
            moveTo("Stonith Enabled", JCheckBox.class);
            leftClick(); /* enable stonith */
            moveTo(Tools.getString("Browser.CommitResources"));
        }
        leftClick();
    }

    /** Disable stonith if it is enabled. */
    void disableStonith() {

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
            moveTo("No Quorum Policy", MComboBox.class);
            leftClick();
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_UP);
            press(KeyEvent.VK_UP);
            press(KeyEvent.VK_ENTER); /* ignore */
            apply = true;
        }
        if (apply) {
            moveTo(Tools.getString("Browser.CommitResources"));
            leftClick();
        }
    }

    /** Sets location. */
    void setLocation(final Integer[] events) {
        moveScrollBar(true);

        moveTo("on ", MComboBox.class);
        leftClick();
        leftClick();
        for (final int ev : events) {
            if (ev == KeyEvent.VK_PLUS) {
                robot.keyPress(KeyEvent.VK_SHIFT);
            }
            press(ev);
            if (ev == KeyEvent.VK_PLUS) {
                robot.keyRelease(KeyEvent.VK_SHIFT);
            }
        }
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick();

        moveScrollBar(false);

    }

    /** Choose dummy resource. */
    void typeDummy() {
        press(KeyEvent.VK_D);
        press(KeyEvent.VK_U);
        press(KeyEvent.VK_M);
        press(KeyEvent.VK_M);
        press(KeyEvent.VK_Y);
        press(KeyEvent.VK_ENTER); /* choose dummy */
    }

    /** Sets start timeout. */
    void setTimeouts(final boolean migrateTimeouts) {
        moveScrollBar(true);
        moveTo("Start / Timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_2);
        press(KeyEvent.VK_0);
        press(KeyEvent.VK_0);

        moveTo("Stop / Timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_1);
        press(KeyEvent.VK_9);
        press(KeyEvent.VK_2);

        moveTo("Monitor / Timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_1);
        press(KeyEvent.VK_5);
        press(KeyEvent.VK_4);

        moveTo("Monitor / Interval", MTextField.class);
        leftClick();
        press(KeyEvent.VK_1);
        press(KeyEvent.VK_2);
        press(KeyEvent.VK_1);
        if (migrateTimeouts) {
            moveTo("Reload / Timeout", MTextField.class);
            leftClick();
            press(KeyEvent.VK_BACK_SPACE);
            press(KeyEvent.VK_BACK_SPACE);

            moveTo("Migrate_to / Timeout", MTextField.class);
            leftClick();
            press(KeyEvent.VK_1);
            press(KeyEvent.VK_2);
            press(KeyEvent.VK_3);

            moveTo("Migrate_from / T", MTextField.class);
            leftClick();
            press(KeyEvent.VK_1);
            press(KeyEvent.VK_2);
            press(KeyEvent.VK_2);
        }

        moveScrollBar(false);
    }

    public void sleepNoFactor(final double x) {
        sleep(x / slowFactor);
    }

    /** Sleep for x milliseconds * slowFactor + some random time. */
    void sleep(final double x) {
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
    boolean maybe() {
        return Math.random() < 0.5;
    }

    /** Create dummy resource. */
    void chooseDummy(final int x, final int y, final boolean clone, final boolean apply) {
        moveTo(x, y);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("OCF Resource Agents");
        typeDummy();
        if (apply) {
            setTimeouts(true);
            if (clone) {
                moveTo(893, CLONE_RADIO_Y);
                leftClick(); /* clone */
            }
            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
        }
    }

    /** Removes service. */
    void removeResource(final int x, final int y, final boolean confirm) {
        moveTo(x + 20, y);
        rightClick();
        moveTo(Tools.getString("ClusterBrowser.Hb.RemoveService"));
        leftClick();
        if (confirm == CONFIRM_REMOVE) {
            confirmRemove();
        }
    }

    /** Removes group. */
    void removeGroup(final int x, final int y) {
        moveTo(x + 20, y);
        rightClick();
        moveTo(Tools.getString("ClusterBrowser.Hb.RemoveService"));
        leftClick();
        confirmRemove();
    }

    /** Removes placeholder. */
    void removePlaceHolder(final int x, final int y, final boolean confirm) {
        moveTo(x + 20, y);
        rightClick();
        moveTo(Tools.getString("ConstraintPHInfo.Remove"));
        leftClick();
        if (confirm) {
            confirmRemove();
        }
    }

    /** Confirms remove dialog. */
    void confirmRemove() {
        dialogColorTest("confirm remove");
        press(KeyEvent.VK_TAB);
        press(KeyEvent.VK_TAB);
        press(KeyEvent.VK_SPACE);
    }

    /** Stops resource. */
    void stopResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.StopResource"));
        leftClick(); /* stop */
    }

    /** Stops group. */
    void stopGroup() {
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.StopResource"));
        leftClick(); /* stop */
    }

    /** Removes target role. */
    void resetStartStopResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        leftClick(); /* select */

        moveTo("Target Role", MComboBox.class);
        leftClick(); /* pull down */
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_UP);
        press(KeyEvent.VK_UP);
        leftClick();
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick();
    }

    /** Starts resource. */
    void startResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        rightClick(); /* popup */
        moveTo('^' + Tools.getString("ClusterBrowser.Hb.StartResource") + '$');
        leftClick();
    }

    /** Migrate resource. */
    void migrateResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.MigrateFromResource"));
        leftClick(); /* stop */
    }

    /** Unmigrate resource. */
    void unmigrateResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.UnmigrateResource"));
        leftClick(); /* stop */
    }

    /** Unmanage resource. */
    void unmanageResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.UnmanageResource"));
        leftClick(); /* stop */
    }

    /** Manage resource. */
    void manageResource(final int x, final int y) {
        moveTo(x + 50, y + 5);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.ManageResource"));
        leftClick(); /* stop */
    }

    /** Adds constraint from vertex. */
    void addConstraint(final int number) {
        rightClick();
        moveTo(Tools.getString("ClusterBrowser.Hb.AddStartBefore"));
        leftClick();
        for (int i = 0; i < number; i++) {
            press(KeyEvent.VK_DOWN);
        }
        press(KeyEvent.VK_ENTER);
    }

    /** Adds constraint (order only) from vertex. */
    void addConstraintOrderOnly(final int x, final int y, final int number) {
        moveTo(x + 20, y + 5);
        rightClick();
        moveTo(Tools.getString("ClusterBrowser.Hb.AddStartBefore"));
        leftClick();
        press(KeyEvent.VK_TAB);
        press(KeyEvent.VK_SPACE); /* disable colocation */
        press(KeyEvent.VK_TAB);
        press(KeyEvent.VK_TAB); /* list */

        for (int i = 0; i < number; i++) {
            press(KeyEvent.VK_DOWN);
        }
        press(KeyEvent.VK_ENTER);
    }

    /** Adds constraint (colocation only) from vertex. */
    void addConstraintColocationOnly(final int x, final int y, final int number) {
        moveTo(x + 20, y + 5);
        rightClick();
        moveTo(Tools.getString("ClusterBrowser.Hb.AddStartBefore"));
        leftClick();
        press(KeyEvent.VK_TAB);
        press(KeyEvent.VK_TAB);
        press(KeyEvent.VK_SPACE); /* disable order */
        press(KeyEvent.VK_TAB); /* list */

        for (int i = 0; i < number; i++) {
            press(KeyEvent.VK_DOWN);
        }
        press(KeyEvent.VK_ENTER);
    }

    /** Removes constraint. */
    void removeConstraint(final int popX, final int popY) {
        moveTo(popX, popY);
        rightClick(); /* constraint popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.RemoveEdge"));
        leftClick(); /* remove ord */
    }

    /** Removes order. */
    void removeOrder(final int popX, final int popY) {
        moveTo(popX, popY);
        rightClick(); /* constraint popup */
        final String s = Tools.getString("ClusterBrowser.Hb.RemoveOrder");
        moveTo(s.substring(0, s.length() - 2));
        leftClick(); /* remove ord */
    }

    /** Adds order. */
    void addOrder(final int popX, final int popY) {
        moveTo(popX, popY);
        rightClick(); /* constraint popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddOrder"));
        leftClick(); /* add ord */
    }

    /** Removes colocation. */
    void removeColocation(final int popX, final int popY) {
        moveTo(popX, popY);
        rightClick(); /* constraint popup */
        moveTo(
            Tools.getString("ClusterBrowser.Hb.RemoveColocation").substring(1));
        leftClick(); /* remove col */
    }

    /** Adds colocation. */
    void addColocation(final int popX, final int popY) {
        moveTo(popX, popY);
        rightClick(); /* constraint popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddColocation").substring(1));
        leftClick(); /* add col */
    }

    /** Press button. */
    void press(final int ke) {
        if (aborted) {
            return;
        }
        robot.keyPress(ke);
        robot.keyRelease(ke);
        sleepNoFactor(200);
    }

    /** Control left click. */
    void controlLeftClick()  {
        robot.keyPress(KeyEvent.VK_CONTROL);
        leftClick();
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }

    /** Left click. */
    void leftClick()  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        Tools.sleep(400);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Tools.sleep(300);
    }

    /** Left press. */
    void leftPress()  {
        if (aborted) {
            return;
        }
        robot.mousePress(InputEvent.BUTTON1_MASK);
        sleepNoFactor(300);
    }

    /** Left release. */
    void leftRelease()  {
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        if (aborted) {
            return;
        }
        sleepNoFactor(300);
    }

    /** Right click. */
    void rightClick()  {
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
    boolean isColor(final int fromX, final int fromY, final Color color, final boolean expected)  {
        if (aborted) {
            return true;
        }
        final int xOffset = getOffset();
        final Point2D appP =
            guiData.getMainFrameContentPane()
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

    void moveToSlowly(final int toX, final int toY) {
        slowFactor *= 50;
        moveTo(toX, toY);
        slowFactor /= 50;
    }

    /** Move to position. */
    void moveTo(final int toX, final int toY) {
        if (aborted) {
            return;
        }
        prevP = null;
        final Point2D origP = MouseInfo.getPointerInfo().getLocation();
        final int origX = (int) origP.getX();
        final int origY = (int) origP.getY();
        final Point2D endP =
            guiData.getMainFrameContentPane().getLocationOnScreen();
        final int endX = (int) endP.getX() + toX;
        final int endY = (int) endP.getY() + toY;
        moveToAbs(endX, endY);
    }

    int getY() {
        final Point2D origP = MouseInfo.getPointerInfo().getLocation();
        final Point2D endP =
            guiData.getMainFrameContentPane().getLocationOnScreen();
        return (int) origP.getY() - (int) endP.getY();
    }

    int getX() {
        final Point2D origP = MouseInfo.getPointerInfo().getLocation();
        final Point2D endP =
            guiData.getMainFrameContentPane().getLocationOnScreen();
        return (int) origP.getX() - (int) endP.getX();
    }

    void moveTo(final String text) {
        if (aborted) {
            return;
        }
        moveTo(text, null);
    }

    void moveScrollBar(final boolean down) {
        moveScrollBar(down, 300);
    }

    void moveScrollBar(final boolean down, final int delta) {
        if (aborted) {
            return;
        }

        java.awt.Component scrollbar = null;
        int scrollbarX = 0;
        int scrollbarY = 0;
        int i = 0;
        do {
            final List<java.awt.Component> res = new ArrayList<java.awt.Component>();
            try {
                findInside(guiData.getMainFrame(),
                           Class.forName("javax.swing.JScrollPane$ScrollBar"),
                           res);
            } catch (final ClassNotFoundException e) {
                Tools.printStackTrace("can't find the scrollbar");
                return;
            }
            final java.awt.Component app = guiData.getMainFrameContentPane();
            final int mX =
                (int) app.getLocationOnScreen().getX() + app.getWidth() / 2;
            final int mY =
                (int) app.getLocationOnScreen().getY() + app.getHeight() / 2;
            for (final java.awt.Component c : res) {
                final Point2D p = c.getLocationOnScreen();
                final int pX = (int) p.getX();
                final int pY = (int) p.getY();
                if (pX > mX && pY < mY) {
                    scrollbar = c;
                    scrollbarX = pX + c.getWidth() / 2;
                    scrollbarY = pY + c.getHeight() / 2;
                }
            }
            if (i > 3) {
                break;
            }
            i++;
        } while (scrollbar == null);
        if (scrollbar == null) {
            Tools.printStackTrace("can't find the scrollbar");
        }
        moveToAbs(scrollbarX, scrollbarY);
        leftPress();
        if (down) {
            moveToAbs(scrollbarX, scrollbarY + delta);
        } else {
            moveToAbs(scrollbarX, scrollbarY - delta);
        }
        leftRelease();
    }

    private java.awt.Component getFocusedWindow() {
        for (final Window w : Window.getWindows()) {
            java.awt.Component c = w.getFocusOwner();
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

    void moveTo(final Class<?> clazz, final int number) {
        if (aborted) {
            return;
        }
        java.awt.Component c = null;
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
        Point2D endP;
        try {
            endP = c.getLocationOnScreen();
        } catch (final IllegalComponentStateException e) {
            Tools.sleep(5000);
            endP = c.getLocationOnScreen();
        }
        final int endX = (int) endP.getX() + c.getWidth() / 2;
        final int endY = (int) endP.getY() + c.getHeight() / 2;
        moveToAbs(endX, endY);
    }

    void moveToMenu(final String text) {
        if (aborted) {
            return;
        }
        final JTree tree = (JTree) findInside(guiData.getMainFrame(),
                                              JTree.class,
                                              0);
        if (tree == null) {
            info("can't find the tree");
            return;
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

    void moveToGraph(final String text) {
        if (aborted) {
            return;
        }
        final DrbdGraph graph = cluster.getBrowser().getDrbdGraph();
        for (final Info i : graph.infoToVertexKeySet()) {
            final String item = graph.getMainText(graph.getVertex(i), Application.RunMode.LIVE);
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

    void moveTo(final String text, final Class<?> clazz) {
        moveTo(text, 1, clazz);
    }

    void moveTo(final String text, final int number, final Class<?> clazz) {
        if (aborted) {
            return;
        }
        java.awt.Component c = null;
        int i = 0;
        while (c == null && i < 30 && !aborted) {
            c = findComponent(text, number);
            if (i > 0) {
                sleepNoFactor(100);
            } else if (i > 10) {
                sleepNoFactor(1000);
                LOG.info("moveTo: cannot find: " + text);
            }
            i++;
        }
        if (aborted) {
            return;
        }
        if (c == null) {
            Tools.printStackTrace("can't find: " + text);
            return;
        }
        final java.awt.Component n;
        if (clazz == null) {
            n = c;
        } else {
            n = findNext(c, clazz);
            if (n == null) {
                Tools.printStackTrace("can't find: " + text + " -> " + clazz);
                return;
            }
        }
        Point2D endP;
        try {
            endP = n.getLocationOnScreen();
        } catch (final IllegalComponentStateException e) {
            Tools.sleep(5000);
            endP = n.getLocationOnScreen();
        }
        final int endX = (int) endP.getX() + 15;
        final int endY = (int) endP.getY() + c.getHeight() / 2;
        int tries = 0;
        while (!n.isEnabled() && tries < 30) {
            Tools.sleep(1000);
            tries++;
            if (tries == 30) {
                LOG.appWarning("moveTo: component disabled " + text);
            }
        }
        moveToAbs(endX, endY);
    }

    void moveToAbs(final int endX, final int endY) {
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
            if (x < destX) {
                directionX = 1;
            } else if (x > destX) {
                directionX = -1;
            }
            int directionY = 0;
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
    public void registerMovement() {
        Robot rbt = null;
        try {
            rbt = new Robot(SCREEN_DEVICE);
        } catch (final AWTException e) {
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

    boolean dialogColorTest(final String text) {
        if (aborted) {
            return false;
        }
        sleepNoFactor(100);
        java.awt.Component dialog = getFocusedWindow();
        for (int i = 0; i < 30; i++) {
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

    void resetTerminalAreas() {
        for (final Host h : getClusterHosts()) {
            if (!aborted) {
                h.getTerminalPanel().resetTerminalArea();
            }
        }
    }

    public void info(final String text) {
        if (cluster != null) {
            for (final Host h : getClusterHosts()) {
                h.getTerminalPanel().addCommandOutput(text + '\n');
            }
        }
        LOG.info(text);
    }

    public java.awt.Component findInside(final java.awt.Component component, final Class<?> clazz, final int position) {
        final List<java.awt.Component> res = new ArrayList<java.awt.Component>();
        findInside(component, clazz, res);
        if (res.size() > position) {
            return res.get(position);
        }
        return null;
    }

    public void findInside(final java.awt.Component component, final Class<?> clazz, final List<java.awt.Component> results) {
        int i = 0;
        while (results.isEmpty() && i < 10) {
            if (i > 0) {
                Tools.sleep(1000);
            }
            findInside0(component, clazz, results);
            i++;
        }
    }

    private void findInside0(final java.awt.Component component, final Class<?> clazz, final List<java.awt.Component> results) {
        if (component.getClass().equals(clazz)
            && component.isShowing()) {
            results.add(component);
        }
        if (component instanceof Container) {
            for (final java.awt.Component c : ((Container) component).getComponents()) {
                findInside0(c, clazz, results);
            }
        }
    }

    /** Find component that is next to the specified component and is of the
     * specified class. */
    public java.awt.Component findNext(final java.awt.Component component, final Class<?> clazz) {
        boolean next = false;
        for (final java.awt.Component c : component.getParent().getComponents()) {
            if (next) {
                final java.awt.Component f = findInside(c, clazz, 0);
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

    Container findComponent(final String text, final Container component, final Integer[] number) {
        final String quotedText;
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
        for (final java.awt.Component c : component.getComponents()) {
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

    public Container findComponent(final String text) {
        return findComponent(text, 1);
    }

    public Container findComponent(final String text, final int number) {
        return findComponent(text,
                             (Container) getFocusedWindow(),
                             new Integer[]{number});
    }

    Point2D getAppPosition() {
        final Point2D loc =
            guiData.getMainFrameContentPane().getLocationOnScreen();
        final Point2D pos = MouseInfo.getPointerInfo().getLocation();
        final Point2D newPos = new Point2D.Double(pos.getX() - loc.getX(),
            pos.getY() - loc.getY());
        return newPos;
    }

    void waitForMe() {
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

    void checkNumberOfVertices(final String name, final int should) {
        if (aborted) {
            return;
        }
        final CrmGraph graph = cluster.getBrowser().getCrmGraph();
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
    int getBlockDevY() {
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

    Robot getRobot() {
        return robot;
    }

    public List<Host> getClusterHosts() {
        return new ArrayList<Host>(cluster.getHosts());
    }
}
