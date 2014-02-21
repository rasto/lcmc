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


package lcmc.gui.dialog.vm;

import lcmc.utilities.Tools;
import lcmc.gui.resources.VMSVirtualDomainInfo;
import lcmc.gui.resources.VMSInterfaceInfo;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.widget.Widget;
import lcmc.data.VMSXML.InterfaceData;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;

import java.awt.Component;
import java.awt.Dimension;
import lcmc.data.StringValue;

/**
 * An implementation of a dialog where user can enter a new domain.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
final class Network extends VMConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Input pane cache for back button. */
    private JComponent inputPane = null;
    /** Configuration options of the new domain. */
    private static final String[] PARAMS = {InterfaceData.TYPE,
                                            InterfaceData.MAC_ADDRESS,
                                            InterfaceData.SOURCE_NETWORK,
                                            InterfaceData.SOURCE_BRIDGE,
                                            InterfaceData.SCRIPT_PATH,
                                            InterfaceData.MODEL_TYPE};
    /** VMS interface info object. */
    private VMSInterfaceInfo vmsii = null;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /** Prepares a new <code>Network</code> object. */
    Network(final WizardDialog previousDialog,
            final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(previousDialog, vmsVirtualDomainInfo);
    }

    /** Next dialog. */
    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            if (getVMSVirtualDomainInfo().needDisplay()) {
                nextDialogObject = new Display(this, getVMSVirtualDomainInfo());
            } else {
                nextDialogObject = new Finish(this, getVMSVirtualDomainInfo());
            }

        }
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.vm.Domain.Title in TextResources.
     */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.vm.Network.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.vm.Domain.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Network.Description");
    }

    @Override
    protected void initDialogBeforeCreated() {
        if (vmsii == null) {
            vmsii = getVMSVirtualDomainInfo().addInterfacePanel();
            vmsii.waitForInfoPanel();
        } else {
            vmsii.selectMyself();
        }
    }

    /** Inits dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                final boolean enable = vmsii.checkResourceFields(
                                              null,
                                              vmsii.getRealParametersFromXML())
                                            .isCorrect();
                buttonClass(nextButton()).setEnabled(enable);
            }
        });
    }

    /** Returns input pane where user can configure a vm. */
    @Override
    protected JComponent getInputPane() {
        if (inputPane != null) {
            return inputPane;
        }
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        vmsii.savePreferredValues();
        vmsii.getResource().setValue(InterfaceData.TYPE,
                                     VMSInterfaceInfo.TYPE_NETWORK);
        vmsii.getResource().setValue(InterfaceData.SOURCE_NETWORK, new StringValue("default"));
        vmsii.getResource().setValue(InterfaceData.MODEL_TYPE, new StringValue());
        vmsii.addWizardParams(
                      optionsPanel,
                      PARAMS,
                      buttonClass(nextButton()),
                      Tools.getDefaultSize("Dialog.vm.Resource.LabelWidth"),
                      Tools.getDefaultSize("Dialog.vm.Resource.FieldWidth"),
                      null);
        vmsii.getWidget(InterfaceData.MODEL_TYPE,
                        Widget.WIZARD_PREFIX).setValue(new StringValue());

        panel.add(optionsPanel);

        final JScrollPane sp = new JScrollPane(panel);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        inputPane = sp;
        return sp;
    }
}
