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

package lcmc.gui.dialog.drbdConfig;

import java.net.UnknownHostException;
import javax.inject.Provider;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.AppContext;
import lcmc.Exceptions;
import lcmc.LCMC;
import lcmc.gui.GUIData;
import lcmc.gui.resources.drbd.GlobalInfo;
import lcmc.model.Application;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.resources.drbd.ResourceInfo;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.utilities.DRBD;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * An implementation of a dialog where user can enter drbd block device
 * information.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
final class BlockDev extends DrbdConfig {
    private static final Logger LOG = LoggerFactory.getLogger(BlockDev.class);
    private BlockDevInfo blockDevInfo;
    @Autowired
    private GUIData guiData;
    @Autowired
    private CreateMD createMDDialog;
    private GlobalInfo globalInfo;

    void init(final WizardDialog previousDialog, final VolumeInfo dli, final BlockDevInfo blockDevInfo) {
        init(previousDialog, dli);
        this.blockDevInfo = blockDevInfo;
        globalInfo = blockDevInfo.getBrowser().getClusterBrowser().getGlobalInfo();
        globalInfo.setSelectedNode(blockDevInfo);
        globalInfo.selectMyself();
    }

    @Override
    protected void finishDialog() {
        Tools.waitForSwing();
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
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        dri.getInfoPanel();
                    }
                });
                dri.waitForInfoPanel();
                dri.apply(runMode);

                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        dvi.getInfoPanel();
                    }
                });
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
                guiData.expandTerminalSplitPane(1);
                LCMC.MAIN_FRAME.requestFocus();
            } catch (final Exceptions.DrbdConfigException dce) {
                LOG.appError("nextDialog: config failed", dce);
            } catch (final UnknownHostException e) {
                LOG.appError("nextDialog: config failed", e);
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
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                buttonClass(nextButton()).setEnabled(
                   blockDevInfo.checkResourceFields(null, params).isCorrect());
            }
        });
        enableComponents();
        if (Tools.getApplication().getAutoOptionGlobal("autodrbd") != null) {
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
                                     Tools.getDefaultSize("Dialog.DrbdConfig.BlockDev.LabelWidth"),
                                     Tools.getDefaultSize("Dialog.DrbdConfig.BlockDev.FieldWidth"),
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
