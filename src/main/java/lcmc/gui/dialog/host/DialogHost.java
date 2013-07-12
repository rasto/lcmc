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

import lcmc.data.Host;
import lcmc.data.AccessMode;
import lcmc.data.ConfigData;
import lcmc.utilities.CancelCallback;
import lcmc.utilities.SSH.ExecCommandThread;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.WidgetListener;

import javax.swing.JPanel;
import java.util.List;
import java.util.ArrayList;

/**
 * DialogHost.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public abstract class DialogHost extends WizardDialog {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Host for which is this dialog. */
    private final Host host;
    /** Thread in which a command can be executed. */
    private ExecCommandThread commandThread = null;

    /** Prepares a new <code>DialogHost</code> object. */
    public DialogHost(final WizardDialog previousDialog, final Host host) {
        super(previousDialog);
        this.host = host;
    }

    /** Returns host for which is this dialog. */
    protected final Host getHost() {
        return host;
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
            && !host.getName().equals("")
            && !host.getName().equals("unknown")) {

            s.append(" (");
            s.append(host.getName());
            s.append(')');
        }
        return s.toString();
    }

    /** Return title for getDialogTitle() function. */
    protected abstract String getHostDialogTitle();

    /** This class holds install method names, and their indeces. */
    public static final class InstallMethods {
        /** Name of the method like "CD". */
        private final String name;
        /** Index of the method. */
        private final int index;
        /** Method string. */
        private final String method;

        /** Creates new InstallMethods object. */
        public InstallMethods(final String name, final int index) {
            this(name, index, "");
        }

        /** Creates new InstallMethods object. */
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
    }

    /** Get installation methods. */
    protected final Widget getInstallationMethods(
                                           final String prefix,
                                           final boolean staging,
                                           final String lastInstalledMethod,
                                           final String autoOption,
                                           final MyButton installButton) {
        final List<InstallMethods> methods = new ArrayList<InstallMethods>();
        int i = 1;
        String defaultValue = null;
        while (true) {
            final String index = Integer.toString(i);
            final String text =
                    getHost().getDistString(prefix + ".install.text." + index);
            if (text == null || text.equals("")) {
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
                defaultValue = installMethod.toString();
            }
            methods.add(installMethod);
            i++;
        }
        final Widget instMethodWi = WidgetFactory.createInstance(
                       Widget.Type.COMBOBOX,
                       defaultValue,
                       (Object[]) methods.toArray(
                                           new InstallMethods[methods.size()]),
                       Widget.NO_REGEXP,
                       0,    /* width */
                       Widget.NO_ABBRV,
                       new AccessMode(ConfigData.AccessType.RO,
                                      !AccessMode.ADVANCED),
                       Widget.NO_BUTTON);
        if (Tools.getConfigData().getAutoOptionHost(autoOption) != null) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    instMethodWi.setSelectedIndex(
                        Integer.parseInt(
                         Tools.getConfigData().getAutoOptionHost(autoOption)));
                }
            });
        }
        instMethodWi.addListeners(new WidgetListener() {
            @Override
            public void check(final Object value) {
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
}
