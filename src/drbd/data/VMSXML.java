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
import java.util.TreeSet;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
public final class VMSXML extends XML {
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
    /** Map from parameters to values. */
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
    /** Map from domain name and index to the input device data. */
    private final Map<String, Map<String, InputDevData>> inputDevsMap =
                       new LinkedHashMap<String, Map<String, InputDevData>>();
    /** Map from domain name and type to the graphics device data. */
    private final Map<String, Map<String, GraphicsData>> graphicsDevsMap =
                       new LinkedHashMap<String, Map<String, GraphicsData>>();
    /** Map from domain name and model to the sound device data. */
    private final Map<String, Map<String, SoundData>> soundsMap =
                       new LinkedHashMap<String, Map<String, SoundData>>();
    /** Map from domain name and type to the serial device data. */
    private final Map<String, Map<String, SerialData>> serialsMap =
                       new LinkedHashMap<String, Map<String, SerialData>>();
    /** Map from domain name and type to the parallel device data. */
    private final Map<String, Map<String, ParallelData>> parallelsMap =
                       new LinkedHashMap<String, Map<String, ParallelData>>();
    /** Map from domain name and model type to the video device data. */
    private final Map<String, Map<String, VideoData>> videosMap =
                       new LinkedHashMap<String, Map<String, VideoData>>();
    /** Map from domain name and network name to the network data. */
    private final Map<String, NetworkData> networkMap =
                                    new LinkedHashMap<String, NetworkData>();
    /** Directories where are source files. */
    private final Set<String> sourceFileDirs = new TreeSet<String>();
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
    /** VM field: type. */
    public static final String VM_PARAM_TYPE = "type";
    /** VM field: vcpu. */
    public static final String VM_PARAM_VCPU = "vcpu";
    /** VM field: currentMemory. */
    public static final String VM_PARAM_CURRENTMEMORY = "currentMemory";
    /** VM field: memory. */
    public static final String VM_PARAM_MEMORY = "memory";
    /** VM field: os-boot. */
    public static final String VM_PARAM_BOOT = "boot";
    /** VM field: loader. */
    public static final String VM_PARAM_LOADER = "loader";
    /** VM field: autostart. */
    public static final String VM_PARAM_AUTOSTART = "autostart";
    /** VM field: arch. */
    public static final String VM_PARAM_ARCH = "arch";
    /** VM field: acpi. */
    public static final String VM_PARAM_ACPI = "acpi";
    /** VM field: apic. */
    public static final String VM_PARAM_APIC = "apic";
    /** VM field: pae. */
    public static final String VM_PARAM_PAE = "pae";
    /** VM field: hap. */
    public static final String VM_PARAM_HAP = "hap";
    /** VM field: cpu match. */
    public static final String VM_PARAM_CPU_MATCH = "match";
    /** VM field: cpu model. */
    public static final String VM_PARAM_CPUMATCH_MODEL = "model";
    /** VM field: cpu vendor. */
    public static final String VM_PARAM_CPUMATCH_VENDOR = "vendor";
    /** VM field: cpu topology sockets. */
    public static final String VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS = "sockets";
    /** VM field: cpu topology cores. */
    public static final String VM_PARAM_CPUMATCH_TOPOLOGY_CORES = "cores";
    /** VM field: cpu topology threads. */
    public static final String VM_PARAM_CPUMATCH_TOPOLOGY_THREADS = "threads";
    /** VM field: cpu feature policy. */
    public static final String VM_PARAM_CPUMATCH_FEATURE_POLICY = "policy";
    /** VM field: cpu features. A space seperated list. */
    public static final String VM_PARAM_CPUMATCH_FEATURES = "features";
    /** VM field: on poweroff. */
    public static final String VM_PARAM_ON_POWEROFF = "on_poweroff";
    /** VM field: on reboot. */
    public static final String VM_PARAM_ON_REBOOT = "on_reboot";
    /** VM field: on crash. */
    public static final String VM_PARAM_ON_CRASH = "on_crash";
    /** VM field: emulator. */
    public static final String VM_PARAM_EMULATOR = "emulator";
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
    /** Map from paramater to its xml tag. */
    public static final Map<String, String> INPUTDEV_TAG_MAP =
                                             new HashMap<String, String>();
    /** Map from paramater to its xml attribute. */
    public static final Map<String, String> INPUTDEV_ATTRIBUTE_MAP =
                                             new HashMap<String, String>();

    /** Map from paramater to its xml tag. */
    public static final Map<String, String> GRAPHICS_TAG_MAP =
                                             new HashMap<String, String>();
    /** Map from paramater to its xml attribute. */
    public static final Map<String, String> GRAPHICS_ATTRIBUTE_MAP =
                                             new HashMap<String, String>();

    /** Map from paramater to its xml tag. */
    public static final Map<String, String> SOUND_TAG_MAP =
                                             new HashMap<String, String>();
    /** Map from paramater to its xml attribute. */
    public static final Map<String, String> SOUND_ATTRIBUTE_MAP =
                                             new HashMap<String, String>();

    /** Map from paramater to its xml tag. */
    public static final Map<String, String> SERIAL_TAG_MAP =
                                             new HashMap<String, String>();
    /** Map from paramater to its xml attribute. */
    public static final Map<String, String> SERIAL_ATTRIBUTE_MAP =
                                             new HashMap<String, String>();

    /** Map from paramater to its xml tag. */
    public static final Map<String, String> PARALLEL_TAG_MAP =
                                             new HashMap<String, String>();
    /** Map from paramater to its xml attribute. */
    public static final Map<String, String> PARALLEL_ATTRIBUTE_MAP =
                                             new HashMap<String, String>();

    /** Map from paramater to its xml tag. */
    public static final Map<String, String> VIDEO_TAG_MAP =
                                             new HashMap<String, String>();
    /** Map from paramater to its xml attribute. */
    public static final Map<String, String> VIDEO_ATTRIBUTE_MAP =
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

        INPUTDEV_ATTRIBUTE_MAP.put(InputDevData.TYPE, "type");
        INPUTDEV_ATTRIBUTE_MAP.put(InputDevData.BUS, "bus");

        GRAPHICS_ATTRIBUTE_MAP.put(GraphicsData.TYPE, "type");
        GRAPHICS_ATTRIBUTE_MAP.put(GraphicsData.PORT, "port");
        GRAPHICS_ATTRIBUTE_MAP.put(GraphicsData.AUTOPORT, "autoport");
        GRAPHICS_ATTRIBUTE_MAP.put(GraphicsData.LISTEN, "listen");
        GRAPHICS_ATTRIBUTE_MAP.put(GraphicsData.PASSWD, "passwd");
        GRAPHICS_ATTRIBUTE_MAP.put(GraphicsData.KEYMAP, "keymap");
        GRAPHICS_ATTRIBUTE_MAP.put(GraphicsData.DISPLAY, "display");
        GRAPHICS_ATTRIBUTE_MAP.put(GraphicsData.XAUTH, "xauth");

        SOUND_ATTRIBUTE_MAP.put(SoundData.MODEL, "model");

        SERIAL_ATTRIBUTE_MAP.put(SerialData.TYPE, "type");
        SERIAL_TAG_MAP.put(SerialData.SOURCE_PATH, "source");
        SERIAL_ATTRIBUTE_MAP.put(SerialData.SOURCE_PATH, "path");
        SERIAL_TAG_MAP.put(SerialData.BIND_SOURCE_MODE, "source");
        SERIAL_ATTRIBUTE_MAP.put(SerialData.BIND_SOURCE_MODE, "mode");
        SERIAL_TAG_MAP.put(SerialData.BIND_SOURCE_HOST, "source");
        SERIAL_ATTRIBUTE_MAP.put(SerialData.BIND_SOURCE_HOST, "host");
        SERIAL_TAG_MAP.put(SerialData.BIND_SOURCE_SERVICE, "source");
        SERIAL_ATTRIBUTE_MAP.put(SerialData.BIND_SOURCE_SERVICE, "service");
        SERIAL_TAG_MAP.put(SerialData.CONNECT_SOURCE_MODE, "source");
        SERIAL_ATTRIBUTE_MAP.put(SerialData.CONNECT_SOURCE_MODE, "mode");
        SERIAL_TAG_MAP.put(SerialData.CONNECT_SOURCE_HOST, "source");
        SERIAL_ATTRIBUTE_MAP.put(SerialData.CONNECT_SOURCE_HOST, "host");
        SERIAL_TAG_MAP.put(SerialData.CONNECT_SOURCE_SERVICE, "source");
        SERIAL_ATTRIBUTE_MAP.put(SerialData.CONNECT_SOURCE_SERVICE, "service");
        SERIAL_TAG_MAP.put(SerialData.PROTOCOL_TYPE, "protocol");
        SERIAL_ATTRIBUTE_MAP.put(SerialData.PROTOCOL_TYPE, "type");
        SERIAL_TAG_MAP.put(SerialData.TARGET_PORT, "target");
        SERIAL_ATTRIBUTE_MAP.put(SerialData.TARGET_PORT, "port");

