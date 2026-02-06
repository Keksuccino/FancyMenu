package de.keksuccino.fancymenu.util.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.FancyMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

final class FancyMenuMcpServer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String JSON_RPC_VERSION = "2.0";
    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final int CLIENT_READ_TIMEOUT_MS = 5 * 60 * 1000;
    private static final int MAX_REQUEST_BYTES = 8 * 1024 * 1024;

    private final int configuredPort;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<ClientConnection> clients = new CopyOnWriteArrayList<>();
    private final Map<String, FancyMenuMcpSession> httpSessions = new ConcurrentHashMap<>();

    private volatile @Nullable ServerSocket serverSocket;
    private volatile @Nullable Thread acceptThread;
    private volatile int boundPort = -1;

    FancyMenuMcpServer(int configuredPort) {
        this.configuredPort = configuredPort;
    }

    synchronized void start() throws IOException {
        if (running.get()) {
            return;
        }
        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        // Bind on all interfaces so WSL-based MCP clients can connect via Windows host IP.
        socket.bind(new InetSocketAddress(configuredPort));
        this.serverSocket = socket;
        this.boundPort = socket.getLocalPort();
        running.set(true);

        Thread thread = new Thread(this::acceptLoop, "FancyMenu-MCP-Acceptor");
        thread.setDaemon(true);
        thread.start();
        this.acceptThread = thread;
    }

    synchronized void stop() {
        if (!running.getAndSet(false)) {
            return;
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        serverSocket = null;
        boundPort = -1;
        if (acceptThread != null) {
            acceptThread.interrupt();
            acceptThread = null;
        }
        for (ClientConnection client : new ArrayList<>(clients)) {
            client.close();
        }
        clients.clear();
        httpSessions.clear();
    }

    boolean isRunning() {
        return running.get();
    }

    int getBoundPort() {
        return boundPort;
    }

    private void acceptLoop() {
        LOGGER.info("[FANCYMENU MCP] Server started on 0.0.0.0:{}.", boundPort);
        while (running.get()) {
            try {
                ServerSocket socket = this.serverSocket;
                if (socket == null) {
                    break;
                }
                Socket clientSocket = socket.accept();
                clientSocket.setTcpNoDelay(true);
                clientSocket.setSoTimeout(CLIENT_READ_TIMEOUT_MS);
                ClientConnection client = new ClientConnection(clientSocket);
                clients.add(client);
                client.start();
            } catch (Exception ex) {
                if (running.get()) {
                    LOGGER.error("[FANCYMENU MCP] Error while accepting client connection.", ex);
                }
                break;
            }
        }
        LOGGER.info("[FANCYMENU MCP] Server accept loop ended.");
    }

    private final class ClientConnection implements Runnable {

        private final Socket socket;
        private final FancyMenuMcpSession session;
        private final AtomicBoolean connected = new AtomicBoolean(true);
        private @Nullable Thread thread;

        private ClientConnection(@NotNull Socket socket) {
            this.socket = socket;
            this.session = new FancyMenuMcpSession(socket.getRemoteSocketAddress());
        }

        private void start() {
            Thread t = new Thread(this, "FancyMenu-MCP-Client-" + this.session.getRemoteAddress());
            t.setDaemon(true);
            t.start();
            this.thread = t;
        }

        @Override
        public void run() {
            try (BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                 BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {
                while (running.get() && connected.get()) {
                    IncomingMessage incoming;
                    try {
                        incoming = readIncomingMessage(in);
                    } catch (SocketTimeoutException ignored) {
                        continue;
                    }
                    if (incoming == null) {
                        break;
                    }
                    if (incoming.transport() == IncomingTransport.HTTP) {
                        HttpResponse response = handleHttpMessage(incoming);
                        writeHttpResponse(out, response);
                        continue;
                    }

                    JsonObject response = handleMessage(session, incoming.payload());
                    if (response != null && incoming.payload() != null) {
                        writeFramedMessage(out, response.toString());
                    }
                }
            } catch (Exception ex) {
                if (connected.get()) {
                    LOGGER.error("[FANCYMENU MCP] Client connection error: {}", session.getRemoteAddress(), ex);
                }
            } finally {
                close();
            }
        }

        private @NotNull HttpResponse handleHttpMessage(@NotNull IncomingMessage incoming) {
            String method = incoming.httpMethod();
            if (method == null) {
                return HttpResponse.empty(400, "Bad Request", null);
            }

            if ("GET".equals(method)) {
                return HttpResponse.empty(405, "Method Not Allowed", null);
            }

            if ("DELETE".equals(method)) {
                String sessionId = incoming.headers().get("mcp-session-id");
                if (sessionId != null && !sessionId.isBlank()) {
                    httpSessions.remove(sessionId.trim());
                }
                return HttpResponse.empty(204, "No Content", null);
            }

            if (!"POST".equals(method)) {
                return HttpResponse.empty(405, "Method Not Allowed", null);
            }

            String rawPayload = incoming.payload();
            if (rawPayload == null || rawPayload.isBlank()) {
                JsonObject error = error(null, -32600, "Invalid Request: empty body.");
                return HttpResponse.json(400, "Bad Request", error, null);
            }

            JsonElement parsed;
            try {
                parsed = JsonParser.parseString(rawPayload);
            } catch (Exception ex) {
                JsonObject error = error(null, -32700, "Parse error: invalid JSON.");
                return HttpResponse.json(400, "Bad Request", error, null);
            }
            if (!parsed.isJsonObject()) {
                JsonObject error = error(null, -32600, "Invalid Request: expected JSON object.");
                return HttpResponse.json(400, "Bad Request", error, null);
            }

            JsonObject request = parsed.getAsJsonObject();
            String requestMethod = getString(request, "method", null);
            String requestedSessionId = incoming.headers().get("mcp-session-id");
            FancyMenuMcpSession requestSession = resolveHttpSession(requestedSessionId, requestMethod);
            JsonObject response = handleParsedMessage(requestSession, request);

            String responseSessionId = null;
            if ("initialize".equals(requestMethod) && response != null && response.has("result")) {
                responseSessionId = (requestedSessionId != null && !requestedSessionId.isBlank())
                        ? requestedSessionId
                        : UUID.randomUUID().toString();
                httpSessions.put(responseSessionId, requestSession);
            } else if (requestedSessionId != null && httpSessions.containsKey(requestedSessionId)) {
                responseSessionId = requestedSessionId;
            }

            if (response == null) {
                return HttpResponse.empty(202, "Accepted", responseSessionId);
            }
            return HttpResponse.json(200, "OK", response, responseSessionId);
        }

        private @NotNull FancyMenuMcpSession resolveHttpSession(@Nullable String requestedSessionId, @Nullable String requestMethod) {
            if (requestedSessionId != null) {
                FancyMenuMcpSession known = httpSessions.get(requestedSessionId);
                if (known != null) {
                    return known;
                }
                if ("initialize".equals(requestMethod)) {
                    return new FancyMenuMcpSession(socket.getRemoteSocketAddress());
                }
            }
            return session;
        }

        private @Nullable JsonObject handleMessage(@NotNull FancyMenuMcpSession activeSession, @Nullable String rawMessage) {
            if (rawMessage == null) {
                return error(null, -32600, "Invalid Request: empty payload.");
            }
            JsonElement parsed;
            try {
                parsed = JsonParser.parseString(rawMessage);
            } catch (Exception ex) {
                return error(null, -32700, "Parse error: invalid JSON.");
            }
            if (!parsed.isJsonObject()) {
                return error(null, -32600, "Invalid Request: expected JSON object.");
            }
            return handleParsedMessage(activeSession, parsed.getAsJsonObject());
        }

        private @Nullable JsonObject handleParsedMessage(@NotNull FancyMenuMcpSession activeSession, @NotNull JsonObject request) {
            JsonElement id = request.get("id");
            boolean respond = id != null && !id.isJsonNull();
            String method = getString(request, "method", null);
            if (method == null || method.isBlank()) {
                return respond ? error(id, -32600, "Invalid Request: missing method.") : null;
            }

            try {
                JsonObject response = switch (method) {
                    case "initialize" -> {
                        JsonObject initializeParams = request.has("params") && request.get("params").isJsonObject()
                                ? request.getAsJsonObject("params")
                                : null;
                        activeSession.setInitializeCalled(true);
                        activeSession.setInitialized(false);
                        activeSession.setIntroCalled(false);
                        yield success(id, initializeResult(initializeParams));
                    }
                    case "notifications/initialized" -> {
                        ensureInitializeCalled(activeSession);
                        activeSession.setInitialized(true);
                        yield null;
                    }
                    case "ping" -> success(id, new JsonObject());
                    case "tools/list" -> {
                        yield success(id, toolsListResult());
                    }
                    case "tools/call" -> {
                        ensureInitialized(activeSession);
                        yield handleToolCall(activeSession, id, request.getAsJsonObject("params"));
                    }
                    default -> error(id, -32601, "Method not found: " + method);
                };
                if (!respond) {
                    return null;
                }
                return response;
            } catch (IllegalArgumentException ex) {
                return respond ? error(id, -32602, ex.getMessage()) : null;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU MCP] Error while handling request.", ex);
                return respond ? error(id, -32000, "Server error: " + ex.getMessage()) : null;
            }
        }

        private @Nullable JsonObject handleToolCall(@NotNull FancyMenuMcpSession activeSession, @Nullable JsonElement id, @Nullable JsonObject params) throws Exception {
            if (params == null) {
                throw new IllegalArgumentException("Missing tools/call params.");
            }
            String toolName = getString(params, "name", null);
            if (toolName == null || toolName.isBlank()) {
                throw new IllegalArgumentException("Missing tool name.");
            }
            if (!FancyMenuMcpTools.toolExists(toolName)) {
                throw new IllegalArgumentException("Unknown tool: " + toolName);
            }
            if (!activeSession.isIntroCalled() && !"fancymenu_intro".equals(toolName)) {
                throw new IllegalArgumentException("You must call 'fancymenu_intro' first in this session before using other tools.");
            }

            JsonObject toolArguments = params.has("arguments") && params.get("arguments").isJsonObject()
                    ? params.getAsJsonObject("arguments")
                    : new JsonObject();

            FancyMenuMcpTools.ToolExecution execution = FancyMenuMcpTools.executeTool(toolName, toolArguments);
            if ("fancymenu_intro".equals(execution.getToolName())) {
                activeSession.setIntroCalled(true);
            }

            JsonObject toolResult = new JsonObject();
            JsonArray content = new JsonArray();
            JsonObject text = new JsonObject();
            text.addProperty("type", "text");
            text.addProperty("text", GSON.toJson(execution.getStructuredContent()));
            content.add(text);

            if (execution.getImageBase64() != null && execution.getImageMimeType() != null) {
                JsonObject image = new JsonObject();
                image.addProperty("type", "image");
                image.addProperty("mimeType", execution.getImageMimeType());
                image.addProperty("data", execution.getImageBase64());
                content.add(image);
            }

            toolResult.add("content", content);
            toolResult.add("structuredContent", execution.getStructuredContent());
            toolResult.addProperty("isError", false);

            return success(id, toolResult);
        }

        private @NotNull JsonObject initializeResult(@Nullable JsonObject initializeParams) {
            JsonObject result = new JsonObject();
            result.addProperty("protocolVersion", negotiateProtocolVersion(initializeParams));
            JsonObject capabilities = new JsonObject();
            capabilities.add("tools", new JsonObject());
            result.add("capabilities", capabilities);
            JsonObject serverInfo = new JsonObject();
            serverInfo.addProperty("name", "FancyMenu MCP Server");
            serverInfo.addProperty("version", FancyMenu.VERSION);
            result.add("serverInfo", serverInfo);
            return result;
        }

        private @NotNull JsonObject toolsListResult() {
            JsonObject result = new JsonObject();
            result.add("tools", FancyMenuMcpTools.listToolDefinitions());
            return result;
        }

        private void ensureInitializeCalled(@NotNull FancyMenuMcpSession activeSession) {
            if (!activeSession.isInitializeCalled()) {
                throw new IllegalArgumentException("Call 'initialize' before using this method.");
            }
        }

        private void ensureInitialized(@NotNull FancyMenuMcpSession activeSession) {
            if (!activeSession.isInitializeCalled()) {
                throw new IllegalArgumentException("Call 'initialize' before calling tools.");
            }
            // Some clients skip notifications/initialized - tolerate this.
            if (!activeSession.isInitialized()) {
                activeSession.setInitialized(true);
            }
        }

        private void close() {
            if (!connected.getAndSet(false)) {
                return;
            }
            clients.remove(this);
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            Thread t = this.thread;
            if (t != null) {
                t.interrupt();
            }
        }
    }

    private static @NotNull String negotiateProtocolVersion(@Nullable JsonObject initializeParams) {
        if (initializeParams == null) {
            return PROTOCOL_VERSION;
        }
        String requested = getString(initializeParams, "protocolVersion", null);
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        if (initializeParams.has("supportedProtocolVersions") && initializeParams.get("supportedProtocolVersions").isJsonArray()) {
            JsonArray supported = initializeParams.getAsJsonArray("supportedProtocolVersions");
            for (JsonElement element : supported) {
                if (element == null || element.isJsonNull()) {
                    continue;
                }
                String value = element.getAsString();
                if (value != null) {
                    String normalized = value.trim();
                    if (!normalized.isEmpty()) {
                        return normalized;
                    }
                }
            }
        }
        return PROTOCOL_VERSION;
    }

    private static @Nullable IncomingMessage readIncomingMessage(@NotNull InputStream inputStream) throws IOException {
        String firstLine = readLine(inputStream);
        if (firstLine == null) {
            return null;
        }
        while (firstLine.isBlank()) {
            firstLine = readLine(inputStream);
            if (firstLine == null) {
                return null;
            }
        }

        if (firstLine.startsWith("{")) {
            throw new IOException("Unsupported unframed JSON request. Use Content-Length framing.");
        }

        if (isHttpRequestLine(firstLine)) {
            String[] parts = firstLine.split(" ", 3);
            String method = (parts.length > 0 ? parts[0] : "").toUpperCase(Locale.ROOT);
            Map<String, String> headers = readHeaders(inputStream, readLine(inputStream));
            int contentLength = parseContentLength(headers, false);
            String payload = "";
            if (contentLength > 0) {
                byte[] bytes = readExact(inputStream, contentLength);
                payload = new String(bytes, StandardCharsets.UTF_8);
            }
            return IncomingMessage.http(method, headers, payload);
        }

        Map<String, String> headers = readHeaders(inputStream, firstLine);
        int contentLength = parseContentLength(headers, true);
        if (contentLength <= 0) {
            throw new IOException("Missing or invalid Content-Length header.");
        }
        byte[] payload = readExact(inputStream, contentLength);
        return IncomingMessage.framed(new String(payload, StandardCharsets.UTF_8));
    }

    private static @NotNull Map<String, String> readHeaders(@NotNull InputStream inputStream, @Nullable String firstHeaderLine) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        String line = firstHeaderLine;
        while (line != null && !line.isBlank()) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(separator + 1).trim();
                headers.put(key, value);
            }
            line = readLine(inputStream);
        }
        return headers;
    }

    private static int parseContentLength(@NotNull Map<String, String> headers, boolean required) throws IOException {
        String value = headers.get("content-length");
        if (value == null || value.isBlank()) {
            if (required) {
                throw new IOException("Missing or invalid Content-Length header.");
            }
            return 0;
        }
        int contentLength;
        try {
            contentLength = Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid Content-Length header value: " + value, ex);
        }
        if (contentLength < 0) {
            throw new IOException("Invalid Content-Length header value: " + value);
        }
        if (contentLength > MAX_REQUEST_BYTES) {
            throw new IOException("Content-Length exceeds max request size: " + contentLength);
        }
        return contentLength;
    }

    private static void writeFramedMessage(@NotNull OutputStream outputStream, @NotNull String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + bytes.length + "\r\n\r\n";
        outputStream.write(header.getBytes(StandardCharsets.US_ASCII));
        outputStream.write(bytes);
        outputStream.flush();
    }

    private static void writeHttpResponse(@NotNull OutputStream outputStream, @NotNull HttpResponse response) throws IOException {
        byte[] body = response.body() != null ? response.body().getBytes(StandardCharsets.UTF_8) : new byte[0];
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 ").append(response.status()).append(' ').append(response.reason()).append("\r\n");
        header.append("Content-Length: ").append(body.length).append("\r\n");
        if (body.length > 0) {
            header.append("Content-Type: application/json; charset=utf-8\r\n");
        }
        if (response.status() == 405) {
            header.append("Allow: GET, POST, DELETE\r\n");
        }
        header.append("Cache-Control: no-store\r\n");
        header.append("Connection: keep-alive\r\n");
        if (response.sessionId() != null && !response.sessionId().isBlank()) {
            header.append("Mcp-Session-Id: ").append(response.sessionId()).append("\r\n");
        }
        header.append("\r\n");
        outputStream.write(header.toString().getBytes(StandardCharsets.US_ASCII));
        if (body.length > 0) {
            outputStream.write(body);
        }
        outputStream.flush();
    }

    private static boolean isHttpRequestLine(@NotNull String line) {
        return line.contains("HTTP/") && line.indexOf(' ') > 0;
    }

    private static @Nullable String readLine(@NotNull InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        while (true) {
            int read = inputStream.read();
            if (read == -1) {
                if (builder.isEmpty()) {
                    return null;
                }
                break;
            }
            if (read == '\n') {
                break;
            }
            if (read != '\r') {
                builder.append((char) read);
            }
        }
        return builder.toString();
    }

    private static byte @NotNull [] readExact(@NotNull InputStream inputStream, int length) throws IOException {
        byte[] bytes = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = inputStream.read(bytes, offset, length - offset);
            if (read == -1) {
                throw new IOException("Unexpected end of stream while reading payload.");
            }
            offset += read;
        }
        return bytes;
    }

    private static @NotNull JsonObject success(@Nullable JsonElement id, @NotNull JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", JSON_RPC_VERSION);
        response.add("id", id != null ? id : JsonNull.INSTANCE);
        response.add("result", result);
        return response;
    }

    private static @NotNull JsonObject error(@Nullable JsonElement id, int code, @NotNull String message) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", JSON_RPC_VERSION);
        response.add("id", (id == null || id.isJsonNull()) ? JsonNull.INSTANCE : id);
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        return response;
    }

    private static @Nullable String getString(@NotNull JsonObject json, @NotNull String key, @Nullable String fallback) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return fallback;
        }
        String value = json.get(key).getAsString();
        if (value == null) {
            return fallback;
        }
        return value.trim();
    }

    private enum IncomingTransport {
        FRAMED,
        HTTP
    }

    private record IncomingMessage(
            @NotNull IncomingTransport transport,
            @Nullable String httpMethod,
            @NotNull Map<String, String> headers,
            @Nullable String payload
    ) {
        private static @NotNull IncomingMessage framed(@NotNull String payload) {
            return new IncomingMessage(IncomingTransport.FRAMED, null, Map.of(), payload);
        }

        private static @NotNull IncomingMessage http(@NotNull String method, @NotNull Map<String, String> headers, @Nullable String payload) {
            return new IncomingMessage(IncomingTransport.HTTP, method, headers, payload);
        }
    }

    private record HttpResponse(
            int status,
            @NotNull String reason,
            @Nullable String body,
            @Nullable String sessionId
    ) {
        private static @NotNull HttpResponse empty(int status, @NotNull String reason, @Nullable String sessionId) {
            return new HttpResponse(status, reason, null, sessionId);
        }

        private static @NotNull HttpResponse json(int status, @NotNull String reason, @NotNull JsonObject body, @Nullable String sessionId) {
            return new HttpResponse(status, reason, GSON.toJson(body), sessionId);
        }
    }
}
