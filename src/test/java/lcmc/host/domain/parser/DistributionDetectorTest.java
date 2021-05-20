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

package lcmc.host.domain.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableList;

import lcmc.common.domain.ConvertCmdCallback;

@ExtendWith(MockitoExtension.class)
class DistributionDetectorTest {

    @Test
    void commandShouldBeUndefined() {
        assertThat((Object) getDistCommand("undefined", null, false, false)).isEqualTo("undefined");
    }

    @Test
    void twoCommandsShouldBeUndefined() {
        assertThat((Object) getDistCommand("undefined2;;;undefined3", null, false, false)).isEqualTo("undefined2;;;undefined3");
    }

    private ConvertCmdCallback getConvertCallback() {
        return command -> command.replaceAll(lcmc.configs.DistResource.SUDO, "sudo ");
    }

    @Test
    void commandWithoutBash() {
        assertThat((Object) getDistCommand("Corosync.startCorosync", getConvertCallback(), false, false)).isEqualTo(
                "sudo /etc/init.d/corosync start");
    }

    @Test
    void commandWithBash() {
        assertThat((Object) getDistCommand("Corosync.startCorosync", getConvertCallback(), true, true)).isEqualTo(
                "sudo bash -c \"sudo /etc/init.d/corosync start\"");
    }

    @Test
    void multipleCommands() {
        assertThat(
                (Object) getDistCommand("Corosync.startCorosync;;;" + "Corosync.startCorosync", getConvertCallback(), false, false))
                .isEqualTo("sudo /etc/init.d/corosync start" + ";;;sudo /etc/init.d/corosync start");
    }

    @Test
    void undefinedAndCommandShouldBeConverted() {
        assertThat(
                (Object) getDistCommand("undefined4;;;" + "Corosync.startCorosync", getConvertCallback(), false, false)).isEqualTo(
                "undefined4" + ";;;sudo /etc/init.d/corosync start");
    }

    @Test
    void nullCommandShouldReturnNull() {
        assertThat(getDistCommand(null, getConvertCallback(), false, false)).describedAs("null command").isNull();
    }

    @Test
    void nullCommandInBashShouldReturnNull() {
        assertThat(getDistCommand(null, getConvertCallback(), true, true)).isNull();
    }

    @Test
    void nullCommandWithNullDistShouldReturnNull() {
        assertThat(getDistCommand(null, null, true, true)).isNull();
    }

    private String getDistCommand(final String text, final ConvertCmdCallback convertCmdCallback, final boolean inBash,
            final boolean inSudo) {
        final DistributionDetector distributionDetector = new DistributionDetector(null);
        distributionDetector.detect(ImmutableList.of("Linux", "x86_64", "3.16.0-4-amd64", "8.6", "debian", "debian", "8.6"));

        return distributionDetector.getDistCommand(text, convertCmdCallback, inBash, inSudo);
    }

