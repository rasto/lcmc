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

public class HextileDecoder extends Decoder {

  public HextileDecoder(CMsgReader reader_) { reader = reader_; }

  static final int BPP=8;
  static final int readPixel(rdr.InStream is) { return is.readU8(); }

  public void readRect(int x, int y, int w, int h, CMsgHandler handler) {
    rdr.InStream is = reader.getInStream();
    byte[] buf = reader.getImageBuf(16 * 16 * 4, 0);

    int bg = 0;
    int fg = 0;

    for (int ty = y; ty < y+h; ty += 16) {

      int th = Math.min(y+h-ty, 16);

      for (int tx = x; tx < x+w; tx += 16) {

        int tw = Math.min(x+w-tx, 16);

        int tileType = is.readU8();

        if ((tileType & Hextile.raw) != 0) {
          is.readBytes(buf, 0, tw * th * (BPP/8));
          handler.imageRect(tx,ty,tw,th, buf, 0);
          continue;
        }

        if ((tileType & Hextile.bgSpecified) != 0)
          bg = readPixel(is);

        int len = tw * th;
        int ptr = 0;
        while (len-- > 0) buf[ptr++] = (byte)bg;

        if ((tileType & Hextile.fgSpecified) != 0)
          fg = readPixel(is);

        if ((tileType & Hextile.anySubrects) != 0) {
          int nSubrects = is.readU8();

          for (int i = 0; i < nSubrects; i++) {

            if ((tileType & Hextile.subrectsColoured) != 0)
              fg = readPixel(is);

            int xy = is.readU8();
            int wh = is.readU8();
            int sx = ((xy >> 4) & 15);
            int sy = (xy & 15);
            int sw = ((wh >> 4) & 15) + 1;
            int sh = (wh & 15) + 1;
            ptr = sy * tw + sx;
            int rowAdd = tw - sw;
            while (sh-- > 0) {
              len = sw;
              while (len-- > 0) buf[ptr++] = (byte)fg;
              ptr += rowAdd;
            }
          }
        }
        handler.imageRect(tx,ty,tw,th, buf, 0);
      }
    }
  }

  CMsgReader reader;
}
