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
import drbd.gui.ClusterBrowser.DrbdResourceInfo;
import drbd.utilities.MyButton;

import javax.swing.JPanel;
import javax.swing.JComponent;

import javax.swing.BoxLayout;
import java.awt.Component;
import java.util.Random;
import java.util.ArrayList;

/**
 * An implementation of a dialog where user can enter drbd resource
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class DrbdConfigResource extends DrbdConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Configuration options of the drbd resource. */
    private static final String[] PARAMS = {"name",
                                            "device",
                                            "protocol",
                                            "cram-hmac-alg",
                                            "shared-secret"};
    /** Length of the secret string. */
    private static final int SECRET_STRING_LENGTH = 32;

    /**
     * Prepares a new <code>DrbdConfigResource</code> object.
     */
    public DrbdConfigResource(final WizardDialog previousDialog,
                              final DrbdResourceInfo dri) {
        super(previousDialog, dri);
    }

    /**
     * Returns a string with SECRET_STRING_LENGTH random characters.
     */
    private String getRandomSecret() {
        final Random rand = new Random();
        final ArrayList<Character> charsL = new ArrayList<Character>();
        for (int a = 'a'; a <= 'z'; a++) {
            charsL.add((char) a);
            charsL.add(Character.toUpperCase((char) a));
        }
        for (int a = '0'; a <= '9'; a++) {
            charsL.add((char) a);
        }

        final Character[] chars = charsL.toArray(new Character[charsL.size()]);
        final StringBuffer s = new StringBuffer(32);
        for (int i = 0; i < SECRET_STRING_LENGTH; i++) {
            s.append(chars[rand.nextInt(chars.length)]);
        }
        return s.toString();
    }

    /**
     * Applies the changes and returns next dialog (DrbdConfigBlockDev).
     */
    public WizardDialog nextDialog() {
        getDrbdResourceInfo().apply();
        return new DrbdConfigBlockDev(
                                  this,
                                  getDrbdResourceInfo(),
                                  getDrbdResourceInfo().getFirstBlockDevInfo());
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.DrbdConfigResource.Title in TextResources.
     */
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfigResource.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.DrbdConfigResource.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfigResource.Description");
    }

    /**
     * Inits dialog.
     */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        enableComponents();
    }

    /**
     * Returns input pane where user can configure a drbd resource.
     */
    protected JComponent getInputPane() {
        final JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.X_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        final JPanel extraOptionsPanel = new JPanel();
        extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel,
                                                  BoxLayout.Y_AXIS));
        extraOptionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        getDrbdResourceInfo().getResource().setValue("cram-hmac-alg", "sha1");
        getDrbdResourceInfo().getResource().setValue("shared-secret",
                                                     getRandomSecret());

        getDrbdResourceInfo().addWizardParams(
                  optionsPanel,
                  extraOptionsPanel,
                  PARAMS,
                  (MyButton) buttonClass(nextButton()),
                  Tools.getDefaultInt("Dialog.DrbdConfigResource.LabelWidth"),
                  Tools.getDefaultInt("Dialog.DrbdConfigResource.FieldWidth")
                  );

        inputPane.add(optionsPanel);
        inputPane.add(extraOptionsPanel);

        ((MyButton) buttonClass(nextButton())).setEnabled(
                getDrbdResourceInfo().checkResourceFields(null, PARAMS));
        return inputPane;
    }
}
