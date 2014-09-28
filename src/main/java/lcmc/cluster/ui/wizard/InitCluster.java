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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.border.TitledBorder;

import lcmc.gui.GUIData;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.cluster.domain.Cluster;
import lcmc.host.domain.Host;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.gui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.gui.widget.Check;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.Corosync;
import lcmc.utilities.DRBD;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Heartbeat;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.Openais;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.ExecCommandThread;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog where cluster is initialized on all hosts.
 */
@Named
public class InitCluster extends DialogCluster {
    private static final Logger LOG = LoggerFactory.getLogger(InitCluster.class);
    private static final int CHECK_INTERVAL = 1000;
    private static final String HEARTBEAT_BUTTON_SWITCH_TEXT = Tools.getString("Dialog.Cluster.Init.HbButtonSwitch");
    private static final String COROSYNC_AIS_BUTTON_SWITCH_TEXT =
                                                          Tools.getString("Dialog.Cluster.Init.CsAisButtonSwitch");
    private static final String COROSYNC_INIT_SCRIPT = "use /etc/init.d/corosync";
    private static final String OPENAIS_INIT_SCRIPT = "/etc/init.d/openais";
    private List<JLabel> drbdLoadedLabels;
    private List<MyButton> drbdLoadButtons;

    /** List with texts whether Pacemaker is started per host. */
    private List<JLabel> pacemakerStartedLabels;
    private List<MyButton> startPacemakerButtons;
    private List<JLabel> heartbeatStartedLabels;
    private List<MyButton> startHeartbeatButtons;

    private JPanel mainPanel;
    private volatile boolean checkClusterStopped;
    private Boolean[] lastDrbdLoaded;
    private Boolean[] lastPacemakerStarted;
    private Boolean[] lastPacemakerInRc;
    private Boolean[] lastPacemakerInstalled;
    private Boolean[] lastPacemakerConfigured;
    private Boolean[] lastHeartbeatStarted;
    private Boolean[] lastHeartbeatInRc;
    private Boolean[] lastHeartbeatInstalled;
    private Boolean[] lastHeartbeatConfigured;

    private Thread checkClusterThread = null;
    /** Button that acts as a finish button. This is used by methods that
     * override this one and use different finish/next button.
     */
    private String otherFinishButton = null;
    @Inject
    private WidgetFactory widgetFactory;
    /** Whether to use openais init script instead of corosync. It applies only
     * if both of them are present. */
    private Widget useOpenaisButton;
    @Inject
    private Finish finishDialog;
    @Inject
    private GUIData guiData;
    @Inject
    private Application application;

    public void init(final WizardDialog previousDialog, final Cluster cluster) {
        super.init(previousDialog, cluster);
        useOpenaisButton = widgetFactory.createInstance(
                Widget.Type.RADIOGROUP,
                Widget.NO_DEFAULT,
                new Value[]{new StringValue(COROSYNC_INIT_SCRIPT), new StringValue(OPENAIS_INIT_SCRIPT)},
                Widget.NO_REGEXP,
                0,
                Widget.NO_ABBRV,
                new AccessMode(Application.AccessType.ADMIN, !AccessMode.ADVANCED),
                Widget.NO_BUTTON);
        setOtherFinishButton(finishButton());
        finishDialog.init(this, getCluster());
    }

    private void setOtherFinishButton(final String otherFinishButton) {
        this.otherFinishButton = otherFinishButton;
    }

    private void stopCheckCluster() {
        checkClusterStopped = true;
    }

    @Override
    public final WizardDialog getPreviousDialog() {
        stopCheckCluster();
        return super.getPreviousDialog();
    }

    @Override
    protected final void finishDialog() {
        stopCheckCluster();
    }

    @Override
    public final void cancelDialog() {
        stopCheckCluster();
    }

    @Override
    public final WizardDialog nextDialog() {
        stopCheckCluster();
        return finishDialog;
    }

