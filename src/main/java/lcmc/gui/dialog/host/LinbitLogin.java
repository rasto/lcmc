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
package lcmc.gui.dialog.host;

import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.Tools;
import lcmc.gui.SpringUtilities;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import lcmc.data.StringValue;

/**.
 * An implementation of a dialog where user can enter the name and password
 * for the linbit website.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class LinbitLogin extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Field with user name. */
    private Widget downloadUserField;
    /** Field with password. */
    private Widget downloadPasswordField;
    /** Checkbox to save the info. */
    private JCheckBox saveCheckBox;
    /** Width of the check boxes. */
    private static final int CHECKBOX_WIDTH = 120;

    /** Prepares a new <code>LinbitLogin</code> object. */
    public LinbitLogin(final WizardDialog previousDialog,
                       final Host host) {
        super(previousDialog, host);
    }

    /** Finishes the dialog and sets the information. */
    @Override
    protected final void finishDialog() {
        Tools.getConfigData().setDownloadLogin(
                                downloadUserField.getStringValue().trim(),
                                downloadPasswordField.getStringValue().trim(),
                                saveCheckBox.isSelected());
    }

    /** Returns the next dialog. */
    @Override
    public WizardDialog nextDialog() {
        return new DrbdLinbitInst(this, getHost());
    }

    /**
     * Check all fields if they are correct.
     * TODO: two checkfields?
     */
    protected final void checkFields() {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean v =
                    (downloadUserField.getStringValue().trim().length() > 0);
                v = v && (downloadPasswordField.getStringValue().trim().length()
                         > 0);
                buttonClass(nextButton()).setEnabled(v);
            }
        });
    }

    /** Check all fields if they are correct. */
    @Override
    protected final void checkFields(final Widget field) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean v =
                    (downloadUserField.getStringValue().trim().length() > 0);
                v = v && (downloadPasswordField.getStringValue().trim().length()
                         > 0);
                buttonClass(nextButton()).setEnabled(v);
            }
        });
    }

    /**
     * Returns the title of the dialog, defined as
     * Dialog.Host.LinbitLogin.Title in TextResources.
     */
    @Override
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.LinbitLogin.Title");
    }

    /**
     * Returns the description of the dialog, defined as
     * Dialog.Host.LinbitLogin.Description in TextResources.
     */
    @Override
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.LinbitLogin.Description");
    }

    /** Inits the dialog. */
    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        checkFields();
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                downloadUserField.requestFocus();
            }
        });
        if (Tools.getConfigData().getAutoOptionHost("drbdinst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /**
     * Returns the input pane, where user can enter the user name, password and
     * can select a check box to save the info for later.
     */
    @Override
    protected final JComponent getInputPane() {
        final JPanel p = new JPanel(new BorderLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Light"));

        /* user */
        final JLabel userLabel = new JLabel(
                      Tools.getString("Dialog.Host.LinbitLogin.EnterUser"));
        inputPane.add(userLabel);
        downloadUserField = WidgetFactory.createInstance(
                                       Widget.GUESS_TYPE,
                                       new StringValue(Tools.getConfigData().getDownloadUser()),
                                       Widget.NO_ITEMS,
                                       "^[,\\w.-]+$",
                                       CHECKBOX_WIDTH,
                                       Widget.NO_ABBRV,
                                       new AccessMode(ConfigData.AccessType.RO,
                                                      !AccessMode.ADVANCED),
                                       Widget.NO_BUTTON);

        addCheckField(downloadUserField);
        userLabel.setLabelFor(downloadUserField);
        inputPane.add(downloadUserField);

        /* password */
        final JLabel passwordLabel = new JLabel(
                  Tools.getString("Dialog.Host.LinbitLogin.EnterPassword"));

        inputPane.add(passwordLabel);
        downloadPasswordField = WidgetFactory.createInstance(
                                  Widget.Type.PASSWDFIELD,
                                  new StringValue(Tools.getConfigData().getDownloadPassword()),
                                  Widget.NO_ITEMS,
                                  Widget.NO_REGEXP,
                                  CHECKBOX_WIDTH,
                                  Widget.NO_ABBRV,
                                  new AccessMode(ConfigData.AccessType.RO,
                                                 !AccessMode.ADVANCED),
                                  Widget.NO_BUTTON);

        addCheckField(downloadPasswordField);
        passwordLabel.setLabelFor(downloadPasswordField);
        inputPane.add(downloadPasswordField);

        /* save */
        final JLabel saveLabel = new JLabel("");
        saveLabel.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Light"));

        inputPane.add(saveLabel);
        saveCheckBox = new JCheckBox(
                            Tools.getString("Dialog.Host.LinbitLogin.Save"),
                            Tools.getConfigData().getLoginSave());
        saveLabel.setLabelFor(saveCheckBox);
        saveCheckBox.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Light"));
        inputPane.add(saveCheckBox);

        SpringUtilities.makeCompactGrid(inputPane, 3, 2,  // rows, cols
                                                   1, 1,  // initX, initY
                                                   1, 1); // xPad, yPad

        p.add(inputPane, BorderLayout.SOUTH);
        return p;
    }
}
