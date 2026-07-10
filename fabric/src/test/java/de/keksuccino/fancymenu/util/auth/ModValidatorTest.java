package de.keksuccino.fancymenu.util.auth;

import de.keksuccino.fancymenu.util.mod.UniversalModContainer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModValidatorTest {

    private static final String DESCRIPTION = "Customize Minecraft's menus with ease!";
    private static final String LICENSE = "DSMSLv3 (DON'T SNATCH MA STUFF LICENSE v3)";

    @Test
    void acceptsUnmodifiedValidatedMetadata() {
        assertTrue(ModValidator.isFancyMenuMetadataValid(mod("FancyMenu", DESCRIPTION, LICENSE)));
    }

    @Test
    void rejectsRuntimeDisplayNameMutation() {
        assertFalse(ModValidator.isFancyMenuMetadataValid(mod("A Custom Name", DESCRIPTION, LICENSE)));
    }

    @Test
    void rejectsPrefixedDescriptionMutation() {
        assertFalse(ModValidator.isFancyMenuMetadataValid(mod("FancyMenu", "Modified: " + DESCRIPTION, LICENSE)));
    }

    @Test
    void rejectsAppendedDescriptionMutation() {
        assertFalse(ModValidator.isFancyMenuMetadataValid(mod("FancyMenu", DESCRIPTION + " Modified", LICENSE)));
    }

    @Test
    void rejectsPrefixedLicenseMutation() {
        assertFalse(ModValidator.isFancyMenuMetadataValid(mod("FancyMenu", DESCRIPTION, "Modified: " + LICENSE)));
    }

    @Test
    void rejectsAppendedLicenseMutation() {
        assertFalse(ModValidator.isFancyMenuMetadataValid(mod("FancyMenu", DESCRIPTION, LICENSE + " Modified")));
    }

    @Test
    void rejectsMissingModMetadata() {
        assertFalse(ModValidator.isFancyMenuMetadataValid(null));
    }

    private static UniversalModContainer mod(String name, String description, String license) {
        return new UniversalModContainer("fancymenu", name, description, license, List.of("Keksuccino"));
    }
}
