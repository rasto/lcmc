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

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.network.InfoPresenter;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class holds host and cluster resource data in a tree. It shows
 * panels that allow to edit the data of resources, services etc., hosts and
 * clusters.
 * Every resource has its Info object, that accessible through the tree view.
 */
@RequiredArgsConstructor
public class Browser {
    private final Application application;

    public static final ImageIcon APPLY_ICON = Tools.createImageIcon(Tools.getDefault("Browser.ApplyIcon"));
    public static final ImageIcon REVERT_ICON = Tools.createImageIcon(Tools.getDefault("Browser.RevertIcon"));
    public static final ImageIcon ACTIONS_MENU_ICON = Tools.createImageIcon(Tools.getDefault("Browser.MenuIcon"));
    public static final Color PANEL_BACKGROUND = Tools.getDefaultColor("ViewPanel.Background");
    public static final Color BUTTON_PANEL_BACKGROUND = Tools.getDefaultColor("ViewPanel.ButtonPanel.Background");
    public static final Color STATUS_BACKGROUND = Tools.getDefaultColor("ViewPanel.Status.Background");

    private JSplitPane infoPanelSplitPane;
    private final Lock mDRBDtestLock = new ReentrantLock();

    /**
     * Returns panel with info of some resource from Info object. The info is
     * specified in getInfoPanel method in the Info object. If a resource has a
     * graphical view, it returns a split pane with this view and the info
     * underneath.
     */
    final JComponent getInfoPanel(final Object infoPresenter, final boolean disabledDuringLoad) {
        if (infoPresenter == null) {
            return null;
        }
        final JPanel gView = ((InfoPresenter) infoPresenter).getGraphicalView();
        JComponent iPanel;
        if (infoPresenter instanceof Info) {
            iPanel = ((Info) infoPresenter).getInfoPanel();
        } else {
            iPanel = new JPanel();
            iPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
            iPanel.setLayout(new BoxLayout(iPanel, BoxLayout.PAGE_AXIS));
            iPanel.setPreferredSize(iPanel.getMaximumSize());
            ((InfoPresenter) infoPresenter).show(iPanel);
        }
        if (gView == null) {
            return iPanel;
        } else {
            final int maxWidth = application.getServiceLabelWidth() + application.getServiceFieldWidth() + 36;
            iPanel.setMinimumSize(new Dimension(maxWidth, 0));
            iPanel.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
            if (infoPanelSplitPane != null) {
                if (!disabledDuringLoad) {
                    final int loc = infoPanelSplitPane.getDividerLocation();
                    if (infoPanelSplitPane.getLeftComponent() != gView) {
                        infoPanelSplitPane.setLeftComponent(gView);
                    }
                    infoPanelSplitPane.setRightComponent(iPanel);
                    infoPanelSplitPane.setDividerLocation(loc);
                }
                return infoPanelSplitPane;
            }
            final JSplitPane newSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gView, iPanel);
            newSplitPane.setResizeWeight(1.0);
            newSplitPane.setOneTouchExpandable(true);
            infoPanelSplitPane = newSplitPane;
            infoPanelSplitPane.repaint();
            return infoPanelSplitPane;
        }
    }

    public final void drbdtestLockAcquire() {
        mDRBDtestLock.lock();
    }

    public final void drbdtestLockRelease() {
        mDRBDtestLock.unlock();
    }


    protected final void repaintSplitPane() {
        if (infoPanelSplitPane != null) {
            infoPanelSplitPane.repaint();
        }
    }

    public void fireEventInViewPanel(final DefaultMutableTreeNode node) {
    }
}
