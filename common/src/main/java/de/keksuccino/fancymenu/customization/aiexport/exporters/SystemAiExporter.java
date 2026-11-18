package de.keksuccino.fancymenu.customization.aiexport.exporters;

import de.keksuccino.fancymenu.Compat;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.aiexport.AiExportDocument;
import de.keksuccino.fancymenu.customization.aiexport.AiExportText;
import de.keksuccino.fancymenu.customization.aiexport.AiExportWriter;
import de.keksuccino.fancymenu.customization.aiexport.AiExports;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerHandler;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerInstance;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.SystemSpecsUtils;
import de.keksuccino.fancymenu.util.ffmpeg.downloader.FFMPEGDownloader;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.mod.UniversalModContainer;
import de.keksuccino.fancymenu.util.rinku.RinkuUtil;
import de.keksuccino.fancymenu.util.watermedia.WatermediaUtil;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import java.util.Comparator;
import java.util.List;

/**
 * Exports info about the game, the hardware, the installed mods and the state of FancyMenu for AI chats.
 */
public final class SystemAiExporter {

    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private SystemAiExporter() {
    }

    @NotNull
    public static AiExportDocument export() {
        return AiExports.create("System Info", "system_info", writer -> {
            writeGame(writer);
            writeFancyMenu(writer);
            writeOptionalDependencies(writer);
            writeMods(writer);
        });
    }

    private static void writeGame(@NotNull AiExportWriter writer) {
        Minecraft minecraft = Minecraft.getInstance();
        writer.heading(2, "Game and Hardware");
        writer.fieldText("Window", minecraft.getWindow().getWidth() + "x" + minecraft.getWindow().getHeight() + " real pixels, GUI scale " + WindowHandler.getGuiScale());
        writer.field("CPU", AiExportText.safe(SystemSpecsUtils::getCpuInfo, "unknown"));
        writer.field("GPU", AiExportText.safe(SystemSpecsUtils::getGpuRenderer, "unknown"));
        writer.field("Graphics driver", AiExportText.safe(SystemSpecsUtils::getGpuDriverInfo, "unknown"));
        Runtime runtime = Runtime.getRuntime();
        writer.fieldText("Java memory", ((runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MEGABYTE) + " MB used of " + (runtime.maxMemory() / BYTES_PER_MEGABYTE) + " MB");
        writer.field("Game directory", AiExportText.safe(() -> AiExportText.path(GameDirectoryUtils.getGameDirectory()), "unknown"));
        writer.fieldText("Development environment", AiExportText.yesNo(Services.PLATFORM.isDevelopmentEnvironment()));
    }

    private static void writeFancyMenu(@NotNull AiExportWriter writer) {
        writer.heading(2, "FancyMenu");
        writer.fieldText("Modpack mode", AiExportText.yesNo(FancyMenu.getOptions().modpackMode.getValue()));
        writer.fieldText("Advanced customization mode", AiExportText.yesNo(FancyMenu.getOptions().advancedCustomizationMode.getValue()));
        writer.fieldText("Customization menu bar shown", AiExportText.yesNo(FancyMenu.getOptions().showCustomizationOverlay.getValue()));
        List<Layout> layouts = LayoutHandler.getAllLayouts();
        long enabledLayouts = layouts.stream().filter(Layout::isEnabled).count();
        long universalLayouts = layouts.stream().filter(Layout::isUniversalLayout).count();
        writer.fieldText("Layouts", layouts.size() + " (" + enabledLayouts + " enabled, " + universalLayouts + " universal)");
        writer.fieldText("Custom GUIs", CustomGuiHandler.getGuis().size() + " (" + CustomGuiHandler.getOverriddenScreens().size() + " screens get replaced by custom GUIs)");
        writer.fieldText("Variables", String.valueOf(VariableHandler.getVariables().size()));
        writer.fieldText("Listeners", String.valueOf(ListenerHandler.getInstances().size()));
        List<SchedulerInstance> schedulers = SchedulerHandler.getInstances();
        writer.fieldText("Schedulers", schedulers.size() + " (" + schedulers.stream().filter(SchedulerHandler::isRunning).count() + " running right now)");
        writer.field("FancyMenu folder", AiExportText.path(FancyMenu.MOD_DIR));
    }

    private static void writeOptionalDependencies(@NotNull AiExportWriter writer) {
        writer.heading(2, "Optional Dependencies and Compatibility");
        writer.paragraph("Some features of FancyMenu need other mods or tools.");
        writer.fieldText("WaterMedia (needed for video backgrounds, video elements and some audio formats)", "installed: " + AiExportText.yesNo(WatermediaUtil.isWatermediaLoaded())
                + ", binaries installed: " + AiExportText.yesNo(WatermediaUtil.isWatermediaBinariesLoaded())
                + ", video playback works: " + AiExportText.yesNo(WatermediaUtil.isWatermediaVideoPlaybackAvailable()));
        writer.fieldText("Rinku (needed for browser elements, browser backgrounds and web videos)", "installed: " + AiExportText.yesNo(RinkuUtil.isRinkuLoaded()) + ", started: " + AiExportText.yesNo(RinkuUtil.rinku_initialized));
        writer.fieldText("FFmpeg (needed by the AFMA Creator tool, FancyMenu downloads it when needed)", "downloaded: " + AiExportText.yesNo(!FFMPEGDownloader.isDownloadNeeded()));
        writer.fieldText("Old FancyMenu Audio Extension add-on installed", AiExportText.yesNo(Compat.isAudioExtensionLoaded()));
        writer.fieldText("Old FancyMenu Video Extension add-on installed", AiExportText.yesNo(Compat.isVideoExtensionLoaded()));
        writer.fieldText("OptiFine installed", AiExportText.yesNo(Compat.isOptiFineLoaded()));
    }

    private static void writeMods(@NotNull AiExportWriter writer) {
        List<UniversalModContainer> mods = Services.PLATFORM.getLoadedMods().stream().sorted(Comparator.comparing(UniversalModContainer::id)).toList();
        writer.heading(2, "Installed Mods (" + mods.size() + ")");
        for (UniversalModContainer mod : mods) {
            String version = AiExportText.safe(() -> Services.PLATFORM.getModVersion(mod.id()), "unknown");
            writer.bullet(AiExportWriter.inlineCode(mod.id()) + " " + AiExportText.plain(mod.name()) + " " + AiExportWriter.inlineCode(version));
        }
    }

}
