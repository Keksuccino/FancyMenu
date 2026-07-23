package de.keksuccino.fancymenu.web.videoplayer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class VideoPlayerScriptTest {

    private static final Path HARNESS_RELATIVE_PATH = Path.of("fabric", "src", "test", "javascript", "de", "keksuccino", "fancymenu", "web", "videoplayer", "player.test.mjs");

    @Test
    void productionPlayerRejectsStaleAsynchronousCallbacks() throws Exception {
        Path projectRoot = findProjectRoot();
        Path harnessPath = projectRoot.resolve(HARNESS_RELATIVE_PATH);
        Process process = startNodeHarness(projectRoot, harnessPath);
        ExecutorService outputExecutor = Executors.newSingleThreadExecutor();
        Future<String> outputFuture = outputExecutor.submit(() -> new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

        try {
            if (!process.waitFor(45, TimeUnit.SECONDS)) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
                fail("The video-player Node.js regression harness exceeded its 45-second timeout.");
            }
            String output = outputFuture.get(5, TimeUnit.SECONDS);
            assertEquals(0, process.exitValue(), "The video-player Node.js regression harness failed:\n" + output);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            outputExecutor.shutdownNow();
        }
    }

    private static Process startNodeHarness(Path projectRoot, Path harnessPath) {
        IOException lastFailure = null;
        for (String nodeExecutable : nodeExecutableCandidates()) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(nodeExecutable, "--test", harnessPath.toString());
                processBuilder.directory(projectRoot.toFile());
                processBuilder.redirectErrorStream(true);
                return processBuilder.start();
            } catch (IOException e) {
                lastFailure = e;
            }
        }
        return fail("Node.js is required to run the video-player regression harness, but no usable Node.js executable was found.", lastFailure);
    }

    private static List<String> nodeExecutableCandidates() {
        String configuredExecutable = System.getenv("NODE_BINARY");
        if (configuredExecutable != null && !configuredExecutable.isBlank()) {
            return List.of(configuredExecutable, "/opt/homebrew/bin/node", "/usr/local/bin/node", "node");
        }
        return List.of("/opt/homebrew/bin/node", "/usr/local/bin/node", "node");
    }

    private static Path findProjectRoot() {
        Path currentDirectory = Path.of("").toAbsolutePath().normalize();
        while (currentDirectory != null) {
            if (Files.isRegularFile(currentDirectory.resolve(HARNESS_RELATIVE_PATH))) {
                return currentDirectory;
            }
            currentDirectory = currentDirectory.getParent();
        }
        return fail("Could not locate the project root containing " + HARNESS_RELATIVE_PATH + " from " + Path.of("").toAbsolutePath());
    }
}
