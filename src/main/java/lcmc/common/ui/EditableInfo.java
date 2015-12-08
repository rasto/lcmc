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
package lcmc.common.ui;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Label;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.ResourceValue;
import lcmc.common.domain.Unit;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.WidgetListener;
import lcmc.crm.domain.CrmXml;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import org.apache.commons.collections15.map.MultiKeyMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides textfields, combo boxes etc. for editable info objects.
 */
@Named
public abstract class EditableInfo extends Info {
    private static final Logger LOG = LoggerFactory.getLogger(EditableInfo.class);
    /** Whether is's a wizard element. */
    public static final boolean WIZARD = true;
    /** Hash from parameter to boolean value if the last entered value was correct. */
    private final Map<String, Boolean> paramCorrectValueMap = new ConcurrentHashMap<String, Boolean>();
    private final Table<String, String, JPanel> sectionPanels = HashBasedTable.create();
    /** Old apply button, is used for wizards. */
    private MyButton oldApplyButton = null;
    private MyButton applyButton;
    private MyButton revertButton;
    /** Is counted down, first time the info panel is initialized. */
    private CountDownLatch infoPanelLatch = new CountDownLatch(1);
    private final Collection<JPanel> advancedPanelList = new ArrayList<JPanel>();
    private final Collection<String> advancedOnlySectionList = new ArrayList<String>();
    private final JPanel moreOptionsPanel = new JPanel();
    /** Whether dialog was started. It disables the apply button. */
    private boolean dialogStarted = false;
    /** Disabled section, not visible. */
    private final Collection<String> disabledSections = new HashSet<String>();
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private WidgetFactory widgetFactory;
    @Inject
    private Access access;

    private ResourceValue resource;

    public void einit(final ResourceValue resource, final String name, final Browser browser) {
        super.init(name, browser);
        this.resource = resource;
    }

    protected abstract String getSection(String param);

    protected abstract boolean isRequired(String param);

    protected abstract boolean isAdvanced(String param);

    /** Returns null this parameter should be enabled. Otherwise return
    a reason that appears in the tooltip. */
    protected abstract String isEnabled(String param);

    protected abstract AccessMode.Type getAccessType(String param);

    protected abstract AccessMode.Mode isEnabledOnlyInAdvancedMode(String param);

    protected abstract boolean isLabel(String param);

    protected abstract boolean isInteger(String param);

    protected abstract boolean isTimeType(String param);

    protected boolean hasUnitPrefix(final String param) {
        return false;
    }

    protected abstract boolean isCheckBox(String param);

    protected Widget.Type getFieldType(final String param) {
        return null;
    }

    protected abstract String getParamType(final String param);

    protected String getParamRegexp(final String param) {
        // TODO: this should be only for Pacemaker
        if (isInteger(param)) {
            return "^((-?\\d*|(-|\\+)?" + CrmXml.INFINITY_VALUE
                   + '|' + CrmXml.DISABLED_IN_COMBOBOX
                   + "))|@NOTHING_SELECTED@$";
        }
        return null;
    }

    /** Returns the possible choices for pull down menus if applicable. */
    protected abstract Value[] getParamPossibleChoices(String param);

    /** Returns array of all parameters. */
    public abstract String[] getParametersFromXML(); // TODO: no XML

    /** How much of the info is used. */
    public int getUsed() {
        return -1;
    }

    protected final void initApplyButton(final ButtonCallback buttonCallback) {
        initApplyButton(buttonCallback, Tools.getString("Browser.ApplyResource"));
    }

    protected final void initCommitButton(final ButtonCallback buttonCallback) {
        initApplyButton(buttonCallback, Tools.getString("Browser.CommitResources"));
    }

    /** Inits apply or commit button button. */
    protected final void initApplyButton(final ButtonCallback buttonCallback, final String text) {
        if (oldApplyButton == null) {
            applyButton = widgetFactory.createButton(text, Browser.APPLY_ICON);
            application.makeMiniButton(applyButton);
            applyButton.setEnabled(false);
            oldApplyButton = applyButton;
            revertButton = widgetFactory.createButton(Tools.getString("Browser.RevertResource"), Browser.REVERT_ICON);
            revertButton.setEnabled(false);
            revertButton.setToolTipText(Tools.getString("Browser.RevertResource.ToolTip"));
            application.makeMiniButton(revertButton);
            revertButton.setPreferredSize(new Dimension(65, 50));
        } else {
            applyButton = oldApplyButton;
        }
        applyButton.setEnabled(false);
        if (buttonCallback != null) {
            addMouseOverListener(applyButton, buttonCallback);
        }
    }

