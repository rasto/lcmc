package Gui_Test;

our $suffix;

sub gui_test_compare {
    my $testfile_part = shift;
    my $realconf = shift;

	my $try = 0;
	my $res;
    do {
		sleep($try * 10);
        $suffix = 0;
        $res = gui_test_compare_once($testfile_part, $realconf);
		$try++;
    } until ($res == 0 || $try > 3);

    if ($res == 0) {
        print "ok ";
    } else {
        my $testfile;
        if ($suffix > 0) {
            $testfile = "$testfile_part-$suffix";
        }
        else {
            $testfile = $testfile_part;
        }
        open TEST, ">$testfile" or print "$!";
        print TEST $realconf;
        close TEST;
        print "failed ";
		exit 1;
    }
}

sub gui_test_compare_once {
    my $testfile_part = shift;
    my $realconf = remove_spaces(shift);
    my $test = "";
    my $diff = "";
    do {
        my $testfile;
        if ($suffix > 0) {
            $testfile = "$testfile_part-$suffix";
        } else {
            $testfile = $testfile_part;
        }
        my $notestfile;
        if (!open TEST, $testfile) {
            print "$!";
            # .new can be used for new tests.
            open TEST, ">$testfile.new" or print "$!";
            print TEST $test;
            close TEST;
            $notestfile++;
        } else {
            {
                local $/;
                $test = remove_spaces(<TEST>);
            }
            close TEST;
        }
        open TEST, ">$testfile.error" or print "$!";
        print TEST $realconf;
        close TEST;
        open TEST, ">$testfile.error.file" or print "$!";
        print TEST $test;
        close TEST;
        $diff .= Command::_exec("diff -u $testfile.error.file $testfile.error") . "\n";
		unlink "$testfile.error" or die "$!";
        unlink "$testfile.error.file" or die "$!";
        $suffix++;
    } until ($realconf eq $test || !-e "$testfile_part-$suffix");
    if ($realconf eq $test) {
		return 0;
    } else {
        print "error\n";
        print "-------------\n";
        print $diff;
        return 1;
    }
}

sub remove_spaces {
    my $config = shift // "";
    $config =~ s/\s+//mg;
    return $config;
}

sub gui_pcmk_config_test {
    my $testname = shift;
    my $index = shift;
    my @hosts = @_;
    my $crm_config = Command::_exec_or_die("TERM=dumb PATH=\$PATH:/usr/sbin /usr/sbin/crm configure show");
    for my $host (@hosts) {
        $crm_config =~ s/$host\b/host/gi;
    }
    gui_test_compare("/tmp/lcmc-test/$testname/test$index.crm", strip_crm_config($crm_config));
}

sub gui_pcmk_status_test {
    my $testname = shift;
    my $index = shift;
    my @hosts = @_;
    my $status = Command::_exec_or_die("crm_resource --list");
    for my $host (@hosts) {
        $status =~ s/$host\b/host/gi;
    }

    gui_test_compare("/tmp/lcmc-test/$testname/status$index.crm", $status);
}

sub strip_crm_config {
    my $crm_config = shift;
    $crm_config =~ s/^property.*//ms;
    $crm_config =~ s/^rsc_defaults .*//ms;
    $crm_config =~ s/^node .*//mg;
    $crm_config =~ s/^\s*attributes .*//mg;
    $crm_config =~ s/\\$//mg;
    $crm_config =~ s/^\s+//g;
    $crm_config =~ s/\s+$//g;
    # older crm shell had _rsc_set_
    $crm_config =~ s/_rsc_set_ //g;
    $crm_config =~ s/(start-delay=\d+) (timeout=\d+)/$2 $1/g;
    return $crm_config;
}


sub gui_vm_test {
    my $testname = shift;
    my $index = shift;
    my $name = shift;
    my $xml;
    for (@VM::VM_OPTIONS) {
        $xml = Command::_exec_or_die("$VM::VIRSH_COMMAND_NO_RO $_ dumpxml --security-info $name 2>/dev/null");
        if ($xml !~ /^\s*$/) {
            last;
        }
    }
    $xml =~ s/$name/\@NAME@/gm;
    my $simplified_xml = join "\n", $xml =~ /^\s*((?:<domain.*|<name.*|<disk.*|<source.*|<interface.*|<graphics type\S+))/mg;
    gui_test_compare("/tmp/lcmc-test/$testname/domain.xml$index", $simplified_xml);
}

sub gui_drbd_test {
    my $testname = shift;
    my $index = shift;
    my @hosts = @_;
    if (!open CONF, "/etc/drbd.conf") {
        print "$!";
        exit 2;
    }
    my $conf;
    {
        local $/;
        $conf = <CONF>;
    }
    close CONF;
    if (!$conf) {
        print "no /etc/drbd.conf";
        exit 3;
    }

    if ($conf =~ m!^include "drbd\.d/\*\.res"!m) {
        if (opendir my $dir, "/etc/drbd.d/") {
            for my $file (sort grep {/^[^.]/} readdir $dir) {
                $conf .= "--- $file ---\n";
                open my $fh, "/etc/drbd.d/$file" or die $!;
                {
                    local $/;
                    $conf .= <$fh>;
                }
                $conf .= "--- $file ---\n";
            }
        }
    }

    if (!open PROC, "/proc/drbd") {
        return;
    }
    my $proc = "";
    while (<PROC>) {
        next if /^version:/;
        next if /^GIT-hash:/;
        next if /^srcversion:/;
        next if /^\s+ns:/;
        next if /^\s+\d+:\s+cs:Unconfigured/;
        s/(\s\S\sr----)$/$1-/;
        $proc .= $_;
    }
    close PROC;
    for ($conf) {
        my $i = 1;
        for my $host (@hosts) {
            s/$host\b/host$i/gi;
            $i++;
        }
    }
    $conf =~ s/^(## generated by drbd-gui )\S+/$1VERSION/m;
    $conf =~ s/^(\s+shared-secret\s+)[^;]+/$1SECRET/m;
    $conf =~ s/^(\s+disk\s+)[^;{]+(\s*;\s*)$/$1DISK$2/mg;
    $conf =~ s/^(\s+address\s+)(?!.*127\.0\.0\.1)[^:]+/$1IP/mg;
    $conf =~ s/^(\s+outside\s+)[^:]+/$1IP/mg;
    my $libdir = Host_software::get_hb_lib_path();
    $conf =~ s/$libdir/LIBDIR/g;
    gui_test_compare("/tmp/lcmc-test/$testname/drbd.conf$index", $conf);
    gui_test_compare("/tmp/lcmc-test/$testname/proc$index", $proc);
}
