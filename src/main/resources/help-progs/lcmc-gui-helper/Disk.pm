package Disk;

our $LV_CACHE;
our $VG_CACHE;

# get_mount
#
# returns hash with block device as a key and mount point with filesystem as
# value. LVM device name is converted to the /dev/group/name from
# /dev/group-name. If there is - in the group or name, it is escaped as --, so
# it is unescaped here. /bin/mount is used rather than cat /proc/mounts,
# because in the output from /bin/mount is lvm device name always in the same
# form.
sub get_mount {
    my $drbd_devs = shift;
    my %dev_to_mount;
    for (Command::_exec("/bin/mount")) {
        # /dev/md1 on / type xfs (rw)
        # /dev/mapper/vg00--sepp-sources on /mnt/local-src type xfs (rw)
        if (m!/dev/(\S+)\s+on\s+(\S+)\s+type\s+(\S+)!) {
            my ($dev, $mountpoint, $filesystem) = ($1, $2, $3);
            $dev = "/dev/$dev";
            if ($$drbd_devs{"$dev"}) {
                $dev = $$drbd_devs{"$dev"};
            }
            if ($dev =~ m!^/dev/mapper/(.+)!) {
                # convert mapper/vg00--sepp-sources to vg00-sepp/sources
                my ($group, $name) = map {s/--/-/g;
                    $_} $1 =~ /(.*[^-])-([^-].*)/;
                if ($group && $name) { # && !$$lvm_devs{"$group/$name"}) {
                    $dev = "/dev/$group/$name";
                }
            }
            Log::print_debug("mount: $dev, $mountpoint, $filesystem");
            $dev_to_mount{$dev} = "$mountpoint fs:$filesystem";
        }
    }
    return \%dev_to_mount;
}

#
# Returns hash with free disk space
sub disk_space {
    my %dev_to_used;
    for (Command::_exec("/bin/df -Pl 2>/dev/null")) {
        if (m!(\S+)\s+\d+\s+\d+\s+\d+\s+(\d+)%\s+!) {
            my ($dev, $used) = ($1, $2);
            if ($dev =~ m!^/dev/mapper/(.+)!) {
                # convert mapper/vg00--sepp-sources to vg00-sepp/sources
                my ($group, $name) = map {s/--/-/g;
                    $_} $1 =~ /(.*[^-])-([^-].*)/;
                if ($group && $name) { # && !$$lvm_devs{"$group/$name"}) {
                    $dev = "/dev/$group/$name";
                }
            }
            $dev_to_used{$dev} = $used;
            print "$dev $used\n"
        }
    }
}

# get_swaps
# returns hash with swaps as keys.
sub get_swaps {
    open SW, "/proc/swaps" or Log::disk_info_error("cannot open /proc/swaps");
    my %swaps;
    while (<SW>) {
        next if /^Filename/; # header
        my ($swap_dev) = split;
        if ($swap_dev =~ m!^/dev/mapper/(.+)!) {
            # convert
            my ($group, $name) = map {s/--/-/g;
                $_} $1 =~ /(.*[^-])-([^-].*)/;
            if ($group && $name) {
                $swap_dev = "/dev/$group/$name";
            }
        }
        $swaps{$swap_dev}++;
    }
    return \%swaps;
}

# get_lvm
#
# returns 4 hashes. One hash that maps lvm group to the physical volume. A hash
# that maps major and minor kernel numbers to the lvm device name. Major and
# minor numbers are separated with ":". And a hash that contains block devices
# that have lvm on top of them.
sub get_lvm {
    my $use_cache = shift // 0;
    if ($use_cache && $LV_CACHE) {
        return @$LV_CACHE;
    }
    my $path = "/usr/sbin/";
    if (-e "/sbin/pvdisplay") {
        $path = "/sbin";
    }
    if (!-e "/sbin/pvdisplay" && !-e "/usr/sbin/pvdisplay") {
        return({}, {}, {}, {});
    }

    # create physical volume to volume group hash
    my %pv_to_group;
    for (Command::_exec("$path/pvdisplay -C --noheadings -o pv_name,vg_name 2>/dev/null")) {
        my ($pv_name, $vg_name) = split;
        $pv_name =~ s!^/dev/!!;
        Log::print_debug("pv: $pv_name, $vg_name");
        $pv_to_group{$pv_name} = $vg_name;
    }

    my %major_minor_to_dev;
    my %major_minor_to_group;
    my %major_minor_to_lv_name;

    # create major:minor kernel number to device hash
    for (Command::_exec("$path/lvdisplay -C --noheadings -o lv_kernel_major,lv_kernel_minor,vg_name,lv_name 2>/dev/null")) {
        my ($major, $minor, $group, $name) = split;
        Log::print_debug("get_lvm: ($major, $minor, $group, $name)");
        $major_minor_to_dev{"$major:$minor"} = "$group/$name";
        $major_minor_to_group{"$major:$minor"} = $group;
        $major_minor_to_lv_name{"$major:$minor"} = $name;
    }
    $LV_CACHE = [ \%pv_to_group,
        \%major_minor_to_dev,
        \%major_minor_to_group,
        \%major_minor_to_lv_name ];
    return @$LV_CACHE;
}

