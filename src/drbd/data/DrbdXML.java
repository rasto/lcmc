/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
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

import drbd.gui.DrbdGraph;
import drbd.gui.resources.BlockDevInfo;
import drbd.utilities.Tools;
import drbd.utilities.ConvertCmdCallback;
import drbd.gui.resources.StringInfo;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.math.BigInteger;


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
public class DrbdXML extends XML {
    // TODO: should that not be per host?
    /** Drbd config filename. */
    private String configFile = "unknown";
    /** Map from parameter name to the default value. */
    private final Map<String, String> paramDefaultMap =
                                               new HashMap<String, String>();
    /** Map from parameter name to its type. */
    private final Map<String, String> paramTypeMap =
                                                new HashMap<String, String>();
    /** Map from parameter name to its section. */
    private final Map<String, String> paramSectionMap =
                                            new LinkedHashMap<String, String>();
    /** Map from section to this section's parameters. */
    private final Map<String, List<String>> sectionParamsMap =
                                      new LinkedHashMap<String, List<String>>();
    /** Map from perameter name to its unit name (long). */
    private final Map<String, String> paramUnitLongMap =
                                           new LinkedHashMap<String, String>();

    /** Map from perameter name to its unit name. */
    private final Map<String, String> paramDefaultUnitMap =
                                           new LinkedHashMap<String, String>();

    /** Map from perameter name to its long description. */
    private final Map<String, String> paramLongDescMap =
                                                new HashMap<String, String>();

    /** Map from perameter name to its minimum value. */
    private final Map<String, BigInteger> paramMinMap =
                                    new LinkedHashMap<String, BigInteger>();

    /** Map from perameter name to its maximum value. */
    private final Map<String, BigInteger> paramMaxMap =
                                        new LinkedHashMap<String, BigInteger>();

    /** Map from perameter name to its correct value. */
    private final Map<String, Boolean> paramCorrectValueMap =
                                                new HashMap<String, Boolean>();

    /** Map from perameter name to its items if there is a choice list. */
    private final Map<String, List<Object>> paramItemsMap =
                                    new LinkedHashMap<String, List<Object>>();
    /** List of all parameters. */
    private final List<String> parametersList =
                                                new ArrayList<String>();
    /** List of all required parameters. */
    private final List<String> requiredParametersList =
                                                    new ArrayList<String>();
    /** Map from resource option to the value. */
    private final Map<String, Map<String, String>> optionsMap =
                                    new HashMap<String, Map<String, String>>();

    /** List with drbd resources. */
    private final List<String> resourceList = new ArrayList<String>();
    /** Map from drbd resource name to the drbd device. */
    private final Map<String, String> resourceDeviceMap =
                                                new HashMap<String, String>();
    /** Map from drbd device to the drbd resource name. */
    private final Map<String, String> deviceResourceMap =
                                                new HashMap<String, String>();

    /** Map from drbd resource name and the host to the block device. */
    private final Map<String, Map<String, String>> resourceHostDiskMap =
                                    new HashMap<String, Map<String, String>>();
    /** Map from drbd resource name and the host to the ip. */
    private final Map<String, Map<String, String>> resourceHostIpMap =
                                    new HashMap<String, Map<String, String>>();
    /** Map from drbd resource name and the host to the port. */
    private final Map<String, Map<String, String>> resourceHostPortMap =
                                    new HashMap<String, Map<String, String>>();
    /** Map from drbd resource name and the host to the meta disk. */
    private final Map<String, Map<String, String>> resourceHostMetaDiskMap =
                                    new HashMap<String, Map<String, String>>();
    /** Map from drbd resource name and the host to the meta disk index. */
    private final Map<String, Map<String, String>>
                resourceHostMetaDiskIndexMap =
                                    new HashMap<String, Map<String, String>>();
    /** Map from host to the boolean value if drbd is loaded on this host. */
    private final Map<String, Boolean> hostDrbdLoadedMap =
                                                new HashMap<String, Boolean>();
    /** DRBD protocol C, that is a default. */
    private static final String PROTOCOL_C = "C / Synchronous";
    /** DRBD communication protocols. */
    public static final StringInfo[] PROTOCOLS =
        {new StringInfo("A / Asynchronous",     "A", null),
         new StringInfo("B / Semi-Synchronous", "B", null),
         new StringInfo(PROTOCOL_C,             "C", null)};
    /** DRBD single-primary mode, that is a default. */
    private static final String MODE_SINGLE_PRIMARY = "False";
    /** DRBD single and dual primary mode. */
    public static final List<Object> MODES = new ArrayList<Object>();
    static {
        MODES.add(new StringInfo("Single-Primary", MODE_SINGLE_PRIMARY, null));
        MODES.add(new StringInfo("Dual-Primary",   "True",   null));
    }

