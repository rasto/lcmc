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
package lcmc.gui.resources;

import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.data.Host;
import lcmc.data.HostLocation;
import lcmc.data.ResourceAgent;
import lcmc.gui.Widget;

import java.awt.geom.Point2D;
import lcmc.data.resources.Service;
import lcmc.data.Subtext;
import lcmc.data.CRMXML;
import lcmc.data.ClusterStatus;
import lcmc.data.ConfigData;
import lcmc.data.PtestData;
import lcmc.data.AccessMode;
import lcmc.utilities.MyMenu;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.Unit;
import lcmc.utilities.Tools;
import lcmc.utilities.CRM;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.MyList;
import lcmc.utilities.MyListModel;
import lcmc.utilities.WidgetListener;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.pacemaker.ServiceLogs;
import lcmc.gui.dialog.EditConfig;

import java.awt.Color;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.SpringLayout;
import javax.swing.AbstractButton;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.collections15.map.MultiKeyMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

/**
 * This class holds info data for one hearteat service and allows to enter
 * its arguments and execute operations on it.
 */
public class ServiceInfo extends EditableInfo {
    /** A map from host to the combobox with scores. */
    private final Map<HostInfo, Widget> scoreComboBoxHash =
                                          new HashMap<HostInfo, Widget>();
    /** A map from host to stored score. */
    private final Map<HostInfo, HostLocation> savedHostLocations =
                                         new HashMap<HostInfo, HostLocation>();
    /** A combobox with pingd constraint. */
    private Widget pingComboBox = null;
    /** Saved ping constraint. */
    private String savedPingOperation = null;
    /** Saved meta attrs id. */
    private String savedMetaAttrsId = null;
    /** Saved operations id. */
    private String savedOperationsId = null;
    /** A map from operation to the stored value. First key is
     * operation name like "start" and second key is parameter like
     * "timeout". */
    private final MultiKeyMap<String, String> savedOperation =
                                             new MultiKeyMap<String, String>();
    /** Whether id-ref for meta-attributes is used. */
    private ServiceInfo savedMetaAttrInfoRef = null;
    /** Combo box with same as operations option. */
    private Widget sameAsMetaAttrsWi = null;
    /** Whether id-ref for operations is used. */
    private ServiceInfo savedOperationIdRef = null;
    /** Combo box with same as operations option. */
    private Widget sameAsOperationsWi = null;
    /** Saved operations lock. */
    private final Lock mSavedOperationsLock = new ReentrantLock();
    /** Operations combo box hash lock. */
    private final ReadWriteLock mOperationsComboBoxHashLock =
                                                  new ReentrantReadWriteLock();
    /** Operations combo box hash read lock. */
    private final Lock mOperationsComboBoxHashReadLock =
                                        mOperationsComboBoxHashLock.readLock();
    /** Operations combo box hash write lock. */
    private final Lock mOperationsComboBoxHashWriteLock =
                                       mOperationsComboBoxHashLock.writeLock();
    /** A map from operation to its combo box. */
    private final MultiKeyMap<String, Widget> operationsComboBoxHash =
                                        new MultiKeyMap<String, Widget>();
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Group info object of the group this service is in or null, if it is
     * not in any group. */
    private GroupInfo groupInfo = null;
    /** Master/Slave info object, if is null, it is not master/slave
     * resource. */
    private volatile CloneInfo cloneInfo = null;
    /** ResourceAgent object of the service, with name, ocf informations
     * etc. */
    private final ResourceAgent resourceAgent;
    /** Radio buttons for clone/master/slave primitive resources. */
    private Widget typeRadioGroup;
    /** Default values item in the "same as" scrolling list in meta
        attributes.*/
    private static final String META_ATTRS_DEFAULT_VALUES_TEXT =
                                                          "default values";
    /** Default values internal name. */
    private static final String META_ATTRS_DEFAULT_VALUES = "default";
    /** Default values item in the "same as" scrolling list in operations. */
    private static final String OPERATIONS_DEFAULT_VALUES_TEXT =
                                                          "advisory minimum";
    /** Default values internal name. */
    private static final String OPERATIONS_DEFAULT_VALUES = "default";

    /** Check the cached fields. */
    protected static final String CACHED_FIELD = "cached";
    /** Master / Slave type string. */
    static final String MASTER_SLAVE_TYPE_STRING = "Master/Slave";
    /** Manage by CRM icon. */
    static final ImageIcon MANAGE_BY_CRM_ICON = Tools.createImageIcon(
                  Tools.getDefault("ServiceInfo.ManageByCRMIcon"));
    /** Don't Manage by CRM icon. */
    static final ImageIcon UNMANAGE_BY_CRM_ICON = Tools.createImageIcon(
                 Tools.getDefault("ServiceInfo.UnmanageByCRMIcon"));
    /** Unmanage service icon. */
    private static final ImageIcon UNMANAGE_ICON = Tools.createImageIcon(
                      Tools.getDefault("HeartbeatGraph.ServiceUnmanagedIcon"));
    /** Icon that indicates a running service. */
    public static final ImageIcon SERVICE_RUNNING_ICON_SMALL =
                                     Tools.createImageIcon(Tools.getDefault(
                                    "ServiceInfo.ServiceRunningIconSmall"));
    /** Icon that indicates a running that failed. */
    private static final ImageIcon SERVICE_RUNNING_FAILED_ICON_SMALL =
                            Tools.createImageIcon(Tools.getDefault(
                              "ServiceInfo.ServiceRunningFailedIconSmall"));
    /** Icon that indicates a started service (but not running). */
    private static final ImageIcon SERVICE_STARTED_ICON_SMALL =
                                  Tools.createImageIcon(Tools.getDefault(
                                    "ServiceInfo.ServiceStartedIconSmall"));
    /** Icon that indicates a stopping service (but not stopped). */
    private static final ImageIcon SERVICE_STOPPING_ICON_SMALL =
                                 Tools.createImageIcon(Tools.getDefault(
                                   "ServiceInfo.ServiceStoppingIconSmall"));
    /** Icon that indicates a not running service. */
    public static final ImageIcon SERVICE_STOPPED_ICON_SMALL =
                               Tools.createImageIcon(Tools.getDefault(
                                    "ServiceInfo.ServiceStoppedIconSmall"));
    /** Icon that indicates a not running service that failed. */
    private static final ImageIcon SERVICE_STOPPED_FAILED_ICON_SMALL =
                           Tools.createImageIcon(Tools.getDefault(
                              "ServiceInfo.ServiceStoppedFailedIconSmall"));
    /** Running service icon. */
    static final ImageIcon SERVICE_RUNNING_ICON =
                Tools.createImageIcon(
                        Tools.getDefault("HeartbeatGraph.ServiceRunningIcon"));
    /** Not running service icon. */
    private static final ImageIcon SERVICE_STOPPED_ICON =
            Tools.createImageIcon(
                        Tools.getDefault("HeartbeatGraph.ServiceStoppedIcon"));
    /** Start service icon. */
    static final ImageIcon START_ICON = SERVICE_RUNNING_ICON;
    /** Stop service icon. */
    static final ImageIcon STOP_ICON  = SERVICE_STOPPED_ICON;
    /** Migrate icon. */
    protected static final ImageIcon MIGRATE_ICON = Tools.createImageIcon(
                            Tools.getDefault("HeartbeatGraph.MigrateIcon"));
    /** Unmigrate icon. */
    static final ImageIcon UNMIGRATE_ICON = Tools.createImageIcon(
                            Tools.getDefault("HeartbeatGraph.UnmigrateIcon"));
    /** Group up icon. */
    static final ImageIcon GROUP_UP_ICON = Tools.createImageIcon(
                                Tools.getDefault("HeartbeatGraph.GroupUp"));
    /** Group down icon. */
    static final ImageIcon GROUP_DOWN_ICON = Tools.createImageIcon(
                                Tools.getDefault("HeartbeatGraph.GroupDown"));
    /** Orphaned subtext. */
    private static final Subtext ORPHANED_SUBTEXT = new Subtext("(LRM)",
                                                                null,
                                                                Color.BLACK);
    /** Orphaned with fail-count subtext. */
    private static final Subtext ORPHANED_FAILED_SUBTEXT =
                                    new Subtext("(ORPHANED)", null, Color.RED);
    /** Unmanaged subtext. */
    private static final Subtext UNMANAGED_SUBTEXT =
                                   new Subtext("(unmanaged)", null, Color.RED);
    /** Migrated subtext. */
    private static final Subtext MIGRATED_SUBTEXT =
                                    new Subtext("(migrated)", null, Color.RED);
    /** Clone type string. */
    protected static final String CLONE_TYPE_STRING = "Clone";
    /** Primitive type string. */
    private static final String PRIMITIVE_TYPE_STRING = "Primitive";
    /** Gui ID parameter. */
    public static final String GUI_ID = "__drbdmcid";
    /** PCMK ID parameter. */
    public static final String PCMK_ID = "__pckmkid";
    /** String that appears as a tooltip in menu items if item is being
     * removed. */
    static final String IS_BEING_REMOVED_STRING = "it is being removed";

    /** String that appears as a tooltip in menu items if item is orphan. */
    static final String IS_ORPHANED_STRING = "cannot do that to an ophan";
    /** String that appears as a tooltip in menu items if item is new. */
    static final String IS_NEW_STRING = "it is not applied yet";
    /** Ping attributes. */
    private static final Map<String, String> PING_ATTRIBUTES =
                                                new HashMap<String, String>();
    static {
        PING_ATTRIBUTES.put("eq0", "no ping: stop"); /* eq 0 */
        PING_ATTRIBUTES.put("defined", "most connections");
    }

    /**
     * Prepares a new <code>ServiceInfo</code> object and creates
     * new service object.
     */
    ServiceInfo(final String name,
                final ResourceAgent resourceAgent,
                final Browser browser) {
        super(name, browser);
        this.resourceAgent = resourceAgent;
        if (resourceAgent != null && resourceAgent.isStonith()) {
            setResource(new Service(name.replaceAll("/", "_")));
            getService().setStonith(true);
        } else {
            setResource(new Service(name));
        }
        getService().setNew(true);
    }

    /**
     * Prepares a new <code>ServiceInfo</code> object and creates
     * new service object. It also initializes parameters along with
     * heartbeat id with values from xml stored in resourceNode.
     */
    ServiceInfo(final String name,
                final ResourceAgent ra,
                final String heartbeatId,
                final Map<String, String> resourceNode,
                final Browser browser) {
        this(name, ra, browser);
        getService().setHeartbeatId(heartbeatId);
        /* TODO: cannot call setParameters here, only after it is
         * constructed. */
        setParameters(resourceNode);
    }

    /**
     * Returns id of the service, which is heartbeatId.
     * TODO: this id is used for stored position info, should be named
     * differently.
     */
    @Override
    public String getId() {
        return getService().getHeartbeatId();
    }

    /** Returns browser object of this info. */
    @Override
    protected ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Sets info panel of the service. */
    public void setInfoPanel(final JPanel infoPanel) {
        this.infoPanel = infoPanel;
    }

