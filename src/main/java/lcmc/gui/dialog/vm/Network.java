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

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import lcmc.model.StringValue;
import lcmc.model.vm.VmsXml.InterfaceData;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.resources.vms.DomainInfo;
import lcmc.gui.resources.vms.InterfaceInfo;
import lcmc.gui.widget.Widget;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog where user can enter a new domain.
 */
final class Network extends VMConfig {
    private static final String[] PARAMS = {InterfaceData.TYPE,
                                            InterfaceData.MAC_ADDRESS,
                                            InterfaceData.SOURCE_NETWORK,
                                            InterfaceData.SOURCE_BRIDGE,
                                            InterfaceData.SCRIPT_PATH,
                                            InterfaceData.MODEL_TYPE};
    private JComponent inputPane = null;
    private InterfaceInfo interfaceInfo = null;
    private WizardDialog nextDialogObject = null;

    Network(final WizardDialog previousDialog, final DomainInfo vmsVirtualDomainInfo) {
        super(previousDialog, vmsVirtualDomainInfo);
    }

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

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.vm.Network.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Network.Description");
    }

    @Override
    protected void initDialogBeforeCreated() {
        if (interfaceInfo == null) {
            interfaceInfo = getVMSVirtualDomainInfo().addInterfacePanel();
            interfaceInfo.waitForInfoPanel();
        } else {
            interfaceInfo.selectMyself();
        }
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                final boolean enable = interfaceInfo.checkResourceFields(null, interfaceInfo.getRealParametersFromXML())
                                                    .isCorrect();
                buttonClass(nextButton()).setEnabled(enable);
            }
        });
    }

    @Override
    protected JComponent getInputPane() {
        if (inputPane != null) {
            return inputPane;
        }
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        interfaceInfo.savePreferredValues();
        interfaceInfo.getResource().setValue(InterfaceData.TYPE, InterfaceInfo.TYPE_NETWORK);
        interfaceInfo.getResource().setValue(InterfaceData.SOURCE_NETWORK, new StringValue("default"));
        interfaceInfo.getResource().setValue(InterfaceData.MODEL_TYPE, new StringValue());
        interfaceInfo.addWizardParams(optionsPanel,
                                      PARAMS,
                                      buttonClass(nextButton()),
                                      Tools.getDefaultSize("Dialog.vm.Resource.LabelWidth"),
                                      Tools.getDefaultSize("Dialog.vm.Resource.FieldWidth"),
                                      null);
        interfaceInfo.getWidget(InterfaceData.MODEL_TYPE, Widget.WIZARD_PREFIX).setValue(new StringValue());

        panel.add(optionsPanel);

        final JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        inputPane = scrollPane;
        return scrollPane;
    }
}
