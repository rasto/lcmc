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
import lcmc.gui.widget.GenericWidget.MTextField;
import lcmc.gui.widget.MComboBox;
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
        slowFactor = 0.2f;
        aborted = false;
        int offset = 0;
        final String drbdTest = "drbd-test8";
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
            moveTo(700, 450);
            leftClick();

            moveTo(Tools.getString("ResourceInfo.ProxyOutsideIp"),
                   MComboBox.class); /* outside */
            leftClick();
            press(KeyEvent.VK_E);
            press(KeyEvent.VK_ENTER);

            moveTo(MComboBox.class, 8); /* outside */
            leftClick();
            press(KeyEvent.VK_E);
            press(KeyEvent.VK_ENTER);

            drbdNext();
            dialogColorTest("chooseDrbdResource");

            addDrbdVolume();
            addBlockDevice();
            addBlockDevice();

            if (offset == 0) {
                checkDRBDTest(drbdTest, 1.1);
            } else {
                checkDRBDTest(drbdTest, 1.2);
            }
            addMetaData();
            addFileSystem();
            moveTo(Tools.getString("Dialog.Dialog.Finish"));
            leftClick();

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

        moveTo(Tools.getString("MultiSelectionInfo.Detach"));
        leftClick();
        checkDRBDTest(drbdTest, 2.01);

        moveTo(400, blockDevY);
        rightClick();
        moveTo(Tools.getString("MultiSelectionInfo.Attach"));
        leftClick();
        checkDRBDTest(drbdTest, 2.02);

        moveTo(480, 152); /* select r0 */
        leftClick();

        moveTo(900, 300);
        Tools.sleep(1000);
        robot.mouseWheel(100);
        Tools.sleep(1000);

        moveTo("Protocol", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN); /* protocol b */
        press(KeyEvent.VK_ENTER);

        moveTo("Fence peer", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_DOWN); /* select dopd */
        press(KeyEvent.VK_ENTER);
        Tools.getGUIData().expandTerminalSplitPane(1);

        moveTo("Wfc timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        press(KeyEvent.VK_9);

        moveTo("Max buffers", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        press(KeyEvent.VK_5);
        moveTo("Max buffers", MComboBox.class); /* Unit */
        leftClick();
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER);

        moveScrollBar(true);
        final String v = cluster.getHostsArray()[0].getDrbdVersion();
        try {
            if (v != null && Tools.compareVersions(v, "8.4.0") < 0) {
                moveTo("After", MComboBox.class);
            } else {
                moveTo("after", MComboBox.class);
            }
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("start: " + e.getMessage(), e);
        }
        leftClick();
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER);

        moveScrollBar(false);

        moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        leftClick(); /* apply/disables tooltip */
        leftClick();
        checkDRBDTest(drbdTest, 2.1); /* 2.1 */


        /* common */
        moveTo(500, 342); /* select background */
        leftClick();
        leftClick();

        moveTo("Wfc timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        press(KeyEvent.VK_3);

        moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        leftClick(); /* apply/disables tooltip */
        leftClick();
        checkDRBDTest(drbdTest, 2.11); /* 2.11 */
        moveTo("Wfc timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        press(KeyEvent.VK_0);

        moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        leftClick(); /* apply/disables tooltip */
        leftClick();

        /* resource */
        moveTo(480, 152); /* select r0 */
        leftClick();
        leftClick();

        moveTo(900, 300);
        Tools.sleep(1000);
        robot.mouseWheel(100);
        Tools.sleep(1000);

        moveTo("Protocol", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_UP); /* protocol a */
        press(KeyEvent.VK_ENTER);

        moveTo("Fence peer", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_UP); /* deselect dopd */
        press(KeyEvent.VK_ENTER);

        moveTo("Wfc timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        press(KeyEvent.VK_5);

        moveTo("Max buffers", MTextField.class);
        leftClick();
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        press(KeyEvent.VK_2);
        press(KeyEvent.VK_0);
        press(KeyEvent.VK_4);
        press(KeyEvent.VK_8);
        moveTo("Max buffers", MComboBox.class); /* Unit */
        leftClick();
        press(KeyEvent.VK_UP);
        press(KeyEvent.VK_ENTER);

        moveScrollBar(true);
        try {
            if (v != null && Tools.compareVersions(v, "8.4.0") < 0) {
                moveTo("After", MComboBox.class);
            } else {
                moveTo("after", MComboBox.class);
            }
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("start: " + e.getMessage(), e);
        }
        leftClick();
        press(KeyEvent.VK_UP);
        press(KeyEvent.VK_ENTER);

        moveScrollBar(false);

        moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        leftClick(); /* apply/disables tooltip */
        leftClick();
        checkDRBDTest(drbdTest, 2.2); /* 2.2 */

        moveTo("Wfc timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        press(KeyEvent.VK_0);

        moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        leftClick();
        checkDRBDTest(drbdTest, 2.3); /* 2.3 */

        moveTo(480, 152); /* rsc popup */
        rightClick();
        moveTo(Tools.getString("ClusterBrowser.Drbd.RemoveEdge"));
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 3);
        moveTo(480, 152); /* rsc popup */
        rightClick();
        moveTo(Tools.getString("ClusterBrowser.Drbd.RemoveEdge"));
        leftClick();
        confirmRemove();
        checkDRBDTest(drbdTest, 4);
    }
}
