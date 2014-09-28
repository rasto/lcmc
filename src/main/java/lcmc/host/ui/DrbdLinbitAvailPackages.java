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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.gui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.ConvertCmdCallback;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import lcmc.utilities.WidgetListener;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.ExecCommandThread;

/**
 * An implementation of a dialog where user can choose a distribution of the
 * host.
 */
@Named
public class DrbdLinbitAvailPackages extends DialogHost {
    private static final Logger LOG = LoggerFactory.getLogger(DrbdLinbitAvailPackages.class);
    private static final String NO_MATCH_STRING = "No Match";
    private static final String NEWLINE = "\\r?\\n";
    private static final int CHOICE_BOX_HEIGHT = 30;
    private Widget drbdDistributionWidget = null;
    private Widget drbdKernelDirWidget = null;
    private Widget drbdArchWidget = null;

    private List<String> drbdDistItems = null;
    private List<String> drbdKernelDirItems = null;
    private List<String> drbdArchItems = null;

    @Inject
    private DrbdAvailFiles drbdAvailFilesDialog;
    @Inject
    private Provider<CheckInstallation> checkInstallationFactory;
    @Inject
    private Application application;
    @Inject
    private WidgetFactory widgetFactory;

    protected final void availDrbdVersions() {
        /* get drbd available versions,
         * they are independent from distribution and kernel version and
         * are first directory part in the download area.*/
        drbdDistributionWidget.setEnabled(false);
        drbdKernelDirWidget.setEnabled(false);
        drbdArchWidget.setEnabled(false);
        getProgressBar().start(20000);
        final ExecCommandThread t = getHost().execCommand(new ExecCommandConfig()
                .commandString("DrbdAvailVersions")
                .convertCmdCallback(getDrbdInstallationConvertCmdCallback())
                .execCallback(new ExecCallback() {
                    @Override
                    public void done(final String answer) {
                        final String[] items = answer.split(NEWLINE);
                                /* all drbd versions are stored in form
                                 * {version1,version2,...}. This will be
                                 * later expanded by shell. */
                        availDistributions();
                    }

                    @Override
                    public void doneError(final String answer, final int errorCode) {
                        printErrorAndRetry(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.NoVersions"),
                                           answer,
                                           errorCode);
                    }
                }));
        setCommandThread(t);
    }

