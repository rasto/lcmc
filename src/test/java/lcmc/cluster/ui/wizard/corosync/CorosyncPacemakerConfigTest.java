package lcmc.cluster.ui.wizard.corosync;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lcmc.host.domain.Host;
import lombok.val;

@ExtendWith(MockitoExtension.class)
class CorosyncPacemakerConfigTest {
    public static final String COROSYNC_VERSION_1 = "1";
    public static final String COROSYNC_VERSION_2 = "2";
    public static final String COROSYNC_VERSION_3 = "3";
    private static final String SERVICE_VERSION = "1";
    @Mock
    private Host host1;
    @Mock
    private Host host2;
    private Host[] hosts;

    @BeforeEach
    void setUp() {
        hosts = new Host[]{host1, host2};
    }

    @Test
    void shouldContainServiceIfCorosync1() {
        val creator = new CorosyncPacemakerConfig("\t", SERVICE_VERSION, COROSYNC_VERSION_1, hosts);

        String config = creator.create();

        assertThat(config).contains("service");
    }

    @Test
    void shouldContainQuorumIfCorosync2() {
        val creator = new CorosyncPacemakerConfig("\t", SERVICE_VERSION, COROSYNC_VERSION_2, hosts);

        String config = creator.create();

        assertThat(config).contains("quorum");
    }

    @Test
    void shouldNotContainNodeListIfCorosync2() {
        val creator = new CorosyncPacemakerConfig("\t", SERVICE_VERSION, COROSYNC_VERSION_2, hosts);

        String config = creator.create();

        assertThat(config).doesNotContain("nodelist");
    }

    @Test
    void shouldContainNodeListIfCorosync3() {
        val creator = new CorosyncPacemakerConfig("\t", SERVICE_VERSION, COROSYNC_VERSION_3, hosts);

        String config = creator.create();

        assertThat(config).contains("nodelist");
    }
}