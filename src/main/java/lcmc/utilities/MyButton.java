/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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

package lcmc.utilities;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolTip;

import lcmc.gui.widget.Check;

/**
 * This class creates a button with any gradient colors.
 */
public class MyButton extends JButton implements ComponentWithTest {
    private static final Logger LOG = LoggerFactory.getLogger(MyButton.class);
    private static final Color DEFAULT_BACKGROUND_COLOR = Tools.getDefaultColor("DefaultButton.Background");
    private static final GraphicsDevice SCREEN_DEVICE =
                                           GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    /** Second color in the gradient. */
    private Color color2 = DEFAULT_BACKGROUND_COLOR;
    /** Robot to move a mouse a little if a tooltip has changed. */
    private Robot robot;
    private JToolTip toolTip = null;
    private String simulationResultToolTip = "";
    /** Tooltip about changed/incorrect fields. */
    private String checkToolTip = "";
    /** Tooltip background color. */
    private Color toolTipBackground = null;

    public MyButton() {
        robot = createRobot();
        setContentAreaFilled(false);  // *
    }

    private Robot createRobot() {
        try {
            return new Robot(SCREEN_DEVICE);
        } catch (final AWTException e) {
            LOG.appError("MyButton: robot error");
        }
        return null;
    }

    public MyButton(final String text) {
        super.setText(text);
        robot = createRobot();
        setContentAreaFilled(false);
    }

    public MyButton(final String text, final Icon icon) {
        super.setText(text);
        super.setIcon(icon);
        robot = createRobot();
        setContentAreaFilled(false);
    }

    public MyButton(final String text, final Icon icon, final String toolTipText) {
        super.setText(text);
        super.setIcon(icon);
        super.setToolTipText(toolTipText);
    }
    /**
     * @param c1
     *          color 1 in the gradient
     * @param c2
     *          color 2 in the gradient
     */
    public MyButton(final Color c1, final Color c2) {
        robot = createRobot();

        color2 = c2;
        setContentAreaFilled(false);  // *
    }

    @Override
    public final JToolTip createToolTip() {
        toolTip = super.createToolTip();
        if (toolTipBackground != null) {
            toolTip.setBackground(toolTipBackground);
        }
        return toolTip;
    }

    @Override
    public final void setToolTipBackground(final Color toolTipBackground) {
        this.toolTipBackground = toolTipBackground;
    }

    @Override
    public final void setToolTipText(final String simulationToolTip) {
        this.simulationResultToolTip = simulationToolTip;
        updateToolTip();
    }

    /** Sets tooltip and wiggles the mouse to refresh it. */
    private void updateToolTip() {
        final String toolTipText = "<html>" + simulationResultToolTip + "<br>" + checkToolTip + "</html>";
        if (toolTip != null && robot != null && toolTip.isShowing()) {
            super.setToolTipText(toolTipText);
            final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            int xOffset = 0;
            if (devices.length == 2) {
                /* workaround for dual monitors that are flipped. */
                //TODO: not sure how is it with three monitors
                final int x1 = devices[0].getDefaultConfiguration().getBounds().x;
                final int x2 = devices[1].getDefaultConfiguration().getBounds().x;
                if (x1 > x2) {
                    xOffset = -x1;
                }
            }
            final Point2D p = MouseInfo.getPointerInfo().getLocation();
            robot.mouseMove((int) p.getX() + xOffset - 1, (int) p.getY());
            robot.mouseMove((int) p.getX() + xOffset + 1, (int) p.getY());
            robot.mouseMove((int) p.getX() + xOffset, (int) p.getY());
        } else {
            super.setToolTipText(toolTipText);
        }
    }


    public final void setBackgroundColor(final Color c) {
        if (c == null) {
            color2 = DEFAULT_BACKGROUND_COLOR;
        } else {
            color2 = c;
        }
        super.setBackground(c);
        repaint();
    }

    @Override
    public final Color getBackground() {
        if (color2 == null) {
            return DEFAULT_BACKGROUND_COLOR;
        }
        return color2;
    }

    @Override
    protected final void paintComponent(final Graphics g) {
        if (!isEnabled()) {
            super.paintComponent(g);
            return;
        }
        if (getModel().isPressed()) {
            setContentAreaFilled(true);
            super.paintComponent(g);
            return;
        }
        setContentAreaFilled(false);

        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final Paint gp2 = new GradientPaint(1.0f,
                                            0.0f,
                                            Tools.brighterColor(getBackground(), 1.1),
                                            1.0f,
                                            getHeight() * 0.3f,
                                            Tools.brighterColor(getBackground(), 1.2));
        final Paint gp1 = new GradientPaint(1.0f,
                                            getHeight(),
                                            Tools.brighterColor(getBackground(), 0.85),
                                            1.0f,
                                            getHeight() * 0.3f,
                                            Tools.brighterColor(getBackground(), 1.2));
        final Shape rf2 = new Rectangle2D.Float(0.0f, 0.0f, getWidth(), getHeight());
        final Shape rf1 = new Rectangle2D.Float(3.0f,
                                                getHeight() * 0.5f,
                                                getWidth() - 6,
                                                (float) (getHeight() * 0.5 - 3));
        g2.setPaint(gp2);
        g2.fill(rf2);
        g2.setPaint(gp1);
        g2.fill(rf1);

        super.paintComponent(g);
    }

    public final void pressButton() {
        fireActionPerformed(new ActionEvent(this, 0, "pressed"));
    }

    public final void setEnabled(final Check check) {
        setEnabled(check.isChanged() && check.isCorrect());
        checkToolTip = check.getToolTip();
        updateToolTip();
    }

    /** For revert buttons. */
    public final void setEnabledChanged(final Check check) {
        setEnabled(check.isChanged());
        checkToolTip = check.getToolTip();
        updateToolTip();
    }

    public final void setEnabledCorrect(final Check check) {
        setEnabled(check.isCorrect());
        checkToolTip = check.getToolTip();
        updateToolTip();
    }

    public MyButton addAction(final Runnable action) {
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                action.run();
            }
        });
        return this;
    }
}
