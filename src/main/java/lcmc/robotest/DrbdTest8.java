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
import lcmc.gui.GUIData;
import lcmc.model.Cluster;
import lcmc.gui.widget.GenericWidget.MTextField;
import lcmc.gui.widget.MComboBox;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is used to test the GUI.
 */
@Component
final class DrbdTest8 {
    @Autowired
    private RoboTest roboTest;
    @Autowired
    private DrbdTest1 drbdTest1;
    private static final Logger LOG = LoggerFactory.getLogger(DrbdTest8.class);
    @Autowired
    private GUIData guiData;

    /** DRBD Test 8 / proxy. */
    void start(final Cluster cluster, final int blockDevY) {
        /* Two drbds. */
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);
        int offset = 0;
        final String drbdTest = "drbd-test8";
        for (int i = 0; i < 2; i++) {
            drbdTest1.addDrbdResource(cluster, blockDevY + offset);
            if (i == 1 && cluster.getHostsArray()[0].hasVolumes()) {
                drbdTest1.newDrbdResource();
            }
            drbdTest1.chooseDrbdResourceInterface(cluster.getHostsArray()[0].getName(), roboTest.PROXY);
            drbdTest1.chooseDrbdResourceInterface(cluster.getHostsArray()[1].getName(), roboTest.PROXY);

            roboTest.moveTo(700, 450);
            roboTest.leftClick();
            roboTest.getRobot().mouseWheel(70);
            roboTest.moveTo(700, 450);
            roboTest.leftClick();

            roboTest.moveTo(Tools.getString("ResourceInfo.ProxyOutsideIp"), MComboBox.class); /* outside */
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_E);
            roboTest.press(KeyEvent.VK_ENTER);

