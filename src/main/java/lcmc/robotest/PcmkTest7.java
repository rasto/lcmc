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
final class PcmkTest7 {
    @Inject
    private RoboTest roboTest;

    void start(final int count) {
        roboTest.setSlowFactor(0.5f);
        roboTest.setAborted(false);
        roboTest.disableStonith();
        final int dummy1X = 235;
        final int dummy1Y = 207;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                roboTest.info("test7 I: " + i);
            }
            roboTest.checkTest("test7", 1);
            /* create dummy */
            roboTest.chooseDummy(dummy1X, dummy1Y, false, true);
            roboTest.checkTest("test7", 2);
            roboTest.stopResource(dummy1X, dummy1Y);
            roboTest.checkTest("test7", 3);
            /* copy/paste */
            roboTest.moveTo(dummy1X + 10 , dummy1Y + 10);
            roboTest.leftClick();
            roboTest.getRobot().keyPress(KeyEvent.VK_CONTROL);
            roboTest.press(KeyEvent.VK_C);
            roboTest.press(KeyEvent.VK_V);
            roboTest.getRobot().keyRelease(KeyEvent.VK_CONTROL);
            roboTest.moveTo(dummy1X + 10 , dummy1Y + 90);
            roboTest.leftClick();
            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
            roboTest.checkTest("test7", 4);

            roboTest.removeResource(dummy1X, dummy1Y, CONFIRM_REMOVE);
            roboTest.removeResource(dummy1X, dummy1Y + 90, CONFIRM_REMOVE);
        }
        System.gc();
    }
}
