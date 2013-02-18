/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.gui.dialog.drbdConfig;

import lcmc.data.Host;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH;
import lcmc.gui.SpringUtilities;
import lcmc.gui.widget.Widget;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.dialog.host.DialogHost;
import lcmc.gui.resources.DrbdVolumeInfo;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class ProxyCheckInstallation extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /** Checking proxy label. */
    private final JLabel proxyLabel = new JLabel(
          ": " + Tools.getString("ProxyCheckInstallation.CheckingProxy"));

    /** Install/Upgrade proxy button. */
    private final MyButton proxyButton = new MyButton(
            Tools.getString("ProxyCheckInstallation.ProxyInstallButton"));
    /** Proxy installation method. */
    private Widget proxyInstMethodWi;

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

    private static final String PROXY_PREFIX = "ProxyInst";
    private static final String PROXY_AUTO_OPTION = "proxyinst";

    /** Proxy icon: checking ... */
    private final JLabel proxyIcon = new JLabel(CHECKING_ICON);
    /** Whether proxy installation was ok. */
    private boolean proxyOk = false;
    /** The proxy host. */
    private final Host host;
    /** Drbd volume info. */
    private final DrbdVolumeInfo drbdVolumeInfo;
    /** The dialog we came from. */
    private final WizardDialog origDialog;

    /** Prepares a new <code>ProxyCheckInstallation</code> object. */
    ProxyCheckInstallation(final WizardDialog previousDialog,
                           final Host host,
                           final DrbdVolumeInfo drbdVolumeInfo,
                           final WizardDialog origDialog) {
        super(previousDialog, host);
        this.host = host;
        this.drbdVolumeInfo = drbdVolumeInfo;
        this.origDialog = origDialog;
    }

    /** Inits dialog. */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{});
    }

    /** Inits the dialog. */
    @Override
    protected void initDialogAfterVisible() {
        proxyOk = false;

        nextDialogObject = null;
        final ProxyCheckInstallation thisClass = this;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                proxyButton.setBackgroundColor(
                                 Tools.getDefaultColor("ConfigDialog.Button"));
                proxyButton.setEnabled(false);
                proxyInstMethodWi.setEnabled(false);
            }
        });
        proxyButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    nextDialogObject = new ProxyInst(thisClass,
                                                     getHost(),
                                                     drbdVolumeInfo,
                                                     origDialog);
                    final InstallMethods im =
                                 (InstallMethods) proxyInstMethodWi.getValue();
                    getHost().setProxyInstallMethod(im.getIndex());
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            buttonClass(nextButton()).pressButton();
                        }
                    });
                }
            }
        );

        getHost().execCommand("ProxyCheck.version",
                         getProgressBar(),
                         new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                                 checkProxy(ans);
                             }
                             @Override
                             public void doneError(
                                                         final String ans,
                                                         final int exitCode) {
                                 checkProxy(""); // not installed
                             }
                         },
                         null,   /* ConvertCmdCallback */
                         false,  /* outputVisible */
                         SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /**
     * Checks whether proxy is installed.
     */
    void checkProxy(final String ans) {
        if ("".equals(ans) || "\n".equals(ans)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    proxyLabel.setText(": " + Tools.getString(
                                "ProxyCheckInstallation.ProxyNotInstalled"));
                    proxyIcon.setIcon(NOT_INSTALLED_ICON);
                    final String toolTip = getInstToolTip(PROXY_PREFIX, "1");
                    proxyInstMethodWi.setToolTipText(toolTip);
                    proxyButton.setToolTipText(toolTip);
                    proxyButton.setEnabled(true);
                    proxyInstMethodWi.setEnabled(true);
                }
            });
            progressBarDone();
            printErrorAndRetry(Tools.getString(
                                "Dialog.Host.CheckInstallation.SomeFailed"));
        } else {
            proxyOk = true;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    proxyLabel.setText(": " + ans.trim());
                    proxyButton.setText(Tools.getString(
                 "ProxyCheckInstallation.ProxyCheckForUpgradeButton"
                    ));
                    if (false) {
                        // TODO: disabled
                        proxyButton.setEnabled(true);
                        proxyInstMethodWi.setEnabled(true);
                    }
                    proxyIcon.setIcon(INSTALLED_ICON);
                    buttonClass(finishButton()).setEnabled(true);
                    makeDefaultAndRequestFocus(buttonClass(finishButton()));
                }
            });
            progressBarDone();
            //nextButtonSetEnabled(true);
            enableComponents();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    answerPaneSetText(Tools.getString(
                                       "Dialog.Host.CheckInstallation.AllOk"));
                }
            });
        }
    }

    /** Returns the next dialog object. It is set dynamicaly. */
    @Override
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /** Finish dialog. */
    @Override
    protected void finishDialog() {
        if (isPressedButton(finishButton())
            || isPressedButton(nextButton())) {
            if (nextDialogObject == null && origDialog != null) {
                nextDialogObject = origDialog;
                setPressedButton(nextButton());
            }
            getHost().getCluster().addProxyHost(getHost());
            if (drbdVolumeInfo != null) {
                drbdVolumeInfo.getDrbdResourceInfo().resetDrbdResourcePanel();
                drbdVolumeInfo.getDrbdInfo().addProxyHostNode(getHost());
            }
        }
    }

    /**
     * Returns the title of the dialog. It is defined as
     * ProxyCheckInstallation.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("ProxyCheckInstallation.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * ProxyCheckInstallation.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("ProxyCheckInstallation.Description");
    }

    /**
     * Returns the pane, that checks the installation of different
     * components and provides buttons to update or upgrade.
     */
    private JPanel getInstallationPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        /* get proxy installation methods */
        proxyInstMethodWi = getInstallationMethods(
                             PROXY_PREFIX,
                             Tools.getConfigData().isStagingPacemaker(),
                             null, /* last installed method */
                             PROXY_AUTO_OPTION,
                             proxyButton);
        pane.add(new JLabel("Proxy"));
        pane.add(proxyLabel);
        pane.add(proxyIcon);
        pane.add(proxyInstMethodWi);
        pane.add(proxyButton);

        SpringUtilities.makeCompactGrid(pane, 1, 5,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad
        return pane;
    }

    /** Returns input pane with installation pane and answer pane. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getInstallationPane());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString(
                                     "ProxyCheckInstallation.CheckingProxy")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  //rows, cols
                                              0, 0,  //initX, initY
                                              0, 0); //xPad, yPad

        return pane;
    }

    /** Enable skip button. */
    @Override
    protected boolean skipButtonEnabled() {
        return true;
    }

    /**
     * Return dialog that comes after "cancel" button was pressed.
     */
    @Override
    protected final WizardDialog dialogAfterCancel() {
        return origDialog;
    }
}
