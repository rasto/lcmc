/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package plugins;

import drbd.gui.SpringUtilities;
import drbd.gui.dialog.ConfigDialog;
import drbd.gui.resources.Info;
import drbd.gui.resources.BlockDevInfo;
import drbd.utilities.Tools;
import drbd.utilities.RemotePlugin;
import drbd.utilities.MyMenuItem;
import drbd.utilities.UpdatableItem;
import drbd.data.ConfigData;
import drbd.data.AccessMode;

import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.JMenu;
/**
 * This class implements LVM resize plugin.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class LVM_Resize implements RemotePlugin {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Private. */
    public LVM_Resize() {
    }

    /** Inits the plugin. */
    public final void init() {
        System.out.println("lvm resize init");
        for (final BlockDevInfo bdi
                        : Tools.getGUIData().getAllBlockDevices()) {
            registerInfo(bdi);
        }
    }

    private final void registerInfo(final Info info) {
        if (info instanceof BlockDevInfo) {
            final JMenu menu = info.getMenu();
            if (menu != null) {
                System.out.println("register plugin: " + info.toString());
                info.addPluginMenuItem(resizeLVMItem((BlockDevInfo) info));
                info.addPluginActionMenuItem(resizeLVMItem(
                                                     (BlockDevInfo) info));
            }
        }
    }

    /** Adds menu items from the plugin. */
    public final void addPluginMenuItems(final Info info,
                                         final List<UpdatableItem> items) {
        if (items != null && info instanceof BlockDevInfo) {
            items.add(resizeLVMItem((BlockDevInfo) info));
        }
    }

    private MyMenuItem resizeLVMItem(final BlockDevInfo bdi) {
        /* attach / detach */
        final MyMenuItem resizeMenu =
            new MyMenuItem("LVM resize",
                           null,
                           "LVM resize",
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, true)) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    return true;
                }

                public boolean visiblePredicate() {
                    return bdi.isLVM();
                }

                public String enablePredicate() {
                    return null;
                }

                public void action() {
                    System.out.println("resize");
                }
            };
        return resizeMenu;
    }

    /** Shows dialog with description. */
    public final void showDescription() {
        final ConfigDialog description = new ConfigDialog() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            protected final void initDialog() {
                super.initDialog();
                enableComponents();
            }

            protected final String getDialogTitle() {
                return "LVM Resize " + Tools.getRelease();
            }

            protected final String getDescription() {
                return "You can now use LVM Resize menu item in the "
                       + "\"Storage (DRBD)\" view.";
            }

            protected final JComponent getInputPane() {
                return null;
            }
        };
        description.showDialog();
    }
}
