package de.keksuccino.fancymenu.customization.requirement.requirements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OncePerSessionRequirementTest {

    @Test
    void englishTextDescribesPerInstanceScope() throws IOException {
        JsonObject localizations = readEnglishLocalizations();

        assertEquals("Only Once Per Session", localizations.get("fancymenu.requirements.once_per_session").getAsString());
        assertEquals("Each instance of this requirement returns true once per game session.\nAfter an instance returns true, it will return false\nuntil the game is restarted.", localizations.get("fancymenu.requirements.once_per_session.desc").getAsString());
    }

    @Test
    void requirementIdentifiersRemainBackwardCompatible() {
        OncePerSessionRequirement requirement = new OncePerSessionRequirement();

        assertEquals("once_per_session", requirement.getIdentifier());
        TranslatableContents displayName = assertInstanceOf(TranslatableContents.class, requirement.getDisplayName().getContents());
        TranslatableContents description = assertInstanceOf(TranslatableContents.class, requirement.getDescription().getContents());
        assertEquals("fancymenu.requirements.once_per_session", displayName.getKey());
        assertEquals("fancymenu.requirements.once_per_session.desc", description.getKey());
    }

    @Test
    void eachRequirementInstanceIsAdmittedIndependently() {
        OncePerSessionRequirement requirement = new OncePerSessionRequirement();
        RequirementContainer parent = new RequirementContainer();
        RequirementInstance first = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, parent);
        RequirementInstance second = new RequirementInstance(requirement, null, RequirementInstance.RequirementMode.IF, parent);

        assertNotEquals(first.instanceIdentifier, second.instanceIdentifier);
        requirement.setCurrentInstance(first);
        assertTrue(requirement.isRequirementMet(null));
        assertFalse(requirement.isRequirementMet(null));
        requirement.setCurrentInstance(second);
        assertTrue(requirement.isRequirementMet(null));
        assertFalse(requirement.isRequirementMet(null));
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = OncePerSessionRequirementTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
