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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JToolTip;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A menu item that can have an alternate text depending on the predicate()
 * method and be enabled/disabled depending on the enablePredicate() method.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MyMenuItem extends JMenuItem
implements ActionListener, UpdatableItem, ComponentWithTest {
    private static final Logger LOG = LoggerFactory.getLogger(MyMenuItem.class);
    private static final long serialVersionUID = 1L;
    private static final GraphicsDevice SCREEN_DEVICE =
                                         GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    private String text1;
    private ImageIcon icon1;
    /** Short decription of the item for tool tip. */
    private String shortDesc1;
    private String text2;
    private ImageIcon icon2;
    /** Alternate short decription of the item for tool tip. */
    private String shortDesc2;
    private JToolTip toolTip = null;
    /** Pos of the click that can be used in the overriden action method. */
    private Point2D pos;
    /** Robot to move a mouse a little if a tooltip has changed. */
    private Robot robot;
    private Color toolTipBackground = null;
    private AccessMode enableAccessMode;
    private AccessMode visibleAccessMode;
    private String origToolTipText = "";
    @Autowired
    private Application application;

    private MenuAction menuAction;

    private Predicate predicate = new Predicate() {
        @Override
        public boolean check() {
            return true;
        }
    };

    private EnablePredicate enablePredicate = new EnablePredicate() {
        @Override
        public String check() {
            return null;
        }
    };

    private VisiblePredicate visiblePredicate = new VisiblePredicate() {
        @Override
        public boolean check() {
            return true;
        }
    };

    private Runnable update = new Runnable() {
        @Override
        public void run() {
        }
    };

    protected void init(final String text,
                        final ImageIcon icon,
                        final AccessMode enableAccessMode,
                        final AccessMode visibleAccessMode) {
        super.setText(text);
        text1 = text;
        icon1 = icon;
        this.enableAccessMode = enableAccessMode;
        this.visibleAccessMode = visibleAccessMode;
        toolTip = createToolTip();
        toolTip.setTipText(text);
        setNormalFont();
        addActionListener(this);
        this.robot = createRobot();
        processAccessMode();
        setIconAndTooltip();
        application.isSwingThread();
    }

    protected void init(final String text,
                        final ImageIcon icon,
                        final String shortDesc,
                        final AccessMode enableAccessMode,
                        final AccessMode visibleAccessMode) {
        super.setText(text);
        if (shortDesc != null && !shortDesc.isEmpty()) {
            toolTip = createToolTip();
            toolTip.setTipText(shortDesc);
        }
        setNormalFont();
        text1 = text;
        icon1 = icon;
        if (shortDesc == null) {
            shortDesc1 = "";
        } else {
            shortDesc1 = shortDesc;
        }
        this.enableAccessMode = enableAccessMode;
        this.visibleAccessMode = visibleAccessMode;
        addActionListener(this);
        robot = createRobot();
        processAccessMode();
        setIconAndTooltip();
        application.isSwingThread();
    }

    private Robot createRobot() {
        try {
            return new Robot(SCREEN_DEVICE);
        } catch (final AWTException e) {
            LOG.appError("MyMenuItem: robot error");
        }
        return null;
    }

    /**
     * Prepares a new {@code MyMenuItem} object. The alternate item is
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
    protected void init(final String text1a,
                        final ImageIcon icon1a,
                        final String shortDesc1a,
                        final String text2,
                        final ImageIcon icon2,
                        final String shortDesc2,
                        final AccessMode enableAccessMode,
                        final AccessMode visibleAccessMode) {
        init(text1a, icon1a, shortDesc1a, enableAccessMode, visibleAccessMode);
        this.text2 = text2;
        this.icon2 = icon2;
        if (shortDesc2 == null) {
            this.shortDesc2 = "";
        } else {
            this.shortDesc2 = shortDesc2;
        }
        processAccessMode();
        setIconAndTooltip();
    }

    /**
     * Sets the pos of the click that can be used in the overriden action
     * method.
     */
    @Override
    public final void setPos(final Point2D pos) {
        this.pos = pos;
    }

    /** Returns the saved position. */
    public final Point2D getPos() {
        return pos;
    }

    /** Sets normal font for this menu item. */
    private void setNormalFont() {
        final Font font = getFont();
        final String name = font.getFontName();
        final int style   = Font.PLAIN;
        final int size    = font.getSize();
        application.invokeLater(!Application.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                setFont(new Font(name, style, size));
            }
        });
    }

    /** Sets special font for this menu item. */
    public final void setSpecialFont() {
        final Font font = getFont();
        final String name = font.getFontName();
        final int style   = Font.ITALIC;
        final int size    = font.getSize();
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                setFont(new Font(name, style, size));
            }
        });
    }

    //  /**
    //   * This method can be overriden to define an action that should be taken
    //   * after the item is selected.
    //   */
  //    public abstract void action();

