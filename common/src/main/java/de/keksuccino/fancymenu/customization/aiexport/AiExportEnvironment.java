package de.keksuccino.fancymenu.customization.aiexport;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.platform.Services;
import org.jetbrains.annotations.NotNull;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Environment info that every AI export starts with. Works on clients and dedicated servers.
 */
public final class AiExportEnvironment {

    private static final DateTimeFormatter EXPORT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx", Locale.ROOT);

    private AiExportEnvironment() {
    }

    public static void writeFields(@NotNull AiExportWriter writer) {
        writer.field("FancyMenu version", FancyMenu.VERSION);
        writer.field("Minecraft version", AiExportText.safe(FancyMenu::getMinecraftVersion, "unknown"));
        writer.field("Mod loader", AiExportText.safe(() -> Services.PLATFORM.getPlatformDisplayName() + " " + Services.PLATFORM.getLoaderVersion(), "unknown"));
        writer.field("Java", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        writer.field("Operating system", System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
        writer.field("Exported at", ZonedDateTime.now().format(EXPORT_TIME_FORMAT));
    }

}
