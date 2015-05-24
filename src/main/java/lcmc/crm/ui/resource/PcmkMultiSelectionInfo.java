/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2011-2012, Rasto Levrinc
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
package lcmc.crm.ui.resource;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lcmc.common.domain.AccessMode;
import lcmc.common.ui.GUIData;
import lcmc.common.domain.Application;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.crm.domain.ClusterStatus;
import lcmc.host.domain.Host;
import lcmc.crm.domain.PtestData;
import lcmc.common.domain.Value;
import lcmc.common.ui.Browser;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.ui.EditableInfo;
import lcmc.common.ui.Info;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.crm.service.CRM;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;

/**
 * This class provides menus for service and host multi selection.
 */
@Named
public class PcmkMultiSelectionInfo extends EditableInfo {
    private static final Logger LOG = LoggerFactory.getLogger(PcmkMultiSelectionInfo.class);
    private List<Info> selectedInfos;
    @Inject
    private PcmkMultiSelectionMenu pcmkMultiSelectionMenu;
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;

    public void init(final List<Info> selectedInfos, final Browser browser) {
        super.init("selection", browser);
        this.selectedInfos = selectedInfos;
    }

    @Override
    protected String getInfoMimeType() {
        return GUIData.MIME_TYPE_TEXT_HTML;
    }

    @Override
    public String getInfo() {
        final StringBuilder s = new StringBuilder(80);
        s.append(Tools.getString("PcmkMultiSelectionInfo.Selection"));
        for (final Info si : selectedInfos) {
            if (si != null) {
                s.append(si);
            }
            s.append("<br />");
        }
        return s.toString();
    }
    @Override
    public List<UpdatableItem> createPopup() {
        return pcmkMultiSelectionMenu.getPulldownMenu(this);
    }

    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getCrmGraph().getGraphPanel();
    }

    @Override
    protected AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
        return AccessMode.NORMAL;
    }

    @Override
    protected AccessMode.Type getAccessType(final String param) {
        return null;
    }

    @Override
    protected String getSection(final String param) {
        return null;
    }

    @Override
    protected boolean isRequired(final String param) {
        return false;
    }

    @Override
    protected boolean isAdvanced(final String param) {
        return false;
    }

    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    @Override
    protected boolean isInteger(final String param) {
        return false;
    }

    @Override
    protected boolean isLabel(final String param) {
        return false;
    }

    @Override
    protected boolean isTimeType(final String param) {
        return false;
    }

    @Override
    protected boolean isCheckBox(final String param) {
        return false;
    }

    @Override
    protected String getParamType(final String param) {
        return null;
    }

    @Override
    public String[] getParametersFromXML() {
        return null;
    }

    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        return null;
    }

    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        return true;
    }

    @Override
    public Value getParamDefault(final String param) {
        return null;
    }

    @Override
    protected Value getParamPreferred(final String param) {
        return null;
    }

    @Override
    protected String getParamShortDesc(final String param) {
        return null;
    }

    @Override
    protected String getParamLongDesc(final String param) {
        return null;
    }

    @Override
    public JComponent getInfoPanel() {
        swingUtils.isSwingThread();
        final boolean abExisted = getApplyButton() != null;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;
            /**
             * Whether the whole thing should be enabled.
             */
            @Override
            public boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                return dcHost != null && !Tools.versionBeforePacemaker(dcHost);
            }
            @Override
            public void mouseOut(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getCrmGraph().stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }

            @Override
            public void mouseOver(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                component.setToolTipText(ClusterBrowser.STARTING_PTEST_TOOLTIP);
                component.setToolTipBackground(Tools.getDefaultColor("ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getCrmGraph().startTestAnimation((JComponent) component, startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                try {
                    final ClusterStatus cs = getBrowser().getClusterStatus();
                    cs.setPtestResult(null);
                    apply(dcHost, Application.RunMode.TEST);
                    final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                    component.setToolTipText(ptestData.getToolTip());
                    cs.setPtestResult(ptestData);
                } finally {
                    getBrowser().ptestLockRelease();
                }
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);
        /* add item listeners to the apply button. */
        if (!abExisted) {
            getApplyButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        LOG.debug1("getInfoPanel: BUTTON: apply");
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), Application.RunMode.LIVE);
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );

            getRevertButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        LOG.debug1("getInfoPanel: BUTTON: revert");
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                revert();
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
        }
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final AbstractButton serviceMenu = getActionsButton();
        buttonPanel.add(serviceMenu, BorderLayout.LINE_END);

        /* apply button */
        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);
        final String[] params = getParametersFromXML();
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                /* invoke later on purpose  */
                setApplyButtons(null, params);
            }
        });
        mainPanel.add(optionsPanel);
        mainPanel.add(super.getInfoPanel());
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(application.getServiceLabelWidth() + application.getServiceFieldWidth() + 4));
        newPanel.add(new JScrollPane(mainPanel));
        /* if id textfield was changed and this id is not used,
         * enable apply button */
        infoPanelDone();
        return newPanel;
    }

    public List<Info> getSelectedInfos() {
        return selectedInfos;
    }

    /**
     * Apply the changes to the service parameters.
     * not implemented
     */
    void apply(final Host dcHost, final Application.RunMode runMode) {
    }
}
