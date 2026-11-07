package de.keksuccino.fancymenu.util.rendering.text.smooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Builds smooth fonts from the bundled font files, without the Minecraft resource manager.
 */
final class TestFonts {

    static final String BASE = "noto_sans.ttf";
    static final String JAPANESE = "noto_sans_jp.ttf";
    static final String COLOR_EMOJI = "noto_color_emoji.ttf";
    static final String MONO_EMOJI = "noto_sans_emoji.ttf";

    private static final String FOLDER = "assets/fancymenu/fonts/noto_sans/";

    private TestFonts() {
    }

    static byte[] bytes(String file) {
        try (InputStream in = TestFonts.class.getClassLoader().getResourceAsStream(FOLDER + file)) {
            if (in == null) {
                throw new IllegalStateException("Missing test font: " + file);
            }
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Builds a font from the given files; files containing "emoji" become emoji fallbacks.
     *
     * @param loads counts how often each file got loaded, by file index
     */
    static SmoothFont font(String languageCode, Map<String, int[]> languageOrders, AtomicIntegerArray loads, String... files) {
        List<SmoothFontSource> sources = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            String file = files[i];
            int index = i;
            sources.add(new SmoothFontSource(file, i, file.contains("emoji"), () -> {
                loads.incrementAndGet(index);
                return bytes(file);
            }));
        }
        return new SmoothFont("test", sources, 32.0F, -1.0F, 0.0F, 0.0F, languageOrders, 0.86F, () -> languageCode);
    }

    static SmoothFont font(String... files) {
        return font("en_us", null, new AtomicIntegerArray(files.length), files);
    }

}
