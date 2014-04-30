/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package lcmc.gui.resources.crm;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.data.ResourceAgent;
import lcmc.data.StringValue;
import lcmc.data.VMSXML;
import lcmc.data.Value;
import lcmc.gui.Browser;
import lcmc.gui.resources.vms.DomainInfo;
import lcmc.utilities.UpdatableItem;

/**
 * This class holds info about VirtualDomain service in the cluster menu.
 */
public class VirtualDomainInfo extends ServiceInfo {
    /** Pattern that captures a name from xml file name. */
    static final Pattern LIBVIRT_CONF_PATTERN =
                                            Pattern.compile(".*?([^/]+).xml$");
    /** Parameters. */
    private static final String CONFIG_PARAM = "config";
    private static final String HYPERVISOR_PARAM = "hypervisor";
    /** Hypervisor choices. */
    private static final Value[] HYPERVISORS = {new StringValue("qemu:///system"),
                                                new StringValue("xen:///"),
                                                new StringValue("lxc:///"),
                                                new StringValue("vbox:///"),
                                                new StringValue("openvz:///system"),
                                                new StringValue("uml:///system")};
    private static final String PARAM_ALLOW_MIGRATE = "allow-migrate";
    /** VirtualDomain in the VMs menu. */
    private DomainInfo domainInfo = null;

    /** Creates the VirtualDomainInfo object. */
    public VirtualDomainInfo(final String name,
                      final ResourceAgent ra,
                      final Browser browser) {
        super(name, ra, browser);
    }

    /** Creates the VirtualDomainInfo object. */
    public VirtualDomainInfo(final String name,
                      final ResourceAgent ra,
                      final String hbId,
                      final Map<String, String> resourceNode,
                      final Browser browser) {
        super(name, ra, hbId, resourceNode, browser);
    }

    /** Returns object with vm data. */
    VMSXML getVMSXML(final Host host) {
        return getBrowser().getVMSXML(host);
    }

    /** Removes the service without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Host dcHost,
                                         final Application.RunMode runMode) {
        super.removeMyselfNoConfirm(dcHost, runMode);
    }

    /** Sets service parameters with values from resourceNode hash. */
    @Override
    protected void setParameters(final Map<String, String> resourceNode) {
        super.setParameters(resourceNode);
        connectWithVMS();
    }

    /** Connects with DomainInfo object. */
    @Override
    public DomainInfo connectWithVMS() {
        final Value config = getParamSaved(CONFIG_PARAM);
        DomainInfo newVMSVDI = null;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                final String name = vxml.getNameFromConfig(config.getValueForConfig());
                newVMSVDI = getBrowser().findVMSVirtualDomainInfo(name);
                if (newVMSVDI != null) {
                    newVMSVDI.setUsedByCRM(true);
                    break;
                }
            }
        }
        domainInfo = newVMSVDI;
        return newVMSVDI;
    }
    
    public DomainInfo getDomainInfo() {
        return domainInfo;
    }

    /** Returns the possible values for the pulldown menus, if applicable. */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        if (CONFIG_PARAM.equals(param)) {
            final Set<Value> configs = new TreeSet<Value>();
            for (final Host host : getBrowser().getClusterHosts()) {
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null) {
                    configs.addAll(vxml.getConfigs());
                }
            }
            return configs.toArray(new Value[configs.size()]);
        } else if (HYPERVISOR_PARAM.equals(param)) {
            return HYPERVISORS;
        } else {
            return super.getParamPossibleChoices(param);
        }
    }

    /**
     * Returns list of items for service popup menu with actions that can
     * be executed on the pacemaker services.
     */
    @Override
    public List<UpdatableItem> createPopup() {
        final VirtualDomainMenu virtualDomainMenu = new VirtualDomainMenu(this);
        return virtualDomainMenu.getPulldownMenu();
    }

    /** Returns a name of the service with virtual domain name. */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(30);
        s.append(getName());
        final String string;
        final String id = getService().getId();
        final Value configName = getParamSaved(CONFIG_PARAM);
        if (configName == null || configName.getValueForConfig() == null) {
            string = id;
        } else {
            final Matcher m = LIBVIRT_CONF_PATTERN.matcher(configName.getValueForConfig());
            if (m.matches()) {
                string = m.group(1);
            } else {
                string = id;
            }
        }
        if (string == null) {
            s.insert(0, "new ");
        } else {
            if (!string.isEmpty()) {
                s.append(" (");
                s.append(string);
                s.append(')');
            }
        }
        return s.toString();
    }

    /** Applies the changes to the service parameters. */
    @Override
    public void apply(final Host dcHost, final Application.RunMode runMode) {
        super.apply(dcHost, runMode);
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        return !PARAM_ALLOW_MIGRATE.equals(param) && super.isAdvanced(param);
    }
}
