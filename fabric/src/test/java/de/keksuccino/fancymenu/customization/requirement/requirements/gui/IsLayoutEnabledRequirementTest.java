package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.layout.Layout;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsLayoutEnabledRequirementTest {

    @Test
    void matchesLaterEnabledLayoutWhenFirstLayoutHasDifferentName() {
        List<Layout> enabledLayouts = List.of(layout("first_layout.txt"), layout("target_layout.txt"));

        assertTrue(IsLayoutEnabledRequirement.hasLayoutNamed("target_layout", enabledLayouts));
    }

    @Test
    void returnsFalseWhenNoEnabledLayoutMatches() {
        List<Layout> enabledLayouts = List.of(layout("first_layout.txt"), layout("second_layout.txt"));

        assertFalse(IsLayoutEnabledRequirement.hasLayoutNamed("target_layout", enabledLayouts));
    }

    @Test
    void skipsLayoutsWithoutBackingFilesWhileSearching() {
        List<Layout> enabledLayouts = List.of(layout(null), layout("target_layout.txt"));

        assertTrue(IsLayoutEnabledRequirement.hasLayoutNamed("target_layout", enabledLayouts));
    }

    @Test
    void returnsFalseForNullRequirementValue() {
        assertFalse(IsLayoutEnabledRequirement.hasLayoutNamed(null, List.of(layout("target_layout.txt"))));
    }

    @Test
    void returnsFalseWhenThereAreNoEnabledLayouts() {
        assertFalse(IsLayoutEnabledRequirement.hasLayoutNamed("target_layout", List.of()));
    }

    private static Layout layout(@Nullable String fileName) {
        Layout layout = new Layout();
        if (fileName != null) layout.layoutFile = new File(fileName);
        return layout;
    }

}
