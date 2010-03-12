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
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Font;
import java.awt.Color;
import java.awt.geom.Point2D;
import javax.swing.JMenuItem;
import javax.swing.ImageIcon;
import javax.swing.JToolTip;

import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

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
    private final ConfigData.AccessType enableAccessType;
    /** Access Type for this component to become visible. */
    private final ConfigData.AccessType visibleAccessType;

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
                      final ConfigData.AccessType enableAccessType,
                      final ConfigData.AccessType visibleAccessType) {
        super(text);
        this.text1 = text;
        this.icon1 = icon;
        this.enableAccessType = enableAccessType;
        this.visibleAccessType = visibleAccessType;
        toolTip = createToolTip();
        setNormalFont();
        addActionListener(this);
        try {
            robot = new Robot(SCREEN_DEVICE);
        } catch (java.awt.AWTException e) {
            Tools.appError("Robot error");
        }
        processAccessType(); //TODO: should not be called here
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
                      final ConfigData.AccessType enableAccessType,
                      final ConfigData.AccessType visibleAccessType) {
        super(text);
        toolTip = createToolTip();
        setNormalFont();
        this.text1 = text;
        this.icon1 = icon;
        this.shortDesc1 = shortDesc;
        this.enableAccessType = enableAccessType;
        this.visibleAccessType = visibleAccessType;
        addActionListener(this);
        try {
            robot = new Robot(SCREEN_DEVICE);
        } catch (java.awt.AWTException e) {
            Tools.appError("Robot error");
        }
        processAccessType(); //TODO: should not be called here
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
                      final ConfigData.AccessType enableAccessType,
                      final ConfigData.AccessType visibleAccessType) {
        this(text1a, icon1a, shortDesc1a, enableAccessType, visibleAccessType);
        this.text2 = text2;
        this.icon2 = icon2;
        this.shortDesc2 = shortDesc2;
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
        final int style   = font.PLAIN;
        final int size    = font.getSize();
        setFont(new Font(name, style, size));
    }

    /**
     * Sets special font for this menu item.
     */
    public final void setSpecialFont() {
        final Font font = getFont();
        final String name = font.getFontName();
        final int style   = font.ITALIC;
        final int size    = font.getSize();
        setFont(new Font(name, style, size));
        setBackground(Color.WHITE);
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
     */
    public boolean enablePredicate() {
        return true;
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
            setText(text1);
            if (icon1 != null) {
                setIcon(icon1);
            }
            if (shortDesc1 != null && !shortDesc1.equals(text1)) {
                toolTip.setTipText(shortDesc1);
            }
        } else {
            setText(text2);
            if (icon2 != null) {
                setIcon(icon2);
            }
            if (shortDesc2 != null && !shortDesc1.equals(text2)) {
                toolTip.setTipText(shortDesc2);
            }
        }
        processAccessType();
    }

    /** Sets this item enabled and visible according to its access type. */
    private void processAccessType() {
        final boolean accessible =
                   Tools.getConfigData().isAccessible(enableAccessType);
        setEnabled(enablePredicate() && accessible);
        setVisible(visiblePredicate()
                   && Tools.getConfigData().isAccessible(visibleAccessType));
        if (isVisible() && !accessible) {
            setToolTipText("<html><b>"
                           + getText()
                           + " (disabled)</b><br>available in \""
                           + ConfigData.OP_MODES_MAP.get(enableAccessType)
                           + "\" mode</html>");
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

    /**
     * Sets tooltip and wiggles the mouse to refresh it.
     */
    public final void setToolTipText(final String toolTipText) {
        super.setToolTipText(toolTipText);
        if (toolTip != null && robot != null && toolTip.isShowing()) {
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
}
