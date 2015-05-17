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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JCheckBox;

import lcmc.common.ui.GUIData;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.widget.GenericWidget.MTextField;
import lcmc.cluster.ui.widget.MComboBox;
import lcmc.common.domain.util.Tools;

/**
 * This class is used to test the GUI.
 */
@Named
@Singleton
final class VMTest1 {
    @Inject
    private RoboTest roboTest;
    @Inject
    private GUIData guiData;

    void start(final Cluster cluster, final String vmTest, final int count) {
        startVMTest(cluster, vmTest, "kvm", count);
    }

    /** VM Test 1. */
    void startVMTest(final Cluster cluster, final String vmTest, final String type, final int count) {
        roboTest.setSlowFactor(0.1f);
        roboTest.setAborted(false);
        String name = "dmc";
        for (int j = 0; j < count; j++) {
            roboTest.checkVMTest(vmTest, 1, name);
            name += "i";
        }
        name = "dmc";
        final Collection<String> names = new ArrayList<String>();

        final int count2 = 1;
        for (int j = 0; j < count; j++) {
            roboTest.moveToMenu("VMs (KVM");
            roboTest.rightClick();
            roboTest.moveTo("Add New Virtual Machine");
            roboTest.leftClick();
            roboTest.dialogColorTest("new domain");
            roboTest.moveTo("Domain name", MTextField.class); /* domain name */
            roboTest.leftClick();
            roboTest.press(KeyEvent.VK_D);
            roboTest.press(KeyEvent.VK_M);
            roboTest.press(KeyEvent.VK_C);
            for (int k = 0; k < j; k++) {
                roboTest.press(KeyEvent.VK_I); /* dmci, dmcii, etc. */
            }
            /* type */
            roboTest.moveTo("Domain Type", MComboBox.class);
            roboTest.leftClick();
            if ("lxc".equals(type)) {
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_ENTER);
            }

            /* next */
            roboTest.moveTo("Next");
            roboTest.leftClick();

            if ("lxc".equals(type)) {
                /* filesystem */
                roboTest.dialogColorTest("filesystem");
                roboTest.moveTo("Source Dir", MComboBox.class);
                roboTest.leftClick();
                roboTest.press(KeyEvent.VK_END);
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_ENTER);
                roboTest.press(KeyEvent.VK_SLASH);
                roboTest.press(KeyEvent.VK_D);
                roboTest.press(KeyEvent.VK_M);
                roboTest.press(KeyEvent.VK_C);
                for (int k = 0; k < j; k++) {
                    roboTest.press(KeyEvent.VK_I); /* dmci, dmcii, etc. */
                }
                roboTest.press(KeyEvent.VK_SLASH);
                roboTest.press(KeyEvent.VK_R);
                roboTest.press(KeyEvent.VK_O);
                roboTest.press(KeyEvent.VK_O);
                roboTest.press(KeyEvent.VK_T);
                roboTest.press(KeyEvent.VK_F);
                roboTest.press(KeyEvent.VK_S);
                roboTest.moveTo("Next");
                roboTest.leftClick();
            } else {
                /* source file */
                roboTest.dialogColorTest("source file");

                roboTest.moveTo("File", MComboBox.class);
                roboTest.leftClick();
                roboTest.press(KeyEvent.VK_END);
                roboTest.press(KeyEvent.VK_T);
                roboTest.press(KeyEvent.VK_E);
                roboTest.press(KeyEvent.VK_S);
                roboTest.press(KeyEvent.VK_T);
                for (int i = 0; i < count2; i++) {
                    roboTest.moveTo("Disk/block device");
                    roboTest.leftClick();
                    roboTest.moveTo("Image file");
                    roboTest.leftClick();
                }

                roboTest.moveTo("Next");
                roboTest.leftClick();
                roboTest.dialogColorTest("disk image");
                roboTest.moveTo("Next");
                roboTest.leftClick();
            }
            roboTest.dialogColorTest("network");
            for (int i = 0; i < count2; i++) {
                roboTest.moveTo("bridge");
                roboTest.leftClick();
                roboTest.moveTo("network");
                roboTest.leftClick();
            }
            roboTest.moveTo("Next");
            roboTest.leftClick();
            if (!"lxc".equals(type)) {
                roboTest.dialogColorTest("display");
                for (int i = 0; i < count2; i++) {
                    roboTest.moveTo("sdl"); /* sdl */
                    roboTest.leftClick();
                    roboTest.moveTo("vnc"); /* vnc */
                    roboTest.leftClick();
                }
                roboTest.moveTo("Next");
                roboTest.leftClick();
            }
            roboTest.dialogColorTest("create config");

