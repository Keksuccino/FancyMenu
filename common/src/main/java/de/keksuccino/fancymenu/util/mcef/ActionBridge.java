package de.keksuccino.fancymenu.util.mcef;

import com.cinemamod.mcef.MCEF;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
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
import net.minecraft.client.Minecraft;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This is the base class for the bridge between the MCEF browser and FancyMenu to make it possible to execute FancyMenu's actions in the browser via JavaScript.
 */
public class ActionBridge {

    public static final Logger LOGGER = LogManager.getLogger();
    private static final String JAVASCRIPT_NAMESPACE = "fancymenu";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String REQUEST_TYPE_ACTION = "fancymenu_action";
    private static final String REQUEST_TYPE_PLACEHOLDER = "fancymenu_placeholder";
    private static final String PLACEHOLDER_ERROR_NOT_FOUND = "NOT_FOUND";
    private static final String PLACEHOLDER_ERROR_MISSING_VARIABLE = "MISSING_VARIABLE";
    private static final String PLACEHOLDER_ERROR_INVALID_VARIABLE = "INVALID_VARIABLE";
    private static final String PLACEHOLDER_ERROR_EVALUATION = "EVALUATION_ERROR";
    private static final long PLACEHOLDER_MAIN_THREAD_TIMEOUT_MS = 3000L;
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
                                    type: '%action_type%',
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
                        },

                        placeholders: {
                            get: function(identifier) {
                                return this.getWithVars(identifier);
                            },

                            getWithVars: function(identifier) {
                                var rawVars = Array.prototype.slice.call(arguments, 1);
                                return new Promise(function(resolve, reject) {
                                    if (!identifier || typeof identifier !== 'string') {
                                        reject({ code: 'INVALID_VARIABLE', message: 'Placeholder identifier must be a non-empty string.' });
                                        return;
                                    }

                                    var normalizedVars = [];
                                    for (var i = 0; i < rawVars.length; i++) {
                                        var entry = rawVars[i];
                                        if (typeof entry !== 'string') {
                                            reject({ code: 'INVALID_VARIABLE', message: 'Placeholder variables must be strings.' });
                                            return;
                                        }
                                        var colonIndex = entry.indexOf(':');
                                        if (colonIndex <= 0) {
                                            reject({ code: 'INVALID_VARIABLE', message: 'Placeholder variable "' + entry + '" must contain a colon separator.' });
                                            return;
                                        }
                                        normalizedVars.push(entry);
                                    }

                                    window.cefQuery({
                                        request: JSON.stringify({
                                            type: '%placeholder_type%',
                                            identifier: identifier,
                                            vars: normalizedVars
                                        }),
                                        onSuccess: function(response) {
                                            try {
                                                var payload = JSON.parse(response);
                                                if (payload && typeof payload.value !== 'undefined') {
                                                    resolve(payload.value);
                                                    return;
                                                }
                                            } catch (e) {
                                                // Fallback below
                                            }
                                            resolve(response);
                                        },
                                        onFailure: function(error_code, error_message) {
                                            var errorPayload = {
                                                code: 'INTERNAL_ERROR',
                                                message: error_message || 'Unknown error',
                                                details: { status: error_code }
                                            };

                                            if (error_message) {
                                                try {
                                                    var parsedError = JSON.parse(error_message);
                                                    if (parsedError && parsedError.code) {
                                                        errorPayload = parsedError;
                                                        if (parsedError.details && typeof parsedError.details === 'object') {
                                                            parsedError.details.status = error_code;
                                                        } else {
                                                            errorPayload.details = { status: error_code };
                                                        }
                                                    }
                                                } catch (e) {
                                                    errorPayload.message = error_message;
                                                }
                                            }

                                            console.error('[FancyMenu] Placeholder request failed:', errorPayload);
                                            reject(errorPayload);
                                        }
                                    });
                                });
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
            """.replace("%namespace%", JAVASCRIPT_NAMESPACE)
            .replace("%placeholder_type%", REQUEST_TYPE_PLACEHOLDER)
            .replace("%action_type%", REQUEST_TYPE_ACTION);

    /**
     * Creates a CEF message router handler for processing FancyMenu action requests from JavaScript
     */
    public static CefMessageRouterHandlerAdapter createMessageHandler() {
        return new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long query_id, String request, boolean persistent, CefQueryCallback callback) {
                LOGGER.info("[FANCYMENU] Received query from browser: {}", request);

                if ((request == null) || request.isEmpty()) {
                    callback.failure(400, "Empty request");
                    return true;
                }

                final JsonObject payload;
                try {
                    JsonElement parsed = JsonParser.parseString(request);
                    if (!parsed.isJsonObject()) {
                        callback.failure(400, "Invalid request payload");
                        return true;
                    }
                    payload = parsed.getAsJsonObject();
                } catch (JsonParseException ex) {
                    LOGGER.warn("[FANCYMENU] Failed to parse browser request JSON", ex);
                    callback.failure(400, "Invalid JSON payload");
                    return true;
                }

                String type = getStringOrNull(payload, "type");
                if ((type == null) || type.isEmpty()) {
                    callback.failure(400, "Missing request type");
                    return true;
                }

                try {
                    return switch (type) {
                        case REQUEST_TYPE_ACTION -> handleActionRequest(payload, callback);
                        case REQUEST_TYPE_PLACEHOLDER -> handlePlaceholderRequest(payload, callback);
                        default -> {
                            LOGGER.debug("[FANCYMENU] Query type '{}' not handled by FancyMenu bridge", type);
                            yield false;
                        }
                    };
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Error processing browser request", ex);
                    callback.failure(500, "Internal error: " + ex.getMessage());
                    return true;
                }
            }
        };
    }

    private static boolean handleActionRequest(@NotNull JsonObject payload, @NotNull CefQueryCallback callback) {
        String actionString = getStringOrNull(payload, "action");
        if ((actionString == null) || actionString.isEmpty()) {
            callback.failure(400, "Invalid action format");
            return true;
        }

        LOGGER.info("[FANCYMENU] Processing action: {}", actionString);
        boolean success = processAction(actionString);
        if (success) {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Action executed successfully");
            callback.success(GSON.toJson(response));
        } else {
            callback.failure(500, "Failed to execute action");
        }
        return true;
    }

    private static boolean handlePlaceholderRequest(@NotNull JsonObject payload, @NotNull CefQueryCallback callback) {
        String identifier = getStringOrNull(payload, "identifier");
        if (identifier == null) {
            sendPlaceholderFailure(callback, 400, PLACEHOLDER_ERROR_INVALID_VARIABLE, "Placeholder identifier is required");
            return true;
        }
        identifier = identifier.trim();
        if (identifier.isEmpty()) {
            sendPlaceholderFailure(callback, 400, PLACEHOLDER_ERROR_INVALID_VARIABLE, "Placeholder identifier is required");
            return true;
        }

        Placeholder placeholder = PlaceholderRegistry.getPlaceholder(identifier);
        if ((placeholder == null) && !identifier.equals(identifier.toLowerCase(Locale.ROOT))) {
            placeholder = PlaceholderRegistry.getPlaceholder(identifier.toLowerCase(Locale.ROOT));
        }

        if (placeholder == null) {
            sendPlaceholderFailure(callback, 404, PLACEHOLDER_ERROR_NOT_FOUND, "Unknown placeholder: " + identifier);
            return true;
        }

        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        JsonElement varsElement = payload.get("vars");
        if ((varsElement != null) && !varsElement.isJsonNull()) {
            if (!varsElement.isJsonArray()) {
                sendPlaceholderFailure(callback, 400, PLACEHOLDER_ERROR_INVALID_VARIABLE, "Placeholder variables must be provided as an array");
                return true;
            }

            JsonArray varsArray = varsElement.getAsJsonArray();
            for (JsonElement element : varsArray) {
                if (!element.isJsonPrimitive()) {
                    sendPlaceholderFailure(callback, 400, PLACEHOLDER_ERROR_INVALID_VARIABLE, "Placeholder variables must be strings");
                    return true;
                }
                String raw = element.getAsString();
                int colonIndex = raw.indexOf(':');
                if (colonIndex <= 0) {
                    sendPlaceholderFailure(callback, 400, PLACEHOLDER_ERROR_INVALID_VARIABLE, "Invalid placeholder variable format: " + raw);
                    return true;
                }
                String name = raw.substring(0, colonIndex).trim();
                String value = raw.substring(colonIndex + 1).trim();
                if (name.isEmpty()) {
                    sendPlaceholderFailure(callback, 400, PLACEHOLDER_ERROR_INVALID_VARIABLE, "Placeholder variable name is empty");
                    return true;
                }
                values.put(name, value);
            }
        }

        List<String> requiredNames = placeholder.getValueNames();
        if ((requiredNames != null) && !requiredNames.isEmpty()) {
            for (String required : requiredNames) {
                if (!values.containsKey(required)) {
                    sendPlaceholderFailure(callback, 400, PLACEHOLDER_ERROR_MISSING_VARIABLE, "Missing placeholder variable: " + required);
                    return true;
                }
            }
        }

        LinkedHashMap<String, String> valuesCopy = new LinkedHashMap<>(values);
        DeserializedPlaceholderString deserialized = new DeserializedPlaceholderString(placeholder.getIdentifier(), valuesCopy, "");
        deserialized.placeholderString = deserialized.toString();

        try {
            String replacement = evaluatePlaceholderValue(placeholder, deserialized);
            if (replacement == null) replacement = "";
            sendPlaceholderSuccess(callback, placeholder.getIdentifier(), replacement);
        } catch (TimeoutException ex) {
            LOGGER.error("[FANCYMENU] Placeholder '{}' evaluation timed out", placeholder.getIdentifier(), ex);
            sendPlaceholderFailure(callback, 504, PLACEHOLDER_ERROR_EVALUATION, "Placeholder evaluation timed out", ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.error("[FANCYMENU] Placeholder '{}' evaluation interrupted", placeholder.getIdentifier(), ex);
            sendPlaceholderFailure(callback, 500, PLACEHOLDER_ERROR_EVALUATION, "Placeholder evaluation interrupted", ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to evaluate placeholder '{}'", placeholder.getIdentifier(), ex);
            sendPlaceholderFailure(callback, 500, PLACEHOLDER_ERROR_EVALUATION, "Placeholder evaluation failed", ex.getMessage());
        }
        return true;
    }

    @Nullable
    private static String getStringOrNull(@NotNull JsonObject payload, @NotNull String key) {
        JsonElement element = payload.get(key);
        if ((element == null) || element.isJsonNull()) return null;
        if (!element.isJsonPrimitive()) return null;
        return element.getAsString();
    }

    private static void sendPlaceholderSuccess(@NotNull CefQueryCallback callback, @NotNull String identifier, @NotNull String value) {
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("identifier", identifier);
        result.addProperty("value", value);
        callback.success(GSON.toJson(result));
    }

    private static void sendPlaceholderFailure(@NotNull CefQueryCallback callback, int statusCode, @NotNull String code, @NotNull String message) {
        sendPlaceholderFailure(callback, statusCode, code, message, null);
    }

    private static void sendPlaceholderFailure(@NotNull CefQueryCallback callback, int statusCode, @NotNull String code, @NotNull String message, @Nullable String details) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        if ((details != null) && !details.isEmpty()) {
            error.addProperty("details", details);
        }
        callback.failure(statusCode, GSON.toJson(error));
    }

    @NotNull
    private static String evaluatePlaceholderValue(@NotNull Placeholder placeholder, @NotNull DeserializedPlaceholderString deserialized) throws Exception {
        boolean sameThread = Minecraft.getInstance().isSameThread();
        if (placeholder.canRunAsync() || sameThread) {
            if (!placeholder.checkAsync()) {
                throw new IllegalStateException("Placeholder cannot be evaluated in the current thread");
            }
            return placeholder.getReplacementFor(deserialized);
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        MainThreadTaskExecutor.executeInMainThread(() -> {
            try {
                if (!placeholder.checkAsync()) {
                    future.completeExceptionally(new IllegalStateException("Placeholder cannot be evaluated in the current thread"));
                    return;
                }
                future.complete(placeholder.getReplacementFor(deserialized));
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);

        try {
            return future.get(PLACEHOLDER_MAIN_THREAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw ex;
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw ex;
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
