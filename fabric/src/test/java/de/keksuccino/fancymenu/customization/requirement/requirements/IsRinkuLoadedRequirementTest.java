package de.keksuccino.fancymenu.customization.requirement.requirements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.rinku.RinkuVideoMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.nativevideo.NativeVideoMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.element.elements.video.rinku.RinkuVideoElementBuilder;
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

class IsRinkuLoadedRequirementTest {

    @Test
    void englishHelpDistinguishesRinkuFeaturesFromNativeVideo() throws IOException {
        JsonObject localizations = readEnglishLocalizations();

        assertEquals("Checks if the Rinku mod is loaded.\n\nRinku is needed for Browser features and the deprecated\nRinku-based Video element and menu background. Native Video\nfeatures use Watermedia instead.", localizations.get("fancymenu.requirements.is_rinku_loaded.desc").getAsString());
    }

    @Test
    void requirementUsesRinkuIdentifiers() {
        IsRinkuLoadedRequirement requirement = new IsRinkuLoadedRequirement();

        assertEquals("is_rinku_loaded", requirement.getIdentifier());
        TranslatableContents displayName = assertInstanceOf(TranslatableContents.class, requirement.getDisplayName().getContents());
        TranslatableContents description = assertInstanceOf(TranslatableContents.class, requirement.getDescription().getContents());
        assertEquals("fancymenu.requirements.is_rinku_loaded", displayName.getKey());
        assertEquals("fancymenu.requirements.is_rinku_loaded.desc", description.getKey());
    }

    @Test
    void onlyRinkuVideoBuildersAreDeprecated() {
        NativeVideoElementBuilder nativeElement = new NativeVideoElementBuilder();
        RinkuVideoElementBuilder rinkuElement = new RinkuVideoElementBuilder();
        NativeVideoMenuBackgroundBuilder nativeBackground = new NativeVideoMenuBackgroundBuilder();
        RinkuVideoMenuBackgroundBuilder rinkuBackground = new RinkuVideoMenuBackgroundBuilder();

        assertEquals("video", nativeElement.getIdentifier());
        assertFalse(nativeElement.isDeprecated());
        assertEquals("video_rinku", rinkuElement.getIdentifier());
        assertTrue(rinkuElement.isDeprecated());
        assertEquals("video", nativeBackground.getIdentifier());
        assertFalse(nativeBackground.isDeprecated());
        assertEquals("video_rinku", rinkuBackground.getIdentifier());
        assertTrue(rinkuBackground.isDeprecated());
    }

    private static JsonObject readEnglishLocalizations() throws IOException {
        try (InputStream stream = IsRinkuLoadedRequirementTest.class.getResourceAsStream("/assets/fancymenu/lang/en_us.json")) {
            assertNotNull(stream, "English localization resource is missing from the test runtime classpath");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

}
