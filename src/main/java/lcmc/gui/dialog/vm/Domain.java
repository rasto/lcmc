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
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.widget.Widget;
import lcmc.data.VMSXML;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;

import java.awt.Component;
import java.awt.Dimension;

/**
 * An implementation of a dialog where user can enter a new domain.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Domain extends VMConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Input pane cache for back button. */
    private JComponent inputPane = null;
    private Widget domainNameWi;
    /** Configuration options of the new domain. */
    private static final String[] PARAMS = {VMSXML.VM_PARAM_DOMAIN_TYPE,
                                            VMSXML.VM_PARAM_NAME,
                                            VMSXML.VM_PARAM_VIRSH_OPTIONS,
                                            VMSXML.VM_PARAM_EMULATOR,
                                            VMSXML.VM_PARAM_VCPU,
                                            VMSXML.VM_PARAM_CURRENTMEMORY,
                                            VMSXML.VM_PARAM_BOOT,
                                            VMSXML.VM_PARAM_BOOT_2,
                                            VMSXML.VM_PARAM_LOADER,
                                            VMSXML.VM_PARAM_TYPE,
                                            VMSXML.VM_PARAM_INIT,
                                            VMSXML.VM_PARAM_TYPE_ARCH,
                                            VMSXML.VM_PARAM_TYPE_MACHINE};
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /** Prepares a new <code>Domain</code> object. */
    public Domain(final WizardDialog previousDialog,
                  final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(previousDialog, vmsVirtualDomainInfo);
    }

    /** Next dialog. */
    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            if (getVMSVirtualDomainInfo().needFilesystem()) {
                nextDialogObject =
                        new Filesystem(this, getVMSVirtualDomainInfo());
            } else {
                nextDialogObject =
                        new InstallationDisk(this, getVMSVirtualDomainInfo());
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
        return Tools.getString("Dialog.vm.Domain.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.vm.Domain.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Domain.Description");
    }

    /** Inits dialog. */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog. */
    @Override
    protected void initDialogAfterVisible() {
        super.initDialogAfterVisible();
        final VMSVirtualDomainInfo vdi = getVMSVirtualDomainInfo();
        final boolean ch = vdi.checkResourceFieldsChanged(null, PARAMS);
        final boolean cor = vdi.checkResourceFieldsCorrect(null, PARAMS);
        if (cor || nextDialogObject != null) {
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
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                domainNameWi.requestFocus();
            }
        });
    }

    /** Returns input pane where user can configure a vm. */
    @Override
    protected JComponent getInputPane() {
        final VMSVirtualDomainInfo vdi = getVMSVirtualDomainInfo();
        vdi.waitForInfoPanel();
        if (inputPane != null) {
            return inputPane;
        }
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        vdi.getResource().setValue(VMSXML.VM_PARAM_BOOT, "CD-ROM");
        vdi.savePreferredValues();
        Tools.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                vdi.addWizardParams(
                          optionsPanel,
                          PARAMS,
                          buttonClass(nextButton()),
                          Tools.getDefaultSize("Dialog.vm.Resource.LabelWidth"),
                          Tools.getDefaultSize("Dialog.vm.Resource.FieldWidth"),
                          null);
            }
        });
        domainNameWi = vdi.getWidget(VMSXML.VM_PARAM_NAME,
                                     Widget.WIZARD_PREFIX);
        panel.add(optionsPanel);

        final JScrollPane sp = new JScrollPane(panel);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        inputPane = sp;
        return sp;
    }
}
