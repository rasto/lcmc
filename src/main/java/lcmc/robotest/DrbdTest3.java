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

import javax.inject.Named;
import javax.inject.Singleton;

import lcmc.Exceptions;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.widget.GenericWidget.MTextField;
import lcmc.cluster.ui.widget.MComboBox;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.MainPanel;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class DrbdTest3 {
    private final RoboTest roboTest;
    private final DrbdTest1 drbdTest1;
    private static final Logger LOG = LoggerFactory.getLogger(DrbdTest3.class);
    private final MainPanel mainPanel;

    public DrbdTest3(RoboTest roboTest, DrbdTest1 drbdTest1, MainPanel mainPanel) {
        this.roboTest = roboTest;
        this.drbdTest1 = drbdTest1;
        this.mainPanel = mainPanel;
    }

    void start(final Cluster cluster, final int blockDevY) {
        /* Two drbds. */
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);
        int offset = 0;
        final String drbdTest = "drbd-test3";
        for (int i = 0; i < 2; i++) {
            drbdTest1.addDrbdResource(cluster, blockDevY + offset);
            if (i == 1 && cluster.getHostsArray()[0].hasVolumes()) {
                drbdTest1.newDrbdResource();
            }
            drbdTest1.chooseDrbdResource(cluster);

            drbdTest1.addDrbdVolume();
            drbdTest1.addBlockDevice();
            drbdTest1.addBlockDevice();
            roboTest.sleep(20000);

            if (offset == 0) {
                roboTest.checkDRBDTest(drbdTest, 1.1);
            } else {
                roboTest.checkDRBDTest(drbdTest, 1.2);
            }
            roboTest.sleep(10000);
            drbdTest1.addMetaData();
            drbdTest1.addFileSystem();
            roboTest.sleep(10000);
            roboTest.moveTo(Tools.getString("Dialog.Dialog.Finish"));
            roboTest.leftClick();
            roboTest.sleep(10000);

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

        roboTest.moveTo("Detach Selected");
        roboTest.leftClick();
        roboTest.checkDRBDTest(drbdTest, 2.01);

        roboTest.moveTo(400, blockDevY);
        roboTest.rightClick();
        roboTest.moveTo("Attach Selected");
        roboTest.leftClick();
        roboTest.checkDRBDTest(drbdTest, 2.02);

        roboTest.moveTo(480, 152); /* select r0 */
        roboTest.leftClick();

        roboTest.moveTo("Protocol", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_UP); /* protocol b */
        roboTest.press(KeyEvent.VK_ENTER);

        roboTest.moveTo("Fence peer", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_DOWN); /* select dopd */
        roboTest.press(KeyEvent.VK_ENTER);
        mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);

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
        Tools.sleep(10000);
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

        roboTest.moveTo("Protocol", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_DOWN); /* protocol c */
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
