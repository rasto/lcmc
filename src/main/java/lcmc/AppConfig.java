package lcmc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lcmc.common.domain.util.GuiHelperFiles;

@Configuration
public class AppConfig {
    @Bean
    public GuiHelperFiles guiHelperFiles() {
        return new GuiHelperFiles();
    }
}
