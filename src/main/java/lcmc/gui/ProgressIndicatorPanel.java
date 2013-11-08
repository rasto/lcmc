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

/**
 * 18:12 17/02/2005
 * Romain Guy <romain.guy@jext.org>
 * Subject to the BSD license.
 */

package lcmc.gui;

import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.configs.AppDefaults;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

import javax.swing.JComponent;
import javax.swing.ImageIcon;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * An infinite progress panel displays a rotating figure and
 * a message to notice the user of a long, duration unknown
 * task. The shape and the text are drawn upon a white veil
 * which alpha level (or shield value) lets the underlying
 * component shine through. This panel is meant to be used
 * asa <i>glass pane</i> in the window performing the long
 * operation.
 * <br /><br />
 * On the contrary to regular glass panes, you don't need to
 * set it visible or not by yourself. Once you've started the
 * animation all the mouse events are intercepted by this
 * panel, preventing them from being forwared to the
 * underlying components.
 * <br /><br />
 * The panel can be controlled by the <code>start()</code>,
 * <code>stop()</code> and <code>interrupt()</code> methods.
 * <br /><br />
 * Example:
 * <br /><br />
 * <pre>ProgressIndicatorPanel pane = new ProgressIndicatorPanel();
 * frame.setGlassPane(pane);
 * pane.start()</pre>
 * <br /><br />
 * Several properties can be configured at creation time. The
 * message and its font can be changed at runtime. Changing the
 * font can be done using <code>setFont()</code> and
 * <code>setForeground()</code>.
 */

