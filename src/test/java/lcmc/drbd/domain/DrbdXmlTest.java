/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2016, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.drbd.domain;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import lcmc.common.domain.AccessMode;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.drbd.ui.DrbdGraph;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.host.domain.Host;
import lombok.val;

@ExtendWith(MockitoExtension.class)
class DrbdXmlTest {

    private static final String OPTION1 = "OPTION1";
    private static final String OPTION2 = "OPTION2";
    private static final String OPTION3 = "OPTION3";
    private static final String OPTION4 = "OPTION4";
    private static final String OPTION5 = "OPTION5";

    private DrbdXml drbdXml;
    @Mock
    private Host host1;
    @Mock
    private Host host2;
    @Mock
    private DrbdGraph drbdGraph;
    @Mock
    private BlockDevInfo blockDevInfo;

    @BeforeEach
    void setUp() {
        drbdXml = new DrbdXml(null, null);
        val output = readFile("DrbdCommands.txt");

        given(host1.getName()).willReturn("HOST1");
        given(host2.getName()).willReturn("HOST2");

        given(host1.getArch()).willReturn("ANY_ARCH");
        given(host1.getHeartbeatLibPath()).willReturn("hbpath");
        given(host1.getAvailableCryptoModules()).willReturn(Sets.newHashSet("CRYPTO"));

        drbdXml.parseDrbdParameters(host1, output, new Host[]{host1, host2});
    }

    @Test
    void shouldParseNumericOption() {

        val option = OPTION1;

        assertThat(drbdXml.getParamType(option)).isEqualTo("numeric");
        assertThat(drbdXml.getParamDefault(option).getValueForConfigWithUnit()).isEqualTo("16");
        assertThat(drbdXml.getParamDefault(option).getValueForGui()).isEqualTo("16");
        assertThat(drbdXml.getParamDefault(option).getValueForConfig()).isEqualTo("16");
        assertThat(drbdXml.getParamShortDesc(option)).isEqualTo("OPTION1 (bytes)");
        assertThat(drbdXml.getParamLongDesc(option)).isEqualTo("DESC1");
        assertThat(drbdXml.getDefaultUnit(option)).isEqualTo("K");
        assertThat(drbdXml.getPossibleChoices(option)).isNull();
        assertThat(drbdXml.getSection(option)).isEqualTo("COMMAND");
        assertThat(drbdXml.checkParam(option, new StringValue("1023"))).isEqualTo(false);
        assertThat(drbdXml.checkParam(option, new StringValue("1024"))).isEqualTo(true);
        assertThat(drbdXml.checkParam(option, new StringValue("10240"))).isEqualTo(true);
        assertThat(drbdXml.checkParam(option, new StringValue("10241"))).isEqualTo(false);
        assertThat(drbdXml.getAccessType(option)).isEqualTo(AccessMode.ADMIN);
    }

    @Test
    void shouldParseBooleanOption() {
        val option = OPTION2;

        assertThat(drbdXml.getParamType(option)).isEqualTo("boolean");
        assertThat(drbdXml.getParamDefault(option).getValueForConfigWithUnit()).isEqualTo("no");
        assertThat(drbdXml.getParamDefault(option).getValueForGui()).isEqualTo("no");
        assertThat(drbdXml.getParamDefault(option).getValueForConfig()).isEqualTo("no");
        assertThat(drbdXml.getParamShortDesc(option)).isEqualTo("OPTION2");
        assertThat(drbdXml.getParamLongDesc(option)).isEqualTo("DESC2");
        assertThat(drbdXml.getDefaultUnit(option)).isNull();
        assertThat(drbdXml.getPossibleChoices(option)).isEqualTo(new Value[]{new StringValue("yes"), new StringValue("no")});
        assertThat(drbdXml.getSection(option)).isEqualTo("COMMAND");
        assertThat(drbdXml.getAccessType(option)).isEqualTo(AccessMode.ADMIN);
    }

    @Test
    void shouldParseStringOption() {
        val option = OPTION3;

        assertThat(drbdXml.getParamType(option)).isEqualTo("string");
        assertThat(drbdXml.getParamDefault(option)).isNull();
        assertThat(drbdXml.getParamShortDesc(option)).isEqualTo("OPTION3");
        assertThat(drbdXml.getParamLongDesc(option)).isEqualTo("DESC3");
        assertThat(drbdXml.getDefaultUnit(option)).isNull();
        assertThat(drbdXml.getPossibleChoices(option)).isNull();
        assertThat(drbdXml.getSection(option)).isEqualTo("COMMAND");
        assertThat(drbdXml.getAccessType(option)).isEqualTo(AccessMode.ADMIN);
    }

