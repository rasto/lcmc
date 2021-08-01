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

import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.vm.domain.VMParams;
import lcmc.vm.domain.data.DiskData;
import lcmc.vm.ui.resource.DiskInfo;

/**
 * An implementation of a dialog where user can enter a new domain.
 */
@Named
final class Storage extends VMConfig {
    private static final String[] PARAMS = {DiskData.TYPE,
                                            DiskData.TARGET_BUS_TYPE,
                                            DiskData.SOURCE_FILE,
                                            DiskData.SOURCE_DEVICE,

                                            DiskData.SOURCE_PROTOCOL,
                                            DiskData.SOURCE_NAME,
                                            DiskData.SOURCE_HOST_NAME,
                                            DiskData.SOURCE_HOST_PORT,

            DiskData.AUTH_USERNAME, DiskData.AUTH_SECRET_TYPE, DiskData.AUTH_SECRET_UUID,

            DiskData.DRIVER_NAME, DiskData.DRIVER_TYPE, DiskData.DRIVER_CACHE};
    private JComponent inputPane = null;
    private DiskInfo diskInfo = null;
    private WizardDialog nextDialogObject = null;
    private final Network networkDialog;
    private final Application application;
    private final SwingUtils swingUtils;

    public Storage(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData,
            Network networkDialog) {
        super(application, swingUtils, widgetFactory, mainData);
        this.networkDialog = networkDialog;
        this.application = application;
        this.swingUtils = swingUtils;
    }

    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            networkDialog.init(this, getVMSVirtualDomainInfo());
            nextDialogObject = networkDialog;
        }
        return nextDialogObject;
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.vm.Storage.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Storage.Description");
    }

    @Override
    protected void initDialogBeforeCreated() {
        if (diskInfo == null) {
            diskInfo = getVMSVirtualDomainInfo().addDiskPanel();
            diskInfo.waitForInfoPanel();
        } else {
            diskInfo.selectMyself();
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
        swingUtils.invokeLater(() -> {
            final boolean enable = diskInfo.checkResourceFields(null, diskInfo.getRealParametersFromXML()).isCorrect();
            buttonClass(nextButton()).setEnabled(enable);
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
        optionsPanel.setAlignmentY(java.awt.Component.TOP_ALIGNMENT);
        diskInfo.savePreferredValues();
        diskInfo.getResource().setValue(DiskData.TYPE, DiskInfo.FILE_TYPE);
        diskInfo.getResource().setValue(DiskData.TARGET_BUS_TYPE, DiskInfo.BUS_TYPE_IDE);
        diskInfo.getResource().setValue(DiskData.TARGET_DEVICE, new StringValue("hda"));
        diskInfo.getResource().setValue(DiskData.DRIVER_TYPE, new StringValue("raw"));
        diskInfo.getResource().setValue(DiskData.DRIVER_CACHE, new StringValue("default"));
        if ("xen".equals(getVMSVirtualDomainInfo().getWidget(VMParams.VM_PARAM_DOMAIN_TYPE, null).getStringValue())) {
            diskInfo.getResource().setValue(DiskData.DRIVER_NAME, new StringValue("file"));
        } else {
            diskInfo.getResource().setValue(DiskData.DRIVER_NAME, new StringValue("qemu"));
        }
        diskInfo.getResource().setValue(
                                    DiskData.SOURCE_FILE,
                                    new StringValue("/var/lib/libvirt/images/"
                                                    + getVMSVirtualDomainInfo().getComboBoxValue(VMParams.VM_PARAM_NAME)
                                                    + ".img"));
        diskInfo.addWizardParams(optionsPanel,
                                 PARAMS,
                                 buttonClass(nextButton()),
                                 application.getDefaultSize("Dialog.vm.Resource.LabelWidth"),
                                 application.getDefaultSize("Dialog.vm.Resource.FieldWidth"),
                                 null);
        panel.add(optionsPanel);
        final JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        inputPane = scrollPane;
        return scrollPane;
    }
}
