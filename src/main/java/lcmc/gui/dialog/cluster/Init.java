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


package lcmc.gui.dialog.cluster;

import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.Tools;
import lcmc.utilities.DRBD;
import lcmc.utilities.Heartbeat;
import lcmc.utilities.Openais;
import lcmc.utilities.Corosync;
import lcmc.utilities.SSH.ExecCommandThread;
import lcmc.utilities.MyButton;
import lcmc.utilities.SSH;
import lcmc.gui.SpringUtilities;
import lcmc.utilities.ExecCallback;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.dialog.WizardDialog;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JLabel;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Color;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import lcmc.data.StringValue;

import lcmc.data.Value;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * An implementation of a dialog where heartbeat is initialized on all hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class Init extends DialogCluster {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(Init.class);
    /** List with texts if drbd is loaded per host. */
    private List<JLabel> drbdLoadedInfos;
    /** List of load drbd buttons. */
    private List<MyButton> drbdLoadButtons;

    /** List with texts whether Pacemaker is started per host. */
    private List<JLabel> pmStartedInfos;
    /** List of start Pacemaker buttons. */
    private List<MyButton> pmStartButtons;
    /** List with texts whether hb is started per host. */
    private List<JLabel> hbStartedInfos;
    /** List of start hb buttons. */
    private List<MyButton> hbStartButtons;

    /** Main panel, so that it can be revalidated, if something have changed.
     */
    private JPanel mainPanel;
    /** Whether the checking of the cluster should be stopped. */
    private volatile boolean checkClusterStopped;
    /** Last value of drbd-is-loadeded check. */
    private Boolean[] lastDrbdLoaded;
    /** Last value of pacemaker-is-started check. */
    private Boolean[] lastPmStarted;
    /** Last value of pacemaker-is-in-rc check. */
    private Boolean[] lastPmRc;
    /** Last value of pacemaker-is-installed check. */
    private Boolean[] lastPmInstalled;
    /** Last value of pacemaker-is-configured check. */
    private Boolean[] lastPmConf;
    /** Last value of hb-is-started check. */
    private Boolean[] lastHbStarted;
    /** Last value of heartbeat-is-in-rc check. */
    private Boolean[] lastHbRc;
    /** Last value of heartbeat-is-installed check. */
    private Boolean[] lastHbInstalled;
    /** Last value of heartbeat-is-configured check. */
    private Boolean[] lastHbConf;

    /** Cluster check thread. */
    private Thread checkClusterThread = null;
    /** Button that acts as a finish button. This is used by methods that
     * override this one and use different finish/next button.
     */
    private String button = null;
    /** Interval between checks. */
    private static final int CHECK_INTERVAL = 1000;
    /** Switch to Heartbeat button text. */
    private static final String HB_BUTTON_SWITCH =
                        Tools.getString("Dialog.Cluster.Init.HbButtonSwitch");
    /** Switch to Corosync/OpenAIS button text. */
    private static final String CS_AIS_BUTTON_SWITCH =
                       Tools.getString("Dialog.Cluster.Init.CsAisButtonSwitch");
    /** Corosync init script. */
    private static final String COROSYNC_INIT_SCRIPT =
                                                 "use /etc/init.d/corosync";
    /** Openais init script. */
    private static final String OPENAIS_INIT_SCRIPT = "/etc/init.d/openais";
    /** Whether to use openais init script instead of corosync. It applies only
     * if both of them are present. */
    private final Widget useOpenaisButton = WidgetFactory.createInstance(
                                   Widget.Type.RADIOGROUP,
                                   Widget.NO_DEFAULT,
                                   new Value[]{new StringValue(COROSYNC_INIT_SCRIPT),
                                               new StringValue(OPENAIS_INIT_SCRIPT)},
                                   Widget.NO_REGEXP,
                                   0,
                                   Widget.NO_ABBRV,
                                   new AccessMode(ConfigData.AccessType.ADMIN,
                                                  !AccessMode.ADVANCED),
                                   Widget.NO_BUTTON);

    /** Prepares a new {@code Init} object. */
    public Init(final WizardDialog previousDialog,
                final Cluster cluster) {
        super(previousDialog, cluster);
        setButton(finishButton());
    }

    /** Sets button. Which acts as a finish button. */
    private void setButton(final String button) {
        this.button = button;
    }

    /** Stops the checks and waits for them to stop. */
    private void stopCheckCluster() {
        checkClusterStopped = true;
    }

    /**
     * Returns previous dialog. It is used to get with the back button to
     * the dialog before this one.
     */
    @Override
    public final WizardDialog getPreviousDialog() {
        stopCheckCluster();
        return super.getPreviousDialog();
    }

    /** After the dialog is finished. */
    @Override
    protected final void finishDialog() {
        stopCheckCluster();
    }

    /** Is called before the dialog is canceled. It stops all the checks. */
    @Override
    public final void cancelDialog() {
        stopCheckCluster();
    }

    /** Returns the next dialog. */
    @Override
    public final WizardDialog nextDialog() {
        stopCheckCluster();
        return new Finish(this, getCluster());
    }

    /** Returns the title of the dialog. */
    @Override
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.Init.Title");
    }

    /** Returns the description of the dialog. */
    @Override
    protected final String getDescription() {
        return Tools.getString("Dialog.Cluster.Init.Description");
    }

    /** Inits the dialog. */
    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        lastDrbdLoaded = null;
        lastPmStarted = null;
        lastPmRc = null;
        lastHbStarted = null;
        lastHbRc = null;
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
        final ExecCommandThread[] infoThreads =
                                        new ExecCommandThread[hosts.length];
        int i = 0;
        for (final Host h : hosts) {
            infoThreads[i] = h.execCommand("Cluster.Init.getInstallationInfo",
                    null,
                             new ExecCallback() {
                                 @Override
                                 public void done(final String answer) {
                                     //drbdLoaded[index] = true;
                                     for (final String line
                                                    : answer.split("\\r?\\n")) {
                                         h.parseInstallationInfo(line);
                                     }
                                 }
                                 @Override
                                 public void doneError(final String answer,
                                                       final int errorCode) {
                                     LOG.appWarning("doneError: could not get install info");
                                 }
                             },
                             null,   /* ConvertCmdCallback */
                             false,  /* outputVisible */
                             SSH.DEFAULT_COMMAND_TIMEOUT);
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
        final boolean lastPmStartedExists = (lastPmStarted != null);
        if (!lastPmStartedExists) {
            lastPmStarted = new Boolean[hosts.length];
            lastPmRc = new Boolean[hosts.length];
            lastPmConf = new Boolean[hosts.length];
            lastPmInstalled = new Boolean[hosts.length];
        }
        final boolean lastHbStartedExists = (lastHbStarted != null);
        if (!lastHbStartedExists) {
            lastHbStarted = new Boolean[hosts.length];
            lastHbRc = new Boolean[hosts.length];
            lastHbConf = new Boolean[hosts.length];
            lastHbInstalled = new Boolean[hosts.length];
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
            final JLabel drbdLoadedInfo = drbdLoadedInfos.get(i);
            boolean drbdFailed = false;
            if (drbdLoaded) {
                if (drbdLoadedChanged) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            drbdLoadedInfo.setText(Tools.getString(
                                           "Dialog.Cluster.Init.DrbdIsLoaded"));
                            drbdLoadedInfo.setForeground(Color.BLACK);
                        }
                    });
                }
            } else {
                drbdFailed = true;
                if (drbdLoadedChanged) {
                    final MyButton drbdLoadButton = drbdLoadButtons.get(i);
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            drbdLoadedInfo.setText(Tools.getString(
                                        "Dialog.Cluster.Init.DrbdIsNotLoaded"));
                            drbdLoadedInfo.setForeground(Color.RED);
                            drbdLoadButton.setVisible(true);
                        }
                    });
                }
            }

            final boolean csAisIsInstalled = h.getOpenaisVersion() != null
                                             || h.getCorosyncVersion() != null;
            final boolean csAisRunning     = h.isCsRunning()
                                             || h.isAisRunning();
            final boolean csAisIsRc        = h.isCsRc() || h.isAisRc();
            final boolean csAisIsConf      = h.isCsAisConf();

            final boolean heartbeatIsInstalled =
                                            h.getHeartbeatVersion() != null;
            final boolean heartbeatIsRunning   = h.isHeartbeatRunning();
            final boolean heartbeatIsRc      = h.isHeartbeatRc();
            final boolean heartbeatIsConf      = h.isHeartbeatConf();
            if (!csAisRunning && h.isCsInit() && h.isAisInit()) {
                needOpenaisButton = true;
            }

            boolean hbChanged = false;
            boolean csAisChanged = false;
            if (lastPmStartedExists) {
                if (lastPmStarted[i] != csAisRunning) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPmStarted[i] = csAisRunning;
                }
                if (lastPmRc[i] != csAisIsRc) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPmRc[i] = csAisIsRc;
                }
                if (lastPmConf[i] != csAisIsConf) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPmConf[i] = csAisIsConf;
                }
                if (lastPmInstalled[i] != csAisIsInstalled) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPmInstalled[i] = csAisIsInstalled;
                }

                if (lastHbStarted[i] != heartbeatIsRunning) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHbStarted[i] = heartbeatIsRunning;
                }
                if (lastHbRc[i] != heartbeatIsRc) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHbRc[i] = heartbeatIsRc;
                }
                if (lastHbConf[i] != heartbeatIsConf) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHbConf[i] = heartbeatIsConf;
                }
                if (lastHbInstalled[i] != heartbeatIsInstalled) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHbInstalled[i] = heartbeatIsInstalled;
                }
            } else {
                oneChanged = true;
                csAisChanged = true;
                lastPmStarted[i] = csAisRunning;
                lastPmRc[i] = csAisIsRc;
                lastPmConf[i] = csAisIsConf;
                lastPmInstalled[i] = csAisIsInstalled;

                hbChanged = true;
                lastHbStarted[i] = heartbeatIsRunning;
                lastHbRc[i] = heartbeatIsRc;
                lastHbConf[i] = heartbeatIsConf;
                lastHbInstalled[i] = heartbeatIsInstalled;
            }

            /* Corosync/Openais */
            final JLabel pmStartedInfo = pmStartedInfos.get(i);
            final MyButton csAisStartButton = pmStartButtons.get(i);
            String is = "Corosync";
            if (!useCorosync(h)
                && h.getOpenaisVersion() != null) {
                is = "OpenAIS";
            }
            final String initScript = is;
            boolean csAisFailed = false;
            if (csAisRunning) {
                if (csAisChanged || hbChanged) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            pmStartedInfo.setText(
                                initScript + Tools.getString(
                                        "Dialog.Cluster.Init.CsAisIsRunning"));
                            pmStartedInfo.setForeground(Color.BLACK);
                            if (csAisIsRc) {
                                csAisStartButton.setVisible(false);
                                pmStartedInfo.setText(
                                    initScript + Tools.getString(
                                             "Dialog.Cluster.Init.CsAisIsRc"));
                            } else if (heartbeatIsRunning || heartbeatIsRc) {
                                csAisStartButton.setText(CS_AIS_BUTTON_SWITCH);
                                csAisStartButton.setVisible(true);
                            } else {
                                csAisStartButton.setText(Tools.getString(
                                         "Dialog.Cluster.Init.CsAisButtonRc"));
                                csAisStartButton.setVisible(true);
                            }
                        }
                    });
                }
            } else {
                csAisFailed = true;
                if (csAisChanged || hbChanged) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (heartbeatIsRunning || heartbeatIsRc) {
                                csAisStartButton.setText(CS_AIS_BUTTON_SWITCH);
                            } else {
                                csAisStartButton.setText(Tools.getString(
                                      "Dialog.Cluster.Init.StartCsAisButton"));
                            }
                            if (!csAisIsInstalled) {
                                pmStartedInfo.setText(
                                    initScript + Tools.getString(
                                   "Dialog.Cluster.Init.CsAisIsNotInstalled"));
                                csAisStartButton.setEnabled(false);
                            } else if (!csAisIsConf) {
                                pmStartedInfo.setText(
                                   initScript + Tools.getString(
                                   "Dialog.Cluster.Init.CsAisIsNotConfigured"));
                                csAisStartButton.setEnabled(false);
                            } else {
                                pmStartedInfo.setText(
                                   initScript + Tools.getString(
                                        "Dialog.Cluster.Init.CsAisIsStopped"));
                                if (heartbeatIsRunning) {
                                    csAisStartButton.setEnabled(false);
                                } else {
                                    Tools.getGUIData().setAccessible(
                                                    csAisStartButton,
                                                    ConfigData.AccessType.OP);
                                }
                            }
                            csAisStartButton.setVisible(true);
                            pmStartedInfo.setForeground(Color.RED);
                        }
                    });
                }
            }

            /* Heartbeat */
            final JLabel hbStartedInfo = hbStartedInfos.get(i);
            final MyButton hbStartButton = hbStartButtons.get(i);
            boolean hbFailed = false;
            if (heartbeatIsRunning) {
                if (hbChanged || csAisChanged) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            hbStartedInfo.setText(Tools.getString(
                                           "Dialog.Cluster.Init.HbIsRunning"));
                            hbStartedInfo.setForeground(Color.BLACK);
                            if (heartbeatIsRc) {
                                hbStartButton.setVisible(false);
                                hbStartedInfo.setText(Tools.getString(
                                                "Dialog.Cluster.Init.HbIsRc"));
                            } else if (csAisRunning || csAisIsRc) {
                                hbStartButton.setText(HB_BUTTON_SWITCH);
                                hbStartButton.setVisible(true);
                            } else {
                                hbStartButton.setText(Tools.getString(
                                            "Dialog.Cluster.Init.HbButtonRc"));
                                hbStartButton.setVisible(true);
                            }
                        }
                    });
                }
            } else {
                hbFailed = true;
                if (hbChanged || csAisChanged) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (csAisRunning || csAisIsRc) {
                                hbStartButton.setText(HB_BUTTON_SWITCH);
                            } else {
                                hbStartButton.setText(Tools.getString(
                                         "Dialog.Cluster.Init.StartHbButton"));
                            }
                            if (!heartbeatIsInstalled) {
                                hbStartedInfo.setText(Tools.getString(
                                       "Dialog.Cluster.Init.HbIsNotInstalled"));
                                hbStartButton.setEnabled(false);
                            } else if (!heartbeatIsConf) {
                                hbStartedInfo.setText(Tools.getString(
                                     "Dialog.Cluster.Init.HbIsNotConfigured"));
                                hbStartButton.setEnabled(false);
                            } else {
                                hbStartedInfo.setText(Tools.getString(
                                            "Dialog.Cluster.Init.HbIsStopped"));
                                if (csAisRunning) {
                                    hbStartButton.setEnabled(false);
                                } else {
                                    Tools.getGUIData().setAccessible(
                                                    hbStartButton,
                                                    ConfigData.AccessType.OP);
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
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                useOpenaisButton.setEnabled(nob);
            }
        });

        if (oneChanged || !periodic) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    mainPanel.invalidate();
                    mainPanel.validate();
                    mainPanel.repaint();
                }
            });

            if (oneFailed) {
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        buttonClass(button).setEnabled(false);
                    }
                });
                nextButtonSetEnabled(false);
            } else {
                nextButtonSetEnabled(true);
            }
            enableComponents();
            if (!Tools.getConfigData().getAutoClusters().isEmpty()) {
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
        drbdLoadedInfos = new ArrayList<JLabel>();
        drbdLoadButtons = new ArrayList<MyButton>();

        pmStartedInfos = new ArrayList<JLabel>();
        pmStartButtons = new ArrayList<MyButton>();

        hbStartedInfos = new ArrayList<JLabel>();
        hbStartButtons = new ArrayList<MyButton>();

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
            pane.setAlignmentX(Component.LEFT_ALIGNMENT);

            final TitledBorder titledBorder =
                            BorderFactory.createTitledBorder(host.getName());
            titledBorder.setTitleJustification(TitledBorder.LEFT);
            pane.setBorder(titledBorder);

            drbdLoadedInfos.add(new JLabel(
                        Tools.getString("Dialog.Cluster.Init.CheckingDrbd")));
            final MyButton drbdb = new MyButton(
                       Tools.getString("Dialog.Cluster.Init.LoadDrbdButton"));
            drbdb.setBackgroundColor(
                               Tools.getDefaultColor("ConfigDialog.Button"));
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
                                    Tools.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            drbdLoadButtons.get(
                                                      index).setVisible(false);
                                        }
                                    });
                                    final boolean testOnly = false;
                                    DRBD.load(host, testOnly);
                                    if (host.isDrbdUpgraded()) {
                                        DRBD.adjustApply(host,
                                                    DRBD.ALL,
                                                    null,
                                                    testOnly);
                                    }
                                    checkCluster(false);
                                }
                            }
                        );
                        thread.start();
                    }
                });

            pane.add(drbdLoadedInfos.get(i));
            pane.add(drbdLoadButtons.get(i));

            /* Heartbeat */
            hbStartedInfos.add(new JLabel(
                        Tools.getString("Dialog.Cluster.Init.CheckingHb")));
            final MyButton btn;
            if (host.isCsRunning()
                || host.isAisRunning()
                || host.isCsRc()
                || host.isAisRc()) {
                btn = new MyButton(HB_BUTTON_SWITCH);
            } else {
                btn = new MyButton(
                         Tools.getString("Dialog.Cluster.Init.StartHbButton"));
            }
            btn.setBackgroundColor(
                               Tools.getDefaultColor("ConfigDialog.Button"));
            hbStartButtons.add(btn);
            hbStartButtons.get(i).setVisible(false);

            hbStartButtons.get(i).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    disableComponents();
                                    Tools.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            hbStartButtons.get(
                                                      index).setVisible(false);
                                        }
                                    });
                                    if (Tools.getString(
                                      "Dialog.Cluster.Init.HbButtonRc").equals(
                                        e.getActionCommand())) {
                                        Heartbeat.addHeartbeatToRc(host);
                                    } else if (useCorosync(host)
                                               && HB_BUTTON_SWITCH.equals(
                                                       e.getActionCommand())) {
                                        Heartbeat.switchFromCorosyncToHeartbeat(
                                                                          host);
                                    } else if (!useCorosync(host)
                                               && HB_BUTTON_SWITCH.equals(
                                                       e.getActionCommand())) {
                                        Heartbeat.switchFromOpenaisToHeartbeat(
                                                                          host);
                                    } else {
                                        if (host.isHeartbeatRc()) {
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

            pane.add(hbStartedInfos.get(i));
            pane.add(hbStartButtons.get(i));


            /* Pacemaker */
            pmStartedInfos.add(new JLabel(
                        Tools.getString("Dialog.Cluster.Init.CheckingPm")));
            final MyButton pmsb = new MyButton(
                      Tools.getString("Dialog.Cluster.Init.StartCsAisButton"));
            pmsb.setBackgroundColor(
                               Tools.getDefaultColor("ConfigDialog.Button"));
            pmStartButtons.add(pmsb);
            pmStartButtons.get(i).setVisible(false);

            pmStartButtons.get(i).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    disableComponents();
                                    Tools.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            pmStartButtons.get(
                                                      index).setVisible(false);
                                        }
                                    });
                                    if (Tools.getString(
                                   "Dialog.Cluster.Init.CsAisButtonRc").equals(
                                        e.getActionCommand())) {
                                        if (useCorosync(host)) {
                                            Corosync.addCorosyncToRc(host);
                                        } else {
                                            Openais.addOpenaisToRc(host);
                                        }
                                    } else if (CS_AIS_BUTTON_SWITCH.equals(
                                                    e.getActionCommand())) {
                                        if (useCorosync(host)) {
                                            Corosync.switchToCorosync(host);
                                        } else {
                                            Openais.switchToOpenais(host);
                                        }
                                    } else {
                                        if (host.isCsRc() || host.isAisRc()) {
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
                            }
                        );
                        thread.start();
                    }
                });
            if (host.isCsRunning() && host.isAisRunning()) {
                /* started with openais init script. */
                oneStartedAsOpenais = true;
            }
            if (!host.isCsInit()) {
                noCorosync = true;
            }

            pane.add(pmStartedInfos.get(i));
            pane.add(pmStartButtons.get(i));
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

    /** Enable skip button. */
    @Override
    protected final boolean skipButtonEnabled() {
        return true;
    }

    /** Whether to use corosync or openais init script. */
    private boolean useCorosync(final Host host) {
        return !(!host.isCorosync() || !host.isCsInit())
               && (host.isCsInit() && COROSYNC_INIT_SCRIPT.equals(useOpenaisButton.getStringValue())
               || !host.isAisInit());
    }
}
