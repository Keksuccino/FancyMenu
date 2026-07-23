package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class IsElementFocusedRequirementTest {

    @Test
    void descriptionUsesCanonicalTranslationKey() {
        Component description = new IsElementFocusedRequirement().getDescription();

        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, description.getContents());
        assertEquals("fancymenu.requirements.is_element_focused.desc", contents.getKey());
    }

}
