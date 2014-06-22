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
import lcmc.gui.widget.MComboBox;
import static lcmc.robotest.RoboTest.CONFIRM_REMOVE;
import static lcmc.robotest.RoboTest.aborted;
import static lcmc.robotest.RoboTest.checkTest;
import static lcmc.robotest.RoboTest.disableStonith;
import static lcmc.robotest.RoboTest.info;
import static lcmc.robotest.RoboTest.leftClick;
import static lcmc.robotest.RoboTest.moveTo;
import static lcmc.robotest.RoboTest.moveToMenu;
import static lcmc.robotest.RoboTest.press;
import static lcmc.robotest.RoboTest.removeResource;
import static lcmc.robotest.RoboTest.resetTerminalAreas;
import static lcmc.robotest.RoboTest.rightClick;
import static lcmc.robotest.RoboTest.setTimeouts;
import static lcmc.robotest.RoboTest.slowFactor;
import static lcmc.robotest.RoboTest.stopResource;
import static lcmc.robotest.RoboTest.typeDummy;
import lcmc.utilities.Tools;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTestF {
    /** Cloned group. */
    static void start(final Cluster cluster, final int count) {
        slowFactor = 0.2f;
        aborted = false;
        disableStonith();
        final String testName = "testF";
        final String distro = cluster.getHostsArray()[0].getDistributionName();
        checkTest(testName, 1);
        /* group with dummy resources */
        final int gx = 235;
        final int gy = 207;
        moveTo(gx, gy);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        leftClick(); /* choose group */
        moveTo("Clone");
        leftClick(); /* clone */

        final int type = 1;
        //int type = 2;
        for (int i = count; i > 0; i--) {
            info("I: " + i);
            /* create dummy */
            moveToMenu("Group (1)");
            rightClick(); /* group popup */
            moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
            moveTo("OCF Resource Agents");
            typeDummy();
            setTimeouts(true);
            if (type == 1) {
                moveTo(Tools.getString("Browser.ApplyResource"));
                leftClick();
            }
        }
        if (type != 1) {
            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
        }
        checkTest(testName, 2);
        /* set resource stickiness */
        moveTo("Resource Stickiness", MComboBox.class);
        leftClick();
        press(KeyEvent.VK_BACK_SPACE);
        press(KeyEvent.VK_2);
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick();
        checkTest(testName, 3);

        stopResource(gx, gy);
        checkTest(testName, 4);
        removeResource(gx, gy, CONFIRM_REMOVE);
        resetTerminalAreas();
        System.gc();
    }

    /** Private constructor, cannot be instantiated. */
    private PcmkTestF() {
        /* Cannot be instantiated. */
    }
}
