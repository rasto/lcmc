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

import static junitparams.JUnitParamsRunner.$;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lcmc.common.domain.ConvertCmdCallback;

@RunWith(JUnitParamsRunner.class)
public class DistributionDetectorTest {

	@Test
	public void commandShouldBeUndefined() {
		assertEquals("undefined",
					 getDistCommand("undefined",
									null,
									false,
									false));
	}

	@Test
	public void twoCommandsShouldBeUndefined() {
		assertEquals("undefined2;;;undefined3",
					 getDistCommand("undefined2;;;undefined3",
									null,
									false,
									false));
	}

	private ConvertCmdCallback getConvertCallback() {
		return command -> command.replaceAll(lcmc.configs.DistResource.SUDO, "sudo ");
	}

	@Test
	public void commandWithoutBash() {
		assertEquals("sudo /etc/init.d/corosync start",
					 getDistCommand("Corosync.startCorosync",
									getConvertCallback(),
									false,
									false));
	}

	@Test
	public void commandWithBash() {
		assertEquals("sudo bash -c \"sudo /etc/init.d/corosync start\"",
					 getDistCommand("Corosync.startCorosync",
									getConvertCallback(),
									true,
									true));
	}

	@Test
	public void multipleCommands() {
		assertEquals("sudo /etc/init.d/corosync start" + ";;;sudo /etc/init.d/corosync start",
					 getDistCommand("Corosync.startCorosync;;;"
											+ "Corosync.startCorosync",
									getConvertCallback(),
									false,
									false));
	}

	@Test
	public void undefinedAndCommandShouldBeConverted() {
		assertEquals("undefined4" + ";;;sudo /etc/init.d/corosync start",
					 getDistCommand("undefined4;;;"
											+ "Corosync.startCorosync",
									getConvertCallback(),
									false,
									false));
	}

	@Test
	public void nullCommandShouldReturnNull() {
		assertNull("null command",
				   getDistCommand(null, getConvertCallback(), false, false));
	}

	@Test
	public void nullCommandInBashShouldReturnNull() {
		assertNull(getDistCommand(null, getConvertCallback(), true, true));
	}

	@Test
	public void nullCommandWithNullDistShouldReturnNull() {
		assertNull(getDistCommand(null, null, true, true));
	}

	private String getDistCommand(final String text,
								  final ConvertCmdCallback convertCmdCallback,
								  final boolean inBash,
								  final boolean inSudo) {
		final DistributionDetector distributionDetector = new DistributionDetector(null);
		distributionDetector.detect(ImmutableList.of("Linux",
													 "x86_64",
													 "3.16.0-4-amd64",
													 "8.6",
													 "debian",
													 "debian",
													 "8.6"));

		return distributionDetector.getDistCommand(text, convertCmdCallback, inBash, inSudo);
	}

	@SuppressWarnings("unused")
	private Object[] parametersForDistStringShouldBeReturned() {
		return $(
				$(null, "none", "none", "none", "none"),
				$("no", "Support", "none", "none", "none"),
				$("no", "Support", "", "", ""),
				$("debian", "Support", "debian", "", ""),
				$("debian-SQUEEZE", "Support", "debian", "6", "x86_64"),
				$("debian-SQUEEZE", "Support", "debian", "6", "a"),
				$("i586", "PmInst.install", "suse", "", "i386")
		);
	}

	@Test
	@Parameters(method = "parametersForDistStringShouldBeReturned")
	public void distStringShouldBeReturned(final String distString,
										   final String text,
										   final String dist,
										   final String version,
										   final String arch) {

		final DistributionDetector distributionDetector = new DistributionDetector(null);
		distributionDetector.detect(ImmutableList.of("Linux", arch, "3.16", version, dist));
		assertThat(distributionDetector.getDistString(text), is(distString));
	}

