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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import lcmc.common.domain.ConvertCmdCallback;

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

}