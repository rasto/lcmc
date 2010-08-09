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
import drbd.utilities.SSH;
import drbd.utilities.VIRSH;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.commons.collections.map.MultiKeyMap;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPath;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerFactory;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

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
    /** Map from names to configs. */
    private final Map<String, String> namesConfigsMap =
                                            new HashMap<String, String>();
    /** Map from network configs to names. */
    private final Map<String, String> netConfigsMap =
                                            new HashMap<String, String>();
    /** Map from network names to configs. */
    private final Map<String, String> netNamesConfigsMap =
                                            new HashMap<String, String>();
    /** Map from names to vcpus. */
    private final MultiKeyMap parameterValues = new MultiKeyMap();
    /** Hash of domains and their remote ports. */
    private final Map<String, Integer> remotePorts =
                                                new HashMap<String, Integer>();
    /** Hash of domains that use autoport. */
    private final Map<String, Boolean> autoports =
                                                new HashMap<String, Boolean>();
    /** Whether the domain is running. */
    private final Map<String, Boolean> runningMap =
                                                new HashMap<String, Boolean>();
    /** Whether the domain is suspended. */
    private final Map<String, Boolean> suspendedMap =
                                                new HashMap<String, Boolean>();
    /** Map from domain name and target device to the disk data. */
    private final Map<String, Map<String, DiskData>> disksMap =
                           new LinkedHashMap<String, Map<String, DiskData>>();
    /** Map from domain name and mac address to the interface data. */
    private final Map<String, Map<String, InterfaceData>> interfacesMap =
                       new LinkedHashMap<String, Map<String, InterfaceData>>();
    /** Map from domain name and network name to the network data. */
    private final Map<String, NetworkData> networkMap =
                                    new LinkedHashMap<String, NetworkData>();
    /** Pattern that maches display e.g. :4. */
    private static final Pattern DISPLAY_PATTERN =
                                                 Pattern.compile(".*:(\\d+)$");
    /** Host on which the vm is defined. */
    private final Host host;
    /** VM field: name. */
    public static final String VM_PARAM_NAME = "name";
    /** VM field: defined. */
    public static final String VM_PARAM_DEFINED = "defined";
    /** VM field: status. */
    public static final String VM_PARAM_STATUS = "status";
    /** VM field: vcpu. */
    public static final String VM_PARAM_VCPU = "vcpu";
    /** VM field: currentMemory. */
    public static final String VM_PARAM_CURRENTMEMORY = "currentMemory";
    /** VM field: memory. */
    public static final String VM_PARAM_MEMORY = "memory";
    /** VM field: os-boot. */
    public static final String VM_PARAM_BOOT = "boot";
    /** VM field: autostart. */
    public static final String VM_PARAM_AUTOSTART = "autostart";
    /** Network field: name. */
    public static final String NET_PARAM_NAME = "name";
    /** Network field: uuid. */
    public static final String NET_PARAM_UUID = "uuid";
    /** Network field: autostart. */
    public static final String NET_PARAM_AUTOSTART = "autostart";
    /** Map from paramater to its xml tag. */
    public static final Map<String, String> INTERFACE_TAG_MAP =
                                             new HashMap<String, String>();
    /** Map from paramater to its xml attribute. */
    public static final Map<String, String> INTERFACE_ATTRIBUTE_MAP =
                                             new HashMap<String, String>();
    /** Map from paramater to its xml tag. */
    public static final Map<String, String> DISK_TAG_MAP =
                                             new HashMap<String, String>();
    /** Map from paramater to its xml attribute. */
    public static final Map<String, String> DISK_ATTRIBUTE_MAP =
                                             new HashMap<String, String>();

    static {
        INTERFACE_ATTRIBUTE_MAP.put(InterfaceData.TYPE, "type");

        INTERFACE_TAG_MAP.put(InterfaceData.MAC_ADDRESS, "mac");
        INTERFACE_ATTRIBUTE_MAP.put(InterfaceData.MAC_ADDRESS, "address");
        INTERFACE_TAG_MAP.put(InterfaceData.SOURCE_NETWORK, "source");
        INTERFACE_ATTRIBUTE_MAP.put(InterfaceData.SOURCE_NETWORK, "network");
        INTERFACE_TAG_MAP.put(InterfaceData.SOURCE_BRIDGE, "source");
        INTERFACE_ATTRIBUTE_MAP.put(InterfaceData.SOURCE_BRIDGE, "bridge");
        INTERFACE_TAG_MAP.put(InterfaceData.TARGET_DEV, "target");
        INTERFACE_ATTRIBUTE_MAP.put(InterfaceData.TARGET_DEV, "dev");
        INTERFACE_TAG_MAP.put(InterfaceData.MODEL_TYPE, "model");
        INTERFACE_ATTRIBUTE_MAP.put(InterfaceData.MODEL_TYPE, "type");

        DISK_ATTRIBUTE_MAP.put(InterfaceData.TYPE, "type");

        DISK_TAG_MAP.put(DiskData.TARGET_DEVICE, "target");
        DISK_ATTRIBUTE_MAP.put(DiskData.TARGET_DEVICE, "dev");
        DISK_TAG_MAP.put(DiskData.SOURCE_FILE, "source");
        DISK_ATTRIBUTE_MAP.put(DiskData.SOURCE_FILE, "file");
        DISK_TAG_MAP.put(DiskData.SOURCE_DEVICE, "source");
        DISK_ATTRIBUTE_MAP.put(DiskData.SOURCE_DEVICE, "dev");
        DISK_TAG_MAP.put(DiskData.TARGET_BUS, "target");
        DISK_ATTRIBUTE_MAP.put(DiskData.TARGET_BUS, "bus");
        DISK_TAG_MAP.put(DiskData.DRIVER_NAME, "driver");
        DISK_ATTRIBUTE_MAP.put(DiskData.DRIVER_NAME, "name");
        DISK_TAG_MAP.put(DiskData.DRIVER_TYPE, "driver");
        DISK_ATTRIBUTE_MAP.put(DiskData.DRIVER_TYPE, "type");

        DISK_ATTRIBUTE_MAP.put(DiskData.TARGET_TYPE, "device");

        DISK_TAG_MAP.put(DiskData.READONLY, "readonly");

        DISK_TAG_MAP.put(DiskData.SHAREABLE, "shareable");
    }

    /** XML document lock. */
    private final Mutex mXMLDocumentLock = new Mutex();
    /** XML document. */
    private Document xmlDocument = null;

    /**
     * Prepares a new <code>VMSXML</code> object.
     */
    public VMSXML(final Host host) {
        super();
        this.host = host;
    }

    /** Returns xml node of the specified domain. */
    private Node getDomainNode(final String domainName) {
        try {
            mXMLDocumentLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final Document document = xmlDocument;
        mXMLDocumentLock.release();
        final XPath xpath = XPathFactory.newInstance().newXPath();
        Node domainNode = null;
        try {
            final String path = "//vms/vm[@name='"
                                + domainName
                                + "']/config/domain";
            final NodeList domainNodes = (NodeList) xpath.evaluate(
                                                       path,
                                                       document,
                                                       XPathConstants.NODESET);
            if (domainNodes.getLength() == 1) {
                domainNode = domainNodes.item(0);
            } else if (domainNodes.getLength() >= 1) {
                Tools.appError(domainNodes.getLength()
                               + " supposedly unique "
                               + domainName
                               + " configs.");
                return null;
            } else {
                Tools.appWarning("could not find xml for " + domainName);
                return null;
            }
        } catch (final javax.xml.xpath.XPathExpressionException e) {
            Tools.appError("could not evaluate: ", e);
            return null;
        }
        return domainNode;
    }

    /** Convert xml node to the string. */
    private void saveDomainXML(final String configName, final Node node) {
        String xml = null;
        try {
            Transformer transformer =
                            TransformerFactory.newInstance().newTransformer();
            final StreamResult res = new StreamResult(new StringWriter());
            final DOMSource src = new DOMSource(node);
            transformer.transform(src, res);
            xml = res.getWriter().toString();
        } catch (final javax.xml.transform.TransformerException e) {
            e.printStackTrace();
            return;
        }
        if (xml != null) {
            host.getSSH().scp(xml, configName, "0600", true);
        }
    }

    /** Return devices node. */
    private Node getDevicesNode(final XPath xpath, final Node domainNode) {
        final String devicesPath = "devices";
        try {
            final NodeList devicesNodes = (NodeList) xpath.evaluate(
                                                       devicesPath,
                                                       domainNode,
                                                       XPathConstants.NODESET);
            if (devicesNodes.getLength() != 1) {
                Tools.appWarning("devices nodes: " + devicesNodes.getLength());
                return null;
            }
            return devicesNodes.item(0);
        } catch (final javax.xml.xpath.XPathExpressionException e) {
            Tools.appError("could not evaluate: ", e);
            return null;
        }
    }

    /** Modify disk XML. */
    public final void modifyDiskXML(final String domainName,
                                    final String targetDev,
                                    final Map<String, String> parametersMap) {
        final String configName = namesConfigsMap.get(domainName);
        if (configName == null) {
            return;
        }
        final Node domainNode = getDomainNode(domainName);
        if (domainNode == null) {
            return;
        }
        final XPath xpath = XPathFactory.newInstance().newXPath();
        final Node devicesNode = getDevicesNode(xpath, domainNode);
        if (devicesNode == null) {
            return;
        }
        try {
            final String diskPath = "devices/disk";
            final NodeList nodes = (NodeList) xpath.evaluate(
                                                       diskPath,
                                                       domainNode,
                                                       XPathConstants.NODESET);
            Element diskNode = null;
            if (targetDev != null) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    final Node mn = getChildNode(nodes.item(i), "target");
                    if (targetDev.equals(getAttribute(mn, "dev"))) {
                        diskNode = (Element) nodes.item(i);
                    }
                }
            }
            if (diskNode == null) {
                diskNode = (Element) devicesNode.appendChild(
                          domainNode.getOwnerDocument().createElement("disk"));
            }
            for (final String param : parametersMap.keySet()) {
                final String value = parametersMap.get(param);
                if (!DISK_TAG_MAP.containsKey(param)
                    && DISK_ATTRIBUTE_MAP.containsKey(param)) {
                    /* disk attribute */
                    final Node attributeNode =
                                diskNode.getAttributes().getNamedItem(
                                           DISK_ATTRIBUTE_MAP.get(param));
                    if (attributeNode == null) {
                        diskNode.setAttribute(DISK_ATTRIBUTE_MAP.get(param),
                                              value);
                    } else {
                        attributeNode.setNodeValue(value);
                    }
                    continue;
                }
                Element node = (Element) getChildNode(
                                                diskNode,
                                                DISK_TAG_MAP.get(param));
                if ((DISK_ATTRIBUTE_MAP.containsKey(param)
                     || "True".equals(value))
                    && node == null) {
                    node = (Element) diskNode.appendChild(
                          domainNode.getOwnerDocument().createElement(
                                                  DISK_TAG_MAP.get(param)));
                } else if (!DISK_ATTRIBUTE_MAP.containsKey(param)
                           && "False".equals(value)
                           && node != null) {
                    diskNode.removeChild(node);
                }
                if (DISK_ATTRIBUTE_MAP.containsKey(param)) {
                    final Node attributeNode =
                                node.getAttributes().getNamedItem(
                                            DISK_ATTRIBUTE_MAP.get(param));
                    if (attributeNode == null) {
                        node.setAttribute(DISK_ATTRIBUTE_MAP.get(param),
                                          value);

                    } else {
                        if ("".equals(value)) {
                            node.removeAttribute(DISK_ATTRIBUTE_MAP.get(param));
                        } else {
                            attributeNode.setNodeValue(value);
                        }
                    }
                }
            }
        } catch (final javax.xml.xpath.XPathExpressionException e) {
            Tools.appError("could not evaluate: ", e);
            return;
        }
        saveDomainXML(configName, domainNode);
        VIRSH.define(host, configName);
        host.setVMInfoMD5(null);
    }

    /** Remove disk XML. */
    public final void removeDiskXML(final String domainName,
                                    final String targetDev) {
        final String configName = namesConfigsMap.get(domainName);
        if (configName == null) {
            return;
        }
        final Node domainNode = getDomainNode(domainName);
        if (domainNode == null) {
            return;
        }
        final XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            final String diskPath = "devices/disk";
            final NodeList nodes = (NodeList) xpath.evaluate(
                                                       diskPath,
                                                       domainNode,
                                                       XPathConstants.NODESET);
            Element diskNode = null;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node mn = getChildNode(nodes.item(i), "target");
                if (targetDev.equals(getAttribute(mn, "dev"))) {
                    diskNode = (Element) nodes.item(i);
                }
            }
            if (diskNode != null) {
                diskNode.getParentNode().removeChild(diskNode);
            }
        } catch (final javax.xml.xpath.XPathExpressionException e) {
            Tools.appError("could not evaluate: ", e);
            return;
        }
        saveDomainXML(configName, domainNode);
        VIRSH.define(host, configName);
        host.setVMInfoMD5(null);
    }

    /** Modify interface XML. */
    public final void modifyInterfaceXML(
                                     final String domainName,
                                     final String macAddress,
                                     final Map<String, String> parametersMap) {
        final String configName = namesConfigsMap.get(domainName);
        if (configName == null) {
            return;
        }
        final Node domainNode = getDomainNode(domainName);
        if (domainNode == null) {
            return;
        }
        final XPath xpath = XPathFactory.newInstance().newXPath();
        final Node devicesNode = getDevicesNode(xpath, domainNode);
        if (devicesNode == null) {
            return;
        }
        try {
            final String interfacePath = "devices/interface";
            final NodeList nodes = (NodeList) xpath.evaluate(
                                                       interfacePath,
                                                       domainNode,
                                                       XPathConstants.NODESET);
            Element interfaceNode = null;
            if (macAddress != null) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    final Node mn = getChildNode(nodes.item(i), "mac");
                    if (macAddress.equals(getAttribute(mn, "address"))) {
                        interfaceNode = (Element) nodes.item(i);
                    }
                }
            }
            if (interfaceNode == null) {
                interfaceNode = (Element) devicesNode.appendChild(
                    domainNode.getOwnerDocument().createElement("interface"));
            }
            for (final String param : parametersMap.keySet()) {
                if (!INTERFACE_TAG_MAP.containsKey(param)
                    && INTERFACE_ATTRIBUTE_MAP.containsKey(param)) {
                    /* interface attribute */
                    final Node attributeNode =
                            interfaceNode.getAttributes().getNamedItem(
                                       INTERFACE_ATTRIBUTE_MAP.get(param));
                    if (attributeNode == null) {
                        interfaceNode.setAttribute(
                                       INTERFACE_ATTRIBUTE_MAP.get(param),
                                       parametersMap.get(param));
                    } else {
                        attributeNode.setNodeValue(
                                                parametersMap.get(param));
                    }
                    continue;
                }
                Element node = (Element) getChildNode(
                                            interfaceNode,
                                            INTERFACE_TAG_MAP.get(param));
                if (node == null) {
                    node = (Element) interfaceNode.appendChild(
                            domainNode.getOwnerDocument().createElement(
                                            INTERFACE_TAG_MAP.get(param)));
                }
                if (INTERFACE_ATTRIBUTE_MAP.containsKey(param)) {
                    final Node attributeNode =
                            node.getAttributes().getNamedItem(
                                       INTERFACE_ATTRIBUTE_MAP.get(param));
                    if (attributeNode == null) {
                        node.setAttribute(
                                        INTERFACE_ATTRIBUTE_MAP.get(param),
                                        parametersMap.get(param));
                    } else {
                        attributeNode.setNodeValue(
                                                parametersMap.get(param));
                    }
                }
            }
        } catch (final javax.xml.xpath.XPathExpressionException e) {
            Tools.appError("could not evaluate: ", e);
            return;
        }
        saveDomainXML(configName, domainNode);
        VIRSH.define(host, configName);
        host.setVMInfoMD5(null);
    }

    /** Remove interface XML. */
    public final void removeInterfaceXML(final String domainName,
                                         final String macAddress) {
        final String configName = namesConfigsMap.get(domainName);
        if (configName == null) {
            return;
        }
        final Node domainNode = getDomainNode(domainName);
        if (domainNode == null) {
            return;
        }
        final XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            final String interfacePath = "devices/interface";
            final NodeList nodes = (NodeList) xpath.evaluate(
                                                       interfacePath,
                                                       domainNode,
                                                       XPathConstants.NODESET);
            Element interfaceNode = null;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node mn = getChildNode(nodes.item(i), "mac");
                if (macAddress.equals(getAttribute(mn, "address"))) {
                    interfaceNode = (Element) nodes.item(i);
                }
            }
            if (interfaceNode != null) {
                interfaceNode.getParentNode().removeChild(interfaceNode);
            }
        } catch (final javax.xml.xpath.XPathExpressionException e) {
            Tools.appError("could not evaluate: ", e);
            return;
        }
        saveDomainXML(configName, domainNode);
        VIRSH.define(host, configName);
        host.setVMInfoMD5(null);
    }

    /** Updates data. */
    public final boolean update() {
        final String command = host.getDistCommand("VMSXML.GetData",
                                                   (ConvertCmdCallback) null);
        final SSH.SSHOutput ret = Tools.execCommand(
                                               host,
                                               command,
                                               null,  /* ExecCallback */
                                               false,  /* outputVisible */
                                               SSH.DEFAULT_COMMAND_TIMEOUT);
        if (ret.getExitCode() != 0) {
            return false;
        }
        final String output = ret.getOutput();
        if (output == null) {
            return false;
        }
        final Document document = getXMLDocument(output);
        try {
            mXMLDocumentLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        xmlDocument = document;
        mXMLDocumentLock.release();
        if (document == null) {
            return false;
        }
        final Node vmsNode = getChildNode(document, "vms");
        final String md5 = getAttribute(vmsNode, "md5");
        if (md5 == null
            || md5.equals(host.getVMInfoMD5())) {
            return false;
        }
        host.setVMInfoMD5(md5);
        final NodeList vms = vmsNode.getChildNodes();
        for (int i = 0; i < vms.getLength(); i++) {
            final Node node = vms.item(i);
            if ("net".equals(node.getNodeName())) {
                updateNetworks(node);
            }
            if ("vm".equals(node.getNodeName())) {
                updateVM(node);
            }
        }
        return true;
    }

    /** Updates one network. */
    private void updateNetworks(final Node netNode) {
        /* one vm */
        if (netNode == null) {
            return;
        }
        final String name = getAttribute(netNode, VM_PARAM_NAME);
        final String config = getAttribute(netNode, "config");
        netConfigsMap.put(config, name);
        netNamesConfigsMap.put(name, config);
        final String autostartString = getAttribute(netNode,
                                                    NET_PARAM_AUTOSTART);
        parseNetConfig(getChildNode(netNode, "network"), name, autostartString);
    }

    /** Parses the libvirt network config file. */
    private void parseNetConfig(final Node networkNode,
                                final String nameInFilename,
                                final String autostartString) {
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
                name = getText(optionNode);
                if (!name.equals(nameInFilename)) {
                    Tools.appWarning("unexpected name: " + name
                                     + " != " + nameInFilename);
                    return;
                }
            } else if (NET_PARAM_UUID.equals(nodeName)) {
                uuid = getText(optionNode);
            } else if ("forward".equals(nodeName)) {
                forwardMode = getAttribute(optionNode, "mode");
            } else if ("bridge".equals(nodeName)) {
                bridgeName = getAttribute(optionNode, "name");
                bridgeSTP = getAttribute(optionNode, "stp");
                bridgeDelay = getAttribute(optionNode, "delay");
                bridgeForwardDelay = getAttribute(optionNode, "forwardDelay");
            } else if (!"#text".equals(nodeName)) {
                Tools.appWarning("unknown network option: "
                                 + nodeName);
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

            networkMap.put(name, networkData);
        }
    }

    /** Parses the libvirt config file. */
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
            if (VM_PARAM_NAME.equals(option.getNodeName())) {
                name = getText(option);
                if (!domainNames.contains(name)) {
                    domainNames.add(name);
                }
                if (!name.equals(nameInFilename)) {
                    Tools.appWarning("unexpected name: " + name
                                     + " != " + nameInFilename);
                    return;
                }
            } else if (VM_PARAM_VCPU.equals(option.getNodeName())) {
                parameterValues.put(name, VM_PARAM_VCPU, getText(option));
            } else if (VM_PARAM_CURRENTMEMORY.equals(option.getNodeName())) {
                parameterValues.put(name,
                                    VM_PARAM_CURRENTMEMORY,
                                    Tools.convertKilobytes(getText(option)));
            } else if (VM_PARAM_MEMORY.equals(option.getNodeName())) {
                parameterValues.put(name,
                                    VM_PARAM_MEMORY,
                                    Tools.convertKilobytes(getText(option)));
            } else if ("os".equals(option.getNodeName())) {
                final NodeList osOptions = option.getChildNodes();
                for (int j = 0; j < osOptions.getLength(); j++) {
                    final Node osOption = osOptions.item(j);
                    if (VM_PARAM_BOOT.equals(osOption.getNodeName())) {
                        parameterValues.put(name,
                                            VM_PARAM_BOOT,
                                            getAttribute(osOption, "dev"));
                    }
                }
            } else if ("devices".equals(option.getNodeName())) {
                final Map<String, DiskData> devMap =
                                    new LinkedHashMap<String, DiskData>();
                final Map<String, InterfaceData> macMap =
                                    new LinkedHashMap<String, InterfaceData>();
                final NodeList devices = option.getChildNodes();
                for (int j = 0; j < devices.getLength(); j++) {
                    final Node deviceNode = devices.item(j);
                    if ("input".equals(deviceNode.getNodeName())) {
                        final String type = getAttribute(deviceNode, "type");
                        if ("tablet".equals(type)) {
                            tabletOk = true;
                        }
                    } else if ("graphics".equals(deviceNode.getNodeName())) {
                        /** remotePort will be overwritten with virsh output */
                        final String type = getAttribute(deviceNode, "type");
                        final String port = getAttribute(deviceNode, "port");
                        final String ap = getAttribute(deviceNode, "autoport");
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
                    } else if ("disk".equals(deviceNode.getNodeName())) {
                        final String type = getAttribute(deviceNode, "type");
                        final String device = getAttribute(deviceNode,
                                                           "device");
                        final NodeList opts = deviceNode.getChildNodes();
                        String sourceFile = null;
                        String sourceDev = null;
                        String targetDev = null;
                        String targetBus = null;
                        String driverName = null;
                        String driverType = null;
                        boolean readonly = false;
                        boolean shareable = false;
                        for (int k = 0; k < opts.getLength(); k++) {
                            final Node optionNode = opts.item(k);
                            final String nodeName = optionNode.getNodeName();
                            if ("source".equals(nodeName)) {
                                sourceFile = getAttribute(optionNode, "file");
                                sourceDev = getAttribute(optionNode, "dev");
                            } else if ("target".equals(nodeName)) {
                                targetDev = getAttribute(optionNode, "dev");
                                targetBus = getAttribute(optionNode, "bus");
                            } else if ("driver".equals(nodeName)) {
                                driverName = getAttribute(optionNode, "name");
                                driverType = getAttribute(optionNode, "type");
                            } else if ("readonly".equals(nodeName)) {
                                readonly = true;
                            } else if ("shareable".equals(nodeName)) {
                                shareable = true;
                            } else if (!"#text".equals(nodeName)) {
                                Tools.appWarning("unknown disk option: "
                                                 + nodeName);
                            }
                        }
                        if (targetDev != null) {
                            final DiskData diskData =
                                         new DiskData(type,
                                                      targetDev,
                                                      sourceFile,
                                                      sourceDev,
                                                      targetBus + "/" + device,
                                                      driverName,
                                                      driverType,
                                                      readonly,
                                                      shareable);
                            devMap.put(targetDev, diskData);
                        }
                    } else if ("interface".equals(deviceNode.getNodeName())) {
                        final String type = getAttribute(deviceNode, "type");
                        String macAddress = null;
                        String sourceNetwork = null;
                        String sourceBridge = null;
                        String targetDev = null;
                        String modelType = null;
                        final NodeList opts = deviceNode.getChildNodes();
                        for (int k = 0; k < opts.getLength(); k++) {
                            final Node optionNode = opts.item(k);
                            final String nodeName = optionNode.getNodeName();
                            if ("source".equals(nodeName)) {
                                sourceBridge = getAttribute(optionNode,
                                                            "bridge");
                                sourceNetwork = getAttribute(optionNode,
                                                             "network");
                            } else if ("target".equals(nodeName)) {
                                targetDev = getAttribute(optionNode, "dev");
                            } else if ("mac".equals(nodeName)) {
                                macAddress = getAttribute(optionNode,
                                                          "address");
                            } else if ("model".equals(nodeName)) {
                                modelType = getAttribute(optionNode, "type");
                            } else if (!"#text".equals(nodeName)) {
                                Tools.appWarning("unknown interface option: "
                                                 + nodeName);
                            }
                        }
                        if (macAddress != null) {
                            final InterfaceData interfaceData =
                                              new InterfaceData(type,
                                                                macAddress,
                                                                sourceNetwork,
                                                                sourceBridge,
                                                                targetDev,
                                                                modelType);
                            macMap.put(macAddress, interfaceData);
                        }
                    }
                }
                disksMap.put(name, devMap);
                interfacesMap.put(name, macMap);
            }
        }
        if (!tabletOk) {
            Tools.appWarning("you should enable input type tablet for " + name);
        }
    }

    /** Updates one vm. */
    private void updateVM(final Node vmNode) {
        /* one vm */
        if (vmNode == null) {
            return;
        }
        final Node infoNode = getChildNode(vmNode, "info");
        final String name = getAttribute(vmNode, VM_PARAM_NAME);
        final String config = getAttribute(vmNode, "config");
        configsMap.put(config, name);
        namesConfigsMap.put(name, config);
        final String autostart = getAttribute(vmNode, VM_PARAM_AUTOSTART);
        if (autostart == null) {
            parameterValues.put(name, VM_PARAM_AUTOSTART, "false");
        } else {
            parameterValues.put(name, VM_PARAM_AUTOSTART, autostart);
        }
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
     * Updates all data for this domain.
     */
    public final void parseInfo(final String name, final String info) {
        if (info != null) {
            boolean running = false;
            boolean suspended = false;
            for (final String line : info.split("\n")) {
                final String[] optionValue = line.split(":");
                if (optionValue.length == 2) {
                    final String option = optionValue[0].trim();
                    final String value = optionValue[1].trim();
                    if ("State".equals(option)) {
                        if ("running".equals(value)) {
                            running = true;
                            suspended = false;
                        } else if ("paused".equals(value)) {
                            running = true;
                            suspended = true;
                        }
                    }
                }
            }
            runningMap.put(name, running);
            suspendedMap.put(name, suspended);
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
     * Returns whether the domain is suspended.
     */
    public final boolean isSuspended(final String name) {
        final Boolean s = suspendedMap.get(name);
        if (s != null) {
            return s;
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

    /**
     * Returns value.
     */
    public final String getValue(final String name, final String param) {
        return (String) parameterValues.get(name, param);
    }

    /**
     * Returns disk data.
     */
    public final Map<String, DiskData> getDisks(final String name) {
        return disksMap.get(name);
    }

    /** Returns interface data. */
    public final Map<String, InterfaceData> getInterfaces(final String name) {
        return interfacesMap.get(name);
    }

    /** Returns array of networks. */
    public final List<String> getNetworks() {
        return new ArrayList<String>(networkMap.keySet());
    }

    /** Class that holds data about networks. */
    public class NetworkData {
        /** Name value pairs. */
        private final Map<String, String> valueMap =
                                                new HashMap<String, String>();
        /** Name of the network. */
        private final String name;
        /** UUID of the network. */
        private final String uuid;
        /** Autostart. */
        private final boolean autostart;
        /** Forward mode. */
        private final String forwardMode;
        /** Bridge name. */
        private final String bridgeName;
        /** Bridge STP. */
        private final String bridgeSTP;
        /** Bridge delay. */
        private final String bridgeDelay;
        /** Bridge forward delay. */
        private final String bridgeForwardDelay;

        /** Autostart. */
        public static final String AUTOSTART = "autostart";
        /** Forward mode. */
        public static final String FORWARD_MODE = "forward_mode";
        /** Bridge name. */
        public static final String BRIDGE_NAME = "bridge_name";
        /** Bridge STP. */
        public static final String BRIDGE_STP = "bridge_stp";
        /** Bridge delay. */
        public static final String BRIDGE_DELAY = "bridge_delay";
        /** Bridge forward delay. */
        public static final String BRIDGE_FORWARD_DELAY =
                                                        "bridge_forward_delay";

        /** Creates new NetworkData object. */
        public NetworkData(final String name,
                           final String uuid,
                           final boolean autostart,
                           final String forwardMode,
                           final String bridgeName,
                           final String bridgeSTP,
                           final String bridgeDelay,
                           final String bridgeForwardDelay) {
            this.name = name;
            this.uuid = uuid;
            this.autostart = autostart;
            if (autostart) {
                valueMap.put(AUTOSTART, "true");
            } else {
                valueMap.put(AUTOSTART, "false");
            }
            this.forwardMode = forwardMode;
            valueMap.put(FORWARD_MODE, forwardMode);
            this.bridgeName = bridgeName;
            valueMap.put(BRIDGE_NAME, bridgeName);
            this.bridgeSTP = bridgeSTP;
            valueMap.put(BRIDGE_STP, bridgeSTP);
            this.bridgeDelay = bridgeDelay;
            valueMap.put(BRIDGE_DELAY, bridgeDelay);
            this.bridgeForwardDelay = bridgeForwardDelay;
            valueMap.put(BRIDGE_FORWARD_DELAY, bridgeForwardDelay);
        }

        /** Whether it is autostart. */
        public final boolean isAutostart() {
            return autostart;
        }

        /** Returns forward mode. */
        public final String getForwardMode() {
            return forwardMode;
        }

        /** Returns bridge name. */
        public final String getBridgeName() {
            return bridgeName;
        }

        /** Returns bridge STP. */
        public final String getBridgeSTP() {
            return bridgeSTP;
        }

        /** Returns bridge delay. */
        public final String getBridgeDelay() {
            return bridgeDelay;
        }

        /** Returns bridge forward delay. */
        public final String getBridgeForwardDelay() {
            return bridgeForwardDelay;
        }

        /** Returns value of this parameter. */
        public final String getValue(final String param) {
            return valueMap.get(param);
        }
    }

    /** Class that holds data about virtual disks. */
    public class DiskData {
        /** Type: file, block... */
        private final String type;
        /** Target device: hda, hdb, hdc, sda... */
        private final String targetDev;
        /** Source file. */
        private final String sourceFile;
        /** Source device: /dev/drbd0... */
        private final String sourceDev;
        /** Target bus: ide... and type: disk..., delimited with, */
        private final String targetBusType;
        /** Driver name: qemu... */
        private final String driverName;
        /** Driver type: raw... */
        private final String driverType;
        /** Whether the disk is read only. */
        private final boolean readonly;
        /** Whether the disk is shareable. */
        private final boolean shareable;
        /** Name value pairs. */
        private final Map<String, String> valueMap =
                                                new HashMap<String, String>();
        /** Type. */
        public static final String TYPE = "type";
        /** Target device string. */
        public static final String TARGET_DEVICE = "target_device";
        /** Source file. */
        public static final String SOURCE_FILE = "source_file";
        /** Source dev. */
        public static final String SOURCE_DEVICE = "source_dev";
        /** Target bus and type. */
        public static final String TARGET_BUS_TYPE = "target_bus_type";
        /** Target bus. */
        public static final String TARGET_BUS = "target_bus";
        /** Target type. */
        public static final String TARGET_TYPE = "target_type";
        /** Driver name. */
        public static final String DRIVER_NAME = "driver_name";
        /** Driver type. */
        public static final String DRIVER_TYPE = "driver_type";
        /** Readonly. */
        public static final String READONLY = "readonly";
        /** Shareable. */
        public static final String SHAREABLE = "shareable";

        /**
         * Creates new DiskData object.
         */
        public DiskData(final String type,
                        final String targetDev,
                        final String sourceFile,
                        final String sourceDev,
                        final String targetBusType,
                        final String driverName,
                        final String driverType,
                        final boolean readonly,
                        final boolean shareable) {
            this.type = type;
            valueMap.put(TYPE, type);
            this.targetDev = targetDev;
            valueMap.put(TARGET_DEVICE, targetDev);
            this.sourceFile = sourceFile;
            valueMap.put(SOURCE_FILE, sourceFile);
            this.sourceDev = sourceDev;
            valueMap.put(SOURCE_DEVICE, sourceDev);
            this.targetBusType = targetBusType;
            valueMap.put(TARGET_BUS_TYPE, targetBusType);
            this.driverName = driverName;
            valueMap.put(DRIVER_NAME, driverName);
            this.driverType = driverType;
            valueMap.put(DRIVER_TYPE, driverType);
            this.readonly = readonly;
            if (readonly) {
                valueMap.put(READONLY, "True");
            } else {
                valueMap.put(READONLY, "False");
            }
            this.shareable = shareable;
            if (shareable) {
                valueMap.put(SHAREABLE, "True");
            } else {
                valueMap.put(SHAREABLE, "False");
            }
        }

        /** Returns type. */
        public final String getType() {
            return type;
        }

        /** Returns target device. */
        public final String getTargetDev() {
            return targetDev;
        }

        /** Returns source file. */
        public final String getSourceFile() {
            return sourceFile;
        }

        /** Returns source device. */
        public final String getSourceDev() {
            return sourceDev;
        }

        /** Returns target bus. */
        public final String getTargetBusType() {
            return targetBusType;
        }

        /** Returns driver name. */
        public final String getDriverName() {
            return driverName;
        }

        /** Returns driver type. */
        public final String getDriverType() {
            return driverType;
        }

        /** Returns whether the disk is read only. */
        public final boolean isReadonly() {
            return readonly;
        }

        /** Returns whether the disk is read only. */
        public final boolean isShareable() {
            return shareable;
        }

        /** Returns value of this parameter. */
        public final String getValue(final String param) {
            return valueMap.get(param);
        }
    }

    /** Class that holds data about virtual interfaces. */
    public class InterfaceData {
        /** Type: network, bridge... */
        private final String type;
        /** Mac address. */
        private final String macAddress;
        /** Source network: default, ... */
        private final String sourceNetwork;
        /** Source bridge: br0... */
        private final String sourceBridge;
        /** Target dev: vnet0... */
        private final String targetDev;
        /** Model type: virtio... */
        private final String modelType;
        /** Name value pairs. */
        private final Map<String, String> valueMap =
                                                new HashMap<String, String>();

        /** Type. */
        public static final String TYPE = "type";
        /** Mac address. */
        public static final String MAC_ADDRESS = "mac_address";
        /** Source network. */
        public static final String SOURCE_NETWORK = "source_network";
        /** Source bridge. */
        public static final String SOURCE_BRIDGE = "source_bridge";
        /** Target dev. */
        public static final String TARGET_DEV = "target_dev";
        /** Model type. */
        public static final String MODEL_TYPE = "model_type";
        /** Creates new InterfaceData object. */
        public InterfaceData(final String type,
                             final String macAddress,
                             final String sourceNetwork,
                             final String sourceBridge,
                             final String targetDev,
                             final String modelType) {
            this.type = type;
            valueMap.put(TYPE, type);
            this.macAddress = macAddress;
            valueMap.put(MAC_ADDRESS, macAddress);
            this.sourceNetwork = sourceNetwork;
            valueMap.put(SOURCE_NETWORK, sourceNetwork);
            this.sourceBridge = sourceBridge;
            valueMap.put(SOURCE_BRIDGE, sourceBridge);
            this.targetDev = targetDev;
            valueMap.put(TARGET_DEV, targetDev);
            this.modelType = modelType;
            valueMap.put(MODEL_TYPE, modelType);
        }

        /** Returns type. */
        public final String getType() {
            return type;
        }

        /** Returns mac address. */
        public final String getMacAddress() {
            return macAddress;
        }

        /** Returns source network. */
        public final String getSourceNetwork() {
            return sourceNetwork;
        }

        /** Returns source bridge. */
        public final String getSourceBridge() {
            return sourceBridge;
        }

        /** Returns target dev. */
        public final String getTargetDev() {
            return targetDev;
        }

        /** Returns model type. */
        public final String getModelType() {
            return modelType;
        }

        /** Returns value of this parameter. */
        public final String getValue(final String param) {
            return valueMap.get(param);
        }
    }
}
