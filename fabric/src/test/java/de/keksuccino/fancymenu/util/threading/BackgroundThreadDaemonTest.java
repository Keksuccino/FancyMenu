package de.keksuccino.fancymenu.util.threading;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A non-daemon thread that still runs keeps the JVM alive after Minecraft finished its own shutdown, so the client hangs until the
 * shutdown watchdog crashes it. This scans the compiled mod classes for every way to start one.
 */
class BackgroundThreadDaemonTest {

    private static final String THREAD = "java/lang/Thread";
    private static final String EXECUTORS = "java/util/concurrent/Executors";
    /** {@link java.util.concurrent.Executors} factories without a thread factory start non-daemon threads. */
    private static final Set<String> NON_DAEMON_EXECUTOR_FACTORIES = Set.of(
            "newSingleThreadExecutor()Ljava/util/concurrent/ExecutorService;",
            "newSingleThreadScheduledExecutor()Ljava/util/concurrent/ScheduledExecutorService;",
            "newFixedThreadPool(I)Ljava/util/concurrent/ExecutorService;",
            "newCachedThreadPool()Ljava/util/concurrent/ExecutorService;",
            "newScheduledThreadPool(I)Ljava/util/concurrent/ScheduledExecutorService;");
    /** Constructors of pools and timers that start non-daemon threads. */
    private static final Set<String> NON_DAEMON_CONSTRUCTORS = Set.of(
            "java/util/concurrent/ScheduledThreadPoolExecutor(I)V",
            "java/util/concurrent/ThreadPoolExecutor(IIJLjava/util/concurrent/TimeUnit;Ljava/util/concurrent/BlockingQueue;)V",
            "java/util/Timer()V",
            "java/util/Timer(Ljava/lang/String;)V");

    @Test
    void everyThreadOfTheModIsADaemonThread() throws IOException, URISyntaxException {
        Path classesRoot = Path.of(FancyMenuThreads.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path modClasses = classesRoot.resolve("de/keksuccino/fancymenu");
        assertTrue(Files.isDirectory(modClasses), "Compiled mod classes not found in " + classesRoot);

        List<String> violations = new ArrayList<>();
        int scannedClasses = 0;
        try (Stream<Path> files = Files.walk(modClasses)) {
            for (Path classFile : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".class"))::iterator) {
                scanClass(classFile, violations);
                scannedClasses++;
            }
        }

        // Makes sure the scan really found the mod, the loaders of all resource types alone are more than this
        assertTrue(scannedClasses > 100, "Only scanned " + scannedClasses + " classes in " + modClasses);
        assertEquals(List.of(), violations, "Methods that start non-daemon threads, use FancyMenuThreads or FancyMenuExecutors instead");
    }

    private static void scanClass(Path classFile, List<String> violations) throws IOException {
        ClassNode classNode = new ClassNode();
        try (InputStream input = Files.newInputStream(classFile)) {
            new ClassReader(input).accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        for (MethodNode method : classNode.methods) {
            String location = classNode.name.replace('/', '.') + "#" + method.name;
            int createdThreads = 0;
            int daemonThreads = 0;
            for (AbstractInsnNode instruction : method.instructions) {
                if ((instruction instanceof TypeInsnNode typeInstruction) && (typeInstruction.getOpcode() == Opcodes.NEW) && typeInstruction.desc.equals(THREAD)) {
                    createdThreads++;
                } else if (instruction instanceof MethodInsnNode invocation) {
                    if (isDaemonFlagSetToTrue(invocation) || isShutdownHookRegistration(invocation)) {
                        // Shutdown hooks only start once the JVM already shuts down
                        daemonThreads++;
                    } else if (invocation.owner.equals(EXECUTORS) && NON_DAEMON_EXECUTOR_FACTORIES.contains(invocation.name + invocation.desc)) {
                        violations.add(location + " uses Executors#" + invocation.name + " without a thread factory");
                    } else if (invocation.name.equals("<init>") && NON_DAEMON_CONSTRUCTORS.contains(invocation.owner + invocation.desc)) {
                        violations.add(location + " creates a non-daemon " + invocation.owner.replace('/', '.'));
                    }
                }
            }
            if (createdThreads > daemonThreads) violations.add(location + " starts a Thread without setDaemon(true)");
        }
    }

    private static boolean isDaemonFlagSetToTrue(MethodInsnNode invocation) {
        if (!invocation.owner.equals(THREAD) || !invocation.name.equals("setDaemon") || !invocation.desc.equals("(Z)V")) return false;
        AbstractInsnNode argument = invocation.getPrevious();
        return (argument != null) && (argument.getOpcode() == Opcodes.ICONST_1);
    }

    private static boolean isShutdownHookRegistration(MethodInsnNode invocation) {
        return invocation.owner.equals("java/lang/Runtime") && invocation.name.equals("addShutdownHook");
    }

}
