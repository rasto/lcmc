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
import static lcmc.robotest.RoboTest.*;
import lcmc.utilities.Tools;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTestC {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTestC.class);

    /** Private constructor, cannot be instantiated. */
    private PcmkTestC() {
        /* Cannot be instantiated. */
    }

    static void start(final int count) {
        slowFactor = 0.5f;
        final int statefulX = 500;
        final int statefulY = 207;
        disableStonith();
        final String testName = "testC";
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info(testName + " I: " + i);
            }
            checkTest(testName, 1);
            /** Add m/s Stateful resource */
            moveTo(statefulX, statefulY);
            rightClick(); /* popup */
            moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
            moveTo("Filesystem + Linbit:DRBD");
            moveTo("OCF Resource Agents");

            press(KeyEvent.VK_S);
            press(KeyEvent.VK_T);
            press(KeyEvent.VK_A);
            press(KeyEvent.VK_T);
            press(KeyEvent.VK_E);
            press(KeyEvent.VK_F);
            press(KeyEvent.VK_ENTER); /* choose Stateful */

            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
            stopResource(statefulX, statefulY);
            checkTest(testName, 2);
            /* copy/paste */
            moveTo(statefulX, statefulY);
            leftClick();
            robot.keyPress(KeyEvent.VK_CONTROL);
            press(KeyEvent.VK_C);
            press(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            moveTo(245, statefulY + 90);
            leftClick();
            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
            checkTest(testName, 4);


            removeResource(statefulX, statefulY, CONFIRM_REMOVE);
            removeResource(245, statefulY + 90, CONFIRM_REMOVE);
            resetTerminalAreas();
        }
    }
}
