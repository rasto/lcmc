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


package lcmc.vm.domain;

import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import lcmc.host.domain.Host;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.XMLTools;
import lcmc.common.domain.ConvertCmdCallback;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;
import lcmc.common.domain.Unit;
import lcmc.vm.domain.data.DiskData;
import lcmc.vm.domain.data.FilesystemData;
import lcmc.vm.domain.data.GraphicsData;
import lcmc.vm.domain.data.InputDevData;
import lcmc.vm.domain.data.InterfaceData;
import lcmc.vm.domain.data.ParallelData;
import lcmc.vm.domain.data.SerialData;
import lcmc.vm.domain.data.SoundData;
import lcmc.vm.domain.data.VideoData;
import lcmc.vm.service.VIRSH;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.SshOutput;

import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Named
public class VmsXml {
    private static final Logger LOG = LoggerFactory.getLogger(VmsXml.class);

    @Autowired
    private NetworkParser networkParser;
    @Autowired
    private VMParser vmParser;
    @Autowired
    private VMCreator vmCreator;

    private final Map<String, String> namesToConfigs = new HashMap<String, String>();

    /** Returns string representation of the port; it can be autoport. */
    static String portString(final String port) {
        if ("-1".equals(port)) {
            return "auto";
        }
        return port;
    }

    /** Returns string representation graphic display SDL/VNC. */
    public static String graphicsDisplayName(final String type, final String port, final String display) {
        if ("vnc".equals(type)) {
            return type + " : " + portString(port);
        } else if ("sdl".equals(type)) {
            return type + " (" + display + ')';
        }
        return "unknown";
    }

    public static Unit getUnitKiBytes() {
        return new Unit("K", "K", "KiByte", "KiBytes");
    }

    public static Unit getUnitMiBytes() {
        return new Unit("M", "M", "MiByte", "MiBytes");
    }

    public static Unit getUnitGiBytes() {
        return new Unit("G", "G", "GiByte", "GiBytes");
    }

    public static Unit getUnitTiBytes() {
        return new Unit("T", "T", "TiByte", "TiBytes");
    }

    public static Unit getUnitPiBytes() {
        return new Unit("P", "P", "PiByte", "PiBytes");
    }

    public static Unit[] getUnits() {
        return new Unit[]{getUnitKiBytes(), getUnitMiBytes(), getUnitGiBytes(), getUnitTiBytes(), getUnitPiBytes()
        };
    }

    //TODO: move somewhere else
    public static Value convertKilobytes(final String kb) {
        if (!Tools.isNumber(kb)) {
            return new StringValue(kb, getUnitKiBytes());
        }
        final double k = Long.parseLong(kb);
        if (k == 0) {
            return new StringValue("0", getUnitKiBytes());
        }
        if (k / 1024 != (long) (k / 1024)) {
            return new StringValue(kb, getUnitKiBytes());
        }
        final double m = k / 1024;
        if (m / 1024 != (long) (m / 1024)) {
            return new StringValue(Long.toString((long) m), getUnitMiBytes());
        }
        final double g = m / 1024;
        if (g / 1024 != (long) (g / 1024)) {
            return new StringValue(Long.toString((long) g), getUnitGiBytes());
        }
        final double t = g / 1024;
        if (t / 1024 != (long) (t / 1024)) {
            return new StringValue(Long.toString((long) t), getUnitTiBytes());
        }
        final double p = t / 1024;
        return new StringValue(Long.toString((long) p), getUnitPiBytes());
    }

    /** Converts value with unit to kilobites. */
    //TODO: move somewhere else
    public static long convertToKilobytes(final Value value) {
        if (value == null) {
            return -1;
        }
        final String numS = value.getValueForConfig();
        if (Tools.isNumber(numS)) {
            long num = Long.parseLong(numS);
            final Unit unitObject = value.getUnit();
            if (unitObject == null) {
                return -1;
            }
            final String unit = unitObject.getShortName();
            if ("P".equalsIgnoreCase(unit)) {
                num = num * 1024 * 1024 * 1024 * 1024;
            } else if ("T".equalsIgnoreCase(unit)) {
                num = num * 1024 * 1024 * 1024;
            } else if ("G".equalsIgnoreCase(unit)) {
                num = num * 1024 * 1024;
            } else if ("M".equalsIgnoreCase(unit)) {
                num *= 1024;
            } else if ("K".equalsIgnoreCase(unit)) {
            } else {
                return -1;
            }
            return num;
        }
        return -1;
    }
    /** Map from domain name and network name to the network data. */
    private Host definedOnHost;

