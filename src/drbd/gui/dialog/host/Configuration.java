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


package drbd.gui.dialog.host;

import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.Tools;
import drbd.gui.SpringUtilities;
import drbd.gui.GuiComboBox;
import drbd.gui.dialog.WizardDialog;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;

import java.net.UnknownHostException;
import java.net.InetAddress;

/**
 * An implementation of a dialog where entered ip or the host is looked up
 * with dns.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Configuration extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Maximum hops. */
    private static final int MAX_HOPS = Tools.getDefaultInt("MaxHops");
    /** Hostname fields. */
    private GuiComboBox[] hostnameField =
                            new GuiComboBox[MAX_HOPS];
    /** Ip fields. */
    private GuiComboBox[] ipCombo =
                            new GuiComboBox[MAX_HOPS];
    /** Hostnames. */
    private String[] hostnames = new String[MAX_HOPS];
    /** Whether the hostname was ok. */
    private volatile boolean hostnameOk = false;
    /** Width of the combo boxes. */
    private static final int COMBO_BOX_WIDTH = 120;
    /** DNS timeout. */
    private static final int DNS_TIMEOUT = 5000;

    /**
     * Prepares a new <code>Configuration</code> object.
     */
    public Configuration(final WizardDialog previousDialog,
                         final Host host) {
        super(previousDialog, host);
    }

    /**
     * Finishes the dialog and stores the values.
     */
    protected final void finishDialog() {
        getHost().setHostname(Tools.join(",", hostnames, getHops()));
        final int hops = getHops();
        String[] ipsA = new String[hops];
        for (int i = 0; i < hops; i++) {
            ipsA[i] = ipCombo[i].getStringValue();
        }
        getHost().setIp(Tools.join(",", ipsA));
    }

    /**
     * Returns the next dialog. Depending on if the host is already connected
     * it is the SSH or it is skipped and Devices is the next dialog.
     */
    public final WizardDialog nextDialog() {
        if (hostnameOk) {
            if (getHost().isConnected()) {
                return new Devices(this, getHost());
            } else {
                return new SSH(this, getHost());
            }
        } else {
            return this;
        }
    }

    /**
     * Checks the fields and if they are correct the buttons will be enabled.
     */
    protected final void checkFields(final GuiComboBox field) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                buttonClass(nextButton()).setEnabled(hostnameOk);
            }
        });
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.Configuration.Title in TextResources.
     */
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.Configuration.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.Configuration.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.Configuration.Description");
    }

    /**
     * Checks the dns entries for all the hosts.
     * This assumes that getHost().hostnameEntered was set.
     */
    protected final boolean checkDNS(final int hop,
                                     final String hostnameEntered) {
        InetAddress[] addresses = null;
        try {
            addresses = InetAddress.getAllByName(hostnameEntered);
        } catch (UnknownHostException e) {
            return false;
        }
        String hostname = null;
        String ip = null;
        Tools.debug(this, "addresses.length: " + addresses.length + "a: "
                          + addresses[0].getHostAddress());
        if (addresses.length == 0) {
            Tools.debug(this, "lookup failed");
            // lookup failed;
            return false;
        } else if (addresses.length == 1) {
            ip = addresses[0].getHostAddress();
            /* if user entered ip, reverse lookup is needed.
             * Making reverse lookup even if user entered a some of the
             * hostnames since it can be different than canonical name.
             */
            try {
                hostname = InetAddress.getByName(ip).getHostName();
            } catch (UnknownHostException e) {
                Tools.appError("Host.Configuration.Unknown.Host", "", e);
                return false;
            }
        } else {
            // user entered hostname that has many addresses
            hostname = hostnameEntered;
            ip = getHost().getIp(hop);
            if (ip == null) {
                ip = addresses[0].getHostAddress();
            }
        }
        String[] items = new String[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            items[i] = addresses[i].getHostAddress();
        }
        //getHost().setIp(ip);
        getHost().setIps(hop, items);
        hostnames[hop] = hostname;

        hostnameField[hop].setValue(hostname);
        Tools.debug(this, "got " + hostname + " (" + ip + ")", 1);
        return true;
    }

    /**
     * Class that implements check dns thread.
     */
    class CheckDNSThread extends Thread {
        /** Number of hops. */
        private final int hop;
        /** Host names as entered by user. */
        private final String hostnameEntered;

        /**
         * Prepares a new <code>CheckDNSThread</code> object, with number of
         * hops and host names delimited with commas.
         */
        public CheckDNSThread(final int hop, final String hostnameEntered) {
            super();
            this.hop = hop;
            this.hostnameEntered = hostnameEntered;
        }

        /**
         * Runs the check dns thread.
         */
        public final void run() {
            answerPaneSetText(
                        Tools.getString("Dialog.Host.Configuration.DNSLookup"));
            hostnameOk = checkDNS(hop, hostnameEntered);
            if (hostnameOk) {
                answerPaneSetText(
                    Tools.getString("Dialog.Host.Configuration.DNSLookupOk"));
            } else {
                printErrorAndRetry(
                   Tools.getString("Dialog.Host.Configuration.DNSLookupError"));
            }
            final String[] items = getHost().getIps(hop);
            if (items != null) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ipCombo[hop].reloadComboBox(getHost().getIp(hop),
                                                    items);

                        if (items.length > 1) {
                            ipCombo[hop].setEnabled(true);
                        }
                    }
                });
            }
        }
    }

    /**
     * Returns number of hops.
     */
    protected final int getHops() {
        final String hostnameEntered = getHost().getHostnameEntered();
        return Tools.charCount(hostnameEntered, ',') + 1;
    }

    /**
     * Inits dialog and starts dns check for every host.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        if (getHost().getIp() == null || "".equals(getHost().getIp())) {
            final CheckDNSThread[] checkDNSThread =
                            new CheckDNSThread[MAX_HOPS];
            getProgressBar().start(DNS_TIMEOUT);
            for (int i = 0; i < getHops(); i++) {
                final String hostnameEntered =
                                getHost().getHostnameEntered().split(",")[i];

                hostnameField[i].setEnabled(false);
                if (Tools.isIp(hostnameEntered)) {
                    hostnames[i] = hostnameEntered;
                    hostnameField[i].setValue(hostnameEntered);
                    getHost().setIp(hostnameEntered);
                    getHost().setIps(i, new String[]{hostnameEntered});
                    ipCombo[i].reloadComboBox(hostnameEntered,
                                              new String[]{hostnameEntered});
                    hostnameOk = true;
                } else {
                    checkDNSThread[i] = new CheckDNSThread(i, hostnameEntered);
                    checkDNSThread[i].setPriority(Thread.MIN_PRIORITY);
                    checkDNSThread[i].start();
                }
            }


            final Thread thread = new Thread(
                new Runnable() {
                    public void run() {
                        for (int i = 0; i < getHops(); i++) {
                            if (checkDNSThread[i] != null) {
                                try {
                                    checkDNSThread[i].join();
                                } catch (java.lang.InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                        progressBarDone();
                        getHost().setHostname(
                                Tools.join(",", hostnames, getHops()));
                        String name = getHost().getName();
                        if (name == null || "null".equals(name)) {
                            name = "";
                        }
                        enableComponents();
                        if (!Tools.getConfigData().getAutoHosts().isEmpty()) {
                            Tools.sleep(1000);
                            pressNextButton();
                        }
                    }
                });
            thread.start();
        } else {
            final Thread thread = new Thread(new Runnable() {
                public void run() {
                    getHost().setHostname(
                                       Tools.join(",", hostnames, getHops()));
                    String name = getHost().getName();
                    if (name == null || "null".equals(name)) {
                        name = "";
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            for (int i = 0; i < getHops(); i++) {
                                hostnameField[i].setEnabled(false);
                            }
                        }
                    });
                    enableComponents();
                    hostnameOk = true;
                    if (!Tools.getConfigData().getAutoHosts().isEmpty()) {
                        Tools.sleep(1000);
                        pressNextButton();
                    }
                }
            });
            thread.start();
        }
    }

    /**
     * Returns input pane where names of host or more hosts delimited with
     * comma, can be entered.
     */
    protected final JComponent getInputPane() {
        final int hops = getHops();
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Host/Hosts */
        final JLabel hostnameLabel = new JLabel(
                        Tools.getString("Dialog.Host.Configuration.Hostname"));
        inputPane.add(hostnameLabel);
        final String hostname = getHost().getHostname();
        if (hostname == null || Tools.charCount(hostname, ',') == 0) {
            hostnames[0] = hostname;
        } else {
            hostnames = hostname.split(",");
        }
        for (int i = 0; i < hops; i++) {
            hostnameField[i] = new GuiComboBox(hostnames[i],
                                               null, /* items */
                                               null, /* units */
                                               null, /* type*/
                                               null, /* regexp*/
                                               COMBO_BOX_WIDTH,
                                               null, /* abbrv */
                                               new AccessMode(
                                                    ConfigData.AccessType.RO,
                                                    false)); /* only adv mode */

            inputPane.add(hostnameField[i]);
        }

        final JLabel ipLabel = new JLabel(
                            Tools.getString("Dialog.Host.Configuration.Ip"));
        inputPane.add(ipLabel);

        for (int i = 0; i < hops; i++) {
            if (getHost().getIp(i) == null) {
                getHost().setIps(i, null);
            }
            ipCombo[i] = new GuiComboBox(getHost().getIp(i),
                                         getHost().getIps(i),
                                         null, /* units */
                                         GuiComboBox.Type.COMBOBOX,
                                         null, /* regexp */
                                         COMBO_BOX_WIDTH,
                                         null, /* abbrv */
                                         new AccessMode(
                                                     ConfigData.AccessType.RO,
                                                     false)); /* only adv. */

            inputPane.add(ipCombo[i]);
            ipCombo[i].setEnabled(false);
        }

        SpringUtilities.makeCompactGrid(inputPane, 2, 1 + hops, // rows, cols
                                                   0, 0,  // initX, initY
                                                   0, 0); // xPad, yPad
        final JPanel pane = new JPanel(new SpringLayout());
        pane.setBorder(null);
        pane.add(inputPane);
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  //rows, cols
                                              0, 0,  //initX, initY
                                              0, 0); //xPad, yPad
        return pane;
    }
}
