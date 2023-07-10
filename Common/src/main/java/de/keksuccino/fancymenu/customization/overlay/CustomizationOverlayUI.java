package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.ManageLayoutsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseAnimationScreen;
import de.keksuccino.fancymenu.customization.variables.ManageVariablesScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedValueCycle;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.NonStackableOverlayUI;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resources.texture.LocalTexture;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
import de.keksuccino.fancymenu.util.resources.texture.WrappedTexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class CustomizationOverlayUI {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation FM_LOGO_ICON_LOCATION = new ResourceLocation("fancymenu", "textures/menubar/icons/fancymenu_logo.png");

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

        // CUSTOMIZATION
        ContextMenu customizationMenu = new ContextMenu();
        menuBar.addContextMenuEntry("screen", Component.translatable("fancymenu.overlay.menu_bar.customization"), customizationMenu);

        customizationMenu.addValueCycleEntry("toggle_current_customization", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.current_customization", ScreenCustomization.isCustomizationEnabledForScreen(screen))
                .addCycleListener(cycle -> {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                        ScreenCustomization.setCustomizationForScreenEnabled(screen, cycle.getAsBoolean());
                        ScreenCustomization.reInitCurrentScreen();
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }));

        customizationMenu.addClickableEntry("override_current", Component.translatable("fancymenu.overlay.menu_bar.customization.current_override"), (menu, entry) -> {
                    //TODO implement this
                }).setIsActiveSupplier((menu, entry) -> FancyMenu.getOptions().advancedCustomizationMode.getValue())
                .setTooltipSupplier((menu, entry) -> {
                    if (!FancyMenu.getOptions().advancedCustomizationMode.getValue()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.current_override.disabled.tooltip"));
                    }
                    return null;
                });

        customizationMenu.addSeparatorEntry("separator_after_override_current");

        ContextMenu layoutMenu = new ContextMenu();
        customizationMenu.addSubMenuEntry("layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout"), layoutMenu);

        ContextMenu layoutNewMenu = new ContextMenu();
        layoutMenu.addSubMenuEntry("new_layout", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.new"), layoutNewMenu);

        layoutNewMenu.addClickableEntry("new_layout_for_current", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.new.current"), (menu, entry) -> {
                    if (screen != null) {
                        LayoutHandler.openLayoutEditor(new Layout(screen), screen);
                    }
                }).setIsActiveSupplier((menu, entry) -> ScreenCustomization.isCustomizationEnabledForScreen(screen))
                .setTooltipSupplier((menu, entry) -> {
                    if (!ScreenCustomization.isCustomizationEnabledForScreen(screen)) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.layout.new.current.disabled.tooltip"));
                    }
                    return null;
                });

        layoutNewMenu.addClickableEntry("new_universal_layout", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.new.universal"), (menu, entry) -> {
            LayoutHandler.openLayoutEditor(new Layout(), null);
        });

        ContextMenu layoutManageMenu = new ContextMenu();
        layoutMenu.addSubMenuEntry("manage_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage"), layoutManageMenu);

        ContextMenu layoutManageCurrentMenu = new ContextMenu();
        layoutManageMenu.addSubMenuEntry("manage_layouts_for_current", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.current"), layoutManageCurrentMenu)
                .setIsActiveSupplier((menu, entry) -> ScreenCustomization.isCustomizationEnabledForScreen(screen))
                .setTooltipSupplier((menu, entry) -> {
                    if (!ScreenCustomization.isCustomizationEnabledForScreen(screen)) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.layout.manage.current.disabled.tooltip"));
                    }
                    return null;
                }).setSubMenuOpeningSide(ContextMenu.SubMenuOpeningSide.LEFT);

        if (identifier != null) {
            int i = 0;
            for (Layout l : LayoutHandler.sortLayoutListByLastEdited(LayoutHandler.getAllLayoutsForMenuIdentifier(identifier, false), true, 8)) {
                layoutManageCurrentMenu.addSubMenuEntry("layout_" + i, Component.empty(), buildManageLayoutSubMenu(l))
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

        layoutManageCurrentMenu.addClickableEntry("layout.manage.current.all", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.all"), (menu, entry) -> {
            if (identifier != null) {
                Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForMenuIdentifier(identifier, false), screen, layouts -> {
                    Minecraft.getInstance().setScreen(screen);
                }));
            }
        });

        ContextMenu layoutManageUniversalMenu = new ContextMenu();
        layoutManageMenu.addSubMenuEntry("layout.manage.universal", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.universal"), layoutManageUniversalMenu)
                .setSubMenuOpeningSide(ContextMenu.SubMenuOpeningSide.LEFT);

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

        layoutManageUniversalMenu.addClickableEntry("layout.manage.universal.all", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.all"), (menu, entry) -> {
            Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForMenuIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), null, (layouts -> {
                Minecraft.getInstance().setScreen(screen);
            })));
        });

        customizationMenu.addSeparatorEntry("separator_after_layout_menu");

        ContextMenu screenSettingsMenu = new ContextMenu();
        customizationMenu.addSubMenuEntry("screen_settings", Component.translatable("fancymenu.overlay.menu_bar.customization.settings"), screenSettingsMenu);

        screenSettingsMenu.addValueCycleEntry("advanced_customization_mode", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.advanced_customization_mode", FancyMenu.getOptions().advancedCustomizationMode.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().advancedCustomizationMode.setValue(cycle.getAsBoolean());
                })).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.advanced_customization_mode.tooltip")));

        screenSettingsMenu.addSeparatorEntry("separator_after_advanced_mode");

        screenSettingsMenu.addValueCycleEntry("play_menu_music", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.play_menu_music", FancyMenu.getOptions().playMenuMusic.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().playMenuMusic.setValue(cycle.getAsBoolean());
                }));

        NonStackableOverlayUI.addIntegerInputContextMenuEntryTo(screenSettingsMenu, "default_gui_scale",
                        Component.translatable("fancymenu.overlay.menu_bar.customization.settings.set_default_gui_scale"),
                        () -> FancyMenu.getOptions().defaultGuiScale.getValue(),
                        integer -> FancyMenu.getOptions().defaultGuiScale.setValue(integer),
                        true, FancyMenu.getOptions().defaultGuiScale.getDefaultValue(), null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.set_default_gui_scale.tooltip")));

        screenSettingsMenu.addValueCycleEntry("force_fullscreen", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.force_fullscreen", FancyMenu.getOptions().forceFullscreen.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().forceFullscreen.setValue(cycle.getAsBoolean());
                }));

        ContextMenu screenSettingsGameIntroMenu = new ContextMenu();
        screenSettingsMenu.addSubMenuEntry("game_intro", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro"), screenSettingsGameIntroMenu);

        screenSettingsGameIntroMenu.addClickableEntry("game_intro_set_animation", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro.set_intro_animation"), (menu, entry) -> {
            String preSelected = FancyMenu.getOptions().gameIntroAnimation.getValue();
            Minecraft.getInstance().setScreen(new ChooseAnimationScreen((!preSelected.isEmpty()) ? preSelected : null, (call) -> {
                if (call != null) {
                    FancyMenu.getOptions().gameIntroAnimation.setValue(call);
                }
                Minecraft.getInstance().setScreen(screen);
            }));
        });

        screenSettingsGameIntroMenu.addClickableEntry("game_intro_reset_animation", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro.reset_intro_animation"), (menu, entry) -> {
            FancyMenu.getOptions().gameIntroAnimation.resetToDefault();
        });

        screenSettingsGameIntroMenu.addSeparatorEntry("separator_after_game_intro_set_animation");

        screenSettingsGameIntroMenu.addValueCycleEntry("game_intro_allow_skip", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.game_intro.allow_skip", FancyMenu.getOptions().allowGameIntroSkip.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().allowGameIntroSkip.setValue(cycle.getAsBoolean());
                }));

        screenSettingsGameIntroMenu.addClickableEntry("game_intro_set_custom_skip_text", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro.set_custom_skip_text"), (menu, entry) -> {
            TextInputScreen s = new TextInputScreen(Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro.set_custom_skip_text"), null, call -> {
                if (call != null) {
                    FancyMenu.getOptions().customGameIntroSkipText.setValue(call);
                }
                Minecraft.getInstance().setScreen(screen);
            });
            s.setText(FancyMenu.getOptions().customGameIntroSkipText.getValue());
            Minecraft.getInstance().setScreen(s);
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.game_intro.set_custom_skip_text.tooltip")));

        screenSettingsMenu.addValueCycleEntry("preload_animations", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.preload_animations", FancyMenu.getOptions().preLoadAnimations.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().preLoadAnimations.setValue(cycle.getAsBoolean());
                }));

        screenSettingsMenu.addSeparatorEntry("separator_after_preload_animations");

        ContextMenu worldLoadingScreenSettingsMenu = new ContextMenu();
        screenSettingsMenu.addSubMenuEntry("world_loading_screen", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.world_loading_screen"), worldLoadingScreenSettingsMenu);

        worldLoadingScreenSettingsMenu.addValueCycleEntry("world_loading_screen_animation", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.world_loading_screen.animation", FancyMenu.getOptions().showLevelLoadingScreenChunkAnimation.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().showLevelLoadingScreenChunkAnimation.setValue(cycle.getAsBoolean());
                }));

        worldLoadingScreenSettingsMenu.addValueCycleEntry("world_loading_screen_percent", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.world_loading_screen.percent", FancyMenu.getOptions().showLevelLoadingScreenPercent.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().showLevelLoadingScreenPercent.setValue(cycle.getAsBoolean());
                }));

        screenSettingsMenu.addValueCycleEntry("singleplayer_world_icons", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.singleplayer_screen.world_icons", FancyMenu.getOptions().showSingleplayerScreenWorldIcons.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().showSingleplayerScreenWorldIcons.setValue(cycle.getAsBoolean());
                }));

        screenSettingsMenu.addValueCycleEntry("multiplayer_server_icons", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.multiplayer_screen.server_icons", FancyMenu.getOptions().showMultiplayerScreenServerIcons.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().showMultiplayerScreenServerIcons.setValue(cycle.getAsBoolean());
                }));

        screenSettingsMenu.addSeparatorEntry("separator_after_mp_server_icons");

        ContextMenu windowIconMenu = new ContextMenu();
        screenSettingsMenu.addSubMenuEntry("window_icon", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon"), windowIconMenu);

        LocalizedValueCycle<CommonCycles.CycleEnabledDisabled> windowIconToggleCycle = CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.toggle", FancyMenu.getOptions().showCustomWindowIcon.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().showCustomWindowIcon.setValue(cycle.getAsBoolean());
                    if (cycle.getAsBoolean()) {
                        WindowHandler.updateCustomWindowIcon();
                    } else {
                        WindowHandler.resetWindowIcon();
                    }
                });
        windowIconMenu.addValueCycleEntry("window_icon_toggle", windowIconToggleCycle)
                .setIsActiveSupplier((menu, entry) -> WindowHandler.allCustomWindowIconsSetAndFound())
                .setTooltipSupplier((menu, entry) -> !WindowHandler.allCustomWindowIconsSetAndFound() ? Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.toggle.disabled.tooltip")) : null);

        ContextMenu.ClickableContextMenuEntry<?> icon16Entry = NonStackableOverlayUI.addFileChooserContextMenuEntryTo(windowIconMenu, "icon_16",
                        Component.empty(),
                        () -> FancyMenu.getOptions().customWindowIcon16.getValue(),
                        s -> {
                            FancyMenu.getOptions().customWindowIcon16.setValue(s);
                            FancyMenu.getOptions().showCustomWindowIcon.setValue(false);
                            windowIconToggleCycle.setCurrentValue(CommonCycles.CycleEnabledDisabled.DISABLED);
                            WindowHandler.resetWindowIcon();
                        },
                        true, FancyMenu.getOptions().customWindowIcon16.getDefaultValue(),
                        file -> {
                            if (file.getName().toLowerCase().endsWith(".png")) {
                                LocalTexture t = TextureHandler.INSTANCE.getTexture(file);
                                return ((t != null) && (t.getWidth() == 16) && (t.getHeight() == 16));
                            }
                            return false;
                        })
                .setLabelSupplier((menu, entry) -> {
                    MutableComponent notFound = Component.literal("✖").withStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().error_text_color.getColorInt()));
                    MutableComponent found = Component.literal("✔").withStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().success_text_color.getColorInt()));
                    return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_16", (WindowHandler.getCustomWindowIcon16() != null) ? found : notFound);
                });
        if (icon16Entry instanceof ContextMenu.SubMenuContextMenuEntry s) {
            s.getSubContextMenu().setSubMenuOpeningSide(ContextMenu.SubMenuOpeningSide.LEFT);
        }

        ContextMenu.ClickableContextMenuEntry<?> icon32Entry = NonStackableOverlayUI.addFileChooserContextMenuEntryTo(windowIconMenu, "icon_32",
                        Component.empty(),
                        () -> FancyMenu.getOptions().customWindowIcon32.getValue(),
                        s -> {
                            FancyMenu.getOptions().customWindowIcon32.setValue(s);
                            FancyMenu.getOptions().showCustomWindowIcon.setValue(false);
                            windowIconToggleCycle.setCurrentValue(CommonCycles.CycleEnabledDisabled.DISABLED);
                            WindowHandler.resetWindowIcon();
                        },
                        true, FancyMenu.getOptions().customWindowIcon32.getDefaultValue(),
                        file -> {
                            if (file.getName().toLowerCase().endsWith(".png")) {
                                LocalTexture t = TextureHandler.INSTANCE.getTexture(file);
                                return ((t != null) && (t.getWidth() == 32) && (t.getHeight() == 32));
                            }
                            return false;
                        })
                .setLabelSupplier((menu, entry) -> {
                    MutableComponent notFound = Component.literal("✖").withStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().error_text_color.getColorInt()));
                    MutableComponent found = Component.literal("✔").withStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().success_text_color.getColorInt()));
                    return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_32", (WindowHandler.getCustomWindowIcon32() != null) ? found : notFound);
                });
        if (icon32Entry instanceof ContextMenu.SubMenuContextMenuEntry s) {
            s.getSubContextMenu().setSubMenuOpeningSide(ContextMenu.SubMenuOpeningSide.LEFT);
        }

        ContextMenu.ClickableContextMenuEntry<?> iconMacOSEntry = NonStackableOverlayUI.addFileChooserContextMenuEntryTo(windowIconMenu, "icon_macos",
                        Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_macos"),
                        () -> FancyMenu.getOptions().customWindowIconMacOS.getValue(),
                        s -> {
                            FancyMenu.getOptions().customWindowIconMacOS.setValue(s);
                            FancyMenu.getOptions().showCustomWindowIcon.setValue(false);
                            windowIconToggleCycle.setCurrentValue(CommonCycles.CycleEnabledDisabled.DISABLED);
                            WindowHandler.resetWindowIcon();
                        },
                        true, FancyMenu.getOptions().customWindowIconMacOS.getDefaultValue(),
                        file -> file.getName().toLowerCase().endsWith(".icns"))
                .setLabelSupplier((menu, entry) -> {
                    MutableComponent notFound = Component.literal("✖").withStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().error_text_color.getColorInt()));
                    MutableComponent found = Component.literal("✔").withStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().success_text_color.getColorInt()));
                    return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_macos", (WindowHandler.getCustomWindowIconMacOS() != null) ? found : notFound);
                });
        if (iconMacOSEntry instanceof ContextMenu.SubMenuContextMenuEntry s) {
            s.getSubContextMenu().setSubMenuOpeningSide(ContextMenu.SubMenuOpeningSide.LEFT);
        }

        windowIconMenu.addSeparatorEntry("separator_after_macos_icon");

        windowIconMenu.addClickableEntry("convert_png_to_macos_icon", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.convert_to_macos_icon"), (menu, entry) -> {
            WebUtils.openWebLink("https://miconv.com/convert-png-to-icns/");
        });

        NonStackableOverlayUI.addInputContextMenuEntryTo(screenSettingsMenu, "window_title", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_title"),
                        () -> FancyMenu.getOptions().customWindowTitle.getValue(),
                        s -> {
                            FancyMenu.getOptions().customWindowTitle.setValue(s);
                            WindowHandler.updateWindowTitle();
                        }, true, FancyMenu.getOptions().customWindowTitle.getDefaultValue(), null, false, false, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.custom_window_title.tooltip")));

        customizationMenu.addSeparatorEntry("separator_after_settings");

        customizationMenu.addClickableEntry("reload_fancymenu", Component.translatable("fancymenu.overlay.menu_bar.customization.reload_fancymenu"), (menu, entry) -> {
            MainThreadTaskExecutor.executeInMainThread(() -> {
                grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                ScreenCustomization.reloadFancyMenu();
            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.customization.reload_fancymenu.shortcut"));

        customizationMenu.addClickableEntry("disable_customization_for_all", Component.translatable("fancymenu.overlay.menu_bar.customization.disable_customization_for_all"), (menu, entry) -> {
            Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call) -> {
                Minecraft.getInstance().setScreen(screen);
                if (call) {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                        ScreenCustomization.disableCustomizationForAllScreens();
                        ScreenCustomization.reInitCurrentScreen();
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
            }, LocalizationUtils.splitLocalizedStringLines("fancymenu.overlay.menu_bar.customization.disable_customization_for_all.confirm")));
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.disable_customization_for_all.tooltip")));

        customizationMenu.addSeparatorEntry("separator_before_hide_menu_bar");

        customizationMenu.addClickableEntry("hide_menu_bar", Component.translatable("fancymenu.overlay.menu_bar.customization.hide_overlay"), (menu, entry) -> {
            Minecraft.getInstance().setScreen(ConfirmationScreen.critical((call) -> {
                if (call) {
                    FancyMenu.getOptions().showCustomizationOverlay.setValue(!FancyMenu.getOptions().showCustomizationOverlay.getValue());
                }
                Minecraft.getInstance().setScreen(screen);
            }, LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.hide_overlay.confirm")).setDelay(4000));
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.customization.hide_overlay.shortcut"));

        // VARIABLES
        ContextMenu variablesMenu = new ContextMenu();
        menuBar.addContextMenuEntry("variables", Component.translatable("fancymenu.overlay.menu_bar.variables"), variablesMenu);

        variablesMenu.addClickableEntry("manage_variables", Component.translatable("fancymenu.overlay.menu_bar.variables.manage"), (menu, entry) -> {
            ManageVariablesScreen s = new ManageVariablesScreen(call -> {
                Minecraft.getInstance().setScreen(screen);
            });
            Minecraft.getInstance().setScreen(s);
        });

        // TOOLS
        ContextMenu toolsMenu = new ContextMenu();
        menuBar.addContextMenuEntry("tools", Component.translatable("fancymenu.overlay.menu_bar.tools"), toolsMenu);

        toolsMenu.addClickableEntry("copy_current_screen_identifier", Component.translatable("fancymenu.overlay.menu_bar.tools.copy_current_screen_identifier"), (menu, entry) -> {
            if (identifier != null) {
                Minecraft.getInstance().keyboardHandler.setClipboard(identifier);
                menu.closeMenu();
            }
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.tools.copy_current_screen_identifier.tooltip")));

        // USER INTERFACE
        ContextMenu userInterfaceMenu = new ContextMenu();
        menuBar.addContextMenuEntry("user_interface", Component.translatable("fancymenu.overlay.menu_bar.user_interface"), userInterfaceMenu);

        int preSelectedUiScale = (int)FancyMenu.getOptions().uiScale.getValue().floatValue();
        if ((preSelectedUiScale != 1) && (preSelectedUiScale != 2) && (preSelectedUiScale != 3)) {
            preSelectedUiScale = 1;
        }
        userInterfaceMenu.addValueCycleEntry("ui_scale", CommonCycles.cycle("fancymenu.overlay.menu_bar.user_interface.ui_scale", ListUtils.build(1,2,3), preSelectedUiScale)
                .addCycleListener(integer -> {
                    FancyMenu.getOptions().uiScale.setValue(Float.valueOf(integer));
                }));

        userInterfaceMenu.addValueCycleEntry("ui_text_shadow", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.user_interface.ui_text_shadow", FancyMenu.getOptions().enableUiTextShadow.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().enableUiTextShadow.setValue(cycle.getAsBoolean());
                }));

        userInterfaceMenu.addValueCycleEntry("ui_click_sounds", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.user_interface.ui_click_sounds", FancyMenu.getOptions().playUiClickSounds.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().playUiClickSounds.setValue(cycle.getAsBoolean());
                }));

        ContextMenu windowUiThemeMenu = new ContextMenu();
        userInterfaceMenu.addSubMenuEntry("ui_theme", Component.translatable("fancymenu.overlay.menu_bar.user_interface.ui_theme"), windowUiThemeMenu);

        int i2 = 0;
        for (UIColorTheme s : UIColorThemeRegistry.getThemes()) {
            windowUiThemeMenu.addClickableEntry("ui_theme_" + i2, s.getDisplayName(), (menu, entry) -> {
                FancyMenu.getOptions().uiTheme.setValue(s.getIdentifier());
                UIColorThemeRegistry.setActiveTheme(s.getIdentifier());
            });
            i2++;
        }

        // HELP
        ContextMenu helpMenu = new ContextMenu();
        menuBar.addContextMenuEntry("help", Component.translatable("fancymenu.overlay.menu_bar.help"), helpMenu);

        helpMenu.addClickableEntry("fancymenu_wiki", Component.translatable("fancymenu.overlay.menu_bar.help.wiki"), (menu, entry) -> {
            WebUtils.openWebLink("https://fm.keksuccino.dev");
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.help.wiki.tooltip")));

        helpMenu.addClickableEntry("join_the_discord", Component.translatable("fancymenu.overlay.menu_bar.help.discord"), (menu, entry) -> {
            WebUtils.openWebLink("https://discord.gg/UzmeWkD");
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.help.discord.tooltip")));

        helpMenu.addClickableEntry("report_issue", Component.translatable("fancymenu.overlay.menu_bar.help.report_issue"), (menu, entry) -> {
            WebUtils.openWebLink("https://github.com/Keksuccino/FancyMenu/issues");
        });

        helpMenu.addSeparatorEntry("separator_after_report_issue");

        helpMenu.addClickableEntry("curseforge_fancymenu_category", Component.translatable("fancymenu.overlay.menu_bar.help.curseforge_fancymenu_category"), (menu, entry) -> {
            WebUtils.openWebLink("https://www.curseforge.com/minecraft/search?page=1&class=customization&categoryIds=5186");
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.help.curseforge_fancymenu_category.tooltip")));

        helpMenu.addSeparatorEntry("separator_after_curseforge");

        helpMenu.addClickableEntry("keksuccino_patreon", Component.translatable("fancymenu.overlay.menu_bar.help.patreon"), (menu, entry) -> {
            WebUtils.openWebLink("https://www.patreon.com/keksuccino");
        });

        helpMenu.addClickableEntry("paypal_tip_jar", Component.translatable("fancymenu.overlay.menu_bar.help.paypal"), (menu, entry) -> {
            WebUtils.openWebLink("https://www.paypal.com/paypalme/TimSchroeter");
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
            Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings(call -> {
                if (call) {
                    layout.delete(false);
                }
                Minecraft.getInstance().setScreen(screen);
            }, LocalizationUtils.splitLocalizedStringLines("fancymenu.layout.manage.delete.confirm")));
        });

        menu.addClickableEntry("edit_in_system_text_editor", Component.translatable("fancymenu.layout.manage.open_in_text_editor"), (menu1, entry) -> {
            if (layout.layoutFile != null) {
                menu1.closeMenu();
                FileUtils.openFile(layout.layoutFile);
            }
        });

        return menu;

    }

}
