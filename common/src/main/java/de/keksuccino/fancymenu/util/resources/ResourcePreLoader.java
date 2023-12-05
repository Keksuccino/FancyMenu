package de.keksuccino.fancymenu.util.resources;

import de.keksuccino.fancymenu.FancyMenu;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class ResourcePreLoader {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOURCE_SEPARATOR = "%!source_end!%";

    //TODO eventuell noch ein "isLoadingFinished" und "isLoadingFailed" zu Resources adden und dann auf eins von beiden warten, wenn boolean true in preLoadAll()
    //TODO eventuell noch ein "isLoadingFinished" und "isLoadingFailed" zu Resources adden und dann auf eins von beiden warten, wenn boolean true in preLoadAll()
    //TODO eventuell noch ein "isLoadingFinished" und "isLoadingFailed" zu Resources adden und dann auf eins von beiden warten, wenn boolean true in preLoadAll()
    //TODO eventuell noch ein "isLoadingFinished" und "isLoadingFailed" zu Resources adden und dann auf eins von beiden warten, wenn boolean true in preLoadAll()

    public static void preLoadAll(long waitForReadyMillis) {
        try {
            for (ResourceSource source : getRegisteredResourceSources()) {
                ResourceHandler<?,?> handler = ResourceHandlers.findHandlerForSource(source, false);
                if (handler == null) handler = ResourceHandlers.findHandlerForSource(source, true);
                if (handler != null) {
                    //this will construct (and register) the resource and should trigger its asynchronous loading process
                    Resource resource = handler.get(source);
                    if (resource != null) {
                        if (waitForReadyMillis > 0) resource.waitForReady(waitForReadyMillis);
                    } else {
                        LOGGER.error("[FANCYMENU] Failed to pre-load resource! Resource returned by ResourceHandler was NULL for: " + source.getSourceWithPrefix());
                    }
                } else {
                    LOGGER.error("[FANCYMENU] Failed to pre-load resource! No ResourceHandler found for: " + source.getSourceWithPrefix());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error while pre-loading resources!", ex);
        }
    }

    @NotNull
    public static List<ResourceSource> getRegisteredResourceSources() {
        List<ResourceSource> sources = new ArrayList<>();
        String raw = FancyMenu.getOptions().preLoadResources.getValue();
        if (raw.trim().isEmpty()) return sources;
        if (!raw.contains(SOURCE_SEPARATOR)) {
            sources.add(ResourceSource.of(raw));
            return sources;
        }
        for (String s : StringUtils.split(raw, SOURCE_SEPARATOR)) {
            if (!s.trim().isEmpty()) sources.add(ResourceSource.of(s));
        }
        return sources;
    }

    public static boolean isResourceSourceRegistered(@NotNull ResourceSource source) {
        for (ResourceSource s : getRegisteredResourceSources()) {
            if (StringUtils.equals(s.getSourceWithPrefix(), source.getSourceWithPrefix())) return true;
        }
        return false;
    }

    public static void addResourceSource(@NotNull ResourceSource source) {
        if (!isResourceSourceRegistered(source)) {
            String old = FancyMenu.getOptions().preLoadResources.getValue();
            FancyMenu.getOptions().preLoadResources.setValue(old + source.getSourceWithPrefix() + SOURCE_SEPARATOR);
        }
    }

    public static void removeResourceSource(@NotNull ResourceSource source) {
        List<ResourceSource> sources = getRegisteredResourceSources();
        sources.removeIf(resourceSource -> StringUtils.equals(resourceSource.getSourceWithPrefix(), source.getSourceWithPrefix()));
        FancyMenu.getOptions().preLoadResources.resetToDefault();
        StringBuilder builder = new StringBuilder();
        for (ResourceSource s2 : sources) {
            builder.append(s2.getSourceWithPrefix()).append(SOURCE_SEPARATOR);
        }
        FancyMenu.getOptions().preLoadResources.setValue(builder.toString());
    }

}
