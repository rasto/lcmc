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

package lcmc.host.ui;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lombok.val;

import java.util.function.Supplier;

/**
 * An implementation of a dialog that shows which distribution was detected.
 */
final class DistDetection extends DialogHost {
    private final CheckInstallation checkInstallation;
    private final Application application;

    public DistDetection(Supplier<ProgressBar> progressBarProvider, Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData, CheckInstallation checkInstallation) {
        super(progressBarProvider, application, swingUtils, widgetFactory, mainData);
        this.checkInstallation = checkInstallation;
        this.application = application;
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        val hostParser = getHost().getHostParser();
        final String support = hostParser.getDistString("Support");
        final String answerText = "\nversion: " + hostParser.getDetectedInfo() + " (support file: " + support + ')';
        answerPaneSetText(answerText);
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        if (!application.getAutoHosts().isEmpty()) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    @Override
    public WizardDialog nextDialog() {
        checkInstallation.init(this, getHost(), getDrbdInstallation());
        return checkInstallation;
    }

    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DistDetection.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.DistDetection.Description");
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.DistDetection.Executing")));
        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }
}
