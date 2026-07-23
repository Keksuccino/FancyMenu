package de.keksuccino.fancymenu.platform.services;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Adapts a reversible indexed branding source to FancyMenu's top-to-bottom title-screen contract.
 *
 * <p>Loader title screens can request reversed enumeration when they position callback index zero at the bottom.
 * FancyMenu instead positions list index zero at the top, so the source must include Minecraft's line and use its
 * natural order.</p>
 */
public final class TitleScreenBrandingLineCollector {

    private TitleScreenBrandingLineCollector() {
    }

    public static <S, T> List<T> collectTopToBottom(@Nonnull ReversibleLineSource<S> source, @Nonnull Function<? super S, ? extends T> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        List<T> lines = new ArrayList<>();
        source.forEachLine(true, false, (lineIndex, line) -> lines.add(mapper.apply(line)));
        return List.copyOf(lines);
    }

    @FunctionalInterface
    public interface ReversibleLineSource<T> {

        void forEachLine(boolean includeMinecraft, boolean reverse, @Nonnull BiConsumer<Integer, T> lineConsumer);

    }

}
