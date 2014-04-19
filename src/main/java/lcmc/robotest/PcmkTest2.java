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
final class PcmkTest2 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTest2.class);

    static void start() {
        slowFactor = 0.6f;
        aborted = false;

        disableStonith();
        checkTest("test2", 1);
        /* create 4 dummies */
        final int dummy1X = 235;
        final int dummy1Y = 207;
        chooseDummy(dummy1X, dummy1Y, false, true);
        final int dummy2X = 545;
        final int dummy2Y = 207;
        chooseDummy(dummy2X, dummy2Y, false, true);
        final int dummy3X = 235;
        final int dummy3Y = 342;
        chooseDummy(dummy3X, dummy3Y, false, true);
        final int dummy4X = 545;
        final int dummy4Y = 342;
        chooseDummy(dummy4X, dummy4Y, false, true);
        checkTest("test2", 2);

        /* placeholder */
        final int phX = 445;
        final int phY = 342;
        moveTo(phX, phY);
        rightClick();
        moveTo("Placeholder (AND)");
        leftClick();
        checkTest("test2", 3);
        /* constraints */
        moveTo(phX, phY);
        addConstraint(1); /* with dummy 1 */
        moveTo(Tools.getString("Browser.ApplyResource"));
        leftClick();
        checkTest("test2", 4);

        final int dum1PopX = dummy1X + 130;
        final int dum1PopY = dummy1Y + 50;
        for (int i = 0; i < 1; i++) {
            removeOrder(dum1PopX, dum1PopY);

            checkTest("test2", 5);

            addOrder(dum1PopX, dum1PopY);
            checkTest("test2", 6);

            removeColocation(dum1PopX, dum1PopY);
            checkTest("test2", 7);

            addColocation(dum1PopX, dum1PopY);
            checkTest("test2", 8);
        }

        moveTo(dummy3X, dummy3Y);
        addConstraint(5); /* with ph */
        checkTest("test2", 9);

        final int dum3PopX = dummy3X + 165;
        final int dum3PopY = dummy3Y;
        for (int i = 0; i < 2; i++) {
            removeColocation(dum3PopX, dum3PopY);

            checkTest("test2", 9.1);

            addColocation(dum3PopX, dum3PopY);
            checkTest("test2", 9.2);

            removeOrder(dum3PopX, dum3PopY);
            checkTest("test2", 9.3);

            addOrder(dum3PopX, dum3PopY);
            checkTest("test2", 9.4);
        }

        moveTo(phX, phY);
        addConstraint(1); /* with dummy 2 */
        checkTest("test2", 10);
        moveTo(dummy4X, dummy4Y);
        addConstraint(5); /* with ph */
        checkTest("test2", 11);

        /* ph -> dummy2 */
        final int dum2PopX = dummy2X - 10;
        final int dum2PopY = dummy2Y + 70;
        removeConstraint(dum2PopX, dum2PopY);
        checkTest("test2", 11.1);
        addConstraintOrderOnly(phX, phY, 2); /* with dummy 2 */
        checkTest("test2", 11.2);
        removeOrder(dum2PopX, dum2PopY);
        checkTest("test2", 11.3);
        addConstraintColocationOnly(phX, phY, 2); /* with dummy 2 */
        checkTest("test2", 11.4);
        addOrder(dum2PopX, dum2PopY);
        checkTest("test2", 11.5);

        /* dummy4 -> ph */
        final int dum4PopX = dummy4X - 40;
        final int dum4PopY = dummy4Y;
        removeConstraint(dum4PopX, dum4PopY);
        checkTest("test2", 11.6);
        moveTo(dummy4X + 20, dummy4Y + 5);
        rightClick(); /* workaround for the next popup not working. */
        rightClick(); /* workaround for the next popup not working. */
        addConstraintColocationOnly(dummy4X, dummy4Y, 5); /* with ph */
        checkTest("test2", 11.7);
        removeColocation(dum4PopX, dum4PopY);
        checkTest("test2", 11.8);
        moveTo(dummy4X + 20, dummy4Y + 5);
        rightClick(); /* workaround for the next popup not working. */
        addConstraintOrderOnly(dummy4X, dummy4Y, 5); /* ph 2 */
        checkTest("test2", 11.9);
        addColocation(dum4PopX, dum4PopY);
        checkTest("test2", 11.91);
        /* remove one dummy */
        stopResource(dummy1X, dummy1Y);
        checkTest("test2", 11.92);
        removeResource(dummy1X, dummy1Y, CONFIRM_REMOVE);
        checkTest("test2", 12);
        stopResource(dummy2X, dummy2Y);
        stopResource(dummy3X, dummy3Y);
        stopResource(dummy4X, dummy4Y);
        stopEverything();
        checkTest("test2", 12.5);
        if (maybe()) {
            /* remove placeholder */
            moveTo(phX , phY);
            rightClick();
            moveTo(Tools.getString("ConstraintPHInfo.Remove"));
            leftClick();
            confirmRemove();

            /* remove rest of the dummies */
            removeResource(dummy2X, dummy2Y, CONFIRM_REMOVE);
            checkTest("test2", 14);
            removeResource(dummy3X, dummy3Y, CONFIRM_REMOVE);
            checkTest("test2", 15);
            removeResource(dummy4X, dummy4Y, CONFIRM_REMOVE);
        } else {
            removeEverything();
            /* remove placeholder */
            moveTo(phX , phY);
            rightClick();
            moveTo(Tools.getString("ConstraintPHInfo.Remove"));
            leftClick();
            confirmRemove();
        }
        checkTest("test2", 16);
    }

    /** Private constructor, cannot be instantiated. */
    private PcmkTest2() {
        /* Cannot be instantiated. */
    }
}
