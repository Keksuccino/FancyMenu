package de.keksuccino.fancymenu.customization.overlay;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.colorscheme.schemes.UIColorSchemes;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.resources.texture.WrappedTexture;
import net.minecraft.client.Minecraft;
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

        ContextMenu layoutNewMenu = new ContextMenu();
        layoutMenu.addSubMenuEntry("layout.new", Component.translatable("fancymenu.overlay.menu_bar.layout.new"), layoutNewMenu);

        layoutNewMenu.addClickableEntry("layout.new.current", Component.translatable("fancymenu.overlay.menu_bar.layout.new.current"), (menu, entry) -> {
            if (Minecraft.getInstance().screen != null) {
                LayoutHandler.openLayoutEditor(new Layout(Minecraft.getInstance().screen), Minecraft.getInstance().screen);
            }
        });

        layoutNewMenu.addClickableEntry("layout.new.universal", Component.translatable("fancymenu.overlay.menu_bar.layout.new.universal"), (menu, entry) -> {
            LayoutHandler.openLayoutEditor(new Layout(), null);
        });

        ContextMenu layoutManageMenu = new ContextMenu();
        layoutMenu.addSubMenuEntry("layout.manage", Component.translatable("fancymenu.overlay.menu_bar.layout.manage"), layoutManageMenu);

        ContextMenu layoutManageCurrentMenu = new ContextMenu();
        layoutManageMenu.addSubMenuEntry("layout.manage.current", Component.translatable("fancymenu.overlay.menu_bar.layout.manage.current"), layoutManageCurrentMenu);

        String identifier = null;
        if (Minecraft.getInstance().screen != null) {
            if (Minecraft.getInstance().screen instanceof CustomGuiBase c) {
                identifier = c.getIdentifier();
            } else {
                identifier = Minecraft.getInstance().screen.getClass().getName();
            }
        }
        if (identifier != null) {
            int i = 0;
            for (Layout l : LayoutHandler.getRecentlyEditedLayoutsForMenuIdentifier(identifier, false)) {
                //TODO replace with sub menu entry
                layoutManageCurrentMenu.addClickableEntry("layout.manage.current.recent_" + i, Component.literal(Files.getNameWithoutExtension(l.layoutFile.getName())), (menu, entry) -> {
                    LayoutHandler.openLayoutEditor(l, Minecraft.getInstance().screen);
                });
                i++;
            }
        }

        layoutManageCurrentMenu.addSeparatorEntry("layout.manage.current.separator_1");

        layoutManageCurrentMenu.addClickableEntry("layout.manage.current.all", Component.translatable("fancymenu.overlay.menu_bar.layout.manage.all"), (menu, entry) -> {
           //TODO open manage layouts screen for current layouts (manage screen shows both enabled and disabled layouts)
        });

        ContextMenu layoutManageUniversalMenu = new ContextMenu();
        layoutManageMenu.addSubMenuEntry("layout.manage.universal", Component.translatable("fancymenu.overlay.menu_bar.layout.manage.universal"), layoutManageUniversalMenu);

        int i = 0;
        for (Layout l : LayoutHandler.getRecentlyEditedLayoutsForMenuIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true)) {
            //TODO replace with sub menu entry
            layoutManageUniversalMenu.addClickableEntry("layout.manage.universal.recent_" + i, Component.literal(Files.getNameWithoutExtension(l.layoutFile.getName())), (menu, entry) -> {
                LayoutHandler.openLayoutEditor(l, null);
            });
            i++;
        }

        layoutManageUniversalMenu.addSeparatorEntry("layout.manage.universal.separator_1");

        layoutManageUniversalMenu.addClickableEntry("layout.manage.universal.all", Component.translatable("fancymenu.overlay.menu_bar.layout.manage.all"), (menu, entry) -> {
            //TODO open manage layouts screen for universal layouts (manage screen shows both enabled and disabled layouts)
        });

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
