package de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScrollScreenNormalizer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<ScrollableScreenBlacklistRule> SCROLLABLE_SCREEN_BLACKLIST = new ArrayList<>();

    static {

        addScrollableScreenBlacklistRule(screen -> (screen instanceof SelectWorldScreen));
        addScrollableScreenBlacklistRule(screen -> (screen instanceof JoinMultiplayerScreen));
        addScrollableScreenBlacklistRule(screen -> (screen instanceof PackSelectionScreen));
        addScrollableScreenBlacklistRule(screen -> (screen instanceof CustomGuiBaseScreen));

    }

    @NotNull
    public static Screen normalizeScrollableScreen(@NotNull Screen screen) {

        if (isBlacklisted(screen)) return screen;
        if (!ScrollScreenNormalizerHandler.shouldNormalize(screen)) return screen;
        if (!ScreenCustomization.isCustomizationEnabledForScreen(screen)) return screen;

        List<AbstractWidget> extracted = extractAllWidgetsFromScrollListsOfScreen(screen);
        IMixinScreen accessor = ((IMixinScreen)screen);

        extractAllScrollListsOfScreen(screen).forEach(scroll -> {
            scroll.updateSizeAndPosition(1000000, 1000000, 0);
            accessor.getChildrenFancyMenu().remove(scroll);
            accessor.getNarratablesFancyMenu().remove(scroll);
            accessor.getRenderablesFancyMenu().remove(scroll);
        });
        extracted.forEach(widget -> {
            accessor.getChildrenFancyMenu().remove(widget);
            accessor.getNarratablesFancyMenu().remove(widget);
            accessor.getRenderablesFancyMenu().remove(widget);
        });

        int pos = 50;
        Map<String, Integer> ids = new HashMap<>();
        for (AbstractWidget widget : extracted) {

            widget.setX(pos);
            widget.setY(pos);
            pos++;

            if (screen instanceof OptionsSubScreen) {
                if (widget instanceof UniqueWidget w) {
                    StringBuilder id = new StringBuilder("options");
                    buildId(widget.getMessage().getContents(), id);
                    String idString = id.toString();
                    if (idString.equals("options")) idString += "_generic";
                    if (ids.containsKey(idString)) {
                        int count = ids.get(idString);
                        count++;
                        ids.put(idString, count);
                        idString = idString + "_" + count;
                    } else {
                        ids.put(idString, 0);
                    }
                    w.setWidgetIdentifierFancyMenu(idString);
                }
            }

            accessor.getChildrenFancyMenu().add(widget);
            accessor.getRenderablesFancyMenu().add(widget);
            accessor.getNarratablesFancyMenu().add(widget);

        }

        return screen;

    }

    @NotNull
    private static StringBuilder buildId(@NotNull ComponentContents contents, @NotNull StringBuilder builder) {
        if (contents instanceof TranslatableContents t) {
            builder.append("_");
            Object[] args = t.getArgs();
            if (args.length > 0) {
                if (args[0] instanceof MutableComponent c) {
                    if (c.getContents() instanceof TranslatableContents t2) {
                        builder.append(t2.getKey());
                        return builder;
                    }
                }
            }
            builder.append(t.getKey());
        }
        return builder;
    }

    public static boolean isBlacklisted(Screen screen) {
        if (screen == null) return false;
        for (ScrollableScreenBlacklistRule e : SCROLLABLE_SCREEN_BLACKLIST) {
            if (e.isBlacklisted(screen)) return true;
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
                        extractWidgetsRecursively(h, list);
                    }
                });
            }
        }
        return list;
    }

    private static void extractWidgetsRecursively(@NotNull ContainerEventHandler container, @NotNull List<AbstractWidget> list) {
        container.children().forEach(child -> {
            if (child instanceof ContainerEventHandler childContainer) {
                // Recursively extract widgets from nested containers
                extractWidgetsRecursively(childContainer, list);
            } else if (child instanceof AbstractWidget widget) {
                list.add(widget);
            }
        });
    }

    public static void addScrollableScreenBlacklistRule(@NotNull ScrollScreenNormalizer.ScrollableScreenBlacklistRule rule) {
        SCROLLABLE_SCREEN_BLACKLIST.add(rule);
    }

    @FunctionalInterface
    public interface ScrollableScreenBlacklistRule {
        boolean isBlacklisted(@NotNull Screen screen);
    }

}