    /**
     * Prepares a new <code>DrbdXML</code> object.
     */
    public DrbdXML(final Host[] hosts) {
        super();
        addSpecialParameter("resource", "name", true);
        addSpecialParameter("resource", "device", true);
        addParameter("resource", "protocol", PROTOCOL_C, PROTOCOLS, true);
        for (Host host : hosts) {
            final String command =
                                host.getDistCommand("Drbd.getParameters",
                                                    (ConvertCmdCallback) null);

            final String output =
                        Tools.execCommand(
                                    host,
                                    command,
                                    null,  /* ExecCallback */
                                    false, /* outputVisible */
                                    Tools.getString("DrbdXML.GetParameters"));
            if (output == null) {
                return;
            }

            final String[] lines = output.split("\\r?\\n");
            final Pattern bp = Pattern.compile("^<command name=\"(.*?)\".*");
            final Pattern ep = Pattern.compile("^</command>$");
            final StringBuffer xml = new StringBuffer();
            String section = null;

            for (String line : lines) {
                //<command name="AudibleAlarm">
                // ...
                //</command>
                final Matcher m = bp.matcher(line);
                if (m.matches()) {
                    section = m.group(1);
                }
                if (section != null) {
                    xml.append(line);
                    xml.append('\n');
                    final Matcher m2 = ep.matcher(line);
                    if (m2.matches()) {
                        parseSection(section, xml.toString(), host, hosts);
                        section = null;
                        xml.delete(0, xml.length() - 1);
                    }
                }
            }
//            update(host); // TODO: don't do this here.
        }

    }

    /**
     * Returns the filename of the drbd config file.
     */
    public final String getConfigFile() {
        return configFile;
    }

    /** 
     * Returns config from server. */
    public final String getConfig(final Host host) {
        if (!host.isConnected()) {
            return null;
        }
        final String command2 = host.getDistCommand("Drbd.getConfig",
                                                    (ConvertCmdCallback) null);
        final String configString =
                      Tools.execCommand(host,
                                        command2,
                                        null,  /* ExecCallback */
                                        false, /* outputVisible */
                                        Tools.getString("DrbdXML.GetConfig"));
        return configString;
    }

    /**
     * Retrieves and updates all the data. This should be called if there is
     * a drbd event.
     */
    public final void update(final String configString) {
        if (configString != null && !configString.equals("")) {
            parseConfig(configString);
        }
    }

    /**
     * Returns all drbd parameters.
     */
    public final String[] getParameters() {
        return parametersList.toArray(new String[parametersList.size()]);
    }

    /**
     * Gets short description for the parameter.
     */
    public final String getParamShortDesc(final String param) {
        final StringBuffer name =
                                new StringBuffer(param.replaceAll("\\-", " "));
        name.replace(0, 1, name.substring(0, 1).toUpperCase());
        if (paramUnitLongMap.containsKey(param)) {
            name.append(" (" + paramUnitLongMap.get(param) + ")");
        }
        return name.toString();
    }

    /**
     * Gets long description for the parameter.
     */
    public final String getParamLongDesc(final String param) {
        return paramLongDescMap.get(param);
    }

    /**
     * Returns the long name of the unit of the specified parameter.
     */
    public final String getUnitLong(final String param) {
        return paramUnitLongMap.get(param);
    }

    /**
     * Returns the default unit of the specified parameter.
     */
    public final String getDefaultUnit(final String param) {
        return paramDefaultUnitMap.get(param);
    }

