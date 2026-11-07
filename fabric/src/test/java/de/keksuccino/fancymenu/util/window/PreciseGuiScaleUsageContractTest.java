package de.keksuccino.fancymenu.util.window;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Window#getGuiScale() only exposes the floored integer part of FancyMenu's precise GUI scale, so while a layout applies a
 * fractional scale (auto-scaling), every conversion between GUI units and window pixels has to use {@link WindowHandler#getGuiScale()}.
 */
class PreciseGuiScaleUsageContractTest {

    private static final String PACKAGE_PATH = "de/keksuccino/fancymenu/";
    private static final List<String> MODULES = List.of("common", "fabric", "forge", "neoforge");
    private static final Pattern INTEGER_SCALE_READ = Pattern.compile("(?<!WindowHandler)\\.getGuiScale\\(\\)");

    /** Files that intentionally read the integer scale, with the reason. */
    private static final Map<String, String> ALLOWED = Map.of(
            "mixin/mixins/common/client/MixinWindow.java", "vanilla fallback of the precise scale itself",
            "customization/element/AbstractElement.java", "persisted auto-sizing baselines were recorded with the integer scale",
            "customization/placeholder/placeholders/gui/GuiScalePlaceholder.java", "user-facing value of the GUI scale placeholder",
            "customization/requirement/requirements/gui/IsGuiScaleRequirement.java", "user-facing GUI scale requirement"
    );

    @Test
    void pixelConversionsUseThePreciseGuiScale() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (SourceFile file : collectSources()) {
            if (ALLOWED.containsKey(file.packageRelativePath())) continue;
            String[] lines = file.content().split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (INTEGER_SCALE_READ.matcher(lines[i]).find()) offenders.add(file.packageRelativePath() + ":" + (i + 1));
            }
        }
        assertTrue(offenders.isEmpty(), "Integer GUI scale read where WindowHandler.getGuiScale() is needed: " + offenders);
    }

    @Test
    void allowListOnlyContainsFilesThatStillReadTheIntegerScale() throws IOException {
        List<SourceFile> sources = collectSources();
        for (String allowed : ALLOWED.keySet()) {
            boolean stillReads = sources.stream()
                    .filter(file -> file.packageRelativePath().equals(allowed))
                    .anyMatch(file -> INTEGER_SCALE_READ.matcher(file.content()).find());
            assertTrue(stillReads, "Stale allow-list entry, remove it: " + allowed);
        }
    }

    private static List<SourceFile> collectSources() throws IOException {
        Path repositoryRoot = findRepositoryRoot();
        List<SourceFile> sources = new ArrayList<>();
        for (String module : MODULES) {
            Path javaRoot = repositoryRoot.resolve(module).resolve("src/main/java");
            if (!Files.isDirectory(javaRoot)) continue;
            try (Stream<Path> files = Files.walk(javaRoot)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String relative = javaRoot.relativize(file).toString().replace('\\', '/');
                    if (!relative.startsWith(PACKAGE_PATH)) continue;
                    sources.add(new SourceFile(relative.substring(PACKAGE_PATH.length()), Files.readString(file, StandardCharsets.UTF_8)));
                }
            }
        }
        assertTrue(sources.size() > 100, "Source scan found too few files, the repository layout probably changed");
        return sources;
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle")) || Files.isRegularFile(current.resolve("settings.gradle.kts"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate the repository root");
    }

    private record SourceFile(String packageRelativePath, String content) {
    }

}
