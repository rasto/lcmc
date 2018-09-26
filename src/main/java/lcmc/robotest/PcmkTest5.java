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

import static lcmc.robotest.RoboTest.CONFIRM_REMOVE;
import lcmc.common.domain.util.Tools;
import lombok.RequiredArgsConstructor;

/**
 * This class is used to test the GUI.
 */
@RequiredArgsConstructor
final class PcmkTest5 {
    private final RoboTest roboTest;
    @SuppressWarnings("TooBroadScope")
    void start(final int count) {
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);

        roboTest.disableStonith();
        /* create 2 dummies */
        roboTest.checkTest("test5", 1);

        /* placeholders */
        final int ph1X = 380;
        final int ph1Y = 452;
        roboTest.moveTo(ph1X, ph1Y);
        roboTest.rightClick();
        roboTest.moveTo("Placeholder (AND)");
        roboTest.leftClick();

        final int dummy1X = 235;
        final int dummy1Y = 207;
        roboTest.chooseDummy(dummy1X, dummy1Y, false, true);
        final int dummy2X = 500;
        final int dummy2Y = 207;
        roboTest.chooseDummy(dummy2X, dummy2Y, false, true);
        roboTest.checkTest("test5", 2);

        roboTest.moveTo(dummy2X, dummy2Y);
        roboTest.addConstraint(2);
        roboTest.checkTest("test5", 2);
        roboTest.moveTo(ph1X, ph1Y);
        roboTest.addConstraint(1);

        roboTest.moveTo(ph1X, ph1Y);
        roboTest.leftClick();
        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick();
        roboTest.checkTest("test5", 2.1);

        final int dum1PopX = dummy1X + 80;
        final int dum1PopY = dummy1Y + 60;
        roboTest.removeConstraint(dum1PopX, dum1PopY);
        roboTest.checkTest("test5", 2.5);
        /* constraints */
        for (int i = 1; i <= count; i++) {
            roboTest.moveTo(dummy1X, dummy1Y);
            roboTest.addConstraint(2);

            roboTest.checkTest("test5", 3);

            roboTest.removeConstraint(dum1PopX, dum1PopY);
            roboTest.checkTest("test5", 2.5);

            roboTest.moveTo(ph1X, ph1Y);
            roboTest.addConstraint(1);

            roboTest.checkTest("test5", 3.5);

            roboTest.removeConstraint(dum1PopX, dum1PopY);
            roboTest.checkTest("test5", 2.5);
            roboTest.info("i: " + i);
        }
        roboTest.stopEverything();
        roboTest.checkTest("test5", 3.1);
        roboTest.removeResource(dummy1X, dummy1Y, CONFIRM_REMOVE);
        roboTest.removePlaceHolder(ph1X, ph1Y, CONFIRM_REMOVE);
        roboTest.removeResource(dummy2X, dummy2Y, CONFIRM_REMOVE);
        roboTest.checkTest("test5", 1);
    }
}
