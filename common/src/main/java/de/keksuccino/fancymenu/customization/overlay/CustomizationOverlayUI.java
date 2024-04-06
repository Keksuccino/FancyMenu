package de.keksuccino.fancymenu.customization.overlay;

import de.keksuccino.fancymenu.CreditsScreen;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.customgui.ManageCustomGuisScreen;
import de.keksuccino.fancymenu.customization.customgui.ManageOverriddenGuisScreen;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.ManageLayoutsScreen;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.screen.dummyscreen.DummyScreenBuilder;
import de.keksuccino.fancymenu.customization.screen.dummyscreen.DummyScreenRegistry;
import de.keksuccino.fancymenu.customization.variables.ManageVariablesScreen;
import de.keksuccino.fancymenu.util.*;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedEnumValueCycle;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.file.ResourceFile;
import de.keksuccino.fancymenu.util.file.type.FileCodec;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroups;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.NonStackableOverlayUI;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.NotificationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringListChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.preload.ManageResourcePreLoadScreen;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class CustomizationOverlayUI {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceSupplier<ITexture> FM_LOGO_TEXTURE_SUPPLIER = ResourceSupplier.image(ResourceSource.of("fancymenu:textures/menubar/icons/fancymenu_logo.png", ResourceSourceType.LOCATION).getSourceWithPrefix());
    private static final ResourceSupplier<ITexture> LEAVE_SCREEN_TEXTURE_SUPPLIER = ResourceSupplier.image(ResourceSource.of("fancymenu:textures/menubar/icons/exit.png", ResourceSourceType.LOCATION).getSourceWithPrefix());

    private static CustomizationOverlayMenuBar grandfatheredMenuBar = null;

    @NotNull
    public static DebugOverlay buildDebugOverlay(@NotNull MenuBar menuBar) {
        return DebugOverlayBuilder.buildDebugOverlay(menuBar);
    }

    @Nullable
    protected static MenuBar.ContextMenuBarEntry getScreenMenuBarTab() {
        MenuBar bar = CustomizationOverlay.getCurrentMenuBarInstance();
        if (bar != null) {
            MenuBar.MenuBarEntry tab = bar.getEntry("screen");
            if (tab instanceof MenuBar.ContextMenuBarEntry c) return c;
        }
        return null;
    }

    protected static void forScreenMenuBarTab(Consumer<MenuBar.ContextMenuBarEntry> task) {
        MenuBar.ContextMenuBarEntry tab = getScreenMenuBarTab();
        if (tab != null) {
            MainThreadTaskExecutor.executeInMainThread(() -> task.accept(tab), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }
    }

    @NotNull
    protected static CustomizationOverlayMenuBar buildMenuBar(boolean expanded) {

        CustomizationOverlayMenuBar grand = grandfatheredMenuBar;
        if (grand != null) {
            grandfatheredMenuBar = null;
            return grand;
        }

        CustomizationOverlayMenuBar menuBar = new CustomizationOverlayMenuBar();
        menuBar.setExpanded(expanded);
        Screen screen = Minecraft.getInstance().screen;
        String identifier = (screen != null) ? ScreenIdentifierHandler.getIdentifierOfScreen(screen) : null;

        // FANCYMENU ICON
        buildFMIconTabAndAddTo(menuBar);

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
                }))
                .setIsActiveSupplier((menu, entry) -> !(screen instanceof CustomGuiBaseScreen))
                .setTooltipSupplier((menu, entry) -> {
                    if (screen instanceof CustomGuiBaseScreen) return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.current_customization.cant_toggle_custom_guis"));
                    return null;
                })
                .setIcon(ContextMenu.IconFactory.getIcon("edit"));

        customizationMenu.addClickableEntry("copy_current_screen_identifier", Component.translatable("fancymenu.overlay.menu_bar.customization.copy_current_screen_identifier"), (menu, entry) -> {
            if (identifier != null) {
                Minecraft.getInstance().keyboardHandler.setClipboard(identifier);
                menu.closeMenu();
            }
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.copy_current_screen_identifier.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("copy"));

        customizationMenu.addClickableEntry("force_close_current_screen", Component.translatable("fancymenu.overlay.menu_bar.customization.close_current_screen"), (menu, entry) -> {
            menu.closeMenu();
            Minecraft.getInstance().setScreen(null);
        }).setIcon(ContextMenu.IconFactory.getIcon("exit"));

        customizationMenu.addSeparatorEntry("separator_after_override_current");

        ContextMenu layoutMenu = new ContextMenu();
        customizationMenu.addSubMenuEntry("layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout"), layoutMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("layout"));

        ContextMenu layoutNewMenu = new ContextMenu();
        layoutMenu.addSubMenuEntry("new_layout", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.new"), layoutNewMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("add"));

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
        layoutMenu.addSubMenuEntry("manage_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage"), layoutManageMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("edit"));

        ContextMenu layoutManageCurrentMenu = new ContextMenu();
        layoutManageMenu.addSubMenuEntry("manage_layouts_for_current", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.current"), layoutManageCurrentMenu)
                .setIsActiveSupplier((menu, entry) -> ScreenCustomization.isCustomizationEnabledForScreen(screen))
                .setTooltipSupplier((menu, entry) -> {
                    if (!ScreenCustomization.isCustomizationEnabledForScreen(screen)) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.layout.manage.current.disabled.tooltip"));
                    }
                    return null;
                });

        if (identifier != null) {
            int i = 0;
            List<Layout> allLayouts = LayoutHandler.getAllLayoutsForScreenIdentifier(identifier, false);
            int allLayoutsCount = allLayouts.size();
            for (Layout l : LayoutHandler.sortLayoutListByLastEdited(allLayouts, true, 8)) {
                layoutManageCurrentMenu.addSubMenuEntry("layout_" + i, Component.empty(), buildManageLayoutSubMenu(l, List.of("layouts", "manage_layouts", "manage_layouts_for_current")))
                        .setLabelSupplier((menu, entry) -> {
                            Style style = l.getStatus().getValueComponentStyle();
                            MutableComponent c = Component.literal(l.getLayoutName());
                            c.append(Component.literal(" (").setStyle(style));
                            c.append(l.getStatus().getValueComponent());
                            c.append(Component.literal(")").setStyle(style));
                            return c;
                        });
                i++;
            }
            if (allLayoutsCount > 8) {
                String moreLayoutCount = "" + (allLayoutsCount-8);
                layoutManageCurrentMenu.addClickableEntry("x_more_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.more_layouts", moreLayoutCount), (menu, entry) -> {
                    Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(identifier, false), screen, layouts -> {
                        Minecraft.getInstance().setScreen(screen);
                    }));
                });
            }
        }

        layoutManageCurrentMenu.addSeparatorEntry("layout.manage.current.separator_1");

        layoutManageCurrentMenu.addClickableEntry("layout_manage_current_all", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.all"), (menu, entry) -> {
            if (identifier != null) {
                Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(identifier, false), screen, layouts -> {
                    Minecraft.getInstance().setScreen(screen);
                }));
            }
        });

        ContextMenu layoutManageUniversalMenu = new ContextMenu();
        layoutManageMenu.addSubMenuEntry("layout_manage_universal", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.universal"), layoutManageUniversalMenu);

        int i = 0;
        List<Layout> allLayouts = LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true);
        int allLayoutsCount = allLayouts.size();
        for (Layout l : LayoutHandler.sortLayoutListByLastEdited(allLayouts, true, 8)) {
            layoutManageUniversalMenu.addSubMenuEntry("layout.manage.universal.recent_" + i, Component.empty(), buildManageLayoutSubMenu(l, List.of("layouts", "manage_layouts", "layout_manage_universal")))
                    .setLabelSupplier((menu, entry) -> {
                        Style style = l.getStatus().getValueComponentStyle();
                        MutableComponent c = Component.literal(l.getLayoutName());
                        c.append(Component.literal(" (").setStyle(style));
                        c.append(l.getStatus().getValueComponent());
                        c.append(Component.literal(")").setStyle(style));
                        return c;
                    });
            i++;
        }
        if (allLayoutsCount > 8) {
            String moreLayoutCount = "" + (allLayoutsCount-8);
            layoutManageUniversalMenu.addClickableEntry("x_more_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.more_layouts", moreLayoutCount), (menu, entry) -> {
                Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), null, (layouts -> {
                    Minecraft.getInstance().setScreen(screen);
                })));
            });
        }

        layoutManageUniversalMenu.addSeparatorEntry("layout.manage.universal.separator_1");

        layoutManageUniversalMenu.addClickableEntry("layout.manage.universal.all", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.all"), (menu, entry) -> {
            Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), null, (layouts -> {
                Minecraft.getInstance().setScreen(screen);
            })));
        });

        layoutMenu.addSeparatorEntry("separator_after_manage_layouts");

        if (identifier != null) {
            int layoutIndex = 0;
            List<Layout> allLayouts2 = LayoutHandler.getAllLayoutsForScreenIdentifier(identifier, false);
            allLayouts2.addAll(LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true));
