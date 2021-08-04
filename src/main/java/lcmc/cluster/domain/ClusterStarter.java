package lcmc.cluster.domain;

import java.util.Collection;
import java.util.Set;

import javax.inject.Named;

import lcmc.cluster.ui.ClusterTabFactory;
import lcmc.cluster.ui.ClustersPanel;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;

@Named
public class ClusterStarter {
    private final Clusters allClusters;
    private final ClusterTabFactory clusterTabFactory;
    private final SwingUtils swingUtils;
    private final ClustersPanel clustersPanel;

    public ClusterStarter(Clusters allClusters, ClusterTabFactory clusterTabFactory, SwingUtils swingUtils,
            ClustersPanel clustersPanel) {
        this.allClusters = allClusters;
        this.clusterTabFactory = clusterTabFactory;
        this.swingUtils = swingUtils;
        this.clustersPanel = clustersPanel;
    }

    public void startClusters(final Collection<Cluster> selectedClusters) {
        final Set<Cluster> clusters = allClusters.getClusterSet();
        for (final Cluster cluster : clusters) {
            if (selectedClusters != null && !selectedClusters.contains(cluster)) {
                continue;
            }
            swingUtils.invokeLater(() -> clusterTabFactory.createClusterTab(cluster));
            if (cluster.getHosts()
                       .isEmpty()) {
                continue;
            }
            final boolean ok = cluster.connect(null, true, 1);
            if (!ok) {
                swingUtils.invokeLater(() -> clustersPanel.removeTabWithCluster(cluster));
                continue;
            }
            final Runnable runnable = () -> {
                for (final Host host : cluster.getHosts()) {
                    host.waitOnLoading();
                }
                swingUtils.invokeLater(() -> cluster.getClusterTab()
                                                    .ifPresent(clusterTab -> {
                                                        clusterTab.addClusterView();
                                                        clusterTab.requestFocus();
                                                    }));
            };
            final Thread thread = new Thread(runnable);
            thread.start();
        }
    }
}
