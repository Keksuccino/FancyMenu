package de.keksuccino.fancymenu.customization.element.scrolling;

import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * The entries the layout editor shows for the {@link ScrollSettings} of a scrollable element.<br>
 * The names of the entries are the same for every element. The descriptions that say what gets scrolled come from the keys of the element.
 */
public final class ScrollSettingsMenus {

    /** The key the texts start with that are the same for all elements. */
    private static final String SHARED_KEY_BASE = "fancymenu.elements.scroll_area";

    private ScrollSettingsMenus() {
    }

    /**
     * Adds the entry of the direction the content scrolls in.
     */
    public static <H extends PropertyHolder> void addScrollDirectionEntry(@NotNull ContextMenu addTo, @NotNull ContextMenuBuilder<H> builder, @NotNull ScrollSettings settings) {
        settings.scrollDirection.buildContextMenuEntryAndAddTo(addTo, builder)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(settings.getDescriptionKeyBase() + ".scroll_direction.desc")))
                .setIcon(MaterialIcons.SWAP_VERT);
    }

    /**
     * Adds the menu of everything the user scrolls the content with, and of where the content starts.
     */
    public static <H extends PropertyHolder> void addScrollingMenu(@NotNull ContextMenu addTo, @NotNull ContextMenuBuilder<H> builder, @NotNull ScrollSettings settings) {

        String descriptionKeyBase = settings.getDescriptionKeyBase();

        ContextMenu scrollingMenu = new ContextMenu();
        addTo.addSubMenuEntry("scrolling", Component.translatable(SHARED_KEY_BASE + ".scrolling"), scrollingMenu)
                .setStackable(true)
                .setIcon(MaterialIcons.MOUSE);

        settings.userScrolling.buildContextMenuEntryAndAddTo(scrollingMenu, builder)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(descriptionKeyBase + ".scrolling.user.desc")))
                .setIcon(MaterialIcons.TOUCH_APP);

        scrollingMenu.addSeparatorEntry("separator_after_user_scrolling").setStackable(true);

        settings.scrollSpeed.buildContextMenuEntryAndAddTo(scrollingMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> settings.userScrolling.getBoolean())
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(SHARED_KEY_BASE + ".scroll_speed.desc")))
                .setIcon(MaterialIcons.SPEED);

        settings.smoothScrolling.buildContextMenuEntryAndAddTo(scrollingMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.ANIMATION);

        settings.dragScrolling.buildContextMenuEntryAndAddTo(scrollingMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> settings.userScrolling.getBoolean())
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(descriptionKeyBase + ".scrolling.drag.desc")))
                .setIcon(MaterialIcons.PAN_TOOL);

        settings.keyboardScrolling.buildContextMenuEntryAndAddTo(scrollingMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> settings.userScrolling.getBoolean())
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(descriptionKeyBase + ".scrolling.keyboard.desc")))
                .setIcon(MaterialIcons.KEYBOARD);

        settings.snapMode.buildContextMenuEntryAndAddTo(scrollingMenu, builder)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(descriptionKeyBase + ".scrolling.snap.desc")))
                .setIcon(MaterialIcons.VIEW_CAROUSEL);

        scrollingMenu.addSeparatorEntry("separator_before_start_position").setStackable(true);

        settings.startPosition.buildContextMenuEntryAndAddTo(scrollingMenu, builder)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(descriptionKeyBase + ".scrolling.start_position.desc")))
                .setIcon(MaterialIcons.START);

        settings.resetScrollOnOpen.buildContextMenuEntryAndAddTo(scrollingMenu, builder)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(descriptionKeyBase + ".scrolling.reset_on_open.desc")))
                .setIcon(MaterialIcons.RESTART_ALT);

    }

    /**
     * Adds the menu of the content scrolling on its own.
     */
    public static <H extends PropertyHolder> void addAutoScrollMenu(@NotNull ContextMenu addTo, @NotNull ContextMenuBuilder<H> builder, @NotNull ScrollSettings settings) {

        ContextMenu autoScrollMenu = new ContextMenu();
        addTo.addSubMenuEntry("auto_scroll", Component.translatable(SHARED_KEY_BASE + ".auto_scroll"), autoScrollMenu)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(settings.getDescriptionKeyBase() + ".auto_scroll.desc")))
                .setIcon(MaterialIcons.PLAY_ARROW);

        settings.autoScroll.buildContextMenuEntryAndAddTo(autoScrollMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.PLAY_ARROW);

        settings.autoScrollSpeed.buildContextMenuEntryAndAddTo(autoScrollMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> settings.autoScroll.getBoolean())
                .setIcon(MaterialIcons.SPEED);

        settings.autoScrollLoop.buildContextMenuEntryAndAddTo(autoScrollMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> settings.autoScroll.getBoolean())
                .setIcon(MaterialIcons.REPEAT);

        settings.autoScrollPauseOnHover.buildContextMenuEntryAndAddTo(autoScrollMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> settings.autoScroll.getBoolean())
                .setIcon(MaterialIcons.MOUSE);

    }

    /**
     * Adds the menu of the scroll bars, with the entries for when they show, how thick they are and where they sit.
     *
     * @return The menu, for the entries of what the grabbers and the bars of the element look like.
     */
    @NotNull
    public static <H extends PropertyHolder> ContextMenu addScrollBarsMenu(@NotNull ContextMenu addTo, @NotNull ContextMenuBuilder<H> builder, @NotNull ScrollSettings settings) {

        ContextMenu scrollBarsMenu = new ContextMenu();
        addTo.addSubMenuEntry("scroll_bars", Component.translatable(SHARED_KEY_BASE + ".scroll_bars"), scrollBarsMenu)
                .setStackable(true)
                .setIcon(MaterialIcons.SWIPE_VERTICAL);

        settings.scrollBarVisibility.buildContextMenuEntryAndAddTo(scrollBarsMenu, builder)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(SHARED_KEY_BASE + ".scroll_bars.visibility.desc")))
                .setIcon(MaterialIcons.VISIBILITY);

        settings.scrollBarThickness.buildContextMenuEntryAndAddTo(scrollBarsMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.LINE_WEIGHT);

        settings.verticalScrollBarLeft.buildContextMenuEntryAndAddTo(scrollBarsMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.ALIGN_HORIZONTAL_LEFT);

        settings.horizontalScrollBarTop.buildContextMenuEntryAndAddTo(scrollBarsMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.ALIGN_VERTICAL_TOP);

        scrollBarsMenu.addSeparatorEntry("separator_before_grabber_looks").setStackable(true);

        return scrollBarsMenu;

    }

}