    private final ReadWriteLock mXMLDocumentLock = new ReentrantReadWriteLock();
    private final Lock mXMLDocumentReadLock = mXMLDocumentLock.readLock();
    private final Lock mXMLDocumentWriteLock = mXMLDocumentLock.writeLock();
    private Document xmlDocument = null;
    private String oldConfig = null;

    public void init(final Host definedOnHost) {
        this.definedOnHost = definedOnHost;
    }

    public Node getDomainNode(final String domainName) {
        mXMLDocumentReadLock.lock();
        final Document document;
        try {
            document = xmlDocument;
        } finally {
            mXMLDocumentReadLock.unlock();
        }
        final XPath xpath = XPathFactory.newInstance().newXPath();
        final Node domainNode;
        try {
            final String path = "//vms/vm[@name='" + domainName + "']/config/domain";
            final NodeList domainNodes = (NodeList) xpath.evaluate(path, document, XPathConstants.NODESET);
            if (domainNodes.getLength() == 1) {
                domainNode = domainNodes.item(0);
            } else if (domainNodes.getLength() >= 1) {
                LOG.appError("getDomainNode: " + domainNodes.getLength() + " supposedly unique " + domainName + " configs.");
                return null;
            } else {
                LOG.appWarning("getDomainNode: could not find xml for " + domainName);
                return null;
            }
        } catch (final XPathExpressionException e) {
            LOG.appError("getDomainNode: could not evaluate: ", e);
            return null;
        }
        return domainNode;
    }

