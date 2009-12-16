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

public class CSecurityVncAuth extends CSecurity {

  public CSecurityVncAuth(UserPasswdGetter upg_) { upg = upg_; }

  public int processMsg(CConnection cc) {
    rdr.InStream is = cc.getInStream();
    rdr.OutStream os = cc.getOutStream();

    byte[] challenge = new byte[VncAuth.challengeSize];
    is.readBytes(challenge, 0, VncAuth.challengeSize);
    StringBuffer passwd = new StringBuffer();
    if (!upg.getUserPasswd(null, passwd)) {
      vlog.error("Getting password failed");
      return 0;
    }
    VncAuth.encryptChallenge(challenge, passwd.toString());
    os.writeBytes(challenge, 0, VncAuth.challengeSize);
    os.flush();
    return 1;
  }

  public int getType() { return SecTypes.vncAuth; }

  UserPasswdGetter upg;

  static LogWriter vlog = new LogWriter("VncAuth");
  public String description() { return "No Encryption"; }
}
