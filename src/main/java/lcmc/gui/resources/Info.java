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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import lcmc.gui.GUIData;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Value;
import lcmc.model.resources.Resource;
import lcmc.gui.Browser;
import lcmc.gui.widget.Widget;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.ComponentWithTest;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.MyButtonCellRenderer;
import lcmc.utilities.MyCellRenderer;
import lcmc.utilities.MyMenu;
import lcmc.utilities.Tools;
import lcmc.utilities.Unit;
import lcmc.utilities.UpdatableItem;

/**
 * This class holds info data for resources, services, hosts, clusters
 * etc. It provides methods to show this info and graphical view if
 * available.
 */
public class Info implements Comparable<Info>, Value {
    private static final Logger LOG = LoggerFactory.getLogger(Info.class);
    /** Amount of frames per second. */
    private static final float FPS = Tools.getApplication().getAnimFPS();
    public static final ImageIcon LOGFILE_ICON = Tools.createImageIcon(Tools.getDefault("Info.LogIcon"));
    /** Menu node of this object. */
    private DefaultMutableTreeNode node = null;
    /** Name of the object. */
    private String name;
    /** Resource object as found in data/resources associated with this object. */
    private Resource resource;
    /** TODL: Checking for leak. */
    private int maxMenuList = 0;

    /** Area with text info.  */
    private JEditorPane resourceInfoArea;

    /** Map from parameter to its user-editable widget. */
    private final Map<String, Widget> widgetHash = Collections.synchronizedMap(new HashMap<String, Widget>());
    /** popup menu for this object. */
    private JPopupMenu popup;
    private final Lock mPopupLock = new ReentrantLock();
    private final Lock mMenuListLock = new ReentrantLock();
    private List<UpdatableItem> menuList = new ArrayList<UpdatableItem>();
    /** Whether the info object is being updated. */
    private boolean updated = false;
    private double animationIndex = 0;
    private String infoCache = "";
    private Browser browser;
    private final Map<String, JTable> tables = new HashMap<String, JTable>();
    private final Map<String, DefaultTableModel> tableModels = new HashMap<String, DefaultTableModel>();
    /** Hash from component to the edit access mode. */
    private final Map<JTextComponent, AccessMode> componentToEditAccessMode =
                                                                new HashMap<JTextComponent, AccessMode>();
    /** Hash from component to the enable access mode. */
    private final Map<JComponent, AccessMode> componentToEnableAccessMode = new HashMap<JComponent, AccessMode>();

    public void init(final String name, final Browser browser) {
        this.name = name;
        this.browser = browser;
    }

    /**
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

    public Browser getBrowser() {
        return browser;
    }

    public String getToolTipText(final Application.RunMode runMode) {
        return "no tooltip";
    }

    protected final void setResource(final Resource resource) {
        this.resource = resource;
    }

    /** Adds the widget for parameter. */
    protected final void widgetAdd(final String param, final String prefix, final Widget paramWi) {
        if (prefix == null) {
            widgetHash.put(param, paramWi);
        } else {
            widgetHash.put(prefix + ':' + param, paramWi);
        }
    }

    public final Widget getWidget(final String param, final String prefix) {
        if (prefix == null) {
            return widgetHash.get(param);
        } else {
            return widgetHash.get(prefix + ':' + param);
        }
    }

    /** Removes the parameter from the widget hash. */
    public final Widget widgetRemove(final String param, final String prefix) {
        if (prefix == null) {
            if (widgetHash.containsKey(param)) {
                widgetHash.get(param).cleanup();
                return widgetHash.remove(param);
            }
            return null;
        } else {
            if (widgetHash.containsKey(prefix + ':' + param)) {
                widgetHash.get(prefix + ':' + param).cleanup();
                return widgetHash.remove(prefix + ':' + param);
            }
            return null;
        }
    }

