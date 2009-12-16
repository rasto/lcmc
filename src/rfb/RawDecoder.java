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

public class RawDecoder extends Decoder {

  public RawDecoder(CMsgReader reader_) { reader = reader_; }

  public void readRect(int x, int y, int w, int h, CMsgHandler handler) {
    byte[] imageBuf = reader.getImageBuf(w, w*h);
    int nPixels = imageBuf.length / (reader.bpp() / 8);
    int nRows = nPixels / w;
    int bytesPerRow = w * (reader.bpp() / 8);
    while (h > 0) {
      if (nRows > h) nRows = h;
      reader.getInStream().readBytes(imageBuf, 0, nRows * bytesPerRow);
      handler.imageRect(x, y, w, nRows, imageBuf, 0);
      h -= nRows;
      y += nRows;
    }
  }

  CMsgReader reader;
}
