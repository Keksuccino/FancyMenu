package de.keksuccino.fancymenu.util.mcef;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A centralized manager for browser load events.
 * Works around the limitation that CefClient can only have one load handler at a time.
 */
public class GlobalLoadHandlerManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final GlobalLoadHandlerManager INSTANCE = new GlobalLoadHandlerManager();
    
    // Maps browser IDs to their initialization futures
    private final Map<Integer, BrowserLoadInfo> browserMap = new ConcurrentHashMap<>();
    
    // The single load handler that will be registered with CefClient
    private final CefLoadHandlerAdapter globalHandler = new CefLoadHandlerAdapter() {
        @Override
        public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
            if (!frame.isMain()) return; // Only care about main frame loads
            
            int browserId = cefBrowser.getIdentifier();
            LOGGER.debug("[FANCYMENU] GlobalLoadHandler: onLoadEnd for browser ID {} with status {}", browserId, httpStatusCode);
            
            BrowserLoadInfo loadInfo = browserMap.get(browserId);
            
            if (loadInfo != null && !loadInfo.isHandled()) {
                boolean success = (httpStatusCode >= 200 && httpStatusCode < 300) || 
                                  (frame.getURL() != null && frame.getURL().startsWith("file:") && httpStatusCode == 0);
                
                LOGGER.info("[FANCYMENU] Browser [ID:{}] page loaded: {}, Success: {}", browserId, frame.getURL(), success);
                
                loadInfo.setHandled(true);
                loadInfo.getFuture().complete(success);
            } else if (loadInfo == null) {
                LOGGER.warn("[FANCYMENU] onLoadEnd: No load info found for browser ID: {}", browserId);
            }
        }
        
        @Override
        public void onLoadError(CefBrowser cefBrowser, CefFrame frame, CefLoadHandler.ErrorCode errorCode, 
                               String errorText, String failedUrl) {
            if (!frame.isMain()) return;
            
            int browserId = cefBrowser.getIdentifier();
            LOGGER.debug("[FANCYMENU] GlobalLoadHandler: onLoadError for browser ID {} with error {}", browserId, errorCode);
            
            BrowserLoadInfo loadInfo = browserMap.get(browserId);
            
            if (loadInfo != null && !loadInfo.isHandled()) {
                LOGGER.error("[FANCYMENU] Browser [ID:{}] load error: {}, {}, URL: {}", 
                             browserId, errorCode, errorText, failedUrl);
                             
                loadInfo.setHandled(true);
                loadInfo.getFuture().complete(false);
            } else if (loadInfo == null) {
                LOGGER.warn("[FANCYMENU] onLoadError: No load info found for browser ID: {}", browserId);
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
     * Registers a browser for load event tracking.
     * 
     * @param browserId The ID of the browser to track
     * @param future The future to complete when the load completes
     * @return True if registration was successful, false if already registered or invalid ID
     */
    public boolean registerBrowser(int browserId, CompletableFuture<Boolean> future) {
        // Check if the browser ID is valid
        if (browserId <= 0) {
            LOGGER.error("[FANCYMENU] Cannot register browser with invalid ID: {}", browserId);
            // Complete the future immediately as a failure
            future.complete(false);
            return false;
        }
        
        if (browserMap.containsKey(browserId)) {
            LOGGER.warn("[FANCYMENU] Browser with ID {} is already registered", browserId);
            return false;
        }
        
        LOGGER.debug("[FANCYMENU] Registering browser with ID: {}", browserId);
        browserMap.put(browserId, new BrowserLoadInfo(future));
        return true;
    }
    
    /**
     * Unregisters a browser from load event tracking.
     * 
     * @param browserId The ID of the browser to unregister
     */
    public void unregisterBrowser(int browserId) {
        if (browserMap.remove(browserId) != null) {
            LOGGER.debug("[FANCYMENU] Unregistered browser with ID: {}", browserId);
        } else {
            LOGGER.debug("[FANCYMENU] No browser found with ID: {} for unregistration", browserId);
        }
    }
    
    /**
     * Gets the number of registered browsers.
     */
    public int getRegisteredBrowserCount() {
        return browserMap.size();
    }
    
    /**
     * Information about a browser's load state.
     */
    private static class BrowserLoadInfo {
        private final CompletableFuture<Boolean> future;
        private volatile boolean handled = false;
        
        public BrowserLoadInfo(CompletableFuture<Boolean> future) {
            this.future = future;
        }
        
        public CompletableFuture<Boolean> getFuture() {
            return future;
        }
        
        public boolean isHandled() {
            return handled;
        }
        
        public void setHandled(boolean handled) {
            this.handled = handled;
        }
    }
}