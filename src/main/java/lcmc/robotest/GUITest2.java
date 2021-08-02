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

import java.awt.Color;

import javax.inject.Named;
import javax.inject.Singleton;

import lcmc.common.domain.util.Tools;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class GUITest2 {
    private final RoboTest roboTest;

    public GUITest2(RoboTest roboTest) {
        this.roboTest = roboTest;
    }

    /**
     * Cluster wizard locked until focus is lost.
     */
    void start(final int count) {
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                roboTest.info("gui-test2 " + i);
            }

            roboTest.moveTo(Tools.getString("ClusterTab.AddNewCluster"));
            roboTest.sleep(500);
            roboTest.leftClick();
            roboTest.sleep(2000);
            if (!roboTest.isColor(326, 512, new Color(184, 207, 229), true)) {
                roboTest.info("gui-test2: error");
                break;
            }
            roboTest.moveTo(Tools.getString("Dialog.Dialog.Cancel"));
            roboTest.sleep(500);
            roboTest.leftClick();
            roboTest.sleep(1000);
        }
    }
}
