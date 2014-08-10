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

import static lcmc.robotest.RoboTest.CLONE_RADIO_Y;
import static lcmc.robotest.RoboTest.CONFIRM_REMOVE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is used to test the GUI.
 */
@Component
final class PcmkTestD {
    @Autowired
    private RoboTest roboTest;

    /** Pacemaker Leak tests. */
    void start(final int count) {
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);
        final int dummy1X = 540;
        final int dummy1Y = 202;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                roboTest.info("testD 1 I: " + i);
            }
            roboTest.chooseDummy(dummy1X, dummy1Y, false, false);
            roboTest.removeResource(dummy1X, dummy1Y, !CONFIRM_REMOVE);
        }
        roboTest.chooseDummy(dummy1X, dummy1Y, false, false);
        int pos = 0;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                roboTest.info("testD 2 I: " + i);
            }
            final double rand = Math.random();
            if (rand < 0.33) {
                if (pos == 1) {
                    continue;
                }
                pos = 1;
                roboTest.moveTo(796, CLONE_RADIO_Y);
                roboTest.leftClick();
            } else if (rand < 0.66) {
                if (pos == 2) {
                    continue;
                }
                pos = 2;
                roboTest.moveTo(894, CLONE_RADIO_Y);
                roboTest.leftClick();
            } else {
                if (pos == 3) {
                    continue;
                }
                pos = 3;
                roboTest.moveTo(994, CLONE_RADIO_Y);
                roboTest.leftClick();
            }
        }
        roboTest.removeResource(dummy1X, dummy1Y, !CONFIRM_REMOVE);
    }
}
