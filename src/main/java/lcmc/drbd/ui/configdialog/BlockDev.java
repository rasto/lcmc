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

import java.net.UnknownHostException;

import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.AppContext;
import lcmc.Exceptions;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.MainPanel;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.drbd.service.DRBD;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.drbd.ui.resource.GlobalInfo;
import lcmc.drbd.ui.resource.ResourceInfo;
import lcmc.drbd.ui.resource.VolumeInfo;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * An implementation of a dialog where user can enter drbd block device information.
 */
@Named
final class BlockDev extends DrbdConfig {
    private static final Logger LOG = LoggerFactory.getLogger(BlockDev.class);
    private BlockDevInfo blockDevInfo;
    private final MainData mainData;
    private final MainPanel mainPanel;
    private final CreateMD createMDDialog;
    private GlobalInfo globalInfo;
    private final Application application;
    private final SwingUtils swingUtils;

    public BlockDev(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData,
            MainPanel mainPanel, CreateMD createMDDialog) {
        super(application, swingUtils, widgetFactory, mainData);
        this.mainData = mainData;
        this.mainPanel = mainPanel;
        this.createMDDialog = createMDDialog;
        this.application = application;
        this.swingUtils = swingUtils;
    }

    void init(final WizardDialog previousDialog, final VolumeInfo dli, final BlockDevInfo blockDevInfo) {
        init(previousDialog, dli);
        this.blockDevInfo = blockDevInfo;
        globalInfo = blockDevInfo.getBrowser()
                                 .getClusterBrowser()
                                 .getGlobalInfo();
        globalInfo.setSelectedNode(blockDevInfo);
        globalInfo.selectMyself();
    }

    @Override
    protected void finishDialog() {
        swingUtils.waitForSwing();
    }

    /** Calls drbdadm get-gi, to find out if there is meta-data area. */
    private String getGI(final BlockDevInfo bdi) {
        return DRBD.getGI(bdi.getHost(),
                          bdi.getDrbdVolumeInfo().getDrbdResourceInfo().getName(),
                          bdi.getDrbdVolumeInfo().getName(),
                          Application.RunMode.LIVE);
    }

    /**
     * Sets next dialog and returns it. It is either second block device, or
     * drbd config create md dialog. In the second case the drbd admin adjust
     * is called.
     */
    @Override
    public WizardDialog nextDialog() {
        if (getDrbdVolumeInfo().isFirstBlockDevInfo(blockDevInfo)) {
            final BlockDevInfo oBdi = getDrbdVolumeInfo().getOtherBlockDevInfo(blockDevInfo);
            final BlockDev nextBlockDev = AppContext.getBean(BlockDev.class);
            nextBlockDev.init(this, getDrbdVolumeInfo(), oBdi);
            return nextBlockDev;
        } else {
            final BlockDevInfo oBdi = getDrbdVolumeInfo().getOtherBlockDevInfo(blockDevInfo);
            try {
                final Application.RunMode runMode = Application.RunMode.LIVE;

                /* apply */
                final VolumeInfo dvi = getDrbdVolumeInfo();
                globalInfo.apply(runMode);
                final ResourceInfo dri = dvi.getDrbdResourceInfo();
                swingUtils.invokeAndWait(dri::getInfoPanel);
                dri.waitForInfoPanel();
                dri.apply(runMode);

                swingUtils.invokeAndWait(dvi::getInfoPanel);
                dvi.waitForInfoPanel();
                dvi.apply(runMode);
                blockDevInfo.apply(runMode);
                oBdi.apply(runMode);

                /* create config */
                globalInfo.createDrbdConfigLive();
                final String gi1 = getGI(blockDevInfo);
                final String gi2 = getGI(oBdi);
                if (gi1 == null || gi2 == null) {
                    getDrbdVolumeInfo().getDrbdResourceInfo().setHaveToCreateMD(true);
                }
                final ClusterBrowser browser = getDrbdVolumeInfo().getDrbdResourceInfo().getBrowser();
                browser.reloadAllComboBoxes(null);
                mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);
                mainData.getMainFrame().requestFocus();
            } catch (final Exceptions.DrbdConfigException | UnknownHostException dce) {
                LOG.appError("nextDialog: config failed", dce);
            }
            createMDDialog.init(this, getDrbdVolumeInfo());
            return createMDDialog;
        }
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.BlockDev.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.BlockDev.Description");
    }

    @Override
    protected void initDialogAfterVisible() {
        final String[] params = blockDevInfo.getParametersFromXML();
        swingUtils.invokeLater(
                () -> buttonClass(nextButton()).setEnabled(blockDevInfo.checkResourceFields(null, params).isCorrect()));
        enableComponents();
        if (application.getAutoOptionGlobal("autodrbd") != null) {
            pressNextButton();
        }
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel inputPane = new JPanel(new SpringLayout());
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        final String[] params = blockDevInfo.getParametersFromXML();
        blockDevInfo.selectMyself();
        blockDevInfo.waitForInfoPanel();
        blockDevInfo.addWizardParams(optionsPanel,
                                     params,
                                     buttonClass(nextButton()),
                                     application.getDefaultSize("Dialog.DrbdConfig.BlockDev.LabelWidth"),
                                     application.getDefaultSize("Dialog.DrbdConfig.BlockDev.FieldWidth"),
                                     null);

        final JPanel p = new JPanel();
        p.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Dark"));
        p.add(new JLabel(blockDevInfo.getHost().getName()));
        inputPane.add(p);
        inputPane.add(optionsPanel);
        SpringUtilities.makeCompactGrid(inputPane, 2, 1,  //rows, cols
                                                   0, 0,  //initX, initY
                                                   0, 0); //xPad, yPad
        return inputPane;
    }
}
