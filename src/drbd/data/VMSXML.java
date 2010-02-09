/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, Rastislav Levrinc
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


package drbd.data;

import drbd.utilities.Tools;
import drbd.utilities.ConvertCmdCallback;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class parses xml from drbdsetup and drbdadm, stores the
 * information in the hashes and provides methods to get this
 * information.
 * The xml is obtained with drbdsetp xml command and drbdadm dump-xml.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class VMSXML extends XML {
    /** List of domain names. */
    private final List<String> domainNames = new ArrayList<String>();
    /** Map from configs to names. */
    private final Map<String, String> configsMap =
                                            new HashMap<String, String>();
    /** Hash of domains and their remote ports. */
    private final Map<String, Integer> remotePorts =
                                                new HashMap<String, Integer>();
    /** Hash of domains that use autoport. */
    private final Map<String, Boolean> autoports =
                                                new HashMap<String, Boolean>();
    /** Whether the domain is running. */
    private final Map<String, Boolean> runningMap =
                                                new HashMap<String, Boolean>();
    /** Pattern that maches display e.g. :4. */
    private static final Pattern DISPLAY_PATTERN =
                                                 Pattern.compile(".*:(\\d+)$");
    /** Host on which the vm is defined. */
    private final Host host;
    /**
     * Prepares a new <code>VMSXML</code> object.
     */
    public VMSXML(final Host host) {
        super();
        this.host = host;
    }

    /**
     * Updates data.
     */
    public void update() {
        final String command = host.getDistCommand("VMSXML.GetData",
                                                   (ConvertCmdCallback) null);
        final String output = Tools.execCommand(host,
                                                command,
                                                null,  /* ExecCallback */
                                                false); /* outputVisible */
        if (output == null) {
            return;
        }
        final Document document = getXMLDocument(output);
        if (document == null) {
            return;
        }
        final Node vmsNode = getChildNode(document, "vms");
        final NodeList vms = vmsNode.getChildNodes();
        for (int i = 0; i < vms.getLength(); i++) {
            final Node vmNode = vms.item(i);
            if ("vm".equals(vmNode.getNodeName())) {
                updateVM(vmNode);
            }
        }
    }

    /**
     * Updates one vm.
     */
    private void updateVM(final Node vmNode) {
        /* one vm */
        if (vmNode == null) {
            return;
        }
        final Node infoNode = getChildNode(vmNode, "info");
        final String name = getAttribute(vmNode, "name");
        final String config = getAttribute(vmNode, "config");
        configsMap.put(config, name);
        if (infoNode != null) {
            parseInfo(name, getText(infoNode));
        }
        final Node vncdisplayNode = getChildNode(vmNode, "vncdisplay");
        remotePorts.put(name, -1);
        if (vncdisplayNode != null) {
            final String vncdisplay = getText(vncdisplayNode).trim();
            final Matcher m = DISPLAY_PATTERN.matcher(vncdisplay);
            if (m.matches()) {
                remotePorts.put(name, Integer.parseInt(m.group(1)) + 5900);
            }
        }
        parseConfig(getChildNode(vmNode, "config"), name);
    }

    /**
     * Parses the libvirt config file.
     */
    private void parseConfig(final Node configNode,
                             final String nameInFilename) {
        if (configNode == null) {
            return;
        }
        final Node domainNode = getChildNode(configNode, "domain");
        if (domainNode == null) {
            return;
        }
        final NodeList options = domainNode.getChildNodes();
        boolean tabletOk = false;
        String name = null;
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if ("name".equals(option.getNodeName())) {
                name = getText(option);
                if (!domainNames.contains(name)) {
                    domainNames.add(name);
                }
                if (!name.equals(nameInFilename)) {
                    Tools.appWarning("unexpected name: " + name 
                                     + " != " + nameInFilename);
                    return;
                }
            } else if ("devices".equals(option.getNodeName())) {
                final NodeList devices = option.getChildNodes();
                for (int j = 0; j < devices.getLength(); j++) {
                    final Node device = devices.item(j);
                    if ("input".equals(device.getNodeName())) {
                        final String type = getAttribute(device, "type");
                        if ("tablet".equals(type)) {
                            tabletOk = true;
                        }
                    } else if ("graphics".equals(device.getNodeName())) {
                        /** remotePort will be overwritten with virsh output */
                        final String type = getAttribute(device, "type");
                        final String port = getAttribute(device, "port");
                        final String ap = getAttribute(device, "autoport");
                        Tools.debug(this, "type: " + type, 2);
                        Tools.debug(this, "port: " + port, 2);
                        Tools.debug(this, "autoport: " + ap, 2);
                        if ("vnc".equals(type)) {
                            if (port != null && Tools.isNumber(port)) {
                                remotePorts.put(name, Integer.parseInt(port));
                            }
                            if ("yes".equals(ap)) {
                                autoports.put(name, true);
                            } else {
                                autoports.put(name, false);
                            }
                        }
                    }
                }
            }
        }
        if (!tabletOk) {
            Tools.appWarning("you should enable input type tablet for " + name);
        }
    }

    /**
     * Updates all data for this domain.
     */
    public final void parseInfo(final String name, final String info) {
        if (info != null) {
            boolean running = false;
            for (final String line : info.split("\n")) {
                final String[] optionValue = line.split(":");
                if (optionValue.length == 2) {
                    final String option = optionValue[0].trim();
                    final String value = optionValue[1].trim();
                    if ("State".equals(option)
                        && "running".equals(value)) {
                        running = true;
                    }
                }
            }
            runningMap.put(name, running);
        }
    }

    /**
     * Returns all domain names.
     */
    public final List<String> getDomainNames() {
        return domainNames;
    }

    /**
     * Returns whether the domain is running.
     */
    public final boolean isRunning(final String name) {
        final Boolean r = runningMap.get(name);
        if (r != null) {
            return r;
        }
        return false;
    }

    /**
     * Returns remote port.
     */
    public final int getRemotePort(final String name) {
        return remotePorts.get(name);
    }

    /**
     * Returns host.
     */
    public final Host getHost() {
        return host;
    }

    /**
     * Returns configs of all vms.
     */
    public final Set<String> getConfigs() {
        return configsMap.keySet();
    }

    /**
     * Returns domain name from config file.
     */
    public final String getNameFromConfig(final String config) {
        return configsMap.get(config);
    }
}
