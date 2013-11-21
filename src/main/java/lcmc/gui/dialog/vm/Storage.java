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
import lcmc.data.VMSXML;
import lcmc.gui.resources.VMSVirtualDomainInfo;
import lcmc.gui.resources.VMSDiskInfo;
import lcmc.gui.dialog.WizardDialog;
import lcmc.data.VMSXML.DiskData;

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

                                            DiskData.SOURCE_PROTOCOL,
                                            DiskData.SOURCE_NAME,
                                            DiskData.SOURCE_HOST_NAME,
                                            DiskData.SOURCE_HOST_PORT,

                                            DiskData.AUTH_USERNAME,
                                            DiskData.AUTH_SECRET_TYPE,
                                            DiskData.AUTH_SECRET_UUID,

                                            DiskData.DRIVER_NAME,
                                            DiskData.DRIVER_TYPE,
                                            DiskData.DRIVER_CACHE};
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

    @Override
    protected void initDialogBeforeCreated() {
        if (vmsdi == null) {
            vmsdi = getVMSVirtualDomainInfo().addDiskPanel();
            vmsdi.waitForInfoPanel();
        } else {
            vmsdi.selectMyself();
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
        if (inputPane != null) {
            return inputPane;
        }
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        vmsdi.savePreferredValues();
        vmsdi.getResource().setValue(DiskData.TYPE, new StringValue("file"));
        vmsdi.getResource().setValue(DiskData.TARGET_BUS_TYPE, new StringValue("IDE Disk"));
        vmsdi.getResource().setValue(DiskData.TARGET_DEVICE, new StringValue("hda"));
        vmsdi.getResource().setValue(DiskData.DRIVER_TYPE, new StringValue("raw"));
        vmsdi.getResource().setValue(DiskData.DRIVER_CACHE, new StringValue("default"));
        if ("xen".equals(getVMSVirtualDomainInfo().getWidget(
                        VMSXML.VM_PARAM_DOMAIN_TYPE, null).getStringValue())) {
            vmsdi.getResource().setValue(DiskData.DRIVER_NAME, new StringValue("file"));
        } else {
            vmsdi.getResource().setValue(DiskData.DRIVER_NAME, new StringValue("qemu"));
        }
        vmsdi.getResource().setValue(
                     DiskData.SOURCE_FILE,
                     new StringValue("/var/lib/libvirt/images/"
                                     +
                                     getVMSVirtualDomainInfo().getComboBoxValue(
                                                         VMSXML.VM_PARAM_NAME)
                                     + ".img"));
        vmsdi.addWizardParams(
                      optionsPanel,
                      PARAMS,
                      buttonClass(nextButton()),
                      Tools.getDefaultSize("Dialog.vm.Resource.LabelWidth"),
                      Tools.getDefaultSize("Dialog.vm.Resource.FieldWidth"),
                      null);
        panel.add(optionsPanel);
        final JScrollPane sp = new JScrollPane(panel);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        inputPane = sp;
        return sp;
    }
}
