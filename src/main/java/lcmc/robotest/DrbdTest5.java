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
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is used to test the GUI.
 *
 * @author Rasto Levrinc
 */
final class DrbdTest5 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(DrbdTest5.class);

    /** Private constructor, cannot be instantiated. */
    private DrbdTest5() {
        /* Cannot be instantiated. */
    }

    static void start(final Cluster cluster, final int blockDevY) {
        /* Two bds. */
        slowFactor = 0.6f;
        aborted = false;
        /* multi */
        LOG.info("create pvs");
        moveTo(334, blockDevY);
        leftClick();

        moveTo(534, getY());
        controlLeftClick();

        moveTo(334, blockDevY + 40);
        controlLeftClick();

        moveTo(534, blockDevY + 40);
        controlLeftClick();

        createPV(334, blockDevY);
        LOG.info("create vgs");
        createVGMulti(blockDevY);
        sleepNoFactor(5000);
        LOG.info("create lvs");
        moveToGraph("VG vg00");
        sleepNoFactor(5000);
        createLVMulti();
        LOG.info("remove lvs");
        moveTo(334, blockDevY);
        leftClick();
        moveTo(534, blockDevY);
        controlLeftClick();
        lvRemoveMulti();
        LOG.info("remove vgs");
        sleepNoFactor(5000);
        moveToGraph("VG vg00");
        leftClick();
        moveTo(534, getY());
        controlLeftClick();
        rightClick();
        sleep(1000);
        moveTo("Remove selected VGs");
        leftClick();
        sleep(1000);
        moveTo("Remove VG"); /* button */
        leftClick();
        LOG.info("remove pvs");
        moveTo(334, blockDevY);
        leftClick();
        moveTo(534, blockDevY);
        controlLeftClick();
        moveTo(334, blockDevY + 40);
        controlLeftClick();
        moveTo(534, blockDevY + 40);
        controlLeftClick();
        rightClick();
        sleep(3000);
        moveTo("Remove selected PVs");
        sleep(2000);
        leftClick();

        moveTo(430, 90);
        leftClick(); // reset selection

        /* single */
        for (int i = 0; i < 2; i++) {
            LOG.info("create pv 1 " + i);
            createPV(334, blockDevY);
            LOG.info("create pv 2 " + i);
            createPV(534, blockDevY);
            LOG.info("create vg " + i);
            createVG(cluster, blockDevY);
            sleepNoFactor(10000);
            LOG.info("create lv" + i);
            moveToGraph("VG vg0" + i);
            createLV(cluster);
            LOG.info("resize lv" + i);
            moveToGraph("vg0" + i + "/lvol0");
            resizeLV(cluster);
        }
        int offset = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                moveToGraph("vg0" + i + "/lvol0");
                LOG.info("remove lv " + i + " " + j);
                lvRemove();
                sleepNoFactor(10000);
            }
            LOG.info("remove vg " + i);
            moveToGraph("VG vg0" + i);
            vgRemove(cluster);
            sleepNoFactor(10000);
            pvRemove(334, blockDevY + offset);
            pvRemove(534, blockDevY + offset);
            offset += 40;
        }
    }
}