        PARALLEL_ATTRIBUTE_MAP.put(ParallelData.TYPE, "type");
        PARALLEL_TAG_MAP.put(ParallelData.SOURCE_PATH, "source");
        PARALLEL_ATTRIBUTE_MAP.put(ParallelData.SOURCE_PATH, "path");
        PARALLEL_TAG_MAP.put(ParallelData.BIND_SOURCE_MODE, "source");
        PARALLEL_ATTRIBUTE_MAP.put(ParallelData.BIND_SOURCE_MODE, "mode");
        PARALLEL_TAG_MAP.put(ParallelData.BIND_SOURCE_HOST, "source");
        PARALLEL_ATTRIBUTE_MAP.put(ParallelData.BIND_SOURCE_HOST, "host");
        PARALLEL_TAG_MAP.put(ParallelData.BIND_SOURCE_SERVICE, "source");
        PARALLEL_ATTRIBUTE_MAP.put(ParallelData.BIND_SOURCE_SERVICE, "service");

        PARALLEL_TAG_MAP.put(ParallelData.CONNECT_SOURCE_MODE, "source");
        PARALLEL_ATTRIBUTE_MAP.put(ParallelData.CONNECT_SOURCE_MODE, "mode");
        PARALLEL_TAG_MAP.put(ParallelData.CONNECT_SOURCE_HOST, "source");
        PARALLEL_ATTRIBUTE_MAP.put(ParallelData.CONNECT_SOURCE_HOST, "host");
        PARALLEL_TAG_MAP.put(ParallelData.CONNECT_SOURCE_SERVICE, "source");
        PARALLEL_ATTRIBUTE_MAP.put(ParallelData.CONNECT_SOURCE_SERVICE,
                                   "service");

        PARALLEL_TAG_MAP.put(ParallelData.PROTOCOL_TYPE, "protocol");
        PARALLEL_ATTRIBUTE_MAP.put(ParallelData.PROTOCOL_TYPE, "type");
        PARALLEL_TAG_MAP.put(ParallelData.TARGET_PORT, "target");
        PARALLEL_ATTRIBUTE_MAP.put(ParallelData.TARGET_PORT, "port");

