package de.keksuccino.fancymenu.util.resource.preload;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.resource.Resource;
import de.keksuccino.fancymenu.util.resource.ResourceHandler;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ResourcePreLoader {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOURCE_SEPARATOR = "%!source_end!%";

    public static void preLoadAll(long waitForCompletedMillis) {
        LOGGER.info("[FANCYMENU] Pre-loading resources..");
        for (ResourceSource source : getRegisteredResourceSources(null)) {
            try {
                ResourceHandler<?,?> handler = ResourceHandlers.findHandlerForSource(source, true);
                if (handler != null) {
                    //This will construct (and register) the resource and should trigger its asynchronous loading process
                    Resource resource = handler.get(source);
                    if (resource != null) {
                        if (waitForCompletedMillis > 0) {
                            resource.waitForLoadingCompletedOrFailed(waitForCompletedMillis);
                            if (resource.isLoadingFailed()) {
                                LOGGER.error("[FANCYMENU] Failed to pre-load resource! Loading failed for: " + source.getSourceWithPrefix());
                            }
                            if (!resource.isLoadingFailed() && !resource.isLoadingCompleted()) {
                                LOGGER.error("[FANCYMENU] Pre-loading resource took too long! Will not finish pre-loading resource: " + source.getSourceWithPrefix());
                            }
                        }
                    } else {
                        LOGGER.error("[FANCYMENU] Failed to pre-load resource! Resource returned by ResourceHandler was NULL for: " + source.getSourceWithPrefix());
                    }
                } else {
                    LOGGER.error("[FANCYMENU] Failed to pre-load resource! No ResourceHandler found for: " + source.getSourceWithPrefix());
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Error while pre-loading resource: " + source.getSourceWithPrefix(), ex);
            }
        }
    }

    @NotNull
    public static List<ResourceSource> getRegisteredResourceSources(@Nullable String serialized) {
        List<ResourceSource> sources = new ArrayList<>();
        try {
            if (serialized == null) serialized = FancyMenu.getOptions().preLoadResources.getValue();
            if (serialized.trim().isEmpty()) return sources;
            if (!serialized.contains(SOURCE_SEPARATOR)) {
                sources.add(ResourceSource.of(serialized));
                return sources;
            }
            for (String s : serialized.split(SOURCE_SEPARATOR)) {
                if (!s.trim().isEmpty()) sources.add(ResourceSource.of(s));
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get resource sources for pre-loading!", ex);
        }
        return sources;
    }

    public static boolean isResourceSourceRegistered(@NotNull ResourceSource source, @Nullable String serialized) {
        for (ResourceSource s : getRegisteredResourceSources(serialized)) {
            if (StringUtils.equals(s.getSerializationSource(), source.getSerializationSource())) return true;
        }
        return false;
    }

    @NotNull
    public static String addResourceSource(@NotNull ResourceSource source, @Nullable String serialized, boolean syncToConfig) {
        if (serialized == null) serialized = FancyMenu.getOptions().preLoadResources.getValue();
        if (!isResourceSourceRegistered(source, serialized)) {
            String ret = serialized + source.getSerializationSource() + SOURCE_SEPARATOR;
            if (syncToConfig) FancyMenu.getOptions().preLoadResources.setValue(ret);
            return ret;
        }
        return serialized;
    }

    @NotNull
    public static String removeResourceSource(@NotNull ResourceSource source, @Nullable String serialized, boolean syncToConfig) {
        List<ResourceSource> sources = getRegisteredResourceSources(serialized);
        sources.removeIf(resourceSource -> StringUtils.equals(resourceSource.getSerializationSource(), source.getSerializationSource()));
        StringBuilder builder = new StringBuilder();
        for (ResourceSource s2 : sources) {
            builder.append(s2.getSerializationSource()).append(SOURCE_SEPARATOR);
        }
        String ret = builder.toString();
        if (syncToConfig) {
            FancyMenu.getOptions().preLoadResources.setValue(ret);
        }
        return ret;
    }

}
