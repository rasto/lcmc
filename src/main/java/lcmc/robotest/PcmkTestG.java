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
final class PcmkTestG {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTestG.class);

    /** Private constructor, cannot be instantiated. */
    private PcmkTestG() {
        /* Cannot be instantiated. */
    }

    static void start(final int count) {
        slowFactor = 0.5f;
        aborted = false;
        disableStonith();
        checkTest("testG", 1);
        /* group with dummy resources */
        final int gx = 235;
        final int gy = 207;
        moveTo(gx, gy);
        sleep(1000);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddGroup"));
        leftClick();
        sleep(3000);
        /* create dummy */
        moveTo(gx + 46, gy + 11);
        rightClick(); /* group popup */
        sleep(2000);

        for (int i = 0; i < count; i++) {
            /* another group resource */
            moveTo(gx + 10, gy - 25);
            rightClick(); /* popup */
            sleep(10000);
            moveTo(Tools.getString("ClusterBrowser.Hb.AddGroupService"));
            sleep(1000);
            moveTo("OCF Resource Agents");
            sleep(1000);
            typeDummy();
            sleep(i * 300);
            setTimeouts(true);
            moveTo(Tools.getString("Browser.ApplyResource"));
            sleep(6000);
            leftClick();
            sleep(1000);
        }
        checkTest("testG", 2);
        sleep(4000);
        stopResource(gx, gy);
        sleep(6000);
        checkTest("testG", 3);

        /* copy/paste */
        moveTo(gx + 10 , gy + 10);
        leftClick();
        robot.keyPress(KeyEvent.VK_CONTROL);
        press(KeyEvent.VK_C);
        press(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        moveTo(gx + 10 , gy + 90);
        leftClick();
        moveTo(Tools.getString("Browser.ApplyGroup"));
        sleep(4000);
        leftClick();
        checkTest("testG", 4);

        if (count < 10) {
            removeResource(gx, gy, CONFIRM_REMOVE);
            removeResource(gx, gy + 90, CONFIRM_REMOVE);
            resetTerminalAreas();
        }
        System.gc();
    }
}
