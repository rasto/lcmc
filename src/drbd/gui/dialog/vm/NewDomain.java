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


package drbd.gui.dialog.vm;

import drbd.utilities.Tools;
import drbd.gui.resources.VMSVirtualDomainInfo;
import drbd.gui.dialog.WizardDialog;
import drbd.data.VMSXML;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
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
public class NewDomain extends VMConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Configuration options of the new domain. */
    private static final String[] PARAMS = {VMSXML.VM_PARAM_NAME,
                                            VMSXML.VM_PARAM_VCPU,
                                            VMSXML.VM_PARAM_CURRENTMEMORY,
                                            VMSXML.VM_PARAM_BOOT,
                                            VMSXML.VM_PARAM_ARCH,
                                            VMSXML.VM_PARAM_EMULATOR};

    /** Installation disk dialog object. */
    private NewInstallationDisk installationDisk = null;

    /** Prepares a new <code>NewDomain</code> object. */
    public NewDomain(final WizardDialog previousDialog,
                    final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(previousDialog, vmsVirtualDomainInfo);
    }

    /** Applies the changes and returns next dialog (BlockDev). */
    public final WizardDialog nextDialog() {
        if (installationDisk == null) {
            installationDisk =
                    new NewInstallationDisk(this, getVMSVirtualDomainInfo());
        }
        return installationDisk;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.VMConfig.Domain.Title in TextResources.
     */
    protected final String getDialogTitle() {
        return Tools.getString("Dialog.VMConfig.Domain.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.VMConfig.Domain.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.VMConfig.Domain.Description");
    }

    /** Inits dialog. */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        enableComponents();
    }

    /** Returns input pane where user can configure a vm. */
    protected final JComponent getInputPane() {
        final JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.X_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        getVMSVirtualDomainInfo().getResource().setValue(VMSXML.VM_PARAM_BOOT,
                                                         "cdrom");
        getVMSVirtualDomainInfo().addWizardParams(
                  optionsPanel,
                  PARAMS,
                  buttonClass(nextButton()),
                  Tools.getDefaultInt("Dialog.DrbdConfig.Resource.LabelWidth"),
                  Tools.getDefaultInt("Dialog.DrbdConfig.Resource.FieldWidth"),
                  null);

        inputPane.add(optionsPanel);

        buttonClass(nextButton()).setEnabled(
                  getVMSVirtualDomainInfo().checkResourceFields(null, PARAMS));
        final JScrollPane sp = new JScrollPane(inputPane);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        return sp;
    }
}
