package lcmc.common.domain.util;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import lombok.val;

public class GuiHelperFiles {
    private static final String GUI_HELPER_DIR = "/help-progs/lcmc-gui-helper/";
    private static final String GUI_HELPER_FILENAME = GUI_HELPER_DIR + "Main.pl";

    public String readGuiHelper() {
        return Tools.readFile(GUI_HELPER_FILENAME) + inlinePerlModules();
    }

    @SneakyThrows
    private String inlinePerlModules() {
        Path start = dirPath();
        try (val paths = Files.walk(start, 1)) {
            return paths.filter(path -> path.toString().endsWith(".pm"))
                    .map(this::readPerlModule)
                    .map(this::surroundWithParenthesis)
                    .collect(Collectors.joining("\n"));
        }
    }

    @SneakyThrows
    private Path dirPath() {
        val uri = Tools.class.getResource(GUI_HELPER_DIR).toURI();
        if (uri.getScheme().equals("jar")) {
            val fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            return fileSystem.getPath(GUI_HELPER_DIR);
        } else {
            return Paths.get(uri);
        }
    }

    @SneakyThrows
    private String readPerlModule(Path path) {
        return Files.readAllLines(path)
                    .stream()
                    .map(line -> "    " + line)
                    .collect(Collectors.joining("\n"));

    }

    private String surroundWithParenthesis(String text) {
        return "\n{\n" + text + "\n}\n";
    }
}
