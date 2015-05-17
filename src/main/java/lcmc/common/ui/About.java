/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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

package lcmc.common.ui;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;

import lcmc.common.domain.util.Tools;

/**
 * An implementation of an About dialog.
 */
@Named
@Singleton
public final class About extends ConfigDialog {
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponents();
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.About.Title") + Tools.getRelease();
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.About.Description");
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        final JScrollPane sp = getAnswerPane(Tools.getString("Dialog.About.Licences"));

        pane.add(sp);
        SpringUtilities.makeCompactGrid(pane, 1, 1,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad
        return pane;
    }
}
