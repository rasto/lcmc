/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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

package lcmc.drbd.domain;

import static java.util.Arrays.asList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import lcmc.Exceptions;
import lcmc.cluster.infrastructure.ssh.ExecCommandConfig;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.ConvertCmdCallback;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Unit;
import lcmc.common.domain.Value;
import lcmc.common.domain.XMLTools;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Access;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.drbd.ui.DrbdGraph;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.drbd.ui.resource.ProxyNetInfo;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

/**
 * This class parses xml from drbdsetup and drbdadm, stores the
 * information in the hashes and provides methods to get this
 * information.
 * The xml is obtained with drbdsetp xml command and drbdadm dump-xml.
 */
@Named
public class DrbdXml {
    private static final Logger LOG = LoggerFactory.getLogger(DrbdXml.class);
    private static final String[] EMPTY_STRING = new String[0];

    private static final Pattern UNIT_PATTERN = Pattern.compile("(\\d*)([kmgtsKMGTS]?)");
    public static final String GLOBAL_SECTION = "global";
    public static final Value PROTOCOL_A = new StringValue("A", "A / Asynchronous");
    private static final Value PROTOCOL_B = new StringValue("B", "B / Semi-Synchronous");
    public static final Value PROTOCOL_C = new StringValue("C", "C / Synchronous");
    public static final String PROTOCOL_PARAM = "protocol";
    public static final String PING_TIMEOUT_PARAM = "ping-timeout";

    private static final BigInteger KILO = new BigInteger("1024");
    private static final Value[] PROTOCOLS = {PROTOCOL_A, PROTOCOL_B, PROTOCOL_C};
    private static final Collection<String> NOT_ADVANCED_PARAMS =
            asList("rate", PROTOCOL_PARAM, PING_TIMEOUT_PARAM, "fence-peer", "wfc-timeout", "degr-wfc-timeout", "become-primary-on",
                    "timeout", "allow-two-primaries", "fencing", "after", "resync-after", "usage-count", "memlimit", "plugin-zlib",
                    "plugin-lzma");

    private static final Collection<String> IGNORE_CONFIG_ERRORS =
            asList("no resources defined!", "Can not open '/etc/drbd.conf': No such file or directory");
    private static final Map<String, AccessMode.Type> PARAM_ACCESS_TYPE = new HashMap<>();

    private static final Map<String, Value> PREFERRED_VALUES_MAP = new HashMap<>();

    /**
     * Yes / true drbd config value.
     */
    public static final Value CONFIG_YES = new StringValue("yes");
    /**
     * No / false drbd config value.
     */
    private static final Value CONFIG_NO = new StringValue("no");
    /**
     * Hardcoded defaults, for options that have it but we don't get it from the drbdsetup.
     */
    static final Map<String, Value> HARDCODED_DEFAULTS = new HashMap<>();

    private final Access access;

    static {
        PARAM_ACCESS_TYPE.put("rate", AccessMode.OP);
    }

    static {
        PREFERRED_VALUES_MAP.put(PROTOCOL_PARAM, PROTOCOL_C);
    }

    static {
        HARDCODED_DEFAULTS.put("usage-count", new StringValue());
        HARDCODED_DEFAULTS.put("disable-ip-verification", CONFIG_NO);

        HARDCODED_DEFAULTS.put(PROTOCOL_PARAM, PROTOCOL_C);
        HARDCODED_DEFAULTS.put("after-sb-0pri", new StringValue("disconnect"));
        HARDCODED_DEFAULTS.put("after-sb-1pri", new StringValue("disconnect"));
        HARDCODED_DEFAULTS.put("after-sb-2pri", new StringValue("disconnect"));
        HARDCODED_DEFAULTS.put("rr-conflict", new StringValue("disconnect"));
        HARDCODED_DEFAULTS.put("on-io-error", new StringValue("pass_on"));
        HARDCODED_DEFAULTS.put("fencing", new StringValue("dont-care"));
        HARDCODED_DEFAULTS.put("on-no-data-accessible", new StringValue("io-error"));
        HARDCODED_DEFAULTS.put("on-congestion", new StringValue("block"));
    }

    public DrbdXml(Access access, ProgressIndicator progressIndicator) {
        this.access = access;
        this.progressIndicator = progressIndicator;
    }

    public static Unit getUnitBytes(final String unitPart) {
        return new Unit("", "", "Byte" + unitPart, "Bytes" + unitPart);
    }

    public static Unit getUnitKiBytes(final String unitPart) {
        return new Unit("K", "k", "KiByte" + unitPart, "KiBytes" + unitPart);
    }

    public static Unit getUnitMiBytes(final String unitPart) {
        return new Unit("M", "m", "MiByte" + unitPart, "MiBytes" + unitPart);
    }

    public static Unit getUnitGiBytes(final String unitPart) {
        return new Unit("G", "g", "GiByte" + unitPart, "GiBytes" + unitPart);
    }

    public static Unit getUnitTiBytes(final String unitPart) {
        return new Unit("T", "t", "TiByte" + unitPart, "TiBytes" + unitPart);
    }

    public static Unit getUnitPiBytes(final String unitPart) {
        return new Unit("P", "p", "PiByte" + unitPart, "PiBytes" + unitPart);
    }

    public static Unit getUnitSectors(final String unitPart) {
        return new Unit("s", "s", "Sector" + unitPart, "Sectors" + unitPart); 
    }

    /** Returns units. */
    public Unit[] getUnits(final String param, final String unitPart) {
        final List<Unit> units = new ArrayList<>();
        if ("k".equalsIgnoreCase(getDefaultUnit(param))) {
            units.add(getUnitKi(unitPart));
            units.add(getUnitMi(unitPart));
            units.add(getUnitGi(unitPart));
        } else if ("m".equalsIgnoreCase(getDefaultUnit(param))) {
            units.add(getUnitMi(unitPart));
            units.add(getUnitGi(unitPart));
        } else if ("g".equalsIgnoreCase(getDefaultUnit(param))) {
            units.add(getUnitGi(unitPart));
        } else {
            units.add(getUnitDefault(unitPart));
            units.add(getUnitKi(unitPart));
            units.add(getUnitMi(unitPart));
            units.add(getUnitGi(unitPart));
        }
        return units.toArray(new Unit[units.size()]);
    }

