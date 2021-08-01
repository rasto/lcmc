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

import javax.inject.Named;
import javax.inject.Singleton;

import lcmc.common.domain.util.Tools;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class PcmkTest4 {
    private final RoboTest roboTest;

    public PcmkTest4(RoboTest roboTest) {
        this.roboTest = roboTest;
    }

    void start() {
        roboTest.setSlowFactor(0.6f);
        roboTest.setAborted(false);

        roboTest.disableStonith();
        roboTest.checkTest("test4", 1);
        /* create 6 dummies */
        final int dummy1X = 235;
        final int dummy1Y = 207;
        roboTest.chooseDummy(dummy1X, dummy1Y, false, true);
        final int dummy2X = 545;
        final int dummy2Y = 207;
        roboTest.chooseDummy(dummy2X, dummy2Y, false, true);
        final int dummy3X = 235;
        final int dummy3Y = 346;
        roboTest.chooseDummy(dummy3X, dummy3Y, false, true);
        final int dummy4X = 545;
        final int dummy4Y = 346;
        roboTest.chooseDummy(dummy4X, dummy4Y, false, true);
        final int dummy5X = 235;
        final int dummy5Y = 505;
        roboTest.chooseDummy(dummy5X, dummy5Y, false, true);
        final int dummy6X = 545;
        final int dummy6Y = 505;
        roboTest.chooseDummy(dummy6X, dummy6Y, false, true);
        roboTest.checkTest("test4", 2);

        /* 2 placeholders */
        final int count = 1;
        final int ph1X = 445;
        final int ph1Y = 266;
        final int ph2X = 445;
        final int ph2Y = 425;
        for (int i = 0; i < count; i++) {
            roboTest.moveTo(ph1X, ph1Y);
            roboTest.rightClick();
            roboTest.moveTo("Placeholder (AND)");
            roboTest.leftClick();

            roboTest.moveTo(ph2X, ph2Y);
            roboTest.rightClick();
            roboTest.moveTo("Placeholder (AND)");
            roboTest.leftClick();
            roboTest.checkTest("test4", 2);

            /* constraints */
            /* menu dummy 5 with ph2 */
            roboTest.moveToMenu("Dummy (5)");
            roboTest.addConstraint(7);
            /* menu dummy 6 with ph2 */
            roboTest.moveToMenu("Dummy (6)");
            roboTest.addConstraint(7);

            /* with dummy 3 */
            roboTest.moveTo(ph2X, ph2Y);
            roboTest.addConstraint(3);
            /* with dummy 4 */
            roboTest.moveTo(ph2X, ph2Y);
            roboTest.addConstraint(3);

            /* with ph1 */
            roboTest.moveTo(dummy3X, dummy3Y);
            roboTest.addConstraint(4);
            /* with ph1 */
            roboTest.moveTo(dummy4X, dummy4Y);
            roboTest.addConstraint(4);

            roboTest.moveTo(ph1X, ph1Y);
            roboTest.addConstraint(1); /* with dummy 1 */
            roboTest.moveTo(ph1X, ph1Y);
            roboTest.addConstraint(1); /* with dummy 2 */
            roboTest.checkTest("test4", 2);

        }
        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick();

        roboTest.checkTest("test4", 3);
        roboTest.stopEverything();
        roboTest.checkTest("test4", 4);
        roboTest.removeEverything();
        roboTest.removePlaceHolder(ph1X, ph1Y, !CONFIRM_REMOVE);
        roboTest.removePlaceHolder(ph2X, ph2Y, !CONFIRM_REMOVE);
    }
}
