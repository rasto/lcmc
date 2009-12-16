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
// rfb.CMsgHandler
//

package rfb;

public class CMsgHandler {

  public CMsgHandler() {
    cp = new ConnParams();
  }
  public void setDesktopSize(int w, int h) {
    cp.width = w;
    cp.height = h;
  }
  public void setCursor(int hotspotX, int hotspotY, int w, int h,
                        byte[] data, byte[] mask) {}
  public void setPixelFormat(PixelFormat pf) {
    cp.setPF(pf);
  }
  public void setName(String name) {
    cp.name = name;
  }

  public void serverInit() {
    throw new Exception("CMsgHandler.serverInit called");
  }

  public void framebufferUpdateStart() {}
  public void framebufferUpdateEnd() {}
  public void beginRect(int x, int y, int w, int h, int encoding) {}
  public void endRect(int x, int y, int w, int h, int encoding) {}

  public void setColourMapEntries(int firstColour, int nColours, int[] rgbs) {
    throw new Exception("CMsgHandler.setColourMapEntries called");
  }
  public void bell() {}
  public void serverCutText(String str) {}

  public void fillRect(int x, int y, int w, int h, int pix) {}
  public void imageRect(int x, int y, int w, int h, byte[] pix, int offset) {}
  public void copyRect(int x, int y, int w, int h, int srcX, int srcY) {}

  public ConnParams cp;
}
