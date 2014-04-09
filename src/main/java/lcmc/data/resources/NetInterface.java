/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */


package lcmc.data.resources;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lcmc.data.StringValue;
import lcmc.data.Value;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Unit;

/**
 * This class holds data of one network interface.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class NetInterface extends Resource implements Value {
    /** Logger. */
    private static final Logger LOG =
                                 LoggerFactory.getLogger(NetInterface.class);
    /** Ip address. */
    private final String ip;
    /** Cidr. */
    private final Integer cidr;
    /** Network ip. */
    private final String networkIp;
    /** Whether it is a bridge. */
    private final boolean bridge;


    /** Address family. */
    public enum AF {IPV4, IPV6, SSOCKS, SDP};
    /** Address family. */
    private final AF af;
    private final String IPV6_STRING = "ipv6";
    private final String IPV4_STRING = "ipv4";
    private final String SSOCKS_STRING = "ssocks";
    private final String SDP_STRING = "sdp";

    /**
     * Prepares a new {@code NetInterface} object.
     *
     * @param line
     *          line with interface, ip, mac addr and net mask  delimited
     *          with space
     */
    public NetInterface(final String line) throws UnknownHostException {
        super();
        LOG.debug1("NetInterface: " + line);
        final String[] cols = line.split(" ");
        if (cols.length < 4) {
            LOG.appWarning("NetInterface: cannot parse: " + line);
        }
        String iface = "unknown";
        if (cols.length > 0) {
            iface = cols[0];
        }
        String ip0 = null;
        AF af0 = null;
        int size = 4;
        if (cols.length > 2) {
            final String af_string = cols[1];
            ip0 = cols[2];
            if (IPV6_STRING.equals(af_string)) {
                af0 = AF.IPV6;
                size = 16;
            } else if (SSOCKS_STRING.equals(af_string)) {
                af0 = AF.SSOCKS;
            } else if (SDP_STRING.equals(af_string)) {
                af0 = AF.SDP;
            } else {
                af0 = AF.IPV4;
            }
        }
        this.ip = ip0;
        this.af = af0;
        if (cols.length > 3) {
            this.cidr = new Integer(cols[3]);
            this.networkIp = calcNetworkIp(getNumericIp(ip), cidr, size);
        } else {
            this.cidr = null;
            this.networkIp = null;
        }
        this.bridge = cols.length > 4 && "bridge".equals(cols[4]);
        setName(iface);
    }

    /**
     * Creates a new {@code NetInterface} object.
     *
     * @param iface
     *          network interface
     * @param ip
     *          ip address
     * @param cidr
     *          cidr
     */
    public NetInterface(final String iface,
                        final String ip,
                        final Integer cidr,
                        final boolean bridge,
                        final AF af) throws UnknownHostException {
        super(iface);
        this.ip = ip;
        this.cidr = cidr;
        this.bridge = bridge;
        this.af = af;
        int size = 4;
        if (af == AF.IPV6) {
            size = 16;
        }
        this.networkIp = calcNetworkIp(getNumericIp(ip), cidr, size);
    }

    private String calcNetworkIp(final BigInteger numericIp,
                                 final Integer cidr,
                                 final int size) {
        return getSymbolicIp(
                 numericIp.and(new BigInteger("2").pow(8 * size)
                                                  .subtract(new BigInteger("1"))
                                                  .shiftLeft(8 * size - cidr)),
                 size);
    }
 
    private BigInteger getNumericIp(final String ip) throws UnknownHostException {
        final byte[] bytes = InetAddress.getByName(ip).getAddress();
        BigInteger numericIp = new BigInteger("0");
        for (final byte b : bytes) {
            numericIp = numericIp.shiftLeft(8).add(new BigInteger(Long.toString(b & 0xff)));
        }
        return numericIp;
    }

    //private int getNumericIp(final String ip) {
    //    final String[] ipParts = ip.split("\\.");
    //    int numericIp = 0;
    //    for (final String ipPart : ipParts) {
    //        numericIp = (numericIp << 8) + new Integer(ipPart);
    //    }
    //    return numericIp;
    //}

    private static String getSymbolicIp(BigInteger numericIp,
                                        final int size) {
        final byte[] addr = new byte[size];
        for (int i = size - 1; i >= 0; i--) {
            final byte a = (byte) numericIp.and(
                                new BigInteger(Long.toString(0xff))).intValue();
            numericIp = numericIp.shiftRight(8);
            addr[i] = a;
        }
        try {
            return InetAddress.getByAddress(addr).getHostAddress();
        } catch (final UnknownHostException e) {
            LOG.appWarning("getSymbolicIp: unkonwn host: " + addr);
            return null;
        }
    }

    /** Returns ip. */
    public String getIp() {
        return ip;
    }

    /** Returns CIDR. */
    public Integer getCidr() {
        return cidr;
    }

    ///**
    // * Returns network ip. The ip has '*' instead of bytes, that are
    // * not part of the network. e.g. 192.168.1.1 and mask 255.255.255.0 gives
    // * 192.168.1.*
    // */
    //public final String getNetworkIp() {
    //    if (netMask == null) {
    //        return null;
    //    }
    //    final String[] ipParts = ip.split("\\.");
    //    final String[] netMaskParts = netMask.split("\\.");
    //    if (ipParts.length != 4 && netMaskParts.length != 4) {
    //        return "";
    //    }
    //    final String[] networkIpParts = new String[4];
    //    for (int i = 0; i < 4; i++) {
    //        if (netMaskParts[i].equals("255")) {
    //            networkIpParts[i] = ipParts[i];
    //        } else {
    //            networkIpParts[i] = "*";
    //        }
    //    }
    //    return Tools.join(".", networkIpParts);
    //}

    /**
     * Returns first ip in the network.
     * e.g. 192.168.1.1 and mask 255.255.255.0 gives 192.168.1.0.
     */
    public String getNetworkIp() {
        return networkIp;
    }

    /** Returns value for parameter. */
    @Override
    public Value getValue(final String parameter) {
        if ("ip".equals(parameter)) {
            return new StringValue(ip);
        }
        if ("String".equals(parameter)) {
            return new StringValue(ip);
        } else {
            LOG.appError("getValue: Unknown parameter: " + parameter, "");
            return null;
        }
    }

    /** Returns bindnetaddr. */
    public String getBindnetaddr() {
        return networkIp;
    }
    ///** Returns bindnetaddr. */
    //public String getBindnetaddr() {
    //    final String[] ipParts = ip.split("\\.");
    //    if (netMask == null) {
    //        return null;
    //    }
    //    final String[] netMaskParts = netMask.split("\\.");
    //    String[] networkIpParts = new String[4];
    //    if (ipParts.length != 4 && netMaskParts.length != 4) {
    //        return "";
    //    }
    //    for (int i = 0; i < 4; i++) {
    //        networkIpParts[i] =
    //                     Integer.toString(Integer.parseInt(ipParts[i])
    //                                      & Integer.parseInt(netMaskParts[i]));
    //    }
    //    return Tools.join(".", networkIpParts);
    //}

    /** Returns whether it is a bridge. */
    public boolean isBridge() {
        return bridge;
    }

    /** Return whether it's a localhost. */
    public boolean isLocalHost() {
        return "lo".equals(getName());
    }

    @Override
    public String getValueForGui() {
        return getName();
    }

    @Override
    public String getValueForConfig() {
        return getName();
    }

    @Override
    public boolean isNothingSelected() {
        return getName() == null;
    }

    @Override
    public Unit getUnit() {
        return null;
    }

    @Override
    public String getValueForConfigWithUnit() {
        return getValueForConfig();
    }

    @Override
    public String getNothingSelected() {
        return NOTHING_SELECTED;
    }
}
