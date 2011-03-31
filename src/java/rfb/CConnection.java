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

abstract public class CConnection extends CMsgHandler {

  public CConnection() {
    state_ = RFBSTATE_UNINITIALISED;
    secTypes = new int[maxSecTypes];
  }

  // Methods to initialise the connection

  public void setServerName(String name) {
    serverName = name;
  }

  // setStreams() sets the streams to be used for the connection.  These must
  // be set before initialiseProtocol() and processMsg() are called.  The
  // CSecurity object may call setStreams() again to provide alternative
  // streams over which the RFB protocol is sent (i.e. encrypting/decrypting
  // streams).  Ownership of the streams remains with the caller
  // (i.e. SConnection will not delete them).
  public void setStreams(rdr.InStream is_, rdr.OutStream os_) {
    is = is_;
    os = os_;
  }

  public void initSecTypes() {
    nSecTypes = 0;
  }

  // addSecType() should be called once for each security type which the
  // client supports.  The order in which they're added is such that the
  // first one is most preferred.
  public void addSecType(int secType) {
    if (nSecTypes == maxSecTypes)
      throw new Exception("too many security types");
    secTypes[nSecTypes++] = secType;
  }

  // setShared sets the value of the shared flag which will be sent to the
  // server upon initialisation.
  public void setShared(boolean s) { shared = s; }

  // setProtocol3_3 configures whether or not the CConnection should
  // only ever support protocol version 3.3
  public void setProtocol3_3(boolean b) { useProtocol3_3 = b; }

  // initialiseProtocol() should be called once the streams and security
  // types are set.  Subsequently, processMsg() should be called whenever
  // there is data to read on the InStream.
  public void initialiseProtocol() {
    state_ = RFBSTATE_PROTOCOL_VERSION;
  }

  // processMsg() should be called whenever there is data to read on the
  // InStream.  You must have called initialiseProtocol() first.
  public void processMsg() {
    switch (state_) {

    case RFBSTATE_PROTOCOL_VERSION: processVersionMsg();        break;
    case RFBSTATE_SECURITY_TYPES:   processSecurityTypesMsg();  break;
    case RFBSTATE_SECURITY:         processSecurityMsg();       break;
    case RFBSTATE_SECURITY_RESULT:  processSecurityResultMsg(); break;
    case RFBSTATE_INITIALISATION:   processInitMsg();           break;
    case RFBSTATE_NORMAL:           reader_.readMsg();          break;
    case RFBSTATE_UNINITIALISED:
      throw new Exception("CConnection.processMsg: not initialised yet?");
    default:
      throw new Exception("CConnection.processMsg: invalid state");
    }
  }

  // Methods to be overridden in a derived class

  // getCSecurity() gets the CSecurity object for the given type.  The type
  // is guaranteed to be one of the secTypes passed in to addSecType().  The
  // CSecurity object's destroy() method will be called by the CConnection
  // from its destructor.
  abstract public CSecurity getCSecurity(int secType);

  // getCurrentCSecurity() gets the CSecurity instance used for this
  // connection.
  public CSecurity getCurrentCSecurity() { return security; }
  
  // setClientSecTypeOrder() determines whether the client should obey the
  // server's security type preference, by picking the first server security
  // type that the client supports, or whether it should pick the first type
  // that the server supports, from the client-supported list of types.
  public void setClientSecTypeOrder( boolean csto ) {
    clientSecTypeOrder = csto;
  }

  // authSuccess() is called when authentication has succeeded.
  public void authSuccess() {}

  // serverInit() is called when the ServerInit message is received.  The
  // derived class must call on to CConnection::serverInit().
  public void serverInit() {
    state_ = RFBSTATE_NORMAL;
    vlog.debug("initialisation done");
  }

  // Other methods

  public CMsgReader reader() { return reader_; }
  public CMsgWriter writer() { return writer_; }

  public rdr.InStream getInStream() { return is; }
  public rdr.OutStream getOutStream() { return os; }

  public String getServerName() { return serverName; }

  public static final int RFBSTATE_UNINITIALISED = 0;
  public static final int RFBSTATE_PROTOCOL_VERSION = 1;
  public static final int RFBSTATE_SECURITY_TYPES = 2;
  public static final int RFBSTATE_SECURITY = 3;
  public static final int RFBSTATE_SECURITY_RESULT = 4;
  public static final int RFBSTATE_INITIALISATION = 5;
  public static final int RFBSTATE_NORMAL = 6;
  public static final int RFBSTATE_INVALID = 7;

  public int state() { return state_; }

  protected void setState(int s) { state_ = s; }