            roboTest.moveTo("Create Config");
            roboTest.leftClick();
            roboTest.checkVMTest(vmTest, 2, name);

            final String firstHost = cluster.getHostsArray()[0].getName();
            final String secondHost = cluster.getHostsArray()[1].getName();
            if (cluster.getHosts().size() > 1) {
                /* two hosts */
                roboTest.moveTo(firstHost, JCheckBox.class); /* deselect first */
                roboTest.leftClick();
                roboTest.moveTo("Create Config");
                roboTest.leftClick();
                roboTest.checkVMTest(cluster.getHostsArray()[0], vmTest, 1, name);
                roboTest.checkVMTest(cluster.getHostsArray()[1], vmTest, 2, name);

                roboTest.moveTo(firstHost, JCheckBox.class); /* select first */
                roboTest.leftClick();
                roboTest.moveTo(secondHost, JCheckBox.class); /* deselect second */
                roboTest.leftClick();
                roboTest.moveTo("Create Config");
                roboTest.leftClick();
                roboTest.checkVMTest(cluster.getHostsArray()[0], vmTest, 2, name);
                roboTest.checkVMTest(cluster.getHostsArray()[1], vmTest, 1, name);

                roboTest.moveTo(secondHost, JCheckBox.class); /* select second */
                roboTest.leftClick();
                roboTest.moveTo("Create Config");
                roboTest.leftClick();
                roboTest.checkVMTest(vmTest, 2, name);
            }

            roboTest.moveTo("Finish"); /* finish */
            roboTest.leftClick();

            roboTest.moveTo("Number of CPUs", MTextField.class);
            roboTest.leftClick();
            Tools.sleep(500);
            roboTest.press(KeyEvent.VK_BACK_SPACE);
            roboTest.press(KeyEvent.VK_2);
            roboTest.moveTo("Apply");
            roboTest.leftClick();
            roboTest.checkVMTest(vmTest, 3, name);

            if (j  == 0) {
                for (int i = 0; i < count2; i++) {
                    /* remove net interface */
                    roboTest.moveToMenu("VMs (");
                    roboTest.leftClick();
                    roboTest.press(KeyEvent.VK_RIGHT);

                    roboTest.moveToMenu("dmc");
                    roboTest.leftClick();
                    roboTest.moveToMenu("default (:");
                    roboTest.rightClick();
                    roboTest.moveTo("Remove");
                    roboTest.leftClick();
                    roboTest.confirmRemove();
                    roboTest.checkVMTest(vmTest, 3.001, name);

                    /* add net interface */
                    roboTest.moveToMenu("dmc");
                    roboTest.rightClick();
                    roboTest.moveTo("Add Hardware");
                    roboTest.moveTo("New Disk");
                    roboTest.moveTo("New Network Interface");
                    roboTest.leftClick();
                    Tools.sleep(500);
                    roboTest.moveTo("network");
                    roboTest.leftClick();
                    roboTest.moveTo("Apply");
                    roboTest.leftClick();
                    roboTest.checkVMTest(vmTest, 3, name);
                }
            }
            roboTest.checkVMTest(vmTest, 3, name);

