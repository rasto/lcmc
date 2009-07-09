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


package drbd.gui.dialog;

import drbd.data.Host;
import drbd.data.Cluster;
import drbd.utilities.Tools;
import drbd.utilities.DRBD;
import drbd.utilities.Heartbeat;
import drbd.utilities.Openais;
import drbd.utilities.SSH.ExecCommandThread;
import drbd.utilities.MyButton;
import drbd.gui.SpringUtilities;
import drbd.utilities.ExecCallback;
import drbd.gui.ProgressBar;
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
public class ClusterInit extends DialogCluster {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** List with texts if drbd is loaded per host. */
    private List<JLabel> drbdLoadedInfos;
    /** List of load drbd buttons. */
    private List<MyButton> drbdLoadButtons;

    /** List with texts whether openais is started per host. */
    private List<JLabel> aisStartedInfos;
    /** List of start openais buttons. */
    private List<MyButton> aisStartButtons;
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
    /** Last value of openais-is-started check. */
    private Boolean[] lastAisStarted;
    /** Last value of openais-is-in-rc check. */
    private Boolean[] lastAisRc;
    /** Last value of openais-is-installed check. */
    private Boolean[] lastAisInstalled;
    /** Last value of openais-is-configured check. */
    private Boolean[] lastAisConf;
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

    /**
     * Prepares a new <code>ClusterInit</code> object.
     */
    public ClusterInit(final WizardDialog previousDialog,
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
    protected final WizardDialog getPreviousDialog() {
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
        return new ClusterFinish(this, getCluster());
    }

    /**
     * Returns the title of the dialog.
     */
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.ClusterInit.Title");
    }

