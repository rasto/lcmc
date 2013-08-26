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
import lcmc.gui.widget.Widget;
import static lcmc.robotest.RoboTest.*;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTestF {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTestF.class);

    /** Private constructor, cannot be instantiated. */
    private PcmkTestF() {
        /* Cannot be instantiated. */
    }

    /** Cloned group. */
    static void start(final Cluster cluster, final int count) {
        slowFactor = 0.2f;
        aborted = false;
        final int gx = 235;
        final int gy = 207;
        disableStonith();
        final String testName = "testF";
        final String distro = cluster.getHostsArray()[0].getDist();
        checkTest(testName, 1);
        /* group with dummy resources */
        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo("Add Group");
        leftClick(); /* choose group */
        sleep(1000);
        moveTo("Clone");
        leftClick(); /* clone */

        final int type = 1;
        //int type = 2;
        for (int i = count; i > 0; i--) {
            info("I: " + i);
            /* create dummy */
            moveToMenu("Group (1)");
            rightClick(); /* group popup */
            sleep(2000);
            moveTo("Add Group Service");
            sleep(2000);
            moveTo("OCF Resource Agents");
            sleep(1000);
            typeDummy();
            sleep(300);
            setTimeouts(true);
            if (type == 1) {
                moveTo("Apply");
                sleep(6000);
                leftClick();
                sleep(6000);
            }
        }
        if (type != 1) {
            moveTo("Apply");
            sleep(6000);
            leftClick();
            sleep(6000);
        }
        checkTest(testName, 2);
        /* set resource stickiness */
        moveTo("Resource Stickiness", Widget.MComboBox.class);
        leftClick();
        sleep(1000);
        press(KeyEvent.VK_BACK_SPACE);
        sleep(200);
        press(KeyEvent.VK_2);
        sleep(1000);
        moveTo("Apply");
        sleep(6000);
        leftClick();
        sleep(6000);
        checkTest(testName, 3);

        stopResource(gx, gy);
        sleep(6000);
        checkTest(testName, 4);
        removeResource(gx, gy, CONFIRM_REMOVE);
        resetTerminalAreas();
        System.gc();
    }
}
