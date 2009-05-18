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
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.gui.SpringUtilities;
import drbd.gui.GuiComboBox;
import drbd.utilities.ExecCallback;

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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * An implementation of a dialog where drbd/heartbeat installation is checked.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostCheckInstallation extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /** Checking drbd label. */
    private final JLabel drbdLabel = new JLabel(
          ": " + Tools.getString("Dialog.HostCheckInstallation.CheckingDrbd"));
    /** Checking hb label. */
    private final JLabel heartbeatLabel = new JLabel(
          ": " + Tools.getString("Dialog.HostCheckInstallation.CheckingHb"));

    /** Install/Upgrade drbd button. */
    private final MyButton drbdButton = new MyButton(
            Tools.getString("Dialog.HostCheckInstallation.DrbdInstallButton"));
    /** Install heartbeat button. */
    private final MyButton heartbeatButton = new MyButton(
            Tools.getString("Dialog.HostCheckInstallation.HbInstallButton"));
    /** Heartbeat installation method */
    private GuiComboBox hbInstMethodCB;

    /** Checking icon. */
    private static final ImageIcon CHECKING_ICON =
        Tools.createImageIcon(
                Tools.getDefault("Dialog.HostCheckInstallation.CheckingIcon"));
    /** Not installed icon. */
    private static final ImageIcon NOT_INSTALLED_ICON =
        Tools.createImageIcon(
             Tools.getDefault("Dialog.HostCheckInstallation.NotInstalledIcon"));
    /** Already installed icon. */
    private static final ImageIcon INSTALLED_ICON =
        Tools.createImageIcon(
                Tools.getDefault("Dialog.HostCheckInstallation.InstalledIcon"));
    /** Upgrade available icon. */
    private static final ImageIcon UPGR_AVAIL_ICON =
        Tools.createImageIcon(
                Tools.getDefault("Dialog.HostCheckInstallation.UpgrAvailIcon"));

    /** Drbd icon: checking ... */
    private final JLabel drbdIcon = new JLabel(CHECKING_ICON);
    /** Heartbeat icon: checking ... */
    private final JLabel heartbeatIcon = new JLabel(CHECKING_ICON);

    /** Whether drbd installation was ok. */
    private boolean drbdOk = false;
    /** Whether heartbeat installation was ok. */
    private boolean heartbeatOk = false;
    /** Version that appears in the dialog. */
    private String versionText;
    private Map<String, InstallMethods> installedMethodMap =
                                         new HashMap<String, InstallMethods>();

    /**
     * Prepares a new <code>HostCheckInstallation</code> object.
     */
    public HostCheckInstallation(final WizardDialog previousDialog,
                                 final Host host) {
        super(previousDialog, host);
    }

    /**
     * Inits dialog.
     */
    protected void initDialog() {
        super.initDialog();
        drbdOk         = false;
        heartbeatOk    = false;
        nextDialogObject = new HostFinish(this, getHost());
        final HostCheckInstallation thisClass = this;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                drbdButton.setEnabled(false);
                heartbeatButton.setEnabled(false);
                hbInstMethodCB.setEnabled(false);
            }
        });
        drbdButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    if (drbdOk) {
                        getHost().setDrbdWillBeUpgraded(true);
                    }
                    final String button = e.getActionCommand();
                    if (!drbdOk || button.equals(Tools.getString(
                     "Dialog.HostCheckInstallation.DrbdCheckForUpgradeButton"))) {
                        nextDialogObject = new HostDist(thisClass, getHost());
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                ((MyButton) buttonClass(
                                                nextButton())).pressButton();
                            }
                        });
                    } else {
                        nextDialogObject = new HostLogin(thisClass, getHost());
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                ((MyButton) buttonClass(
                                                nextButton())).pressButton();
                            }
                        });
                    }
                }
            }
        );


        heartbeatButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    nextDialogObject = new HostHbInst(thisClass, getHost());
                    InstallMethods im =
                                   (InstallMethods) hbInstMethodCB.getValue();
                    getHost().setHbInstallMethod(im.getIndex());
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ((MyButton) buttonClass(
                                                nextButton())).pressButton();
                        }
                    });
                }
            }
        );

        enableComponentsLater(new JComponent[]{});

        getHost().execCommand("DrbdCheck.version",
                         getProgressBar(),
                         new ExecCallback() {
                             public void done(final String ans) {
                                 checkDrbd(ans);
                             }
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 checkDrbd(""); // not installed
                             }
                         },
                         null,   /* ConvertCmdCallback */
                         false); /* outputVisible */
    }

    /**
     * Checks whether drbd is installed and starts heartbeat check.
     */
    public void checkDrbd(final String ans) {
        if ("".equals(ans) || "\n".equals(ans)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    drbdLabel.setText(": " + Tools.getString(
                            "Dialog.HostCheckInstallation.DrbdNotInstalled"));
                    drbdIcon.setIcon(NOT_INSTALLED_ICON);
                    drbdButton.setEnabled(true);
                }
            });
        } else {
            drbdOk = true;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    drbdLabel.setText(": " + ans.trim());
                    if (getHost().isDrbdUpgradeAvailable(ans.trim())) {
                        drbdIcon.setIcon(UPGR_AVAIL_ICON);
                        drbdButton.setText(Tools.getString(
                          "Dialog.HostCheckInstallation.DrbdUpgradeButton"));
                        drbdButton.setEnabled(true);
                    } else {
                        drbdButton.setText(Tools.getString(
                          "Dialog.HostCheckInstallation.DrbdCheckForUpgradeButton"));
                        drbdButton.setEnabled(true);
                        drbdIcon.setIcon(INSTALLED_ICON);
                    }
                }
            });
        }
        getHost().execCommand("HbCheck.version",
                         getProgressBar(),
                         new ExecCallback() {
                             public void done(final String ans) {
                                 Tools.debug(this, "ans: " + ans);
                                 checkHeartbeat(ans);
                             }
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 done("");
                             }
                         },
                         null,   /* ConvertCmdCallback */
                         false); /* outputVisible */
    }

    /**
     * Checks whether heartbeat is installed and starts heartbeat gui check.
     */
    public void checkHeartbeat(final String ans) {
        if ("".equals(ans) || "\n".equals(ans)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    heartbeatIcon.setIcon(NOT_INSTALLED_ICON);
                    heartbeatLabel.setText(": " + Tools.getString(
                             "Dialog.HostCheckInstallation.HbNotInstalled"));
                    heartbeatButton.setEnabled(true);
                    hbInstMethodCB.setEnabled(true);
                    final String toolTip =
                                   getHbInstToolTip("1");
                    hbInstMethodCB.setToolTipText(toolTip);
                    heartbeatButton.setToolTipText(toolTip);
                }
            });
        } else {
            heartbeatOk = true;
            final int i = ans.indexOf(' ');
            String version;
            if (i < 0) {
                version = ans.trim();
            } else {
                version = ans.substring(0, i);
            }
            if ("2.1.3".equals(version)
                && "sles10".equals(getHost().getDistVersion())) {
                /* sles10 heartbeat 2.1.3 looks like hb 2.1.4 */
                version = "2.1.4";
                versionText = "2.1.3 (2.1.4)";
            } else {
                versionText = version;
            }
            getHost().setHeartbeatVersion(version);
            final InstallMethods installedMethod =
                                              installedMethodMap.get(version);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    heartbeatIcon.setIcon(INSTALLED_ICON);
                    heartbeatLabel.setText(": " + versionText);
                    if (installedMethod != null) {
                        hbInstMethodCB.setValue(installedMethod);
                    }
                }
            });

        }

        if (drbdOk && heartbeatOk) { 
            progressBarDone();
            nextButtonSetEnabled(true);
            enableComponents();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    answerPaneSetText(Tools.getString(
                                        "Dialog.HostCheckInstallation.AllOk"));
                }
            });
        } else {
            progressBarDoneError();
            Tools.debug(this, "drbd: " + drbdOk + ", hb: " + heartbeatOk);
            printErrorAndRetry(Tools.getString(
                                "Dialog.HostCheckInstallation.SomeFailed"));
        }
    }

    /**
     * Returns the next dialog object. It is set dynamicaly.
     */
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.HostCheckInstallation.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostCheckInstallation.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.HostCheckInstallation.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.HostCheckInstallation.Description");
    }

    /**
     * This class holds install method names, and their indeces.
     */
    private class InstallMethods {
        /** Name of the method like "CD". */
        private final String name;
        /** Index of the method. */
        private final int index;

        /**
         * Creates new InstallMethods object.
         */
        InstallMethods(final String name, final int index) {
            this.name = name;
            this.index = index;
        }

        /**
         * Returns name of the install method.
         */
        public final String toString() {
            return name;
        }

        /**
         * Returns index of the install method.
         */
        public final String getIndex() {
            return Integer.toString(index);
        }
    }

    /**
     * Returns tool tip texts for hb installation method combo box and install
     * button.
     */
    private final String getHbInstToolTip(final String index) {
        return Tools.html(
            getHost().getDistString(
                "HbInst.install." + index)).replaceAll(";", ";<br>&gt; ")
                                           .replaceAll("&&", "<br>&gt; &&");
    }
    /**
     * Returns the pane, that checks the installation of different
     * components and provides buttons to update or upgrade.
     */
    private JPanel getInstallationPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        List<InstallMethods> methods = new ArrayList<InstallMethods>();
        int i = 1;
        while (true) {
            final String index = Integer.toString(i);
            final String text =
                      getHost().getDistString("HbInst.install.text." + index);
            if (text == null) {
                break;
            }
            InstallMethods installMethod = new InstallMethods(
                Tools.getString("Dialog.HostCheckInstallation.HbInstallMethod")
                + text, i);
            methods.add(installMethod);
            final String forVersion =
                      getHost().getDistString("HbInst.install.version." + index);
            if (forVersion != null) {
                installedMethodMap.put(forVersion, installMethod);
            }
            i++;
        }
        final String defaultValue = methods.get(0).toString();
        hbInstMethodCB = new GuiComboBox(
                           defaultValue,
                           (Object[]) methods.toArray(new InstallMethods[methods.size()]),
                           GuiComboBox.Type.COMBOBOX,
                           null,
                           0);
        hbInstMethodCB.addListeners(
            new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                InstallMethods method =
                                     (InstallMethods) hbInstMethodCB.getValue();
                                final String toolTip =
                                           getHbInstToolTip(method.getIndex());
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        hbInstMethodCB.setToolTipText(toolTip);
                                        heartbeatButton.setToolTipText(toolTip);
                                    }
                                });

                            }
                        });
                        thread.start();
                    }
                }
            }, null);
        pane.add(new JLabel("HB/Pacemaker"));
        pane.add(heartbeatLabel);
        pane.add(heartbeatButton);
        pane.add(heartbeatIcon);
        pane.add(hbInstMethodCB);
        pane.add(new JLabel("Drbd"));
        pane.add(drbdLabel);
        pane.add(drbdButton);
        pane.add(drbdIcon);
        pane.add(new JPanel());
        //pane.add(drbdInstMethodCB); // TODO
        SpringUtilities.makeCompactGrid(pane, 2, 5,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad
        return pane;
    }

    /**
     * Returns input pane with installation pane and answer pane.
     */
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getInstallationPane());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString(
                                    "Dialog.HostCheckInstallation.Checking")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad

        return pane;
    }

    /**
     * Enable skip button.
     */
    protected boolean skipButtonEnabled() {
        return true;
    }
}
