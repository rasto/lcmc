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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import lcmc.Exceptions;
import lcmc.data.Cluster;
import lcmc.gui.widget.GenericWidget.MTextField;
import lcmc.gui.widget.MComboBox;
import static lcmc.robotest.RoboTest.CONFIRM_REMOVE;
import static lcmc.robotest.RoboTest.addColocation;
import static lcmc.robotest.RoboTest.addConstraint;
import static lcmc.robotest.RoboTest.addConstraintColocationOnly;
import static lcmc.robotest.RoboTest.addConstraintOrderOnly;
import static lcmc.robotest.RoboTest.addOrder;
import static lcmc.robotest.RoboTest.checkTest;
import static lcmc.robotest.RoboTest.disableStonith;
import static lcmc.robotest.RoboTest.enableStonith;
import static lcmc.robotest.RoboTest.leftClick;
import static lcmc.robotest.RoboTest.leftPress;
import static lcmc.robotest.RoboTest.leftRelease;
import static lcmc.robotest.RoboTest.manageResource;
import static lcmc.robotest.RoboTest.migrateResource;
import static lcmc.robotest.RoboTest.moveScrollBar;
import static lcmc.robotest.RoboTest.moveTo;
import static lcmc.robotest.RoboTest.moveToMenu;
import static lcmc.robotest.RoboTest.moveToSlowly;
import static lcmc.robotest.RoboTest.press;
import static lcmc.robotest.RoboTest.removeColocation;
import static lcmc.robotest.RoboTest.removeConstraint;
import static lcmc.robotest.RoboTest.removeGroup;
import static lcmc.robotest.RoboTest.removeOrder;
import static lcmc.robotest.RoboTest.removeResource;
import static lcmc.robotest.RoboTest.resetStartStopResource;
import static lcmc.robotest.RoboTest.rightClick;
import static lcmc.robotest.RoboTest.robot;
import static lcmc.robotest.RoboTest.setLocation;
import static lcmc.robotest.RoboTest.setTimeouts;
import static lcmc.robotest.RoboTest.sleep;
import static lcmc.robotest.RoboTest.slowFactor;
import static lcmc.robotest.RoboTest.startResource;
import static lcmc.robotest.RoboTest.stopGroup;
import static lcmc.robotest.RoboTest.stopResource;
import static lcmc.robotest.RoboTest.typeDummy;
import static lcmc.robotest.RoboTest.unmanageResource;
import static lcmc.robotest.RoboTest.unmigrateResource;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTest1 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTest1.class);

    static void start(final Cluster cluster) {
        RoboTest.slowFactor = 0.4f;
        RoboTest.aborted = false;
        /* create IPaddr2 with 192.168.100.100 ip */
        String testName = "test1";
        final String pmV = cluster.getHostsArray()[0].getPacemakerVersion();
        try {
            if (pmV != null && Tools.compareVersions(pmV, "1.1.6") < 0) {
                testName = "test1-1.0";
            }
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("start: " + e.getMessage(), e);
        }

        disableStonith();
        checkTest(testName, 1);
        enableStonith();
        checkTest(testName, 1.1);
        disableStonith();
        checkTest(testName, 1);
        final int ipX = 235;
        final int ipY = 207;
        moveTo(ipX, ipY + 200);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("OCF Resource Agents");
        press(KeyEvent.VK_D);
        press(KeyEvent.VK_R);
        press(KeyEvent.VK_B);
        press(KeyEvent.VK_D);
        press(KeyEvent.VK_ENTER);
        moveTo(Tools.getString("ConfirmDialog.Yes"));
        leftClick();
        removeResource(ipX, ipY + 200, !CONFIRM_REMOVE);

        moveTo(ipX, ipY + 200);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("IPaddr2");
        leftClick();

        moveTo(ipX, ipY);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("IPaddr2");
        leftClick();

        final float savedSlowFactor = RoboTest.slowFactor;
        RoboTest.slowFactor = 0.00001f;
        for (final Integer pos1 : new Integer[]{850, 900, 1000}) {
            for (final Integer pos2 : new Integer[]{850, 900, 1000}) {
                if (pos1 == pos2) {
                    continue;
                }
                for (int i = 0; i < 70; i++) {
                    moveTo(pos1, RoboTest.CLONE_RADIO_Y);
                    robot.mousePress(InputEvent.BUTTON1_MASK);
                    Tools.sleep(20);
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                    moveTo(pos2, RoboTest.CLONE_RADIO_Y);
                    robot.mousePress(InputEvent.BUTTON1_MASK);
                    Tools.sleep(20);
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                }
            }
        }
        slowFactor = savedSlowFactor;
        removeResource(ipX, ipY + 200, !RoboTest.CONFIRM_REMOVE);
        removeResource(ipX, ipY, !RoboTest.CONFIRM_REMOVE);
        /* again */
        moveTo(ipX, ipY);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("IPaddr2");
        leftClick();

        moveTo("IPv4 ", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER);
        press(KeyEvent.VK_1);
        press(KeyEvent.VK_0);
        press(KeyEvent.VK_0);
        setTimeouts(false);
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick(); /* apply */
        /* CIDR netmask 24 */

        moveTo("CIDR netmask", MTextField.class); /* CIDR */
        leftClick();
        press(KeyEvent.VK_2);
        press(KeyEvent.VK_4);

        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick(); /* apply */

        checkTest(testName, 2); /* 2 */

        /* pingd */
        moveScrollBar(true);
        moveTo("pingd", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN); /* no ping */
        press(KeyEvent.VK_DOWN); /* no ping */
        press(KeyEvent.VK_ENTER); /* no ping */
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick();
        checkTest(testName, 2.1); /* 2.1 */

        moveTo("pingd", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_UP); /* no ping */
        press(KeyEvent.VK_UP); /* no ping */
        press(KeyEvent.VK_ENTER); /* no ping */
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick();

        moveScrollBar(false);

        /* group with dummy resources */
        final int gx = 230;
        final int gy = 305;
        moveTo(gx, gy);
        rightClick(); /* popup */
        moveTo(gx + 46, gy + 11);
        leftClick(); /* choose group */
        /* remove it */
        removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        moveTo(gx, gy);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        leftClick();

        rightClick(); /* group popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        moveTo("OCF Resource Agents");
        typeDummy();

        /* remove it */
        removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        /* group with dummy resources, once again */
        moveTo(gx, gy);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        leftClick();
        rightClick(); /* group popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        moveTo("OCF Resource Agents");
        typeDummy();

        moveToMenu("Dummy (1)");
        rightClick();

        moveTo(Tools.getString("ClusterBrowser.Hb.RemoveService"));
        leftClick(); /* menu remove service */
        removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        /* group with dummy resources, once again */
        moveTo(gx, gy);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        leftClick();
        rightClick(); /* group popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        moveTo("OCF Resource Agents");
        typeDummy();

        removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        /* once again */
        moveTo(gx, gy);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        leftClick();

        checkTest(testName, 2); /* 2 */

        rightClick(); /* group popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        moveTo("OCF Resource Agents");
        typeDummy();

        setTimeouts(true);
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick();
        for (int i = 0; i < 2; i++) {
            /* another group resource */
            moveTo(gx + 10, gy - 25);
            rightClick(); /* popup */
            moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
            moveTo("OCF Resource Agents");
            typeDummy();
            setTimeouts(true);
            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
        }
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
                    moveTo(Tools.getString("ClusterBrowser.Hb.ResGrpMoveUp"));
                    leftClick(); /* move res 3 up */
                    checkTest(testName, 3.11); /* 3.11 */
                    moveToMenu("Dummy (3)");
                    rightClick();
                    moveTo(Tools.getString("ClusterBrowser.Hb.ResGrpMoveDown"));
                    leftClick();
                    checkTest(testName, 3.12); /* 3.12 */
                }
            }
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("start: " + e.getMessage(), e);
        }

        moveToMenu("Dummy (3)");
        leftClick();

        moveScrollBar(true);

        for (int i = 0; i < 2; i++) {
            moveTo(Tools.getString("ClusterBrowser.SameAs"),
                   2,
                   MComboBox.class);
            leftClick();
            press(KeyEvent.VK_DOWN); /* choose another dummy */
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_ENTER);
            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
            checkTest(testName, 3.2); /* 3.2 */

            moveTo(Tools.getString("ClusterBrowser.SameAs"),
                   2,
                   MComboBox.class);
            leftClick();

            press(KeyEvent.VK_PAGE_UP); /* choose "nothing selected */
            press(KeyEvent.VK_ENTER);

            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
            checkTest(testName, 4); /* 4 */
            sleep(500);
        }
        moveScrollBar(false);

        /* locations */
        moveTo(ipX + 20, ipY);
        leftClick(); /* choose ip */
        setLocation(new Integer[]{KeyEvent.VK_I});
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
        checkTest(testName, 4.4); /* 4.4 */
        final int popX = 340;
        final int popY = 257;
        removeConstraint(popX, popY);
        checkTest(testName, 5); /* 5 */
        moveTo(gx + 10, gy - 25);
        addConstraint(1);
        checkTest(testName, 5.1); /* 4.4 */

        removeConstraint(popX, popY);
        checkTest(testName, 5.2); /* 5 */

        moveTo(gx + 10, gy - 25);
        addConstraint(1);
        checkTest(testName, 6); /* 6 */

        removeOrder(popX, popY);
        checkTest(testName, 7);

        addOrder(popX, popY);
        checkTest(testName, 8);

        removeColocation(popX, popY);
        checkTest(testName, 9);

        addColocation(popX, popY);
        checkTest(testName, 10);

        removeColocation(popX, popY);
        checkTest(testName, 10.1);

        removeOrder(popX, popY);
        checkTest(testName, 10.2);

        addConstraintOrderOnly(gx, gy - 25, 2);
        checkTest(testName, 10.3);

        addColocation(popX, popY);
        checkTest(testName, 10.4);

        removeOrder(popX, popY);
        checkTest(testName, 10.5);

        removeColocation(popX, popY);
        checkTest(testName, 10.6);

        addConstraintColocationOnly(gx, gy - 25, 2);
        checkTest(testName, 10.7);

        addOrder(popX, popY);
        checkTest(testName, 10.8);

        removeConstraint(popX, popY);
        checkTest(testName, 10.9);

        moveTo(ipX, ipY);
        addConstraint(1);
        checkTest(testName, 10.91);

        removeOrder(popX, popY);
        checkTest(testName, 10.92);

        addOrder(popX, popY);
        checkTest(testName, 10.93);

        removeColocation(popX, popY);
        checkTest(testName, 10.94);

        addColocation(popX, popY);
        checkTest(testName, 10.95);

        removeColocation(popX, popY);
        checkTest(testName, 10.96);

        removeOrder(popX, popY);
        checkTest(testName, 10.97);

        addConstraintColocationOnly(ipX, ipY, 1);
        checkTest(testName, 10.98);

        removeColocation(popX, popY);
        checkTest(testName, 10.99);

        addConstraintOrderOnly(ipX, ipY, 1);
        checkTest(testName, 11);

        addColocation(popX, popY);
        checkTest(testName, 11.1);

        removeConstraint(popX, popY);
        checkTest(testName, 11.2);

        moveTo(ipX, ipY);
        addConstraint(3);
        checkTest(testName, 11.3);
        stopResource(ipX, ipY);
        checkTest(testName, 11.4);
        resetStartStopResource(ipX, ipY);
        checkTest(testName, 11.5);

        moveTo(ipX + 20, ipY + 10);
        leftClick(); /* choose ip */
        moveTo(Tools.getString("Browser.ActionsMenu"));
        rightClick();
        moveTo(Tools.getString("ClusterBrowser.Hb.StopResource"));
        leftClick();
        checkTest(testName, 11.501);

        moveTo(ipX + 20, ipY + 10);
        leftClick(); /* choose ip */
        moveTo(Tools.getString("Browser.ActionsMenu"));
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.StartResource").substring(1));
        /* actions menu start */
        leftClick();
        checkTest(testName, 11.502);

        resetStartStopResource(ipX, ipY);
        checkTest(testName, 11.5);

        removeColocation(popX, popY);
        checkTest(testName, 11.51);

        addColocation(popX, popY);
        checkTest(testName, 11.52);

        removeOrder(popX, popY);
        checkTest(testName, 11.53);

        addOrder(popX, popY);
        checkTest(testName, 11.54);

        removeColocation(popX, popY);
        checkTest(testName, 11.55);

        removeOrder(popX, popY);
        checkTest(testName, 11.56);

        addConstraintOrderOnly(ipX, ipY, 3);
        checkTest(testName, 11.57);

        removeOrder(popX, popY);
        checkTest(testName, 11.58);

        addConstraintColocationOnly(ipX, ipY, 3);
        checkTest(testName, 11.59);

        addOrder(popX, popY);
        checkTest(testName, 11.6);

        removeConstraint(popX, popY);
        checkTest(testName, 11.7);

        moveTo(gx + 10, gy - 25);
        addConstraint(1);
        checkTest(testName, 11.8);
        /** Add m/s Stateful resource */
        final int statefulX = 500;
        final int statefulY = 207;
        moveTo(statefulX, statefulY);
        rightClick();
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("OCF Resource Agents");

        press(KeyEvent.VK_S);
        press(KeyEvent.VK_T);
        press(KeyEvent.VK_A);
        press(KeyEvent.VK_T);
        press(KeyEvent.VK_E);
        press(KeyEvent.VK_F);
        press(KeyEvent.VK_ENTER); /* choose Stateful */

        Tools.sleep(400);
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick();
        checkTest(testName, 11.9);
        /* set clone max to 1 */
        moveTo("Clone Max", MComboBox.class);
        leftClick();
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        press(KeyEvent.VK_1);
        setTimeouts(false);
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick();
        checkTest(testName, 12);
        stopResource(statefulX, statefulY);
        checkTest(testName, 13);

        startResource(statefulX, statefulY);
        checkTest(testName, 14);
        unmanageResource(statefulX, statefulY);
        checkTest(testName, 15);
        manageResource(statefulX, statefulY);
        checkTest(testName, 16);

        /* IP addr cont. */
        stopResource(ipX, ipY);
        checkTest(testName, 17);
        startResource(ipX, ipY);
        checkTest(testName, 18);
        unmanageResource(ipX, ipY);
        checkTest(testName, 19);
        manageResource(ipX, ipY);
        checkTest(testName, 20);
        migrateResource(ipX, ipY);
        checkTest(testName, 21);
        unmigrateResource(ipX, ipY);
        checkTest(testName, 22);

        /* Group cont. */
        stopResource(gx, gy - 25);
        checkTest(testName, 23);
        startResource(gx, gy - 25);
        checkTest(testName, 24);
        unmanageResource(gx, gy - 25);
        checkTest(testName, 25);
        manageResource(gx, gy - 25);
        checkTest(testName, 26);
        migrateResource(gx, gy - 25);
        moveTo(gx, gy);
        leftClick();
        checkTest(testName, 27);
        moveTo(Tools.getString("Browser.ActionsMenu"));
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.UnmigrateResource"));
        leftClick(); /* stop */
        checkTest(testName, 28);

        moveTo(700, 450); /* rectangle */
        leftPress();
        moveTo(220, 65);
        leftRelease();

        moveTo(ipX, ipY);
        rightClick();
        moveTo(ipX + 30, ipY); /* ptest */
        moveToSlowly(ipX + 30, ipY + 350);
        moveTo(ipX, ipY);

        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER); /* standby selected hosts */
        checkTest(testName, 28.1);

        moveTo(700, 450);
        leftPress();
        moveTo(220, 65);
        leftRelease();
        moveTo(ipX, ipY);
        rightClick();
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER); /* online selected hosts */
        checkTest(testName, 28.2);

        moveTo(700, 450);
        leftPress();
        moveTo(220, 65);
        leftRelease();
        moveTo(ipX, ipY);
        rightClick();
        moveTo(Tools.getString("PcmkMultiSelectionInfo.StopSelectedResources"));
        leftClick();
        checkTest(testName, 28.3);

        moveTo(700, 450);
        leftPress();
        moveTo(220, 65);
        leftRelease();
        moveTo(ipX, ipY);
        rightClick();
        moveTo(
            Tools.getString("PcmkMultiSelectionInfo.StartSelectedResources"));
        leftClick();
        checkTest(testName, 28.4);
        moveTo(700, 520); /* reset selection */
        leftClick();

        stopResource(ipX, ipY);
        moveTo(gx, gy);
        leftClick();
        moveTo(Tools.getString("Browser.ActionsMenu"));
        stopGroup();
        moveTo(statefulX, statefulY);
        stopGroup();
        checkTest(testName, 29);

        removeResource(ipX, ipY, RoboTest.CONFIRM_REMOVE);
        removeGroup(gx, gy - 20);
        removeGroup(statefulX, statefulY);
        checkTest(testName, 1);
    }

    /** Private constructor, cannot be instantiated. */
    private PcmkTest1() {
        /* Cannot be instantiated. */
    }
}