  void processVersionMsg() {
    vlog.debug("reading protocol version");
    if (!cp.readVersion(is)) {
      state_ = RFBSTATE_INVALID;
      throw new Exception("reading version failed: not an RFB server?");
    }

    vlog.info("Server supports RFB protocol version "+cp.majorVersion+"."+
              cp.minorVersion);

    // The only official RFB protocol versions are currently 3.3, 3.7 and 3.8
    if (cp.beforeVersion(3,3)) {
      String msg = ("Server gave unsupported RFB protocol version "+
                    cp.majorVersion+"."+cp.minorVersion);
      vlog.error(msg);
      state_ = RFBSTATE_INVALID;
      throw new Exception(msg);
    } else if (useProtocol3_3 || cp.beforeVersion(3,7)) {
      cp.setVersion(3,3);
    } else if (cp.afterVersion(3,8)) {
      cp.setVersion(3,8);
    }

    cp.writeVersion(os);
    state_ = RFBSTATE_SECURITY_TYPES;

    vlog.info("Using RFB protocol version "+cp.majorVersion+"."+
              cp.minorVersion);
  }

  void processSecurityTypesMsg() {
    vlog.debug("processing security types message");

    int secType = SecTypes.invalid;

    if (cp.majorVersion == 3 && cp.minorVersion == 3) {

      // legacy 3.3 server may only offer "vnc authentication" or "none"

      secType = is.readU32();
      if (secType == SecTypes.invalid) {
        throwConnFailedException();

      } else if (secType == SecTypes.none || secType == SecTypes.vncAuth) {
        int j;
        for (j = 0; j < nSecTypes; j++)
          if (secTypes[j] == secType) break;
        if (j == nSecTypes)
          secType = SecTypes.invalid;
      } else {
        vlog.error("Unknown 3.3 security type "+secType);
        throw new Exception("Unknown 3.3 security type");
      }

    } else {

      // 3.7 server will offer us a list

      int nServerSecTypes = is.readU8();
      if (nServerSecTypes == 0)
        throwConnFailedException();

      int secTypePos = nSecTypes;
      for (int i = 0; i < nServerSecTypes; i++) {
        int serverSecType = is.readU8();
        vlog.debug("Server offers security type "+
                   SecTypes.name(serverSecType)+"("+serverSecType+")");

        // If we haven't already chosen a secType, try this one
        if (secType == SecTypes.invalid || clientSecTypeOrder ) {
          for (int j = 0; j < nSecTypes; j++) {
            if (secTypes[j] == serverSecType && j < secTypePos) {
              secType = secTypes[j];
              secTypePos = j;
              break;
            }
          }
          // Continue reading the remaining server secTypes, but ignore them
        }
      }

      if (secType != SecTypes.invalid) {
        os.writeU8(secType);
        os.flush();
        vlog.debug("Choosing security type "+SecTypes.name(secType)+
                   "("+secType+")");
      }
    }

    if (secType == SecTypes.invalid) {
      state_ = RFBSTATE_INVALID;
      vlog.error("No matching security types");
      throw new Exception("No matching security types");
    }

    state_ = RFBSTATE_SECURITY;
    security = getCSecurity(secType);
    processSecurityMsg();
  }

  void processSecurityMsg() {
    vlog.debug("processing security message");
    int rc = security.processMsg(this);
    if (rc == 0)
      throwAuthFailureException();
    if (rc == 1) {
      state_ = RFBSTATE_SECURITY_RESULT;
      processSecurityResultMsg();
    }
  }

  void processSecurityResultMsg() {
    vlog.debug("processing security result message");
    int result;
    if (cp.beforeVersion(3,8) && security.getType() == SecTypes.none) {
      result = SecTypes.resultOK;
    } else {
      //if (!is->checkNoWait(1)) return;
      result = is.readU32();
    }
    switch (result) {
    case SecTypes.resultOK:
      securityCompleted();
      break;
    case SecTypes.resultFailed:
      vlog.debug("auth failed");
      throwAuthFailureException();
    case SecTypes.resultTooMany:
      vlog.debug("auth failed - too many tries");
      throwAuthFailureException();
    default:
      vlog.error("unknown security result");
      throwAuthFailureException();
    }
  }

  void processInitMsg() {
    vlog.debug("reading server initialisation");
    reader_.readServerInit();
  }

  void throwAuthFailureException() {
    String reason;
    vlog.debug("state="+state()+", ver="+cp.majorVersion+"."+cp.minorVersion);
    if (state() == RFBSTATE_SECURITY_RESULT && !cp.beforeVersion(3,8)) {
      reason = is.readString();
    } else {
      reason = "Authentication failure";
    }
    state_ = RFBSTATE_INVALID;
    vlog.error(reason);
    throw new AuthFailureException(reason);
  }

  void throwConnFailedException() {
    state_ = RFBSTATE_INVALID;
    String reason = is.readString();
    throw new ConnFailedException(reason);
  }

  void securityCompleted() {
    state_ = RFBSTATE_INITIALISATION;
    reader_ = new CMsgReaderV3(this, is);
    writer_ = new CMsgWriterV3(cp, os);
    vlog.debug("Authentication success!");
    authSuccess();
    writer_.writeClientInit(shared);
  }

  rdr.InStream is;
  rdr.OutStream os;
  CMsgReader reader_;
  CMsgWriter writer_;
  boolean shared;
  CSecurity security;
  public static final int maxSecTypes = 8;
  int nSecTypes;
  int[] secTypes;
  int state_;

  String serverName;

  boolean useProtocol3_3;
  
  boolean clientSecTypeOrder;

  static LogWriter vlog = new LogWriter("CConnection");
}
