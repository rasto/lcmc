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
        /* Two drbds. */
        slowFactor = 0.2f;
        aborted = false;
        if (!cluster.getHostsArray()[0].hasVolumes()) {
            final int protocolY = 352;
            final int correctionY = 30;
        }
        final String drbdTest = "drbd-test4";
        int offset = 0;
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
                press(KeyEvent.VK_ENTER);

                drbdNext();

                //addDrbdVolume();
            }
            addDrbdVolume();

            addBlockDevice();
            addBlockDevice();
            if (offset == 0) {
                checkDRBDTest(drbdTest, 1.1);
            } else {
                checkDRBDTest(drbdTest, 1.2);
            }
            sleep(10000);

            addMetaData();
            addFileSystem();
            moveTo(Tools.getString("Dialog.Dialog.Finish")); /* fs */
            leftClick();

            if (offset == 0) {
                checkDRBDTest(drbdTest, 1);
            }
            offset += 40;
        }
        checkDRBDTest(drbdTest, 2);

        moveTo(480, 152); /* select r0 */
        leftClick();
        leftClick();

        moveTo("Protocol", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_UP); /* protocol b */
        press(KeyEvent.VK_ENTER);

        moveTo("Fence peer", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_DOWN); /* select dopd */
        press(KeyEvent.VK_ENTER);

        moveTo("Wfc timeout", MTextField.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        press(KeyEvent.VK_9);

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
        Tools.sleep(10000);
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

        moveTo("Protocol", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_DOWN); /* protocol c */
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
