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
package lcmc.common.ui;

import java.awt.BorderLayout;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import lcmc.cluster.ui.ClustersPanel;
import lcmc.common.ui.utils.SwingUtils;
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
    private SwingUtils swingUtils;

    private JSplitPane terminalSplitPane;
    private boolean terminalAreaExpanded = true;
    private int lastDividerLocation = -1;

    public void init() {
        setLayout(new BorderLayout());
        clustersPanel.init();
        final Host noHost = hostFactory.createInstance();
        terminalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, clustersPanel, noHost.getTerminalPanel());

        terminalSplitPane.setContinuousLayout(true);
        terminalSplitPane.setResizeWeight(1.0);
        terminalSplitPane.setOneTouchExpandable(true);

        add(terminalSplitPane, BorderLayout.CENTER);
        initTerminalSplitPane();
    }

    public void setTerminalPanel(final java.awt.Component terminalPanel) {
        if (terminalPanel == null) {
            return;
        }
        swingUtils.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                final java.awt.Component oldTerminalPanel = terminalSplitPane.getBottomComponent();
                if (!terminalPanel.equals(oldTerminalPanel)) {
                    expandTerminalSplitPane(TerminalSize.EXPAND);
                    terminalSplitPane.setBottomComponent(terminalPanel);
                    expandTerminalSplitPane(TerminalSize.COLLAPSE);
                }
            }
        });
    }

    /** Returns the position of the terminal panel. */
    public int getTerminalPanelPos() {
        if (terminalSplitPane.getBottomComponent() == null) {
            return 0;
        } else {
            return getY() + terminalSplitPane.getBottomComponent().getY();
        }
    }

    public boolean isTerminalPanelExpanded() {
        return terminalSplitPane.getBottomComponent().getSize().getHeight() != 0;
    }

    public void expandTerminalSplitPane(final TerminalSize terminalSize) {
        if (terminalSplitPane == null) {
            return;
        }
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                final int height = terminalSplitPane.getHeight() - terminalSplitPane.getDividerLocation() - 11;
                if (!terminalAreaExpanded && terminalSize == TerminalSize.EXPAND) {
                    terminalAreaExpanded = true;
                    lastDividerLocation = terminalSplitPane.getDividerLocation();
                    if (height < 10) {
                        terminalSplitPane.setDividerLocation(terminalSplitPane.getHeight() - 150);
                    }
                } else if (terminalAreaExpanded && terminalSize == TerminalSize.COLLAPSE) {
                    terminalAreaExpanded = false;
                    if (lastDividerLocation < 0) {
                        terminalSplitPane.setDividerLocation(1.0);
                    } else {
                        terminalSplitPane.setDividerLocation(lastDividerLocation);
                    }
                }
            }
        });
    }

    public void initTerminalSplitPane() {
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                final BasicSplitPaneUI ui = (BasicSplitPaneUI) terminalSplitPane.getUI();
                final BasicSplitPaneDivider divider = ui.getDivider();
                final JButton button = (JButton) divider.getComponent(1);
                button.doClick();
            }
        });
    }

    public enum TerminalSize {
        EXPAND,
        COLLAPSE
    }
}
