package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedGenericValueCycle;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.gui.VanillaTooltip;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SendHttpRequestAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String VALUE_SEPARATOR = "|||";
    private static final String HEADER_SEPARATOR = "~~~";
    private static final String HEADER_KEY_VALUE_SEPARATOR = ":::";

    public SendHttpRequestAction() {
        super("send_http_request");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            LOGGER.error("[FANCYMENU] SendHttpRequestAction: No value provided!");
            return;
        }

        HttpRequestConfig config = HttpRequestConfig.parse(value);
        if (config == null) {
            LOGGER.error("[FANCYMENU] SendHttpRequestAction: Failed to parse configuration!");
            return;
        }

        // Check internet availability first
        if (!WebUtils.isInternetAvailable()) {
            LOGGER.warn("[FANCYMENU] SendHttpRequestAction: No internet connection available!");
            return;
        }

        // Execute request asynchronously to avoid blocking
        CompletableFuture.runAsync(() -> {
            try {
                sendHttpRequest(config);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] SendHttpRequestAction: Failed to send HTTP request!", ex);
            }
        });
    }

    private void sendHttpRequest(HttpRequestConfig config) throws Exception {
        URI uri = new URI(config.url);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            // Set request method
            connection.setRequestMethod(config.method);
            
            // Set timeout
            connection.setConnectTimeout(config.timeout * 1000);
            connection.setReadTimeout(config.timeout * 1000);

            // Set headers
            for (HttpHeader header : config.headers) {
                connection.setRequestProperty(header.key, header.value);
            }

            // Set authentication
            if (config.authType != AuthType.NONE) {
                switch (config.authType) {
                    case BASIC:
                        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((config.authUsername + ":" + config.authPassword).getBytes(StandardCharsets.UTF_8));
                        connection.setRequestProperty("Authorization", basicAuth);
                        break;
                    case BEARER:
                        connection.setRequestProperty("Authorization", "Bearer " + config.authToken);
                        break;
                    case API_KEY:
                        connection.setRequestProperty(config.authApiKeyHeader, config.authApiKey);
                        break;
                }
            }

            // Set content type if not already set
            if (config.contentType != null && !config.contentType.isEmpty()) {
                connection.setRequestProperty("Content-Type", config.contentType);
            }

            // Send body if applicable
            if (!config.method.equals("GET") && !config.method.equals("HEAD") && config.body != null && !config.body.isEmpty()) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = config.body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // Get response
            int responseCode = connection.getResponseCode();
            
            // Always capture the response if we need to log it or store it in a variable
            if (config.logResponse || !config.responseVariable.isEmpty()) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                }
                
                final String responseText = response.toString().trim();
                
                // Set variable if specified
                if (!config.responseVariable.isEmpty()) {
                    final String variableValue;
                    if (config.singleLineResponse) {
                        // Convert to single line by removing all line breaks
                        variableValue = responseText.replace("\n", " ").replace("\r", " ").trim();
                    } else {
                        // Keep multi-line format
                        variableValue = responseText;
                    }
                    
                    MainThreadTaskExecutor.executeInMainThread(() -> {
                        VariableHandler.setVariable(config.responseVariable, variableValue);
                    }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
                
                // Log if requested (keep multi-line for readability)
                if (config.logResponse) {
                    LOGGER.info("[FANCYMENU] HTTP Request completed - Code: {}, Response: {}", responseCode, responseText);
                }
            } else {
                LOGGER.info("[FANCYMENU] HTTP Request completed - Code: {}", responseCode);
            }

        } finally {
            connection.disconnect();
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.send_http_request");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.send_http_request.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public String getValueExample() {
        return "https://api.example.com|||POST|||{\"key\":\"value\"}|||application/json|||10|||true||||||true|||NONE|||||||||||||";
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {
        SendHttpRequestActionValueScreen s = new SendHttpRequestActionValueScreen(Objects.requireNonNullElse(instance.value, this.getValueExample()), value -> {
            if (value != null) {
                instance.value = value;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
    }

    public enum AuthType {
        NONE("fancymenu.actions.send_http_request.auth.none"),
        BASIC("fancymenu.actions.send_http_request.auth.basic"),
        BEARER("fancymenu.actions.send_http_request.auth.bearer"),
        API_KEY("fancymenu.actions.send_http_request.auth.api_key");

        private final String localizationKey;

        AuthType(String localizationKey) {
            this.localizationKey = localizationKey;
        }

        public String getLocalizationKey() {
            return localizationKey;
        }
    }

    public static class HttpHeader {
        public String key;
        public String value;

        public HttpHeader(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class HttpRequestConfig {
        public String url = "";
        public String method = "GET";
        public String body = "";
        public String contentType = "application/json";
        public int timeout = 10;
        public boolean logResponse = false;
        public String responseVariable = "";
        public boolean singleLineResponse = true;
        public AuthType authType = AuthType.NONE;
        public String authUsername = "";
        public String authPassword = "";
        public String authToken = "";
        public String authApiKeyHeader = "X-API-Key";
        public String authApiKey = "";
        public List<HttpHeader> headers = new ArrayList<>();

        public String serialize() {
            StringBuilder sb = new StringBuilder();
            
            // Basic fields
            sb.append(url).append(VALUE_SEPARATOR);
            sb.append(method).append(VALUE_SEPARATOR);
            sb.append(body).append(VALUE_SEPARATOR);
            sb.append(contentType).append(VALUE_SEPARATOR);
            sb.append(timeout).append(VALUE_SEPARATOR);
            sb.append(logResponse).append(VALUE_SEPARATOR);
            sb.append(responseVariable).append(VALUE_SEPARATOR);
            sb.append(singleLineResponse).append(VALUE_SEPARATOR);
            sb.append(authType.name()).append(VALUE_SEPARATOR);
            sb.append(authUsername).append(VALUE_SEPARATOR);
            sb.append(authPassword).append(VALUE_SEPARATOR);
            sb.append(authToken).append(VALUE_SEPARATOR);
            sb.append(authApiKeyHeader).append(VALUE_SEPARATOR);
            sb.append(authApiKey).append(VALUE_SEPARATOR);
            
            // Headers
            for (HttpHeader header : headers) {
                sb.append(header.key).append(HEADER_KEY_VALUE_SEPARATOR).append(header.value).append(HEADER_SEPARATOR);
            }
            
            return sb.toString();
        }

        @Nullable
        public static HttpRequestConfig parse(String value) {
            if (value == null || value.isEmpty()) return null;
            
            HttpRequestConfig config = new HttpRequestConfig();
            String[] parts = value.split("\\|\\|\\|", -1);
            
            try {
                if (parts.length >= 1) config.url = parts[0];
                if (parts.length >= 2) config.method = parts[1];
                if (parts.length >= 3) config.body = parts[2];
                if (parts.length >= 4) config.contentType = parts[3];
                if (parts.length >= 5) config.timeout = Integer.parseInt(parts[4]);
                if (parts.length >= 6) config.logResponse = Boolean.parseBoolean(parts[5]);
                if (parts.length >= 7) config.responseVariable = parts[6];
                if (parts.length >= 8) config.singleLineResponse = Boolean.parseBoolean(parts[7]);
                if (parts.length >= 9) config.authType = AuthType.valueOf(parts[8]);
                if (parts.length >= 10) config.authUsername = parts[9];
                if (parts.length >= 11) config.authPassword = parts[10];
                if (parts.length >= 12) config.authToken = parts[11];
                if (parts.length >= 13) config.authApiKeyHeader = parts[12];
                if (parts.length >= 14) config.authApiKey = parts[13];
                
                // Parse headers
                if (parts.length >= 15 && !parts[14].isEmpty()) {
                    String[] headerPairs = parts[14].split(HEADER_SEPARATOR);
                    for (String headerPair : headerPairs) {
                        if (headerPair.contains(HEADER_KEY_VALUE_SEPARATOR)) {
                            String[] kv = headerPair.split(HEADER_KEY_VALUE_SEPARATOR, 2);
                            config.headers.add(new HttpHeader(kv[0], kv[1]));
                        }
                    }
                }
                
                return config;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to parse HTTP request configuration!", ex);
                return null;
            }
        }
    }

    public static class SendHttpRequestActionValueScreen extends CellScreen {

        protected HttpRequestConfig config;
        protected Consumer<String> callback;
        protected List<HeaderEditRow> headerRows = new ArrayList<>();
        protected boolean authFieldsInitialized = false;
        protected boolean firstInit = true;

        protected SendHttpRequestActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.send_http_request.edit_value"));
            this.callback = callback;
            this.config = HttpRequestConfig.parse(value);
            if (this.config == null) {
                this.config = new HttpRequestConfig();
            }
        }

        @Override
        protected void initCells() {
            // Store existing header data before clearing
            List<HttpHeader> existingHeaders = new ArrayList<>();
            for (HeaderEditRow row : this.headerRows) {
                existingHeaders.add(new HttpHeader(row.key, row.value));
            }
            this.headerRows.clear();
            
            this.addStartEndSpacerCell();

            // URL
            this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.url"));
            this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.url = s)
                    .setText(this.config.url);

            this.addCellGroupEndSpacerCell();

            // Method
            this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.method.title"));
            List<HttpMethod> methods = Arrays.asList(HttpMethod.values());
            LocalizedGenericValueCycle<HttpMethod> methodCycle = LocalizedGenericValueCycle.of("fancymenu.actions.send_http_request.edit.method", methods.toArray(new HttpMethod[0]));
            methodCycle.setCurrentValue(HttpMethod.valueOf(this.config.method));
            methodCycle.setValueComponentStyleSupplier(consumes -> Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()));
            this.addCycleButtonCell(methodCycle, true, (value, button) -> this.config.method = value.name());

            this.addCellGroupEndSpacerCell();

            // Content Type
            this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.content_type"));
            this.addTextInputCell(null, false, false)
                    .setEditListener(s -> this.config.contentType = s)
                    .setText(this.config.contentType);

            this.addCellGroupEndSpacerCell();

            // Body
            this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.body"));
            TextInputCell bodyCell = this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.body = s)
                    .setText(this.config.body);
            bodyCell.setEditorCallback((s, cell) -> {
                this.config.body = s;
                cell.editBox.setValue(s);
            });

            this.addCellGroupEndSpacerCell();

            // Timeout
            this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.timeout"));
            this.addTextInputCell(CharacterFilter.buildIntegerFiler(), false, false)
                    .setEditListener(s -> {
                        try {
                            this.config.timeout = Integer.parseInt(s);
                        } catch (Exception ignore) {}
                    })
                    .setText(String.valueOf(this.config.timeout));

            this.addCellGroupEndSpacerCell();

            // Log Response
            this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.actions.send_http_request.edit.log_response", this.config.logResponse), (value, button) -> this.config.logResponse = value.getAsBoolean()), true);

            this.addCellGroupEndSpacerCell();

            // Response Variable
            this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.response_variable"));
            this.addTextInputCell(CharacterFilter.buildResourceNameFilter(), false, false)
                    .setEditListener(s -> this.config.responseVariable = s)
                    .setText(this.config.responseVariable)
                    .editBox.setTooltip(() -> Tooltip.of(Component.translatable("fancymenu.actions.send_http_request.edit.response_variable.desc")));

            this.addCellGroupEndSpacerCell();

            // Single Line Response
            this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.actions.send_http_request.edit.single_line_response", this.config.singleLineResponse), (value, button) -> this.config.singleLineResponse = value.getAsBoolean()), true);

            this.addCellGroupEndSpacerCell();

            // Authentication
            this.addSeparatorCell();
            this.addCellGroupEndSpacerCell();
            
            this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.auth_type.title"));
            
            // Add auth type cycle with localized display
            List<AuthType> authTypes = Arrays.asList(AuthType.values());
            LocalizedGenericValueCycle<AuthType> authCycle = LocalizedGenericValueCycle.of("fancymenu.actions.send_http_request.edit.auth_type", authTypes.toArray(new AuthType[0]));
            // Set a custom value name supplier that returns the localized name
            authCycle.setValueNameSupplier(authType -> Component.translatable(authType.getLocalizationKey()).getString());
            authCycle.setValueComponentStyleSupplier(consumes -> Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()));
            authCycle.setCurrentValue(this.config.authType);
            
            this.addCycleButtonCell(authCycle, true, (value, button) -> {
                this.config.authType = value;
                this.rebuildAuthFields();
            });

            this.addCellGroupEndSpacerCell();

            // Add auth fields based on current auth type
            this.addAuthFields();

            this.addCellGroupEndSpacerCell();

            // Headers
            this.addSeparatorCell();
            this.addCellGroupEndSpacerCell();
            
            this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.headers"));
            
            // Add existing headers
            if (!existingHeaders.isEmpty()) {
                for (HttpHeader header : existingHeaders) {
                    this.addHeaderRow(header.key, header.value);
                }
            } else if (this.firstInit) {
                for (HttpHeader header : this.config.headers) {
                    this.addHeaderRow(header.key, header.value);
                }
            }
            
            // Add spacing before button
            this.addCellGroupEndSpacerCell();
            
            // Add header button
            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.actions.send_http_request.edit.add_header"), button -> {
                this.addHeaderRow("", "");
                this.rebuild();
            }), true);

            this.addStartEndSpacerCell();
            
            this.authFieldsInitialized = true;

            this.firstInit = false;

        }

        private void addAuthFields() {
            if (this.config.authType == AuthType.BASIC) {
                this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.auth_username"));
                this.addTextInputCell(null, false, false)
                        .setEditListener(s -> this.config.authUsername = s)
                        .setText(this.config.authUsername);
                
                this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.auth_password"));
                this.addTextInputCell(null, false, false)
                        .setEditListener(s -> this.config.authPassword = s)
                        .setText(this.config.authPassword);
            } else if (this.config.authType == AuthType.BEARER) {
                this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.auth_token"));
                this.addTextInputCell(null, false, false)
                        .setEditListener(s -> this.config.authToken = s)
                        .setText(this.config.authToken);
            } else if (this.config.authType == AuthType.API_KEY) {
                this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.auth_api_key_header"));
                this.addTextInputCell(null, false, false)
                        .setEditListener(s -> this.config.authApiKeyHeader = s)
                        .setText(this.config.authApiKeyHeader);
                
                this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.auth_api_key"));
                this.addTextInputCell(null, false, false)
                        .setEditListener(s -> this.config.authApiKey = s)
                        .setText(this.config.authApiKey);
            }
        }

        private void rebuildAuthFields() {
            if (this.authFieldsInitialized) {
                this.rebuild();
            }
        }

        private void addHeaderRow(String key, String value) {
            HeaderEditRow row = new HeaderEditRow(key, value);
            this.headerRows.add(row);
            
            // Add a separator for visual clarity
            if (this.headerRows.size() > 1) {
                this.addSpacerCell(3);
            }
            
            // Header section label
            this.addLabelCell(Component.literal("> Header #" + this.headerRows.size()).withStyle(s -> s.withBold(true)));
            
            // Key input
            this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.header_key"));
            this.addTextInputCell(null, true, true)
                    .setEditListener(s -> row.key = s)
                    .setText(key);
            row.keyCell = (CellScrollEntry) this.scrollArea.getEntries().get(this.scrollArea.getEntries().size() - 1);
            
            // Value input
            this.addLabelCell(Component.translatable("fancymenu.actions.send_http_request.edit.header_value"));
            this.addTextInputCell(null, true, true)
                    .setEditListener(s -> row.value = s)
                    .setText(value);
            row.valueCell = (CellScrollEntry) this.scrollArea.getEntries().get(this.scrollArea.getEntries().size() - 1);
            
            // Remove button
            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.actions.send_http_request.edit.remove_header"), button -> {
                this.headerRows.remove(row);
                this.rebuild();
            }), true);
            row.removeButtonCell = (CellScrollEntry) this.scrollArea.getEntries().get(this.scrollArea.getEntries().size() - 1);
        }

        @Override
        public boolean allowDone() {
            return !this.config.url.isEmpty();
        }

        @Override
        protected void onCancel() {
            this.callback.accept(null);
        }

        @Override
        protected void onDone() {
            // Update headers from UI
            this.config.headers.clear();
            for (HeaderEditRow row : this.headerRows) {
                if (!row.key.isEmpty() && !row.value.isEmpty()) {
                    this.config.headers.add(new HttpHeader(row.key, row.value));
                }
            }
            
            this.callback.accept(this.config.serialize());
        }

        protected static class HeaderEditRow {
            String key;
            String value;
            CellScrollEntry keyCell;
            CellScrollEntry valueCell;
            CellScrollEntry removeButtonCell;

            HeaderEditRow(String key, String value) {
                this.key = key;
                this.value = value;
            }
        }
    }
}
