package de.keksuccino.fancymenu.customization.overlay;

import com.google.common.io.Files;
import de.keksuccino.fancymenu.CreditsScreen;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.customgui.ManageCustomGuisScreen;
import de.keksuccino.fancymenu.customization.customgui.ManageOverriddenGuisScreen;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.customization.global.ManageGlobalMenuMusicTracksScreen;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.ChoosePanoramaScreen;
import de.keksuccino.fancymenu.customization.listener.gui.ManageListenersScreen;
import de.keksuccino.fancymenu.customization.scheduler.gui.ManageSchedulersScreen;
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
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothTextRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIconTexture;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringListChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizer;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizerHandler;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorThemeRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.preload.ManageResourcePreLoadScreen;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.fancymenu.util.MathUtils;
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
    private static final MaterialIconTexture LEAVE_SCREEN_ICON_TEXTURE = new MaterialIconTexture(MaterialIcons.EXIT_TO_APP, UIBase::getUIMaterialIconTextureSizeMedium);

    private static CustomizationOverlayMenuBar grandfatheredMenuBar = null;

    @NotNull
    public static DebugOverlay buildDebugOverlay() {
        return DebugOverlayBuilder.buildDebugOverlay();
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

        ResourceSupplier<ITexture> emptyImageSupplier = ResourceSupplier.empty(ITexture.class, FileMediaType.IMAGE);

        CustomizationOverlayMenuBar menuBar = new CustomizationOverlayMenuBar();
        menuBar.setExpanded(expanded);
        @Nullable final Screen screen = Minecraft.getInstance().screen;
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
                .addIsActiveSupplier((menu, entry) -> !(screen instanceof CustomGuiBaseScreen))
                .setTooltipSupplier((menu, entry) -> {
                    if (screen instanceof CustomGuiBaseScreen) return UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.current_customization.cant_toggle_custom_guis"));
                    return null;
                })
                .setIcon(MaterialIcons.TOGGLE_ON);

        customizationMenu.addClickableEntry("copy_current_screen_identifier", Component.translatable("fancymenu.overlay.menu_bar.customization.copy_current_screen_identifier"), (menu, entry) -> {
            if (identifier != null) {
                Minecraft.getInstance().keyboardHandler.setClipboard(identifier);
                menu.closeMenu();
            }
        }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.copy_current_screen_identifier.desc")))
                .setIcon(MaterialIcons.CONTENT_COPY);

        customizationMenu.addValueCycleEntry("normalize_current_scroll_screen", CommonCycles.cycleEnabledDisabled("fancymenu.scroll_screen_normalizer", ScrollScreenNormalizerHandler.shouldNormalize(screen))
                        .addCycleListener(cycle -> {
                            MainThreadTaskExecutor.executeInMainThread(() -> {
                                grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                                if (screen == null) return;
                                ScrollScreenNormalizerHandler.setForScreen(screen, cycle.getAsBoolean());
                                ScreenCustomization.reInitCurrentScreen();
                            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                        }))
                .addIsActiveSupplier((menu, entry) -> !ScrollScreenNormalizer.isBlacklisted(screen))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.scroll_screen_normalizer.desc")))
                .setIcon(MaterialIcons.SWIPE);

        customizationMenu.addClickableEntry("force_close_current_screen", Component.translatable("fancymenu.overlay.menu_bar.customization.close_current_screen"), (menu, entry) -> {
            menu.closeMenu();
            Minecraft.getInstance().setScreen(null);
        }).setIcon(MaterialIcons.EXIT_TO_APP);

        customizationMenu.addSeparatorEntry("separator_after_override_current");

        ContextMenu layoutMenu = new ContextMenu();
        customizationMenu.addSubMenuEntry("layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout"), layoutMenu)
                .setIcon(MaterialIcons.GALLERY_THUMBNAIL);

        ContextMenu layoutNewMenu = new ContextMenu();
        layoutMenu.addSubMenuEntry("new_layout", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.new"), layoutNewMenu)
                .setIcon(MaterialIcons.ADD);

        layoutNewMenu.addClickableEntry("new_layout_for_current", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.new.current"), (menu, entry) -> {
                    if (screen != null) {
                        LayoutHandler.openLayoutEditor(new Layout(screen), screen);
                    }
                }).addIsActiveSupplier((menu, entry) -> ScreenCustomization.isCustomizationEnabledForScreen(screen))
                .setTooltipSupplier((menu, entry) -> {
                    if (!ScreenCustomization.isCustomizationEnabledForScreen(screen)) {
                        return UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.layout.new.current.disabled.tooltip"));
                    }
                    return null;
                })
                .setIcon(MaterialIcons.ADD);

        layoutNewMenu.addClickableEntry("new_universal_layout", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.new.universal"), (menu, entry) -> {
            LayoutHandler.openLayoutEditor(new Layout(), null);
        }).setIcon(MaterialIcons.ADD);

        layoutMenu.addSeparatorEntry("separator_after_new_layouts");

        if (identifier != null) {
            int layoutIndex = 0;
            List<Layout> normalLayouts = LayoutHandler.getAllLayoutsForScreenIdentifier(identifier, false);
            LayoutHandler.sortLayoutListByLastEdited(normalLayouts, true);
            for (Layout layout : normalLayouts) {
                layoutMenu.addSubMenuEntry("layout_normal_" + layoutIndex, Component.empty(), buildManageLayoutSubMenu(layout, List.of("layouts")))
                        .setLabelSupplier((menu, entry) -> {
                            Style style = layout.getStatus().getValueComponentStyle();
                            MutableComponent c = Component.literal(layout.getLayoutName());
                            c.append(Component.literal(" "));
                            MutableComponent normalSuffix = Component.translatable("fancymenu.ui.customization_overlay.layouts.normal_suffix").setStyle(Style.EMPTY.withBold(true));
                            c.append(normalSuffix);
                            c.append(Component.literal(" (").setStyle(style));
                            c.append(layout.getStatus().getValueComponent());
                            c.append(Component.literal(")").setStyle(style));
                            return c;
                        })
                        .setIcon(MaterialIcons.GALLERY_THUMBNAIL);
                layoutIndex++;
            }
        }

        layoutMenu.addSeparatorEntry("separator_after_normal_layouts");

        int layoutIndex = 0;
        List<Layout> universalLayouts = LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true);
        LayoutHandler.sortLayoutListByLastEdited(universalLayouts, true);
        for (Layout layout : universalLayouts) {
            layoutMenu.addSubMenuEntry("layout_universal_" + layoutIndex, Component.empty(), buildManageLayoutSubMenu(layout, List.of("layouts")))
                    .setLabelSupplier((menu, entry) -> {
                        Style style = layout.getStatus().getValueComponentStyle();
                        MutableComponent c = Component.literal(layout.getLayoutName());
                        c.append(Component.literal(" "));
                        MutableComponent universalSuffix = Component.translatable("fancymenu.ui.customization_overlay.layouts.universal_suffix").setStyle(Style.EMPTY.withBold(true));
                        c.append(universalSuffix);
                        c.append(Component.literal(" (").setStyle(style));
                        c.append(layout.getStatus().getValueComponent());
                        c.append(Component.literal(")").setStyle(style));
                        return c;
                    })
                    .setIcon(MaterialIcons.GALLERY_THUMBNAIL);
            layoutIndex++;
        }

        customizationMenu.addSeparatorEntry("separator_after_layout_menu");

        customizationMenu.addSubMenuEntry("global_customizations", Component.translatable("fancymenu.overlay.menu_bar.customization.global_customizations"), buildGlobalCustomizationMenu(emptyImageSupplier))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.global_customizations.desc")))
                .setIcon(MaterialIcons.TUNE);

        customizationMenu.addSeparatorEntry("separator_after_global_customizations");

        customizationMenu.addSubMenuEntry("screen_settings", Component.translatable("fancymenu.overlay.menu_bar.customization.settings"), buildSettingsMenu(emptyImageSupplier))
                .setIcon(MaterialIcons.SETTINGS);

        customizationMenu.addSeparatorEntry("separator_after_settings");

        ContextMenu customGuiMenu = new ContextMenu();
        customizationMenu.addSubMenuEntry("custom_guis", Component.translatable("fancymenu.overlay.menu_bar.customization.custom_guis"), customGuiMenu)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.custom_guis.desc")))
                .setIcon(MaterialIcons.CONTEXTUAL_TOKEN);

        customGuiMenu.addClickableEntry("manage_custom_guis", Component.translatable("fancymenu.overlay.menu_bar.customization.custom_guis.manage"), (menu, entry) -> {
            ManageCustomGuisScreen manageCustomGuisScreen = new ManageCustomGuisScreen(() -> {});
            ManageCustomGuisScreen.openInWindow(manageCustomGuisScreen);
            menu.closeMenuChain();
        }).setIcon(MaterialIcons.EDIT);

        customGuiMenu.addSeparatorEntry("separator_after_manage_custom_guis");

        customGuiMenu.addClickableEntry("override_current", Component.translatable("fancymenu.overlay.menu_bar.customization.custom_guis.override_current"), (menu, entry) -> {
                    Screen current = Minecraft.getInstance().screen;
                    if (!(current instanceof CustomGuiBaseScreen)) {
                        Dialogs.openMessageWithCallback(Component.translatable("fancymenu.custom_guis.override.confirm"), MessageDialogStyle.WARNING, override -> {
                            if (override) {
                                StringListChooserScreen chooserScreen = new StringListChooserScreen(Component.translatable("fancymenu.custom_guis.override.choose_custom"), CustomGuiHandler.getGuiIdentifiers(), s -> {
                                    CustomGuiBaseScreen customInstance = null;
                                    if (s != null) {
                                        CustomGuiHandler.overrideScreenWithCustomGui(current.getClass().getName(), s);
                                        //This is to avoid setting the Choose Custom GUI screen as parent of the custom GUI instance
                                        customInstance = CustomGuiHandler.constructInstance(s, null, current);
                                    }
                                    Minecraft.getInstance().setScreen((customInstance != null) ? customInstance : current);
                                });
                                StringListChooserScreen.openInWindow(chooserScreen);
                                menu.closeMenuChain();
                            }
                        }).setDelay(2000);
                    }
                }).addIsActiveSupplier((menu, entry) -> FancyMenu.getOptions().advancedCustomizationMode.getValue() && !(Minecraft.getInstance().screen instanceof CustomGuiBaseScreen))
                .setTooltipSupplier((menu, entry) -> {
                    if (!FancyMenu.getOptions().advancedCustomizationMode.getValue()) {
                        return UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.custom_guis.override_current.disabled.tooltip"));
                    }
                    if (Minecraft.getInstance().screen instanceof CustomGuiBaseScreen) {
                        return UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.custom_guis.override_current.cant_override_custom"));
                    }
                    return null;
                })
                .setIcon(MaterialIcons.LINK);

        customGuiMenu.addClickableEntry("manage_overridden_screens", Component.translatable("fancymenu.custom_guis.manage_overridden"), (menu, entry) -> {
            ManageOverriddenGuisScreen manageOverriddenGuisScreen = new ManageOverriddenGuisScreen(() -> {});
            ManageOverriddenGuisScreen.openInWindow(manageOverriddenGuisScreen);
            menu.closeMenuChain();
        }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.custom_guis.manage_overridden.desc")))
                .setIcon(MaterialIcons.SCREEN_SHARE);

        customizationMenu.addClickableEntry("variables", Component.translatable("fancymenu.overlay.menu_bar.variables.manage"), (menu, entry) -> {
                    ManageVariablesScreen s = new ManageVariablesScreen(call -> {});
                    ManageVariablesScreen.openInWindow(s);
                    menu.closeMenuChain();
                })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.variables.desc")))
                .setIcon(MaterialIcons.CODE);

        customizationMenu.addClickableEntry("manage_listeners", Component.translatable("fancymenu.listeners.manage"), (menu, entry) -> {
                    ManageListenersScreen s = new ManageListenersScreen(call -> {
                    });
                    ManageListenersScreen.openInWindow(s);
                    menu.closeMenuChain();
                })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.listeners.manage.desc")))
                .setIcon(MaterialIcons.HEARING);

        customizationMenu.addClickableEntry("manage_schedulers", Component.translatable("fancymenu.schedulers.manage"), (menu, entry) -> {
                    ManageSchedulersScreen s = new ManageSchedulersScreen(call -> {
                    });
                    ManageSchedulersScreen.openInWindow(s);
                    menu.closeMenuChain();
                })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.schedulers.manage.desc")))
                .setIcon(MaterialIcons.TIMER);

        customizationMenu.addClickableEntry("pre_load_resources", Component.translatable("fancymenu.resources.pre_loading"),
                        (menu, entry) -> {
                            ManageResourcePreLoadScreen s = new ManageResourcePreLoadScreen(aBoolean -> {});
                            ManageResourcePreLoadScreen.openInWindow(s);
                            menu.closeMenuChain();
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.resources.pre_loading.desc")))
                .setIcon(MaterialIcons.CHECKLIST);

        customizationMenu.addSeparatorEntry("separator_after_pre_load_resources");

        ContextMenu debugOverlayMenu = new ContextMenu();
        customizationMenu.addSubMenuEntry("debug_overlay", Component.translatable("fancymenu.overlay.debug"), debugOverlayMenu)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.debug.toggle.desc")))
                .setIcon(MaterialIcons.TERMINAL);

        debugOverlayMenu.addValueCycleEntry("toggle_debug_overlay", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.debug.toggle", FancyMenu.getOptions().showDebugOverlay.getValue())
                        .addCycleListener(cycleEnabledDisabled -> {
                            FancyMenu.getOptions().showDebugOverlay.setValue(cycleEnabledDisabled.getAsBoolean());
                            ScreenCustomization.reInitCurrentScreen();
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("debug_overlay")));
                        }))
                .setIcon(MaterialIcons.VISIBILITY)
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.debug.toggle.shortcut"));

        debugOverlayMenu.addSeparatorEntry("separator_after_toggle_debug_overlay");

        debugOverlayMenu.addValueCycleEntry("toggle_debug_overlay_category_screen_basic", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.debug.basic_screen_category", FancyMenu.getOptions().debugOverlayShowBasicScreenCategory.getValue())
                .addCycleListener(cycleEnabledDisabled -> {
                    FancyMenu.getOptions().debugOverlayShowBasicScreenCategory.setValue(cycleEnabledDisabled.getAsBoolean());
                    CustomizationOverlay.refreshDebugOverlay();
                }))
                .setIcon(MaterialIcons.SCREEN_SHARE);

        debugOverlayMenu.addValueCycleEntry("toggle_debug_overlay_category_screen_advanced", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.debug.advanced_screen_category", FancyMenu.getOptions().debugOverlayShowAdvancedScreenCategory.getValue())
                .addCycleListener(cycleEnabledDisabled -> {
                    FancyMenu.getOptions().debugOverlayShowAdvancedScreenCategory.setValue(cycleEnabledDisabled.getAsBoolean());
                    CustomizationOverlay.refreshDebugOverlay();
                }))
                .setIcon(MaterialIcons.WIDGETS);

        debugOverlayMenu.addValueCycleEntry("toggle_debug_overlay_category_resources", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.debug.resources_category", FancyMenu.getOptions().debugOverlayShowResourcesCategory.getValue())
                .addCycleListener(cycleEnabledDisabled -> {
                    FancyMenu.getOptions().debugOverlayShowResourcesCategory.setValue(cycleEnabledDisabled.getAsBoolean());
                    CustomizationOverlay.refreshDebugOverlay();
                }))
                .setIcon(MaterialIcons.STORAGE);

        debugOverlayMenu.addValueCycleEntry("toggle_debug_overlay_category_system", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.debug.system_category", FancyMenu.getOptions().debugOverlayShowSystemCategory.getValue())
                .addCycleListener(cycleEnabledDisabled -> {
                    FancyMenu.getOptions().debugOverlayShowSystemCategory.setValue(cycleEnabledDisabled.getAsBoolean());
                    CustomizationOverlay.refreshDebugOverlay();
                }))
                .setIcon(MaterialIcons.SETTINGS);

        customizationMenu.addClickableEntry("reload_fancymenu", Component.translatable("fancymenu.overlay.menu_bar.customization.reload_fancymenu"), (menu, entry) -> {
            MainThreadTaskExecutor.executeInMainThread(() -> {
                grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                ScreenCustomization.reloadFancyMenu();
            }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.customization.reload_fancymenu.shortcut"))
                .setIcon(MaterialIcons.REFRESH);

        customizationMenu.addClickableEntry("disable_customization_for_all", Component.translatable("fancymenu.overlay.menu_bar.customization.disable_customization_for_all"), (menu, entry) -> {
            Dialogs.openMessageWithCallback(Component.translatable("fancymenu.overlay.menu_bar.customization.disable_customization_for_all.confirm"), MessageDialogStyle.WARNING, call -> {
                if (call) {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
                        ScreenCustomization.disableCustomizationForAllScreens();
                        ScreenCustomization.reInitCurrentScreen();
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
            }).setDelay(4000);
        }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.disable_customization_for_all.tooltip")))
                .setIcon(MaterialIcons.WARNING);

        customizationMenu.addSeparatorEntry("separator_before_hide_menu_bar");

        customizationMenu.addClickableEntry("hide_menu_bar", Component.translatable("fancymenu.overlay.menu_bar.customization.hide_overlay"), (menu, entry) -> {
            menuBar.closeAllContextMenus();
            Dialogs.openMessageWithCallback(Component.translatable("fancymenu.overlay.menu_bar.customization.hide_overlay.confirm"), MessageDialogStyle.ERROR, call -> {
                if (call) {
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        FancyMenu.getOptions().showCustomizationOverlay.setValue(!FancyMenu.getOptions().showCustomizationOverlay.getValue());
                        ScreenCustomization.reInitCurrentScreen();
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
            }).setDelay(4000);
        }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.overlay.menu_bar.customization.hide_overlay.shortcut"))
                .setIcon(MaterialIcons.CLOSE);

        // TOOLS
        ContextMenu toolsMenu = new ContextMenu();
        menuBar.addContextMenuEntry("tools", Component.translatable("fancymenu.overlay.menu_bar.tools"), toolsMenu);

        ContextMenu dummyScreenMenu = new ContextMenu();
        toolsMenu.addSubMenuEntry("dummy_screens", Component.translatable("fancymenu.overlay.menu_bar.tools.dummy_screen_instances"), dummyScreenMenu)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.tools.dummy_screen_instances.desc")))
                .setIcon(MaterialIcons.WIDGETS);

        int builderCount = 1;
        for (DummyScreenBuilder builder : DummyScreenRegistry.getBuilders()) {
            ContextMenu.ClickableContextMenuEntry<?> entry = dummyScreenMenu.addClickableEntry("builder_" + builderCount, builder.getScreenDisplayName(), (menu, entry2) -> Minecraft.getInstance().setScreen(builder.tryConstruct()))
                    .setIcon(MaterialIcons.SCREEN_SHARE);
            if (builder.getScreenDescriptionSupplier() != null) {
                entry.setTooltipSupplier((menu, entry1) -> UITooltip.of(builder.getScreenDescriptionSupplier().get().toArray(new Component[0])));
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
        }).setIconTexture(LEAVE_SCREEN_ICON_TEXTURE)
                .setIconTextureColor(() -> UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_icon_texture_color : UIBase.getUITheme().ui_icon_texture_color)
                .setBaseWidth(MenuBar.PIXEL_SIZE)
                .setIconPaddingSupplier(consumes -> 7)
                .setTooltipSupplier(consumes -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.leave_current_menu.desc")));

        return menuBar;

    }

    public static ContextMenu buildSettingsMenu(@NotNull ResourceSupplier<ITexture> emptyImageSupplier) {

        ContextMenu customizationSettingsMenu = new ContextMenu();

        customizationSettingsMenu.addValueCycleEntry("advanced_customization_mode", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.advanced_customization_mode", FancyMenu.getOptions().advancedCustomizationMode.getValue())
                        .addCycleListener(cycle -> {
                            FancyMenu.getOptions().advancedCustomizationMode.setValue(cycle.getAsBoolean());
                        })).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.settings.advanced_customization_mode.tooltip")))
                .setIcon(MaterialIcons.WARNING);

        customizationSettingsMenu.addSeparatorEntry("separator_after_advanced_mode");

        ContextMenuUtils.addLongInputContextMenuEntryTo(customizationSettingsMenu, "placeholder_caching_duration",
                        Component.translatable("fancymenu.settings.caching.placeholders.set"),
                        () -> FancyMenu.getOptions().placeholderCachingDurationMs.getValue(),
                        duration -> FancyMenu.getOptions().placeholderCachingDurationMs.setValue(duration),
                        true, FancyMenu.getOptions().placeholderCachingDurationMs.getDefaultValue(), null, null, (screen1, s) -> {
                            Minecraft.getInstance().setScreen(screen1);
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "placeholder_caching_duration")));
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.settings.caching.placeholders.set.desc")))
                .setIcon(MaterialIcons.TIMER);

        ContextMenuUtils.addLongInputContextMenuEntryTo(customizationSettingsMenu, "requirement_caching_duration",
                        Component.translatable("fancymenu.settings.caching.requirements.set"),
                        () -> FancyMenu.getOptions().requirementCachingDurationMs.getValue(),
                        duration -> FancyMenu.getOptions().requirementCachingDurationMs.setValue(duration),
                        true, FancyMenu.getOptions().requirementCachingDurationMs.getDefaultValue(), null, null, (screen1, s) -> {
                            Minecraft.getInstance().setScreen(screen1);
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "requirement_caching_duration")));
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.settings.caching.requirements.set.desc")))
                .setIcon(MaterialIcons.TIMER);

        customizationSettingsMenu.addSeparatorEntry("separator_before_developer");

        ContextMenu developerMenu = new ContextMenu();
        customizationSettingsMenu.addSubMenuEntry("developer", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.developer"), developerMenu)
                .setIcon(MaterialIcons.DEVELOPER_MODE_TV);

        developerMenu.addValueCycleEntry("show_pip_window_debug",
                        CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.developer.show_pip_window_debug", FancyMenu.getOptions().devShowPipWindowDebug.getValue())
                                .addCycleListener(cycle -> {
                                    FancyMenu.getOptions().devShowPipWindowDebug.setValue(cycle.getAsBoolean());
                                }))
                .setIcon(MaterialIcons.DEVELOPER_BOARD);

        developerMenu.addValueCycleEntry("smooth_font_multiline_rendering",
                        CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.developer.smooth_font_multiline_rendering", FancyMenu.getOptions().smoothFontMultilineRendering.getValue())
                                .addCycleListener(cycle -> {
                                    FancyMenu.getOptions().smoothFontMultilineRendering.setValue(cycle.getAsBoolean());
                                    SmoothTextRenderer.clearCaches();
                                }))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.settings.developer.smooth_font_multiline_rendering.desc")))
                .setIcon(MaterialIcons.FORMAT_TEXT_WRAP);

        developerMenu.addClickableEntry("reset_welcome_screen",
                        Component.translatable("fancymenu.overlay.menu_bar.customization.settings.developer.reset_welcome_screen"),
                        (menu, entry) -> FancyMenu.getOptions().showWelcomeScreen.setValue(true))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.settings.developer.reset_welcome_screen.desc")))
                .setIcon(MaterialIcons.RESET_SETTINGS);

        return customizationSettingsMenu;

    }

    public static ContextMenu buildGlobalCustomizationMenu(@NotNull ResourceSupplier<ITexture> emptyImageSupplier) {

        ContextMenu globalCustomizationsMenu = new ContextMenu();

        globalCustomizationsMenu.addSubMenuEntry("game_intro", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro"), buildGameIntroMenu(emptyImageSupplier))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.game_intro.desc")))
                .setIcon(MaterialIcons.VIDEOCAM);

        globalCustomizationsMenu.addSeparatorEntry("separator_after_game_intro");

        globalCustomizationsMenu.addValueCycleEntry("singleplayer_world_icons", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.singleplayer_screen.world_icons", FancyMenu.getOptions().showSingleplayerScreenWorldIcons.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().showSingleplayerScreenWorldIcons.setValue(cycle.getAsBoolean());
                }))
                .setIcon(MaterialIcons.IMAGE);

        globalCustomizationsMenu.addValueCycleEntry("multiplayer_server_icons", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.multiplayer_screen.server_icons", FancyMenu.getOptions().showMultiplayerScreenServerIcons.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().showMultiplayerScreenServerIcons.setValue(cycle.getAsBoolean());
                }))
                .setIcon(MaterialIcons.IMAGE);

        globalCustomizationsMenu.addSeparatorEntry("separator_after_mp_server_icons");

        globalCustomizationsMenu.addSubMenuEntry("window_icon", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon"), buildWindowIconMenu())
                .setIcon(MaterialIcons.IMAGE);

        ContextMenuUtils.addInputContextMenuEntryTo(globalCustomizationsMenu, "window_title", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_title"),
                        () -> FancyMenu.getOptions().customWindowTitle.getValue(),
                        s -> {
                            FancyMenu.getOptions().customWindowTitle.setValue(s);
                            WindowHandler.updateWindowTitle();
                        }, true, FancyMenu.getOptions().customWindowTitle.getDefaultValue(), null, false, false, TextValidators.NO_EMPTY_STRING_SPACES_ALLOWED_TEXT_VALIDATOR, null, (screen1, s) -> {
                            Minecraft.getInstance().setScreen(screen1);
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "window_title")));
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_title.tooltip")))
                .setIcon(MaterialIcons.TEXT_FIELDS);

        globalCustomizationsMenu.addSeparatorEntry("separator_after_window_title");

        ContextMenuUtils.addIntegerInputContextMenuEntryTo(globalCustomizationsMenu, "default_gui_scale",
                        Component.translatable("fancymenu.overlay.menu_bar.customization.settings.set_default_gui_scale"),
                        () -> FancyMenu.getOptions().defaultGuiScale.getValue(),
                        integer -> FancyMenu.getOptions().defaultGuiScale.setValue(integer),
                        true, FancyMenu.getOptions().defaultGuiScale.getDefaultValue(), null, null, (screen1, s) -> {
                            Minecraft.getInstance().setScreen(screen1);
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "default_gui_scale")));
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.settings.set_default_gui_scale.tooltip")))
                .setIcon(MaterialIcons.STRAIGHTEN);

        globalCustomizationsMenu.addValueCycleEntry("force_fullscreen", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.force_fullscreen", FancyMenu.getOptions().forceFullscreen.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().forceFullscreen.setValue(cycle.getAsBoolean());
                })).setIcon(MaterialIcons.FULLSCREEN);

        globalCustomizationsMenu.addSeparatorEntry("separator_after_force_fullscreen");

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        globalCustomizationsMenu.addSubMenuEntry("button_background_textures", Component.translatable("fancymenu.global_customizations.button_backgrounds"), buttonBackgroundMenu)
                .setIcon(MaterialIcons.IMAGE);

        ResourceSupplier<IAudio> emptyAudioSupplier = ResourceSupplier.empty(IAudio.class, FileMediaType.AUDIO);

        ContextMenuUtils.addImageResourceChooserContextMenuEntryTo(buttonBackgroundMenu, "button_background_normal", emptyImageSupplier,
                GlobalCustomizationHandler::getCustomButtonBackgroundNormalSupplier,
                supplier -> FancyMenu.getOptions().globalButtonBackgroundNormal.setValue((supplier == null || supplier.isEmpty()) ? "" : supplier.getSourceWithPrefix()),
                Component.translatable("fancymenu.global_customizations.button_backgrounds.normal"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        ContextMenuUtils.addImageResourceChooserContextMenuEntryTo(buttonBackgroundMenu, "button_background_hover", emptyImageSupplier,
                GlobalCustomizationHandler::getCustomButtonBackgroundHoverSupplier,
                supplier -> FancyMenu.getOptions().globalButtonBackgroundHover.setValue((supplier == null || supplier.isEmpty()) ? "" : supplier.getSourceWithPrefix()),
                Component.translatable("fancymenu.global_customizations.button_backgrounds.hover"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        ContextMenuUtils.addImageResourceChooserContextMenuEntryTo(buttonBackgroundMenu, "button_background_inactive", emptyImageSupplier,
                GlobalCustomizationHandler::getCustomButtonBackgroundInactiveSupplier,
                supplier -> FancyMenu.getOptions().globalButtonBackgroundInactive.setValue((supplier == null || supplier.isEmpty()) ? "" : supplier.getSourceWithPrefix()),
                Component.translatable("fancymenu.global_customizations.button_backgrounds.inactive"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        globalCustomizationsMenu.addSeparatorEntry("separator_after_button_textures");

        ContextMenu sliderTexturesMenu = new ContextMenu();
        globalCustomizationsMenu.addSubMenuEntry("slider_textures", Component.translatable("fancymenu.global_customizations.slider_textures"), sliderTexturesMenu)
                .setIcon(MaterialIcons.IMAGE);

        ContextMenuUtils.addImageResourceChooserContextMenuEntryTo(sliderTexturesMenu, "slider_background_texture", emptyImageSupplier,
                        GlobalCustomizationHandler::getCustomSliderBackgroundSupplier,
                        supplier -> FancyMenu.getOptions().globalSliderBackground.setValue((supplier == null || supplier.isEmpty()) ? "" : supplier.getSourceWithPrefix()),
                        Component.translatable("fancymenu.global_customizations.slider_background"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        sliderTexturesMenu.addSeparatorEntry("separator_after_slider_background");

        ContextMenu sliderHandleMenu = new ContextMenu();
        sliderTexturesMenu.addSubMenuEntry("slider_handle_textures", Component.translatable("fancymenu.global_customizations.slider_handle_textures"), sliderHandleMenu)
                .setIcon(MaterialIcons.IMAGE);

        ContextMenuUtils.addImageResourceChooserContextMenuEntryTo(sliderHandleMenu, "slider_handle_normal", emptyImageSupplier,
                GlobalCustomizationHandler::getCustomSliderHandleNormalSupplier,
                supplier -> FancyMenu.getOptions().globalSliderHandleNormal.setValue((supplier == null || supplier.isEmpty()) ? "" : supplier.getSourceWithPrefix()),
                Component.translatable("fancymenu.global_customizations.slider_handle_textures.normal"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        ContextMenuUtils.addImageResourceChooserContextMenuEntryTo(sliderHandleMenu, "slider_handle_hover", emptyImageSupplier,
                GlobalCustomizationHandler::getCustomSliderHandleHoverSupplier,
                supplier -> FancyMenu.getOptions().globalSliderHandleHover.setValue((supplier == null || supplier.isEmpty()) ? "" : supplier.getSourceWithPrefix()),
                Component.translatable("fancymenu.global_customizations.slider_handle_textures.hover"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        ContextMenuUtils.addImageResourceChooserContextMenuEntryTo(sliderHandleMenu, "slider_handle_inactive", emptyImageSupplier,
                GlobalCustomizationHandler::getCustomSliderHandleInactiveSupplier,
                supplier -> FancyMenu.getOptions().globalSliderHandleInactive.setValue((supplier == null || supplier.isEmpty()) ? "" : supplier.getSourceWithPrefix()),
                Component.translatable("fancymenu.global_customizations.slider_handle_textures.inactive"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        globalCustomizationsMenu.addSeparatorEntry("separator_after_slider_textures");

        ContextMenuUtils.addImageResourceChooserContextMenuEntryTo(globalCustomizationsMenu, "menu_background_texture", emptyImageSupplier,
                        GlobalCustomizationHandler::getCustomMenuBackgroundSupplier,
                        supplier -> FancyMenu.getOptions().globalMenuBackgroundTexture.setValue((supplier == null || supplier.isEmpty()) ? "" : supplier.getSourceWithPrefix()),
                        Component.translatable("fancymenu.global_customizations.menu_background_texture"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        ContextMenu panoramaMenu = new ContextMenu();
        globalCustomizationsMenu.addSubMenuEntry("background_panorama", Component.translatable("fancymenu.global_customizations.background_panorama"), panoramaMenu)
                .setIcon(MaterialIcons.IMAGE);

        panoramaMenu.addClickableEntry("choose_panorama", Component.translatable("fancymenu.global_customizations.background_panorama.choose"), (menu, entry) -> {
            String currentPanorama = FancyMenu.getOptions().globalBackgroundPanorama.getValue();
            ChoosePanoramaScreen s = new ChoosePanoramaScreen(currentPanorama.isEmpty() ? null : currentPanorama, panoramaName -> {
                if (panoramaName != null) {
                    FancyMenu.getOptions().globalBackgroundPanorama.setValue(panoramaName);
                }
                forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "background_panorama")));
            });
            ChoosePanoramaScreen.openInWindow(s);
        }).setIcon(MaterialIcons.IMAGE);

        panoramaMenu.addClickableEntry("clear_panorama", Component.translatable("fancymenu.global_customizations.background_panorama.clear"), (menu, entry) -> {
            FancyMenu.getOptions().globalBackgroundPanorama.setValue("");
            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "background_panorama")));
        }).setIcon(MaterialIcons.DELETE);

        panoramaMenu.addSeparatorEntry("separator_before_current_panorama");

        panoramaMenu.addClickableEntry("current_panorama", Component.empty(), (menu, entry) -> {})
                .setLabelSupplier((menu, entry) -> {
                    String value = FancyMenu.getOptions().globalBackgroundPanorama.getValue();
                    Component display = value.isEmpty()
                            ? Component.literal("---").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt()))
                            : Component.literal(value).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_text_color.getColorInt()));
                    return Component.translatable("fancymenu.global_customizations.background_panorama.current", display);
                })
                .setClickSoundEnabled(false)
                .setIcon(MaterialIcons.INFO);

        globalCustomizationsMenu.addSeparatorEntry("separator_after_panorama");

        globalCustomizationsMenu.addValueCycleEntry("play_menu_music", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.play_menu_music", FancyMenu.getOptions().playVanillaMenuMusic.getValue())
                        .addCycleListener(cycle -> {
                            FancyMenu.getOptions().playVanillaMenuMusic.setValue(cycle.getAsBoolean());
                            Minecraft.getInstance().getMusicManager().stopPlaying();
                            if (FancyMenu.getOptions().playVanillaMenuMusic.getValue()) {
                                Minecraft.getInstance().getMusicManager().startPlaying(Minecraft.getInstance().getSituationalMusic());
                            }
                        }))
                .setIcon(MaterialIcons.MUSIC_NOTE);

        globalCustomizationsMenu.addClickableEntry("menu_music_tracks", Component.translatable("fancymenu.global_customizations.menu_music_tracks"), (menu, entry) -> {
                    Screen current = Minecraft.getInstance().screen;
                    Minecraft.getInstance().setScreen(new ManageGlobalMenuMusicTracksScreen(call -> {
                        Minecraft.getInstance().setScreen(current);
                    }));
                }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.global_customizations.menu_music_tracks.desc")))
                .setIcon(MaterialIcons.QUEUE_MUSIC);

        globalCustomizationsMenu.addSeparatorEntry("separator_after_menu_music");

        ContextMenuUtils.addAudioResourceChooserContextMenuEntryTo(globalCustomizationsMenu, "button_click_sound", emptyAudioSupplier,
                        GlobalCustomizationHandler::getCustomButtonClickSoundSupplier,
                        supplier -> FancyMenu.getOptions().globalButtonClickSound.setValue((supplier == null || supplier.isEmpty()) ? "" : supplier.getSourceWithPrefix()),
                        Component.translatable("fancymenu.global_customizations.button_click_sound"), true, null, true, true, true)
                .setIcon(MaterialIcons.VOLUME_UP);

        return globalCustomizationsMenu;

    }

    public static ContextMenu buildWindowIconMenu() {

        ContextMenu windowIconMenu = new ContextMenu();

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
                .addIsActiveSupplier((menu, entry) -> WindowHandler.allCustomWindowIconsSetAndFound())
                .setTooltipSupplier((menu, entry) -> !WindowHandler.allCustomWindowIconsSetAndFound() ? UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.toggle.disabled.tooltip")) : null)
                .setIcon(MaterialIcons.IMAGE);

        FileTypeGroup<?> pngFileGroup = FileTypeGroup.of(FileTypes.PNG_IMAGE);
        pngFileGroup.setDisplayName(FileTypeGroups.IMAGE_GROUP_COMPONENT);

        ContextMenuUtils.addFileChooserContextMenuEntryTo(windowIconMenu, "icon_16",
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
                                        Minecraft.getInstance().setScreen(screen1);
                                        Dialogs.openMessageWithCallback(Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.wrong_resolution", "16x16"), MessageDialogStyle.ERROR, b -> {
                                            FancyMenu.getOptions().customWindowIcon16.setValue("");
                                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "window_icon")));
                                        });
                                    } else {
                                        Minecraft.getInstance().setScreen(screen1);
                                        forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "window_icon")));
                                    }
                                } else {
                                    Minecraft.getInstance().setScreen(screen1);
                                    forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "window_icon")));
                                }
                            } else {
                                Minecraft.getInstance().setScreen(screen1);
                                forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "window_icon")));
                            }
                        })
                .setLabelSupplier((menu, entry) -> {
                    MutableComponent notFound = Component.literal("✖").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt()));
                    MutableComponent found = Component.literal("✔").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_text_color.getColorInt()));
                    File icon = WindowHandler.getCustomWindowIcon16();
                    if ((icon != null) && icon.isFile()) {
                        return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_16", found);
                    }
                    return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_16", notFound);
                })
                .setIcon(MaterialIcons.IMAGE);

        ContextMenuUtils.addFileChooserContextMenuEntryTo(windowIconMenu, "icon_32",
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
                                        Minecraft.getInstance().setScreen(screen1);
                                        Dialogs.openMessageWithCallback(Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.wrong_resolution", "32x32"), MessageDialogStyle.ERROR, b -> {
                                            FancyMenu.getOptions().customWindowIcon32.setValue("");
                                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "window_icon")));
                                        });
                                    } else {
                                        Minecraft.getInstance().setScreen(screen1);
                                        forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "window_icon")));
                                    }
                                } else {
                                    Minecraft.getInstance().setScreen(screen1);
                                    forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "window_icon")));
                                }
                            } else {
                                Minecraft.getInstance().setScreen(screen1);
                                forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "window_icon")));
                            }
                        })
                .setLabelSupplier((menu, entry) -> {
                    MutableComponent notFound = Component.literal("✖").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt()));
                    MutableComponent found = Component.literal("✔").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_text_color.getColorInt()));
                    File icon = WindowHandler.getCustomWindowIcon32();
                    if ((icon != null) && icon.isFile()) {
                        return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_32", found);
                    }
                    return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_32", notFound);
                })
                .setIcon(MaterialIcons.IMAGE);

        FileTypeGroup<?> macOsIconTypeGroup = FileTypeGroup.of(new ImageFileType(FileCodec.empty(ITexture.class), null, "icns"));
        macOsIconTypeGroup.setDisplayName(Component.translatable("fancymenu.file_types.icns"));

        ContextMenuUtils.addFileChooserContextMenuEntryTo(windowIconMenu, "icon_macos",
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
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("global_customizations", "window_icon")));
                        })
                .setLabelSupplier((menu, entry) -> {
                    MutableComponent notFound = Component.literal("✖").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt()));
                    MutableComponent found = Component.literal("✔").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_text_color.getColorInt()));
                    File icon = WindowHandler.getCustomWindowIconMacOS();
                    if ((icon != null) && icon.isFile()) {
                        return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_macos", found);
                    }
                    return Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.choose_macos", notFound);
                })
                .setIcon(MaterialIcons.IMAGE);

        windowIconMenu.addSeparatorEntry("separator_after_macos_icon");

        windowIconMenu.addClickableEntry("convert_png_to_macos_icon", Component.translatable("fancymenu.overlay.menu_bar.customization.settings.custom_window_icon.convert_to_macos_icon"), (menu, entry) -> {
            WebUtils.openWebLink("https://miconv.com/convert-png-to-icns/");
        }).setIcon(MaterialIcons.OPEN_IN_BROWSER);

        return windowIconMenu;

    }

    public static ContextMenu buildGameIntroMenu(@NotNull ResourceSupplier<ITexture> emptyImageSupplier) {

        ContextMenu gameIntroMenu = new ContextMenu();

        FileTypeGroup<ImageFileType> introFileTypeGroup = FileTypeGroup.of(FileTypes.APNG_IMAGE, FileTypes.GIF_IMAGE, FileTypes.FMA_IMAGE);

        ContextMenuUtils.addGenericResourceChooserContextMenuEntryTo(gameIntroMenu, "set_game_intro",
                () -> new ResourceChooserWindowBody<>(Component.empty(), introFileTypeGroup, null, s -> {}),
                ResourceSupplier::image, emptyImageSupplier,
                () -> {
                    if (FancyMenu.getOptions().gameIntroAnimation.getValue().trim().isEmpty()) return emptyImageSupplier;
                    return ResourceSupplier.image(FancyMenu.getOptions().gameIntroAnimation.getValue());
                },
                supplier -> {
                    FancyMenu.getOptions().gameIntroAnimation.setValue(supplier.getSourceWithPrefix());
                    forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "game_intro", "set_game_intro")));
                },
                Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro.set"), true, introFileTypeGroup, null, true, true, false)
                .setIcon(MaterialIcons.VIDEOCAM);

        gameIntroMenu.addSeparatorEntry("separator_after_game_intro_set_animation");

        gameIntroMenu.addValueCycleEntry("game_intro_allow_skip", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.game_intro.allow_skip", FancyMenu.getOptions().gameIntroAllowSkip.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().gameIntroAllowSkip.setValue(cycle.getAsBoolean());
                }))
                .setIcon(MaterialIcons.SKIP_NEXT);

        gameIntroMenu.addValueCycleEntry("game_intro_fade_out", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.customization.settings.game_intro.fade_out", FancyMenu.getOptions().gameIntroFadeOut.getValue())
                .addCycleListener(cycle -> {
                    FancyMenu.getOptions().gameIntroFadeOut.setValue(cycle.getAsBoolean());
                }))
                .setIcon(MaterialIcons.TRANSITION_FADE);

        ContextMenuUtils.addInputContextMenuEntryTo(gameIntroMenu, "game_intro_set_custom_skip_text",
                        Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro.set_custom_skip_text"),
                        () -> {
                            String s = FancyMenu.getOptions().gameIntroCustomSkipText.getValue();
                            if (s.isEmpty()) return null;
                            return s;
                        },
                        s -> FancyMenu.getOptions().gameIntroCustomSkipText.setValue((s != null) ? s : ""),
                        true, null, null, false, false, null, null, (screen1, s) -> {
                            Minecraft.getInstance().setScreen(screen1);
                            forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(List.of("screen_settings", "game_intro", "game_intro_set_custom_skip_text")));
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.customization.settings.game_intro.set_custom_skip_text.tooltip")))
                .setIcon(MaterialIcons.TEXT_FIELDS);

        return gameIntroMenu;

    }

    public static MenuBar.ClickableMenuBarEntry buildFMIconTabAndAddTo(MenuBar menuBar) {
        return menuBar.addClickableEntry(MenuBar.Side.LEFT, "fancymenu_icon", Component.empty(), (bar, entry) -> {})
                .setIconTextureSupplier((bar, entry) -> FM_LOGO_TEXTURE_SUPPLIER.get())
                .setBaseWidth(MenuBar.PIXEL_SIZE)
                .setActive(false);
    }

    public static ContextMenu buildHelpTabAndAddTo(MenuBar menuBar) {

        ContextMenu helpMenu = new ContextMenu();
        menuBar.addContextMenuEntry("help", Component.translatable("fancymenu.overlay.menu_bar.help"), helpMenu);

        helpMenu.addClickableEntry("fancymenu_wiki", Component.translatable("fancymenu.overlay.menu_bar.help.wiki"), (menu, entry) -> {
            WebUtils.openWebLink("https://docs.fancymenu.net");
        }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.help.wiki.tooltip")))
                .setIcon(MaterialIcons.MENU_BOOK);

        helpMenu.addClickableEntry("join_the_discord", Component.translatable("fancymenu.overlay.menu_bar.help.discord"), (menu, entry) -> {
            WebUtils.openWebLink("https://discord.gg/rhayah27GC");
        }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.help.discord.tooltip")))
                .setIcon(MaterialIcons.CHAT);

        helpMenu.addClickableEntry("report_issue", Component.translatable("fancymenu.overlay.menu_bar.help.report_issue"), (menu, entry) -> {
            WebUtils.openWebLink("https://github.com/Keksuccino/FancyMenu/issues");
        }).setIcon(MaterialIcons.BUG_REPORT);

        helpMenu.addSeparatorEntry("separator_after_report_issue");

        helpMenu.addClickableEntry("curseforge_fancymenu_category", Component.translatable("fancymenu.overlay.menu_bar.help.curseforge_fancymenu_category"), (menu, entry) -> {
            WebUtils.openWebLink("https://www.curseforge.com/minecraft/search?page=1&class=customization&categoryIds=5186");
        }).setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.help.curseforge_fancymenu_category.tooltip")))
                .setIcon(MaterialIcons.SEARCH);

        helpMenu.addSeparatorEntry("separator_after_curseforge");

        helpMenu.addClickableEntry("keksuccino_patreon", Component.translatable("fancymenu.overlay.menu_bar.help.patreon"), (menu, entry) -> {
            WebUtils.openWebLink("https://www.patreon.com/keksuccino");
        }).setIcon(MaterialIcons.FAVORITE);

        helpMenu.addClickableEntry("paypal_tip_jar", Component.translatable("fancymenu.overlay.menu_bar.help.paypal"), (menu, entry) -> {
            WebUtils.openWebLink("https://www.paypal.com/paypalme/TimSchroeter");
        }).setIcon(MaterialIcons.PAYMENTS);

        helpMenu.addSeparatorEntry("separator_after_paypal");

        helpMenu.addClickableEntry("credits", Component.translatable("fancymenu.credits"), (menu, entry) -> {
            Minecraft.getInstance().setScreen(new CreditsScreen((Minecraft.getInstance().screen != null) ? Minecraft.getInstance().screen : new TitleScreen()));
        }).setIcon(MaterialIcons.INFO);

        return helpMenu;

    }

    public static ContextMenu buildUITabAndAddTo(MenuBar menuBar) {

        ContextMenu userInterfaceMenu = new ContextMenu();
        menuBar.addContextMenuEntry("user_interface", Component.translatable("fancymenu.overlay.menu_bar.user_interface"), userInterfaceMenu);

        float preSelectedUiScale = FancyMenu.getOptions().uiScale.getValue();
        if ((preSelectedUiScale != 1F) && (preSelectedUiScale != 1.5F) && (preSelectedUiScale != 2F) && (preSelectedUiScale != 2.5F) && (preSelectedUiScale != 3F) && (preSelectedUiScale != 3.5F) && (preSelectedUiScale != 4F)) {
            preSelectedUiScale = 4F;
        }
        String preSelectedUIScaleString = "4";
        if (preSelectedUiScale == 1F) preSelectedUIScaleString = "1";
        if (preSelectedUiScale == 1.5F) preSelectedUIScaleString = "1.5";
        if (preSelectedUiScale == 2F) preSelectedUIScaleString = "2";
        if (preSelectedUiScale == 2.5F) preSelectedUIScaleString = "2.5";
        if (preSelectedUiScale == 3F) preSelectedUIScaleString = "3";
        if (preSelectedUiScale == 3.5F) preSelectedUIScaleString = "3.5";
        userInterfaceMenu.addValueCycleEntry("ui_scale", CommonCycles.cycle("fancymenu.overlay.menu_bar.user_interface.ui_scale", ListUtils.of("1","1.5","2","2.5","3","3.5","4"), preSelectedUIScaleString)
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
                .setIcon(MaterialIcons.STRAIGHTEN);

        userInterfaceMenu.addValueCycleEntry("ui_click_sounds", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.user_interface.ui_click_sounds", FancyMenu.getOptions().playUiClickSounds.getValue())
                .addCycleListener(cycle -> {
                    if (cycle.getAsBoolean()) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    FancyMenu.getOptions().playUiClickSounds.setValue(cycle.getAsBoolean());
                })).setClickSoundEnabled(false)
                .setIcon(MaterialIcons.VOLUME_UP);

        userInterfaceMenu.addValueCycleEntry("ui_animations", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.user_interface.ui_animations", FancyMenu.getOptions().enableUiAnimations.getValue())
                        .addCycleListener(cycle -> FancyMenu.getOptions().enableUiAnimations.setValue(cycle.getAsBoolean())))
                .setIcon(MaterialIcons.ANIMATION);

        userInterfaceMenu.addValueCycleEntry("use_minecraft_font", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.user_interface.use_minecraft_font", FancyMenu.getOptions().useMinecraftFont.getValue())
                        .addCycleListener(cycle -> FancyMenu.getOptions().useMinecraftFont.setValue(cycle.getAsBoolean())))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.user_interface.use_minecraft_font.desc")))
                .setIcon(MaterialIcons.BRAND_FAMILY);

        userInterfaceMenu.addSeparatorEntry("separator_before_blur_settings");

        userInterfaceMenu.addValueCycleEntry("ui_blur", CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.user_interface.ui_blur", FancyMenu.getOptions().enableUiBlur.getValue())
                        .addCycleListener(cycle -> FancyMenu.getOptions().enableUiBlur.setValue(cycle.getAsBoolean())))
                .setIcon(MaterialIcons.BLUR_ON);

        ContextMenu.ClickableContextMenuEntry<?> blurIntensityEntry = ContextMenuUtils.addRangeSliderInputContextMenuEntryTo(
                userInterfaceMenu,
                "ui_blur_intensity",
                Component.translatable("fancymenu.overlay.menu_bar.user_interface.ui_blur_intensity"),
                () -> FancyMenu.getOptions().uiBlurIntensity.getValue().doubleValue(),
                value -> FancyMenu.getOptions().uiBlurIntensity.setValue(value.floatValue()),
                true,
                FancyMenu.getOptions().uiBlurIntensity.getDefaultValue().doubleValue(),
                0.25D,
                3.0D,
                v -> {
                    double pct = MathUtils.round(v * 100.0D, 1);
                    return Component.translatable("fancymenu.overlay.menu_bar.user_interface.ui_blur_intensity.slider_label", pct);
                }
        );
        blurIntensityEntry.addIsActiveSupplier((menu, entry) -> FancyMenu.getOptions().enableUiBlur.getValue());
        blurIntensityEntry.setIcon(MaterialIcons.OPACITY);

        userInterfaceMenu.addSeparatorEntry("separator_after_blur_settings");

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
                .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.overlay.menu_bar.user_interface.context_menu_hover_open_speed.desc")))
                .setIcon(MaterialIcons.TIMER);

        ContextMenu windowUiThemeMenu = new ContextMenu();
        userInterfaceMenu.addSubMenuEntry("ui_theme", Component.translatable("fancymenu.overlay.menu_bar.user_interface.ui_theme"), windowUiThemeMenu)
                .setIcon(MaterialIcons.PALETTE);

        int i2 = 0;
        for (UITheme s : UIColorThemeRegistry.getThemes()) {
            windowUiThemeMenu.addClickableEntry("ui_theme_" + i2, s.getDisplayName(), (menu, entry) -> {
                FancyMenu.getOptions().uiTheme.setValue(s.getIdentifier());
                UIColorThemeRegistry.setActiveTheme(s.getIdentifier());
            }).setIcon(MaterialIcons.PALETTE);
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
        }).setLabelSupplier((menu1, entry) -> layout.getStatus().getCycleComponent())
                .setIcon(MaterialIcons.TOGGLE_ON);

        menu.addClickableEntry("edit_layout", Component.translatable("fancymenu.layout.manage.edit"), (menu1, entry) -> {
            menu1.closeMenu();
            MainThreadTaskExecutor.executeInMainThread(() -> LayoutHandler.openLayoutEditor(layout, layout.isUniversalLayout() ? null : screen), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        }).setIcon(MaterialIcons.EDIT);

        menu.addClickableEntry("delete_layout", Component.translatable("fancymenu.layout.manage.delete"), (menu1, entry) -> {
            menu1.closeMenu();
            Dialogs.openMessageWithCallback(Component.translatable("fancymenu.layout.manage.delete.confirm"), MessageDialogStyle.WARNING, call -> {
                if (call) {
                    layout.delete(false);
                }
                forScreenMenuBarTab(contextMenuBarEntry -> contextMenuBarEntry.openContextMenu(entryPath));
            });
        }).setIcon(MaterialIcons.DELETE);

        menu.addClickableEntry("rename_layout", Component.translatable("fancymenu.layout.manage.rename"), (menu1, entry) -> {
            if (layout.layoutFile == null) return;
            TextInputWindowBody inputScreen = new TextInputWindowBody(CharacterFilter.buildLowercaseAndUppercaseFileNameFilter(), s -> {
                if ((s != null) && !s.isBlank()) {
                    String newName = s + ".txt";
                    String newPath = layout.layoutFile.getParent();
                    if (newPath == null) newPath = "";
                    newPath = newPath + "/" + newName;
                    File f = new File(newPath);
                    if (!f.isFile()) {
                        try {
                            Files.move(layout.layoutFile, f);
                        } catch (Exception ex) {
                            LOGGER.error("[FANCYMENU] Failed to rename layout file!", ex);
                        }
                        LayoutHandler.reloadLayouts();
                    } else {
                        Dialogs.openMessage(Component.literal(String.join("\n", LocalizationUtils.splitLocalizedStringLines("fancymenu.layout.manage.rename.error.file_already_exists"))), MessageDialogStyle.ERROR);
                    }
                } else if (s != null) {
                    Dialogs.openMessage(Component.literal(String.join("\n", LocalizationUtils.splitLocalizedStringLines("fancymenu.layout.manage.rename.error.empty_name"))), MessageDialogStyle.ERROR);
                }
            });
            Dialogs.openGeneric(inputScreen,
                    Component.translatable("fancymenu.layout.manage.rename"),
                    null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT)
                    .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
            inputScreen.setText(layout.getLayoutName());
            menu1.closeMenuChain();
        }).setIcon(MaterialIcons.DRIVE_FILE_RENAME_OUTLINE);

        menu.addClickableEntry("edit_in_system_text_editor", Component.translatable("fancymenu.layout.manage.open_in_text_editor"), (menu1, entry) -> {
            if (layout.layoutFile != null) {
                FileUtils.openFile(layout.layoutFile);
            }
        }).setIcon(MaterialIcons.OPEN_IN_NEW);

        return menu;

    }

}