    /**
     * Returns whether the parameter has a unit prefix.
     */
    public final boolean hasUnitPrefix(final String param) {
        final String unit = getUnitLong(param);
        if (unit != null && (unit.equals("bytes")
                             || unit.equals("bytes/second"))) {
            return true;
        }
        return false;
    }

    /**
     * Returns parameter type, that is one of the following:
     *  numeric
     *  string
     *  boolean
     *  handle
     * .
     */
    public final String getParamType(final String param) {
        return paramTypeMap.get(param);
    }

    /**
     * Gets default for the parameter.
     */
    public final String getParamDefault(final String param) {
        final String defaultValue = paramDefaultMap.get(param);

        if (defaultValue == null) {
            return "";
        }
        final StringBuffer defaultValueBuf = new StringBuffer(defaultValue);
        if (hasUnitPrefix(param)) {
            final String unit = getDefaultUnit(param);
            if (unit != null) {
                defaultValueBuf.append(unit);
            }
        }
        return defaultValueBuf.toString();
    }

    /**
     * Gets preferred value for the parameter.
     */
    public final String getParamPreferred(final String param) {
        // TODO:
        return null;
    }

    /**
     * Returns section in which this param is in.
     */
    public final String getSection(final String param) {
        return paramSectionMap.get(param);
    }

    /**
     * Checks parameter according to its type. Returns false if value is wrong.
     */
    public final boolean checkParam(final String param,
                                    final String rawValue) {
        final String type = paramTypeMap.get(param);
        boolean correctValue = true;

        String value = rawValue;
        if (rawValue != null && getDefaultUnit(param) != null) {
            /* number with unit */
            final Pattern p = Pattern.compile("\\d*([kmgsKMGS])");
            final Matcher m = p.matcher(value);
            if (m.matches()) {
                /* remove unit from value */
                final String unit = m.group(1).toUpperCase();
                value = rawValue.substring(0,
                                           rawValue.length() - unit.length());
            }
        }

        if (value == null || "".equals(value)) {
            if (isRequired(param)) {
                correctValue = false;
            } else {
                correctValue = true;
            }
        } else if ("boolean".equals(type)) {
            if (!value.equals(Tools.getString("Boolean.True"))
                && !value.equals(Tools.getString("Boolean.False"))) {
                correctValue = false;
            }
        } else if ("numeric".equals(type)) {
            final Pattern p = Pattern.compile("(-?\\d+)|\\d*");
            final Matcher m = p.matcher(value);
            if (!m.matches()) {
                correctValue = false;
            } else if (paramMaxMap.get(param) != null
                       && new BigInteger(value).longValue()
                          > paramMaxMap.get(param).longValue()) {
                correctValue = false;
            } else if (paramMinMap.get(param) != null
                       && new BigInteger(value).longValue()
                          < paramMinMap.get(param).longValue()) {
                correctValue = false;
            }
        } else {
            correctValue = true;
        }
        paramCorrectValueMap.remove(param);
        paramCorrectValueMap.put(param, correctValue);
        return correctValue;
    }

    /**
     * Returns whether parameter expects integer value.
     */
    public final boolean isInteger(final String param) {
        final String type = paramTypeMap.get(param);
        return "numeric".equals(type);
    }

    /**
     * Returns whether parameter expects string value.
     */
    public final boolean isStringType(final String param) {
        final String type = paramTypeMap.get(param);
        return "string".equals(type);
    }

    /**
     * Checks in the cache if the parameter was correct.
     */
    public final boolean checkParamCache(final String param) {
        return paramCorrectValueMap.get(param).booleanValue();
    }

    /**
     * Adds parameter to the specified section. This parameter will be not
     * used in the generated config.
     */
    private void addSpecialParameter(final String section,
                                     final String param,
                                     final boolean required) {
        if (!parametersList.contains(param)) {
            parametersList.add(param);
            if (required) {
                requiredParametersList.add(param);
            }

            paramTypeMap.put(param, "string");
            paramSectionMap.put(param, section);
        }
    }

    /**
     * Add paremeter with choice combo box.
     */
    private void addParameter(final String section,
                              final String param,
                              final String defaultValue,
                              final Object[] items,
                              final boolean required) {
        addParameter(section, param, defaultValue, required);
        final List<Object> l = new ArrayList<Object>();
        for (int i = 0; i < items.length; i++) {
            if (!l.contains(items[i])) {
                l.add(items[i]);
            }
        }
        paramItemsMap.put(param, l);
        paramTypeMap.put(param, "handler");
    }

