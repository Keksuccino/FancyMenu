package de.keksuccino.fancymenu.customization.remote;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CorruptedWebSocketFrameException;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Netty is used instead of {@code java.net.http.WebSocket} because Java 25's client API rejects close status 1009.
 * Netty exposes RFC 6455 frames directly, allowing limits to be enforced before aggregation and a real 1009 close
 * frame to be flushed to the peer.
 */
final class NettyRemoteWebSocketTransport implements RemoteWebSocketTransport {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int HANDSHAKE_RESPONSE_MAX_BYTES = 64 * 1024;
    private static final long HANDSHAKE_TIMEOUT_MILLIS = 15_000L;
    private static final long FORCE_CLOSE_TIMEOUT_MILLIS = 5_000L;
    private static final int ABNORMAL_CLOSURE_STATUS = 1006;
    private static final int NO_STATUS_RECEIVED = 1005;
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private static final SslContext SSL_CONTEXT = createSslContext();

    private final EventLoopGroup eventLoopGroup;
    private final int maxInboundMessageBytes;
    private final int maxInboundFragments;

    NettyRemoteWebSocketTransport(int maxInboundMessageBytes, int maxInboundFragments) {
        this(new MultiThreadIoEventLoopGroup(1, runnable -> {
            Thread thread = new Thread(runnable, "FancyMenu-RemoteServerConnection-IO-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }, NioIoHandler.newFactory()), maxInboundMessageBytes, maxInboundFragments);
    }

    NettyRemoteWebSocketTransport(@NotNull EventLoopGroup eventLoopGroup, int maxInboundMessageBytes, int maxInboundFragments) {
        if (maxInboundMessageBytes <= 0 || maxInboundFragments <= 0) {
            throw new IllegalArgumentException("Inbound WebSocket limits must be positive");
        }
        this.eventLoopGroup = eventLoopGroup;
        this.maxInboundMessageBytes = maxInboundMessageBytes;
        this.maxInboundFragments = maxInboundFragments;
    }

    @Override
    public @NotNull Connection connect(@NotNull URI uri, @NotNull Listener listener) {
        NettyConnection connection = new NettyConnection(listener, this.maxInboundMessageBytes, this.maxInboundFragments);
        int port = resolvePort(uri);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(this.eventLoopGroup).channel(NioSocketChannel.class).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel channel) {
                    if ("wss".equalsIgnoreCase(uri.getScheme())) {
                        channel.pipeline().addLast("ssl", SSL_CONTEXT.newHandler(channel.alloc(), uri.getHost(), port));
                    }
                    channel.pipeline().addLast("http_codec", new HttpClientCodec());
                    channel.pipeline().addLast("http_aggregator", new HttpObjectAggregator(HANDSHAKE_RESPONSE_MAX_BYTES));
                    WebSocketClientProtocolConfig protocolConfig = WebSocketClientProtocolConfig.newBuilder().webSocketUri(uri).version(WebSocketVersion.V13).allowExtensions(false).maxFramePayloadLength(maxInboundMessageBytes).handleCloseFrames(false).sendCloseFrame(null).dropPongFrames(false).handshakeTimeoutMillis(HANDSHAKE_TIMEOUT_MILLIS).forceCloseTimeoutMillis(FORCE_CLOSE_TIMEOUT_MILLIS).withUTF8Validator(false).build();
                    channel.pipeline().addLast("websocket_protocol", new WebSocketClientProtocolHandler(new BoundedClientHandshaker(uri, maxInboundMessageBytes), protocolConfig));
                    // Decoder violations originate before Netty's frame encoder in the inbound pipeline. The custom
                    // decoder suppresses its incorrectly positioned close write so this guard can emit the exact RFC
                    // status through the encoder before closing the channel.
                    channel.pipeline().addBefore("websocket_protocol", "fancymenu_protocol_violation_guard", new ProtocolViolationGuard(connection));
                    channel.pipeline().addLast("fancymenu_remote", connection.handler);
                }
            });
            ChannelFuture connectFuture = bootstrap.connect(uri.getHost(), port);
            connection.attachChannel(connectFuture.channel());
            connectFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    connection.notifyError(Objects.requireNonNullElseGet(future.cause(), () -> new IllegalStateException("Remote WebSocket connection failed")));
                }
            });
        } catch (Throwable throwable) {
            connection.notifyError(throwable);
        }
        return connection;
    }

    private static int resolvePort(@NotNull URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "wss".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static @NotNull SslContext createSslContext() {
        try {
            return SslContextBuilder.forClient().endpointIdentificationAlgorithm("HTTPS").build();
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static @NotNull CompletableFuture<Void> toCompletableFuture(@NotNull ChannelFuture channelFuture) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                result.complete(null);
            } else {
                result.completeExceptionally(Objects.requireNonNullElseGet(future.cause(), () -> new IllegalStateException("Remote WebSocket operation failed")));
            }
        });
        return result;
    }

    private static final class NettyConnection implements Connection {

        private final Listener listener;
        private final InboundMessageBuffer inboundMessageBuffer;
        private final AtomicBoolean handshakeCompleted = new AtomicBoolean();
        private final AtomicBoolean closing = new AtomicBoolean();
        private final AtomicBoolean terminalCallbackSent = new AtomicBoolean();
        private final FrameHandler handler = new FrameHandler(this);

        private volatile @Nullable Channel channel;
        private volatile int localCloseStatus = ABNORMAL_CLOSURE_STATUS;
        private volatile String localCloseReason = "connection_closed";

        private NettyConnection(@NotNull Listener listener, int maxInboundMessageBytes, int maxInboundFragments) {
            this.listener = listener;
            this.inboundMessageBuffer = new InboundMessageBuffer(maxInboundMessageBytes, maxInboundFragments);
        }

        @Override
        public boolean isOpen() {
            Channel currentChannel = this.channel;
            return this.handshakeCompleted.get() && !this.closing.get() && currentChannel != null && currentChannel.isActive();
        }

        @Override
        public @NotNull CompletableFuture<Void> sendText(@NotNull String data) {
            Channel currentChannel = this.channel;
            if (!isOpen() || currentChannel == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Remote WebSocket is not open"));
            }
            return toCompletableFuture(currentChannel.writeAndFlush(new TextWebSocketFrame(data)));
        }

        @Override
        public @NotNull CompletableFuture<Void> sendPing(byte @NotNull [] data) {
            Channel currentChannel = this.channel;
            if (!isOpen() || currentChannel == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Remote WebSocket is not open"));
            }
            return toCompletableFuture(currentChannel.writeAndFlush(new io.netty.handler.codec.http.websocketx.PingWebSocketFrame(io.netty.buffer.Unpooled.copiedBuffer(data))));
        }

        @Override
        public @NotNull CompletableFuture<Void> close(int statusCode, @NotNull String reason) {
            Channel currentChannel = this.channel;
            if (currentChannel == null || !currentChannel.isActive()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Remote WebSocket is not active"));
            }

            CloseWebSocketFrame closeFrame;
            try {
                closeFrame = new CloseWebSocketFrame(statusCode, reason);
            } catch (RuntimeException ex) {
                return CompletableFuture.failedFuture(ex);
            }
            if (!this.closing.compareAndSet(false, true)) {
                closeFrame.release();
                return CompletableFuture.failedFuture(new IllegalStateException("Remote WebSocket is already closing"));
            }

            this.localCloseStatus = statusCode;
            this.localCloseReason = reason;
            ChannelFuture closeFuture = currentChannel.writeAndFlush(closeFrame);
            closeFuture.addListener(ChannelFutureListener.CLOSE);
            return toCompletableFuture(closeFuture);
        }

        @Override
        public void abort() {
            this.inboundMessageBuffer.reset();
            this.closing.set(true);
            Channel currentChannel = this.channel;
            if (currentChannel != null) {
                currentChannel.close();
            }
        }

        private void attachChannel(@NotNull Channel channel) {
            this.channel = channel;
            if (this.closing.get()) {
                channel.close();
            }
        }

        private void notifyOpen() {
            if (this.terminalCallbackSent.get() || !this.handshakeCompleted.compareAndSet(false, true)) {
                return;
            }
            try {
                this.listener.onOpen(this);
            } catch (Throwable throwable) {
                notifyError(throwable);
            }
        }

        private void notifyText(@NotNull String text, int utf8Bytes) {
            if (this.terminalCallbackSent.get() || this.closing.get()) {
                return;
            }
            try {
                this.listener.onText(this, text, utf8Bytes);
            } catch (Throwable throwable) {
                notifyError(throwable);
            }
        }

        private void notifyPong() {
            if (this.terminalCallbackSent.get() || this.closing.get()) {
                return;
            }
            try {
                this.listener.onPong(this);
            } catch (Throwable throwable) {
                notifyError(throwable);
            }
        }

        private void notifyClose(int statusCode, @NotNull String reason) {
            this.inboundMessageBuffer.reset();
            this.closing.set(true);
            if (!this.terminalCallbackSent.compareAndSet(false, true)) {
                return;
            }
            try {
                this.listener.onClose(this, statusCode, reason);
            } catch (Throwable throwable) {
                LOGGER.error("[FANCYMENU] Remote WebSocket close listener failed", throwable);
            }
        }

        private void notifyError(@NotNull Throwable error) {
            this.inboundMessageBuffer.reset();
            this.closing.set(true);
            if (!this.terminalCallbackSent.compareAndSet(false, true)) {
                return;
            }
            try {
                this.listener.onError(this, error);
            } catch (Throwable throwable) {
                LOGGER.error("[FANCYMENU] Remote WebSocket error listener failed", throwable);
            } finally {
                Channel currentChannel = this.channel;
                if (currentChannel != null) {
                    currentChannel.close();
                }
            }
        }

        private void notifyChannelInactive() {
            if (this.handshakeCompleted.get()) {
                notifyClose(this.localCloseStatus, this.localCloseReason);
            } else {
                notifyError(new IllegalStateException("Remote WebSocket channel closed before the handshake completed"));
            }
        }

        private void receiveClose(@NotNull ChannelHandlerContext context, @NotNull CloseWebSocketFrame frame) {
            int statusCode = frame.statusCode();
            if (statusCode < 0) {
                statusCode = NO_STATUS_RECEIVED;
            }
            String reason = frame.reasonText();
            this.closing.set(true);
            notifyClose(statusCode, reason);
            context.writeAndFlush(statusCode == NO_STATUS_RECEIVED ? new CloseWebSocketFrame() : new CloseWebSocketFrame(statusCode, reason)).addListener(ChannelFutureListener.CLOSE);
        }

        private void closeForInboundViolation(@NotNull ChannelHandlerContext context, int statusCode, @NotNull String reason) {
            this.inboundMessageBuffer.reset();
            if (!this.closing.compareAndSet(false, true)) {
                return;
            }
            this.localCloseStatus = statusCode;
            this.localCloseReason = reason;
            context.writeAndFlush(new CloseWebSocketFrame(statusCode, reason)).addListener(ChannelFutureListener.CLOSE);
        }

    }

    private static final class BoundedClientHandshaker extends WebSocketClientHandshaker13 {

        private final int maxFramePayloadLength;

        private BoundedClientHandshaker(@NotNull URI uri, int maxFramePayloadLength) {
            super(uri, WebSocketVersion.V13, null, false, null, maxFramePayloadLength, true, false, FORCE_CLOSE_TIMEOUT_MILLIS);
            this.maxFramePayloadLength = maxFramePayloadLength;
        }

        @Override
        protected WebSocketFrameDecoder newWebsocketDecoder() {
            WebSocketDecoderConfig decoderConfig = WebSocketDecoderConfig.newBuilder().expectMaskedFrames(false).allowExtensions(false).maxFramePayloadLength(this.maxFramePayloadLength).allowMaskMismatch(false).closeOnProtocolViolation(false).withUTF8Validator(false).build();
            return new WebSocket13FrameDecoder(decoderConfig);
        }
    }

    private static final class ProtocolViolationGuard extends ChannelInboundHandlerAdapter {

        private final NettyConnection connection;

        private ProtocolViolationGuard(@NotNull NettyConnection connection) {
            this.connection = connection;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            if (cause instanceof CorruptedWebSocketFrameException corruptedFrame) {
                String reason = Objects.requireNonNullElse(corruptedFrame.getMessage(), corruptedFrame.closeStatus().reasonText());
                this.connection.closeForInboundViolation(context, corruptedFrame.closeStatus().code(), reason);
                return;
            }
            context.fireExceptionCaught(cause);
        }
    }

    private static final class FrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

        private final NettyConnection connection;

        private FrameHandler(@NotNull NettyConnection connection) {
            this.connection = connection;
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
            if (event == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                this.connection.notifyOpen();
            } else if (event == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT) {
                this.connection.notifyError(new IllegalStateException("Remote WebSocket handshake timed out"));
            }
            super.userEventTriggered(context, event);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, WebSocketFrame frame) {
            if (this.connection.closing.get()) {
                return;
            }
            if (frame instanceof CloseWebSocketFrame closeFrame) {
                this.connection.receiveClose(context, closeFrame);
                return;
            }
            if (frame instanceof PongWebSocketFrame) {
                this.connection.notifyPong();
                return;
            }

            InboundMessageBuffer.Result result;
            ByteBuffer payload = frame.content().nioBuffer();
            if (frame instanceof TextWebSocketFrame) {
                result = this.connection.inboundMessageBuffer.acceptText(payload, frame.isFinalFragment());
            } else if (frame instanceof BinaryWebSocketFrame) {
                result = this.connection.inboundMessageBuffer.acceptBinary(payload, frame.isFinalFragment());
            } else if (frame instanceof ContinuationWebSocketFrame) {
                result = this.connection.inboundMessageBuffer.acceptContinuation(payload, frame.isFinalFragment());
            } else {
                return;
            }
            handleInboundResult(context, result);
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) throws Exception {
            this.connection.notifyChannelInactive();
            super.channelInactive(context);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            if (cause instanceof CorruptedWebSocketFrameException corruptedFrame) {
                String reason = Objects.requireNonNullElse(corruptedFrame.getMessage(), corruptedFrame.closeStatus().reasonText());
                this.connection.closeForInboundViolation(context, corruptedFrame.closeStatus().code(), reason);
                return;
            }
            this.connection.notifyError(cause);
            context.close();
        }

        private void handleInboundResult(@NotNull ChannelHandlerContext context, @NotNull InboundMessageBuffer.Result result) {
            switch (result.type()) {
                case COMPLETE_TEXT -> this.connection.notifyText(Objects.requireNonNull(result.text()), result.utf8Bytes());
                case TOO_LARGE -> this.connection.closeForInboundViolation(context, WebSocketCloseStatus.MESSAGE_TOO_BIG.code(), "inbound_message_too_large");
                case TOO_MANY_FRAGMENTS -> this.connection.closeForInboundViolation(context, WebSocketCloseStatus.MESSAGE_TOO_BIG.code(), "too_many_message_fragments");
                case INVALID_UTF8 -> this.connection.closeForInboundViolation(context, WebSocketCloseStatus.INVALID_PAYLOAD_DATA.code(), "invalid_utf8_text_message");
                case INVALID_SEQUENCE -> this.connection.closeForInboundViolation(context, WebSocketCloseStatus.PROTOCOL_ERROR.code(), "invalid_fragment_sequence");
                case PARTIAL, COMPLETE_BINARY -> {
                }
            }
        }
    }
}
