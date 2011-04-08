#!/usr/bin/perl

use warnings;
use strict;

my $dfile = shift || "";
my $dir = "src/java/drbd/configs/";
opendir DIR, $dir or die "$!";
my %all;
for my $file (sort readdir DIR) {
	next if $file !~/^(DistResource.*)\.java$/;
	my @parts = split /_/, $1;
	open INPUT, "$dir$file" or die $!;
	my $text = do { local $/; <INPUT> };
	my $pp = "";
	my @methods;
	for my $p (@parts) {
		if ($all{$pp.$p}) {
			push @methods, @{$all{$pp.$p}};
		}
		$pp = $pp.$p."_";
	}
	my @method;
	for ($text =~ /^\s*\{\s*"(\S+?)Inst.install.text.(\d+)"\s*,\s*\n?\s*"(.*?)"/gm) {
		push @method, $_;
		if (@method == 3) {
			my $method = "@method";
			push @methods, $method;
			@method = ();
		}
	}
	my %mhash;
	for my $m (@methods) {
		my ($key, $x, $rest) = $m =~ /^(\w+)\s+(\d+)\s+(.*)/;
		$mhash{$key." ".$x} = $rest;
	}
	my @newmethods;
	for my $keys (sort keys %mhash) {
		push @newmethods, $keys." ".$mhash{$keys};
	}
	$all{join "_", @parts} = [@newmethods];
}

for (sort keys %all) {
	if ($dfile && $_ !~ /$dfile/i) {
		next;
	}
	print "$_\n";
	my $prev = "";
	for my $s (sort @{$all{$_}}) {
		$s =~ s/(\w+)\s+(\d+)(.*)/($2-1)." ".$1.$3/e;
		if ($prev ne $s) {
			print "    $s\n";
		}
		$prev = $s;
	}
}
closedir DIR;
