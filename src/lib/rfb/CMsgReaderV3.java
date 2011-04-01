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

public class CMsgReaderV3 extends CMsgReader {

  public CMsgReaderV3(CMsgHandler handler_, rdr.InStream is_) {
    super(handler_, is_);
  }

  public void readServerInit() {
    int width = is.readU16();
    int height = is.readU16();
    handler.setDesktopSize(width, height);
    PixelFormat pf = new PixelFormat();
    pf.read(is);
    handler.setPixelFormat(pf);
    String name = is.readString();
    handler.setName(name);
    endMsg();
    handler.serverInit();
  }

  public void readMsg() {
    if (nUpdateRectsLeft == 0) {

      int type = is.readU8();
      switch (type) {
      case MsgTypes.framebufferUpdate:   readFramebufferUpdate(); break;
      case MsgTypes.setColourMapEntries: readSetColourMapEntries(); break;
      case MsgTypes.bell:                readBell(); break;
      case MsgTypes.serverCutText:       readServerCutText(); break;
      default:
        vlog.error("unknown message type "+type);
        throw new Exception("unknown message type");
      }

    } else {

      int x = is.readU16();
      int y = is.readU16();
      int w = is.readU16();
      int h = is.readU16();
      int encoding = is.readU32();

      switch (encoding) {
      case Encodings.pseudoEncodingDesktopSize:
        handler.setDesktopSize(w, h);
        break;
      case Encodings.pseudoEncodingCursor:
        readSetCursor(x, y, w, h);
        break;
      default:
        readRect(x, y, w, h, encoding);
        break;
      }

      nUpdateRectsLeft--;
      if (nUpdateRectsLeft == 0) handler.framebufferUpdateEnd();
    }
  }

  void readFramebufferUpdate() {
    is.skip(1);
    nUpdateRectsLeft = is.readU16();
    endMsg();
    handler.framebufferUpdateStart();
  }

  int nUpdateRectsLeft;

  static LogWriter vlog = new LogWriter("CMsgReaderV3");
}
