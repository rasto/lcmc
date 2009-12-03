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


package drbd.gui;

import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.ButtonCallback;
import drbd.data.resources.Resource;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Unit;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

import javax.swing.JSplitPane;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;


import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.geom.Point2D;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import javax.swing.border.TitledBorder;
import javax.swing.SpringLayout;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * This class holds host and cluster resource data in a tree. It shows
 * panels that allow to edit the data of resources, services etc., hosts and
 * clusters.
 * Every resource has its Info object, that accessible through the tree view.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Browser {

    /** Tree model of the menu tree. */
    private DefaultTreeModel treeModel;
    /** Top of the menu tree. */
    private DefaultMutableTreeNode treeTop;
    /** Tree. */
    private JTree tree;

    /** Split pane next to the menu. */
    private JSplitPane infoPanelSplitPane;
    /** Icon fot the categories. */
    private static final ImageIcon CATEGORY_ICON =
            Tools.createImageIcon(Tools.getDefault("Browser.CategoryIcon"));
    /** Apply icon. */
    private static final ImageIcon APPLY_ICON =
            Tools.createImageIcon(Tools.getDefault("Browser.ApplyIcon"));
    /** Actions memu icon. */
    private static final ImageIcon ACTIONS_ICON =
            Tools.createImageIcon(Tools.getDefault("Browser.ActionsIcon"));
    /** Color of the most of backgrounds. */
    private static final Color PANEL_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Background");
    /** Color of the extra panel with advanced options. */
    private static final Color EXTRA_PANEL_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Status.Background");
    /** DRBD test lock. */
    private final Mutex mDRBDtestLock = new Mutex();

    /** Sets the top of the menu tree. */
    protected final void setTreeTop() {
        treeTop = new DefaultMutableTreeNode(new CategoryInfo(
                                        Tools.getString("Browser.Resources")));
        treeModel = new DefaultTreeModel(treeTop);
    }

    /** Sets the top of the menu tree. */
    protected final void setTreeTop(final Info info) {
        treeTop = new DefaultMutableTreeNode(info);
        treeModel = new DefaultTreeModel(treeTop);
    }

    /**
     * Sets the tree instance variable.
     */
    protected final void setTree(final JTree tree) {
        this.tree = tree;
    }

    /**
     * Repaints the menu tree.
     */
    protected final void repaintTree() {
        tree.repaint();
    }

    /**
     * Gets node that is on the top of the tree.
     */
    public final DefaultMutableTreeNode getTreeTop() {
        return treeTop;
    }
    //public DefaultMutableTreeNode getTreeTop(Info info) {
    //    treeTop = new DefaultMutableTreeNode(info);
    //    treeModel = new DefaultTreeModel(treeTop);
    //    return treeTop;
    //}

    /** Reloads the node. */
    protected final void reload(final DefaultMutableTreeNode node) {
        treeModel.reload(node);
    }

    /** Sets the node change for the node. */
    protected final void nodeChanged(final DefaultMutableTreeNode node) {
        treeModel.nodeChanged(node);
    }

    /** Adds the node to the top level. */
    protected final void topAdd(final DefaultMutableTreeNode node) {
        treeTop.add(node);
    }

    /** Repaints the split pane. */
    protected final void repaintSplitPane() {
        if (infoPanelSplitPane != null) {
            infoPanelSplitPane.repaint();
        }
    }

    /**
     * Sets node variable in the info object that this tree node points to.
     */
    protected final void setNode(final DefaultMutableTreeNode node) {
        ((Info) node.getUserObject()).setNode(node);
    }

    /**
     * Gets tree model object.
     */
    public final DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * Returns panel with info of some resource from Info object. The info is
     * specified in getInfoPanel method in the Info object. If a resource has a
     * graphical view, it returns a split pane with this view and the info
     * underneath.
     */
    public final JComponent getInfoPanel(final Object nodeInfo) {
        final JPanel gView = ((Info) nodeInfo).getGraphicalView();
        final JComponent iPanel = ((Info) nodeInfo).getInfoPanel();
        if (gView == null) {
            return iPanel;
        } else {
            final Dimension d = iPanel.getPreferredSize();
            /* + 20 scrollbar */
            iPanel.setMinimumSize(new Dimension((int) d.getWidth() + 20,
                                                (int) d.getHeight()));
            final JSplitPane newSplitPane =
                                    new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                   gView,
                                                   iPanel);
            newSplitPane.setDividerSize(0);
            newSplitPane.setResizeWeight(1);
            infoPanelSplitPane = newSplitPane;
            infoPanelSplitPane.repaint();
            return infoPanelSplitPane;
        }
    }

    /**
     * This class holds info data for resources, services, hosts, clusters
     * etc. It provides methods to show this info and graphical view if
     * available.
     */
    public class Info {
        /** Menu node of this object. */
        private DefaultMutableTreeNode node;
        /** Name of the object. */
        private String name;
        /** Resource object as found in data/resources associated with this
         * object. */
        private Resource resource;

        /**
         * Area with text info.
         */
        private JEditorPane resourceInfoArea;

        /** Map from parameter to its user-editable widget. */
        private final Map<String, GuiComboBox> paramComboBoxHash =
                                            new HashMap<String, GuiComboBox>();
        /** popup menu for this object. */
        private JPopupMenu popup;
        /** menu of this object. */
        private JMenu menu;
        /** list of items in the menu for this object. */
        private final List<UpdatableItem> menuList =
                                                new ArrayList<UpdatableItem>();
        /** Whether the info object is being updated. */
        private boolean updated = false;
        /** Animation index. */
        private int animationIndex = 0;
        /** Cache with info text. */
        private String infoCache = "";

        /**
         * Prepares a new <code>Info</code> object.
         *
         * @param name
         *      name that will be shown to the user
         */
        public Info(final String name) {
            this.name = name;
        }

        /**
         * Sets name for this resource.
         *
         * @param name
         *      name that will be shown in the tree
         */
        public final void setName(final String name) {
            this.name = name;
        }

        /**
         * Returns the id of this object which is the name.
         */
        public String getId() {
            return name;
        }

        /**
         * Returns the tool tip for this object.
         */
        public String getToolTipText(final boolean testOnly) {
            return "no tooltip";
        }

        /**
         * Sets resource.
         */
        protected final void setResource(final Resource resource) {
            this.resource = resource;
        }

        /**
         * Adds the widget for parameter.
         */
        protected final void paramComboBoxAdd(final String param,
                                              final String prefix,
                                              final GuiComboBox paramCb) {
            if (prefix == null) {
                paramComboBoxHash.put(param, paramCb);
            } else {
                paramComboBoxHash.put(prefix + ":" + param, paramCb);
            }
        }

        /**
         * Returns the widget for the parameter.
         */
        protected final GuiComboBox paramComboBoxGet(final String param,
                                                     final String prefix) {
            if (prefix == null) {
                return paramComboBoxHash.get(param);
            } else {
                return paramComboBoxHash.get(prefix + ":" + param);
            }
        }

        /**
         * Returns true if the paramComboBox contains the parameter.
         */
        protected final boolean paramComboBoxContains(final String param,
                                                      final String prefix) {
            if (prefix == null) {
                return paramComboBoxHash.containsKey(param);
            } else {
                return paramComboBoxHash.containsKey(prefix + ":" + param);
            }
        }

        /**
         * Removes the parameter from the paramComboBox hash.
         */
        protected final GuiComboBox paramComboBoxRemove(final String param,
                                                        final String prefix) {
            if (prefix == null) {
                return paramComboBoxHash.remove(param);
            } else {
                return paramComboBoxHash.remove(prefix + ":" + param);
            }
        }

        /**
         * Clears the whole paramComboBox hash.
         */
        protected final void paramComboBoxClear() {
            paramComboBoxHash.clear();
        }

        /**
         * Sets the terminal panel, if necessary.
         */
        protected void setTerminalPanel() {
        }

        /**
         * Returns whether the info object is being updated. This can be used
         * for animations.
         */
        protected boolean isUpdated() {
            return updated;
        }

        /**
         * Sets whether the info object is being updated.
         */
        protected void setUpdated(final boolean updated) {
            this.updated = updated;
            animationIndex = 0;
        }

        /**
         * Returns the animation index.
         */
        public final int getAnimationIndex() {
            return animationIndex;
        }

        /**
         * Increments the animation index that wraps to zero if it is greater
         * than 100.
         */
        public final void incAnimationIndex() {
            animationIndex += 5;
            if (animationIndex > 100) {
                animationIndex = 0;
            }
        }

        /**
         * Returns the icon.
         */
        public ImageIcon getMenuIcon(final boolean testOnly) {
            return null;
        }

        /**
         * Returns the icon fot the category.
         */
        public ImageIcon getCategoryIcon() {
            return null;
        }

        ///**
        // * Returns whether two info objects are equal.
        // */
        //public boolean equals(final Object value) {
        //    if (value == null) {
        //        return false;
        //    }
        //    if (Tools.isStringClass(value)) {
        //        return name.equals(value.toString());
        //    } else {
        //        if (toString() == null) {
        //            return false;
        //        }
        //        return toString().equals(value.toString());
        //    }
        //}

        //public int hashCode() {
        //    return toString().hashCode();
        //}

        /**
         * Updates the info text.
         */
        public void updateInfo() {
            if (resourceInfoArea != null) {
                final String newInfo = getInfo();
                if (newInfo != null) {
                    if (!newInfo.equals(infoCache)) {
                        infoCache = newInfo;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                resourceInfoArea.setText(newInfo);
                            }
                        });
                    }
                }
            }
        }
        /**
         * Updates the info in the info panel, long after it is drawn. For
         * example, if command has to be executed to get the info.
         */
        protected void updateInfo(final JEditorPane ep) {
        }

        /**
         * Returns type of the info text. text/plain or text/html.
         */
        protected String getInfoType() {
            return "text/plain";
        }

        /**
         * Returns info panel for this resource.
         */
        public JComponent getInfoPanel() {
            //setTerminalPanel();
            final String info = getInfo();
            resourceInfoArea = null;
            if (info == null) {
                final JPanel panel = new JPanel();
                panel.setBackground(PANEL_BACKGROUND);
                return panel;
            } else {
                final Font f = new Font("Monospaced", Font.PLAIN, 12);
                resourceInfoArea = new JEditorPane(getInfoType(),
                                                   info);
                resourceInfoArea.setMinimumSize(new Dimension(
                    Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                    Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")
                    ));
                resourceInfoArea.setPreferredSize(new Dimension(
                    Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                    Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")
                    ));
                resourceInfoArea.setEditable(false);
                resourceInfoArea.setFont(f);
                resourceInfoArea.setBackground(PANEL_BACKGROUND);
                updateInfo(resourceInfoArea);
            }

            return new JScrollPane(resourceInfoArea);
        }


        /**
         * TODO: clears info panel cache most of the time.
         */
        public boolean selectAutomaticallyInTreeMenu() {
            return false;
        }

        /**
         * Returns graphics view of this resource.
         */
        public JPanel getGraphicalView() {
            return null;
        }

        /**
         * Returns info as string. This can be used by simple view, when
         * getInfoPanel() is not overwritten.
         */
        public String getInfo() {
            return name;
        }

        /**
         * Returns name of the object.
         */
        public String toString() {
            return name;
        }

        /**
         * Returns name of this resource.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets node of this resource or service.
         */
        public final DefaultMutableTreeNode getNode() {
            return node;
        }

        /**
         * Sets node in the tree view for this resource or service.
         */
        protected final void setNode(final DefaultMutableTreeNode node) {
            this.node = node;
        }

        /**
         * Removes this object from the tree and highlights and selects parent
         * node.
         */
        public void removeMyself(final boolean testOnly) {
            if (node != null) {
                final DefaultMutableTreeNode parent =
                                    (DefaultMutableTreeNode) node.getParent();
                if (parent != null) {
                    parent.remove(node);
                    setNode(null);
                    reload(parent);
                }
            }
        }

        /**
         * Selects and highlights this node.
         */
        public void selectMyself() {
            // this fires an event in ViewPanel.
            reload(node);
            nodeChanged(node);
        }

        /**
         * Returns resource object.
         */
        public final Resource getResource() {
            return resource;
        }

        /**
         * Returns tool tip for this object.
         */
        protected String getToolTipText(final String param,
                                        final String value) {
            return "TODO: ToolTipText";
        }

        /**
         * this method is used for values that are stored and can be different
         * than their appearance, which is taken from toString() method.
         * they are usually the same as in toString() method, but e.g. in
         * combo boxes they can be different. It trims the result too.
         */
        public String getStringValue() {
            if (name != null) {
                return name.trim();
            }
            return null;
        }

        /**
         * Shows the popup on the specified coordinates.
         */
        public final void showPopup(final JComponent c,
                                    final int x,
                                    final int y) {
            final JPopupMenu pm = getPopup();
            if (pm != null) {
                pm.show(c, x, y);
            }
        }

        /**
         * Returns tooltip for the object in the graph.
         */
        public String getToolTipForGraph(final boolean testOnly) {
            return getToolTipText(testOnly);
        }

        /**
         * Returns list of menu items for the popup.
         */
        protected /*abstract*/ List<UpdatableItem> createPopup() {
            return null;
        }

        /**
         * Returns the popup widget. The createPopup must be defined with menu
         * items.
         */
        public final JPopupMenu getPopup() {
            if (popup == null) {
                final List<UpdatableItem>items = createPopup();
                if (items != null) {
                    popup = new JPopupMenu();
                    for (final UpdatableItem u : items) {
                        popup.add((JMenuItem) u);
                    }
                }
            }
            if (popup != null) {
                updateMenus(null);
            }
            return popup;
        }

        /**
         * Returns popup on the spefified position.
         */
        public final JPopupMenu getPopup(final Point2D pos) {
            if (popup == null) {
                popup = new JPopupMenu();
                final List<UpdatableItem>items = createPopup();
                for (final UpdatableItem u : items) {
                    popup.add((JMenuItem) u);
                }
            }
            updateMenus(pos);
            return popup;
        }

        /**
         * Returns the Action menu.
         */
        public final JMenu getActionsMenu() {
            return getMenu(Tools.getString("Browser.ActionsMenu"));
        }

        /**
         * Returns the menu with menu item spefified in the createPopup method.
         */
        public final JMenu getMenu(final String name) {
            if (menu == null) {
                menu = new JMenu(name);
                menu.setIcon(ACTIONS_ICON);
                final List<UpdatableItem>items = createPopup();
                if (items != null) {
                    for (final UpdatableItem u : items) {
                        menu.add((JMenuItem) u);
                    }
                }
                menu.addItemListener(
                    new ItemListener() {
                        public void itemStateChanged(final ItemEvent e) {
                            if (e.getStateChange() == ItemEvent.SELECTED) {
                                final Thread thread = new Thread(
                                    new Runnable() {
                                        public void run() {
                                            updateMenus(null);
                                        }
                                    });
                                thread.start();
                            }
                        }
                    });
            }
            updateMenus(null);
            return menu;
        }

        /**
         * Update menus with positions and calles their update methods.
         */
        public final void updateMenus(final Point2D pos) {
            for (UpdatableItem i : menuList) {
                i.setPos(pos);
                i.update();
            }
        }

        /**
         * Registers a menu item.
         */
        protected final void registerMenuItem(final UpdatableItem m) {
            menuList.add(m);
        }

        /**
         * Returns units.
         */
        protected Unit[] getUnits() {
            return new Unit[]{
               new Unit("", "", "", ""),
               new Unit("msec", "ms", "Millisecond", "Milliseconds"),
               new Unit("usec", "us", "Microsecond", "Microseconds"),
               new Unit("sec",  "s",  "Second",      "Seconds"),
               new Unit("min",  "m",  "Minute",      "Minutes"),
               new Unit("hr",   "h",  "Hour",        "Hours")
           };
        }

        /**
         * Adds mouse over listener.
         */
        protected final void addMouseOverListener(final Component c,
                                                  final ButtonCallback bc) {
            c.addMouseListener(new MouseListener() {
                public void mouseClicked(final MouseEvent e) {
                    /* do nothing */
                }

                public synchronized void mouseEntered(final MouseEvent e) {
                    if (c.isShowing()
                        && c.isEnabled()) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                bc.mouseOver();
                            }
                        });
                        thread.start();
                    }
                }

                public synchronized void mouseExited(final MouseEvent e) {
                    final Thread t = new Thread(new Runnable() {
                        public void run() {
                            bc.mouseOut();
                        }
                    });
                    t.start();
                }

                public synchronized void mousePressed(final MouseEvent e) {
                    mouseExited(e);
                    /* do nothing */
                }

                public void mouseReleased(final MouseEvent e) {
                    /* do nothing */
                }
            });
        }
    }

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
        /** Returns whether this parameter is of the check box type, like
         * boolean. */
        protected abstract boolean isCheckBox(String param);
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
        public EditableInfo(final String name) {
            super(name);
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
                        APPLY_ICON);
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
                                        final int rightWidth) {
            final JLabel leftLabel = new JLabel(left);
            leftLabel.setToolTipText(left);
            final JLabel rightLabel = new JLabel(right);
            rightLabel.setToolTipText(right);
            addField(panel, leftLabel, rightLabel, leftWidth, rightWidth);
        }


        /**
         * Adds field with left and right component to the panel. Use panel
         * with spring layout for this.
         */
        public final void addField(final JPanel panel,
                                   final JComponent left,
                                   final JComponent right,
                                   final int leftWidth,
                                   final int rightWidth) {
            /* right component with fixed width. */
            Tools.setSize(left,
                          leftWidth,
                          Tools.getDefaultInt("Browser.FieldHeight"));

            panel.add(left);
            Tools.setSize(right,
                          rightWidth,
                          Tools.getDefaultInt("Browser.FieldHeight"));
            panel.add(right);
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
            //reload(getNode());
            //nodeChanged(getNode());
            //getInfoPanel(); // TODO: finished here
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
                    sectionRowsMap.put(section,
                                       sectionRowsMap.get(section) + 1);
                } else {
                    panel = getParamPanel(section);
                    sectionPanelMap.put(section, panel);
                    sectionRowsMap.put(section, 1);
                    sectionIsRequiredMap.put(section, isRequired);
                    //if (!isRequired)
                    //    panel.setVisible(false);
                }

                /* label */
                final JLabel label = new JLabel(getParamShortDesc(param));
                labelMap.put(paramCb, label);

                /* tool tip */
                final String longDesc = getParamLongDesc(param);
                label.setToolTipText(longDesc);
                final GuiComboBox realParamCb = paramComboBoxGet(param, null);
                addField(panel, label, paramCb, leftWidth, rightWidth);
                realParamCb.setValue(paramCb.getValue());
                paramCb.addListeners(
                    new ItemListener() {
                        public void itemStateChanged(final ItemEvent e) {
                            if (paramCb.isCheckBox()
                                || e.getStateChange() == ItemEvent.SELECTED) {
                                final Thread thread =
                                                new Thread(new Runnable() {
                                    public void run() {
                                        paramCb.setEditable();
                                        realParamCb.setValue(
                                                        paramCb.getValue());
                                        final boolean enable =
                                                checkResourceFieldsCorrect(
                                                    param, params);
                                        SwingUtilities.invokeLater(
                                            new Runnable() {
                                                public void run() {
                                                    wizardApplyButton.
                                                            setEnabled(enable);
                                                }
                                            }
                                        );
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
                                    realParamCb.setValue(
                                                        paramCb.getValue());
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            wizardApplyButton.setEnabled(check);
                                        }
                                    });
                                }
                            });
                            thread.start();
                        }

                        public void removeUpdate(final DocumentEvent e) {
                            final boolean check =
                                    checkResourceFieldsCorrect(param, params);
                            final Thread thread = new Thread(
                                new Runnable() {
                                    public void run() {
                                        wizardApplyButton.setEnabled(check);
                                        realParamCb.setValue(
                                                            paramCb.getValue());
                                    }
                                });
                            thread.start();
                        }

                        public void changedUpdate(final DocumentEvent e) {
                            final Thread thread = new Thread(
                                new Runnable() {
                                    public void run() {
                                        final boolean check =
                                            checkResourceFieldsCorrect(param,
                                                                       params);
                                        realParamCb.setValue(
                                                            paramCb.getValue());
                                        SwingUtilities.invokeLater(
                                            new Runnable() {
                                                public void run() {
                                                    wizardApplyButton.
                                                            setEnabled(check);
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
            //Tools.hideExpertModePanel(extraOptionsPanel);
        }

        /**
         * Adds parameters to the panel.
         * Returns number of rows.
         */
        public final void addParams(final JPanel optionsPanel,
                                    final JPanel extraOptionsPanel,
                                    final String[] params,
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
                    sectionRowsMap.put(section,
                                       sectionRowsMap.get(section) + 1);
                } else {
                    if (isRequired) {
                        panel = getParamPanel(section);
                    } else {
                        panel = getParamPanel(section, EXTRA_PANEL_BACKGROUND);
                    }
                    sectionPanelMap.put(section, panel);
                    sectionRowsMap.put(section, 1);
                    sectionIsRequiredMap.put(section, isRequired);
                    //if (!isRequired)
                    //    panel.setVisible(false);
                }

                /* label */
                final JLabel label = new JLabel(getParamShortDesc(param));
                labelMap.put(paramCb, label);

                /* tool tip */
                final String longDesc = getParamLongDesc(param);
                label.setToolTipText(longDesc);
                addField(panel, label, paramCb, leftWidth, rightWidth);
            }

            for (final String param : params) {
                final GuiComboBox paramCb = paramComboBoxGet(param, null);
                paramCb.addListeners(
                    new ItemListener() {
                        public void itemStateChanged(final ItemEvent e) {
                            if (paramCb.isCheckBox()
                                || e.getStateChange() == ItemEvent.SELECTED) {
                                final Thread thread = new Thread(
                                    new Runnable() {
                                        public void run() {
                                            paramCb.setEditable();
                                            final boolean check =
                                                    checkResourceFields(param,
                                                                        params);
                                            SwingUtilities.invokeLater(
                                                new Runnable() {
                                                    public void run() {
                                                        applyButton.setEnabled(
                                                                        check);
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
                                              checkResourceFields(param,
                                                                  params);
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            applyButton.setEnabled(check);
                                        }
                                    });
                                }
                            });
                            thread.start();
                        }

                        public void removeUpdate(final DocumentEvent e) {
                            final Thread thread = new Thread(
                                new Runnable() {
                                    public void run() {
                                        final boolean check =
                                                  checkResourceFields(param,
                                                                      params);
                                        SwingUtilities.invokeLater(
                                            new Runnable() {
                                                public void run() {
                                                    applyButton.setEnabled(
                                                                        check);
                                                }
                                            });
                                    }
                                });
                            thread.start();
                        }

                        public void changedUpdate(final DocumentEvent e) {
                            final Thread thread = new Thread(new Runnable() {
                                public void run() {
                                    final boolean check =
                                                   checkResourceFields(param,
                                                                       params);
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            applyButton.setEnabled(check);
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
            Tools.hideExpertModePanel(extraOptionsPanel);

        }

        /**
         * Get stored value in the combo box.
         */
        protected final String getComboBoxValue(final String param) {
            final GuiComboBox cb = paramComboBoxGet(param, null);
            final Object o = cb.getValue();
            String value;
            if (Tools.isStringClass(o)) {
                value = cb.getStringValue();
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
            }
        }

        /**
         * Returns combo box for one parameter.
         */
        protected GuiComboBox getParamComboBox(final String param,
                                               final String prefix,
                                               final int width) {
            getResource().setPossibleChoices(param,
                                             getParamPossibleChoices(param));
            /* set default value */
            String value = getResource().getValue(param);
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
                regexp = "^(-?(\\d*|INFINITY))|@NOTHING_SELECTED@$";
                abbreviations = new HashMap<String, String>();
                abbreviations.put("i", "INFINITY");
                abbreviations.put("I", "INFINITY");
            }
            GuiComboBox.Type type = null;
            Unit[] units = null;
            if (isCheckBox(param)) {
                type = GuiComboBox.Type.CHECKBOX;
            } else if (isTimeType(param)) {
                type = GuiComboBox.Type.TEXTFIELDWITHUNIT;
                units = getUnits();
            }
            final GuiComboBox paramCb = new GuiComboBox(
                                                     initValue,
                                                     getPossibleChoices(param),
                                                     units,
                                                     type,
                                                     regexp,
                                                     width,
                                                     abbreviations);
            paramComboBoxAdd(param, prefix, paramCb);
            paramCb.setEditable(true);
            paramCb.setToolTipText(getToolTipText(param,
                                                  getParamDefault(param)));
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
            return paramCorrectValueMap.get(param);
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
            return getParamPanel(title, PANEL_BACKGROUND);
        }

        /**
         * Creates panel with border and title for parameters with specified
         * background.
         */
        protected final JPanel getParamPanel(final String title,
                                             final Color background) {
            final JPanel panel = new JPanel(new SpringLayout());
            panel.setBackground(background);
            final TitledBorder titleBorder = Tools.getBorder(title);
            panel.setBorder(titleBorder);

            return panel;
        }

        /**
         * Returns on mouse over text for parameter. If value is different
         * from default value, default value will be returned.
         */
        protected final String getToolTipText(final String param,
                                              final String value) {
            String defaultValue = getParamDefault(param);
            if (defaultValue == null || defaultValue.equals("")) {
                defaultValue = "\"\"";
            }
            final StringBuffer ret = new StringBuffer(120);
            ret.append("<html><table><tr><td><b>");
            ret.append(Tools.getString("Browser.ParamDefault"));
            ret.append("</b></td><td>");
            ret.append(defaultValue);
            ret.append("</td></tr>");
            final String pt = getParamType(param);
            if (pt != null) {
                final StringBuffer paramType = new StringBuffer(pt);
                /* uppercase the first character */
                paramType.replace(0,
                                  1,
                                  paramType.substring(0, 1).toUpperCase());
                ret.append("<tr><td><b>");
                ret.append(Tools.getString("Browser.ParamType"));
                ret.append("</b></td><td>");
                ret.append(paramType);
                ret.append("</td></tr>");
            }
            ret.append("</table></html>");
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
                            } else {
                                newValue = ((Info) wo).getStringValue();
                            }
                        }

                        final boolean check = checkParam(otherParam, newValue);
                        if (check) {
                            cb.setBackground(getParamDefault(otherParam),
                                             isRequired(otherParam));
                            if (wizardCb != null) {
                                wizardCb.setBackground(
                                            getParamDefault(otherParam),
                                            isRequired(otherParam));
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
                        correctValue = correctValue
                                       && checkParamCache(otherParam);
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
                        return false; // TODO: should it be so?
                    }
                    final Object o = cb.getValue();
                    String newValue;
                    if (Tools.isStringClass(o)) {
                        newValue = cb.getStringValue();
                    } else {
                        newValue = ((Info) o).getStringValue();
                    }

                    /* check if value changed */
                    String oldValue = getResource().getValue(otherParam);
                    if (oldValue == null) {
                        oldValue = getParamDefault(otherParam);
                    }
                    if (oldValue == null) {
                        oldValue = "";
                    }
                    if (!oldValue.equals(newValue)) {
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
                /* ignored */
            }
        }

        /**
         * Should be called after info panel is done.
         */
        public final void infoPanelDone() {
            infoPanelLatch.countDown();
        }
    }

    /**
     * This class holds info data for a category.
     * Nothing is displayed.
     */
    class CategoryInfo extends Info {

        /**
         * Prepares a new <code>CategoryInfo</code> object.
         *
         * @param name
         *      name that will be shown in the tree
         */
        CategoryInfo(final String name) {
            super(name);
        }

        /**
         * Info panel for the category.
         */
        public String getInfo() {
            return null;
        }

        /**
         * Returns the icon.
         */
        public ImageIcon getMenuIcon(final boolean testOnly) {
            return CATEGORY_ICON;
        }
    }

    /**
     * This class is used for elements, that have their appearence (name)
     * different than is their stored value (string).
     */
    public class StringInfo extends Info {
        /** Internal string. */
        private final String string;

        /**
         * Creates new <code>StringInfo</code> object.
         *
         * @param name
         *              user visible name
         * @param string
         *              string representation
         */
        StringInfo(final String name, final String string) {
            super(name);
            this.string = string;
        }

        /**
         * Returns the name. It will be shown to the user.
         */
        public final String toString() {
            return getName();
        }

        /**
         * Returns the string that is used internally.
         */
        public final String getStringValue() {
            return string;
        }
    }

    /**
     * Returns cell rendererer for tree.
     */
    public final CellRenderer getCellRenderer() {
        return new CellRenderer();
    }

    /**
     * Renders the cells for the menu.
     */
    class CellRenderer extends DefaultTreeCellRenderer {
        /** Serial version UUID. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates new CellRenderer object.
         */
        public CellRenderer() {
            super();
            setBackgroundNonSelectionColor(PANEL_BACKGROUND);
            setBackgroundSelectionColor(
                        Tools.getDefaultColor("ViewPanel.Status.Background"));
            setTextNonSelectionColor(
                        Tools.getDefaultColor("ViewPanel.Foreground"));
            setTextSelectionColor(
                        Tools.getDefaultColor("ViewPanel.Status.Foreground"));
        }

        /**
         * Returns the CellRenderer component, setting up the icons and
         * tooltips.
         */
        public Component getTreeCellRendererComponent(
                            final JTree tree,
                            final Object value,
                            final boolean sel,
                            final boolean expanded,
                            final boolean leaf,
                            final int row,
                            final boolean hasFocus) {

            super.getTreeCellRendererComponent(
                            tree, value, sel,
                            expanded, leaf, row,
                            hasFocus);
            final Info i =
                    (Info) ((DefaultMutableTreeNode) value).getUserObject();
            if (leaf) {
                final ImageIcon icon = i.getMenuIcon(false);
                if (icon != null) {
                    setIcon(icon);
                }
                setToolTipText(null);
            } else {
                setToolTipText(null);
                ImageIcon icon = i.getCategoryIcon();
                if (icon == null) {
                    icon = CATEGORY_ICON;
                }
                setIcon(icon);
            }

            return this;
        }
    }

    /**
     * Acquire drbd test lock.
     */
    protected final void drbdtestLockAcquire() {
        try {
            mDRBDtestLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Release drbd test lock.
     */
    protected final void drbdtestLockRelease() {
        mDRBDtestLock.release();
    }
}
