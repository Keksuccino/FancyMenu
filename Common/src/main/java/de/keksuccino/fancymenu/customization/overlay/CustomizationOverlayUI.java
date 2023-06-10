package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.rendering.ui.colorscheme.schemes.UIColorSchemes;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.resources.texture.WrappedTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class CustomizationOverlayUI {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation FM_LOGO_ICON_LOCATION = new ResourceLocation("fancymenu", "textures/fancymenu_logo_icon.png");
    private static final WrappedTexture LIGHT_MODE_ICON_TEXTURE = WrappedTexture.of(new ResourceLocation("fancymenu", "textures/light_mode_icon.png"));
    private static final WrappedTexture DARK_MODE_ICON_TEXTURE = WrappedTexture.of(new ResourceLocation("fancymenu", "textures/dark_mode_icon.png"));

    @NotNull
    protected static MenuBar buildMenuBar() {

        MenuBar menuBar = new MenuBar();

        // FANCYMENU ICON
        ContextMenu fmMenu = new ContextMenu();
        menuBar.addContextMenuEntry("fancymenu_icon", Component.literal(""), fmMenu).setIconTexture(WrappedTexture.of(FM_LOGO_ICON_LOCATION));

        // SCREEN
        ContextMenu screenMenu = new ContextMenu();
        menuBar.addContextMenuEntry("screen", Component.translatable("fancymenu.overlay.menu_bar.screen"), screenMenu);

        // LAYOUT
        ContextMenu layoutMenu = new ContextMenu();
        menuBar.addContextMenuEntry("layout", Component.translatable("fancymenu.overlay.menu_bar.layout"), layoutMenu);

        // TOOLS
        ContextMenu toolsMenu = new ContextMenu();
        menuBar.addContextMenuEntry("tools", Component.translatable("fancymenu.overlay.menu_bar.tools"), toolsMenu);

        // SETTINGS
        ContextMenu settingsMenu = new ContextMenu();
        menuBar.addContextMenuEntry("settings", Component.translatable("fancymenu.overlay.menu_bar.settings"), settingsMenu);

        // HELP
        ContextMenu helpMenu = new ContextMenu();
        menuBar.addContextMenuEntry("help", Component.translatable("fancymenu.overlay.menu_bar.help"), helpMenu);

        // DARK/LIGHT MODE SWITCHER
        menuBar.addClickableEntry(MenuBar.Side.RIGHT, "dark_light_mode_switcher", Component.literal(""), (bar, entry) -> {
            LOGGER.info("CLICK");
            try {
                FancyMenu.getConfig().setValue("light_mode", !FancyMenu.getConfig().getOrDefault("light_mode", false));
                UIColorSchemes.updateActiveScheme();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).setIconTextureSupplier((bar, entry) -> {
            if (FancyMenu.getConfig().getOrDefault("light_mode", false)) {
                return LIGHT_MODE_ICON_TEXTURE;
            }
            return DARK_MODE_ICON_TEXTURE;
        });

        return menuBar;

    }

}
