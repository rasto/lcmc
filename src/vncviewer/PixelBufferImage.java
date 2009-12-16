/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * 
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */
//
// PixelBufferImage is an rfb.PixelBuffer which also acts as an ImageProducer.
// Currently it only supports 8-bit colourmapped pixel format.
//

package vncviewer;

import java.awt.*;
import java.awt.image.*;

public class PixelBufferImage extends rfb.PixelBuffer implements ImageProducer
{
  public PixelBufferImage(int w, int h, java.awt.Component win) {
    setPF(new rfb.PixelFormat(8, 8, false, false, 0, 0, 0, 0, 0, 0));

    resize(w, h, win);

    reds = new byte[256];
    greens = new byte[256];
    blues = new byte[256];
    // Fill the colour map with bgr233.  This is only so that if the server
    // doesn't set the colour map properly, at least we're likely to see
    // something instead of a completely black screen.
    for (int i = 0; i < 256; i++) {
      reds[i] = (byte)(((i & 7) * 255 + 3) / 7);
      greens[i] = (byte)((((i >> 3) & 7) * 255 + 3) / 7);
      blues[i] = (byte)((((i >> 6) & 3) * 255 + 1) / 3);
    }
    cm = new IndexColorModel(8, 256, reds, greens, blues);
  }

  // resize() resizes the image, preserving the image data where possible.
  public void resize(int w, int h, java.awt.Component win) {
    if (w == width() && h == height()) return;

    // Clear the ImageConsumer so that we don't attempt to do any drawing until
    // the AWT has noticed that the resize has happened.
    ic = null;

    int oldStrideBytes = getStride() * (format.bpp/8);
    int rowsToCopy = h < height() ? h : height();
    int bytesPerRow = (w < width() ? w : width()) * (format.bpp/8);
    byte[] oldData = data;

    width_ = w;
    height_ = h;
    image = win.createImage(this);

    data = new byte[width() * height() * (format.bpp/8)];

    int newStrideBytes = getStride() * (format.bpp/8);
    for (int i = 0; i < rowsToCopy; i++)
      System.arraycopy(oldData, oldStrideBytes * i,
                       data, newStrideBytes * i, bytesPerRow);
  }

  // put() causes the given rectangle to be drawn using the given graphics
  // context.
  public void put(int x, int y, int w, int h, Graphics g) {
    if (ic == null) return;
    ic.setPixels(x, y, w, h, cm, data, width() * y + x, width());
    ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
    g.setClip(x, y, w, h);
    g.drawImage(image, 0, 0, null);
  }

  // fillRect(), imageRect(), maskRect() are inherited from PixelBuffer.  For
  // copyRect() we also need to tell the ImageConsumer that the pixels have
  // changed (this is done in the put() call for the others).

  public void copyRect(int x, int y, int w, int h, int srcX, int srcY) {
    super.copyRect(x, y, w, h, srcX, srcY);
    if (ic == null) return;
    ic.setPixels(x, y, w, h, cm, data, width() * y + x, width());
    ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
  }

  // setColourMapEntries() changes some of the entries in the colourmap.
  // However these settings won't take effect until updateColourMap() is
  // called.  This is because getting java to recalculate its internal
  // translation table and redraw the screen is expensive.

  public void setColourMapEntries(int firstColour, int nColours,
                                               int[] rgbs) {
    for (int i = 0; i < nColours; i++) {
      reds  [firstColour+i] = (byte)(rgbs[i*3]   >> 8);
      greens[firstColour+i] = (byte)(rgbs[i*3+1] >> 8);
      blues [firstColour+i] = (byte)(rgbs[i*3+2] >> 8);
    }
  }

  // ImageProducer methods

  public void updateColourMap() {
    cm = new IndexColorModel(8, 256, reds, greens, blues);
  }

  public void addConsumer(ImageConsumer c) {
    vlog.debug("adding consumer "+c);
    ic = c;
    ic.setDimensions(width(), height());
    ic.setHints(ImageConsumer.RANDOMPIXELORDER);
    // Calling ic.setColorModel(cm) seemed to help in some earlier versions of
    // the JDK, but it shouldn't be necessary because we pass the ColorModel
    // with each setPixels() call.
    ic.setPixels(0, 0, width(), height(), cm, data, 0, width());
    ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
  }

  public void removeConsumer(ImageConsumer c) {
    System.err.println("removeConsumer "+c);
    if (ic == c) ic = null;
  }

  public boolean isConsumer(ImageConsumer c) { return ic == c; }
  public void requestTopDownLeftRightResend(ImageConsumer c) {}
  public void startProduction(ImageConsumer c) { addConsumer(c); }

  Image image;
  Graphics graphics;
  ImageConsumer ic;
  ColorModel cm;

  byte[] reds;
  byte[] greens;
  byte[] blues;

  static rfb.LogWriter vlog = new rfb.LogWriter("PixelBufferImage");
}
