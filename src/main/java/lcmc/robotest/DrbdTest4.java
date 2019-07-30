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
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.widget.GenericWidget.MTextField;
import lcmc.cluster.ui.widget.MComboBox;
import lcmc.common.domain.util.Tools;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class DrbdTest4 {
    @Inject
    private RoboTest roboTest;
    @Inject
    private DrbdTest1 drbdTest1;

    void start(final Cluster cluster, final int blockDevY) {
        /* Two drbds. */
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);
        final String drbdTest = "drbd-test4";
        int offset = 0;
        for (int i = 0; i < 2; i++) {
            drbdTest1.addDrbdResource(cluster, blockDevY + offset);
            if (i == 0) {
                /* first one */
                drbdTest1.chooseDrbdResource(cluster);
            } else {
                /* existing drbd resource */
                roboTest.moveTo("DRBD Resource", MComboBox.class);
                roboTest.leftClick();
                roboTest.press(KeyEvent.VK_DOWN); /* drbd: r0 */
                roboTest.press(KeyEvent.VK_ENTER);

            	roboTest.sleep(20000);
                drbdTest1.drbdNext();
            	roboTest.sleep(20000);
            }
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
            roboTest.moveTo(Tools.getString("Dialog.Dialog.Finish")); /* fs */
            roboTest.leftClick();

            if (offset == 0) {
                roboTest.checkDRBDTest(drbdTest, 1);
            }
            offset += 40;
        }
        roboTest.checkDRBDTest(drbdTest, 2);

        roboTest.moveTo(480, 152); /* select r0 */
        roboTest.leftClick();
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

        roboTest.moveTo("Wfc timeout", MTextField.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_BACK_SPACE);
        roboTest.press(KeyEvent.VK_9);

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
        roboTest.moveTo("Remove DRBD Volume");
        roboTest.leftClick();
        roboTest.confirmRemove();
        roboTest.checkDRBDTest(drbdTest, 3);
        roboTest.moveTo(480, 152); /* rsc popup */
        roboTest.rightClick();
        roboTest.moveTo("Remove DRBD Volume");
        roboTest.leftClick();
        roboTest.confirmRemove();
        roboTest.checkDRBDTest(drbdTest, 4);
    }
}
