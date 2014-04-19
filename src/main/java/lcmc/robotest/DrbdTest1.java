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
import lcmc.data.Cluster;
import lcmc.gui.widget.MComboBox;
import static lcmc.robotest.RoboTest.PROXY;
import static lcmc.robotest.RoboTest.aborted;
import static lcmc.robotest.RoboTest.checkDRBDTest;
import static lcmc.robotest.RoboTest.confirmRemove;
import static lcmc.robotest.RoboTest.dialogColorTest;
import static lcmc.robotest.RoboTest.info;
import static lcmc.robotest.RoboTest.leftClick;
import static lcmc.robotest.RoboTest.moveTo;
import static lcmc.robotest.RoboTest.press;
import static lcmc.robotest.RoboTest.rightClick;
import static lcmc.robotest.RoboTest.sleep;
import static lcmc.robotest.RoboTest.slowFactor;
import lcmc.utilities.Tools;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class DrbdTest1 {
    static void start(final Cluster cluster, final int blockDevY) {
        slowFactor = 0.2f;
        aborted = false;

        addDrbdResource(cluster, blockDevY);
        chooseDrbdResource(cluster);
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        addMetaData();
        addFileSystem();
        moveTo(Tools.getString("Dialog.Dialog.Finish"));
        leftClick();
        final String drbdTest = "drbd-test1";
        checkDRBDTest(drbdTest, 1.1);
        for (int i = 0; i < 2; i++) {
            info("i: " + i);
            removeDrbdVolume(false);
        }
        removeDrbdVolume(true);
        checkDRBDTest(drbdTest, 2);
    }

    static void addDrbdResource(final Cluster cluster, final int blockDevY) {
        moveTo(334, blockDevY); /* add drbd resource */
        rightClick();
        moveTo(Tools.getString("HostBrowser.Drbd.AddDrbdResource"));
        moveTo(cluster.getHostsArray()[1].getName());
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        press(KeyEvent.VK_ENTER);
        dialogColorTest("addDrbdResource");
        moveTo(500, 300);
        rightClick();
    }

    static void drbdNext() {
        press(KeyEvent.VK_ENTER);
    }

    static void newDrbdResource() {
        drbdNext();
        dialogColorTest("newDrbdResource");
        moveTo(500, 300);
        rightClick();
    }

    static void chooseDrbdResourceInterface(final String hostName, final boolean proxy) {
        moveTo("on " + hostName, MComboBox.class); /* interface */
        leftClick();
        if (proxy) {
            press(KeyEvent.VK_P); /* select first interface to proxy*/
            press(KeyEvent.VK_DOWN);
        } else {
            press(KeyEvent.VK_E); /* select first interface */
        }
        press(KeyEvent.VK_ENTER);
        dialogColorTest("chooseDrbdResourceInterface");
        moveTo(500, 300);
        rightClick();
    }

    static void chooseDrbdResource(final Cluster cluster) {
        chooseDrbdResourceInterface(cluster.getHostsArray()[0].getName(),
                                    !PROXY);
        chooseDrbdResourceInterface(cluster.getHostsArray()[1].getName(),
                                    !PROXY);

        drbdNext();
        dialogColorTest("chooseDrbdResource");
        moveTo(500, 300);
        rightClick();
    }

    static void addDrbdVolume() {
        drbdNext();
        dialogColorTest("addDrbdVolume");
        moveTo(500, 300);
        rightClick();
    }

    static void addBlockDevice() {
        drbdNext();
        dialogColorTest("addBlockDevice");
        moveTo(500, 300);
        rightClick();
    }

    static void addMetaData() {
        drbdNext();
        dialogColorTest("addMetaData");
        moveTo(500, 300);
        rightClick();
    }

    static void addFileSystem() {
        /* do nothing. */
        dialogColorTest("addFileSystem");
        moveTo(500, 300);
        rightClick();
    }

    static void removeDrbdVolume(final boolean really) {
        if (aborted) {
            return;
        }
        moveTo(480, 152); /* rsc popup */
        rightClick(); /* remove */
        moveTo(Tools.getString("ClusterBrowser.Drbd.RemoveEdge"));
        leftClick();
        dialogColorTest("removeDrbdVolume");
        if (really) {
            confirmRemove();
        } else {
            press(KeyEvent.VK_ENTER); /* cancel */
        }
    }

    static void createPV(final int blockDevX, final int blockDevY) {
        moveTo(blockDevX, blockDevY);
        rightClick();
        moveTo("Create PV");
        leftClick();
    }

    static void pvRemove(final int blockDevX, final int blockDevY) {
        moveTo(blockDevX, blockDevY);
        rightClick();
        moveTo("Remove PV");
        leftClick();
    }

    static void createVG(final Cluster cluster, final int blockDevY) {
        moveTo(334, blockDevY);
        rightClick();
        moveTo("Create VG");
        leftClick();
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();
        moveTo("Create VG"); /* button */
        leftClick();
    }

    static void createVGMulti(final int blockDevY) {
        moveTo(334, blockDevY);
        rightClick();
        moveTo("Create VG");
        leftClick();
        moveTo("Create VG"); /* button */
        leftClick();
    }

    static void createLV(final Cluster cluster) {
        rightClick();
        moveTo("Create LV in VG");
        leftClick();
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();
        moveTo("Create"); /* button */
        leftClick();
        moveTo("Close");
        leftClick();
    }

    static void createLVMulti() {
        rightClick();
        moveTo("Create LV in VG");
        leftClick();
        moveTo("Create"); /* button */
        leftClick();
        moveTo("Close");
        leftClick();
    }

    static void resizeLV(final Cluster cluster) {
        rightClick();
        moveTo("Resize LV");
        leftClick();
        press(KeyEvent.VK_2);
        press(KeyEvent.VK_5);
        press(KeyEvent.VK_2);
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();

        moveTo("Resize");
        leftClick();
        moveTo("Close");
        leftClick();
    }

    static void vgRemove(final Cluster cluster) {
        rightClick();
        moveTo("Remove VG");
        leftClick();
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();
        moveTo("Remove VG"); /* button */
        leftClick();
    }

    static void lvRemove() {
        rightClick();
        moveTo("Remove LV");
        leftClick();
        moveTo("Remove"); /* button */
        leftClick();
    }

    static void lvRemoveMulti() {
        rightClick();
        moveTo("Remove selected LV");
        leftClick();
        moveTo("Remove"); /* button */
        leftClick();
    }

    /** Private constructor, cannot be instantiated. */
    private DrbdTest1() {
        /* Cannot be instantiated. */
    }
}