    /** Creates apply button and adds it to the panel. */
    protected final void addApplyButton(final JPanel panel) {
        panel.add(applyButton, BorderLayout.LINE_START);
    }

    /** Creates revert button and adds it to the panel. */
    protected final void addRevertButton(final JPanel panel) {
        final JPanel p = new JPanel(new FlowLayout(FlowLayout.TRAILING, 4, 0));
        p.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
        p.add(revertButton);
        panel.add(p, BorderLayout.CENTER);
    }

    /** Adds jlabel field with tooltip. */
    public final void addLabelField(final JPanel panel,
                                    final String left,
                                    final String right,
                                    final int leftWidth,
                                    final int rightWidth,
                                    final int height) {
        final JLabel leftLabel = new JLabel(left);
        leftLabel.setToolTipText(left);
        final JLabel rightLabel = new JLabel(right);
        rightLabel.setToolTipText(right);
        addField(panel, leftLabel, rightLabel, leftWidth, rightWidth, height);
    }

    /**
     * Adds field with left and right component to the panel. Use panel
     * with spring layout for this.
     */
    public final void addField(final JPanel panel,
                               final java.awt.Component left,
                               final java.awt.Component right,
                               final int leftWidth,
                               final int rightWidth,
                               int height) {
        /* right component with fixed width. */
        if (height == 0) {
            height = application.getDefaultSize("Browser.FieldHeight");
        }
        Tools.setSize(left, leftWidth, height);
        panel.add(left);
        Tools.setSize(right, rightWidth, height);
        panel.add(right);
        right.setBackground(panel.getBackground());
    }

    /**
     * Adds parameters to the panel in a wizard.
     * Returns number of rows.
     */
    public final void addWizardParams(final JPanel optionsPanel,
                                      final String[] params,
                                      final MyButton wizardApplyButton,
                                      final int leftWidth,
                                      final int rightWidth,
                                      final Map<String, Widget> sameAsFields) {
        addParams(optionsPanel, Widget.WIZARD_PREFIX, params, wizardApplyButton, leftWidth, rightWidth, sameAsFields);
    }

    /** Adds parameters to the panel. */
    public final void addParams(final JPanel optionsPanel,
                                final String[] params,
                                final int leftWidth,
                                final int rightWidth,
                                final Map<String, Widget> sameAsFields) {
        addParams(optionsPanel, null, params, applyButton, leftWidth, rightWidth, sameAsFields);
    }

