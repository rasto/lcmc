/*
 * This file is part of Linux Cluster Management Console (LCMC)
 * by Rasto Levrinc.
 *
 * Copyright (C) 2012, Rasto Levrinc
 *
 * LCMC is free software; you can redistribute it and/or
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
import lcmc.data.vm.VMSXML.FilesystemData;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.resources.vms.DomainInfo;
import lcmc.gui.resources.vms.FilesystemInfo;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog where user can enter a new domain.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
final class Filesystem extends VMConfig {
    /** Configuration options of the new domain. */
    private static final String[] PARAMS = {FilesystemData.TYPE,
                                            FilesystemData.SOURCE_DIR,
                                            FilesystemData.SOURCE_NAME,
                                            FilesystemData.TARGET_DIR};
    /** Input pane cache for back button. */
    private JComponent inputPane = null;
    /** VMS filesystem info object. */
    private FilesystemInfo vmsfi = null;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /** Prepares a new {@code Filesystem} object. */
    Filesystem(final WizardDialog previousDialog,
               final DomainInfo vmsVirtualDomainInfo) {
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
        return Tools.getString("Dialog.vm.Filesystem.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.vm.Domain.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Filesystem.Description");
    }

    @Override
    protected void initDialogBeforeCreated() {
        if (vmsfi == null) {
            vmsfi = getVMSVirtualDomainInfo().addFilesystemPanel();
            vmsfi.waitForInfoPanel();
        } else {
            vmsfi.selectMyself();
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
                final boolean enable = vmsfi.checkResourceFields(
                                              null,
                                              vmsfi.getRealParametersFromXML())
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
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        vmsfi.savePreferredValues();
        vmsfi.getResource().setValue(FilesystemData.TYPE,
                                     FilesystemInfo.MOUNT_TYPE);
        vmsfi.addWizardParams(
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
