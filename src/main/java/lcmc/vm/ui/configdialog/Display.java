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


package lcmc.vm.ui.configdialog;

import java.awt.Dimension;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lcmc.common.domain.Application;
import lcmc.vm.domain.GraphicsData;
import lcmc.common.ui.WizardDialog;
import lcmc.vm.ui.resource.GraphicsInfo;
import lcmc.common.domain.util.Tools;

/**
 * An implementation of a dialog where user can enter a new domain.
 */
@Named
final class Display extends VMConfig {
    private static final String[] PARAMS = {GraphicsData.TYPE,
                                            GraphicsData.PORT,
                                            GraphicsData.LISTEN,
                                            GraphicsData.PASSWD,
                                            GraphicsData.KEYMAP,
                                            GraphicsData.DISPLAY,
                                            GraphicsData.XAUTH};
    private JComponent inputPane = null;
    private GraphicsInfo graphicsInfo = null;
    private WizardDialog nextDialogObject = null;
    @Inject
    private VMFinish vmFinishDialog;
    @Inject
    private Application application;

    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            vmFinishDialog.init(this, getVMSVirtualDomainInfo());
            nextDialogObject = vmFinishDialog;
        }
        return nextDialogObject;
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.vm.Display.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Display.Description");
    }

    @Override
    protected void initDialogBeforeCreated() {
        if (graphicsInfo == null) {
            graphicsInfo = getVMSVirtualDomainInfo().addGraphicsPanel();
            graphicsInfo.waitForInfoPanel();
        } else {
            graphicsInfo.selectMyself();
        }
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                final boolean enable = graphicsInfo.checkResourceFields(null, graphicsInfo.getRealParametersFromXML())
                                                   .isCorrect();
                buttonClass(nextButton()).setEnabled(enable);
            }
        });
    }

    @Override
    protected JComponent getInputPane() {
        if (inputPane != null) {
            return inputPane;
        }
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentY(java.awt.Component.TOP_ALIGNMENT);
        graphicsInfo.savePreferredValues();
        graphicsInfo.getResource().setValue(GraphicsData.TYPE, GraphicsInfo.TYPE_VNC);
        graphicsInfo.getResource().setValue(GraphicsData.PORT, GraphicsInfo.PORT_AUTO);

        graphicsInfo.addWizardParams(optionsPanel,
                                     PARAMS,
                                     buttonClass(nextButton()),
                                     application.getDefaultSize("Dialog.vm.Resource.LabelWidth"),
                                     application.getDefaultSize("Dialog.vm.Resource.FieldWidth"),
                                     null);

        panel.add(optionsPanel);
        final JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        inputPane = scrollPane;
        return scrollPane;
    }
}
