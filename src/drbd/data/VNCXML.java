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

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.Map;
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
public class VNCXML extends XML {
    /** Domain name. */
    private String name = null;
    /** Remote port. */
    private int remotePort = -1;
    /** Autoport. */
    private boolean autoport = false;
    /** Pattern that maches display e.g. :4. */
    private static final Pattern DISPLAY_PATTERN =
                                                 Pattern.compile(".*:(\\d+)$");
    /**
     * Prepares a new <code>VNCXML</code> object.
     */
    public VNCXML(final Host host, final String configFile) {
        super();
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@CONFIG@", configFile);
        final String command = host.getDistCommand("VNCXML.GetConfig",
                                                   replaceHash);
        final String output = Tools.execCommandProgressIndicator(
                                       host,
                                       command,
                                       null,  /* ExecCallback */
                                       false, /* outputVisible */
                                       Tools.getString("VNCXML.GetConfig"));
        if (output == null) {
            return;
        }
        parseConfig(output);
        if (name != null) {
            final Map<String, String> vncreplaceHash =
                                                 new HashMap<String, String>();
            vncreplaceHash.put("@NAME@", name);
            final String vnccommand =
                   host.getDistCommand("VNCXML.GetVncDisplay", vncreplaceHash);
            final String display = Tools.execCommandProgressIndicator(
                                           host,
                                           vnccommand,
                                           null,  /* ExecCallback */
                                           false, /* outputVisible */
                                           Tools.getString("VNCXML.GetConfig"));
            if (display != null) {
                final Matcher m = DISPLAY_PATTERN.matcher(display.trim());
                if (m.matches()) {
                    remotePort = Integer.parseInt(m.group(1)) + 5900;
                }
            }
        }
    }

    /**
     * Parses the libvirt config file.
     */
    private void parseConfig(final String xml) {
        final Document document = getXMLDocument(xml);
        if (document == null) {
            return;
        }
        final Node domainNode = getChildNode(document, "domain");
        if (domainNode == null) {
            return;
        }
        final NodeList options = domainNode.getChildNodes();
        boolean tabletOk = false;
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if ("name".equals(option.getNodeName())) {
                name = getText(option);
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
                        Tools.debug(this, "autoport: " + autoport, 2);
                        if ("vnc".equals(type)) {
                            if (port != null && Tools.isNumber(port)) {
                                remotePort = Integer.parseInt(port);
                            }
                            if ("yes".equals(ap)) {
                                autoport = true;
                            }
                        }
                    }
                }
            }
        }
        if (!tabletOk) {
            Tools.appWarning("you should enable input type tablet");
        }
    }

    /**
     * Returns domain name.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns remote port.
     */
    public final int getRemotePort() {
        return remotePort;
    }
}
