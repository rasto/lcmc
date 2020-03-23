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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lcmc.common.domain.XMLTools;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

@Named
class VMCreator {
    private static final Logger LOG = LoggerFactory.getLogger(VMCreator.class);
    private Document doc;
    private Map<String, String> parametersMap;

    public void init(final Document doc, final Map<String, String> parametersMap) {
        this.doc = doc;
        this.parametersMap = parametersMap;
    }

    //<domain type='kvm'>
    //  <memory>524288</memory>
    //  <name>fff</name>
    //  <os>
    //    <type arch='i686' machine='pc-0.12'>hvm</type>
    //  </os>
    //</domain>
    public Element createDomain(final String uuid,
                                final String domainName,
                                final boolean needConsole,
                                final String type) {
        final var root = (Element) doc.appendChild(doc.createElement("domain"));
        root.setAttribute("type", type); /* kvm/xen */
        createTextNode(root, "uuid", uuid);
        createTextNode(root, "name", domainName);
        final var memoryNode = root.appendChild(doc.createElement("memory"));
        final var mem = Long.parseLong(parametersMap.get(VMParams.VM_PARAM_MEMORY));
        memoryNode.appendChild(doc.createTextNode(Long.toString(mem)));
        final var curMemoryNode = root.appendChild(doc.createElement("currentMemory"));
        final var curMem = Long.parseLong(parametersMap.get(VMParams.VM_PARAM_CURRENTMEMORY));
        curMemoryNode.appendChild(doc.createTextNode(Long.toString(curMem)));
        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_VCPU))
                .ifPresent(vcpu -> createTextNode(root, "vcpu", vcpu));

        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_BOOTLOADER))
                .ifPresent(bootloader -> createTextNode(root, "bootloader", bootloader));

        final var osNode = root.appendChild(doc.createElement("os"));
        final var typeNode = (Element) osNode.appendChild(doc.createElement("type"));
        typeNode.appendChild(doc.createTextNode(parametersMap.get(VMParams.VM_PARAM_TYPE)));
        typeNode.setAttribute("arch", parametersMap.get(VMParams.VM_PARAM_TYPE_ARCH));
        typeNode.setAttribute("machine", parametersMap.get(VMParams.VM_PARAM_TYPE_MACHINE));
        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_INIT))
                .filter(it -> !it.isBlank())
                .ifPresent(init -> {
            final Node initNode = osNode.appendChild(doc.createElement("init"));
            initNode.appendChild(doc.createTextNode(init));
        });
        final var bootNode = (Element) osNode.appendChild(doc.createElement(VMParams.OS_BOOT_NODE));
        bootNode.setAttribute(VMParams.OS_BOOT_NODE_DEV, parametersMap.get(VMParams.VM_PARAM_BOOT));
        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_BOOT_2))
                .filter(it -> !it.isBlank())
                .ifPresent(bootDev2 -> {
            final Element bootNode2 = (Element) osNode.appendChild(doc.createElement(VMParams.OS_BOOT_NODE));
            bootNode2.setAttribute(VMParams.OS_BOOT_NODE_DEV, parametersMap.get(VMParams.VM_PARAM_BOOT_2));
        });

        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_LOADER))
                .filter(it -> !it.isBlank())
                .ifPresent(loader -> osNode.appendChild(doc.createElement("loader"))
                                           .appendChild(doc.createTextNode(loader)));
        addFeatures(root);
        addClockOffset(root);
        addCPUMatchNode(root);

        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_ON_POWEROFF))
                .ifPresent(onPoweroff -> createTextNode(root, "on_poweroff", onPoweroff));
        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_ON_REBOOT))
                .ifPresent(onReboot -> createTextNode(root, "on_reboot", onReboot));

        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_ON_CRASH))
                .ifPresent(onCrash -> createTextNode(root, "on_crash", onCrash));
        final var emulator = parametersMap.get(VMParams.VM_PARAM_EMULATOR);
        if (emulator != null || needConsole) {
            final var devicesNode = root.appendChild(doc.createElement("devices"));
            if (needConsole) {
                final var consoleNode = (Element) devicesNode.appendChild(doc.createElement("console"));
                consoleNode.setAttribute("type", "pty");
            }
            final var emulatorNode = devicesNode.appendChild(doc.createElement("emulator"));
            emulatorNode.appendChild(doc.createTextNode(emulator));
        }
        return root;
    }

    public void modifyDomain(final Node domainNode) {
        final var xpath = XPathFactory.newInstance().newXPath();
        final Map<String, String> paths = new HashMap<>();
        paths.put(VMParams.VM_PARAM_MEMORY, "memory");
        paths.put(VMParams.VM_PARAM_CURRENTMEMORY, "currentMemory");
        paths.put(VMParams.VM_PARAM_VCPU, "vcpu");
        paths.put(VMParams.VM_PARAM_BOOTLOADER, "bootloader");
        paths.put(VMParams.VM_PARAM_BOOT, "os/boot");
        paths.put(VMParams.VM_PARAM_BOOT_2, "os/boot");
        paths.put(VMParams.VM_PARAM_TYPE, "os/type");
        paths.put(VMParams.VM_PARAM_TYPE_ARCH, "os/type");
        paths.put(VMParams.VM_PARAM_TYPE_MACHINE, "os/type");
        paths.put(VMParams.VM_PARAM_INIT, "os/init");
        paths.put(VMParams.VM_PARAM_LOADER, "os/loader");
        paths.put(VMParams.VM_PARAM_CPU_MATCH, "cpu");
        paths.put(VMParams.VM_PARAM_ACPI, "features");
        paths.put(VMParams.VM_PARAM_APIC, "features");
        paths.put(VMParams.VM_PARAM_PAE, "features");
        paths.put(VMParams.VM_PARAM_HAP, "features");
        paths.put(VMParams.VM_PARAM_CLOCK_OFFSET, "clock");
        paths.put(VMParams.VM_PARAM_ON_POWEROFF, "on_poweroff");
        paths.put(VMParams.VM_PARAM_ON_REBOOT, "on_reboot");
        paths.put(VMParams.VM_PARAM_ON_CRASH, "on_crash");
        paths.put(VMParams.VM_PARAM_EMULATOR, "devices/emulator");
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
                if (VMParams.VM_PARAM_BOOT_2.equals(param)) {
                    if (nodes.getLength() > 1) {
                        node = (Element) nodes.item(1);
                    } else {
                        node = (Element) node.getParentNode().appendChild(doc.createElement(VMParams.OS_BOOT_NODE));
                    }
                }
                final String value = parametersMap.get(param);
                if (VMParams.VM_PARAM_CPU_MATCH.equals(param)
                        || VMParams.VM_PARAM_CLOCK_OFFSET.equals(param)
                        || VMParams.VM_PARAM_ACPI.equals(param)
                        || VMParams.VM_PARAM_APIC.equals(param)
                        || VMParams.VM_PARAM_PAE.equals(param)
                        || VMParams.VM_PARAM_HAP.equals(param)) {
                    domainNode.removeChild(node);
                } else if (VMParams.VM_PARAM_BOOT.equals(param)) {
                    node.setAttribute(VMParams.OS_BOOT_NODE_DEV, value);
                } else if (VMParams.VM_PARAM_BOOT_2.equals(param)) {
                    if (value == null || "".equals(value)) {
                        node.getParentNode().removeChild(node);
                    } else {
                        node.setAttribute(VMParams.OS_BOOT_NODE_DEV, value);
                    }
                } else if (VMParams.VM_PARAM_TYPE_ARCH.equals(param)) {
                    node.setAttribute("arch", value);
                } else if (VMParams.VM_PARAM_TYPE_MACHINE.equals(param)) {
                    node.setAttribute("machine", value);
                } else if (VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS.equals(param)) {
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
            addCPUMatchNode(domainNode);
            addFeatures(domainNode);
            addClockOffset(domainNode);
        } catch (final XPathExpressionException e) {
            LOG.appError("modifyDomainXML: could not evaluate: ", e);
        }
    }

    private void createTextNode(Element root, String name, String text) {
        final var uuidNode = root.appendChild(doc.createElement(name));
        uuidNode.appendChild(doc.createTextNode(text));
    }

    private void addCPUMatchNode(final Node root) {
        final var cpuMatchNode = (Element) root.appendChild(doc.createElement("cpu"));
        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_CPU_MATCH))
                .filter(it -> !it.isBlank())
                .ifPresent(cpuMatch -> cpuMatchNode.setAttribute("match", cpuMatch));
        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_CPUMATCH_MODEL))
                .filter(it -> !it.isBlank())
                .ifPresent(model -> createTextNode(cpuMatchNode, "model", model));
        Optional.ofNullable(parametersMap.get(VMParams.VM_PARAM_CPUMATCH_VENDOR))
                .filter(it -> !it.isBlank())
                .ifPresent(vendor -> createTextNode(cpuMatchNode, "vendor", vendor));
        final var sockets = parametersMap.get(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS);
        final var cores = parametersMap.get(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES);
        final var threads = parametersMap.get(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS);
        final boolean isSockets = !"".equals(sockets);
        final boolean isCores = !"".equals(cores);
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
        final var policy = parametersMap.get(VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY);
        final var features = parametersMap.get(VMParams.VM_PARAM_CPUMATCH_FEATURES);
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

    private void addFeatures(final Node root) {
        final boolean acpi = "True".equals(parametersMap.get(VMParams.VM_PARAM_ACPI));
        final boolean apic = "True".equals(parametersMap.get(VMParams.VM_PARAM_APIC));
        final boolean pae = "True".equals(parametersMap.get(VMParams.VM_PARAM_PAE));
        final boolean hap = "True".equals(parametersMap.get(VMParams.VM_PARAM_HAP));
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

    private void addClockOffset(final Node root) {
        final var clockNode = (Element) root.appendChild(doc.createElement("clock"));
        final var offset = parametersMap.get(VMParams.VM_PARAM_CLOCK_OFFSET);
        clockNode.setAttribute("offset", offset);
        final var timer1 = (Element) clockNode.appendChild(doc.createElement("timer"));
        timer1.setAttribute("name", "pit");
        timer1.setAttribute("tickpolicy", "delay");
        final var timer2 = (Element) clockNode.appendChild(doc.createElement("timer"));
        timer2.setAttribute("name", "rtc");
        timer2.setAttribute("tickpolicy", "catchup");

        final var timer3 = (Element) clockNode.appendChild(doc.createElement("timer"));
        timer3.setAttribute("name", "hpet");
        timer3.setAttribute("present", "no");
    }

}
