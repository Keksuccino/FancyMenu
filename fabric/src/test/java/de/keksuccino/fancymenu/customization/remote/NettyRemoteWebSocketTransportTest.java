package de.keksuccino.fancymenu.customization.remote;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NettyRemoteWebSocketTransportTest {

    private static final int TIMEOUT_SECONDS = 5;
    private static final int MESSAGE_LIMIT = 8;

    @Test
    void exactLimitTextFrameIsDeliveredWithoutAProtocolClose() throws Exception {
        try (LoopbackWebSocketServer server = LoopbackWebSocketServer.webSocket(context -> {
            context.write(new TextWebSocketFrame("12345678"));
            context.writeAndFlush(new CloseWebSocketFrame(1000, "")).addListener(ChannelFutureListener.CLOSE);
        }); ClientHarness client = new ClientHarness(MESSAGE_LIMIT, 4)) {
            CountingListener listener = new CountingListener();

            client.transport.connect(server.uri(), listener);

            TextEvent text = listener.text.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            CloseEvent close = listener.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals("12345678", text.text());
            assertEquals(MESSAGE_LIMIT, text.utf8Bytes());
            assertEquals(1000, close.statusCode());
            assertEquals(1, listener.openCallbacks.get());
            assertEquals(1, listener.textCallbacks.get());
            assertEquals(1, listener.closeCallbacks.get());
            assertEquals(0, listener.errorCallbacks.get());
        }
    }

    @Test
    void peerObserves1009ForSingleFrameOneByteOverLimit() throws Exception {
        try (LoopbackWebSocketServer server = LoopbackWebSocketServer.webSocket(context -> context.writeAndFlush(new TextWebSocketFrame("123456789"))); ClientHarness client = new ClientHarness(MESSAGE_LIMIT, 4)) {
            CountingListener listener = new CountingListener();

            client.transport.connect(server.uri(), listener);

            CloseEvent peerClose = server.clientClose.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            CloseEvent localClose = listener.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals(1009, peerClose.statusCode());
            assertEquals(1009, localClose.statusCode());
            assertEquals(0, listener.textCallbacks.get());
            assertEquals(1, listener.closeCallbacks.get());
            assertEquals(0, listener.errorCallbacks.get());
        }
    }

    @Test
    void peerObserves1009WhenFragmentAggregateExceedsLimit() throws Exception {
        try (LoopbackWebSocketServer server = LoopbackWebSocketServer.webSocket(context -> {
            context.write(new TextWebSocketFrame(false, 0, Unpooled.wrappedBuffer(new byte[]{'1', '2', '3', '4', '5'})));
            context.writeAndFlush(new ContinuationWebSocketFrame(true, 0, Unpooled.wrappedBuffer(new byte[]{'6', '7', '8', '9'})));
        }); ClientHarness client = new ClientHarness(MESSAGE_LIMIT, 4)) {
            CountingListener listener = new CountingListener();

            client.transport.connect(server.uri(), listener);

            CloseEvent peerClose = server.clientClose.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            CloseEvent localClose = listener.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals(1009, peerClose.statusCode());
            assertEquals(1009, localClose.statusCode());
            assertEquals(0, listener.textCallbacks.get());
            assertEquals(1, listener.closeCallbacks.get());
            assertEquals(0, listener.errorCallbacks.get());
        }
    }

    @Test
    void multibyteCodePointCanSpanWireFragments() throws Exception {
        byte[] emoji = "😀".getBytes(StandardCharsets.UTF_8);
        try (LoopbackWebSocketServer server = LoopbackWebSocketServer.webSocket(context -> {
            context.write(new TextWebSocketFrame(false, 0, Unpooled.wrappedBuffer(emoji, 0, 2)));
            context.write(new ContinuationWebSocketFrame(true, 0, Unpooled.wrappedBuffer(emoji, 2, 2)));
            context.writeAndFlush(new CloseWebSocketFrame(1000, "")).addListener(ChannelFutureListener.CLOSE);
        }); ClientHarness client = new ClientHarness(4, 2)) {
            CountingListener listener = new CountingListener();

            client.transport.connect(server.uri(), listener);

            TextEvent text = listener.text.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            listener.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals("😀", text.text());
            assertEquals(4, text.utf8Bytes());
            assertEquals(1, listener.textCallbacks.get());
            assertEquals(0, listener.errorCallbacks.get());
        }
    }

    @Test
    void peerObserves1009WhenZeroByteFragmentsExceedFragmentLimit() throws Exception {
        try (LoopbackWebSocketServer server = LoopbackWebSocketServer.webSocket(context -> {
            context.write(new TextWebSocketFrame(false, 0, Unpooled.EMPTY_BUFFER));
            context.write(new ContinuationWebSocketFrame(false, 0, Unpooled.EMPTY_BUFFER));
            context.writeAndFlush(new ContinuationWebSocketFrame(true, 0, Unpooled.EMPTY_BUFFER));
        }); ClientHarness client = new ClientHarness(MESSAGE_LIMIT, 2)) {
            CountingListener listener = new CountingListener();

            client.transport.connect(server.uri(), listener);

            assertEquals(1009, server.clientClose.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).statusCode());
            assertEquals(1009, listener.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).statusCode());
            assertEquals(0, listener.textCallbacks.get());
            assertEquals(0, listener.errorCallbacks.get());
        }
    }

    @Test
    void fragmentedBinaryCompletionResetsAggregateBeforeNextText() throws Exception {
        try (LoopbackWebSocketServer server = LoopbackWebSocketServer.webSocket(context -> {
            context.write(new BinaryWebSocketFrame(false, 0, Unpooled.wrappedBuffer(new byte[]{1, 2})));
            context.write(new ContinuationWebSocketFrame(true, 0, Unpooled.wrappedBuffer(new byte[]{3, 4})));
            context.write(new TextWebSocketFrame("next"));
            context.writeAndFlush(new CloseWebSocketFrame(1000, "")).addListener(ChannelFutureListener.CLOSE);
        }); ClientHarness client = new ClientHarness(4, 2)) {
            CountingListener listener = new CountingListener();

            client.transport.connect(server.uri(), listener);

            TextEvent text = listener.text.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            listener.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals("next", text.text());
            assertEquals(4, text.utf8Bytes());
            assertEquals(1, listener.textCallbacks.get());
            assertEquals(0, listener.errorCallbacks.get());
        }
    }

    @Test
    void peerObserves1009WhenFragmentedBinaryAggregateExceedsLimit() throws Exception {
        try (LoopbackWebSocketServer server = LoopbackWebSocketServer.webSocket(context -> {
            context.write(new BinaryWebSocketFrame(false, 0, Unpooled.wrappedBuffer(new byte[]{1, 2, 3})));
            context.writeAndFlush(new ContinuationWebSocketFrame(true, 0, Unpooled.wrappedBuffer(new byte[]{4, 5})));
        }); ClientHarness client = new ClientHarness(4, 2)) {
            CountingListener listener = new CountingListener();

            client.transport.connect(server.uri(), listener);

            assertEquals(1009, server.clientClose.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).statusCode());
            assertEquals(1009, listener.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).statusCode());
            assertEquals(0, listener.textCallbacks.get());
            assertEquals(0, listener.errorCallbacks.get());
        }
    }

    @Test
    void terminalCloseDiscardsAnIncompleteTextMessage() throws Exception {
        try (LoopbackWebSocketServer server = LoopbackWebSocketServer.webSocket(context -> {
            context.write(new TextWebSocketFrame(false, 0, Unpooled.copiedBuffer("partial", StandardCharsets.UTF_8)));
            context.writeAndFlush(new CloseWebSocketFrame(1000, "")).addListener(ChannelFutureListener.CLOSE);
        }); ClientHarness client = new ClientHarness(MESSAGE_LIMIT, 2)) {
            CountingListener listener = new CountingListener();

            client.transport.connect(server.uri(), listener);

            assertEquals(1000, listener.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).statusCode());
            client.drainEventLoop();
            assertEquals(0, listener.textCallbacks.get());
            assertEquals(1, listener.closeCallbacks.get());
            assertEquals(0, listener.errorCallbacks.get());
        }
    }

    @Test
    void outboundTextPingAndCloseUseWebSocketFrames() throws Exception {
        try (LoopbackWebSocketServer server = LoopbackWebSocketServer.webSocket(context -> {
        }); ClientHarness client = new ClientHarness(MESSAGE_LIMIT, 4)) {
            CountingListener listener = new CountingListener();
            RemoteWebSocketTransport.Connection connection = client.transport.connect(server.uri(), listener);
            listener.opened.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            connection.sendText("outbound").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            connection.sendPing(new byte[]{1, 2, 3}).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertEquals("outbound", server.clientText.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            listener.pong.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            connection.close(1000, "client_done").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            CloseEvent close = server.clientClose.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals(1000, close.statusCode());
            assertEquals("client_done", close.reason());
            assertEquals(1, listener.pongCallbacks.get());
            assertEquals(0, listener.errorCallbacks.get());
        }
    }

    @Test
    void successfulHandshakeAndCloseCallbacksAreEachDeliveredOnce() throws Exception {
        try (LoopbackWebSocketServer server = LoopbackWebSocketServer.webSocket(context -> context.writeAndFlush(new CloseWebSocketFrame(1000, "")).addListener(ChannelFutureListener.CLOSE)); ClientHarness client = new ClientHarness(MESSAGE_LIMIT, 4)) {
            CountingListener listener = new CountingListener();

            RemoteWebSocketTransport.Connection connection = client.transport.connect(server.uri(), listener);

            listener.opened.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            listener.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            connection.abort();
            client.drainEventLoop();
            assertEquals(1, listener.openCallbacks.get());
            assertEquals(1, listener.closeCallbacks.get());
            assertEquals(0, listener.errorCallbacks.get());
        }
    }

    @Test
    void failedHandshakeProducesOneErrorAndNoOpenOrCloseCallback() throws Exception {
        try (RejectingHttpServer server = new RejectingHttpServer(); ClientHarness client = new ClientHarness(MESSAGE_LIMIT, 4)) {
            CountingListener listener = new CountingListener();

            RemoteWebSocketTransport.Connection connection = client.transport.connect(server.uri(), listener);

            Throwable error = listener.failed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertInstanceOf(Exception.class, error);
            connection.abort();
            client.drainEventLoop();
            assertEquals(0, listener.openCallbacks.get());
            assertEquals(0, listener.closeCallbacks.get());
            assertEquals(1, listener.errorCallbacks.get());
        }
    }

    @Test
    void disconnectBeforeHandshakeProducesOneErrorAndNoCloseCallback() throws Exception {
        try (PrematureDisconnectServer server = new PrematureDisconnectServer(); ClientHarness client = new ClientHarness(MESSAGE_LIMIT, 4)) {
            CountingListener listener = new CountingListener();

            client.transport.connect(server.uri(), listener);

            listener.failed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            client.drainEventLoop();
            assertEquals(0, listener.openCallbacks.get());
            assertEquals(0, listener.closeCallbacks.get());
            assertEquals(1, listener.errorCallbacks.get());
        }
    }

    @Test
    void shutdownIsIdempotentTerminatesTheEventLoopAndRejectsLateConnections() throws Exception {
        EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        NettyRemoteWebSocketTransport transport = new NettyRemoteWebSocketTransport(eventLoopGroup, MESSAGE_LIMIT, 4);
        try {
            CountingListener listener = new CountingListener();

            transport.shutdown();
            transport.shutdown();
            RemoteWebSocketTransport.Connection rejected = transport.connect(URI.create("ws://127.0.0.1:1/socket"), listener);

            assertTrue(transport.isTerminated());
            assertFalse(rejected.isOpen());
            assertInstanceOf(RejectedExecutionException.class, listener.failed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertEquals(0, listener.openCallbacks.get());
            assertEquals(0, listener.closeCallbacks.get());
            assertEquals(1, listener.errorCallbacks.get());
        } finally {
            transport.shutdown();
        }
    }

    private static final class ClientHarness implements AutoCloseable {

        private final EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        private final NettyRemoteWebSocketTransport transport;
        private boolean drained;

        private ClientHarness(int maxInboundMessageBytes, int maxInboundFragments) {
            this.transport = new NettyRemoteWebSocketTransport(this.eventLoopGroup, maxInboundMessageBytes, maxInboundFragments);
        }

        private void drainEventLoop() {
            if (!this.drained) {
                this.transport.shutdown();
                this.drained = true;
            }
        }

        @Override
        public void close() {
            drainEventLoop();
        }
    }

    private static final class LoopbackWebSocketServer implements AutoCloseable {

        private final EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        private final CompletableFuture<CloseEvent> clientClose = new CompletableFuture<>();
        private final CompletableFuture<String> clientText = new CompletableFuture<>();
        private final Channel serverChannel;

        private LoopbackWebSocketServer(@NotNull Consumer<ChannelHandlerContext> onHandshake) {
            WebSocketServerProtocolConfig protocolConfig = WebSocketServerProtocolConfig.newBuilder().websocketPath("/socket").allowExtensions(false).maxFramePayloadLength(1024).handleCloseFrames(false).sendCloseFrame(null).dropPongFrames(false).withUTF8Validator(true).build();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(this.eventLoopGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel channel) {
                    channel.pipeline().addLast(new HttpServerCodec());
                    channel.pipeline().addLast(new HttpObjectAggregator(64 * 1024));
                    channel.pipeline().addLast(new WebSocketServerProtocolHandler(protocolConfig));
                    channel.pipeline().addLast(new ServerFrameHandler(onHandshake, LoopbackWebSocketServer.this.clientClose, LoopbackWebSocketServer.this.clientText));
                }
            });
            this.serverChannel = bootstrap.bind("127.0.0.1", 0).syncUninterruptibly().channel();
        }

        private static LoopbackWebSocketServer webSocket(@NotNull Consumer<ChannelHandlerContext> onHandshake) {
            return new LoopbackWebSocketServer(onHandshake);
        }

        private URI uri() {
            int port = ((InetSocketAddress) this.serverChannel.localAddress()).getPort();
            return URI.create("ws://127.0.0.1:" + port + "/socket");
        }

        @Override
        public void close() {
            this.serverChannel.close().syncUninterruptibly();
            this.eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
        }
    }

    private static final class ServerFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

        private final Consumer<ChannelHandlerContext> onHandshake;
        private final CompletableFuture<CloseEvent> clientClose;
        private final CompletableFuture<String> clientText;

        private ServerFrameHandler(@NotNull Consumer<ChannelHandlerContext> onHandshake, @NotNull CompletableFuture<CloseEvent> clientClose, @NotNull CompletableFuture<String> clientText) {
            this.onHandshake = onHandshake;
            this.clientClose = clientClose;
            this.clientText = clientText;
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
            if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                this.onHandshake.accept(context);
            }
            super.userEventTriggered(context, event);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, WebSocketFrame frame) {
            if (frame instanceof CloseWebSocketFrame closeFrame) {
                this.clientClose.complete(new CloseEvent(closeFrame.statusCode(), closeFrame.reasonText()));
                context.close();
            } else if (frame instanceof TextWebSocketFrame textFrame) {
                this.clientText.complete(textFrame.text());
            }
        }
    }

    private static final class RejectingHttpServer implements AutoCloseable {

        private final EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        private final Channel serverChannel;

        private RejectingHttpServer() {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(this.eventLoopGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel channel) {
                    channel.pipeline().addLast(new HttpServerCodec());
                    channel.pipeline().addLast(new HttpObjectAggregator(64 * 1024));
                    channel.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) {
                            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
                            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
                            context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                        }
                    });
                }
            });
            this.serverChannel = bootstrap.bind("127.0.0.1", 0).syncUninterruptibly().channel();
        }

        private URI uri() {
            int port = ((InetSocketAddress) this.serverChannel.localAddress()).getPort();
            return URI.create("ws://127.0.0.1:" + port + "/socket");
        }

        @Override
        public void close() {
            this.serverChannel.close().syncUninterruptibly();
            this.eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
        }
    }

    private static final class PrematureDisconnectServer implements AutoCloseable {

        private final EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        private final Channel serverChannel;

        private PrematureDisconnectServer() {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(this.eventLoopGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel channel) {
                    channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelActive(ChannelHandlerContext context) {
                            context.close();
                        }
                    });
                }
            });
            this.serverChannel = bootstrap.bind("127.0.0.1", 0).syncUninterruptibly().channel();
        }

        private URI uri() {
            int port = ((InetSocketAddress) this.serverChannel.localAddress()).getPort();
            return URI.create("ws://127.0.0.1:" + port + "/socket");
        }

        @Override
        public void close() {
            this.serverChannel.close().syncUninterruptibly();
            this.eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
        }
    }

    private static final class CountingListener implements RemoteWebSocketTransport.Listener {

        private final AtomicInteger openCallbacks = new AtomicInteger();
        private final AtomicInteger textCallbacks = new AtomicInteger();
        private final AtomicInteger closeCallbacks = new AtomicInteger();
        private final AtomicInteger errorCallbacks = new AtomicInteger();
        private final AtomicInteger pongCallbacks = new AtomicInteger();
        private final CompletableFuture<RemoteWebSocketTransport.Connection> opened = new CompletableFuture<>();
        private final CompletableFuture<TextEvent> text = new CompletableFuture<>();
        private final CompletableFuture<CloseEvent> closed = new CompletableFuture<>();
        private final CompletableFuture<Throwable> failed = new CompletableFuture<>();
        private final CompletableFuture<Void> pong = new CompletableFuture<>();

        @Override
        public void onOpen(@NotNull RemoteWebSocketTransport.Connection connection) {
            this.openCallbacks.incrementAndGet();
            this.opened.complete(connection);
        }

        @Override
        public void onText(@NotNull RemoteWebSocketTransport.Connection connection, @NotNull String data, int utf8Bytes) {
            this.textCallbacks.incrementAndGet();
            this.text.complete(new TextEvent(data, utf8Bytes));
        }

        @Override
        public void onPong(@NotNull RemoteWebSocketTransport.Connection connection) {
            this.pongCallbacks.incrementAndGet();
            this.pong.complete(null);
        }

        @Override
        public void onClose(@NotNull RemoteWebSocketTransport.Connection connection, int statusCode, @NotNull String reason) {
            this.closeCallbacks.incrementAndGet();
            this.closed.complete(new CloseEvent(statusCode, reason));
        }

        @Override
        public void onError(@NotNull RemoteWebSocketTransport.Connection connection, @NotNull Throwable error) {
            this.errorCallbacks.incrementAndGet();
            this.failed.complete(error);
        }
    }

    private record TextEvent(@NotNull String text, int utf8Bytes) {
    }

    private record CloseEvent(int statusCode, @NotNull String reason) {
    }
}
