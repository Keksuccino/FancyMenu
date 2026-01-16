package de.keksuccino.fancymenu.util.rendering.text.smooth;

import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SmoothFonts {

    private static final float SMOOTH_FONT_BASE_SIZE = 32.0F;

    public static final float DEFAULT_TEXT_SIZE = 11F;

    public static final Supplier<SmoothFont> NOTO_SANS = () -> {
        ResourceLocation folder = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans");
        ResourceLocation base = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans.ttf");
        ResourceLocation jp   = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_jp.ttf");
        ResourceLocation kr   = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_kr.ttf");
        ResourceLocation sc   = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_sc.ttf");
        ResourceLocation tc   = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_tc.ttf");
        Map<String, List<ResourceLocation>> overrides = Map.of(
                "ja_jp", List.of(base, jp, sc, tc, kr),
                "ko_kr", List.of(base, kr, sc, jp, tc),
                "zh_cn", List.of(base, sc, tc, jp, kr),
                "zh_tw", List.of(base, tc, sc, jp, kr),
                "zh_hk", List.of(base, tc, sc, jp, kr)
        );
        return SmoothFontManager.getFontFromFolder(folder, SMOOTH_FONT_BASE_SIZE, overrides);
    };

}
