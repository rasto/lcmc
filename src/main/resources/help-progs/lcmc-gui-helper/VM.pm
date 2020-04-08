package VM;

our $VIRSH_COMMAND;
our %DISABLE_VM_OPTIONS; # it'll be populated for options that give an error
our $VIRSH_COMMAND_NO_RO;
our @VM_OPTIONS;

sub init() {
    $VIRSH_COMMAND = "virsh -r";
    $VIRSH_COMMAND_NO_RO = "virsh";
    @VM_OPTIONS = ("",
        "-c 'xen:///'",
        "-c lxc:///",
        "-c openvz:///system",
        "-c vbox:///session",
        "-c uml:///system");
}

# prints available qemu keymaps.
sub get_qemu_keymaps_info {
    my $out = "";
    for (Command::_exec("ls /usr/share/qemu*/keymaps/ 2>/dev/null")) {
        $out .= $_;
    }
    return $out;
}

sub get_cpu_map_info {
    my @models;
    my %vendors;
    if (open my $cpu_map_fh, "/usr/share/libvirt/cpu_map.xml") {
        while (<$cpu_map_fh>) {
            my ($model) = /<model\s+name=\'(.*)'>/;
            push @models, $model if $model;
            my ($vendor) = /<vendor>(.*)</
                || /<vendor\s+name=\'(.*?)'.*\/>/;
            $vendors{$vendor} = 1 if $vendor;
        }
    }
    my $out = "";
    $out .= "cpu-map-model-info\n";
    for (@models) {
        $out .= "$_\n";
    }
    $out .= "cpu-map-vendor-info\n";
    for (sort keys %vendors) {
        $out .= "$_\n";
    }
    return $out;
}


sub get_vm_networks {
    my %autostart;
    for (Command::_exec("ls /etc/libvirt/qemu/networks/autostart/*.xml 2>/dev/null")) {
        my ($name) = /([^\/]+).xml/;
        next if !$name;
        $autostart{$name}++;
    }
    my $out = "";
    for (Command::_exec("ls /etc/libvirt/qemu/networks/*.xml 2>/dev/null")) {
        my ($name) = /([^\/]+).xml/;
        next if !$name;
        chomp;
        my $config = Command::_exec("$VIRSH_COMMAND net-dumpxml $name 2>/dev/null")
            || "";
        if ($config) {
            $out .= "<net name=\"$name\" config=\"$_\"";
            if ($autostart{$name}) {
                $out .= ' autostart="True"';
            }
            else {
                $out .= ' autostart="False"';
            }
            $out .= ">\n";
            $out .= $config;
            $out .= "</net>\n";
        }
    }
    return $out;
}


sub get_vm_info {
    my $networks = get_vm_networks();
    my %autostart;
    for (Command::_exec("ls /etc/libvirt/qemu/autostart/*.xml 2>/dev/null; ls /etc/xen/auto/ 2>/dev/null")) {
        my ($name) = /([^\/]+).xml/;
        next if !$name;
        $autostart{$name}++;
    }
    my $libvirt_version = "";
    if (Command::_exec("$VIRSH_COMMAND version 2>/dev/null") =~ /libvirt\s+([0-9\.]+)/) {
        $libvirt_version = $1;
    }
    my $out = "<version>$libvirt_version</version>\n";
    OPTIONS:
    for my $options (@VM_OPTIONS) {
        if ($DISABLE_VM_OPTIONS{$options}) {
            next;
        }
        my $header = 1;
        for (Command::_exec("$VIRSH_COMMAND $options list --all 2>&1")) {
            if ($header) {
                if (/^-{5}/) {
                    $header = 0;
                }
                elsif (/^error:/) {
                    # disable the ones that give an
                    # error
                    $DISABLE_VM_OPTIONS{$options}++;
                    next OPTIONS;
                }
                next;
            }
            my ($name) = /^\s*\S+\s+(\S+)/;
            next if !$name;
            chomp;
            my $info =
                Command::_exec("$VIRSH_COMMAND $options dominfo $name 2>/dev/null|grep -v 'CPU time'")
                    || "";
            next if !$info;
            my $vncdisplay =
                Command::_exec("$VIRSH_COMMAND $options vncdisplay $name 2>/dev/null") || "";
            my $config_in_etc;
            #if (open CONFIG, $_) {
            #	local $/;
            #	$config_in_etc = <CONFIG>;
            #	close CONFIG;
            #}
            my $config;
            $config =
                Command::_exec("$VIRSH_COMMAND_NO_RO $options dumpxml --security-info $name 2>/dev/null") || "";
            $out .= "<vm name=\"$name\"";
            if ($autostart{$name}) {
                $out .= ' autostart="True"';
            }
            else {
                $out .= ' autostart="False"';
            }
            if ($options) {
                $out .= ' virsh-options="' . $options . '"';
            }
            $out .= ">\n";
            $out .= "<info>\n";
            $out .= $info;
            $out .= "</info>\n";
            $out .= "<vncdisplay>$vncdisplay</vncdisplay>\n";
            if ($config) {
                $out .= "<config>\n";
                $out .= $config;
                $out .= "</config>\n";
            }
            if ($config_in_etc) {
                $out .= "<config-in-etc>\n";
                $out .= "<![CDATA[$config_in_etc]]>";
                $out .= "</config-in-etc>\n";
            }
            $out .= "</vm>\n";
        }
    }
    if ($networks) {
        $out .= $networks;
    }
    my $md5 = Digest::MD5::md5_hex($out);
    my $ret = "<vms md5=\"$md5\">\n";
    $ret .= $out;
    $ret .= "</vms>\n";
    return $ret;
}
