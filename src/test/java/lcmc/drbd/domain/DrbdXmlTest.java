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

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.host.domain.Host;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class DrbdXmlTest {

    private static final String OPTION1 = "OPTION1";
    private static final String OPTION2 = "OPTION2";
    private static final String OPTION3 = "OPTION3";
    private static final String OPTION4 = "OPTION4";
    private static final String OPTION5 = "OPTION5";

    private DrbdXml drbdXml;
    private String output;
    @Mock
    private Host host1;
    @Mock
    private Host host2;

    @Before
    public void setUp() {
        drbdXml = new DrbdXml();
        output = readFile("DrbdCommands.txt");

        given(host1.getName()).willReturn("HOST1");
        given(host2.getName()).willReturn("HOST2");

        given(host1.getArch()).willReturn("ANY_ARCH");
        given(host1.getHeartbeatLibPath()).willReturn("hbpath");
        given(host1.getAvailableCryptoModules()).willReturn(Sets.newHashSet("CRYPTO"));

        drbdXml.parseDrbdParameters(host1, output, new Host[]{host1, host2});
    }

    @Test
    public void shouldParseNumericOption() {

        val option = OPTION1;

        assertThat(drbdXml.getParamType(option), is("numeric"));
        assertThat(drbdXml.getParamDefault(option).getValueForConfigWithUnit(), is("16"));
        assertThat(drbdXml.getParamDefault(option).getValueForGui(), is("16"));
        assertThat(drbdXml.getParamDefault(option).getValueForConfig(), is("16"));
        assertThat(drbdXml.getParamShortDesc(option), is("OPTION1 (bytes)"));
        assertThat(drbdXml.getParamLongDesc(option), is("DESC1"));
        assertThat(drbdXml.getDefaultUnit(option), is("K"));
        assertThat(drbdXml.getPossibleChoices(option), nullValue());
        assertThat(drbdXml.getSection(option), is("COMMAND"));
        assertThat(drbdXml.checkParam(option, new StringValue("1023")), is(false));
        assertThat(drbdXml.checkParam(option, new StringValue("1024")), is(true));
        assertThat(drbdXml.checkParam(option, new StringValue("10240")), is(true));
        assertThat(drbdXml.checkParam(option, new StringValue("10241")), is(false));
        assertThat(drbdXml.getAccessType(option), is(AccessMode.ADMIN));
    }

    @Test
    public void shouldParseBooleanOption() {
        val option = OPTION2;

        assertThat(drbdXml.getParamType(option), is("boolean"));
        assertThat(drbdXml.getParamDefault(option).getValueForConfigWithUnit(), is("no"));
        assertThat(drbdXml.getParamDefault(option).getValueForGui(), is("no"));
        assertThat(drbdXml.getParamDefault(option).getValueForConfig(), is("no"));
        assertThat(drbdXml.getParamShortDesc(option), is("OPTION2"));
        assertThat(drbdXml.getParamLongDesc(option), is("DESC2"));
        assertThat(drbdXml.getDefaultUnit(option), nullValue());
        assertThat(drbdXml.getPossibleChoices(option), is(new Value[]{new StringValue("yes"), new StringValue("no")}));
        assertThat(drbdXml.getSection(option), is("COMMAND"));
        assertThat(drbdXml.getAccessType(option), is(AccessMode.ADMIN));
    }

    @Test
    public void shouldParseStringOption() {
        val option = OPTION3;

        assertThat(drbdXml.getParamType(option), is("string"));
        assertThat(drbdXml.getParamDefault(option), nullValue());
        assertThat(drbdXml.getParamShortDesc(option), is("OPTION3"));
        assertThat(drbdXml.getParamLongDesc(option), is("DESC3"));
        assertThat(drbdXml.getDefaultUnit(option), nullValue());
        assertThat(drbdXml.getPossibleChoices(option), nullValue());
        assertThat(drbdXml.getSection(option), is("COMMAND"));
        assertThat(drbdXml.getAccessType(option), is(AccessMode.ADMIN));
    }

    @Test
    public void shouldParseHandlerOption() {
        val option = OPTION4;

        assertThat(drbdXml.getParamType(option), is("handler"));
        assertThat(drbdXml.getParamDefault(option), nullValue());
        assertThat(drbdXml.getParamShortDesc(option), is("OPTION4"));
        assertThat(drbdXml.getParamLongDesc(option), is("DESC4"));
        assertThat(drbdXml.getDefaultUnit(option), nullValue());
        assertThat(drbdXml.getPossibleChoices(option), is(new Value[]{new StringValue(), new StringValue("HANDLER1"), new StringValue("HANDLER2")}));
        assertThat(drbdXml.getSection(option), is("COMMAND"));
        assertThat(drbdXml.getAccessType(option), is(AccessMode.ADMIN));
    }

    @Test
    public void shouldParseFencePeerOption() {
        val option = "fence-peer";

        assertThat(drbdXml.getPossibleChoices(option), is(new Value[]{
                new StringValue(),
                new StringValue("hbpath/drbd-peer-outdater -t 5"),
                new StringValue("/usr/lib/drbd/crm-fence-peer.sh")
        }));
    }

    @Test
    public void shouldParseAfterResyncTarget() {
        val option = "after-resync-target";

        assertThat(drbdXml.getPossibleChoices(option), is(new Value[]{
                new StringValue(),
                new StringValue("/usr/lib/drbd/crm-unfence-peer.sh")
        }));
    }

    @Test
    public void shouldParseSplitBrain() {
        val option = "split-brain";

        assertThat(drbdXml.getPossibleChoices(option), is(new Value[]{
                new StringValue(),
                new StringValue("/usr/lib/drbd/notify-split-brain.sh root")
        }));
    }

    @Test
    public void shouldParseBecomePrimaryOn() {
        val option = "become-primary-on";

        assertThat(drbdXml.getPossibleChoices(option), is(new Value[]{
                new StringValue(),
                new StringValue("both"),
                new StringValue("HOST1"),
                new StringValue("HOST2")
        }));
    }

    @Test
    public void shouldParseVerifyAlg() {
        val option = "verify-alg";

        assertThat(drbdXml.getPossibleChoices(option), is(new Value[]{
                new StringValue(),
                new StringValue("CRYPTO")
        }));
    }


    @Test
    public void flatOptionShouldBeIgnored() {
        val option = OPTION5;

        assertThat(drbdXml.getParamType(option), nullValue());
        assertThat(drbdXml.getSection(option), nullValue());
    }

    @Test
    public void parameterListsShouldBePopulated() {
        assertThat(newArrayList(drbdXml.getParameters()), is(asList(OPTION1, OPTION2, OPTION3, OPTION4, "fence-peer", "after-resync-target", "split-brain", "become-primary-on", "verify-alg",  "protocol")));
        assertThat(newArrayList(drbdXml.getGlobalParams()), is(asList(OPTION1, OPTION2, OPTION3, OPTION4, "fence-peer", "after-resync-target", "split-brain", "become-primary-on", "verify-alg")));
        assertThat(newArrayList(drbdXml.getSections()), is(asList("COMMAND", "resource")));
        assertThat(newArrayList(drbdXml.getSectionParams("COMMAND")), is(asList(OPTION1, OPTION2, OPTION3, OPTION4, "fence-peer", "after-resync-target", "split-brain", "become-primary-on", "verify-alg")));
    }

    private String readFile(final String resourceName) {
        try {
            return Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