    private void saveDomainXML(final String configName, final Node node, final String defineCommand) {
        final String xml;
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            final StreamResult res = new StreamResult(new StringWriter());
            final Source src = new DOMSource(node);
            transformer.transform(src, res);
            xml = res.getWriter().toString();
        } catch (final TransformerException e) {
            LOG.appError("saveDomainXML: " + e.getMessageAndLocation(), e);
            return;
        }
        if (xml != null) {
            definedOnHost.getSSH().scp(xml, configName, "0600", true, defineCommand, null, null);
        }
    }

    private Node getDevicesNode(final XPath xpath, final Node domainNode) {
        try {
            final String devicesPath = "devices";
            final NodeList devicesNodes = (NodeList) xpath.evaluate(devicesPath, domainNode, XPathConstants.NODESET);
            if (devicesNodes.getLength() != 1) {
                LOG.appWarning("getDevicesNode: nodes: " + devicesNodes.getLength());
                return null;
            }
            return devicesNodes.item(0);
        } catch (final XPathExpressionException e) {
            LOG.appError("getDevicesNode: could not evaluate: ", e);
            return null;
        }
    }

    public static String getConfigName(final String type, final String domainName) { //TODO: move out
        if ("xen".equals(type)) {
            return "/etc/xen/vm/" + domainName + ".xml";
        } else if ("lxc".equals(type)) {
            return "/etc/libvirt/lxc/" + domainName + ".xml";
        } else {
            return "/etc/libvirt/qemu/" + domainName + ".xml";
        }
    }

    public Node createDomainXML(final String uuid,
                                final String domainName,
                                final Map<String, String> parametersMap,
                                final boolean needConsole) {
        /* domain type: kvm/xen */
        final String type = parametersMap.get(VMParams.VM_PARAM_DOMAIN_TYPE);
        final String configName = getConfigName(type, domainName);
        namesToConfigs.put(domainName, configName);
        /* build xml */
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        final DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (final ParserConfigurationException pce) {
            throw new RuntimeException("cannot configure parser", pce);
        }
        final Document doc = db.newDocument();
        return vmCreator.createDomain(uuid, domainName, parametersMap, needConsole, type, doc);
    }

    public Node modifyDomainXML(final String domainName, final Map<String, String> parametersMap) {
        final String configName = namesToConfigs.get(domainName);
        if (configName == null) {
            return null;
        }
        final Node domainNode = getDomainNode(domainName);
        if (domainNode == null) {
            return null;
        }
        final Document doc = domainNode.getOwnerDocument();
        vmCreator.modifyDomain(doc, domainNode, parametersMap);
        return domainNode;
    }

    private void modifyXMLOption(final Node domainNode,
                                 final Element hwNode,
                                 final String value,
                                 final int pos,
                                 final String tag0,
                                 final String attribute,
                                 final Map<String, Element> parentNodes) {
        if (tag0 == null && attribute != null) {
            /* attribute */
            final Node attributeNode = hwNode.getAttributes().getNamedItem(attribute);
            if (attributeNode == null) {
                if (value != null && !"".equals(value)) {
                    hwNode.setAttribute(attribute, value);
                }
            } else if (value == null || "".equals(value)) {
                hwNode.removeAttribute(attribute);
            } else {
                attributeNode.setNodeValue(value);
            }
            return;
        }

        if (tag0 == null) {
            return;
        }

        final String tag;
        final int i = tag0.indexOf(':');
        final Element pNode;
        if (i > 0) {
            if (value == null) {
                /* don't make empty parent */
                return;
            }
            /* with parent */
            final String parent = tag0.substring(0, i);
            tag = tag0.substring(i + 1);
            pNode = parentNodes.get(parent);
        } else {
            tag = tag0;
            pNode = hwNode;
        }

        Element node = (Element) XMLTools.getChildNode(pNode, tag, pos);
        if ((attribute != null || "True".equals(value))
            && node == null) {
            node = (Element) pNode.appendChild(domainNode.getOwnerDocument().createElement(tag));
        } else if (node != null && attribute == null && (value == null || "".equals(value))) {
            pNode.removeChild(node);
        }
        parentNodes.put(tag, node);

        if (attribute != null) {
            final Node attributeNode = node.getAttributes().getNamedItem(attribute);
            if (attributeNode == null) {
                if (value != null && !"".equals(value)) {
                    node.setAttribute(attribute, value);
                }
            } else {
                if (value == null || "".equals(value)) {
                    node.removeAttribute(attribute);
                } else {
                    attributeNode.setNodeValue(value);
                }
            }
        }
    }

    private void removeXMLOption(final Element hwNode,
                                 int pos,
                                 final String tag0,
                                 final Map<String, Element> parentNodes) {
        if (tag0 == null) {
            return;
        }

        final String tag;
        final int i = tag0.indexOf(':');
        final Element pNode;
        if (i > 0) {
            /* with parent */
            final String parent = tag0.substring(0, i);
            tag = tag0.substring(i + 1);
            pNode = parentNodes.get(parent);
        } else {
            tag = tag0;
            pNode = hwNode;
        }

        Element node;
        do {
            node = (Element) XMLTools.getChildNode(pNode, tag, pos);
            if (node != null) {
                pNode.removeChild(node);
                pos++;
            }
        } while (node != null);
    }

    /** Modify xml of some device element. */
    private void modifyXML(final Node domainNode,
                           final String domainName,
                           final Map<String, String> tagMap,
                           final Map<String, String> attributeMap,
                           final Map<String, String> parametersMap,
                           final String path,
                           final String elementName,
                           final VirtualHardwareComparator vhc) {
        final String configName = namesToConfigs.get(domainName);
        if (configName == null) {
            return;
        }
        if (domainNode == null) {
            return;
        }
        final XPath xpath = XPathFactory.newInstance().newXPath();
        final Node devicesNode = getDevicesNode(xpath, domainNode);
        if (devicesNode == null) {
            return;
        }
        try {
            final NodeList nodes = (NodeList) xpath.evaluate(path, domainNode, XPathConstants.NODESET);
            Element hwNode = vhc.getElement(nodes, parametersMap);
            if (hwNode == null) {
                hwNode = (Element) devicesNode.appendChild(domainNode.getOwnerDocument().createElement(elementName));
            }
            final Map<String, Element> parentNodes = new HashMap<String, Element>();
            for (final String param : parametersMap.keySet()) {
                final String value = parametersMap.get(param);
                final String tag = tagMap.get(param);
                final String attribute = attributeMap.get(param);
                if (value == null) {
                    modifyXMLOption(domainNode, hwNode, value, 0, tag, attribute, parentNodes);
                } else {
                    final String[] values = value.split("\\s*,\\s*");
                    int pos = 0;
                    for (final String v : values) {
                        modifyXMLOption(domainNode, hwNode, v, pos, tag, attribute, parentNodes);
                        pos++;
                    }
                    removeXMLOption(hwNode, pos, tag, parentNodes);
                }
            }
            final Node hwAddressNode = XMLTools.getChildNode(hwNode, VMParams.HW_ADDRESS);
            if (hwAddressNode != null) {
                hwNode.removeChild(hwAddressNode);
            }
        } catch (final XPathExpressionException e) {
            LOG.appError("modifyXML: could not evaluate: ", e);
        }
    }

    private void removeXML(final String domainName,
                           final Map<String, String> parametersMap,
                           final String path,
                           final VirtualHardwareComparator vhc,
                           final String virshOptions) {
        final String configName = namesToConfigs.get(domainName);
        if (configName == null) {
            return;
        }
        final Node domainNode = getDomainNode(domainName);
        if (domainNode == null) {
            return;
        }
        final XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            final NodeList nodes = (NodeList) xpath.evaluate(path, domainNode, XPathConstants.NODESET);
            final Element hwNode = vhc.getElement(nodes, parametersMap);
            if (hwNode != null) {
                hwNode.getParentNode().removeChild(hwNode);
            }
        } catch (final XPathExpressionException e) {
            LOG.appError("removeXML: could not evaluate: ", e);
            return;
        }
        saveAndDefine(domainNode, domainName, virshOptions);
    }

    public void modifyDiskXML(final Node domainNode,
                              final String domainName,
                              final Map<String, String> parametersMap) {
        modifyXML(domainNode, domainName,
                  VMParams.PARAM_DISK_TAG,
                  VMParams.PARAM_DISK_ATTRIBUTE,
                  parametersMap,
                  "devices/disk",
                  "disk",
                  getDiskDataComparator());
    }

    public void modifyFilesystemXML(final Node domainNode,
                                    final String domainName,
                                    final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  VMParams.PARAM_FILESYSTEM_TAG,
                  VMParams.PARAM_FILESYSTEM_ATTRIBUTE,
                  parametersMap,
                  "devices/filesystem",
                  "filesystem",
                  getFilesystemDataComparator());
    }

    public void saveAndDefine(final Node domainNode, final String domainName, final String virshOptions) {
        final String configName = namesToConfigs.get(domainName);
        final String defineCommand = VIRSH.getDefineCommand(definedOnHost,
                                                            configName + ".new" + " && rm " + configName + ".new",
                                                            virshOptions);
        saveDomainXML(configName, domainNode, defineCommand);
        definedOnHost.setVMInfoMD5(null);
    }

    public void modifyInterfaceXML(final Node domainNode,
                                   final String domainName,
                                   final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  VMParams.PARAM_INTERFACE_TAG,
                  VMParams.PARAM_INTERFACE_ATTRIBUTE,
                  parametersMap,
                  "devices/interface",
                  "interface",
                  getInterfaceDataComparator());
    }

    public void modifyInputDevXML(final Node domainNode,
                                  final String domainName,
                                  final Map<String, String> parametersMap) {
        modifyXML(domainNode, domainName,
                  VMParams.PARAM_INPUTDEV_TAG,
                  VMParams.PARAM_INPUTDEV_ATTRIBUTE,
                  parametersMap,
                  "devices/input",
                  "input",
                  getInputDevDataComparator());
    }

    public void modifyGraphicsXML(final Node domainNode,
                                  final String domainName,
                                  final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  VMParams.PARAM_GRAPHICS_TAG,
                  VMParams.PARAM_GRAPHICS_ATTRIBUTE,
                  parametersMap,
                  "devices/graphics",
                  "graphics",
                  getGraphicsDataComparator());
    }

    public void modifySoundXML(final Node domainNode,
                               final String domainName,
                               final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  VMParams.PARAM_SOUND_TAG,
                  VMParams.PARAM_SOUND_ATTRIBUTE,
                  parametersMap,
                  "devices/sound",
                  "sound",
                  getSoundDataComparator());
    }

    public void modifySerialXML(final Node domainNode,
                                final String domainName,
                                final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  VMParams.PARAM_SERIAL_TAG,
                  VMParams.PARAM_SERIAL_ATTRIBUTE,
                  parametersMap,
                  "devices/serial",
                  "serial",
                  getSerialDataComparator());
    }

    public void modifyParallelXML(final Node domainNode,
                                  final String domainName,
                                  final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  VMParams.PARAM_PARALLEL_TAG,
                  VMParams.PARAM_PARALLEL_ATTRIBUTE,
                  parametersMap,
                  "devices/parallel",
                  "parallel",
                  getParallelDataComparator());
    }

    /** Modify video device XML. */
    public void modifyVideoXML(final Node domainNode,
                               final String domainName,
                               final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  VMParams.PARAM_VIDEO_TAG,
                  VMParams.PARAM_VIDEO_ATTRIBUTE,
                  parametersMap,
                  "devices/video",
                  "video",
                  getVideoDataComparator());
    }

    public void removeDiskXML(final String domainName,
                              final Map<String, String> parametersMap,
                              final String virshOptions) {
        removeXML(domainName, parametersMap, "devices/disk", getDiskDataComparator(), virshOptions);
    }

    public void removeFilesystemXML(final String domainName,
                                    final Map<String, String> parametersMap,
                                    final String virshOptions) {
        removeXML(domainName, parametersMap, "devices/filesystem", getFilesystemDataComparator(), virshOptions);
    }

    public void removeInterfaceXML(final String domainName,
                                   final Map<String, String> parametersMap,
                                   final String virshOptions) {
        removeXML(domainName, parametersMap, "devices/interface", getInterfaceDataComparator(), virshOptions);
    }

    public void removeInputDevXML(final String domainName,
                                  final Map<String, String> parametersMap,
                                  final String virshOptions) {
        removeXML(domainName, parametersMap, "devices/input", getInputDevDataComparator(), virshOptions);
    }

    public void removeGraphicsXML(final String domainName,
                                  final Map<String, String> parametersMap,
                                  final String virshOptions) {
        removeXML(domainName, parametersMap, "devices/graphics", getGraphicsDataComparator(), virshOptions);
    }

    public void removeSoundXML(final String domainName,
                               final Map<String, String> parametersMap,
                               final String virshOptions) {
        removeXML(domainName, parametersMap, "devices/sound", getSoundDataComparator(), virshOptions);
    }

    public void removeSerialXML(final String domainName,
                                final Map<String, String> parametersMap,
                                final String virshOptions) {
        removeXML(domainName, parametersMap, "devices/serial", getSerialDataComparator(), virshOptions);
    }

    public void removeParallelXML(final String domainName,
                                  final Map<String, String> parametersMap,
                                  final String virshOptions) {
        removeXML(domainName, parametersMap, "devices/parallel", getParallelDataComparator(), virshOptions);
    }

    public void removeVideoXML(final String domainName,
                               final Map<String, String> parametersMap,
                               final String virshOptions) {
        removeXML(domainName, parametersMap, "devices/video", getVideoDataComparator(), virshOptions);
    }

    public boolean parseXml() {
        final String command = definedOnHost.getDistCommand("VMSXML.GetData", (ConvertCmdCallback) null);
        final SshOutput ret = definedOnHost.captureCommand(new ExecCommandConfig().command(command)
                                                                                  .silentCommand()
                                                                                  .silentOutput());
        if (ret.getExitCode() != 0) {
            return false;
        }
        final String output = ret.getOutput();
        if (output == null) {
            return false;
        }
        return parseXml(output);
    }

    public boolean parseXml(final String xml) {
        oldConfig = xml;
        final Document document = XMLTools.getXMLDocument(xml);
        mXMLDocumentWriteLock.lock();
        try {
            xmlDocument = document;
        } finally {
            mXMLDocumentWriteLock.unlock();
        }
        if (document == null) {
            return false;
        }
        final Node vmsNode = XMLTools.getChildNode(document, "vms");
        final String md5 = XMLTools.getAttribute(vmsNode, "md5");
        if (md5 == null || md5.equals(definedOnHost.getVMInfoMD5())) {
            return false;
        }
        definedOnHost.setVMInfoMD5(md5);
        final NodeList vms = vmsNode.getChildNodes();
        for (int i = 0; i < vms.getLength(); i++) {
            final Node node = vms.item(i);
            if ("net".equals(node.getNodeName())) {
                networkParser.parseNetwork(node);
            } else if ("vm".equals(node.getNodeName())) {
                vmParser.parseVM(node, definedOnHost, namesToConfigs);
            } else if ("version".equals(node.getNodeName())) {
                definedOnHost.setLibvirtVersion(XMLTools.getText(node));
            }
        }
        return true;
    }

    public Collection<String> getDomainNames() {
        return vmParser.getDomainNames();
    }

    public boolean isRunning(final String domainName) {
        return vmParser.isRunning(domainName);
    }

    public boolean isSuspended(final String domainName) {
        return vmParser.isSuspended(domainName);
    }


    public int getRemotePort(final String domainName) {
        return vmParser.getRemotePort(domainName);
    }

    public Host getDefinedOnHost() {
        return definedOnHost;
    }

    public Collection<Value> getConfigs() {
        return vmParser.getConfigs();
    }

    public String getNameFromConfig(final String config) {
        return vmParser.getNameFromConfig(config);
    }

    public String getValue(final String name, final String param) {
        return vmParser.getValue(name, param);
    }

    public List<Value> getNetworks() {
        return networkParser.getNetworks();
    }

    public Map<String, DiskData> getDisks(final String name) {
        return vmParser.getDisks(name);
    }

    public Map<String, FilesystemData> getFilesystems(final String name) {
        return vmParser.getFilesystems(name);
    }

    public Map<String, InterfaceData> getInterfaces(final String name) {
        return vmParser.getInterfaces(name);
    }

    public Map<String, InputDevData> getInputDevs(final String name) {
        return vmParser.getInputDevs(name);
    }

    public Map<String, GraphicsData> getGraphicDisplays(final String name) {
        return vmParser.getGraphicDisplays(name);
    }

    public Map<String, SoundData> getSounds(final String name) {
        return vmParser.getSounds(name);
    }

    public Map<String, SerialData> getSerials(final String name) {
        return vmParser.getSerials(name);
    }

    public Map<String, ParallelData> getParallels(final String name) {
        return vmParser.getParallels(name);
    }

    public Map<String, VideoData> getVideos(final String name) {
        return vmParser.getVideos(name);
    }

    /** Returns function that gets the node that belongs to the paremeters. */
    protected VirtualHardwareComparator getDiskDataComparator() {
        return new VirtualHardwareComparator() {
            @Override
            public Element getElement(final NodeList nodes, final Map<String, String> parameters) {
                Element el = null;
                final String targetDev = parameters.get(DiskData.SAVED_TARGET_DEVICE);
                if (targetDev != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = XMLTools.getChildNode(nodes.item(i), "target");
                        if (targetDev.equals(XMLTools.getAttribute(mn, "dev"))) {
                            el = (Element) nodes.item(i);
                        }
                    }
                }
                return el;
            }
        };
    }

    /** Returns function that gets the node that belongs to the paremeters. */
    protected VirtualHardwareComparator getFilesystemDataComparator() {
        return new VirtualHardwareComparator() {
            @Override
            public Element getElement(final NodeList nodes, final Map<String, String> parameters) {
                Element el = null;
                final String targetDev = parameters.get(FilesystemData.SAVED_TARGET_DIR);
                if (targetDev != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = XMLTools.getChildNode(nodes.item(i), "target");
                        if (targetDev.equals(XMLTools.getAttribute(mn, "dir"))) {
                            el = (Element) nodes.item(i);
                        }
                    }
                }
                return el;
            }
        };
    }

    /** Returns function that gets the node that belongs to the paremeters. */
    protected VirtualHardwareComparator getInterfaceDataComparator() {
        return new VirtualHardwareComparator() {
            @Override
            public Element getElement(final NodeList nodes, final Map<String, String> parameters) {
                final String macAddress = parameters.get(InterfaceData.SAVED_MAC_ADDRESS);
                Element el = null;
                if (macAddress != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = XMLTools.getChildNode(nodes.item(i), "mac");
                        if (macAddress.equals(XMLTools.getAttribute(mn, "address"))) {
                            el = (Element) nodes.item(i);
                            break;
                        }
                    }
                }
                return el;
            }
        };
    }

    /** Returns function that gets the node that belongs to the paremeters. */
    protected VirtualHardwareComparator getInputDevDataComparator() {
        return new VirtualHardwareComparator() {
            @Override
            public Element getElement(final NodeList nodes, final Map<String, String> parameters) {
                final String type = parameters.get(InputDevData.SAVED_TYPE);
                final String bus = parameters.get(InputDevData.SAVED_BUS);
                Element el = null;
                if (type != null && bus != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = nodes.item(i);
                        if (type.equals(XMLTools.getAttribute(mn, "type")) && bus.equals(XMLTools.getAttribute(mn, "bus"))) {
                            el = (Element) nodes.item(i);
                            break;
                        }
                    }
                }
                return el;
            }
        };
    }

    /** Returns function that gets the node that belongs to the paremeters. */
    protected VirtualHardwareComparator getGraphicsDataComparator() {
        return new VirtualHardwareComparator() {
            @Override
            public Element getElement(final NodeList nodes, final Map<String, String> parameters) {
                final String type = parameters.get(GraphicsData.SAVED_TYPE);
                Element el = null;
                if (type != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = nodes.item(i);
                        if (type.equals(XMLTools.getAttribute(mn, "type"))) {
                            el = (Element) nodes.item(i);
                            break;
                        }
                    }
                }
                return el;
            }
        };
    }

    /** Returns function that gets the node that belongs to the paremeters. */
    protected VirtualHardwareComparator getSoundDataComparator() {
        return new VirtualHardwareComparator() {
            @Override
            public Element getElement(final NodeList nodes, final Map<String, String> parameters) {
                final String model = parameters.get(SoundData.SAVED_MODEL);
                Element el = null;
                if (model != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = nodes.item(i);
                        if (model.equals(XMLTools.getAttribute(mn, "model"))) {
                            el = (Element) nodes.item(i);
                            break;
                        }
                    }
                }
                return el;
            }
        };
    }

    /** Returns function that gets the node that belongs to the paremeters. */
    protected VirtualHardwareComparator getSerialDataComparator() {
        return new VirtualHardwareComparator() {
            @Override
            public Element getElement(final NodeList nodes, final Map<String, String> parameters) {
                final String type = parameters.get(SerialData.SAVED_TYPE);
                Element el = null;
                if (type != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = nodes.item(i);
                        if (type.equals(XMLTools.getAttribute(mn, "type"))) {
                            el = (Element) nodes.item(i);
                            break;
                        }
                    }
                }
                return el;
            }
        };
    }

    /** Returns function that gets the node that belongs to the paremeters. */
    protected VirtualHardwareComparator getParallelDataComparator() {
        return new VirtualHardwareComparator() {
            @Override
            public Element getElement(final NodeList nodes, final Map<String, String> parameters) {
                final String type = parameters.get(ParallelData.SAVED_TYPE);
                Element el = null;
                if (type != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = nodes.item(i);
                        if (type.equals(XMLTools.getAttribute(mn, "type"))) {
                            el = (Element) nodes.item(i);
                            break;
                        }
                    }
                }
                return el;
            }
        };
    }

    /** Returns function that gets the node that belongs to the paremeters. */
    protected VirtualHardwareComparator getVideoDataComparator() {
        return new VirtualHardwareComparator() {
            @Override
            public Element getElement(final NodeList nodes, final Map<String, String> parameters) {
                Element el = null;
                final String modelType = parameters.get(VideoData.SAVED_MODEL_TYPE);
                if (modelType != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = XMLTools.getChildNode(nodes.item(i), "model");
                        if (modelType.equals(XMLTools.getAttribute(mn, "type"))) {
                            el = (Element) nodes.item(i);
                        }
                    }
                }
                return el;
            }
        };
    }

    public Iterable<String> getSourceFileDirs() {
        return vmParser.getSourceFileDirs();
    }

    /** Return set of mac addresses. */
    public Collection<String> getUsedMacAddresses() {
        return vmParser.getUsedMacAddresses();
    }

    public String getConfig() {
        return oldConfig;
    }
}
