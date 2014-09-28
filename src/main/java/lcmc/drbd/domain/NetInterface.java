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


package lcmc.drbd.domain;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.Resource;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Unit;

/**
 * This class holds data of one network interface.
 */
public final class NetInterface extends Resource implements Value {
    private static final Logger LOG = LoggerFactory.getLogger(NetInterface.class);

    private static String getSymbolicIp(BigInteger numericIp, final int size) {
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
    private final String ip;
    private final Integer cidr;
    private final String networkIp;
    private final boolean bridge;
    private final AddressFamily addressFamily;
    private final String IPV6_STRING = "ipv6";
    private final String IPV4_STRING = "ipv4";
    private final String SSOCKS_STRING = "ssocks";
    private final String SDP_STRING = "sdp";

    /**
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
        AddressFamily addressFamily0 = null;
        int size = 4;
        if (cols.length > 2) {
            final String af_string = cols[1];
            ip0 = cols[2];
            if (IPV6_STRING.equals(af_string)) {
                addressFamily0 = AddressFamily.IPV6;
                size = 16;
            }else if (IPV4_STRING.equals(af_string)) {
                addressFamily0 = AddressFamily.IPV4;
            } else if (SSOCKS_STRING.equals(af_string)) {
                addressFamily0 = AddressFamily.SSOCKS;
            } else if (SDP_STRING.equals(af_string)) {
                addressFamily0 = AddressFamily.SDP;
            } else {
                LOG.debug1("NetInterface: af_string: " + af_string + "-> ipv4");
                addressFamily0 = AddressFamily.IPV4;
            }
        }
        this.ip = ip0;
        this.addressFamily = addressFamily0;
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

    public NetInterface(final String iface,
                        final String ip,
                        final Integer cidr,
                        final boolean bridge,
                        final AddressFamily addressFamily) throws UnknownHostException {
        super(iface);
        this.ip = ip;
        this.cidr = cidr;
        this.bridge = bridge;
        this.addressFamily = addressFamily;
        int size = 4;
        if (addressFamily == AddressFamily.IPV6) {
            size = 16;
        }
        this.networkIp = calcNetworkIp(getNumericIp(ip), cidr, size);
    }

    private String calcNetworkIp(final BigInteger numericIp, final Integer cidr, final int size) {
        return getSymbolicIp(numericIp.and(new BigInteger("2").pow(8 * size)
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

    public String getIp() {
        return ip;
    }

    public Integer getCidr() {
        return cidr;
    }

    // * Returns network ip. The ip has '*' instead of bytes, that are
    public String getNetworkIp() {
        return networkIp;
    }

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

    public String getBindnetaddr() {
        return networkIp;
    }

    public boolean isBridge() {
        return bridge;
    }

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

    public enum AddressFamily {IPV4, IPV6, SSOCKS, SDP}
}