    /**
     * Returns the description of the dialog.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.ClusterInit.Description");
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();

        enableComponentsLater(new JComponent[]{});
        lastDrbdLoaded = null;
        lastAisStarted = null;
        lastAisRc      = null;
        lastHbStarted  = null;
        lastHbRc       = null;
        checkClusterThread = new Thread(
            new Runnable() {
                public void run() {
                    while (!checkClusterStopped) {
                        checkCluster();
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
    private void checkCluster() {
        /* check if modules are loaded. */
        final Host[] hosts = getCluster().getHostsArray();
        final ExecCommandThread[] infoThreads =
                                        new ExecCommandThread[hosts.length];
        int i = 0;
        for (final Host h : hosts) {
            final int index = i;
            infoThreads[i] = h.execCommand("ClusterInit.getInstallationInfo",
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
        final boolean lastAisStartedExists = (lastAisStarted != null);
        if (!lastAisStartedExists) {
            lastAisStarted = new Boolean[hosts.length];
            lastAisRc = new Boolean[hosts.length];
            lastAisConf = new Boolean[hosts.length];
            lastAisInstalled = new Boolean[hosts.length];
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
            boolean aisFailed = false;
            boolean hbFailed = false;
            /* is drbd loaded */
            boolean drbdLoadedChanged = false;
            boolean drbdLoaded = h.isDrbdLoaded();

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
                                           "Dialog.ClusterInit.DrbdIsLoaded"));
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
                                        "Dialog.ClusterInit.DrbdIsNotLoaded"));
                            drbdLoadedInfo.setForeground(Color.RED);
                            drbdLoadButton.setVisible(true);
                        }
                    });
                }
            }

            final boolean openaisIsInstalled = h.getOpenaisVersion() != null;
            final boolean openaisRunning     = h.isOpenaisRunning();
            final boolean openaisIsRc        = h.isOpenaisRc();
            final boolean openaisIsConf      = h.isOpenaisConf();

            final boolean heartbeatIsInstalled =
                                            h.getHeartbeatVersion() != null;
            final boolean heartbeatIsRunning   = h.isHeartbeatRunning();
            final boolean heartbeatIsRc      = h.isHeartbeatRc();
            final boolean heartbeatIsConf      = h.isHeartbeatConf();

            boolean hbChanged = false;
            boolean openaisChanged = false;
            if (lastAisStartedExists) {
                if (lastAisStarted[i].booleanValue() != openaisRunning) {
                    oneChanged = true;
                    openaisChanged = true;
                    lastAisStarted[i] = openaisRunning;
                }
                if (lastAisRc[i].booleanValue() != openaisIsRc) {
                    oneChanged = true;
                    openaisChanged = true;
                    lastAisRc[i] = openaisIsRc;
                }
                if (lastAisConf[i].booleanValue() != openaisIsConf) {
                    oneChanged = true;
                    openaisChanged = true;
                    lastAisConf[i] = openaisIsConf;
                }
                if (lastAisInstalled[i].booleanValue() != openaisIsInstalled) {
                    oneChanged = true;
                    openaisChanged = true;
                    lastAisInstalled[i] = openaisIsInstalled;
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
                openaisChanged = true;
                lastAisStarted[i] = openaisRunning;
                lastAisRc[i] = openaisIsRc;
                lastAisConf[i] = openaisIsConf;
                lastAisInstalled[i] = openaisIsInstalled;

                hbChanged = true;
                lastHbStarted[i] = heartbeatIsRunning;
                lastHbRc[i] = heartbeatIsRc;
                lastHbConf[i] = heartbeatIsConf;
                lastHbInstalled[i] = heartbeatIsInstalled;
            }

            /* Openais */
            final JLabel aisStartedInfo = aisStartedInfos.get(i);
            final MyButton aisStartButton = aisStartButtons.get(i);
            if (openaisRunning) {
                if (openaisChanged || hbChanged) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            aisStartedInfo.setText(Tools.getString(
                                           "Dialog.ClusterInit.AisIsRunning"));
                            aisStartedInfo.setForeground(Color.BLACK);
                            if (openaisIsRc) {
                                aisStartButton.setVisible(false);
                                aisStartedInfo.setText(
                                        Tools.getString(
                                                "Dialog.ClusterInit.AisIsRc"));
                            } else if (heartbeatIsRunning || heartbeatIsRc) {
                                aisStartButton.setText(Tools.getString(
                                        "Dialog.ClusterInit.AisButtonSwitch"));
                                aisStartButton.setVisible(true);
                            } else {
                                aisStartButton.setText(
                                          Tools.getString(
                                            "Dialog.ClusterInit.AisButtonRc"));
                                aisStartButton.setVisible(true);
                            }
                        }
                    });
                }
            } else {
                aisFailed = true;
                if (openaisChanged || hbChanged) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (heartbeatIsRunning || heartbeatIsRc) {
                                aisStartButton.setText(Tools.getString(
                                        "Dialog.ClusterInit.AisButtonSwitch"));
                            } else {
                                aisStartButton.setText(Tools.getString(
                                         "Dialog.ClusterInit.StartAisButton"));
                            }
                            if (!openaisIsInstalled) {
                                aisStartedInfo.setText(Tools.getString(
                                       "Dialog.ClusterInit.AisIsNotInstalled"));
                                aisStartButton.setEnabled(false);
                            } else if (!openaisIsConf) {
                                aisStartedInfo.setText(Tools.getString(
                                     "Dialog.ClusterInit.AisIsNotConfigured"));
                                aisStartButton.setEnabled(false);
                            } else {
                                aisStartedInfo.setText(Tools.getString(
                                           "Dialog.ClusterInit.AisIsStopped"));
                                aisStartButton.setEnabled(true);
                            }
                            aisStartButton.setVisible(true);
                            aisStartedInfo.setForeground(Color.RED);
                        }
                    });
                }
            }
        
            /* Heartbeat */
            final JLabel hbStartedInfo = hbStartedInfos.get(i);
            final MyButton hbStartButton = hbStartButtons.get(i);
            if (heartbeatIsRunning) {
                if (hbChanged || openaisChanged) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            hbStartedInfo.setText(Tools.getString(
                                            "Dialog.ClusterInit.HbIsRunning"));
                            hbStartedInfo.setForeground(Color.BLACK);
                            if (heartbeatIsRc) {
                                hbStartButton.setVisible(false);
                                hbStartedInfo.setText(Tools.getString(
                                                "Dialog.ClusterInit.HbIsRc"));
                            } else if (openaisRunning || openaisIsRc) {
                                hbStartButton.setText(Tools.getString(
                                          "Dialog.ClusterInit.HbButtonSwitch"));
                                hbStartButton.setVisible(true);
                            } else {
                                hbStartButton.setText(Tools.getString(
                                            "Dialog.ClusterInit.HbButtonRc"));
                                hbStartButton.setVisible(true);
                            }
                        }
                    });
                }
            } else {
                hbFailed = true;
                if (hbChanged || openaisChanged) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (openaisRunning || openaisIsRc) {
                                hbStartButton.setText(Tools.getString(
                                         "Dialog.ClusterInit.HbButtonSwitch"));
                            } else {
                                hbStartButton.setText(Tools.getString(
                                         "Dialog.ClusterInit.StartHbButton"));
                            }
                            if (!heartbeatIsInstalled) {
                                hbStartedInfo.setText(Tools.getString(
                                       "Dialog.ClusterInit.HbIsNotInstalled"));
                                hbStartButton.setEnabled(false);
                            } else if (!heartbeatIsConf) {
                                hbStartedInfo.setText(Tools.getString(
                                     "Dialog.ClusterInit.HbIsNotConfigured"));
                                hbStartButton.setEnabled(false);
                            } else {
                                hbStartedInfo.setText(Tools.getString(
                                            "Dialog.ClusterInit.HbIsStopped"));
                                hbStartButton.setEnabled(true);
                            }
                            hbStartButton.setVisible(true);
                            hbStartedInfo.setForeground(Color.RED);
                        }
                    });
                }
            }
            if (drbdFailed || (aisFailed && hbFailed)) {
                oneFailed = true;
            }
            i++;
        }

        if (oneChanged) {
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

        aisStartedInfos = new ArrayList<JLabel>();
        aisStartButtons = new ArrayList<MyButton>();

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
                        Tools.getString("Dialog.ClusterInit.CheckingDrbd")));
            drbdLoadButtons.add(new MyButton(
                        Tools.getString("Dialog.ClusterInit.LoadDrbdButton")));
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
                                    DRBD.load(host);
                                    if (host.isDrbdUpgraded()) {
                                        DRBD.adjust(host, "all");
                                    }
                                    checkCluster();
                                }
                            }
                        );
                        thread.start();
                    }
                });

            pane.add(drbdLoadedInfos.get(i));
            pane.add(drbdLoadButtons.get(i));

            /* OpenAIS */
            aisStartedInfos.add(new JLabel(
                        Tools.getString("Dialog.ClusterInit.CheckingAis")));
            aisStartButtons.add(new MyButton(
                        Tools.getString("Dialog.ClusterInit.StartAisButton")));
            aisStartButtons.get(i).setVisible(false);

            aisStartButtons.get(i).addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                public void run() {
                                    disableComponents();
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            aisStartButtons.get(
                                                      index).setVisible(false);
                                        }
                                    });
                                    if (Tools.getString(
                                      "Dialog.ClusterInit.AisButtonRc").equals(
                                        e.getActionCommand())) {
                                        Openais.addOpenaisToRc(host);
                                    } else if (Tools.getString(
                                    "Dialog.ClusterInit.AisButtonSwitch").equals(
                                        e.getActionCommand())) {
                                        Openais.switchToOpenais(host);
                                    } else {
                                        if (host.isOpenaisRc()) {
                                            Openais.startOpenais(host);
                                        } else {
                                            Openais.startOpenaisRc(host);
                                        }
                                    }
                                    checkCluster();
                                }
                            }
                        );
                        thread.start();
                    }
                });

            pane.add(aisStartedInfos.get(i));
            pane.add(aisStartButtons.get(i));

            /* Heartbeat */
            hbStartedInfos.add(new JLabel(
                        Tools.getString("Dialog.ClusterInit.CheckingHb")));
            System.out.println(host.getName() + ": is openais running: " + host.isOpenaisRunning());
            if (host.isOpenaisRunning() || host.isOpenaisRc()) {
                hbStartButtons.add(new MyButton(
                        Tools.getString("Dialog.ClusterInit.HbButtonSwitch")));
            } else {
                hbStartButtons.add(new MyButton(
                         Tools.getString("Dialog.ClusterInit.StartHbButton")));
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
                                      "Dialog.ClusterInit.HbButtonRc").equals(
                                        e.getActionCommand())) {
                                        Heartbeat.addHeartbeatToRc(host);
                                    } else if (Tools.getString(
                                    "Dialog.ClusterInit.HbButtonSwitch").equals(
                                        e.getActionCommand())) {
                                        System.out.println("switching");
                                        Heartbeat.switchToHeartbeat(host);
                                        System.out.println("switching done");
                                    } else {
                                        if (host.isHeartbeatRc()) {
                                            Heartbeat.startHeartbeat(host);
                                        } else {
                                            Heartbeat.startHeartbeatRc(host);
                                        }
                                    }
                                    checkCluster();
                                }
                            }
                        );
                        thread.start();
                    }
                });

            pane.add(hbStartedInfos.get(i));
            pane.add(hbStartButtons.get(i));

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
