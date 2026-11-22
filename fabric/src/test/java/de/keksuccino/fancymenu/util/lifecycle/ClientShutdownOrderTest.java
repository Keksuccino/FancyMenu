package de.keksuccino.fancymenu.util.lifecycle;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The shutdown cleanups need a running client, so this checks the order in which {@link ClientShutdownHandler#shutdown()} passes them
 * to its cleanup runner, read from the compiled class.
 */
class ClientShutdownOrderTest {

    private static final String SERVER_CACHE = "de/keksuccino/fancymenu/customization/server/ServerCache.shutdown";
    private static final String EXECUTORS = "de/keksuccino/fancymenu/util/threading/FancyMenuExecutors.shutdownAll";
    private static final String DEFERRED_WATERMEDIA_PLAYERS = "de/keksuccino/fancymenu/util/watermedia/WatermediaDeferredPlayerReleaseTracker.shutdown";
    private static final String RINKU_BROWSERS = "de/keksuccino/fancymenu/util/rinku/BrowserHandler.closeAll";
    private static final String PANORAMAS = "de/keksuccino/fancymenu/customization/panorama/PanoramaHandler.shutdown";
    private static final String RESOURCES = "de/keksuccino/fancymenu/util/resource/ResourceHandlers.shutdownAll";
    private static final String MAIN_THREAD_TASKS = "de/keksuccino/fancymenu/util/threading/MainThreadTaskExecutor.shutdown";

    @Test
    void recurringWorkStopsBeforeTheResourcesItCanTouchGetDisposed() throws IOException {
        List<String> cleanups = readCleanupOrder();
        int executors = indexOf(cleanups, EXECUTORS);

        // The server cache marks itself as shutting down first, so pings that are still running cancel the connections they open late
        assertTrue(indexOf(cleanups, SERVER_CACHE) < executors, "The server cache has to shut down before the executors stop: " + cleanups);
        for (String laterCleanup : List.of(DEFERRED_WATERMEDIA_PLAYERS, RINKU_BROWSERS, PANORAMAS, RESOURCES, MAIN_THREAD_TASKS)) {
            assertTrue(executors < indexOf(cleanups, laterCleanup), laterCleanup + " has to run after the executors stopped: " + cleanups);
        }
    }

    @Test
    void theExecutorsGetStoppedAgainAsLastCleanup() throws IOException {
        List<String> cleanups = readCleanupOrder();

        // The finally block stops executors that got created while the other cleanups ran, even when one of them failed
        assertEquals(EXECUTORS, cleanups.get(cleanups.size() - 1), "Cleanups: " + cleanups);
        assertTrue(cleanups.lastIndexOf(EXECUTORS) > indexOf(cleanups, MAIN_THREAD_TASKS), "Cleanups: " + cleanups);
    }

    private static int indexOf(List<String> cleanups, String cleanup) {
        int index = cleanups.indexOf(cleanup);
        assertTrue(index >= 0, cleanup + " is missing in the shutdown: " + cleanups);
        return index;
    }

    /** The method references of the shutdown, in the order of the compiled code. Lambdas show up as their synthetic methods. */
    private static List<String> readCleanupOrder() throws IOException {
        ClassNode classNode = new ClassNode();
        try (InputStream input = ClientShutdownOrderTest.class.getResourceAsStream("/" + ClientShutdownHandler.class.getName().replace('.', '/') + ".class")) {
            assertNotNull(input, "Compiled ClientShutdownHandler class was not available on the test classpath");
            new ClassReader(input).accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        MethodNode shutdown = classNode.methods.stream().filter(method -> method.name.equals("shutdown") && method.desc.equals("()V")).findFirst().orElseThrow();
        List<String> cleanups = new ArrayList<>();
        for (AbstractInsnNode instruction : shutdown.instructions) {
            if ((instruction instanceof InvokeDynamicInsnNode invokeDynamic) && (invokeDynamic.bsmArgs.length > 1) && (invokeDynamic.bsmArgs[1] instanceof Handle implementation)) {
                cleanups.add(implementation.getOwner() + "." + implementation.getName());
            }
        }
        return cleanups;
    }

}
