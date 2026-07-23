package de.keksuccino.fancymenu.util.resource;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DataResult;
import org.junit.jupiter.api.Test;

import java.util.List;

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

}