    /** Adds parameters to the panel. */
    private void addParams(final JPanel optionsPanel,
                           final String prefix,
                           final String[] params,
                           final MyButton thisApplyButton,
                           final int leftWidth,
                           final int rightWidth,
                           final Map<String, Widget> sameAsFields) {
        swingUtils.isSwingThread();
        if (params == null) {
            return;
        }
        final MultiKeyMap<String, JPanel> panelPartsMap = new MultiKeyMap<String, JPanel>();
        final Collection<PanelPart> panelPartsList = new ArrayList<PanelPart>();
        final MultiKeyMap<String, Integer> panelPartRowsMap = new MultiKeyMap<String, Integer>();
        
        for (final String param : params) {
            final Widget paramWi = createWidget(param, prefix, rightWidth);
            /* sub panel */
            final String section = getSection(param);
            final JPanel panel;
            final AccessMode.Type accessType = getAccessType(param);
            final String accessTypeString = accessType.toString();
            final Boolean advanced = isAdvanced(param);
            final String advancedString = advanced.toString();
            if (panelPartsMap.containsKey(section, accessTypeString, advancedString)) {
                panel = panelPartsMap.get(section, accessTypeString, advancedString);
                panelPartRowsMap.put(section,
                                     accessTypeString,
                                     advancedString,
                                     panelPartRowsMap.get(section, accessTypeString, advancedString) + 1);
            } else {
                panel = new JPanel(new SpringLayout());

                panel.setBackground(getSectionColor(section));
                if (advanced) {
                    advancedPanelList.add(panel);
                    panel.setVisible(access.isAdvancedMode());
                }
                panelPartsMap.put(section, accessTypeString, advancedString, panel);
                panelPartsList.add(new PanelPart(section, accessType, advanced));
                panelPartRowsMap.put(section, accessTypeString, advancedString, 1);
            }

            /* label */
            final JLabel label = new JLabel(getParamShortDesc(param));
            final String longDesc = getParamLongDesc(param);
            paramWi.setLabel(label, longDesc);

            /* tool tip */
            paramWi.setToolTipText(getToolTipText(param, paramWi));
            label.setToolTipText(longDesc + additionalToolTip(param));
            int height = 0;
            if (paramWi instanceof Label) {
                height = application.getDefaultSize("Browser.LabelFieldHeight");
            }
            addField(panel, label, paramWi.getComponent(), leftWidth, rightWidth, height);
        }
        final boolean wizard = Widget.WIZARD_PREFIX.equals(prefix);
        for (final String param : params) {
            final Widget paramWi = getWidget(param, prefix);
            if (wizard) {
                final Widget rpwi = getWidget(param, null);
                if (rpwi == null) {
                    LOG.error("addParams: unknown param: " + param);
                    continue;
                }
                if (paramWi.getValue() == null
                    || paramWi.getValue().isNothingSelected()) {
                    rpwi.setValueAndWait(null);
                } else {
                    final Value value = paramWi.getValue();
                    rpwi.setValueAndWait(value);
                }
            }
        }
        for (final String param : params) {
            final Widget paramWi = getWidget(param, prefix);
            Widget rpwi = null;
            if (wizard) {
                rpwi = getWidget(param, null);
            }
            final Widget realParamWi = rpwi;
            paramWi.addListeners(new WidgetListener() {
                @Override
                public void check(final Value value) {
                    checkParameterFields(paramWi, realParamWi, param, getParametersFromXML(), thisApplyButton);
                }
            });
        }

        /* add sub panels to the option panel */
        final Map<String, JPanel> sectionMap = new HashMap<String, JPanel>();
        final Collection<JPanel> notAdvancedSections = new HashSet<JPanel>();
        final Collection<JPanel> advancedSections = new HashSet<JPanel>();
        for (final PanelPart panelPart : panelPartsList) {
            final String section = panelPart.getSection();
            final AccessMode.Type accessType = panelPart.getType();
            final String accessTypeString = accessType.toString();
            final Boolean advanced = panelPart.isAdvanced();
            final String advancedString = advanced.toString();

            final JPanel panel = panelPartsMap.get(section, accessTypeString, advancedString);
            final int rows = panelPartRowsMap.get(section, accessTypeString, advancedString);
            final int columns = 2;
            SpringUtilities.makeCompactGrid(panel, rows, columns,
                                                         1, 1,  // initX, initY
                                                         1, 1); // xPad, yPad
            final JPanel sectionPanel;
            if (sectionMap.containsKey(section)) {
                sectionPanel = sectionMap.get(section);
            } else {
                sectionPanel = getParamPanel(getSectionDisplayName(section), getSectionColor(section));
                sectionMap.put(section, sectionPanel);
                addSectionPanel(section, wizard, sectionPanel);
                optionsPanel.add(sectionPanel);
                if (sameAsFields != null) {
                    final Widget sameAsCombo = sameAsFields.get(section);
                    if (sameAsCombo != null) {
                        final JPanel saPanel = new JPanel(new SpringLayout());
                        saPanel.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
                        final JLabel label = new JLabel(Tools.getString("ClusterBrowser.SameAs"));
                        sameAsCombo.setLabel(label, "");
                        addField(saPanel, label, sameAsCombo.getComponent(), leftWidth, rightWidth, 0);
                        SpringUtilities.makeCompactGrid(saPanel, 1, 2,
                                                                 1, 1,  // initX, initY
                                                                 1, 1); // xPad, yPad
                        sectionPanel.add(saPanel);
                    }
                }
            }
            sectionPanel.setVisible(isSectionEnabled(section));
            sectionPanel.add(panel);
            if (advanced) {
                advancedSections.add(sectionPanel);
            } else {
                notAdvancedSections.add(sectionPanel);
            }
        }
        boolean advanced = false;
        for (final Map.Entry<String, JPanel> sectionEntry : sectionMap.entrySet()) {
            final JPanel sectionPanel = sectionEntry.getValue();
            if (advancedSections.contains(sectionPanel)) {
                advanced = true;
            }
            if (!notAdvancedSections.contains(sectionPanel)) {
                advancedOnlySectionList.add(sectionEntry.getKey());
                sectionPanel.setVisible(access.isAdvancedMode() && isSectionEnabled(sectionEntry.getKey()));
            }
        }
        moreOptionsPanel.setVisible(advanced && !access.isAdvancedMode());
    }


