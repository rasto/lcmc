package lcmc.common.domain.util;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.val;

public class GuiHelperFiles {
    private static final String GUI_HELPER_DIR = "/help-progs/lcmc-gui-helper/";
    private static final String GUI_HELPER_FILENAME = GUI_HELPER_DIR + "Main.pl";
    private final URI dirUri;

    @SneakyThrows
    public GuiHelperFiles() {
        dirUri = Tools.class.getResource(GUI_HELPER_DIR).toURI();
    }

    public String readGuiHelper() {
        return Tools.readFile(GUI_HELPER_FILENAME) + inlinePerlModules();
    }

    private String inlinePerlModules() {
        if (isJarScheme()) {
            return withFilesystem(this::readPerlModules);
        } else {
            return withFiles(this::readPerlModules);
        }
    }

    @SneakyThrows
    @Synchronized
    private String withFilesystem(Function<Path, String> function) {
        try (val fileSystem = FileSystems.newFileSystem(dirUri, Collections.emptyMap())) {
            return function.apply(fileSystem.getPath(GUI_HELPER_DIR));
        }
    }

    private String withFiles(Function<Path, String> function) {
        return function.apply(Paths.get(dirUri));
    }

    @SneakyThrows
    private String readPerlModules(Path dirPath) {
        try (val paths = Files.walk(dirPath, 1)) {
            return paths.filter(path -> path.toString().endsWith(".pm"))
                    .map(this::readPerlModule)
                    .map(this::surroundWithParenthesis)
                    .collect(Collectors.joining("\n"));
        }
    }

    private boolean isJarScheme() {
        return "jar".equals(dirUri.getScheme());
    }

    @SneakyThrows
    private String readPerlModule(Path path) {
        return String.join("\n", Files.readAllLines(path));

    }

    private String surroundWithParenthesis(String text) {
        return "\n{\n" + text + "\n}\n";
    }
}
