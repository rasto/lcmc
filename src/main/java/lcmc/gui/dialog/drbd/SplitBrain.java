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


package lcmc.gui.dialog.drbd;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.dialog.drbdConfig.DrbdConfig;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.DRBD;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * An implementation of a dialog where drbd block devices are initialized.
 * information.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public final class SplitBrain extends DrbdConfig {
    private static final Logger LOG = LoggerFactory.getLogger(SplitBrain.class);
    private static final int COMBOBOX_WIDTH = 160;
    /** Combo box with host that has more recent data. */
    private Widget hostWithBetterDataWidget;
    private final MyButton resolveButton = new MyButton(Tools.getString("Dialog.Drbd.SplitBrain.ResolveButton"));

    protected void resolve() {
        final Host h1 = getDrbdVolumeInfo().getFirstBlockDevInfo().getHost();
        final Host h2 = getDrbdVolumeInfo().getSecondBlockDevInfo().getHost();
        final String h = hostWithBetterDataWidget.getStringValue();

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final Host hostPri;
                final Host hostSec;
                if (h.equals(h1.getName())) {
                    hostPri = h1;
                    hostSec = h2;
                } else if (h.equals(h2.getName())) {
                    hostPri = h2;
                    hostSec = h1;
                } else {
                    LOG.appError("resolve: unknown host: " + h);
                    return;
                }
                buttonClass(finishButton()).setEnabled(false);
                resolveButton.setEnabled(false);
                final Application.RunMode runMode = Application.RunMode.LIVE;
                final String resName = getDrbdVolumeInfo().getDrbdResourceInfo().getName();
                DRBD.setSecondary(hostSec, resName, getDrbdVolumeInfo().getName(), runMode);
                DRBD.disconnect(hostSec, resName, getDrbdVolumeInfo().getName(), runMode);
                DRBD.discardData(hostSec, resName, null, runMode);
                getDrbdVolumeInfo().connect(hostPri, runMode);
                buttonClass(finishButton()).setEnabled(true);
                buttonClass(cancelButton()).setEnabled(false);
            }
        };
        final Thread thread = new Thread(runnable);
        thread.start();
    }

    @Override
    public WizardDialog nextDialog() {
        return null;
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.Drbd.SplitBrain.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Drbd.SplitBrain.Description");
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        resolveButton.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel inputPane = new JPanel(new SpringLayout());
        /* host */
        final Set<Host> hosts = getDrbdVolumeInfo().getHosts();
        final JLabel hostLabel = new JLabel(Tools.getString("Dialog.Drbd.SplitBrain.ChooseHost"));
        final Host[] hostsArray = hosts.toArray(new Host[hosts.size()]);
        hostWithBetterDataWidget = WidgetFactory.createInstance(
                                    Widget.Type.COMBOBOX,
                                    hostsArray[0],
                                    hostsArray,
                                    Widget.NO_REGEXP,
                                    COMBOBOX_WIDTH,
                                    Widget.NO_ABBRV,
                                    new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                                    Widget.NO_BUTTON);
        inputPane.add(hostLabel);
        inputPane.add(hostWithBetterDataWidget.getComponent());
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
