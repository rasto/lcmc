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
import static lcmc.robotest.RoboTest.CONFIRM_REMOVE;
import lcmc.utilities.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is used to test the GUI.
 */
@Component
final class PcmkTestG {
    @Autowired
    private RoboTest roboTest;

    void start(final int count) {
        roboTest.setSlowFactor(0.5f);
        roboTest.setAborted(false);
        roboTest.disableStonith();
        roboTest.checkTest("testG", 1);
        /* group with dummy resources */
        final int gx = 235;
        final int gy = 207;
        roboTest.moveTo(gx, gy);
        roboTest.sleep(1000);
        roboTest.rightClick(); /* popup */
        roboTest.sleep(1000);
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        roboTest.leftClick();
        roboTest.sleep(3000);
        /* create dummy */
        roboTest.moveTo(gx + 46, gy + 11);
        roboTest.rightClick(); /* group popup */
        roboTest.sleep(2000);

        for (int i = 0; i < count; i++) {
            /* another group resource */
            roboTest.moveTo(gx + 10, gy - 25);
            roboTest.rightClick(); /* popup */
            roboTest.sleep(10000);
            roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
            roboTest.sleep(1000);
            roboTest.moveTo("OCF Resource Agents");
            roboTest.sleep(1000);
            roboTest.typeDummy();
            roboTest.sleep(i * 300);
            roboTest.setTimeouts(true);
            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.sleep(6000);
            roboTest.leftClick();
            roboTest.sleep(1000);
        }
        roboTest.checkTest("testG", 2);
        roboTest.sleep(4000);
        roboTest.stopResource(gx, gy);
        roboTest.sleep(6000);
        roboTest.checkTest("testG", 3);

        /* copy/paste */
        roboTest.moveTo(gx + 10 , gy + 10);
        roboTest.leftClick();
        roboTest.getRobot().keyPress(KeyEvent.VK_CONTROL);
        roboTest.press(KeyEvent.VK_C);
        roboTest.press(KeyEvent.VK_V);
        roboTest.getRobot().keyRelease(KeyEvent.VK_CONTROL);
        roboTest.moveTo(gx + 10 , gy + 90);
        roboTest.leftClick();
        roboTest.moveTo(Tools.getString("Browser.ApplyGroup"));
        roboTest.sleep(4000);
        roboTest.leftClick();
        roboTest.checkTest("testG", 4);

        if (count < 10) {
            roboTest.removeResource(gx, gy, CONFIRM_REMOVE);
            roboTest.removeResource(gx, gy + 90, CONFIRM_REMOVE);
            roboTest.resetTerminalAreas();
        }
        System.gc();
    }
}
