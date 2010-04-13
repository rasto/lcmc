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


package drbd.gui.dialog.host;

import drbd.data.Host;
import drbd.utilities.CancelCallback;
import drbd.utilities.SSH.ExecCommandThread;
import drbd.gui.dialog.WizardDialog;

import javax.swing.JPanel;

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

    /**
     * Prepares a new <code>DialogHost</code> object.
     */
    public DialogHost(final WizardDialog previousDialog,
                      final Host host) {
        super(previousDialog);
        this.host = host;
    }

    /**
     * Returns host for which is this dialog.
     */
    protected final Host getHost() {
        return host;
    }

    /**
     * Sets the command thread, so that it can be canceled.
     */
    public final void setCommandThread(final ExecCommandThread commandThread) {
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
    public final JPanel getProgressBarPane() {
        final CancelCallback cancelCallback = new CancelCallback() {
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
    public final void printErrorAndRetry(final String text) {
        super.printErrorAndRetry(text);
        progressBarDone();
    }

    /**
     * Cancels the dialog. It removes the host tab.
     * TODO: deprecated?
     */
    public final void cancelDialog() {
    //    Tools.getGUIData().removeSelectedHostTab();
    }

    /**
     * Returns title of the dialog, if host was already specified, the hostname
     * will appear in the dialog as well.
     */
    protected final String getDialogTitle() {
        final StringBuffer s = new StringBuffer(50);
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

    /**
     * Return title for getDialogTitle() function.
     */
    protected abstract String getHostDialogTitle();
}
