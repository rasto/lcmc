package lcmc.cluster.ui.wizard.corosync;

import lcmc.host.domain.Host;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class CorosyncPacemakerConfigTest {
    public static final String COROSYNC_VERSION_1 = "1";
    public static final String COROSYNC_VERSION_2 = "2";
    public static final String COROSYNC_VERSION_3 = "3";
    private static final String SERVICE_VERSION = "1";
    @Mock
    private Host host1;
    @Mock
    private Host host2;
    private Host[] hosts;

    @Before
    public void setUp() {
        given(host1.getName()).willReturn("HOST1");
        given(host1.getName()).willReturn("HOST2");
        hosts = new Host[]{host1, host2};
    }

    @Test
    public void shouldContainServiceIfCorosync1() {
        val creator = new CorosyncPacemakerConfig("\t", SERVICE_VERSION, COROSYNC_VERSION_1, hosts);

        String config = creator.create();

        assertThat(config, containsString("service"));
    }

    @Test
    public void shouldContainQuorumIfCorosync2() {
        val creator = new CorosyncPacemakerConfig("\t", SERVICE_VERSION, COROSYNC_VERSION_2, hosts);

        String config = creator.create();

        assertThat(config, containsString("quorum"));
    }

    @Test
    public void shouldNotContainNodeListIfCorosync2() {
        val creator = new CorosyncPacemakerConfig("\t", SERVICE_VERSION, COROSYNC_VERSION_2, hosts);

        String config = creator.create();

        assertThat(config, not(containsString("nodelist")));
    }

    @Test
    public void shouldContainNodeListIfCorosync3() {
        val creator = new CorosyncPacemakerConfig("\t", SERVICE_VERSION, COROSYNC_VERSION_3, hosts);

        String config = creator.create();

        assertThat(config, containsString("nodelist"));
    }
}