    public Unit[] getByteUnits(final String param, final String unitPart) {
        final List<Unit> units = new ArrayList<>();
        if ("1".equalsIgnoreCase(getDefaultUnit(param))) {
            units.add(getUnitBytes(unitPart));
            units.add(getUnitKiBytes(unitPart));
            units.add(getUnitMiBytes(unitPart));
            units.add(getUnitGiBytes(unitPart));
            units.add(getUnitSectors(unitPart));
        } else if ("s".equalsIgnoreCase(getDefaultUnit(param))) {
            units.add(getUnitSectors(unitPart));
            units.add(getUnitBytes(unitPart));
            units.add(getUnitKiBytes(unitPart));
            units.add(getUnitMiBytes(unitPart));
            units.add(getUnitGiBytes(unitPart));
        } else if ("k".equalsIgnoreCase(getDefaultUnit(param))) {
            units.add(getUnitKiBytes(unitPart));
            units.add(getUnitMiBytes(unitPart));
            units.add(getUnitGiBytes(unitPart));
            units.add(getUnitSectors(unitPart));
        } else if ("m".equalsIgnoreCase(getDefaultUnit(param))) {
            units.add(getUnitMiBytes(unitPart));
            units.add(getUnitGiBytes(unitPart));
            units.add(getUnitSectors(unitPart));
        } else if ("g".equalsIgnoreCase(getDefaultUnit(param))) {
            units.add(getUnitGiBytes(unitPart));
            units.add(getUnitSectors(unitPart));
        }
        return units.toArray(new Unit[units.size()]);
    }

    public static Unit getUnitDefault(final String unitPart) {
        return new Unit("", "", "", "");
    }

    public static Unit getUnitKi(final String unitPart) {
        return new Unit("k", "K", "k", "k");
    }
                    
    public static Unit getUnitMi(final String unitPart) {
        return new Unit("m", "M", "m", "m");
    }

    public static Unit getUnitGi(final String unitPart) {
        return new Unit("g", "G", "g", "g");
    }

    public static Unit getUnitTi(final String unitPart) {
        return new Unit("t", "T", "t", "t");
    }

    public static Unit getUnitPi(final String unitPart) {
        return new Unit("p", "P", "p", "p");
    }

    /** Return part after '/' from the unit long description. */
    public static String getUnitPart(final String unitLong) {
        if (unitLong == null) {
            return "";
        }

        final int index = unitLong.indexOf('/');
        String unitPart = "";
        if (index > -1) {
            unitPart = unitLong.substring(index);
        }
        return unitPart;
    }

    @lombok.experimental.Accessors(fluent = true)
    public static class DrbdParam {
        @Getter
        @Setter
        private Value defaultValue;
        @Getter
        @Setter
        private List<Value> items;
        @Getter
        @Setter
        private BigInteger min;
        @Getter
        @Setter
        private BigInteger max;
        @Getter
        @Setter
        private String unitLong;
        @Getter
        @Setter
        private String defaultUnit;
        @Getter
        @Setter
        private String longDesc;
        @Getter
        @Setter
        private String type;
        @Getter
        @Setter
        private String section;
    }

    private final Map<String, DrbdParam> drbdParamMap = Maps.newHashMap();

    public DrbdParam getDrbdParam(final String param) {
        val drbdParam = drbdParamMap.get(param);
        if (drbdParam != null) {
            return drbdParam;
        }
        val newDrbdParam = new DrbdParam();
        drbdParamMap.put(param, newDrbdParam);
        return newDrbdParam;
    }


    /**
     * Map from section to this section's parameters.
     */
    private final Map<String, List<String>> sectionParamsMap = new LinkedHashMap<>();

    /**
     * Map from parameter name to its correct value.
     */
    private final Map<String, Boolean> paramCorrectValueMap = new HashMap<>();

    /**
     * List of all parameters.
     */
    private final Collection<String> parametersList = new ArrayList<>();
    /**
     * List of all global parameters.
     */
    private final Collection<String> globalParametersList = new ArrayList<>();
    /**
     * List of all required parameters.
     */
    private final Collection<String> requiredParametersList = new ArrayList<>();
    /**
     * Map from resource option to the value.
     */
    private final Map<String, Map<String, Value>> optionsMap = new HashMap<>();

    /**
     * List with drbd resources.
     */
    private final List<String> resourceList = new ArrayList<>();
    /**
     * Map from drbd resource name to the drbd device.
     */
    private final Table<String, String, String> resourceDeviceMap = TreeBasedTable.create();
    /**
     * Map from drbd device to the drbd resource name.
     */
    private final Map<String, String> deviceResourceMap = new HashMap<>();
    /**
     * Map from drbd device to the drbd volume.
     */
    private final Map<String, String> deviceVolumeMap = new HashMap<>();

    /**
     * Map from drbd resource name and the host to the block device.
     */
    private final Table<String, String, Map<String, String>> resourceHostDiskMap = HashBasedTable.create();
    /**
     * Map from drbd resource name and the host to the ip.
     */
    private final Map<String, Map<String, String>> resourceHostIpMap = new HashMap<>();
    /**
     * Map from drbd resource name and the host to the port.
     */
    private final Map<String, Map<String, String>> resourceHostPortMap = new HashMap<>();
    /**
     * Map from drbd resource name and the host to the family.
     */
    private final Map<String, Map<String, String>> resourceHostFamilyMap = new HashMap<>();
    /**
     * Map from drbd resource name and the host to the meta disk.
     */
    private final Table<String, String, Map<String, String>> resourceHostMetaDiskMap = HashBasedTable.create();
    /**
     * Map from drbd resource name and the host to the meta disk index.
     */
    private final Table<String, String, Map<String, String>> resourceHostMetaDiskIndexMap = HashBasedTable.create();
    /**
     * Map from resource and host to the proxy information.
     */
    private final Table<String, String, HostProxy> resourceHostProxyMap = HashBasedTable.create();
    /**
     * Set of all proxy hosts.
     */
    private final Collection<String> proxyHostNames = new LinkedHashSet<>();
    /**
     * Map from host to the boolean value if drbd is loaded on this host.
     */
    private final Map<String, Boolean> hostDrbdLoadedMap = new HashMap<>();
    private boolean unknownSections = false;
    private String oldConfig = null;
    private final ProgressIndicator progressIndicator;

    public void init(final Host[] hosts, final Map<Host, String> drbdParameters) {
        addSpecialParameter("resource", "name", true);
        for (final Host host : hosts) {
            final String output;
            if (drbdParameters.get(host) == null) {
                output = updateDrbdParameters(host);
                drbdParameters.put(host, output);
                if (output == null) {
                    return;
                }
            } else {
                output = drbdParameters.get(host);
            }
            /* TODO: move this part somewhere else, it should be called
               once per invocation or interactively. (drbd-get-xml) */
            parseDrbdParameters(host, output, hosts);
        }
    }

