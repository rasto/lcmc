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

import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.vm.ui.resource.DomainInfo;

/**
 * An implementation of a dialog where user can enter a new domain.
 */
@Named
final class VMFinish extends VMConfig {
    private JComponent inputPane = null;
    private final SwingUtils swingUtils;
    private final WidgetFactory widgetFactory;

    public VMFinish(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData) {
        super(application, swingUtils, widgetFactory, mainData);
        this.swingUtils = swingUtils;
        this.widgetFactory = widgetFactory;
    }

    @Override
    public WizardDialog nextDialog() {
        return null;
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.vm.Finish.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.vm.Finish.Description");
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(finishButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        swingUtils.invokeLater(() -> buttonClass(finishButton()).setEnabled(false));
    }

    @Override
    protected JComponent getInputPane() {
        final DomainInfo domainInfo = getVMSVirtualDomainInfo();
        domainInfo.selectMyself();
        if (inputPane != null) {
            return inputPane;
        }
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        final MyButton createConfigBtn = widgetFactory.createButton("Create Config");
        createConfigBtn.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
        createConfigBtn.addActionListener(e -> {
            final Thread thread = new Thread(() -> {
                swingUtils.invokeAndWait(() -> createConfigBtn.setEnabled(false));
                domainInfo.apply(Application.RunMode.LIVE);
                swingUtils.invokeLater(() -> buttonClass(finishButton()).setEnabled(true));
            });
            thread.start();
        });
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentY(java.awt.Component.TOP_ALIGNMENT);
        domainInfo.waitForInfoPanel();
        optionsPanel.add(domainInfo.getDefinedOnHostsPanel(Widget.WIZARD_PREFIX, createConfigBtn));

        optionsPanel.add(createConfigBtn);
        panel.add(optionsPanel);

        final JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
        scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, 200));
        inputPane = scrollPane;
        return scrollPane;
    }
}
