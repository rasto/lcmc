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

import java.awt.event.KeyEvent;
import lcmc.cluster.ui.widget.GenericWidget.MTextField;
import lcmc.cluster.ui.widget.MComboBox;
import lcmc.common.domain.util.Tools;
import lombok.RequiredArgsConstructor;

/**
 * This class is used to test the GUI.
 */
@RequiredArgsConstructor
public class PcmkTest3 {
    private final RoboTest roboTest;

    void start(final int count) {
        roboTest.setSlowFactor(0.3f);
        roboTest.setAborted(false);
        roboTest.disableStonith();
        final String testName = "test3";
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                roboTest.info(testName + " I: " + i);
            }
            roboTest.checkTest(testName, 1);
            /* filesystem/drbd */
            roboTest.moveTo(577, 205);
            roboTest.rightClick(); /* popup */
            roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
            roboTest.moveTo("Filesystem + Linbit:DRBD");
            roboTest.leftClick(); /* choose fs */

            roboTest.moveTo("block device", MComboBox.class); /* choose drbd */
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_DOWN);
            roboTest.press(KeyEvent.VK_DOWN);
            roboTest.press(KeyEvent.VK_ENTER);

            roboTest.moveTo("mount point", MComboBox.class);
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_DOWN);
            roboTest.press(KeyEvent.VK_DOWN);
            roboTest.press(KeyEvent.VK_ENTER);

            roboTest.moveTo("filesystem type", MComboBox.class);
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_E);
            roboTest.press(KeyEvent.VK_E);
            roboTest.press(KeyEvent.VK_ENTER);

            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
            roboTest.checkTest(testName, 2);
            roboTest.checkNumberOfVertices(testName, 4);
            roboTest.stopEverything();
            roboTest.checkTest(testName, 3);
            roboTest.removeEverything();
            roboTest.resetTerminalAreas();

            /* filesystem/drbd - with name */
            roboTest.moveTo(577, 205);
            roboTest.rightClick(); /* popup */
            roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
            roboTest.moveTo("Filesystem + Linbit:DRBD");
            roboTest.leftClick(); /* choose fs */

            roboTest.checkTest(testName, 4);
            roboTest.moveTo("Name", MTextField.class);
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_X);
            roboTest.press(KeyEvent.VK_Y);

            roboTest.moveTo("block device", MComboBox.class); /* choose drbd */
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_DOWN);
            roboTest.press(KeyEvent.VK_DOWN);
            roboTest.press(KeyEvent.VK_ENTER);

            roboTest.moveTo("mount point", MComboBox.class);
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_DOWN);
            roboTest.press(KeyEvent.VK_DOWN);
            roboTest.press(KeyEvent.VK_ENTER);

            roboTest.moveTo("filesystem type", MComboBox.class);
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_E);
            roboTest.press(KeyEvent.VK_E);
            roboTest.press(KeyEvent.VK_ENTER);

            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
            roboTest.checkTest(testName, 5);
            roboTest.checkNumberOfVertices(testName, 4);
            roboTest.stopEverything();
            roboTest.checkTest(testName, 6);
            roboTest.removeEverything();
            roboTest.resetTerminalAreas();
        }
        System.gc();
    }
}