    /** Returns true if the node is active. */
    boolean isOfflineNode(final String node) {
        return "no".equals(getBrowser().getClusterStatus().isOnlineNode(node));
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params) {
        return checkResourceFieldsCorrect(param, params, false, false, false);
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params,
                                       final boolean fromServicesInfo,
                                       final boolean fromCloneInfo,
                                       final boolean fromGroupInfo) {
        if (getComboBoxValue(GUI_ID) == null) {
            return true;
        }
        final CloneInfo ci = getCloneInfo();
        if (!fromCloneInfo && ci != null) {
            return ci.checkResourceFieldsCorrect(param,
                                                 ci.getParametersFromXML(),
                                                 fromServicesInfo);
        }
        final GroupInfo gi = getGroupInfo();
        if (!fromGroupInfo && gi != null) {
            if (!gi.checkResourceFieldsCorrect(param,
                                               gi.getParametersFromXML(),
                                               fromServicesInfo,
                                               fromCloneInfo)) {
                return false;
            }
        }
        if (!fromGroupInfo && gi != null) {
            if (!fromServicesInfo) {
                gi.setApplyButtons(null, gi.getParametersFromXML());
            }
        }
        if (getService().isOrphaned()) {
            return false;
        }
        /* Allow it only for resources that are in LRM. */
        final String id = getComboBoxValue(GUI_ID);
        final ServiceInfo si =
                getBrowser().getServiceInfoFromId(getService().getName(), id);
        if (si != null && si != this && !si.getService().isOrphaned()) {
            return false;
        }

        if (!super.checkResourceFieldsCorrect(param, params)) {
            return false;
        }
        if (ci == null) {
            boolean on = false;
            for (Host host : getBrowser().getClusterHosts()) {
                final HostInfo hi = host.getBrowser().getHostInfo();
                /* at least one "eq" */
                final Widget wi = scoreComboBoxHash.get(hi);
                if (wi != null) {
                    final JLabel label = wi.getLabel();
                    if (label != null) {
                        final String op = getOpFromLabel(hi.getName(),
                                                         label.getText());
                        if (wi.getValue() == null || "eq".equals(op)) {
                            on = true;
                            break;
                        }
                    }
                }
            }
            if (!on) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    @Override
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params) {
        return checkResourceFieldsChanged(param, params, false, false, false);
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params,
                                              final boolean fromServicesInfo,
                                              final boolean fromCloneInfo,
                                              final boolean fromGroupInfo) {
        final String id = getComboBoxValue(GUI_ID);
        final CloneInfo ci = getCloneInfo();
        if (!fromCloneInfo && ci != null) {
            return ci.checkResourceFieldsChanged(param,
                                                 ci.getParametersFromXML(),
                                                 fromServicesInfo);
        }
        final GroupInfo gi = getGroupInfo();
        if (!fromGroupInfo && gi != null) {
            if (!fromServicesInfo) {
                gi.setApplyButtons(null, gi.getParametersFromXML());
            }
        }
        if (id == null) {
            return false;
        }
        boolean changed = false;
        if (super.checkResourceFieldsChanged(param, params)) {
            changed = true;
        }
        boolean allMetaAttrsAreDefaultValues = true;
        if (params != null) {
            for (String otherParam : params) {
                if (isMetaAttr(otherParam)) {
                    final Widget wi = getWidget(otherParam, null);
                    if (wi == null) {
                        continue;
                    }
                    final Object newValue = wi.getValue();
                    final Object defaultValue = getParamDefault(otherParam);
                    if (!Tools.areEqual(newValue, defaultValue)) {
                        allMetaAttrsAreDefaultValues = false;
                    }
                }
            }
        }
        final String heartbeatId = getService().getHeartbeatId();
        if (ConfigData.PM_GROUP_NAME.equals(getName())) {
            if (heartbeatId == null) {
                changed = true;
            } else if (heartbeatId.equals(Service.GRP_ID_PREFIX + id)
                || heartbeatId.equals(id)) {
                if (checkHostLocationsFieldsChanged()
                    || checkOperationFieldsChanged()) {
                    changed = true;
                }
            } else {
                changed = true;
            }
        } else if (ConfigData.PM_CLONE_SET_NAME.equals(getName())
                   || ConfigData.PM_MASTER_SLAVE_SET_NAME.equals(getName())) {
            String prefix;
            if (getService().isMaster()) {
                prefix = Service.MS_ID_PREFIX;
            } else {
                prefix = Service.CL_ID_PREFIX;
            }
            if (heartbeatId.equals(prefix + id)
                || heartbeatId.equals(id)) {
                if (checkHostLocationsFieldsChanged()) {
                    changed = true;
                }
            } else {
                changed = true;
            }
        } else {
            if (heartbeatId == null) {
            } else if (heartbeatId.equals(Service.RES_ID_PREFIX
                                          + getService().getName()
                                          + "_" + id)
                       || heartbeatId.equals(Service.STONITH_ID_PREFIX
                                          + getService().getName()
                                          + "_" + id)
                       || heartbeatId.equals(id)) {
                if (checkHostLocationsFieldsChanged()
                    || checkOperationFieldsChanged()) {
                    changed = true;
                }
            } else {
                changed = true;
            }
        }
        final String cl = getService().getResourceClass();
        if (cl != null && (cl.equals(ResourceAgent.HEARTBEAT_CLASS)
                           || cl.equals(ResourceAgent.SERVICE_CLASS)
                           || cl.equals(ResourceAgent.LSB_CLASS))) {
            /* in old style resources don't show all the textfields */
            boolean visible = false;
            Widget wi = null;
            for (int i = params.length - 1; i >= 0; i--) {
                final Widget prevWi = getWidget(params[i], null);
                if (prevWi == null) {
                    continue;
                }
                if (!visible && !prevWi.getStringValue().equals("")) {
                    visible = true;
                }
                if (wi != null && wi.isVisible() != visible) {
                    final boolean v = visible;
                    final Widget c = wi;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            c.setVisible(v);
                            getLabel(c).setVisible(v);
                        }
                    });
                }
                wi = prevWi;
            }
        }

        /* id-refs */
        if (sameAsMetaAttrsWi != null) {
            final Info info = (Info) sameAsMetaAttrsWi.getValue();
            final boolean defaultValues =
                    info != null
                    && META_ATTRS_DEFAULT_VALUES_TEXT.equals(info.toString());
            final boolean nothingSelected =
                      info == null
                      || Widget.NOTHING_SELECTED.equals(info.toString());
            if (!nothingSelected
                && !defaultValues
                && info != savedMetaAttrInfoRef) {
                changed = true;
            } else {
                if ((nothingSelected || defaultValues)
                    && savedMetaAttrInfoRef != null) {
                    changed = true;
                }
                if (savedMetaAttrInfoRef == null
                    && defaultValues != allMetaAttrsAreDefaultValues) {
                    if (allMetaAttrsAreDefaultValues) {
                        sameAsMetaAttrsWi.setValueNoListeners(
                                               META_ATTRS_DEFAULT_VALUES_TEXT);
                    } else {
                        sameAsMetaAttrsWi.setValueNoListeners(
                                                     Widget.NOTHING_SELECTED);
                    }
                }
            }
            sameAsMetaAttrsWi.processAccessMode();
        }
        if (!fromServicesInfo) {
            final ServicesInfo sis = getBrowser().getServicesInfo();
            sis.setApplyButtons(null, sis.getParametersFromXML());
        }
        return changed;
    }

    /** Returns operation default for parameter. */
    private String getOpDefaultsDefault(final String param) {
        assert param != null;
        /* if op_defaults is set... It cannot be set in the GUI  */
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (cs != null) {
            return cs.getOpDefaultsValuePairs().get(param);
        }
        return null;
    }

    /** Sets service parameters with values from resourceNode hash. */
    void setParameters(final Map<String, String> resourceNode) {
        if (resourceNode == null) {
            return;
        }
        final boolean infoPanelOk = isInfoPanelOk();
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (crmXML == null) {
            Tools.appError("crmXML is null");
            return;
        }
        /* Attributes */
        final String[] params = crmXML.getParameters(resourceAgent,
                                                     getService().isMaster());
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (params != null) {
            boolean allMetaAttrsAreDefaultValues = true;
            boolean allSavedMetaAttrsAreDefaultValues = true;
            final String newMetaAttrsId = cs.getMetaAttrsId(
                                               getService().getHeartbeatId());
            if ((savedMetaAttrsId == null && newMetaAttrsId != null)
                || (savedMetaAttrsId != null
                    && !savedMetaAttrsId.equals(newMetaAttrsId))) {
                /* newly generated operations id, reload all other combo
                   boxes. */
                getBrowser().reloadAllComboBoxes(this);
            }
            savedMetaAttrsId = newMetaAttrsId;
            String refCRMId = cs.getMetaAttrsRef(getService().getHeartbeatId());
            final ServiceInfo metaAttrInfoRef =
                                getBrowser().getServiceInfoFromCRMId(refCRMId);
            if (refCRMId == null) {
                refCRMId = getService().getHeartbeatId();
            }
            resourceNode.put(PCMK_ID, getService().getHeartbeatId());
            resourceNode.put(GUI_ID, getService().getId());
            for (String param : params) {
                String value;
                if (isMetaAttr(param) && refCRMId != null) {
                    value = cs.getParameter(refCRMId, param, false);
                } else {
                    value = resourceNode.get(param);
                }
                final String defaultValue = getParamDefault(param);
                if (value == null) {
                    value = defaultValue;
                }
                if (value == null) {
                    value = "";
                }
                final String oldValue = getResource().getValue(param);
                if (isMetaAttr(param)) {
                    if (!Tools.areEqual(defaultValue, value)) {
                        allMetaAttrsAreDefaultValues = false;
                    }
                    if (!Tools.areEqual(defaultValue, oldValue)) {
                        allSavedMetaAttrsAreDefaultValues = false;
                    }
                }
                if (infoPanelOk) {
                    final Widget wi = getWidget(param, null);
                    final boolean haveChanged =
                       !Tools.areEqual(value, oldValue)
                       || !Tools.areEqual(defaultValue,
                                          getResource().getDefaultValue(param));
                    if (haveChanged
                        || (metaAttrInfoRef != null && isMetaAttr(param))) {
                        getResource().setValue(param, value);
                        /* set default value, because it can change in
                         * rsc_defaults. */
                        getResource().setDefaultValue(param, defaultValue);
                        if (wi != null && metaAttrInfoRef == null) {
                            wi.setValue(value);
                        }
                    }
                }
            }
            if (!Tools.areEqual(metaAttrInfoRef, savedMetaAttrInfoRef)) {
                savedMetaAttrInfoRef = metaAttrInfoRef;
                if (sameAsMetaAttrsWi != null) {
                    if (metaAttrInfoRef == null) {
                        if (allMetaAttrsAreDefaultValues) {
                            if (!allSavedMetaAttrsAreDefaultValues) {
                                sameAsMetaAttrsWi.setValue(
                                            META_ATTRS_DEFAULT_VALUES_TEXT);
                            }
                        } else {
                            if (metaAttrInfoRef != null) {
                                sameAsMetaAttrsWi.setValue(
                                             Widget.NOTHING_SELECTED);
                            }
                        }
                    } else {
                        sameAsMetaAttrsWi.setValue(metaAttrInfoRef);
                    }
                }
            }
        }

        /* set scores */
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation hostLocation = cs.getScore(
                                                getService().getHeartbeatId(),
                                                hi.getName(),
                                                false);
            final HostLocation savedLocation = savedHostLocations.get(hi);
            if (!Tools.areEqual(hostLocation, savedLocation)) {
                if (hostLocation == null) {
                    savedHostLocations.remove(hi);
                } else {
                    savedHostLocations.put(hi, hostLocation);
                }
                if (infoPanelOk) {
                    final Widget wi = scoreComboBoxHash.get(hi);
                    if (wi != null) {
                        String score = null;
                        String op = null;
                        if (hostLocation != null) {
                            score = hostLocation.getScore();
                            op = hostLocation.getOperation();
                        }
                        wi.setValue(score);
                        final JLabel label = wi.getLabel();
                        final String text =
                                        getHostLocationLabel(hi.getName(), op);
                        label.setText(text);
                    }
                }
            }
        }

        /* set ping constraint */
        final HostLocation hostLocation = cs.getPingScore(
                                                getService().getHeartbeatId(),
                                                false);
        String pingOperation = null;
        if (hostLocation != null) {
            final String op = hostLocation.getOperation();
            final String value = hostLocation.getValue();
            if ("eq".equals(op) && "0".equals(value)) {
                pingOperation = "eq0";
            } else {
                pingOperation = hostLocation.getOperation();
            }
        }
        if (!Tools.areEqual(pingOperation, savedPingOperation)) {
            savedPingOperation = pingOperation;
        }
        if (infoPanelOk) {
            final Widget wi = pingComboBox;
            if (wi != null) {
                if (pingOperation == null) {
                    wi.setValue(Widget.NOTHING_SELECTED);
                } else {
                    wi.setValue(PING_ATTRIBUTES.get(pingOperation));
                }
            }
        }

        boolean allAreDefaultValues = true;
        boolean allSavedAreDefaultValues = true;
        /* Operations */
        final String newOperationsId = cs.getOperationsId(
                                                getService().getHeartbeatId());
        if ((savedOperationsId == null && newOperationsId != null)
            || (savedOperationsId != null
                && !savedOperationsId.equals(newOperationsId))) {
            /* newly generated operations id, reload all other combo
               boxes. */
            getBrowser().reloadAllComboBoxes(this);
        }

        savedOperationsId = newOperationsId;
        String refCRMId = cs.getOperationsRef(getService().getHeartbeatId());
        final ServiceInfo operationIdRef =
                                getBrowser().getServiceInfoFromCRMId(refCRMId);
        if (refCRMId == null) {
            refCRMId = getService().getHeartbeatId();
        }
        mSavedOperationsLock.lock();
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param
                          : getBrowser().getCRMOperationParams(op)) {
                String defaultValue =
                              resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null) {
                    continue;
                }
                if (ClusterBrowser.HB_OP_IGNORE_DEFAULT.contains(op)) {
                    defaultValue = "";
                }
                String value = cs.getOperation(refCRMId, op, param);
                if (value == null || "".equals(value)) {
                    value = getOpDefaultsDefault(param);
                }
                if (value == null) {
                    value = "";
                }
                if (!defaultValue.equals(value)) {
                    allAreDefaultValues = false;
                }
                if (!defaultValue.equals(savedOperation.get(op, param))) {
                    allSavedAreDefaultValues = false;
                }
            }
        }
        boolean sameAs = false;
        if (!Tools.areEqual(operationIdRef, savedOperationIdRef)) {
            savedOperationIdRef = operationIdRef;
            if (sameAsOperationsWi != null) {
                if (operationIdRef == null) {
                    if (allAreDefaultValues) { // TODO: don't have it yet.
                        if (!allSavedAreDefaultValues) {
                            sameAsOperationsWi.setValue(
                                       OPERATIONS_DEFAULT_VALUES_TEXT);
                        }
                    } else {
                        if (savedOperationIdRef != null) {
                            sameAsOperationsWi.setValue(
                                       Widget.NOTHING_SELECTED);
                        }
                    }
                } else {
                    sameAs = false;
                    sameAsOperationsWi.setValue(operationIdRef);
                }
            }
        }
        if (!sameAs) {
            for (final String op : getResourceAgent().getOperationNames()) {
                for (final String param
                              : getBrowser().getCRMOperationParams(op)) {
                    String defaultValue =
                                  resourceAgent.getOperationDefault(op, param);
                    if (defaultValue == null) {
                        continue;
                    }
                    if (ClusterBrowser.HB_OP_IGNORE_DEFAULT.contains(op)) {
                        defaultValue = "";
                    }
                    String value = cs.getOperation(refCRMId, op, param);
                    if (value == null || "".equals(value)) {
                        value = getOpDefaultsDefault(param);
                    }
                    if (value == null) {
                        value = "";
                    }
                    if (!value.equals(savedOperation.get(op, param))) {
                        savedOperation.put(op, param, value);
                        if (infoPanelOk) {
                            mOperationsComboBoxHashReadLock.lock();
                            final Widget wi = operationsComboBoxHash.get(op,
                                                                         param);
                            mOperationsComboBoxHashReadLock.unlock();
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    wi.setEnabled(operationIdRef == null);
                                }
                            });
                            if (value != null) {
                                wi.setValue(value);
                            }
                        }
                    }
                }
            }
        }
        mSavedOperationsLock.unlock();
        getService().setAvailable();
        if (cs.isOrphaned(getHeartbeatId(false))) {
            getService().setOrphaned(true);
            getService().setNew(false);
            final CloneInfo ci = getCloneInfo();
            if (ci != null) {
                ci.getService().setNew(false);
            }
        } else {
            getService().setOrphaned(false);
        }
    }

    /** Returns name of this resource, that is used in logs. */
    String getNameForLog() {
        return getName();
    }


    /**
     * Returns a name of the service with id in the parentheses.
     * It adds prefix 'new' if id is null.
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(30);
        final String provider = resourceAgent.getProvider();
        if (!ResourceAgent.HEARTBEAT_PROVIDER.equals(provider)
            && !"".equals(provider)) {
            s.append(provider);
            s.append(':');
        }
        s.append(getName());
        final String string = getService().getId();

        /* 'string' contains the last string if there are more dependent
         * resources, although there is usually only one. */
        if (string == null) {
            s.insert(0, "new ");
        } else {
            if (!"".equals(string)) {
                s.append(" (");
                s.append(string);
                s.append(')');
            }
        }
        return s.toString();
    }

    /** Returns node name of the host where this service is running. */
    List<String> getMasterOnNodes(final boolean testOnly) {
        return getBrowser().getClusterStatus().getMasterOnNodes(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
    }

    /** Returns node name of the host where this service is running. */
    List<String> getRunningOnNodes(final boolean testOnly) {
        return getBrowser().getClusterStatus().getRunningOnNodes(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
    }

    /** Returns whether service is started. */
    boolean isStarted(final boolean testOnly) {
        final Host dcHost = getBrowser().getDCHost();
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        String targetRoleString = "target-role";
        if (Tools.versionBeforePacemaker(dcHost)) {
            targetRoleString = "target_role";
        }
        String crmId = getHeartbeatId(testOnly);
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final String refCRMId = cs.getMetaAttrsRef(crmId);
        if (refCRMId != null) {
            crmId = refCRMId;
        }
        String targetRole = cs.getParameter(crmId, targetRoleString, testOnly);
        if (targetRole == null) {
            targetRole = getParamDefault(targetRoleString);
        }
        if (!CRMXML.TARGET_ROLE_STOPPED.equals(targetRole)) {
            return true;
        }
        return false;
    }

    /** Returns whether the service was set to be in slave role. */
    public boolean isEnslaved(final boolean testOnly) {
        final Host dcHost = getBrowser().getDCHost();
        String targetRoleString = "target-role";
        if (Tools.versionBeforePacemaker(dcHost)) {
            targetRoleString = "target_role";
        }
        String crmId = getHeartbeatId(testOnly);
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final String refCRMId = cs.getMetaAttrsRef(crmId);
        if (refCRMId != null) {
            crmId = refCRMId;
        }
        String targetRole = cs.getParameter(crmId, targetRoleString, testOnly);
        if (targetRole == null) {
            targetRole = getParamDefault(targetRoleString);
        }
        if (CRMXML.TARGET_ROLE_SLAVE.equals(targetRole)) {
            return true;
        }
        return false;
    }

    /** Returns whether service is stopped. */
    public boolean isStopped(final boolean testOnly) {
        final Host dcHost = getBrowser().getDCHost();
        String targetRoleString = "target-role";
        if (Tools.versionBeforePacemaker(dcHost)) {
            targetRoleString = "target_role";
        }
        String crmId = getHeartbeatId(testOnly);
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final String refCRMId = cs.getMetaAttrsRef(crmId);
        if (refCRMId != null) {
            crmId = refCRMId;
        }
        String targetRole = cs.getParameter(crmId, targetRoleString, testOnly);
        if (targetRole == null) {
            targetRole = getParamDefault(targetRoleString);
        }
        if (CRMXML.TARGET_ROLE_STOPPED.equals(targetRole)) {
            return true;
        }
        return false;
    }

    /** Returns whether the group is stopped. */
    boolean isGroupStopped(final boolean testOnly) {
        return false;
    }

    /**
     * Returns whether service is managed.
     * TODO: "default" value
     */
    public boolean isManaged(final boolean testOnly) {
        return getBrowser().getClusterStatus().isManaged(
                                                    getHeartbeatId(testOnly),
                                                    testOnly);
    }

    /** Returns whether the service where was migrated or null. */
    public List<Host> getMigratedTo(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (Host host : getBrowser().getClusterHosts()) {
            final String locationId = cs.getLocationId(getHeartbeatId(testOnly),
                                                       host.getName(),
                                                       testOnly);
            if (locationId == null
                || (!locationId.startsWith("cli-prefer-")
                    && !locationId.startsWith("cli-standby-"))) {
                continue;
            }
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation hostLocation = cs.getScore(
                                                      getHeartbeatId(testOnly),
                                                      hi.getName(),
                                                      testOnly);
            String score = null;
            String op = null;
            if (hostLocation != null) {
                score = hostLocation.getScore();
                op = hostLocation.getOperation();
            }
            if ((CRMXML.INFINITY_STRING.equals(score)
                 || CRMXML.PLUS_INFINITY_STRING.equals(score))
                && "eq".equals(op)) {
                final List<Host> hosts = new ArrayList<Host>();
                hosts.add(host);
                return hosts;
            }
        }
        return null;
    }

    /** Returns whether the service where was migrated or null. */
    public List<Host> getMigratedFrom(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (Host host : getBrowser().getClusterHosts()) {
            final String locationId = cs.getLocationId(getHeartbeatId(testOnly),
                                                       host.getName(),
                                                       testOnly);
            if (locationId == null
                || (!locationId.startsWith("cli-prefer-")
                    && !locationId.startsWith("cli-standby-"))) {
                continue;
            }
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation hostLocation = cs.getScore(
                                                      getHeartbeatId(testOnly),
                                                      hi.getName(),
                                                      testOnly);
            String score = null;
            String op = null;
            if (hostLocation != null) {
                score = hostLocation.getScore();
                op = hostLocation.getOperation();
            }
            if (CRMXML.MINUS_INFINITY_STRING.equals(score)
                && "eq".equals(op)) {
                final List<Host> hosts = new ArrayList<Host>();
                hosts.add(host);
                return hosts;
            }
        }
        return null;
    }

    /** Returns whether the service is running. */
    public boolean isRunning(final boolean testOnly) {
        final List<String> runningOnNodes = getRunningOnNodes(testOnly);
        return runningOnNodes != null && !runningOnNodes.isEmpty();
    }

    /** Returns fail count string that appears in the graph. */
    private String getFailCountString(final String hostName,
                                      final boolean testOnly) {
        String fcString = "";
        final String failCount = getFailCount(hostName, testOnly);
        if (failCount != null) {
            if (CRMXML.INFINITY_STRING.equals(failCount)) {
                fcString = " failed";
            } else {
                fcString = " failed: " + failCount;
            }
        }
        return fcString;
    }


    /** Returns fail count. */
    protected String getFailCount(final String hostName,
                                  final boolean testOnly) {

        final ClusterStatus cs = getBrowser().getClusterStatus();
        return cs.getFailCount(hostName, getHeartbeatId(testOnly), testOnly);
    }

    /** Returns ping count. */
    protected String getPingCount(final String hostName,
                                  final boolean testOnly) {

        final ClusterStatus cs = getBrowser().getClusterStatus();
        return cs.getPingCount(hostName, testOnly);
    }

    /** Returns fail ping string that appears in the graph. */
    protected String getPingCountString(final String hostName,
                                        final boolean testOnly) {
        if (!resourceAgent.isPingService()) {
            return "";
        }
        final String pingCount = getPingCount(hostName, testOnly);
        if (pingCount == null || "0".equals(pingCount)) {
            return " / no ping";
        } else {
            return " / ping: " + pingCount;
        }
    }

    /** Returns whether the resource is orphaned on the specified host. */
    protected final boolean isInLRMOnHost(final String hostName,
                                          final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        return cs.isInLRMOnHost(hostName, getHeartbeatId(testOnly), testOnly);
    }

    /** Returns whether the resource failed on the specified host. */
    protected final boolean failedOnHost(final String hostName,
                                         final boolean testOnly) {
        final String failCount = getFailCount(hostName,
                                              testOnly);
        return failCount != null
               && CRMXML.INFINITY_STRING.equals(failCount);
    }

    /** Returns whether the resource has failed to start. */
    public boolean isFailed(final boolean testOnly) {
        if (isRunning(testOnly)) {
            return false;
        }
        for (final Host host : getBrowser().getClusterHosts()) {
            if (host.isClStatus() && failedOnHost(host.getName(),
                                                  testOnly)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether the resource has failed on one of the nodes. */
    boolean isOneFailed(final boolean testOnly) {
        for (final Host host : getBrowser().getClusterHosts()) {
            if (failedOnHost(host.getName(), testOnly)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether the resource has fail-count on one of the nodes. */
    boolean isOneFailedCount(final boolean testOnly) {
        for (final Host host : getBrowser().getClusterHosts()) {
            if (getFailCount(host.getName(), testOnly) != null) {
                return true;
            }
        }
        return false;
    }

    /** Sets whether the service is managed. */
    void setManaged(final boolean isManaged,
                    final Host dcHost,
                    final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.setManaged(dcHost, getHeartbeatId(testOnly), isManaged, testOnly);
    }

    /** Returns color for the host vertex. */
    public List<Color> getHostColors(final boolean testOnly) {
        return getBrowser().getCluster().getHostColors(
                                                  getRunningOnNodes(testOnly));
    }

    /**
     * Returns service icon in the menu. It can be started or stopped.
     * TODO: broken icon, not managed icon.
     */
    @Override
    public ImageIcon getMenuIcon(final boolean testOnly) {
        if (isFailed(testOnly)) {
            if (isRunning(testOnly)) {
                return SERVICE_RUNNING_FAILED_ICON_SMALL;
            } else {
                return SERVICE_STOPPED_FAILED_ICON_SMALL;
            }
        } else if (isStopped(testOnly) || getBrowser().allHostsDown()) {
            if (isRunning(testOnly)) {
                return SERVICE_STOPPING_ICON_SMALL;
            } else {
                return SERVICE_STOPPED_ICON_SMALL;
            }
        } else {
            if (isRunning(testOnly)) {
                return SERVICE_RUNNING_ICON_SMALL;
            } else {
                return SERVICE_STARTED_ICON_SMALL;
            }
        }
    }

    /** Gets saved host scores. */
    Map<HostInfo, HostLocation> getSavedHostLocations() {
        return savedHostLocations;
    }

    /** Returns list of all host names in this cluster. */
    List<String> getHostNames() {
        final List<String> hostNames = new ArrayList<String>();
        final Enumeration e = getBrowser().getClusterHostsNode().children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                              (DefaultMutableTreeNode) e.nextElement();
            final String hostName = ((HostInfo) n.getUserObject()).getName();
            hostNames.add(hostName);
        }
        return hostNames;
    }

    /**
     * TODO: wrong doku
     * Converts enumeration to the info array, get objects from
     * hash if they exist.
     */
    protected Info[] enumToInfoArray(final Info defaultValue,
                                     final String serviceName,
                                     final Enumeration e) {
        final List<Info> list = new ArrayList<Info>();
        if (defaultValue != null) {
            list.add(defaultValue);
        }

        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                             (DefaultMutableTreeNode) e.nextElement();
            final Info i = (Info) n.getUserObject();
            final String name = i.getName();
            final ServiceInfo si = getBrowser().getServiceInfoFromId(
                                                                 serviceName,
                                                                 i.getName());

            if (si == null && !name.equals(defaultValue)) {
                list.add(i);
            }
        }
        return list.toArray(new Info[list.size()]);
    }

    /**
     * Stores scores for host.
     */
    private void storeHostLocations() {
        savedHostLocations.clear();
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Widget wi = scoreComboBoxHash.get(hi);
            final String score = wi.getStringValue();
            final String op = getOpFromLabel(hi.getName(),
                                             wi.getLabel().getText());
            if (score == null || "".equals(score)) {
                savedHostLocations.remove(hi);
            } else {
                savedHostLocations.put(hi,
                                       new HostLocation(score, op, null, null));
            }
        }
        /* ping */
        final Object o = pingComboBox.getValue();
        String value = null;
        if (o != null) {
            value = ((StringInfo) o).getStringValue();
        }
        savedPingOperation = value;
    }

    /**
     * Returns thrue if an operation field changed.
     */
    private boolean checkOperationFieldsChanged() {
        boolean changed = false;
        boolean allAreDefaultValues = true;
        mSavedOperationsLock.lock();
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCRMOperationParams(op)) {
                String defaultValue =
                                resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null) {
                    continue;
                }
                if (ClusterBrowser.HB_OP_IGNORE_DEFAULT.contains(op)) {
                    defaultValue = "";
                }
                mOperationsComboBoxHashReadLock.lock();
                final Widget wi = operationsComboBoxHash.get(op, param);
                mOperationsComboBoxHashReadLock.unlock();
                if (wi == null) {
                    continue;
                }
                final Object[] defaultValueE = Tools.extractUnit(defaultValue);
                Object value = wi.getValue();
                if (Tools.areEqual(value, new Object[]{"", ""})) {
                    value = new Object[]{getOpDefaultsDefault(param), null};
                }
                if (!Tools.areEqual(value, defaultValueE)) {
                    allAreDefaultValues = false;
                }
                final String savedOp = savedOperation.get(op, param);
                final Object[] savedOpE = Tools.extractUnit(savedOp);
                if (savedOp == null) {
                    if (!Tools.areEqual(value, defaultValueE)) {
                        changed = true;
                    }
                } else if (!Tools.areEqual(value, savedOpE)) {
                    changed = true;
                }
                wi.setBackground(defaultValueE, savedOpE, false);
            }
        }
        if (sameAsOperationsWi != null) {
            final Info info = sameAsOperationsWiValue();
            final boolean defaultValues =
                    info != null
                    && OPERATIONS_DEFAULT_VALUES_TEXT.equals(info.toString());
            final boolean nothingSelected =
                      info == null
                      || Widget.NOTHING_SELECTED.equals(info.toString());
            if (!nothingSelected
                && !defaultValues
                && info != savedOperationIdRef) {
                changed = true;
            } else {
                if ((nothingSelected || defaultValues)
                    && savedOperationIdRef != null) {
                    changed = true;
                }
                if (savedOperationIdRef == null
                    && defaultValues != allAreDefaultValues) {
                    if (allAreDefaultValues) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                sameAsOperationsWi.setValueNoListeners(
                                       OPERATIONS_DEFAULT_VALUES_TEXT);
                            }
                        });
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                sameAsOperationsWi.setValueNoListeners(
                                         Widget.NOTHING_SELECTED);
                            }
                        });
                    }
                }
            }
            sameAsOperationsWi.processAccessMode();
        }
        mSavedOperationsLock.unlock();
        return changed;
    }

    /**
     * Returns true if some of the scores have changed.
     */
    private boolean checkHostLocationsFieldsChanged() {
        boolean changed = false;
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Widget wi = scoreComboBoxHash.get(hi);
            final HostLocation hlSaved = savedHostLocations.get(hi);
            String hsSaved = null;
            String opSaved = null;
            if (hlSaved != null) {
                hsSaved = hlSaved.getScore();
                opSaved = hlSaved.getOperation();
            }
            final String opSavedLabel = getHostLocationLabel(host.getName(),
                                                             opSaved);
            if (wi == null) {
                continue;
            }
            String labelText = null;
            if (wi.getLabel() != null) {
                labelText = wi.getLabel().getText();
            }
            if (!Tools.areEqual(hsSaved, wi.getStringValue())
                || (!Tools.areEqual(opSavedLabel, labelText)
                    && (hsSaved != null  && !"".equals(hsSaved)))) {
                changed = true;
            }
            wi.setBackground(getHostLocationLabel(host.getName(), "eq"),
                             null,
                             opSavedLabel,
                             hsSaved,
                             false);
        }
        /* ping */
        final Widget pwi = pingComboBox;
        if (pwi != null) {
            if (!Tools.areEqual(savedPingOperation,
                                pwi.getValue())) {
                changed = true;
            }
            pwi.setBackground(null,
                              savedPingOperation,
                              false);
        }
        return changed;
    }

    /**
     * Returns the list of all services, that can be used in the 'add
     * service' action.
     */
    List<ResourceAgent> getAddServiceList(final String cl) {
        return getBrowser().globalGetAddServiceList(cl);
    }

    /**
     * Returns info object of all block devices on all hosts that have the
     * same names and other attributes.
     */
    Info[] getCommonBlockDevInfos(final Info defaultValue,
                                  final String serviceName) {
        final List<Info> list = new ArrayList<Info>();

        /* drbd resources */
        final Enumeration drbdResources = getBrowser().getDrbdNode().children();

        if (defaultValue != null) {
            list.add(defaultValue);
        }
        while (drbdResources.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                      (DefaultMutableTreeNode) drbdResources.nextElement();
            final DrbdResourceInfo drbdRes =
                                        (DrbdResourceInfo) n.getUserObject();
            final DefaultMutableTreeNode drbdResNode = drbdRes.getNode();
            if (drbdResNode != null) {
                final Enumeration drbdVolumes = drbdResNode.children();
                while (drbdVolumes.hasMoreElements()) {
                    final DefaultMutableTreeNode vn =
                           (DefaultMutableTreeNode) drbdVolumes.nextElement();
                    final CommonDeviceInterface drbdVol =
                                   (CommonDeviceInterface) vn.getUserObject();
                    list.add((Info) drbdVol);
                }
            }
        }

        /* block devices that are the same on all hosts */
        final Enumeration wids =
                        getBrowser().getCommonBlockDevicesNode().children();
        while (wids.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                               (DefaultMutableTreeNode) wids.nextElement();
            final CommonDeviceInterface wid =
                                (CommonDeviceInterface) n.getUserObject();
            list.add((Info) wid);
        }

        return list.toArray(new Info[list.size()]);
    }

    /** Selects the node in the menu and reloads everything underneath. */
    @Override
    public void selectMyself() {
        super.selectMyself();
        final DefaultMutableTreeNode node = getNode();
        if (node != null) {
            getBrowser().nodeChanged(node);
        }
    }

    /**
     * Adds clone fields to the option pane.
     */
    protected void addCloneFields(final JPanel optionsPanel,
                                  final int leftWidth,
                                  final int rightWidth) {
        final CloneInfo ci = getCloneInfo();

        final String[] params = ci.getParametersFromXML();
        final Info savedMAIdRef = ci.getSavedMetaAttrInfoRef();
        ci.getResource().setValue(GUI_ID, ci.getService().getId());
        ci.addParams(optionsPanel,
                     params,
                     ClusterBrowser.SERVICE_LABEL_WIDTH,
                     ClusterBrowser.SERVICE_FIELD_WIDTH,
                     ci.getSameAsFields(savedMAIdRef));
        if (!ci.getService().isNew()) {
            ci.getWidget(GUI_ID, null).setEnabled(false);
        }
        for (final String param : params) {
            if (ci.isMetaAttr(param)) {
                final Widget wi = ci.getWidget(param, null);
                wi.setEnabled(savedMAIdRef == null);
            }
        }

        ci.addHostLocations(optionsPanel,
                                   ClusterBrowser.SERVICE_LABEL_WIDTH,
                                   ClusterBrowser.SERVICE_FIELD_WIDTH);
    }

    /**
     * Returns label for host locations, which consist of host name and
     * operation.
     */
    private String getHostLocationLabel(final String hostName,
                                        final String op) {
        final StringBuilder sb = new StringBuilder(20);
        if (op == null || "eq".equals(op)) {
            sb.append("on ");
        } else if ("ne".equals(op)) {
            sb.append("NOT on ");
        } else {
            sb.append(op);
            sb.append(' ');
        }
        sb.append(hostName);
        return sb.toString();
    }

    /**
     * Creates host score combo boxes with labels, one per host.
     */
    protected void addHostLocations(final JPanel optionsPanel,
                                    final int leftWidth,
                                    final int rightWidth) {
        int rows = 0;

        final JPanel panel =
             getParamPanel(Tools.getString("ClusterBrowser.HostLocations"));
        panel.setLayout(new SpringLayout());

        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Map<String, String> abbreviations =
                                             new HashMap<String, String>();
            abbreviations.put("i", CRMXML.INFINITY_STRING);
            abbreviations.put("+", CRMXML.PLUS_INFINITY_STRING);
            abbreviations.put("I", CRMXML.INFINITY_STRING);
            abbreviations.put("a", "ALWAYS");
            abbreviations.put("n", "NEVER");
            final Widget wi =
                     new Widget(null,
                                new String[]{null,
                                             "0",
                                             "2",
                                             "ALWAYS",
                                             "NEVER",
                                             CRMXML.INFINITY_STRING,
                                             CRMXML.MINUS_INFINITY_STRING,
                                             CRMXML.INFINITY_STRING},
                                null, /* units */
                                null, /* type */
                                "^((-?\\d*|(-|\\+)?" + CRMXML.INFINITY_STRING
                                + "))|ALWAYS|NEVER|@NOTHING_SELECTED@$",
                                rightWidth,
                                abbreviations,
                                new AccessMode(ConfigData.AccessType.ADMIN,
                                               false));
            wi.setEditable(true);
            final Widget prevWi = scoreComboBoxHash.get(hi);
            scoreComboBoxHash.put(hi, wi);

            /* set selected host scores in the combo box from
             * savedHostLocations */
            if (prevWi == null) {
                final HostLocation hl = savedHostLocations.get(hi);
                String hsSaved = null;
                if (hl != null) {
                    hsSaved = hl.getScore();
                }
                wi.setValue(hsSaved);
            } else {
                wi.setValue(prevWi.getValue());
            }
        }

        /* host score combo boxes */
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Widget wi = scoreComboBoxHash.get(hi);
            String op = null;
            final HostLocation hl = savedHostLocations.get(hi);
            if (hl != null) {
                op = hl.getOperation();
            }
            final String text = getHostLocationLabel(hi.getName(), op);
            final JLabel label = new JLabel(text);
            final String onText = getHostLocationLabel(hi.getName(), "eq");
            final String notOnText = getHostLocationLabel(hi.getName(), "ne");
            label.addMouseListener(new MouseListener() {
                @Override
                public final void mouseClicked(final MouseEvent e) {
                    /* do nothing */
                }
                @Override
                public final void mouseEntered(final MouseEvent e) {
                    /* do nothing */
                }
                @Override
                public final void mouseExited(final MouseEvent e) {
                    /* do nothing */
                }
                @Override
                public final void mousePressed(final MouseEvent e) {
                    final String currentText = label.getText();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (currentText.equals(onText)) {
                                label.setText(notOnText);
                            } else if (currentText.equals(notOnText)) {
                                label.setText(onText);
                            } else {
                                /* wierd things */
                                label.setText(onText);
                            }
                            final String[] params = getParametersFromXML();
                            setApplyButtons(CACHED_FIELD, params);
                        }
                    });
                }
                @Override
                public final void mouseReleased(final MouseEvent e) {
                    /* do nothing */
                }
            });
            wi.setLabel(label, "");
            addField(panel,
                     label,
                     wi,
                     leftWidth,
                     rightWidth,
                     0);
            rows++;
        }
        rows += addPingField(panel, leftWidth, rightWidth);

        SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                        1, 1,           /* initX, initY */
                                        1, 1);          /* xPad, yPad */
        optionsPanel.add(panel);
    }

    /** Adds field with ping constraint. */
    private int addPingField(final JPanel panel,
                             final int leftWidth,
                             final int rightWidth) {
        int rows = 0;
        final JLabel pingLabel = new JLabel("pingd");
        String savedPO = null;
        final Widget prevWi = pingComboBox;
        if (prevWi == null) {
            savedPO = savedPingOperation;
        } else {
            savedPO = prevWi.getStringValue();
        }
        final Widget pingWi =
               new Widget(savedPO,
                          new StringInfo[]{new StringInfo(
                                             Widget.NOTHING_SELECTED,
                                             null,
                                             getBrowser()),
                                           new StringInfo(
                                             PING_ATTRIBUTES.get("defined"),
                                             "defined",
                                             getBrowser()),
                                           new StringInfo(
                                             PING_ATTRIBUTES.get("eq0"),
                                             "eq0",
                                             getBrowser())},
                          null, /* units */
                          null, /* type */
                          null, /* regexp */
                          rightWidth,
                          null, /* abbreviations */
                          new AccessMode(ConfigData.AccessType.ADMIN,
                                         false));
        addField(panel, pingLabel, pingWi, leftWidth, rightWidth, 0);
        pingWi.setLabel(pingLabel,
                        Tools.getString("ServiceInfo.PingdToolTip"));
        if (resourceAgent.isPingService() && savedPingOperation == null) {
            pingWi.setEnabled(false);
        }
        pingComboBox = pingWi;
        rows++;
        return rows;
    }

    /**
     * Returns whetrher this service's meta attributes are referenced by
     * some other service.
     */
    private boolean isMetaAttrReferenced() {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        getBrowser().mHeartbeatIdToServiceLock();
        final Map<String, ServiceInfo> services =
                                    getBrowser().getHeartbeatIdToServiceInfo();
        for (final ServiceInfo si : services.values()) {
            final String refCRMId = cs.getMetaAttrsRef(
                                           si.getService().getHeartbeatId());
            if (refCRMId != null
                && refCRMId.equals(getService().getHeartbeatId())) {
                getBrowser().mHeartbeatIdToServiceUnlock();
                return true;
            }
        }
        getBrowser().mHeartbeatIdToServiceUnlock();
        return false;
    }

    /**
     * Sets meta attrs with same values as other service info, or default
     * values.
     */
    private void setMetaAttrsSameAs(final Info info) {
        if (sameAsMetaAttrsWi == null || info == null) {
            return;
        }
        boolean nothingSelected = false;
        if (Widget.NOTHING_SELECTED.equals(info.toString())) {
            nothingSelected = true;
        }
        boolean sameAs = true;
        if (META_ATTRS_DEFAULT_VALUES_TEXT.equals(info.toString())) {
            sameAs = false;
        }
        final String[] params = getParametersFromXML();
        if (params != null) {
            for (final String param : params) {
                if (!isMetaAttr(param)) {
                    continue;
                }
                String defaultValue = getParamPreferred(param);
                if (defaultValue == null) {
                    defaultValue = getParamDefault(param);
                }
                final Widget wi = getWidget(param, null);
                if (wi == null) {
                    continue;
                }
                Object oldValue = wi.getValue();
                if (oldValue == null) {
                    oldValue = defaultValue;
                }
                wi.setEnabled(!sameAs || nothingSelected);
                if (!nothingSelected) {
                    if (sameAs) {
                        /* same as some other service */
                        defaultValue =
                                 ((ServiceInfo) info).getParamSaved(param);
                    }
                    final String newValue = defaultValue;
                    if (!Tools.areEqual(oldValue, newValue)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (wi != null) {
                                    wi.setValue(newValue);
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * Returns whetrher this service's operations are referenced by some
     * other service.
     */
    private boolean isOperationReferenced() {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        getBrowser().mHeartbeatIdToServiceLock();
        final Map<String, ServiceInfo> services =
                                    getBrowser().getHeartbeatIdToServiceInfo();
        for (final ServiceInfo si : services.values()) {
            final String refCRMId = cs.getOperationsRef(
                                        si.getService().getHeartbeatId());
            if (refCRMId != null
                && refCRMId.equals(getService().getHeartbeatId())) {
                getBrowser().mHeartbeatIdToServiceUnlock();
                return true;
            }
        }
        getBrowser().mHeartbeatIdToServiceUnlock();
        return false;
    }

    /** Returns selected operations id reference. */
    private Info getSameServiceOpIdRef() {
        mSavedOperationsLock.lock();
        final ServiceInfo savedOpIdRef = savedOperationIdRef;
        mSavedOperationsLock.unlock();
        return savedOpIdRef;
    }

    /**
     * Returns all services except this one, that are of the same type
     * for meta attributes.
     */
    private Info[] getSameServicesMetaAttrs() {
        final List<Info> sl = new ArrayList<Info>();
        sl.add(new StringInfo(Widget.NOTHING_SELECTED,
                              null,
                              getBrowser()));
        sl.add(new StringInfo(META_ATTRS_DEFAULT_VALUES_TEXT,
                              META_ATTRS_DEFAULT_VALUES,
                              getBrowser()));
        final Host dcHost = getBrowser().getDCHost();
        if (isMetaAttrReferenced() || Tools.versionBeforePacemaker(dcHost)) {
            return sl.toArray(new Info[sl.size()]);
        }
        getBrowser().lockNameToServiceInfo();
        final Map<String, ServiceInfo> idToInfoHash =
                        getBrowser().getNameToServiceInfoHash().get(getName());
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (idToInfoHash != null) {
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(
                                                  idToInfoHash.values())) {
                if (si != this
                    && cs.getMetaAttrsId(
                                si.getService().getHeartbeatId()) != null
                    && cs.getMetaAttrsRef(
                               si.getService().getHeartbeatId()) == null) {
                    sl.add(si);
                }
            }
        }
        final boolean clone = getResourceAgent().isClone();
        for (final String name
                          : getBrowser().getNameToServiceInfoHash().keySet()) {
            final Map<String, ServiceInfo> idToInfo =
                             getBrowser().getNameToServiceInfoHash().get(name);
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(
                                                  idToInfo.values())) {
                if (si != this
                    && !si.getName().equals(getName())
                    && si.getResourceAgent() != null
                    && si.getResourceAgent().isClone() == clone
                    && cs.getMetaAttrsId(
                               si.getService().getHeartbeatId()) != null
                    && cs.getMetaAttrsRef(
                               si.getService().getHeartbeatId()) == null) {
                    sl.add(si);
                }
            }
        }
        getBrowser().unlockNameToServiceInfo();
        return sl.toArray(new Info[sl.size()]);
    }

    /**
     * Returns all services except this one, that are of the same type
     * for operations.
     */
    private Info[] getSameServicesOperations() {
        final List<Info> sl = new ArrayList<Info>();
        sl.add(new StringInfo(Widget.NOTHING_SELECTED,
                              null,
                              getBrowser()));
        sl.add(new StringInfo(OPERATIONS_DEFAULT_VALUES_TEXT,
                              OPERATIONS_DEFAULT_VALUES,
                              getBrowser()));
        final Host dcHost = getBrowser().getDCHost();
        final String pmV = dcHost.getPacemakerVersion();
        final String hbV = dcHost.getHeartbeatVersion();
        if (isOperationReferenced() || Tools.versionBeforePacemaker(dcHost)) {
            return sl.toArray(new Info[sl.size()]);
        }
        getBrowser().lockNameToServiceInfo();
        final Map<String, ServiceInfo> idToInfoHash =
                        getBrowser().getNameToServiceInfoHash().get(getName());
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (idToInfoHash != null) {
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(
                                                  idToInfoHash.values())) {
                if (si != this
                    && cs.getOperationsId(
                                si.getService().getHeartbeatId()) != null
                    && cs.getOperationsRef(
                               si.getService().getHeartbeatId()) == null) {
                    sl.add(si);
                }
            }
        }
        final boolean clone = getResourceAgent().isClone();
        for (final String name
                           : getBrowser().getNameToServiceInfoHash().keySet()) {
            final Map<String, ServiceInfo> idToInfo =
                             getBrowser().getNameToServiceInfoHash().get(name);
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(
                                                  idToInfo.values())) {
                if (si != this
                    && si.getResourceAgent() != null
                    && si.getResourceAgent().isClone() == clone
                    && !si.getName().equals(getName())
                    && cs.getOperationsId(
                               si.getService().getHeartbeatId()) != null
                    && cs.getOperationsRef(
                               si.getService().getHeartbeatId()) == null) {
                    sl.add(si);
                }
            }
        }
        getBrowser().unlockNameToServiceInfo();
        return sl.toArray(new Info[sl.size()]);
    }

    /**
     * Sets operations with same values as other service info, or default
     * values.
     */
    private void setOperationsSameAs(final Info info) {
        if (sameAsOperationsWi == null) {
            return;
        }
        boolean nothingSelected = false;
        if (info == null || Widget.NOTHING_SELECTED.equals(info.toString())) {
            nothingSelected = true;
        }
        boolean sameAs = true;
        if (info == null
            || OPERATIONS_DEFAULT_VALUES_TEXT.equals(info.toString())) {
            sameAs = false;
        }
        mSavedOperationsLock.lock();
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCRMOperationParams(op)) {
                String defaultValue =
                                resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null) {
                    continue;
                }
                if (ClusterBrowser.HB_OP_IGNORE_DEFAULT.contains(op)) {
                    defaultValue = "";
                }
                mOperationsComboBoxHashReadLock.lock();
                final Widget wi = operationsComboBoxHash.get(op, param);
                mOperationsComboBoxHashReadLock.unlock();
                final Object oldValue = wi.getValue();
                wi.setEnabled(!sameAs || nothingSelected);
                if (!nothingSelected) {
                    if (sameAs) {
                        /* same as some other service */
                        defaultValue =
                          ((ServiceInfo) info).getSavedOperation().get(op,
                                                                       param);
                    }
                    final String newValue = defaultValue;
                    if (!Tools.areEqual(oldValue,
                                        Tools.extractUnit(newValue))) {
                        if (wi != null) {
                            wi.setValueNoListeners(newValue);
                        }
                    }
                }
            }
        }
        mSavedOperationsLock.unlock();
    }

    /** Creates operations combo boxes with labels. */
    protected void addOperations(final JPanel optionsPanel,
                                 final int leftWidth,
                                 final int rightWidth) {
        int rows = 0;
        final JPanel sectionPanel = getParamPanel(
                                Tools.getString("ClusterBrowser.Operations"));
        String defaultOpIdRef = null;
        final Info savedOpIdRef = getSameServiceOpIdRef();
        if (savedOpIdRef != null) {
            defaultOpIdRef = savedOpIdRef.toString();
        }
        sameAsOperationsWi = new Widget(defaultOpIdRef,
                                        getSameServicesOperations(),
                                        null, /* units */
                                        null, /* type */
                                        null, /* regexp */
                                        rightWidth,
                                        null, /* abbrv */
                                        new AccessMode(
                                                  ConfigData.AccessType.ADMIN,
                                                  false));
        sameAsOperationsWi.setToolTipText(defaultOpIdRef);
        final JLabel label = new JLabel(Tools.getString(
                                           "ClusterBrowser.OperationsSameAs"));
        sameAsOperationsWi.setLabel(label, "");
        final JPanel saPanel = new JPanel(new SpringLayout());
        saPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        addField(saPanel,
                 label,
                 sameAsOperationsWi,
                 leftWidth,
                 rightWidth,
                 0);
        SpringUtilities.makeCompactGrid(saPanel, 1, 2,
                                        1, 1,  // initX, initY
                                        1, 1); // xPad, yPad
        sectionPanel.add(saPanel);
        boolean allAreDefaultValues = true;
        mSavedOperationsLock.lock();
        final JPanel normalOpPanel = new JPanel(new SpringLayout());
        normalOpPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        int normalRows = 0;
        final JPanel advancedOpPanel = new JPanel(new SpringLayout());
        advancedOpPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        addToAdvancedList(advancedOpPanel);
        advancedOpPanel.setVisible(Tools.getConfigData().isAdvancedMode());
        int advancedRows = 0;
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCRMOperationParams(op)) {
                String defaultValue =
                                   resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null) {
                    continue;
                }
                if (ClusterBrowser.HB_OP_IGNORE_DEFAULT.contains(op)) {
                    defaultValue = "";
                }
                Widget.Type type;
                final String regexp = "^-?\\d*$";
                type = Widget.Type.TEXTFIELDWITHUNIT;
                // TODO: old style resources
                if (defaultValue == null) {
                    defaultValue = "0";
                }
                String savedValue = null;
                mOperationsComboBoxHashWriteLock.lock();
                try {
                    final Widget prevWi = operationsComboBoxHash.get(op, param);
                    if (prevWi != null) {
                        savedValue = prevWi.getStringValue();
                    }
                } finally {
                    mOperationsComboBoxHashWriteLock.unlock();
                }
                if (savedValue == null) {
                    savedValue = savedOperation.get(op, param);
                }
                if (!getService().isNew()
                    && (savedValue == null || "".equals(savedValue))) {
                    savedValue = getOpDefaultsDefault(param);
                    if (savedValue == null) {
                        savedValue = "";
                    }
                }
                if (!defaultValue.equals(savedValue)) {
                    allAreDefaultValues = false;
                }
                if (savedValue != null) {
                    defaultValue = savedValue;
                }
                final Widget wi = new Widget(defaultValue,
                                             null, /* items */
                                             getUnits(),
                                             type,
                                             regexp,
                                             rightWidth,
                                             null, /* abbrv */
                                             new AccessMode(
                                                   ConfigData.AccessType.ADMIN,
                                                   false));
                wi.setEnabled(savedOpIdRef == null);

                mOperationsComboBoxHashWriteLock.lock();
                try {
                    operationsComboBoxHash.put(op, param, wi);
                } finally {
                    mOperationsComboBoxHashWriteLock.unlock();
                }
                rows++;
                final JLabel wiLabel = new JLabel(Tools.ucfirst(op)
                                                  + " / "
                                                  + Tools.ucfirst(param));
                wi.setLabel(wiLabel, "");
                JPanel panel;
                if (getBrowser().isCRMOperationAdvanced(op, param)) {
                    panel = advancedOpPanel;
                    advancedRows++;
                } else {
                    panel = normalOpPanel;
                    normalRows++;
                }
                addField(panel,
                         wiLabel,
                         wi,
                         leftWidth,
                         rightWidth,
                         0);
            }
        }
        SpringUtilities.makeCompactGrid(normalOpPanel, normalRows, 2,
                                        1, 1,  // initX, initY
                                        1, 1); // xPad, yPad
        SpringUtilities.makeCompactGrid(advancedOpPanel, advancedRows, 2,
                                        1, 1,  // initX, initY
                                        1, 1); // xPad, yPad
        sectionPanel.add(normalOpPanel);
        sectionPanel.add(getMoreOptionsPanel(leftWidth + rightWidth + 4));
        sectionPanel.add(advancedOpPanel);
        mSavedOperationsLock.unlock();
        if (allAreDefaultValues && savedOpIdRef == null) {
            sameAsOperationsWi.setValue(OPERATIONS_DEFAULT_VALUES_TEXT);
        }
        sameAsOperationsWi.addListeners(
                        new WidgetListener() {
                            @Override
                            public void check(final Object value) {
                                final Info info = sameAsOperationsWiValue();
                                setOperationsSameAs(info);
                                final String[] params = getParametersFromXML();
                                setApplyButtons(CACHED_FIELD, params);
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (info != null) {
                                            sameAsOperationsWi.setToolTipText(
                                                              info.toString());
                                        }
                                    }
                                });
                }
            });
        optionsPanel.add(sectionPanel);
    }

    /** Returns parameters. */
    @Override
    public String[] getParametersFromXML() {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParameters(resourceAgent, getService().isMaster());
    }

    /** Returns the regexp of the parameter. */
    @Override
    protected String getParamRegexp(final String param) {
        if (isInteger(param)) {
            return "^((-?\\d*|(-|\\+)?" + CRMXML.INFINITY_STRING
                   + "|" + CRMXML.DISABLED_STRING
                   + "))|@NOTHING_SELECTED@$";
        }
        return null;
    }

    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final String newValue) {
        if (param.equals("ip")
            && newValue != null
            && !Tools.isIp(newValue)) {
            return false;
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.checkParam(resourceAgent, param, newValue);
    }

    /** Returns default value for specified parameter. */
    @Override
    public String getParamDefault(final String param) {
        if (isMetaAttr(param)) {
            final String paramDefault = getBrowser().getRscDefaultsInfo()
                                                 .getResource().getValue(param);
            if (paramDefault != null) {
                return paramDefault;
            }
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParamDefault(resourceAgent, param);
    }

    /** Returns saved value for specified parameter. */
    @Override
    protected String getParamSaved(final String param) {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        if (isMetaAttr(param)) {
            final String crmId = getService().getHeartbeatId();
            final String refCRMId = clStatus.getMetaAttrsRef(crmId);
            if (refCRMId != null) {
                String value = clStatus.getParameter(refCRMId, param, false);
                if (value == null) {
                    value = getParamPreferred(param);
                    if (value == null) {
                        return getParamDefault(param);
                    }
                }
                return value;
            }
        }
        String value = super.getParamSaved(param);
        if (value == null) {
            value = clStatus.getParameter(getService().getHeartbeatId(),
                                    param,
                                    false);
            if (value == null) {
                if (getService().isNew()) {
                    value = getParamPreferred(param);
                }
                if (value == null) {
                    return getParamDefault(param);
                }
            }
        }
        return value;
    }

    /**
     * Returns preferred value for specified parameter.
     */
    @Override
    protected String getParamPreferred(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParamPreferred(resourceAgent, param);
    }

    /**
     * Returns possible choices for drop down lists.
     */
    @Override
    protected Object[] getParamPossibleChoices(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (isCheckBox(param)) {
            return crmXML.getCheckBoxChoices(resourceAgent, param);
        } else {
            final CloneInfo ci = getCloneInfo();
            final boolean ms = ci != null
                               && ci.getService().isMaster();
            return crmXML.getParamPossibleChoices(resourceAgent, param, ms);
        }
    }

    /**
     * Returns short description of the specified parameter.
     */
    @Override
    protected String getParamShortDesc(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParamShortDesc(resourceAgent, param);
    }

    /**
     * Returns long description of the specified parameter.
     */
    @Override
    protected String getParamLongDesc(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParamLongDesc(resourceAgent, param);
    }

    /**
     * Returns section to which the specified parameter belongs.
     */
    @Override
    protected String getSection(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getSection(resourceAgent, param);
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRequired(resourceAgent, param);
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param),
                            getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isAdvanced(resourceAgent, param);
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected final String isEnabled(final String param) {
        if (GUI_ID.equals(param) && !getResource().isNew()) {
            return "";
        }
        if (isMetaAttr(param)) {
            final Info info = (Info) sameAsMetaAttrsWi.getValue();
            if (info == null) {
                return null;
            }
            boolean nothingSelected = false;
            if (Widget.NOTHING_SELECTED.equals(info.toString())) {
                nothingSelected = true;
            }
            boolean sameAs = true;
            if (META_ATTRS_DEFAULT_VALUES_TEXT.equals(info.toString())) {
                sameAs = false;
            }
            if (!sameAs || nothingSelected) {
                return null;
            } else {
                return "";
            }
        }
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected final boolean isEnabledOnlyInAdvancedMode(final String param) {
        return false;
    }


    /** Returns access type of this parameter. */
    @Override
    protected ConfigData.AccessType getAccessType(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getAccessType(resourceAgent, param);
    }

    /**
     * Returns true if the specified parameter is meta attribute.
     */
    protected boolean isMetaAttr(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isMetaAttr(resourceAgent, param);
    }

    /** Returns true if the specified parameter is integer. */
    @Override
    protected boolean isInteger(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isInteger(resourceAgent, param);
    }

    /** Returns true if the specified parameter is label. */
    @Override
    protected boolean isLabel(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isLabel(resourceAgent, param);
    }

    /** Returns true if the specified parameter is of time type. */
    @Override
    protected boolean isTimeType(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isTimeType(resourceAgent, param);
    }

    /** Returns whether parameter is checkbox. */
    @Override
    protected boolean isCheckBox(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isBoolean(resourceAgent, param);
    }

    /** Returns the type of the parameter according to the OCF. */
    @Override
    protected String getParamType(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getParamType(resourceAgent, param);
    }

    /** Returns the type of the parameter. */
    @Override
    protected Widget.Type getFieldType(final String param) {
        return resourceAgent.getFieldType(param);
    }

    /**
     * Is called before the service is added. This is for example used by
     * FilesystemInfo so that it can add LinbitDrbdInfo or DrbddiskInfo
     * before it adds itself.
     */
    void addResourceBefore(final Host dcHost, final boolean testOnly) {
        /* Override to add resource before this one. */
    }

    /** Change type to Master, Clone or Primitive. */
    protected final void changeType(final String value) {
        boolean masterSlave = false;
        boolean clone = false;
        if (MASTER_SLAVE_TYPE_STRING.equals(value)) {
            masterSlave = true;
            clone = true;
        } else if (CLONE_TYPE_STRING.equals(value)) {
            clone = true;
        }

        final ServiceInfo thisClass = this;
        if (clone) {
            final CRMXML crmXML = getBrowser().getCRMXML();
            final CloneInfo oldCI = getCloneInfo();
            String title = ConfigData.PM_CLONE_SET_NAME;
            if (masterSlave) {
                title = ConfigData.PM_MASTER_SLAVE_SET_NAME;
            }
            final CloneInfo ci = new CloneInfo(crmXML.getHbClone(),
                                               title,
                                               masterSlave,
                                               getBrowser());
            setCloneInfo(ci);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (oldCI == null) {
                        getBrowser().getHeartbeatGraph()
                                    .exchangeObjectInTheVertex(ci, thisClass);
                        ci.setPingComboBox(pingComboBox);
                        for (final HostInfo hi : scoreComboBoxHash.keySet()) {
                            ci.getScoreComboBoxHash().put(
                                                    hi,
                                                    scoreComboBoxHash.get(hi));
                        }
                        final Widget prevWi = getWidget(GUI_ID, null);
                        if (prevWi != null) {
                            ci.getService().setId(
                                    getName() + "_" + prevWi.getStringValue());
                        }
                    } else {
                        oldCI.removeNode();
                        getBrowser().getHeartbeatGraph()
                                    .exchangeObjectInTheVertex(ci, oldCI);
                        cleanup();
                        oldCI.cleanup();
                        ci.setPingComboBox(oldCI.getPingComboBox());
                        for (final HostInfo hi
                                    : oldCI.getScoreComboBoxHash().keySet()) {
                            ci.getScoreComboBoxHash().put(
                                    hi, oldCI.getScoreComboBoxHash().get(hi));
                        }
                        getBrowser().removeFromServiceInfoHash(oldCI);
                        getBrowser().mHeartbeatIdToServiceLock();
                        getBrowser().getHeartbeatIdToServiceInfo().remove(
                                          oldCI.getService().getHeartbeatId());
                        getBrowser().mHeartbeatIdToServiceUnlock();
                        final DefaultMutableTreeNode oldCINode =
                                                               oldCI.getNode();
                        if (oldCINode != null) {
                            oldCINode.setUserObject(null); /* would leak
                                                              without it*/
                        }
                        ci.getService().setId(oldCI.getWidget(
                                               GUI_ID, null).getStringValue());
                    }
                    ci.setCloneServicePanel(thisClass);
                    infoPanel = null;
                }
            });
        } else if (PRIMITIVE_TYPE_STRING.equals(value)) {
            final CloneInfo ci = getCloneInfo();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setPingComboBox(ci.getPingComboBox());
                    for (final HostInfo hi
                                        : ci.getScoreComboBoxHash().keySet()) {
                        scoreComboBoxHash.put(
                                        hi, ci.getScoreComboBoxHash().get(hi));
                    }
                    final DefaultMutableTreeNode node = getNode();
                    final DefaultMutableTreeNode ciNode = ci.getNode();
                    removeNode();
                    ci.removeNode();
                    cleanup();
                    ci.cleanup();
                    setNode(node);
                    getBrowser().getServicesNode().add(node);
                    getBrowser().getHeartbeatGraph().exchangeObjectInTheVertex(
                                                                     thisClass,
                                                                     ci);
                    getBrowser().mHeartbeatIdToServiceLock();
                    getBrowser().getHeartbeatIdToServiceInfo().remove(
                                            ci.getService().getHeartbeatId());
                    getBrowser().mHeartbeatIdToServiceUnlock();
                    getBrowser().removeFromServiceInfoHash(ci);
                    infoPanel = null;
                    setCloneInfo(null);
                    selectMyself();
                    ciNode.setUserObject(null); /* would leak without it */
                }
            });
        }
    }

    /** Adds host score listeners. */
    protected void addHostLocationsListeners() {
        final String[] params = getParametersFromXML();
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Widget wi = scoreComboBoxHash.get(hi);
            wi.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Object value) {
                                    setApplyButtons(CACHED_FIELD, params);
                                    SwingUtilities.invokeLater(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                wi.setEditable();
                                            }
                                    });
                                }
                            });
        }
        pingComboBox.addListeners(new WidgetListener() {
                                      @Override
                                      public void check(final Object value) {
                                          setApplyButtons(CACHED_FIELD, params);
                                      }
                                  });
    }

    /** Adds listeners for operation and parameter. */
    private void addOperationListeners(final String op, final String param) {
        final String dv = resourceAgent.getOperationDefault(op, param);
        if (dv == null) {
            return;
        }
        mOperationsComboBoxHashReadLock.lock();
        final Widget wi = operationsComboBoxHash.get(op, param);
        mOperationsComboBoxHashReadLock.unlock();
        final String[] params = getParametersFromXML();
        wi.addListeners(new WidgetListener() {
                            @Override
                            public void check(final Object value) {
                                setApplyButtons(CACHED_FIELD, params);
                            }
                        });
    }

    /**
     * Returns "same as" fields for some sections. Currently only "meta
     * attributes".
     */
    protected final Map<String, Widget> getSameAsFields(
                                                final Info savedMAIdRef) {
        String defaultMAIdRef = null;
        if (savedMAIdRef != null) {
            defaultMAIdRef = savedMAIdRef.toString();
        }
        sameAsMetaAttrsWi = new Widget(defaultMAIdRef,
                                       getSameServicesMetaAttrs(),
                                       null, /* units */
                                       null, /* type */
                                       null, /* regexp */
                                       ClusterBrowser.SERVICE_FIELD_WIDTH,
                                       null, /* abbrv */
                                       new AccessMode(
                                                   ConfigData.AccessType.ADMIN,
                                                   false));
        sameAsMetaAttrsWi.setToolTipText(defaultMAIdRef);
        final Map<String, Widget> sameAsFields = new HashMap<String, Widget>();
        sameAsFields.put("Meta Attributes", sameAsMetaAttrsWi);
        sameAsMetaAttrsWi.addListeners(new WidgetListener() {
                            @Override
                            public void check(final Object value) {
                                Info i = null;
                                final Object o =
                                      sameAsMetaAttrsWi.getValue();
                                if (o instanceof Info) {
                                    i = (Info) o;
                                }
                                final Info info = i;
                                setMetaAttrsSameAs(info);
                                final String[] params =
                                                    getParametersFromXML();
                                setApplyButtons(CACHED_FIELD, params);
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (info != null) {
                                            sameAsMetaAttrsWi.setToolTipText(
                                                              info.toString());
                                        }
                                    }
                                });
                            }
                        });
        return sameAsFields;
    }

    /** Returns saved meta attributes reference to another service. */
    protected final Info getSavedMetaAttrInfoRef() {
        return savedMetaAttrInfoRef;
    }

    /** Returns info panel with comboboxes for service parameters. */
    @Override
    public JComponent getInfoPanel() {
        if (!getResourceAgent().isMetaDataLoaded()) {
            final JPanel p = new JPanel();
            p.add(new JLabel(Tools.getString("ServiceInfo.LoadingMetaData")));
            return p;
        }
        final CloneInfo ci = getCloneInfo();
        if (ci == null) {
            getBrowser().getHeartbeatGraph().pickInfo(this);
        } else {
            getBrowser().getHeartbeatGraph().pickInfo(ci);
        }
        if (infoPanel != null) {
            return infoPanel;
        }
        /* init save button */
        final boolean abExisted = getApplyButton() != null;
        final ServiceInfo thisClass = this;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;
            /**
             * Whether the whole thing should be enabled.
             */
            @Override
            public final boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                if (dcHost == null) {
                    return false;
                }
                if (Tools.versionBeforePacemaker(dcHost)) {
                    return false;
                }
                return true;
            }
            @Override
            public final void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getHeartbeatGraph().stopTestAnimation(
                                                             getApplyButton());
                getApplyButton().setToolTipText(null);
            }

            @Override
            public final void mouseOver() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                getApplyButton().setToolTipText(
                                        ClusterBrowser.STARTING_PTEST_TOOLTIP);
                getApplyButton().setToolTipBackground(Tools.getDefaultColor(
                                   "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getHeartbeatGraph().startTestAnimation(
                                                               getApplyButton(),
                                                               startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                final ClusterStatus cs = getBrowser().getClusterStatus();
                cs.setPtestData(null);
                apply(dcHost, true);
                final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                getApplyButton().setToolTipText(ptestData.getToolTip());
                cs.setPtestData(ptestData);
                getBrowser().ptestLockRelease();
                startTestLatch.countDown();
            }
        };
        if (getResourceAgent().isGroup()) {
            initApplyButton(buttonCallback,
                            Tools.getString("Browser.ApplyGroup"));
        } else {
            initApplyButton(buttonCallback);
        }
        if (ci != null) {
            ci.setApplyButton(getApplyButton());
            ci.setRevertButton(getRevertButton());
        }
        /* add item listeners to the apply button. */
        if (!abExisted) {
            getApplyButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), false);
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );

            getRevertButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                revert();
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
        }
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        AbstractButton serviceMenu;
        if (ci == null) {
            serviceMenu = getActionsButton();
        } else {
            serviceMenu = ci.getActionsButton();
        }
        buttonPanel.add(serviceMenu, BorderLayout.EAST);
        String defaultValue = PRIMITIVE_TYPE_STRING;
        if (ci != null) {
            if (ci.getService().isMaster()) {
                defaultValue = MASTER_SLAVE_TYPE_STRING;
            } else {
                defaultValue = CLONE_TYPE_STRING;
            }
        }
        if (!getResourceAgent().isClone() && getGroupInfo() == null) {
            typeRadioGroup = new Widget(
                                     defaultValue,
                                     new String[]{PRIMITIVE_TYPE_STRING,
                                                  CLONE_TYPE_STRING,
                                                  MASTER_SLAVE_TYPE_STRING},
                                     null, /* units */
                                     Widget.Type.RADIOGROUP,
                                     null, /* regexp */
                                     ClusterBrowser.SERVICE_LABEL_WIDTH
                                     + ClusterBrowser.SERVICE_FIELD_WIDTH,
                                     null, /* abbrv */
                                     new AccessMode(ConfigData.AccessType.ADMIN,
                                                    false));

            if (!getService().isNew()) {
                typeRadioGroup.setEnabled(false);
            }
            typeRadioGroup.addListeners(new WidgetListener() {
                @Override
                public void check(final Object value) {
                    changeType(((JRadioButton) value).getText());
                }
            });
            final JPanel tp = new JPanel();
            tp.setBackground(ClusterBrowser.PANEL_BACKGROUND);
            tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
            tp.add(typeRadioGroup);
            typeRadioGroup.setBackgroundColor(ClusterBrowser.PANEL_BACKGROUND);
            optionsPanel.add(tp);
        }
        if (ci != null) {
            /* add clone fields */
            addCloneFields(optionsPanel,
                           ClusterBrowser.SERVICE_LABEL_WIDTH,
                           ClusterBrowser.SERVICE_FIELD_WIDTH);
        }
        getResource().setValue(GUI_ID, getService().getId());

        /* get dependent resources and create combo boxes for ones, that
         * need parameters */
        final String[] params = getParametersFromXML();
        final Info savedMAIdRef = savedMetaAttrInfoRef;
        addParams(optionsPanel,
                  params,
                  ClusterBrowser.SERVICE_LABEL_WIDTH,
                  ClusterBrowser.SERVICE_FIELD_WIDTH,
                  getSameAsFields(savedMAIdRef));
        if (ci == null) {
            /* score combo boxes */
            addHostLocations(optionsPanel,
                             ClusterBrowser.SERVICE_LABEL_WIDTH,
                             ClusterBrowser.SERVICE_FIELD_WIDTH);
        }

        for (final String param : params) {
            if (isMetaAttr(param)) {
                final Widget wi = getWidget(param, null);
                wi.setEnabled(savedMAIdRef == null);
            }
        }
        if (!getService().isNew()) {
            getWidget(GUI_ID, null).setEnabled(false);
        }
        if (!getResourceAgent().isGroup()
            && !getResourceAgent().isClone()) {
            /* Operations */
            addOperations(optionsPanel,
                          ClusterBrowser.SERVICE_LABEL_WIDTH,
                          ClusterBrowser.SERVICE_FIELD_WIDTH);
            /* add item listeners to the operations combos */
            for (final String op : getResourceAgent().getOperationNames()) {
                for (final String param
                              : getBrowser().getCRMOperationParams(op)) {
                    addOperationListeners(op, param);
                }
            }
        }
        /* add item listeners to the host scores combos */
        if (ci == null) {
            addHostLocationsListeners();
        } else {
            ci.addHostLocationsListeners();
        }
        /* apply button */
        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                /* invoke later on purpose  */
                setApplyButtons(null, params);
            }
        });
        mainPanel.add(optionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        newPanel.add(new JScrollPane(mainPanel));
        /* if id textfield was changed and this id is not used,
         * enable apply button */
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /** Clears the info panel cache, forcing it to reload. */
    @Override
    boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Returns operation from host location label. "eq", "ne" etc. */
    private String getOpFromLabel(final String onHost,
                                  final String labelText) {
        final int l = labelText.length();
        final int k = onHost.length();
        String op = null;
        if (l > k) {
            final String labelPart = labelText.substring(0, l - k - 1);
            if ("on".equals(labelPart)) {
                op = "eq";
            } else if ("NOT on".equals(labelPart)) {
                op = "ne";
            } else {
                op = labelPart;
            }
        }
        return op;
    }

    /** Goes through the scores and sets preferred locations. */
    protected void setLocations(final String heartbeatId,
                                final Host dcHost,
                                final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Widget wi = scoreComboBoxHash.get(hi);
            String hs = wi.getStringValue();
            if ("ALWAYS".equals(hs)) {
                hs = CRMXML.INFINITY_STRING;
            } else if ("NEVER".equals(hs)) {
                hs = CRMXML.MINUS_INFINITY_STRING;
            }
            final HostLocation hlSaved = savedHostLocations.get(hi);
            String hsSaved = null;
            String opSaved = null;
            if (hlSaved != null) {
                hsSaved = hlSaved.getScore();
                opSaved = hlSaved.getOperation();
            }
            final String onHost = hi.getName();
            final String op = getOpFromLabel(onHost, wi.getLabel().getText());
            final HostLocation hostLoc = new HostLocation(hs, op, null, null);
            if (!hostLoc.equals(hlSaved)) {
                String locationId = cs.getLocationId(getHeartbeatId(testOnly),
                                                     onHost,
                                                     testOnly);
                if (((hs == null || "".equals(hs))
                    || !Tools.areEqual(op, opSaved))
                    && locationId != null) {
                    CRM.removeLocation(dcHost,
                                       locationId,
                                       getHeartbeatId(testOnly),
                                       testOnly);
                    locationId = null;
                }
                if (hs != null && !"".equals(hs)) {
                    CRM.setLocation(dcHost,
                                    getHeartbeatId(testOnly),
                                    onHost,
                                    hostLoc,
                                    locationId,
                                    testOnly);
                }
            }
        }
        /* ping */
        final Widget pwi = pingComboBox;
        if (pwi != null) {
            String value = null;
            final Object o = pwi.getValue();
            if (o != null) {
                value = ((StringInfo) o).getStringValue();
            }
            final String locationId = null;
            if (!Tools.areEqual(savedPingOperation,
                                value)) {
                final String pingLocationId = cs.getPingLocationId(
                                                    getHeartbeatId(testOnly),
                                                    testOnly);
                if (pingLocationId != null) {
                    CRM.removeLocation(dcHost,
                                       pingLocationId,
                                       getHeartbeatId(testOnly),
                                       testOnly);
                }
                if (value != null) {
                    CRM.setPingLocation(dcHost,
                                        getHeartbeatId(testOnly),
                                        value,
                                        null, /* location id */
                                        testOnly);
                }
            }
        }
        if (!testOnly) {
            storeHostLocations();
        }
    }

    /**
     * Returns hash with changed operation ids and all name, value pairs.
     * This works for new heartbeats >= 2.99.0
     */
    protected Map<String, Map<String, String>> getOperations(
                                                    final String heartbeatId) {
        final Map<String, Map<String, String>> operations =
                              new LinkedHashMap<String, Map<String, String>>();

        final ClusterStatus cs = getBrowser().getClusterStatus();
        final CloneInfo ci = getCloneInfo();
        for (final String op : getResourceAgent().getOperationNames()) {
            final Map<String, String> opHash =
                                           new LinkedHashMap<String, String>();
            String opId = cs.getOpId(heartbeatId, op);
            if (opId == null) {
                /* generate one */
                opId = "op-" + heartbeatId + "-" + op;
            }
            /* operations have different kind of default, that is
             * recommended, but not used by default. */
            boolean firstTime = true;
            for (final String param : ClusterBrowser.HB_OPERATION_PARAM_LIST) {
                if (getBrowser().getCRMOperationParams(op).contains(param)) {
                    if (ci == null
                        && (ClusterBrowser.HB_OP_DEMOTE.equals(op)
                            || ClusterBrowser.HB_OP_PROMOTE.equals(op))) {
                        continue;
                    }
                    mOperationsComboBoxHashReadLock.lock();
                    final Widget wi = operationsComboBoxHash.get(op, param);
                    mOperationsComboBoxHashReadLock.unlock();
                    String value;
                    if (wi == null) {
                        value = "0";
                    } else {
                        value = wi.getStringValue();
                    }
                    if (value != null && !"".equals(value)) {
                        if (wi != null && firstTime) {
                            opHash.put("id", opId);
                            opHash.put("name", op);
                            firstTime = false;
                            operations.put(op, opHash);
                        }
                        opHash.put(param, value);
                    }
                }
            }
        }
        return operations;
    }

    /**
     * Returns id of the meta attrs to which meta attrs of this service are
     * referring to.
     */
    protected String getMetaAttrsRefId() {
        String metaAttrsRefId = null;
        if (sameAsMetaAttrsWi != null) {
            final Info i = (Info) sameAsMetaAttrsWi.getValue();
            if (!Widget.NOTHING_SELECTED.equals(i.toString())
                && !META_ATTRS_DEFAULT_VALUES_TEXT.equals(i.toString())) {
                final ServiceInfo si  = (ServiceInfo) i;
                final ClusterStatus cs = getBrowser().getClusterStatus();
                metaAttrsRefId = cs.getMetaAttrsId(
                                            si.getService().getHeartbeatId());
            }
        }
        return metaAttrsRefId;
    }

    /**
     * Returns id of the operations to which operations of this service are
     * referring to.
     */
    protected String getOperationsRefId() {
        String operationsRefId = null;
        if (sameAsOperationsWi != null) {
            final Info i = sameAsOperationsWiValue();
            if (!Widget.NOTHING_SELECTED.equals(i.toString())
                && !OPERATIONS_DEFAULT_VALUES_TEXT.equals(i.toString())) {
                final ServiceInfo si  = (ServiceInfo) i;
                final ClusterStatus cs = getBrowser().getClusterStatus();
                operationsRefId = cs.getOperationsId(
                                            si.getService().getHeartbeatId());
            }
        }
        return operationsRefId;
    }

    /** Returns attributes of this resource. */
    protected Map<String, String> getPacemakerResAttrs(final boolean testOnly) {
        final Map<String, String> pacemakerResAttrs =
                                            new LinkedHashMap<String, String>();
        final String raClass = getService().getResourceClass();
        final String type = getName();
        final String provider = resourceAgent.getProvider();
        final String heartbeatId = getHeartbeatId(testOnly);

        pacemakerResAttrs.put("id", heartbeatId);
        pacemakerResAttrs.put("class", raClass);
        if (!ResourceAgent.HEARTBEAT_CLASS.equals(raClass)
            && !raClass.equals(ResourceAgent.SERVICE_CLASS)
            && !raClass.equals(ResourceAgent.LSB_CLASS)
            && !raClass.equals(ResourceAgent.STONITH_CLASS)) {
            pacemakerResAttrs.put("provider", provider);
        }
        pacemakerResAttrs.put("type", type);
        return pacemakerResAttrs;
    }

    /** Returns arguments of this resource. */
    protected Map<String, String> getPacemakerResArgs() {
        final Map<String, String> pacemakerResArgs =
                                           new LinkedHashMap<String, String>();
        final String[] params = getParametersFromXML();
        for (final String param : params) {
            if (isMetaAttr(param)) {
                continue;
            }
            if (GUI_ID.equals(param)
                || PCMK_ID.equals(param)) {
                continue;
            }
            String value = getComboBoxValue(param);
            if (value == null) {
                value = "";
            }
            if (!resourceAgent.isIgnoreDefaults()
                && value.equals(getParamDefault(param))) {
                continue;
            }
            if (!"".equals(value)) {
                /* for pacemaker */
                pacemakerResArgs.put(param, value);
            }
        }
        return pacemakerResArgs;
    }

    /** Returns meta arguments of this resource. */
    protected Map<String, String> getPacemakerMetaArgs() {
        final Map<String, String> pacemakerMetaArgs =
                                           new LinkedHashMap<String, String>();
        final String[] params = getParametersFromXML();
        for (final String param : params) {
            if (!isMetaAttr(param)) {
                continue;
            }
            if (GUI_ID.equals(param)
                || PCMK_ID.equals(param)) {
                continue;
            }
            String value = getComboBoxValue(param);
            if (value == null) {
                value = "";
            }
            if (value.equals(getParamDefault(param))) {
                continue;
            }
            if (!"".equals(value)) {
                /* for pacemaker */
                pacemakerMetaArgs.put(param, value);
            }
        }
        return pacemakerMetaArgs;
    }

    /** Revert all values. */
    @Override
    public void revert() {
        final CRMXML crmXML = getBrowser().getCRMXML();
        final String[] params = getParametersFromXML();
        boolean allSavedMetaAttrsAreDefaultValues = true;
        boolean sameAs = false;
        if (sameAsMetaAttrsWi != null) {
            for (String param : params) {
                if (isMetaAttr(param)) {
                    final String defaultValue = getParamDefault(param);
                    final String oldValue = getResource().getValue(param);
                    if (!Tools.areEqual(defaultValue, oldValue)) {
                        allSavedMetaAttrsAreDefaultValues = false;
                    }
                }
            }
            if (savedMetaAttrInfoRef == null) {
                if (allSavedMetaAttrsAreDefaultValues) {
                    sameAsMetaAttrsWi.setValue(
                                META_ATTRS_DEFAULT_VALUES_TEXT);
                } else {
                    sameAsMetaAttrsWi.setValue(Widget.NOTHING_SELECTED);
                }
            } else {
                sameAs = true;
                sameAsMetaAttrsWi.setValue(savedMetaAttrInfoRef);
            }
        }
        for (String param : params) {
            if (!sameAs || !isMetaAttr(param)) {
                final String v = getParamSaved(param);
                final Widget wi = getWidget(param, null);
                if (wi != null
                    && !Tools.areEqual(wi.getStringValue(), v)) {
                    if ("".equals(v)) {
                        wi.setValue(null);
                    } else {
                        wi.setValue(v);
                    }
                }
            }
        }
        final GroupInfo gInfo = groupInfo;
        CloneInfo ci;
        if (gInfo == null) {
            ci = getCloneInfo();
        } else {
            ci = gInfo.getCloneInfo();
        }
        final CloneInfo clInfo = ci;
        if (clInfo != null) {
            clInfo.revert();
        }
        revertOperations();
        revertLocations();
    }

    /** Revert locations to saved values. */
    protected final void revertLocations() {
        for (Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation savedLocation = savedHostLocations.get(hi);
            final Widget wi = scoreComboBoxHash.get(hi);
            if (wi == null) {
                continue;
            }

            String score = null;
            String op = null;
            if (savedLocation != null) {
                score = savedLocation.getScore();
                op = savedLocation.getOperation();
            }
            wi.setValue(score);
            final JLabel label = wi.getLabel();
            final String text = getHostLocationLabel(hi.getName(), op);
            label.setText(text);
        }
        /* pingd */
        final Widget pwi = pingComboBox;
        if (pwi != null) {
            final String spo = savedPingOperation;
            if (spo == null) {
                pwi.setValue(Widget.NOTHING_SELECTED);
            } else {
                pwi.setValue(PING_ATTRIBUTES.get(spo));
            }
        }
    }

    /** Revert to saved operation values. */
    protected final void revertOperations() {
        if (sameAsOperationsWi == null) {
            return;
        }
        final ClusterStatus cs = getBrowser().getClusterStatus();
        mSavedOperationsLock.lock();
        boolean allAreDefaultValues = true;
        boolean allSavedAreDefaultValues = true;
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param
                          : getBrowser().getCRMOperationParams(op)) {
                String defaultValue =
                              resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null) {
                    continue;
                }
                if (ClusterBrowser.HB_OP_IGNORE_DEFAULT.contains(op)) {
                    defaultValue = "";
                }
                mOperationsComboBoxHashReadLock.lock();
                final Widget wi = operationsComboBoxHash.get(op, param);
                mOperationsComboBoxHashReadLock.unlock();
                String value = wi.getStringValue();
                if (value == null || "".equals(value)) {
                    value = getOpDefaultsDefault(param);
                }
                if (value == null) {
                    value = "";
                }
                if (!defaultValue.equals(value)) {
                    allAreDefaultValues = false;
                }
                if (!defaultValue.equals(savedOperation.get(op, param))) {
                    allSavedAreDefaultValues = false;
                }
            }
        }
        boolean sameAs = false;
        final ServiceInfo savedOpIdRef = savedOperationIdRef;
        ServiceInfo operationIdRef = null;
        final Info ref = sameAsOperationsWiValue();
        if (ref instanceof ServiceInfo) {
            operationIdRef = (ServiceInfo) ref;
        }
        if (!Tools.areEqual(operationIdRef, savedOpIdRef)) {
            if (savedOpIdRef == null) {
                if (allSavedAreDefaultValues) {
                    sameAsOperationsWi.setValue(
                               OPERATIONS_DEFAULT_VALUES_TEXT);
                } else {
                    if (operationIdRef != null) {
                        sameAsOperationsWi.setValue(Widget.NOTHING_SELECTED);
                    }
                }
            } else {
                sameAs = true;
                sameAsOperationsWi.setValue(savedOpIdRef);
            }
        }
        if (!sameAs) {
            for (final String op : getResourceAgent().getOperationNames()) {
                for (final String param
                              : getBrowser().getCRMOperationParams(op)) {
                    final String value = savedOperation.get(op, param);
                    mOperationsComboBoxHashReadLock.lock();
                    final Widget wi = operationsComboBoxHash.get(op, param);
                    mOperationsComboBoxHashReadLock.unlock();
                    if (wi != null) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                wi.setEnabled(savedOpIdRef == null);
                            }
                        });
                        if (value != null) {
                            wi.setValue(value);
                        }
                    }
                }
            }
        }
        mSavedOperationsLock.unlock();
    }

    /** Applies the changes to the service parameters. */
    void apply(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                }
            });
        }
        getInfoPanel();
        waitForInfoPanel();
        /* TODO: make progress indicator per resource. */
        if (!testOnly) {
            setUpdated(true);
        }
        final String[] params = getParametersFromXML();
        String cloneId = null;
        String[] cloneParams = null;
        boolean master = false;
        final GroupInfo gInfo = groupInfo;
        CloneInfo ci;
        String[] groupParams = null;
        if (gInfo == null) {
            ci = getCloneInfo();
        } else {
            ci = gInfo.getCloneInfo();
            groupParams = gInfo.getParametersFromXML();
        }
        final CloneInfo clInfo = ci;
        if (clInfo != null) {
            cloneId = clInfo.getHeartbeatId(testOnly);
            cloneParams = clInfo.getParametersFromXML();
            master = clInfo.getService().isMaster();

        }
        if (!testOnly) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setToolTipText(null);
                    getWidget(GUI_ID, null).setEnabled(false);
                    if (clInfo != null) {
                        clInfo.getWidget(GUI_ID, null).setEnabled(false);
                    }
                }
            });

            /* add myself to the hash with service name and id as
             * keys */
            getBrowser().removeFromServiceInfoHash(this);
            final String oldHeartbeatId = getHeartbeatId(testOnly);
            if (oldHeartbeatId != null) {
                getBrowser().mHeartbeatIdToServiceLock();
                getBrowser().getHeartbeatIdToServiceInfo().remove(
                                                               oldHeartbeatId);
                getBrowser().mHeartbeatIdToServiceUnlock();
            }
            if (getService().isNew()) {
                final String id = getComboBoxValue(GUI_ID);
                getService().setIdAndCrmId(id);
                if (clInfo != null) {
                    final String clid = clInfo.getComboBoxValue(GUI_ID);
                    clInfo.getService().setIdAndCrmId(clid);
                }
                if (typeRadioGroup != null) {
                    typeRadioGroup.setEnabled(false);
                }
            }
            getBrowser().addNameToServiceInfoHash(this);
            getBrowser().addToHeartbeatIdList(this);
        }
        if (!testOnly) {
            addResourceBefore(dcHost, testOnly);
        }

        final Map<String, String> cloneMetaArgs =
                                            new LinkedHashMap<String, String>();
        final Map<String, String> groupMetaArgs =
                                            new LinkedHashMap<String, String>();
        final Map<String, String> pacemakerResAttrs =
                                                getPacemakerResAttrs(testOnly);
        final Map<String, String> pacemakerResArgs = getPacemakerResArgs();
        final Map<String, String> pacemakerMetaArgs = getPacemakerMetaArgs();
        final String raClass = getService().getResourceClass();
        final String type = getName();
        final String provider = resourceAgent.getProvider();
        final String heartbeatId = getHeartbeatId(testOnly);

        String groupId = null; /* for pacemaker */
        if (gInfo != null) {
            if (gInfo.getService().isNew()) {
                gInfo.apply(dcHost, testOnly);
                return;
            }
            groupId = gInfo.getHeartbeatId(testOnly);
        }
        String cloneMetaAttrsRefIds = null;
        if (clInfo != null) {
            cloneMetaAttrsRefIds = clInfo.getMetaAttrsRefId();
        }
        String groupMetaAttrsRefIds = null;
        if (gInfo != null) {
            groupMetaAttrsRefIds = gInfo.getMetaAttrsRefId();
        }

        final String refCRMId = getOperationsRefId();
        savedOperationsId = refCRMId;
        savedOperationIdRef = getBrowser().getServiceInfoFromCRMId(refCRMId);
        final Info i = sameAsOperationsWiValue();
        if (i == null || (i instanceof StringInfo)) {
            savedOperationsId = null;
        } else {
            savedOperationIdRef = (ServiceInfo) i;
            savedOperationsId = ((ServiceInfo) i).getService().getHeartbeatId();
        }
        if (getService().isNew()) {
            if (clInfo != null) {
                for (String param : cloneParams) {
                    if (GUI_ID.equals(param)
                        || PCMK_ID.equals(param)) {
                        continue;
                    }
                    final String value = clInfo.getComboBoxValue(param);
                    if (value.equals(clInfo.getParamDefault(param))) {
                            continue;
                    }
                    if (!GUI_ID.equals(param) && !"".equals(value)) {
                        cloneMetaArgs.put(param, value);
                    }
                }
            }
            if (gInfo != null) {
                for (String param : groupParams) {
                    if (GUI_ID.equals(param)
                        || PCMK_ID.equals(param)) {
                        continue;
                    }
                    final String value = gInfo.getComboBoxValue(param);
                    if (value.equals(gInfo.getParamDefault(param))) {
                            continue;
                    }
                    if (!GUI_ID.equals(param) && !"".equals(value)) {
                        groupMetaArgs.put(param, value);
                    }
                }
            }
            String command = "-C";
            if ((gInfo != null && !gInfo.getService().isNew())
                || (clInfo != null && !clInfo.getService().isNew())) {
                command = "-U";
            }
            if (!testOnly) {
                getService().setNew(false);
                if (clInfo != null) {
                    clInfo.getService().setNew(false);
                }
            }
            CRM.setParameters(dcHost,
                              command,
                              heartbeatId,
                              cloneId,
                              master,
                              cloneMetaArgs,
                              groupMetaArgs,
                              groupId,
                              pacemakerResAttrs,
                              pacemakerResArgs,
                              pacemakerMetaArgs,
                              null,
                              null,
                              getOperations(heartbeatId),
                              null,
                              getMetaAttrsRefId(),
                              cloneMetaAttrsRefIds,
                              groupMetaAttrsRefIds,
                              refCRMId,
                              resourceAgent.isStonith(),
                              testOnly);
            if (gInfo == null) {
                String hbId = heartbeatId;
                if (clInfo != null) {
                    hbId = clInfo.getHeartbeatId(testOnly);
                }
                final List<Map<String, String>> colAttrsList =
                                       new ArrayList<Map<String, String>>();
                final List<Map<String, String>> ordAttrsList =
                                       new ArrayList<Map<String, String>>();
                final List<String> parentIds = new ArrayList<String>();
                ServiceInfo infoForDependency;
                if (clInfo == null) {
                    infoForDependency = this;
                } else {
                    infoForDependency = clInfo;
                }
                final Set<ServiceInfo> parents =
                          getBrowser().getHeartbeatGraph().getParents(
                                                            infoForDependency);
                for (final ServiceInfo parentInfo : parents) {
                    if (parentInfo.isConstraintPH()) {
                        final boolean colocation = true;
                        final boolean order = true;
                        final Set<ServiceInfo> with =
                                                 new TreeSet<ServiceInfo>();
                        with.add(infoForDependency);
                        final Set<ServiceInfo> withFrom =
                                                 new TreeSet<ServiceInfo>();
                        ((ConstraintPHInfo) parentInfo)
                                    .addConstraintWithPlaceholder(
                                             with,
                                             withFrom,
                                             colocation,
                                             order,
                                             dcHost,
                                             !parentInfo.getService().isNew(),
                                             testOnly);
                    } else {
                        final String parentId =
                                    parentInfo.getService().getHeartbeatId();
                        parentIds.add(parentId);
                        final Map<String, String> colAttrs =
                                           new LinkedHashMap<String, String>();
                        final Map<String, String> ordAttrs =
                                           new LinkedHashMap<String, String>();
                        if (getBrowser().getHeartbeatGraph().isColocation(
                                                        parentInfo,
                                                        infoForDependency)) {
                            colAttrs.put(CRMXML.SCORE_STRING,
                                         CRMXML.INFINITY_STRING);
                            if (parentInfo.getService().isMaster()) {
                                colAttrs.put("with-rsc-role", "Master");
                            }
                            colAttrsList.add(colAttrs);
                        } else {
                            colAttrsList.add(null);
                        }
                        if (getBrowser().getHeartbeatGraph().isOrder(
                                                         parentInfo,
                                                         infoForDependency)) {
                            ordAttrs.put(CRMXML.SCORE_STRING,
                                         CRMXML.INFINITY_STRING);
                            if (parentInfo.getService().isMaster()) {
                                ordAttrs.put("first-action", "promote");
                                ordAttrs.put("then-action", "start");
                            }
                            ordAttrsList.add(ordAttrs);
                        } else {
                            ordAttrsList.add(null);
                        }
                    }
                }
                if (!parentIds.isEmpty()) {
                    CRM.setOrderAndColocation(dcHost,
                                              hbId,
                                              parentIds.toArray(
                                                 new String [parentIds.size()]),
                                              colAttrsList,
                                              ordAttrsList,
                                              testOnly);
                }
            } else {
                gInfo.resetPopup();
            }
        } else {
            if (clInfo != null) {
                for (String param : cloneParams) {
                    if (GUI_ID.equals(param)
                        || PCMK_ID.equals(param)) {
                        continue;
                    }
                    final String value = clInfo.getComboBoxValue(param);
                    if (value.equals(clInfo.getParamDefault(param))) {
                            continue;
                    }
                    if (!"".equals(value)) {
                        cloneMetaArgs.put(param, value);
                    }
                }
            }
            if (gInfo != null) {
                for (String param : groupParams) {
                    if (GUI_ID.equals(param)
                        || PCMK_ID.equals(param)) {
                        continue;
                    }
                    final String value = gInfo.getComboBoxValue(param);
                    if (value == null
                        || value.equals(gInfo.getParamDefault(param))) {
                        continue;
                    }
                    if (!"".equals(value)) {
                        groupMetaArgs.put(param, value);
                    }
                }
                cloneId = null;
            }

            groupId = null; /* we don't want to replace the whole group */
            //TODO: should not be called if only host locations have
            //changed.
            final ClusterStatus cs = getBrowser().getClusterStatus();
            CRM.setParameters(
                        dcHost,
                        "-R",
                        heartbeatId,
                        cloneId,
                        master,
                        cloneMetaArgs,
                        groupMetaArgs,
                        groupId,
                        pacemakerResAttrs,
                        pacemakerResArgs,
                        pacemakerMetaArgs,
                        cs.getResourceInstanceAttrId(heartbeatId),
                        cs.getParametersNvpairsIds(heartbeatId),
                        getOperations(heartbeatId),
                        cs.getOperationsId(heartbeatId),
                        getMetaAttrsRefId(),
                        cloneMetaAttrsRefIds,
                        groupMetaAttrsRefIds,
                        refCRMId,
                        resourceAgent.isStonith(),
                        testOnly);
            if (isFailed(testOnly)) {
                cleanupResource(dcHost, testOnly);
            }
        }

        if (gInfo == null) {
            if (clInfo == null) {
                setLocations(heartbeatId, dcHost, testOnly);
            } else {
                clInfo.setLocations(heartbeatId, dcHost, testOnly);
            }
        } else {
            setLocations(heartbeatId, dcHost, testOnly);
        }
        if (!testOnly) {
            storeComboBoxValues(params);
            storeOperations();
            if (clInfo != null) {
                clInfo.storeComboBoxValues(cloneParams);
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setApplyButtons(null, params);
                }
            });
            final DefaultMutableTreeNode node = getNode();
            if (node != null) {
                if (ci == null) {
                    getBrowser().reload(node, false);
                } else {
                    getBrowser().reload(ci.getNode(), false);
                    getBrowser().reload(node, false);
                }
                getBrowser().getHeartbeatGraph().repaint();
            }
        }
    }

    /** Removes order(s). */
    public void removeOrder(final ServiceInfo parent,
                            final Host dcHost,
                            final boolean testOnly) {
        if (getService().isNew() || parent.getService().isNew()) {
            return;
        }
        if (!testOnly
            && !getService().isNew() && !parent.getService().isNew()) {
            parent.setUpdated(true);
            setUpdated(true);
        }
        final ClusterStatus clStatus = getBrowser().getClusterStatus();

        String rscId;
        if (isConstraintPH()) {
            rscId = getId();
        } else {
            rscId = getHeartbeatId(testOnly);
        }
        if (isConstraintPH() || parent.isConstraintPH()) {
            ConstraintPHInfo cphi = null;
            if (isConstraintPH()) {
                cphi = (ConstraintPHInfo) this;
            } else {
                cphi = (ConstraintPHInfo) parent;
            }
            final Map<CRMXML.RscSet, Map<String, String>> rscSetsOrdAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
            final CRMXML.RscSetConnectionData rdata =
                                             cphi.getRscSetConnectionDataOrd();
            /** resource set */
            final String ordId = rdata.getConstraintId();
            String idToRemove;
            if (isConstraintPH()) {
                idToRemove = parent.getService().getHeartbeatId();
            } else {
                idToRemove = getService().getHeartbeatId();
            }
            CRMXML.RscSet modifiedRscSet = null;
            final List<CRMXML.RscSet> ordRscSets =
                                               clStatus.getRscSetsOrd(ordId);
            if (ordRscSets != null) {
                for (final CRMXML.RscSet rscSet : ordRscSets) {
                    if (rscSet.equals(rdata.getRscSet1())
                        || rscSet.equals(rdata.getRscSet2())) {
                        final List<String> newRscIds =
                                              new ArrayList<String>();
                        newRscIds.addAll(rscSet.getRscIds());
                        if (newRscIds.remove(idToRemove) && !testOnly) {
                            modifiedRscSet = rscSet;
                        }
                        if (!newRscIds.isEmpty()) {
                            final CRMXML.RscSet newRscSet =
                                    new CRMXML.RscSet(
                                                   rscSet.getId(),
                                                   newRscIds,
                                                   rscSet.getSequential(),
                                                   rscSet.getRequireAll(),
                                                   rscSet.getOrderAction(),
                                                   rscSet.getColocationRole());
                            rscSetsOrdAttrs.put(newRscSet, null);
                        }
                    } else {
                        rscSetsOrdAttrs.put(rscSet, null);
                    }
                }
            }
            if (!testOnly && rscSetsOrdAttrs.isEmpty()) {
                cphi.getRscSetConnectionDataOrd().setConstraintId(null);
            }
            final Map<String, String> attrs =
                                        new LinkedHashMap<String, String>();
            final CRMXML.OrderData od = clStatus.getOrderData(ordId);
            if (od != null) {
                final String score = od.getScore();
                attrs.put(CRMXML.SCORE_STRING, score);
            }
            if (!testOnly) {
                ///* so that it will not be removed */
                cphi.setUpdated(false);
            }
            CRM.setRscSet(dcHost,
                          null,
                          false,
                          ordId,
                          false,
                          null,
                          rscSetsOrdAttrs,
                          attrs,
                          testOnly);
        } else {
            final String rscFirstId = parent.getHeartbeatId(testOnly);
            final List<CRMXML.OrderData> allData =
                                            clStatus.getOrderDatas(rscFirstId);
            if (allData != null) {
                for (final CRMXML.OrderData orderData : allData) {
                    final String orderId = orderData.getId();
                    final String rscThenId = orderData.getRscThen();
                    if (rscThenId.equals(getHeartbeatId(testOnly))) {
                        CRM.removeOrder(dcHost,
                                        orderId,
                                        testOnly);
                    }
                }
            }
        }
    }

    /** Returns pacemaker id. */
    final String getHeartbeatId(final boolean testOnly) {
        String heartbeatId = getService().getHeartbeatId();
        if (testOnly && heartbeatId == null) {
            heartbeatId = getService().getCrmIdFromId(getComboBoxValue(GUI_ID));
        }
        return heartbeatId;
    }

    /** Adds order constraint from this service to the child. */
    public void addOrder(final ServiceInfo child,
                         final Host dcHost,
                         final boolean testOnly) {
        if (!testOnly
            && !getService().isNew() && !child.getService().isNew()) {
            child.setUpdated(true);
            setUpdated(true);
        }
        if (isConstraintPH() || child.isConstraintPH()) {
            if (!testOnly) {
                if (isConstraintPH()
                    && ((ConstraintPHInfo) this).isReversedCol()) {
                    ((ConstraintPHInfo) this).reverseOrder();
                } else if (child.isConstraintPH()
                           && ((ConstraintPHInfo) child).isReversedCol()) {
                    ((ConstraintPHInfo) child).reverseOrder();
                }
            }
            final ConstraintPHInfo cphi;
            final ServiceInfo withService;
            final Set<ServiceInfo> withFrom = new TreeSet<ServiceInfo>();
            if (isConstraintPH()) {
                cphi = (ConstraintPHInfo) this;
                withService = child;
            } else {
                cphi = (ConstraintPHInfo) child;
                withService = this;
                withFrom.add(this);
            }
            final Set<ServiceInfo> with = new TreeSet<ServiceInfo>();
            with.add(withService);
            cphi.addConstraintWithPlaceholder(with,
                                              withFrom,
                                              false,
                                              true,
                                              dcHost,
                                              !cphi.getService().isNew(),
                                              testOnly);
        } else {
            final String childHbId = child.getHeartbeatId(testOnly);
            final Map<String, String> attrs =
                                          new LinkedHashMap<String, String>();
            attrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
            final CloneInfo chCI = child.getCloneInfo();
            if (chCI != null
                && chCI.getService().isMaster()) {
                attrs.put("first-action", "promote");
                attrs.put("then-action", "start");
            }
            CRM.addOrder(dcHost,
                         null, /* order id */
                         getHeartbeatId(testOnly),
                         childHbId,
                         attrs,
                         testOnly);
        }
    }

    /** Removes colocation(s). */
    public void removeColocation(final ServiceInfo parent,
                                 final Host dcHost,
                                 final boolean testOnly) {
        if (getService().isNew() || parent.getService().isNew()) {
            return;
        }
        if (!testOnly
            && !getService().isNew() && !parent.getService().isNew()) {
            parent.setUpdated(true);
            setUpdated(true);
        }
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        String rscId;
        if (isConstraintPH()) {
            rscId = getId();
        } else {
            rscId = getHeartbeatId(testOnly);
        }
        if (isConstraintPH() || parent.isConstraintPH()) {
            final Map<CRMXML.RscSet, Map<String, String>> rscSetsColAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
            ConstraintPHInfo cphi = null;
            if (isConstraintPH()) {
                cphi = (ConstraintPHInfo) this;
            } else {
                cphi = (ConstraintPHInfo) parent;
            }
            final CRMXML.RscSetConnectionData rdata =
                                             cphi.getRscSetConnectionDataCol();
            /** resource set */
            final String colId = rdata.getConstraintId();
            String idToRemove;
            if (isConstraintPH()) {
                idToRemove = parent.getService().getHeartbeatId();
            } else {
                idToRemove = getService().getHeartbeatId();
            }
            CRMXML.RscSet modifiedRscSet = null;
            final List<CRMXML.RscSet> colRscSets =
                                               clStatus.getRscSetsCol(colId);
            if (colRscSets != null) {
                for (final CRMXML.RscSet rscSet : colRscSets) {
                    if (rscSet.equals(rdata.getRscSet1())
                        || rscSet.equals(rdata.getRscSet2())) {
                        final List<String> newRscIds =
                                              new ArrayList<String>();
                        newRscIds.addAll(rscSet.getRscIds());
                        if (newRscIds.remove(idToRemove) && !testOnly) {
                            modifiedRscSet = rscSet;
                        }
                        if (!newRscIds.isEmpty()) {
                            final CRMXML.RscSet newRscSet =
                                    new CRMXML.RscSet(
                                                   rscSet.getId(),
                                                   newRscIds,
                                                   rscSet.getSequential(),
                                                   rscSet.getRequireAll(),
                                                   rscSet.getOrderAction(),
                                                   rscSet.getColocationRole());
                            rscSetsColAttrs.put(newRscSet, null);
                        }
                    } else {
                        rscSetsColAttrs.put(rscSet, null);
                    }
                }
            }
            if (!testOnly && rscSetsColAttrs.isEmpty()) {
                cphi.getRscSetConnectionDataCol().setConstraintId(null);
            }
            final Map<String, String> attrs =
                                        new LinkedHashMap<String, String>();
            final CRMXML.ColocationData cd = clStatus.getColocationData(colId);
            if (cd != null) {
                final String score = cd.getScore();
                attrs.put(CRMXML.SCORE_STRING, score);
            }
            if (!testOnly) {
                cphi.setUpdated(false);
            }
            CRM.setRscSet(dcHost,
                          colId,
                          false,
                          null,
                          false,
                          rscSetsColAttrs,
                          null,
                          attrs,
                          testOnly);
        } else {
            final List<CRMXML.ColocationData> allData =
                                            clStatus.getColocationDatas(rscId);
            if (allData != null) {
                for (final CRMXML.ColocationData colocationData
                                                       : allData) {
                    final String colId = colocationData.getId();
                    final String withRscId =
                                       colocationData.getWithRsc();
                    if (withRscId.equals(
                                parent.getHeartbeatId(testOnly))) {
                        CRM.removeColocation(dcHost,
                                             colId,
                                             testOnly);
                    }
                }
            }
        }
    }

    /**
     * Adds colocation constraint from this service to the child. The
     * child - child order is here important, in case colocation
     * constraint is used along with order constraint.
     */
    public void addColocation(final ServiceInfo child,
                              final Host dcHost,
                              final boolean testOnly) {
        if (!testOnly
            && !getService().isNew() && !child.getService().isNew()) {
            child.setUpdated(true);
            setUpdated(true);
        }
        if (isConstraintPH() || child.isConstraintPH()) {
            if (!testOnly) {
                if (isConstraintPH()
                    && ((ConstraintPHInfo) this).isReversedOrd()) {
                    ((ConstraintPHInfo) this).reverseColocation();
                } else if (child.isConstraintPH()
                           && ((ConstraintPHInfo) child).isReversedOrd()) {
                    ((ConstraintPHInfo) child).reverseColocation();
                }
            }
            final ConstraintPHInfo cphi;
            final ServiceInfo withService;
            final Set<ServiceInfo> withFrom = new TreeSet<ServiceInfo>();
            if (isConstraintPH()) {
                cphi = (ConstraintPHInfo) this;
                withService = child;
            } else {
                cphi = (ConstraintPHInfo) child;
                withService = this;
                withFrom.add(this);
            }
            final Set<ServiceInfo> with = new TreeSet<ServiceInfo>();
            with.add(withService);
            cphi.addConstraintWithPlaceholder(with,
                                              withFrom,
                                              true,
                                              false,
                                              dcHost,
                                              !cphi.getService().isNew(),
                                              testOnly);
        } else {
            final String childHbId = child.getHeartbeatId(testOnly);
            final Map<String, String> attrs =
                                        new LinkedHashMap<String, String>();
            attrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
            final CloneInfo pCI = child.getCloneInfo();
            if (pCI != null
                && pCI.getService().isMaster()) {
                attrs.put("with-rsc-role", "Master");
            }
            CRM.addColocation(dcHost,
                              null, /* col id */
                              childHbId,
                              getHeartbeatId(testOnly),
                              attrs,
                              testOnly);
        }
    }

    /** Returns panel with graph. */
    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getHeartbeatGraph().getGraphPanel();
    }

    /** Adds service panel to the position 'pos'. */
    public ServiceInfo addServicePanel(final ResourceAgent newRA,
                                       final Point2D pos,
                                       final boolean colocation,
                                       final boolean order,
                                       final boolean reloadNode,
                                       final boolean master,
                                       final boolean testOnly) {
        ServiceInfo newServiceInfo;

        final String name = newRA.getName();
        if (newRA.isFilesystem()) {
            newServiceInfo = new FilesystemInfo(name, newRA, getBrowser());
        } else if (newRA.isLinbitDrbd()) {
            newServiceInfo = new LinbitDrbdInfo(name, newRA, getBrowser());
        } else if (newRA.isDrbddisk()) {
            newServiceInfo = new DrbddiskInfo(name, newRA, getBrowser());
        } else if (newRA.isIPaddr()) {
            newServiceInfo = new IPaddrInfo(name, newRA, getBrowser());
        } else if (newRA.isVirtualDomain()) {
            newServiceInfo = new VirtualDomainInfo(name, newRA, getBrowser());
        } else if (newRA.isGroup()) {
            newServiceInfo = new GroupInfo(newRA, getBrowser());
        } else if (newRA.isClone()) {
            String cloneName;
            if (master) {
                cloneName = ConfigData.PM_MASTER_SLAVE_SET_NAME;
            } else {
                cloneName = ConfigData.PM_CLONE_SET_NAME;
            }
            newServiceInfo = new CloneInfo(newRA,
                                           cloneName,
                                           master,
                                           getBrowser());
        } else {
            newServiceInfo = new ServiceInfo(name, newRA, getBrowser());
        }

        addServicePanel(newServiceInfo,
                        pos,
                        colocation,
                        order,
                        reloadNode,
                        getBrowser().getDCHost(),
                        testOnly);
        return newServiceInfo;
    }

    /**
     * Adds service panel to the position 'pos'.
     * TODO: is it used?
     */
    public void addServicePanel(final ServiceInfo serviceInfo,
                                final Point2D pos,
                                final boolean colocation,
                                final boolean order,
                                final boolean reloadNode,
                                final Host dcHost,
                                final boolean testOnly) {
        final ResourceAgent ra = serviceInfo.getResourceAgent();
        if (ra != null) {
            serviceInfo.getService().setResourceClass(ra.getResourceClass());
        }
        if (getBrowser().getHeartbeatGraph().addResource(serviceInfo,
                                                         this,
                                                         pos,
                                                         colocation,
                                                         order,
                                                         testOnly)) {
            Tools.waitForSwing();
            /* edge added */
            if (isConstraintPH() || serviceInfo.isConstraintPH()) {
                final ConstraintPHInfo cphi;
                final ServiceInfo withService;
                final Set<ServiceInfo> withFrom = new TreeSet<ServiceInfo>();
                if (isConstraintPH()) {
                    cphi = (ConstraintPHInfo) this;
                    withService = serviceInfo;
                } else {
                    cphi = (ConstraintPHInfo) serviceInfo;
                    withService = this;
                    withFrom.add(this);
                }
                withFrom.addAll(
                            getBrowser().getHeartbeatGraph().getParents(cphi));
                final Set<ServiceInfo> with = new TreeSet<ServiceInfo>();
                with.add(withService);
                cphi.addConstraintWithPlaceholder(with,
                                                  withFrom,
                                                  colocation,
                                                  order,
                                                  dcHost,
                                                  !cphi.getService().isNew(),
                                                  testOnly);
                if (!testOnly) {
                    final PcmkRscSetsInfo prsi = cphi.getPcmkRscSetsInfo();
                    prsi.setApplyButtons(null, prsi.getParametersFromXML());
                }
            } else {
                final String parentId = getHeartbeatId(testOnly);
                final String heartbeatId = serviceInfo.getHeartbeatId(testOnly);
                final List<Map<String, String>> colAttrsList =
                                          new ArrayList<Map<String, String>>();
                final List<Map<String, String>> ordAttrsList =
                                          new ArrayList<Map<String, String>>();
                final Map<String, String> colAttrs =
                                           new LinkedHashMap<String, String>();
                final Map<String, String> ordAttrs =
                                           new LinkedHashMap<String, String>();
                colAttrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
                ordAttrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
                if (getService().isMaster()) {
                    colAttrs.put("with-rsc-role", "Master");
                    ordAttrs.put("first-action", "promote");
                    ordAttrs.put("then-action", "start");
                }
                if (colocation) {
                    colAttrsList.add(colAttrs);
                } else {
                    colAttrsList.add(null);
                }
                if (order) {
                    ordAttrsList.add(ordAttrs);
                } else {
                    ordAttrsList.add(null);
                }
                if (!getService().isNew()
                    && !serviceInfo.getService().isNew()) {
                    CRM.setOrderAndColocation(dcHost,
                                              heartbeatId,
                                              new String[]{parentId},
                                              colAttrsList,
                                              ordAttrsList,
                                              testOnly);
                }
            }
        } else {
            getBrowser().addNameToServiceInfoHash(serviceInfo);
            final DefaultMutableTreeNode newServiceNode =
                                    new DefaultMutableTreeNode(serviceInfo);
            serviceInfo.setNode(newServiceNode);

            getBrowser().getServicesNode().add(newServiceNode);
            if (reloadNode) {
                getBrowser().reload(getBrowser().getServicesNode(), false);
                getBrowser().reload(newServiceNode, false);
            }
            getBrowser().reloadAllComboBoxes(serviceInfo);
        }
        if (reloadNode && ra != null && serviceInfo.getResource().isNew()) {
            if (ra.isProbablyMasterSlave()) {
                serviceInfo.changeType(MASTER_SLAVE_TYPE_STRING);
            } else if (ra.isProbablyClone()) {
                serviceInfo.changeType(CLONE_TYPE_STRING);
            }
        }
        getBrowser().getHeartbeatGraph().reloadServiceMenus();
        if (reloadNode) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getBrowser().getHeartbeatGraph().scale();
                }
            });
        }
    }

    /** Returns service that belongs to this info object. */
    public Service getService() {
        return (Service) getResource();
    }

    /** Starts resource in crm. */
    void startResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.startResource(dcHost, getHeartbeatId(testOnly), testOnly);
    }

    /** Stops resource in crm. */
    void stopResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.stopResource(dcHost, getHeartbeatId(testOnly), testOnly);
    }

    /** Puts a resource up in a group. */
    void upResource(final Host dcHost, final boolean testOnly) {
        final GroupInfo gi = groupInfo;
        final DefaultMutableTreeNode giNode = gi.getNode();
        if (giNode == null) {
            return;
        }
        final DefaultMutableTreeNode node = getNode();
        if (node == null) {
            return;
        }
        final int index = giNode.getIndex(node);
        if (index > 0) {
            final Enumeration e = giNode.children();
            final List<String> newOrder = new ArrayList<String>();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode n =
                                     (DefaultMutableTreeNode) e.nextElement();
                final ServiceInfo child = (ServiceInfo) n.getUserObject();
                newOrder.add(child.getHeartbeatId(testOnly));
            }
            final String el = newOrder.remove(index);
            newOrder.add(index - 1,  el);
            if (!testOnly) {
                setUpdated(true);
            }
            gi.applyWhole(dcHost, false, newOrder, testOnly);
        }
    }

    /** Puts a resource down in a group. */
    void downResource(final Host dcHost, final boolean testOnly) {
        final GroupInfo gi = groupInfo;
        final DefaultMutableTreeNode giNode = gi.getNode();
        if (giNode == null) {
            return;
        }
        final DefaultMutableTreeNode node = getNode();
        if (node == null) {
            return;
        }
        final int index = giNode.getIndex(node);
        if (index < giNode.getChildCount() - 1) {
            final Enumeration e = giNode.children();
            final List<String> newOrder = new ArrayList<String>();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode n =
                                     (DefaultMutableTreeNode) e.nextElement();
                final ServiceInfo child = (ServiceInfo) n.getUserObject();
                newOrder.add(child.getHeartbeatId(testOnly));
            }
            final String el = newOrder.remove(index);
            newOrder.add(index + 1,  el);
            if (!testOnly) {
                setUpdated(true);
            }
            gi.applyWhole(dcHost, false, newOrder, testOnly);
        }
    }

    /** Migrates resource in cluster from current location. */
    void migrateResource(final String onHost,
                         final Host dcHost,
                         final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.migrateResource(dcHost,
                            getHeartbeatId(testOnly),
                            onHost,
                            testOnly);
    }

    /** Migrates resource in heartbeat from current location. */
    void migrateFromResource(final Host dcHost,
                             final String fromHost,
                             final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        /* don't need fromHost, but m/s resources need it. */
        CRM.migrateFromResource(dcHost,
                                getHeartbeatId(testOnly),
                                testOnly);
    }

    /**
     * Migrates resource in cluster from current location with --force option.
     */
    void forceMigrateResource(final String onHost,
                              final Host dcHost,
                              final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.forceMigrateResource(dcHost,
                                 getHeartbeatId(testOnly),
                                 onHost,
                                 testOnly);
    }

    /** Removes constraints created by resource migrate command. */
    void unmigrateResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        CRM.unmigrateResource(dcHost, getHeartbeatId(testOnly), testOnly);
    }

    /** Cleans up the resource. */
    void cleanupResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final String rscId = getHeartbeatId(testOnly);
        boolean failedClone = false;
        for (final Host host : getBrowser().getClusterHosts()) {
            final Set<String> failedClones =
                       cs.getFailedClones(host.getName(), rscId, testOnly);
            if (failedClones == null) {
                continue;
            }
            failedClone = true;
            for (final String fc : failedClones) {
                CRM.cleanupResource(dcHost,
                                    rscId + ":" + fc,
                                    new Host[]{host},
                                    testOnly);
            }
        }
        if (!failedClone) {
            final List<Host> dirtyHosts = new ArrayList<Host>();
            for (final Host host : getBrowser().getClusterHosts()) {
                if (isInLRMOnHost(host.getName(), testOnly)
                    || getFailCount(host.getName(), testOnly) != null) {
                    dirtyHosts.add(host);
                }
            }
            if (!dirtyHosts.isEmpty()) {
                CRM.cleanupResource(
                               dcHost,
                               rscId,
                               dirtyHosts.toArray(new Host[dirtyHosts.size()]),
                               testOnly);
            }
        }
    }

    /** Removes the service without confirmation dialog. */
    protected void removeMyselfNoConfirm(final Host dcHost,
                                         final boolean testOnly) {
        if (!testOnly) {
            if (!getService().isNew()) {
                setUpdated(true);
            }
            getService().setRemoved(true);
            cleanup();
        }
        final CloneInfo ci = getCloneInfo();
        if (ci != null) {
            ci.removeMyselfNoConfirm(dcHost, testOnly);
            setCloneInfo(null);
        }
        final GroupInfo gi = groupInfo;
        if (getService().isNew() && gi == null) {
            if (!testOnly) {
                getService().setNew(false);
                getBrowser().getHeartbeatGraph().killRemovedVertices();
            }
        } else {
            final ClusterStatus cs = getBrowser().getClusterStatus();
            if (gi == null) {
                removeConstraints(dcHost, testOnly);
            }
            if (!getResourceAgent().isGroup()
                && !getResourceAgent().isClone()) {
                String groupId = null; /* for pacemaker */
                if (gi != null) {
                    /* get group id only if there is only one resource in a
                     * group.
                     */
                    if (getService().isNew()) {
                        if (!testOnly) {
                            super.removeMyself(false);
                        }
                    } else {
                        final String group = gi.getHeartbeatId(testOnly);
                        final DefaultMutableTreeNode giNode = gi.getNode();
                        if (giNode != null) {
                            final Enumeration e = giNode.children();
                            while (e.hasMoreElements()) {
                                final DefaultMutableTreeNode n =
                                      (DefaultMutableTreeNode) e.nextElement();
                                final ServiceInfo child =
                                               (ServiceInfo) n.getUserObject();
                                child.getService().setModified(true);
                                child.getService().doneModifying();
                            }
                        }
                        if (cs.getGroupResources(group, testOnly).size() == 1) {
                            if (!testOnly) {
                                gi.getService().setRemoved(true);
                            }
                            gi.removeMyselfNoConfirmFromChild(dcHost, testOnly);
                            groupId = group;
                            gi.getService().doneRemoving();
                        }
                    }
                    gi.resetPopup();
                }
                if (!getService().isNew()) {
                    String cloneId = null;
                    boolean master = false;
                    if (ci != null) {
                        cloneId = ci.getHeartbeatId(testOnly);
                        master = ci.getService().isMaster();
                    }
                    final boolean ret = CRM.removeResource(
                                                      dcHost,
                                                      getHeartbeatId(testOnly),
                                                      groupId,
                                                      cloneId,
                                                      master,
                                                      testOnly);
                    cleanupResource(dcHost, testOnly);
                    setUpdated(false); /* must be here, is not a clone anymore*/
                    if (!testOnly && !ret) {
                        Tools.progressIndicatorFailed(dcHost.getName(),
                                                      "removing failed");
                    }
                }
            }
        }
        if (!testOnly) {
            getBrowser().removeFromServiceInfoHash(this);
            infoPanel = null;
            getService().doneRemoving();
        }
    }

    /** Removes this service from the crm with confirmation dialog. */
    @Override
    public void removeMyself(final boolean testOnly) {
        if (getService().isNew()) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
            getService().setNew(false);
            getService().doneRemoving();
            return;
        }
        String desc = Tools.getString(
                        "ClusterBrowser.confirmRemoveService.Description");

        desc  = desc.replaceAll("@SERVICE@",
                                Matcher.quoteReplacement(toString()));
        if (Tools.confirmDialog(
               Tools.getString("ClusterBrowser.confirmRemoveService.Title"),
               desc,
               Tools.getString("ClusterBrowser.confirmRemoveService.Yes"),
               Tools.getString("ClusterBrowser.confirmRemoveService.No"))) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
            removeInfo();
            getService().setNew(false);
        }
    }

    /** Removes the service from some global hashes and lists. */
    public void removeInfo() {
        getBrowser().mHeartbeatIdToServiceLock();
        getBrowser().getHeartbeatIdToServiceInfo().remove(
                                                getService().getHeartbeatId());
        getBrowser().mHeartbeatIdToServiceUnlock();
        getBrowser().removeFromServiceInfoHash(this);
        removeNode();
        final CloneInfo ci = cloneInfo;
        if (ci != null) {
            ci.removeNode();
        }
        super.removeMyself(false);
    }

    /** Sets this service as part of a group. */
    void setGroupInfo(final GroupInfo groupInfo) {
        this.groupInfo = groupInfo;
    }

    /** Sets this service as part of a clone set. */
    void setCloneInfo(final CloneInfo cloneInfo) {
        this.cloneInfo = cloneInfo;
    }

    /**
     * Returns the group to which this service belongs or null, if it is
     * not in any group.
     */
    public GroupInfo getGroupInfo() {
        return groupInfo;
    }

    /**
     * Returns the clone set to which this service belongs
     * or null, if it is not in such set.
     */
    CloneInfo getCloneInfo() {
        return cloneInfo;
    }

    /** Adds existing service menu item for every member of a group. */
    protected void addExistingGroupServiceMenuItems(
                        final ServiceInfo asi,
                        final MyListModel dlm,
                        final Map<MyMenuItem, ButtonCallback> callbackHash,
                        final MyList list,
                        final JCheckBox colocationWi,
                        final JCheckBox orderWi,
                        final List<JDialog> popups,
                        final boolean testOnly) {
        /* empty */
    }

    /** Adds existing service menu item. */
    protected void addExistingServiceMenuItem(
                        final String name,
                        final ServiceInfo asi,
                        final MyListModel dlm,
                        final Map<MyMenuItem, ButtonCallback> callbackHash,
                        final MyList list,
                        final JCheckBox colocationWi,
                        final JCheckBox orderWi,
                        final List<JDialog> popups,
                        final boolean testOnly) {
        final MyMenuItem mmi = new MyMenuItem(name,
                                              null,
                                              null,
                                              new AccessMode(
                                                   ConfigData.AccessType.ADMIN,
                                                   false),
                                              new AccessMode(
                                                   ConfigData.AccessType.OP,
                                                   false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        hidePopup();
                        for (final JDialog otherP : popups) {
                            otherP.dispose();
                        }
                        addServicePanel(asi,
                                        null,
                                        colocationWi.isSelected(),
                                        orderWi.isSelected(),
                                        true,
                                        getBrowser().getDCHost(),
                                        testOnly);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                repaint();
                            }
                        });
                    }
                });
                thread.start();
            }
        };
        dlm.addElement(mmi);
        final ClusterBrowser.ClMenuItemCallback mmiCallback =
            getBrowser().new ClMenuItemCallback(list, null) {
                           @Override
                           public void action(final Host dcHost) {
                               addServicePanel(asi,
                                               null,
                                               colocationWi.isSelected(),
                                               orderWi.isSelected(),
                                               true,
                                               dcHost,
                                               true); /* test only */
                           }
                       };
        callbackHash.put(mmi, mmiCallback);
    }

    /** Returns existing service manu item. */
    private MyMenu getExistingServiceMenuItem(final String name,
                                              final boolean enableForNew,
                                              final boolean testOnly) {
        final ServiceInfo thisClass = this;
        return new MyMenu(name,
                          new AccessMode(ConfigData.AccessType.ADMIN, false),
                          new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            private final Lock mUpdateLock = new ReentrantLock();

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                } else if (getService().isRemoved()) {
                    return IS_BEING_REMOVED_STRING;
                } else if (getService().isOrphaned()) {
                    return IS_ORPHANED_STRING;
                } else if (!enableForNew && getService().isNew()) {
                    return IS_NEW_STRING;
                }
                if (getBrowser().getExistingServiceList(thisClass).size()
                    == 0) {
                    return "&lt;&lt;empty;&gt;&gt;";
                }
                return null;
            }

            @Override
            public void update() {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (mUpdateLock.tryLock()) {
                            try {
                                updateThread();
                            } finally {
                                mUpdateLock.unlock();
                            }
                        }
                    }
                });
                t.start();
            }

            private void updateThread() {
                final JCheckBox colocationWi = new JCheckBox("Colo", true);
                final JCheckBox orderWi = new JCheckBox("Order", true);
                colocationWi.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                colocationWi.setPreferredSize(colocationWi.getMinimumSize());
                orderWi.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                orderWi.setPreferredSize(orderWi.getMinimumSize());
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setEnabled(false);
                    }
                });
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        removeAll();
                    }
                });

                final MyListModel dlm = new MyListModel();
                final Map<MyMenuItem, ButtonCallback> callbackHash =
                                 new HashMap<MyMenuItem, ButtonCallback>();
                final MyList list = new MyList(dlm, getBackground());

                final List<JDialog> popups = new ArrayList<JDialog>();
                for (final ServiceInfo asi
                            : getBrowser().getExistingServiceList(thisClass)) {
                    if (asi.isConstraintPH() && isConstraintPH()) {
                        continue;
                    }
                    if (asi.getCloneInfo() != null
                        || asi.getGroupInfo() != null) {
                        /* skip services that are clones or in groups. */
                        continue;
                    }
                    addExistingServiceMenuItem(asi.toString(),
                                               asi,
                                               dlm,
                                               callbackHash,
                                               list,
                                               colocationWi,
                                               orderWi,
                                               popups,
                                               testOnly);
                    asi.addExistingGroupServiceMenuItems(thisClass,
                                                         dlm,
                                                         callbackHash,
                                                         list,
                                                         colocationWi,
                                                         orderWi,
                                                         popups,
                                                         testOnly);
                }
                final JPanel colOrdPanel =
                            new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                colOrdPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                colOrdPanel.add(colocationWi);
                colOrdPanel.add(orderWi);
                final boolean ret = Tools.getScrollingMenu(name,
                                                           colOrdPanel,
                                                           this,
                                                           dlm,
                                                           list,
                                                           thisClass,
                                                           popups,
                                                           callbackHash);
                if (!ret) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setEnabled(false);
                        }
                    });
                }
                super.update();
            }
        };
    }

    /** Adds Linbit DRBD RA menu item. It is called in swing thread. */
    private void addDrbdLinbitMenu(final MyMenu menu,
                                   final CRMXML crmXML,
                                   final Point2D pos,
                                   final ResourceAgent fsService,
                                   final boolean testOnly) {
        final MyMenuItem ldMenuItem = new MyMenuItem(
                           Tools.getString("ClusterBrowser.linbitDrbdMenuName"),
                           null,
                           null,
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                hidePopup();
                if (!getBrowser().linbitDrbdConfirmDialog()) {
                    return;
                }

                final FilesystemInfo fsi = (FilesystemInfo)
                                               addServicePanel(
                                                    fsService,
                                                    getPos(),
                                                    true, /* colocation */
                                                    true, /* order */
                                                    true,
                                                    false,
                                                    testOnly);
                fsi.setDrbddiskIsPreferred(false);
                getBrowser().getHeartbeatGraph().repaint();
            }
        };
        if (getBrowser().atLeastOneDrbddisk()
            || !crmXML.isLinbitDrbdPresent()) {
            ldMenuItem.setEnabled(false);
        }
        ldMenuItem.setPos(pos);
        menu.add(ldMenuItem);
    }

    /** Adds drbddisk RA menu item. It is called in swing thread. */
    private void addDrbddiskMenu(final MyMenu menu,
                                 final CRMXML crmXML,
                                 final Point2D pos,
                                 final ResourceAgent fsService,
                                 final boolean testOnly) {
        final ResourceAgent drbddiskService = crmXML.getHbDrbddisk();
        final MyMenuItem ddMenuItem = new MyMenuItem(
                         Tools.getString("ClusterBrowser.DrbddiskMenuName"),
                         null,
                         null,
                         new AccessMode(ConfigData.AccessType.ADMIN, false),
                         new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                hidePopup();
                final FilesystemInfo fsi = (FilesystemInfo) addServicePanel(
                                                    fsService,
                                                    getPos(),
                                                    true, /* colocation */
                                                    true, /* order */
                                                    true,
                                                    false,
                                                    testOnly);
                fsi.setDrbddiskIsPreferred(true);
                getBrowser().getHeartbeatGraph().repaint();
            }
        };
        if (getBrowser().isOneLinbitDrbd()
            || !crmXML.isDrbddiskPresent()) {
            ddMenuItem.setEnabled(false);
        }
        ddMenuItem.setPos(pos);
        menu.add(ddMenuItem);
    }

    /** Adds Ipaddr RA menu item. It is called in swing thread. */
    private void addIpMenu(final MyMenu menu,
                           final Point2D pos,
                           final ResourceAgent ipService,
                           final boolean testOnly) {
        final MyMenuItem ipMenuItem =
          new MyMenuItem(ipService.getMenuName(),
                         null,
                         null,
                         new AccessMode(ConfigData.AccessType.ADMIN, false),
                         new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                hidePopup();
                addServicePanel(ipService,
                                getPos(),
                                true, /* colocation */
                                true, /* order */
                                true,
                                false,
                                testOnly);
                getBrowser().getHeartbeatGraph().repaint();
            }
        };
        ipMenuItem.setPos(pos);
        menu.add(ipMenuItem);
    }

    /** Adds Filesystem RA menu item. It is called in swing thread. */
    private void addFilesystemMenu(final MyMenu menu,
                                   final Point2D pos,
                                   final ResourceAgent fsService,
                                   final boolean testOnly) {
        final MyMenuItem fsMenuItem =
              new MyMenuItem(fsService.getMenuName(),
                             null,
                             null,
                             new AccessMode(ConfigData.AccessType.ADMIN, false),
                             new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;
                @Override
                public void action() {
                    hidePopup();
                    addServicePanel(fsService,
                                    getPos(),
                                    true, /* colocation */
                                    true, /* order */
                                    true,
                                    false,
                                    testOnly);
                    getBrowser().getHeartbeatGraph().repaint();
                }
        };
        fsMenuItem.setPos(pos);
        menu.add(fsMenuItem);
    }

    /** Adds resource agent RA menu item. It is called in swing thread. */
    private void addResourceAgentMenu(final ResourceAgent ra,
                                      final MyListModel dlm,
                                      final Point2D pos,
                                      final List<JDialog> popups,
                                      final JCheckBox colocationWi,
                                      final JCheckBox orderWi,
                                      final boolean testOnly) {
        final MyMenuItem mmi =
               new MyMenuItem(
                     ra.getMenuName(),
                     null,
                     null,
                     new AccessMode(ConfigData.AccessType.ADMIN,
                                    false),
                     new AccessMode(ConfigData.AccessType.OP,
                                    false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                hidePopup();
                for (final JDialog otherP : popups) {
                    otherP.dispose();
                }
                if (ra.isLinbitDrbd()
                    &&
                     !getBrowser().linbitDrbdConfirmDialog()) {
                    return;
                } else if (ra.isHbDrbd()
                     && !getBrowser().hbDrbdConfirmDialog()) {
                    return;
                }
                addServicePanel(ra,
                                getPos(),
                                colocationWi.isSelected(),
                                orderWi.isSelected(),
                                true,
                                false,
                                testOnly);
                getBrowser().getHeartbeatGraph().repaint();
            }
        };
        mmi.setPos(pos);
        dlm.addElement(mmi);
    }

    /** Adds new Service and dependence. */
    private MyMenu getAddServiceMenuItem(final boolean testOnly,
                                         final String name) {
        final ServiceInfo thisClass = this;
        return new MyMenu(name,
                          new AccessMode(ConfigData.AccessType.ADMIN, false),
                          new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            private final Lock mUpdateLock = new ReentrantLock();

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                } else if (getService().isRemoved()) {
                    return IS_BEING_REMOVED_STRING;
                } else if (getService().isOrphaned()) {
                    return IS_ORPHANED_STRING;
                } else if (getService().isNew()) {
                    return IS_NEW_STRING;
                }
                return null;
            }

            @Override
            public void update() {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (mUpdateLock.tryLock()) {
                            try {
                                updateThread();
                            } finally {
                                mUpdateLock.unlock();
                            }
                        }
                    }
                });
                t.start();
            }

            private void updateThread() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                       setEnabled(false);
                    }
                });
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        removeAll();
                    }
                });
                final Point2D pos = getPos();
                final CRMXML crmXML = getBrowser().getCRMXML();
                final ResourceAgent fsService =
                     crmXML.getResourceAgent("Filesystem",
                                             ResourceAgent.HEARTBEAT_PROVIDER,
                                             ResourceAgent.OCF_CLASS);
                final MyMenu thisMenu = this;
                if (crmXML.isLinbitDrbdPresent()) { /* just skip it, if it
                                                       is not */
                    final ResourceAgent linbitDrbdService =
                                                  crmXML.getHbLinbitDrbd();
                    /* Linbit:DRBD */
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                addDrbdLinbitMenu(thisMenu,
                                                  crmXML,
                                                  pos,
                                                  fsService,
                                                  testOnly);
                            }
                        });
                    } catch (final InterruptedException ix) {
                        Thread.currentThread().interrupt();
                    } catch (final InvocationTargetException x) {
                        Tools.printStackTrace();
                    }

                }
                if (crmXML.isDrbddiskPresent()) { /* just skip it,
                                                     if it is not */
                    /* drbddisk */
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                addDrbddiskMenu(thisMenu,
                                                crmXML,
                                                pos,
                                                fsService,
                                                testOnly);
                            }
                        });
                    } catch (final InterruptedException ix) {
                        Thread.currentThread().interrupt();
                    } catch (final InvocationTargetException x) {
                        Tools.printStackTrace();
                    }
                }
                final ResourceAgent ipService = crmXML.getResourceAgent(
                                         "IPaddr2",
                                         ResourceAgent.HEARTBEAT_PROVIDER,
                                         ResourceAgent.OCF_CLASS);
                if (ipService != null) { /* just skip it, if it is not*/
                    /* ipaddr */
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                addIpMenu(thisMenu,
                                          pos,
                                          ipService,
                                          testOnly);
                            }
                        });
                    } catch (final InterruptedException ix) {
                        Thread.currentThread().interrupt();
                    } catch (final InvocationTargetException x) {
                        Tools.printStackTrace();
                    }
                }
                if (fsService != null) { /* just skip it, if it is not*/
                    /* Filesystem */
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                addFilesystemMenu(thisMenu,
                                                  pos,
                                                  fsService,
                                                  testOnly);
                            }
                        });
                    } catch (final InterruptedException ix) {
                        Thread.currentThread().interrupt();
                    } catch (final InvocationTargetException x) {
                        Tools.printStackTrace();
                    }
                }
                final List<JDialog> popups = new ArrayList<JDialog>();
                for (final String cl : ClusterBrowser.HB_CLASSES) {
                    final List<ResourceAgent> services = getAddServiceList(cl);
                    if (services.size() == 0) {
                        /* no services, don't show */
                        continue;
                    }
                    final JCheckBox colocationWi = new JCheckBox("Colo", true);
                    final JCheckBox orderWi = new JCheckBox("Order", true);
                    colocationWi.setBackground(
                                            ClusterBrowser.STATUS_BACKGROUND);
                    colocationWi.setPreferredSize(
                                            colocationWi.getMinimumSize());
                    orderWi.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                    orderWi.setPreferredSize(orderWi.getMinimumSize());
                    final JPanel colOrdPanel =
                            new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                    colOrdPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                    colOrdPanel.add(colocationWi);
                    colOrdPanel.add(orderWi);
                    final MyMenu classItem = new MyMenu(
                            ClusterBrowser.HB_CLASS_MENU.get(cl),
                            new AccessMode(ConfigData.AccessType.ADMIN, false),
                            new AccessMode(ConfigData.AccessType.OP, false));
                    final MyListModel dlm = new MyListModel();
                    for (final ResourceAgent ra : services) {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    addResourceAgentMenu(ra,
                                                         dlm,
                                                         pos,
                                                         popups,
                                                         colocationWi,
                                                         orderWi,
                                                         testOnly);
                                }
                            });
                        } catch (final InterruptedException ix) {
                            Thread.currentThread().interrupt();
                        } catch (final InvocationTargetException x) {
                            Tools.printStackTrace();
                        }
                    }
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                final boolean ret = Tools.getScrollingMenu(
                                        ClusterBrowser.HB_CLASS_MENU.get(cl),
                                        colOrdPanel,
                                        classItem,
                                        dlm,
                                        new MyList(dlm, getBackground()),
                                        thisClass,
                                        popups,
                                        null);
                                if (!ret) {
                                    classItem.setEnabled(false);
                                }
                                thisMenu.add(classItem);
                            }
                        });
                    } catch (final InterruptedException ix) {
                        Thread.currentThread().interrupt();
                    } catch (final InvocationTargetException x) {
                        Tools.printStackTrace();
                    }
                }
                super.update();
            }
        };
    }

    /** Adds menu items with dependend services and groups. */
    protected void addDependencyMenuItems(final List<UpdatableItem> items,
                                          final boolean enableForNew,
                                          final boolean testOnly) {
        /* add new group and dependency*/
        final MyMenuItem addGroupMenuItem =
            new MyMenuItem(Tools.getString(
                                "ClusterBrowser.Hb.AddDependentGroup"),
                           null,
                           null,
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (getService().isRemoved()) {
                        return IS_BEING_REMOVED_STRING;
                    } else if (getService().isOrphaned()) {
                        return IS_ORPHANED_STRING;
                    } else if (getService().isNew()) {
                        return IS_NEW_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    hidePopup();
                    final StringInfo gi = new StringInfo(
                                            ConfigData.PM_GROUP_NAME,
                                            ConfigData.PM_GROUP_NAME,
                                            getBrowser());
                    final CRMXML crmXML = getBrowser().getCRMXML();
                    addServicePanel(crmXML.getHbGroup(),
                                    getPos(),
                                    false, /* colocation only */
                                    false, /* order only */
                                    true,
                                    false,
                                    testOnly);
                    getBrowser().getHeartbeatGraph().repaint();
                }
            };
        items.add((UpdatableItem) addGroupMenuItem);

        /* add new service and dependency*/
        final MyMenu addServiceMenuItem = getAddServiceMenuItem(
                        testOnly,
                        Tools.getString("ClusterBrowser.Hb.AddDependency"));
        items.add((UpdatableItem) addServiceMenuItem);

        /* add existing service dependency*/
        final MyMenu existingServiceMenuItem = getExistingServiceMenuItem(
                    Tools.getString("ClusterBrowser.Hb.AddStartBefore"),
                    enableForNew,
                    testOnly);
        items.add((UpdatableItem) existingServiceMenuItem);
    }

    /**
     * Returns list of items for service popup menu with actions that can
     * be executed on the heartbeat services.
     */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;
        final CloneInfo ci = getCloneInfo();
        if (ci == null) {
            addDependencyMenuItems(items, false, testOnly);
        }
        /* start resource */
        final MyMenuItem startMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.StartResource"),
                           START_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public final String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (isStarted(testOnly)) {
                        return Tools.getString("ServiceInfo.AlreadyStarted");
                    } else {
                        return getService().isAvailableWithText();
                    }
                }

                @Override
                public void action() {
                    hidePopup();
                    startResource(getBrowser().getDCHost(), testOnly);
                }
            };
        final ClusterBrowser.ClMenuItemCallback startItemCallback =
                   getBrowser().new ClMenuItemCallback(startMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                startResource(dcHost, true); /* testOnly */
            }
        };
        addMouseOverListener(startMenuItem, startItemCallback);
        items.add((UpdatableItem) startMenuItem);

        /* stop resource */
        final MyMenuItem stopMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.StopResource"),
                           STOP_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (isStopped(testOnly)) {
                        return Tools.getString("ServiceInfo.AlreadyStopped");
                    } else {
                        return getService().isAvailableWithText();
                    }
                }

                @Override
                public void action() {
                    hidePopup();
                    stopResource(getBrowser().getDCHost(), testOnly);
                }
            };
        final ClusterBrowser.ClMenuItemCallback stopItemCallback =
                    getBrowser().new ClMenuItemCallback(stopMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                stopResource(dcHost, true); /* testOnly */
            }
        };
        addMouseOverListener(stopMenuItem, stopItemCallback);
        items.add((UpdatableItem) stopMenuItem);

        /* up group resource */
        final MyMenuItem upMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.UpResource"),
                           GROUP_UP_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return groupInfo != null;
                }

                @Override
                public String enablePredicate() {
                    if (getResource().isNew()) {
                        return IS_NEW_STRING;
                    }
                    final GroupInfo gi = groupInfo;
                    if (gi == null) {
                        return "no";
                    }
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    final DefaultMutableTreeNode giNode = gi.getNode();
                    if (giNode == null) {
                        return "no";
                    }
                    final DefaultMutableTreeNode node = getNode();
                    if (node == null) {
                        return "no";
                    }
                    final int index = giNode.getIndex(node);
                    if (index == 0) {
                        return "already up";
                    }
                    return null;
                }

                @Override
                public void action() {
                    hidePopup();
                    upResource(getBrowser().getDCHost(), testOnly);
                }
            };
        final ClusterBrowser.ClMenuItemCallback upItemCallback =
                    getBrowser().new ClMenuItemCallback(upMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                upResource(dcHost, true); /* testOnly */
            }
        };
        addMouseOverListener(upMenuItem, upItemCallback);
        items.add((UpdatableItem) upMenuItem);

        /* down group resource */
        final MyMenuItem downMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.DownResource"),
                           GROUP_DOWN_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return groupInfo != null;
                }

                @Override
                public String enablePredicate() {
                    if (getResource().isNew()) {
                        return IS_NEW_STRING;
                    }
                    final GroupInfo gi = groupInfo;
                    if (gi == null) {
                        return "no";
                    }
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    final DefaultMutableTreeNode giNode = gi.getNode();
                    if (giNode == null) {
                        return "no";
                    }
                    final DefaultMutableTreeNode node = getNode();
                    if (node == null) {
                        return "no";
                    }
                    final int index = giNode.getIndex(node);
                    if (index >= giNode.getChildCount() - 1) {
                        return "already down";
                    }
                    return null;
                }

                @Override
                public void action() {
                    hidePopup();
                    downResource(getBrowser().getDCHost(), testOnly);
                }
            };
        final ClusterBrowser.ClMenuItemCallback downItemCallback =
                    getBrowser().new ClMenuItemCallback(downMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                downResource(dcHost, true); /* testOnly */
            }
        };
        addMouseOverListener(downMenuItem, downItemCallback);
        items.add((UpdatableItem) downMenuItem);

        /* clean up resource */
        final MyMenuItem cleanupMenuItem =
            new MyMenuItem(
               Tools.getString("ClusterBrowser.Hb.CleanUpFailedResource"),
               SERVICE_RUNNING_ICON,
               ClusterBrowser.STARTING_PTEST_TOOLTIP,

               Tools.getString("ClusterBrowser.Hb.CleanUpResource"),
               SERVICE_RUNNING_ICON,
               ClusterBrowser.STARTING_PTEST_TOOLTIP,
               new AccessMode(ConfigData.AccessType.OP, false),
               new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return getService().isAvailable()
                           && isOneFailed(testOnly);
                }

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (!isOneFailedCount(testOnly)) {
                        return "no fail count";
                    } else {
                        return getService().isAvailableWithText();
                    }
                }

                @Override
                public void action() {
                    hidePopup();
                    cleanupResource(getBrowser().getDCHost(), testOnly);
                }
            };
        /* cleanup ignores CIB_file */
        items.add((UpdatableItem) cleanupMenuItem);


        /* manage resource */
        final MyMenuItem manageMenuItem =
            new MyMenuItem(
                  Tools.getString("ClusterBrowser.Hb.ManageResource"),
                  MANAGE_BY_CRM_ICON,
                  ClusterBrowser.STARTING_PTEST_TOOLTIP,

                  Tools.getString("ClusterBrowser.Hb.UnmanageResource"),
                  UNMANAGE_BY_CRM_ICON,
                  ClusterBrowser.STARTING_PTEST_TOOLTIP,

                  new AccessMode(ConfigData.AccessType.OP, false),
                  new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return !isManaged(testOnly);
                }
                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else {
                        return getService().isAvailableWithText();
                    }
                }

                @Override
                public void action() {
                    hidePopup();
                    if (this.getText().equals(Tools.getString(
                                    "ClusterBrowser.Hb.ManageResource"))) {
                        setManaged(true, getBrowser().getDCHost(), testOnly);
                    } else {
                        setManaged(false, getBrowser().getDCHost(), testOnly);
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback manageItemCallback =
                  getBrowser().new ClMenuItemCallback(manageMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                setManaged(!isManaged(false),
                           dcHost, true); /* testOnly */
            }
        };
        addMouseOverListener(manageMenuItem, manageItemCallback);
        items.add((UpdatableItem) manageMenuItem);
        addMigrateMenuItems(items);
        if (ci == null) {
            /* remove service */
            final MyMenuItem removeMenuItem = new MyMenuItem(
                        Tools.getString("ClusterBrowser.Hb.RemoveService"),
                        ClusterBrowser.REMOVE_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(ConfigData.AccessType.ADMIN, false),
                        new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getService().isNew()) {
                        return null;
                    }
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (getService().isRemoved()) {
                        return IS_BEING_REMOVED_STRING;
                    } else if (isRunning(testOnly)
                               && !Tools.getConfigData().isAdvancedMode()) {
                        return "cannot remove running resource<br>"
                               + "(advanced mode only)";
                    }
                    if (groupInfo == null) {
                        return null;
                    }
                    final ClusterStatus cs = getBrowser().getClusterStatus();
                    final List<String> gr = cs.getGroupResources(
                                          groupInfo.getHeartbeatId(testOnly),
                                          testOnly);


                    if (gr != null && gr.size() > 1) {
                        return null;
                    } else {
                        return "you can remove the group";
                    }
                }

                @Override
                public void action() {
                    hidePopup();
                    if (getService().isOrphaned()) {
                        cleanupResource(getBrowser().getDCHost(), testOnly);
                    } else {
                        removeMyself(false);
                    }
                    getBrowser().getHeartbeatGraph().repaint();
                }
            };
            final ServiceInfo thisClass = this;
            final ClusterBrowser.ClMenuItemCallback removeItemCallback =
                    getBrowser().new ClMenuItemCallback(removeMenuItem, null) {
                @Override
                public final boolean isEnabled() {
                    return super.isEnabled() && !getService().isNew();
                }
                @Override
                public final void action(final Host dcHost) {
                    removeMyselfNoConfirm(dcHost, true); /* test only */
                }
            };
            addMouseOverListener(removeMenuItem, removeItemCallback);
            items.add((UpdatableItem) removeMenuItem);
        }
        /* view log */
        final MyMenuItem viewLogMenu = new MyMenuItem(
                        Tools.getString("ClusterBrowser.Hb.ViewServiceLog"),
                        LOGFILE_ICON,
                        null,
                        new AccessMode(ConfigData.AccessType.RO, false),
                        new AccessMode(ConfigData.AccessType.RO, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getService().isNew()) {
                    return IS_NEW_STRING;
                } else {
                    return null;
                }
            }

            @Override
            public void action() {
                hidePopup();
                ServiceLogs l = new ServiceLogs(getBrowser().getCluster(),
                                                getNameForLog(),
                                                getService().getHeartbeatId());
                l.showDialog();
            }
        };
        items.add(viewLogMenu);
        /* more migrate options */
        final MyMenu migrateSubmenu = new MyMenu(
                        Tools.getString("ClusterBrowser.MigrateSubmenu"),
                        new AccessMode(ConfigData.AccessType.OP, false),
                        new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public String enablePredicate() {
                return null; //TODO: enable only if it has items
            }
        };
        items.add(migrateSubmenu);
        addMoreMigrateMenuItems(migrateSubmenu);

        /* config files */
        final MyMenu filesSubmenu = new MyMenu(
                        Tools.getString("ClusterBrowser.FilesSubmenu"),
                        new AccessMode(ConfigData.AccessType.ADMIN, false),
                        new AccessMode(ConfigData.AccessType.ADMIN, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public String enablePredicate() {
                return null; //TODO: enable only if it has items
            }
            @Override
            public void update() {
                super.update();
                final MyMenu self = this;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        removeAll();
                        addFilesMenuItems(self);
                    }
                });
            }
        };
        items.add(filesSubmenu);
        return items;
    }

    /** Adds migrate and unmigrate menu items. */
    protected void addMigrateMenuItems(final List<UpdatableItem> items) {
        /* migrate resource */
        final boolean testOnly = false;
        final ServiceInfo thisClass = this;
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateFromMenuItem =
               new MyMenuItem(Tools.getString(
                                   "ClusterBrowser.Hb.MigrateFromResource")
                                   + " " + hostName,
                              MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,

                              Tools.getString(
                                   "ClusterBrowser.Hb.MigrateFromResource")
                                   + " " + hostName + " (offline)",
                              MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean predicate() {
                        return host.isClStatus();
                    }

                    @Override
                    public boolean visiblePredicate() {
                        return !host.isClStatus()
                               || enablePredicate() == null;
                    }

                    @Override
                    public String enablePredicate() {
                        final List<String> runningOnNodes =
                                               getRunningOnNodes(testOnly);
                        if (runningOnNodes == null
                            || runningOnNodes.size() < 1) {
                            return "must run";
                        }
                        boolean runningOnNode = false;
                        for (final String ron : runningOnNodes) {
                            if (hostName.toLowerCase(Locale.US).equals(
                                               ron.toLowerCase(Locale.US))) {
                                runningOnNode = true;
                                break;
                            }
                        }
                        if (!getBrowser().clStatusFailed()
                               && getService().isAvailable()
                               && runningOnNode
                               && host.isClStatus()) {
                            return null;
                        } else {
                            return ""; /* is not visible anyway */
                        }
                    }

                    @Override
                    public void action() {
                        hidePopup();
                        migrateFromResource(getBrowser().getDCHost(),
                                            hostName,
                                            testOnly);
                    }
                };
            final ClusterBrowser.ClMenuItemCallback migrateItemCallback =
               getBrowser().new ClMenuItemCallback(migrateFromMenuItem, null) {
                @Override
                public void action(final Host dcHost) {
                    migrateFromResource(dcHost, hostName, true); /* testOnly */
                }
            };
            addMouseOverListener(migrateFromMenuItem, migrateItemCallback);
            items.add(migrateFromMenuItem);
        }

        /* unmigrate resource */
        final MyMenuItem unmigrateMenuItem =
            new MyMenuItem(
                    Tools.getString("ClusterBrowser.Hb.UnmigrateResource"),
                    UNMIGRATE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    new AccessMode(ConfigData.AccessType.OP, false),
                    new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return enablePredicate() == null;
                }

                @Override
                public String enablePredicate() {
                    // TODO: if it was migrated
                    if (!getBrowser().clStatusFailed()
                           && getService().isAvailable()
                           && (getMigratedTo(testOnly) != null
                               || getMigratedFrom(testOnly) != null)) {
                        return null;
                    } else {
                        return ""; /* it's not visible anyway */
                    }
                }

                @Override
                public void action() {
                    hidePopup();
                    unmigrateResource(getBrowser().getDCHost(), testOnly);
                }
            };
        final ClusterBrowser.ClMenuItemCallback unmigrateItemCallback =
               getBrowser().new ClMenuItemCallback(unmigrateMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                unmigrateResource(dcHost, true); /* testOnly */
            }
        };
        addMouseOverListener(unmigrateMenuItem, unmigrateItemCallback);
        items.add((UpdatableItem) unmigrateMenuItem);
    }

    /** Adds "migrate from" and "force migrate" menuitems to the submenu. */
    protected void addMoreMigrateMenuItems(final MyMenu submenu) {
        final boolean testOnly = false;
        final ServiceInfo thisClass = this;
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateMenuItem =
               new MyMenuItem(Tools.getString(
                                   "ClusterBrowser.Hb.MigrateResource")
                                   + " " + hostName,
                              MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,

                              Tools.getString(
                                   "ClusterBrowser.Hb.MigrateResource")
                                   + " " + hostName + " (offline)",
                              MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean predicate() {
                        return host.isClStatus();
                    }

                    @Override
                    public boolean visiblePredicate() {
                        return !host.isClStatus()
                               || enablePredicate() == null;
                    }

                    @Override
                    public String enablePredicate() {
                        final List<String> runningOnNodes =
                                               getRunningOnNodes(testOnly);
                        if (runningOnNodes == null
                            || runningOnNodes.isEmpty()) {
                            return Tools.getString(
                                            "ServiceInfo.NotRunningAnywhere");
                        }
                        final String runningOnNode =
                                runningOnNodes.get(0).toLowerCase(Locale.US);
                        if (getBrowser().clStatusFailed()
                            || !host.isClStatus()) {
                            return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                        } else {
                            final String tp =
                                            getService().isAvailableWithText();
                            if (tp != null) {
                                return tp;
                            }
                        }
                        if (hostName.toLowerCase(Locale.US).equals(
                                                             runningOnNode)) {
                            return Tools.getString(
                                           "ServiceInfo.AlreadyRunningOnNode");
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public void action() {
                        hidePopup();
                        migrateResource(hostName,
                                        getBrowser().getDCHost(),
                                        testOnly);
                    }
                };
            final ClusterBrowser.ClMenuItemCallback migrateItemCallback =
                 getBrowser().new ClMenuItemCallback(migrateMenuItem, null) {
                @Override
                public void action(final Host dcHost) {
                    migrateResource(hostName, dcHost, true); /* testOnly */
                }
            };
            addMouseOverListener(migrateMenuItem, migrateItemCallback);
            submenu.add(migrateMenuItem);
        }
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();

            final MyMenuItem forceMigrateMenuItem =
               new MyMenuItem(Tools.getString(
                                   "ClusterBrowser.Hb.ForceMigrateResource")
                                   + " " + hostName,
                              MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,

                              Tools.getString(
                                   "ClusterBrowser.Hb.ForceMigrateResource")
                                   + " " + hostName + " (offline)",
                              MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean predicate() {
                        return host.isClStatus();
                    }

                    @Override
                    public boolean visiblePredicate() {
                        return !host.isClStatus()
                               || enablePredicate() == null;
                    }

                    @Override
                    public String enablePredicate() {
                        final List<String> runningOnNodes =
                                               getRunningOnNodes(testOnly);
                        if (runningOnNodes == null
                            || runningOnNodes.isEmpty()) {
                            return Tools.getString(
                                            "ServiceInfo.NotRunningAnywhere");
                        }
                        final String runningOnNode =
                                runningOnNodes.get(0).toLowerCase(Locale.US);
                        if (!getBrowser().clStatusFailed()
                               && getService().isAvailable()
                               && !hostName.toLowerCase(Locale.US).equals(
                                                                 runningOnNode)
                               && host.isClStatus()) {
                            return null;
                        } else {
                            return "";
                        }
                    }

                    @Override
                    public void action() {
                        hidePopup();
                        forceMigrateResource(hostName,
                                             getBrowser().getDCHost(),
                                             testOnly);
                    }
                };
            final ClusterBrowser.ClMenuItemCallback forceMigrateItemCallback =
                 getBrowser().new ClMenuItemCallback(forceMigrateMenuItem,
                                                     null) {
                @Override
                public void action(final Host dcHost) {
                    forceMigrateResource(hostName, dcHost, true); /* testOnly */
                }
            };
            addMouseOverListener(forceMigrateMenuItem,
                                 forceMigrateItemCallback);
            submenu.add(forceMigrateMenuItem);
        }
    }

    /** Return config files defined in DistResource config files. */
    private List<String> getConfigFiles() {
        String raName;
        final ServiceInfo cs = getContainedService();
        if (cs == null) {
            raName = getResourceAgent().getRAString();
        } else {
            raName = cs.getResourceAgent().getRAString();
        }
        final Host[] hosts = getBrowser().getCluster().getHostsArray();
        final List<String> cfs =
             new ArrayList<String>(hosts[0].getDistStrings(raName + ".files"));
        final List<String> params =
            new ArrayList<String>(hosts[0].getDistStrings(raName + ".params"));
        params.add("configfile");
        params.add("config");
        params.add("conffile");
        for (final String param : params) {
            String value;
            if (cs == null) {
                final Widget wi = getWidget(param, null);
                if (wi == null) {
                    value = getParamSaved(param);
                } else {
                    value = wi.getStringValue();
                }
            } else {
                final Widget wi = cs.getWidget(param, null);
                if (wi == null) {
                    value = cs.getParamSaved(param);
                } else {
                    value = wi.getStringValue();
                }
            }
            if (value != null && !"".equals(value)) {
                cfs.add(value);
            }
        }
        return cfs;
    }

    /** Adds config files menuitems to the submenu. */
    protected void addFilesMenuItems(final MyMenu submenu) {
        final boolean testOnly = false;
        final ServiceInfo thisClass = this;
        final List<String> configFiles = getConfigFiles();
        for (final String configFile : configFiles) {
            final MyMenuItem fileItem =
               new MyMenuItem(
                          configFile,
                          null,
                          null,
                          new AccessMode(ConfigData.AccessType.ADMIN, false),
                          new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean predicate() {
                        return true;
                    }

                    @Override
                    public boolean visiblePredicate() {
                        return true;
                    }

                    @Override
                    public String enablePredicate() {
                        return null;
                    }

                    @Override
                    public void action() {
                        final EditConfig ed =
                          new EditConfig(configFile,
                                         getBrowser().getCluster().getHosts());
                        ed.showDialog();

                    }
                };
            submenu.add(fileItem);
        }
    }

    /** Returns tool tip for the service. */
    @Override
    public String getToolTipText(final boolean testOnly) {
        String nodeString = null;
        final List<String> nodes = getRunningOnNodes(testOnly);
        if (nodes != null && !nodes.isEmpty()) {
            nodeString =
                     Tools.join(", ", nodes.toArray(new String[nodes.size()]));
        }
        final Host[] hosts = getBrowser().getCluster().getHostsArray();
        if (getBrowser().allHostsDown()) {
            nodeString = "unknown";
        }
        final StringBuilder sb = new StringBuilder(200);
        sb.append("<b>");
        sb.append(toString());
        String textOn;
        String textNotOn;
        if (getResourceAgent().isFilesystem()) {
            textOn = Tools.getString("ServiceInfo.Filesystem.MoutedOn");
            textNotOn = Tools.getString("ServiceInfo.Filesystem.NotMounted");
        } else {

            textOn = Tools.getString("ServiceInfo.Filesystem.RunningOn");
            textNotOn = Tools.getString("ServiceInfo.Filesystem.NotRunning");
        }
        if (isFailed(testOnly)) {
            sb.append("</b> <b>Failed</b>");
        } else if (isStopped(testOnly)
                   || nodeString == null) {
            sb.append("</b> " + textNotOn);
        } else {
            sb.append("</b> " + textOn + ": ");
            sb.append(nodeString);
        }
        if (!isManaged(testOnly)) {
            sb.append(" (unmanaged)");
        }
        return sb.toString();
    }

    /** Returns heartbeat service class. */
    public ResourceAgent getResourceAgent() {
        return resourceAgent;
    }

    /** Sets whether the info object is being updated. */
    @Override
    public void setUpdated(final boolean updated) {
        final GroupInfo gi = groupInfo;
        if (gi != null) {
            gi.setUpdated(updated);
            return;
        }
        final CloneInfo ci = cloneInfo;
        if (ci != null) {
            ci.setUpdated(updated);
            return;
        }
        if (updated && !isUpdated()) {
            getBrowser().getHeartbeatGraph().startAnimation(this);
        } else if (!updated) {
            getBrowser().getHeartbeatGraph().stopAnimation(this);
        }
        super.setUpdated(updated);
    }

    /** Returns text that appears in the corner of the graph. */
    public Subtext getRightCornerTextForGraph(final boolean testOnly) {
        if (getService().isOrphaned()) {
            if (isFailed(testOnly)) {
                return ORPHANED_FAILED_SUBTEXT;
            } else {
                return ORPHANED_SUBTEXT;
            }
        } else if (!isManaged(testOnly)) {
            return UNMANAGED_SUBTEXT;
        } else if (getMigratedTo(testOnly) != null
                   || getMigratedFrom(testOnly) != null) {
            return MIGRATED_SUBTEXT;
        }
        return null;
    }

    /** Returns text with lines as array that appears in the cluster graph. */
    public Subtext[] getSubtextsForGraph(final boolean testOnly) {
        Color color = null;
        final List<Subtext> texts = new ArrayList<Subtext>();
        String textOn;
        String textNotOn;
        if (getResourceAgent().isFilesystem()) {
            textOn = Tools.getString("ServiceInfo.Filesystem.MoutedOn");
            textNotOn = Tools.getString("ServiceInfo.Filesystem.NotMounted");
        } else {

            textOn = Tools.getString("ServiceInfo.Filesystem.RunningOn");
            textNotOn = Tools.getString("ServiceInfo.Filesystem.NotRunning");
        }
        if (getService().isOrphaned()) {
            texts.add(new Subtext("...",
                                  null,
                                  Color.BLACK));
        } else if (getResource().isNew()) {
            texts.add(new Subtext(textNotOn + " (new)",
                                  ClusterBrowser.FILL_PAINT_STOPPED,
                                  Color.BLACK));
        } else if (isFailed(testOnly)) {
            texts.add(new Subtext(textNotOn,
                                  null,
                                  Color.BLACK));
        } else if (isStopped(testOnly) && !isRunning(testOnly)) {
            texts.add(new Subtext("stopped",
                                  ClusterBrowser.FILL_PAINT_STOPPED,
                                  Color.BLACK));
        } else {
            Color textColor = Color.BLACK;
            String runningOnNodeString = null;
            if (getBrowser().allHostsDown()) {
                runningOnNodeString = "unknown";
            } else {
                final List<String> runningOnNodes = getRunningOnNodes(testOnly);
                if (runningOnNodes != null
                           && !runningOnNodes.isEmpty()) {
                    runningOnNodeString = runningOnNodes.get(0);
                    if (resourceAgent.isPingService()
                        && "0".equals(
                                getPingCount(runningOnNodeString, testOnly))) {
                        color = Color.RED;
                        textColor = Color.WHITE;
                    } else {
                        color = getBrowser().getCluster().getHostColors(
                                                        runningOnNodes).get(0);
                    }
                }
            }
            if (runningOnNodeString == null) {
                texts.add(new Subtext(textNotOn,
                                      ClusterBrowser.FILL_PAINT_STOPPED,
                                      textColor));
            } else {
                texts.add(new Subtext(textOn + ": " + runningOnNodeString
                                      + getPingCountString(runningOnNodeString,
                                                           testOnly),
                                      color,
                                      textColor));
            }
        }
        if (isOneFailedCount(testOnly)) {
            for (final Host host : getBrowser().getClusterHosts()) {
                if (host.isClStatus()
                    && getFailCount(host.getName(), testOnly) != null) {
                    texts.add(new Subtext(ClusterBrowser.IDENT_4
                                          + host.getName()
                                          + getFailCountString(
                                                        host.getName(),
                                                        testOnly),
                                          null,
                                          Color.BLACK));
                }
            }
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /** Returns null, when this service is not a clone. */
    public ServiceInfo getContainedService() {
        return null;
    }

    /** Returns type radio group. */
    Widget getTypeRadioGroup() {
        return typeRadioGroup;
    }

    /** Returns units. */
    @Override
    protected final Unit[] getUnits() {
        return new Unit[]{
            new Unit("",    "s",  "Second",      "Seconds"), /* default unit */
            new Unit("ms",  "ms", "Millisecond", "Milliseconds"),
            new Unit("us",  "us", "Microsecond", "Microseconds"),
            new Unit("s",   "s",  "Second",      "Seconds"),
            new Unit("min", "m",  "Minute",      "Minutes"),
            new Unit("h",   "h",  "Hour",        "Hours")
        };
    }

    /** Returns whether it is slave on all nodes. */
    protected boolean isSlaveOnAllNodes(final boolean testOnly) {
        return false;
    }

    /** Returns text that appears above the icon in the graph. */
    public String getIconTextForGraph(final boolean testOnly) {
        if (getBrowser().allHostsDown()) {
            return Tools.getString("ClusterBrowser.Hb.NoInfoAvailable");
        }
        final Host dcHost = getBrowser().getDCHost();
        if (getService().isNew()) {
            return "new...";
        } else if (getService().isOrphaned()) {
            return "";
        } else if (isEnslaved(testOnly)) {
            if (isSlaveOnAllNodes(testOnly)) {
                return "";
            } else {
                return Tools.getString("ClusterBrowser.Hb.Enslaving");
            }
        } else if (isStarted(testOnly)) {
            if (isRunning(testOnly)) {
                final List<Host> migratedTo = getMigratedTo(testOnly);
                if (migratedTo == null) {
                    final List<Host> migratedFrom = getMigratedFrom(testOnly);
                    if (migratedFrom != null) {
                        final List<String> runningOnNodes =
                                                   getRunningOnNodes(testOnly);
                        boolean alreadyThere = false;
                        if (runningOnNodes == null) {
                            alreadyThere = true;
                        } else  {
                            alreadyThere = true;
                            for (final Host mfrom : migratedFrom) {
                                if (runningOnNodes.contains(mfrom.getName())) {
                                    alreadyThere = false;
                                }
                            }
                        }
                        if (!alreadyThere) {
                            return Tools.getString(
                                            "ClusterBrowser.Hb.Migrating");
                        }
                    }
                } else {
                    final List<String> runningOnNodes =
                                               getRunningOnNodes(testOnly);
                    if (runningOnNodes != null) {
                        boolean alreadyThere = false;
                        for (final Host mto : migratedTo) {
                            if (runningOnNodes.contains(mto.getName())) {
                                alreadyThere = true;
                            }
                            if (!alreadyThere) {
                                return Tools.getString(
                                                "ClusterBrowser.Hb.Migrating");
                            }
                        }
                    }
                }
                return null;
            } else if (isFailed(testOnly)) {
                return Tools.getString("ClusterBrowser.Hb.StartingFailed");
            } else if (isGroupStopped(testOnly)) {
                return Tools.getString("ClusterBrowser.Hb.GroupStopped");
            } else {
                return Tools.getString("ClusterBrowser.Hb.Starting");
            }
        } else if (isStopped(testOnly)) {
            if (isRunning(testOnly)) {
                return Tools.getString("ClusterBrowser.Hb.Stopping");
            } else {
                return null;
            }
        }
        return null;
    }

    /** Returns hash with saved operations. */
    MultiKeyMap<String, String> getSavedOperation() {
        return savedOperation;
    }

    /** Reload combo boxes. */
    public void reloadComboBoxes() {
        if (sameAsOperationsWi != null) {
            String defaultOpIdRef = null;
            final Info savedOpIdRef = sameAsOperationsWiValue();
            if (savedOpIdRef != null) {
                defaultOpIdRef = savedOpIdRef.toString();
            }
            final String idRef = defaultOpIdRef;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    sameAsOperationsWi.reloadComboBox(
                                                  idRef,
                                                  getSameServicesOperations());
                }
            });
        }
        if (sameAsMetaAttrsWi != null) {
            String defaultMAIdRef = null;
            final Info savedMAIdRef = (Info) sameAsMetaAttrsWi.getValue();
            if (savedMAIdRef != null) {
                defaultMAIdRef = savedMAIdRef.toString();
            }
            final String idRef = defaultMAIdRef;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    sameAsMetaAttrsWi.reloadComboBox(
                                              idRef,
                                              getSameServicesMetaAttrs());
                }
            });
        }
    }

    /** Returns whether info panel is already created. */
    boolean isInfoPanelOk() {
        return infoPanel != null;
    }

    /** Connects with VMSVirtualDomainInfo object. */
    public VMSVirtualDomainInfo connectWithVMS() {
        /* for VirtualDomainInfo */
        return null;
    }

    /** Whether this class is a constraint placeholder. */
    public boolean isConstraintPH() {
        return false;
    }

    /** Remove constraints of this service. */
    void removeConstraints(final Host dcHost, final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final HbConnectionInfo[] hbcis =
                     getBrowser().getHeartbeatGraph().getHbConnections(this);
        for (final HbConnectionInfo hbci : hbcis) {
            if (hbci != null) {
                getBrowser().getHeartbeatGraph().removeOrder(hbci,
                                                             dcHost,
                                                             testOnly);
                getBrowser().getHeartbeatGraph().removeColocation(
                                                              hbci,
                                                              dcHost,
                                                              testOnly);
            }
        }

        for (final String locId : cs.getLocationIds(
                                      getHeartbeatId(testOnly),
                                      testOnly)) {
            CRM.removeLocation(dcHost,
                               locId,
                               getHeartbeatId(testOnly),
                               testOnly);
        }
    }

    /**
     * Returns value of the same as drop down menu as an info object or null.
     */
    final Info sameAsOperationsWiValue() {
        if (sameAsOperationsWi == null) {
            return null;
        }
        Info i = null;
        final Object o = sameAsOperationsWi.getValue();
        if (o instanceof Info) {
            i = (Info) o;
        }
        return i;
    }

    /** Store operation values. */
    final void storeOperations() {
        mSavedOperationsLock.lock();
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCRMOperationParams(op)) {
                final String defaultValue =
                              resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null) {
                    continue;
                }
                mOperationsComboBoxHashReadLock.lock();
                final Widget wi = operationsComboBoxHash.get(op, param);
                mOperationsComboBoxHashReadLock.unlock();
                savedOperation.put(op, param, wi.getStringValue());
            }
        }
        mSavedOperationsLock.unlock();
    }

    /** Returns score combo box. */
    protected final Map<HostInfo, Widget> getScoreComboBoxHash() {
        return scoreComboBoxHash;
    }

    /** Returns ping combo box. */
    public final Widget getPingComboBox() {
        return pingComboBox;
    }

    /** Sets ping combo box. */
    protected final void setPingComboBox(final Widget pingComboBox) {
        this.pingComboBox = pingComboBox;
    }

    /** Return operation combo box. */
    public final Widget getOperationsComboBox(final String op,
                                                final String param) {
        mOperationsComboBoxHashReadLock.lock();
        final Widget wi = operationsComboBoxHash.get(op, param);
        mOperationsComboBoxHashReadLock.unlock();
        return wi;
    }

    /** Return same as operations combo box. */
    public final Widget getSameAsOperationsWi() {
        return sameAsOperationsWi;
    }
}
