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
import lcmc.common.domain.util.Tools;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.vm.domain.data.DiskData;
import lcmc.vm.domain.data.DomainData;
import lcmc.vm.domain.data.FilesystemData;
import lcmc.vm.domain.data.GraphicsData;
import lcmc.vm.domain.data.InputDevData;
import lcmc.vm.domain.data.InterfaceData;
import lcmc.vm.domain.data.ParallelData;
import lcmc.vm.domain.data.SerialData;
import lcmc.vm.domain.data.SoundData;
import lcmc.vm.domain.data.VideoData;
import lombok.val;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.base.Optional;

public class VMParser {
    private static final Logger LOG = LoggerFactory.getLogger(VMParser.class);
    private static final Pattern DISPLAY_PATTERN = Pattern.compile(".*:(\\d+)$");

    private final Map<String, DomainData> domainDataMap = Maps.newHashMap();

    private final Collection<String> domainNames = new ArrayList<String>();
    private final Map<Value, String> configsToNames = new HashMap<Value, String>();
    private final Collection<String> usedMacAddresses = new HashSet<String>();
    private final Collection<String> sourceFileDirs = new TreeSet<String>();

    private DomainData getDomainData(final String domainName) {
        DomainData domainData = domainDataMap.get(domainName);
        if (domainData == null) {
            domainData = new DomainData(domainName);
            domainDataMap.put(domainName, domainData);
        }
        return domainData;
    }
    public void parseVM(final Node vmNode, final Host definedOnHost, final Map<String, String> namesToConfigs) {
        /* one vm */
        if (vmNode == null) {
            return;
        }
        final Node infoNode = XMLTools.getChildNode(vmNode, "info");
        final String domainName = XMLTools.getAttribute(vmNode, VMParams.VM_PARAM_NAME);
        final String autostart = XMLTools.getAttribute(vmNode, VMParams.VM_PARAM_AUTOSTART);
        final String virshOptions = XMLTools.getAttribute(vmNode, VMParams.VM_PARAM_VIRSH_OPTIONS);
        val domainData = getDomainData(domainName);
        if (virshOptions != null) {
            domainData.setParameter(VMParams.VM_PARAM_VIRSH_OPTIONS, virshOptions);
        }
        if (autostart != null && "True".equals(autostart)) {
            domainData.setParameter(VMParams.VM_PARAM_AUTOSTART, definedOnHost.getName());
        } else {
            domainData.removeParameter(VMParams.VM_PARAM_AUTOSTART);
        }
        if (infoNode != null) {
            parseInfo(domainName, XMLTools.getText(infoNode));
        }
        final Node vncdisplayNode = XMLTools.getChildNode(vmNode, "vncdisplay");
        domainData.setRemotePort(-1);
        if (vncdisplayNode != null) {
            final String vncdisplay = XMLTools.getText(vncdisplayNode).trim();
            final Matcher m = DISPLAY_PATTERN.matcher(vncdisplay);
            if (m.matches()) {
                domainData.setRemotePort(Integer.parseInt(m.group(1)) + 5900);
            }
        }
        final Node configNode = XMLTools.getChildNode(vmNode, "config");
        final String type = parseConfig(configNode, domainName);
        final String configName = VmsXml.getConfigName(type, domainName);
        configsToNames.put(new StringValue(configName), domainName);
        namesToConfigs.put(domainName, configName);
    }

    public String getValue(final String name, final String param) {
        return getDomainData(name).getValue(param);
    }

    public Map<String, DiskData> getDisks(final String name) {
        return getDomainData(name).getDisksMap();
    }

    public Map<String, FilesystemData> getFilesystems(final String name) {
        return getDomainData(name).getFilesystemsMap();
    }

    public Map<String, InterfaceData> getInterfaces(final String name) {
        return getDomainData(name).getInterfacesMap();
    }

    public Map<String, InputDevData> getInputDevs(final String name) {
        return getDomainData(name).getInputDevsMap();
    }

    public Map<String, GraphicsData> getGraphicDisplays(final String name) {
        return getDomainData(name).getGraphicsDevsMap();
    }

    public Map<String, SoundData> getSounds(final String name) {
        return getDomainData(name).getSoundsMap();
    }

    public Map<String, SerialData> getSerials(final String name) {
        return getDomainData(name).getSerialsMap();
    }

