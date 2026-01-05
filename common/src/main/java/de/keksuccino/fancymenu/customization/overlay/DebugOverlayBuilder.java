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
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class DebugOverlayBuilder {

    @NotNull
    public static DebugOverlay buildDebugOverlay(@NotNull MenuBar menuBar) {

        DebugOverlay overlay = new DebugOverlay();

        overlay.setLineTextShadow(false);

        int menuBarHeight = (int)((float)menuBar.getHeight() * UIBase.calculateFixedScale(menuBar.getBaseScale()));
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
        boolean customizationEnabled = ScreenCustomization.isCustomizationEnabledForScreen(currentScreen);
        List<Layout> allLayoutsCurrent = LayoutHandler.getAllLayoutsForScreenIdentifier(currentIdentifier, false);
        List<Layout> allUniversalLayoutsCurrent = LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true);
        List<Layout> enabledUniversalLayoutsCurrent = new ArrayList<>();
        for (Layout l : currentScreenLayer.activeLayouts) {
            if (l.isUniversalLayout()) enabledUniversalLayoutsCurrent.add(l);
        }

        if (FancyMenu.getOptions().debugOverlayShowBasicScreenCategory.getValue()) {

            overlay.addLine("screen_identifier", DebugOverlay.LinePosition.TOP_LEFT,
                            consumes -> {
                                MutableComponent c = Component.translatable("fancymenu.overlay.debug.screen_identifier", currentIdentifier);
                                if (consumes.isHovered()) c = c.setStyle(Style.EMPTY.withUnderlined(true));
                                if (consumes.recentlyClicked()) c = Component.translatable("fancymenu.overlay.debug.screen_identifier.copied").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                                return c;
                            })
                    .setClickAction(line -> Minecraft.getInstance().keyboardHandler.setClipboard(currentIdentifier));
            overlay.addLine("screen_size", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.screen_size", "" + Minecraft.getInstance().getWindow().getScreenWidth(), "" + Minecraft.getInstance().getWindow().getScreenHeight()));

        }

        if (FancyMenu.getOptions().debugOverlayShowAdvancedScreenCategory.getValue()) {

            overlay.addLine("active_layout_count", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.screen_active_layout_count", "" + (customizationEnabled ? currentScreenLayer.activeLayouts.size() : 0), "" + (customizationEnabled ? enabledUniversalLayoutsCurrent.size() : 0)));
            overlay.addLine("total_layout_count", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.screen_total_layout_count", "" + (allLayoutsCurrent.size() + allUniversalLayoutsCurrent.size()), "" + allUniversalLayoutsCurrent.size()));
            overlay.addLine("active_elements_count", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.screen_active_element_count", "" + (customizationEnabled ? currentScreenLayer.allElements.size() : 0)));

        }

        overlay.addSpacerLine("spacer_after_active_element_count", DebugOverlay.LinePosition.TOP_LEFT, 5);

    }

    private static void addResourcesCategory(@NotNull DebugOverlay overlay) {

        if (!FancyMenu.getOptions().debugOverlayShowResourcesCategory.getValue()) return;

        int slideshowCount = SlideshowHandler.getSlideshows().size();
        int totalSlideshowImages = 0;
        for (ExternalTextureSlideshowRenderer slide : SlideshowHandler.getSlideshows()) {
            totalSlideshowImages += slide.getImageCount();
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
        overlay.addLine("ram_usage", DebugOverlay.LinePosition.TOP_LEFT, consumes -> {
            long max = Runtime.getRuntime().maxMemory();
            long total = Runtime.getRuntime().totalMemory();
            long free = Runtime.getRuntime().freeMemory();
            long used = total - free;
            long percent = (total - free) * 100L / max;
            String ramString = bytesToMb(used) + "/" + bytesToMb(max) + "MB";
            String percentString = percent + "%";
            if (percent >= 60) percentString = ChatFormatting.GOLD + "" + percent + "%" + ChatFormatting.RESET;
            if (percent >= 80) percentString = ChatFormatting.RED + "" + percent + "%" + ChatFormatting.RESET;
            return Component.translatable("fancymenu.overlay.debug.memory", ramString, percentString);
        });
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
        overlay.addLine("cpu_info", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.cpu", GlUtil.getCpuInfo()));
        overlay.addLine("gpu_info", DebugOverlay.LinePosition.TOP_LEFT, consumes -> Component.translatable("fancymenu.overlay.debug.gpu", GlUtil.getRenderer(), GlUtil.getOpenGLVersion()));

        overlay.addSpacerLine("spacer_after_gpu_info", DebugOverlay.LinePosition.TOP_LEFT, 5);

    }

    private static long bytesToMb(long bytes) {
        return bytes / 1024L / 1024L;
    }

}
