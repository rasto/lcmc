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

import java.awt.Color;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.ExecCommandThread;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.ExecCallback;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lombok.val;

/**
 * An implementation of a dialog where user can choose cluster stack, that can be Corosync or Heartbeat.
 */
@Named
final class CommStack extends DialogCluster {
    private static final Logger LOG = LoggerFactory.getLogger(CommStack.class);
    private Widget chooseStackCombo;

    private final HbConfig hbConfigDialog;
    private final CoroConfig coroConfigDialog;
    private final Application application;
    private final SwingUtils swingUtils;
    private final WidgetFactory widgetFactory;

    public CommStack(MainData mainData, HbConfig hbConfigDialog, CoroConfig coroConfigDialog, Application application,
            SwingUtils swingUtils, WidgetFactory widgetFactory, Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
        this.hbConfigDialog = hbConfigDialog;
        this.coroConfigDialog = coroConfigDialog;
        this.application = application;
        this.swingUtils = swingUtils;
        this.widgetFactory = widgetFactory;
    }

    @Override
    public WizardDialog nextDialog() {
        DialogCluster configDialog;
        final String chosenStack = chooseStackCombo.getValue()
                                                   .getValueForConfig();
        if (Application.HEARTBEAT_NAME.equals(chosenStack)) {
            configDialog = hbConfigDialog;
        } else {
            configDialog = coroConfigDialog;
        }
        application.setLastInstalledClusterStack(chosenStack);
        configDialog.init(this, getCluster());
        return configDialog;
    }

    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.CommStack.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.CommStack.Description");
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{});
    }

    @Override
    protected void initDialogAfterVisible() {
        final Host[] hosts = getCluster().getHostsArray();
        final ExecCommandThread[] infoThreads = new ExecCommandThread[hosts.length];
        getProgressBar().start(10000);
        int i = 0;
        for (final Host host : hosts) {
            infoThreads[i] = host.execCommand(new ExecCommandConfig()
                                                  .commandString("Cluster.Init.getInstallationInfo")
                                                  .progressBar(getProgressBar())
                                                  .execCallback(new ExecCallback() {
                                                      @Override
                                                      public void done(final String answer) {
                                                          for (final String line : answer.split("\\r?\\n")) {
                                                              host.getHostParser().parseInstallationInfo(line);
                                                          }
                                                      }
                                                      @Override
                                                      public void doneError(final String answer, final int errorCode) {
                                                          skipButtonSetEnabled(false);
                                                          LOG.error("initDialogAfterVisible: "
                                                                    + host.getName()
                                                                    + ": could not get install info: "
                                                                    + answer);
                                                      }
                                                  })
                                                  .silentCommand()
                                                  .silentOutput());
            i++;
        }
        for (final ExecCommandThread t : infoThreads) {
            /* wait for all of them */
            try {
                t.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        progressBarDone();
        boolean aisIsPossible = true;
        boolean hbIsPossible = true;
        for (final Host host : hosts) {
            val hostParser = host.getHostParser();
            if (hostParser.getCorosyncVersion() == null && hostParser.getOpenaisVersion() == null) {
                aisIsPossible = false;
            }
            if (hostParser.getHeartbeatVersion() == null) {
                hbIsPossible = false;
            }
        }
        if (!aisIsPossible && hbIsPossible) {
            chooseStackCombo.setValue(new StringValue(Application.HEARTBEAT_NAME));
        }
        final boolean ais = aisIsPossible;
        final boolean hb = hbIsPossible;
        if (ais || hb) {
            swingUtils.invokeLater(() -> {
                if (ais) {
                    chooseStackCombo.setEnabled(Application.COROSYNC_NAME, true);
                }
                if (hb) {
                    chooseStackCombo.setEnabled(Application.HEARTBEAT_NAME, true);
                }
            });
        }
        enableComponents();
        if (ais || hb) {
            swingUtils.invokeLater(() -> {
                buttonClass(nextButton()).setEnabled(true);
                makeDefaultAndRequestFocus(buttonClass(nextButton()));
            });
            if (!application.getAutoClusters().isEmpty()) {
                Tools.sleep(1000);
                pressNextButton();
            }
        }
    }


    @Override
    protected JComponent getInputPane() {
        final JPanel inputPane = new JPanel(new SpringLayout());
        final Host[] hosts = getCluster().getHostsArray();
        boolean hbImpossible = false;
        boolean aisImpossible = false;
        int aisIsRc = 0;
        int hbIsRc = 0;
        int aisIsRunning = 0;
        int hbIsRunning = 0;
        for (final Host host : hosts) {
            val hostParser = host.getHostParser();
            if (hostParser.getHeartbeatVersion() == null) {
                hbImpossible = true;
            }
            if (hostParser.getCorosyncVersion() == null
                && hostParser.getOpenaisVersion() == null) {
                aisImpossible = true;
            }
            if (hostParser.isCorosyncInRc() || hostParser.isOpenaisInRc()) {
                aisIsRc++;
            }
            if (hostParser.isHeartbeatInRc()) {
                hbIsRc++;
            }
            if (hostParser.isCorosyncRunning() || hostParser.isOpenaisRunning()) {
                aisIsRunning++;
            }
            if (hostParser.isHeartbeatRunning()) {
                hbIsRunning++;
            }
        }
        /* slight preference to corosync */
        String defaultValue;
        if (hbImpossible) {
            defaultValue = Application.COROSYNC_NAME;
        } else if (aisImpossible) {
            defaultValue = Application.HEARTBEAT_NAME;
        } else if (aisIsRc < hbIsRc) {
            defaultValue = Application.HEARTBEAT_NAME;
        } else if (aisIsRc > hbIsRc) {
            defaultValue = Application.COROSYNC_NAME;
        } else if (aisIsRunning < hbIsRunning) {
            defaultValue = Application.HEARTBEAT_NAME;
        } else if (aisIsRunning > hbIsRunning) {
            defaultValue = Application.COROSYNC_NAME;
        } else {
            defaultValue = application.getLastInstalledClusterStack();
        }
        if (defaultValue == null) {
            defaultValue = Application.COROSYNC_NAME;
        }
        chooseStackCombo = widgetFactory.createInstance(
                                          Widget.Type.RADIOGROUP,
                                          new StringValue(defaultValue),
                                          new Value[]{new StringValue(Application.HEARTBEAT_NAME),
                                                      new StringValue(Application.COROSYNC_NAME)},
                                          Widget.NO_REGEXP,
                                          500,
                                          Widget.NO_ABBRV,
                                          new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                          Widget.NO_BUTTON);
        chooseStackCombo.setEnabled(Application.COROSYNC_NAME, false);
        chooseStackCombo.setEnabled(Application.HEARTBEAT_NAME, false);
        chooseStackCombo.setBackgroundColor(Color.WHITE);
        inputPane.add(getProgressBarPane(null));
        inputPane.add(chooseStackCombo.getComponent());
        SpringUtilities.makeCompactGrid(inputPane, 2, 1,  // rows, cols
                                                   0, 0,  // initX, initY
                                                   0, 0); // xPad, yPad
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(inputPane);
        panel.add(Box.createVerticalStrut(100));

        return panel;
    }

    @Override
    protected boolean skipButtonEnabled() {
        return true;
    }
}
