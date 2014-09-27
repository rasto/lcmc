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


package lcmc.drbd.ui.configdialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.StringValue;
import lcmc.model.Value;
import lcmc.gui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.resources.drbd.GlobalInfo;
import lcmc.gui.resources.drbd.ResourceInfo;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog where user start to configure the DRBD.
 * information.
 */
@Named
public final class Start extends WizardDialog {
    private static final int COMBOBOX_WIDTH = 250;
    private Widget drbdResourceWidget;
    private BlockDevInfo blockDevInfo1;
    private BlockDevInfo blockDevInfo2;
    private ResourceInfo resourceInfo;
    @Inject
    private Resource resourceDialog;
    @Inject
    private Volume volumeDialog;
    private GlobalInfo globalInfo;
    @Inject
    private Application application;
    @Inject
    private WidgetFactory widgetFactory;

    public void init(final WizardDialog previousDialog,
                     final BlockDevInfo blockDevInfo1,
                     final BlockDevInfo blockDevInfo2) {
        setPreviousDialog(previousDialog);
        this.blockDevInfo1 = blockDevInfo1;
        this.blockDevInfo2 = blockDevInfo2;
        globalInfo = blockDevInfo1.getBrowser().getClusterBrowser().getGlobalInfo();
    }

    /** Applies the changes and returns next dialog (BlockDev). */
    @Override
    public WizardDialog nextDialog() {
        boolean newResource = false;
        final Value i = drbdResourceWidget.getValue();
        if (i == null || i.isNothingSelected()) {
            final Iterable<BlockDevInfo> bdis =
                                            new ArrayList<BlockDevInfo>(Arrays.asList(blockDevInfo1, blockDevInfo2));
            resourceInfo = globalInfo.getNewDrbdResource(VolumeInfo.getHostsFromBlockDevices(bdis));
            globalInfo.addDrbdResource(resourceInfo);
            newResource = true;
        } else {
            resourceInfo = (ResourceInfo) i;
        }
        final VolumeInfo dvi = globalInfo.getNewDrbdVolume(
                                             resourceInfo,
                                             new ArrayList<BlockDevInfo>(Arrays.asList(blockDevInfo1, blockDevInfo2)));
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                resourceInfo.addDrbdVolume(dvi);
                globalInfo.addDrbdVolume(dvi);
            }
        });
        if (newResource) {
            resourceDialog.init(this, dvi);
            return resourceDialog;
        } else {
            volumeDialog.init(this, dvi);
            return volumeDialog;
        }
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.Start.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.Start.Description");
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        if (application.getAutoOptionGlobal("autodrbd") != null) {
            pressNextButton();
        }
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());

        /* Drbd Resource */
        final JLabel drbdResourceLabel = new JLabel(Tools.getString("Dialog.DrbdConfig.Start.DrbdResource"));
        final String newDrbdResource = Tools.getString("Dialog.DrbdConfig.Start.NewDrbdResource");
        final List<Value> choices = new ArrayList<Value>();
        choices.add(new StringValue(null, newDrbdResource));
        for (final ResourceInfo dri : globalInfo.getDrbdResources()) {
            choices.add(dri);
        }
        drbdResourceWidget = widgetFactory.createInstance(
                                    Widget.Type.COMBOBOX,
                                    Widget.NO_DEFAULT,
                                    choices.toArray(new Value[choices.size()]),
                                    Widget.NO_REGEXP,
                                    COMBOBOX_WIDTH,
                                    Widget.NO_ABBRV,
                                    new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                                    Widget.NO_BUTTON);
        inputPane.add(drbdResourceLabel);
        inputPane.add(drbdResourceWidget.getComponent());
        SpringUtilities.makeCompactGrid(inputPane, 1, 2,  // rows, cols
                                                   1, 1,  // initX, initY
                                                   1, 1); // xPad, yPad

        pane.add(inputPane);
        pane.add(getProgressBarPane(null));
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad

        return pane;
    }
}
