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
import lcmc.cluster.ui.widget.MComboBox;
import static lcmc.robotest.RoboTest.CONFIRM_REMOVE;
import lcmc.common.domain.util.Tools;
import lombok.RequiredArgsConstructor;

/**
 * This class is used to test the GUI.
 */
@RequiredArgsConstructor
public class PcmkTestH {
    private final RoboTest roboTest;
    /** Create ipmi resource. */
    private void chooseIpmi(final int x, final int y, final boolean apply) {
        roboTest.moveTo(x, y);
        roboTest.rightClick(); /* popup */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        roboTest.moveTo("Filesystem + Linbit:DRBD");
        roboTest.moveTo("Stonith Devices");
        roboTest.press(KeyEvent.VK_I);
        roboTest.press(KeyEvent.VK_P);
        roboTest.press(KeyEvent.VK_M);
        roboTest.press(KeyEvent.VK_I);
        roboTest.press(KeyEvent.VK_ENTER);
        roboTest.moveTo("Target Role", MComboBox.class);
        roboTest.leftClick(); /* pull down */
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_ENTER);
        if (apply) {
            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
        }
    }

    void start(final int count) {
        roboTest.setSlowFactor(0.5f);
        roboTest.setAborted(false);
        roboTest.disableStonith();
        final int ipmiX = 235;
        final int ipmiY = 207;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                roboTest.info("testH I: " + i);
            }
            roboTest.checkTest("testH", 1);
            /* create ipmi res */
            chooseIpmi(ipmiX, ipmiY, true);
            roboTest.checkTest("testH", 3);
            /* copy/paste */
            roboTest.moveTo(ipmiX + 10 , ipmiY + 10);
            roboTest.leftClick();
            roboTest.getRobot().keyPress(KeyEvent.VK_CONTROL);
            roboTest.press(KeyEvent.VK_C);
            roboTest.press(KeyEvent.VK_V);
            roboTest.getRobot().keyRelease(KeyEvent.VK_CONTROL);
            roboTest.moveTo(ipmiX + 10 , ipmiY + 90);
            roboTest.leftClick();
            roboTest.moveTo(Tools.getString("Browser.ApplyResource"));
            roboTest.leftClick();
            roboTest.checkTest("testH", 4);

            roboTest.removeResource(ipmiX, ipmiY, CONFIRM_REMOVE);
            roboTest.removeResource(ipmiX, ipmiY + 90, CONFIRM_REMOVE);
        }
        System.gc();
    }
}
