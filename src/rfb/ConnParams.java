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

public class ConnParams {

  public ConnParams() {
  }

  public boolean readVersion(rdr.InStream is) {
    byte[] b = new byte[12];
    is.readBytes(b, 0, 12);
    try {
      if ((b[0] != 'R') || (b[1] != 'F') || (b[2] != 'B') || (b[3] != ' ')
          || (b[4] < '0') || (b[4] > '9') || (b[5] < '0') || (b[5] > '9')
          || (b[6] < '0') || (b[6] > '9') || (b[7] != '.')
          || (b[8] < '0') || (b[8] > '9') || (b[9] < '0') || (b[9] > '9')
          || (b[10] < '0') || (b[10] > '9') || (b[11] != '\n'))
      {
        return false;
      }
    } catch ( ArrayIndexOutOfBoundsException e ) {
      // On IE 5.0, the above test causes an exception if executed unmodified.
      // Wrapping it inside a try/catch block is the cleanest way of fixing it.
      return false;
    }
    majorVersion = (b[4] - '0') * 100 + (b[5] - '0') * 10 + (b[6] - '0');
    minorVersion = (b[8] - '0') * 100 + (b[9] - '0') * 10 + (b[10] - '0');
    return true;
  }

  public void writeVersion(rdr.OutStream os) {
    byte[] b = new byte[12];
    b[0] = (byte)'R'; b[1] = (byte)'F'; b[2] = (byte)'B'; b[3] = (byte)' ';
    b[4] = (byte)('0' + (majorVersion / 100) % 10);
    b[5] = (byte)('0' + (majorVersion / 10) % 10);
    b[6] = (byte)('0' + majorVersion % 10);
    b[7] = (byte)'.';
    b[8] = (byte)('0' + (minorVersion / 100) % 10);
    b[9] = (byte)('0' + (minorVersion / 10) % 10);
    b[10] = (byte)('0' + minorVersion % 10);
    b[11] = (byte)'\n';
    os.writeBytes(b, 0, 12);
    os.flush();
  }

  public int majorVersion;
  public int minorVersion;

  public void setVersion(int major, int minor) {
    majorVersion = major; minorVersion = minor;
  }
  public boolean isVersion(int major, int minor) {
    return majorVersion == major && minorVersion == minor;
  }
  public boolean beforeVersion(int major, int minor) {
    return (majorVersion < major ||
            (majorVersion == major && minorVersion < minor));
  }
  public boolean afterVersion(int major, int minor) {
    return !beforeVersion(major,minor+1);
  }

  public int width;
  public int height;

  public PixelFormat pf() { return pf_; }
  public void setPF(PixelFormat pf) {
    pf_ = pf;
    if (pf.bpp != 8 && pf.bpp != 16 && pf.bpp != 32) {
      throw new Exception("setPF: not 8, 16 or 32 bpp?");
    }
  }

  public String name;

  public int currentEncoding() { return currentEncoding_; }
  public int nEncodings() { return nEncodings_; }
  public int[] encodings() { return encodings_; }
  public void setEncodings(int nEncodings, int[] encodings)
  {
    if (nEncodings > nEncodings_) {
      encodings_ = new int[nEncodings];
    }
    nEncodings_ = nEncodings;
    useCopyRect = false;
    supportsLocalCursor = false;
    supportsDesktopResize = false;
    currentEncoding_ = Encodings.raw;

    for (int i = nEncodings-1; i >= 0; i--) {
      encodings_[i] = encodings[i];
      if (encodings[i] == Encodings.copyRect)
        useCopyRect = true;
      else if (encodings[i] == Encodings.pseudoEncodingCursor)
        supportsLocalCursor = true;
      else if (encodings[i] == Encodings.pseudoEncodingDesktopSize)
        supportsDesktopResize = true;
      else if (encodings[i] <= Encodings.max &&
               Encoder.supported(encodings[i]))
        currentEncoding_ = encodings[i];
    }
  }
  public boolean useCopyRect;
  public boolean supportsLocalCursor;
  public boolean supportsDesktopResize;

  private PixelFormat pf_;
  private int nEncodings_;
  private int[] encodings_;
  private int currentEncoding_;
}
