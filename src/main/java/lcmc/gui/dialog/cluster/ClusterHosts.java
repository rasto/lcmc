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


package lcmc.gui.dialog.cluster;

import lcmc.data.Host;
import lcmc.data.Hosts;
import lcmc.data.Cluster;
import lcmc.utilities.Tools;
import lcmc.gui.dialog.WizardDialog;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Scrollable;

import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An implementation of a dialog where user can choose which hosts belong to
 * the cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class ClusterHosts extends DialogCluster {
    /** Host checked icon. */
    private static final ImageIcon HOST_CHECKED_ICON = Tools.createImageIcon(
            Tools.getDefault("Dialog.Cluster.ClusterHosts.HostCheckedIcon"));
    /** Host not checked icon. */
    private static final ImageIcon HOST_UNCHECKED_ICON = Tools.createImageIcon(
            Tools.getDefault("Dialog.Cluster.ClusterHosts.HostUncheckedIcon"));
    ///** Whether the scrolling pane was already moved. */
    //private volatile boolean alreadyMoved = false;
    /** Map from checkboxes to the host, which they choose. */
    private final Map<JCheckBox, Host> checkBoxToHost =
                                    new LinkedHashMap<JCheckBox, Host>();

    /** Prepares a new {@code ClusterHosts} object. */
    ClusterHosts(final WizardDialog previousDialog,
                 final Cluster cluster) {
        super(previousDialog, cluster);
    }

    /** It is executed after the dialog is applied. */
    @Override
    protected void finishDialog() {
        getCluster().clearHosts();
        for (final Map.Entry<JCheckBox, Host> checkBoxEntry : checkBoxToHost.entrySet()) {
            if (checkBoxEntry.getKey().isSelected()) {
                final Host host = checkBoxEntry.getValue();
                host.setCluster(getCluster());
                getCluster().addHost(host);
            }
        }
        Tools.getGUIData().refreshClustersPanel();
    }

    /** Returns the next dialog. */
    @Override
    public WizardDialog nextDialog() {
        boolean allConnected = true;
        for (final Host host : getCluster().getHosts()) {
            if (!host.isConnected()) {
                allConnected = false;
            }
        }
        if (allConnected) {
            return new CommStack(this, getCluster());
        } else {
            return new Connect(this, getCluster());
        }
    }

    /** Checks whether at least two hosts are selected for the cluster. */
    protected void checkCheckBoxes() {
        Tools.getApplication().getHosts().removeHostsFromCluster(getCluster());
        int selected = 0;
        for (final JCheckBox button : checkBoxToHost.keySet()) {
            if (button.isSelected()) {
                selected++;
            }
        }
        boolean enable = true;
        final Collection<String> hostnames = new ArrayList<String>();
        if (selected < 1
            || (selected == 1
                && !Tools.getApplication().isOneHostCluster())) {
            enable = false;
        } else {
            /* check if some of the hosts are the same. It will not work all
             * the time if hops are used. */
            for (final Map.Entry<JCheckBox, Host> checkBoxEntry : checkBoxToHost.entrySet()) {
                if (checkBoxEntry.getKey().isSelected() && checkBoxEntry.getKey().isEnabled()) {
                    final Host host = checkBoxEntry.getValue();
                    final String hostname = host.getHostname();
                    if (hostnames.contains(hostname)) {
                        enable = false;
                        break;
                    }
                    hostnames.add(hostname);
                }
            }
        }
        final boolean enableButton = enable;
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                buttonClass(nextButton()).setEnabled(enableButton);
            }
        });
        if (!Tools.getApplication().getAutoClusters().isEmpty()) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /** Returns the title of the dialog. */
    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.ClusterHosts.Title");
    }

    /** Returns the description of the dialog. */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.ClusterHosts.Description");
    }

    /** Inits the dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        final Thread thread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    checkCheckBoxes();
                }
            });
        thread.start();
    }

    /** Inits dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
    }

    /** Returns the panel with hosts that can be selected. */
    @Override
    protected JComponent getInputPane() {
        /* Hosts */
        final ScrollableFlowPanel p1 =
            new ScrollableFlowPanel(new FlowLayout(FlowLayout.LEADING, 1, 1));
        final Hosts hosts = Tools.getApplication().getHosts();

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
            final JCheckBox button = new JCheckBox(host.getName(),
                                                   HOST_UNCHECKED_ICON);
            button.setBackground(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));
            button.setSelectedIcon(HOST_CHECKED_ICON);
            if (getCluster().getBrowser() != null
                && getCluster() == host.getCluster()) {
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
        // TODO: it does not work all the time, it destroys the pane sometimes.
        //if (lastButton != null) {
        //    /* move the scrolling pane till the end. */
        //    final JCheckBox lb = lastButton;
        //    lb.addComponentListener(new ComponentListener() {
        //        public final void componentHidden(final ComponentEvent e) {
        //        }

        //        public final void componentMoved(final ComponentEvent e) {
        //            Tools.invokeLater(new Runnable() {
        //                @Override
        //                public void run() {
        //                    if (alreadyMoved) {
        //                        return;
        //                    }
        //                    alreadyMoved = true;
        //                    sp.getViewport().setViewPosition(
        //                                        lb.getBounds().getLocation());
        //                }
        //            });
        //        }

        //        public final void componentResized(final ComponentEvent e) {
        //        }

        //        public final void componentShown(final ComponentEvent e) {
        //        }
        //    });
        //}
        p1.setBackground(Color.WHITE);
        return sp;
    }

    /** Workaround so that flow layout scrolls right. */
    private class ScrollableFlowPanel extends JPanel
                                             implements Scrollable {
        /** Serial version UID. */
        private static final long serialVersionUID = 1L;
        /** New ScrollableFlowPanel object. */
        ScrollableFlowPanel(final LayoutManager layout) {
            super(layout);
        }

        @Override
        public void setBounds(final int x,
                              final int y,
                              final int width,
                              final int height) {
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
        public int getScrollableUnitIncrement(final Rectangle visibleRect,
                                              final int orientation,
                                              final int direction) {
            final int hundredth = (orientation ==  SwingConstants.VERTICAL
                    ? getParent().getHeight() : getParent().getWidth()) / 100;
            return (hundredth == 0 ? 1 : hundredth);
        }

        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect,
                                               final int orientation,
                                               final int direction) {
            return orientation == SwingConstants.VERTICAL
                            ? getParent().getHeight() : getParent().getWidth();
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        /** Returns preferred height. */
        private int getPreferredHeight() {
            int rv = 0;
            final int count = getComponentCount();
            for (int k = 0; k < count; k++) {
                final Component comp = getComponent(k);
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
