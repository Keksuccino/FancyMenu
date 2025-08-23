package de.keksuccino.fancymenu.util.mcef;

import com.cinemamod.mcef.MCEF;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * This is the base class for the bridge between the MCEF browser and FancyMenu to make it possible to execute FancyMenu's actions in the browser via JavaScript.
 */
public class ActionBridge {

    public static final Logger LOGGER = LogManager.getLogger();
    private static final String JAVASCRIPT_NAMESPACE = "fancymenu";
    private static CefMessageRouter messageRouter;
    private static boolean initialized = false;
    
    /**
     * Initialize the global message router for all browsers
     */
    public static void initialize() {
        if (initialized) return;
        
        // Check if MCEF is loaded
        if (!MCEFUtil.isMCEFLoaded()) {
            LOGGER.warn("[FANCYMENU] Cannot initialize ActionBridge - MCEF is not loaded");
            return;
        }
        
        try {
            LOGGER.info("[FANCYMENU] Initializing ActionBridge message router");
            
            // Ensure MCEF client is initialized
            if (MCEF.getClient() == null) {
                LOGGER.warn("[FANCYMENU] MCEF client is not initialized yet, delaying ActionBridge initialization");
                return;
            }
            
            // Create message router configuration
            CefMessageRouterConfig config = new CefMessageRouterConfig();
            config.jsQueryFunction = "cefQuery";
            config.jsCancelFunction = "cefQueryCancel";
            
            // Create the message router
            messageRouter = CefMessageRouter.create(config);
            messageRouter.addHandler(createMessageHandler(), true);
            
            // Add to global MCEF client
            MCEF.getClient().getHandle().addMessageRouter(messageRouter);
            
            initialized = true;
            LOGGER.info("[FANCYMENU] ActionBridge message router initialized successfully");
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to initialize ActionBridge message router", ex);
        }
    }
    
    /**
     * Clean up the message router
     */
    public static void dispose() {
        if (messageRouter != null && initialized) {
            try {
                MCEF.getClient().getHandle().removeMessageRouter(messageRouter);
                messageRouter.dispose();
                messageRouter = null;
                initialized = false;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to dispose ActionBridge message router", ex);
            }
        }
    }
    
    /**
     * JavaScript API that will be injected into the browser
     */
    public static final String JAVASCRIPT_API = """
            (function() {
                console.log('[FancyMenu] Initializing JavaScript API...');
                console.log('[FancyMenu] typeof window.cefQuery:', typeof window.cefQuery);
                
                // Wait for cefQuery to be available
                var attempts = 0;
                function waitForCefQuery(callback) {
                    attempts++;
                    if (typeof window.cefQuery !== 'undefined') {
                        console.log('[FancyMenu] cefQuery found after ' + attempts + ' attempts');
                        callback();
                    } else if (attempts > 100) {
                        console.error('[FancyMenu] cefQuery not found after 100 attempts. Message router may not be properly configured.');
                    } else {
                        setTimeout(function() {
                            waitForCefQuery(callback);
                        }, 50);
                    }
                }
                
                waitForCefQuery(function() {
                    window.%namespace% = {
                        executeWithCallback: function(actionType, actionValue, onSuccess, onFailure) {
                            // Handle overloaded calls
                            if (typeof actionValue === 'function') {
                                // executeWithCallback(actionType, onSuccess, onFailure)
                                onFailure = onSuccess;
                                onSuccess = actionValue;
                                actionValue = null;
                            }
                            
                            if (!actionType || typeof actionType !== 'string') {
                                if (onFailure) onFailure('Invalid action type');
                                return;
                            }
                            
                            // Construct action string
                            var actionString = actionType;
                            if (actionValue !== null && actionValue !== undefined && actionValue !== '') {
                                actionString += ':' + actionValue;
                            }
                            
                            console.log('[FancyMenu] Executing action:', actionString);
                            
                            window.cefQuery({
                                request: JSON.stringify({
                                    type: 'fancymenu_action',
                                    action: actionString
                                }),
                                onSuccess: function(response) {
                                    console.log('[FancyMenu] Action response:', response);
                                    if (onSuccess) {
                                        try {
                                            var result = JSON.parse(response);
                                            onSuccess(result);
                                        } catch (e) {
                                            onSuccess(response);
                                        }
                                    }
                                },
                                onFailure: function(error_code, error_message) {
                                    console.error('[FancyMenu] Action failed:', error_code, error_message);
                                    if (onFailure) onFailure(error_message);
                                }
                            });
                        },
                        
                        // Convenience method for executing actions without callbacks
                        execute: function(actionType, actionValue) {
                            if (arguments.length === 1) {
                                // Single argument - action without value
                                this.executeWithCallback(actionType);
                            } else {
                                // Two arguments - action with value
                                this.executeWithCallback(actionType, actionValue);
                            }
                        }
                    };
                    
                    // Also create a global alias for easier access
                    window.FancyMenu = window.%namespace%;
                    
                    console.log('[FancyMenu] API initialized successfully');
                    
                    // Dispatch event to notify that the API is ready
                    window.dispatchEvent(new Event('fancymenu-ready'));
                });
            })();
            """.replace("%namespace%", JAVASCRIPT_NAMESPACE);

