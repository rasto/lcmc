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
public class PcmkTest2 {
    private final RoboTest roboTest;

    void start() {
        roboTest.setSlowFactor(0.6f);
        roboTest.setAborted(false);

        roboTest.disableStonith();
        roboTest.checkTest("test2", 1);
        /* create 4 dummies */
        final int dummy1X = 235;
        final int dummy1Y = 207;
        roboTest.chooseDummy(dummy1X, dummy1Y, false, true);
        final int dummy2X = 545;
        final int dummy2Y = 207;
        roboTest.chooseDummy(dummy2X, dummy2Y, false, true);
        final int dummy3X = 235;
        final int dummy3Y = 342;
        roboTest.chooseDummy(dummy3X, dummy3Y, false, true);
        final int dummy4X = 545;
        final int dummy4Y = 342;
        roboTest.chooseDummy(dummy4X, dummy4Y, false, true);
        roboTest.checkTest("test2", 2);

        /* placeholder */
        final int phX = 445;
        final int phY = 342;
        roboTest.moveTo(phX, phY);
        roboTest.rightClick();
        roboTest.moveTo("Placeholder (AND)");
        roboTest.leftClick();
        roboTest.checkTest("test2", 3);
        /* constraints */
        roboTest.moveTo(phX, phY);
        roboTest.addConstraint(1); /* with dummy 1 */
        roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
        roboTest.leftClick();
        roboTest.checkTest("test2", 4);

        final int dum1PopX = dummy1X + 130;
        final int dum1PopY = dummy1Y + 50;
        for (int i = 0; i < 1; i++) {
            roboTest.removeOrder(dum1PopX, dum1PopY);

            roboTest.checkTest("test2", 5);

            roboTest.addOrder(dum1PopX, dum1PopY);
            roboTest.checkTest("test2", 6);

            roboTest.removeColocation(dum1PopX, dum1PopY);
            roboTest.checkTest("test2", 7);

            roboTest.addColocation(dum1PopX, dum1PopY);
            roboTest.checkTest("test2", 8);
        }

        roboTest.moveTo(dummy3X, dummy3Y);
        roboTest.addConstraint(5); /* with ph */
        roboTest.checkTest("test2", 9);

        final int dum3PopX = dummy3X + 165;
        final int dum3PopY = dummy3Y;
        for (int i = 0; i < 2; i++) {
            roboTest.removeColocation(dum3PopX, dum3PopY);

            roboTest.checkTest("test2", 9.1);

            roboTest.addColocation(dum3PopX, dum3PopY);
            roboTest.checkTest("test2", 9.2);

            roboTest.removeOrder(dum3PopX, dum3PopY);
            roboTest.checkTest("test2", 9.3);

            roboTest.addOrder(dum3PopX, dum3PopY);
            roboTest.checkTest("test2", 9.4);
        }

        roboTest.moveTo(phX, phY);
        roboTest.addConstraint(1); /* with dummy 2 */
        roboTest.checkTest("test2", 10);
        roboTest.moveTo(dummy4X, dummy4Y);
        roboTest.addConstraint(5); /* with ph */
        roboTest.checkTest("test2", 11);

        /* ph -> dummy2 */
        final int dum2PopX = dummy2X - 10;
        final int dum2PopY = dummy2Y + 70;
        roboTest.removeConstraint(dum2PopX, dum2PopY);
        roboTest.checkTest("test2", 11.1);
        roboTest.addConstraintOrderOnly(phX, phY, 2); /* with dummy 2 */
        roboTest.checkTest("test2", 11.2);
        roboTest.removeOrder(dum2PopX, dum2PopY);
        roboTest.checkTest("test2", 11.3);
        roboTest.addConstraintColocationOnly(phX, phY, 2); /* with dummy 2 */
        roboTest.checkTest("test2", 11.4);
        roboTest.addOrder(dum2PopX, dum2PopY);
        roboTest.checkTest("test2", 11.5);

        /* dummy4 -> ph */
        final int dum4PopX = dummy4X - 40;
        final int dum4PopY = dummy4Y;
        roboTest.removeConstraint(dum4PopX, dum4PopY);
        roboTest.checkTest("test2", 11.6);
        roboTest.moveTo(dummy4X + 20, dummy4Y + 5);
        roboTest.rightClick(); /* workaround for the next popup not working. */
        roboTest.rightClick(); /* workaround for the next popup not working. */
        roboTest.addConstraintColocationOnly(dummy4X, dummy4Y, 5); /* with ph */
        roboTest.checkTest("test2", 11.7);
        roboTest.removeColocation(dum4PopX, dum4PopY);
        roboTest.checkTest("test2", 11.8);
        roboTest.moveTo(dummy4X + 20, dummy4Y + 5);
        roboTest.rightClick(); /* workaround for the next popup not working. */
        roboTest.addConstraintOrderOnly(dummy4X, dummy4Y, 5); /* ph 2 */
        roboTest.checkTest("test2", 11.9);
        roboTest.addColocation(dum4PopX, dum4PopY);
        roboTest.checkTest("test2", 11.91);
        /* remove one dummy */
        roboTest.stopResource(dummy1X, dummy1Y);
        roboTest.checkTest("test2", 11.92);
        roboTest.removeResource(dummy1X, dummy1Y, CONFIRM_REMOVE);
        roboTest.checkTest("test2", 12);
        roboTest.stopResource(dummy2X, dummy2Y);
        roboTest.stopResource(dummy3X, dummy3Y);
        roboTest.stopResource(dummy4X, dummy4Y);
        roboTest.stopEverything();
        roboTest.checkTest("test2", 12.5);
        if (roboTest.maybe()) {
            /* remove placeholder */
            roboTest.moveTo(phX , phY);
            roboTest.rightClick();
            roboTest.moveTo(Tools.getString("ConstraintPHInfo.Remove"));
            roboTest.leftClick();
            roboTest.confirmRemove();

            /* remove rest of the dummies */
            roboTest.removeResource(dummy2X, dummy2Y, CONFIRM_REMOVE);
            roboTest.checkTest("test2", 14);
            roboTest.removeResource(dummy3X, dummy3Y, CONFIRM_REMOVE);
            roboTest.checkTest("test2", 15);
            roboTest.removeResource(dummy4X, dummy4Y, CONFIRM_REMOVE);
        } else {
            roboTest.removeEverything();
            /* remove placeholder */
            roboTest.moveTo(phX , phY);
            roboTest.rightClick();
            roboTest.moveTo(Tools.getString("ConstraintPHInfo.Remove"));
            roboTest.leftClick();
        }
        roboTest.checkTest("test2", 16);
    }
}