//    /** Returns false if the alternate menu item text etc. should be shown. */
//    public boolean predicate() {
//        return true;
//    }

//    /**
//     * Returns whether the item should be enabled or not.
//     * null if it should be enabled or some string that can be used as
//     * tooltip if it should be disabled.
//     */
//    public String enablePredicate() {
//        return null;
//    }

//    /** Returns whether the item should be visible or not. */
//    public boolean visiblePredicate() {
//        return true;
//    }

    /** Updates the menu item, checking the predicate and enablePredicate. */
    @Override
    public void updateAndWait() {
        update.run();
        processAccessMode();
        setIconAndTooltip();
    }

    private void setIconAndTooltip() {
        if (predicate.check()) {
            setText(text1);
            if (icon1 != null) {
                setIcon(icon1);
            }
            if (toolTip != null
                && shortDesc1 != null
                && !shortDesc1.equals(text1)) {
                origToolTipText = shortDesc1;
                toolTip.setTipText(shortDesc1);
            }
        } else {
            setText(text2);
            if (icon1 != null) { /* icon1 is here on purpose */
                setIcon(icon2);
            }
            if (toolTip != null
                && shortDesc2 != null
                && !shortDesc2.equals(text2)) {
                origToolTipText = shortDesc2;
                toolTip.setTipText(shortDesc2);
            }
        }
    }

    /** Sets this item enabled and visible according to its access type. */
    private void processAccessMode() {
        final boolean accessible = application.isAccessible(enableAccessMode);
        final String disableTooltip = enablePredicate.check();
        final boolean visible = visiblePredicate.check();
        setEnabled(disableTooltip == null && accessible);
        setVisible(visible && application.isAccessible(visibleAccessMode));
        if (toolTip != null && isVisible()) {
            if (!accessible && enableAccessMode.getAccessType()
                               != Application.AccessType.NEVER) {
                String advanced = "";
                if (enableAccessMode.isAdvancedMode()) {
                    advanced = "Advanced ";
                }
                setToolTipText0("<html><b>"
                                + getText()
                                + " (disabled)</b><br>available in \""
                                + advanced
                                + Application.OP_MODES_MAP.get(
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
    }

    /**
     * When an item was selected this calls an action method that can be
     * overridden.
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        LOG.debug1("actionPerformed: ACTION: " + e.getSource());
        actionThread();
    }

    public void actionThread() {
        final Thread thread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    menuAction.run(getText());
                }
            }
        );
        thread.start();
    }

    /** Returns the text of the menu item. */
    @Override
    public final String toString() {
        return getText();
    }

    /** Creates tooltip. */
    @Override
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

    /** Sets tooltip's background color. */
    @Override
    public final void setToolTipBackground(final Color toolTipBackground) {
        this.toolTipBackground = toolTipBackground;
    }

    /** Returns location of the tooltip, so that it does not cover the menu
     * item. */
    @Override
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
    @Override
    public final void setToolTipText(final String text) {
        if (toolTip == null || text == null) {
            return;
        }
        origToolTipText = text;
        setToolTipText0(text);
    }

    /** Wiggle the mouse. */
    private void moveMouse() {
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
            final PointerInfo pi = MouseInfo.getPointerInfo();
            if (pi != null) {
                final Point2D p = pi.getLocation();
                robot.mouseMove((int) p.getX() + xOffset - 1,
                                (int) p.getY());
                robot.mouseMove((int) p.getX() + xOffset + 1,
                                (int) p.getY());
                robot.mouseMove((int) p.getX() + xOffset, (int) p.getY());
            }
        }
    }

    /** Sets tooltip and wiggles the mouse to refresh it. */
    private void setToolTipText0(String toolTipText) {
        if (toolTip == null) {
            return;
        }
        if (toolTipText != null && toolTipText.isEmpty()) {
            toolTipText = text1;
        }
        toolTip.setTipText(toolTipText);
        super.setToolTipText(toolTipText);
        if (toolTip != null && robot != null && toolTip.isShowing()) {
            final Thread t = new Thread(new Runnable() {
                @Override
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
    @Override
    public final void cleanup() {
        if (toolTip != null) {
            toolTip.setComponent(null);
        }
    }

    /** Set text1. */
    public final void setText1(final String text1) {
        this.text1 = text1;
    }

    public MyMenuItem addAction(final MenuAction menuAction) {
        this.menuAction = menuAction;
        return this;
    }

    public MyMenuItem predicate(final Predicate predicate) {
        this.predicate = predicate;
        return this;
    }

    public MyMenuItem enablePredicate(final EnablePredicate enablePredicate) {
        this.enablePredicate = enablePredicate;
        return this;
    }

    public MyMenuItem visiblePredicate(final VisiblePredicate visiblePredicate) {
        this.visiblePredicate = visiblePredicate;
        return this;
    }

    public MyMenuItem onUpdate(final Runnable update) {
        this.update = update;
        return this;
    }
}