    /** Checks the available distributions. */
    protected final void availDistributions() {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                drbdKernelDirWidget.setEnabled(false);
                drbdArchWidget.setEnabled(false);
            }
        });
        final ExecCommandThread t = getHost().execCommand(new ExecCommandConfig()
                .commandString("DrbdAvailDistributions")
                .convertCmdCallback(getDrbdInstallationConvertCmdCallback())
                .execCallback(new ExecCallback() {
                    @Override
                    public void done(String answer) {
                        answer = NO_MATCH_STRING + '\n' + answer;
                        final String[] items = answer.split(NEWLINE);
                        drbdDistItems = Arrays.asList(items);
                        application.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                drbdDistributionWidget.reloadComboBox(
                                        new StringValue(getHost().getDistributionVersion()),
                                        StringValue.getValues(items));
                                drbdDistributionWidget.setEnabled(true);
                            }
                        });
                        availKernels();
                    }

                    @Override
                    public void doneError(final String answer, final int errorCode) {
                        printErrorAndRetry(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.NoDistributions"),
                                           answer,
                                           errorCode);
                    }
                })
                .convertCmdCallback(new ConvertCmdCallback() {
                    @Override
                    public String convert(String command) {
                        return getDrbdInstallation().replaceVarsInCommand(command);
                    }
                }));
        setCommandThread(t);
    }

    protected final void availKernels() {
        final String distVersion = getHost().getDistributionVersion();
        if (drbdDistItems == null || !drbdDistItems.contains(distVersion)) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    drbdKernelDirWidget.reloadComboBox(null, new Value[]{new StringValue(NO_MATCH_STRING)});
                }
            });
            availArchs();
            return;
        }
        final ExecCommandThread t = getHost().execCommand(new ExecCommandConfig()
                        .commandString("DrbdAvailKernels")
                        .convertCmdCallback(getDrbdInstallationConvertCmdCallback())
                        .execCallback(new ExecCallback() {
                            @Override
                            public void done(String answer) {
                                answer = NO_MATCH_STRING + '\n' + answer;
                                final String[] items = answer.split(NEWLINE);
                                drbdKernelDirItems = Arrays.asList(items);
                                application.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        drbdKernelDirWidget.reloadComboBox(
                                                new StringValue(getHost().getKernelVersion()),
                                                StringValue.getValues(items));
                                        drbdKernelDirWidget.setEnabled(true);
                                    }
                                });
                                availArchs();
                            }

                            @Override
                            public void doneError(final String answer, final int errorCode) {
                                LOG.debug("doneError:");
                                printErrorAndRetry(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.NoKernels"),
                                                   answer,
                                                   errorCode);
                            }
                          }));
        setCommandThread(t);
    }

    protected final void availArchs() {
        final String kernelVersion = getHost().getKernelVersion();
        final String arch = getHost().getArch();
        if (drbdDistItems == null
            || drbdKernelDirItems == null
            || arch == null
            || !drbdDistItems.contains(getHost().getDistributionVersion())
            || !drbdKernelDirItems.contains(kernelVersion)) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    drbdArchWidget.reloadComboBox(null, new Value[]{new StringValue(NO_MATCH_STRING)});
                    drbdArchWidget.setEnabled(false);
                }
            });
            allDone(null);
            return;
        }
        final ExecCommandThread t = getHost().execCommand(new ExecCommandConfig()
                .commandString("DrbdAvailArchs")
                .convertCmdCallback(getDrbdInstallationConvertCmdCallback())
                .execCallback(new ExecCallback() {
                    @Override
                    public void done(String answer) {
                        answer = NO_MATCH_STRING + '\n' + answer;
                        final String[] items = answer.split(NEWLINE);
                        drbdArchItems = Arrays.asList(items);
                        application.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                drbdArchWidget.reloadComboBox(new StringValue(arch), StringValue.getValues(items));
                                drbdArchWidget.setEnabled(true);
                            }
                        });
                        if (drbdArchItems == null) {
                            allDone(null);
                        } else {
                            availVersionsForDist();
                        }
                    }

                    @Override
                    public void doneError(final String answer, final int errorCode) {
                        printErrorAndRetry(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.NoArchs"),
                                           answer,
                                           errorCode);
                    }
                }));
        setCommandThread(t);
    }

    protected final void availVersionsForDist() {
        final ExecCommandThread t = getHost().execCommand(new ExecCommandConfig()
                .commandString("DrbdAvailVersionsForDist")
                .execCallback(new ExecCallback() {
                            @Override
                            public void done(final String answer) {
                                allDone(answer);
                            }

                            @Override
                            public void doneError(final String answer, final int errorCode) {
                                printErrorAndRetry(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.NoArchs"),
                                                   answer,
                                                   errorCode);
                            }
                         }));

        setCommandThread(t);
    }

    /**
     * Is called after all is done. It adds the listeners if it is the first
     * time it is called.
     */
    protected final void allDone(final String ans) {
        progressBarDone();

        enableComponents();
        if (ans == null) {
            final StringBuilder errorText = new StringBuilder(80);
            final String dist = getHost().getDistributionVersion();
            final String kernel = getHost().getKernelVersion();
            final String arch = getHost().getArch();
            if (drbdDistItems == null || !drbdDistItems.contains(dist)) {
                errorText.append(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.NotAvailable.Dist"));
            } else if (drbdKernelDirItems == null || !drbdKernelDirItems.contains(kernel)) {
                errorText.append(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.NotAvailable.Kernel"));
            } else if (drbdArchItems == null || !drbdArchItems.contains(arch)) {
                errorText.append(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.NotAvailable.Arch"));
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
            getDrbdInstallation().setAvailableDrbdVersions(versions);
            answerPaneSetText(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.AvailVersions")
                                              + " "
                                              + Tools.join(", ", versions));
            if (application.getAutoOptionHost("drbdinst") != null) {
                Tools.sleep(1000);
                pressNextButton();
            }
        }
        addListeners();
    }

    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        availDrbdVersions();
    }

    @Override
    public WizardDialog nextDialog() {
        if (getDrbdInstallation().isDrbdUpgraded()) {
            final CheckInstallation checkInstallation = checkInstallationFactory.get();
            checkInstallation.init(this, getHost(), getDrbdInstallation());
            return checkInstallation;
        } else {
            drbdAvailFilesDialog.init(this, getHost(), getDrbdInstallation());
            return drbdAvailFilesDialog;
        }
    }

    @Override
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.Title");
    }

    @Override
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.Description");
    }

    protected final JPanel getChoiceBoxes() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        final int maxX = (int) pane.getMaximumSize().getWidth();
        pane.setMaximumSize(new Dimension(maxX, CHOICE_BOX_HEIGHT));

        /* combo boxes */
        drbdDistributionWidget = widgetFactory.createInstance(
                                       Widget.Type.COMBOBOX,
                                       Widget.NO_DEFAULT,
                                       Widget.NO_ITEMS,
                                       Widget.NO_REGEXP,
                                       0,    /* width */
                                       Widget.NO_ABBRV,
                                       new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                                       Widget.NO_BUTTON);

        drbdDistributionWidget.setEnabled(false);
        pane.add(drbdDistributionWidget.getComponent());
        drbdKernelDirWidget = widgetFactory.createInstance(
                                       Widget.Type.COMBOBOX,
                                       Widget.NO_DEFAULT,
                                       Widget.NO_ITEMS,
                                       Widget.NO_REGEXP,
                                       0,    /* width */
                                       Widget.NO_ABBRV,
                                       new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                                       Widget.NO_BUTTON);

        drbdKernelDirWidget.setEnabled(false);
        pane.add(drbdKernelDirWidget.getComponent());
        drbdArchWidget = widgetFactory.createInstance(
                                       Widget.Type.COMBOBOX,
                                       Widget.NO_DEFAULT,
                                       Widget.NO_ITEMS,
                                       Widget.NO_REGEXP,
                                       0,    /* width */
                                       Widget.NO_ABBRV,
                                       new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                                       Widget.NO_BUTTON);

        drbdArchWidget.setEnabled(false);
        pane.add(drbdArchWidget.getComponent());
        pane.add(Box.createHorizontalGlue());
        pane.add(Box.createRigidArea(new Dimension(10, 0)));
        return pane;
    }

    private void addListeners() {
        /* listeners, that disallow to select anything. */
        /* distribution combo box */
        drbdDistributionWidget.addListeners(new WidgetListener() {
                    @Override
                    public void check(final Value value) {
                        String v = getHost().getDistributionVersion();
                        if (drbdDistItems == null || !drbdDistItems.contains(v)) {
                            v = NO_MATCH_STRING;
                        }
                        drbdDistributionWidget.setValue(new StringValue(v));
                    }
                });


        /* kernel version combo box */
        drbdKernelDirWidget.addListeners(
                new WidgetListener() {
                    @Override
                    public void check(final Value value) {
                        String v = getHost().getKernelVersion();
                        if (drbdKernelDirItems == null || !drbdKernelDirItems.contains(v)) {
                            v = NO_MATCH_STRING;
                        }
                        drbdKernelDirWidget.setValue(new StringValue(v));
                    }
                });

        /* arch combo box */
        drbdArchWidget.addListeners(new WidgetListener() {
                    @Override
                    public void check(final Value value) {
                        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
                        getHost().setArch(drbdArchWidget.getStringValue());
                        availVersionsForDist();
                    }
                });
    }

    /** Returns the input pane with check boxes and other info. */
    @Override
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel labelP = new JPanel(new FlowLayout(FlowLayout.LEADING));
        labelP.setPreferredSize(new Dimension(0, 0));
        labelP.add(new JLabel(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.AvailablePackages")));
        pane.add(labelP);
        pane.add(getChoiceBoxes());
        final JPanel progrPane = getProgressBarPane();
        pane.add(progrPane);
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.DrbdLinbitAvailPackages.Executing")));
        SpringUtilities.makeCompactGrid(pane, 4, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad
        return pane;
    }
}
