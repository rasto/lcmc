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

package lcmc.vm.domain;

import java.util.HashMap;
import java.util.Map;

import lcmc.vm.domain.data.DiskData;
import lcmc.vm.domain.data.FilesystemData;
import lcmc.vm.domain.data.GraphicsData;
import lcmc.vm.domain.data.InputDevData;
import lcmc.vm.domain.data.InterfaceData;
import lcmc.vm.domain.data.ParallelData;
import lcmc.vm.domain.data.SerialData;
import lcmc.vm.domain.data.SoundData;
import lcmc.vm.domain.data.VideoData;

public class VMParams {
    public static final String VM_PARAM_NAME = "name";
    public static final String VM_PARAM_EMULATOR = "emulator";
    public static final String VM_PARAM_UUID = "uuid";
    public static final String VM_PARAM_VCPU = "vcpu";
    public static final String VM_PARAM_BOOTLOADER = "bootloader";
    public static final String VM_PARAM_CURRENTMEMORY = "currentMemory";
    public static final String VM_PARAM_MEMORY = "memory";
    public static final String OS_BOOT_NODE = "boot";
    public static final String OS_BOOT_NODE_DEV = "dev";
    public static final String VM_PARAM_BOOT = "boot";
    public static final String VM_PARAM_BOOT_2 = "boot2";
    public static final String VM_PARAM_LOADER = "loader";
    public static final String VM_PARAM_AUTOSTART = "autostart";
    public static final String VM_PARAM_VIRSH_OPTIONS = "virsh-options";
    public static final String VM_PARAM_TYPE = "type";
    public static final String VM_PARAM_INIT = "init";
    public static final String VM_PARAM_TYPE_ARCH = "arch";
    public static final String VM_PARAM_TYPE_MACHINE = "machine";
    public static final String VM_PARAM_ACPI = "acpi";
    public static final String VM_PARAM_APIC = "apic";
    public static final String VM_PARAM_PAE = "pae";
    public static final String VM_PARAM_HAP = "hap";
    public static final String VM_PARAM_CLOCK_OFFSET = "offset";
    public static final String VM_PARAM_CPU_MATCH = "match";
    public static final String VM_PARAM_CPUMATCH_MODEL = "model";
    public static final String VM_PARAM_CPUMATCH_VENDOR = "vendor";
    public static final String VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS = "sockets";
    public static final String VM_PARAM_CPUMATCH_TOPOLOGY_CORES = "cores";
    public static final String VM_PARAM_CPUMATCH_TOPOLOGY_THREADS = "threads";
    public static final String VM_PARAM_CPUMATCH_FEATURE_POLICY = "policy";
    public static final String VM_PARAM_CPUMATCH_FEATURES = "features";
    public static final String VM_PARAM_ON_POWEROFF = "on_poweroff";
    public static final String VM_PARAM_ON_REBOOT = "on_reboot";
    public static final String VM_PARAM_ON_CRASH = "on_crash";
    public static final String VM_PARAM_DOMAIN_TYPE = "domain-type";
    public static final String HW_ADDRESS = "address";
    public static final Map<String, String> PARAM_INTERFACE_TAG = new HashMap<>();
    public static final Map<String, String> PARAM_INTERFACE_ATTRIBUTE = new HashMap<>();
    public static final Map<String, String> PARAM_DISK_TAG = new HashMap<>();
    public static final Map<String, String> PARAM_DISK_ATTRIBUTE = new HashMap<>();
    public static final Map<String, String> PARAM_FILESYSTEM_TAG = new HashMap<>();
    public static final Map<String, String> PARAM_FILESYSTEM_ATTRIBUTE = new HashMap<>();
    public static final Map<String, String> PARAM_INPUTDEV_TAG = new HashMap<>();
    public static final Map<String, String> PARAM_INPUTDEV_ATTRIBUTE = new HashMap<>();
    public static final Map<String, String> PARAM_GRAPHICS_TAG = new HashMap<>();
    public static final Map<String, String> PARAM_GRAPHICS_ATTRIBUTE = new HashMap<>();
    public static final Map<String, String> PARAM_SOUND_TAG = new HashMap<>();
    public static final Map<String, String> PARAM_SOUND_ATTRIBUTE = new HashMap<>();
    public static final Map<String, String> PARAM_SERIAL_TAG = new HashMap<>();
    public static final Map<String, String> PARAM_SERIAL_ATTRIBUTE = new HashMap<>();
    public static final Map<String, String> PARAM_PARALLEL_TAG = new HashMap<>();
    public static final Map<String, String> PARAM_PARALLEL_ATTRIBUTE = new HashMap<>();
    public static final Map<String, String> PARAM_VIDEO_TAG = new HashMap<>();
    public static final Map<String, String> PARAM_VIDEO_ATTRIBUTE = new HashMap<>();

