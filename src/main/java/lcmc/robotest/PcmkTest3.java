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
import java.awt.event.KeyEvent;
import lcmc.gui.widget.GenericWidget.MTextField;
import lcmc.gui.widget.MComboBox;
import lcmc.utilities.Tools;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTest3 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTest3.class);

    /** Private constructor, cannot be instantiated. */
    private PcmkTest3() {
        /* Cannot be instantiated. */
    }

    static void start(final int count) {
        slowFactor = 0.3f;
        aborted = false;
        disableStonith();
        final String testName = "test3";
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info(testName + " I: " + i);
            }
            checkTest(testName, 1);
            /* filesystem/drbd */
            moveTo(577, 205);
            rightClick(); /* popup */
            moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
            moveTo("Filesystem + Linbit:DRBD");
            leftClick(); /* choose fs */

            moveTo("block device", MComboBox.class); /* choose drbd */
            leftClick();
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_ENTER);

            moveTo("mount point", MComboBox.class);
            leftClick();
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_ENTER);

            moveTo("filesystem type", MComboBox.class);
            leftClick();
            press(KeyEvent.VK_E);
            press(KeyEvent.VK_E);
            press(KeyEvent.VK_ENTER);

            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
            checkTest(testName, 2);
            checkNumberOfVertices(testName, 4);
            stopEverything();
            checkTest(testName, 3);
            removeEverything();
            resetTerminalAreas();

            /* filesystem/drbd - with name */
            moveTo(577, 205);
            rightClick(); /* popup */
            moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
            moveTo("Filesystem + Linbit:DRBD");
            leftClick(); /* choose fs */

            checkTest(testName, 4);
            moveTo("Name", MTextField.class);
            leftClick();
            press(KeyEvent.VK_X);
            press(KeyEvent.VK_Y);

            moveTo("block device", MComboBox.class); /* choose drbd */
            leftClick();
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_ENTER);

            moveTo("mount point", MComboBox.class);
            leftClick();
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_DOWN);
            press(KeyEvent.VK_ENTER);

            moveTo("filesystem type", MComboBox.class);
            leftClick();
            press(KeyEvent.VK_E);
            press(KeyEvent.VK_E);
            press(KeyEvent.VK_ENTER);

            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
            checkTest(testName, 5);
            checkNumberOfVertices(testName, 4);
            stopEverything();
            checkTest(testName, 6);
            removeEverything();
            resetTerminalAreas();
        }
        System.gc();
    }
}
