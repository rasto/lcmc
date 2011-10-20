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
// VNCViewer - the VNC viewer applet.  It can also be run from the
// command-line, when it behaves as much as possibly like the windows and unix
// viewers.
//
// Unfortunately, because of the way Java classes are loaded on demand, only
// configuration parameters defined in this file can be set from the command
// line or in applet parameters.

package vncviewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;

public @SuppressWarnings({"unchecked", "deprecation", "serial"}) class VNCViewer extends java.applet.Applet implements Runnable
{
  public static final String version = "4.1";
  public static final String about1 = "VNC Viewer Free Edition "+version;
  public static final String about2 = "Copyright (C) 2002-2005 RealVNC Ltd.";
  public static final String about3 = ("See http://www.realvnc.com for "+
                                       "information on VNC.");
  public static final String aboutText = about1+"\n"+about2+"\n"+about3;

  public static void main(String[] argv) {
    VNCViewer viewer = new VNCViewer(argv);
    viewer.start();
  }
  
  public VNCViewer(String[] argv) {
    applet = false;
    
    // Override defaults with command-line options
    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equalsIgnoreCase("-log")) {
        if (++i >= argv.length) usage();
        System.err.println("Log setting: "+argv[i]);
        rfb.LogWriter.setLogParams(argv[i]);
        continue;
      }

      if (rfb.Configuration.setParam(argv[i]))
        continue;

      if (argv[i].charAt(0) == '-') {
        if (i+1 < argv.length) {
          if (rfb.Configuration.setParam(argv[i].substring(1), argv[i+1])) {
            i++;
            continue;
          }
        }
        usage();
      }

      if (vncServerName.getValue() != null)
        usage();
      vncServerName.setParam(argv[i]);
    }
  }

  public static void usage() {
    String usage = ("\nusage: vncviewer [options/parameters] "+
                    "[host:displayNum] [options/parameters]\n"+
                    //"       vncviewer [options/parameters] -listen [port] "+
                    //"[options/parameters]\n"+
                    "\n"+
                    "Options:\n"+
                    "  -log <level>    configure logging level\n"+
                    "\n"+
                    "Parameters can be turned on with -<param> or off with "+
                    "-<param>=0\n"+
                    "Parameters which take a value can be specified as "+
                    "-<param> <value>\n"+
                    "Other valid forms are <param>=<value> -<param>=<value> "+
                    "--<param>=<value>\n"+
                    "Parameter names are case-insensitive.  The parameters "+
                    "are:\n\n"+
                    rfb.Configuration.listParams());
    System.err.print(usage);
    System.exit(1);
  }

  public VNCViewer() {
    applet = true;
    firstApplet = true;
  }

  public static void newViewer(VNCViewer oldViewer) {
    VNCViewer viewer = new VNCViewer();
    viewer.applet = oldViewer.applet;
    viewer.firstApplet = false;
    viewer.start();
  }


  public void init() {
    vlog.debug("init called");
    setBackground(Color.white);
    logo = getImage(getDocumentBase(), "logo150x150.gif");
  }

  public void start() {
    vlog.debug("start called");
    nViewers++;
    if (firstApplet) {
      alwaysShowServerDialog.setParam(true);
      rfb.Configuration.readAppletParams(this);
      String host = getCodeBase().getHost();
      if (vncServerName.getValue() == null && vncServerPort.getValue() != 0) {
        int port = vncServerPort.getValue();
        vncServerName.setParam(host + ((port >= 5900 && port <= 5999)
                                       ? (":"+(port-5900))
                                       : ("::"+port)));
      }
    }
    thread = new Thread(this);
    thread.start();
  }

  public void join() {
    try {
      thread.join();
    } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
    }
  }

  public void paint(Graphics g) {
    g.drawImage(logo, 0, 0, this);
    int h = logo.getHeight(this)+20;
    g.drawString(about1, 0, h);
    h += g.getFontMetrics().getHeight();
    g.drawString(about2, 0, h);
    h += g.getFontMetrics().getHeight();
    g.drawString(about3, 0, h);
  }

  public void run() {
    CConn cc = null;
    try {
      cc = new CConn(this);
      if (cc.init(null, vncServerName.getValue(),
                  alwaysShowServerDialog.getValue())) {
        while (true)
          cc.processMsg();
      }
    } catch (rdr.EndOfStream e) {
      vlog.info(e.toString());
    } catch (Exception e) {
      if (cc != null) cc.removeWindow();
      if (cc == null || !cc.shuttingDown) {
        e.printStackTrace();
        new MessageBox(e.toString());
      }
    }
    if (cc != null) cc.removeWindow();
    nViewers--;
    if (!applet && nViewers == 0) {
      System.exit(0);
    }
  }

  rfb.BoolParameter fastCopyRect
  = new rfb.BoolParameter("FastCopyRect",
                          "Use fast CopyRect - turn this off if you get "+
                          "screen corruption when copying from off-screen",
                          true);
  rfb.BoolParameter useLocalCursor
  = new rfb.BoolParameter("UseLocalCursor",
                          "Render the mouse cursor locally", true);
  rfb.BoolParameter autoSelect
  = new rfb.BoolParameter("AutoSelect",
                          "Auto select pixel format and encoding", true);
  rfb.BoolParameter fullColour
  = new rfb.BoolParameter("FullColour",
                          "Use full colour - otherwise 6-bit colour is used "+
                          "until AutoSelect decides the link is fast enough",
                          false);
  rfb.AliasParameter fullColor
  = new rfb.AliasParameter("FullColor", "Alias for FullColour", fullColour);
  rfb.StringParameter preferredEncoding
  = new rfb.StringParameter("PreferredEncoding",
                            "Preferred encoding to use (ZRLE, hextile or"+
                            " raw) - implies AutoSelect=0", null);
  rfb.BoolParameter viewOnly
  = new rfb.BoolParameter("ViewOnly", "Don't send any mouse or keyboard "+
                          "events to the server", false);
  rfb.BoolParameter shared
  = new rfb.BoolParameter("Shared", "Don't disconnect other viewers upon "+
                          "connection - share the desktop instead", false);
  rfb.BoolParameter acceptClipboard
  = new rfb.BoolParameter("AcceptClipboard",
                          "Accept clipboard changes from the server", true);
  rfb.BoolParameter sendClipboard
  = new rfb.BoolParameter("SendClipboard",
                          "Send clipboard changes to the server", true);
  rfb.BoolParameter alwaysShowServerDialog
  = new rfb.BoolParameter("AlwaysShowServerDialog",
                          "Always show the server dialog even if a server "+
                          "has been specified in an applet parameter or on "+
                          "the command line", false);
  rfb.StringParameter vncServerName
  = new rfb.StringParameter("Server",
                            "The VNC server <host>[:<dpyNum>] or "+
                            "<host>::<port>", null);
  rfb.IntParameter vncServerPort
  = new rfb.IntParameter("Port",
                         "The VNC server's port number, assuming it is on "+
                         "the host from which the applet was downloaded", 0);

  Thread thread;
  boolean applet, firstApplet;
  Image logo;
  Label versionLabel;
  static int nViewers;
  static rfb.LogWriter vlog = new rfb.LogWriter("main");
}
