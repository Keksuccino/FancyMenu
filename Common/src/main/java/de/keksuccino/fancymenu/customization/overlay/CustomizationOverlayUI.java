package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.ManageLayoutsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseAnimationScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.ui.colorscheme.schemes.UIColorSchemes;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resources.texture.WrappedTexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class CustomizationOverlayUI {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation FM_LOGO_ICON_LOCATION = new ResourceLocation("fancymenu", "textures/fancymenu_logo_icon.png");
    private static final WrappedTexture LIGHT_MODE_ICON_TEXTURE = WrappedTexture.of(new ResourceLocation("fancymenu", "textures/light_mode_icon.png"));
    private static final WrappedTexture DARK_MODE_ICON_TEXTURE = WrappedTexture.of(new ResourceLocation("fancymenu", "textures/dark_mode_icon.png"));

    private static MenuBar grandfatheredMenuBar = null;

    @NotNull
    protected static MenuBar buildMenuBar() {

        MenuBar grand = grandfatheredMenuBar;
        if (grand != null) {
            grandfatheredMenuBar = null;
            return grand;
        }

        MenuBar menuBar = new MenuBar();
        Screen screen = Minecraft.getInstance().screen;
        String identifier = ScreenCustomization.getScreenIdentifier(screen);

        // FANCYMENU ICON
        ContextMenu fmMenu = new ContextMenu();
        menuBar.addContextMenuEntry("fancymenu_icon", Component.empty(), fmMenu).setIconTexture(WrappedTexture.of(FM_LOGO_ICON_LOCATION));

        // SCREEN
        ContextMenu screenMenu = new ContextMenu();
        menuBar.addContextMenuEntry("screen", Component.translatable("fancymenu.overlay.menu_bar.screen"), screenMenu);

        screenMenu.addValueCycleEntry("toggle_current_customization", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.screen.current_customization", ScreenCustomization.isCustomizationEnabledForScreen(screen))
                .addCycleListener(cycle -> {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                        ScreenCustomization.setCustomizationForScreenEnabled(screen, cycle.getAsBoolean());
                        ScreenCustomization.reInitCurrentScreen();
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }));

        screenMenu.addClickableEntry("override_current", Component.translatable("fancymenu.overlay.menu_bar.screen.current_override"), (menu, entry) -> {
            //TODO implement this
        }).setIsActiveSupplier((menu, entry) -> FancyMenu.getOptions().advancedCustomizationMode.getValue())
                .setTooltipSupplier((menu, entry) -> {
                    if (!FancyMenu.getOptions().advancedCustomizationMode.getValue()) {
                        return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.screen.current_override.disabled.tooltip"));
                    }
                    return null;
                });

        screenMenu.addSeparatorEntry("separator_1");

        ContextMenu screenSettingsMenu = new ContextMenu();
        screenMenu.addSubMenuEntry("screen_settings", Component.translatable("fancymenu.overlay.menu_bar.screen.settings"), screenSettingsMenu);

        screenSettingsMenu.addValueCycleEntry("play_menu_music", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.screen.settings.play_menu_music", FancyMenu.getOptions().playMenuMusic.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().playMenuMusic.setValue(cycle.getAsBoolean());
                }));

        screenSettingsMenu.addClickableEntry("custom_gui_scale", Component.translatable("fancymenu.overlay.menu_bar.screen.settings.set_default_gui_scale"), (menu, entry) -> {
           //TODO open TextInputScreen here
        });

        screenSettingsMenu.addValueCycleEntry("force_fullscreen", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.screen.settings.force_fullscreen", FancyMenu.getOptions().forceFullscreen.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().forceFullscreen.setValue(cycle.getAsBoolean());
                }));

        ContextMenu screenSettingsGameIntroMenu = new ContextMenu();
        screenSettingsMenu.addSubMenuEntry("game_intro", Component.translatable("fancymenu.overlay.menu_bar.screen.settings.game_intro"), screenSettingsGameIntroMenu);

        screenSettingsGameIntroMenu.addClickableEntry("game_intro_set_animation", Component.translatable("fancymenu.overlay.menu_bar.screen.settings.game_intro.set_intro_animation"), (menu, entry) -> {
            String preSelected = FancyMenu.getOptions().gameIntroAnimation.getValue();
            Minecraft.getInstance().setScreen(new ChooseAnimationScreen((!preSelected.isEmpty()) ? preSelected : null, (call) -> {
                if (call != null) {
                    FancyMenu.getOptions().gameIntroAnimation.setValue(call);
                }
                Minecraft.getInstance().setScreen(screen);
            }));
        });

        screenSettingsGameIntroMenu.addValueCycleEntry("game_intro_allow_skip", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.screen.settings.game_intro.allow_skip", FancyMenu.getOptions().allowGameIntroSkip.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().allowGameIntroSkip.setValue(cycle.getAsBoolean());
                }));

        screenSettingsGameIntroMenu.addClickableEntry("game_intro_set_custom_skip_text", Component.translatable("fancymenu.overlay.menu_bar.screen.settings.game_intro.set_custom_skip_text"), (menu, entry) -> {
           //TODO open TextInputScreen here
        });

        screenSettingsMenu.addValueCycleEntry("preload_animations", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.screen.settings.preload_animations", FancyMenu.getOptions().preLoadAnimations.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().preLoadAnimations.setValue(cycle.getAsBoolean());
                }));

        screenMenu.addSeparatorEntry("separator_2");

        screenMenu.addClickableEntry("reload_fancymenu", Component.translatable("fancymenu.overlay.menu_bar.screen.reload_fancymenu"), (menu, entry) -> {
            MainThreadTaskExecutor.executeInMainThread(() -> {
                grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                ScreenCustomization.reloadFancyMenu();
            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.screen.reload_fancymenu.shortcut"));

        screenMenu.addClickableEntry("disable_customization_for_all", Component.translatable("fancymenu.overlay.menu_bar.screen.disable_customization_for_all"), (menu, entry) -> {
            ConfirmationScreen s = new ConfirmationScreen((call) -> {
                Minecraft.getInstance().setScreen(screen);
                if (call) {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                        ScreenCustomization.disableCustomizationForAllScreens();
                        ScreenCustomization.reInitCurrentScreen();
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
            }, LocalizationUtils.splitLocalizedStringLines("fancymenu.overlay.menu_bar.screen.disable_customization_for_all.confirm"));
            Minecraft.getInstance().setScreen(s);
        }).setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.screen.disable_customization_for_all.tooltip")));

        // LAYOUT
        ContextMenu layoutMenu = new ContextMenu();
        menuBar.addContextMenuEntry("layout", Component.translatable("fancymenu.overlay.menu_bar.layout"), layoutMenu);

        ContextMenu layoutNewMenu = new ContextMenu();
        layoutMenu.addSubMenuEntry("layout.new", Component.translatable("fancymenu.overlay.menu_bar.layout.new"), layoutNewMenu);

        layoutNewMenu.addClickableEntry("layout.new.current", Component.translatable("fancymenu.overlay.menu_bar.layout.new.current"), (menu, entry) -> {
            if (screen != null) {
                LayoutHandler.openLayoutEditor(new Layout(screen), screen);
            }
        }).setIsActiveSupplier((menu, entry) -> ScreenCustomization.isCustomizationEnabledForScreen(screen));

        layoutNewMenu.addClickableEntry("layout.new.universal", Component.translatable("fancymenu.overlay.menu_bar.layout.new.universal"), (menu, entry) -> {
            LayoutHandler.openLayoutEditor(new Layout(), null);
        });

        ContextMenu layoutManageMenu = new ContextMenu();
        layoutMenu.addSubMenuEntry("layout.manage", Component.translatable("fancymenu.overlay.menu_bar.layout.manage"), layoutManageMenu);

        ContextMenu layoutManageCurrentMenu = new ContextMenu();
        layoutManageMenu.addSubMenuEntry("layout.manage.current", Component.translatable("fancymenu.overlay.menu_bar.layout.manage.current"), layoutManageCurrentMenu)
                .setIsActiveSupplier((menu, entry) -> ScreenCustomization.isCustomizationEnabledForScreen(screen));

        if (identifier != null) {
            int i = 0;
            for (Layout l : LayoutHandler.sortLayoutListByLastEdited(LayoutHandler.getAllLayoutsForMenuIdentifier(identifier, false), true, 8)) {
                layoutManageCurrentMenu.addSubMenuEntry("layout.manage.current.recent_" + i, Component.empty(), buildManageLayoutSubMenu(l))
                        .setLabelSupplier((menu, entry) -> {
                            Style style = l.getStatus().getEntryComponentStyle();
                            MutableComponent c = Component.literal(l.getLayoutName());
                            c.append(Component.literal(" (").setStyle(style));
                            c.append(l.getStatus().getEntryComponent());
                            c.append(Component.literal(")").setStyle(style));
                            return c;
                        });
                i++;
            }
        }

        layoutManageCurrentMenu.addSeparatorEntry("layout.manage.current.separator_1");

        layoutManageCurrentMenu.addClickableEntry("layout.manage.current.all", Component.translatable("fancymenu.overlay.menu_bar.layout.manage.all"), (menu, entry) -> {
            if (identifier != null) {
                Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForMenuIdentifier(identifier, false), screen, layouts -> {
                    Minecraft.getInstance().setScreen(screen);
                }));
            }
        });

        ContextMenu layoutManageUniversalMenu = new ContextMenu();
        layoutManageMenu.addSubMenuEntry("layout.manage.universal", Component.translatable("fancymenu.overlay.menu_bar.layout.manage.universal"), layoutManageUniversalMenu);

        int i = 0;
        for (Layout l : LayoutHandler.sortLayoutListByLastEdited(LayoutHandler.getAllLayoutsForMenuIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), true, 8)) {
            layoutManageUniversalMenu.addSubMenuEntry("layout.manage.universal.recent_" + i, Component.empty(), buildManageLayoutSubMenu(l))
                    .setLabelSupplier((menu, entry) -> {
                        Style style = l.getStatus().getEntryComponentStyle();
                        MutableComponent c = Component.literal(l.getLayoutName());
                        c.append(Component.literal(" (").setStyle(style));
                        c.append(l.getStatus().getEntryComponent());
                        c.append(Component.literal(")").setStyle(style));
                        return c;
                    });
            i++;
        }

        layoutManageUniversalMenu.addSeparatorEntry("layout.manage.universal.separator_1");

        layoutManageUniversalMenu.addClickableEntry("layout.manage.universal.all", Component.translatable("fancymenu.overlay.menu_bar.layout.manage.all"), (menu, entry) -> {
            Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForMenuIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), null, (layouts -> {
                Minecraft.getInstance().setScreen(screen);
            })));
        });

        // VARIABLES
        ContextMenu variablesMenu = new ContextMenu();
        menuBar.addContextMenuEntry("variables", Component.translatable("fancymenu.overlay.menu_bar.variables"), variablesMenu);

        // - Manage Variables (reset, delete, set, etc.)
        // - Set Variables to reset on launch

        // TOOLS
        ContextMenu toolsMenu = new ContextMenu();
        menuBar.addContextMenuEntry("tools", Component.translatable("fancymenu.overlay.menu_bar.tools"), toolsMenu);

        // WINDOW
        ContextMenu uiMenu = new ContextMenu();
        menuBar.addContextMenuEntry("window", Component.translatable("fancymenu.overlay.menu_bar.window"), uiMenu);

        // - Custom Window Icon (Toggle On/Off)
        // - Custom Window Title (Input)
        // -------------
        // - UI Scale (Cycle 1-2-3)
        // - UI Text Shadow (Toggle On/Off)
        // - UI Click Sounds (Toggle On/Off)
        // - UI Theme (erstmal nur light/dark mode, später mehr)

        // HELP
        ContextMenu helpMenu = new ContextMenu();
        menuBar.addContextMenuEntry("help", Component.translatable("fancymenu.overlay.menu_bar.help"), helpMenu);

        //TODO Link zu CurseForge FancyMenu Kategorie zu HELP Tab adden

        // DARK/LIGHT MODE SWITCHER
        menuBar.addClickableEntry(MenuBar.Side.RIGHT, "dark_light_mode_switcher", Component.literal(""), (bar, entry) -> {
            FancyMenu.getOptions().lightMode.setValue(!FancyMenu.getOptions().lightMode.getValue());
            UIColorSchemes.updateActiveScheme();
        }).setIconTextureSupplier((bar, entry) -> {
            if (FancyMenu.getOptions().lightMode.getValue()) {
                return LIGHT_MODE_ICON_TEXTURE;
            }
            return DARK_MODE_ICON_TEXTURE;
        });

        return menuBar;

    }

    @NotNull
    protected static ContextMenu buildManageLayoutSubMenu(Layout layout) {

        ContextMenu menu = new ContextMenu();
        Screen screen = Minecraft.getInstance().screen;

        menu.addClickableEntry("toggle_layout_status", Component.empty(), (menu1, entry) -> {
            MainThreadTaskExecutor.executeInMainThread(() -> {
                grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                layout.setEnabled(!layout.isEnabled(), true);
            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }).setLabelSupplier((menu1, entry) -> layout.getStatus().getCycleComponent());

        menu.addClickableEntry("edit_layout", Component.translatable("fancymenu.layout.manage.edit"), (menu1, entry) -> {
            menu1.closeMenu();
            MainThreadTaskExecutor.executeInMainThread(() -> LayoutHandler.openLayoutEditor(layout, layout.isUniversalLayout() ? null : screen), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        });

        menu.addClickableEntry("delete_layout", Component.translatable("fancymenu.layout.manage.delete"), (menu1, entry) -> {
            menu1.closeMenu();
            ConfirmationScreen s = new ConfirmationScreen(call -> {
                if (call) {
                    layout.delete(false);
                }
                Minecraft.getInstance().setScreen(screen);
            }, LocalizationUtils.splitLocalizedStringLines("fancymenu.layout.manage.delete.confirm"));
            Minecraft.getInstance().setScreen(s);
        });

        menu.addClickableEntry("edit_in_system_text_editor", Component.translatable("fancymenu.layout.manage.open_in_text_editor"), (menu1, entry) -> {
            if (layout.layoutFile != null) {
                menu1.closeMenu();
                ScreenCustomization.openFile(layout.layoutFile);
            }
        });

        return menu;

    }

}
