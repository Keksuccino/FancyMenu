package de.keksuccino.fancymenu.customization.profile;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.LegacyHandler;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.customlocals.CustomLocalsHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerHandler;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerStateController;
import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElementController;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.fancymenu.customization.fmdata.FmDataServerListenerHandler;
import de.keksuccino.fancymenu.customization.fmdata.FmDataWelcomeDataHandler;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidgetBuilder;
import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced.FileTextPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.placeholders.other.RandomTextPlaceholder;
import de.keksuccino.fancymenu.customization.remote.RemoteServerConnectionManager;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.requirements.OncePerSessionRequirement;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerHandler;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizerHandler;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.UIThemes;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Brings FancyMenu's runtime state in line with the files of the active profile, so after a profile switch everything
 * behaves like on a fresh game launch with the new profile.<br><br>
 *
 * Many handlers only read their files once at launch and some write their in-memory state back to disk, so the order of
 * {@link #suspend(PiPWindow)} and {@link #resume(boolean)} matters. Everything that could write into the config directory has to be stopped
 * and flushed before the files change, and everything has to read the new files before anything saves again.
 */
public final class ProfileRuntimeReloader {

    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean suspended = false;

    private ProfileRuntimeReloader() {
    }

    /**
     * Stops everything that reads or writes profile files and releases all files FancyMenu keeps open.
     * Has to get called on the main thread before the content of the config directory changes.
     *
     * @param keepOpenWindow a window that stays open, like the profile manager. All other FancyMenu windows get closed, because they show data of the old profile.
     * @return FALSE if pending variable changes could not get saved, so changing the files would lose data.
     */
    public static boolean suspend(@Nullable PiPWindow keepOpenWindow) {
        if (suspended) return true;
        LOGGER.info("[FANCYMENU] Suspending FancyMenu for a profile switch..");
        suspended = true;
        closeWindowsExcept(keepOpenWindow);
        Screen screen = ScreenUtils.getScreen();
        if (screen != null) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(screen);
            if (layer != null) {
                layer.layoutBase.menuBackgrounds.forEach(MenuBackground::onDisableOrRemove);
                layer.allElements.forEach(AbstractElement::onDestroyElement);
                layer.resetLayer();
            }
        }
        ScreenCustomization.setScreenCustomizationEnabled(false);
        ScreenCustomization.reInitCurrentScreen();
        SchedulerHandler.stopAllSchedulers();
        AnimationControllerHandler.stopAllAnimations();
        AudioElementBuilder.stopAllActiveAudios();
        RemoteServerConnectionManager.closeAllConnections();
        ResourceHandlers.reloadAll();
        return VariableHandler.flush();
    }

    /**
     * Reloads everything from the files of the active profile and enables screen customization again.
     * Gets also used after failed switches, so the unchanged old profile gets back to work.
     *
     * @param profileChanged if TRUE, the current screen gets replaced like on a fresh launch, otherwise it only gets re-initialized.
     */
    public static void resume(boolean profileChanged) {
        if (!suspended) return;
        LOGGER.info("[FANCYMENU] Reloading FancyMenu with the files of the active profile..");
        recreateDirectories();
        LegacyHandler.updateCheckList();
        FancyMenu.reloadOptions();
        VariableHandler.init();
        UIThemes.reloadThemes();
        CustomLocalsHandler.loadLocalizations();
        ListenerHandler.reload();
        VideoElementController.reload();
        AudioElementController.reload();
        AnimationControllerStateController.reload();
        ScrollScreenNormalizerHandler.reload();
        FmDataServerListenerHandler.reload();
        FmDataWelcomeDataHandler.reload();
        OncePerSessionRequirement.clearSeenInstances();
        RequirementContainer.clearCooldownCache();
        FileTextPlaceholder.clearCache();
        RandomTextPlaceholder.clearCache();
        // Custom GUIs need to get read before the reload event, because their regular reload saves the GUIs in memory first
        CustomGuiHandler.reloadFromFile();
        LayoutHandler.init();
        if (WindowHandler.isCustomWindowIconEnabled()) {
            WindowHandler.updateCustomWindowIcon();
        } else {
            WindowHandler.resetWindowIcon();
        }
        SchedulerHandler.reload();
        ScreenCustomization.setScreenCustomizationEnabled(true);
        suspended = false;
        ScreenCustomization.finishFancyMenuReload(false);
        if (profileChanged) {
            openFreshScreen();
        } else {
            ScreenCustomization.reInitCurrentScreen();
        }
    }

    public static boolean isSuspended() {
        return suspended;
    }

    /**
     * The handlers create their directories only once at launch, but a profile doesn't need to have all of them.
     */
    private static void recreateDirectories() {
        FileUtils.createDirectory(FancyMenu.MOD_DIR);
        FileUtils.createDirectory(LayoutHandler.LAYOUT_DIR);
        FileUtils.createDirectory(LayoutHandler.ASSETS_DIR);
        FileUtils.createDirectory(PanoramaHandler.PANORAMA_DIR);
        FileUtils.createDirectory(SlideshowHandler.SLIDESHOW_DIR);
        FileUtils.createDirectory(UIThemes.THEME_DIR);
        FileUtils.createDirectory(AbstractLayoutEditorWidgetBuilder.WIDGET_SETTINGS_DIR);
    }

    /**
     * Shows the title screen like after a fresh launch. When in a world, the current screen stays, unless it is a custom GUI,
     * because the new profile might not have it.
     */
    private static void openFreshScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen current = ScreenUtils.getScreen();
        if (minecraft.level == null) {
            ScreenUtils.setScreen(new TitleScreen());
        } else if (current instanceof CustomGuiBaseScreen) {
            ScreenUtils.setScreen(new PauseScreen(true));
        } else {
            ScreenCustomization.reInitCurrentScreen();
        }
    }

    private static void closeWindowsExcept(@Nullable PiPWindow keepOpenWindow) {
        List<PiPWindow> windows = new ArrayList<>(PiPWindowHandler.INSTANCE.getOpenWindows());
        for (PiPWindow window : windows) {
            if ((window == keepOpenWindow) || isChildOf(window, keepOpenWindow)) continue;
            window.setCloseWindowCheck(null);
            PiPWindowHandler.INSTANCE.closeWindow(window);
        }
    }

    private static boolean isChildOf(@NotNull PiPWindow window, @Nullable PiPWindow parent) {
        if (parent == null) return false;
        PiPWindow current = window.getParentWindow();
        while (current != null) {
            if (current == parent) return true;
            current = current.getParentWindow();
        }
        return false;
    }

}
