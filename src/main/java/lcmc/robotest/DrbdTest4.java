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
import lcmc.data.Cluster;
import lcmc.gui.widget.GenericWidget.MTextField;
import lcmc.gui.widget.MComboBox;
import static lcmc.robotest.RoboTest.*;
import static lcmc.robotest.DrbdTest1.*;
import lcmc.utilities.Tools;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class DrbdTest4 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(DrbdTest4.class);

    /** Private constructor, cannot be instantiated. */
    private DrbdTest4() {
        /* Cannot be instantiated. */
    }

    static void start(final Cluster cluster, final int blockDevY) {
        final String drbdTest = "drbd-test4";
        /* Two drbds. */
        slowFactor = 0.2f;
        aborted = false;
        int offset = 0;
        int protocolY = 552;
        int correctionY = 0;
        if (!cluster.getHostsArray()[0].hasVolumes()) {
            protocolY = 352;
            correctionY = 30;
        }
        for (int i = 0; i < 2; i++) {
            addDrbdResource(cluster, blockDevY + offset);
            if (i == 0) {
                /* first one */
                chooseDrbdResource(cluster);
            } else {
                /* existing drbd resource */
                moveTo("DRBD Resource", MComboBox.class);
                leftClick();
                press(KeyEvent.VK_DOWN); /* drbd: r0 */
                sleep(200);
                press(KeyEvent.VK_ENTER);

                drbdNext();
                sleep(10000);

                //addDrbdVolume();
            }
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
            sleep(20000);
            moveTo(Tools.getString("Dialog.Dialog.Finish")); /* fs */
            leftClick();
            sleep(10000);

            if (offset == 0) {
                checkDRBDTest(drbdTest, 1);
            }
            offset += 40;
        }
        checkDRBDTest(drbdTest, 2);

        moveTo(480, 152); /* select r0 */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo("Protocol", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_UP); /* protocol b */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Fence peer", MComboBox.class);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        sleep(200);
        press(KeyEvent.VK_DOWN); /* select dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Wfc timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_9);
        sleep(2000);

        moveTo(Tools.getString("Browser.ApplyDRBDResource"));
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

        moveTo("Wfc timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_3);
        sleep(2000);

        moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        sleep(10000);
        checkDRBDTest(drbdTest, 2.11); /* 2.11 */
        moveTo("Wfc timeout", MTextField.class);
        sleep(6000);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);
        moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();

        /* resource */
        moveTo(480, 152); /* select r0 */
        leftClick();
        sleep(2000);
        leftClick();

        moveTo("Protocol", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN); /* protocol c */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Fence peer", MComboBox.class);
        leftClick();
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        sleep(200);
        press(KeyEvent.VK_UP); /* deselect dopd */
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(2000);

        moveTo("Wfc timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_5);
        sleep(2000);

        moveTo(Tools.getString("Browser.ApplyDRBDResource"));
        sleep(6000); /* test */
        leftClick(); /* apply/disables tooltip */
        sleep(2000); /* test */
        leftClick();
        checkDRBDTest(drbdTest, 2.2); /* 2.2 */

        moveTo("Wfc timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        sleep(1000);
        press(KeyEvent.VK_0);
        sleep(2000);

        moveTo(Tools.getString("Browser.ApplyDRBDResource"));
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
