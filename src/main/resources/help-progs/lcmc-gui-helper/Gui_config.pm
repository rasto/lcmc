package Gui_config;

sub get_gui_info {
    my $out = "";
    if (open FH, "/var/lib/heartbeat/drbdgui.cf") {
        while (<FH>) {
            $out .= "$_";
        }
        close FH;
    }
    return $out;
}

sub get_gui_options_info {
    my $out = "o:vm.filesystem.source.dir.lxc\n";
    $out .= "/var/lib/lxc\n";
    $out .= Command::_exec("ls -1d /var/lib/lxc/*/rootfs 2>/dev/null");
    return $out;
}

