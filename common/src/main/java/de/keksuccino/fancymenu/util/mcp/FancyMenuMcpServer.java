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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
        socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), configuredPort));
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
    }

    boolean isRunning() {
        return running.get();
    }

    int getBoundPort() {
        return boundPort;
    }

    private void acceptLoop() {
        LOGGER.info("[FANCYMENU MCP] Server started on 127.0.0.1:{}.", boundPort);
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
                    String message;
                    try {
                        message = readMessage(in);
                    } catch (SocketTimeoutException ignored) {
                        continue;
                    }
                    if (message == null) {
                        break;
                    }
                    JsonObject response = handleMessage(message);
                    if (response != null) {
                        writeMessage(out, response.toString());
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

        private @Nullable JsonObject handleMessage(@NotNull String rawMessage) {
            JsonElement parsed;
            try {
                parsed = JsonParser.parseString(rawMessage);
            } catch (Exception ex) {
                return error(null, -32700, "Parse error: invalid JSON.");
            }
            if (!parsed.isJsonObject()) {
                return error(null, -32600, "Invalid Request: expected JSON object.");
            }
            JsonObject request = parsed.getAsJsonObject();
            JsonElement id = request.get("id");
            boolean respond = id != null && !id.isJsonNull();
            String method = getString(request, "method", null);
            if (method == null || method.isBlank()) {
                return respond ? error(id, -32600, "Invalid Request: missing method.") : null;
            }

            try {
                JsonObject response = switch (method) {
                    case "initialize" -> {
                        session.setInitializeCalled(true);
                        session.setInitialized(false);
                        session.setIntroCalled(false);
                        yield success(id, initializeResult());
                    }
                    case "notifications/initialized" -> {
                        ensureInitializeCalled();
                        session.setInitialized(true);
                        yield null;
                    }
                    case "ping" -> success(id, new JsonObject());
                    case "tools/list" -> {
                        ensureInitializeCalled();
                        yield success(id, toolsListResult());
                    }
                    case "tools/call" -> {
                        ensureInitialized();
                        yield handleToolCall(id, request.getAsJsonObject("params"));
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

        private @Nullable JsonObject handleToolCall(@Nullable JsonElement id, @Nullable JsonObject params) throws Exception {
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
            if (!session.isIntroCalled() && !"fancymenu_intro".equals(toolName)) {
                throw new IllegalArgumentException("You must call 'fancymenu_intro' first in this session before using other tools.");
            }

            JsonObject toolArguments = params.has("arguments") && params.get("arguments").isJsonObject()
                    ? params.getAsJsonObject("arguments")
                    : new JsonObject();

            FancyMenuMcpTools.ToolExecution execution = FancyMenuMcpTools.executeTool(toolName, toolArguments);
            if ("fancymenu_intro".equals(execution.getToolName())) {
                session.setIntroCalled(true);
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

        private @NotNull JsonObject initializeResult() {
            JsonObject result = new JsonObject();
            result.addProperty("protocolVersion", PROTOCOL_VERSION);
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

        private void ensureInitializeCalled() {
            if (!session.isInitializeCalled()) {
                throw new IllegalArgumentException("Call 'initialize' before using this method.");
            }
        }

        private void ensureInitialized() {
            if (!session.isInitializeCalled()) {
                throw new IllegalArgumentException("Call 'initialize' before calling tools.");
            }
            if (!session.isInitialized()) {
                throw new IllegalArgumentException("Send 'notifications/initialized' before calling tools.");
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

    private static @Nullable String readMessage(@NotNull InputStream inputStream) throws IOException {
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

        int contentLength = -1;
        String line = firstLine;
        while (line != null && !line.isBlank()) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if ("content-length".equalsIgnoreCase(key)) {
                    try {
                        contentLength = Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        throw new IOException("Invalid Content-Length header value: " + value, ex);
                    }
                }
            }
            line = readLine(inputStream);
        }
        if (contentLength <= 0) {
            throw new IOException("Missing or invalid Content-Length header.");
        }
        if (contentLength > MAX_REQUEST_BYTES) {
            throw new IOException("Content-Length exceeds max request size: " + contentLength);
        }
        byte[] payload = readExact(inputStream, contentLength);
        return new String(payload, StandardCharsets.UTF_8);
    }

    private static void writeMessage(@NotNull OutputStream outputStream, @NotNull String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + bytes.length + "\r\n\r\n";
        outputStream.write(header.getBytes(StandardCharsets.US_ASCII));
        outputStream.write(bytes);
        outputStream.flush();
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
        return json.get(key).getAsString();
    }
}
