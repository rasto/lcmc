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
import java.util.Collection;
import java.util.List;
import javax.swing.JCheckBox;
import lcmc.data.Cluster;
import lcmc.gui.widget.GenericWidget.MTextField;
import lcmc.gui.widget.MComboBox;
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
        for (int j = 0; j < count; j++) {
            checkVMTest(vmTest, 1, name);
            name += "i";
        }
        name = "dmc";
        final Collection<String> names = new ArrayList<String>();

        final int count2 = 1;
        for (int j = 0; j < count; j++) {
            moveToMenu("VMs (KVM");
            rightClick();
            moveTo("Add New Virtual Machine");
            leftClick();
            dialogColorTest("new domain");
            moveTo("Domain name", MTextField.class); /* domain name */
            leftClick();
            press(KeyEvent.VK_D);
            press(KeyEvent.VK_M);
            press(KeyEvent.VK_C);
            for (int k = 0; k < j; k++) {
                press(KeyEvent.VK_I); /* dmci, dmcii, etc. */
            }
            /* type */
            moveTo("Domain Type", MComboBox.class);
            leftClick();
            if ("lxc".equals(type)) {
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_ENTER);
            }

            /* next */
            moveTo("Next");
            leftClick();

            if ("lxc".equals(type)) {
                /* filesystem */
                dialogColorTest("filesystem");
                moveTo("Source Dir", MComboBox.class);
                leftClick();
                press(KeyEvent.VK_END);
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_DOWN);
                press(KeyEvent.VK_ENTER);
                press(KeyEvent.VK_SLASH);
                press(KeyEvent.VK_D);
                press(KeyEvent.VK_M);
                press(KeyEvent.VK_C);
                for (int k = 0; k < j; k++) {
                    press(KeyEvent.VK_I); /* dmci, dmcii, etc. */
                }
                press(KeyEvent.VK_SLASH);
                press(KeyEvent.VK_R);
                press(KeyEvent.VK_O);
                press(KeyEvent.VK_O);
                press(KeyEvent.VK_T);
                press(KeyEvent.VK_F);
                press(KeyEvent.VK_S);
                moveTo("Next");
                leftClick();
            } else {
                /* source file */
                dialogColorTest("source file");

                moveTo("File", MComboBox.class);
                leftClick();
                press(KeyEvent.VK_END);
                press(KeyEvent.VK_T);
                press(KeyEvent.VK_E);
                press(KeyEvent.VK_S);
                press(KeyEvent.VK_T);
                for (int i = 0; i < count2; i++) {
                    moveTo("Disk/block device");
                    leftClick();
                    moveTo("Image file");
                    leftClick();
                }

                moveTo("Next");
                leftClick();
                dialogColorTest("disk image");
                moveTo("Next");
                leftClick();
            }
            dialogColorTest("network");
            for (int i = 0; i < count2; i++) {
                moveTo("bridge");
                leftClick();
                moveTo("network");
                leftClick();
            }
            moveTo("Next");
            leftClick();
            if (!"lxc".equals(type)) {
                dialogColorTest("display");
                for (int i = 0; i < count2; i++) {
                    moveTo("sdl"); /* sdl */
                    leftClick();
                    moveTo("vnc"); /* vnc */
                    leftClick();
                }
                moveTo("Next");
                leftClick();
            }
            dialogColorTest("create config");

            moveTo("Create Config");
            leftClick();
            checkVMTest(vmTest, 2, name);

            final String firstHost = cluster.getHostsArray()[0].getName();
            final String secondHost = cluster.getHostsArray()[1].getName();
            if (cluster.getHosts().size() > 1) {
                /* two hosts */
                moveTo(firstHost, JCheckBox.class); /* deselect first */
                leftClick();
                moveTo("Create Config");
                leftClick();
                checkVMTest(cluster.getHostsArray()[0], vmTest, 1, name);
                checkVMTest(cluster.getHostsArray()[1], vmTest, 2, name);

                moveTo(firstHost, JCheckBox.class); /* select first */
                leftClick();
                moveTo(secondHost, JCheckBox.class); /* deselect second */
                leftClick();
                moveTo("Create Config");
                leftClick();
                checkVMTest(cluster.getHostsArray()[0], vmTest, 2, name);
                checkVMTest(cluster.getHostsArray()[1], vmTest, 1, name);

                moveTo(secondHost, JCheckBox.class); /* select second */
                leftClick();
                moveTo("Create Config");
                leftClick();
                checkVMTest(vmTest, 2, name);
            }

            moveTo("Finish"); /* finish */
            leftClick();

            moveTo("Number of CPUs", MTextField.class);
            leftClick();
            Tools.sleep(500);
            press(KeyEvent.VK_BACK_SPACE);
            press(KeyEvent.VK_2);
            moveTo("Apply");
            leftClick();
            checkVMTest(vmTest, 3, name);

            if (j  == 0) {
                for (int i = 0; i < count2; i++) {
                    /* remove net interface */
                    moveToMenu("VMs (");
                    leftClick();
                    press(KeyEvent.VK_RIGHT);

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
                    moveTo("Add Hardware");
                    moveTo("New Disk");
                    moveTo("New Network Interface");
                    leftClick();
                    Tools.sleep(500);
                    moveTo("network");
                    leftClick();
                    moveTo("Apply");
                    leftClick();
                    checkVMTest(vmTest, 3, name);
                }
            }
            checkVMTest(vmTest, 3, name);

            if (j  == 0 && !"lxc".equals(type)) {
                /* add disk */
                moveToMenu("VMs (");
                leftClick();
                press(KeyEvent.VK_RIGHT);

                moveToMenu("dmc");
                rightClick();
                moveTo("Add Hardware");
                moveTo("New Disk");
                leftClick();
                Tools.sleep(500);
                moveTo("Disk/block device");
                leftClick();
                moveTo("Device", MComboBox.class);
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
                moveTo("Add Hardware");
                moveTo("New Disk");
                leftClick();
                Tools.sleep(500);
                moveTo("Disk/block device");
                leftClick();
                moveTo("Device", MComboBox.class);
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
                moveTo("Disk Type", MComboBox.class);
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
                moveTo("Readonly", JCheckBox.class);
                leftClick();
                moveTo("Apply"); /* apply */
                leftClick();
                checkVMTest(vmTest, 3.1, name);
                Tools.getGUIData().expandTerminalSplitPane(1);
                moveTo("Readonly", JCheckBox.class);
                leftClick();

                moveTo("VM Host Overview"); /* host overview */
                leftClick();

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

        for (int j = 0; j < count; j++) {
            moveToMenu("dmc");
            rightClick();
            moveTo("Remove Domain");
            leftClick();
            dialogColorTest("remove VM");
            confirmRemove();
            leftClick();
            Tools.sleep(500);
        }
    }
}
