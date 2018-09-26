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
import lcmc.common.domain.util.Tools;
import lombok.RequiredArgsConstructor;

/**
 * This class is used to test the GUI.
 */
@RequiredArgsConstructor
final class PcmkTestA {
    private final RoboTest roboTest;

    void start(final int count) {
        roboTest.setSlowFactor(0.5f);
        roboTest.setAborted(false);
        roboTest.disableStonith();
        final int gx = 235;
        final int gy = 207;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                roboTest.info("testA I: " + i);
            }
            roboTest.checkTest("testA", 1);
            /* group with dummy resources */
            roboTest.moveTo(gx, gy);
            roboTest.rightClick(); /* popup */
            roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
            roboTest.leftClick();
            /* create dummy */
            roboTest.moveTo(gx + 46, gy + 11);
            roboTest.rightClick(); /* group popup */
            roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
            roboTest.moveTo("OCF Resource Agents");
            roboTest.typeDummy();
            roboTest.setTimeouts(true);
            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
            roboTest.checkTest("testA", 2);
            roboTest.stopResource(gx, gy);
            roboTest.checkTest("testA", 3);

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
            roboTest.leftClick();
            roboTest.checkTest("testA", 4);

            roboTest.removeResource(gx, gy, CONFIRM_REMOVE);
            roboTest.removeResource(gx, gy + 90, CONFIRM_REMOVE);
            roboTest.resetTerminalAreas();
        }
        System.gc();
    }
}
