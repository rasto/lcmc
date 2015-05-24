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

package lcmc.cluster.ui.wizard;

import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import lcmc.cluster.ui.EmptyBrowser;
import lcmc.common.ui.WizardDialog;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;

/**
 * Cluster finish dialog. Shows some text and let's the user press the finish
 * button.
 */
@Named
final class Finish extends DialogCluster {
    private final JCheckBox saveCheckBox = new JCheckBox(Tools.getString("Dialog.Cluster.Finish.Save"), true);
    @Inject
    private EmptyBrowser emptyBrowser;
    @Inject
    private Application application;

    @Override
    public WizardDialog nextDialog() {
        return null;
    }

    @Override
    protected void finishDialog() {
        emptyBrowser.addClusterBox(getCluster());
        if (saveCheckBox.isSelected()) {
            final String saveFile = application.getDefaultSaveFile();
            application.saveConfig(saveFile, false);
        }
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton()), buttonClass(finishButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents(new JComponent[]{buttonClass(nextButton())});
        if (!application.getAutoClusters().isEmpty()) {
            application.removeAutoCluster();
            Tools.sleep(1000);
            buttonClass(finishButton()).pressButton();
        }
    }

    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.Finish.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.Finish.Description");
    }

    @Override
    protected JPanel getInputPane() {
        final JPanel pane = new JPanel();
        pane.add(saveCheckBox);
        saveCheckBox.setBackground(Color.WHITE);
        return pane;
    }
}