        VIDEO_TAG_MAP.put(VideoData.MODEL_TYPE, "model");
        VIDEO_ATTRIBUTE_MAP.put(VideoData.MODEL_TYPE, "type");
        VIDEO_TAG_MAP.put(VideoData.MODEL_VRAM, "model");
        VIDEO_ATTRIBUTE_MAP.put(VideoData.MODEL_VRAM, "vram");
        VIDEO_TAG_MAP.put(VideoData.MODEL_HEADS, "model");
        VIDEO_ATTRIBUTE_MAP.put(VideoData.MODEL_HEADS, "heads");
    }

    /** XML document lock. */
    private final Mutex mXMLDocumentLock = new Mutex();
    /** XML document. */
    private Document xmlDocument = null;

    /** Prepares a new <code>VMSXML</code> object. */
    public VMSXML(final Host host) {
        super();
        this.host = host;
    }

    /** Returns xml node of the specified domain. */
    public Node getDomainNode(final String domainName) {
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
    private void saveDomainXML(final String configName,
                               final Node node,
                               final String defineCommand) {
        String xml = null;
        try {
            final Transformer transformer =
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
            host.getSSH().scp(xml,
                              configName,
                              "0600",
                              true,
                              defineCommand,
                              null,
                              null);
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

    /** Add CPU match node. */
    private void addCPUMatchNode(final Document doc,
                                 final Node root,
                                 final Map<String, String> parametersMap) {
        final String cpuMatch = parametersMap.get(VM_PARAM_CPU_MATCH);
        if (!"".equals(cpuMatch)) {
            final Element cpuMatchNode = (Element) root.appendChild(
                                               doc.createElement("cpu"));
            cpuMatchNode.setAttribute("match", cpuMatch);
            final String model = parametersMap.get(VM_PARAM_CPUMATCH_MODEL);
            if (!"".equals(model)) {
                final Node modelNode = (Element) cpuMatchNode.appendChild(
                                                  doc.createElement("model"));
                modelNode.appendChild(doc.createTextNode(model));
            }
            final String vendor = parametersMap.get(VM_PARAM_CPUMATCH_VENDOR);
            if (!"".equals(vendor)) {
                final Node vendorNode = (Element) cpuMatchNode.appendChild(
                                                  doc.createElement("vendor"));
                vendorNode.appendChild(doc.createTextNode(vendor));
            }
            final String sockets = parametersMap.get(
                                           VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS);
            final String cores = parametersMap.get(
                                           VM_PARAM_CPUMATCH_TOPOLOGY_CORES);
            final String threads = parametersMap.get(
                                           VM_PARAM_CPUMATCH_TOPOLOGY_THREADS);
            final boolean isSockets = !"".equals(sockets);
            final boolean isCores =   !"".equals(cores);
            final boolean isThreads = !"".equals(threads);
            if (isSockets || isCores || isThreads) {
                final Element topologyNode = (Element) cpuMatchNode.appendChild(
                                              doc.createElement("topology"));
                if (isSockets) {
                    topologyNode.setAttribute("sockets", sockets);
                }
                if (isCores) {
                    topologyNode.setAttribute("cores", cores);
                }
                if (isThreads) {
                    topologyNode.setAttribute("threads", threads);
                }
            }
            final String policy = parametersMap.get(
                                           VM_PARAM_CPUMATCH_FEATURE_POLICY);
            final String features = parametersMap.get(
                                           VM_PARAM_CPUMATCH_FEATURES);
            if (!"".equals(policy) && !"".equals(features)) {
                for (final String feature : features.split("\\s+")) {
                    final Element featureNode =
                                      (Element) cpuMatchNode.appendChild(
                                                 doc.createElement("feature"));
                    featureNode.setAttribute("policy", policy);
                    featureNode.setAttribute("name", feature);
                }
            }
        }
    }

    /** Add features. */
    private void addFeatures(final Document doc,
                             final Node root,
                             final Map<String, String> parametersMap) {
        final boolean acpi = "True".equals(parametersMap.get(VM_PARAM_ACPI));
        final boolean apic = "True".equals(parametersMap.get(VM_PARAM_APIC));
        final boolean pae = "True".equals(parametersMap.get(VM_PARAM_PAE));
        final boolean hap = "True".equals(parametersMap.get(VM_PARAM_HAP));
        if (acpi || apic || pae || hap) {
            final Element featuresNode = (Element) root.appendChild(
                                                doc.createElement("features"));
            if (acpi) {
                featuresNode.appendChild(doc.createElement("acpi"));
            }
            if (apic) {
                featuresNode.appendChild(doc.createElement("apic"));
            }
            if (pae) {
                featuresNode.appendChild(doc.createElement("pae"));
            }
            if (hap) {
                featuresNode.appendChild(doc.createElement("hap"));
            }
        }
    }

    /** Creates XML for new domain. */
    public Node createDomainXML(final String domainName,
                                final Map<String, String> parametersMap) {
        //<domain type='kvm'>
        //  <memory>524288</memory>
        //  <name>fff</name>
        //  <os>
        //    <type arch='i686' machine='pc-0.12'>hvm</type>
        //  </os>
        //</domain>

        final String type = parametersMap.get(VM_PARAM_TYPE); /* kvm/xen */
        String configName = "/etc/libvirt/qemu/" + domainName + ".xml";
        if ("xen".equals(type)) {
            configName = "/etc/xen/vm/" + domainName + ".xml";
        }
        namesConfigsMap.put(domainName, configName);
        /* build xml */
        final String encoding = "UTF-8";
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;

        try {
             db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
             assert false;
        }
        final Document doc = db.newDocument();
        final Element root = (Element) doc.appendChild(
                                                  doc.createElement("domain"));
        /* type */
        root.setAttribute("type", type); /* kvm/xen */
        /* name */
        final Node nameNode = (Element) root.appendChild(
                                                    doc.createElement("name"));
        nameNode.appendChild(doc.createTextNode(domainName));
        /* memory */
        final Node memoryNode = (Element) root.appendChild(
                                                  doc.createElement("memory"));
        final long mem = Tools.convertToKilobytes(
                                           parametersMap.get(VM_PARAM_MEMORY));
        memoryNode.appendChild(doc.createTextNode(Long.toString(mem)));
        /* current memory */
        final Node curMemoryNode = (Element) root.appendChild(
                                           doc.createElement("currentMemory"));
        final long curMem = Tools.convertToKilobytes(
                                    parametersMap.get(VM_PARAM_CURRENTMEMORY));
        curMemoryNode.appendChild(doc.createTextNode(Long.toString(curMem)));
        /* vcpu */
        final String vcpu = parametersMap.get(VM_PARAM_VCPU);
        if (vcpu != null) {
            final Node vcpuNode = (Element) root.appendChild(
                                                   doc.createElement("vcpu"));
            vcpuNode.appendChild(doc.createTextNode(vcpu));
        }

        /* os */
        final Element osNode = (Element) root.appendChild(
                                                  doc.createElement("os"));
        final Element typeNode = (Element) osNode.appendChild(
                                                  doc.createElement("type"));
        typeNode.setAttribute("arch", parametersMap.get(VM_PARAM_ARCH));
        typeNode.setAttribute("machine", "pc-0.12");
        typeNode.appendChild(doc.createTextNode("hvm"));
        final Element bootNode = (Element) osNode.appendChild(
                                                  doc.createElement("boot"));
        final Node loaderNode = (Element) osNode.appendChild(
                                                  doc.createElement("loader"));
        loaderNode.appendChild(doc.createTextNode(
                                          parametersMap.get(VM_PARAM_LOADER)));
        bootNode.setAttribute("dev", parametersMap.get(VM_PARAM_BOOT));

        /* features */
        addFeatures(doc, root, parametersMap);

        /* cpu match */
        addCPUMatchNode(doc, root, parametersMap);

        /* on_ */
        final String onPoweroff = parametersMap.get(VM_PARAM_ON_POWEROFF);
        if (onPoweroff != null) {
            final Element onPoweroffNode = (Element) root.appendChild(
                                              doc.createElement("on_poweroff"));
            onPoweroffNode.appendChild(doc.createTextNode(onPoweroff));

        }
        final String onReboot = parametersMap.get(VM_PARAM_ON_REBOOT);
        if (onReboot != null) {
            final Element onRebootNode = (Element) root.appendChild(
                                              doc.createElement("on_reboot"));
            onRebootNode.appendChild(doc.createTextNode(onReboot));

        }
        final String onCrash = parametersMap.get(VM_PARAM_ON_CRASH);
        if (onCrash != null) {
            final Element onCrashNode = (Element) root.appendChild(
                                              doc.createElement("on_crash"));
            onCrashNode.appendChild(doc.createTextNode(onCrash));

        }
        /* devices / emulator */
        final String emulator = parametersMap.get(VM_PARAM_EMULATOR);
        if (emulator != null) {
            final Element devicesNode = (Element) root.appendChild(
                                              doc.createElement("devices"));
            final Element emulatorNode = (Element) devicesNode.appendChild(
                                              doc.createElement("emulator"));
            emulatorNode.appendChild(doc.createTextNode(emulator));
        }
        return root;
    }

    /** Modify xml of the domain. */
    public Node modifyDomainXML(final String domainName,
                                final Map<String, String> parametersMap) {
        final String configName = namesConfigsMap.get(domainName);
        if (configName == null) {
            return null;
        }
        final Node domainNode = getDomainNode(domainName);
        if (domainNode == null) {
            return null;
        }
        final XPath xpath = XPathFactory.newInstance().newXPath();
        final Map<String, String> paths = new HashMap<String, String>();
        paths.put(VM_PARAM_MEMORY, "memory");
        paths.put(VM_PARAM_CURRENTMEMORY, "currentMemory");
        paths.put(VM_PARAM_VCPU, "vcpu");
        paths.put(VM_PARAM_BOOT, "os/boot");
        paths.put(VM_PARAM_ARCH, "os/type");
        paths.put(VM_PARAM_LOADER, "os/loader");
        paths.put(VM_PARAM_CPU_MATCH, "cpu");
        paths.put(VM_PARAM_ACPI, "features");
        paths.put(VM_PARAM_APIC, "features");
        paths.put(VM_PARAM_PAE, "features");
        paths.put(VM_PARAM_HAP, "features");
        paths.put(VM_PARAM_ON_POWEROFF, "on_poweroff");
        paths.put(VM_PARAM_ON_REBOOT, "on_reboot");
        paths.put(VM_PARAM_ON_CRASH, "on_crash");
        paths.put(VM_PARAM_EMULATOR, "devices/emulator");
        try {
            for (final String param : parametersMap.keySet()) {
                final String path = paths.get(param);
                if (path == null) {
                    continue;
                }
                final NodeList nodes = (NodeList) xpath.evaluate(
                                                   path,
                                                   domainNode,
                                                   XPathConstants.NODESET);
                final Element node = (Element) nodes.item(0);
                if (node == null) {
                    continue;
                }
                String value = parametersMap.get(param);
                if (VM_PARAM_MEMORY.equals(param)
                    || VM_PARAM_CURRENTMEMORY.equals(param)) {
                    value = Long.toString(
                                        Tools.convertToKilobytes(value));
                }
                if (VM_PARAM_CPU_MATCH.equals(param)
                    || VM_PARAM_ACPI.equals(param)
                    || VM_PARAM_APIC.equals(param)
                    || VM_PARAM_PAE.equals(param)
                    || VM_PARAM_HAP.equals(param)) {
                    domainNode.removeChild(node);
                } else if (VM_PARAM_BOOT.equals(param)) {
                    node.setAttribute("dev", value);
                } else if (VM_PARAM_ARCH.equals(param)) {
                    node.setAttribute("arch", value);
                } else if (VM_PARAM_CPU_MATCH.equals(param)) {
                    if ("".equals(value)) {
                        node.getParentNode().removeChild(node);
                    } else {
                        node.setAttribute("match", value);
                    }
                } else if (VM_PARAM_CPUMATCH_TOPOLOGY_THREADS.equals(
                                                                  param)) {
                    node.setAttribute("threads", value);
                } else {
                    getChildNode(node, "#text").setNodeValue(value);
                }
            }
            addCPUMatchNode(domainNode.getOwnerDocument(),
                            domainNode,
                            parametersMap);
            addFeatures(domainNode.getOwnerDocument(),
                        domainNode,
                        parametersMap);
        } catch (final javax.xml.xpath.XPathExpressionException e) {
            Tools.appError("could not evaluate: ", e);
            return null;
        }
        return domainNode;
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
        final String configName = namesConfigsMap.get(domainName);
        if (configName == null) {
            return;
        }
        //final Node domainNode = getDomainNode(domainName);
        if (domainNode == null) {
            return;
        }
        final XPath xpath = XPathFactory.newInstance().newXPath();
        final Node devicesNode = getDevicesNode(xpath, domainNode);
        if (devicesNode == null) {
            return;
        }
        try {
            final NodeList nodes = (NodeList) xpath.evaluate(
                                                       path,
                                                       domainNode,
                                                       XPathConstants.NODESET);
            Element hwNode = vhc.getElement(nodes, parametersMap);
            if (hwNode == null) {
                hwNode = (Element) devicesNode.appendChild(
                     domainNode.getOwnerDocument().createElement(elementName));
            }
            for (final String param : parametersMap.keySet()) {
                final String value = parametersMap.get(param);
                if (!tagMap.containsKey(param)
                    && attributeMap.containsKey(param)) {
                    /* attribute */
                    final Node attributeNode =
                         hwNode.getAttributes().getNamedItem(
                                                    attributeMap.get(param));
                    if (attributeNode == null) {
                        hwNode.setAttribute(attributeMap.get(param), value);
                    } else {
                        attributeNode.setNodeValue(value);
                    }
                    continue;
                }
                Element node = (Element) getChildNode(hwNode,
                                                      tagMap.get(param));
                if ((attributeMap.containsKey(param) || "True".equals(value))
                    && node == null) {
                    node = (Element) hwNode.appendChild(
                          domainNode.getOwnerDocument().createElement(
                                                           tagMap.get(param)));
                } else if (!attributeMap.containsKey(param)
                           && "False".equals(value)
                           && node != null) {
                    hwNode.removeChild(node);
                }
                if (attributeMap.containsKey(param)) {
                    final Node attributeNode =
                                    node.getAttributes().getNamedItem(
                                                      attributeMap.get(param));
                    if (attributeNode == null) {
                        node.setAttribute(attributeMap.get(param), value);

                    } else {
                        if ("".equals(value)) {
                            node.removeAttribute(attributeMap.get(param));
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
    }

    /** Remove XML from some device. */
    private void removeXML(final String domainName,
                           final Map<String, String> parametersMap,
                           final String path,
                           final VirtualHardwareComparator vhc) {
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
            final NodeList nodes = (NodeList) xpath.evaluate(
                                                       path,
                                                       domainNode,
                                                       XPathConstants.NODESET);
            final Element hwNode = vhc.getElement(nodes, parametersMap);
            if (hwNode != null) {
                hwNode.getParentNode().removeChild(hwNode);
            }
        } catch (final javax.xml.xpath.XPathExpressionException e) {
            Tools.appError("could not evaluate: ", e);
            return;
        }
        saveAndDefine(domainNode, domainName);
    }

    /** Modify disk XML. */
    public void modifyDiskXML(final Node domainNode,
                              final String domainName,
                              final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  DISK_TAG_MAP,
                  DISK_ATTRIBUTE_MAP,
                  parametersMap,
                  "devices/disk",
                  "disk",
                  getDiskDataComparator());
    }

    /** Save and define. */
    public void saveAndDefine(final Node domainNode,
                              final String domainName) {
        final String configName = namesConfigsMap.get(domainName);
        final String defineCommand =
                            VIRSH.getDefineCommand(host, configName + ".new"
                            + " && rm " + configName + ".new");
        saveDomainXML(configName, domainNode, defineCommand);
        host.setVMInfoMD5(null);
    }

    /** Modify interface XML. */
    public void modifyInterfaceXML(final Node domainNode,
                                  final String domainName,
                                  final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  INTERFACE_TAG_MAP,
                  INTERFACE_ATTRIBUTE_MAP,
                  parametersMap,
                  "devices/interface",
                  "interface",
                  getInterfaceDataComparator());
    }

    /** Modify input device XML. */
    public void modifyInputDevXML(final Node domainNode,
                                  final String domainName,
                                  final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  INPUTDEV_TAG_MAP,
                  INPUTDEV_ATTRIBUTE_MAP,
                  parametersMap,
                  "devices/input",
                  "input",
                  getInputDevDataComparator());
    }

    /** Modify graphics device XML. */
    public void modifyGraphicsXML(final Node domainNode,
                                  final String domainName,
                                  final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  GRAPHICS_TAG_MAP,
                  GRAPHICS_ATTRIBUTE_MAP,
                  parametersMap,
                  "devices/graphics",
                  "graphics",
                  getGraphicsDataComparator());
    }

    /** Modify sound device XML. */
    public void modifySoundXML(final Node domainNode,
                               final String domainName,
                               final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  SOUND_TAG_MAP,
                  SOUND_ATTRIBUTE_MAP,
                  parametersMap,
                  "devices/sound",
                  "sound",
                  getSoundDataComparator());
    }

    /** Modify serial device XML. */
    public void modifySerialXML(final Node domainNode,
                                final String domainName,
                                final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  SERIAL_TAG_MAP,
                  SERIAL_ATTRIBUTE_MAP,
                  parametersMap,
                  "devices/serial",
                  "serial",
                  getSerialDataComparator());
    }

    /** Modify parallel device XML. */
    public void modifyParallelXML(final Node domainNode,
                                  final String domainName,
                                  final Map<String, String> parametersMap) {
        modifyXML(domainNode,
                  domainName,
                  PARALLEL_TAG_MAP,
                  PARALLEL_ATTRIBUTE_MAP,
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
                  VIDEO_TAG_MAP,
                  VIDEO_ATTRIBUTE_MAP,
                  parametersMap,
                  "devices/video",
                  "video",
                  getVideoDataComparator());
    }

    /** Remove disk XML. */
    public void removeDiskXML(final String domainName,
                              final Map<String, String> parametersMap) {
        removeXML(domainName,
                  parametersMap,
                  "devices/disk",
                  getDiskDataComparator());
    }

    /** Remove interface XML. */
    public void removeInterfaceXML(final String domainName,
                                   final Map<String, String> parametersMap) {
        removeXML(domainName,
                  parametersMap,
                  "devices/interface",
                  getInterfaceDataComparator());
    }

    /** Remove input device XML. */
    public void removeInputDevXML(final String domainName,
                                  final Map<String, String> parametersMap) {
        removeXML(domainName,
                  parametersMap,
                  "devices/input",
                  getInputDevDataComparator());
    }

    /** Remove graphics device XML. */
    public void removeGraphicsXML(final String domainName,
                                  final Map<String, String> parametersMap) {
        removeXML(domainName,
                  parametersMap,
                  "devices/graphics",
                  getGraphicsDataComparator());
    }

    /** Remove sound device XML. */
    public void removeSoundXML(final String domainName,
                               final Map<String, String> parametersMap) {
        removeXML(domainName,
                  parametersMap,
                  "devices/sound",
                  getSoundDataComparator());
    }

    /** Remove serial device XML. */
    public void removeSerialXML(final String domainName,
                                final Map<String, String> parametersMap) {
        removeXML(domainName,
                  parametersMap,
                  "devices/serial",
                  getSerialDataComparator());
    }

    /** Remove parallel device XML. */
    public void removeParallelXML(final String domainName,
                                  final Map<String, String> parametersMap) {
        removeXML(domainName,
                  parametersMap,
                  "devices/parallel",
                  getParallelDataComparator());
    }

    /** Remove parallel device XML. */
    public void removeVideoXML(final String domainName,
                               final Map<String, String> parametersMap) {
        removeXML(domainName,
                  parametersMap,
                  "devices/video",
                  getVideoDataComparator());
    }

    /** Updates data. */
    public boolean update() {
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
            } else if ("vm".equals(node.getNodeName())) {
                updateVM(node);
            } else if ("version".equals(node.getNodeName())) {
                host.setLibvirtVersion(getText(node));
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
        final String domainType = getAttribute(domainNode, "type");
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
                parameterValues.put(name, VM_PARAM_NAME, name);
                parameterValues.put(name, VM_PARAM_TYPE, domainType);
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
                    } else if ("type".equals(osOption.getNodeName())) {
                        parameterValues.put(name,
                                            VM_PARAM_ARCH,
                                            getAttribute(osOption, "arch"));
                    } else {
                        parameterValues.put(name,
                                            osOption.getNodeName(),
                                            getText(osOption));
                    }
                }
            } else if ("features".equals(option.getNodeName())) {
                final NodeList ftrOptions = option.getChildNodes();
                for (int j = 0; j < ftrOptions.getLength(); j++) {
                    final Node ftrOption = ftrOptions.item(j);
                    if (VM_PARAM_ACPI.equals(ftrOption.getNodeName())) {
                        parameterValues.put(name, VM_PARAM_ACPI, "True");
                    } else if (VM_PARAM_APIC.equals(ftrOption.getNodeName())) {
                        parameterValues.put(name, VM_PARAM_APIC, "True");
                    } else if (VM_PARAM_PAE.equals(ftrOption.getNodeName())) {
                        parameterValues.put(name, VM_PARAM_PAE, "True");
                    } else if (VM_PARAM_HAP.equals(ftrOption.getNodeName())) {
                        parameterValues.put(name, VM_PARAM_HAP, "True");
                    }
                }
            } else if ("cpu".equals(option.getNodeName())) {
                final String match = getAttribute(option, "match");
                if (!"".equals(match)) {
                    parameterValues.put(name, VM_PARAM_CPU_MATCH, match);
                    final NodeList cpuMatchOptions = option.getChildNodes();
                    String policy = "";
                    final List<String> features = new ArrayList<String>();
                    for (int j = 0; j < cpuMatchOptions.getLength(); j++) {
                        final Node cpuMatchOption = cpuMatchOptions.item(j);
                        final String op = cpuMatchOption.getNodeName();
                        if ("topology".equals(op)) {
                            parameterValues.put(
                                    name,
                                    VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                                    getAttribute(
                                          cpuMatchOption,
                                          VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS));
                            parameterValues.put(
                                    name,
                                    VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                                    getAttribute(
                                          cpuMatchOption,
                                          VM_PARAM_CPUMATCH_TOPOLOGY_CORES));
                            parameterValues.put(
                                    name,
                                    VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                                    getAttribute(
                                          cpuMatchOption,
                                          VM_PARAM_CPUMATCH_TOPOLOGY_THREADS));
                        } else if ("feature".equals(op)) {
                            /* asuming the same policy for all */
                            policy = getAttribute(
                                          cpuMatchOption,
                                          VM_PARAM_CPUMATCH_FEATURE_POLICY);
                            features.add(getAttribute(cpuMatchOption, "name"));
                        } else {
                            parameterValues.put(name,
                                                op,
                                                getText(cpuMatchOption));
                        }
                    }
                    if (!"".equals(policy) && !features.isEmpty()) {
                        parameterValues.put(name,
                                            VM_PARAM_CPUMATCH_FEATURE_POLICY,
                                            policy);
                        parameterValues.put(name,
                                            VM_PARAM_CPUMATCH_FEATURES,
                                            Tools.join(" ", features));
                    }
                }
            } else if (VM_PARAM_ON_POWEROFF.equals(option.getNodeName())) {
                parameterValues.put(name,
                                    VM_PARAM_ON_POWEROFF,
                                    getText(option));
            } else if (VM_PARAM_ON_REBOOT.equals(option.getNodeName())) {
                parameterValues.put(name, VM_PARAM_ON_REBOOT, getText(option));
            } else if (VM_PARAM_ON_CRASH.equals(option.getNodeName())) {
                parameterValues.put(name, VM_PARAM_ON_CRASH, getText(option));
            } else if ("devices".equals(option.getNodeName())) {
                final Map<String, DiskData> devMap =
                                    new LinkedHashMap<String, DiskData>();
                final Map<String, InterfaceData> macMap =
                                    new LinkedHashMap<String, InterfaceData>();
                final Map<String, InputDevData> inputMap =
                                    new LinkedHashMap<String, InputDevData>();
                final Map<String, GraphicsData> graphicsMap =
                                    new LinkedHashMap<String, GraphicsData>();
                final Map<String, SoundData> soundMap =
                                    new LinkedHashMap<String, SoundData>();
                final Map<String, SerialData> serialMap =
                                    new LinkedHashMap<String, SerialData>();
                final Map<String, ParallelData> parallelMap =
                                    new LinkedHashMap<String, ParallelData>();
                final Map<String, VideoData> videoMap =
                                    new LinkedHashMap<String, VideoData>();
                final NodeList devices = option.getChildNodes();
                for (int j = 0; j < devices.getLength(); j++) {
                    final Node deviceNode = devices.item(j);
                    if ("emulator".equals(deviceNode.getNodeName())) {
                        parameterValues.put(name,
                                            VM_PARAM_EMULATOR,
                                            getText(deviceNode));
                    } else if ("input".equals(deviceNode.getNodeName())) {
                        final String type = getAttribute(deviceNode, "type");
                        final String bus = getAttribute(deviceNode, "bus");
                        if ("tablet".equals(type)) {
                            tabletOk = true;
                        }
                        final InputDevData inputDevData =
                                                 new InputDevData(type, bus);
                        inputMap.put(type + " : " + bus,
                                     inputDevData);
                    } else if ("graphics".equals(deviceNode.getNodeName())) {
                        /** remotePort will be overwritten with virsh output */
                        final String type = getAttribute(deviceNode, "type");
                        final String port = getAttribute(deviceNode, "port");
                        final String autoport = getAttribute(deviceNode,
                                                             "autoport");
                        final String listen = getAttribute(deviceNode,
                                                           "listen");
                        final String passwd = getAttribute(deviceNode,
                                                           "passwd");
                        final String keymap = getAttribute(deviceNode,
                                                           "keymap");
                        final String display = getAttribute(deviceNode,
                                                           "display");
                        final String xauth = getAttribute(deviceNode, "xauth");
                        Tools.debug(this, "type: " + type, 2);
                        Tools.debug(this, "port: " + port, 2);
                        Tools.debug(this, "autoport: " + autoport, 2);
                        if ("vnc".equals(type)) {
                            if (port != null && Tools.isNumber(port)) {
                                remotePorts.put(name, Integer.parseInt(port));
                            }
                            if ("yes".equals(autoport)) {
                                autoports.put(name, true);
                            } else {
                                autoports.put(name, false);
                            }
                        }
                        final GraphicsData graphicsData =
                                     new GraphicsData(type,
                                                      port,
                                                      listen,
                                                      passwd,
                                                      keymap,
                                                      display,
                                                      xauth);
                        graphicsMap.put(graphicsDisplayName(type,
                                                            port,
                                                            display),
                                        graphicsData);
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
                                final String dir = Tools.getDirectoryPart(
                                                                  sourceFile);
                                if (dir != null) {
                                    sourceFileDirs.add(dir);
                                }
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
                            } else if ("address".equals(nodeName)) {
                                /* it's generated, ignoring. */
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
                            } else if ("address".equals(nodeName)) {
                                /* it's generated, ignoring. */
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
                    } else if ("sound".equals(deviceNode.getNodeName())) {
                        final String model = getAttribute(deviceNode, "model");
                        final NodeList opts = deviceNode.getChildNodes();
                        for (int k = 0; k < opts.getLength(); k++) {
                            final Node optionNode = opts.item(k);
                            final String nodeName = optionNode.getNodeName();
                            if ("address".equals(nodeName)) {
                                /* it's generated, ignoring. */
                            } else if (!"#text".equals(nodeName)) {
                                Tools.appWarning("unknown sound option: "
                                                 + nodeName);
                            }
                        }
                        if (model != null) {
                            final SoundData soundData = new SoundData(model);
                            soundMap.put(model, soundData);
                        }
                    } else if ("serial".equals(deviceNode.getNodeName())) {
                        final String type = getAttribute(deviceNode, "type");
                        final NodeList opts = deviceNode.getChildNodes();
                        String sourcePath = null;
                        String bindSourceMode = null;
                        String bindSourceHost = null;
                        String bindSourceService = null;
                        String connectSourceMode = null;
                        String connectSourceHost = null;
                        String connectSourceService = null;
                        String protocolType = null;
                        String targetPort = null;
                        for (int k = 0; k < opts.getLength(); k++) {
                            final Node optionNode = opts.item(k);
                            final String nodeName = optionNode.getNodeName();
                            if ("source".equals(nodeName)) {
                                sourcePath = getAttribute(optionNode, "path");
                                final String sourceMode =
                                               getAttribute(optionNode, "mode");
                                if ("bind".equals(sourceMode)) {
                                    bindSourceMode = sourceMode;
                                    bindSourceHost = getAttribute(optionNode,
                                                                  "host");
                                    bindSourceService = getAttribute(optionNode,
                                                                     "service");
                                } else if ("connect".equals(sourceMode)) {
                                    connectSourceMode = sourceMode;
                                    connectSourceHost = getAttribute(optionNode,
                                                                    "host");
                                    connectSourceService =
                                                       getAttribute(optionNode,
                                                                    "service");
                                } else {
                                    Tools.appWarning("uknown source mode: "
                                                     + sourceMode);
                                }
                            } else if ("protocol".equals(nodeName)) {
                                protocolType = getAttribute(optionNode, "type");
                            } else if ("target".equals(nodeName)) {
                                targetPort = getAttribute(optionNode, "port");
                            } else if ("address".equals(nodeName)) {
                                /* it's generated, ignoring. */
                            } else if (!"#text".equals(nodeName)) {
                                Tools.appWarning("unknown serial option: "
                                                 + nodeName);
                            }
                        }
                        if (type != null) {
                            final SerialData serialData =
                                           new SerialData(type,
                                                          sourcePath,
                                                          bindSourceMode,
                                                          bindSourceHost,
                                                          bindSourceService,
                                                          connectSourceMode,
                                                          connectSourceHost,
                                                          connectSourceService,
                                                          protocolType,
                                                          targetPort);
                            serialMap.put("serial "
                                          + targetPort
                                          + " / "
                                          + type,
                                          serialData);
                        }
                    } else if ("parallel".equals(deviceNode.getNodeName())) {
                        final String type = getAttribute(deviceNode, "type");
                        final NodeList opts = deviceNode.getChildNodes();
                        String sourcePath = null;
                        String bindSourceMode = null;
                        String bindSourceHost = null;
                        String bindSourceService = null;
                        String connectSourceMode = null;
                        String connectSourceHost = null;
                        String connectSourceService = null;
                        String protocolType = null;
                        String targetPort = null;
                        for (int k = 0; k < opts.getLength(); k++) {
                            final Node optionNode = opts.item(k);
                            final String nodeName = optionNode.getNodeName();
                            if ("source".equals(nodeName)) {
                                sourcePath = getAttribute(optionNode, "path");
                                final String sourceMode =
                                            getAttribute(optionNode, "mode");
                                if ("bind".equals(sourceMode)) {
                                    bindSourceMode = sourceMode;
                                    bindSourceHost =
                                           getAttribute(optionNode, "host");
                                    bindSourceService =
                                           getAttribute(optionNode, "service");
                                } else if ("connect".equals(sourceMode)) {
                                    connectSourceMode = sourceMode;
                                    connectSourceHost =
                                           getAttribute(optionNode, "host");
                                    connectSourceService =
                                           getAttribute(optionNode, "service");
                                } else {
                                    Tools.appWarning("uknown source mode: "
                                                     + sourceMode);
                                }
                            } else if ("protocol".equals(nodeName)) {
                                protocolType = getAttribute(optionNode, "type");
                            } else if ("target".equals(nodeName)) {
                                targetPort = getAttribute(optionNode, "port");
                            } else if ("address".equals(nodeName)) {
                                /* it's generated, ignoring. */
                            } else if (!"#text".equals(nodeName)) {
                                Tools.appWarning("unknown parallel option: "
                                                 + nodeName);
                            }
                        }
                        if (type != null) {
                            final ParallelData parallelData =
                                         new ParallelData(type,
                                                          sourcePath,
                                                          bindSourceMode,
                                                          bindSourceHost,
                                                          bindSourceService,
                                                          connectSourceMode,
                                                          connectSourceHost,
                                                          connectSourceService,
                                                          protocolType,
                                                          targetPort);
                            parallelMap.put("parallel "
                                            + targetPort
                                            + " / "
                                            + type,
                                            parallelData);
                        }
                    } else if ("video".equals(deviceNode.getNodeName())) {
                        final NodeList opts = deviceNode.getChildNodes();
                        String modelType = null;
                        String modelVRAM = null;
                        String modelHeads = null;
                        for (int k = 0; k < opts.getLength(); k++) {
                            final Node optionNode = opts.item(k);
                            final String nodeName = optionNode.getNodeName();
                            if ("model".equals(nodeName)) {
                                modelType = getAttribute(optionNode, "type");
                                modelVRAM = getAttribute(optionNode, "vram");
                                modelHeads = getAttribute(optionNode, "heads");
                            } else if ("address".equals(nodeName)) {
                                /* it's generated, ignoring. */
                            } else if (!"#text".equals(nodeName)) {
                                Tools.appWarning("unknown video option: "
                                                 + nodeName);
                            }
                        }
                        if (modelType != null) {
                            final VideoData videoData =
                                                 new VideoData(modelType,
                                                               modelVRAM,
                                                               modelHeads);
                            videoMap.put(modelType, videoData);
                        }
                    } else if ("controller".equals(deviceNode.getNodeName())) {
                        /* it's generated, ignore */
                    } else if ("memballoon".equals(deviceNode.getNodeName())) {
                        /* it's generated, ignore */
                    } else if (!"#text".equals(deviceNode.getNodeName())) {
                        Tools.appWarning("unknown device: "
                                         + deviceNode.getNodeName());

                    }
                }
                disksMap.put(name, devMap);
                interfacesMap.put(name, macMap);
                inputDevsMap.put(name, inputMap);
                graphicsDevsMap.put(name, graphicsMap);
                soundsMap.put(name, soundMap);
                serialsMap.put(name, serialMap);
                parallelsMap.put(name, parallelMap);
                videosMap.put(name, videoMap);
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
        //parseConfig(getXMLDocument(
        //                    getCDATA(getChildNode(vmNode, "config-in-etc"))),
        //            name);
        parseConfig(getChildNode(vmNode, "config"), name);
    }

    /** Updates all data for this domain. */
    void parseInfo(final String name, final String info) {
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

    /** Returns all domain names. */
    public List<String> getDomainNames() {
        return domainNames;
    }

    /** Returns whether the domain is running. */
    public boolean isRunning(final String name) {
        final Boolean r = runningMap.get(name);
        if (r != null) {
            return r;
        }
        return false;
    }

    /** Returns whether the domain is suspended. */
    public boolean isSuspended(final String name) {
        final Boolean s = suspendedMap.get(name);
        if (s != null) {
            return s;
        }
        return false;
    }


    /** Returns remote port. */
    public int getRemotePort(final String name) {
        return remotePorts.get(name);
    }

    /** Returns host. */
    public Host getHost() {
        return host;
    }

    /** Returns configs of all vms. */
    public Set<String> getConfigs() {
        return configsMap.keySet();
    }

    /** Returns domain name from config file. */
    public String getNameFromConfig(final String config) {
        return configsMap.get(config);
    }

    /** Returns value. */
    public String getValue(final String name, final String param) {
        return (String) parameterValues.get(name, param);
    }

    /** Returns disk data. */
    public Map<String, DiskData> getDisks(final String name) {
        return disksMap.get(name);
    }

    /** Returns interface data. */
    public Map<String, InterfaceData> getInterfaces(final String name) {
        return interfacesMap.get(name);
    }

    /** Returns array of networks. */
    public List<String> getNetworks() {
        return new ArrayList<String>(networkMap.keySet());
    }

    /** Returns array of input devices. */
    public Map<String, InputDevData> getInputDevs(final String name) {
        return inputDevsMap.get(name);
    }

    /** Returns array of graphics devices. */
    public Map<String, GraphicsData> getGraphicDisplays(final String name) {
        return graphicsDevsMap.get(name);
    }

    /** Returns array of sound devices. */
    public Map<String, SoundData> getSounds(final String name) {
        return soundsMap.get(name);
    }

    /** Returns array of serial devices. */
    public Map<String, SerialData> getSerials(final String name) {
        return serialsMap.get(name);
    }

    /** Returns array of parallel devices. */
    public Map<String, ParallelData> getParallels(final String name) {
        return parallelsMap.get(name);
    }

    /** Returns array of video devices. */
    public Map<String, VideoData> getVideos(final String name) {
        return videosMap.get(name);
    }

    /** Class that holds data about networks. */
    final class NetworkData extends HardwareData {
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
        static final String AUTOSTART = "autostart";
        /** Forward mode. */
        static final String FORWARD_MODE = "forward_mode";
        /** Bridge name. */
        static final String BRIDGE_NAME = "bridge_name";
        /** Bridge STP. */
        static final String BRIDGE_STP = "bridge_stp";
        /** Bridge delay. */
        static final String BRIDGE_DELAY = "bridge_delay";
        /** Bridge forward delay. */
        static final String BRIDGE_FORWARD_DELAY = "bridge_forward_delay";

        /** Creates new NetworkData object. */
        NetworkData(final String name,
                    final String uuid,
                    final boolean autostart,
                    final String forwardMode,
                    final String bridgeName,
                    final String bridgeSTP,
                    final String bridgeDelay,
                    final String bridgeForwardDelay) {
            super();
            this.name = name;
            this.uuid = uuid;
            this.autostart = autostart;
            if (autostart) {
                setValue(AUTOSTART, "true");
            } else {
                setValue(AUTOSTART, "false");
            }
            this.forwardMode = forwardMode;
            setValue(FORWARD_MODE, forwardMode);
            this.bridgeName = bridgeName;
            setValue(BRIDGE_NAME, bridgeName);
            this.bridgeSTP = bridgeSTP;
            setValue(BRIDGE_STP, bridgeSTP);
            this.bridgeDelay = bridgeDelay;
            setValue(BRIDGE_DELAY, bridgeDelay);
            this.bridgeForwardDelay = bridgeForwardDelay;
            setValue(BRIDGE_FORWARD_DELAY, bridgeForwardDelay);
        }

        /** Whether it is autostart. */
        boolean isAutostart() {
            return autostart;
        }

        /** Returns forward mode. */
        String getForwardMode() {
            return forwardMode;
        }

        /** Returns bridge name. */
        String getBridgeName() {
            return bridgeName;
        }

        /** Returns bridge STP. */
        String getBridgeSTP() {
            return bridgeSTP;
        }

        /** Returns bridge delay. */
        String getBridgeDelay() {
            return bridgeDelay;
        }

        /** Returns bridge forward delay. */
        String getBridgeForwardDelay() {
            return bridgeForwardDelay;
        }
    }

    /** Returns function that gets the node that belongs to the paremeters. */
    protected VirtualHardwareComparator getDiskDataComparator() {
        return new VirtualHardwareComparator() {
            @Override public Element getElement(
                                    final NodeList nodes,
                                    final Map<String, String> parameters) {
                Element el = null;
                final String targetDev = parameters.get(
                                                DiskData.SAVED_TARGET_DEVICE);
                if (targetDev != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = getChildNode(nodes.item(i), "target");
                        if (targetDev.equals(getAttribute(mn, "dev"))) {
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
            @Override public Element getElement(
                                    final NodeList nodes,
                                    final Map<String, String> parameters) {
                final String macAddress = parameters.get(
                                              InterfaceData.SAVED_MAC_ADDRESS);
                Element el = null;
                if (macAddress != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = getChildNode(nodes.item(i), "mac");
                        if (macAddress.equals(getAttribute(mn, "address"))) {
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
            @Override public Element getElement(
                                    final NodeList nodes,
                                    final Map<String, String> parameters) {
                final String type = parameters.get(InputDevData.SAVED_TYPE);
                final String bus = parameters.get(InputDevData.SAVED_BUS);
                Element el = null;
                if (type != null && bus != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = nodes.item(i);
                        if (type.equals(getAttribute(mn, "type"))
                            && bus.equals(getAttribute(mn, "bus"))) {
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
            @Override public Element getElement(
                                    final NodeList nodes,
                                    final Map<String, String> parameters) {
                final String type = parameters.get(GraphicsData.SAVED_TYPE);
                Element el = null;
                if (type != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = nodes.item(i);
                        if (type.equals(getAttribute(mn, "type"))) {
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
            @Override public Element getElement(
                                    final NodeList nodes,
                                    final Map<String, String> parameters) {
                final String model = parameters.get(SoundData.SAVED_MODEL);
                Element el = null;
                if (model != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = nodes.item(i);
                        if (model.equals(getAttribute(mn, "model"))) {
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
            @Override public Element getElement(
                                    final NodeList nodes,
                                    final Map<String, String> parameters) {
                final String type = parameters.get(SerialData.SAVED_TYPE);
                Element el = null;
                if (type != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = nodes.item(i);
                        if (type.equals(getAttribute(mn, "type"))) {
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
            @Override public Element getElement(
                                    final NodeList nodes,
                                    final Map<String, String> parameters) {
                final String type = parameters.get(ParallelData.SAVED_TYPE);
                Element el = null;
                if (type != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = nodes.item(i);
                        if (type.equals(getAttribute(mn, "type"))) {
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
            @Override public Element getElement(
                                    final NodeList nodes,
                                    final Map<String, String> parameters) {
                Element el = null;
                final String modelType = parameters.get(
                                                VideoData.SAVED_MODEL_TYPE);
                if (modelType != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        final Node mn = getChildNode(nodes.item(i), "model");
                        if (modelType.equals(getAttribute(mn, "type"))) {
                            el = (Element) nodes.item(i);
                        }
                    }
                }
                return el;
            }
        };
    }


    /** Class that holds data about virtual disks. */
    public final class DiskData extends HardwareData {
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
        /** Type. */
        public static final String TYPE = "type";
        /** Target device string. */
        public static final String TARGET_DEVICE = "target_device";
        /** Saved target device string. */
        public static final String SAVED_TARGET_DEVICE = "saved_target_device";
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

        /** Creates new DiskData object. */
        public DiskData(final String type,
                        final String targetDev,
                        final String sourceFile,
                        final String sourceDev,
                        final String targetBusType,
                        final String driverName,
                        final String driverType,
                        final boolean readonly,
                        final boolean shareable) {
            super();
            this.type = type;
            setValue(TYPE, type);
            this.targetDev = targetDev;
            setValue(TARGET_DEVICE, targetDev);
            this.sourceFile = sourceFile;
            setValue(SOURCE_FILE, sourceFile);
            this.sourceDev = sourceDev;
            setValue(SOURCE_DEVICE, sourceDev);
            this.targetBusType = targetBusType;
            setValue(TARGET_BUS_TYPE, targetBusType);
            this.driverName = driverName;
            setValue(DRIVER_NAME, driverName);
            this.driverType = driverType;
            setValue(DRIVER_TYPE, driverType);
            this.readonly = readonly;
            if (readonly) {
                setValue(READONLY, "True");
            } else {
                setValue(READONLY, "False");
            }
            this.shareable = shareable;
            if (shareable) {
                setValue(SHAREABLE, "True");
            } else {
                setValue(SHAREABLE, "False");
            }
        }

        /** Returns type. */
        public String getType() {
            return type;
        }

        /** Returns target device. */
        public String getTargetDev() {
            return targetDev;
        }

        /** Returns source file. */
        public String getSourceFile() {
            return sourceFile;
        }

        /** Returns source device. */
        public String getSourceDev() {
            return sourceDev;
        }

        /** Returns target bus. */
        public String getTargetBusType() {
            return targetBusType;
        }

        /** Returns driver name. */
        String getDriverName() {
            return driverName;
        }

        /** Returns driver type. */
        String getDriverType() {
            return driverType;
        }

        /** Returns whether the disk is read only. */
        boolean isReadonly() {
            return readonly;
        }

        /** Returns whether the disk is read only. */
        boolean isShareable() {
            return shareable;
        }
    }

    /** Class that holds data about virtual hardware. */
    private abstract class HardwareData {
        /** Name value pairs. */
        private final Map<String, String> valueMap =
                                                new HashMap<String, String>();
        /** Sets value of this parameter. */
        final void setValue(final String param, final String value) {
            valueMap.put(param, value);
        }
        /** Returns value of this parameter. */
        public final String getValue(final String param) {
            return valueMap.get(param);
        }
    }

    /** Class that holds data about virtual interfaces. */
    public final class InterfaceData extends HardwareData {
        /** Type: network, bridge... */
        private final String type;
        /** Source network: default, ... */
        private final String sourceNetwork;
        /** Source bridge: br0... */
        private final String sourceBridge;
        /** Target dev: vnet0... */
        private final String targetDev;

        /** Type. */
        public static final String TYPE = "type";
        /** Mac address. */
        public static final String MAC_ADDRESS = "mac_address";
        /** Saved mac address. */
        public static final String SAVED_MAC_ADDRESS = "saved_mac_address";
        /** Source network. */
        public static final String SOURCE_NETWORK = "source_network";
        /** Source bridge. */
        public static final String SOURCE_BRIDGE = "source_bridge";
        /** Target dev. */
        public static final String TARGET_DEV = "target_dev";
        /** Model type: virtio... */
        public static final String MODEL_TYPE = "model_type";

        /** Creates new InterfaceData object. */
        public InterfaceData(final String type,
                             final String macAddress,
                             final String sourceNetwork,
                             final String sourceBridge,
                             final String targetDev,
                             final String modelType) {
            super();
            this.type = type;
            setValue(TYPE, type);
            setValue(MAC_ADDRESS, macAddress);
            this.sourceNetwork = sourceNetwork;
            setValue(SOURCE_NETWORK, sourceNetwork);
            this.sourceBridge = sourceBridge;
            setValue(SOURCE_BRIDGE, sourceBridge);
            this.targetDev = targetDev;
            setValue(TARGET_DEV, targetDev);
            setValue(MODEL_TYPE, modelType);
        }

        /** Returns type. */
        public String getType() {
            return type;
        }

        /** Returns source network. */
        public String getSourceNetwork() {
            return sourceNetwork;
        }

        /** Returns source bridge. */
        public String getSourceBridge() {
            return sourceBridge;
        }

        /** Returns target dev. */
        public String getTargetDev() {
            return targetDev;
        }

    }

    /** Class that holds data about virtual input devices. */
    public final class InputDevData extends HardwareData {
        /** Type: tablet, mouse... */
        private final String type;
        /** Bus: usb... */
        private final String bus;
        /** Type. */
        public static final String TYPE = "type";
        /** Bus. */
        public static final String BUS = "bus";
        /** Saved type. */
        public static final String SAVED_TYPE = "saved_type";
        /** Saved bus. */
        public static final String SAVED_BUS = "saved_bus";

        /** Creates new InputDevData object. */
        public InputDevData(final String type,
                            final String bus) {
            super();
            this.type = type;
            setValue(TYPE, type);
            this.bus = bus;
            setValue(BUS, bus);
        }

        /** Returns type. */
        public String getType() {
            return type;
        }

        /** Returns bus. */
        public String getBus() {
            return bus;
        }

    }

    /** Class that holds data about virtual displays. */
    public final class GraphicsData extends HardwareData {
        /** Type. */
        private final String type;
        /** Type: vnc, sdl... */
        public static final String TYPE = "type";
        /** Saved type. */
        public static final String SAVED_TYPE = "saved_type";
        /** Autoport. */
        public static final String AUTOPORT = "autoport";
        /** Port: -1 for auto. */
        public static final String PORT = "port";
        /** Listen: ip, on which interface to listen. */
        public static final String LISTEN = "listen";
        /** Passwdord. */
        public static final String PASSWD = "passwd";
        /** Keymap. */
        public static final String KEYMAP = "keymap";
        /** Display / SDL. */
        public static final String DISPLAY = "display";
        /** Xauth file / SDL. */
        public static final String XAUTH = "xauth";

        /** Creates new GraphicsData object. */
        public GraphicsData(final String type,
                            final String port,
                            final String listen,
                            final String passwd,
                            final String keymap,
                            final String display,
                            final String xauth) {
            super();
            this.type = type;
            setValue(TYPE, type);
            setValue(PORT, port);
            setValue(LISTEN, listen);
            setValue(PASSWD, passwd);
            setValue(KEYMAP, keymap);
            setValue(DISPLAY, display);
            setValue(XAUTH, xauth);
        }

        /** Returns type. */
        public String getType() {
            return type;
        }
    }

    /** Class that holds data about virtual sound devices. */
    public final class SoundData extends HardwareData {
        /** Model. */
        private final String model;
        /** Model: ac97, es1370, pcspk, sb16. */
        public static final String MODEL = "model";
        /** Saved model. */
        public static final String SAVED_MODEL = "saved_model";

        /** Creates new SoundData object. */
        public SoundData(final String model) {
            super();
            this.model = model;
            setValue(MODEL, model);
        }

        /** Returns model. */
        public String getModel() {
            return model;
        }
    }

    /** Class that holds data about virtual parallel or serial devices. */
    public abstract class ParallelSerialData extends HardwareData {
        /** Type. */
        private final String type;
        /** Type: dev, file, null, pipe, pty, stdio, tcp, udp, unix, vc. */
        public static final String TYPE = "type";
        /** Saved type. */
        public static final String SAVED_TYPE = "saved_type";
        /** Source path. */
        public static final String SOURCE_PATH = "source_path";
        /** Source mode: bind, connect. This is for tcp, because it is
            either-or */
        public static final String SOURCE_MODE = "source_mode";
        /** Source mode: bind, connect. */
        public static final String BIND_SOURCE_MODE = "bind_source_mode";
        /** Bind source host. */
        public static final String BIND_SOURCE_HOST = "bind_source_host";
        /** Bind source service. */
        public static final String BIND_SOURCE_SERVICE = "bind_source_service";
        /** Source mode: bind, connect. */
        public static final String CONNECT_SOURCE_MODE = "bind_source_mode";
        /** Connect source host. */
        public static final String CONNECT_SOURCE_HOST = "connect_source_host";
        /** Connect source service. */
        public static final String CONNECT_SOURCE_SERVICE =
                                                    "connect_source_service";
        /** Protocol type. */
        public static final String PROTOCOL_TYPE = "protocol_type";
        /** Target port. */
        public static final String TARGET_PORT = "target_port";
        /** Creates new ParallelSerialData object. */
        public ParallelSerialData(final String type,
                                  final String sourcePath,
                                  final String bindSourceMode,
                                  final String bindSourceHost,
                                  final String bindSourceService,
                                  final String connectSourceMode,
                                  final String connectSourceHost,
                                  final String connectSourceService,
                                  final String protocolType,
                                  final String targetPort) {
            super();
            this.type = type;
            setValue(TYPE, type);
            setValue(SOURCE_PATH, sourcePath);
            setValue(BIND_SOURCE_MODE, bindSourceMode);
            setValue(BIND_SOURCE_HOST, bindSourceHost);
            setValue(BIND_SOURCE_SERVICE, bindSourceService);
            setValue(CONNECT_SOURCE_MODE, connectSourceMode);
            setValue(CONNECT_SOURCE_HOST, connectSourceHost);
            setValue(CONNECT_SOURCE_SERVICE, connectSourceService);
            setValue(PROTOCOL_TYPE, protocolType);
            setValue(TARGET_PORT, targetPort);
        }

        /** Returns model. */
        public final String getType() {
            return type;
        }
    }

    /** Class that holds data about virtual serial devices. */
    public final class SerialData extends ParallelSerialData {
        /** Creates new SerialData object. */
        public SerialData(final String type,
                          final String sourcePath,
                          final String bindSourceMode,
                          final String bindSourceHost,
                          final String bindSourceService,
                          final String connectSourceMode,
                          final String connectSourceHost,
                          final String connectSourceService,
                          final String protocolType,
                          final String targetPort) {

        super(type,
              sourcePath,
              bindSourceMode,
              bindSourceHost,
              bindSourceService,
              connectSourceMode,
              connectSourceHost,
              connectSourceService,
              protocolType,
              targetPort);
        }
    }

    /** Class that holds data about virtual parallel devices. */
    public final class ParallelData extends ParallelSerialData {
        /** Creates new ParallelData object. */
        public ParallelData(final String type,
                            final String sourcePath,
                            final String bindSourceMode,
                            final String bindSourceHost,
                            final String bindSourceService,
                            final String connectSourceMode,
                            final String connectSourceHost,
                            final String connectSourceService,
                            final String protocolType,
                            final String targetPort) {

        super(type,
              sourcePath,
              bindSourceMode,
              bindSourceHost,
              bindSourceService,
              connectSourceMode,
              connectSourceHost,
              connectSourceService,
              protocolType,
              targetPort);
        }
    }

    /** Class that holds data about virtual video devices. */
    public final class VideoData extends HardwareData {
        /** Model type. */
        private final String modelType;
        /** Model type: cirrus, vga, vmvga, xen. */
        public static final String MODEL_TYPE = "model_type";
        /** Saved model type. */
        public static final String SAVED_MODEL_TYPE = "saved_model_type";
        /** Model VRAM. */
        public static final String MODEL_VRAM = "model_vram";
        /** Model heads: 1. */
        public static final String MODEL_HEADS = "model_heads";

        /** Creates new VideoData object. */
        public VideoData(final String modelType,
                         final String modelVRAM,
                         final String modelHeads) {
            super();
            this.modelType = modelType;
            setValue(MODEL_TYPE, modelType);
            setValue(MODEL_VRAM, modelVRAM);
            setValue(MODEL_HEADS, modelHeads);
        }

        /** Returns model. */
        public String getModelType() {
            return modelType;
        }
    }

    /** Comparator. */
    private interface VirtualHardwareComparator {
        /** Returns an element. */
        Element getElement(final NodeList nodes,
                           final Map<String, String> parameters);
    }

    /** Returns string representation of the port; it can be autoport. */
    static String portString(final String port) {
        if ("-1".equals(port)) {
            return "auto";
        }
        return port;
    }

    /** Returns string representation graphic display SDL/VNC. */
    public static String graphicsDisplayName(final String type,
                                             final String port,
                                             final String display) {
        if ("vnc".equals(type)) {
            return type + " : " + portString(port);
        } else if ("sdl".equals(type)) {
            return type + " (" + display + ")";
        }
        return "unknown";
    }

    /** Returns source file directories. */
    public Set<String> getsourceFileDirs() {
        return sourceFileDirs;
    }
}
