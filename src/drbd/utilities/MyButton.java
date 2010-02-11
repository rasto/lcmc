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

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToolTip;
import java.awt.geom.Point2D;

import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;


/**
 * This class creates a button with any gradient colors.
 */
public class MyButton extends JButton implements ComponentWithTest {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** First color in the gradient. */
    private Color color1;
    /** Second color in the gradient. */
    private Color color2;
    /** Robot to move a mouse a little if a tooltip has changed. */
    private Robot robot = null;
    /** Button tooltip. */
    private JToolTip toolTip = null;
    /** Screen device. */
    private static final GraphicsDevice SCREEN_DEVICE =
     GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    /** Tooltip background color. */
    private Color toolTipBackground = null;

    /**
     * Prepares a new <code>MyButton</code> object.
     */
    public MyButton() {
        this(Color.WHITE, Tools.getDefaultColor("DefaultButton.Background"));
    }

    /**
     * Prepares a new <code>MyButton</code> object.
     *
     * @param text
     *          text in the button
     */
    public MyButton(final String text) {
        this();
        setText(text);
    }

    /**
     * Prepares a new <code>MyButton</code> object.
     *
     * @param text
     *          text in the button
     * @param icon
     *          icon in the button
     */
    public MyButton(final String text, final ImageIcon icon) {
        this();
        setText(text);
        setIcon((Icon) icon);
    }

    /**
     * Prepares a new <code>MyButton</code> object.
     *
     * @param text
     *          text in the button
     * @param icon
     *          icon in the button
     * @param toolTip
     *          tool tip in the button
     */
    public MyButton(final String text,
                    final ImageIcon icon,
                    final String toolTipText) {
        this(text, icon);
        super.setToolTipText(toolTipText);
    }
    /**
     * Prepares a new <code>MyButton</code> object.
     *
     * @param c1
     *          color 1 in the gradient
     * @param c2
     *          color 2 in the gradient
     */
    public MyButton(final Color c1, final Color c2) {
        super();
        try {
            robot = new Robot(SCREEN_DEVICE);
        } catch (java.awt.AWTException e) {
            Tools.appError("Robot error");
        }

        this.color1 = c1;
        this.color2 = c2;
        setContentAreaFilled(false);  // *
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
            if (devices.length == 2) {
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
            robot.mouseMove((int) p.getX() + xOffset,
                            (int) p.getY());
        }
    }


    /**
     * Sets color 1 in the gradient.
     */
    public final void setColor1(final Color c1) {
        this.color1 = c1;
        repaint();
    }

    /**
     * Sets color 2 in the gradient.
     */
    public final void setColor2(final Color c2) {
        this.color2 = c2;
        repaint();
    }

    /**
     * Sets background of the button.
     */
    public final void setBackground(final Color c) {
        this.color2 = c;
        repaint();
    }

    /**
     * Gets background of the button.
     */
    public final Color getBackground() {
        return color2;
    }

    /**
     * Overloaded in order to paint the background.
     */
    protected final void paintComponent(final Graphics g) {
        if (!isEnabled()) {
            super.paintComponent(g);
            return;
        }

        GradientPaint gp1, gp2;
        Rectangle2D.Float rf1, rf2;
        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        gp1 = new GradientPaint(1.0f, (float) getHeight(),
            color2, 1.0f,
            (float) getHeight() * 0.3f, color1);
        gp2 = new GradientPaint(1.0f, 0.0f, color2,
            1.0f, (float) getHeight() * 0.3f, color1);
        rf1 = new Rectangle2D.Float(0.0f, (float) getHeight() * 0.3f,
            (float) getWidth(), (float) getHeight());
        rf2 = new Rectangle2D.Float(0.0f, 0.0f, (float) getWidth(),
            (float) getHeight() * 0.3f);
        g2.setPaint(gp1);
        g2.fill(rf1);
        g2.setPaint(gp2);
        g2.fill(rf2);

        super.paintComponent(g);
    }

    /**
     * Presses this button.
     */
    public final void pressButton() {
        fireActionPerformed(new ActionEvent(this, 0, "pressed"));
    }
}
