package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.web.WebUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.*;

public class WebTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Object EVENT_REGISTRATION_LOCK = new Object();
    private static final WebTextPlaceholderCache CACHE = new WebTextPlaceholderCache(task -> FancyMenuThreads.startDaemonThread(task, "WebTextPlaceholder-WebLoader"), link -> WebUtils.isValidUrl(link) ? WebTextPlaceholderCache.LoadResult.valid(WebUtils.getPlainTextContentOfPage(new URL(link))) : WebTextPlaceholderCache.LoadResult.invalid());
    private static boolean eventsRegistered;

    public WebTextPlaceholder() {
        super("webtext");
        registerEventsIfNeeded();
    }

    private static void registerEventsIfNeeded() {
        synchronized (EVENT_REGISTRATION_LOCK) {
            if (!eventsRegistered) {
                EventHandler.INSTANCE.registerListenersOf(WebTextPlaceholder.class);
                eventsRegistered = true;
            }
        }
    }

    @EventListener
    public static void onReload(ModReloadEvent e) {
        CACHE.reload();
        LOGGER.info("[FANCYMENU] V2 WebTextPlaceholder cache successfully cleared!");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String link = dps.values.get("link");
        if (link != null) {
            link = StringUtils.convertFormatCodes(link, "§", "&");
            WebTextPlaceholderCache.Lookup lookup = CACHE.getOrLoad(dps.placeholderString, link);
            if (lookup.status() == WebTextPlaceholderCache.Status.INVALID) return null;
            if (lookup.status() == WebTextPlaceholderCache.Status.LOADING) return "";
            if (!lookup.lines().isEmpty()) return lookup.lines().get(0);
        }
        return null;
    }

    protected static boolean isInvalidWebPlaceholderLink(String link) {
        return CACHE.isInvalidLink(link);
    }

    protected static List<String> getCachedWebPlaceholder(String placeholder) {
        return CACHE.getCached(placeholder);
    }

    protected static boolean isWebPlaceholderUpdating(String placeholder) {
        return CACHE.isLoading(placeholder);
    }

    protected static void cacheWebPlaceholder(String placeholder, String link) {
        CACHE.loadIfAbsent(placeholder, link);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("link");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.webtext");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.webtext.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        dps.values.put("link", "http://somewebsite.com/textfile.txt");
        return dps;
    }

}
