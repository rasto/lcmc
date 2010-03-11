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
import drbd.data.resources.Resource;
import drbd.utilities.ButtonCallback;
import drbd.utilities.Unit;
import drbd.utilities.Tools;
import drbd.utilities.UpdatableItem;
import drbd.utilities.MyCellRenderer;
import drbd.utilities.MyButtonCellRenderer;
import drbd.utilities.MyButton;

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
import javax.swing.RowSorter;
import javax.swing.BoxLayout;

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
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseListener;
import java.awt.Point;
import java.awt.Color;
import java.awt.event.MouseMotionListener;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * This class holds info data for resources, services, hosts, clusters
 * etc. It provides methods to show this info and graphical view if
 * available.
 */
public class Info implements Comparable {
    /** Menu node of this object. */
    private DefaultMutableTreeNode node;
    /** Name of the object. */
    private String name;
    /** Resource object as found in data/resources associated with this
     * object. */
    private Resource resource;
    /** Amount of frames per second. */
    private static final float FPS = Tools.getConfigData().getAnimFPS();

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
    /** Menu list lock. */
    private final Mutex mMenuListLock = new Mutex();
    /** list of items in the menu for this object. */
    private final List<UpdatableItem> menuList = new ArrayList<UpdatableItem>();
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

    /**
     * Prepares a new <code>Info</code> object.
     *
     * @param name
     *      name that will be shown to the user
     */
    public Info(final String name, final Browser browser) {
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

    /**
     * Returns the id of this object which is the name.
     */
    public String getId() {
        return name;
    }

    /**
     * Returns browser object of this info.
     */
    protected Browser getBrowser() {
        return browser;
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
        /* set terminal panel, or don't */
    }

    /**
     * Returns whether the info object is being updated. This can be used
     * for animations.
     */
    protected final boolean isUpdated() {
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
            if (newInfo != null && !newInfo.equals(infoCache)) {
                infoCache = newInfo;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        resourceInfoArea.setText(newInfo);
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

    /**
     * Returns type of the info text. text/plain or text/html.
     */
    protected String getInfoType() {
        return Tools.MIME_TYPE_TEXT_PLAIN;
    }

    /**
     * Returns back button.
     */
    protected JComponent getBackButton() {
        return null;
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
    public final void setNode(final DefaultMutableTreeNode node) {
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
                getBrowser().reload(parent);
            }
        }
    }

    /**
     * Selects and highlights this node.
     */
    public void selectMyself() {
        // this fires an event in ViewPanel.
        getBrowser().reload(node);
        getBrowser().nodeChanged(node);
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
            final List<UpdatableItem> items = createPopup();
            if (items != null) {
                popup = new JPopupMenu();
                for (final UpdatableItem u : items) {
                    popup.add((JMenuItem) u);
                }
            }
            updateMenus(null);
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
            final List<UpdatableItem> items = createPopup();
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
            menu = new JMenu(name) {
                /** Serial version uid. */
                private static final long serialVersionUID = 1L;
                /**
                 * Overloaded in order to paint the background.
                 */
                protected final void paintComponent(final Graphics g) {
                    final Color color1 = Color.WHITE;
                    final Color color2 = Browser.STATUS_BACKGROUND;
                    if (!isEnabled() || getModel().isPressed()) {
                        super.paintComponent(g);
                        return;
                    }

                    final Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    final GradientPaint gp1 = new GradientPaint(
                                                   1.0f,
                                                   (float) getHeight(),
                                                   color2,
                                                   1.0f,
                                                   (float) getHeight() * 0.3f,
                                                   color1);
                    final GradientPaint gp2 = new GradientPaint(
                                                   1.0f,
                                                   0.0f,
                                                   color2,
                                                   1.0f,
                                                   (float) getHeight() * 0.3f,
                                                   color1);
                    final Rectangle2D.Float rf = new Rectangle2D.Float(
                                                   0.0f,
                                                   0.0f,
                                                   (float) getWidth() - 1,
                                                   (float) getHeight() - 1);
                    final Rectangle2D.Float rf1 = new Rectangle2D.Float(
                                                   0.0f,
                                                   (float) getHeight() * 0.3f,
                                                   (float) getWidth(),
                                                   (float) getHeight());
                    final Rectangle2D.Float rf2 = new Rectangle2D.Float(
                                                   0.0f,
                                                   0.0f,
                                                   (float) getWidth(),
                                                   (float) getHeight() * 0.3f);
                    g2.setPaint(gp1);
                    g2.fill(rf1);
                    g2.setPaint(gp2);
                    g2.fill(rf2);
                    final Color c = g2.getColor();
                    g2.setColor(Color.GRAY);
                    g2.draw(rf);
                    g2.setColor(c);

                    super.paintComponent(g);
                }
            };
            final Thread thread = new Thread(new Runnable() {
                public void run() {
                    menu.setIcon(Browser.ACTIONS_ICON);
                    menu.setBackground(Browser.STATUS_BACKGROUND);
                    final List<UpdatableItem> items = createPopup();
                    if (items != null) {
                        for (final UpdatableItem u : items) {
                            menu.add((JMenuItem) u);
                        }
                    }
                    menu.addItemListener(
                        new ItemListener() {
                            public void itemStateChanged(final ItemEvent e) {
                                if (e.getStateChange() == ItemEvent.SELECTED) {
                                    final Thread t = new Thread(new Runnable() {
                                        public void run() {
                                            updateMenus(null);
                                        }
                                    });
                                    t.start();
                                }
                            }
                        });
                    updateMenus(null);
                }
            });
            thread.start();
        } else {
            final Thread thread = new Thread(new Runnable() {
                public void run() {
                    updateMenus(null);
                }
            });
            thread.start();
        }
        return menu;
    }

    /**
     * Force popup to be recreated.
     */
    protected final void resetPopup() {
        popup = null;
    }

    /**
     * Update menus with positions and calles their update methods.
     */
    public void updateMenus(final Point2D pos) {
        try {
            mMenuListLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        List<UpdatableItem> copy = new ArrayList<UpdatableItem>(menuList);
        mMenuListLock.release();
        for (final UpdatableItem i : copy) {
            i.setPos(pos);
            i.update();
        }
    }

    /**
     * Registers a menu item.
     */
    public final void registerMenuItem(final UpdatableItem m) {
        try {
            mMenuListLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        menuList.add(m);
        mMenuListLock.release();
    }

    /**
     * Returns units.
     */
    protected Unit[] getUnits() {
        return null;
    }

    /**
     * Returns units.
     */
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

    /**
     * Adds mouse over listener.
     */
    protected final void addMouseOverListener(final Component c,
                                              final ButtonCallback bc) {
        c.addMouseListener(new MouseListener() {
            public void mouseClicked(final MouseEvent e) {
                /* do nothing */
            }

            public void mouseEntered(final MouseEvent e) {
                if (c.isShowing() && c.isEnabled()) {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            bc.mouseOver();
                        }
                    });
                    thread.start();
                }
            }

            public void mouseExited(final MouseEvent e) {
                final Thread t = new Thread(new Runnable() {
                    public void run() {
                        bc.mouseOut();
                    }
                });
                t.start();
            }

            public void mousePressed(final MouseEvent e) {
                mouseExited(e);
                /* do nothing */
            }

            public void mouseReleased(final MouseEvent e) {
                /* do nothing */
            }
        });
    }

