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

public class ZRLEDecoder extends Decoder {

  public ZRLEDecoder(CMsgReader reader_) {
    reader = reader_;
    zis = new rdr.ZlibInStream();
  }

  static final int BPP=8;
  static final int readPixel(rdr.InStream is) { return is.readU8(); }

  public void readRect(int x, int y, int w, int h, CMsgHandler handler) {
    rdr.InStream is = reader.getInStream();
    byte[] buf = reader.getImageBuf(64 * 64 * 4, 0);

    int length = is.readU32();
    zis.setUnderlying(is, length);

    for (int ty = y; ty < y+h; ty += 64) {

      int th = Math.min(y+h-ty, 64);

      for (int tx = x; tx < x+w; tx += 64) {

        int tw = Math.min(x+w-tx, 64);

        int mode = zis.readU8();
        boolean rle = (mode & 128) != 0;
        int palSize = mode & 127;
        int[] palette = new int[128];

        for (int i = 0; i < palSize; i++) {
          palette[i] = readPixel(zis);
        }

        if (palSize == 1) {
          int pix = palette[0];
          handler.fillRect(tx,ty,tw,th, pix);
          continue;
        }

        if (!rle) {
          if (palSize == 0) {

            // raw

            zis.readBytes(buf, 0, tw * th * (BPP / 8));

          } else {

            // packed pixels
            int bppp = ((palSize > 16) ? 8 :
                        ((palSize > 4) ? 4 : ((palSize > 2) ? 2 : 1)));

            int ptr = 0;

            for (int i = 0; i < th; i++) {
              int eol = ptr + tw;
              int b = 0;
              int nbits = 0;

              while (ptr < eol) {
                if (nbits == 0) {
                  b = zis.readU8();
                  nbits = 8;
                }
                nbits -= bppp;
                int index = (b >> nbits) & ((1 << bppp) - 1) & 127;
                buf[ptr++] = (byte)palette[index];
              }
            }
          }

        } else {

          if (palSize == 0) {

            // plain RLE

            int ptr = 0;
            int end = ptr + tw * th;
            while (ptr < end) {
              int pix = readPixel(zis);
              int len = 1;
              int b;
              do {
                b = zis.readU8();
                len += b;
              } while (b == 255);

              if (!(len <= end - ptr))
                throw new Exception("ZRLEDecoder: assertion (len <= end - ptr)"
                                    +" failed");

              while (len-- > 0) buf[ptr++] = (byte)pix;
            }
          } else {

            // palette RLE

            int ptr = 0;
            int end = ptr + tw * th;
            while (ptr < end) {
              int index = zis.readU8();
              int len = 1;
              if ((index & 128) != 0) {
                int b;
                do {
                  b = zis.readU8();
                  len += b;
                } while (b == 255);

                if (!(len <= end - ptr))
                  throw new Exception("ZRLEDecoder: assertion "
                                      +"(len <= end - ptr) failed");
              }

              index &= 127;

              int pix = palette[index];

              while (len-- > 0) buf[ptr++] = (byte)pix;
            }
          }
        }

        handler.imageRect(tx,ty,tw,th, buf, 0);
      }
    }

    zis.reset();
  }

  CMsgReader reader;
  rdr.ZlibInStream zis;
}
