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
public class GlobalLoadHandlerManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final GlobalLoadHandlerManager INSTANCE = new GlobalLoadHandlerManager();
    
    // Maps browser IDs to their initialization futures
    private final Map<String, List<BrowserLoadListener>> browserMap = new ConcurrentHashMap<>();
    
    // The single load handler that will be registered with CefClient
    private final CefLoadHandlerAdapter globalHandler = new CefLoadHandlerAdapter() {

        @Override
        public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
            if (!frame.isMain()) return; // Only care about main frame loads

            String browserId = getIdByCefBrowser(cefBrowser);
            if (browserId == null) {
                LOGGER.error("[FANCYMENU] Unable to process onLoadEnd because browser ID was NULL!", new NullPointerException());
                return;
            }

            LOGGER.info("[FANCYMENU] GlobalLoadHandler: onLoadEnd for browser ID {} with status {}", browserId, httpStatusCode);
            
            List<BrowserLoadListener> loadListeners = browserMap.get(browserId);
            if (loadListeners != null) {
                boolean success = (httpStatusCode >= 200 && httpStatusCode < 300) || (frame.getURL() != null && frame.getURL().startsWith("file:") && httpStatusCode == 0);
                LOGGER.info("[FANCYMENU] Browser [ID:{}] page loaded: {}, Success: {}", browserId, frame.getURL(), success);
                loadListeners.forEach(loadListener -> {
                    if (!loadListener.isHandled()) {
                        loadListener.setHandled(true);
                        loadListener.getOnLoadCompletedTask().accept(success);
                    }
                });
                loadListeners.clear();
            } else {
                LOGGER.warn("[FANCYMENU] onLoadEnd: No load listeners found for browser ID: {}", browserId);
            }
        }
        
        @Override
        public void onLoadError(CefBrowser cefBrowser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
            if (!frame.isMain()) return;

            String browserId = getIdByCefBrowser(cefBrowser);
            if (browserId == null) {
                LOGGER.error("[FANCYMENU] Unable to process onLoadError because browser ID was NULL!", new NullPointerException());
                return;
            }

            LOGGER.info("[FANCYMENU] GlobalLoadHandler: onLoadError for browser ID {} with error {}", browserId, errorCode);

            List<BrowserLoadListener> loadListeners = browserMap.get(browserId);
            if (loadListeners != null) {
                loadListeners.forEach(loadListener -> {
                    if (!loadListener.isHandled()) {
                        LOGGER.error("[FANCYMENU] Browser [ID:{}] load error: {}, {}, URL: {}", browserId, errorCode, errorText, failedUrl);
                        loadListener.setHandled(true);
                        loadListener.getOnLoadCompletedTask().accept(false);
                    }
                });
                loadListeners.clear();
            } else {
                LOGGER.warn("[FANCYMENU] onLoadError: No load listeners found for browser ID: {}", browserId);
            }
        }
    };
    
    private GlobalLoadHandlerManager() {
        // Private constructor for singleton
    }
    
    /**
     * Gets the singleton instance of the handler manager.
     */
    public static GlobalLoadHandlerManager getInstance() {
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
        if (!browserMap.containsKey(browser.getIdentifier())) {
            browserMap.put(browser.getIdentifier(), new ArrayList<>());
        }
        LOGGER.info("[FANCYMENU] Registering load listener for browser ID: {}", browser.getIdentifier());
        browserMap.get(browser.getIdentifier()).add(new BrowserLoadListener(browser, onLoadListener));
    }
    
    /**
     * Unregisters a browser from load event tracking.
     * 
     * @param browserId The ID of the browser to unregister
     */
    public void unregisterAllListenersForBrowser(String browserId) {
        if (browserMap.remove(browserId) != null) {
            LOGGER.info("[FANCYMENU] Unregistered browser with ID: {}", browserId);
        } else {
            LOGGER.info("[FANCYMENU] No browser found with ID: {} for unregistration", browserId);
        }
    }
    
    /**
     * Gets the number of registered browsers.
     */
    public int getRegisteredBrowserCount() {
        return browserMap.size();
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
        private volatile boolean handled = false;
        
        public BrowserLoadListener(WrappedMCEFBrowser browser, Consumer<Boolean> onLoadCompleted) {
            this.onLoadCompleted = onLoadCompleted;
            this.browser = browser;
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

    }

}