            roboTest.moveTo(MComboBox.class, 8); /* outside */
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_E);
            roboTest.press(KeyEvent.VK_ENTER);

            drbdTest1.drbdNext();
            roboTest.dialogColorTest("chooseDrbdResource");

            drbdTest1.addDrbdVolume();
            drbdTest1.addBlockDevice();
            drbdTest1.addBlockDevice();

            if (offset == 0) {
                roboTest.checkDRBDTest(drbdTest, 1.1);
            } else {
                roboTest.checkDRBDTest(drbdTest, 1.2);
            }
            drbdTest1.addMetaData();
            drbdTest1.addFileSystem();
            roboTest.moveTo(Tools.getString("Dialog.Dialog.Finish"));
            roboTest.leftClick();

            offset += 40;
        }
        roboTest.checkDRBDTest(drbdTest, 2);

        roboTest.moveTo(730, 475); /* rectangle */
        roboTest.leftPress();
        roboTest.moveTo(225, 65);
        roboTest.leftRelease();

        roboTest.moveTo(334, blockDevY);
        roboTest.rightClick();
        roboTest.moveToSlowly(400, blockDevY + 160);

        roboTest.moveTo(Tools.getString("MultiSelectionInfo.Detach"));
        roboTest.leftClick();
        roboTest.checkDRBDTest(drbdTest, 2.01);

        roboTest.moveTo(400, blockDevY);
        roboTest.rightClick();
        roboTest.moveTo(Tools.getString("MultiSelectionInfo.Attach"));
        roboTest.leftClick();
        roboTest.checkDRBDTest(drbdTest, 2.02);

        roboTest.moveTo(480, 152); /* select r0 */
        roboTest.leftClick();

        roboTest.moveTo(900, 300);
        Tools.sleep(1000);
        roboTest.getRobot().mouseWheel(100);
        Tools.sleep(1000);

        roboTest.moveTo("Protocol", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_DOWN); /* protocol b */
        roboTest.press(KeyEvent.VK_ENTER);

        roboTest.moveTo("Fence peer", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_DOWN); /* select dopd */
        roboTest.press(KeyEvent.VK_ENTER);
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);

        roboTest.moveTo("Wfc timeout", MTextField.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_BACK_SPACE);
        roboTest.press(KeyEvent.VK_9);

        roboTest.moveTo("Max epoch size", MTextField.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_BACK_SPACE);
        roboTest.press(KeyEvent.VK_5);
        roboTest.moveTo("Max epoch size", MComboBox.class); /* Unit */
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_ENTER);

        roboTest.moveScrollBar(true);
        try {
            if (cluster.getHostsArray()[0].drbdVersionSmaller("8.4.0")) {
                roboTest.moveTo("After", MComboBox.class);
            } else {
                roboTest.moveTo("after", MComboBox.class);
            }
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("start: " + e.getMessage(), e);
        }
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_ENTER);

        roboTest.moveScrollBar(false);

        roboTest.moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        roboTest.leftClick(); /* apply/disables tooltip */
        roboTest.leftClick();
        roboTest.checkDRBDTest(drbdTest, 2.1); /* 2.1 */
        /* common */
        roboTest.moveTo(500, 342); /* select background */
        roboTest.leftClick();
        roboTest.leftClick();

        roboTest.moveTo("Wfc timeout", MTextField.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_BACK_SPACE);
        roboTest.press(KeyEvent.VK_3);

        roboTest.moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        roboTest.leftClick(); /* apply/disables tooltip */
        roboTest.leftClick();
        roboTest.checkDRBDTest(drbdTest, 2.11); /* 2.11 */
        roboTest.moveTo("Wfc timeout", MTextField.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_BACK_SPACE);
        roboTest.press(KeyEvent.VK_0);

        roboTest.moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        roboTest.leftClick(); /* apply/disables tooltip */
        roboTest.leftClick();

        /* resource */
        roboTest.moveTo(480, 152); /* select r0 */
        roboTest.leftClick();
        roboTest.leftClick();

        roboTest.moveTo(900, 300);
        Tools.sleep(1000);
        roboTest.getRobot().mouseWheel(100);
        Tools.sleep(1000);

        roboTest.moveTo("Protocol", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_UP); /* protocol a */
        roboTest.press(KeyEvent.VK_ENTER);

        roboTest.moveTo("Fence peer", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_UP); /* deselect dopd */
        roboTest.press(KeyEvent.VK_ENTER);

        roboTest.moveTo("Wfc timeout", MTextField.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_BACK_SPACE);
        roboTest.press(KeyEvent.VK_5);

        roboTest.moveTo("Max epoch size", MTextField.class);
        roboTest.leftClick();
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_BACK_SPACE);
        roboTest.press(KeyEvent.VK_2);
        roboTest.press(KeyEvent.VK_0);
        roboTest.press(KeyEvent.VK_4);
        roboTest.press(KeyEvent.VK_8);
        roboTest.moveTo("Max epoch size", MComboBox.class); /* Unit */
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_UP);
        roboTest.press(KeyEvent.VK_ENTER);

        roboTest.moveScrollBar(true);
        try {
            if (cluster.getHostsArray()[0].drbdVersionSmaller("8.4.0")) {
                roboTest.moveTo("After", MComboBox.class);
            } else {
                roboTest.moveTo("after", MComboBox.class);
            }
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("start: " + e.getMessage(), e);
        }
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_UP);
        roboTest.press(KeyEvent.VK_ENTER);

        roboTest.moveScrollBar(false);

        roboTest.moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        roboTest.leftClick(); /* apply/disables tooltip */
        roboTest.leftClick();
        roboTest.checkDRBDTest(drbdTest, 2.2); /* 2.2 */

        roboTest.moveTo("Wfc timeout", MTextField.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_BACK_SPACE);
        roboTest.press(KeyEvent.VK_0);

        roboTest.moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        roboTest.leftClick();
        roboTest.checkDRBDTest(drbdTest, 2.3); /* 2.3 */

        roboTest.moveTo(480, 152); /* rsc popup */
        roboTest.rightClick();
        roboTest.moveTo(Tools.getString("ClusterBrowser.Drbd.RemoveEdge"));
        roboTest.leftClick();
        roboTest.confirmRemove();
        roboTest.checkDRBDTest(drbdTest, 3);
        roboTest.moveTo(480, 152); /* rsc popup */
        roboTest.rightClick();
        roboTest.moveTo(Tools.getString("ClusterBrowser.Drbd.RemoveEdge"));
        roboTest.leftClick();
        roboTest.confirmRemove();
        roboTest.checkDRBDTest(drbdTest, 4);
    }
}
