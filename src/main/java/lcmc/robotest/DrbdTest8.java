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

import java.awt.event.KeyEvent;
import lcmc.Exceptions;
import lcmc.data.Cluster;
import lcmc.gui.widget.Widget;
import static lcmc.robotest.RoboTest.*;
import static lcmc.robotest.DrbdTest1.*;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class DrbdTest8 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(DrbdTest8.class);

    /** Private constructor, cannot be instantiated. */
    private DrbdTest8() {
        /* Cannot be instantiated. */
    }

    /** DRBD Test 8 / proxy. */
    static void start(final Cluster cluster, final int blockDevY) {
        /* Two drbds. */
        final String drbdTest = "drbd-test8";
        slowFactor = 0.2f;
        aborted = false;
        int offset = 0;
        for (int i = 0; i < 2; i++) {
            addDrbdResource(cluster, blockDevY + offset);
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

        moveTo("Detach Selected");
        leftClick();
        checkDRBDTest(drbdTest, 2.01);

        moveTo(400, blockDevY);
        rightClick();
        moveTo("Attach Selected");
        leftClick();
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
            LOG.appWarning(e.getMessage(), e);
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
            LOG.appWarning(e.getMessage(), e);
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
}
