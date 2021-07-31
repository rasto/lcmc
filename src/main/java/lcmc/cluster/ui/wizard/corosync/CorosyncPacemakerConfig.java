package lcmc.cluster.ui.wizard.corosync;

import lcmc.Exceptions;
import lcmc.common.domain.util.Tools;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CorosyncPacemakerConfig {
    public static final String COROSYNC_VERSION_2 = "2";
    public static final String COROSYNC_VERSION_3 = "3";
    private final String tab;
    private final String serviceVersion;
    private final String corosyncVersion;
    private final Host[] hosts;

    private static final Logger LOG = LoggerFactory.getLogger(CorosyncPacemakerConfig.class);

    public String create() {
        if (isCorosyncVersion(COROSYNC_VERSION_2)) {
            var config = quorum();
            if (isCorosyncVersion(COROSYNC_VERSION_3)) {
                config += nodeList();
            }
            return config;
        } else {
            return service();
        }
    }

    private String service() {
        return "\nservice {\n"
                + tab
                + "ver: "
                + serviceVersion
                + '\n'
                + tab
                + "name: pacemaker\n"
                + tab
                + "use_mgmtd: no\n"
                + "}\n";
    }

    private String quorum() {
        return "\nquorum {\n"
                   + tab
                   + "provider: corosync_votequorum\n"
                   + tab
                   + "expected_votes: "
                   + hosts.length
                   + "\n}\n";
    }

    private String nodeList() {
        var nodeId = 1;
        StringBuilder config = new StringBuilder("nodelist {\n");
        for (final Host host : hosts) {
            config.append(tab)
                    .append("node {\n")
                    .append(tab)
                    .append(tab)
                    .append("nodeid: ")
                    .append(nodeId)
                    .append("\n")
                    .append(tab)
                    .append(tab)
                    .append("ring0_addr: ")
                    .append(host.getName())
                    .append("\n")
                    .append(tab)
                    .append("}\n");
            nodeId++;
        }
        config.append("}\n");
        return config.toString();
    }

    private boolean isCorosyncVersion(String version2) {
        try {
            return Tools.compareVersions(corosyncVersion, version2) >= 0;
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("corosyncConfigPacemaker: cannot compare corosync version: " + corosyncVersion);
            return false;
        }
    }
}
