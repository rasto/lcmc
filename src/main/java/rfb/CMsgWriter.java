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

abstract public class CMsgWriter {

  abstract public void writeClientInit(boolean shared);

  public void writeSetPixelFormat(PixelFormat pf) {
    startMsg(MsgTypes.setPixelFormat);                                 
    os.pad(3);
    pf.write(os);
    endMsg();
  }

  public void writeSetEncodings(int nEncodings, int[] encodings) {
    startMsg(MsgTypes.setEncodings);
    os.skip(1);
    os.writeU16(nEncodings);
    for (int i = 0; i < nEncodings; i++)
      os.writeU32(encodings[i]);
    endMsg();
  }

  // Ask for encodings based on which decoders are supported.  Assumes higher
  // encoding numbers are more desirable.

  public void writeSetEncodings(int preferredEncoding, boolean useCopyRect) {
    int nEncodings = 0;
    int[] encodings = new int[Encodings.max+2];
    if (cp.supportsLocalCursor)
      encodings[nEncodings++] = Encodings.pseudoEncodingCursor;
    if (cp.supportsDesktopResize)
      encodings[nEncodings++] = Encodings.pseudoEncodingDesktopSize;
    if (Decoder.supported(preferredEncoding)) {
      encodings[nEncodings++] = preferredEncoding;
    }
    if (useCopyRect) {
      encodings[nEncodings++] = Encodings.copyRect;
    }
    for (int i = Encodings.max; i >= 0; i--) {
      if (i != preferredEncoding && Decoder.supported(i)) {
        encodings[nEncodings++] = i;
      }
    }
    writeSetEncodings(nEncodings, encodings);
  }

  public void writeFramebufferUpdateRequest(int x, int y, int w, int h,
                                            boolean incremental) {
    startMsg(MsgTypes.framebufferUpdateRequest);
    os.writeU8(incremental?1:0);
    os.writeU16(x);
    os.writeU16(y);
    os.writeU16(w);
    os.writeU16(h);
    endMsg();
  }

  public void writeKeyEvent(int key, boolean down) {
    startMsg(MsgTypes.keyEvent);
    os.writeU8(down?1:0);
    os.pad(2);
    os.writeU32(key);
    endMsg();
  }

  public void writePointerEvent(int x, int y, int buttonMask) {
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    if (x >= cp.width) x = cp.width - 1;
    if (y >= cp.height) y = cp.height - 1;

    startMsg(MsgTypes.pointerEvent);
    os.writeU8(buttonMask);
    os.writeU16(x);
    os.writeU16(y);
    endMsg();
  }

  public void writeClientCutText(String str) {
    startMsg(MsgTypes.clientCutText);
    os.pad(3);
    os.writeString(str);
    endMsg();
  }

  abstract public void startMsg(int type);
  abstract public void endMsg();

  public void setOutStream(rdr.OutStream os_) { os = os_; }

  ConnParams getConnParams() { return cp; }
  rdr.OutStream getOutStream() { return os; }

  protected CMsgWriter(ConnParams cp_, rdr.OutStream os_) {cp = cp_; os = os_;}

  ConnParams cp;
  rdr.OutStream os;
}