    static {
        VMParams.PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.TYPE, "type");
        VMParams.PARAM_INTERFACE_TAG.put(InterfaceData.MAC_ADDRESS, "mac");
        VMParams.PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.MAC_ADDRESS, "address");
        VMParams.PARAM_INTERFACE_TAG.put(InterfaceData.SOURCE_NETWORK, "source");
        VMParams.PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.SOURCE_NETWORK, "network");
        VMParams.PARAM_INTERFACE_TAG.put(InterfaceData.SOURCE_BRIDGE, "source");
        VMParams.PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.SOURCE_BRIDGE, "bridge");
        VMParams.PARAM_INTERFACE_TAG.put(InterfaceData.TARGET_DEV, "target");
        VMParams.PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.TARGET_DEV, "dev");
        VMParams.PARAM_INTERFACE_TAG.put(InterfaceData.MODEL_TYPE, "model");
        VMParams.PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.MODEL_TYPE, "type");
        VMParams.PARAM_INTERFACE_TAG.put(InterfaceData.SCRIPT_PATH, "script");
        VMParams.PARAM_INTERFACE_ATTRIBUTE.put(InterfaceData.SCRIPT_PATH, "path");

        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.TYPE, "type");
        VMParams.PARAM_DISK_TAG.put(DiskData.TARGET_DEVICE, "target");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.TARGET_DEVICE, "dev");
        VMParams.PARAM_DISK_TAG.put(DiskData.SOURCE_FILE, "source");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_FILE, "file");
        VMParams.PARAM_DISK_TAG.put(DiskData.SOURCE_DEVICE, "source");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_DEVICE, "dev");

        VMParams.PARAM_DISK_TAG.put(DiskData.SOURCE_PROTOCOL, "source");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_PROTOCOL, "protocol");
        VMParams.PARAM_DISK_TAG.put(DiskData.SOURCE_NAME, "source");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_NAME, "name");

        VMParams.PARAM_DISK_TAG.put(DiskData.SOURCE_HOST_NAME, "source:host");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_HOST_NAME, "name");
        VMParams.PARAM_DISK_TAG.put(DiskData.SOURCE_HOST_PORT, "source:host");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.SOURCE_HOST_PORT, "port");

        VMParams.PARAM_DISK_TAG.put(DiskData.AUTH_USERNAME, "auth");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.AUTH_USERNAME, "username");
        VMParams.PARAM_DISK_TAG.put(DiskData.AUTH_SECRET_TYPE, "auth:secret");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.AUTH_SECRET_TYPE, "type");
        VMParams.PARAM_DISK_TAG.put(DiskData.AUTH_SECRET_UUID, "auth:secret");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.AUTH_SECRET_UUID, "uuid");

        VMParams.PARAM_DISK_TAG.put(DiskData.TARGET_BUS, "target");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.TARGET_BUS, "bus");
        VMParams.PARAM_DISK_TAG.put(DiskData.DRIVER_NAME, "driver");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.DRIVER_NAME, "name");
        VMParams.PARAM_DISK_TAG.put(DiskData.DRIVER_TYPE, "driver");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.DRIVER_TYPE, "type");
        VMParams.PARAM_DISK_TAG.put(DiskData.DRIVER_CACHE, "driver");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.DRIVER_CACHE, "cache");
        VMParams.PARAM_DISK_ATTRIBUTE.put(DiskData.TARGET_TYPE, "device");
        VMParams.PARAM_DISK_TAG.put(DiskData.READONLY, "readonly");
        VMParams.PARAM_DISK_TAG.put(DiskData.SHAREABLE, "shareable");

        VMParams.PARAM_FILESYSTEM_ATTRIBUTE.put(InterfaceData.TYPE, "type");
        VMParams.PARAM_FILESYSTEM_TAG.put(FilesystemData.SOURCE_DIR, "source");
        VMParams.PARAM_FILESYSTEM_ATTRIBUTE.put(FilesystemData.SOURCE_DIR, "dir");
        VMParams.PARAM_FILESYSTEM_TAG.put(FilesystemData.SOURCE_NAME, "source");
        VMParams.PARAM_FILESYSTEM_ATTRIBUTE.put(FilesystemData.SOURCE_NAME, "name");
        VMParams.PARAM_FILESYSTEM_TAG.put(FilesystemData.TARGET_DIR, "target");
        VMParams.PARAM_FILESYSTEM_ATTRIBUTE.put(FilesystemData.TARGET_DIR, "dir");

        VMParams.PARAM_INPUTDEV_ATTRIBUTE.put(InputDevData.TYPE, "type");
        VMParams.PARAM_INPUTDEV_ATTRIBUTE.put(InputDevData.BUS, "bus");

        VMParams.PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.TYPE, "type");
        VMParams.PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.PORT, "port");
        VMParams.PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.AUTOPORT, "autoport");
        VMParams.PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.LISTEN, "listen");
        VMParams.PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.PASSWD, "passwd");
        VMParams.PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.KEYMAP, "keymap");
        VMParams.PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.DISPLAY, "display");
        VMParams.PARAM_GRAPHICS_ATTRIBUTE.put(GraphicsData.XAUTH, "xauth");

        VMParams.PARAM_SOUND_ATTRIBUTE.put(SoundData.MODEL, "model");

        VMParams.PARAM_SERIAL_ATTRIBUTE.put(SerialData.TYPE, "type");
        VMParams.PARAM_SERIAL_TAG.put(SerialData.SOURCE_PATH, "source");
        VMParams.PARAM_SERIAL_ATTRIBUTE.put(SerialData.SOURCE_PATH, "path");
        VMParams.PARAM_SERIAL_TAG.put(SerialData.BIND_SOURCE_MODE, "source");
        VMParams.PARAM_SERIAL_ATTRIBUTE.put(SerialData.BIND_SOURCE_MODE, "mode");
        VMParams.PARAM_SERIAL_TAG.put(SerialData.BIND_SOURCE_HOST, "source");
        VMParams.PARAM_SERIAL_ATTRIBUTE.put(SerialData.BIND_SOURCE_HOST, "host");
        VMParams.PARAM_SERIAL_TAG.put(SerialData.BIND_SOURCE_SERVICE, "source");
        VMParams.PARAM_SERIAL_ATTRIBUTE.put(SerialData.BIND_SOURCE_SERVICE, "service");
        VMParams.PARAM_SERIAL_TAG.put(SerialData.CONNECT_SOURCE_MODE, "source");
        VMParams.PARAM_SERIAL_ATTRIBUTE.put(SerialData.CONNECT_SOURCE_MODE, "mode");
        VMParams.PARAM_SERIAL_TAG.put(SerialData.CONNECT_SOURCE_HOST, "source");
        VMParams.PARAM_SERIAL_ATTRIBUTE.put(SerialData.CONNECT_SOURCE_HOST, "host");
        VMParams.PARAM_SERIAL_TAG.put(SerialData.CONNECT_SOURCE_SERVICE, "source");
        VMParams.PARAM_SERIAL_ATTRIBUTE.put(SerialData.CONNECT_SOURCE_SERVICE, "service");
        VMParams.PARAM_SERIAL_TAG.put(SerialData.PROTOCOL_TYPE, "protocol");
        VMParams.PARAM_SERIAL_ATTRIBUTE.put(SerialData.PROTOCOL_TYPE, "type");
        VMParams.PARAM_SERIAL_TAG.put(SerialData.TARGET_PORT, "target");
        VMParams.PARAM_SERIAL_ATTRIBUTE.put(SerialData.TARGET_PORT, "port");

        VMParams.PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.TYPE, "type");
        VMParams.PARAM_PARALLEL_TAG.put(ParallelData.SOURCE_PATH, "source");
        VMParams.PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.SOURCE_PATH, "path");
        VMParams.PARAM_PARALLEL_TAG.put(ParallelData.BIND_SOURCE_MODE, "source");
        VMParams.PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.BIND_SOURCE_MODE, "mode");
        VMParams.PARAM_PARALLEL_TAG.put(ParallelData.BIND_SOURCE_HOST, "source");
        VMParams.PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.BIND_SOURCE_HOST, "host");
        VMParams.PARAM_PARALLEL_TAG.put(ParallelData.BIND_SOURCE_SERVICE, "source");
        VMParams.PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.BIND_SOURCE_SERVICE, "service");

        VMParams.PARAM_PARALLEL_TAG.put(ParallelData.CONNECT_SOURCE_MODE, "source");
        VMParams.PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.CONNECT_SOURCE_MODE, "mode");
        VMParams.PARAM_PARALLEL_TAG.put(ParallelData.CONNECT_SOURCE_HOST, "source");
        VMParams.PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.CONNECT_SOURCE_HOST, "host");
        VMParams.PARAM_PARALLEL_TAG.put(ParallelData.CONNECT_SOURCE_SERVICE, "source");
        VMParams.PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.CONNECT_SOURCE_SERVICE,
                "service");

        VMParams.PARAM_PARALLEL_TAG.put(ParallelData.PROTOCOL_TYPE, "protocol");
        VMParams.PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.PROTOCOL_TYPE, "type");
        VMParams.PARAM_PARALLEL_TAG.put(ParallelData.TARGET_PORT, "target");
        VMParams.PARAM_PARALLEL_ATTRIBUTE.put(ParallelData.TARGET_PORT, "port");

        VMParams.PARAM_VIDEO_TAG.put(VideoData.MODEL_TYPE, "model");
        VMParams.PARAM_VIDEO_ATTRIBUTE.put(VideoData.MODEL_TYPE, "type");
        VMParams.PARAM_VIDEO_TAG.put(VideoData.MODEL_VRAM, "model");
        VMParams.PARAM_VIDEO_ATTRIBUTE.put(VideoData.MODEL_VRAM, "vram");
        VMParams.PARAM_VIDEO_TAG.put(VideoData.MODEL_HEADS, "model");
        VMParams.PARAM_VIDEO_ATTRIBUTE.put(VideoData.MODEL_HEADS, "heads");
    }

}
