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

import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.StringValue;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**.
 * An implementation of a dialog where user can enter the name and password
 * for the linbit website.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LinbitLogin extends DialogHost {
    private static final int CHECKBOX_WIDTH = 120;
    private Widget downloadUserField;
    private Widget downloadPasswordField;
    private JCheckBox saveCheckBox;
    @Autowired
    private DrbdLinbitInst drbdLinbitInst;
    @Autowired
    private Application application;
    @Autowired
    private WidgetFactory widgetFactory;

    @Override
    protected final void finishDialog() {
        application.setDownloadLogin(downloadUserField.getStringValue().trim(),
                                     downloadPasswordField.getStringValue().trim(),
                                     saveCheckBox.isSelected());
    }

    @Override
    public WizardDialog nextDialog() {
        drbdLinbitInst.init(this, getHost(), getDrbdInstallation());
        return drbdLinbitInst;
    }

    /**
     * Check all fields if they are correct.
     * TODO: two checkfields?
     */
    protected final void checkFields() {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean v = (!downloadUserField.getStringValue().trim().isEmpty());
                v = v && (!downloadPasswordField.getStringValue().trim().isEmpty());
                buttonClass(nextButton()).setEnabled(v);
            }
        });
    }

    @Override
    protected final void checkFields(final Widget field) {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean v = (!downloadUserField.getStringValue().trim().isEmpty());
                v = v && (!downloadPasswordField.getStringValue().trim().isEmpty());
                buttonClass(nextButton()).setEnabled(v);
            }
        });
    }

    @Override
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.LinbitLogin.Title");
    }

    @Override
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.LinbitLogin.Description");
    }

    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        checkFields();
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                downloadUserField.requestFocus();
            }
        });
        if (application.getAutoOptionHost("drbdinst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    @Override
    protected final JComponent getInputPane() {
        final JPanel p = new JPanel(new BorderLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));

        /* user */
        final JLabel userLabel = new JLabel(Tools.getString("Dialog.Host.LinbitLogin.EnterUser"));
        inputPane.add(userLabel);
        downloadUserField = widgetFactory.createInstance(
                                       Widget.GUESS_TYPE,
                                       new StringValue(application.getDownloadUser()),
                                       Widget.NO_ITEMS,
                                       "^[,\\w.-]+$",
                                       CHECKBOX_WIDTH,
                                       Widget.NO_ABBRV,
                                       new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                                       Widget.NO_BUTTON);

        addCheckField(downloadUserField);
        userLabel.setLabelFor(downloadUserField.getComponent());
        inputPane.add(downloadUserField.getComponent());

        /* password */
        final JLabel passwordLabel = new JLabel(Tools.getString("Dialog.Host.LinbitLogin.EnterPassword"));

        inputPane.add(passwordLabel);
        downloadPasswordField = widgetFactory.createInstance(
                                  Widget.Type.PASSWDFIELD,
                                  new StringValue(application.getDownloadPassword()),
                                  Widget.NO_ITEMS,
                                  Widget.NO_REGEXP,
                                  CHECKBOX_WIDTH,
                                  Widget.NO_ABBRV,
                                  new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                                  Widget.NO_BUTTON);

        addCheckField(downloadPasswordField);
        passwordLabel.setLabelFor(downloadPasswordField.getComponent());
        inputPane.add(downloadPasswordField.getComponent());

        /* save */
        final JLabel saveLabel = new JLabel("");
        saveLabel.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));

        inputPane.add(saveLabel);
        saveCheckBox = new JCheckBox(Tools.getString("Dialog.Host.LinbitLogin.Save"), application.getLoginSave());
        saveLabel.setLabelFor(saveCheckBox);
        saveCheckBox.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));
        inputPane.add(saveCheckBox);

        SpringUtilities.makeCompactGrid(inputPane, 3, 2,  // rows, cols
                                                   1, 1,  // initX, initY
                                                   1, 1); // xPad, yPad

        p.add(inputPane, BorderLayout.PAGE_END);
        return p;
    }
}
