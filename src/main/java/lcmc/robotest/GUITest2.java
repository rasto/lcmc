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
import static lcmc.robotest.RoboTest.*;
import lcmc.utilities.Tools;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class GUITest2 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(GUITest2.class);

    /** Cluster wizard locked until focus is lost. */
    static void start(final int count) {
        slowFactor = 0.2f;
        aborted = false;
        for (int i = count; i > 0; i--) {
            if (i % 10 == 0) {
                info("gui-test2 " + i);
            }

            moveTo(Tools.getString("ClusterTab.AddNewCluster"));
            sleep(500);
            leftClick();
            sleep(2000);
            if (!isColor(326, 512, new Color(184, 207, 229), true)) {
                info("gui-test2: error");
                break;
            }
            moveTo(Tools.getString("Dialog.Dialog.Cancel"));
            sleep(500);
            leftClick();
            sleep(1000);
        }
    }

    /** Private constructor, cannot be instantiated. */
    private GUITest2() {
        /* Cannot be instantiated. */
    }
}
