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


package lcmc.vm.ui.configdialog;

import java.awt.Dimension;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lcmc.common.domain.Application;
import lcmc.vm.domain.VMParams;
import lcmc.common.ui.WizardDialog;
import lcmc.vm.ui.resource.DomainInfo;
import lcmc.cluster.ui.widget.Widget;
import lcmc.common.domain.util.Tools;

/**
 * An implementation of a dialog where user can enter a new domain.
 */
@Named
public final class Domain extends VMConfig {
    private static final String[] PARAMS = {VMParams.VM_PARAM_DOMAIN_TYPE,
                                            VMParams.VM_PARAM_NAME,
                                            VMParams.VM_PARAM_VIRSH_OPTIONS,
                                            VMParams.VM_PARAM_EMULATOR,
                                            VMParams.VM_PARAM_VCPU,
                                            VMParams.VM_PARAM_CURRENTMEMORY,
                                            VMParams.VM_PARAM_BOOT,
                                            VMParams.VM_PARAM_BOOT_2,
                                            VMParams.VM_PARAM_LOADER,
                                            VMParams.VM_PARAM_TYPE,
                                            VMParams.VM_PARAM_INIT,
                                            VMParams.VM_PARAM_TYPE_ARCH,
                                            VMParams.VM_PARAM_TYPE_MACHINE};
    private JComponent inputPane = null;
    private Widget domainNameWidget;
    private VMConfig nextDialogObject = null;
    @Inject
    private InstallationDisk installationDiskDialog;
    @Inject
    private Filesystem filesystemDialog;
    @Inject
    private Application application;

    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            if (getVMSVirtualDomainInfo().needFilesystem()) {
                nextDialogObject = filesystemDialog;
            } else {
                nextDialogObject = installationDiskDialog;
            }
            nextDialogObject.init(this, getVMSVirtualDomainInfo());
        }
        return nextDialogObject;
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.vm.Domain.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Domain.Description");
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        super.initDialogAfterVisible();
        final DomainInfo vdi = getVMSVirtualDomainInfo();
        final boolean cor = vdi.checkResourceFields(null, PARAMS).isCorrect();
        if (cor || nextDialogObject != null) {
            enableComponents();
        } else {
            /* don't enable */
            enableComponents(new JComponent[]{buttonClass(nextButton())});
        }
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                makeDefaultButton(buttonClass(nextButton()));
            }
        });
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                domainNameWidget.requestFocus();
            }
        });
    }

    @Override
    protected JComponent getInputPane() {
        final DomainInfo vdi = getVMSVirtualDomainInfo();
        vdi.waitForInfoPanel();
        if (inputPane != null) {
            return inputPane;
        }
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentY(java.awt.Component.TOP_ALIGNMENT);

        vdi.getResource().setValue(VMParams.VM_PARAM_BOOT, DomainInfo.BOOT_CDROM);
        vdi.savePreferredValues();
        vdi.addWizardParams(optionsPanel,
                            PARAMS,
                            buttonClass(nextButton()),
                            application.getDefaultSize("Dialog.vm.Resource.LabelWidth"),
                            application.getDefaultSize("Dialog.vm.Resource.FieldWidth"),
                            null);
        domainNameWidget = vdi.getWidget(VMParams.VM_PARAM_NAME, Widget.WIZARD_PREFIX);
        panel.add(optionsPanel);

        final JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        inputPane = scrollPane;
        return scrollPane;
    }
}
