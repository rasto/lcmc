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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;

/**
 * An implementation of a dialog where entered ip or the host is looked up
 * with dns.
 */
@Named
public class Configuration extends DialogHost {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    private static final int MAX_HOPS = Tools.getDefaultInt("MaxHops");
    private static final int COMBO_BOX_WIDTH = 180;
    private static final int DNS_TIMEOUT = 5000;
    private final Widget[] hostnameField = new Widget[MAX_HOPS];
    private final Widget[] ipCombo = new Widget[MAX_HOPS];
    private String[] hostnames = new String[MAX_HOPS];
    private volatile boolean hostnameOk = false;
    @Inject
    private Devices devices;
    @Resource(name="SSH")
    private SSH sshDialog;
    @Inject
    private Application application;
    @Inject
    private WidgetFactory widgetFactory;

    /** Finishes the dialog and stores the values. */
    @Override
    protected void finishDialog() {
        getHost().setHostname(Tools.join(",", hostnames, getHops()));
        final int hops = getHops();
        final String[] ipsA = new String[hops];
        for (int i = 0; i < hops; i++) {
            ipsA[i] = ipCombo[i].getStringValue();
        }
        getHost().setIpAddress(Tools.join(",", ipsA));
    }

    /**
     * Returns the next dialog. Depending on if the host is already connected
     * it is the SSH or it is skipped and Devices is the next dialog.
     */
    @Override
    public WizardDialog nextDialog() {
        if (hostnameOk) {
            if (getHost().isConnected()) {
                devices.init(this, getHost(), getDrbdInstallation());
                return devices;
            } else {
                sshDialog.init(this, getHost(), getDrbdInstallation());
                return sshDialog;
            }
        } else {
            return this;
        }
    }

    /**
     * Checks the fields and if they are correct the buttons will be enabled.
     */
    @Override
    protected final void checkFields(final Widget field) {
        final List<String> incorrect = new ArrayList<String>();
        final List<String> changed = new ArrayList<String>();
        if (!hostnameOk) {
            incorrect.add(Tools.getString("Dialog.Host.Configuration.DNSLookupError"));
        }
        enableNextButtons(incorrect, changed);
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.Configuration.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.Configuration.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.Configuration.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.Configuration.Description");
    }

    /**
     * Checks the dns entries for all the hosts.
     * This assumes that getHost().hostnameEntered was set.
     */
    protected final boolean checkDNS(final int hop, final String hostnameEntered) {
        final InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(hostnameEntered);
        } catch (final UnknownHostException e) {
            return false;
        }
        LOG.debug2("checkDNS: addresses.length: " + addresses.length + " a: " + addresses[0].getHostAddress());
        final String hostname;
        String ip;
        if (addresses.length == 0) {
            LOG.debug("checkDNS: lookup failed");
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
            } catch (final UnknownHostException e) {
                LOG.appError("checkDNS: unknown host", "", e);
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
        final String[] items = new String[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            items[i] = addresses[i].getHostAddress();
        }
        //getHost().setIpAddress(ip);
        getHost().setIps(hop, items);
        hostnames[hop] = hostname;

        hostnameField[hop].setValue(new StringValue(hostname));
        LOG.debug1("checkDNS: got " + hostname + " (" + ip + ')');
        return true;
    }

    /** Returns number of hops. */
    protected final int getHops() {
        final String hostnameEntered = getHost().getEnteredHostOrIp();
        return Tools.charCount(hostnameEntered, ',') + 1;
    }

