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

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import java.util.List;

import drbd.gui.SpringUtilities;
import drbd.gui.dialog.ConfigDialog;
import drbd.gui.resources.Info;
import drbd.utilities.Tools;
import drbd.utilities.RemotePlugin;
import drbd.utilities.UpdatableItem;
/**
 * This class implements test plugin.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class Test_Plugin implements RemotePlugin {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Private. */
    public Test_Plugin() {
    }

    /** Inits the plugin. */
    public final void init() {
    }

    /** Adds menu items from the plugin. */
    public final void addPluginMenuItems(final Info info,
                                         final List<UpdatableItem> items) {
    }

    /** Shows dialog with description. */
    public final void showDescription() {
        final Description description = new Description();
        description.showDialog();
    }

    /** Description dialog. */
    private class Description extends ConfigDialog {
        /** Serial version UID. */
        private static final long serialVersionUID = 2L;

        public Description() {
            super();
        }

        protected final void initDialog() {
            super.initDialog();
            enableComponents();
        }

        protected final String getDialogTitle() {
            return "Test Plugin " + Tools.getRelease();
        }

        protected final String getDescription() {
            return "This is a test plugin to test plugin interface";
        }

        protected final JComponent getInputPane() {
            return null;
        }
    }
}
