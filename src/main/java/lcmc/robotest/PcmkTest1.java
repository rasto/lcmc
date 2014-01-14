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

import static lcmc.robotest.RoboTest.*;
import lcmc.Exceptions;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import lcmc.data.Cluster;
import lcmc.gui.widget.GenericWidget.MTextField;
import lcmc.gui.widget.MComboBox;
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

    /** Private constructor, cannot be instantiated. */
    private PcmkTest1() {
        /* Cannot be instantiated. */
    }

    static void start(final Cluster cluster) {
        RoboTest.slowFactor = 0.4f;
        RoboTest.aborted = false;
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
            LOG.appWarning("start: " + e.getMessage(), e);
        }

        disableStonith();
        checkTest(testName, 1);
        enableStonith();
        checkTest(testName, 1.1);
        disableStonith();
        checkTest(testName, 1);
        moveTo(ipX, ipY + 200);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        sleep(1000);
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("OCF Resource Agents");
        press(KeyEvent.VK_D);
        sleep(200);
        press(KeyEvent.VK_R);
        sleep(200);
        press(KeyEvent.VK_B);
        sleep(200);
        press(KeyEvent.VK_D);
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);
        moveTo(Tools.getString("ConfirmDialog.Yes"));
        sleep(2000);
        leftClick();
        removeResource(ipX, ipY + 200, !CONFIRM_REMOVE);

        moveTo(ipX, ipY + 200);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        sleep(1000);
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("IPaddr2");
        leftClick();

        moveTo(ipX, ipY);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        sleep(1000);
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
        sleep(1000);
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("IPaddr2");
        leftClick();

        moveTo("IPv4 ", MComboBox.class);
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
        moveTo(Tools.getString("Browser.ApplyResource"));
        sleep(6000); /* ptest */
        leftClick(); /* apply */
        /* CIDR netmask 24 */
        sleep(10000);

        moveTo("CIDR netmask", MTextField.class); /* CIDR */
        sleep(3000);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_2);
        sleep(200);
        press(KeyEvent.VK_4);
        sleep(1000);

        moveTo(Tools.getString("Browser.ApplyResource"));
        sleep(6000); /* ptest */
        leftClick(); /* apply */

        checkTest(testName, 2); /* 2 */

        /* pingd */
        moveScrollBar(true);
        moveTo("pingd", MComboBox.class);
        leftClick();
        sleep(500);
        press(KeyEvent.VK_DOWN); /* no ping */
        sleep(200);
        press(KeyEvent.VK_DOWN); /* no ping */
        sleep(200);
        press(KeyEvent.VK_ENTER); /* no ping */
        sleep(200);
        moveTo(Tools.getString("Browser.ApplyResource"));
        sleep(2000);
        leftClick();
        sleep(2000);
        checkTest(testName, 2.1); /* 2.1 */

        moveTo("pingd", MComboBox.class);
        leftClick();
        sleep(500);
        press(KeyEvent.VK_UP); /* no ping */
        sleep(200);
        press(KeyEvent.VK_UP); /* no ping */
        sleep(200);
        press(KeyEvent.VK_ENTER); /* no ping */
        sleep(200);
        moveTo(Tools.getString("Browser.ApplyResource"));
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
        removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        moveTo(gx, gy);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        leftClick();
        sleep(3000);

        rightClick(); /* group popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        sleep(1000);
        moveTo("OCF Resource Agents");
        sleep(1000);
        typeDummy();
        sleep(1000);

        /* remove it */
        removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        /* group with dummy resources, once again */
        moveTo(gx, gy);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        leftClick();
        sleep(3000);
        rightClick(); /* group popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        sleep(1000);
        moveTo("OCF Resource Agents");
        sleep(1000);
        typeDummy();
        sleep(1000);

        moveToMenu("Dummy (1)");
        rightClick();
        sleep(1000);

        moveTo(Tools.getString("ClusterBrowser.Hb.RemoveService"));
        leftClick(); /* menu remove service */
        removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        /* group with dummy resources, once again */
        moveTo(gx, gy);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        leftClick();
        sleep(3000);
        rightClick(); /* group popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        sleep(1000);
        moveTo("OCF Resource Agents");
        sleep(1000);
        typeDummy();
        sleep(1000);

        removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        /* once again */
        moveTo(gx, gy);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        leftClick();
        sleep(3000);

        checkTest(testName, 2); /* 2 */

        rightClick(); /* group popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        sleep(1000);
        moveTo("OCF Resource Agents");
        sleep(1000);
        typeDummy();
        sleep(1000);

        setTimeouts(true);
        moveTo(Tools.getString("Browser.ApplyResource"));
        sleep(2000);
        leftClick();
        for (int i = 0; i < 2; i++) {
            /* another group resource */
            moveTo(gx + 10, gy - 25);
            rightClick(); /* popup */
            sleep(10000);
            moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
            sleep(1000);
            moveTo("OCF Resource Agents");
            sleep(1000);
            typeDummy();
            sleep(i * 300);
            setTimeouts(true);
            moveTo(Tools.getString("Browser.ApplyResource"));
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
                    moveTo(Tools.getString("ClusterBrowser.Hb.ResGrpMoveUp"));
                    leftClick(); /* move res 3 up */
                    sleepNoFactor(2000);
                    checkTest(testName, 3.11); /* 3.11 */
                    moveToMenu("Dummy (3)");
                    rightClick();
                    moveTo(Tools.getString("ClusterBrowser.Hb.ResGrpMoveDown"));
                    leftClick();
                    sleepNoFactor(2000);
                    checkTest(testName, 3.12); /* 3.12 */
                }
            }
        } catch (Exceptions.IllegalVersionException e) {
            LOG.appWarning("start: " + e.getMessage(), e);
        }

        moveToMenu("Dummy (3)");
        sleep(1000);
        leftClick();

        moveScrollBar(true);

        for (int i = 0; i < 2; i++) {
            sleep(1000);
            moveTo(Tools.getString("ClusterBrowser.SameAs"),
                   2,
                   MComboBox.class);
            sleep(2000);
            leftClick();
            sleep(1000);
            press(KeyEvent.VK_DOWN); /* choose another dummy */
            sleep(20000);
            press(KeyEvent.VK_DOWN);
            sleep(20000);
            press(KeyEvent.VK_ENTER);
            sleep(10000);
            moveTo(Tools.getString("Browser.ApplyResource"));
            sleep(4000);
            leftClick();
            sleep(4000);
            checkTest(testName, 3.2); /* 3.2 */

            moveTo(Tools.getString("ClusterBrowser.SameAs"),
                   2,
                   MComboBox.class);
            sleep(2000);
            leftClick();
            sleep(1000);

            press(KeyEvent.VK_PAGE_UP); /* choose "nothing selected */
            sleep(10000);
            press(KeyEvent.VK_ENTER);
            sleep(10000);

            moveTo(Tools.getString("Browser.ApplyResource"));
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
        moveTo(Tools.getString("Browser.ActionsMenu"));
        sleep(2000);
        rightClick();
        moveTo(Tools.getString("ClusterBrowser.Hb.StopResource"));
        sleep(6000);
        leftClick();
        sleep(5000);
        checkTest(testName, 11.501);

        moveTo(ipX + 20, ipY + 10);
        leftClick(); /* choose ip */
        sleep(10000);
        moveTo(Tools.getString("Browser.ActionsMenu"));
        sleep(1000);
        rightClick(); /* popup */
        sleep(2000);
        moveTo(Tools.getString("ClusterBrowser.Hb.StartResource").substring(1));
        /* actions menu start */
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
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
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

        moveTo(Tools.getString("Browser.ApplyResource"));
        sleep(5000);
        leftClick();
        checkTest(testName, 11.9);
        sleep(3000);
        /* set clone max to 1 */
        moveTo("Clone Max", MComboBox.class);
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
        moveTo(Tools.getString("Browser.ApplyResource"));
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
        moveTo(Tools.getString("Browser.ActionsMenu"));
        sleep(6000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.UnmigrateResource"));
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
        moveTo(Tools.getString("PcmkMultiSelectionInfo.StopSelectedResources"));
        leftClick();
        checkTest(testName, 28.3);

        moveTo(700, 450);
        leftPress();
        moveTo(220, 65);
        leftRelease();
        moveTo(ipX, ipY);
        rightClick();
        sleep(1000);
        moveTo(
             Tools.getString("PcmkMultiSelectionInfo.StartSelectedResources"));
        leftClick();
        checkTest(testName, 28.4);
        sleep(10000);
        moveTo(700, 520); /* reset selection */
        leftClick();

        stopResource(ipX, ipY);
        sleep(5000);
        moveTo(gx, gy);
        leftClick();
        moveTo(Tools.getString("Browser.ActionsMenu"));
        stopGroup();
        sleep(5000);
        moveTo(statefulX, statefulY);
        stopGroup();
        sleep(5000);
        checkTest(testName, 29);

        if (true) {
            removeResource(ipX, ipY, RoboTest.CONFIRM_REMOVE);
            sleep(5000);
            removeGroup(gx, gy - 20);
            sleep(5000);
            removeGroup(statefulX, statefulY);
        } else {
            removeEverything();
        }
        if (!RoboTest.aborted) {
            sleepNoFactor(20000);
        }
        checkTest(testName, 1);
    }
}
