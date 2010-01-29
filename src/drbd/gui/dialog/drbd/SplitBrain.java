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


package drbd.gui.dialog.drbd;

import drbd.utilities.Tools;
import drbd.utilities.DRBD;
import drbd.data.Host;
import drbd.gui.SpringUtilities;
import drbd.gui.resources.DrbdResourceInfo;
import drbd.gui.GuiComboBox;
import drbd.gui.dialog.WizardDialog;
import drbd.gui.dialog.drbdConfig.DrbdConfig;
import drbd.utilities.MyButton;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * An implementation of a dialog where drbd block devices are initialized.
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class SplitBrain extends DrbdConfig {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Combo box with host that has more recent data. */
    private GuiComboBox hostCB;
    /** Resolve split brain button. */
    private final MyButton resolveButton = new MyButton(
                    Tools.getString("Dialog.Drbd.SplitBrain.ResolveButton"));
    /** Width of the combo box. */
    private static final int COMBOBOX_WIDTH = 160;

    /**
     * Prepares a new <code>SplitBrain</code> object.
     */
    public SplitBrain(final WizardDialog previousDialog,
                          final DrbdResourceInfo dri) {
        super(previousDialog, dri);
    }

    /**
     * Resolves the split brain.
     */
    protected final void resolve() {
        final Host h1 = getDrbdResourceInfo().getFirstBlockDevInfo().getHost();
        final Host h2 = getDrbdResourceInfo().getSecondBlockDevInfo().getHost();
        final String h = hostCB.getStringValue();

        final Runnable runnable = new Runnable() {
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
                DRBD.setSecondary(hostSec,
                                  getDrbdResourceInfo().getName(),
                                  testOnly);
                DRBD.disconnect(hostSec,
                                getDrbdResourceInfo().getName(),
                                testOnly);
                DRBD.discardData(hostSec,
                                 getDrbdResourceInfo().getName(),
                                 testOnly);
                getDrbdResourceInfo().connect(hostPri, testOnly);
                buttonClass(finishButton()).setEnabled(true);
                buttonClass(cancelButton()).setEnabled(false);
            }
        };
        final Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Returns next dialog which is null.
     */
    public final WizardDialog nextDialog() {
        return null;
    }

    /**
     * Returns the title for the dialog. It is defined in TextResources as
     * Dialog.Drbd.SplitBrain.Title.
     */
    protected final String getDialogTitle() {
        return Tools.getString("Dialog.Drbd.SplitBrain.Title");
    }

    /**
     * Returns the description for the dialog. It is defined in TextResources
     * as Dialog.Drbd.SplitBrain.Description.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.Drbd.SplitBrain.Description");
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponents();
    }

    /**
     * Returns an input pane, where user can select the host with more recent
     * data.
     */
    protected final JComponent getInputPane() {
        final JPanel inputPane = new JPanel(new SpringLayout());

        /* host */
        final Host[] hosts = getDrbdResourceInfo().getHosts();
        final JLabel hostLabel = new JLabel(
                        Tools.getString("Dialog.Drbd.SplitBrain.ChooseHost"));
        hostCB = new GuiComboBox(null,
                                 hosts,
                                 GuiComboBox.Type.COMBOBOX,
                                 null,
                                 COMBOBOX_WIDTH,
                                 null);
        inputPane.add(hostLabel);
        inputPane.add(hostCB);
        resolveButton.addActionListener(new ActionListener() {
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
