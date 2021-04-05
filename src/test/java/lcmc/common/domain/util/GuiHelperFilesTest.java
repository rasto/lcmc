package lcmc.common.domain.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import lombok.val;

class GuiHelperFilesTest {
    private final GuiHelperFiles guiHelperFiles = new GuiHelperFiles();

    @Test
    void shouldInlinePerlModules() {
        val guiHelper = guiHelperFiles.readGuiHelper();

        assertThat(guiHelper).contains("#!/usr/bin/perl");
        assertThat(guiHelper).contains("package Log");
    }
}