    /** Returns a more panel with "more options are available" message. */
    public final JPanel getMoreOptionsPanel(final int width) {
        final JLabel l = new JLabel(Tools.getString("EditableInfo.MoreOptions"));
        final Font font = l.getFont();
        final String name = font.getFontName();
        final int style = Font.ITALIC;
        final int size = font.getSize();
        l.setFont(new Font(name, style, size - 3));

        moreOptionsPanel.setBackground(Browser.PANEL_BACKGROUND);
        moreOptionsPanel.add(l);
        final Dimension d = moreOptionsPanel.getPreferredSize();
        d.width = width;
        moreOptionsPanel.setMaximumSize(d);
        return moreOptionsPanel;
    }

    /** Checks ands sets paramter fields. */
    public void checkParameterFields(final Widget paramWi,
                                     final Widget realParamWi,
                                     final String param,
                                     final String[] params,
                                     final MyButton thisApplyButton) {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Check check;
                if (realParamWi == null) {
                    swingUtils.waitForSwing();
                    check = checkResourceFields(param, params);
                } else {
                    swingUtils.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (paramWi.getValue() == null || paramWi.getValue().isNothingSelected()) {
                                realParamWi.setValueAndWait(null);
                            } else {
                                final Value value = paramWi.getValue();
                                realParamWi.setValueAndWait(value);
                            }
                        }
                    });
                    swingUtils.waitForSwing();
                    check = checkResourceFields(param, params);
                }
                swingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (resource.isNew()) {
                            check.addChanged("new resource");
                        }
                        if (thisApplyButton == applyButton) {
                            /* not a wizard button */
                            if (isDialogStarted()) {
                                check.addIncorrect("dialog started");
                            }
                            thisApplyButton.setEnabled(check);
                        } else {
                            /* wizard button */
                            thisApplyButton.setEnabledCorrect(check);
                        }
                        if (revertButton != null) {
                            revertButton.setEnabledChanged(check);
                        }
                        final String toolTip = getToolTipText(param, paramWi);
                        paramWi.setToolTipText(toolTip);
                        if (realParamWi != null) {
                            realParamWi.setToolTipText(toolTip);
                        }
                    }
                });
            }
        });
        thread.start();
    }

    /** Get stored value in the combo box. */
    public final Value getComboBoxValue(final String param) {
        final Widget wi = getWidget(param, null);
        if (wi == null) {
            return null;
        }
        return wi.getValue();
    }

    /** Stores values in the combo boxes in the component c. */
    public void storeComboBoxValues(final String[] params) {
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            resource.setValue(param, value);
            final Widget wi = getWidget(param, null);
            if (wi != null) {
                wi.setToolTipText(getToolTipText(param, wi));
            }
        }
    }

    /** Returns combo box for one parameter. */
    protected Widget createWidget(final String param, final String prefix, final int width) {
        resource.setPossibleChoices(param, getParamPossibleChoices(param));
        /* set default value */
        Value initValue = getPreviouslySelected(param, prefix);
        if (initValue == null) {
            final Value value = getParamSaved(param);
            if (value == null || value.isNothingSelected()) {
                if (resource.isNew()) {
                    initValue = resource.getPreferredValue(param);
                    if (initValue == null) {
                        initValue = getParamPreferred(param);
                    }
                }
                if (initValue == null) {
                    initValue = getParamDefault(param);
                    resource.setValue(param, initValue);
                }
            } else {
                initValue = value;
            }
        }
        final String regexp = getParamRegexp(param);
        Map<String, String> abbreviations = new HashMap<String, String>();
        if (isInteger(param)) {
            abbreviations = new HashMap<String, String>();
            abbreviations.put("i", CrmXml.INFINITY_VALUE.getValueForConfig());
            abbreviations.put("I", CrmXml.INFINITY_VALUE.getValueForConfig());
            abbreviations.put("+", CrmXml.PLUS_INFINITY_VALUE.getValueForConfig());
            abbreviations.put("d", CrmXml.DISABLED_IN_COMBOBOX.getValueForConfig());
            abbreviations.put("D", CrmXml.DISABLED_IN_COMBOBOX.getValueForConfig());
        }
        Widget.Type type = getFieldType(param);
        Unit[] units = null;
        if (type == Widget.Type.TEXTFIELDWITHUNIT) {
            units = getUnits(param);
        }
        if (isCheckBox(param)) {
            type = Widget.Type.CHECKBOX;
        } else if (isTimeType(param)) {
            type = Widget.Type.TEXTFIELDWITHUNIT;
            units = getTimeUnits();
        } else if (isLabel(param)) {
            type = Widget.Type.LABELFIELD;
        }
        final Widget paramWi = widgetFactory.createInstance(
                                      type,
                                      initValue,
                                      getPossibleChoices(param),
                                      units,
                                      regexp,
                                      width,
                                      abbreviations,
                                      new AccessMode(getAccessType(param), isEnabledOnlyInAdvancedMode(param)),
                                      null);
        widgetAdd(param, prefix, paramWi);
        paramWi.setEditable(true);
        return paramWi;
    }

    /**
     * Checks new value of the parameter if it correct and has changed.
     * Returns false if parameter is invalid or has not not changed from
     * the stored value. This is needed to disable apply button, if some of
     * the values are invalid or none of the parameters have changed.
     */
    protected abstract boolean checkParam(String param, Value newValue);

    /** Checks whether this value matches the regexp of this field. */
    protected final boolean checkRegexp(final String param, final Value newValue) {
        String regexp = getParamRegexp(param);
        if (regexp == null) {
            final Widget wi = getWidget(param, null);
            if (wi != null) {
                regexp = wi.getRegexp();
            }
        }
        if (regexp != null) {
            final Pattern p = Pattern.compile(regexp);
            if (newValue == null || newValue.isNothingSelected()) {
                return true;
            }
            final Matcher m = p.matcher(newValue.getValueForConfig());
            return m.matches();
        }
        return true;
    }

    /**
     * Checks parameter, but use cached value. This is useful if some other
     * parameter was modified, but not this one.
     */
    protected boolean checkParamCache(final String param) {
        if (param == null || !paramCorrectValueMap.containsKey(param)) {
            return false;
        }
        final Boolean ret = paramCorrectValueMap.get(param);
        if (ret == null) {
            return false;
        }
        return ret;
    }

    /** Sets the cache for the result of the parameter check. */
    protected final void setCheckParamCache(final String param, final boolean correctValue) {
        if (param == null) {
            return;
        }
        paramCorrectValueMap.put(param, correctValue);
    }

    protected abstract Value getParamDefault(String param);

    public Value getParamSaved(final String param) {
        return resource.getValue(param);
    }

    protected String getParamSavedForConfig(final String param) {
        final Value v = resource.getValue(param);
        if (v == null) {
            return null;
        } else {
            return v.getValueForConfig();
        }
    }

    protected abstract Value getParamPreferred(String param);

    protected abstract String getParamShortDesc(String param);

    protected abstract String getParamLongDesc(String param);

    /**
     * Returns possible choices in a combo box, if possible choices are
     * null, instead of combo box a text field will be generated.
     */
    protected Value[] getPossibleChoices(final String param) {
        return resource.getPossibleChoices(param);
    }

    /**
     * Creates panel with border and title for parameters with default
     * background.
     */
    protected final JPanel getParamPanel(final String title) {
        return getParamPanel(title, Browser.PANEL_BACKGROUND);
    }

    /**
     * Creates panel with border and title for parameters with specified
     * background.
     */
    protected final JPanel getParamPanel(final String title, final Color background) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBackground(background);
        final TitledBorder titleBorder = Tools.getBorder(title);
        panel.setBorder(titleBorder);
        return panel;
    }

    /**
     * Returns on mouse over text for parameter. If value is different
     * from default value, default value will be returned.
     */
    protected final String getToolTipText(final String param, final Widget wi) {
        final Value defaultValue = getParamDefault(param);
        final StringBuilder ret = new StringBuilder(120);
        if (wi != null) {
            final String value = wi.getStringValue();
            ret.append("<b>");
            ret.append(value);
            ret.append("</b>");
        }
        if (defaultValue != null && !defaultValue.isNothingSelected()) {
            ret.append("<table><tr><td><b>");
            ret.append(Tools.getString("Browser.ParamDefault"));
            ret.append("</b></td><td>");
            ret.append(defaultValue);
            ret.append("</td></tr></table>");
        }
        ret.append(additionalToolTip(param));
        return ret.toString();
        
    }

    /** Enables and disabled apply and revert button. */
    public final void setApplyButtons(final String param, final String[] params) {
        final Check check = checkResourceFields(param, params);
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                final MyButton ab = getApplyButton();
                final ResourceValue r = resource;
                if (ab != null) {
                    if (r != null && r.isNew()) {
                        check.addChanged("new resource");
                    }
                    ab.setEnabled(check);
                }
                final MyButton rb = getRevertButton();
                if (rb != null) {
                    rb.setEnabledChanged(check);
                }
            }
        });
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    public Check checkResourceFields(final String param, final String[] params) {
        /* check if values are correct */
        final List<String> incorrect = new ArrayList<String>();
        final List<String> changed = new ArrayList<String>();
        if (params != null) {
            for (final String otherParam : params) {
                final Widget wi = getWidget(otherParam, null);
                if (wi == null) {
                    continue;
                }
                Value newValue = wi.getValue();

                /* check if value has changed */
                Value oldValue = getParamSaved(otherParam);
                if (oldValue == null) {
                    oldValue = getParamDefault(otherParam);
                }
                if (!Tools.areEqual(newValue, oldValue)) {
                    changed.add(otherParam + ": " + oldValue + " \u2192 " + newValue);
                }

                /* check correctness */
                final Boolean correctValueCache = (otherParam == null) ? null : paramCorrectValueMap.get(otherParam);
                if (param == null || param.equals(otherParam) || correctValueCache == null) {
                    final Widget wizardWi = getWidget(otherParam, Widget.WIZARD_PREFIX);
                    final String enable = isEnabled(otherParam);
                    if (wizardWi != null) {
                        wizardWi.setDisabledReason(enable);
                        wizardWi.setEnabled(enable == null);
                        newValue = wizardWi.getValue();
                    }
                    wi.setDisabledReason(enable);
                    wi.setEnabled(enable == null);
                    final boolean check = checkParam(otherParam, newValue) && checkRegexp(otherParam, newValue);
                    if (check) {
                        if (isTimeType(otherParam) || hasUnitPrefix(otherParam)) {
                            wi.setBackground(getParamDefault(otherParam),
                                             getParamSaved(otherParam),
                                             isRequired(otherParam));
                            if (wizardWi != null) {
                                wizardWi.setBackground(getParamDefault(otherParam),
                                                       getParamSaved(otherParam),
                                                       isRequired(otherParam));
                            }
                        } else {
                            wi.setBackground(getParamDefault(otherParam),
                                             getParamSaved(otherParam),
                                             isRequired(otherParam));
                            if (wizardWi != null) {
                                wizardWi.setBackground(getParamDefault(otherParam),
                                                       getParamSaved(otherParam),
                                                       isRequired(otherParam));
                            }
                        }
                    } else {
                        wi.wrongValue();
                        if (wizardWi != null) {
                            wizardWi.wrongValue();
                        }
                        incorrect.add(otherParam);
                    }
                    setCheckParamCache(otherParam, check);
                } else {
                    if (!correctValueCache) {
                        incorrect.add(otherParam);
                    }
                }
                wi.processAccessMode();
            }
        }
        return new Check(incorrect, changed);
    }

    /** Waits till the info panel is done for the first time. */
    public final void waitForInfoPanel() {
        try {
            final boolean ret = infoPanelLatch.await(20, TimeUnit.SECONDS);
            if (!ret) {
                Tools.printStackTrace("latch timeout detected");
            }
            infoPanelLatch.await();
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /** Should be called after info panel is done. */
    protected final void infoPanelDone() {
        infoPanelLatch.countDown();
    }

    public void resetInfoPanel() {
        infoPanelLatch = new CountDownLatch(1);
    }

    protected final void addToAdvancedList(final JPanel panel) {
        advancedPanelList.add(panel);
    }

    /** Hide/Show advanced panels. */
    @Override
    public void updateAdvancedPanels() {
        super.updateAdvancedPanels();
        final boolean advancedMode = access.isAdvancedMode();
        boolean advanced = false;
        for (final JPanel apl : advancedPanelList) {
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    apl.setVisible(advancedMode);
                }
            });
            advanced = true;
        }
        for (final String section : advancedOnlySectionList) {
            final JPanel p = sectionPanels.get(section, Boolean.toString(!WIZARD));
            final JPanel pw = sectionPanels.get(section, Boolean.toString(WIZARD));
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final boolean v = advancedMode && isSectionEnabled(section);
                    p.setVisible(v);
                    if (pw != null) {
                        pw.setVisible(v);
                    }
                }
            });
            advanced = true;
        }
        final boolean a = advanced;
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                moreOptionsPanel.setVisible(a && !advancedMode);
            }
        });
    }

    /** Revert values. */
    public void revert() {
        final String[] params = getParametersFromXML();
        if (params == null) {
            return;
        }
        for (final String param : params) {
            Value v = getParamSaved(param);
            if (v == null) {
                v = getParamDefault(param);
            }
            final Widget wi = getWidget(param, null);
            if (wi != null && !Tools.areEqual(wi.getValue(), v)) {
                wi.setValue(v);
                final Widget wizardWi = getWidget(param, Widget.WIZARD_PREFIX);
                if (wizardWi != null) {
                    wizardWi.setValue(v);
                }
            }
        }
    }

    public final MyButton getApplyButton() {
        return applyButton;
    }

    public final MyButton getRevertButton() {
        return revertButton;
    }

    public final void setApplyButton(final MyButton applyButton) {
        this.applyButton = applyButton;
    }

    public final void setRevertButton(final MyButton revertButton) {
        this.revertButton = revertButton;
    }

    /** Returns if dialog was started. It disables the apply button. */
    private boolean isDialogStarted() {
        return dialogStarted;
    }

    /** Sets if dialog was started. It disables the apply button. */
    public void setDialogStarted(final boolean dialogStarted) {
        this.dialogStarted = dialogStarted;
    }

    public void clearPanelLists() {
        applyButton = null;
        oldApplyButton = null;
        advancedPanelList.clear();
        advancedOnlySectionList.clear();
        sectionPanels.clear();
        disabledSections.clear();
    }

    @Override
    public final void cleanup() {
        super.cleanup();
        clearPanelLists();
    }

    public void reloadComboBoxes() {
    }

    /** Return previously selected value of the parameter. This is used, when
     *  primitive changes to and from clone. */
    protected final Value getPreviouslySelected(final String param, final String prefix) {
        final Widget prevParamWi = getWidget(param, prefix);
        if (prevParamWi != null) {
            return prevParamWi.getValue();
        }
        return null;
    }

    protected String getSectionDisplayName(final String section) {
        return Tools.ucfirst(section);
    }

    protected Color getSectionColor(final String section) {
        return Browser.PANEL_BACKGROUND;
    }

    private JPanel getSectionPanel(final String section, final boolean wizard) {
        return sectionPanels.get(section, Boolean.toString(wizard));
    }

    protected final void addSectionPanel(final String section, final boolean wizard, final JPanel sectionPanel) {
        sectionPanels.put(section, Boolean.toString(wizard), sectionPanel);
    }

    protected final void enableSection(final String section, final boolean enable, final boolean wizard) {
        if (enable) {
            disabledSections.remove(section);
        } else {
            disabledSections.add(section);
        }
        final JPanel p = getSectionPanel(section, wizard);
        if (p != null) {
            p.setVisible(enable);
        }
    }

    protected final String[] getEnabledSectionParams(final Iterable<String> params) {
        final List<String> newParams = new ArrayList<String>();
        for (final String param : params) {
            if (isSectionEnabled(getSection(param))) {
                newParams.add(param);
            }
        }
        return newParams.toArray(new String[newParams.size()]);
    }

    protected final boolean isSectionEnabled(final String section) {
        return !disabledSections.contains(section);
    }

    protected String additionalToolTip(final String param) {
        return "";
    }

    public ResourceValue getResource() {
        return resource;
    }

    /**
     * This class holds a part of the panel within the same section, access
     * type and advanced mode setting.
     */
    private static class PanelPart {
        /** Section of this panel part. */
        private final String section;
        private final AccessMode.Type type;
        private final boolean advanced;

        PanelPart(final String section, final AccessMode.Type type, final boolean advanced) {
            this.section = section;
            this.type = type;
            this.advanced = advanced;
        }

        public final String getSection() {
            return section;
        }

        public final AccessMode.Type getType() {
            return type;
        }

        public final boolean isAdvanced() {
            return advanced;
        }
    }

}
