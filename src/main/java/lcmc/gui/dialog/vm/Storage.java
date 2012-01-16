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


package lcmc.gui.dialog.vm;

import lcmc.utilities.Tools;
import lcmc.data.VMSXML;
import lcmc.gui.resources.VMSVirtualDomainInfo;
import lcmc.gui.resources.VMSDiskInfo;
import lcmc.gui.dialog.WizardDialog;
import lcmc.data.VMSXML.DiskData;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Dimension;

/**
 * An implementation of a dialog where user can enter a new domain.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
final class Storage extends VMConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Input pane cache for back button. */
    private JComponent inputPane = null;
    /** Configuration options of the new domain. */
    private static final String[] PARAMS = {DiskData.TYPE,
                                            DiskData.TARGET_BUS_TYPE,
                                            DiskData.SOURCE_FILE,
                                            DiskData.SOURCE_DEVICE,
                                            DiskData.DRIVER_NAME,
                                            DiskData.DRIVER_TYPE};
    /** VMS disk info object. */
    private VMSDiskInfo vmsdi = null;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /** Prepares a new <code>Storage</code> object. */
    Storage(final WizardDialog previousDialog,
            final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(previousDialog, vmsVirtualDomainInfo);
    }

    /** Next dialog. */
    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            nextDialogObject = new Network(this, getVMSVirtualDomainInfo());
        }
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.vm.Domain.Title in TextResources.
     */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.vm.Storage.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.vm.Domain.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Storage.Description");
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
        enableComponents();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final boolean enable = vmsdi.checkResourceFieldsCorrect(
                                            null,
                                            vmsdi.getRealParametersFromXML());
                buttonClass(nextButton()).setEnabled(enable);
            }
        });
    }

    /** Returns input pane where user can configure a vm. */
    @Override
    protected JComponent getInputPane() {
        if (vmsdi != null) {
            vmsdi.selectMyself();
        }
        if (inputPane != null) {
            return inputPane;
        }
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        if (vmsdi == null) {
            vmsdi = getVMSVirtualDomainInfo().addDiskPanel();
        }
        vmsdi.waitForInfoPanel();
        vmsdi.savePreferredValues();
        vmsdi.getResource().setValue(DiskData.TYPE, "file");
        vmsdi.getResource().setValue(DiskData.TARGET_BUS_TYPE, "IDE Disk");
        vmsdi.getResource().setValue(DiskData.TARGET_DEVICE, "hda");
        vmsdi.getResource().setValue(DiskData.DRIVER_TYPE, "raw");
        if ("xen".equals(getVMSVirtualDomainInfo().paramComboBoxGet(
                        VMSXML.VM_PARAM_DOMAIN_TYPE, null).getStringValue())) {
            vmsdi.getResource().setValue(DiskData.DRIVER_NAME, "file");
        } else {
            vmsdi.getResource().setValue(DiskData.DRIVER_NAME, "qemu");
        }
        vmsdi.getResource().setValue(DiskData.SOURCE_FILE,
                                     "/var/lib/libvirt/images/"
                                     +
                                     getVMSVirtualDomainInfo().getComboBoxValue(
                                                         VMSXML.VM_PARAM_NAME)
                                     + ".img");
        vmsdi.addWizardParams(
                      optionsPanel,
                      PARAMS,
                      buttonClass(nextButton()),
                      Tools.getDefaultInt("Dialog.vm.Resource.LabelWidth"),
                      Tools.getDefaultInt("Dialog.vm.Resource.FieldWidth"),
                      null);
        panel.add(optionsPanel);
        final JScrollPane sp = new JScrollPane(panel);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        inputPane = sp;
        return sp;
    }
}
