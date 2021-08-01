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

import javax.inject.Named;
import javax.inject.Singleton;

import lcmc.common.domain.util.Tools;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class PcmkTestE {
    private final RoboTest roboTest;

    public PcmkTestE(RoboTest roboTest) {
        this.roboTest = roboTest;
    }

    /**
     * Host wizard deadlock.
     */
    void start(final int count) {
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                roboTest.info("testE I: " + i);
            }
            roboTest.moveTo(300, HOST_Y); /* host */
            roboTest.rightClick();
            roboTest.moveTo(Tools.getString("HostBrowser.HostWizard"));
            roboTest.leftClick();
            roboTest.sleep(30000);
            roboTest.moveTo(Tools.getString("Dialog.Dialog.Cancel"));
            roboTest.leftClick();
        }
    }
}
