package de.keksuccino.fancymenu.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MixinPackageStructureTest {

    private static final Pattern MIXIN_ANNOTATION_PATTERN = Pattern.compile("@Mixin\\s*\\(");
    private static final List<MixinSourceSet> MIXIN_SOURCE_SETS = List.of(new MixinSourceSet("common/src/main/resources/fancymenu.mixins.json", "common/src/main/java"), new MixinSourceSet("fabric/src/main/resources/fancymenu.fabric.mixins.json", "fabric/src/main/java"), new MixinSourceSet("forge/src/main/resources/fancymenu.forge.mixins.json", "forge/src/main/java"));

    @Test
    void ownedMixinPackagesContainOnlyConfiguredMixinClasses() throws IOException {
        Path repositoryRoot = findRepositoryRoot();
        for (MixinSourceSet sourceSet : MIXIN_SOURCE_SETS) verifyMixinSourceSet(repositoryRoot, sourceSet);
    }

    private static void verifyMixinSourceSet(Path repositoryRoot, MixinSourceSet sourceSet) throws IOException {
        Path configPath = repositoryRoot.resolve(sourceSet.configPath());
        JsonObject config;
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            config = JsonParser.parseReader(reader).getAsJsonObject();
        }

        String ownedPackage = config.get("package").getAsString();
        Path sourceRoot = repositoryRoot.resolve(sourceSet.javaSourcePath()).resolve(ownedPackage.replace('.', '/'));
        Set<String> configuredMixins = new TreeSet<>();
        addConfiguredMixins(config, "client", configuredMixins);
        addConfiguredMixins(config, "mixins", configuredMixins);
        addConfiguredMixins(config, "server", configuredMixins);

        Map<String, Path> topLevelSources = findTopLevelSources(sourceRoot);
        Set<String> invalidSources = new TreeSet<>();
        for (Map.Entry<String, Path> entry : topLevelSources.entrySet()) {
            String source = Files.readString(entry.getValue(), StandardCharsets.UTF_8);
            if (!configuredMixins.contains(entry.getKey()) || !MIXIN_ANNOTATION_PATTERN.matcher(source).find()) invalidSources.add(entry.getKey());
        }

        assertEquals(Set.of(), invalidSources, configPath + " owns this package recursively; it may only contain configured @Mixin classes because Mixin rejects direct loading of every other class below that root");
        assertEquals(configuredMixins, topLevelSources.keySet(), "Every mixin configured in " + configPath + " must have exactly one top-level source in its owned package");
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("common/src/main/resources/fancymenu.mixins.json"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate the FancyMenu repository root from the test working directory");
    }

    private static void addConfiguredMixins(JsonObject config, String key, Set<String> configuredMixins) {
        JsonArray entries = config.getAsJsonArray(key);
        if (entries == null) return;
        for (JsonElement entry : entries) configuredMixins.add(entry.getAsString());
    }

    private static Map<String, Path> findTopLevelSources(Path sourceRoot) throws IOException {
        Map<String, Path> sources = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            paths.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java")).forEach(path -> sources.put(toMixinClassName(sourceRoot, path), path));
        }
        return sources;
    }

    private static String toMixinClassName(Path sourceRoot, Path sourcePath) {
        String relativePath = sourceRoot.relativize(sourcePath).toString();
        return relativePath.substring(0, relativePath.length() - ".java".length()).replace(sourcePath.getFileSystem().getSeparator(), ".");
    }

    private record MixinSourceSet(String configPath, String javaSourcePath) {
    }
}
