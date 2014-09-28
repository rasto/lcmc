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
package lcmc.view;

import java.awt.BorderLayout;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import lcmc.gui.GUIData;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostFactory;

/**
 * The very main panel, where everything is inside.
 */
@Named
@Singleton
public final class MainPanel extends JPanel {

    @Inject
    private ClustersPanel clustersPanel;
    @Inject
    private HostFactory hostFactory;
    @Inject
    private GUIData guiData;
    @Inject
    private Application application;

    public void init() {
        setLayout(new BorderLayout());
        clustersPanel.init();
        final Host noHost = hostFactory.createInstance();
        final JSplitPane splitPane =
                                new JSplitPane(JSplitPane.VERTICAL_SPLIT, clustersPanel, noHost.getTerminalPanel());
        guiData.setTerminalSplitPane(splitPane);

        splitPane.setContinuousLayout(true);
        splitPane.setResizeWeight(1.0);
        splitPane.setOneTouchExpandable(true);

        add(splitPane, BorderLayout.CENTER);
        guiData.initTerminalSplitPane();
    }
}
