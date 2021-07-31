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

import static lcmc.robotest.RoboTest.CONFIRM_REMOVE;

import java.awt.event.KeyEvent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.widget.MComboBox;
import lcmc.common.domain.util.Tools;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class PcmkTestF {
    @Inject
    private RoboTest roboTest;

    /** Cloned group. */
    void start(final Cluster cluster, final int count) {
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);
        roboTest.disableStonith();
        final String testName = "testF";
        roboTest.checkTest(testName, 1);
        /* group with dummy resources */
        final int gx = 235;
        final int gy = 207;
        roboTest.moveTo(gx, gy);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        roboTest.leftClick(); /* choose group */
        roboTest.moveTo("Clone");
        roboTest.leftClick(); /* clone */

        for (int i = count; i > 0; i--) {
            roboTest.info("I: " + i);
            /* create dummy */
            Tools.sleep(1000);
            roboTest.moveToMenu("Group (1)");
            roboTest.rightClick(); /* group popup */
            roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
            roboTest.moveTo("OCF Resource Agents");
            roboTest.typeDummy();
            roboTest.setTimeouts(true);
            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
        }
        roboTest.checkTest(testName, 2);
        /* set resource stickiness */
        roboTest.moveTo("Resource Stickiness", MComboBox.class);
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_BACK_SPACE);
        roboTest.press(KeyEvent.VK_2);
        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick();
        roboTest.checkTest(testName, 3);

        roboTest.stopResource(gx, gy);
        roboTest.checkTest(testName, 4);
        roboTest.removeResource(gx, gy, CONFIRM_REMOVE);
        roboTest.resetTerminalAreas();
        System.gc();
    }
}
