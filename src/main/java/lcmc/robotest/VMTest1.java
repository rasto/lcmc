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
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
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
final class VMTest1 {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(VMTest1.class);

    /** Private constructor, cannot be instantiated. */
    private VMTest1() {
        /* Cannot be instantiated. */
    }

    static void start(final Cluster cluster,
                      final String vmTest,
                      final int count) {
        startVMTest(cluster, vmTest, "kvm", count);
    }

    /** VM Test 1. */
    static void startVMTest(final Cluster cluster,
                            final String vmTest,
                            final String type,
                            final int count) {
        slowFactor = 0.1f;
        aborted = false;
        String name = "dmc";
        final int count2 = 3;
        for (int j = 0; j < count; j++) {
            checkVMTest(vmTest, 1, name);
            name += "i";
        }
        name = "dmc";
        final List<String> names = new ArrayList<String>();

        for (int j = 0; j < count; j++) {
            moveToMenu("VMs (KVM");
            rightClick();
            sleep(2000);
            moveTo("Add New Virtual Machine");
            leftClick();
            sleep(10000);
            dialogColorTest("new domain");
            moveTo("Domain name", Widget.MTextField.class); /* domain name */
            leftClick();
            press(KeyEvent.VK_D);
            sleep(200);
            press(KeyEvent.VK_M);
            sleep(200);
            press(KeyEvent.VK_C);
            sleep(200);
            for (int k = 0; k < j; k++) {
                press(KeyEvent.VK_I); /* dmci, dmcii, etc. */
                sleep(200);
            }
            /* type */
            moveTo("Domain Type", Widget.MComboBox.class);
            leftClick();
            sleep(1000);
            if ("lxc".equals(type)) {
                press(KeyEvent.VK_DOWN);
                sleep(1000);
                press(KeyEvent.VK_DOWN);
                sleep(1000);
                press(KeyEvent.VK_DOWN);
                sleep(1000);
                press(KeyEvent.VK_ENTER);
                sleep(3000);
            }

            /* next */
            moveTo("Next");
            leftClick();
            sleep(2000);

            if ("lxc".equals(type)) {
                /* filesystem */
                dialogColorTest("filesystem");
                moveTo("Source Dir", Widget.MComboBox.class);
                sleep(2000);
                leftClick();
                sleep(2000);
                press(KeyEvent.VK_END);
                sleep(200);
                press(KeyEvent.VK_DOWN);
                sleep(200);
                press(KeyEvent.VK_DOWN);
                sleep(200);
                press(KeyEvent.VK_ENTER);
                sleep(200);
                press(KeyEvent.VK_SLASH);
                sleep(200);
                press(KeyEvent.VK_D);
                sleep(200);
                press(KeyEvent.VK_M);
                sleep(200);
                press(KeyEvent.VK_C);
                sleep(200);
                for (int k = 0; k < j; k++) {
                    press(KeyEvent.VK_I); /* dmci, dmcii, etc. */
                    sleep(200);
                }
                press(KeyEvent.VK_SLASH);
                sleep(200);
                press(KeyEvent.VK_R);
                sleep(200);
                press(KeyEvent.VK_O);
                sleep(200);
                press(KeyEvent.VK_O);
                sleep(200);
                press(KeyEvent.VK_T);
                sleep(200);
                press(KeyEvent.VK_F);
                sleep(200);
                press(KeyEvent.VK_S);
                sleep(2000);
                moveTo("Next");
                leftClick();
            } else {
                /* source file */
                dialogColorTest("source file");

                moveTo("File", Widget.MComboBox.class);
                sleep(2000);
                leftClick();
                sleep(2000);
                press(KeyEvent.VK_END);
                sleep(200);
                press(KeyEvent.VK_T);
                sleep(200);
                press(KeyEvent.VK_E);
                sleep(200);
                press(KeyEvent.VK_S);
                sleep(200);
                press(KeyEvent.VK_T);
                sleep(2000);
                for (int i = 0; i < count2; i++) {
                    moveTo("Disk/block device");
                    leftClick();
                    sleep(1000);
                    moveTo("Image file");
                    leftClick();
                    sleep(1000);
                }

                moveTo("Next");
                leftClick();
                sleep(5000);
                dialogColorTest("disk image");
                moveTo("Next");
                leftClick();
            }
            sleep(5000);
            dialogColorTest("network");
            for (int i = 0; i < count2; i++) {
                moveTo("bridge");
                leftClick();
                sleep(1000);
                moveTo("network");
                leftClick();
                sleep(1000);
            }
            moveTo("Next");
            leftClick();
            sleep(10000);
            if (!"lxc".equals(type)) {
                dialogColorTest("display");
                for (int i = 0; i < count2; i++) {
                    moveTo("sdl"); /* sdl */
                    leftClick();
                    sleep(1000);
                    moveTo("vnc"); /* vnc */
                    leftClick();
                    sleep(1000);
                }
                moveTo("Next");
                leftClick();
                sleep(20000);
            }
            dialogColorTest("create config");

            sleep(10000);
            moveTo("Create Config");
            sleep(4000);
            leftClick();
            checkVMTest(vmTest, 2, name);
            sleep(8000);


            final String firstHost = cluster.getHostsArray()[0].getName();
            final String secondHost = cluster.getHostsArray()[1].getName();
            if (cluster.getHosts().size() > 1) {
                for (int i = 0; i < 3; i++) {
                    /* two hosts */
                    moveTo(firstHost, JCheckBox.class); /* deselect first */
                    leftClick();
                    sleep(10000);
                    moveTo("Create Config");
                    sleep(2000);
                    leftClick();
                    checkVMTest(cluster.getHostsArray()[0], vmTest, 1, name);
                    checkVMTest(cluster.getHostsArray()[1], vmTest, 2, name);

                    moveTo(firstHost, JCheckBox.class); /* select first */
                    sleep(1000);
                    leftClick();
                    sleep(1000);
                    moveTo(secondHost, JCheckBox.class); /* deselect second */
                    sleep(1000);
                    leftClick();
                    sleep(10000);
                    moveTo("Create Config");
                    sleep(2000);
                    leftClick();
                    checkVMTest(cluster.getHostsArray()[0], vmTest, 2, name);
                    checkVMTest(cluster.getHostsArray()[1], vmTest, 1, name);

                    moveTo(secondHost, JCheckBox.class); /* select second */
                    leftClick();
                    sleep(10000);
                    moveTo("Create Config");
                    sleep(4000);
                    leftClick();
                    checkVMTest(vmTest, 2, name);
                }
            }

            sleepNoFactor(2000);
            moveTo("Finish"); /* finish */
            leftClick();
            sleepNoFactor(5000);

            moveTo("Number of CPUs", Widget.MTextField.class);
            sleep(1000);
            leftClick();
            sleep(500);
            press(KeyEvent.VK_BACK_SPACE);
            sleep(500);
            press(KeyEvent.VK_2);
            sleep(500);
            moveTo("Apply");
            sleep(1000);
            leftClick();
            sleep(1000);
            checkVMTest(vmTest, 3, name);

            if (j  == 0) {
                for (int i = 0; i < count2; i++) {
                    /* remove net interface */
                    moveToMenu("dmc");
                    leftClick();
                    moveToMenu("default (:");
                    rightClick();
                    moveTo("Remove");
                    leftClick();
                    confirmRemove();
                    checkVMTest(vmTest, 3.001, name);

                    /* add net interface */
                    moveToMenu("dmc");
                    rightClick();
                    sleep(1000);
                    moveTo("Add Hardware");
                    sleep(1000);
                    moveTo("New Disk");
                    moveTo("New Network Interface");
                    leftClick();
                    sleep(2000);
                    moveTo("network");
                    leftClick();
                    sleep(2000);
                    moveTo("Apply");
                    leftClick();
                    checkVMTest(vmTest, 3, name);
                }
            }
            checkVMTest(vmTest, 3, name);

            if (j  == 0 && !"lxc".equals(type)) {
                /* add disk */
                moveToMenu("dmc");
                rightClick();
                sleep(2000);
                moveTo("Add Hardware");
                sleep(1000);
                moveTo("New Disk");
                leftClick();
                sleep(2000);
                moveTo("Disk/block device");
                leftClick();
                sleep(2000);
                moveTo("Device", Widget.MComboBox.class);
                leftClick();
                press(KeyEvent.VK_SLASH);
                press(KeyEvent.VK_D);
                press(KeyEvent.VK_E);
                press(KeyEvent.VK_V);

                press(KeyEvent.VK_SLASH);
                press(KeyEvent.VK_S);
                press(KeyEvent.VK_D);
                press(KeyEvent.VK_A);
                press(KeyEvent.VK_1);
                press(KeyEvent.VK_ENTER);
                moveTo("Apply");
                leftClick();
                checkVMTest(vmTest, 3.01, name);
                /* remove disk */
                moveToMenu("hdb (IDE");
                rightClick();
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_ENTER); /* remove */
                confirmRemove();
                checkVMTest(vmTest, 3, name);

                /* add disk /virtio */
                moveToMenu("dmc");
                rightClick();
                sleep(2000);
                moveTo("Add Hardware");
                sleep(1000);
                moveTo("New Disk");
                leftClick();
                sleep(2000);
                moveTo("Disk/block device");
                leftClick();
                sleep(2000);
                moveTo("Device", Widget.MComboBox.class);
                leftClick();
                press(KeyEvent.VK_SLASH);
                press(KeyEvent.VK_D);
                press(KeyEvent.VK_E);
                press(KeyEvent.VK_V);

                press(KeyEvent.VK_SLASH);
                press(KeyEvent.VK_S);
                press(KeyEvent.VK_D);
                press(KeyEvent.VK_A);
                press(KeyEvent.VK_2);
                press(KeyEvent.VK_ENTER);
                moveTo("Disk Type", Widget.MComboBox.class);
                leftClick();
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_ENTER); /* virtio */
                moveTo("Apply");
                leftClick();
                checkVMTest(vmTest, 3.02, name);

                /* remove disk */
                moveToMenu("vda (Virtio");
                rightClick();
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_ENTER); /* remove */
                confirmRemove();
                checkVMTest(vmTest, 3, name);
            }

            if (!"lxc".equals(type)) {
                /* disk readonly */
                moveToMenu("hda (IDE"); /* popup */
                leftClick();
                sleep(1000);
                moveTo("Readonly", JCheckBox.class);
                sleep(1000);
                leftClick();
                sleep(1000);
                moveTo("Apply"); /* apply */
                sleep(1000);
                leftClick();
                checkVMTest(vmTest, 3.1, name);
                sleep(1000);
                Tools.getGUIData().expandTerminalSplitPane(1);
                moveTo("Readonly", JCheckBox.class);
                sleep(1000);
                leftClick();

                sleep(1000);
                moveTo("VM Host Overview"); /* host overview */
                sleep(1000);
                leftClick();
                sleep(1000);

                moveTo("Apply"); /* host apply */
                leftClick();
                checkVMTest(vmTest, 3.2, name);
            }

            names.add(name);
            for (final String n : names) {
                checkVMTest(vmTest, 3, n);
            }
            name += "i";
        }

        sleepNoFactor(5000);
        for (int j = 0; j < count; j++) {
            moveToMenu("dmc");
            rightClick();
            sleep(1000);
            moveTo("Remove Domain");
            leftClick();
            sleepNoFactor(2000);
            dialogColorTest("remove VM");
            confirmRemove();
            leftClick();
            sleepNoFactor(5000);
        }
    }
}
