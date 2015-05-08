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

package lcmc.vm.domain;

import com.google.common.collect.Maps;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.XMLTools;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Named
public class NetworkParser {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkParser.class);

    public static final String NET_PARAM_NAME = "name";
    public static final String NET_PARAM_UUID = "uuid";
    public static final String NET_PARAM_AUTOSTART = "autostart";

    private final Map<String, String> netToConfigs = new HashMap<String, String>();
    private final Map<String, String> netNamesConfigsMap = new HashMap<String, String>();
    private Map<Value, NetworkData> networkMap = new LinkedHashMap<Value, NetworkData>();

    public void parseNetwork(final Node netNode) {
        /* one vm */
        if (netNode == null) {
            Maps.newHashMap();
        }
        final String name = XMLTools.getAttribute(netNode, VmsXml.VM_PARAM_NAME);
        final String config = XMLTools.getAttribute(netNode, "config");
        netToConfigs.put(config, name);
        netNamesConfigsMap.put(name, config);
        final String autostartString = XMLTools.getAttribute(netNode, NET_PARAM_AUTOSTART);
        parseNetConfig(XMLTools.getChildNode(netNode, "network"), name, autostartString);
    }

    /** Parses the libvirt network config file. */
    private void parseNetConfig(final Node networkNode, final String nameInFilename, final String autostartString) {
        final Map<Value, NetworkData> newNetworkMap = Maps.newHashMap();
        if (networkNode == null) {
            return;
        }
        boolean autostart = false;
        if (autostartString != null && "true".equals(autostartString)) {
            autostart = true;
        }
        final NodeList options = networkNode.getChildNodes();
        String name = null;
        String uuid = null;
        String forwardMode = null;
        String bridgeName = null;
        String bridgeSTP = null;
        String bridgeDelay = null;
        String bridgeForwardDelay = null;
        for (int i = 0; i < options.getLength(); i++) {
            final Node optionNode = options.item(i);
            final String nodeName = optionNode.getNodeName();
            if (NET_PARAM_NAME.equals(nodeName)) {
                name = XMLTools.getText(optionNode);
                if (!name.equals(nameInFilename)) {
                    LOG.appWarning("parseNetConfig: unexpected name: " + name + " != " + nameInFilename);
                    return;
                }
            } else if (NET_PARAM_UUID.equals(nodeName)) {
                uuid = XMLTools.getText(optionNode);
            } else if ("forward".equals(nodeName)) {
                forwardMode = XMLTools.getAttribute(optionNode, "mode");
            } else if ("bridge".equals(nodeName)) {
                bridgeName = XMLTools.getAttribute(optionNode, "name");
                bridgeSTP = XMLTools.getAttribute(optionNode, "stp");
                bridgeDelay = XMLTools.getAttribute(optionNode, "delay");
                bridgeForwardDelay = XMLTools.getAttribute(optionNode, "forwardDelay");
            } else if ("mac".equals(nodeName)) {
                /* skip */
            } else if ("ip".equals(nodeName)) {
                /* skip */
            } else if (!"#text".equals(nodeName)) {
                LOG.appWarning("parseNetConfig: unknown network option: " + nodeName);
            }
        }
        if (name != null) {
            final NetworkData networkData = new NetworkData(name,
                                                            uuid,
                                                            autostart,
                                                            forwardMode,
                                                            bridgeName,
                                                            bridgeSTP,
                                                            bridgeDelay,
                                                            bridgeForwardDelay);

            newNetworkMap.put(new StringValue(name), networkData);
        }
        this.networkMap = newNetworkMap;
    }

    public List<Value> getNetworks() {
        return new ArrayList<Value>(networkMap.keySet());
    }
}
