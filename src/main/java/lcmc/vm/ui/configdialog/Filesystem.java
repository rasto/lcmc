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


package lcmc.vm.ui.configdialog;

import java.awt.Dimension;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.vm.domain.data.FilesystemData;
import lcmc.vm.ui.resource.FilesystemInfo;

/**
 * An implementation of a dialog where user can enter a new domain.
 */
@Named
final class Filesystem extends VMConfig {
    /**
     * Configuration options of the new domain.
     */
    private static final String[] PARAMS =
            {FilesystemData.TYPE, FilesystemData.SOURCE_DIR, FilesystemData.SOURCE_NAME, FilesystemData.TARGET_DIR};
    private JComponent inputPane = null;
    private FilesystemInfo filesystemInfo = null;
    private WizardDialog nextDialogObject = null;
    private final Network networkDialog;
    private final Application application;
    private final SwingUtils swingUtils;

    public Filesystem(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData,
            Network networkDialog, Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
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
        return Tools.getString("Dialog.vm.Filesystem.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Filesystem.Description");
    }

    @Override
    protected void initDialogBeforeCreated() {
        if (filesystemInfo == null) {
            filesystemInfo = getVMSVirtualDomainInfo().addFilesystemPanel();
            filesystemInfo.waitForInfoPanel();
        } else {
            filesystemInfo.selectMyself();
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
            final boolean enable = filesystemInfo.checkResourceFields(null, filesystemInfo.getRealParametersFromXML()).isCorrect();
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
        filesystemInfo.savePreferredValues();
        filesystemInfo.getResource().setValue(FilesystemData.TYPE, FilesystemInfo.MOUNT_TYPE);
        filesystemInfo.addWizardParams(optionsPanel,
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
