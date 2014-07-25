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


package lcmc.gui.dialog.host;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.model.Value;
import lcmc.model.drbd.DrbdInstallation;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.widget.Check;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.*;
import lcmc.utilities.ssh.ExecCommandThread;

/**
 * DialogHost.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public abstract class DialogHost extends WizardDialog {
    /** Host for which is this dialog. */
    private final Host host;
    /** Thread in which a command can be executed. */
    private ExecCommandThread commandThread = null;

    private DrbdInstallation drbdInstallation;

    /** Prepares a new {@code DialogHost} object. */
    protected DialogHost(final WizardDialog previousDialog, final Host host, final DrbdInstallation drbdInstallation) {
        super(previousDialog);
        this.host = host;
        this.drbdInstallation = drbdInstallation;
    }

    protected final Host getHost() {
        return host;
    }

    protected DrbdInstallation getDrbdInstallation() {
        return drbdInstallation;
    }

    /** Sets the command thread, so that it can be canceled. */
    final void setCommandThread(final ExecCommandThread commandThread) {
        this.commandThread = commandThread;
        if (getProgressBar() != null) {
            getProgressBar().setCancelEnabled(commandThread != null);
        }
    }
    /**
     * Creates progress bar that can be used during connecting to the host
     * and returns pane, where the progress bar is displayed.
     */
    public final JPanel getProgressBarPane(final String title) {
        final CancelCallback cancelCallback = new CancelCallback() {
            @Override
            public void cancel() {
                if (commandThread != null) {
                    host.getSSH().cancelSession(commandThread);
                }
            }
        };
        return getProgressBarPane(title, cancelCallback);
    }

    /**
     * Creates progress bar that can be used during connecting to the host
     * and returns pane, where the progress bar is displayed.
     */
    protected final JPanel getProgressBarPane() {
        final CancelCallback cancelCallback = new CancelCallback() {
            @Override
            public void cancel() {
                if (commandThread != null) {
                    host.getSSH().cancelSession(commandThread);
                }
            }
        };
        return getProgressBarPane(cancelCallback);
    }

    /**
     * Prints error text in the answer pane, stops progress bar, reenables
     * buttons and adds retry button.
     */
    @Override
    public final void printErrorAndRetry(final String text) {
        super.printErrorAndRetry(text);
        progressBarDone();
    }

    /**
     * Returns title of the dialog, if host was already specified, the hostname
     * will appear in the dialog as well.
     */
    @Override
    protected final String getDialogTitle() {
        final StringBuilder s = new StringBuilder(50);
        s.append(getHostDialogTitle());
        if (host != null
            && !host.getName().isEmpty()
            && !"unknown".equals(host.getName())) {

            s.append(" (");
            s.append(host.getName());
            s.append(')');
        }
        return s.toString();
    }

    protected ConvertCmdCallback getDrbdInstallationConvertCmdCallback() {
        return new ConvertCmdCallback() {
            @Override
            public String convert(final String command) {
                return drbdInstallation.replaceVarsInCommand(command);
            }
        };
    }

    /** Return title for getDialogTitle() function. */
    protected abstract String getHostDialogTitle();

    /** Get installation methods. */
    protected final Widget getInstallationMethods(
                                           final String prefix,
                                           final boolean staging,
                                           final String lastInstalledMethod,
                                           final String autoOption,
                                           final ComponentWithTest installButton) {
        final List<InstallMethods> methods = new ArrayList<InstallMethods>();
        int i = 1;
        Value defaultValue = null;
        while (true) {
            final String index = Integer.toString(i);
            final String text =
                    getHost().getDistString(prefix + ".install.text." + index);
            if (text == null || text.isEmpty()) {
                if (i > 9) {
                    break;
                }
                i++;
                continue;
            }
            final String stagingMethod =
                 getHost().getDistString(prefix + ".install.staging." + index);
            if (stagingMethod != null && "true".equals(stagingMethod)
                && !staging) {
                /* skip staging */
                i++;
                continue;
            }
            String method =
                  getHost().getDistString(prefix + ".install.method." + index);
            if (method == null) {
                method = "";
            }
            final InstallMethods installMethod = new InstallMethods(
                Tools.getString("Dialog.Host.CheckInstallation.InstallMethod")
                + text, i, method);
            if (text.equals(lastInstalledMethod)) {
                defaultValue = installMethod;
            }
            methods.add(installMethod);
            i++;
        }
        final Widget instMethodWi = WidgetFactory.createInstance(
                       Widget.Type.COMBOBOX,
                       defaultValue,
                       methods.toArray(new InstallMethods[methods.size()]),
                       Widget.NO_REGEXP,
                       0,    /* width */
                       Widget.NO_ABBRV,
                       new AccessMode(Application.AccessType.RO,
                                      !AccessMode.ADVANCED),
                       Widget.NO_BUTTON);
        if (Tools.getApplication().getAutoOptionHost(autoOption) != null) {
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    instMethodWi.setSelectedIndex(
                        Integer.parseInt(
                         Tools.getApplication().getAutoOptionHost(autoOption)));
                }
            });
        }
        instMethodWi.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                final InstallMethods method =
                                      (InstallMethods) instMethodWi.getValue();
                final String toolTip =
                                    getInstToolTip(prefix, method.getIndex());
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        instMethodWi.setToolTipText(toolTip);
                        installButton.setToolTipText(toolTip);
                    }
                });
            }
        });
        return instMethodWi;
    }

    /**
     * Returns tool tip texts for installation method combo box and
     * install button.
     */
    protected final String getInstToolTip(final String prefix,
                                          final String index) {
        return Tools.html(
            getHost().getDistString(
                prefix + ".install." + index)).replaceAll(";", ";<br>&gt; ")
                                           .replaceAll("&&", "<br>&gt; &&");
    }

    protected void enableNextButtons(final List<String> incorrect,
                                     final List<String> changed) {
        final Check check = new Check(incorrect, changed);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                buttonClass(nextButton()).setEnabledCorrect(check);
                for (final MyButton button : nextButtons()) {
                    button.setEnabled(check.isCorrect());
                }
            }
        });
    }

    /** Buttons that are enabled/disabled during checks. */
    protected MyButton[] nextButtons() {
        return new MyButton[]{};
    }

    /** This class holds install method names, and their indeces. */
    public static final class InstallMethods implements Value {
        /** Name of the method like "CD". */
        private final String name;
        /** Index of the method. */
        private final int index;
        /** Method string. */
        private final String method;

        public InstallMethods(final String name,
                              final int index,
                              final String method) {
            this.name = name;
            this.index = index;
            this.method = method;
        }

        /** Returns name of the install method. */
        @Override
        public String toString() {
            return name;
        }

        /** Returns index of the install method. */
        public String getIndex() {
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

        @Override
        public String getValueForGui() {
            return name;
        }

        @Override
        public String getValueForConfig() {
            return name;
        }

        @Override
        public boolean isNothingSelected() {
            return name == null;
        }

        @Override
        public String getNothingSelected() {
            return NOTHING_SELECTED;
        }

        @Override
        public Unit getUnit() {
            return null;
        }

        @Override
        public String getValueForConfigWithUnit() {
            return getValueForConfig();
        }
    }
}
