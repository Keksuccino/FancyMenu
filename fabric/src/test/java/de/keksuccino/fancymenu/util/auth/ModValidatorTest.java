package de.keksuccino.fancymenu.util.auth;

import de.keksuccino.fancymenu.util.mod.UniversalModContainer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModValidatorTest {

    private static final String DESCRIPTION = "Customize Minecraft's menus with ease!";
    private static final String LICENSE = "DSMSLv3 (DON'T SNATCH MA STUFF LICENSE v3)";

    @Test
    void acceptsRuntimeDisplayNameOverrideWhenDeclaredMetadataIsValid() {
        UniversalModContainer mod = mod("A Custom Name", DESCRIPTION, LICENSE);

        assertTrue(ModValidator.isFancyMenuMetadataValid(mod, "FancyMenu"));
    }

    @Test
    void rejectsModifiedDeclaredDisplayNameEvenWhenRuntimeNameLooksValid() {
        UniversalModContainer mod = mod("FancyMenu", DESCRIPTION, LICENSE);

        assertFalse(ModValidator.isFancyMenuMetadataValid(mod, "Modified Name"));
    }

    @Test
    void rejectsMissingDeclaredDisplayNameFallback() {
        UniversalModContainer mod = mod("FancyMenu", DESCRIPTION, LICENSE);

        assertFalse(ModValidator.isFancyMenuMetadataValid(mod, "fancymenu"));
    }

    @Test
    void rejectsModifiedDescription() {
        UniversalModContainer mod = mod("FancyMenu", "Modified description", LICENSE);

        assertFalse(ModValidator.isFancyMenuMetadataValid(mod, "FancyMenu"));
    }

    @Test
    void rejectsModifiedLicense() {
        UniversalModContainer mod = mod("FancyMenu", DESCRIPTION, "Modified license");

        assertFalse(ModValidator.isFancyMenuMetadataValid(mod, "FancyMenu"));
    }

    @Test
    void preservesUniversalModContainerRecordShape() {
        assertArrayEquals(new String[]{"id", "name", "description", "license", "authors"}, List.of(UniversalModContainer.class.getRecordComponents()).stream().map(component -> component.getName()).toArray(String[]::new));
    }

    private static UniversalModContainer mod(String name, String description, String license) {
        return new UniversalModContainer("fancymenu", name, description, license, List.of("Keksuccino"));
    }
}
