/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2015, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.common.ui.main;

import lcmc.common.domain.AllHostsUpdatable;
import lcmc.common.domain.Application;
import lcmc.common.domain.UserConfig;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;

@Named
@Singleton
public class MainPresenter {
    private static final Logger LOG = LoggerFactory.getLogger(MainPresenter.class);

    private static final int DIALOG_PANEL_WIDTH = 400;
    private static final int DIALOG_PANEL_HEIGHT = 300;
    private static final Dimension DIALOG_PANEL_SIZE = new Dimension(DIALOG_PANEL_WIDTH, DIALOG_PANEL_HEIGHT);

    @Inject
    private SwingUtils swingUtils;
    @Inject
    private Application application;
    @Inject
    private ProgressIndicator progressIndicator;
    @Inject
    private UserConfig userConfig;
    @Inject
    private MainData mainData;

    public void renameSelectedClusterTab(final String newName) {
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                mainData.getClustersPanel().renameSelectedTab(newName);
            }
        });
    }

    /**
     * This is used, if cluster was added, but than it was canceled.
     */
    public void removeSelectedClusterTab() {
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                mainData.getClustersPanel().removeTab();
            }
        });
    }

    /** Revalidates and repaints clusters panel. */
    public void refreshClustersPanel() {
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                mainData.getClustersPanel().refreshView();
            }
        });
    }

    /**
     * Adds the 'Add Cluster' button to the list, so that it can be enabled or
     * disabled.
     */
    public void registerAddClusterButton(final JComponent addClusterButton) {
        mainData.registerAddClusterButton(addClusterButton);
        addClusterButton.setEnabled(application.danglingHostsCount() >= 1);
    }

    /**
     * Checks 'Add Cluster' buttons and menu items and enables them, if there
     * are enough hosts to make cluster.
     */
    public void checkAddClusterButtons() {
        swingUtils.isSwingThread();
        final boolean enabled = application.danglingHostsCount() >= 1;

        for (final JComponent addClusterButton : mainData.getAddClusterButtonList()) {
            addClusterButton.setEnabled(enabled);
        }
    }

    public void enableAddClusterButtons(final boolean enable) {
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final JComponent addClusterButton : mainData.getAddClusterButtonList()) {
                    addClusterButton.setEnabled(enable);
                }
            }
        });
    }

    public void enableAddHostButtons(final boolean enable) {
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final JComponent addHostButton : mainData.getAddHostButtonList()) {
                    addHostButton.setEnabled(enable);
                }
            }
        });
    }

    /** Calls allHostsUpdate method on all registered components. */
    public void allHostsUpdate() {
        for (final AllHostsUpdatable component : mainData.getAllHostsUpdateList()) {
            component.allHostsUpdate();
        }
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                checkAddClusterButtons();
            }
        });
    }

    /** Dialog that informs a user about something with ok button. */
    public void infoDialog(final String title, final String info1, final String info2) {
        final JEditorPane infoPane = new JEditorPane(MainData.MIME_TYPE_TEXT_PLAIN, info1 + '\n' + info2);
        infoPane.setEditable(false);
        infoPane.setMinimumSize(DIALOG_PANEL_SIZE);
        infoPane.setMaximumSize(DIALOG_PANEL_SIZE);
        infoPane.setPreferredSize(DIALOG_PANEL_SIZE);
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(mainData.getMainFrame(),
                        new JScrollPane(infoPane),
                        title,
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /** Removes all the hosts and clusters from all the panels and data. */
    public void removeEverything() {
        progressIndicator.startProgressIndicator(Tools.getString("MainMenu.RemoveEverything"));
        application.disconnectAllHosts();
        mainData.getClustersPanel().removeAllTabs();
        progressIndicator.stopProgressIndicator(Tools.getString("MainMenu.RemoveEverything"));
    }

    public void saveConfig(final String filename,
                           final boolean saveAll) {
        LOG.debug1("save: start");
        final String text = Tools.getString("Tools.Saving").replaceAll("@FILENAME@",
                Matcher.quoteReplacement(filename));
        progressIndicator.startProgressIndicator(text);
        userConfig.saveConfig(filename, saveAll);
        progressIndicator.stopProgressIndicator(text);
    }


}
