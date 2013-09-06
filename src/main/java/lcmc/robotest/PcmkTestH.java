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
import lcmc.utilities.Tools;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.gui.widget.Widget;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class PcmkTestH {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PcmkTestH.class);

    /** Private constructor, cannot be instantiated. */
    private PcmkTestH() {
        /* Cannot be instantiated. */
    }

    /** Create ipmi resource. */
    private static void chooseIpmi(final int x,
                                   final int y,
                                   final boolean apply) {
        moveTo(x, y);
        rightClick(); /* popup */
        sleep(1000);
        moveTo(Tools.getString("ClusterBrowser.Hb.AddService"));
        sleep(1000);
        moveTo("Filesystem + Linbit:DRBD");
        moveTo("Stonith Devices");
        sleep(2000);
        press(KeyEvent.VK_I);
        sleep(200);
        press(KeyEvent.VK_P);
        sleep(200);
        press(KeyEvent.VK_M);
        sleep(200);
        press(KeyEvent.VK_I);
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(200);
        moveTo("Target Role", Widget.MComboBox.class);
        sleep(2000);
        leftClick(); /* pull down */
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_ENTER);
        sleep(500);
        if (apply) {
            sleep(2000);
            moveTo(Tools.getString("Browser.ApplyResource"));
            sleep(4000);
            leftClick();
            sleep(2000);
        }
    }

    static void start(final int count) {
        slowFactor = 0.5f;
        aborted = false;
        final int ipmiX = 235;
        final int ipmiY = 207;
        disableStonith();
        for (int i = count; i > 0; i--) {
            if (i % 5 == 0) {
                info("testH I: " + i);
            }
            checkTest("testH", 1);
            /* create ipmi res */
            sleep(5000);
            chooseIpmi(ipmiX, ipmiY, true);
            checkTest("testH", 3);
            sleep(5000);
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
            sleep(4000);
            leftClick();
            checkTest("testH", 4);

            removeResource(ipmiX, ipmiY, CONFIRM_REMOVE);
            removeResource(ipmiX, ipmiY + 90, CONFIRM_REMOVE);
        }
        System.gc();
    }
}