//            allLayouts2.removeIf(layout -> !layout.isEnabled());
            int allLayoutsCount2 = allLayouts2.size();
            for (Layout layout : LayoutHandler.sortLayoutListByLastEdited(allLayouts2, true, 8)) {
                layoutMenu.addSubMenuEntry("layout_" + layoutIndex, Component.empty(), buildManageLayoutSubMenu(layout, List.of("layouts")))
                        .setLabelSupplier((menu, entry) -> {
                            Style style = layout.getStatus().getValueComponentStyle();
                            MutableComponent c = Component.literal(layout.getLayoutName());
                            c.append(Component.literal(" "));
                            if (layout.isUniversalLayout()) {
                                MutableComponent universalSuffix = Component.translatable("fancymenu.ui.customization_overlay.layouts.universal_suffix").setStyle(Style.EMPTY.withBold(true));
                                c.append(universalSuffix);
                            } else {
                                MutableComponent normalSuffix = Component.translatable("fancymenu.ui.customization_overlay.layouts.normal_suffix").setStyle(Style.EMPTY.withBold(true));
                                c.append(normalSuffix);
                            }
                            c.append(Component.literal(" (").setStyle(style));
                            c.append(layout.getStatus().getValueComponent());
                            c.append(Component.literal(")").setStyle(style));
                            return c;
                        });
                layoutIndex++;
            }
            if (allLayoutsCount2 > 8) {
                String moreLayoutCount = "" + (allLayoutsCount2-8);
                layoutMenu.addClickableEntry("x_more_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.more_layouts", moreLayoutCount), (menu, entry) -> {})
                        .setClickSoundEnabled(false)
                        .setChangeBackgroundColorOnHover(false);
            }
        }

        customizationMenu.addSeparatorEntry("separator_after_layout_menu");

        ContextMenu customizationSettingsMenu = new ContextMenu();
        customizationMenu.addSubMenuEntry("screen_settings", Component.translatable("fancymenu.overlay.menu_bar.customization.settings"), customizationSettingsMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("settings"));

        customizationSettingsMenu.addValueCycleEntry("advanced_customization_mode", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.advanced_customization_mode", FancyMenu.getOptions().advancedCustomizationMode.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().advancedCustomizationMode.setValue(cycle.getAsBoolean());
                })).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.advanced_customization_mode.tooltip")))
                .setIcon(ContextMenu.IconFactory.getIcon("warning"));

        customizationSettingsMenu.addSeparatorEntry("separator_after_advanced_mode");

        customizationSettingsMenu.addValueCycleEntry("play_menu_music", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.play_menu_music", FancyMenu.getOptions().playVanillaMenuMusic.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().playVanillaMenuMusic.setValue(cycle.getAsBoolean());
                    Minecraft.getInstance().getMusicManager().stopPlaying();
                    if (FancyMenu.getOptions().playVanillaMenuMusic.getValue()) {
                        Minecraft.getInstance().getMusicManager().startPlaying(Minecraft.getInstance().getSituationalMusic());
                    }
                }))
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        NonStackableOverlayUI.addIntegerInputContextMenuEntryTo(customizationSettingsMenu, "default_gui_scale",
                        Component.translatable("fancymenu.overlay.menu_bar.customization.settings.set_default_gui_scale"),
                        () -> FancyMenu.getOptions().defaultGuiScale.getValue(),
                        integer -> FancyMenu.getOptions().defaultGuiScale.setValue(integer),
                        true, FancyMenu.getOptions().defaultGuiScale.getDefaultValue(), null, null, (screen1, s) -> {
                            Minecraft.getInstance().setScreen(screen1);
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "default_gui_scale")));
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.set_default_gui_scale.tooltip")))
                .setIcon(ContextMenu.IconFactory.getIcon("measure"));

        customizationSettingsMenu.addValueCycleEntry("force_fullscreen", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.force_fullscreen", FancyMenu.getOptions().forceFullscreen.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().forceFullscreen.setValue(cycle.getAsBoolean());
                })).setIcon(ContextMenu.IconFactory.getIcon("fullscreen"));

        ContextMenu screenSettingsGameIntroMenu = new ContextMenu();
        customizationSettingsMenu.addSubMenuEntry("game_intro", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro"), screenSettingsGameIntroMenu)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.game_intro.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("video"));

        ResourceSupplier<ITexture> emptyImageSupplier = ResourceSupplier.empty(ITexture.class, FileMediaType.IMAGE);
        FileTypeGroup<ImageFileType> introFileTypeGroup = FileTypeGroup.of(FileTypes.APNG_IMAGE);

        NonStackableOverlayUI.addGenericResourceChooserContextMenuEntryTo(screenSettingsGameIntroMenu, "set_game_intro",
                () -> new ResourceChooserScreen<>(Component.empty(), introFileTypeGroup, null, s -> {}),
                ResourceSupplier::image, emptyImageSupplier,
                () -> {
                    if (GameIntroHandler.introIsAnimation()) return emptyImageSupplier;
                    if (FancyMenu.getOptions().gameIntroAnimation.getValue().trim().isEmpty()) return emptyImageSupplier;
                    return ResourceSupplier.image(FancyMenu.getOptions().gameIntroAnimation.getValue());
                },
                supplier -> {
                    FancyMenu.getOptions().gameIntroAnimation.setValue(supplier.getSourceWithPrefix());
                    forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "game_intro", "set_game_intro")));
                },
                Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro.set"), true, introFileTypeGroup, null, true, true, false);

        screenSettingsGameIntroMenu.addSeparatorEntry("separator_after_game_intro_set_animation");

        screenSettingsGameIntroMenu.addValueCycleEntry("game_intro_allow_skip", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.game_intro.allow_skip", FancyMenu.getOptions().gameIntroAllowSkip.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().gameIntroAllowSkip.setValue(cycle.getAsBoolean());
                }));

        screenSettingsGameIntroMenu.addValueCycleEntry("game_intro_fade_out", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.game_intro.fade_out", FancyMenu.getOptions().gameIntroFadeOut.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().gameIntroFadeOut.setValue(cycle.getAsBoolean());
                }));

        NonStackableOverlayUI.addInputContextMenuEntryTo(screenSettingsGameIntroMenu, "game_intro_set_custom_skip_text",
                        Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro.set_custom_skip_text"),
                        () -> {
                            String s = FancyMenu.getOptions().gameIntroCustomSkipText.getValue();
                            if (s.equals("")) return null;
                            return s;
                        },
                        s -> FancyMenu.getOptions().gameIntroCustomSkipText.setValue((s != null) ? s : ""),
                        true, null, null, false, false, null, null, (screen1, s) -> {
                            Minecraft.getInstance().setScreen(screen1);
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "game_intro", "game_intro_set_custom_skip_text")));
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.game_intro.set_custom_skip_text.tooltip")));

        customizationSettingsMenu.addValueCycleEntry("preload_animations", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.preload_animations", FancyMenu.getOptions().preLoadAnimations.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().preLoadAnimations.setValue(cycle.getAsBoolean());
                }));

        customizationSettingsMenu.addSeparatorEntry("separator_after_preload_animations");

        customizationSettingsMenu.addValueCycleEntry("singleplayer_world_icons", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.singleplayer_screen.world_icons", FancyMenu.getOptions().showSingleplayerScreenWorldIcons.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().showSingleplayerScreenWorldIcons.setValue(cycle.getAsBoolean());
                }));

        customizationSettingsMenu.addValueCycleEntry("multiplayer_server_icons", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.multiplayer_screen.server_icons", FancyMenu.getOptions().showMultiplayerScreenServerIcons.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().showMultiplayerScreenServerIcons.setValue(cycle.getAsBoolean());
                }));

        customizationSettingsMenu.addSeparatorEntry("separator_after_mp_server_icons");

        ContextMenu windowIconMenu = new ContextMenu();
        customizationSettingsMenu.addSubMenuEntry("window_icon", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon"), windowIconMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("image"));

        LocalizedEnumValueCycle<CommonCycles.CycleEnabledDisabled> windowIconToggleCycle = CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.toggle", FancyMenu.getOptions().showCustomWindowIcon.getValue())
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

        FileTypeGroup<?> pngFileGroup = FileTypeGroup.of(FileTypes.PNG_IMAGE);
        pngFileGroup.setDisplayName(FileTypeGroups.IMAGE_GROUP_COMPONENT);

        NonStackableOverlayUI.addFileChooserContextMenuEntryTo(windowIconMenu, "icon_16",
                        Component.empty(),
                        () -> FancyMenu.getOptions().customWindowIcon16.getValue(),
                        s -> {
                            ResourceFile f = ResourceFile.of(s);
                            FancyMenu.getOptions().customWindowIcon16.setValue(f.getShortPath());
                            FancyMenu.getOptions().showCustomWindowIcon.setValue(false);
                            windowIconToggleCycle.setCurrentValue(CommonCycles.CycleEnabledDisabled.DISABLED);
                            WindowHandler.resetWindowIcon();
                        },
                        true, FancyMenu.getOptions().customWindowIcon16.getDefaultValue(), null, pngFileGroup, (screen1, file) -> {
                            if (file != null) {
                                ITexture tex = ResourceHandlers.getImageHandler().get(ResourceSource.of(file.getPath(), ResourceSourceType.LOCAL));
                                if (tex != null) {
                                    tex.waitForReady(5000);
                                    if ((tex.getWidth() != 16) || (tex.getHeight() != 16)) {
                                        Minecraft.getInstance().setScreen(NotificationScreen.error(b -> {
                                            FancyMenu.getOptions().customWindowIcon16.setValue("");
                                            Minecraft.getInstance().setScreen(screen1);
                                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "window_icon")));
                                        }, LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.wrong_resolution", "16x16")));
                                    } else {
                                        Minecraft.getInstance().setScreen(screen1);
                                        forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "window_icon")));
                                    }
                                } else {
                                    Minecraft.getInstance().setScreen(screen1);
                                    forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "window_icon")));
                                }
                            } else {
                                Minecraft.getInstance().setScreen(screen1);
                                forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "window_icon")));
                            }
                        })
                .setLabelSupplier((menu, entry) -> {
                    MutableComponent notFound = Component.literal("✖").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
                    MutableComponent found = Component.literal("✔").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
                    //TODO übernehmen
                    File icon = WindowHandler.getCustomWindowIcon16();
                    if ((icon != null) && icon.isFile()) {
                        return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_16", found);
                    }
                    return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_16", notFound);
                    //------------------
                });

        NonStackableOverlayUI.addFileChooserContextMenuEntryTo(windowIconMenu, "icon_32",
                        Component.empty(),
                        () -> FancyMenu.getOptions().customWindowIcon32.getValue(),
                        s -> {
                            ResourceFile f = ResourceFile.of(s);
                            FancyMenu.getOptions().customWindowIcon32.setValue(f.getShortPath());
                            FancyMenu.getOptions().showCustomWindowIcon.setValue(false);
                            windowIconToggleCycle.setCurrentValue(CommonCycles.CycleEnabledDisabled.DISABLED);
                            WindowHandler.resetWindowIcon();
                        },
                        true, FancyMenu.getOptions().customWindowIcon32.getDefaultValue(), null, pngFileGroup, (screen1, file) -> {
                            if (file != null) {
                                ITexture tex = ResourceHandlers.getImageHandler().get(ResourceSource.of(file.getPath(), ResourceSourceType.LOCAL));
                                if (tex != null) {
                                    tex.waitForReady(5000);
                                    if ((tex.getWidth() != 32) || (tex.getHeight() != 32)) {
                                        Minecraft.getInstance().setScreen(NotificationScreen.error(b -> {
                                            FancyMenu.getOptions().customWindowIcon32.setValue("");
                                            Minecraft.getInstance().setScreen(screen1);
                                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "window_icon")));
                                        }, LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.wrong_resolution", "32x32")));
                                    } else {
                                        Minecraft.getInstance().setScreen(screen1);
                                        forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "window_icon")));
                                    }
                                } else {
                                    Minecraft.getInstance().setScreen(screen1);
                                    forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "window_icon")));
                                }
                            } else {
                                Minecraft.getInstance().setScreen(screen1);
                                forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "window_icon")));
                            }
                        })
                .setLabelSupplier((menu, entry) -> {
                    MutableComponent notFound = Component.literal("✖").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
                    MutableComponent found = Component.literal("✔").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
                    //TODO übernehmen
                    File icon = WindowHandler.getCustomWindowIcon32();
                    if ((icon != null) && icon.isFile()) {
                        return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_32", found);
                    }
                    return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_32", notFound);
                    //------------------------------
                });

        FileTypeGroup<?> macOsIconTypeGroup = FileTypeGroup.of(new ImageFileType(FileCodec.empty(ITexture.class), null, "icns"));
        macOsIconTypeGroup.setDisplayName(Component.translatable("fancymenu.file_types.icns"));

        NonStackableOverlayUI.addFileChooserContextMenuEntryTo(windowIconMenu, "icon_macos",
                        Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_macos"),
                        () -> FancyMenu.getOptions().customWindowIconMacOS.getValue(),
                        s -> {
                            ResourceFile f = ResourceFile.of(s);
                            FancyMenu.getOptions().customWindowIconMacOS.setValue(f.getShortPath());
                            FancyMenu.getOptions().showCustomWindowIcon.setValue(false);
                            windowIconToggleCycle.setCurrentValue(CommonCycles.CycleEnabledDisabled.DISABLED);
                            WindowHandler.resetWindowIcon();
                        },
                        true, FancyMenu.getOptions().customWindowIconMacOS.getDefaultValue(), null, macOsIconTypeGroup, (screen1, file) -> {
                            Minecraft.getInstance().setScreen(screen1);
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "window_icon")));
                        })
                .setLabelSupplier((menu, entry) -> {
                    MutableComponent notFound = Component.literal("✖").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
                    MutableComponent found = Component.literal("✔").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
                    //TODO übernehmen
                    File icon = WindowHandler.getCustomWindowIconMacOS();
                    if ((icon != null) && icon.isFile()) {
                        return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_macos", found);
                    }
                    return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_macos", notFound);
                    //---------------------------
                });

        windowIconMenu.addSeparatorEntry("separator_after_macos_icon");

        windowIconMenu.addClickableEntry("convert_png_to_macos_icon", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.convert_to_macos_icon"), (menu, entry) -> {
            WebUtils.openWebLink("https://miconv.com/convert-png-to-icns/");
        });

        NonStackableOverlayUI.addInputContextMenuEntryTo(customizationSettingsMenu, "window_title", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_title"),
                        () -> FancyMenu.getOptions().customWindowTitle.getValue(),
                        s -> {
                            FancyMenu.getOptions().customWindowTitle.setValue(s);
                            WindowHandler.updateWindowTitle();
                        }, true, FancyMenu.getOptions().customWindowTitle.getDefaultValue(), null, false, false, TextValidators.NO_EMPTY_STRING_SPACES_ALLOWED_TEXT_VALIDATOR, null, (screen1, s) -> {
                            Minecraft.getInstance().setScreen(screen1);
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "window_title")));
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.settings.custom_window_title.tooltip")))
                .setIcon(ContextMenu.IconFactory.getIcon("text"));

        customizationMenu.addSeparatorEntry("separator_after_settings");

        ContextMenu customGuiMenu = new ContextMenu();
        customizationMenu.addSubMenuEntry("custom_guis", Component.translatable("fancymenu.overlay.menu_bar.customization.custom_guis"), customGuiMenu)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.custom_guis.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("gui"));

        customGuiMenu.addClickableEntry("manage_custom_guis", Component.translatable("fancymenu.overlay.menu_bar.customization.custom_guis.manage"), (menu, entry) -> {
            Screen s = Minecraft.getInstance().screen;
            Minecraft.getInstance().setScreen(new ManageCustomGuisScreen(() -> Minecraft.getInstance().setScreen(s)));
        });

        customGuiMenu.addSeparatorEntry("separator_after_manage_custom_guis");

        customGuiMenu.addClickableEntry("override_current", Component.translatable("fancymenu.overlay.menu_bar.customization.custom_guis.override_current"), (menu, entry) -> {
                    Screen current = Minecraft.getInstance().screen;
                    if (!(current instanceof CustomGuiBaseScreen)) {
                        Minecraft.getInstance().setScreen(ConfirmationScreen.warning(override -> {
                            if (override) {
                                Minecraft.getInstance().setScreen(new StringListChooserScreen(Component.translatable("fancymenu.custom_guis.override.choose_custom"), CustomGuiHandler.getGuiIdentifiers(), s -> {
                                    CustomGuiBaseScreen customInstance = null;
                                    if (s != null) {
                                        CustomGuiHandler.overrideScreenWithCustomGui(current.getClass().getName(), s);
                                        //This is to avoid setting the Choose Custom GUI screen as parent of the custom GUI instance
                                        customInstance = CustomGuiHandler.constructInstance(s, null, current);
                                    }
                                    Minecraft.getInstance().setScreen((customInstance != null) ? customInstance : current);
                                }));
                            } else {
                                Minecraft.getInstance().setScreen(current);
                            }
                        }, LocalizationUtils.splitLocalizedLines("fancymenu.custom_guis.override.confirm")));
                    }
                }).setIsActiveSupplier((menu, entry) -> FancyMenu.getOptions().advancedCustomizationMode.getValue() && !(Minecraft.getInstance().screen instanceof CustomGuiBaseScreen))
                .setTooltipSupplier((menu, entry) -> {
                    if (!FancyMenu.getOptions().advancedCustomizationMode.getValue()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.custom_guis.override_current.disabled.tooltip"));
                    }
                    if (Minecraft.getInstance().screen instanceof CustomGuiBaseScreen) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.custom_guis.override_current.cant_override_custom"));
                    }
                    return null;
                });

        customGuiMenu.addClickableEntry("manage_overridden_screens", Component.translatable("fancymenu.custom_guis.manage_overridden"), (menu, entry) -> {
            Screen s = Minecraft.getInstance().screen;
            Minecraft.getInstance().setScreen(new ManageOverriddenGuisScreen(() -> Minecraft.getInstance().setScreen(s)));
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.custom_guis.manage_overridden.desc")));

        ContextMenu variablesMenu = new ContextMenu();
        customizationMenu.addSubMenuEntry("variables", Component.translatable("fancymenu.overlay.menu_bar.variables"), variablesMenu)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.variables.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"));

        variablesMenu.addClickableEntry("manage_variables", Component.translatable("fancymenu.overlay.menu_bar.variables.manage"), (menu, entry) -> {
            ManageVariablesScreen s = new ManageVariablesScreen(call -> {
                Minecraft.getInstance().setScreen(screen);
            });
            Minecraft.getInstance().setScreen(s);
        });

        customizationMenu.addClickableEntry("pre_load_resources", Component.translatable("fancymenu.resources.pre_loading"),
                        (menu, entry) -> {
                            Screen current = Minecraft.getInstance().screen;
                            ManageResourcePreLoadScreen s = new ManageResourcePreLoadScreen(aBoolean -> {
                                Minecraft.getInstance().setScreen(current);
                            });
                            Minecraft.getInstance().setScreen(s);
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.resources.pre_loading.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("check_list"));

        customizationMenu.addSeparatorEntry("separator_after_pre_load_resources");

        ContextMenu debugOverlayMenu = new ContextMenu();
        customizationMenu.addSubMenuEntry("debug_overlay", Component.translatable("fancymenu.overlay.debug"), debugOverlayMenu)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.debug.toggle.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"));

        debugOverlayMenu.addValueCycleEntry("toggle_debug_overlay", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.debug.toggle", FancyMenu.getOptions().showDebugOverlay.getValue())
                        .addCycleListener(cycleEnabledDisabled -> {
                            FancyMenu.getOptions().showDebugOverlay.setValue(cycleEnabledDisabled.getAsBoolean());
                        }))
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.debug.toggle.shortcut"));

        debugOverlayMenu.addSeparatorEntry("separator_after_toggle_debug_overlay");

        debugOverlayMenu.addValueCycleEntry("toggle_debug_overlay_category_screen_basic", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.debug.basic_screen_category", FancyMenu.getOptions().debugOverlayShowBasicScreenCategory.getValue())
                .addCycleListener(cycleEnabledDisabled -> {
                    FancyMenu.getOptions().debugOverlayShowBasicScreenCategory.setValue(cycleEnabledDisabled.getAsBoolean());
                    CustomizationOverlay.rebuildDebugOverlay();
                }));

        debugOverlayMenu.addValueCycleEntry("toggle_debug_overlay_category_screen_advanced", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.debug.advanced_screen_category", FancyMenu.getOptions().debugOverlayShowAdvancedScreenCategory.getValue())
                .addCycleListener(cycleEnabledDisabled -> {
                    FancyMenu.getOptions().debugOverlayShowAdvancedScreenCategory.setValue(cycleEnabledDisabled.getAsBoolean());
                    CustomizationOverlay.rebuildDebugOverlay();
                }));

        debugOverlayMenu.addValueCycleEntry("toggle_debug_overlay_category_resources", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.debug.resources_category", FancyMenu.getOptions().debugOverlayShowResourcesCategory.getValue())
                .addCycleListener(cycleEnabledDisabled -> {
                    FancyMenu.getOptions().debugOverlayShowResourcesCategory.setValue(cycleEnabledDisabled.getAsBoolean());
                    CustomizationOverlay.rebuildDebugOverlay();
                }));

        debugOverlayMenu.addValueCycleEntry("toggle_debug_overlay_category_system", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.debug.system_category", FancyMenu.getOptions().debugOverlayShowSystemCategory.getValue())
                .addCycleListener(cycleEnabledDisabled -> {
                    FancyMenu.getOptions().debugOverlayShowSystemCategory.setValue(cycleEnabledDisabled.getAsBoolean());
                    CustomizationOverlay.rebuildDebugOverlay();
                }));

        customizationMenu.addClickableEntry("reload_fancymenu", Component.translatable("fancymenu.overlay.menu_bar.customization.reload_fancymenu"), (menu, entry) -> {
            MainThreadTaskExecutor.executeInMainThread(() -> {
                grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                ScreenCustomization.reloadFancyMenu();
            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.customization.reload_fancymenu.shortcut"))
                .setIcon(ContextMenu.IconFactory.getIcon("reload"));

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
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.disable_customization_for_all.tooltip")))
                .setIcon(ContextMenu.IconFactory.getIcon("warning"));

        customizationMenu.addSeparatorEntry("separator_before_hide_menu_bar");

        customizationMenu.addClickableEntry("hide_menu_bar", Component.translatable("fancymenu.overlay.menu_bar.customization.hide_overlay"), (menu, entry) -> {
            Minecraft.getInstance().setScreen(ConfirmationScreen.critical((call) -> {
                if (call) {
                    FancyMenu.getOptions().showCustomizationOverlay.setValue(!FancyMenu.getOptions().showCustomizationOverlay.getValue());
                }
                Minecraft.getInstance().setScreen(screen);
            }, LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.customization.hide_overlay.confirm")).setDelay(4000));
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.customization.hide_overlay.shortcut"))
                .setIcon(ContextMenu.IconFactory.getIcon("close"));

        // TOOLS
        ContextMenu toolsMenu = new ContextMenu();
        menuBar.addContextMenuEntry("tools", Component.translatable("fancymenu.overlay.menu_bar.tools"), toolsMenu);

        ContextMenu dummyScreenMenu = new ContextMenu();
        toolsMenu.addSubMenuEntry("dummy_screens", Component.translatable("fancymenu.overlay.menu_bar.tools.dummy_screen_instances"), dummyScreenMenu)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.tools.dummy_screen_instances.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("gui"));

        int builderCount = 1;
        for (DummyScreenBuilder builder : DummyScreenRegistry.getBuilders()) {
            ContextMenu.ClickableContextMenuEntry<?> entry = dummyScreenMenu.addClickableEntry("builder_" + builderCount, builder.getScreenDisplayName(), (menu, entry2) -> Minecraft.getInstance().setScreen(builder.tryConstruct()));
            if (builder.getScreenDescriptionSupplier() != null) {
                entry.setTooltipSupplier((menu, entry1) -> Tooltip.of(builder.getScreenDescriptionSupplier().get().toArray(new Component[0])));
            }
            builderCount++;
        }

        // UI
        buildUITabAndAddTo(menuBar);

        // HELP
        buildHelpTabAndAddTo(menuBar);

        //LEAVE CURRENT SCREEN BUTTON
        menuBar.addClickableEntry(MenuBar.Side.RIGHT, "leave_current_screen", Component.empty(), (bar, entry) -> {
            Minecraft.getInstance().setScreen(null);
        }).setIconTextureSupplier((bar, entry) -> LEAVE_SCREEN_TEXTURE_SUPPLIER.get())
                .setTooltipSupplier(consumes -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.leave_current_menu.desc")));

        return menuBar;

    }

    public static MenuBar.ClickableMenuBarEntry buildFMIconTabAndAddTo(MenuBar menuBar) {
        return menuBar.addClickableEntry(MenuBar.Side.LEFT, "fancymenu_icon", Component.empty(), (bar, entry) -> {}).setIconTextureSupplier((bar, entry) -> FM_LOGO_TEXTURE_SUPPLIER.get()).setActive(false);
    }

    public static ContextMenu buildHelpTabAndAddTo(MenuBar menuBar) {

        ContextMenu helpMenu = new ContextMenu();
        menuBar.addContextMenuEntry("help", Component.translatable("fancymenu.overlay.menu_bar.help"), helpMenu);

        helpMenu.addClickableEntry("fancymenu_wiki", Component.translatable("fancymenu.overlay.menu_bar.help.wiki"), (menu, entry) -> {
            WebUtils.openWebLink("https://docs.fancymenu.net");
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.help.wiki.tooltip")))
                .setIcon(ContextMenu.IconFactory.getIcon("book"));

        helpMenu.addClickableEntry("join_the_discord", Component.translatable("fancymenu.overlay.menu_bar.help.discord"), (menu, entry) -> {
            WebUtils.openWebLink("https://discord.gg/UzmeWkD");
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.help.discord.tooltip")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

        helpMenu.addClickableEntry("report_issue", Component.translatable("fancymenu.overlay.menu_bar.help.report_issue"), (menu, entry) -> {
            WebUtils.openWebLink("https://github.com/Keksuccino/FancyMenu/issues");
        }).setIcon(ContextMenu.IconFactory.getIcon("notes"));

        helpMenu.addSeparatorEntry("separator_after_report_issue");

        helpMenu.addClickableEntry("curseforge_fancymenu_category", Component.translatable("fancymenu.overlay.menu_bar.help.curseforge_fancymenu_category"), (menu, entry) -> {
            WebUtils.openWebLink("https://www.curseforge.com/minecraft/search?page=1&class=customization&categoryIds=5186");
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.help.curseforge_fancymenu_category.tooltip")))
                .setIcon(ContextMenu.IconFactory.getIcon("curseforge"));

        helpMenu.addSeparatorEntry("separator_after_curseforge");

        helpMenu.addClickableEntry("keksuccino_patreon", Component.translatable("fancymenu.overlay.menu_bar.help.patreon"), (menu, entry) -> {
            WebUtils.openWebLink("https://www.patreon.com/keksuccino");
        }).setIcon(ContextMenu.IconFactory.getIcon("patreon"));

        helpMenu.addClickableEntry("paypal_tip_jar", Component.translatable("fancymenu.overlay.menu_bar.help.paypal"), (menu, entry) -> {
            WebUtils.openWebLink("https://www.paypal.com/paypalme/TimSchroeter");
        }).setIcon(ContextMenu.IconFactory.getIcon("coin"));

        helpMenu.addSeparatorEntry("separator_after_paypal");

        helpMenu.addClickableEntry("credits", Component.translatable("fancymenu.credits"), (menu, entry) -> {
            Minecraft.getInstance().setScreen(new CreditsScreen((Minecraft.getInstance().screen != null) ? Minecraft.getInstance().screen : new TitleScreen()));
        });

        return helpMenu;

    }

    public static ContextMenu buildUITabAndAddTo(MenuBar menuBar) {

        ContextMenu userInterfaceMenu = new ContextMenu();
        menuBar.addContextMenuEntry("user_interface", Component.translatable("fancymenu.overlay.menu_bar.user_interface"), userInterfaceMenu);

        int preSelectedUiScale = (int)FancyMenu.getOptions().uiScale.getValue().floatValue();
        if ((preSelectedUiScale != 1) && (preSelectedUiScale != 2) && (preSelectedUiScale != 3) && (preSelectedUiScale != 4)) {
            preSelectedUiScale = 4;
        }
        userInterfaceMenu.addValueCycleEntry("ui_scale", CommonCycles.cycle("fancymenu.overlay.menu_bar.user_interface.ui_scale", ListUtils.of("1","2","3","4"), "" + preSelectedUiScale)
                .addCycleListener(scaleString -> {
                    if (!MathUtils.isFloat(scaleString)) {
                        scaleString = "4";
                    }
                    FancyMenu.getOptions().uiScale.setValue(Float.valueOf(scaleString));
                    userInterfaceMenu.closeMenu();
                }).setValueNameSupplier(value -> {
                    if (value.equals("4")) return I18n.get("fancymenu.overlay.menu_bar.user_interface.ui_scale.auto");
                    return value;
                }))
                .setIcon(ContextMenu.IconFactory.getIcon("measure"));

        userInterfaceMenu.addValueCycleEntry("ui_text_shadow", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.user_interface.ui_text_shadow", FancyMenu.getOptions().enableUiTextShadow.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().enableUiTextShadow.setValue(cycle.getAsBoolean());
                }))
                .setIcon(ContextMenu.IconFactory.getIcon("shadow"));

        userInterfaceMenu.addValueCycleEntry("ui_click_sounds", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.user_interface.ui_click_sounds", FancyMenu.getOptions().playUiClickSounds.getValue())
                .addCycleListener(cycle -> {
                    if (cycle.getAsBoolean()) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    FancyMenu.getOptions().playUiClickSounds.setValue(cycle.getAsBoolean());
                })).setClickSoundEnabled(false)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        int preSelectedContextHoverOpenSpeed = FancyMenu.getOptions().contextMenuHoverOpenSpeed.getValue();
        if ((preSelectedContextHoverOpenSpeed != 1) && (preSelectedContextHoverOpenSpeed != 2) && (preSelectedContextHoverOpenSpeed != 3)) {
            preSelectedContextHoverOpenSpeed = 3;
        }
        userInterfaceMenu.addValueCycleEntry("context_menu_hover_open_speed", CommonCycles.cycle("fancymenu.overlay.menu_bar.user_interface.context_menu_hover_open_speed", ListUtils.of("1","2","3"), "" + preSelectedContextHoverOpenSpeed)
                        .addCycleListener(speedString -> {
                            if (!MathUtils.isInteger(speedString)) {
                                speedString = "3";
                            }
                            FancyMenu.getOptions().contextMenuHoverOpenSpeed.setValue(Integer.parseInt(speedString));
                        }))
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.user_interface.context_menu_hover_open_speed.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("timer"));

        ContextMenu windowUiThemeMenu = new ContextMenu();
        userInterfaceMenu.addSubMenuEntry("ui_theme", Component.translatable("fancymenu.overlay.menu_bar.user_interface.ui_theme"), windowUiThemeMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("edit"));

        int i2 = 0;
        for (UIColorTheme s : UIColorThemeRegistry.getThemes()) {
            windowUiThemeMenu.addClickableEntry("ui_theme_" + i2, s.getDisplayName(), (menu, entry) -> {
                FancyMenu.getOptions().uiTheme.setValue(s.getIdentifier());
                UIColorThemeRegistry.setActiveTheme(s.getIdentifier());
            });
            i2++;
        }

        return userInterfaceMenu;

    }

    @NotNull
    protected static ContextMenu buildManageLayoutSubMenu(Layout layout, @NotNull List<String> entryPath) {

        // layouts -> manage_layouts -> manage_layouts_for_current
        // layouts -> manage_layouts -> layout_manage_universal

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
        }).setIcon(ContextMenu.IconFactory.getIcon("edit"));

        menu.addClickableEntry("delete_layout", Component.translatable("fancymenu.layout.manage.delete"), (menu1, entry) -> {
            menu1.closeMenu();
            Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings(call -> {
                if (call) {
                    layout.delete(false);
                }
                Minecraft.getInstance().setScreen(screen);
                forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(entryPath));
            }, LocalizationUtils.splitLocalizedStringLines("fancymenu.layout.manage.delete.confirm")));
        }).setIcon(ContextMenu.IconFactory.getIcon("delete"));

        menu.addClickableEntry("edit_in_system_text_editor", Component.translatable("fancymenu.layout.manage.open_in_text_editor"), (menu1, entry) -> {
            if (layout.layoutFile != null) {
                FileUtils.openFile(layout.layoutFile);
            }
        });

        return menu;

    }

}