    private static Stream<Arguments> parametersForDistributionShouldBeDetected() {
        return Stream.of(Arguments.of("ubuntu",
                ImmutableList.of("Linux", "x86_64", "3.2.0-79-generic", "wheezy/sid", "debian", "ubuntu", "12.04")),
                Arguments.of("ubuntu",
                        ImmutableList.of("Linux", "x86_64", "3.13.0-88-generic", "jessie/sid", "debian", "ubuntu", "14.04")),
                Arguments.of("ubuntu",
                        ImmutableList.of("Linux", "x86_64", "4.4.0-46-generic", "stretch/sid", "debian", "ubuntu", "16.04")),
                Arguments.of("ubuntu",
                        ImmutableList.of("Linux", "x86_64", "4.8.0-26-generic", "stretch/sid", "debian", "ubuntu", "16.10")),
                Arguments.of("ubuntu",
                        ImmutableList.of("Linux", "x86_64", "4.4.0-42-generic", "stretch/sid", "debian", "ubuntu", "16.04")),

                Arguments.of("debian", ImmutableList.of("Linux", "x86_64", "3.2.0-4-amd64", "7.10", "debian", "debian", "7.10")),
                Arguments.of("debian-JESSIE",
                        ImmutableList.of("Linux", "x86_64", "3.16.0-4-amd64", "8.6", "debian", "debian", "8.6")),

                Arguments.of("redhat-5",
                        ImmutableList.of("Linux", "x87_64", "2.6.18-409.el5", "CentOS release 5.11 (Final)", "redhat", "redhat",
                                "5.11")), Arguments.of("redhat-6",
                        ImmutableList.of("Linux", "x86_64", "2.6.32-504.12.2.el6.x86_64", "CentOS release 6.6 (Final)", "redhat",
                                "redhat", "6.6", "linux", "6")), Arguments.of("redhat-6",
                        ImmutableList.of("Linux", "x86_64", "2.6.39-400.212.1.el6uek.x86_64",
                                "Red Hat Enterprise Linux Server release 6.5 (Santiago)", "redhat", "redhat", "6.5", "oracle_linux",
                                "6server")), Arguments.of("redhat-7",
                        ImmutableList.of("Linux", "x86_64", "3.10.0-327.36.2.el7.x86_64", "CentOS Linux release 7.2.1511 (Core) ",
                                "redhat", "redhat", "7")), Arguments.of("fedora",
                        ImmutableList.of("Linux", "x86_64", "4.5.7-200.fc23.x86_64", "Fedora release 23 (Twenty Three)", "redhat",
                                "fedora", "23")), Arguments.of("fedora",
                        ImmutableList.of("Linux", "x86_64", "4.7.7-200.fc24.x86_64", "Fedora release 24 (Twenty Four)", "redhat",
                                "fedora", "24")), Arguments.of("redhatenterpriseserver-6",
                        ImmutableList.of("Linux", "x86_64", "2.6.32-131.6.1.el6.x86_64",
                                "Red Hat Enterprise Linux Server release 6.1 (Santiago)", "redhat", "redhatenterpriseserver",
                                "6server")),

                Arguments.of("suse-OPENSUSE13_1",
                        ImmutableList.of("Linux", "x86_64", "3.11.10-21-desktop", "openSUSE 13.1 (x86_64)", "SuSE", "suse",
                                "13.1")), Arguments.of("suse-OPENSUSE13_1",
                        ImmutableList.of("Linux", "x86_64", "3.16.6-2-desktop", "openSUSE 13.2 (x86_64)", "SuSE", "suse", "13.2")),
                Arguments.of("suse-OPENSUSE13_1",
                        ImmutableList.of("Linux", "x86_64", "4.1.34-33-default", "openSUSE 42.1 (x86_64)", "SuSE", "suse",
                                "42.1")));
    }

    @MethodSource("parametersForDistributionShouldBeDetected")
    @ParameterizedTest
    void distributionShouldBeDetected(final String distString, final List<String> lines) {

        final DistributionDetector distributionDetector = new DistributionDetector(null);
        distributionDetector.detect(lines);
        assertThat(distributionDetector.getDistString("Support")).isEqualTo(distString);
    }

    @ParameterizedTest
    @CsvSource({", none, none, none, none", //
            "no, Support, none, none, none", //
            "no, Support, '', '', ''", //
            "debian, Support, debian, '', ''", //
            "debian-SQUEEZE, Support, debian, 6, x86_64", //
            "debian-SQUEEZE, Support, debian, 6, a", //
            "i586, PmInst.install, suse, '', i386"})
    void distStringShouldBeReturned(String distString, final String text, final String dist, final String version,
            final String arch) {
        final DistributionDetector distributionDetector = new DistributionDetector(null);
        distributionDetector.detect(ImmutableList.of("Linux", arch, "3.16", version, dist));
        assertThat(distributionDetector.getDistString(text)).isEqualTo(distString);
    }

