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

package drbd.utilities;

import drbd.data.ConfigData;
import drbd.data.AccessMode;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.Color;
import java.awt.geom.Point2D;
import javax.swing.JMenuItem;
import javax.swing.ImageIcon;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;

import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.Point;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * A menu item that can have an alternate text depending on the predicate()
 * method and be enabled/disabled depending on the enablePredicate() method.
 */
public class MyMenuItem extends JMenuItem
implements ActionListener, UpdatableItem, ComponentWithTest {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Text of the item. */
    private String text1;
    /** Icon of the item. */
    private ImageIcon icon1;
    /** Short decription of the item for tool tip. */
    private String shortDesc1;
    /** Alternate text of the item. */
    private String text2;
    /** Alternate icon of the item. */
    private ImageIcon icon2;
    /** Alternate short decription of the item for tool tip. */
    private String shortDesc2;
    /** Tools tip object. */
    private JToolTip toolTip = null;
    /** Pos of the click that can be used in the overriden action method. */
    private Point2D pos;
    /** Robot to move a mouse a little if a tooltip has changed. */
    private Robot robot = null;
    /** Screen device. */
    private static final GraphicsDevice SCREEN_DEVICE =
     GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    /** Tooltip background color. */
    private Color toolTipBackground = null;
    /** Access Type for this component to become enabled. */
    private final AccessMode enableAccessMode;
    /** Access Type for this component to become visible. */
    private final AccessMode visibleAccessMode;
    /** Original tool tip text. */
    private String origToolTipText = "";

    /**
     * Prepares a new <code>MyMenuItem</code> object with icon but without
     * tooltip.
     *
     * @param text
     *          text of the item
     * @param icon
     *          icon of the item
     */
    public MyMenuItem(final String text,
                      final ImageIcon icon,
                      final AccessMode enableAccessMode,
                      final AccessMode visibleAccessMode) {
        super(text);
        this.text1 = text;
        this.icon1 = icon;
        this.enableAccessMode = enableAccessMode;
        this.visibleAccessMode = visibleAccessMode;
        toolTip = createToolTip();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                toolTip.setTipText(text);
            }
        });
        setNormalFont();
        addActionListener(this);
        try {
            robot = new Robot(SCREEN_DEVICE);
        } catch (java.awt.AWTException e) {
            Tools.appError("Robot error");
        }
    }


    /**
     * Prepares a new <code>MyMenuItem</code> object.
     *
     * @param text
     *          text of the item
     * @param icon
     *          icon of the item
     * @param shortDesc
     *          short description for the tool tip of the item
     */
    public MyMenuItem(final String text,
                      final ImageIcon icon,
                      final String shortDesc,
                      final AccessMode enableAccessMode,
                      final AccessMode visibleAccessMode) {
        super(text);
        if (shortDesc != null && !"".equals(shortDesc)) {
            toolTip = createToolTip();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    toolTip.setTipText(shortDesc);
                }
            });
        }
        setNormalFont();
        this.text1 = text;
        this.icon1 = icon;
        if (shortDesc1 == null) {
            this.shortDesc1 = "";
        } else {
            this.shortDesc1 = shortDesc;
        }
        this.enableAccessMode = enableAccessMode;
        this.visibleAccessMode = visibleAccessMode;
        addActionListener(this);
        try {
            robot = new Robot(SCREEN_DEVICE);
        } catch (java.awt.AWTException e) {
            Tools.appError("Robot error");
        }
    }

    /**
     * Prepares a new <code>MyMenuItem</code> object. The alternate item is
     * selected if predicate() returns false.
     *
     * @param text1a
     *          text of the item
     * @param icon1a
     *          icon of the item
     * @param shortDesc1a
     *          short description for the tool tip of the item
     * @param text2
     *          text of the alternate item
     * @param icon2
     *          icon of the alternate item
     * @param shortDesc2
     *          short description for the tool tip of the alternate item
     */
    public MyMenuItem(final String text1a,
                      final ImageIcon icon1a,
                      final String shortDesc1a,
                      final String text2,
                      final ImageIcon icon2,
                      final String shortDesc2,
                      final AccessMode enableAccessMode,
                      final AccessMode visibleAccessMode) {
        this(text1a, icon1a, shortDesc1a, enableAccessMode, visibleAccessMode);
        this.text2 = text2;
        this.icon2 = icon2;
        if (shortDesc2 == null) {
            this.shortDesc2 = "";
        } else {
            this.shortDesc2 = shortDesc2;
        }
    }
    /**
     * Sets the pos of the click that can be used in the overriden action
     * method.
     */
    public final void setPos(final Point2D pos) {
        this.pos = pos;
    }

    /**
     * Returns the saved position.
     */
    protected final Point2D getPos() {
        return pos;
    }

    /**
     * Sets normal font for this menu item.
     */
    private void setNormalFont() {
        final Font font = getFont();
        final String name = font.getFontName();
        final int style   = Font.PLAIN;
        final int size    = font.getSize();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setFont(new Font(name, style, size));
            }
        });
    }

    /**
     * Sets special font for this menu item.
     */
    public final void setSpecialFont() {
        final Font font = getFont();
        final String name = font.getFontName();
        final int style   = Font.ITALIC;
        final int size    = font.getSize();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setFont(new Font(name, style, size));
            }
        });
    }

    /**
     * This method can be overriden to define an action that should be taken
     * after the item is selected.
     */
    public void action() {
        Tools.appError("No action defined.");
    }

    /**
     * Returns false if the alternate menu item text etc. should be shown.
     */
    public boolean predicate() {
        return true;
    }

    /**
     * Returns whether the item should be enabled or not.
     * null if it should be enabled or some string that can be used as
     * tooltip if it should be disabled.
     */
    public String enablePredicate() {
        return null;
    }

    /**
     * Returns whether the item should be visible or not.
     */
    public boolean visiblePredicate() {
        return true;
    }

    /**
     * Updates the menu item, checking the predicate and enablePredicate.
     */
    public final void update() {
        if (predicate()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setText(text1);
                    if (icon1 != null) {
                        setIcon(icon1);
                    }
                    if (toolTip != null
                        && shortDesc1 != null
                        && !shortDesc1.equals(text1)) {
                        toolTip.setTipText(shortDesc1);
                    }
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setText(text2);
                    if (icon1 != null) { /* icon1 is here on purpose */
                        setIcon(icon2);
                    }
                    if (toolTip != null
                        && shortDesc2 != null
                        && !shortDesc1.equals(text2)) {
                        toolTip.setTipText(shortDesc2);
                    }
                }
            });
        }
        processAccessMode();
    }

    /** Sets this item enabled and visible according to its access type. */
    private void processAccessMode() {
        final boolean accessible =
                   Tools.getConfigData().isAccessible(enableAccessMode);
        final String disableTooltip = enablePredicate();
        final boolean visible = visiblePredicate();
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                setEnabled(disableTooltip == null && accessible);
                setVisible(visible
                           && Tools.getConfigData().isAccessible(
                                                        visibleAccessMode));
            }
        });
        if (toolTip != null && isVisible()) {
            Tools.invokeAndWait(new Runnable() {
                public void run() {
                    if (!accessible && enableAccessMode.getAccessType()
                                       != ConfigData.AccessType.NEVER) {
                        String advanced = "";
                        if (enableAccessMode.isAdvancedMode()) {
                            advanced = "Advanced ";
                        }
                        setToolTipText0("<html><b>"
                                        + getText()
                                        + " (disabled)</b><br>available in \""
                                        + advanced
                                        + ConfigData.OP_MODES_MAP.get(
                                              enableAccessMode.getAccessType())
                                        + "\" mode</html>");
                    } else if (disableTooltip != null) {
                        setToolTipText0("<html><b>"
                                        + getText()
                                        + " (disabled)</b><br>"
                                        + disableTooltip
                                        + "</html>");
                    } else if (origToolTipText != null) {
                        setToolTipText0(origToolTipText);
                    }
                }
            });
        }
    }

    /**
     * When an item was selected this calls an action method that can be
     * overridden.
     */
    public final void actionPerformed(final ActionEvent e) {
        final Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    action();
                }
            }
        );
        thread.start();
    }

    /**
     * Returns the text of the menu item.
     */
    public final String toString() {
        return getText();
    }

    /**
     * Creates tooltip.
     */
    public final JToolTip createToolTip() {
        if (toolTip != null) {
            toolTip.setComponent(null);
        }
        toolTip = super.createToolTip();
        if (toolTipBackground != null) {
            toolTip.setBackground(toolTipBackground);
        }
        return toolTip;
    }

    /**
     * Sets tooltip's background color.
     */
    public final void setToolTipBackground(final Color toolTipBackground) {
        this.toolTipBackground = toolTipBackground;
    }

    /** Returns location of the tooltip, so that it does not cover the menu
     * item. */
    public Point getToolTipLocation(final MouseEvent event) {
        final Point screenLocation = getLocationOnScreen();
        final Rectangle sBounds = Tools.getScreenBounds(this);
        final Dimension size = toolTip.getPreferredSize();
        if (screenLocation.x + size.width + event.getX() + 5 > sBounds.width) {
            return new Point(event.getX() - size.width - 5,
                             event.getY() + 20);
        }
        return new Point(event.getX() + 5, /* to not cover the pointer. */
                         event.getY() + 20);
    }

    /** Sets tooltip and wiggles the mouse to refresh it. */
    public final void setToolTipText(final String toolTipText) {
        if (toolTip == null || toolTipText == null) {
            return;
        }
        origToolTipText = toolTipText;
        setToolTipText0(toolTipText);
    }

    private final void moveMouse() {
        if (robot != null) {
            final GraphicsDevice[] devices =
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                                       .getScreenDevices();
            int xOffset = 0;
            if (devices.length >= 2) {
                /* workaround for dual monitors that are flipped. */
                //TODO: not sure how is it with three monitors
                final int x1 =
                    devices[0].getDefaultConfiguration().getBounds().x;
                final int x2 =
                    devices[1].getDefaultConfiguration().getBounds().x;
                if (x1 > x2) {
                    xOffset = -x1;
                }
            }
            final Point2D p = MouseInfo.getPointerInfo().getLocation();
            robot.mouseMove((int) p.getX() + xOffset - 1,
                            (int) p.getY());
            robot.mouseMove((int) p.getX() + xOffset + 1,
                            (int) p.getY());
            robot.mouseMove((int) p.getX() + xOffset, (int) p.getY());
        }
    }

    /** Sets tooltip and wiggles the mouse to refresh it. */
    private final void setToolTipText0(String toolTipText) {
        if (toolTip == null) {
            return;
        }
        if ("".equals(toolTipText)) {
            toolTipText = "---";
        }
        toolTip.setTipText(toolTipText);
        super.setToolTipText(toolTipText);
        if (toolTip != null && robot != null && toolTip.isShowing()) {
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    Tools.sleep(1000); /* well, doesn't work all the time */
                    moveMouse();
                    Tools.sleep(2000);
                    moveMouse();
                }
            });
            t.start();
        }
    }

    /** Clean up. */
    public final void cleanup() {
        if (toolTip != null) {
            toolTip.setComponent(null);
        }
     }

}