    /** Clears the whole widget hash. */
    public final void widgetClear() {
        for (final Map.Entry<String, Widget> widgetEntry : widgetHash.entrySet()) {
            widgetEntry.getValue().cleanup();
        }
        widgetHash.clear();
    }

    protected void setTerminalPanel() {
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

    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return null;
    }

    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        return null;
    }

    /** Updates the info text. */
    public void updateInfo() {
        final JEditorPane ria = resourceInfoArea;
        if (ria != null) {
            final String newInfo = getInfo();
            if (newInfo != null && !newInfo.equals(infoCache)) {
                infoCache = newInfo;
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
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
    protected String getInfoMimeType() {
        return GUIData.MIME_TYPE_TEXT_PLAIN;
    }

    protected JComponent getBackButton() {
        return null;
    }

    public JComponent getInfoPanel() {
        final String info = getInfo();
        resourceInfoArea = null;
        if (info == null) {
            final JPanel panel = new JPanel();
            panel.setBackground(Browser.PANEL_BACKGROUND);
            return panel;
        } else {
            final Font f = new Font("Monospaced", Font.PLAIN, Tools.getApplication().scaled(12));
            resourceInfoArea = new JEditorPane(getInfoMimeType(), info);
            resourceInfoArea.setMinimumSize(new Dimension(
                                                    Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                                                    Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
            resourceInfoArea.setPreferredSize(new Dimension(
                                                    Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                                                    Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
            resourceInfoArea.setEditable(false);
            resourceInfoArea.setFont(f);
            resourceInfoArea.setBackground(Browser.PANEL_BACKGROUND);
            updateInfo(resourceInfoArea);
        }
        final JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));
        infoPanel.setBackground(Browser.PANEL_BACKGROUND);
        final JComponent backButton = getBackButton();
        if (backButton != null) {
            infoPanel.add(backButton);
        }
        infoPanel.add(new JScrollPane(resourceInfoArea));
        return infoPanel;
    }

    /** TODO: clears info panel cache most of the time. */
    public boolean selectAutomaticallyInTreeMenu() {
        return false;
    }

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

    /** Returns name of the object. */
    @Override
    public String toString() {
        return name;
    }

    /** Returns name of this resource. */
    public String getName() {
        return name;
    }

    /** Gets node of this resource or service. */
    public final DefaultMutableTreeNode getNode() {
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
            final Iterable<UpdatableItem> menuListCopy = new ArrayList<UpdatableItem>(menuList);
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
    public void removeMyself(final Application.RunMode runMode) {
        cleanup();
    }

    /** Selects and highlights this node. */
    public void selectMyself() {
        // this fires an event in ViewPanel.
        final DefaultMutableTreeNode n = node;
        if (n != null) {
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    getBrowser().reloadAndWait(n, true);
                }
            });
            getBrowser().nodeChanged(n);
        }
    }

    /** Returns resource object. */
    public Resource getResource() {
        return resource;
    }

    /**
     * this method is used for values that are stored and can be different
     * than their appearance, which is taken from toString() method.
     * they are usually the same as in toString() method, but e.g. in
     * combo boxes they can be different. It trims the result too.
     */
    public String getInternalValue() {
        if (name != null) {
            return name.trim();
        }
        return null;
    }

    /** Shows the popup on the specified coordinates. */
    public final void showPopup(final JComponent c, final int x, final int y) {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final JPopupMenu pm = getPopup();
                if (pm != null) {
                    updateMenus(null);
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (!c.isShowing() || !c.isDisplayable()) {
                                return;
                            }
                            pm.show(c, x, y);
                        }
                    });
                }
            }
        });
        thread.start();
    }

    /** Returns tooltip for the object in the graph. */
    public String getToolTipForGraph(final Application.RunMode runMode) {
        return getToolTipText(runMode);
    }

    /** Returns list of menu items for the popup. */
    protected List<UpdatableItem> createPopup() {
        return null;
    }

    /** Returns popup object without updating. */
    public void hidePopup() {
        mPopupLock.lock();
        final JPopupMenu popup0;
        try {
            popup0 = popup;
        } finally {
            mPopupLock.unlock();
        }
        if (popup0 != null) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
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
        try {
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    if (popup == null) {
                        final List<UpdatableItem> items = createPopup();
                        if (items != null) {
                            registerAllMenuItems(items);
                            popup = new JPopupMenu();
                            for (final UpdatableItem u : items) {
                                popup.add((JMenuItem) u);
                            }
                        }
                    }
                }
            });
            return popup;
        } finally {
            mPopupLock.unlock();
        }
    }

    /** Adds listener that deselects the toggle button, when the popup menu closes. */
    private void addPopupMenuListener(final JPopupMenu pm, final AbstractButton b) {
        pm.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
                Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                    @Override
                    public void run() {
                        b.setSelected(false);
                    }
                });
            }
            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                    @Override
                    public void run() {
                        b.setSelected(false);
                    }
                });
            }
            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
            }
        });
    }

    /** Show popup underneath the button. */
    private void showPopup(final JPopupMenu pm, final AbstractButton b) {
        if (!b.isShowing() || !b.isDisplayable()) {
            return;
        }
        int w = (int) pm.getBounds().getWidth();
        if (w == 0) {
            pm.show(b, (int) b.getBounds().getWidth(), (int) b.getBounds().getHeight());
            w = (int) pm.getBounds().getWidth();
        }
        pm.show(b, (int) (b.getBounds().getWidth() - w), (int) b.getBounds().getHeight());
        addPopupMenuListener(pm, b);
    }

    /** Returns the Action button. */
    protected final JToggleButton getActionsButton() {
        final JToggleButton b = new JToggleButton(Tools.getString("Browser.ActionsMenu"), Browser.ACTIONS_MENU_ICON);
        b.setToolTipText(Tools.getString("Browser.ActionsMenu"));
        Tools.makeMiniButton(b);
        b.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    final JToggleButton source = (JToggleButton) (e.getSource());
                    if (source.isSelected()) {
                        Tools.invokeLater(new Runnable() {
                        @Override
                            public void run() {
                                b.setSelected(true);
                            }
                        });
                    } else {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final JPopupMenu pm = getPopup();
                                if (pm != null) {
                                    updateMenus(null);
                                    Tools.invokeLater(new Runnable() {
                                    @Override
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

    public final JMenu getMenu() {
        return null;
    }

    protected final void resetPopup() {
        mPopupLock.lock();
        try {
            popup = null;
        } finally {
            mPopupLock.unlock();
        }
    }

    /** Update menus with positions and calles their update methods. */
    public void updateMenus(final Point2D pos) {
        Tools.isNotSwingThread();
        mMenuListLock.lock();
        if (menuList == null) {
            mMenuListLock.unlock();
        } else {
            final Collection<UpdatableItem> menuListCopy = new ArrayList<UpdatableItem>(menuList);
            mMenuListLock.unlock();
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    for (final UpdatableItem i : menuListCopy) {
                        i.setPos(pos);
                        if (i instanceof MyMenu) {
                            i.setEnabled(false);
                        } else {
                            i.updateAndWait();
                        }
                    }
                }
            });
            for (final UpdatableItem i : menuListCopy) {
                if (i instanceof MyMenu) {
                    Tools.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            i.updateAndWait();
                        }
                    });
                }
            }
            final int size = menuListCopy.size();
            if (size > maxMenuList) {
                maxMenuList = size;
                LOG.debug2("updateMenus: menu list size: " + maxMenuList);
            }
        }
    }

    /** Registers all menu items. */
    final void registerAllMenuItems(final List<UpdatableItem> allItemsAndSubitems) {
        mMenuListLock.lock();
        try {
            menuList = allItemsAndSubitems;
        } finally {
            mMenuListLock.unlock();
        }
    }

    /** Returns units. */
    protected Unit[] getUnits(final String param) {
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
    public final void addMouseOverListener(final ComponentWithTest c, final ButtonCallback bc) {
        if (bc == null) {
            return;
        }
        ((Component) c).addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                /* do nothing */
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                if (((Component) c).isShowing() && ((Component) c).isEnabled()) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            bc.mouseOver(c);
                        }
                    });
                    thread.start();
                }
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        bc.mouseOut(c);
                    }
                });
                t.start();
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                mouseExited(e);
                /* do nothing */
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                /* do nothing */
            }
        });
    }

    @Override
    public int compareTo(final Info o) {
        return Tools.compareNames(toString(), o.toString());
    }

    /** Retruns panel with table and border. */
    protected JComponent getTablePanel(final String title, final String tableName, final MyButton newButton) {
        final JPanel p = new JPanel();
        p.setBackground(Browser.PANEL_BACKGROUND);
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
        final TitledBorder titleBorder = Tools.getBorder(title);
        p.setBorder(titleBorder);
        final JTable table = getTable(tableName);
        if (table != null) {
            if (newButton != null) {
                final JPanel bp = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
                bp.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
                bp.add(newButton);
                final Dimension d = bp.getPreferredSize();
                bp.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) d.getHeight()));
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
            final DefaultTableModel tableModel = new DefaultTableModel(data, colNames) {
                @Override
                    public boolean isCellEditable(final int row, final int column) {
                        return false;
                    }
                };
            tableModels.put(tableName, tableModel);
            final TableCellRenderer bcr = new MyButtonCellRenderer() {
                         @Override
                         public Color getRowColor(final String key) {
                             return getTableRowColor(tableName, key);
                         }

                         @Override
                         public int getColumnAlignment(final int column) {
                             return getTableColumnAlignment(tableName, column);
                         }
                     };
            final JTable table = new JTable(tableModel) {
                /** Overriding so that jlabels show up. */
                @Override
                public Class getColumnClass(final int column) {
                    final Object o = getValueAt(0, column);
                    if (o == null) {
                        return super.getColumnClass(column);
                    }
                    return o.getClass();
                }

                @Override
                public TableCellRenderer getCellRenderer(final int row, final int column) {
                    if (column == 0 || isControlButton(tableName, column)) {
                        return bcr;
                    }
                    return super.getCellRenderer(row, column);
                }

                @Override
                public String getToolTipText(final MouseEvent event) {
                    final int row = rowAtPoint(event.getPoint());
                    final int column = columnAtPoint(event.getPoint());
                    try {
                        final String key = ((AbstractButton) getValueAt(row, 0)).getText();
                        final Object o = getValueAt(row, column);
                        return getTableToolTip(tableName, key, o, row, column);
                    } catch (final IndexOutOfBoundsException e) {
                        /* could be removed in the meantime, ignoring. */
                    }
                    return null;
                }

            };
            tables.put(tableName, table);
            final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(tableModel);
            for (int i = 0; i < colNames.length; i++) {
                final Comparator<Object> c = getColComparator(tableName, i);
                if (c != null) {
                    sorter.setComparator(i, c);
                }
            }
            table.setRowSorter(sorter);
            sorter.setSortsOnUpdates(true);
            table.getTableHeader().setReorderingAllowed(true);
            table.setBackground(Browser.PANEL_BACKGROUND);
            table.setDefaultRenderer(Object.class,
                                     new MyCellRenderer() {
                                         @Override
                                         public Color getRowColor(final String key) {
                                             return getTableRowColor(tableName, key);
                                         }

                                         @Override
                                         public int getColumnAlignment(final int column) {
                                             return getTableColumnAlignment(tableName, column);
                                         }
                                     });
            final int h = getRowHeight();
            if (h >= 0) {
                table.setRowHeight(h);
            }
            table.addMouseMotionListener(new MouseMotionListener() {
                private int row;

                @Override
                public void mouseMoved(final MouseEvent me) {
                   final Point p = me.getPoint();
                   final int newRow = table.rowAtPoint(p);
                   if (row >= 0 && newRow != row) {
                       try {
                           for (int c = 0; c < table.getColumnCount(); c++) {
                               final Object v = table.getValueAt(row, c);
                               if (v instanceof MyButton) {
                                   ((AbstractButton) v).getModel().setRollover(false);
                                   table.setValueAt(v, row, c);
                               }
                           }
                       } catch (final IndexOutOfBoundsException e) {
                           /* could be removed in the meantime, ignoring. */
                       }
                   }
                   if (newRow >= 0 && newRow != row) {
                       row = newRow;
                       for (int c = 0; c < table.getColumnCount(); c++) {
                           final Object v = table.getValueAt(row, c);
                           if (v instanceof MyButton) {
                               ((AbstractButton) v).getModel().setRollover(true);
                               table.setValueAt(v, row, c);
                           }
                       }
                   }
                }

                @Override
                public void mouseDragged(final MouseEvent e) {
                    /* nothing */
                }
            });
            table.addMouseListener(new MouseAdapter() {
                private int row;
                private boolean paintIt = false;
                private boolean paintItMouseOver = false;

                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (e.getClickCount() > 1 || SwingUtilities.isRightMouseButton(e)) {
                        return;
                    }
                    final JTable thisTable = (JTable) e.getSource();
                    final Point p = e.getPoint();
                    final int row0 = thisTable.rowAtPoint(p);
                    final int column = thisTable.columnAtPoint(p);
                    final MyButton keyB = (MyButton) thisTable.getValueAt(row0, 0);
                    rowClicked(tableName, keyB.getText(), column);
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                    final JTable thisTable = (JTable) e.getSource();
                    final Point p = e.getPoint();
                    row = thisTable.rowAtPoint(p);
                    final MyButton b = (MyButton) thisTable.getValueAt(row, 0);
                    if (SwingUtilities.isRightMouseButton(e)) {
                        final Info info = getTableInfo(tableName, b.getText());
                        if (info != null) {
                            info.showPopup(thisTable, e.getX(), e.getY());
                        }
                        return;
                    }
                    for (int c = 0; c < thisTable.getColumnCount(); c++) {
                        final Object v = thisTable.getValueAt(row, c);
                        if (v instanceof MyButton) {
                            ((AbstractButton) v).getModel().setPressed(true);
                            ((AbstractButton) v).getModel().setArmed(true);
                            thisTable.setValueAt(v, row, c);
                        }
                    }
                    paintIt = true;
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    if (paintIt) {
                        for (int c = 0; c < table.getColumnCount(); c++) {
                            final Object v = table.getValueAt(row, c);
                            if (v instanceof MyButton) {
                                ((AbstractButton) v).getModel().setPressed(false);
                                ((AbstractButton) v).getModel().setArmed(false);
                                table.setValueAt(v, row, c);
                            }
                        }
                    }
                    paintIt = false;
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                    final JTable thisTable = (JTable) e.getSource();
                    final Point p = e.getPoint();
                    final int row0 = thisTable.rowAtPoint(p);
                    try {
                        for (int c = 0; c < thisTable.getColumnCount(); c++) {
                            final Object v = thisTable.getValueAt(row0, c);
                            if (v instanceof MyButton) {
                                ((AbstractButton) v).getModel().setRollover(true);
                                thisTable.setValueAt(v, row0, c);
                            }
                        }
                        paintItMouseOver = true;
                    } catch (final IndexOutOfBoundsException ie) {
                        /* could be removed in the meantime, ignoring. */
                    }
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    if (paintItMouseOver) {
                        for (int i = 0; i < table.getRowCount(); i++) {
                            for (int c = 0; c < table.getColumnCount(); c++) {
                                final Object v = table.getValueAt(i, c);
                                if (v instanceof MyButton) {
                                    ((AbstractButton) v).getModel().setRollover(false);
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


    protected int getTableColumnAlignment(final String tableName, final int column) {
        return SwingConstants.LEFT;
    }

    /** Returns info object for this row. */
    protected Info getTableInfo(final String tableName, final String key) {
        return null;
    }

    protected String[] getColumnNames(final String tableName) {
        return new String[]{};
    }

    protected Object[][] getTableData(final String tableName) {
        return new Object[][]{};
    }

    public final void updateTable(final String tableName) {
        LOG.debug2("updateTable: " + tableName);
        final JTable table = tables.get(tableName);
        final DefaultTableModel tableModel = tableModels.get(tableName);
        if (tableModel != null) {
            final String[] colNames = getColumnNames(tableName);
            if (colNames != null && colNames.length > 0) {
                Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                    @Override
                    public void run() {
                        final Object[][] data = getTableData(tableName);
                        LOG.debug2("updateTable: in: " + getName());
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

    protected final int getRowHeight() {
        return 40;
    }

    protected Color getTableRowColor(final String tableName, final String key) {
        return null;
    }

    /** Returns comparator for column. */
    protected Comparator<Object> getColComparator(final String tableName, final int col) {
        return null;
    }

    /** Returns default widths for columns. Null for computed width. */
    protected Map<Integer, Integer> getDefaultWidths(final String tableName) {
        return null;
    }

    /** Returns default widths for columns. Null for computed width. */
    protected boolean isControlButton(final String tableName, final int column) {
        return false;
    }

    /** Returns tool tip text in the table. */
    protected String getTableToolTip(final String tableName,
                                     final String key,
                                     final Object object,
                                     final int raw,
                                     final int column) {
        if (object instanceof MyButton) {
            return ((AbstractButton) object).getText();
        }
        return object.toString();
    }

    /** Remove node in tree menu. Call it from swing thread. */
    protected final void removeNodeAndWait() {
        Tools.isSwingThread();
        final DefaultMutableTreeNode n = node;
        node = null;
        if (n == null) {
            return;
        }
        final MutableTreeNode p = (MutableTreeNode) n.getParent();
        if (p != null) {
            p.remove(n);
            getBrowser().reloadAndWait(p, true);
        }
    }

    protected final void removeNode() {
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                removeNodeAndWait();
            }
        });
    }

    /** Returns the main text that appears in the graph. */
    public String getMainTextForGraph() {
        return toString();
    }

    protected void registerComponentEditAccessMode(final JTextComponent component, final AccessMode mode) {
        componentToEditAccessMode.put(component, mode);
    }

    protected void registerComponentEnableAccessMode(final JComponent component, final AccessMode mode) {
        componentToEnableAccessMode.put(component, mode);
    }

    /** Process access lists. TODO: rename.*/
    public void updateAdvancedPanels() {
        for (final Map.Entry<JComponent, AccessMode> componentEntry : componentToEnableAccessMode.entrySet()) {
            final boolean accessible = Tools.getApplication().isAccessible(componentEntry.getValue());
            componentEntry.getKey().setEnabled(accessible);
        }
        for (final Map.Entry<JTextComponent, AccessMode> componentEntry : componentToEditAccessMode.entrySet()) {
            final boolean accessible = Tools.getApplication().isAccessible(componentEntry.getValue());
            componentEntry.getKey().setEditable(accessible);
        }
    }
 
    @Override
    public String getValueForConfig() {
        return name;
    }

    @Override
    public final String getValueForGui() {
        return toString();
    }

    @Override
    public final String getValueForConfigWithUnit() {
        return getValueForConfig();
    }

    @Override
    public Unit getUnit() {
        return null;
    }

    @Override
    public boolean isNothingSelected() {
        return name == null;
    }

    @Override
    public String getNothingSelected() {
        return NOTHING_SELECTED;
    }
}
