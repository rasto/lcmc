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


package lcmc.cluster.ui.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.domain.Application;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.host.domain.Hosts;
import lcmc.common.ui.WizardDialog;
import lcmc.common.domain.util.Tools;

/**
 * An implementation of a dialog where user can choose which hosts belong to
 * the cluster.
 */
public class ClusterHosts extends DialogCluster {

    private final CommStack commStackDialog;
    private final Connect connectDialog;
    private final MainPresenter mainPresenter;
    private final Application application;
    private final SwingUtils swingUtils;
    private final Hosts allHosts;

    private static final ImageIcon HOST_CHECKED_ICON = Tools.createImageIcon(
            Tools.getDefault("Dialog.Cluster.ClusterHosts.HostCheckedIcon"));
    private static final ImageIcon HOST_UNCHECKED_ICON = Tools.createImageIcon(
            Tools.getDefault("Dialog.Cluster.ClusterHosts.HostUncheckedIcon"));
    /** Map from checkboxes to the host, which they choose. */
    private final Map<JCheckBox, Host> checkBoxToHost = new LinkedHashMap<JCheckBox, Host>();

    public ClusterHosts(Supplier<ProgressBar> progressBarProvider, Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData, CommStack commStackDialog, Connect connectDialog, MainPresenter mainPresenter, Hosts allHosts) {
        super(progressBarProvider, application, swingUtils, widgetFactory, mainData);
        this.commStackDialog = commStackDialog;
        this.connectDialog = connectDialog;
        this.mainPresenter = mainPresenter;
        this.application = application;
        this.swingUtils = swingUtils;
        this.allHosts = allHosts;
    }

    /** It is executed after the dialog is applied. */
    @Override
    protected void finishDialog() {
        getCluster().removeAllHosts();
        for (final Map.Entry<JCheckBox, Host> checkBoxEntry : checkBoxToHost.entrySet()) {
            if (checkBoxEntry.getKey().isSelected()) {
                final Host host = checkBoxEntry.getValue();
                host.setCluster(getCluster());
                getCluster().addHost(host);
            }
        }
        mainPresenter.refreshClustersPanel();
    }

    @Override
    public WizardDialog nextDialog() {
        boolean allConnected = true;
        for (final Host host : getCluster().getHosts()) {
            if (!host.isConnected()) {
                allConnected = false;
            }
        }
        DialogCluster nextDialog;
        if (allConnected) {
            nextDialog = commStackDialog;
        } else {
            nextDialog = connectDialog;
        }
        nextDialog.init(this, getCluster());
        return nextDialog;
    }

    /** Checks whether at least two hosts are selected for the cluster. */
    protected void checkCheckBoxes() {
        allHosts.removeHostsFromCluster(getCluster());
        int selected = 0;
        for (final JCheckBox button : checkBoxToHost.keySet()) {
            if (button.isSelected()) {
                selected++;
            }
        }
        boolean enable = true;
        final Collection<String> hostNames = new ArrayList<String>();
        if (selected < 1 || (selected == 1 && !application.isOneHostCluster())) {
            enable = false;
        } else {
            /* check if some of the hosts are the same. It will not work all
             * the time if hops are used. */
            for (final Map.Entry<JCheckBox, Host> checkBoxEntry : checkBoxToHost.entrySet()) {
                if (checkBoxEntry.getKey().isSelected() && checkBoxEntry.getKey().isEnabled()) {
                    final Host host = checkBoxEntry.getValue();
                    final String hostname = host.getHostname();
                    if (hostNames.contains(hostname)) {
                        enable = false;
                        break;
                    }
                    hostNames.add(hostname);
                }
            }
        }
        final boolean enableButton = enable;
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                buttonClass(nextButton()).setEnabled(enableButton);
            }
        });
        if (!application.getAutoClusters().isEmpty()) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.ClusterHosts.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.ClusterHosts.Description");
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                checkCheckBoxes();
            }
        });
        thread.start();
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
    }

    @Override
    protected JComponent getInputPane() {
        /* Hosts */
        final ScrollableFlowPanel p1 = new ScrollableFlowPanel(new FlowLayout(FlowLayout.LEADING, 1, 1));
        final Hosts hosts = allHosts;

        final ItemListener chListener = new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    checkCheckBoxes();
                }
            };
        Host lastHost1 = null;
        Host lastHost2 = null;
        if (getCluster().getHosts().isEmpty()) {
            /* mark last two available hosts */
            for (final Host host : hosts.getHostsArray()) {
                if (!getCluster().getHosts().contains(host)
                    && !host.isInCluster()) {
                    if (lastHost2 != null
                        && lastHost2.getIpAddress() != null
                        && lastHost2.getIpAddress().equals(host.getIpAddress())) {
                        lastHost2 = host;
                    } else {
                        lastHost1 = lastHost2;
                        lastHost2 = host;
                    }
                }
            }
        }
        final JScrollPane sp = new JScrollPane(p1,
                                               JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                               JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        for (final Host host : hosts.getHostsArray()) {
            final JCheckBox button = new JCheckBox(host.getName(), HOST_UNCHECKED_ICON);
            button.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));
            button.setSelectedIcon(HOST_CHECKED_ICON);
            if (getCluster().getBrowser() != null && getCluster() == host.getCluster()) {
                /* once we have browser the cluster members cannot be removed.
                 * TODO: make it possible
                 */
                button.setEnabled(false);
            } else if (host.isInCluster(getCluster())) {
                button.setEnabled(false);
            }
            checkBoxToHost.put(button, host);
            if (getCluster().getHosts().contains(host)) {
                button.setSelected(true);
            } else {
                if (host == lastHost1 || host == lastHost2) {
                    button.setSelected(true);
                } else {
                    button.setSelected(false);
                }
            }
            button.addItemListener(chListener);
            p1.add(button);
        }
        p1.setBackground(Color.WHITE);
        return sp;
    }

    /** Workaround so that flow layout scrolls right. */
    private class ScrollableFlowPanel extends JPanel implements Scrollable {
        ScrollableFlowPanel(final LayoutManager layout) {
            super(layout);
        }

        @Override
        public void setBounds(final int x, final int y, final int width, final int height) {
            super.setBounds(x, y, getParent().getWidth(), height);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(getWidth(), getPreferredHeight());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return super.getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
            final int hundredth = (orientation ==  SwingConstants.VERTICAL
                    ? getParent().getHeight() : getParent().getWidth()) / 100;
            return hundredth == 0 ? 1 : hundredth;
        }

        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect,
                                               final int orientation,
                                               final int direction) {
            return orientation == SwingConstants.VERTICAL ? getParent().getHeight() : getParent().getWidth();
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        private int getPreferredHeight() {
            int rv = 0;
            final int count = getComponentCount();
            for (int k = 0; k < count; k++) {
                final java.awt.Component comp = getComponent(k);
                final Rectangle r = comp.getBounds();
                final int height = r.y + r.height;
                if (height > rv) {
                    rv = height;
                }
            }
            rv += ((FlowLayout) getLayout()).getVgap();
            return rv;
        }
    }
}
