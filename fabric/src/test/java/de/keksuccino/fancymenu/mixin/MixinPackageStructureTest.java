package de.keksuccino.fancymenu.mixin;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Configured Mixin packages are transformer-owned package prefixes. Ordinary classes placed below one of these prefixes cannot be loaded when woven target code references them, so this source-level check must run without loading the offending class first.
 */
class MixinPackageStructureTest {

    private static final Pattern CONFIGURED_PACKAGE_PATTERN = Pattern.compile("\\\"package\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern MIXIN_ANNOTATION_PATTERN = Pattern.compile("^\\s*@Mixin(?:\\s*\\(|\\s*$)", Pattern.MULTILINE);

    @ParameterizedTest
    @MethodSource("mixinConfigurations")
    void configuredMixinPackagesContainOnlyMixinTypes(String module, String configFileName) throws IOException {
        Path repositoryRoot = findRepositoryRoot();
        Path configPath = repositoryRoot.resolve(module).resolve("src/main/resources").resolve(configFileName);
        String config = Files.readString(configPath, StandardCharsets.UTF_8);
        Matcher packageMatcher = CONFIGURED_PACKAGE_PATTERN.matcher(config);
        assertTrue(packageMatcher.find(), () -> "Missing configured package in " + repositoryRoot.relativize(configPath));

        Path mixinSourceRoot = repositoryRoot.resolve(module).resolve("src/main/java").resolve(packageMatcher.group(1).replace('.', '/'));
        assertTrue(Files.isDirectory(mixinSourceRoot), () -> "Missing configured Mixin source package " + repositoryRoot.relativize(mixinSourceRoot));

        List<Path> ordinaryTypes;
        try (Stream<Path> sourceFiles = Files.walk(mixinSourceRoot)) {
            ordinaryTypes = sourceFiles.filter(path -> path.toString().endsWith(".java")).filter(path -> !isMixinType(path)).map(repositoryRoot::relativize).sorted().toList();
        }
        assertTrue(ordinaryTypes.isEmpty(), () -> "Configured Mixin packages cannot contain ordinary runtime classes because transformed target code cannot load them: " + ordinaryTypes);
    }

    private static Stream<Arguments> mixinConfigurations() {
        return Stream.of(
                Arguments.of("common", "fancymenu.mixins.json"),
                Arguments.of("fabric", "fancymenu.fabric.mixins.json"),
                Arguments.of("neoforge", "fancymenu.neoforge.mixins.json")
        );
    }

    private static boolean isMixinType(Path sourcePath) {
        try {
            return MIXIN_ANNOTATION_PATTERN.matcher(Files.readString(sourcePath, StandardCharsets.UTF_8)).find();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect " + sourcePath, exception);
        }
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.isRegularFile(current.resolve("settings.gradle"))) {
            current = current.getParent();
        }
        assertNotNull(current, "Could not locate the repository root from the test working directory");
        return current;
    }

}
