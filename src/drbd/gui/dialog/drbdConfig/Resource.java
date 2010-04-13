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


package drbd.gui.dialog.drbdConfig;

import drbd.utilities.Tools;
import drbd.gui.resources.DrbdResourceInfo;
import drbd.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;

import java.awt.Component;
import java.awt.Dimension;

/**
 * An implementation of a dialog where user can enter drbd resource
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Resource extends DrbdConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Configuration options of the drbd resource. */
    private static final String[] PARAMS = {"name",
                                            "device",
                                            "protocol",
                                            "cram-hmac-alg",
                                            "shared-secret",
                                            "on-io-error"};
    /** Length of the secret string. */
    private static final int SECRET_STRING_LENGTH = 32;

    /**
     * Prepares a new <code>Resource</code> object.
     */
    public Resource(final WizardDialog previousDialog,
                    final DrbdResourceInfo dri) {
        super(previousDialog, dri);
    }

    /**
     * Returns a string with SECRET_STRING_LENGTH random characters.
     */
    private String getRandomSecret() {
        return Tools.getRandomSecret(SECRET_STRING_LENGTH);
    }

    /**
     * Applies the changes and returns next dialog (BlockDev).
     */
    public final WizardDialog nextDialog() {
        getDrbdResourceInfo().apply(false);
        return new BlockDev(this,
                            getDrbdResourceInfo(),
                            getDrbdResourceInfo().getFirstBlockDevInfo());
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.DrbdConfig.Resource.Title in TextResources.
     */
    protected final String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.Resource.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.DrbdConfig.Resource.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.Resource.Description");
    }

    /**
     * Inits dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        enableComponents();
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    pressNextButton();
                }
            });
        }
    }

    /**
     * Returns input pane where user can configure a drbd resource.
     */
    protected final JComponent getInputPane() {
        final JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.X_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        getDrbdResourceInfo().getResource().setValue("cram-hmac-alg", "sha1");
        getDrbdResourceInfo().getResource().setValue("shared-secret",
                                                     getRandomSecret());
        getDrbdResourceInfo().getResource().setValue("on-io-error", "detach");

        getDrbdResourceInfo().addWizardParams(
                  optionsPanel,
                  PARAMS,
                  buttonClass(nextButton()),
                  Tools.getDefaultInt("Dialog.DrbdConfig.Resource.LabelWidth"),
                  Tools.getDefaultInt("Dialog.DrbdConfig.Resource.FieldWidth"),
                  null);

        inputPane.add(optionsPanel);

        buttonClass(nextButton()).setEnabled(
                     getDrbdResourceInfo().checkResourceFields(null, PARAMS));
        final JScrollPane sp = new JScrollPane(inputPane);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        return sp;
    }
}
