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

import static lcmc.robotest.RoboTest.*;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTest8 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTest8.class);

    /** Private constructor, cannot be instantiated. */
    private PcmkTest8() {
        /* Cannot be instantiated. */
    }

    static void start(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 540;
        final int dummy1Y = 202;
        disableStonith();
        checkTest("test8", 1);
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("test8 i: " + i);
            }
            //checkTest("test7", 1);
            sleep(5000);
            chooseDummy(dummy1X, dummy1Y, false, true);
            sleep(5000);
            moveTo(550, 202);
            leftPress(); /* move the reosurce */
            moveTo(300, 202);
            leftRelease();
        }
        checkTest("test8-" + count, 2);
        stopEverything();
        checkTest("test8-" + count, 3);
        removeEverything();
        checkTest("test8", 4);
        resetTerminalAreas();
    }
}
