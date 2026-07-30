package de.keksuccino.fancymenu.util.resource;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

class PackResourcesRootEnumerationTest {

    @Test
    void removesOnlyTheExtraSlashProducedForArchiveRootEnumeration() {
        assertEquals("assets/example/", PackResourcesRootEnumeration.normalizeArchivePrefix("assets/example//", ""));
        assertEquals("overlays/modern/assets/example/", PackResourcesRootEnumeration.normalizeArchivePrefix("overlays/modern/assets/example//", ""));
    }

    @Test
    void leavesNonRootAndAlreadyValidArchivePrefixesUnchanged() {
        assertEquals("assets/example/textures/", PackResourcesRootEnumeration.normalizeArchivePrefix("assets/example/textures/", "textures"));
        assertEquals("assets/example/", PackResourcesRootEnumeration.normalizeArchivePrefix("assets/example/", ""));
    }

    @Test
    void representsTheNamespaceRootWithNoPathSegments() {
        DataResult<List<String>> result = PackResourcesRootEnumeration.decomposeDirectory("", args -> fail("Vanilla decomposition must not receive its invalid empty path"));

        assertEquals(List.of(), result.result().orElseThrow());
    }

    @Test
    void delegatesNonRootDirectoriesUnchanged() {
        DataResult<List<String>> expected = DataResult.success(List.of("textures", "gui"));
        Operation<DataResult<List<String>>> original = args -> {
            assertEquals(List.of("textures/gui"), List.of(args));
            return expected;
        };

        assertSame(expected, PackResourcesRootEnumeration.decomposeDirectory("textures/gui", original));
    }

    @Test
    void enumeratesRootAndNestedFilesFromAPathBackedNamespace(@TempDir Path packRoot) throws Exception {
        Path namespaceRoot = packRoot.resolve("assets/example");
        Files.createDirectories(namespaceRoot.resolve("textures/gui"));
        Files.writeString(namespaceRoot.resolve("root.txt"), "root");
        Files.writeString(namespaceRoot.resolve("textures/gui/button.png"), "image");
        Set<ResourceLocation> locations = new LinkedHashSet<>();

        PackResourcesRootEnumeration.listPathNamespaceRoot(packRoot, PackType.CLIENT_RESOURCES, "example", (location, streamSupplier) -> locations.add(location));

        assertEquals(Set.of(new ResourceLocation("example", "root.txt"), new ResourceLocation("example", "textures/gui/button.png")), locations);
    }

}
