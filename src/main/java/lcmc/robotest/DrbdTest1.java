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
import lcmc.gui.widget.Widget;
import static lcmc.robotest.RoboTest.*;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class DrbdTest1 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(DrbdTest1.class);

    /** Private constructor, cannot be instantiated. */
    private DrbdTest1() {
        /* Cannot be instantiated. */
    }

    static void start(final Cluster cluster, final int blockDevY) {
        final String drbdTest = "drbd-test1";
        slowFactor = 0.2f;
        aborted = false;

        addDrbdResource(cluster, blockDevY);
        chooseDrbdResource(cluster);
        addDrbdVolume();
        addBlockDevice();
        sleep(1000);
        addBlockDevice();
        sleep(20000);
        addMetaData();
        addFileSystem();
        sleep(5000);
        moveTo(Tools.getString("Dialog.Dialog.Finish"));
        leftClick();
        sleep(10000);
        checkDRBDTest(drbdTest, 1.1);
        for (int i = 0; i < 2; i++) {
            info("i: " + i);
            removeDrbdVolume(false);
        }
        removeDrbdVolume(true);
        checkDRBDTest(drbdTest, 2);
    }

    static void addDrbdResource(final Cluster cluster,
                                final int blockDevY) {
        moveTo(334, blockDevY); /* add drbd resource */
        rightClick();
        sleep(1000);
        moveTo(Tools.getString("HostBrowser.Drbd.AddDrbdResource"));
        sleep(1000);
        moveTo(cluster.getHostsArray()[1].getName());
        sleep(2000);
        press(KeyEvent.VK_DOWN);
        sleep(500);
        press(KeyEvent.VK_ENTER);
        sleep(20000);
        dialogColorTest("addDrbdResource");
    }

    static void drbdNext() {
        press(KeyEvent.VK_ENTER);
    }

    static void newDrbdResource() {
        drbdNext();
        sleep(10000);
        dialogColorTest("newDrbdResource");
    }

    static void chooseDrbdResourceInterface(final String hostName,
                                            final boolean proxy) {
        moveTo("on " + hostName, Widget.MComboBox.class); /* interface */
        leftClick();
        sleep(500);
        if (proxy) {
            press(KeyEvent.VK_P); /* select first interface to proxy*/
        } else {
            press(KeyEvent.VK_E); /* select first interface */
        }
        sleep(200);
        press(KeyEvent.VK_ENTER);
        sleep(1000);
        dialogColorTest("chooseDrbdResourceInterface");
    }

    static void chooseDrbdResource(final Cluster cluster) {
        chooseDrbdResourceInterface(cluster.getHostsArray()[0].getName(),
                                    !PROXY);
        chooseDrbdResourceInterface(cluster.getHostsArray()[1].getName(),
                                    !PROXY);

        drbdNext();
        sleep(10000);
        dialogColorTest("chooseDrbdResource");
    }

    static void addDrbdVolume() {
        drbdNext();
        sleep(10000);
        dialogColorTest("addDrbdVolume");
    }

    static void addBlockDevice() {
        drbdNext();
        sleep(10000);
        dialogColorTest("addBlockDevice");
    }

    static void addMetaData() {
        drbdNext();
        sleep(30000);
        dialogColorTest("addMetaData");
    }

    static void addFileSystem() {
        /* do nothing. */
        dialogColorTest("addFileSystem");
    }

    static void removeDrbdVolume(final boolean really) {
        if (aborted) {
            return;
        }
        moveTo(480, 152); /* rsc popup */
        rightClick(); /* remove */
        moveTo(Tools.getString("ClusterBrowser.Drbd.RemoveEdge"));
        leftClick();
        Tools.sleep(10000);
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
        sleep(1000);
        moveTo("Create PV");
        leftClick();
        sleep(5000);
    }

    static void pvRemove(final int blockDevX, final int blockDevY) {
        moveTo(blockDevX, blockDevY);
        rightClick();
        sleep(3000);
        moveTo("Remove PV");
        leftClick();
    }

    static void createVG(final Cluster cluster, final int blockDevY) {
        moveTo(334, blockDevY);
        rightClick();
        sleep(1000);
        moveTo("Create VG");
        leftClick();
        sleep(2000);
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();
        moveTo("Create VG"); /* button */
        leftClick();
    }

    static void createVGMulti(final int blockDevY) {
        moveTo(334, blockDevY);
        rightClick();
        sleep(1000);
        moveTo("Create VG");
        leftClick();
        sleep(2000);
        moveTo("Create VG"); /* button */
        leftClick();
    }

    static void createLV(final Cluster cluster) {
        rightClick();
        sleep(1000);
        moveTo("Create LV in VG");
        leftClick();
        sleep(2000);
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();
        moveTo("Create"); /* button */
        leftClick();
        sleep(3000);
        moveTo("Close");
        leftClick();
    }

    static void createLVMulti() {
        rightClick();
        moveTo("Create LV in VG");
        leftClick();
        sleep(2000);
        moveTo("Create"); /* button */
        leftClick();
        sleep(3000);
        moveTo("Close");
        leftClick();
    }

    static void resizeLV(final Cluster cluster) {
        rightClick();
        sleep(1000);
        moveTo("Resize LV");
        leftClick();
        sleep(5000);
        press(KeyEvent.VK_2);
        press(KeyEvent.VK_5);
        press(KeyEvent.VK_2);
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();

        moveTo("Resize");
        leftClick();
        sleep(3000);
        moveTo("Close");
        leftClick();
    }

    static void vgRemove(final Cluster cluster) {
        rightClick();
        sleep(1000);
        moveTo("Remove VG");
        leftClick();
        sleep(2000);
        moveTo(cluster.getHostsArray()[1].getName());
        leftClick();
        moveTo("Remove VG"); /* button */
        leftClick();
    }

    static void lvRemove() {
        sleep(5000);
        rightClick();
        sleep(5000);
        moveTo("Remove LV");
        leftClick();
        sleep(2000);
        moveTo("Remove"); /* button */
        leftClick();
    }

    static void lvRemoveMulti() {
        sleep(5000);
        rightClick();
        sleep(5000);
        moveTo("Remove selected LV");
        leftClick();
        sleep(2000);
        moveTo("Remove"); /* button */
        leftClick();
    }
}