public final class ProgressIndicatorPanel extends JComponent
                                        implements MouseListener, KeyListener {
    /** Logger. */
    private static final Logger LOG =
                        LoggerFactory.getLogger(ProgressIndicatorPanel.class);
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Notifies whether the animation is running or not. */
    private boolean started    = false;
    /** Alpha level of the veil, used for fade in/out. */
    private volatile int alphaLevel = 0;
    /** Duration of the veil's fade in/out. */
    private int rampDelay  = 1000;
    /** Ramp delay stop. */
    private static final int RAMP_DELAY_STOP  = 1000;
    /** Alpha level of the veil. */
    private float shield     = 0.80f;
    /** Message displayed. */
    private final Map<String, Integer> texts =
                                        new LinkedHashMap<String, Integer>();
    /** Message positions or null. */
    private final Map<String, Point2D> textsPositions =
                                              new HashMap<String, Point2D>();
    /** List of failed commands. */
    private final List<String> failuresMap = new LinkedList<String>();
    /** Amount of frames per second. */
    private static final float FPS = Tools.getConfigData().getAnimFPS();
    /** Rendering hints to set anti aliasing. */
    private RenderingHints hints = null;
    /** Lock for the animator. */
    private final Lock mAnimatorLock = new ReentrantLock();
    /** Lock for manipulating the text array. */
    private final Lock mTextsLock = new ReentrantLock();
    /** Cancel button icon. */
    private static final ImageIcon CANCEL_ICON =
                                Tools.createImageIcon(Tools.getDefault(
                                        "ProgressIndicatorPanel.CancelIcon"));
    /** Cancel button. TODO: not used. */
    private final MyButton cancelButton = new MyButton(
                Tools.getString("ProgressIndicatorPanel.Cancel"), CANCEL_ICON);
    /** Animator thread. */
    private Animator animator;

    /** Old width of the whole frame. */
    private int oldWidth  = getWidth();
    /** Old height of the whole frame. */
    private int oldHeight = getHeight();
    /** Beginning position of the bar. */
    private double barPos = -1;
    /** Width of the bar. */
    private double barWidth = 10;
    /** Maximum alpha level. */
    private static final int MAX_ALPHA_LEVEL = 255;
    /** Veil color. */
    private static final Color VEIL2_COLOR = Browser.STATUS_BACKGROUND;
    /** Text color. */
    private static final Color VEIL_COLOR = Browser.PANEL_BACKGROUND;

    /**
     * Creates a new progress panel with default values:<br />
     * <ul>
     * <li>Veil's alpha level is 70%</li>
     * <li>15 frames per second</li>
     * <li>Fade in/out last 300 ms</li>
     * </ul>.
     */
    public ProgressIndicatorPanel() {
        this(0.40f, 300);
    }

    /**
     * Creates a new progress panel.
     * @param shield The alpha level between 0.0 and 1.0 of the colored
     *               shield (or veil).
     * @param rampDelay The duration, in milli seconds, of the fade in and
     *                  the fade out of the veil.
     */
    private ProgressIndicatorPanel(final float shield, final int rampDelay) {
        super();
        this.rampDelay = rampDelay >= 0 ? rampDelay : 0;
        this.shield    = shield >= 0.0f ? shield : 0.0f;

        this.hints = new RenderingHints(RenderingHints.KEY_RENDERING,
                                        RenderingHints.VALUE_RENDER_QUALITY);
        this.hints.put(RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON);
        this.hints.put(RenderingHints.KEY_FRACTIONALMETRICS,
                       RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        cancelButton.setBounds(0,
                               0,
                               cancelButton.getPreferredSize().width,
                               cancelButton.getPreferredSize().height);
    }

    /** Is called upan a failure. */
    public void failure(final String text) {
        LOG.appWarning("failure: " + text);
        if (text == null) {
            return;
        }
        failuresMap.add(text);
        start(text, null);
        Tools.sleep(1000);
        stop(text);
    }

    /** Is called upan a failure and shows it for n seconds. */
    public void failure(final String text, final int n) {
        LOG.appWarning("failure: " + text);
        if (text == null || n < 0) {
            return;
        }
        failuresMap.add(text);
        start(text, null);
        Tools.sleep(n);
        stop(text);
    }

    /**
     * Starts the waiting animation by fading the veil in, then
     * rotating the shapes. This method handles the visibility
     * of the glass pane.
     */
    public void start(final String text,
                      final Point2D position) {
        if (text == null) {
            return;
        }
        mAnimatorLock.lock();
        mTextsLock.lock();
        if (texts.containsKey(text)) {
            texts.put(text, MAX_ALPHA_LEVEL);
            textsPositions.put(text, position);
            mTextsLock.unlock();
            animator.setRampUp(true);
            mAnimatorLock.unlock();
            return;
        }
        texts.put(text, MAX_ALPHA_LEVEL);
        textsPositions.put(text, position);

        if (texts.size() > 1) {
            mTextsLock.unlock();
            animator.setRampUp(true);
            mAnimatorLock.unlock();
            return;
        }
        mTextsLock.unlock();
        Tools.getGUIData().getMainMenu().turnOff();
        cancelButton.setEnabled(true);
        addMouseListener(this);
        addKeyListener(this);
        if (!isVisible()) {
            setVisible(true);
        }
        animator = new Animator();
        final Thread t = new Thread(animator);
        t.start();
        mAnimatorLock.unlock();
    }

    /**
     * Stops the waiting animation by stopping the rotation
     * of the circular shape and then by fading out the veil.
     * This methods sets the panel invisible at the end.
     */
    public void stop(final String text) {
        if (text == null) {
            return;
        }
        mAnimatorLock.lock();
        mTextsLock.lock();
        if (!texts.containsKey(text)) {
            LOG.appWarning("stop: progress indicator already stopped for: --" + text + "--");
            mTextsLock.unlock();
            mAnimatorLock.unlock();
            Tools.printStackTrace();
            return;
        }
        texts.put(text, 250);
        for (final String t : texts.keySet()) {
            final int a = texts.get(t).intValue();
            if (a == MAX_ALPHA_LEVEL) {
                /* at least one is going up */
                mTextsLock.unlock();
                mAnimatorLock.unlock();
                return;
            }
        }
        mTextsLock.unlock();
        animator.setRampUp(false);
        removeMouseListener(ProgressIndicatorPanel.this);
        removeKeyListener(ProgressIndicatorPanel.this);
        Tools.getGUIData().getMainMenu().turnOn();
        mAnimatorLock.unlock();
    }

    /** Paints the glass pane with info and progress indicator. */
    @Override
    protected void paintComponent(final Graphics g) {
        if (started) {
            final int width  = getWidth();

            double maxY;

            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHints(hints);

            final int newAlphaLevel = (int) (alphaLevel * shield);
            if (newAlphaLevel < 0 || newAlphaLevel > MAX_ALPHA_LEVEL) {
                LOG.appWarning("paintComponent: alpha level out of range: "
                               + newAlphaLevel);
            }
            g2.setColor(new Color(VEIL_COLOR.getRed(),
                                  VEIL_COLOR.getGreen(),
                                  VEIL_COLOR.getBlue(),
                                  newAlphaLevel));
            final int startAtHeight = 22;
            g2.fillRect(0,
                        startAtHeight,
                        width,
                        Tools.getGUIData().getTerminalPanelPos()
                            - startAtHeight);
            if (barPos < 0) {
                barPos = width / 2;
            }
            if (barPos < width) {
                final int he = Tools.getGUIData().getTerminalPanelPos()
                                    - startAtHeight;
                g2.setColor(new Color(VEIL2_COLOR.getRed(),
                                      VEIL2_COLOR.getGreen(),
                                      VEIL2_COLOR.getBlue(),
                                      (int) (alphaLevel * shield * 0.3)));
                if (barPos < width / 2) {
                    g2.fillRect((int) barPos,
                                startAtHeight,
                                (int) (width - barPos * 2),
                                he);
                } else {
                    g2.fillRect((int) (width  - barPos),
                                startAtHeight,
                                (int) (barPos * 2 - width),
                                he);
                }
            }
            barPos += 5.0 * 20.0 / FPS;
            if (barPos >= width / 2 + barWidth / 2) {
                barPos = width / 2 - barWidth / 2;
            }


            mTextsLock.lock();
            if (!texts.isEmpty()) {
                final FontRenderContext context = g2.getFontRenderContext();
                final Font font = new Font(getFont().getName(),
                                           getFont().getStyle(),
                                           (int) (getFont().getSize() * 1.75));

                maxY = getHeight() / 2 - 30;

                int y = 0;
                int x = 0;
                for (String text : texts.keySet()) {
                    if (text == null || text.length() == 0) {
                        continue;
                    }
                    final int alpha = texts.get(text).intValue();
                    final TextLayout layout = new TextLayout(
                                                    text,
                                                    new Font(font.getName(),
                                                             font.getStyle(),
                                                             font.getSize()),
                                                    context);
                    final Rectangle2D bounds = layout.getBounds();
                    Color f;
                    if (failuresMap.contains(text)) {
                        f = new Color(255, 0, 0);
                    } else {
                        f = AppDefaults.BACKGROUND_DARK;
                    }
                    g2.setColor(new Color(f.getRed(),
                                          f.getGreen(),
                                          f.getBlue(),
                                          alpha));
                    final Point2D textPos = textsPositions.get(text);
                    float textPosX;
                    float textPosY;
                    if (textPos == null) {
                        textPosX = (float) (width - bounds.getWidth()) / 2;
                        textPosY = (float) (maxY + layout.getLeading()
                                    + 2 * layout.getAscent());
                    } else {
                        textPosX = (float) textPos.getX();
                        textPosY = (float) textPos.getY();
                    }
                    layout.draw(g2,
                                textPosX,
                                textPosY + y);
                    x = (int) (x + (((float) (10 + 15 * text.length())
                                     / MAX_ALPHA_LEVEL)
                                    * alpha));
                    y = (int) (y + (((float) 27 / MAX_ALPHA_LEVEL)
                                    * alpha));
                    if (bounds.getWidth() > barWidth) {
                        barWidth = bounds.getWidth() + 30;
                    }
                }
            }
            mTextsLock.unlock();
            super.paintComponent(g2);
        }

    }

    /** Animation thread. */
    private class Animator implements Runnable {
        /** Whether the alpha level goes up or down. */
        private volatile boolean rampUp;

        /** Prepares a new <code>Animator</code> object. */
        protected Animator() {
            rampUp = true;
        }

        /** Sets the rump up. */
        private void setRampUp(final boolean rampUp) {
            this.rampUp = rampUp;
        }

        /** Runs the thread. */
        @Override
        public void run() {
            LOG.debug1("run: animator start");
            long start = System.currentTimeMillis();
            if (rampDelay == 0) {
                alphaLevel = rampUp ? MAX_ALPHA_LEVEL : 0;
            }

            started = true;
            while (true) {
                final boolean lRampUp = rampUp;
                if (getWidth() != oldWidth || getHeight() != oldHeight) {
                    oldWidth  = getWidth();
                    oldHeight = getHeight();
                }

                final long time = System.currentTimeMillis();
                if (start >= time) {
                    continue;
                }
                if (lRampUp) {
                    int newAlphaLevel = alphaLevel
                        + (int) (MAX_ALPHA_LEVEL * (time - start) / rampDelay);
                    if (newAlphaLevel >= MAX_ALPHA_LEVEL) {
                        newAlphaLevel = MAX_ALPHA_LEVEL;
                    } else if (newAlphaLevel < 0) {
                        LOG.appWarning("wrong alpha: " + newAlphaLevel
                                       + ", prev alpha: " + alphaLevel
                                       + ", rampDelay: " + rampDelay
                                       + ", t-s: " + (time - start));
                    }
                    alphaLevel = newAlphaLevel;
                } else {
                    int newAlphaLevel = alphaLevel - (int) (MAX_ALPHA_LEVEL
                                  * (time - start) / RAMP_DELAY_STOP);
                    if (newAlphaLevel <= 0) {
                        newAlphaLevel = 0;
                        mTextsLock.lock();
                        if (texts.size() <= 0) {
                            mTextsLock.unlock();
                            break;
                        } else {
                            mTextsLock.unlock();
                        }
                    }
                    alphaLevel = newAlphaLevel;
                }

                final ArrayList<String> toRemove = new ArrayList<String>();
                mTextsLock.lock();
                for (final String text : texts.keySet()) {
                    int alpha = texts.get(text).intValue();
                    if (alpha < MAX_ALPHA_LEVEL) {
                        int delay = 1000;
                        if (failuresMap.contains(text)) {
                            delay = 10000;
                        }
                        alpha -=
                            (int) (MAX_ALPHA_LEVEL * (time - start) / delay);
                        if (alpha < 0) {
                            toRemove.add(text);
                        } else {
                            texts.put(text, alpha);
                        }
                    }
                }
                for (final String text : toRemove) {
                    texts.remove(text);
                    textsPositions.remove(text);
                    failuresMap.remove(text);
                }

                mTextsLock.unlock();
                try {
                    Thread.sleep((int) (1000 / FPS));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                start = time;
                repaint();
                Thread.yield();
            }
            started = false;
            barWidth = 10;
            LOG.debug1("run: animator end");
        }
    }

    /** Mouse clicked. */
    @Override
    public void mouseClicked(final MouseEvent e) {
        /* do nothing */
    }

    /** Mouse pressed. */
    @Override
    public void mousePressed(final MouseEvent e) {
        /* do nothing */
    }

    /** Mouse released. */
    @Override
    public void mouseReleased(final MouseEvent e) {
        /* do nothing */
    }

    /** Mouse entered. */
    @Override
    public void mouseEntered(final MouseEvent e) {
        /* do nothing */
    }

    /** Mouse exited. */
    @Override
    public void mouseExited(final MouseEvent e) {
        /* do nothing */
    }

    /** Key pressed. */
    @Override
    public void keyPressed(final KeyEvent e) {
        /* do nothing */
    }

    /** Key released. */
    @Override
    public void keyReleased(final KeyEvent e) {
        /* do nothing */
    }

    /** Key typed. */
    @Override
    public void keyTyped(final KeyEvent e) {
        /* do nothing */
    }
}
