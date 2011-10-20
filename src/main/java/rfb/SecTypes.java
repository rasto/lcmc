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
// SecTypes.java - constants for the various security types.
//

package rfb;

public class SecTypes {
  public static final int invalid = 0;
  public static final int none    = 1;
  public static final int vncAuth = 2;

  public static final int RA2     = 5;
  public static final int RA2ne   = 6;

  public static final int tight   = 16;
  public static final int ultra   = 17;
  public static final int TLS     = 18;

  // result types

  public static final int resultOK = 0;
  public static final int resultFailed = 1;
  public static final int resultTooMany = 2; // deprecated

  public static String name(int num) {
    switch (num) {
    case none:       return "None";
    case vncAuth:    return "VncAuth";
    case RA2:        return "RA2";
    case RA2ne:      return "RA2ne";
    default:         return "[unknown secType]";
    }
  }
  public static int num(String name) {
    if (name.equalsIgnoreCase("None"))    return none;
    if (name.equalsIgnoreCase("VncAuth")) return vncAuth;
    if (name.equalsIgnoreCase("RA2"))     return RA2;
    if (name.equalsIgnoreCase("RA2ne"))	  return RA2ne;
    return invalid;
  }
  //std::list<int> parseSecTypes(const char* types);
}
