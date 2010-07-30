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

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.commons.collections.map.MultiKeyMap;

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
                             new HashMap<String, Map<String, DiskData>>();
    /** Map from domain name and mac address to the interface data. */
    private final Map<String, Map<String, InterfaceData>> interfacesMap =
                             new HashMap<String, Map<String, InterfaceData>>();
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

    /**
     * Prepares a new <code>VMSXML</code> object.
     */
    public VMSXML(final Host host) {
        super();
        this.host = host;
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
            final Node vmNode = vms.item(i);
            if ("vm".equals(vmNode.getNodeName())) {
                updateVM(vmNode);
            }
        }
        return true;
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
        final String name = getAttribute(vmNode, VM_PARAM_NAME);
        final String config = getAttribute(vmNode, "config");
        configsMap.put(config, name);
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
                        boolean readonly = false;
                        for (int k = 0; k < opts.getLength(); k++) {
                            final Node optionNode = opts.item(k);
                            final String nodeName = optionNode.getNodeName();
                            if ("source".equals(nodeName)) {
                                sourceFile = getAttribute(optionNode, "file");
                                sourceDev = getAttribute(optionNode, "dev");
                            } else if ("target".equals(nodeName)) {
                                targetDev = getAttribute(optionNode, "dev");
                                targetBus = getAttribute(optionNode, "bus");
                            } else if ("readonly".equals(nodeName)) {
                                readonly = true;
                            } else if (!"#text".equals(nodeName)) {
                                Tools.appWarning("unknown disk option: "
                                                 + nodeName);
                            }
                        }
                        if (targetDev != null) {
                            final DiskData diskData = new DiskData(type,
                                                                   device,
                                                                   targetDev,
                                                                   sourceFile,
                                                                   sourceDev,
                                                                   targetBus,
                                                                   readonly);
                            devMap.put(targetDev, diskData);
                        }
                    } else if ("interface".equals(deviceNode.getNodeName())) {
                        final String type = getAttribute(deviceNode, "type");
                        String macAddress = null;
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

    /**
     * Returns interface data.
     */
    public final Map<String, InterfaceData> getInterfaces(final String name) {
        return interfacesMap.get(name);
    }

    /** Class that holds data about virtual disks. */
    public class DiskData {
        /** Type: file, block... */
        private final String type;
        /** Device: disk, cdrom... */
        private final String device;
        /** Target device: hda, hdb, hdc, sda... */
        private final String targetDev;
        /** Source file. */
        private final String sourceFile;
        /** Source device: /dev/drbd0... */
        private final String sourceDev;
        /** Target bus: ide. */
        private final String targetBus;
        /** Whether the disk is read only. */
        private final boolean readonly;
        /** Name value pairs. */
        private final Map<String, String> valueMap =
                                                new HashMap<String, String>();
        /** Type. */
        public static final String TYPE = "type";
        /** Device. */
        public static final String DEVICE = "device";
        /** Target device string. */
        public static final String TARGET_DEVICE = "target_device";
        /** Source file. */
        public static final String SOURCE_FILE = "source_file";
        /** Source dev. */
        public static final String SOURCE_DEVICE = "source_dev";
        /** Target bus. */
        public static final String TARGET_BUS = "target_bus";
        /** Readonly. */
        public static final String READONLY = "readonly";

        /**
         * Creates new DiskData object.
         */
        public DiskData(final String type,
                        final String device,
                        final String targetDev,
                        final String sourceFile,
                        final String sourceDev,
                        final String targetBus,
                        final boolean readonly) {
            this.type = type;
            valueMap.put(TYPE, type);
            this.device = device;
            valueMap.put(DEVICE, device);
            this.targetDev = targetDev;
            valueMap.put(TARGET_DEVICE, targetDev);
            this.sourceFile = sourceFile;
            valueMap.put(SOURCE_FILE, sourceFile);
            this.sourceDev = sourceDev;
            valueMap.put(SOURCE_DEVICE, sourceDev);
            this.targetBus = targetBus;
            valueMap.put(TARGET_BUS, targetBus);
            this.readonly = readonly;
            if (readonly) {
                valueMap.put(READONLY, "true");
            } else {
                valueMap.put(READONLY, "false");
            }
        }

        /** Returns type. */
        public final String getType() {
            return type;
        }

        /** Returns device. */
        public final String getDevice() {
            return device;
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
        public final String getTargetBus() {
            return targetBus;
        }

        /** Returns whether the disk is read only. */
        public final boolean isReadonly() {
            return readonly;
        }

        /** Returns value of this parameter. */
        public final String getValue(final String param) {
            return valueMap.get(param);
        }
    }

    /** Class that holds data about virtual interfaces. */
    public class InterfaceData {
        /** Type: bridge... */
        private final String type;
        /** Mac address. */
        private final String macAddress;
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
        /** Source bridge. */
        public static final String SOURCE_BRIDGE = "source_bridge";
        /** Target dev. */
        public static final String TARGET_DEV = "target_dev";
        /** Model type. */
        public static final String MODEL_TYPE = "model_type";

        /** Creates new InterfaceData object. */
        public InterfaceData(final String type,
                             final String macAddress,
                             final String sourceBridge,
                             final String targetDev,
                             final String modelType) {
            this.type = type;
            valueMap.put(TYPE, type);
            this.macAddress = macAddress;
            valueMap.put(MAC_ADDRESS, macAddress);
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
