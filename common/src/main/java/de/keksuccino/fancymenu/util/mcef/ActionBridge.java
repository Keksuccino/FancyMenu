package de.keksuccino.fancymenu.util.mcef;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
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
    
    /**
     * JavaScript API that will be injected into the browser
     */
    public static final String JAVASCRIPT_API = """
            window.%namespace% = {
                executeAction: function(actionType, actionValue, onSuccess, onFailure) {
                    // Handle overloaded calls
                    if (typeof actionValue === 'function') {
                        // executeAction(actionType, onSuccess, onFailure)
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
                    
                    window.cefQuery({
                        request: JSON.stringify({
                            type: 'fancymenu_action',
                            action: actionString
                        }),
                        onSuccess: function(response) {
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
                            if (onFailure) onFailure(error_message);
                        }
                    });
                },
                
                // Convenience method for executing actions without callbacks
                execute: function(actionType, actionValue) {
                    if (arguments.length === 1) {
                        // Single argument - action without value
                        this.executeAction(actionType);
                    } else {
                        // Two arguments - action with value
                        this.executeAction(actionType, actionValue);
                    }
                }
            };
            
            // Also create a global alias for easier access
            window.FancyMenu = window.%namespace%;
            """.replace("%namespace%", JAVASCRIPT_NAMESPACE);

    /**
     * Creates a CEF message router handler for processing FancyMenu action requests from JavaScript
     */
    public static CefMessageRouterHandlerAdapter createMessageHandler() {
        return new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request, boolean persistent, CefQueryCallback callback) {
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
