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


package drbd.gui.dialog.cluster;

import drbd.data.Host;
import drbd.data.Cluster;
import drbd.utilities.Tools;
import drbd.utilities.DRBD;
import drbd.utilities.Heartbeat;
import drbd.utilities.Openais;
import drbd.utilities.Corosync;
import drbd.utilities.SSH.ExecCommandThread;
import drbd.utilities.MyButton;
import drbd.gui.SpringUtilities;
import drbd.utilities.ExecCallback;
import drbd.gui.ProgressBar;
import drbd.gui.dialog.WizardDialog;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Color;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;

/**
 * An implementation of a dialog where heartbeat is initialized on all hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class Init extends DialogCluster {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
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

    /**
     * Prepares a new <code>Init</code> object.
     */
    public Init(final WizardDialog previousDialog,
                final Cluster cluster) {
        super(previousDialog, cluster);
        setButton(finishButton());
    }

    /**
     * Sets button. Which acts as a finish button.
     */
    private void setButton(final String button) {
        this.button = button;
    }

    /**
     * Stops the checks and waits for them to stop.
     */
    private void stopCheckCluster() {
        checkClusterStopped = true;
    }

    /**
     * Returns previous dialog. It is used to get with the back button to
     * the dialog before this one.
     */
    public final WizardDialog getPreviousDialog() {
        stopCheckCluster();
        return super.getPreviousDialog();
    }

    /**
     * After the dialog is finished.
     */
    protected final void finishDialog() {
        stopCheckCluster();
    }

    /**
     * Is called before the dialog is canceled. It stops all the checks.
     */
    public final void cancelDialog() {
        stopCheckCluster();
    }

    /**
     * Returns the next dialog.
     */
    public final WizardDialog nextDialog() {
        stopCheckCluster();
        return new Finish(this, getCluster());
    }

    /**
     * Returns the title of the dialog.
     */
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.Init.Title");
    }

    /**
     * Returns the description of the dialog.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.Cluster.Init.Description");
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();

        enableComponentsLater(new JComponent[]{});
        lastDrbdLoaded = null;
        lastPmStarted = null;
        lastPmRc      = null;
        lastHbStarted  = null;
        lastHbRc       = null;
        checkClusterThread = new Thread(
            new Runnable() {
                public void run() {
                    while (!checkClusterStopped) {
                        checkCluster(true);
                        if (!checkClusterStopped) {
                            try {
                                Thread.sleep(CHECK_INTERVAL);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                    checkClusterThread = null;
                }
            });
        checkClusterThread.start();
    }

    /**
     * Checks drbds and heartbeats on all nodes of the cluster.
     */
    private void checkCluster(final boolean periodic) {
        /* check if modules are loaded. */
        final Host[] hosts = getCluster().getHostsArray();
        final ExecCommandThread[] infoThreads =
                                        new ExecCommandThread[hosts.length];
        int i = 0;
        for (final Host h : hosts) {
            infoThreads[i] = h.execCommand("Cluster.Init.getInstallationInfo",
                             (ProgressBar) null,
                             new ExecCallback() {
                                 public void done(final String ans) {
                                     //drbdLoaded[index] = true;
                                     for (final String line
                                                    : ans.split("\\r?\\n")) {
                                         h.parseInstallationInfo(line);
                                     }
                                 }
                                 public void doneError(final String ans,
                                                       final int exitCode) {
                                     Tools.appWarning(
                                                "could not get install info");
                                 }
                             },
                             null,   /* ConvertCmdCallback */
                             false); /* outputVisible */
            i++;
        }
        for (final ExecCommandThread t : infoThreads) {
            /* wait for all of them */
            try {
                t.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /* DRBD */
        i = 0;
        boolean oneFailed = false;
        boolean oneChanged = false;
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
        for (final Host h : hosts) {
            boolean drbdFailed = false;
            boolean csAisFailed = false;
            boolean hbFailed = false;
            /* is drbd loaded */
            boolean drbdLoadedChanged = false;
            final boolean drbdLoaded = h.isDrbdLoaded();

            if (lastDrbdLoadedExists) {
                if (lastDrbdLoaded[i].booleanValue() != drbdLoaded) {
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
            if (drbdLoaded) {
                if (drbdLoadedChanged) {
                    SwingUtilities.invokeLater(new Runnable() {
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
                    SwingUtilities.invokeLater(new Runnable() {
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
            final boolean csAisRunning     = h.isCsAisRunning();
            final boolean csAisIsRc        = h.isCsAisRc();
            final boolean csAisIsConf      = h.isCsAisConf();

            final boolean heartbeatIsInstalled =
                                            h.getHeartbeatVersion() != null;
            final boolean heartbeatIsRunning   = h.isHeartbeatRunning();
            final boolean heartbeatIsRc      = h.isHeartbeatRc();
            final boolean heartbeatIsConf      = h.isHeartbeatConf();

            boolean hbChanged = false;
            boolean csAisChanged = false;
            if (lastPmStartedExists) {
                if (lastPmStarted[i].booleanValue() != csAisRunning) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPmStarted[i] = csAisRunning;
                }
                if (lastPmRc[i].booleanValue() != csAisIsRc) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPmRc[i] = csAisIsRc;
                }
                if (lastPmConf[i].booleanValue() != csAisIsConf) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPmConf[i] = csAisIsConf;
                }
                if (lastPmInstalled[i].booleanValue() != csAisIsInstalled) {
                    oneChanged = true;
                    csAisChanged = true;
                    lastPmInstalled[i] = csAisIsInstalled;
                }

                if (lastHbStarted[i].booleanValue() != heartbeatIsRunning) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHbStarted[i] = heartbeatIsRunning;
                }
                if (lastHbRc[i].booleanValue() != heartbeatIsRc) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHbRc[i] = heartbeatIsRc;
                }
                if (lastHbConf[i].booleanValue() != heartbeatIsConf) {
                    oneChanged = true;
                    hbChanged = true;
                    lastHbConf[i] = heartbeatIsConf;
                }
                if (lastHbInstalled[i].booleanValue() != heartbeatIsInstalled) {
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
            if (!h.isCorosync() && h.getOpenaisVersion() != null) {
                is = "OpenAIS";
            }
            final String initScript = is;
            if (csAisRunning) {
                if (csAisChanged || hbChanged) {
                    SwingUtilities.invokeLater(new Runnable() {
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
                    SwingUtilities.invokeLater(new Runnable() {
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
                                csAisStartButton.setEnabled(
                                                          !heartbeatIsRunning);
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
            if (heartbeatIsRunning) {
                if (hbChanged || csAisChanged) {
                    SwingUtilities.invokeLater(new Runnable() {
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
                    SwingUtilities.invokeLater(new Runnable() {
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
                                hbStartButton.setEnabled(!csAisRunning);
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

        if (oneChanged || !periodic) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    mainPanel.invalidate();
                    mainPanel.validate();
                    mainPanel.repaint();
                }
            });

            if (oneFailed) {
                SwingUtilities.invokeLater(new Runnable() {
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
    protected final JComponent getInputPane() {
        /* Waiting for check cluster thread to finish. To avoid all races. This
         * can happen after clicking the back button from the next dialog. */
        final Thread t = checkClusterThread;
        if (t != null) {
            try {
                t.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        checkClusterStopped = false;
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
            drbdLoadButtons.add(new MyButton(
                       Tools.getString("Dialog.Cluster.Init.LoadDrbdButton")));
            drbdLoadButtons.get(i).setVisible(false);

            drbdLoadButtons.get(i).addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                public void run() {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            drbdLoadButtons.get(
                                                      index).setVisible(false);
                                        }
                                    });
                                    final boolean testOnly = false;
                                    DRBD.load(host, testOnly);
                                    if (host.isDrbdUpgraded()) {
                                        DRBD.adjust(host, "all", testOnly);
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
            if (host.isCsAisRunning() || host.isCsAisRc()) {
                hbStartButtons.add(new MyButton(HB_BUTTON_SWITCH));
            } else {
                hbStartButtons.add(new MyButton(
                         Tools.getString("Dialog.Cluster.Init.StartHbButton")));
            }
            hbStartButtons.get(i).setVisible(false);

            hbStartButtons.get(i).addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                public void run() {
                                    disableComponents();
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            hbStartButtons.get(
                                                      index).setVisible(false);
                                        }
                                    });
                                    if (Tools.getString(
                                      "Dialog.Cluster.Init.HbButtonRc").equals(
                                        e.getActionCommand())) {
                                        Heartbeat.addHeartbeatToRc(host);
                                    } else if (host.isCorosync()
                                               && HB_BUTTON_SWITCH.equals(
                                                       e.getActionCommand())) {
                                        Heartbeat.switchFromCorosyncToHeartbeat(
                                                                          host);
                                    } else if (!host.isCorosync()
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
            pmStartButtons.add(new MyButton(
                      Tools.getString("Dialog.Cluster.Init.StartCsAisButton")));
            pmStartButtons.get(i).setVisible(false);

            pmStartButtons.get(i).addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                public void run() {
                                    disableComponents();
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            pmStartButtons.get(
                                                      index).setVisible(false);
                                        }
                                    });
                                    if (Tools.getString(
                                   "Dialog.Cluster.Init.CsAisButtonRc").equals(
                                        e.getActionCommand())) {
                                        if (host.isCorosync()) {
                                            Corosync.addCorosyncToRc(host);
                                        } else {
                                            Openais.addOpenaisToRc(host);
                                        }
                                    } else if (CS_AIS_BUTTON_SWITCH.equals(
                                                    e.getActionCommand())) {
                                        if (host.isCorosync()) {
                                            Corosync.switchToCorosync(host);
                                        } else {
                                            Openais.switchToOpenais(host);
                                        }
                                    } else {
                                        if (host.isCsAisRc()) {
                                            if (host.isCorosync()) {
                                                Corosync.startCorosync(host);
                                            } else {
                                                Openais.startOpenais(host);
                                            }
                                        } else {
                                            if (host.isCorosync()) {
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

            pane.add(pmStartedInfos.get(i));
            pane.add(pmStartButtons.get(i));
            i++;
            SpringUtilities.makeCompactGrid(pane, 3, 2,  //rows, cols
                                                  1, 1,  //initX, initY
                                                  1, 0); //xPad, yPad
            mainPanel.add(pane);
        }
        return new JScrollPane(mainPanel);
    }

    /**
     * Enable skip button.
     */
    protected final boolean skipButtonEnabled() {
        return true;
    }
}
