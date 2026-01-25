package de.keksuccino.fancymenu.util.rendering.text.smooth;

import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SmoothFonts {

    private static final float SMOOTH_FONT_BASE_SIZE = 32.0F;
    private static final Object NOTO_SANS_LOCK = new Object();
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

    public static final Supplier<SmoothFont> NOTO_SANS = SmoothFonts::getNotoSans;

    private static volatile SmoothFont cachedNotoSans;

    @Nullable
    private static SmoothFont getNotoSans() {
        SmoothFont cached = cachedNotoSans;
        if (cached != null) {
            return cached;
        }
        synchronized (NOTO_SANS_LOCK) {
            cached = cachedNotoSans;
            if (cached != null) {
                return cached;
            }
            SmoothFont created = SmoothFontManager.getFontFromFolder(NOTO_SANS_FOLDER, SMOOTH_FONT_BASE_SIZE, NOTO_SANS_ORDER_OVERRIDES, 0.8F);
            if (created != null) {
                cachedNotoSans = created;
            }
            return created;
        }
    }

    public static void clearCache() {
        cachedNotoSans = null;
    }

}
