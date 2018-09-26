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


package lcmc.cluster.ui.wizard;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;

import java.util.function.Supplier;

/**
 * An implementation of a dialog where connection to every host will be checked
 * and established if there isn't one.
 */
public class Connect extends DialogCluster {

    private final CommStack commStackDialog;
    private final SwingUtils swingUtils;

    private static final Logger LOG = LoggerFactory.getLogger(Connect.class);

    public Connect(Supplier<ProgressBar> progressBarProvider, Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData, CommStack commStackDialog) {
        super(progressBarProvider, application, swingUtils, widgetFactory, mainData);
        this.commStackDialog = commStackDialog;
        this.swingUtils = swingUtils;
    }

    @Override
    public WizardDialog nextDialog() {
        commStackDialog.init(getPreviousDialog(), getCluster());
        return commStackDialog;
    }

    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.Connect.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.Connect.Description");
    }

    /** Checks hosts, if they are connected and if not reconnects them. */
    protected void checkHosts() {
        final StringBuilder text = new StringBuilder();
        boolean pending = false;
        boolean oneFailed = false;
        for (final Host host : getCluster().getHosts()) {
            final String status;
            if (host.getSSH().isConnectionFailed()) {
                status = "failed.";
                oneFailed = true;
            } else if (host.isConnected()) {
                status = "connected.";
            } else {
                pending = true;
                status = "connecting...";
            }
            text.append(host.getName()).append(' ').append(status).append('\n');
        }
        LOG.debug("checkHosts: pending: " + pending + ", one failed: " + oneFailed);
        if (pending) {
             answerPaneSetText(text.toString());
        } else if (oneFailed) {
             printErrorAndRetry(text.toString());
        } else {
             answerPaneSetText(text.toString());
             try {
                 Thread.sleep(1000);
             } catch (final InterruptedException ex) {
                 Thread.currentThread().interrupt();
             }

             swingUtils.invokeLater(new Runnable() {
                 @Override
                 public void run() {
                    buttonClass(nextButton()).pressButton();
                 }
             });
        }
    }

    protected void connectHosts() {
        getCluster().connect(getDialogPanel(), true, 1);
        for (final Host host : getCluster().getHosts()) {
            host.waitOnLoading();
        }
        checkHosts();
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                connectHosts();
            }
        });
        t.start();
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        final StringBuilder text = new StringBuilder();
        for (final Host host : getCluster().getHosts()) {
            text.append(host.getName());
            text.append(" connecting...\n");
        }
        pane.add(getAnswerPane(text.toString()));

        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }

    @Override
    protected boolean skipButtonEnabled() {
        return true;
    }
}