    /**
     * Adds parameter to the specified section.
     */
    private void addParameter(final String section,
                              final String param,
                              final boolean required) {
        addSpecialParameter(section, param, required);
        if (!sectionParamsMap.containsKey(section)) {
            sectionParamsMap.put(section, new ArrayList<String>());
        }
        if (!sectionParamsMap.get(section).contains(param)) {
            sectionParamsMap.get(section).add(param);
        }
    }

    /**
     * Adds parameter with a default value to the specified section.
     */
    private void addParameter(final String section,
                              final String param,
                              final String defaultValue,
                              final boolean required) {
        addParameter(section, param, required);
        paramDefaultMap.put(param, defaultValue);
    }

    /**
     * Adds parameter with the specified type.
     */
    private void addParameter(final String section,
                              final String param,
                              final String defaultValue,
                              final boolean required,
                              final String type) {
        addParameter(section, param, defaultValue, required);
        paramTypeMap.put(param, type);
    }

    /**
     * Returns array with all the sections.
     */
    public final String[] getSections() {
        return sectionParamsMap.keySet().toArray(
                                        new String[sectionParamsMap.size()]);
    }

    /**
     * Returns parameters for the specified section.
     */
    public final String[] getSectionParams(final String section) {
        final List<String> params = sectionParamsMap.get(section);
        if (params == null) {
            return new String[0];
        }
        return params.toArray(new String[params.size()]);
    }

    /**
     * Returns parameters for the global section.
     */
    public final String[] getGlobalParams() {
        final List<String> params = sectionParamsMap.get("global");
        if (params == null) {
            return new String[0];
        }
        return params.toArray(new String[params.size()]);
    }

    /**
     * Returns possible choices.
     */
    public final Object[] getPossibleChoices(final String param) {
        final List<Object> items = paramItemsMap.get(param);
        if (items == null) {
            return null;
        } else {
            return items.toArray(new Object[items.size()]);
        }
    }

    /**
     * Return whether parameter is required.
     */
    public final boolean isRequired(final String param) {
        return requiredParametersList.contains(param);
    }

