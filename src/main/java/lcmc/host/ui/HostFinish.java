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

package lcmc.host.ui;

import java.awt.Dimension;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.cluster.ui.wizard.AddClusterDialog;
import lcmc.common.domain.Application;
import lcmc.common.domain.UserConfig;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostFactory;

/**
 * Host finish dialog with buttons to configure next host or configure the
 * clsuter.
 */
@Named
final class HostFinish extends DialogHost {
    /**
     * Host icon for add another host button.
     */
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(Tools.getDefault("Dialog.Host.Finish.HostIcon"));
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(Tools.getDefault("Dialog.Host.Finish.ClusterIcon"));
    private static final Dimension BUTTON_DIMENSION = new Dimension(300, 100);
    private MyButton addAnotherHostButton;
    private MyButton configureClusterButton;
    private final JCheckBox saveCheckBox = new JCheckBox(Tools.getString("Dialog.Host.Finish.Save"), true);
    private NewHostDialog newHostDialog;
    private final HostFactory hostFactory;
    private final AddClusterDialog addClusterDialog;
    private final MainPresenter mainPresenter;
    private final Provider<NewHostDialog> newHostDialogFactory;
    private final Application application;
    private final SwingUtils swingUtils;
    private final WidgetFactory widgetFactory;
    private final UserConfig userConfig;

    public HostFinish(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData,
            HostFactory hostFactory, AddClusterDialog addClusterDialog, MainPresenter mainPresenter,
            @Named("newHostDialog") Provider<NewHostDialog> newHostDialogFactory, UserConfig userConfig,
            Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
        this.hostFactory = hostFactory;
        this.addClusterDialog = addClusterDialog;
        this.mainPresenter = mainPresenter;
        this.newHostDialogFactory = newHostDialogFactory;
        this.application = application;
        this.swingUtils = swingUtils;
        this.widgetFactory = widgetFactory;
        this.userConfig = userConfig;
    }

    @Override
    public WizardDialog nextDialog() {
        return newHostDialog;
    }

    @Override
    protected void finishDialog() {
        if (saveCheckBox.isSelected()) {
            final String saveFile = application.getDefaultSaveFile();
            userConfig.saveConfig(saveFile, false);
        }
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton()), buttonClass(finishButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents(new JComponent[]{buttonClass(nextButton())});
        if (application.danglingHostsCount() < 2) {
            makeDefaultAndRequestFocusLater(addAnotherHostButton);
        } else {
            makeDefaultAndRequestFocusLater(configureClusterButton);
        }
        application.removeAutoHost();
        if (application.getAutoHosts().isEmpty()) {
            if (!application.getAutoClusters().isEmpty()) {
                Tools.sleep(1000);
                swingUtils.invokeLater(() -> configureClusterButton.pressButton());
            }
        } else {
            Tools.sleep(1000);
            swingUtils.invokeLater(() -> addAnotherHostButton.pressButton());
        }
    }

    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.Finish.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.Finish.Description");
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel();
        /* host wizard button */
        addAnotherHostButton = widgetFactory.createButton(Tools.getString("Dialog.Host.Finish.AddAnotherHostButton"), HOST_ICON);
        addAnotherHostButton.setPreferredSize(BUTTON_DIMENSION);
        final DialogHost thisClass = this;
        addAnotherHostButton.addActionListener(e -> {
            final Thread t = new Thread(() -> {
                final Host newHost = hostFactory.createInstance();
                newHost.getSSH()
                        .setPasswords(getHost().getSSH().getLastSuccessfulDsaKey(), getHost().getSSH().getLastSuccessfulRsaKey(),
                                getHost().getSSH().getLastSuccessfulPassword());
                newHostDialog = newHostDialogFactory.get();
                newHostDialog.init(thisClass, newHost, getDrbdInstallation());
                mainPresenter.allHostsUpdate();
                swingUtils.invokeLater(() -> {
                    addAnotherHostButton.setEnabled(false);
                    buttonClass(nextButton()).pressButton();
                });
            });
            t.start();
        });
        /* cluster wizard button */
        configureClusterButton =
                widgetFactory.createButton(Tools.getString("Dialog.Host.Finish.ConfigureClusterButton"), CLUSTER_ICON);
        configureClusterButton.setPreferredSize(BUTTON_DIMENSION);
        configureClusterButton.addActionListener(e -> {
            final Thread t = new Thread(() -> {
                swingUtils.invokeLater(() -> {
                    configureClusterButton.setEnabled(false);
                    buttonClass(finishButton()).pressButton();
                });
                addClusterDialog.showDialogs();
            });
            t.start();
        });
        pane.add(addAnotherHostButton);
        if (application.danglingHostsCount() < 1) {
            configureClusterButton.setEnabled(false);
        }
        pane.add(configureClusterButton);
        /* Save checkbox */
        saveCheckBox.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        pane.add(saveCheckBox);
        return pane;
    }
}
