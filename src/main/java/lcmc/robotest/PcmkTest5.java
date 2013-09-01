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
import lcmc.utilities.Tools;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTest5 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTest5.class);

    /** Private constructor, cannot be instantiated. */
    private PcmkTest5() {
        /* Cannot be instantiated. */
    }

    static void start(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        final int dummy1X = 235;
        final int dummy1Y = 207;

        final int dummy2X = 500;
        final int dummy2Y = 207;

        final int ph1X = 380;
        final int ph1Y = 452;


        disableStonith();
        /* create 2 dummies */
        checkTest("test5", 1);

        /* placeholders */
        moveTo(ph1X, ph1Y);
        rightClick();
        sleep(2000);
        moveTo("Placeholder (AND)");
        sleep(2000);
        leftClick();

        chooseDummy(dummy1X, dummy1Y, false, true);
        chooseDummy(dummy2X, dummy2Y, false, true);
        checkTest("test5", 2);

        moveTo(dummy2X, dummy2Y);
        addConstraint(2);
        sleep(20000);
        checkTest("test5", 2);
        moveTo(ph1X, ph1Y);
        addConstraint(1);

        moveTo(ph1X, ph1Y);
        sleep(2000);
        leftClick();
        sleep(2000);
        moveTo(Tools.getString("Browser.ApplyResource"));
        sleep(2000);
        leftClick();
        checkTest("test5", 2.1);

        final int dum1PopX = dummy1X + 80;
        final int dum1PopY = dummy1Y + 60;
        removeConstraint(dum1PopX, dum1PopY);
        checkTest("test5", 2.5);
        /* constraints */
        for (int i = 1; i <= count; i++) {
            moveTo(dummy1X, dummy1Y);
            addConstraint(2);

            checkTest("test5", 3);

            removeConstraint(dum1PopX, dum1PopY);
            checkTest("test5", 2.5);

            moveTo(ph1X, ph1Y);
            addConstraint(1);

            checkTest("test5", 3.5);

            removeConstraint(dum1PopX, dum1PopY);
            checkTest("test5", 2.5);
            info("i: " + i);
        }
        stopEverything();
        checkTest("test5", 3.1);
        removeResource(dummy1X, dummy1Y, CONFIRM_REMOVE);
        removeResource(dummy2X, dummy2Y, CONFIRM_REMOVE);
        removePlaceHolder(ph1X, ph1Y, !CONFIRM_REMOVE);
        sleep(5000);
        checkTest("test5", 1);
    }
}
