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
import lcmc.gui.GuiComboBox;
import lcmc.data.resources.Resource;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.Unit;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.MyCellRenderer;
import lcmc.utilities.MyButtonCellRenderer;
import lcmc.utilities.MyButton;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import javax.swing.JMenuItem;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.JTable;
import javax.swing.BoxLayout;
import javax.swing.border.TitledBorder;
import javax.swing.JToggleButton;
import javax.swing.AbstractButton;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.Component;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
import java.awt.event.MouseListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Point;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.MouseMotionListener;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

/**
 * This class holds info data for resources, services, hosts, clusters
 * etc. It provides methods to show this info and graphical view if
 * available.
 */
public class Info implements Comparable {
    /** Menu node of this object. */
    private DefaultMutableTreeNode node = null;
    /** Name of the object. */
    private String name;
    /** Resource object as found in data/resources associated with this
     * object. */
    private Resource resource;
    /** Amount of frames per second. */
    private static final float FPS = Tools.getConfigData().getAnimFPS();
    /** TODL: Checking for leak. */
    private int maxMenuList = 0;

    /**
     * Area with text info.
     */
    private JEditorPane resourceInfoArea;

    /** Map from parameter to its user-editable widget. */
    private final Map<String, GuiComboBox> paramComboBoxHash =
              Collections.synchronizedMap(new HashMap<String, GuiComboBox>());
    /** popup menu for this object. */
    private JPopupMenu popup;
    /** Popup object lock. */
    private final Lock mPopupLock = new ReentrantLock();
    /** menu of this object. */
    private JMenu menu;
    /** Menu list lock. */
    private final Lock mMenuListLock = new ReentrantLock();
    /** list of items in the menu for this object. */
    private List<UpdatableItem> menuList = new ArrayList<UpdatableItem>();
    /** Whether the info object is being updated. */
    private boolean updated = false;
    /** Animation index. */
    private double animationIndex = 0;
    /** Cache with info text. */
    private String infoCache = "";
    /** Browser object. */
    private final Browser browser;
    /** Table. */
    private final Map<String, JTable> tables = new HashMap<String, JTable>();
    /** Table models. */
    private final Map<String, DefaultTableModel> tableModels =
                                    new HashMap<String, DefaultTableModel>();
    /** Log file icon. */
    public static final ImageIcon LOGFILE_ICON = Tools.createImageIcon(
                                  Tools.getDefault("Info.LogIcon"));
    /**
     * Prepares a new <code>Info</code> object.
     *
     * @param name
     *      name that will be shown to the user
     */
    Info(final String name, final Browser browser) {
        this.name = name;
        this.browser = browser;
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

    /** Returns the id of this object which is the name. */
    public String getId() {
        return name;
    }

    /** Returns browser object of this info. */
    protected Browser getBrowser() {
        return browser;
    }

    /** Returns the tool tip for this object. */
    String getToolTipText(final boolean testOnly) {
        return "no tooltip";
    }

    /** Sets resource. */
    protected final void setResource(final Resource resource) {
        this.resource = resource;
    }

    /** Adds the widget for parameter. */
    protected final void paramComboBoxAdd(final String param,
                                          final String prefix,
                                          final GuiComboBox paramCb) {
        if (prefix == null) {
            paramComboBoxHash.put(param, paramCb);
        } else {
            paramComboBoxHash.put(prefix + ":" + param, paramCb);
        }
    }

    /** Returns the widget for the parameter. */
    public final GuiComboBox paramComboBoxGet(final String param,
                                                        final String prefix) {
        if (prefix == null) {
            return paramComboBoxHash.get(param);
        } else {
            return paramComboBoxHash.get(prefix + ":" + param);
        }
    }

    /** Returns true if the paramComboBox contains the parameter. */
    protected final boolean paramComboBoxContains(final String param,
                                                  final String prefix) {
        if (prefix == null) {
            return paramComboBoxHash.containsKey(param);
        } else {
            return paramComboBoxHash.containsKey(prefix + ":" + param);
        }
    }

    /** Removes the parameter from the paramComboBox hash. */
    protected final GuiComboBox paramComboBoxRemove(final String param,
                                                    final String prefix) {
        if (prefix == null) {
            if (paramComboBoxHash.containsKey(param)) {
                paramComboBoxHash.get(param).cleanup();
                return paramComboBoxHash.remove(param);
            }
            return null;
        } else {
            if (paramComboBoxHash.containsKey(prefix + ":" + param)) {
                paramComboBoxHash.get(prefix + ":" + param).cleanup();
                return paramComboBoxHash.remove(prefix + ":" + param);
            }
            return null;
        }
    }

    /** Clears the whole paramComboBox hash. */
    protected final void paramComboBoxClear() {
        for (final String param : paramComboBoxHash.keySet()) {
            paramComboBoxHash.get(param).cleanup();
        }
        paramComboBoxHash.clear();
    }

    /** Sets the terminal panel, if necessary. */
    protected void setTerminalPanel() {
        /* set terminal panel, or don't */
    }

    /**
     * Returns whether the info object is being updated. This can be used
     * for animations.
     */
    protected final boolean isUpdated() {
        return updated;
    }

    /** Sets whether the info object is being updated. */
    protected void setUpdated(final boolean updated) {
        this.updated = updated;
        animationIndex = 0;
    }

    /** Returns the animation index. */
    public final double getAnimationIndex() {
        return animationIndex;
    }

    /**
     * Increments the animation index that wraps to zero if it is greater
     * than 100.
     */
    public final void incAnimationIndex() {
        animationIndex += 3.0 * 20.0 / FPS;
        if (animationIndex > 100) {
            animationIndex = 0;
        }
    }

    /** Returns the icon. */
    public ImageIcon getMenuIcon(final boolean testOnly) {
        return null;
    }

    /** Returns the icon for the category. */
    public ImageIcon getCategoryIcon(final boolean testOnly) {
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

    /** Updates the info text. */
    public void updateInfo() {
        final JEditorPane ria = resourceInfoArea;
        if (ria != null) {
            final String newInfo = getInfo();
            if (newInfo != null && !newInfo.equals(infoCache)) {
                infoCache = newInfo;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        ria.setText(newInfo);
                    }
                });
            }
        }
    }

    /**
     * Updates the info in the info panel, long after it is drawn. For
     * example, if command has to be executed to get the info.
     */
    protected void updateInfo(final JEditorPane ep) {
        /* override this method. */
    }

    /** Returns type of the info text. text/plain or text/html. */
    protected String getInfoType() {
        return Tools.MIME_TYPE_TEXT_PLAIN;
    }

    /** Returns back button. */
    protected JComponent getBackButton() {
        return null;
    }

    /** Returns info panel for this resource. */
    public JComponent getInfoPanel() {
        //setTerminalPanel();
        final String info = getInfo();
        resourceInfoArea = null;
        if (info == null) {
            final JPanel panel = new JPanel();
            panel.setBackground(Browser.PANEL_BACKGROUND);
            return panel;
        } else {
            final Font f = new Font("Monospaced", Font.PLAIN, 12);
            resourceInfoArea = new JEditorPane(getInfoType(), info);
            resourceInfoArea.setMinimumSize(new Dimension(
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")));
            resourceInfoArea.setPreferredSize(new Dimension(
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")));
            resourceInfoArea.setEditable(false);
            resourceInfoArea.setFont(f);
            resourceInfoArea.setBackground(Browser.PANEL_BACKGROUND);
            updateInfo(resourceInfoArea);
        }
        final JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Browser.PANEL_BACKGROUND);
        final JComponent backButton = getBackButton();
        if (backButton != null) {
            infoPanel.add(backButton);
        }
        infoPanel.add(new JScrollPane(resourceInfoArea));
        return infoPanel;
    }

    /** TODO: clears info panel cache most of the time. */
    boolean selectAutomaticallyInTreeMenu() {
        return false;
    }

    /** Returns graphics view of this resource. */
    public JPanel getGraphicalView() {
        return null;
    }

    /**
     * Returns info as string. This can be used by simple view, when
     * getInfoPanel() is not overwritten.
     */
    String getInfo() {
        return name;
    }

    /** Returns name of the object. */
    @Override public String toString() {
        return name;
    }

    /** Returns name of this resource. */
    public String getName() {
        return name;
    }

    /** Gets node of this resource or service. */
    final DefaultMutableTreeNode getNode() {
        return node;
    }

    /** Sets node in the tree view for this resource or service. */
    public final void setNode(final DefaultMutableTreeNode node) {
        this.node = node;
    }

    /** Cleanup. */
    void cleanup() {
        mMenuListLock.lock();
        if (menuList == null) {
            mMenuListLock.unlock();
        } else {
            final List<UpdatableItem> menuListCopy =
                                   new ArrayList<UpdatableItem>(menuList);
            mMenuListLock.unlock();
            for (final UpdatableItem i : menuListCopy) {
                i.cleanup();
            }
        }
    }

    /**
     * Removes this object from the tree and highlights and selects parent
     * node.
     */
    public void removeMyself(final boolean testOnly) {
        cleanup();
    }

    /** Selects and highlights this node. */
    public void selectMyself() {
        // this fires an event in ViewPanel.
        if (node != null) {
            getBrowser().reload(node, true);
            getBrowser().nodeChanged(node);
        }
    }

    /** Returns resource object. */
    public final Resource getResource() {
        return resource;
    }

    /** Returns tool tip for this object. */
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

    /** Shows the popup on the specified coordinates. */
    public final void showPopup(final JComponent c,
                                final int x,
                                final int y) {
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                final JPopupMenu pm = getPopup();
                if (pm != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            pm.show(c, x, y);
                        }
                    });
                }
            }
        });
        thread.start();
    }

    /** Returns tooltip for the object in the graph. */
    public String getToolTipForGraph(final boolean testOnly) {
        return getToolTipText(testOnly);
    }

    /** Returns list of menu items for the popup. */
    protected /*abstract*/ List<UpdatableItem> createPopup() {
        return null;
    }

    /** Returns popup object without updating. */
    public void hidePopup() {
        mPopupLock.lock();
        final JPopupMenu popup0 = popup;
        mPopupLock.unlock();
        if (popup0 != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    popup0.setVisible(false);
                }
            });
        }
    }

    /**
     * Returns the popup widget. The createPopup must be defined with menu
     * items.
     */
    public final JPopupMenu getPopup() {
        mPopupLock.lock();
        if (popup == null) {
            final List<UpdatableItem> items = createPopup();
            if (items != null) {
                registerAllMenuItems(items);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override public void run() {
                            popup = new JPopupMenu();
                        }
                    });
                } catch (final InterruptedException ix) {
                    Thread.currentThread().interrupt();
                } catch (final InvocationTargetException x) {
                    Tools.printStackTrace();
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        for (final UpdatableItem u : items) {
                            popup.add((JMenuItem) u);
                        }
                    }
                });
            }
        }
        final JPopupMenu popup0 = popup;
        mPopupLock.unlock();
        if (popup0 != null) {
            updateMenus(null);
        }
        return popup0;
    }

    /** Returns popup on the spefified position. */
    public final JPopupMenu getPopup(final Point2D pos) {
        mPopupLock.lock();
        if (popup == null) {
            final List<UpdatableItem> items = createPopup();
            if (items != null) {
                registerAllMenuItems(items);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override public void run() {
                            popup = new JPopupMenu();
                        }
                    });
                } catch (final InterruptedException ix) {
                    Thread.currentThread().interrupt();
                } catch (final InvocationTargetException x) {
                    Tools.printStackTrace();
                }
                for (final UpdatableItem u : items) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            popup.add((JMenuItem) u);
                        }
                    });
                }
            }
        }
        final JPopupMenu popup0 = popup;
        mPopupLock.unlock();
        if (popup0 != null) {
            updateMenus(pos);
        }
        return popup0;
    }

    /** Adds listener that deselects the toggle button, when the popup menu
        closes. */
    private void addPopupMenuListener(final JPopupMenu pm,
                                      final AbstractButton b) {
        pm.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuCanceled(final PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        b.setSelected(false);
                    }
                });
            }
            @Override public void popupMenuWillBecomeInvisible(
                                                    final PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        b.setSelected(false);
                    }
                });
            }
            @Override public void popupMenuWillBecomeVisible(
                                                    final PopupMenuEvent e) {
            }
        });
    }

    /** Show popup underneath the button. */
    private void showPopup(final JPopupMenu pm, final AbstractButton b) {
        int w = (int) pm.getBounds().getWidth();
        if (w == 0) {
            pm.show(b,
                    (int) b.getBounds().getWidth(),
                    (int) b.getBounds().getHeight());
            w = (int) pm.getBounds().getWidth();
        }
        pm.show(b,
                (int) (b.getBounds().getWidth() - w),
                (int) b.getBounds().getHeight());
        addPopupMenuListener(pm, b);
    }

    /** Returns the Action button. */
    final JToggleButton getActionsButton() {
        final JToggleButton b =
                      new JToggleButton(Tools.getString("Browser.ActionsMenu"),
                                        Browser.MENU_ICON);
        b.setToolTipText(Tools.getString("Browser.ActionsMenu"));
        Tools.makeMiniButton(b);
        b.addMouseListener(
            new MouseAdapter() {
                @Override public void mousePressed(final MouseEvent e) {
                    final JToggleButton source =
                                              (JToggleButton) (e.getSource());
                    if (source.isSelected()) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                b.setSelected(true);
                            }
                        });
                    } else {
                        final Thread thread = new Thread(new Runnable() {
                            @Override public void run() {
                                final JPopupMenu pm = getPopup();
                                if (pm != null) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            showPopup(pm, b);
                                        }
                                    });
                                }
                            }
                        });
                        thread.start();
                    }
                }
            }
        );
        return b;
    }

    /** Returns menu object. */
    public final JMenu getMenu() {
        return menu;
    }

    /** Force popup to be recreated. */
    protected final void resetPopup() {
        mPopupLock.lock();
        popup = null;
        mPopupLock.unlock();
    }

    /** Update menus with positions and calles their update methods. */
    void updateMenus(final Point2D pos) {
        mMenuListLock.lock();
        if (menuList == null) {
            mMenuListLock.unlock();
        } else {
            final List<UpdatableItem> menuListCopy =
                                       new ArrayList<UpdatableItem>(menuList);
            mMenuListLock.unlock();
            for (final UpdatableItem i : menuListCopy) {
                i.setPos(pos);
                i.update();
            }
            final int size = menuListCopy.size();
            if (size > maxMenuList) {
                maxMenuList = size;
                Tools.debug(this, "menu list size: " + maxMenuList, 2);
            }
        }
    }

    /** Registers all menu items. */
    final void registerAllMenuItems(
                               final List<UpdatableItem> allItemsAndSubitems) {
        mMenuListLock.lock();
        menuList = allItemsAndSubitems;
        mMenuListLock.unlock();
    }

    /** Returns units. */
    protected Unit[] getUnits() {
        return null;
    }

    /** Returns units. */
    protected Unit[] getTimeUnits() {
        return new Unit[]{
                   //new Unit("", "", "", ""),
                   new Unit("", "s", "Second", "Seconds"), /* default unit */
                   new Unit("msec", "ms", "Millisecond", "Milliseconds"),
                   new Unit("usec", "us", "Microsecond", "Microseconds"),
                   new Unit("sec",  "s",  "Second",      "Seconds"),
                   new Unit("min",  "m",  "Minute",      "Minutes"),
                   new Unit("hr",   "h",  "Hour",        "Hours")
       };
    }

    /** Adds mouse over listener. */
    protected final void addMouseOverListener(final Component c,
                                              final ButtonCallback bc) {
        if (bc == null) {
            return;
        }
        c.addMouseListener(new MouseListener() {
            @Override public void mouseClicked(final MouseEvent e) {
                /* do nothing */
            }

            @Override public void mouseEntered(final MouseEvent e) {
                if (c.isShowing() && c.isEnabled()) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            bc.mouseOver();
                        }
                    });
                    thread.start();
                }
            }

            @Override public void mouseExited(final MouseEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        bc.mouseOut();
                    }
                });
                t.start();
            }

            @Override public void mousePressed(final MouseEvent e) {
                mouseExited(e);
                /* do nothing */
            }

            @Override public void mouseReleased(final MouseEvent e) {
                /* do nothing */
            }
        });
    }

    /** Compares ignoring case. */
    @Override public int compareTo(final Object o) {
        return toString().compareToIgnoreCase(o.toString());
    }

    /** Retruns panel with table and border. */
    protected JComponent getTablePanel(final String title,
                                       final String tableName,
                                       final MyButton newButton) {
        final JPanel p = new JPanel();
        p.setBackground(Browser.PANEL_BACKGROUND);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        final TitledBorder titleBorder = Tools.getBorder(title);
        p.setBorder(titleBorder);
        final JTable table = getTable(tableName);
        if (table != null) {
            if (newButton != null) {
                final JPanel bp = new JPanel(
                                    new FlowLayout(FlowLayout.LEFT, 0, 0));
                bp.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
                bp.add(newButton);
                final Dimension d = bp.getPreferredSize();
                bp.setMaximumSize(new Dimension(Short.MAX_VALUE,
                                                (int) d.getHeight()));
                p.add(bp);
            }
            p.add(table.getTableHeader());
            p.add(table);
        }
        return p;
    }

    /**
     * Returns table. The table name can be whatever but should be unique if
     * more tables are used.
     */
    protected final JTable getTable(final String tableName) {
        final String[] colNames = getColumnNames(tableName);
        if (colNames != null && colNames.length > 0) {
            final Object[][] data = getTableData(tableName);
            final DefaultTableModel tableModel =
                new DefaultTableModel(data, colNames) {
                    /** Serial version uid. */
                    private static final long serialVersionUID = 1L;
                    public final boolean isCellEditable(final int r,
                                                        final int c) {
                        return false;
                    }
                };
            tableModels.put(tableName, tableModel);
            final MyButtonCellRenderer bcr = new MyButtonCellRenderer() {
                         /** Serial version uid. */
                         private static final long serialVersionUID = 1L;

                         /** Returns row color. */
                         public final Color getRowColor(final String key) {
                             return getTableRowColor(tableName, key);
                         }

                         /** Returns alignment of the column. */
                         public final int getColumnAlignment(final int column) {
                             return getTableColumnAlignment(tableName, column);
                         }
                     };
            final JTable table = new JTable(tableModel) {
                /** Serial version uid. */
                private static final long serialVersionUID = 1L;
                /** Overriding so that jlabels show up. */
                public Class getColumnClass(final int c) {
                    final Object o = getValueAt(0, c);
                    if (o == null) {
                        return super.getColumnClass(c);
                    }
                    return o.getClass();
                }

                public TableCellRenderer getCellRenderer(final int row,
                                                         final int column) {
                    if (column == 0 || isControlButton(tableName, column)) {
                        return bcr;
                    }
                    return super.getCellRenderer(row, column);
                }

                public String getToolTipText(final MouseEvent me) {
                    int row = rowAtPoint(me.getPoint());
                    int column = columnAtPoint(me.getPoint());
                    try {
                        final String key =
                                   ((MyButton) getValueAt(row, 0)).getText();
                        final Object o = getValueAt(row, column);
                        return getTableToolTip(tableName, key, o, row, column);
                    } catch (final java.lang.IndexOutOfBoundsException e) {
                        /* could be removed in the meantime, ignoring. */
                    }
                    return null;
                }

            };
            tables.put(tableName, table);
            final TableRowSorter<DefaultTableModel> sorter =
                        new TableRowSorter<DefaultTableModel>(tableModel);
            for (int i = 0; i < colNames.length; i++) {
                final Comparator<Object> c =
                                               getColComparator(tableName, i);
                if (c != null) {
                    sorter.setComparator(i, c);
                }
            }
            table.setRowSorter(sorter);
            sorter.setSortsOnUpdates(true);
            table.getTableHeader().setReorderingAllowed(true);
            table.setBackground(Browser.PANEL_BACKGROUND);
            table.setDefaultRenderer(
                     Object.class,
                     new MyCellRenderer() {
                         /** Serial version uid. */
                         private static final long serialVersionUID = 1L;

                         /** Returns row color. */
                         public final Color getRowColor(final String key) {
                             return getTableRowColor(tableName, key);
                         }

                         /** Returns alignment of the column. */
                         public final int getColumnAlignment(final int column) {
                             return getTableColumnAlignment(tableName, column);
                         }
                     });
            final int h = getRowHeight();
            if (h >= 0) {
                table.setRowHeight(h);
            }
            table.addMouseMotionListener(new MouseMotionListener() {
                private int row;

                @Override public void mouseMoved(final MouseEvent me) {
                   final Point p = me.getPoint();
                   final int newRow = table.rowAtPoint(p);
                   if (row >= 0 && newRow != row) {
                       try {
                           for (int c = 0; c < table.getColumnCount(); c++) {
                               final Object v = table.getValueAt(row, c);
                               if (v instanceof MyButton) {
                                   ((MyButton) v).getModel().setRollover(false);
                                   table.setValueAt(v, row, c);
                               }
                           }
                       } catch (final java.lang.IndexOutOfBoundsException e) {
                           /* could be removed in the meantime, ignoring. */
                       }
                   }
                   if (newRow >= 0 && newRow != row) {
                       row = newRow;
                       for (int c = 0; c < table.getColumnCount(); c++) {
                           final Object v = table.getValueAt(row, c);
                           if (v instanceof MyButton) {
                               ((MyButton) v).getModel().setRollover(true);
                               table.setValueAt(v, row, c);
                           }
                       }
                   }
                }
                @Override public void mouseDragged(final MouseEvent me) {
                    /* nothing */
                }
            });
            table.addMouseListener(new MouseAdapter() {
                private int row;
                private boolean paintIt = false;
                private boolean paintItMouseOver = false;

                @Override public final void mouseClicked(final MouseEvent e) {
                    if (e.getClickCount() > 1
                        || SwingUtilities.isRightMouseButton(e)) {
                        return;
                    }
                    final JTable table = (JTable) e.getSource();
                    final Point p = e.getPoint();
                    final int row = table.rowAtPoint(p);
                    final int column = table.columnAtPoint(p);
                    final MyButton keyB = (MyButton) table.getValueAt(row, 0);
                    rowClicked(tableName, keyB.getText(), column);
                }

                @Override public final void mousePressed(final MouseEvent e) {
                    final JTable table = (JTable) e.getSource();
                    final Point p = e.getPoint();
                    row = table.rowAtPoint(p);
                    final MyButton b = (MyButton) table.getValueAt(row, 0);
                    if (SwingUtilities.isRightMouseButton(e)) {
                        final Info info = getTableInfo(tableName, b.getText());
                        if (info != null) {
                            info.showPopup(table, e.getX(), e.getY());
                        }
                        return;
                    }
                    for (int c = 0; c < table.getColumnCount(); c++) {
                        final Object v = table.getValueAt(row, c);
                        if (v instanceof MyButton) {
                            ((MyButton) v).getModel().setPressed(true);
                            ((MyButton) v).getModel().setArmed(true);
                            table.setValueAt(v, row, c);
                        }
                    }
                    paintIt = true;
                }

                @Override public final void mouseReleased(final MouseEvent e) {
                    if (paintIt) {
                        for (int c = 0; c < table.getColumnCount(); c++) {
                            final Object v = table.getValueAt(row, c);
                            if (v instanceof MyButton) {
                                ((MyButton) v).getModel().setPressed(false);
                                ((MyButton) v).getModel().setArmed(false);
                                table.setValueAt(v, row, c);
                            }
                        }
                    }
                    paintIt = false;
                }

                @Override public final void mouseEntered(final MouseEvent e) {
                    final JTable table = (JTable) e.getSource();
                    final Point p = e.getPoint();
                    final int row = table.rowAtPoint(p);
                    try {
                        for (int c = 0; c < table.getColumnCount(); c++) {
                            final Object v = table.getValueAt(row, c);
                            if (v instanceof MyButton) {
                                ((MyButton) v).getModel().setRollover(true);
                                table.setValueAt(v, row, c);
                            }
                        }
                        paintItMouseOver = true;
                    } catch (final java.lang.IndexOutOfBoundsException ie) {
                        /* could be removed in the meantime, ignoring. */
                    }
                }

                @Override public final void mouseExited(final MouseEvent e) {
                    if (paintItMouseOver) {
                        for (int i = 0; i < table.getRowCount(); i++) {
                            for (int c = 0; c < table.getColumnCount(); c++) {
                                final Object v = table.getValueAt(i, c);
                                if (v instanceof MyButton) {
                                    ((MyButton) v).getModel().setRollover(
                                                                        false);
                                    table.setValueAt(v, i, c);
                                }
                            }
                        }
                    }
                    paintItMouseOver = false;
                }
            });
            Tools.resizeTable(table, getDefaultWidths(tableName));
            return table;
        }
        return null;
    }


    /** Alignment for the specified column. */
    protected int getTableColumnAlignment(final String tableName,
                                          final int column) {
        return SwingConstants.LEFT;
    }

    /** Returns info object for this row. */
    protected Info getTableInfo(final String tableName, final String key) {
        return null;
    }

    /** Returns columns for the table. */
    protected String[] getColumnNames(final String tableName) {
        return new String[]{};
    }

    /** Returns data for the table. */
    protected Object[][] getTableData(final String tableName) {
        return new Object[][]{};
    }

    /** Updates data in the table. */
    public final void updateTable(final String tableName) {
        Tools.debug(this, "update table: " + tableName, 2);
        final JTable table = tables.get(tableName);
        final DefaultTableModel tableModel = tableModels.get(tableName);
        if (tableModel != null) {
            final String[] colNames = getColumnNames(tableName);
            if (colNames != null && colNames.length > 0) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        final Object[][] data = getTableData(tableName);
                        Tools.debug(this, "update table in: " + getName(), 1);
                        tableModel.setDataVector(data, colNames);
                        tableModel.fireTableDataChanged();
                        Tools.resizeTable(table, getDefaultWidths(tableName));
                    }
                });
            }
        }
    }

    /** Execute when row in the table was clicked. */
    protected void rowClicked(final String tableName,
                              final String key,
                              final int column) {
        /* do nothing */
    }

    /** Returns row height for the table. */
    protected final int getRowHeight() {
        return 40;
    }

    /** Retrurns color for some rows. */
    protected Color getTableRowColor(final String tableName, final String key) {
        return null;
    }

    /** Returns comparator for column. */
    protected Comparator<Object> getColComparator(final String tableName,
                                                  final int col) {
        return null;
    }

    /** Returns default widths for columns. Null for computed width. */
    protected Map<Integer, Integer> getDefaultWidths(final String tableName) {
        return null;
    }

    /** Returns default widths for columns. Null for computed width. */
    protected boolean isControlButton(final String tableName,
                                      final int column) {
        return false;
    }

    /** Returns tool tip text in the table. */
    protected String getTableToolTip(final String tableName,
                                     final String key,
                                     final Object object,
                                     final int raw,
                                     final int column) {
        if (object instanceof MyButton) {
            return ((MyButton) object).getText();
        }
        return object.toString();
    }

    /** Remove node in tree menu. Call it from swing thread. */
    final void removeNode() {
        final DefaultMutableTreeNode n = node;
        node = null;
        if (n == null) {
            return;
        }
        final DefaultMutableTreeNode p = (DefaultMutableTreeNode) n.getParent();
        if (p != null) {
            p.remove(n);
            getBrowser().reload(p, true);
        }
    }

    /** Returns the main text that appears in the graph. */
    public String getMainTextForGraph() {
        return toString();
    }

}
