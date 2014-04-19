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
import lcmc.gui.widget.MComboBox;
import static lcmc.robotest.RoboTest.CONFIRM_REMOVE;
import static lcmc.robotest.RoboTest.aborted;
import static lcmc.robotest.RoboTest.checkTest;
import static lcmc.robotest.RoboTest.disableStonith;
import static lcmc.robotest.RoboTest.info;
import static lcmc.robotest.RoboTest.leftClick;
import static lcmc.robotest.RoboTest.moveTo;
import static lcmc.robotest.RoboTest.press;
import static lcmc.robotest.RoboTest.removeResource;
import static lcmc.robotest.RoboTest.rightClick;
import static lcmc.robotest.RoboTest.robot;
import static lcmc.robotest.RoboTest.slowFactor;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTestH {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTestH.class);

    /** Create ipmi resource. */
    private static void chooseIpmi(final int x, final int y, final boolean apply) {
        moveTo(x, y);
        rightClick(); /* popup */
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("Stonith Devices");
        press(KeyEvent.VK_I);
        press(KeyEvent.VK_P);
        press(KeyEvent.VK_M);
        press(KeyEvent.VK_I);
        press(KeyEvent.VK_ENTER);
        moveTo("Target Role", MComboBox.class);
        leftClick(); /* pull down */
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER);
        if (apply) {
            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
        }
    }

    static void start(final int count) {
        slowFactor = 0.5f;
        aborted = false;
        disableStonith();
        final int ipmiX = 235;
        final int ipmiY = 207;
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("testH I: " + i);
            }
            checkTest("testH", 1);
            /* create ipmi res */
            chooseIpmi(ipmiX, ipmiY, true);
            checkTest("testH", 3);
            /* copy/paste */
            moveTo(ipmiX + 10 , ipmiY + 10);
            leftClick();
            robot.keyPress(KeyEvent.VK_CONTROL);
            press(KeyEvent.VK_C);
            press(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            moveTo(ipmiX + 10 , ipmiY + 90);
            leftClick();
            moveTo(Tools.getString("Browser.ApplyResource"));
            leftClick();
            checkTest("testH", 4);

            removeResource(ipmiX, ipmiY, CONFIRM_REMOVE);
            removeResource(ipmiX, ipmiY + 90, CONFIRM_REMOVE);
        }
        System.gc();
    }

    /** Private constructor, cannot be instantiated. */
    private PcmkTestH() {
        /* Cannot be instantiated. */
    }
}