    @Test
    void shouldParseHandlerOption() {
        val option = OPTION4;

        assertThat(drbdXml.getParamType(option)).isEqualTo("handler");
        assertThat(drbdXml.getParamDefault(option)).isNull();
        assertThat(drbdXml.getParamShortDesc(option)).isEqualTo("OPTION4");
        assertThat(drbdXml.getParamLongDesc(option)).isEqualTo("DESC4");
        assertThat(drbdXml.getDefaultUnit(option)).isNull();
        assertThat(drbdXml.getPossibleChoices(option)).isEqualTo(
                new Value[]{new StringValue(), new StringValue("HANDLER1"), new StringValue("HANDLER2")});
        assertThat(drbdXml.getSection(option)).isEqualTo("COMMAND");
        assertThat(drbdXml.getAccessType(option)).isEqualTo(AccessMode.ADMIN);
    }

    @Test
    void shouldParseFencePeerOption() {
        val option = "fence-peer";

        assertThat(drbdXml.getPossibleChoices(option)).isEqualTo(
                new Value[]{new StringValue(), new StringValue("hbpath/drbd-peer-outdater -t 5"),
                        new StringValue("/usr/lib/drbd/crm-fence-peer.sh")});
    }

    @Test
    void shouldParseAfterResyncTarget() {
        val option = "after-resync-target";

        assertThat(drbdXml.getPossibleChoices(option)).isEqualTo(
                new Value[]{new StringValue(), new StringValue("/usr/lib/drbd/crm-unfence-peer.sh")});
    }

    @Test
    void shouldParseSplitBrain() {
        val option = "split-brain";

        assertThat(drbdXml.getPossibleChoices(option)).isEqualTo(
                new Value[]{new StringValue(), new StringValue("/usr/lib/drbd/notify-split-brain.sh root")});
    }

    @Test
    void shouldParseBecomePrimaryOn() {
        val option = "become-primary-on";

        assertThat(drbdXml.getPossibleChoices(option)).isEqualTo(
                new Value[]{new StringValue(), new StringValue("both"), new StringValue("HOST1"), new StringValue("HOST2")});
    }

    @Test
    void shouldParseVerifyAlg() {
        val option = "verify-alg";

        assertThat(drbdXml.getPossibleChoices(option)).isEqualTo(new Value[]{new StringValue(), new StringValue("CRYPTO")});
    }


    @Test
    void flatOptionShouldBeIgnored() {
        val option = OPTION5;

        assertThat(drbdXml.getParamType(option)).isNull();
        assertThat(drbdXml.getSection(option)).isNull();
    }

    @Test
    void parameterListsShouldBePopulated() {
        assertThat(newArrayList(drbdXml.getParameters())).isEqualTo(
                asList(OPTION1, OPTION2, OPTION3, OPTION4, "fence-peer", "after-resync-target", "split-brain", "become-primary-on",
                        "verify-alg", "protocol"));
        assertThat(newArrayList(drbdXml.getGlobalParams())).isEqualTo(
                asList(OPTION1, OPTION2, OPTION3, OPTION4, "fence-peer", "after-resync-target", "split-brain", "become-primary-on",
                        "verify-alg"));
        assertThat(newArrayList(drbdXml.getSections())).isEqualTo(asList("COMMAND", "resource"));
        assertThat(newArrayList(drbdXml.getSectionParams("COMMAND"))).isEqualTo(
                asList(OPTION1, OPTION2, OPTION3, OPTION4, "fence-peer", "after-resync-target", "split-brain", "become-primary-on",
                        "verify-alg"));
    }

    @Test
    void shouldParseDrbdEvent() {
        drbdXml.addDeviceAddResource("/dev/drbd0", "r0");
        drbdXml.addHostDiskMap("r0", "0", ImmutableMap.of("host1", "/dev/sda1", "host2", "/dev/sda1"));
        val blockDevice = new BlockDevice(host1, "/dev/sda1");
        when(blockDevInfo.getBlockDevice()).thenReturn(blockDevice);
        when(drbdGraph.findBlockDevInfo("host1", "/dev/sda1")).thenReturn(blockDevInfo);

        boolean ret = drbdXml.parseDrbdEvent("host1", drbdGraph, "40 ST 0,r0[0] { cs:WFReportParams ro:Secondary/Unknown ds:Attaching/DUnknown r--- }");

        assertThat(blockDevice.isSyncing()).isFalse();
        assertThat(blockDevice.isPrimary()).isFalse();
        assertThat(blockDevice.isSecondary()).isTrue();
        assertThat(blockDevice.isAttached()).isTrue();
        assertThat(blockDevice.isAvailable()).isTrue();
        assertThat(blockDevice.isConnected()).isFalse();
        assertThat(blockDevice.isWFConnection()).isFalse();
        assertThat(blockDevice.isConnectedOrWF()).isFalse();
        assertThat(blockDevice.getNodeState()).isEqualTo("Secondary");
        assertThat(blockDevice.getConnectionState()).isEqualTo("WFReportParams");
        assertThat(blockDevice.getDiskState()).isEqualTo("Attaching");
        assertThat(blockDevice.getDiskStateOther()).isEqualTo("DUnknown");
        assertThat(ret).isTrue();
    }

    private String readFile(final String resourceName) {
        try {
            return Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