    /**
     * Parses command xml for parameters and fills up the hashes.
     */
    private void parseSection(final String section,
                              final String xml,
                              final Host host,
                              final Host[] hosts) {
        final Document document = getXMLDocument(xml);

        /* get root <command> */
        final Node commandNode = getChildNode(document, "command");
        if (commandNode == null) {
            return;
        }
        final NodeList options = commandNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node optionNode = options.item(i);

            /* <option> */
            if (optionNode.getNodeName().equals("option")) {
                final String name = getAttribute(optionNode, "name");
                String type = getAttribute(optionNode, "type");

                if ("allow-two-primaries".equals(name)) {
                    paramItemsMap.put(name, MODES);
                    paramDefaultMap.put(name, MODE_SINGLE_PRIMARY);
                    type = "booleanhandler";
                } else if ("protocol".equals(name)) {
                    type = "handler";
                } else if ("handler".equals(type)) {
                    paramItemsMap.put(name, new ArrayList<Object>());
                } else if ("boolean".equals(type)) {
                    final List<Object> l = new ArrayList<Object>();
                    l.add(Tools.getString("Boolean.True"));
                    l.add(Tools.getString("Boolean.False"));
                    paramItemsMap.put(name, l);
                    paramDefaultMap.put(name, Tools.getString("Boolean.False"));
                }
                if ("fence-peer".equals(name)) {
                    final List<Object> l = new ArrayList<Object>();
                    l.add("");
                    l.add(host.getHeartbeatLibPath()
                          + "/drbd-peer-outdater -t 5");
                    l.add("/usr/lib/drbd/crm-fence-peer.sh");
                    paramItemsMap.put(name, l);
                } else if ("after-resync-target".equals(name)) {
                    final List<Object> l = new ArrayList<Object>();
                    l.add("");
                    l.add("/usr/lib/drbd/crm-unfence-peer.sh");
                    paramItemsMap.put(name, l);
                } else if ("split-brain".equals(name)) {
                    final List<Object> l = new ArrayList<Object>();
                    l.add("");
                    l.add("/usr/lib/drbd/notify-split-brain.sh root");
                    paramItemsMap.put(name, l);
                } else if ("become-primary-on".equals(name)) {
                    final List<Object> l = new ArrayList<Object>();
                    l.add("");
                    l.add("both");
                    for (final Host h : hosts) {
                        l.add(h.getName());
                    }
                    paramItemsMap.put(name, l);
                } else if ("verify-alg".equals(name)
                           || "csums-alg".equals(name)
                           || "data-integrity-alg".equals(name)
                           || "cram-hmac-alg".equals(name)) {
                    final List<Object> l = new ArrayList<Object>();
                    l.add("");
                    for (final String cr : host.getCryptoModules()) {
                        l.add(cr);
                    }
                    paramItemsMap.put(name, l);
                }
                final NodeList optionInfos = optionNode.getChildNodes();
                for (int j = 0; j < optionInfos.getLength(); j++) {
                    final Node optionInfo = optionInfos.item(j);
                    final String tag = optionInfo.getNodeName();
                    /* <min>, <max>, <handler>, <default> */
                    if ("min".equals(tag)) {
                        paramMinMap.put(name,
                                        new BigInteger(getText(optionInfo)));
                    } else if ("max".equals(tag)) {
                        paramMaxMap.put(name,
                                        new BigInteger(getText(optionInfo)));
                    } else if ("handler".equals(tag)) {
                        paramItemsMap.get(name).add(getText(optionInfo));
                        /* first value is default */
                        if (!paramDefaultMap.containsKey(name)) {
                            paramDefaultMap.put(name, getText(optionInfo));
                        }
                    } else if ("default".equals(tag)) {
                        paramDefaultMap.put(name, getText(optionInfo));
                    } else if ("unit".equals(tag)) {
                        paramUnitLongMap.put(name, getText(optionInfo));
                    } else if ("unit_prefix".equals(tag)) {
                        String option = getText(optionInfo);
                        if (!"s".equals(option)) {
                            /* "s" is an exception */
                            option = option.toUpperCase();
                        }
                        paramDefaultUnitMap.put(name, option);
                    } else if ("desc".equals(tag)) {
                        paramLongDescMap.put(name, getText(optionInfo));
                    }
                }
                paramTypeMap.put(name, type);
                if (!"global".equals(section)
                    && !parametersList.contains(name)) {
                    parametersList.add(name);
                }

                paramSectionMap.put(name, section);
                if (!sectionParamsMap.containsKey(section)) {
                    sectionParamsMap.put(section, new ArrayList<String>());
                }
                if (!sectionParamsMap.get(section).contains(name)) {
                    sectionParamsMap.get(section).add(name);
                }
            }
        }
    }

    /**
     * Parses section node and creates map with option name value pairs.
     */
    private void parseConfigSectionNode(final Node sectionNode,
                                       final Map<String, String> nameValueMap) {
        final NodeList options = sectionNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if (option.getNodeName().equals("option")) {
                final String name = getAttribute(option, "name");
                String value = getAttribute(option, "value");
                if (value == null) { /* boolean option */
                    value = Tools.getString("Boolean.True");
                } else if (hasUnitPrefix(name)) { /* with unit */
                    final Pattern p = Pattern.compile("\\d+([kmgs])");
                    final Matcher m = p.matcher(value);
                    if (m.matches()) {
                        /* uppercase unit in value */
                        final String unit = m.group(1).toUpperCase();
                        value = value.substring(0, value.length()
                                                   - unit.length())
                                + unit;
                    }
                }
                nameValueMap.remove(name);
                nameValueMap.put(name, value);
            }
        }
    }

    /**
     * Parses host node in the drbd config.
     */
    private void parseHostConfig(final String resName, final Node hostNode) {
        final String hostName = getAttribute(hostNode, "name");
        final NodeList options = hostNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if (option.getNodeName().equals("device")) {
                String device = getText(option);
                if (device != null && "".equals(device)) {
                    final String minor = getAttribute(option, "minor");
                    device = "/dev/drbd" + minor;
                }
                resourceDeviceMap.put(resName, device);
                deviceResourceMap.put(device, resName);
            } else if (option.getNodeName().equals("disk")) {
                final String disk = getText(option);
                Map<String, String> hostDiskMap =
                                            resourceHostDiskMap.get(resName);
                if (hostDiskMap == null) {
                    hostDiskMap = new HashMap<String, String>();
                    resourceHostDiskMap.put(resName, hostDiskMap);
                }
                hostDiskMap.put(hostName, disk);
            } else if (option.getNodeName().equals("address")) {
                final String ip = getText(option);
                final String port = getAttribute(option, "port");
                /* ip */
                Map<String, String> hostIpMap = resourceHostIpMap.get(resName);
                if (hostIpMap == null) {
                    hostIpMap = new HashMap<String, String>();
                    resourceHostIpMap.put(resName, hostIpMap);
                }
                hostIpMap.put(hostName, ip);
                /* port */
                Map<String, String> hostPortMap =
                                            resourceHostPortMap.get(resName);
                if (hostPortMap == null) {
                    hostPortMap = new HashMap<String, String>();
                    resourceHostPortMap.put(resName, hostPortMap);
                }
                hostPortMap.put(hostName, port);
            } else if (option.getNodeName().equals("meta-disk")
                       || option.getNodeName().equals("flexible-meta-disk")) {
                final boolean flexible =
                            option.getNodeName().equals("flexible-meta-disk");
                final String metaDisk = getText(option);
                String metaDiskIndex = null;

                if (!flexible) {
                    metaDiskIndex = getAttribute(option, "index");
                }

                if (metaDiskIndex == null) {
                    metaDiskIndex = "Flexible";
                }
                /* meta-disk */
                Map<String, String> hostMetaDiskMap =
                                        resourceHostMetaDiskMap.get(resName);
                if (hostMetaDiskMap == null) {
                    hostMetaDiskMap = new HashMap<String, String>();
                    resourceHostMetaDiskMap.put(resName, hostMetaDiskMap);
                }
                hostMetaDiskMap.put(hostName, metaDisk);

                /* meta-disk index */
                Map<String, String> hostMetaDiskIndexMap =
                                    resourceHostMetaDiskIndexMap.get(resName);
                if (hostMetaDiskIndexMap == null) {
                    hostMetaDiskIndexMap = new HashMap<String, String>();
                    resourceHostMetaDiskIndexMap.put(resName,
                                                     hostMetaDiskIndexMap);
                }
                hostMetaDiskIndexMap.put(hostName, metaDiskIndex);
            }
        }
    }

    /**
     * Returns map with hosts as keys and disks as values.
     */
    public final Map<String, String> getHostDiskMap(final String resName) {
        return resourceHostDiskMap.get(resName);
    }

    /**
     * Returns map with hosts as keys and ips as values.
     */
    public final Map<String, String> getHostIpMap(final String resName) {
        return resourceHostIpMap.get(resName);
    }

    /**
     * Gets virtual net interface port for a host and a resource.
     */
    public final String getVirtualInterfacePort(final String hostName,
                                                final String resName) {
        if (resourceHostPortMap.containsKey(resName)) {
            return resourceHostPortMap.get(resName).get(hostName);
        }
        return null;
    }

    /**
     * Gets virtual net interface for a host and a resource.
     */
    public final String getVirtualInterface(final String hostName,
                                            final String resName) {
        if (resourceHostIpMap.containsKey(resName)) {
            return resourceHostIpMap.get(resName).get(hostName);
        }
        return null;
    }

    /**
     * Gets meta-disk block device for a host and a resource.
     */
    public final String getMetaDisk(final String hostName,
                                    final String resName) {
        if (resourceHostMetaDiskMap.containsKey(resName)) {
            return resourceHostMetaDiskMap.get(resName).get(hostName);
        }
        return null;
    }

    /**
     * Gets meta-disk index for a host and a resource. Index can
     * be keyword 'flexible'. In this case 'flexible-meta-disk option
     * will be generated in the config.
     */
    public final String getMetaDiskIndex(final String hostName,
                                         final String resName) {
        if (resourceHostMetaDiskIndexMap.containsKey(resName)) {
            return resourceHostMetaDiskIndexMap.get(resName).get(hostName);
        }
        return null;
    }

    /**
     * Parses resource xml.
     */
    private void parseConfigResourceNode(final Node resourceNode,
                                 final String resName) {
        final String resProtocol = getAttribute(resourceNode, "protocol");
        if (resProtocol != null) {
            Map<String, String> nameValueMap =
                                    optionsMap.get(resName + "." + "resource");
            if (nameValueMap == null) {
                nameValueMap = new HashMap<String, String>();
            } else {
                optionsMap.remove(resName + "." + "resource");
            }

            nameValueMap.put("protocol", resProtocol);
            optionsMap.put(resName + "." + "resource", nameValueMap);
        }
        final NodeList c = resourceNode.getChildNodes();
        for (int i = 0; i < c.getLength(); i++) {
            final Node n = c.item(i);
            if (n.getNodeName().equals("host")) {
                /* <host> */
                parseHostConfig(resName, n);
            } else if (n.getNodeName().equals("section")) {
                /* <resource> */
                final String secName = getAttribute(n, "name");

                Map<String, String> nameValueMap =
                                      optionsMap.get(resName + "." + secName);
                if (nameValueMap == null) {
                    nameValueMap = new HashMap<String, String>();
                } else {
                    optionsMap.remove(resName + "." + secName);
                }

                parseConfigSectionNode(n, nameValueMap);
                optionsMap.put(resName + "." + secName, nameValueMap);
            }
        }
    }

    /**
     * Parses config xml from drbdadm dump-xml.
     */
    private void parseConfig(final String configXML) {
        final int start = configXML.indexOf("<config");
        if (start < 0) {
            return;
        }
        final Document document = getXMLDocument(configXML.substring(start));
        if (document == null) {
            return;
        }

        /* get root <config> */
        final Node configNode = getChildNode(document, "config");
        if (configNode == null) {
            return;
        }
        /* config file=".." TODO: */
        configFile = getAttribute(configNode, "file");

        final NodeList resources = configNode.getChildNodes();

        for (int i = 0; i < resources.getLength(); i++) {
            final Node resourceNode = resources.item(i);
            /* <common> */
            if (resourceNode.getNodeName().equals("common")) {
                parseConfigResourceNode(resourceNode, "Section.Common");
            }
            /* <resource> */
            if (resourceNode.getNodeName().equals("resource")) {
                final String resName = getAttribute(resourceNode, "name");
                if (!resourceList.contains(resName)) {
                    resourceList.add(resName);
                }
                parseConfigResourceNode(resourceNode, resName);
            }
        }
    }

    /**
     * Returns value from drbd config identified by resource, section and
     * option name.
     */
    public final String getConfigValue(final String res,
                                       final String section,
                                       final String optionName) {
        Map<String, String> option = optionsMap.get(res + "." + section);

        String value = null;
        if (option != null) {
            value = option.get(optionName);
        }

        if (value == null) {
            option = optionsMap.get("Section.Common." + section);
            if (option != null) {
                value = option.get(optionName);
            }
        }

        if (value == null) {
            return "";
        }
        return value;
    }

    /**
     * Returns array of resource.
     */
    public final String[] getResources() {
        return resourceList.toArray(new String[resourceList.size()]);
    }

    /**
     * Returns drbd device of the resource. Although there can be different
     * drbd devices on different hosts. We do not allow that.
     */
    public final String getDrbdDevice(final String res) {
        return resourceDeviceMap.get(res);
    }

    /**
     * Gets block device object from device number. Can return null.
     */
    private BlockDevInfo getBlockDevInfo(final String devNr,
                                         final String hostName,
                                         final DrbdGraph drbdGraph) {
        BlockDevInfo bdi = null;
        final String device = "/dev/drbd" + devNr;
        final String resName = deviceResourceMap.get(device);
        if (resName != null) {
            final Map<String, String> hostDiskMap =
                                            resourceHostDiskMap.get(resName);
            if (hostDiskMap != null) {
                final String disk = hostDiskMap.get(hostName);
                if (disk != null) {
                    bdi = drbdGraph.findBlockDevInfo(hostName, disk);
                }
            }
        }
        return bdi;
    }

    /**
     * Returns whether the drbd is loaded.
     */
    public final boolean isDrbdLoaded(final String hostName) {
        final Boolean l = hostDrbdLoadedMap.get(hostName);
        if (l != null) {
            return l.booleanValue();
        }
        return true;
    }

    /**
     * Parses events from drbd kernel module obtained via drbdsetup .. events
     * command and stores the values in the BlockDevice object.
     */
    public final boolean parseDrbdEvent(final String hostName,
                                        final DrbdGraph drbdGraph,
                                        final String rawOutput) {
        if (rawOutput == null) {
            return false;
        }

        final String output = rawOutput.trim();
        if ("".equals(output)) {
            return false;
        }
        if ("No response from the DRBD driver! Is the module loaded?".equals(
                output)) {
            if (hostDrbdLoadedMap.get(hostName)) {
                hostDrbdLoadedMap.put(hostName, false);
                return true;
            } else {
                return false;
            }
        } else {
            hostDrbdLoadedMap.put(hostName, true);
        }
        /* since drbd 8.3 there is ro: instead of st:
         */
        Pattern p =
            Pattern.compile(
                "^(\\d+)\\s+ST\\s+(\\d+)\\s+\\{\\s+cs:(\\S+)\\s+"
                + "(?:st|ro):(\\S+)/(\\S+)\\s+ds:(\\S+)/(\\S+)\\s+(\\S+).*?");
        Matcher m = p.matcher(output);
        if (m.matches()) {
            /* String counter      = m.group(1); // not used */
            final String devNr        = m.group(2);
            final String cs           = m.group(3);
            final String ro1          = m.group(4);
            final String ro2          = m.group(5);
            final String ds1          = m.group(6);
            final String ds2          = m.group(7);
            final String flags        = m.group(8);
            /* get blockdevice object from device */
            final BlockDevInfo bdi =
                                  getBlockDevInfo(devNr, hostName, drbdGraph);
            if (bdi != null) {
                if (bdi.getBlockDevice().isDifferent(cs, ro1, ds1, flags)) {
                    bdi.getBlockDevice().setConnectionState(cs);
                    bdi.getBlockDevice().setNodeState(ro1);
                    bdi.getBlockDevice().setDiskState(ds1);
                    bdi.getBlockDevice().setDrbdFlags(flags);
                    bdi.updateInfo();
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
        /* 19 SP 0 16.9 */
        p = Pattern.compile("^(\\d+)\\s+SP\\s+(\\d+)\\s(\\d+\\.\\d+).*");
        m = p.matcher(output);
        if (m.matches()) {
            /* String counter      = m.group(1); // not used */
            final String devNr        = m.group(2);
            final String synced       = m.group(3);
            final BlockDevInfo bdi =
                                   getBlockDevInfo(devNr, hostName, drbdGraph);
            if (bdi != null && bdi.getBlockDevice().isDrbd()) {
                if (Tools.areEqual(bdi.getBlockDevice().getSyncedProgress(),
                                   synced)) {
                    return false;
                } else {
                    bdi.getBlockDevice().setSyncedProgress(synced);
                    bdi.updateInfo();
                    return true;
                }
            }
            return false;
        }
        /* 19 UH 1 split-brain */
        p = Pattern.compile("^(\\d+)\\s+UH\\s+(\\d+)\\s([a-z-]+).*");
        m = p.matcher(output);
        if (m.matches()) {
            /* String counter      = m.group(1); // not used */
            final String devNr        = m.group(2);
            final String what         = m.group(3);
            Tools.debug(this, "drbd event: " + devNr + " - " + what);
            if ("split-brain".equals(what)) {
                final BlockDevInfo bdi = getBlockDevInfo(devNr,
                                                       hostName,
                                                       drbdGraph);

                if (bdi != null && bdi.getBlockDevice().isDrbd()) {
                    if (bdi.getBlockDevice().isSplitBrain()) {
                        return false;
                    } else {
                        bdi.getBlockDevice().setSplitBrain(true);
                        bdi.updateInfo();
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }
}
