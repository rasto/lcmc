package lcmc.vm.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.SneakyThrows;

public class VMCreatorTest {

    private final HashMap<String, String> parameters = new HashMap<>();
    private final VMCreator vmCreator = new VMCreator();
    private final Document document = createDocument();

    @Before
    public void setUp() {
        parameters.put(VMParams.VM_PARAM_NAME, "NAME");
        parameters.put(VMParams.VM_PARAM_EMULATOR, "EMULATOR");
        parameters.put(VMParams.VM_PARAM_UUID, "UUID");
        parameters.put(VMParams.VM_PARAM_VCPU, "VCPU");
        parameters.put(VMParams.VM_PARAM_BOOTLOADER, "BOOTLOADER");
        parameters.put(VMParams.VM_PARAM_CURRENTMEMORY, "234");
        parameters.put(VMParams.VM_PARAM_MEMORY, "123");
        parameters.put(VMParams.VM_PARAM_BOOT, "BOOT");
        parameters.put(VMParams.VM_PARAM_BOOT_2, "BOOT_2");
        parameters.put(VMParams.VM_PARAM_LOADER, "LOADER");
        parameters.put(VMParams.VM_PARAM_AUTOSTART, "AUTOSTART");
        parameters.put(VMParams.VM_PARAM_VIRSH_OPTIONS, "VIRSH_OPTIONS");
        parameters.put(VMParams.VM_PARAM_TYPE, "TYPE");
        parameters.put(VMParams.VM_PARAM_INIT, "INIT");
        parameters.put(VMParams.VM_PARAM_TYPE_ARCH, "TYPE_ARCH");
        parameters.put(VMParams.VM_PARAM_TYPE_MACHINE, "TYPE_MACHINE");
        parameters.put(VMParams.VM_PARAM_ACPI, "ACPI");
        parameters.put(VMParams.VM_PARAM_APIC, "APIC");
        parameters.put(VMParams.VM_PARAM_PAE, "PAE");
        parameters.put(VMParams.VM_PARAM_HAP, "HAP");
        parameters.put(VMParams.VM_PARAM_CLOCK_OFFSET, "CLOCK_OFFSET");
        parameters.put(VMParams.VM_PARAM_CPU_MATCH, "CPU_MATCH");
        parameters.put(VMParams.VM_PARAM_CPUMATCH_MODEL, "CPUMATCH_MODEL");
        parameters.put(VMParams.VM_PARAM_CPUMATCH_VENDOR, "CPUMATCH_VENDOR");
        parameters.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS, "CPUMATCH_TOPOLOGY_SOCKETS");
        parameters.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES, "CPUMATCH_TOPOLOGY_CORES");
        parameters.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS, "CPUMATCH_TOPOLOGY_THREADS");
        parameters.put(VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY, "CPUMATCH_FEATURE_POLICY");
        parameters.put(VMParams.VM_PARAM_CPUMATCH_FEATURES, "CPUMATCH_FEATURES");
        parameters.put(VMParams.VM_PARAM_ON_POWEROFF, "ON_POWEROFF");
        parameters.put(VMParams.VM_PARAM_ON_REBOOT, "ON_REBOOT");
        parameters.put(VMParams.VM_PARAM_ON_CRASH, "ON_CRASH");
        parameters.put(VMParams.VM_PARAM_DOMAIN_TYPE, "DOMAIN_TYPE");
    }

    @Test
    @SneakyThrows
    public void shouldCreateLoaderNode() {
        parameters.put(VMParams.VM_PARAM_LOADER, "LOADER");
        vmCreator.init(document, parameters);
        vmCreator.createDomain("UUID", "DOMAIN_NAME", false, "kvm");

        final var node = findNode(document, "domain/os/loader");

        assertThat(node.getFirstChild().getTextContent()).isEqualTo("LOADER");
    }

    @Test
    @SneakyThrows
    public void shouldOmitLoaderNode() {
        parameters.put(VMParams.VM_PARAM_LOADER, "");
        vmCreator.init(document, parameters);
        vmCreator.createDomain("UUID", "DOMAIN_NAME", false, "kvm");

        final var node = findNode(document, "domain/os/loader");

        assertThat(node).isNull();
    }

    @SneakyThrows
    private Node findNode(Document document, String path) {
        final XPath xpath = XPathFactory.newInstance().newXPath();
        return ((NodeList) xpath.evaluate(path, document, XPathConstants.NODESET)).item(0);
    }

    @SneakyThrows
    private Document createDocument() {
        return DocumentBuilderFactory.newInstance()
                                     .newDocumentBuilder()
                                     .newDocument();
    }
}