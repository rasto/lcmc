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
package drbd.gui.resources;

import drbd.gui.Browser;
import drbd.gui.GuiComboBox;
import drbd.utilities.ButtonCallback;
import drbd.utilities.MyButton;
import drbd.utilities.Tools;
import drbd.utilities.Unit;
import drbd.data.CRMXML;
import drbd.gui.SpringUtilities;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.SpringLayout;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

/**
 * This class provides textfields, combo boxes etc. for editable info
 * objects.
 */
public abstract class EditableInfo extends Info {
    /** Hash from parameter to boolean value if the last entered value was
     * correct. */
    private final Map<String, Boolean> paramCorrectValueMap =
                                                new HashMap<String, Boolean>();

    /** Returns section in which is this parameter. */
    protected abstract String getSection(String param);
    /** Returns whether this parameter is required. */
    protected abstract boolean isRequired(String param);
    /** Returns whether this parameter is of the integer type. */
    protected abstract boolean isInteger(String param);
    /** Returns whether this parameter is of the time type. */
    protected abstract boolean isTimeType(String param);
    /** Returns whether this parameter has a unit prefix. */
    protected boolean hasUnitPrefix(final String param) {
        return false;
    }
    /** Returns whether this parameter is of the check box type, like
     * boolean. */
    protected abstract boolean isCheckBox(String param);
    /** Returns type of the field. */
    protected GuiComboBox.Type getFieldType(final String param) {
        return null;
    }
    /** Returns the name of the type. */
    protected abstract String getParamType(String param);
    /** Returns the possible choices for pull down menus if applicable. */
    protected abstract Object[] getParamPossibleChoices(String param);
    /** Returns array of all parameters. */
    protected abstract String[] getParametersFromXML(); // TODO: no XML
    /** Map from widget to its label. */
    private final Map<GuiComboBox, JLabel> labelMap =
                                        new HashMap<GuiComboBox, JLabel>();
    /** Old apply button, is used for wizards. */
    private MyButton oldApplyButton = null;
    /** Apply button. */ // TODO: private
    protected MyButton applyButton;
    /** Is counted down, first time the info panel is initialized. */
    private final CountDownLatch infoPanelLatch = new CountDownLatch(1);
    /** How much of the info is used. */
    public int getUsed() {
        return -1;
    }

    /**
     * Prepares a new <code>EditableInfo</code> object.
     *
     * @param name
     *      name that will be shown to the user.
     */
    public EditableInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /**
     * Inits apply button.
     */
    public final void initApplyButton(final ButtonCallback buttonCallback) {
        if (applyButton != null) {
            Tools.appWarning("wrong call to initApplyButton: " + getName());
        }
        if (oldApplyButton == null) {
            applyButton = new MyButton(
                    Tools.getString("Browser.ApplyResource"),
                    Browser.APPLY_ICON);
            oldApplyButton = applyButton;
        } else {
            applyButton = oldApplyButton;
        }
        applyButton.setEnabled(false);
        if (buttonCallback != null) {
            addMouseOverListener(applyButton, buttonCallback);
        }
    }

    /**
     * Creates apply button and adds it to the panel.
     */
    protected final void addApplyButton(final JPanel panel) {
        panel.add(applyButton, BorderLayout.WEST);
        Tools.getGUIData().getMainFrame().getRootPane().setDefaultButton(
                                                              applyButton);
    }

