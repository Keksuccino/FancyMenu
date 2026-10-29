package de.keksuccino.fancymenu.customization.action.actions.file;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolver;
import de.keksuccino.fancymenu.util.file.GameDirectoryActionPathResolverFixture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadFileActionTest {

    private static final String BODY = "download-body";

    @TempDir
    Path temporaryDirectory;

    private final List<ListenerInstance> registeredInstances = new ArrayList<>();
    private Path gameRoot;
    private GameDirectoryActionPathResolver resolver;
    private HttpServer server;

    @BeforeEach
    void setUp() throws Exception {
        this.gameRoot = Files.createDirectory(this.temporaryDirectory.resolve("game"));
        Path minecraftRoot = Files.createDirectory(this.temporaryDirectory.resolve("minecraft"));
        this.resolver = GameDirectoryActionPathResolverFixture.create(this.gameRoot, minecraftRoot);
        drainMainThreadQueue();
    }

    @AfterEach
    void tearDown() {
        for (ListenerInstance instance : this.registeredInstances) {
            instance.parent.unregisterInstance(instance);
        }
        if (this.server != null) {
            this.server.stop(0);
        }
        drainMainThreadQueue();
    }

    @Test
    void transferStartReportsTheFinalTargetPathBeforeTheFileDataGetsTransferred() throws Exception {
        CountDownLatch transferStarted = new CountDownLatch(1);
        this.server = startServer("/am_update.arsa", exchange -> {
            try {
                exchange.sendResponseHeaders(200, BODY.length());
                // The server buffers the headers until the body gets written, so they need to get flushed for the client to see the response before the body
                exchange.getResponseBody().flush();
                // The body only gets sent after the client reported the transfer start, so reporting it after the transfer would fail the download
                if (!transferStarted.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("The transfer start was not reported before the file data got requested");
                }
                exchange.getResponseBody().write(BODY.getBytes(StandardCharsets.UTF_8));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException(ex);
            } finally {
                exchange.close();
            }
        });
        Files.createDirectory(this.gameRoot.resolve("downloads"));
        List<String> reportedPaths = new ArrayList<>();
        boolean[] targetExistedOnStart = {true};

        String downloadedPath = new DownloadFileAction().downloadFile(serverUrl("/am_update.arsa"), this.resolver.resolve("downloads"), targetPath -> {
            reportedPaths.add(targetPath);
            targetExistedOnStart[0] = Files.exists(Path.of(targetPath), LinkOption.NOFOLLOW_LINKS);
            transferStarted.countDown();
        });

        assertAll(
                () -> assertEquals(List.of(downloadedPath), reportedPaths),
                () -> assertFalse(targetExistedOnStart[0]),
                () -> assertTrue(Files.isSameFile(this.gameRoot.resolve("downloads/am_update.arsa"), Path.of(downloadedPath))),
                () -> assertEquals(BODY, Files.readString(Path.of(downloadedPath))));
    }

    @Test
    void successfulDownloadDeliversTheStartedEventBeforeTheDownloadedEvent() throws Exception {
        this.server = startServer("/ordered.txt", fixedBodyHandler());
        ListenerInstance started = registerInstance(Listeners.ON_FILE_DOWNLOAD_STARTED);
        ListenerInstance downloaded = registerInstance(Listeners.ON_FILE_DOWNLOADED);
        String url = serverUrl("/ordered.txt");

        new DownloadFileAction().executeWithResolver(url + "||downloads", this.resolver).get(5, TimeUnit.SECONDS);
        List<Runnable> tasks = drainMainThreadQueue();

        assertEquals(2, tasks.size());
        tasks.get(0).run();
        assertEquals(url, variable(started, "download_url"));
        assertNotEquals(url, variable(downloaded, "download_url"));
        tasks.get(1).run();
        assertAll(
                () -> assertEquals(url, variable(downloaded, "download_url")),
                () -> assertEquals("true", variable(downloaded, "download_succeeded")),
                () -> assertEquals(variable(downloaded, "target_file_path"), variable(started, "target_file_path")),
                () -> assertTrue(Files.isSameFile(this.gameRoot.resolve("downloads/ordered.txt"), Path.of(variable(started, "target_file_path")))));
    }

    @Test
    void rejectedDownloadOnlyDeliversAFailedDownloadedEvent() throws Exception {
        this.server = startServer("/missing.txt", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        ListenerInstance started = registerInstance(Listeners.ON_FILE_DOWNLOAD_STARTED);
        ListenerInstance downloaded = registerInstance(Listeners.ON_FILE_DOWNLOADED);
        String url = serverUrl("/missing.txt");

        new DownloadFileAction().executeWithResolver(url + "||downloads", this.resolver).get(5, TimeUnit.SECONDS);
        List<Runnable> tasks = drainMainThreadQueue();

        assertEquals(1, tasks.size());
        tasks.get(0).run();
        assertAll(
                () -> assertNotEquals(url, variable(started, "download_url")),
                () -> assertEquals(url, variable(downloaded, "download_url")),
                () -> assertEquals("false", variable(downloaded, "download_succeeded")));
    }

    @Test
    void downloadWithoutListeningInstancesQueuesNoEvents() throws Exception {
        this.server = startServer("/unobserved.txt", fixedBodyHandler());

        new DownloadFileAction().executeWithResolver(serverUrl("/unobserved.txt") + "||downloads", this.resolver).get(5, TimeUnit.SECONDS);

        assertAll(
                () -> assertTrue(drainMainThreadQueue().isEmpty()),
                () -> assertEquals(BODY, Files.readString(this.gameRoot.resolve("downloads/unobserved.txt"))));
    }

    @Test
    void startedEventIsDroppedWhenTheListeningInstancesChangeBeforeDelivery() throws Exception {
        this.server = startServer("/replaced.txt", fixedBodyHandler());
        ListenerInstance staleInstance = registerInstance(Listeners.ON_FILE_DOWNLOAD_STARTED);
        String url = serverUrl("/replaced.txt");

        new DownloadFileAction().executeWithResolver(url + "||downloads", this.resolver).get(5, TimeUnit.SECONDS);
        Listeners.ON_FILE_DOWNLOAD_STARTED.unregisterInstance(staleInstance);
        ListenerInstance replacement = registerInstance(Listeners.ON_FILE_DOWNLOAD_STARTED);
        List<Runnable> tasks = drainMainThreadQueue();

        assertEquals(1, tasks.size());
        tasks.get(0).run();
        assertNotEquals(url, variable(replacement, "download_url"));
    }

    private ListenerInstance registerInstance(AbstractListener listener) {
        ListenerInstance instance = listener.createFreshInstance();
        instance.registerSelfToParent();
        this.registeredInstances.add(instance);
        return instance;
    }

    private static String variable(ListenerInstance instance, String name) {
        return instance.getActionScript().getValuePlaceholders().get(name).get();
    }

    private static List<Runnable> drainMainThreadQueue() {
        return MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
    }

    private static HttpHandler fixedBodyHandler() {
        return exchange -> {
            byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        };
    }

    private HttpServer startServer(String path, HttpHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext(path, handler);
        httpServer.start();
        return httpServer;
    }

    private String serverUrl(String path) {
        return "http://127.0.0.1:" + this.server.getAddress().getPort() + path;
    }

}
