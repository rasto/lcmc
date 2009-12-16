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

public class Encodings {

  public static final int raw = 0;
  public static final int copyRect = 1;
  public static final int RRE = 2;
  public static final int coRRE = 4;
  public static final int hextile = 5;
  public static final int ZRLE = 16;

  public static final int max = 255;

  public static final int pseudoEncodingCursor = 0xffffff11;
  public static final int pseudoEncodingDesktopSize = 0xffffff21;

  public static int num(String name) {
    if (name.equalsIgnoreCase("raw"))      return raw;
    if (name.equalsIgnoreCase("copyRect")) return copyRect;
    if (name.equalsIgnoreCase("RRE"))      return RRE;
    if (name.equalsIgnoreCase("coRRE"))    return coRRE;
    if (name.equalsIgnoreCase("hextile"))  return hextile;
    if (name.equalsIgnoreCase("ZRLE"))     return ZRLE;
    return -1;
  }

  public static String name(int num) {
    switch (num) {
    case raw:          return "raw";
    case copyRect:     return "copyRect";
    case RRE:          return "RRE";
    case coRRE:        return "CoRRE";
    case hextile:      return "hextile";
    case ZRLE:         return "ZRLE";
    default:           return "[unknown encoding]";
    }
  }
}
