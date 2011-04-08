#!/usr/bin/perl

use strict;
use warnings;

use File::Find;

my $dir = "src/java/drbd/configs";
opendir my $dh, $dir or die "cannot open $dir: $!";
for my $file (grep { /TextResource.*\.java/} readdir ($dh)) {
	print "file: $file\n";
	open my $f, "src/java/drbd/configs/TextResource_ja_JP.java" or die "$!";
	my %strings;
	my @prev_parts;
	my %parts;
	my $pos = 0;
	while (<$f>) {
		if (/^\s*{"(.*)",/) {
			die "duplicate: $1" if $strings{$1};
			$strings{$1}++;
			$pos++;
		}
	}
	our %strings_in_files;
	find(\&wanted, "src/java/drbd/");
	for my $s (keys %strings) {
		if (!$strings_in_files{$s}) {
			print "superfluous: $s\n";
#		print `grep -rs $s src/drbd|grep -v TextResource`;
		}
	}

	sub wanted {
		return if $_ !~ /\.java$/;
		open my $f, "$_" or die "cannot open $File::Find::name: $!";
		my $cont = do { local $/; <$f> };
		my @ss = $cont =~ /Tools.getString\(\s*"(.*?)"/msg;
		for my $s (@ss) {
			if (!$strings{$s}) {
				if ($s ne "Dialog.Dialog.") {
					print "not defined: $File::Find::name $s\n";
				}
			}
			$strings_in_files{$s}++;
		}
	}
}

