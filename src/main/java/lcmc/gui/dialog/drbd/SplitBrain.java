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


package lcmc.gui.dialog.drbd;

import lcmc.utilities.Tools;
import lcmc.utilities.DRBD;
import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.DrbdVolumeInfo;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.dialog.drbdConfig.DrbdConfig;
import lcmc.utilities.MyButton;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.Set;

/**
 * An implementation of a dialog where drbd block devices are initialized.
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class SplitBrain extends DrbdConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Combo box with host that has more recent data. */
    private Widget hostWi;
    /** Resolve split brain button. */
    private final MyButton resolveButton = new MyButton(
                    Tools.getString("Dialog.Drbd.SplitBrain.ResolveButton"));
    /** Width of the combo box. */
    private static final int COMBOBOX_WIDTH = 160;

    /** Prepares a new <code>SplitBrain</code> object. */
    public SplitBrain(final WizardDialog previousDialog,
               final DrbdVolumeInfo dvi) {
        super(previousDialog, dvi);
    }

    /** Resolves the split brain. */
    protected void resolve() {
        final Host h1 = getDrbdVolumeInfo().getFirstBlockDevInfo().getHost();
        final Host h2 = getDrbdVolumeInfo().getSecondBlockDevInfo().getHost();
        final String h = hostWi.getStringValue();

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Host hostPri;
                Host hostSec;
                if (h.equals(h1.getName())) {
                    hostPri = h1;
                    hostSec = h2;
                } else if (h.equals(h2.getName())) {
                    hostPri = h2;
                    hostSec = h1;
                } else {
                    Tools.appError("unknown host: " + h);
                    return;
                }
                buttonClass(finishButton()).setEnabled(false);
                resolveButton.setEnabled(false);
                final boolean testOnly = false;
                final String resName =
                          getDrbdVolumeInfo().getDrbdResourceInfo().getName();
                DRBD.setSecondary(hostSec,
                                  resName,
                                  getDrbdVolumeInfo().getName(),
                                  testOnly);
                DRBD.disconnect(hostSec,
                                resName,
                                getDrbdVolumeInfo().getName(),
                                testOnly);
                DRBD.discardData(hostSec, resName, null, testOnly);
                getDrbdVolumeInfo().connect(hostPri, testOnly);
                buttonClass(finishButton()).setEnabled(true);
                buttonClass(cancelButton()).setEnabled(false);
            }
        };
        final Thread thread = new Thread(runnable);
        thread.start();
    }

    /** Returns next dialog which is null. */
    @Override
    public WizardDialog nextDialog() {
        return null;
    }

    /**
     * Returns the title for the dialog. It is defined in TextResources as
     * Dialog.Drbd.SplitBrain.Title.
     */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.Drbd.SplitBrain.Title");
    }

    /**
     * Returns the description for the dialog. It is defined in TextResources
     * as Dialog.Drbd.SplitBrain.Description.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Drbd.SplitBrain.Description");
    }

    /** Inits the dialog. */
    @Override
    protected void initDialog() {
        super.initDialog();
        resolveButton.setBackgroundColor(
                               Tools.getDefaultColor("ConfigDialog.Button"));
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
    }

    /**
     * Returns an input pane, where user can select the host with more recent
     * data.
     */
    @Override
    protected JComponent getInputPane() {
        final JPanel inputPane = new JPanel(new SpringLayout());
        /* host */
        final Set<Host> hosts = getDrbdVolumeInfo().getHosts();
        final JLabel hostLabel = new JLabel(
                        Tools.getString("Dialog.Drbd.SplitBrain.ChooseHost"));
        hostWi = WidgetFactory.createInstance(
                                    Widget.Type.COMBOBOX,
                                    Widget.NO_DEFAULT,
                                    hosts.toArray(new Host[hosts.size()]),
                                    Widget.NO_REGEXP,
                                    COMBOBOX_WIDTH,
                                    Widget.NO_ABBRV,
                                    new AccessMode(ConfigData.AccessType.RO,
                                                   !AccessMode.ADVANCED),
                                    Widget.NO_BUTTON);
        inputPane.add(hostLabel);
        inputPane.add(hostWi);
        resolveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                resolve();
            }
        });
        inputPane.add(resolveButton);

        SpringUtilities.makeCompactGrid(inputPane, 1, 3,  //rows, cols
                                                   1, 1,  //initX, initY
                                                   1, 1); //xPad, yPad

        return inputPane;
    }
}
