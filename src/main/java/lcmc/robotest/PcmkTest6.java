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
final class PcmkTest6 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTest6.class);

    /** Private constructor, cannot be instantiated. */
    private PcmkTest6() {
        /* Cannot be instantiated. */
    }

    static void start(final int count) {
        slowFactor = 0.2f;
        aborted = false;

        final int ph1X = 315;
        final int ph1Y = 346;


        //disableStonith();
        /* create 2 dummies */
        //checkTest("test5", 1);

        /* placeholders */
        moveTo(ph1X, ph1Y);
        rightClick();
        moveTo("Placeholder (AND)");
        leftClick();

        final int dummy1X = 235;
        final int dummy1Y = 207;
        chooseDummy(dummy1X, dummy1Y, false, true);
        final int dum1PopX = dummy1X + 70;
        final int dum1PopY = dummy1Y + 60;
        for (int i = 0; i < count; i++) {
            if (i % 5 == 0) {
                info("test6 i: " + i);
            }
            moveTo(ph1X, ph1Y);
            addConstraint(1);
            removeConstraint(dum1PopX, dum1PopY);
        }
        stopEverything();
        removeEverything();
        resetTerminalAreas();
    }
}