    @Override
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.Init.Title");
    }

    @Override
    protected final String getDescription() {
        return Tools.getString("Dialog.Cluster.Init.Description");
    }

    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{});
    }

    @Override
    protected void initDialogAfterVisible() {
        lastDrbdLoaded = null;
        lastPacemakerStarted = null;
        lastPacemakerInRc = null;
        lastHeartbeatStarted = null;
        lastHeartbeatInRc = null;
        checkClusterStopped = false;
        checkClusterThread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    while (!checkClusterStopped) {
                        checkCluster(true);
                        if (!checkClusterStopped) {
                            try {
                                Thread.sleep(CHECK_INTERVAL);
                            } catch (final InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                    checkClusterThread = null;
                }
            });
        checkClusterThread.start();
    }

    /** Checks drbds and heartbeats on all nodes of the cluster. */
    private void checkCluster(final boolean periodic) {
        /* check if modules are loaded. */
        final Host[] hosts = getCluster().getHostsArray();
        final ExecCommandThread[] infoThreads = new ExecCommandThread[hosts.length];
        int i = 0;
        for (final Host h : hosts) {
            infoThreads[i] = h.execCommand(new ExecCommandConfig()
                                               .commandString("Cluster.Init.getInstallationInfo")
                                               .execCallback(new ExecCallback() {
                                                   @Override
                                                   public void done(final String answer) {
                                                       for (final String line : answer.split("\\r?\\n")) {
                                                           h.parseInstallationInfo(line);
                                                       }
                                                   }
                                                   @Override
                                                   public void doneError(final String answer, final int errorCode) {
                                                       LOG.appWarning("doneError: could not get install info");
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

        /* DRBD */
        i = 0;
        final boolean lastDrbdLoadedExists = (lastDrbdLoaded != null);
        if (!lastDrbdLoadedExists) {
            lastDrbdLoaded = new Boolean[hosts.length];
        }
        final boolean lastPmStartedExists = (lastPacemakerStarted != null);
        if (!lastPmStartedExists) {
            lastPacemakerStarted = new Boolean[hosts.length];
            lastPacemakerInRc = new Boolean[hosts.length];
            lastPacemakerConfigured = new Boolean[hosts.length];
            lastPacemakerInstalled = new Boolean[hosts.length];
        }
        final boolean lastHbStartedExists = (lastHeartbeatStarted != null);
        if (!lastHbStartedExists) {
            lastHeartbeatStarted = new Boolean[hosts.length];
            lastHeartbeatInRc = new Boolean[hosts.length];
            lastHeartbeatConfigured = new Boolean[hosts.length];
            lastHeartbeatInstalled = new Boolean[hosts.length];
        }
        boolean needOpenaisButton = false;

        boolean oneFailed = false;
        boolean oneChanged = false;
        for (final Host h : hosts) {
            /* is drbd loaded */
            boolean drbdLoadedChanged = false;
            final boolean drbdLoaded = h.isDrbdLoaded();

            if (lastDrbdLoadedExists) {
                if (lastDrbdLoaded[i] != drbdLoaded) {
                    oneChanged = true;
                    drbdLoadedChanged = true;
                    lastDrbdLoaded[i] = drbdLoaded;
                }
            } else {
                oneChanged = true;
                drbdLoadedChanged = true;
                lastDrbdLoaded[i] = drbdLoaded;
            }
            final JLabel drbdLoadedInfo = drbdLoadedLabels.get(i);
            boolean drbdFailed = false;
            if (drbdLoaded) {
                if (drbdLoadedChanged) {
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            drbdLoadedInfo.setText(Tools.getString("Dialog.Cluster.Init.DrbdIsLoaded"));
                            drbdLoadedInfo.setForeground(Color.BLACK);
                        }
                    });
                }
            } else {
                drbdFailed = true;
                if (drbdLoadedChanged) {
                    final MyButton drbdLoadButton = drbdLoadButtons.get(i);
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            drbdLoadedInfo.setText(Tools.getString("Dialog.Cluster.Init.DrbdIsNotLoaded"));
                            drbdLoadedInfo.setForeground(Color.RED);
                            drbdLoadButton.setVisible(true);
                        }
                    });
                }
            }

            final boolean csAisIsInstalled = h.getOpenaisVersion() != null || h.getCorosyncVersion() != null;
            final boolean csAisRunning = h.isCorosyncRunning() || h.isOpenaisRunning();
            final boolean csAisIsRc = h.isCorosyncInRc() || h.isOpenaisInRc();
            final boolean csAisIsConf = h.corosyncOrOpenaisConfigExists();

            final boolean heartbeatIsInstalled = h.getHeartbeatVersion() != null;
            final boolean heartbeatIsRunning = h.isHeartbeatRunning();
            final boolean heartbeatIsRc = h.isHeartbeatInRc();
            final boolean heartbeatIsConf = h.heartbeatConfigExists();
            if (!csAisRunning && h.hasCorosyncInitScript() && h.hasOpenaisInitScript()) {
                needOpenaisButton = true;
            }

            boolean hbChanged = false;
            boolean csAisChanged = false;
            if (lastPmStartedExists) {
                if (lastPacemakerStarted[i] != csAisRunning) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPacemakerStarted[i] = csAisRunning;
                }
                if (lastPacemakerInRc[i] != csAisIsRc) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPacemakerInRc[i] = csAisIsRc;
                }
                if (lastPacemakerConfigured[i] != csAisIsConf) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPacemakerConfigured[i] = csAisIsConf;
                }
                if (lastPacemakerInstalled[i] != csAisIsInstalled) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPacemakerInstalled[i] = csAisIsInstalled;
                }

                if (lastHeartbeatStarted[i] != heartbeatIsRunning) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHeartbeatStarted[i] = heartbeatIsRunning;
                }
                if (lastHeartbeatInRc[i] != heartbeatIsRc) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHeartbeatInRc[i] = heartbeatIsRc;
                }
                if (lastHeartbeatConfigured[i] != heartbeatIsConf) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHeartbeatConfigured[i] = heartbeatIsConf;
                }
                if (lastHeartbeatInstalled[i] != heartbeatIsInstalled) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHeartbeatInstalled[i] = heartbeatIsInstalled;
                }
            } else {
                oneChanged = true;
                csAisChanged = true;
                lastPacemakerStarted[i] = csAisRunning;
                lastPacemakerInRc[i] = csAisIsRc;
                lastPacemakerConfigured[i] = csAisIsConf;
                lastPacemakerInstalled[i] = csAisIsInstalled;

                hbChanged = true;
                lastHeartbeatStarted[i] = heartbeatIsRunning;
                lastHeartbeatInRc[i] = heartbeatIsRc;
                lastHeartbeatConfigured[i] = heartbeatIsConf;
                lastHeartbeatInstalled[i] = heartbeatIsInstalled;
            }

            /* Corosync/Openais */
            final JLabel pmStartedInfo = pacemakerStartedLabels.get(i);
            final MyButton csAisStartButton = startPacemakerButtons.get(i);
            String is = "Corosync";
            if (!useCorosync(h) && h.getOpenaisVersion() != null) {
                is = "OpenAIS";
            }
            final String initScript = is;
            boolean csAisFailed = false;
            if (csAisRunning) {
                if (csAisChanged || hbChanged) {
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            pmStartedInfo.setText(
                                initScript + Tools.getString("Dialog.Cluster.Init.CsAisIsRunning"));
                            pmStartedInfo.setForeground(Color.BLACK);
                            if (csAisIsRc) {
                                csAisStartButton.setVisible(false);
                                pmStartedInfo.setText(
                                    initScript + Tools.getString("Dialog.Cluster.Init.CsAisIsRc"));
                            } else if (heartbeatIsRunning || heartbeatIsRc) {
                                csAisStartButton.setText(COROSYNC_AIS_BUTTON_SWITCH_TEXT);
                                csAisStartButton.setVisible(true);
                            } else {
                                csAisStartButton.setText(Tools.getString("Dialog.Cluster.Init.CsAisButtonRc"));
                                csAisStartButton.setVisible(true);
                            }
                        }
                    });
                }
            } else {
                csAisFailed = true;
                if (csAisChanged || hbChanged) {
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (heartbeatIsRunning || heartbeatIsRc) {
                                csAisStartButton.setText(COROSYNC_AIS_BUTTON_SWITCH_TEXT);
                            } else {
                                csAisStartButton.setText(Tools.getString("Dialog.Cluster.Init.StartCsAisButton"));
                            }
                            if (!csAisIsInstalled) {
                                pmStartedInfo.setText(initScript
                                                      + Tools.getString("Dialog.Cluster.Init.CsAisIsNotInstalled"));
                                csAisStartButton.setEnabled(false);
                            } else if (!csAisIsConf) {
                                pmStartedInfo.setText(initScript
                                                      + Tools.getString("Dialog.Cluster.Init.CsAisIsNotConfigured"));
                                csAisStartButton.setEnabled(false);
                            } else {
                                pmStartedInfo.setText(
                                   initScript + Tools.getString("Dialog.Cluster.Init.CsAisIsStopped"));
                                if (heartbeatIsRunning) {
                                    csAisStartButton.setEnabled(false);
                                } else {
                                    guiData.setAccessible(csAisStartButton, Application.AccessType.OP);
                                }
                            }
                            csAisStartButton.setVisible(true);
                            pmStartedInfo.setForeground(Color.RED);
                        }
                    });
                }
            }

            /* Heartbeat */
            final JLabel hbStartedInfo = heartbeatStartedLabels.get(i);
            final MyButton hbStartButton = startHeartbeatButtons.get(i);
            boolean hbFailed = false;
            if (heartbeatIsRunning) {
                if (hbChanged || csAisChanged) {
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            hbStartedInfo.setText(Tools.getString("Dialog.Cluster.Init.HbIsRunning"));
                            hbStartedInfo.setForeground(Color.BLACK);
                            if (heartbeatIsRc) {
                                hbStartButton.setVisible(false);
                                hbStartedInfo.setText(Tools.getString("Dialog.Cluster.Init.HbIsRc"));
                            } else if (csAisRunning || csAisIsRc) {
                                hbStartButton.setText(HEARTBEAT_BUTTON_SWITCH_TEXT);
                                hbStartButton.setVisible(true);
                            } else {
                                hbStartButton.setText(Tools.getString("Dialog.Cluster.Init.HbButtonRc"));
                                hbStartButton.setVisible(true);
                            }
                        }
                    });
                }
            } else {
                hbFailed = true;
                if (hbChanged || csAisChanged) {
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (csAisRunning || csAisIsRc) {
                                hbStartButton.setText(HEARTBEAT_BUTTON_SWITCH_TEXT);
                            } else {
                                hbStartButton.setText(Tools.getString("Dialog.Cluster.Init.StartHbButton"));
                            }
                            if (!heartbeatIsInstalled) {
                                hbStartedInfo.setText(Tools.getString("Dialog.Cluster.Init.HbIsNotInstalled"));
                                hbStartButton.setEnabled(false);
                            } else if (!heartbeatIsConf) {
                                hbStartedInfo.setText(Tools.getString("Dialog.Cluster.Init.HbIsNotConfigured"));
                                hbStartButton.setEnabled(false);
                            } else {
                                hbStartedInfo.setText(Tools.getString("Dialog.Cluster.Init.HbIsStopped"));
                                if (csAisRunning) {
                                    hbStartButton.setEnabled(false);
                                } else {
                                    guiData.setAccessible(hbStartButton, Application.AccessType.OP);
                                }
                            }
                            hbStartButton.setVisible(true);
                            hbStartedInfo.setForeground(Color.RED);
                        }
                    });
                }
            }
            if (drbdFailed || (csAisFailed && hbFailed)) {
                oneFailed = true;
            }
            i++;
        }
        final boolean nob = needOpenaisButton;
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                useOpenaisButton.setEnabled(nob);
            }
        });

        if (oneChanged || !periodic) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    mainPanel.invalidate();
                    mainPanel.validate();
                    mainPanel.repaint();
                }
            });

        final List<String> incorrect = new ArrayList<String>();
        if (oneFailed) {
            incorrect.add("one component failed");
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        buttonClass(otherFinishButton).setEnabled(false);
                    }
                });
            }
        final List<String> changed = new ArrayList<String>();
            enableComponents();
            nextButtonSetEnabled(new Check(incorrect, changed));
            if (!application.getAutoClusters().isEmpty()) {
                Tools.sleep(1000);
                pressNextButton();
            }
        }
    }

    /**
     * Returns the input pane with status information about drbd and heartbeat
     * and some buttons.
     */
    @Override
    protected final JComponent getInputPane() {
        /* Waiting for check cluster thread to finish. To avoid all races. This
         * can happen after clicking the back button from the next dialog. */
        final Thread t = checkClusterThread;
        if (t != null) {
            try {
                t.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        drbdLoadedLabels = new ArrayList<JLabel>();
        drbdLoadButtons = new ArrayList<MyButton>();

        pacemakerStartedLabels = new ArrayList<JLabel>();
        startPacemakerButtons = new ArrayList<MyButton>();

        heartbeatStartedLabels = new ArrayList<JLabel>();
        startHeartbeatButtons = new ArrayList<MyButton>();

        mainPanel = new JPanel(new GridLayout(1, 0));

        final Host[] hosts = getCluster().getHostsArray();
        /* DRBD */
        int i = 0;
        boolean oneStartedAsOpenais = false;
        boolean noCorosync = false;
        for (final Host host : hosts) {
            final int index = i;

            final SpringLayout layout = new SpringLayout();
            final JPanel pane = new JPanel(layout);
            pane.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

            final TitledBorder titledBorder = BorderFactory.createTitledBorder(host.getName());
            titledBorder.setTitleJustification(TitledBorder.LEFT);
            pane.setBorder(titledBorder);

            drbdLoadedLabels.add(new JLabel(Tools.getString("Dialog.Cluster.Init.CheckingDrbd")));
            final MyButton drbdb = widgetFactory.createButton(Tools.getString("Dialog.Cluster.Init.LoadDrbdButton"));
            drbdb.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
            drbdLoadButtons.add(drbdb);
            drbdLoadButtons.get(i).setVisible(false);

            drbdLoadButtons.get(i).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    application.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            drbdLoadButtons.get(index).setVisible(false);
                                        }
                                    });
                                    final Application.RunMode runMode = Application.RunMode.LIVE;
                                    DRBD.load(host, runMode);
                                    checkCluster(false);
                                }
                            }
                        );
                        thread.start();
                    }
                });

            pane.add(drbdLoadedLabels.get(i));
            pane.add(drbdLoadButtons.get(i));

            /* Heartbeat */
            heartbeatStartedLabels.add(new JLabel(Tools.getString("Dialog.Cluster.Init.CheckingHb")));
            final MyButton btn;
            if (host.isCorosyncRunning() || host.isOpenaisRunning() || host.isCorosyncInRc() || host.isOpenaisInRc()) {
                btn = widgetFactory.createButton(HEARTBEAT_BUTTON_SWITCH_TEXT);
            } else {
                btn = widgetFactory.createButton(Tools.getString("Dialog.Cluster.Init.StartHbButton"));
            }
            btn.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
            startHeartbeatButtons.add(btn);
            startHeartbeatButtons.get(i).setVisible(false);

            startHeartbeatButtons.get(i).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    disableComponents();
                                    application.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            startHeartbeatButtons.get(index).setVisible(false);
                                        }
                                    });
                                    if (Tools.getString("Dialog.Cluster.Init.HbButtonRc").equals(
                                                                                            e.getActionCommand())) {
                                        Heartbeat.addHeartbeatToRc(host);
                                    } else if (useCorosync(host)
                                               && HEARTBEAT_BUTTON_SWITCH_TEXT.equals(e.getActionCommand())) {
                                        Heartbeat.switchFromCorosyncToHeartbeat(host);
                                    } else if (!useCorosync(host)
                                               && HEARTBEAT_BUTTON_SWITCH_TEXT.equals(e.getActionCommand())) {
                                        Heartbeat.switchFromOpenaisToHeartbeat(host);
                                    } else {
                                        if (host.isHeartbeatInRc()) {
                                            Heartbeat.startHeartbeat(host);
                                        } else {
                                            Heartbeat.startHeartbeatRc(host);
                                        }
                                    }
                                    checkCluster(false);
                                }
                            }
                        );
                        thread.start();
                    }
                });

            pane.add(heartbeatStartedLabels.get(i));
            pane.add(startHeartbeatButtons.get(i));

            /* Pacemaker */
            pacemakerStartedLabels.add(new JLabel(Tools.getString("Dialog.Cluster.Init.CheckingPm")));
            final MyButton pmsb = widgetFactory.createButton(Tools.getString("Dialog.Cluster.Init.StartCsAisButton"));
            pmsb.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
            startPacemakerButtons.add(pmsb);
            startPacemakerButtons.get(i).setVisible(false);

            startPacemakerButtons.get(i).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            disableComponents();
                            application.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    startPacemakerButtons.get(index).setVisible(false);
                                }
                            });
                            if (Tools.getString("Dialog.Cluster.Init.CsAisButtonRc").equals(e.getActionCommand())) {
                                if (useCorosync(host)) {
                                    Corosync.addCorosyncToRc(host);
                                } else {
                                    Openais.addOpenaisToRc(host);
                                }
                            } else if (COROSYNC_AIS_BUTTON_SWITCH_TEXT.equals(e.getActionCommand())) {
                                if (useCorosync(host)) {
                                    Corosync.switchToCorosync(host);
                                } else {
                                    Openais.switchToOpenais(host);
                                }
                            } else {
                                if (host.isCorosyncInRc() || host.isOpenaisInRc()) {
                                    if (useCorosync(host)) {
                                        Corosync.startCorosync(host);
                                    } else {
                                        Openais.startOpenais(host);
                                    }
                                } else {
                                    if (useCorosync(host)) {
                                        Corosync.startCorosyncRc(host);
                                    } else {
                                        Openais.startOpenaisRc(host);
                                    }
                                }
                            }
                            checkCluster(false);
                        }
                    });
                    thread.start();
                }
            });
            if (host.isCorosyncRunning() && host.isOpenaisRunning()) {
                /* started with openais init script. */
                oneStartedAsOpenais = true;
            }
            if (!host.hasCorosyncInitScript()) {
                noCorosync = true;
            }

            pane.add(pacemakerStartedLabels.get(i));
            pane.add(startPacemakerButtons.get(i));
            i++;
            SpringUtilities.makeCompactGrid(pane, 3, 2,  //rows, cols
                                                  1, 1,  //initX, initY
                                                  1, 0); //xPad, yPad
            mainPanel.add(pane);
        }
        final JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
        final JScrollPane s = new JScrollPane(mainPanel);
        if (oneStartedAsOpenais || noCorosync) {
            useOpenaisButton.setValue(new StringValue(OPENAIS_INIT_SCRIPT));
        } else {
            useOpenaisButton.setValue(new StringValue(COROSYNC_INIT_SCRIPT));
        }
        useOpenaisButton.setEnabled(false);
        useOpenaisButton.setBackgroundColor(Color.WHITE);
        useOpenaisButton.getComponent().setMaximumSize(useOpenaisButton.getComponent().getMinimumSize());
        p.add(useOpenaisButton.getComponent());
        p.add(s);
        return p;
    }

    @Override
    protected final boolean skipButtonEnabled() {
        return true;
    }

    /** Whether to use corosync or openais init script. */
    private boolean useCorosync(final Host host) {
        return !(!host.isCorosyncInstalled() || !host.hasCorosyncInitScript())
               && (host.hasCorosyncInitScript() && COROSYNC_INIT_SCRIPT.equals(useOpenaisButton.getStringValue())
               || !host.hasOpenaisInitScript());
    }
}
