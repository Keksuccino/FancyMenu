package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class ScrollScreenNormalizer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<ScrollableScreenEvaluator> SCROLLABLE_SCREENS = new ArrayList<>();

    static {

        addScrollableScreenEvaluator(screen -> screen instanceof OptionsSubScreen);

    }

    @NotNull
    public static Screen normalizeScrollableScreen(@NotNull Screen screen) {

        if (!isScrollableScreen(screen)) return screen;

        List<AbstractWidget> extracted = extractAllWidgetsFromScrollListsOfScreen(screen);

        IMixinScreen accessor = ((IMixinScreen)screen);

        extractAllScrollListsOfScreen(screen).forEach(scroll -> {
            accessor.getChildrenFancyMenu().remove(scroll);
            accessor.getNarratablesFancyMenu().remove(scroll);
            accessor.getRenderablesFancyMenu().remove(scroll);
        });
        extracted.forEach(widget -> {
            accessor.getChildrenFancyMenu().remove(widget);
            accessor.getNarratablesFancyMenu().remove(widget);
            accessor.getRenderablesFancyMenu().remove(widget);
        });

        extracted.forEach(widget -> {

            widget.setX(50);
            widget.setY(50);

            if (screen instanceof OptionsSubScreen) {
                if (widget instanceof UniqueWidget w) {
                    if (widget.getMessage().getContents() instanceof TranslatableContents contents) {
                        w.setWidgetIdentifierFancyMenu("options_" + contents.getKey());
                        LOGGER.info("################ WIDGET FOUND IN SCROLL LIST: " + contents.getKey());
                    }
                }
            }

            accessor.getChildrenFancyMenu().add(widget);
            accessor.getRenderablesFancyMenu().add(widget);
            accessor.getNarratablesFancyMenu().add(widget);

        });

        return screen;

    }

    public static boolean isScrollableScreen(@NotNull Screen screen) {
        for (ScrollableScreenEvaluator e : SCROLLABLE_SCREENS) {
            if (e.isScrollable(screen)) return true;
        }
        for (Object o : screen.children()) {
            if (o instanceof AbstractSelectionList<?>) return true;
        }
        return false;
    }

    @NotNull
    public static List<AbstractSelectionList<?>> extractAllScrollListsOfScreen(@NotNull Screen screen) {
        List<AbstractSelectionList<?>> list = new ArrayList<>();
        screen.children().forEach(o -> {
            if (o instanceof AbstractSelectionList<?> l) list.add(l);
        });
        return list;
    }

    @NotNull
    public static List<AbstractWidget> extractAllWidgetsFromScrollListsOfScreen(@NotNull Screen screen) {
        List<AbstractWidget> list = new ArrayList<>();
        for (Object o : ((IMixinScreen)screen).getChildrenFancyMenu()) {
            if (o instanceof AbstractSelectionList<?> sel) {
                sel.children().forEach(entry -> {
                    if (entry instanceof ContainerEventHandler h) {
                        h.children().forEach(widget -> {
                            if (widget instanceof AbstractWidget w) list.add(w);
                        });
                    }
                });
            }
        }
        return list;
    }

    public static void addScrollableScreenEvaluator(@NotNull ScrollableScreenEvaluator evaluator) {
        SCROLLABLE_SCREENS.add(evaluator);
    }

    @FunctionalInterface
    public interface ScrollableScreenEvaluator {
        boolean isScrollable(@NotNull Screen screen);
    }

}
