package de.keksuccino.fancymenu.util.ffmpeg.downloader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FFMPEGDownloader {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "FancyMenu-FFMPEG-Downloader");
        thread.setDaemon(true);
        return thread;
    });

    private static final File DOWNLOAD_DIRECTORY = FileUtils.createDirectory(new File(FancyMenu.INSTANCE_DATA_DIR, "ffmpeg_cache"));
    private static final File CURRENT_DIRECTORY = FileUtils.createDirectory(new File(DOWNLOAD_DIRECTORY, "current"));
    private static final File TEMP_DIRECTORY = FileUtils.createDirectory(new File(DOWNLOAD_DIRECTORY, "temp"));
    private static final Object LOCK = new Object();
    private static final AtomicReference<FFMPEGDownloadSnapshot> SNAPSHOT = new AtomicReference<>(FFMPEGDownloadSnapshot.idle());

    private static volatile @Nullable DownloadOperation currentOperation;
    private static volatile @Nullable CompletableFuture<FFMPEGInstallation> currentFuture;

    public static @NotNull File getDownloadDirectory() {
        return DOWNLOAD_DIRECTORY;
    }

    public static @NotNull FFMPEGDownloadSnapshot getSnapshot() {
        return SNAPSHOT.get();
    }

    public static boolean isDownloadRunning() {
        FFMPEGDownloadSnapshot snapshot = SNAPSHOT.get();
        return snapshot.isActive();
    }

    public static boolean isDownloadNeeded() {
        return getCachedInstallation() == null;
    }

    public static @Nullable FFMPEGInstallation getCachedInstallation() {
        try {
            PlatformSpec platform = detectPlatform();
            return loadCurrentInstallation(platform);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static @Nullable File getInstalledFfmpegBinary() {
        FFMPEGInstallation installation = getCachedInstallation();
        return installation != null ? installation.getFfmpegBinary() : null;
    }

    public static @Nullable File getInstalledFfprobeBinary() {
        FFMPEGInstallation installation = getCachedInstallation();
        return installation != null ? installation.getFfprobeBinary() : null;
    }

    public static @Nullable File getInstalledDirectory() {
        FFMPEGInstallation installation = getCachedInstallation();
        return installation != null ? installation.getInstallDirectory() : null;
    }

    public static void cancelCurrentDownload() {
        DownloadOperation operation = currentOperation;
        if (operation != null) {
            operation.cancelled = true;
        }
    }

    public static @NotNull CompletableFuture<FFMPEGInstallation> startDownloadIfNeededAsync() {
        return startDownloadIfNeededAsync(false);
    }

    public static @NotNull CompletableFuture<FFMPEGInstallation> ensureInstalledAsync() {
        return startDownloadIfNeededAsync(false);
    }

    public static @NotNull CompletableFuture<FFMPEGInstallation> startDownloadIfNeededAsync(boolean forceRedownload) {
        synchronized (LOCK) {
            if (!forceRedownload) {
                FFMPEGInstallation cached = getCachedInstallation();
                if (cached != null) {
                    setSnapshot(new FFMPEGDownloadSnapshot(
                            FFMPEGDownloadSnapshot.Stage.COMPLETE,
                            "FFmpeg is already installed.",
                            cached.getPlatformId() + " | " + cached.getProviderId(),
                            1.0D,
                            0L,
                            0L,
                            null,
                            cached,
                            true
                    ));
                    return CompletableFuture.completedFuture(cached);
                }
            }

            CompletableFuture<FFMPEGInstallation> existingFuture = currentFuture;
            if ((existingFuture != null) && !existingFuture.isDone()) {
                return existingFuture;
            }

            DownloadOperation operation = new DownloadOperation();
            currentOperation = operation;
            setSnapshot(new FFMPEGDownloadSnapshot(
                    FFMPEGDownloadSnapshot.Stage.CHECKING,
                    "Checking FFmpeg installation...",
                    null,
                    0.02D,
                    0L,
                    0L,
                    null,
                    null,
                    false
            ));

            CompletableFuture<FFMPEGInstallation> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return install(operation, forceRedownload);
                } catch (CancellationException ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }, EXECUTOR);
            currentFuture = future;
            future.whenComplete((result, throwable) -> {
                synchronized (LOCK) {
                    if (currentOperation == operation) {
                        currentOperation = null;
                    }
                    if (currentFuture == future) {
                        currentFuture = null;
                    }
                }
            });
            return future;
        }
    }

    private static @NotNull FFMPEGInstallation install(@NotNull DownloadOperation operation, boolean forceRedownload) throws Exception {
        PlatformSpec platform = detectPlatform();
        Distribution distribution = resolveDistribution(platform);
        File tempRoot = FileUtils.createDirectory(new File(TEMP_DIRECTORY, platform.id + "_" + UUID.randomUUID()));
        File stagingDirectory = new File(tempRoot, "install");
        File downloadsDirectory = FileUtils.createDirectory(new File(tempRoot, "downloads"));
        FileUtils.createDirectory(stagingDirectory);

        try {
            checkCancelled(operation);

            if (!forceRedownload) {
                FFMPEGInstallation cached = loadCurrentInstallation(platform);
                if (cached != null) {
                    setSnapshot(new FFMPEGDownloadSnapshot(
                            FFMPEGDownloadSnapshot.Stage.COMPLETE,
                            "FFmpeg is already installed.",
                            cached.getPlatformId() + " | " + cached.getProviderId(),
                            1.0D,
                            0L,
                            0L,
                            null,
                            cached,
                            true
                    ));
                    return cached;
                }
            }

            setSnapshot(new FFMPEGDownloadSnapshot(
                    FFMPEGDownloadSnapshot.Stage.CHECKING,
                    "Resolving FFmpeg download sources...",
                    platform.displayName + " | " + distribution.providerDisplayName,
                    0.05D,
                    0L,
                    0L,
                    null,
                    null,
                    false
            ));

            List<ResolvedArtifact> resolvedArtifacts = new ArrayList<>();
            long totalDownloadBytes = 0L;
            for (Artifact artifact : distribution.artifacts) {
                checkCancelled(operation);
                ResolvedRemote archive = resolveRemote(artifact.archiveUrl);
                String checksumUrl = archive.url() + ".sha256";
                String checksum = parseSha256(downloadText(checksumUrl));
                resolvedArtifacts.add(new ResolvedArtifact(artifact, archive, checksumUrl, checksum));
                if (archive.contentLength() > 0L) {
                    totalDownloadBytes += archive.contentLength();
                }
            }

            long downloadedBytes = 0L;
            List<File> archiveFiles = new ArrayList<>();
            for (ResolvedArtifact artifact : resolvedArtifacts) {
                checkCancelled(operation);
                String detail = "Downloading " + artifact.artifact.displayName + "...";
                setSnapshot(new FFMPEGDownloadSnapshot(
                        FFMPEGDownloadSnapshot.Stage.DOWNLOADING,
                        detail,
                        platform.displayName + " | " + distribution.providerDisplayName,
                        computeRangeProgress(0.10D, 0.65D, downloadedBytes, totalDownloadBytes),
                        downloadedBytes,
                        totalDownloadBytes,
                        null,
                        null,
                        false
                ));
                File targetArchive = new File(downloadsDirectory, artifact.artifact.id + ".zip");
                long actualBytes = downloadFile(operation, artifact, targetArchive, downloadedBytes, totalDownloadBytes);
                downloadedBytes += actualBytes;
                archiveFiles.add(targetArchive);
            }

            long totalExtractBytes = 0L;
            for (File archiveFile : archiveFiles) {
                totalExtractBytes += computeZipUncompressedSize(archiveFile);
            }

            long extractedBytes = 0L;
            for (int i = 0; i < archiveFiles.size(); i++) {
                checkCancelled(operation);
                File archiveFile = archiveFiles.get(i);
                ResolvedArtifact artifact = resolvedArtifacts.get(i);
                setSnapshot(new FFMPEGDownloadSnapshot(
                        FFMPEGDownloadSnapshot.Stage.EXTRACTING,
                        "Extracting " + artifact.artifact.displayName + "...",
                        archiveFile.getName(),
                        computeRangeProgress(0.65D, 0.90D, extractedBytes, totalExtractBytes),
                        extractedBytes,
                        totalExtractBytes,
                        null,
                        null,
                        false
                ));
                extractedBytes += extractZip(operation, archiveFile, stagingDirectory, extractedBytes, totalExtractBytes);
            }

            setSnapshot(new FFMPEGDownloadSnapshot(
                    FFMPEGDownloadSnapshot.Stage.VALIDATING,
                    "Validating downloaded FFmpeg binaries...",
                    platform.displayName + " | " + distribution.providerDisplayName,
                    0.93D,
                    0L,
                    0L,
                    null,
                    null,
                    false
            ));

            File ffmpegBinary = findBinary(stagingDirectory.toPath(), "ffmpeg" + platform.binarySuffix);
            File ffprobeBinary = findBinary(stagingDirectory.toPath(), "ffprobe" + platform.binarySuffix);
            if (ffmpegBinary == null || ffprobeBinary == null) {
                throw new IOException("Downloaded archive did not contain both ffmpeg and ffprobe binaries.");
            }

            ensureExecutable(ffmpegBinary, platform);
            ensureExecutable(ffprobeBinary, platform);

            String ffmpegVersionLine = validateBinary(ffmpegBinary, "ffmpeg");
            String ffprobeVersionLine = validateBinary(ffprobeBinary, "ffprobe");

            InstallationMetadata metadata = new InstallationMetadata();
            metadata.schemaVersion = 1;
            metadata.platformId = platform.id;
            metadata.providerId = distribution.providerId;
            metadata.platformDisplayName = platform.displayName;
            metadata.installedAtEpochMs = System.currentTimeMillis();
            metadata.ffmpegRelativePath = relativize(stagingDirectory, ffmpegBinary);
            metadata.ffprobeRelativePath = relativize(stagingDirectory, ffprobeBinary);
            metadata.ffmpegVersionLine = ffmpegVersionLine;
            metadata.ffprobeVersionLine = ffprobeVersionLine;
            metadata.artifacts = new ArrayList<>();
            for (ResolvedArtifact artifact : resolvedArtifacts) {
                ArtifactMetadata artifactMetadata = new ArtifactMetadata();
                artifactMetadata.id = artifact.artifact.id;
                artifactMetadata.displayName = artifact.artifact.displayName;
                artifactMetadata.resolvedArchiveUrl = artifact.remote.url();
                artifactMetadata.checksumUrl = artifact.checksumUrl;
                artifactMetadata.sha256 = artifact.sha256;
                artifactMetadata.contentLength = artifact.remote.contentLength();
                metadata.artifacts.add(artifactMetadata);
            }
            writeMetadata(new File(stagingDirectory, "installation.json"), metadata);

            FFMPEGInstallation installation = activateInstall(platform, stagingDirectory, tempRoot);
            setSnapshot(new FFMPEGDownloadSnapshot(
                    FFMPEGDownloadSnapshot.Stage.COMPLETE,
                    "FFmpeg download completed.",
                    installation.getPlatformId() + " | " + installation.getProviderId(),
                    1.0D,
                    totalDownloadBytes,
                    totalDownloadBytes,
                    null,
                    installation,
                    false
            ));
            return installation;
        } catch (CancellationException ex) {
            setSnapshot(new FFMPEGDownloadSnapshot(
                    FFMPEGDownloadSnapshot.Stage.CANCELLED,
                    "FFmpeg download cancelled.",
                    null,
                    0.0D,
                    0L,
                    0L,
                    "Cancelled",
                    null,
                    false
            ));
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] FFmpeg download failed!", ex);
            String message = ex.getMessage();
            if ((message == null) || message.isBlank()) {
                message = ex.getClass().getSimpleName();
            }
            setSnapshot(new FFMPEGDownloadSnapshot(
                    FFMPEGDownloadSnapshot.Stage.FAILED,
                    "FFmpeg download failed.",
                    message,
                    0.0D,
                    0L,
                    0L,
                    message,
                    null,
                    false
            ));
            throw ex;
        } finally {
            org.apache.commons.io.FileUtils.deleteQuietly(tempRoot);
        }
    }

    private static @NotNull FFMPEGInstallation activateInstall(@NotNull PlatformSpec platform, @NotNull File stagingDirectory, @NotNull File tempRoot) throws Exception {
        File currentDirectory = getPlatformInstallDirectory(platform);
        File backupDirectory = new File(currentDirectory.getParentFile(), currentDirectory.getName() + ".backup");
        org.apache.commons.io.FileUtils.deleteQuietly(backupDirectory);

        boolean movedCurrentToBackup = false;
        try {
            if (currentDirectory.exists()) {
                moveDirectory(currentDirectory.toPath(), backupDirectory.toPath());
                movedCurrentToBackup = true;
            }
            moveDirectory(stagingDirectory.toPath(), currentDirectory.toPath());
            org.apache.commons.io.FileUtils.deleteQuietly(backupDirectory);
            return Objects.requireNonNull(loadCurrentInstallation(platform));
        } catch (Exception ex) {
            if (!currentDirectory.exists() && movedCurrentToBackup && backupDirectory.exists()) {
                try {
                    moveDirectory(backupDirectory.toPath(), currentDirectory.toPath());
                } catch (Exception restoreEx) {
                    LOGGER.error("[FANCYMENU] Failed to restore previous FFmpeg installation after activation failure!", restoreEx);
                }
            }
            throw ex;
        } finally {
            if (tempRoot.exists()) {
                File refreshedStage = new File(tempRoot, "install");
                if (refreshedStage.exists()) {
                    org.apache.commons.io.FileUtils.deleteQuietly(refreshedStage);
                }
            }
        }
    }

    private static void moveDirectory(@NotNull Path source, @NotNull Path target) throws IOException {
        Files.createDirectories(Objects.requireNonNull(target.getParent()));
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static long computeZipUncompressedSize(@NotNull File zipFile) throws IOException {
        long totalBytes = 0L;
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getSize() > 0L) {
                    totalBytes += entry.getSize();
                }
            }
        }
        return totalBytes;
    }

    private static long extractZip(
            @NotNull DownloadOperation operation,
            @NotNull File zipFile,
            @NotNull File targetDirectory,
            long extractedBytesBefore,
            long totalExtractBytes
    ) throws Exception {
        long extractedNow = 0L;
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            byte[] buffer = new byte[65536];
            while (entries.hasMoreElements()) {
                checkCancelled(operation);
                ZipEntry entry = entries.nextElement();
                Path targetPath = targetDirectory.toPath().resolve(entry.getName()).normalize();
                if (!targetPath.startsWith(targetDirectory.toPath())) {
                    throw new IOException("Blocked unsafe ZIP entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                    continue;
                }
                Files.createDirectories(Objects.requireNonNull(targetPath.getParent()));
                try (InputStream in = new BufferedInputStream(zip.getInputStream(entry));
                     OutputStream out = Files.newOutputStream(targetPath)) {
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        checkCancelled(operation);
                        out.write(buffer, 0, read);
                        extractedNow += read;
                        setSnapshot(new FFMPEGDownloadSnapshot(
                                FFMPEGDownloadSnapshot.Stage.EXTRACTING,
                                "Extracting FFmpeg binaries...",
                                zipFile.getName(),
                                computeRangeProgress(0.65D, 0.90D, extractedBytesBefore + extractedNow, totalExtractBytes),
                                extractedBytesBefore + extractedNow,
                                totalExtractBytes,
                                null,
                                null,
                                false
                        ));
                    }
                }
            }
        }
        return extractedNow;
    }

    private static long downloadFile(
            @NotNull DownloadOperation operation,
            @NotNull ResolvedArtifact artifact,
            @NotNull File targetFile,
            long downloadedBytesBefore,
            long totalDownloadBytes
    ) throws Exception {
        HttpURLConnection connection = null;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long downloadedNow = 0L;
        try {
            connection = openConnection(artifact.remote.url(), "GET");
            int responseCode = connection.getResponseCode();
            if ((responseCode < 200) || (responseCode >= 300)) {
                throw new IOException("Failed to download " + artifact.artifact.displayName + " (" + responseCode + ")");
            }
            Files.createDirectories(Objects.requireNonNull(targetFile.getParentFile()).toPath());
            byte[] buffer = new byte[65536];
            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    checkCancelled(operation);
                    out.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                    downloadedNow += read;
                    setSnapshot(new FFMPEGDownloadSnapshot(
                            FFMPEGDownloadSnapshot.Stage.DOWNLOADING,
                            "Downloading " + artifact.artifact.displayName + "...",
                            formatBytes(downloadedBytesBefore + downloadedNow) + " / " + formatBytes(totalDownloadBytes),
                            computeRangeProgress(0.10D, 0.65D, downloadedBytesBefore + downloadedNow, totalDownloadBytes),
                            downloadedBytesBefore + downloadedNow,
                            totalDownloadBytes,
                            null,
                            null,
                            false
                    ));
                }
            }
            String actualSha256 = toHex(digest.digest());
            if (!actualSha256.equalsIgnoreCase(artifact.sha256)) {
                throw new IOException("Checksum verification failed for " + artifact.artifact.displayName + ".");
            }
            return downloadedNow;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static @NotNull String validateBinary(@NotNull File binary, @NotNull String binaryName) throws Exception {
        Process process = new ProcessBuilder(binary.getAbsolutePath(), "-version")
                .directory(binary.getParentFile())
                .redirectErrorStream(true)
                .start();
        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (!process.waitFor(10L, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Timed out while validating " + binaryName + ".");
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Validation process for " + binaryName + " exited with code " + exitCode + ".");
        }
        String[] lines = output.split("\\R");
        String firstLine = lines.length > 0 ? lines[0].trim() : "";
        if (!firstLine.toLowerCase(Locale.ROOT).contains(binaryName + " version")) {
            throw new IOException("Validation output for " + binaryName + " looked invalid.");
        }
        return firstLine;
    }

    private static void ensureExecutable(@NotNull File binary, @NotNull PlatformSpec platform) {
        if (platform.family == PlatformFamily.WINDOWS) {
            return;
        }
        try {
            binary.setExecutable(true, false);
        } catch (Exception ignored) {
        }
    }

    private static @Nullable File findBinary(@NotNull Path root, @NotNull String binaryFileName) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(binaryFileName))
                    .map(Path::toFile)
                    .findFirst()
                    .orElse(null);
        }
    }

    private static @NotNull PlatformSpec detectPlatform() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        PlatformFamily family;
        String osSegment;
        if (osName.contains("win")) {
            family = PlatformFamily.WINDOWS;
            osSegment = "windows";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            family = PlatformFamily.MACOS;
            osSegment = "macos";
        } else if (osName.contains("linux")) {
            family = PlatformFamily.LINUX;
            osSegment = "linux";
        } else {
            throw new IllegalStateException("Unsupported operating system: " + osName);
        }

        String archSegment;
        String displayArch;
        if (arch.equals("amd64") || arch.equals("x86_64") || arch.equals("x64")) {
            archSegment = "amd64";
            displayArch = "x64";
        } else if (arch.equals("aarch64") || arch.equals("arm64")) {
            archSegment = "arm64";
            displayArch = "arm64";
        } else {
            throw new IllegalStateException("Unsupported system architecture: " + arch);
        }

        if ((family == PlatformFamily.WINDOWS) && !archSegment.equals("amd64")) {
            throw new IllegalStateException("Windows FFmpeg download is currently only supported on x64.");
        }

        String displayName = switch (family) {
            case WINDOWS -> "Windows " + displayArch;
            case LINUX -> "Linux " + displayArch;
            case MACOS -> "macOS " + displayArch;
        };
        String binarySuffix = family == PlatformFamily.WINDOWS ? ".exe" : "";

        return new PlatformSpec(family, osSegment, archSegment, binarySuffix, osSegment + "-" + archSegment, displayName);
    }

    private static @NotNull Distribution resolveDistribution(@NotNull PlatformSpec platform) {
        if ((platform.family == PlatformFamily.WINDOWS) && platform.archSegment.equals("amd64")) {
            return new Distribution(
                    "gyan",
                    "Gyan.dev",
                    List.of(new Artifact(
                            "bundle",
                            "FFmpeg bundle",
                            "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"
                    ))
            );
        }
        if ((platform.family == PlatformFamily.LINUX) || (platform.family == PlatformFamily.MACOS)) {
            String base = "https://ffmpeg.martin-riedl.de/redirect/latest/" + platform.osSegment + "/" + platform.archSegment + "/release";
            return new Distribution(
                    "martin-riedl",
                    "Martin Riedl",
                    List.of(
                            new Artifact("ffmpeg", "ffmpeg", base + "/ffmpeg.zip"),
                            new Artifact("ffprobe", "ffprobe", base + "/ffprobe.zip")
                    )
            );
        }
        throw new IllegalStateException("No FFmpeg download provider is configured for " + platform.displayName + ".");
    }

    private static @Nullable FFMPEGInstallation loadCurrentInstallation(@NotNull PlatformSpec platform) {
        File currentDirectory = getPlatformInstallDirectory(platform);
        File metadataFile = new File(currentDirectory, "installation.json");
        if (!metadataFile.isFile()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(metadataFile.toPath()), StandardCharsets.UTF_8))) {
            InstallationMetadata metadata = GSON.fromJson(reader, InstallationMetadata.class);
            if ((metadata == null) || (metadata.schemaVersion != 1)) {
                return null;
            }
            File ffmpegBinary = new File(currentDirectory, Objects.requireNonNullElse(metadata.ffmpegRelativePath, ""));
            File ffprobeBinary = new File(currentDirectory, Objects.requireNonNullElse(metadata.ffprobeRelativePath, ""));
            FFMPEGInstallation installation = new FFMPEGInstallation(
                    currentDirectory,
                    ffmpegBinary,
                    ffprobeBinary,
                    Objects.requireNonNullElse(metadata.platformId, platform.id),
                    Objects.requireNonNullElse(metadata.providerId, "unknown"),
                    Objects.requireNonNullElse(metadata.ffmpegVersionLine, ""),
                    Objects.requireNonNullElse(metadata.ffprobeVersionLine, "")
            );
            return installation.isValid() ? installation : null;
        } catch (IOException | JsonSyntaxException ex) {
            LOGGER.error("[FANCYMENU] Failed to read cached FFmpeg installation metadata!", ex);
            return null;
        }
    }

    private static void writeMetadata(@NotNull File targetFile, @NotNull InstallationMetadata metadata) throws IOException {
        Files.createDirectories(Objects.requireNonNull(targetFile.getParentFile()).toPath());
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(targetFile.toPath()), StandardCharsets.UTF_8))) {
            GSON.toJson(metadata, writer);
        }
    }

    private static @NotNull File getPlatformInstallDirectory(@NotNull PlatformSpec platform) {
        return new File(CURRENT_DIRECTORY, platform.id);
    }

    private static @NotNull String relativize(@NotNull File root, @NotNull File file) {
        return root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
    }

    private static @NotNull String downloadText(@NotNull String url) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(url, "GET");
            int responseCode = connection.getResponseCode();
            if ((responseCode < 200) || (responseCode >= 300)) {
                throw new IOException("Failed to fetch remote metadata (" + responseCode + "): " + url);
            }
            try (InputStream in = connection.getInputStream()) {
                return IOUtils.toString(in, StandardCharsets.UTF_8);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static @NotNull String parseSha256(@NotNull String raw) throws IOException {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IOException("Remote SHA-256 response was empty.");
        }
        String candidate = trimmed.split("\\s+")[0];
        if (!candidate.matches("(?i)[0-9a-f]{64}")) {
            throw new IOException("Remote SHA-256 response was invalid.");
        }
        return candidate.toLowerCase(Locale.ROOT);
    }

    private static @NotNull ResolvedRemote resolveRemote(@NotNull String url) throws Exception {
        URL current = URI.create(url).toURL();
        for (int i = 0; i < 8; i++) {
            HttpURLConnection connection = (HttpURLConnection) current.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "FancyMenu/" + FancyMenu.VERSION);
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestMethod("HEAD");
            int responseCode;
            try {
                responseCode = connection.getResponseCode();
            } catch (IOException ex) {
                connection.disconnect();
                return resolveRemoteViaGet(current.toString());
            }
            if (isRedirect(responseCode)) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if ((location == null) || location.isBlank()) {
                    throw new IOException("Remote redirect response was missing a location.");
                }
                current = new URL(current, location);
                continue;
            }
            if ((responseCode >= 200) && (responseCode < 300)) {
                long contentLength = connection.getContentLengthLong();
                connection.disconnect();
                return new ResolvedRemote(current.toString(), contentLength);
            }
            connection.disconnect();
            return resolveRemoteViaGet(current.toString());
        }
        throw new IOException("Too many redirects while resolving remote download.");
    }

    private static @NotNull ResolvedRemote resolveRemoteViaGet(@NotNull String url) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(url, "GET");
            int responseCode = connection.getResponseCode();
            if ((responseCode < 200) || (responseCode >= 300)) {
                throw new IOException("Failed to resolve remote download (" + responseCode + ")");
            }
            long contentLength = connection.getContentLengthLong();
            return new ResolvedRemote(connection.getURL().toString(), contentLength);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static boolean isRedirect(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_MOVED_PERM
                || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                || responseCode == 307
                || responseCode == 308;
    }

    private static @NotNull HttpURLConnection openConnection(@NotNull String url, @NotNull String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("User-Agent", "FancyMenu/" + FancyMenu.VERSION);
        connection.setRequestProperty("Accept-Encoding", "identity");
        return connection;
    }

    private static void checkCancelled(@NotNull DownloadOperation operation) {
        if (operation.cancelled) {
            throw new CancellationException("FFmpeg download cancelled.");
        }
    }

    private static void setSnapshot(@NotNull FFMPEGDownloadSnapshot snapshot) {
        SNAPSHOT.set(snapshot);
    }

    private static double computeRangeProgress(double rangeStart, double rangeEnd, long current, long total) {
        if (total <= 0L) {
            return rangeStart;
        }
        double ratio = Math.max(0.0D, Math.min(1.0D, (double) current / (double) total));
        return rangeStart + ((rangeEnd - rangeStart) * ratio);
    }

    private static @NotNull String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (value >= 1024.0D && unitIndex < units.length - 1) {
            value /= 1024.0D;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex]);
    }

    private static @NotNull String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }

    private enum PlatformFamily {
        WINDOWS,
        LINUX,
        MACOS
    }

    private static final class DownloadOperation {
        private volatile boolean cancelled;
    }

    private static final class PlatformSpec {
        private final @NotNull PlatformFamily family;
        private final @NotNull String osSegment;
        private final @NotNull String archSegment;
        private final @NotNull String binarySuffix;
        private final @NotNull String id;
        private final @NotNull String displayName;

        private PlatformSpec(
                @NotNull PlatformFamily family,
                @NotNull String osSegment,
                @NotNull String archSegment,
                @NotNull String binarySuffix,
                @NotNull String id,
                @NotNull String displayName
        ) {
            this.family = family;
            this.osSegment = osSegment;
            this.archSegment = archSegment;
            this.binarySuffix = binarySuffix;
            this.id = id;
            this.displayName = displayName;
        }
    }

    private static final class Distribution {
        private final @NotNull String providerId;
        private final @NotNull String providerDisplayName;
        private final @NotNull List<Artifact> artifacts;

        private Distribution(@NotNull String providerId, @NotNull String providerDisplayName, @NotNull List<Artifact> artifacts) {
            this.providerId = providerId;
            this.providerDisplayName = providerDisplayName;
            this.artifacts = artifacts;
        }
    }

    private static final class Artifact {
        private final @NotNull String id;
        private final @NotNull String displayName;
        private final @NotNull String archiveUrl;

        private Artifact(@NotNull String id, @NotNull String displayName, @NotNull String archiveUrl) {
            this.id = id;
            this.displayName = displayName;
            this.archiveUrl = archiveUrl;
        }
    }

    private record ResolvedRemote(@NotNull String url, long contentLength) {
    }

    private static final class ResolvedArtifact {
        private final @NotNull Artifact artifact;
        private final @NotNull ResolvedRemote remote;
        private final @NotNull String checksumUrl;
        private final @NotNull String sha256;

        private ResolvedArtifact(@NotNull Artifact artifact, @NotNull ResolvedRemote remote, @NotNull String checksumUrl, @NotNull String sha256) {
            this.artifact = artifact;
            this.remote = remote;
            this.checksumUrl = checksumUrl;
            this.sha256 = sha256;
        }
    }

    private static final class InstallationMetadata {
        private int schemaVersion;
        private @Nullable String platformId;
        private @Nullable String providerId;
        private @Nullable String platformDisplayName;
        private long installedAtEpochMs;
        private @Nullable String ffmpegRelativePath;
        private @Nullable String ffprobeRelativePath;
        private @Nullable String ffmpegVersionLine;
        private @Nullable String ffprobeVersionLine;
        private @Nullable List<ArtifactMetadata> artifacts;
    }

    private static final class ArtifactMetadata {
        private @Nullable String id;
        private @Nullable String displayName;
        private @Nullable String resolvedArchiveUrl;
        private @Nullable String checksumUrl;
        private @Nullable String sha256;
        private long contentLength;
    }

}