    public final String updateDrbdParameters(final Host host) {
        final String command = host.getHostParser()
                                   .getDistCommand("Drbd.getParameters", (ConvertCmdCallback) null);
        final var ret = host.captureCommand(new ExecCommandConfig().command(command)
                                                                   .silentCommand()
                                                                   .silentOutput());
        if (ret.getExitCode() != 0) {
            return null;
        }
        return ret.getOutput();
    }

    public final void parseDrbdParameters(final Host host, final String output, final Host[] hosts) {
        final String[] lines = output.split("\\r?\\n");
        final Pattern bp = Pattern.compile("^<command name=\"(.*?)\".*");
        final Pattern ep = Pattern.compile("^</command>$");
        final StringBuilder xml = new StringBuilder();
        String section = null;

        for (final String line : lines) {
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
                    xml.delete(0, xml.length());
                }
            }
        }
        if (!parametersList.contains(PROTOCOL_PARAM)) {
            /* prior 8.4 */
            addParameter("resource", PROTOCOL_PARAM, new StringValue(), PROTOCOLS, true);
        }

        val drbdParam = getDrbdParam("after-sb-0pri");
        if (drbdParam.items() != null) {
            for (final Host h : hosts) {
                drbdParam.items().add(new StringValue("discard-node-" + h.getName()));
            }
        }
    }

    /** Returns config from server. */
    public String getConfig(final Host host) {
        if (!host.isConnected()) {
            return null;
        }
        final String command2 = host.getHostParser()
                                    .getDistCommand("Drbd.getConfig", (ConvertCmdCallback) null);
        final var ret = host.captureCommand(new ExecCommandConfig().command(command2)
                                                                   .silentCommand()
                                                                   .silentOutput());
        if (ret.getExitCode() == 0) {
            final StringBuffer confSB = new StringBuffer(ret.getOutput());
            return host.getHostParser().getOutput("drbd", confSB);
        }
        return null;
    }

    /**
     * Retrieves and updates all the data. This should be called if there is
     * a drbd event.
     */
    public void update(final String configString) {
        if (configString != null && !configString.isEmpty()) {
            oldConfig = configString;
            parseConfig(configString);
        }
    }

    /** Returns all drbd parameters. */
    public Iterable<String> getParameters() {
        return parametersList;
    }

    public String getParamShortDesc(final String param) {
        final StringBuilder name = new StringBuilder(param.replaceAll("-", " "));
        name.replace(0, 1, name.substring(0, 1).toUpperCase());
        if (getDrbdParam(param).unitLong() != null) {
            name.append(" (").append(getDrbdParam(param).unitLong()).append(')');
        }
        return name.toString();
    }

    public String getParamLongDesc(final String param) {
        return getDrbdParam(param).longDesc();
    }

    public String getUnitLong(final String param) {
        return getDrbdParam(param).unitLong();
    }

    public String getDefaultUnit(String param) {
        return getDrbdParam(param).defaultUnit();
    }

    public boolean hasUnitPrefix(final String param) {
        final String unit = getDrbdParam(param).unitLong();
        return getDrbdParam(param).defaultUnit() != null
               && (unit == null || "bytes".equals(unit) || "bytes/second".equals(unit));
    }

    /**
     * Returns parameter type, that is one of the following:
     *  numeric
     *  string
     *  boolean
     *  handle
     * .
     */
    public String getParamType(final String param) {
        return getDrbdParam(param).type();
    }

    public Value getParamDefault(final String param) {
        return getDrbdParam(param).defaultValue();
    }

    public Value getParamPreferred(final String param) {
        return PREFERRED_VALUES_MAP.get(param);
    }

    public String getSection(final String param) {
        return getDrbdParam(param).section();
    }

    /**
     * Checks parameter according to its type. Returns false if value is wrong.
     */
    public boolean checkParam(final String param, final Value rawValue) {
        final String type = getDrbdParam(param).type();
        boolean correctValue = true;

        if (rawValue == null || rawValue.isNothingSelected()) {
            correctValue = !isRequired(param);
        } else if ("boolean".equals(type)) {
            if (!rawValue.equals(CONFIG_YES)
                && !rawValue.equals(CONFIG_NO)) {
                correctValue = false;
            }
        } else if ("numeric".equals(type)) {
            final Pattern p = Pattern.compile("(-?\\d+)|\\d*");
            final Matcher m = p.matcher(rawValue.getValueForConfig());
            if (!m.matches()) {
                correctValue = false;
            } else if (rawValue.getUnit() == null || !"s".equalsIgnoreCase(rawValue.getUnit().getShortName())) {
                //TODO: skipping check for sectors, the size of sector
                // must be determined first.
                final BigInteger value = convertToUnit(param, rawValue);

                final BigInteger min = getDrbdParam(param).min();
                final BigInteger max = getDrbdParam(param).max();

                if (min != null && value.compareTo(min) < 0) {
                    correctValue = false;
                }

                if (max != null && value.compareTo(max) > 0) {
                    correctValue = false;
                }
            }
        } else {
            correctValue = true;
        }
        paramCorrectValueMap.put(param, correctValue);
        return correctValue;
    }

    public boolean isInteger(final String param) {
        final String type = getDrbdParam(param).type();
        return "numeric".equals(type);
    }

    public boolean isLabel(final String param) {
        return false;
    }

    public boolean isStringType(final String param) {
        final String type = getDrbdParam(param).type();
        return "string".equals(type);
    }

    /**
     * Adds parameter to the specified section. This parameter will be not
     * used in the generated config.
     */
    private void addSpecialParameter(final String section, final String param, final boolean required) {
        if (!parametersList.contains(param)) {
            parametersList.add(param);
            if (required) {
                requiredParametersList.add(param);
            }

            getDrbdParam(param).type("string");
            getDrbdParam(param).section(section);
        }
    }

    /**
     * Add parameter with choice combo box.
     */
    private void addParameter(final String section, final String param, final Value defaultValue, final Value[] items,
            final boolean required) {
        addParameter(section, param, defaultValue, required);
        final List<Value> l = new ArrayList<>();
        for (final Value item : items) {
            if (!l.contains(item)) {
                l.add(item);
            }
        }
        getDrbdParam(param).items(l);
        getDrbdParam(param).type("handler");
    }

    private void addParameter(final String section, final String param, final boolean required) {
        addSpecialParameter(section, param, required);
        sectionParamsMap.put(section, new ArrayList<>());
        sectionParamsMap.get(section)
                        .add(param);
    }

    private void addParameter(final String section,
                              final String param,
                              final Value defaultValue,
                              final boolean required) {
        addParameter(section, param, required);
        getDrbdParam(param).defaultValue(defaultValue);
    }

    public String[] getSections() {
        return sectionParamsMap.keySet().toArray(new String[sectionParamsMap.size()]);
    }

    public String[] getSectionParams(final String section) {
        final List<String> params = sectionParamsMap.get(section);
        if (params == null) {
            return EMPTY_STRING;
        }
        return params.toArray(new String[params.size()]);
    }

    public Iterable<String> getGlobalParams() {
        return globalParametersList;
    }

    public Value[] getPossibleChoices(final String param) {
        final List<Value> items = getDrbdParam(param).items();
        if (items == null) {
            return null;
        } else {
            return items.toArray(new Value[items.size()]);
        }
    }

    public boolean isRequired(final String param) {
        return requiredParametersList.contains(param);
    }

    public boolean isAdvanced(final String param) {
        return !isRequired(param) && !NOT_ADVANCED_PARAMS.contains(param);
    }

    public AccessMode.Type getAccessType(final String param) {
        final AccessMode.Type at = PARAM_ACCESS_TYPE.get(param);
        if (at == null) {
          return AccessMode.Type.ADMIN;
        }
        return at;
    }

    private void parseConfigGlobalNode(final Node globalNode, final Map<String, Value> nameValueMap) {
        final NodeList options = globalNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            final String nodeName = option.getNodeName();
            if ("dialog-refresh".equals(nodeName)) {
                final String dialogRefresh = XMLTools.getAttribute(option, "refresh");
                nameValueMap.put(nodeName, new StringValue(dialogRefresh));
            } else if ("minor-count".equals(nodeName)) {
                final String minorCount = XMLTools.getAttribute(option, "count");
                nameValueMap.put(nodeName, new StringValue(minorCount));
            } else if ("disable-ip-verification".equals(nodeName)) {
                nameValueMap.put(nodeName, CONFIG_YES);
            } else if ("usage-count".equals(nodeName)) {
                /* does not come from drbdadm */
                /* TODO: "count" is guessed. */
                final String usageCount = XMLTools.getAttribute(option, "count");
                if (usageCount != null) {
                    nameValueMap.put(nodeName, new StringValue(usageCount));
                }
            }
        }
    }

    /** Parses command xml for parameters and fills up the hashes. */
    private void parseSection(final String section, final String xml, final Host host, final Host[] hosts) {
        final Document document = XMLTools.getXMLDocument(xml);

        /* get root <command> */
        final Node commandNode = XMLTools.getChildNode(document, "command");
        if (commandNode == null) {
            return;
        }
        final NodeList options = commandNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node optionNode = options.item(i);

            /* <option> */
            if ("option".equals(optionNode.getNodeName())) {
                final String param = XMLTools.getAttribute(optionNode, "name");
                final String type = XMLTools.getAttribute(optionNode, "type");
                if ("flag".equals(type)) {
                    /* ignore flags */
                    continue;
                }
                if ("handler".equals(type)) {
                    final List<Value> items = new ArrayList<>();
                    items.add(new StringValue());
                    getDrbdParam(param).items(items);
                    getDrbdParam(param).defaultValue(HARDCODED_DEFAULTS.get(param));
                } else if ("boolean".equals(type)) {
                    final List<Value> items = new ArrayList<>();
                    items.add(CONFIG_YES);
                    items.add(CONFIG_NO);
                    getDrbdParam(param).items(items);
                    getDrbdParam(param).defaultValue(CONFIG_NO);
                }
                if ("fence-peer".equals(param)) {
                    final List<Value> items = new ArrayList<>();
                    items.add(new StringValue());
                    if (host.getArch() != null && !host.getArch().isEmpty()) {
                        items.add(new StringValue(host.getHeartbeatLibPath() + "/drbd-peer-outdater -t 5"));
                    }
                    items.add(new StringValue("/usr/lib/drbd/crm-fence-peer.sh"));
                    getDrbdParam(param).items(items);
                } else if ("after-resync-target".equals(param)) {
                    final List<Value> items = new ArrayList<>();
                    items.add(new StringValue());
                    items.add(new StringValue("/usr/lib/drbd/crm-unfence-peer.sh"));
                    getDrbdParam(param).items(items);
                } else if ("split-brain".equals(param)) {
                    final List<Value> items = new ArrayList<>();
                    items.add(new StringValue());
                    items.add(new StringValue("/usr/lib/drbd/notify-split-brain.sh root"));
                    getDrbdParam(param).items(items);
                } else if ("become-primary-on".equals(param)) {
                    final List<Value> items = new ArrayList<>();
                    items.add(new StringValue());
                    items.add(new StringValue("both"));
                    for (final Host h : hosts) {
                        items.add(new StringValue(h.getName()));
                    }
                    getDrbdParam(param).items(items);
                } else if ("verify-alg".equals(param) || "csums-alg".equals(param) || "data-integrity-alg".equals(param)
                           || "cram-hmac-alg".equals(param)) {
                    final List<Value> items = new ArrayList<>();
                    items.add(new StringValue());
                    for (final String cr : host.getAvailableCryptoModules()) {
                        items.add(new StringValue(cr));
                    }
                    getDrbdParam(param).items(items);
                }
                final NodeList optionInfos = optionNode.getChildNodes();
                for (int j = 0; j < optionInfos.getLength(); j++) {
                    final Node optionInfo = optionInfos.item(j);
                    final String tag = optionInfo.getNodeName();
                    /* <min>, <max>, <handler>, <default> */
                    if ("min".equals(tag)) {
                        final Value minValue = new StringValue(XMLTools.getText(optionInfo),
                                                               parseUnit(param, getDefaultUnit(param)));
                        getDrbdParam(param).min(convertToUnit(param, minValue));
                    } else if ("max".equals(tag)) {
                        final Value maxValue = new StringValue(XMLTools.getText(optionInfo),
                                                               parseUnit(param, getDefaultUnit(param)));
                        getDrbdParam(param).max(convertToUnit(param, maxValue));
                    } else if ("handler".equals(tag)) {
                        getDrbdParam(param).items().add(new StringValue(XMLTools.getText(optionInfo)));
                    } else if ("default".equals(tag)) {
                        getDrbdParam(param).defaultValue(new StringValue(XMLTools.getText(optionInfo),
                                                                  parseUnit(param, getDefaultUnit(param))));
                    } else if ("unit".equals(tag)) {
                        getDrbdParam(param).unitLong(XMLTools.getText(optionInfo));
                    } else if ("unit_prefix".equals(tag)) {
                        if (!"after".equals(param)
                            && !"resync-after".equals(param)) {
                            String option = XMLTools.getText(optionInfo);
                            if (!"s".equals(option)) {
                                /* "s" is an exception */
                                option = option.toUpperCase(Locale.US);
                            }
                            if ("1".equals(option)) {
                                option = "";
                            }
                            getDrbdParam(param).defaultUnit(option);
                        }
                    } else if ("desc".equals(tag)) {
                        getDrbdParam(param).longDesc(XMLTools.getText(optionInfo));
                    }
                }
                getDrbdParam(param).type(type);
                if (!GLOBAL_SECTION.equals(section)
                    && !parametersList.contains(param)) {
                    parametersList.add(param);
                }
                if (!"resource".equals(section)
                    && !globalParametersList.contains(param)
                    && !("syncer".equals(section) && "after".equals(param))
                    && !"resync-after".equals(param)) {
                    globalParametersList.add(param);
                }

                getDrbdParam(param).section(section);
                if (!sectionParamsMap.containsKey(section)) {
                    sectionParamsMap.put(section, new ArrayList<>());
                }
                if (!sectionParamsMap.get(section).contains(param)) {
                    sectionParamsMap.get(section).add(param);
                }
            }
        }
    }

    /** Parses section node and creates map with option name value pairs. */
    private void parseConfigSectionNode(final Node sectionNode, final Map<String, Value> nameValueMap) {
        final NodeList options = sectionNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if ("option".equals(option.getNodeName())) {
                final String name = XMLTools.getAttribute(option, "name");
                final String valueS = XMLTools.getAttribute(option, "value");

                final Value value;
                if (valueS == null) { /* boolean option */
                    value = CONFIG_YES;
                } else if (hasUnitPrefix(name)) {
                    value = parseValue(name, valueS);
                } else {
                    value = new StringValue(valueS);
                }
                nameValueMap.put(name, value);
            } else if ("section".equals(option.getNodeName())) {
                final String name = XMLTools.getAttribute(option, "name");
                if ("plugin".equals(name)) {
                    parseProxyPluginNode(option.getChildNodes(), nameValueMap);
                }
            }
        }
    }

    /** Parse proxy XML plugin section. */
    private void parseProxyPluginNode(final NodeList options, final Map<String, Value> nameValueMap) {
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if ("option".equals(option.getNodeName())) {
                final String nameValues = XMLTools.getAttribute(option, "name");
                final int spacePos = nameValues.indexOf(' ');
                final String name;
                final String value;
                if (spacePos > 0) {
                    name = DrbdProxy.PLUGIN_PARAM_PREFIX + nameValues.substring(0, spacePos);
                    value = nameValues.substring(spacePos + 1);
                } else {
                    /* boolean */
                    name = DrbdProxy.PLUGIN_PARAM_PREFIX + nameValues;
                    value = CONFIG_YES.getValueForConfig();
                }
                nameValueMap.put(name, new StringValue(value));
            }
        }
    }

    /** Parses host node in the drbd config. */
    private void parseHostConfig(final String resName, final Node hostNode) {
        final String hostName = XMLTools.getAttribute(hostNode, "name");
        parseVolumeConfig(hostName, resName, hostNode); /* before 8.4 */
        final NodeList options = hostNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if ("volume".equals(option.getNodeName())) {
                parseVolumeConfig(hostName, resName, option);
            } else if ("address".equals(option.getNodeName())) {
                parseAddress(resName, hostName, option);
            } else if ("proxy".equals(option.getNodeName())) {
                parseProxyHostConfig(hostName, resName, option);
            }
        }
    }

    private void parseAddress(String resName, String hostName, Node option) {
        final String ip = XMLTools.getText(option);
        final String port = XMLTools.getAttribute(option, "port");
        final String family = XMLTools.getAttribute(option, "family");
        /* ip */
        Map<String, String> hostIpMap = resourceHostIpMap.computeIfAbsent(resName, k -> new HashMap<>());
        hostIpMap.put(hostName, ip);
        /* port */
        Map<String, String> hostPortMap = resourceHostPortMap.computeIfAbsent(resName, k -> new HashMap<>());
        hostPortMap.put(hostName, port);
        /* family */
        Map<String, String> hostFamilyMap = resourceHostFamilyMap.computeIfAbsent(resName, k -> new HashMap<>());
        hostFamilyMap.put(hostName, family);
    }

    /** Parses host node in the drbd config. */
    private void parseVolumeConfig(final String hostName, final String resName, final Node volumeNode) {
        String volumeNr = XMLTools.getAttribute(volumeNode, "vnr");
        if (volumeNr == null) {
            volumeNr = "0";
        }
        final NodeList options = volumeNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if ("device".equals(option.getNodeName())) {
                String device = XMLTools.getText(option);
                if (device != null && device.isEmpty()) {
                    final String minor = XMLTools.getAttribute(option, "minor");
                    device = "/dev/drbd" + minor;
                }
                resourceDeviceMap.put(resName, volumeNr, device);
                deviceResourceMap.put(device, resName);
                deviceVolumeMap.put(device, volumeNr);
            } else if ("disk".equals(option.getNodeName())) {
                final String disk = XMLTools.getText(option);
                Map<String, String> hostDiskMap = resourceHostDiskMap.get(resName, volumeNr);
                if (hostDiskMap == null) {
                    hostDiskMap = new HashMap<>();
                    resourceHostDiskMap.put(resName, volumeNr, hostDiskMap);
                }
                hostDiskMap.put(hostName, disk);
            } else if ("meta-disk".equals(option.getNodeName())
                       || "flexible-meta-disk".equals(option.getNodeName())) {
                final boolean flexible = "flexible-meta-disk".equals(option.getNodeName());
                final String metaDisk = XMLTools.getText(option);
                String metaDiskIndex = null;

                if (!flexible) {
                    metaDiskIndex = XMLTools.getAttribute(option, "index");
                }

                if (metaDiskIndex == null) {
                    metaDiskIndex = "Flexible";
                }
                /* meta-disk */
                Map<String, String> hostMetaDiskMap = resourceHostMetaDiskMap.get(resName, volumeNr);
                if (hostMetaDiskMap == null) {
                    hostMetaDiskMap = new HashMap<>();
                    resourceHostMetaDiskMap.put(resName, volumeNr, hostMetaDiskMap);
                }
                hostMetaDiskMap.put(hostName, metaDisk);

                /* meta-disk index */
                Map<String, String> hostMetaDiskIndexMap = resourceHostMetaDiskIndexMap.get(resName, volumeNr);
                if (hostMetaDiskIndexMap == null) {
                    hostMetaDiskIndexMap = new HashMap<>();
                    resourceHostMetaDiskIndexMap.put(resName, volumeNr, hostMetaDiskIndexMap);
                }
                hostMetaDiskIndexMap.put(hostName, metaDiskIndex);
            } else if ("address".equals(option.getNodeName())) {
                /* since 8.4, it's outside of volume */
                parseAddress(resName, hostName, option);
            } else if ("proxy".equals(option.getNodeName())) {
                parseProxyHostConfig(hostName, resName, option);
            }
        }
    }

    private void parseProxyHostConfig(final String hostName, final String resName, final Node proxyNode) {
        final String proxyHostName = XMLTools.getAttribute(proxyNode, "hostname");
        final NodeList options = proxyNode.getChildNodes();
        String insideIp = null;
        String insidePort = null;
        String outsideIp = null;
        String outsidePort = null;
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if ("inside".equals(option.getNodeName())) {
                insideIp = XMLTools.getText(option);
                insidePort = XMLTools.getAttribute(option, "port");
            } else if ("outside".equals(option.getNodeName())) {
                outsideIp = XMLTools.getText(option);
                outsidePort = XMLTools.getAttribute(option, "port");
            }
        }
        resourceHostProxyMap.put(resName, hostName,
                new HostProxy(proxyHostName, new StringValue(insideIp), new StringValue(insidePort), new StringValue(outsideIp),
                        new StringValue(outsidePort)));
        proxyHostNames.add(proxyHostName);
    }

    public Map<String, String> getHostDiskMap(final String resName, final String volumeNr) {
        return resourceHostDiskMap.get(resName, volumeNr);
    }

    public String getVirtualInterfacePort(final String hostName, final String resName) {
        if (resourceHostPortMap.containsKey(resName)) {
            return resourceHostPortMap.get(resName).get(hostName);
        }
        return null;
    }

    public String getVirtualInterfaceFamily(final String hostName, final String resName) {
        if (resourceHostFamilyMap.containsKey(resName)) {
            return resourceHostFamilyMap.get(resName).get(hostName);
        }
        return null;
    }

    public String getVirtualInterface(final String hostName, final String resName) {
        if (resourceHostIpMap.containsKey(resName)) {
            final String ip = resourceHostIpMap.get(resName).get(hostName);
            final HostProxy hostProxy = getHostProxy(hostName, resName);
            if (hostProxy == null) {
                return ip;
            }
            final String proxyHostName = hostProxy.getProxyHostName();
            return ProxyNetInfo.displayString(ip, hostName, proxyHostName);
        }
        return null;
    }

    public HostProxy getHostProxy(final String hostName, final String resName) {
        return resourceHostProxyMap.get(resName, hostName);
    }

    public Iterable<String> getProxyHostNames() {
        return proxyHostNames;
    }

    public String getMetaDisk(final String hostName, final String resName, final String volumeNr) {
        final Map<String, String> hostMetaDiskMap = resourceHostMetaDiskMap.get(resName, volumeNr);
        if (hostMetaDiskMap != null) {
            return hostMetaDiskMap.get(hostName);
        }
        return null;
    }

    /**
     * Gets meta-disk index for a host and a resource. Index can
     * be keyword 'flexible'. In this case 'flexible-meta-disk option
     * will be generated in the config.
     */
    public String getMetaDiskIndex(final String hostName, final String resName, final String volumeNr) {
        final Map<String, String> hostMetaDiskIndexMap = resourceHostMetaDiskIndexMap.get(resName, volumeNr);
        if (hostMetaDiskIndexMap != null) {
            return hostMetaDiskIndexMap.get(hostName);
        }
        return null;
    }

    /** Parses resource xml. */
    private void parseConfigResourceNode(final Node resourceNode, final String resName) {
        final String resProtocol = XMLTools.getAttribute(resourceNode, PROTOCOL_PARAM);
        if (resProtocol != null) {
            Map<String, Value> nameValueMap = optionsMap.get(resName + '.' + "resource");
            if (nameValueMap == null) {
                nameValueMap = new HashMap<>();
            } else {
                optionsMap.remove(resName + '.' + "resource");
            }

            nameValueMap.put(PROTOCOL_PARAM, new StringValue(resProtocol));
            optionsMap.put(resName + '.' + "resource", nameValueMap);
        }
        final NodeList c = resourceNode.getChildNodes();
        for (int i = 0; i < c.getLength(); i++) {
            final Node n = c.item(i);
            if ("host".equals(n.getNodeName())) {
                /* <host> */
                parseHostConfig(resName, n);
            } else if ("section".equals(n.getNodeName())
                       || ("#text".equals(n.getNodeName()) && !"".equals(n.getNodeValue().trim()))) {
                final String secName;
                if ("#text".equals(n.getNodeName())) {
                    secName = "proxy";
                    /* workaround for broken proxy xml in common section
                       at least till drbd 8.4.2 */
                    Map<String, Value> nameValueMap = optionsMap.get(resName + '.' + secName);
                    if (nameValueMap == null) {
                        nameValueMap = new HashMap<>();
                    } else {
                        optionsMap.remove(resName + '.' + secName);
                    }
                    try {
                        final boolean isProxy = DrbdProxy.parse(this, n.getNodeValue(), nameValueMap);
                        if (!isProxy) {
                            continue;
                        }
                    } catch (final Exceptions.DrbdConfigException e) {
                        LOG.appWarning("parseConfigResourceNode: " + e.getMessage());
                        LOG.appWarning("parseConfigResourceNode: " + n.getNodeValue());
                        continue;
                    }
                    optionsMap.put(resName + '.' + secName, nameValueMap);
                } else {
                    /* <resource> */
                    secName = XMLTools.getAttribute(n, "name");

                    Map<String, Value> nameValueMap = optionsMap.get(resName + '.' + secName);
                    if (nameValueMap == null) {
                        nameValueMap = new HashMap<>();
                    } else {
                        optionsMap.remove(resName + '.' + secName);
                    }

                    parseConfigSectionNode(n, nameValueMap);
                    optionsMap.put(resName + '.' + secName, nameValueMap);
                }
                if (!sectionParamsMap.containsKey(secName)
                    && !sectionParamsMap.containsKey(secName + "-options")) {
                    LOG.appWarning("parseConfigResourceNode: unknown section: " + secName);
                    if (!unknownSections) {
                        /* unknown section, so it's not removed. */
                        progressIndicator.progressIndicatorFailed("DRBD: unknown section: " + secName);
                        unknownSections = true;
                    }
                }
            }
        }
    }

    /** Parses config xml from drbdadm dump-xml. */
    private void parseConfig(final String configXML) {
        final int start = configXML.indexOf("<config");
        if (start < 0) {
            final String c = configXML.trim();
            if (!c.isEmpty() && !IGNORE_CONFIG_ERRORS.contains(c)) {
                LOG.error("parseConfig: error: " + c);
            }
            return;
        }
        final Document document = XMLTools.getXMLDocument(configXML.substring(start));
        if (document == null) {
            return;
        }

        /* get root <config> */
        final Node configNode = XMLTools.getChildNode(document, "config");
        if (configNode == null) {
            return;
        }
        /* config file=".." TODO: */

        final NodeList resources = configNode.getChildNodes();
        Map<String, Value> globalNameValueMap = optionsMap.computeIfAbsent(GLOBAL_SECTION, k -> new HashMap<>());
        globalNameValueMap.put("usage-count", CONFIG_YES);
        globalNameValueMap.put("disable-ip-verification", CONFIG_NO);

        for (int i = 0; i < resources.getLength(); i++) {
            final Node resourceNode = resources.item(i);
            /* <global> */
            if (resourceNode.getNodeName().equals(GLOBAL_SECTION)) {
                parseConfigGlobalNode(resourceNode, globalNameValueMap);
            }
            /* <common> */
            if ("common".equals(resourceNode.getNodeName())) {
                parseConfigResourceNode(resourceNode, "Section.Common");
            }
            /* <resource> */
            if ("resource".equals(resourceNode.getNodeName())) {
                final String resName = XMLTools.getAttribute(resourceNode, "name");
                if (!resourceList.contains(resName)) {
                    resourceList.add(resName);
                }
                parseConfigResourceNode(resourceNode, resName);
            }
        }
    }

    /** Returns value from drbd global config identified by option name. */
    public Value getGlobalConfigValue(final String optionName) {
        final Map<String, Value> option = optionsMap.get(GLOBAL_SECTION);
        if (option != null) {
            return option.get(optionName);
        }
        return null;
    }

    /**
     * Returns value from drbd config identified by resource, section and
     * option name.
     */
    public Value getConfigValue(final String res, final String section, final String optionName) {
        final Map<String, Value> option = optionsMap.get(res + '.' + section);

        Value value = null;
        if (option != null) {
            value = option.get(optionName);
        }

        return value;
    }

    public Value getCommonConfigValue(final String section, final String optionName) {
        Value value = null;
        final Map<String, Value> option = optionsMap.get("Section.Common." + section);
        if (option != null) {
            value = option.get(optionName);
        }
        return value;
    }

    public String[] getResources() {
        return resourceList.toArray(new String[resourceList.size()]);
    }

    /**
     * Returns drbd device of the resource. Although there can be different
     * drbd devices on different hosts. We do not allow that.
     */
    public String getDrbdDevice(final String res, final String volumeNr) {
        return resourceDeviceMap.get(res, volumeNr);
    }

    /** Returns map from res and volume to drbd device. */
    public Table<String, String, String> getResourceDeviceMap() {
        return resourceDeviceMap;
    }

    /** Gets block device object from device number. Can return null. */
    private BlockDevInfo getBlockDevInfo(final String devNr, final String hostName, final DrbdGraph drbdGraph) {
        final String disk = getBackingDisk(devNr, hostName);
        if (disk != null) {
            return drbdGraph.findBlockDevInfo(hostName, disk);
        }
        return null;
    }

    /** Return backing disk from device number. Can return null. */
    private String getBackingDisk(final String devNr, final String hostName) {
        final String device = "/dev/drbd" + devNr;
        final String resName = deviceResourceMap.get(device);
        String volumeNr = deviceVolumeMap.get(device);
        if (volumeNr == null) {
            volumeNr = "0";
        }
        if (resName != null) {
            final Map<String, String> hostDiskMap = resourceHostDiskMap.get(resName, volumeNr);
            if (hostDiskMap != null) {
                return hostDiskMap.get(hostName);
            }
        }
        return null;
    }

    /**
     * Parses events from drbd kernel module obtained via drbdsetup .. events
     * command and stores the values in the BlockDevice object.
     */
    public boolean parseDrbdEvent(final String hostName, final DrbdGraph drbdGraph, final String rawOutput) {
        if (rawOutput == null || hostName == null) {
            return false;
        }

        final String output = rawOutput.trim();
        if (output.isEmpty()) {
            return false;
        }
        if ("No response from the DRBD driver! Is the module loaded?".equals(output)) {
            if (hostDrbdLoadedMap.get(hostName)) {
                hostDrbdLoadedMap.put(hostName, false);
                return true;
            } else {
                return false;
            }
        } else {
            hostDrbdLoadedMap.put(hostName, true);
        }
        /* since drbd 8.3 there is ro: instead of st: */
        /* since drbd 8.4 there is ro: instead of st: */
        Pattern p = Pattern.compile("^(\\d+)\\s+ST\\s+(\\S+)\\s+\\{\\s+cs:(\\S+)\\s+"
                                    + "(?:st|ro):(\\S+)/(\\S+)\\s+ds:(\\S+)/(\\S+)\\s+(\\S+).*?");
        Matcher m = p.matcher(output);
        final Pattern pDev = Pattern.compile("^(\\d+),(\\S+)\\[(\\d+)]$");
        if (m.matches()) {
            /* String counter      = m.group(1); // not used */
            final String devNrString  = m.group(2);
            final String cs           = m.group(3);
            final String ro1          = m.group(4);
            final String ro2          = m.group(5);
            final String ds1          = m.group(6);
            final String ds2          = m.group(7);
            final String flags        = m.group(8);

            final Matcher mDev = pDev.matcher(devNrString);
            String devNr = devNrString;
            if (mDev.matches()) { /* since 8.4 */
                devNr = mDev.group(1);
            }
            /* get blockdevice object from device */
            final String disk = getBackingDisk(devNr, hostName);
            if (disk != null) {
                final BlockDevInfo bdi = drbdGraph.findBlockDevInfo(hostName, disk);
                if (bdi != null) {
                    if (bdi.getBlockDevice().isDifferent(cs, ro1, ds1, flags)) {
                        bdi.getBlockDevice().setDrbdBackingDisk(disk);
                        bdi.getBlockDevice().setConnectionState(cs);
                        bdi.getBlockDevice().setNodeState(ro1);
                        bdi.getBlockDevice().setDiskState(ds1);
                        bdi.getBlockDevice().setNodeStateOther(ro2);
                        bdi.getBlockDevice().setDiskStateOther(ds2);
                        bdi.getBlockDevice().setDrbdFlags(flags);
                        bdi.updateInfo();
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }
        /* 19 SP 0 16.9 */
        p = Pattern.compile("^(\\d+)\\s+SP\\s+(\\S+)\\s(\\d+\\.\\d+).*");
        m = p.matcher(output);
        if (m.matches()) {
            /* String counter      = m.group(1); // not used */
            final String devNrString = m.group(2);
            final String synced = m.group(3);

            final Matcher mDev = pDev.matcher(devNrString);
            String devNr = devNrString;
            if (mDev.matches()) { /* since 8.4 */
                devNr = mDev.group(1);
            }
            final BlockDevInfo bdi = getBlockDevInfo(devNr, hostName, drbdGraph);
            if (bdi != null && bdi.getBlockDevice().isDrbd()) {
                if (Tools.areEqual(bdi.getBlockDevice().getSyncedProgress(), synced)) {
                    return false;
                } else {
                    bdi.getBlockDevice().setSyncedProgressInPercents(synced);
                    bdi.updateInfo();
                    return true;
                }
            }
            return false;
        }
        /* 19 UH 1 split-brain */
        p = Pattern.compile("^(\\d+)\\s+UH\\s+(\\S+)\\s([a-z-]+).*");
        m = p.matcher(output);
        if (m.matches()) {
            /* String counter      = m.group(1); // not used */
            final String devNrString = m.group(2);
            final String what = m.group(3);
            final Matcher mDev = pDev.matcher(devNrString);
            String devNr = devNrString;
            if (mDev.matches()) { /* since 8.4 */
                devNr = mDev.group(1);
            }
            LOG.debug("parseDrbdEvent: event: " + devNr + " - " + what);
            if ("split-brain".equals(what)) {
                final BlockDevInfo bdi = getBlockDevInfo(devNr, hostName, drbdGraph);

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

    /** Removes the resource from resources, so that it does not reappear. */
    public void removeResource(final String res) {
        resourceList.remove(res);
    }

    /** Removes the volume from hashes, so that it does not reappear. */
    public void removeVolume(final String res, final String dev, final String volumeNr) {
        resourceDeviceMap.remove(res, volumeNr);
        deviceResourceMap.remove(dev);
        deviceVolumeMap.remove(dev);
    }

    /**
     * Whether drbd is disabled. If there are unknown sections, that we don't
     * want to overwrite.
     */
    public boolean isDrbdDisabled() {
        return unknownSections && !access.isAdvancedMode();
    }

    public String getOldConfig() {
        return oldConfig;
    }

    public Value parseValue(final String param, final String v) {
        if (v == null) {
            return null;
        }
        final Matcher m = UNIT_PATTERN.matcher(v);
        if (m.matches()) {
            final String value = m.group(1);
            final String u = m.group(2);

            final Unit unit = parseUnit(param, u);
            return new StringValue(value, unit);
        }
        return new StringValue(v);
    }

    private Unit parseUnit(final String param, final String u) {
        if (!hasUnitPrefix(param)) {
            return null;
        }
        final String unitType = getUnitLong(param);
        if (unitType == null || unitType.isEmpty()) {
            return parseSizeUnit(param, u);
        } else {
            return parseByteUnit(param, u);
        }
    }

    private Unit parseSizeUnit(final String param, final String u) {
        final String unitPart = getUnitPart(getUnitLong(param));
        if (u == null || u.isEmpty()) {
            return getUnitDefault(unitPart);
        } else if ("k".equalsIgnoreCase(u)) {
            return getUnitKi(unitPart);
        } else if ("m".equalsIgnoreCase(u)) {
            return getUnitMi(unitPart);
        } else if ("g".equalsIgnoreCase(u)) {
            return getUnitGi(unitPart);
        } else if ("t".equalsIgnoreCase(u)) {
            return getUnitTi(unitPart);
        } else {
            LOG.appError("can't parse unit: " + u + " param: " + param);
        }
        return null;
    }

    private Unit parseByteUnit(final String param, final String u) {
        final String unitPart = getUnitPart(getUnitLong(param));
        if (u == null || u.isEmpty()) {
            return getUnitBytes(unitPart);
        } else if ("k".equalsIgnoreCase(u)) {
            return getUnitKiBytes(unitPart);
        } else if ("m".equalsIgnoreCase(u)) {
            return getUnitMiBytes(unitPart);
        } else if ("g".equalsIgnoreCase(u)) {
            return getUnitGiBytes(unitPart);
        } else if ("t".equalsIgnoreCase(u)) {
            return getUnitTiBytes(unitPart);
        } else if ("s".equalsIgnoreCase(u)) {
            return getUnitSectors(unitPart);
        } else {
            LOG.appError("can't parse unit: " + u + " param: " + param);
        }
        return null;
    }

    public static class HostProxy {
        private final String proxyHostName;
        private final Value insideIp;
        private final Value insidePort;
        private final Value outsideIp;
        private final Value outsidePort;

        public HostProxy(final String proxyHostName, final Value insideIp, final Value insidePort, final Value outsideIp,
                final Value outsidePort) {
            this.proxyHostName = proxyHostName;
            this.insideIp = insideIp;
            this.insidePort = insidePort;
            this.outsideIp = outsideIp;
            this.outsidePort = outsidePort;
        }

        public String getProxyHostName() {
            return proxyHostName;
        }

        public Value getInsideIp() {
            return insideIp;
        }

        public Value getInsidePort() {
            return insidePort;
        }

        public Value getOutsideIp() {
            return outsideIp;
        }

        public Value getOutsidePort() {
            return outsidePort;
        }
    }

    /** Converts value to unit e.g. bytes. */
    public BigInteger convertToUnit(final String param, final Value value) {
        final String unitPart = getUnitPart(getUnitLong(param));
        final Unit unit = value.getUnit();
        BigInteger num = new BigInteger(value.getValueForConfig());
        if (getUnitPiBytes(unitPart).equals(unit)
            || getUnitPi(unitPart).equals(unit)) {
            num = num.multiply(KILO)
                     .multiply(KILO)
                     .multiply(KILO)
                     .multiply(KILO)
                     .multiply(KILO);
        } else if (getUnitTiBytes(unitPart).equals(unit)
                   || getUnitTi(unitPart).equals(unit)) {
            num = num.multiply(KILO)
                     .multiply(KILO)
                     .multiply(KILO)
                     .multiply(KILO);
        } else if (getUnitGiBytes(unitPart).equals(unit)
                   || getUnitGi(unitPart).equals(unit)) {
            num = num.multiply(KILO)
                     .multiply(KILO)
                     .multiply(KILO);
        } else if (getUnitMiBytes(unitPart).equals(unit)
                   || getUnitMi(unitPart).equals(unit)) {
            num = num.multiply(KILO)
                     .multiply(KILO);
        } else if (getUnitKiBytes(unitPart).equals(unit)
                   || getUnitKi(unitPart).equals(unit)) {
            num = num.multiply(KILO);
        } else {
            LOG.appWarning("unknown unit: " + unit);
        }
        return num;
    }
}
