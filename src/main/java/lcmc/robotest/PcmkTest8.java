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

import lombok.RequiredArgsConstructor;

/**
 * This class is used to test the GUI.
 */
@RequiredArgsConstructor
public class PcmkTest8 {
    private final RoboTest roboTest;

    void start(final int count) {
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);
        roboTest.disableStonith();
        roboTest.checkTest("test8", 1);
        final int dummy1X = 540;
        final int dummy1Y = 202;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                roboTest.info("test8 i: " + i);
            }
            //checkTest("test7", 1);
            roboTest.chooseDummy(dummy1X, dummy1Y, false, true);
            roboTest.moveTo(550, 202);
            roboTest.leftPress(); /* move the reosurce */
            roboTest.moveTo(300, 202);
            roboTest.leftRelease();
        }
        roboTest.checkTest("test8-" + count, 2);
        roboTest.stopEverything();
        roboTest.checkTest("test8-" + count, 3);
        roboTest.removeEverything();
        roboTest.checkTest("test8", 4);
        roboTest.resetTerminalAreas();
    }
}