# this is used if the devices is dm but not lvm
sub get_device_mapper_hash {
    my %major_minor_hash;
    my $dir = "/dev/mapper";
    if (opendir(DIR, $dir)) {
        for (grep {$_ !~ /^\./ && -b "$dir/$_"} readdir(DIR)) {
            my $out = Command::_exec("/sbin/dmsetup info $dir/$_ 2>&1");
            if ($out) {
                my ($major, $minor) =
                    $out =~ /^Major.*?(\d+)\D+(\d+)/m;
                $major_minor_hash{"$major:$minor"} = "$dir/$_";
            }

        }
        closedir DIR;
    }
    return \%major_minor_hash;
}

# get_raid()
#
# returns hash with devices that are in the raid.
sub get_raid {
    return if !-e "/proc/mdstat";
    open MDSTAT, "/proc/mdstat" or Log::disk_info_error("cannot open /proc/mdstat");
    # md1 : active raid1 sdb2[1] sda2[0]
    #	   9775488 blocks [2/2] [UU]
    my %devs_in_raid;

    # create hash with devices that are in the raid.
    while (<MDSTAT>) {
        if (/^(md\d+)\s+:\s+(.+)/ # old way
            || /^(md_d\d+)\s+:\s+(.+)/) {
            my $dev = $1;
            my ($active, $type, @members) = split /\s+/, $2;
            Log::print_debug("get_raid: $dev ($active, $type, @members)");
            for my $member (@members) {
                $member =~ s/\[\d+\]$//;
                $devs_in_raid{"$member"}++;
            }
        }
    }
    return \%devs_in_raid;
}

sub get_device_mapper_major {
    my $m = 253;
    open DM, "/proc/devices" or Log::disk_info_error("cannot open /proc/devices");
    while (<DM>) {
        $m = $1 if /^(\d+)\s+device-mapper/;
    }
    close DM;
    return $m;
}

sub get_disk_uuid_map {
    my $dir = shift;
    my %ids;
    if (opendir(DIR, $dir)) {
        for (grep {$_ !~ /^\./ && -l "$dir/$_"} readdir(DIR)) {
            my $dev = Command::_exec("readlink -f \"$dir/$_\"", 2);
            chomp $dev;
            $ids{$dev} = "$dir/$_";
        }
        closedir DIR;
    }
    return \%ids;
}

sub get_disk_id_map {
    my $dir = shift;
    my %ids;
    if (opendir(DIR, $dir)) {
        for (grep {$_ !~ /^\./ && -l "$dir/$_"} readdir(DIR)) {
            my $dev = Command::_exec("readlink -f \"$dir/$_\"", 2);
            chomp $dev;
            push @{$ids{$dev}}, "$dir/$_";
        }
        closedir DIR;
    }
    return \%ids;
}