    private static Stream<Arguments> parametersForDownloadDirShouldBeReturned() {
        return Stream.of(Arguments.of("", "", "", "", ""),
                Arguments.of("2.6.32-28", "2.6.32-28-server", "ubuntu", "squeeze/sid/10.04", "x86_64"),
                Arguments.of("2.6.32-28", "2.6.32-28-server", "ubuntu", "squeeze/sid/10.04", "i386"),
                Arguments.of("2.6.24-28", "2.6.24-28-server", "ubuntu", "lenny/sid/8.04", "x86_64"),
                Arguments.of("2.6.32.27-0.2", "2.6.32.27-0.2-default", "suse", "SLES11", "x86_64"),
                Arguments.of("2.6.16.60-0.60.1", "2.6.16.60-0.60.1-default", "suse", "SLES10", "x86_64"),
                Arguments.of("2.6.18-194.8.1.el5", "2.6.18-194.8.1.el5", "redhatenterpriseserver", "5", "x86_64"),
                Arguments.of("2.6.32-71.18.1.el6.x86_64", "2.6.32-71.18.1.el6.x86_64", "redhatenterpriseserver", "6", "x86_64"),
                Arguments.of("2.6.26-2", "2.6.26-2-amd64", "debian", "5", "x86_64"),
                Arguments.of("2.6.32-5", "2.6.32-5-amd64", "debian", "6", "x86_64"),
                Arguments.of("2.6.32-5", "2.6.32-5-amd64", "debian", "", "x86_64"),
                Arguments.of("2.6.32-5-amd64", "2.6.32-5-amd64", "unknown", "unknown", "x86_64"),
                Arguments.of("", "", "unknown", "unknown", "x86_64"),
                Arguments.of("2.6.32-5-amd64", "2.6.32-5-amd64", "", "", "x86_64"));
    }

    @ParameterizedTest
    @MethodSource("parametersForDownloadDirShouldBeReturned")
    void downloadDirShouldBeReturned(final String kernelDir, final String kernelVersion, final String dist, final String version,
            final String arch) {
        final DistributionDetector distributionDetector = new DistributionDetector(null);

        distributionDetector.detect(ImmutableList.of("Linux", arch, "3.16", version, dist));

        assertThat(distributionDetector.getKernelDownloadDir(kernelVersion)).isEqualTo(kernelDir);
    }

    private static Stream<Arguments> parametersForDistVersionShouldBeReturned() {
        return Stream.of(Arguments.of("LENNY", "debian", "5.0.8"), Arguments.of("SQUEEZE", "debian", "6.0"),
                Arguments.of("12", "fedora", "Fedora release 12 (Constantine)"),
                Arguments.of("13", "fedora", "Fedora release 13 (Goddard)"),
                Arguments.of("14", "fedora", "Fedora release 14 (Laughlin)"),
                Arguments.of("5", "redhat", "CentOS release 5.5 (Final)"),
                Arguments.of("5", "redhat", "CentOS release 5.5 (Final)"),
                Arguments.of("6", "redhatenterpriseserver", "Red Hat Enterprise Linux Server release 6.0 (Santiago)"),
                Arguments.of("5", "redhatenterpriseserver", "Red Hat Enterprise Linux Server release 5.5 (Tikanga)"),
                /* maverick */
                Arguments.of("squeeze/sid/10.10", "ubuntu", "squeeze/sid/10.10"),
                Arguments.of("KARMIC", "ubuntu", "squeeze/sid/9.10"), Arguments.of("LUCID", "ubuntu", "squeeze/sid/10.04"),
                Arguments.of("HARDY", "ubuntu", "lenny/sid/8.04"),
                Arguments.of("SLES10", "suse", "SUSE Linux Enterprise Server 10 (x86_64)"),
                Arguments.of("SLES11", "suse", "SUSE Linux Enterprise Server 11 (x86_64)"),
                Arguments.of("OPENSUSE11_2", "suse", "openSUSE 11.2 (x86_64)"),
                Arguments.of("OPENSUSE11_3", "suse", "openSUSE 11.3 (x86_64)"),
                Arguments.of("OPENSUSE11_4", "suse", "openSUSE 11.4 (x86_64)"), Arguments.of("2", "openfiler", "Openfiler NSA 2.3"),
                Arguments.of("8", "redhat", "AlmaLinux release 8.4 (Electric Cheetah)"));
    }

    @ParameterizedTest
    @MethodSource("parametersForDistVersionShouldBeReturned")
    void distVersionShouldBeReturned(final String distVersion, final String dist, final String version) {
        final DistributionDetector distributionDetector = new DistributionDetector(null);
        distributionDetector.detect(ImmutableList.of("Linux", "", "3.16", version, dist));

        assertThat(distributionDetector.getDistVersionString(version)).isEqualTo(distVersion);
    }

}
