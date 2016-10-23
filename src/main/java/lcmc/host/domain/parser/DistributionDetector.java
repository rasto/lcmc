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

import java.util.Arrays;

import lcmc.common.domain.util.Tools;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lombok.Getter;

public class DistributionDetector {

	private final Host host;
	/** Whether dist info was already logged. */
	private boolean distInfoAlreadyLogged = false;
	private String detectedKernelName = "";
	private String detectedDistVersion = "";
	private String detectedKernelVersion = "";
	private String detectedDist = "";
	private String detectedArch = "";
	@Getter
	private String distributionName = "";
	@Getter
	private String distributionVersion = "";
	@Getter
	private String distributionVersionString = "";
	private String kernelName = "";
	private String kernelVersion = "";
	private String arch = "";

	private static final Logger LOG = LoggerFactory.getLogger(Host.class);

	public DistributionDetector(final Host host) {
		this.host = host;
	}

	/**
	 * Sets distribution info for this host from array of strings.
	 * Array consists of: kernel name, kernel version, arch, os, version
	 * and distribution.
	 */
	@SuppressWarnings("fallthrough")
	public void setDistInfo(final String[] info) {
		if (info == null) {
			LOG.debug("setDistInfo: " + host.getName() + " dist info is null");
			return;
		}
		if (!distInfoAlreadyLogged) {
			for (final String di : info) {
				LOG.debug1("setDistInfo: dist info: " + di);
			}
		}

        /* no breaks in the switch statement are intentional */
		String lsbVersion = null;
		String lsbDist = null;
		switch (info.length) {
			case 9:
				lsbVersion = info[8]; // TODO: not used
			case 8:
				lsbDist = info[7];
			case 7:
				lsbVersion = info[6]; // TODO: not used
			case 6:
				lsbDist = info[5];
			case 5:
				if (lsbDist == null || "linux".equals(lsbDist)) {
					detectedDist = info[4];
				} else {
					detectedDist = lsbDist;
				}
			case 4:
				if (lsbVersion == null) {
					detectedDistVersion = info[3];
				} else {
					detectedDistVersion = info[3] + '/' + lsbVersion;
				}
			case 3:
				detectedKernelVersion = info[2];
			case 2:
				detectedArch = info[1];
			case 1:
				detectedKernelName = info[0];
			case 0:
				break;
			default:
				LOG.appError("setDistInfo: list: ", Arrays.asList(info).toString());
				break;
		}
		distributionName = detectedDist;
		distributionVersion = detectedDistVersion;
		initDistInfo();
		if (!distInfoAlreadyLogged) {
			LOG.debug1("setDistInfo: kernel name: " + detectedKernelName);
			LOG.debug1("setDistInfo: kernel version: " + detectedKernelVersion);
			LOG.debug1("setDistInfo: arch: " + detectedArch);
			LOG.debug1("setDistInfo: dist version: " + detectedDistVersion);
			LOG.debug1("setDistInfo: dist: " + detectedDist);
		}
		distInfoAlreadyLogged = true;
	}

	/** Initializes dist info. Must be called after setDistInfo. */
	void initDistInfo() {
		if (!"Linux".equals(detectedKernelName)) {
			LOG.appWarning("initDistInfo: detected kernel not linux: " + detectedKernelName);
		}
		setKernelName("Linux");

		if (!distributionName.equals(detectedDist)) {
			LOG.appError("initDistInfo: dist: " + distributionName + " does not match " + detectedDist);
		}
		distributionVersionString = Tools.getDistVersionString(distributionName, distributionVersion);
		distributionVersion = Tools.getDistString("distributiondir", detectedDist, distributionVersionString, null);
		setKernelVersion(Tools.getKernelDownloadDir(detectedKernelVersion, getDistributionName(), distributionVersionString, null));
		String arch0 = Tools.getDistString("arch:" + detectedArch, getDistributionName(), distributionVersionString, null);
		if (arch0 == null) {
			arch0 = detectedArch;
		}
		setArch(arch0);
	}

	/** Returns the detected info to show. */
	public String getDetectedInfo() {
		return detectedDist + ' ' + detectedDistVersion;
	}

	/**
	 * Gets distribution name from distribution version. E.g. suse from sles9.
	 * This is used only when the distribution is selected in the pulldown menu,
	 * not when it is detected.
	 * The conversion rules for distributions are defined in DistResource.java,
	 * with 'dist:' prefix.
	 * TODO: remove it?
	 */
	public String getDistFromDistVersion(final String dV) {
        /* remove numbers */
		if ("No Match".equals(dV)) {
			return null;
		}
		LOG.debug1("getDistFromDistVersion:" + dV.replaceFirst("\\d.*", ""));
		return Tools.getDistString("dist:" + dV.replaceFirst("\\d.*", ""), "", "", null);
	}

	void setDistributionName(final String dist) {
		this.distributionName = dist;
	}

	void setDistributionVersion(final String distVersion) {
		this.distributionVersion = distVersion;
		distributionVersionString = Tools.getDistVersionString(distributionName, distVersion);
		distributionName = getDistFromDistVersion(distVersion);
	}

	/** Sets arch, e.g. "i386". */
	public void setArch(final String arch) {
		this.arch = arch;
	}

	/** Sets kernel name, e.g. "linux". */
	void setKernelName(final String kernelName) {
		this.kernelName = kernelName;
	}

	void setKernelVersion(final String kernelVersion) {
		this.kernelVersion = kernelVersion;
	}

	/** Gets kernel name. Normaly "Linux" for this application. */
	public String getKernelName() {
		return kernelName;
	}

	/**
	 * Gets kernel version. Usually some version,
	 * like: "2.6.13.2ws-k7-up-lowmem".
	 */
	public String getKernelVersion() {
		return kernelVersion;
	}

	public String getDetectedKernelVersion() {
		return detectedKernelVersion;
	}

	/** Gets architecture like i686. */
	public String getArch() {
		return arch;
	}

}
