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

package lcmc.common.ui.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;

import lcmc.common.ui.Browser;
import lcmc.common.ui.MainPanel;
import lcmc.configs.AppDefaults;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.domain.util.Tools;

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
 * The panel can be controlled by the {@code start()},
 * {@code stop()} and {@code interrupt()} methods.
 * <br /><br />
 * Example:
 * <br /><br />
 * <pre>ProgressIndicatorPanel pane = new ProgressIndicatorPanel();
 * frame.setGlassPane(pane);
 * pane.start()</pre>
 * <br /><br />
 * Several properties can be configured at creation time. The
 * message and its font can be changed at runtime. Changing the
 * font can be done using {@code setFont()} and
 * {@code setForeground()}.
 */

@Named
@Singleton
class ProgressIndicatorPanel extends JComponent implements MouseListener, KeyListener {
    private static final Logger LOG = LoggerFactory.getLogger(ProgressIndicatorPanel.class);
    private static final int RAMP_DELAY_STOP  = 1000;
    private static final ImageIcon CANCEL_ICON = Tools.createImageIcon(
                                                            Tools.getDefault("ProgressIndicatorPanel.CancelIcon"));
    private static final int MAX_ALPHA_LEVEL = 255;
    private static final Color VEIL2_COLOR = Browser.STATUS_BACKGROUND;
    /** Text color. */
    private static final Color VEIL_COLOR = Browser.PANEL_BACKGROUND;
    /** Notifies whether the animation is running or not. */
    private boolean started    = false;
    /** Alpha level of the veil, used for fade in/out. */
    private volatile int alphaLevel = 0;
    /** Duration of the veil's fade in/out. */
    private int rampDelay  = 1000;
    /** Alpha level of the veil. */
    private float shield     = 0.80f;
    /** Message displayed. */
    private final Map<String, Integer> texts =
                                        new LinkedHashMap<String, Integer>();
    /** Message positions or null. */
    private final Map<String, Point2D> textsPositions =
                                              new HashMap<String, Point2D>();
    /** List of failed commands. */
    private final Collection<String> failuresMap = new LinkedList<String>();
    /** Rendering hints to set anti aliasing. */
    private RenderingHints hints = null;
    private final Lock mAnimatorLock = new ReentrantLock();
    private final Lock mTextsLock = new ReentrantLock();
    @Inject
    private WidgetFactory widgetFactory;
    /** Cancel button. TODO: not used. */
    private MyButton cancelButton;
    /** Animator thread. */
    private Animator animator;

    /** Old width of the whole frame. */
    private int oldWidth  = getWidth();
    /** Old height of the whole frame. */
    private int oldHeight = getHeight();
    /** Beginning position of the bar. */
    private double barPos = -1;
    @Inject
    private MainPanel mainPanel;
    @Inject
    private MainData mainData;

    public void init() {
        this.rampDelay = 300;
        this.shield = 0.40f;

        this.hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        this.hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        this.hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        cancelButton = widgetFactory.createButton(Tools.getString("ProgressIndicatorPanel.Cancel"), CANCEL_ICON);
        cancelButton.setBounds(0, 0, cancelButton.getPreferredSize().width, cancelButton.getPreferredSize().height);
    }

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
    public void start(final String text, final Point2D position) {
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
        for (final Map.Entry<String, Integer> textEntry : texts.entrySet()) {
            final int a = textEntry.getValue();
            if (a == MAX_ALPHA_LEVEL) {
                /* at least one is going up */
                mTextsLock.unlock();
                mAnimatorLock.unlock();
                return;
            }
        }
        mTextsLock.unlock();
        animator.setRampUp(false);
        removeMouseListener(this);
        removeKeyListener(this);
        mAnimatorLock.unlock();
    }