    /**
     * Compares ignoring case.
     */
    public final int compareTo(final Object o) {
        return toString().compareToIgnoreCase(o.toString());
    }

    /**
     * Returns table. The table name can be whatever but should be unique if
     * more tables are used.
     */
    @SuppressWarnings("unchecked")
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
                /**
                 * Overriding so that jlabels show up.
                 */
                public Class getColumnClass(final int c) {
                    return getValueAt(0, c).getClass();
                }

                public TableCellRenderer getCellRenderer(final int row,
                                                         final int column) {
                    if (column == 0) {
                        return bcr;
                    }
                    return super.getCellRenderer(row, column);
                }

            };
            tables.put(tableName, table);
            final TableRowSorter sorter =
                        new TableRowSorter<DefaultTableModel>(tableModel);
            for (int i = 0; i < colNames.length; i++) {
                final Comparator<Object> c = getColComparator(i);
                if (c != null) {
                    sorter.setComparator(i, c);
                }
            }
            table.setRowSorter((RowSorter) sorter);
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

                public void mouseMoved(final MouseEvent me) {
                   final Point p = me.getPoint();
                   final int newRow = table.rowAtPoint(p);
                   if (row >= 0 && newRow != row) {
                       final MyButton b = (MyButton) table.getValueAt(row, 0);
                       b.getModel().setRollover(false);
                       table.setValueAt((Object) b, row, 0);
                   }
                   if (newRow >= 0 && newRow != row) {
                       row = newRow;
                       final MyButton b = (MyButton) table.getValueAt(row, 0);
                       b.getModel().setRollover(true);
                       table.setValueAt((Object) b, row, 0);
                   }
                }
                public void mouseDragged(final MouseEvent me) {
                    /* nothing */
                }
            });
            table.addMouseListener(new MouseAdapter() {
                private int row;
                private boolean paintIt = false;
                private boolean paintItMouseOver = false;

                public final void mouseClicked(final MouseEvent e) {
                    if (e.getClickCount() > 1
                        || SwingUtilities.isRightMouseButton(e)) {
                        return;
                    }
                    final JTable table = (JTable) e.getSource();
                    final Point p = e.getPoint();
                    final int row = table.rowAtPoint(p);
                    final MyButton b = (MyButton) table.getValueAt(row, 0);
                    rowClicked(tableName, b.getText());
                }

                public final void mousePressed(final MouseEvent e) {
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
                    b.getModel().setPressed(true);
                    b.getModel().setArmed(true);
                    table.setValueAt((Object) b, row,  0);
                    paintIt = true;
                }

                public final void mouseReleased(final MouseEvent e) {
                    if (paintIt) {
                        final MyButton b = (MyButton) table.getValueAt(row, 0);
                        b.getModel().setPressed(false);
                        b.getModel().setArmed(false);
                        table.setValueAt((Object) b, row, 0);
                    }
                    paintIt = false;
                }

                public final void mouseEntered(final MouseEvent e) {
                    final JTable table = (JTable) e.getSource();
                    final Point p = e.getPoint();
                    final int row = table.rowAtPoint(p);
                    final MyButton b = (MyButton) table.getValueAt(row, 0);
                    b.getModel().setRollover(true);
                    table.setValueAt((Object) b, row, 0);
                    paintItMouseOver = true;
                }

                public final void mouseExited(final MouseEvent e) {
                    if (paintItMouseOver) {
                        for (int i = 0; i < table.getRowCount(); i++) {
                            final MyButton b =
                                            (MyButton) table.getValueAt(i, 0);
                            b.getModel().setRollover(false);
                            table.setValueAt((Object) b, i, 0);
                        }
                    }
                    paintItMouseOver = false;
                }
            });

            Tools.resizeTable(table);
            return table;
        }
        return null;
    }


    /**
     * Alignment for the specified column.
     */
    protected int getTableColumnAlignment(final String tableName,
                                          final int column) {
        return SwingConstants.LEFT;
    }

    /**
     * Returns info object for this row.
     */
    protected Info getTableInfo(final String tableName, final String key) {
        return null;
    }

    /**
     * Returns columns for the table.
     */
    protected String[] getColumnNames(final String tableName) {
        return new String[]{};
    }

    /**
     * Returns data for the table.
     */
    protected Object[][] getTableData(final String tableName) {
        return new Object[][]{};
    }

    /**
     * Updates data in the table.
     */
    @SuppressWarnings("unchecked")
    public final void updateTable(final String tableName) {
        Tools.debug(this, "update table: " + tableName, 2);
        final JTable table = tables.get(tableName);
        final DefaultTableModel tableModel = tableModels.get(tableName);
        if (tableModel != null) {
            final String[] colNames = getColumnNames(tableName);
            if (colNames != null && colNames.length > 0) {
                final Object[][] data = getTableData(tableName);
                Tools.debug(this, "update table in: " + getName());
                final TableRowSorter sorter =
                                        (TableRowSorter) table.getRowSorter();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        final List sortKeys = sorter.getSortKeys();
                        tableModel.setDataVector(data, colNames);
                        sorter.setSortKeys(sortKeys);
                        tableModel.fireTableDataChanged();
                        Tools.resizeTable(tables.get(tableName));
                    }
                });
            }
        }
    }

    /**
     * Execute when row in the table was clicked.
     */
    protected void rowClicked(final String tableName, final String key) {
        /* do nothing */
    }

    /**
     * Returns row height for the table.
     */
    protected final int getRowHeight() {
        return 38;
    }

    /**
     * Retrurns color for some rows.
     */
    protected Color getTableRowColor(final String tableName, final String key) {
        return null;
    }

    /**
     * Returns comparator for column.
     */
    protected Comparator<Object> getColComparator(final int col) {
        return null;
    }
}
