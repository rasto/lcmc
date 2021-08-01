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

import javax.inject.Named;
import javax.inject.Singleton;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.widget.MComboBox;
import lcmc.common.domain.util.Tools;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class DrbdTest1 {
    private final RoboTest roboTest;

    public DrbdTest1(RoboTest roboTest) {
        this.roboTest = roboTest;
    }

    void start(final Cluster cluster, final int blockDevY) {
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);

        addDrbdResource(cluster, blockDevY);
        chooseDrbdResource(cluster);
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        addMetaData();
        addFileSystem();
        roboTest.moveTo(Tools.getString("Dialog.Dialog.Finish"));
        roboTest.leftClick();
        final String drbdTest = "drbd-test1";
        roboTest.checkDRBDTest(drbdTest, 1.1);
        for (int i = 0; i < 2; i++) {
            roboTest.info("i: " + i);
            removeDrbdVolume(false);
        }
        removeDrbdVolume(true);
        roboTest.checkDRBDTest(drbdTest, 2);
    }

    void addDrbdResource(final Cluster cluster, final int blockDevY) {
        roboTest.moveTo(334, blockDevY); /* add drbd resource */
        roboTest.rightClick();
        roboTest.moveTo(Tools.getString("HostBrowser.Drbd.AddDrbdResource"));
        roboTest.moveTo(cluster.getHostsArray()[1].getName());
        roboTest.sleep(2000);
        roboTest.press(KeyEvent.VK_DOWN);
        roboTest.press(KeyEvent.VK_ENTER);
        roboTest.dialogColorTest("addDrbdResource");
        roboTest.moveTo(500, 300);
        roboTest.rightClick();
    }

    void drbdNext() {
        roboTest.press(KeyEvent.VK_ENTER);
    }

    void newDrbdResource() {
        drbdNext();
        roboTest.dialogColorTest("newDrbdResource");
        roboTest.moveTo(500, 300);
        roboTest.rightClick();
    }

    void chooseDrbdResourceInterface(final String hostName, final boolean proxy) {
        roboTest.moveTo("on " + hostName, MComboBox.class); /* interface */
        roboTest.leftClick();
        if (proxy) {
            roboTest.press(KeyEvent.VK_P); /* select first interface to proxy*/
            roboTest.press(KeyEvent.VK_DOWN);
        } else {
            roboTest.press(KeyEvent.VK_E); /* select first interface */
        }
        roboTest.dialogColorTest("chooseDrbdResourceInterface");
        roboTest.moveTo(500, 300);
        roboTest.rightClick();
    }

    void chooseDrbdResource(final Cluster cluster) {
        chooseDrbdResourceInterface(cluster.getHostsArray()[0].getName(), !RoboTest.PROXY);
        chooseDrbdResourceInterface(cluster.getHostsArray()[1].getName(), !RoboTest.PROXY);

        drbdNext();
        roboTest.dialogColorTest("chooseDrbdResource");
        roboTest.moveTo(500, 300);
        roboTest.rightClick();
    }

    void addDrbdVolume() {
        drbdNext();
        roboTest.dialogColorTest("addDrbdVolume");
        roboTest.moveTo(500, 300);
        roboTest.rightClick();
    }

    void addBlockDevice() {
        drbdNext();
        roboTest.dialogColorTest("addBlockDevice");
        roboTest.moveTo(500, 300);
        roboTest.rightClick();
    }

    void addMetaData() {
        drbdNext();
        roboTest.dialogColorTest("addMetaData");
        roboTest.moveTo(500, 300);
        roboTest.rightClick();
    }

    void addFileSystem() {
        /* do nothing. */
        roboTest.dialogColorTest("addFileSystem");
        roboTest.moveTo(500, 300);
        roboTest.rightClick();
    }

    void removeDrbdVolume(final boolean really) {
        if (roboTest.isAborted()) {
            return;
        }
        roboTest.moveTo(480, 152); /* rsc popup */
        roboTest.rightClick(); /* remove */
        roboTest.moveTo(Tools.getString("ClusterBrowser.Drbd.RemoveEdge"));
        roboTest.leftClick();
        roboTest.dialogColorTest("removeDrbdVolume");
        if (really) {
            roboTest.confirmRemove();
        } else {
            roboTest.press(KeyEvent.VK_ENTER); /* cancel */
        }
    }

    void createPV(final int blockDevX, final int blockDevY) {
        roboTest.moveTo(blockDevX, blockDevY);
        roboTest.rightClick();
        roboTest.moveTo("Create PV");
        roboTest.leftClick();
    }

    void pvRemove(final int blockDevX, final int blockDevY) {
        roboTest.moveTo(blockDevX, blockDevY);
        roboTest.rightClick();
        roboTest.moveTo("Remove PV");
        roboTest.leftClick();
    }

    void createVG(final Cluster cluster, final int blockDevY) {
        roboTest.moveTo(334, blockDevY);
        roboTest.rightClick();
        roboTest.moveTo("Create VG");
        roboTest.leftClick();
        roboTest.moveTo(cluster.getHostsArray()[1].getName());
        roboTest.leftClick();
        roboTest.moveTo("Create VG"); /* button */
        roboTest.leftClick();
    }

    void createVGMulti(final int blockDevY) {
        roboTest.moveTo(334, blockDevY);
        roboTest.rightClick();
        roboTest.moveTo("Create VG");
        roboTest.leftClick();
        roboTest.moveTo("Create VG"); /* button */
        roboTest.leftClick();
    }

    void createLV(final Cluster cluster) {
        roboTest.rightClick();
        roboTest.moveTo("Create LV in VG");
        roboTest.leftClick();
        roboTest.moveTo(cluster.getHostsArray()[1].getName());
        roboTest.leftClick();
        roboTest.moveTo("Create"); /* button */
        roboTest.leftClick();
        roboTest.moveTo("Close");
        roboTest.leftClick();
    }

    void createLVMulti() {
        roboTest.rightClick();
        roboTest.moveTo("Create LV in VG");
        roboTest.leftClick();
        roboTest.moveTo("Create"); /* button */
        roboTest.leftClick();
        roboTest.moveTo("Close");
        roboTest.leftClick();
    }

    void resizeLV(final Cluster cluster) {
        roboTest.rightClick();
        roboTest.moveTo("Resize LV");
        roboTest.leftClick();
        roboTest.press(KeyEvent.VK_2);
        roboTest.press(KeyEvent.VK_5);
        roboTest.press(KeyEvent.VK_2);
        roboTest.moveTo(cluster.getHostsArray()[1].getName());
        roboTest.leftClick();

        roboTest.moveTo("Resize");
        roboTest.leftClick();
        roboTest.moveTo("Close");
        roboTest.leftClick();
    }

    void vgRemove(final Cluster cluster) {
        roboTest.rightClick();
        roboTest.moveTo("Remove VG");
        roboTest.leftClick();
        roboTest.moveTo(cluster.getHostsArray()[1].getName());
        roboTest.leftClick();
        roboTest.moveTo("Remove VG"); /* button */
        roboTest.leftClick();
    }

    void lvRemove() {
        roboTest.rightClick();
        roboTest.moveTo("Remove LV");
        roboTest.leftClick();
        roboTest.moveTo("Remove"); /* button */
        roboTest.leftClick();
    }

    void lvRemoveMulti() {
        roboTest.rightClick();
        roboTest.moveTo("Remove selected LV");
        roboTest.leftClick();
        roboTest.moveTo("Remove"); /* button */
        roboTest.leftClick();
    }
}