    /** Paints the glass pane with info and progress indicator. */
    @Override
    protected void paintComponent(final Graphics g) {
        if (started) {
            final int width  = getWidth();

            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHints(hints);

            final int newAlphaLevel = (int) (alphaLevel * shield);
            if (newAlphaLevel < 0 || newAlphaLevel > MAX_ALPHA_LEVEL) {
                LOG.appWarning("paintComponent: alpha level out of range: " + newAlphaLevel);
            }
            g2.setColor(new Color(VEIL_COLOR.getRed(), VEIL_COLOR.getGreen(), VEIL_COLOR.getBlue(), newAlphaLevel));
            final int barHeight = 40;
            final int startAtHeight = getHeight() / 2 - barHeight / 2;
            g2.fillRect(0, 20, width, mainPanel.getTerminalPanelPos() - 20);
            if (barPos < 0) {
                barPos = width / 2;
            }
            if (barPos < width) {
                g2.setColor(new Color(VEIL2_COLOR.getRed(),
                                      VEIL2_COLOR.getGreen(),
                                      VEIL2_COLOR.getBlue(),
                                      (int) (alphaLevel * shield * 0.5)));
                if (barPos < width / 2) {
                    g2.fillRect((int) barPos, startAtHeight, (int) (width - barPos * 2), barHeight);
                } else {
                    g2.fillRect((int) (width  - barPos), startAtHeight, (int) (barPos * 2 - width), barHeight);
                }
            }
            barPos += 5.0 * 20.0 / mainData.getAnimFPS();
            if (barPos >= width / 2 + getWidth() / 2) {
                barPos = width / 2 - getWidth() / 2;
            }


            mTextsLock.lock();
            if (!texts.isEmpty()) {
                final FontRenderContext context = g2.getFontRenderContext();
                final Font font = new Font(getFont().getName(),
                                           getFont().getStyle(),
                                           (int) (getFont().getSize() * 1.75));

                final double maxY = getHeight() / 2 - 30;

                int y = 0;
                for (final Map.Entry<String, Integer> textEntry : texts.entrySet()) {
                    if (textEntry.getKey() == null || textEntry.getKey().isEmpty()) {
                        continue;
                    }
                    final int alpha = textEntry.getValue();
                    final TextLayout layout = new TextLayout(textEntry.getKey(),
                                                             new Font(font.getName(),
                                                                      font.getStyle(),
                                                                      font.getSize()),
                                                             context);
                    final Rectangle2D bounds = layout.getBounds();
                    final Color f;
                    if (failuresMap.contains(textEntry.getKey())) {
                        f = new Color(255, 0, 0);
                    } else {
                        f = AppDefaults.BACKGROUND_DARK;
                    }
                    g2.setColor(new Color(f.getRed(), f.getGreen(), f.getBlue(), alpha));
                    final Point2D textPos = textsPositions.get(textEntry.getKey());
                    final float textPosX;
                    final float textPosY;
                    if (textPos == null) {
                        textPosX = (float) (width - bounds.getWidth()) / 2;
                        textPosY = (float) (maxY + layout.getLeading() + 2 * layout.getAscent());
                    } else {
                        textPosX = (float) textPos.getX();
                        textPosY = (float) textPos.getY();
                    }
                    layout.draw(g2, textPosX, textPosY + y);
                    y += (((float) 27 / MAX_ALPHA_LEVEL) * alpha);
                }
            }
            mTextsLock.unlock();
            super.paintComponent(g2);
        }

    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        /* do nothing */
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        /* do nothing */
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        /* do nothing */
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
        /* do nothing */
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        /* do nothing */
    }

    @Override
    public void keyPressed(final KeyEvent e) {
        /* do nothing */
    }

    @Override
    public void keyReleased(final KeyEvent e) {
        /* do nothing */
    }

    @Override
    public void keyTyped(final KeyEvent e) {
        /* do nothing */
    }

    /** Animation thread. */
    private class Animator implements Runnable {
        /** Whether the alpha level goes up or down. */
        private volatile boolean rampUp;

        protected Animator() {
            rampUp = true;
        }

        private void setRampUp(final boolean rampUp) {
            this.rampUp = rampUp;
        }

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
                    int newAlphaLevel = alphaLevel + (int) (MAX_ALPHA_LEVEL * (time - start) / rampDelay);
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
                    int newAlphaLevel = alphaLevel - (int) (MAX_ALPHA_LEVEL * (time - start) / RAMP_DELAY_STOP);
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

                final Collection<String> toRemove = new ArrayList<String>();
                mTextsLock.lock();
                for (final Map.Entry<String, Integer> textEntry : texts.entrySet()) {
                    int alpha = textEntry.getValue();
                    if (alpha < MAX_ALPHA_LEVEL) {
                        int delay = 1000;
                        if (failuresMap.contains(textEntry.getKey())) {
                            delay = 10000;
                        }
                        alpha -= (int) (MAX_ALPHA_LEVEL * (time - start) / delay);
                        if (alpha < 0) {
                            toRemove.add(textEntry.getKey());
                        } else {
                            texts.put(textEntry.getKey(), alpha);
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
                    Thread.sleep((long) (1000 / mainData.getAnimFPS()));
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                start = time;
                repaint();
                Thread.yield();
            }
            started = false;
            LOG.debug1("run: animator end");
        }
    }
}
