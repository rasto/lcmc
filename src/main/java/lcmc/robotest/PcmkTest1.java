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
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.widget.GenericWidget.MTextField;
import lcmc.cluster.ui.widget.MComboBox;
import static lcmc.robotest.RoboTest.CONFIRM_REMOVE;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;
import lombok.RequiredArgsConstructor;

/**
 * This class is used to test the GUI.
 */
@RequiredArgsConstructor
final class PcmkTest1 {
    private final RoboTest roboTest;
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTest1.class);

    void start(final Cluster cluster) {
        roboTest.setSlowFactor(0.4f);
        roboTest.setAborted(false);
        /* create IPaddr2 with 192.168.100.100 ip */
        String testName = "test1";
        final String pmV = cluster.getHostsArray()[0].getHostParser().getPacemakerVersion();
        try {
            if (pmV != null && Tools.compareVersions(pmV, "1.1.6") < 0) {
                testName = "test1-1.0";
            }
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("start: " + e.getMessage(), e);
        }

        roboTest.disableStonith();
        roboTest.checkTest(testName, 1);
        roboTest.enableStonith();
        roboTest.checkTest(testName, 1.1);
        roboTest.disableStonith();
        roboTest.checkTest(testName, 1);
        final int ipX = 235;
        final int ipY = 207;
        roboTest.moveTo(ipX, ipY + 200);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        roboTest.moveTo("Filesystem + Linbit:DRBD");
        roboTest.moveTo("OCF Resource Agents");
        roboTest.press(KeyEvent.VK_D);
        roboTest.press(KeyEvent.VK_R);
        roboTest.press(KeyEvent.VK_B);
        roboTest.press(KeyEvent.VK_D);
        roboTest.press(KeyEvent.VK_ENTER);
        roboTest.removeResource(ipX, ipY + 200, !CONFIRM_REMOVE);

        roboTest.moveTo(ipX, ipY + 200);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        roboTest.moveTo("Filesystem + Linbit:DRBD");
        roboTest.moveTo("IPaddr2");
        roboTest.leftClick();

        roboTest.moveTo(ipX, ipY);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        roboTest.moveTo("Filesystem + Linbit:DRBD");
        roboTest.moveTo("IPaddr2");
        roboTest.leftClick();

        final float savedSlowFactor = roboTest.getSlowFactor();
        roboTest.setSlowFactor(0.00001f);
        for (final Integer pos1 : new Integer[]{850, 900, 1000}) {
            for (final Integer pos2 : new Integer[]{850, 900, 1000}) {
                if (pos1 == pos2) {
                    continue;
                }
                for (int i = 0; i < 70; i++) {
                    roboTest.moveTo(pos1, RoboTest.CLONE_RADIO_Y);
                    roboTest.getRobot().mousePress(InputEvent.BUTTON1_MASK);
                    Tools.sleep(20);
                    roboTest.getRobot().mouseRelease(InputEvent.BUTTON1_MASK);
                    roboTest.moveTo(pos2, RoboTest.CLONE_RADIO_Y);
                    roboTest.getRobot().mousePress(InputEvent.BUTTON1_MASK);
                    Tools.sleep(20);
                    roboTest.getRobot().mouseRelease(InputEvent.BUTTON1_MASK);
                }
            }
        }
        roboTest.setSlowFactor(savedSlowFactor);
        roboTest.removeResource(ipX, ipY + 200, !RoboTest.CONFIRM_REMOVE);
        roboTest.removeResource(ipX, ipY, !RoboTest.CONFIRM_REMOVE);
        /* again */
        roboTest.moveTo(ipX, ipY);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        roboTest.moveTo("Filesystem + Linbit:DRBD");
        roboTest.moveTo("IPaddr2");
        roboTest.leftClick();

        roboTest.moveTo("IPv4 ", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_ENTER);
        roboTest.press(KeyEvent.VK_1);
        roboTest.press(KeyEvent.VK_0);
        roboTest.press(KeyEvent.VK_0);
        roboTest.setTimeouts(false);
        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick(); /* apply */
        /* CIDR netmask 24 */

        roboTest.moveTo("CIDR netmask", MTextField.class); /* CIDR */
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_2);
        roboTest.press(KeyEvent.VK_4);

        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick(); /* apply */

        roboTest.checkTest(testName, 2); /* 2 */

        /* pingd */
        roboTest.moveScrollBar(true);
        roboTest.moveTo("pingd", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_DOWN); /* no ping */
        roboTest.press(KeyEvent.VK_DOWN); /* no ping */
        roboTest.press(KeyEvent.VK_ENTER); /* no ping */
        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick();
        roboTest.checkTest(testName, 2.1); /* 2.1 */

        roboTest.moveTo("pingd", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_UP); /* no ping */
        roboTest.press(KeyEvent.VK_UP); /* no ping */
        roboTest.press(KeyEvent.VK_ENTER); /* no ping */
        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick();

        roboTest.moveScrollBar(false);

        /* group with dummy resources */
        final int gx = 230;
        final int gy = 305;
        roboTest.moveTo(gx, gy);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(gx + 46, gy + 11);
        roboTest.leftClick(); /* choose group */
        /* remove it */
        roboTest.removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        roboTest.moveTo(gx, gy);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        roboTest.leftClick();

        roboTest.rightClick(); /* group popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        roboTest.moveTo("OCF Resource Agents");
        roboTest.typeDummy();

        /* remove it */
        roboTest.removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        /* group with dummy resources, once again */
        roboTest.moveTo(gx, gy);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        roboTest.leftClick();
        roboTest.rightClick(); /* group popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        roboTest.moveTo("OCF Resource Agents");
        roboTest.typeDummy();

        roboTest.moveToMenu("Dummy (1)");
        roboTest.rightClick();

        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.RemoveService"));
        roboTest.leftClick(); /* menu remove service */
        roboTest.removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        /* group with dummy resources, once again */
        roboTest.moveTo(gx, gy);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        roboTest.leftClick();
        roboTest.rightClick(); /* group popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        roboTest.moveTo("OCF Resource Agents");
        roboTest.typeDummy();

        roboTest.removeResource(gx, gy, !RoboTest.CONFIRM_REMOVE);

        /* once again */
        roboTest.moveTo(gx, gy);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        roboTest.leftClick();

        roboTest.checkTest(testName, 2); /* 2 */

        roboTest.rightClick(); /* group popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
        roboTest.moveTo("OCF Resource Agents");
        roboTest.typeDummy();

        roboTest.setTimeouts(true);
        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick();
        for (int i = 0; i < 2; i++) {
            /* another group resource */
            roboTest.moveTo(gx + 10, gy - 25);
            roboTest.rightClick(); /* popup */
            roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
            roboTest.moveTo("OCF Resource Agents");
            roboTest.typeDummy();
            roboTest.setTimeouts(true);
            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
        }
        roboTest.checkTest(testName, 3); /* 3 */

        /* constraints */
        roboTest.moveTo(gx + 10, gy - 25);
        roboTest.addConstraint(1);
        roboTest.checkTest(testName, 3.1); /* 3.1 */

        try {
            if (false && pmV != null && Tools.compareVersions(pmV, "1.0.8") > 0) {
                /* move up, move down */
                for (int i = 0; i < 2; i++) {
                    roboTest.moveToMenu("Dummy (3)");
                    roboTest.rightClick();
                    roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.ResGrpMoveUp"));
                    roboTest.leftClick(); /* move res 3 up */
                    roboTest.checkTest(testName, 3.11); /* 3.11 */
                    roboTest.moveToMenu("Dummy (3)");
                    roboTest.rightClick();
                    roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.ResGrpMoveDown"));
                    roboTest.leftClick();
                    roboTest.checkTest(testName, 3.12); /* 3.12 */
                }
            }
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("start: " + e.getMessage(), e);
        }

        roboTest.moveToMenu("Dummy (3)");
        roboTest.leftClick();

        roboTest.moveScrollBar(true);

        for (int i = 0; i < 2; i++) {
            roboTest.moveTo(Tools.getString("ClusterBrowser.SameAs"), 2, MComboBox.class);
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_DOWN); /* choose another dummy */
            roboTest.press(KeyEvent.VK_DOWN);
            roboTest.press(KeyEvent.VK_ENTER);
            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
            roboTest.checkTest(testName, 3.2); /* 3.2 */

            roboTest.moveTo(Tools.getString("ClusterBrowser.SameAs"), 2, MComboBox.class);
            roboTest.leftClick();

            roboTest.press(KeyEvent.VK_PAGE_UP); /* choose "nothing selected */
            roboTest.press(KeyEvent.VK_ENTER);

            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
            roboTest.checkTest(testName, 4); /* 4 */
            roboTest.sleep(500);
        }
        roboTest.moveScrollBar(false);

        /* locations */
        roboTest.moveTo(ipX + 20, ipY);
        roboTest.leftClick(); /* choose ip */
        roboTest.setLocation(new Integer[]{KeyEvent.VK_I});
        roboTest.checkTest(testName, 4.1); /* 4.1 */

        roboTest.setLocation(new Integer[]{KeyEvent.VK_END,
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
        roboTest.checkTest(testName, 4.2); /* 4.2 */

        roboTest.setLocation(new Integer[]{KeyEvent.VK_END,
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
        roboTest.checkTest(testName, 4.3); /* 4.3 */

        roboTest.setLocation(new Integer[]{KeyEvent.VK_END,
                                           KeyEvent.VK_BACK_SPACE,
                                           KeyEvent.VK_BACK_SPACE,
                                           KeyEvent.VK_BACK_SPACE,
                                           KeyEvent.VK_BACK_SPACE,
                                           KeyEvent.VK_BACK_SPACE,
                                           KeyEvent.VK_BACK_SPACE,
                                           KeyEvent.VK_BACK_SPACE,
                                           KeyEvent.VK_BACK_SPACE,
                                           KeyEvent.VK_BACK_SPACE});
        roboTest.checkTest(testName, 4.4); /* 4.4 */
        final int popX = 340;
        final int popY = 257;
        roboTest.removeConstraint(popX, popY);
        roboTest.checkTest(testName, 5); /* 5 */
        roboTest.moveTo(gx + 10, gy - 25);
        roboTest.addConstraint(1);
        roboTest.checkTest(testName, 5.1); /* 4.4 */

        roboTest.removeConstraint(popX, popY);
        roboTest.checkTest(testName, 5.2); /* 5 */

        roboTest.moveTo(gx + 10, gy - 25);
        roboTest.addConstraint(1);
        roboTest.checkTest(testName, 6); /* 6 */

        roboTest.removeOrder(popX, popY);
        roboTest.checkTest(testName, 7);

        roboTest.addOrder(popX, popY);
        roboTest.checkTest(testName, 8);

        roboTest.removeColocation(popX, popY);
        roboTest.checkTest(testName, 9);

        roboTest.addColocation(popX, popY);
        roboTest.checkTest(testName, 10);

        roboTest.removeColocation(popX, popY);
        roboTest.checkTest(testName, 10.1);

        roboTest.removeOrder(popX, popY);
        roboTest.checkTest(testName, 10.2);

        roboTest.addConstraintOrderOnly(gx, gy - 25, 2);
        roboTest.checkTest(testName, 10.3);

        roboTest.addColocation(popX, popY);
        roboTest.checkTest(testName, 10.4);

        roboTest.removeOrder(popX, popY);
        roboTest.checkTest(testName, 10.5);

        roboTest.removeColocation(popX, popY);
        roboTest.checkTest(testName, 10.6);

        roboTest.addConstraintColocationOnly(gx, gy - 25, 2);
        roboTest.checkTest(testName, 10.7);

        roboTest.addOrder(popX, popY);
        roboTest.checkTest(testName, 10.8);

        roboTest.removeConstraint(popX, popY);
        roboTest.checkTest(testName, 10.9);

        roboTest.moveTo(ipX, ipY);
        roboTest.addConstraint(1);
        roboTest.checkTest(testName, 10.91);

        roboTest.removeOrder(popX, popY);
        roboTest.checkTest(testName, 10.92);

        roboTest.addOrder(popX, popY);
        roboTest.checkTest(testName, 10.93);

        roboTest.removeColocation(popX, popY);
        roboTest.checkTest(testName, 10.94);

        roboTest.addColocation(popX, popY);
        roboTest.checkTest(testName, 10.95);

        roboTest.removeColocation(popX, popY);
        roboTest.checkTest(testName, 10.96);

        roboTest.removeOrder(popX, popY);
        roboTest.checkTest(testName, 10.97);

        roboTest.addConstraintColocationOnly(ipX, ipY, 1);
        roboTest.checkTest(testName, 10.98);

        roboTest.removeColocation(popX, popY);
        roboTest.checkTest(testName, 10.99);

        roboTest.addConstraintOrderOnly(ipX, ipY, 1);
        roboTest.checkTest(testName, 11);

        roboTest.addColocation(popX, popY);
        roboTest.checkTest(testName, 11.1);

        roboTest.removeConstraint(popX, popY);
        roboTest.checkTest(testName, 11.2);

        roboTest.moveTo(ipX, ipY);
        roboTest.addConstraint(3);
        roboTest.checkTest(testName, 11.3);
        roboTest.stopResource(ipX, ipY);
        roboTest.checkTest(testName, 11.4);
        roboTest.resetStartStopResource(ipX, ipY);
        roboTest.checkTest(testName, 11.5);

        roboTest.moveTo(ipX + 20, ipY + 10);
        roboTest.leftClick(); /* choose ip */
        roboTest.moveTo(Tools.getString("Browser.ActionsMenu"));
        roboTest.rightClick();
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.StopResource"));
        roboTest.leftClick();
        roboTest.checkTest(testName, 11.501);

        roboTest.moveTo(ipX + 20, ipY + 10);
        roboTest.leftClick(); /* choose ip */
        roboTest.moveTo(Tools.getString("Browser.ActionsMenu"));
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.StartResource").substring(1));
        /* actions menu start */
        roboTest.leftClick();
        roboTest.checkTest(testName, 11.502);

        roboTest.resetStartStopResource(ipX, ipY);
        roboTest.checkTest(testName, 11.5);

        roboTest.removeColocation(popX, popY);
        roboTest.checkTest(testName, 11.51);

        roboTest.addColocation(popX, popY);
        roboTest.checkTest(testName, 11.52);

        roboTest.removeOrder(popX, popY);
        roboTest.checkTest(testName, 11.53);

        roboTest.addOrder(popX, popY);
        roboTest.checkTest(testName, 11.54);

        roboTest.removeColocation(popX, popY);
        roboTest.checkTest(testName, 11.55);

        roboTest.removeOrder(popX, popY);
        roboTest.checkTest(testName, 11.56);

        roboTest.addConstraintOrderOnly(ipX, ipY, 3);
        roboTest.checkTest(testName, 11.57);

        roboTest.removeOrder(popX, popY);
        roboTest.checkTest(testName, 11.58);

        roboTest.addConstraintColocationOnly(ipX, ipY, 3);
        roboTest.checkTest(testName, 11.59);

        roboTest.addOrder(popX, popY);
        roboTest.checkTest(testName, 11.6);

        roboTest.removeConstraint(popX, popY);
        roboTest.checkTest(testName, 11.7);

        roboTest.moveTo(gx + 10, gy - 25);
        roboTest.addConstraint(1);
        roboTest.checkTest(testName, 11.8);
        /** Add m/s Stateful resource */
        final int statefulX = 500;
        final int statefulY = 207;
        roboTest.moveTo(statefulX, statefulY);
        roboTest.rightClick();
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        roboTest.moveTo("Filesystem + Linbit:DRBD");
        roboTest.moveTo("OCF Resource Agents");

        roboTest.press(KeyEvent.VK_S);
        roboTest.press(KeyEvent.VK_T);
        roboTest.press(KeyEvent.VK_A);
        roboTest.press(KeyEvent.VK_T);
        roboTest.press(KeyEvent.VK_E);
        roboTest.press(KeyEvent.VK_F);
        roboTest.press(KeyEvent.VK_ENTER); /* choose Stateful */

        Tools.sleep(400);
        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick();
        roboTest.checkTest(testName, 11.9);
        /* set clone max to 1 */
        roboTest.moveTo("Clone Max", MComboBox.class);
        roboTest.leftClick();
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_BACK_SPACE);
        roboTest.press(KeyEvent.VK_1);
        roboTest.setTimeouts(false);
        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick();
        roboTest.checkTest(testName, 12);
        roboTest.stopResource(statefulX, statefulY);
        roboTest.checkTest(testName, 13);

        roboTest.startResource(statefulX, statefulY);
        roboTest.checkTest(testName, 14);
        roboTest.unmanageResource(statefulX, statefulY);
        roboTest.checkTest(testName, 15);
        roboTest.manageResource(statefulX, statefulY);
        roboTest.checkTest(testName, 16);

        /* IP addr cont. */
        roboTest.stopResource(ipX, ipY);
        roboTest.checkTest(testName, 17);
        roboTest.startResource(ipX, ipY);
        roboTest.checkTest(testName, 18);
        roboTest.unmanageResource(ipX, ipY);
        roboTest.checkTest(testName, 19);
        roboTest.manageResource(ipX, ipY);
        roboTest.checkTest(testName, 20);
        roboTest.migrateResource(ipX, ipY);
        roboTest.checkTest(testName, 21);
        roboTest.unmigrateResource(ipX, ipY);
        roboTest.checkTest(testName, 22);

        /* Group cont. */
        roboTest.stopResource(gx, gy - 25);
        roboTest.checkTest(testName, 23);
        roboTest.startResource(gx, gy - 25);
        roboTest.checkTest(testName, 24);
        roboTest.unmanageResource(gx, gy - 25);
        roboTest.checkTest(testName, 25);
        roboTest.manageResource(gx, gy - 25);
        roboTest.checkTest(testName, 26);
        roboTest.migrateResource(gx, gy - 25);
        roboTest.moveTo(gx, gy);
        roboTest.leftClick();
        roboTest.checkTest(testName, 27);
        roboTest.moveTo(Tools.getString("Browser.ActionsMenu"));
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.UnmigrateResource"));
        roboTest.leftClick(); /* stop */
        roboTest.checkTest(testName, 28);

        roboTest.moveTo(700, 450); /* rectangle */
        roboTest.leftPress();
        roboTest.moveTo(220, 65);
        roboTest.leftRelease();

        roboTest.moveTo(ipX, ipY);
        roboTest.rightClick();
        roboTest.moveTo(ipX + 30, ipY); /* ptest */
        roboTest.moveToSlowly(ipX + 30, ipY + 350);
        roboTest.moveTo(ipX, ipY);

        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_ENTER); /* standby selected hosts */
        roboTest.checkTest(testName, 28.1);

        roboTest.moveTo(700, 450);
        roboTest.leftPress();
        roboTest.moveTo(220, 65);
        roboTest.leftRelease();
        roboTest.moveTo(ipX, ipY);
        roboTest.rightClick();
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_ENTER); /* online selected hosts */
        roboTest.checkTest(testName, 28.2);

        roboTest.moveTo(700, 450);
        roboTest.leftPress();
        roboTest.moveTo(220, 65);
        roboTest.leftRelease();
        roboTest.moveTo(ipX, ipY);
        roboTest.rightClick();
        roboTest.moveTo(Tools.getString("PcmkMultiSelectionInfo.StopSelectedResources"));
        roboTest.leftClick();
        roboTest.checkTest(testName, 28.3);

        roboTest.moveTo(700, 450);
        roboTest.leftPress();
        roboTest.moveTo(220, 65);
        roboTest.leftRelease();
        roboTest.moveTo(ipX, ipY);
        roboTest.rightClick();
        roboTest.moveTo(Tools.getString("PcmkMultiSelectionInfo.StartSelectedResources"));
        roboTest.leftClick();
        roboTest.checkTest(testName, 28.4);
        roboTest.moveTo(700, 520); /* reset selection */
        roboTest.leftClick();

        roboTest.stopResource(ipX, ipY);
        roboTest.moveTo(gx, gy);
        roboTest.leftClick();
        roboTest.moveTo(Tools.getString("Browser.ActionsMenu"));
        roboTest.stopGroup();
        roboTest.moveTo(statefulX, statefulY);
        roboTest.stopGroup();
        roboTest.checkTest(testName, 29);

        roboTest.removeResource(ipX, ipY, RoboTest.CONFIRM_REMOVE);
        roboTest.removeGroup(gx, gy - 20);
        roboTest.removeGroup(statefulX, statefulY);
        roboTest.checkTest(testName, 1);
    }
}