    /**
     * Creates a CEF message router handler for processing FancyMenu action requests from JavaScript
     */
    public static CefMessageRouterHandlerAdapter createMessageHandler() {
        return new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request, boolean persistent, CefQueryCallback callback) {
                LOGGER.info("[FANCYMENU] Received query from browser: {}", request);
                
                try {
                    // Parse the JSON request
                    if (request == null || request.isEmpty()) {
                        callback.failure(400, "Empty request");
                        return true;
                    }
                    
                    // Simple JSON parsing for the request
                    if (request.contains("\"type\":\"fancymenu_action\"") && request.contains("\"action\":")) {
                        // Extract action string from JSON
                        String actionString = extractActionFromJson(request);
                        if (actionString != null) {
                            LOGGER.info("[FANCYMENU] Processing action: {}", actionString);
                            // Process the action
                            boolean success = processAction(actionString);
                            
                            if (success) {
                                callback.success("{\"success\":true,\"message\":\"Action executed successfully\"}");
                            } else {
                                callback.failure(500, "Failed to execute action");
                            }
                        } else {
                            callback.failure(400, "Invalid action format");
                        }
                    } else {
                        // Not a FancyMenu action request, let other handlers process it
                        LOGGER.debug("[FANCYMENU] Query is not a FancyMenu action, passing to other handlers");
                        return false;
                    }
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Error processing browser action request", ex);
                    callback.failure(500, "Internal error: " + ex.getMessage());
                }
                return true;
            }
        };
    }
    
    /**
     * Simple JSON parser to extract action string
     */
    private static String extractActionFromJson(String json) {
        try {
            int actionIndex = json.indexOf("\"action\":\"");
            if (actionIndex == -1) return null;
            
            int start = actionIndex + 10; // Length of "\"action\":\""
            int end = json.indexOf("\"", start);
            if (end == -1) return null;
            
            String action = json.substring(start, end);
            // Unescape basic JSON escapes
            action = action.replace("\\\"", "\"")
                          .replace("\\\\", "\\")
                          .replace("\\/", "/")
                          .replace("\\b", "\b")
                          .replace("\\f", "\f")
                          .replace("\\n", "\n")
                          .replace("\\r", "\r")
                          .replace("\\t", "\t");
            return action;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to extract action from JSON", ex);
            return null;
        }
    }
    
    /**
     * Process an action string received from JavaScript
     */
    private static boolean processAction(@NotNull String actionString) {
        try {
            ActionInstance action = parseBrowserAction(actionString);
            if (action != null) {
                executeAction(action);
                return true;
            } else {
                LOGGER.warn("[FANCYMENU] Failed to parse browser action: {}", actionString);
                return false;
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error processing browser action: " + actionString, ex);
            return false;
        }
    }

    /**
     * This method parses browser action strings into actual {@link ActionInstance}s.<br><br>
     *
     * Browser action strings should have this format: {@code action_type:value}<br>
     * For actions without value, it's only the action type: {@code action_type}<br><br>
     *
     * The action string should be what gets used in the actual JavaScript script to send to the mod for executing it.
     */
    @Nullable
    public static ActionInstance parseBrowserAction(@NotNull String actionString) {
        try {
            String actionType;
            String value = null;
            if (actionString.contains(":")) {
                var array = actionString.split(":", 2);
                actionType = array[0];
                value = array[1];
            } else {
                actionType = actionString;
            }
            String parsableKey = "[executable_action_instance:" + ScreenCustomization.generateUniqueIdentifier() + "][action_type:" + actionType + "]";
            PropertyContainer container = new PropertyContainer("dummy_action_holder");
            container.putProperty(parsableKey, (value == null) ? "" : value);
            List<ActionInstance> deserialized = ActionInstance.deserializeAll(container);
            if (!deserialized.isEmpty()) return deserialized.get(0);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to parse browser action: " + actionString, ex);
        }
        return null;
    }

    public static void executeAction(@NotNull ActionInstance action) {
        try {
            action.execute();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute browser action!", ex);
        }
    }

}
