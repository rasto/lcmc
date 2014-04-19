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

import static lcmc.robotest.RoboTest.HOST_Y;
import static lcmc.robotest.RoboTest.aborted;
import static lcmc.robotest.RoboTest.info;
import static lcmc.robotest.RoboTest.leftClick;
import static lcmc.robotest.RoboTest.moveTo;
import static lcmc.robotest.RoboTest.rightClick;
import static lcmc.robotest.RoboTest.sleep;
import static lcmc.robotest.RoboTest.slowFactor;
import lcmc.utilities.Tools;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTestE {
    /** Host wizard deadlock. */
    static void start(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                info("testE I: " + i);
            }
            moveTo(300 , HOST_Y); /* host */
            rightClick();
            moveTo(Tools.getString("HostBrowser.HostWizard"));
            leftClick();
            sleep(30000);
            moveTo(Tools.getString("Dialog.Dialog.Cancel"));
            leftClick();
        }
    }

    /** Private constructor, cannot be instantiated. */
    private PcmkTestE() {
        /* Cannot be instantiated. */
    }
}
