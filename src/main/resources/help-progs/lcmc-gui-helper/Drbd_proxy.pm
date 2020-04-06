package Drbd_proxy;

our $DRBD_PROXY_GET_PLUGINS;
our $DRBD_PROXY_SHOW;

sub init {
    $DRBD_PROXY_GET_PLUGINS = "drbd-proxy-ctl -c 'show avail-plugins'";
    $DRBD_PROXY_SHOW = "drbd-proxy-ctl -c show";
}
sub get_drbd_proxy_plugins {
    my $out = Command::_exec("$DRBD_PROXY_GET_PLUGINS");
    my @parts = split /\n\n/, $out;
    my %plugins = (
        debug => "",
        lzma  => "",
        zlib  => "",
    ); # default in case proxy is not installed
    for my $p (@parts) {
        my ($name, $desc) = $p =~ /Plugin.*?:\s+(\S+)\s+(.*)/s;
        $desc =~ s/\n/&lt;br&gt;/;
        $desc =~ s!\*(.*?)\*!&lt;b&gt;$1&lt;/b&gt;!;
        $plugins{$name} = $desc;
    }
    return \%plugins;

}

sub get_drbd_proxy_xml {
    my $proxy = <<"MEMLIMIT";
<command name="proxy">
	<option name="memlimit" type="numeric">
		<default>16</default>
		<unit_prefix>M</unit_prefix>
		<unit>bytes</unit>
		<desc>&lt;html&gt;
The amount of memory used by the proxy for incoming packets. This means&lt;br&gt;
the raw data size of DRBD packets. The actual memory used is typically&lt;br&gt;
twice as much (depending on the compression ratio)
		&lt;/html&gt;</desc>
	</option>
MEMLIMIT
    my $plugins = get_drbd_proxy_plugins();
    my %boolean_plugins = (debug => 1,
        noop                     => 1);
    for my $plugin (sort keys %$plugins) {
        if ($$plugins{$plugin} =~ /compress/i) {
            $proxy .= <<"PLUGIN";
	<option name="plugin-$plugin" type="handler">
		<handler>level 9</handler>
		<handler>contexts 4 level 9</handler>
		<handler>level 8</handler>
		<handler>level 7</handler>
		<handler>level 6</handler>
		<handler>level 5</handler>
		<handler>level 4</handler>
		<handler>level 3</handler>
		<handler>level 2</handler>
		<handler>level 1</handler>
		<desc>&lt;html&gt;
		$$plugins{$plugin}
		&lt;/html&gt;</desc>
	</option>
PLUGIN
        }
        else {
            my $type = "string";
            $type = "boolean" if $boolean_plugins{$plugin};
            $proxy .= <<"PLUGIN";
	<option name="plugin-$plugin" type="$type">
		<desc>&lt;html&gt;
		$$plugins{$plugin}
		&lt;/html&gt;</desc>
	</option>
PLUGIN
        }
    }
    # deprecated options
    $proxy .= <<"OTHER";
	<option name="read-loops" type="numeric">
		<desc>&lt;html&gt;
		&lt;b&gt;DEPRECATED&lt;/b&gt;: don't use
		&lt;/html&gt;</desc>
	</option>
	<option name="compression" type="handler">
		<handler>on</handler>
		<handler>off</handler>
		<desc>&lt;html&gt;
		&lt;b&gt;DEPRECATED&lt;/b&gt;: don't use
		&lt;/html&gt;</desc>
	</option>
OTHER
    $proxy .= "</command>\n";
    print $proxy;
}

sub get_drbd_proxy_info {
    my $out = "";
    for (Command::_exec("$DRBD_PROXY_SHOW 2>/dev/null")) {
        if (/add connection\s+(\S*)/) {
            $out .= "up:$1\n";
        }
    }
    return $out;
}
