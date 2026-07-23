package de.keksuccino.fancymenu.customization.requirement.requirements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.mcef.MCEFVideoMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.nativevideo.NativeVideoMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.element.elements.video.mcef.MCEFVideoElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.video.nativevideo.NativeVideoElementBuilder;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsMcefLoadedRequirementTest {

    @Test
    void englishHelpDistinguishesMcefFeaturesFromNativeVideo() throws IOException {
        JsonObject localizations = readEnglishLocalizations();

        assertEquals("Checks if the MCEF mod is loaded.\n\nMCEF is needed for Browser features and the deprecated\nMCEF-based Video element and menu background. Native Video\nfeatures use Watermedia instead.", localizations.get("fancymenu.requirements.is_mcef_loaded.desc").getAsString());
    }

    @Test
    void requirementIdentifiersRemainBackwardCompatible() {
        IsMcefLoadedRequirement requirement = new IsMcefLoadedRequirement();

        assertEquals("is_mcef_loaded", requirement.getIdentifier());
        TranslatableContents displayName = assertInstanceOf(TranslatableContents.class, requirement.getDisplayName().getContents());
        TranslatableContents description = assertInstanceOf(TranslatableContents.class, requirement.getDescription().getContents());
        assertEquals("fancymenu.requirements.is_mcef_loaded", displayName.getKey());
        assertEquals("fancymenu.requirements.is_mcef_loaded.desc", description.getKey());
    }

    @Test
    void onlyMcefVideoBuildersAreDeprecated() {
        NativeVideoElementBuilder nativeElement = new NativeVideoElementBuilder();
        MCEFVideoElementBuilder mcefElement = new MCEFVideoElementBuilder();
        NativeVideoMenuBackgroundBuilder nativeBackground = new NativeVideoMenuBackgroundBuilder();
        MCEFVideoMenuBackgroundBuilder mcefBackground = new MCEFVideoMenuBackgroundBuilder();

        assertEquals("video", nativeElement.getIdentifier());
        assertFalse(nativeElement.isDeprecated());
        assertEquals("video_mcef", mcefElement.getIdentifier());
        assertTrue(mcefElement.isDeprecated());
        assertEquals("video", nativeBackground.getIdentifier());
        assertFalse(nativeBackground.isDeprecated());
        assertEquals("video_mcef", mcefBackground.getIdentifier());
        assertTrue(mcefBackground.isDeprecated());
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = IsMcefLoadedRequirementTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