	@SuppressWarnings("unused")
	private Object[] parametersForDownloadDirShouldBeReturned() {
		return $(
				$("", "", "", "", ""),
				$("2.6.32-28", "2.6.32-28-server", "ubuntu", "squeeze/sid/10.04", "x86_64"),
				$("2.6.32-28", "2.6.32-28-server", "ubuntu", "squeeze/sid/10.04", "i386"),
				$("2.6.24-28", "2.6.24-28-server", "ubuntu", "lenny/sid/8.04", "x86_64"),
				$("2.6.32.27-0.2",
				  "2.6.32.27-0.2-default",
				  "suse",
				  "SLES11",
				  "x86_64"),
				$("2.6.16.60-0.60.1",
				  "2.6.16.60-0.60.1-default",
				  "suse",
				  "SLES10",
				  "x86_64"),
				$("2.6.18-194.8.1.el5",
				  "2.6.18-194.8.1.el5",
				  "redhatenterpriseserver",
				  "5",
				  "x86_64"),
				$("2.6.32-71.18.1.el6.x86_64",
				  "2.6.32-71.18.1.el6.x86_64",
				  "redhatenterpriseserver",
				  "6",
				  "x86_64"),
				$("2.6.26-2", "2.6.26-2-amd64", "debian", "5", "x86_64"),
				$("2.6.32-5", "2.6.32-5-amd64", "debian", "6", "x86_64"),
				$("2.6.32-5", "2.6.32-5-amd64", "debian", "", "x86_64"),
				$("2.6.32-5-amd64",
				  "2.6.32-5-amd64",
				  "unknown",
				  "unknown",
				  "x86_64"),
				$("", "", "unknown", "unknown", "x86_64"),
				$("2.6.32-5-amd64", "2.6.32-5-amd64", "", "", "x86_64")
		);
	}

	@Test
	@Parameters(method = "parametersForDownloadDirShouldBeReturned")
	public void downloadDirShouldBeReturned(final String kernelDir,
											final String kernelVersion,
											final String dist,
											final String version,
											final String arch) {
		final DistributionDetector distributionDetector = new DistributionDetector(null);

		distributionDetector.detect(ImmutableList.of("Linux", arch, "3.16", version, dist));

		assertEquals(kernelDir, distributionDetector.getKernelDownloadDir(kernelVersion));
	}

	@SuppressWarnings("unused")
	private Object[] parametersForDistVersionShouldBeReturned() {
		return $(
				$("LENNY", "debian", "5.0.8"),
				$("SQUEEZE", "debian", "6.0"),
				$("12", "fedora", "Fedora release 12 (Constantine)"),
				$("13", "fedora", "Fedora release 13 (Goddard)"),
				$("14", "fedora", "Fedora release 14 (Laughlin)"),
				$("5", "redhat", "CentOS release 5.5 (Final)"),
				$("5", "redhat", "CentOS release 5.5 (Final)"),
				$("6",
				  "redhatenterpriseserver",
				  "Red Hat Enterprise Linux Server release 6.0 (Santiago)"),
				$("5",
				  "redhatenterpriseserver",
				  "Red Hat Enterprise Linux Server release 5.5 (Tikanga)"),
			/* maverick */
				$("squeeze/sid/10.10", "ubuntu", "squeeze/sid/10.10"),
				$("KARMIC", "ubuntu", "squeeze/sid/9.10"),
				$("LUCID", "ubuntu", "squeeze/sid/10.04"),
				$("HARDY", "ubuntu", "lenny/sid/8.04"),
				$("SLES10", "suse", "SUSE Linux Enterprise Server 10 (x86_64)"),
				$("SLES11", "suse", "SUSE Linux Enterprise Server 11 (x86_64)"),
				$("OPENSUSE11_2", "suse", "openSUSE 11.2 (x86_64)"),
				$("OPENSUSE11_3", "suse", "openSUSE 11.3 (x86_64)"),
				$("OPENSUSE11_4", "suse", "openSUSE 11.4 (x86_64)"),
				$("2", "openfiler", "Openfiler NSA 2.3")
		);
	}

	@Test
	@Parameters(method = "parametersForDistVersionShouldBeReturned")
	public void distVersionShouldBeReturned(final String distVersion, final String dist, final String version) {
		final DistributionDetector distributionDetector = new DistributionDetector(null);
		distributionDetector.detect(ImmutableList.of("Linux", "", "3.16", version, dist));

		assertThat(distributionDetector.getDistVersionString(version), is(distVersion));
	}
}