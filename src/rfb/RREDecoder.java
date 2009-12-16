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

package rfb;

public class RREDecoder extends Decoder {

  public RREDecoder(CMsgReader reader_) { reader = reader_; }

  static final int BPP=8;
  static final int readPixel(rdr.InStream is) { return is.readU8(); }

  public void readRect(int x, int y, int w, int h, CMsgHandler handler) {
    rdr.InStream is = reader.getInStream();
    int nSubrects = is.readU32();
    int bg = readPixel(is);
    handler.fillRect(x,y,w,h, bg);

    for (int i = 0; i < nSubrects; i++) {
      int pix = readPixel(is);
      int sx = is.readU16();
      int sy = is.readU16();
      int sw = is.readU16();
      int sh = is.readU16();
      handler.fillRect(x+sx, y+sy, sw, sh, pix);
    }
  }

  CMsgReader reader;
}
