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

abstract public class Decoder {

  abstract public void readRect(int x, int y, int w, int h,
                                CMsgHandler handler);

  static public boolean supported(int encoding) {
    return (encoding == Encodings.raw || encoding == Encodings.RRE ||
            encoding == Encodings.hextile || encoding == Encodings.ZRLE);
  }
  static public Decoder createDecoder(int encoding, CMsgReader reader) {
    switch(encoding) {
    case Encodings.raw:     return new RawDecoder(reader);
    case Encodings.RRE:     return new RREDecoder(reader);
    case Encodings.hextile: return new HextileDecoder(reader);
    case Encodings.ZRLE:    return new ZRLEDecoder(reader);
    }
    return null;
  }
}
