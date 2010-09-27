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
import drbd.data.VMSXML;
import drbd.gui.resources.VMSVirtualDomainInfo;
import drbd.gui.resources.VMSDiskInfo;
import drbd.gui.dialog.WizardDialog;
import drbd.data.VMSXML.DiskData;

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
 */
public class Storage extends VMConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Configuration options of the new domain. */
    private static final String[] PARAMS = {DiskData.TYPE,
                                            DiskData.TARGET_BUS_TYPE,
                                            DiskData.SOURCE_FILE,
                                            DiskData.SOURCE_DEVICE};
    /** VMS disk info object. */
    private VMSDiskInfo vmsdi = null;

    /** Prepares a new <code>Storage</code> object. */
    public Storage(final WizardDialog previousDialog,
                   final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(previousDialog, vmsVirtualDomainInfo);
    }

    /** Next dialog. */
    public final WizardDialog nextDialog() {
        return new Network(this, getVMSVirtualDomainInfo());
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.vm.Domain.Title in TextResources.
     */
    protected final String getDialogTitle() {
        return Tools.getString("Dialog.vm.Storage.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.vm.Domain.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.vm.Storage.Description");
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
        if (vmsdi == null) {
            vmsdi = getVMSVirtualDomainInfo().addDiskPanel();
            vmsdi.waitForInfoPanel();
        } else {
            vmsdi.selectMyself();
        }
        vmsdi.addWizardParams(
                      optionsPanel,
                      PARAMS,
                      buttonClass(nextButton()),
                      Tools.getDefaultInt("Dialog.vm.Resource.LabelWidth"),
                      Tools.getDefaultInt("Dialog.vm.Resource.FieldWidth"),
                      null);
        vmsdi.paramComboBoxGet(DiskData.TARGET_BUS_TYPE, "wizard").setValue(
                                                                "virtio/disk");
        vmsdi.paramComboBoxGet(DiskData.SOURCE_FILE, "wizard").setValue(
                                     "/var/lib/libvirt/images/"
                                     +
                                     getVMSVirtualDomainInfo().getComboBoxValue(
                                                         VMSXML.VM_PARAM_NAME)
                                     + ".img");
        inputPane.add(optionsPanel);
        buttonClass(nextButton()).setEnabled(
                                      vmsdi.checkResourceFields(null, PARAMS));
        final JScrollPane sp = new JScrollPane(inputPane);
        sp.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        sp.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        return sp;
    }
}
