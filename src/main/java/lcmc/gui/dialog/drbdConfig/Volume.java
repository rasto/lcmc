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


package lcmc.gui.dialog.drbdConfig;

import java.awt.Dimension;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import lcmc.gui.dialog.WizardDialog;
import lcmc.model.Application;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog where user can enter drbd volume
 * information.
 */
@Named
public final class Volume extends DrbdConfig {

    /** Configuration options of the drbd volume. */
    private static final String[] PARAMS = {"number", "device"};
    @Inject
    private BlockDev blockDevDialog;
    @Inject
    private Application application;

    /** Applies the changes and returns next dialog (BlockDev). */
    @Override
    public WizardDialog nextDialog() {
        application.waitForSwing();
        blockDevDialog.init(this, getDrbdVolumeInfo(), getDrbdVolumeInfo().getFirstBlockDevInfo());
        return blockDevDialog;
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.Volume.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.Volume.Description");
    }

    @Override
    protected void initDialogBeforeCreated() {
        getDrbdVolumeInfo().waitForInfoPanel();
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        final boolean correct = getDrbdVolumeInfo().checkResourceFields(null, PARAMS).isCorrect();
        if (correct) {
            enableComponents();
        } else {
            /* don't enable */
            enableComponents(new JComponent[]{buttonClass(nextButton())});
        }
        enableComponents();
        if (application.getAutoOptionGlobal("autodrbd") != null) {
            pressNextButton();
        }
    }

    /** Returns input pane where user can configure a drbd volume. */
    @Override
    protected JComponent getInputPane() {
        final JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.LINE_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentY(java.awt.Component.TOP_ALIGNMENT);

        getDrbdVolumeInfo().addWizardParams(optionsPanel,
                                            PARAMS,
                                            buttonClass(nextButton()),
                                            application.getDefaultSize("Dialog.DrbdConfig.Resource.LabelWidth"),
                                            application.getDefaultSize("Dialog.DrbdConfig.Resource.FieldWidth"),
                                            null);

        inputPane.add(optionsPanel);
        final JScrollPane sp = new JScrollPane(inputPane);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        return sp;
    }
}
