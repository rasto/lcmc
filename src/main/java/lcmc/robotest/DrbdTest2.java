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

import lcmc.data.Cluster;
import static lcmc.robotest.RoboTest.*;
import static lcmc.robotest.DrbdTest1.*;
import lcmc.utilities.Tools;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class DrbdTest2 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(DrbdTest2.class);

    /** Private constructor, cannot be instantiated. */
    private DrbdTest2() {
        /* Cannot be instantiated. */
    }

    static void start(final Cluster cluster, final int blockDevY) {
        final String drbdTest = "drbd-test1";
        slowFactor = 0.2f;
        aborted = false;
        for (int i = 0; i < 2; i++) {
            info(drbdTest + "/1");
            addDrbdResource(cluster, blockDevY);

            moveTo(Tools.getString("Dialog.Dialog.Cancel"));
            leftClick();
            sleep(2000);
        }

        info(drbdTest + "/2");
        addDrbdResource(cluster, blockDevY);
        chooseDrbdResource(cluster);

        moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        leftClick();
        sleep(20000);

        info(drbdTest + "/3");
        addDrbdResource(cluster, blockDevY);
        chooseDrbdResource(cluster);
        addDrbdVolume();

        moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        leftClick();
        sleep(10000);

        info(drbdTest + "/4");
        addDrbdResource(cluster, blockDevY);
        chooseDrbdResource(cluster);
        addDrbdVolume();
        addBlockDevice();
        sleep(20000);

        moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        leftClick();
        sleep(10000);

        info(drbdTest + "/5");
        addDrbdResource(cluster, blockDevY);
        chooseDrbdResource(cluster);
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(20000);
        checkDRBDTest(drbdTest, 1);

        moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        leftClick();
        confirmRemove();
        sleep(60000);

        info(drbdTest + "/6");
        addDrbdResource(cluster, blockDevY);
        chooseDrbdResource(cluster);
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(20000);
        addMetaData();

        moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        leftClick();
        confirmRemove();
        sleep(20000);

        info(drbdTest + "/7");
        addDrbdResource(cluster, blockDevY);
        chooseDrbdResource(cluster);
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(20000);
        addMetaData();
        addFileSystem();
        checkDRBDTest(drbdTest, 1.1);
        sleep(10000);

        moveTo(Tools.getString("Dialog.Dialog.Cancel"));
        sleep(2000);
        leftClick();
        sleep(20000);
        confirmRemove();
        sleep(20000);

        info(drbdTest + "/8");
        addDrbdResource(cluster, blockDevY);
        chooseDrbdResource(cluster);
        addDrbdVolume();
        addBlockDevice();
        addBlockDevice();
        sleep(20000);
        addMetaData();
        addFileSystem();
        moveTo(Tools.getString("Dialog.Dialog.Finish"));
        leftClick();
        sleep(10000);
        checkDRBDTest(drbdTest, 1.1);

        for (int i = 0; i < 3; i++) {
            removeDrbdVolume(false);
        }
        removeDrbdVolume(true);
        checkDRBDTest(drbdTest, 2);
    }
}
