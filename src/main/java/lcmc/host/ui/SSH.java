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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import lcmc.cluster.ui.SSHGui;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.domain.ConnectionCallback;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * An implementation of a dialog where ssh connection will be established.
 */
@Named
public class SSH extends DialogHost {
    private static final Logger LOG = LoggerFactory.getLogger(SSH.class);
    private final Devices devices;
    private final SwingUtils swingUtils;

    public SSH(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData, Devices devices,
            Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
        this.devices = devices;
        this.swingUtils = swingUtils;
    }

    private void connectHost() {
        final SSHGui sshGui = new SSHGui(getDialogPanel(), getHost(), getProgressBar());

        getHost().connect(sshGui, getProgressBar(), new ConnectionCallback() {
            @Override
            public void done(final int flag) {
                /* flag 0 now connected
                 * flag 1 already connected. */
                LOG.debug1("done: callback done flag: " + flag);
                getHost().setConnected();
                progressBarDone();
                answerPaneSetText(Tools.getString("Dialog.Host.SSH.Connected"));
                swingUtils.invokeLater(() -> buttonClass(nextButton()).pressButton());
                final List<String> incorrect = new ArrayList<>();
                final List<String> changed = new ArrayList<>();
                enableNextButtons(incorrect, changed);
            }

                         @Override
                         public void doneError(final String errorText) {
                             getHost().setConnected();
                             final String error = Tools.getString("Dialog.Host.SSH.NotConnected") + '\n' + errorText;
                             printErrorAndRetry(error);
                             final List<String> incorrect = new ArrayList<>();
                             incorrect.add(error);
                             final List<String> changed = new ArrayList<>();
                             enableNextButtons(incorrect, changed);
                         }
                      });
        getProgressBar().setCancelEnabled(true);
    }

    @Override
    public WizardDialog nextDialog() {
        devices.init(getPreviousDialog(), getHost(), getDrbdInstallation());
        return devices;
    }

    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
    }

    @Override
    protected final void initDialogAfterVisible() {
        final Thread thread = new Thread(this::connectHost);
        thread.start();
    }

    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.SSH.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.SSH.Description");
    }

    @Override
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        pane.add(getProgressBarPane(() -> {
            LOG.debug("cancel: callback");
            getHost().getSSH().cancelConnection();
        }));
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.SSH.Connecting")));

        return pane;
    }
}