    /** Inits dialog and starts dns check for every host. */
    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected final void initDialogAfterVisible() {
        if (getHost().getIpAddress() == null || getHost().getIpAddress().isEmpty()) {
            getProgressBar().start(DNS_TIMEOUT);
            final CheckDNSThread[] checkDNSThread = new CheckDNSThread[MAX_HOPS];
            for (int i = 0; i < getHops(); i++) {
                final String hostnameEntered = getHost().getEnteredHostOrIp().split(",")[i];

                hostnameField[i].setEnabled(false);
                if (Tools.isIp(hostnameEntered)) {
                    hostnames[i] = hostnameEntered;
                    getHost().setIpAddress(hostnameEntered);
                    getHost().setIps(i, new String[]{hostnameEntered});
                    final Value hostnameEnteredValue = new StringValue(hostnameEntered);
                    hostnameField[i].setValue(hostnameEnteredValue);
                    ipCombo[i].reloadComboBox(hostnameEnteredValue, new Value[]{hostnameEnteredValue});
                    hostnameOk = true;
                } else {
                    checkDNSThread[i] = new CheckDNSThread(i, hostnameEntered);
                    checkDNSThread[i].start();
                }
            }


            final Thread thread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < getHops(); i++) {
                            if (checkDNSThread[i] != null) {
                                try {
                                    checkDNSThread[i].join();
                                } catch (final InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                        progressBarDone();
                        getHost().setHostname(Tools.join(",", hostnames, getHops()));
                        enableComponents();
                        makeDefaultAndRequestFocus(buttonClass(nextButton()));
                        checkFields(null);
                        if (!application.getAutoHosts().isEmpty()) {
                            Tools.sleep(1000);
                            pressNextButton();
                        }
                    }
                });
            thread.start();
        } else {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    getHost().setHostname(Tools.join(",", hostnames, getHops()));
                    application.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < getHops(); i++) {
                                hostnameField[i].setEnabled(false);
                            }
                        }
                    });
                    enableComponents();
                    makeDefaultAndRequestFocus(buttonClass(nextButton()));
                    checkFields(null);
                    hostnameOk = true;
                    if (!application.getAutoHosts().isEmpty()) {
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
    @Override
    protected final JComponent getInputPane() {
        final int hops = getHops();
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        /* Host/Hosts */
        final JLabel hostnameLabel = new JLabel(Tools.getString("Dialog.Host.Configuration.Hostname"));
        inputPane.add(hostnameLabel);
        final String hostname = getHost().getHostname();
        if (hostname == null || Tools.charCount(hostname, ',') == 0) {
            hostnames[0] = hostname;
        } else {
            hostnames = hostname.split(",");
        }
        for (int i = 0; i < hops; i++) {
            hostnameField[i] = widgetFactory.createInstance(
                                      Widget.GUESS_TYPE,
                                      new StringValue(hostnames[i]),
                                      Widget.NO_ITEMS,
                                      Widget.NO_REGEXP,
                                      COMBO_BOX_WIDTH,
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
            inputPane.add(hostnameField[i].getComponent());
        }

        final JLabel ipLabel = new JLabel(Tools.getString("Dialog.Host.Configuration.Ip"));
        inputPane.add(ipLabel);

        for (int i = 0; i < hops; i++) {
            if (getHost().getIp(i) == null) {
                getHost().setIps(i, null);
            }
            ipCombo[i] = widgetFactory.createInstance(
                                Widget.Type.COMBOBOX,
                                new StringValue(getHost().getIp(i)),
                                StringValue.getValues(getHost().getIps(i)),
                                Widget.NO_REGEXP,
                                COMBO_BOX_WIDTH,
                                Widget.NO_ABBRV,
                                new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                                Widget.NO_BUTTON);

            inputPane.add(ipCombo[i].getComponent());
            ipCombo[i].setEnabled(false);
        }

        SpringUtilities.makeCompactGrid(inputPane, 2, 1 + hops, 0, 0, 0, 0);
        final JPanel pane = new JPanel(new SpringLayout());
        pane.setBorder(null);
        pane.add(inputPane);
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 3, 1, 0, 0, 0, 0);
        return pane;
    }

    /**
     * Class that implements check dns thread.
     */
    private class CheckDNSThread extends Thread {
        private final int numberOfHops;
        private final String hostnameEntered;

        /**
         * Prepares a new {@code CheckDNSThread} object, with number of
         * hops and host names delimited with commas.
         */
        CheckDNSThread(final int numberOfHops, final String hostnameEntered) {
            super();
            this.numberOfHops = numberOfHops;
            this.hostnameEntered = hostnameEntered;
        }

        /** Runs the check dns thread. */
        @Override
        public void run() {
            answerPaneSetText(Tools.getString("Dialog.Host.Configuration.DNSLookup"));
            hostnameOk = checkDNS(numberOfHops, hostnameEntered);
            if (hostnameOk) {
                answerPaneSetText(Tools.getString("Dialog.Host.Configuration.DNSLookupOk"));
            } else {
                printErrorAndRetry(Tools.getString("Dialog.Host.Configuration.DNSLookupError"));
            }
            final Value[] items = StringValue.getValues(getHost().getIps(numberOfHops));
            if (items != null) {
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        final String savedIp = getHost().getIp(numberOfHops);
                        final Value defaultIp;
                        if (savedIp == null && items.length > 0) {
                            defaultIp = items[0];
                        } else {
                            defaultIp = new StringValue(savedIp);
                        }
                        ipCombo[numberOfHops].reloadComboBox(defaultIp, items);

                        if (items.length > 1) {
                            ipCombo[numberOfHops].setEnabled(true);
                        }
                    }
                });
            }
        }
    }
}
