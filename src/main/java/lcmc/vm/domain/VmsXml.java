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
    /** Pattern that maches display e.g. :4. */
    public static final String VM_PARAM_NAME = "name";
    public static final String VM_PARAM_UUID = "uuid";
    public static final String VM_PARAM_DEFINED = "defined";
    public static final String VM_PARAM_STATUS = "status";
    public static final String VM_PARAM_DOMAIN_TYPE = "domain-type";
    public static final String VM_PARAM_VCPU = "vcpu";
    public static final String VM_PARAM_BOOTLOADER = "bootloader";
    public static final String VM_PARAM_CURRENTMEMORY = "currentMemory";
    public static final String VM_PARAM_MEMORY = "memory";
    public static final String OS_BOOT_NODE = "boot";
    public static final String OS_BOOT_NODE_DEV = "dev";
    public static final String VM_PARAM_BOOT = "boot";
    public static final String VM_PARAM_BOOT_2 = "boot2";
    public static final String VM_PARAM_LOADER = "loader";
    public static final String VM_PARAM_AUTOSTART = "autostart";
    public static final String VM_PARAM_VIRSH_OPTIONS = "virsh-options";
    public static final String VM_PARAM_TYPE = "type";
    public static final String VM_PARAM_INIT = "init";
    public static final String VM_PARAM_TYPE_ARCH = "arch";
    public static final String VM_PARAM_TYPE_MACHINE = "machine";
    public static final String VM_PARAM_ACPI = "acpi";
    public static final String VM_PARAM_APIC = "apic";
    public static final String VM_PARAM_PAE = "pae";
    public static final String VM_PARAM_HAP = "hap";
    public static final String VM_PARAM_CLOCK_OFFSET = "offset";
    public static final String VM_PARAM_CPU_MATCH = "match";
    public static final String VM_PARAM_CPUMATCH_MODEL = "model";
    public static final String VM_PARAM_CPUMATCH_VENDOR = "vendor";
    public static final String VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS = "sockets";
    public static final String VM_PARAM_CPUMATCH_TOPOLOGY_CORES = "cores";
    public static final String VM_PARAM_CPUMATCH_TOPOLOGY_THREADS = "threads";
    public static final String VM_PARAM_CPUMATCH_FEATURE_POLICY = "policy";
    public static final String VM_PARAM_CPUMATCH_FEATURES = "features";
    public static final String VM_PARAM_ON_POWEROFF = "on_poweroff";
    public static final String VM_PARAM_ON_REBOOT = "on_reboot";
    public static final String VM_PARAM_ON_CRASH = "on_crash";
    public static final String VM_PARAM_EMULATOR = "emulator";
    public static final String HW_ADDRESS = "address";
    public static final Map<String, String> PARAM_INTERFACE_TAG = new HashMap<String, String>();
    public static final Map<String, String> PARAM_INTERFACE_ATTRIBUTE = new HashMap<String, String>();
    public static final Map<String, String> PARAM_DISK_TAG = new HashMap<String, String>();
    public static final Map<String, String> PARAM_DISK_ATTRIBUTE = new HashMap<String, String>();
    public static final Map<String, String> PARAM_FILESYSTEM_TAG = new HashMap<String, String>();
    public static final Map<String, String> PARAM_FILESYSTEM_ATTRIBUTE = new HashMap<String, String>();
    public static final Map<String, String> PARAM_INPUTDEV_TAG = new HashMap<String, String>();
    public static final Map<String, String> PARAM_INPUTDEV_ATTRIBUTE = new HashMap<String, String>();

    public static final Map<String, String> PARAM_GRAPHICS_TAG = new HashMap<String, String>();
    public static final Map<String, String> PARAM_GRAPHICS_ATTRIBUTE = new HashMap<String, String>();

    public static final Map<String, String> PARAM_SOUND_TAG = new HashMap<String, String>();
    public static final Map<String, String> PARAM_SOUND_ATTRIBUTE = new HashMap<String, String>();

    public static final Map<String, String> PARAM_SERIAL_TAG = new HashMap<String, String>();
    public static final Map<String, String> PARAM_SERIAL_ATTRIBUTE = new HashMap<String, String>();

    public static final Map<String, String> PARAM_PARALLEL_TAG = new HashMap<String, String>();
    public static final Map<String, String> PARAM_PARALLEL_ATTRIBUTE = new HashMap<String, String>();

    public static final Map<String, String> PARAM_VIDEO_TAG = new HashMap<String, String>();
    public static final Map<String, String> PARAM_VIDEO_ATTRIBUTE = new HashMap<String, String>();

    static {
        PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.TYPE, "type");
        PARAM_INTERFACE_TAG.put(InterfaceData.MAC_ADDRESS, "mac");
        PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.MAC_ADDRESS, "address");
        PARAM_INTERFACE_TAG.put(InterfaceData.SOURCE_NETWORK, "source");
        PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.SOURCE_NETWORK, "network");
        PARAM_INTERFACE_TAG.put(InterfaceData.SOURCE_BRIDGE, "source");
        PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.SOURCE_BRIDGE, "bridge");
        PARAM_INTERFACE_TAG.put(InterfaceData.TARGET_DEV, "target");
        PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.TARGET_DEV, "dev");
        PARAM_INTERFACE_TAG.put(InterfaceData.MODEL_TYPE, "model");
        PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.MODEL_TYPE, "type");
        PARAM_INTERFACE_TAG.put(InterfaceData.SCRIPT_PATH, "script");
        PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.SCRIPT_PATH, "path");

        PARAM_DISK_ATTRIBUTE.put(DiskData.TYPE, "type");
        PARAM_DISK_TAG.put(DiskData.TARGET_DEVICE, "target");
        PARAM_DISK_ATTRIBUTE.put(DiskData.TARGET_DEVICE, "dev");
        PARAM_DISK_TAG.put(DiskData.SOURCE_FILE, "source");
        PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_FILE, "file");
        PARAM_DISK_TAG.put(DiskData.SOURCE_DEVICE, "source");
        PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_DEVICE, "dev");

        PARAM_DISK_TAG.put(DiskData.SOURCE_PROTOCOL, "source");
        PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_PROTOCOL, "protocol");
        PARAM_DISK_TAG.put(DiskData.SOURCE_NAME, "source");
        PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_NAME, "name");

        PARAM_DISK_TAG.put(DiskData.SOURCE_HOST_NAME, "source:host");
        PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_HOST_NAME, "name");
        PARAM_DISK_TAG.put(DiskData.SOURCE_HOST_PORT, "source:host");
        PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_HOST_PORT, "port");

        PARAM_DISK_TAG.put(DiskData.AUTH_USERNAME, "auth");
        PARAM_DISK_ATTRIBUTE.put(DiskData.AUTH_USERNAME, "username");
        PARAM_DISK_TAG.put(DiskData.AUTH_SECRET_TYPE, "auth:secret");
        PARAM_DISK_ATTRIBUTE.put(DiskData.AUTH_SECRET_TYPE, "type");
        PARAM_DISK_TAG.put(DiskData.AUTH_SECRET_UUID, "auth:secret");
        PARAM_DISK_ATTRIBUTE.put(DiskData.AUTH_SECRET_UUID, "uuid");

        PARAM_DISK_TAG.put(DiskData.TARGET_BUS, "target");
        PARAM_DISK_ATTRIBUTE.put(DiskData.TARGET_BUS, "bus");
        PARAM_DISK_TAG.put(DiskData.DRIVER_NAME, "driver");
        PARAM_DISK_ATTRIBUTE.put(DiskData.DRIVER_NAME, "name");
        PARAM_DISK_TAG.put(DiskData.DRIVER_TYPE, "driver");
        PARAM_DISK_ATTRIBUTE.put(DiskData.DRIVER_TYPE, "type");
        PARAM_DISK_TAG.put(DiskData.DRIVER_CACHE, "driver");
        PARAM_DISK_ATTRIBUTE.put(DiskData.DRIVER_CACHE, "cache");
        PARAM_DISK_ATTRIBUTE.put(DiskData.TARGET_TYPE, "device");
        PARAM_DISK_TAG.put(DiskData.READONLY, "readonly");
        PARAM_DISK_TAG.put(DiskData.SHAREABLE, "shareable");

        PARAM_FILESYSTEM_ATTRIBUTE.put(InterfaceData.TYPE, "type");
        PARAM_FILESYSTEM_TAG.put(FilesystemData.SOURCE_DIR, "source");
        PARAM_FILESYSTEM_ATTRIBUTE.put(FilesystemData.SOURCE_DIR, "dir");
        PARAM_FILESYSTEM_TAG.put(FilesystemData.SOURCE_NAME, "source");
        PARAM_FILESYSTEM_ATTRIBUTE.put(FilesystemData.SOURCE_NAME, "name");
        PARAM_FILESYSTEM_TAG.put(FilesystemData.TARGET_DIR, "target");
        PARAM_FILESYSTEM_ATTRIBUTE.put(FilesystemData.TARGET_DIR, "dir");

        PARAM_INPUTDEV_ATTRIBUTE.put(InputDevData.TYPE, "type");
        PARAM_INPUTDEV_ATTRIBUTE.put(InputDevData.BUS, "bus");

        PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.TYPE, "type");
        PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.PORT, "port");
        PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.AUTOPORT, "autoport");
        PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.LISTEN, "listen");
        PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.PASSWD, "passwd");
        PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.KEYMAP, "keymap");
        PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.DISPLAY, "display");
        PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.XAUTH, "xauth");

        PARAM_SOUND_ATTRIBUTE.put(SoundData.MODEL, "model");

        PARAM_SERIAL_ATTRIBUTE.put(SerialData.TYPE, "type");
        PARAM_SERIAL_TAG.put(SerialData.SOURCE_PATH, "source");
        PARAM_SERIAL_ATTRIBUTE.put(SerialData.SOURCE_PATH, "path");
        PARAM_SERIAL_TAG.put(SerialData.BIND_SOURCE_MODE, "source");
        PARAM_SERIAL_ATTRIBUTE.put(SerialData.BIND_SOURCE_MODE, "mode");
        PARAM_SERIAL_TAG.put(SerialData.BIND_SOURCE_HOST, "source");
        PARAM_SERIAL_ATTRIBUTE.put(SerialData.BIND_SOURCE_HOST, "host");
        PARAM_SERIAL_TAG.put(SerialData.BIND_SOURCE_SERVICE, "source");
        PARAM_SERIAL_ATTRIBUTE.put(SerialData.BIND_SOURCE_SERVICE, "service");
        PARAM_SERIAL_TAG.put(SerialData.CONNECT_SOURCE_MODE, "source");
        PARAM_SERIAL_ATTRIBUTE.put(SerialData.CONNECT_SOURCE_MODE, "mode");
        PARAM_SERIAL_TAG.put(SerialData.CONNECT_SOURCE_HOST, "source");
        PARAM_SERIAL_ATTRIBUTE.put(SerialData.CONNECT_SOURCE_HOST, "host");
        PARAM_SERIAL_TAG.put(SerialData.CONNECT_SOURCE_SERVICE, "source");
        PARAM_SERIAL_ATTRIBUTE.put(SerialData.CONNECT_SOURCE_SERVICE, "service");
        PARAM_SERIAL_TAG.put(SerialData.PROTOCOL_TYPE, "protocol");
        PARAM_SERIAL_ATTRIBUTE.put(SerialData.PROTOCOL_TYPE, "type");
        PARAM_SERIAL_TAG.put(SerialData.TARGET_PORT, "target");
        PARAM_SERIAL_ATTRIBUTE.put(SerialData.TARGET_PORT, "port");

        PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.TYPE, "type");
        PARAM_PARALLEL_TAG.put(ParallelData.SOURCE_PATH, "source");
        PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.SOURCE_PATH, "path");
        PARAM_PARALLEL_TAG.put(ParallelData.BIND_SOURCE_MODE, "source");
        PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.BIND_SOURCE_MODE, "mode");
        PARAM_PARALLEL_TAG.put(ParallelData.BIND_SOURCE_HOST, "source");
        PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.BIND_SOURCE_HOST, "host");
        PARAM_PARALLEL_TAG.put(ParallelData.BIND_SOURCE_SERVICE, "source");
        PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.BIND_SOURCE_SERVICE, "service");

        PARAM_PARALLEL_TAG.put(ParallelData.CONNECT_SOURCE_MODE, "source");
        PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.CONNECT_SOURCE_MODE, "mode");
        PARAM_PARALLEL_TAG.put(ParallelData.CONNECT_SOURCE_HOST, "source");
        PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.CONNECT_SOURCE_HOST, "host");
        PARAM_PARALLEL_TAG.put(ParallelData.CONNECT_SOURCE_SERVICE, "source");
        PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.CONNECT_SOURCE_SERVICE,
                "service");

        PARAM_PARALLEL_TAG.put(ParallelData.PROTOCOL_TYPE, "protocol");
        PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.PROTOCOL_TYPE, "type");
        PARAM_PARALLEL_TAG.put(ParallelData.TARGET_PORT, "target");
        PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.TARGET_PORT, "port");

        PARAM_VIDEO_TAG.put(VideoData.MODEL_TYPE, "model");
        PARAM_VIDEO_ATTRIBUTE.put(VideoData.MODEL_TYPE, "type");
        PARAM_VIDEO_TAG.put(VideoData.MODEL_VRAM, "model");
        PARAM_VIDEO_ATTRIBUTE.put(VideoData.MODEL_VRAM, "vram");
        PARAM_VIDEO_TAG.put(VideoData.MODEL_HEADS, "model");
        PARAM_VIDEO_ATTRIBUTE.put(VideoData.MODEL_HEADS, "heads");
    }

    @Autowired
    private NetworkParser networkParser;
    @Autowired
    private VMParser vmParser;

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

    private void addCPUMatchNode(final Document doc, final Node root, final Map<String, String> parametersMap) {
        final String cpuMatch = parametersMap.get(VM_PARAM_CPU_MATCH);
        final Element cpuMatchNode = (Element) root.appendChild(doc.createElement("cpu"));
        if (!"".equals(cpuMatch)) {
            cpuMatchNode.setAttribute("match", cpuMatch);
        }
        final String model = parametersMap.get(VM_PARAM_CPUMATCH_MODEL);
        if (!"".equals(model)) {
            final Node modelNode = cpuMatchNode.appendChild(doc.createElement("model"));
            modelNode.appendChild(doc.createTextNode(model));
        }
        final String vendor = parametersMap.get(VM_PARAM_CPUMATCH_VENDOR);
        if (!"".equals(vendor)) {
            final Node vendorNode = cpuMatchNode.appendChild(doc.createElement("vendor"));
            vendorNode.appendChild(doc.createTextNode(vendor));
        }
        final String sockets = parametersMap.get(VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS);
        final String cores = parametersMap.get(VM_PARAM_CPUMATCH_TOPOLOGY_CORES);
        final String threads = parametersMap.get(VM_PARAM_CPUMATCH_TOPOLOGY_THREADS);
        final boolean isSockets = !"".equals(sockets);
        final boolean isCores =   !"".equals(cores);
        final boolean isThreads = !"".equals(threads);
        if (isSockets || isCores || isThreads) {
            final Element topologyNode = (Element) cpuMatchNode.appendChild(doc.createElement("topology"));
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
        final String policy = parametersMap.get(VM_PARAM_CPUMATCH_FEATURE_POLICY);
        final String features = parametersMap.get(VM_PARAM_CPUMATCH_FEATURES);
        if (policy != null && features != null && !"".equals(policy) && !"".equals(features)) {
            for (final String feature : features.split("\\s+")) {
                final Element featureNode = (Element) cpuMatchNode.appendChild(doc.createElement("feature"));
                featureNode.setAttribute("policy", policy);
                featureNode.setAttribute("name", feature);
            }
        }
        if (!cpuMatchNode.hasChildNodes()) {
            root.removeChild(cpuMatchNode);
        }
    }

    private void addFeatures(final Document doc, final Node root, final Map<String, String> parametersMap) {
        final boolean acpi = "True".equals(parametersMap.get(VM_PARAM_ACPI));
        final boolean apic = "True".equals(parametersMap.get(VM_PARAM_APIC));
        final boolean pae = "True".equals(parametersMap.get(VM_PARAM_PAE));
        final boolean hap = "True".equals(parametersMap.get(VM_PARAM_HAP));
        if (acpi || apic || pae || hap) {
            final Node featuresNode = root.appendChild(doc.createElement("features"));
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

    private void addClockOffset(final Document doc, final Node root, final Map<String, String> parametersMap) {
        final Element clockNode = (Element) root.appendChild(doc.createElement("clock"));
        final String offset = parametersMap.get(VM_PARAM_CLOCK_OFFSET);
        clockNode.setAttribute("offset", offset);
        final Element timer1 = (Element) clockNode.appendChild(doc.createElement("timer"));
        timer1.setAttribute("name", "pit");
        timer1.setAttribute("tickpolicy", "delay");
        final Element timer2 = (Element) clockNode.appendChild(doc.createElement("timer"));
        timer2.setAttribute("name", "rtc");
        timer2.setAttribute("tickpolicy", "catchup");

        final Element timer3 = (Element) clockNode.appendChild(doc.createElement("timer"));
        timer3.setAttribute("name", "hpet");
        timer3.setAttribute("present", "no");
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
        //<domain type='kvm'>
        //  <memory>524288</memory>
        //  <name>fff</name>
        //  <os>
        //    <type arch='i686' machine='pc-0.12'>hvm</type>
        //  </os>
        //</domain>

        /* domain type: kvm/xen */
        final String type = parametersMap.get(VM_PARAM_DOMAIN_TYPE);
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
        final Element root = (Element) doc.appendChild(doc.createElement("domain"));
        root.setAttribute("type", type); /* kvm/xen */
        final Node uuidNode = root.appendChild(doc.createElement("uuid"));
        uuidNode.appendChild(doc.createTextNode(uuid));
        final Node nameNode = root.appendChild(doc.createElement("name"));
        nameNode.appendChild(doc.createTextNode(domainName));
        final Node memoryNode = root.appendChild(doc.createElement("memory"));
        final long mem = Long.parseLong(parametersMap.get(VM_PARAM_MEMORY));
        memoryNode.appendChild(doc.createTextNode(Long.toString(mem)));
        final Node curMemoryNode = root.appendChild(doc.createElement("currentMemory"));
        final long curMem = Long.parseLong(parametersMap.get(VM_PARAM_CURRENTMEMORY));
        curMemoryNode.appendChild(doc.createTextNode(Long.toString(curMem)));
        final String vcpu = parametersMap.get(VM_PARAM_VCPU);
        if (vcpu != null) {
            final Node vcpuNode = root.appendChild(doc.createElement("vcpu"));
            vcpuNode.appendChild(doc.createTextNode(vcpu));
        }

        final String bootloader = parametersMap.get(VM_PARAM_BOOTLOADER);
        if (bootloader != null) {
            final Node bootloaderNode = root.appendChild(doc.createElement("bootloader"));
            bootloaderNode.appendChild(doc.createTextNode(bootloader));
        }

        final Node osNode = root.appendChild(doc.createElement("os"));
        final Element typeNode = (Element) osNode.appendChild(doc.createElement("type"));
        typeNode.appendChild(doc.createTextNode(parametersMap.get(VM_PARAM_TYPE)));
        typeNode.setAttribute("arch", parametersMap.get(VM_PARAM_TYPE_ARCH));
        typeNode.setAttribute("machine", parametersMap.get(VM_PARAM_TYPE_MACHINE));
        final String init = parametersMap.get(VM_PARAM_INIT);
        if (init != null && !"".equals(init)) {
            final Node initNode = osNode.appendChild(doc.createElement("init"));
            initNode.appendChild(doc.createTextNode(init));
        }
        final Element bootNode = (Element) osNode.appendChild(doc.createElement(OS_BOOT_NODE));
        bootNode.setAttribute(OS_BOOT_NODE_DEV, parametersMap.get(VM_PARAM_BOOT));
        final String bootDev2 = parametersMap.get(VM_PARAM_BOOT_2);
        if (bootDev2 != null && !"".equals(bootDev2)) {
            final Element bootNode2 = (Element) osNode.appendChild(doc.createElement(OS_BOOT_NODE));
            bootNode2.setAttribute(OS_BOOT_NODE_DEV, parametersMap.get(VM_PARAM_BOOT_2));
        }

        final Node loaderNode = osNode.appendChild(doc.createElement("loader"));
        loaderNode.appendChild(doc.createTextNode(parametersMap.get(VM_PARAM_LOADER)));

        addFeatures(doc, root, parametersMap);
        addClockOffset(doc, root, parametersMap);
        addCPUMatchNode(doc, root, parametersMap);

        final String onPoweroff = parametersMap.get(VM_PARAM_ON_POWEROFF);
        if (onPoweroff != null) {
            final Node onPoweroffNode = root.appendChild(doc.createElement("on_poweroff"));
            onPoweroffNode.appendChild(doc.createTextNode(onPoweroff));
        }
        final String onReboot = parametersMap.get(VM_PARAM_ON_REBOOT);
        if (onReboot != null) {
            final Node onRebootNode = root.appendChild(doc.createElement("on_reboot"));
            onRebootNode.appendChild(doc.createTextNode(onReboot));
        }
        final String onCrash = parametersMap.get(VM_PARAM_ON_CRASH);
        if (onCrash != null) {
            final Node onCrashNode = root.appendChild(doc.createElement("on_crash"));
            onCrashNode.appendChild(doc.createTextNode(onCrash));
        }
        final String emulator = parametersMap.get(VM_PARAM_EMULATOR);
        if (emulator != null || needConsole) {
            final Node devicesNode = root.appendChild(doc.createElement("devices"));
            if (needConsole) {
                final Element consoleNode = (Element) devicesNode.appendChild(doc.createElement("console"));
                consoleNode.setAttribute("type", "pty");
            }
            final Node emulatorNode = devicesNode.appendChild(doc.createElement("emulator"));
            emulatorNode.appendChild(doc.createTextNode(emulator));
        }
        return root;
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
        final XPath xpath = XPathFactory.newInstance().newXPath();
        final Map<String, String> paths = new HashMap<String, String>();
        paths.put(VM_PARAM_MEMORY, "memory");
        paths.put(VM_PARAM_CURRENTMEMORY, "currentMemory");
        paths.put(VM_PARAM_VCPU, "vcpu");
        paths.put(VM_PARAM_BOOTLOADER, "bootloader");
        paths.put(VM_PARAM_BOOT, "os/boot");
        paths.put(VM_PARAM_BOOT_2, "os/boot");
        paths.put(VM_PARAM_TYPE, "os/type");
        paths.put(VM_PARAM_TYPE_ARCH, "os/type");
        paths.put(VM_PARAM_TYPE_MACHINE, "os/type");
        paths.put(VM_PARAM_INIT, "os/init");
        paths.put(VM_PARAM_LOADER, "os/loader");
        paths.put(VM_PARAM_CPU_MATCH, "cpu");
        paths.put(VM_PARAM_ACPI, "features");
        paths.put(VM_PARAM_APIC, "features");
        paths.put(VM_PARAM_PAE, "features");
        paths.put(VM_PARAM_HAP, "features");
        paths.put(VM_PARAM_CLOCK_OFFSET, "clock");
        paths.put(VM_PARAM_ON_POWEROFF, "on_poweroff");
        paths.put(VM_PARAM_ON_REBOOT, "on_reboot");
        paths.put(VM_PARAM_ON_CRASH, "on_crash");
        paths.put(VM_PARAM_EMULATOR, "devices/emulator");
        final Document doc = domainNode.getOwnerDocument();
        try {
            for (final String param : parametersMap.keySet()) {
                final String path = paths.get(param);
                if (path == null) {
                    continue;
                }
                final NodeList nodes = (NodeList) xpath.evaluate(path, domainNode, XPathConstants.NODESET);
                Element node = (Element) nodes.item(0);
                if (node == null) {
                    continue;
                }
                if (VM_PARAM_BOOT_2.equals(param)) {
                    if (nodes.getLength() > 1) {
                        node = (Element) nodes.item(1);
                    } else {
                        node = (Element) node.getParentNode().appendChild(doc.createElement(OS_BOOT_NODE));
                    }
                }
                final String value = parametersMap.get(param);
                if (VM_PARAM_CPU_MATCH.equals(param)
                    || VM_PARAM_CLOCK_OFFSET.equals(param)
                    || VM_PARAM_ACPI.equals(param)
                    || VM_PARAM_APIC.equals(param)
                    || VM_PARAM_PAE.equals(param)
                    || VM_PARAM_HAP.equals(param)) {
                    domainNode.removeChild(node);
                } else if (VM_PARAM_BOOT.equals(param)) {
                    node.setAttribute(OS_BOOT_NODE_DEV, value);
                } else if (VM_PARAM_BOOT_2.equals(param)) {
                    if (value == null || "".equals(value)) {
                        node.getParentNode().removeChild(node);
                    } else {
                        node.setAttribute(OS_BOOT_NODE_DEV, value);
                    }
                } else if (VM_PARAM_TYPE_ARCH.equals(param)) {
                    node.setAttribute("arch", value);
                } else if (VM_PARAM_TYPE_MACHINE.equals(param)) {
                    node.setAttribute("machine", value);
                } else if (VM_PARAM_CPU_MATCH.equals(param)) {
                    if ("".equals(value)) {
                        node.getParentNode().removeChild(node);
                    } else {
                        node.setAttribute("match", value);
                    }
                } else if (VM_PARAM_CPUMATCH_TOPOLOGY_THREADS.equals(param)) {
                    node.setAttribute("threads", value);
                } else {
                    final Node n = XMLTools.getChildNode(node, "#text");
                    if (n == null) {
                        node.appendChild(doc.createTextNode(value));
                    } else {
                        n.setNodeValue(value);
                    }
                }
            }
            addCPUMatchNode(doc, domainNode, parametersMap);
            addFeatures(doc, domainNode, parametersMap);
            addClockOffset(doc, domainNode, parametersMap);
        } catch (final XPathExpressionException e) {
            LOG.appError("modifyDomainXML: could not evaluate: ", e);
            return null;
        }
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
            final Node hwAddressNode = XMLTools.getChildNode(hwNode, HW_ADDRESS);
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
                  PARAM_DISK_TAG,
                  PARAM_DISK_ATTRIBUTE,
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
                  PARAM_FILESYSTEM_TAG,
                  PARAM_FILESYSTEM_ATTRIBUTE,
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
                  PARAM_INTERFACE_TAG,
                  PARAM_INTERFACE_ATTRIBUTE,
                  parametersMap,
                  "devices/interface",
                  "interface",
                  getInterfaceDataComparator());
    }

    public void modifyInputDevXML(final Node domainNode,
                                  final String domainName,
                                  final Map<String, String> parametersMap) {
        modifyXML(domainNode, domainName,
                  PARAM_INPUTDEV_TAG,
                  PARAM_INPUTDEV_ATTRIBUTE,
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
                  PARAM_GRAPHICS_TAG,
                  PARAM_GRAPHICS_ATTRIBUTE,
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
                  PARAM_SOUND_TAG,
                  PARAM_SOUND_ATTRIBUTE,
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
                  PARAM_SERIAL_TAG,
                  PARAM_SERIAL_ATTRIBUTE,
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
                  PARAM_PARALLEL_TAG,
                  PARAM_PARALLEL_ATTRIBUTE,
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
                  PARAM_VIDEO_TAG,
                  PARAM_VIDEO_ATTRIBUTE,
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
