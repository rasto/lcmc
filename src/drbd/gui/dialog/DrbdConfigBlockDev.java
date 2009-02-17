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


package drbd.gui.dialog;

import drbd.utilities.Tools;
import drbd.utilities.DRBD;
import drbd.gui.ClusterBrowser.DrbdResourceInfo;
import drbd.gui.HostBrowser.BlockDevInfo;
import drbd.utilities.MyButton;
import drbd.Exceptions;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComponent;

import javax.swing.BoxLayout;
import java.awt.Component;

/**
 * An implementation of a dialog where user can enter drbd block device
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class DrbdConfigBlockDev extends DrbdConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** This block device. */
    private final BlockDevInfo blockDevInfo;
    /** Return code of adjust command that says 'no metadata'. */
    private static final int DRBD_NO_METADATA_RC = 119;

    /**
     * Prepares a new <code>DrbdConfigBlockDev</code> object.
     */
    public DrbdConfigBlockDev(final WizardDialog previousDialog,
                              final DrbdResourceInfo dri,
                              final BlockDevInfo blockDevInfo) {
        super(previousDialog, dri);
        this.blockDevInfo = blockDevInfo;
        dri.getDrbdInfo().setSelectedNode(blockDevInfo);
        dri.getDrbdInfo().selectMyself();
    }

    /**
     * Applies the changes to the blockDevInfo object.
     */
    protected void finishDialog() {
        blockDevInfo.apply();
    }

    /**
     * Calls drbd adjust, returns false if there is no meta-data area.
     */
    private boolean adjust(final BlockDevInfo bdi) {
        final int err = DRBD.adjust(bdi.getHost(),
                                    bdi.getDrbdResourceInfo().getName());
        if (err == DRBD_NO_METADATA_RC) {
            return false;
        }
        return true;
    }

    /**
     * Sets next dialog and returns it. It is either second block device, or
     * drbd config create md dialog. In the second case the drbd admin adjust
     * is called.
     */
    public WizardDialog nextDialog() {
        if (getDrbdResourceInfo().isFirstBlockDevInfo(blockDevInfo)) {
            final BlockDevInfo oBdi =
                    getDrbdResourceInfo().getOtherBlockDevInfo(blockDevInfo);
            return new DrbdConfigBlockDev(this, getDrbdResourceInfo(), oBdi);
        } else {
            final BlockDevInfo oBdi =
                    getDrbdResourceInfo().getOtherBlockDevInfo(blockDevInfo);
            try {
                // TODO: check this
                getDrbdResourceInfo().getDrbdInfo().createDrbdConfig();
                if (adjust(blockDevInfo) && adjust(oBdi)) {
                    DRBD.down(blockDevInfo.getHost(),
                              getDrbdResourceInfo().getName());
                    DRBD.down(oBdi.getHost(),
                              getDrbdResourceInfo().getName());
                } else {
                    getDrbdResourceInfo().setHaveToCreateMD(true);
                }
            } catch (Exceptions.DrbdConfigException dce) {
                Tools.appError("config failed");
            }
            return new DrbdConfigCreateMD(this, getDrbdResourceInfo());
        }
    }

    /**
     * Returns title of the dialog.
     */
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfigBlockDev.Title");
    }

    /**
     * Returns description of the dialog.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfigBlockDev.Description");
    }

    /**
     * Inits the dialog.
     */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        enableComponents();

        final String[] params = blockDevInfo.getParametersFromXML();
        ((MyButton) buttonClass(nextButton())).setEnabled(
                    blockDevInfo.checkResourceFieldsCorrect(null, params));
    }

    /**
     * Returns the input pane with block device parameters.
     */
    protected JComponent getInputPane() {
        final JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.Y_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JPanel extraOptionsPanel = new JPanel();
        extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel,
                                                  BoxLayout.Y_AXIS));
        extraOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final String[] params = blockDevInfo.getParametersFromXML();
        blockDevInfo.addWizardParams(
                 optionsPanel,
                 extraOptionsPanel,
                 params,
                 (MyButton) buttonClass(nextButton()),
                 Tools.getDefaultInt("Dialog.DrbdConfigBlockDev.LabelWidth"),
                 Tools.getDefaultInt("Dialog.DrbdConfigBlockDev.FieldWidth"));

        inputPane.add(new JLabel(blockDevInfo.getHost().getName()));

        inputPane.add(optionsPanel);
        inputPane.add(extraOptionsPanel);

        return inputPane;
    }
}
