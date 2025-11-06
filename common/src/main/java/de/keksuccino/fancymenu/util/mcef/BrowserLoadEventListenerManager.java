package de.keksuccino.fancymenu.util.mcef;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A centralized manager for browser load events.
 * Works around the limitation that CefClient can only have one load handler at a time.
 */
public class BrowserLoadEventListenerManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final BrowserLoadEventListenerManager INSTANCE = new BrowserLoadEventListenerManager();
    
    // Maps browser IDs to their initialization futures
    private final Map<String, List<BrowserLoadListener>> browserMap = new ConcurrentHashMap<>();
    
    // The single load handler that will be registered with CefClient
    private final CefLoadHandlerAdapter globalHandler = new CefLoadHandlerAdapter() {

        @Override
        public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
            if (!frame.isMain()) return; // Only care about main frame loads

            String browserId = getIdByCefBrowser(cefBrowser);
            if (browserId == null) return;
            
            List<BrowserLoadListener> loadListeners = browserMap.get(browserId);
            if (loadListeners != null) {
                boolean success = (httpStatusCode >= 200 && httpStatusCode < 300) || (frame.getURL() != null && frame.getURL().startsWith("file:") && httpStatusCode == 0);
                synchronized (loadListeners) {
                    loadListeners.removeIf(loadListener -> processListener(loadListener, success));
                }
            } else {
                LOGGER.warn("[FANCYMENU] onLoadEnd: No load listeners found for browser ID: {}", browserId);
            }
        }
        
        @Override
        public void onLoadError(CefBrowser cefBrowser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
            if (!frame.isMain()) return;

            String browserId = getIdByCefBrowser(cefBrowser);
            if (browserId == null) return;

            List<BrowserLoadListener> loadListeners = browserMap.get(browserId);
            if (loadListeners != null) {
                synchronized (loadListeners) {
                    loadListeners.removeIf(loadListener -> {
                        LOGGER.error("[FANCYMENU] Browser [ID:{}] load error: {}, {}, URL: {}", browserId, errorCode, errorText, failedUrl);
                        return processListener(loadListener, false);
                    });
                }
            } else {
                LOGGER.warn("[FANCYMENU] onLoadError: No load listeners found for browser ID: {}", browserId);
            }
        }
    };
    
    private BrowserLoadEventListenerManager() {
        // Private constructor for singleton
    }
    
    /**
     * Gets the singleton instance of the handler manager.
     */
    public static BrowserLoadEventListenerManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Gets the global load handler that should be registered with CefClient.
     */
    public CefLoadHandler getGlobalHandler() {
        return globalHandler;
    }
    
    /**
     * Registers a browser load listener for load event tracking.
     */
    public void registerListenerForBrowser(@NotNull WrappedMCEFBrowser browser, @NotNull Consumer<Boolean> onLoadListener) {
        registerListenerForBrowserInternal(browser, onLoadListener, false);
    }
    
    /**
     * Registers a persistent browser load listener that should fire for every load event.
     */
    public void registerPersistentListenerForBrowser(@NotNull WrappedMCEFBrowser browser, @NotNull Consumer<Boolean> onLoadListener) {
        registerListenerForBrowserInternal(browser, onLoadListener, true);
    }
    
    /**
     * Unregisters a browser from load event tracking.
     * 
     * @param browserId The ID of the browser to unregister
     */
    public void unregisterAllListenersForBrowser(String browserId) {
        browserMap.remove(browserId);
    }

    @Nullable
    public String getIdByCefBrowser(@NotNull CefBrowser cefBrowser) {
        for (Map.Entry<String, List<BrowserLoadListener>> m : this.browserMap.entrySet()) {
            if (m.getValue().isEmpty()) continue;
            BrowserLoadListener listener1 = m.getValue().get(0);
            if (Objects.equals(listener1.getBrowser().getBrowser(), cefBrowser)) return m.getKey();
        }
        return null;
    }

    private static class BrowserLoadListener {

        private final Consumer<Boolean> onLoadCompleted;
        private final WrappedMCEFBrowser browser;
        private final boolean persistent;
        private volatile boolean handled = false;
        
        public BrowserLoadListener(WrappedMCEFBrowser browser, Consumer<Boolean> onLoadCompleted, boolean persistent) {
            this.onLoadCompleted = onLoadCompleted;
            this.browser = browser;
            this.persistent = persistent;
        }
        
        public Consumer<Boolean> getOnLoadCompletedTask() {
            return this.onLoadCompleted;
        }

        public WrappedMCEFBrowser getBrowser() {
            return this.browser;
        }
        
        public boolean isHandled() {
            return this.handled;
        }
        
        public void setHandled(boolean handled) {
            this.handled = handled;
        }
        
        public boolean isPersistent() {
            return this.persistent;
        }

    }
    
    private void registerListenerForBrowserInternal(@NotNull WrappedMCEFBrowser browser, @NotNull Consumer<Boolean> onLoadListener, boolean persistent) {
        browserMap.computeIfAbsent(browser.getIdentifier(), id -> new ArrayList<>());
        List<BrowserLoadListener> listeners = browserMap.get(browser.getIdentifier());
        synchronized (listeners) {
            listeners.add(new BrowserLoadListener(browser, onLoadListener, persistent));
        }
    }
    
    private boolean processListener(@NotNull BrowserLoadListener loadListener, boolean success) {
        if (!loadListener.isHandled()) {
            loadListener.setHandled(true);
            loadListener.getOnLoadCompletedTask().accept(success);
        }
        if (loadListener.isPersistent()) {
            loadListener.setHandled(false);
            return false;
        }
        return true;
    }

}
