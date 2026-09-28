package de.keksuccino.fancymenu.util.rendering.text.smooth;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SmoothFonts {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final float SMOOTH_FONT_BASE_SIZE = 32.0F;
    private static final ResourceLocation NOTO_SANS_FOLDER = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans");
    private static final ResourceLocation NOTO_SANS_BASE = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans.ttf");
    private static final ResourceLocation NOTO_SANS_JP = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_jp.ttf");
    private static final ResourceLocation NOTO_SANS_KR = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_kr.ttf");
    private static final ResourceLocation NOTO_SANS_SC = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_sc.ttf");
    private static final ResourceLocation NOTO_SANS_TC = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_tc.ttf");
    private static final ResourceLocation NOTO_SANS_EMOJI = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_emoji.ttf");
    private static final Map<String, List<ResourceLocation>> NOTO_SANS_ORDER_OVERRIDES = Map.of(
            "ja_jp", List.of(NOTO_SANS_BASE, NOTO_SANS_JP, NOTO_SANS_SC, NOTO_SANS_TC, NOTO_SANS_KR, NOTO_SANS_EMOJI),
            "ko_kr", List.of(NOTO_SANS_BASE, NOTO_SANS_KR, NOTO_SANS_SC, NOTO_SANS_JP, NOTO_SANS_TC, NOTO_SANS_EMOJI),
            "zh_cn", List.of(NOTO_SANS_BASE, NOTO_SANS_SC, NOTO_SANS_TC, NOTO_SANS_JP, NOTO_SANS_KR, NOTO_SANS_EMOJI),
            "zh_tw", List.of(NOTO_SANS_BASE, NOTO_SANS_TC, NOTO_SANS_SC, NOTO_SANS_JP, NOTO_SANS_KR, NOTO_SANS_EMOJI),
            "zh_hk", List.of(NOTO_SANS_BASE, NOTO_SANS_TC, NOTO_SANS_SC, NOTO_SANS_JP, NOTO_SANS_KR, NOTO_SANS_EMOJI)
    );

    public static final float DEFAULT_TEXT_SIZE = 10F;

    private static final FallbackFontProvider<SmoothFont> NOTO_SANS_PROVIDER = new FallbackFontProvider<>(SmoothFonts::loadNotoSans);
    public static final Supplier<SmoothFont> NOTO_SANS = NOTO_SANS_PROVIDER;

    @Nullable
    private static SmoothFont loadNotoSans() {
        // Register before looking for resources: an entirely missing font folder must also recover after reload.
        SmoothFontManager.registerReloadListener();
        SmoothFont font = SmoothFontManager.fontBuilderFromFolder(NOTO_SANS_FOLDER, SMOOTH_FONT_BASE_SIZE).languageOverrides(NOTO_SANS_ORDER_OVERRIDES).yOffset(-10).lineHeightOffset(-20).build();
        if (font == null) {
            LOGGER.warn("[FANCYMENU] Failed to load the UI font. Using the Minecraft font until the next resource reload. Check the preceding font errors and your Java/system font configuration.");
        }
        return font;
    }

    public static boolean shouldUseMinecraftFont(boolean preferMinecraftFont) {
        return NOTO_SANS_PROVIDER.shouldUseFallback(preferMinecraftFont);
    }

    public static void clearCache() {
        NOTO_SANS_PROVIDER.clear();
    }

}