    public Map<String, ParallelData> getParallels(final String name) {
        return getDomainData(name).getParallelsMap();
    }

    public Map<String, VideoData> getVideos(final String name) {
        return getDomainData(name).getVideosMap();
    }

    public Collection<String> getDomainNames() {
        return domainNames;
    }

    public int getRemotePort(final String domainName) {
        final Integer port = getDomainData(domainName).getRemotePort();
        if (port == null) {
            return -1;
        } else {
            return port;
        }
    }

    public Collection<Value> getConfigs() {
        return configsToNames.keySet();
    }

    public String getNameFromConfig(final String config) {
        return configsToNames.get(new StringValue(config));
    }

    public boolean isRunning(final String domainName) {
        final Boolean running = getDomainData(domainName).isRunning();
        if (running != null) {
            return running;
        }
        return false;
    }

    public boolean isSuspended(final String domainName) {
        final Boolean suspended = getDomainData(domainName).isSuspended();
        if (suspended != null) {
            return suspended;
        }
        return false;
    }

    /** Return set of mac addresses. */
    public Collection<String> getUsedMacAddresses() {
        return usedMacAddresses;
    }

    public Iterable<String> getSourceFileDirs() {
        return sourceFileDirs;
    }

    /** Updates all data for this domain. */
    private void parseInfo(final String domainName, final String info) {
        if (info != null) {
            boolean running = false;
            boolean suspended = false;
            for (final String line : info.split("\n")) {
                final String[] optionValue = line.split(":");
                if (optionValue.length == 2) {
                    final String option = optionValue[0].trim();
                    final String value = optionValue[1].trim();
                    if ("State".equals(option)) {
                        if ("running".equals(value) || "idle".equals(value)) {
                            running = true;
                            suspended = false;
                        } else if ("paused".equals(value)) {
                            running = true;
                            suspended = true;
                        }
                    }
                }
            }
            val domainData = getDomainData(domainName);
            domainData.setRunning(running);
            domainData.setSuspended(suspended);
        }
    }

