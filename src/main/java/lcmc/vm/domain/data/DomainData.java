/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2015, Rastislav Levrinc.
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

package lcmc.vm.domain.data;

import java.util.Map;

import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class DomainData {
    @Getter
    private final String domainName;
    private final Map<String, String> parameterValues = Maps.newHashMap();
    @Setter
    @Getter
    private Map<String, DiskData> disksMap = Maps.newLinkedHashMap();
    @Setter
    @Getter
    private Map<String, FilesystemData> filesystemsMap = Maps.newLinkedHashMap();
    @Setter
    @Getter
    private Map<String, InterfaceData> interfacesMap = Maps.newLinkedHashMap();
    @Setter
    @Getter
    private Map<String, InputDevData> inputDevsMap = Maps.newLinkedHashMap();
    @Setter
    @Getter
    private Map<String, GraphicsData> graphicsDevsMap = Maps.newLinkedHashMap();
    @Setter
    @Getter
    private Map<String, SoundData> soundsMap = Maps.newLinkedHashMap();
    @Setter
    @Getter
    private Map<String, SerialData> serialsMap = Maps.newLinkedHashMap();
    @Setter
    @Getter
    private Map<String, ParallelData> parallelsMap = Maps.newLinkedHashMap();
    @Setter
    @Getter
    private Map<String, VideoData> videosMap = Maps.newLinkedHashMap();
    @Setter
    @Getter
    private Integer remotePort = -1;
    @Setter
    @Getter
    private boolean autoport = false;
    @Setter
    @Getter
    private boolean running = false;
    @Setter
    @Getter
    private boolean suspended = false;

    public void setParameter(final String param, final String value) {
        parameterValues.put(param, value);
    }

    public void removeParameter(final String param) {
        parameterValues.remove(param);
    }

    public String getValue(final String param) {
        return parameterValues.get(param);
    }
}
