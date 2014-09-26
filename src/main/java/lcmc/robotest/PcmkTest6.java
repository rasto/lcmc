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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class PcmkTest6 {
    @Inject
    private RoboTest roboTest;

    void start(final int count) {
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);

        final int ph1X = 315;
        final int ph1Y = 346;
        
        /* placeholders */
        roboTest.moveTo(ph1X, ph1Y);
        roboTest.rightClick();
        roboTest.moveTo("Placeholder (AND)");
        roboTest.leftClick();

        final int dummy1X = 235;
        final int dummy1Y = 207;
        roboTest.chooseDummy(dummy1X, dummy1Y, false, true);
        final int dum1PopX = dummy1X + 70;
        final int dum1PopY = dummy1Y + 60;
        for (int i = 0; i < count; i++) {
            if (i % 5 == 0) {
                roboTest.info("test6 i: " + i);
            }
            roboTest.moveTo(ph1X, ph1Y);
            roboTest.addConstraint(1);
            roboTest.removeConstraint(dum1PopX, dum1PopY);
        }
        roboTest.stopEverything();
        roboTest.removeEverything();
        roboTest.resetTerminalAreas();
    }
}
