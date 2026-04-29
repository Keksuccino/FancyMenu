package de.keksuccino.fancymenu.customization.overlay;

import com.mojang.blaze3d.platform.GlUtil;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.PerformanceUtils;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class DebugOverlayBuilder {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    public static DebugOverlay buildDebugOverlay() {

        DebugOverlay overlay = new DebugOverlay();

        int menuBarHeight = (int)((float)MenuBar.PIXEL_SIZE * MenuBar.getRenderScale());
        overlay.setTopYOffsetSupplier(() -> menuBarHeight + 10);
        overlay.setBottomYOffsetSupplier(() -> -10);

        Screen current = Minecraft.getInstance().screen;
        if (current == null) return overlay;
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(current);
        if (layer == null) return overlay;

        addScreenCategory(overlay, current, layer);

        addResourcesCategory(overlay);

        addSystemCategory(overlay);

        overlay.addLine("right_click_elements", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.right_click_elements"));

        return overlay;

    }

    private static void addScreenCategory(@NotNull DebugOverlay overlay, @NotNull Screen currentScreen, @NotNull ScreenCustomizationLayer currentScreenLayer) {

        if (!FancyMenu.getOptions().debugOverlayShowBasicScreenCategory.getValue() && !FancyMenu.getOptions().debugOverlayShowAdvancedScreenCategory.getValue()) return;

        String currentIdentifier = ScreenIdentifierHandler.getIdentifierOfScreen(currentScreen);
        final String normalizedCurrentIdentifier = currentIdentifier != null ? currentIdentifier : "unknown";
        boolean customizationEnabled = ScreenCustomization.isCustomizationEnabledForScreen(currentScreen);
        List<Layout> allLayoutsCurrentRaw = LayoutHandler.getAllLayoutsForScreenIdentifier(normalizedCurrentIdentifier, false);
        final List<Layout> allLayoutsCurrent = allLayoutsCurrentRaw != null ? allLayoutsCurrentRaw : Collections.emptyList();
        List<Layout> allUniversalLayoutsCurrentRaw = LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true);
        final List<Layout> allUniversalLayoutsCurrent = allUniversalLayoutsCurrentRaw != null ? allUniversalLayoutsCurrentRaw : Collections.emptyList();
        final List<Layout> activeLayouts = currentScreenLayer.activeLayouts != null ? currentScreenLayer.activeLayouts : Collections.emptyList();
        final List<?> allElements = currentScreenLayer.allElements != null ? currentScreenLayer.allElements : Collections.emptyList();
        List<Layout> enabledUniversalLayoutsCurrent = new ArrayList<>();
        for (Layout l : activeLayouts) {
            if (l.isUniversalLayout()) enabledUniversalLayoutsCurrent.add(l);
        }

        if (FancyMenu.getOptions().debugOverlayShowBasicScreenCategory.getValue()) {

            overlay.addLine("screen_identifier", DebugOverlay.LinePosition.TOP_LEFT,
                            consumes -> {
                                MutableComponent c = Component.translatable("fancymenu.overlay.debug.screen_identifier", normalizedCurrentIdentifier);
                                if (consumes.isHovered()) c = c.setStyle(Style.EMPTY.withUnderlined(true));
                                if (consumes.recentlyClicked()) c = Component.translatable("fancymenu.overlay.debug.screen_identifier.copied").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                                return c;
                            })
                    .setClickAction(line -> Minecraft.getInstance().keyboardHandler.setClipboard(normalizedCurrentIdentifier));
            overlay.addLine("screen_size", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.screen_size", "" + Minecraft.getInstance().getWindow().getScreenWidth(), "" + Minecraft.getInstance().getWindow().getScreenHeight()));

        }

        if (FancyMenu.getOptions().debugOverlayShowAdvancedScreenCategory.getValue()) {

            overlay.addLine("active_layout_count", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.screen_active_layout_count", "" + (customizationEnabled ? activeLayouts.size() : 0), "" + (customizationEnabled ? enabledUniversalLayoutsCurrent.size() : 0)));
            overlay.addLine("total_layout_count", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.screen_total_layout_count", "" + (allLayoutsCurrent.size() + allUniversalLayoutsCurrent.size()), "" + allUniversalLayoutsCurrent.size()));
            overlay.addLine("active_elements_count", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.screen_active_element_count", "" + (customizationEnabled ? allElements.size() : 0)));

        }

        overlay.addSpacerLine("spacer_after_active_element_count", DebugOverlay.LinePosition.TOP_LEFT, 5);

    }

    private static void addResourcesCategory(@NotNull DebugOverlay overlay) {

        if (!FancyMenu.getOptions().debugOverlayShowResourcesCategory.getValue()) return;

        List<ExternalTextureSlideshowRenderer> slideshows = new ArrayList<>(SlideshowHandler.getSlideshows());
        int slideshowCount = slideshows.size();
        int totalSlideshowImages = 0;
        for (ExternalTextureSlideshowRenderer slide : slideshows) {
            if (slide != null) totalSlideshowImages += slide.getImageCount();
        }
        final int finalTotalSlideshowImages = totalSlideshowImages;

        overlay.addLine("total_slideshows", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.loaded_slideshows", "" + slideshowCount, "" + finalTotalSlideshowImages));

        overlay.addSpacerLine("spacer_after_total_slideshows", DebugOverlay.LinePosition.TOP_LEFT, 5);

    }

    private static void addSystemCategory(@NotNull DebugOverlay overlay) {

        if (!FancyMenu.getOptions().debugOverlayShowSystemCategory.getValue()) return;

        overlay.addLine("frames_per_second", DebugOverlay.LinePosition.TOP_LEFT, consumes -> {
            int fps = Minecraft.getInstance().getFps();
            MutableComponent fpsComp = Component.literal("" + fps);
            if (fps < 20) fpsComp = fpsComp.setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD));
            if (fps < 10) fpsComp = fpsComp.setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            return Component.translatable("fancymenu.overlay.debug.fps", fpsComp);
        });
        overlay.addGraphLine("frames_per_second_graph", DebugOverlay.LinePosition.TOP_LEFT,
                        () -> (double)Minecraft.getInstance().getFps(),
                        0.0D, 240.0D)
                .setInvertColorGradient(true);
        overlay.addLine("ram_usage", DebugOverlay.LinePosition.TOP_LEFT, consumes -> {
            long max = Runtime.getRuntime().maxMemory();
            long total = Runtime.getRuntime().totalMemory();
            long free = Runtime.getRuntime().freeMemory();
            long used = Math.max(0L, total - free);
            long percent = (max > 0L) ? Math.max(0L, Math.min(100L, (long)(((double)used * 100.0D) / (double)max))) : 0L;
            String ramString = bytesToMb(used) + "/" + bytesToMb(max) + "MB";
            String percentString = percent + "%";
            if (percent >= 60) percentString = ChatFormatting.GOLD + "" + percent + "%" + ChatFormatting.RESET;
            if (percent >= 80) percentString = ChatFormatting.RED + "" + percent + "%" + ChatFormatting.RESET;
            return Component.translatable("fancymenu.overlay.debug.memory", ramString, percentString);
        });
        overlay.addGraphLine("ram_usage_graph", DebugOverlay.LinePosition.TOP_LEFT, () -> {
            long max = Runtime.getRuntime().maxMemory();
            long total = Runtime.getRuntime().totalMemory();
            long free = Runtime.getRuntime().freeMemory();
            long used = Math.max(0L, total - free);
            if (max <= 0L) return 0.0D;
            double percent = ((double)used * 100.0D) / (double)max;
            if (!Double.isFinite(percent)) return 0.0D;
            return Math.max(0.0D, Math.min(100.0D, percent));
        }, 0.0D, 100.0D);
        overlay.addLine("cpu_jvm_usage", DebugOverlay.LinePosition.TOP_LEFT, consumes -> {
            double usage = PerformanceUtils.getJvmCpuUsage();
            if (usage < 0D) usage = 0D;
            usage = usage * 100.0D;
            usage = MathUtils.round(usage, 1);
            String usageString = usage + "%";
            if (usage > 50) usageString = ChatFormatting.GOLD + "" + usage + "%" + ChatFormatting.RESET;
            if (usage >= 80) usageString = ChatFormatting.RED + "" + usage + "%" + ChatFormatting.RESET;
            return Component.translatable("fancymenu.overlay.debug.cpu_usage.jvm", usageString);
        });
        overlay.addGraphLine("cpu_jvm_usage_graph", DebugOverlay.LinePosition.TOP_LEFT, () -> {
            double usage = PerformanceUtils.getJvmCpuUsage();
            if (usage < 0D) usage = 0D;
            return usage * 100.0D;
        }, 0.0D, 100.0D);
        overlay.addLine("cpu_os_usage", DebugOverlay.LinePosition.TOP_LEFT, consumes -> {
            double usage = PerformanceUtils.getOsCpuUsage();
            if (usage < 0D) usage = 0D;
            usage = usage * 100.0D;
            usage = MathUtils.round(usage, 1);
            String usageString = usage + "%";
            if (usage > 50) usageString = ChatFormatting.GOLD + "" + usage + "%" + ChatFormatting.RESET;
            if (usage >= 80) usageString = ChatFormatting.RED + "" + usage + "%" + ChatFormatting.RESET;
            return Component.translatable("fancymenu.overlay.debug.cpu_usage.os", usageString);
        });
        overlay.addGraphLine("cpu_os_usage_graph", DebugOverlay.LinePosition.TOP_LEFT, () -> {
            double usage = PerformanceUtils.getOsCpuUsage();
            if (usage < 0D) usage = 0D;
            return usage * 100.0D;
        }, 0.0D, 100.0D);
        overlay.addLine("cpu_info", DebugOverlay.LinePosition.TOP_LEFT, consumes ->
                Component.translatable("fancymenu.overlay.debug.cpu", safeReadSystemInfo("cpu_info", GlUtil::getCpuInfo))
        );
        overlay.addLine("gpu_info", DebugOverlay.LinePosition.TOP_LEFT, consumes ->
                Component.translatable(
                        "fancymenu.overlay.debug.gpu",
                        safeReadSystemInfo("gpu_renderer", GlUtil::getRenderer),
                        safeReadSystemInfo("gpu_opengl", GlUtil::getOpenGLVersion)
                )
        );

        overlay.addSpacerLine("spacer_after_gpu_info", DebugOverlay.LinePosition.TOP_LEFT, 5);

    }

    private static long bytesToMb(long bytes) {
        return bytes / 1024L / 1024L;
    }

    @NotNull
    private static String safeReadSystemInfo(@NotNull String key, @NotNull Supplier<String> supplier) {
        try {
            String value = supplier.get();
            return value != null ? value : "Unknown";
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to read debug overlay system info '{}'", key, ex);
            return "Unknown";
        }
    }

}
