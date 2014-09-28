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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class PcmkTestC {
    @Inject
    private RoboTest roboTest;

    void start(final int count) {
        roboTest.setSlowFactor(0.5f);
        roboTest.disableStonith();
        final String testName = "testC";
        final int statefulX = 500;
        final int statefulY = 207;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                roboTest.info(testName + " I: " + i);
            }
            roboTest.checkTest(testName, 1);
            /** Add m/s Stateful resource */
            roboTest.moveTo(statefulX, statefulY);
            roboTest.rightClick(); /* popup */
            roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
            roboTest.moveTo("Filesystem + Linbit:DRBD");
            roboTest.moveTo("OCF Resource Agents");

            roboTest.press(KeyEvent.VK_S);
            roboTest.press(KeyEvent.VK_T);
            roboTest.press(KeyEvent.VK_A);
            roboTest.press(KeyEvent.VK_T);
            roboTest.press(KeyEvent.VK_E);
            roboTest.press(KeyEvent.VK_F);
            roboTest.press(KeyEvent.VK_ENTER); /* choose Stateful */

            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
            roboTest.stopResource(statefulX, statefulY);
            roboTest.checkTest(testName, 2);
            /* copy/paste */
            roboTest.moveTo(statefulX, statefulY);
            roboTest.leftClick();
            roboTest.getRobot().keyPress(KeyEvent.VK_CONTROL);
            roboTest.press(KeyEvent.VK_C);
            roboTest.press(KeyEvent.VK_V);
            roboTest.getRobot().keyRelease(KeyEvent.VK_CONTROL);
            roboTest.moveTo(245, statefulY + 90);
            roboTest.leftClick();
            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
            roboTest.checkTest(testName, 4);

            roboTest.removeResource(statefulX, statefulY, CONFIRM_REMOVE);
            roboTest.removeResource(245, statefulY + 90, CONFIRM_REMOVE);
            roboTest.resetTerminalAreas();
        }
    }
}