# get_disk_info()
#
# parses /proc/partitions and writes device and size of one block device per
# line separated by one space. If block device is mounted, mount point and
# file system type is attached.
# It doesn't show block devices, that are in raid or there is lvm on top of
# them. In this case only device names of raid or lvm are used.
sub get_disk_info {
    my $use_lvm_cache = shift // die;
    my $drbd_devs = shift // die;
    my $devs_in_raid = get_raid();
    my ($pvs,
        $lvm_major_minor_to_dev,
        $lvm_major_minor_to_group,
        $lvm_major_minor_to_lv_name) = get_lvm($use_lvm_cache);
    my $dm_major_minor_to_dev = get_device_mapper_hash();
    my $dev_to_mount = get_mount($drbd_devs);
    my $dev_to_swap = get_swaps();
    # read partition table
    open PT, "/proc/partitions" or Log::disk_info_error("cannot open /proc/partitions");
    my $info;
    my $device_mapper_major = get_device_mapper_major();
    my $by_uuids = get_disk_uuid_map("/dev/disk/by-uuid");
    my $by_ids = get_disk_id_map("/dev/disk/by-id");
    while (<PT>) {
        next if /^major / || /^$/; # skip header
        chomp;
        my ($major, $minor, $blocks, $name) = split;
        next if $$devs_in_raid{$name};
        my $device;
        my $lvm_group;
        my $lv_name;
        if ($major == $device_mapper_major) {
            if ($$lvm_major_minor_to_dev{"$major:$minor"}) {
                $device = "/dev/" . $$lvm_major_minor_to_dev{"$major:$minor"};
                my $dev = $$lvm_major_minor_to_dev{"$major:$minor"};
                $dev = $name if !$dev;
                $device = "/dev/" . $dev;
                $lvm_group = $$lvm_major_minor_to_group{"$major:$minor"};
                $lv_name = $$lvm_major_minor_to_lv_name{"$major:$minor"};
            }
            elsif ($$dm_major_minor_to_dev{"$major:$minor"}) {
                $device =
                    $$dm_major_minor_to_dev{"$major:$minor"};
                if ($device =~ /(-cow|-real)$/) {
                    # skip snapshot devices.
                    next;
                }
            }
            else {
                $device = "/dev/$name";
            }
        }
        elsif ($major == 1) {
            next; # ramdisk
        }
        elsif ($major == 7) {
            next; # loop device
        }
        elsif ($major == 3
            || $major == 8
            || $major == 72
            || $major == 202
            || $major == 104) { # ide and scsi disks
            # 104 cciss0
            if ($_ !~ /\d$/) { # whole disk
                $device = "/dev/$name";
            }
            elsif ($blocks == 1) { # extended partition
                next;
            }
            else {
                $device = "/dev/$name";
            }
        }
        elsif ($major == 9 || $major == 254) { # raid
            $device = "/dev/$name";
        }
        elsif ($name =~ /^drbd/) {
            $device = "/dev/$name";
        }
        else {
            Log::disk_info_warning("unknown partition: $_");
            $device = "/dev/$name";
        }
        my $readlink = Command::_exec("readlink -f $device", 2);
        chomp $readlink;
        my $dev_sec = $$by_uuids{$readlink} || $readlink || $device;

        my $mount = $$dev_to_mount{$device} || $$dev_to_mount{$dev_sec};
        my $swap = $$dev_to_swap{$device} || $$dev_to_swap{$dev_sec};

        my $disk_ids_s = "";

        my $disk_ids = $$by_ids{$readlink};
        if ($disk_ids) {
            for (@$disk_ids) {
                $disk_ids_s .= " disk-id:" . $_;
            }
        }

        $info .= "$device uuid:$dev_sec$disk_ids_s size:$blocks";
        $info .= " mp:$mount" if $mount;
        $info .= " fs:swap mp:swap" if $swap;
        $info .= " lv:" . $lv_name if defined $lv_name;
        $info .= " vg:" . $lvm_group if defined $lvm_group;
        $info .= " pv:" . $$pvs{$name} if defined $$pvs{$name};
        $info .= "\n";
    }
    close PT;

    return $info;
}

# returns volume group info
sub get_vg_info {
    my $use_cache = shift // 0;
    if ($use_cache && defined $VG_CACHE) {
        return $VG_CACHE;
    }
    my $path = "/usr/sbin/";
    if (-e "/sbin/pvdisplay") {
        $path = "/sbin";
    }
    my $out = "";
    for (Command::_exec("$path/vgdisplay -C --noheadings --units b -o name,free 2>/dev/null")) {
        my ($name, $free) = split;
        $free =~ s/B$//;
        $out .= "$name $free\n";
    }
    $VG_CACHE = $out;
    return $out;
}

# get_filesystems_info
#
# prints available filesystems on this host.
sub get_filesystems_info {
    my $out = "";
    for (Command::_exec("ls /sbin/mkfs.* 2>/dev/null")) {
        chomp;
        my ($fs) = /([^\.]+)$/;
        Command::_exec("/sbin/modinfo $fs >/dev/null 2>&1 || grep '\\<$fs\\>' /proc/filesystems", 2);
        $out .= "$fs\n" if !$?;
    }
    return $out;
}

# get_mount_points_info
#
# prints directories in the /mnt directory
sub get_mount_points_info {
    my $dir = "/mnt";
    my $out = "";
    if (opendir(DIR, $dir)) {
        $out .= "$dir/$_\n" for (sort grep {$_ !~ /^\./ && -d "$dir/$_"} readdir(DIR));
        closedir DIR;
    }
    return $out;
}
