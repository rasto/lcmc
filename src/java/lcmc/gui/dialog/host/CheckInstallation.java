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

package lcmc.gui.dialog.host;

import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH;
import lcmc.gui.SpringUtilities;
import lcmc.gui.GuiComboBox;
import lcmc.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

/**
 * An implementation of a dialog where
 * drbd/heartbeat/pacemaker/openais/corosync etc. installation is checked.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class CheckInstallation extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /** Checking drbd label. */
    private final JLabel drbdLabel = new JLabel(
          ": " + Tools.getString("Dialog.Host.CheckInstallation.CheckingDrbd"));
    /** Checking corosync/openais/pacemaker label. */
    private final JLabel pmLabel = new JLabel(
          ": " + Tools.getString("Dialog.Host.CheckInstallation.CheckingPm"));
    /** Checking heartbeat/pacemaker label. */
    private final JLabel hbPmLabel = new JLabel(
          ": " + Tools.getString("Dialog.Host.CheckInstallation.CheckingHbPm"));

    /** Install/Upgrade drbd button. */
    private final MyButton drbdButton = new MyButton(
            Tools.getString("Dialog.Host.CheckInstallation.DrbdInstallButton"));
    /** Install corosync/openais/pacemaker button. */
    private final MyButton pmButton = new MyButton(
            Tools.getString("Dialog.Host.CheckInstallation.PmInstallButton"));
    /** Install heartbeat/pacemaker button. */
    private final MyButton hbPmButton = new MyButton(
            Tools.getString("Dialog.Host.CheckInstallation.HbPmInstallButton"));
    /** Corosync/Openais/Pacemaker installation method. */
    private GuiComboBox pmInstMethodCB;
    /** Heartbeat/pacemaker installation method. */
    private GuiComboBox hbPmInstMethodCB;
    /** DRBD installation method. */
    private GuiComboBox drbdInstMethodCB;

    /** Checking icon. */
    private static final ImageIcon CHECKING_ICON =
        Tools.createImageIcon(
                Tools.getDefault("Dialog.Host.CheckInstallation.CheckingIcon"));
    /** Not installed icon. */
    private static final ImageIcon NOT_INSTALLED_ICON =
        Tools.createImageIcon(
            Tools.getDefault("Dialog.Host.CheckInstallation.NotInstalledIcon"));
    /** Already installed icon. */
    private static final ImageIcon INSTALLED_ICON =
        Tools.createImageIcon(
              Tools.getDefault("Dialog.Host.CheckInstallation.InstalledIcon"));
    /** Upgrade available icon. */
    private static final ImageIcon UPGR_AVAIL_ICON =
        Tools.createImageIcon(
              Tools.getDefault("Dialog.Host.CheckInstallation.UpgrAvailIcon"));

    /** Drbd icon: checking ... */
    private final JLabel drbdIcon = new JLabel(CHECKING_ICON);
    /** Corosync/openais/pacemaker icon: checking ... */
    private final JLabel pmIcon = new JLabel(CHECKING_ICON);
    /** Heartbeat/pacemaker icon: checking ... */
    private final JLabel hbPmIcon = new JLabel(CHECKING_ICON);
    /** Whether drbd installation was ok. */
    private boolean drbdOk = false;
    /** Whether corosync/openais/pacemaker installation was ok. */
    private boolean pmOk = false;
    /** Whether heartbeat/pacemaker installation was ok. */
    private boolean hbPmOk = false;
    /** Whether there are drbd methods available. */
    private boolean drbdInstallMethodsAvailable = false;
    /** Label of heartbeat that can be with or without pacemaker. */
    private final JLabel hbPmJLabel = new JLabel("Pacemaker/HB");
    /** Label of pacemaker that can be with corosync or openais. */
    private final JLabel pmJLabel = new JLabel("Pacemaker/Coro");

    /** Prepares a new <code>CheckInstallation</code> object. */
    CheckInstallation(final WizardDialog previousDialog,
                      final Host host) {
        super(previousDialog, host);
    }

    /** Inits dialog. */
    @Override protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{});
    }

    /** Inits the dialog. */
    @Override protected void initDialogAfterVisible() {
        drbdOk = false;
        pmOk = false;
        hbPmOk = false;

        nextDialogObject = new Finish(this, getHost());
        final CheckInstallation thisClass = this;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                drbdButton.setBackgroundColor(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));
                drbdButton.setEnabled(false);
                drbdInstMethodCB.setEnabled(false);
                pmButton.setEnabled(false);
                pmInstMethodCB.setEnabled(false);
                hbPmButton.setEnabled(false);
                hbPmInstMethodCB.setEnabled(false);
            }
        });
        drbdButton.addActionListener(
            new ActionListener() {
                @Override public void actionPerformed(final ActionEvent e) {
                    if (drbdOk) {
                        getHost().setDrbdWillBeUpgraded(true);
                    }
                    final InstallMethods im =
                                  (InstallMethods) drbdInstMethodCB.getValue();
                    getHost().setDrbdInstallMethod(im.getIndex());
                    final String button = e.getActionCommand();
                    if (!drbdOk || button.equals(Tools.getString(
                  "Dialog.Host.CheckInstallation.DrbdCheckForUpgradeButton"))) {
                        if (im.isLinbitMethod()) {
                            nextDialogObject =
                                new DrbdLinbitAvailPackages(thisClass,
                                                            getHost());
                        } else if (im.isSourceMethod()) {
                           nextDialogObject =
                               new DrbdAvailSourceFiles(thisClass, getHost());
                        } else {
                            // TODO: this only when there is no drbd installed
                            nextDialogObject = new DrbdCommandInst(
                                                        thisClass, getHost());
                            getHost().setDrbdInstallMethod(im.getIndex());
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                buttonClass(nextButton()).pressButton();
                            }
                        });
                    } else {
                        nextDialogObject =
                                 new LinbitLogin(thisClass, getHost());
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                buttonClass(nextButton()).pressButton();
                            }
                        });
                    }
                }
            }
        );

        hbPmButton.setBackgroundColor(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));
        hbPmButton.addActionListener(
            new ActionListener() {
                @Override public void actionPerformed(final ActionEvent e) {
                    nextDialogObject = new HeartbeatInst(thisClass, getHost());
                    final InstallMethods im =
                                   (InstallMethods) hbPmInstMethodCB.getValue();
                    getHost().setHbPmInstallMethod(im.getIndex());
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            buttonClass(nextButton()).pressButton();
                        }
                    });
                }
            }
        );

        pmButton.setBackgroundColor(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));
        pmButton.addActionListener(
            new ActionListener() {
                @Override public void actionPerformed(final ActionEvent e) {
                    nextDialogObject = new PacemakerInst(thisClass, getHost());
                    final InstallMethods im =
                                (InstallMethods) pmInstMethodCB.getValue();
                    getHost().setPmInstallMethod(im.getIndex());
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            buttonClass(nextButton()).pressButton();
                        }
                    });
                }
            }
        );

        getHost().execCommand("DrbdCheck.version",
                         getProgressBar(),
                         new ExecCallback() {
                             @Override public void done(final String ans) {
                                 checkDrbd(ans);
                             }
                             @Override public void doneError(
                                                         final String ans,
                                                         final int exitCode) {
                                 checkDrbd(""); // not installed
                             }
                         },
                         null,   /* ConvertCmdCallback */
                         false,  /* outputVisible */
                         SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /**
     * Checks whether drbd is installed and starts heartbeat/pacemaker check.
     */
    void checkDrbd(final String ans) {
        if ("".equals(ans) || "\n".equals(ans)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    drbdLabel.setText(": " + Tools.getString(
                            "Dialog.Host.CheckInstallation.DrbdNotInstalled"));
                    drbdIcon.setIcon(NOT_INSTALLED_ICON);
                    final String toolTip =
                                   getDrbdInstToolTip("1");
                    drbdInstMethodCB.setToolTipText(toolTip);
                    drbdButton.setToolTipText(toolTip);
                    if (drbdInstallMethodsAvailable) {
                        drbdButton.setEnabled(true);
                        drbdInstMethodCB.setEnabled(true);
                    }
                }
            });
        } else {
            drbdOk = true;
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    drbdLabel.setText(": " + ans.trim());
                    if (getHost().isDrbdUpgradeAvailable(ans.trim())) {
                        drbdIcon.setIcon(UPGR_AVAIL_ICON);
                        drbdButton.setText(Tools.getString(
                          "Dialog.Host.CheckInstallation.DrbdUpgradeButton"));
                        if (drbdInstallMethodsAvailable) {
                            drbdButton.setEnabled(true);
                            drbdInstMethodCB.setEnabled(true);
                        }
                    } else {
                        drbdButton.setText(Tools.getString(
                     "Dialog.Host.CheckInstallation.DrbdCheckForUpgradeButton"
                        ));
                        if (false && drbdInstallMethodsAvailable) {
                            // TODO: disabled
                            drbdButton.setEnabled(true);
                            drbdInstMethodCB.setEnabled(true);
                        }
                        drbdIcon.setIcon(INSTALLED_ICON);
                    }
                }
            });
        }
        getHost().execCommand("HbCheck.version",
                         getProgressBar(),
                         new ExecCallback() {
                             @Override public void done(final String ans) {
                                 Tools.debug(this, "ans: " + ans, 2);
                                 checkAisHbPm(ans);
                             }
                             @Override public void doneError(
                                                         final String ans,
                                                         final int exitCode) {
                                 done("");
                             }
                         },
                         null,   /* ConvertCmdCallback */
                         false,
                         SSH.DEFAULT_COMMAND_TIMEOUT); /* outputVisible */
    }

    /**
     * Checks whether heartbeat/pacemaker is installed and starts
     * openais/pacemaker check.
     */
    void checkAisHbPm(final String ans) {
        getHost().setPacemakerVersion(null);
        getHost().setOpenaisVersion(null);
        getHost().setHeartbeatVersion(null);
        getHost().setCorosyncVersion(null);
        if (!"".equals(ans) && !"\n".equals(ans)) {
            for (final String line : ans.split("\n")) {
                getHost().parseInstallationInfo(line);
            }
        }
        final String aisVersion = getHost().getOpenaisVersion();
        final String corosyncVersion = getHost().getCorosyncVersion();
        String hbVersion = getHost().getHeartbeatVersion();
        if (hbVersion == null
            && (getHost().getPacemakerVersion() == null
                || (corosyncVersion == null && aisVersion == null))) {
            final InstallMethods hbim =
                                  (InstallMethods) hbPmInstMethodCB.getValue();
            if (hbim != null) {
                hbPmButton.setEnabled(true);
                hbPmInstMethodCB.setEnabled(true);
                final String toolTip = getHbPmInstToolTip(hbim.getIndex());
                hbPmInstMethodCB.setToolTipText(toolTip);
                hbPmButton.setToolTipText(toolTip);
            }
            final InstallMethods pmim =
                                    (InstallMethods) pmInstMethodCB.getValue();
            if (pmim != null) {
                pmButton.setEnabled(true);
                pmInstMethodCB.setEnabled(true);
                final String aisToolTip = getPmInstToolTip(pmim.getIndex());
                pmInstMethodCB.setToolTipText(aisToolTip);
                pmButton.setToolTipText(aisToolTip);
            }
        }
        if (hbVersion == null) {
            /* hb */
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    hbPmIcon.setIcon(NOT_INSTALLED_ICON);
                    hbPmLabel.setText(": " + Tools.getString(
                             "Dialog.Host.CheckInstallation.HbPmNotInstalled"));
                }
            });
        } else {
            hbPmOk = true;
            String text;
            if ("2.1.3".equals(hbVersion)
                && "sles10".equals(getHost().getDistVersion())) {
                /* sles10 heartbeat 2.1.3 looks like hb 2.1.4 */
                hbVersion = "2.1.4";
                text = "2.1.3 (2.1.4)";
            } else {
                text = hbVersion;
            }
            final String hbVersionText = text;
            getHost().setHeartbeatVersion(hbVersion);
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    if (getHost().getPacemakerVersion() == null
                        || getHost().getHeartbeatVersion().equals(
                                            getHost().getPacemakerVersion())) {
                        hbPmJLabel.setText("Heartbeat");
                        hbPmLabel.setText(": " + hbVersionText);
                    } else {
                        hbPmLabel.setText(": "
                                          + getHost().getPacemakerVersion()
                                          + "/" + hbVersionText);
                    }
                    hbPmIcon.setIcon(INSTALLED_ICON);
                }
            });
        }
        if (getHost().getPacemakerVersion() == null
            || (aisVersion == null && corosyncVersion == null)) {
            /* corosync */
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    pmIcon.setIcon(NOT_INSTALLED_ICON);
                    pmLabel.setText(": " + Tools.getString(
                            "Dialog.Host.CheckInstallation.PmNotInstalled"));
                    pmJLabel.setText("Pacemaker/Coro");
                }
            });
        } else {
            pmOk = true;
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    String coroAisVersion = "no";
                    pmIcon.setIcon(INSTALLED_ICON);
                    if (corosyncVersion != null) {
                        pmJLabel.setText("Pacemaker/Coro");
                        coroAisVersion = corosyncVersion;
                    } else if (aisVersion != null) {
                        pmJLabel.setText("Pacemaker/AIS");
                        coroAisVersion = aisVersion;
                    }
                    pmJLabel.repaint();
                    pmLabel.setText(": "
                                       + getHost().getPacemakerVersion() + "/"
                                       + coroAisVersion);
                }
            });
        }

        if (drbdOk && (hbPmOk || pmOk)) {
            progressBarDone();
            nextButtonSetEnabled(true);
            enableComponents();
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    answerPaneSetText(Tools.getString(
                                       "Dialog.Host.CheckInstallation.AllOk"));
                }
            });
            if (Tools.getConfigData().getAutoOptionHost("drbdinst") != null
                || Tools.getConfigData().getAutoOptionHost("hbinst") != null
                || Tools.getConfigData().getAutoOptionHost("pminst") != null) {
                Tools.sleep(1000);
                pressNextButton();
            }
        } else {
            progressBarDoneError();
            Tools.debug(this, "drbd: " + drbdOk
                              + ", ais/pm: " + pmOk
                              + ", hb/pm: " + hbPmOk, 2);
            printErrorAndRetry(Tools.getString(
                                "Dialog.Host.CheckInstallation.SomeFailed"));
        }
        if (!drbdOk
            && Tools.getConfigData().getAutoOptionHost("drbdinst") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    Tools.sleep(1000);
                    drbdButton.pressButton();
                }
            });
        } else if (!hbPmOk
            && Tools.getConfigData().getAutoOptionHost("hbinst") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    Tools.sleep(1000);
                    hbPmButton.pressButton();
                }
            });
        } else if (!pmOk
            && Tools.getConfigData().getAutoOptionHost("pminst") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    Tools.sleep(1000);
                    pmButton.pressButton();
                }
            });
        }
    }

    /** Returns the next dialog object. It is set dynamicaly. */
    @Override public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.CheckInstallation.Title in TextResources.
     */
    @Override protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.CheckInstallation.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.CheckInstallation.Description in TextResources.
     */
    @Override protected String getDescription() {
        return Tools.getString("Dialog.Host.CheckInstallation.Description");
    }

    /** This class holds install method names, and their indeces. */
    private static class InstallMethods {
        /** Name of the method like "CD". */
        private final String name;
        /** Index of the method. */
        private final int index;
        /** Method string. */
        private final String method;

        /** Creates new InstallMethods object. */
        InstallMethods(final String name, final int index) {
            this(name, index, "");
        }

        /** Creates new InstallMethods object. */
        InstallMethods(final String name,
                       final int index,
                       final String method) {
            this.name = name;
            this.index = index;
            this.method = method;
        }

        /** Returns name of the install method. */
        @Override public String toString() {
            return name;
        }

        /** Returns index of the install method. */
        String getIndex() {
            return Integer.toString(index);
        }

        /** Returns method. */
        String getMethod() {
            return method;
        }

        /** Returns whether the installation method is "source". */
        boolean isSourceMethod() {
            return "source".equals(method);
        }

        /** Returns whether the installation method is "linbit". */
        boolean isLinbitMethod() {
            return "linbit".equals(method);
        }
    }

    /**
     * Returns tool tip texts for ais/pm installation method combo box and
     * install button.
     */
    private String getPmInstToolTip(final String index) {
        return Tools.html(
            getHost().getDistString(
                "PmInst.install." + index)).replaceAll(";", ";<br>&gt; ")
                                           .replaceAll("&&", "<br>&gt; &&");
    }

    /**
     * Returns tool tip texts for hb/pm installation method combo box and
     * install button.
     */
    private String getHbPmInstToolTip(final String index) {
        return Tools.html(
            getHost().getDistString(
                "HbPmInst.install." + index)).replaceAll(";", ";<br>&gt; ")
                                           .replaceAll("&&", "<br>&gt; &&");
    }

    /**
     * Returns tool tip texts for drbd installation method combo box and install
     * button.
     */
    private String getDrbdInstToolTip(final String index) {
        return Tools.html(
            getHost().getDistString(
                "DrbdInst.install." + index)).replaceAll(";", ";<br>&gt; ")
                                           .replaceAll("&&", "<br>&gt; &&");
    }
    /**
     * Returns the pane, that checks the installation of different
     * components and provides buttons to update or upgrade.
     */
    private JPanel getInstallationPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        /* get corosync/pacemaker installation methods */
        final List<InstallMethods> pmMethods =
                                            new ArrayList<InstallMethods>();
        int i = 1;
        String pmDefaultValue = null;
        while (true) {
            final String index = Integer.toString(i);
            final String text =
                    getHost().getDistString("PmInst.install.text." + index);
            if (text == null || text.equals("")) {
                if (i > 9) {
                    break;
                }
                i++;
                continue;
            }
            final String staging =
                    getHost().getDistString("PmInst.install.staging." + index);
            if (staging != null && "true".equals(staging)
                && !Tools.getConfigData().isStagingPacemaker()) {
                /* skip staging */
                i++;
                continue;
            }
            final InstallMethods pmInstallMethod = new InstallMethods(
              Tools.getString("Dialog.Host.CheckInstallation.PmInstallMethod")
              + text, i);
            if (text.equals(
                     Tools.getConfigData().getLastHbPmInstalledMethod())) {
                pmDefaultValue = pmInstallMethod.toString();
            }
            pmMethods.add(pmInstallMethod);
            i++;
        }
        pmInstMethodCB = new GuiComboBox(
                   pmDefaultValue,
                   (Object[]) pmMethods.toArray(
                                      new InstallMethods[pmMethods.size()]),
                   null, /* units */
                   GuiComboBox.Type.COMBOBOX,
                   null, /* regexp */
                   0,    /* width */
                   null, /* abbrv */
                   new AccessMode(ConfigData.AccessType.RO,
                                  false)); /* only adv. mode */
        if (Tools.getConfigData().getAutoOptionHost("pminst") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    pmInstMethodCB.setSelectedIndex(
                        Integer.parseInt(
                            Tools.getConfigData().getAutoOptionHost(
                                                                "pminst")));
                }
            });
        }
        pmInstMethodCB.addListeners(
            new ItemListener() {
                @Override public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override public void run() {
                                InstallMethods method =
                                  (InstallMethods) pmInstMethodCB.getValue();
                                final String toolTip =
                                        getPmInstToolTip(method.getIndex());
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override public void run() {
                                        pmInstMethodCB.setToolTipText(
                                                                      toolTip);
                                        pmButton.setToolTipText(toolTip);
                                    }
                                });

                            }
                        });
                        thread.start();
                    }
                }
            }, null);
        /* get hb/pm installation methods */
        final List<InstallMethods> hbPmMethods =
                                            new ArrayList<InstallMethods>();
        i = 1;
        String hbPmDefaultValue = null;
        while (true) {
            final String index = Integer.toString(i);
            final String text =
                     getHost().getDistString("HbPmInst.install.text." + index);
            if (text == null || text.equals("")) {
                if (i > 9) {
                    break;
                }
                i++;
                continue;
            }
            final String staging =
                  getHost().getDistString("HbPmInst.install.staging." + index);
            if (staging != null && "true".equals(staging)
                && !Tools.getConfigData().isStagingPacemaker()) {
                /* skip staging */
                i++;
                continue;
            }
            final InstallMethods hbPmInstallMethod = new InstallMethods(
              Tools.getString("Dialog.Host.CheckInstallation.HbPmInstallMethod")
              + text, i);
            hbPmMethods.add(hbPmInstallMethod);
            if (text.equals(
                       Tools.getConfigData().getLastHbPmInstalledMethod())) {
                hbPmDefaultValue = hbPmInstallMethod.toString();
            }
            i++;
        }
        hbPmInstMethodCB = new GuiComboBox(
                   hbPmDefaultValue,
                   (Object[]) hbPmMethods.toArray(
                                        new InstallMethods[hbPmMethods.size()]),
                   null, /* units */
                   GuiComboBox.Type.COMBOBOX,
                   null, /* regexp */
                   0,    /* width */
                   null, /* abbrv */
                   new AccessMode(ConfigData.AccessType.RO,
                                  false)); /* only adv. mode */
        if (Tools.getConfigData().getAutoOptionHost("hbinst") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    hbPmInstMethodCB.setSelectedIndex(
                        Integer.parseInt(
                            Tools.getConfigData().getAutoOptionHost(
                                                                "hbinst")));
                }
            });
        }
        hbPmInstMethodCB.addListeners(
            new ItemListener() {
                @Override public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override public void run() {
                                InstallMethods method =
                                     (InstallMethods) hbPmInstMethodCB.
                                                                    getValue();
                                final String toolTip =
                                         getHbPmInstToolTip(method.getIndex());
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override public void run() {
                                        hbPmInstMethodCB.setToolTipText(
                                                                      toolTip);
                                        hbPmButton.setToolTipText(toolTip);
                                    }
                                });

                            }
                        });
                        thread.start();
                    }
                }
            }, null);
        /* get drbd installation methods */
        final List<InstallMethods> drbdMethods =
                                               new ArrayList<InstallMethods>();
        i = 1;
        String drbdDefaultValue = null;
        while (true) {
            final String index = Integer.toString(i);
            final String text =
                      getHost().getDistString("DrbdInst.install.text." + index);
            if (text == null || text.equals("")) {
                if (i > 9) {
                    break;
                }
                i++;
                continue;
            }
            final String staging =
                  getHost().getDistString("DrbdInst.install.staging." + index);
            if (staging != null && "true".equals(staging)
                && !Tools.getConfigData().isStagingDrbd()) {
                /* skip staging */
                i++;
                continue;
            }
            String method =
                   getHost().getDistString("DrbdInst.install.method." + index);
            if (method == null) {
                method = "";
            }
            final InstallMethods drbdInstallMethod = new InstallMethods(
              Tools.getString("Dialog.Host.CheckInstallation.DrbdInstallMethod")
               + text, i, method);
            if (text.equals(
                        Tools.getConfigData().getLastDrbdInstalledMethod())) {
                drbdDefaultValue = drbdInstallMethod.toString();
            }
            drbdMethods.add(drbdInstallMethod);
            i++;
        }
        if (i > 1) {
            drbdInstallMethodsAvailable = true;
            drbdInstMethodCB = new GuiComboBox(
                       drbdDefaultValue,
                       (Object[]) drbdMethods.toArray(
                                        new InstallMethods[drbdMethods.size()]),
                       null, /* units */
                       GuiComboBox.Type.COMBOBOX,
                       null, /* regexp */
                       0,    /* width */
                       null, /* abbrv */
                       new AccessMode(ConfigData.AccessType.RO,
                                      false)); /* only adv. mode */
            drbdInstMethodCB.addListeners(
                new ItemListener() {
                    @Override public void itemStateChanged(final ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            final Thread thread = new Thread(new Runnable() {
                                @Override public void run() {
                                    InstallMethods method =
                                       (InstallMethods) drbdInstMethodCB.
                                                                    getValue();
                                    final String toolTip =
                                         getDrbdInstToolTip(method.getIndex());
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override public void run() {
                                           drbdInstMethodCB.setToolTipText(
                                                                    toolTip);
                                           drbdButton.setToolTipText(toolTip);
                                        }
                                    });

                                }
                            });
                            thread.start();
                        }
                    }
                }, null);
        } else {
            drbdInstMethodCB = new GuiComboBox("",
                                               null, /* items */
                                               null, /* units */
                                               GuiComboBox.Type.COMBOBOX,
                                               null, /* regexp */
                                               0,    /* width */
                                               null, /* abbrv */
                                               new AccessMode(
                                                      ConfigData.AccessType.RO,
                                                      false)); /* only adv. */
            drbdInstMethodCB.setEnabled(false);
        }
        if (Tools.getConfigData().getAutoOptionHost("drbdinst") != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    drbdInstMethodCB.setSelectedIndex(
                        Integer.parseInt(
                            Tools.getConfigData().getAutoOptionHost(
                                                                "drbdinst")));
                }
            });
        }
        final String lastInstalled =
                          Tools.getConfigData().getLastInstalledClusterStack();
        if (lastInstalled != null) {
            if (ConfigData.HEARTBEAT_NAME.equals(lastInstalled)) {
                pmJLabel.setForeground(Color.LIGHT_GRAY);
                pmLabel.setForeground(Color.LIGHT_GRAY);
            } else if (ConfigData.COROSYNC_NAME.equals(lastInstalled)) {
                hbPmJLabel.setForeground(Color.LIGHT_GRAY);
                hbPmLabel.setForeground(Color.LIGHT_GRAY);
            }
        }
        pane.add(hbPmJLabel);
        pane.add(hbPmLabel);
        pane.add(hbPmIcon);
        pane.add(hbPmInstMethodCB);
        pane.add(hbPmButton);
        pane.add(pmJLabel);
        pane.add(pmLabel);
        pane.add(pmIcon);
        pane.add(pmInstMethodCB);
        pane.add(pmButton);
        pane.add(new JLabel("Drbd"));
        pane.add(drbdLabel);
        pane.add(drbdIcon);
        pane.add(drbdInstMethodCB);
        pane.add(drbdButton);

        SpringUtilities.makeCompactGrid(pane, 3, 5,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad
        return pane;
    }

    /** Returns input pane with installation pane and answer pane. */
    @Override protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getInstallationPane());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString(
                                    "Dialog.Host.CheckInstallation.Checking")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  //rows, cols
                                              0, 0,  //initX, initY
                                              0, 0); //xPad, yPad

        return pane;
    }

    /** Enable skip button. */
    @Override protected boolean skipButtonEnabled() {
        return true;
    }
}
