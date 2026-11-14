package de.keksuccino.fancymenu.customization.element.style;

import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * The entries the layout editor shows for the looks elements share: panel backgrounds, scroll grabbers and scroll bar backgrounds.
 */
public final class ElementStyleMenus {

    private ElementStyleMenus() {
    }

    /**
     * Adds the menu of a panel background.
     */
    public static <H extends PropertyHolder> void addPanelBackgroundMenu(@NotNull ContextMenu addTo, @NotNull ContextMenuBuilder<H> builder, @NotNull ElementPanelBackground background) {

        String sharedKeyBase = ElementPanelBackground.SHARED_LOCALIZATION_KEY_BASE;
        String descriptionKeyBase = background.getDescriptionKeyBase();

        ContextMenu backgroundMenu = new ContextMenu();
        addTo.addSubMenuEntry("background", Component.translatable(sharedKeyBase), backgroundMenu)
                .setStackable(true)
                .setIcon(MaterialIcons.WALLPAPER);

        background.enabled.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(descriptionKeyBase + ".enabled.desc")))
                .setIcon(MaterialIcons.WALLPAPER);

        background.padding.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(descriptionKeyBase + ".padding.desc")))
                .setIcon(MaterialIcons.PADDING_ICON);

        background.behindScrollBars.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(descriptionKeyBase + ".behind_scroll_bars.desc")))
                .setIcon(MaterialIcons.EXPAND_CONTENT);

        backgroundMenu.addSeparatorEntry("separator_before_background_texture").setStackable(true);

        background.texture.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(sharedKeyBase + ".texture.desc")))
                .setIcon(MaterialIcons.IMAGE);

        // The built-in panel always gets drawn the way it needs it, so everything below only does something for a texture of the user
        background.tile.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> background.texture.get() != null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(sharedKeyBase + ".tile.desc")))
                .setIcon(MaterialIcons.GRID_VIEW);

        background.nineSlice.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> background.texture.get() != null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(sharedKeyBase + ".nine_slice.desc")))
                .setIcon(MaterialIcons.GRID_GUIDES);

        background.nineSliceBorderX.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> background.texture.get() != null)
                .setIcon(MaterialIcons.BORDER_HORIZONTAL);

        background.nineSliceBorderY.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> background.texture.get() != null)
                .setIcon(MaterialIcons.BORDER_VERTICAL);

    }

    /**
     * Adds the menu of the backgrounds of scroll bars.
     */
    public static <H extends PropertyHolder> void addScrollBarBackgroundMenu(@NotNull ContextMenu addTo, @NotNull ContextMenuBuilder<H> builder, @NotNull ScrollBarTrackBackground background) {

        ContextMenu backgroundMenu = new ContextMenu();
        addTo.addSubMenuEntry("scroll_bar_background", Component.translatable("fancymenu.elements.scroll_bar_background"), backgroundMenu)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.scroll_bar_background.desc")))
                .setIcon(MaterialIcons.WALLPAPER);

        background.verticalTexture.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.IMAGE);

        background.horizontalTexture.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.IMAGE);

        backgroundMenu.addSeparatorEntry("separator_before_scroll_bar_background_borders").setStackable(true);

        background.nineSliceBorderX.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> background.hasAnyTexture())
                .setIcon(MaterialIcons.BORDER_HORIZONTAL);

        background.nineSliceBorderY.buildContextMenuEntryAndAddTo(backgroundMenu, builder)
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> background.hasAnyTexture())
                .setIcon(MaterialIcons.BORDER_VERTICAL);

    }

    /**
     * Adds the menus of the textures and the colors of scroll grabbers.
     */
    public static <H extends PropertyHolder> void addScrollGrabberMenus(@NotNull ContextMenu addTo, @NotNull ContextMenuBuilder<H> builder, @NotNull ScrollBarGrabberStyle style) {

        ContextMenu textureMenu = new ContextMenu();
        addTo.addSubMenuEntry("grabber_texture", Component.translatable("fancymenu.elements.text.scroll_grabber_texture"), textureMenu)
                .setStackable(true)
                .setIcon(MaterialIcons.IMAGE);

        style.verticalTextureNormal.buildContextMenuEntryAndAddTo(textureMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.IMAGE);

        style.verticalTextureHover.buildContextMenuEntryAndAddTo(textureMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.IMAGE);

        textureMenu.addSeparatorEntry("separator_after_vertical_textures").setStackable(true);

        style.horizontalTextureNormal.buildContextMenuEntryAndAddTo(textureMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.IMAGE);

        style.horizontalTextureHover.buildContextMenuEntryAndAddTo(textureMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.IMAGE);

        ContextMenu colorMenu = new ContextMenu();
        addTo.addSubMenuEntry("grabber_color", Component.translatable("fancymenu.elements.text.scroll_grabber_color"), colorMenu)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.text.scroll_grabber_color.desc")))
                .setIcon(MaterialIcons.PALETTE);

        style.colorNormal.buildContextMenuEntryAndAddTo(colorMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

        style.colorHover.buildContextMenuEntryAndAddTo(colorMenu, builder)
                .setStackable(true)
                .setIcon(MaterialIcons.PALETTE);

    }

}
