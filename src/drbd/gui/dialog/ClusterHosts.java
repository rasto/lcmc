/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.gui.dialog;

import drbd.data.Host;
import drbd.data.Hosts;
import drbd.data.Cluster;
import drbd.utilities.Tools;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.Map;
import java.util.LinkedHashMap;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import java.awt.FlowLayout;

import javax.swing.JComponent;

/**
 * An implementation of a dialog where user can choose which hosts belong to
 * the cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ClusterHosts extends DialogCluster {

    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Map from checkboxes to the host, which they choose. */
    private final Map<JCheckBox, Host> checkBoxToHost =
                                    new LinkedHashMap<JCheckBox, Host>();
    /** Host checked icon. */
    private static final ImageIcon HOST_CHECKED_ICON = Tools.createImageIcon(
                Tools.getDefault("Dialog.ClusterHosts.HostCheckedIcon"));
    /** Host not checked icon. */
    private static final ImageIcon HOST_UNCHECKED_ICON = Tools.createImageIcon(
                Tools.getDefault("Dialog.ClusterHosts.HostUncheckedIcon"));

    /**
     * Prepares a new <code>ClusterHosts</code> object.
     */
    public ClusterHosts(final WizardDialog previousDialog,
                        final Cluster cluster) {
        super(previousDialog, cluster);
    }

    /**
     * It is executed after the dialog is applied.
     */
    protected final void finishDialog() {
        getCluster().clearHosts();
        for (final JCheckBox button : checkBoxToHost.keySet()) {
            if (button.isSelected()) {
                final Host host = checkBoxToHost.get(button);
                host.setCluster(getCluster());
                getCluster().addHost(host);
            }
        }
        Tools.getGUIData().refreshClustersPanel();
        checkBoxToHost.clear();
    }

    /**
     * Returns the next dialog.
     */
    public final WizardDialog nextDialog() {
        return new ClusterConnect(this, getCluster());
    }

    /**
     * Checks whether at least two hosts are selected for the cluster.
     */
    protected final void checkCheckBoxes() {
        Tools.getConfigData().getHosts().removeHostsFromCluster(getCluster());
        int selected = 0;
        for (final JCheckBox button : checkBoxToHost.keySet()) {
            if (button.isSelected()) {
                selected++;
            }
        }
        if (selected >= 2) {
            buttonClass(nextButton()).setEnabled(true);
        } else {
            buttonClass(nextButton()).setEnabled(false);
        }
    }

    /**
     * Returns the title of the dialog.
     */
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.ClusterHosts.Title");
    }

    /**
     * Returns the description of the dialog.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.ClusterHosts.Description");
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        final JComponent[] cl = {buttonClass(nextButton())};
        enableComponentsLater(cl);

        final Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    enableComponents();
                    checkCheckBoxes();
                }
            });
        thread.start();
    }

    /**
     * Returns the panel with hosts that can be selected.
     */
    protected final JComponent getInputPane() {
        /* Hosts */
        final JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEADING, 1, 1));
        final Hosts hosts = Tools.getConfigData().getHosts();

        final ItemListener chListener = new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    checkCheckBoxes();
                }
            };
        for (final Host host : hosts.getHostsArray()) {
            final JCheckBox button = new JCheckBox(host.getName(),
                                                   HOST_UNCHECKED_ICON);
            button.setSelectedIcon(HOST_CHECKED_ICON);
            if (host.isInCluster(getCluster())) {
                button.setEnabled(false);
            }
            checkBoxToHost.put(button, host);
            if (getCluster().getHosts().contains(host)) {
                button.setSelected(true);
            } else {
                // !!!!!!FOR TESTING
                if (hosts.size() == 2) {
                    button.setSelected(true);
                } else {
                    button.setSelected(false);
                }
            }
            button.addItemListener(chListener);
            p1.add(button);
        }
        return p1;
    }
}
