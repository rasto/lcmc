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

package lcmc.gui.dialog;

import lcmc.utilities.Tools;
import lcmc.gui.SpringUtilities;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

/**
 * An implementation of an About dialog.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class About extends ConfigDialog {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Inits the dialog and enables all the components. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponents();
    }

    /** Gets the title of the dialog as string. */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.About.Title") + Tools.getRelease();
    }

    /**
     * Returns description for dialog. This can be HTML defined in
     * TextResource.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.About.Description");
    }

    /** Returns the content of the about dialog. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        final JScrollPane sp =
                      getAnswerPane(Tools.getString("Dialog.About.Licences"));

        pane.add(sp);
        //Tools.invokeLater(new Runnable() {
        //    @Override
        //    public void run() {
        //        sp.getVerticalScrollBar().setValue(0);
        //    }
        //});
        SpringUtilities.makeCompactGrid(pane, 1, 1,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad
        return pane;
    }
}
