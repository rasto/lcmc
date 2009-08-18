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
import drbd.gui.SpringUtilities;
import drbd.utilities.Tools;
import drbd.utilities.ExecCallback;
import drbd.gui.GuiComboBox;
import drbd.utilities.SSH.ExecCommandThread;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Arrays;
import javax.swing.BoxLayout;
import javax.swing.Box;

/**
 * An implementation of a dialog where user can choose a distribution of the
 * host.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostDrbdLinbitAvailPackages extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Combo box with distributions. */
    private GuiComboBox drbdDistCombo = null;
    /** Combo box with available kernel versions for this distribution. */
    private GuiComboBox drbdKernelDirCombo = null;
    /** Combo box with available architectures versions for this distribution.
     */
    private GuiComboBox drbdArchCombo = null;
    /** List of items in the dist combo. */
    private List<String> drbdDistItems = null;
    /** List of items in the kernel versions combo. */
    private List<String> drbdKernelDirItems = null;
    /** List of items in the arch combo. */
    private List<String> drbdArchItems = null;
    /** No match string. */
    private static final String NO_MATCH_STRING = "No Match";
    /** Newline. */
    private static final String NEWLINE = "\\r?\\n";
    /** Height of the choice boxes. */
    private static final int CHOICE_BOX_HEIGHT = 30;

    /**
     * Prepares a new <code>HostDrbdLinbitAvailPackages</code> object.
     */
    public HostDrbdLinbitAvailPackages(final WizardDialog previousDialog,
                                       final Host host) {
        super(previousDialog, host);
    }

    /**
     * Checks the available drbd verisions.
     */
    protected void availVersions() {
        /* get drbd available versions,
         * they are independent from distribution and kernel version and
         * are first directory part in the download area.*/
        drbdDistCombo.setEnabled(false);
        drbdKernelDirCombo.setEnabled(false);
        drbdArchCombo.setEnabled(false);
        getProgressBar().start(20000);
        final ExecCommandThread t = getHost().execCommandCache(
                          "DrbdAvailVersions",
                          null, /* ProgressBar */
                          new ExecCallback() {
                            public void done(final String ans) {
                                final String[] items = ans.split(NEWLINE);
                                /* all drbd versions are stored in form
                                 * {version1,version2,...}. This will be
                                 * later expanded by shell. */
                                StringBuffer item = new StringBuffer("{");
                                for (int i = 0; i < items.length - 1; i++) {
                                    item.append("'" + items[i] + "',");
                                }
                                if (items.length != 0) {
                                    item.append("'"
                                                + items[items.length - 1]
                                                + "'");
                                }
                                item.append('}');
                                getHost().setDrbdVersionToInstall(
                                                               item.toString());
                                availDistributions();
                            }
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(Tools.getString(
                              "Dialog.HostDrbdLinbitAvailPackages.NoVersions"),
                                                   ans,
                                                   exitCode);
                            }
                          },
                          null,   /* ConvertCmdCallback */
                          false); /* outputVisible */
        setCommandThread(t);
    }

    /**
     * Checks the available distributions.
     */
    protected void availDistributions() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                drbdKernelDirCombo.setEnabled(false);
                drbdArchCombo.setEnabled(false);
            }
        });
        final ExecCommandThread t = getHost().execCommandCache(
                          "DrbdAvailDistributions",
                          null, /* ProgressBar */
                          new ExecCallback() {
                            public void done(String ans) {
                                ans = NO_MATCH_STRING + "\n" + ans;
                                final String[] items = ans.split(NEWLINE);
                                drbdDistItems = Arrays.asList(items);
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        drbdDistCombo.reloadComboBox(
                                                    getHost().getDistVersion(),
                                                    items);
                                        drbdDistCombo.setEnabled(true);
                                    }
                                });
                                availKernels();
                            }
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(Tools.getString(
                          "Dialog.HostDrbdLinbitAvailPackages.NoDistributions"),
                                                   ans,
                                                   exitCode);
                            }
                          },
                          null,   /* ConvertCmdCallback */
                          false); /* outputVisible */
        setCommandThread(t);
    }

    /**
     * Checks what are the available kernels for this distribution.
     */
    protected void availKernels() {
        final String distVersion = getHost().getDistVersion();
        if (drbdDistItems == null || !drbdDistItems.contains(distVersion)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    drbdKernelDirCombo.reloadComboBox(
                                                null,
                                                new String[]{NO_MATCH_STRING});
                }
            });
            availArchs();
            return;
        }
        final ExecCommandThread t = getHost().execCommandCache(
                          "DrbdAvailKernels",
                          null, /* ProgressBar */
                          new ExecCallback() {
                            public void done(String ans) {
                                ans = NO_MATCH_STRING + "\n" + ans;
                                final String[] items = ans.split(NEWLINE);
                                drbdKernelDirItems = Arrays.asList(items);
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        drbdKernelDirCombo.reloadComboBox(
                                                getHost().getKernelVersion(),
                                                items);
                                        drbdKernelDirCombo.setEnabled(true);
                                    }
                                });
                                availArchs();
                            }

                            public void doneError(final String ans,
                                                  final int exitCode) {
                                Tools.debug(this, "doneError");
                                printErrorAndRetry(
                Tools.getString("Dialog.HostDrbdLinbitAvailPackages.NoKernels"),
                                   ans,
                                   exitCode);
                            }
                          },
                          null,   /* ConvertCmdCallback */
                          false); /* outputVisible */
        setCommandThread(t);
    }

    /**
     * Checks what are the available architectures for this distribution.
     */
    protected void availArchs() {
        final String kernelVersion = getHost().getKernelVersion();
        final String arch = getHost().getArch();
        if (drbdDistItems == null
            || drbdKernelDirItems == null
            || arch == null
            || !drbdDistItems.contains(getHost().getDistVersion())
            || !drbdKernelDirItems.contains(kernelVersion)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    drbdArchCombo.reloadComboBox(null,
                                                 new String[]{NO_MATCH_STRING});
                    drbdArchCombo.setEnabled(false);
                }
            });
            allDone(null);
            return;
        }
        final ExecCommandThread t = getHost().execCommandCache(
                          "DrbdAvailArchs",
                          null, /* ProgressBar */
                          new ExecCallback() {
                            public void done(String ans) {
                                ans = NO_MATCH_STRING + "\n" + ans;
                                final String[] items = ans.split(NEWLINE);
                                drbdArchItems = Arrays.asList(items);
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        drbdArchCombo.reloadComboBox(
                                                        arch,
                                                        items);
                                        drbdArchCombo.setEnabled(true);
                                    }
                                });
                                if (drbdArchItems == null
                                    || !drbdArchItems.contains(arch)) {
                                    allDone(null);
                                } else {
                                    availVersionsForDist();
                                }
                            }

                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(Tools.getString(
                                 "Dialog.HostDrbdLinbitAvailPackages.NoArchs"),
                                                   ans,
                                                   exitCode);
                            }
                          },
                          null,   /* ConvertCmdCallback */
                          false); /* outputVisible */

        setCommandThread(t);
    }

    /**
     * Checks what are the avail drbd versions for this distribution.
     */
    protected void availVersionsForDist() {
        final ExecCommandThread t = getHost().execCommandCache(
                          "DrbdAvailVersionsForDist",
                          null, /* ProgressBar */
                          new ExecCallback() {
                            public void done(final String ans) {
                                allDone(ans);
                            }

                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(
                                    Tools.getString(
                                  "Dialog.HostDrbdLinbitAvailPackages.NoArchs"),
                                    ans,
                                    exitCode);
                            }
                          },
                          null,   /* ConvertCmdCallback */
                          false); /* outputVisible */

        setCommandThread(t);
    }

    /**
     * Is called after all is done. It adds the listeners if it is the first
     * time it is called.
     */
    protected void allDone(final String ans) {
        progressBarDone();

        enableComponents();
        if (ans == null) {
            final StringBuffer errorText = new StringBuffer(80);
            final String dist = getHost().getDistVersion();
            final String kernel = getHost().getKernelVersion();
            final String arch = getHost().getArch();
            if (drbdDistItems == null || !drbdDistItems.contains(dist)) {
                errorText.append(
                  Tools.getString(
                    "Dialog.HostDrbdLinbitAvailPackages.DownloadNotAvailable.Dist"));
            } else if (drbdKernelDirItems == null
                       || !drbdKernelDirItems.contains(kernel)) {
                errorText.append(
                  Tools.getString(
                    "Dialog.HostDrbdLinbitAvailPackages.DownloadNotAvailable.Kernel"));
            } else if (drbdArchItems == null || !drbdArchItems.contains(arch)) {
                errorText.append(
                  Tools.getString(
                    "Dialog.HostDrbdLinbitAvailPackages.DownloadNotAvailable.Arch"));
            }
            errorText.append("\n\n");
            errorText.append(dist);
            errorText.append('\n');
            errorText.append(kernel);
            errorText.append('\n');
            errorText.append(arch);
            printErrorAndRetry(errorText.toString());
        } else {
            final String[] versions = ans.split(NEWLINE);
            getHost().setAvailableDrbdVersions(versions);
            answerPaneSetText(
                    Tools.getString(
                        "Dialog.HostDrbdLinbitAvailPackages.AvailVersions")
                    + " " + Tools.join(", ", versions));
        }
        addListeners();
    }

    /**
     * Inits dialog and starts the distribution detection.
     */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        availVersions();
    }

    /**
     * Returns the next dialog which is HostCheckInstallation.
     */
    public WizardDialog nextDialog() {
        if (getHost().isDrbdUpgraded()) {
            return new HostCheckInstallation(this, getHost());
        } else {
            return new HostDrbdAvailFiles(this, getHost());
        }
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.HostDrbdLinbitAvailPackages.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostDrbdLinbitAvailPackages.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.HostDrbdLinbitAvailPackages.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString(
                            "Dialog.HostDrbdLinbitAvailPackages.Description");
    }

    /**
     * Returns the pane with all combo boxes.
     */
    protected JPanel getChoiceBoxes() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
        final int maxX = (int) pane.getMaximumSize().getWidth();
        pane.setMaximumSize(new Dimension(maxX, CHOICE_BOX_HEIGHT));

        /* combo boxes */
        drbdDistCombo = new GuiComboBox(null,
                                        null,
                                        GuiComboBox.Type.COMBOBOX,
                                        null,
                                        0);

        drbdDistCombo.setEnabled(false);
        pane.add(drbdDistCombo);
        drbdKernelDirCombo = new GuiComboBox(null,
                                             null,
                                             GuiComboBox.Type.COMBOBOX,
                                             null,
                                             0);

        drbdKernelDirCombo.setEnabled(false);
        pane.add(drbdKernelDirCombo);
        drbdArchCombo = new GuiComboBox(null,
                                        null,
                                        GuiComboBox.Type.COMBOBOX,
                                        null,
                                        0);

        drbdArchCombo.setEnabled(false);
        pane.add(drbdArchCombo);
        pane.add(Box.createHorizontalGlue());
        pane.add(Box.createRigidArea(new Dimension(10, 0)));
        return pane;
    }

    /**
     * Adds listeners to the check boxes.
     */
    private void addListeners() {
        /* listeners, that disallow to select anything. */
        /* distribution combo box */
        final ItemListener distItemListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String v = getHost().getDistVersion();
                    if (drbdDistItems == null || !drbdDistItems.contains(v)) {
                        v = NO_MATCH_STRING;
                    }
                    drbdDistCombo.setValue(v);
                }
            }
        };
        drbdDistCombo.addListeners(distItemListener, null);

        /* kernel version combo box */
        final ItemListener kernelItemListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String v = getHost().getKernelVersion();
                    if (drbdKernelDirItems == null
                        || !drbdKernelDirItems.contains(v)) {
                        v = NO_MATCH_STRING;
                    }
                    drbdKernelDirCombo.setValue(v);
                }
            }
        };
        drbdKernelDirCombo.addListeners(kernelItemListener, null);

        /* arch combo box */
        final ItemListener archItemListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String v = getHost().getArch();
                    if (drbdArchItems == null || !drbdArchItems.contains(v)) {
                        v = NO_MATCH_STRING;
                    }
                    drbdArchCombo.setValue(v);
                }
            }
        };
        drbdArchCombo.addListeners(archItemListener, null);

    }

    /**
     * Returns the input pane with check boxes and other info.
     */
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel progrPane = getProgressBarPane();
        pane.add(progrPane);
        final JPanel labelP = new JPanel(new FlowLayout(FlowLayout.LEFT));
        labelP.setPreferredSize(new Dimension(0, 0));
        labelP.add(new JLabel(
            Tools.getString(
                "Dialog.HostDrbdLinbitAvailPackages.AvailablePackages")));
        labelP.setBackground(progrPane.getBackground());
        pane.add(labelP);
        pane.add(getChoiceBoxes());
        pane.add(getAnswerPane(Tools.getString(
                            "Dialog.HostDrbdLinbitAvailPackages.Executing")));
        SpringUtilities.makeCompactGrid(pane, 4, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }
}