            if (j  == 0 && !"lxc".equals(type)) {
                /* add disk */
                roboTest.moveToMenu("VMs (");
                roboTest.leftClick();
                roboTest.press(KeyEvent.VK_RIGHT);

                roboTest.moveToMenu("dmc");
                roboTest.rightClick();
                roboTest.moveTo("Add Hardware");
                roboTest.moveTo("New Disk");
                roboTest.leftClick();
                Tools.sleep(500);
                roboTest.moveTo("Disk/block device");
                roboTest.leftClick();
                roboTest.moveTo("Device", MComboBox.class);
                roboTest.leftClick();
                roboTest.press(KeyEvent.VK_SLASH);
                roboTest.press(KeyEvent.VK_D);
                roboTest.press(KeyEvent.VK_E);
                roboTest.press(KeyEvent.VK_V);

                roboTest.press(KeyEvent.VK_SLASH);
                roboTest.press(KeyEvent.VK_S);
                roboTest.press(KeyEvent.VK_D);
                roboTest.press(KeyEvent.VK_A);
                roboTest.press(KeyEvent.VK_1);
                roboTest.press(KeyEvent.VK_ENTER);
                roboTest.moveTo("Apply");
                roboTest.leftClick();
                roboTest.checkVMTest(vmTest, 3.01, name);
                /* remove disk */
                roboTest.moveToMenu("hdb (IDE");
                roboTest.rightClick();
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_ENTER); /* remove */
                roboTest.confirmRemove();
                roboTest.checkVMTest(vmTest, 3, name);

                /* add disk /virtio */
                roboTest.moveToMenu("dmc");
                roboTest.rightClick();
                roboTest.moveTo("Add Hardware");
                roboTest.moveTo("New Disk");
                roboTest.leftClick();
                Tools.sleep(500);
                roboTest.moveTo("Disk/block device");
                roboTest.leftClick();
                roboTest.moveTo("Device", MComboBox.class);
                roboTest.leftClick();
                roboTest.press(KeyEvent.VK_SLASH);
                roboTest.press(KeyEvent.VK_D);
                roboTest.press(KeyEvent.VK_E);
                roboTest.press(KeyEvent.VK_V);

                roboTest.press(KeyEvent.VK_SLASH);
                roboTest.press(KeyEvent.VK_S);
                roboTest.press(KeyEvent.VK_D);
                roboTest.press(KeyEvent.VK_A);
                roboTest.press(KeyEvent.VK_2);
                roboTest.press(KeyEvent.VK_ENTER);
                roboTest.moveTo("Disk Type", MComboBox.class);
                roboTest.leftClick();
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_ENTER); /* virtio */
                roboTest.moveTo("Apply");
                roboTest.leftClick();
                roboTest.checkVMTest(vmTest, 3.02, name);

                /* remove disk */
                roboTest.moveToMenu("vda (Virtio");
                roboTest.rightClick();
                roboTest.press(KeyEvent.VK_DOWN);
                roboTest.press(KeyEvent.VK_ENTER); /* remove */
                roboTest.confirmRemove();
                roboTest.checkVMTest(vmTest, 3, name);
            }

            if (!"lxc".equals(type)) {
                /* disk readonly */
                roboTest.moveToMenu("hda (IDE"); /* popup */
                roboTest.leftClick();
                roboTest.moveTo("Readonly", JCheckBox.class);
                roboTest.leftClick();
                roboTest.moveTo("Apply"); /* apply */
                roboTest.leftClick();
                roboTest.checkVMTest(vmTest, 3.1, name);
                guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
                roboTest.moveTo("Readonly", JCheckBox.class);
                roboTest.leftClick();

                roboTest.moveTo("VM Host Overview"); /* host overview */
                roboTest.leftClick();

                roboTest.moveTo("Apply"); /* host apply */
                roboTest.leftClick();
                roboTest.checkVMTest(vmTest, 3.2, name);
            }

            names.add(name);
            for (final String n : names) {
                roboTest.checkVMTest(vmTest, 3, n);
            }
            name += "i";
        }

        for (int j = 0; j < count; j++) {
            roboTest.moveToMenu("dmc");
            roboTest.rightClick();
            roboTest.moveTo("Remove Domain");
            roboTest.leftClick();
            roboTest.dialogColorTest("remove VM");
            roboTest.confirmRemove();
            roboTest.leftClick();
            Tools.sleep(500);
        }
    }
}
