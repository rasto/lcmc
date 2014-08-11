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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import javax.inject.Provider;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.tree.DefaultMutableTreeNode;

import lcmc.gui.GUIData;
import lcmc.model.*;
import lcmc.model.crm.*;
import lcmc.model.crm.CrmXml;
import lcmc.model.ColorText;
import lcmc.model.resources.Service;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.EditableInfo;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.drbd.ResourceInfo;
import lcmc.gui.resources.vms.DomainInfo;
import lcmc.gui.widget.Check;
import lcmc.gui.widget.TextfieldWithUnit;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.CRM;
import lcmc.utilities.ComponentWithTest;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import lcmc.utilities.Unit;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.WidgetListener;
import org.apache.commons.collections15.map.MultiKeyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * This class holds info data for one hearteat service and allows to enter
 * its arguments and execute operations on it.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ServiceInfo extends EditableInfo {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceInfo.class);

    private static final Value NOTHING_SELECTED_VALUE = new StringValue();
    /** Default values item in the "same as" scrolling list in meta attributes.*/
    private static final Value META_ATTRS_DEFAULT_VALUES = new StringValue("default", "default values");
    ///** Default values internal name. */
    //private static final String META_ATTRS_DEFAULT_VALUES = "default";
    /** Default values item in the "same as" scrolling list in operations. */
    private static final Value OPERATIONS_DEFAULT_VALUES = new StringValue("default", "advisory minimum");
    /** Check the cached fields. */
    public static final String CACHED_FIELD = "cached";
    static final Value MASTER_SLAVE_TYPE_STRING = new StringValue("Master/Slave");
    static final ImageIcon MANAGE_BY_CRM_ICON = Tools.createImageIcon(
                                                      Tools.getDefault("ServiceInfo.ManageByCRMIcon"));
    static final ImageIcon UNMANAGE_BY_CRM_ICON = Tools.createImageIcon(
                                                      Tools.getDefault("ServiceInfo.UnmanageByCRMIcon"));
    public static final ImageIcon SERVICE_RUNNING_ICON_SMALL = Tools.createImageIcon(
                                                      Tools.getDefault("ServiceInfo.ServiceRunningIconSmall"));
    private static final ImageIcon SERVICE_RUNNING_FAILED_ICON_SMALL = Tools.createImageIcon(
                                                      Tools.getDefault("ServiceInfo.ServiceRunningFailedIconSmall"));
    /** Icon that indicates a started service (but not running). */
    private static final ImageIcon SERVICE_STARTED_ICON_SMALL = Tools.createImageIcon(
                                                      Tools.getDefault("ServiceInfo.ServiceStartedIconSmall"));
    /** Icon that indicates a stopping service (but not stopped). */
    private static final ImageIcon SERVICE_STOPPING_ICON_SMALL = Tools.createImageIcon(
                                                      Tools.getDefault("ServiceInfo.ServiceStoppingIconSmall"));
    public static final ImageIcon SERVICE_STOPPED_ICON_SMALL = Tools.createImageIcon(
                                                      Tools.getDefault("ServiceInfo.ServiceStoppedIconSmall"));
    private static final ImageIcon SERVICE_STOPPED_FAILED_ICON_SMALL = Tools.createImageIcon(
                                                      Tools.getDefault("ServiceInfo.ServiceStoppedFailedIconSmall"));
    static final ImageIcon SERVICE_RUNNING_ICON = Tools.createImageIcon(
                                                                Tools.getDefault("CRMGraph.ServiceRunningIcon"));
    /** Not running service icon. */
    private static final ImageIcon SERVICE_STOPPED_ICON = Tools.createImageIcon(
                                                                Tools.getDefault("CRMGraph.ServiceStoppedIcon"));
    static final ImageIcon START_ICON = SERVICE_RUNNING_ICON;
    static final ImageIcon STOP_ICON  = SERVICE_STOPPED_ICON;
    protected static final ImageIcon MIGRATE_ICON = Tools.createImageIcon(Tools.getDefault("CRMGraph.MigrateIcon"));
    static final ImageIcon UNMIGRATE_ICON = Tools.createImageIcon(Tools.getDefault("CRMGraph.UnmigrateIcon"));
    static final ImageIcon GROUP_UP_ICON = Tools.createImageIcon(Tools.getDefault("CRMGraph.GroupUp"));
    static final ImageIcon GROUP_DOWN_ICON = Tools.createImageIcon(Tools.getDefault("CRMGraph.GroupDown"));
    private static final ColorText ORPHANED_COLOR_TEXT = new ColorText("(LRM)", null, Color.BLACK);
    private static final ColorText ORPHANED_FAILED_COLOR_TEXT = new ColorText("(ORPHANED)", null, Color.RED);
    private static final ColorText UNMANAGED_COLOR_TEXT = new ColorText("(unmanaged)", null, Color.RED);
    private static final ColorText MIGRATED_COLOR_TEXT = new ColorText("(migrated)", null, Color.RED);
    private static final ColorText FENCED_COLOR_TEXT = new ColorText("(FENCED)", null, Color.RED);
    protected static final Value CLONE_TYPE_STRING = new StringValue("Clone");
    private static final Value PRIMITIVE_TYPE_STRING = new StringValue("Primitive");
    public static final String GUI_ID = "__drbdmcid";
    public static final String PCMK_ID = "__pckmkid";
    public static final String RA_PARAM = "__ra";
    /** String that appears as a tooltip in menu items if item is being removed. */
    static final String IS_BEING_REMOVED_STRING = "it is being removed";
    /** String that appears as a tooltip in menu items if item is orphan. */
    static final String IS_ORPHANED_STRING = "cannot do that to an ophan";
    /** String that appears as a tooltip in menu items if item is new. */
    static final String IS_NEW_STRING = "it is not applied yet";
    private static final Map<String, Value> PING_ATTRIBUTES = new HashMap<String, Value>();
    static {
        PING_ATTRIBUTES.put("eq0", new StringValue("eq0", "no ping: stop")); /* eq 0 */
        PING_ATTRIBUTES.put("defined", new StringValue("defined", "most connections"));
    }
    private final Map<HostInfo, Widget> scoreComboBoxHash = new HashMap<HostInfo, Widget>();
    private final Map<HostInfo, HostLocation> savedHostLocations = new HashMap<HostInfo, HostLocation>();
    /** A combobox with pingd constraint. */
    private Widget pingComboBox = null;
    /** Saved ping constraint. */
    private Value savedPingOperation = NOTHING_SELECTED_VALUE;
    /** Saved meta attrs id. */
    private String savedMetaAttrsId = null;
    /** Saved operations id. */
    private String savedOperationsId = null;
    /** A map from operation to the stored value. First key is
     * operation name like "start" and second key is parameter like
     * "timeout". */
    private final MultiKeyMap<String, Value> savedOperation = new MultiKeyMap<String, Value>();
    /** Whether id-ref for meta-attributes is used. */
    private ServiceInfo savedMetaAttrInfoRef = null;
    /** Combo box with same as operations option. */
    private Widget sameAsMetaAttrsWi = null;
    /** Whether id-ref for operations is used. */
    private ServiceInfo savedOperationIdRef = null;
    /** Combo box with same as operations option. */
    private Widget sameAsOperationsWi = null;
    private final Lock mSavedOperationsLock = new ReentrantLock();
    private final ReadWriteLock mOperationsComboBoxHashLock = new ReentrantReadWriteLock();
    private final Lock mOperationsComboBoxHashReadLock = mOperationsComboBoxHashLock.readLock();
    private final Lock mOperationsComboBoxHashWriteLock = mOperationsComboBoxHashLock.writeLock();
    private final MultiKeyMap<String, Widget> operationsComboBoxHash = new MultiKeyMap<String, Widget>();
    private JComponent infoPanel = null;
    /** Group info object of the group this service is in or null, if it is
     * not in any group. */
    private GroupInfo groupInfo = null;
    /** Master/Slave info object, if is null, it is not master/slave
     * resource. */
    private volatile CloneInfo cloneInfo = null;
    private ResourceAgent resourceAgent;
    /** Radio buttons for clone/master/slave primitive resources. */
    private Widget typeRadioGroup;
    @Autowired
    private GUIData guiData;
    @Autowired
    private ServiceMenu serviceMenu;
    @Autowired
    private Provider<FilesystemInfo> filesystemInfoProvider;
    @Autowired
    private Provider<LinbitDrbdInfo> linbitDrbdInfoProvider;
    @Autowired
    private Provider<DrbddiskInfo> drbddiskInfoProvider;
    @Autowired
    private Provider<IPaddrInfo> ipaddrInfoProvider;
    @Autowired
    private Provider<VirtualDomainInfo> virtualDomainInfoProvider;
    @Autowired
    private Provider<GroupInfo> groupInfoProvider;
    @Autowired
    private Provider<CloneInfo> cloneInfoProvider;
    @Autowired @Qualifier("serviceInfo")
    private Provider<ServiceInfo> serviceInfoProvider;

    protected void init(final String name, final ResourceAgent resourceAgent, final Browser browser) {
        super.init(name, browser);
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
     * Prepares a new {@code ServiceInfo} object and creates
     * new service object. It also initializes parameters along with
     * heartbeat id with values from xml stored in resourceNode.
     */
    protected void init(final String name,
                        final ResourceAgent ra,
                        final String heartbeatId,
                        final Map<String, String> resourceNode,
                        final Browser browser) {
        init(name, ra, browser);
        getService().setCrmId(heartbeatId);
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
        return getService().getCrmId();
    }

    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    public void setInfoPanel(final JPanel infoPanel) {
        this.infoPanel = infoPanel;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        return checkResourceFields(param, params, false, false, false);
    }

    private boolean areAllMetaAttrsAreDefaultValues(final String[] params) {
        if (params != null) {
            for (final String otherParam : params) {
                if (isMetaAttr(otherParam)) {
                    final Widget wi = getWidget(otherParam, null);
                    if (wi == null) {
                        continue;
                    }
                    final Value newValue = wi.getValue();
                    final Value defaultValue = getParamDefault(otherParam);
                    if (!Tools.areEqual(newValue, defaultValue)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void oldStyleResourcesHideFields(final String[] params) {
        /* in old style resources don't show all the textfields */
        boolean visible = false;
        Widget wi = null;
        for (int i = params.length - 1; i >= 0; i--) {
            final Widget prevWi = getWidget(params[i], null);
            if (prevWi == null) {
                continue;
            }
            if (!visible && !prevWi.getStringValue().isEmpty()) {
                visible = true;
            }
            if (wi != null && wi.getComponent().isVisible() != visible) {
                wi.setVisible(visible);
            }
            wi = prevWi;
        }
    }

    private boolean checkSameAsMetaAttrsFieldsChanged(final String[] params) {
        final Value info = sameAsMetaAttrsWiValue();
        final boolean defaultValues = info != null && META_ATTRS_DEFAULT_VALUES.equals(info);
        final boolean nothingSelected = info == null || info.isNothingSelected();
        if (!nothingSelected && !defaultValues && info != savedMetaAttrInfoRef) {
            sameAsMetaAttrsWi.processAccessMode();
            return true;
        } else {
            if ((nothingSelected || defaultValues) && savedMetaAttrInfoRef != null) {
                sameAsMetaAttrsWi.processAccessMode();
                return true;
            }
            final boolean allMetaAttrsAreDefaultValues = areAllMetaAttrsAreDefaultValues(params);
            if (savedMetaAttrInfoRef == null
                && defaultValues != allMetaAttrsAreDefaultValues) {
                if (allMetaAttrsAreDefaultValues) {
                    Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                        @Override
                        public void run() {
                            sameAsMetaAttrsWi.setValueNoListeners(META_ATTRS_DEFAULT_VALUES);
                        }
                    });
                } else {
                    Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                        @Override
                        public void run() {
                            sameAsMetaAttrsWi.setValueNoListeners(null);
                        }
                    });
                }
            }
        }
        sameAsMetaAttrsWi.processAccessMode();
        return false;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    public Check checkResourceFields(final String param,
                                     final String[] params,
                                     final boolean fromServicesInfo,
                                     final boolean fromCloneInfo,
                                     final boolean fromGroupInfo) {
        final Value idV = getComboBoxValue(GUI_ID);
        String id = null;
        if (idV != null) {
            id = getComboBoxValue(GUI_ID).getValueForConfig();
        }
        final CloneInfo ci = getCloneInfo();
        if (!fromCloneInfo && ci != null) {
            return ci.checkResourceFields(param, ci.getParametersFromXML(), fromServicesInfo);
        }

        final List<String> incorrect = new ArrayList<String>();
        final List<String> changed = new ArrayList<String>();

        final Check check = new Check(incorrect, changed);
        final GroupInfo gi = getGroupInfo();
        if (!fromGroupInfo && gi != null) {
            final Check groupCheck = gi.checkResourceFields(param,
                                                            gi.getParametersFromXML(),
                                                            fromServicesInfo,
                                                            fromCloneInfo);
            check.addCheck(groupCheck);
            if (!fromServicesInfo) {
                gi.setApplyButtons(null, gi.getParametersFromXML());
            }
        }
        check.addCheck(super.checkResourceFields(param, params));
        if (getService().isOrphaned()) {
            incorrect.add("resource is orphaned");
        }
        final String heartbeatId = getService().getCrmId();
        /* Allow it only for resources that are in LRM. */
        ServiceInfo si = null;
        if (id != null) {
            si = getBrowser().getServiceInfoFromId(getService().getName(), id);
        }
        if (si != null && si != this && !si.getService().isOrphaned()) {
            incorrect.add("not in LRM");
        }
        if (Application.PACEMAKER_GROUP_NAME.equals(getName())) {
            if (heartbeatId == null) {
                changed.add("new resource");
            } else if (heartbeatId.equals(Service.GRP_ID_PREFIX + id)
                || heartbeatId.equals(id)) {
                if (ci == null) {
                    check.addCheck(checkHostLocationsFields());
                }
                check.addCheck(checkOperationFields());
            }
        } else if (Application.PM_CLONE_SET_NAME.equals(getName())
                   || Application.PM_MASTER_SLAVE_SET_NAME.equals(getName())) {
            final String prefix;
            if (getService().isMaster()) {
                prefix = Service.MS_ID_PREFIX;
            } else {
                prefix = Service.CL_ID_PREFIX;
            }
            if (heartbeatId != null
                && (heartbeatId.equals(prefix + id)
                    || heartbeatId.equals(id))) {
                if (ci == null) {
                    check.addCheck(checkHostLocationsFields());
                }
            }
        } else {
            if (heartbeatId == null) {
            } else if (heartbeatId.equals(Service.RES_ID_PREFIX + getService().getName() + '_' + id)
                       || heartbeatId.equals(Service.STONITH_ID_PREFIX + getService().getName() + '_' + id)
                       || heartbeatId.equals(id)) {
                if (ci == null) {
                    check.addCheck(checkHostLocationsFields());
                }
                check.addCheck(checkOperationFields());
            }
        }
        final String cl = getService().getResourceClass();
        if (cl != null && (cl.equals(ResourceAgent.HEARTBEAT_CLASS_NAME) || ResourceAgent.SERVICE_CLASSES.contains(cl))) {
            oldStyleResourcesHideFields(params);
        }

        /* id-refs */
        if (sameAsMetaAttrsWi != null) {
            if (checkSameAsMetaAttrsFieldsChanged(params)) {
                changed.add("meta attributes");
            }
        }
        if (!fromServicesInfo) {
            final ServicesInfo sis = getBrowser().getServicesInfo();
            sis.setApplyButtons(null, sis.getParametersFromXML());
        }
        return check;
    }

    private Value getOpDefaultsDefault(final String param) {
        assert param != null;
        /* if op_defaults is set... It cannot be set in the GUI  */
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (cs != null) {
            return cs.getOpDefaultsValuePairs().get(param);
        }
        return null;
    }

    /** Sets service parameters with values from resourceNode hash. */
    protected void setParameters(final Map<String, String> resourceNode) {
        if (resourceNode == null) {
            return;
        }
        final boolean infoPanelOk = isInfoPanelOk();
        final CrmXml crmXML = getBrowser().getCrmXml();
        if (crmXML == null) {
            LOG.appError("setParameters: crmXML is null");
            return;
        }
        /* Attributes */
        final String[] params = getEnabledSectionParams(crmXML.getOcfMetaDataParameters(resourceAgent,
                getService().isMaster()));
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (params != null) {
            final String newMetaAttrsId = cs.getMetaAttrsId(getService().getCrmId());
            if ((savedMetaAttrsId == null && newMetaAttrsId != null)
                || (savedMetaAttrsId != null && !savedMetaAttrsId.equals(newMetaAttrsId))) {
                /* newly generated operations id, reload all other combo
                   boxes. */
                getBrowser().reloadAllComboBoxes(this);
            }
            savedMetaAttrsId = newMetaAttrsId;
            String refCRMId = cs.getMetaAttrsRef(getService().getCrmId());
            final ServiceInfo metaAttrInfoRef = getBrowser().getServiceInfoFromCRMId(refCRMId);
            if (refCRMId == null) {
                refCRMId = getService().getCrmId();
            }
            resourceNode.put(PCMK_ID, getService().getCrmId());
            resourceNode.put(GUI_ID, getService().getId());
            boolean allMetaAttrsAreDefaultValues = true;
            boolean allSavedMetaAttrsAreDefaultValues = true;
            for (final String param : params) {
                Value value;
                if (isMetaAttr(param) && refCRMId != null) {
                    value = new StringValue(cs.getParameter(refCRMId, param, Application.RunMode.LIVE));
                } else {
                    value = new StringValue(resourceNode.get(param));
                }
                final Value defaultValue = getParamDefault(param);
                if (value.isNothingSelected()) {
                    value = defaultValue;
                }
                final Value oldValue = getResource().getValue(param);
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
                    final boolean haveChanged = !Tools.areEqual(value, oldValue)
                                                || !Tools.areEqual(defaultValue,
                                                                   getResource().getDefaultValue(param));
                    if (haveChanged || (metaAttrInfoRef != null && isMetaAttr(param))) {
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
                                sameAsMetaAttrsWi.setValue(META_ATTRS_DEFAULT_VALUES);
                            }
                        } else {
                            if (metaAttrInfoRef != null) {
                                sameAsMetaAttrsWi.setValue(null);
                            }
                        }
                    } else {
                        sameAsMetaAttrsWi.setValue(metaAttrInfoRef);
                    }
                }
            }
        }

        /* set scores */
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation hostLocation = cs.getScore(getService().getCrmId(),
                                                          hi.getName(),
                                                          Application.RunMode.LIVE);
            final HostLocation savedLocation = savedHostLocations.get(hi);
            if (Tools.areEqual(hostLocation, savedLocation)) {
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
                        wi.setValue(new StringValue(score));
                        final JLabel label = wi.getLabel();
                        final String text = getHostLocationLabel(hi.getName(), op);
                        label.setText(text);
                    }
                }
            }
        }

        /* set ping constraint */
        final HostLocation hostLocation = cs.getPingScore(getService().getCrmId(), Application.RunMode.LIVE);
        Value pingOperation = null;
        if (hostLocation != null) {
            final String op = hostLocation.getOperation();
            final String value = hostLocation.getValue();
            if ("eq".equals(op) && "0".equals(value)) {
                pingOperation = new StringValue("eq0");
            } else {
                pingOperation = new StringValue(hostLocation.getOperation());
            }
        }
        if (!Tools.areEqual(pingOperation, savedPingOperation)) {
            savedPingOperation = pingOperation;
        }
        if (infoPanelOk) {
            final Widget wi = pingComboBox;
            if (wi != null) {
                if (pingOperation == null) {
                    wi.setValue(null);
                } else {
                    wi.setValue(PING_ATTRIBUTES.get(pingOperation.getValueForConfig()));
                }
            }
        }

        /* Operations */
        final String newOperationsId = cs.getOperationsId(getService().getCrmId());
        if ((savedOperationsId == null && newOperationsId != null)
            || (savedOperationsId != null && !savedOperationsId.equals(newOperationsId))) {
            /* newly generated operations id, reload all other combo
               boxes. */
            getBrowser().reloadAllComboBoxes(this);
        }

        savedOperationsId = newOperationsId;
        String refCRMId = cs.getOperationsRef(getService().getCrmId());
        final ServiceInfo operationIdRef = getBrowser().getServiceInfoFromCRMId(refCRMId);
        if (refCRMId == null) {
            refCRMId = getService().getCrmId();
        }
        mSavedOperationsLock.lock();
        boolean allAreDefaultValues = true;
        boolean allSavedAreDefaultValues = true;
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCrmOperationParams(op)) {
                Value defaultValue = resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null || defaultValue.isNothingSelected()) {
                    continue;
                }
                if (ClusterBrowser.CRM_OPERATIONS_WITH_IGNORED_DEFAULT.contains(op)) {
                    defaultValue = NOTHING_SELECTED_VALUE;
                }
                Value value = cs.getOperation(refCRMId, op, param);
                if (value == null || value.isNothingSelected()) {
                    value = getOpDefaultsDefault(param);
                }
                if (!Tools.areEqual(defaultValue, value)) {
                    allAreDefaultValues = false;
                }
                if (!Tools.areEqual(defaultValue, savedOperation.get(op, param))) {
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
                            sameAsOperationsWi.setValue(OPERATIONS_DEFAULT_VALUES);
                        }
                    } else {
                        if (savedOperationIdRef != null) {
                            sameAsOperationsWi.setValue(null);
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
                for (final String param : getBrowser().getCrmOperationParams(op)) {
                    final Value defaultValue = resourceAgent.getOperationDefault(op, param);
                    if (defaultValue == null || defaultValue.isNothingSelected()) {
                        continue;
                    }
                    Value value = cs.getOperation(refCRMId, op, param);
                    if (value == null || value.isNothingSelected()) {
                        value = getOpDefaultsDefault(param);
                    }
                    if (!Tools.areEqual(value, savedOperation.get(op, param))) {
                        savedOperation.put(op, param, value);
                        if (infoPanelOk) {
                            mOperationsComboBoxHashReadLock.lock();
                            final Widget wi;
                            try {
                                wi = operationsComboBoxHash.get(op, param);
                            } finally {
                                mOperationsComboBoxHashReadLock.unlock();
                            }
                            Tools.invokeLater(new Runnable() {
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
        if (cs.isOrphaned(getHeartbeatId(Application.RunMode.LIVE))) {
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

    String getNameForLog() {
        return getName();
    }

    /**
     * Returns a name of the service with id in the parentheses.
     * It adds prefix 'new' if id is null.
     */
    @Override
    public String getValueForConfig() {
        return getName() + '_' + getService().getId();
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
            && provider != null && !provider.isEmpty()) {
            s.append(provider);
            s.append(':');
        }
        s.append(getName());
        final String serviceId = getService().getId();

        /* 'string' contains the last string if there are more dependent
         * resources, although there is usually only one. */
        if (serviceId == null) {
            s.insert(0, "new ");
        } else {
            if (!serviceId.isEmpty()) {
                s.append(" (");
                s.append(serviceId);
                s.append(')');
            }
        }
        return s.toString();
    }

    /** Returns node name of the host where this service is running. */
    List<String> getMasterOnNodes(final Application.RunMode runMode) {
        return getBrowser().getClusterStatus().getMasterOnNodes(getHeartbeatId(runMode), runMode);
    }

    /** Returns node name of the host where this service is running. */
    protected List<String> getRunningOnNodes(final Application.RunMode runMode) {
        return getBrowser().getClusterStatus().getRunningOnNodes(getHeartbeatId(runMode), runMode);
    }

    boolean isStarted(final Application.RunMode runMode) {
        final Host dcHost = getBrowser().getDCHost();
        String targetRoleString = "target-role";
        if (Tools.versionBeforePacemaker(dcHost)) {
            targetRoleString = "target_role";
        }
        String crmId = getHeartbeatId(runMode);
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final String refCRMId = cs.getMetaAttrsRef(crmId);
        if (refCRMId != null) {
            crmId = refCRMId;
        }
        String targetRole = cs.getParameter(crmId, targetRoleString, runMode);
        if (targetRole == null) {
            targetRole = getParamDefault(targetRoleString).getValueForConfig();
        }
        return !CrmXml.TARGET_ROLE_STOPPED.equals(targetRole);
    }

    /** Returns whether the service was set to be in slave role. */
    public boolean isEnslaved(final Application.RunMode runMode) {
        final Host dcHost = getBrowser().getDCHost();
        String targetRoleString = "target-role";
        if (Tools.versionBeforePacemaker(dcHost)) {
            targetRoleString = "target_role";
        }
        String crmId = getHeartbeatId(runMode);
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final String refCRMId = cs.getMetaAttrsRef(crmId);
        if (refCRMId != null) {
            crmId = refCRMId;
        }
        String targetRole = cs.getParameter(crmId, targetRoleString, runMode);
        if (targetRole == null) {
            targetRole = getParamDefault(targetRoleString).getValueForConfig();
        }
        return CrmXml.TARGET_ROLE_SLAVE.equals(targetRole);
    }

    public boolean isStopped(final Application.RunMode runMode) {
        final Host dcHost = getBrowser().getDCHost();
        String targetRoleString = "target-role";
        if (Tools.versionBeforePacemaker(dcHost)) {
            targetRoleString = "target_role";
        }
        String crmId = getHeartbeatId(runMode);
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final String refCRMId = cs.getMetaAttrsRef(crmId);
        if (refCRMId != null) {
            crmId = refCRMId;
        }
        String targetRole = cs.getParameter(crmId, targetRoleString, runMode);
        if (targetRole == null) {
            targetRole = getParamDefault(targetRoleString).getValueForConfig();
        }
        return CrmXml.TARGET_ROLE_STOPPED.equals(targetRole);
    }

    boolean isGroupStopped(final Application.RunMode runMode) {
        return false;
    }

    /**
     * TODO: "default" value
     */
    public boolean isManaged(final Application.RunMode runMode) {
        return getBrowser().getClusterStatus().isManaged(getHeartbeatId(runMode), runMode);
    }

    /** Returns whether the service where was migrated or null. */
    public List<Host> getMigratedTo(final Application.RunMode runMode) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (final Host host : getBrowser().getClusterHosts()) {
            final String locationId = cs.getLocationId(getHeartbeatId(runMode), host.getName(), runMode);
            if (locationId == null
                || (!locationId.startsWith("cli-prefer-")
                    && !locationId.startsWith("cli-standby-")
                    && !locationId.startsWith("cli-ban-"))) {
                continue;
            }
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation hostLocation = cs.getScore(getHeartbeatId(runMode), hi.getName(), runMode);
            String score = null;
            String op = null;
            if (hostLocation != null) {
                score = hostLocation.getScore();
                op = hostLocation.getOperation();
            }
            if ((CrmXml.INFINITY_VALUE.getValueForConfig().equals(score)
                 || CrmXml.PLUS_INFINITY_VALUE.getValueForConfig().equals(score))
                && "eq".equals(op)) {
                final List<Host> hosts = new ArrayList<Host>();
                hosts.add(host);
                return hosts;
            }
        }
        return null;
    }

    public List<Host> isFenced(final Application.RunMode runMode) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (final Host host : getBrowser().getClusterHosts()) {
            final String locationId = cs.getLocationId(getHeartbeatId(runMode), host.getName(), runMode);
            if (locationId == null || !locationId.contains("fence")) {
                continue;
            }
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation hostLocation = cs.getScore(getHeartbeatId(runMode), hi.getName(), runMode);
            String score = null;
            if (hostLocation != null) {
                score = hostLocation.getScore();
            }
            if (CrmXml.MINUS_INFINITY_VALUE.getValueForConfig().equals(score)) {
                final List<Host> hosts = new ArrayList<Host>();
                hosts.add(host);
                return hosts;
            }
        }
        return null;
    }

    /** Returns whether the service where was migrated or null. */
    public List<Host> getMigratedFrom(final Application.RunMode runMode) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (final Host host : getBrowser().getClusterHosts()) {
            final String locationId = cs.getLocationId(getHeartbeatId(runMode), host.getName(), runMode);
            if (locationId == null
                || (!locationId.startsWith("cli-prefer-")
                    && !locationId.startsWith("cli-standby-")
                    && !locationId.startsWith("cli-ban-"))) {
                continue;
            }
            final HostInfo hi = host.getBrowser().getHostInfo();
            final HostLocation hostLocation = cs.getScore(getHeartbeatId(runMode), hi.getName(), runMode);
            String score = null;
            String op = null;
            if (hostLocation != null) {
                score = hostLocation.getScore();
                op = hostLocation.getOperation();
            }
            if (CrmXml.MINUS_INFINITY_VALUE.getValueForConfig().equals(score) && "eq".equals(op)) {
                final List<Host> hosts = new ArrayList<Host>();
                hosts.add(host);
                return hosts;
            }
        }
        return null;
    }

    public boolean isRunning(final Application.RunMode runMode) {
        final List<String> runningOnNodes = getRunningOnNodes(runMode);
        return runningOnNodes != null && !runningOnNodes.isEmpty();
    }

    private String getFailCountString(final String hostName, final Application.RunMode runMode) {
        String fcString = "";
        final String failCount = getFailCount(hostName, runMode);
        if (failCount != null) {
            if (CrmXml.INFINITY_VALUE.getValueForConfig().equals(failCount)) {
                fcString = " failed";
            } else {
                fcString = " failed: " + failCount;
            }
        }
        return fcString;
    }


    protected String getFailCount(final String hostName, final Application.RunMode runMode) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        return cs.getFailCount(hostName, getHeartbeatId(runMode), runMode);
    }

    protected String getPingCount(final String hostName, final Application.RunMode runMode) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        return cs.getPingCount(hostName, runMode);
    }

    protected String getPingCountString(final String hostName, final Application.RunMode runMode) {
        if (!resourceAgent.isPingService()) {
            return "";
        }
        final String pingCount = getPingCount(hostName, runMode);
        if (pingCount == null || "0".equals(pingCount)) {
            return " / no ping";
        } else {
            return " / ping: " + pingCount;
        }
    }

    /** Returns whether the resource is orphaned on the specified host. */
    protected final boolean isInLRMOnHost(final String hostName, final Application.RunMode runMode) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        return cs.isInLRMOnHost(hostName, getHeartbeatId(runMode), runMode);
    }

    /** Returns whether the resource failed on the specified host. */
    protected final boolean failedOnHost(final String hostName, final Application.RunMode runMode) {
        final String failCount = getFailCount(hostName, runMode);
        return failCount != null && CrmXml.INFINITY_VALUE.getValueForConfig().equals(failCount);
    }

    /** Returns whether the resource has failed to start. */
    public boolean isFailed(final Application.RunMode runMode) {
        if (isRunning(runMode)) {
            return false;
        }
        for (final Host host : getBrowser().getClusterHosts()) {
            if (host.isCrmStatusOk() && failedOnHost(host.getName(), runMode)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether the resource has failed on one of the nodes. */
    boolean isOneFailed(final Application.RunMode runMode) {
        for (final Host host : getBrowser().getClusterHosts()) {
            if (failedOnHost(host.getName(), runMode)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether the resource has fail-count on one of the nodes. */
    boolean isOneFailedCount(final Application.RunMode runMode) {
        for (final Host host : getBrowser().getClusterHosts()) {
            if (getFailCount(host.getName(), runMode) != null) {
                return true;
            }
        }
        return false;
    }

    void setManaged(final boolean isManaged, final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        CRM.setManaged(dcHost, getHeartbeatId(runMode), isManaged, runMode);
    }

    public List<Color> getHostColors(final Application.RunMode runMode) {
        return getBrowser().getCluster().getHostColorsInGraphs(getRunningOnNodes(runMode));
    }

    /**
     * Returns service icon in the menu. It can be started or stopped.
     * TODO: broken icon, not managed icon.
     */
    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        if (isFailed(runMode)) {
            if (isRunning(runMode)) {
                return SERVICE_RUNNING_FAILED_ICON_SMALL;
            } else {
                return SERVICE_STOPPED_FAILED_ICON_SMALL;
            }
        } else if (isStopped(runMode) || getBrowser().allHostsWithoutClusterStatus()) {
            if (isRunning(runMode)) {
                return SERVICE_STOPPING_ICON_SMALL;
            } else {
                return SERVICE_STOPPED_ICON_SMALL;
            }
        } else {
            if (isRunning(runMode)) {
                return SERVICE_RUNNING_ICON_SMALL;
            } else {
                return SERVICE_STARTED_ICON_SMALL;
            }
        }
    }

    /**
     * TODO: wrong doku
     * Converts enumeration to the info array, get objects from
     * hash if they exist.
     */
    protected Value[] enumToInfoArray(final Value defaultValue,
                                      final String serviceName,
                                      final Enumeration<DefaultMutableTreeNode> e) {
        final List<Value> list = new ArrayList<Value>();
        if (defaultValue != null) {
            list.add(defaultValue);
        }

        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n = e.nextElement();
            final Info i = (Info) n.getUserObject();
            final ServiceInfo si = getBrowser().getServiceInfoFromId(serviceName, i.getName());
            if (si == null && !Tools.areEqual(i, defaultValue)) {
                list.add(i);
            }
        }
        return list.toArray(new Value[list.size()]);
    }

    private void storeHostLocations() {
        savedHostLocations.clear();
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Widget wi = scoreComboBoxHash.get(hi);
            final Value score = wi.getValue();
            final String op = getOpFromLabel(hi.getName(), wi.getLabel().getText());
            if (score == null || score.isNothingSelected()) {
                savedHostLocations.remove(hi);
            } else {
                savedHostLocations.put(hi, new HostLocation(score.getValueForConfig(), op, null, null));
            }
        }
        /* ping */
        savedPingOperation = pingComboBox.getValue();
    }

    private Check checkOperationFields() {
        final List<String> changed = new ArrayList<String>();
        mSavedOperationsLock.lock();
        boolean allAreDefaultValues = true;
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCrmOperationParams(op)) {
                Value defaultValue = resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null || defaultValue.isNothingSelected()) {
                    continue;
                }
                if (ClusterBrowser.CRM_OPERATIONS_WITH_IGNORED_DEFAULT.contains(op)) {
                    defaultValue = NOTHING_SELECTED_VALUE;
                }
                mOperationsComboBoxHashReadLock.lock();

                final Widget wi;
                try {
                    wi = operationsComboBoxHash.get(op, param);
                } finally {
                    mOperationsComboBoxHashReadLock.unlock();
                }
                if (wi == null) {
                    continue;
                }
                if (CrmXml.PARAM_OCF_CHECK_LEVEL.equals(param)) {
                    final Value value = wi.getValue();
                    if (!Tools.areEqual(value, defaultValue)) {
                        allAreDefaultValues = false;
                    }
                    final Value savedOp = savedOperation.get(op, param);
                    if (savedOp == null) {
                        if (!Tools.areEqual(value, defaultValue)) {
                            changed.add(param + ": " + defaultValue + " \u2192 " + value);
                        }
                    } else if (!Tools.areEqual(value, savedOp)) {
                        changed.add(param + ": " + savedOp + " \u2192 " + value);
                    }
                    wi.setBackground(defaultValue, savedOp, false);
                } else {
                    final Value value = wi.getValue();
                    if (!Tools.areEqual(value, defaultValue)) {
                        allAreDefaultValues = false;
                    }
                    final Value savedOp = savedOperation.get(op, param);
                    if (!Tools.areEqual(value, savedOp)) {
                        changed.add(param + ": " + savedOp + " \u2192 " + value);
                    }
                    wi.setBackground(defaultValue, savedOp, false);
                }
            }
        }
        if (sameAsOperationsWi != null) {
            final Value info = sameAsOperationsWiValue();
            final boolean defaultValues = info != null && OPERATIONS_DEFAULT_VALUES.equals(info);
            final boolean nothingSelected = info == null || info.isNothingSelected();
            if (!nothingSelected && !defaultValues && info != savedOperationIdRef) {
                changed.add("operation id ref");
            } else {
                if ((nothingSelected || defaultValues) && savedOperationIdRef != null) {
                    changed.add("operation id ref");
                }
                if (savedOperationIdRef == null && defaultValues != allAreDefaultValues) {
                    if (allAreDefaultValues) {
                        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                            @Override
                            public void run() {
                                sameAsOperationsWi.setValueNoListeners(OPERATIONS_DEFAULT_VALUES);
                            }
                        });
                    } else {
                        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                            @Override
                            public void run() {
                                sameAsOperationsWi.setValueNoListeners(null);
                            }
                        });
                    }
                }
            }
            sameAsOperationsWi.processAccessMode();
        }
        mSavedOperationsLock.unlock();
        final List<String> incorrect = new ArrayList<String>();
        return new Check(incorrect, changed);
    }

    private Check checkHostLocationsFields() {
        final List<String> changed = new ArrayList<String>();
        final List<String> incorrect = new ArrayList<String>();
        boolean hostLocationFound = false;
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Widget wi = scoreComboBoxHash.get(hi);
            final HostLocation hlSaved = savedHostLocations.get(hi);
            final Value hsSaved;
            String opSaved = null;
            if (hlSaved == null) {
                hsSaved = NOTHING_SELECTED_VALUE;
            } else {
                hsSaved = new StringValue(hlSaved.getScore());
                opSaved = hlSaved.getOperation();
            }
            final String opSavedLabel = getHostLocationLabel(host.getName(), opSaved);
            if (wi == null) {
                continue;
            }
            String labelText = null;
            final JLabel label = wi.getLabel();
            if (label != null) {
                labelText = label.getText();
                final String op = getOpFromLabel(hi.getName(), label.getText());
                if (wi.getValue() == null || "eq".equals(op)) {
                    hostLocationFound = true;
                }
            }
            if (!Tools.areEqual(hsSaved, wi.getValue())
                || (!Tools.areEqual(opSavedLabel, labelText)
                    && (hsSaved != null  && !hsSaved.isNothingSelected()))) {
                changed.add("host location");
            }
            wi.setBackground(getHostLocationLabel(host.getName(), "eq"), null, opSavedLabel, hsSaved, false);
        }
        if (!hostLocationFound) {
            incorrect.add("no host location found");
        }
        /* ping */
        final Widget pwi = pingComboBox;
        if (pwi != null) {
            if (!Tools.areEqual(savedPingOperation, pwi.getValue())) {
                changed.add("ping operation");
            }
            pwi.setBackground(null, savedPingOperation, false);
        }
        return new Check(incorrect, changed);
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
    Value[] getCommonBlockDevInfos(final Value defaultValue, final String serviceName) {
        final List<Value> list = new ArrayList<Value>();

        /* drbd resources */
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> drbdResources = getBrowser().getDrbdNode().children();

        if (defaultValue != null) {
            list.add(defaultValue);
        }
        while (drbdResources.hasMoreElements()) {
            final DefaultMutableTreeNode n = drbdResources.nextElement();
            if (!(n.getUserObject() instanceof ResourceInfo)) {
                continue;
            }
            final ResourceInfo drbdRes = (ResourceInfo) n.getUserObject();
            final DefaultMutableTreeNode drbdResNode = drbdRes.getNode();
            if (drbdResNode != null) {
                @SuppressWarnings("unchecked")
                final Enumeration<DefaultMutableTreeNode> drbdVolumes = drbdResNode.children();
                while (drbdVolumes.hasMoreElements()) {
                    final DefaultMutableTreeNode vn = drbdVolumes.nextElement();
                    final Value drbdVol = (Value) vn.getUserObject();
                    list.add(drbdVol);
                }
            }
        }

        /* block devices that are the same on all hosts */
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> wids = getBrowser().getCommonBlockDevicesNode().children();
        while (wids.hasMoreElements()) {
            final DefaultMutableTreeNode n = wids.nextElement();
            final Value wid = (Value) n.getUserObject();
            list.add(wid);
        }

        return list.toArray(new Value[list.size()]);
    }

    protected void addCloneFields(final JPanel optionsPanel,
                                  final int leftWidth,
                                  final int rightWidth,
                                  final CloneInfo ci) {
        final String[] params = ci.getParametersFromXML();
        final Info savedMAIdRef = ci.getSavedMetaAttrInfoRef();
        ci.getResource().setValue(GUI_ID, new StringValue(ci.getService().getId()));
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

        ci.addHostLocations(optionsPanel, ClusterBrowser.SERVICE_LABEL_WIDTH, ClusterBrowser.SERVICE_FIELD_WIDTH);
    }

    /**
     * Returns label for host locations, which consist of host name and
     * operation.
     */
    private String getHostLocationLabel(final String hostName, final String op) {
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

    protected void addHostLocations(final JPanel optionsPanel, final int leftWidth, final int rightWidth) {
        final JPanel panel = getParamPanel(Tools.getString("ClusterBrowser.HostLocations"));
        panel.setLayout(new SpringLayout());
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Map<String, String> abbreviations = new HashMap<String, String>();
            abbreviations.put("i", CrmXml.INFINITY_VALUE.getValueForConfig());
            abbreviations.put("+", CrmXml.PLUS_INFINITY_VALUE.getValueForConfig());
            abbreviations.put("I", CrmXml.INFINITY_VALUE.getValueForConfig());
            abbreviations.put("a", "ALWAYS");
            abbreviations.put("n", "NEVER");
            final Widget wi = WidgetFactory.createInstance(
                                  Widget.Type.COMBOBOX,
                                  NOTHING_SELECTED_VALUE,
                                  new Value[]{NOTHING_SELECTED_VALUE,
                                              new StringValue("0"),
                                              new StringValue("2"),
                                              new StringValue("ALWAYS"),
                                              new StringValue("NEVER"),
                                              CrmXml.INFINITY_VALUE,
                                              CrmXml.MINUS_INFINITY_VALUE,
                                              CrmXml.INFINITY_VALUE},

                                  "^((-?\\d*|(-|\\+)?"
                                  + CrmXml.INFINITY_VALUE
                                  + "))|ALWAYS|NEVER|@NOTHING_SELECTED@$",

                                  rightWidth,
                                  abbreviations,
                                  new AccessMode(Application.AccessType.ADMIN, false),
                                  Widget.NO_BUTTON);
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
                wi.setValue(new StringValue(hsSaved));
            } else {
                wi.setValue(prevWi.getValue());
            }
        }

        /* host score combo boxes */
        int rows = 0;
        for (final Host host : getBrowser().getClusterHosts()) {
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
                public void mouseClicked(final MouseEvent e) {
                    /* do nothing */
                }
                @Override
                public void mouseEntered(final MouseEvent e) {
                    /* do nothing */
                }
                @Override
                public void mouseExited(final MouseEvent e) {
                    /* do nothing */
                }
                @Override
                public void mousePressed(final MouseEvent e) {
                    final String currentText = label.getText();
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (currentText.equals(onText)) {
                                label.setText(notOnText);
                            } else {
                                label.setText(onText);
                            }
                            final String[] params = getParametersFromXML();
                            setApplyButtons(CACHED_FIELD, params);
                        }
                    });
                }
                @Override
                public void mouseReleased(final MouseEvent e) {
                    /* do nothing */
                }
            });
            wi.setLabel(label, text);
            addField(panel, label, wi.getComponent(), leftWidth, rightWidth, 0);
            rows++;
        }
        rows += addPingField(panel, leftWidth, rightWidth);

        SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                        1, 1,           /* initX, initY */
                                        1, 1);          /* xPad, yPad */
        optionsPanel.add(panel);
    }

    private int addPingField(final JPanel panel, final int leftWidth, final int rightWidth) {
        final JLabel pingLabel = new JLabel("pingd");
        final Value savedPO;
        final Widget prevWi = pingComboBox;
        if (prevWi == null) {
            savedPO = savedPingOperation;
        } else {
            savedPO = prevWi.getValue();
        }
        final Widget pingWi = WidgetFactory.createInstance(Widget.Type.COMBOBOX,
                                                           savedPO,
                                                           new Value[]{NOTHING_SELECTED_VALUE,
                                                                       PING_ATTRIBUTES.get("defined"),
                                                                       PING_ATTRIBUTES.get("eq0")},
                                                           Widget.NO_REGEXP,
                                                           rightWidth,
                                                           Widget.NO_ABBRV,
                                                           new AccessMode(Application.AccessType.ADMIN, false),
                                                           Widget.NO_BUTTON);
        addField(panel, pingLabel, pingWi.getComponent(), leftWidth, rightWidth, 0);
        pingWi.setLabel(pingLabel, Tools.getString("ServiceInfo.PingdToolTip"));
        if (resourceAgent.isPingService()
            && (savedPingOperation == null || savedPingOperation.isNothingSelected())) {
            pingWi.setEnabled(false);
        }
        pingComboBox = pingWi;
        int rows = 0;
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
        final Map<String, ServiceInfo> services = getBrowser().getHeartbeatIdToServiceInfo();
        for (final ServiceInfo si : services.values()) {
            final String refCRMId = cs.getMetaAttrsRef(si.getService().getCrmId());
            if (refCRMId != null
                && refCRMId.equals(getService().getCrmId())) {
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
    private void setMetaAttrsSameAs(final Value info) {
        Tools.isSwingThread();
        if (sameAsMetaAttrsWi == null || info == null || info.isNothingSelected()) {
            return;
        }
        boolean nothingSelected = false;
        if (info.isNothingSelected()) {
            nothingSelected = true;
        }
        boolean sameAs = true;
        if (META_ATTRS_DEFAULT_VALUES.equals(info)) {
            sameAs = false;
        }
        final String[] params = getParametersFromXML();
        if (params != null) {
            for (final String param : params) {
                if (!isMetaAttr(param)) {
                    continue;
                }
                Value defaultValue = getParamPreferred(param);
                if (defaultValue == null || defaultValue.isNothingSelected()) {
                    defaultValue = getParamDefault(param);
                }
                final Widget wi = getWidget(param, null);
                if (wi == null) {
                    continue;
                }
                Value oldValue = wi.getValue();
                if (oldValue == null || oldValue.isNothingSelected()) {
                    oldValue = defaultValue;
                }
                wi.setEnabled(!sameAs || nothingSelected);
                if (!nothingSelected) {
                    if (sameAs) {
                        /* same as some other service */
                        defaultValue = ((EditableInfo) info).getParamSaved(param);
                    }
                    final Value newValue = defaultValue;
                    if (!Tools.areEqual(oldValue, newValue)) {
                        wi.setValueNoListeners(newValue);
                    }
                }
            }
        }
    }

    /**
     * Returns whether this service's operations are referenced by some
     * other service.
     */
    private boolean isOperationReferenced() {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        getBrowser().mHeartbeatIdToServiceLock();
        final Map<String, ServiceInfo> services = getBrowser().getHeartbeatIdToServiceInfo();
        for (final ServiceInfo si : services.values()) {
            final String refCRMId = cs.getOperationsRef(si.getService().getCrmId());
            if (refCRMId != null && refCRMId.equals(getService().getCrmId())) {
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
        try {
            return savedOperationIdRef;
        } finally {
            mSavedOperationsLock.unlock();
        }
    }

    /**
     * Returns all services except this one, that are of the same type
     * for meta attributes.
     */
    private Value[] getSameServicesMetaAttrs() {
        final List<Value> sl = new ArrayList<Value>();
        sl.add(NOTHING_SELECTED_VALUE);
        sl.add(META_ATTRS_DEFAULT_VALUES);
                              
        final Host dcHost = getBrowser().getDCHost();
        if (isMetaAttrReferenced() || Tools.versionBeforePacemaker(dcHost)) {
            return sl.toArray(new Value[sl.size()]);
        }
        getBrowser().lockNameToServiceInfo();
        final Map<String, ServiceInfo> idToInfoHash = getBrowser().getNameToServiceInfoHash().get(getName());
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (idToInfoHash != null) {
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(idToInfoHash.values())) {
                if (si != this
                    && cs.getMetaAttrsId(si.getService().getCrmId()) != null
                    && cs.getMetaAttrsRef(si.getService().getCrmId()) == null) {
                    sl.add(si);
                }
            }
        }
        final boolean clone = getResourceAgent().isClone();
        for (final String name : getBrowser().getNameToServiceInfoHash().keySet()) {
            final Map<String, ServiceInfo> idToInfo = getBrowser().getNameToServiceInfoHash().get(name);
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(idToInfo.values())) {
                if (si != this
                    && !si.getName().equals(getName())
                    && si.getResourceAgent() != null
                    && si.getResourceAgent().isClone() == clone
                    && cs.getMetaAttrsId(si.getService().getCrmId()) != null
                    && cs.getMetaAttrsRef(si.getService().getCrmId()) == null) {
                    sl.add(si);
                }
            }
        }
        getBrowser().unlockNameToServiceInfo();
        return sl.toArray(new Value[sl.size()]);
    }

    /**
     * Returns all services except this one, that are of the same type
     * for operations.
     */
    private Value[] getSameServicesOperations() {
        final List<Value> sl = new ArrayList<Value>();
        sl.add(NOTHING_SELECTED_VALUE);
        sl.add(OPERATIONS_DEFAULT_VALUES);
        final Host dcHost = getBrowser().getDCHost();
        if (isOperationReferenced() || Tools.versionBeforePacemaker(dcHost)) {
            return sl.toArray(new Value[sl.size()]);
        }
        getBrowser().lockNameToServiceInfo();
        final Map<String, ServiceInfo> idToInfoHash = getBrowser().getNameToServiceInfoHash().get(getName());
        final ClusterStatus cs = getBrowser().getClusterStatus();
        if (idToInfoHash != null) {
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(idToInfoHash.values())) {
                if (si != this
                    && cs.getOperationsId(si.getService().getCrmId()) != null
                    && cs.getOperationsRef(si.getService().getCrmId()) == null) {
                    sl.add(si);
                }
            }
        }
        final boolean clone = getResourceAgent().isClone();
        for (final String name : getBrowser().getNameToServiceInfoHash().keySet()) {
            final Map<String, ServiceInfo> idToInfo = getBrowser().getNameToServiceInfoHash().get(name);
            for (final ServiceInfo si : new TreeSet<ServiceInfo>(idToInfo.values())) {
                if (si != this
                    && si.getResourceAgent() != null
                    && si.getResourceAgent().isClone() == clone
                    && !si.getName().equals(getName())
                    && cs.getOperationsId(si.getService().getCrmId()) != null
                    && cs.getOperationsRef(si.getService().getCrmId()) == null) {
                    sl.add(si);
                }
            }
        }
        getBrowser().unlockNameToServiceInfo();
        return sl.toArray(new Value[sl.size()]);
    }

    /**
     * Sets operations with same values as other service info, or default
     * values.
     */
    private void setOperationsSameAs(final Value info) {
        Tools.isSwingThread();
        if (sameAsOperationsWi == null) {
            return;
        }
        boolean nothingSelected = false;
        if (info == null || info.isNothingSelected()) {
            nothingSelected = true;
        }
        boolean sameAs = true;
        if (info == null || OPERATIONS_DEFAULT_VALUES.equals(info)) {
            sameAs = false;
        }
        mSavedOperationsLock.lock();
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCrmOperationParams(op)) {
                Value defaultValue = resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null || defaultValue.isNothingSelected()) {
                    continue;
                }
                if (ClusterBrowser.CRM_OPERATIONS_WITH_IGNORED_DEFAULT.contains(op)) {
                    defaultValue = NOTHING_SELECTED_VALUE;
                }
                final Widget wi;
                mOperationsComboBoxHashReadLock.lock();
                try {
                    wi = operationsComboBoxHash.get(op, param);
                } finally {
                    mOperationsComboBoxHashReadLock.unlock();
                }
                final Value oldValue = wi.getValue();
                wi.setEnabled(!sameAs || nothingSelected);
                if (!nothingSelected) {
                    if (sameAs) {
                        /* same as some other service */
                        defaultValue = ((ServiceInfo) info).getSavedOperation().get(op, param);
                    }
                    final Value newValue = defaultValue;
                    if (!Tools.areEqual(oldValue, newValue)) {
                        wi.setValueNoListeners(newValue);
                    }
                }
            }
        }
        mSavedOperationsLock.unlock();
    }

    /** Creates operations combo boxes with labels. */
    protected void addOperations(final JPanel optionsPanel, final int leftWidth, final int rightWidth) {
        final JPanel sectionPanel = getParamPanel(Tools.getString("ClusterBrowser.Operations"));
        final Info savedOpIdRef = getSameServiceOpIdRef();
        sameAsOperationsWi = WidgetFactory.createInstance(Widget.Type.COMBOBOX,
                                                          savedOpIdRef,
                                                          getSameServicesOperations(),
                                                          Widget.NO_REGEXP,
                                                          rightWidth,
                                                          Widget.NO_ABBRV,
                                                          new AccessMode(Application.AccessType.ADMIN, false),
                                                          Widget.NO_BUTTON);
        final String toolTip;
        if (savedOpIdRef == null) {
            toolTip = "";
        } else {
            toolTip = savedOpIdRef.getValueForGui();
        }
        sameAsOperationsWi.setToolTipText(toolTip);
        final JLabel label = new JLabel(Tools.getString("ClusterBrowser.SameAs"));
        sameAsOperationsWi.setLabel(label, "");
        final JPanel saPanel = new JPanel(new SpringLayout());
        saPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        addField(saPanel, label, sameAsOperationsWi.getComponent(), leftWidth, rightWidth, 0);
        SpringUtilities.makeCompactGrid(saPanel, 1, 2,
                                        1, 1,  // initX, initY
                                        1, 1); // xPad, yPad
        sectionPanel.add(saPanel);
        mSavedOperationsLock.lock();
        final JPanel normalOpPanel = new JPanel(new SpringLayout());
        normalOpPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final JPanel advancedOpPanel = new JPanel(new SpringLayout());
        advancedOpPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        addToAdvancedList(advancedOpPanel);
        advancedOpPanel.setVisible(Tools.getApplication().isAdvancedMode());
        int advancedRows = 0;
        boolean allAreDefaultValues = true;
        int normalRows = 0;
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCrmOperationParams(op)) {
                Value defaultValue = resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null || defaultValue.isNothingSelected()) {
                    continue;
                }
                if (ClusterBrowser.CRM_OPERATIONS_WITH_IGNORED_DEFAULT.contains(op)) {
                    defaultValue = NOTHING_SELECTED_VALUE;
                }

                // TODO: old style resources
                if (defaultValue == null) {
                    defaultValue = new StringValue("0");
                }
                mOperationsComboBoxHashWriteLock.lock();
                Value savedValue = null;
                try {
                    final Widget prevWi = operationsComboBoxHash.get(op, param);
                    if (prevWi != null) {
                        savedValue = prevWi.getValue();
                    }
                } finally {
                    mOperationsComboBoxHashWriteLock.unlock();
                }
                if (savedValue == null) {
                    savedValue = savedOperation.get(op, param);
                }
                if (!getService().isNew() && (savedValue == null || savedValue.isNothingSelected())) {
                    savedValue = getOpDefaultsDefault(param);
                    if (savedValue ==null) {
                        savedValue = new StringValue("");
                    }
                }
                if (!Tools.areEqual(defaultValue, savedValue)) {
                    allAreDefaultValues = false;
                }
                if (savedValue != null) {
                    defaultValue = savedValue;
                }
                final Widget wi;
                if (CrmXml.PARAM_OCF_CHECK_LEVEL.equals(param)) {
                    wi = WidgetFactory.createInstance(Widget.Type.COMBOBOX,
                                                      defaultValue,
                                                      new Value[]{NOTHING_SELECTED_VALUE,
                                                                  new StringValue("10"),
                                                                  new StringValue("20")},
                                                      "^\\d*$",
                                                      rightWidth,
                                                      Widget.NO_ABBRV,
                                                      new AccessMode(Application.AccessType.ADMIN, false),
                                                      Widget.NO_BUTTON);
                } else {
                    final String regexp = "^-?\\d*$";
                    wi = new TextfieldWithUnit(defaultValue,
                                               getUnits(param),
                                               regexp,
                                               rightWidth,
                                               Widget.NO_ABBRV,
                                               new AccessMode(Application.AccessType.ADMIN, !AccessMode.ADVANCED),
                                               Widget.NO_BUTTON);
                }
                wi.setEnabled(savedOpIdRef == null);

                mOperationsComboBoxHashWriteLock.lock();
                try {
                    operationsComboBoxHash.put(op, param, wi);
                } finally {
                    mOperationsComboBoxHashWriteLock.unlock();
                }
                final String labelText = Tools.ucfirst(op) + " / " + Tools.ucfirst(param);
                final JLabel wiLabel = new JLabel(labelText);
                wi.setLabel(wiLabel, labelText);
                final JPanel panel;
                if (getBrowser().isCrmOperationAdvanced(op, param)) {
                    panel = advancedOpPanel;
                    advancedRows++;
                } else {
                    panel = normalOpPanel;
                    normalRows++;
                }
                addField(panel, wiLabel, wi.getComponent(), leftWidth, rightWidth, 0);
                Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                    @Override
                    public void run() {
                        wiLabel.setToolTipText(labelText);
                    }
                });
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
            sameAsOperationsWi.setValue(OPERATIONS_DEFAULT_VALUES);
        }
        sameAsOperationsWi.addListeners(
                new WidgetListener() {
                    @Override
                    public void check(final Value value) {
                        final String[] params = getParametersFromXML();
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                final Value info = sameAsOperationsWiValue();
                                    setOperationsSameAs(info);
                                    sameAsOperationsWi.setEditable();
                                    if (info == null) {
                                        sameAsOperationsWi.setToolTipText("");
                                    } else {
                                        sameAsOperationsWi.setToolTipText(info.getValueForConfig());
                                    }
                                    setApplyButtons(CACHED_FIELD, params);
                            }
                        });
                }
            });
        optionsPanel.add(sectionPanel);
    }

    @Override
    public String[] getParametersFromXML() {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return getEnabledSectionParams(crmXML.getOcfMetaDataParameters(resourceAgent, getService().isMaster()));
    }

    @Override
    protected String getParamRegexp(final String param) {
        if (isInteger(param)) {
            return "^((-?\\d*|(-|\\+)?" + CrmXml.INFINITY_VALUE
                   + '|' + CrmXml.DISABLED_IN_COMBOBOX
                   + "))|@NOTHING_SELECTED@$";
        }
        return null;
    }

    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        if ("ip".equals(param) && newValue != null && !Tools.isIp(newValue.getValueForConfig())) {
            return false;
        }
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.checkParam(resourceAgent, param, newValue);
    }

    @Override
    public Value getParamDefault(final String param) {
        if (isMetaAttr(param)) {
            final Value paramDefault = getBrowser().getRscDefaultsInfo().getResource().getValue(param);
            if (paramDefault != null && !paramDefault.isNothingSelected()) {
                return paramDefault;
            }
        }
        final CrmXml crmXML = getBrowser().getCrmXml();
        return new StringValue(crmXML.getDefaultValue(resourceAgent, param));
    }

    @Override
    public Value getParamSaved(final String param) {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        if (isMetaAttr(param)) {
            final String crmId = getService().getCrmId();
            final String refCRMId = clStatus.getMetaAttrsRef(crmId);
            if (refCRMId != null) {
                final String v = clStatus.getParameter(refCRMId, param, Application.RunMode.LIVE);
                if (v == null) {
                    final Value value = getParamPreferred(param);
                    if (value == null || value.isNothingSelected()) {
                        return getParamDefault(param);
                    } else {
                        return value;
                    }
                }
                return new StringValue(v);
            }
        }
        final Value value = super.getParamSaved(param);
        if (value == null || value.isNothingSelected()) {
            final String v = clStatus.getParameter(getService().getCrmId(), param, Application.RunMode.LIVE);
            if (v == null) {
                Value vp = null;
                if (getService().isNew()) {
                    vp = getParamPreferred(param);
                }
                
                if (vp == null || vp.isNothingSelected()) {
                    return getParamDefault(param);
                } else {
                    return vp;
                }
            } else {
                return new StringValue(v);
            }
        } else {
            return value;
        }
    }

    @Override
    protected Value getParamPreferred(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return new StringValue(crmXML.getPreferredValue(resourceAgent, param));
    }

    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        if (isCheckBox(param)) {
            return crmXML.getCheckBoxChoices(resourceAgent, param);
        } else {
            final CloneInfo ci = getCloneInfo();
            final boolean ms = ci != null && ci.getService().isMaster();
            return crmXML.getComboBoxChoices(resourceAgent, param, ms);
        }
    }

    @Override
    protected String getParamShortDesc(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.getShortDesc(resourceAgent, param);
    }

    @Override
    protected String getParamLongDesc(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.getLongDesc(resourceAgent, param);
    }

    /**
     * Returns section to which the specified parameter belongs.
     */
    @Override
    protected String getSection(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.getSectionForDisplay(resourceAgent, param);
    }

    @Override
    protected boolean isRequired(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isRequired(resourceAgent, param);
    }

    @Override
    protected boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param), getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isAdvanced(resourceAgent, param);
    }

    @Override
    protected final String isEnabled(final String param) {
        if (GUI_ID.equals(param) && !getResource().isNew()) {
            return "";
        }
        if (isMetaAttr(param)) {
            final Value info = sameAsMetaAttrsWiValue();
            if (info == null) {
                return null;
            }
            boolean nothingSelected = false;
            if (info.isNothingSelected()) {
                nothingSelected = true;
            }
            boolean sameAs = true;
            if (META_ATTRS_DEFAULT_VALUES.equals(info)) {
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

    @Override
    protected final boolean isEnabledOnlyInAdvancedMode(final String param) {
        return false;
    }

    @Override
    protected Application.AccessType getAccessType(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.getAccessType(resourceAgent, param);
    }

    protected boolean isMetaAttr(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isMetaAttr(resourceAgent, param);
    }

    @Override
    protected boolean isInteger(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isIntegerValue(resourceAgent, param);
    }

    @Override
    protected boolean isLabel(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isLabel(resourceAgent, param);
    }

    @Override
    protected boolean isTimeType(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isTimeType(resourceAgent, param);
    }

    @Override
    protected boolean isCheckBox(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isBoolean(resourceAgent, param);
    }

    @Override
    protected String getParamType(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
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
    void addResourceBefore(final Host dcHost, final Application.RunMode runMode) {
        /* Override to add resource before this one. */
    }

    private void changeTypeToClone(final boolean masterSlave) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        final CloneInfo oldCI = getCloneInfo();
        String title = Application.PM_CLONE_SET_NAME;
        if (masterSlave) {
            title = Application.PM_MASTER_SLAVE_SET_NAME;
        }
        final CloneInfo cloneInfo = cloneInfoProvider.get();
        cloneInfo.init(crmXML.getCloneResourceAgent(), title, masterSlave, getBrowser());
        setCloneInfo(cloneInfo);
        cloneInfo.setContainedService(this);
        if (oldCI == null) {
            final Widget prevWi = getWidget(GUI_ID, null);
            if (prevWi != null) {
                cloneInfo.getService().setId(getName() + '_' + prevWi.getStringValue());
            }
            getBrowser().addNameToServiceInfoHash(cloneInfo);
            getBrowser().addToHeartbeatIdList(cloneInfo);
            getBrowser().getCrmGraph().exchangeObjectInTheVertex(cloneInfo, this);
            cloneInfo.setPingComboBox(pingComboBox);
            for (final Map.Entry<HostInfo, Widget> hostInfoWidgetEntry : scoreComboBoxHash.entrySet()) {
                cloneInfo.getScoreComboBoxHash().put(hostInfoWidgetEntry.getKey(), hostInfoWidgetEntry.getValue());
            }
        } else {
            oldCI.removeNodeAndWait();
            cloneInfo.getService().setId(oldCI.getWidget(GUI_ID, null).getStringValue());
            getBrowser().addNameToServiceInfoHash(cloneInfo);
            getBrowser().addToHeartbeatIdList(cloneInfo);
            getBrowser().getCrmGraph().exchangeObjectInTheVertex(cloneInfo, oldCI);
            cleanup();
            oldCI.cleanup();
            cloneInfo.setPingComboBox(oldCI.getPingComboBox());
            for (final HostInfo hi : oldCI.getScoreComboBoxHash().keySet()) {
                cloneInfo.getScoreComboBoxHash().put(hi, oldCI.getScoreComboBoxHash().get(hi));
            }
            getBrowser().removeFromServiceInfoHash(oldCI);
            getBrowser().mHeartbeatIdToServiceLock();
            getBrowser().getHeartbeatIdToServiceInfo().remove(oldCI.getService().getCrmId());
            getBrowser().mHeartbeatIdToServiceUnlock();
            final DefaultMutableTreeNode oldCINode = oldCI.getNode();
            if (oldCINode != null) {
                oldCINode.setUserObject(null); /* would leak without it*/
            }
        }
        cloneInfo.setCloneServicePanel(this);
        resetInfoPanel();
        infoPanel = null;
        getInfoPanel();
    }

    /** Change type to clone or master/slave resource. */
    private void changeTypeToPrimitive() {
        final CloneInfo ci = getCloneInfo();
        if (ci == null) {
            return;
        }
        setCloneInfo(null);
        setPingComboBox(ci.getPingComboBox());
        for (final HostInfo hi : ci.getScoreComboBoxHash().keySet()) {
            scoreComboBoxHash.put(hi, ci.getScoreComboBoxHash().get(hi));
        }
        final DefaultMutableTreeNode node = getNode();
        final DefaultMutableTreeNode ciNode = ci.getNode();
        removeNodeAndWait();
        ci.removeNodeAndWait();
        cleanup();
        ci.cleanup();
        setNode(node);
        getBrowser().getServicesNode().add(node);
        getBrowser().reloadAndWait(getBrowser().getServicesNode(), false);
        getBrowser().getCrmGraph().exchangeObjectInTheVertex(this, ci);
        getBrowser().mHeartbeatIdToServiceLock();
        getBrowser().getHeartbeatIdToServiceInfo().remove(ci.getService().getCrmId());
        getBrowser().mHeartbeatIdToServiceUnlock();
        getBrowser().removeFromServiceInfoHash(ci);
        resetInfoPanel();
        infoPanel = null;
        getInfoPanel();
        getBrowser().reloadAndWait(node, true);
        getBrowser().nodeChangedAndWait(node);
        ciNode.setUserObject(null); /* would leak without it */
    }

    /** Change type to Master, Clone or Primitive. */
    protected final void changeType(final Value value) {
        LOG.debug1("changeType: value: " + value);
        boolean masterSlave0 = false;
        boolean clone0 = false;
        if (MASTER_SLAVE_TYPE_STRING.equals(value)) {
            masterSlave0 = true;
            clone0 = true;
        } else if (CLONE_TYPE_STRING.equals(value)) {
            clone0 = true;
        }
        final boolean clone = clone0;
        final boolean masterSlave = masterSlave0;

        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (clone) {
                    changeTypeToClone(masterSlave);
                } else if (PRIMITIVE_TYPE_STRING.equals(value)) {
                    changeTypeToPrimitive();
                }
            }
        });
    }

    protected void addHostLocationsListeners() {
        final String[] params = getParametersFromXML();
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Widget wi = scoreComboBoxHash.get(hi);
            wi.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Value value) {
                                    setApplyButtons(CACHED_FIELD, params);
                                    Tools.invokeLater(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                wi.setEditable();
                                            }
                                    });
                                }
                                @Override
                                public void checkText(final String text) {
                                    setApplyButtons(CACHED_FIELD, params);
                                    final Value v = wi.getValue();
                                    if (v == null || v.isNothingSelected()) {
                                        wi.setText("");
                                    }
                                }
                            });
        }
        pingComboBox.addListeners(new WidgetListener() {
                                      @Override
                                      public void check(final Value value) {
                                          setApplyButtons(CACHED_FIELD, params);
                                      }
                                  });
    }

    /** Adds listeners for operation and parameter. */
    private void addOperationListeners(final String op, final String param) {
        final Value dv = resourceAgent.getOperationDefault(op, param);
        if (dv == null || dv.isNothingSelected()) {
            return;
        }
        final Widget wi;
        mOperationsComboBoxHashReadLock.lock();
        try {
            wi = operationsComboBoxHash.get(op, param);
        } finally {
            mOperationsComboBoxHashReadLock.unlock();
        }
        final String[] params = getParametersFromXML();
        wi.addListeners(new WidgetListener() {
                            @Override
                            public void check(final Value value) {
                                setApplyButtons(CACHED_FIELD, params);
                            }
                        });
    }

    /**
     * Returns "same as" fields for some sections. Currently only "meta
     * attributes".
     */
    protected final Map<String, Widget> getSameAsFields(final Value savedMAIdRef) {
        sameAsMetaAttrsWi = WidgetFactory.createInstance(Widget.Type.COMBOBOX,
                                                         savedMAIdRef,
                                                         getSameServicesMetaAttrs(),
                                                         Widget.NO_REGEXP,
                                                         ClusterBrowser.SERVICE_FIELD_WIDTH,
                                                         Widget.NO_ABBRV,
                                                         new AccessMode(Application.AccessType.ADMIN, false),
                                                         Widget.NO_BUTTON);
        final String toolTip;
        if (savedMAIdRef == null) {
            toolTip = "";
        } else {
            toolTip = savedMAIdRef.getValueForGui();
        }
        sameAsMetaAttrsWi.setToolTipText(toolTip);
        final Map<String, Widget> sameAsFields = new HashMap<String, Widget>();
        sameAsFields.put(Tools.getString("CRMXML.MetaAttrOptions"), sameAsMetaAttrsWi);
        sameAsMetaAttrsWi.addListeners(new WidgetListener() {
                @Override
                public void check(final Value value) {
                    final String[] params = getParametersFromXML();
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final Value v = sameAsMetaAttrsWiValue();
                            if (v != null) {
                                setMetaAttrsSameAs(v);
                                sameAsMetaAttrsWi.setToolTipText(v.getValueForGui());
                                setApplyButtons(CACHED_FIELD, params);
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

    @Override
    public JComponent getInfoPanel() {
        LOG.debug1("getInfoPanel: " + getName() + ": start");
        if (!getResourceAgent().isMetaDataLoaded()) {
            final JPanel p = new JPanel();
            p.add(new JLabel(Tools.getString("ServiceInfo.LoadingMetaData")));
            return p;
        }
        if (getService().isRemoved()) {
            final JPanel p = new JPanel();
            p.add(new JLabel(IS_BEING_REMOVED_STRING));
            return p;
        }
        if (getBrowser().getCrmXml() == null || getBrowser().getClusterStatus() == null) {
            return new JPanel();
        }
        final CloneInfo ci = getCloneInfo();
        if (ci == null) {
            getBrowser().getCrmGraph().pickInfo(this);
        } else {
            getBrowser().getCrmGraph().pickInfo(ci);
        }
        if (infoPanel != null) {
            LOG.debug1("getInfoPanel: " + getName() + ": cached end");
            return infoPanel;
        }
        /* init save button */
        final boolean abExisted = getApplyButton() != null;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;
            @Override
            public boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                return dcHost != null && !Tools.versionBeforePacemaker(dcHost);
            }
            @Override
            public void mouseOut(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getCrmGraph().stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }

            @Override
            public void mouseOver(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                component.setToolTipText(ClusterBrowser.STARTING_PTEST_TOOLTIP);
                component.setToolTipBackground(Tools.getDefaultColor("ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getCrmGraph().startTestAnimation((JComponent) component, startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                try {
                    final ClusterStatus cs = getBrowser().getClusterStatus();
                    cs.setPtestResult(null);
                    apply(dcHost, Application.RunMode.TEST);
                    final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                    component.setToolTipText(ptestData.getToolTip());
                    cs.setPtestResult(ptestData);
                } finally {
                    getBrowser().ptestLockRelease();
                }
                startTestLatch.countDown();
            }
        };
        if (getResourceAgent().isGroup()) {
            initApplyButton(buttonCallback, Tools.getString("Browser.ApplyGroup"));
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
                        LOG.debug1("getInfoPanel: BUTTON: apply");
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), Application.RunMode.LIVE);
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
                        LOG.debug1("getInfoPanel: BUTTON: revert");
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
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final AbstractButton serviceMenu;
        if (ci == null) {
            serviceMenu = getActionsButton();
        } else {
            serviceMenu = ci.getActionsButton();
        }
        buttonPanel.add(serviceMenu, BorderLayout.LINE_END);
        Value defaultValue = PRIMITIVE_TYPE_STRING;
        if (ci != null) {
            if (ci.getService().isMaster()) {
                defaultValue = MASTER_SLAVE_TYPE_STRING;
            } else {
                defaultValue = CLONE_TYPE_STRING;
            }
        }
        if (!getResourceAgent().isClone() && getGroupInfo() == null) {
            typeRadioGroup = WidgetFactory.createInstance(Widget.Type.RADIOGROUP,
                                                          defaultValue,
                                                          new Value[]{PRIMITIVE_TYPE_STRING,
                                                                      CLONE_TYPE_STRING,
                                                                      MASTER_SLAVE_TYPE_STRING},
                                                          Widget.NO_REGEXP,
                                                          ClusterBrowser.SERVICE_LABEL_WIDTH
                                                          + ClusterBrowser.SERVICE_FIELD_WIDTH,
                                                          Widget.NO_ABBRV,
                                                          new AccessMode(Application.AccessType.ADMIN, false),
                                                          Widget.NO_BUTTON);

            if (!getService().isNew()) {
                typeRadioGroup.setEnabled(false);
            }
            typeRadioGroup.addListeners(new WidgetListener() {
                @Override
                public void check(final Value value) {
                    typeRadioGroup.setEnabled(false);
                    changeType(value);
                }
            });
            final JPanel tp = new JPanel();
            tp.setBackground(ClusterBrowser.PANEL_BACKGROUND);
            tp.setLayout(new BoxLayout(tp, BoxLayout.PAGE_AXIS));
            tp.add(typeRadioGroup.getComponent());
            typeRadioGroup.setBackgroundColor(ClusterBrowser.PANEL_BACKGROUND);
            optionsPanel.add(tp);
        }
        if (ci != null) {
            /* add clone fields */
            addCloneFields(optionsPanel, ClusterBrowser.SERVICE_LABEL_WIDTH, ClusterBrowser.SERVICE_FIELD_WIDTH, ci);
        }
        getResource().setValue(GUI_ID, new StringValue(getService().getId()));

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
            addHostLocations(optionsPanel, ClusterBrowser.SERVICE_LABEL_WIDTH, ClusterBrowser.SERVICE_FIELD_WIDTH);
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
        if (!getResourceAgent().isGroup() && !getResourceAgent().isClone()) {
            /* Operations */
            addOperations(optionsPanel, ClusterBrowser.SERVICE_LABEL_WIDTH, ClusterBrowser.SERVICE_FIELD_WIDTH);
            /* add item listeners to the operations combos */
            for (final String op : getResourceAgent().getOperationNames()) {
                for (final String param : getBrowser().getCrmOperationParams(op)) {
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
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                /* invoke later on purpose  */
                setApplyButtons(null, params);
            }
        });
        mainPanel.add(optionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(ClusterBrowser.SERVICE_LABEL_WIDTH
                                         + ClusterBrowser.SERVICE_FIELD_WIDTH
                                         + 4));
        newPanel.add(new JScrollPane(mainPanel));
        /* if id textfield was changed and this id is not used,
         * enable apply button */
        infoPanel = newPanel;
        infoPanelDone();
        LOG.debug1("getInfoPanel: " + getName() + ": end");
        return infoPanel;
    }

    /** Clears the info panel cache, forcing it to reload. */
    @Override
    public boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Returns operation from host location label. "eq", "ne" etc. */
    private String getOpFromLabel(final CharSequence onHost, final String labelText) {
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
    protected void setLocations(final String heartbeatId, final Host dcHost, final Application.RunMode runMode) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            final Widget wi = scoreComboBoxHash.get(hi);
            String hs = wi.getStringValue();
            if ("ALWAYS".equals(hs)) {
                hs = CrmXml.INFINITY_VALUE.getValueForConfig();
            } else if ("NEVER".equals(hs)) {
                hs = CrmXml.MINUS_INFINITY_VALUE.getValueForConfig();
            }
            final HostLocation hlSaved = savedHostLocations.get(hi);
            String opSaved = null;
            if (hlSaved != null) {
                opSaved = hlSaved.getOperation();
            }
            final String onHost = hi.getName();
            final String op = getOpFromLabel(onHost, wi.getLabel().getText());
            final HostLocation hostLoc = new HostLocation(hs, op, null, null);
            if (!hostLoc.equals(hlSaved)) {
                String locationId = cs.getLocationId(getHeartbeatId(runMode), onHost, runMode);
                if ((hs == null || hs.isEmpty() || !Tools.areEqual(op, opSaved))
                    && locationId != null) {
                    CRM.removeLocation(dcHost, locationId, getHeartbeatId(runMode), runMode);
                    locationId = null;
                }
                if (hs != null && !hs.isEmpty()) {
                    CRM.setLocation(dcHost,
                                    getHeartbeatId(runMode),
                                    onHost,
                                    hostLoc,
                                    locationId,
                                    runMode);
                }
            }
        }
        /* ping */
        final Widget pwi = pingComboBox;
        if (pwi != null) {
            final Value value = pwi.getValue();
            if (!Tools.areEqual(savedPingOperation, value)) {
                final String pingLocationId = cs.getPingLocationId(getHeartbeatId(runMode), runMode);
                if (pingLocationId != null) {
                    CRM.removeLocation(dcHost, pingLocationId, getHeartbeatId(runMode), runMode);
                }
                if (value != null && !value.isNothingSelected()) {
                    CRM.setPingLocation(dcHost,
                                        getHeartbeatId(runMode),
                                        value.getValueForConfig(),
                                        null, /* location id */
                                        runMode);
                }
            }
        }
        if (Application.isLive(runMode)) {
            storeHostLocations();
        }
    }

    /**
     * Returns hash with changed operation ids and all name, value pairs.
     * This works for new heartbeats >= 2.99.0
     */
    protected Map<String, Map<String, String>> getOperations(final String heartbeatId) {
        final Map<String, Map<String, String>> operations = new LinkedHashMap<String, Map<String, String>>();

        final ClusterStatus cs = getBrowser().getClusterStatus();
        final CloneInfo ci = getCloneInfo();
        for (final String op : getResourceAgent().getOperationNames()) {
            final Map<String, String> opHash = new LinkedHashMap<String, String>();
            String opId = cs.getOpId(heartbeatId, op);
            if (opId == null) {
                /* generate one */
                opId = "op-" + heartbeatId + '-' + op;
            }
            /* operations have different kind of default, that is
             * recommended, but not used by default. */
            boolean firstTime = true;
            for (final String param : ClusterBrowser.HB_OPERATION_PARAM_LIST) {
                if (getBrowser().getCrmOperationParams(op).contains(param)) {
                    if (ci == null
                        && (ClusterBrowser.CRM_DEMOTE_OPERATOR.equals(op) || ClusterBrowser.CRM_PROMOTE_OPERATOR.equals(op))) {
                        continue;
                    }
                    final Widget wi;
                    mOperationsComboBoxHashReadLock.lock();
                    try {
                        wi = operationsComboBoxHash.get(op, param);
                    } finally {
                        mOperationsComboBoxHashReadLock.unlock();
                    }
                    final Value value;
                    if (wi == null) {
                        if (CrmXml.PARAM_OCF_CHECK_LEVEL.equals(param)) {
                            value = NOTHING_SELECTED_VALUE;
                        } else {
                            value = new StringValue("0");
                        }
                    } else {
                        value = wi.getValue();
                    }
                    if (value != null && !value.isNothingSelected()) {
                        if (wi != null && firstTime) {
                            opHash.put("id", opId);
                            opHash.put("name", op);
                            firstTime = false;
                            operations.put(op, opHash);
                        }
                        final String sn;
                        if (value.getUnit() != null) {
                            sn = value.getUnit().getShortName();
                        } else {
                            sn = "";
                        }
                        opHash.put(param, value.getValueForConfig() + sn);
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
            final Value i = sameAsMetaAttrsWiValue();
            if (i != null && !i.isNothingSelected() && !META_ATTRS_DEFAULT_VALUES.equals(i)) {
                final ServiceInfo si  = (ServiceInfo) i;
                final ClusterStatus cs = getBrowser().getClusterStatus();
                metaAttrsRefId = cs.getMetaAttrsId(si.getService().getCrmId());
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
            final Value i = sameAsOperationsWiValue();
            if (i != null && !i.isNothingSelected() && !META_ATTRS_DEFAULT_VALUES.equals(i)) {
                final ServiceInfo si  = (ServiceInfo) i;
                final ClusterStatus cs = getBrowser().getClusterStatus();
                operationsRefId = cs.getOperationsId(si.getService().getCrmId());
            }
        }
        return operationsRefId;
    }

    /** Returns attributes of this resource. */
    protected Map<String, String> getPacemakerResAttrs(final Application.RunMode runMode) {
        final Map<String, String> pacemakerResAttrs = new LinkedHashMap<String, String>();
        final String raClass = getService().getResourceClass();
        final String type = getName();
        final String provider = resourceAgent.getProvider();
        final String heartbeatId = getHeartbeatId(runMode);
        LOG.debug1("getPacemakerResAttrs: raClass: "
                   + raClass
                   + ", type: " + type
                   + ", provider: " + provider
                   + ", crm id: " + heartbeatId
                   + ", test: " + runMode);

        pacemakerResAttrs.put("id", heartbeatId);
        pacemakerResAttrs.put("class", raClass);
        if (!ResourceAgent.HEARTBEAT_CLASS_NAME.equals(raClass)
            && !ResourceAgent.SERVICE_CLASSES.contains(raClass)
            && !ResourceAgent.STONITH_CLASS_NAME.equals(raClass)) {
            pacemakerResAttrs.put("provider", provider);
        }
        pacemakerResAttrs.put("type", type);
        return pacemakerResAttrs;
    }

    /** Returns arguments of this resource. */
    protected Map<String, String> getPacemakerResArgs() {
        final Map<String, String> pacemakerResArgs = new LinkedHashMap<String, String>();
        final String[] params = getParametersFromXML();
        for (final String param : params) {
            if (isMetaAttr(param)) {
                continue;
            }
            if (GUI_ID.equals(param) || PCMK_ID.equals(param) || RA_PARAM.equals(param)) {
                continue;
            }
            final Value value = getComboBoxValue(param);
            if (!resourceAgent.isIgnoreDefaults() && Tools.areEqual(value, getParamDefault(param))) {
                continue;
            }
            if (value != null && !value.isNothingSelected()) {
                /* for pacemaker */
                pacemakerResArgs.put(param, value.getValueForConfig());
            }
        }
        return pacemakerResArgs;
    }

    /** Returns meta arguments of this resource. */
    protected Map<String, String> getPacemakerMetaArgs() {
        final Map<String, String> pacemakerMetaArgs = new LinkedHashMap<String, String>();
        final String[] params = getParametersFromXML();
        for (final String param : params) {
            if (!isMetaAttr(param)) {
                continue;
            }
            if (GUI_ID.equals(param)
                || PCMK_ID.equals(param)
                || RA_PARAM.equals(param)) {
                continue;
            }
            final Value value = getComboBoxValue(param);
            if (Tools.areEqual(value, getParamDefault(param))) {
                continue;
            }
            if (value != null && !value.isNothingSelected()) {
                /* for pacemaker */
                pacemakerMetaArgs.put(param, value.getValueForConfig());
            }
        }
        return pacemakerMetaArgs;
    }

    /** Revert all values. */
    @Override
    public void revert() {
        final String[] params = getParametersFromXML();
        boolean sameAs = false;
        if (sameAsMetaAttrsWi != null) {
            boolean allSavedMetaAttrsAreDefaultValues = true;
            for (final String param : params) {
                if (isMetaAttr(param)) {
                    final Value defaultValue = getParamDefault(param);
                    final Value oldValue = getResource().getValue(param);
                    if (!Tools.areEqual(defaultValue, oldValue)) {
                        allSavedMetaAttrsAreDefaultValues = false;
                    }
                }
            }
            if (savedMetaAttrInfoRef == null) {
                if (allSavedMetaAttrsAreDefaultValues) {
                    sameAsMetaAttrsWi.setValue(META_ATTRS_DEFAULT_VALUES);
                } else {
                    sameAsMetaAttrsWi.setValue(null);
                }
            } else {
                sameAs = true;
                sameAsMetaAttrsWi.setValue(savedMetaAttrInfoRef);
            }
        }
        for (final String param : params) {
            if (!sameAs || !isMetaAttr(param)) {
                final Value v = getParamSaved(param);
                final Widget wi = getWidget(param, null);
                if (wi != null && !Tools.areEqual(wi.getValue(), v)) {
                    if (v == null || v.isNothingSelected()) {
                        wi.setValue(null);
                    } else {
                        wi.setValue(v);
                    }
                }
            }
        }
        final GroupInfo gInfo = groupInfo;
        final CloneInfo ci;
        if (gInfo == null) {
            ci = getCloneInfo();
        } else {
            ci = gInfo.getCloneInfo();
        }
        if (ci != null) {
            ci.revert();
        }
        revertOperations();
        revertLocations();
    }

    /** Revert locations to saved values. */
    protected final void revertLocations() {
        for (final Host host : getBrowser().getClusterHosts()) {
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
            wi.setValue(new StringValue(score));
            final JLabel label = wi.getLabel();
            final String text = getHostLocationLabel(hi.getName(), op);
            label.setText(text);
        }
        /* pingd */
        final Widget pwi = pingComboBox;
        if (pwi != null) {
            final Value spo = savedPingOperation;
            if (spo == null || spo.isNothingSelected()) {
                pwi.setValue(null);
            } else {
                pwi.setValue(PING_ATTRIBUTES.get(spo.getValueForConfig()));
            }
        }
    }

    /** Revert to saved operation values. */
    protected final void revertOperations() {
        if (sameAsOperationsWi == null) {
            return;
        }
        mSavedOperationsLock.lock();
        boolean allSavedAreDefaultValues = true;
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCrmOperationParams(op)) {
                Value defaultValue = resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null || defaultValue.isNothingSelected()) {
                    continue;
                }
                if (ClusterBrowser.CRM_OPERATIONS_WITH_IGNORED_DEFAULT.contains(op)) {
                    defaultValue = null;
                }
                final Widget wi;
                mOperationsComboBoxHashReadLock.lock();
                try {
                    wi = operationsComboBoxHash.get(op, param);
                } finally {
                    mOperationsComboBoxHashReadLock.unlock();
                }
                Value value = wi.getValue();
                if (value == null || value.isNothingSelected()) {
                    value = getOpDefaultsDefault(param);
                }
                if (!Tools.areEqual(defaultValue, value)) {
                }
                if (!Tools.areEqual(defaultValue, savedOperation.get(op, param))) {
                    allSavedAreDefaultValues = false;
                }
            }
        }
        final ServiceInfo savedOpIdRef = savedOperationIdRef;
        ServiceInfo operationIdRef = null;
        final Value ref = sameAsOperationsWiValue();
        if (ref instanceof ServiceInfo) {
            operationIdRef = (ServiceInfo) ref;
        }
        boolean sameAs = false;
        if (!Tools.areEqual(operationIdRef, savedOpIdRef)) {
            if (savedOpIdRef == null) {
                if (allSavedAreDefaultValues) {
                    sameAsOperationsWi.setValue(OPERATIONS_DEFAULT_VALUES);
                } else {
                    if (operationIdRef != null) {
                        sameAsOperationsWi.setValue(null);
                    }
                }
            } else {
                sameAs = true;
                sameAsOperationsWi.setValue(savedOpIdRef);
            }
        }
        if (!sameAs) {
            for (final String op : getResourceAgent().getOperationNames()) {
                for (final String param : getBrowser().getCrmOperationParams(op)) {
                    final Value value = savedOperation.get(op, param);
                    final Widget wi;
                    mOperationsComboBoxHashReadLock.lock();
                    try {
                        wi = operationsComboBoxHash.get(op, param);
                    } finally {
                        mOperationsComboBoxHashReadLock.unlock();
                    }
                    if (wi != null) {
                        Tools.invokeLater(new Runnable() {
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
    public void apply(final Host dcHost, final Application.RunMode runMode) {
        LOG.debug1("apply: start: test: " + runMode);
        if (Application.isLive(runMode)) {
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
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        final String[] params = getParametersFromXML();
        final GroupInfo gInfo = groupInfo;
        final CloneInfo clInfo;
        String[] groupParams = null;
        if (gInfo == null) {
            clInfo = getCloneInfo();
        } else {
            clInfo = gInfo.getCloneInfo();
            groupParams = gInfo.getParametersFromXML();
        }
        String cloneId = null;
        String[] cloneParams = null;
        boolean master = false;
        if (clInfo != null) {
            cloneId = clInfo.getHeartbeatId(runMode);
            cloneParams = clInfo.getParametersFromXML();
            master = clInfo.getService().isMaster();
        }
        if (Application.isLive(runMode)) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setToolTipText("");
                    getWidget(GUI_ID, null).setEnabled(false);
                    if (clInfo != null) {
                        clInfo.getWidget(GUI_ID, null).setEnabled(false);
                    }
                }
            });

            /* add myself to the hash with service name and id as
             * keys */
            getBrowser().removeFromServiceInfoHash(this);
            final String oldHeartbeatId = getHeartbeatId(runMode);
            if (oldHeartbeatId != null) {
                getBrowser().mHeartbeatIdToServiceLock();
                getBrowser().getHeartbeatIdToServiceInfo().remove(oldHeartbeatId);
                getBrowser().mHeartbeatIdToServiceUnlock();
            }
            if (getService().isNew()) {
                final String id = getComboBoxValue(GUI_ID).getValueForConfig();
                if (id == null) {
                    LOG.appWarning("apply: id is null: " + getName());
                }
                getService().setIdAndCrmId(id);
                if (clInfo != null) {
                    final String clid = clInfo.getComboBoxValue(GUI_ID).getValueForConfig();
                    if (clid == null) {
                        LOG.appWarning("apply: clone id is null: " + getName());
                    }
                    clInfo.getService().setIdAndCrmId(clid);
                }
                if (typeRadioGroup != null) {
                    typeRadioGroup.setEnabled(false);
                }
            }
            getBrowser().addNameToServiceInfoHash(this);
            getBrowser().addToHeartbeatIdList(this);
        }
        if (Application.isLive(runMode)) {
            addResourceBefore(dcHost, runMode);
        }

        final Map<String, String> cloneMetaArgs = new LinkedHashMap<String, String>();
        final Map<String, String> groupMetaArgs = new LinkedHashMap<String, String>();
        final Map<String, String> pacemakerResAttrs = getPacemakerResAttrs(runMode);
        final Map<String, String> pacemakerResArgs = getPacemakerResArgs();
        final Map<String, String> pacemakerMetaArgs = getPacemakerMetaArgs();
        final String heartbeatId = getHeartbeatId(runMode);

        String groupId = null; /* for pacemaker */
        if (gInfo != null) {
            if (gInfo.getService().isNew()) {
                gInfo.apply(dcHost, runMode);
                return;
            }
            groupId = gInfo.getHeartbeatId(runMode);
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
        final Value i = sameAsOperationsWiValue();
        if (i == null || i.isNothingSelected() || i.equals(OPERATIONS_DEFAULT_VALUES)) {
            savedOperationsId = null;
        } else {
            savedOperationIdRef = (ServiceInfo) i;
            savedOperationsId = ((ServiceInfo) i).getService().getCrmId();
        }
        if (getService().isNew()) {
            if (clInfo != null) {
                for (final String param : cloneParams) {
                    if (GUI_ID.equals(param) || PCMK_ID.equals(param) || RA_PARAM.equals(param)) {
                        continue;
                    }
                    final Value value = clInfo.getComboBoxValue(param);
                    if (Tools.areEqual(value, clInfo.getParamDefault(param))) {
                        continue;
                    }
                    if (!GUI_ID.equals(param) && value != null && !value.isNothingSelected()) {
                        cloneMetaArgs.put(param, value.getValueForConfig());
                    }
                }
            }
            if (gInfo != null) {
                for (final String param : groupParams) {
                    if (GUI_ID.equals(param) || PCMK_ID.equals(param) || RA_PARAM.equals(param)) {
                        continue;
                    }
                    final Value value = gInfo.getComboBoxValue(param);
                    if (Tools.areEqual(value, gInfo.getParamDefault(param))) {
                            continue;
                    }
                    if (!GUI_ID.equals(param) && value != null && !value.isNothingSelected()) {
                        groupMetaArgs.put(param, value.getValueForConfig());
                    }
                }
            }
            String command = "-C";
            if ((gInfo != null && !gInfo.getService().isNew())
                || (clInfo != null && !clInfo.getService().isNew())) {
                command = "-U";
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
                              runMode);
            if (gInfo == null) {
                String hbId = heartbeatId;
                if (clInfo != null) {
                    hbId = clInfo.getHeartbeatId(runMode);
                }
                final List<Map<String, String>> colAttrsList = new ArrayList<Map<String, String>>();
                final List<Map<String, String>> ordAttrsList = new ArrayList<Map<String, String>>();
                final List<String> parentIds = new ArrayList<String>();
                final ServiceInfo infoForDependency;
                if (clInfo == null) {
                    infoForDependency = this;
                } else {
                    infoForDependency = clInfo;
                }
                final Set<ServiceInfo> parents = getBrowser().getCrmGraph().getParents(infoForDependency);
                for (final ServiceInfo parentInfo : parents) {
                    if (parentInfo.isConstraintPlaceholder()) {
                        final Collection<ServiceInfo> with = new TreeSet<ServiceInfo>();
                        with.add(infoForDependency);
                        final Collection<ServiceInfo> withFrom = new TreeSet<ServiceInfo>();
                        final boolean colocation = true;
                        final boolean order = true;
                        ((ConstraintPHInfo) parentInfo).addConstraintWithPlaceholder(with,
                                                                                     withFrom,
                                                                                     colocation,
                                                                                     order,
                                                                                     dcHost,
                                                                                     !parentInfo.getService().isNew(),
                                                                                     runMode);
                    } else {
                        final String parentId = parentInfo.getService().getCrmId();
                        parentIds.add(parentId);
                        final Map<String, String> colAttrs = new LinkedHashMap<String, String>();
                        final Map<String, String> ordAttrs = new LinkedHashMap<String, String>();
                        if (getBrowser().getCrmGraph().isColocation(parentInfo, infoForDependency)) {
                            colAttrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, CrmXml.INFINITY_VALUE.getValueForConfig());
                            if (parentInfo.getService().isMaster()) {
                                colAttrs.put("with-rsc-role", "Master");
                            }
                            colAttrsList.add(colAttrs);
                        } else {
                            colAttrsList.add(null);
                        }
                        if (getBrowser().getCrmGraph().isOrder(parentInfo, infoForDependency)) {
                            ordAttrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, CrmXml.INFINITY_VALUE.getValueForConfig());
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
                                              parentIds.toArray(new String [parentIds.size()]),
                                              colAttrsList,
                                              ordAttrsList,
                                              runMode);
                }
            } else {
                gInfo.resetPopup();
            }
        } else {
            if (clInfo != null) {
                for (final String param : cloneParams) {
                    if (GUI_ID.equals(param) || PCMK_ID.equals(param) || RA_PARAM.equals(param)) {
                        continue;
                    }
                    final Value value = clInfo.getComboBoxValue(param);
                    if (Tools.areEqual(value, clInfo.getParamDefault(param))) {
                            continue;
                    }
                    if (value != null && !value.isNothingSelected()) {
                        cloneMetaArgs.put(param, value.getValueForConfig());
                    }
                }
            }
            if (gInfo != null) {
                for (final String param : groupParams) {
                    if (GUI_ID.equals(param) || PCMK_ID.equals(param) || RA_PARAM.equals(param)) {
                        continue;
                    }
                    final Value value = gInfo.getComboBoxValue(param);
                    if (value == null
                        || Tools.areEqual(value, gInfo.getParamDefault(param))) {
                        continue;
                    }
                    if (!value.isNothingSelected()) {
                        groupMetaArgs.put(param, value.getValueForConfig());
                    }
                }
                cloneId = null;
            }

            groupId = null; /* we don't want to replace the whole group */
            //TODO: should not be called if only host locations have
            //changed.
            final ClusterStatus cs = getBrowser().getClusterStatus();
            CRM.setParameters(dcHost,
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
                              runMode);
            if (isFailed(runMode)) {
                cleanupResource(dcHost, runMode);
            }
        }

        if (gInfo == null) {
            if (clInfo == null) {
                setLocations(heartbeatId, dcHost, runMode);
            } else {
                clInfo.setLocations(heartbeatId, dcHost, runMode);
            }
        } else {
            setLocations(heartbeatId, dcHost, runMode);
        }
        if (Application.isLive(runMode)) {
            storeComboBoxValues(params);
            storeOperations();
            if (clInfo != null) {
                clInfo.storeComboBoxValues(cloneParams);
            }

            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getWidget(PCMK_ID, null).setValueAndWait(getParamSaved(PCMK_ID));
                    if (clInfo != null) {
                        clInfo.getWidget(PCMK_ID, null).setValueAndWait(clInfo.getParamSaved(PCMK_ID));
                    }

                    setApplyButtons(null, params);
                    final DefaultMutableTreeNode node = getNode();
                    if (node != null) {
                        if (clInfo == null) {
                            getBrowser().reloadNode(node, false);
                        } else {
                            getBrowser().reloadNode(clInfo.getNode(), false);
                            getBrowser().reloadNode(node, false);
                        }
                        getBrowser().getCrmGraph().repaint();
                    }
                }
            });
        }
        LOG.debug1("apply: end: test: " + runMode);
    }

    /** Removes order(s). */
    public void removeOrder(final ServiceInfo parent, final Host dcHost, final Application.RunMode runMode) {
        if (getService().isNew() || parent.getService().isNew()) {
            return;
        }
        if (Application.isLive(runMode) && !getService().isNew() && !parent.getService().isNew()) {
            parent.setUpdated(true);
            setUpdated(true);
        }
        final ClusterStatus clStatus = getBrowser().getClusterStatus();

        if (isConstraintPlaceholder() || parent.isConstraintPlaceholder()) {
            final ConstraintPHInfo cphi;
            if (isConstraintPlaceholder()) {
                cphi = (ConstraintPHInfo) this;
            } else {
                cphi = (ConstraintPHInfo) parent;
            }
            final Map<CrmXml.RscSet, Map<String, String>> rscSetsOrdAttrs =
                                                            new LinkedHashMap<CrmXml.RscSet, Map<String, String>>();
            final CrmXml.RscSetConnectionData rdata = cphi.getRscSetConnectionDataOrder();
            /** resource set */
            final String ordId = rdata.getConstraintId();
            String idToRemove;
            if (isConstraintPlaceholder()) {
                idToRemove = parent.getService().getCrmId();
            } else {
                idToRemove = getService().getCrmId();
            }
            CrmXml.RscSet modifiedRscSet = null;
            final List<CrmXml.RscSet> ordRscSets = clStatus.getRscSetsOrd(ordId);
            if (ordRscSets != null) {
                for (final CrmXml.RscSet rscSet : ordRscSets) {
                    if (rscSet.equals(rdata.getRscSet1()) || rscSet.equals(rdata.getRscSet2())) {
                        final List<String> newRscIds = new ArrayList<String>();
                        newRscIds.addAll(rscSet.getRscIds());
                        if (newRscIds.remove(idToRemove)
                            && Application.isLive(runMode)) {
                            modifiedRscSet = rscSet;
                        }
                        if (!newRscIds.isEmpty()) {
                            final CrmXml.RscSet newRscSet = new CrmXml.RscSet(rscSet.getId(),
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
            if (Application.isLive(runMode) && rscSetsOrdAttrs.isEmpty()) {
                cphi.getRscSetConnectionDataOrder().setConstraintId(null);
            }
            final Map<String, String> attrs =
                                        new LinkedHashMap<String, String>();
            final CrmXml.OrderData od = clStatus.getOrderData(ordId);
            if (od != null) {
                final String score = od.getScore();
                attrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, score);
            }
            if (Application.isLive(runMode)) {
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
                          runMode);
        } else {
            final String rscFirstId = parent.getHeartbeatId(runMode);
            final List<CrmXml.OrderData> allData = clStatus.getOrderDatas(rscFirstId);
            if (allData != null) {
                for (final CrmXml.OrderData orderData : allData) {
                    final String orderId = orderData.getId();
                    final String rscThenId = orderData.getRscThen();
                    if (rscThenId.equals(getHeartbeatId(runMode))) {
                        CRM.removeOrder(dcHost, orderId, runMode);
                    }
                }
            }
        }
    }

    /** Returns pacemaker id. */
    final String getHeartbeatId(final Application.RunMode runMode) {
        String heartbeatId = getService().getCrmId();
        if (heartbeatId == null && Application.isTest(runMode)) {
            final String guiId = getComboBoxValue(GUI_ID).getValueForConfig();
            if (guiId == null) {
                LOG.appWarning("getHearbeatId: RA meta-data not loaded: " + getName());
                return null;
            }
            heartbeatId = getService().getCrmIdFromId(guiId);
        }
        return heartbeatId;
    }

    /** Adds order constraint from this service to the child. */
    public void addOrder(final ServiceInfo child, final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode) && !getService().isNew() && !child.getService().isNew()) {
            child.setUpdated(true);
            setUpdated(true);
        }
        if (isConstraintPlaceholder() || child.isConstraintPlaceholder()) {
            if (Application.isLive(runMode)) {
                if (isConstraintPlaceholder() && ((ConstraintPHInfo) this).isReversedColocation()) {
                    ((ConstraintPHInfo) this).reverseOrder();
                } else if (child.isConstraintPlaceholder() && ((ConstraintPHInfo) child).isReversedColocation()) {
                    ((ConstraintPHInfo) child).reverseOrder();
                }
            }
            final ConstraintPHInfo cphi;
            final ServiceInfo withService;
            final Collection<ServiceInfo> withFrom = new TreeSet<ServiceInfo>();
            if (isConstraintPlaceholder()) {
                cphi = (ConstraintPHInfo) this;
                withService = child;
            } else {
                cphi = (ConstraintPHInfo) child;
                withService = this;
                withFrom.add(this);
            }
            final Collection<ServiceInfo> with = new TreeSet<ServiceInfo>();
            with.add(withService);
            cphi.addConstraintWithPlaceholder(with,
                                              withFrom,
                                              false,
                                              true,
                                              dcHost,
                                              !cphi.getService().isNew(),
                                              runMode);
        } else {
            final String childHbId = child.getHeartbeatId(runMode);
            final Map<String, String> attrs = new LinkedHashMap<String, String>();
            attrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, CrmXml.INFINITY_VALUE.getValueForConfig());
            final CloneInfo chCI = child.getCloneInfo();
            if (chCI != null && chCI.getService().isMaster()) {
                attrs.put("first-action", "promote");
                attrs.put("then-action", "start");
            }
            CRM.addOrder(dcHost,
                         null, /* order id */
                         getHeartbeatId(runMode),
                         childHbId,
                         attrs,
                         runMode);
        }
    }

    /** Removes colocation(s). */
    public void removeColocation(final ServiceInfo parent, final Host dcHost, final Application.RunMode runMode) {
        if (getService().isNew() || parent.getService().isNew()) {
            return;
        }
        if (Application.isLive(runMode) && !getService().isNew() && !parent.getService().isNew()) {
            parent.setUpdated(true);
            setUpdated(true);
        }
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final String rscId;
        if (isConstraintPlaceholder()) {
            rscId = getId();
        } else {
            rscId = getHeartbeatId(runMode);
        }
        if (isConstraintPlaceholder() || parent.isConstraintPlaceholder()) {
            final Map<CrmXml.RscSet, Map<String, String>> rscSetsColAttrs =
                                                            new LinkedHashMap<CrmXml.RscSet, Map<String, String>>();
            final ConstraintPHInfo cphi;
            if (isConstraintPlaceholder()) {
                cphi = (ConstraintPHInfo) this;
            } else {
                cphi = (ConstraintPHInfo) parent;
            }
            final CrmXml.RscSetConnectionData rdata = cphi.getRscSetConnectionDataColocation();
            /** resource set */
            final String colId = rdata.getConstraintId();
            final String idToRemove;
            if (isConstraintPlaceholder()) {
                idToRemove = parent.getService().getCrmId();
            } else {
                idToRemove = getService().getCrmId();
            }
            final List<CrmXml.RscSet> colRscSets = clStatus.getRscSetsCol(colId);
            if (colRscSets != null) {
                CrmXml.RscSet modifiedRscSet = null;
                for (final CrmXml.RscSet rscSet : colRscSets) {
                    if (rscSet.equals(rdata.getRscSet1()) || rscSet.equals(rdata.getRscSet2())) {
                        final List<String> newRscIds = new ArrayList<String>();
                        newRscIds.addAll(rscSet.getRscIds());
                        if (newRscIds.remove(idToRemove) && Application.isLive(runMode)) {
                            modifiedRscSet = rscSet;
                        }
                        if (!newRscIds.isEmpty()) {
                            final CrmXml.RscSet newRscSet = new CrmXml.RscSet(rscSet.getId(),
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
            if (Application.isLive(runMode) && rscSetsColAttrs.isEmpty()) {
                cphi.getRscSetConnectionDataColocation().setConstraintId(null);
            }
            final Map<String, String> attrs = new LinkedHashMap<String, String>();
            final CrmXml.ColocationData cd = clStatus.getColocationData(colId);
            if (cd != null) {
                final String score = cd.getScore();
                attrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, score);
            }
            if (Application.isLive(runMode)) {
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
                          runMode);
        } else {
            final List<CrmXml.ColocationData> allData = clStatus.getColocationDatas(rscId);
            if (allData != null) {
                for (final CrmXml.ColocationData colocationData : allData) {
                    final String colId = colocationData.getId();
                    final String withRscId = colocationData.getWithRsc();
                    if (withRscId.equals(parent.getHeartbeatId(runMode))) {
                        CRM.removeColocation(dcHost, colId, runMode);
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
    public void addColocation(final ServiceInfo child, final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode) && !getService().isNew() && !child.getService().isNew()) {
            child.setUpdated(true);
            setUpdated(true);
        }
        if (isConstraintPlaceholder() || child.isConstraintPlaceholder()) {
            if (Application.isLive(runMode)) {
                if (isConstraintPlaceholder() && ((ConstraintPHInfo) this).isReversedOrder()) {
                    ((ConstraintPHInfo) this).reverseColocation();
                } else if (child.isConstraintPlaceholder() && ((ConstraintPHInfo) child).isReversedOrder()) {
                    ((ConstraintPHInfo) child).reverseColocation();
                }
            }
            final ConstraintPHInfo cphi;
            final ServiceInfo withService;
            final Collection<ServiceInfo> withFrom = new TreeSet<ServiceInfo>();
            if (isConstraintPlaceholder()) {
                cphi = (ConstraintPHInfo) this;
                withService = child;
            } else {
                cphi = (ConstraintPHInfo) child;
                withService = this;
                withFrom.add(this);
            }
            final Collection<ServiceInfo> with = new TreeSet<ServiceInfo>();
            with.add(withService);
            cphi.addConstraintWithPlaceholder(with,
                                              withFrom,
                                              true,
                                              false,
                                              dcHost,
                                              !cphi.getService().isNew(),
                                              runMode);
        } else {
            final String childHbId = child.getHeartbeatId(runMode);
            final Map<String, String> attrs = new LinkedHashMap<String, String>();
            attrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, CrmXml.INFINITY_VALUE.getValueForConfig());
            final CloneInfo pCI = child.getCloneInfo();
            if (pCI != null && pCI.getService().isMaster()) {
                attrs.put("with-rsc-role", "Master");
            }
            CRM.addColocation(dcHost,
                              null, /* col id */
                              childHbId,
                              getHeartbeatId(runMode),
                              attrs,
                              runMode);
        }
    }

    /** Returns panel with graph. */
    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getCrmGraph().getGraphPanel();
    }

    /** Adds service panel to the position 'pos'. */
    public ServiceInfo addServicePanel(final ResourceAgent newRA,
                                       final Point2D pos,
                                       final boolean colocation,
                                       final boolean order,
                                       final boolean reloadNode,
                                       final boolean master,
                                       final Application.RunMode runMode) {
        final ServiceInfo newServiceInfo;

        final String name = newRA.getServiceName();
        if (newRA.isFilesystem()) {
            newServiceInfo = filesystemInfoProvider.get();
            newServiceInfo.init(name, newRA, getBrowser());
        } else if (newRA.isLinbitDrbd()) {
            newServiceInfo = linbitDrbdInfoProvider.get();
            newServiceInfo.init(name, newRA, getBrowser());
        } else if (newRA.isDrbddisk()) {
            newServiceInfo = drbddiskInfoProvider.get();
            newServiceInfo.init(name, newRA, getBrowser());
        } else if (newRA.isIPaddr()) {
            newServiceInfo = ipaddrInfoProvider.get();
            newServiceInfo.init(name, newRA, getBrowser());
        } else if (newRA.isVirtualDomain()) {
            newServiceInfo = virtualDomainInfoProvider.get();
            newServiceInfo.init(name, newRA, getBrowser());
        } else if (newRA.isGroup()) {
            newServiceInfo = groupInfoProvider.get();
            newServiceInfo.init(name, newRA, getBrowser());
        } else if (newRA.isClone()) {
            final String cloneName;
            if (master) {
                cloneName = Application.PM_MASTER_SLAVE_SET_NAME;
            } else {
                cloneName = Application.PM_CLONE_SET_NAME;
            }
            final CloneInfo newCloneInfo = cloneInfoProvider.get();
            newCloneInfo.init(newRA, cloneName, master, getBrowser());
            newServiceInfo = newCloneInfo;
        } else {
            newServiceInfo = serviceInfoProvider.get();
            newServiceInfo.init(name, newRA, getBrowser());
        }

        addServicePanel(newServiceInfo,
                        pos,
                        colocation,
                        order,
                        reloadNode,
                        getBrowser().getDCHost(),
                        runMode);
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
                                final Application.RunMode runMode) {
        final ResourceAgent ra = serviceInfo.getResourceAgent();
        if (ra != null) {
            serviceInfo.getService().setResourceClass(ra.getResourceClass());
        }
        if (getBrowser().getCrmGraph().addResource(serviceInfo, this, pos, colocation, order, runMode)) {
            Tools.waitForSwing();
            /* edge added */
            if (isConstraintPlaceholder() || serviceInfo.isConstraintPlaceholder()) {
                final ConstraintPHInfo cphi;
                final ServiceInfo withService;
                final Collection<ServiceInfo> withFrom = new TreeSet<ServiceInfo>();
                if (isConstraintPlaceholder()) {
                    cphi = (ConstraintPHInfo) this;
                    withService = serviceInfo;
                } else {
                    cphi = (ConstraintPHInfo) serviceInfo;
                    withService = this;
                    withFrom.add(this);
                }
                withFrom.addAll(getBrowser().getCrmGraph().getParents(cphi));
                final Collection<ServiceInfo> with = new TreeSet<ServiceInfo>();
                with.add(withService);
                cphi.addConstraintWithPlaceholder(with,
                                                  withFrom,
                                                  colocation,
                                                  order,
                                                  dcHost,
                                                  !cphi.getService().isNew(),
                                                  runMode);
                if (Application.isLive(runMode)) {
                    final PcmkRscSetsInfo prsi = cphi.getPcmkRscSetsInfo();
                    prsi.setApplyButtons(null, prsi.getParametersFromXML());
                }
            } else {
                final String parentId = getHeartbeatId(runMode);
                final String heartbeatId = serviceInfo.getHeartbeatId(runMode);
                final List<Map<String, String>> colAttrsList = new ArrayList<Map<String, String>>();
                final List<Map<String, String>> ordAttrsList = new ArrayList<Map<String, String>>();
                final Map<String, String> colAttrs = new LinkedHashMap<String, String>();
                final Map<String, String> ordAttrs = new LinkedHashMap<String, String>();
                colAttrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, CrmXml.INFINITY_VALUE.getValueForConfig());
                ordAttrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, CrmXml.INFINITY_VALUE.getValueForConfig());
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
                                              runMode);
                }
            }
        } else {
            getBrowser().addNameToServiceInfoHash(serviceInfo);
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final DefaultMutableTreeNode newServiceNode = new DefaultMutableTreeNode(serviceInfo);
                    serviceInfo.setNode(newServiceNode);

                    getBrowser().getServicesNode().add(newServiceNode);
                    if (reloadNode) {
                        getBrowser().reloadAndWait(getBrowser().getServicesNode(), false);
                        getBrowser().reloadAndWait(newServiceNode, false);
                    }
                }
            });
            getBrowser().reloadAllComboBoxes(serviceInfo);
        }
        if (reloadNode && ra != null && serviceInfo.getResource().isNew()) {
            if (ra.isProbablyMasterSlave()) {
                serviceInfo.changeType(MASTER_SLAVE_TYPE_STRING);
            } else if (ra.isProbablyClone()) {
                serviceInfo.changeType(CLONE_TYPE_STRING);
            }
        }
        getBrowser().getCrmGraph().reloadServiceMenus();
        if (reloadNode) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getBrowser().getCrmGraph().scale();
                }
            });
        }
    }

    public Service getService() {
        return (Service) getResource();
    }

    /** Starts resource in crm. */
    void startResource(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        CRM.startResource(dcHost, getHeartbeatId(runMode), runMode);
    }

    /** Stops resource in crm. */
    void stopResource(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        CRM.stopResource(dcHost, getHeartbeatId(runMode), runMode);
    }

    /** Puts a resource up in a group. */
    void upResource(final Host dcHost, final Application.RunMode runMode) {
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
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e = giNode.children();
            final List<String> newOrder = new ArrayList<String>();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode n = e.nextElement();
                final ServiceInfo child = (ServiceInfo) n.getUserObject();
                newOrder.add(child.getHeartbeatId(runMode));
            }
            final String el = newOrder.remove(index);
            newOrder.add(index - 1,  el);
            if (Application.isLive(runMode)) {
                setUpdated(true);
            }
            gi.applyWhole(dcHost, false, newOrder, runMode);
        }
    }

    /** Puts a resource down in a group. */
    void downResource(final Host dcHost, final Application.RunMode runMode) {
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
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e = giNode.children();
            final List<String> newOrder = new ArrayList<String>();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode n = e.nextElement();
                final ServiceInfo child = (ServiceInfo) n.getUserObject();
                newOrder.add(child.getHeartbeatId(runMode));
            }
            final String el = newOrder.remove(index);
            newOrder.add(index + 1,  el);
            if (Application.isLive(runMode)) {
                setUpdated(true);
            }
            gi.applyWhole(dcHost, false, newOrder, runMode);
        }
    }

    /** Migrates resource in cluster from current location. */
    void migrateResource(final String onHost, final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        CRM.migrateResource(dcHost, getHeartbeatId(runMode), onHost, runMode);
    }

    /** Migrates resource in heartbeat from current location. */
    void migrateFromResource(final Host dcHost, final String fromHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        /* don't need fromHost, but m/s resources need it. */
        CRM.migrateFromResource(dcHost, getHeartbeatId(runMode), runMode);
    }

    /**
     * Migrates resource in cluster from current location with --force option.
     */
    void forceMigrateResource(final String onHost, final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        CRM.forceMigrateResource(dcHost, getHeartbeatId(runMode), onHost, runMode);
    }

    /** Removes constraints created by resource migrate command. */
    void unmigrateResource(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        CRM.unmigrateResource(dcHost, getHeartbeatId(runMode), runMode);
    }

    void cleanupResource(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final String rscId = getHeartbeatId(runMode);
        boolean failedClone = false;
        for (final Host host : getBrowser().getClusterHosts()) {
            final Set<String> failedClones = cs.getFailedClones(host.getName(), rscId, runMode);
            if (failedClones == null) {
                continue;
            }
            failedClone = true;
            for (final String fc : failedClones) {
                CRM.cleanupResource(dcHost, rscId + ':' + fc, new Host[]{host}, runMode);
            }
        }
        if (!failedClone) {
            final List<Host> dirtyHosts = new ArrayList<Host>();
            for (final Host host : getBrowser().getClusterHosts()) {
                if (isInLRMOnHost(host.getName(), runMode) || getFailCount(host.getName(), runMode) != null) {
                    dirtyHosts.add(host);
                }
            }
            if (!dirtyHosts.isEmpty()) {
                CRM.cleanupResource(dcHost, rscId, dirtyHosts.toArray(new Host[dirtyHosts.size()]), runMode);
            }
        }
    }

    /** Removes the service without confirmation dialog. */
    protected void removeMyselfNoConfirm(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            if (!getService().isNew()) {
                setUpdated(true);
            }
            getService().setRemoved(true);
            cleanup();
        }
        final CloneInfo ci = getCloneInfo();
        if (ci != null) {
            ci.removeMyselfNoConfirm(dcHost, runMode);
            setCloneInfo(null);
        }
        final GroupInfo gi = groupInfo;
        if (getService().isNew() && gi == null) {
            if (Application.isLive(runMode)) {
                getService().setNew(false);
                getBrowser().getCrmGraph().killRemovedVertices();
            }
        } else {
            final ClusterStatus cs = getBrowser().getClusterStatus();
            if (gi == null) {
                removeConstraints(dcHost, runMode);
            }
            if (!getResourceAgent().isGroup()
                && !getResourceAgent().isClone()) {
                String groupId = null; /* for pacemaker */
                if (gi != null) {
                    /* get group id only if there is only one resource in a
                     * group.
                     */
                    if (getService().isNew()) {
                        if (Application.isLive(runMode)) {
                            super.removeMyself(Application.RunMode.LIVE);
                        }
                    } else {
                        final String group = gi.getHeartbeatId(runMode);
                        final DefaultMutableTreeNode giNode = gi.getNode();
                        if (giNode != null) {
                            @SuppressWarnings("unchecked")
                            final Enumeration<DefaultMutableTreeNode> e = giNode.children();
                            while (e.hasMoreElements()) {
                                final DefaultMutableTreeNode n = e.nextElement();
                                final ServiceInfo child = (ServiceInfo) n.getUserObject();
                                child.getService().setModified(true);
                                child.getService().doneModifying();
                            }
                        }
                        if (cs.getGroupResources(group, runMode).size() == 1) {
                            if (Application.isLive(runMode)) {
                                gi.getService().setRemoved(true);
                            }
                            gi.removeMyselfNoConfirmFromChild(dcHost, runMode);
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
                        cloneId = ci.getHeartbeatId(runMode);
                        master = ci.getService().isMaster();
                    }
                    final boolean ret = CRM.removeResource(dcHost,
                                                           getHeartbeatId(runMode),
                                                           groupId,
                                                           cloneId,
                                                           master,
                                                           runMode);
                    cleanupResource(dcHost, runMode);
                    setUpdated(false); /* must be here, is not a clone anymore*/
                    if (!ret && Application.isLive(runMode)) {
                        guiData.progressIndicatorFailed(dcHost.getName(), "removing failed");
                    }
                }
            }
        }
        if (Application.isLive(runMode)) {
            getBrowser().removeFromServiceInfoHash(this);
            infoPanel = null;
            getService().doneRemoving();
        }
    }

    /** Removes this service from the crm with confirmation dialog. */
    @Override
    public void removeMyself(final Application.RunMode runMode) {
        if (getService().isNew()) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), runMode);
            getService().setNew(false);
            getService().doneRemoving();
            return;
        }
        String desc = Tools.getString("ClusterBrowser.confirmRemoveService.Description");

        desc  = desc.replaceAll("@SERVICE@", Matcher.quoteReplacement(toString()));
        if (Tools.confirmDialog(Tools.getString("ClusterBrowser.confirmRemoveService.Title"),
                                desc,
                                Tools.getString("ClusterBrowser.confirmRemoveService.Yes"),
                                Tools.getString("ClusterBrowser.confirmRemoveService.No"))) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), runMode);
            removeInfo();
            getService().setNew(false);
        }
    }

    /** Removes the service from some global hashes and lists. */
    public void removeInfo() {
        getBrowser().mHeartbeatIdToServiceLock();
        getBrowser().getHeartbeatIdToServiceInfo().remove(getService().getCrmId());
        getBrowser().mHeartbeatIdToServiceUnlock();
        getBrowser().removeFromServiceInfoHash(this);
        final CloneInfo ci = cloneInfo;
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                removeNodeAndWait();
                if (ci != null) {
                    ci.removeNodeAndWait();
                }
            }
        });
        super.removeMyself(Application.RunMode.LIVE);
    }

    /** Sets this service as part of a group. */
    public void setGroupInfo(final GroupInfo groupInfo) {
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
    public CloneInfo getCloneInfo() {
        return cloneInfo;
    }

    /**
     * Returns list of items for service popup menu with actions that can
     * be executed on the heartbeat services.
     */
    @Override
    public List<UpdatableItem> createPopup() {
        return serviceMenu.getPulldownMenu(this);
    }

    @Override
    public String getToolTipText(final Application.RunMode runMode) {
        String nodeString = null;
        final List<String> nodes = getRunningOnNodes(runMode);
        if (nodes != null && !nodes.isEmpty()) {
            nodeString = Tools.join(", ", nodes.toArray(new String[nodes.size()]));
        }
        if (getBrowser().allHostsWithoutClusterStatus()) {
            nodeString = "unknown";
        }
        final StringBuilder sb = new StringBuilder(200);
        sb.append("<b>");
        sb.append(this);
        final String textOn;
        final String textNotOn;
        if (getResourceAgent().isFilesystem()) {
            textOn = Tools.getString("ServiceInfo.Filesystem.MoutedOn");
            textNotOn = Tools.getString("ServiceInfo.Filesystem.NotMounted");
        } else {
            textOn = Tools.getString("ServiceInfo.Filesystem.RunningOn");
            textNotOn = Tools.getString("ServiceInfo.Filesystem.NotRunning");
        }
        if (isFailed(runMode)) {
            sb.append("</b> <b>Failed</b>");
        } else if (isStopped(runMode) || nodeString == null) {
            sb.append("</b> ").append(textNotOn);
        } else {
            sb.append("</b> ").append(textOn).append(": ");
            sb.append(nodeString);
        }
        if (!isManaged(runMode)) {
            sb.append(" (unmanaged)");
        }
        final Map<String, String> scores =
                               getBrowser().getClusterStatus().getAllocationScores(getHeartbeatId(runMode), runMode);
        for (final Map.Entry<String, String> scoreEntry : scores.entrySet()) {
            sb.append("<br>allocation score on ");
            sb.append(scoreEntry.getKey());
            sb.append(": ");
            sb.append(scoreEntry.getValue());
        }
        return sb.toString();
    }

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
            getBrowser().getCrmGraph().startAnimation(this);
        } else if (!updated) {
            getBrowser().getCrmGraph().stopAnimation(this);
        }
        super.setUpdated(updated);
    }

    public ColorText getRightCornerTextForGraph(final Application.RunMode runMode) {
        if (getService().isOrphaned()) {
            if (isFailed(runMode)) {
                return ORPHANED_FAILED_COLOR_TEXT;
            } else {
                return ORPHANED_COLOR_TEXT;
            }
        } else if (!isManaged(runMode)) {
            return UNMANAGED_COLOR_TEXT;
        } else if (isFenced(runMode) != null) {
            return FENCED_COLOR_TEXT;
        } else if (getMigratedTo(runMode) != null
                   || getMigratedFrom(runMode) != null) {
            return MIGRATED_COLOR_TEXT;
        }
        return null;
    }

    /** Returns text with lines as array that appears in the cluster graph. */
    public ColorText[] getSubtextsForGraph(final Application.RunMode runMode) {
        final List<ColorText> texts = new ArrayList<ColorText>();
        final String textOn;
        final String textNotOn;
        if (getResourceAgent().isFilesystem()) {
            textOn = Tools.getString("ServiceInfo.Filesystem.MoutedOn");
            textNotOn = Tools.getString("ServiceInfo.Filesystem.NotMounted");
        } else {
            textOn = Tools.getString("ServiceInfo.Filesystem.RunningOn");
            textNotOn = Tools.getString("ServiceInfo.Filesystem.NotRunning");
        }
        if (getService().isOrphaned()) {
            texts.add(new ColorText("...", null, Color.BLACK));
        } else if (getResource().isNew()) {
            texts.add(new ColorText(textNotOn + " (new)", ClusterBrowser.SERVICE_STOPPED_FILL_PAINT, Color.BLACK));
        } else if (isFailed(runMode)) {
            texts.add(new ColorText(textNotOn, null, Color.BLACK));
        } else if (isStopped(runMode) && !isRunning(runMode)) {
            texts.add(new ColorText("stopped", ClusterBrowser.SERVICE_STOPPED_FILL_PAINT, Color.BLACK));
        } else {
            Color textColor = Color.BLACK;
            String runningOnNodeString = null;
            Color color = null;
            if (getBrowser().allHostsWithoutClusterStatus()) {
                runningOnNodeString = "unknown";
            } else {
                final List<String> runningOnNodes = getRunningOnNodes(runMode);
                if (runningOnNodes != null && !runningOnNodes.isEmpty()) {
                    runningOnNodeString = runningOnNodes.get(0);
                    if (resourceAgent.isPingService()
                        && "0".equals(getPingCount(runningOnNodeString, runMode))) {
                        color = Color.RED;
                        textColor = Color.WHITE;
                    } else {
                        color = getBrowser().getCluster().getHostColorsInGraphs(runningOnNodes).get(0);
                    }
                }
            }
            if (runningOnNodeString == null) {
                texts.add(new ColorText(textNotOn, ClusterBrowser.SERVICE_STOPPED_FILL_PAINT, textColor));
            } else {
                texts.add(new ColorText(textOn + ": " + runningOnNodeString
                                      + getPingCountString(runningOnNodeString, runMode),
                                      color,
                                      textColor));
            }
        }
        if (isOneFailedCount(runMode)) {
            for (final Host host : getBrowser().getClusterHosts()) {
                if (host.isCrmStatusOk() && getFailCount(host.getName(), runMode) != null) {
                    texts.add(new ColorText(ClusterBrowser.IDENT_4
                                          + host.getName()
                                          + getFailCountString(host.getName(), runMode),
                                          null,
                                          Color.BLACK));
                }
            }
        }
        return texts.toArray(new ColorText[texts.size()]);
    }

    public ServiceInfo getContainedService() {
        return null;
    }

    Widget getTypeRadioGroup() {
        return typeRadioGroup;
    }

    @Override
    protected final Unit[] getUnits(final String param) {
        return CrmXml.getUnits();
    }

    protected boolean isSlaveOnAllNodes(final Application.RunMode runMode) {
        return false;
    }

    public String getIconTextForGraph(final Application.RunMode runMode) {
        if (getBrowser().allHostsWithoutClusterStatus()) {
            return Tools.getString("ClusterBrowser.Hb.NoInfoAvailable");
        }
        if (getService().isNew()) {
            return "new...";
        } else if (getService().isOrphaned()) {
            return "";
        } else if (isEnslaved(runMode)) {
            if (isSlaveOnAllNodes(runMode)) {
                return "";
            } else {
                return Tools.getString("ClusterBrowser.Hb.Enslaving");
            }
        } else if (isStarted(runMode)) {
            if (isRunning(runMode)) {
                final List<Host> migratedTo = getMigratedTo(runMode);
                if (migratedTo == null) {
                    final List<Host> migratedFrom = getMigratedFrom(runMode);
                    if (migratedFrom != null) {
                        final List<String> runningOnNodes = getRunningOnNodes(runMode);
                        boolean alreadyThere;
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
                            return Tools.getString("ClusterBrowser.Hb.Migrating");
                        }
                    }
                } else {
                    final List<String> runningOnNodes = getRunningOnNodes(runMode);
                    if (runningOnNodes != null) {
                        boolean alreadyThere = false;
                        for (final Host mto : migratedTo) {
                            if (runningOnNodes.contains(mto.getName())) {
                                alreadyThere = true;
                            }
                            if (!alreadyThere) {
                                return Tools.getString("ClusterBrowser.Hb.Migrating");
                            }
                        }
                    }
                }
                return null;
            } else if (isFailed(runMode)) {
                return Tools.getString("ClusterBrowser.Hb.StartingFailed");
            } else if (isGroupStopped(runMode)) {
                return Tools.getString("ClusterBrowser.Hb.GroupStopped");
            } else {
                return Tools.getString("ClusterBrowser.Hb.Starting");
            }
        } else if (isStopped(runMode)) {
            if (isRunning(runMode)) {
                return Tools.getString("ClusterBrowser.Hb.Stopping");
            } else {
                return null;
            }
        }
        return null;
    }

    /** Returns hash with saved operations. */
    MultiKeyMap<String, Value> getSavedOperation() {
        return savedOperation;
    }

    @Override
    public void reloadComboBoxes() {
        if (sameAsOperationsWi != null) {
            final Value savedOpIdRef = sameAsOperationsWiValue();
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    sameAsOperationsWi.reloadComboBox(savedOpIdRef, getSameServicesOperations());
                }
            });
        }
        if (sameAsMetaAttrsWi != null) {
            final Value savedMAIdRef = sameAsMetaAttrsWiValue();
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    sameAsMetaAttrsWi.reloadComboBox(savedMAIdRef, getSameServicesMetaAttrs());
                }
            });
        }
    }

    /** Returns whether info panel is already created. */
    boolean isInfoPanelOk() {
        return infoPanel != null;
    }

    /** Connects with DomainInfo object. */
    public DomainInfo connectWithVMS() {
        return null;
    }

    /** Whether this class is a constraint placeholder. */
    public boolean isConstraintPlaceholder() {
        return false;
    }

    void removeConstraints(final Host dcHost, final Application.RunMode runMode) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final HbConnectionInfo[] hbcis = getBrowser().getCrmGraph().getHbConnections(this);
        for (final HbConnectionInfo hbci : hbcis) {
            if (hbci != null) {
                getBrowser().getCrmGraph().removeOrder(hbci, dcHost, runMode);
                getBrowser().getCrmGraph().removeColocation(hbci, dcHost, runMode);
            }
        }

        for (final String locId : cs.getLocationIds(getHeartbeatId(runMode), runMode)) {
            CRM.removeLocation(dcHost, locId, getHeartbeatId(runMode), runMode);
        }
    }

    /**
     * Returns value of the same as drop down menu as an info object or null.
     */
    final Value sameAsOperationsWiValue() {
        if (sameAsOperationsWi == null) {
            return null;
        }
        return sameAsOperationsWi.getValue();
    }

    /**
     * Returns value of the same as drop down menu as an info object or null.
     */
    final Value sameAsMetaAttrsWiValue() {
        if (sameAsMetaAttrsWi == null) {
            return null;
        }
        return sameAsMetaAttrsWi.getValue();
    }

    final void storeOperations() {
        mSavedOperationsLock.lock();
        for (final String op : getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCrmOperationParams(op)) {
                final Value defaultValue = resourceAgent.getOperationDefault(op, param);
                if (defaultValue == null || defaultValue.isNothingSelected()) {
                    continue;
                }
                final Widget wi;
                mOperationsComboBoxHashReadLock.lock();
                try {
                    wi = operationsComboBoxHash.get(op, param);
                } finally {
                    mOperationsComboBoxHashReadLock.unlock();
                }
                savedOperation.put(op, param, wi.getValue());
            }
        }
        mSavedOperationsLock.unlock();
    }

    protected final Map<HostInfo, Widget> getScoreComboBoxHash() {
        return scoreComboBoxHash;
    }

    public final Widget getPingComboBox() {
        return pingComboBox;
    }

    protected final void setPingComboBox(final Widget pingComboBox) {
        this.pingComboBox = pingComboBox;
    }

    public final Widget getOperationsComboBox(final String op, final String param) {
        mOperationsComboBoxHashReadLock.lock();
        try {
            return operationsComboBoxHash.get(op, param);
        } finally {
            mOperationsComboBoxHashReadLock.unlock();
        }
    }

    public final Widget getSameAsOperationsWi() {
        return sameAsOperationsWi;
    }
}
