package de.keksuccino.fancymenu.util.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void removesOnlyTheLeadingSlashProducedForFolderRootEnumeration() {
        assertEquals("", PackResourcesRootEnumeration.normalizeFolderPrefix("/", ""));
        assertEquals("textures/", PackResourcesRootEnumeration.normalizeFolderPrefix("textures/", "textures"));
    }
}
