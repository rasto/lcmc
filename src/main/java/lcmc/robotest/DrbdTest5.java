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

import lcmc.cluster.domain.Cluster;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lombok.RequiredArgsConstructor;

/**
 * This class is used to test the GUI.
 */
@RequiredArgsConstructor
final class DrbdTest5 {
    private final RoboTest roboTest;
    private final DrbdTest1 drbdTest1;

    private static final Logger LOG = LoggerFactory.getLogger(DrbdTest5.class);

    void start(final Cluster cluster, final int blockDevY) {
        /* Two bds. */
        roboTest.setSlowFactor(0.6f);
        roboTest.setAborted(false);
        /* multi */
        LOG.info("start: create pvs");
        roboTest.moveTo(334, blockDevY);
        roboTest.leftClick();

        roboTest.moveTo(534, roboTest.getY());
        roboTest.controlLeftClick();

        roboTest.moveTo(334, blockDevY + 40);
        roboTest.controlLeftClick();

        roboTest.moveTo(534, blockDevY + 40);
        roboTest.controlLeftClick();

        drbdTest1.createPV(334, blockDevY);
        LOG.info("start: create vgs");
        drbdTest1.createVGMulti(blockDevY);
        roboTest.sleepNoFactor(5000);
        LOG.info("start: create lvs");
        roboTest.moveToGraph("VG vg00");
        roboTest.sleepNoFactor(5000);
        drbdTest1.createLVMulti();
        LOG.info("start: remove lvs");
        roboTest.moveTo(334, blockDevY);
        roboTest.leftClick();
        roboTest.moveTo(534, blockDevY);
        roboTest.controlLeftClick();
        drbdTest1.lvRemoveMulti();
        LOG.info("start: remove vgs");
        roboTest.sleepNoFactor(5000);
        roboTest.moveToGraph("VG vg00");
        roboTest.leftClick();
        roboTest.moveTo(534, roboTest.getY());
        roboTest.controlLeftClick();
        roboTest.rightClick();
        roboTest.sleep(1000);
        roboTest.moveTo("Remove selected VGs");
        roboTest.leftClick();
        roboTest.sleep(1000);
        roboTest.moveTo("Remove VG"); /* button */
        roboTest.leftClick();
        LOG.info("start: remove pvs");
        roboTest.moveTo(334, blockDevY);
        roboTest.leftClick();
        roboTest.moveTo(534, blockDevY);
        roboTest.controlLeftClick();
        roboTest.moveTo(334, blockDevY + 40);
        roboTest.controlLeftClick();
        roboTest.moveTo(534, blockDevY + 40);
        roboTest.controlLeftClick();
        roboTest.rightClick();
        roboTest.sleep(3000);
        roboTest.moveTo("Remove selected PVs");
        roboTest.sleep(2000);
        roboTest.leftClick();

        roboTest.moveTo(430, 90);
        roboTest.leftClick(); // reset selection

        /* single */
        for (int i = 0; i < 2; i++) {
            LOG.info("start: create pv 1 " + i);
            drbdTest1.createPV(334, blockDevY);
            LOG.info("start: create pv 2 " + i);
            drbdTest1.createPV(534, blockDevY);
            LOG.info("start: create vg " + i);
            drbdTest1.createVG(cluster, blockDevY);
            roboTest.sleepNoFactor(10000);
            LOG.info("start: create lv" + i);
            roboTest.moveToGraph("VG vg0" + i);
            drbdTest1.createLV(cluster);
            LOG.info("start: resize lv" + i);
            roboTest.moveToGraph("vg0" + i + "/lvol0");
            drbdTest1.resizeLV(cluster);
        }
        int offset = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                roboTest.moveToGraph("vg0" + i + "/lvol0");
                LOG.info("start: remove lv " + i + ' ' + j);
                drbdTest1.lvRemove();
                roboTest.sleepNoFactor(10000);
            }
            LOG.info("start: remove vg " + i);
            roboTest.moveToGraph("VG vg0" + i);
            drbdTest1.vgRemove(cluster);
            roboTest.sleepNoFactor(10000);
            drbdTest1.pvRemove(334, blockDevY + offset);
            drbdTest1.pvRemove(534, blockDevY + offset);
            offset += 40;
        }
    }
}
