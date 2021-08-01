package lcmc.vm.ui.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lcmc.common.domain.util.Tools;
import lcmc.vm.domain.data.DiskData;

class DiskInfoTest {
    private DiskInfo diskInfo;

    @BeforeEach
    void setUp() {
        Tools.init();
        diskInfo = new DiskInfo(null, null, null, null, null, null, null);
        diskInfo.init("", null, null);
    }

    private String invokeFixSourceHostParams(final String names, final String ports) {
        final Map<String, String> params = new HashMap<>();
        params.put(DiskData.SOURCE_HOST_NAME, names);
        params.put(DiskData.SOURCE_HOST_PORT, ports);
        diskInfo.fixSourceHostParams(params);
        return params.get(DiskData.SOURCE_HOST_NAME) + ":" + params.get(DiskData.SOURCE_HOST_PORT);
    }

    @Test
    void testFixSourceHostParams() {
        assertThat(invokeFixSourceHostParams("a,b", "1,2")).isEqualTo("a, b:1, 2");
        assertThat(invokeFixSourceHostParams("a,b", "1")).isEqualTo("a, b:1, 1");
        assertThat(invokeFixSourceHostParams("a,b", "1,2,3")).isEqualTo("a, b:1, 2");
        assertThat(invokeFixSourceHostParams("", "1,2,3")).isEqualTo(":");
        assertThat(invokeFixSourceHostParams(null, "1,2,3")).isEqualTo("null:null");
        assertThat(invokeFixSourceHostParams("a", null)).isEqualTo("a:6789");
        assertThat(invokeFixSourceHostParams("a", "")).isEqualTo("a:6789");
        assertThat(invokeFixSourceHostParams("a,b,c", "")).isEqualTo("a, b, c:6789, 6789, 6789");
        assertThat(invokeFixSourceHostParams("a,", "1,2")).isEqualTo("a:1");
        assertThat(invokeFixSourceHostParams(",a", "1,2")).isEqualTo(", a:1, 2");
        assertThat(invokeFixSourceHostParams(" a , b , c", " 1 , 2 ")).isEqualTo("a, b, c:1, 2, 2");
        assertThat(invokeFixSourceHostParams(" a , b ", " 1 , 2 , 3 ")).isEqualTo("a, b:1, 2");
    }
}