    /**
     * Adds jlabel field with tooltip.
     */
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
                               final JComponent left,
                               final JComponent right,
                               final int leftWidth,
                               final int rightWidth,
                               int height) {
        /* right component with fixed width. */
        if (height == 0) {
            height = Tools.getDefaultInt("Browser.FieldHeight");
        }
        Tools.setSize(left,
                      leftWidth,
                      height);
        panel.add(left);
        Tools.setSize(right,
                      rightWidth,
                      height);
        panel.add(right);
        right.setBackground(panel.getBackground());
    }

    /**
     * Adds parameters to the panel in a wizard.
     * Returns number of rows.
     */
    public final void addWizardParams(final JPanel optionsPanel,
                                      final JPanel extraOptionsPanel,
                                      final String[] params,
                                      final MyButton wizardApplyButton,
                                      final int leftWidth,
                                      final int rightWidth) {
        if (params == null) {
            return;
        }
        final Map<String, JPanel>  sectionPanelMap =
                                        new LinkedHashMap<String, JPanel>();
        final Map<String, Integer> sectionRowsMap =
                                        new HashMap<String, Integer>();
        final Map<String, Boolean> sectionIsRequiredMap =
                                        new HashMap<String, Boolean>();
        // TODO: parts of this are the same as in addParams
        for (final String param : params) {
            final GuiComboBox paramCb = getParamComboBox(param,
                                                         "wizard",
                                                         rightWidth);
            /* sub panel */
            final String section = getSection(param);
            final boolean isRequired = isRequired(param);
            JPanel panel;
            if (sectionPanelMap.containsKey(section)) {
                panel = sectionPanelMap.get(section);
                sectionRowsMap.put(section, sectionRowsMap.get(section) + 1);
            } else {
                panel = getParamPanel(section);
                sectionPanelMap.put(section, panel);
                sectionRowsMap.put(section, 1);
                sectionIsRequiredMap.put(section, isRequired);
            }

            /* label */
            final JLabel label = new JLabel(getParamShortDesc(param));
            paramCb.setLabel(label);
            labelMap.put(paramCb, label);

            /* tool tip */
            final String longDesc = getParamLongDesc(param);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    label.setToolTipText(longDesc);
                    paramCb.setToolTipText(getToolTipText(param));
                }
            });
            final GuiComboBox realParamCb = paramComboBoxGet(param, null);
            int height = 0;
            if (realParamCb.getType() == GuiComboBox.Type.LABELFIELD) {
                height = Tools.getDefaultInt("Browser.LabelFieldHeight");
            }
            addField(panel, label, paramCb, leftWidth, rightWidth, height);
            realParamCb.setValue(paramCb.getValue());
            paramCb.addListeners(
                new ItemListener() {
                    public void itemStateChanged(final ItemEvent e) {
                        if (paramCb.isCheckBox()
                            || e.getStateChange() == ItemEvent.SELECTED) {
                            final Thread thread = new Thread(new Runnable() {
                                public void run() {
                                    paramCb.setEditable();
                                    realParamCb.setValue(paramCb.getValue());
                                    final boolean enable =
                                      checkResourceFieldsCorrect(param, params);
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            wizardApplyButton.setEnabled(
                                                                       enable);
                                            paramCb.setToolTipText(
                                                        getToolTipText(param));
                                        }
                                    });
                                }
                            });
                            thread.start();
                        }
                    }
                },

                new DocumentListener() {
                    public void insertUpdate(final DocumentEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                final boolean check =
                                        checkResourceFieldsCorrect(param,
                                                                   params);
                                realParamCb.setValue(paramCb.getValue());
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        wizardApplyButton.setEnabled(check);
                                        paramCb.setToolTipText(
                                                        getToolTipText(param));
                                    }
                                });
                            }
                        });
                        thread.start();
                    }

                    public void removeUpdate(final DocumentEvent e) {
                        final boolean check =
                                checkResourceFieldsCorrect(param, params);
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                wizardApplyButton.setEnabled(check);
                                realParamCb.setValue(paramCb.getValue());
                                paramCb.setToolTipText(getToolTipText(param));
                            }
                        });
                        thread.start();
                    }

                    public void changedUpdate(final DocumentEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                final boolean check =
                                    checkResourceFieldsCorrect(param, params);
                                realParamCb.setValue(paramCb.getValue());
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        wizardApplyButton.setEnabled(check);
                                        paramCb.setToolTipText(
                                                        getToolTipText(param));
                                    }
                                });
                            }
                        });
                        thread.start();
                    }
                }
            );
        }

        /* add sub panels to the option panel */
        for (final String section : sectionPanelMap.keySet()) {
            final JPanel panel = sectionPanelMap.get(section);
            final int rows = sectionRowsMap.get(section);
            final int columns = 2;
            SpringUtilities.makeCompactGrid(panel, rows, columns,
                                            1, 1,  // initX, initY
                                            1, 1); // xPad, yPad
            final boolean isRequired =
                            sectionIsRequiredMap.get(section).booleanValue();

            if (isRequired) {
                optionsPanel.add(panel);
            } else {
                extraOptionsPanel.add(panel);
            }
        }
    }

    /**
     * Adds parameters to the panel.
     */
    public final void addParams(final JPanel optionsPanel,
                                final JPanel extraOptionsPanel,
                                final String[] params,
                                final int leftWidth,
                                final int rightWidth) {
        addParams(optionsPanel,
                  extraOptionsPanel,
                  params,
                  leftWidth,
                  rightWidth,
                  null);
    }

    /**
     * Adds parameters to the panel.
     */
    public final void addParams(final JPanel optionsPanel,
                                final JPanel extraOptionsPanel,
                                final String[] params,
                                final int leftWidth,
                                final int rightWidth,
                                final Map<String, GuiComboBox> sameAsFields) {
        if (params == null) {
            return;
        }
        final Map<String, JPanel>  sectionPanelMap =
                                        new LinkedHashMap<String, JPanel>();
        final Map<String, Integer> sectionRowsMap =
                                        new HashMap<String, Integer>();
        final Map<String, Boolean> sectionIsRequiredMap =
                                        new HashMap<String, Boolean>();

        for (final String param : params) {
            final GuiComboBox paramCb = getParamComboBox(param,
                                                         null,
                                                         rightWidth);
            /* sub panel */
            final String section = getSection(param);
            final boolean isRequired = isRequired(param);
            JPanel panel;
            if (sectionPanelMap.containsKey(section)) {
                panel = sectionPanelMap.get(section);
                sectionRowsMap.put(section, sectionRowsMap.get(section) + 1);
            } else {
                if (isRequired) {
                    panel = getParamPanel(section);
                } else {
                    panel = getParamPanel(section,
                                          Browser.EXTRA_PANEL_BACKGROUND);
                }
                sectionPanelMap.put(section, panel);
                sectionRowsMap.put(section, 1);
                sectionIsRequiredMap.put(section, isRequired);
                if (sameAsFields != null) {
                    final GuiComboBox sameAsCombo = sameAsFields.get(section);
                    if (sameAsCombo != null) {
                        final JLabel label = new JLabel("Same As");
                        addField(panel,
                                 label,
                                 sameAsCombo,
                                 leftWidth,
                                 rightWidth,
                                 0);
                        final int rows = sectionRowsMap.get(section);
                        sectionRowsMap.put(section, rows + 1);
                    }
                }
            }

            /* label */
            final JLabel label = new JLabel(getParamShortDesc(param));
            labelMap.put(paramCb, label);
            paramCb.setLabel(label);

            /* tool tip */
            final String longDesc = getParamLongDesc(param);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    paramCb.setToolTipText(getToolTipText(param));
                    label.setToolTipText(longDesc);
                }
            });
            int height = 0;
            if (paramCb.getType() == GuiComboBox.Type.LABELFIELD) {
                height = Tools.getDefaultInt("Browser.LabelFieldHeight");
            }
            addField(panel, label, paramCb, leftWidth, rightWidth, height);
        }

        for (final String param : params) {
            final GuiComboBox paramCb = paramComboBoxGet(param, null);
            paramCb.addListeners(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    if (paramCb.isCheckBox()
                        || e.getStateChange() == ItemEvent.SELECTED) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                public void run() {
                                    paramCb.setEditable();
                                    final boolean check =
                                            checkResourceFields(param, params);
                                    SwingUtilities.invokeLater(
                                        new Runnable() {
                                            public void run() {
                                                applyButton.setEnabled(check);
                                                paramCb.setToolTipText(
                                                        getToolTipText(param));
                                            }
                                        });
                                }
                            });
                        thread.start();
                    }
                }
            },

            new DocumentListener() {
                public void insertUpdate(final DocumentEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            final boolean check = checkResourceFields(param,
                                                                      params);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    applyButton.setEnabled(check);
                                    paramCb.setToolTipText(
                                                        getToolTipText(param));
                                }
                            });
                        }
                    });
                    thread.start();
                }

                public void removeUpdate(final DocumentEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            final boolean check =
                                      checkResourceFields(param, params);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    applyButton.setEnabled(check);
                                    paramCb.setToolTipText(
                                                getToolTipText(param));
                                }
                            });
                        }
                    });
                    thread.start();
                }

                public void changedUpdate(final DocumentEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            final boolean check = checkResourceFields(param,
                                                                      params);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    applyButton.setEnabled(check);
                                    paramCb.setToolTipText(
                                                getToolTipText(param));
                                }
                            });
                        }
                    });
                    thread.start();
                }
            });
        }

        /* add sub panels to the option panel */
        for (final String section : sectionPanelMap.keySet()) {
            final JPanel panel = sectionPanelMap.get(section);
            final int rows = sectionRowsMap.get(section);
            final int columns = 2;
            SpringUtilities.makeCompactGrid(panel, rows, columns,
                                            1, 1,  // initX, initY
                                            1, 1); // xPad, yPad
            final boolean isRequired =
                        sectionIsRequiredMap.get(section).booleanValue();

            if (isRequired) {
                optionsPanel.add(panel);
            } else {
                extraOptionsPanel.add(panel);
            }
        }
        Tools.hideExpertModePanel(extraOptionsPanel);
    }

    /**
     * Get stored value in the combo box.
     */
    protected final String getComboBoxValue(final String param) {
        final GuiComboBox cb = paramComboBoxGet(param, null);
        if (cb == null) {
            return null;
        }
        final Object o = cb.getValue();
        String value;
        if (Tools.isStringClass(o)) {
            value = cb.getStringValue();
        } else if (o instanceof Object[]) {
            value = ((Object[]) o)[0].toString()
                       + ((Unit) ((Object[]) o)[1]).getShortName();
        } else {
            value = ((Info) o).getStringValue();
        }
        return value;
    }

    /**
     * Stores values in the combo boxes in the component c.
     */
    protected final void storeComboBoxValues(final String[] params) {
        for (String param : params) {
            final String value = getComboBoxValue(param);
            getResource().setValue(param, value);
            final GuiComboBox cb = paramComboBoxGet(param, null);
            if (cb != null) {
               cb.setToolTipText(getToolTipText(param));
            }
        }
    }

    /**
     * Returns combo box for one parameter.
     */
    protected GuiComboBox getParamComboBox(final String param,
                                           final String prefix,
                                           final int width) {
        getResource().setPossibleChoices(param, getParamPossibleChoices(param));
        /* set default value */
        final String value = getParamSaved(param);
        String initValue;
        if (value == null || "".equals(value)) {
            initValue = getParamPreferred(param);
            if (initValue == null) {
                initValue = getParamDefault(param);
            }
            getResource().setValue(param, initValue);
        } else {
            initValue = value;
        }
        String regexp = null;
        Map<String, String> abbreviations = new HashMap<String, String>();
        if (isInteger(param)) {
            regexp = "^(-?(\\d*|" + CRMXML.INFINITY_STRING
                     + "))|@NOTHING_SELECTED@$";
            abbreviations = new HashMap<String, String>();
            abbreviations.put("i", CRMXML.INFINITY_STRING);
            abbreviations.put("I", CRMXML.INFINITY_STRING);
        }
        GuiComboBox.Type type = getFieldType(param);
        Unit[] units = null;
        if (type == GuiComboBox.Type.TEXTFIELDWITHUNIT) {
            units = getUnits();
        }
        if (isCheckBox(param)) {
            type = GuiComboBox.Type.CHECKBOX;
        } else if (isTimeType(param)) {
            type = GuiComboBox.Type.TEXTFIELDWITHUNIT;
            units = getTimeUnits();
        }
        final GuiComboBox paramCb = new GuiComboBox(initValue,
                                                    getPossibleChoices(param),
                                                    units,
                                                    type,
                                                    regexp,
                                                    width,
                                                    abbreviations);
        paramComboBoxAdd(param, prefix, paramCb);
        paramCb.setEditable(true);
        //addMouseOverListener(
        //    paramCb,
        //    new ButtonCallback() {
        //        public final boolean isEnabled() {
        //            return true;
        //        }
        //        public final void mouseOut() {
        //            /* do nothing */
        //        }
        //        public final void mouseOver() {
        //            System.out.println("set tool tip: " + param);
        //            paramCb.setToolTipText(getToolTipText(param));
        //        }
        //    });
        return paramCb;
    }

    /**
     * Checks new value of the parameter if it correct and has changed.
     * Returns false if parameter is invalid or has not not changed from
     * the stored value. This is needed to disable apply button, if some of
     * the values are invalid or none of the parameters have changed.
     */
    protected abstract boolean checkParam(String param, String newValue);

    /**
     * Checks parameter, but use cached value. This is useful if some other
     * parameter was modified, but not this one.
     */
    protected boolean checkParamCache(final String param) {
        if (!paramCorrectValueMap.containsKey(param)) {
            return false;
        }
        final Boolean ret = paramCorrectValueMap.get(param);
        if (ret == null) {
            return false;
        }
        return ret;
    }

    /**
     * Sets the cache for the result of the parameter check.
     */
    protected final void setCheckParamCache(final String param,
                                            final boolean correctValue) {
        paramCorrectValueMap.put(param, correctValue);
    }

    /**
     * Returns default value of a parameter.
     */
    protected abstract String getParamDefault(String param);

    /**
     * Returns saved value of a parameter.
     */
    protected String getParamSaved(final String param) {
        return getResource().getValue(param);
    }

    /**
     * Returns preferred value of a parameter.
     */
    protected abstract String getParamPreferred(String param);

    /**
     * Returns short description of a parameter.
     */
    protected abstract String getParamShortDesc(String param);

    /**
     * Returns long description of a parameter.
     */
    protected abstract String getParamLongDesc(String param);

    /**
     * Returns possible choices in a combo box, if possible choices are
     * null, instead of combo box a text field will be generated.
     */
    protected Object[] getPossibleChoices(final String param) {
        return getResource().getPossibleChoices(param);
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
    protected final JPanel getParamPanel(final String title,
                                         final Color background) {
        final JPanel panel = new JPanel(new SpringLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBackground(background);
        final TitledBorder titleBorder = Tools.getBorder(title);
        panel.setBorder(titleBorder);
        return panel;
    }

    /**
     * Returns on mouse over text for parameter. If value is different
     * from default value, default value will be returned.
     */
    protected final String getToolTipText(final String param) {
        final String defaultValue = getParamDefault(param);
        final StringBuffer ret = new StringBuffer(120);
        ret.append("<html>");
        final GuiComboBox cb = paramComboBoxGet(param, null);
        if (cb != null) {
            final Object value = cb.getStringValue();
            ret.append("<b>");
            ret.append(value);
            ret.append("</b>");
        }
        if (defaultValue != null && !defaultValue.equals("")) {
            ret.append("<table><tr><td><b>");
            ret.append(Tools.getString("Browser.ParamDefault"));
            ret.append("</b></td><td>");
            ret.append(defaultValue);
            ret.append("</td></tr></table></html>");
        }
        return ret.toString();

    }

    /**
     * Can be called from dialog box, where it does not need to check if
     * fields have changed.
     */
    public boolean checkResourceFields(final String param,
                                       final String[] params) {
        final boolean cor = checkResourceFieldsCorrect(param, params);
        if (cor) {
            return checkResourceFieldsChanged(param, params);
        }
        return cor;
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    public boolean checkResourceFieldsCorrect(final String param,
                                              final String[] params) {
        /* check if values are correct */
        boolean correctValue = true;
        if (params != null) {
            for (final String otherParam : params) {
                final GuiComboBox cb = paramComboBoxGet(otherParam, null);
                if (cb == null) {
                    continue;
                }
                String newValue;
                final Object o = cb.getValue();
                if (Tools.isStringClass(o)) {
                    newValue = cb.getStringValue();
                } else if (o instanceof Object[]) {
                    final Object o0 = ((Object[]) o)[0];
                    final Object o1 = ((Object[]) o)[1];
                    newValue = o0.toString();
                    if (o1 != null) {
                        newValue += ((Unit) o1).getShortName();
                    }
                } else {
                    newValue = ((Info) o).getStringValue();
                }

                if (param == null || otherParam.equals(param)) {
                    final GuiComboBox wizardCb =
                                    paramComboBoxGet(otherParam, "wizard");
                    if (wizardCb != null) {
                        final Object wo = wizardCb.getValue();
                        if (Tools.isStringClass(wo)) {
                            newValue = wizardCb.getStringValue();
                        } else if (wo instanceof Object[]) {
                            newValue =
                                   ((Object[]) wo)[0].toString()
                                   + ((Unit) ((Object[]) wo)[1]).getShortName();
                        } else {
                            newValue = ((Info) wo).getStringValue();
                        }
                    }
                    final boolean check = checkParam(otherParam, newValue);
                    if (check) {
                        if (isTimeType(otherParam)
                            || hasUnitPrefix(otherParam)) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    cb.setBackground(
                                                Tools.extractUnit(
                                                   getParamDefault(otherParam)),
                                                Tools.extractUnit(
                                                   getParamSaved(otherParam)),
                                                isRequired(otherParam));
                                }
                            });
                            if (wizardCb != null) {
                                wizardCb.setBackground(
                                    Tools.extractUnit(
                                              getParamDefault(otherParam)),
                                    Tools.extractUnit(
                                        getParamSaved(otherParam)),
                                    isRequired(otherParam));
                            }
                        } else {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    cb.setBackground(
                                             getParamDefault(otherParam),
                                             getParamSaved(otherParam),
                                             isRequired(otherParam));
                                    if (wizardCb != null) {
                                        wizardCb.setBackground(
                                                    getParamDefault(otherParam),
                                                    getParamSaved(otherParam),
                                                    isRequired(otherParam));
                                    }
                                }
                            });
                        }
                    } else {
                        cb.wrongValue();
                        if (wizardCb != null) {
                            wizardCb.wrongValue();
                        }
                        correctValue = false;
                    }
                    setCheckParamCache(otherParam, check);
                } else {
                    correctValue = correctValue && checkParamCache(otherParam);
                }
            }
        }
        return correctValue;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params) {
        /* check if something is different from saved values */
        boolean changedValue = false;
        if (params != null) {
            for (String otherParam : params) {
                final GuiComboBox cb = paramComboBoxGet(otherParam, null);
                if (cb == null) {
                    continue;
                }
                final Object newValue = cb.getValue();

                /* check if value has changed */
                Object oldValue = getParamSaved(otherParam);
                if (oldValue == null) {
                    oldValue = getParamDefault(otherParam);
                }
                if (isTimeType(otherParam) || hasUnitPrefix(otherParam)) {
                    oldValue = Tools.extractUnit((String) oldValue);
                }
                if (!Tools.areEqual(newValue, oldValue)) {
                    changedValue = true;
                }
            }
        }
        return changedValue;
    }

    /**
     * Return JLabel object for the combobox.
     */
    protected final JLabel getLabel(final GuiComboBox cb) {
        //TODO: labelMap can be removed. cb.getLabel()
        return labelMap.get(cb);
    }

    /**
     * Removes this editable object and clealrs the parameter hashes.
     */
    public void removeMyself(final boolean testOnly) {
        super.removeMyself(testOnly);
        paramComboBoxClear();
    }

    /**
     * Waits till the info panel is done for the first time.
     */
    public final void waitForInfoPanel() {
        try {
            infoPanelLatch.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Should be called after info panel is done.
     */
    public final void infoPanelDone() {
        infoPanelLatch.countDown();
    }
}
