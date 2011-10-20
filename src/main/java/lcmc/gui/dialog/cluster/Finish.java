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

package lcmc.gui.dialog.cluster;

import lcmc.data.Cluster;
import lcmc.utilities.Tools;
import lcmc.gui.dialog.WizardDialog;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JCheckBox;

/**
 * Cluster finish dialog. Shows some text and let's the user press the finish
 * button.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class Finish extends DialogCluster {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Save checkbox. */
    private final JCheckBox saveCB = new JCheckBox(
                                  Tools.getString("Dialog.Cluster.Finish.Save"),
                                  true);

    /** Prepares a new <code>Finish</code> object. */
    Finish(final WizardDialog previousDialog, final Cluster cluster) {
        super(previousDialog, cluster);
    }

    /** Returns next dialog. Null in this case. */
    @Override public WizardDialog nextDialog() {
        return null;
    }

    /** Finishes the dialog, and saves the cluster. */
    @Override protected void finishDialog() {
        Tools.getGUIData().getEmptyBrowser().addClusterBox(getCluster());
        if (saveCB.isSelected()) {
            final String saveFile = Tools.getConfigData().getSaveFile();
            Tools.save(saveFile);
        }
    }

    /** Inits dialog and enables the finish button. */
    @Override protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton()),
                                               buttonClass(finishButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override protected void initDialogAfterVisible() {
        enableComponents(new JComponent[]{buttonClass(nextButton())});
        if (!Tools.getConfigData().getAutoClusters().isEmpty()) {
            Tools.getConfigData().removeAutoCluster();
            Tools.sleep(1000);
            buttonClass(finishButton()).pressButton();
        }
    }

    /** Returns the title of the dialog. */
    @Override protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.Finish.Title");
    }

    /** Returns the description of the dialog. */
    @Override protected String getDescription() {
        return Tools.getString("Dialog.Cluster.Finish.Description");
    }

    /** Returns the input panel. */
    @Override protected JPanel getInputPane() {
        final JPanel pane = new JPanel();
        /* Save checkbox */
        pane.add(saveCB);
        saveCB.setBackground(Color.WHITE);
        return pane;
    }
}