    /** Parses the libvirt config file. */
    private String parseConfig(final Node configNode, final String nameInFilename) {
        if (configNode == null) {
            return null;
        }
        final Node domainNode = XMLTools.getChildNode(configNode, "domain");
        if (domainNode == null) {
            return null;
        }
        final String domainType = XMLTools.getAttribute(domainNode, "type");
        final NodeList options = domainNode.getChildNodes();
        boolean tabletOk = false;
        Optional<DomainData> domainData = Optional.absent();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if (VMParams.VM_PARAM_NAME.equals(option.getNodeName())) {
                final String domainName = XMLTools.getText(option);
                domainData = Optional.of(getDomainData(domainName));
                if (!domainNames.contains(domainName)) {
                    domainNames.add(domainName);
                }
                domainData.get().setParameter(VMParams.VM_PARAM_NAME, domainName);
                domainData.get().setParameter(VMParams.VM_PARAM_DOMAIN_TYPE, domainType);
                if (!domainName.equals(nameInFilename)) {
                    LOG.appWarning("parseConfig: unexpected name: " + domainName + " != " + nameInFilename);
                    return domainType;
                }
            } else if (VMParams.VM_PARAM_UUID.equals(option.getNodeName())) {
                domainData.get().setParameter(VMParams.VM_PARAM_UUID, XMLTools.getText(option));
            } else if (VMParams.VM_PARAM_VCPU.equals(option.getNodeName())) {
                domainData.get().setParameter(VMParams.VM_PARAM_VCPU, XMLTools.getText(option));
            } else if (VMParams.VM_PARAM_BOOTLOADER.equals(option.getNodeName())) {
                domainData.get().setParameter(VMParams.VM_PARAM_BOOTLOADER, XMLTools.getText(option));
            } else if (VMParams.VM_PARAM_CURRENTMEMORY.equals(option.getNodeName())) {
                domainData.get().setParameter(VMParams.VM_PARAM_CURRENTMEMORY, XMLTools.getText(option));
            } else if (VMParams.VM_PARAM_MEMORY.equals(option.getNodeName())) {
                domainData.get().setParameter(VMParams.VM_PARAM_MEMORY, XMLTools.getText(option));
            } else if ("os".equals(option.getNodeName())) {
                final NodeList osOptions = option.getChildNodes();
                int bootOption = 0;
                for (int j = 0; j < osOptions.getLength(); j++) {
                    final Node osOption = osOptions.item(j);
                    if (VMParams.OS_BOOT_NODE.equals(osOption.getNodeName())) {
                        if (bootOption == 0) {
                            domainData.get().setParameter(VMParams.VM_PARAM_BOOT, XMLTools.getAttribute(osOption, VMParams.OS_BOOT_NODE_DEV));
                        } else {
                            domainData.get().setParameter(VMParams.VM_PARAM_BOOT_2, XMLTools.getAttribute(osOption, VMParams.OS_BOOT_NODE_DEV));
                        }
                        bootOption++;
                    } else if (VMParams.VM_PARAM_LOADER.equals(osOption.getNodeName())) {
                        domainData.get().setParameter(VMParams.VM_PARAM_LOADER, XMLTools.getText(osOption));
                    } else if (VMParams.VM_PARAM_TYPE.equals(osOption.getNodeName())) {
                        domainData.get().setParameter(VMParams.VM_PARAM_TYPE, XMLTools.getText(osOption));
                        domainData.get().setParameter(VMParams.VM_PARAM_TYPE_ARCH, XMLTools.getAttribute(osOption, "arch"));
                        domainData.get().setParameter(VMParams.VM_PARAM_TYPE_MACHINE, XMLTools.getAttribute(osOption, "machine"));
                    } else if (VMParams.VM_PARAM_INIT.equals(osOption.getNodeName())) {
                        domainData.get().setParameter(VMParams.VM_PARAM_INIT, XMLTools.getText(osOption));
                    } else {
                        domainData.get().setParameter(osOption.getNodeName(), XMLTools.getText(osOption));
                    }
                }
            } else if ("features".equals(option.getNodeName())) {
                final NodeList ftrOptions = option.getChildNodes();
                for (int j = 0; j < ftrOptions.getLength(); j++) {
                    final Node ftrOption = ftrOptions.item(j);
                    if (VMParams.VM_PARAM_ACPI.equals(ftrOption.getNodeName())) {
                        domainData.get().setParameter(VMParams.VM_PARAM_ACPI, "True");
                    } else if (VMParams.VM_PARAM_APIC.equals(ftrOption.getNodeName())) {
                        domainData.get().setParameter(VMParams.VM_PARAM_APIC, "True");
                    } else if (VMParams.VM_PARAM_PAE.equals(ftrOption.getNodeName())) {
                        domainData.get().setParameter(VMParams.VM_PARAM_PAE, "True");
                    } else if (VMParams.VM_PARAM_HAP.equals(ftrOption.getNodeName())) {
                        domainData.get().setParameter(VMParams.VM_PARAM_HAP, "True");
                    }
                }
            } else if ("clock".equals(option.getNodeName())) {
                final String offset = XMLTools.getAttribute(option, "offset");
                domainData.get().setParameter(VMParams.VM_PARAM_CLOCK_OFFSET, offset);
            } else if ("cpu".equals(option.getNodeName())) {
                final String match = XMLTools.getAttribute(option, "match");
                if (!"".equals(match)) {
                    domainData.get().setParameter(VMParams.VM_PARAM_CPU_MATCH, match);
                    final NodeList cpuMatchOptions = option.getChildNodes();
                    String policy = "";
                    final Collection<String> features = new ArrayList<String>();
                    for (int j = 0; j < cpuMatchOptions.getLength(); j++) {
                        final Node cpuMatchOption = cpuMatchOptions.item(j);
                        final String op = cpuMatchOption.getNodeName();
                        if ("topology".equals(op)) {
                            domainData.get().setParameter(
                                    VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                                    XMLTools.getAttribute(cpuMatchOption, VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS));
                            domainData.get().setParameter(
                                    VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                                    XMLTools.getAttribute(cpuMatchOption, VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES));
                            domainData.get().setParameter(
                                    VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                                    XMLTools.getAttribute(cpuMatchOption, VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS));
                        } else if ("feature".equals(op)) {
                        /* asuming the same policy for all */
                            policy = XMLTools.getAttribute(cpuMatchOption, VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY);
                            features.add(XMLTools.getAttribute(cpuMatchOption, "name"));
                        } else {
                            domainData.get().setParameter(op, XMLTools.getText(cpuMatchOption));
                        }
                    }
                    if (!"".equals(policy) && !features.isEmpty()) {
                        domainData.get().setParameter(VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY, policy);
                        domainData.get().setParameter(VMParams.VM_PARAM_CPUMATCH_FEATURES, Tools.join(" ", features));
                    }
                }
            } else if (VMParams.VM_PARAM_ON_POWEROFF.equals(option.getNodeName())) {
                domainData.get().setParameter(VMParams.VM_PARAM_ON_POWEROFF, XMLTools.getText(option));
            } else if (VMParams.VM_PARAM_ON_REBOOT.equals(option.getNodeName())) {
                domainData.get().setParameter(VMParams.VM_PARAM_ON_REBOOT, XMLTools.getText(option));
            } else if (VMParams.VM_PARAM_ON_CRASH.equals(option.getNodeName())) {
                domainData.get().setParameter(VMParams.VM_PARAM_ON_CRASH, XMLTools.getText(option));
            } else if ("devices".equals(option.getNodeName())) {
                final Map<String, DiskData> devMap = new LinkedHashMap<String, DiskData>();
                final Map<String, FilesystemData> fsMap = new LinkedHashMap<String, FilesystemData>();
                final Map<String, InterfaceData> macMap = new LinkedHashMap<String, InterfaceData>();
                final Map<String, InputDevData> inputMap = new LinkedHashMap<String, InputDevData>();
                final Map<String, GraphicsData> graphicsMap = new LinkedHashMap<String, GraphicsData>();
                final Map<String, SoundData> soundMap = new LinkedHashMap<String, SoundData>();
                final Map<String, SerialData> serialMap = new LinkedHashMap<String, SerialData>();
                final Map<String, ParallelData> parallelMap = new LinkedHashMap<String, ParallelData>();
                final Map<String, VideoData> videoMap = new LinkedHashMap<String, VideoData>();
                final NodeList devices = option.getChildNodes();
                for (int j = 0; j < devices.getLength(); j++) {
                    final Node deviceNode = devices.item(j);
                    if ("emulator".equals(deviceNode.getNodeName())) {
                        domainData.get().setParameter(VMParams.VM_PARAM_EMULATOR, XMLTools.getText(deviceNode));
                    } else if ("input".equals(deviceNode.getNodeName())) {
                        final String type = XMLTools.getAttribute(deviceNode, "type");
                        final String bus = XMLTools.getAttribute(deviceNode, "bus");
                        if ("tablet".equals(type)) {
                            tabletOk = true;
                        }
                        final InputDevData inputDevData = new InputDevData(type, bus);
                        inputMap.put(type + " : " + bus, inputDevData);
                    } else if ("graphics".equals(deviceNode.getNodeName())) {
                        /** remotePort will be overwritten with virsh output */
                        final String type = XMLTools.getAttribute(deviceNode, "type");
                        final String port = XMLTools.getAttribute(deviceNode, "port");
                        final String autoport = XMLTools.getAttribute(deviceNode, "autoport");
                        final String listen = XMLTools.getAttribute(deviceNode, "listen");
                        final String passwd = XMLTools.getAttribute(deviceNode, "passwd");
                        final String keymap = XMLTools.getAttribute(deviceNode, "keymap");
                        final String display = XMLTools.getAttribute(deviceNode, "display");
                        final String xauth = XMLTools.getAttribute(deviceNode, "xauth");
                        LOG.debug2("parseConfig: type: " + type);
                        LOG.debug2("parseConfig: port: " + port);
                        LOG.debug2("parseConfig: autoport: " + autoport);
                        if ("vnc".equals(type)) {
                            if (port != null && Tools.isNumber(port)) {
                                domainData.get().setRemotePort(Integer.parseInt(port));
                            }
                            if ("yes".equals(autoport)) {
                                domainData.get().setAutoport(true);
                            } else {
                                domainData.get().setAutoport(false);
                            }
                        }
                        final GraphicsData graphicsData =
                                new GraphicsData(type, port, listen, passwd, keymap, display, xauth);
                        graphicsMap.put(VmsXml.graphicsDisplayName(type, port, display), graphicsData);
                    } else if ("disk".equals(deviceNode.getNodeName())) {
                        parseDiskNode(deviceNode, devMap);
                    } else if ("filesystem".equals(deviceNode.getNodeName())) {
                        final String type = XMLTools.getAttribute(deviceNode, "type");
                        final NodeList opts = deviceNode.getChildNodes();
                        String sourceDir = null;
                        String sourceName = null;
                        String targetDir = null;
                        for (int k = 0; k < opts.getLength(); k++) {
                            final Node optionNode = opts.item(k);
                            final String nodeName = optionNode.getNodeName();
                            if ("source".equals(nodeName)) {
                                sourceDir = XMLTools.getAttribute(optionNode, "dir");
                                sourceName = XMLTools.getAttribute(optionNode, "name");
                            } else if ("target".equals(nodeName)) {
                                targetDir = XMLTools.getAttribute(optionNode, "dir");
                            } else if (!"#text".equals(nodeName)) {
                                LOG.appWarning("parseConfig: unknown fs option: " + nodeName);
                            }
                        }
                        if (targetDir != null) {
                            final FilesystemData filesystemData =
                                    new FilesystemData(type, sourceDir, sourceName, targetDir);
                            fsMap.put(targetDir, filesystemData);
                        }
                    } else if ("interface".equals(deviceNode.getNodeName())) {
                        final String type = XMLTools.getAttribute(deviceNode, "type");
                        String macAddress = null;
                        String sourceNetwork = null;
                        String sourceBridge = null;
                        String targetDev = null;
                        String modelType = null;
                        String scriptPath = null;
                        final NodeList opts = deviceNode.getChildNodes();
                        for (int k = 0; k < opts.getLength(); k++) {
                            final Node optionNode = opts.item(k);
                            final String nodeName = optionNode.getNodeName();
                            if ("source".equals(nodeName)) {
                                sourceBridge = XMLTools.getAttribute(optionNode, "bridge");
                                sourceNetwork = XMLTools.getAttribute(optionNode, "network");
                            } else if ("target".equals(nodeName)) {
                                targetDev = XMLTools.getAttribute(optionNode, "dev");
                            } else if ("mac".equals(nodeName)) {
                                macAddress = XMLTools.getAttribute(optionNode, "address");
                            } else if ("model".equals(nodeName)) {
                                modelType = XMLTools.getAttribute(optionNode, "type");
                            } else if ("script".equals(nodeName)) {
                                scriptPath = XMLTools.getAttribute(optionNode, "path");
                            } else if (VMParams.HW_ADDRESS.equals(nodeName)) {
                            /* it's generated, ignoring. */
                            } else if (!"#text".equals(nodeName)) {
                                LOG.appWarning("parseConfig: unknown interface option: " + nodeName);
                            }
                        }
                        if (macAddress != null) {
                            final InterfaceData interfaceData = new InterfaceData(type,
                                    macAddress,
                                    sourceNetwork,
                                    sourceBridge,
                                    targetDev,
                                    modelType,
                                    scriptPath);
                            macMap.put(macAddress, interfaceData);
                            usedMacAddresses.add(macAddress);
                        }
                    } else if ("sound".equals(deviceNode.getNodeName())) {
                        final String model = XMLTools.getAttribute(deviceNode, "model");
                        final NodeList opts = deviceNode.getChildNodes();
                        for (int k = 0; k < opts.getLength(); k++) {
                            final Node optionNode = opts.item(k);
                            final String nodeName = optionNode.getNodeName();
                            if (VMParams.HW_ADDRESS.equals(nodeName)) {
                            /* it's generated, ignoring. */
                            } else if (!"#text".equals(nodeName)) {
                                LOG.appWarning("parseConfig: unknown sound option: " + nodeName);
                            }
                        }
                        if (model != null) {
                            final SoundData soundData = new SoundData(model);
                            soundMap.put(model, soundData);
                        }
                    } else if ("serial".equals(deviceNode.getNodeName())) {
                        final String type = XMLTools.getAttribute(deviceNode, "type");
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
                                sourcePath = XMLTools.getAttribute(optionNode, "path");
                                final String sourceMode = XMLTools.getAttribute(optionNode, "mode");
                                if ("bind".equals(sourceMode)) {
                                    bindSourceMode = sourceMode;
                                    bindSourceHost = XMLTools.getAttribute(optionNode, "host");
                                    bindSourceService = XMLTools.getAttribute(optionNode, "service");
                                } else if ("connect".equals(sourceMode)) {
                                    connectSourceMode = sourceMode;
                                    connectSourceHost = XMLTools.getAttribute(optionNode, "host");
                                    connectSourceService = XMLTools.getAttribute(optionNode, "service");
                                } else {
                                    LOG.appWarning("parseConfig: uknown source mode: " + sourceMode);
                                }
                            } else if ("protocol".equals(nodeName)) {
                                protocolType = XMLTools.getAttribute(optionNode, "type");
                            } else if ("target".equals(nodeName)) {
                                targetPort = XMLTools.getAttribute(optionNode, "port");
                            } else if (VMParams.HW_ADDRESS.equals(nodeName)) {
                            /* it's generated, ignoring. */
                            } else if (!"#text".equals(nodeName)) {
                                LOG.appWarning("parseConfig: unknown serial option: " + nodeName);
                            }
                        }
                        if (type != null) {
                            final SerialData serialData = new SerialData(type,
                                    sourcePath,
                                    bindSourceMode,
                                    bindSourceHost,
                                    bindSourceService,
                                    connectSourceMode,
                                    connectSourceHost,
                                    connectSourceService,
                                    protocolType,
                                    targetPort);
                            serialMap.put("serial " + targetPort + " / " + type, serialData);
                        }
                    } else if ("parallel".equals(deviceNode.getNodeName())) {
                        final String type = XMLTools.getAttribute(deviceNode, "type");
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
                                sourcePath = XMLTools.getAttribute(optionNode, "path");
                                final String sourceMode = XMLTools.getAttribute(optionNode, "mode");
                                if ("bind".equals(sourceMode)) {
                                    bindSourceMode = sourceMode;
                                    bindSourceHost = XMLTools.getAttribute(optionNode, "host");
                                    bindSourceService = XMLTools.getAttribute(optionNode, "service");
                                } else if ("connect".equals(sourceMode)) {
                                    connectSourceMode = sourceMode;
                                    connectSourceHost = XMLTools.getAttribute(optionNode, "host");
                                    connectSourceService = XMLTools.getAttribute(optionNode, "service");
                                } else {
                                    LOG.appWarning("parseConfig: unknown source mode: " + sourceMode);
                                }
                            } else if ("protocol".equals(nodeName)) {
                                protocolType = XMLTools.getAttribute(optionNode, "type");
                            } else if ("target".equals(nodeName)) {
                                targetPort = XMLTools.getAttribute(optionNode, "port");
                            } else if (VMParams.HW_ADDRESS.equals(nodeName)) {
                            /* it's generated, ignoring. */
                            } else if (!"#text".equals(nodeName)) {
                                LOG.appWarning("parseConfig: unknown parallel option: " + nodeName);
                            }
                        }
                        if (type != null) {
                            final ParallelData parallelData = new ParallelData(type,
                                    sourcePath,
                                    bindSourceMode,
                                    bindSourceHost,
                                    bindSourceService,
                                    connectSourceMode,
                                    connectSourceHost,
                                    connectSourceService,
                                    protocolType,
                                    targetPort);
                            parallelMap.put("parallel " + targetPort + " / " + type, parallelData);
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
                                modelType = XMLTools.getAttribute(optionNode, "type");
                                modelVRAM = XMLTools.getAttribute(optionNode, "vram");
                                modelHeads = XMLTools.getAttribute(optionNode, "heads");
                            } else if (VMParams.HW_ADDRESS.equals(nodeName)) {
                            /* it's generated, ignoring. */
                            } else if (!"#text".equals(nodeName)) {
                                LOG.appWarning("parseConfig: unknown video option: " + nodeName);
                            }
                        }
                        if (modelType != null) {
                            final VideoData videoData = new VideoData(modelType, modelVRAM, modelHeads);
                            videoMap.put(modelType, videoData);
                        }
                    } else if ("controller".equals(deviceNode.getNodeName())) {
                    /* it's generated, ignore */
                    } else if ("memballoon".equals(deviceNode.getNodeName())) {
                    /* it's generated, ignore */
                    } else if (!"#text".equals(deviceNode.getNodeName())) {
                        LOG.appWarning("parseConfig: unknown device: " + deviceNode.getNodeName());

                    }
                }
                domainData.get().setDisksMap(devMap);
                domainData.get().setFilesystemsMap(fsMap);
                domainData.get().setInterfacesMap(macMap);
                domainData.get().setInputDevsMap(inputMap);
                domainData.get().setGraphicsDevsMap(graphicsMap);
                domainData.get().setSoundsMap(soundMap);
                domainData.get().setSerialsMap(serialMap);
                domainData.get().setParallelsMap(parallelMap);
                domainData.get().setVideosMap(videoMap);
            }
        }
        if (!tabletOk) {
            LOG.appWarning("parseConfig: you should enable input type tablet for " + domainData.get().getDomainName());
        }
        return domainType;
    }

    /** Parse disk xml and populate the devMap. */
    private void parseDiskNode(final Node diskNode, final Map<String, DiskData> devMap) {
        final String type = XMLTools.getAttribute(diskNode, "type");
        final String device = XMLTools.getAttribute(diskNode, "device");
        final NodeList opts = diskNode.getChildNodes();
        String sourceFile = null;
        String sourceDev = null;
        String sourceProtocol = null;
        String sourceName = null;
        final Collection<String> sourceHostNames = new ArrayList<String>();
        final Collection<String> sourceHostPorts = new ArrayList<String>();
        String authUsername = null;
        String authSecretType = null;
        String authSecretUuid = null;
        String targetDev = null;
        String targetBus = null;
        String driverName = null;
        String driverType = null;
        String driverCache = null;
        boolean readonly = false;
        boolean shareable = false;
        for (int k = 0; k < opts.getLength(); k++) {
            final Node optionNode = opts.item(k);
            final String nodeName = optionNode.getNodeName();
            if ("source".equals(nodeName)) {
                sourceFile = XMLTools.getAttribute(optionNode, "file");
                final String dir = Tools.getDirectoryPart(sourceFile);
                if (dir != null) {
                    sourceFileDirs.add(dir);
                }
                sourceDev = XMLTools.getAttribute(optionNode, "dev");
                sourceProtocol = XMLTools.getAttribute(optionNode, "protocol");
                sourceName = XMLTools.getAttribute(optionNode, "name");
                getHostNodes(optionNode, sourceHostNames, sourceHostPorts);
            } else if ("auth".equals(nodeName)) {
                authUsername = XMLTools.getAttribute(optionNode, "username");
                final Node secretN = XMLTools.getChildNode(optionNode, "secret");
                if (secretN != null) {
                    authSecretType = XMLTools.getAttribute(secretN, "type");
                    authSecretUuid = XMLTools.getAttribute(secretN, "uuid");
                }

            } else if ("target".equals(nodeName)) {
                targetDev = XMLTools.getAttribute(optionNode, "dev");
                targetBus = XMLTools.getAttribute(optionNode, "bus");
            } else if ("driver".equals(nodeName)) {
                driverName = XMLTools.getAttribute(optionNode, "name");
                driverType = XMLTools.getAttribute(optionNode, "type");
                driverCache = XMLTools.getAttribute(optionNode, "cache");
            } else if ("readonly".equals(nodeName)) {
                readonly = true;
            } else if ("shareable".equals(nodeName)) {
                shareable = true;
            } else if (VMParams.HW_ADDRESS.equals(nodeName)) {
                /* it's generated, ignoring. */
            } else if (!"#text".equals(nodeName)) {
                LOG.appWarning("parseDiskNode: unknown disk option: " + nodeName);
            }
        }
        if (targetDev != null) {
            final DiskData diskData = new DiskData(type,
                    targetDev,
                    sourceFile,
                    sourceDev,
                    sourceProtocol,
                    sourceName,
                    Tools.join(", ", sourceHostNames),
                    Tools.join(", ", sourceHostPorts),
                    authUsername,
                    authSecretType,
                    authSecretUuid,
                    targetBus + '/' + device,
                    driverName,
                    driverType,
                    driverCache,
                    readonly,
                    shareable);
            devMap.put(targetDev, diskData);
        }
    }

    /**
     * Zero, one or more host nodes.
     */
    private void getHostNodes(final Node n, final Collection<String> names, final Collection<String> ports) {
        final NodeList nodes = n.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node hostN = nodes.item(i);
            if ("host".equals(hostN.getNodeName())) {
                names.add(XMLTools.getAttribute(hostN, "name"));
                ports.add(XMLTools.getAttribute(hostN, "port"));
            }
        }
    }

}
