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

import lcmc.utilities.Tools;
import lcmc.gui.resources.DrbdInfo;
import lcmc.gui.resources.DrbdResourceInfo;
import lcmc.gui.resources.DrbdVolumeInfo;
import lcmc.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;

import java.awt.Component;
import java.awt.Dimension;

/**
 * An implementation of a dialog where user can enter drbd volume
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Volume extends DrbdConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Configuration options of the drbd volume. */
    private static final String[] PARAMS = {"number", "device"};
    /** Prepares a new <code>Volume</code> object. */
    public Volume(final WizardDialog previousDialog,
                  final DrbdVolumeInfo dvi) {
        super(previousDialog, dvi);
    }

    /** Applies the changes and returns next dialog (BlockDev). */
    @Override
    public WizardDialog nextDialog() {
        Tools.waitForSwing();
        getDrbdVolumeInfo().apply(false);
        return new BlockDev(this,
                            getDrbdVolumeInfo(),
                            getDrbdVolumeInfo().getFirstBlockDevInfo());
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.DrbdConfig.Volume.Title in TextResources.
     */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.Volume.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.DrbdConfig.Volume.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.Volume.Description");
    }

    @Override
    protected void initDialogBeforeCreated() {
        final DrbdResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        getDrbdVolumeInfo().waitForInfoPanel();
    }

    /** Inits dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        final boolean cor =
            getDrbdVolumeInfo().checkResourceFields(null, PARAMS).isCorrect();
        if (cor) {
            enableComponents();
        } else {
            /* don't enable */
            enableComponents(new JComponent[]{buttonClass(nextButton())});
        }
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                makeDefaultButton(buttonClass(nextButton()));
            }
        });
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            pressNextButton();
        }
    }

    /** Returns input pane where user can configure a drbd volume. */
    @Override
    protected JComponent getInputPane() {
        final DrbdResourceInfo dri = getDrbdVolumeInfo().getDrbdResourceInfo();
        final DrbdInfo drbdInfo = dri.getDrbdInfo();
        final JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.X_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        getDrbdVolumeInfo().addWizardParams(
                  optionsPanel,
                  PARAMS,
                  buttonClass(nextButton()),
                  Tools.getDefaultSize("Dialog.DrbdConfig.Resource.LabelWidth"),
                  Tools.getDefaultSize("Dialog.DrbdConfig.Resource.FieldWidth"),
                  null);

        inputPane.add(optionsPanel);
        final JScrollPane sp = new JScrollPane(inputPane);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        return sp;
    }
}
