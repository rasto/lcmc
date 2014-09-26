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

import lcmc.model.Cluster;
import lcmc.utilities.Tools;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class DrbdTest2 {
    @Inject
    private RoboTest roboTest;
    @Inject
    private DrbdTest1 drbdTest1;

    void start(final Cluster cluster, final int blockDevY) {
        roboTest.setSlowFactor(0.2f);
        roboTest.setAborted(false);
        final String drbdTest = "drbd-test1";
        for (int i = 0; i < 2; i++) {
            roboTest.info(drbdTest + "/1");
            drbdTest1.addDrbdResource(cluster, blockDevY);

            roboTest.moveTo(Tools.getString("Dialog.Dialog.Cancel"));
            roboTest.leftClick();
        }

        roboTest.info(drbdTest + "/2");
        drbdTest1.addDrbdResource(cluster, blockDevY);
        drbdTest1.chooseDrbdResource(cluster);

        roboTest.moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        roboTest.leftClick();

        roboTest.info(drbdTest + "/3");
        drbdTest1.addDrbdResource(cluster, blockDevY);
        drbdTest1.chooseDrbdResource(cluster);
        drbdTest1.addDrbdVolume();

        roboTest.moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        roboTest.leftClick();

        roboTest.info(drbdTest + "/4");
        drbdTest1.addDrbdResource(cluster, blockDevY);
        drbdTest1.chooseDrbdResource(cluster);
        drbdTest1.addDrbdVolume();
        drbdTest1.addBlockDevice();

        roboTest.moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        roboTest.leftClick();

        roboTest.info(drbdTest + "/5");
        drbdTest1.addDrbdResource(cluster, blockDevY);
        drbdTest1.chooseDrbdResource(cluster);
        drbdTest1.addDrbdVolume();
        drbdTest1.addBlockDevice();
        drbdTest1.addBlockDevice();
        roboTest.checkDRBDTest(drbdTest, 1);

        roboTest.moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        roboTest.leftClick();
        roboTest.confirmRemove();

        roboTest.info(drbdTest + "/6");
        drbdTest1.addDrbdResource(cluster, blockDevY);
        drbdTest1.chooseDrbdResource(cluster);
        drbdTest1.addDrbdVolume();
        drbdTest1.addBlockDevice();
        drbdTest1.addBlockDevice();
        drbdTest1.addMetaData();

        roboTest.moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        roboTest.leftClick();
        roboTest.confirmRemove();

        roboTest.info(drbdTest + "/7");
        drbdTest1.addDrbdResource(cluster, blockDevY);
        drbdTest1.chooseDrbdResource(cluster);
        drbdTest1.addDrbdVolume();
        drbdTest1.addBlockDevice();
        drbdTest1.addBlockDevice();
        drbdTest1.addMetaData();
        drbdTest1.addFileSystem();
        roboTest.checkDRBDTest(drbdTest, 1.1);

        roboTest.moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        roboTest.leftClick();
        roboTest.confirmRemove();

        roboTest.info(drbdTest + "/8");
        drbdTest1.addDrbdResource(cluster, blockDevY);
        drbdTest1.chooseDrbdResource(cluster);
        drbdTest1.addDrbdVolume();
        drbdTest1.addBlockDevice();
        drbdTest1.addBlockDevice();
        drbdTest1.addMetaData();
        drbdTest1.addFileSystem();
        roboTest.moveTo(Tools.getString("Dialog.Dialog.Finish"));
        roboTest.leftClick();
        roboTest.checkDRBDTest(drbdTest, 1.1);

        for (int i = 0; i < 3; i++) {
            drbdTest1.removeDrbdVolume(false);
        }
        drbdTest1.removeDrbdVolume(true);
        roboTest.checkDRBDTest(drbdTest, 2);
    }
}
